@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package duc_phi.music

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.ImageDecoder
import android.media.RingtoneManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer as AndroidEqualizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import android.util.Log
import android.webkit.WebView
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata as CastMediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import java.util.ArrayDeque
import java.util.Locale
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.pow

internal enum class MusicScreen {
    HOME,
    LIBRARY,
    NOW_PLAYING
}

internal enum class LibraryTab {
    SONGS,
    ALBUMS,
    ARTISTS,
    FOLDERS,
    PLAYLISTS
}

internal enum class LibrarySource {
    ALL,
    LOCAL,
    PERSONAL_DRIVE,
    PUBLIC_DRIVE,
    INTERNET,
    DOWNLOADED,
    FAVORITES
}

internal enum class RepeatSetting {
    OFF,
    ALL,
    ONE
}

internal enum class OnlineSearchMode {
    APPLE_MUSIC,
    MOVIE
}

internal enum class SongSource {
    LOCAL_MEDIASTORE,
    PERSONAL_DRIVE,
    PUBLIC_DRIVE,
    DOWNLOADED,
    ONLINE_HTTP,
    INTERNET_P2P,
    MOVIE,
    APP_RESOURCE
}

/**
 * Sound Studio 2.0 uses a 10-point reference curve and maps it logarithmically to
 * the real Equalizer bands reported by each Android device. A phone with 5 bands
 * still uses all 10 preset control points as an interpolation source; a device with
 * more bands receives a smoother, more detailed curve.
 */
private val MELOWAVE_EQ_REFERENCE_HZ = intArrayOf(
    31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000
)
private val MELOWAVE_EQ_FIVE_HZ = intArrayOf(60, 230, 910, 3_600, 14_000)
private const val MELOWAVE_EQ_CURVE_POINTS = 10
private const val MELOWAVE_EQ_TRANSITION_MS = 180L

private fun sanitizeEqCurve(values: IntArray): IntArray = IntArray(MELOWAVE_EQ_CURVE_POINTS) { index ->
    values.getOrElse(index) { 0 }.coerceIn(-6, 6)
}

private fun interpolateEqCurveDb(
    curve: IntArray,
    frequencyHz: Float,
    referenceHz: IntArray = MELOWAVE_EQ_REFERENCE_HZ
): Float {
    val safeCurve = sanitizeEqCurve(curve)
    val frequency = frequencyHz.coerceAtLeast(1f)
    if (frequency <= referenceHz.first()) return safeCurve.first().toFloat()
    if (frequency >= referenceHz.last()) return safeCurve.last().toFloat()
    val upper = referenceHz.indexOfFirst { frequency <= it }.coerceAtLeast(1)
    val lower = upper - 1
    val x0 = ln(referenceHz[lower].toDouble())
    val x1 = ln(referenceHz[upper].toDouble())
    val x = ln(frequency.toDouble())
    val ratio = ((x - x0) / (x1 - x0)).toFloat().coerceIn(0f, 1f)
    return safeCurve[lower] + (safeCurve[upper] - safeCurve[lower]) * ratio
}

private fun curveFromAnchors(anchorHz: IntArray, anchorDb: IntArray): IntArray {
    if (anchorHz.isEmpty() || anchorHz.size != anchorDb.size) return IntArray(MELOWAVE_EQ_CURVE_POINTS)
    return IntArray(MELOWAVE_EQ_CURVE_POINTS) { index ->
        val frequency = MELOWAVE_EQ_REFERENCE_HZ[index].toFloat()
        when {
            frequency <= anchorHz.first() -> anchorDb.first()
            frequency >= anchorHz.last() -> anchorDb.last()
            else -> {
                val upper = anchorHz.indexOfFirst { frequency <= it }.coerceAtLeast(1)
                val lower = upper - 1
                val x0 = ln(anchorHz[lower].toDouble())
                val x1 = ln(anchorHz[upper].toDouble())
                val x = ln(frequency.toDouble())
                val ratio = ((x - x0) / (x1 - x0)).toFloat().coerceIn(0f, 1f)
                (anchorDb[lower] + (anchorDb[upper] - anchorDb[lower]) * ratio).roundToInt()
            }
        }.coerceIn(-6, 6)
    }
}

private fun curveFromThreeBand(bass: Int, mid: Int, treble: Int): IntArray = curveFromAnchors(
    anchorHz = intArrayOf(100, 1_000, 10_000),
    anchorDb = intArrayOf(bass.coerceIn(-6, 6), mid.coerceIn(-6, 6), treble.coerceIn(-6, 6))
)

private fun curveFromFiveBand(values: IntArray): IntArray = curveFromAnchors(
    anchorHz = MELOWAVE_EQ_FIVE_HZ,
    anchorDb = IntArray(5) { values.getOrElse(it) { 0 }.coerceIn(-6, 6) }
)

private fun parseEqCurvePreference(raw: String?): IntArray? {
    if (raw.isNullOrBlank()) return null
    val values = raw.split(',').mapNotNull { it.trim().toIntOrNull() }
    if (values.size != MELOWAVE_EQ_CURVE_POINTS) return null
    return sanitizeEqCurve(values.toIntArray())
}

private fun eqCurvePreference(curve: IntArray): String = sanitizeEqCurve(curve).joinToString(",")

/** Outcome of a MediaStore operation. Android may require a system permission prompt. */
internal sealed class LocalSongMutationResult {
    object Completed : LocalSongMutationResult()
    object LibraryOnly : LocalSongMutationResult()
    data class NeedsConsent(val intentSender: android.content.IntentSender) : LocalSongMutationResult()
    data class Failed(val message: String) : LocalSongMutationResult()
}

internal data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val folderName: String = "",
    val uri: Uri,
    val durationMs: Long = 0L,
    val source: SongSource,
    val mimeType: String? = null,
    val artworkUrl: String? = null,
    val genre: String = "",
    val dateAddedMs: Long = 0L
) {
    val durationLabel: String
        get() = if (durationMs > 0L) formatDuration(durationMs) else "--:--"
}

internal data class MeloWareLanPeer(
    val name: String,
    val address: String,
    val port: Int,
    val token: String
)

internal data class MeloWareSharedTrack(
    val key: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String
)

/** Only audio folders selected explicitly on the sender are visible to the receiver. */
internal data class MeloWareLanFolder(val path: String, val count: Int)

internal data class MeloWareLanAudio(
    val key: String,
    val folder: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String
)

private data class LanSharedAudio(
    val key: String,
    val folder: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val uri: Uri
)

internal data class AudioTechnicalInfo(
    val qualityLabel: String,
    val extensionLabel: String,
    val sizeLabel: String,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val sizeBytes: Long? = null
)

/** Word-level timestamps are available only in enhanced LRC (<mm:ss.xx> tags). */
internal data class LyricWordTiming(
    val offset: Int,
    val timestampMs: Long
)

internal data class LyricLine(
    val timestampMs: Long?,
    val text: String,
    val wordTimings: List<LyricWordTiming> = emptyList()
)

internal data class LyricsContent(
    val recordId: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationMs: Long,
    val instrumental: Boolean,
    val synced: Boolean,
    val lines: List<LyricLine>,
    val sourceName: String = "LRCLIB"
)

internal data class MusicFolder(
    val name: String,
    val songs: List<SongItem>,
    val source: SongSource
)

internal data class SharedDriveLink(
    val folderId: String,
    val resourceKey: String?
)

internal data class SharedDriveFolder(
    val id: String,
    val name: String,
    val resourceKey: String?
)

internal data class SharedDriveEntry(
    val id: String,
    val name: String,
    val mimeType: String,
    val resourceKey: String?,
    val isFolder: Boolean,
    val canDownload: Boolean,
    val size: Long
)

internal data class PersonalDriveFolder(
    val id: String,
    val name: String
)

internal data class PersonalDriveEntry(
    val id: String,
    val name: String,
    val mimeType: String,
    val isFolder: Boolean,
    val canDownload: Boolean,
    val size: Long
)

private class PersonalDriveAuthorizationException(message: String) : IOException(message)

private val SUPPORTED_AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "m4b", "aac", "adts", "flac", "wav", "wave",
    "ogg", "oga", "opus", "amr", "3gp", "mp4", "webm", "mka",
    "mkv", "ac3", "ec3", "ac4", "ts", "mpeg", "mpg"
)

private fun extensionOf(fileName: String): String =
    fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)

private fun stripAudioExtension(fileName: String): String {
    val ext = extensionOf(fileName)
    return if (ext in SUPPORTED_AUDIO_EXTENSIONS) {
        fileName.substringBeforeLast('.').trim()
    } else {
        fileName.trim()
    }
}

private fun resolveAudioMime(displayName: String, providerMime: String?): String? {
    val normalized = providerMime
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase(Locale.ROOT)
    if (normalized?.startsWith("audio/") == true || normalized?.startsWith("video/") == true) {
        return normalized
    }
    return when (extensionOf(displayName)) {
        "mp3", "mpeg", "mpg" -> "audio/mpeg"
        "m4a", "m4b", "mp4" -> "audio/mp4"
        "aac", "adts" -> "audio/aac"
        "flac" -> "audio/flac"
        "wav", "wave" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "amr" -> "audio/amr"
        "3gp" -> "audio/3gpp"
        "webm" -> "audio/webm"
        "mka", "mkv" -> "audio/x-matroska"
        "ac3" -> "audio/ac3"
        "ec3" -> "audio/eac3"
        "ac4" -> "audio/ac4"
        "ts" -> "video/mp2t"
        else -> null
    }
}

private fun extensionForMime(mimeType: String?): String = when (
    mimeType?.substringBefore(';')?.lowercase(Locale.ROOT)
) {
    "audio/mpeg" -> "mp3"
    "audio/mp4" -> "m4a"
    "audio/aac" -> "aac"
    "audio/flac" -> "flac"
    "audio/wav", "audio/x-wav" -> "wav"
    "audio/ogg" -> "ogg"
    "audio/opus" -> "opus"
    "audio/amr" -> "amr"
    "audio/3gpp" -> "3gp"
    "audio/webm" -> "webm"
    "audio/x-matroska" -> "mka"
    "audio/ac3" -> "ac3"
    "audio/eac3" -> "ec3"
    "audio/ac4" -> "ac4"
    "video/mp2t" -> "ts"
    else -> "audio"
}

private fun isSupportedAudio(name: String, mimeType: String?): Boolean {
    if (mimeType?.startsWith("audio/", ignoreCase = true) == true) return true
    return extensionOf(name) in SUPPORTED_AUDIO_EXTENSIONS
}

private fun parseTitleArtist(displayName: String, fallbackArtist: String): Pair<String, String> {
    val clean = stripAudioExtension(displayName)
        .replace(Regex("""^\s*\d{1,3}\s*[.)_\-]+\s*"""), "")
        .trim()
        .ifBlank { "Bài hát không tên" }
    if (!clean.contains(" - ")) return clean to fallbackArtist
    val parts = clean.split(" - ", limit = 2)
    val first = parts[0].trim().ifBlank { "Bài hát không tên" }
    val second = parts[1].trim().ifBlank { fallbackArtist }
    return first to second
}

