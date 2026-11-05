<?php
/**
 * TunerTools - Google Drive AppGuides verification (READ ONLY)
 * Auto-discovers My Drive/TunerTools_AppBackup/app-guides created by the
 * Drive_Buoc2D Windows uploader; DOES NOT use the old browser-uploaded folder ID.
 * PHP 7.4+ with cURL; place beside api_gp_drive.php on alwaysdata.
 * GET ?action=check
 *
 * Optional: TUNERTOOLS_APP_GUIDES_FOLDER_ID environment variable overrides
 * discovery with an explicit new folder ID (never set it to the old ID).
 * Token resolution matches api_gp_drive.php: TUNERTOOLS_GP_TOKEN_PATH or
 * local drive_token.json. Never prints token contents.
 */
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');
header('X-Robots-Tag: noindex, nofollow');

const TT_FOLDER_MIME = 'application/vnd.google-apps.folder';
const TT_ROOT_NAME = 'TunerTools_AppBackup';
const TT_GUIDES_NAME = 'app-guides';
const TT_CATEGORIES = ['songs', 'chords', 'tuner', 'metronome', 'training', 'settings'];
const TT_OPTIONAL_CATEGORIES = ['documents'];
const TT_LANGUAGES = ['vi', 'en', 'es', 'fr', 'de', 'ru', 'ja', 'zh', 'th', 'lo', 'km', 'ko', 'pt', 'sw', 'ar'];
const TT_EXPECTED_LESSONS = 10;
const TT_EXPECTED_FILES = 180;
const TT_MAX_GOOGLE_CALLS = 160;

function ttReply(int $status, array $data): void {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES |
        JSON_INVALID_UTF8_SUBSTITUTE | JSON_PRETTY_PRINT);
    exit;
}
function ttFail(string $code, int $status = 503, array $extra = []): void {
    ttReply($status, array_merge(['ok' => false, 'error' => $code], $extra));
}
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') ttFail('get_only', 405);
if (($_GET['action'] ?? 'check') !== 'check') ttFail('invalid_action', 400);
if (!function_exists('curl_init')) ttFail('curl_required');

$tokenPath = getenv('TUNERTOOLS_GP_TOKEN_PATH');
if (!is_string($tokenPath) || trim($tokenPath) === '') $tokenPath = __DIR__ . '/drive_token.json';
if (!is_file($tokenPath) || !is_readable($tokenPath)) ttFail('drive_token_not_found');
$token = json_decode((string)@file_get_contents($tokenPath), true);
if (!is_array($token)) ttFail('drive_token_invalid_json');

function ttRefresh(array $token): string {
    foreach (['refresh_token', 'client_id', 'client_secret'] as $key) {
        if (empty($token[$key])) ttFail('oauth_refresh_configuration_missing');
    }
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'refresh_token' => (string)$token['refresh_token'],
            'client_id' => (string)$token['client_id'],
            'client_secret' => (string)$token['client_secret'],
            'grant_type' => 'refresh_token',
        ]),
        CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded'],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 8,
        CURLOPT_TIMEOUT => 20,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    $body = curl_exec($ch);
    $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $errno = curl_errno($ch);
    curl_close($ch);
    $decoded = is_string($body) ? json_decode($body, true) : null;
    if ($errno !== 0 || $status !== 200 || !is_array($decoded) || empty($decoded['access_token'])) {
        ttFail('oauth_refresh_failed', 503, ['google_http' => $status, 'curl_errno' => $errno]);
    }
    return (string)$decoded['access_token'];
}

$expiry = trim((string)($token['expiry'] ?? ''));
if ($expiry !== '' && !preg_match('/(?:Z|[+-][0-9]{2}:[0-9]{2})$/i', $expiry)) $expiry .= 'Z';
$expiresAt = $expiry !== '' ? strtotime($expiry) : false;
$accessToken = (string)($token['token'] ?? ($token['access_token'] ?? ''));
$refreshed = false;
if ($accessToken === '' || $expiresAt === false || $expiresAt <= time() + 120) {
    $accessToken = ttRefresh($token);
    $refreshed = true;
}
$googleCalls = 0;

