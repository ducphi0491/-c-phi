<?php
declare(strict_types=1);
/**
 * MeloWare Premium API - compatible with the MeloWare 15.10 Android client.
 * URL: https://tunertools.top/melowave/meloware_premium.php
 * PHP 8.0+, mysqli, curl, openssl. DB: existing "tuner" on 127.0.0.1:3306.
 *
 * SINGLE PHP FILE: the configuration is inside this file, NOT in a second PHP.
 * The Google Play service-account JSON (NOT a PHP file) is stored outside the
 * IIS wwwroot: C:\ProgramData\MeloWare\play-service-account.json.
 * Never put database passwords, service-account JSON, or private keys on GitHub.
 */
@ini_set('display_errors', '0');
@ini_set('log_errors', '1');
error_reporting(E_ALL);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store, no-cache, must-revalidate, private');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: no-referrer');


// ============================================================================
// CAU HINH MELOWAVE PREMIUM - CHI SUA TAI DAY / ONE PHP FILE ONLY
// ============================================================================
// IMPORTANT: Copy the MySQL password from the server's LOCAL configuration;
// do NOT send your password, service account JSON, or AES key via chat.
$MW_CONFIG = [
    'package_name' => 'duc_phi.music',
    // Matches the Google OAuth Web Client ID used by MeloWarePremium.kt.
    'google_web_client_id' => '886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com',
    'mysql' => [
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'tuner',
        'user' => 'root', // Existing account for initial testing; use limited SQL user later.
        'password' => getenv('MELOWARE_DB_PASSWORD') ?: 'NHAP_MAT_KHAU_MYSQL',
    ],
    // Google Play service account credential: never put this JSON under wwwroot.
    'service_account_json' => 'C:/ProgramData/MeloWare/play-service-account.json',
    // Make a one-time random key on your own server:
    //   php -r "echo base64_encode(random_bytes(32)), PHP_EOL;"
    // Paste the resulting Base64 into this field, or set MELOWARE_AES_KEY_B64.
    // Keep the key unchanged once receipts have been saved.
    'aes_256_key_b64' => getenv('MELOWARE_AES_KEY_B64') ?: 'NHAP_KHOA_AES_32_BYTE_BASE64',
    // Only turn on paid checkout after Google Play Developer API has been tested.
    'checkout_enabled' => false,
    'trial_enabled' => false,
    'enforce_features' => false,
    // Create only premium_accounts and premium_purchases in database tuner.
    // Can set false after a successful first run / table creation.
    'auto_create_tables' => true,
];

function mw_json(array $payload, int $http = 200): void {
    http_response_code($http);
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}
function mw_fail(int $http, string $message): void { mw_json(['ok' => false, 'message' => $message], $http); }
function mw_b64url(string $bytes): string { return rtrim(strtr(base64_encode($bytes), '+/', '-_'), '='); }
function mw_utc(int $unix): string { return gmdate('Y-m-d H:i:s', max(0, $unix)); }
function mw_millis(int $unix): int { return max(0, $unix) * 1000; }
function mw_time(string $timestamp): int {
    if (trim($timestamp) === '') return 0;
    try { return (new DateTimeImmutable($timestamp))->getTimestamp(); }
    catch (Throwable $ignored) { return 0; }
}

