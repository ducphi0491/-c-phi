package duc_phi.music

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.min
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ===== Merged from MeloWareLaptopClient.kt (no behavioral changes) =====
/** An audio item discovered in a folder explicitly selected in Chrome on the laptop. */
internal data class LaptopAudioEntry(
    val name: String,
    val isFolder: Boolean,
    val path: List<String>,
    val size: Long = 0L,
    val mime: String = "audio/*",
    val fileId: String = ""
)

/**
 * MeloWare phone-hosted LAN browser bridge (NO PHP / INTERNET / PC app / SMB).
 *
 * Android creates a short link: http://PHONE_LAN_IP:port (no suffix in the URL).
 * PC Chrome opens this page, explicitly selects a directory via <input webkitdirectory>.
 * Chrome POSTs names/sizes only; when Android plays/downloads, Chrome serves requested
 * File.slice() byte ranges through authenticated same-origin HTTP requests to Android.
 * Media3 reads a separate authenticated loopback endpoint at 127.0.0.1.
 * This independent server does not modify the existing phone-to-phone HTTP server.
 */
internal class MeloWareLaptopClient(private val context: Context) {
    /**
     * API 23-compatible replacement for CompletableFuture (which requires API 24).
     * Each browser read job has exactly one waiter and can finish only once.
     */
    private class ReadCompletion {
        private val finished = CountDownLatch(1)
        private val resolved = AtomicBoolean(false)
        @Volatile private var bytes: ByteArray? = null
        @Volatile private var failure: Throwable? = null

        fun complete(data: ByteArray): Boolean {
            if (!resolved.compareAndSet(false, true)) return false
            bytes = data
            finished.countDown()
            return true
        }

        fun completeExceptionally(error: Throwable): Boolean {
            if (!resolved.compareAndSet(false, true)) return false
            failure = error
            finished.countDown()
            return true
        }

        fun get(timeout: Long, unit: TimeUnit): ByteArray {
            if (!finished.await(timeout, unit)) {
                throw TimeoutException("Chrome khong phan hoi yeu cau doc file")
            }
            failure?.let { throw it }
            return bytes ?: throw IOException("Khong nhan duoc du lieu tu Chrome")
        }
    }

    private data class ReadJob(
        val id: String,
        val fileId: String,
        val offset: Long,
        val length: Int,
        val result: ReadCompletion = ReadCompletion()
    )
    private data class Request(val method: String, val path: String, val headers: Map<String, String>, val input: InputStream)

    private val main = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val io = Executors.newFixedThreadPool(12)
    private val catalog = ConcurrentHashMap<String, LaptopAudioEntry>()
    private val knownTracks = ConcurrentHashMap<String, LaptopAudioEntry>()
    private val readJobs = ConcurrentHashMap<String, ReadJob>()
    private val readQueue = LinkedBlockingQueue<ReadJob>(100)
    private val random = SecureRandom()
    private var serverSocket: ServerSocket? = null
    private var port: Int = 0
    private var sessionToken: String = ""
    private var pageOffset = 0
    @Volatile private var lastBrowserSeenAt = 0L
    @Volatile private var closed = false

    var state by mutableStateOf("idle")
        private set
    var message by mutableStateOf("Nhấn Tạo link, sau đó mở link trên Chrome máy tính cùng Wi-Fi.")
        private set
    var shareUrl by mutableStateOf("")
        private set
    val entries = mutableStateListOf<LaptopAudioEntry>()
    var folder by mutableStateOf<List<String>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var moreAvailable by mutableStateOf(false)
        private set
    var downloading by mutableStateOf<String?>(null)
        private set
    var downloadProgress by mutableIntStateOf(0)
        private set
    val connected: Boolean get() = state == "connected" &&
        System.currentTimeMillis() - lastBrowserSeenAt < 35_000L
    val isRunning: Boolean get() = serverSocket?.isClosed == false

