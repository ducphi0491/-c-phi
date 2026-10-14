<?php
declare(strict_types=1);

/**
 * MeloWare Premium diagnostic monitor.
 * Public URL (password protected):
 *   https://tunertools.top/melowave/meloware_premium_monitor.php
 *
 * The page reads only the sanitized trace created by meloware_premium.php.
 * It never displays raw ID tokens, purchase tokens, Gmail addresses,
 * database passwords, or service-account private keys.
 */
@ini_set('display_errors', '0');
@ini_set('log_errors', '1');
error_reporting(E_ALL);

$traceFile = 'C:/Windows/Temp/MeloWare_Premium_trace.jsonl';
$monitorPassword = getenv('MELOWARE_MONITOR_PASSWORD') ?: 'w%Iv@ud88hMKUB%LkJbB';
$isHttps = (!empty($_SERVER['HTTPS']) && strtolower((string) $_SERVER['HTTPS']) !== 'off')
    || (int) ($_SERVER['SERVER_PORT'] ?? 0) === 443;

session_name('MW_PREMIUM_MONITOR');
session_set_cookie_params([
    'lifetime' => 0,
    'path' => '/',
    'secure' => $isHttps,
    'httponly' => true,
    'samesite' => 'Strict',
]);
session_start();

header('X-Content-Type-Options: nosniff');
header('X-Frame-Options: DENY');
header('Referrer-Policy: no-referrer');
header("Content-Security-Policy: default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
header('X-Robots-Tag: noindex, nofollow, noarchive');

if (isset($_GET['logout'])) {
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $params['path'], $params['domain'] ?? '', (bool) $params['secure'], (bool) $params['httponly']);
    }
    session_destroy();
    header('Location: ' . strtok((string) ($_SERVER['REQUEST_URI'] ?? ''), '?'));
    exit;
}

$loginError = '';
if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET')) === 'POST'
    && (string) ($_POST['action'] ?? '') === 'login') {
    $now = time();
    $lockedUntil = (int) ($_SESSION['mw_locked_until'] ?? 0);
    if ($lockedUntil > $now) {
        $loginError = 'Too many failed attempts. Please wait 60 seconds.';
    } elseif (hash_equals($monitorPassword, (string) ($_POST['password'] ?? ''))) {
        session_regenerate_id(true);
        $_SESSION['mw_monitor_ok'] = true;
        $_SESSION['mw_failed'] = 0;
        $_SESSION['mw_locked_until'] = 0;
        header('Location: ' . strtok((string) ($_SERVER['REQUEST_URI'] ?? ''), '?'));
        exit;
    } else {
        $failed = (int) ($_SESSION['mw_failed'] ?? 0) + 1;
        $_SESSION['mw_failed'] = $failed;
        if ($failed >= 5) {
            $_SESSION['mw_locked_until'] = $now + 60;
            $_SESSION['mw_failed'] = 0;
        }
        $loginError = 'Incorrect monitor password.';
    }
}

if (empty($_SESSION['mw_monitor_ok'])) {
    $isDataRequest = isset($_GET['data'])
        || (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET')) === 'POST'
            && (string) ($_POST['action'] ?? '') === 'test_write');
    if ($isDataRequest) {
        http_response_code(401);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        echo json_encode(['ok' => false, 'message' => 'Monitor login required.']);
        exit;
    }
    header('Content-Type: text/html; charset=utf-8');
    ?>
<!doctype html>
<html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MeloWare Premium Monitor - Login</title>
<style>body{margin:0;background:#0b1020;color:#eef2ff;font:15px Segoe UI,Arial,sans-serif;display:grid;place-items:center;min-height:100vh}.box{width:min(430px,calc(100% - 28px));background:#11182d;border:1px solid #293554;border-radius:16px;padding:24px;box-shadow:0 20px 60px #0007}h1{font-size:23px;margin:0 0 7px}.muted{color:#9eabc9;margin-bottom:20px}.err{background:#3a1721;border:1px solid #8a3345;color:#ffd4db;padding:10px;border-radius:10px;margin-bottom:12px}input,button{width:100%;padding:12px;border-radius:10px;border:1px solid #293554;background:#18213a;color:#fff;font:inherit}button{margin-top:10px;cursor:pointer;background:#315fbe;border-color:#4f7bd7;font-weight:700}.note{font-size:12px;color:#9eabc9;margin-top:14px;line-height:1.5}</style></head><body><form class="box" method="post" autocomplete="off"><h1>MeloWare Premium Monitor</h1><div class="muted">Dang nhap de xem luong Android - PHP - Google - Android.</div>
<?php if ($loginError !== ''): ?><div class="err"><?= htmlspecialchars($loginError, ENT_QUOTES, 'UTF-8') ?></div><?php endif; ?>
<input type="hidden" name="action" value="login"><input type="password" name="password" placeholder="Monitor password" required autofocus><button type="submit">Dang nhap</button><div class="note">Trang nay da cho phep mo qua ten mien tunertools.top, nhung duoc bao ve bang mat khau va session HTTPS.</div></form></body></html>
<?php
    exit;
}

if (empty($_SESSION['mw_csrf'])) {
    $_SESSION['mw_csrf'] = bin2hex(random_bytes(24));
}

function mw_monitor_json(array $payload, int $http = 200): never {
    http_response_code($http);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store, no-cache, must-revalidate, private');
    echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE);
    exit;
}

