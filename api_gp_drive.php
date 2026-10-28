<?php
declare(strict_types=1);

/*
 * TunerTools - Google Drive backup API (TEST 5000 songs)
 *
 * Files expected:
 *   api_gp_drive.php
 *   songsterr_drive_test.db
 *   drive_token.json
 *
 * Endpoints:
 *   ?action=health
 *   ?action=search&q=hotel&limit=50
 *   ?action=file&id=6635943603902029564
 *
 * IMPORTANT:
 * - Keep drive_token.json private. Prefer placing it outside public_html.
 * - This script never stores GP files on the PHP server.
 * - GP bytes are streamed directly: Google Drive -> PHP -> client.
 */

// ---------------------------------------------------------------------
// CONFIG
// ---------------------------------------------------------------------

$DB_PATH = __DIR__ . DIRECTORY_SEPARATOR . 'songsterr_drive_test.db';
$TOKEN_PATH = __DIR__ . DIRECTORY_SEPARATOR . 'drive_token.json';

$DEFAULT_LIMIT = 50;
$MAX_LIMIT = 100;

// ---------------------------------------------------------------------
// BASIC HEADERS
// ---------------------------------------------------------------------

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Range');
header('Cache-Control: no-store');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// ---------------------------------------------------------------------
// HELPERS
// ---------------------------------------------------------------------

function jsonResponse(array $data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
    );

    exit;
}

function openDb(string $dbPath): PDO
{
    if (!is_file($dbPath)) {
        jsonResponse([
            'ok' => false,
            'error' => 'database_not_found',
            'path' => basename($dbPath),
        ], 500);
    }

    try {
        $pdo = new PDO('sqlite:' . $dbPath);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        return $pdo;
    } catch (Throwable $e) {
        jsonResponse([
            'ok' => false,
            'error' => 'sqlite_open_failed',
            'message' => $e->getMessage(),
        ], 500);
    }
}

function makeBaseUrl(): string
{
    $https = !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off';
    $scheme = $https ? 'https' : 'http';

    if (
        isset($_SERVER['HTTP_X_FORWARDED_PROTO']) &&
        strtolower((string)$_SERVER['HTTP_X_FORWARDED_PROTO']) === 'https'
    ) {
        $scheme = 'https';
    }

    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $script = $_SERVER['SCRIPT_NAME'] ?? '/api_gp_drive.php';

    return $scheme . '://' . $host . $script;
}

function buildFtsQuery(string $q): string
{
    $q = trim($q);

    if ($q === '') {
        return '';
    }

    // Keep Unicode letters and numbers only.
    $clean = preg_replace('/[^\p{L}\p{N}]+/u', ' ', $q);

    if ($clean === null) {
        $clean = $q;
    }

    $parts = preg_split(
        '/\s+/u',
        trim($clean),
        -1,
        PREG_SPLIT_NO_EMPTY
    );

    if (!$parts) {
        return '';
    }

    $terms = [];

    foreach ($parts as $part) {
        // Prefix search: hotel -> hotel*
        $terms[] = $part . '*';
    }

    return implode(' AND ', $terms);
}

function loadTokenFile(string $tokenPath): array
{
    if (!is_file($tokenPath)) {
        jsonResponse([
            'ok' => false,
            'error' => 'drive_token_not_found',
            'message' => 'drive_token.json is missing on the PHP server.',
        ], 500);
    }

    $raw = file_get_contents($tokenPath);

    if ($raw === false) {
        jsonResponse([
            'ok' => false,
            'error' => 'drive_token_read_failed',
        ], 500);
    }

    $token = json_decode($raw, true);

    if (!is_array($token)) {
        jsonResponse([
            'ok' => false,
            'error' => 'drive_token_invalid_json',
        ], 500);
    }

    return $token;
}

function tokenNeedsRefresh(array $token): bool
{
    if (empty($token['token'])) {
        return true;
    }

    if (empty($token['expiry'])) {
        return false;
    }

    $expiry = strtotime((string)$token['expiry']);

    if ($expiry === false) {
        return false;
    }

    return $expiry <= (time() + 60);
}

