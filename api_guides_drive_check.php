<?php
/**
 * TunerTools Google Drive AppGuides verification (READ-ONLY)
 * PHP 7.4+, cURL, OpenSSL. Independent from api_gp_drive.php.
 * This script NEVER changes Drive files, permissions, OAuth tokens, or server files.
 * Upload beside api_gp_drive.php on alwaysdata, where drive_token.json is available.
 * GET ?action=check
 * V2: on exact-ID HTTP 404, discover the only app-guides folder whose
 * parent is named tunertools, using the SAME OAuth token already in use.
 * No Drive permission or OAuth scope is changed. Read-only verification only.
 */
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, max-age=0');
header('X-Content-Type-Options: nosniff');
header('X-Robots-Tag: noindex, nofollow');

const TT_GUIDES_ROOT_ID = '1v275O1J7W3EDnxKbBtoO0_sYZBdaMUkr';
const TT_FOLDER_MIME = 'application/vnd.google-apps.folder';
const TT_CATEGORIES = ['songs','chords','tuner','metronome','training','documents','settings'];
const TT_LANGUAGES = ['vi','en','es','fr','de','ru','ja','zh','th','lo','km','ko','pt','sw','ar'];
const TT_MAX_GOOGLE_CALLS = 120;

function ttReply(int $http, array $payload): void {
    http_response_code($http);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE | JSON_PRETTY_PRINT);
    exit;
}
function ttFail(string $code, int $status = 503, array $details = []): void {
    ttReply($status, array_merge(['ok'=>false, 'error'=>$code], $details));
}
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') ttFail('get_only', 405);
if (($_GET['action'] ?? 'check') !== 'check') ttFail('invalid_action', 400);
if (!function_exists('curl_init')) ttFail('php_curl_required');

$tokenPath = getenv('TUNERTOOLS_GP_TOKEN_PATH');
if (!is_string($tokenPath) || trim($tokenPath) === '') $tokenPath = __DIR__ . '/drive_token.json';
if (!is_file($tokenPath) || !is_readable($tokenPath)) ttFail('drive_token_not_found');
$token = json_decode((string)@file_get_contents($tokenPath), true);
if (!is_array($token)) ttFail('drive_token_bad_json');

function ttPostRefresh(array $token): string {
    foreach (['refresh_token','client_id','client_secret'] as $field) {
        if (empty($token[$field])) ttFail('oauth_refresh_configuration_missing');
    }
    $ch = curl_init('https://oauth2.googleapis.com/token');
    curl_setopt_array($ch, [
        CURLOPT_POST=>true,
        CURLOPT_POSTFIELDS=>http_build_query([
            'refresh_token'=>(string)$token['refresh_token'],
            'client_id'=>(string)$token['client_id'],
            'client_secret'=>(string)$token['client_secret'],
            'grant_type'=>'refresh_token',
        ]),
        CURLOPT_HTTPHEADER=>['Content-Type: application/x-www-form-urlencoded'],
        CURLOPT_RETURNTRANSFER=>true,
        CURLOPT_CONNECTTIMEOUT=>8,
        CURLOPT_TIMEOUT=>20,
        CURLOPT_SSL_VERIFYPEER=>true,
        CURLOPT_SSL_VERIFYHOST=>2,
    ]);
    $body = curl_exec($ch);
    $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $err = curl_errno($ch);
    curl_close($ch);
    $data = is_string($body) ? json_decode($body, true) : null;
    if ($err || $code !== 200 || !is_array($data) || empty($data['access_token'])) {
        ttFail('oauth_refresh_failed', 503, ['google_http'=>$code, 'curl_errno'=>$err]);
    }
    return (string)$data['access_token'];
}
$expiresRaw = trim((string)($token['expiry'] ?? ''));
if ($expiresRaw !== '' && !preg_match('/(?:Z|[+-][0-9]{2}:[0-9]{2})$/i', $expiresRaw)) $expiresRaw .= 'Z';
$expires = $expiresRaw !== '' ? strtotime($expiresRaw) : false;
$accessToken = (string)($token['token'] ?? ($token['access_token'] ?? ''));
$refreshed = false;
if ($accessToken === '' || $expires === false || $expires <= time() + 120) {
    $accessToken = ttPostRefresh($token);
    $refreshed = true;
}

