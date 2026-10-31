<?php
// Tắt hiển thị lỗi ra màn hình để bảo mật cho bản chính thức
ini_set('display_errors', 0);
ini_set('display_startup_errors', 0);
error_reporting(0);

// Tăng giới hạn thời gian chạy phòng trường hợp mạng chậm khi tải lượng data khổng lồ
set_time_limit(300); 

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

// --- 1. Cấu hình kết nối MySQL (Sử dụng MySQLi) ---
$servername = "127.0.0.1";
$username = "root";
$password = "123456";    
$dbname = "tuner";       
$port = 3306;

// Kết nối DB: không để ngoại lệ mysqli tạo trang HTML 500 không có mã lỗi.
try {
    $conn = new mysqli($servername, $username, $password, $dbname, $port);
    if ($conn->connect_errno) {
        throw new RuntimeException('Database connection was rejected: ' . $conn->connect_error);
    }
    if (!$conn->set_charset('utf8mb4')) {
        throw new RuntimeException('Cannot set database encoding to utf8mb4');
    }
} catch (Throwable $e) {
    error_log('[TunerTools][DB_CONNECTION_FAILED] ' . $e->getMessage());
    http_response_code(503);
    echo json_encode([
        'success' => false,
        'code' => 'DB_CONNECTION_FAILED',
        'error' => 'Không kết nối được MySQL. Kiểm tra nhật ký PHP máy chủ.'
    ], JSON_UNESCAPED_UNICODE);
    exit;
}

// --- 2. Nhận tham số từ App Android ---
$action = isset($_GET['action']) ? $_GET['action'] : '';

// Các trang Sync V2 nhỏ được nén gzip để giảm dung lượng tải lời/hợp âm.
// Không áp dụng cho action sync cũ vì action đó đang stream trực tiếp từng record.
if (in_array($action, ['check_update_v2', 'sync_bootstrap_v2', 'sync_v2'], true)) {
    header('Cache-Control: no-store, no-cache, must-revalidate, max-age=0');
    header('Pragma: no-cache');
    // No ob_gzhandler here: some hosting compression stacks reject nested handlers.
}


// =========================================================
// CẤU HÌNH FIREBASE REALTIME DATABASE CHO ADMIN LIÊN HỆ USER
// =========================================================
// Ưu tiên biến môi trường FIREBASE_DATABASE_URL nếu máy chủ đã cấu hình.
// Nếu chưa có, hệ thống sẽ tự thử URL Singapore và URL mặc định của project.
// Ví dụ URL Singapore:
// https://tuner-tools-default-rtdb.asia-southeast1.firebasedatabase.app
$firebase_service_account_path = __DIR__ . '/firebase-service-account.json';
$firebase_project_id = 'tuner-tools';

/** Trả JSON và dừng xử lý. */
function tt_json_exit($payload, $httpCode = 200) {
    http_response_code($httpCode);
    echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}


/**
 * Safe error reporting for Sync V2. Internal SQL and credentials are never
 * returned to a public caller. See PHP error log for the matching trace id.
 */
function tt_song_sync_abort($code, $exception, $status = 500) {
    try {
        $trace = bin2hex(random_bytes(5));
    } catch (Throwable $ignored) {
        $trace = (string)time();
    }
    error_log('[TunerTools][SyncV2][' . $trace . '][' . $code . '] ' . $exception->getMessage());
    tt_json_exit([
        'success' => false,
        'code' => $code,
        'trace_id' => $trace,
        'error' => 'Lỗi đồng bộ phía máy chủ. Kiểm tra PHP error log theo trace_id.'
    ], $status);
}

// =========================================================
// SONG DELTA SYNC V2
// - Giữ nguyên action check_update/sync cho các bản app cũ.
// - App mới dùng cursor change_id và tải theo từng trang nhỏ.
// - Bảng + trigger được tạo bởi ALTER_song_sync_v2.sql.
// =========================================================
function tt_song_sync_schema_ready($conn) {
    static $ready = null;
    if ($ready !== null) return $ready;

    try {
        $result = $conn->query("SHOW TABLES LIKE 'song_sync_changes'");
        if (!$result) throw new RuntimeException('SHOW TABLES failed: ' . $conn->error);
        $ready = $result->num_rows > 0;
        $result->free();
        return $ready;
    } catch (Throwable $e) {
        tt_song_sync_abort('SYNC_SCHEMA_QUERY_FAILED', $e);
    }
}

function tt_song_sync_latest_cursor($conn) {
    if (!tt_song_sync_schema_ready($conn)) return 0;
    try {
        $result = $conn->query("SELECT COALESCE(MAX(change_id), 0) AS cursor FROM song_sync_changes");
        if (!$result) throw new RuntimeException('Cursor query failed: ' . $conn->error);
        $row = $result->fetch_assoc();
        $result->free();
        return (int)($row['cursor'] ?? 0);
    } catch (Throwable $e) {
        tt_song_sync_abort('SYNC_CURSOR_QUERY_FAILED', $e);
    }
}

function tt_song_sync_require_schema($conn) {
    if (tt_song_sync_schema_ready($conn)) return;
    tt_json_exit([
        'success' => false,
        'error' => 'Chưa cài bảng song_sync_changes. Hãy chạy ALTER_song_sync_v2.sql trước.',
        'code' => 'SYNC_SCHEMA_MISSING'
    ], 409);
}

function tt_song_sync_normalize_limit($value, $default = 200, $max = 500) {
    $limit = (int)$value;
    if ($limit <= 0) $limit = $default;
    return max(1, min($max, $limit));
}

function tt_song_sync_valid_timestamp($value) {
    $value = trim((string)$value);
    if ($value === '') return '2000-01-01 00:00:00';
    $date = DateTime::createFromFormat('Y-m-d H:i:s', $value);
    $errors = DateTime::getLastErrors();
    if (!$date || (is_array($errors) && (($errors['warning_count'] ?? 0) > 0 || ($errors['error_count'] ?? 0) > 0))) {
        return '2000-01-01 00:00:00';
    }
    return $date->format('Y-m-d H:i:s');
}

function tt_song_sync_cast_song_row($song) {
    if (!is_array($song)) return [];

    foreach (['favoriteCount', 'viewCount', 'commentCount', 'ratingCount', 'bpm', 'image_count', 'status'] as $field) {
        if (array_key_exists($field, $song)) $song[$field] = (int)$song[$field];
    }
    if (array_key_exists('ratingStars', $song)) $song['ratingStars'] = (float)$song['ratingStars'];
    if (!isset($song['id_zing_mp3']) || $song['id_zing_mp3'] === null) $song['id_zing_mp3'] = '';
    if (!isset($song['lyrics']) || $song['lyrics'] === null) $song['lyrics'] = '';
    return $song;
}

/** Chuẩn hóa email để đối chiếu với tuner.songs_app_admins.email. */
function tt_normalize_email($email) {
    return strtolower(trim((string)$email));
}

/**
 * Lấy tài khoản quản trị từ bảng songs_app_admins.
 * Chỉ các role super_admin / admin / mod mới được xem là nhân sự quản trị.
 */
function tt_get_admin_account($conn, $email) {
    $email = tt_normalize_email($email);
    if ($email === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
        return null;
    }

    $stmt = $conn->prepare(
        "SELECT id, email, role, created_at
         FROM songs_app_admins
         WHERE LOWER(email) = LOWER(?)
         LIMIT 1"
    );
    if (!$stmt) return null;

    $stmt->bind_param('s', $email);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result ? $result->fetch_assoc() : null;
    $stmt->close();

    if (!$row) return null;

    $role = strtolower(trim((string)($row['role'] ?? '')));
    if (!in_array($role, ['super_admin', 'admin', 'mod'], true)) {
        return null;
    }

    $row['role'] = $role;
    $row['email'] = tt_normalize_email($row['email'] ?? $email);
    return $row;
}

/** Tạo tên hiển thị dự phòng từ email khi app không gửi display name. */
function tt_display_name_from_email($email) {
    $email = trim((string)$email);
    if ($email === '') return 'Quản trị viên';
    $name = explode('@', $email)[0];
    $name = str_replace(['.', '_', '-'], ' ', $name);
    $name = preg_replace('/\\s+/', ' ', $name);
    return ucwords(trim($name));
}

/** Mã hóa một segment dùng làm key/path trong Firebase Realtime Database. */
function tt_firebase_path_segment($value) {
    return rawurlencode((string)$value);
}

/**
 * Danh sách URL Realtime Database có thể dùng.
 * - Đọc FIREBASE_DATABASE_URL nếu có.
 * - Đọc project_info.firebase_url nếu google-services.json nằm cạnh PHP.
 * - Thử Singapore rồi URL mặc định của Firebase.
 */
function tt_get_database_url_candidates($projectId) {
    $urls = [];

    $envUrl = trim((string)getenv('FIREBASE_DATABASE_URL'));
    if ($envUrl !== '') {
        $urls[] = rtrim($envUrl, '/');
    }

    $googleServicesPath = __DIR__ . '/google-services.json';
    if (file_exists($googleServicesPath)) {
        $googleServices = json_decode(file_get_contents($googleServicesPath), true);
        $firebaseUrl = $googleServices['project_info']['firebase_url'] ?? '';
        if (is_string($firebaseUrl) && trim($firebaseUrl) !== '') {
            $urls[] = rtrim(trim($firebaseUrl), '/');
        }
    }

    $urls[] = "https://{$projectId}-default-rtdb.asia-southeast1.firebasedatabase.app";
    $urls[] = "https://{$projectId}-default-rtdb.firebaseio.com";

    return array_values(array_unique(array_filter($urls)));
}

// =========================================================
// LẤY DANH SÁCH ÂM THANH METRONOME TỪ Ổ C:\AI_Music\METRONOME
// =========================================================
if ($action === 'get_metronome_sounds') {
    $dir = 'C:/AI_Music/METRONOME'; 
    $sounds = [];
    
    if (is_dir($dir)) {
        $files = scandir($dir);
        foreach ($files as $file) {
            if (strtolower(pathinfo($file, PATHINFO_EXTENSION)) === 'wav') {
                $baseName = str_replace(['.wav', '.WAV'], '', $file);
                $displayName = ucfirst($baseName);
                $sounds[] = $displayName;
            }
        }
    }
    
    if (empty($sounds)) {
        if (!is_dir($dir)) {
            $sounds = ["LỖI: Không thấy thư mục $dir"]; 
        } else {
            $sounds = ["LỖI: Thư mục $dir không có file .wav"]; 
        }
    }
    
    echo json_encode($sounds);
    exit;
}

// =========================================================
// HÀM HỖ TRỢ ĐẨY THÔNG BÁO GOOGLE FIREBASE (FCM HTTP v1)
// =========================================================
function get_firebase_access_token($service_account_path, $scopes) {
    // Một request admin_start_contact có thể gọi RTDB nhiều lần liên tiếp.
    // Cache access token trong chính request PHP hiện tại để không phải xin OAuth token
    // lại cho từng GET/PUT. Điều này tránh lỗi ngẫu nhiên HTTP 0 ở lần gọi thứ 2/3.
    static $tokenCache = [];

    if (!file_exists($service_account_path)) return null;

    if (is_array($scopes)) {
        $scopes = implode(' ', array_values(array_unique($scopes)));
    }
    $scopes = trim((string)$scopes);

    $cacheKey = $service_account_path . '|' . $scopes;
    $now = time();
    if (isset($tokenCache[$cacheKey])) {
        $cached = $tokenCache[$cacheKey];
        if (!empty($cached['token']) && (int)($cached['expires_at'] ?? 0) > ($now + 60)) {
            return $cached['token'];
        }
    }

    $key = json_decode(file_get_contents($service_account_path), true);
    if (!$key || empty($key['client_email']) || empty($key['private_key']) || empty($key['token_uri'])) {
        return null;
    }

    $header = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
    $claim = json_encode([
        'iss' => $key['client_email'],
        'scope' => (string)$scopes,
        'aud' => $key['token_uri'],
        'exp' => $now + 3600,
        'iat' => $now
    ]);

    $base64UrlHeader = rtrim(strtr(base64_encode($header), '+/', '-_'), '=');
    $base64UrlClaim = rtrim(strtr(base64_encode($claim), '+/', '-_'), '=');
    $signatureInput = $base64UrlHeader . '.' . $base64UrlClaim;

    $signed = openssl_sign(
        $signatureInput,
        $signature,
        $key['private_key'],
        OPENSSL_ALGO_SHA256
    );
    if (!$signed) return null;

    $base64UrlSignature = rtrim(strtr(base64_encode($signature), '+/', '-_'), '=');
    $jwt = $signatureInput . '.' . $base64UrlSignature;

    $ch = curl_init();
    curl_setopt_array($ch, [
        CURLOPT_URL => $key['token_uri'],
        CURLOPT_POST => true,
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_POSTFIELDS => http_build_query([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ]),
        CURLOPT_CONNECTTIMEOUT => 15,
        CURLOPT_TIMEOUT => 30,
        // Giữ tương thích với cấu hình máy chủ hiện tại.
        CURLOPT_SSL_VERIFYPEER => false
    ]);

    $response = curl_exec($ch);
    $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);

    if ($httpCode < 200 || $httpCode >= 300 || !$response) return null;

    $data = json_decode($response, true);
    $token = $data['access_token'] ?? null;
    if (!$token) return null;

    $expiresIn = max(300, (int)($data['expires_in'] ?? 3600));
    $tokenCache[$cacheKey] = [
        'token' => $token,
        'expires_at' => time() + $expiresIn
    ];
    return $token;
}

function get_fcm_access_token($service_account_path) {
    return get_firebase_access_token(
        $service_account_path,
        ['https://www.googleapis.com/auth/firebase.messaging']
    );
}

function get_realtime_database_access_token($service_account_path) {
    return get_firebase_access_token(
        $service_account_path,
        [
            'https://www.googleapis.com/auth/firebase.database',
            'https://www.googleapis.com/auth/userinfo.email'
        ]
    );
}

/**
 * Gọi Firebase Realtime Database REST API bằng service account.
 * Kết quả: success, http_code, data, raw, error, database_url.
 */
