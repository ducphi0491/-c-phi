<?php
/**
 * TunerTools guide video catalog, PHP 7.4+.
 * Put beside app-guides/ in the same web root as api_songs.php.
 * GET api_guides.php?category=songs&lang=ja
 *
 * Each lesson has ONE video.mp4; lang chooses a separate WebVTT subtitle.
 * Video and VTT files are served directly by HTTPS, never streamed by PHP.
 * This endpoint reads public folders only and never accepts paths from users.
 */
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-cache, no-store, must-revalidate');
header('X-Content-Type-Options: nosniff');

const GUIDE_BASE_URL = 'https://tunertools.top/app-guides';
const GUIDE_CATEGORIES = ['songs', 'chords', 'tuner', 'metronome', 'training', 'documents', 'settings'];
const GUIDE_LANGUAGES = ['vi', 'en', 'es', 'fr', 'de', 'ru', 'ja', 'zh', 'th', 'lo', 'km', 'ko', 'pt', 'sw', 'ar'];
// Support both layouts: api_guides.php next to app-guides/ OR inside app-guides/
// (as in the owner's current C:/inetpub/wwwroot/tunertools/app-guides folder).
// GUIDE_BASE_URL stays the same in both cases.
define('GUIDE_DIR', is_dir(__DIR__ . '/songs') ? __DIR__ : __DIR__ . '/app-guides');

function guidesReply(int $status, array $payload): void {
    http_response_code($status);
    echo json_encode(
        $payload,
        JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE
    );
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') !== 'GET') {
    header('Allow: GET');
    guidesReply(405, ['error' => 'method_not_allowed']);
}

function guidesNormalizedLang(string $raw): string {
    $raw = strtolower(trim($raw));
    $root = preg_split('/[-_]/', $raw)[0] ?? '';
    return in_array($root, GUIDE_LANGUAGES, true) ? $root : 'en';
}

$requestedLang = guidesNormalizedLang((string) ($_GET['lang'] ?? 'en'));
$categoryFilter = strtolower(trim((string) ($_GET['category'] ?? 'all')));
if ($categoryFilter !== 'all' && !in_array($categoryFilter, GUIDE_CATEGORIES, true)) {
    guidesReply(400, ['error' => 'unknown_category']);
}

/** Never allow a path segment from untrusted filenames without validation. */
function guidesSafeSlug(string $name): bool {
    return preg_match('/^[a-z0-9][a-z0-9_-]{0,63}$/D', $name) === 1;
}