    /** User initiates; no local network listener is started until this action. */
    fun startShare() {
        if (closed || isRunning || state == "starting") return
        state = "starting"
        message = "Đang tạo đường dẫn Wi-Fi nội bộ..."
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val ip = findLanIpv4Address()
                        ?: throw IOException("Không có IP Wi-Fi/LAN nội bộ. Hãy kết nối cả hai thiết bị cùng mạng.")
                    // Keep the address easy to type: prefer port 8080, fallback if occupied.
                    val bindAddress = InetAddress.getByName("0.0.0.0")
                    val listener = try {
                        ServerSocket(8080, 36, bindAddress)
                    } catch (busy: java.net.BindException) {
                        Log.w(TAG, "Port 8080 is in use, choosing a free port", busy)
                        ServerSocket(0, 36, bindAddress)
                    }
                    // Session token remains internal to the browser API; it is NOT in the visible link.
                    val secretBytes = ByteArray(12).also(random::nextBytes)
                    val secret = secretBytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    Triple(listener, secret, "http://$ip:${listener.localPort}")
                }
            }
            result.onSuccess { (listener, secret, url) ->
                if (closed) { listener.close(); return@onSuccess }
                serverSocket = listener
                port = listener.localPort
                sessionToken = secret
                lastBrowserSeenAt = 0L
                catalog.clear()
                knownTracks.clear()
                entries.clear()
                folder = emptyList()
                shareUrl = url
                state = "waiting"
                message = "Trên laptop, mở link này bằng Chrome rồi chọn thư mục nhạc."
                io.execute { acceptLoop(listener) }
                Log.i(TAG, "LAN browser bridge started on port=$port")
            }.onFailure { error ->
                state = "error"
                message = error.localizedMessage ?: "Không tạo được link Wi-Fi."
                Log.e(TAG, "startShare", error)
            }
        }
    }

    private fun findLanIpv4Address(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
        return Collections.list(interfaces)
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { nic -> Collections.list(nic.inetAddresses)
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
                .map { nic.name.orEmpty().lowercase(Locale.ROOT) to it.hostAddress } }
            .sortedByDescending { (name, _) ->
                if (name.contains("wlan") || name.contains("wifi") || name.startsWith("ap")) 3
                else if (name.contains("eth")) 2 else 1
            }
            .firstOrNull()?.second
    }

    private fun acceptLoop(listener: ServerSocket) {
        while (!closed && !listener.isClosed) {
            try {
                val socket = listener.accept()
                io.execute { handle(socket) }
            } catch (error: Exception) {
                if (!listener.isClosed) Log.e(TAG, "acceptLoop", error)
                break
            }
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            try {
                client.soTimeout = 45_000
                val req = parseRequest(client) ?: return
                val secret = sessionToken
                val path = req.path.substringBefore('?')
                if (secret.isBlank()) {
                    respond(client, "403 Forbidden", "text/plain", "Forbidden".toByteArray()); return
                }
                // Chrome opens the bare IP:PORT. Also accept the old page URL as a fallback.
                if (req.method == "GET" && (path == "/" || path == "/pc" || path == "/pc/$secret")) {
                    val html = MeloWareLaptopPage.HTML.replace("__TOKEN__", secret)
                    respond(client, "200 OK", "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
                    return
                }
                // Keep internal API/read routes as before to avoid affecting playback/download.
                if (!path.endsWith("/$secret") && !path.contains("/$secret/")) {
                    respond(client, "403 Forbidden", "text/plain", "Forbidden".toByteArray()); return
                }
                when {
                    req.method == "GET" && path == "/api/poll/$secret" -> {
                        markBrowserAlive()
                        val job = readQueue.poll(8, TimeUnit.SECONDS)
                        if (job == null) respond(client, "204 No Content", "text/plain", ByteArray(0))
                        else if (!readJobs.containsKey(job.id)) respond(client, "204 No Content", "text/plain", ByteArray(0))
                        else respondJson(client, "200 OK", JSONObject().apply {
                            put("id", job.id); put("fileId", job.fileId)
                            put("offset", job.offset); put("length", job.length)
                        })
                    }
                    req.method == "POST" && path == "/api/catalog/$secret" -> {
                        ensureBrowserOrigin(req)
                        val data = readBody(req, MAX_JSON_BYTES)
                        registerCatalog(JSONObject(String(data, Charsets.UTF_8)))
                        respondJson(client, "200 OK", JSONObject().put("ok", true))
                    }
                    req.method == "POST" && path.startsWith("/api/push/$secret/") -> {
                        ensureBrowserOrigin(req)
                        val jobId = path.removePrefix("/api/push/$secret/")
                        val job = readJobs[jobId]
                        if (job == null) { respond(client, "410 Gone", "text/plain", ByteArray(0)); return }
                        val data = readBody(req, CHUNK_BYTES)
                        if (data.size != job.length) throw IOException("Byte count does not match request")
                        job.result.complete(data)
                        respond(client, "200 OK", "text/plain", ByteArray(0))
                    }
                    req.method == "POST" && path.startsWith("/api/fail/$secret/") -> {
                        ensureBrowserOrigin(req)
                        val jobId = path.removePrefix("/api/fail/$secret/")
                        val error = JSONObject(String(readBody(req, 2048), Charsets.UTF_8)).optString("error", "Chrome cannot read selected file")
                        readJobs[jobId]?.result?.completeExceptionally(IOException(error.take(200)))
                        respond(client, "200 OK", "text/plain", ByteArray(0))
                    }
                    req.method == "POST" && path == "/api/bye/$secret" -> {
                        ensureBrowserOrigin(req)
                        lastBrowserSeenAt = 0L
                        closePending("Laptop closed Chrome tab")
                        main.post { if (!closed && isRunning) {
                            state = "waiting"
                            message = "Chrome đã đóng hoặc chuyển trang. Mở lại link và chọn thư mục để tiếp tục."
                        } }
                        respond(client, "200 OK", "text/plain", ByteArray(0))
                    }
                    (req.method == "GET" || req.method == "HEAD") && path.startsWith("/audio/$secret/") -> {
                        serveAudio(client, req, path.removePrefix("/audio/$secret/"))
                    }
                    else -> respond(client, "404 Not Found", "text/plain", "Not found".toByteArray())
                }
            } catch (error: Exception) {
                if (isRunning && !closed) Log.e(TAG, "PC LAN request failed", error)
                runCatching { respond(client, "500 Internal Server Error", "text/plain", (error.message ?: "Server error").take(120).toByteArray()) }
            }
        }
    }

    private fun parseRequest(client: Socket): Request? {
        val stream = BufferedInputStream(client.getInputStream(), 64 * 1024)
        val first = readLine(stream) ?: return null
        val parts = first.split(' ', limit = 3)
        if (parts.size < 3 || parts[0] !in listOf("GET", "HEAD", "POST")) throw IOException("Invalid HTTP request")
        val headers = HashMap<String, String>()
        repeat(70) {
            val line = readLine(stream) ?: throw IOException("Unexpected EOF in HTTP header")
            if (line.isEmpty()) return Request(parts[0], parts[1], headers, stream)
            val pos = line.indexOf(':')
            if (pos > 0) headers[line.take(pos).lowercase(Locale.ROOT)] = line.substring(pos + 1).trim()
        }
        throw IOException("Too many headers")
    }

    private fun readLine(input: InputStream): String? {
        val builder = StringBuilder()
        while (builder.length < 8192) {
            val ch = input.read()
            if (ch < 0) return if (builder.isEmpty()) null else throw IOException("Truncated HTTP line")
            if (ch == 10) return builder.toString().trimEnd('\r')
            builder.append(ch.toChar())
        }
        throw IOException("HTTP header line too large")
    }

    private fun ensureBrowserOrigin(req: Request) {
        // Prevent cross-origin HTML from abusing the local-network upload endpoints.
        val origin = req.headers["origin"] ?: return
        val host = req.headers["host"] ?: throw IOException("Missing host")
        if (origin != "http://$host") throw IOException("Cross-origin request denied")
    }

    private fun readBody(req: Request, maxBytes: Int): ByteArray {
        val length = req.headers["content-length"]?.toIntOrNull()
            ?: throw IOException("POST Content-Length required")
        if (length < 0 || length > maxBytes) throw IOException("Request too large: $length")
        val result = ByteArray(length)
        var pos = 0
        while (pos < length) {
            val n = req.input.read(result, pos, length - pos)
            if (n < 0) throw IOException("Truncated HTTP body")
            pos += n
        }
        return result
    }

    private fun registerCatalog(data: JSONObject) {
        if (data.optBoolean("reset")) {
            catalog.clear()
            knownTracks.clear()
            closePending("PC selected another folder")
            main.post {
                if (!closed) { entries.clear(); folder = emptyList(); state = "waiting"; message = "Đang đọc thư mục máy tính..." }
            }
        }
        val files = data.optJSONArray("files") ?: throw IOException("Missing files")
        if (files.length() > 170 || catalog.size + files.length() > MAX_FILES) throw IOException("Too many audio items")
        for (i in 0 until files.length()) {
            val file = files.optJSONObject(i) ?: continue
            val id = file.optString("id")
            val rawPath = file.optJSONArray("path") ?: continue
            if (!id.matches(Regex("t[0-9]{1,6}")) || rawPath.length() !in 1..25) continue
            val path = (0 until rawPath.length()).map { rawPath.optString(it) }
            val size = file.optLong("size", -1)
            if (size <= 0 || path.any { it.isBlank() || it == "." || it == ".." || it.length > 220 || it.contains('\\') }) continue
            val name = path.last()
            val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (extension !in AUDIO_EXTENSIONS) continue
            val mime = file.optString("mime", "audio/*").take(72)
            catalog[id] = LaptopAudioEntry(name, false, path, size, mime, id)
        }
        markBrowserAlive()
        if (data.optBoolean("done")) {
            main.post {
                if (!closed && isRunning) {
                    state = "connected"
                    message = "Đã nhận ${catalog.size} file nhạc từ Chrome. Chọn bài để nghe hoặc tải."
                    listDirectory(emptyList())
                }
            }
        }
    }

    private fun markBrowserAlive() { lastBrowserSeenAt = System.currentTimeMillis() }

    fun showRoot() = listDirectory(emptyList())
    fun openFolder(entry: LaptopAudioEntry) { if (entry.isFolder) listDirectory(entry.path) }
    fun upFolder() { if (folder.isNotEmpty()) listDirectory(folder.dropLast(1)) }

    /** Filter only direct children; page the result so a huge folder won't overload Compose. */
    fun listDirectory(path: List<String>, append: Boolean = false) {
        if (!connected) { message = "Chưa nhận thư viện từ Chrome máy tính."; return }
        if (!append) { folder = path; pageOffset = 0; entries.clear() }
        val next = LinkedHashMap<String, LaptopAudioEntry>()
        for (item in catalog.values) {
            if (item.path.size <= folder.size || item.path.take(folder.size) != folder) continue
            val level = item.path[folder.size]
            val childPath = folder + level
            val child = if (item.path.size == folder.size + 1) item
                        else LaptopAudioEntry(level, true, childPath)
            val key = childPath.joinToString("\u0000")
            if (!next.containsKey(key)) next[key] = child
        }
        val sorted = next.values.sortedWith(compareByDescending<LaptopAudioEntry> { it.isFolder }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        val first = pageOffset.coerceAtMost(sorted.size)
        val end = (first + 60).coerceAtMost(sorted.size)
        entries.addAll(sorted.subList(first, end))
        pageOffset = end
        moreAvailable = end < sorted.size
        loading = false
    }
    fun loadMore() { if (!loading && moreAvailable) listDirectory(folder, append = true) }

    fun streamUrl(entry: LaptopAudioEntry): Uri {
        require(!entry.isFolder && entry.fileId.isNotBlank())
        if (!isRunning) throw IOException("MeloWare LAN server is not running")
        val key = MessageDigest.getInstance("SHA-256")
            .digest(entry.fileId.toByteArray(Charsets.UTF_8) + entry.path.joinToString("/").toByteArray(Charsets.UTF_8))
            .take(16).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        knownTracks[key] = entry
        return Uri.parse("http://127.0.0.1:$port/audio/$sessionToken/$key")
    }

    private fun serveAudio(client: Socket, req: Request, key: String) {
        val entry = knownTracks[key]
        if (entry == null || !connected || entry.size <= 0L) {
            respond(client, "404 Not Found", "text/plain", ByteArray(0)); return
        }
        val range = req.headers["range"]
        var start = 0L
        var end = entry.size - 1L
        if (range != null) {
            val match = Regex("^bytes=(\\d*)-(\\d*)$", RegexOption.IGNORE_CASE).matchEntire(range)
            if (match == null) { respond416(client, entry.size); return }
            val a = match.groupValues[1]
            val b = match.groupValues[2]
            when {
                a.isNotBlank() -> {
                    start = a.toLongOrNull() ?: -1L
                    if (b.isNotBlank()) end = b.toLongOrNull() ?: -1L
                }
                b.isNotBlank() -> {
                    val suffix = b.toLongOrNull() ?: -1L
                    if (suffix <= 0) { respond416(client, entry.size); return }
                    start = (entry.size - suffix).coerceAtLeast(0L)
                }
                else -> { respond416(client, entry.size); return }
            }
        }
        if (start < 0 || start >= entry.size || end < start) { respond416(client, entry.size); return }
        end = min(end, entry.size - 1)
        val total = end - start + 1L
        val initialLength = min(CHUNK_BYTES.toLong(), total).toInt()
        // Fetch first chunk before sending 200/206. Errors can still return a real HTTP status.
        val initial = if (req.method == "HEAD") ByteArray(0) else requestChunk(entry, start, initialLength)
        val out = BufferedOutputStream(client.getOutputStream(), 64 * 1024)
        val mime = audioMime(entry)
        val status = if (range != null) "206 Partial Content" else "200 OK"
        val header = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $mime\r\n")
            append("Content-Length: $total\r\n")
            append("Accept-Ranges: bytes\r\n")
            if (range != null) append("Content-Range: bytes $start-$end/${entry.size}\r\n")
            append("Cache-Control: no-store\r\nConnection: close\r\n\r\n")
        }
        out.write(header.toByteArray(Charsets.US_ASCII)); out.flush()
        if (req.method == "HEAD") return
        out.write(initial)
        var cursor = start + initial.size
        while (cursor <= end && !closed && connected) {
            val length = min(CHUNK_BYTES.toLong(), end - cursor + 1L).toInt()
            val bytes = requestChunk(entry, cursor, length)
            out.write(bytes)
            cursor += bytes.size
            out.flush()
        }
        out.flush()
        if (cursor <= end) throw IOException("Browser disconnected while streaming")
    }

    private fun requestChunk(entry: LaptopAudioEntry, offset: Long, length: Int): ByteArray {
        val id = ByteArray(12).also(random::nextBytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val job = ReadJob(id, entry.fileId, offset, length)
        readJobs[id] = job
        try {
            if (!readQueue.offer(job, 3, TimeUnit.SECONDS)) throw IOException("Laptop requests queue full")
            return job.result.get(30, TimeUnit.SECONDS).also {
                if (it.size != length) throw IOException("Incomplete laptop audio chunk")
            }
        } catch (error: Exception) {
            Log.e(TAG, "Browser audio chunk failed: ${entry.name} at $offset", error)
            throw IOException("Chrome không phản hồi dữ liệu nhạc: ${error.message}", error)
        } finally { readJobs.remove(id) }
    }

    private fun respond416(client: Socket, size: Long) {
        val message = "HTTP/1.1 416 Range Not Satisfiable\r\nContent-Length: 0\r\nContent-Range: bytes */$size\r\nConnection: close\r\n\r\n"
        client.getOutputStream().write(message.toByteArray(Charsets.US_ASCII))
    }

    private fun respondJson(client: Socket, code: String, json: JSONObject) {
        respond(client, code, "application/json; charset=utf-8", json.toString().toByteArray(Charsets.UTF_8))
    }

    private fun respond(client: Socket, code: String, mime: String, data: ByteArray) {
        val out = client.getOutputStream()
        val header = "HTTP/1.1 $code\r\nContent-Type: $mime\r\nContent-Length: ${data.size}\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        if (data.isNotEmpty()) out.write(data)
        out.flush()
    }

    /** Called from the existing Player's download button, also after leaving the laptop dialog. */
    fun downloadPlayingUri(songUri: Uri, onSaved: () -> Unit): Boolean {
        val key = songUri.lastPathSegment ?: return false
        val entry = knownTracks[key] ?: return false
        if (!connected || downloading != null) {
            message = "Máy tính chưa kết nối hoặc đang tải bài khác."
            return false
        }
        download(entry, onSaved)
        return true
    }

    fun download(entry: LaptopAudioEntry, onSaved: () -> Unit) {
        if (!connected || entry.isFolder || downloading != null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            message = "Android 9 trở xuống cần cấp quyền lưu tệp trước."
            return
        }
        downloading = entry.name
        downloadProgress = 0
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { saveToMusic(entry) } }
            downloading = null
            result.onSuccess {
                downloadProgress = 100
                message = "Đã tải $it vào Music/meloware_laptop"
                onSaved()
            }.onFailure { error ->
                message = "Lỗi tải nhạc: ${error.localizedMessage ?: "Lỗi không xác định"}"
                Log.e(TAG, "download failure", error)
            }
        }
    }

    private fun audioMime(entry: LaptopAudioEntry): String {
        if (entry.mime.startsWith("audio/") && entry.mime != "audio/*") return entry.mime
        return when (entry.name.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "wav", "wave" -> "audio/wav"
            "m4a", "m4b" -> "audio/mp4"
            "aac" -> "audio/aac"
            "ogg", "oga" -> "audio/ogg"
            "opus" -> "audio/opus"
            "webm" -> "audio/webm"
            else -> "application/octet-stream"
        }
    }

    private fun saveToMusic(entry: LaptopAudioEntry): String {
        val original = entry.name.replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").take(150)
        val ext = original.substringAfterLast('.', "mp3").lowercase(Locale.ROOT)
        val base = original.substringBeforeLast('.', original).take(110)
        val suffix = System.currentTimeMillis().toString().takeLast(7)
        val name = "${base}_$suffix.$ext"
        val connection = URL(streamUrl(entry).toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000
        var created: Uri? = null
        var oldFile: File? = null
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) throw IOException("Local audio bridge HTTP ${connection.responseCode}")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val meta = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                    put(MediaStore.Audio.Media.MIME_TYPE, audioMime(entry))
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/meloware_laptop/")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                created = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, meta)
                    ?: throw IOException("MediaStore cannot create audio")
                resolver.openOutputStream(created, "w")?.use { dst ->
                    connection.inputStream.use { src -> copyWithProgress(src, dst, entry.size) }
                } ?: throw IOException("Cannot open MediaStore destination")
                resolver.update(created, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "meloware_laptop")
                if (!dir.exists() && !dir.mkdirs()) throw IOException("Cannot create ${dir.absolutePath}")
                val destination = File(dir, name)
                oldFile = destination
                connection.inputStream.use { src -> destination.outputStream().use { dst -> copyWithProgress(src, dst, entry.size) } }
                MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), null, null)
            }
            return name
        } catch (error: Exception) {
            created?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            oldFile?.let { runCatching { it.delete() } }
            throw error
        } finally { connection.disconnect() }
    }

    private fun copyWithProgress(input: InputStream, output: OutputStream, length: Long) {
        val data = ByteArray(64 * 1024)
        var done = 0L
        var last = -1
        while (true) {
            val n = input.read(data)
            if (n < 0) break
            output.write(data, 0, n)
            done += n
            if (length > 0) {
                val percent = (done * 100 / length).coerceIn(0, 100).toInt()
                if (percent != last) { last = percent; main.post { downloadProgress = percent } }
            }
        }
        if (length > 0 && done != length) throw IOException("Tải thiếu dữ liệu: $done/$length bytes")
        output.flush()
    }

    private fun closePending(reason: String) {
        readJobs.values.forEach { it.result.completeExceptionally(IOException(reason)) }
        readJobs.clear()
        readQueue.clear()
    }

    fun disconnect() {
        val listener = serverSocket
        serverSocket = null
        runCatching { listener?.close() }
        closePending("MeloWare laptop sharing stopped")
        catalog.clear(); knownTracks.clear()
        entries.clear(); folder = emptyList(); pageOffset = 0; moreAvailable = false
        sessionToken = ""; port = 0; shareUrl = ""; lastBrowserSeenAt = 0L
        state = "idle"; message = "Đã ngừng chia sẻ với máy tính. Nhấn Tạo link để bắt đầu lại."
    }

    fun release() {
        closed = true
        disconnect()
        scope.cancel()
        io.shutdownNow()
    }

    companion object {
        private const val TAG = "MeloWareLaptopLAN"
        private const val CHUNK_BYTES = 128 * 1024
        private const val MAX_JSON_BYTES = 512 * 1024
        private const val MAX_FILES = 15_000
        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "m4b", "aac", "flac", "wav", "wave", "ogg", "oga", "opus",
            "amr", "3gp", "webm", "wma", "aif", "aiff", "ac3", "ec3", "ac4", "mka"
        )
    }
}


