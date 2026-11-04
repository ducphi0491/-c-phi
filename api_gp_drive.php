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
 *
 * Security: Keep the DB and drive_token.json outside webroot where possible.
 * Set TUNERTOOLS_GP_DB_PATH and TUNERTOOLS_GP_TOKEN_PATH in hosting environment
 * if those files are elsewhere. Never expose OAuth credentials over HTTP.
 */

// --------------------------- CONFIG ----------------------------------
// Unchanged database name, endpoints, and Android JSON response shape.
$DB_PATH = getenv('TUNERTOOLS_GP_DB_PATH') ?: __DIR__ . '/songsterr_drive_test.db';
$TOKEN_PATH = getenv('TUNERTOOLS_GP_TOKEN_PATH') ?: __DIR__ . '/drive_token.json';

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

// ------------------------------ ROUTES --------------------------------
$action = strtolower(trim((string)($_GET['action'] ?? 'search')));

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
    'allowed_actions' => ['health', 'search', 'file'],
], 400);
