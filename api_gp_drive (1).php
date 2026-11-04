<?php
declare(strict_types=1);

/**
 * TunerTools - Google Drive GP API, optimized for ~1.64M songs / alwaysdata.
 * Drop-in replacement for api_gp_drive.php (same actions and JSON fields).
 * Requires: PHP 7.4+, pdo_sqlite with SQLite FTS5, curl, openssl.
 *
 * Endpoints:
 *   ?action=health
 *   ?action=search&q=hotel&limit=50[&offset=0]
 *   ?action=file&id=6635943603902029564
 *   ?action=sections&section=nhac_viet&q=
 *   ?action=sections&section=dieu_nhac&rhythm=bolero
 *   ?action=sections&section=quoc_te&q=hotel
 *
 * Sections use indexed SQLite file_path; gp_sections.json is optional for extra Drive-only files.
 * Security: Keep the DB and drive_token.json outside webroot where possible.
 * Set TUNERTOOLS_GP_DB_PATH and TUNERTOOLS_GP_TOKEN_PATH in hosting environment
 * if those files are elsewhere. Never expose OAuth credentials over HTTP.
 */

// --------------------------- CONFIG ----------------------------------
// Unchanged database name, endpoints, and Android JSON response shape.
$DB_PATH = getenv('TUNERTOOLS_GP_DB_PATH') ?: __DIR__ . '/songsterr_drive_test.db';
$TOKEN_PATH = getenv('TUNERTOOLS_GP_TOKEN_PATH') ?: __DIR__ . '/drive_token.json';
// Optional JSON generated on Windows; database alone is enough for indexed GP files.
$SECTION_MANIFEST_PATH = getenv('TUNERTOOLS_GP_SECTIONS_PATH') ?: __DIR__ . '/gp_sections.json';

// Fixed public address avoids Host-header injection and keeps Kotlin URLs stable.
$PUBLIC_API_URL = 'https://tuner.alwaysdata.net/api_gp_drive.php';
$DEFAULT_LIMIT = 50;
$MAX_LIMIT = 100;
$MAX_OFFSET = 10000;
$MAX_QUERY_BYTES = 240;

// Preserve legacy search compatibility if FTS5 is missing/broken.
// LIKE is used ONLY on an FTS execution error, never when FTS returns 0 hits.
// On 1.64M rows, FTS5 should be enabled for good performance.
$ALLOW_LIKE_ON_FTS_ERROR = true;

// ------------------------- HTTP HEADERS -------------------------------
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Range');
header('Access-Control-Expose-Headers: Content-Length, Content-Range, Accept-Ranges, Content-Disposition');
header('X-Content-Type-Options: nosniff');
header('X-Robots-Tag: noindex');
header('Cache-Control: no-store');

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
if ($method === 'OPTIONS') {
    http_response_code(204);
    exit;
}
if ($method !== 'GET') {
    header('Allow: GET, OPTIONS');
    apiJson(['ok' => false, 'error' => 'method_not_allowed'], 405);
}

