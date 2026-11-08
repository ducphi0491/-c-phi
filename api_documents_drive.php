<?php
/**
 * TunerTools Documents Drive-FIRST LIST + MEDIA proxy, PHP 7.4+.
 * Upload to Alwaysdata beside api_gp_drive.php; do NOT replace old api_documents.php.
 * Google Drive holds the copied /document files in:
 *   My Drive/TunerTools_AppBackup/document/Guitar|Ukulele|Piano/...
 * This standalone PHP automatically resolves Drive file IDs by folder/name.
 * No documents_drive_index.php is needed or read. A private server-side cache
 * (APCu if available, otherwise temporary files) accelerates media Range calls.
 *
 * Access: original IIS api_documents.php action=drive_authorize authenticates
 * signed session/ticket and enforces Gmail, trial and folder scopes WITHOUT
 * opening a local document. Alwaysdata streams file bytes from Google Drive.
 * NOTE: public writes remain disabled until an atomic Drive-write backend exists.
 */
declare(strict_types=1);
header('X-Content-Type-Options: nosniff');
header('X-Robots-Tag: noindex, nofollow');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, HEAD, OPTIONS');
header('Access-Control-Allow-Headers: Range, Content-Type, Authorization, X-Document-Token');
header('Access-Control-Expose-Headers: Accept-Ranges, Content-Range, Content-Length, X-TT-Source');
function ttdJson(int $status, array $data): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}
function ttdError(string $code, int $status=503, array $extra=[]): void {
    ttdJson($status, array_merge(['ok'=>false, 'success'=>false, 'error'=>$code, 'message'=>'Documents Drive: ' . $code], $extra));
}
function ttdLog(string $msg): void { error_log('[TT Documents Drive] ' . $msg); }

