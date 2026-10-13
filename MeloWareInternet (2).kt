package duc_phi.music

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContentValues
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.Manifest
import android.content.pm.PackageManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.ArrayDeque
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.nio.ByteBuffer
import java.security.SecureRandom
import livekit.org.webrtc.DataChannel
import livekit.org.webrtc.IceCandidate
import livekit.org.webrtc.MediaConstraints
import livekit.org.webrtc.MediaStream
import livekit.org.webrtc.PeerConnection
import livekit.org.webrtc.PeerConnectionFactory
import livekit.org.webrtc.RtpReceiver
import livekit.org.webrtc.SdpObserver
import livekit.org.webrtc.SessionDescription

/**
 * MeloWare Internet - Stage 2: opt-in sharing of an audio folder and file transfer.
 * Firebase ONLY exchanges SDP/ICE; song metadata/file bytes go through WebRTC DataChannel.
 * STUN-only is required for audio transfer to avoid spending Metered trial bandwidth.
 * TURN can still be entered for HELLO/diagnostic connections, not for file transfer.
 * Files are shared only after the owner chooses a directory via Android SAF.
 * Receiver downloads one audio file at a time, with SHA-256 verification.
 */
private const val MELOWARE_P2P_TAG = "MeloWareP2P"
private const val MELOWARE_P2P_DB =
    "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
private const val MELOWARE_P2P_PATH = "meloware_internet_rooms"
private const val MELOWARE_P2P_LIFETIME_MS = 15 * 60 * 1000L

// Limit the first transfer test to MP3-size files and simple, predictable memory use.
// The limit is an MVP safety guard, NOT a WebRTC/Internet transfer limit.
private const val MW_AUDIO_PREFIX = "MWAUDIO1:"
private const val MW_AUDIO_MAX_COUNT = 300
private const val MW_AUDIO_MAX_FILE_BYTES = 32L * 1024L * 1024L
private const val MW_AUDIO_CHUNK = 16 * 1024
private const val MW_AUDIO_BATCH = 8
private val MW_AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")

private data class SharedAudioFile(
    val id: String,
    val uri: Uri,
    val name: String,
    val folder: String,
    val size: Long,
    val mime: String
)

private data class InternetRemoteAudio(
    val id: String,
    val name: String,
    val folder: String,
    val size: Long,
    val mime: String
)

private class OutgoingAudio(
    val item: SharedAudioFile,
    var input: InputStream? = null,
    var sent: Long = 0L,
    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
)

private class IncomingAudio(
    val id: String,
    val name: String,
    val mime: String,
    val size: Long,
    val temporary: File,
    val output: FileOutputStream,
    var received: Long = 0L,
    var inBatch: Int = 0,
    val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
)

private fun sha256Hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it.toInt() and 0xff) }
private fun audioSize(size: Long): String = if (size >= 1024 * 1024) {
    "%.1f MB".format(java.util.Locale.US, size / 1048576.0)
} else "${(size + 1023) / 1024} KB"
private fun safeMusicName(s: String): String = s.replace(Regex("[\\/:*?\"<>|\\p{Cntrl}]"), "_")
    .trim().trim('.').take(140).ifBlank { "MeloWare_Audio.mp3" }

private enum class InternetRole { NONE, HOST, GUEST }

private fun generateInternetRoomCode(): String {
    // 16 characters, 80 bits of entropy. Never use a short numerical PIN as a Firebase path.
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val rand = SecureRandom()
    return buildString {
        repeat(16) { append(alphabet[rand.nextInt(alphabet.length)]) }
    }
}