// ---------------------------- HELPERS ---------------------------------
function apiJson(array $data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    if ($status >= 400) {
        header('Cache-Control: no-store');
    }
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

function apiLog(string $message): void
{
    // Never include OAuth token, refresh token, client secret or file content.
    error_log('[TunerTools GP] ' . $message);
}

function gpDb(string $dbPath): PDO
{
    if (!is_file($dbPath) || !is_readable($dbPath)) {
        apiJson(['ok' => false, 'error' => 'database_not_found'], 500);
    }
    if (!extension_loaded('pdo_sqlite')) {
        apiJson(['ok' => false, 'error' => 'pdo_sqlite_not_available'], 503);
    }
    try {
        $pdo = new PDO('sqlite:' . $dbPath, null, null, [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_TIMEOUT => 3,
        ]);
        $pdo->exec('PRAGMA query_only = ON');
        $pdo->exec('PRAGMA busy_timeout = 3000');
        // Small cache: shared hosting may have limited per-process memory.
        $pdo->exec('PRAGMA cache_size = -2048');
        return $pdo;
    } catch (Throwable $e) {
        apiLog('Could not open SQLite: ' . $e->getMessage());
        apiJson(['ok' => false, 'error' => 'sqlite_open_failed'], 500);
    }
}

/** Safe prefix-AND query: "hotel"* AND "california"*.
 * Requires SQLite FTS5 table songs_fts with rowid matching songs.rowid.
 */
function gpFtsQuery(string $query): string
{
    if (!preg_match_all('/[\p{L}\p{N}\p{M}]+/u', $query, $matches)) {
        return '';
    }
    $terms = array_slice($matches[0], 0, 12);
    $parts = [];
    foreach ($terms as $term) {
        // Regex excludes FTS operators and double quotes.
        $parts[] = '"' . $term . '"*';
    }
    return implode(' AND ', $parts);
}

function gpLikeFallback(PDO $pdo, string $query, int $limit, int $offset): array
{
    // LIKE cannot use a normal B-tree index with a leading wildcard.
    $q = str_replace(['\\', '%', '_'], ['\\\\', '\\%', '\\_'], $query);
    $stmt = $pdo->prepare(
        "SELECT id, title, artist, file_path, format FROM songs
         WHERE drive_file_id IS NOT NULL AND drive_file_id <> ''
           AND (title LIKE :q ESCAPE '\\' OR artist LIKE :q ESCAPE '\\' OR file_path LIKE :q ESCAPE '\\')
         LIMIT :lim OFFSET :off"
    );
    $stmt->bindValue(':q', '%' . $q . '%', PDO::PARAM_STR);
    $stmt->bindValue(':lim', $limit, PDO::PARAM_INT);
    $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
    $stmt->execute();
    return $stmt->fetchAll();
}

// ----------------------- TOKEN MANAGEMENT -----------------------------
function gpReadToken(string $tokenPath): array
{
    if (!is_file($tokenPath) || !is_readable($tokenPath)) {
        apiJson(['ok' => false, 'error' => 'drive_token_not_found'], 500);
    }
    $raw = @file_get_contents($tokenPath);
    $token = is_string($raw) ? json_decode($raw, true) : null;
    if (!is_array($token)) {
        apiJson(['ok' => false, 'error' => 'drive_token_invalid_json'], 500);
    }
    return $token;
}

function gpTokenNeedsRefresh(array $token): bool
{
    if (empty($token['token'])) {
        return true;
    }
    $raw = trim((string)($token['expiry'] ?? ''));
    if ($raw === '') {
        return true;
    }
    // google-auth Python serializes a UTC timestamp without necessarily adding Z.
    if (!preg_match('/(?:Z|[+-][0-9]{2}:[0-9]{2})$/i', $raw)) {
        $raw .= 'Z';
    }
    $expiry = strtotime($raw);
    return ($expiry === false || $expiry <= time() + 120);
}

function gpRefreshToken(array $token): array
{
    $refreshToken = (string)($token['refresh_token'] ?? '');
    $clientId = (string)($token['client_id'] ?? '');
    $secret = (string)($token['client_secret'] ?? '');
    if ($refreshToken === '' || $clientId === '' || $secret === '') {
        apiJson(['ok' => false, 'error' => 'refresh_token_data_missing'], 500);
    }
    if (!function_exists('curl_init')) {
        apiJson(['ok' => false, 'error' => 'php_curl_not_available'], 503);
    }

    // Google OAuth endpoint is fixed, not supplied by clients.
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'client_id' => $clientId,
            'client_secret' => $secret,
            'refresh_token' => $refreshToken,
            'grant_type' => 'refresh_token',
        ]),
        CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded'],
        CURLOPT_CONNECTTIMEOUT => 8,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    $body = curl_exec($ch);
    $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlErr = curl_errno($ch);
    curl_close($ch);

    $payload = is_string($body) ? json_decode($body, true) : null;
    if ($curlErr !== 0 || $status < 200 || $status >= 300 || !is_array($payload) || empty($payload['access_token'])) {
        $googleCode = is_array($payload) ? (string)($payload['error'] ?? '') : '';
        apiLog('OAuth refresh failed: HTTP ' . $status . ', curl ' . $curlErr . ', reason=' . $googleCode);
        apiJson([
            'ok' => false,
            'error' => 'google_token_refresh_failed',
            'http_code' => $status,
            'google_error' => $googleCode,
        ], 502);
    }
    $token['token'] = (string)$payload['access_token'];
    if (isset($payload['expires_in'])) {
        $token['expiry'] = gmdate('Y-m-d\TH:i:s\Z', time() + (int)$payload['expires_in']);
    }
    // Preserve refresh_token, client_id, client_secret and scopes.
    return $token;
}

