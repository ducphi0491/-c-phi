<?php
/**
 * TunerTools AI cached media on Google Drive: standalone PHP 7.4+.
 * Upload ONLY this PHP to Alwaysdata alongside api_gp_drive.php.
 * Reads a private shared Drive token via TUNERTOOLS_GP_TOKEN_PATH.
 * Drive structure: My Drive / TunerTools_AppBackup / AI_Music /
 *                  stream_cache / <audio filename>
 *                  lyrics       / <json filename> (optional; future backend)
 *
 * The cached_songs endpoint intentionally publishes song titles/artist names
 * already listed by the existing public Python get_cached_songs endpoint.
 * It never exposes Drive IDs, OAuth credentials or folder structure.
 * Security: No embedded secrets, no directory traversal; media access is
 * comparable to the CURRENT public /api/stream_cache/<filename> endpoint.
 * For non-public content, enforce authorization in the origin/backend before
 * using public cached media URLs. Store only media you have rights to serve.
 * PHP never uploads files; Windows sync tool or AI Python backend uploads.
 * It intentionally does not silently redirect to Python: Android falls back
 * after a failed lookup or a failed prepare().
 */
declare(strict_types=1);
header('X-Content-Type-Options: nosniff');
header('X-Robots-Tag: noindex, nofollow');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, HEAD, OPTIONS');
header('Access-Control-Allow-Headers: Range, Content-Type');
header('Access-Control-Expose-Headers: X-TT-Source, Content-Range, Accept-Ranges, Content-Length');
function ttaJson(int $status, array $data): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}
function ttaError(string $code, int $status=503, array $extra=[]): void {
    ttaJson($status, array_merge(['ok'=>false, 'error'=>$code], $extra));
}
function ttaLog(string $msg): void { error_log('[TT AI Drive] ' . $msg); }

function ttaEnvPath(string $env, string $fallback): string {
    $v = getenv($env);
    return is_string($v) && trim($v) !== '' ? trim($v) : $fallback;
}
function ttaTokenPath(): string {
    return ttaEnvPath('TUNERTOOLS_GP_TOKEN_PATH', __DIR__ . '/drive_token.json');
}
/** Same token JSON format as api_gp_drive.php. Refresh with a shared lock. */
function ttaReadToken(string $path): array {
    if (!is_file($path) || !is_readable($path)) ttaError('drive_token_not_found');
    $token = json_decode((string)@file_get_contents($path), true);
    if (!is_array($token)) ttaError('drive_token_invalid_json');
    return $token;
}
function ttaNeedsRefresh(array $t): bool {
    if (empty($t['token'])) return true;
    $expiry = trim((string)($t['expiry'] ?? ''));
    if ($expiry === '') return true;
    if (!preg_match('/(?:Z|[+-][0-9]{2}:[0-9]{2})$/i', $expiry)) $expiry .= 'Z';
    $time = strtotime($expiry);
    return $time === false || $time <= time() + 120;
}
function ttaRefreshToken(array $t): array {
    foreach (['refresh_token', 'client_id', 'client_secret'] as $field) {
        if (empty($t[$field])) ttaError('oauth_refresh_configuration_missing');
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
        ttaLog('OAuth refresh failed: HTTP ' . $status . ', errno ' . $errno);
        ttaError('oauth_refresh_failed');
    }
    $t['token'] = (string)$data['access_token'];
    $t['expiry'] = gmdate('Y-m-d\TH:i:s\Z', time() + (int)($data['expires_in'] ?? 3600));
    return $t;
}
function ttaAccessToken(bool $force = false, string $rejected = ''): string {
    $path = ttaTokenPath();
    if (!$force) {
        $now = ttaReadToken($path);
        if (!ttaNeedsRefresh($now)) return (string)$now['token'];
    }
    // Reuse exact lock naming of GP API to prevent simultaneous refresh writes.
    $lockPath = sys_get_temp_dir() . '/tt_gp_token_' . hash('sha256', $path) . '.lock';
    $lock = @fopen($lockPath, 'c');
    if ($lock === false || !flock($lock, LOCK_EX)) ttaError('token_lock_failed');
    try {
        $t = ttaReadToken($path);
        $changed = $force && $rejected !== '' && (string)($t['token'] ?? '') !== $rejected;
        if (!$changed && ($force || ttaNeedsRefresh($t))) {
            $t = ttaRefreshToken($t);
            $tmp = $path . '.tmp.' . bin2hex(random_bytes(5));
            $json = json_encode($t, JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT);
            if ($json === false || @file_put_contents($tmp, $json, LOCK_EX) === false) ttaError('token_write_failed');
            @chmod($tmp, 0600);
            if (!@rename($tmp, $path)) { @unlink($tmp); ttaError('token_write_failed'); }
        }
        $access = trim((string)($t['token'] ?? ''));
        if ($access === '') ttaError('token_empty');
        return $access;
    } finally {
        flock($lock, LOCK_UN);
        fclose($lock);
    }
}



