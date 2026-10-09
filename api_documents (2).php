<?php
/**
 * TunerTools learning-material API (single PHP + MySQLi access control).
 *
 * Install at:
 *   C:\inetpub\wwwroot\api_documents.php
 *
 * Public material root:
 *   C:\inetpub\wwwroot\document
 *
 * Guitar course root (JSON / images / video):
 *   C:\inetpub\wwwroot\guitar_HD
 *
 * Public guitar-course actions (read only, no MySQL login required):
 *   GET  action=course_manifest
 *   GET  action=course_asset&path=course_index.json
 *
 * Supported document actions:
 *   GET  action=list
 *   GET  action=view
 *   GET  action=download
 *   POST action=create_folder
 *   POST action=upload (multipart/form-data, field name: file)
 *   POST action=rename
 *   POST action=delete_item
 *
 * Compatibility:
 * If document/Guitar does not exist, Guitar reads and writes directly in
 * document/. Ukulele and Piano use their own folders.
 */

declare(strict_types=1);

ini_set('display_errors', '0');
ini_set('log_errors', '1');
ini_set('error_log', __DIR__ . DIRECTORY_SEPARATOR . 'api_documents_error.log');
error_reporting(E_ALL);

// Keep normal JSON responses buffered so a fatal PHP/FastCGI error cannot leak an
// IIS HTML page to Android. Binary view/download actions clear this buffer before
// streaming the file.
ob_start();
$GLOBALS['TT_DOCUMENT_JSON_SENT'] = false;
register_shutdown_function(static function (): void {
    $error = error_get_last();
    if (!is_array($error)) return;
    $fatalTypes = [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR, E_USER_ERROR, E_RECOVERABLE_ERROR];
    if (!in_array((int)($error['type'] ?? 0), $fatalTypes, true)) return;

    error_log('[TunerTools Documents fatal] ' . json_encode($error, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES));
    while (ob_get_level() > 0) {
        @ob_end_clean();
    }
    if (!headers_sent()) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
    }
    echo json_encode([
        'success' => false,
        'code' => 'fatal_server_error',
        'message' => 'Máy chủ gặp lỗi khi xử lý tài liệu. Vui lòng thử lại.',
    ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
});

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Accept, Range, X-Requested-With, Authorization, X-Document-Token');
header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
header('Pragma: no-cache');
header('X-TunerTools-Documents-Version: 2026-10-09-video-switch-v11');

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$documentRoot = __DIR__ . DIRECTORY_SEPARATOR . 'document';
$guitarCourseRoot = __DIR__ . DIRECTORY_SEPARATOR . 'guitar_HD';
$guitarCourseAllowedExtensions = [
    'json',
    'jpg', 'jpeg', 'png', 'webp', 'gif',
    'mp4', 'webm', 'mov', 'm4v', 'mkv',
    'mp3', 'm4a', 'aac', 'wav', 'ogg',
    'vtt',
];
$instrumentMap = [
    'guitar' => 'Guitar',
    'ukulele' => 'Ukulele',
    'piano' => 'Piano',
];

$imageExtensions = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'];
$pdfExtensions = ['pdf'];
$videoExtensions = ['mp4', 'webm', 'mov', 'm4v', 'mkv', 'avi'];
$allowedExtensions = array_merge($imageExtensions, $pdfExtensions, $videoExtensions);
$maxUploadBytes = 250 * 1024 * 1024; // 250 MB per file.


/*
 * --------------------------------------------------------------------------
 * MYSQL + GMAIL ACCESS CONFIGURATION
 * --------------------------------------------------------------------------
 * This build authorizes learning materials by the Gmail address of the
 * Google account already signed in on Android. No Google Web Client ID is needed.
 * Only this PHP file contains the MySQL credentials.
 */
// --- 1. Cấu hình kết nối MySQL (MySQLi) ---
$servername = '127.0.0.1';
$username = 'root';
$password = '123456';
$dbname = 'tuner';
$port = 3306;

$DB_CONFIG = [
    'host' => $servername,
    'port' => $port,
    'name' => $dbname,
    'user' => $username,
    'password' => $password,
];

$INITIAL_SUPER_ADMIN_GMAIL = 'tunertools.top@gmail.com';
$DOCUMENT_SESSION_SECRET = '76dae828edd511e2aebeb4afea005331f733a8e989c72ee234da4b618fe0db06fcdd52e86b0042db273d6babe3433a5ea90bee103b4870cc72875dd067d958d7';
$DOCUMENT_SESSION_DAYS = 30;
$DOCUMENT_TICKET_TTL_SECONDS = 900;
$USER_CONTRIBUTION_FOLDER = 'Cộng đồng';
$COMMUNITY_OWNER_METADATA_FILE = '.tunertools_community_owners.json';
$AUTO_CREATE_DOCUMENT_TABLES = true;
$REMOVE_LEGACY_DOCUMENT_TABLES = false;
$DOCUMENT_SCHEMA_MARKER_FILE = __DIR__ . DIRECTORY_SEPARATOR . '.tunertools_document_schema_v10.ready';
$DOCUMENT_SUMMARY_CACHE_DIR = __DIR__ . DIRECTORY_SEPARATOR . 'document_cache';
$DOCUMENT_SUMMARY_CACHE_TTL_SECONDS = 120;

// PHP-only compatibility for the already-published Android build.
// A valid Google account receives one server-side 30-day SongTab trial.
// Existing permanent/admin permissions are never shortened or overwritten.
$SONG_TAB_AUTO_TRIAL_ENABLED = true;
$SONG_TAB_TRIAL_DAYS = 30;
$SONG_TAB_TRIAL_NOTE_MARKER = '[AUTO_SONGTAB_TRIAL_30D]';
$SONG_TAB_TRIAL_CREATED_BY = 'song_tab_auto_trial';

function isPlaceholderValue(string $value): bool
{
    $value = trim($value);
    return $value === '' || stripos($value, 'REPLACE_') !== false || stripos($value, 'CHANGE_ME') !== false;
}

function mysqlConfigurationProblems(): array
{
    global $DB_CONFIG;

    $problems = [];
    foreach (['host', 'name', 'user', 'password'] as $key) {
        if (!isset($DB_CONFIG[$key]) || isPlaceholderValue((string)$DB_CONFIG[$key])) {
            $problems[] = 'DB_' . strtoupper($key);
        }
    }
    if (!extension_loaded('mysqli')) {
        $problems[] = 'MYSQLI_EXTENSION';
    }
    return array_values(array_unique($problems));
}

function documentAccessConfigurationProblems(): array
{
    global $INITIAL_SUPER_ADMIN_GMAIL, $DOCUMENT_SESSION_SECRET;

    $problems = [];
    if (!filter_var(strtolower(trim($INITIAL_SUPER_ADMIN_GMAIL)), FILTER_VALIDATE_EMAIL) ||
        isPlaceholderValue($INITIAL_SUPER_ADMIN_GMAIL)) {
        $problems[] = 'INITIAL_SUPER_ADMIN_GMAIL';
    }
    if (isPlaceholderValue($DOCUMENT_SESSION_SECRET) || strlen($DOCUMENT_SESSION_SECRET) < 48) {
        $problems[] = 'DOCUMENT_SESSION_SECRET';
    }
    return array_values(array_unique($problems));
}

function serverConfigurationProblems(): array
{
    return array_values(array_unique(array_merge(
        mysqlConfigurationProblems(),
        documentAccessConfigurationProblems()
    )));
}

function requireMysqlConfiguration(): void
{
    $problems = mysqlConfigurationProblems();
    if ($problems !== []) {
        sendJson([
            'success' => false,
            'code' => 'mysql_not_configured',
            'message' => 'Máy chủ chưa cấu hình MySQL cho quyền tài liệu.',
            'missing' => $problems,
        ], 503);
    }
}

function requireDocumentAccessConfiguration(): void
{
    $problems = documentAccessConfigurationProblems();
    if ($problems !== []) {
        sendJson([
            'success' => false,
            'code' => 'access_not_configured',
            'message' => 'Máy chủ chưa cấu hình Gmail quản trị hoặc session secret.',
            'missing' => $problems,
        ], 503);
    }
}

function requireServerConfiguration(): void
{
    $problems = serverConfigurationProblems();
    if ($problems !== []) {
        sendJson([
            'success' => false,
            'code' => 'server_not_configured',
            'message' => 'Máy chủ chưa cấu hình quyền tài liệu.',
            'missing' => $problems,
        ], 503);
    }
}

function normalizeEmailAddress(string $email): string
{
    return strtolower(trim($email));
}

function contributionFolderPath(): string
{
    global $USER_CONTRIBUTION_FOLDER;
    return normalizeRelativePath($USER_CONTRIBUTION_FOLDER);
}

function isContributionPath(string $relativePath): bool
{
    $relativePath = normalizeRelativePath($relativePath);
    $root = contributionFolderPath();
    return $relativePath === $root || ($root !== '' && strpos($relativePath, $root . '/') === 0);
}

function isContributionRootPath(string $relativePath): bool
{
    return normalizeRelativePath($relativePath) === contributionFolderPath();
}


/**
 * Ownership of community files/folders is kept in a hidden JSON manifest next
 * to the instrument folder. This deliberately avoids adding another MySQL
 * table: document_permissions remains the only database table used here.
 *
 * Format:
 * {
 *   "version": 1,
 *   "owners": {"Cộng đồng/My folder": "owner@gmail.com"}
 * }
 */
function communityOwnerMetadataPath(string $instrumentRoot): string
{
    global $COMMUNITY_OWNER_METADATA_FILE;
    return rtrim($instrumentRoot, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . $COMMUNITY_OWNER_METADATA_FILE;
}

function loadCommunityOwnerMap(string $instrumentRoot): array
{
    $path = communityOwnerMetadataPath($instrumentRoot);
    if (!is_file($path)) {
        return [];
    }
    $raw = @file_get_contents($path);
    if (!is_string($raw) || trim($raw) === '') {
        return [];
    }
    $decoded = json_decode($raw, true);
    $owners = is_array($decoded) && isset($decoded['owners']) && is_array($decoded['owners'])
        ? $decoded['owners']
        : [];

    $result = [];
    foreach ($owners as $pathKey => $email) {
        if (!is_string($pathKey) || !is_string($email)) continue;
        try {
            $cleanPath = normalizeRelativePath($pathKey);
        } catch (Throwable $ignored) {
            continue;
        }
        $cleanEmail = normalizeEmailAddress($email);
        if ($cleanPath !== '' && isContributionPath($cleanPath) && filter_var($cleanEmail, FILTER_VALIDATE_EMAIL)) {
            $result[$cleanPath] = $cleanEmail;
        }
    }
    return $result;
}

function saveCommunityOwnerMap(string $instrumentRoot, array $owners): void
{
    $clean = [];
    foreach ($owners as $pathKey => $email) {
        if (!is_string($pathKey) || !is_string($email)) continue;
        $cleanPath = normalizeRelativePath($pathKey);
        $cleanEmail = normalizeEmailAddress($email);
        if ($cleanPath === '' || !isContributionPath($cleanPath) || !filter_var($cleanEmail, FILTER_VALIDATE_EMAIL)) continue;
        $clean[$cleanPath] = $cleanEmail;
    }
    ksort($clean, SORT_NATURAL | SORT_FLAG_CASE);
    $payload = json_encode(
        ['version' => 1, 'owners' => $clean],
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRETTY_PRINT
    );
    if (!is_string($payload)) {
        throw new RuntimeException('Không thể tạo dữ liệu chủ sở hữu Cộng đồng.');
    }

    $path = communityOwnerMetadataPath($instrumentRoot);
    $tmp = $path . '.' . getmypid() . '.' . uniqid('', true) . '.tmp';
    if (@file_put_contents($tmp, $payload, LOCK_EX) === false) {
        throw new RuntimeException('IIS/PHP không thể lưu thông tin chủ sở hữu Cộng đồng.');
    }
    @chmod($tmp, 0664);
    if (is_file($path) && !@unlink($path)) {
        @unlink($tmp);
        throw new RuntimeException('Không thể cập nhật thông tin chủ sở hữu Cộng đồng.');
    }
    if (!@rename($tmp, $path)) {
        @unlink($tmp);
        throw new RuntimeException('Không thể hoàn tất thông tin chủ sở hữu Cộng đồng.');
    }
    @chmod($path, 0664);
}

function exactCommunityOwnerEmail(array $owners, string $relativePath): string
{
    $relativePath = normalizeRelativePath($relativePath);
    return normalizeEmailAddress((string)($owners[$relativePath] ?? ''));
}

/** Return the owner of the item itself, or the nearest owned parent folder. */
function effectiveCommunityOwnerEmail(array $owners, string $relativePath): string
{
    $path = normalizeRelativePath($relativePath);
    if ($path === '' || !isContributionPath($path)) return '';

    while ($path !== '') {
        $owner = exactCommunityOwnerEmail($owners, $path);
        if ($owner !== '') return $owner;
        if ($path === contributionFolderPath()) break;
        $parent = parentRelativePath($path);
        if ($parent === null || $parent === $path) break;
        $path = $parent;
    }
    return '';
}

function permissionEmail(array $permission): string
{
    return normalizeEmailAddress((string)($permission['email'] ?? ''));
}

function permissionIsDocumentAdmin(array $permission): bool
{
    return (bool)($permission['can_manage'] ?? false);
}

function permissionOwnsCommunityPath(array $permission, array $owners, string $relativePath): bool
{
    $email = permissionEmail($permission);
    if ($email === '' || !isContributionPath($relativePath)) return false;
    return effectiveCommunityOwnerEmail($owners, $relativePath) === $email;
}

/**
 * Normal contributors may write directly in the Community root, or inside a
 * folder that belongs to them. Other members' folders are read-only.
 */
function permissionCanWriteCommunityFolder(
    array $permission,
    array $owners,
    string $folderRelativePath,
    string $requiredFlag,
    ?string $instrumentRoot = null
): bool {
    if (permissionIsDocumentAdmin($permission)) return true;
    if (!(bool)($permission[$requiredFlag] ?? false)) return false;

    $path = normalizeRelativePath($folderRelativePath);
    if (!isContributionPath($path)) return false;
    if (isContributionRootPath($path)) return true;

    $email = permissionEmail($permission);
    $owner = effectiveCommunityOwnerEmail($owners, $path);
    if ($owner !== '') return $owner === $email;

    // An existing, unowned community folder is treated as protected legacy
    // content. A missing path may be materialized by the current contributor
    // only when its nearest existing parent is Community root or their folder.
    if ($instrumentRoot === null) return false;
    try {
        if (resolveDirectoryPath($instrumentRoot, $path, false) !== null) return false;
    } catch (Throwable $ignored) {
        return false;
    }

    $parent = parentRelativePath($path);
    while ($parent !== null && $parent !== '') {
        try {
            $existingParent = resolveDirectoryPath($instrumentRoot, $parent, false);
        } catch (Throwable $ignored) {
            return false;
        }
        if ($existingParent !== null) {
            if (isContributionRootPath($parent)) return true;
            return effectiveCommunityOwnerEmail($owners, $parent) === $email;
        }
        $parent = parentRelativePath($parent);
    }
    return false;
}

/** Create only the missing folders in a contributor-owned Community path. */
function ensureOwnedCommunityFolderPath(
    string $instrumentRoot,
    array &$owners,
    string $folderRelativePath,
    string $ownerEmail
): string {
    $path = normalizeRelativePath($folderRelativePath);
    if (!isContributionPath($path)) {
        throw new RuntimeException('Đường dẫn không nằm trong khu vực Cộng đồng.');
    }
    $rootReal = realpath($instrumentRoot);
    if ($rootReal === false) throw new RuntimeException('Không thể truy cập thư mục nhạc cụ.');

    $ownerEmail = normalizeEmailAddress($ownerEmail);
    $currentFs = $rootReal;
    $currentRelative = '';
    $changed = false;
    foreach (explode('/', $path) as $part) {
        if ($part === '') continue;
        $currentRelative = $currentRelative === '' ? $part : $currentRelative . '/' . $part;
        $next = $currentFs . DIRECTORY_SEPARATOR . $part;
        $existed = is_dir($next);
        if (!$existed) {
            if (!@mkdir($next, 0775) && !is_dir($next)) {
                throw new RuntimeException('Không thể tạo đường dẫn chia sẻ Cộng đồng trên máy chủ.');
            }
            @chmod($next, 0775);
            if (!isContributionRootPath($currentRelative)) {
                setCommunityOwner($owners, $currentRelative, $ownerEmail);
                $changed = true;
            }
        }
        if (!is_dir($next)) throw new RuntimeException('Một phần đường dẫn không phải thư mục.');
        $nextReal = realpath($next);
        if ($nextReal === false || !isPathInside($nextReal, $rootReal)) {
            throw new RuntimeException('Đường dẫn chia sẻ nằm ngoài thư mục cho phép.');
        }
        $currentFs = $nextReal;
    }
    if ($changed) saveCommunityOwnerMap($instrumentRoot, $owners);
    return $currentFs;
}

function permissionCanManageCommunityItem(array $permission, array $owners, string $relativePath): bool
{
    $path = normalizeRelativePath($relativePath);
    // The instrument root itself is never an item and cannot be renamed/deleted.
    if ($path === '') return false;

    // A document Admin may manage every public file/folder shown by the API,
    // including original project folders and the Community root folder.
    if (permissionIsDocumentAdmin($permission)) return true;

    // Normal members may manage only their own content below Community.
    if (isContributionRootPath($path)) return false;
    return isContributionPath($path) && permissionOwnsCommunityPath($permission, $owners, $path);
}

function setCommunityOwner(array &$owners, string $relativePath, string $email): void
{
    $path = normalizeRelativePath($relativePath);
    $email = normalizeEmailAddress($email);
    if ($path !== '' && isContributionPath($path) && filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $owners[$path] = $email;
    }
}

function renameCommunityOwnerPrefix(array &$owners, string $oldPath, string $newPath): void
{
    $oldPath = normalizeRelativePath($oldPath);
    $newPath = normalizeRelativePath($newPath);
    $updated = [];
    foreach ($owners as $pathKey => $email) {
        $cleanKey = normalizeRelativePath((string)$pathKey);
        if ($cleanKey === $oldPath || strpos($cleanKey, $oldPath . '/') === 0) {
            $suffix = substr($cleanKey, strlen($oldPath));
            $updated[$newPath . $suffix] = $email;
        } else {
            $updated[$cleanKey] = $email;
        }
    }
    $owners = $updated;
}

function removeCommunityOwnerPrefix(array &$owners, string $relativePath): void
{
    $path = normalizeRelativePath($relativePath);
    foreach (array_keys($owners) as $pathKey) {
        $cleanKey = normalizeRelativePath((string)$pathKey);
        if ($cleanKey === $path || strpos($cleanKey, $path . '/') === 0) {
            unset($owners[$pathKey]);
        }
    }
}

function boolRequestValue(string $name, bool $default = false): bool
{
    $raw = strtolower(requestValue($name, $default ? '1' : '0'));
    return in_array($raw, ['1', 'true', 'yes', 'on'], true);
}

function mysqliBindAndExecute(mysqli $db, string $sql, array $params = []): mysqli_stmt
{
    $statement = $db->prepare($sql);
    if ($params !== []) {
        $types = '';
        $values = [];
        foreach ($params as $value) {
            if (is_bool($value) || is_int($value)) {
                $types .= 'i';
                $values[] = (int)$value;
            } elseif (is_float($value)) {
                $types .= 'd';
                $values[] = $value;
            } else {
                // MySQLi accepts null when it is bound as a string value.
                $types .= 's';
                $values[] = $value;
            }
        }

        $bindArguments = [$types];
        foreach ($values as $index => $_value) {
            $bindArguments[] = &$values[$index];
        }
        if (!call_user_func_array([$statement, 'bind_param'], $bindArguments)) {
            $statement->close();
            throw new RuntimeException('Không thể gắn tham số truy vấn MySQL.');
        }
    }
    $statement->execute();
    return $statement;
}

function mysqliStatementRows(mysqli_stmt $statement): array
{
    // mysqlnd is normally included with PHP on Windows/IIS.
    if (method_exists($statement, 'get_result')) {
        try {
            $result = $statement->get_result();
            if ($result instanceof mysqli_result) {
                $rows = $result->fetch_all(MYSQLI_ASSOC);
                $result->free();
                return is_array($rows) ? $rows : [];
            }
        } catch (Throwable $ignored) {
            // Fall back to bind_result below when mysqlnd/get_result is unavailable.
        }
    }

    $metadata = $statement->result_metadata();
    if (!$metadata instanceof mysqli_result) {
        return [];
    }

    $row = [];
    $references = [];
    while ($field = $metadata->fetch_field()) {
        $row[$field->name] = null;
        $references[] = &$row[$field->name];
    }
    $metadata->free();

    if ($references === []) {
        return [];
    }
    call_user_func_array([$statement, 'bind_result'], $references);

    $rows = [];
    while ($statement->fetch()) {
        $copy = [];
        foreach ($row as $name => $value) {
            $copy[$name] = $value;
        }
        $rows[] = $copy;
    }
    return $rows;
}

function mysqliSelectAll(mysqli $db, string $sql, array $params = []): array
{
    $statement = mysqliBindAndExecute($db, $sql, $params);
    try {
        return mysqliStatementRows($statement);
    } finally {
        $statement->close();
    }
}

function mysqliSelectOne(mysqli $db, string $sql, array $params = []): ?array
{
    $rows = mysqliSelectAll($db, $sql, $params);
    return isset($rows[0]) && is_array($rows[0]) ? $rows[0] : null;
}

function mysqliExecuteNonQuery(mysqli $db, string $sql, array $params = []): void
{
    $statement = mysqliBindAndExecute($db, $sql, $params);
    $statement->close();
}

function testMysqlConnection(): array
{
    global $DB_CONFIG;

    if (!extension_loaded('mysqli')) {
        return [false, 'PHP chưa bật extension mysqli.'];
    }
    foreach (['host', 'name', 'user', 'password'] as $key) {
        if (!isset($DB_CONFIG[$key]) || isPlaceholderValue((string)$DB_CONFIG[$key])) {
            return [false, 'Thông tin kết nối MySQL chưa đầy đủ.'];
        }
    }

    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
    try {
        $testDb = new mysqli(
            (string)$DB_CONFIG['host'],
            (string)$DB_CONFIG['user'],
            (string)$DB_CONFIG['password'],
            (string)$DB_CONFIG['name'],
            (int)($DB_CONFIG['port'] ?? 3306)
        );
        $testDb->set_charset('utf8mb4');
        $testDb->close();
        return [true, 'Kết nối MySQL thành công.'];
    } catch (Throwable $error) {
        // Không trả chi tiết exception ra Internet để tránh lộ thông tin máy chủ.
        return [false, 'Không thể kết nối MySQL. Hãy kiểm tra dịch vụ MySQL, database và tài khoản.'];
    }
}

function documentDatabase(): mysqli
{
    global $DB_CONFIG, $AUTO_CREATE_DOCUMENT_TABLES;
    static $db = null;
    static $schemaReady = false;

    if ($db instanceof mysqli) {
        return $db;
    }

    // Việc tạo bảng chỉ phụ thuộc MySQL. Google Web Client ID có thể cấu hình sau.
    requireMysqlConfiguration();
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

    try {
        $db = new mysqli(
            (string)$DB_CONFIG['host'],
            (string)$DB_CONFIG['user'],
            (string)$DB_CONFIG['password'],
            (string)$DB_CONFIG['name'],
            (int)($DB_CONFIG['port'] ?? 3306)
        );
        $db->set_charset('utf8mb4');
    } catch (Throwable $error) {
        throw new RuntimeException('Không thể kết nối MySQL: ' . $error->getMessage());
    }

    if ($AUTO_CREATE_DOCUMENT_TABLES && !$schemaReady) {
        ensureDocumentAccessSchema($db);
        $schemaReady = true;
    }
    return $db;
}

function ensureDocumentAccessSchema(mysqli $db): void
{
    global $DOCUMENT_SCHEMA_MARKER_FILE;

    if (is_file($DOCUMENT_SCHEMA_MARKER_FILE)) {
        return;
    }

    $lockPath = $DOCUMENT_SCHEMA_MARKER_FILE . '.lock';
    $lockHandle = @fopen($lockPath, 'c');
    if (is_resource($lockHandle)) {
        @flock($lockHandle, LOCK_EX);
    }

    try {
        if (!is_file($DOCUMENT_SCHEMA_MARKER_FILE)) {
            createDocumentAccessTables($db);
            $temporaryMarker = $DOCUMENT_SCHEMA_MARKER_FILE . '.tmp.' . getmypid();
            @file_put_contents($temporaryMarker, gmdate('c') . PHP_EOL, LOCK_EX);
            if (!@rename($temporaryMarker, $DOCUMENT_SCHEMA_MARKER_FILE)) {
                @unlink($temporaryMarker);
                @file_put_contents($DOCUMENT_SCHEMA_MARKER_FILE, gmdate('c') . PHP_EOL, LOCK_EX);
            }
        }
    } finally {
        if (is_resource($lockHandle)) {
            @flock($lockHandle, LOCK_UN);
            @fclose($lockHandle);
        }
    }
}

function createDocumentAccessTables(mysqli $db): void
{
    global $INITIAL_SUPER_ADMIN_GMAIL, $REMOVE_LEGACY_DOCUMENT_TABLES;

    $db->query(<<<'SQL'
CREATE TABLE IF NOT EXISTS document_permissions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    google_sub VARCHAR(255) NULL,
    can_view TINYINT(1) NOT NULL DEFAULT 1,
    can_download TINYINT(1) NOT NULL DEFAULT 1,
    can_upload TINYINT(1) NOT NULL DEFAULT 0,
    can_create_folder TINYINT(1) NOT NULL DEFAULT 0,
    can_manage TINYINT(1) NOT NULL DEFAULT 0,
    can_view_all_folders TINYINT(1) NOT NULL DEFAULT 1,
    allowed_folders LONGTEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    expires_at DATETIME NULL,
    note VARCHAR(255) NULL,
    created_by VARCHAR(254) NULL,
    last_used_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_document_email (email),
    UNIQUE KEY uq_document_google_sub (google_sub),
    KEY idx_document_status_expiry (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
SQL);

    // CREATE TABLE IF NOT EXISTS does not add new columns to an existing install.
    // Add folder-scope columns automatically while preserving all old accounts as
    // "all folders" so this deployment never removes an existing permission.
    $scopeColumns = [];
    $scopeResult = $db->query("SHOW COLUMNS FROM document_permissions");
    if ($scopeResult) {
        while ($scopeRow = $scopeResult->fetch_assoc()) {
            $scopeColumns[(string)($scopeRow['Field'] ?? '')] = true;
        }
        $scopeResult->free();
    }
    if (empty($scopeColumns['can_view_all_folders'])) {
        try {
            $db->query("ALTER TABLE document_permissions ADD COLUMN can_view_all_folders TINYINT(1) NOT NULL DEFAULT 1 AFTER can_manage");
        } catch (Throwable $migrationError) {
            // Another request may have completed the same migration concurrently.
            $verify = $db->query("SHOW COLUMNS FROM document_permissions LIKE 'can_view_all_folders'");
            $existsNow = $verify && $verify->num_rows > 0;
            if ($verify) $verify->free();
            if (!$existsNow) throw $migrationError;
        }
    }
    if (empty($scopeColumns['allowed_folders'])) {
        try {
            $db->query("ALTER TABLE document_permissions ADD COLUMN allowed_folders LONGTEXT NULL AFTER can_view_all_folders");
        } catch (Throwable $migrationError) {
            $verify = $db->query("SHOW COLUMNS FROM document_permissions LIKE 'allowed_folders'");
            $existsNow = $verify && $verify->num_rows > 0;
            if ($verify) $verify->free();
            if (!$existsNow) throw $migrationError;
        }
    }

    if ($REMOVE_LEGACY_DOCUMENT_TABLES) {
        $db->query('DROP TABLE IF EXISTS document_sessions');
        $db->query('DROP TABLE IF EXISTS document_access_logs');
    }

    $adminEmail = normalizeEmailAddress($INITIAL_SUPER_ADMIN_GMAIL);
    if (filter_var($adminEmail, FILTER_VALIDATE_EMAIL) && !isPlaceholderValue($INITIAL_SUPER_ADMIN_GMAIL)) {
        mysqliExecuteNonQuery($db, <<<'SQL'
INSERT INTO document_permissions (
    email, can_view, can_download, can_upload, can_create_folder,
    can_manage, can_view_all_folders, allowed_folders, status, created_by
) VALUES (
    ?, 1, 1, 1, 1, 1, 1, NULL, 'active', ?
)
ON DUPLICATE KEY UPDATE
    can_view = 1,
    can_download = 1,
    can_upload = 1,
    can_create_folder = 1,
    can_manage = 1,
    can_view_all_folders = 1,
    allowed_folders = NULL,
    status = 'active',
    expires_at = NULL
SQL, [$adminEmail, $adminEmail]);
    }
}

function base64UrlEncode(string $value): string
{
    return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
}

function base64UrlDecode(string $value): string
{
    $padding = strlen($value) % 4;
    if ($padding > 0) {
        $value .= str_repeat('=', 4 - $padding);
    }
    $decoded = base64_decode(strtr($value, '-_', '+/'), true);
    return $decoded === false ? '' : $decoded;
}

function requestAuthorizationHeader(): string
{
    $header = trim((string)($_SERVER['HTTP_AUTHORIZATION'] ?? ''));
    if ($header !== '') {
        return $header;
    }
    if (function_exists('getallheaders')) {
        foreach ((array)getallheaders() as $name => $value) {
            if (strcasecmp((string)$name, 'Authorization') === 0) {
                return trim((string)$value);
            }
        }
    }
    return '';
}

function requestBearerToken(): string
{
    $header = requestAuthorizationHeader();
    if (preg_match('/^Bearer\s+(.+)$/i', $header, $matches)) {
        return trim((string)$matches[1]);
    }

    // IIS/FastCGI may strip the Authorization header on some installations.
    // The Android client therefore also sends the same token in this custom header.
    $fallbackHeader = trim((string)($_SERVER['HTTP_X_DOCUMENT_TOKEN'] ?? ''));
    if ($fallbackHeader !== '') {
        return $fallbackHeader;
    }

    // Final compatibility fallback for manual testing. Do not place this token in
    // public links; normal Android requests use the headers above.
    return trim(requestValue('session_token', ''));
}

function clientIpAddress(): string
{
    $forwarded = trim((string)($_SERVER['HTTP_X_FORWARDED_FOR'] ?? ''));
    if ($forwarded !== '') {
        return trim(explode(',', $forwarded)[0]);
    }
    return trim((string)($_SERVER['REMOTE_ADDR'] ?? ''));
}

/*
 * Authentication mode for this build:
 * Android supplies the Gmail address of the Google account that is currently
 * signed in. The server then checks that Gmail against document_permissions and
 * issues its own random session token. This intentionally avoids any dependency
 * on GOOGLE_WEB_CLIENT_ID / Google tokeninfo.
 */

function permissionIsActive(array $permission): bool
{
    if (($permission['status'] ?? '') !== 'active') {
        return false;
    }
    $expiresAt = trim((string)($permission['expires_at'] ?? ''));
    return $expiresAt === '' || strtotime($expiresAt) > time();
}

function normalizeDocumentFolderScopeValue(string $value): string
{
    $value = str_replace('\\', '/', trim($value));
    $segments = [];
    foreach (explode('/', $value) as $segment) {
        $segment = trim($segment);
        if ($segment === '' || $segment === '.') continue;
        if ($segment === '..') return '';
        $segments[] = $segment;
    }
    return implode('/', $segments);
}

function decodeAllowedDocumentFolders($raw): array
{
    if (is_array($raw)) {
        $values = $raw;
    } else {
        $text = trim((string)$raw);
        if ($text === '') return [];
        $decoded = json_decode($text, true);
        $values = is_array($decoded) ? $decoded : preg_split('/[\r\n,]+/', $text);
    }

    $result = [];
    foreach ((array)$values as $value) {
        if (!is_string($value)) continue;
        $clean = normalizeDocumentFolderScopeValue($value);
        if ($clean !== '') $result[$clean] = true;
        if (count($result) >= 200) break;
    }
    return array_keys($result);
}

function encodeAllowedDocumentFolders(array $folders): ?string
{
    $clean = decodeAllowedDocumentFolders($folders);
    if ($clean === []) return null;
    return json_encode(array_values($clean), JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
}

function documentFolderScopeKey(string $instrument, string $relativePath): string
{
    $instrument = trim($instrument) === '' ? 'Guitar' : trim($instrument);
    $relativePath = normalizeDocumentFolderScopeValue($relativePath);
    return $relativePath === '' ? $instrument : $instrument . '/' . $relativePath;
}

function permissionCanViewAllDocumentFolders(array $permission): bool
{
    if (permissionIsDocumentAdmin($permission)) return true;
    // Existing installations did not have this column. Missing data must retain
    // the previous all-folder behavior instead of silently blocking accounts.
    if (!array_key_exists('can_view_all_folders', $permission)) return true;
    return (bool)$permission['can_view_all_folders'];
}

function permissionAllowedDocumentFolders(array $permission): array
{
    return decodeAllowedDocumentFolders($permission['allowed_folders'] ?? null);
}

function permissionCanAccessDocumentPath(
    array $permission,
    string $instrument,
    string $relativePath,
    bool $allowRootCatalog = true
): bool {
    if (permissionCanViewAllDocumentFolders($permission)) return true;

    $relativePath = normalizeRelativePath($relativePath);
    if ($relativePath === '') return $allowRootCatalog;

    $target = documentFolderScopeKey($instrument, $relativePath);
    $targetCompare = function_exists('mb_strtolower')
        ? mb_strtolower($target, 'UTF-8')
        : strtolower($target);

    foreach (permissionAllowedDocumentFolders($permission) as $allowedRaw) {
        $allowed = normalizeDocumentFolderScopeValue($allowedRaw);
        if ($allowed === '') continue;
        // Compatibility: a path saved without the instrument is interpreted for
        // the instrument currently being opened.
        if (strpos($allowed, '/') === false) {
            $allowed = documentFolderScopeKey($instrument, $allowed);
        }
        $allowedCompare = function_exists('mb_strtolower')
            ? mb_strtolower($allowed, 'UTF-8')
            : strtolower($allowed);
        if ($targetCompare === $allowedCompare || strpos($targetCompare, $allowedCompare . '/') === 0) {
            return true;
        }
    }
    return false;
}

function permissionPayload(array $permission): array
{
    return [
        'id' => (int)($permission['id'] ?? 0),
        'email' => (string)($permission['email'] ?? ''),
        'google_sub' => (string)($permission['google_sub'] ?? ''),
        'can_view' => (bool)($permission['can_view'] ?? false),
        'can_download' => (bool)($permission['can_download'] ?? false),
        'can_upload' => (bool)($permission['can_upload'] ?? false),
        'can_create_folder' => (bool)($permission['can_create_folder'] ?? false),
        'can_manage' => (bool)($permission['can_manage'] ?? false),
        'can_view_all_folders' => permissionCanViewAllDocumentFolders($permission),
        'allowed_folders' => permissionAllowedDocumentFolders($permission),
        'status' => (string)($permission['status'] ?? 'disabled'),
        'expires_at' => $permission['expires_at'] ?? null,
        'note' => (string)($permission['note'] ?? ''),
        'created_by' => (string)($permission['created_by'] ?? ''),
        'created_at' => (string)($permission['created_at'] ?? ''),
        'updated_at' => (string)($permission['updated_at'] ?? ''),
        'access_type' => permissionIsSongTabTrial($permission) ? 'trial' : 'permanent',
        'trial_started_at' => permissionIsSongTabTrial($permission) ? (string)($permission['created_at'] ?? '') : null,
        'trial_expires_at' => permissionIsSongTabTrial($permission) ? ($permission['expires_at'] ?? null) : null,
    ];
}

function findPermissionByEmail(mysqli $db, string $email): ?array
{
    return mysqliSelectOne(
        $db,
        'SELECT * FROM document_permissions WHERE email = ? LIMIT 1',
        [normalizeEmailAddress($email)]
    );
}

/** Return true only for rows created automatically for the SongTab trial. */
function permissionIsSongTabTrial(array $permission): bool
{
    global $SONG_TAB_TRIAL_NOTE_MARKER, $SONG_TAB_TRIAL_CREATED_BY;

    // Clearing expires_at or granting can_manage converts the row to permanent access.
    if ((bool)($permission['can_manage'] ?? false) || trim((string)($permission['expires_at'] ?? '')) === '') {
        return false;
    }

    $createdBy = strtolower(trim((string)($permission['created_by'] ?? '')));
    $note = (string)($permission['note'] ?? '');
    return $createdBy === strtolower((string)$SONG_TAB_TRIAL_CREATED_BY) ||
        ($SONG_TAB_TRIAL_NOTE_MARKER !== '' && strpos($note, (string)$SONG_TAB_TRIAL_NOTE_MARKER) !== false);
}

/** Extra fields are ignored by old clients but useful for newer builds and diagnostics. */
function songTabAccessPayload(array $permission): array
{
    global $SONG_TAB_TRIAL_DAYS;

    $isTrial = permissionIsSongTabTrial($permission);
    return [
        'type' => $isTrial ? 'trial' : 'permanent',
        'trial_days' => $isTrial ? max(1, (int)$SONG_TAB_TRIAL_DAYS) : null,
        'trial_started_at' => $isTrial ? (string)($permission['created_at'] ?? '') : null,
        'trial_expires_at' => $isTrial ? ($permission['expires_at'] ?? null) : null,
    ];
}

/**
 * Add a missing Google account to document_permissions for exactly one trial.
 * Existing rows are returned unchanged so an expired/disabled account is never reset.
 */
function ensureSongTabTrialPermission(mysqli $db, string $email, string $googleSub = ''): ?array
{
    global $SONG_TAB_AUTO_TRIAL_ENABLED, $SONG_TAB_TRIAL_DAYS,
        $SONG_TAB_TRIAL_NOTE_MARKER, $SONG_TAB_TRIAL_CREATED_BY;

    $email = normalizeEmailAddress($email);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        return null;
    }

    $existing = findPermissionByEmail($db, $email);
    if (is_array($existing)) {
        // Populate google_sub when an older row did not have it. Ignore conflicts safely.
        if ($googleSub !== '' && trim((string)($existing['google_sub'] ?? '')) === '') {
            try {
                mysqliExecuteNonQuery(
                    $db,
                    'UPDATE document_permissions SET google_sub = ? WHERE id = ? AND (google_sub IS NULL OR google_sub = \'\')',
                    [$googleSub, (int)$existing['id']]
                );
                $existing = findPermissionByEmail($db, $email) ?: $existing;
            } catch (Throwable $ignored) {
                // A duplicate Google subject must not block an existing email permission.
            }
        }
        return $existing;
    }

    if (!$SONG_TAB_AUTO_TRIAL_ENABLED) {
        return null;
    }

    $days = max(1, min(365, (int)$SONG_TAB_TRIAL_DAYS));
    $safeGoogleSub = trim($googleSub) !== '' ? trim($googleSub) : null;
    $note = trim((string)$SONG_TAB_TRIAL_NOTE_MARKER . ' SongTab server trial ' . $days . ' days');
    $createdBy = trim((string)$SONG_TAB_TRIAL_CREATED_BY);
    $sql = 'INSERT IGNORE INTO document_permissions (' .
        'email, google_sub, can_view, can_download, can_upload, can_create_folder, can_manage, ' .
        'status, expires_at, note, created_by, last_used_at' .
        ') VALUES (?, ?, 1, 0, 0, 0, 0, \'active\', DATE_ADD(NOW(), INTERVAL ' . $days . ' DAY), ?, ?, NOW())';

    mysqliExecuteNonQuery($db, $sql, [$email, $safeGoogleSub, $note, $createdBy]);
    return findPermissionByEmail($db, $email);
}

/** Verify an ID token only when an older client does not send the email field. */
function verifiedGoogleIdentityFromIdToken(string $idToken): ?array
{
    $idToken = trim($idToken);
    if ($idToken === '' || !function_exists('curl_init')) {
        return null;
    }

    $url = 'https://oauth2.googleapis.com/tokeninfo?id_token=' . rawurlencode($idToken);
    $handle = curl_init($url);
    curl_setopt_array($handle, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 8,
        CURLOPT_TIMEOUT => 12,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_HTTPHEADER => ['Accept: application/json'],
    ]);
    $raw = curl_exec($handle);
    $status = (int)curl_getinfo($handle, CURLINFO_HTTP_CODE);
    curl_close($handle);

    if ($status < 200 || $status >= 300 || !is_string($raw) || $raw === '') {
        return null;
    }

    $data = json_decode($raw, true);
    if (!is_array($data)) {
        return null;
    }

    $email = normalizeEmailAddress((string)($data['email'] ?? ''));
    $verified = strtolower((string)($data['email_verified'] ?? ''));
    $expiresAt = (int)($data['exp'] ?? 0);
    if (!filter_var($email, FILTER_VALIDATE_EMAIL) ||
        !in_array($verified, ['1', 'true'], true) ||
        $expiresAt <= time()) {
        return null;
    }

    return [
        'email' => $email,
        'google_sub' => trim((string)($data['sub'] ?? '')),
        'source' => 'google_id_token',
    ];
}

/** Accept both the current email client and older ID-token clients. */
function resolveDocumentLoginIdentity(): array
{
    $email = normalizeEmailAddress(requestValue('email', ''));
    if (filter_var($email, FILTER_VALIDATE_EMAIL)) {
        return ['email' => $email, 'google_sub' => '', 'source' => 'android_email'];
    }

    $identity = verifiedGoogleIdentityFromIdToken(requestValue('id_token', ''));
    if (is_array($identity)) {
        return $identity;
    }

    return ['email' => '', 'google_sub' => '', 'source' => 'missing'];
}

function findActivePermissionById(mysqli $db, int $permissionId): ?array
{
    return mysqliSelectOne($db, <<<'SQL'
SELECT * FROM document_permissions
WHERE id = ?
  AND status = 'active'
  AND (expires_at IS NULL OR expires_at > NOW())
LIMIT 1
SQL, [$permissionId]);
}

function createDocumentSession(mysqli $db, int $permissionId): string
{
    global $DOCUMENT_SESSION_DAYS, $DOCUMENT_SESSION_SECRET;

    $permission = findActivePermissionById($db, $permissionId);
    if (!is_array($permission)) {
        throw new RuntimeException('Permission record is not active.');
    }

    $payload = [
        'v' => 3,
        'pid' => (int)$permission['id'],
        'email' => normalizeEmailAddress((string)$permission['email']),
        'exp' => time() + (max(1, (int)$DOCUMENT_SESSION_DAYS) * 86400),
    ];
    $encoded = base64UrlEncode((string)json_encode($payload, JSON_UNESCAPED_SLASHES));
    $signature = base64UrlEncode(hash_hmac('sha256', $encoded, $DOCUMENT_SESSION_SECRET, true));
    return $encoded . '.' . $signature;
}

function decodeDocumentSessionToken(string $token): ?array
{
    global $DOCUMENT_SESSION_SECRET;

    $parts = explode('.', $token, 2);
    if (count($parts) !== 2) {
        return null;
    }
    [$encoded, $signature] = $parts;
    $expected = base64UrlEncode(hash_hmac('sha256', $encoded, $DOCUMENT_SESSION_SECRET, true));
    if (!hash_equals($expected, $signature)) {
        return null;
    }
    $payload = json_decode(base64UrlDecode($encoded), true);
    if (!is_array($payload) || (int)($payload['v'] ?? 0) !== 3 || (int)($payload['exp'] ?? 0) <= time()) {
        return null;
    }
    if ((int)($payload['pid'] ?? 0) <= 0 || !filter_var((string)($payload['email'] ?? ''), FILTER_VALIDATE_EMAIL)) {
        return null;
    }
    return $payload;
}

function requireDocumentSession(mysqli $db): array
{
    $token = requestBearerToken();
    if ($token === '') {
        sendJson([
            'success' => false,
            'code' => 'login_required',
            'message' => 'Please sign in with an authorized Google account.',
        ], 401);
    }

    $payload = decodeDocumentSessionToken($token);
    if (!is_array($payload)) {
        sendJson([
            'success' => false,
            'code' => 'session_expired',
            'message' => 'The document access session is invalid or expired. Please sign in again.',
        ], 401);
    }

    $permission = findActivePermissionById($db, (int)$payload['pid']);
    if (!is_array($permission) || normalizeEmailAddress((string)$permission['email']) !== normalizeEmailAddress((string)$payload['email'])) {
        sendJson([
            'success' => false,
            'code' => 'session_expired',
            'message' => 'The document access permission is no longer valid.',
        ], 401);
    }

    mysqliExecuteNonQuery(
        $db,
        'UPDATE document_permissions SET last_used_at = NOW() WHERE id = ?',
        [(int)$permission['id']]
    );
    return $permission;
}

function permissionAllows(array $permission, string $flag): bool
{
    return (bool)($permission['can_manage'] ?? false) || (bool)($permission[$flag] ?? false);
}

function requirePermissionFlag(array $permission, string $flag, string $message): void
{
    if (!permissionAllows($permission, $flag)) {
        sendJson([
            'success' => false,
            'code' => 'permission_denied',
            'message' => $message,
            'permissions' => permissionPayload($permission),
        ], 403);
    }
}

function createDocumentTicket(
    array $permission,
    string $action,
    string $instrument,
    string $relativePath
): string {
    global $DOCUMENT_SESSION_SECRET, $DOCUMENT_TICKET_TTL_SECONDS;

    $payload = [
        'v' => 1,
        'pid' => (int)$permission['id'],
        'a' => $action,
        'i' => $instrument,
        'p' => $relativePath,
        'exp' => time() + max(60, (int)$DOCUMENT_TICKET_TTL_SECONDS),
    ];
    $encoded = base64UrlEncode(json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES));
    $signature = base64UrlEncode(hash_hmac('sha256', $encoded, $DOCUMENT_SESSION_SECRET, true));
    return $encoded . '.' . $signature;
}

function permissionFromDocumentTicket(
    mysqli $db,
    string $ticket,
    string $action,
    string $instrument,
    string $relativePath
): ?array {
    global $DOCUMENT_SESSION_SECRET;

    $parts = explode('.', $ticket, 2);
    if (count($parts) !== 2) {
        return null;
    }
    [$encoded, $signature] = $parts;
    $expected = base64UrlEncode(hash_hmac('sha256', $encoded, $DOCUMENT_SESSION_SECRET, true));
    if (!hash_equals($expected, $signature)) {
        return null;
    }
    $payload = json_decode(base64UrlDecode($encoded), true);
    if (!is_array($payload) ||
        (int)($payload['exp'] ?? 0) < time() ||
        (string)($payload['a'] ?? '') !== $action ||
        (string)($payload['i'] ?? '') !== $instrument ||
        (string)($payload['p'] ?? '') !== $relativePath) {
        return null;
    }
    return findActivePermissionById($db, (int)($payload['pid'] ?? 0));
}

function authorizeDocumentAction(
    mysqli $db,
    string $action,
    string $instrument,
    string $relativePath
): array {
    $permission = null;
    if (in_array($action, ['view', 'download'], true)) {
        $ticket = requestValue('ticket', '');
        if ($ticket !== '') {
            $permission = permissionFromDocumentTicket($db, $ticket, $action, $instrument, $relativePath);
        }
    }
    if (!is_array($permission)) {
        $permission = requireDocumentSession($db);
    }

    // A 30-day SongTab trial may browse only the Documents root catalog and
    // the Community tree. Every other Documents folder remains protected until
    // an Admin explicitly registers this Gmail for Documents.
    if (permissionIsSongTabTrial($permission)) {
        $trialCanReadRootCatalog = $action === 'list' && $relativePath === '';
        $trialCanReadCommunity = in_array($action, ['list', 'view'], true) &&
            isContributionPath($relativePath);

        if (!$trialCanReadRootCatalog && !$trialCanReadCommunity) {
            sendJson([
                'success' => false,
                'code' => 'songtab_trial_scope_only',
                'message' => 'Tài khoản chưa được cấp quyền Tài liệu. Vui lòng liên hệ Gmail hoặc Zalo để đăng ký.',
                'permissions' => permissionPayload($permission),
                'catalog_access' => [
                    'root' => true,
                    'community' => true,
                    'protected_folders' => false,
                ],
            ], 403);
        }
    }

    if (!permissionIsSongTabTrial($permission)) {
        $allowRootCatalog = $action === 'list';
        if (!permissionCanAccessDocumentPath(
            $permission,
            $instrument,
            $relativePath,
            $allowRootCatalog
        )) {
            sendJson([
                'success' => false,
                'code' => 'folder_scope_denied',
                'message' => 'Gmail này chưa được cấp quyền truy cập thư mục tài liệu đã chọn.',
                'instrument' => $instrument,
                'path' => $relativePath,
                'permissions' => permissionPayload($permission),
            ], 403);
        }
    }

    switch ($action) {
        case 'list':
        case 'view':
            requirePermissionFlag($permission, 'can_view', 'Tài khoản không có quyền xem tài liệu.');
            break;
        case 'download':
            requirePermissionFlag($permission, 'can_download', 'Tài khoản không có quyền tải tài liệu.');
            break;
        case 'upload':
            requirePermissionFlag($permission, 'can_upload', 'Tài khoản không có quyền thêm file công khai.');
            break;
        case 'create_folder':
            requirePermissionFlag($permission, 'can_create_folder', 'Tài khoản không có quyền tạo thư mục công khai.');
            break;
        case 'rename':
        case 'delete_item':
            // Admin can manage everything. A normal contributor can manage only
            // items owned by their Gmail; that check is done after loading the
            // hidden community ownership manifest.
            requirePermissionFlag($permission, 'can_view', 'Tài khoản không có quyền xem tài liệu.');
            break;
        default:
            sendJson(['success' => false, 'message' => 'Thao tác tài liệu không hợp lệ.'], 400);
    }
    return $permission;
}

function writeAccessLog(mysqli $db, array $permission, string $action, string $path = ''): void
{
    // Access logging is intentionally disabled in the one-table build.
}

function normalizeExpiryInput(string $value): ?string
{
    $value = trim($value);
    if ($value === '') {
        return null;
    }
    if (preg_match('/^\d{4}-\d{2}-\d{2}$/', $value)) {
        $value .= ' 23:59:59';
    }
    $timestamp = strtotime($value);
    if ($timestamp === false) {
        throw new RuntimeException('Ngày hết hạn không hợp lệ.');
    }
    return gmdate('Y-m-d H:i:s', $timestamp);
}

function sendJson(array $payload, int $statusCode = 200): void
{
    $GLOBALS['TT_DOCUMENT_JSON_SENT'] = true;
    if (ob_get_level() > 0) {
        @ob_clean();
    }
    http_response_code($statusCode);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(
        $payload,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE
    );
    exit;
}

function requestValue(string $name, string $default = ''): string
{
    if (isset($_POST[$name])) {
        return trim((string)$_POST[$name]);
    }
    if (isset($_GET[$name])) {
        return trim((string)$_GET[$name]);
    }
    return $default;
}

function documentAccessScope(string $default = 'documents'): string
{
    $raw = strtolower(trim(requestValue('scope', requestValue('access_scope', ''))));
    if (in_array($raw, ['songtab', 'song_tab', 'sheet_music', 'tab'], true)) {
        return 'songtab';
    }
    if (in_array($raw, ['documents', 'document', 'learning_materials', 'materials'], true)) {
        return 'documents';
    }
    return $default === 'songtab' ? 'songtab' : 'documents';
}

function normalizeRelativePath(string $path): string
{
    $path = trim(str_replace('\\', '/', $path));
    $path = trim($path, '/');

    if ($path === '') {
        return '';
    }

    $safeParts = [];
    foreach (explode('/', $path) as $part) {
        $part = trim($part);
        if ($part === '' || $part === '.') {
            continue;
        }
        if ($part === '..' || strpos($part, "\0") !== false) {
            throw new RuntimeException('Đường dẫn không hợp lệ.');
        }
        $safeParts[] = $part;
    }

    return implode('/', $safeParts);
}

function normalizedComparePath(string $path): string
{
    $path = rtrim(str_replace('\\', '/', $path), '/');
    if (PHP_OS_FAMILY === 'Windows') {
        $path = strtolower($path);
    }
    return $path;
}

function isPathInside(string $candidate, string $root): bool
{
    $candidate = normalizedComparePath($candidate);
    $root = normalizedComparePath($root);
    return $candidate === $root || strpos($candidate, $root . '/') === 0;
}

function sanitizeEntryName(string $name, bool $isFile): string
{
    $name = trim($name);
    $name = preg_replace('/[<>:"\/\\\\|?*\x00-\x1F]/u', '_', $name);
    if (!is_string($name)) {
        $name = '';
    }
    $name = trim($name, " .\t\n\r\0\x0B");

    if ($name === '' || $name === '.' || $name === '..') {
        throw new RuntimeException($isFile ? 'Tên tệp không hợp lệ.' : 'Tên thư mục không hợp lệ.');
    }

    if (function_exists('mb_substr')) {
        $name = mb_substr($name, 0, 160, 'UTF-8');
    } else {
        $name = substr($name, 0, 160);
    }
    $name = rtrim($name, " .");

    $reservedBase = strtoupper((string)pathinfo($name, PATHINFO_FILENAME));
    if (preg_match('/^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$/i', $reservedBase)) {
        $name = '_' . $name;
    }

    if ($name === '') {
        throw new RuntimeException($isFile ? 'Tên tệp không hợp lệ.' : 'Tên thư mục không hợp lệ.');
    }
    return $name;
}

function ensureDocumentRoot(string $documentRoot): string
{
    if (!is_dir($documentRoot) && !@mkdir($documentRoot, 0775, true) && !is_dir($documentRoot)) {
        throw new RuntimeException('Không thể tạo thư mục document trên máy chủ.');
    }

    $real = realpath($documentRoot);
    if ($real === false) {
        throw new RuntimeException('Không thể truy cập thư mục document.');
    }
    return $real;
}

function resolveInstrumentRoot(
    string $documentRoot,
    string $instrument,
    bool &$usingGuitarRootFallback,
    bool $createForWrite = false
): ?string {
    $usingGuitarRootFallback = false;

    if ($createForWrite) {
        $documentRoot = ensureDocumentRoot($documentRoot);
    } elseif (!is_dir($documentRoot)) {
        return null;
    }

    $dedicatedFolder = $documentRoot . DIRECTORY_SEPARATOR . $instrument;
    if (is_dir($dedicatedFolder)) {
        $real = realpath($dedicatedFolder);
        return $real !== false ? $real : null;
    }

    // Existing Guitar deployment stores materials directly under /document.
    if ($instrument === 'Guitar' && is_dir($documentRoot)) {
        $usingGuitarRootFallback = true;
        $real = realpath($documentRoot);
        return $real !== false ? $real : null;
    }

    if ($createForWrite) {
        if (!@mkdir($dedicatedFolder, 0775, true) && !is_dir($dedicatedFolder)) {
            throw new RuntimeException('Không thể tạo thư mục cho nhạc cụ trên máy chủ.');
        }
        $real = realpath($dedicatedFolder);
        return $real !== false ? $real : null;
    }

    return null;
}

function publicServerPath(
    string $instrument,
    bool $usingGuitarRootFallback,
    string $relativePath
): string {
    $base = $usingGuitarRootFallback ? 'document' : 'document/' . $instrument;
    $relativePath = normalizeRelativePath($relativePath);
    return $relativePath === '' ? $base : $base . '/' . $relativePath;
}

function resolveExistingPath(string $instrumentRoot, string $relativePath): string
{
    $candidate = $instrumentRoot;
    if ($relativePath !== '') {
        $candidate .= DIRECTORY_SEPARATOR . str_replace('/', DIRECTORY_SEPARATOR, $relativePath);
    }

    $real = realpath($candidate);
    if ($real === false || !isPathInside($real, $instrumentRoot)) {
        throw new RuntimeException('Không tìm thấy đường dẫn tài liệu.');
    }

    return $real;
}

function resolveDirectoryPath(
    string $instrumentRoot,
    string $relativePath,
    bool $createMissing
): ?string {
    $rootReal = realpath($instrumentRoot);
    if ($rootReal === false) {
        throw new RuntimeException('Không thể truy cập thư mục nhạc cụ.');
    }

    $current = $rootReal;
    if ($relativePath === '') {
        return $current;
    }

    foreach (explode('/', normalizeRelativePath($relativePath)) as $part) {
        if ($part === '') {
            continue;
        }
        $next = $current . DIRECTORY_SEPARATOR . $part;
        if (!file_exists($next)) {
            if (!$createMissing) {
                return null;
            }
            if (!@mkdir($next, 0775) && !is_dir($next)) {
                throw new RuntimeException('Không thể tạo đường dẫn thư mục trên máy chủ.');
            }
        }
        if (!is_dir($next)) {
            throw new RuntimeException('Một phần đường dẫn không phải thư mục.');
        }
        $nextReal = realpath($next);
        if ($nextReal === false || !isPathInside($nextReal, $rootReal)) {
            throw new RuntimeException('Đường dẫn nằm ngoài thư mục cho phép.');
        }
        $current = $nextReal;
    }

    return $current;
}

function ensureContributionFolder(string $instrumentRoot): string
{
    $folderName = sanitizeEntryName(contributionFolderPath(), false);
    $target = $instrumentRoot . DIRECTORY_SEPARATOR . $folderName;
    if (!is_dir($target)) {
        if (!@mkdir($target, 0775, true) && !is_dir($target)) {
            throw new RuntimeException('Không thể tạo thư mục Cộng đồng trên máy chủ.');
        }
        @chmod($target, 0775);
    }
    $real = realpath($target);
    if ($real === false || !isPathInside($real, $instrumentRoot)) {
        throw new RuntimeException('Không thể xác nhận thư mục Cộng đồng.');
    }
    return $real;
}

function removePathRecursively(string $target, string $instrumentRoot): void
{
    $real = realpath($target);
    if ($real === false || !isPathInside($real, $instrumentRoot) || normalizedComparePath($real) === normalizedComparePath($instrumentRoot)) {
        throw new RuntimeException('Đường dẫn xóa không hợp lệ.');
    }
    if (is_link($real)) {
        throw new RuntimeException('Không cho phép thao tác với liên kết tượng trưng.');
    }
    if (is_file($real)) {
        if (!@unlink($real)) throw new RuntimeException('Không thể xóa tệp.');
        return;
    }
    if (!is_dir($real)) throw new RuntimeException('Mục cần xóa không tồn tại.');
    $entries = @scandir($real);
    if (!is_array($entries)) throw new RuntimeException('Không thể đọc thư mục cần xóa.');
    foreach ($entries as $name) {
        if ($name === '.' || $name === '..') continue;
        $child = $real . DIRECTORY_SEPARATOR . $name;
        if (is_link($child)) {
            if (!@unlink($child)) throw new RuntimeException('Không thể xóa liên kết trong thư mục.');
        } elseif (is_dir($child)) {
            removePathRecursively($child, $instrumentRoot);
        } elseif (!@unlink($child)) {
            throw new RuntimeException('Không thể xóa tệp trong thư mục.');
        }
    }
    if (!@rmdir($real)) throw new RuntimeException('Không thể xóa thư mục.');
}

function requestBaseUrl(): string
{
    $forwardedProto = trim((string)($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? ''));
    if ($forwardedProto !== '') {
        $scheme = strtolower(explode(',', $forwardedProto)[0]) === 'https' ? 'https' : 'http';
    } else {
        $https = strtolower((string)($_SERVER['HTTPS'] ?? ''));
        $scheme = ($https !== '' && $https !== 'off' && $https !== '0') ? 'https' : 'http';
    }

    $host = (string)($_SERVER['HTTP_HOST'] ?? 'tunertools.top');
    $scriptName = str_replace('\\', '/', (string)($_SERVER['SCRIPT_NAME'] ?? '/api_documents.php'));
    $directory = rtrim(str_replace('\\', '/', dirname($scriptName)), '/.');
    return $scheme . '://' . $host . ($directory !== '' ? $directory : '');
}

function endpointUrl(string $action, string $instrument, string $relativePath, string $ticket = ''): string
{
    $scriptName = basename((string)($_SERVER['SCRIPT_NAME'] ?? 'api_documents.php'));
    $url = requestBaseUrl()
        . '/' . rawurlencode($scriptName)
        . '?action=' . rawurlencode($action)
        . '&instrument=' . rawurlencode($instrument)
        . '&path=' . rawurlencode($relativePath);
    if ($ticket !== '') {
        $url .= '&ticket=' . rawurlencode($ticket);
    }
    return $url;
}

function classifyFile(string $fileName, array $imageExtensions, array $pdfExtensions, array $videoExtensions): ?string
{
    $extension = strtolower((string)pathinfo($fileName, PATHINFO_EXTENSION));
    if (in_array($extension, $imageExtensions, true)) {
        return 'image';
    }
    if (in_array($extension, $pdfExtensions, true)) {
        return 'pdf';
    }
    if (in_array($extension, $videoExtensions, true)) {
        return 'video';
    }
    return null;
}

function humanFileSize(int $bytes): string
{
    if ($bytes < 1024) {
        return $bytes . ' B';
    }

    $units = ['KB', 'MB', 'GB', 'TB'];
    $value = $bytes / 1024;
    foreach ($units as $unit) {
        if ($value < 1024 || $unit === 'TB') {
            $decimals = $value >= 100 ? 0 : ($value >= 10 ? 1 : 2);
            return number_format($value, $decimals, '.', '') . ' ' . $unit;
        }
        $value /= 1024;
    }
    return $bytes . ' B';
}

function detectMimeType(string $filePath): string
{
    if (class_exists('finfo')) {
        $finfo = new finfo(FILEINFO_MIME_TYPE);
        $mime = $finfo->file($filePath);
        if (is_string($mime) && $mime !== '') {
            return $mime;
        }
    }

    $extension = strtolower((string)pathinfo($filePath, PATHINFO_EXTENSION));
    $fallback = [
        'json' => 'application/json',
        'jpg' => 'image/jpeg', 'jpeg' => 'image/jpeg', 'png' => 'image/png',
        'webp' => 'image/webp', 'gif' => 'image/gif', 'bmp' => 'image/bmp',
        'pdf' => 'application/pdf', 'mp4' => 'video/mp4', 'webm' => 'video/webm',
        'mov' => 'video/quicktime', 'm4v' => 'video/x-m4v',
        'mkv' => 'video/x-matroska', 'avi' => 'video/x-msvideo',
        'mp3' => 'audio/mpeg', 'm4a' => 'audio/mp4', 'aac' => 'audio/aac',
        'wav' => 'audio/wav', 'ogg' => 'audio/ogg', 'vtt' => 'text/vtt; charset=utf-8',
    ];
    return $fallback[$extension] ?? 'application/octet-stream';
}

function validateUploadedFile(
    string $tempPath,
    string $displayName,
    int $size,
    int $maxUploadBytes,
    array $imageExtensions,
    array $pdfExtensions,
    array $videoExtensions
): string {
    if (!is_file($tempPath) || !is_readable($tempPath)) {
        throw new RuntimeException('Không thể đọc tệp tải lên.');
    }
    if ($size <= 0) {
        throw new RuntimeException('Tệp tải lên không có dữ liệu.');
    }
    if ($size > $maxUploadBytes) {
        throw new RuntimeException('Tệp vượt quá giới hạn 250 MB.');
    }

    $type = classifyFile($displayName, $imageExtensions, $pdfExtensions, $videoExtensions);
    if ($type === null) {
        throw new RuntimeException('Chỉ hỗ trợ hình ảnh, PDF và video.');
    }

    $mime = strtolower(detectMimeType($tempPath));
    if ($type === 'image') {
        if (strpos($mime, 'image/') !== 0 || @getimagesize($tempPath) === false) {
            throw new RuntimeException('Tệp hình ảnh không hợp lệ.');
        }
    } elseif ($type === 'pdf') {
        $handle = @fopen($tempPath, 'rb');
        $signature = $handle !== false ? (string)fread($handle, 5) : '';
        if ($handle !== false) {
            fclose($handle);
        }
        if ($signature !== '%PDF-') {
            throw new RuntimeException('Tệp PDF không hợp lệ.');
        }
    } else {
        $allowedVideoMimes = [
            'application/octet-stream',
            'application/x-matroska',
            'application/vnd.rn-realmedia',
        ];
        if (strpos($mime, 'video/') !== 0 && !in_array($mime, $allowedVideoMimes, true)) {
            throw new RuntimeException('Tệp video không hợp lệ.');
        }
    }

    return $type;
}

function uniqueFilePath(string $folder, string $requestedName): string
{
    $target = $folder . DIRECTORY_SEPARATOR . $requestedName;
    if (!file_exists($target)) {
        return $target;
    }

    $extension = (string)pathinfo($requestedName, PATHINFO_EXTENSION);
    $base = (string)pathinfo($requestedName, PATHINFO_FILENAME);
    $index = 1;
    do {
        $candidate = $base . ' (' . $index . ')' . ($extension !== '' ? '.' . $extension : '');
        $target = $folder . DIRECTORY_SEPARATOR . $candidate;
        $index++;
    } while (file_exists($target));

    return $target;
}

function shouldSkipEntry(string $name, bool $usingGuitarRootFallback, string $currentRelativePath): bool
{
    if ($name === '' || $name[0] === '.') {
        return true;
    }

    $lower = strtolower($name);
    if (in_array($lower, ['_pending', '_private', 'thumbs.db', 'desktop.ini'], true)) {
        return true;
    }

    if ($usingGuitarRootFallback && $currentRelativePath === '' &&
        in_array($lower, ['guitar', 'ukulele', 'piano'], true)) {
        return true;
    }
    return false;
}

function countVisibleChildren(
    string $folderPath,
    bool $usingGuitarRootFallback,
    string $relativePath,
    array $imageExtensions,
    array $pdfExtensions,
    array $videoExtensions
): int {
    $count = 0;
    $entries = @scandir($folderPath);
    if (!is_array($entries)) {
        return 0;
    }

    foreach ($entries as $name) {
        if ($name === '.' || $name === '..' || shouldSkipEntry($name, $usingGuitarRootFallback, $relativePath)) {
            continue;
        }
        $fullPath = $folderPath . DIRECTORY_SEPARATOR . $name;
        if (is_link($fullPath)) {
            continue;
        }
        if (is_dir($fullPath) || (is_file($fullPath) && classifyFile($name, $imageExtensions, $pdfExtensions, $videoExtensions) !== null)) {
            $count++;
        }
    }
    return $count;
}

/**
 * Return exact recursive counts for every supported file type below a folder.
 * These counts let the Android client update each folder's number when the
 * quick filter changes between Image, PDF and Video.
 */
function collectContainedSummary(
    string $folderPath,
    bool $usingGuitarRootFallback,
    string $relativePath,
    array $imageExtensions,
    array $pdfExtensions,
    array $videoExtensions,
    int $depth = 0
): array {
    $counts = ['image' => 0, 'pdf' => 0, 'video' => 0];
    if ($depth > 32) {
        return ['types' => [], 'counts' => $counts];
    }

    $entries = @scandir($folderPath);
    if (!is_array($entries)) {
        return ['types' => [], 'counts' => $counts];
    }

    foreach ($entries as $name) {
        if ($name === '.' || $name === '..' || shouldSkipEntry($name, $usingGuitarRootFallback, $relativePath)) {
            continue;
        }

        $fullPath = $folderPath . DIRECTORY_SEPARATOR . $name;
        if (is_link($fullPath)) {
            continue;
        }

        $itemRelativePath = $relativePath === '' ? $name : $relativePath . '/' . $name;
        if (is_dir($fullPath)) {
            $childSummary = collectContainedSummary(
                $fullPath,
                $usingGuitarRootFallback,
                $itemRelativePath,
                $imageExtensions,
                $pdfExtensions,
                $videoExtensions,
                $depth + 1
            );
            foreach (['image', 'pdf', 'video'] as $type) {
                $counts[$type] += (int)($childSummary['counts'][$type] ?? 0);
            }
        } elseif (is_file($fullPath)) {
            $type = classifyFile($name, $imageExtensions, $pdfExtensions, $videoExtensions);
            if ($type !== null && array_key_exists($type, $counts)) {
                $counts[$type]++;
            }
        }
    }

    $types = [];
    foreach (['image', 'pdf', 'video'] as $type) {
        if ($counts[$type] > 0) {
            $types[] = $type;
        }
    }
    return ['types' => $types, 'counts' => $counts];
}

function documentSummaryCacheFile(string $folderPath, string $relativePath): string
{
    global $DOCUMENT_SUMMARY_CACHE_DIR;
    $realPath = realpath($folderPath);
    $key = sha1(($realPath !== false ? $realPath : $folderPath) . '|' . $relativePath);
    return rtrim($DOCUMENT_SUMMARY_CACHE_DIR, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . $key . '.json';
}

function collectContainedSummaryCached(
    string $folderPath,
    bool $usingGuitarRootFallback,
    string $relativePath,
    array $imageExtensions,
    array $pdfExtensions,
    array $videoExtensions
): array {
    global $DOCUMENT_SUMMARY_CACHE_DIR, $DOCUMENT_SUMMARY_CACHE_TTL_SECONDS;

    $cacheFile = documentSummaryCacheFile($folderPath, $relativePath);
    if (is_file($cacheFile) && (time() - (int)@filemtime($cacheFile)) <= $DOCUMENT_SUMMARY_CACHE_TTL_SECONDS) {
        $raw = @file_get_contents($cacheFile);
        $cached = is_string($raw) ? json_decode($raw, true) : null;
        if (is_array($cached) && isset($cached['types'], $cached['counts']) &&
            is_array($cached['types']) && is_array($cached['counts'])) {
            return $cached;
        }
    }

    $summary = collectContainedSummary(
        $folderPath,
        $usingGuitarRootFallback,
        $relativePath,
        $imageExtensions,
        $pdfExtensions,
        $videoExtensions
    );

    if (is_dir($DOCUMENT_SUMMARY_CACHE_DIR) || @mkdir($DOCUMENT_SUMMARY_CACHE_DIR, 0777, true)) {
        $temporary = $cacheFile . '.tmp.' . getmypid();
        @file_put_contents(
            $temporary,
            json_encode($summary, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            LOCK_EX
        );
        if (!@rename($temporary, $cacheFile)) {
            @unlink($temporary);
        }
    }
    return $summary;
}

function clearDocumentSummaryCache(): void
{
    global $DOCUMENT_SUMMARY_CACHE_DIR;
    if (!is_dir($DOCUMENT_SUMMARY_CACHE_DIR)) return;
    $files = @glob(rtrim($DOCUMENT_SUMMARY_CACHE_DIR, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . '*.json');
    if (!is_array($files)) return;
    foreach ($files as $file) {
        if (is_file($file)) @unlink($file);
    }
}

function containedCountsForType(?string $type): array
{
    $counts = ['image' => 0, 'pdf' => 0, 'video' => 0];
    if ($type !== null && array_key_exists($type, $counts)) {
        $counts[$type] = 1;
    }
    return $counts;
}

function breadcrumbList(string $instrument, string $relativePath): array
{
    $breadcrumbs = [['path' => '', 'name' => $instrument]];
    if ($relativePath === '') {
        return $breadcrumbs;
    }

    $built = [];
    foreach (explode('/', $relativePath) as $part) {
        $built[] = $part;
        $breadcrumbs[] = ['path' => implode('/', $built), 'name' => $part];
    }
    return $breadcrumbs;
}

function parentRelativePath(string $relativePath): ?string
{
    if ($relativePath === '') {
        return null;
    }
    $position = strrpos($relativePath, '/');
    return $position === false ? '' : substr($relativePath, 0, $position);
}

/**
 * Resolve a public course asset under /guitar_HD without allowing path traversal.
 * The course endpoint is intentionally read-only and never exposes PHP/source files.
 */
function resolveGuitarCourseAsset(
    string $courseRoot,
    string $relativePath,
    array $allowedExtensions
): string {
    $relativePath = normalizeRelativePath($relativePath);
    if ($relativePath === '') {
        throw new RuntimeException('Thiếu đường dẫn nội dung khóa học.');
    }

    foreach (explode('/', $relativePath) as $part) {
        if ($part === '' || $part[0] === '.') {
            throw new RuntimeException('Đường dẫn nội dung khóa học không hợp lệ.');
        }
    }

    $extension = strtolower((string)pathinfo($relativePath, PATHINFO_EXTENSION));
    if ($extension === '' || !in_array($extension, $allowedExtensions, true)) {
        throw new RuntimeException('Định dạng nội dung khóa học không được hỗ trợ.');
    }

    $rootReal = realpath($courseRoot);
    if ($rootReal === false || !is_dir($rootReal)) {
        throw new RuntimeException('Thư mục guitar_HD chưa tồn tại trên máy chủ.');
    }

    $candidate = $rootReal . DIRECTORY_SEPARATOR
        . str_replace('/', DIRECTORY_SEPARATOR, $relativePath);
    $assetReal = realpath($candidate);
    if ($assetReal === false || !is_file($assetReal) || !isPathInside($assetReal, $rootReal)) {
        throw new RuntimeException('Không tìm thấy nội dung khóa học.');
    }

    return $assetReal;
}

function guitarCourseAssetUrl(string $relativePath): string
{
    $scriptName = basename((string)($_SERVER['SCRIPT_NAME'] ?? 'api_documents.php'));
    return requestBaseUrl()
        . '/' . rawurlencode($scriptName)
        . '?action=course_asset&path=' . rawurlencode(normalizeRelativePath($relativePath));
}

function summarizeGuitarCourseRoot(string $courseRoot, array $allowedExtensions): array
{
    $summary = [
        'json' => 0,
        'image' => 0,
        'video' => 0,
        'audio' => 0,
        'subtitle' => 0,
        'other' => 0,
    ];

    $rootReal = realpath($courseRoot);
    if ($rootReal === false || !is_dir($rootReal)) {
        return $summary;
    }

    $iterator = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($rootReal, FilesystemIterator::SKIP_DOTS),
        RecursiveIteratorIterator::LEAVES_ONLY
    );

    foreach ($iterator as $item) {
        if (!$item instanceof SplFileInfo || !$item->isFile() || $item->isLink()) continue;
        $name = $item->getFilename();
        if ($name === '' || $name[0] === '.') continue;
        $extension = strtolower((string)$item->getExtension());
        if (!in_array($extension, $allowedExtensions, true)) continue;

        if ($extension === 'json') {
            $summary['json']++;
        } elseif (in_array($extension, ['jpg', 'jpeg', 'png', 'webp', 'gif'], true)) {
            $summary['image']++;
        } elseif (in_array($extension, ['mp4', 'webm', 'mov', 'm4v', 'mkv'], true)) {
            $summary['video']++;
        } elseif (in_array($extension, ['mp3', 'm4a', 'aac', 'wav', 'ogg'], true)) {
            $summary['audio']++;
        } elseif ($extension === 'vtt') {
            $summary['subtitle']++;
        } else {
            $summary['other']++;
        }
    }

    return $summary;
}

function streamGuitarCourseAsset(string $filePath): void
{
    $extension = strtolower((string)pathinfo($filePath, PATHINFO_EXTENSION));
    if (function_exists('header_remove')) {
        @header_remove('Pragma');
    }

    if ($extension === 'json') {
        header('Cache-Control: no-cache, no-store, must-revalidate, max-age=0');
    } else {
        // Media can be cached. Change the filename or append a version in JSON when replacing it.
        header('Cache-Control: public, max-age=86400, stale-while-revalidate=3600');
    }
    header('X-Content-Type-Options: nosniff');
    header('X-TunerTools-Course-Asset: 1');
    streamFile($filePath, false);
}

function streamFile(string $filePath, bool $download): void
{
    // A document video is served by PHP only after its signed ticket is checked.
    // Stop as soon as Android closes VideoView and keep each video Range response
    // small so an abandoned stream cannot occupy an IIS/FastCGI worker for long.
    @set_time_limit(0);
    @ignore_user_abort(false);
    @ini_set('output_buffering', '0');
    @ini_set('zlib.output_compression', '0');

    while (ob_get_level() > 0) {
        @ob_end_clean();
    }

    if (!is_file($filePath) || !is_readable($filePath)) {
        sendJson(['success' => false, 'message' => 'Không thể đọc tệp tài liệu.'], 404);
    }

    clearstatcache(true, $filePath);
    $size = (int)filesize($filePath);
    $fileName = basename($filePath);
    $mime = detectMimeType($filePath);
    $isVideo = strncmp($mime, 'video/', 6) === 0;
    $start = 0;
    $end = max(0, $size - 1);
    $statusCode = 200;

    $rangeHeader = (string)($_SERVER['HTTP_RANGE'] ?? '');
    if ($rangeHeader !== '' && preg_match('/bytes=(\d*)-(\d*)/i', $rangeHeader, $matches)) {
        $requestedStart = (string)($matches[1] ?? '');
        $requestedEnd = (string)($matches[2] ?? '');

        if ($requestedStart === '' && $requestedEnd !== '') {
            // Suffix range: bytes=-500 means the final 500 bytes.
            $suffixLength = max(0, (int)$requestedEnd);
            $start = max(0, $size - $suffixLength);
            $end = max(0, $size - 1);
        } else {
            if ($requestedStart !== '') {
                $start = max(0, (int)$requestedStart);
            }
            if ($requestedEnd !== '') {
                $end = min($end, (int)$requestedEnd);
            }
        }

        if ($start > $end || $start >= $size) {
            header('Content-Range: bytes */' . $size);
            http_response_code(416);
            exit;
        }
        $statusCode = 206;

        // Android normally asks for an open-ended range (bytes=N-). Returning a
        // bounded segment makes the PHP worker exit after at most 2 MB. MediaPlayer
        // then requests the next range normally. This is what prevents video A from
        // blocking video B after the user exits midway.
        if ($isVideo) {
            $maxVideoSegmentBytes = 2 * 1024 * 1024;
            $end = min($end, $start + $maxVideoSegmentBytes - 1);
        }
    }

    $length = $end - $start + 1;
    http_response_code($statusCode);
    header('Content-Type: ' . $mime);
    header('Content-Encoding: identity');
    header('Accept-Ranges: bytes');
    header('Content-Length: ' . $length);
    header('Connection: close');
    header('X-Accel-Buffering: no');
    if ($isVideo && $statusCode === 206) {
        header('X-TunerTools-Video-Segment: 2097152');
    }
    if ($statusCode === 206) {
        header('Content-Range: bytes ' . $start . '-' . $end . '/' . $size);
    }

    $asciiName = preg_replace('/[^\x20-\x7E]/', '_', $fileName);
    $asciiName = str_replace(['"', '\\'], '_', (string)$asciiName);
    $disposition = $download ? 'attachment' : 'inline';
    header(
        'Content-Disposition: ' . $disposition
        . '; filename="' . $asciiName . '"'
        . "; filename*=UTF-8''" . rawurlencode($fileName)
    );

    $handle = fopen($filePath, 'rb');
    if ($handle === false) {
        sendJson(['success' => false, 'message' => 'Không thể mở tệp tài liệu.'], 500);
    }
    if ($start > 0) {
        fseek($handle, $start);
    }

    $remaining = $length;
    try {
        while ($remaining > 0 && !feof($handle)) {
            if (connection_aborted() || connection_status() !== CONNECTION_NORMAL) {
                break;
            }

            // Small writes let IIS detect a closed Android socket much earlier.
            $chunkSize = min(64 * 1024, $remaining);
            $buffer = fread($handle, $chunkSize);
            if ($buffer === false || $buffer === '') {
                break;
            }

            echo $buffer;
            $remaining -= strlen($buffer);
            @ob_flush();
            @flush();

            if (connection_aborted() || connection_status() !== CONNECTION_NORMAL) {
                break;
            }
        }
    } finally {
        @fclose($handle);
    }
    exit;
}

try {
    $method = strtoupper((string)($_SERVER['REQUEST_METHOD'] ?? 'GET'));
    $contentLength = (int)($_SERVER['CONTENT_LENGTH'] ?? 0);
    $contentType = strtolower((string)($_SERVER['CONTENT_TYPE'] ?? ''));

    // Some IIS/FastCGI setups do not populate $_POST for Android form requests.
    // Parse the raw form body before deciding that the request is empty.
    if ($method === 'POST' && empty($_POST) &&
        strpos($contentType, 'application/x-www-form-urlencoded') !== false) {
        $rawBody = (string)file_get_contents('php://input');
        if ($rawBody !== '') {
            $parsedBody = [];
            parse_str($rawBody, $parsedBody);
            if (is_array($parsedBody)) {
                $_POST = $parsedBody;
            }
        }
    }

    $canUseQueryFallback = strpos($contentType, 'application/x-www-form-urlencoded') !== false &&
        isset($_GET['action']) && trim((string)$_GET['action']) !== '';

    if ($method === 'POST' && $contentLength > 0 && empty($_POST) && empty($_FILES) &&
        !$canUseQueryFallback) {
        sendJson([
            'success' => false,
            'message' => strpos($contentType, 'multipart/form-data') !== false
                ? 'Dữ liệu tải lên vượt giới hạn PHP/IIS. Hãy tăng upload_max_filesize và post_max_size.'
                : 'Máy chủ không đọc được dữ liệu POST. Hãy kiểm tra cấu hình PHP/FastCGI trên IIS.',
        ], 413);
    }

    $action = strtolower(requestValue('action', 'list'));
    $documentActionAliases = [
        'login' => 'auth_login',
        'google_login' => 'auth_login',
        'auth_google' => 'auth_login',
        'document_login' => 'auth_login',
        'me' => 'permission_me',
        'permissions_me' => 'permission_me',
        'document_me' => 'permission_me',
        'status' => 'setup_status',
    ];
    if (isset($documentActionAliases[$action])) {
        $action = $documentActionAliases[$action];
    }

    // Very light connectivity probe used by Android's status dot. It deliberately
    // avoids MySQL, permission checks and recursive directory scans.
    if ($action === 'health') {
        if ($method !== 'GET') {
            sendJson(['success' => false, 'code' => 'method_not_allowed', 'message' => 'health chỉ hỗ trợ GET.'], 405);
        }
        sendJson([
            'success' => true,
            'service' => 'tunertools_documents',
            'version' => '2026-10-09-video-switch-v11',
            'time' => gmdate('c'),
        ]);
    }

    // ------------------------------------------------------------------
    // PUBLIC GUITAR COURSE BACKEND
    // These two actions deliberately run before documentDatabase(), so the
    // public course remains available even when MySQL/document login is down.
    // ------------------------------------------------------------------
    if ($action === 'course_manifest') {
        if ($method !== 'GET') {
            sendJson(['success' => false, 'message' => 'course_manifest chỉ hỗ trợ GET.'], 405);
        }

        $rootReal = realpath($guitarCourseRoot);
        $available = $rootReal !== false && is_dir($rootReal);
        $indexAvailable = $available && is_file($rootReal . DIRECTORY_SEPARATOR . 'course_index.json');

        sendJson([
            'success' => true,
            'api_version' => '2026-10-09-video-switch-v11',
            'course' => 'guitar_accompaniment_basic',
            'public_read_only' => true,
            'root_folder' => 'guitar_HD',
            'available' => $available,
            'index_available' => $indexAvailable,
            'index_url' => guitarCourseAssetUrl('course_index.json'),
            'asset_url_template' => requestBaseUrl() . '/api_documents.php?action=course_asset&path={URL_ENCODED_PATH}',
            'summary' => summarizeGuitarCourseRoot(
                $guitarCourseRoot,
                $guitarCourseAllowedExtensions
            ),
            'message' => !$available
                ? 'Chưa tìm thấy C:\\inetpub\\wwwroot\\guitar_HD.'
                : (!$indexAvailable ? 'Thiếu file guitar_HD/course_index.json.' : ''),
            'updated_at' => gmdate('c'),
        ]);
    }

    if ($action === 'course_asset') {
        if ($method !== 'GET') {
            sendJson(['success' => false, 'message' => 'course_asset chỉ hỗ trợ GET.'], 405);
        }

        $coursePath = '';
        try {
            $coursePath = normalizeRelativePath(requestValue('path', ''));
            $courseAsset = resolveGuitarCourseAsset(
                $guitarCourseRoot,
                $coursePath,
                $guitarCourseAllowedExtensions
            );
        } catch (Throwable $courseError) {
            sendJson([
                'success' => false,
                'code' => 'course_asset_not_found',
                'message' => $courseError->getMessage(),
                'path' => $coursePath,
            ], 404);
        }

        streamGuitarCourseAsset($courseAsset);
    }

    if ($action === 'setup_status') {
        $problems = serverConfigurationProblems();
        [$mysqlConnected, $mysqlMessage] = testMysqlConnection();

        // Quan trọng: setup_status cũng khởi tạo schema MySQL ngay lập tức.
        // Việc này chỉ phụ thuộc MySQL; không cần Google Web Client ID.
        $tablesReady = false;
        $schemaMessage = '';
        $tables = [];
        $adminSeeded = false;

        if ($mysqlConnected) {
            try {
                $statusDb = documentDatabase(); // tự gọi createDocumentAccessTables()
                $requiredTables = [
                    'document_permissions',
                ];
                foreach ($requiredTables as $tableName) {
                    $row = mysqliSelectOne(
                        $statusDb,
                        'SELECT COUNT(*) AS c FROM information_schema.tables WHERE table_schema = ? AND table_name = ?',
                        [(string)$DB_CONFIG['name'], $tableName]
                    );
                    if ((int)($row['c'] ?? 0) > 0) {
                        $tables[] = $tableName;
                    }
                }
                $tablesReady = count($tables) === count($requiredTables);
                $adminRow = findPermissionByEmail($statusDb, (string)$INITIAL_SUPER_ADMIN_GMAIL);
                $adminSeeded = is_array($adminRow) && (bool)($adminRow['can_manage'] ?? false);
                $schemaMessage = $tablesReady
                    ? 'Các bảng quyền tài liệu đã sẵn sàng.'
                    : 'Chưa tạo đủ các bảng quyền tài liệu.';
            } catch (Throwable $schemaError) {
                $schemaMessage = 'Không thể khởi tạo bảng MySQL: ' . $schemaError->getMessage();
            }
        }

        sendJson([
            'success' => true,
            'api_version' => '2026-10-09-video-switch-v11',
            'auth_mode' => 'google_email_scope_split_documents_plus_songtab_trial',
            'contribution_folder' => contributionFolderPath(),
            'regular_user_write_scope' => 'contribution_folder_only',
            'admin_file_management' => true,
            'community_owner_management' => true,
            'admin_manage_all_public_items' => true,
            'ownership_storage' => 'hidden_json_manifest',
            'configured' => $problems === [],
            'missing' => $problems,
            'php_version' => PHP_VERSION,
            'mysqli' => extension_loaded('mysqli'),
            'mysql_connected' => $mysqlConnected,
            'mysql_message' => $mysqlMessage,
            'tables_ready' => $tablesReady,
            'tables' => $tables,
            'schema_message' => $schemaMessage,
            'initial_admin' => normalizeEmailAddress((string)$INITIAL_SUPER_ADMIN_GMAIL),
            'admin_seeded' => $adminSeeded,
            'song_tab_auto_trial_enabled' => (bool)$SONG_TAB_AUTO_TRIAL_ENABLED,
            'song_tab_trial_days' => max(1, (int)$SONG_TAB_TRIAL_DAYS),
            'access_scope_split' => true,
            'documents_require_admin_registration' => true,
            'songtab_scope' => 'songtab',
            'documents_scope' => 'documents',
            'trial_documents_catalog' => [
                'root_visible' => true,
                'community_visible' => true,
                'other_folders_require_registration' => true,
            ],
            'document_root' => 'document',
            'course_root' => 'guitar_HD',
            'course_available' => is_file($guitarCourseRoot . DIRECTORY_SEPARATOR . 'course_index.json'),
            'course_index_url' => guitarCourseAssetUrl('course_index.json'),
        ]);
    }

    $db = documentDatabase();

    if ($action === 'auth_login') {
        requireDocumentAccessConfiguration();
        if (!in_array($method, ['GET', 'POST'], true)) {
            sendJson(['success' => false, 'message' => 'Đăng nhập chỉ hỗ trợ GET hoặc POST.'], 405);
        }

        $identity = resolveDocumentLoginIdentity();
        $email = normalizeEmailAddress((string)($identity['email'] ?? ''));
        $googleSub = trim((string)($identity['google_sub'] ?? ''));
        $scope = documentAccessScope('documents');
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            sendJson([
                'success' => false,
                'code' => 'invalid_email',
                'message' => 'Không đọc được Gmail đăng nhập hợp lệ từ ứng dụng.',
            ], 400);
        }

        // SongTab and Documents are intentionally separated:
        // - songtab: a missing Gmail may start one 30-day trial;
        // - documents: only an Admin-registered permission row is accepted.
        $permission = $scope === 'songtab'
            ? ensureSongTabTrialPermission($db, $email, $googleSub)
            : findPermissionByEmail($db, $email);

        if (!is_array($permission)) {
            sendJson([
                'success' => true,
                'authorized' => false,
                'code' => $scope === 'songtab' ? 'songtab_not_granted' : 'documents_not_granted',
                'scope' => $scope,
                'email' => $email,
                'message' => $scope === 'songtab'
                    ? 'Không thể khởi tạo quyền dùng thử Bản nhạc cho Gmail này.'
                    : 'Gmail này chưa được quản trị viên cấp quyền sử dụng Tài liệu. Vui lòng liên hệ Gmail hoặc Zalo để đăng ký.',
            ]);
        }

        $isTrial = permissionIsSongTabTrial($permission);

        // A SongTab trial must never become a Documents permission implicitly.
        if ($scope === 'documents' && $isTrial) {
            sendJson([
                'success' => true,
                'authorized' => false,
                'code' => 'documents_not_granted',
                'scope' => $scope,
                'email' => $email,
                'message' => 'Tài khoản này chỉ có quyền dùng thử Bản nhạc 30 ngày và chưa được cấp quyền Tài liệu. Vui lòng liên hệ Gmail hoặc Zalo để đăng ký.',
                'permissions' => permissionPayload($permission),
                'access' => songTabAccessPayload($permission),
            ]);
        }

        if (!permissionIsActive($permission)) {
            $isExpired = (($permission['status'] ?? '') === 'active');
            sendJson([
                'success' => true,
                'authorized' => false,
                'code' => $isExpired ? ($isTrial ? 'trial_expired' : 'expired') : 'disabled',
                'scope' => $scope,
                'email' => $email,
                'message' => $isExpired
                    ? ($isTrial
                        ? 'Thời gian dùng thử 30 ngày của chức năng Bản nhạc đã hết. Vui lòng liên hệ Gmail hoặc Zalo để được hỗ trợ gia hạn.'
                        : 'Quyền truy cập đã hết hạn.')
                    : 'Quyền truy cập đang bị khóa.',
                'access' => songTabAccessPayload($permission),
            ]);
        }

        $hasViewAccess = permissionAllows($permission, 'can_view') || permissionAllows($permission, 'can_manage');
        if (!$hasViewAccess) {
            sendJson([
                'success' => true,
                'authorized' => false,
                'code' => 'view_not_granted',
                'scope' => $scope,
                'email' => $email,
                'message' => $scope === 'documents'
                    ? 'Gmail này chưa được cấp quyền xem Tài liệu.'
                    : 'Gmail này chưa được cấp quyền sử dụng Bản nhạc.',
            ]);
        }

        mysqliExecuteNonQuery(
            $db,
            'UPDATE document_permissions SET last_used_at = NOW() WHERE id = ?',
            [(int)$permission['id']]
        );
        $permission['last_used_at'] = date('Y-m-d H:i:s');

        $sessionToken = createDocumentSession($db, (int)$permission['id']);
        writeAccessLog($db, $permission, 'auth_login:' . $scope);
        sendJson([
            'success' => true,
            'authorized' => true,
            'scope' => $scope,
            'session_token' => $sessionToken,
            'user' => [
                'email' => $email,
            ],
            'permissions' => permissionPayload($permission),
            'access' => songTabAccessPayload($permission),
        ]);
    }

    if ($action === 'permission_me') {
        $scope = documentAccessScope('documents');
        $permission = requireDocumentSession($db);

        if ($scope === 'documents' && permissionIsSongTabTrial($permission)) {
            sendJson([
                'success' => false,
                'authorized' => false,
                'code' => 'songtab_trial_scope_only',
                'scope' => $scope,
                'email' => (string)$permission['email'],
                'message' => 'Tài khoản này chỉ có quyền dùng thử Bản nhạc 30 ngày và chưa được cấp quyền Tài liệu. Vui lòng liên hệ Gmail hoặc Zalo để đăng ký.',
                'permissions' => permissionPayload($permission),
                'access' => songTabAccessPayload($permission),
            ], 403);
        }

        sendJson([
            'success' => true,
            'authorized' => true,
            'scope' => $scope,
            'user' => ['email' => (string)$permission['email']],
            'permissions' => permissionPayload($permission),
            'access' => songTabAccessPayload($permission),
        ]);
    }

    if ($action === 'permission_list') {
        $manager = requireDocumentSession($db);
        requirePermissionFlag($manager, 'can_manage', 'Chỉ quản trị viên quyền tài liệu mới được xem danh sách.');
        $rows = mysqliSelectAll($db, 'SELECT * FROM document_permissions ORDER BY can_manage DESC, email ASC');
        sendJson([
            'success' => true,
            'items' => array_map('permissionPayload', is_array($rows) ? $rows : []),
        ]);
    }

    if ($action === 'permission_folder_options') {
        $manager = requireDocumentSession($db);
        requirePermissionFlag($manager, 'can_manage', 'Bạn không có quyền quản lý tài liệu.');

        $instrumentKey = strtolower(requestValue('instrument', 'guitar'));
        if (!isset($instrumentMap[$instrumentKey])) {
            sendJson(['success' => false, 'message' => 'Nhạc cụ không được hỗ trợ.'], 400);
        }
        $instrument = $instrumentMap[$instrumentKey];
        $usingGuitarRootFallback = false;
        $instrumentRoot = resolveInstrumentRoot(
            $documentRoot,
            $instrument,
            $usingGuitarRootFallback,
            true
        );
        $items = [];
        if ($instrumentRoot !== null) {
            ensureContributionFolder($instrumentRoot);
            $entries = @scandir($instrumentRoot);
            if (is_array($entries)) {
                foreach ($entries as $name) {
                    if ($name === '.' || $name === '..' ||
                        shouldSkipEntry($name, $usingGuitarRootFallback, '') ||
                        strpos($name, '.') === 0) {
                        continue;
                    }
                    $fullPath = $instrumentRoot . DIRECTORY_SEPARATOR . $name;
                    if (!is_dir($fullPath) || is_link($fullPath)) continue;
                    $path = normalizeRelativePath($name);
                    if ($path === '') continue;
                    $items[] = [
                        'key' => documentFolderScopeKey($instrument, $path),
                        'instrument' => $instrument,
                        'path' => $path,
                        'name' => $name,
                    ];
                }
            }
        }
        usort($items, static function (array $left, array $right): int {
            return strnatcasecmp((string)$left['name'], (string)$right['name']);
        });
        sendJson([
            'success' => true,
            'instrument' => $instrument,
            'items' => $items,
        ]);
    }

    if ($action === 'permission_save') {
        if ($method !== 'POST') {
            sendJson(['success' => false, 'message' => 'Cập nhật quyền yêu cầu phương thức POST.'], 405);
        }
        $manager = requireDocumentSession($db);
        requirePermissionFlag($manager, 'can_manage', 'Bạn không có quyền quản lý tài liệu.');

        $email = normalizeEmailAddress(requestValue('email', ''));
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            sendJson(['success' => false, 'message' => 'Địa chỉ Gmail không hợp lệ.'], 400);
        }
        $existingTargetPermission = findPermissionByEmail($db, $email);
        $convertingSongTabTrial = is_array($existingTargetPermission) &&
            permissionIsSongTabTrial($existingTargetPermission);

        $status = strtolower(requestValue('status', 'active')) === 'disabled' ? 'disabled' : 'active';
        $expiresAt = normalizeExpiryInput(requestValue('expires_at', ''));

        // If Admin is converting an automatic SongTab trial and did not choose a
        // new expiry date, make the Documents registration permanent by default.
        if ($convertingSongTabTrial) {
            $oldTrialExpiry = trim((string)($existingTargetPermission['expires_at'] ?? ''));
            $oldTrialExpiryNormalized = $oldTrialExpiry === '' ? null : normalizeExpiryInput($oldTrialExpiry);
            if ($expiresAt === null || $expiresAt === $oldTrialExpiryNormalized) {
                $expiresAt = null;
            }
        }

        $note = trim(requestValue('note', ''));

        // An explicit Admin save means this Gmail is being registered for Documents.
        // Strip the automatic SongTab-trial marker so this row becomes a normal
        // document permission even if it originally came from the 30-day trial.
        if (isset($SONG_TAB_TRIAL_NOTE_MARKER) && $SONG_TAB_TRIAL_NOTE_MARKER !== '') {
            $note = trim(str_replace((string)$SONG_TAB_TRIAL_NOTE_MARKER, '', $note));
        }
        if (function_exists('mb_substr')) {
            $note = mb_substr($note, 0, 255, 'UTF-8');
        } else {
            $note = substr($note, 0, 255);
        }

        $canManage = boolRequestValue('can_manage', false);
        $canViewAllFolders = boolRequestValue('can_view_all_folders', true);
        $allowedFolders = decodeAllowedDocumentFolders(requestValue('allowed_folders', '[]'));
        if ($email === normalizeEmailAddress((string)$manager['email'])) {
            // A manager must not accidentally lock their own management access.
            $canManage = true;
            $status = 'active';
        }
        if ($canManage) {
            $canViewAllFolders = true;
            $allowedFolders = [];
        } elseif ($canViewAllFolders) {
            $allowedFolders = [];
        }
        $allowedFoldersJson = encodeAllowedDocumentFolders($allowedFolders);

        mysqliExecuteNonQuery($db, <<<'SQL'
INSERT INTO document_permissions (
    email, can_view, can_download, can_upload, can_create_folder,
    can_manage, can_view_all_folders, allowed_folders,
    status, expires_at, note, created_by
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
ON DUPLICATE KEY UPDATE
    can_view = VALUES(can_view),
    can_download = VALUES(can_download),
    can_upload = VALUES(can_upload),
    can_create_folder = VALUES(can_create_folder),
    can_manage = VALUES(can_manage),
    can_view_all_folders = VALUES(can_view_all_folders),
    allowed_folders = VALUES(allowed_folders),
    status = VALUES(status),
    expires_at = VALUES(expires_at),
    note = VALUES(note),
    created_by = VALUES(created_by),
    updated_at = CURRENT_TIMESTAMP
SQL, [
            $email,
            boolRequestValue('can_view', true) ? 1 : 0,
            boolRequestValue('can_download', true) ? 1 : 0,
            boolRequestValue('can_upload', false) ? 1 : 0,
            boolRequestValue('can_create_folder', false) ? 1 : 0,
            $canManage ? 1 : 0,
            $canViewAllFolders ? 1 : 0,
            $allowedFoldersJson,
            $status,
            $expiresAt,
            $note === '' ? null : $note,
            (string)$manager['email'],
        ]);

        $saved = findPermissionByEmail($db, $email);
        sendJson([
            'success' => true,
            'message' => 'Đã lưu quyền tài liệu.',
            'item' => permissionPayload($saved ?: []),
        ]);
    }

    if ($action === 'permission_delete') {
        if ($method !== 'POST') {
            sendJson(['success' => false, 'message' => 'Xóa quyền yêu cầu phương thức POST.'], 405);
        }
        $manager = requireDocumentSession($db);
        requirePermissionFlag($manager, 'can_manage', 'Bạn không có quyền quản lý tài liệu.');
        $permissionId = (int)requestValue('id', '0');
        if ($permissionId <= 0) {
            sendJson(['success' => false, 'message' => 'Thiếu mã quyền cần xóa.'], 400);
        }
        if ($permissionId === (int)$manager['id']) {
            sendJson(['success' => false, 'message' => 'Không thể tự xóa quyền quản trị đang sử dụng.'], 400);
        }
        mysqliExecuteNonQuery($db, 'DELETE FROM document_permissions WHERE id = ?', [$permissionId]);
        sendJson(['success' => true, 'message' => 'Đã xóa quyền tài liệu.']);
    }
    $instrumentKey = strtolower(requestValue('instrument', 'guitar'));
    if (!isset($instrumentMap[$instrumentKey])) {
        sendJson(['success' => false, 'message' => 'Nhạc cụ không được hỗ trợ.'], 400);
    }

    $instrument = $instrumentMap[$instrumentKey];
    $relativePath = normalizeRelativePath(requestValue('path', ''));
    $isWriteAction = in_array($action, ['create_folder', 'upload', 'rename', 'delete_item'], true);

    $documentPermission = authorizeDocumentAction($db, $action, $instrument, $relativePath);

    // Mỗi nhạc cụ luôn có đúng một vùng Cộng đồng để người dùng đóng góp.
    // Việc có được ghi vào đó hay không vẫn do can_upload/can_create_folder quyết định.
    $shouldEnsureContributionFolder = $action === 'list';

    if ($isWriteAction && $method !== 'POST') {
        sendJson(['success' => false, 'message' => 'Thao tác ghi yêu cầu phương thức POST.'], 405);
    }

    $usingGuitarRootFallback = false;
    $instrumentRoot = resolveInstrumentRoot(
        $documentRoot,
        $instrument,
        $usingGuitarRootFallback,
        $isWriteAction || $shouldEnsureContributionFolder
    );
    $communityOwners = [];

    if ($instrumentRoot === null) {
        if ($action === 'view' || $action === 'download') {
            sendJson(['success' => false, 'message' => 'Tệp tài liệu không tồn tại.'], 404);
        }
        sendJson([
            'success' => true,
            'action' => 'list',
            'instrument' => $instrument,
            'available' => false,
            'current_path' => $relativePath,
            'parent_path' => parentRelativePath($relativePath),
            'breadcrumbs' => breadcrumbList($instrument, $relativePath),
            'contribution_folder' => contributionFolderPath(),
            'can_public_upload_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_upload', $instrumentRoot),
            'can_public_create_folder_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_create_folder', $instrumentRoot),
            'current_folder_owner_email' => effectiveCommunityOwnerEmail($communityOwners, $relativePath),
            'current_folder_is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $relativePath),
            'items' => [],
            'message' => 'Chưa có tài liệu cho ' . $instrument . '.',
        ]);
    }

    if ($shouldEnsureContributionFolder && $relativePath === '') {
        ensureContributionFolder($instrumentRoot);
    }

    $communityOwners = loadCommunityOwnerMap($instrumentRoot);

    if ($action === 'rename') {
        if ($relativePath === '') {
            sendJson(['success' => false, 'message' => 'Không thể đổi tên thư mục gốc.'], 400);
        }
        if (isContributionRootPath($relativePath) && !permissionIsDocumentAdmin($documentPermission)) {
            sendJson(['success' => false, 'message' => 'Chỉ Admin mới được đổi tên thư mục Cộng đồng.'], 403);
        }
        if (!permissionCanManageCommunityItem($documentPermission, $communityOwners, $relativePath)) {
            sendJson([
                'success' => false,
                'code' => 'not_item_owner',
                'message' => 'Chỉ chính chủ nội dung hoặc Admin mới được đổi tên mục này.',
            ], 403);
        }
        $sourcePath = resolveExistingPath($instrumentRoot, $relativePath);
        $isFolder = is_dir($sourcePath);
        if (!$isFolder && !is_file($sourcePath)) {
            sendJson(['success' => false, 'message' => 'Không tìm thấy mục cần đổi tên.'], 404);
        }
        $requestedName = trim(requestValue('new_name', ''));
        if ($requestedName === '') {
            sendJson(['success' => false, 'message' => 'Tên mới không được để trống.'], 400);
        }
        if ($isFolder) {
            $newName = sanitizeEntryName($requestedName, false);
        } else {
            $oldExtension = strtolower((string)pathinfo($sourcePath, PATHINFO_EXTENSION));
            $requestedExtension = strtolower((string)pathinfo($requestedName, PATHINFO_EXTENSION));
            $requestedBase = $requestedName;
            if ($oldExtension !== '' && $requestedExtension === $oldExtension) {
                $requestedBase = substr($requestedName, 0, -(strlen($oldExtension) + 1));
            }
            $requestedBase = trim($requestedBase);
            if ($requestedBase === '') {
                sendJson(['success' => false, 'message' => 'Tên mới không hợp lệ.'], 400);
            }
            $safeBase = sanitizeEntryName($requestedBase, false);
            $newName = $safeBase . ($oldExtension !== '' ? '.' . $oldExtension : '');
        }
        $parentRelative = parentRelativePath($relativePath) ?? '';
        $parentFolder = resolveDirectoryPath($instrumentRoot, $parentRelative, false);
        if ($parentFolder === null) throw new RuntimeException('Không thể truy cập thư mục cha.');
        $targetPath = $parentFolder . DIRECTORY_SEPARATOR . $newName;
        if (file_exists($targetPath)) {
            sendJson(['success' => false, 'message' => 'Tên mới đã tồn tại.'], 409);
        }
        if (!@rename($sourcePath, $targetPath)) {
            throw new RuntimeException('IIS/PHP không thể đổi tên mục này.');
        }
        $newRelativePath = $parentRelative === '' ? $newName : $parentRelative . '/' . $newName;
        if (isContributionPath($relativePath)) {
            renameCommunityOwnerPrefix($communityOwners, $relativePath, $newRelativePath);
            saveCommunityOwnerMap($instrumentRoot, $communityOwners);
        }
        clearDocumentSummaryCache();
        sendJson([
            'success' => true,
            'action' => 'rename',
            'renamed' => true,
            'old_path' => $relativePath,
            'new_path' => $newRelativePath,
            'message' => 'Đã đổi tên thành công.',
        ]);
    }

    if ($action === 'delete_item') {
        if ($relativePath === '') {
            sendJson(['success' => false, 'message' => 'Không thể xóa thư mục gốc.'], 400);
        }
        if (isContributionRootPath($relativePath) && !permissionIsDocumentAdmin($documentPermission)) {
            sendJson(['success' => false, 'message' => 'Chỉ Admin mới được xóa thư mục Cộng đồng.'], 403);
        }
        if (!permissionCanManageCommunityItem($documentPermission, $communityOwners, $relativePath)) {
            sendJson([
                'success' => false,
                'code' => 'not_item_owner',
                'message' => 'Chỉ chính chủ nội dung hoặc Admin mới được xóa mục này.',
            ], 403);
        }
        $targetPath = resolveExistingPath($instrumentRoot, $relativePath);
        removePathRecursively($targetPath, $instrumentRoot);
        if (isContributionPath($relativePath)) {
            removeCommunityOwnerPrefix($communityOwners, $relativePath);
            saveCommunityOwnerMap($instrumentRoot, $communityOwners);
        }
        clearDocumentSummaryCache();
        sendJson([
            'success' => true,
            'action' => 'delete_item',
            'deleted' => true,
            'path' => $relativePath,
            'message' => 'Đã xóa thành công.',
        ]);
    }

    if ($action === 'create_folder') {
        if (!permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_create_folder', $instrumentRoot)) {
            sendJson([
                'success' => false,
                'code' => 'community_folder_read_only',
                'message' => isContributionPath($relativePath)
                    ? 'Thư mục này thuộc thành viên khác. Bạn chỉ có quyền xem.'
                    : 'Người dùng chỉ được tạo thư mục trong khu vực Cộng đồng.',
            ], 403);
        }
        $folderName = sanitizeEntryName(requestValue('name', ''), false);
        $parentFolder = resolveDirectoryPath($instrumentRoot, $relativePath, false);
        if ($parentFolder === null && !permissionIsDocumentAdmin($documentPermission) && isContributionPath($relativePath)) {
            $parentFolder = ensureOwnedCommunityFolderPath(
                $instrumentRoot,
                $communityOwners,
                $relativePath,
                permissionEmail($documentPermission)
            );
        }
        if ($parentFolder === null) {
            throw new RuntimeException('Không thể tạo thư mục cha.');
        }

        $target = $parentFolder . DIRECTORY_SEPARATOR . $folderName;
        if (file_exists($target)) {
            sendJson(['success' => false, 'message' => 'Tên thư mục đã tồn tại.'], 409);
        }
        if (!@mkdir($target, 0775) && !is_dir($target)) {
            throw new RuntimeException('IIS/PHP không có quyền tạo thư mục trong document.');
        }
        @chmod($target, 0775);
        clearstatcache(true, $target);

        $createdRealPath = realpath($target);
        if ($createdRealPath === false || !is_dir($createdRealPath) ||
            !isPathInside($createdRealPath, $instrumentRoot)) {
            throw new RuntimeException('Máy chủ không xác nhận được thư mục vừa tạo.');
        }

        $itemRelativePath = $relativePath === '' ? $folderName : $relativePath . '/' . $folderName;
        if (isContributionPath($itemRelativePath)) {
            $newOwnerEmail = effectiveCommunityOwnerEmail($communityOwners, $relativePath);
            if ($newOwnerEmail === '') $newOwnerEmail = permissionEmail($documentPermission);
            setCommunityOwner($communityOwners, $itemRelativePath, $newOwnerEmail);
            saveCommunityOwnerMap($instrumentRoot, $communityOwners);
        }
        $serverPath = publicServerPath($instrument, $usingGuitarRootFallback, $itemRelativePath);
        writeAccessLog($db, $documentPermission, 'create_folder', $instrument . '/' . $itemRelativePath);
        clearDocumentSummaryCache();
        sendJson([
            'success' => true,
            'action' => 'create_folder',
            'created' => true,
            'instrument' => $instrument,
            'parent_path' => $relativePath,
            'created_path' => $itemRelativePath,
            'server_path' => $serverPath,
            'message' => 'Đã tạo thư mục công khai tại ' . $serverPath . '.',
            'item' => [
                'id' => sha1($instrument . '|folder|' . $itemRelativePath),
                'name' => $folderName,
                'type' => 'folder',
                'path' => $itemRelativePath,
                'children_count' => 0,
                'contained_types' => [],
                'contained_counts' => containedCountsForType(null),
                'is_protected_system_folder' => false,
                'owner_email' => effectiveCommunityOwnerEmail($communityOwners, $itemRelativePath),
                'is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $itemRelativePath),
                'can_manage_item' => permissionCanManageCommunityItem($documentPermission, $communityOwners, $itemRelativePath),
                'is_community_content' => isContributionPath($itemRelativePath),
            ],
        ]);
    }

    if ($action === 'upload') {
        if (!permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_upload', $instrumentRoot)) {
            sendJson([
                'success' => false,
                'code' => 'community_folder_read_only',
                'message' => isContributionPath($relativePath)
                    ? 'Thư mục này thuộc thành viên khác. Bạn chỉ có quyền xem.'
                    : 'Người dùng chỉ được thêm file trong khu vực Cộng đồng.',
            ], 403);
        }
        if (!isset($_FILES['file']) || !is_array($_FILES['file'])) {
            sendJson(['success' => false, 'message' => 'Không nhận được tệp tải lên.'], 400);
        }

        $upload = $_FILES['file'];
        $uploadError = (int)($upload['error'] ?? UPLOAD_ERR_NO_FILE);
        if ($uploadError !== UPLOAD_ERR_OK) {
            $messages = [
                UPLOAD_ERR_INI_SIZE => 'Tệp vượt upload_max_filesize của PHP.',
                UPLOAD_ERR_FORM_SIZE => 'Tệp vượt giới hạn biểu mẫu.',
                UPLOAD_ERR_PARTIAL => 'Tệp chỉ được tải lên một phần.',
                UPLOAD_ERR_NO_FILE => 'Chưa chọn tệp.',
                UPLOAD_ERR_NO_TMP_DIR => 'Máy chủ thiếu thư mục tạm.',
                UPLOAD_ERR_CANT_WRITE => 'Máy chủ không thể ghi tệp.',
                UPLOAD_ERR_EXTENSION => 'PHP đã dừng quá trình tải tệp.',
            ];
            sendJson([
                'success' => false,
                'message' => $messages[$uploadError] ?? ('Lỗi tải tệp: ' . $uploadError),
            ], 400);
        }

        $originalName = requestValue('display_name', (string)($upload['name'] ?? 'tai_lieu'));
        $safeName = sanitizeEntryName($originalName, true);
        $tempPath = (string)($upload['tmp_name'] ?? '');
        $size = (int)($upload['size'] ?? 0);
        validateUploadedFile(
            $tempPath,
            $safeName,
            $size,
            $maxUploadBytes,
            $imageExtensions,
            $pdfExtensions,
            $videoExtensions
        );

        $targetFolder = resolveDirectoryPath($instrumentRoot, $relativePath, false);
        if ($targetFolder === null && !permissionIsDocumentAdmin($documentPermission) && isContributionPath($relativePath)) {
            $targetFolder = ensureOwnedCommunityFolderPath(
                $instrumentRoot,
                $communityOwners,
                $relativePath,
                permissionEmail($documentPermission)
            );
        }
        if ($targetFolder === null) {
            throw new RuntimeException('Không thể tạo thư mục đích.');
        }
        $targetPath = uniqueFilePath($targetFolder, $safeName);
        if (!@move_uploaded_file($tempPath, $targetPath)) {
            throw new RuntimeException('IIS/PHP không có quyền ghi tệp vào document.');
        }
        @chmod($targetPath, 0664);

        $savedName = basename($targetPath);
        $itemRelativePath = $relativePath === '' ? $savedName : $relativePath . '/' . $savedName;
        $targetFolder = resolveDirectoryPath($instrumentRoot, $relativePath, false);
        if ($targetFolder === null && !permissionIsDocumentAdmin($documentPermission) && isContributionPath($relativePath)) {
            $targetFolder = ensureOwnedCommunityFolderPath(
                $instrumentRoot,
                $communityOwners,
                $relativePath,
                permissionEmail($documentPermission)
            );
        }
        if ($targetFolder === null) {
            throw new RuntimeException('Không thể tạo thư mục đích.');
        }
        $type = classifyFile($savedName, $imageExtensions, $pdfExtensions, $videoExtensions);
        $displayName = (string)pathinfo($savedName, PATHINFO_FILENAME);
        $extension = strtoupper((string)pathinfo($savedName, PATHINFO_EXTENSION));
        $savedSize = (int)(@filesize($targetPath) ?: 0);

        $serverPath = publicServerPath($instrument, $usingGuitarRootFallback, $itemRelativePath);
        writeAccessLog($db, $documentPermission, 'upload', $instrument . '/' . $itemRelativePath);
        clearDocumentSummaryCache();
        sendJson([
            'success' => true,
            'action' => 'upload',
            'uploaded' => true,
            'instrument' => $instrument,
            'server_path' => $serverPath,
            'message' => 'Đã tải tài liệu công khai lên ' . $serverPath . '.',
            'item' => [
                'id' => sha1($instrument . '|file|' . $itemRelativePath),
                'name' => $displayName !== '' ? $displayName : $savedName,
                'file_name' => $savedName,
                'type' => $type,
                'path' => $itemRelativePath,
                'description' => '',
                'meta' => $extension . ' • ' . humanFileSize($savedSize),
                'size_bytes' => $savedSize,
                'mime_type' => detectMimeType($targetPath),
                'children_count' => 0,
                'contained_types' => [$type],
                'contained_counts' => containedCountsForType($type),
                'is_protected_system_folder' => false,
                'owner_email' => effectiveCommunityOwnerEmail($communityOwners, $itemRelativePath),
                'is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $itemRelativePath),
                'can_manage_item' => permissionCanManageCommunityItem($documentPermission, $communityOwners, $itemRelativePath),
                'is_community_content' => isContributionPath($itemRelativePath),
                'preview_url' => endpointUrl(
                    'view',
                    $instrument,
                    $itemRelativePath,
                    createDocumentTicket($documentPermission, 'view', $instrument, $itemRelativePath)
                ),
                'download_url' => permissionAllows($documentPermission, 'can_download')
                    ? endpointUrl(
                        'download',
                        $instrument,
                        $itemRelativePath,
                        createDocumentTicket($documentPermission, 'download', $instrument, $itemRelativePath)
                    )
                    : '',
            ],
        ]);
    }

    if ($action === 'view' || $action === 'download') {
        if ($relativePath === '') {
            sendJson(['success' => false, 'message' => 'Thiếu đường dẫn tệp.'], 400);
        }
        $filePath = resolveExistingPath($instrumentRoot, $relativePath);
        if (!is_file($filePath)) {
            sendJson(['success' => false, 'message' => 'Không tìm thấy tệp tài liệu.'], 404);
        }
        if (classifyFile($filePath, $imageExtensions, $pdfExtensions, $videoExtensions) === null) {
            sendJson(['success' => false, 'message' => 'Định dạng tệp không được hỗ trợ.'], 415);
        }
        writeAccessLog($db, $documentPermission, $action, $instrument . '/' . $relativePath);
        // Do not hold a MySQL connection while PHP streams a large video/PDF.
        try { $db->close(); } catch (Throwable $ignored) { }
        streamFile($filePath, $action === 'download');
    }

    if ($action !== 'list') {
        sendJson(['success' => false, 'message' => 'Thao tác không hợp lệ.'], 400);
    }

    $currentFolder = resolveDirectoryPath($instrumentRoot, $relativePath, false);
    if ($currentFolder === null) {
        sendJson([
            'success' => true,
            'instrument' => $instrument,
            'available' => false,
            'current_path' => $relativePath,
            'parent_path' => parentRelativePath($relativePath),
            'breadcrumbs' => breadcrumbList($instrument, $relativePath),
            'contribution_folder' => contributionFolderPath(),
            'can_public_upload_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_upload', $instrumentRoot),
            'can_public_create_folder_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_create_folder', $instrumentRoot),
            'current_folder_owner_email' => effectiveCommunityOwnerEmail($communityOwners, $relativePath),
            'current_folder_is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $relativePath),
            'items' => [],
            'message' => 'Thư mục công khai này chưa tồn tại.',
            'updated_at' => gmdate('c'),
        ]);
    }

    $entries = @scandir($currentFolder);
    if (!is_array($entries)) {
        throw new RuntimeException('Không thể đọc thư mục tài liệu.');
    }

    $items = [];
    foreach ($entries as $name) {
        if ($name === '.' || $name === '..' || shouldSkipEntry($name, $usingGuitarRootFallback, $relativePath)) {
            continue;
        }

        $fullPath = $currentFolder . DIRECTORY_SEPARATOR . $name;
        if (is_link($fullPath)) {
            continue;
        }
        $itemRelativePath = $relativePath === '' ? $name : $relativePath . '/' . $name;

        // For a restricted Gmail, the root catalog itself is visible but only
        // specifically granted top-level folders are returned. Direct URL/API
        // access is also blocked earlier by authorizeDocumentAction().
        if ($relativePath === '' &&
            !permissionIsSongTabTrial($documentPermission) &&
            !permissionCanAccessDocumentPath(
                $documentPermission,
                $instrument,
                $itemRelativePath,
                false
            )) {
            continue;
        }

        if (is_dir($fullPath)) {
            $containedSummary = collectContainedSummaryCached(
                $fullPath,
                $usingGuitarRootFallback,
                $itemRelativePath,
                $imageExtensions,
                $pdfExtensions,
                $videoExtensions
            );
            $items[] = [
                'id' => sha1($instrument . '|folder|' . $itemRelativePath),
                'name' => $name,
                'type' => 'folder',
                'path' => $itemRelativePath,
                'description' => '',
                'meta' => '',
                'children_count' => countVisibleChildren(
                    $fullPath,
                    $usingGuitarRootFallback,
                    $itemRelativePath,
                    $imageExtensions,
                    $pdfExtensions,
                    $videoExtensions
                ),
                'contained_types' => $containedSummary['types'],
                'contained_counts' => $containedSummary['counts'],
                'preview_url' => '',
                'download_url' => '',
                'modified_at' => (int)(@filemtime($fullPath) ?: 0),
                'is_protected_system_folder' => isContributionRootPath($itemRelativePath) && !permissionIsDocumentAdmin($documentPermission),
                'owner_email' => effectiveCommunityOwnerEmail($communityOwners, $itemRelativePath),
                'is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $itemRelativePath),
                'can_manage_item' => permissionCanManageCommunityItem($documentPermission, $communityOwners, $itemRelativePath),
                'is_community_content' => isContributionPath($itemRelativePath),
            ];
            continue;
        }

        if (!is_file($fullPath)) {
            continue;
        }
        $type = classifyFile($name, $imageExtensions, $pdfExtensions, $videoExtensions);
        if ($type === null) {
            continue;
        }

        $extension = strtoupper((string)pathinfo($name, PATHINFO_EXTENSION));
        $size = (int)(@filesize($fullPath) ?: 0);
        $displayName = (string)pathinfo($name, PATHINFO_FILENAME);
        if ($displayName === '') {
            $displayName = $name;
        }

        $items[] = [
            'id' => sha1($instrument . '|file|' . $itemRelativePath),
            'name' => $displayName,
            'file_name' => $name,
            'type' => $type,
            'path' => $itemRelativePath,
            'description' => '',
            'meta' => $extension . ' • ' . humanFileSize($size),
            'size_bytes' => $size,
            'mime_type' => detectMimeType($fullPath),
            'children_count' => 0,
            'contained_types' => [$type],
            'contained_counts' => containedCountsForType($type),
            'preview_url' => endpointUrl(
                'view',
                $instrument,
                $itemRelativePath,
                createDocumentTicket($documentPermission, 'view', $instrument, $itemRelativePath)
            ),
            'download_url' => permissionAllows($documentPermission, 'can_download')
                ? endpointUrl(
                    'download',
                    $instrument,
                    $itemRelativePath,
                    createDocumentTicket($documentPermission, 'download', $instrument, $itemRelativePath)
                )
                : '',
            'modified_at' => (int)(@filemtime($fullPath) ?: 0),
            'is_protected_system_folder' => false,
            'owner_email' => effectiveCommunityOwnerEmail($communityOwners, $itemRelativePath),
            'is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $itemRelativePath),
            'can_manage_item' => permissionCanManageCommunityItem($documentPermission, $communityOwners, $itemRelativePath),
            'is_community_content' => isContributionPath($itemRelativePath),
        ];
    }

    usort($items, static function (array $left, array $right): int {
        $leftFolder = $left['type'] === 'folder';
        $rightFolder = $right['type'] === 'folder';
        if ($leftFolder !== $rightFolder) {
            return $leftFolder ? -1 : 1;
        }
        return strnatcasecmp((string)$left['name'], (string)$right['name']);
    });

    writeAccessLog($db, $documentPermission, 'list', $instrument . '/' . $relativePath);
    sendJson([
        'success' => true,
        'action' => 'list',
        'instrument' => $instrument,
        'available' => true,
        'current_path' => $relativePath,
        'parent_path' => parentRelativePath($relativePath),
        'breadcrumbs' => breadcrumbList($instrument, $relativePath),
        'contribution_folder' => contributionFolderPath(),
        'can_public_upload_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_upload', $instrumentRoot),
        'can_public_create_folder_here' => permissionCanWriteCommunityFolder($documentPermission, $communityOwners, $relativePath, 'can_create_folder', $instrumentRoot),
        'current_folder_owner_email' => effectiveCommunityOwnerEmail($communityOwners, $relativePath),
        'current_folder_is_owner' => permissionOwnsCommunityPath($documentPermission, $communityOwners, $relativePath),
        'items' => $items,
        'message' => count($items) === 0 ? 'Thư mục này chưa có tài liệu công khai.' : '',
        'updated_at' => gmdate('c'),
        'permissions' => permissionPayload($documentPermission),
    ]);
} catch (Throwable $error) {
    error_log('[TunerTools Documents] ' . $error->getMessage() . "\n" . $error->getTraceAsString());
    sendJson([
        'success' => false,
        'code' => 'server_error',
        'message' => 'Không thể xử lý tài liệu. Vui lòng thử lại.',
    ], 500);
}