function gpSaveToken(string $path, array $token): void
{
    $json = json_encode($token, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
    if ($json === false) {
        apiJson(['ok' => false, 'error' => 'drive_token_write_failed'], 500);
    }
    $temp = $path . '.tmp.' . bin2hex(random_bytes(5));
    if (@file_put_contents($temp, $json, LOCK_EX) === false) {
        apiLog('Token directory not writable');
        apiJson(['ok' => false, 'error' => 'drive_token_write_failed'], 500);
    }
    @chmod($temp, 0600);
    if (!@rename($temp, $path)) {
        @unlink($temp);
        apiJson(['ok' => false, 'error' => 'drive_token_write_failed'], 500);
    }
}

/** Lock refresh across concurrent PHP workers, re-read after taking lock. */
function gpGetAccessToken(string $tokenPath, bool $force = false, string $rejectedToken = ''): string
{
    // Fast path for normal downloads: no file lock unless refresh is needed.
    if (!$force) {
        $current = gpReadToken($tokenPath);
        if (!gpTokenNeedsRefresh($current)) {
            return (string)$current['token'];
        }
    }
    $lockPath = sys_get_temp_dir() . '/tt_gp_token_' . hash('sha256', $tokenPath) . '.lock';
    $lock = @fopen($lockPath, 'c');
    if ($lock === false || !flock($lock, LOCK_EX)) {
        apiJson(['ok' => false, 'error' => 'drive_token_lock_failed'], 500);
    }
    try {
        $token = gpReadToken($tokenPath);
        // A concurrent request may already have refreshed the rejected token.
        $changedSinceRequest = ($force && $rejectedToken !== '' &&
            (string)($token['token'] ?? '') !== $rejectedToken);
        if (!$changedSinceRequest && ($force || gpTokenNeedsRefresh($token))) {
            $token = gpRefreshToken($token);
            gpSaveToken($tokenPath, $token);
        }
        $access = trim((string)($token['token'] ?? ''));
        if ($access === '') {
            apiJson(['ok' => false, 'error' => 'google_access_token_missing'], 500);
        }
        return $access;
    } finally {
        flock($lock, LOCK_UN);
        fclose($lock);
    }
}

// ------------------------- FILE TRANSFER ------------------------------
function gpFilename(array $song): string
{
    $fmt = strtolower(trim((string)($song['format'] ?? '')));
    if ($fmt === '') {
        $fmt = strtolower(pathinfo((string)($song['file_path'] ?? ''), PATHINFO_EXTENSION));
    }
    if (!in_array($fmt, ['gp', 'gp3', 'gp4', 'gp5', 'gp6', 'gp7', 'gp8', 'gpx'], true)) {
        $fmt = 'gp5';
    }
    $title = trim((string)($song['title'] ?? ''));
    if ($title === '') {
        $title = pathinfo((string)($song['file_path'] ?? ''), PATHINFO_FILENAME);
    }
    $title = preg_replace('/[\x00-\x1f\x7f\/\\\\:*?"<>|]+/u', '_', $title) ?: 'tunertools';
    // Prevent excessively long Content-Disposition header lines.
    if (strlen($title) > 140) {
        $title = function_exists('mb_strcut') ? mb_strcut($title, 0, 140, 'UTF-8') : substr($title, 0, 140);
    }
    $title = rtrim($title, ' .');
    return ($title === '' ? 'tunertools' : $title) . '.' . $fmt;
}

function gpFileHeaders(int $status, array $upstreamHeaders, string $filename): void
{
    http_response_code($status === 206 ? 206 : 200);
    header('Content-Type: application/octet-stream');
    $ascii = (string)preg_replace('/[^\x20-\x7e]/', '_', $filename);
    $ascii = str_replace(['"', '\\'], '_', $ascii);
    header('Content-Disposition: inline; filename="' . $ascii . '"; filename*=UTF-8\'\'' . rawurlencode($filename));
    header('Cache-Control: private, max-age=86400');
    header('X-Accel-Buffering: no');
    if (isset($upstreamHeaders['content-length']) && ctype_digit($upstreamHeaders['content-length'])) {
        header('Content-Length: ' . $upstreamHeaders['content-length']);
    }
    if (isset($upstreamHeaders['content-range']) && preg_match('/^bytes [0-9]+-[0-9]+\/[0-9*]+$/i', $upstreamHeaders['content-range'])) {
        header('Content-Range: ' . $upstreamHeaders['content-range']);
    }
    header('Accept-Ranges: bytes');
}

function gpGoogleErrorReason(string $body): string
{
    $json = json_decode($body, true);
    if (!is_array($json)) {
        return '';
    }
    $err = $json['error'] ?? null;
    if (!is_array($err)) {
        return is_string($err) ? $err : '';
    }
    $items = $err['errors'] ?? [];
    if (is_array($items) && isset($items[0]['reason'])) {
        return (string)$items[0]['reason'];
    }
    return (string)($err['status'] ?? '');
}

/** A single cURL attempt. No buffering of successful GP file content. */
function gpStreamOnce(string $fileId, string $accessToken, string $filename, ?string $range): array
{
    $url = 'https://www.googleapis.com/drive/v3/files/' . rawurlencode($fileId)
        . '?alt=media&supportsAllDrives=true';
    $upstreamCode = 0;
    $upstreamHeaders = [];
    $errorBody = '';
    $started = false;
    $bytes = 0;
    $sinceFlush = 0;

    $headers = ['Authorization: Bearer ' . $accessToken, 'Accept: application/octet-stream'];
    if ($range !== null) {
        $headers[] = 'Range: ' . $range;
    }

    $ch = curl_init($url);
    if ($ch === false) {
        return ['status' => 0, 'started' => false, 'ok' => false, 'curl_errno' => -1, 'body' => ''];
    }
    curl_setopt_array($ch, [
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_TIMEOUT => 120,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_RETURNTRANSFER => false,
        CURLOPT_BUFFERSIZE => 65536,
        CURLOPT_HEADERFUNCTION => static function ($handle, string $line) use (&$upstreamCode, &$upstreamHeaders): int {
            $len = strlen($line);
            if (preg_match('/^HTTP\/(?:[0-9.]+|2|3)\s+([0-9]{3})/i', trim($line), $m)) {
                $upstreamCode = (int)$m[1];
                $upstreamHeaders = []; // Final response headers override any redirects.
            } elseif (strpos($line, ':') !== false) {
                [$k, $v] = explode(':', $line, 2);
                $k = strtolower(trim($k));
                if (in_array($k, ['content-length', 'content-range', 'retry-after'], true)) {
                    $upstreamHeaders[$k] = trim($v);
                }
            }
            return $len;
        },
        CURLOPT_WRITEFUNCTION => static function ($handle, string $chunk) use (
            &$upstreamCode, &$upstreamHeaders, &$errorBody, &$started, &$bytes, &$sinceFlush, $filename
        ): int {
            $len = strlen($chunk);
            if ($upstreamCode === 200 || $upstreamCode === 206) {
                if (!$started) {
                    gpFileHeaders($upstreamCode, $upstreamHeaders, $filename);
                    $started = true;
                }
                echo $chunk;
                $bytes += $len;
                $sinceFlush += $len;
                if ($sinceFlush >= 131072) {
                    @flush();
                    $sinceFlush = 0;
                }
                // Stop fetching when the Android client disconnects.
                return connection_aborted() ? 0 : $len;
            }
            if (strlen($errorBody) < 32768) {
                $errorBody .= substr($chunk, 0, 32768 - strlen($errorBody));
            }
            return $len;
        },
    ]);
    $ok = curl_exec($ch);
    $curlErr = curl_errno($ch);
    $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    if ($started) {
        @flush();
    }
    return [
        'status' => $httpCode,
        'started' => $started,
        'ok' => $ok !== false && $curlErr === 0,
        'curl_errno' => $curlErr,
        'bytes' => $bytes,
        'body' => $errorBody,
        'retry_after' => (string)($upstreamHeaders['retry-after'] ?? ''),
        'headers' => $upstreamHeaders,
    ];
}

function gpStreamFile(string $fileId, string $tokenPath, string $filename): void
{
    if (!function_exists('curl_init')) {
        apiJson(['ok' => false, 'error' => 'php_curl_not_available'], 503);
    }
    $range = null;
    if (isset($_SERVER['HTTP_RANGE'])) {
        $value = trim((string)$_SERVER['HTTP_RANGE']);
        if (!preg_match('/^bytes=([0-9]*)-([0-9]*)$/', $value, $m) || ($m[1] === '' && $m[2] === '')) {
            apiJson(['ok' => false, 'error' => 'invalid_range'], 416);
        }
        $range = $value;
    }

    $accessToken = gpGetAccessToken($tokenPath);
    @ini_set('zlib.output_compression', '0');
    @ini_set('output_buffering', '0');
    @set_time_limit(0);
    while (ob_get_level() > 0) {
        @ob_end_clean();
    }

    $refreshedOnce = false;
    $retryNumber = 0;
    for ($attempt = 1; $attempt <= 4; $attempt++) {
        $result = gpStreamOnce($fileId, $accessToken, $filename, $range);
        $status = $result['status'];
        if ($result['started']) {
            // Do not retry after sending even a partial file: that would corrupt GP bytes.
            if (!$result['ok']) {
                apiLog('Drive stream interrupted after ' . $result['bytes'] . ' bytes, HTTP ' . $status);
            }
            exit;
        }
        if ($result['ok'] && ($status === 200 || $status === 206)) {
            gpFileHeaders($status, $result['headers'], $filename);
            exit; // Valid zero-byte file.
        }
        if ($status === 401 && !$refreshedOnce) {
            $accessToken = gpGetAccessToken($tokenPath, true, $accessToken);
            $refreshedOnce = true;
            continue;
        }
        $reason = gpGoogleErrorReason((string)$result['body']);
        $rateLimited = ($status === 403 && in_array($reason,
            ['rateLimitExceeded', 'userRateLimitExceeded', 'dailyLimitExceeded'], true));
        $transient = ($status === 429 || $status === 500 || $status === 502 ||
            $status === 503 || $status === 504 || $rateLimited || $status === 0);
        if (!$transient || $attempt === 4) {
            apiLog('Drive download failed: upstream HTTP ' . $status . ', reason=' . $reason .
                ', curl=' . $result['curl_errno']);
            apiJson([
                'ok' => false,
                'error' => 'google_drive_download_failed',
                'http_code' => $status,
                'google_error' => $reason,
                'retryable' => $transient,
            ], 502);
        }
        ++$retryNumber;
        $retryAfter = (int)($result['retry_after'] ?? 0);
        $delayMs = min(4000, max($retryAfter * 1000, (1000 * (1 << ($retryNumber - 1))) + random_int(0, 350)));
        usleep($delayMs * 1000);
    }
    apiJson(['ok' => false, 'error' => 'google_drive_download_failed'], 502);
}

// ----------------- CATEGORY LISTING (SAME PHP) -------------------------
/** Optional small catalog generated by tt_gp_sections.py; never exposes OAuth tokens. */
function gpSectionsCatalogOptional(string $path): ?array
{
    if (!is_file($path)) return null;
    if (!is_readable($path) || @filesize($path) > 8 * 1024 * 1024) {
        apiLog('Section manifest unreadable/too large');
        return null;
    }
    $raw = @file_get_contents($path);
    $json = is_string($raw) ? json_decode($raw, true) : null;
    if (!is_array($json) || !isset($json['entries']) || !is_array($json['entries'])) {
        apiLog('Section manifest invalid');
        return null;
    }
    return $json['entries'];
}

/** Read only short, indexed file_path prefixes from the existing SQLite.
 *  1.64M Songsterr rows are never loaded into memory.
 *  db file_path values normally begin with tab/.
 */
function gpSectionsFromDb(PDO $pdo): array
{
    $roots = [
        'nhac_viet' => [
            ['tab/vietnam/', ''], ['tab/Vietnam/', ''],
            ['vietnam/', ''], ['Vietnam/', ''],
            ['tab/trinhcongson/', 'Trinh Cong Son/'],
            ['tab/TrinhCongSon/', 'Trinh Cong Son/'],
            ['trinhcongson/', 'Trinh Cong Son/'],
        ],
        'dieu_nhac' => [
            ['tab/dieu_dem_nhac/', ''], ['tab/Dieu_Dem_Nhac/', ''],
            ['dieu_dem_nhac/', ''],
            ['tab/dieu_nhac/', ''], ['dieu_nhac/', ''],
        ],
    ];
    $entries = [];
    // GLOB 'literal-prefix*' can use a B-tree index on file_path.
    $sql = "SELECT id, title, artist, format, file_path, drive_file_id
            FROM songs
            WHERE file_path GLOB :pattern
              AND drive_file_id IS NOT NULL AND drive_file_id <> ''
            LIMIT 10000";
    $stmt = $pdo->prepare($sql);
    foreach ($roots as $section => $prefixes) {
        foreach ($prefixes as [$prefix, $logicalPrefix]) {
            $stmt->bindValue(':pattern', $prefix . '*', PDO::PARAM_STR);
            $stmt->execute();
            while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
                $path = str_replace('\\', '/', (string)$row['file_path']);
                if (strncmp($path, $prefix, strlen($prefix)) !== 0) continue;
                $relative = substr($path, strlen($prefix));
                if ($relative === '' || substr($relative, -1) === '/') continue;
                $logical = $logicalPrefix . $relative;
                $key = md5($section . '|' . gpSectionsLower($path));
                $entries[] = [
                    'key' => $key,
                    'section' => $section,
                    'logical_path' => $logical,
                    'local_relative_path' => $path,
                    'song_id' => (string)$row['id'],
                    'from_db' => true,
                    'title' => (string)($row['title'] ?: pathinfo($path, PATHINFO_FILENAME)),
                    'artist' => (string)($row['artist'] ?? ''),
                    'format' => (string)($row['format'] ?? pathinfo($path, PATHINFO_EXTENSION)),
                    'drive_file_id' => (string)$row['drive_file_id'],
                ];
            }
            $stmt->closeCursor();
        }
    }
    return $entries;
}