/** Only connect to fixed Google API hosts. Verify TLS and never follow redirects. */
function mw_google_json(string $url, string $method = 'GET', ?array $form = null, ?string $token = null): array {
    $host = (string) parse_url($url, PHP_URL_HOST);
    if (!in_array($host, ['oauth2.googleapis.com', 'androidpublisher.googleapis.com'], true)
        || parse_url($url, PHP_URL_SCHEME) !== 'https') {
        throw new RuntimeException('Unexpected Google API host');
    }
    $handle = curl_init($url);
    if ($handle === false) throw new RuntimeException('cURL not available');
    $headers = ['Accept: application/json'];
    if ($token !== null) $headers[] = 'Authorization: Bearer ' . $token;
    if ($form !== null) $headers[] = 'Content-Type: application/x-www-form-urlencoded';
    curl_setopt_array($handle, [
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 6,
        CURLOPT_TIMEOUT => 18,
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
        CURLOPT_HTTPHEADER => $headers,
        CURLOPT_CUSTOMREQUEST => $method,
        CURLOPT_ENCODING => '',
    ]);
    if ($form !== null) curl_setopt($handle, CURLOPT_POSTFIELDS, http_build_query($form));
    $body = curl_exec($handle);
    $http = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
    $curlError = $body === false ? curl_error($handle) : '';
    curl_close($handle);
    if ($body === false || $http < 200 || $http >= 300) {
        error_log('MeloWare Premium Google API failure, HTTP ' . $http . ($curlError !== '' ? ', network error' : ''));
        throw new RuntimeException('Google API request failed');
    }
    $decoded = json_decode((string) $body, true);
    if (!is_array($decoded)) throw new RuntimeException('Unexpected Google API response');
    return $decoded;
}

/** Authenticate the Google account used in the MeloWare app. Never trust email sent by Android. */
function mw_google_user(string $idToken, array $config): string {
    if (strlen($idToken) < 80 || strlen($idToken) > 8000) mw_fail(401, 'Vui lòng đăng nhập lại Google trong MeloWare.');
    $data = mw_google_json('https://oauth2.googleapis.com/tokeninfo?id_token=' . rawurlencode($idToken));
    $issuer = (string) ($data['iss'] ?? '');
    $subject = (string) ($data['sub'] ?? '');
    if (!in_array($issuer, ['accounts.google.com', 'https://accounts.google.com'], true)
        || !hash_equals((string) ($config['google_web_client_id'] ?? ''), (string) ($data['aud'] ?? ''))
        || (int) ($data['exp'] ?? 0) <= time()
        || !preg_match('/^[0-9]{8,64}$/', $subject)) {
        mw_fail(401, 'Phiên đăng nhập Google đã hết hạn hoặc không thuộc MeloWare.');
    }
    return $subject; // Google subject (stable user id), NOT the user's email.
}

/** Generate an OAuth access token for the Play Developer API using a server-side service account. */
function mw_play_access_token(array $config): string {
    $path = (string) ($config['service_account_json'] ?? '');
    if ($path === '' || !is_file($path) || !is_readable($path)) throw new RuntimeException('Google service account not installed');
    $keyData = json_decode((string) file_get_contents($path), true);
    if (!is_array($keyData) || !isset($keyData['client_email'], $keyData['private_key'])) {
        throw new RuntimeException('Google service account is invalid');
    }
    $now = time();
    $head = mw_b64url(json_encode(['alg' => 'RS256', 'typ' => 'JWT'], JSON_THROW_ON_ERROR));
    $claims = mw_b64url(json_encode([
        'iss' => (string) $keyData['client_email'],
        'scope' => 'https://www.googleapis.com/auth/androidpublisher',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3300,
    ], JSON_THROW_ON_ERROR));
    $unsigned = $head . '.' . $claims;
    if (!openssl_sign($unsigned, $signature, $keyData['private_key'], OPENSSL_ALGO_SHA256)) {
        throw new RuntimeException('Cannot sign OAuth JWT');
    }
    $jwt = $unsigned . '.' . mw_b64url($signature);
    $data = mw_google_json('https://oauth2.googleapis.com/token', 'POST', [
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt,
    ]);
    $access = (string) ($data['access_token'] ?? '');
    if ($access === '') throw new RuntimeException('Google Play access token not available');
    return $access;
}

