package duc_phi.music

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.IOException
import java.io.RandomAccessFile
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
 * This variant uses STUN only. It never configures a TURN relay or its credentials.
 * Direct P2P may fail between some mobile and Wi-Fi networks because of CGNAT/firewalls.
 * Files are shared only after the owner chooses a directory via Android SAF.
 * Receiver downloads a song or folder sequentially with SHA-256 integrity checking.
 * Online listening uses a temporary progressive file exposed to Media3 through a
 * blocking custom DataSource; it can start before the whole file is transferred.
 * Only one DataChannel audio transfer is in flight, never Firebase audio bytes.
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
private const val MW_STREAM_START_BYTES = 256 * 1024L
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

private enum class AudioReceiveMode { DOWNLOAD, ONLINE }

/** The growing temporary file is read by Media3 while WebRTC writes to it. */
private class OnlineAudioCache(val file: File, val size: Long, val name: String) {
    @Volatile var availableBytes: Long = 0L
    @Volatile var verified: Boolean = false
    @Volatile var cancelled: Boolean = false
    @Volatile var playbackStarted: Boolean = false
}

/** Backpressure: Media3's extractor waits for more WebRTC bytes instead of reaching early EOF. */
@OptIn(UnstableApi::class)
private class MeloWareOnlineDataSource(private val cache: OnlineAudioCache) : BaseDataSource(true) {
    private var currentUri: Uri? = null
    private var file: RandomAccessFile? = null
    private var position = 0L
    private var remaining = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        if (dataSpec.position > cache.size) throw IOException("Vị trí phát vượt quá dung lượng bài nhạc")
        transferInitializing(dataSpec)
        currentUri = dataSpec.uri
        file = RandomAccessFile(cache.file, "r")
        position = dataSpec.position
        file!!.seek(position)
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            cache.size - position
        } else minOf(cache.size - position, dataSpec.length)
        opened = true
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val start = System.currentTimeMillis()
        while (true) {
            if (cache.cancelled) throw IOException("Luồng nhạc đã dừng")
            if (remaining == 0L && cache.verified) return -1
            val ready = cache.availableBytes - position
            if (remaining > 0L && ready > 0L) {
                val count = minOf(length.toLong(), ready, remaining).toInt()
                val read = file?.read(buffer, offset, count) ?: throw IOException("Tệp tạm đã đóng")
                if (read > 0) {
                    remaining -= read
                    position += read
                    bytesTransferred(read)
                    return read
                }
            }
            if (cache.verified && cache.availableBytes <= position && remaining > 0L) {
                throw IOException("File stream bị thiếu dữ liệu")
            }
            if (System.currentTimeMillis() - start > 45000L) {
                throw IOException("Quá thời gian chờ dữ liệu nhạc từ máy A")
            }
            try { Thread.sleep(45) } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Đã ngắt phát nhạc")
            }
        }
    }

    override fun getUri(): Uri? = currentUri
    override fun close() {
        runCatching { file?.close() }
        file = null
        currentUri = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}