/** This object is created only while the Internet dialog is open. */
private class MeloWareInternetSession(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(MELOWARE_P2P_DB)

    var roomCode by mutableStateOf("")
        private set
    var inputCode by mutableStateOf("")
    var isHost by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var guestRequested by mutableStateOf(false)
        private set
    var approved by mutableStateOf(false)
        private set
    var dataChannelOpen by mutableStateOf(false)
        private set
    var status by mutableStateOf("Sẵn sàng kết nối qua Internet")
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastMessage by mutableStateOf("")
        private set
    var iceState by mutableStateOf("NEW")
        private set
    var diagnostics by mutableStateOf("")
        private set

    // Optional TURN test configuration: never saved to disk or Firebase.
    var turnUrl by mutableStateOf("")
    var turnUsername by mutableStateOf("")
    var turnPassword by mutableStateOf("")

    // Sender: only SAF-selected audio is offered. Receiver: music is saved after integrity check.
    var sharedFolder by mutableStateOf("")
        private set
    var sharedCount by mutableStateOf(0)
        private set
    val remoteAudio = mutableStateListOf<InternetRemoteAudio>()
    var transferMessage by mutableStateOf("Chưa chọn thư mục chia sẻ")
        private set
    var transferProgress by mutableFloatStateOf(0f)
        private set
    var transferBusy by mutableStateOf(false)
        private set
    private val sharedAudio = linkedMapOf<String, SharedAudioFile>()
    private val fileIo = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MeloWare-Internet-Audio").apply { isDaemon = true }
    }
    @Volatile private var outgoing: OutgoingAudio? = null
    @Volatile private var incoming: IncomingAudio? = null
    private var requestedFileId: String? = null
    private var listReceiving = false
    private var negotiatedWithTurn = false


    private val localIceTypes = linkedMapOf<String, Int>()
    private val remoteIceTypes = linkedMapOf<String, Int>()
    private var localIceGenerated = 0
    private var localIceSaved = 0
    private var localIceSaveFailed = 0
    private var remoteIceReceived = 0
    private var remoteIceAdded = 0
    private var remoteIceRejected = 0
    private var gatheringState = "NEW"
    private var localSdpType = "-"
    private var remoteSdpType = "-"

    @Volatile private var sequence = 0
    private var role = InternetRole.NONE
    private var room: DatabaseReference? = null
    private var roomListener: ValueEventListener? = null
    private var factory: PeerConnectionFactory? = null
    private var peer: PeerConnection? = null
    private var channel: DataChannel? = null
    private var remoteSdpInProgress = false
    private var remoteSdpReady = false
    private var offerStarted = false
    private val pendingRemoteIce = mutableListOf<IceCandidate>()
    private val observedRemoteIceKeys = mutableSetOf<String>()

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    private fun candidateType(candidate: IceCandidate): String =
        Regex("(?:^|\\s)typ\\s+(host|srflx|prflx|relay)(?:\\s|$)")
            .find(candidate.sdp)?.groupValues?.get(1) ?: "unknown"

    private fun updateDiagnostics() {
        fun types(map: Map<String, Int>): String =
            if (map.isEmpty()) "-" else map.entries.joinToString(", ") { "${it.key}:${it.value}" }
        diagnostics = "Gathering: $gatheringState | ICE: $iceState\n" +
            "SDP: local=$localSdpType, remote=$remoteSdpType\n" +
            "Local: $localIceGenerated tạo / $localIceSaved lưu Firebase " +
            "(lỗi: $localIceSaveFailed); ${types(localIceTypes)}\n" +
            "Remote: $remoteIceReceived nhận / $remoteIceAdded thêm " +
            "(lỗi: $remoteIceRejected); ${types(remoteIceTypes)}"
    }

    fun diagnosticReport(): String = "MeloWareP2P ${if (isHost) "A" else "B"}\n$diagnostics\n" +
        "Status: $status\nError: ${error ?: "-"}"


    /** The trial version NEVER transfers audio when TURN is configured locally. */
    private fun allowedToTransfer(): Boolean {
        if (negotiatedWithTurn || turnUrl.isNotBlank() || turnUsername.isNotBlank() || turnPassword.isNotBlank()) {
            transferMessage = "Phiên hiện tại có cấu hình TURN. Để tránh hao 500 MB Metered, xóa cả 3 ô TURN trên HAI máy và tạo phòng mới (P2P trực tiếp)."
            return false
        }
        return true
    }

    /** Enumerate ONLY audio files in an explicitly chosen SAF tree; no file bytes sent yet. */
    fun chooseAudioFolder(treeUri: Uri) {
        if (role != InternetRole.HOST) {
            transferMessage = "Hãy tạo phòng từ máy A trước khi chia sẻ thư mục nhạc."
            return
        }
        val token = sequence
        transferMessage = "Đang đọc thư mục nhạc đã chọn..."
        fileIo.execute {
            val items = linkedMapOf<String, SharedAudioFile>()
            var folderLabel = "Nhạc đã chọn"
            var truncated = false
            try {
                val resolver = context.contentResolver
                val rootId = DocumentsContract.getTreeDocumentId(treeUri)
                val rootDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
                resolver.query(rootDocumentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null)?.use { c ->
                    if (c.moveToFirst()) folderLabel = c.getString(0).orEmpty().take(70).ifBlank { folderLabel }
                }
                val queue: ArrayDeque<Pair<String, String>> = ArrayDeque()
                queue.add(rootId to "")
                var scanned = 0
                while (queue.isNotEmpty() && items.size < MW_AUDIO_MAX_COUNT && scanned < 12000 && sequence == token) {
                    val (parentId, parentPath) = queue.removeFirst()
                    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
                    val projection = arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    )
                    resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                        while (cursor.moveToNext() && sequence == token) {
                            scanned++
                            if (scanned > 12000 || items.size >= MW_AUDIO_MAX_COUNT) {
                                truncated = true
                                break
                            }
                            val docId = cursor.getString(0) ?: continue
                            val name = cursor.getString(1).orEmpty().take(140)
                            val mime = cursor.getString(2).orEmpty()
                            val size = if (cursor.isNull(3)) -1L else cursor.getLong(3)
                            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                                // Avoid recursing into an unexpectedly enormous tree.
                                if (parentPath.count { it == '/' } < 12) {
                                    queue.add(docId to (parentPath + name.take(60) + "/"))
                                }
                                continue
                            }
                            val ext = name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
                            if ((mime.startsWith("audio/") || ext in MW_AUDIO_EXTENSIONS) &&
                                size in 1..MW_AUDIO_MAX_FILE_BYTES) {
                                val id = UUID.randomUUID().toString()
                                val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                items[id] = SharedAudioFile(
                                    id, uri, name, parentPath.take(160), size,
                                    if (mime.startsWith("audio/")) mime else "audio/${ext.ifBlank { "mpeg" }}"
                                )
                            }
                        }
                    }
                }
                if (items.size >= MW_AUDIO_MAX_COUNT || scanned >= 12000) truncated = true
                onMain {
                    if (sequence != token) return@onMain
                    sharedAudio.clear()
                    sharedAudio.putAll(items)
                    sharedCount = items.size
                    sharedFolder = folderLabel
                    transferMessage = "Máy A chia sẻ $sharedCount bài từ '$folderLabel'" +
                        (if (truncated) " (giới hạn danh sách thử nghiệm)" else "")
                    if (dataChannelOpen) sendAudioList()
                }
            } catch (cause: Exception) {
                onMain { if (sequence == token) transferMessage = "Không đọc được thư mục: ${cause.localizedMessage}" }
            }
        }
    }

    private fun sendAudioControl(type: String, mutate: (JSONObject) -> Unit = {}): Boolean {
        val dc = channel ?: return false
        if (dc.state() != DataChannel.State.OPEN) return false
        val obj = JSONObject().put("type", type)
        mutate(obj)
        val bytes = (MW_AUDIO_PREFIX + obj.toString()).toByteArray(Charsets.UTF_8)
        return dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
    }

    /** Small, ordered pages; large file lists are never uploaded into Firebase. */
    private fun sendAudioList() {
        if (role != InternetRole.HOST || !dataChannelOpen) return
        sendAudioControl("list_begin") { it.put("count", sharedAudio.size) }
        sharedAudio.values.toList().chunked(12).forEach { page ->
            sendAudioControl("list_page") { packet ->
                val data = JSONArray()
                page.forEach { audio ->
                    data.put(JSONObject()
                        .put("id", audio.id).put("name", audio.name)
                        .put("folder", audio.folder).put("size", audio.size).put("mime", audio.mime))
                }
                packet.put("items", data)
            }
        }
        sendAudioControl("list_end")
    }

    fun refreshRemoteAudio() {
        if (role != InternetRole.GUEST || !dataChannelOpen) {
            transferMessage = "Máy B cần tham gia phòng và kết nối WebRTC trước."
            return
        }
        sendAudioControl("list_request")
        transferMessage = "Đang yêu cầu danh sách nhạc từ máy A..."
    }

    fun downloadAudio(audio: InternetRemoteAudio) {
        if (role != InternetRole.GUEST || !dataChannelOpen) return
        if (!allowedToTransfer()) return
        if (transferBusy || requestedFileId != null) return
        if (audio.size !in 1..MW_AUDIO_MAX_FILE_BYTES) {
            transferMessage = "Bản thử nghiệm chỉ tải file tối đa 32 MB."
            return
        }
        requestedFileId = audio.id
        transferBusy = true
        transferProgress = 0f
        transferMessage = "Đang yêu cầu '${audio.name}'..."
        if (!sendAudioControl("file_request") { it.put("id", audio.id) }) {
            requestedFileId = null
            transferBusy = false
            transferMessage = "Không gửi được yêu cầu tải nhạc"
        }
    }

    private fun processAudioControl(text: String, token: Int) {
        val msg = runCatching { JSONObject(text.removePrefix(MW_AUDIO_PREFIX)) }.getOrNull() ?: return
        val type = msg.optString("type")
        val id = msg.optString("id")
        when (type) {
            "list_request" -> if (role == InternetRole.HOST) sendAudioList()
            "list_begin" -> if (role == InternetRole.GUEST) {
                remoteAudio.clear()
                listReceiving = true
                transferMessage = "Đang nhận danh sách nhạc..."
            }
            "list_page" -> if (role == InternetRole.GUEST && listReceiving) {
                val items = msg.optJSONArray("items") ?: return
                for (idx in 0 until items.length()) {
                    val item = items.optJSONObject(idx) ?: continue
                    if (remoteAudio.size >= MW_AUDIO_MAX_COUNT) break
                    val itemId = item.optString("id")
                    val name = item.optString("name")
                    val size = item.optLong("size", -1)
                    if (!Regex("[0-9a-f-]{36}").matches(itemId) || name.isBlank() || size <= 0) continue
                    remoteAudio.add(InternetRemoteAudio(
                        itemId, name.take(140), item.optString("folder").take(160), size,
                        item.optString("mime").take(100)
                    ))
                }
            }
            "list_end" -> if (role == InternetRole.GUEST) {
                listReceiving = false
                transferMessage = "Đã nhận danh sách ${remoteAudio.size} bài hát từ máy A."
            }
            "file_request" -> if (role == InternetRole.HOST) {
                if (!allowedToTransfer()) {
                    sendAudioControl("file_error") { it.put("text", "Máy A đang bật TURN; không truyền nhạc trong bản thử nghiệm") }
                    return
                }
                val item = sharedAudio[id]
                if (item == null || item.size !in 1..MW_AUDIO_MAX_FILE_BYTES || outgoing != null) {
                    sendAudioControl("file_error") { it.put("text", "Không tìm thấy bài hát hoặc máy A đang bận") }
                    return
                }
                outgoing = OutgoingAudio(item)
                transferBusy = true
                transferProgress = 0f
                transferMessage = "Chuẩn bị gửi ${item.name}..."
                sendAudioControl("file_begin") { it.put("id", item.id).put("name", item.name)
                    .put("mime", item.mime).put("size", item.size) }
            }
            "file_begin" -> if (role == InternetRole.GUEST) {
                val size = msg.optLong("size", -1L)
                if (id != requestedFileId || size !in 1..MW_AUDIO_MAX_FILE_BYTES || incoming != null) {
                    sendAudioControl("file_error") { it.put("text", "Thông tin file không hợp lệ") }
                    return
                }
                val name = safeMusicName(msg.optString("name"))
                val mime = msg.optString("mime").take(100).ifBlank { "audio/mpeg" }
                fileIo.execute {
                    try {
                        val temporary = File.createTempFile("meloware_music_", ".part", context.cacheDir)
                        incoming = IncomingAudio(id, name, mime, size, temporary, FileOutputStream(temporary))
                        onMain {
                            if (sequence == token) {
                                transferMessage = "Đang tải $name ..."
                                sendAudioControl("file_ready") { it.put("id", id) }
                            }
                        }
                    } catch (cause: Exception) {
                        onMain { if (sequence == token) rejectAudio("Không tạo được file tạm: ${cause.localizedMessage}") }
                    }
                }
            }
            "file_ready" -> if (role == InternetRole.HOST && outgoing?.item?.id == id) {
                sendNextAudioBatch(token)
            }
            "file_credit" -> if (role == InternetRole.HOST && outgoing?.item?.id == id) {
                sendNextAudioBatch(token)
            }
            "file_end" -> if (role == InternetRole.GUEST && incoming?.id == id) {
                val hash = msg.optString("sha256")
                fileIo.execute { finalizeAudio(hash, token) }
            }
            "file_saved" -> if (role == InternetRole.HOST && outgoing?.item?.id == id) {
                fileIo.execute { runCatching { outgoing?.input?.close() }; outgoing = null }
                transferBusy = false
                transferProgress = 1f
                transferMessage = "Máy B đã nhận và lưu nhạc thành công."
            }
            "file_error" -> {
                val reason = msg.optString("text").take(180)
                transferMessage = "Truyền nhạc thất bại: $reason"
                transferBusy = false
                requestedFileId = null
                fileIo.execute { discardTransfers() }
            }
        }
    }

    /** One 128-KiB batch is released per receiver credit: bounded memory even on slow phones. */
    private fun sendNextAudioBatch(token: Int) {
        fileIo.execute {
            val current = outgoing ?: return@execute
            try {
                if (sequence != token) return@execute
                if (current.input == null) {
                    current.input = context.contentResolver.openInputStream(current.item.uri)
                        ?: throw java.io.IOException("Không thể đọc nhạc từ thư mục đã chọn")
                }
                val dc = channel ?: throw java.io.IOException("WebRTC đã ngắt")
                val chunk = ByteArray(MW_AUDIO_CHUNK)
                repeat(MW_AUDIO_BATCH) {
                    if (sequence != token || dc.state() != DataChannel.State.OPEN) return@execute
                    val read = current.input!!.read(chunk)
                    if (read <= 0) throw java.io.EOFException("File nguồn bị thay đổi khi đang truyền")
                    if (!dc.send(DataChannel.Buffer(ByteBuffer.wrap(chunk.copyOf(read)), true))) {
                        throw java.io.IOException("DataChannel từ chối gửi dữ liệu")
                    }
                    current.digest.update(chunk, 0, read)
                    current.sent += read
                    if (current.sent > current.item.size) throw java.io.IOException("Dung lượng file nguồn thay đổi")
                    val progress = (current.sent.toFloat() / current.item.size).coerceIn(0f, 1f)
                    onMain { if (sequence == token) transferProgress = progress }
                    if (current.sent == current.item.size) {
                        current.input?.close()
                        current.input = null
                        val hash = sha256Hex(current.digest.digest())
                        sendAudioControl("file_end") { it.put("id", current.item.id).put("sha256", hash) }
                        return@execute
                    }
                }
                // Wait for file_credit from B before pushing the next batch.
            } catch (cause: Exception) {
                onMain { if (sequence == token) rejectAudio("Lỗi gửi file: ${cause.localizedMessage}") }
            }
        }
    }

    /** Binary DataChannel frames contain raw audio only. Ordered/reliable SCTP preserves chunks. */
    private fun receiveAudioBytes(bytes: ByteArray, token: Int) {
        fileIo.execute {
            val current = incoming ?: return@execute
            if (sequence != token) return@execute
            try {
                if (current.received + bytes.size > current.size) throw java.io.IOException("Nhận quá dung lượng")
                current.output.write(bytes)
                current.digest.update(bytes)
                current.received += bytes.size
                current.inBatch++
                val progress = (current.received.toFloat() / current.size).coerceIn(0f, 1f)
                if (current.inBatch >= MW_AUDIO_BATCH && current.received < current.size) {
                    current.inBatch = 0
                    sendAudioControl("file_credit") { it.put("id", current.id) }
                }
                onMain { if (sequence == token) transferProgress = progress }
            } catch (cause: Exception) {
                onMain { if (sequence == token) rejectAudio("Lỗi ghi nhạc: ${cause.localizedMessage}") }
            }
        }
    }

    private fun finalizeAudio(expectedHash: String, token: Int) {
        val current = incoming ?: return
        try {
            current.output.flush()
            current.output.close()
            if (current.received != current.size) throw java.io.IOException("File nhận thiếu dữ liệu")
            if (sha256Hex(current.digest.digest()) != expectedHash) throw java.io.IOException("SHA-256 không khớp")
            val path = publishVerifiedAudio(current)
            current.temporary.delete()
            incoming = null
            onMain {
                if (sequence != token) return@onMain
                transferBusy = false
                transferProgress = 1f
                transferMessage = "Đã lưu '${current.name}' vào $path. Vào Thư viện → Quét nhạc để cập nhật."
                requestedFileId = null
                sendAudioControl("file_saved") { it.put("id", current.id) }
            }
        } catch (cause: Exception) {
            onMain { if (sequence == token) rejectAudio("Không lưu được bài hát: ${cause.localizedMessage}") }
        }
    }

    private fun publishVerifiedAudio(current: IncomingAudio): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, safeMusicName(current.name))
                put(MediaStore.Audio.Media.TITLE, current.name.substringBeforeLast('.').take(140))
                put(MediaStore.Audio.Media.MIME_TYPE, current.mime)
                put(MediaStore.Audio.Media.IS_MUSIC, 1)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/MeloWare Internet/")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val dest = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw java.io.IOException("MediaStore không tạo được file")
            try {
                resolver.openOutputStream(dest, "w")?.use { output ->
                    FileInputStream(current.temporary).use { it.copyTo(output) }
                } ?: throw java.io.IOException("Không mở được file âm thanh đích")
                resolver.update(dest, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
                return "Music/MeloWare Internet"
            } catch (e: Exception) {
                resolver.delete(dest, null, null)
                throw e
            }
        }
        // API 23-28: public Music requires WRITE_EXTERNAL_STORAGE (already in the manifest).
        val canWritePublic = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val folder = if (canWritePublic) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MeloWare Internet")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MeloWare Internet")
        }
        if (!folder.exists() && !folder.mkdirs()) throw java.io.IOException("Không tạo được thư mục lưu nhạc")
        val output = File(folder, "${System.currentTimeMillis()}_${safeMusicName(current.name)}")
        FileInputStream(current.temporary).use { input -> FileOutputStream(output).use { input.copyTo(it) } }
        MediaScannerConnection.scanFile(context, arrayOf(output.absolutePath), arrayOf(current.mime), null)
        return if (canWritePublic) "Music/MeloWare Internet" else "Android/data/.../Music/MeloWare Internet"
    }

    private fun rejectAudio(reason: String) {
        transferMessage = reason
        transferBusy = false
        requestedFileId = null
        sendAudioControl("file_error") { it.put("text", reason.take(150)) }
        fileIo.execute { discardTransfers() }
    }

    private fun discardTransfers() {
        runCatching { outgoing?.input?.close() }
        outgoing = null
        val old = incoming
        incoming = null
        runCatching { old?.output?.close() }
        runCatching { old?.temporary?.delete() }
    }

    fun shutdown() {
        close()
        fileIo.shutdown()
    }

    private fun validTurnConfig(): Boolean {
        val url = turnUrl.trim()
        // Allow STUN-only testing when all TURN inputs are empty.
        if (url.isEmpty() && turnUsername.isBlank() && turnPassword.isBlank()) return true
        // Do not silently ignore partial TURN credentials.
        if ((!url.startsWith("turn:") && !url.startsWith("turns:")) ||
            turnUsername.isBlank() || turnPassword.isBlank()) {
            fail("TURN: vui lòng nhập đủ URL, Username và Password (hoặc để trống cả 3 để dùng STUN)")
            return false
        }
        return true
    }

    private fun info(message: String) {
        Log.i(MELOWARE_P2P_TAG, message)
        onMain { status = message }
    }

    private fun fail(message: String, cause: Throwable? = null) {
        Log.e(MELOWARE_P2P_TAG, message, cause)
        onMain {
            busy = false
            error = "$message${cause?.localizedMessage?.let { ": $it" } ?: ""}"
            status = "Không thể hoàn tất kết nối"
        }
    }

    private fun authenticate(token: Int, callback: (String) -> Unit) {
        auth.currentUser?.uid?.let {
            if (sequence == token) callback(it)
            return
        }
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                if (sequence == token) {
                    val uid = result.user?.uid
                    if (uid.isNullOrBlank()) fail("Firebase chưa trả UID") else callback(uid)
                }
            }
            .addOnFailureListener { if (sequence == token) fail("Firebase Anonymous Auth thất bại", it) }
    }

    fun createRoom() {
        if (!validTurnConfig()) return
        close()
        val token = sequence
        role = InternetRole.HOST
        isHost = true
        busy = true
        error = null
        info("Đang tạo phòng...")
        authenticate(token) { uid ->
            val code = generateInternetRoomCode()
            val reference = database.getReference(MELOWARE_P2P_PATH).child(code)
            room = reference
            val data = mapOf(
                "hostUid" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "expiresAt" to (System.currentTimeMillis() + MELOWARE_P2P_LIFETIME_MS)
            )
            reference.setValue(data).addOnSuccessListener {
                if (sequence != token) return@addOnSuccessListener
                roomCode = code
                busy = false
                // Firebase removes this room if the host loses its DB connection.
                reference.onDisconnect().removeValue()
                subscribeRoom(token)
                info("Đã tạo phòng. Gửi mã cho điện thoại B.")
            }.addOnFailureListener {
                if (sequence == token) fail("Không thể tạo phòng; kiểm tra Firebase Rules", it)
            }
        }
    }

    fun joinRoom() {
        if (!validTurnConfig()) return
        val code = inputCode.uppercase().replace(Regex("[^A-Z2-9]"), "")
        if (!Regex("[A-HJ-NP-Z2-9]{16}").matches(code)) {
            fail("Mã kết nối cần đủ 16 ký tự")
            return
        }
        close()
        val token = sequence
        role = InternetRole.GUEST
        isHost = false
        busy = true
        roomCode = code
        error = null
        info("Đang gửi yêu cầu đến điện thoại A...")
        authenticate(token) { uid ->
            val reference = database.getReference(MELOWARE_P2P_PATH).child(code)
            room = reference
            // Guest cannot read a room until this atomic claim has succeeded.
            reference.child("guestUid").setValue(uid)
                .addOnSuccessListener {
                    if (sequence == token) {
                        busy = false
                        subscribeRoom(token)
                        info("Đã gửi yêu cầu. Chờ máy A chấp nhận.")
                    }
                }
                .addOnFailureListener {
                    if (sequence == token) fail("Không tham gia được: mã sai, hết hạn hoặc phòng đã có khách", it)
                }
        }
    }

    fun approveGuest() {
        if (role != InternetRole.HOST || !guestRequested || approved) return
        val token = sequence
        busy = true
        room?.child("approved")?.setValue(true)
            ?.addOnSuccessListener {
                if (sequence == token) {
                    busy = false
                    info("Đã cho phép máy B. Đang tạo kết nối P2P...")
                }
            }
            ?.addOnFailureListener {
                if (sequence == token) fail("Không thể chấp nhận kết nối", it)
            }
    }

    private fun subscribeRoom(token: Int) {
        val reference = room ?: return
        val listener = object : ValueEventListener {
            override fun onCancelled(dbError: DatabaseError) {
                if (sequence == token) fail("Firebase từ chối đọc phòng: ${dbError.message}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (sequence != token) return
                if (!snapshot.exists()) {
                    fail("Phòng đã đóng hoặc bị ngắt kết nối")
                    return
                }
                val expiresAt = snapshot.child("expiresAt").getValue(Long::class.java) ?: 0L
                if (expiresAt < System.currentTimeMillis()) {
                    fail("Mã ghép nối đã hết hạn. Tạo mã mới.")
                    return
                }
                val guestUid = snapshot.child("guestUid").getValue(String::class.java)
                val guestApproved = snapshot.child("approved").getValue(Boolean::class.java) == true
                if (role == InternetRole.HOST) {
                    if (!guestUid.isNullOrBlank() && !guestApproved) {
                        guestRequested = true
                        info("Máy B muốn kết nối. Nhấn Chấp nhận trên máy A.")
                    }
                    if (guestApproved && !offerStarted) {
                        approved = true
                        startOffer(token)
                    }
                    if (guestApproved) {
                        processRemoteSdp(snapshot.child("answer"), SessionDescription.Type.ANSWER, token)
                    }
                } else if (role == InternetRole.GUEST) {
                    if (guestApproved) {
                        approved = true
                        processRemoteSdp(snapshot.child("offer"), SessionDescription.Type.OFFER, token)
                    }
                }
                if (guestApproved) {
                    val candidates = if (role == InternetRole.HOST) "guestIce" else "hostIce"
                    processIce(snapshot.child(candidates))
                }
            }
        }
        roomListener = listener
        reference.addValueEventListener(listener)
    }

    private fun setupPeer(token: Int): PeerConnection? {
        peer?.let { return it }
        return runCatching {
            initializeWebRtc(context.applicationContext)
            val newFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            factory = newFactory
            // Redundant public STUN endpoints reduce single-server failures, but do not
            // bypass restrictive 5G CGNAT. TURN is the reliable relay fallback.
            val iceServers = mutableListOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
            negotiatedWithTurn = turnUrl.isNotBlank()
            if (turnUrl.isNotBlank()) {
                iceServers += PeerConnection.IceServer.builder(turnUrl.trim())
                    .setUsername(turnUsername.trim())
                    .setPassword(turnPassword)
                    .createIceServer()
                Log.i(MELOWARE_P2P_TAG, "TURN configured (credentials not logged)")
            } else {
                Log.i(MELOWARE_P2P_TAG, "STUN-only connection; TURN not configured")
            }
            val config = PeerConnection.RTCConfiguration(iceServers).apply {
                sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            }
            val newPeer = newFactory.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState) {
                    Log.d(MELOWARE_P2P_TAG, "Signaling: $state")
                }
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                    onMain {
                        if (sequence != token) return@onMain
                        iceState = state.toString()
                        status = "ICE: $state"
                        Log.i(MELOWARE_P2P_TAG, "ICE connection: $state")
                        if (state == PeerConnection.IceConnectionState.CONNECTED ||
                            state == PeerConnection.IceConnectionState.COMPLETED) {
                            error = null
                        }
                        if (state == PeerConnection.IceConnectionState.FAILED) {
                            error = if ((localIceTypes["srflx"] ?: 0) == 0 &&
                                (localIceTypes["relay"] ?: 0) == 0) {
                                "ICE FAILED: Không có candidate công khai/relay ở máy này. " +
                                    "Kiểm tra STUN trên mạng hiện tại; có thể cần TURN."
                            } else {
                                "ICE FAILED: Hai mạng không tìm được đường truyền chung. " +
                                    "Nếu Firebase đã trao đổi ICE đủ, hãy cấu hình TURN."
                            }
                        }
                        updateDiagnostics()
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {
                    onMain {
                        if (sequence != token) return@onMain
                        gatheringState = state.toString()
                        Log.i(MELOWARE_P2P_TAG, "ICE gathering: $state")
                        updateDiagnostics()
                    }
                }
                override fun onIceCandidate(candidate: IceCandidate) {
                    // Never print raw ICE SDP: it can contain local/public IP addresses.
                    onMain {
                        if (sequence != token) return@onMain
                        val kind = candidateType(candidate)
                        localIceGenerated++
                        localIceTypes[kind] = (localIceTypes[kind] ?: 0) + 1
                        Log.d(MELOWARE_P2P_TAG, "Local candidate: type=$kind")
                        updateDiagnostics()
                        val branch = if (role == InternetRole.HOST) "hostIce" else "guestIce"
                        val target = room?.child(branch)?.push()
                        if (target == null) {
                            localIceSaveFailed++
                            Log.e(MELOWARE_P2P_TAG, "Cannot queue ICE: room missing")
                            updateDiagnostics()
                            return@onMain
                        }
                        target.setValue(
                            mapOf("mid" to (candidate.sdpMid ?: ""),
                                "line" to candidate.sdpMLineIndex, "sdp" to candidate.sdp)
                        ).addOnSuccessListener {
                            onMain {
                                if (sequence == token) {
                                    localIceSaved++
                                    updateDiagnostics()
                                }
                            }
                        }.addOnFailureListener { cause ->
                            onMain {
                                if (sequence == token) {
                                    localIceSaveFailed++
                                    Log.e(MELOWARE_P2P_TAG, "Firebase ICE write rejected", cause)
                                    error = "Firebase không ghi được ICE Candidate: ${cause.localizedMessage}"
                                    updateDiagnostics()
                                }
                            }
                        }
                    }
                }
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
                override fun onAddStream(stream: MediaStream) = Unit
                override fun onRemoveStream(stream: MediaStream) = Unit
                override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) = Unit
                override fun onDataChannel(incoming: DataChannel) {
                    onMain { if (sequence == token) observeDataChannel(incoming, token) }
                }
                override fun onRenegotiationNeeded() = Unit
            }) ?: throw IllegalStateException("Không tạo được PeerConnection")
            peer = newPeer
            newPeer
        }.onFailure { fail("Không khởi tạo được WebRTC", it) }.getOrNull()
    }

    private fun observeDataChannel(newChannel: DataChannel, token: Int) {
        channel = newChannel
        newChannel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) = Unit
            override fun onStateChange() {
                onMain {
                    if (sequence != token) return@onMain
                    dataChannelOpen = newChannel.state() == DataChannel.State.OPEN
                    if (dataChannelOpen) {
                        info("WebRTC DataChannel đã kết nối! Có thể gửi HELLO hoặc chia sẻ nhạc.")
                        if (role == InternetRole.GUEST) refreshRemoteAudio()
                        if (role == InternetRole.HOST && sharedAudio.isNotEmpty()) sendAudioList()
                    }
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                if (buffer.binary) {
                    receiveAudioBytes(bytes, token)
                } else {
                    val text = bytes.toString(Charsets.UTF_8)
                    onMain {
                        if (sequence == token) {
                            if (text.startsWith(MW_AUDIO_PREFIX)) {
                                processAudioControl(text, token)
                            } else {
                                lastMessage = text.take(300)
                                info("Đã nhận dữ liệu WebRTC từ thiết bị còn lại")
                            }
                        }
                    }
                }
            }
        })
        dataChannelOpen = newChannel.state() == DataChannel.State.OPEN
    }

    private fun startOffer(token: Int) {
        offerStarted = true
        val pc = setupPeer(token) ?: return
        val newChannel = pc.createDataChannel("meloware-test", DataChannel.Init())
        if (newChannel == null) {
            fail("Không tạo được WebRTC DataChannel")
            return
        }
        observeDataChannel(newChannel, token)
        pc.createOffer(object : BaseInternetSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                onMain { if (sequence == token) applyLocalSdp(sdp, token, "offer") }
            }
            override fun onCreateFailure(reason: String) {
                if (sequence == token) fail("Tạo SDP offer thất bại: $reason")
            }
        }, MediaConstraints())
    }

    private fun applyLocalSdp(sdp: SessionDescription, token: Int, key: String) {
        val pc = peer ?: return
        pc.setLocalDescription(object : BaseInternetSdpObserver() {
            override fun onSetSuccess() {
                onMain {
                    if (sequence == token) {
                        localSdpType = key
                        Log.i(MELOWARE_P2P_TAG, "Local SDP set: $key")
                        updateDiagnostics()
                        room?.child(key)?.setValue(mapOf("type" to sdp.type.canonicalForm(), "sdp" to sdp.description))
                            ?.addOnFailureListener { fail("Không gửi được $key", it) }
                    }
                }
            }
            override fun onSetFailure(reason: String) { if (sequence == token) fail("Lỗi local SDP: $reason") }
        }, sdp)
    }

    private fun processRemoteSdp(node: DataSnapshot, kind: SessionDescription.Type, token: Int) {
        if (!node.exists() || remoteSdpInProgress || remoteSdpReady) return
        val sdpText = node.child("sdp").getValue(String::class.java).orEmpty()
        val sdpType = node.child("type").getValue(String::class.java).orEmpty()
        if (sdpText.isBlank() || sdpType != kind.canonicalForm()) return
        remoteSdpInProgress = true
        val pc = setupPeer(token) ?: return
        pc.setRemoteDescription(object : BaseInternetSdpObserver() {
            override fun onSetSuccess() {
                onMain {
                    if (sequence != token) return@onMain
                    remoteSdpReady = true
                    remoteSdpInProgress = false
                    remoteSdpType = kind.canonicalForm()
                    Log.i(MELOWARE_P2P_TAG, "Remote SDP set: $remoteSdpType")
                    flushRemoteIce()
                    updateDiagnostics()
                    if (kind == SessionDescription.Type.OFFER) {
                        pc.createAnswer(object : BaseInternetSdpObserver() {
                            override fun onCreateSuccess(sdp: SessionDescription) {
                                onMain { if (sequence == token) applyLocalSdp(sdp, token, "answer") }
                            }
                            override fun onCreateFailure(reason: String) {
                                if (sequence == token) fail("Tạo SDP answer thất bại: $reason")
                            }
                        }, MediaConstraints())
                    }
                }
            }
            override fun onSetFailure(reason: String) {
                onMain {
                    remoteSdpInProgress = false
                    if (sequence == token) fail("Remote SDP thất bại: $reason")
                }
            }
        }, SessionDescription(kind, sdpText))
    }

    private fun processIce(node: DataSnapshot) {
        node.children.forEach { child ->
            val key = child.key ?: return@forEach
            if (key in observedRemoteIceKeys) return@forEach
            val sdp = child.child("sdp").getValue(String::class.java).orEmpty()
            if (sdp.isBlank()) return@forEach
            observedRemoteIceKeys.add(key)
            val candidate = IceCandidate(
                child.child("mid").getValue(String::class.java).orEmpty(),
                child.child("line").getValue(Int::class.java) ?: 0,
                sdp
            )
            val kind = candidateType(candidate)
            remoteIceReceived++
            remoteIceTypes[kind] = (remoteIceTypes[kind] ?: 0) + 1
            Log.d(MELOWARE_P2P_TAG, "Remote candidate received: type=$kind")
            if (remoteSdpReady) addRemoteIce(candidate) else pendingRemoteIce.add(candidate)
        }
        updateDiagnostics()
    }

    private fun addRemoteIce(candidate: IceCandidate) {
        val accepted = peer?.addIceCandidate(candidate) == true
        if (accepted) {
            remoteIceAdded++
        } else {
            remoteIceRejected++
            Log.w(MELOWARE_P2P_TAG, "WebRTC rejected remote candidate: type=${candidateType(candidate)}")
        }
        updateDiagnostics()
    }

    private fun flushRemoteIce() {
        pendingRemoteIce.forEach { addRemoteIce(it) }
        pendingRemoteIce.clear()
    }

    fun sendHello() {
        val dc = channel
        if (dc == null || dc.state() != DataChannel.State.OPEN) {
            fail("WebRTC chưa kết nối; chưa thể gửi HELLO")
            return
        }
        val message = "HELLO từ MeloWare ${if (isHost) "máy A" else "máy B"}"
        if (dc.send(DataChannel.Buffer(ByteBuffer.wrap(message.toByteArray(Charsets.UTF_8)), false))) {
            info("Đã gửi HELLO trực tiếp qua WebRTC")
        } else {
            fail("Không gửi được HELLO")
        }
    }

    fun close() {
        sequence++
        fileIo.execute { discardTransfers() }
        sharedAudio.clear()
        remoteAudio.clear()
        requestedFileId = null
        sharedFolder = ""
        sharedCount = 0
        transferBusy = false
        transferProgress = 0f
        transferMessage = "Chưa chọn thư mục chia sẻ"
        listReceiving = false
        negotiatedWithTurn = false
        val oldRoom = room
        val oldListener = roomListener
        val host = role == InternetRole.HOST
        room = null
        roomListener = null
        if (oldRoom != null && oldListener != null) oldRoom.removeEventListener(oldListener)
        if (host) {
            oldRoom?.onDisconnect()?.cancel()
            oldRoom?.removeValue()
        }
        runCatching { channel?.unregisterObserver() }
        runCatching { channel?.dispose() }
        runCatching { peer?.close() }
        runCatching { peer?.dispose() }
        runCatching { factory?.dispose() }
        channel = null
        peer = null
        factory = null
        observedRemoteIceKeys.clear()
        pendingRemoteIce.clear()
        localIceTypes.clear()
        remoteIceTypes.clear()
        localIceGenerated = 0
        localIceSaved = 0
        localIceSaveFailed = 0
        remoteIceReceived = 0
        remoteIceAdded = 0
        remoteIceRejected = 0
        gatheringState = "NEW"
        iceState = "NEW"
        localSdpType = "-"
        remoteSdpType = "-"
        diagnostics = ""
        remoteSdpReady = false
        remoteSdpInProgress = false
        offerStarted = false
        role = InternetRole.NONE
        roomCode = ""
        isHost = false
        busy = false
        guestRequested = false
        approved = false
        dataChannelOpen = false
        lastMessage = ""
        status = "Sẵn sàng kết nối qua Internet"
        error = null
    }
}