internal fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Stable
internal class MusicController(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    internal val smartPlaylists = SmartPlaylistRepository(context.applicationContext, scope)
    private var playbackJob: Job? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    // Smart Playlist listening session. Database writes are batched so playback stays smooth.
    private var smartTrackedSongId: String? = null
    private var smartTrackedListenMs: Long = 0L
    private var smartTrackedPersistedMs: Long = 0L
    private var smartTrackedLastTickElapsedMs: Long = 0L
    private var smartTrackedPlayCounted: Boolean = false
    private val artworkRequests = mutableSetOf<String>()
    private val cachedPlaybackUris = mutableMapOf<String, Uri>()
    private val activeTransformers = mutableSetOf<Transformer>()
    private val audioTechnicalInfoById = mutableStateMapOf<String, AudioTechnicalInfo>()
    private val lyricsBySongId = mutableStateMapOf<String, LyricsContent>()
    private val lyricsLoadingBySongId = mutableStateMapOf<String, Boolean>()
    private val lyricsErrorBySongId = mutableStateMapOf<String, String>()
    private val lyricsJobs = mutableMapOf<String, Job>()
    private var audioFxEngine = PersistentAudioFxEngine()
    private val laptopShareLazy = lazy { MeloWareLaptopClient(context.applicationContext) }
    val laptopShare: MeloWareLaptopClient get() = laptopShareLazy.value

    // Internet WebRTC belongs to the app controller, not to a transient dialog.
    private val internetSessionLazy = lazy(LazyThreadSafetyMode.NONE) {
        MeloWareInternetSession(context.applicationContext).apply {
            onClosed = { reason -> onInternetSessionClosed(reason) }
            onDownloaded = { scanLocalMusic() }
            onRemoteListChanged = { reconcileInternetRemoteList() }
        }
    }
    val internetSession: MeloWareInternetSession get() = internetSessionLazy.value
    val internetLinkedFolders = mutableStateListOf<String>()
    private var internetWasPlaying = false

    /** Remote songs are only mounted while the approved P2P connection is alive. */
    fun internetLibrarySongs(): List<SongItem> {
        if (!internetSessionLazy.isInitialized()) return emptyList()
        val session = internetSession
        if (!session.dataChannelOpen || internetLinkedFolders.isEmpty()) return emptyList()
        return session.remoteAudio.mapNotNull { audio ->
            val selected = internetLinkedFolders.firstOrNull { audio.folder.startsWith(it) }
                ?: return@mapNotNull null
            val (title, artist) = parseTitleArtist(
                audio.name.substringBeforeLast('.', audio.name), "Nhạc chia sẻ qua Internet"
            )
            SongItem(
                id = "meloware_internet_${session.roomCode}_${audio.id}",
                title = title,
                artist = artist,
                album = "MeloWare Internet",
                folderName = "Internet · ${session.remoteRootFolder}" +
                        if (selected.isBlank()) "" else " / ${selected.trimEnd('/')}",
                uri = Uri.parse("meloware-internet://audio/${audio.id}/${Uri.encode(audio.name)}"),
                source = SongSource.INTERNET_P2P,
                mimeType = audio.mime
            )
        }
    }

    private fun reconcileInternetRemoteList() {
        // A can refresh or replace its shared SAF folder without closing the room.
        val keys = internetSession.remoteAudio.mapTo(mutableSetOf()) { it.id }
        val playingId = currentSong?.id
        val lostCurrent = currentSong?.source == SongSource.INTERNET_P2P &&
                currentSong?.uri?.pathSegments?.firstOrNull() !in keys
        if (lostCurrent) {
            player.stop()
            internetSession.stopOnline()
            internetWasPlaying = false
            isPlaying = false
            isLoading = false
            statusMessage = "Máy A đã đổi thư mục chia sẻ. Danh sách nhạc đã cập nhật."
        }
        val dropped = queue.removeAll {
            it.source == SongSource.INTERNET_P2P && it.uri.pathSegments.firstOrNull() !in keys
        }
        if (dropped) {
            currentIndex = if (queue.isEmpty()) -1 else
                queue.indexOfFirst { it.id == playingId }.takeIf { it >= 0 } ?: 0
            saveQueue()
        }
    }

    fun addInternetFolderToLibrary(folder: String): Boolean {
        val session = internetSession
        if (!session.dataChannelOpen) {
            statusMessage = "Hãy kết nối với máy A trước khi thêm thư mục Internet."
            return false
        }
        if (session.remoteAudio.none { it.folder.startsWith(folder) }) {
            statusMessage = "Thư mục được chọn chưa có file nhạc để phát."
            return false
        }
        if (internetLinkedFolders.none { folder.startsWith(it) }) {
            // A selected parent replaces redundant descendant folders.
            internetLinkedFolders.removeAll { it.startsWith(folder) }
            internetLinkedFolders.add(folder)
        }
        librarySource = LibrarySource.INTERNET
        libraryTab = LibraryTab.FOLDERS
        libraryQuery = ""
        statusMessage = "Đã đưa thư mục nhạc Internet vào Thư viện."
        return true
    }

    fun playInternetFolder(folder: String) {
        if (!addInternetFolderToLibrary(folder)) return
        val keys = internetSession.remoteAudio.filter { it.folder.startsWith(folder) }.map { it.id }.toSet()
        val songs = internetLibrarySongs().filter { it.uri.pathSegments.firstOrNull() in keys }
        if (songs.isNotEmpty()) playCollection(songs, 0)
    }

    fun playInternetAudio(audioId: String) {
        val audio = internetSession.remoteAudio.firstOrNull { it.id == audioId } ?: return
        if (!addInternetFolderToLibrary(audio.folder)) return
        val collection = internetLibrarySongs()
        val song = collection.firstOrNull { it.uri.pathSegments.firstOrNull() == audioId } ?: return
        playSong(song, collection)
    }

    fun stopInternetPlayback() {
        if (currentSong?.source != SongSource.INTERNET_P2P) return
        player.stop()
        internetSession.stopOnline()
        internetWasPlaying = false
        isPlaying = false
        isLoading = false
    }

    private fun onInternetSessionClosed(reason: String?) {
        internetLinkedFolders.clear()
        val currentId = currentSong?.id
        val currentWasInternet = currentSong?.source == SongSource.INTERNET_P2P
        if (currentWasInternet) {
            player.stop()
            player.clearMediaItems()
            isPlaying = false
            isLoading = false
            positionMs = 0L
            durationMs = 1L
        }
        internetWasPlaying = false
        queue.removeAll { it.source == SongSource.INTERNET_P2P }
        recentSongs.removeAll { it.source == SongSource.INTERNET_P2P }
        currentIndex = if (queue.isEmpty()) -1 else
            queue.indexOfFirst { it.id == currentId }.takeIf { it >= 0 } ?: 0
        if (librarySource == LibrarySource.INTERNET) librarySource = LibrarySource.ALL
        saveQueue()
        if (!reason.isNullOrBlank()) statusMessage = reason
    }

    /** Online playback from PC Chrome via the phone-hosted local HTTP browser bridge. */
    fun playLaptopFile(selected: LaptopAudioEntry) {
        if (castConnected) {
            statusMessage = "Hãy ngắt Google Cast trước khi nghe nhạc máy tính trên điện thoại."
            return
        }
        if (selected.isFolder || !laptopShare.connected) {
            statusMessage = "Hãy kết nối máy tính trước khi phát nhạc."
            return
        }
        val tracks = laptopShare.entries.filter { !it.isFolder && it.size > 0L }.map { entry ->
            val titleArtist = parseTitleArtist(entry.name, "Máy tính")
            SongItem(
                id = "meloware_pc_" + entry.path.joinToString("/").sha256().take(32),
                title = titleArtist.first,
                artist = titleArtist.second,
                folderName = "Máy tính / " + entry.path.dropLast(1).joinToString("/"),
                uri = laptopShare.streamUrl(entry),
                source = SongSource.ONLINE_HTTP,
                mimeType = entry.mime
            )
        }
        val song = tracks.firstOrNull { it.id == "meloware_pc_" + selected.path.joinToString("/").sha256().take(32) }
        if (song == null) { statusMessage = "Không tìm thấy bài hát trong thư mục."; return }
        playSong(song, tracks)
    }

    val wifiLanDiscovery = MeloWareLanDiscovery(context.applicationContext)
    private var wifiAppShareRequested = false
    var wifiAppShareEnabled by mutableStateOf(false)
        private set
    var wifiReceivingPeer by mutableStateOf<MeloWareLanPeer?>(null)
        private set
    var wifiPeerMessage by mutableStateOf<String?>(null)
        private set
    private var wifiPeerReceiveJob: Job? = null
    private var wifiLastRemoteKey: String? = null

    // Selected MediaStore audio folders only. Nothing is shared without a choice on A.
    val wifiSharedFolderPaths = mutableStateListOf<String>()
    val wifiShareableFolders: List<MeloWareLanFolder>
        get() = localSongs
            .filter { it.source == SongSource.LOCAL_MEDIASTORE || it.source == SongSource.DOWNLOADED }
            .groupingBy { it.folderName.trim('/').ifBlank { "Nhạc trên máy" } }
            .eachCount()
            .map { (path, count) -> MeloWareLanFolder(path, count) }
            .sortedBy { it.path.lowercase(Locale.ROOT) }

    var wifiLibraryPeer by mutableStateOf<MeloWareLanPeer?>(null)
        private set
    val wifiRemoteFolders = mutableStateListOf<MeloWareLanFolder>()
    val wifiRemoteSongs = mutableStateListOf<MeloWareLanAudio>()
    var wifiRemoteFolderPath by mutableStateOf<String?>(null)
        private set
    var wifiRemoteLoading by mutableStateOf(false)
        private set
    var wifiRemoteError by mutableStateOf<String?>(null)
        private set
    var wifiRemoteNextOffset by mutableIntStateOf(-1)
        private set
    private var wifiLibraryJob: Job? = null

    private var localAudioServer: LocalAudioHttpServer? = null
    private var hostedSongId: String? = null
    private var wifiHostRequestedByUser = false
    var wifiHostActive by mutableStateOf(false)
        private set
    var wifiHostBusy by mutableStateOf(false)
        private set
    var wifiHostUrl by mutableStateOf<String?>(null)
        private set
    var wifiHostError by mutableStateOf<String?>(null)
        private set

    private var castContext: CastContext? = null
    private var castInitialized = false
    private var castSessionListener: SessionManagerListener<CastSession>? = null
    private var castStateListener: CastStateListener? = null
    private var remoteMediaCallback: RemoteMediaClient.Callback? = null
    private var remoteProgressListener: RemoteMediaClient.ProgressListener? = null
    var castConnected by mutableStateOf(false)
        private set
    var castConnecting by mutableStateOf(false)
        private set
    var castDeviceName by mutableStateOf<String?>(null)
        private set


    // During real Crossfade, the incoming ExoPlayer becomes the active player.
    // Compose observes the swap; the outgoing instance remains audible only during fading.
    private val mediaAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()
    var player: ExoPlayer by mutableStateOf(ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(mediaAudioAttributes, true)
    })
        private set

    val queue = mutableStateListOf<SongItem>()
    val localSongs = mutableStateListOf<SongItem>()
    val personalDriveSongs = mutableStateListOf<SongItem>()
    val publicDriveSongs = mutableStateListOf<SongItem>()
    val recentSongs = mutableStateListOf<SongItem>()
    val artworkById = mutableStateMapOf<String, String>()
    // Private, persistent covers selected by the user. This overrides Internet/embedded artwork.
    private val customArtworkByKey = mutableStateMapOf<String, String>()
    val favoriteIds = mutableStateMapOf<String, Boolean>()

    var currentScreen by mutableStateOf(MusicScreen.HOME)
        private set
    private var previousScreen by mutableStateOf(MusicScreen.HOME)

    var libraryTab by mutableStateOf(LibraryTab.SONGS)
    var librarySource by mutableStateOf(LibrarySource.ALL)
    var libraryQuery by mutableStateOf("")

    var currentIndex by mutableIntStateOf(-1)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var positionMs by mutableLongStateOf(0L)
        private set
    var durationMs by mutableLongStateOf(1L)
        private set
    var audioSessionId by mutableIntStateOf(0)
        private set

    // Equalizer state belongs to the player/controller, not to the dialog UI.
    // New installs start Flat and bypassed; audio changes only after the listener opts in.
    var equalizerBass by mutableIntStateOf(prefs.getInt(KEY_EQ_BASS, 0))
        private set
    var equalizerMid by mutableIntStateOf(prefs.getInt(KEY_EQ_MID, 0))
        private set
    var equalizerTreble by mutableIntStateOf(prefs.getInt(KEY_EQ_TREBLE, 0))
        private set
    var equalizerBand60 by mutableIntStateOf(prefs.getInt(KEY_EQ_BAND_60, 0))
        private set
    var equalizerBand230 by mutableIntStateOf(prefs.getInt(KEY_EQ_BAND_230, 0))
        private set
    var equalizerBand910 by mutableIntStateOf(prefs.getInt(KEY_EQ_BAND_910, 0))
        private set
    var equalizerBand3600 by mutableIntStateOf(prefs.getInt(KEY_EQ_BAND_3600, 0))
        private set
    var equalizerBand14000 by mutableIntStateOf(prefs.getInt(KEY_EQ_BAND_14000, 0))
        private set
    private var equalizerCurve10 by mutableStateOf(
        parseEqCurvePreference(prefs.getString(KEY_EQ_CURVE_10, null))
            ?: curveFromFiveBand(
                intArrayOf(
                    equalizerBand60,
                    equalizerBand230,
                    equalizerBand910,
                    equalizerBand3600,
                    equalizerBand14000
                )
            )
    )
    val equalizerResponseCurveDb: List<Int>
        get() = if (MeloWarePremiumAccess.equalizerRequiresPremium) {
            curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble).toList()
        } else {
            equalizerCurve10.toList()
        }
    var equalizerSelectedPresetIndex by mutableIntStateOf(prefs.getInt(KEY_EQ_PRESET, 1))
        private set
    var equalizerAdvancedMode by mutableStateOf(prefs.getBoolean(KEY_EQ_ADVANCED, false))
        private set
    var equalizerLoudnessEnabled by mutableStateOf(prefs.getBoolean(KEY_EQ_LOUDNESS, false))
        private set
    var equalizerLoudnessGainDb by mutableIntStateOf(
        prefs.getInt(KEY_EQ_LOUDNESS_GAIN_DB, 3).coerceIn(1, 6)
    )
        private set
    var equalizerBassBoostStrength by mutableIntStateOf(
        prefs.getInt(KEY_EQ_BASS_BOOST, 0).coerceIn(0, 100)
    )
        private set
    var equalizerVirtualizerStrength by mutableIntStateOf(
        prefs.getInt(KEY_EQ_VIRTUALIZER, 0).coerceIn(0, 100)
    )
        private set
    var equalizerVirtualizerAvailable by mutableStateOf(false)
        private set
    var equalizerVirtualizerStrengthSupported by mutableStateOf(false)
        private set
    private var _equalizerCompareOriginal by mutableStateOf(false)
    val equalizerCompareOriginal: Boolean
        get() = _equalizerCompareOriginal
    var equalizerAutoHeadroomEnabled by mutableStateOf(
        prefs.getBoolean(KEY_EQ_AUTO_HEADROOM, true)
    )
        private set
    var equalizerHeadroomDb by mutableStateOf(0f)
        private set
    var equalizerBassBoostAvailable by mutableStateOf(false)
        private set
    var equalizerBassBoostStrengthSupported by mutableStateOf(false)
        private set
    var equalizerActualFiveBandLabels by mutableStateOf(
        listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    )
        private set
    var equalizerEnabled by mutableStateOf(prefs.getBoolean(KEY_EQ_ENABLED, false))
        private set
    var equalizerReady by mutableStateOf(false)
        private set
    var equalizerHasControl by mutableStateOf(false)
        private set
    var equalizerError by mutableStateOf<String?>(null)
        private set
    var equalizerWarning by mutableStateOf<String?>(null)
        private set

    // Master Volume is independent from Android's system media volume.
    // It scales only MeloWave's local ExoPlayer output and remains active even when EQ is bypassed.
    var soundMasterVolumePercent by mutableIntStateOf(
        prefs.getInt(KEY_SOUND_MASTER_VOLUME, 100).coerceIn(0, 100)
    )
        private set

    // Sound Studio Pro: saved presets are keyed by the connected audio output,
    // not by the current song or an ephemeral Android audio-device id.
    private val systemAudioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var soundOutputName by mutableStateOf("Loa điện thoại")
        private set
    var soundOutputKey by mutableStateOf("speaker")
        private set
    var soundPresetSaved by mutableStateOf(false)
        private set
    // Read-only public Compose state: custom setSound... methods below are the only setters.
    // Using public var properties here generates JVM setters with the same names as those methods.
    private var soundAutoApplyDeviceEQState by mutableStateOf(prefs.getBoolean(KEY_SOUND_AUTO_EQ, true))
    val soundAutoApplyDeviceEQ: Boolean
        get() = soundAutoApplyDeviceEQState
    private var soundCrossfadeSecondsState by mutableIntStateOf(prefs.getInt(KEY_SOUND_CROSSFADE, 0).coerceIn(0, 10))
    val soundCrossfadeSeconds: Int
        get() = soundCrossfadeSecondsState
    private var soundDeviceCallbacksEnabled = false
    private var soundOutputInitialized = false
    private var lastAutoCrossfadeSongId: String? = null
    private val soundDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refreshSoundOutput()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refreshSoundOutput()
    }
    private var lastSoundDevicePollElapsedMs = 0L
    private var crossfadeJob: Job? = null
    private var crossfadeStandby: ExoPlayer? = null
    private var crossfadeOutgoing: ExoPlayer? = null
    private var crossfadeStandbyFxEngine: PersistentAudioFxEngine? = null
    private var crossfadeOutgoingFxEngine: PersistentAudioFxEngine? = null
    // These gains preserve the current fade position when Master Volume or EQ protection changes.
    private var crossfadeIncomingTransitionGain = 1f
    private var crossfadeOutgoingTransitionGain = 1f
    private var lastEqualizerPremiumLocked = MeloWarePremiumAccess.equalizerRequiresPremium
    private var lastGoogleDrivePremiumLocked = MeloWarePremiumAccess.googleDriveRequiresPremium

    var shuffleEnabled by mutableStateOf(false)
    var repeatSetting by mutableStateOf(RepeatSetting.OFF)
    var errorMessage by mutableStateOf<String?>(null)
    var statusMessage by mutableStateOf<String?>(null)

    private var personalDriveAccessToken: String? = null
    private var personalDriveInvalidToken: String? = null
    private var pendingPersonalDrivePlaybackId: String? = null

    var personalDriveAccountEmail by mutableStateOf<String?>(
        prefs.getString(KEY_PERSONAL_DRIVE_ACCOUNT, null)
    )
        private set
    var personalDriveBusy by mutableStateOf(false)
        private set
    var personalDriveError by mutableStateOf<String?>(null)
        private set
    var personalDriveNeedsAuthorization by mutableStateOf(false)
        private set
    val personalDrivePath = mutableStateListOf<PersonalDriveFolder>()
    var personalDriveEntries by mutableStateOf<List<PersonalDriveEntry>>(emptyList())
        private set
    val personalDriveSelectedIds = mutableStateMapOf<String, Boolean>()
    var showPersonalDriveDialog by mutableStateOf(false)

    val hasPersonalDriveAuthorization: Boolean
        get() = !personalDriveAccessToken.isNullOrBlank()

    var isScanningLocal by mutableStateOf(false)
        private set

    var publicLinkText by mutableStateOf(prefs.getString(KEY_PUBLIC_LINK, "").orEmpty())
    var publicDriveBusy by mutableStateOf(false)
        private set
    var publicDriveError by mutableStateOf<String?>(null)
        private set
    var publicRoot by mutableStateOf<SharedDriveLink?>(null)
        private set
    val publicPath = mutableStateListOf<SharedDriveFolder>()
    var publicEntries by mutableStateOf<List<SharedDriveEntry>>(emptyList())
        private set
    val publicSelectedIds = mutableStateMapOf<String, Boolean>()
    var showPublicDriveDialog by mutableStateOf(false)

    val onlineSearchResults = mutableStateListOf<SongItem>()
    var onlineSearchMode by mutableStateOf(OnlineSearchMode.APPLE_MUSIC)
    var onlineSearchQuery by mutableStateOf("")
    var onlineSearchBusy by mutableStateOf(false)
        private set
    var onlineSearchError by mutableStateOf<String?>(null)
        private set

    var sleepTimerMinutesLeft by mutableIntStateOf(0)
        private set

    var audioToolBusy by mutableStateOf(false)
        private set

    private var movieWebView: WebView? = null

    val currentSong: SongItem?
        get() = queue.getOrNull(currentIndex)

    init {
        migrateLegacyEqualizerDefaults()
        restoreState()
        configurePlayerListener()
        runCatching {
            systemAudioManager.registerAudioDeviceCallback(soundDeviceCallback, Handler(Looper.getMainLooper()))
            soundDeviceCallbacksEnabled = true
        }
        refreshSoundOutput()
        reapplyOutputVolumes()
        tickerJob = scope.launch {
            while (true) {
                if (currentSong?.source != SongSource.MOVIE && !castConnected) {
                    positionMs = player.currentPosition.coerceAtLeast(0L)
                    val detected = player.duration
                    durationMs = if (detected == C.TIME_UNSET || detected <= 0L) 1L else detected
                }
                updateSmartPlaybackTracking()
                if (SystemClock.elapsedRealtime() - lastSoundDevicePollElapsedMs >= 3_000L) {
                    lastSoundDevicePollElapsedMs = SystemClock.elapsedRealtime()
                    refreshSoundOutput()
                }
                val premiumEqLocked = MeloWarePremiumAccess.equalizerRequiresPremium
                if (premiumEqLocked != lastEqualizerPremiumLocked) {
                    lastEqualizerPremiumLocked = premiumEqLocked
                    applyEqualizerSettings(force = true)
                }
                val drivePremiumLocked = MeloWarePremiumAccess.googleDriveRequiresPremium
                if (drivePremiumLocked != lastGoogleDrivePremiumLocked) {
                    lastGoogleDrivePremiumLocked = drivePremiumLocked
                    if (drivePremiumLocked && currentSong?.let(::isGoogleDriveSong) == true) {
                        cancelSoundCrossfade()
                        runCatching { player.pause() }
                        isPlaying = false
                        statusMessage = "Google Drive Music cần Premium hoặc Premium Trial còn hiệu lực."
                    }
                }
                maybeAutoSoundCrossfade()
                delay(if (isPlaying) 250L else 650L)
            }
        }
    }

    private fun migrateLegacyEqualizerDefaults() {
        val version = prefs.getInt(KEY_EQ_TUNING_VERSION, 0)
        if (version < 2) {
            val legacyUntunedDefaults = equalizerBass == 4 && equalizerMid == 2 && equalizerTreble == 3 &&
                equalizerBand60 == 4 && equalizerBand230 == 3 && equalizerBand910 == 2 &&
                equalizerBand3600 == 2 && equalizerBand14000 == 3 &&
                equalizerSelectedPresetIndex == 0 && !equalizerAdvancedMode
            if (legacyUntunedDefaults) {
                equalizerBass = 0
                equalizerMid = 0
                equalizerTreble = 0
                equalizerBand60 = 0
                equalizerBand230 = 0
                equalizerBand910 = 0
                equalizerBand3600 = 0
                equalizerBand14000 = 0
                equalizerSelectedPresetIndex = 1
                equalizerBassBoostStrength = 0
                equalizerLoudnessEnabled = false
                equalizerLoudnessGainDb = 3
                equalizerAutoHeadroomEnabled = true
            }
        }
        if (version < 3 || !prefs.contains(KEY_EQ_CURVE_10)) {
            equalizerCurve10 = if (equalizerAdvancedMode) {
                curveFromFiveBand(
                    intArrayOf(
                        equalizerBand60,
                        equalizerBand230,
                        equalizerBand910,
                        equalizerBand3600,
                        equalizerBand14000
                    )
                )
            } else {
                curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble)
            }
        }
        prefs.edit().putInt(KEY_EQ_TUNING_VERSION, 3).apply()
        persistEqualizerState()
    }

    fun release() {
        cancelSoundCrossfade()
        if (soundDeviceCallbacksEnabled) {
            runCatching { systemAudioManager.unregisterAudioDeviceCallback(soundDeviceCallback) }
            soundDeviceCallbacksEnabled = false
        }
        finishSmartPlaybackTracking(markSkipped = false)
        playbackJob?.cancel()
        tickerJob?.cancel()
        sleepTimerJob?.cancel()
        lyricsJobs.values.forEach(Job::cancel)
        lyricsJobs.clear()
        saveQueue()
        movieWebView = null
        activeTransformers.clear()
        if (laptopShareLazy.isInitialized()) laptopShareLazy.value.release()
        if (internetSessionLazy.isInitialized()) {
            internetSession.onClosed = null
            internetSession.onDownloaded = null
            internetSession.onRemoteListChanged = null
            internetSession.shutdown()
        }
        closeMeloWareLibrary()
        stopReceivingMeloWare()
        wifiLanDiscovery.stopDiscovery()
        stopWifiHostInternal(clearUserRequest = true)
        unregisterCastListeners()
        audioFxEngine.release()
        crossfadeStandbyFxEngine?.release()
        crossfadeStandbyFxEngine = null
        crossfadeOutgoingFxEngine?.release()
        crossfadeOutgoingFxEngine = null
        player.release()
        scope.cancel()
    }

    fun dismissError() {
        errorMessage = null
    }

    fun dismissStatus() {
        statusMessage = null
    }

    fun lyricsFor(song: SongItem): LyricsContent? = lyricsBySongId[song.id]

    fun lyricsLoading(song: SongItem): Boolean = lyricsLoadingBySongId[song.id] == true

    fun lyricsError(song: SongItem): String? = lyricsErrorBySongId[song.id]

    fun loadLyrics(song: SongItem, forceRefresh: Boolean = false) {
        if (!forceRefresh && lyricsBySongId.containsKey(song.id)) return
        if (lyricsLoadingBySongId[song.id] == true) return

        lyricsJobs.remove(song.id)?.cancel()
        lyricsLoadingBySongId[song.id] = true
        lyricsErrorBySongId.remove(song.id)

        val knownDurationMs = when {
            song.durationMs > 1_000L -> song.durationMs
            currentSong?.id == song.id && durationMs > 1_000L -> durationMs
            else -> 0L
        }

        lyricsJobs[song.id] = scope.launch {
            var finalResult: Result<LyricsContent>? = null
            // A slow/unstable LRCLIB response should not appear as an immediate
            // failure that requires the user to tap the karaoke icon twice.
            // All retries remain on IO; the UI keeps showing the loading state.
            for (attempt in 0..1) {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        LrclibLyricsRepository.findLyrics(
                            context = context,
                            song = song,
                            knownDurationMs = knownDurationMs,
                            forceRefresh = forceRefresh
                        )
                    }
                }
                val failure = result.exceptionOrNull()
                if (failure is CancellationException) throw failure
                finalResult = result
                if (failure == null || attempt == 1) break
                // Do not retry permanent input/configuration errors. A short
                // delay helps with first-open network warm-up and LRCLIB latency.
                if (failure !is IOException ||
                    failure.message.orEmpty().startsWith("Tên bài hát đang trống") ||
                    failure.message.orEmpty().contains("HTTP 400") ||
                    failure.message.orEmpty().contains("HTTP 401") ||
                    failure.message.orEmpty().contains("HTTP 403")) break
                delay(if (failure.message.orEmpty().contains("429")) 2_000L else 1_200L)
            }
            lyricsLoadingBySongId[song.id] = false
            lyricsJobs.remove(song.id)
            finalResult?.onSuccess { content ->
                lyricsBySongId[song.id] = content
                lyricsErrorBySongId.remove(song.id)
            }?.onFailure { error ->
                lyricsErrorBySongId[song.id] = error.localizedMessage
                    ?: "Không tìm thấy lời bài hát phù hợp trên LRCLIB."
            }
        }
    }

    fun audioTechnicalInfoFor(song: SongItem): AudioTechnicalInfo =
        audioTechnicalInfoById[song.id] ?: provisionalAudioTechnicalInfo(song)

    fun ensureAudioTechnicalInfo(song: SongItem) {
        val existing = audioTechnicalInfoById[song.id]
        val driveCacheReady = song.source in setOf(SongSource.PERSONAL_DRIVE, SongSource.PUBLIC_DRIVE) &&
                cachedPlaybackUris[song.id]?.path?.let(::File)?.takeIf(File::isFile)?.length()?.let { it > 0L } == true
        // Với bài Drive, metadata có thể được đọc quá sớm trước khi file cache sẵn sàng.
        // Khi cache đã xuất hiện mà dung lượng cũ vẫn chưa biết, cho phép đọc lại một lần.
        if (existing != null && !(driveCacheReady && existing.sizeBytes == null)) return
        scope.launch {
            val info = runCatching {
                withContext(Dispatchers.IO) { readAudioTechnicalInfo(song) }
            }.getOrElse { provisionalAudioTechnicalInfo(song) }
            audioTechnicalInfoById[song.id] = info
        }
    }

    private fun syncEqualizerCurveFromBasicControls() {
        equalizerCurve10 = curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble)
    }

    private fun syncEqualizerCurveFromAdvancedControls() {
        equalizerCurve10 = curveFromFiveBand(
            intArrayOf(
                equalizerBand60,
                equalizerBand230,
                equalizerBand910,
                equalizerBand3600,
                equalizerBand14000
            )
        )
    }

    private fun syncVisibleEqualizerControlsFromCurve(curve: IntArray) {
        val safe = sanitizeEqCurve(curve)
        equalizerBass = interpolateEqCurveDb(safe, 100f).roundToInt().coerceIn(-6, 6)
        equalizerMid = interpolateEqCurveDb(safe, 1_000f).roundToInt().coerceIn(-6, 6)
        equalizerTreble = interpolateEqCurveDb(safe, 10_000f).roundToInt().coerceIn(-6, 6)
        val five = MELOWAVE_EQ_FIVE_HZ.map { frequency ->
            interpolateEqCurveDb(safe, frequency.toFloat()).roundToInt().coerceIn(-6, 6)
        }
        equalizerBand60 = five[0]
        equalizerBand230 = five[1]
        equalizerBand910 = five[2]
        equalizerBand3600 = five[3]
        equalizerBand14000 = five[4]
    }

    fun setEqualizerActive(enabled: Boolean) {
        if (crossfadeJob != null) cancelSoundCrossfade()
        equalizerEnabled = enabled
        if (!enabled) _equalizerCompareOriginal = false
        persistEqualizerState()
        if (enabled) {
            applyEqualizerSettings(force = true)
        } else {
            audioFxEngine.release()
            crossfadeStandbyFxEngine?.release()
            crossfadeStandbyFxEngine = null
            crossfadeOutgoingFxEngine?.release()
            crossfadeOutgoingFxEngine = null
            equalizerReady = false
            equalizerHasControl = false
            equalizerError = null
            equalizerWarning = null
            equalizerHeadroomDb = 0f
            equalizerVirtualizerAvailable = false
            equalizerVirtualizerStrengthSupported = false
            reapplyOutputVolumes()
        }
    }

    /**
     * Applies a 10-point preset curve. The curve is mapped logarithmically to the
     * actual bands offered by the device and animated over 180 ms by the audio engine.
     */
    fun selectEqualizerCurvePreset(
        index: Int,
        curveDb: List<Int>,
        bassBoostStrength: Int = 0
    ) {
        if (curveDb.size < MELOWAVE_EQ_CURVE_POINTS) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerSelectedPresetIndex = index.coerceAtLeast(0)
        equalizerCurve10 = sanitizeEqCurve(curveDb.take(MELOWAVE_EQ_CURVE_POINTS).toIntArray())
        syncVisibleEqualizerControlsFromCurve(equalizerCurve10)
        // Preset curves retain all ten reference points instead of collapsing to Bass/Mid/Treble.
        equalizerAdvancedMode = true
        equalizerBassBoostStrength = if (MeloWarePremiumAccess.equalizerRequiresPremium) 0
            else bassBoostStrength.coerceIn(0, 100)
        persistEqualizerState()
        applyEqualizerSettings()
    }

    /** Backward-compatible entry point for older UI code. */
    fun selectEqualizerPreset(
        index: Int,
        bass: Int,
        mid: Int,
        treble: Int,
        bassBoostStrength: Int = 0
    ) {
        selectEqualizerCurvePreset(
            index = index,
            curveDb = curveFromThreeBand(bass, mid, treble).toList(),
            bassBoostStrength = bassBoostStrength
        )
        if (index == 0) {
            equalizerSelectedPresetIndex = 0
            persistEqualizerState()
        }
    }

    fun updateEqualizerBass(value: Int) {
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerBass = value.coerceIn(-6, 6)
        equalizerSelectedPresetIndex = 0
        equalizerAdvancedMode = false
        syncEqualizerCurveFromBasicControls()
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun updateEqualizerMid(value: Int) {
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerMid = value.coerceIn(-6, 6)
        equalizerSelectedPresetIndex = 0
        equalizerAdvancedMode = false
        syncEqualizerCurveFromBasicControls()
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun updateEqualizerTreble(value: Int) {
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerTreble = value.coerceIn(-6, 6)
        equalizerSelectedPresetIndex = 0
        equalizerAdvancedMode = false
        syncEqualizerCurveFromBasicControls()
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun prepareAdvancedEqualizerBands() {
        if (MeloWarePremiumAccess.equalizerRequiresPremium || equalizerAdvancedMode) return
        syncVisibleEqualizerControlsFromCurve(equalizerCurve10)
        persistEqualizerState()
    }

    fun updateAdvancedEqualizerBand(index: Int, value: Int) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        val safe = value.coerceIn(-6, 6)
        when (index) {
            0 -> equalizerBand60 = safe
            1 -> equalizerBand230 = safe
            2 -> equalizerBand910 = safe
            3 -> equalizerBand3600 = safe
            4 -> equalizerBand14000 = safe
            else -> return
        }
        equalizerAdvancedMode = true
        equalizerSelectedPresetIndex = 0
        syncEqualizerCurveFromAdvancedControls()
        // Keep the compact three controls representative of the detailed curve.
        equalizerBass = interpolateEqCurveDb(equalizerCurve10, 100f).roundToInt().coerceIn(-6, 6)
        equalizerMid = interpolateEqCurveDb(equalizerCurve10, 1_000f).roundToInt().coerceIn(-6, 6)
        equalizerTreble = interpolateEqCurveDb(equalizerCurve10, 10_000f).roundToInt().coerceIn(-6, 6)
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun setAdvancedEqualizerFlat() {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerBand60 = 0
        equalizerBand230 = 0
        equalizerBand910 = 0
        equalizerBand3600 = 0
        equalizerBand14000 = 0
        equalizerBass = 0
        equalizerMid = 0
        equalizerTreble = 0
        equalizerCurve10 = IntArray(MELOWAVE_EQ_CURVE_POINTS)
        equalizerAdvancedMode = true
        equalizerSelectedPresetIndex = 0
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun setEqualizerLoudness(enabled: Boolean) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerLoudnessEnabled = enabled
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun updateEqualizerLoudnessGainDb(value: Int) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerLoudnessGainDb = value.coerceIn(1, 6)
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun updateEqualizerBassBoostStrength(value: Int) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        val requested = value.coerceIn(0, 100)
        equalizerBassBoostStrength = if (
            audioSessionId > 0 && equalizerBassBoostAvailable &&
            !equalizerBassBoostStrengthSupported && requested > 0
        ) 100 else requested
        equalizerSelectedPresetIndex = 0
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun updateEqualizerVirtualizerStrength(value: Int) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        val requested = value.coerceIn(0, 100)
        equalizerVirtualizerStrength = if (
            audioSessionId > 0 && equalizerVirtualizerAvailable &&
            !equalizerVirtualizerStrengthSupported && requested > 0
        ) 100 else requested
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun setEqualizerCompareOriginal(enabled: Boolean) {
        if (!equalizerEnabled) return
        _equalizerCompareOriginal = enabled
        applyEqualizerSettings()
        reapplyOutputVolumes()
    }

    fun setEqualizerAutoHeadroom(enabled: Boolean) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        equalizerAutoHeadroomEnabled = enabled
        persistEqualizerState()
        applyEqualizerSettings()
    }

    /** Applies the nearest built-in curve for the current song metadata, on demand. */
    fun applyEqualizerForCurrentGenre(): Boolean {
        val genre = currentSong?.genre.orEmpty().lowercase(Locale.ROOT).trim()
        if (genre.isBlank()) {
            statusMessage = "Bài hát chưa có thông tin thể loại để gợi ý EQ."
            return false
        }
        val selection = when {
            listOf("rock", "metal", "punk", "alternative").any { genre.contains(it) } ->
                Triple(4, listOf(5, 3, 1, 0, -1, 1, 3, 4, 5, 5), 0)
            listOf("pop", "k-pop", "j-pop").any { genre.contains(it) } ->
                Triple(3, listOf(-1, 1, 3, 4, 4, 3, 1, 0, 1, 2), 0)
            listOf("jazz", "blues", "swing").any { genre.contains(it) } ->
                Triple(5, listOf(4, 3, 1, 2, -1, -1, 0, 1, 3, 4), 0)
            listOf("classical", "classic", "orchestra", "symphony").any { genre.contains(it) } ->
                Triple(6, listOf(4, 3, 2, 2, -1, -1, 0, 2, 3, 4), 0)
            listOf("electronic", "edm", "dance", "house", "techno", "trance").any { genre.contains(it) } ->
                Triple(8, listOf(5, 4, 1, 0, -2, 2, 1, 4, 5, 4), 0)
            listOf("vocal", "acoustic", "ballad", "country", "folk", "r&b", "rnb", "soul").any { genre.contains(it) } ->
                Triple(7, listOf(-2, -1, 1, 3, 5, 4, 3, 1, 0, -1), 0)
            else -> null
        }
        if (selection == null) {
            statusMessage = "Chưa có preset EQ phù hợp cho thể loại: ${currentSong?.genre.orEmpty()}"
            return false
        }
        selectEqualizerCurvePreset(selection.first, selection.second, selection.third)
        statusMessage = "Đã áp dụng EQ theo thể loại ${currentSong?.genre.orEmpty()}."
        return true
    }

    fun resetEqualizer() {
        equalizerEnabled = true
        _equalizerCompareOriginal = false
        equalizerBass = 0
        equalizerMid = 0
        equalizerTreble = 0
        equalizerBand60 = 0
        equalizerBand230 = 0
        equalizerBand910 = 0
        equalizerBand3600 = 0
        equalizerBand14000 = 0
        equalizerCurve10 = IntArray(MELOWAVE_EQ_CURVE_POINTS)
        equalizerBassBoostStrength = 0
        equalizerVirtualizerStrength = 0
        equalizerLoudnessEnabled = false
        equalizerLoudnessGainDb = 3
        equalizerAutoHeadroomEnabled = true
        equalizerSelectedPresetIndex = 1
        equalizerAdvancedMode = true
        persistEqualizerState()
        applyEqualizerSettings(force = true)
    }

    fun refreshEqualizer() {
        if (!equalizerEnabled) equalizerEnabled = true
        persistEqualizerState()
        applyEqualizerSettings(force = true)
    }

    /** Discover the likely active MEDIA output; API 33+ queries the chosen route.
     * Older Android versions expose connected outputs but may not expose the chosen route.
     * No BLUETOOTH_CONNECT permission is requested: device names fall back gracefully.
     */
    @Suppress("DEPRECATION")
    fun refreshSoundOutput() {
        val connected = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                val attributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                systemAudioManager.getAudioDevicesForAttributes(attributes).toList()
                    .ifEmpty { systemAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() }
            } else {
                systemAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
            }
        }.getOrDefault(emptyList())
        fun isBluetooth(type: Int) = type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= 31 && (type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
        fun isHeadphone(type: Int) = type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
        val legacyBluetoothActive = Build.VERSION.SDK_INT < 33 && runCatching {
            systemAudioManager.isBluetoothA2dpOn || systemAudioManager.isBluetoothScoOn
        }.getOrDefault(false)
        val legacyWiredActive = Build.VERSION.SDK_INT < 33 && runCatching {
            systemAudioManager.isWiredHeadsetOn
        }.getOrDefault(false)
        val detected = when {
            Build.VERSION.SDK_INT >= 33 -> connected.firstOrNull()
            legacyBluetoothActive -> connected.firstOrNull { isBluetooth(it.type) }
            legacyWiredActive -> connected.firstOrNull { isHeadphone(it.type) }
            else -> connected.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        } ?: connected.firstOrNull { isBluetooth(it.type) }
            ?: connected.firstOrNull { isHeadphone(it.type) }
            ?: connected.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?: connected.firstOrNull()
        val type = detected?.type ?: AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        val rawName = detected?.let { runCatching { it.productName?.toString()?.trim() }.getOrNull() }
            .orEmpty()
        val rawAddress = if (Build.VERSION.SDK_INT >= 28) {
            detected?.let { runCatching { it.address?.trim() }.getOrNull() }.orEmpty()
        } else ""
        val category = when {
            isBluetooth(type) -> "Bluetooth"
            isHeadphone(type) -> "Tai nghe"
            else -> "Loa điện thoại"
        }
        val generic = rawName.isBlank() || rawName.equals("unknown", true) ||
            rawName.equals("built-in speaker", true) || rawName.equals("speaker", true)
        val label = if (generic) category else if (category == "Loa điện thoại") category else "$category · $rawName"
        val stableIdentity = rawAddress.takeIf {
            it.isNotBlank() && it != "0" && it != "00:00:00:00:00:00"
        } ?: rawName.takeUnless { generic }.orEmpty()
        val key = when {
            category == "Loa điện thoại" -> "speaker"
            stableIdentity.isBlank() -> "${category.lowercase(Locale.ROOT)}:$type"
            else -> "${category.lowercase(Locale.ROOT)}:$type:${stableIdentity.lowercase(Locale.ROOT).sha256().take(16)}"
        }
        if (key == soundOutputKey && label == soundOutputName && soundOutputInitialized) return
        val previousKey = soundOutputKey
        if (!soundOutputInitialized && key != "speaker" && !prefs.contains(KEY_SOUND_BASE_PROFILE)) {
            prefs.edit().putString(KEY_SOUND_BASE_PROFILE, soundProfileSnapshot().toString()).apply()
        }
        val previousSaved = prefs.contains(soundProfileKey(previousKey))
        if (soundOutputInitialized && previousKey == "speaker" && key != previousKey && !previousSaved) {
            // Keep a neutral speaker profile when switching to a saved headset EQ.
            prefs.edit().putString(KEY_SOUND_BASE_PROFILE, soundProfileSnapshot().toString()).apply()
        }
        soundOutputKey = key
        soundOutputName = label
        soundPresetSaved = prefs.contains(soundProfileKey(key))
        soundOutputInitialized = true
        if (soundAutoApplyDeviceEQ && !MeloWarePremiumAccess.equalizerRequiresPremium) {
            if (soundPresetSaved) applySavedSoundProfile(key)
            else if ((previousSaved && previousKey != key) || key == "speaker") {
                prefs.getString(KEY_SOUND_BASE_PROFILE, null)?.let(::applySoundProfileText)
            }
        }
    }

    private fun soundProfileKey(key: String) = "sound_eq_profile_" + key.sha256().take(32)

    fun setSoundMasterVolume(percent: Int) {
        val safe = percent.coerceIn(0, 100)
        if (safe == soundMasterVolumePercent) return
        soundMasterVolumePercent = safe
        prefs.edit().putInt(KEY_SOUND_MASTER_VOLUME, safe).apply()
        // Keep the neutral speaker profile in sync, while explicitly saved device profiles
        // continue to change only when the listener presses Save.
        if (soundOutputInitialized && soundOutputKey == "speaker" && !soundPresetSaved) {
            prefs.edit().putString(KEY_SOUND_BASE_PROFILE, soundProfileSnapshot().toString()).apply()
        }
        reapplyOutputVolumes()
    }

    fun setSoundAutoApplyDeviceEQ(enabled: Boolean) {
        soundAutoApplyDeviceEQState = enabled
        prefs.edit().putBoolean(KEY_SOUND_AUTO_EQ, enabled).apply()
        if (enabled && !MeloWarePremiumAccess.equalizerRequiresPremium) {
            applySavedSoundProfile(soundOutputKey)
        }
    }

    private fun soundProfileSnapshot(): JSONObject = JSONObject().apply {
        put("masterVolume", soundMasterVolumePercent)
        put("equalizerEnabled", equalizerEnabled)
        put("bass", equalizerBass)
        put("mid", equalizerMid)
        put("treble", equalizerTreble)
        put("band60", equalizerBand60)
        put("band230", equalizerBand230)
        put("band910", equalizerBand910)
        put("band3600", equalizerBand3600)
        put("band14000", equalizerBand14000)
        put("curve10", JSONArray(equalizerCurve10.toList()))
        put("advanced", equalizerAdvancedMode)
        put("loudness", equalizerLoudnessEnabled)
        put("loudnessGainDb", equalizerLoudnessGainDb)
        put("bassBoost", equalizerBassBoostStrength)
        put("virtualizer", equalizerVirtualizerStrength)
        put("autoHeadroom", equalizerAutoHeadroomEnabled)
        put("preset", equalizerSelectedPresetIndex)
    }

    /** Explicitly saved by the listener: device sound settings are not silently overwritten. */
    fun saveSoundProfileForCurrentOutput(): Boolean {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return false
        val data = soundProfileSnapshot()
        prefs.edit().putString(soundProfileKey(soundOutputKey), data.toString()).apply()
        soundPresetSaved = true
        persistEqualizerState()
        applyEqualizerSettings()
        return true
    }

    fun removeSoundProfileForCurrentOutput() {
        prefs.edit().remove(soundProfileKey(soundOutputKey)).apply()
        soundPresetSaved = false
    }

    private fun applySavedSoundProfile(key: String) {
        prefs.getString(soundProfileKey(key), null)?.let(::applySoundProfileText)
    }

    private fun applySoundProfileText(text: String) {
        val data = runCatching { JSONObject(text) }.getOrNull() ?: return
        if (data.has("masterVolume")) {
            soundMasterVolumePercent = data.optInt("masterVolume", soundMasterVolumePercent)
                .coerceIn(0, 100)
            prefs.edit().putInt(KEY_SOUND_MASTER_VOLUME, soundMasterVolumePercent).apply()
        }
        equalizerBass = data.optInt("bass", 0).coerceIn(-6, 6)
        equalizerMid = data.optInt("mid", 0).coerceIn(-6, 6)
        equalizerTreble = data.optInt("treble", 0).coerceIn(-6, 6)
        equalizerBand60 = data.optInt("band60", 0).coerceIn(-6, 6)
        equalizerBand230 = data.optInt("band230", 0).coerceIn(-6, 6)
        equalizerBand910 = data.optInt("band910", 0).coerceIn(-6, 6)
        equalizerBand3600 = data.optInt("band3600", 0).coerceIn(-6, 6)
        equalizerBand14000 = data.optInt("band14000", 0).coerceIn(-6, 6)
        equalizerCurve10 = data.optJSONArray("curve10")?.let { array ->
            if (array.length() == MELOWAVE_EQ_CURVE_POINTS) {
                sanitizeEqCurve(IntArray(MELOWAVE_EQ_CURVE_POINTS) { array.optInt(it, 0) })
            } else null
        } ?: if (data.optBoolean("advanced", false)) {
            curveFromFiveBand(
                intArrayOf(
                    equalizerBand60,
                    equalizerBand230,
                    equalizerBand910,
                    equalizerBand3600,
                    equalizerBand14000
                )
            )
        } else {
            curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble)
        }
        syncVisibleEqualizerControlsFromCurve(equalizerCurve10)
        equalizerAdvancedMode = data.optBoolean("advanced", false)
        equalizerLoudnessEnabled = data.optBoolean("loudness", false)
        equalizerLoudnessGainDb = data.optInt("loudnessGainDb", 3).coerceIn(1, 6)
        equalizerBassBoostStrength = data.optInt("bassBoost", 0).coerceIn(0, 100)
        equalizerVirtualizerStrength = data.optInt("virtualizer", 0).coerceIn(0, 100)
        equalizerAutoHeadroomEnabled = data.optBoolean("autoHeadroom", true)
        equalizerSelectedPresetIndex = data.optInt("preset", 1)
        // Legacy EQ-only profiles did not store this flag and historically enabled EQ when applied.
        equalizerEnabled = data.optBoolean("equalizerEnabled", true)
        persistEqualizerState()
        applyEqualizerSettings()
    }

    fun setSoundCrossfadeSeconds(seconds: Int) {
        if (MeloWarePremiumAccess.equalizerRequiresPremium) return
        soundCrossfadeSecondsState = seconds.coerceIn(0, 10)
        prefs.edit().putInt(KEY_SOUND_CROSSFADE, soundCrossfadeSeconds).apply()
        if (soundCrossfadeSeconds == 0) cancelSoundCrossfade()
    }

    private fun isSoundCrossfadeAllowed(): Boolean = soundCrossfadeSeconds > 0 &&
        !MeloWarePremiumAccess.equalizerRequiresPremium && !castConnected &&
        currentSong?.source !in setOf(SongSource.MOVIE, SongSource.INTERNET_P2P)

    private fun crossfadeMediaSupported(source: SongSource): Boolean = source != SongSource.MOVIE &&
        source != SongSource.INTERNET_P2P

    /** Do not leave a second player or AudioEffect chain alive after a seek or switch. */
    private fun cancelSoundCrossfade() {
        val oldJob = crossfadeJob
        crossfadeJob = null
        oldJob?.cancel()
        crossfadeStandby?.let { standby ->
            if (standby !== player) runCatching { standby.release() }
        }
        crossfadeStandby = null
        crossfadeOutgoing?.let { outgoing ->
            if (outgoing !== player) runCatching { outgoing.release() }
        }
        crossfadeOutgoing = null
        crossfadeStandbyFxEngine?.release()
        crossfadeStandbyFxEngine = null
        crossfadeOutgoingFxEngine?.release()
        crossfadeOutgoingFxEngine = null
        crossfadeIncomingTransitionGain = 1f
        crossfadeOutgoingTransitionGain = 1f
        reapplyOutputVolumes()
    }

    /** True overlapping Crossfade. Both players retain their own EQ chain until fading ends. */
    private fun trySoundCrossfade(targetIndex: Int, automatic: Boolean): Boolean {
        if (!isSoundCrossfadeAllowed() || targetIndex !in queue.indices ||
            targetIndex == currentIndex || crossfadeJob != null || crossfadeOutgoing != null ||
            !player.isPlaying || !crossfadeMediaSupported(queue[targetIndex].source)) return false
        val oldPlayer = player
        val oldFxEngine = audioFxEngine
        val startingIndex = currentIndex
        val fromId = currentSong?.id
        val nextSong = queue[targetIndex]
        val work = scope.launch(start = CoroutineStart.LAZY) {
            var incoming: ExoPlayer? = null
            var incomingFx: PersistentAudioFxEngine? = null
            var transitioned = false
            var fxPromoted = false
            try {
                val uri = resolvePlaybackUri(nextSong)
                if (player !== oldPlayer || currentIndex != startingIndex ||
                    currentSong?.id != fromId || !oldPlayer.isPlaying || castConnected) return@launch
                val nextPlayer = ExoPlayer.Builder(context).build().apply {
                    setAudioAttributes(mediaAudioAttributes, false)
                    volume = 0f
                    setMediaItem(nextSong.toMediaItem(uri, artworkFor(nextSong)))
                    prepare()
                }
                incoming = nextPlayer
                crossfadeStandby = nextPlayer
                val newFx = PersistentAudioFxEngine()
                incomingFx = newFx
                crossfadeStandbyFxEngine = newFx
                val deadline = SystemClock.elapsedRealtime() + if (automatic) 3_000L else 12_000L
                while (nextPlayer.playbackState != Player.STATE_READY &&
                    SystemClock.elapsedRealtime() < deadline &&
                    nextPlayer.playerError == null && oldPlayer.isPlaying) {
                    delay(45)
                }
                if (nextPlayer.playbackState != Player.STATE_READY ||
                    player !== oldPlayer || !oldPlayer.isPlaying || castConnected ||
                    currentIndex != startingIndex || currentSong?.id != fromId) return@launch
                val nextSession = nextPlayer.audioSessionId
                if (equalizerEnabled && nextSession > 0 && nextSession != C.AUDIO_SESSION_ID_UNSET) {
                    applyEqualizerToEngine(newFx, nextSession, force = true, updatePublicState = false)
                }
                val remaining = oldPlayer.duration.takeIf { it > 0L && it != C.TIME_UNSET }
                    ?.minus(oldPlayer.currentPosition) ?: Long.MAX_VALUE
                val fadeMs = if (automatic) {
                    minOf(soundCrossfadeSeconds * 1_000L, remaining - 250L)
                } else soundCrossfadeSeconds * 1_000L
                if (fadeMs < 500L) return@launch
                finishSmartPlaybackTracking(markSkipped = false)
                currentIndex = targetIndex
                beginSmartPlaybackTracking(nextSong)
                crossfadeStandby = null
                crossfadeStandbyFxEngine = null
                crossfadeOutgoing = oldPlayer
                crossfadeOutgoingFxEngine = oldFxEngine
                crossfadeIncomingTransitionGain = 0f
                crossfadeOutgoingTransitionGain = 1f
                audioFxEngine = newFx
                fxPromoted = true
                player = nextPlayer
                // Crossfade swaps ExoPlayer; the system media notification must
                // now follow the new player/track rather than the outgoing one.
                MeloWavePlaybackRuntime.onPlaybackChanged()
                transitioned = true
                configurePlayerListener(nextPlayer)
                audioSessionId = nextSession.takeIf { it > 0 && it != C.AUDIO_SESSION_ID_UNSET } ?: 0
                if (equalizerEnabled && audioSessionId > 0) {
                    applyEqualizerToEngine(audioFxEngine, audioSessionId, force = false, updatePublicState = true)
                }
                isLoading = false
                positionMs = 0L
                durationMs = nextPlayer.duration.takeIf { it > 0L && it != C.TIME_UNSET }
                    ?: nextSong.durationMs.coerceAtLeast(1L)
                errorMessage = null
                addRecent(nextSong)
                ensureArtwork(nextSong)
                ensureAudioTechnicalInfo(nextSong)
                saveQueue()
                nextPlayer.play()
                val startTime = SystemClock.elapsedRealtime()
                while (true) {
                    val elapsed = SystemClock.elapsedRealtime() - startTime
                    val progress = (elapsed.toFloat() / fadeMs).coerceIn(0f, 1f)
                    val angle = progress.toDouble() * PI / 2.0
                    crossfadeOutgoingTransitionGain = cos(angle).toFloat().coerceIn(0f, 1f)
                    crossfadeIncomingTransitionGain = sin(angle).toFloat().coerceIn(0f, 1f)
                    val baseGain = currentOutputGain()
                    applyPlayerOutputGain(oldPlayer, crossfadeOutgoingTransitionGain, baseGain)
                    applyPlayerOutputGain(nextPlayer, crossfadeIncomingTransitionGain, baseGain)
                    if (progress >= 1f || nextPlayer.playerError != null) break
                    delay(35L)
                }
                crossfadeIncomingTransitionGain = 1f
                crossfadeOutgoingTransitionGain = 0f
                val finalGain = currentOutputGain()
                applyPlayerOutputGain(nextPlayer, 1f, finalGain)
                applyPlayerOutputGain(oldPlayer, 0f, finalGain)
                oldPlayer.pause()
                nextPlayer.setAudioAttributes(mediaAudioAttributes, true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                Log.w("MeloWaveSoundStudio", "Crossfade fallback: ${error.localizedMessage}")
            } finally {
                val useNormalSwitch = !automatic && !transitioned &&
                    coroutineContext[Job]?.isCancelled != true &&
                    player === oldPlayer && oldPlayer.isPlaying && currentIndex == startingIndex &&
                    !castConnected
                if (crossfadeStandby === incoming) crossfadeStandby = null
                if (crossfadeStandbyFxEngine === incomingFx) crossfadeStandbyFxEngine = null
                incoming?.let { standby ->
                    if (standby !== player) runCatching { standby.release() }
                }
                if (!fxPromoted) incomingFx?.release()
                if (crossfadeOutgoing === oldPlayer) {
                    crossfadeOutgoing = null
                    if (oldPlayer !== player) runCatching { oldPlayer.release() }
                }
                if (crossfadeOutgoingFxEngine === oldFxEngine) {
                    crossfadeOutgoingFxEngine = null
                    if (oldFxEngine !== audioFxEngine) oldFxEngine.release()
                }
                crossfadeIncomingTransitionGain = 1f
                crossfadeOutgoingTransitionGain = 1f
                reapplyOutputVolumes()
                if (crossfadeJob === coroutineContext[Job]) crossfadeJob = null
                if (useNormalSwitch) {
                    currentIndex = targetIndex
                    prepareCurrent(autoPlay = true)
                }
            }
        }
        crossfadeJob = work
        work.start()
        return true
    }

    private fun maybeAutoSoundCrossfade() {
        if (!isSoundCrossfadeAllowed() || crossfadeJob != null || crossfadeOutgoing != null ||
            !player.isPlaying || repeatSetting == RepeatSetting.ONE || queue.size < 2 ||
            currentSong?.id == lastAutoCrossfadeSongId) return
        val validDuration = player.duration
        if (validDuration == C.TIME_UNSET || validDuration <= 0L) return
        val remain = validDuration - player.currentPosition
        if (remain <= 0L || remain > soundCrossfadeSeconds * 1_000L + 350L) return
        val target = when {
            shuffleEnabled -> randomOtherIndex(currentIndex)
            currentIndex < queue.lastIndex -> currentIndex + 1
            repeatSetting == RepeatSetting.ALL -> 0
            else -> return
        }
        lastAutoCrossfadeSongId = currentSong?.id
        trySoundCrossfade(target, automatic = true)
    }

    private fun persistEqualizerState() {
        prefs.edit()
            .putInt(KEY_EQ_BASS, equalizerBass)
            .putInt(KEY_EQ_MID, equalizerMid)
            .putInt(KEY_EQ_TREBLE, equalizerTreble)
            .putInt(KEY_EQ_BAND_60, equalizerBand60)
            .putInt(KEY_EQ_BAND_230, equalizerBand230)
            .putInt(KEY_EQ_BAND_910, equalizerBand910)
            .putInt(KEY_EQ_BAND_3600, equalizerBand3600)
            .putInt(KEY_EQ_BAND_14000, equalizerBand14000)
            .putString(KEY_EQ_CURVE_10, eqCurvePreference(equalizerCurve10))
            .putInt(KEY_EQ_PRESET, equalizerSelectedPresetIndex)
            .putBoolean(KEY_EQ_ADVANCED, equalizerAdvancedMode)
            .putBoolean(KEY_EQ_LOUDNESS, equalizerLoudnessEnabled)
            .putInt(KEY_EQ_LOUDNESS_GAIN_DB, equalizerLoudnessGainDb)
            .putInt(KEY_EQ_BASS_BOOST, equalizerBassBoostStrength)
            .putInt(KEY_EQ_VIRTUALIZER, equalizerVirtualizerStrength)
            .putBoolean(KEY_EQ_AUTO_HEADROOM, equalizerAutoHeadroomEnabled)
            .putBoolean(KEY_EQ_ENABLED, equalizerEnabled)
            .apply()
        if (soundOutputInitialized && soundOutputKey == "speaker" && !soundPresetSaved) {
            prefs.edit().putString(KEY_SOUND_BASE_PROFILE, soundProfileSnapshot().toString()).apply()
        }
    }

    private fun activeEqualizerValues(): IntArray =
        if (equalizerCompareOriginal) IntArray(MELOWAVE_EQ_CURVE_POINTS)
        else sanitizeEqCurve(equalizerCurve10)

    private fun calculateAutoHeadroomDb(): Float {
        if (!equalizerEnabled || equalizerCompareOriginal) return 0f
        val premiumLocked = MeloWarePremiumAccess.equalizerRequiresPremium
        // Basic EQ keeps automatic protection active even when Pro controls are locked.
        val headroomEnabled = if (premiumLocked) true else equalizerAutoHeadroomEnabled
        if (!headroomEnabled) return 0f
        val headroomCurve = if (premiumLocked) {
            curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble)
        } else {
            activeEqualizerValues()
        }
        val eqBoost = headroomCurve.maxOrNull()?.coerceAtLeast(0)?.toFloat() ?: 0f
        val bassBoostReserve = if (premiumLocked) 0f
            else equalizerBassBoostStrength.coerceIn(0, 100) / 100f * 4f
        // Spatial widening can add a small perceived peak on some OEM implementations.
        val spatialReserve = if (premiumLocked) 0f
            else equalizerVirtualizerStrength.coerceIn(0, 100) / 100f * 0.5f
        // LoudnessEnhancer already limits/compresses peaks. Reserving its full target gain here
        // cancelled the audible boost, so Auto Headroom protects positive EQ, Bass Boost and Spatial.
        return (eqBoost + bassBoostReserve + spatialReserve).coerceIn(0f, 10f)
    }

    private fun currentEqualizerOutputGain(): Float {
        equalizerHeadroomDb = calculateAutoHeadroomDb()
        if (!equalizerEnabled || equalizerHeadroomDb <= 0f) return 1f
        return 10.0.pow(-equalizerHeadroomDb.toDouble() / 20.0).toFloat().coerceIn(0.20f, 1f)
    }

    private fun currentOutputGain(): Float {
        val masterGain = soundMasterVolumePercent.coerceIn(0, 100) / 100f
        return (masterGain * currentEqualizerOutputGain()).coerceIn(0f, 1f)
    }

    /** The only normal path that writes ExoPlayer.volume.
     * Final gain = Master Volume x Auto Headroom x Crossfade transition.
     */
    private fun applyPlayerOutputGain(
        target: ExoPlayer,
        transitionGain: Float = 1f,
        baseGain: Float = currentOutputGain()
    ) {
        runCatching {
            target.volume = (baseGain * transitionGain.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        }
    }

    private fun reapplyOutputVolumes() {
        val baseGain = currentOutputGain()
        val outgoing = crossfadeOutgoing
        if (outgoing != null) {
            applyPlayerOutputGain(outgoing, crossfadeOutgoingTransitionGain, baseGain)
            applyPlayerOutputGain(player, crossfadeIncomingTransitionGain, baseGain)
        } else {
            applyPlayerOutputGain(player, 1f, baseGain)
        }
        // A preloaded standby player must stay silent until it is promoted to the active player.
        crossfadeStandby?.let { standby ->
            if (standby !== player) applyPlayerOutputGain(standby, 0f, baseGain)
        }
    }

    private fun formatEqFrequency(hz: Int): String = when {
        hz >= 10_000 -> "${(hz / 1000f).roundToInt()} kHz"
        hz >= 1_000 -> {
            val khz = hz / 1000f
            if (kotlin.math.abs(khz - khz.roundToInt()) < 0.05f) "${khz.roundToInt()} kHz"
            else String.format(Locale.US, "%.1f kHz", khz)
        }
        else -> "$hz Hz"
    }

    private fun applyEqualizerToEngine(
        engine: PersistentAudioFxEngine,
        sessionId: Int,
        force: Boolean,
        updatePublicState: Boolean
    ): String? {
        val attach = engine.attach(sessionId, force)
        if (updatePublicState) {
            equalizerHasControl = attach.hasControl
            equalizerBassBoostAvailable = attach.bassBoostAvailable
            equalizerBassBoostStrengthSupported = attach.bassBoostStrengthSupported
            equalizerVirtualizerAvailable = attach.virtualizerAvailable
            equalizerVirtualizerStrengthSupported = attach.virtualizerStrengthSupported
            if (attach.mappedFiveBandCentersHz.size == 5) {
                equalizerActualFiveBandLabels = attach.mappedFiveBandCentersHz.map(::formatEqFrequency)
            }
            equalizerError = attach.error
            equalizerWarning = null
            equalizerReady = attach.ready
        }
        if (!attach.ready) return attach.error

        val premiumLocked = MeloWarePremiumAccess.equalizerRequiresPremium
        val bypass = equalizerCompareOriginal
        val curveForDevice = when {
            bypass -> IntArray(MELOWAVE_EQ_CURVE_POINTS)
            premiumLocked -> curveFromThreeBand(equalizerBass, equalizerMid, equalizerTreble)
            else -> activeEqualizerValues()
        }
        val coreError = engine.applyCurve(
            curveDb = curveForDevice,
            smooth = !force
        )
        val bassError = engine.setBassBoost(
            if (premiumLocked || bypass) 0 else equalizerBassBoostStrength
        )
        val virtualizerError = engine.setVirtualizer(
            if (premiumLocked || bypass) 0 else equalizerVirtualizerStrength
        )
        val loudnessError = engine.setLoudness(
            enabled = !premiumLocked && !bypass && equalizerLoudnessEnabled,
            gainDb = equalizerLoudnessGainDb
        )
        if (updatePublicState) {
            equalizerError = coreError
            equalizerWarning = listOfNotNull(bassError, virtualizerError, loudnessError)
                .joinToString(" · ").takeIf { it.isNotBlank() }
            equalizerReady = coreError == null
        }
        return coreError
    }

    private fun attachAndApplyEqualizer(sessionId: Int, force: Boolean = false) {
        if (sessionId <= 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) {
            equalizerReady = false
            equalizerHasControl = false
            equalizerError = "Phiên âm thanh chưa sẵn sàng."
            return
        }
        audioSessionId = sessionId
        applyEqualizerToEngine(audioFxEngine, sessionId, force, updatePublicState = true)
        reapplyOutputVolumes()
    }

    private fun applyEqualizerSettings(force: Boolean = false) {
        if (!equalizerEnabled) {
            audioFxEngine.release()
            equalizerReady = false
            equalizerHasControl = false
            equalizerError = null
            equalizerWarning = null
            equalizerHeadroomDb = 0f
            equalizerVirtualizerAvailable = false
            equalizerVirtualizerStrengthSupported = false
            reapplyOutputVolumes()
            return
        }
        if (currentSong?.source == SongSource.MOVIE || castConnected) {
            equalizerReady = false
            equalizerHasControl = false
            equalizerError = "Equalizer chỉ áp dụng cho nhạc phát trực tiếp trên điện thoại."
            return
        }
        val session = player.audioSessionId.takeIf { it > 0 && it != C.AUDIO_SESSION_ID_UNSET }
            ?: audioSessionId.takeIf { it > 0 && it != C.AUDIO_SESSION_ID_UNSET }
        if (session == null) {
            equalizerReady = false
            equalizerHasControl = false
            equalizerError = "Hãy phát một bài nhạc trước khi điều chỉnh Equalizer."
            reapplyOutputVolumes()
            return
        }
        attachAndApplyEqualizer(session, force = force)
        val outgoing = crossfadeOutgoing
        val outgoingFx = crossfadeOutgoingFxEngine
        if (outgoing != null && outgoingFx != null) {
            val outgoingSession = outgoing.audioSessionId
            if (outgoingSession > 0 && outgoingSession != C.AUDIO_SESSION_ID_UNSET) {
                applyEqualizerToEngine(outgoingFx, outgoingSession, force = false, updatePublicState = false)
            }
        }
    }

    fun initializeCast() {
        if (castInitialized) return
        castInitialized = true
        CastContext.getSharedInstance(context, ContextCompat.getMainExecutor(context))
            .addOnSuccessListener { shared ->
                castContext = shared
                val sessionManager = shared.sessionManager
                castStateListener = CastStateListener { state ->
                    castConnecting = state == CastState.CONNECTING
                    if (state == CastState.NOT_CONNECTED || state == CastState.NO_DEVICES_AVAILABLE) {
                        castConnected = false
                    }
                }.also(shared::addCastStateListener)

                castSessionListener = object : SessionManagerListener<CastSession> {
                    override fun onSessionStarting(session: CastSession) {
                        castConnecting = true
                    }

                    override fun onSessionStarted(session: CastSession, sessionId: String) {
                        onCastSessionReady(session)
                    }

                    override fun onSessionStartFailed(session: CastSession, error: Int) {
                        castConnecting = false
                        castConnected = false
                        statusMessage = "Không thể kết nối thiết bị Cast (mã $error)"
                    }

                    override fun onSessionEnding(session: CastSession) = Unit

                    override fun onSessionEnded(session: CastSession, error: Int) {
                        onCastSessionClosed()
                    }

                    override fun onSessionResuming(session: CastSession, sessionId: String) {
                        castConnecting = true
                    }

                    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                        onCastSessionReady(session)
                    }

                    override fun onSessionResumeFailed(session: CastSession, error: Int) {
                        onCastSessionClosed()
                    }

                    override fun onSessionSuspended(session: CastSession, reason: Int) {
                        castConnecting = false
                        castConnected = false
                    }
                }.also { listener ->
                    sessionManager.addSessionManagerListener(listener, CastSession::class.java)
                }

                sessionManager.currentCastSession?.takeIf { it.isConnected }?.let(::onCastSessionReady)
            }
            .addOnFailureListener { error ->
                castInitialized = false
                statusMessage = "Thiết bị này chưa khởi tạo được Google Cast: ${error.localizedMessage ?: "lỗi không xác định"}"
            }
    }


    /** Share ONLY the currently playing song with other MeloWare devices on the LAN. */
    fun startSharingToMeloWare() {
        val song = currentSong
        if (song == null || song.source == SongSource.MOVIE) {
            wifiHostError = "Hãy chọn một bài nhạc trước khi chia sẻ bằng MeloWare."
            return
        }
        if (castConnected) {
            statusMessage = "Hãy ngắt Cast trước khi chia sẻ MeloWare tới điện thoại khác."
            return
        }
        Log.i(MELOWARE_LAN_LOG_TAG, "A: starting MeloWare sharing, selectedFolders=${wifiSharedFolderPaths.size}")
        wifiAppShareRequested = true
        wifiAppShareEnabled = true
        wifiHostRequestedByUser = true
        if (wifiHostActive && localAudioServer != null) {
            refreshMeloWareSharedFolders()
            wifiLanDiscovery.advertise(localAudioServer!!.port, localAudioServer!!.token)
        } else if (!wifiHostBusy) {
            startWifiHostForCurrentSong(showMessage = true)
        }
    }

    /** User must explicitly choose which audio folders the other phone can browse. */
    fun setMeloWareFolderShared(folder: String, enabled: Boolean) {
        val validFolder = wifiShareableFolders.any { it.path == folder }
        if (!validFolder) return
        if (enabled && folder !in wifiSharedFolderPaths) wifiSharedFolderPaths.add(folder)
        if (!enabled) wifiSharedFolderPaths.remove(folder)
        refreshMeloWareSharedFolders()
    }

    fun selectAllMeloWareFolders(enabled: Boolean) {
        wifiSharedFolderPaths.clear()
        if (enabled) wifiSharedFolderPaths.addAll(wifiShareableFolders.map(MeloWareLanFolder::path))
        refreshMeloWareSharedFolders()
    }

    private fun refreshMeloWareSharedFolders() {
        val selected = if (wifiAppShareRequested) wifiSharedFolderPaths.toSet() else emptySet()
        val available = if (selected.isEmpty()) emptyList() else localSongs
            .asSequence()
            .filter { it.source == SongSource.LOCAL_MEDIASTORE || it.source == SongSource.DOWNLOADED }
            .filter { it.folderName.trim('/').ifBlank { "Nhạc trên máy" } in selected }
            .map { song ->
                LanSharedAudio(
                    key = song.id.sha256().take(24),
                    folder = song.folderName.trim('/').ifBlank { "Nhạc trên máy" },
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.durationMs,
                    mimeType = song.mimeType ?: resolveAudioMime(song.uri.lastPathSegment.orEmpty(), null) ?: "audio/mpeg",
                    uri = song.uri
                )
            }.toList()
        localAudioServer?.updateSharedLibrary(available)
    }

    fun startSearchingMeloWare() {
        wifiLanDiscovery.startDiscovery()
    }

    fun stopSearchingMeloWare() {
        wifiLanDiscovery.stopDiscovery()
    }

    /** Fallback for routers that block mDNS but allow HTTP to the other phone. */
    fun receiveFromMeloWareUrl(rawUrl: String) {
        val peer = parseMeloWareLanUrl(rawUrl) ?: run {
            wifiPeerMessage = "Dán URL Wi-Fi nội bộ hợp lệ từ MeloWare máy A để kết nối."
            return
        }
        receiveFromMeloWare(peer)
    }

    private fun parseMeloWareLanUrl(rawUrl: String): MeloWareLanPeer? {
        val parsed = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return null
        val host = parsed.host.orEmpty()
        val token = parsed.pathSegments.firstOrNull().orEmpty()
        val isPrivate = host.matches(Regex("(?:10|192\\.168)\\..*")) ||
                host.matches(Regex("172\\.(?:1[6-9]|2[0-9]|3[01])\\..*"))
        if (!parsed.scheme.equals("http", ignoreCase = true) || !isPrivate ||
            parsed.port !in 1..65535 || !token.matches(Regex("[a-fA-F0-9]{32}"))) return null
        return MeloWareLanPeer("MeloWare ($host)", host, parsed.port, token)
    }

    /** Receiver uses ExoPlayer, not an external browser. Poll metadata for song changes. */
    fun receiveFromMeloWare(peer: MeloWareLanPeer) {
        stopReceivingMeloWare()
        wifiReceivingPeer = peer
        wifiPeerMessage = "Đang kết nối tới ${peer.name}..."
        wifiPeerReceiveJob = scope.launch {
            var failures = 0
            while (true) {
                val result = runCatching { withContext(Dispatchers.IO) { fetchMeloWareLanTrack(peer) } }
                val remoteTrack = result.getOrNull()
                if (remoteTrack != null) {
                    failures = 0
                    wifiPeerMessage = null
                    val remoteId = "meloware_lan_${peer.address}_${peer.port}_${remoteTrack.key}"
                    if (wifiLastRemoteKey != remoteTrack.key || currentSong?.id != remoteId) {
                        wifiLastRemoteKey = remoteTrack.key
                        val source = Uri.parse("http://${peer.address}:${peer.port}/${peer.token}/current")
                        val remoteSong = SongItem(
                            id = remoteId,
                            title = remoteTrack.title,
                            artist = remoteTrack.artist,
                            album = remoteTrack.album,
                            uri = source,
                            durationMs = remoteTrack.durationMs,
                            source = SongSource.ONLINE_HTTP,
                            mimeType = remoteTrack.mimeType
                        )
                        playSong(remoteSong, listOf(remoteSong))
                        statusMessage = "Đang nghe từ MeloWare: ${peer.name}"
                    }
                } else {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException) throw error
                    failures++
                    wifiPeerMessage = if (failures >= 3) {
                        "Không truy cập được ${peer.name}. Kiểm tra Wi-Fi và máy đang chia sẻ."
                    } else "Đang thử kết nối lại ${peer.name}..."
                }
                delay(2_500L)
            }
        }
    }

    /** Browse folders shared by a selected MeloWare sender, independently of live-sync mode. */
    fun openMeloWareLibrary(peer: MeloWareLanPeer) {
        stopReceivingMeloWare()
        wifiLibraryJob?.cancel()
        wifiLibraryPeer = peer
        wifiRemoteFolders.clear()
        wifiRemoteSongs.clear()
        wifiRemoteFolderPath = null
        wifiRemoteNextOffset = -1
        wifiRemoteError = null
        wifiRemoteLoading = true
        wifiLibraryJob = scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    fetchMeloWareLibraryJson(peer, "library")
                }
            }
            if (wifiLibraryPeer != peer) return@launch
            wifiRemoteLoading = false
            result.onSuccess { data ->
                if (data.optString("app") != "MeloWare") {
                    wifiRemoteError = "Máy này không chạy MeloWare."
                    return@onSuccess
                }
                val folders = data.optJSONArray("folders") ?: JSONArray()
                for (index in 0 until folders.length()) {
                    val item = folders.optJSONObject(index) ?: continue
                    val folderPath = item.optString("path")
                    if (folderPath.isNotBlank()) {
                        wifiRemoteFolders.add(MeloWareLanFolder(folderPath, item.optInt("count", 0)))
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                wifiRemoteError = "Không đọc được thư mục máy gửi: ${error.localizedMessage ?: "Lỗi mạng"}"
            }
        }
    }

    fun openMeloWareLibraryUrl(rawUrl: String): Boolean {
        val peer = parseMeloWareLanUrl(rawUrl) ?: run {
            wifiPeerMessage = "Dán URL mạng nội bộ từ MeloWare máy A."
            return false
        }
        openMeloWareLibrary(peer)
        return true
    }

    /** Paginated remote folder listing: avoids loading thousands of songs at once. */
    fun loadMeloWareFolder(folder: String, append: Boolean = false) {
        val peer = wifiLibraryPeer ?: return
        if (folder !in wifiRemoteFolders.map(MeloWareLanFolder::path)) return
        if (wifiRemoteLoading) return
        val offset = if (append) wifiRemoteNextOffset.takeIf { it >= 0 } ?: return else 0
        wifiLibraryJob?.cancel()
        if (!append) {
            wifiRemoteSongs.clear()
            wifiRemoteNextOffset = -1
        }
        wifiRemoteFolderPath = folder
        wifiRemoteError = null
        wifiRemoteLoading = true
        wifiLibraryJob = scope.launch {
            val endpoint = "library?folder=${URLEncoder.encode(folder, "UTF-8")}&offset=$offset"
            val result = runCatching { withContext(Dispatchers.IO) { fetchMeloWareLibraryJson(peer, endpoint) } }
            if (wifiLibraryPeer != peer || wifiRemoteFolderPath != folder) return@launch
            wifiRemoteLoading = false
            result.onSuccess { data ->
                val tracks = data.optJSONArray("songs") ?: JSONArray()
                for (index in 0 until tracks.length()) {
                    val json = tracks.optJSONObject(index) ?: continue
                    val key = json.optString("key")
                    if (!key.matches(Regex("[0-9a-f]{24}"))) continue
                    wifiRemoteSongs.add(
                        MeloWareLanAudio(
                            key = key,
                            folder = folder,
                            title = json.optString("title", "Bài hát"),
                            artist = json.optString("artist", "Nghệ sĩ chưa biết"),
                            album = json.optString("album"),
                            durationMs = json.optLong("durationMs", 0L).coerceAtLeast(0),
                            mimeType = json.optString("mimeType", "audio/mpeg")
                        )
                    )
                }
                wifiRemoteNextOffset = data.optInt("nextOffset", -1)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                wifiRemoteError = "Không đọc được bài hát: ${error.localizedMessage ?: "Lỗi mạng"}"
            }
        }
    }

    fun loadMoreMeloWareFolder() {
        val path = wifiRemoteFolderPath ?: return
        loadMeloWareFolder(path, append = true)
    }

    fun backToMeloWareFolders() {
        wifiLibraryJob?.cancel()
        wifiRemoteLoading = false
        wifiRemoteFolderPath = null
        wifiRemoteSongs.clear()
        wifiRemoteNextOffset = -1
        wifiRemoteError = null
    }

    fun playFromMeloWareFolder(index: Int) {
        val peer = wifiLibraryPeer ?: return
        val sourceSongs = wifiRemoteSongs.toList()
        if (index !in sourceSongs.indices) return
        val songs = sourceSongs.map { remote ->
            SongItem(
                id = "meloware_folder_${peer.address}_${peer.port}_${remote.key}",
                title = remote.title,
                artist = remote.artist,
                album = remote.album,
                folderName = remote.folder,
                uri = Uri.parse("http://${peer.address}:${peer.port}/${peer.token}/track/${remote.key}"),
                durationMs = remote.durationMs,
                source = SongSource.ONLINE_HTTP,
                mimeType = remote.mimeType
            )
        }
        // Folder playback is independent of the sender's currently playing track.
        playSong(songs[index], songs)
    }

    fun closeMeloWareLibrary() {
        wifiLibraryJob?.cancel()
        wifiLibraryJob = null
        wifiLibraryPeer = null
        wifiRemoteFolders.clear()
        wifiRemoteSongs.clear()
        wifiRemoteFolderPath = null
        wifiRemoteError = null
        wifiRemoteLoading = false
        wifiRemoteNextOffset = -1
    }

    private fun fetchMeloWareLibraryJson(peer: MeloWareLanPeer, endpoint: String): JSONObject {
        val label = endpoint.substringBefore('?')
        Log.d(MELOWARE_LAN_LOG_TAG, "B: requesting $label from ${peer.address}:${peer.port}")
        val url = URL("http://${peer.address}:${peer.port}/${peer.token}/$endpoint")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 4_000
        connection.readTimeout = 8_000
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Máy A trả HTTP ${connection.responseCode}")
            }
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            Log.d(MELOWARE_LAN_LOG_TAG, "B: received $label response, chars=${response.length}")
            return JSONObject(response)
        } catch (error: Exception) {
            Log.e(MELOWARE_LAN_LOG_TAG, "B: failed to load $label", error)
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun stopReceivingMeloWare() {
        wifiPeerReceiveJob?.cancel()
        wifiPeerReceiveJob = null
        wifiReceivingPeer = null
        wifiLastRemoteKey = null
        wifiPeerMessage = null
        if (currentSong?.id?.startsWith("meloware_lan_") == true) player.pause()
    }

    private fun fetchMeloWareLanTrack(peer: MeloWareLanPeer): MeloWareSharedTrack {
        val endpoint = URL("http://${peer.address}:${peer.port}/${peer.token}/info")
        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 4_000
        connection.readTimeout = 5_000
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Không đọc được thông tin bài hát (HTTP ${connection.responseCode})")
            }
            val data = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(data)
            if (json.optString("app") != "MeloWare") throw IOException("Thiết bị không phải MeloWare")
            val songKey = json.optString("key").takeIf { it.isNotBlank() }
                ?: throw IOException("Không có mã bài hát")
            return MeloWareSharedTrack(
                key = songKey,
                title = json.optString("title", "Bài hát không tên"),
                artist = json.optString("artist", "Nghệ sĩ chưa biết"),
                album = json.optString("album"),
                durationMs = json.optLong("durationMs", 0L).coerceAtLeast(0L),
                mimeType = json.optString("mimeType", "audio/mpeg")
            )
        } finally {
            connection.disconnect()
        }
    }

    fun toggleWifiHost() {
        if (wifiHostActive) {
            if (castConnected) {
                statusMessage = "Đang phát qua Cast. Hãy ngắt Cast trước khi dừng máy chủ Wi-Fi."
                return
            }
            wifiHostRequestedByUser = false
            stopWifiHostInternal(clearUserRequest = true)
            statusMessage = "Đã dừng phát nhạc qua Wi-Fi"
            return
        }
        wifiAppShareRequested = false
        wifiAppShareEnabled = false
        wifiLanDiscovery.stopAdvertising()
        wifiHostRequestedByUser = true
        startWifiHostForCurrentSong(showMessage = true)
    }

    fun stopWifiHost() {
        if (castConnected) {
            statusMessage = "Đang phát qua Cast. Hãy ngắt Cast trước."
            return
        }
        wifiHostRequestedByUser = false
        stopWifiHostInternal(clearUserRequest = true)
    }

    fun castCurrentSong() {
        val song = currentSong ?: return
        if (song.source == SongSource.MOVIE) {
            statusMessage = "Movie dùng trình phát riêng; Cast âm thanh hỗ trợ các bài nhạc trong thư viện."
            return
        }
        initializeCast()
        val session = castContext?.sessionManager?.currentCastSession
        if (session == null || !session.isConnected) {
            statusMessage = "Hãy chọn TV hoặc thiết bị âm thanh trong bảng Cast."
            return
        }
        scope.launch {
            val streamUrl = runCatching { ensureWifiHostUrl(song) }
                .onFailure { wifiHostError = it.localizedMessage }
                .getOrNull()
            if (streamUrl == null) {
                errorMessage = wifiHostError ?: "Không tạo được luồng phát nội bộ cho Cast."
                return@launch
            }
            val remote = session.remoteMediaClient ?: run {
                errorMessage = "Thiết bị Cast chưa sẵn sàng nhận bài hát."
                return@launch
            }
            bindRemoteMediaClient(remote)
            val metadata = CastMediaMetadata(CastMediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
                putString(CastMediaMetadata.KEY_TITLE, song.title)
                putString(CastMediaMetadata.KEY_ARTIST, song.artist)
                putString(CastMediaMetadata.KEY_ALBUM_TITLE, song.album)
                artworkFor(song)?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                    ?.let { addImage(WebImage(Uri.parse(it))) }
            }
            val mediaInfo = MediaInfo.Builder(streamUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(song.mimeType ?: mimeForExtension(extensionForMime(song.mimeType)))
                .setMetadata(metadata)
                .setStreamDuration(song.durationMs.coerceAtLeast(durationMs))
                .build()
            remote.load(
                MediaLoadRequestData.Builder()
                    .setMediaInfo(mediaInfo)
                    .setAutoplay(true)
                    .setCurrentTime(positionMs.coerceAtLeast(0L))
                    .build()
            )
            player.pause()
            castConnected = true
            castDeviceName = session.castDevice?.friendlyName
            statusMessage = "Đang phát trên ${castDeviceName ?: "thiết bị Cast"}"
        }
    }

    fun openHome() {
        currentScreen = MusicScreen.HOME
    }

    fun openLibrary(source: LibrarySource = librarySource) {
        librarySource = source
        currentScreen = MusicScreen.LIBRARY
    }

    fun openNowPlaying() {
        if (currentSong != null) {
            previousScreen = currentScreen.takeIf { it != MusicScreen.NOW_PLAYING } ?: MusicScreen.HOME
            currentScreen = MusicScreen.NOW_PLAYING
        }
    }

    fun goBackFromPlayer() {
        currentScreen = previousScreen
    }

    fun allLibrarySongs(): List<SongItem> = buildList {
        addAll(localSongs)
        addAll(personalDriveSongs)
        addAll(publicDriveSongs)
        addAll(internetLibrarySongs())
    }.distinctBy { it.id }

    private fun isGoogleDriveSong(song: SongItem): Boolean =
        song.source == SongSource.PERSONAL_DRIVE || song.source == SongSource.PUBLIC_DRIVE

    private fun premiumAccessibleLibrarySongs(): List<SongItem> =
        if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
            allLibrarySongs().filterNot(::isGoogleDriveSong)
        } else {
            allLibrarySongs()
        }

    private fun ensurePremiumPlaybackAccess(song: SongItem): Boolean {
        if (MeloWarePremiumAccess.googleDriveRequiresPremium && isGoogleDriveSong(song)) {
            statusMessage = "Google Drive Music là quyền lợi Premium. Hãy kích hoạt Premium hoặc Premium Trial để tiếp tục."
            return false
        }
        return true
    }

    fun builtInSmartPlaylistSongs(type: BuiltInSmartPlaylist, limit: Int = 0): List<SongItem> =
        smartPlaylistSongs(type, allLibrarySongs(), smartPlaylists.statsBySongId, limit)

    fun builtInSmartPlaylistCount(type: BuiltInSmartPlaylist): Int =
        smartPlaylistCount(type, allLibrarySongs(), smartPlaylists.statsBySongId)

    fun customSmartPlaylistSongs(definition: SmartPlaylistDefinition): List<SongItem> =
        evaluateCustomSmartPlaylistSongs(
            definition = definition,
            songs = allLibrarySongs(),
            stats = smartPlaylists.statsBySongId,
            isFavorite = ::isFavorite
        )

    fun customSmartPlaylistCount(definition: SmartPlaylistDefinition): Int =
        countCustomSmartPlaylistSongs(
            definition = definition,
            songs = allLibrarySongs(),
            stats = smartPlaylists.statsBySongId,
            isFavorite = ::isFavorite
        )

    fun saveSmartPlaylist(draft: SmartPlaylistDraft, onSaved: ((Long) -> Unit)? = null) {
        smartPlaylists.savePlaylist(draft, onSaved)
    }

    fun deleteSmartPlaylist(playlistId: Long) {
        smartPlaylists.deletePlaylist(playlistId)
    }

    fun playbackStats(song: SongItem): SongPlaybackStatsEntity? =
        smartPlaylists.statsBySongId[song.id]

    fun smartDuplicateGroups(): List<SmartDuplicateGroup> =
        findSmartDuplicateGroups(localSongs)

    fun filteredLibrarySongs(): List<SongItem> {
        val sourceFiltered = when (librarySource) {
            LibrarySource.ALL -> premiumAccessibleLibrarySongs()
            LibrarySource.LOCAL -> localSongs.filter { it.source == SongSource.LOCAL_MEDIASTORE }
            LibrarySource.PERSONAL_DRIVE -> if (MeloWarePremiumAccess.googleDriveRequiresPremium) emptyList()
                else personalDriveSongs.toList()
            LibrarySource.PUBLIC_DRIVE -> if (MeloWarePremiumAccess.googleDriveRequiresPremium) emptyList()
                else publicDriveSongs.toList()
            LibrarySource.INTERNET -> internetLibrarySongs()
            LibrarySource.DOWNLOADED -> localSongs.filter { it.source == SongSource.DOWNLOADED }
            LibrarySource.FAVORITES -> premiumAccessibleLibrarySongs().filter { isFavorite(it) }
        }
        val query = normalizeText(libraryQuery)
        if (query.isBlank()) return sourceFiltered
        return sourceFiltered.filter { song ->
            normalizeText("${song.title} ${song.artist} ${song.album} ${song.folderName}").contains(query)
        }
    }

    fun groupedAlbums(): Map<String, List<SongItem>> = filteredLibrarySongs()
        .groupBy { it.album.ifBlank { "Album chưa xác định" } }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    fun groupedArtists(): Map<String, List<SongItem>> = filteredLibrarySongs()
        .groupBy { it.artist.ifBlank { "Nghệ sĩ chưa biết" } }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    fun groupedFolders(): Map<String, List<SongItem>> = filteredLibrarySongs()
        .groupBy { it.folderName.ifBlank { sourceLabel(it.source) } }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)

    fun sourceCount(source: LibrarySource): Int = when (source) {
        LibrarySource.ALL -> premiumAccessibleLibrarySongs().size
        LibrarySource.LOCAL -> localSongs.count { it.source == SongSource.LOCAL_MEDIASTORE }
        LibrarySource.PERSONAL_DRIVE -> personalDriveSongs.size
        LibrarySource.PUBLIC_DRIVE -> publicDriveSongs.size
        LibrarySource.INTERNET -> internetLibrarySongs().size
        LibrarySource.DOWNLOADED -> localSongs.count { it.source == SongSource.DOWNLOADED }
        LibrarySource.FAVORITES -> premiumAccessibleLibrarySongs().count { isFavorite(it) }
    }

    fun recentForHome(limit: Int = 12): List<SongItem> = recentSongs
        .asSequence()
        .filter { !MeloWarePremiumAccess.googleDriveRequiresPremium || !isGoogleDriveSong(it) }
        .take(limit)
        .toList()

    fun recommendations(limit: Int = 8): List<SongItem> {
        val accessible = premiumAccessibleLibrarySongs()
        val favorites = accessible.filter { isFavorite(it) }
        val remaining = accessible.filterNot { song -> favorites.any { it.id == song.id } }
        return (favorites + remaining).distinctBy { it.id }.take(limit)
    }

    fun newlyAddedFromDrive(limit: Int = 8): List<SongItem> =
        if (MeloWarePremiumAccess.googleDriveRequiresPremium) emptyList()
        else (personalDriveSongs.asReversed() + publicDriveSongs.asReversed())
            .distinctBy { it.id }
            .take(limit)

    fun queueFromCurrent(): List<SongItem> {
        if (currentIndex !in queue.indices) return emptyList()
        return queue.drop(currentIndex)
    }

    fun scanLocalMusic() {
        if (isScanningLocal) return
        isScanningLocal = true
        errorMessage = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { queryLocalAudio() }
            }
            isScanningLocal = false
            result.onSuccess { songs ->
                localSongs.clear()
                localSongs.addAll(songs)
                // If the sender rescans files, refresh the already-authorized folder list.
                wifiSharedFolderPaths.removeAll { folder -> wifiShareableFolders.none { it.path == folder } }
                refreshMeloWareSharedFolders()
                // Quét và cập nhật thư viện ngầm; không hiển thị Toast khi hoàn tất.
            }.onFailure { throwable ->
                errorMessage = "Không thể quét nhạc trên điện thoại: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }

    /**
     * Access tokens are kept in memory only. Google Identity Services can silently issue
     * a fresh token on the next authorization request after the user has granted consent.
     */
    fun setPersonalDriveAuthorization(accessToken: String, accountEmail: String?) {
        if (accessToken.isBlank()) {
            errorMessage = "Google không trả access token cho Drive. Hãy cấp lại quyền."
            return
        }
        personalDriveAccessToken = accessToken
        personalDriveInvalidToken = null
        personalDriveAccountEmail = accountEmail?.takeIf { it.isNotBlank() }
        personalDriveNeedsAuthorization = false
        personalDriveError = null
        personalDriveAccountEmail?.let { email ->
            prefs.edit().putString(KEY_PERSONAL_DRIVE_ACCOUNT, email).apply()
        }

        val retryId = pendingPersonalDrivePlaybackId
        pendingPersonalDrivePlaybackId = null
        if (retryId != null) {
            val retryIndex = queue.indexOfFirst { it.id == retryId }
            if (retryIndex >= 0) playQueueIndex(retryIndex)
        }
    }

    /** Imports only the Drive items that the user explicitly shared through Google Picker. */
    fun importPersonalDrivePickerSelection(
        accessToken: String,
        accountEmail: String?,
        pickedIds: List<String>
    ) {
        val validIds = pickedIds
            .map { it.trim() }
            .filter { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
            .distinct()
        if (validIds.isEmpty()) {
            statusMessage = "Bạn chưa chọn file hoặc thư mục Google Drive nào."
            return
        }

        setPersonalDriveAuthorization(accessToken, accountEmail)
        if (personalDriveBusy) return
        personalDriveBusy = true
        personalDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    PersonalDriveApi.importPickerSelection(accessToken, validIds)
                }
            }
            personalDriveBusy = false
            result.onSuccess { songs ->
                if (songs.isEmpty()) {
                    personalDriveError =
                        "Các mục đã chọn không có file âm thanh được hỗ trợ. Nếu đã chọn thư mục nhưng Google không cấp quyền cho file con, hãy chọn trực tiếp các bài hát trong Picker."
                } else {
                    mergePersonalDriveSongs(songs)
                    librarySource = LibrarySource.PERSONAL_DRIVE
                    libraryTab = LibraryTab.SONGS
                    currentScreen = MusicScreen.LIBRARY
                    statusMessage = "Đã thêm ${songs.size} bài từ Google Drive"
                }
            }.onFailure(::handlePersonalDriveFailure)
        }
    }

    fun requestPersonalDriveAuthorization() {
        personalDriveNeedsAuthorization = true
    }

    fun consumePersonalDriveAuthorizationRequest() {
        personalDriveNeedsAuthorization = false
    }

    /** Returns a token rejected by Drive so Google Play services can evict it from cache. */
    fun consumePersonalDriveInvalidToken(): String? {
        val token = personalDriveInvalidToken
        personalDriveInvalidToken = null
        return token
    }

    /** Clears only the current app session. It does not revoke the Google OAuth grant. */
    fun clearPersonalDriveSession(
        removeSongs: Boolean = true,
        forgetAccount: Boolean = false
    ) {
        personalDriveAccessToken = null
        personalDriveInvalidToken = null
        personalDriveBusy = false
        personalDriveError = null
        personalDriveNeedsAuthorization = false
        personalDrivePath.clear()
        personalDriveEntries = emptyList()
        personalDriveSelectedIds.clear()
        showPersonalDriveDialog = false
        pendingPersonalDrivePlaybackId = null
        if (removeSongs) {
            personalDriveSongs.clear()
            saveSongList(KEY_PERSONAL_DRIVE_SONGS, emptyList())
        }
        if (forgetAccount) {
            personalDriveAccountEmail = null
            prefs.edit().remove(KEY_PERSONAL_DRIVE_ACCOUNT).apply()
        }
    }

    /** Called after Google AuthorizationClient.revokeAccess completes. */
    fun disconnectPersonalDrive() {
        clearPersonalDriveSession(removeSongs = true, forgetAccount = true)
        statusMessage = "Đã ngắt quyền Google Drive của MeloWare"
    }

    fun openPersonalDriveRoot() {
        val token = requirePersonalDriveToken() ?: return
        personalDriveBusy = true
        personalDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    PersonalDriveApi.listChildren(token, PersonalDriveFolder("root", "Drive của tôi"))
                }
            }
            personalDriveBusy = false
            result.onSuccess { entries ->
                personalDrivePath.clear()
                personalDrivePath.add(PersonalDriveFolder("root", "Drive của tôi"))
                personalDriveEntries = entries
                personalDriveSelectedIds.clear()
            }.onFailure(::handlePersonalDriveFailure)
        }
    }

    fun openPersonalDriveFolder(entry: PersonalDriveEntry) {
        val token = requirePersonalDriveToken() ?: return
        if (!entry.isFolder || personalDriveBusy) return
        val folder = PersonalDriveFolder(entry.id, entry.name)
        personalDriveBusy = true
        personalDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { PersonalDriveApi.listChildren(token, folder) }
            }
            personalDriveBusy = false
            result.onSuccess { entries ->
                personalDrivePath.add(folder)
                personalDriveEntries = entries
                personalDriveSelectedIds.clear()
            }.onFailure(::handlePersonalDriveFailure)
        }
    }

    fun goBackPersonalDrive() {
        val token = requirePersonalDriveToken() ?: return
        if (personalDrivePath.size <= 1 || personalDriveBusy) return
        val parent = personalDrivePath[personalDrivePath.lastIndex - 1]
        personalDriveBusy = true
        personalDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { PersonalDriveApi.listChildren(token, parent) }
            }
            personalDriveBusy = false
            result.onSuccess { entries ->
                personalDrivePath.removeAt(personalDrivePath.lastIndex)
                personalDriveEntries = entries
                personalDriveSelectedIds.clear()
            }.onFailure(::handlePersonalDriveFailure)
        }
    }

    fun isPersonalDrivePlayable(entry: PersonalDriveEntry): Boolean =
        PersonalDriveApi.isPlayable(entry)

    fun togglePersonalDriveSelection(entry: PersonalDriveEntry) {
        if (!PersonalDriveApi.isPlayable(entry)) return
        if (personalDriveSelectedIds.containsKey(entry.id)) {
            personalDriveSelectedIds.remove(entry.id)
        } else {
            personalDriveSelectedIds[entry.id] = true
        }
    }

    fun toggleAllPersonalDrivePlayable() {
        val playable = personalDriveEntries.filter(PersonalDriveApi::isPlayable)
        if (playable.isNotEmpty() && playable.all { personalDriveSelectedIds.containsKey(it.id) }) {
            personalDriveSelectedIds.clear()
        } else {
            personalDriveSelectedIds.clear()
            playable.forEach { personalDriveSelectedIds[it.id] = true }
        }
    }

    fun addSelectedPersonalDrive(playNow: Boolean) {
        val folderName = personalDrivePath.joinToString(" / ") { it.name }
        val selected = personalDriveEntries
            .filter { personalDriveSelectedIds.containsKey(it.id) && PersonalDriveApi.isPlayable(it) }
            .map { PersonalDriveApi.toSong(it, folderName) }
        if (selected.isEmpty()) {
            personalDriveError = "Hãy chọn ít nhất một file âm thanh."
            return
        }
        val addedSongs = mergePersonalDriveSongs(selected)
        if (playNow) {
            showPersonalDriveDialog = false
            playCollection(addedSongs, 0)
        } else {
            statusMessage = "Đã thêm ${selected.size} bài từ Drive của tôi"
        }
    }

    fun addCurrentPersonalDriveFolder(playNow: Boolean) {
        val token = requirePersonalDriveToken() ?: return
        val folder = personalDrivePath.lastOrNull() ?: return
        personalDriveBusy = true
        personalDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    PersonalDriveApi.collectAudioRecursively(token, folder)
                }
            }
            personalDriveBusy = false
            result.onSuccess { songs ->
                if (songs.isEmpty()) {
                    personalDriveError = "Thư mục này không có file âm thanh được hỗ trợ."
                } else {
                    val addedSongs = mergePersonalDriveSongs(songs)
                    if (playNow) {
                        showPersonalDriveDialog = false
                        playCollection(addedSongs, 0)
                    } else {
                        statusMessage = "Đã thêm ${songs.size} bài từ thư mục ${folder.name}"
                    }
                }
            }.onFailure(::handlePersonalDriveFailure)
        }
    }

    private fun mergePersonalDriveSongs(songs: List<SongItem>): List<SongItem> {
        val existingById = personalDriveSongs.associateBy { it.id }
        val now = System.currentTimeMillis()
        val stamped = songs.mapIndexed { index, song ->
            val existing = existingById[song.id]
            song.copy(
                genre = song.genre.ifBlank { existing?.genre.orEmpty() },
                dateAddedMs = existing?.dateAddedMs?.takeIf { it > 0L }
                    ?: song.dateAddedMs.takeIf { it > 0L }
                    ?: (now + index)
            )
        }
        val merged = (personalDriveSongs + stamped).distinctBy { it.id }
        personalDriveSongs.clear()
        personalDriveSongs.addAll(merged)
        saveSongList(KEY_PERSONAL_DRIVE_SONGS, personalDriveSongs)
        return stamped
    }

    private fun requirePersonalDriveToken(): String? {
        val token = personalDriveAccessToken
        if (token.isNullOrBlank()) {
            personalDriveError = "Hãy mở Google Picker và chọn lại file Google Drive."
            personalDriveNeedsAuthorization = true
            return null
        }
        return token
    }

    private fun handlePersonalDriveFailure(throwable: Throwable) {
        if (throwable is PersonalDriveAuthorizationException) {
            personalDriveInvalidToken = personalDriveAccessToken
            personalDriveAccessToken = null
            personalDriveNeedsAuthorization = true
        }
        personalDriveError = throwable.localizedMessage ?: "Không đọc được Google Drive cá nhân"
    }

    fun setPublicLink(value: String) {
        if (publicLinkText == value) return
        publicLinkText = value
        publicRoot = null
        publicPath.clear()
        publicEntries = emptyList()
        publicSelectedIds.clear()
        publicDriveError = null
    }

    fun clearPublicLink() {
        setPublicLink("")
        prefs.edit().remove(KEY_PUBLIC_LINK).apply()
    }

    fun openPublicDriveLink() {
        if (publicDriveBusy) return
        val parsed = try {
            SharedDriveApi.parseFolderLink(publicLinkText)
        } catch (error: Exception) {
            publicDriveError = error.localizedMessage
            return
        }
        publicDriveBusy = true
        publicDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val rootFolder = SharedDriveApi.readRoot(context, parsed)
                    val entries = SharedDriveApi.listChildren(context, parsed, rootFolder)
                    rootFolder to entries
                }
            }
            publicDriveBusy = false
            result.onSuccess { (folder, entries) ->
                publicRoot = parsed
                publicPath.clear()
                publicPath.add(folder)
                publicEntries = entries
                publicSelectedIds.clear()
                prefs.edit().putString(KEY_PUBLIC_LINK, publicLinkText.trim()).apply()
            }.onFailure { throwable ->
                publicDriveError = throwable.localizedMessage ?: "Không mở được link Google Drive công khai"
            }
        }
    }

    fun openPublicFolder(entry: SharedDriveEntry) {
        val root = publicRoot ?: return
        if (!entry.isFolder || publicDriveBusy) return
        val folder = SharedDriveFolder(entry.id, entry.name, entry.resourceKey)
        publicDriveBusy = true
        publicDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { SharedDriveApi.listChildren(context, root, folder) }
            }
            publicDriveBusy = false
            result.onSuccess { loaded ->
                publicPath.add(folder)
                publicEntries = loaded
                publicSelectedIds.clear()
            }.onFailure { throwable ->
                publicDriveError = throwable.localizedMessage ?: "Không đọc được thư mục Drive"
            }
        }
    }

    fun goBackPublicFolder() {
        val root = publicRoot ?: return
        if (publicPath.size <= 1 || publicDriveBusy) return
        val parent = publicPath[publicPath.lastIndex - 1]
        publicDriveBusy = true
        publicDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { SharedDriveApi.listChildren(context, root, parent) }
            }
            publicDriveBusy = false
            result.onSuccess { loaded ->
                publicPath.removeAt(publicPath.lastIndex)
                publicEntries = loaded
                publicSelectedIds.clear()
            }.onFailure { throwable ->
                publicDriveError = throwable.localizedMessage ?: "Không quay lại được thư mục"
            }
        }
    }

    fun isPublicEntryPlayable(entry: SharedDriveEntry): Boolean =
        SharedDriveApi.isPlayable(entry)

    fun togglePublicEntry(entry: SharedDriveEntry) {
        if (!SharedDriveApi.isPlayable(entry)) return
        if (publicSelectedIds[entry.id] == true) publicSelectedIds.remove(entry.id)
        else publicSelectedIds[entry.id] = true
    }

    fun toggleAllPublicEntries() {
        val playable = publicEntries.filter(SharedDriveApi::isPlayable)
        if (playable.isEmpty()) return
        if (playable.all { publicSelectedIds[it.id] == true }) {
            playable.forEach { publicSelectedIds.remove(it.id) }
        } else {
            playable.forEach { publicSelectedIds[it.id] = true }
        }
    }

    fun addSelectedPublicSongs(playImmediately: Boolean) {
        val root = publicRoot ?: return
        val folderName = publicPath.lastOrNull()?.name.orEmpty()
        val selected = publicEntries
            .filter { publicSelectedIds[it.id] == true && SharedDriveApi.isPlayable(it) }
            .map { SharedDriveApi.toSong(it, root, folderName) }
        if (selected.isEmpty()) {
            publicDriveError = "Bạn chưa chọn bài hát nào"
            return
        }
        val addedSongs = mergePublicSongs(selected)
        showPublicDriveDialog = false
        if (playImmediately) playCollection(addedSongs, 0)
        else statusMessage = "Đã thêm ${selected.size} bài từ link Drive"
    }

    fun addCurrentPublicFolder(playImmediately: Boolean) {
        val root = publicRoot ?: return
        val folder = publicPath.lastOrNull() ?: return
        if (publicDriveBusy) return
        publicDriveBusy = true
        publicDriveError = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    SharedDriveApi.collectAudioRecursively(context, root, folder)
                }
            }
            publicDriveBusy = false
            result.onSuccess { songs ->
                if (songs.isEmpty()) {
                    publicDriveError = "Thư mục này không có file âm thanh được hỗ trợ"
                } else {
                    val addedSongs = mergePublicSongs(songs)
                    showPublicDriveDialog = false
                    if (playImmediately) playCollection(addedSongs, 0)
                    else statusMessage = "Đã thêm ${songs.size} bài từ thư mục Drive"
                }
            }.onFailure { throwable ->
                publicDriveError = throwable.localizedMessage ?: "Không quét được thư mục Drive"
            }
        }
    }

    private fun mergePublicSongs(newSongs: List<SongItem>): List<SongItem> {
        val existingById = publicDriveSongs.associateBy { it.id }
        val now = System.currentTimeMillis()
        val stamped = newSongs.mapIndexed { index, song ->
            val existing = existingById[song.id]
            song.copy(
                genre = song.genre.ifBlank { existing?.genre.orEmpty() },
                dateAddedMs = existing?.dateAddedMs?.takeIf { it > 0L }
                    ?: song.dateAddedMs.takeIf { it > 0L }
                    ?: (now + index)
            )
        }
        val merged = (publicDriveSongs + stamped).distinctBy { it.id }
        publicDriveSongs.clear()
        publicDriveSongs.addAll(merged)
        saveSongList(KEY_PUBLIC_SONGS, publicDriveSongs)
        return stamped
    }

    fun changeOnlineSearchMode(mode: OnlineSearchMode) {
        if (onlineSearchMode == mode) return
        onlineSearchMode = mode
        onlineSearchResults.clear()
        onlineSearchError = null
    }

    fun searchOnline(query: String = onlineSearchQuery) {
        val cleanQuery = query.trim()
        onlineSearchQuery = cleanQuery
        if (cleanQuery.isBlank()) {
            onlineSearchResults.clear()
            onlineSearchError = "Hãy nhập tên bài hát hoặc ca sĩ."
            return
        }
        if (onlineSearchBusy) return
        onlineSearchBusy = true
        onlineSearchError = null
        scope.launch {
            val mode = onlineSearchMode
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    when (mode) {
                        OnlineSearchMode.APPLE_MUSIC -> searchAppleMusic(cleanQuery)
                        OnlineSearchMode.MOVIE -> searchMovie(cleanQuery)
                    }
                }
            }
            onlineSearchBusy = false
            result.onSuccess { songs ->
                onlineSearchResults.clear()
                onlineSearchResults.addAll(songs)
                if (songs.isEmpty()) {
                    onlineSearchError = "Không tìm thấy kết quả cho '$cleanQuery'."
                }
            }.onFailure { error ->
                onlineSearchError = "Không thể tìm kiếm: ${error.localizedMessage ?: "Lỗi kết nối"}"
            }
        }
    }

    private fun searchAppleMusic(query: String): List<SongItem> {
        val url = "https://itunes.apple.com/search?term=${URLEncoder.encode(query, "UTF-8")}&media=music&entity=song&limit=30&country=US"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Tunertools-Android/2.0")
        return try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Music HTTP ${connection.responseCode}")
            }
            val array = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                .optJSONArray("results") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val preview = item.optString("previewUrl").replace("http://", "https://")
                    if (!preview.startsWith("https://")) continue
                    val rawArtwork = item.optString("artworkUrl100")
                    add(
                        SongItem(
                            id = "apple_${item.optString("trackId", index.toString())}",
                            title = item.optString("trackName", "Không rõ tên"),
                            artist = item.optString("artistName", "Không rõ nghệ sĩ"),
                            album = item.optString("collectionName", "Music"),
                            folderName = "Music Preview",
                            uri = Uri.parse(preview),
                            durationMs = 30_000L,
                            source = SongSource.ONLINE_HTTP,
                            mimeType = "audio/mp4",
                            artworkUrl = rawArtwork
                                .replace("100x100bb", "600x600bb")
                                .takeIf { it.startsWith("https://") }
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun searchMovie(query: String): List<SongItem> {
        val url = "https://cut.tunertools.top/api/v1/search?query=${URLEncoder.encode(query, "UTF-8")}&limit=30"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "Tunertools-Android/2.0")
        return try {
            if (connection.responseCode !in 200..299) {
                throw IOException("Movie search HTTP ${connection.responseCode}")
            }
            val json = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
            if (!json.optString("status").equals("success", ignoreCase = true)) return emptyList()
            val array = json.optJSONArray("data") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val rawId = item.optString("id")
                    val videoId = rawId.removePrefix("vid_").takeIf { it.isNotBlank() } ?: continue
                    val durationSeconds = item.optLong("duration", 0L)
                    add(
                        SongItem(
                            id = if (rawId.startsWith("vid_")) rawId else "vid_$videoId",
                            title = item.optString("title", "Movie Video"),
                            artist = item.optString("artist", "Movie").let {
                                if (it.equals("YouTube", ignoreCase = true)) "Movie" else it
                            },
                            album = "Movie",
                            folderName = "Movie",
                            uri = Uri.parse("https://youtu.be/$videoId"),
                            durationMs = if (durationSeconds > 0L) durationSeconds * 1000L else 0L,
                            source = SongSource.MOVIE,
                            mimeType = "video/youtube",
                            artworkUrl = item.optString("artworkUrl").takeIf { it.startsWith("https://") }
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun playOnlineResult(song: SongItem) {
        val list = onlineSearchResults.toList().ifEmpty { listOf(song) }
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playCollection(list, index)
        openNowPlaying()
    }

    fun attachMovieWebView(webView: WebView) {
        movieWebView = webView
    }

    fun detachMovieWebView(webView: WebView) {
        if (movieWebView === webView) movieWebView = null
    }

    fun updateMoviePlaybackState(
        position: Long,
        duration: Long,
        playerState: Int,
        ready: Boolean,
        errorCode: Int = 0
    ) {
        if (currentSong?.source != SongSource.MOVIE) return
        positionMs = position.coerceAtLeast(0L)
        if (duration > 0L) durationMs = duration
        isPlaying = playerState == 1
        isLoading = !ready || playerState == -1 || playerState == 3

        if (errorCode != 0) {
            isPlaying = false
            isLoading = false
            val message = when (errorCode) {
                2 -> "ID hoặc tham số video Movie không hợp lệ."
                5 -> "Movie không thể phát video này bằng trình phát HTML5 trên thiết bị hiện tại."
                100 -> "Video này đã bị xóa, đặt riêng tư hoặc không còn tồn tại."
                101, 150 -> "Chủ sở hữu video không cho phép phát trong ứng dụng nhúng. Hãy dùng nút Movie để mở video bên ngoài."
                153 -> "Nguồn video chưa nhận diện được ứng dụng nhúng. Hãy cập nhật ứng dụng rồi thử lại."
                else -> "Không thể phát video Movie trong ứng dụng (mã lỗi $errorCode)."
            }
            if (errorMessage != message) errorMessage = message
        }
    }

    fun movieVideoId(song: SongItem? = currentSong): String? {
        val resolvedSong = song ?: return null
        val text = resolvedSong.uri.toString()
        val candidate = when {
            "youtu.be/" in text -> text.substringAfter("youtu.be/").substringBefore('?').substringBefore('&')
            "watch?v=" in text -> text.substringAfter("watch?v=").substringBefore('&')
            "/embed/" in text -> text.substringAfter("/embed/").substringBefore('?').substringBefore('&')
            else -> resolvedSong.id.removePrefix("vid_")
        }
        return candidate.takeIf { Regex("[A-Za-z0-9_-]{11}").matches(it) }
    }

    fun movieEmbedUrl(song: SongItem? = currentSong): String {
        val resolvedSong = song ?: return ""
        val videoId = movieVideoId(resolvedSong) ?: return resolvedSong.uri.toString()
        val appIdentity = "https://${context.packageName.lowercase(Locale.ROOT)}"
        val encodedIdentity = URLEncoder.encode(appIdentity, "UTF-8")
        return "https://www.youtube.com/embed/$videoId" +
                "?autoplay=1&playsinline=1&controls=1&rel=0&fs=1&enablejsapi=1" +
                "&origin=$encodedIdentity&widget_referrer=$encodedIdentity"
    }

    fun openCurrentMovieExternally() {
        val song = currentSong ?: return
        val videoId = movieVideoId(song)
        val webUri = Uri.parse(
            videoId?.let { "https://www.youtube.com/watch?v=$it" }
                ?: song.uri.toString()
        )
        val movieIntent = videoId?.let {
            Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$it")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            if (movieIntent != null && movieIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(movieIntent)
            } else {
                context.startActivity(webIntent)
            }
        }.onFailure {
            errorMessage = "Không tìm thấy ứng dụng có thể mở video."
        }
    }

    fun playCollection(collection: List<SongItem>, index: Int) {
        if (collection.isEmpty()) return
        val requestedSong = collection.getOrNull(index.coerceIn(0, collection.lastIndex)) ?: return
        if (!ensurePremiumPlaybackAccess(requestedSong)) return
        val accessibleCollection = if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
            collection.filterNot(::isGoogleDriveSong)
        } else {
            collection
        }.distinctBy { it.id }
        if (accessibleCollection.isEmpty()) return
        val selectedIndex = accessibleCollection.indexOfFirst { it.id == requestedSong.id }
            .takeIf { it >= 0 } ?: return
        queue.clear()
        queue.addAll(accessibleCollection)
        currentIndex = selectedIndex
        saveQueue()
        prepareCurrent(autoPlay = true)
    }

    fun playSong(song: SongItem, collection: List<SongItem> = premiumAccessibleLibrarySongs()) {
        if (!ensurePremiumPlaybackAccess(song)) return
        val list = collection.takeIf { it.any { item -> item.id == song.id } } ?: listOf(song)
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playCollection(list, index)
    }

    fun playQueueIndex(index: Int) {
        if (index !in queue.indices) return
        val target = queue[index]
        if (!ensurePremiumPlaybackAccess(target)) return
        currentIndex = index
        prepareCurrent(autoPlay = true)
    }

    /** MediaSession pauses playback without accidentally starting an idle/buffering player. */
    internal fun pauseForSystemMediaControls() {
        cancelSoundCrossfade()
        if (player.playWhenReady || player.isPlaying) player.pause()
        MeloWavePlaybackRuntime.onPlaybackChanged()
    }

    fun togglePlayback() {
        if (crossfadeOutgoing != null) cancelSoundCrossfade()
        if (castConnected) {
            castContext?.sessionManager?.currentCastSession?.remoteMediaClient?.togglePlayback()
            return
        }
        val song = currentSong
        if (song != null && !ensurePremiumPlaybackAccess(song)) {
            runCatching { player.pause() }
            isPlaying = false
            return
        }
        if (song?.source == SongSource.MOVIE) {
            movieWebView?.evaluateJavascript(
                "window.tunertoolsToggle ? window.tunertoolsToggle() : 'not_ready';",
                null
            )
            return
        }
        when {
            song == null -> {
                val first = premiumAccessibleLibrarySongs().firstOrNull() ?: return
                playSong(first)
            }
            player.currentMediaItem?.mediaId != song.id ||
                    (song.source == SongSource.INTERNET_P2P && player.playbackState == Player.STATE_IDLE) ->
                prepareCurrent(autoPlay = true)
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0L)
                player.play()
            }
            player.isPlaying -> player.pause()
            else -> player.play()
        }
    }

    fun previous() {
        if (queue.isEmpty()) return
        if (positionMs > 3_000L) {
            seekTo(0L)
            return
        }
        val target = when {
            shuffleEnabled && queue.size > 1 -> randomOtherIndex(currentIndex)
            currentIndex > 0 -> currentIndex - 1
            repeatSetting == RepeatSetting.ALL -> queue.lastIndex
            else -> 0
        }
        currentIndex = target
        prepareCurrent(autoPlay = true)
    }

    fun next() {
        if (queue.isEmpty()) return
        val target = when {
            repeatSetting == RepeatSetting.ONE -> currentIndex
            shuffleEnabled && queue.size > 1 -> randomOtherIndex(currentIndex)
            currentIndex < queue.lastIndex -> currentIndex + 1
            repeatSetting == RepeatSetting.ALL -> 0
            else -> return
        }
        if (trySoundCrossfade(target, automatic = false)) return
        currentIndex = target
        prepareCurrent(autoPlay = true)
    }

    fun seekTo(position: Long) {
        if (crossfadeOutgoing != null || crossfadeStandby != null) cancelSoundCrossfade()
        val safePosition = position.coerceAtLeast(0L)
        if (castConnected) {
            castContext?.sessionManager?.currentCastSession?.remoteMediaClient?.seek(safePosition)
            positionMs = safePosition
            return
        }
        if (currentSong?.source == SongSource.MOVIE) {
            movieWebView?.evaluateJavascript(
                "window.tunertoolsSeek ? window.tunertoolsSeek(${safePosition / 1000.0}) : -1;",
                null
            )
        } else {
            player.seekTo(safePosition)
        }
        positionMs = safePosition
        MeloWavePlaybackRuntime.onSeekChanged()
    }

    fun cycleRepeat() {
        repeatSetting = when (repeatSetting) {
            RepeatSetting.OFF -> RepeatSetting.ALL
            RepeatSetting.ALL -> RepeatSetting.ONE
            RepeatSetting.ONE -> RepeatSetting.OFF
        }
    }

    fun toggleShuffle() {
        shuffleEnabled = !shuffleEnabled
    }

    fun toggleFavorite(song: SongItem) {
        if (isFavorite(song)) favoriteIds.remove(song.id) else favoriteIds[song.id] = true
        prefs.edit().putStringSet(KEY_FAVORITES, favoriteIds.keys.toSet()).apply()
    }

    fun isFavorite(song: SongItem): Boolean = favoriteIds[song.id] == true

    private fun customArtworkKey(songId: String) = songId.sha256().take(32)

    private fun customArtworkPreferenceKey(hash: String) = "melowave_custom_cover_$hash"

    fun hasCustomArtwork(song: SongItem): Boolean =
        customArtworkByKey.containsKey(customArtworkKey(song.id))

    fun artworkFor(song: SongItem): String? =
        customArtworkByKey[customArtworkKey(song.id)] ?: artworkById[song.id] ?: song.artworkUrl

    /**
     * Copy the selected image into app-private storage: no gallery permission is required,
     * and the photo stays available even after the photo picker's URI grant expires.
     * Original MP3/FLAC metadata and the remote cover are never overwritten.
     */
    fun setCustomArtwork(song: SongItem, photoUri: Uri, normalizedCrop: RectF? = null) {
        if (MeloWarePremiumAccess.albumStudioRequiresPremium) {
            discardSearchedArtworkTemp(photoUri)
            statusMessage = "Album Studio Pro là quyền lợi Premium."
            return
        }
        scope.launch {
            val hash = customArtworkKey(song.id)
            val result = withContext(Dispatchers.IO) {
                try {
                    runCatching {
                        val folder = File(context.filesDir, "melowave_custom_covers")
                        if (!folder.isDirectory && !folder.mkdirs()) {
                            throw IOException("Không tạo được thư mục ảnh bìa")
                        }
                        val out = File(folder, "${hash}_${UUID.randomUUID()}.jpg")
                        try {
                            val decoded = decodeChosenArtwork(photoUri)
                            try {
                                val ready = if (normalizedCrop != null) {
                                    cropChosenArtwork(decoded, normalizedCrop)
                                } else decoded
                                try {
                                    FileOutputStream(out).use { stream ->
                                        if (!ready.compress(Bitmap.CompressFormat.JPEG, 90, stream)) {
                                            throw IOException("Không thể lưu ảnh JPEG")
                                        }
                                    }
                                } finally {
                                    if (ready !== decoded) ready.recycle()
                                }
                            } finally {
                                decoded.recycle()
                            }
                            if (out.length() <= 0L) throw IOException("Ảnh không có dữ liệu")
                            Uri.fromFile(out).toString()
                        } catch (failure: Exception) {
                            out.delete()
                            throw failure
                        }
                    }
                } finally {
                    // Delete only our own temporary WebView image, after the copy/crop
                    // finishes (never before the coroutine has opened the input file).
                    discardSearchedArtworkTemp(photoUri)
                }
            }
            result.onSuccess { savedUri ->
                val oldUri = customArtworkByKey.put(hash, savedUri)
                prefs.edit().putString(customArtworkPreferenceKey(hash), savedUri).apply()
                scope.launch(Dispatchers.IO) { deleteOldCustomCover(oldUri) }
                statusMessage = "Đã cập nhật ảnh bìa cho ${song.title}"
            }.onFailure { failure ->
                statusMessage = "Không đổi được ảnh bìa: ${failure.localizedMessage ?: "Ảnh không hợp lệ"}"
            }
        }
    }

    /** Never touch Gallery photos; only delete cache files created by the WebView picker. */
    fun discardSearchedArtworkTemp(photoUri: Uri) {
        if (photoUri.scheme != "file") return
        runCatching {
            val folder = File(context.cacheDir, "melowave_web_cover_search").canonicalFile
            val item = photoUri.path?.let(::File)?.canonicalFile ?: return
            if (item.parentFile == folder && item.name.startsWith("cover_") &&
                item.name.endsWith(".img")) {
                item.delete()
            }
        }
    }

    fun restoreAutomaticArtwork(song: SongItem) {
        if (MeloWarePremiumAccess.albumStudioRequiresPremium) {
            statusMessage = "Album Studio Pro là quyền lợi Premium."
            return
        }
        val hash = customArtworkKey(song.id)
        val oldUri = customArtworkByKey.remove(hash)
        prefs.edit().remove(customArtworkPreferenceKey(hash)).apply()
        scope.launch(Dispatchers.IO) { deleteOldCustomCover(oldUri) }
        statusMessage = "Đã khôi phục ảnh bìa tự động"
        if (artworkById[song.id].isNullOrBlank() && song.artworkUrl.isNullOrBlank()) {
            ensureArtwork(song)
        }
    }

    private fun deleteOldCustomCover(savedUri: String?) {
        if (savedUri.isNullOrBlank()) return
        runCatching {
            val file = Uri.parse(savedUri).path?.let(::File)?.canonicalFile ?: return
            val folder = File(context.filesDir, "melowave_custom_covers").canonicalFile
            // Never delete content outside the directory controlled by MeloWave.
            if (file.parentFile == folder) file.delete()
        }
    }

    /** Decode on a worker thread so a large gallery photo cannot freeze the player UI. */
    suspend fun loadArtworkForCrop(photoUri: Uri): Bitmap =
        withContext(Dispatchers.IO) { decodeChosenArtwork(photoUri) }

    /** The normalized crop comes from the on-screen square preview, not from the audio file. */
    private fun cropChosenArtwork(bitmap: Bitmap, normalized: RectF): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val cropWidth = ((normalized.right - normalized.left).coerceIn(0f, 1f) * width)
            .roundToInt().coerceAtLeast(1)
        val cropHeight = ((normalized.bottom - normalized.top).coerceIn(0f, 1f) * height)
            .roundToInt().coerceAtLeast(1)
        val side = minOf(cropWidth, cropHeight, width, height).coerceAtLeast(1)
        val left = (normalized.left.coerceIn(0f, 1f) * width).roundToInt()
            .coerceIn(0, width - side)
        val top = (normalized.top.coerceIn(0f, 1f) * height).roundToInt()
            .coerceIn(0, height - side)
        return Bitmap.createBitmap(bitmap, left, top, side, side)
    }

    private fun decodeChosenArtwork(photoUri: Uri): Bitmap {
        val resolver = context.contentResolver
        val maxEdge = 1200
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, photoUri)) {
                    decoder, imageInfo, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val width = imageInfo.size.width
                val height = imageInfo.size.height
                if (width <= 0 || height <= 0) throw IOException("Ảnh không hợp lệ")
                val factor = maxOf(width, height).toFloat() / maxEdge.toFloat()
                if (factor > 1f) {
                    decoder.setTargetSize(
                        maxOf(1, (width / factor).toInt()),
                        maxOf(1, (height / factor).toInt())
                    )
                }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(photoUri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
            Unit
        } ?: throw IOException("Không thể đọc ảnh")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Ảnh không hợp lệ")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(photoUri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Không thể giải mã ảnh")
    }

    fun ensureArtwork(song: SongItem) {
        // P2P files have no persistent URI outside the live Internet session.
        if (song.source == SongSource.INTERNET_P2P) return
        if (!artworkFor(song).isNullOrBlank()) return
        if (!artworkRequests.add(song.id)) return
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    ArtworkResolver.findArtwork(context, song, cachedPlaybackUris[song.id])
                }
            } catch (cancelled: CancellationException) {
                artworkRequests.remove(song.id)
                throw cancelled
            } catch (_: Exception) {
                null
            }
            artworkRequests.remove(song.id)
            if (!result.isNullOrBlank()) {
                artworkById[song.id] = result
                prefs.edit().putString("art_${song.id.sha256().take(32)}", result).apply()
            }
        }
    }

    fun downloadSongForOffline(song: SongItem) {
        if (song.source == SongSource.INTERNET_P2P) {
            val id = song.uri.pathSegments.firstOrNull()
            val audio = internetSession.remoteAudio.firstOrNull { it.id == id }
            if (audio == null || !internetSession.dataChannelOpen) {
                statusMessage = "Máy A đã ngắt mạng. Hãy kết nối lại để tải nhạc."
            } else {
                internetSession.downloadAudio(audio)
                statusMessage = internetSession.transferMessage
            }
            return
        }
        if (song.id.startsWith("meloware_pc_")) {
            if (laptopShare.downloadPlayingUri(song.uri) {
                    statusMessage = "Đã tải bài hát vào Music/meloware_laptop"
                    scanLocalMusic()
                }) {
                statusMessage = "Đang tải nhạc từ máy tính vào Music/meloware_laptop..."
            } else statusMessage = "Không thể tải: hãy kết nối lại Chrome rồi chọn bài từ thư mục máy tính."
            return
        }
        if (song.source == SongSource.MOVIE) {
            statusMessage = "Movie không hỗ trợ tải trực tiếp âm thanh của video bằng chức năng này."
            return
        }
        scope.launch {
            isLoading = true
            val result = runCatching {
                val sourceUri = resolvePlaybackUri(song)
                withContext(Dispatchers.IO) { saveUriToMusicFolder(sourceUri, song) }
            }
            isLoading = false
            result.onSuccess { path ->
                statusMessage = "Đã tải về $path"
                scanLocalMusic()
            }.onFailure { throwable ->
                errorMessage = "Không tải được bài hát: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }

    /** Local file management is intentionally limited to MediaStore entries on this device. */
    fun canManageLocalSong(song: SongItem): Boolean =
        canUseLocalAudioTools(song) && song.uri.scheme == "content" &&
            song.uri.authority == MediaStore.AUTHORITY

    private fun localSongLabelKey(songId: String): String =
        "local_song_label_" + songId.sha256().take(32)

    private fun withSavedLocalLabel(song: SongItem): SongItem {
        val encoded = prefs.getString(localSongLabelKey(song.id), null) ?: return song
        return runCatching {
            val saved = JSONObject(encoded)
            song.copy(
                title = saved.optString("title").ifBlank { song.title },
                artist = saved.optString("artist").ifBlank { song.artist }
            )
        }.getOrDefault(song)
    }

    /**
     * Update the Android media library metadata and maintain a durable in-app label.
     * This does NOT rewrite the embedded ID3/FLAC tags inside the audio file.
     */
    suspend fun updateLocalSongInfo(
        song: SongItem, title: String, artist: String,
        consentGranted: Boolean = false
    ): LocalSongMutationResult {
        if (!canManageLocalSong(song)) {
            return LocalSongMutationResult.Failed("Chỉ sửa thông tin của nhạc lưu trên thiết bị.")
        }
        val newTitle = title.trim()
        val newArtist = artist.trim().ifBlank { "Nghệ sĩ chưa biết" }
        if (newTitle.isBlank() || newTitle.length > 200 || newArtist.length > 200) {
            return LocalSongMutationResult.Failed("Tên bài hát và nghệ sĩ không được vượt quá 200 ký tự; tên bài hát không được trống.")
        }
        val resolver = context.contentResolver
        val attempt = withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, newTitle)
                    put(MediaStore.Audio.Media.ARTIST, newArtist)
                }
                val changed = resolver.update(song.uri, values, null, null)
                if (changed > 0) LocalSongMutationResult.Completed
                else {
                    // Some media providers won't modify TITLE/ARTIST. Keep a durable
                    // in-app label rather than silently failing the user's edit.
                    val stillExists = resolver.query(
                        song.uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null
                    )?.use { it.moveToFirst() } == true
                    if (stillExists) LocalSongMutationResult.LibraryOnly
                    else LocalSongMutationResult.Failed("Không tìm thấy tệp nhạc trên thiết bị.")
                }
            } catch (security: SecurityException) {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            security is android.app.RecoverableSecurityException ->
                        LocalSongMutationResult.NeedsConsent(security.userAction.actionIntent.intentSender)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !consentGranted -> {
                        runCatching<LocalSongMutationResult> {
                            LocalSongMutationResult.NeedsConsent(
                                MediaStore.createWriteRequest(resolver, listOf(song.uri)).intentSender
                            )
                        }.getOrElse {
                            LocalSongMutationResult.Failed("Không xin được quyền sửa bài hát: ${it.localizedMessage ?: "Không xác định"}")
                        }
                    }
                    else -> LocalSongMutationResult.Failed("Android chưa cấp quyền sửa bài hát: ${security.localizedMessage ?: "Bị từ chối"}")
                }
            } catch (failure: Exception) {
                LocalSongMutationResult.Failed("Sửa thông tin thất bại: ${failure.localizedMessage ?: "Lỗi không xác định"}")
            }
        }
        if (attempt is LocalSongMutationResult.Completed ||
            attempt is LocalSongMutationResult.LibraryOnly) {
            prefs.edit().putString(localSongLabelKey(song.id), JSONObject().apply {
                put("title", newTitle)
                put("artist", newArtist)
            }.toString()).apply()
            fun replace(list: MutableList<SongItem>) {
                val idx = list.indexOfFirst { it.id == song.id }
                if (idx >= 0) list[idx] = list[idx].copy(title = newTitle, artist = newArtist)
            }
            replace(localSongs)
            replace(queue)
            replace(recentSongs)
            saveQueue()
            saveSongList(KEY_RECENTS, recentSongs)
            lyricsJobs.remove(song.id)?.cancel()
            lyricsBySongId.remove(song.id)
            lyricsErrorBySongId.remove(song.id)
            statusMessage = if (attempt is LocalSongMutationResult.LibraryOnly)
                "Đã lưu tên bài hát và nghệ sĩ trong MeloWave (Android không cho cập nhật MediaStore)."
            else "Đã cập nhật tên bài hát và nghệ sĩ"
        }
        return attempt
    }

    /**
     * On Android 11+ the system deletes via MediaStore.createDeleteRequest itself.
     * The 'consentGranted' continuation just synchronizes app state after RESULT_OK.
     */
    suspend fun deleteLocalSongFromDevice(
        song: SongItem, consentGranted: Boolean = false
    ): LocalSongMutationResult {
        if (!canManageLocalSong(song)) {
            return LocalSongMutationResult.Failed("Chỉ có thể xóa tệp nhạc trên điện thoại.")
        }
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!consentGranted) {
                return runCatching<LocalSongMutationResult> {
                    LocalSongMutationResult.NeedsConsent(
                        MediaStore.createDeleteRequest(resolver, listOf(song.uri)).intentSender
                    )
                }.getOrElse {
                    LocalSongMutationResult.Failed("Không tạo được yêu cầu xóa: ${it.localizedMessage ?: "Lỗi không xác định"}")
                }
            }
            // Android has confirmed and performed this deletion through the OS dialog.
            onLocalSongDeleted(song)
            return LocalSongMutationResult.Completed
        }
        val result = withContext(Dispatchers.IO) {
            try {
                val count = resolver.delete(song.uri, null, null)
                if (count > 0) LocalSongMutationResult.Completed
                else LocalSongMutationResult.Failed("Không xóa được tệp nhạc. File có thể đã bị di chuyển hoặc xóa trước đó.")
            } catch (security: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    security is android.app.RecoverableSecurityException
                ) LocalSongMutationResult.NeedsConsent(security.userAction.actionIntent.intentSender)
                else LocalSongMutationResult.Failed("Không có quyền xóa tệp nhạc: ${security.localizedMessage ?: "Bị từ chối"}")
            } catch (failure: Exception) {
                LocalSongMutationResult.Failed("Xóa bài hát thất bại: ${failure.localizedMessage ?: "Lỗi không xác định"}")
            }
        }
        if (result is LocalSongMutationResult.Completed) onLocalSongDeleted(song)
        return result
    }

    private fun onLocalSongDeleted(song: SongItem) {
        val removedIndex = queue.indexOfFirst { it.id == song.id }
        val wasCurrent = currentSong?.id == song.id
        if (wasCurrent) {
            player.stop()
            player.clearMediaItems()
            isPlaying = false
            isLoading = false
            positionMs = 0L
            durationMs = 1L
            if (castConnected) {
                castContext?.sessionManager?.currentCastSession?.remoteMediaClient?.stop()
            }
            if (hostedSongId == song.id) stopWifiHostInternal(clearUserRequest = false)
        }
        localSongs.removeAll { it.id == song.id }
        recentSongs.removeAll { it.id == song.id }
        queue.removeAll { it.id == song.id }
        currentIndex = when {
            queue.isEmpty() -> -1
            removedIndex < 0 -> currentIndex.coerceIn(0, queue.lastIndex)
            removedIndex < currentIndex -> (currentIndex - 1).coerceAtLeast(0)
            else -> currentIndex.coerceIn(0, queue.lastIndex)
        }
        if (wasCurrent && currentScreen == MusicScreen.NOW_PLAYING) {
            currentScreen = previousScreen
        }
        favoriteIds.remove(song.id)
        val removedCover = customArtworkByKey.remove(customArtworkKey(song.id))
        scope.launch(Dispatchers.IO) { deleteOldCustomCover(removedCover) }
        prefs.edit()
            .remove(customArtworkPreferenceKey(customArtworkKey(song.id)))
            .remove(localSongLabelKey(song.id))
            .remove("art_${song.id.sha256().take(32)}")
            .putStringSet(KEY_FAVORITES, favoriteIds.keys.toSet())
            .apply()
        cachedPlaybackUris.remove(song.id)
        audioTechnicalInfoById.remove(song.id)
        artworkById.remove(song.id)
        lyricsJobs.remove(song.id)?.cancel()
        lyricsBySongId.remove(song.id)
        lyricsErrorBySongId.remove(song.id)
        saveSongList(KEY_RECENTS, recentSongs)
        saveQueue()
        statusMessage = "Đã xóa bài hát khỏi điện thoại"
    }

    fun canUseLocalAudioTools(song: SongItem): Boolean =
        song.source == SongSource.LOCAL_MEDIASTORE || song.source == SongSource.DOWNLOADED

    fun localAudioToolSongs(excludeId: String? = null): List<SongItem> =
        localSongs
            .filter(::canUseLocalAudioTools)
            .filter { excludeId == null || it.id != excludeId }
            .distinctBy { it.id }

    fun cutLocalSong(song: SongItem, startMs: Long, endMs: Long) {
        if (!canUseLocalAudioTools(song)) {
            statusMessage = "Cắt nhạc chỉ áp dụng cho tệp đã lưu trên điện thoại."
            return
        }
        val safeDuration = song.durationMs.coerceAtLeast(1L)
        val start = startMs.coerceIn(0L, safeDuration - 1L)
        val end = endMs.coerceIn(start + 1L, safeDuration)
        val clippedMedia = MediaItem.Builder()
            .setUri(song.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(start)
                    .setEndPositionMs(end)
                    .build()
            )
            .build()
        val edited = EditedMediaItem.Builder(clippedMedia).build()
        val sequence = EditedMediaItemSequence(edited)
        val composition = Composition.Builder(listOf(sequence)).build()
        val outputTitle = "${song.title} (Cut)"
        val fileName = "${safeFileName(song.title)}_cut_${System.currentTimeMillis()}.m4a"
        startAudioExport(
            composition = composition,
            fileName = fileName,
            title = outputTitle,
            artist = song.artist,
            duration = end - start,
            successMessage = "Đã cắt và lưu bài hát vào Music/MeloWare"
        )
    }

    fun mergeLocalSongs(
        first: SongItem,
        second: SongItem,
        firstStartMs: Long = 0L,
        firstEndMs: Long = first.durationMs,
        secondStartMs: Long = 0L,
        secondEndMs: Long = second.durationMs
    ) {
        if (!canUseLocalAudioTools(first) || !canUseLocalAudioTools(second)) {
            statusMessage = "Ghép nhạc chỉ áp dụng cho các tệp đã lưu trên điện thoại."
            return
        }
        if (first.id == second.id) {
            statusMessage = "Hãy chọn hai bài hát khác nhau để ghép."
            return
        }

        val firstDuration = first.durationMs.coerceAtLeast(1L)
        val secondDuration = second.durationMs.coerceAtLeast(1L)

        val safeFirstStart = firstStartMs.coerceIn(0L, firstDuration - 1L)
        val safeFirstEnd = firstEndMs.coerceIn(safeFirstStart + 1L, firstDuration)
        val safeSecondStart = secondStartMs.coerceIn(0L, secondDuration - 1L)
        val safeSecondEnd = secondEndMs.coerceIn(safeSecondStart + 1L, secondDuration)

        val firstMediaItem = MediaItem.Builder()
            .setUri(first.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeFirstStart)
                    .setEndPositionMs(safeFirstEnd)
                    .build()
            )
            .build()
        val secondMediaItem = MediaItem.Builder()
            .setUri(second.uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeSecondStart)
                    .setEndPositionMs(safeSecondEnd)
                    .build()
            )
            .build()

        val firstItem = EditedMediaItem.Builder(firstMediaItem).build()
        val secondItem = EditedMediaItem.Builder(secondMediaItem).build()
        val sequence = EditedMediaItemSequence(firstItem, secondItem)
        val composition = Composition.Builder(listOf(sequence)).build()
        val mergedTitle = "${first.title} + ${second.title}"
        val mergedArtist = listOf(first.artist, second.artist)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" & ")
            .ifBlank { "MeloWare" }
        val fileName = "${safeFileName(first.title)}_merged_${System.currentTimeMillis()}.m4a"
        startAudioExport(
            composition = composition,
            fileName = fileName,
            title = mergedTitle,
            artist = mergedArtist,
            duration = (safeFirstEnd - safeFirstStart) + (safeSecondEnd - safeSecondStart),
            successMessage = "Đã cắt hai đoạn, ghép và lưu vào Music/MeloWare"
        )
    }

    fun setAsRingtone(song: SongItem, ringtoneType: Int) {
        if (!canUseLocalAudioTools(song)) {
            statusMessage = "Hãy tải bài hát về máy trước khi đặt làm nhạc chuông."
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            statusMessage = "Hãy cấp quyền sửa đổi cài đặt hệ thống, sau đó chọn lại chức năng nhạc chuông."
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return
        }
        runCatching {
            RingtoneManager.setActualDefaultRingtoneUri(context, ringtoneType, song.uri)
        }.onSuccess {
            statusMessage = if (ringtoneType == RingtoneManager.TYPE_NOTIFICATION) {
                "Đã đặt làm âm báo thông báo"
            } else {
                "Đã đặt làm nhạc chuông cuộc gọi"
            }
        }.onFailure { error ->
            errorMessage = "Không thể đặt nhạc chuông: ${error.localizedMessage ?: "Lỗi không xác định"}"
        }
    }

    fun restoreDefaultRingtone(ringtoneType: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(context)) {
            statusMessage = "Hãy cấp quyền sửa đổi cài đặt hệ thống rồi thử lại."
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return
        }
        runCatching {
            val manager = RingtoneManager(context).apply { setType(ringtoneType) }
            val defaultUri = manager.cursor?.use { cursor ->
                if (cursor.moveToFirst()) manager.getRingtoneUri(cursor.position) else null
            }
            RingtoneManager.setActualDefaultRingtoneUri(context, ringtoneType, defaultUri)
        }.onSuccess {
            statusMessage = "Đã khôi phục âm thanh hệ thống mặc định"
        }.onFailure { error ->
            errorMessage = "Không thể khôi phục nhạc chuông: ${error.localizedMessage ?: "Lỗi không xác định"}"
        }
    }

    private fun startAudioExport(
        composition: Composition,
        fileName: String,
        title: String,
        artist: String,
        duration: Long,
        successMessage: String
    ) {
        if (audioToolBusy) {
            statusMessage = "Một tác vụ âm thanh khác đang được xử lý."
            return
        }
        audioToolBusy = true
        errorMessage = null
        val outputFile = File(context.cacheDir, fileName).apply {
            if (exists()) delete()
        }
        lateinit var transformer: Transformer
        transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    activeTransformers.remove(transformer)
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                publishExportedAudio(
                                    sourceFile = outputFile,
                                    fileName = fileName,
                                    title = title,
                                    artist = artist,
                                    duration = duration
                                )
                            }
                        }
                        outputFile.delete()
                        audioToolBusy = false
                        result.onSuccess {
                            statusMessage = successMessage
                            scanLocalMusic()
                        }.onFailure { error ->
                            errorMessage = "Không thể lưu tệp âm thanh: ${error.localizedMessage ?: "Lỗi không xác định"}"
                        }
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    activeTransformers.remove(transformer)
                    outputFile.delete()
                    scope.launch {
                        audioToolBusy = false
                        errorMessage = "Xử lý âm thanh thất bại: ${exportException.localizedMessage ?: "Lỗi Transformer"}"
                    }
                }
            })
            .build()
        activeTransformers.add(transformer)
        runCatching {
            transformer.start(composition, outputFile.absolutePath)
        }.onFailure { error ->
            activeTransformers.remove(transformer)
            outputFile.delete()
            audioToolBusy = false
            errorMessage = "Không thể bắt đầu xử lý âm thanh: ${error.localizedMessage ?: "Lỗi không xác định"}"
        }
    }

    private fun publishExportedAudio(
        sourceFile: File,
        fileName: String,
        title: String,
        artist: String,
        duration: Long
    ): Uri {
        if (!sourceFile.isFile || sourceFile.length() <= 0L) {
            throw IOException("Tệp xuất âm thanh bị rỗng")
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, title)
            put(MediaStore.Audio.Media.ARTIST, artist)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.IS_MUSIC, true)
            if (duration > 0L) put(MediaStore.Audio.Media.DURATION, duration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/MeloWare")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(collection, values)
            ?: throw IOException("Không thể tạo tệp trong MediaStore")
        try {
            sourceFile.inputStream().use { input ->
                resolver.openOutputStream(target)?.use { output ->
                    input.copyTo(output, 128 * 1024)
                } ?: throw IOException("Không mở được tệp đích")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            }
            return target
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        }
    }

    private fun safeFileName(value: String): String = value
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .ifBlank { "MeloWare" }

    fun shareCurrentSong() {
        val song = currentSong ?: return
        val shareText = buildString {
            append(song.title)
            if (song.artist.isNotBlank()) append(" - ${song.artist}")
            if (song.source == SongSource.PUBLIC_DRIVE) append("\nNguồn: Google Drive công khai")
            if (song.source == SongSource.ONLINE_HTTP) append("\nNguồn: Music Preview")
            if (song.source == SongSource.MOVIE) append("\n${song.uri}")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Chia sẻ bài hát").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            sleepTimerMinutesLeft = 0
            statusMessage = "Đã tắt hẹn giờ"
            return
        }
        sleepTimerMinutesLeft = minutes
        statusMessage = "Sẽ dừng nhạc sau $minutes phút"
        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                sleepTimerMinutesLeft = remaining
            }
            player.pause()
            statusMessage = "Hẹn giờ đã dừng phát nhạc"
        }
    }

    fun clearDriveCache() {
        scope.launch(Dispatchers.IO) {
            val directory = File(context.cacheDir, DRIVE_CACHE_DIR)
            val deleted = directory.listFiles()?.count { it.delete() } ?: 0
            cachedPlaybackUris.clear()
            withContext(Dispatchers.Main) {
                statusMessage = "Đã xóa $deleted file cache Google Drive"
            }
        }
    }

    fun sourceLabel(source: SongSource): String = when (source) {
        SongSource.LOCAL_MEDIASTORE -> "Nhạc trên máy"
        SongSource.DOWNLOADED -> "Đã tải Offline"
        SongSource.PERSONAL_DRIVE -> "Google Drive"
        SongSource.PUBLIC_DRIVE -> "Drive chia sẻ"
        SongSource.ONLINE_HTTP -> "Music Preview"
        SongSource.INTERNET_P2P -> "MeloWare Internet"
        SongSource.MOVIE -> "Movie"
        SongSource.APP_RESOURCE -> "Ứng dụng"
    }

    fun formatBadge(song: SongItem): String {
        val ext = extensionForMime(song.mimeType).uppercase(Locale.ROOT)
        return when (song.source) {
            SongSource.PERSONAL_DRIVE -> "DRIVE $ext"
            SongSource.PUBLIC_DRIVE -> "DRIVE PUBLIC $ext"
            SongSource.DOWNLOADED -> "OFFLINE $ext"
            SongSource.LOCAL_MEDIASTORE -> ext.takeIf { it != "AUDIO" } ?: "OFFLINE"
            SongSource.ONLINE_HTTP -> if (song.id.startsWith("meloware_lan_")) "MELOWARE LAN" else "MUSIC"
            SongSource.INTERNET_P2P -> "INTERNET P2P"
            SongSource.MOVIE -> "MOVIE"
            SongSource.APP_RESOURCE -> "DEMO"
        }
    }

    private fun configurePlayerListener(target: ExoPlayer = player) {
        target.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                if (target !== player) return
                // Movie/Cast/P2P have separate playback lifecycles. Do not start
                // an Android FGS that cannot display a supported media session.
                if (value && !castConnected && currentSong?.source !in setOf(
                        SongSource.MOVIE, SongSource.INTERNET_P2P)) {
                    MeloWavePlaybackRuntime.ensureService(context)
                }
                MeloWavePlaybackRuntime.onPlaybackChanged()
                if (currentSong?.source != SongSource.MOVIE) {
                    isPlaying = value
                    if (value && currentSong != null) {
                        smartTrackedLastTickElapsedMs = SystemClock.elapsedRealtime()
                    } else {
                        persistSmartPlaybackProgress()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (target !== player) return
                MeloWavePlaybackRuntime.onPlaybackChanged()
                if (currentSong?.source == SongSource.MOVIE) return
                isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    isLoading = false
                    val readyDuration = player.duration
                    if (readyDuration != C.TIME_UNSET && readyDuration > 0L) {
                        durationMs = readyDuration
                        updateSongDuration(currentSong?.id, readyDuration)
                    }
                    val readySessionId = player.audioSessionId
                    if (readySessionId > 0 && readySessionId != C.AUDIO_SESSION_ID_UNSET) {
                        // Keep the session available even when Equalizer is disabled.
                        // Visual Experience uses the same ExoPlayer session for FFT capture.
                        audioSessionId = readySessionId
                        if (equalizerEnabled) attachAndApplyEqualizer(readySessionId)
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    finishSmartPlaybackTracking(markSkipped = false)
                    when (repeatSetting) {
                        RepeatSetting.ONE -> {
                            currentSong?.let(::beginSmartPlaybackTracking)
                            player.seekTo(0L)
                            player.play()
                        }
                        RepeatSetting.ALL -> next()
                        RepeatSetting.OFF -> if (currentIndex < queue.lastIndex || shuffleEnabled) next()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (target !== player) return
                MeloWavePlaybackRuntime.onPlaybackChanged()
                if (currentSong?.source == SongSource.MOVIE) return
                persistSmartPlaybackProgress()
                isLoading = false
                isPlaying = false
                errorMessage = "Không thể phát bài hát. ${error.errorCodeName}: ${error.cause?.localizedMessage ?: error.localizedMessage}"
            }

            override fun onAudioSessionIdChanged(id: Int) {
                if (target !== player) return
                audioSessionId = if (id > 0 && id != C.AUDIO_SESSION_ID_UNSET) id else 0
                if (equalizerEnabled && audioSessionId > 0) {
                    attachAndApplyEqualizer(audioSessionId, force = true)
                }
            }
        })
    }

    private fun prepareCurrent(autoPlay: Boolean) {
        cancelSoundCrossfade()
        val song = currentSong ?: return
        if (!ensurePremiumPlaybackAccess(song)) {
            runCatching { player.pause() }
            isLoading = false
            isPlaying = false
            return
        }
        beginSmartPlaybackTracking(song)
        // Start the real foreground music service while the app is visible,
        // before resolving Drive/cache/network sources asynchronously.
        if (autoPlay && !castConnected && song.source != SongSource.MOVIE &&
            song.source != SongSource.INTERNET_P2P) {
            MeloWavePlaybackRuntime.ensureService(context)
        }
        MeloWavePlaybackRuntime.onPlaybackChanged()
        playbackJob?.cancel()
        // Stop an old Internet stream before switching to a local/Drive source.
        if (internetWasPlaying && song.source != SongSource.INTERNET_P2P) {
            player.stop()
            if (internetSessionLazy.isInitialized()) internetSession.stopOnline()
            internetWasPlaying = false
        }
        if (song.source == SongSource.INTERNET_P2P) {
            if (castConnected) {
                errorMessage = "Hãy ngắt Google Cast trước khi phát nhạc WebRTC Internet."
                return
            }
            if (!internetSession.dataChannelOpen) {
                isLoading = false
                errorMessage = "Máy A không còn kết nối Internet."
                return
            }
            internetWasPlaying = true
            isLoading = true
            isPlaying = false
            errorMessage = null
            player.stop()
            player.clearMediaItems()
            val remoteId = song.uri.pathSegments.firstOrNull().orEmpty()
            internetSession.playInLibrary(
                remoteId,
                song.toMediaItem(song.uri, artworkFor(song)),
                onReady = { source ->
                    if (currentSong?.id == song.id && internetSession.dataChannelOpen) {
                        player.setMediaSource(source)
                        player.prepare()
                        if (autoPlay) player.play()
                    }
                },
                onError = { reason ->
                    if (currentSong?.id == song.id) {
                        isLoading = false
                        isPlaying = false
                        errorMessage = reason
                    }
                }
            )
            addRecent(song)
            saveQueue()
            return
        }
        // Keep the HTTP port/token stable while the user is sharing, so the
        // receiving MeloWare can follow the next song without rediscovering A.
        if (hostedSongId != null && hostedSongId != song.id && !wifiHostRequestedByUser) {
            stopWifiHostInternal(clearUserRequest = false)
        }
        if (castConnected && song.source != SongSource.MOVIE) {
            audioFxEngine.release()
            equalizerReady = false
            equalizerHasControl = false
            player.pause()
            player.clearMediaItems()
            positionMs = 0L
            durationMs = song.durationMs.coerceAtLeast(1L)
            addRecent(song)
            ensureArtwork(song)
            saveQueue()
            castCurrentSong()
            return
        }
        if (song.source == SongSource.MOVIE) {
            audioFxEngine.release()
            equalizerReady = false
            equalizerHasControl = false
            player.pause()
            player.clearMediaItems()
            positionMs = 0L
            durationMs = song.durationMs.coerceAtLeast(1L)
            errorMessage = null
            isLoading = true
            // Chờ IFrame Player báo trạng thái thật; không giả định autoplay luôn thành công.
            isPlaying = false
            addRecent(song)
            ensureArtwork(song)
            saveQueue()
            return
        }
        playbackJob = scope.launch {
            isLoading = true
            errorMessage = null
            val resolvedUri = try {
                resolvePlaybackUri(song)
            } catch (throwable: Throwable) {
                isLoading = false
                if (throwable is PersonalDriveAuthorizationException) {
                    personalDriveInvalidToken = personalDriveAccessToken
                    personalDriveAccessToken = null
                    personalDriveNeedsAuthorization = true
                    pendingPersonalDrivePlaybackId = song.id
                }
                errorMessage = when (throwable) {
                    is PersonalDriveAuthorizationException -> throwable.localizedMessage
                        ?: "Phiên Google Drive đã hết hạn. Hãy cấp lại quyền."
                    is SecurityException -> "Ứng dụng đã mất quyền đọc Google Drive. Hãy cấp lại quyền."
                    is FileNotFoundException -> "Không tìm thấy file nhạc hoặc tài khoản hiện tại không có quyền."
                    else -> "Không chuẩn bị được bài hát: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
                }
                return@launch
            }
            if (currentSong?.id != song.id) return@launch
            player.stop()
            player.setMediaItem(song.toMediaItem(resolvedUri, artworkFor(song)))
            reapplyOutputVolumes()
            player.prepare()
            if (autoPlay) player.play()
            addRecent(song)
            ensureArtwork(song)
            ensureAudioTechnicalInfo(song)
            saveQueue()
            if (wifiHostRequestedByUser) startWifiHostForCurrentSong(showMessage = false)
        }
    }

    /**
     * Read-only source for the waveform analyzer. Never downloads a Drive file:
     * the playback pipeline has already prepared it in cachedPlaybackUris.
     * P2P returns a file only after the existing transfer passed SHA-256 check.
     */
    internal fun waveformSourceFor(song: SongItem): Uri? = when (song.source) {
        SongSource.PERSONAL_DRIVE, SongSource.PUBLIC_DRIVE ->
            cachedPlaybackUris[song.id]?.takeIf { cached ->
                val file = cached.path?.let(::File)
                file?.isFile == true && file.length() > 0L
            }
        SongSource.INTERNET_P2P ->
            if (internetSessionLazy.isInitialized()) {
                internetSession.verifiedWaveformUri(song.uri.pathSegments.firstOrNull().orEmpty())
            } else null
        SongSource.MOVIE -> null
        else -> song.uri.takeIf { source ->
            source.scheme?.lowercase(Locale.ROOT) in
                listOf("content", "file", "android.resource", "http", "https")
        }
    }

    private suspend fun resolvePlaybackUri(song: SongItem): Uri {
        if (song.source != SongSource.PERSONAL_DRIVE && song.source != SongSource.PUBLIC_DRIVE) {
            return song.uri
        }
        cachedPlaybackUris[song.id]?.let { cached ->
            val file = cached.path?.let(::File)
            if (file?.isFile == true && file.length() > 0L) return cached
        }
        val cached = withContext(Dispatchers.IO) {
            DriveAudioCache.getOrCreate(
                context = context,
                song = song,
                personalDriveAccessToken = personalDriveAccessToken
            )
        }
        cachedPlaybackUris[song.id] = cached
        return cached
    }

    private fun provisionalAudioTechnicalInfo(song: SongItem): AudioTechnicalInfo {
        val extension = resolvedExtension(song).uppercase(Locale.ROOT)
        val quality = when (extension.lowercase(Locale.ROOT)) {
            "flac", "wav", "wave", "alac", "ape" -> "Lossless"
            "m4a", "aac" -> "AAC"
            "mp3" -> "MP3"
            else -> "Chất lượng gốc"
        }
        val size = if (song.source == SongSource.ONLINE_HTTP ||
            song.source == SongSource.INTERNET_P2P || song.source == SongSource.MOVIE) {
            "Trực tuyến"
        } else {
            "Đang đọc..."
        }
        return AudioTechnicalInfo(quality, extension.ifBlank { "AUDIO" }, size)
    }

    private fun readAudioTechnicalInfo(song: SongItem): AudioTechnicalInfo {
        if (song.source == SongSource.MOVIE) {
            return AudioTechnicalInfo("Video online", "STREAM", "Trực tuyến")
        }
        if (song.source == SongSource.INTERNET_P2P) {
            return provisionalAudioTechnicalInfo(song)
        }
        val uri = cachedPlaybackUris[song.id] ?: song.uri
        val extension = resolvedExtension(song).uppercase(Locale.ROOT).ifBlank { "AUDIO" }
        val sizeBytes = readUriSize(uri)
        var bitrateKbps: Int? = null
        var sampleRateHz: Int? = null
        val retriever = MediaMetadataRetriever()
        try {
            when (uri.scheme?.lowercase(Locale.ROOT)) {
                "file" -> uri.path?.let(retriever::setDataSource)
                "http", "https" -> retriever.setDataSource(uri.toString(), emptyMap())
                else -> retriever.setDataSource(context, uri)
            }
            bitrateKbps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toLongOrNull()?.takeIf { it > 0L }?.div(1000L)?.toInt()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                sampleRateHz = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()?.takeIf { it > 0 }
            }
        } catch (_: Exception) {
            // Metadata kỹ thuật có thể không tồn tại ở một số codec/provider.
        } finally {
            runCatching { retriever.release() }
        }
        val lossless = extension.lowercase(Locale.ROOT) in setOf("flac", "wav", "wave", "alac", "ape")
        val quality = when {
            lossless && (sampleRateHz ?: 0) >= 96_000 -> "Hi-Res • ${(sampleRateHz ?: 0) / 1000} kHz"
            lossless -> "Lossless"
            bitrateKbps != null -> "$bitrateKbps kbps"
            sampleRateHz != null -> "${sampleRateHz!! / 1000} kHz"
            else -> "Chất lượng gốc"
        }
        return AudioTechnicalInfo(
            qualityLabel = quality,
            extensionLabel = extension,
            sizeLabel = sizeBytes?.let(::formatFileSize) ?:
            if (song.source == SongSource.ONLINE_HTTP || song.source == SongSource.INTERNET_P2P)
                "Trực tuyến" else "Không rõ dung lượng",
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            sizeBytes = sizeBytes
        )
    }

    private fun resolvedExtension(song: SongItem): String {
        val fromPath = extensionOf(song.uri.lastPathSegment.orEmpty())
        return fromPath.takeIf { it.isNotBlank() && it != "audio" }
            ?: extensionForMime(song.mimeType).takeIf { it != "audio" }
            ?: "audio"
    }

    private fun readUriSize(uri: Uri): Long? {
        return when (uri.scheme?.lowercase(Locale.ROOT)) {
            "file" -> uri.path?.let(::File)?.takeIf(File::isFile)?.length()
            "content" -> runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
                    }
            }.getOrNull()
            else -> null
        }?.takeIf { it > 0L }
    }

    private fun formatFileSize(bytes: Long): String {
        val value = bytes.coerceAtLeast(0L).toDouble()
        return when {
            value >= 1024.0 * 1024.0 * 1024.0 -> String.format(Locale.US, "%.2f GB", value / (1024.0 * 1024.0 * 1024.0))
            value >= 1024.0 * 1024.0 -> String.format(Locale.US, "%.1f MB", value / (1024.0 * 1024.0))
            value >= 1024.0 -> String.format(Locale.US, "%.0f KB", value / 1024.0)
            else -> "${bytes} B"
        }
    }

    private fun startWifiHostForCurrentSong(showMessage: Boolean) {
        val song = currentSong ?: return
        if (song.source == SongSource.MOVIE) {
            wifiHostError = "Movie không hỗ trợ máy chủ âm thanh Wi-Fi."
            return
        }
        if (wifiHostBusy) return
        wifiHostBusy = true
        wifiHostError = null
        scope.launch {
            val result = runCatching { ensureWifiHostUrl(song) }
            wifiHostBusy = false
            result.onSuccess { url ->
                if (showMessage) statusMessage = "Máy chủ Wi-Fi đã sẵn sàng: $url"
            }.onFailure { error ->
                wifiHostError = error.localizedMessage ?: "Không tạo được máy chủ Wi-Fi"
                wifiHostActive = false
                wifiHostUrl = null
                if (wifiAppShareRequested) {
                    wifiAppShareRequested = false
                    wifiAppShareEnabled = false
                    wifiHostRequestedByUser = false
                    wifiLanDiscovery.stopAdvertising()
                }
                if (showMessage) errorMessage = wifiHostError
            }
        }
    }

    private suspend fun ensureWifiHostUrl(song: SongItem): String {
        if (wifiHostActive && hostedSongId == song.id && !wifiHostUrl.isNullOrBlank()) {
            return wifiHostUrl!!
        }
        val resolvedUri = resolvePlaybackUri(song)
        val sourceFile = withContext(Dispatchers.IO) { materializeForWifiHost(song, resolvedUri) }
        val ip = withContext(Dispatchers.IO) { findLanIpv4Address() }
            ?: throw IOException("Không tìm thấy địa chỉ Wi-Fi nội bộ. Hãy kết nối cùng một mạng Wi-Fi.")
        val mime = song.mimeType ?: mimeForExtension(resolvedExtension(song))
        val key = song.id.sha256().take(20)
        val existing = localAudioServer?.takeIf { wifiHostActive }
        val server = if (existing != null) {
            existing.updateSong(sourceFile, mime, song.title, song.artist, song.album, song.durationMs, key)
            existing
        } else {
            stopWifiHostInternal(clearUserRequest = false)
            LocalAudioHttpServer(
                contentResolver = context.contentResolver,
                file = sourceFile,
                contentType = mime,
                displayName = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                songKey = key
            ).also {
                it.start()
                localAudioServer = it
            }
        }
        hostedSongId = song.id
        wifiHostActive = true
        wifiHostError = null
        wifiHostUrl = "http://$ip:${server.port}/${server.token}/${sourceFile.name}"
        refreshMeloWareSharedFolders()
        if (wifiAppShareRequested) wifiLanDiscovery.advertise(server.port, server.token)
        return wifiHostUrl!!
    }

    private fun materializeForWifiHost(song: SongItem, uri: Uri): File {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val direct = uri.path?.let(::File)
            if (direct?.isFile == true && direct.length() > 0L) return direct
        }
        val directory = File(context.cacheDir, "meloware_wifi_host").apply { mkdirs() }
        val extension = resolvedExtension(song).ifBlank { "audio" }
        val target = File(directory, "${song.id.sha256().take(24)}.$extension")
        if (target.isFile && target.length() > 0L) return target
        val input = when (uri.scheme?.lowercase(Locale.ROOT)) {
            "http", "https" -> {
                val connection = URL(uri.toString()).openConnection() as HttpURLConnection
                connection.connectTimeout = 20_000
                connection.readTimeout = 60_000
                connection.instanceFollowRedirects = true
                if (connection.responseCode !in 200..299) {
                    val code = connection.responseCode
                    connection.disconnect()
                    throw IOException("Không tải được bài hát trực tuyến (HTTP $code)")
                }
                connection.inputStream
            }
            else -> context.contentResolver.openInputStream(uri)
                ?: throw FileNotFoundException("Không mở được dữ liệu của ${song.title}")
        }
        input.use { source -> FileOutputStream(target).use { out -> source.copyTo(out, 128 * 1024) } }
        if (target.length() <= 0L) {
            target.delete()
            throw IOException("File chuẩn bị cho Wi-Fi bị rỗng")
        }
        return target
    }

    private fun findLanIpv4Address(): String? {
        val candidates = Collections.list(NetworkInterface.getNetworkInterfaces())
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { network ->
                Collections.list(network.inetAddresses)
                    .filterIsInstance<Inet4Address>()
                    .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
                    .map { address -> network.name.orEmpty() to address }
            }
        return candidates
            .sortedByDescending { (name, _) ->
                val normalized = name.lowercase(Locale.ROOT)
                normalized.contains("wlan") || normalized.contains("wifi") || normalized.startsWith("ap")
            }
            .firstOrNull()
            ?.second
            ?.hostAddress
    }

    private fun stopWifiHostInternal(clearUserRequest: Boolean) {
        wifiLanDiscovery.stopAdvertising()
        localAudioServer?.stop()
        localAudioServer = null
        hostedSongId = null
        wifiHostActive = false
        wifiHostBusy = false
        wifiHostUrl = null
        wifiHostError = null
        if (clearUserRequest) {
            wifiHostRequestedByUser = false
            wifiAppShareRequested = false
            wifiAppShareEnabled = false
            // Nothing from the previous share session is shared on the next start.
            wifiSharedFolderPaths.clear()
        }
    }

    private fun onCastSessionReady(session: CastSession) {
        castConnecting = false
        castConnected = true
        castDeviceName = session.castDevice?.friendlyName
        session.remoteMediaClient?.let(::bindRemoteMediaClient)
        castCurrentSong()
    }

    private fun onCastSessionClosed() {
        val remotePosition = positionMs
        val resumeLocally = isPlaying
        unbindRemoteMediaClient()
        castConnecting = false
        castConnected = false
        castDeviceName = null
        if (!wifiHostRequestedByUser) stopWifiHostInternal(clearUserRequest = false)
        currentSong?.takeIf { it.source != SongSource.MOVIE }?.let {
            prepareCurrent(autoPlay = false)
            scope.launch {
                delay(350L)
                player.seekTo(remotePosition.coerceAtLeast(0L))
                if (resumeLocally) player.play()
            }
        }
    }

    private fun bindRemoteMediaClient(remote: RemoteMediaClient) {
        unbindRemoteMediaClient()
        remoteMediaCallback = object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                isPlaying = remote.isPlaying
                isLoading = remote.isBuffering
                val remoteDuration = remote.streamDuration
                if (remoteDuration > 0L) durationMs = remoteDuration
            }
        }.also(remote::registerCallback)
        remoteProgressListener = RemoteMediaClient.ProgressListener { progress, duration ->
            positionMs = progress.coerceAtLeast(0L)
            if (duration > 0L) durationMs = duration
        }.also { remote.addProgressListener(it, 500L) }
    }

    private fun unbindRemoteMediaClient() {
        val remote = castContext?.sessionManager?.currentCastSession?.remoteMediaClient
        remoteMediaCallback?.let { callback -> runCatching { remote?.unregisterCallback(callback) } }
        remoteProgressListener?.let { listener -> runCatching { remote?.removeProgressListener(listener) } }
        remoteMediaCallback = null
        remoteProgressListener = null
    }

    private fun unregisterCastListeners() {
        unbindRemoteMediaClient()
        val shared = castContext ?: return
        castSessionListener?.let { listener ->
            runCatching { shared.sessionManager.removeSessionManagerListener(listener, CastSession::class.java) }
        }
        castStateListener?.let { listener -> runCatching { shared.removeCastStateListener(listener) } }
        castSessionListener = null
        castStateListener = null
        castContext = null
    }

    private fun mimeForExtension(extension: String): String = when (extension.lowercase(Locale.ROOT)) {
        "mp3" -> "audio/mpeg"
        "m4a", "mp4" -> "audio/mp4"
        "aac" -> "audio/aac"
        "flac" -> "audio/flac"
        "wav", "wave" -> "audio/wav"
        "ogg", "oga" -> "audio/ogg"
        "opus" -> "audio/opus"
        "webm" -> "audio/webm"
        else -> "application/octet-stream"
    }

    private fun beginSmartPlaybackTracking(song: SongItem) {
        if (song.source == SongSource.MOVIE) {
            finishSmartPlaybackTracking(markSkipped = false)
            return
        }
        if (smartTrackedSongId == song.id) {
            smartTrackedLastTickElapsedMs = SystemClock.elapsedRealtime()
            return
        }
        val previousWasSkipped = smartTrackedSongId != null &&
                !smartTrackedPlayCounted &&
                smartTrackedListenMs >= 1_000L &&
                smartTrackedListenMs < smartPlaybackThresholdMs()
        finishSmartPlaybackTracking(markSkipped = previousWasSkipped)
        smartTrackedSongId = song.id
        smartTrackedListenMs = 0L
        smartTrackedPersistedMs = 0L
        smartTrackedPlayCounted = false
        smartTrackedLastTickElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun updateSmartPlaybackTracking() {
        val trackedId = smartTrackedSongId ?: return
        val song = currentSong
        if (song == null || song.id != trackedId || song.source == SongSource.MOVIE) return

        val now = SystemClock.elapsedRealtime()
        if (smartTrackedLastTickElapsedMs <= 0L) {
            smartTrackedLastTickElapsedMs = now
            return
        }
        val delta = (now - smartTrackedLastTickElapsedMs).coerceIn(0L, 2_000L)
        smartTrackedLastTickElapsedMs = now
        if (!isPlaying || delta <= 0L) return

        smartTrackedListenMs += delta
        val threshold = smartPlaybackThresholdMs()
        val shouldCountPlay = !smartTrackedPlayCounted && smartTrackedListenMs >= threshold
        val shouldFlushProgress = smartTrackedListenMs - smartTrackedPersistedMs >= 15_000L
        if (shouldCountPlay || shouldFlushProgress) {
            val unpersisted = (smartTrackedListenMs - smartTrackedPersistedMs).coerceAtLeast(0L)
            smartPlaylists.recordListening(
                songId = trackedId,
                listenDeltaMs = unpersisted,
                incrementPlayCount = shouldCountPlay,
                incrementSkipCount = false
            )
            smartTrackedPersistedMs = smartTrackedListenMs
            if (shouldCountPlay) smartTrackedPlayCounted = true
        }
    }

    private fun persistSmartPlaybackProgress(incrementSkip: Boolean = false) {
        val trackedId = smartTrackedSongId ?: return
        val threshold = smartPlaybackThresholdMs()
        val shouldCountPlay = !smartTrackedPlayCounted && smartTrackedListenMs >= threshold
        val unpersisted = (smartTrackedListenMs - smartTrackedPersistedMs).coerceAtLeast(0L)
        if (unpersisted > 0L || shouldCountPlay || incrementSkip) {
            smartPlaylists.recordListening(
                songId = trackedId,
                listenDeltaMs = unpersisted,
                incrementPlayCount = shouldCountPlay,
                incrementSkipCount = incrementSkip
            )
            smartTrackedPersistedMs = smartTrackedListenMs
            if (shouldCountPlay) smartTrackedPlayCounted = true
        }
        smartTrackedLastTickElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun finishSmartPlaybackTracking(markSkipped: Boolean) {
        persistSmartPlaybackProgress(incrementSkip = markSkipped)
        smartTrackedSongId = null
        smartTrackedListenMs = 0L
        smartTrackedPersistedMs = 0L
        smartTrackedPlayCounted = false
        smartTrackedLastTickElapsedMs = 0L
    }

    private fun smartPlaybackThresholdMs(): Long {
        val trackedId = smartTrackedSongId
        val knownDuration = when {
            trackedId != null && currentSong?.id == trackedId && durationMs > 1_000L -> durationMs
            trackedId != null -> allLibrarySongs().firstOrNull { it.id == trackedId }?.durationMs ?: 0L
            else -> 0L
        }
        return if (knownDuration > 0L) {
            minOf(30_000L, maxOf(5_000L, knownDuration / 2L))
        } else {
            30_000L
        }
    }

    private fun addRecent(song: SongItem) {
        // Chrome laptop streams use an ephemeral localhost URL; do not keep stale shortcuts after disconnect.
        if (song.id.startsWith("meloware_pc_")) return
        recentSongs.removeAll { it.id == song.id }
        recentSongs.add(0, song)
        while (recentSongs.size > 30) recentSongs.removeAt(recentSongs.lastIndex)
        saveSongList(KEY_RECENTS, recentSongs)
    }

    private fun randomOtherIndex(current: Int): Int {
        if (queue.size <= 1) return current.coerceAtLeast(0)
        var next = current
        while (next == current) next = queue.indices.random()
        return next
    }

    private fun updateSongDuration(songId: String?, duration: Long) {
        if (songId == null) return
        fun update(list: MutableList<SongItem>) {
            val index = list.indexOfFirst { it.id == songId }
            if (index >= 0 && list[index].durationMs <= 0L) {
                list[index] = list[index].copy(durationMs = duration)
            }
        }
        update(queue)
        update(localSongs)
        update(personalDriveSongs)
        update(publicDriveSongs)
        update(recentSongs)
        saveQueue()
    }

    private fun queryLocalAudio(): List<SongItem> {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(MediaStore.MediaColumns.GENRE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Audio.Media.RELATIVE_PATH)
            else {
                @Suppress("DEPRECATION")
                add(MediaStore.Audio.Media.DATA)
            }
        }.toTypedArray()
        val output = mutableListOf<SongItem>()
        resolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val displayIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val genreIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cursor.getColumnIndex(MediaStore.MediaColumns.GENRE)
            } else -1
            val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                @Suppress("DEPRECATION")
                cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            }
            while (cursor.moveToNext()) {
                val displayName = cursor.stringOrEmpty(displayIndex)
                val mime = cursor.stringOrNull(mimeIndex)
                if (!isSupportedAudio(displayName, mime)) continue
                val id = cursor.getLong(idIndex)
                val uri = ContentUris.withAppendedId(collection, id)
                val rawTitle = cursor.stringOrEmpty(titleIndex).ifBlank { displayName }
                val rawArtist = cursor.stringOrEmpty(artistIndex)
                    .takeUnless { it.equals("<unknown>", true) }
                    .orEmpty()
                val parsed = parseTitleArtist(rawTitle, rawArtist.ifBlank { "Nghệ sĩ chưa biết" })
                val relativeOrPath = cursor.stringOrEmpty(pathIndex)
                val folder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    relativeOrPath.trim('/').ifBlank { "Nhạc trên máy" }
                } else {
                    File(relativeOrPath).parentFile?.name ?: "Nhạc trên máy"
                }
                val downloaded = (folder.contains("Tunertools", ignoreCase = true) || folder.contains("MeloWare", ignoreCase = true))
                output += withSavedLocalLabel(SongItem(
                    id = uri.toString(),
                    title = parsed.first,
                    artist = parsed.second,
                    album = cursor.stringOrEmpty(albumIndex),
                    folderName = folder,
                    uri = uri,
                    durationMs = cursor.longOrZero(durationIndex),
                    source = if (downloaded) SongSource.DOWNLOADED else SongSource.LOCAL_MEDIASTORE,
                    mimeType = mime,
                    genre = cursor.stringOrEmpty(genreIndex)
                        .takeUnless { it.equals("<unknown>", ignoreCase = true) }
                        .orEmpty(),
                    dateAddedMs = cursor.longOrZero(dateAddedIndex)
                        .takeIf { it > 0L }
                        ?.times(1_000L)
                        ?: 0L
                ))
            }
        }
        return output.distinctBy { it.id }
    }

    private fun saveUriToMusicFolder(sourceUri: Uri, song: SongItem): String {
        val safeTitle = song.title.replace(Regex("[\\/:*?\"<>|]"), "_").trim().ifBlank { "MeloWare" }
        val safeArtist = song.artist.replace(Regex("[\\/:*?\"<>|]"), "_").trim()
        val extension = extensionForMime(song.mimeType).takeUnless { it == "audio" } ?: "mp3"
        val fileName = if (safeArtist.isBlank()) "$safeTitle.$extension" else "$safeTitle - $safeArtist.$extension"
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.TITLE, song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artist)
            put(MediaStore.Audio.Media.MIME_TYPE, song.mimeType ?: "audio/mpeg")
            put(MediaStore.Audio.Media.IS_MUSIC, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/MeloWare")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(collection, values)
            ?: throw IOException("Không thể tạo file trong thư mục Music")
        var networkConnection: HttpURLConnection? = null
        try {
            val input = when (sourceUri.scheme?.lowercase(Locale.ROOT)) {
                "http", "https" -> {
                    val connection = URL(sourceUri.toString()).openConnection() as HttpURLConnection
                    networkConnection = connection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 20_000
                    connection.readTimeout = 60_000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("Accept", "audio/*,*/*;q=0.8")
                    connection.setRequestProperty("User-Agent", "MeloWave-Android/1.0")
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("Không tải được bài hát trực tuyến (HTTP $responseCode)")
                    }
                    val responseType = connection.contentType
                        ?.substringBefore(';')
                        ?.trim()
                        ?.lowercase(Locale.ROOT)
                        .orEmpty()
                    if (responseType.contains("text/html") ||
                        responseType.contains("application/json")) {
                        throw IOException("Máy chủ không trả về dữ liệu âm thanh")
                    }
                    connection.inputStream
                }
                else -> resolver.openInputStream(sourceUri)
                    ?: throw IOException("Không đọc được dữ liệu bài hát")
            }
            val copiedBytes = input.use { source ->
                resolver.openOutputStream(target)?.use { output ->
                    source.copyTo(output, 128 * 1024)
                } ?: throw IOException("Không mở được file đích")
            }
            if (copiedBytes <= 0L) {
                throw IOException("Dữ liệu bài hát tải về bị rỗng")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            }
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        } finally {
            networkConnection?.disconnect()
        }
        return "Music/MeloWare/$fileName"
    }

    private fun restoreState() {
        // Load persisted cover overrides once, BEFORE the restored queue is displayed.
        val customFolder = File(context.filesDir, "melowave_custom_covers").canonicalFile
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith("melowave_custom_cover_")) return@forEach
            val savedUri = value as? String
            val file = runCatching { savedUri?.let { Uri.parse(it).path }?.let(::File)?.canonicalFile }
                .getOrNull()
            if (savedUri != null && file != null && file.isFile && file.parentFile == customFolder) {
                customArtworkByKey[key.removePrefix("melowave_custom_cover_")] = savedUri
            } else {
                prefs.edit().remove(key).apply()
            }
        }
        favoriteIds.putAll(
            prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().associateWith { true }
        )
        queue.addAll(loadSongList(KEY_QUEUE))
        recentSongs.addAll(loadSongList(KEY_RECENTS))
        publicDriveSongs.addAll(loadSongList(KEY_PUBLIC_SONGS))
        personalDriveSongs.addAll(loadSongList(KEY_PERSONAL_DRIVE_SONGS))
        currentIndex = prefs.getInt(KEY_CURRENT_INDEX, if (queue.isEmpty()) -1 else 0)
            .coerceIn(if (queue.isEmpty()) -1 else 0, max(queue.lastIndex, 0))
        val restoredPublic = queue.filter { it.source == SongSource.PUBLIC_DRIVE }
        if (restoredPublic.isNotEmpty()) {
            val merged = (publicDriveSongs + restoredPublic).distinctBy { it.id }
            publicDriveSongs.clear()
            publicDriveSongs.addAll(merged)
        }
        val allRestored = (queue + recentSongs + publicDriveSongs + personalDriveSongs).distinctBy { it.id }
        allRestored.forEach { song ->
            val saved = prefs.getString("art_${song.id.sha256().take(32)}", null)
            if (!saved.isNullOrBlank()) artworkById[song.id] = saved
        }
    }

    private fun saveQueue() {
        saveSongList(KEY_QUEUE, queue)
        prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
    }

    private fun saveSongList(key: String, songs: List<SongItem>) {
        val array = JSONArray()
        songs.filterNot { it.id.startsWith("meloware_pc_") ||
                it.source == SongSource.INTERNET_P2P }.forEach { song ->
            array.put(JSONObject().apply {
                put("id", song.id)
                put("title", song.title)
                put("artist", song.artist)
                put("album", song.album)
                put("folderName", song.folderName)
                put("uri", song.uri.toString())
                put("durationMs", song.durationMs)
                put("source", song.source.name)
                put("mimeType", song.mimeType.orEmpty())
                // Do not persist app-private custom cover as the original online cover.
                // Otherwise "restore automatic" could fall back to a deleted private file.
                put("artworkUrl", (artworkById[song.id] ?: song.artworkUrl).orEmpty())
                put("genre", song.genre)
                put("dateAddedMs", song.dateAddedMs)
            })
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun loadSongList(key: String): List<SongItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val rawSource = item.optString("source")
                    val source = when (rawSource) {
                        "SAF_CLOUD", "PERSONAL_DRIVE" -> SongSource.PERSONAL_DRIVE
                        "PUBLIC_DRIVE" -> SongSource.PUBLIC_DRIVE
                        "DOWNLOADED" -> SongSource.DOWNLOADED
                        "ONLINE_HTTP" -> SongSource.ONLINE_HTTP
                        "YOUTUBE", "MOVIE" -> SongSource.MOVIE
                        "APP_RESOURCE" -> SongSource.APP_RESOURCE
                        else -> SongSource.LOCAL_MEDIASTORE
                    }
                    val duration = item.optLong("durationMs", 0L).takeIf { it > 0L }
                        ?: parseDurationLabel(item.optString("durationLabel"))
                    val song = SongItem(
                        id = item.optString("id"),
                        title = item.optString("title", "Bài hát không tên"),
                        artist = item.optString("artist", "Nghệ sĩ chưa biết").let {
                            if (source == SongSource.MOVIE && it.equals("YouTube", ignoreCase = true)) "Movie" else it
                        },
                        album = item.optString("album").let {
                            if (source == SongSource.MOVIE && it.equals("YouTube", ignoreCase = true)) "Movie" else it
                        },
                        folderName = item.optString("folderName").let {
                            if (source == SongSource.MOVIE && it.equals("YouTube", ignoreCase = true)) "Movie" else it
                        },
                        uri = Uri.parse(item.optString("uri")),
                        durationMs = duration,
                        source = source,
                        mimeType = item.optString("mimeType").takeIf { it.isNotBlank() },
                        artworkUrl = item.optString("artworkUrl").takeIf { it.isNotBlank() },
                        genre = item.optString("genre"),
                        dateAddedMs = item.optLong("dateAddedMs", 0L)
                    )
                    if (song.id.isNotBlank()) add(song)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun SongItem.toMediaItem(uriOverride: Uri, artwork: String?): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(uriOverride)
            .apply { mimeType?.let(::setMimeType) }
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artwork?.let(Uri::parse))
                    .build()
            )
            .build()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private const val PREFS_NAME = "music_app_prefs"
        private const val KEY_QUEUE = "saved_playlist"
        private const val KEY_RECENTS = "recents"
        private const val KEY_PUBLIC_SONGS = "public_songs"
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PERSONAL_DRIVE_ACCOUNT = "google_drive_oauth_account"
        private const val KEY_PERSONAL_DRIVE_SONGS = "personal_drive_picker_songs"
        private const val KEY_PUBLIC_LINK = "public_drive_last_link"
        private const val KEY_EQ_BASS = "equalizer_bass"
        private const val KEY_EQ_MID = "equalizer_mid"
        private const val KEY_EQ_TREBLE = "equalizer_treble"
        private const val KEY_EQ_BAND_60 = "equalizer_band_60"
        private const val KEY_EQ_BAND_230 = "equalizer_band_230"
        private const val KEY_EQ_BAND_910 = "equalizer_band_910"
        private const val KEY_EQ_BAND_3600 = "equalizer_band_3600"
        private const val KEY_EQ_BAND_14000 = "equalizer_band_14000"
        private const val KEY_EQ_CURVE_10 = "equalizer_curve_10"
        private const val KEY_EQ_PRESET = "equalizer_preset"
        private const val KEY_EQ_ADVANCED = "equalizer_advanced"
        private const val KEY_EQ_LOUDNESS = "equalizer_loudness"
        private const val KEY_EQ_LOUDNESS_GAIN_DB = "equalizer_loudness_gain_db"
        private const val KEY_EQ_BASS_BOOST = "equalizer_bass_boost_strength"
        private const val KEY_EQ_VIRTUALIZER = "equalizer_virtualizer_strength"
        private const val KEY_EQ_AUTO_HEADROOM = "equalizer_auto_headroom"
        private const val KEY_EQ_TUNING_VERSION = "equalizer_tuning_version"
        private const val KEY_EQ_ENABLED = "equalizer_enabled"
        private const val KEY_SOUND_MASTER_VOLUME = "soundstudio_master_volume_percent"
        private const val KEY_SOUND_AUTO_EQ = "soundstudio_auto_device_eq"
        private const val KEY_SOUND_CROSSFADE = "soundstudio_crossfade_seconds"
        private const val KEY_SOUND_BASE_PROFILE = "soundstudio_default_device_eq"
        private const val DRIVE_CACHE_DIR = "tunertools_drive_cache"
    }
}