function guidesReadMetadata(string $path): array {
    if (!is_file($path) || filesize($path) > 65536) {
        return [];
    }
    $raw = @file_get_contents($path);
    if ($raw === false) {
        return [];
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

function guidesLocalizedText($data, string $lang, string $fallback): string {
    if (!is_array($data)) {
        return $fallback;
    }
    foreach (array_unique([$lang, 'en', 'vi']) as $candidate) {
        if (isset($data[$candidate]) && is_string($data[$candidate])) {
            $value = trim($data[$candidate]);
            if ($value !== '') {
                return substr($value, 0, 400);
            }
        }
    }
    return $fallback;
}

function guidesValidSubtitle(string $path): bool {
    if (!is_file($path) || (int) @filesize($path) < 20) {
        return false;
    }
    $fp = @fopen($path, 'rb');
    if ($fp === false) {
        return false;
    }
    $first = fread($fp, 2048);
    fclose($fp);
    if (!is_string($first)) {
        return false;
    }
    $first = preg_replace('/^\xEF\xBB\xBF/', '', $first);
    // Reject blank template VTT files. Captions must have at least one cue.
    return strncmp($first, 'WEBVTT', 6) === 0 && strpos($first, '-->') !== false;
}

function guidesPublicUrl(string ...$parts): string {
    return GUIDE_BASE_URL . '/' . implode('/', array_map('rawurlencode', $parts));
}

$allGuides = [];
$categories = $categoryFilter === 'all' ? GUIDE_CATEGORIES : [$categoryFilter];
foreach ($categories as $category) {
    $categoryDir = GUIDE_DIR . '/' . $category;
    if (!is_dir($categoryDir)) {
        continue;
    }
    $lessonDirs = @scandir($categoryDir);
    if (!is_array($lessonDirs)) {
        continue;
    }
    foreach ($lessonDirs as $slug) {
        if (!guidesSafeSlug($slug)) {
            continue;
        }
        $lessonDir = $categoryDir . '/' . $slug;
        if (!is_dir($lessonDir) || is_link($lessonDir)) {
            continue;
        }

        // One screen recording per lesson, regardless of requested language.
        $videoPath = $lessonDir . '/videos/video.mp4';
        if (!is_file($videoPath) || is_link($videoPath) || (int) @filesize($videoPath) <= 0) {
            continue;
        }

        $availableSubtitleLanguages = [];
        foreach (GUIDE_LANGUAGES as $subLang) {
            $vttPath = $lessonDir . '/subtitles/' . $subLang . '.vtt';
            if (guidesValidSubtitle($vttPath) && !is_link($vttPath)) {
                $availableSubtitleLanguages[] = $subLang;
            }
        }

        $metadata = guidesReadMetadata($lessonDir . '/guide.json');
        // The recording stays the same for EVERY language. Only WebVTT changes.
        $audioLangRaw = (string) ($metadata['audio_language'] ?? 'vi');
        $audioLang = guidesNormalizedLang($audioLangRaw);
        $chosenSubLang = null;
        foreach (array_unique([$requestedLang, $audioLang, 'en', 'vi']) as $subLang) {
            if (in_array($subLang, $availableSubtitleLanguages, true)) {
                $chosenSubLang = $subLang;
                break;
            }
        }
        $defaultTitle = ucwords(str_replace(['_', '-'], ' ', $slug));
        $order = $metadata['order'] ?? 1000;
        $order = is_numeric($order) ? (int) $order : 1000;
        $thumbnailUrl = null;
        foreach (['jpg', 'png', 'webp'] as $ext) {
            $image = $lessonDir . '/thumbnail.' . $ext;
            if (is_file($image) && !is_link($image)) {
                $thumbnailUrl = guidesPublicUrl($category, $slug, 'thumbnail.' . $ext);
                break;
            }
        }

        $allGuides[] = [
            'id' => $category . '/' . $slug,
            'category' => $category,
            'order' => $order,
            'title' => guidesLocalizedText($metadata['titles'] ?? null, $requestedLang, $defaultTitle),
            'description' => guidesLocalizedText($metadata['descriptions'] ?? null, $requestedLang, ''),
            'audio_language' => $audioLang,
            'video_url' => guidesPublicUrl($category, $slug, 'videos', 'video.mp4'),
            'subtitle_language' => $chosenSubLang,
            'subtitle_url' => $chosenSubLang === null
                ? null : guidesPublicUrl($category, $slug, 'subtitles', $chosenSubLang . '.vtt'),
            'subtitle_fallback' => $chosenSubLang !== null && $chosenSubLang !== $requestedLang,
            'available_subtitle_languages' => $availableSubtitleLanguages,
            'thumbnail_url' => $thumbnailUrl,
        ];
    }
}

// Sorted by category order, then lesson order, then stable lesson ID.
$categoryRank = array_flip(GUIDE_CATEGORIES);
usort($allGuides, static function (array $a, array $b) use ($categoryRank): int {
    return ($categoryRank[$a['category']] <=> $categoryRank[$b['category']])
        ?: ($a['order'] <=> $b['order'])
        ?: strcmp($a['id'], $b['id']);
});

guidesReply(200, [
    'api_version' => 2,
    'language' => $requestedLang,
    'category' => $categoryFilter,
    'total' => count($allGuides),
    'guides' => $allGuides,
]);