function ttDriveGet(string $url): array {
    global $accessToken, $token, $refreshed, $googleCalls;
    if (++$googleCalls > TT_MAX_GOOGLE_CALLS) ttFail('google_request_limit');
    for ($attempt = 0; $attempt < 2; $attempt++) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HTTPHEADER => ['Authorization: Bearer ' . $accessToken, 'Accept: application/json'],
            CURLOPT_CONNECTTIMEOUT => 8,
            CURLOPT_TIMEOUT => 20,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_FOLLOWLOCATION => false,
        ]);
        $body = curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $errno = curl_errno($ch);
        curl_close($ch);
        if ($status === 401 && !$refreshed) {
            $accessToken = ttRefresh($token);
            $refreshed = true;
            continue;
        }
        if ($errno !== 0 || $status < 200 || $status >= 300) {
            ttFail('drive_request_failed', 503, [
                'google_http' => $status,
                'curl_errno' => $errno,
                'hint' => $status === 404 ? 'Drive folder ID unavailable to this token.' :
                    ($status === 403 ? 'Drive denied this request.' : 'Drive request failed.'),
            ]);
        }
        $result = is_string($body) ? json_decode($body, true) : null;
        if (!is_array($result)) ttFail('drive_invalid_json');
        return $result;
    }
    ttFail('drive_authentication_failed');
}
function ttValidId(string $id): string {
    if (!preg_match('/^[A-Za-z0-9_-]{8,200}$/D', $id)) ttFail('invalid_folder_id', 400);
    return $id;
}
function ttDriveInfo(string $id): array {
    return ttDriveGet('https://www.googleapis.com/drive/v3/files/' . rawurlencode(ttValidId($id)) . '?' .
        http_build_query(['fields' => 'id,name,mimeType,trashed,parents', 'supportsAllDrives' => 'true'], '', '&', PHP_QUERY_RFC3986));
}
function ttDriveList(string $q): array {
    $result = [];
    $page = '';
    $pageCount = 0;
    do {
        $params = [
            'q' => $q,
            'fields' => 'nextPageToken,files(id,name,mimeType,trashed,size,parents)',
            'pageSize' => '1000',
            'spaces' => 'drive',
        ];
        if ($page !== '') $params['pageToken'] = $page;
        $data = ttDriveGet('https://www.googleapis.com/drive/v3/files?' .
            http_build_query($params, '', '&', PHP_QUERY_RFC3986));
        foreach (($data['files'] ?? []) as $file) {
            if (is_array($file) && isset($file['id'], $file['name'], $file['mimeType'])) $result[] = $file;
        }
        $page = (string)($data['nextPageToken'] ?? '');
        $pageCount++;
        if ($pageCount > 10 || count($result) > 5000) ttFail('drive_listing_too_large');
    } while ($page !== '');
    return $result;
}
function ttChildren(string $folderId): array {
    return ttDriveList("'" . ttValidId($folderId) . "' in parents and trashed = false");
}
function ttNamedFolders(array $children, string $name): array {
    return array_values(array_filter($children, static function (array $item) use ($name): bool {
        return (string)$item['name'] === $name && (string)$item['mimeType'] === TT_FOLDER_MIME;
    }));
}
function ttOneFolder(array $children, string $name, string $errorCode): array {
    $found = ttNamedFolders($children, $name);
    if (count($found) !== 1) {
        ttFail($errorCode, 503, ['name' => $name, 'matching_folders' => count($found)]);
    }
    return $found[0];
}

// No old hard-coded ID. Discover the new app-owned root created by the uploader.
$explicitId = trim((string)(getenv('TUNERTOOLS_APP_GUIDES_FOLDER_ID') ?: ''));
if ($explicitId !== '') {
    $rootId = ttValidId($explicitId);
    $root = ttDriveInfo($rootId);
    $rootSource = 'environment_variable';
} else {
    $rootFolders = ttDriveList("name = '" . TT_ROOT_NAME . "' and mimeType = '" . TT_FOLDER_MIME .
        "' and 'root' in parents and trashed = false");
    if (count($rootFolders) !== 1) {
        ttFail('backup_root_not_found_or_ambiguous', 503, [
            'root_name' => TT_ROOT_NAME,
            'matching_folders' => count($rootFolders),
            'hint' => 'The uploader creates this folder before uploading any files. Check that alwaysdata uses the same OAuth client/account.',
        ]);
    }
    $rootFolder = $rootFolders[0];
    $child = ttOneFolder(ttChildren((string)$rootFolder['id']), TT_GUIDES_NAME,
        'app_guides_not_found_or_ambiguous');
    $rootId = (string)$child['id'];
    $root = ttDriveInfo($rootId);
    $rootSource = 'auto_discovered_new_backup';
}
if (($root['mimeType'] ?? '') !== TT_FOLDER_MIME || ($root['name'] ?? '') !== TT_GUIDES_NAME || !empty($root['trashed'])) {
    ttFail('unexpected_root_folder', 503, [
        'hint' => 'Expected the new TunerTools_AppBackup/app-guides folder, not the old manual upload folder.',
    ]);
}