function tt_realtime_database_request(
    $method,
    $relativePath,
    $payload = null,
    $serviceAccountPath = null,
    $projectId = 'tuner-tools'
) {
    if ($serviceAccountPath === null) {
        $serviceAccountPath = __DIR__ . '/firebase-service-account.json';
    }

    $accessToken = get_realtime_database_access_token($serviceAccountPath);
    if (!$accessToken) {
        return [
            'success' => false,
            'http_code' => 0,
            'data' => null,
            'raw' => '',
            'error' => 'Không lấy được Firebase access token. Kiểm tra firebase-service-account.json.',
            'database_url' => ''
        ];
    }

    $pathParts = array_filter(explode('/', trim((string)$relativePath, '/')), 'strlen');
    $encodedPath = implode('/', array_map('tt_firebase_path_segment', $pathParts));
    $lastResult = null;

    foreach (tt_get_database_url_candidates($projectId) as $databaseUrl) {
        $url = rtrim($databaseUrl, '/') . '/' . $encodedPath . '.json';
        $ch = curl_init($url);

        $options = [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST => strtoupper($method),
            CURLOPT_CONNECTTIMEOUT => 15,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_HTTPHEADER => [
                'Authorization: Bearer ' . $accessToken,
                'Content-Type: application/json; charset=utf-8'
            ]
        ];

        if ($payload !== null && strtoupper($method) !== 'GET') {
            $options[CURLOPT_POSTFIELDS] = json_encode(
                $payload,
                JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
            );
        }

        curl_setopt_array($ch, $options);
        $raw = curl_exec($ch);
        $curlError = curl_error($ch);
        $httpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $decoded = null;
        if (is_string($raw) && $raw !== '') {
            $decoded = json_decode($raw, true);
        }

        $lastResult = [
            'success' => $curlError === '' && $httpCode >= 200 && $httpCode < 300,
            'http_code' => $httpCode,
            'data' => $decoded,
            'raw' => is_string($raw) ? $raw : '',
            'error' => $curlError,
            'database_url' => $databaseUrl
        ];

        if ($lastResult['success']) {
            return $lastResult;
        }

        // URL Database không đúng thường trả 404. Khi đó thử URL ứng viên tiếp theo.
        if (!in_array($httpCode, [0, 404], true)) {
            break;
        }
    }

    return $lastResult ?: [
        'success' => false,
        'http_code' => 0,
        'data' => null,
        'raw' => '',
        'error' => 'Không xác định được Realtime Database URL.',
        'database_url' => ''
    ];
}

/**
 * Phát một tín hiệu nhẹ lên Realtime Database sau mỗi thay đổi tin nhắn.
 * Không lưu nội dung chat tại RTDB; client nhận event rồi tải nội dung chuẩn từ MySQL.
 */
function tt_publish_support_ticket_event(
    $deviceId,
    $ticketId,
    $sender,
    $status,
    $messageCount,
    $serviceAccountPath,
    $projectId
) {
    $deviceId = trim((string)$deviceId);
    $ticketId = trim((string)$ticketId);

    if ($deviceId === '' || $ticketId === '') {
        return [
            'success' => false,
            'http_code' => 0,
            'error' => 'Thiếu device_id hoặc ticket_id.',
            'database_url' => ''
        ];
    }

    $nowMs = (int)round(microtime(true) * 1000);
    $payload = [
        'ticket_id' => $ticketId,
        'device_id' => $deviceId,
        'last_sender' => (string)$sender,
        'status' => (int)$status,
        'message_count' => (int)$messageCount,
        'revision' => $nowMs,
        'updated_at' => $nowMs
    ];

    return tt_realtime_database_request(
        'PUT',
        'support_ticket_events/' . $deviceId . '/' . $ticketId,
        $payload,
        $serviceAccountPath,
        $projectId
    );
}



// =========================================================
// HỘI THOẠI HỖ TRỢ DUY NHẤT THEO USER
// =========================================================
// Mỗi người dùng chỉ có 1 thread support. Nếu đã đăng nhập Google thì khóa ưu tiên
// theo email; nếu là guest thì khóa theo device_id. Các ticket cũ trùng nhau được
// gộp lịch sử và đánh dấu merged_into_id để mọi ID cũ vẫn có thể redirect về thread chính.
function tt_support_merge_schema_ready($conn) {
    static $ready = null;
    if ($ready !== null) return $ready;

    $required = ['conversation_key', 'user_email', 'merged_into_id'];
    $found = [];
    $result = $conn->query("SHOW COLUMNS FROM support_tickets");
    if ($result) {
        while ($row = $result->fetch_assoc()) {
            $found[(string)$row['Field']] = true;
        }
    }
    $ready = true;
    foreach ($required as $column) {
        if (empty($found[$column])) {
            $ready = false;
            break;
        }
    }
    return $ready;
}

function tt_support_read_receipt_schema_ready($conn) {
    static $ready = null;
    if ($ready !== null) return $ready;

    $result = $conn->query("SHOW COLUMNS FROM support_tickets LIKE 'admin_read'");
    $ready = $result && $result->num_rows > 0;
    if ($result) $result->free();
    return $ready;
}


/**
 * Xac dinh nguoi gui tin nhan cuoi cung cua mot ticket.
 * Neu chat_history cu/trong, dung status lam fallback:
 *   status=0 => tin cuoi tu User, status=1 => tin cuoi tu Admin.
 */
function tt_support_last_sender($ticket) {
    $rawHistory = trim((string)($ticket['chat_history'] ?? ''));
    if ($rawHistory !== '' && $rawHistory !== 'null') {
        $history = json_decode($rawHistory, true);
        if (is_array($history) && !empty($history)) {
            $last = $history[count($history) - 1];
            if (is_array($last)) {
                $sender = strtolower(trim((string)($last['sender'] ?? '')));
                if ($sender !== '') return $sender;
            }
        }
    }

    if (trim((string)($ticket['message'] ?? '')) !== '') {
        return ((int)($ticket['status'] ?? 0) === 1) ? 'admin' : 'user';
    }

    return ((int)($ticket['status'] ?? 0) === 1) ? 'admin' : 'user';
}

function tt_support_valid_email($email) {
    $email = tt_normalize_email($email);
    return $email !== '' && $email !== 'guest' && filter_var($email, FILTER_VALIDATE_EMAIL);
}

function tt_support_conversation_key($email, $deviceId) {
    $email = tt_normalize_email($email);
    $deviceId = trim((string)$deviceId);
    if (tt_support_valid_email($email)) {
        return 'email:' . $email;
    }
    return 'device:' . strtolower($deviceId);
}

function tt_support_resolve_ticket_id($conn, $ticketId) {
    $ticketId = trim((string)$ticketId);
    if ($ticketId === '' || !ctype_digit($ticketId) || !tt_support_merge_schema_ready($conn)) {
        return $ticketId;
    }

    $current = $ticketId;
    for ($i = 0; $i < 8; $i++) {
        $stmt = $conn->prepare("SELECT merged_into_id FROM support_tickets WHERE id = ? LIMIT 1");
        if (!$stmt) return $current;
        $stmt->bind_param('s', $current);
        $stmt->execute();
        $res = $stmt->get_result();
        $row = $res ? $res->fetch_assoc() : null;
        $stmt->close();
        if (!$row) return $current;
        $next = trim((string)($row['merged_into_id'] ?? ''));
        if ($next === '' || $next === '0' || $next === $current) return $current;
        $current = $next;
    }
    return $current;
}

function tt_support_history_from_row($row) {
    $messages = [];
    $raw = trim((string)($row['chat_history'] ?? ''));
    if ($raw !== '' && $raw !== 'null') {
        $decoded = json_decode($raw, true);
        if (is_array($decoded)) {
            foreach ($decoded as $msg) {
                if (!is_array($msg)) continue;
                $text = trim((string)($msg['text'] ?? ''));
                if ($text === '') continue;
                $clean = $msg;
                $clean['sender'] = (($msg['sender'] ?? 'user') === 'admin') ? 'admin' : 'user';
                $clean['text'] = $text;
                $clean['timestamp'] = trim((string)($msg['timestamp'] ?? ($row['created_at'] ?? '')));
                $messages[] = $clean;
            }
        }
    }

    // Ticket cũ do user tự tạo thường chưa có chat_history.
    if (empty($messages)) {
        $text = trim((string)($row['message'] ?? ''));
        if ($text !== '') {
            $messages[] = [
                'sender' => 'user',
                'text' => $text,
                'timestamp' => trim((string)($row['created_at'] ?? ''))
            ];
        }
    }

    return $messages;
}

function tt_support_merge_histories($rows) {
    $all = [];
    $seq = 0;
    foreach ($rows as $row) {
        foreach (tt_support_history_from_row($row) as $message) {
            $timestamp = trim((string)($message['timestamp'] ?? ''));
            $ts = $timestamp !== '' ? strtotime($timestamp) : false;
            $message['_sort_ts'] = ($ts !== false) ? (int)$ts : 0;
            $message['_seq'] = $seq++;
            $all[] = $message;
        }
    }

    usort($all, function($a, $b) {
        $ta = (int)($a['_sort_ts'] ?? 0);
        $tb = (int)($b['_sort_ts'] ?? 0);
        if ($ta === $tb) return ((int)$a['_seq']) <=> ((int)$b['_seq']);
        if ($ta === 0) return 1;
        if ($tb === 0) return -1;
        return $ta <=> $tb;
    });

    $result = [];
    $seen = [];
    foreach ($all as $message) {
        unset($message['_sort_ts'], $message['_seq']);
        $dedupeKey = ($message['sender'] ?? '') . '|' . ($message['timestamp'] ?? '') . '|' . ($message['text'] ?? '');
        if (isset($seen[$dedupeKey])) continue;
        $seen[$dedupeKey] = true;
        $result[] = $message;
    }
    return $result;
}

function tt_support_ticket_access_allowed($ticket, $deviceId, $requesterEmail) {
    $deviceId = trim((string)$deviceId);
    $requesterEmail = tt_normalize_email($requesterEmail);
    $ticketDeviceId = trim((string)($ticket['device_id'] ?? ''));
    $ticketEmail = tt_normalize_email($ticket['user_email'] ?? '');

    if ($deviceId !== '' && $ticketDeviceId !== '' && hash_equals($ticketDeviceId, $deviceId)) {
        return true;
    }
    if (tt_support_valid_email($requesterEmail) && tt_support_valid_email($ticketEmail) && hash_equals($ticketEmail, $requesterEmail)) {
        return true;
    }
    return false;
}

function sendFirebaseNotification($title, $body) {
    $service_account_path = __DIR__ . '/firebase-service-account.json'; // File JSON của bạn
    
    $project_id = "tuner-tools"; // Project ID lấy từ file JSON
    
    $access_token = get_fcm_access_token($service_account_path);
    if (!$access_token) return false;

    // Gửi lệnh đánh thức tất cả máy cài App có subscribe 'all_users'
    $data = [
        "message" => [
            "topic" => "all_users",
            "notification" => [
                "title" => $title,
                "body" => $body
            ]
        ]
    ];

    $ch = curl_init("https://fcm.googleapis.com/v1/projects/$project_id/messages:send");
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $access_token
    ]);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($data));
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    $response = curl_exec($ch);
    curl_close($ch);
    
    return $response;
}

// =========================================================
// ĐĂNG THÔNG BÁO MỚI (DÀNH CHO ADMIN)
// =========================================================
if ($action === 'post_notification') {
    $message = isset($_POST['message']) ? $conn->real_escape_string($_POST['message']) : '';
    $send_push = isset($_POST['send_push']) && $_POST['send_push'] === 'true'; // Nhận cờ từ Android
    
    if (empty($message)) {
        echo json_encode(["error" => "Nội dung không được để trống"]);
        exit;
    }
    
    $sql = "INSERT INTO system_notifications (message, created_at) VALUES ('$message', NOW())";
    if ($conn->query($sql) === TRUE) {
        
        // Nếu admin tích chọn Gửi thông báo ngầm
        if ($send_push) {
            $pushTitle = "Tuner Tools - Thông báo mới";
            sendFirebaseNotification($pushTitle, $_POST['message']); // Dùng chuỗi gốc chưa escape
        }

        echo json_encode(["success" => true]);
    } else {
        echo json_encode(["error" => "Lỗi CSDL: " . $conn->error]);
    }
    exit;
}

// =========================================================
// LẤY THÔNG BÁO MỚI NHẤT CHO APP
// =========================================================
if ($action === 'check_notifications') {
    $sql = "SELECT id, message, created_at FROM system_notifications ORDER BY id DESC LIMIT 1";
    $result = $conn->query($sql);
    
    if ($result && $result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $row['id'] = (int)$row['id']; 
        echo json_encode(["has_notification" => true, "data" => $row]);
    } else {
        echo json_encode(["has_notification" => false]);
    }
    exit;
}


// =========================================================
// SONG SYNC V2 - KIỂM TRA CURSOR
// =========================================================
if ($action === 'check_update_v2') {
    try {
    tt_song_sync_require_schema($conn);

    $after = max(0, (int)($_GET['after'] ?? 0));
    $currentCursor = tt_song_sync_latest_cursor($conn);

    // Không COUNT toàn bộ phần đuôi change log ở mỗi lần app kiểm tra.
    // So sánh cursor là O(1) trên primary key và đủ cho UI biết có cập nhật hay không.
    tt_json_exit([
        'success' => true,
        'has_update' => $currentCursor > $after,
        'change_count' => max(0, $currentCursor - $after),
        'current_cursor' => $currentCursor
    ]);
    } catch (Throwable $e) {
        tt_song_sync_abort('SYNC_CHECK_FAILED', $e);
    }
}

// =========================================================
// SONG SYNC V2 - BOOTSTRAP MỘT LẦN CHO DATABASE CŨ/SEED
// Dùng cặp (updatedAt, id), có phân trang, không xóa database.
// snapshot_cursor được cố định từ trang đầu để không bỏ sót thay đổi
// phát sinh trong lúc thiết bị đang tải nhiều trang bootstrap.
// =========================================================
if ($action === 'sync_bootstrap_v2') {
    try {
    tt_song_sync_require_schema($conn);

    $lastUpdate = tt_song_sync_valid_timestamp($_GET['last_update'] ?? '2000-01-01 00:00:00');
    $lastId = trim((string)($_GET['last_id'] ?? ''));
    if (strlen($lastId) > 255) $lastId = substr($lastId, 0, 255);

    $limit = tt_song_sync_normalize_limit($_GET['limit'] ?? 200);
    $fetchLimit = $limit + 1;
    $snapshotProvided = array_key_exists('snapshot_cursor', $_GET);
    $snapshotCursor = $snapshotProvided
        ? max(0, (int)$_GET['snapshot_cursor'])
        : tt_song_sync_latest_cursor($conn);

    $stmt = $conn->prepare(
        "SELECT *
         FROM songs
         WHERE status = 1
           AND (updatedAt > ? OR (updatedAt = ? AND id > ?))
         ORDER BY updatedAt ASC, id ASC
         LIMIT ?"
    );
    if (!$stmt) {
        tt_json_exit(['success' => false, 'error' => 'Không chuẩn bị được truy vấn bootstrap.', 'code' => 'BOOTSTRAP_QUERY_FAILED'], 500);
    }

    $stmt->bind_param('sssi', $lastUpdate, $lastUpdate, $lastId, $fetchLimit);
    $stmt->execute();
    $result = $stmt->get_result();

    $items = [];
    $hasMore = false;
    $nextUpdate = $lastUpdate;
    $nextId = $lastId;
    $seen = 0;

    while ($result && ($song = $result->fetch_assoc())) {
        $seen++;
        if ($seen > $limit) {
            $hasMore = true;
            break;
        }
        $song = tt_song_sync_cast_song_row($song);
        $items[] = $song;
        $nextUpdate = (string)($song['updatedAt'] ?? $nextUpdate);
        $nextId = (string)($song['id'] ?? $nextId);
    }
    $stmt->close();

    tt_json_exit([
        'success' => true,
        'items' => $items,
        'next_updated_at' => $nextUpdate,
        'next_id' => $nextId,
        'has_more' => $hasMore,
        'snapshot_cursor' => $snapshotCursor,
        'page_size' => count($items)
    ]);
    } catch (Throwable $e) {
        tt_song_sync_abort('SYNC_BOOTSTRAP_FAILED', $e);
    }
}