/** Check product, prepaid base plan, expiry, state and account binding at Google Play. */
function mw_play_purchase(string $purchaseToken, array $config, string $accessToken): array {
    $packageName = rawurlencode((string) ($config['package_name'] ?? 'duc_phi.music'));
    $url = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/'
        . $packageName . '/purchases/subscriptionsv2/tokens/' . rawurlencode($purchaseToken);
    $data = mw_google_json($url, 'GET', null, $accessToken);
    $expiry = 0;
    $basePlan = '';
    foreach ((array) ($data['lineItems'] ?? []) as $line) {
        if (!is_array($line) || ($line['productId'] ?? '') !== 'meloware_premium'
            || !isset($line['prepaidPlan']) || !is_array($line['prepaidPlan'])) continue;
        $candidatePlan = (string) ($line['offerDetails']['basePlanId'] ?? '');
        if (!in_array($candidatePlan, ['premium-6m', 'premium-12m'], true)) continue;
        $candidateExpiry = mw_time((string) ($line['expiryTime'] ?? ''));
        if ($candidateExpiry > $expiry) {
            $expiry = $candidateExpiry;
            $basePlan = $candidatePlan;
        }
    }
    $state = (string) ($data['subscriptionState'] ?? '');
    return [
        'active' => $state === 'SUBSCRIPTION_STATE_ACTIVE' && $expiry > time() && $basePlan !== '',
        'state' => $state,
        'plan' => $basePlan,
        'expires' => $expiry,
        'ack_pending' => ($data['acknowledgementState'] ?? '') === 'ACKNOWLEDGEMENT_STATE_PENDING',
        'owner_hash' => (string) ($data['externalAccountIdentifiers']['obfuscatedExternalAccountId'] ?? ''),
    ];
}

/** Google Play's acknowledgement endpoint returns an empty body on success. */
function mw_play_acknowledge(string $purchaseToken, array $config, string $accessToken): void {
    $url = 'https://androidpublisher.googleapis.com/androidpublisher/v3/applications/'
        . rawurlencode((string) $config['package_name']) . '/purchases/subscriptions/'
        . rawurlencode('meloware_premium') . '/tokens/' . rawurlencode($purchaseToken) . ':acknowledge';
    $handle = curl_init($url);
    if ($handle === false) throw new RuntimeException('Could not initialize Play acknowledgement');
    curl_setopt_array($handle, [
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => '{}',
        CURLOPT_HTTPHEADER => [
            'Accept: application/json', 'Content-Type: application/json',
            'Authorization: Bearer ' . $accessToken,
        ],
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 6,
        CURLOPT_TIMEOUT => 18,
        CURLOPT_FOLLOWLOCATION => false,
        CURLOPT_SSL_VERIFYPEER => true,
        CURLOPT_SSL_VERIFYHOST => 2,
    ]);
    $response = curl_exec($handle);
    $status = (int) curl_getinfo($handle, CURLINFO_HTTP_CODE);
    curl_close($handle);
    if ($response === false || $status < 200 || $status >= 300) {
        error_log('MeloWare Premium Play acknowledgement failure, HTTP ' . $status);
        throw new RuntimeException('Google Play acknowledgement unsuccessful');
    }
}

function mw_aes_key(array $config): string {
    $key = base64_decode((string) ($config['aes_256_key_b64'] ?? ''), true);
    if ($key === false || strlen($key) !== 32) throw new RuntimeException('Purchase token encryption key missing');
    return $key;
}
function mw_encrypt_token(string $token, array $config): string {
    $iv = random_bytes(12); $tag = '';
    $encrypted = openssl_encrypt($token, 'aes-256-gcm', mw_aes_key($config), OPENSSL_RAW_DATA, $iv, $tag);
    if ($encrypted === false) throw new RuntimeException('Purchase token encryption failure');
    return base64_encode($iv . $tag . $encrypted);
}
function mw_decrypt_token(string $encrypted, array $config): string {
    $raw = base64_decode($encrypted, true);
    if ($raw === false || strlen($raw) < 29) throw new RuntimeException('Invalid encrypted purchase token');
    $plain = openssl_decrypt(substr($raw, 28), 'aes-256-gcm', mw_aes_key($config), OPENSSL_RAW_DATA,
        substr($raw, 0, 12), substr($raw, 12, 16));
    if ($plain === false) throw new RuntimeException('Purchase token decryption failure');
    return $plain;
}