/** Canonicalize the same physical local GP path across bulk SQLite and manifest.
 *  SQLite paths often use tab/vietnam/x.gp5 while manifest uses vietnam/x.gp5.
 *  Without this, the same song is shown twice after installing gp_sections.json.
 */
function gpSectionsCanonicalPathKey(string $section, string $localPath): string
{
    $path = gpSectionsLower(trim(str_replace('\\', '/', $localPath), '/'));
    if (strncmp($path, 'tab/', 4) === 0) {
        $path = substr($path, 4);
    }
    return $section . '|' . $path;
}

/** Manifest may add files not present in SQLite without duplicating those already there. */
function gpSectionsMerged(PDO $pdo, ?array $manifest): array
{
    $entries = gpSectionsFromDb($pdo);
    if ($manifest === null) return $entries;
    $map = [];
    foreach ($entries as $item) {
        $key = gpSectionsCanonicalPathKey(
            (string)$item['section'], (string)$item['local_relative_path']
        );
        $map[$key] = $item;
    }
    foreach ($manifest as $item) {
        if (!is_array($item)) continue;
        if (!in_array((string)($item['section'] ?? ''), ['nhac_viet','dieu_nhac'], true)) continue;
        if (!preg_match('/^[a-f0-9]{32}$/D', (string)($item['key'] ?? ''))) continue;
        if (!preg_match('/^[A-Za-z0-9_-]{5,200}$/D', (string)($item['drive_file_id'] ?? ''))) continue;
        $key = gpSectionsCanonicalPathKey(
            (string)$item['section'], (string)($item['local_relative_path'] ?? '')
        );
        if (isset($map[$key]) &&
            $map[$key]['drive_file_id'] === (string)$item['drive_file_id']) {
            // Prefer the indexed SQLite row and action=file URL when both match.
            continue;
        }
        $map[$key] = $item;
    }
    return array_values($map);
}