function ttdEnvPath(string $env, string $fallback): string {
    $v = getenv($env);
    return is_string($v) && trim($v) !== '' ? trim($v) : $fallback;
}
function ttdTokenPath(): string {
    return ttdEnvPath('TUNERTOOLS_GP_TOKEN_PATH', __DIR__ . '/drive_token.json');
}
/** Same token JSON format as api_gp_drive.php. Refresh with a shared lock. */
function ttdReadToken(string $path): array {
    if (!is_file($path) || !is_readable($path)) ttdError('drive_token_not_found');
    $token = json_decode((string)@file_get_contents($path), true);
    if (!is_array($token)) ttdError('drive_token_invalid_json');
    return $token;
}
function ttdNeedsRefresh(array $t): bool {
    if (empty($t['token'])) return true;
    $expiry = trim((string)($t['expiry'] ?? ''));
    if ($expiry === '') return true;
    if (!preg_match('/(?:Z|[+-][0-9]{2}:[0-9]{2})$/i', $expiry)) $expiry .= 'Z';
    $time = strtotime($expiry);
    return $time === false || $time <= time() + 120;
}
function ttdRefreshToken(array $t): array {
    foreach (['refresh_token', 'client_id', 'client_secret'] as $field) {
        if (empty($t[$field])) ttdError('oauth_refresh_configuration_missing');
    }
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'client_id' => $t['client_id'],
            'client_secret' => $t['client_secret'],
            'refresh_token' => $t['refresh_token'],
            'grant_type' => 'refresh_token',
        ]),
        CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded'],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 8,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    $response = curl_exec($ch);
    $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $errno = curl_errno($ch);
    curl_close($ch);
    $data = is_string($response) ? json_decode($response, true) : null;
    if ($errno !== 0 || $status !== 200 || !is_array($data) || empty($data['access_token'])) {
        ttdLog('OAuth refresh failed: HTTP ' . $status . ', errno ' . $errno);
        ttdError('oauth_refresh_failed');
    }
    $t['token'] = (string)$data['access_token'];
    $t['expiry'] = gmdate('Y-m-d\TH:i:s\Z', time() + (int)($data['expires_in'] ?? 3600));
    return $t;
}
function ttdAccessToken(bool $force = false, string $rejected = ''): string {
    $path = ttdTokenPath();
    if (!$force) {
        $now = ttdReadToken($path);
        if (!ttdNeedsRefresh($now)) return (string)$now['token'];
    }
    // Reuse exact lock naming of GP API to prevent simultaneous refresh writes.
    $lockPath = sys_get_temp_dir() . '/tt_gp_token_' . hash('sha256', $path) . '.lock';
    $lock = @fopen($lockPath, 'c');
    if ($lock === false || !flock($lock, LOCK_EX)) ttdError('token_lock_failed');
    try {
        $t = ttdReadToken($path);
        $changed = $force && $rejected !== '' && (string)($t['token'] ?? '') !== $rejected;
        if (!$changed && ($force || ttdNeedsRefresh($t))) {
            $t = ttdRefreshToken($t);
            $tmp = $path . '.tmp.' . bin2hex(random_bytes(5));
            $json = json_encode($t, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
            if ($json === false || @file_put_contents($tmp, $json, LOCK_EX) === false) ttdError('token_write_failed');
            @chmod($tmp, 0600);
            if (!@rename($tmp, $path)) { @unlink($tmp); ttdError('token_write_failed'); }
        }
        $access = trim((string)($t['token'] ?? ''));
        if ($access === '') ttdError('token_empty');
        return $access;
    } finally {
        flock($lock, LOCK_UN);
        fclose($lock);
    }
}



/**
 * Google Drive lookup is fully contained in this PHP file. No published index.
 * Only credentials already used by api_gp_drive.php are read, via ttdAccessToken().
 * Resolve each segment under: My Drive/TunerTools_AppBackup/document.
 * Cache Drive IDs privately for fast repeated Range requests. Authentication
 * is ALWAYS checked separately against the original Documents API.
 */
const TTD_DRIVE_PARENT_NAME = 'TunerTools_AppBackup';
const TTD_DRIVE_DOCUMENTS_NAME = 'document';
const TTD_DRIVE_FOLDER_MIME = 'application/vnd.google-apps.folder';
const TTD_LOOKUP_CACHE_SECONDS = 600;
const TTD_MISS_CACHE_SECONDS = 30;

function ttdCacheName(string $key): string {
    return 'ttdocs_v3_' . hash('sha256', ttdTokenPath() . '|' . $key);
}
function ttdCachePath(string $key): string {
    $root = rtrim(sys_get_temp_dir(), '/\\') . '/ttdocs_' . substr(hash('sha256', __DIR__ . '|' . ttdTokenPath()), 0, 20);
    if (!is_dir($root)) {
        @mkdir($root, 0700, true);
    }
    return $root . '/' . ttdCacheName($key) . '.json';
}
/** Returns array(hit: bool, value: ?array). Negative lookup is cached briefly. */
function ttdCacheRead(string $key): array {
    $cacheKey = ttdCacheName($key);
    if (function_exists('apcu_fetch')) {
        $found = false;
        $value = @apcu_fetch($cacheKey, $found);
        if ($found && is_array($value) && array_key_exists('value', $value)) {
            return ['hit' => true, 'value' => $value['value']];
        }
    }
    $path = ttdCachePath($key);
    if (!is_file($path)) return ['hit' => false, 'value' => null];
    $raw = @file_get_contents($path);
    $obj = is_string($raw) ? json_decode($raw, true) : null;
    if (!is_array($obj) || !isset($obj['until']) || (int)$obj['until'] <= time()
            || !array_key_exists('value', $obj)) {
        @unlink($path);
        return ['hit' => false, 'value' => null];
    }
    return ['hit' => true, 'value' => $obj['value']];
}
function ttdCacheSave(string $key, ?array $value, int $seconds): void {
    $cacheKey = ttdCacheName($key);
    if (function_exists('apcu_store')) {
        @apcu_store($cacheKey, ['value' => $value], $seconds);
    }
    $path = ttdCachePath($key);
    if (!is_dir(dirname($path)) || !is_writable(dirname($path))) return;
    $data = json_encode(['until' => time() + $seconds, 'value' => $value], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    if (!is_string($data)) return;
    $tmp = $path . '.' . bin2hex(random_bytes(5)) . '.tmp';
    if (@file_put_contents($tmp, $data, LOCK_EX) !== false) {
        @chmod($tmp, 0600);
        if (!@rename($tmp, $path)) @unlink($tmp);
    } else {
        @unlink($tmp);
    }
}
/** Exactly one folder/file of this name must exist in the given Drive parent. */
function ttdDriveNamed(string $parentId, string $name, bool $folder): ?array {
    if ($parentId !== 'root' && !preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $parentId)) {
        throw new RuntimeException('invalid_drive_parent');
    }
    $cacheKey = 'named:' . ($folder ? 'd:' : 'f:') . $parentId . '/' . $name;
    $cached = ttdCacheRead($cacheKey);
    if ($cached['hit']) return is_array($cached['value']) ? $cached['value'] : null;

    $escapedName = str_replace(['\\', "'"], ['\\\\', "\\'"], $name);
    $query = "'" . $parentId . "' in parents and name = '" . $escapedName . "' and trashed = false and mimeType " .
             ($folder ? '= ' : '!= ') . "'" . TTD_DRIVE_FOLDER_MIME . "'";
    $params = [
        'q' => $query,
        'pageSize' => 3, // Catch ambiguously duplicated names without listing the full Drive.
        'fields' => 'nextPageToken,files(id,name,mimeType,size,md5Checksum)',
        'spaces' => 'drive',
    ];
    $url = 'https://www.googleapis.com/drive/v3/files?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);
    $token = ttdAccessToken();
    $refreshed = false;
    for ($attempt = 0; $attempt < 3; $attempt++) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_HTTPGET => true,
            CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token, 'Accept: application/json'],
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_TIMEOUT => 22,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_FOLLOWLOCATION => false,
        ]);
        $body = curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $errno = curl_errno($ch);
        curl_close($ch);
        if ($status === 401 && !$refreshed) {
            $token = ttdAccessToken(true, $token);
            $refreshed = true;
            continue;
        }
        if (in_array($status, [408, 429, 500, 502, 503, 504], true) && $attempt < 2) {
            usleep(250000 * ($attempt + 1));
            continue;
        }
        if ($errno !== 0 || $status !== 200 || !is_string($body)) {
            throw new RuntimeException('drive_list_failed_http_' . $status);
        }
        $result = json_decode($body, true);
        if (!is_array($result) || !isset($result['files']) || !is_array($result['files'])) {
            throw new RuntimeException('drive_list_invalid_json');
        }
        if (count($result['files']) > 1 || !empty($result['nextPageToken'])) {
            throw new RuntimeException('duplicate_drive_path');
        }
        $match = $result['files'][0] ?? null;
        if ($match !== null && (!is_array($match) || empty($match['id']) ||
                !preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', (string)$match['id']))) {
            throw new RuntimeException('drive_list_invalid_id');
        }
        ttdCacheSave($cacheKey, $match, $match === null ? TTD_MISS_CACHE_SECONDS : TTD_LOOKUP_CACHE_SECONDS);
        return $match;
    }
    throw new RuntimeException('drive_list_retries_exhausted');
}
function ttdDocumentsDriveRoot(): ?string {
    // Optional faster configuration, if known. NOT required for normal use.
    $override = trim((string)getenv('TUNERTOOLS_DOCUMENTS_DRIVE_ROOT_ID'));
    if ($override !== '') {
        if (!preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $override)) throw new RuntimeException('invalid_drive_root_override');
        return $override;
    }
    $root = ttdDriveNamed('root', TTD_DRIVE_PARENT_NAME, true);
    if (!$root) return null;
    $documents = ttdDriveNamed((string)$root['id'], TTD_DRIVE_DOCUMENTS_NAME, true);
    return $documents ? (string)$documents['id'] : null;
}
/** Returns ['id'=>Drive ID, 'size'=>int] or null when not yet uploaded. */
function ttdDriveFile(string $instrument, string $path): ?array {
    $cacheKey = 'media:' . $instrument . '/' . $path;
    $cached = ttdCacheRead($cacheKey);
    if ($cached['hit']) return is_array($cached['value']) ? $cached['value'] : null;
    $parent = ttdDocumentsDriveRoot();
    if ($parent === null) {
        ttdCacheSave($cacheKey, null, TTD_MISS_CACHE_SECONDS);
        return null;
    }
    $parts = array_merge([$instrument], explode('/', $path));
    $last = count($parts) - 1;
    foreach ($parts as $i => $name) {
        $entry = ttdDriveNamed($parent, $name, $i < $last);
        if ($entry === null) {
            ttdCacheSave($cacheKey, null, TTD_MISS_CACHE_SECONDS);
            return null;
        }
        $parent = (string)$entry['id'];
    }
    $file = [
        'id' => (string)$entry['id'],
        'size' => isset($entry['size']) && ctype_digit((string)$entry['size']) ? (int)$entry['size'] : null,
    ];
    // If Google did not return size, fail closed to the original API.
    if ($file['size'] === null) throw new RuntimeException('drive_file_size_missing');
    ttdCacheSave($cacheKey, $file, TTD_LOOKUP_CACHE_SECONDS);
    return $file;
}