private class IncomingAudio(
    val id: String,
    val name: String,
    val mime: String,
    val size: Long,
    val temporary: File,
    val output: FileOutputStream,
    val folder: String = "",
    val online: OnlineAudioCache? = null,
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

private fun safeMusicFolder(folder: String): String = folder.replace('\\', '/')
    .split('/').filter { it.isNotBlank() && it != "." && it != ".." }
    .take(10).joinToString("/") { safeMusicName(it).take(48) }

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
@OptIn(UnstableApi::class)
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

    // Sender: only SAF-selected audio is offered. Receiver: music is saved after integrity check.
    var sharedFolder by mutableStateOf("")
        private set
    var sharedCount by mutableStateOf(0)
        private set
    val remoteAudio = mutableStateListOf<InternetRemoteAudio>()
    var remoteRootFolder by mutableStateOf("Nhạc máy A")
        private set
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

    // The DataChannel protocol serializes file transfers to prevent mixing binary chunks.
    private var requestMode = AudioReceiveMode.DOWNLOAD
    private val pendingDownloads = ArrayDeque<InternetRemoteAudio>()
    private val pendingOnline = ArrayDeque<InternetRemoteAudio>()
    var batchDownloaded by mutableStateOf(0)
        private set
    var batchTotal by mutableStateOf(0)
        private set
    var onlineName by mutableStateOf("")
        private set
    var onlineActive by mutableStateOf(false)
        private set
    var onlinePlaying by mutableStateOf(false)
        private set
    var onlineTrackNumber by mutableStateOf(0)
        private set
    var onlineTrackTotal by mutableStateOf(0)
        private set
    val onlineUpcomingCount: Int get() = pendingOnline.size
    private var onlinePlayer: ExoPlayer? = null
    private var onlineCache: OnlineAudioCache? = null
    private var cancelRequested = false

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
        sendAudioControl("list_begin") { it.put("count", sharedAudio.size).put("root", sharedFolder) }
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

    private fun filesInFolder(path: String): List<InternetRemoteAudio> = remoteAudio
        .filter { it.folder.startsWith(path) }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.folder + it.name })

    fun downloadAudio(audio: InternetRemoteAudio) {
        if (onlineActive) {
            transferMessage = "Hãy dừng nghe online trước khi tải bài hát."
            return
        }
        if (transferBusy) return
        pendingDownloads.clear()
        batchTotal = 0
        batchDownloaded = 0
        requestAudio(audio, AudioReceiveMode.DOWNLOAD)
    }

    fun downloadFolder(path: String) {
        if (onlineActive || transferBusy || !dataChannelOpen) {
            transferMessage = "Hãy hoàn tất tác vụ đang chạy trước khi tải thư mục."
            return
        }
        val songs = filesInFolder(path)
        if (songs.isEmpty()) {
            transferMessage = "Thư mục chưa có bài nhạc được chia sẻ."
            return
        }
        pendingDownloads.clear()
        pendingDownloads.addAll(songs)
        batchTotal = songs.size
        batchDownloaded = 0
        startNextDownload()
    }

    private fun startNextDownload() {
        if (pendingDownloads.isEmpty()) {
            if (batchTotal > 0) transferMessage = "Đã tải xong $batchDownloaded/$batchTotal bài vào thư viện."
            return
        }
        if (transferBusy || requestedFileId != null) return
        requestAudio(pendingDownloads.removeFirst(), AudioReceiveMode.DOWNLOAD)
    }

    fun playAudio(audio: InternetRemoteAudio) {
        if (transferBusy) {
            transferMessage = "Đang truyền nhạc. Hãy đợi hoàn thành rồi chọn bài khác."
            return
        }
        pendingOnline.clear()
        onlineTrackTotal = 1
        onlineTrackNumber = 1
        requestOnline(audio)
    }

    fun playFolder(path: String) {
        if (transferBusy) {
            transferMessage = "Đang truyền nhạc. Hãy đợi hoàn thành trước khi phát thư mục."
            return
        }
        val songs = filesInFolder(path)
        if (songs.isEmpty()) {
            transferMessage = "Thư mục này chưa có bài để phát."
            return
        }
        pendingOnline.clear()
        pendingOnline.addAll(songs)
        onlineTrackTotal = songs.size
        onlineTrackNumber = 0
        startNextOnline()
    }

    private fun startNextOnline() {
        if (transferBusy || requestedFileId != null) return
        if (pendingOnline.isEmpty()) {
            onlinePlaying = false
            transferMessage = "Đã phát hết danh sách nhạc online."
            return
        }
        onlineTrackNumber++
        requestOnline(pendingOnline.removeFirst())
    }

    fun nextOnline() {
        if (!transferBusy && pendingOnline.isNotEmpty()) startNextOnline()
    }

    private fun requestOnline(audio: InternetRemoteAudio) {
        if (transferBusy) return
        clearOnlinePlayer()
        onlineName = audio.name
        onlineActive = true
        onlinePlaying = false
        requestAudio(audio, AudioReceiveMode.ONLINE)
    }

    private fun requestAudio(audio: InternetRemoteAudio, mode: AudioReceiveMode) {
        if (role != InternetRole.GUEST || !dataChannelOpen || requestedFileId != null || transferBusy) return
        if (audio.size !in 1..MW_AUDIO_MAX_FILE_BYTES) {
            transferMessage = "Bản thử nghiệm hỗ trợ tối đa 32 MB/bài."
            return
        }
        requestMode = mode
        requestedFileId = audio.id
        transferBusy = true
        cancelRequested = false
        transferProgress = 0f
        transferMessage = if (mode == AudioReceiveMode.ONLINE) {
            "Đang đệm nhạc để phát online: ${audio.name}"
        } else {
            "Đang tải: ${audio.name}" + if (batchTotal > 0) " (${batchDownloaded + 1}/$batchTotal)" else ""
        }
        if (!sendAudioControl("file_request") { it.put("id", audio.id) }) {
            requestedFileId = null
            transferBusy = false
            transferMessage = "Không gửi được yêu cầu đến máy A."
        }
    }

    private fun beginOnlinePlayer(cache: OnlineAudioCache) {
        if (cache.playbackStarted || cache.cancelled || role != InternetRole.GUEST) return
        cache.playbackStarted = true
        onlineCache = cache
        runCatching {
            val engine = ExoPlayer.Builder(context).build()
            engine.setAudioAttributes(
                AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(), true
            )
            engine.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    onMain { if (onlinePlayer === engine) onlinePlaying = isPlaying }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) onMain {
                        if (onlinePlayer === engine) {
                            onlinePlaying = false
                            if (pendingOnline.isNotEmpty()) startNextOnline()
                        }
                    }
                }
                override fun onPlayerError(error: PlaybackException) {
                    Log.e(MELOWARE_P2P_TAG, "Online ExoPlayer failed", error)
                    onMain {
                        if (onlinePlayer === engine) {
                            onlinePlaying = false
                            transferMessage = "Không thể phát bài này. Bạn có thể thử tải về máy."
                        }
                    }
                }
            })
            onlinePlayer = engine
            val uri = Uri.parse("meloware-online://audio/${Uri.encode(cache.name)}")
            val source = ProgressiveMediaSource.Factory(DataSource.Factory {
                MeloWareOnlineDataSource(cache)
            }).createMediaSource(MediaItem.fromUri(uri))
            engine.setMediaSource(source)
            engine.prepare()
            engine.playWhenReady = true
        }.onFailure {
            Log.e(MELOWARE_P2P_TAG, "Cannot start streamed audio", it)
            transferMessage = "Không mở được trình phát online."
            onlinePlaying = false
        }
    }

    fun toggleOnlinePlayback() {
        val engine = onlinePlayer ?: return
        if (engine.playbackState == Player.STATE_ENDED) engine.seekTo(0)
        engine.playWhenReady = !engine.playWhenReady
    }

    private fun clearOnlinePlayer() {
        val previous = onlineCache
        previous?.cancelled = true // unblock any DataSource.read() before release() waits
        onlineCache = null
        runCatching { onlinePlayer?.stop() }
        runCatching { onlinePlayer?.release() }
        onlinePlayer = null
        if (previous != null && incoming?.online !== previous) {
            fileIo.execute { runCatching { previous.file.delete() } }
        }
        onlinePlaying = false
    }

    fun cancelDownload() {
        pendingDownloads.clear()
        batchTotal = 0
        batchDownloaded = 0
        if (requestedFileId != null && transferBusy &&
            requestMode == AudioReceiveMode.DOWNLOAD) {
            cancelRequested = true
            sendAudioControl("file_cancel") { it.put("id", requestedFileId) }
            transferMessage = "Đang dừng tải bài hát..."
        }
    }

    fun stopOnline() {
        pendingOnline.clear()
        onlineTrackTotal = 0
        onlineTrackNumber = 0
        onlineActive = false
        val streaming = requestedFileId != null && transferBusy && requestMode == AudioReceiveMode.ONLINE
        clearOnlinePlayer()
        if (streaming) {
            cancelRequested = true
            sendAudioControl("file_cancel") { it.put("id", requestedFileId) }
            transferMessage = "Đang dừng luồng nhạc online..."
        } else {
            onlineName = ""
            transferMessage = "Đã dừng phát nhạc online."
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
                remoteRootFolder = msg.optString("root").take(70).ifBlank { "Nhạc máy A" }
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
                    .put("folder", item.folder).put("mime", item.mime).put("size", item.size) }
            }
            "file_begin" -> if (role == InternetRole.GUEST) {
                val size = msg.optLong("size", -1L)
                if (id != requestedFileId || size !in 1..MW_AUDIO_MAX_FILE_BYTES || incoming != null) {
                    sendAudioControl("file_error") { it.put("text", "Thông tin file không hợp lệ") }
                    return
                }
                val name = safeMusicName(msg.optString("name"))
                val mime = msg.optString("mime").take(100).ifBlank { "audio/mpeg" }
                val folder = safeMusicFolder(msg.optString("folder").take(160))
                fileIo.execute {
                    try {
                        val temporary = File.createTempFile("meloware_music_", ".part", context.cacheDir)
                        val cache = if (requestMode == AudioReceiveMode.ONLINE) {
                            OnlineAudioCache(temporary, size, name)
                        } else null
                        incoming = IncomingAudio(
                            id, name, mime, size, temporary, FileOutputStream(temporary), folder, cache
                        )
                        onMain {
                            if (sequence == token && requestedFileId == id && !cancelRequested) {
                                if (cache != null) onlineCache = cache
                                transferMessage = if (cache != null) "Đang đệm để nghe online: $name"
                                    else "Đang tải: $name"
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
                val completed = outgoing
                outgoing = null
                fileIo.execute { runCatching { completed?.input?.close() } }
                transferBusy = false
                transferProgress = 1f
                transferMessage = "Máy B đã nhận xong bài hát."
            }
            "file_cancel" -> if (role == InternetRole.HOST) {
                val old = outgoing
                if (old != null && old.item.id == id) {
                    // Finish queued send batches before replying on the same ordered channel.
                    fileIo.execute {
                        runCatching { old.input?.close() }
                        onMain {
                            if (outgoing === old) outgoing = null
                            transferBusy = false
                            sendAudioControl("file_cancelled") { it.put("id", id) }
                        }
                    }
                } else sendAudioControl("file_cancelled") { it.put("id", id) }
            }
            "file_cancelled" -> if (role == InternetRole.GUEST && requestedFileId == id) {
                requestedFileId = null
                transferBusy = false
                cancelRequested = false
                onlineName = ""
                onlineActive = false
                pendingDownloads.clear()
                fileIo.execute { discardTransfers() }
                transferMessage = "Đã dừng luồng nhạc."
            }
            "file_error" -> {
                val reason = msg.optString("text").take(180)
                transferMessage = "Không thể truyền bài hát: $reason"
                transferBusy = false
                requestedFileId = null
                pendingDownloads.clear()
                pendingOnline.clear()
                if (requestMode == AudioReceiveMode.ONLINE) {
                    onlineActive = false
                    clearOnlinePlayer()
                }
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
                current.online?.let { stream ->
                    stream.availableBytes = current.received
                    if (current.received >= minOf(current.size, MW_STREAM_START_BYTES) &&
                        !stream.playbackStarted && !cancelRequested) {
                        onMain {
                            if (sequence == token && onlineCache === stream &&
                                !cancelRequested && onlineActive) beginOnlinePlayer(stream)
                        }
                    }
                }
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
            if (current.received != current.size) throw IOException("File nhận thiếu dữ liệu")
            if (sha256Hex(current.digest.digest()) != expectedHash) throw IOException("SHA-256 không khớp")
            val path = if (current.online == null) publishVerifiedAudio(current) else "bộ nhớ đệm phát online"
            if (current.online == null) current.temporary.delete()
            current.online?.verified = true
            incoming = null
            onMain {
                if (sequence != token) return@onMain
                transferBusy = false
                transferProgress = 1f
                requestedFileId = null
                if (current.online != null) {
                    transferMessage = "Đã đệm xong '${current.name}'. Đang nghe online."
                    if (!current.online.playbackStarted && onlineActive) beginOnlinePlayer(current.online)
                } else {
                    transferMessage = "Đã lưu '${current.name}' vào $path."
                    if (batchTotal > 0) batchDownloaded++
                }
                sendAudioControl("file_saved") { it.put("id", current.id) }
                if (current.online != null && onlinePlayer?.playbackState == Player.STATE_ENDED &&
                    pendingOnline.isNotEmpty()) {
                    main.post { if (sequence == token && !transferBusy) startNextOnline() }
                }
                if (current.online == null && pendingDownloads.isNotEmpty()) {
                    main.post { if (sequence == token) startNextDownload() }
                } else if (current.online == null && batchTotal > 0) {
                    transferMessage = "Đã tải xong $batchDownloaded/$batchTotal bài trong thư mục."
                }
            }
        } catch (cause: Exception) {
            current.online?.cancelled = true
            onMain { if (sequence == token) rejectAudio("Không thể hoàn tất bài hát: ${cause.localizedMessage}") }
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
                val relative = safeMusicFolder(current.folder)
                put(MediaStore.Audio.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MUSIC}/MeloWare Internet/" +
                        (if (relative.isBlank()) "" else "$relative/"))
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val dest = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw java.io.IOException("MediaStore không tạo được file")
            try {
                resolver.openOutputStream(dest, "w")?.use { output ->
                    FileInputStream(current.temporary).use { it.copyTo(output) }
                } ?: throw java.io.IOException("Không mở được file âm thanh đích")
                resolver.update(dest, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
                return "Music/MeloWare Internet" +
                    (if (current.folder.isBlank()) "" else "/${safeMusicFolder(current.folder)}")
            } catch (e: Exception) {
                resolver.delete(dest, null, null)
                throw e
            }
        }
        // API 23-28: public Music requires WRITE_EXTERNAL_STORAGE (already in the manifest).
        val canWritePublic = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val base = if (canWritePublic) {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MeloWare Internet")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MeloWare Internet")
        }
        val folder = if (current.folder.isBlank()) base else File(base, safeMusicFolder(current.folder))
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
        pendingDownloads.clear()
        pendingOnline.clear()
        if (requestMode == AudioReceiveMode.ONLINE) {
            onlineActive = false
            clearOnlinePlayer()
        }
        sendAudioControl("file_error") { it.put("text", reason.take(150)) }
        fileIo.execute { discardTransfers() }
    }

    private fun discardTransfers() {
        runCatching { outgoing?.input?.close() }
        outgoing = null
        val old = incoming
        incoming = null
        old?.online?.cancelled = true
        runCatching { old?.output?.close() }
        runCatching { old?.temporary?.delete() }
    }

    fun shutdown() {
        close()
        fileIo.shutdown()
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
                info("Đã tạo phòng. Nhấn 'Chia sẻ mã' để gửi cho máy B.")
            }.addOnFailureListener {
                if (sequence == token) fail("Không thể tạo phòng; kiểm tra Firebase Rules", it)
            }
        }
    }

    fun joinRoom() {
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
            // STUN-only P2P. ICE cannot use relay candidates on restricted networks.
            val iceServers = listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
            Log.i(MELOWARE_P2P_TAG, "Direct STUN-only WebRTC; no TURN service configured")
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
                                "ICE FAILED: Không tìm được candidate công khai từ STUN. " +
                                    "Hãy kiểm tra mạng hoặc thử Wi-Fi/5G khác."
                            } else {
                                "ICE FAILED: Hai mạng không kết nối P2P trực tiếp được. " +
                                    "Chế độ này không sử dụng TURN; hãy thử mạng khác."
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
        clearOnlinePlayer()
        pendingDownloads.clear()
        pendingOnline.clear()
        batchTotal = 0
        batchDownloaded = 0
        onlineActive = false
        onlinePlaying = false
        onlineName = ""
        onlineTrackTotal = 0
        onlineTrackNumber = 0
        cancelRequested = false
        remoteRootFolder = "Nhạc máy A"
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
private fun MwLabel(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, maxLines = 1, fontSize = 12.sp, overflow = TextOverflow.Ellipsis)
    }
}