// ===== Merged from MeloWareLaptopDialog.kt (no behavioral changes) =====
/** Works both from Home and the existing Wi-Fi sharing chooser. */
@Composable
internal fun MeloWareLaptopDialog(controller: MusicController, onDismiss: () -> Unit) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val laptop = controller.laptopShare
    var pendingPermissionEntry by remember { mutableStateOf<LaptopAudioEntry?>(null) }
    val saveEntry: (LaptopAudioEntry) -> Unit = { item ->
        laptop.download(item) { controller.scanLocalMusic() }
    }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        val pending = pendingPermissionEntry
        pendingPermissionEntry = null
        if (allowed && pending != null) saveEntry(pending)
    }
    val requestDownload: (LaptopAudioEntry) -> Unit = { item ->
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingPermissionEntry = item
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else saveEntry(item)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.48f))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Computer, null, tint = colors.purpleBright)
                    Spacer(Modifier.width(9.dp))
                    Text("Máy tính · Chrome", modifier = Modifier.weight(1f),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Đóng", tint = colors.textSecondary) }
                }
                Text("1. Nhấn Tạo link trên điện thoại.\n2. Mở link này bằng Chrome trên laptop cùng Wi-Fi.\n3. Trên Chrome nhấn Chọn thư mục nhạc. MeloWare sẽ hiển thị danh sách để nghe online hoặc tải về.",
                    color = colors.textSecondary, fontSize = 12.sp)
                if (!laptop.isRunning) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = laptop::startShare,
                        enabled = laptop.state != "starting",
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) { Text(if (laptop.state == "starting") "Đang tạo link..." else "Tạo link MeloWare trên điện thoại") }
                } else {
                    OutlinedTextField(
                        value = laptop.shareUrl, onValueChange = {},
                        modifier = Modifier.fillMaxWidth(), readOnly = true,
                        label = { Text("Địa chỉ mở trên Chrome laptop", fontSize = 12.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare laptop URL", laptop.shareUrl))
                                Toast.makeText(context, "Đã sao chép link MeloWare", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) { Text("Sao chép link", fontSize = 12.sp) }
                        OutlinedButton(modifier = Modifier.weight(1f), onClick = laptop::disconnect) {
                            Text("Dừng chia sẻ", fontSize = 12.sp, color = colors.textPrimary)
                        }
                    }
                }
                if (laptop.connected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = laptop::upFolder, enabled = laptop.folder.isNotEmpty()) {
                            Icon(Icons.Rounded.ArrowBack, "Thư mục trước", tint = colors.purpleBright)
                        }
                        Text(if (laptop.folder.isEmpty()) "Thư mục nhạc" else laptop.folder.joinToString(" / "),
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        TextButton(onClick = laptop::showRoot) { Text("Gốc", color = colors.purpleBright) }
                    }
                    if (laptop.loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = colors.purpleBright)
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 375.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (laptop.entries.isEmpty() && !laptop.loading) item {
                            Text("Không có nhạc trong thư mục này.", fontSize = 12.sp, color = colors.textSecondary)
                        }
                        items(laptop.entries, key = { it.path.joinToString("\u0000") }) { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().then(if (item.isFolder) Modifier.clickable { laptop.openFolder(item) } else Modifier),
                                color = colors.backgroundSoft,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(Modifier.padding(horizontal = 9.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (item.isFolder) Icons.Rounded.Folder else Icons.Rounded.MusicNote,
                                        null, tint = colors.purpleBright, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            color = colors.textPrimary)
                                        if (!item.isFolder) Text("${item.size / 1024 / 1024} MB", fontSize = 10.sp, color = colors.textSecondary)
                                    }
                                    if (item.isFolder) {
                                        Icon(Icons.Rounded.PlayArrow, null, tint = colors.textSecondary)
                                    } else {
                                        IconButton(onClick = {
                                            controller.playLaptopFile(item)
                                            onDismiss()
                                        }, modifier = Modifier.size(38.dp)) {
                                            Icon(Icons.Rounded.PlayArrow, "Nghe online", tint = colors.purpleBright)
                                        }
                                        IconButton(onClick = { requestDownload(item) },
                                            modifier = Modifier.size(38.dp), enabled = laptop.downloading == null) {
                                            Icon(Icons.Rounded.Download, "Tải vào meloware_laptop", tint = colors.purpleBright)
                                        }
                                    }
                                }
                            }
                        }
                        if (laptop.moreAvailable) item {
                            TextButton(onClick = laptop::loadMore, enabled = !laptop.loading,
                                modifier = Modifier.fillMaxWidth()) { Text("Tải thêm bài hát", color = colors.purpleBright) }
                        }
                    }
                    laptop.downloading?.let { name ->
                        Text("Đang tải $name: ${laptop.downloadProgress}%", fontSize = 12.sp, color = colors.purpleBright)
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = laptop.downloadProgress / 100f, modifier = Modifier.fillMaxWidth())
                    }
                }
                // No explanatory notes under the buttons; only surface actionable errors.
                if (laptop.state == "error" || laptop.message.startsWith("Lỗi") ||
                    laptop.message.startsWith("Android 9") || laptop.message.startsWith("Máy tính chưa")) {
                    Text(laptop.message, color = Color(0xFFF09B9B), fontSize = 11.sp, maxLines = 3)
                }
            }
        }
    }
}