function ttdInstrument(string $raw): string {
    $v = strtolower(trim($raw));
    $map = ['guitar' => 'Guitar', 'ukulele' => 'Ukulele', 'piano' => 'Piano'];
    if (!isset($map[$v])) ttdError('unsupported_instrument', 400);
    return $map[$v];
}
function ttdPath(string $raw): string {
    $raw = str_replace('\\', '/', $raw);
    $segments = explode('/', trim($raw, '/'));
    if ($raw === '' || strlen($raw) > 1000 || count($segments) > 32) ttdError('invalid_path', 400);
    foreach ($segments as $part) {
        if ($part === '' || $part === '.' || $part === '..' || strlen($part) > 255 ||
            preg_match('/[\x00-\x1F\x7F]/', $part) || $part[0] === '.') {
            ttdError('invalid_path', 400);
        }
    }
    return implode('/', $segments);
}
function ttdAuthHeaders(): array {
    $result = [];
    $auth = (string)($_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '');
    if (preg_match('/^Bearer [A-Za-z0-9._~+\/-]{12,4096}$/D', $auth)) $result[] = 'Authorization: ' . $auth;
    $token = trim((string)($_SERVER['HTTP_X_DOCUMENT_TOKEN'] ?? ''));
    if ($token !== '' && strlen($token) <= 4096 && preg_match('/^[A-Za-z0-9._~+\/-]+$/D', $token)) {
        $result[] = 'X-Document-Token: ' . $token;
    }
    return $result;
}
function ttdMime(string $path): string {
    $ext = strtolower(pathinfo($path, PATHINFO_EXTENSION));
    $map = [
      'pdf'=>'application/pdf', 'jpg'=>'image/jpeg','jpeg'=>'image/jpeg','png'=>'image/png',
      'webp'=>'image/webp','gif'=>'image/gif','bmp'=>'image/bmp', 'mp4'=>'video/mp4',
      'm4v'=>'video/mp4','mov'=>'video/quicktime','webm'=>'video/webm',
      'mkv'=>'video/x-matroska','avi'=>'video/x-msvideo',
    ];
    if (!isset($map[$ext])) ttdError('unsupported_media_type', 415);
    return $map[$ext];
}
function ttdMediaHeaders(int $status, string $mime, string $name, bool $download, array $h): void {
    http_response_code($status);
    header('Content-Type: ' . $mime);
    header('Content-Encoding: identity');
    header('Cache-Control: private, no-store, must-revalidate');
    header('Accept-Ranges: bytes');
    header('X-Accel-Buffering: no');
    header('X-TT-Source: google_drive');
    $safe = preg_replace('/[^a-zA-Z0-9._-]/', '_', $name) ?: 'asset';
    header('Content-Disposition: ' . ($download ? 'attachment' : 'inline') . '; filename="' . $safe . '"');
    if (isset($h['content-length']) && ctype_digit($h['content-length'])) {
        header('Content-Length: ' . $h['content-length']);
    }
    if (isset($h['content-range']) && preg_match('/^bytes [0-9]+-[0-9]+\/[0-9*]+$/D', $h['content-range'])) {
        header('Content-Range: ' . $h['content-range']);
    }
}
function ttdStreamAttempt(string $id, string $accessToken, string $mime, string $filename,
                          bool $download, ?string $range): array {
    $code = 0; $headers = []; $sent = false; $received = 0;
    $reqHeaders = ['Authorization: Bearer ' . $accessToken];
    if ($range !== null) $reqHeaders[] = 'Range: ' . $range;
    $ch = curl_init('https://www.googleapis.com/drive/v3/files/' . rawurlencode($id) . '?alt=media');
    curl_setopt_array($ch, [
        CURLOPT_FOLLOWLOCATION => true, CURLOPT_MAXREDIRS => 3,
        CURLOPT_HTTPHEADER => $reqHeaders,
        CURLOPT_SSL_VERIFYPEER => true, CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_CONNECTTIMEOUT => 8, CURLOPT_TIMEOUT => 180,
        CURLOPT_RETURNTRANSFER => false, CURLOPT_BUFFERSIZE => 65536,
        CURLOPT_HEADERFUNCTION => static function ($handle, string $line) use (&$code, &$headers): int {
            if (preg_match('/^HTTP\/[0-9.]+\s+([0-9]{3})/i', trim($line), $m)) {
                $code = (int)$m[1]; $headers = [];
            } elseif (strpos($line, ':') !== false) {
                [$k, $v] = explode(':', $line, 2);
                $k = strtolower(trim($k));
                if (in_array($k, ['content-length', 'content-range'], true)) $headers[$k] = trim($v);
            }
            return strlen($line);
        },
        CURLOPT_WRITEFUNCTION => static function ($handle, string $chunk) use (&$code, &$headers, &$sent, &$received, $mime, $filename, $download, $range): int {
            $len = strlen($chunk);
            // An ignored seek is not a successful video Range response.
            if ($range !== null && $code === 200 && $range !== 'bytes=0-') return 0;
            if ($code !== 200 && $code !== 206) return $len;
            if (!$sent) {
                ttdMediaHeaders($code, $mime, $filename, $download, $headers);
                $sent = true;
            }
            echo $chunk;
            $received += $len;
            if ($received % 262144 < $len) @flush();
            return connection_aborted() ? 0 : $len;
        },
    ]);
    $ok = curl_exec($ch);
    $http = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $errno = curl_errno($ch);
    curl_close($ch);
    return ['status'=>$http, 'errno'=>$errno, 'ok'=>$ok !== false, 'sent'=>$sent,
            'headers'=>$headers, 'range_ignored'=>$range !== null && $http===200 && $range!=='bytes=0-'];
}
function ttdStream(array $file, string $mime, string $filename, bool $download): void {
    if (!preg_match('/^[A-Za-z0-9_-]{8,200}$/D', (string)($file['id'] ?? ''))) ttdError('missing_drive_file', 404);
    $range = null;
    if (isset($_SERVER['HTTP_RANGE'])) {
        $range = trim((string)$_SERVER['HTTP_RANGE']);
        if (!preg_match('/^bytes=([0-9]*)-([0-9]*)$/D', $range, $matches) ||
            ($matches[1] === '' && $matches[2] === '')) ttdError('invalid_range', 416);
    }
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'HEAD') {
        ttdMediaHeaders(200, $mime, $filename, $download, ['content-length' => (string)($file['size'] ?? 0)]);
        exit;
    }
    @ini_set('zlib.output_compression', '0');
    @ignore_user_abort(false);
    @set_time_limit(0);
    while (ob_get_level() > 0) @ob_end_clean();
    $accessToken = ttdAccessToken();
    $refreshed = false;
    for ($i = 0; $i < 3; $i++) {
        $result = ttdStreamAttempt((string)$file['id'], $accessToken, $mime, $filename, $download, $range);
        if ($result['sent']) exit; // Never retry after media was partially delivered.
        if ($result['ok'] && in_array($result['status'], [200,206], true) && !$result['range_ignored']) {
            ttdMediaHeaders($result['status'], $mime, $filename, $download, $result['headers']);
            exit;
        }
        if ($result['status'] === 401 && !$refreshed) {
            $accessToken = ttdAccessToken(true, $accessToken); $refreshed = true;
            continue;
        }
        if (in_array($result['status'], [429,500,502,503,504], true) && $i < 2) {
            usleep(250000 * ($i+1)); continue;
        }
        ttdLog('Google Drive media unavailable; HTTP ' . $result['status'] . '; errno ' . $result['errno']);
        ttdError('google_drive_media_unavailable', 502, ['google_http'=>$result['status']]);
    }
    ttdError('google_drive_media_unavailable', 502);
}