/**
 * Google Drive lookup is fully contained in this PHP file.
 * Only credentials already used by api_gp_drive.php are read, via ttaAccessToken().
 * Resolve each segment under: My Drive/TunerTools_AppBackup/AI_Music.
 * Drive IDs and OAuth credentials remain private. Media is publicly accessible
 * under the same policy as the existing public Python cached-media endpoint.
 */
const TTA_DRIVE_PARENT_NAME = 'TunerTools_AppBackup';
const TTA_DRIVE_DOCUMENTS_NAME = 'AI_Music';
const TTA_DRIVE_FOLDER_MIME = 'application/vnd.google-apps.folder';
const TTA_LOOKUP_CACHE_SECONDS = 600;
const TTA_MISS_CACHE_SECONDS = 30;

function ttaCacheName(string $key): string {
    return 'ttaocs_v3_' . hash('sha256', ttaTokenPath() . '|' . $key);
}
function ttaCachePath(string $key): string {
    $root = rtrim(sys_get_temp_dir(), '/\\') . '/ttaocs_' . substr(hash('sha256', __DIR__ . '|' . ttaTokenPath()), 0, 20);
    if (!is_dir($root)) {
        @mkdir($root, 0700, true);
    }
    return $root . '/' . ttaCacheName($key) . '.json';
}
/** Returns array(hit: bool, value: ?array). Negative lookup is cached briefly. */
function ttaCacheRead(string $key): array {
    $cacheKey = ttaCacheName($key);
    if (function_exists('apcu_fetch')) {
        $found = false;
        $value = @apcu_fetch($cacheKey, $found);
        if ($found && is_array($value) && array_key_exists('value', $value)) {
            return ['hit' => true, 'value' => $value['value']];
        }
    }
    $path = ttaCachePath($key);
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
function ttaCacheSave(string $key, ?array $value, int $seconds): void {
    $cacheKey = ttaCacheName($key);
    if (function_exists('apcu_store')) {
        @apcu_store($cacheKey, ['value' => $value], $seconds);
    }
    $path = ttaCachePath($key);
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
function ttaDriveNamed(string $parentId, string $name, bool $folder): ?array {
    if ($parentId !== 'root' && !preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $parentId)) {
        throw new RuntimeException('invalid_drive_parent');
    }
    $cacheKey = 'named:' . ($folder ? 'd:' : 'f:') . $parentId . '/' . $name;
    $cached = ttaCacheRead($cacheKey);
    if ($cached['hit']) return is_array($cached['value']) ? $cached['value'] : null;

    $escapedName = str_replace(['\\', "'"], ['\\\\', "\\'"], $name);
    $query = "'" . $parentId . "' in parents and name = '" . $escapedName . "' and trashed = false and mimeType " .
             ($folder ? '= ' : '!= ') . "'" . TTA_DRIVE_FOLDER_MIME . "'";
    $params = [
        'q' => $query,
        'pageSize' => 3, // Catch ambiguously duplicated names without listing the full Drive.
        'fields' => 'nextPageToken,files(id,name,mimeType,size,md5Checksum)',
        'spaces' => 'drive',
    ];
    $url = 'https://www.googleapis.com/drive/v3/files?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);
    $token = ttaAccessToken();
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
            $token = ttaAccessToken(true, $token);
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
        ttaCacheSave($cacheKey, $match, $match === null ? TTA_MISS_CACHE_SECONDS : TTA_LOOKUP_CACHE_SECONDS);
        return $match;
    }
    throw new RuntimeException('drive_list_retries_exhausted');
}
function ttaAiDriveRoot(): ?string {
    // Optional faster configuration, if known. NOT required for normal use.
    $override = trim((string)getenv('TUNERTOOLS_AI_DRIVE_ROOT_ID'));
    if ($override !== '') {
        if (!preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $override)) throw new RuntimeException('invalid_drive_root_override');
        return $override;
    }
    $root = ttaDriveNamed('root', TTA_DRIVE_PARENT_NAME, true);
    if (!$root) return null;
    $documents = ttaDriveNamed((string)$root['id'], TTA_DRIVE_DOCUMENTS_NAME, true);
    return $documents ? (string)$documents['id'] : null;
}