private data class AudioFxAttachResult(
    val ready: Boolean,
    val hasControl: Boolean,
    val bassBoostAvailable: Boolean = false,
    val bassBoostStrengthSupported: Boolean = false,
    val virtualizerAvailable: Boolean = false,
    val virtualizerStrengthSupported: Boolean = false,
    val mappedFiveBandCentersHz: IntArray = intArrayOf(),
    val error: String? = null
)

@Suppress("DEPRECATION")
private class PersistentAudioFxEngine {
    private var equalizer: AndroidEqualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var sessionId: Int = 0
    private var centerFrequenciesHz: IntArray = intArrayOf()
    private var bassStrengthSupported: Boolean = false
    private var virtualizerStrengthSupported: Boolean = false
    private val animationHandler = Handler(Looper.getMainLooper())
    private var animationGeneration = 0
    private var currentBandLevelsMb: IntArray = intArrayOf()

    private fun mappedFiveBandCenters(): IntArray {
        val targets = intArrayOf(60, 230, 910, 3600, 14000)
        if (centerFrequenciesHz.isEmpty()) return targets
        return targets.map { target ->
            centerFrequenciesHz.minByOrNull { actual ->
                kotlin.math.abs(
                    kotlin.math.ln(
                        kotlin.math.max(actual, 1).toDouble() / target.toDouble()
                    )
                )
            } ?: target
        }.toIntArray()
    }