// =========================================================
// SONG SYNC V2 - DELTA THEO change_id
// operation=upsert: trả dữ liệu bài hiện tại.
// operation=delete: app chỉ xóa đúng song_id ở SQLite.
// =========================================================
if ($action === 'sync_v2') {
    try {
    tt_song_sync_require_schema($conn);

    $after = max(0, (int)($_GET['after'] ?? 0));
    $limit = tt_song_sync_normalize_limit($_GET['limit'] ?? 200);
    $fetchLimit = $limit + 1;

    $stmt = $conn->prepare(
        "SELECT
            c.change_id AS sync_change_id,
            c.song_id AS sync_song_id,
            c.operation AS sync_operation,
            s.*
         FROM song_sync_changes c
         LEFT JOIN songs s ON s.id = c.song_id
         WHERE c.change_id > ?
         ORDER BY c.change_id ASC
         LIMIT ?"
    );
    if (!$stmt) {
        tt_json_exit(['success' => false, 'error' => 'Không chuẩn bị được truy vấn delta.', 'code' => 'SYNC_QUERY_FAILED'], 500);
    }

    $stmt->bind_param('ii', $after, $fetchLimit);
    $stmt->execute();
    $result = $stmt->get_result();

    // Trong cùng một trang có thể một bài đã bị sửa nhiều lần. Chỉ gửi trạng thái
    // cuối cùng của mỗi song_id để không tải lặp lại toàn bộ lyrics/hợp âm.
    $changeMap = [];
    $hasMore = false;
    $nextCursor = $after;
    $seen = 0;

    while ($result && ($row = $result->fetch_assoc())) {
        $seen++;
        if ($seen > $limit) {
            $hasMore = true;
            break;
        }

        $changeId = (int)($row['sync_change_id'] ?? 0);
        $songId = (string)($row['sync_song_id'] ?? '');
        $requestedOperation = strtolower((string)($row['sync_operation'] ?? 'upsert'));
        $isVisibleSong = !empty($row['id']) && (int)($row['status'] ?? 0) === 1;
        $effectiveOperation = ($requestedOperation === 'delete' || !$isVisibleSong) ? 'delete' : 'upsert';

        $change = [
            'change_id' => $changeId,
            'operation' => $effectiveOperation,
            'song_id' => $songId
        ];

        if ($effectiveOperation === 'upsert') {
            unset($row['sync_change_id'], $row['sync_song_id'], $row['sync_operation']);
            $change['song'] = tt_song_sync_cast_song_row($row);
        }

        $changeMap['song:' . $songId] = $change;
        $nextCursor = $changeId;
    }
    $stmt->close();

    $changes = array_values($changeMap);
    usort($changes, function ($a, $b) {
        return ((int)($a['change_id'] ?? 0)) <=> ((int)($b['change_id'] ?? 0));
    });

    tt_json_exit([
        'success' => true,
        'changes' => $changes,
        'next_cursor' => $nextCursor,
        'has_more' => $hasMore,
        'current_cursor' => tt_song_sync_latest_cursor($conn),
        'page_size' => count($changes)
    ]);
    } catch (Throwable $e) {
        tt_song_sync_abort('SYNC_DELTA_FAILED', $e);
    }
}

if ($action === 'check_update') {
    $last_update = isset($_GET['last_update']) ? $_GET['last_update'] : '2000-01-01 00:00:00';
    $last_update = $conn->real_escape_string($last_update);

    $sql = "SELECT COUNT(*) as total FROM songs WHERE updatedAt > '$last_update' AND status = 1";
    $result = $conn->query($sql);

    $count = 0;
    if ($result && $result->num_rows > 0) {
        $row = $result->fetch_assoc();
        $count = (int)$row['total'];
    }

    echo json_encode([
        'has_update' => $count > 0, 
        'new_songs_count' => $count
    ]);
    exit;
}

// --- 2.7. TỰ ĐỘNG HỌC MÃ ZING ID TỪ THIẾT BỊ NGƯỜI DÙNG BẮN LÊN ---
if ($action === 'update_zing_id') {
    $id = isset($_POST['id']) ? $_POST['id'] : '';
    $zing_id = isset($_POST['zing_id']) ? $_POST['zing_id'] : '';
    
    if (!empty($id) && !empty($zing_id)) {
        // Chỉ đổi updatedAt khi Zing ID thật sự thay đổi. Trigger sync sẽ tự ghi 1 delta upsert.
        $stmt = $conn->prepare(
            "UPDATE songs
             SET id_zing_mp3 = ?, updatedAt = NOW()
             WHERE id = ? AND NOT (COALESCE(id_zing_mp3, '') <=> ?)"
        );
        $stmt->bind_param("sss", $zing_id, $id, $zing_id);
        
        if ($stmt->execute()) {
            echo json_encode(["success" => true, "changed" => $stmt->affected_rows > 0]);
        } else {
            echo json_encode(["error" => "Database update failed"]);
        }
        $stmt->close();
    } else {
        echo json_encode(["error" => "Missing parameters"]);
    }
    exit;
}

// --- 3. Xử lý đồng bộ (Sync) bằng kỹ thuật Streaming ---
if ($action === 'sync') {
    $last_update = isset($_GET['last_update']) ? $_GET['last_update'] : '2000-01-01 00:00:00';
    $last_update = $conn->real_escape_string($last_update);

    $sql = "SELECT * FROM songs WHERE updatedAt > '$last_update' AND status = 1 ORDER BY updatedAt ASC, id ASC";
    
    $result = $conn->query($sql, MYSQLI_USE_RESULT);

    if ($result) {
        echo "["; 
        $is_first = true;
        
        while ($song = $result->fetch_assoc()) {
            if (!$is_first) {
                echo ",";
            }
            $is_first = false;

            $song['favoriteCount'] = (int)$song['favoriteCount'];
            $song['viewCount']     = (int)$song['viewCount'];
            $song['commentCount']  = (int)$song['commentCount'];
            $song['ratingStars']   = (float)$song['ratingStars'];
            $song['ratingCount']   = (int)$song['ratingCount'];
            $song['bpm']           = (int)$song['bpm'];
            $song['image_count']   = (int)$song['image_count'];
            $song['id_zing_mp3']   = isset($song['id_zing_mp3']) ? $song['id_zing_mp3'] : '';

            echo json_encode($song);
            unset($song); 
        }
        echo "]"; 
        exit;
    } else {
        echo json_encode(["error" => "Lỗi truy vấn SQL"]);
        exit;
    }
}

// --- 4. Xử lý Thả Tim & Ghi Sổ Nhớ (Toggle Like) ---
if ($action === 'toggle_like') {
    $id = isset($_GET['id']) ? $conn->real_escape_string($_GET['id']) : '';
    $email = isset($_GET['email']) ? $conn->real_escape_string($_GET['email']) : '';
    $is_liking = isset($_GET['is_liking']) ? $_GET['is_liking'] : 'false';

    if (empty($id) || empty($email)) {
        echo json_encode(["error" => "Thiếu dữ liệu"]);
        exit;
    }

    if ($is_liking === 'true') {
        $conn->query("INSERT IGNORE INTO device_likes (user_email, song_id) VALUES ('$email', '$id')");
        $conn->query("UPDATE songs SET favoriteCount = favoriteCount + 1 WHERE id = '$id'");
    } else {
        $conn->query("DELETE FROM device_likes WHERE user_email = '$email' AND song_id = '$id'");
        $conn->query("UPDATE songs SET favoriteCount = GREATEST(favoriteCount - 1, 0) WHERE id = '$id'");
    }
    
    echo json_encode(["success" => true]);
    exit;
}

// --- 5.1. Lấy Danh Sách ID Yêu Thích (Dùng cho logic load SQLite local) ---
if ($action === 'get_favorite_ids') {
    $email = isset($_GET['email']) ? $conn->real_escape_string($_GET['email']) : '';
    
    $sql = "SELECT song_id FROM device_likes WHERE user_email = '$email'";
    $result = $conn->query($sql);
    $ids = [];
    
    if ($result && $result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $ids[] = $row['song_id'];
        }
    }
    
    echo json_encode($ids);
    exit;
}

// --- 5. Lấy Danh Sách Yêu Thích Của 1 Người Dùng (Favorites) ---
if ($action === 'favorites') {
    $email = isset($_GET['email']) ? $conn->real_escape_string($_GET['email']) : '';
    
    $sql = "SELECT 
                s.id AS songId, 
                s.title, 
                s.singerNames AS singer, 
                s.authorName AS composer, 
                s.youtubeUrl AS videoUrl, 
                s.chordList, 
                s.slug, 
                s.image_count AS imageCount,
                s.id_zing_mp3 AS zingMp3Id
            FROM songs s 
            INNER JOIN device_likes d ON s.id = d.song_id 
            WHERE d.user_email = '$email'";
            
    $result = $conn->query($sql);
    $songs = [];
    
    if ($result && $result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $chordStr = $row['chordList'];
            $row['chords'] = !empty($chordStr) ? explode(',', $chordStr) : [];
            unset($row['chordList']);
            
            $row['imageCount'] = (int)$row['imageCount']; 
            $row['zingMp3Id'] = isset($row['zingMp3Id']) ? $row['zingMp3Id'] : '';
            
            $songs[] = $row;
        }
    }
    
    echo json_encode($songs);
    exit;
}

// =========================================================
// PHÂN HỆ QUẢN TRỊ VIÊN (ADMIN & ROLES)
// =========================================================

if ($action === 'check_role') {
    $email = isset($_GET['email']) ? $conn->real_escape_string($_GET['email']) : '';
    if (empty($email)) {
        echo json_encode(["role" => "user"]);
        exit;
    }

    $sql = "SELECT role FROM songs_app_admins WHERE email = '$email' LIMIT 1";
    $result = $conn->query($sql);
    
    if ($result && $result->num_rows > 0) {
        $row = $result->fetch_assoc();
        echo json_encode(["role" => $row['role']]);
    } else {
        echo json_encode(["role" => "user"]);
    }
    exit;
}

if ($action === 'get_admins') {
    $sql = "SELECT id, email, role, created_at FROM songs_app_admins ORDER BY id ASC";
    $result = $conn->query($sql);
    $admins = [];
    
    if ($result && $result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $admins[] = $row;
        }
    }
    echo json_encode($admins);
    exit;
}

if ($action === 'add_admin') {
    $email = isset($_POST['email']) ? $conn->real_escape_string($_POST['email']) : '';
    $role = isset($_POST['role']) ? $conn->real_escape_string($_POST['role']) : 'mod';

    if (empty($email)) {
        echo json_encode(["error" => "Thiếu email"]);
        exit;
    }

    $sql = "INSERT IGNORE INTO songs_app_admins (email, role) VALUES ('$email', '$role')";
    if ($conn->query($sql) === TRUE) {
        echo json_encode(["success" => true]);
    } else {
        echo json_encode(["error" => "Lỗi MySQL"]);
    }
    exit;
}

if ($action === 'update_admin') {
    $id = isset($_POST['id']) ? $conn->real_escape_string($_POST['id']) : '';
    $role = isset($_POST['role']) ? $conn->real_escape_string($_POST['role']) : '';

    if (empty($id) || empty($role)) {
        echo json_encode(["error" => "Thiếu dữ liệu"]);
        exit;
    }

    if ($role === 'delete') {
        $sql = "DELETE FROM songs_app_admins WHERE id = '$id'";
    } else {
        $sql = "UPDATE songs_app_admins SET role = '$role' WHERE id = '$id'";
    }

    if ($conn->query($sql) === TRUE) {
        echo json_encode(["success" => true]);
    } else {
        echo json_encode(["error" => "Lỗi MySQL"]);
    }
    exit;
}