/** mysqlnd-independent prepared statement row reader (works with mysqli on IIS). */
function mw_rows(mysqli_stmt $stmt): array {
    $metadata = $stmt->result_metadata();
    if (!$metadata instanceof mysqli_result) return [];
    $row = []; $refs = [];
    while ($column = $metadata->fetch_field()) {
        $row[$column->name] = null;
        $refs[] = &$row[$column->name];
    }
    $metadata->free();
    if (!$refs) return [];
    call_user_func_array([$stmt, 'bind_result'], $refs);
    $results = [];
    while ($stmt->fetch()) {
        $copy = [];
        foreach ($row as $key => $value) $copy[$key] = $value;
        $results[] = $copy;
    }
    return $results;
}
function mw_select(mysqli $db, string $sql, string $param, string $type = 's'): array {
    $stmt = $db->prepare($sql);
    try {
        $stmt->bind_param($type, $param);
        $stmt->execute();
        return mw_rows($stmt);
    } finally { $stmt->close(); }
}
function mw_run(mysqli $db, string $sql, string $types, &...$params): void {
    $stmt = $db->prepare($sql);
    try {
        if ($types !== '') $stmt->bind_param($types, ...$params);
        $stmt->execute();
    } finally { $stmt->close(); }
}
function mw_connect_db(array $config): mysqli {
    $dbCfg = (array) ($config['mysql'] ?? []);
    $host = (string) ($dbCfg['host'] ?? '127.0.0.1');
    $port = (int) ($dbCfg['port'] ?? 3306);
    $database = (string) ($dbCfg['name'] ?? 'tuner');
    $username = (string) ($dbCfg['user'] ?? '');
    $password = (string) ($dbCfg['password'] ?? '');
    if ($username === '' || $password === '' || $password === 'REPLACE_WITH_DB_PASSWORD' || $password === 'NHAP_MAT_KHAU_MYSQL') {
        throw new RuntimeException('Database not configured');
    }
    mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
    $db = new mysqli($host, $username, $password, $database, $port);
    $db->set_charset('utf8mb4');
    $db->query("SET time_zone = '+00:00'");
    return $db;
}

/**
 * Optional one-time schema initializer, kept inside the single PHP file.
 * Does not create a new database and never changes existing TunerTools tables.
 */