function refreshGoogleToken(array $token, string $tokenPath): array
{
    $refreshToken = $token['refresh_token'] ?? '';
    $clientId = $token['client_id'] ?? '';
    $clientSecret = $token['client_secret'] ?? '';
    $tokenUri = $token['token_uri'] ?? 'https://oauth2.googleapis.com/token';

    if ($refreshToken === '' || $clientId === '' || $clientSecret === '') {
        jsonResponse([
            'ok' => false,
            'error' => 'refresh_token_data_missing',
            'message' => 'drive_token.json does not contain refresh credentials.',
        ], 500);
    }

    if (!function_exists('curl_init')) {
        jsonResponse([
            'ok' => false,
            'error' => 'php_curl_not_available',
        ], 500);
    }

    $ch = curl_init($tokenUri);

    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'client_id' => $clientId,
            'client_secret' => $clientSecret,
            'refresh_token' => $refreshToken,
            'grant_type' => 'refresh_token',
        ]),
        CURLOPT_HTTPHEADER => [
            'Content-Type: application/x-www-form-urlencoded',
        ],
        CURLOPT_CONNECTTIMEOUT => 15,
        CURLOPT_TIMEOUT => 30,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);

    $body = curl_exec($ch);
    $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);

    curl_close($ch);

    if ($body === false || $httpCode < 200 || $httpCode >= 300) {
        jsonResponse([
            'ok' => false,
            'error' => 'google_token_refresh_failed',
            'http_code' => $httpCode,
            'curl_error' => $curlError,
            'google_response' => $body,
        ], 502);
    }

    $result = json_decode((string)$body, true);

    if (!is_array($result) || empty($result['access_token'])) {
        jsonResponse([
            'ok' => false,
            'error' => 'google_token_refresh_invalid_response',
        ], 502);
    }

    $token['token'] = $result['access_token'];

    if (!empty($result['expires_in'])) {
        $token['expiry'] = gmdate(
            'Y-m-d\TH:i:s\Z',
            time() + (int)$result['expires_in']
        );
    }

    // Preserve refresh_token/client_id/client_secret from the old file.
    $encoded = json_encode(
        $token,
        JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT
    );

    if ($encoded !== false) {
        // Atomic-ish replacement to reduce the chance of a partial token file.
        $tmp = $tokenPath . '.tmp';

        if (file_put_contents($tmp, $encoded, LOCK_EX) !== false) {
            @rename($tmp, $tokenPath);
        }
    }

    return $token;
}

function getGoogleAccessToken(string $tokenPath): string
{
    $token = loadTokenFile($tokenPath);

    if (tokenNeedsRefresh($token)) {
        $token = refreshGoogleToken($token, $tokenPath);
    }

    $accessToken = $token['token'] ?? '';

    if ($accessToken === '') {
        jsonResponse([
            'ok' => false,
            'error' => 'google_access_token_missing',
        ], 500);
    }

    return $accessToken;
}

function safeDownloadFilename(
    string $title,
    string $format,
    string $filePath
): string {
    $format = strtolower(trim($format));

    if ($format === '') {
        $format = strtolower(pathinfo($filePath, PATHINFO_EXTENSION));
    }

    if ($format === '') {
        $format = 'gp5';
    }

    $name = trim($title);

    if ($name === '') {
        $name = pathinfo($filePath, PATHINFO_FILENAME);
    }

    $name = preg_replace('/[\/\\\\:*?"<>|]+/u', '_', $name);

    if ($name === null || trim($name) === '') {
        $name = 'tunertools';
    }

    return trim($name) . '.' . $format;
}