// --- SỬA BÀI HÁT (CÓ HỖ TRỢ XÓA/THÊM ẢNH GITHUB) ---
if ($action === 'update') {
    $id = isset($_POST['id']) ? $_POST['id'] : '';
    $title = isset($_POST['title']) ? $_POST['title'] : '';
    $singer = isset($_POST['singer']) ? $_POST['singer'] : '';
    $composer = isset($_POST['composer']) ? $_POST['composer'] : '';
    $baseTone = isset($_POST['baseTone']) ? $_POST['baseTone'] : '';
    $rhythm = isset($_POST['rhythm']) ? $_POST['rhythm'] : '';
    $bpm = isset($_POST['bpm']) ? (int)$_POST['bpm'] : 0; 
    $videoUrl = isset($_POST['videoUrl']) ? $_POST['videoUrl'] : '';
    $raw_lyrics = isset($_POST['lyrics']) ? $_POST['lyrics'] : '';

    if (empty($id) || empty($title)) {
        echo json_encode(["error" => "Thiếu ID hoặc Tiêu đề"]);
        exit;
    }

    $chordList = '';
    if (preg_match_all('/\[(.*?)\]/', $raw_lyrics, $matches)) {
        $chords = array_unique($matches[1]); 
        $chordList = implode(',', $chords);
    }

    $slugStmt = $conn->prepare("SELECT slug, IFNULL(image_count, 0) as image_count FROM songs WHERE id = ?");
    $slugStmt->bind_param("s", $id);
    $slugStmt->execute();
    $slugResult = $slugStmt->get_result();
    $currentSlug = "";
    $oldImageCount = 0;
    if ($row = $slugResult->fetch_assoc()) {
        $currentSlug = trim((string)($row['slug'] ?? ''));
        $oldImageCount = (int)($row['image_count'] ?? 0);
    }
    $slugStmt->close();

    $keptIndices = [];
    if (isset($_POST['kept_old_indices']) && $_POST['kept_old_indices'] !== '') {
        $keptIndices = array_filter(explode(',', $_POST['kept_old_indices']));
    }
    
    $hasNewImages = isset($_FILES['sheet_images']) && !empty($_FILES['sheet_images']['name'][0]);
    $hasDeletedImages = count($keptIndices) !== $oldImageCount;
    $imagesChanged = $hasNewImages || $hasDeletedImages;

    if ($imagesChanged) {
        $ghUsername = 'ducphi0491';
        $ghRepo     = 'sheet_images';
        $ghBranch   = 'main';
        $ghToken    = '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';

        $newSlug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $title))) . '-' . substr(md5(time()), 0, 4);
        $newSlug = trim(preg_replace('/-+/', "-", $newSlug), '-');
        
        $newImageCount = 0;

        $tempFilesDir = __DIR__ . '/tuner_images_' . uniqid();
        if (!is_dir($tempFilesDir)) {
            mkdir($tempFilesDir, 0777, true);
        }

        $extensions = ['jpg', 'png', 'jpeg', 'gif'];
        foreach ($keptIndices as $oldIdx) {
            $fileContent = false;
            $extUsed = 'jpg';
            foreach ($extensions as $testExt) {
                $fileName = "{$currentSlug}-{$oldIdx}.{$testExt}";
                $apiUrl = "https://api.github.com/repos/{$ghUsername}/{$ghRepo}/contents/" . urlencode($fileName);
                $chGet = curl_init($apiUrl);
                curl_setopt($chGet, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($chGet, CURLOPT_HTTPHEADER, ["Authorization: token {$ghToken}", "User-Agent: TunerApp"]);
                curl_setopt($chGet, CURLOPT_SSL_VERIFYPEER, false);
                $getResponse = curl_exec($chGet);
                $httpCode = curl_getinfo($chGet, CURLINFO_HTTP_CODE);
                curl_close($chGet);
                if ($httpCode == 200) {
                    $fileData = json_decode($getResponse, true);
                    if (isset($fileData['content'])) {
                        $fileContent = base64_decode(str_replace(["\n", "\r"], "", $fileData['content']));
                        $extUsed = $testExt;
                        break;
                    }
                }
            }
            if ($fileContent) {
                $newImageCount++;
                file_put_contents("{$tempFilesDir}/{$newImageCount}.{$extUsed}", $fileContent);
            }
        }

        if ($hasNewImages) {
            $files = $_FILES['sheet_images'];
            $totalFiles = is_array($files['name']) ? count($files['name']) : 0;
            for ($i = 0; $i < $totalFiles; $i++) {
                if ($files['error'][$i] === UPLOAD_ERR_OK) {
                    $newImageCount++;
                    $ext = strtolower(pathinfo($files['name'][$i], PATHINFO_EXTENSION));
                    if (!$ext) $ext = 'jpg';
                    move_uploaded_file($files['tmp_name'][$i], "{$tempFilesDir}/{$newImageCount}.{$ext}");
                }
            }
        }

        for ($i = 1; $i <= $newImageCount; $i++) {
            $matchedFile = glob("{$tempFilesDir}/{$i}.*");
            if (!empty($matchedFile)) {
                $localFilePath = $matchedFile[0];
                $ext = pathinfo($localFilePath, PATHINFO_EXTENSION);

                $fileName = "{$newSlug}-{$i}.{$ext}";
                $safeFileName = urlencode($fileName);
                $apiUrl = "https://api.github.com/repos/{$ghUsername}/{$ghRepo}/contents/{$safeFileName}";

                $base64Content = base64_encode(file_get_contents($localFilePath));
                $bodyArray = ['message' => "Update song sheet: " . $fileName, 'content' => $base64Content, 'branch'  => $ghBranch];

                $ch = curl_init($apiUrl);
                curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "PUT");
                curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($bodyArray));
                curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
                curl_setopt($ch, CURLOPT_HTTPHEADER, ["Authorization: token {$ghToken}", "User-Agent: TunerApp", "Content-Type: application/json"]);
                curl_exec($ch);
                curl_close($ch);

                unlink($localFilePath);
            }
        }
        rmdir($tempFilesDir); 

        for ($i = 1; $i <= $oldImageCount; $i++) {
            foreach ($extensions as $testExt) {
                $oldFileName = "{$currentSlug}-{$i}.{$testExt}";
                $apiUrl = "https://api.github.com/repos/{$ghUsername}/{$ghRepo}/contents/" . urlencode($oldFileName);
                $chGet = curl_init($apiUrl);
                curl_setopt($chGet, CURLOPT_RETURNTRANSFER, true);
                curl_setopt($chGet, CURLOPT_HTTPHEADER, ["Authorization: token {$ghToken}", "User-Agent: TunerApp"]);
                curl_setopt($chGet, CURLOPT_SSL_VERIFYPEER, false);
                $getResponse = curl_exec($chGet);
                $httpCode = curl_getinfo($chGet, CURLINFO_HTTP_CODE);
                curl_close($chGet);
                if ($httpCode == 200) {
                    $fileData = json_decode($getResponse, true);
                    if (isset($fileData['sha'])) {
                        $chDel = curl_init($apiUrl);
                        curl_setopt($chDel, CURLOPT_RETURNTRANSFER, true);
                        curl_setopt($chDel, CURLOPT_CUSTOMREQUEST, "DELETE");
                        curl_setopt($chDel, CURLOPT_POSTFIELDS, json_encode(['message' => "Delete old image: " . $oldFileName, 'sha' => $fileData['sha'], 'branch' => $ghBranch]));
                        curl_setopt($chDel, CURLOPT_HTTPHEADER, ["Authorization: token {$ghToken}", "User-Agent: TunerApp", "Content-Type: application/json"]);
                        curl_setopt($chDel, CURLOPT_SSL_VERIFYPEER, false);
                        curl_exec($chDel);
                        curl_close($chDel);
                        break;
                    }
                }
            }
        }

        $sql = "UPDATE songs SET title=?, singerNames=?, authorName=?, originalKey=?, rhythm=?, bpm=?, youtubeUrl=?, lyrics=?, chordList=?, slug=?, image_count=?, updatedAt=NOW() WHERE id=?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("sssssissssis", $title, $singer, $composer, $baseTone, $rhythm, $bpm, $videoUrl, $raw_lyrics, $chordList, $newSlug, $newImageCount, $id);

    } else {
        $sql = "UPDATE songs SET title=?, singerNames=?, authorName=?, originalKey=?, rhythm=?, bpm=?, youtubeUrl=?, lyrics=?, chordList=?, updatedAt=NOW() WHERE id=?";
        $stmt = $conn->prepare($sql);
        $stmt->bind_param("sssssissss", $title, $singer, $composer, $baseTone, $rhythm, $bpm, $videoUrl, $raw_lyrics, $chordList, $id);
    }

    if ($stmt->execute()) {
        echo json_encode(["success" => true, "message" => "Cập nhật thành công"]);
    } else {
        echo json_encode(["error" => "Lỗi thực thi MySQL: " . $stmt->error]);
    }
    $stmt->close();
    exit;
}

// =========================================================
// 6. LẤY CHI TIẾT 1 BÀI HÁT (DETAIL SONG)
// =========================================================
if ($action === 'detail') {
    $id = isset($_GET['id']) ? $conn->real_escape_string($_GET['id']) : '';
    $email = isset($_GET['email']) ? $conn->real_escape_string($_GET['email']) : '';

    if (empty($id)) {
        echo json_encode(["error" => "Thiếu ID"]);
        exit;
    }

    $conn->query("UPDATE songs SET viewCount = viewCount + 1 WHERE id = '$id'");

    $sql = "SELECT 
                id AS songId, 
                title, 
                singerNames AS singer, 
                authorName AS composer, 
                originalKey AS baseTone, 
                rhythm, 
                bpm, 
                youtubeUrl AS videoUrl, 
                lyrics, 
                favoriteCount AS likes, 
                slug, 
                image_count AS imageCount,
                id_zing_mp3 AS zingMp3Id
            FROM songs 
            WHERE id = '$id' LIMIT 1";
            
    $result = $conn->query($sql);

    if ($result && $result->num_rows > 0) {
        $song = $result->fetch_assoc();
        
        $song['isLikedByDevice'] = false;
        if (!empty($email)) {
            $checkLike = $conn->query("SELECT 1 FROM device_likes WHERE user_email = '$email' AND song_id = '$id'");
            if ($checkLike && $checkLike->num_rows > 0) {
                $song['isLikedByDevice'] = true;
            }
        }
        
        $song['likes'] = (int)$song['likes'];
        $song['imageCount'] = (int)$song['imageCount'];
        $song['bpm'] = (string)$song['bpm']; 
        $song['zingMp3Id'] = isset($song['zingMp3Id']) ? $song['zingMp3Id'] : '';
        
        echo json_encode($song);
    } else {
        echo json_encode(["error" => "Không tìm thấy bài hát"]);
    }
    exit;
}