// ===== Merged from MeloWareLaptopPage.kt (no behavioral changes) =====
/** Self-contained browser page served by MeloWare over local Wi-Fi. No hosting required. */
internal object MeloWareLaptopPage {
    val HTML: String = """
<!doctype html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>MeloWare | Chia sẻ nhạc cho điện thoại</title>
  <style>
    *{box-sizing:border-box}
    body{margin:0;background:#10121d;color:#f1f1fa;font:15px system-ui,-apple-system,Segoe UI,sans-serif;min-height:100vh;display:grid;place-items:center;padding:18px}
    main{width:min(600px,100%);padding:26px;background:#202234;border:1px solid #393a52;border-radius:22px;box-shadow:0 22px 85px #0005}
    h1{font-size:25px;margin:0 0 6px}p{line-height:1.7;color:#c5c6d6}
    .tag{display:inline-block;padding:5px 10px;border-radius:12px;background:#243b35;color:#79e0b1;font-size:12px;font-weight:700}
    .panel{background:#151726;border:1px solid #393a52;border-radius:14px;padding:15px;margin:16px 0}
    label.pick{display:flex;align-items:center;justify-content:center;background:#7459df;border-radius:12px;cursor:pointer;font-weight:700;padding:14px 12px;text-align:center}
    input[type=file]{position:absolute;opacity:0;left:-9999px;width:1px;height:1px}
    #status{white-space:pre-wrap;overflow-wrap:anywhere;margin:10px 0;color:#a9ebc9}
    #detail{color:#bfc1d1;font-size:13px;white-space:pre-wrap;overflow-wrap:anywhere}
    .warning{font-size:12px;color:#e8ce89}
    .small{font-size:12px;color:#9698aa}
  </style>
</head>
<body>
<main>
  <span class="tag">● Wi-Fi LAN · Không cần cài trên PC</span>
  <h1>MeloWare · Kết nối laptop</h1>
  <p>Điện thoại đang cung cấp trang này. Chọn thư mục nhạc trên máy tính để MeloWare có thể duyệt, nghe trực tuyến hoặc tải từng bài về điện thoại.</p>
  <section class="panel">
    <label class="pick" for="pick">📁 Chọn thư mục nhạc trên laptop</label>
    <input id="pick" type="file" webkitdirectory directory multiple accept="audio/*,.mp3,.flac,.wav,.m4a,.aac,.ogg,.opus,.wma,.aiff,.aif,.m4b,.3gp,.webm">
    <div id="status" role="status">Đã mở link MeloWare. Hãy chọn thư mục muốn chia sẻ.</div>
    <div id="detail"></div>
  </section>
  <p class="warning">Giữ tab Chrome này mở trong suốt lúc điện thoại nghe nhạc hoặc tải file. Không cần bật chia sẻ thư mục Windows, không tải toàn bộ nhạc lên Internet.</p>
  <p class="small">Chỉ những file bạn chọn qua hộp thoại mới được đọc. Dữ liệu truyền trực tiếp qua mạng LAN tới điện thoại MeloWare.</p>
</main>
<script>
'use strict';
(function() {
  const token = '__TOKEN__';
  const pick = document.getElementById('pick');
  const status = document.getElementById('status');
  const detail = document.getElementById('detail');
  const files = new Map();
  const audioExtensions = new Set(['mp3','m4a','m4b','aac','flac','wav','wave','ogg','oga','opus','amr','3gp','webm','wma','aif','aiff','ac3','ec3','ac4','mka']);
  let connected = false;
  let generation = 0;
  let processed = 0;
  let total = 0;
  function route(endpoint) { return '/api/' + endpoint + '/' + token; }
  function setStatus(s) { status.textContent = s; }
  function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }
  async function postJson(url, data) {
    const response = await fetch(url, {method:'POST', cache:'no-store', headers:{'Content-Type':'application/json'}, body:JSON.stringify(data)});
    if (!response.ok) throw new Error('HTTP ' + response.status + ': ' + await response.text());
    return response;
  }
  async function selectFolder() {
    const current = ++generation;
    files.clear(); connected = false;
    const selected = Array.from(pick.files || []);
    const all = [];
    for (const file of selected) {
      if (all.length >= 15000) break;
      const raw = String(file.webkitRelativePath || file.name);
      const parts = raw.split('/').filter(Boolean);
      // The chosen directory name is removed; only paths inside it are shown.
      const path = parts.length > 1 ? parts.slice(1) : [file.name];
      const suffix = file.name.split('.').pop().toLowerCase();
      if (!audioExtensions.has(suffix) || !file.size || !path.length || path.length > 25) continue;
      const id = 't' + all.length;
      files.set(id, file);
      all.push({id:id, path:path, size:file.size, mime:file.type || 'audio/*'});
    }
    if (!all.length) { setStatus('Thư mục này chưa có file âm thanh được hỗ trợ.'); detail.textContent = ''; return; }
    const firstName = selected.length ? String(selected[0].webkitRelativePath || '').split('/')[0] : '';
    setStatus('Đang gửi danh sách ' + all.length + ' bài hát tới MeloWare...');
    detail.textContent = 'Thư mục: ' + firstName + '\nChỉ gửi danh sách tên file, chưa truyền dữ liệu nhạc.';
    try {
      for (let i = 0; i < all.length; i += 150) {
        if (generation !== current) return;
        await postJson(route('catalog'), {reset:i === 0, done:i+150 >= all.length, files:all.slice(i,i+150)});
        setStatus('Đang lập thư viện: ' + Math.min(i+150,all.length) + ' / ' + all.length);
      }
      if (generation !== current) return;
      connected = true;
      processed = 0; total = all.length;
      setStatus('✓ Đã chia sẻ ' + all.length + ' bài hát. Mở MeloWare trên điện thoại để nghe hoặc tải.');
      detail.textContent = 'Thư mục: ' + firstName + '\nTab Chrome này phải tiếp tục mở.';
    } catch (error) {
      setStatus('Không thể gửi danh sách: ' + error.message + '. Kiểm tra kết nối Wi-Fi hoặc tạo link mới.');
    }
  }
  pick.addEventListener('change', selectFolder);
  async function pollLoop() {
    for (;;) {
      try {
        const reply = await fetch(route('poll'), {cache:'no-store'});
        if (reply.status === 204) continue;
        if (reply.status === 403 || reply.status === 404) {
          setStatus('Phiên MeloWare trên điện thoại đã kết thúc. Hãy lấy link mới.');
          return;
        }
        if (!reply.ok) throw new Error('poll HTTP ' + reply.status);
        const job = await reply.json();
        const file = files.get(job.fileId);
        if (!file || !connected) {
          await postJson(route('fail') + '/' + encodeURIComponent(job.id), {error:'File không còn trong thư mục đã chia sẻ'});
          continue;
        }
        const start = Number(job.offset), length = Number(job.length);
        if (!Number.isSafeInteger(start) || !Number.isSafeInteger(length) || start < 0 || length < 1 || length > 262144 || start+length > file.size) {
          await postJson(route('fail') + '/' + encodeURIComponent(job.id), {error:'Yêu cầu vùng dữ liệu không hợp lệ'});
          continue;
        }
        const piece = file.slice(start, start+length);
        const response = await fetch(route('push') + '/' + encodeURIComponent(job.id), {
          method:'POST', cache:'no-store', body:piece,
          headers:{'Content-Type':'application/octet-stream'}
        });
        if (!response.ok) throw new Error('Không gửi được dữ liệu (HTTP ' + response.status + ')');
        processed += length;
      } catch (error) {
        if (connected) detail.textContent = 'Đã chọn ' + total + ' file. Đang thử kết nối lại: ' + error.message;
        await sleep(1200);
      }
    }
  }
  // Two workers keep seek/play responsive while a download is running.
  pollLoop(); pollLoop();
  window.addEventListener('pagehide', function() {
    try { navigator.sendBeacon(route('bye'), ''); } catch (_) {}
  });
})();
</script>
</body>
</html>
""".trimIndent()
}