    private fun currentAttachResult(error: String? = null): AudioFxAttachResult {
        val control = equalizer?.let { runCatching { it.hasControl() }.getOrDefault(false) } == true
        return AudioFxAttachResult(
            ready = control && error == null,
            hasControl = control,
            bassBoostAvailable = bassBoost != null,
            bassBoostStrengthSupported = bassStrengthSupported,
            virtualizerAvailable = virtualizer != null,
            virtualizerStrengthSupported = virtualizerStrengthSupported,
            mappedFiveBandCentersHz = mappedFiveBandCenters(),
            error = error
        )
    }

    fun attach(newSessionId: Int, force: Boolean = false): AudioFxAttachResult {
        if (newSessionId <= 0 || newSessionId == C.AUDIO_SESSION_ID_UNSET) {
            release()
            return AudioFxAttachResult(false, false, error = "Phiên âm thanh chưa sẵn sàng.")
        }

        val currentEqualizer = equalizer
        if (!force && newSessionId == sessionId && currentEqualizer != null) {
            return if (runCatching { currentEqualizer.hasControl() }.getOrDefault(false)) {
                currentAttachResult()
            } else {
                currentAttachResult("Equalizer đang bị hệ thống hoặc ứng dụng âm thanh khác kiểm soát.")
            }
        }

        release()
        sessionId = newSessionId
        val createdEqualizer = try {
            AndroidEqualizer(1000, newSessionId).apply { enabled = true }
        } catch (error: Throwable) {
            sessionId = 0
            return AudioFxAttachResult(
                false,
                false,
                error = "Thiết bị không khởi tạo được Equalizer: ${error.localizedMessage ?: error.javaClass.simpleName}"
            )
        }

        equalizer = createdEqualizer
        val hasControl = runCatching { createdEqualizer.hasControl() }.getOrDefault(false)
        if (!hasControl) {
            return currentAttachResult("Equalizer đang bị hệ thống hoặc ứng dụng âm thanh khác kiểm soát.")
        }

        centerFrequenciesHz = runCatching {
            val count = createdEqualizer.numberOfBands.toInt()
            IntArray(count) { index -> createdEqualizer.getCenterFreq(index.toShort()) / 1000 }
        }.getOrDefault(intArrayOf())
        currentBandLevelsMb = runCatching {
            IntArray(centerFrequenciesHz.size) { index ->
                createdEqualizer.getBandLevel(index.toShort()).toInt()
            }
        }.getOrDefault(IntArray(centerFrequenciesHz.size))

        loudnessEnhancer = runCatching {
            LoudnessEnhancer(newSessionId).apply {
                setTargetGain(300)
                enabled = false
            }
        }.getOrNull()

        bassBoost = runCatching {
            BassBoost(1000, newSessionId).apply { enabled = false }
        }.getOrNull()
        bassStrengthSupported = bassBoost?.let {
            runCatching { it.strengthSupported }.getOrDefault(false)
        } == true

        virtualizer = runCatching {
            Virtualizer(1000, newSessionId).apply { enabled = false }
        }.getOrNull()
        virtualizerStrengthSupported = virtualizer?.let {
            runCatching { it.strengthSupported }.getOrDefault(false)
        } == true

        return currentAttachResult()
    }