/**
 * Drive-first Documents bridge (read only).
 * Trust boundary: the original IIS API alone validates Gmail session, trial,
 * folder scopes and short-lived media tickets. This file NEVER trusts a
 * permission claim from the Android client.
 */
function ttdIisAuthorize(string $purpose, string $instrument, string $path,
                         string $ticket = '', array $children = []): array {
    $headers = ttdAuthHeaders();
    if ($ticket === '' && $headers === []) ttdError('authentication_required', 401);
    $fields = [
        'action' => 'drive_authorize', 'purpose' => $purpose,
        'instrument' => $instrument, 'path' => $path,
    ];
    if ($ticket !== '') $fields['ticket'] = $ticket;
    if ($purpose === 'list') {
        $encoded = json_encode($children, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
        if (!is_string($encoded)) ttdError('invalid_children', 400);
        $fields['children_json'] = $encoded;
    }
    $ch = curl_init('https://tunertools.top/api_documents.php');
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => http_build_query($fields, '', '&', PHP_QUERY_RFC3986),
        CURLOPT_HTTPHEADER => array_merge([
            'Content-Type: application/x-www-form-urlencoded; charset=utf-8',
            'Accept: application/json', 'Cache-Control: no-store',
        ], $headers),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 8, CURLOPT_TIMEOUT => 23,
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_SSL_VERIFYPEER => true, CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    $body = curl_exec($ch);
    $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $errno = curl_errno($ch);
    curl_close($ch);
    $reply = is_string($body) ? json_decode($body, true) : null;
    if ($status === 401 || $status === 403) {
        ttdError('permission_denied', $status);
    }
    if ($status === 404 || $status === 405 || $status === 400 ||
        ($status === 200 && (!is_array($reply) || empty($reply['authorized'])))) {
        ttdError('iis_drive_authorize_patch_required', 503, [
            'message' => 'Cần cập nhật action=drive_authorize trong api_documents.php trên IIS.'
        ]);
    }
    if ($errno !== 0 || $status !== 200 || !is_array($reply) ||
        empty($reply['success']) || empty($reply['authorized'])) {
        ttdLog('IIS authorization unavailable; status=' . $status . ', errno=' . $errno);
        ttdError('iis_authorization_unavailable', 503);
    }
    return $reply;
}

/** List at most 1000 per request and paginate without disclosing Drive IDs. */
function ttdDriveChildren(string $folderId): array {
    if (!preg_match('/^[A-Za-z0-9_-]{8,200}$/D', $folderId)) {
        throw new RuntimeException('invalid_drive_folder');
    }
    $cacheKey = 'children:' . $folderId;
    $cached = ttdCacheRead($cacheKey);
    if ($cached['hit'] && is_array($cached['value'])) return $cached['value'];

    $files = [];
    $pageToken = '';
    $token = ttdAccessToken();
    $refreshed = false;
    for ($page = 0; $page < 30; $page++) {
        $params = [
            'q' => "'" . $folderId . "' in parents and trashed = false",
            'fields' => 'nextPageToken,files(id,name,mimeType,size,modifiedTime)',
            'pageSize' => 1000, 'spaces' => 'drive',
        ];
        if ($pageToken !== '') $params['pageToken'] = $pageToken;
        $url = 'https://www.googleapis.com/drive/v3/files?' .
            http_build_query($params, '', '&', PHP_QUERY_RFC3986);
        $result = null;
        for ($attempt=0; $attempt < 3; $attempt++) {
            $ch = curl_init($url);
            curl_setopt_array($ch, [
                CURLOPT_HTTPGET => true,
                CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token, 'Accept: application/json'],
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_CONNECTTIMEOUT => 8, CURLOPT_TIMEOUT => 25,
                CURLOPT_FOLLOWLOCATION => false,
                CURLOPT_SSL_VERIFYPEER => true, CURLOPT_SSL_VERIFYHOST => 2,
            ]);
            $body = curl_exec($ch);
            $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $errno = curl_errno($ch);
            curl_close($ch);
            if ($status === 401 && !$refreshed) {
                $token = ttdAccessToken(true, $token);
                $refreshed = true;
                continue;
            }
            if (in_array($status, [408,429,500,502,503,504], true) && $attempt < 2) {
                usleep(250000 * ($attempt + 1));
                continue;
            }
            if ($errno !== 0 || $status !== 200 || !is_string($body)) {
                throw new RuntimeException('drive_children_http_' . $status);
            }
            $result = json_decode($body, true);
            if (!is_array($result) || !isset($result['files']) || !is_array($result['files'])) {
                throw new RuntimeException('drive_children_invalid_json');
            }
            break;
        }
        if (!is_array($result)) throw new RuntimeException('drive_children_retries_exhausted');
        foreach ($result['files'] as $item) {
            if (!is_array($item)) continue;
            $id = (string)($item['id'] ?? '');
            $name = (string)($item['name'] ?? '');
            if ($id === '' || !preg_match('/^[A-Za-z0-9_-]{8,200}$/D', $id) ||
                $name === '' || $name[0] === '.' ||
                strlen($name) > 255 || strpos($name, '/') !== false ||
                strpos($name, '\\') !== false || preg_match('/[\x00-\x1f\x7f]/', $name)) continue;
            $files[] = [
                'id' => $id, 'name' => $name,
                'mimeType' => (string)($item['mimeType'] ?? ''),
                'size' => (string)($item['size'] ?? '0'),
            ];
            if (count($files) > 25000) throw new RuntimeException('drive_folder_too_large');
        }
        $pageToken = (string)($result['nextPageToken'] ?? '');
        if ($pageToken === '') {
            // Shorter cache for directory changes. Access always reauthorized.
            ttdCacheSave($cacheKey, $files, 45);
            return $files;
        }
    }
    throw new RuntimeException('drive_folder_pagination_limit');
}
function ttdFolderPath(string $raw): string {
    if ($raw === '') return '';
    return ttdPath($raw);
}
function ttdDriveFolderId(string $instrument, string $path): ?string {
    $root = ttdDocumentsDriveRoot();
    if ($root === null) return null;
    $instrumentDir = ttdDriveNamed($root, $instrument, true);
    if ($instrumentDir === null) return null;
    $id = (string)$instrumentDir['id'];
    if ($path === '') return $id;
    foreach (explode('/', $path) as $segment) {
        $next = ttdDriveNamed($id, $segment, true);
        if ($next === null) return null;
        $id = (string)$next['id'];
    }
    return $id;
}
function ttdMediaType(string $mime, string $name): ?string {
    $ext = strtolower(pathinfo($name, PATHINFO_EXTENSION));
    if ($mime === TTD_DRIVE_FOLDER_MIME) return 'folder';
    if ($mime === 'application/vnd.google-apps.shortcut' ||
        strpos($mime, 'application/vnd.google-apps.') === 0) return null;
    if (in_array($ext, ['pdf'], true)) return 'pdf';
    if (in_array($ext, ['jpg','jpeg','png','webp','gif','bmp'], true)) return 'image';
    if (in_array($ext, ['mp4','m4v','mov','webm','mkv','avi'], true)) return 'video';
    return null;
}
function ttdParentPath(string $path): ?string {
    if ($path === '') return null;
    $pos = strrpos($path, '/');
    return $pos === false ? '' : substr($path, 0, $pos);
}
function ttdBreadcrumbs(string $instrument, string $path): array {
    $parts = [['path'=>'', 'name'=>$instrument]];
    $chain = '';
    if ($path !== '') {
        foreach (explode('/', $path) as $part) {
            $chain = $chain === '' ? $part : $chain . '/' . $part;
            $parts[] = ['path'=>$chain, 'name'=>$part];
        }
    }
    return $parts;
}
function ttdMediaLink(string $action, string $instrument, string $path, string $ticket): string {
    if ($ticket === '') return '';
    $query = http_build_query([
        'action'=>$action, 'instrument'=>$instrument, 'path'=>$path, 'ticket'=>$ticket,
    ], '', '&', PHP_QUERY_RFC3986);
    return 'https://tuner.alwaysdata.net/api_documents_drive.php?' . $query;
}
function ttdHumanBytes(int $size): string {
    if ($size < 1024) return $size . ' B';
    if ($size < 1048576) return number_format($size / 1024, 1) . ' KB';
    return number_format($size / 1048576, 1) . ' MB';
}
/** Admin-only folder choices for the existing permissions editor. */
function ttdFolderOptions(string $instrument): void {
    // Access MUST be granted by the original permission engine on IIS.
    ttdIisAuthorize('folder_options', $instrument, '');
    $id = ttdDriveFolderId($instrument, '');
    $folders = $id === null ? [] : ttdDriveChildren($id);
    $items = [];
    $seen = [];
    foreach ($folders as $f) {
        if ((string)($f['mimeType'] ?? '') !== TTD_DRIVE_FOLDER_MIME) continue;
        $name = (string)($f['name'] ?? '');
        if ($name === '' || isset($seen[$name])) continue;
        $seen[$name] = true;
        $items[] = [
            'key'=>$instrument . '/' . $name,
            'instrument'=>$instrument,
            'path'=>$name, 'name'=>$name,
        ];
    }
    usort($items, static function(array $a, array $b): int {
        return strnatcasecmp((string)$a['name'], (string)$b['name']);
    });
    ttdJson(200, ['ok'=>true, 'success'=>true, 'action'=>'folder_options',
        'source'=>'google_drive', 'instrument'=>$instrument, 'items'=>$items]);
}