private open class BaseInternetSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) = Unit
    override fun onSetSuccess() = Unit
    override fun onCreateFailure(reason: String) = Unit
    override fun onSetFailure(reason: String) = Unit
}

private val WEBRTC_INIT_LOCK = Any()
@Volatile private var webRtcInitialized = false
private fun initializeWebRtc(context: Context) {
    if (webRtcInitialized) return
    synchronized(WEBRTC_INIT_LOCK) {
        if (!webRtcInitialized) {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
            )
            webRtcInitialized = true
        }
    }
}

@Composable
fun MeloWareInternetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val session = remember(context) { MeloWareInternetSession(context.applicationContext) }
    DisposableEffect(session) { onDispose { session.shutdown() } }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) session.chooseAudioFolder(uri)
    }
    var remoteSearch by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 680.dp),
            color = colors.card,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kết nối qua Internet", fontWeight = FontWeight.Bold, fontSize = 19.sp, color = colors.textPrimary)
                    TextButton(onClick = onDismiss) { Text("Đóng", color = colors.purpleBright) }
                }
                Text(
                    "Chia sẻ thư mục nhạc A → B qua Internet. Firebase chỉ dùng ghép nối; " +
                        "bài hát truyền qua WebRTC. Để trống TURN để tải nhạc P2P không tốn hạn mức Metered.",
                    color = colors.textSecondary, fontSize = 12.sp
                )
                // TURN credentials are displayed before connection controls on purpose.
                // Older versions hid these fields at the bottom of a collapsed section,
                // making them very hard to discover on compact Android screens.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.backgroundSoft,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            "Cấu hình TURN (5G ↔ Wi-Fi)",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Điền trên cả hai điện thoại trước khi tạo / tham gia phòng. " +
                                "Để trống cả 3 ô nếu muốn thử STUN không dùng TURN.",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = session.turnUrl,
                            onValueChange = { session.turnUrl = it.take(240) },
                            label = { Text("TURN URL") },
                            placeholder = { Text("turn:global.relay.metered.ca:80") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(onClick = {
                            session.turnUrl = "turn:global.relay.metered.ca:80"
                        }) {
                            Text("Điền nhanh URL Metered", color = colors.purpleBright, fontSize = 12.sp)
                        }
                        OutlinedTextField(
                            value = session.turnUsername,
                            onValueChange = { session.turnUsername = it.take(200) },
                            label = { Text("TURN Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = session.turnPassword,
                            onValueChange = { session.turnPassword = it.take(300) },
                            label = { Text("TURN Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        val anyTurnFieldEntered = session.turnUrl.isNotBlank() ||
                            session.turnUsername.isNotBlank() || session.turnPassword.isNotBlank()
                        val completeTurnFields = session.turnUrl.isNotBlank() &&
                            session.turnUsername.isNotBlank() && session.turnPassword.isNotBlank()
                        Text(
                            when {
                                completeTurnFields -> "TURN đã nhập đủ. Hãy tạo phiên mới để áp dụng."
                                anyTurnFieldEntered -> "Bạn cần nhập đủ 3 ô trước khi kết nối."
                                else -> "Chưa nhập TURN: ứng dụng sẽ chỉ dùng STUN."
                            },
                            color = if (anyTurnFieldEntered && !completeTurnFields) colors.red else colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = session::createRoom,
                    enabled = !session.busy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)
                ) { Text("Máy A · Tạo mã kết nối") }

                if (session.roomCode.isNotBlank() && session.isHost) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(session.roomCode.chunked(4).joinToString("-"),
                            color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare Internet", session.roomCode))
                        }) { Text("Sao chép", color = colors.purpleBright) }
                    }
                }
                OutlinedTextField(
                    value = session.inputCode,
                    onValueChange = { session.inputCode = it.take(23) },
                    label = { Text("Máy B · Nhập mã từ máy A") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = session::joinRoom, enabled = !session.busy,
                    modifier = Modifier.fillMaxWidth()) { Text("Máy B · Tham gia phòng") }

                if (session.isHost && session.guestRequested && !session.approved) {
                    Button(onClick = session::approveGuest, enabled = !session.busy,
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Chấp nhận cho máy B kết nối")
                    }
                }
                if (session.dataChannelOpen) {
                    Button(onClick = session::sendHello, modifier = Modifier.fillMaxWidth()) {
                        Text("Gửi HELLO để thử P2P")
                    }
                }
                if (session.isHost && session.roomCode.isNotBlank()) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Máy A · Chia sẻ nhạc", color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Chọn một thư mục nhạc trên điện thoại. Máy B chỉ được xem các bài trong thư mục bạn chọn.",
                                color = colors.textSecondary, fontSize = 11.sp)
                            OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Chọn thư mục nhạc trên máy A")
                            }
                            if (session.sharedFolder.isNotBlank()) {
                                Text("${session.sharedFolder} · ${session.sharedCount} bài có thể chia sẻ",
                                    color = colors.textPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                if (!session.isHost && session.roomCode.isNotBlank()) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Máy B · Nhận nhạc từ Internet", color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            OutlinedButton(onClick = session::refreshRemoteAudio,
                                enabled = session.dataChannelOpen, modifier = Modifier.fillMaxWidth()) {
                                Text("Làm mới danh sách nhạc máy A")
                            }
                            if (session.remoteAudio.isNotEmpty()) {
                                OutlinedTextField(value = remoteSearch, onValueChange = { remoteSearch = it.take(70) },
                                    label = { Text("Tìm tên bài hát") }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth())
                            }
                            val filtered = session.remoteAudio.filter {
                                remoteSearch.isBlank() || it.name.contains(remoteSearch, ignoreCase = true) ||
                                    it.folder.contains(remoteSearch, ignoreCase = true)
                            }
                            filtered.take(30).forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = colors.textPrimary, fontSize = 12.sp,
                                            maxLines = 2)
                                        Text("${item.folder}${audioSize(item.size)}",
                                            color = colors.textSecondary, fontSize = 10.sp)
                                    }
                                    TextButton(onClick = { session.downloadAudio(item) },
                                        enabled = session.dataChannelOpen && !session.transferBusy) {
                                        Text("Tải", color = colors.purpleBright)
                                    }
                                }
                            }
                            if (filtered.size > 30) Text("Hiển thị 30/${filtered.size} bài. Hãy tìm kiếm để thu hẹp.",
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
                if (session.roomCode.isNotBlank()) {
                    Text("Lưu ý: Bản thử nghiệm cho phép tải từng bài tối đa 32 MB khi cả hai máy ĐỂ TRỐNG TURN. " +
                        "Nếu cần TURN Metered, ứng dụng sẽ không truyền file để bảo vệ hạn mức 500 MB.",
                        color = colors.textSecondary, fontSize = 11.sp)
                    if (session.transferBusy) {
                        LinearProgressIndicator(progress = session.transferProgress, modifier = Modifier.fillMaxWidth())
                    }
                    Text(session.transferMessage, color = colors.purpleBright, fontSize = 12.sp)
                }
                Text(session.status, color = colors.textPrimary, fontSize = 13.sp)
                if (session.diagnostics.isNotBlank()) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(12.dp)) {
                        Text(session.diagnostics, color = colors.textSecondary, fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp))
                    }
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText("MeloWare ICE", session.diagnosticReport())
                        )
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Sao chép chẩn đoán ICE")
                    }
                }
                session.error?.let { Text(it, color = colors.red, fontSize = 12.sp) }
                if (session.lastMessage.isNotBlank()) {
                    Text("Tin nhắn nhận được: ${session.lastMessage}",
                        color = colors.purpleBright, fontSize = 13.sp)
                }
                OutlinedButton(onClick = session::close, modifier = Modifier.fillMaxWidth()) {
                    Text("Ngắt / Tạo phiên mới")
                }
                Text("Dữ liệu TURN chỉ giữ trong bộ nhớ trong lúc mở cửa sổ này; không ghi vào Firebase.",
                    color = colors.textSecondary, fontSize = 11.sp)
            }
        }
    }
}