function mw_ensure_premium_tables(mysqli $db): void {
    $db->query(<<<'SQL'
CREATE TABLE IF NOT EXISTS premium_accounts (
    google_sub VARCHAR(64) NOT NULL,
    trial_started_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (google_sub)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL
    );
    $db->query(<<<'SQL'
CREATE TABLE IF NOT EXISTS premium_purchases (
    token_hash CHAR(64) NOT NULL,
    google_sub VARCHAR(64) NOT NULL,
    encrypted_token TEXT NOT NULL,
    plan_id VARCHAR(32) NOT NULL,
    expires_at DATETIME NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'inactive',
    verified_at DATETIME NOT NULL,
    PRIMARY KEY (token_hash),
    INDEX idx_account_expiry (google_sub, expires_at),
    CONSTRAINT fk_mw_premium_user FOREIGN KEY (google_sub)
        REFERENCES premium_accounts (google_sub) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
SQL
    );
}

function mw_account(mysqli $db, string $googleSub, array $config): array {
    mw_run($db, 'INSERT IGNORE INTO premium_accounts (google_sub) VALUES (?)', 's', $googleSub);
    if (!empty($config['trial_enabled'])) {
        mw_run($db, 'UPDATE premium_accounts SET trial_started_at = UTC_TIMESTAMP() WHERE google_sub = ? AND trial_started_at IS NULL', 's', $googleSub);
    }
    return mw_select($db, 'SELECT trial_started_at FROM premium_accounts WHERE google_sub = ? LIMIT 1', $googleSub)[0] ?? [];
}

/** Refresh stored Google Play receipts at most every 15 minutes per token. */
function mw_refresh_receipts(mysqli $db, string $googleSub, array $config): void {
    $receipts = mw_select($db, 'SELECT token_hash, encrypted_token, verified_at FROM premium_purchases WHERE google_sub = ? AND expires_at > UTC_TIMESTAMP()', $googleSub);
    $accessToken = null;
    foreach ($receipts as $receipt) {
        $last = strtotime((string) ($receipt['verified_at'] ?? '') . ' UTC') ?: 0;
        if (time() - $last < 900) continue;
        try {
            if ($accessToken === null) $accessToken = mw_play_access_token($config);
            $purchaseToken = mw_decrypt_token((string) $receipt['encrypted_token'], $config);
            $play = mw_play_purchase($purchaseToken, $config, $accessToken);
            // Strictly maintain the original account binding even when re-checking a receipt.
            $expected = hash('sha256', 'meloware:' . $googleSub);
            if ((string) $play['owner_hash'] !== '' && !hash_equals($expected, (string) $play['owner_hash'])) {
                $play['active'] = false;
            }
            if ($play['active'] && $play['ack_pending']) {
                mw_play_acknowledge($purchaseToken, $config, $accessToken);
            }
            $plan = (string) $play['plan'];
            if ($plan === '') $plan = 'unknown';
            $expiry = mw_utc((int) $play['expires']);
            $status = $play['active'] ? 'active' : 'inactive';
            $hash = (string) $receipt['token_hash'];
            mw_run($db,
                'UPDATE premium_purchases SET plan_id = ?, expires_at = ?, status = ?, verified_at = UTC_TIMESTAMP() WHERE token_hash = ? AND google_sub = ?',
                'sssss', $plan, $expiry, $status, $hash, $googleSub);
        } catch (Throwable $e) {
            error_log('MeloWare Premium: Google Play receipt refresh failed');
            // Keep the most recently confirmed status until its known expiration.
        }
    }
}

/** Response keys must remain compatible with MeloWarePremium.kt. */
function mw_status(mysqli $db, string $googleSub, array $config): array {
    $account = mw_account($db, $googleSub, $config);
    mw_refresh_receipts($db, $googleSub, $config);
    $purchases = mw_select($db,
        "SELECT plan_id, UNIX_TIMESTAMP(expires_at) AS expiry FROM premium_purchases WHERE google_sub = ? AND status = 'active' AND expires_at > UTC_TIMESTAMP() ORDER BY expires_at DESC LIMIT 1",
        $googleSub);
    $purchase = $purchases[0] ?? null;
    $trialStart = !empty($account['trial_started_at']) ? (strtotime((string) $account['trial_started_at'] . ' UTC') ?: 0) : 0;
    $trialEnd = $trialStart > 0 ? $trialStart + (30 * 86400) : 0;
    $expiry = $purchase !== null ? (int) $purchase['expiry'] : 0;
    $kind = 'none';
    if ($expiry > time()) $kind = 'paid';
    elseif (!empty($config['trial_enabled']) && $trialEnd > time()) { $kind = 'trial'; $expiry = $trialEnd; }
    $keyReady = false;
    try { mw_aes_key($config); $keyReady = true; } catch (Throwable $ignored) {}
    $googleReady = !empty($config['service_account_json']) && is_file((string) $config['service_account_json']);
    return [
        'ok' => true,
        'access' => $kind !== 'none',
        'accessKind' => $kind,
        'expiresAtMillis' => mw_millis($expiry),
        'trialEndMillis' => mw_millis($trialEnd),
        'planId' => $purchase['plan_id'] ?? null,
        'checkoutEnabled' => !empty($config['checkout_enabled']) && $keyReady && $googleReady,
        'enforceFeatures' => !empty($config['enforce_features']),
        'serverTimeMillis' => mw_millis(time()),
    ];
}

if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET')) !== 'POST') mw_fail(405, 'POST required');
if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > 16000) mw_fail(413, 'Request too large');
try {
    $config = $MW_CONFIG;
    if (!is_array($config) || (string) ($config['package_name'] ?? '') !== 'duc_phi.music') {
        throw new RuntimeException('Premium server config invalid');
    }
    $input = json_decode((string) file_get_contents('php://input', false, null, 0, 16001), true);
    if (!is_array($input)) mw_fail(400, 'JSON không hợp lệ.');
    $action = (string) ($input['action'] ?? '');
    if (!in_array($action, ['status', 'verify'], true)) mw_fail(400, 'Thao tác không hợp lệ.');
    $googleSub = mw_google_user((string) ($input['idToken'] ?? ''), $config);
    $db = mw_connect_db($config);
    if (!empty($config['auto_create_tables'])) mw_ensure_premium_tables($db);
    if ($action === 'status') mw_json(mw_status($db, $googleSub, $config));

    $purchaseToken = (string) ($input['purchaseToken'] ?? '');
    if (strlen($purchaseToken) < 10 || strlen($purchaseToken) > 2048) mw_fail(400, 'Mã giao dịch không hợp lệ.');
    $googleAccess = mw_play_access_token($config);
    $play = mw_play_purchase($purchaseToken, $config, $googleAccess);
    if (!$play['active']) mw_fail(409, 'Google Play chưa xác nhận gói Premium trả trước còn hiệu lực.');
    $expectedOwnerHash = hash('sha256', 'meloware:' . $googleSub);
    // The Android app sends this value via setObfuscatedAccountId() on checkout.
    // Do not accept receipts with a missing owner binding or one from another account.
    if (!hash_equals($expectedOwnerHash, (string) $play['owner_hash'])) {
        mw_fail(409, 'Giao dịch không thuộc tài khoản Google đang đăng nhập trong MeloWare.');
    }
    // Acknowledge before granting entitlement; otherwise Play may refund an unacknowledged order.
    if ($play['ack_pending']) mw_play_acknowledge($purchaseToken, $config, $googleAccess);
    $digest = hash('sha256', $purchaseToken);
    $encrypted = mw_encrypt_token($purchaseToken, $config);
    $plan = (string) $play['plan'];
    $expiry = mw_utc((int) $play['expires']);
    $db->begin_transaction();
    try {
        mw_account($db, $googleSub, $config);
        $existing = mw_select($db, 'SELECT google_sub FROM premium_purchases WHERE token_hash = ? LIMIT 1 FOR UPDATE', $digest);
        if (isset($existing[0]) && (string) $existing[0]['google_sub'] !== $googleSub) {
            $db->rollback();
            mw_fail(409, 'Giao dịch đã được liên kết với tài khoản khác.');
        }
        mw_run($db,
            "INSERT INTO premium_purchases (token_hash, google_sub, encrypted_token, plan_id, expires_at, status, verified_at) VALUES (?, ?, ?, ?, ?, 'active', UTC_TIMESTAMP()) ON DUPLICATE KEY UPDATE plan_id = VALUES(plan_id), expires_at = VALUES(expires_at), status = 'active', verified_at = UTC_TIMESTAMP()",
            'sssss', $digest, $googleSub, $encrypted, $plan, $expiry);
        // A concurrent request must never allow an existing hash to grant a new owner.
        $checkOwner = mw_select($db, 'SELECT google_sub FROM premium_purchases WHERE token_hash = ? LIMIT 1 FOR UPDATE', $digest);
        if (empty($checkOwner[0]) || (string) $checkOwner[0]['google_sub'] !== $googleSub) {
            $db->rollback();
            mw_fail(409, 'Không thể xác nhận tài khoản sở hữu giao dịch.');
        }
        $db->commit();
    } catch (Throwable $error) {
        try { $db->rollback(); } catch (Throwable $ignored) {}
        throw $error;
    }
    mw_json(mw_status($db, $googleSub, $config));
} catch (Throwable $error) {
    // Never write raw ID tokens, purchase tokens, DB credentials, or Google keys to the log.
    error_log('MeloWare Premium backend error (' . get_class($error) . ')');
    mw_fail(503, 'Máy chủ Premium chưa sẵn sàng hoặc Google Play API tạm thời lỗi. Vui lòng thử lại.');
}