/** Cache is deliberately flat per bucket. No arbitrary path traversal. */
function ttaAiFile(string $bucket, string $name): ?array {
    $cacheKey = 'ai:' . $bucket . '/' . $name;
    $cached = ttaCacheRead($cacheKey);
    if ($cached['hit']) return is_array($cached['value']) ? $cached['value'] : null;
    $root = ttaAiDriveRoot();
    if (!$root) { ttaCacheSave($cacheKey, null, TTA_MISS_CACHE_SECONDS); return null; }
    $folder = ttaDriveNamed($root, $bucket, true);
    if (!$folder) { ttaCacheSave($cacheKey, null, TTA_MISS_CACHE_SECONDS); return null; }
    $entry = ttaDriveNamed((string)$folder['id'], $name, false);
    if (!$entry) { ttaCacheSave($cacheKey, null, TTA_MISS_CACHE_SECONDS); return null; }
    if (!isset($entry['size']) || !ctype_digit((string)$entry['size'])) {
        throw new RuntimeException('drive_file_size_missing');
    }
    $file = ['id'=>(string)$entry['id'], 'size'=>(int)$entry['size']];
    ttaCacheSave($cacheKey, $file, TTA_LOOKUP_CACHE_SECONDS);
    return $file;
}


/** Read a flat Drive bucket to build the public cache catalog without IIS/Python. */
function ttaCatalogFolderFiles(string $folderId): array {
    if (!preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $folderId)) {
        throw new RuntimeException('invalid_catalog_folder');
    }
    $key = 'catalog-folder-v1:' . $folderId;
    $cache = ttaCacheRead($key);
    if ($cache['hit'] && is_array($cache['value'])) return $cache['value'];

    $items = [];
    $pageToken = '';
    $token = ttaAccessToken();
    for ($page = 0; $page < 30; $page++) {
        $params = [
            'q' => "'" . $folderId . "' in parents and trashed = false and mimeType != '" . TTA_DRIVE_FOLDER_MIME . "'",
            'pageSize' => 1000,
            'fields' => 'nextPageToken,files(id,name,size,mimeType)',
            'spaces' => 'drive',
        ];
        if ($pageToken !== '') $params['pageToken'] = $pageToken;
        $url = 'https://www.googleapis.com/drive/v3/files?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986);
        $payload = null;
        $refreshed = false;
        for ($attempt = 0; $attempt < 3; $attempt++) {
            $ch = curl_init($url);
            curl_setopt_array($ch, [
                CURLOPT_HTTPGET => true,
                CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $token, 'Accept: application/json'],
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_CONNECTTIMEOUT => 8,
                CURLOPT_TIMEOUT => 26,
                CURLOPT_SSL_VERIFYPEER => true,
                CURLOPT_SSL_VERIFYHOST => 2,
                CURLOPT_FOLLOWLOCATION => false,
            ]);
            $body = curl_exec($ch);
            $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
            $errno = curl_errno($ch);
            curl_close($ch);
            if ($status === 401 && !$refreshed) {
                $token = ttaAccessToken(true, $token);
                $refreshed = true;
                continue;
            }
            if (in_array($status, [408,429,500,502,503,504], true) && $attempt < 2) {
                usleep(250000 * ($attempt + 1));
                continue;
            }
            if ($errno !== 0 || $status !== 200 || !is_string($body)) {
                throw new RuntimeException('catalog_drive_http_' . $status);
            }
            $payload = json_decode($body, true);
            if (!is_array($payload) || !isset($payload['files']) || !is_array($payload['files'])) {
                throw new RuntimeException('catalog_drive_response_invalid');
            }
            break;
        }
        if (!is_array($payload)) throw new RuntimeException('catalog_drive_retries_exhausted');
        foreach ($payload['files'] as $item) {
            if (!is_array($item) || !is_string($item['name'] ?? null)) continue;
            $name = (string)$item['name'];
            if ($name === '' || strlen($name) > 240) continue;
            $items[] = ['name' => $name, 'size' => (int)($item['size'] ?? 0)];
            if (count($items) > 30000) throw new RuntimeException('catalog_too_large');
        }
        $next = (string)($payload['nextPageToken'] ?? '');
        if ($next === '') {
            ttaCacheSave($key, $items, TTA_LOOKUP_CACHE_SECONDS);
            return $items;
        }
        if ($next === $pageToken) throw new RuntimeException('catalog_page_stalled');
        $pageToken = $next;
    }
    throw new RuntimeException('catalog_page_limit_reached');
}