// =========================================================
// ADMIN CHỦ ĐỘNG LIÊN HỆ NGƯỜI DÙNG (ONLINE HOẶC OFFLINE)
// =========================================================
// Mỗi user chỉ dùng 1 thread support. Nếu đã có ticket trước đó (cùng device_id
// hoặc cùng email đăng nhập), lịch sử cũ được gộp vào thread chính thay vì INSERT thread mới.
if ($action === 'admin_start_contact') {
    $adminEmail = tt_normalize_email($_POST['admin_email'] ?? '');
    $adminName = trim((string)($_POST['admin_name'] ?? ''));
    $targetDeviceId = trim((string)($_POST['target_device_id'] ?? ''));
    $targetFirebaseUid = trim((string)($_POST['target_firebase_uid'] ?? ''));
    $targetEmail = tt_normalize_email($_POST['target_email'] ?? 'guest');
    $targetName = trim((string)($_POST['target_name'] ?? ''));
    $targetDeviceName = trim((string)($_POST['target_device_name'] ?? ''));
    $subject = trim((string)($_POST['subject'] ?? 'Admin chủ động liên hệ'));
    $message = trim((string)($_POST['message'] ?? ''));

    $adminAccount = tt_get_admin_account($conn, $adminEmail);
    if (!$adminAccount) {
        tt_json_exit([
            'success' => false,
            'error' => 'Tài khoản Google đang đăng nhập không có quyền Admin.',
            'code' => 'ADMIN_PERMISSION_DENIED'
        ], 403);
    }

    if (!tt_support_merge_schema_ready($conn)) {
        tt_json_exit([
            'success' => false,
            'error' => 'Bảng support_tickets chưa có cột conversation_key, user_email, merged_into_id. Hãy chạy file ALTER_support_tickets_single_chat.sql trước.',
            'code' => 'SUPPORT_SCHEMA_NOT_UPDATED'
        ], 409);
    }

    if ($targetDeviceId === '' || !preg_match('/^[A-Za-z0-9_-]{3,200}$/', $targetDeviceId)) {
        tt_json_exit(['success' => false, 'error' => 'Không xác định được thiết bị người dùng.', 'code' => 'INVALID_TARGET_DEVICE'], 422);
    }
    if ($message === '') {
        tt_json_exit(['success' => false, 'error' => 'Nội dung liên hệ không được để trống.', 'code' => 'EMPTY_MESSAGE'], 422);
    }

    $messageLength = function_exists('mb_strlen') ? mb_strlen($message, 'UTF-8') : strlen($message);
    if ($messageLength > 3000) {
        tt_json_exit(['success' => false, 'error' => 'Nội dung liên hệ tối đa 3000 ký tự.', 'code' => 'MESSAGE_TOO_LONG'], 422);
    }
    $subjectLength = function_exists('mb_strlen') ? mb_strlen($subject, 'UTF-8') : strlen($subject);
    if ($subjectLength > 255) {
        $subject = function_exists('mb_substr') ? mb_substr($subject, 0, 255, 'UTF-8') : substr($subject, 0, 255);
    }

    // Bổ sung dữ liệu định danh lâu dài từ RTDB user_directory nếu có.
    $directoryResult = tt_realtime_database_request(
        'GET',
        'user_directory/' . $targetDeviceId,
        null,
        $firebase_service_account_path,
        $firebase_project_id
    );
    $directory = ($directoryResult['success'] && is_array($directoryResult['data'])) ? $directoryResult['data'] : [];

    if ($targetFirebaseUid === '') $targetFirebaseUid = trim((string)($directory['firebase_uid'] ?? ''));
    if ($targetFirebaseUid !== '' && !preg_match('/^[A-Za-z0-9_-]{6,160}$/', $targetFirebaseUid)) $targetFirebaseUid = '';

    if (!tt_support_valid_email($targetEmail)) {
        $directoryEmail = tt_normalize_email($directory['email'] ?? 'guest');
        if (tt_support_valid_email($directoryEmail)) $targetEmail = $directoryEmail;
    }
    if ($targetName === '') $targetName = trim((string)($directory['display_name'] ?? ''));
    if ($targetDeviceName === '') $targetDeviceName = trim((string)($directory['device_name'] ?? ''));
    if ($targetName === '') {
        $targetName = tt_support_valid_email($targetEmail) ? tt_display_name_from_email($targetEmail) : 'Người dùng TunerTools';
    }
    if ($adminName === '') $adminName = tt_display_name_from_email($adminAccount['email']);
    if ($subject === '') $subject = 'Admin chủ động liên hệ';

    $conversationKey = tt_support_conversation_key($targetEmail, $targetDeviceId);
    $emailForDb = tt_support_valid_email($targetEmail) ? $targetEmail : null;
    $createdAtText = date('Y-m-d H:i:s');
    $newAdminMessage = [
        'sender' => 'admin',
        'text' => $message,
        'timestamp' => $createdAtText,
        'sender_email' => $adminAccount['email'],
        'sender_name' => $adminName
    ];

    $conn->begin_transaction();
    $requestPath = '';
    $ticketEventPath = '';
    $mergedIds = [];

    try {
        // Tìm TẤT CẢ support thread đang hoạt động của cùng user theo conversation_key,
        // device_id hoặc email. FOR UPDATE khóa các hàng để tránh tạo thread trùng trong cùng transaction.
        if ($emailForDb !== null) {
            $findStmt = $conn->prepare(
                "SELECT * FROM support_tickets
                 WHERE type = 'support' AND merged_into_id IS NULL
                   AND (conversation_key = ? OR device_id = ? OR LOWER(COALESCE(user_email, '')) = ?)
                 ORDER BY created_at ASC, id ASC
                 FOR UPDATE"
            );
            if (!$findStmt) throw new Exception('Không chuẩn bị được truy vấn tìm cuộc trò chuyện cũ.');
            $findStmt->bind_param('sss', $conversationKey, $targetDeviceId, $emailForDb);
        } else {
            $findStmt = $conn->prepare(
                "SELECT * FROM support_tickets
                 WHERE type = 'support' AND merged_into_id IS NULL
                   AND (conversation_key = ? OR device_id = ?)
                 ORDER BY created_at ASC, id ASC
                 FOR UPDATE"
            );
            if (!$findStmt) throw new Exception('Không chuẩn bị được truy vấn tìm cuộc trò chuyện cũ.');
            $findStmt->bind_param('ss', $conversationKey, $targetDeviceId);
        }
        $findStmt->execute();
        $findResult = $findStmt->get_result();
        $existingRows = [];
        while ($findResult && ($row = $findResult->fetch_assoc())) $existingRows[] = $row;
        $findStmt->close();

        $type = 'support';
        $status = 1;   // Admin vừa gửi -> user chưa đọc.
        $userRead = 0;
        $reused = !empty($existingRows);

        if ($reused) {
            // Ưu tiên row đã mang đúng conversation_key, nếu chưa có thì giữ row cũ nhất làm canonical.
            $canonical = $existingRows[0];
            foreach ($existingRows as $row) {
                if (($row['conversation_key'] ?? '') === $conversationKey) {
                    $canonical = $row;
                    break;
                }
            }
            $ticketId = (string)$canonical['id'];

            $history = tt_support_merge_histories($existingRows);
            $history[] = $newAdminMessage;
            $historyJson = json_encode($history, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);

            $initialMessage = trim((string)($canonical['message'] ?? ''));
            if ($initialMessage === '') $initialMessage = $message;

            $updateStmt = $conn->prepare(
                "UPDATE support_tickets
                 SET device_id = ?, conversation_key = ?, user_email = ?, subject = ?, message = ?,
                     chat_history = ?, status = ?, user_read = ?, merged_into_id = NULL
                 WHERE id = ?"
            );
            $updateStmt->bind_param(
                'ssssssiis',
                $targetDeviceId,
                $conversationKey,
                $emailForDb,
                $subject,
                $initialMessage,
                $historyJson,
                $status,
                $userRead,
                $ticketId
            );
            if (!$updateStmt->execute()) {
                $err = $updateStmt->error;
                $updateStmt->close();
                throw new Exception('Không cập nhật được cuộc trò chuyện hiện tại: ' . $err);
            }
            $updateStmt->close();

            // Không xóa row cũ: đánh dấu merged_into_id để mọi request/popup cũ vẫn redirect được.
            foreach ($existingRows as $row) {
                $oldId = (string)$row['id'];
                if ($oldId === $ticketId) continue;
                $mergedIds[] = $oldId;
                $mergeStmt = $conn->prepare(
                    "UPDATE support_tickets
                     SET conversation_key = NULL, merged_into_id = ?
                     WHERE id = ?"
                );
                $mergeStmt->bind_param('ss', $ticketId, $oldId);
                if (!$mergeStmt->execute()) {
                    $err = $mergeStmt->error;
                    $mergeStmt->close();
                    throw new Exception('Không gộp được cuộc trò chuyện cũ #' . $oldId . ': ' . $err);
                }
                $mergeStmt->close();
            }
        } else {
            $history = [$newAdminMessage];
            $historyJson = json_encode($history, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
            $insertStmt = $conn->prepare(
                "INSERT INTO support_tickets
                    (type, device_id, conversation_key, user_email, subject, message, chat_history, status, user_read, merged_into_id, created_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NOW())"
            );
            if (!$insertStmt) throw new Exception('Không chuẩn bị được câu lệnh tạo cuộc trò chuyện.');
            $insertStmt->bind_param(
                'sssssssii',
                $type,
                $targetDeviceId,
                $conversationKey,
                $emailForDb,
                $subject,
                $message,
                $historyJson,
                $status,
                $userRead
            );
            if (!$insertStmt->execute()) {
                $err = $insertStmt->error;
                $insertStmt->close();
                throw new Exception('Không tạo được cuộc trò chuyện: ' . $err);
            }
            $insertStmt->close();
            $ticketId = (string)$conn->insert_id;
            if ($ticketId === '' || $ticketId === '0') throw new Exception('Không lấy được mã cuộc trò chuyện.');
        }

        // 1 ticket = 1 contact request cố định trên mỗi device. Gửi lần sau chỉ cập nhật node cũ.
        $requestId = 'ticket_' . $ticketId;
        $requestPath = 'contact_requests/' . $targetDeviceId . '/' . $requestId;
        $nowMs = (int)round(microtime(true) * 1000);
        $contactPayload = [
            'request_id' => $requestId,
            'ticket_id' => $ticketId,
            'ticket_type' => $type,
            'contact_origin' => 'admin',
            'admin_email' => $adminAccount['email'],
            'admin_name' => $adminName,
            'admin_role' => $adminAccount['role'],
            'target_firebase_uid' => $targetFirebaseUid,
            'target_device_id' => $targetDeviceId,
            'target_email' => $targetEmail,
            'target_name' => $targetName,
            'target_device_name' => $targetDeviceName,
            'title' => $subject,
            'message' => $message,
            'status' => 'pending',
            'created_at' => $nowMs,
            'updated_at' => $nowMs,
            'delivery_mode' => 'persistent',
            'popup_version' => 3
        ];

        $writeResult = tt_realtime_database_request(
            'PUT', $requestPath, $contactPayload,
            $firebase_service_account_path, $firebase_project_id
        );
        if (!$writeResult['success']) {
            throw new Exception(
                'Không lưu được tin nhắn vào Realtime Database. HTTP ' .
                $writeResult['http_code'] . ' ' . $writeResult['error']
            );
        }

        // Dọn các request ngẫu nhiên cũ của chính các ticket vừa gộp trên device hiện tại.
        $requestsResult = tt_realtime_database_request(
            'GET', 'contact_requests/' . $targetDeviceId, null,
            $firebase_service_account_path, $firebase_project_id
        );
        if ($requestsResult['success'] && is_array($requestsResult['data'])) {
            $allMergedTicketIds = array_merge([$ticketId], $mergedIds);
            foreach ($requestsResult['data'] as $oldRequestId => $oldPayload) {
                if ($oldRequestId === $requestId || !is_array($oldPayload)) continue;
                $oldTicketId = (string)($oldPayload['ticket_id'] ?? '');
                if (in_array($oldTicketId, $allMergedTicketIds, true)) {
                    tt_realtime_database_request(
                        'DELETE', 'contact_requests/' . $targetDeviceId . '/' . $oldRequestId, null,
                        $firebase_service_account_path, $firebase_project_id
                    );
                }
            }
        }

        $ticketEventPath = 'support_ticket_events/' . $targetDeviceId . '/' . $ticketId;
        $eventWriteResult = tt_publish_support_ticket_event(
            $targetDeviceId, $ticketId, 'admin', $status, count($history),
            $firebase_service_account_path, $firebase_project_id
        );
        if (!$eventWriteResult['success']) {
            error_log('[TunerTools] support_ticket_events write failed for ticket ' . $ticketId);
        }

        if (!$conn->commit()) {
            tt_realtime_database_request('DELETE', $requestPath, null, $firebase_service_account_path, $firebase_project_id);
            throw new Exception('Không thể hoàn tất transaction MySQL.');
        }

        tt_json_exit([
            'success' => true,
            'message' => $reused
                ? 'Đã gửi vào cuộc trò chuyện hiện có. Lịch sử cũ được giữ nguyên.'
                : 'Đã tạo cuộc trò chuyện và gửi tin nhắn.',
            'ticket_id' => $ticketId,
            'request_id' => $requestId,
            'conversation_key' => $conversationKey,
            'reused_conversation' => $reused,
            'merged_old_threads' => count($mergedIds),
            'target_device_id' => $targetDeviceId,
            'target_email' => $targetEmail,
            'persistent' => true,
            'realtime_event' => (bool)$eventWriteResult['success']
        ]);
    } catch (Throwable $e) {
        $conn->rollback();
        if ($requestPath !== '') {
            tt_realtime_database_request('DELETE', $requestPath, null, $firebase_service_account_path, $firebase_project_id);
        }
        tt_json_exit(['success' => false, 'error' => $e->getMessage(), 'code' => 'ADMIN_CONTACT_FAILED'], 500);
    }
}

// Lấy đúng một cuộc trò chuyện; nếu ID cũ đã được gộp thì tự redirect về ticket chính.
if ($action === 'get_ticket_detail') {
    $ticketId = trim((string)($_GET['id'] ?? ''));
    $deviceId = trim((string)($_GET['device_id'] ?? ''));
    $requesterEmail = tt_normalize_email($_GET['requester_email'] ?? '');
    $adminAccount = tt_get_admin_account($conn, $requesterEmail);

    if ($ticketId === '' || !ctype_digit($ticketId)) {
        tt_json_exit(['success' => false, 'error' => 'Mã cuộc trò chuyện không hợp lệ.'], 422);
    }

    $resolvedTicketId = tt_support_resolve_ticket_id($conn, $ticketId);
    $stmt = $conn->prepare("SELECT * FROM support_tickets WHERE id = ? LIMIT 1");
    $stmt->bind_param('s', $resolvedTicketId);
    $stmt->execute();
    $result = $stmt->get_result();
    $ticket = $result ? $result->fetch_assoc() : null;
    $stmt->close();

    if (!$ticket) tt_json_exit(['success' => false, 'error' => 'Không tìm thấy cuộc trò chuyện.'], 404);
    if (!$adminAccount && !tt_support_ticket_access_allowed($ticket, $deviceId, $requesterEmail)) {
        tt_json_exit(['success' => false, 'error' => 'Bạn không có quyền xem cuộc trò chuyện này.'], 403);
    }

    // Day la diem xac nhan "Admin da xem" dang tin cay nhat:
    // get_ticket_detail chi duoc goi khi cua so chat da mo. Neu nguoi goi la Admin
    // va tin cuoi la cua User, danh dau admin_read ngay tren server truoc khi tra ve.
    // Nho vay khong con phu thuoc vao mot request mark_admin_read rieng tu Compose.
    $lastSender = tt_support_last_sender($ticket);
    $adminReadUpdated = false;
    $adminReadEvent = ['success' => false, 'http_code' => 0];

    if (
        $adminAccount &&
        $lastSender !== 'admin' &&
        tt_support_read_receipt_schema_ready($conn)
    ) {
        $currentAdminRead = (int)($ticket['admin_read'] ?? 0);
        if ($currentAdminRead !== 1) {
            $readStmt = $conn->prepare(
                "UPDATE support_tickets SET admin_read=1 WHERE id=? AND COALESCE(admin_read,0)<>1"
            );
            if ($readStmt) {
                $readStmt->bind_param('s', $resolvedTicketId);
                $readStmt->execute();
                $adminReadUpdated = $readStmt->affected_rows > 0;
                $readStmt->close();
            }
        }

        // Response phai luon phan anh trang thai da doc, ke ca request khac vua cap nhat truoc do.
        $ticket['admin_read'] = 1;

        if ($adminReadUpdated) {
            $history = [];
            $rawHistory = trim((string)($ticket['chat_history'] ?? ''));
            if ($rawHistory !== '' && $rawHistory !== 'null') {
                $history = json_decode($rawHistory, true) ?? [];
            }
            $adminReadEvent = tt_publish_support_ticket_event(
                (string)($ticket['device_id'] ?? ''),
                $resolvedTicketId,
                'admin_read',
                (int)($ticket['status'] ?? 0),
                is_array($history) ? count($history) : 0,
                $firebase_service_account_path,
                $firebase_project_id
            );
        }
    } elseif (!array_key_exists('admin_read', $ticket)) {
        // Tuong thich trong luc chua chay ALTER TABLE.
        $ticket['admin_read'] = 0;
    }

    tt_json_exit([
        'success' => true,
        'ticket' => $ticket,
        'requested_ticket_id' => $ticketId,
        'resolved_ticket_id' => $resolvedTicketId,
        'requester_role' => $adminAccount['role'] ?? 'user',
        'last_sender' => $lastSender,
        'admin_read_updated' => $adminReadUpdated,
        'admin_read_realtime_event' => (bool)($adminReadEvent['success'] ?? false),
        'admin_read_realtime_http' => (int)($adminReadEvent['http_code'] ?? 0),
        'read_receipt_version' => 'admin_auto_v2'
    ]);
}


// =========================================================
// PHÂN HỆ HỖ TRỢ & BÁO CÁO (CẦN XỬ LÝ & LỊCH SỬ)
// =========================================================

if ($action === 'report' || $action === 'contact_support') {
    $deviceId = trim((string)($_POST['device_id'] ?? ''));
    $type = ($action === 'report') ? 'report' : 'support';
    $requesterEmail = tt_normalize_email($_POST['requester_email'] ?? ($_POST['user_email'] ?? ''));
    $subject = trim((string)($_POST['subject'] ?? ''));

    if ($action === 'report') {
        $songId = trim((string)($_POST['song_id'] ?? ''));
        $reason = trim((string)($_POST['reason'] ?? ''));
        $note = trim((string)($_POST['note'] ?? ''));
        $subject = "Báo cáo bài hát ID: $songId";
        $message = "Lý do: $reason. Ghi chú: $note";

        $stmt = $conn->prepare(
            "INSERT INTO support_tickets (type, device_id, subject, message, status, user_read, created_at)
             VALUES (?, ?, ?, ?, 0, 0, NOW())"
        );
        $stmt->bind_param('ssss', $type, $deviceId, $subject, $message);
        $ok = $stmt->execute();
        $err = $stmt->error;
        $stmt->close();
        if ($ok) tt_json_exit(['success' => true]);
        tt_json_exit(['success' => false, 'error' => 'Lỗi MySQL: ' . $err], 500);
    }

    $message = trim((string)($_POST['message'] ?? ''));
    if ($deviceId === '' || $message === '') {
        tt_json_exit(['success' => false, 'error' => 'Thiếu thiết bị hoặc nội dung hỗ trợ.'], 422);
    }

    // Client cũ chưa gửi email vẫn được gộp theo device_id.
    if (tt_support_merge_schema_ready($conn)) {
        $conversationKey = tt_support_conversation_key($requesterEmail, $deviceId);
        $emailForDb = tt_support_valid_email($requesterEmail) ? $requesterEmail : null;
        $conn->begin_transaction();
        try {
            if ($emailForDb !== null) {
                $find = $conn->prepare(
                    "SELECT * FROM support_tickets
                     WHERE type='support' AND merged_into_id IS NULL
                       AND (conversation_key=? OR device_id=? OR LOWER(COALESCE(user_email,''))=?)
                     ORDER BY created_at ASC, id ASC FOR UPDATE"
                );
                if (!$find) throw new Exception('Không chuẩn bị được truy vấn tìm cuộc trò chuyện cũ.');
                $find->bind_param('sss', $conversationKey, $deviceId, $emailForDb);
            } else {
                $find = $conn->prepare(
                    "SELECT * FROM support_tickets
                     WHERE type='support' AND merged_into_id IS NULL
                       AND (conversation_key=? OR device_id=?)
                     ORDER BY created_at ASC, id ASC FOR UPDATE"
                );
                if (!$find) throw new Exception('Không chuẩn bị được truy vấn tìm cuộc trò chuyện cũ.');
                $find->bind_param('ss', $conversationKey, $deviceId);
            }
            $find->execute();
            $res = $find->get_result();
            $rows = [];
            while ($res && ($row = $res->fetch_assoc())) $rows[] = $row;
            $find->close();

            $newUserMessage = ['sender'=>'user', 'text'=>$message, 'timestamp'=>date('Y-m-d H:i:s')];
            if (!empty($rows)) {
                $canonical = $rows[0];
                foreach ($rows as $row) {
                    if (($row['conversation_key'] ?? '') === $conversationKey) { $canonical = $row; break; }
                }
                $ticketId = (string)$canonical['id'];
                $history = tt_support_merge_histories($rows);
                $history[] = $newUserMessage;
                $historyJson = json_encode($history, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
                $upd = $conn->prepare(
                    "UPDATE support_tickets SET device_id=?, conversation_key=?, user_email=?, subject=?, chat_history=?, status=0, user_read=1, merged_into_id=NULL WHERE id=?"
                );
                $upd->bind_param('ssssss', $deviceId, $conversationKey, $emailForDb, $subject, $historyJson, $ticketId);
                if (!$upd->execute()) throw new Exception($upd->error);
                $upd->close();
                if (tt_support_read_receipt_schema_ready($conn)) {
                    $readReset = $conn->prepare("UPDATE support_tickets SET admin_read=0 WHERE id=?");
                    if ($readReset) {
                        $readReset->bind_param('s', $ticketId);
                        $readReset->execute();
                        $readReset->close();
                    }
                }
                foreach ($rows as $row) {
                    $oldId=(string)$row['id']; if ($oldId===$ticketId) continue;
                    $mg=$conn->prepare("UPDATE support_tickets SET conversation_key=NULL, merged_into_id=? WHERE id=?");
                    $mg->bind_param('ss',$ticketId,$oldId);
                    if (!$mg->execute()) throw new Exception($mg->error);
                    $mg->close();
                }
            } else {
                $historyJson = json_encode([$newUserMessage], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
                $ins = $conn->prepare(
                    "INSERT INTO support_tickets (type,device_id,conversation_key,user_email,subject,message,chat_history,status,user_read,merged_into_id,created_at)
                     VALUES ('support',?,?,?,?,?,?,0,1,NULL,NOW())"
                );
                $ins->bind_param('ssssss', $deviceId, $conversationKey, $emailForDb, $subject, $message, $historyJson);
                if (!$ins->execute()) throw new Exception($ins->error);
                $ticketId=(string)$conn->insert_id;
                $ins->close();
            }
            $conn->commit();
            tt_json_exit(['success'=>true,'ticket_id'=>$ticketId,'reused_conversation'=>!empty($rows)]);
        } catch (Throwable $e) {
            $conn->rollback();
            tt_json_exit(['success'=>false,'error'=>'Lỗi MySQL: '.$e->getMessage()],500);
        }
    }

    // Fallback nếu chưa chạy migration SQL.
    $stmt = $conn->prepare("INSERT INTO support_tickets (type, device_id, subject, message, status, user_read, created_at) VALUES ('support', ?, ?, ?, 0, 0, NOW())");
    $stmt->bind_param('sss', $deviceId, $subject, $message);
    $ok=$stmt->execute(); $err=$stmt->error; $stmt->close();
    if ($ok) tt_json_exit(['success'=>true]);
    tt_json_exit(['success'=>false,'error'=>'Lỗi MySQL: '.$err],500);
}

// Admin xóa toàn bộ một cuộc trò chuyện hỗ trợ/báo cáo từ Cần xử lý hoặc Lịch sử.
// Chỉ tài khoản có trong songs_app_admins mới được phép xóa.
if ($action === 'delete_support_ticket') {
    $ticketId = trim((string)($_POST['id'] ?? ''));
    $adminEmail = tt_normalize_email($_POST['admin_email'] ?? '');
    $adminAccount = tt_get_admin_account($conn, $adminEmail);

    if (!$adminAccount) {
        tt_json_exit([
            'success' => false,
            'error' => 'Tài khoản Google đang đăng nhập không có quyền Admin.'
        ], 403);
    }
    if ($ticketId === '' || !ctype_digit($ticketId)) {
        tt_json_exit(['success' => false, 'error' => 'Mã cuộc trò chuyện không hợp lệ.'], 422);
    }

    $resolvedTicketId = tt_support_resolve_ticket_id($conn, $ticketId);
    $select = $conn->prepare(
        "SELECT id, device_id, type FROM support_tickets WHERE id = ? LIMIT 1"
    );
    $select->bind_param('s', $resolvedTicketId);
    $select->execute();
    $result = $select->get_result();
    $ticket = $result ? $result->fetch_assoc() : null;
    $select->close();

    if (!$ticket) {
        tt_json_exit(['success' => false, 'error' => 'Không tìm thấy cuộc trò chuyện.'], 404);
    }

    $targetDeviceId = trim((string)($ticket['device_id'] ?? ''));
    $idsToDelete = [$resolvedTicketId];

    if (tt_support_merge_schema_ready($conn)) {
        $mergedStmt = $conn->prepare("SELECT id FROM support_tickets WHERE merged_into_id = ?");
        if ($mergedStmt) {
            $mergedStmt->bind_param('s', $resolvedTicketId);
            $mergedStmt->execute();
            $mergedResult = $mergedStmt->get_result();
            while ($mergedResult && ($row = $mergedResult->fetch_assoc())) {
                $idsToDelete[] = (string)$row['id'];
            }
            $mergedStmt->close();
        }
    }

    $conn->begin_transaction();
    try {
        // Xóa các row đã gộp trước để an toàn nếu sau này có foreign key.
        if (tt_support_merge_schema_ready($conn)) {
            $deleteMerged = $conn->prepare("DELETE FROM support_tickets WHERE merged_into_id = ?");
            $deleteMerged->bind_param('s', $resolvedTicketId);
            if (!$deleteMerged->execute()) throw new Exception($deleteMerged->error);
            $deleteMerged->close();
        }

        $deleteMain = $conn->prepare("DELETE FROM support_tickets WHERE id = ?");
        $deleteMain->bind_param('s', $resolvedTicketId);
        if (!$deleteMain->execute()) throw new Exception($deleteMain->error);
        $deleteMain->close();

        $conn->commit();
    } catch (Throwable $e) {
        $conn->rollback();
        tt_json_exit(['success' => false, 'error' => 'Lỗi MySQL: ' . $e->getMessage()], 500);
    }

    // Dọn tín hiệu Realtime theo kiểu best-effort; lỗi Firebase không làm khôi phục dữ liệu MySQL đã xóa.
    if ($targetDeviceId !== '') {
        foreach (array_unique($idsToDelete) as $oldTicketId) {
            tt_realtime_database_request(
                'DELETE',
                'support_ticket_events/' . $targetDeviceId . '/' . $oldTicketId,
                null,
                $firebase_service_account_path,
                $firebase_project_id
            );
            tt_realtime_database_request(
                'DELETE',
                'contact_requests/' . $targetDeviceId . '/ticket_' . $oldTicketId,
                null,
                $firebase_service_account_path,
                $firebase_project_id
            );
        }

        // Dọn cả request cũ có ID ngẫu nhiên nhưng trỏ về ticket vừa xóa.
        $requestsResult = tt_realtime_database_request(
            'GET',
            'contact_requests/' . $targetDeviceId,
            null,
            $firebase_service_account_path,
            $firebase_project_id
        );
        if ($requestsResult['success'] && is_array($requestsResult['data'])) {
            foreach ($requestsResult['data'] as $requestId => $payload) {
                if (!is_array($payload)) continue;
                $oldTicketId = (string)($payload['ticket_id'] ?? '');
                if (in_array($oldTicketId, $idsToDelete, true)) {
                    tt_realtime_database_request(
                        'DELETE',
                        'contact_requests/' . $targetDeviceId . '/' . $requestId,
                        null,
                        $firebase_service_account_path,
                        $firebase_project_id
                    );
                }
            }
        }
    }

    tt_json_exit([
        'success' => true,
        'message' => 'Đã xóa cuộc trò chuyện.',
        'ticket_id' => $resolvedTicketId,
        'deleted_rows' => count(array_unique($idsToDelete))
    ]);
}

if ($action === 'get_support_data') {
    $deviceId = trim((string)($_GET['device_id'] ?? ''));
    $requesterEmail = tt_normalize_email($_GET['requester_email'] ?? ($_GET['email'] ?? ''));
    $adminAccount = tt_get_admin_account($conn, $requesterEmail);
    $isAdmin = $adminAccount !== null;
    $tickets = [];
    $unreadCount = 0;
    $stmt = null;

    if ($isAdmin) {
        $sql = tt_support_merge_schema_ready($conn)
            ? "SELECT * FROM support_tickets WHERE merged_into_id IS NULL ORDER BY created_at DESC"
            : "SELECT * FROM support_tickets ORDER BY created_at DESC";
        $result = $conn->query($sql);
    } elseif ($deviceId !== '') {
        if (tt_support_merge_schema_ready($conn) && tt_support_valid_email($requesterEmail)) {
            $stmt = $conn->prepare(
                "SELECT * FROM support_tickets
                 WHERE merged_into_id IS NULL
                   AND (device_id = ? OR LOWER(COALESCE(user_email,'')) = ?)
                 ORDER BY created_at DESC"
            );
            $stmt->bind_param('ss', $deviceId, $requesterEmail);
        } elseif (tt_support_merge_schema_ready($conn)) {
            $stmt = $conn->prepare(
                "SELECT * FROM support_tickets WHERE merged_into_id IS NULL AND device_id = ? ORDER BY created_at DESC"
            );
            $stmt->bind_param('s', $deviceId);
        } else {
            $stmt = $conn->prepare("SELECT * FROM support_tickets WHERE device_id = ? ORDER BY created_at DESC");
            $stmt->bind_param('s', $deviceId);
        }
        $stmt->execute();
        $result = $stmt->get_result();
    } else {
        $result = false;
    }

    if ($result && $result->num_rows > 0) {
        while ($row = $result->fetch_assoc()) {
            $tickets[] = $row;
            if ($isAdmin && (int)$row['status'] === 0) $unreadCount++;
            if (!$isAdmin && (int)$row['status'] === 1 && (int)$row['user_read'] === 0) $unreadCount++;
        }
    }
    if ($stmt instanceof mysqli_stmt) $stmt->close();

    tt_json_exit([
        'success' => true,
        'unread_count' => $unreadCount,
        'tickets' => $tickets,
        'is_admin' => $isAdmin,
        'role' => $adminAccount['role'] ?? 'user'
    ]);
}

// Upload ảnh trong cuộc trò chuyện hỗ trợ.
// Ảnh được lưu trên chính web server, còn chat_history chỉ lưu URL dạng [[image]]URL.
if ($action === 'upload_support_image') {
    $id = trim((string)($_POST['id'] ?? ''));
    $requesterEmail = tt_normalize_email(
        $_POST['requester_email'] ?? ($_POST['admin_email'] ?? '')
    );
    $deviceId = trim((string)($_POST['device_id'] ?? ''));

    if ($id === '' || !ctype_digit($id)) {
        tt_json_exit(['success' => false, 'error' => 'Mã cuộc trò chuyện không hợp lệ.'], 422);
    }

    $adminAccount = tt_get_admin_account($conn, $requesterEmail);
    $id = tt_support_resolve_ticket_id($conn, $id);
    $stmt = $conn->prepare("SELECT device_id, user_email FROM support_tickets WHERE id = ? LIMIT 1");
    $stmt->bind_param('s', $id);
    $stmt->execute();
    $res = $stmt->get_result();
    $ticketRow = $res ? $res->fetch_assoc() : null;
    $stmt->close();

    if (!$ticketRow) {
        tt_json_exit(['success' => false, 'error' => 'Không tìm thấy cuộc trò chuyện.'], 404);
    }

    if (!$adminAccount && !tt_support_ticket_access_allowed($ticketRow, $deviceId, $requesterEmail)) {
        tt_json_exit(['success' => false, 'error' => 'Bạn không có quyền gửi ảnh vào cuộc trò chuyện này.'], 403);
    }

    if (!isset($_FILES['image']) || !is_array($_FILES['image'])) {
        tt_json_exit(['success' => false, 'error' => 'Chưa chọn hình ảnh.'], 422);
    }

    $extensions = [
        'image/jpeg' => 'jpg',
        'image/png' => 'png',
        'image/webp' => 'webp'
    ];

    $file = $_FILES['image'];
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        tt_json_exit([
            'success' => false,
            'error' => 'Tải ảnh lên thất bại. Mã lỗi: ' . (int)($file['error'] ?? -1)
        ], 422);
    }

    $maxBytes = 8 * 1024 * 1024;
    $fileSize = (int)($file['size'] ?? 0);
    if ($fileSize <= 0 || $fileSize > $maxBytes) {
        tt_json_exit(['success' => false, 'error' => 'Ảnh phải nhỏ hơn 8 MB.'], 422);
    }

    $tmpPath = (string)($file['tmp_name'] ?? '');
    $imageInfo = $tmpPath !== '' ? @getimagesize($tmpPath) : false;
    if ($imageInfo === false) {
        tt_json_exit(['success' => false, 'error' => 'File tải lên không phải hình ảnh hợp lệ.'], 422);
    }

    $mime = strtolower((string)($imageInfo['mime'] ?? ''));
    if (!isset($extensions[$mime])) {
        tt_json_exit(['success' => false, 'error' => 'Chỉ hỗ trợ JPG, PNG hoặc WEBP.'], 422);
    }

    // Thumbnail la file rieng do app da resize/nén truoc khi upload.
    // Client cu khong gui thumbnail van hoat dong: thumbnail_url se fallback ve image_url.
    $thumbFile = isset($_FILES['thumbnail']) && is_array($_FILES['thumbnail'])
        ? $_FILES['thumbnail']
        : null;
    $thumbTmpPath = '';
    $thumbMime = '';
    $thumbSize = 0;
    $thumbIsValid = false;

    if ($thumbFile && ($thumbFile['error'] ?? UPLOAD_ERR_NO_FILE) === UPLOAD_ERR_OK) {
        $thumbSize = (int)($thumbFile['size'] ?? 0);
        $thumbTmpPath = (string)($thumbFile['tmp_name'] ?? '');
        $thumbInfo = $thumbTmpPath !== '' ? @getimagesize($thumbTmpPath) : false;
        $thumbMime = $thumbInfo !== false
            ? strtolower((string)($thumbInfo['mime'] ?? ''))
            : '';

        // Thumbnail 480 px / JPEG 68% thuong rat nho; gioi han 2 MB de tranh upload bat thuong.
        $thumbIsValid = $thumbSize > 0
            && $thumbSize <= 2 * 1024 * 1024
            && isset($extensions[$thumbMime]);
    }

    $monthFolder = date('Y-m');
    $baseDir = __DIR__ . DIRECTORY_SEPARATOR . 'support_chat_uploads';
    $targetDir = $baseDir . DIRECTORY_SEPARATOR . $monthFolder;

    if (!is_dir($targetDir) && !@mkdir($targetDir, 0755, true) && !is_dir($targetDir)) {
        tt_json_exit([
            'success' => false,
            'error' => 'Máy chủ chưa có quyền tạo thư mục support_chat_uploads.'
        ], 500);
    }

    try {
        $random = bin2hex(random_bytes(10));
    } catch (Throwable $e) {
        $random = substr(sha1(uniqid('', true)), 0, 20);
    }

    $extension = $extensions[$mime];
    $baseName = 'ticket_' . $id . '_' . date('Ymd_His') . '_' . $random;
    $fileName = $baseName . '.' . $extension;
    $targetPath = $targetDir . DIRECTORY_SEPARATOR . $fileName;

    if (!@move_uploaded_file($tmpPath, $targetPath)) {
        tt_json_exit([
            'success' => false,
            'error' => 'Không thể lưu hình ảnh trên máy chủ. Hãy kiểm tra quyền ghi thư mục wwwroot.'
        ], 500);
    }

    $publicBaseUrl = rtrim((string)(getenv('TUNERTOOLS_PUBLIC_BASE_URL') ?: 'https://tunertools.top'), '/');
    $imageUrl = $publicBaseUrl
        . '/support_chat_uploads/' . rawurlencode($monthFolder)
        . '/' . rawurlencode($fileName);

    $thumbnailUrl = $imageUrl;
    $savedThumbSize = 0;

    if ($thumbIsValid) {
        $thumbExtension = $extensions[$thumbMime];
        $thumbFileName = $baseName . '_thumb.' . $thumbExtension;
        $thumbTargetPath = $targetDir . DIRECTORY_SEPARATOR . $thumbFileName;

        if (@move_uploaded_file($thumbTmpPath, $thumbTargetPath)) {
            $thumbnailUrl = $publicBaseUrl
                . '/support_chat_uploads/' . rawurlencode($monthFolder)
                . '/' . rawurlencode($thumbFileName);
            $savedThumbSize = $thumbSize;
        }
    }

    tt_json_exit([
        'success' => true,
        'image_url' => $imageUrl,
        'thumbnail_url' => $thumbnailUrl,
        'mime_type' => $mime,
        'size' => $fileSize,
        'thumbnail_size' => $savedThumbSize
    ]);
}

if ($action === 'reply_ticket') {
    $id = trim((string)($_POST['id'] ?? ''));
    $reply = trim((string)($_POST['reply'] ?? ''));
    $requesterEmail = tt_normalize_email($_POST['requester_email'] ?? ($_POST['admin_email'] ?? ''));
    $deviceId = trim((string)($_POST['device_id'] ?? ''));

    if ($id === '' || !ctype_digit($id) || $reply === '') {
        tt_json_exit(['success' => false, 'error' => 'Thiếu mã phiếu hoặc nội dung trả lời.'], 422);
    }

    $id = tt_support_resolve_ticket_id($conn, $id);
    $adminAccount = tt_get_admin_account($conn, $requesterEmail);
    $selectSql = tt_support_merge_schema_ready($conn)
        ? "SELECT device_id, user_email, chat_history FROM support_tickets WHERE id = ? LIMIT 1"
        : "SELECT device_id, chat_history FROM support_tickets WHERE id = ? LIMIT 1";
    $stmt = $conn->prepare($selectSql);
    $stmt->bind_param('s', $id);
    $stmt->execute();
    $res = $stmt->get_result();
    $ticketRow = $res ? $res->fetch_assoc() : null;
    $stmt->close();
    if (!$ticketRow) tt_json_exit(['success' => false, 'error' => 'Không tìm thấy phiếu hỗ trợ.'], 404);

    if ($adminAccount) {
        $sender = 'admin';
    } else {
        if (!tt_support_ticket_access_allowed($ticketRow, $deviceId, $requesterEmail)) {
            tt_json_exit(['success' => false, 'error' => 'Bạn không có quyền trả lời phiếu này.'], 403);
        }
        $sender = 'user';
    }

    $history = [];
    if (!empty($ticketRow['chat_history']) && $ticketRow['chat_history'] !== 'null') {
        $history = json_decode($ticketRow['chat_history'], true) ?? [];
    }
    $newMessage = ['sender'=>$sender, 'text'=>$reply, 'timestamp'=>date('Y-m-d H:i:s')];
    if ($sender === 'admin') {
        $newMessage['sender_email']=$adminAccount['email'];
        $newMessage['sender_name']=tt_display_name_from_email($adminAccount['email']);
    }
    $history[]=$newMessage;
    $newHistoryJson=json_encode($history, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    $status=($sender==='admin')?1:0;
    $userRead=($sender==='admin')?0:1;
    $adminRead=($sender==='admin')?1:0;

    if (tt_support_read_receipt_schema_ready($conn)) {
        $updateStmt=$conn->prepare("UPDATE support_tickets SET chat_history=?, status=?, user_read=?, admin_read=? WHERE id=?");
        $updateStmt->bind_param('siiis',$newHistoryJson,$status,$userRead,$adminRead,$id);
    } else {
        // Tuong thich tam thoi neu PHP duoc cap nhat truoc khi chay ALTER TABLE.
        $updateStmt=$conn->prepare("UPDATE support_tickets SET chat_history=?, status=?, user_read=? WHERE id=?");
        $updateStmt->bind_param('siis',$newHistoryJson,$status,$userRead,$id);
    }
    if ($updateStmt->execute()) {
        $updateStmt->close();
        $eventResult=tt_publish_support_ticket_event(
            (string)$ticketRow['device_id'], $id, $sender, $status, count($history),
            $firebase_service_account_path, $firebase_project_id
        );
        // Nếu user đang trả lời từ một device khác nhưng cùng email, cũng phát event cho device hiện tại.
        if ($sender === 'user' && $deviceId !== '' && $deviceId !== (string)$ticketRow['device_id']) {
            tt_publish_support_ticket_event(
                $deviceId, $id, $sender, $status, count($history),
                $firebase_service_account_path, $firebase_project_id
            );
        }
        tt_json_exit([
            'success'=>true,
            'ticket_id'=>$id,
            'sender'=>$sender,
            'role'=>$adminAccount['role'] ?? 'user',
            'realtime_event'=>(bool)$eventResult['success'],
            'realtime_http'=>(int)($eventResult['http_code'] ?? 0)
        ]);
    }
    $error=$updateStmt->error; $updateStmt->close();
    tt_json_exit(['success'=>false,'error'=>'Lỗi MySQL: '.$error],500);
}

if ($action === 'mark_user_read') {
    $id = trim((string)($_POST['id'] ?? ''));
    $deviceId = trim((string)($_POST['device_id'] ?? ''));
    $requesterEmail = tt_normalize_email($_POST['requester_email'] ?? ($_POST['admin_email'] ?? ''));
    $adminAccount = tt_get_admin_account($conn, $requesterEmail);
    if ($id === '' || !ctype_digit($id)) tt_json_exit(['success'=>false,'error'=>'Mã phiếu không hợp lệ.'],422);

    $id = tt_support_resolve_ticket_id($conn, $id);
    $selectSql = tt_support_merge_schema_ready($conn)
        ? "SELECT device_id, user_email, chat_history, status FROM support_tickets WHERE id = ? LIMIT 1"
        : "SELECT device_id, chat_history, status FROM support_tickets WHERE id = ? LIMIT 1";
    $selectStmt=$conn->prepare($selectSql);
    $selectStmt->bind_param('s',$id);
    $selectStmt->execute();
    $res=$selectStmt->get_result();
    $ticket=$res?$res->fetch_assoc():null;
    $selectStmt->close();
    if (!$ticket) tt_json_exit(['success'=>false,'error'=>'Không tìm thấy phiếu hỗ trợ.'],404);
    if (!$adminAccount && !tt_support_ticket_access_allowed($ticket,$deviceId,$requesterEmail)) {
        tt_json_exit(['success'=>false,'error'=>'Bạn không có quyền đọc phiếu này.'],403);
    }
    $updateStmt=$conn->prepare("UPDATE support_tickets SET user_read=1 WHERE id=?");
    $updateStmt->bind_param('s',$id);
    $ok=$updateStmt->execute(); $err=$updateStmt->error; $updateStmt->close();
    if ($ok) {
        $history = [];
        if (!empty($ticket['chat_history']) && $ticket['chat_history'] !== 'null') {
            $history = json_decode($ticket['chat_history'], true) ?? [];
        }
        $eventResult = tt_publish_support_ticket_event(
            (string)$ticket['device_id'], $id, 'user_read', (int)($ticket['status'] ?? 1), count($history),
            $firebase_service_account_path, $firebase_project_id
        );
        tt_json_exit([
            'success'=>true,
            'ticket_id'=>$id,
            'realtime_event'=>(bool)$eventResult['success']
        ]);
    }
    tt_json_exit(['success'=>false,'error'=>'Lỗi MySQL: '.$err],500);
}

// Admin da mo/xem tin nhan cuoi cua User.
// Chi Admin/Moderator duoc phep danh dau. User se nhan event Realtime va hien "Da xem".
if ($action === 'mark_admin_read') {
    $id = trim((string)($_POST['id'] ?? ''));
    $adminEmail = tt_normalize_email($_POST['requester_email'] ?? ($_POST['admin_email'] ?? ''));
    $adminAccount = tt_get_admin_account($conn, $adminEmail);

    if ($id === '' || !ctype_digit($id)) {
        tt_json_exit(['success'=>false,'error'=>'Mã phiếu không hợp lệ.'],422);
    }
    if (!$adminAccount) {
        tt_json_exit(['success'=>false,'error'=>'Chỉ Admin/Moderator mới được đánh dấu đã xem.'],403);
    }
    if (!tt_support_read_receipt_schema_ready($conn)) {
        tt_json_exit([
            'success'=>false,
            'error'=>'Thiếu cột admin_read. Hãy chạy ALTER TABLE support_tickets trước.',
            'code'=>'READ_RECEIPT_SCHEMA_MISSING'
        ],500);
    }

    $id = tt_support_resolve_ticket_id($conn, $id);
    $selectStmt = $conn->prepare(
        "SELECT device_id, chat_history, status FROM support_tickets WHERE id=? LIMIT 1"
    );
    $selectStmt->bind_param('s',$id);
    $selectStmt->execute();
    $res=$selectStmt->get_result();
    $ticket=$res?$res->fetch_assoc():null;
    $selectStmt->close();
    if (!$ticket) tt_json_exit(['success'=>false,'error'=>'Không tìm thấy phiếu hỗ trợ.'],404);

    $updateStmt=$conn->prepare("UPDATE support_tickets SET admin_read=1 WHERE id=?");
    $updateStmt->bind_param('s',$id);
    $ok=$updateStmt->execute();
    $err=$updateStmt->error;
    $updateStmt->close();

    if ($ok) {
        $history = [];
        if (!empty($ticket['chat_history']) && $ticket['chat_history'] !== 'null') {
            $history = json_decode($ticket['chat_history'], true) ?? [];
        }
        $eventResult = tt_publish_support_ticket_event(
            (string)$ticket['device_id'], $id, 'admin_read', (int)($ticket['status'] ?? 0), count($history),
            $firebase_service_account_path, $firebase_project_id
        );
        tt_json_exit([
            'success'=>true,
            'ticket_id'=>$id,
            'realtime_event'=>(bool)$eventResult['success']
        ]);
    }
    tt_json_exit(['success'=>false,'error'=>'Lỗi MySQL: '.$err],500);
}


// =========================================================
// PHÂN HỆ DUYỆT BÀI HÁT (PENDING SONGS)
// =========================================================

// --- THÊM BÀI HÁT (TỰ ĐỘNG UPLOAD ẢNH LÊN GITHUB) ---
if ($action === 'add') {
    $title = trim((string)($_POST['title'] ?? ''));
    $singer = trim((string)($_POST['singer'] ?? ''));
    $composer = trim((string)($_POST['composer'] ?? ''));
    $role = strtolower(trim((string)($_POST['role'] ?? 'user')));
    if ($role === 'supper_admin') $role = 'super_admin';

    $baseTone = trim((string)($_POST['baseTone'] ?? 'C'));
    $rhythm = trim((string)($_POST['rhythm'] ?? ''));
    $bpm = isset($_POST['bpm']) ? (int)$_POST['bpm'] : 0;
    $videoUrl = trim((string)($_POST['videoUrl'] ?? ''));
    $raw_lyrics = (string)($_POST['lyrics'] ?? '');
    $expectedImageCount = max(0, (int)($_POST['expected_image_count'] ?? 0));

    if ($title === '') {
        tt_json_exit([
            'success' => false,
            'error' => 'Tên bài hát không được để trống.',
            'code' => 'EMPTY_TITLE'
        ], 422);
    }

    $chordList = '';
    if (preg_match_all('/\[(.*?)\]/', $raw_lyrics, $matches)) {
        $chords = array_unique($matches[1]);
        $chordList = implode(',', $chords);
    }

    $status = in_array($role, ['super_admin', 'admin', 'mod'], true) ? 1 : 0;
    $id = uniqid();

    $baseSlug = strtolower(trim(preg_replace('/[^A-Za-z0-9-]+/', '-', $title)));
    $baseSlug = trim(preg_replace('/-+/', '-', $baseSlug), '-');
    if ($baseSlug === '') $baseSlug = 'song';
    $slug = $baseSlug . '-' . substr(md5(uniqid('', true)), 0, 6);

    // Chuẩn hóa danh sách file PHP nhận được và không bỏ qua lỗi upload âm thầm.
    $validImageFiles = [];
    $uploadErrors = [];

    if (isset($_FILES['sheet_images']) && is_array($_FILES['sheet_images'])) {
        $files = $_FILES['sheet_images'];
        $names = is_array($files['name'] ?? null) ? $files['name'] : [($files['name'] ?? '')];
        $tmpNames = is_array($files['tmp_name'] ?? null) ? $files['tmp_name'] : [($files['tmp_name'] ?? '')];
        $errors = is_array($files['error'] ?? null) ? $files['error'] : [($files['error'] ?? UPLOAD_ERR_NO_FILE)];
        $sizes = is_array($files['size'] ?? null) ? $files['size'] : [($files['size'] ?? 0)];
        $totalFiles = count($names);

        $allowedMimes = [
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'image/webp' => 'webp',
            'image/gif' => 'gif'
        ];

        for ($i = 0; $i < $totalFiles; $i++) {
            $errorCode = (int)($errors[$i] ?? UPLOAD_ERR_NO_FILE);
            if ($errorCode === UPLOAD_ERR_NO_FILE) continue;

            if ($errorCode !== UPLOAD_ERR_OK) {
                switch ($errorCode) {
                    case UPLOAD_ERR_INI_SIZE:
                        $message = 'vượt giới hạn upload_max_filesize=' . ini_get('upload_max_filesize');
                        break;
                    case UPLOAD_ERR_FORM_SIZE:
                        $message = 'vượt giới hạn dung lượng của biểu mẫu';
                        break;
                    case UPLOAD_ERR_PARTIAL:
                        $message = 'chỉ tải lên được một phần';
                        break;
                    case UPLOAD_ERR_NO_TMP_DIR:
                        $message = 'máy chủ thiếu thư mục tạm';
                        break;
                    case UPLOAD_ERR_CANT_WRITE:
                        $message = 'máy chủ không ghi được file tạm';
                        break;
                    case UPLOAD_ERR_EXTENSION:
                        $message = 'bị tiện ích PHP chặn';
                        break;
                    default:
                        $message = 'mã lỗi upload ' . $errorCode;
                        break;
                }
                $uploadErrors[] = 'Trang ' . ($i + 1) . ' ' . $message . '.';
                continue;
            }

            $tmpName = (string)($tmpNames[$i] ?? '');
            $fileSize = (int)($sizes[$i] ?? 0);
            if ($tmpName === '' || !is_file($tmpName) || $fileSize <= 0) {
                $uploadErrors[] = 'Trang ' . ($i + 1) . ' không có dữ liệu ảnh hợp lệ.';
                continue;
            }

            $imageInfo = @getimagesize($tmpName);
            $mime = is_array($imageInfo) ? strtolower((string)($imageInfo['mime'] ?? '')) : '';
            if (!isset($allowedMimes[$mime])) {
                $uploadErrors[] = 'Trang ' . ($i + 1) . ' không phải ảnh JPG, PNG, WEBP hoặc GIF hợp lệ.';
                continue;
            }

            $validImageFiles[] = [
                'tmp_name' => $tmpName,
                'extension' => $allowedMimes[$mime],
                'size' => $fileSize
            ];
        }
    }

    if (!empty($uploadErrors)) {
        tt_json_exit([
            'success' => false,
            'error' => implode(' ', $uploadErrors),
            'code' => 'IMAGE_UPLOAD_REJECTED',
            'expectedImageCount' => $expectedImageCount,
            'receivedImageCount' => count($validImageFiles),
            'upload_max_filesize' => ini_get('upload_max_filesize'),
            'post_max_size' => ini_get('post_max_size')
        ], 422);
    }

    if ($expectedImageCount > 0 && count($validImageFiles) !== $expectedImageCount) {
        tt_json_exit([
            'success' => false,
            'error' => 'Đã chọn ' . $expectedImageCount . ' trang nhưng PHP chỉ nhận được ' . count($validImageFiles) . ' trang.',
            'code' => 'IMAGE_COUNT_MISMATCH',
            'expectedImageCount' => $expectedImageCount,
            'receivedImageCount' => count($validImageFiles),
            'upload_max_filesize' => ini_get('upload_max_filesize'),
            'post_max_size' => ini_get('post_max_size')
        ], 422);
    }

    $ghUsername = 'ducphi0491';
    $ghRepo = 'sheet_images';
    $ghBranch = 'main';
    $ghToken = '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';

    $createdGithubFiles = [];
    $rollbackGithubFiles = function () use (
        &$createdGithubFiles,
        $ghUsername,
        $ghRepo,
        $ghBranch,
        $ghToken
    ) {
        foreach ($createdGithubFiles as $createdFile) {
            $fileName = (string)($createdFile['name'] ?? '');
            $sha = (string)($createdFile['sha'] ?? '');
            if ($fileName === '' || $sha === '') continue;

            $apiUrl = "https://api.github.com/repos/{$ghUsername}/{$ghRepo}/contents/" . rawurlencode($fileName);
            $deleteBody = json_encode([
                'message' => 'Rollback failed song upload: ' . $fileName,
                'sha' => $sha,
                'branch' => $ghBranch
            ]);
            $chDelete = curl_init($apiUrl);
            curl_setopt_array($chDelete, [
                CURLOPT_RETURNTRANSFER => true,
                CURLOPT_CUSTOMREQUEST => 'DELETE',
                CURLOPT_POSTFIELDS => $deleteBody,
                CURLOPT_CONNECTTIMEOUT => 15,
                CURLOPT_TIMEOUT => 60,
                CURLOPT_SSL_VERIFYPEER => false,
                CURLOPT_HTTPHEADER => [
                    "Authorization: token {$ghToken}",
                    'User-Agent: TunerApp-Android',
                    'Content-Type: application/json'
                ]
            ]);
            curl_exec($chDelete);
            curl_close($chDelete);
        }
    };

    $imageCount = 0;
    foreach ($validImageFiles as $imageFile) {
        $nextIndex = $imageCount + 1;
        $fileName = $slug . '-' . $nextIndex . '.' . $imageFile['extension'];
        $apiUrl = "https://api.github.com/repos/{$ghUsername}/{$ghRepo}/contents/" . rawurlencode($fileName);
        $base64Content = base64_encode(file_get_contents($imageFile['tmp_name']));
        $bodyData = json_encode([
            'message' => 'Upload từ App: ' . $fileName,
            'content' => $base64Content,
            'branch' => $ghBranch
        ]);

        $ch = curl_init($apiUrl);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST => 'PUT',
            CURLOPT_POSTFIELDS => $bodyData,
            CURLOPT_CONNECTTIMEOUT => 20,
            CURLOPT_TIMEOUT => 120,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_HTTPHEADER => [
                "Authorization: token {$ghToken}",
                'User-Agent: TunerApp-Android',
                'Content-Type: application/json'
            ]
        ]);
        $githubResponse = curl_exec($ch);
        $curlError = curl_error($ch);
        $githubHttpCode = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        $githubData = is_string($githubResponse) ? json_decode($githubResponse, true) : null;
        if ($curlError !== '' || !in_array($githubHttpCode, [200, 201], true)) {
            $rollbackGithubFiles();
            $githubMessage = is_array($githubData) ? (string)($githubData['message'] ?? '') : '';
            tt_json_exit([
                'success' => false,
                'error' => 'Không thể tải ảnh sheet trang ' . $nextIndex . ' lên kho ảnh.' .
                    ($githubMessage !== '' ? ' GitHub: ' . $githubMessage : '') .
                    ($curlError !== '' ? ' cURL: ' . $curlError : ''),
                'code' => 'GITHUB_UPLOAD_FAILED',
                'github_http' => $githubHttpCode,
                'uploadedImageCount' => $imageCount
            ], 502);
        }

        $createdGithubFiles[] = [
            'name' => $fileName,
            'sha' => is_array($githubData) ? (string)($githubData['content']['sha'] ?? '') : ''
        ];
        $imageCount++;
    }

    if ($expectedImageCount > 0 && $imageCount !== $expectedImageCount) {
        $rollbackGithubFiles();
        tt_json_exit([
            'success' => false,
            'error' => 'Máy chủ chưa lưu đủ số trang sheet đã chọn.',
            'code' => 'GITHUB_IMAGE_COUNT_MISMATCH',
            'expectedImageCount' => $expectedImageCount,
            'uploadedImageCount' => $imageCount
        ], 502);
    }

    $sql = "INSERT INTO songs (
                id, title, singerNames, authorName, originalKey, rhythm, bpm, youtubeUrl,
                lyrics, chordList, status, slug, image_count, createdAt, updatedAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        $rollbackGithubFiles();
        tt_json_exit([
            'success' => false,
            'error' => 'Không chuẩn bị được câu lệnh thêm bài hát: ' . $conn->error,
            'code' => 'MYSQL_PREPARE_FAILED'
        ], 500);
    }

    $stmt->bind_param(
        'ssssssisssisi',
        $id,
        $title,
        $singer,
        $composer,
        $baseTone,
        $rhythm,
        $bpm,
        $videoUrl,
        $raw_lyrics,
        $chordList,
        $status,
        $slug,
        $imageCount
    );

    if (!$stmt->execute()) {
        $mysqlError = $stmt->error;
        $stmt->close();
        $rollbackGithubFiles();
        tt_json_exit([
            'success' => false,
            'error' => 'Lỗi MySQL: ' . $mysqlError,
            'code' => 'MYSQL_INSERT_FAILED'
        ], 500);
    }
    $stmt->close();

    tt_json_exit([
        'success' => true,
        'newId' => $id,
        'slug' => $slug,
        'imageCount' => $imageCount,
        'expectedImageCount' => $expectedImageCount,
        'status' => $status
    ]);
}