function streamDriveFile(
    string $driveFileId,
    string $accessToken,
    string $downloadName
): void {
    if (!function_exists('curl_init')) {
        jsonResponse([
            'ok' => false,
            'error' => 'php_curl_not_available',
        ], 500);
    }

    // Prevent PHP from buffering the whole GP file.
    @ini_set('zlib.output_compression', '0');
    @ini_set('output_buffering', '0');
    @set_time_limit(0);

    while (ob_get_level() > 0) {
        @ob_end_clean();
    }

    header('Content-Type: application/octet-stream');
    header(
        'Content-Disposition: inline; filename="' .
        addcslashes($downloadName, '"\\') .
        '"'
    );
    header('X-Accel-Buffering: no');

    $url =
        'https://www.googleapis.com/drive/v3/files/' .
        rawurlencode($driveFileId) .
        '?alt=media&supportsAllDrives=true';

    $driveHttpCode = 0;
    $errorBody = '';

    $ch = curl_init($url);

    curl_setopt_array($ch, [
        CURLOPT_FOLLOWLOCATION => true,
        CURLOPT_CONNECTTIMEOUT => 15,
        CURLOPT_TIMEOUT => 120,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_HTTPHEADER => [
            'Authorization: Bearer ' . $accessToken,
        ],

        CURLOPT_HEADERFUNCTION => function ($ch, string $header) use (&$driveHttpCode): int {
            $len = strlen($header);

            if (preg_match('/^HTTP\/\S+\s+(\d{3})/i', trim($header), $m)) {
                $driveHttpCode = (int)$m[1];
            }

            if (
                stripos($header, 'Content-Length:') === 0 &&
                $driveHttpCode >= 200 &&
                $driveHttpCode < 300
            ) {
                $value = trim(substr($header, strlen('Content-Length:')));

                if ($value !== '') {
                    header('Content-Length: ' . $value);
                }
            }

            return $len;
        },

        CURLOPT_WRITEFUNCTION => function ($ch, string $chunk) use (&$driveHttpCode, &$errorBody): int {
            $len = strlen($chunk);

            if ($driveHttpCode >= 200 && $driveHttpCode < 300) {
                echo $chunk;

                if (function_exists('fastcgi_finish_request')) {
                    // Do NOT call fastcgi_finish_request here; it would end the stream.
                }

                @flush();
            } else {
                if (strlen($errorBody) < 65536) {
                    $errorBody .= $chunk;
                }
            }

            return $len;
        },
    ]);

    $ok = curl_exec($ch);
    $curlError = curl_error($ch);
    $finalCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);

    curl_close($ch);

    if (
        $ok === false ||
        $finalCode < 200 ||
        $finalCode >= 300
    ) {
        // If body streaming has not started successfully, return a readable error.
        if (!headers_sent()) {
            jsonResponse([
                'ok' => false,
                'error' => 'google_drive_download_failed',
                'http_code' => $finalCode,
                'curl_error' => $curlError,
                'google_response' => $errorBody,
            ], 502);
        }

        exit;
    }

    exit;
}

// ---------------------------------------------------------------------
// ROUTING
// ---------------------------------------------------------------------

$action = strtolower(trim((string)($_GET['action'] ?? 'search')));

// ---------------------------------------------------------------------
// HEALTH
// ---------------------------------------------------------------------