/** Shape compatible with LearningMaterialsApi.loadPage() in Gamefolder.kt. */
function ttdListPage(string $instrument, string $path): void {
    // IMPORTANT: authenticate BEFORE any Drive list call.
    ttdIisAuthorize('list', $instrument, $path);
    $driveFolder = ttdDriveFolderId($instrument, $path);
    $all = $driveFolder === null ? [] : ttdDriveChildren($driveFolder);
    $candidates = [];
    $seen = [];
    foreach ($all as $item) {
        $kind = ttdMediaType((string)$item['mimeType'], (string)$item['name']);
        if ($kind === null) continue;
        $rel = ($path === '' ? '' : $path . '/') . $item['name'];
        if (isset($seen[$rel])) {
            // Duplicate names are ambiguous for future direct lookup: fail closed.
            unset($candidates[$rel]);
            $seen[$rel] = -1;
            continue;
        }
        $seen[$rel] = 1;
        $candidates[$rel] = ['path'=>$rel, 'type'=>$kind === 'folder' ? 'folder':'file', 'kind'=>$kind, 'entry'=>$item];
    }
    $signed = [];
    foreach (array_chunk(array_values($candidates), 80) as $batch) {
        $requestItems = [];
        foreach ($batch as $child) {
            $requestItems[] = ['path'=>$child['path'], 'type'=>$child['type']];
        }
        $response = ttdIisAuthorize('list', $instrument, $path, '', $requestItems);
        foreach (($response['children'] ?? []) as $allowed) {
            if (is_array($allowed) && isset($allowed['path']) && is_string($allowed['path'])) {
                $signed[$allowed['path']] = $allowed;
            }
        }
    }
    $items = [];
    foreach ($candidates as $rel => $entry) {
        if (!isset($signed[$rel])) continue;
        $approved = $signed[$rel];
        $f = $entry['entry'];
        $kind = $entry['kind'];
        $isFolder = $kind === 'folder';
        $name = (string)$f['name'];
        $size = ctype_digit((string)$f['size']) ? (int)$f['size'] : 0;
        $extension = strtoupper(pathinfo($name, PATHINFO_EXTENSION));
        $baseName = (string)pathinfo($name, PATHINFO_FILENAME);
        $isCommunity = ($rel === 'Cộng đồng' || strpos($rel, 'Cộng đồng/') === 0);
        $viewTicket = $isFolder ? '' : (string)($approved['view_ticket'] ?? '');
        if (!$isFolder && $viewTicket === '') continue;
        $downloadTicket = $isFolder ? '' : (string)($approved['download_ticket'] ?? '');
        $items[] = [
            'id'=>sha1($instrument . '|' . $kind . '|' . $rel),
            'name'=>$isFolder ? $name : ($baseName === '' ? $name : $baseName),
            'file_name'=>$name,
            'type'=>$kind,
            'path'=>$rel,
            'description'=>'',
            'meta'=>$isFolder ? '' : ($extension . ' • ' . ttdHumanBytes($size)),
            'size_bytes'=>$isFolder ? 0 : $size,
            // Folder summary intentionally omitted (unknown rather than false).
            'children_count'=>-1,
            'is_protected_system_folder'=>$isCommunity && $rel === 'Cộng đồng',
            'owner_email'=>'', 'is_owner'=>false, 'can_manage_item'=>false,
            'is_community_content'=>$isCommunity,
            'preview_url'=>$isFolder ? '' : ttdMediaLink('view', $instrument, $rel, $viewTicket),
            'download_url'=>$isFolder ? '' : ttdMediaLink('download', $instrument, $rel, $downloadTicket),
        ];
    }
    usort($items, static function(array $a, array $b): int {
        if ($a['type'] === 'folder' && $b['type'] !== 'folder') return -1;
        if ($b['type'] === 'folder' && $a['type'] !== 'folder') return 1;
        return strnatcasecmp((string)$a['name'], (string)$b['name']);
    });
    ttdJson(200, [
        'success'=>true, 'ok'=>true, 'action'=>'list', 'source'=>'google_drive',
        'instrument'=>$instrument, 'available'=>$driveFolder !== null,
        'current_path'=>$path, 'parent_path'=>ttdParentPath($path),
        'breadcrumbs'=>ttdBreadcrumbs($instrument, $path),
        'contribution_folder'=>'Cộng đồng',
        'can_public_upload_here'=>false, 'can_public_create_folder_here'=>false,
        'current_folder_owner_email'=>'', 'current_folder_is_owner'=>false,
        'items'=>$items,
        'message'=>$driveFolder === null ? 'Thư mục này chưa có trên Google Drive.' :
            (count($items) === 0 ? 'Thư mục này chưa có tài liệu phù hợp.' : ''),
        'updated_at'=>gmdate('c'),
    ]);
}