$found = ['video_mp4' => 0, 'subtitle_vtt' => 0, 'guide_json' => 0, 'thumbnail' => 0];
$lessonTotal = 0;
$warnings = [];
$categories = [];
$rootChildren = ttChildren($rootId);
foreach (array_merge(TT_CATEGORIES, TT_OPTIONAL_CATEGORIES) as $category) {
    $catFolders = ttNamedFolders($rootChildren, $category);
    if (count($catFolders) !== 1) {
        $categories[$category] = ['exists' => false, 'lessons' => 0];
        if (in_array($category, TT_CATEGORIES, true)) {
            $warnings[] = "Missing or ambiguous category: {$category}";
        }
        continue;
    }
    $lessons = array_values(array_filter(ttChildren((string)$catFolders[0]['id']), static function (array $f): bool {
        return (string)$f['mimeType'] === TT_FOLDER_MIME;
    }));
    $categories[$category] = ['exists' => true, 'lessons' => count($lessons)];
    $lessonTotal += count($lessons);
    foreach ($lessons as $lesson) {
        $base = $category . '/' . (string)$lesson['name'];
        $children = ttChildren((string)$lesson['id']);
        $vidFolders = ttNamedFolders($children, 'videos');
        $subFolders = ttNamedFolders($children, 'subtitles');
        $videoItems = count($vidFolders) === 1 ? ttChildren((string)$vidFolders[0]['id']) : [];
        $subtitleItems = count($subFolders) === 1 ? ttChildren((string)$subFolders[0]['id']) : [];

        $videoCount = 0;
        foreach ($videoItems as $f) {
            if ((string)$f['name'] === 'video.mp4' && (string)$f['mimeType'] !== TT_FOLDER_MIME) $videoCount++;
        }
        $found['video_mp4'] += $videoCount;
        if ($videoCount !== 1) $warnings[] = "{$base}: video.mp4 count = {$videoCount}";

        $langs = [];
        foreach ($subtitleItems as $f) {
            $name = (string)$f['name'];
            if ((string)$f['mimeType'] !== TT_FOLDER_MIME && preg_match('/^([a-z]{2})\.vtt$/D', $name, $m) &&
                in_array($m[1], TT_LANGUAGES, true)) {
                $langs[] = $m[1];
            }
        }
        $found['subtitle_vtt'] += count($langs);
        if (count($langs) !== 15 || count(array_unique($langs)) !== 15) {
            $warnings[] = "{$base}: subtitles = " . count($langs) . '/15';
        }

        $metadata = 0;
        $thumbs = 0;
        foreach ($children as $f) {
            if ((string)$f['name'] === 'guide.json' && (string)$f['mimeType'] !== TT_FOLDER_MIME) $metadata++;
            if ((string)$f['mimeType'] !== TT_FOLDER_MIME &&
                preg_match('/^thumbnail\.(jpg|png|webp)$/D', (string)$f['name'])) $thumbs++;
        }
        $found['guide_json'] += $metadata;
        $found['thumbnail'] += $thumbs;
        if ($metadata !== 1) $warnings[] = "{$base}: guide.json count = {$metadata}";
        if ($thumbs !== 1) $warnings[] = "{$base}: thumbnail count = {$thumbs}";
    }
}
$totalFiles = array_sum($found);
$complete = $lessonTotal === TT_EXPECTED_LESSONS &&
    $totalFiles === TT_EXPECTED_FILES &&
    $found['video_mp4'] === 10 && $found['subtitle_vtt'] === 150 &&
    $found['guide_json'] === 10 && $found['thumbnail'] === 10 && empty($warnings);

ttReply(200, [
    'ok' => true,
    'complete' => $complete,
    'status' => $complete ? 'complete' : 'uploading_or_incomplete',
    'source' => $rootSource,
    'folder_id' => $rootId,
    'folder_name' => TT_ROOT_NAME . '/' . TT_GUIDES_NAME,
    'expected_files' => TT_EXPECTED_FILES,
    'found_files' => $totalFiles,
    'lessons_found' => $lessonTotal,
    'counts' => $found,
    'categories' => $categories,
    'warnings' => $warnings,
    'google_api_calls' => $googleCalls,
    'note' => 'READ ONLY: this PHP never changes the Drive files, permissions, or token. The catalog API for Android will be created separately.',
]);