    fun applyCurve(curveDb: IntArray, smooth: Boolean = true): String? {
        val effect = equalizer ?: return "Equalizer chưa được gắn vào phiên âm thanh."
        if (!runCatching { effect.hasControl() }.getOrDefault(false)) {
            return "MeloWave không có quyền điều khiển Equalizer trên thiết bị này."
        }
        val range = runCatching { effect.bandLevelRange }.getOrNull()
            ?: return "Không đọc được dải điều chỉnh Equalizer."
        if (range.size < 2) return "Thiết bị trả dải Equalizer không hợp lệ."
        val bandCount = runCatching { effect.numberOfBands.toInt() }.getOrDefault(0)
        if (bandCount <= 0) return "Thiết bị không cung cấp băng tần Equalizer."
        val minLevel = range[0].toInt()
        val maxLevel = range[1].toInt()
        val safeCurve = sanitizeEqCurve(curveDb)
        val targets = IntArray(bandCount) { bandIndex ->
            val centerHz = centerFrequenciesHz.getOrElse(bandIndex) {
                runCatching { effect.getCenterFreq(bandIndex.toShort()) / 1000 }.getOrDefault(1)
            }
            (interpolateEqCurveDb(safeCurve, centerHz.toFloat()) * 100f)
                .roundToInt().coerceIn(minLevel, maxLevel)
        }

        val generation = ++animationGeneration
        val start = if (currentBandLevelsMb.size == bandCount) {
            currentBandLevelsMb.copyOf()
        } else {
            runCatching {
                IntArray(bandCount) { effect.getBandLevel(it.toShort()).toInt() }
            }.getOrDefault(IntArray(bandCount))
        }

        fun applyLevels(levels: IntArray): Throwable? {
            var error: Throwable? = null
            levels.forEachIndexed { index, value ->
                runCatching { effect.setBandLevel(index.toShort(), value.toShort()) }
                    .onFailure { error = it }
            }
            currentBandLevelsMb = levels.copyOf()
            return error
        }

        if (!smooth || start.contentEquals(targets)) {
            val error = applyLevels(targets)
            return error?.let {
                "Không áp dụng được Equalizer: ${it.localizedMessage ?: it.javaClass.simpleName}"
            }
        }

        val steps = 10
        for (step in 1..steps) {
            animationHandler.postDelayed({
                if (generation == animationGeneration && equalizer === effect) {
                    val ratio = step / steps.toFloat()
                    val eased = ratio * ratio * (3f - 2f * ratio)
                    val levels = IntArray(bandCount) { index ->
                        (start[index] + (targets[index] - start[index]) * eased).roundToInt()
                    }
                    applyLevels(levels)
                }
            }, MELOWAVE_EQ_TRANSITION_MS * step / steps)
        }
        return null
    }