if ($action === 'get_pending_songs') {
    $sql = "SELECT id AS songId, title, singerNames AS singer, authorName AS composer, slug, image_count AS imageCount 
            FROM songs WHERE status = 0 ORDER BY createdAt DESC";
    $result = $conn->query($sql);
    $pending = [];
    if ($result && $result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $row['imageCount'] = (int)$row['imageCount'];
            $pending[] = $row;
        }
    }
    echo json_encode($pending);
    exit;
}

if ($action === 'approve_song') {
    $id = isset($_POST['id']) ? $conn->real_escape_string($_POST['id']) : '';
    $sql = "UPDATE songs SET status = 1, updatedAt = NOW() WHERE id = '$id'";
    if ($conn->query($sql) === TRUE) echo json_encode(["success" => true]);
    else echo json_encode(["error" => "Lỗi MySQL"]);
    exit;
}

if ($action === 'delete') {
    $id = isset($_POST['id']) ? $conn->real_escape_string($_POST['id']) : '';
    
    $conn->query("DELETE FROM device_likes WHERE song_id = '$id'");
    
    $sql = "DELETE FROM songs WHERE id = '$id'";
    
    if ($conn->query($sql) === TRUE) {
        echo json_encode(["success" => true]);
    } else {
        echo json_encode(["error" => "Lỗi MySQL: " . $conn->error]);
    }
    exit;
}