if ($action === 'health') {
    $pdo = openDb($DB_PATH);

    $total = (int)$pdo->query(
        'SELECT COUNT(*) FROM songs'
    )->fetchColumn();

    $ready = (int)$pdo->query(
        "SELECT COUNT(*) FROM songs
         WHERE drive_file_id IS NOT NULL
           AND TRIM(drive_file_id) <> ''"
    )->fetchColumn();

    jsonResponse([
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

// ---------------------------------------------------------------------
// SEARCH
// ---------------------------------------------------------------------

if ($action === 'search') {
    $q = trim((string)($_GET['q'] ?? ''));

    if ($q === '') {
        jsonResponse([
            'ok' => true,
            'query' => '',
            'count' => 0,
            'results' => [],
        ]);
    }

    $limit = isset($_GET['limit'])
        ? (int)$_GET['limit']
        : $DEFAULT_LIMIT;

    if ($limit < 1) {
        $limit = $DEFAULT_LIMIT;
    }

    if ($limit > $MAX_LIMIT) {
        $limit = $MAX_LIMIT;
    }

    $pdo = openDb($DB_PATH);
    $results = [];

    $ftsQuery = buildFtsQuery($q);

    if ($ftsQuery !== '') {
        try {
            $sql = "
                SELECT
                    s.id,
                    s.title,
                    s.artist,
                    s.file_path,
                    s.format
                FROM songs_fts
                JOIN songs AS s
                    ON s.rowid = songs_fts.rowid
                WHERE songs_fts MATCH :match
                  AND s.drive_file_id IS NOT NULL
                  AND TRIM(s.drive_file_id) <> ''
                ORDER BY bm25(songs_fts)
                LIMIT :limit
            ";

            $stmt = $pdo->prepare($sql);
            $stmt->bindValue(':match', $ftsQuery, PDO::PARAM_STR);
            $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
            $stmt->execute();

            $results = $stmt->fetchAll();
        } catch (Throwable $e) {
            // Fallback below.
            $results = [];
        }
    }

    // Fallback for hosts whose SQLite was built without FTS5
    // or when FTS syntax cannot handle the supplied query.
    if (!$results) {
        $like = '%' . $q . '%';

        $stmt = $pdo->prepare("
            SELECT
                id,
                title,
                artist,
                file_path,
                format
            FROM songs
            WHERE (
                    title LIKE :q
                 OR artist LIKE :q
                 OR file_path LIKE :q
            )
              AND drive_file_id IS NOT NULL
              AND TRIM(drive_file_id) <> ''
            LIMIT :limit
        ");

        $stmt->bindValue(':q', $like, PDO::PARAM_STR);
        $stmt->bindValue(':limit', $limit, PDO::PARAM_INT);
        $stmt->execute();

        $results = $stmt->fetchAll();
    }

    $baseUrl = makeBaseUrl();

    foreach ($results as &$row) {
        $row['file_url'] =
            $baseUrl .
            '?action=file&id=' .
            rawurlencode((string)$row['id']);
    }
    unset($row);

    jsonResponse([
        'ok' => true,
        'query' => $q,
        'count' => count($results),
        'results' => $results,
    ]);
}

// ---------------------------------------------------------------------
// STREAM GP FILE FROM GOOGLE DRIVE
// ---------------------------------------------------------------------

if ($action === 'file') {
    $id = trim((string)($_GET['id'] ?? ''));

    if ($id === '') {
        jsonResponse([
            'ok' => false,
            'error' => 'missing_song_id',
        ], 400);
    }

    $pdo = openDb($DB_PATH);

    $stmt = $pdo->prepare("
        SELECT
            id,
            title,
            artist,
            file_path,
            format,
            drive_file_id
        FROM songs
        WHERE id = :id
        LIMIT 1
    ");

    $stmt->execute([':id' => $id]);
    $song = $stmt->fetch();

    if (!$song) {
        jsonResponse([
            'ok' => false,
            'error' => 'song_not_found',
        ], 404);
    }

    $driveFileId = trim((string)($song['drive_file_id'] ?? ''));

    if ($driveFileId === '') {
        jsonResponse([
            'ok' => false,
            'error' => 'drive_file_not_ready',
            'id' => $id,
        ], 404);
    }

    $accessToken = getGoogleAccessToken($TOKEN_PATH);

    $downloadName = safeDownloadFilename(
        (string)($song['title'] ?? ''),
        (string)($song['format'] ?? ''),
        (string)($song['file_path'] ?? '')
    );

    streamDriveFile(
        $driveFileId,
        $accessToken,
        $downloadName
    );
}

// ---------------------------------------------------------------------
// UNKNOWN ACTION
// ---------------------------------------------------------------------

jsonResponse([
    'ok' => false,
    'error' => 'unknown_action',
    'allowed_actions' => [
        'health',
        'search',
        'file',
    ],
], 400);