/**
 * Public index compatible with the existing Python /api/get_cached_songs JSON.
 * No media URLs or Drive IDs are returned, only user-visible song metadata.
 */
function ttaCatalogFromNames(array $audioFiles, array $lyricsFiles): array {
    $zingAudio = [];
    $youtubeAudio = [];
    foreach ($audioFiles as $file) {
        $filename = (string)($file['name'] ?? '');
        $size = (int)($file['size'] ?? 0);
        if ($size < 4096) continue;
        if (preg_match('/^zing_acapella_([A-Za-z0-9]{6,64})\.(?:ogg|mp3|wav|m4a)$/iD', $filename, $m)) {
            $zingAudio[$m[1]] = true;
        } elseif (preg_match('/^(?:acapella|beat_karaoke)_([A-Za-z0-9_-]{11})\.(?:ogg|mp3|wav|m4a)$/iD', $filename, $m)) {
            $youtubeAudio[$m[1]] = true;
        }
    }

    $songs = [];
    $zingWithMetadata = [];
    $youtubeLyrics = [];
    $youtubeMeta = [];
    $zingSimpleLyrics = [];
    foreach ($lyricsFiles as $file) {
        $filename = (string)($file['name'] ?? '');
        $size = (int)($file['size'] ?? 0);
        if ($size <= 0 || $size > 2 * 1024 * 1024) continue;
        if (preg_match('/^lyrics_([A-Za-z0-9_-]{11})\.json$/iD', $filename, $m)) {
            $youtubeLyrics[$m[1]] = true;
        } elseif (preg_match('/^meta_([A-Za-z0-9_-]{11})\.json$/iD', $filename, $m)) {
            $youtubeMeta[$m[1]] = true;
        } elseif (preg_match('/^([A-Za-z0-9]{6,64})_(_?[^_]+)_(.+)\.json$/uD', $filename, $m)) {
            $id = $m[1];
            // Preserve a leading underscore in official song titles such as _WORLD.
            $titlePart = (string)$m[2];
            $title = (substr($titlePart, 0, 1) === '_' ? '_' : '') .
                     trim(str_replace('_', ' ', ltrim($titlePart, '_')));
            $singer = trim(str_replace('_', ' ', $m[3]));
            $key = 'z:' . $id . ':' . hash('sha256', $title . "\0" . $singer);
            $zingWithMetadata[$id] = true;
            $songs[$key] = [
                'id'=>$id, 'title'=>$title, 'singer'=>$singer, 'source'=>'zing',
                'has_audio_cache'=>isset($zingAudio[$id]), 'has_lyrics_cache'=>true,
                'youtube_url'=>'',
            ];
        } elseif (preg_match('/^([A-Za-z0-9]{6,64})\.json$/D', $filename, $m)) {
            $zingSimpleLyrics[$m[1]] = true;
        }
    }
    // Multiple singers / karaoke JSON variants of the same Zing ID are separate
    // list rows, matching the original Python get_cached_songs behavior.
    foreach (array_unique(array_merge(array_keys($zingAudio), array_keys($zingSimpleLyrics))) as $id) {
        if (isset($zingWithMetadata[$id])) continue;
        $songs['z:' . $id] = [
            'id'=>$id, 'title'=>'Zing ' . $id, 'singer'=>'', 'source'=>'zing',
            'has_audio_cache'=>isset($zingAudio[$id]),
            'has_lyrics_cache'=>isset($zingSimpleLyrics[$id]), 'youtube_url'=>'',
        ];
    }
    foreach (array_unique(array_merge(array_keys($youtubeAudio), array_keys($youtubeLyrics), array_keys($youtubeMeta))) as $id) {
        $songs['y:' . $id] = [
            'id'=>$id, 'title'=>'YouTube ' . $id, 'singer'=>'', 'source'=>'youtube',
            'has_audio_cache'=>isset($youtubeAudio[$id]),
            'has_lyrics_cache'=>isset($youtubeLyrics[$id]),
            'youtube_url'=>'https://www.youtube.com/watch?v=' . rawurlencode($id),
        ];
    }
    $result = array_values($songs);
    usort($result, static function(array $a, array $b): int {
        $aReady = ($a['has_audio_cache'] ? 0 : 1);
        $bReady = ($b['has_audio_cache'] ? 0 : 1);
        if ($aReady !== $bReady) return $aReady - $bReady;
        $titleOrder = strcasecmp((string)$a['title'], (string)$b['title']);
        return $titleOrder ?: strcasecmp((string)$a['singer'], (string)$b['singer']);
    });
    return $result;
}
function ttaCachedSongsCatalog(): array {
    $key = 'cached-songs-v2-underscore-title';
    $cached = ttaCacheRead($key);
    if ($cached['hit'] && is_array($cached['value'])) return $cached['value'];
    $root = ttaAiDriveRoot();
    if (!$root) throw new RuntimeException('catalog_drive_root_missing');
    $audioFolder = ttaDriveNamed($root, 'stream_cache', true);
    $lyricsFolder = ttaDriveNamed($root, 'lyrics', true);
    if (!$audioFolder || !$lyricsFolder) throw new RuntimeException('catalog_drive_folders_missing');
    $audio = ttaCatalogFolderFiles((string)$audioFolder['id']);
    $lyrics = ttaCatalogFolderFiles((string)$lyricsFolder['id']);
    $songs = ttaCatalogFromNames($audio, $lyrics);
    $result = [
        'ok'=>true, 'source'=>'google_drive', 'songs'=>$songs,
        'count'=>count($songs),
        'audio_entries_scanned'=>count($audio),
        'json_entries_scanned'=>count($lyrics),
        'cache_ttl_seconds'=>TTA_LOOKUP_CACHE_SECONDS,
    ];
    ttaCacheSave($key, $result, TTA_LOOKUP_CACHE_SECONDS);
    return $result;
}

