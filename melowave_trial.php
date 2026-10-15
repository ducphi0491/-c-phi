<?php
/**
 * MeloWave 30-day guest Premium trial - SINGLE-FILE PHP deployment.
 *
 * Public Android JSON API (POST only): /melowave/melowave_trial.php
 *   {"action":"health"}, {"action":"status", "androidId":"..."},
 *   {"action":"start", "androidId":"..."}.
 *   Admin Android (POST only): {"action":"admin_stats", "idToken":"GOOGLE_ID_TOKEN"}.
 *
 * Password-protected admin UI: /melowave/melowave_trial.php?admin=1
 *   Web Admin: HTTPS / localhost + password from IIS environment.
 *   Android Admin: Google ID token verified server-side against the existing app Admin account.
 *
 * Uses EXACTLY one new MySQL table: mw_trial_devices.
 * Does NOT change meloware_premium.php or existing Google Play receipts.
 * No includes or companion PHP files required.
 */
declare(strict_types=1);
@ini_set('display_errors', '0');
@ini_set('log_errors', '1');
error_reporting(E_ALL);

// Configuration + DB + hashing + JSON-response helpers.
/**
 * MeloWave Trial: server-local settings.
 * This PHP deliberately uses direct DB settings (no Windows environment variables).
 * NEVER commit this file to a public repository because it contains DB credentials
 * and the persistent HMAC key used for device trial identity.
 * Do not change hmac_key_b64 once devices have been registered.
 */
function mwt_config(): array {
    $settings = [
        'db_host' => '127.0.0.1',
        'db_port' => 3306,
        'db_name' => 'tuner',
        'db_user' => 'root',
        'db_password' => '123456',
        'hmac_key_b64' => 'jesz/Gbh5/x5DuKeexs5aV834+ZcdhR1V0Cc/V73Ptw=',
        'enabled' => true,
    ];
    $secret = base64_decode($settings['hmac_key_b64'], true);
    if ($secret === false || strlen($secret) !== 32) {
        throw new RuntimeException('TRIAL_HMAC_INVALID');
    }
    unset($settings['hmac_key_b64']);
    return ['hmac_key' => $secret] + $settings;
}

function mwt_db(array $config): mysqli {
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
    $db = new mysqli(
        (string) $config['db_host'],
        (string) $config['db_user'],
        (string) $config['db_password'],
        (string) $config['db_name'],
        (int) $config['db_port']
    );
    $db->set_charset('utf8mb4');
    $db->query("SET time_zone = '+00:00'");
    return $db;
}

function mwt_hash(string $type, string $value, array $config): string {
    // Domain separation keeps installation hashes distinct from device hashes.
    return hash_hmac('sha256', 'MeloWaveTrial:v1:' . $type . ':' . $value, $config['hmac_key']);
}