// =========================================================
// TẢI TRỰC TIẾP FILE GUITAR PRO QUA PHP (CHỐNG LỖI 404 CỦA IIS)
// =========================================================
if ($action === 'download_gp') {
    $file_param = isset($_GET['file']) ? $_GET['file'] : '';
    if ($file_param !== '') {
        $baseDir = __DIR__ . DIRECTORY_SEPARATOR . 'tab';
        
        // Đảo ngược bảo mật đường dẫn và thay thế '/' thành dấu chia thư mục chuẩn của Windows
        $requestedRel = str_replace(array('/', '\\'), DIRECTORY_SEPARATOR, $file_param);
        $fullPath = realpath($baseDir . DIRECTORY_SEPARATOR . $requestedRel);

        // Kiểm tra file có tồn tại và nằm đúng trong thư mục tab không
        if ($fullPath && strpos($fullPath, realpath($baseDir)) === 0 && is_file($fullPath)) {
            header('Access-Control-Allow-Origin: *');
            header('Access-Control-Allow-Methods: GET, OPTIONS');
            header('Access-Control-Allow-Headers: *');
            header('Content-Type: application/octet-stream');
            header('Content-Disposition: inline; filename="' . basename($fullPath) . '"');
            header('Content-Length: ' . filesize($fullPath));
            
            // Dọn bộ nhớ đệm và nhổ luồng byte ra ngoài
            if (ob_get_length()) {
                ob_clean();
            }
            flush();
            readfile($fullPath);
            exit;
        }
    }
    http_response_code(404);
    echo "File not found";
    exit;
}