$googleCalls = 0;
function ttDriveGet(string $url, bool $allowNotFound = false): array {
    global $accessToken, $token, $refreshed, $googleCalls;
    if (++$googleCalls > TT_MAX_GOOGLE_CALLS) ttFail('request_limit_reached', 503);
    for ($attempt = 0; $attempt < 2; $attempt++) {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER=>true,
            CURLOPT_HTTPHEADER=>['Authorization: Bearer ' . $accessToken, 'Accept: application/json'],
            CURLOPT_CONNECTTIMEOUT=>8,
            CURLOPT_TIMEOUT=>20,
            CURLOPT_SSL_VERIFYPEER=>true,
            CURLOPT_SSL_VERIFYHOST=>2,
            CURLOPT_FOLLOWLOCATION=>false,
        ]);
        $body = curl_exec($ch);
        $status = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_errno($ch);
        curl_close($ch);
        if ($status === 401 && !$refreshed) {
            $accessToken = ttPostRefresh($token);
            $refreshed = true;
            continue;
        }
        if ($error === 0 && $status === 404 && $allowNotFound) {
            return ['_google_http' => 404];
        }
        if ($error !== 0 || $status < 200 || $status >= 300) {
            ttFail('drive_request_failed', 503, [
                'google_http'=>$status,
                'curl_errno'=>$error,
                'hint'=>$status === 403 ? 'OAuth scope / file access not sufficient' :
                       ($status === 404 ? 'Folder missing, wrong ID, or unavailable to this OAuth app' : 'Drive request failed'),
            ]);
        }
        $decoded = is_string($body) ? json_decode($body, true) : null;
        if (!is_array($decoded)) ttFail('drive_response_invalid_json', 503);
        return $decoded;
    }
    ttFail('drive_authentication_failed');
}

function ttDriveInfo(string $id, bool $allowNotFound = false): array {
    if (!preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $id)) ttFail('invalid_folder_id', 400);
    return ttDriveGet('https://www.googleapis.com/drive/v3/files/' . rawurlencode($id) . '?' . http_build_query([
        'fields'=>'id,name,mimeType,trashed,parents', 'supportsAllDrives'=>'true'
    ], '', '&', PHP_QUERY_RFC3986), $allowNotFound);
}

/**
 * The ID in a screenshot can contain similar O/0/l/1 characters. On 404,
 * search visible folders by exact name and verify the parent name to avoid
 * accidentally using an unrelated app-guides directory.
 */
function ttDiscoverGuidesFolder(): array {
    $params = [
        'q' => "name = 'app-guides' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
        'fields' => 'nextPageToken,files(id,name,mimeType,parents,trashed)',
        'pageSize' => '100',
        'supportsAllDrives' => 'true',
        'includeItemsFromAllDrives' => 'true',
    ];
    $candidates = [];
    $page = '';
    $pages = 0;
    do {
        if ($page !== '') $params['pageToken'] = $page;
        $found = ttDriveGet('https://www.googleapis.com/drive/v3/files?' .
            http_build_query($params, '', '&', PHP_QUERY_RFC3986));
        foreach (($found['files'] ?? []) as $folder) {
            if (!is_array($folder) || !isset($folder['id']) ||
                ($folder['mimeType'] ?? '') !== TT_FOLDER_MIME) continue;
            $parents = $folder['parents'] ?? [];
            if (!is_array($parents)) continue;
            foreach ($parents as $parentId) {
                if (!is_string($parentId) || $parentId === '') continue;
                $parent = ttDriveInfo($parentId, true);
                if (($parent['name'] ?? '') === 'tunertools' &&
                    ($parent['mimeType'] ?? '') === TT_FOLDER_MIME &&
                    empty($parent['trashed'])) {
                    $candidates[(string)$folder['id']] = $folder;
                    break;
                }
            }
        }
        $page = (string)($found['nextPageToken'] ?? '');
        ++$pages;
    } while ($page !== '' && $pages < 3);

    if (count($candidates) !== 1) {
        ttFail('app_guides_root_not_accessible_or_ambiguous', 503, [
            'configured_folder_id' => TT_GUIDES_ROOT_ID,
            'configured_id_http' => 404,
            'matching_parent_verified_folders' => count($candidates),
            'hint' => count($candidates) === 0
                ? 'Cannot see tunertools/app-guides with the existing Drive token. Verify the copied folder link and which Drive account created it; no token or permission was changed.'
                : 'Multiple tunertools/app-guides folders are visible. Set TT_GUIDES_ROOT_ID to the correct Drive folder ID.',
        ]);
    }
    $folder = reset($candidates);
    return [(string)$folder['id'], $folder];
}

function ttDriveChildren(string $folderId): array {
    if (!preg_match('/^[a-zA-Z0-9_-]{8,200}$/D', $folderId)) ttFail('invalid_folder_id', 400);
    $result = [];
    $next = '';
    do {
        $q = "'" . $folderId . "' in parents and trashed = false";
        $params = [
            'q'=>$q,
            'fields'=>'nextPageToken,files(id,name,mimeType,size)',
            'pageSize'=>'1000',
            'supportsAllDrives'=>'true',
            'includeItemsFromAllDrives'=>'true',
        ];
        if ($next !== '') $params['pageToken'] = $next;
        $response = ttDriveGet('https://www.googleapis.com/drive/v3/files?' . http_build_query($params, '', '&', PHP_QUERY_RFC3986));
        foreach (($response['files'] ?? []) as $item) {
            if (is_array($item) && isset($item['id'], $item['name'], $item['mimeType'])) $result[] = $item;
        }
        $next = (string)($response['nextPageToken'] ?? '');
        if (count($result) > 3000) ttFail('folder_too_large', 503);
    } while ($next !== '');
    return $result;
}
function ttNamedFolder(array $files, string $name): ?array {
    foreach ($files as $f) {
        if ((string)$f['name'] === $name && (string)$f['mimeType'] === TT_FOLDER_MIME) return $f;
    }
    return null;
}