function mwt_json(array $data, int $status = 200): void {
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store, private');
    header('X-Content-Type-Options: nosniff');
    header('Referrer-Policy: no-referrer');
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

/** All dashboard sources share the SAME read-only SELECT queries. */
function mwt_report_scalar(mysqli $db, string $sql): int {
    $result = $db->query($sql);
    try {
        $row = $result->fetch_row();
        return (int) ($row[0] ?? 0);
    } finally { $result->free(); }
}

/** One table for both the website admin and the in-app Google-authenticated admin. */
function mwt_dashboard_report(mysqli $db, int $maxRecent = 50): array {
    $stats = [];
    $stats['devices'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices');
    $stats['eligible'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE first_played_at IS NULL');
    $stats['active'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE trial_expires_at > UTC_TIMESTAMP()');
    $stats['expired'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE first_played_at IS NOT NULL AND trial_expires_at <= UTC_TIMESTAMP()');
    $stats['today'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE last_active_day = UTC_DATE()');
    $stats['newToday'] = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE DATE(first_seen_at) = UTC_DATE()');
    $stats['activityDays'] = mwt_report_scalar($db, 'SELECT COALESCE(SUM(active_days), 0) FROM mw_trial_devices');
    foreach ([1, 7, 30] as $days) {
        $eligible = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE DATE(first_seen_at) <= DATE_SUB(UTC_DATE(), INTERVAL ' . $days . ' DAY)');
        $returned = mwt_report_scalar($db, 'SELECT COUNT(*) FROM mw_trial_devices WHERE returned_d' . $days . ' = 1');
        $stats['d' . $days] = $eligible > 0 ? number_format(100 * $returned / $eligible, 1) . '%' : '—';
        $stats['d' . $days . 'cohort'] = $eligible;
    }
    // No raw Android ID, Google email, private purchase data or full HMAC values leave the server.
    $limit = max(1, min(50, $maxRecent));
    $rs = $db->query('SELECT LEFT(device_key, 10) AS short_key, first_seen_at, last_seen_at, '
        . 'first_played_at, trial_expires_at, active_days, '
        . 'CASE WHEN first_played_at IS NULL THEN \'Chua dung thu\' '
        . 'WHEN trial_expires_at > UTC_TIMESTAMP() THEN \'Dang dung thu\' ELSE \'Het han\' END AS state '
        . 'FROM mw_trial_devices ORDER BY last_seen_at DESC LIMIT ' . $limit);
    $devices = [];
    try {
        while ($row = $rs->fetch_assoc()) $devices[] = $row;
    } finally { $rs->free(); }
    return ['summary' => $stats, 'recentDevices' => $devices];
}

/** A valid Google login on the PHONE is not proof of Admin until the server verifies it. */
function mwt_assert_google_admin(string $idToken): void {
    // Use the same administrator email that already opens the Firebase stats UI
    // in MainActivity.kt (MELOWARE_ADMIN_EMAIL); no duplicate IIS setting.
    // This value is server-controlled; never trust an email / isAdmin sent by Android.
    $adminEmail = 'ducphi0491@gmail.com';
    if (strlen($idToken) < 80 || strlen($idToken) > 8000) {
        mwt_json(['ok' => false, 'message' => 'Vui lòng đăng nhập lại Google trong MeloWave.'], 401);
    }
    // Same public Web OAuth Client ID as the existing app + paid Premium verification.
    // The client ID is public; the authorized Admin email is controlled ONLY by IIS.
    $webClientId = '886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com';
    $url = 'https://oauth2.googleapis.com/tokeninfo?id_token=' . rawurlencode($idToken);
    $curl = curl_init($url);
    if ($curl === false) throw new RuntimeException('Google OAuth curl not installed');
    curl_setopt_array($curl, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 6,
        CURLOPT_TIMEOUT => 12,
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_HTTPHEADER => ['Accept: application/json'],
    ]);
    $body = curl_exec($curl);
    $http = (int) curl_getinfo($curl, CURLINFO_HTTP_CODE);
    curl_close($curl);
    if ($body === false || $http === 0 || $http >= 500) {
        error_log('MeloWave trial admin OAuth network failure, HTTP ' . $http);
        mwt_json(['ok' => false, 'message' => 'Không kết nối được Google để xác minh Admin.'], 503);
    }
    if ($http < 200 || $http >= 300) {
        mwt_json(['ok' => false, 'message' => 'Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn.'], 401);
    }
    $claims = json_decode((string) $body, true);
    if (!is_array($claims)) throw new RuntimeException('Unexpected OAuth response');
    $issuer = (string) ($claims['iss'] ?? '');
    $audience = (string) ($claims['aud'] ?? '');
    $subject = (string) ($claims['sub'] ?? '');
    $email = strtolower(trim((string) ($claims['email'] ?? '')));
    $verified = in_array($claims['email_verified'] ?? false, [true, 'true', 1, '1'], true);
    if (!in_array($issuer, ['accounts.google.com', 'https://accounts.google.com'], true)
        || !hash_equals($webClientId, $audience)
        || (int) ($claims['exp'] ?? 0) <= time()
        || !preg_match('/^[0-9]{8,64}$/D', $subject)
        || !$verified
        || !hash_equals($adminEmail, $email)) {
        mwt_json(['ok' => false, 'message' => 'Tài khoản Google không có quyền quản trị MeloWave.'], 403);
    }
}

// Admin requests use the same PHP file, but never enter the anonymous API.
// A normal GET to this file remains POST-required, as before.
if (($_GET['admin'] ?? null) === '1') {
header('Cache-Control: private, no-store');
header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'");

$isHttps = !empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off';
$isLocalhost = in_array((string) ($_SERVER['REMOTE_ADDR'] ?? ''), ['127.0.0.1', '::1'], true);
if (!$isHttps && !$isLocalhost) { http_response_code(403); exit('HTTPS required'); }
if (session_status() !== PHP_SESSION_ACTIVE) {
    session_name('melowave_trial_admin');
    session_set_cookie_params([
        'lifetime' => 0, 'path' => '/melowave/', 'secure' => $isHttps,
        'httponly' => true, 'samesite' => 'Strict'
    ]);
    session_start();
}
function e(mixed $value): string { return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8'); }
$adminPassword = (string) getenv('MELOWAVE_TRIAL_ADMIN_PASSWORD');
$loginError = '';
if (!isset($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(16));
$canLogin = strlen($adminPassword) >= 16;
if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? '')) === 'POST') {
    if (!hash_equals((string) $_SESSION['csrf'], (string) ($_POST['csrf'] ?? ''))) {
        http_response_code(403); exit('Invalid form');
    }
    if (isset($_POST['logout'])) {
        $_SESSION = [];
        session_destroy();
        header('Location: melowave_trial.php?admin=1'); exit;
    }
    $attempted = (int) ($_SESSION['attempts'] ?? 0);
    $blockedUntil = (int) ($_SESSION['blockedUntil'] ?? 0);
    if (!$canLogin) $loginError = 'Chua cau hinh MELOWAVE_TRIAL_ADMIN_PASSWORD (toi thieu 16 ky tu).';
    elseif (time() < $blockedUntil) $loginError = 'Thu lai sau 5 phut.';
    elseif (hash_equals($adminPassword, (string) ($_POST['password'] ?? ''))) {
        session_regenerate_id(true);
        $_SESSION['authenticated'] = true;
        $_SESSION['attempts'] = 0;
    } else {
        $_SESSION['attempts'] = $attempted + 1;
        if ($attempted + 1 >= 5) {
            $_SESSION['blockedUntil'] = time() + 300;
            $_SESSION['attempts'] = 0;
        }
        $loginError = 'Mat khau khong dung.';
    }
}
$authenticated = $canLogin && !empty($_SESSION['authenticated']);
$stats = [];
$devices = [];
$reportError = '';
if ($authenticated) {
    try {
        $db = mwt_db(mwt_config());
        $report = mwt_dashboard_report($db, 50);
        $stats = $report['summary'];
        $devices = $report['recentDevices'];
        $db->close();
    } catch (Throwable $error) {
        $reportError = 'Khong the doc du lieu. Kiem tra cau hinh MySQL, 1 bang SQL va IIS.';
        error_log('MeloWave trial admin error (' . get_class($error) . ')');
    }
}
?><!doctype html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>MeloWave - Trial & Retention</title>
<style>
:root{color-scheme:dark;font-family:system-ui,-apple-system,Segoe UI,Arial,sans-serif}
*{box-sizing:border-box}body{background:#101322;color:#f2f3ff;margin:0;line-height:1.5}
main{max-width:1120px;margin:32px auto;padding:0 18px}.head{display:flex;gap:16px;align-items:center;justify-content:space-between;flex-wrap:wrap}
h1{font-size:25px;margin:0}.muted{color:#a7acc4;font-size:13px}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(175px,1fr));gap:12px;margin-top:23px}
.card,section{background:#1b2035;border:1px solid #323952;border-radius:16px;padding:18px}.number{font-weight:750;font-size:28px;margin-top:7px}
section{margin-top:18px;overflow-x:auto}h2{font-size:17px;margin:0 0 14px}
table{width:100%;border-collapse:collapse;font-size:13px}th,td{text-align:left;padding:10px;border-bottom:1px solid #333952;white-space:nowrap}th{color:#a5b0d0}
input{display:block;background:#111528;color:#fff;border:1px solid #52618d;border-radius:9px;padding:11px 13px;margin:9px 0;width:100%;max-width:380px}
button,a.btn{color:#fff;background:#6e4cda;border:0;border-radius:9px;padding:10px 16px;cursor:pointer;font-weight:650;text-decoration:none}
.warn{background:#462d32;color:#ffd1d1;padding:11px;border-radius:9px;margin:13px 0}.status{border-radius:8px;padding:4px 9px;background:#293145;white-space:nowrap}
footer{margin:24px 0;color:#8992b1;font-size:12px}
</style></head><body><main><div class="head"><div><h1>👑 MeloWave — Trial & Retention</h1>
<div class="muted">Dùng thử Premium 30 ngày / thiết bị · 1 bảng MySQL · UTC · Không chứa Gmail, IMEI hoặc Android ID gốc</div></div>
<?php if ($authenticated): ?><form method="post" action="?admin=1"><input type="hidden" name="csrf" value="<?= e($_SESSION['csrf']) ?>"><button type="submit" name="logout" value="1">Đăng xuất</button></form><?php endif; ?></div>
<?php if (!$authenticated): ?>
<section><h2>Đăng nhập quản trị</h2><?php if ($loginError !== ''): ?><div class="warn"><?= e($loginError) ?></div><?php endif; ?>
<?php if (!$canLogin): ?><div class="warn">Cần cấu hình biến môi trường MELOWAVE_TRIAL_ADMIN_PASSWORD dài tối thiểu 16 ký tự, sau đó khởi động lại IIS.</div><?php endif; ?>
<form method="post" action="?admin=1"><input type="hidden" name="csrf" value="<?= e($_SESSION['csrf']) ?>"><label>Mật khẩu quản trị</label><input name="password" type="password" required autocomplete="current-password"><button type="submit">Đăng nhập</button></form></section>
<?php else: ?>
<?php if ($reportError !== ''): ?><div class="warn"><?= e($reportError) ?></div><?php endif; ?>
<?php if ($stats): ?>
<div class="cards">
<?php foreach ([
  'Thiết bị ghi nhận' => $stats['devices'], 'Thiết bị mới hôm nay' => $stats['newToday'],
  'Chưa bắt đầu' => $stats['eligible'], 'Đang dùng thử' => $stats['active'],
  'Đã hết hạn' => $stats['expired'], 'Hoạt động hôm nay' => $stats['today'],
  'Tổng ngày thiết bị hoạt động' => $stats['activityDays'],
  'D1 quay lại' => $stats['d1'], 'D7 quay lại' => $stats['d7'], 'D30 quay lại' => $stats['d30'],
] as $label => $value): ?>
<div class="card"><div class="muted"><?= e($label) ?></div><div class="number"><?= e($value) ?></div></div>
<?php endforeach; ?>
</div>
<section><h2>50 thiết bị gần đây (mã đã ẩn bớt)</h2>
<table><thead><tr><th>Mã HMAC rút gọn</th><th>Ghi nhận lần đầu</th><th>Hoạt động gần nhất</th><th>Số ngày đã hoạt động</th><th>Bắt đầu dùng thử</th><th>Hết hạn</th><th>Trạng thái</th></tr></thead><tbody>
<?php foreach ($devices as $r): ?><tr><td><code><?= e($r['short_key']) ?>…</code></td>
<td><?= e($r['first_seen_at']) ?></td><td><?= e($r['last_seen_at']) ?></td>
<td><?= e($r['active_days']) ?></td><td><?= e($r['first_played_at'] ?? '-') ?></td><td><?= e($r['trial_expires_at'] ?? '-') ?></td>
<td><span class="status"><?= e($r['state']) ?></span></td></tr><?php endforeach; ?>
</tbody></table></section>
<footer>D1/D7/D30 là tỷ lệ mã thiết bị quay lại đúng ngày +1/+7/+30 (UTC) từ lần ghi nhận đầu tiên; mẫu số chỉ gồm thiết bị đã đủ tuổi. Một bảng ghi cờ D1/D7/D30, không lưu lịch sử từng ngày hay số lượt cài đặt. Số thiết bị ≠ số người thật; đổi thiết bị hoặc khóa ký có thể tạo mã mới. Trang chỉ đọc dữ liệu.</footer>
<?php endif; ?><?php endif; ?></main></body></html>
<?php
    exit;
}

// Anonymous trial JSON API (unchanged request and response schema).
if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? '')) !== 'POST') {
    mwt_json(['ok' => false, 'message' => 'POST required'], 405);
}
if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > 4096) {
    mwt_json(['ok' => false, 'message' => 'Request too large'], 413);
}

try {
    $raw = (string) file_get_contents('php://input', false, null, 0, 4097);
    $input = json_decode($raw, true);
    if (!is_array($input)) mwt_json(['ok' => false, 'message' => 'JSON invalid'], 400);
    $action = (string) ($input['action'] ?? '');
    if (!in_array($action, ['health', 'status', 'start', 'admin_stats'], true)) {
        mwt_json(['ok' => false, 'message' => 'Unsupported action'], 400);
    }

    if ($action === 'admin_stats') {
        mwt_assert_google_admin((string) ($input['idToken'] ?? ''));
        // Full device identifiers are NEVER returned. The API is read-only.
        $db = mwt_db(mwt_config());
        try {
            $report = mwt_dashboard_report($db, 30);
        } finally { $db->close(); }
        mwt_json([
            'ok' => true,
            'summary' => $report['summary'],
            'recentDevices' => $report['recentDevices'],
            'generatedAtMillis' => time() * 1000,
            'timezone' => 'UTC'
        ]);
    }

    $config = mwt_config();
    if ($action === 'health') {
        // Verify that the SINGLE intended table and all needed columns exist.
        $db = mwt_db($config);
        $db->query('SELECT device_key, first_seen_at, last_seen_at, last_active_day, '
            . 'active_days, first_played_at, trial_expires_at, returned_d1, returned_d7, '
            . 'returned_d30 FROM mw_trial_devices LIMIT 0')->free();
        $db->close();
        mwt_json(['ok' => true, 'trialApi' => 'ready', 'trialEnabled' => $config['enabled']]);
    }

    // On Android >= 8, ANDROID_ID is scoped to device/user/app signing key.
    // Never store or log this input; it is HMAC-hashed with a SERVER-ONLY secret.
    $androidId = strtolower(trim((string) ($input['androidId'] ?? '')));
    if (!preg_match('/^[a-f0-9]{16,32}$/D', $androidId)) {
        mwt_json(['ok' => false, 'message' => 'Invalid device identifier'], 400);
    }
    if (!$config['enabled']) {
        mwt_json([
            'ok' => true, 'trialState' => 'disabled', 'access' => false,
            'startedAtMillis' => 0, 'expiresAtMillis' => 0,
            'serverTimeMillis' => time() * 1000, 'enforceFeatures' => false
        ]);
    }

    $deviceKey = mwt_hash('device', $androidId, $config);
    $db = mwt_db($config);
    $db->begin_transaction();
    try {
        // Start of a NEW DEVICE'S history = first API request, NOT start of Premium trial.
        // Time UTC comes from MySQL, never Android.
        $s = $db->prepare('INSERT IGNORE INTO mw_trial_devices '
            . '(device_key, first_seen_at, last_seen_at, last_active_day, active_days) '
            . 'VALUES (?, UTC_TIMESTAMP(), UTC_TIMESTAMP(), UTC_DATE(), 1)');
        $s->bind_param('s', $deviceKey);
        $s->execute();
        $s->close();

        // Lock row so simultaneous status/start requests cannot restart the 30-day clock.
        $s = $db->prepare('SELECT device_key FROM mw_trial_devices WHERE device_key = ? FOR UPDATE');
        $s->bind_param('s', $deviceKey);
        $s->execute();
        $s->bind_result($lockedKey);
        if (!$s->fetch() || !hash_equals($deviceKey, (string) $lockedKey)) {
            throw new RuntimeException('Device lookup failure');
        }
        $s->close();

        // A single table is enough for D1/D7/D30 day-exact return flags.
        // active_days counts DISTINCT UTC DAYS, not each opening/refresh call.
        // last_active_day MUST be updated last to preserve the old day in the count.
        $s = $db->prepare('UPDATE mw_trial_devices SET '
            . 'last_seen_at = UTC_TIMESTAMP(), '
            . 'active_days = active_days + IF(last_active_day < UTC_DATE(), 1, 0), '
            . 'returned_d1 = IF(returned_d1 = 1 OR DATEDIFF(UTC_DATE(), DATE(first_seen_at)) = 1, 1, 0), '
            . 'returned_d7 = IF(returned_d7 = 1 OR DATEDIFF(UTC_DATE(), DATE(first_seen_at)) = 7, 1, 0), '
            . 'returned_d30 = IF(returned_d30 = 1 OR DATEDIFF(UTC_DATE(), DATE(first_seen_at)) = 30, 1, 0), '
            . 'last_active_day = UTC_DATE() WHERE device_key = ?');
        $s->bind_param('s', $deviceKey);
        $s->execute();
        $s->close();

        if ($action === 'start') {
            // A single UPDATE sets both times. Once first_played_at is non-null,
            // NO further start request can extend/restart the 30-day trial.
            $s = $db->prepare('UPDATE mw_trial_devices SET '
                . 'first_played_at = UTC_TIMESTAMP(), '
                . 'trial_expires_at = DATE_ADD(UTC_TIMESTAMP(), INTERVAL 30 DAY) '
                . 'WHERE device_key = ? AND first_played_at IS NULL');
            $s->bind_param('s', $deviceKey);
            $s->execute();
            $s->close();
        }

        $s = $db->prepare('SELECT UNIX_TIMESTAMP(first_played_at), '
            . 'UNIX_TIMESTAMP(trial_expires_at), UNIX_TIMESTAMP(UTC_TIMESTAMP()) '
            . 'FROM mw_trial_devices WHERE device_key = ?');
        $s->bind_param('s', $deviceKey);
        $s->execute();
        $s->bind_result($started, $expiry, $serverNow);
        if (!$s->fetch()) throw new RuntimeException('Device row missing');
        $s->close();
        $db->commit();
    } catch (Throwable $exception) {
        try { $db->rollback(); } catch (Throwable $ignored) {}
        throw $exception;
    }
    $db->close();

    $nowSeconds = (int) $serverNow;
    $startSeconds = (int) ($started ?? 0);
    $expirySeconds = (int) ($expiry ?? 0);
    $state = $startSeconds === 0 ? 'eligible' : ($expirySeconds > $nowSeconds ? 'active' : 'expired');
    mwt_json([
        'ok' => true,
        'trialState' => $state,
        'access' => $state === 'active',
        'trialDays' => 30,
        'startedAtMillis' => $startSeconds * 1000,
        'expiresAtMillis' => $expirySeconds * 1000,
        'serverTimeMillis' => $nowSeconds * 1000,
        'enforceFeatures' => true
    ]);
} catch (Throwable $exception) {
    // Never log Android ID or secret, SQL details, client body or personal data.
    error_log('MeloWave guest-trial error (' . get_class($exception) . ')');
    mwt_json(['ok' => false, 'message' => 'MeloWave trial server not ready'], 503);
}