    fun applyThreeBand(bass: Int, mid: Int, treble: Int): String? {
        val effect = equalizer ?: return "Equalizer chưa được gắn vào phiên âm thanh."
        if (!runCatching { effect.hasControl() }.getOrDefault(false)) {
            return "MeloWave không có quyền điều khiển Equalizer trên thiết bị này."
        }
        val range = runCatching { effect.bandLevelRange }.getOrNull()
            ?: return "Không đọc được dải điều chỉnh Equalizer."
        if (range.size < 2) return "Thiết bị trả dải Equalizer không hợp lệ."
        val minLevel = range[0].toInt()
        val maxLevel = range[1].toInt()
        val bandCount = runCatching { effect.numberOfBands.toInt() }.getOrDefault(0)
        if (bandCount <= 0) return "Thiết bị không cung cấp băng tần Equalizer."

        var applied = 0
        var lastError: Throwable? = null
        for (bandIndex in 0 until bandCount) {
            val band = bandIndex.toShort()
            val centerHz = runCatching { effect.getCenterFreq(band) / 1000f }.getOrDefault(0f)
            val desiredDb = when {
                centerHz < 300f -> bass
                centerHz < 4000f -> mid
                else -> treble
            }
            val desiredMillibels = (desiredDb.coerceIn(-6, 6) * 100).coerceIn(minLevel, maxLevel)
            runCatching { effect.setBandLevel(band, desiredMillibels.toShort()) }
                .onSuccess { applied++ }
                .onFailure { lastError = it }
        }
        return if (applied > 0) null else {
            "Không áp dụng được Equalizer: ${lastError?.localizedMessage ?: "thiết bị từ chối AudioEffect"}"
        }
    }