/** Return list of international songs compatible with section=quoc_te clients. */
function gpInternationalSection(PDO $pdo, string $q, string $publicUrl): array
{
    $limit = 50;
    $stmt = null;
    $query = trim($q);
    $fts = gpFtsQuery($query);
    if ($fts !== '') {
        $stmt = $pdo->prepare(
            "SELECT s.id, s.title, s.artist, s.format, s.file_path
             FROM songs_fts
             JOIN songs s ON s.rowid = songs_fts.rowid
             WHERE songs_fts MATCH :match
               AND s.drive_file_id IS NOT NULL AND s.drive_file_id <> ''
             ORDER BY bm25(songs_fts, 6.0, 3.0, 0.4, 0.1) LIMIT 100"
        );
        $stmt->bindValue(':match', $fts, PDO::PARAM_STR);
    } else {
        // Empty search: a sample rather than an expensive random/sort query.
        $stmt = $pdo->prepare(
            "SELECT id,title,artist,format,file_path FROM songs
             WHERE drive_file_id IS NOT NULL AND drive_file_id <> ''
             LIMIT 100"
        );
    }
    $stmt->execute();
    $data = [];
    while ($r = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $path = gpSectionsLower((string)($r['file_path'] ?? ''));
        $excluded = false;
        foreach (['tab/vietnam/', 'tab/trinhcongson/', 'tab/dieu_dem_nhac/', 'tab/dieu_nhac/',
                  'vietnam/', 'trinhcongson/', 'dieu_dem_nhac/', 'dieu_nhac/'] as $prefix) {
            if (strpos($path, $prefix) === 0) { $excluded = true; break; }
        }
        if ($excluded) continue;
        $data[] = [
            'id' => (string)$r['id'],
            'title' => (string)($r['title'] ?? ''),
            'artist' => (string)($r['artist'] ?? ''),
            'format' => (string)($r['format'] ?? ''),
            'item_type' => 'file',
            'library_section' => 'quoc_te',
            'relative_path' => (string)($r['file_path'] ?? ''),
            'file_url' => $publicUrl . '?action=file&id=' . rawurlencode((string)$r['id']),
        ];
        if (count($data) >= $limit) break;
    }
    return ['success' => true, 'data' => $data,
        'meta' => ['section' => 'quoc_te', 'query' => $q, 'count' => count($data)]];
}

