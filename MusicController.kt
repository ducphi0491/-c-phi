package duc_phi.music

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.Normalizer
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

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
    DOWNLOADED,
    FAVORITES
}

internal enum class RepeatSetting {
    OFF,
    ALL,
    ONE
}

internal enum class SongSource {
    LOCAL_MEDIASTORE,
    PERSONAL_DRIVE,
    PUBLIC_DRIVE,
    DOWNLOADED,
    ONLINE_HTTP,
    APP_RESOURCE
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
    val artworkUrl: String? = null
) {
    val durationLabel: String
        get() = if (durationMs > 0L) formatDuration(durationMs) else "--:--"
}

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

private data class PendingDocumentFolder(
    val documentId: String,
    val displayPath: String
)

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
    private var playbackJob: Job? = null
    private var tickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val artworkRequests = mutableSetOf<String>()
    private val cachedPlaybackUris = mutableMapOf<String, Uri>()

    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        setAudioAttributes(attributes, true)
    }

    val queue = mutableStateListOf<SongItem>()
    val localSongs = mutableStateListOf<SongItem>()
    val personalDriveSongs = mutableStateListOf<SongItem>()
    val publicDriveSongs = mutableStateListOf<SongItem>()
    val recentSongs = mutableStateListOf<SongItem>()
    val artworkById = mutableStateMapOf<String, String>()
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
    var shuffleEnabled by mutableStateOf(false)
    var repeatSetting by mutableStateOf(RepeatSetting.OFF)
    var errorMessage by mutableStateOf<String?>(null)
    var statusMessage by mutableStateOf<String?>(null)

    var personalDriveUriText by mutableStateOf(prefs.getString(KEY_PERSONAL_DRIVE_URI, "").orEmpty())
        private set
    var personalDriveName by mutableStateOf(prefs.getString(KEY_PERSONAL_DRIVE_NAME, "").orEmpty())
        private set
    var isScanningLocal by mutableStateOf(false)
        private set
    var isScanningPersonalDrive by mutableStateOf(false)
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
    var sleepTimerMinutesLeft by mutableIntStateOf(0)
        private set

    val currentSong: SongItem?
        get() = queue.getOrNull(currentIndex)

    init {
        restoreState()
        configurePlayerListener()
        tickerJob = scope.launch {
            while (true) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                val detected = player.duration
                durationMs = if (detected == C.TIME_UNSET || detected <= 0L) 1L else detected
                delay(if (player.isPlaying) 250L else 650L)
            }
        }
        restorePersonalDriveIfPossible()
    }

    fun release() {
        playbackJob?.cancel()
        tickerJob?.cancel()
        sleepTimerJob?.cancel()
        saveQueue()
        player.release()
        scope.cancel()
    }

    fun dismissError() {
        errorMessage = null
    }

    fun dismissStatus() {
        statusMessage = null
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
    }.distinctBy { it.id }

    fun filteredLibrarySongs(): List<SongItem> {
        val sourceFiltered = when (librarySource) {
            LibrarySource.ALL -> allLibrarySongs()
            LibrarySource.LOCAL -> localSongs.filter { it.source == SongSource.LOCAL_MEDIASTORE }
            LibrarySource.PERSONAL_DRIVE -> personalDriveSongs.toList()
            LibrarySource.PUBLIC_DRIVE -> publicDriveSongs.toList()
            LibrarySource.DOWNLOADED -> localSongs.filter { it.source == SongSource.DOWNLOADED }
            LibrarySource.FAVORITES -> allLibrarySongs().filter { isFavorite(it) }
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
        LibrarySource.ALL -> allLibrarySongs().size
        LibrarySource.LOCAL -> localSongs.count { it.source == SongSource.LOCAL_MEDIASTORE }
        LibrarySource.PERSONAL_DRIVE -> personalDriveSongs.size
        LibrarySource.PUBLIC_DRIVE -> publicDriveSongs.size
        LibrarySource.DOWNLOADED -> localSongs.count { it.source == SongSource.DOWNLOADED }
        LibrarySource.FAVORITES -> allLibrarySongs().count { isFavorite(it) }
    }

    fun recentForHome(limit: Int = 12): List<SongItem> = recentSongs.take(limit)

    fun recommendations(limit: Int = 8): List<SongItem> {
        val favorites = allLibrarySongs().filter { isFavorite(it) }
        val remaining = allLibrarySongs().filterNot { song -> favorites.any { it.id == song.id } }
        return (favorites + remaining).distinctBy { it.id }.take(limit)
    }

    fun newlyAddedFromDrive(limit: Int = 8): List<SongItem> =
        (personalDriveSongs.asReversed() + publicDriveSongs.asReversed())
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
                statusMessage = "Đã quét ${songs.size} bài hát trên điện thoại"
            }.onFailure { throwable ->
                errorMessage = "Không thể quét nhạc trên điện thoại: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
            }
        }
    }

    fun attachPersonalDrive(treeUri: Uri, grantedFlags: Int) {
        if (!isGoogleDriveTree(treeUri)) {
            errorMessage = "Bạn vừa chọn bộ nhớ điện thoại. Hãy mở menu bên trái trong cửa sổ chọn thư mục rồi chọn Google Drive."
            return
        }
        val takeFlags = grantedFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION
        val persisted = runCatching {
            if (takeFlags != 0) {
                context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
            }
            hasPersistedReadPermission(treeUri)
        }.getOrDefault(false)

        personalDriveUriText = treeUri.toString()
        if (persisted) {
            prefs.edit().putString(KEY_PERSONAL_DRIVE_URI, treeUri.toString()).apply()
        }
        scanPersonalDrive(treeUri, persistName = persisted)
    }

    fun disconnectPersonalDrive() {
        val uri = personalDriveUriText.takeIf { it.isNotBlank() }?.let(Uri::parse)
        if (uri != null) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        personalDriveUriText = ""
        personalDriveName = ""
        personalDriveSongs.clear()
        prefs.edit().remove(KEY_PERSONAL_DRIVE_URI).remove(KEY_PERSONAL_DRIVE_NAME).apply()
        statusMessage = "Đã ngắt liên kết thư mục Google Drive cá nhân"
    }

    private fun restorePersonalDriveIfPossible() {
        val saved = personalDriveUriText.takeIf { it.isNotBlank() } ?: return
        val uri = runCatching { Uri.parse(saved) }.getOrNull() ?: return
        if (isGoogleDriveTree(uri) && hasPersistedReadPermission(uri)) {
            scanPersonalDrive(uri, persistName = true)
        } else {
            personalDriveUriText = ""
            personalDriveName = ""
            prefs.edit().remove(KEY_PERSONAL_DRIVE_URI).remove(KEY_PERSONAL_DRIVE_NAME).apply()
        }
    }

    private fun scanPersonalDrive(treeUri: Uri, persistName: Boolean) {
        if (isScanningPersonalDrive) return
        isScanningPersonalDrive = true
        errorMessage = null
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { scanDocumentTree(treeUri) }
            }
            isScanningPersonalDrive = false
            result.onSuccess { (rootName, songs) ->
                personalDriveName = rootName
                personalDriveSongs.clear()
                personalDriveSongs.addAll(songs)
                if (persistName) {
                    prefs.edit()
                        .putString(KEY_PERSONAL_DRIVE_URI, treeUri.toString())
                        .putString(KEY_PERSONAL_DRIVE_NAME, rootName)
                        .apply()
                }
                statusMessage = "Đã thêm ${songs.size} bài từ Google Drive"
            }.onFailure { throwable ->
                errorMessage = when (throwable) {
                    is SecurityException -> "Quyền Google Drive không còn hiệu lực. Hãy chọn lại thư mục."
                    else -> "Không đọc được Google Drive: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
                }
            }
        }
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

    fun togglePublicEntry(entry: SharedDriveEntry) {
        if (!SharedDriveApi.isPlayable(entry)) return
        if (publicSelectedIds[entry.id] == true) publicSelectedIds.remove(entry.id)
        else publicSelectedIds[entry.id] = true
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
        mergePublicSongs(selected)
        showPublicDriveDialog = false
        if (playImmediately) playCollection(selected, 0)
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
                    mergePublicSongs(songs)
                    showPublicDriveDialog = false
                    if (playImmediately) playCollection(songs, 0)
                    else statusMessage = "Đã thêm ${songs.size} bài từ thư mục Drive"
                }
            }.onFailure { throwable ->
                publicDriveError = throwable.localizedMessage ?: "Không quét được thư mục Drive"
            }
        }
    }

    private fun mergePublicSongs(newSongs: List<SongItem>) {
        val merged = (publicDriveSongs + newSongs).distinctBy { it.id }
        publicDriveSongs.clear()
        publicDriveSongs.addAll(merged)
        saveSongList(KEY_PUBLIC_SONGS, publicDriveSongs)
    }

    fun playCollection(collection: List<SongItem>, index: Int) {
        if (collection.isEmpty()) return
        val safeIndex = index.coerceIn(0, collection.lastIndex)
        queue.clear()
        queue.addAll(collection.distinctBy { it.id })
        currentIndex = safeIndex.coerceAtMost(queue.lastIndex)
        saveQueue()
        prepareCurrent(autoPlay = true)
    }

    fun playSong(song: SongItem, collection: List<SongItem> = allLibrarySongs()) {
        val list = collection.takeIf { it.any { item -> item.id == song.id } } ?: listOf(song)
        val index = list.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playCollection(list, index)
    }

    fun playQueueIndex(index: Int) {
        if (index !in queue.indices) return
        currentIndex = index
        prepareCurrent(autoPlay = true)
    }

    fun togglePlayback() {
        val song = currentSong
        when {
            song == null -> {
                val first = allLibrarySongs().firstOrNull() ?: return
                playSong(first)
            }
            player.currentMediaItem?.mediaId != song.id -> prepareCurrent(autoPlay = true)
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
        if (player.currentPosition > 3_000L) {
            player.seekTo(0L)
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
        currentIndex = target
        prepareCurrent(autoPlay = true)
    }

    fun seekTo(position: Long) {
        player.seekTo(position.coerceAtLeast(0L))
        positionMs = position.coerceAtLeast(0L)
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

    fun artworkFor(song: SongItem): String? = artworkById[song.id] ?: song.artworkUrl

    fun ensureArtwork(song: SongItem) {
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

    fun shareCurrentSong() {
        val song = currentSong ?: return
        val shareText = buildString {
            append(song.title)
            if (song.artist.isNotBlank()) append(" - ${song.artist}")
            if (song.source == SongSource.PUBLIC_DRIVE) append("\nNguồn: Google Drive công khai")
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
        SongSource.ONLINE_HTTP -> "Online"
        SongSource.APP_RESOURCE -> "Ứng dụng"
    }

    fun formatBadge(song: SongItem): String {
        val ext = extensionForMime(song.mimeType).uppercase(Locale.ROOT)
        return when (song.source) {
            SongSource.PERSONAL_DRIVE -> "DRIVE $ext"
            SongSource.PUBLIC_DRIVE -> "DRIVE PUBLIC $ext"
            SongSource.DOWNLOADED -> "OFFLINE $ext"
            SongSource.LOCAL_MEDIASTORE -> ext.takeIf { it != "AUDIO" } ?: "OFFLINE"
            SongSource.ONLINE_HTTP -> "ONLINE"
            SongSource.APP_RESOURCE -> "DEMO"
        }
    }

    private fun configurePlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) {
                isPlaying = value
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_READY) {
                    isLoading = false
                    val readyDuration = player.duration
                    if (readyDuration != C.TIME_UNSET && readyDuration > 0L) {
                        durationMs = readyDuration
                        updateSongDuration(currentSong?.id, readyDuration)
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    when (repeatSetting) {
                        RepeatSetting.ONE -> {
                            player.seekTo(0L)
                            player.play()
                        }
                        RepeatSetting.ALL -> next()
                        RepeatSetting.OFF -> if (currentIndex < queue.lastIndex || shuffleEnabled) next()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isLoading = false
                isPlaying = false
                errorMessage = "Không thể phát bài hát. ${error.errorCodeName}: ${error.cause?.localizedMessage ?: error.localizedMessage}"
            }

            override fun onAudioSessionIdChanged(id: Int) {
                if (id > 0 && id != C.AUDIO_SESSION_ID_UNSET) audioSessionId = id
            }
        })
    }

    private fun prepareCurrent(autoPlay: Boolean) {
        val song = currentSong ?: return
        playbackJob?.cancel()
        playbackJob = scope.launch {
            isLoading = true
            errorMessage = null
            val resolvedUri = try {
                resolvePlaybackUri(song)
            } catch (throwable: Throwable) {
                isLoading = false
                errorMessage = when (throwable) {
                    is SecurityException -> "Ứng dụng đã mất quyền đọc Google Drive. Hãy chọn lại thư mục."
                    is FileNotFoundException -> "Không tìm thấy file nhạc hoặc tài khoản hiện tại không có quyền."
                    else -> "Không chuẩn bị được bài hát: ${throwable.localizedMessage ?: "Lỗi không xác định"}"
                }
                return@launch
            }
            if (currentSong?.id != song.id) return@launch
            player.stop()
            player.setMediaItem(song.toMediaItem(resolvedUri, artworkFor(song)))
            player.prepare()
            if (autoPlay) player.play()
            addRecent(song)
            ensureArtwork(song)
            saveQueue()
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
            DriveAudioCache.getOrCreate(context, song)
        }
        cachedPlaybackUris[song.id] = cached
        return cached
    }

    private fun addRecent(song: SongItem) {
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
            MediaStore.Audio.Media.MIME_TYPE
        ).apply {
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
                val downloaded = folder.contains("Tunertools", ignoreCase = true)
                output += SongItem(
                    id = uri.toString(),
                    title = parsed.first,
                    artist = parsed.second,
                    album = cursor.stringOrEmpty(albumIndex),
                    folderName = folder,
                    uri = uri,
                    durationMs = cursor.longOrZero(durationIndex),
                    source = if (downloaded) SongSource.DOWNLOADED else SongSource.LOCAL_MEDIASTORE,
                    mimeType = mime
                )
            }
        }
        return output.distinctBy { it.id }
    }

    private fun scanDocumentTree(treeUri: Uri): Pair<String, List<SongItem>> {
        val resolver = context.contentResolver
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocument = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val rootName = queryDocumentDisplayName(rootDocument)?.ifBlank { "Google Drive" } ?: "Google Drive"
        val pending = ArrayDeque<PendingDocumentFolder>()
        val visited = hashSetOf<String>()
        val songs = mutableListOf<SongItem>()
        pending.add(PendingDocumentFolder(rootId, rootName))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!visited.add(current.documentId)) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, current.documentId)
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.stringOrEmpty(idIndex)
                    if (documentId.isBlank()) continue
                    val displayName = cursor.stringOrEmpty(nameIndex)
                    val mime = cursor.stringOrNull(mimeIndex)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childName = displayName.ifBlank { "Thư mục" }
                        pending.add(PendingDocumentFolder(documentId, "${current.displayPath}/$childName"))
                        continue
                    }
                    if (!isSupportedAudio(displayName, mime)) continue
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    val parsed = parseTitleArtist(displayName, "Google Drive")
                    songs += SongItem(
                        id = documentUri.toString(),
                        title = parsed.first,
                        artist = parsed.second,
                        album = "Google Drive",
                        folderName = current.displayPath,
                        uri = documentUri,
                        durationMs = 0L,
                        source = SongSource.PERSONAL_DRIVE,
                        mimeType = resolveAudioMime(displayName, mime)
                    )
                }
            }
        }
        return rootName to songs.distinctBy { it.id }
    }

    private fun queryDocumentDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    }.getOrNull()

    private fun isGoogleDriveTree(uri: Uri): Boolean =
        uri.authority.orEmpty().startsWith("com.google.android.apps.docs", ignoreCase = true)

    private fun hasPersistedReadPermission(uri: Uri): Boolean =
        context.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.uri == uri
        }

    private fun saveUriToMusicFolder(sourceUri: Uri, song: SongItem): String {
        val safeTitle = song.title.replace(Regex("[\\/:*?\"<>|]"), "_").trim().ifBlank { "Tunertools" }
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
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Tunertools")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val target = resolver.insert(collection, values)
            ?: throw IOException("Không thể tạo file trong thư mục Music")
        try {
            resolver.openInputStream(sourceUri)?.use { input ->
                resolver.openOutputStream(target)?.use { output ->
                    input.copyTo(output, 128 * 1024)
                } ?: throw IOException("Không mở được file đích")
            } ?: throw IOException("Không đọc được dữ liệu bài hát")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(target, values, null, null)
            }
        } catch (error: Throwable) {
            resolver.delete(target, null, null)
            throw error
        }
        return "Music/Tunertools/$fileName"
    }

    private fun restoreState() {
        favoriteIds.putAll(
            prefs.getStringSet(KEY_FAVORITES, emptySet()).orEmpty().associateWith { true }
        )
        queue.addAll(loadSongList(KEY_QUEUE))
        recentSongs.addAll(loadSongList(KEY_RECENTS))
        publicDriveSongs.addAll(loadSongList(KEY_PUBLIC_SONGS))
        currentIndex = prefs.getInt(KEY_CURRENT_INDEX, if (queue.isEmpty()) -1 else 0)
            .coerceIn(if (queue.isEmpty()) -1 else 0, max(queue.lastIndex, 0))
        val restoredPublic = queue.filter { it.source == SongSource.PUBLIC_DRIVE }
        if (restoredPublic.isNotEmpty()) {
            val merged = (publicDriveSongs + restoredPublic).distinctBy { it.id }
            publicDriveSongs.clear()
            publicDriveSongs.addAll(merged)
        }
        val allRestored = (queue + recentSongs + publicDriveSongs).distinctBy { it.id }
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
        songs.forEach { song ->
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
                put("artworkUrl", artworkFor(song).orEmpty())
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
                        "ONLINE_HTTP", "YOUTUBE" -> SongSource.ONLINE_HTTP
                        "APP_RESOURCE" -> SongSource.APP_RESOURCE
                        else -> SongSource.LOCAL_MEDIASTORE
                    }
                    val duration = item.optLong("durationMs", 0L).takeIf { it > 0L }
                        ?: parseDurationLabel(item.optString("durationLabel"))
                    val song = SongItem(
                        id = item.optString("id"),
                        title = item.optString("title", "Bài hát không tên"),
                        artist = item.optString("artist", "Nghệ sĩ chưa biết"),
                        album = item.optString("album"),
                        folderName = item.optString("folderName"),
                        uri = Uri.parse(item.optString("uri")),
                        durationMs = duration,
                        source = source,
                        mimeType = item.optString("mimeType").takeIf { it.isNotBlank() },
                        artworkUrl = item.optString("artworkUrl").takeIf { it.isNotBlank() }
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
        private const val KEY_PERSONAL_DRIVE_URI = "google_drive_tree_uri"
        private const val KEY_PERSONAL_DRIVE_NAME = "google_drive_tree_name"
        private const val KEY_PUBLIC_LINK = "public_drive_last_link"
        private const val DRIVE_CACHE_DIR = "tunertools_drive_cache"
    }
}

private object DriveAudioCache {
    private const val MAX_FILES = 40
    private const val MAX_BYTES = 512L * 1024L * 1024L
    private val locks = ConcurrentHashMap<String, Any>()

    fun getOrCreate(context: Context, song: SongItem): Uri {
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
                        if (song.source == SongSource.PUBLIC_DRIVE) {
                            SharedDriveApi.copyAudioTo(context, song, output)
                        } else {
                            context.contentResolver.openInputStream(song.uri)?.use { input ->
                                input.copyTo(output, 128 * 1024)
                            } ?: throw FileNotFoundException("Google Drive không trả dữ liệu cho ${song.title}")
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