function ttaMediaHeaders(int $status, string $mime, string $name, bool $download, array $h): void {
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
function ttaStreamAttempt(string $id, string $accessToken, string $mime, string $filename,
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
                ttaMediaHeaders($code, $mime, $filename, $download, $headers);
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
function ttaStream(array $file, string $mime, string $filename, bool $download): void {
    if (!preg_match('/^[A-Za-z0-9_-]{8,200}$/D', (string)($file['id'] ?? ''))) ttaError('missing_drive_file', 404);
    $range = null;
    if (isset($_SERVER['HTTP_RANGE'])) {
        $range = trim((string)$_SERVER['HTTP_RANGE']);
        if (!preg_match('/^bytes=([0-9]*)-([0-9]*)$/D', $range, $matches) ||
            ($matches[1] === '' && $matches[2] === '')) ttaError('invalid_range', 416);
    }
    if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'HEAD') {
        ttaMediaHeaders(200, $mime, $filename, $download, ['content-length' => (string)($file['size'] ?? 0)]);
        exit;
    }
    @ini_set('zlib.output_compression', '0');
    @ignore_user_abort(false);
    @set_time_limit(0);
    while (ob_get_level() > 0) @ob_end_clean();
    $accessToken = ttaAccessToken();
    $refreshed = false;
    for ($i = 0; $i < 3; $i++) {
        $result = ttaStreamAttempt((string)$file['id'], $accessToken, $mime, $filename, $download, $range);
        if ($result['sent']) exit; // Never retry after media was partially delivered.
        if ($result['ok'] && in_array($result['status'], [200,206], true) && !$result['range_ignored']) {
            ttaMediaHeaders($result['status'], $mime, $filename, $download, $result['headers']);
            exit;
        }
        if ($result['status'] === 401 && !$refreshed) {
            $accessToken = ttaAccessToken(true, $accessToken); $refreshed = true;
            continue;
        }
        if (in_array($result['status'], [429,500,502,503,504], true) && $i < 2) {
            usleep(250000 * ($i+1)); continue;
        }
        ttaLog('Google Drive media unavailable; HTTP ' . $result['status'] . '; errno ' . $result['errno']);
        ttaError('google_drive_media_unavailable', 502, ['google_http'=>$result['status']]);
    }
    ttaError('google_drive_media_unavailable', 502);
}


function ttaFilename(string $name, string $kind): string {
    if ($name === '' || strlen($name) > 220 || strpos($name, '/') !== false ||
        strpos($name, "\\") !== false || strpos($name, "\0") !== false || $name[0] === '.') {
        ttaError('invalid_filename', 400);
    }
    if ($kind === 'lyrics') {
        if (!preg_match('/^[\pL\pN][\pL\pN ._()\-+]{0,180}\.json$/iuD', $name)) {
            ttaError('invalid_json_filename', 400);
        }
    } else {
        if (!preg_match('/^[\pL\pN][\pL\pN ._()\-+]{0,180}\.(wav|mp3|m4a|aac|ogg|flac|opus|webm)$/iuD', $name)) {
            ttaError('invalid_audio_filename', 400);
        }
    }
    return $name;
}
function ttaMime(string $name): string {
    $ext = strtolower(pathinfo($name, PATHINFO_EXTENSION));
    $map = ['wav'=>'audio/wav','mp3'=>'audio/mpeg','m4a'=>'audio/mp4',
            'aac'=>'audio/aac','ogg'=>'audio/ogg','flac'=>'audio/flac',
            'opus'=>'audio/ogg','webm'=>'audio/webm','json'=>'application/json; charset=utf-8'];
    if (!isset($map[$ext])) ttaError('unsupported_extension', 415);
    return $map[$ext];
}
function ttaStreamUrl(string $action, string $filename): string {
    // Fixed canonical app host, not derived from an untrusted Host header.
    return 'https://tuner.alwaysdata.net/api_ai_media_drive.php?action=' . rawurlencode($action) .
           '&filename=' . rawurlencode($filename);
}
$method = (string)($_SERVER['REQUEST_METHOD'] ?? 'GET');
if ($method === 'OPTIONS') { http_response_code(204); exit; }
if (!in_array($method, ['GET','HEAD'], true)) ttaError('method_not_allowed', 405);
$action = strtolower(trim((string)($_GET['action'] ?? 'health')));
if ($action === 'health') {
    try {
        $root = ttaAiDriveRoot();
        $audio = $root ? ttaDriveNamed($root, 'stream_cache', true) : null;
        $lyrics = $root ? ttaDriveNamed($root, 'lyrics', true) : null;
        ttaJson(200, ['ok'=>true, 'service'=>'tunertools_ai_drive_media',
            'mode'=>'single_php_live_lookup', 'drive_root_ready'=>($root !== null),
            'audio_folder_ready'=>($audio !== null), 'lyrics_folder_ready'=>($lyrics !== null),
            'cache_ttl_seconds'=>TTA_LOOKUP_CACHE_SECONDS, 'source'=>'google_drive_via_alwaysdata',
            'note'=>'health only checks folders, not an actual media file']);
    } catch (Throwable $e) { ttaLog('health failed ' . substr($e->getMessage(), 0, 60)); ttaError('drive_health_unavailable'); }
}
if ($action === 'cached_songs') {
    try { ttaJson(200, ttaCachedSongsCatalog()); }
    catch (Throwable $e) {
        ttaLog('cached_songs failed ' . substr($e->getMessage(), 0, 90));
        ttaError('drive_catalog_unavailable');
    }
}
if (!in_array($action, ['lookup','stream','lyrics'], true)) ttaError('unsupported_action', 400);
$kind = ($action === 'lyrics' || ($action === 'lookup' && ($_GET['kind'] ?? '') === 'lyrics')) ? 'lyrics' : 'audio';
$filename = ttaFilename((string)($_GET['filename'] ?? ''), $kind);
$bucket = $kind === 'lyrics' ? 'lyrics' : 'stream_cache';
try { $entry = ttaAiFile($bucket, $filename); }
catch (Throwable $e) { ttaLog('lookup failed ' . substr($e->getMessage(), 0, 70)); ttaError('drive_lookup_unavailable'); }
if ($action === 'lookup') {
    ttaJson(200, [
        'ok'=>true, 'found'=>($entry !== null),
        'source'=>($entry !== null ? 'google_drive' : 'not_found'),
        'stream_url'=>($entry !== null ? ttaStreamUrl($kind === 'lyrics' ? 'lyrics' : 'stream', $filename) : null),
        'size_bytes'=>($entry !== null ? $entry['size'] : null),
    ]);
}
if ($entry === null) ttaError('file_not_in_google_drive', 404);
if ($kind === 'lyrics' && $entry['size'] > 2*1024*1024) ttaError('lyrics_json_too_large', 413);
ttaStream($entry, ttaMime($filename), $filename, false);