function gpSectionsNormalize(string $value): string
{
    $value = trim(str_replace('\\', '/', $value), '/ ');
    if (strpos($value, "\0") !== false || preg_match('~(?:^|/)\\.\\.(?:/|$)~', $value)) {
        apiJson(['ok' => false, 'error' => 'invalid_folder'], 400);
    }
    return preg_replace('~/+~', '/', $value) ?? '';
}

function gpSectionsLower(string $text): string
{
    return function_exists('mb_strtolower') ? mb_strtolower($text, 'UTF-8') : strtolower($text);
}

function gpSectionsMatch(string $haystack, string $needle): bool
{
    if ($needle === '') return true;
    return strpos(gpSectionsLower($haystack), gpSectionsLower($needle)) !== false;
}

function gpSectionFileItem(array $item, string $publicUrl): array
{
    $name = (string)($item['title'] ?? pathinfo((string)($item['logical_path'] ?? ''), PATHINFO_FILENAME));
    $logical = gpSectionsNormalize((string)($item['logical_path'] ?? ''));
    $parent = dirname($logical);
    if ($parent === '.') $parent = '';
    return [
        'id' => (string)($item['song_id'] ?: $item['key']),
        'title' => $name,
        'artist' => (string)($item['artist'] ?? ''),
        'file_url' => !empty($item['from_db'])
            ? $publicUrl . '?action=file&id=' . rawurlencode((string)$item['song_id'])
            : $publicUrl . '?action=section_file&key=' . rawurlencode((string)$item['key']),
        'format' => (string)($item['format'] ?? 'gp5'),
        'item_type' => 'file',
        'library_section' => (string)$item['section'],
        'relative_path' => (string)($item['local_relative_path'] ?? ''),
        'folder_path' => $parent,
        'parent_path' => $parent === '' ? '' : (dirname($parent) === '.' ? '' : dirname($parent)),
    ];
}

function gpSectionsFolderItem(string $section, string $name, string $folderPath, string $parent, string $rhythm = ''): array
{
    return [
        'id' => 'folder:' . $section . ':' . hash('sha256', $folderPath),
        'title' => $name,
        'artist' => $section === 'dieu_nhac' ? 'Điệu nhạc' : '',
        'file_url' => '',
        'format' => 'folder',
        'item_type' => 'folder',
        'library_section' => $section,
        'folder_path' => $folderPath,
        'parent_path' => $parent,
        'rhythm' => $rhythm,
    ];
}