    fun applyFiveBand(values: IntArray): String? {
        if (values.size < 5) return "Cấu hình Equalizer 5 băng tần không hợp lệ."
        val effect = equalizer ?: return "Equalizer chưa được gắn vào phiên âm thanh."
        if (!runCatching { effect.hasControl() }.getOrDefault(false)) {
            return "MeloWave không có quyền điều khiển Equalizer trên thiết bị này."
        }
        val range = runCatching { effect.bandLevelRange }.getOrNull()
            ?: return "Không đọc được dải điều chỉnh Equalizer."
        if (range.size < 2) return "Thiết bị trả dải Equalizer không hợp lệ."
        val minLevel = range[0].toInt()
        val maxLevel = range[1].toInt()
        val targets = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
        val bandCount = runCatching { effect.numberOfBands.toInt() }.getOrDefault(0)
        if (bandCount <= 0) return "Thiết bị không cung cấp băng tần Equalizer."

        var applied = 0
        var lastError: Throwable? = null
        for (bandIndex in 0 until bandCount) {
            val band = bandIndex.toShort()
            val centerHz = runCatching { effect.getCenterFreq(band) / 1000f }.getOrDefault(1f)
            val closestTarget = targets.indices.minByOrNull { targetIndex ->
                kotlin.math.abs(
                    kotlin.math.ln(kotlin.math.max(centerHz, 1f).toDouble() / targets[targetIndex].toDouble())
                )
            } ?: 0
            val desiredMillibels = (values[closestTarget].coerceIn(-6, 6) * 100)
                .coerceIn(minLevel, maxLevel)
            runCatching { effect.setBandLevel(band, desiredMillibels.toShort()) }
                .onSuccess { applied++ }
                .onFailure { lastError = it }
        }
        return if (applied > 0) null else {
            "Không áp dụng được Equalizer: ${lastError?.localizedMessage ?: "thiết bị từ chối AudioEffect"}"
        }
    }

    fun setBassBoost(strengthPercent: Int): String? {
        val safePercent = strengthPercent.coerceIn(0, 100)
        val effect = bassBoost
            ?: return if (safePercent == 0) null else "Thiết bị không cung cấp Bass Boost phần cứng."
        return runCatching {
            if (safePercent == 0) {
                effect.enabled = false
            } else {
                val requested = if (bassStrengthSupported) safePercent * 10 else 1000
                effect.setStrength(requested.coerceIn(0, 1000).toShort())
                effect.enabled = true
            }
        }.exceptionOrNull()?.let {
            "Không áp dụng được Bass Boost: ${it.localizedMessage ?: it.javaClass.simpleName}"
        }
    }

    fun setVirtualizer(strengthPercent: Int): String? {
        val safePercent = strengthPercent.coerceIn(0, 100)
        val effect = virtualizer
            ?: return if (safePercent == 0) null else "Thiết bị không cung cấp hiệu ứng không gian âm thanh."
        return runCatching {
            if (safePercent == 0) {
                effect.enabled = false
            } else {
                val requested = if (virtualizerStrengthSupported) safePercent * 10 else 1000
                effect.setStrength(requested.coerceIn(0, 1000).toShort())
                effect.enabled = true
            }
        }.exceptionOrNull()?.let {
            "Không áp dụng được Spatial Audio: ${it.localizedMessage ?: it.javaClass.simpleName}"
        }
    }

    fun setLoudness(enabled: Boolean, gainDb: Int): String? {
        val effect = loudnessEnhancer
            ?: return if (!enabled) null else "Thiết bị không cung cấp LoudnessEnhancer."
        return runCatching {
            effect.setTargetGain(gainDb.coerceIn(1, 6) * 100)
            effect.enabled = enabled
        }.exceptionOrNull()?.let {
            "Không bật được Loudness: ${it.localizedMessage ?: it.javaClass.simpleName}"
        }
    }

    fun release() {
        animationGeneration++
        runCatching { equalizer?.enabled = false }
        runCatching { loudnessEnhancer?.enabled = false }
        runCatching { bassBoost?.enabled = false }
        runCatching { virtualizer?.enabled = false }
        runCatching { equalizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        loudnessEnhancer = null
        bassBoost = null
        virtualizer = null
        centerFrequenciesHz = intArrayOf()
        currentBandLevelsMb = intArrayOf()
        bassStrengthSupported = false
        virtualizerStrengthSupported = false
        sessionId = 0
    }
}

private object DriveAudioCache {
    private const val MAX_FILES = 40
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private val locks = ConcurrentHashMap<String, Any>()

    fun getOrCreate(
        context: Context,
        song: SongItem,
        personalDriveAccessToken: String?
    ): Uri {
        require(song.source == SongSource.PERSONAL_DRIVE || song.source == SongSource.PUBLIC_DRIVE)
        val key = MessageDigest.getInstance("SHA-256")
            .digest(song.id.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(32)
        val extension = extensionForMime(song.mimeType)
        val directory = File(context.cacheDir, "tunertools_drive_cache").apply {
            if (!exists() && !mkdirs()) throw IOException("Không tạo được thư mục cache")
        }
        val target = File(directory, "$key.$extension")
        val lock = locks.getOrPut(key) { Any() }
        try {
            synchronized(lock) {
                if (target.isFile && target.length() > 0L) {
                    target.setLastModified(System.currentTimeMillis())
                    return Uri.fromFile(target)
                }
                val partial = File(directory, "$key.$extension.part")
                partial.delete()
                try {
                    FileOutputStream(partial).use { output ->
                        when {
                            song.source == SongSource.PUBLIC_DRIVE -> {
                                SharedDriveApi.copyAudioTo(context, song, output)
                            }
                            song.uri.scheme.equals("mydrive", ignoreCase = true) -> {
                                val token = personalDriveAccessToken
                                    ?: throw PersonalDriveAuthorizationException(
                                        "Phiên Google Drive đã hết hạn. Hãy cấp lại quyền."
                                    )
                                PersonalDriveApi.copyAudioTo(token, song, output)
                            }
                            else -> {
                                // Backward compatibility for old SAF-based playlist entries.
                                context.contentResolver.openInputStream(song.uri)?.use { input ->
                                    input.copyTo(output, 128 * 1024)
                                } ?: throw FileNotFoundException(
                                    "Google Drive không trả dữ liệu cho ${song.title}"
                                )
                            }
                        }
                    }
                    if (!partial.isFile || partial.length() <= 0L) throw IOException("File tải về bị rỗng")
                    if (target.exists()) target.delete()
                    if (!partial.renameTo(target)) {
                        partial.inputStream().use { input ->
                            target.outputStream().use { output -> input.copyTo(output, 128 * 1024) }
                        }
                        partial.delete()
                    }
                    trim(directory, target)
                    return Uri.fromFile(target)
                } catch (error: Throwable) {
                    partial.delete()
                    if (target.length() <= 0L) target.delete()
                    throw error
                }
            }
        } finally {
            locks.remove(key, lock)
        }
    }

    private fun trim(directory: File, protectedFile: File) {
        val files = directory.listFiles()
            ?.filter { it.isFile && !it.name.endsWith(".part") }
            ?.sortedBy { it.lastModified() }
            ?.toMutableList()
            ?: return
        var bytes = files.sumOf { it.length() }
        while (files.size > MAX_FILES || bytes > MAX_BYTES) {
            val oldest = files.firstOrNull { it.absolutePath != protectedFile.absolutePath } ?: break
            files.remove(oldest)
            val length = oldest.length()
            if (oldest.delete()) bytes -= length
        }
    }
}

private object PersonalDriveApi {
    private const val BASE = "https://www.googleapis.com/drive/v3/files"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    private const val MAX_PER_FOLDER = 10_000
    private const val MAX_PLAYLIST = 2_000
    private const val MAX_AUDIO_BYTES = 512L * 1024L * 1024L

    fun importPickerSelection(
        accessToken: String,
        pickedIds: List<String>
    ): List<SongItem> {
        val songs = linkedMapOf<String, SongItem>()
        var firstFailure: Throwable? = null
        pickedIds.forEach { fileId ->
            runCatching {
                val entry = getEntry(accessToken, fileId)
                if (entry.isFolder) {
                    collectAudioRecursively(
                        accessToken = accessToken,
                        selectedFolder = PersonalDriveFolder(entry.id, entry.name)
                    ).forEach { songs[it.id] = it }
                } else if (isPlayable(entry)) {
                    songs[entry.id] = toSong(entry, "Google Picker")
                }
            }.onFailure { error ->
                if (firstFailure == null) firstFailure = error
            }
            if (songs.size > MAX_PLAYLIST) {
                throw IOException("Bạn đã chọn trên $MAX_PLAYLIST bài. Hãy chọn ít nội dung hơn.")
            }
        }
        if (songs.isEmpty() && firstFailure != null) throw firstFailure as Throwable
        return songs.values.toList()
    }

    private fun getEntry(accessToken: String, fileId: String): PersonalDriveEntry {
        val fields = urlEncode("id,name,mimeType,size,capabilities(canDownload)")
        val url = "$BASE/$fileId?fields=$fields&supportsAllDrives=true"
        val item = getJson(accessToken, url)
        val id = item.optString("id")
        if (id.isBlank()) throw FileNotFoundException("Google Picker không trả File ID hợp lệ")
        val mime = item.optString("mimeType")
        return PersonalDriveEntry(
            id = id,
            name = item.optString("name", "Chưa có tên"),
            mimeType = mime,
            isFolder = mime == FOLDER_MIME,
            canDownload = item.optJSONObject("capabilities")
                ?.optBoolean("canDownload", true) ?: true,
            size = item.optLong("size", 0L)
        )
    }

    fun listChildren(
        accessToken: String,
        folder: PersonalDriveFolder
    ): List<PersonalDriveEntry> {
        val results = arrayListOf<PersonalDriveEntry>()
        val query = "'${folder.id}' in parents and trashed = false"
        val fields = "nextPageToken,files(id,name,mimeType,size,capabilities(canDownload))"
        var nextPage: String? = null
        var pages = 0
        do {
            val url = StringBuilder("$BASE?q=${urlEncode(query)}")
                .append("&fields=${urlEncode(fields)}")
                .append("&spaces=drive&pageSize=1000")
                .append("&supportsAllDrives=true&includeItemsFromAllDrives=true")
                .append("&orderBy=${urlEncode("folder,name_natural")}")
                .apply {
                    if (!nextPage.isNullOrBlank()) append("&pageToken=${urlEncode(nextPage!!)}")
                }
                .toString()
            val json = getJson(accessToken, url)
            val array = json.optJSONArray("files")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    val mime = item.optString("mimeType")
                    results += PersonalDriveEntry(
                        id = id,
                        name = item.optString("name", "Chưa có tên"),
                        mimeType = mime,
                        isFolder = mime == FOLDER_MIME,
                        canDownload = item.optJSONObject("capabilities")
                            ?.optBoolean("canDownload", true) ?: true,
                        size = item.optLong("size", 0L)
                    )
                }
            }
            nextPage = json.optString("nextPageToken").takeIf { it.isNotBlank() }
            pages++
            if (results.size >= MAX_PER_FOLDER || pages >= 10) {
                if (nextPage != null) {
                    throw IOException("Thư mục quá lớn. Hãy chọn một thư mục con nhỏ hơn.")
                }
                break
            }
        } while (nextPage != null)
        return results.sortedWith(
            compareBy<PersonalDriveEntry> { !it.isFolder }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }

    fun isPlayable(entry: PersonalDriveEntry): Boolean {
        if (entry.isFolder || !entry.canDownload) return false
        return entry.mimeType.startsWith("audio/", ignoreCase = true) ||
                extensionOf(entry.name) in SUPPORTED_AUDIO_EXTENSIONS
    }

    fun toSong(entry: PersonalDriveEntry, folderName: String): SongItem {
        val uri = Uri.Builder()
            .scheme("mydrive")
            .authority("file")
            .appendPath(entry.id)
            .build()
        val parsed = parseTitleArtist(entry.name, "Google Drive")
        return SongItem(
            id = "mydrive_${entry.id}",
            title = parsed.first,
            artist = parsed.second,
            album = folderName.ifBlank { "Drive của tôi" },
            folderName = folderName.ifBlank { "Drive của tôi" },
            uri = uri,
            source = SongSource.PERSONAL_DRIVE,
            mimeType = resolveAudioMime(entry.name, entry.mimeType)
        )
    }

    fun collectAudioRecursively(
        accessToken: String,
        selectedFolder: PersonalDriveFolder
    ): List<SongItem> {
        val folders = ArrayDeque<Pair<PersonalDriveFolder, String>>()
        val visited = hashSetOf<String>()
        val songs = linkedMapOf<String, SongItem>()
        folders.add(selectedFolder to selectedFolder.name)
        while (folders.isNotEmpty()) {
            val (current, displayPath) = folders.removeFirst()
            if (!visited.add(current.id)) continue
            val entries = listChildren(accessToken, current)
            entries.forEach { entry ->
                if (entry.isFolder) {
                    folders.add(
                        PersonalDriveFolder(entry.id, entry.name) to "$displayPath / ${entry.name}"
                    )
                } else if (isPlayable(entry)) {
                    songs[entry.id] = toSong(entry, displayPath)
                }
                if (songs.size >= MAX_PLAYLIST) {
                    throw IOException(
                        "Thư mục có trên $MAX_PLAYLIST bài. Hãy chọn thư mục nhỏ hơn."
                    )
                }
            }
        }
        return songs.values.toList()
    }