function mw_monitor_try_write(string $path, string $content, int $flags = 0): array {
    $warning = '';
    set_error_handler(static function (int $severity, string $message) use (&$warning): bool {
        $warning = $message;
        return true;
    });
    try {
        $result = file_put_contents($path, $content, $flags);
    } finally {
        restore_error_handler();
    }
    return ['ok' => $result !== false, 'bytes' => $result === false ? 0 : (int) $result, 'error' => $warning];
}

function mw_monitor_trace_status(string $path, bool $createIfMissing = true): array {
    $directory = dirname($path);
    $createError = '';
    $createdNow = false;
    if ($createIfMissing && !is_file($path)) {
        if (!is_dir($directory)) {
            $warning = '';
            set_error_handler(static function (int $severity, string $message) use (&$warning): bool {
                $warning = $message;
                return true;
            });
            try {
                $made = mkdir($directory, 0770, true);
            } finally {
                restore_error_handler();
            }
            if (!$made && !is_dir($directory)) $createError = $warning !== '' ? $warning : 'Could not create trace directory.';
        }
        if ($createError === '' && is_dir($directory)) {
            $probe = mw_monitor_try_write($path, '', 0);
            if (!$probe['ok']) $createError = (string) $probe['error'];
            else $createdNow = true;
        }
    }
    clearstatcache(true, $path);
    return [
        'path' => $path,
        'directory' => $directory,
        'directoryExists' => is_dir($directory),
        'directoryWritable' => is_dir($directory) && is_writable($directory),
        'exists' => is_file($path),
        'readable' => is_file($path) && is_readable($path),
        'writable' => is_file($path) && is_writable($path),
        'size' => is_file($path) ? (int) @filesize($path) : 0,
        'modifiedUtc' => is_file($path) ? gmdate('c', (int) @filemtime($path)) : null,
        'createdNow' => $createdNow,
        'error' => $createError,
    ];
}

function mw_monitor_events(string $path, int $limit = 700): array {
    if (!is_file($path) || !is_readable($path)) return [];
    $lines = @file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    if (!is_array($lines)) return [];
    $lines = array_slice($lines, -max(20, min(2000, $limit)));
    $events = [];
    foreach ($lines as $line) {
        $row = json_decode((string) $line, true);
        if (is_array($row) && isset($row['event'], $row['requestId'])) $events[] = $row;
    }
    return $events;
}

if (strtoupper((string) ($_SERVER['REQUEST_METHOD'] ?? 'GET')) === 'POST'
    && (string) ($_POST['action'] ?? '') === 'test_write') {
    if (!hash_equals((string) ($_SESSION['mw_csrf'] ?? ''), (string) ($_POST['csrf'] ?? ''))) {
        mw_monitor_json(['ok' => false, 'message' => 'Invalid CSRF token.'], 403);
    }
    $status = mw_monitor_trace_status($traceFile, true);
    if (empty($status['exists']) || empty($status['writable'])) {
        mw_monitor_json(['ok' => false, 'message' => 'Trace file is not writable.', 'traceStatus' => $status], 500);
    }
    $row = [
        'timeUtc' => gmdate('c'),
        'requestId' => 'monitor-' . bin2hex(random_bytes(8)),
        'action' => 'monitor',
        'event' => 'monitor_test_write',
        'details' => ['message' => 'Monitor write test succeeded.'],
    ];
    $write = mw_monitor_try_write(
        $traceFile,
        json_encode($row, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_INVALID_UTF8_SUBSTITUTE) . PHP_EOL,
        FILE_APPEND | LOCK_EX
    );
    clearstatcache(true, $traceFile);
    mw_monitor_json([
        'ok' => (bool) $write['ok'],
        'message' => $write['ok'] ? 'Trace write test succeeded.' : 'Trace write test failed.',
        'writeError' => (string) $write['error'],
        'traceStatus' => mw_monitor_trace_status($traceFile, false),
    ], $write['ok'] ? 200 : 500);
}