function gpSectionsList(array $entries, string $section, string $folder, string $rhythm,
                        string $query, string $publicUrl): array
{
    $section = strtolower(trim($section));
    if (in_array($section, ['vietnam', 'vietnamese'], true)) $section = 'nhac_viet';
    if (in_array($section, ['rhythm', 'rhythms'], true)) $section = 'dieu_nhac';
    if (!in_array($section, ['nhac_viet', 'dieu_nhac'], true)) {
        apiJson(['ok' => false, 'error' => 'unknown_section'], 400);
    }
    $folder = gpSectionsNormalize($folder);
    $rhythm = gpSectionsNormalize($rhythm);
    $query = trim($query);
    $visible = [];
    $folders = [];
    foreach ($entries as $entry) {
        if (!is_array($entry) || (string)($entry['section'] ?? '') !== $section) continue;
        if (!preg_match('/^[a-f0-9]{32}$/', (string)($entry['key'] ?? ''))) continue;
        if (!preg_match('/^[A-Za-z0-9_-]{10,180}$/', (string)($entry['drive_file_id'] ?? ''))) continue;
        $path = gpSectionsNormalize((string)($entry['logical_path'] ?? ''));
        if ($path === '') continue;
        $name = (string)($entry['title'] ?? '');
        if ($section === 'nhac_viet') {
            $prefix = $folder === '' ? '' : $folder . '/';
            if ($prefix !== '' && strncasecmp($path, $prefix, strlen($prefix)) !== 0) continue;
            $rest = substr($path, strlen($prefix));
            if ($query !== '') {
                if (gpSectionsMatch($name . ' ' . (string)($entry['artist'] ?? '') . ' ' . $rest, $query)) {
                    $visible[] = gpSectionFileItem($entry, $publicUrl);
                }
            } elseif (strpos($rest, '/') !== false) {
                $child = explode('/', $rest, 2)[0];
                $childPath = ($folder === '' ? '' : $folder . '/') . $child;
                $folders[$childPath] = gpSectionsFolderItem($section, $child, $childPath, $folder);
            } else {
                $visible[] = gpSectionFileItem($entry, $publicUrl);
            }
        } else {
            // A rhythm is the first directory under dieu_dem_nhac.
            $segments = explode('/', $path, 2);
            $first = count($segments) > 1 ? $segments[0] : '';
            if ($rhythm === '') {
                if ($first !== '' && ($query === '' || gpSectionsMatch($first, $query))) {
                    $folders[$first] = gpSectionsFolderItem($section, $first, $first, '', $first);
                } elseif (gpSectionsMatch($name, $query)) {
                    $visible[] = gpSectionFileItem($entry, $publicUrl);
                }
            } elseif (strcasecmp($first, $rhythm) === 0) {
                if (gpSectionsMatch($name . ' ' . (string)($entry['artist'] ?? ''), $query)) {
                    $visible[] = gpSectionFileItem($entry, $publicUrl);
                }
            }
        }
    }
    $items = array_merge(array_values($folders), $visible);
    usort($items, static function ($a, $b) {
        if ($a['item_type'] !== $b['item_type']) return $a['item_type'] === 'folder' ? -1 : 1;
        return strcasecmp($a['title'], $b['title']);
    });
    return [
        'data' => array_slice($items, 0, 500),
        'meta' => [
            'section' => $section,
            'view' => $section === 'nhac_viet' ? 'folder' : ($rhythm === '' ? 'folders' : 'files'),
            'current_folder' => $folder,
            'parent_folder' => ($folder === '' || dirname($folder) === '.') ? '' : dirname($folder),
            'rhythm' => $rhythm,
            'query' => $query,
            'count' => min(count($items), 500),
            'truncated' => count($items) > 500,
            'supported_formats' => ['gp', 'gp3', 'gp4', 'gp5', 'gpx', 'gp6', 'gp7', 'gp8'],
        ],
    ];
}


// ------------------------------ ROUTES --------------------------------
$action = strtolower(trim((string)($_GET['action'] ?? 'search')));

// Additional section endpoints use the SAME PHP file (no api_gp_sections.php).
// When section is supplied without an action, accept it as a sections request.
if ($action === 'sections' || ($action === 'search' && isset($_GET['section']) && trim((string)$_GET['section']) !== '')) {
    $section = strtolower(trim((string)($_GET['section'] ?? '')));
    if (in_array($section, ['vietnam', 'vietnamese'], true)) $section = 'nhac_viet';
    if (in_array($section, ['rhythm', 'rhythms'], true)) $section = 'dieu_nhac';
    $q = trim((string)($_GET['q'] ?? ''));
    if (strlen($q) > $MAX_QUERY_BYTES) apiJson(['ok' => false, 'error' => 'query_too_long'], 400);
    $pdo = gpDb($DB_PATH);
    if (in_array($section, ['quoc_te', 'international', 'global'], true)) {
        try { $response = gpInternationalSection($pdo, $q, $PUBLIC_API_URL); }
        catch (Throwable $e) { apiLog('International section error: ' . $e->getMessage());
            apiJson(['ok' => false, 'error' => 'section_search_failed'], 503); }
    } else {
        try {
            $entries = gpSectionsMerged($pdo, gpSectionsCatalogOptional($SECTION_MANIFEST_PATH));
            $response = gpSectionsList($entries, $section,
                (string)($_GET['folder'] ?? ''), (string)($_GET['rhythm'] ?? ''),
                $q, $PUBLIC_API_URL);
            $response['success'] = true;
        } catch (Throwable $e) {
            apiLog('Sections query error: ' . $e->getMessage());
            apiJson(['ok' => false, 'error' => 'sections_query_failed'], 503);
        }
    }
    $pdo = null;
    header('Cache-Control: public, max-age=60');
    apiJson($response);
}

if ($action === 'section_file') {
    $key = strtolower(trim((string)($_GET['key'] ?? '')));
    if (!preg_match('/^[a-f0-9]{32}$/D', $key)) {
        apiJson(['ok' => false, 'error' => 'invalid_section_file_key'], 400);
    }
    $manifest = gpSectionsCatalogOptional($SECTION_MANIFEST_PATH);
    if ($manifest === null) apiJson(['ok' => false, 'error' => 'section_manifest_not_available'], 404);
    foreach ($manifest as $entry) {
        if (!is_array($entry) || (string)($entry['key'] ?? '') !== $key) continue;
        $driveId = trim((string)($entry['drive_file_id'] ?? ''));
        if (!preg_match('/^[A-Za-z0-9_-]{5,200}$/D', $driveId)) {
            apiJson(['ok' => false, 'error' => 'section_file_not_ready'], 404);
        }
        gpStreamFile($driveId, $TOKEN_PATH, gpFilename([
            'title' => (string)($entry['title'] ?? ''),
            'file_path' => (string)($entry['local_relative_path'] ?? ''),
            'format' => (string)($entry['format'] ?? ''),
        ]));
    }
    apiJson(['ok' => false, 'error' => 'section_file_not_found'], 404);
}