$rootId = TT_GUIDES_ROOT_ID;
$root = ttDriveInfo($rootId, true);
$source = 'configured_id';
if (($root['_google_http'] ?? 0) === 404) {
    [$rootId, $root] = ttDiscoverGuidesFolder();
    $source = 'discovered_by_name_and_parent';
}
if (($root['mimeType'] ?? '') !== TT_FOLDER_MIME || !empty($root['trashed'])) {
    ttFail('root_not_a_valid_folder', 503, [
        'hint' => 'Configured Drive ID exists but is not an accessible folder.'
    ]);
}
if (($root['name'] ?? '') !== 'app-guides') {
    ttFail('unexpected_folder_name', 503, [
        'hint' => 'Configured ID points to a folder not named app-guides.'
    ]);
}
$rootChildren = ttDriveChildren($rootId);
$categories = [];
$totalLessonFolders = 0;
$totalVideoSamplesChecked = 0;
$warnings = [];
foreach (TT_CATEGORIES as $category) {
    $cat = ttNamedFolder($rootChildren, $category);
    if ($cat === null) {
        $categories[$category] = ['exists'=>false, 'lesson_folders'=>0];
        $warnings[] = 'Category missing: ' . $category;
        continue;
    }
    $children = ttDriveChildren((string)$cat['id']);
    $lessons = array_values(array_filter($children, static function ($f): bool {
        return (string)($f['mimeType'] ?? '') === TT_FOLDER_MIME;
    }));
    usort($lessons, static function ($a, $b): int {
        return strcmp((string)$a['name'], (string)$b['name']);
    });
    $totalLessonFolders += count($lessons);
    $item = ['exists'=>true, 'lesson_folders'=>count($lessons)];
    // One sampled lesson/category: quick check, not a comprehensive inventory.
    if (!empty($lessons)) {
        $sample = $lessons[0];
        $lessonChildren = ttDriveChildren((string)$sample['id']);
        $videoFolder = ttNamedFolder($lessonChildren, 'videos');
        $subtitleFolder = ttNamedFolder($lessonChildren, 'subtitles');
        $videos = $videoFolder !== null ? ttDriveChildren((string)$videoFolder['id']) : [];
        $subtitles = $subtitleFolder !== null ? ttDriveChildren((string)$subtitleFolder['id']) : [];
        $hasMp4 = false;
        foreach ($videos as $v) {
            if ($v['name'] === 'video.mp4' && $v['mimeType'] !== TT_FOLDER_MIME) $hasMp4 = true;
        }
        $langs = [];
        foreach ($subtitles as $sub) {
            $name = (string)$sub['name'];
            if (preg_match('/^([a-z]{2})\.vtt$/D', $name, $match) && in_array($match[1], TT_LANGUAGES, true)) {
                $langs[] = $match[1];
            }
        }
        $langs = array_values(array_unique($langs));
        sort($langs);
        $hasMetadata = false;
        $hasThumb = false;
        foreach ($lessonChildren as $child) {
            if ($child['name'] === 'guide.json') $hasMetadata = true;
            if (preg_match('/^thumbnail\.(jpg|png|webp)$/D', (string)$child['name'])) $hasThumb = true;
        }
        $item['sample'] = [
            'lesson'=>(string)$sample['name'],
            'video_mp4'=>$hasMp4,
            'subtitle_languages'=>$langs,
            'guide_json'=>$hasMetadata,
            'thumbnail'=>$hasThumb,
        ];
        ++$totalVideoSamplesChecked;
        if (!$hasMp4) $warnings[] = $category . '/' . $sample['name'] . ': video.mp4 missing';
        if (count($langs) < 15) $warnings[] = $category . '/' . $sample['name'] . ': fewer than 15 subtitle files';
        if (!$hasMetadata) $warnings[] = $category . '/' . $sample['name'] . ': guide.json missing';
    }
    $categories[$category] = $item;
}

ttReply(200, [
    'ok'=>true,
    'service'=>'TunerTools Drive AppGuides CHECK ONLY',
    'root_name'=>(string)($root['name'] ?? ''),
    'root_id_used'=>$rootId,
    'root_source'=>$source,
    'configured_folder_id'=>TT_GUIDES_ROOT_ID,
    'category_status'=>$categories,
    'lesson_folders_total'=>$totalLessonFolders,
    'lesson_samples_checked'=>$totalVideoSamplesChecked,
    'warnings'=>$warnings,
    'google_api_calls'=>$googleCalls,
    'note'=>'READ-ONLY: no Drive file, permission, OAuth scope, or server content was changed. Sample checks are not a complete inventory.',
]);