$method = strtoupper((string)($_SERVER['REQUEST_METHOD'] ?? 'GET'));
if ($method === 'OPTIONS') { http_response_code(204); exit; }
if (!in_array($method, ['GET','HEAD'], true)) ttdError('method_not_allowed', 405);
$action = strtolower(trim((string)($_GET['action'] ?? 'health')));
if ($action === 'health') {
    try {
        $rootId = ttdDocumentsDriveRoot();
        $ready = $rootId !== null;
        ttdJson(200, [
            'ok'=>true, 'success'=>true,
            'service'=>'tunertools_documents_drive_media',
            'mode'=>'single_php_live_lookup',
            'read_source'=>'google_drive',
            'list_source'=>'google_drive',
            'authorization'=>'iis_drive_authorize_no_filesystem',
            'index_required'=>false,
            'index_ready'=>$ready,
            'drive_root_ready'=>$ready,
            'indexed_files'=>null,
            'media_source'=>'google_drive_via_alwaysdata',
            'cache_ttl_seconds'=>TTD_LOOKUP_CACHE_SECONDS,
        ]);
    } catch (Throwable $e) {
        ttdLog('Drive health failed: ' . substr($e->getMessage(), 0, 80));
        ttdError('drive_lookup_unavailable', 503);
    }
}
if (!in_array($action, ['list','view','download','folder_options'], true)) ttdError('unsupported_action', 400);
$instrument = ttdInstrument((string)($_GET['instrument'] ?? ''));
$rawPath = (string)($_GET['path'] ?? '');
$path = in_array($action, ['list','folder_options'], true) ? ttdFolderPath($rawPath) : ttdPath($rawPath);
try {
    if ($action === 'folder_options') {
        if ($method !== 'GET') ttdError('method_not_allowed', 405);
        ttdFolderOptions($instrument);
    }
    if ($action === 'list') {
        if ($method !== 'GET') ttdError('method_not_allowed', 405);
        ttdListPage($instrument, $path);
    }
    $ticket = trim((string)($_GET['ticket'] ?? ''));
    if (strlen($ticket) > 4096 || preg_match('/[\x00-\x20\x7F]/', $ticket)) {
        ttdError('invalid_ticket', 400);
    }
    // IIS checks signed ticket or user session, with NO read/stat on local files.
    ttdIisAuthorize($action, $instrument, $path, $ticket);
    $entry = ttdDriveFile($instrument, $path);
    if (!is_array($entry) || empty($entry['id'])) {
        ttdError('file_not_found_on_google_drive', 404);
    }
    ttdStream($entry, ttdMime($path), basename($path), $action === 'download');
} catch (Throwable $e) {
    ttdLog('Documents Drive read failed: ' . substr($e->getMessage(), 0, 90));
    ttdError('google_drive_unavailable', 503);
}