/** Normal user flow has no SDP, ICE diagnostics, or technical error dump. */
@Composable
fun MeloWareInternetDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val session = remember(context) { MeloWareInternetSession(context.applicationContext) }
    DisposableEffect(session) { onDispose { session.shutdown() } }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) session.chooseAudioFolder(uri)
    }
    var selectedRole by remember { mutableStateOf(InternetRole.HOST) }
    var currentFolder by remember { mutableStateOf("") }
    var remoteSearch by remember { mutableStateOf("") }
    var pendingFolderDownload by remember { mutableStateOf<String?>(null) }
    val isOnline = session.dataChannelOpen
    val hasRoom = session.roomCode.isNotBlank()
    val canSwitchRole = !hasRoom && !session.busy

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 740.dp),
            color = colors.card,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Nhạc qua Internet", fontWeight = FontWeight.Bold, fontSize = 19.sp,
                            color = colors.textPrimary)
                        Text("Chia sẻ và nghe nhạc giữa hai mạng", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, colors.border)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (isOnline) Icons.Default.CheckCircle else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) colors.green else colors.textSecondary,
                            modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isOnline) "Đã kết nối P2P" else if (session.error != null)
                            "Chưa thể kết nối" else if (session.busy || hasRoom)
                            "Đang chờ kết nối" else "Chưa kết nối",
                            color = if (isOnline) colors.green else colors.textSecondary,
                            fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        if (hasRoom) IconButton(onClick = {
                            session.close()
                            currentFolder = ""
                            remoteSearch = ""
                            pendingFolderDownload = null
                        }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Ngắt kết nối",
                                tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Two clear roles instead of showing both machine forms simultaneously.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedRole == InternetRole.HOST) {
                        Button(onClick = {}, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)) {
                            MwLabel(Icons.Default.FolderOpen, "Máy A · Chia sẻ")
                        }
                    } else {
                        OutlinedButton(onClick = { if (canSwitchRole) selectedRole = InternetRole.HOST },
                            enabled = canSwitchRole, modifier = Modifier.weight(1f)) {
                            MwLabel(Icons.Default.FolderOpen, "Máy A · Chia sẻ")
                        }
                    }
                    if (selectedRole == InternetRole.GUEST) {
                        Button(onClick = {}, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)) {
                            MwLabel(Icons.Default.Headphones, "Máy B · Nghe")
                        }
                    } else {
                        OutlinedButton(onClick = { if (canSwitchRole) selectedRole = InternetRole.GUEST },
                            enabled = canSwitchRole, modifier = Modifier.weight(1f)) {
                            MwLabel(Icons.Default.Headphones, "Máy B · Nghe")
                        }
                    }
                }

                if (selectedRole == InternetRole.HOST) {
                    if (!hasRoom) {
                        Button(onClick = session::createRoom, enabled = !session.busy,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.green)) {
                            MwLabel(Icons.Default.Link, "Tạo mã kết nối")
                        }
                    }
                    if (hasRoom && session.isHost) {
                        Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.border)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text("MÃ KẾT NỐI", color = colors.textSecondary, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold)
                                Text(session.roomCode.chunked(4).joinToString("-"),
                                    color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare Internet", session.roomCode))
                                    }, modifier = Modifier.weight(1f)) {
                                        MwLabel(Icons.Default.ContentCopy, "Sao chép")
                                    }
                                    Button(onClick = {
                                        // Chia se duy nhat ma phong (khong kem loi nhan / huong dan).
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, session.roomCode)
                                        }
                                        runCatching { context.startActivity(Intent.createChooser(intent, "Gửi mã MeloWare")) }
                                            .onFailure { Log.e(MELOWARE_P2P_TAG, "Share failed", it) }
                                    }, modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)) {
                                        MwLabel(Icons.Default.Share, "Chia sẻ mã")
                                    }
                                }
                                Text("Gửi mã bằng Zalo, Messenger, Gmail hoặc ứng dụng chia sẻ khác.",
                                    color = colors.textSecondary, fontSize = 11.sp)
                            }
                        }
                        if (session.guestRequested && !session.approved) {
                            Button(onClick = session::approveGuest, modifier = Modifier.fillMaxWidth(),
                                enabled = !session.busy) {
                                MwLabel(Icons.Default.CheckCircle, "Chấp nhận máy B")
                            }
                        }
                        Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.border)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null,
                                        tint = colors.purpleBright, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text("Thư mục chia sẻ", color = colors.textPrimary, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold)
                                }
                                Text(if (session.sharedFolder.isNotBlank())
                                    "${session.sharedFolder} · ${session.sharedCount} bài" else
                                    "Chọn thư mục nhạc trên máy A để máy B xem và phát.",
                                    color = colors.textSecondary, fontSize = 12.sp)
                                OutlinedButton(onClick = { folderPicker.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                                    MwLabel(Icons.Default.FolderOpen,
                                        if (session.sharedFolder.isBlank()) "Chọn thư mục nhạc" else "Đổi thư mục")
                                }
                            }
                        }
                    }
                } else {
                    if (!hasRoom) {
                        OutlinedTextField(value = session.inputCode,
                            onValueChange = { session.inputCode = it.take(23) },
                            label = { Text("Mã kết nối nhận từ máy A") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            currentFolder = ""
                            remoteSearch = ""
                            session.joinRoom()
                        }, enabled = !session.busy && session.inputCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.green)) {
                            MwLabel(Icons.Default.Link, "Kết nối với máy A")
                        }
                    }
                    if (hasRoom && !session.isHost) {
                        Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.border)) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Folder, contentDescription = null,
                                        tint = colors.purpleBright, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(session.remoteRootFolder, color = colors.textPrimary,
                                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f))
                                    IconButton(onClick = session::refreshRemoteAudio,
                                        enabled = isOnline, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới danh sách",
                                            tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                                    }
                                }
                                if (session.remoteAudio.isNotEmpty()) {
                                    if (currentFolder.isNotBlank()) {
                                        Row(Modifier.fillMaxWidth().clickable {
                                            val parent = currentFolder.trimEnd('/').substringBeforeLast('/', "")
                                            currentFolder = if (parent.isBlank()) "" else "$parent/"
                                            remoteSearch = ""
                                        }.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null,
                                                tint = colors.purpleBright, modifier = Modifier.size(19.dp))
                                            Text("Quay lại / ${currentFolder.trimEnd('/').substringAfterLast('/')}",
                                                fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                    val folderSongs = session.remoteAudio.filter { it.folder.startsWith(currentFolder) }
                                    Text("${folderSongs.size} bài trong thư mục" +
                                        (if (currentFolder.isBlank()) " và thư mục con" else " này"),
                                        fontSize = 11.sp, color = colors.textSecondary)
                                    Text("Bản thử nghiệm: tối đa 300 bài, 32 MB/bài.",
                                        fontSize = 10.sp, color = colors.textMuted)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        OutlinedButton(onClick = { pendingFolderDownload = currentFolder },
                                            enabled = isOnline && !session.transferBusy && !session.onlineActive &&
                                                folderSongs.isNotEmpty(), modifier = Modifier.weight(1f)) {
                                            MwLabel(Icons.Default.Download, "Tải thư mục")
                                        }
                                        Button(onClick = { session.playFolder(currentFolder) },
                                            enabled = isOnline && !session.transferBusy && folderSongs.isNotEmpty(),
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.purpleBright)) {
                                            MwLabel(Icons.Default.PlaylistPlay, "Phát thư mục")
                                        }
                                    }
                                    OutlinedTextField(value = remoteSearch,
                                        onValueChange = { remoteSearch = it.take(70) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        placeholder = { Text("Tìm bài hát") }, singleLine = true,
                                        modifier = Modifier.fillMaxWidth())
                                    if (remoteSearch.isBlank()) {
                                        val subfolders = folderSongs.mapNotNull { song ->
                                            val relative = song.folder.removePrefix(currentFolder)
                                            if (!relative.contains('/')) null else relative.substringBefore('/').takeIf { it.isNotBlank() }
                                        }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
                                        subfolders.forEach { child ->
                                            Row(Modifier.fillMaxWidth()
                                                .clickable { currentFolder += "$child/" }
                                                .padding(vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Folder, contentDescription = null,
                                                    tint = colors.purpleBright, modifier = Modifier.size(21.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(child, modifier = Modifier.weight(1f), fontSize = 12.sp,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    color = colors.textPrimary)
                                                Text("${folderSongs.count { it.folder.startsWith(currentFolder + child + "/") }} bài",
                                                    color = colors.textSecondary, fontSize = 10.sp)
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null,
                                                    tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    val visibleSongs = folderSongs.filter { song ->
                                        if (remoteSearch.isNotBlank()) {
                                            song.name.contains(remoteSearch, ignoreCase = true) ||
                                                song.folder.contains(remoteSearch, ignoreCase = true)
                                        } else song.folder == currentFolder
                                    }
                                    visibleSongs.take(40).forEach { item ->
                                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null,
                                                tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(7.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(item.name, color = colors.textPrimary,
                                                    fontSize = 12.sp, maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis)
                                                Text(audioSize(item.size), color = colors.textSecondary, fontSize = 10.sp)
                                            }
                                            IconButton(onClick = { session.playAudio(item) },
                                                enabled = isOnline && !session.transferBusy,
                                                modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.PlayArrow,
                                                    contentDescription = "Nghe online ${item.name}",
                                                    tint = colors.green, modifier = Modifier.size(21.dp))
                                            }
                                            IconButton(onClick = { session.downloadAudio(item) },
                                                enabled = isOnline && !session.transferBusy && !session.onlineActive,
                                                modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Download,
                                                    contentDescription = "Tải ${item.name}",
                                                    tint = colors.purpleBright, modifier = Modifier.size(19.dp))
                                            }
                                        }
                                    }
                                    if (visibleSongs.size > 40) {
                                        Text("Đang hiển thị 40/${visibleSongs.size} bài. Hãy tìm theo tên.",
                                            fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                } else {
                                    Text("Đang đợi danh sách thư mục từ máy A...",
                                        color = colors.textSecondary, fontSize = 12.sp)
                                    OutlinedButton(onClick = session::refreshRemoteAudio, enabled = isOnline,
                                        modifier = Modifier.fillMaxWidth()) {
                                        MwLabel(Icons.Default.Refresh, "Làm mới danh sách")
                                    }
                                }
                            }
                        }
                    }
                }

                if (session.onlineActive) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colors.border)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Headphones, contentDescription = null,
                                    tint = colors.green, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                                Text("ĐANG NGHE ONLINE", color = colors.green, fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp)
                                Spacer(Modifier.weight(1f))
                                Text("${session.onlineTrackNumber}/${session.onlineTrackTotal}",
                                    color = colors.textSecondary, fontSize = 11.sp)
                            }
                            Text(session.onlineName, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium, color = colors.textPrimary, fontSize = 13.sp)
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = session::toggleOnlinePlayback) {
                                    Icon(if (session.onlinePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (session.onlinePlaying) "Tạm dừng" else "Tiếp tục",
                                        tint = colors.green)
                                }
                                IconButton(onClick = session::nextOnline,
                                    enabled = !session.transferBusy && session.onlineUpcomingCount > 0) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Bài tiếp theo",
                                        tint = colors.textPrimary)
                                }
                                IconButton(onClick = session::stopOnline) {
                                    Icon(Icons.Default.Stop, contentDescription = "Dừng phát online",
                                        tint = colors.red)
                                }
                            }
                        }
                    }
                }

                if (hasRoom && (session.transferBusy || session.transferMessage.isNotBlank())) {
                    if (session.batchTotal > 0) {
                        Text("Đã tải ${session.batchDownloaded}/${session.batchTotal} bài trong thư mục",
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                    if (session.transferBusy) {
                        LinearProgressIndicator(progress = session.transferProgress,
                            modifier = Modifier.fillMaxWidth())
                        if (!session.onlineActive && !session.isHost) {
                            TextButton(onClick = session::cancelDownload) {
                                MwLabel(Icons.Default.Close, "Hủy tải nhạc")
                            }
                        }
                    }
                    if (session.transferMessage.isNotBlank() && session.transferMessage != "Chưa chọn thư mục chia sẻ") {
                        Text(session.transferMessage, color = colors.textSecondary, fontSize = 11.sp,
                            maxLines = 3)
                    }
                }
                if (hasRoom) {
                    Text("Nhạc truyền qua WebRTC P2P. Đóng màn hình này sẽ ngắt phiên và dừng phát.",
                        color = colors.textMuted, fontSize = 10.sp)
                }
            }
        }
    }
    if (pendingFolderDownload != null) {
        val folderPath = pendingFolderDownload.orEmpty()
        val songs = session.remoteAudio.filter { it.folder.startsWith(folderPath) }
        val byteTotal = songs.sumOf { it.size }
        AlertDialog(
            onDismissRequest = { pendingFolderDownload = null },
            title = { Text("Tải toàn bộ thư mục") },
            text = {
                Text("Tải ${songs.size} bài (${audioSize(byteTotal)}) từ máy A? " +
                    "Dữ liệu sử dụng Internet trên cả hai máy. File được lưu vào Music/MeloWare Internet.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingFolderDownload = null
                    session.downloadFolder(folderPath)
                }) { MwLabel(Icons.Default.Download, "Bắt đầu tải") }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolderDownload = null }) {
                    MwLabel(Icons.Default.Close, "Hủy")
                }
            }
        )
    }
}