if (isset($_GET['data'])) {
    $status = mw_monitor_trace_status($traceFile, true);
    mw_monitor_json([
        'ok' => true,
        'serverTimeUtc' => gmdate('c'),
        'traceFileExists' => (bool) $status['exists'],
        'traceFileSize' => (int) $status['size'],
        'traceStatus' => $status,
        'events' => mw_monitor_events($traceFile),
    ]);
}
?>
<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>MeloWare Premium Monitor</title>
<style>
:root{color-scheme:dark;--bg:#0b1020;--panel:#11182d;--soft:#18213a;--line:#293554;--text:#eef2ff;--muted:#9eabc9;--ok:#48d597;--warn:#f4c35b;--bad:#ff7184;--blue:#79a8ff}
*{box-sizing:border-box}body{margin:0;background:linear-gradient(145deg,#080c18,#10172b 48%,#0b1020);color:var(--text);font:14px/1.45 Segoe UI,Arial,sans-serif;min-height:100vh}.wrap{max-width:1400px;margin:auto;padding:22px}.top{display:flex;gap:14px;align-items:center;justify-content:space-between;flex-wrap:wrap}.title{font-size:25px;font-weight:800}.subtitle{color:var(--muted);margin-top:4px}.toolbar{display:flex;gap:8px;flex-wrap:wrap}.btn,.input{border:1px solid var(--line);background:var(--soft);color:var(--text);border-radius:10px;padding:9px 12px}.btn{cursor:pointer;text-decoration:none;display:inline-flex;align-items:center;justify-content:center}.btn:hover{border-color:var(--blue)}.input{min-width:260px}.stats{display:grid;grid-template-columns:repeat(5,minmax(120px,1fr));gap:10px;margin:18px 0}.stat{background:rgba(17,24,45,.9);border:1px solid var(--line);border-radius:14px;padding:13px}.stat b{font-size:21px;display:block}.stat span{color:var(--muted);font-size:12px}.notice{background:rgba(121,168,255,.09);border:1px solid rgba(121,168,255,.35);padding:12px 14px;border-radius:12px;color:#cbd9ff;margin-bottom:14px}.notice.ok{background:rgba(72,213,151,.08);border-color:rgba(72,213,151,.45);color:#c9f7e3}.notice.bad{background:rgba(255,113,132,.09);border-color:rgba(255,113,132,.55);color:#ffd5dc}.mono{font-family:Consolas,monospace;word-break:break-all}.requests{display:grid;gap:12px}.card{background:rgba(17,24,45,.94);border:1px solid var(--line);border-radius:15px;overflow:hidden}.card.error{border-color:rgba(255,113,132,.65)}.head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start;padding:14px 15px;background:rgba(255,255,255,.018)}.rid{font-family:Consolas,monospace;font-weight:700;word-break:break-all}.time{color:var(--muted);white-space:nowrap}.flow{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:12px 15px}.step{border:1px solid var(--line);border-radius:11px;padding:10px;min-height:82px;background:#0d1427}.step .label{font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.55px}.step .value{font-weight:700;margin-top:5px;word-break:break-word}.step.ok{border-color:rgba(72,213,151,.55)}.step.bad{border-color:rgba(255,113,132,.65)}.step.warn{border-color:rgba(244,195,91,.55)}.details{border-top:1px solid var(--line);padding:0 15px 12px}details{margin-top:10px}summary{cursor:pointer;color:#bdc9e9}.event{font-family:Consolas,monospace;background:#0a1020;border:1px solid #202b47;border-radius:9px;margin-top:7px;padding:9px;white-space:pre-wrap;word-break:break-word;color:#c8d2ec}.chip{display:inline-flex;align-items:center;padding:3px 8px;border-radius:999px;font-size:11px;border:1px solid var(--line);color:var(--muted)}.chip.ok{color:var(--ok);border-color:rgba(72,213,151,.45)}.chip.bad{color:var(--bad);border-color:rgba(255,113,132,.45)}.empty{padding:34px;text-align:center;color:var(--muted);border:1px dashed var(--line);border-radius:14px}.footer{color:var(--muted);font-size:12px;margin-top:16px}.hidden{display:none!important}@media(max-width:900px){.stats{grid-template-columns:repeat(2,1fr)}.flow{grid-template-columns:1fr 1fr}}@media(max-width:560px){.wrap{padding:12px}.flow{grid-template-columns:1fr}.input{min-width:100%}}
</style>
</head>
<body>
<div class="wrap">
  <div class="top">
    <div><div class="title">MeloWare Premium Monitor</div><div class="subtitle">Theo dõi Android → PHP → Google/DB → PHP → Android ACK</div></div>
    <div class="toolbar">
      <input id="filter" class="input" placeholder="Lọc request ID, action, lỗi...">
      <button id="errorsOnly" class="btn">Chỉ lỗi: Tắt</button>
      <button id="pause" class="btn">Tạm dừng</button>
      <button id="refresh" class="btn">Làm mới</button>
      <button id="testWrite" class="btn">Kiem tra ghi file</button>
      <a class="btn" href="?logout=1">Dang xuat</a>
    </div>
  </div>
  <div class="stats">
    <div class="stat"><b id="requestCount">0</b><span>Request gần nhất</span></div>
    <div class="stat"><b id="receivedCount">0</b><span>PHP đã nhận</span></div>
    <div class="stat"><b id="responseCount">0</b><span>PHP đã trả</span></div>
    <div class="stat"><b id="ackCount">0</b><span>Android đã ACK</span></div>
    <div class="stat"><b id="errorCount">0</b><span>Có lỗi</span></div>
  </div>
  <div class="notice"><b>Cách đọc:</b> “PHP đã trả” chứng minh máy chủ đã phát JSON. “Android ACK” chỉ chuyển xanh khi ứng dụng đã nhận và đọc được phản hồi đó. Trang không hiển thị token, Gmail hay khóa bí mật.</div>
  <div id="traceStatus" class="notice">Dang kiem tra file trace...</div>
  <div id="requests" class="requests"><div class="empty">Đang tải dữ liệu...</div></div>
  <div id="footer" class="footer"></div>
</div>
<script>
let paused=false, errorsOnly=false, lastData=null;
const csrf=<?= json_encode((string) $_SESSION['mw_csrf'], JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT) ?>;
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
const fmt=t=>{try{return new Date(t).toLocaleString('vi-VN')}catch{return t||''}};
const latest=(events,name)=>[...events].reverse().find(e=>e.event===name);
const has=(events,name)=>events.some(e=>e.event===name);
const cls=(ok,bad=false)=>bad?'bad':ok?'ok':'warn';
function render(data){
 lastData=data;
 const st=data.traceStatus||{};
 const traceBox=document.getElementById('traceStatus');
 const traceOk=!!st.exists&&!!st.readable&&!!st.writable;
 traceBox.className='notice '+(traceOk?'ok':'bad');
 traceBox.innerHTML=traceOk
   ? `<b>File trace san sang.</b> <span class="mono">${esc(st.path||'')}</span> · ${Math.round((st.size||0)/1024)} KB`
   : `<b>File trace chua san sang.</b> <span class="mono">${esc(st.path||'')}</span><br>Thu muc ton tai: ${st.directoryExists?'co':'khong'} · quyen ghi thu muc: ${st.directoryWritable?'co':'khong'} · file ton tai: ${st.exists?'co':'khong'} · quyen doc/ghi: ${(st.readable&&st.writable)?'co':'khong'}${st.error?'<br><b>Loi:</b> '+esc(st.error):''}`;
 const groups=new Map();
 for(const e of data.events||[]){const id=e.requestId||'unknown';if(!groups.has(id))groups.set(id,[]);groups.get(id).push(e)}
 let rows=[...groups.entries()].map(([id,events])=>({id,events,last:events[events.length-1]?.timeUtc||'',action:events.find(e=>e.action)?.action||''})).sort((a,b)=>b.last.localeCompare(a.last));
 const q=document.getElementById('filter').value.trim().toLowerCase();
 rows=rows.filter(r=>{const txt=JSON.stringify(r).toLowerCase();return(!q||txt.includes(q))&&(!errorsOnly||has(r.events,'backend_exception')||((latest(r.events,'php_response_sent')?.details?.http||200)>=400))});
 let received=0,response=0,ack=0,errors=0;
 for(const r of [...groups.values()]){if(has(r,'android_request_received'))received++;if(has(r,'php_response_sent'))response++;if(has(r,'android_response_ack'))ack++;if(has(r,'backend_exception')||((latest(r,'php_response_sent')?.details?.http||200)>=400))errors++}
 document.getElementById('requestCount').textContent=groups.size;document.getElementById('receivedCount').textContent=received;document.getElementById('responseCount').textContent=response;document.getElementById('ackCount').textContent=ack;document.getElementById('errorCount').textContent=errors;
 const root=document.getElementById('requests');
 if(!rows.length){root.innerHTML='<div class="empty">Chưa có request phù hợp. Mở MeloWare Premium rồi nhấn “Thử lại”.</div>';return}
 root.innerHTML=rows.map(r=>{
   const req=latest(r.events,'android_request_received');const google=latest(r.events,'google_response_received');const resp=latest(r.events,'php_response_sent');const ackEv=latest(r.events,'android_response_ack');const ex=latest(r.events,'backend_exception');
   const http=resp?.details?.http??0;const googleHttp=google?.details?.http??0;const bad=!!ex||http>=400;
   const reqText=req?`Đã nhận ${req.details?.contentLength??0} byte<br><span class="chip">ID token: ${req.details?.idTokenPresent?'có':'không'} (${req.details?.idTokenLength??0})</span>`:'Chưa thấy request';
   const googleText=google?`${esc(google.details?.endpoint||'Google')} · HTTP ${googleHttp}<br>${esc(google.details?.googleError||google.details?.curlError||'Phản hồi hợp lệ')}`:'Chưa gọi/Chưa tới bước Google';
   const respText=resp?`HTTP ${http} · ${resp.details?.ok?'ok=true':'ok=false'}<br>${esc(resp.details?.message||resp.details?.checkoutReason||'Đã trả JSON')}`:'Chưa trả phản hồi';
   const ackText=ackEv?`Android đã nhận<br>HTTP ${ackEv.details?.receivedHttp??0} · JSON ${ackEv.details?.androidParsedJson?'OK':'LỖI'}`:'Chưa có ACK từ Android';
   return `<div class="card ${bad?'error':''}"><div class="head"><div><span class="chip ${bad?'bad':'ok'}">${esc(r.action||'unknown')}</span><div class="rid">${esc(r.id)}</div></div><div class="time">${fmt(r.last)}</div></div><div class="flow"><div class="step ${cls(!!req)}"><div class="label">1. Android → PHP</div><div class="value">${reqText}</div></div><div class="step ${cls(googleHttp>=200&&googleHttp<300,googleHttp>=400)}"><div class="label">2. PHP → Google</div><div class="value">${googleText}</div></div><div class="step ${cls(!!resp&&!bad,bad)}"><div class="label">3. PHP → Android</div><div class="value">${respText}</div></div><div class="step ${cls(!!ackEv)}"><div class="label">4. Android ACK</div><div class="value">${ackText}</div></div></div><div class="details"><details><summary>Chi tiết ${r.events.length} sự kiện${ex?' · Lỗi: '+esc(ex.details?.message||ex.details?.category||''):''}</summary>${r.events.map(e=>`<div class="event">${esc(e.timeUtc)}  ${esc(e.event)}\n${esc(JSON.stringify(e.details||{},null,2))}</div>`).join('')}</details></div></div>`
 }).join('');
 document.getElementById('footer').textContent=`Cap nhat: ${fmt(data.serverTimeUtc)} · Truy cap qua ${location.host} · File trace: ${data.traceFileExists?'da tao':'chua co'} · ${Math.round((data.traceFileSize||0)/1024)} KB`;
}
async function load(){
 if(paused)return;
 try{
   const r=await fetch('?data=1&_='+Date.now(),{cache:'no-store',credentials:'same-origin'});
   if(r.status===401||r.status===403){location.reload();return}
   const data=await r.json();
   render(data);
 }catch(e){document.getElementById('footer').textContent='Khong tai duoc du lieu: '+e}
}
document.getElementById('pause').onclick=()=>{paused=!paused;document.getElementById('pause').textContent=paused?'Tiếp tục':'Tạm dừng';if(!paused)load()};
document.getElementById('refresh').onclick=load;document.getElementById('filter').oninput=()=>lastData&&render(lastData);document.getElementById('errorsOnly').onclick=()=>{errorsOnly=!errorsOnly;document.getElementById('errorsOnly').textContent='Chỉ lỗi: '+(errorsOnly?'Bật':'Tắt');lastData&&render(lastData)};
document.getElementById('testWrite').onclick=async()=>{
 const btn=document.getElementById('testWrite');btn.disabled=true;btn.textContent='Dang kiem tra...';
 try{
   const body=new URLSearchParams({action:'test_write',csrf});
   const r=await fetch(location.pathname,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body,credentials:'same-origin',cache:'no-store'});
   const data=await r.json();
   alert((data.ok?'OK: ':'LOI: ')+(data.message||'')+(data.writeError?'\n'+data.writeError:''));
   await load();
 }catch(e){alert('Khong kiem tra duoc file trace: '+e)}finally{btn.disabled=false;btn.textContent='Kiem tra ghi file'}
};
load();setInterval(load,1600);
</script>
</body>
</html>