// =========================================================
// TÌM KIẾM FILE GUITAR PRO (ALPHA TAB)
// =========================================================
if ($action === 'api_gp_search') {
    $keyword = isset($_GET['q']) ? trim($_GET['q']) : '';
    $results = [];

    if ($keyword !== '') {
        $baseDir = __DIR__ . DIRECTORY_SEPARATOR . 'tab'; 
        
        // CẢI TIẾN: Cắt từ khóa thành mảng để tìm kiếm linh hoạt
        $keyword = preg_replace('/\.(gp[345x]|gp)$/i', '', $keyword);
        $cleanKeyword = strtolower(str_replace(['-', '.', ',', '(', ')', '[', ']'], ' ', $keyword));
        $searchTerms = array_values(array_filter(explode(' ', $cleanKeyword)));

        if (is_dir($baseDir)) {
            $iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($baseDir));
            
            foreach ($iterator as $file) {
                if ($file->isFile()) {
                    $ext = strtolower($file->getExtension());
                    
                    if (in_array($ext, ['gp3', 'gp4', 'gp5', 'gpx', 'gp'])) {
                        
                        $filename = strtolower($file->getFilename());
                        $artistFolder = strtolower(basename(dirname($file->getPathname())));
                        
                        $searchableString = $artistFolder . ' ' . $filename;
                        $searchableString = str_replace(['-', '.', ',', '(', ')', '[', ']'], ' ', $searchableString);
                        
                        $isMatch = true;
                        foreach ($searchTerms as $term) {
                            if (strpos($searchableString, $term) === false) {
                                $isMatch = false;
                                break;
                            }
                        }
                        
                        if ($isMatch) {
                            // Lấy đường dẫn tương đối để nhét vào biến 'file'
                            $relativePath = str_replace($baseDir, '', $file->getPathname());
                            // Chuyển \ của Windows thành / cho URL
                            $relativePath = str_replace('\\', '/', ltrim($relativePath, '\\/')); 
                            
                            // TẠO LINK TRUNG GIAN GỌI QUA PHP (GIỐNG TEST.PHP CỦA BẠN)
                            $fileUrl = "https://tunertools.top/api_songs.php?action=download_gp&file=" . rawurlencode($relativePath);

                            $results[] = [
                                "id" => md5($fileUrl),
                                "title" => $file->getBasename('.' . $ext), 
                                "artist" => basename(dirname($file->getPathname())), 
                                "file_url" => $fileUrl, // Link mới chống lỗi 404
                                "format" => $ext
                            ];
                        }
                    }
                }
                if (count($results) >= 50) break;
            }
        }
    }

    echo json_encode(["data" => $results], JSON_UNESCAPED_UNICODE);
    exit;
}

$conn->close();

// =========================================================
// LẤY DANH SÁCH ÂM THANH METRONOME TỪ THƯ MỤC
// =========================================================
if ($action === 'get_metronome_sounds') {
    $dir = __DIR__; 
    $sounds = [];
    
    if (is_dir($dir)) {
        $files = scandir($dir);
        foreach ($files as $file) {
            if (pathinfo($file, PATHINFO_EXTENSION) === 'wav') {
                $baseName = str_replace('.wav', '', $file);
                $displayName = ucfirst($baseName);
                $sounds[] = $displayName;
            }
        }
    }
    
    if (empty($sounds)) {
        $sounds = ["Standard Click"]; 
    }
    
    echo json_encode($sounds);
    exit;
}

// =========================================================
// TẢI TRỰC TIẾP FILE ÂM THANH QUA PHP ĐỂ TRÁNH LỖI WEB SERVER
// =========================================================
if ($action === 'download_sound') {
    $filename = isset($_GET['file']) ? $_GET['file'] : '';
    $filename = basename($filename);
    
    $filepath = 'C:/AI_Music/METRONOME/' . $filename;

    if (!empty($filename) && file_exists($filepath)) {
        if (ob_get_length()) {
            ob_clean();
        }
        flush();
        
        header('Content-Type: audio/wav');
        header('Content-Length: ' . filesize($filepath));
        
        readfile($filepath);
        exit;
    } else {
        http_response_code(404);
        echo "File không tồn tại tại: " . $filepath;
        exit;
    }
}

// =========================================================
// LẤY DANH SÁCH ÂM THANH TRỐNG TỪ Ổ C:\AI_Music\DRUM (CÓ THƯ MỤC)
// =========================================================
if ($action === 'get_drum_sounds') {
    $dir = 'C:/AI_Music/DRUM'; 
    $results = []; // Lưu trữ theo dạng ['Tên_thư_mục' => ['file1.wav', 'file2.wav']]
    
    if (is_dir($dir)) {
        $items = scandir($dir);
        foreach ($items as $item) {
            // Bỏ qua thư mục điều hướng hệ thống
            if ($item === '.' || $item === '..') continue;
            
            $path = $dir . '/' . $item;
            
            // Nếu là thư mục con (ví dụ: Ballad, Disco...)
            if (is_dir($path)) {
                $results[$item] = [];
                $subItems = scandir($path);
                foreach ($subItems as $subItem) {
                    $ext = strtolower(pathinfo($subItem, PATHINFO_EXTENSION));
                    if (in_array($ext, ['wav', 'mp3'])) {
                        // Lưu đường dẫn bao gồm cả tên thư mục để dễ download (VD: Disco/drum-120.wav)
                        $results[$item][] = $item . '/' . $subItem;
                    }
                }
                // Dọn dẹp nếu thư mục con không có file nhạc nào
                if (empty($results[$item])) {
                    unset($results[$item]);
                }
            } else {
                // Nếu file nhạc vứt trực tiếp ở thư mục gốc thì gom vào mục "Mặc định"
                $ext = strtolower(pathinfo($item, PATHINFO_EXTENSION));
                if (in_array($ext, ['wav', 'mp3'])) {
                    if (!isset($results['Mặc định'])) $results['Mặc định'] = [];
                    $results['Mặc định'][] = $item;
                }
            }
        }
    }
    
    // Nếu trống trơn thì trả về lỗi giả dưới dạng Json
    if (empty($results)) {
        if (!is_dir($dir)) {
            $results['LỖI'] = ["Không thấy thư mục $dir"]; 
        } else {
            $results['LỖI'] = ["Thư mục $dir trống"]; 
        }
    }
    
    // Xuất ra JSON phân cấp (Ví dụ: {"Disco": ["Disco/drum-120.wav"]})
    echo json_encode($results);
    exit;
}

// =========================================================
// TẢI TRỰC TIẾP FILE ÂM THANH TRỐNG QUA PHP
// =========================================================
if ($action === 'download_drum_sound') {
    $filename = isset($_GET['file']) ? $_GET['file'] : '';
    
    // Bảo vệ hệ thống: Lọc bỏ các truy xuất ngược thư mục nguy hiểm (../)
    $filename = str_replace(['../', '..\\'], '', $filename);
    $filepath = 'C:/AI_Music/DRUM/' . $filename;

    if (!empty($filename) && file_exists($filepath)) {
        if (ob_get_length()) { ob_clean(); }
        flush();
        
        $ext = strtolower(pathinfo($filepath, PATHINFO_EXTENSION));
        $contentType = ($ext === 'mp3') ? 'audio/mpeg' : 'audio/wav';
        
        header('Content-Type: ' . $contentType);
        header('Content-Length: ' . filesize($filepath));
        readfile($filepath);
        exit;
    } else {
        http_response_code(404);
        echo "File không tồn tại tại: " . $filepath;
        exit;
    }
}

// Nếu không khớp action nào ở trên
echo json_encode(["error" => "Hành động không hợp lệ"]);
?>