    fun copyAudioTo(accessToken: String, song: SongItem, output: OutputStream) {
        val fileId = song.uri.lastPathSegment
            ?.takeIf { Regex("[A-Za-z0-9_-]{10,}").matches(it) }
            ?: throw FileNotFoundException("Không tìm thấy File ID của Google Drive")
        val url = "$BASE/$fileId?alt=media&supportsAllDrives=true"
        val connection = connect(accessToken, url)
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw httpError(code, connection)
            val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                throw IOException("Google Drive trả dữ liệu không phải âm thanh.")
            }
            connection.inputStream.use { input ->
                val buffer = ByteArray(128 * 1024)
                var bytes = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    bytes += count
                    if (bytes > MAX_AUDIO_BYTES) throw IOException("File lớn hơn 512 MB")
                    output.write(buffer, 0, count)
                }
                if (bytes <= 0L) throw IOException("Google Drive trả file rỗng")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun getJson(accessToken: String, url: String): JSONObject {
        val connection = connect(accessToken, url)
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw httpError(code, connection)
            return JSONObject(
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun connect(accessToken: String, url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty(
            "Accept",
            "application/json, audio/*, application/octet-stream"
        )
        return connection
    }

    private fun httpError(code: Int, connection: HttpURLConnection): IOException {
        val body = runCatching {
            connection.errorStream?.bufferedReader()?.use { it.readText().take(1_200) }
        }.getOrNull().orEmpty()
        val message = runCatching {
            JSONObject(body).getJSONObject("error").optString("message")
        }.getOrNull().orEmpty().ifBlank { body.take(250) }
        return when (code) {
            401 -> PersonalDriveAuthorizationException(
                "Phiên Google Drive đã hết hạn hoặc bị thu hồi. Hãy cấp lại quyền."
            )
            403 -> IOException(
                "Google Drive từ chối quyền truy cập. Kiểm tra Drive API, OAuth scope drive.file và quyền file đã chọn bằng Google Picker. $message"
            )
            404 -> FileNotFoundException("Không tìm thấy file hoặc thư mục Google Drive. $message")
            429 -> IOException("Google Drive API đang vượt giới hạn. Hãy thử lại sau.")
            else -> IOException("Google Drive HTTP $code: $message")
        }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

private object SharedDriveApi {
    private const val BASE = "https://www.googleapis.com/drive/v3/files"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    private const val MAX_PER_FOLDER = 10_000
    private const val MAX_PLAYLIST = 2_000
    private const val MAX_AUDIO_BYTES = 512L * 1024L * 1024L

    fun parseFolderLink(input: String): SharedDriveLink {
        val raw = input.trim()
        if (raw.isBlank()) throw IllegalArgumentException("Hãy dán link thư mục Google Drive")
        if (Regex("[A-Za-z0-9_-]{12,}").matches(raw)) return SharedDriveLink(raw, null)
        val uri = runCatching { Uri.parse(raw) }.getOrNull()
            ?: throw IllegalArgumentException("Liên kết Google Drive không hợp lệ")
        if (uri.scheme?.lowercase(Locale.ROOT) != "https") {
            throw IllegalArgumentException("Link Drive phải bắt đầu bằng https://")
        }
        val host = uri.host?.lowercase(Locale.ROOT)
        if (host !in setOf("drive.google.com", "www.drive.google.com")) {
            throw IllegalArgumentException("Chỉ hỗ trợ link thư mục drive.google.com")
        }
        val segments = uri.pathSegments
        val folderIndex = segments.indexOfLast { it == "folders" }
        val id = if (folderIndex >= 0 && folderIndex + 1 < segments.size) {
            segments[folderIndex + 1]
        } else {
            uri.getQueryParameter("id").orEmpty()
        }
        if (!Regex("[A-Za-z0-9_-]{10,}").matches(id)) {
            throw IllegalArgumentException("Link không chứa Folder ID hợp lệ")
        }
        val resourceKey = uri.getQueryParameter("resourcekey")
            ?: uri.getQueryParameter("resourceKey")
        return SharedDriveLink(id, resourceKey?.takeIf { it.isNotBlank() })
    }

    fun readRoot(context: Context, link: SharedDriveLink): SharedDriveFolder {
        val fields = urlEncode("id,name,mimeType,resourceKey")
        val url = "$BASE/${link.folderId}?fields=$fields&supportsAllDrives=true&key=${urlEncode(apiKey(context))}"
        val json = getJson(context, url, listOf(link.folderId to link.resourceKey))
        if (json.optString("mimeType") != FOLDER_MIME) {
            throw IllegalArgumentException("Link này không trỏ tới thư mục Google Drive")
        }
        return SharedDriveFolder(
            id = json.getString("id"),
            name = json.optString("name", "Google Drive công khai"),
            resourceKey = json.optString("resourceKey").takeIf { it.isNotBlank() } ?: link.resourceKey
        )
    }

    fun listChildren(
        context: Context,
        root: SharedDriveLink,
        folder: SharedDriveFolder
    ): List<SharedDriveEntry> {
        val results = arrayListOf<SharedDriveEntry>()
        val query = "'${folder.id}' in parents and trashed = false"
        val fields = "nextPageToken,files(id,name,mimeType,size,resourceKey,capabilities(canDownload))"
        var nextPage: String? = null
        var pages = 0
        do {
            val url = StringBuilder("$BASE?q=${urlEncode(query)}")
                .append("&fields=${urlEncode(fields)}")
                .append("&pageSize=1000&supportsAllDrives=true&includeItemsFromAllDrives=true")
                .append("&key=${urlEncode(apiKey(context))}")
                .apply { if (!nextPage.isNullOrBlank()) append("&pageToken=${urlEncode(nextPage!!)}") }
                .toString()
            val json = getJson(
                context,
                url,
                listOf(root.folderId to root.resourceKey, folder.id to folder.resourceKey)
            )
            val array = json.optJSONArray("files")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val id = item.optString("id")
                    if (id.isBlank()) continue
                    val mime = item.optString("mimeType")
                    results += SharedDriveEntry(
                        id = id,
                        name = item.optString("name", "Chưa có tên"),
                        mimeType = mime,
                        resourceKey = item.optString("resourceKey").takeIf { it.isNotBlank() },
                        isFolder = mime == FOLDER_MIME,
                        canDownload = item.optJSONObject("capabilities")?.optBoolean("canDownload", true) ?: true,
                        size = item.optLong("size", 0L)
                    )
                }
            }
            nextPage = json.optString("nextPageToken").takeIf { it.isNotBlank() }
            pages++
            if (results.size >= MAX_PER_FOLDER || pages >= 10) {
                if (nextPage != null) throw IOException("Thư mục quá lớn. Hãy chọn thư mục con nhỏ hơn.")
                break
            }
        } while (nextPage != null)
        return results.sortedWith(compareBy<SharedDriveEntry> { !it.isFolder }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    fun isPlayable(entry: SharedDriveEntry): Boolean {
        if (entry.isFolder || !entry.canDownload) return false
        return entry.mimeType.startsWith("audio/", ignoreCase = true) ||
                extensionOf(entry.name) in SUPPORTED_AUDIO_EXTENSIONS
    }

    fun toSong(entry: SharedDriveEntry, root: SharedDriveLink, folderName: String): SongItem {
        val fileUri = Uri.Builder()
            .scheme("publicdrive")
            .authority("file")
            .appendPath(entry.id)
            .appendQueryParameter("rk", entry.resourceKey.orEmpty())
            .appendQueryParameter("rootId", root.folderId)
            .appendQueryParameter("rootKey", root.resourceKey.orEmpty())
            .build()
        val parsed = parseTitleArtist(entry.name, "Google Drive (chia sẻ)")
        return SongItem(
            id = "pubdrive_${entry.id}",
            title = parsed.first,
            artist = parsed.second,
            album = folderName.ifBlank { "Google Drive công khai" },
            folderName = folderName,
            uri = fileUri,
            source = SongSource.PUBLIC_DRIVE,
            mimeType = resolveAudioMime(entry.name, entry.mimeType)
        )
    }

    fun collectAudioRecursively(
        context: Context,
        root: SharedDriveLink,
        selectedFolder: SharedDriveFolder
    ): List<SongItem> {
        val folders = ArrayDeque<SharedDriveFolder>()
        val visited = hashSetOf<String>()
        val songs = linkedMapOf<String, SongItem>()
        folders.add(selectedFolder)
        while (folders.isNotEmpty()) {
            val current = folders.removeFirst()
            if (!visited.add(current.id)) continue
            val entries = listChildren(context, root, current)
            entries.forEach { entry ->
                if (entry.isFolder) folders.add(SharedDriveFolder(entry.id, entry.name, entry.resourceKey))
                else if (isPlayable(entry)) songs[entry.id] = toSong(entry, root, current.name)
                if (songs.size >= MAX_PLAYLIST) throw IOException("Thư mục có trên $MAX_PLAYLIST bài. Hãy chọn thư mục nhỏ hơn.")
            }
        }
        return songs.values.toList()
    }

    fun copyAudioTo(context: Context, song: SongItem, output: OutputStream) {
        val fileId = song.uri.lastPathSegment
            ?.takeIf { Regex("[A-Za-z0-9_-]{10,}").matches(it) }
            ?: throw FileNotFoundException("Không tìm thấy File ID")
        val rootId = song.uri.getQueryParameter("rootId").orEmpty()
        val fileKey = song.uri.getQueryParameter("rk")
        val rootKey = song.uri.getQueryParameter("rootKey")
        val url = "$BASE/$fileId?alt=media&supportsAllDrives=true&key=${urlEncode(apiKey(context))}"
        val connection = connect(context, url, listOf(fileId to fileKey, rootId to rootKey))
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw httpError(code, connection)
            val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
            if (contentType.contains("text/html") || contentType.contains("application/json")) {
                throw IOException("Google Drive trả trang đăng nhập thay vì dữ liệu âm thanh")
            }
            connection.inputStream.use { input ->
                val buffer = ByteArray(128 * 1024)
                var bytes = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    bytes += count
                    if (bytes > MAX_AUDIO_BYTES) throw IOException("File lớn hơn 512 MB")
                    output.write(buffer, 0, count)
                }
                if (bytes <= 0L) throw IOException("Google Drive trả file rỗng")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun apiKey(context: Context): String {
        val key = context.getString(R.string.public_drive_api_key).trim()
        if (key.isBlank() || key.startsWith("REPLACE_", ignoreCase = true)) {
            throw IllegalStateException("Chưa cấu hình public_drive_api_key.xml")
        }
        return key
    }

    private fun connect(
        context: Context,
        url: String,
        resourceKeys: List<Pair<String, String?>>
    ): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json, audio/*, application/octet-stream")
        connection.setRequestProperty("X-Android-Package", context.packageName)
        androidCertSha1(context)?.let { connection.setRequestProperty("X-Android-Cert", it) }
        val validKeys = resourceKeys.filter { !it.second.isNullOrBlank() }.distinctBy { it.first }
        if (validKeys.isNotEmpty()) {
            connection.setRequestProperty(
                "X-Goog-Drive-Resource-Keys",
                validKeys.joinToString(",") { "${it.first}/${it.second.orEmpty()}" }
            )
        }
        return connection
    }

    private fun getJson(
        context: Context,
        url: String,
        resourceKeys: List<Pair<String, String?>>
    ): JSONObject {
        val connection = connect(context, url, resourceKeys)
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw httpError(code, connection)
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun httpError(code: Int, connection: HttpURLConnection): IOException {
        val body = runCatching {
            connection.errorStream?.bufferedReader()?.use { it.readText().take(1_200) }
        }.getOrNull().orEmpty()
        val message = runCatching { JSONObject(body).getJSONObject("error").optString("message") }
            .getOrNull().orEmpty().ifBlank { body.take(250) }
        val advice = when (code) {
            401 -> "Thư mục/file chưa mở công khai."
            403 -> "Kiểm tra quyền Anyone with the link, API key, SHA-1 hoặc quota."
            404 -> "Không tìm thấy thư mục/file hoặc thiếu resourcekey."
            429 -> "Drive API đang vượt giới hạn."
            else -> "Lỗi kết nối Google Drive."
        }
        return IOException("Google Drive HTTP $code: $advice ${message.take(220)}")
    }

    private fun androidCertSha1(context: Context): String? = runCatching {
        val manager = context.packageManager
        @Suppress("DEPRECATION")
        val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            manager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.firstOrNull()
        }
        signature?.let {
            MessageDigest.getInstance("SHA-1")
                .digest(it.toByteArray())
                .joinToString("") { byte -> "%02X".format(byte.toInt() and 0xff) }
        }
    }.getOrNull()

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

// Filter this tag in Logcat on BOTH phones; never log the LAN access token.
private const val MELOWARE_LAN_LOG_TAG = "MeloWareLAN"

private class LocalAudioHttpServer(
    private val contentResolver: android.content.ContentResolver,
    file: File,
    contentType: String,
    displayName: String,
    artist: String = "",
    album: String = "",
    durationMs: Long = 0L,
    songKey: String = ""
) {
    private data class ServedSong(
        val file: File,
        val mimeType: String,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val key: String
    )
    @Volatile private var current = ServedSong(file, contentType, displayName, artist, album, durationMs, songKey)
    @Volatile private var sharedAudio: List<LanSharedAudio> = emptyList()

    fun updateSharedLibrary(allowedSongs: List<LanSharedAudio>) {
        // The server only ever sees the immutable, explicitly selected allow-list.
        sharedAudio = allowedSongs.toList()
        Log.i(MELOWARE_LAN_LOG_TAG, "A: shared library updated, songs=${sharedAudio.size}")
    }

    fun updateSong(
        file: File, mimeType: String, title: String, artist: String, album: String,
        durationMs: Long, key: String
    ) {
        current = ServedSong(file, mimeType, title, artist, album, durationMs, key)
    }
    private val serverSocket = ServerSocket(0)
    private val executor: ExecutorService = Executors.newCachedThreadPool()
    @Volatile private var running = false
    val token: String = UUID.randomUUID().toString().replace("-", "")
    val port: Int get() = serverSocket.localPort

    fun start() {
        if (running) return
        running = true
        Log.i(MELOWARE_LAN_LOG_TAG, "A: LAN HTTP server started on port=$port")
        executor.execute {
            while (running) {
                val socket = try {
                    serverSocket.accept()
                } catch (error: Exception) {
                    if (running) Log.e(MELOWARE_LAN_LOG_TAG, "A: accept() failed", error)
                    break
                }
                try {
                    executor.execute { handleClientSafely(socket) }
                } catch (error: Exception) {
                    // The server may stop just after accept(); do not crash the app.
                    runCatching { socket.close() }
                    if (running) Log.e(MELOWARE_LAN_LOG_TAG, "A: cannot dispatch LAN client", error)
                }
            }
        }
    }

    fun stop() {
        running = false
        Log.i(MELOWARE_LAN_LOG_TAG, "A: LAN HTTP server stopping")
        runCatching { serverSocket.close() }
        executor.shutdownNow()
    }

    /** An exception on an Executor worker is normally fatal for the Android process. */
    private fun handleClientSafely(socket: Socket) {
        try {
            handle(socket)
        } catch (error: java.net.SocketException) {
            // Common when the receiver cancels a folder request / skips a song.
            Log.w(MELOWARE_LAN_LOG_TAG, "A: peer disconnected during request", error)
        } catch (error: IOException) {
            Log.e(MELOWARE_LAN_LOG_TAG, "A: LAN HTTP I/O error", error)
        } catch (error: Exception) {
            // Log full stack trace rather than killing phone A because of one client.
            Log.e(MELOWARE_LAN_LOG_TAG, "A: LAN HTTP request failed", error)
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            client.soTimeout = 15_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.US_ASCII))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return
            val method = parts[0].uppercase(Locale.ROOT)
            val path = parts[1].substringBefore('?')
            val rawQuery = parts[1].substringAfter('?', "")
            val endpoint = when {
                path == "/$token/library" -> if (rawQuery.isBlank()) "folders" else "folder songs"
                path == "/$token/info" -> "song info"
                path.startsWith("/$token/track/") -> "track stream"
                else -> "other"
            }
            Log.d(MELOWARE_LAN_LOG_TAG, "A: HTTP $method request=$endpoint")
            val headers = linkedMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val index = line.indexOf(':')
                if (index > 0) headers[line.substring(0, index).trim().lowercase(Locale.ROOT)] = line.substring(index + 1).trim()
            }
            if (!path.startsWith("/$token/")) {
                writeSimpleResponse(client, 404, "Not Found")
                return
            }
            if (method == "OPTIONS") {
                val output = client.getOutputStream()
                output.write(
                    ("HTTP/1.1 204 No Content\r\n" +
                            "Access-Control-Allow-Origin: *\r\n" +
                            "Access-Control-Allow-Methods: GET, HEAD, OPTIONS\r\n" +
                            "Access-Control-Allow-Headers: Range, Content-Type\r\n" +
                            "Access-Control-Max-Age: 600\r\n" +
                            "Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII)
                )
                output.flush()
                return
            }
            if (method !in setOf("GET", "HEAD")) {
                writeSimpleResponse(client, 405, "Method Not Allowed")
                return
            }
            // App-to-app protocol: authenticated JSON metadata for the shared track.
            if (path == "/$token/info") {
                val song = current
                val body = JSONObject().apply {
                    put("app", "MeloWare")
                    put("version", 1)
                    put("key", song.key)
                    put("title", song.title)
                    put("artist", song.artist)
                    put("album", song.album)
                    put("durationMs", song.durationMs)
                    put("mimeType", song.mimeType)
                }.toString().toByteArray(Charsets.UTF_8)
                val out = client.getOutputStream()
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${body.size}\r\nCache-Control: no-store\r\n" +
                        "Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                if (method == "GET") out.write(body)
                out.flush()
                return
            }
            // Folder browsing is available ONLY for folders explicitly enabled on machine A.
            if (path == "/$token/library") {
                sendLanLibraryJson(client, method, rawQuery)
                return
            }
            if (path.startsWith("/$token/track/")) {
                val key = path.removePrefix("/$token/track/")
                val shared = if (key.matches(Regex("[0-9a-f]{24}")))
                    sharedAudio.firstOrNull { it.key == key } else null
                if (shared == null) {
                    writeSimpleResponse(client, 404, "Not Shared")
                    return
                }
                streamSharedTrack(client, method, headers, shared)
                return
            }
            val song = current  // Keep a consistent file snapshot while this client streams.
            val file = song.file
            val contentType = song.mimeType
            val displayName = song.title
            val total = file.length()
            if (total <= 0L) {
                writeSimpleResponse(client, 404, "Empty file")
                return
            }
            val range = parseRange(headers["range"], total)
            if (range == null && !headers["range"].isNullOrBlank()) {
                val out = client.getOutputStream()
                out.write(("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$total\r\nConnection: close\r\n\r\n").toByteArray())
                out.flush()
                return
            }
            val start = range?.first ?: 0L
            val end = range?.second ?: (total - 1L)
            val length = end - start + 1L
            val status = if (range != null) "206 Partial Content" else "200 OK"
            val safeName = displayName.replace(Regex("[\\r\\n\\\"]"), "_")
            val response = buildString {
                append("HTTP/1.1 $status\r\n")
                append("Content-Type: $contentType\r\n")
                append("Content-Length: $length\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Access-Control-Allow-Origin: *\r\n")
                append("Access-Control-Allow-Headers: Range, Content-Type\r\n")
                append("Cache-Control: no-store\r\n")
                append("Content-Disposition: inline; filename=\"$safeName\"\r\n")
                if (range != null) append("Content-Range: bytes $start-$end/$total\r\n")
                append("Connection: close\r\n\r\n")
            }
            val output = client.getOutputStream()
            output.write(response.toByteArray(Charsets.US_ASCII))
            if (method == "HEAD") {
                output.flush()
                return
            }
            RandomAccessFile(file, "r").use { source ->
                source.seek(start)
                val buffer = ByteArray(128 * 1024)
                var remaining = length
                while (remaining > 0L) {
                    val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
            output.flush()
        }
    }

    private fun sendLanLibraryJson(client: Socket, method: String, rawQuery: String) {
        val args = rawQuery.split('&').mapNotNull { pair ->
            val idx = pair.indexOf('=')
            if (idx < 1) null else runCatching {
                URLDecoder.decode(pair.substring(0, idx), "UTF-8") to
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            }.getOrNull()
        }.toMap()
        val startedAt = android.os.SystemClock.elapsedRealtime()
        val allowed = sharedAudio
        val json = JSONObject().apply {
            put("app", "MeloWare")
            put("version", 2)
            if (!args.containsKey("folder")) {
                val grouped = allowed.groupingBy(LanSharedAudio::folder).eachCount()
                val folders = JSONArray()
                grouped.entries.sortedBy { it.key.lowercase(Locale.ROOT) }.forEach { (folder, count) ->
                    folders.put(JSONObject().put("path", folder).put("count", count))
                }
                put("folders", folders)
                Log.d(MELOWARE_LAN_LOG_TAG, "A: listing shared folders count=${grouped.size}, tracks=${allowed.size}")
            } else {
                val folder = args["folder"].orEmpty()
                val offset = (args["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val tracks = JSONArray()
                var total = 0
                var returned = 0
                // A single pass avoids duplicating a potentially very large folder in RAM.
                for (entry in allowed) {
                    if (entry.folder != folder) continue
                    if (total >= offset && returned < 100) {
                        tracks.put(JSONObject().apply {
                            put("key", entry.key)
                            put("title", entry.title)
                            put("artist", entry.artist)
                            put("album", entry.album)
                            put("durationMs", entry.durationMs)
                            put("mimeType", entry.mimeType)
                        })
                        returned++
                    }
                    total++
                }
                put("songs", tracks)
                put("total", total)
                put("nextOffset", if (offset.toLong() + returned < total) offset + returned else -1)
                Log.d(MELOWARE_LAN_LOG_TAG, "A: listing folder songs total=$total, offset=$offset, returned=$returned")
            }
        }
        val body = json.toString().toByteArray(Charsets.UTF_8)
        Log.d(MELOWARE_LAN_LOG_TAG, "A: library response bytes=${body.size}, elapsedMs=${android.os.SystemClock.elapsedRealtime() - startedAt}")
        val output = client.getOutputStream()
        output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${body.size}\r\nCache-Control: no-store\r\n" +
                "Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
        if (method == "GET") output.write(body)
        output.flush()
    }

    /** Stream an authorized MediaStore content URI with HTTP Range, without sharing its real path. */
    private fun streamSharedTrack(
        client: Socket, method: String, headers: Map<String, String>, entry: LanSharedAudio
    ) {
        Log.d(MELOWARE_LAN_LOG_TAG, "A: audio stream requested (key=${entry.key.take(8)}...)")
        val descriptor = runCatching { contentResolver.openAssetFileDescriptor(entry.uri, "r") }
            .onFailure { Log.e(MELOWARE_LAN_LOG_TAG, "A: failed to access shared track", it) }
            .getOrNull()
        if (descriptor == null) {
            writeSimpleResponse(client, 404, "Audio Not Found")
            return
        }
        descriptor.use { afd ->
            val reported = if (afd.length > 0L) afd.length else 0L
            val total = if (reported > 0L) reported else runCatching {
                contentResolver.query(entry.uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { c ->
                        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else 0L
                    } ?: 0L
            }.getOrDefault(0L)
            if (total <= 0L) {
                writeSimpleResponse(client, 404, "Empty Audio")
                return
            }
            val range = parseRange(headers["range"], total)
            if (range == null && !headers["range"].isNullOrBlank()) {
                val out = client.getOutputStream()
                out.write(("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$total\r\n" +
                        "Connection: close\r\n\r\n").toByteArray(Charsets.US_ASCII))
                out.flush()
                return
            }
            val start = range?.first ?: 0L
            val end = range?.second ?: total - 1L
            val length = end - start + 1L
            val code = if (range == null) "200 OK" else "206 Partial Content"
            val output = client.getOutputStream()
            val header = buildString {
                append("HTTP/1.1 $code\r\n")
                append("Content-Type: ${entry.mimeType}\r\n")
                append("Content-Length: $length\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Cache-Control: no-store\r\n")
                if (range != null) append("Content-Range: bytes $start-$end/$total\r\n")
                append("Connection: close\r\n\r\n")
            }
            output.write(header.toByteArray(Charsets.US_ASCII))
            if (method == "HEAD") {
                output.flush()
                return
            }
            afd.createInputStream().use { source ->
                if (!skipAudioBytes(source, start)) return
                val buffer = ByteArray(64 * 1024)
                var remaining = length
                while (remaining > 0L) {
                    val read = source.read(buffer, 0, minOf(remaining, buffer.size.toLong()).toInt())
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
            output.flush()
        }
    }

    private fun skipAudioBytes(input: InputStream, bytes: Long): Boolean {
        var remaining = bytes
        while (remaining > 0L) {
            val n = input.skip(remaining)
            if (n > 0L) remaining -= n
            else {
                if (input.read() < 0) return false
                remaining--
            }
        }
        return true
    }

    private fun parseRange(value: String?, total: Long): Pair<Long, Long>? {
        if (value.isNullOrBlank()) return null
        val match = Regex("bytes=(\\d*)-(\\d*)", RegexOption.IGNORE_CASE).find(value) ?: return null
        val startText = match.groupValues[1]
        val endText = match.groupValues[2]
        val start: Long
        val end: Long
        if (startText.isBlank()) {
            val suffix = endText.toLongOrNull()?.coerceAtLeast(1L) ?: return null
            start = (total - suffix).coerceAtLeast(0L)
            end = total - 1L
        } else {
            start = startText.toLongOrNull() ?: return null
            end = endText.toLongOrNull()?.coerceAtMost(total - 1L) ?: (total - 1L)
        }
        if (start !in 0 until total || end < start) return null
        return start to end
    }

    private fun writeSimpleResponse(socket: Socket, code: Int, message: String) {
        val body = message.toByteArray(Charsets.UTF_8)
        socket.getOutputStream().apply {
            write(("HTTP/1.1 $code $message\r\nContent-Type: text/plain; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray())
            write(body)
            flush()
        }
    }
}


/** Android Network Service Discovery: MeloWare devices on the SAME local Wi-Fi. */
internal class MeloWareLanDiscovery(context: Context) {
    companion object {
        private const val SERVICE_TYPE = "_meloware._tcp."
    }

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val handler = Handler(Looper.getMainLooper())
    val peers = mutableStateListOf<MeloWareLanPeer>()
    var isSearching by mutableStateOf(false)
        private set
    var isAdvertising by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var publisher: NsdManager.RegistrationListener? = null
    private var publishPort = -1
    private var publishToken: String? = null
    private var browser: NsdManager.DiscoveryListener? = null
    private val resolving: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun advertise(port: Int, token: String) {
        if (port <= 0 || token.length != 32) return
        if (publisher != null && publishPort == port && publishToken == token) return
        stopAdvertising()
        publishPort = port
        publishToken = token
        error = null
        val registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                handler.post { if (publisher === this) isAdvertising = true }
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                handler.post {
                    if (publisher === this) {
                        publisher = null
                        publishPort = -1
                        publishToken = null
                        isAdvertising = false
                        error = "Không thể công bố MeloWare trên mạng (mã $errorCode)."
                    }
                }
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                handler.post { if (publisher === this) isAdvertising = false }
            }
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                handler.post { error = "Lỗi ngừng chia sẻ (mã $errorCode)" }
            }
        }
        publisher = registration
        val service = NsdServiceInfo().apply {
            serviceName = "MeloWare - ${Build.MODEL.take(22)}"
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("token", token)
        }
        runCatching { nsd.registerService(service, NsdManager.PROTOCOL_DNS_SD, registration) }
            .onFailure { ex ->
                publisher = null
                publishPort = -1
                publishToken = null
                error = "Không thể bật tìm kiếm MeloWare: ${ex.localizedMessage}"
            }
    }

    fun stopAdvertising() {
        val listener = publisher ?: return
        publisher = null
        publishPort = -1
        publishToken = null
        isAdvertising = false
        runCatching { nsd.unregisterService(listener) }
    }

    fun startDiscovery() {
        if (browser != null) return
        peers.clear()
        error = null
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                handler.post { if (browser === this) isSearching = true }
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                handler.post {
                    if (browser === this) {
                        browser = null
                        isSearching = false
                        error = "Không thể quét mạng Wi-Fi (mã $errorCode)."
                    }
                }
                runCatching { nsd.stopServiceDiscovery(this) }
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                val name = serviceInfo.serviceName.orEmpty()
                if (!resolving.add(name)) return
                val activeBrowser = this
                @Suppress("DEPRECATION")
                runCatching {
                    nsd.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(service: NsdServiceInfo, errorCode: Int) {
                            resolving.remove(name)
                        }
                        override fun onServiceResolved(service: NsdServiceInfo) {
                            resolving.remove(name)
                            val host = (service.host as? Inet4Address)?.hostAddress.orEmpty()
                            val token = service.attributes["token"]?.toString(Charsets.UTF_8).orEmpty()
                            if (host.isBlank() || !Regex("[0-9a-fA-F]{32}").matches(token) || service.port <= 0) return
                            val peer = MeloWareLanPeer(service.serviceName, host, service.port, token)
                            handler.post {
                                if (browser === activeBrowser && peer.token != publishToken) {
                                    peers.removeAll { it.name == peer.name }
                                    peers.add(peer)
                                }
                            }
                        }
                    })
                }.onFailure { resolving.remove(name) }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                handler.post { peers.removeAll { it.name == serviceInfo.serviceName } }
            }
            override fun onDiscoveryStopped(serviceType: String) {
                handler.post { if (browser === this) isSearching = false }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                handler.post { error = "Lỗi khi dừng quét (mã $errorCode)" }
            }
        }
        browser = listener
        runCatching { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { ex ->
                browser = null
                isSearching = false
                error = "Không thể tìm MeloWare: ${ex.localizedMessage}"
            }
    }

    fun stopDiscovery() {
        val listener = browser ?: return
        browser = null
        isSearching = false
        resolving.clear()
        runCatching { nsd.stopServiceDiscovery(listener) }
    }
}

class MeloWareCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions =
        CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .setCastMediaOptions(CastMediaOptions.Builder().build())
            .setResumeSavedSession(true)
            .setEnableReconnectionService(true)
            .build()

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}

private object LrclibLyricsRepository {
    private const val BASE_URL = "https://lrclib.net"
    private const val CLIENT_ID = "MeloWare/12.6 (https://tunertools.top)"
    private const val CACHE_DIR = "meloware_lyrics"

    fun findLyrics(
        context: Context,
        song: SongItem,
        knownDurationMs: Long,
        forceRefresh: Boolean
    ): LyricsContent {
        val title = cleanTrackName(song.title)
        val artist = cleanArtistName(song.artist)
        val album = song.album.trim()
        val durationSeconds = (knownDurationMs / 1000L)
            .takeIf { it in 1L..3600L }

        if (title.isBlank()) throw IOException("Tên bài hát đang trống nên không thể tìm lời.")

        val cacheFile = lyricsCacheFile(context, title, artist)
        if (!forceRefresh) {
            readCached(cacheFile)?.let { return it }
        }

        val candidates = linkedMapOf<Long, JSONObject>()

        if (artist.isNotBlank() && !isUnknownArtist(artist)) {
            durationSeconds?.let { seconds ->
                requestDirect(title, artist, album, seconds)?.let { json ->
                    candidates[json.optLong("id", -1L)] = json
                }
            }
            if (candidates.isEmpty()) {
                requestDirect(title, artist, album, null)?.let { json ->
                    candidates[json.optLong("id", -1L)] = json
                }
            }
        }

        if (candidates.isEmpty()) {
            requestSearch(title, artist).forEach { json ->
                candidates[json.optLong("id", -1L)] = json
            }
        }

        if (candidates.isEmpty()) {
            requestSearch(title, "").forEach { json ->
                candidates[json.optLong("id", -1L)] = json
            }
        }

        val best = chooseBestMatch(
            records = candidates.values.toList(),
            expectedTitle = title,
            expectedArtist = artist,
            expectedDurationSeconds = durationSeconds
        ) ?: throw IOException("Không tìm thấy lời cho “${song.title}” trên LRCLIB.")

        val content = parseRecord(best)
        writeCached(cacheFile, best)
        return content
    }

    private fun requestDirect(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSeconds: Long?
    ): JSONObject? {
        val params = linkedMapOf(
            "track_name" to trackName,
            "artist_name" to artistName
        )
        if (albumName.isNotBlank()) params["album_name"] = albumName
        durationSeconds?.let { params["duration"] = it.toString() }
        val response = request("$BASE_URL/api/get?${encodeParams(params)}")
        return when (response.first) {
            in 200..299 -> JSONObject(response.second)
            404 -> null
            429 -> throw IOException("LRCLIB đang giới hạn tần suất. Hãy thử lại sau.")
            else -> throw IOException("LRCLIB HTTP ${response.first}: ${errorMessage(response.second)}")
        }
    }

    private fun requestSearch(trackName: String, artistName: String): List<JSONObject> {
        val params = linkedMapOf("track_name" to trackName)
        if (artistName.isNotBlank() && !isUnknownArtist(artistName)) {
            params["artist_name"] = artistName
        }
        val response = request("$BASE_URL/api/search?${encodeParams(params)}")
        if (response.first == 429) {
            throw IOException("LRCLIB đang giới hạn tần suất. Hãy thử lại sau.")
        }
        if (response.first !in 200..299) {
            throw IOException("LRCLIB HTTP ${response.first}: ${errorMessage(response.second)}")
        }
        val array = JSONArray(response.second)
        return buildList {
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let(::add)
            }
        }
    }

    private fun request(url: String): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        // Give a slow first connection more time before the UI shows an error.
        connection.connectTimeout = 16_000
        connection.readTimeout = 26_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", CLIENT_ID)
        connection.setRequestProperty("Lrclib-Client", CLIENT_ID)
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            code to body
        } finally {
            connection.disconnect()
        }
    }

    private fun chooseBestMatch(
        records: List<JSONObject>,
        expectedTitle: String,
        expectedArtist: String,
        expectedDurationSeconds: Long?
    ): JSONObject? {
        return records.maxByOrNull { record ->
            val title = record.optString("trackName", record.optString("name"))
            val artist = record.optString("artistName")
            val duration = record.optDouble("duration", 0.0)
            var score = textSimilarity(expectedTitle, title) * 62.0
            if (expectedArtist.isNotBlank() && !isUnknownArtist(expectedArtist)) {
                score += textSimilarity(expectedArtist, artist) * 30.0
            }
            if (expectedDurationSeconds != null && duration > 0.0) {
                val difference = kotlin.math.abs(duration - expectedDurationSeconds)
                score += when {
                    difference <= 2.0 -> 12.0
                    difference <= 5.0 -> 8.0
                    difference <= 15.0 -> 3.0
                    else -> 0.0
                }
            }
            if (!record.optString("syncedLyrics").isNullOrBlank()) score += 5.0
            score
        }?.takeIf { record ->
            textSimilarity(expectedTitle, record.optString("trackName", record.optString("name"))) >= 0.42
        }
    }

    private fun parseRecord(record: JSONObject): LyricsContent {
        val instrumental = record.optBoolean("instrumental", false)
        val syncedLyrics = record.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
        val plainLyrics = record.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" }
        val syncedLines = syncedLyrics?.let(::parseSyncedLyrics).orEmpty()
        val plainLines = plainLyrics?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.map { LyricLine(null, it) }
            ?.toList()
            .orEmpty()
        val lines = when {
            instrumental -> emptyList()
            syncedLines.isNotEmpty() -> syncedLines
            plainLines.isNotEmpty() -> plainLines
            else -> emptyList()
        }
        return LyricsContent(
            recordId = record.optLong("id", -1L),
            trackName = record.optString("trackName", record.optString("name")),
            artistName = record.optString("artistName"),
            albumName = record.optString("albumName"),
            durationMs = (record.optDouble("duration", 0.0) * 1000.0).toLong(),
            instrumental = instrumental,
            synced = lines.any { it.timestampMs != null },
            lines = lines
        )
    }

    /** Supports standard line-timed LRC and optional enhanced LRC inline word tags. */
    private fun parseSyncedLyrics(raw: String): List<LyricLine> {
        val lineTimestampRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[\\.:](\\d{1,3}))?]")
        val output = mutableListOf<LyricLine>()
        raw.lineSequence().forEach { sourceLine ->
            val matches = lineTimestampRegex.findAll(sourceLine).toList()
            if (matches.isEmpty()) return@forEach
            val rawText = sourceLine.substring(matches.last().range.last + 1)
            val initialTime = parseLrcTimestamp(matches.first()) ?: return@forEach
            val (text, inlineWordTimes) = parseInlineLyricWords(rawText, initialTime)
            if (text.isBlank()) return@forEach
            matches.forEach timestampLoop@ { match ->
                val timeMs = parseLrcTimestamp(match) ?: return@timestampLoop
                // Repeated [time] tags should shift the inline relative timing as well.
                val adjustedWords = if (timeMs == initialTime) inlineWordTimes else {
                    inlineWordTimes.map { word ->
                        word.copy(timestampMs = (word.timestampMs + timeMs - initialTime).coerceAtLeast(0L))
                    }
                }
                output += LyricLine(timeMs, text, adjustedWords)
            }
        }
        return output.sortedBy { it.timestampMs ?: Long.MAX_VALUE }
    }

    private fun parseLrcTimestamp(match: MatchResult): Long? {
        val minute = match.groupValues[1].toLongOrNull() ?: return null
        val second = match.groupValues[2].toLongOrNull() ?: return null
        if (second >= 60L) return null
        val fraction = match.groupValues.getOrNull(3).orEmpty()
        val milliseconds = when (fraction.length) {
            0 -> 0L
            1 -> (fraction.toLongOrNull() ?: 0L) * 100L
            2 -> (fraction.toLongOrNull() ?: 0L) * 10L
            else -> fraction.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        }
        return (minute * 60L + second) * 1000L + milliseconds
    }

    /** Removes <time> markers and maps each word to its best available timestamp. */
    private fun parseInlineLyricWords(rawText: String, lineStartMs: Long): Pair<String, List<LyricWordTiming>> {
        val markerRegex = Regex("<(\\d{1,3}):(\\d{2})(?:[\\.:](\\d{1,3}))?>")
        val markers = mutableListOf<Pair<Int, Long>>()
        val stripped = StringBuilder()
        var lastOffset = 0
        markerRegex.findAll(rawText).forEach { marker ->
            stripped.append(rawText, lastOffset, marker.range.first)
            parseLrcTimestamp(marker)?.let { millis -> markers += stripped.length to millis }
            lastOffset = marker.range.last + 1
        }
        stripped.append(rawText, lastOffset, rawText.length)
        val untrimmed = stripped.toString()
        val trimStart = untrimmed.indexOfFirst { !it.isWhitespace() }
        if (trimStart < 0) return "" to emptyList()
        val text = untrimmed.trim()
        if (markers.isEmpty()) return text to emptyList()

        val adjustedMarkers = markers.map { (offset, millis) ->
            (offset - trimStart).coerceAtLeast(0) to millis
        }
        val words = Regex("\\S+").findAll(text).toList()
        if (words.isEmpty()) return text to emptyList()

        val markerForWord = words.map { word ->
            adjustedMarkers.indexOfLast { it.first <= word.range.first }
        }
        val indicesByMarker = words.indices.groupBy { markerForWord[it] }
        val wordTimes = words.mapIndexed { wordIndex, word ->
            val markerIndex = markerForWord[wordIndex]
            val markerTime = if (markerIndex >= 0) adjustedMarkers[markerIndex].second else lineStartMs
            val nextMarkerTime = adjustedMarkers.getOrNull(markerIndex + 1)?.second
            val group = indicesByMarker[markerIndex].orEmpty()
            val withinGroup = group.indexOf(wordIndex).coerceAtLeast(0)
            val time = if (nextMarkerTime != null && nextMarkerTime > markerTime) {
                markerTime + (nextMarkerTime - markerTime) * withinGroup / group.size.coerceAtLeast(1)
            } else {
                markerTime + withinGroup * 320L
            }
            LyricWordTiming(word.range.first, time.coerceAtLeast(0L))
        }
        return text to wordTimes
    }

    private fun cleanTrackName(value: String): String = value
        .substringBeforeLast('.', value)
        .replace(Regex("""^\s*\d{1,3}\s*[.)_\-]+\s*"""), "")
        .replace(Regex("""(?i)\s*(?:\[|\()(official\s*)?(music\s*)?(video|audio|lyrics?|lyric video|mv|visualizer|karaoke)(?:\]|\))\s*"""), " ")
        .replace(Regex("""(?i)\s*[-|]\s*(official\s*)?(video|audio|lyrics?|mv|karaoke)\s*$"""), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun cleanArtistName(value: String): String = value
        .replace(Regex("""(?i)\s*[-–—]\s*topic\s*$"""), "")
        .replace(Regex("""(?i)\s+(official|music)\s*$"""), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun isUnknownArtist(value: String): Boolean {
        val normalized = normalizeText(value)
        return normalized.isBlank() || normalized in setOf(
            "nghe si chua biet", "unknown artist", "google drive", "movie", "youtube"
        )
    }

    private fun textSimilarity(first: String, second: String): Double {
        val left = normalizeText(first)
        val right = normalizeText(second)
        if (left.isBlank() || right.isBlank()) return 0.0
        if (left == right) return 1.0
        if (left.length >= 4 && right.contains(left)) return 0.91
        if (right.length >= 4 && left.contains(right)) return 0.86
        val leftTokens = left.split(' ').filter(String::isNotBlank).toSet()
        val rightTokens = right.split(' ').filter(String::isNotBlank).toSet()
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        return 2.0 * leftTokens.intersect(rightTokens).size / (leftTokens.size + rightTokens.size)
    }

    private fun encodeParams(params: Map<String, String>): String = params.entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8") }=${URLEncoder.encode(it.value, "UTF-8") }"
    }

    private fun errorMessage(body: String): String = runCatching {
        JSONObject(body).optString("message")
    }.getOrDefault(body.take(220)).ifBlank { "Không xác định" }

    private fun lyricsCacheFile(context: Context, title: String, artist: String): File {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("${normalizeText(title)}|${normalizeText(artist)}".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            .take(32)
        val directory = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
        return File(directory, "$key.json")
    }

    private fun readCached(file: File): LyricsContent? = runCatching {
        if (!file.isFile || file.length() <= 0L) return null
        parseRecord(JSONObject(file.readText(Charsets.UTF_8)))
    }.getOrNull()

    private fun writeCached(file: File, record: JSONObject) {
        runCatching { file.writeText(record.toString(), Charsets.UTF_8) }
    }
}

private object ArtworkResolver {
    private fun normalize(value: String): String {
        val ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .lowercase(Locale.ROOT)
            .replace('đ', 'd')
        return ascii.replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
    }

    fun findArtwork(context: Context, song: SongItem, cachedUri: Uri?): String? {
        embeddedArtwork(context, song, cachedUri)?.let { return it }
        val query = listOf(song.title, song.artist)
            .filter { it.isNotBlank() && !it.contains("Google Drive", true) }
            .joinToString(" ")
            .ifBlank { song.title }
        if (query.isBlank()) return null
        val url = "https://itunes.apple.com/search?term=${URLEncoder.encode(query, "UTF-8")}&media=music&entity=song&limit=25&country=US"
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 6_000
        connection.readTimeout = 8_000
        connection.setRequestProperty("User-Agent", "Tunertools-Android/1.0")
        return try {
            if (connection.responseCode !in 200..299) return null
            val results = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                .optJSONArray("results") ?: return null
            val expectedTitle = normalize(song.title)
            val expectedArtist = normalize(song.artist)
            var bestScore = 0.0
            var bestUrl: String? = null
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val track = normalize(item.optString("trackName"))
                val artist = normalize(item.optString("artistName"))
                val score = similarity(expectedTitle, track) * 0.72 +
                        similarity(expectedArtist, artist) * 0.28
                val raw = item.optString("artworkUrl100")
                if (score > bestScore && raw.startsWith("https://")) {
                    bestScore = score
                    bestUrl = raw.replace("100x100bb", "600x600bb")
                }
            }
            bestUrl?.takeIf { bestScore >= 0.62 }
        } finally {
            connection.disconnect()
        }
    }

    private fun embeddedArtwork(context: Context, song: SongItem, cachedUri: Uri?): String? {
        val sourceUri = when {
            cachedUri != null -> cachedUri
            song.source == SongSource.LOCAL_MEDIASTORE || song.source == SongSource.DOWNLOADED -> song.uri
            else -> return null
        }
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, sourceUri)
            val picture = retriever.embeddedPicture ?: return null
            if (picture.isEmpty() || picture.size > 6 * 1024 * 1024) return null
            val directory = File(context.cacheDir, "tunertools_artwork").apply { mkdirs() }
            val key = MessageDigest.getInstance("SHA-256")
                .digest(song.id.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                .take(32)
            val extension = if (picture.size >= 2 && picture[0] == 0x89.toByte()) "png" else "jpg"
            val file = File(directory, "$key.$extension")
            if (!file.exists() || file.length() <= 0L) file.writeBytes(picture)
            Uri.fromFile(file).toString()
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        if (a == b) return 1.0
        if (a.length >= 4 && b.contains(a)) return 0.92
        if (b.length >= 4 && a.contains(b)) return 0.86
        val left = a.split(' ').filter { it.isNotBlank() }.toSet()
        val right = b.split(' ').filter { it.isNotBlank() }.toSet()
        if (left.isEmpty() || right.isEmpty()) return 0.0
        return 2.0 * left.intersect(right).size / (left.size + right.size)
    }
}

private fun parseDurationLabel(value: String): Long {
    val parts = value.trim().split(':')
    if (parts.size != 2) return 0L
    val minutes = parts[0].toLongOrNull() ?: return 0L
    val seconds = parts[1].toLongOrNull() ?: return 0L
    return (minutes * 60L + seconds) * 1000L
}

private fun normalizeText(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("""\p{Mn}+"""), "")
    .lowercase(Locale.ROOT)
    .replace('đ', 'd')
    .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
    .trim()

private fun android.database.Cursor.stringOrEmpty(index: Int): String =
    if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""

private fun android.database.Cursor.stringOrNull(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

private fun android.database.Cursor.longOrZero(index: Int): Long =
    if (index >= 0 && !isNull(index)) getLong(index) else 0L