if ($action === 'health') {
    $pdo = gpDb($DB_PATH);
    try {
        $total = (int)$pdo->query('SELECT COUNT(*) FROM songs')->fetchColumn();
        $ready = (int)$pdo->query("SELECT COUNT(*) FROM songs WHERE drive_file_id IS NOT NULL AND drive_file_id <> ''")->fetchColumn();
    } catch (Throwable $e) {
        apiLog('Health SQL failure: ' . $e->getMessage());
        apiJson(['ok' => false, 'error' => 'health_query_failed'], 503);
    }
    apiJson([
        'ok' => true,
        'service' => 'TunerTools Drive Backup API',
        'database' => basename($DB_PATH),
        'songs_total' => $total,
        'drive_ready' => $ready,
        'drive_pending' => max(0, $total - $ready),
        'token_file_exists' => is_file($TOKEN_PATH),
        'pdo_sqlite' => extension_loaded('pdo_sqlite'),
        'curl' => function_exists('curl_init'),
    ]);
}

if ($action === 'search') {
    $q = trim((string)($_GET['q'] ?? ''));
    if (strlen($q) > $MAX_QUERY_BYTES) {
        apiJson(['ok' => false, 'error' => 'query_too_long'], 400);
    }
    $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : $DEFAULT_LIMIT;
    $limit = max(1, min($MAX_LIMIT, $limit));
    $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
    $offset = max(0, min($MAX_OFFSET, $offset));
    $fts = gpFtsQuery($q);
    if ($fts === '') {
        apiJson(['ok' => true, 'query' => $q, 'count' => 0, 'results' => []]);
    }
    $pdo = gpDb($DB_PATH);
    try {
        $stmt = $pdo->prepare(
            "SELECT s.id, s.title, s.artist, s.file_path, s.format
             FROM songs_fts
             JOIN songs AS s ON s.rowid = songs_fts.rowid
             WHERE songs_fts MATCH :match
               AND s.drive_file_id IS NOT NULL AND s.drive_file_id <> ''
             ORDER BY bm25(songs_fts, 6.0, 3.0, 0.4, 0.1)
             LIMIT :lim OFFSET :off"
        );
        $stmt->bindValue(':match', $fts, PDO::PARAM_STR);
        $stmt->bindValue(':lim', $limit + 1, PDO::PARAM_INT);
        $stmt->bindValue(':off', $offset, PDO::PARAM_INT);
        $stmt->execute();
        $rows = $stmt->fetchAll();
    } catch (Throwable $e) {
        apiLog('FTS5 query failed: ' . $e->getMessage());
        if (!$ALLOW_LIKE_ON_FTS_ERROR) {
            apiJson(['ok' => false, 'error' => 'fts5_search_unavailable'], 503);
        }
        try {
            $rows = gpLikeFallback($pdo, $q, $limit + 1, $offset);
        } catch (Throwable $e2) {
            apiLog('LIKE fallback failed: ' . $e2->getMessage());
            apiJson(['ok' => false, 'error' => 'search_query_failed'], 503);
        }
    }
    $hasMore = count($rows) > $limit;
    if ($hasMore) {
        array_pop($rows);
    }
    foreach ($rows as &$row) {
        $row['file_url'] = $PUBLIC_API_URL . '?action=file&id=' . rawurlencode((string)$row['id']);
    }
    unset($row);
    header('Cache-Control: public, max-age=45');
    apiJson([
        'ok' => true,
        'query' => $q,
        'count' => count($rows),
        'results' => $rows,
        'offset' => $offset,
        'has_more' => $hasMore,
    ]);
}

if ($action === 'file') {
    // Keep Song IDs as strings: Songsterr IDs can exceed PHP's integer limit.
    $id = trim((string)($_GET['id'] ?? ''));
    if ($id === '' || strlen($id) > 150) {
        apiJson(['ok' => false, 'error' => 'missing_song_id'], 400);
    }
    $pdo = gpDb($DB_PATH);
    try {
        $stmt = $pdo->prepare(
            'SELECT id, title, artist, file_path, format, drive_file_id FROM songs WHERE id = :id LIMIT 1'
        );
        $stmt->execute([':id' => $id]);
        $song = $stmt->fetch();
    } catch (Throwable $e) {
        apiLog('Song lookup failed: ' . $e->getMessage());
        apiJson(['ok' => false, 'error' => 'song_lookup_failed'], 503);
    }
    $pdo = null; // Release SQLite before a potentially slow Google request.
    if (!$song) {
        apiJson(['ok' => false, 'error' => 'song_not_found'], 404);
    }
    $fileId = trim((string)($song['drive_file_id'] ?? ''));
    if ($fileId === '') {
        apiJson(['ok' => false, 'error' => 'drive_file_not_ready', 'id' => $id], 404);
    }
    if (!preg_match('/^[A-Za-z0-9_-]{5,200}$/D', $fileId)) {
        apiJson(['ok' => false, 'error' => 'invalid_drive_file_id'], 500);
    }
    gpStreamFile($fileId, $TOKEN_PATH, gpFilename($song));
}

apiJson([
    'ok' => false,
    'error' => 'unknown_action',
    'allowed_actions' => ['health', 'search', 'file', 'sections', 'section_file'],
], 400);
