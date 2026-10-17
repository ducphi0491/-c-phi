package duc_phi.music

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.LruCache
import android.view.TextureView
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.lame.mp3.Lame
import java.io.ByteArrayOutputStream
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal enum class AudioToolMode { CONVERT, EXTRACT }
private enum class AudioTarget(val extension: String, val mime: String, val isLossless: Boolean = false) {
    MP3("mp3", "audio/mpeg"),
    M4A("m4a", "audio/mp4"),
    WAV("wav", "audio/x-wav", true),
    AAC("aac", "audio/aac"),         // AAC-LC bitstream with ADTS headers
    FLAC("flac", "audio/flac", true),
    OGG("ogg", "audio/ogg"),        // Ogg container with Opus codec, NOT Vorbis
    OPUS("opus", "audio/ogg"),      // Same Ogg/Opus format, alternate file extension
    WEBM("webm", "audio/webm"),    // Audio-only WebM/Opus
    AIFF("aiff", "audio/x-aiff", true)
}

/** Do not offer a container/codec combination the installed Android platform cannot encode. */
private fun availableAudioTargets(): List<AudioTarget> {
    val encoders = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { it.isEncoder }
            .flatMap { codec -> codec.supportedTypes.asList() }
            .map { it.lowercase(Locale.ROOT) }.toSet()
    }.getOrDefault(emptySet())
    return AudioTarget.entries.filter { target ->
        when (target) {
            AudioTarget.FLAC -> "audio/flac" in encoders
            AudioTarget.OGG, AudioTarget.OPUS, AudioTarget.WEBM ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && "audio/opus" in encoders
            else -> true
        }
    }
}

private enum class BatchStatus { WAITING, PROCESSING, SAVED, FAILED }

private data class AudioBatchEntry(
    val uri: Uri,
    val name: String,
    val status: BatchStatus = BatchStatus.WAITING,
    val detail: String = ""
)

/** Small, bounded preview cache: no video is copied to memory or persistent storage. */
private data class VideoThumbInfo(val image: Bitmap?, val durationMs: Long?, val sizeBytes: Long?)

private fun readVideoThumbInfo(context: Context, uri: Uri): VideoThumbInfo {
    val bytes = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0) else null
        }
    }.getOrNull()
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
        // Ask Android to decode a small frame directly when supported, instead of
        // decoding a full-resolution 4K frame into app memory.
        val timeUs = ((duration ?: 0L) / 4L).coerceIn(0L, 1_000L) * 1_000L
        val frame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    320, 180)
            }.getOrNull() ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } else retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        val thumb = if (frame != null && (frame.width > 320 || frame.height > 180)) {
            val scale = min(320f / frame.width, 180f / frame.height)
            val resized = Bitmap.createScaledBitmap(frame,
                (frame.width * scale).roundToInt().coerceAtLeast(1),
                (frame.height * scale).roundToInt().coerceAtLeast(1), true)
            if (resized !== frame) frame.recycle()
            resized
        } else frame
        VideoThumbInfo(thumb, duration, bytes)
    } catch (_: Exception) {
        VideoThumbInfo(null, null, bytes)
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
private fun rememberVideoThumbInfo(
    uri: Uri, cache: LruCache<String, VideoThumbInfo>
): VideoThumbInfo? {
    val context = LocalContext.current
    val uriKey = uri.toString()
    val data by produceState<VideoThumbInfo?>(initialValue = cache.get(uriKey), key1 = uriKey) {
        if (value == null) {
            val loaded = withContext(Dispatchers.IO) { readVideoThumbInfo(context, uri) }
            cache.put(uriKey, loaded)
            value = loaded
        }
    }
    return data
}

private fun videoThumbDuration(durationMs: Long?): String =
    durationMs?.let { audioPreviewTime(it) } ?: "--:--"

private fun videoThumbBytes(sizeBytes: Long?): String = when {
    sizeBytes == null || sizeBytes < 0 -> ""
    sizeBytes >= 1024L * 1024L * 1024L -> String.format(Locale.ROOT, "%.1f GB", sizeBytes / (1024.0 * 1024.0 * 1024.0))
    sizeBytes >= 1024L * 1024L -> String.format(Locale.ROOT, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
    else -> "${(sizeBytes / 1024L).coerceAtLeast(1L)} KB"
}

@Composable
private fun VideoThumbFrame(info: VideoThumbInfo?, modifier: Modifier = Modifier) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    Box(
        modifier = modifier.clip(RoundedCornerShape(11.dp))
            .background(if (info?.image != null) Color.Black else colors.borderSoft),
        contentAlignment = Alignment.Center
    ) {
        info?.image?.let { bmp ->
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } ?: Icon(Icons.Rounded.VideoFile, contentDescription = null,
            tint = colors.textSecondary, modifier = Modifier.size(26.dp))
        Box(
            modifier = Modifier.size(30.dp)
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(23.dp))
        }
        if (info?.durationMs != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomStart).padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.70f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(videoThumbDuration(info.durationMs),
                    color = Color.White, fontSize = 10.sp, lineHeight = 11.sp)
            }
        }
    }
}

@Composable
private fun SingleVideoSourcePreview(
    entry: AudioBatchEntry,
    cache: LruCache<String, VideoThumbInfo>,
    canEdit: Boolean,
    onPreview: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    val info = rememberVideoThumbInfo(entry.uri, cache)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPreview).padding(9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoThumbFrame(info, Modifier.width(110.dp).height(66.dp))
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(videoThumbDuration(info?.durationMs), videoThumbBytes(info?.sizeBytes))
                    .filter { it.isNotBlank() }.joinToString(" · "),
                color = colors.textSecondary, fontSize = 11.sp, maxLines = 1
            )
            Text(mwString("audio_tools.preview_video", "Chạm để xem video"),
                color = accent, fontSize = 11.sp)
        }
        if (canEdit) {
            IconButton(onClick = onRemove, modifier = Modifier.size(33.dp)) {
                Icon(Icons.Rounded.Close,
                    contentDescription = mwString("audio_tools.remove_video", "Bỏ video khỏi danh sách"),
                    tint = colors.textSecondary, modifier = Modifier.size(19.dp))
            }
        } else {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                tint = colors.green, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun VideoBatchPreviewTile(
    entry: AudioBatchEntry,
    index: Int,
    cache: LruCache<String, VideoThumbInfo>,
    canEdit: Boolean,
    progress: Float,
    onPreview: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    val info = rememberVideoThumbInfo(entry.uri, cache)
    Column(modifier = Modifier.width(137.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box {
            VideoThumbFrame(info,
                Modifier.fillMaxWidth().height(77.dp).clickable(onClick = onPreview))
            if (canEdit) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(27.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.Black.copy(alpha = 0.77f))
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close,
                        contentDescription = mwString("audio_tools.remove_video", "Bỏ video khỏi danh sách"),
                        tint = Color.White, modifier = Modifier.size(17.dp))
                }
            }
            if (entry.status == BatchStatus.SAVED || entry.status == BatchStatus.FAILED) {
                Icon(
                    if (entry.status == BatchStatus.SAVED) Icons.Rounded.CheckCircle
                    else Icons.Rounded.ErrorOutline, contentDescription = null,
                    tint = if (entry.status == BatchStatus.SAVED) colors.green else colors.red,
                    modifier = Modifier.align(Alignment.TopStart).padding(5.dp)
                        .size(19.dp).background(colors.card, RoundedCornerShape(50))
                )
            }
        }
        Text("${index + 1}. ${entry.name}", color = colors.textPrimary,
            fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        val statusLine = when (entry.status) {
            BatchStatus.SAVED -> mwString("audio_tools.batch_saved", "Đã lưu")
            BatchStatus.FAILED -> entry.detail.ifBlank { mwString("audio_tools.batch_failed", "Thất bại") }
            BatchStatus.PROCESSING -> "${(progress * 100).toInt()}%"
            BatchStatus.WAITING -> videoThumbBytes(info?.sizeBytes).ifBlank {
                videoThumbDuration(info?.durationMs)
            }
        }
        Text(statusLine, maxLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = 10.sp, color = when (entry.status) {
                BatchStatus.SAVED -> colors.green
                BatchStatus.FAILED -> colors.red
                BatchStatus.PROCESSING -> accent
                BatchStatus.WAITING -> colors.textSecondary
            })
    }
}

/**
 * Full-screen overlay opened from Home. Existing one-file Save As flow is kept.
 * For 2+ selections, convert sequentially and save directly to a SAF tree chosen once.
 * No input document is changed or deleted. Cancellation removes only the current temp/output.
 */
@Composable
internal fun MeloWaveAudioToolDialog(mode: AudioToolMode, onDismiss: () -> Unit) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableTargets = remember { availableAudioTargets() }
    val savedLabel = mwString("audio_tools.saved", "Đã lưu file thành công")
    val cancelledLabel = mwString("audio_tools.batch_cancelled", "Đã hủy chuyển đổi. Những file đã lưu được giữ lại.")
    var entries by remember(mode) { mutableStateOf<List<AudioBatchEntry>>(emptyList()) }
    var folderUri by remember(mode) { mutableStateOf<Uri?>(null) }
    var target by remember(mode) { mutableStateOf(if (mode == AudioToolMode.EXTRACT) AudioTarget.M4A else AudioTarget.MP3) }
    var bitrate by remember(mode) { mutableStateOf(192) }
    var progress by remember(mode) { mutableFloatStateOf(0f) }
    var currentIndex by remember(mode) { mutableIntStateOf(-1) }
    var busy by remember(mode) { mutableStateOf(false) }
    var saving by remember(mode) { mutableStateOf(false) }
    var message by remember(mode) { mutableStateOf("") }
    var showHelp by remember(mode) { mutableStateOf(false) }
    var resultFile by remember(mode) { mutableStateOf<File?>(null) }
    var savedUri by remember(mode) { mutableStateOf<Uri?>(null) }
    var previewUri by remember(mode) { mutableStateOf<Uri?>(null) }
    var pendingDeleteUri by remember(mode) { mutableStateOf<Uri?>(null) }
    var videoPreviewUri by remember(mode) { mutableStateOf<Uri?>(null) }
    // LRU capacity is in BYTES (about 50 small video thumbnails at a time).
    val videoThumbCache = remember(mode) {
        object : LruCache<String, VideoThumbInfo>(12 * 1024 * 1024) {
            override fun sizeOf(key: String, value: VideoThumbInfo): Int =
                value.image?.byteCount ?: 256
        }
    }
    var task by remember(mode) { mutableStateOf<Job?>(null) }

    val isBatch = entries.size > 1
    val savedCount = entries.count { it.status == BatchStatus.SAVED }
    val failedCount = entries.count { it.status == BatchStatus.FAILED }
    val processedCount = savedCount + failedCount
    val remainingCount = entries.size - savedCount
    val overallProgress = if (isBatch && entries.isNotEmpty()) {
        ((processedCount + if (busy && currentIndex >= 0) progress else 0f) / entries.size).coerceIn(0f, 1f)
    } else progress
    val folderLabel = remember(folderUri) { folderUri?.let { displayFolderFor(context, it) }.orEmpty() }

    DisposableEffect(mode) {
        onDispose {
            task?.cancel()
            // Single-file export remains in cache until user chooses Save As or closes the dialog.
            resultFile?.delete()
        }
    }

    fun clearResult() {
        resultFile?.delete()
        resultFile = null
        savedUri = null
        previewUri = null
        pendingDeleteUri = null
        progress = 0f
        currentIndex = -1
        message = ""
        entries = entries.map { it.copy(status = BatchStatus.WAITING, detail = "") }
    }

    fun replaceInputs(uris: List<Uri>) {
        if (busy || saving || uris.isEmpty()) return
        clearResult()
        entries = uris.distinctBy { it.toString() }.map { uri ->
            AudioBatchEntry(uri, displayNameFor(context, uri))
        }
    }

    fun updateEntry(index: Int, status: BatchStatus, detail: String = "") {
        entries = entries.mapIndexed { i, item ->
            if (index == i) item.copy(status = status, detail = detail) else item
        }
    }

    val pickSingle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) replaceInputs(listOf(uri))
    }
    val pickMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) replaceInputs(uris)
    }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && !busy && !saving) {
            // Persist the user's tree grant. This can fail on some cloud providers: the
            // session's temporary grant remains usable so it must not block conversion.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            if (folderUri != uri) {
                clearResult()
                folderUri = uri
            }
        }
    }

    fun writeToDocument(destination: Uri?) {
        saving = false
        if (destination == null) return
        val source = resultFile ?: return
        saving = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(destination, "w")
                        ?: error("Cannot open output document")
                    output.use { stream -> source.inputStream().use { it.copyTo(stream, 128 * 1024) } }
                }
                savedUri = destination
                message = ""
                Toast.makeText(context, savedLabel, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Không lưu được file"
            } finally {
                saving = false
            }
        }
    }
    // One SAF picker with the correct MIME for the user's chosen format.
    val createAudio = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        writeToDocument(result.data?.data)
    }
    val chooseDocument = {
        val inputName = entries.firstOrNull()?.name.orEmpty()
        val outputName = "${safeAudioBaseName(inputName)}.${target.extension}"
        createAudio.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = target.mime
            putExtra(Intent.EXTRA_TITLE, outputName)
        })
    }

    fun startSingle() {
        val source = entries.singleOrNull() ?: return
        if (busy || saving) return
        clearResult()
        busy = true
        val format = target
        val kbps = bitrate
        task = scope.launch {
            var temp: File? = null
            try {
                temp = withContext(Dispatchers.IO) {
                    val cache = File(context.cacheDir, "mw_audio_exports").apply { mkdirs() }
                    File.createTempFile("mw_export_", ".${format.extension}", cache)
                }
                val output = temp ?: error("Không tạo được file tạm")
                withContext(Dispatchers.IO) {
                    var lastUiAt = 0L
                    AudioExportEngine.export(
                        context, source.uri, format.extension, kbps, output,
                        check = { if (!isActive) throw CancellationException() },
                        onProgress = { fraction ->
                            val now = SystemClock.uptimeMillis()
                            if (now - lastUiAt >= 150 || fraction >= 1f) {
                                lastUiAt = now
                                progress = fraction.coerceIn(0f, 1f)
                            }
                        }
                    )
                }
                resultFile = temp
                progress = 1f
                temp = null
            } catch (_: CancellationException) {
                message = cancelledLabel
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Không thể xử lý file này"
            } finally {
                temp?.delete()
                busy = false
                task = null
            }
        }
    }

    fun startBatch() {
        val tree = folderUri ?: return
        if (busy || saving || entries.size < 2) return
        val pending = entries.indices.filter { entries[it].status != BatchStatus.SAVED }
        if (pending.isEmpty()) return
        entries = entries.map { if (it.status == BatchStatus.SAVED) it else it.copy(status = BatchStatus.WAITING, detail = "") }
        progress = 0f
        message = ""
        busy = true
        val originals = entries.toList()
        val format = target
        val kbps = bitrate
        task = scope.launch {
            try {
                // Query destination names once; append (2), (3), etc. instead of overwriting.
                val reservedNames = withContext(Dispatchers.IO) { existingNamesInFolder(context, tree) }
                for (index in pending) {
                    if (!isActive) throw CancellationException()
                    currentIndex = index
                    progress = 0f
                    updateEntry(index, BatchStatus.PROCESSING)
                    var temp: File? = null
                    try {
                        temp = withContext(Dispatchers.IO) {
                            val cache = File(context.cacheDir, "mw_audio_exports").apply { mkdirs() }
                            File.createTempFile("mw_batch_", ".${format.extension}", cache)
                        }
                        val output = temp ?: error("Không tạo được file tạm")
                        withContext(Dispatchers.IO) {
                            var lastUiAt = 0L
                            AudioExportEngine.export(
                                context, originals[index].uri, format.extension, kbps, output,
                                check = { if (!isActive) throw CancellationException() },
                                onProgress = { fraction ->
                                    val now = SystemClock.uptimeMillis()
                                    if (now - lastUiAt >= 150 || fraction >= 1f) {
                                        lastUiAt = now
                                        progress = fraction.coerceIn(0f, 1f) * 0.9f
                                    }
                                }
                            )
                        }
                        withContext(Dispatchers.IO) {
                            saveBatchDocument(
                                context, tree, output, originals[index].name, format, reservedNames,
                                check = { if (!isActive) throw CancellationException() },
                                onProgress = { fraction -> progress = 0.9f + fraction.coerceIn(0f, 1f) * 0.1f }
                            )
                        }
                        updateEntry(index, BatchStatus.SAVED)
                        progress = 1f
                    } catch (_: CancellationException) {
                        updateEntry(index, BatchStatus.WAITING)
                        throw CancellationException()
                    } catch (e: Exception) {
                        // One broken/unsupported input does not stop the rest of the queue.
                        updateEntry(index, BatchStatus.FAILED, e.localizedMessage ?: "Không thể xử lý file")
                    } finally {
                        temp?.delete()
                    }
                }
            } catch (_: CancellationException) {
                message = cancelledLabel
            } catch (e: Exception) {
                message = e.localizedMessage ?: "Không thể truy cập thư mục lưu"
            } finally {
                currentIndex = -1
                progress = 0f
                busy = false
                task = null
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!busy && !saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(
                modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .padding(horizontal = 17.dp)
            ) {
                val accent = if (colors.isDark) colors.purpleBright else colors.purple
                // Top bar: the two arrows represent format conversion, rather than a music player.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !busy && !saving,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textPrimary, modifier = Modifier.size(25.dp)
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier.size(39.dp)
                            .background(accent.copy(alpha = if (colors.isDark) 0.19f else 0.11f),
                                RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (mode == AudioToolMode.CONVERT) Icons.Rounded.SwapHoriz else Icons.Rounded.VideoFile,
                            contentDescription = null, tint = accent, modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (mode == AudioToolMode.CONVERT) mwString("audio_tools.convert", "Chuyển đổi nhạc")
                        else mwString("audio_tools.extract", "Tách âm thanh"),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    // Move on-screen explanatory notes into this accessible pop-up.
                    IconButton(
                        onClick = { showHelp = true },
                        modifier = Modifier.size(42.dp)
                            .background(accent.copy(alpha = if (colors.isDark) 0.20f else 0.10f),
                                RoundedCornerShape(13.dp))
                    ) {
                        Icon(Icons.Rounded.Info,
                            contentDescription = mwString("common.help", "Hướng dẫn"),
                            tint = accent, modifier = Modifier.size(23.dp))
                    }
                }

                // On a typical phone all primary controls fit without scrolling.
                // Keep scrolling only as a fallback for very small screens and long batch results.
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(
                        if (mode == AudioToolMode.EXTRACT) 7.dp else 10.dp
                    )
                ) {
                    // 01 - source. File handling, selection logic and batch statuses are unchanged.
                    AudioExportSection(colors.card, colors.border) {
                        AudioToolSectionTitle(
                            number = "01", title = mwString("audio_tools.source", "Tệp nguồn"),
                            icon = Icons.Rounded.UploadFile
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.cardRaised,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, colors.borderSoft)
                        ) {
                            if (mode == AudioToolMode.EXTRACT && entries.isNotEmpty()) {
                                if (isBatch) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.VideoFile, contentDescription = null,
                                                tint = accent, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                mwString("audio_tools.batch_selected", "Đã chọn {count} file",
                                                    "count" to entries.size),
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(mwString("audio_tools.swipe_videos", "Vuốt ngang để xem"),
                                                color = colors.textSecondary, fontSize = 10.sp)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        // Horizontal thumbnails, never an expanding vertical list.
                                        // LazyRow only decodes images for visible video cards.
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                                        ) {
                                            itemsIndexed(entries,
                                                key = { _, item -> item.uri.toString() }) { index, entry ->
                                                VideoBatchPreviewTile(
                                                    entry = entry, index = index,
                                                    cache = videoThumbCache,
                                                    canEdit = !busy && !saving,
                                                    progress = if (currentIndex == index) progress else 0f,
                                                    onPreview = { if (!busy && !saving) videoPreviewUri = entry.uri },
                                                    onRemove = {
                                                        if (!busy && !saving) {
                                                            val remaining = entries.filterNot { it.uri == entry.uri }
                                                            // Reset pending exports; saved output documents remain intact.
                                                            clearResult()
                                                            entries = remaining
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    SingleVideoSourcePreview(
                                        entry = entries.first(), cache = videoThumbCache,
                                        canEdit = !busy && !saving,
                                        onPreview = { if (!busy && !saving) videoPreviewUri = entries.first().uri },
                                        onRemove = {
                                            if (!busy && !saving) {
                                                clearResult()
                                                entries = emptyList()
                                            }
                                        }
                                    )
                                }
                            } else if (isBatch) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AudioToolFileIcon(mode)
                                        Spacer(Modifier.width(9.dp))
                                        Text(
                                            mwString("audio_tools.batch_selected", "Đã chọn {count} file", "count" to entries.size),
                                            modifier = Modifier.weight(1f), color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                        )
                                    }
                                    Spacer(Modifier.height(11.dp))
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().height(108.dp),
                                        verticalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {
                                        itemsIndexed(entries, key = { _, item -> item.uri.toString() }) { index, entry ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                when (entry.status) {
                                                    BatchStatus.SAVED -> Icon(Icons.Rounded.CheckCircle, null,
                                                        tint = colors.green, modifier = Modifier.size(17.dp))
                                                    BatchStatus.FAILED -> Icon(Icons.Rounded.ErrorOutline, null,
                                                        tint = colors.red, modifier = Modifier.size(17.dp))
                                                    BatchStatus.PROCESSING -> CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                                        color = accent
                                                    )
                                                    BatchStatus.WAITING -> Text("${index + 1}.", color = colors.textMuted,
                                                        fontSize = 11.sp)
                                                }
                                                Spacer(Modifier.width(9.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(entry.name, color = colors.textPrimary, fontSize = 12.sp,
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    when (entry.status) {
                                                        BatchStatus.FAILED -> Text(
                                                            entry.detail.ifBlank { mwString("audio_tools.batch_failed", "Thất bại") },
                                                            color = colors.red, fontSize = 11.sp,
                                                            maxLines = 2, overflow = TextOverflow.Ellipsis
                                                        )
                                                        BatchStatus.SAVED -> Text(
                                                            mwString("audio_tools.batch_saved", "Đã lưu"),
                                                            color = colors.green, fontSize = 11.sp
                                                        )
                                                        BatchStatus.PROCESSING -> Text(
                                                            "${mwString("audio_tools.processing", "Đang xử lý...")} ${(progress * 100).toInt()}%",
                                                            color = accent, fontSize = 11.sp
                                                        )
                                                        BatchStatus.WAITING -> Unit
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AudioToolFileIcon(mode)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        entries.firstOrNull()?.name ?: mwString("audio_tools.not_selected", "Chưa chọn tệp"),
                                        color = if (entries.isEmpty()) colors.textSecondary else colors.textPrimary,
                                        fontSize = 13.sp, fontWeight = if (entries.isEmpty()) FontWeight.Normal else FontWeight.Medium,
                                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                    if (entries.isNotEmpty()) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null,
                                            tint = colors.green, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(9.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pickSingle.launch(if (mode == AudioToolMode.CONVERT) arrayOf("audio/*") else arrayOf("video/*")) },
                                enabled = !busy && !saving, modifier = Modifier.weight(1f)
                                    .heightIn(min = if (mode == AudioToolMode.EXTRACT) 43.dp else 47.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Icon(Icons.Rounded.InsertDriveFile, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(mwString("audio_tools.pick_single", "Chọn 1 file"),
                                    color = accent, fontSize = 12.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { pickMultiple.launch(if (mode == AudioToolMode.CONVERT) arrayOf("audio/*") else arrayOf("video/*")) },
                                enabled = !busy && !saving, modifier = Modifier.weight(1f)
                                    .heightIn(min = if (mode == AudioToolMode.EXTRACT) 43.dp else 47.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                border = BorderStroke(1.35.dp, accent)
                            ) {
                                Icon(Icons.Rounded.LibraryAdd, null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(mwString("audio_tools.pick_multiple", "Chọn nhiều file"),
                                    color = accent, fontSize = 12.sp, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (isBatch && mode == AudioToolMode.EXTRACT) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy && !saving) {
                                    pickFolder.launch(null)
                                },
                                color = colors.cardRaised, shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, if (folderUri == null) accent else colors.borderSoft)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(43.dp)
                                        .padding(horizontal = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null,
                                        tint = accent, modifier = Modifier.size(19.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        if (folderUri == null) mwString("audio_tools.batch_choose_folder", "Chọn thư mục lưu")
                                        else folderLabel,
                                        modifier = Modifier.weight(1f), color = colors.textPrimary,
                                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null,
                                        tint = accent, modifier = Modifier.size(17.dp))
                                }
                            }
                        }
                    }

                    if (isBatch && mode == AudioToolMode.CONVERT) {
                        AudioExportSection(colors.card, colors.border) {
                            AudioToolSectionTitle(
                                number = "02", title = mwString("audio_tools.batch_folder", "Thư mục lưu kết quả"),
                                icon = Icons.Rounded.Folder
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (folderUri == null)
                                    mwString("audio_tools.batch_no_folder", "Chọn thư mục một lần để tự động lưu tất cả file sau chuyển đổi.")
                                else folderLabel,
                                color = colors.textSecondary, fontSize = 12.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(11.dp))
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                                enabled = !busy && !saving,
                                onClick = { pickFolder.launch(null) },
                                shape = RoundedCornerShape(13.dp), border = BorderStroke(1.dp, accent)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null,
                                    tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(mwString("audio_tools.batch_choose_folder", "Chọn thư mục lưu"),
                                    color = accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // 02 or 03 - format options in a compact 3-column grid.
                    AudioExportSection(colors.card, colors.border) {
                        AudioToolSectionTitle(
                            number = if (isBatch && mode == AudioToolMode.CONVERT) "03" else "02",
                            title = mwString("audio_tools.output", "Định dạng đầu ra"),
                            icon = Icons.Rounded.SwapHoriz
                        )
                        Spacer(Modifier.height(9.dp))
                        availableTargets.chunked(3).forEach { group ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                group.forEach { entry ->
                                    AudioTargetChoice(
                                        entry = entry, selected = target == entry,
                                        enabled = !busy && !saving,
                                        modifier = Modifier.weight(1f),
                                        onClick = { if (target != entry) { target = entry; clearResult() } }
                                    )
                                }
                                repeat(3 - group.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Spacer(Modifier.height(if (mode == AudioToolMode.EXTRACT) 3.dp else 5.dp))
                        }
                        if (!target.isLossless) {
                            Spacer(Modifier.height(10.dp))
                            Text(mwString("audio_tools.bitrate", "Chất lượng (kbps)"),
                                color = colors.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf(128, 192, 256).forEach { rate ->
                                    val selected = rate == bitrate
                                    Surface(
                                        modifier = Modifier.weight(1f).clickable(enabled = !busy && !saving) {
                                            if (bitrate != rate) { bitrate = rate; clearResult() }
                                        },
                                        color = if (selected) accent.copy(alpha = if (colors.isDark) 0.22f else 0.10f)
                                            else colors.cardRaised,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(if (selected) 1.5.dp else 1.dp,
                                            if (selected) accent else colors.borderSoft)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(42.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text("$rate", color = if (selected) accent else colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                                            if (selected) {
                                                Spacer(Modifier.width(5.dp))
                                                Icon(Icons.Rounded.Check, contentDescription = null,
                                                    tint = accent, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (busy || resultFile != null || (isBatch && processedCount > 0)) {
                        AudioExportSection(colors.card, colors.border) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (busy) CircularProgressIndicator(modifier = Modifier.size(21.dp),
                                    strokeWidth = 2.dp, color = accent)
                                else if (isBatch && failedCount > 0 && savedCount == 0)
                                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.red)
                                else Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.green)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (busy) mwString("audio_tools.processing", "Đang xử lý...")
                                    else if (isBatch) mwString("audio_tools.batch_finished", "Đã hoàn tất lượt chuyển đổi")
                                    else mwString("audio_tools.ready", "Đã xử lý xong — chọn nơi lưu file"),
                                    color = colors.textPrimary, fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${(overallProgress * 100).toInt()}%", color = accent,
                                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(11.dp))
                            LinearProgressIndicator(
                                progress = overallProgress,
                                modifier = Modifier.fillMaxWidth().height(7.dp)
                                    .background(colors.borderSoft, RoundedCornerShape(10.dp)),
                                color = accent, trackColor = colors.borderSoft
                            )
                            if (isBatch) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    mwString("audio_tools.batch_summary", "Đã lưu {done}/{total} • Lỗi {failed}",
                                        "done" to savedCount, "total" to entries.size, "failed" to failedCount),
                                    color = colors.textSecondary, fontSize = 12.sp
                                )
                                if (busy && currentIndex in entries.indices) {
                                    Text(
                                        mwString("audio_tools.batch_current", "File {index}/{total}: {name}",
                                            "index" to currentIndex + 1, "total" to entries.size,
                                            "name" to entries[currentIndex].name),
                                        color = colors.textSecondary, fontSize = 12.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    if (message.isNotBlank()) Text(message, color = colors.red, fontSize = 13.sp)
                    // The SAF URI is the authoritative location. A physical path is only
                    // shown when the document provider actually exposes one.
                    if (!isBatch && savedUri != null) {
                        val fileUri = savedUri!!
                        val savedLocation = remember(fileUri) { savedAudioLocation(context, fileUri) }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = colors.card,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, colors.borderSoft)
                        ) {
                            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Folder, contentDescription = null,
                                        tint = accent, modifier = Modifier.size(17.dp))
                                    Spacer(Modifier.width(8.dp))
                                    // Moves automatically only if the actual path is wider than this area.
                                    Text(savedLocation,
                                        modifier = Modifier.weight(1f)
                                            .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 900),
                                        color = colors.textSecondary,
                                        fontSize = 11.5.sp, lineHeight = 16.sp,
                                        maxLines = 1, softWrap = false)
                                    IconButton(
                                        onClick = { openSavedAudioFolder(context, fileUri) },
                                        enabled = !saving,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(Icons.Rounded.FolderOpen,
                                            contentDescription = mwString("audio_tools.open_folder", "Mở thư mục chứa file"),
                                            tint = accent, modifier = Modifier.size(21.dp))
                                    }
                                    IconButton(
                                        onClick = { pendingDeleteUri = fileUri },
                                        enabled = !saving,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(Icons.Rounded.Delete,
                                            contentDescription = mwString("library.confirm_delete", "Xóa file"),
                                            tint = colors.red, modifier = Modifier.size(21.dp))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { previewUri = fileUri },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
                                    shape = RoundedCornerShape(13.dp),
                                    border = BorderStroke(1.dp, accent)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null,
                                        tint = accent, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(7.dp))
                                    Text(mwString("audio_tools.open_saved", "Mở file vừa lưu"),
                                        color = accent, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // Fixed action area. Disabled state remains legible in light and dark themes.
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 11.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.borderSoft))
                    if (entries.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null,
                                modifier = Modifier.size(17.dp), tint = accent)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "${entries.first().name.substringAfterLast('.', "").uppercase(Locale.ROOT).take(5).ifBlank { "AUDIO" }}  →  ${target.extension.uppercase(Locale.ROOT)}" +
                                    if (target.isLossless) "" else "  ·  $bitrate kbps",
                                color = colors.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (busy) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth().height(53.dp),
                            onClick = { task?.cancel() }, border = BorderStroke(1.dp, colors.red),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(mwString("audio_tools.cancel", "Hủy chuyển đổi"), color = colors.red) }
                    } else if (!isBatch && resultFile != null) {
                        Button(
                            modifier = Modifier.fillMaxWidth().height(54.dp), enabled = !saving,
                            onClick = chooseDocument,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.purple, contentColor = Color.White,
                                disabledContainerColor = colors.cardRaised,
                                disabledContentColor = colors.textMuted
                            )
                        ) {
                            Icon(Icons.Rounded.SaveAlt, contentDescription = null,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(9.dp))
                            Text(mwString("audio_tools.save", "Lưu file vào điện thoại"),
                                fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val canStart = if (isBatch) remainingCount > 0 && folderUri != null else entries.size == 1
                        Button(
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            enabled = canStart && !saving,
                            onClick = { if (isBatch) startBatch() else startSingle() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.purple, contentColor = Color.White,
                                disabledContainerColor = colors.cardRaised,
                                disabledContentColor = colors.textMuted
                            )
                        ) {
                            Icon(
                                if (mode == AudioToolMode.CONVERT) Icons.Rounded.SwapHoriz else Icons.Rounded.VideoFile,
                                contentDescription = null, modifier = Modifier.size(21.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isBatch && remainingCount == 0)
                                    mwString("audio_tools.batch_all_saved", "Đã lưu tất cả file")
                                else if (isBatch && processedCount > 0)
                                    mwString("audio_tools.batch_resume", "Tiếp tục / thử lại file lỗi")
                                else if (mode == AudioToolMode.CONVERT)
                                    mwString("audio_tools.start", "Bắt đầu chuyển đổi")
                                else mwString("audio_tools.extract", "Tách âm thanh"),
                                fontWeight = FontWeight.Bold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Playback of the original video is only a preview: it never changes the
    // conversion queue and stops automatically on dismissal.
    videoPreviewUri?.let { fileUri ->
        VideoSourcePreviewDialog(
            uri = fileUri,
            fileName = remember(fileUri) { entries.firstOrNull { it.uri == fileUri }?.name
                ?: displayNameFor(context, fileUri) },
            onDismiss = { videoPreviewUri = null }
        )
    }

    // Play the saved file *inside* MeloWave, without invoking ACTION_VIEW or
    // requiring another installed music application.
    previewUri?.let { fileUri ->
        SavedAudioPreviewDialog(
            uri = fileUri,
            fileName = remember(fileUri) { displayNameFor(context, fileUri) },
            location = remember(fileUri) { savedAudioLocation(context, fileUri) },
            format = target,
            onOpenFolder = { openSavedAudioFolder(context, fileUri) },
            onDelete = {
                previewUri = null
                pendingDeleteUri = fileUri
            },
            onDismiss = { previewUri = null }
        )
    }

    // Deleting is always explicit and applies ONLY to the saved output document.
    pendingDeleteUri?.let { fileUri ->
        // mwString is @Composable: resolve labels during composition, never inside
        // a coroutine callback or try/catch used to delete the document.
        val deleteSuccessLabel = mwString("audio_tools.deleted", "Đã xóa file đã lưu")
        val deleteFailedLabel = mwString("audio_tools.delete_failed",
            "Không xóa được file. Hãy mở thư mục để xóa bằng trình quản lý tệp.")
        val deleteDeniedLabel = mwString("audio_tools.delete_failed",
            "Không có quyền xóa file này. Hãy mở thư mục để xóa thủ công.")
        AlertDialog(
            onDismissRequest = { if (!saving) pendingDeleteUri = null },
            containerColor = colors.card,
            icon = {
                Icon(Icons.Rounded.Delete, contentDescription = null,
                    tint = colors.red, modifier = Modifier.size(27.dp))
            },
            title = {
                Text(mwString("library.confirm_delete", "Xóa file đã lưu"),
                    color = colors.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(displayNameFor(context, fileUri), color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold)
                    Text(mwString("library.delete_song_warning",
                        "Bạn có chắc muốn xóa vĩnh viễn file này? Không thể hoàn tác."),
                        color = colors.textSecondary, fontSize = 13.sp)
                    Text(mwString("audio_tools.original", "MeloWave không thay đổi hoặc xóa tệp gốc."),
                        color = colors.textSecondary, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteUri = null }, enabled = !saving) {
                    Text(mwString("common.cancel", "Hủy"), color = colors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        pendingDeleteUri = null
                        previewUri = null
                        saving = true
                        scope.launch {
                            try {
                                val deleted = withContext(Dispatchers.IO) {
                                    deleteSavedAudioOutput(context, fileUri)
                                }
                                if (deleted) {
                                    if (savedUri == fileUri) savedUri = null
                                    Toast.makeText(context, deleteSuccessLabel,
                                        Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, deleteFailedLabel,
                                        Toast.LENGTH_LONG).show()
                                }
                            } catch (_: Exception) {
                                Toast.makeText(context, deleteDeniedLabel,
                                    Toast.LENGTH_LONG).show()
                            } finally {
                                saving = false
                            }
                        }
                    }
                ) {
                    Text(mwString("library.confirm_delete", "Xóa file"),
                        color = colors.red, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // The information dialog is the only home for guidance/format notes, freeing
    // the main conversion screen from lengthy text in both light and dark themes.
    if (showHelp) {
        val accent = if (colors.isDark) colors.purpleBright else colors.purple
        val formatNote = when (target) {
            AudioTarget.WAV -> mwString("audio_tools.wav_note", "WAV không nén: file có thể lớn.")
            AudioTarget.M4A -> mwString("audio_tools.m4a_note", "M4A (AAC): ưu tiên chép luồng AAC gốc khi có thể.")
            AudioTarget.MP3 -> mwString("audio_tools.mp3_note", "MP3: mã hóa bằng Java, có thể lâu hơn M4A.")
            AudioTarget.AAC -> mwString("audio_tools.aac_note", "AAC-LC dạng ADTS: gọn nhẹ, tương thích nhiều thiết bị.")
            AudioTarget.FLAC -> mwString("audio_tools.flac_note", "FLAC: không mất dữ liệu sau giải mã, chất lượng tối đa 16-bit từ bộ giải mã.")
            AudioTarget.OGG, AudioTarget.OPUS -> mwString("audio_tools.opus_note", "OGG/OPUS: codec Opus, KHÔNG phải Ogg Vorbis; cần Android 10+.")
            AudioTarget.WEBM -> mwString("audio_tools.webm_note", "WEBM: chỉ chứa âm thanh Opus, không chứa video; cần Android 10+.")
            AudioTarget.AIFF -> mwString("audio_tools.aiff_note", "AIFF: PCM 16-bit không nén, tương thích các phần mềm âm thanh chuyên nghiệp.")
        }
        Dialog(onDismissRequest = { showHelp = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.card,
                shape = RoundedCornerShape(23.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(19.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(39.dp)
                                .background(accent.copy(alpha = if (colors.isDark) 0.22f else 0.11f),
                                    RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = null,
                                modifier = Modifier.size(23.dp), tint = accent)
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(
                            mwString("common.help", "Hướng dẫn"),
                            modifier = Modifier.weight(1f),
                            fontSize = 18.sp, color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showHelp = false }) {
                            Icon(Icons.Rounded.Close,
                                contentDescription = mwString("common.close", "Đóng"),
                                tint = colors.textSecondary)
                        }
                    }
                    Text(
                        if (mode == AudioToolMode.CONVERT)
                            mwString("audio_tools.batch_convert_hint", "Chọn một hoặc nhiều bài nhạc để chuyển đổi. Chế độ nhiều file sẽ tự lưu vào một thư mục.")
                        else mwString("audio_tools.batch_extract_hint", "Chọn một hoặc nhiều video để tách âm thanh. Video gốc được giữ nguyên."),
                        fontSize = 13.sp, lineHeight = 20.sp, color = colors.textSecondary
                    )
                    Surface(
                        color = colors.cardRaised,
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(1.dp, colors.borderSoft)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(
                                "${mwString("audio_tools.output", "Định dạng đầu ra")} · ${target.extension.uppercase(Locale.ROOT)}",
                                color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(formatNote, color = colors.textPrimary,
                                fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.Shield, contentDescription = null,
                            modifier = Modifier.size(17.dp), tint = colors.green)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            mwString("audio_tools.original", "MeloWave không thay đổi hoặc xóa tệp gốc."),
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp, lineHeight = 18.sp, color = colors.textSecondary
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        onClick = { showHelp = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple,
                            contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(mwString("common.understood", "Đã hiểu"),
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


/**
 * SAF documents do not always have a filesystem path. Display a genuine path only
 * for external-storage document IDs (e.g. primary:Music/file.m4a), otherwise show
 * the provider's valid content URI instead of inventing /storage/emulated/0/ paths.
 */
private fun savedAudioLocation(context: Context, uri: Uri): String {
    val name = displayNameFor(context, uri)
    if (uri.scheme == "file") return uri.path ?: name
    if (DocumentsContract.isDocumentUri(context, uri)) {
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
        if (documentId != null) {
            if (uri.authority == "com.android.externalstorage.documents") {
                val volume = documentId.substringBefore(':', "")
                val path = documentId.substringAfter(':', "")
                if (path.isNotBlank()) {
                    val root = if (volume.equals("primary", ignoreCase = true))
                        "Bộ nhớ trong" else "Bộ nhớ ($volume)"
                    return "$root/$path"
                }
            }
            if (documentId.startsWith("raw:")) return documentId.removePrefix("raw:")
        }
    }
    // Cloud/Downloads providers often cannot expose a physical filesystem path.
    // The URI is still an exact, usable Android reference to the saved file.
    return "$name  ·  $uri"
}

/**
 * Ask Android's document browser to start from the selected save location.
 * EXTRA_INITIAL_URI points at the saved document: if it is a file, DocumentsUI
 * attempts to open its parent. Some cloud providers may ignore that hint.
 */
private fun openSavedAudioFolder(context: Context, savedUri: Uri) {
    val browse = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "*/*"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, savedUri)
        }
    }
    try {
        context.startActivity(browse)
    } catch (_: Exception) {
        Toast.makeText(context, "Không mở được trình quản lý tệp trên thiết bị này", Toast.LENGTH_LONG).show()
    }
}

/** SAF document deletion: never touch the original audio/video source or temp files. */
private fun deleteSavedAudioOutput(context: Context, savedUri: Uri): Boolean {
    return when (savedUri.scheme?.lowercase(Locale.ROOT)) {
        "file" -> savedUri.path?.let { File(it).delete() } ?: false
        "content" -> {
            val resolver = context.contentResolver
            // DocumentProvider may support the SAF delete action but not resolver.delete,
            // or vice versa. Try both without requesting broad storage access.
            val safDeleted = if (DocumentsContract.isDocumentUri(context, savedUri)) {
                runCatching { DocumentsContract.deleteDocument(resolver, savedUri) }.getOrDefault(false)
            } else false
            safDeleted || (runCatching { resolver.delete(savedUri, null, null) > 0 }.getOrDefault(false))
        }
        else -> false
    }
}

/** Letterbox/pillarbox portrait and landscape videos instead of distorting them. */
private fun fitVideoTextureView(view: TextureView, videoRatio: Float) {
    val width = view.width.toFloat()
    val height = view.height.toFloat()
    if (width <= 0f || height <= 0f || videoRatio <= 0f) return
    val boxRatio = width / height
    val scaleX = (videoRatio / boxRatio).coerceAtMost(1f)
    val scaleY = (boxRatio / videoRatio).coerceAtMost(1f)
    view.setTransform(Matrix().apply { setScale(scaleX, scaleY, width / 2f, height / 2f) })
}

/** Video preview uses existing Media3 ExoPlayer + Android TextureView; no extra SDK required. */
@Composable
private fun VideoSourcePreviewDialog(uri: Uri, fileName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    var ready by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var seeking by remember(uri) { mutableStateOf(false) }
    var seekFraction by remember(uri) { mutableFloatStateOf(0f) }
    var failure by remember(uri) { mutableStateOf("") }
    var videoRatio by remember(uri) { mutableFloatStateOf(16f / 9f) }
    val player = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA).build(), true)
            setMediaItem(MediaItem.fromUri(uri))
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(value: Boolean) { playing = value }
            override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoRatio = (size.width * size.pixelWidthHeightRatio / size.height)
                        .coerceIn(0.25f, 4f)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    ready = true
                    if (player.duration > 0 && player.duration != C.TIME_UNSET) {
                        durationMs = player.duration
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                ready = false
                failure = error.localizedMessage ?: "Không phát được video trên thiết bị này"
            }
        }
        player.addListener(listener)
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.clearVideoSurface()
            player.release()
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(250L)
            if (player.duration > 0 && player.duration != C.TIME_UNSET) durationMs = player.duration
            if (!seeking) positionMs = player.currentPosition.coerceAtLeast(0L)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth(), color = colors.card,
            shape = RoundedCornerShape(21.dp), border = BorderStroke(1.dp, colors.border)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VideoFile, contentDescription = null,
                        tint = accent, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mwString("audio_tools.preview_video", "Xem trước video"),
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                        Text(fileName, color = colors.textSecondary, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(13.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { androidContext -> TextureView(androidContext).also { texture ->
                            texture.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                                fitVideoTextureView(view as TextureView, videoRatio)
                            }
                            player.setVideoTextureView(texture)
                        } },
                        update = { texture -> fitVideoTextureView(texture, videoRatio) },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!ready && failure.isBlank()) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    }
                }
                if (failure.isNotBlank()) {
                    Text(failure, fontSize = 12.sp, color = colors.red)
                }
                Slider(
                    value = if (seeking) seekFraction else
                        if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { seekFraction = it; seeking = true },
                    onValueChangeFinished = {
                        if (durationMs > 0) player.seekTo((durationMs * seekFraction).toLong())
                        seeking = false
                    },
                    enabled = ready && durationMs > 0 && failure.isBlank(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, activeTrackColor = accent,
                        inactiveTrackColor = colors.borderSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(audioPreviewTime(positionMs), color = colors.textSecondary, fontSize = 12.sp)
                    Text(audioPreviewTime(durationMs), color = colors.textSecondary, fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        if (player.isPlaying) player.pause()
                        else {
                            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0L)
                            player.play()
                        }
                    },
                    enabled = ready && failure.isBlank(),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.purple, contentColor = Color.White,
                        disabledContainerColor = colors.cardRaised,
                        disabledContentColor = colors.textMuted
                    ),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) mwString("library.pause_preview", "Tạm dừng xem")
                        else mwString("home.play", "Phát"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Preview the saved file without switching to another app or replacing the main playlist. */
@Composable
private fun SavedAudioPreviewDialog(
    uri: Uri,
    fileName: String,
    location: String,
    format: AudioTarget,
    onOpenFolder: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    var loading by remember(uri) { mutableStateOf(true) }
    var errorMessage by remember(uri) { mutableStateOf("") }
    var playing by remember(uri) { mutableStateOf(false) }
    var durationMs by remember(uri) { mutableLongStateOf(0L) }
    var positionMs by remember(uri) { mutableLongStateOf(0L) }
    var dragging by remember(uri) { mutableStateOf(false) }
    var draggedFraction by remember(uri) { mutableFloatStateOf(0f) }
    var temporaryWav by remember(uri) { mutableStateOf<File?>(null) }

    val player = remember(context, uri) {
        ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA).build(),
                true
            )
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    loading = false
                    if (player.duration > 0 && player.duration != C.TIME_UNSET) {
                        durationMs = player.duration
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                loading = false
                errorMessage = error.localizedMessage ?: "Không phát được định dạng này trên thiết bị"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
            temporaryWav?.delete()
        }
    }

    LaunchedEffect(player, uri, format) {
        try {
            // Android's default extractors do not support every AIFF variant.
            // MeloWave exports uncompressed 16-bit AIFF, so swap its PCM samples
            // to a temporary standard WAV for preview only. The saved AIFF is untouched.
            val previewSource = if (format == AudioTarget.AIFF) {
                val wav = withContext(Dispatchers.IO) {
                    createAiffWavPreview(context, uri, check = {
                        if (!isActive) throw CancellationException()
                    })
                }
                temporaryWav = wav
                Uri.fromFile(wav)
            } else uri
            player.setMediaItem(MediaItem.fromUri(previewSource))
            player.prepare()
            player.playWhenReady = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loading = false
            errorMessage = e.localizedMessage ?: "Không thể mở file đã lưu"
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(250L)
            val length = player.duration
            if (length > 0 && length != C.TIME_UNSET) durationMs = length
            if (!dragging) positionMs = player.currentPosition.coerceAtLeast(0L)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(), color = colors.card,
            shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, colors.border)
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(
                            accent.copy(alpha = if (colors.isDark) 0.22f else 0.11f),
                            RoundedCornerShape(13.dp)
                        ), contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AudioFile, contentDescription = null,
                            modifier = Modifier.size(23.dp), tint = accent)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mwString("audio_tools.open_saved", "Mở file vừa lưu"),
                            color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(fileName, color = colors.textSecondary, fontSize = 12.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }
                Surface(color = colors.cardRaised, shape = RoundedCornerShape(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 10.dp, end = 3.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null,
                            tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(location,
                            modifier = Modifier.weight(1f)
                                .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 900),
                            color = colors.textSecondary, fontSize = 11.sp, lineHeight = 16.sp,
                            maxLines = 1, softWrap = false)
                        IconButton(onClick = onOpenFolder, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.FolderOpen,
                                contentDescription = mwString("audio_tools.open_folder", "Mở thư mục chứa file"),
                                tint = accent, modifier = Modifier.size(19.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Delete,
                                contentDescription = mwString("library.confirm_delete", "Xóa file"),
                                tint = colors.red, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                if (errorMessage.isNotBlank()) {
                    Text(errorMessage, color = colors.red, fontSize = 12.sp, lineHeight = 18.sp)
                }
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(19.dp),
                            strokeWidth = 2.dp, color = accent)
                        Spacer(Modifier.width(9.dp))
                        Text(mwString("audio_tools.processing", "Đang xử lý..."),
                            fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
                Slider(
                    value = if (dragging) draggedFraction else
                        if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { draggedFraction = it; dragging = true },
                    onValueChangeFinished = {
                        if (durationMs > 0) player.seekTo((durationMs * draggedFraction).toLong())
                        dragging = false
                    },
                    enabled = durationMs > 0 && !loading && errorMessage.isBlank(),
                    colors = SliderDefaults.colors(
                        thumbColor = accent, activeTrackColor = accent,
                        inactiveTrackColor = colors.borderSoft
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(audioPreviewTime(positionMs), color = colors.textSecondary, fontSize = 12.sp)
                    Text(audioPreviewTime(durationMs), color = colors.textSecondary, fontSize = 12.sp)
                }
                Button(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = !loading && errorMessage.isBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.purple, contentColor = Color.White,
                        disabledContainerColor = colors.cardRaised,
                        disabledContentColor = colors.textMuted
                    ),
                    onClick = {
                        if (player.isPlaying) player.pause()
                        else {
                            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                            player.play()
                        }
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (playing) mwString("library.pause_preview", "Tạm dừng nghe thử")
                        else mwString("home.play", "Phát"),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun audioPreviewTime(ms: Long): String {
    val seconds = ms.coerceAtLeast(0L) / 1000L
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

/**
 * Playback-only AIFF (16-bit uncompressed, as exported by AudioExportEngine)
 * -> WAV (little-endian PCM), streamed to cache without loading the song into RAM.
 * This does NOT modify the file chosen by the user in ACTION_CREATE_DOCUMENT.
 */
private fun createAiffWavPreview(context: Context, sourceUri: Uri, check: () -> Unit): File {
    val cache = File(context.cacheDir, "mw_audio_exports").apply { mkdirs() }
    val wavFile = File.createTempFile("mw_preview_", ".wav", cache)
    try {
        val source = context.contentResolver.openInputStream(sourceUri)
            ?: error("Không thể đọc file AIFF vừa lưu")
        DataInputStream(BufferedInputStream(source)).use { input ->
            check()
            val aiffHeader = ByteArray(54)
            input.readFully(aiffHeader)
            fun fourCc(at: Int) = String(aiffHeader, at, 4, Charsets.US_ASCII)
            require(fourCc(0) == "FORM" && fourCc(8) == "AIFF" &&
                fourCc(12) == "COMM" && fourCc(38) == "SSND") {
                "File AIFF không có cấu trúc PCM chuẩn của MeloWave"
            }
            val header = ByteBuffer.wrap(aiffHeader).order(ByteOrder.BIG_ENDIAN)
            val channels = header.getShort(20).toInt()
            val bitsPerSample = header.getShort(26).toInt()
            val exponent = (header.getShort(28).toInt() and 0x7fff) - 16383
            var mantissa = 0.0
            for (index in 30..37) mantissa = mantissa * 256.0 + (aiffHeader[index].toInt() and 255)
            val sampleRate = Math.scalb(mantissa, exponent - 63).roundToInt()
            val ssndLength = header.getInt(42).toLong() and 0xffffffffL
            val pcmLength = ssndLength - 8L
            val dataOffset = header.getInt(46)
            require(channels in 1..2 && bitsPerSample == 16 && sampleRate in 8_000..192_000 &&
                dataOffset == 0 && pcmLength > 0L && pcmLength % (channels * 2L) == 0L &&
                pcmLength <= 0xffffffffL - 36L) {
                "File AIFF không tương thích với bộ nghe thử"
            }
            // WAV RIFF little-endian, 16-bit PCM.
            val wavHeader = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            wavHeader.put("RIFF".toByteArray(Charsets.US_ASCII))
            wavHeader.putInt((36L + pcmLength).toInt())
            wavHeader.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            wavHeader.putInt(16)
            wavHeader.putShort(1)
            wavHeader.putShort(channels.toShort())
            wavHeader.putInt(sampleRate)
            wavHeader.putInt(sampleRate * channels * 2)
            wavHeader.putShort((channels * 2).toShort())
            wavHeader.putShort(16)
            wavHeader.put("data".toByteArray(Charsets.US_ASCII))
            wavHeader.putInt(pcmLength.toInt())
            FileOutputStream(wavFile).use { output ->
                output.write(wavHeader.array())
                val buffer = ByteArray(64 * 1024)
                var remaining = pcmLength
                while (remaining > 0L) {
                    check()
                    val amount = min(remaining, buffer.size.toLong()).toInt()
                    input.readFully(buffer, 0, amount)
                    for (i in 0 until amount step 2) {
                        val tmp = buffer[i]
                        buffer[i] = buffer[i + 1]
                        buffer[i + 1] = tmp
                    }
                    output.write(buffer, 0, amount)
                    remaining -= amount
                }
            }
        }
        return wavFile
    } catch (e: Exception) {
        wavFile.delete()
        throw e
    }
}

@Composable
private fun AudioToolSectionTitle(number: String, title: String, icon: ImageVector) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(32.dp)
                .background(accent.copy(alpha = if (colors.isDark) 0.18f else 0.09f),
                    RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = colors.textPrimary, fontSize = 15.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(number, color = colors.textMuted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun AudioToolFileIcon(mode: AudioToolMode) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    Box(
        modifier = Modifier.size(37.dp)
            .background(accent.copy(alpha = if (colors.isDark) 0.15f else 0.09f),
                RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (mode == AudioToolMode.CONVERT) Icons.Rounded.AudioFile else Icons.Rounded.VideoFile,
            contentDescription = null, modifier = Modifier.size(22.dp), tint = accent
        )
    }
}

@Composable
private fun AudioTargetChoice(
    entry: AudioTarget,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    val accent = if (colors.isDark) colors.purpleBright else colors.purple
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        color = if (selected) accent.copy(alpha = if (colors.isDark) 0.20f else 0.10f)
        else colors.cardRaised,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp,
            if (selected) accent else colors.borderSoft)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(53.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.AudioFile, contentDescription = null,
                tint = if (selected) accent else colors.textSecondary,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(3.dp))
            Text(entry.extension.uppercase(Locale.ROOT), fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) accent else colors.textPrimary)
        }
    }
}

@Composable
private fun AudioExportSection(bg: Color, border: Color, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bg, shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, border)
    ) { Column(modifier = Modifier.padding(13.dp), content = content) }
}

private fun displayNameFor(context: Context, uri: Uri): String = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: uri.lastPathSegment ?: "MeloWave"
}.getOrDefault(uri.lastPathSegment ?: "MeloWave")

private fun displayFolderFor(context: Context, tree: Uri): String = runCatching {
    val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    displayNameFor(context, folder)
}.getOrElse { tree.lastPathSegment ?: "Thư mục đã chọn" }

private fun safeAudioBaseName(name: String): String {
    val base = name.substringBeforeLast('.', name)
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim().trimEnd('.').take(88)
    return base.ifBlank { "MeloWave" }
}

private fun existingNamesInFolder(context: Context, tree: Uri): MutableSet<String> {
    val names = mutableSetOf<String>()
    // A few cloud providers cannot enumerate folder children. They can still create a new
    // document, and the provider itself will resolve collisions if supported.
    runCatching {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getTreeDocumentId(tree)
        )
        context.contentResolver.query(
            children, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                cursor.getString(0)?.let { names += it.lowercase(Locale.ROOT) }
            }
        }
    }
    return names
}

private fun saveBatchDocument(
    context: Context,
    folder: Uri,
    source: File,
    originalName: String,
    target: AudioTarget,
    reservedNames: MutableSet<String>,
    check: () -> Unit,
    onProgress: (Float) -> Unit
): Uri {
    check()
    val base = safeAudioBaseName(originalName)
    var candidate = "$base.${target.extension}"
    var suffix = 2
    while (candidate.lowercase(Locale.ROOT) in reservedNames) {
        candidate = "${base.take(76)} ($suffix).${target.extension}"
        suffix++
    }
    val parent = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
    val destination = DocumentsContract.createDocument(context.contentResolver, parent, target.mime, candidate)
        ?: error("Không tạo được file trong thư mục đã chọn")
    try {
        check()
        context.contentResolver.openOutputStream(destination, "w")?.use { output ->
            source.inputStream().use { input ->
                val buffer = ByteArray(128 * 1024)
                var written = 0L
                var lastUiAt = 0L
                val length = source.length().coerceAtLeast(1L)
                while (true) {
                    check()
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    written += read
                    val now = SystemClock.uptimeMillis()
                    if (now - lastUiAt >= 150L) {
                        lastUiAt = now
                        onProgress(written.toFloat() / length)
                    }
                }
                output.flush()
                onProgress(1f)
            }
        } ?: error("Không thể ghi file vào thư mục đã chọn")
        check()
        reservedNames += candidate.lowercase(Locale.ROOT)
        return destination
    } catch (e: Exception) {
        // Never leave a partial/malformed output behind after cancel or write error.
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, destination) }
        throw e
    }
}

/** Uses platform codecs for AAC/FLAC/Opus, uncompressed PCM for WAV/AIFF, Java LAME for MP3. */
private object AudioExportEngine {
    fun export(
        context: Context, uri: Uri, format: String, bitrateKbps: Int, output: File,
        check: () -> Unit, onProgress: (Float) -> Unit
    ) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("Tệp không có luồng âm thanh có thể đọc")
            val inputFormat = extractor.getTrackFormat(trackIndex)
            extractor.selectTrack(trackIndex)
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION))
                inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L
            val mime = inputFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            if (format == "m4a" && mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                copyAac(extractor, inputFormat, output, durationUs, check, onProgress)
            } else {
                decodeAudio(extractor, inputFormat, format, bitrateKbps, output, durationUs, check, onProgress)
            }
            require(output.length() > 44) { "Không có dữ liệu âm thanh để lưu" }
        } finally {
            extractor.release()
        }
    }

    private fun copyAac(
        extractor: MediaExtractor, input: MediaFormat, target: File, duration: Long,
        check: () -> Unit, progress: (Float) -> Unit
    ) {
        var muxer: MediaMuxer? = null
        var started = false
        try {
            muxer = MediaMuxer(target.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val track = muxer.addTrack(input)
            muxer.start()
            started = true
            val capacity = if (input.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE))
                max(1024 * 1024, input.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)) else 1024 * 1024
            val buffer = ByteBuffer.allocateDirect(min(capacity, 16 * 1024 * 1024))
            val info = MediaCodec.BufferInfo()
            while (true) {
                check()
                buffer.clear()
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val codecFlags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                    MediaCodec.BUFFER_FLAG_KEY_FRAME
                } else {
                    0
                }
                info.set(0, size, max(0L, extractor.sampleTime), codecFlags)
                buffer.position(0)
                buffer.limit(size)
                muxer.writeSampleData(track, buffer, info)
                if (duration > 0) progress((info.presentationTimeUs.toFloat() / duration).coerceIn(0f, 0.99f))
                extractor.advance()
            }
            progress(1f)
        } finally {
            if (started) runCatching { muxer?.stop() }
            muxer?.release()
        }
    }

    private fun decodeAudio(
        extractor: MediaExtractor, source: MediaFormat, format: String, bitrate: Int,
        target: File, duration: Long, check: () -> Unit, progress: (Float) -> Unit
    ) {
        val mime = source.getString(MediaFormat.KEY_MIME) ?: error("Không nhận dạng được âm thanh")
        val decoder = MediaCodec.createDecoderByType(mime)
        var sink: PcmWriter? = null
        try {
            decoder.configure(source, null, null, 0)
            decoder.start()
            var inputEnded = false
            var outputEnded = false
            val info = MediaCodec.BufferInfo()
            while (!outputEnded) {
                check()
                if (!inputEnded) {
                    val index = decoder.dequeueInputBuffer(10_000)
                    if (index >= 0) {
                        val buffer = decoder.getInputBuffer(index) ?: error("Decoder input buffer unavailable")
                        buffer.clear()
                        val read = extractor.readSampleData(buffer, 0)
                        if (read < 0) {
                            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(index, 0, read, max(0L, extractor.sampleTime), 0)
                            extractor.advance()
                        }
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 10_000)
                when (index) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (sink != null) error("Âm thanh thay đổi định dạng trong khi xử lý")
                        sink = buildWriter(decoder.outputFormat, format, bitrate, target)
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER, MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (index >= 0) {
                        if (sink == null) sink = buildWriter(decoder.outputFormat, format, bitrate, target)
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            val data = decoder.getOutputBuffer(index) ?: error("Decoder output buffer unavailable")
                            data.position(info.offset)
                            data.limit(info.offset + info.size)
                            val raw = ByteArray(info.size)
                            data.get(raw)
                            sink!!.write(raw)
                            if (duration > 0) progress((info.presentationTimeUs.toFloat() / duration).coerceIn(0f, 0.99f))
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        decoder.releaseOutputBuffer(index, false)
                    }
                }
            }
            check()
            val writer = sink ?: error("Không tìm được dữ liệu âm thanh")
            writer.finish()
            progress(1f)
        } finally {
            runCatching { sink?.close() }
            runCatching { decoder.stop() }
            decoder.release()
        }
    }

    private fun buildWriter(outputFormat: MediaFormat, ext: String, bitrate: Int, file: File): PcmWriter {
        val sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        require(channels in 1..2) { "Hiện chỉ hỗ trợ âm thanh mono hoặc stereo" }
        val pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else 2
        require(pcmEncoding == 2) { "Thiết bị xuất PCM không phải 16-bit; chưa hỗ trợ codec này" }
        return when (ext) {
            "wav" -> WavWriter(file, sampleRate, channels)
            "aiff" -> AiffWriter(file, sampleRate, channels)
            "mp3" -> Mp3Writer(file, sampleRate, channels, bitrate)
            "m4a" -> AacWriter(file, sampleRate, channels, bitrate)
            "aac" -> PlatformAudioWriter(file, sampleRate, channels, bitrate, AudioTarget.AAC)
            "flac" -> PlatformAudioWriter(file, sampleRate, channels, bitrate, AudioTarget.FLAC)
            "ogg", "opus", "webm" -> {
                // Android's Opus encoder only accepts 8, 12, 16, 24 or 48 kHz.
                // Transparently resample 44.1kHz music and other input rates to 48kHz.
                val kind = when (ext) {
                    "ogg" -> AudioTarget.OGG
                    "opus" -> AudioTarget.OPUS
                    else -> AudioTarget.WEBM
                }
                val writer = PlatformAudioWriter(file, 48_000, channels, bitrate, kind)
                if (sampleRate == 48_000) writer else Pcm16Resampler(writer, sampleRate, 48_000, channels)
            }
            else -> error("Định dạng xuất không được hỗ trợ")
        }
    }

    private interface PcmWriter : Closeable {
        fun write(pcm: ByteArray)
        fun finish()
    }

    private class WavWriter(file: File, private val rate: Int, private val channels: Int) : PcmWriter {
        private val target = RandomAccessFile(file, "rw")
        private var bytes = 0L
        init { target.setLength(0); target.write(ByteArray(44)) }
        override fun write(pcm: ByteArray) {
            require(bytes + pcm.size <= 0xFFFFFFFFL - 36L) { "File WAV vượt giới hạn 4 GB" }
            target.write(pcm)
            bytes += pcm.size
        }
        override fun finish() {
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray(Charsets.US_ASCII))
            h.putInt((36L + bytes).toInt())
            h.put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            h.putInt(16)
            h.putShort(1)
            h.putShort(channels.toShort())
            h.putInt(rate)
            h.putInt(rate * channels * 2)
            h.putShort((channels * 2).toShort())
            h.putShort(16)
            h.put("data".toByteArray(Charsets.US_ASCII))
            h.putInt(bytes.toInt())
            target.seek(0)
            target.write(h.array())
        }
        override fun close() { target.close() }
    }

    private class Mp3Writer(file: File, rate: Int, private val channels: Int, bitrate: Int) : PcmWriter {
        private val stream = FileOutputStream(file)
        private val encoder = Lame()
        private var remainder = ByteArray(0)
        init {
            val flags = encoder.getFlags()
            flags.setInNumChannels(channels)
            flags.setInSampleRate(rate)
            flags.setBitRate(if (rate < 32_000) min(bitrate, 160) else bitrate)
            flags.setQuality(5)
            encoder.getId3().init(flags)
            flags.setWriteId3tagAutomatic(false)
            val state = encoder.initParams()
            require(state >= 0) { "Không khởi tạo được bộ mã hóa MP3 ($state)" }
        }
        override fun write(pcm: ByteArray) {
            val alignedSize = ((remainder.size + pcm.size) / (2 * channels)) * (2 * channels)
            val joined = if (remainder.isEmpty()) pcm else remainder + pcm
            if (alignedSize == 0) { remainder = joined; return }
            val frameBytes = channels * 2
            val maxFrames = 8192
            var offset = 0
            while (offset < alignedSize) {
                val frames = min(maxFrames, (alignedSize - offset) / frameBytes)
                val left = FloatArray(frames)
                val right = FloatArray(frames)
                for (i in 0 until frames) {
                    val p = offset + i * frameBytes
                    val l = (joined[p].toInt() and 0xff) or (joined[p + 1].toInt() shl 8)
                    left[i] = (l.toShort().toInt() shl 16).toFloat()
                    if (channels == 2) {
                        val r = (joined[p + 2].toInt() and 0xff) or (joined[p + 3].toInt() shl 8)
                        right[i] = (r.toShort().toInt() shl 16).toFloat()
                    } else {
                        right[i] = left[i]
                    }
                }
                val buffer = ByteArray((1.25 * frames).toInt() + 8192)
                val count = encoder.encodeBuffer(left, right, frames, buffer)
                require(count >= 0) { "Lỗi mã hóa MP3 ($count)" }
                if (count > 0) stream.write(buffer, 0, count)
                offset += frames * frameBytes
            }
            remainder = if (alignedSize == joined.size) ByteArray(0) else joined.copyOfRange(alignedSize, joined.size)
        }
        override fun finish() {
            require(remainder.isEmpty()) { "Âm thanh PCM thiếu dữ liệu mẫu" }
            val buffer = ByteArray(16384)
            val count = encoder.encodeFlush(buffer)
            require(count >= 0) { "Lỗi hoàn tất MP3 ($count)" }
            if (count > 0) stream.write(buffer, 0, count)
            stream.flush()
        }
        override fun close() {
            runCatching { encoder.close() }
            stream.close()
        }
    }

    private class AacWriter(file: File, rate: Int, private val channels: Int, bitrate: Int) : PcmWriter {
        private val encoder: MediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        private val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        private var muxStarted = false
        private var track = -1
        private var queuedFrames = 0L
        private var encoderEos = false
        private val sampleRate = rate
        private val info = MediaCodec.BufferInfo()
        init {
            val codecFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, rate, channels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate * 1000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32 * 1024)
            }
            encoder.configure(codecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()
        }
        override fun write(pcm: ByteArray) {
            var offset = 0
            val frameSize = channels * 2
            require(pcm.size % frameSize == 0) { "PCM không đồng bộ frame" }
            var emptyPolls = 0
            while (offset < pcm.size) {
                var index = encoder.dequeueInputBuffer(10_000)
                if (index < 0) { drain(false); index = encoder.dequeueInputBuffer(10_000) }
                if (index < 0) {
                    check(++emptyPolls < 500) { "AAC encoder input timed out" }
                    continue
                }
                emptyPolls = 0
                val buffer = encoder.getInputBuffer(index) ?: error("AAC input buffer unavailable")
                buffer.clear()
                val len = min(pcm.size - offset, buffer.remaining()) / frameSize * frameSize
                require(len > 0) { "AAC input buffer quá nhỏ" }
                buffer.put(pcm, offset, len)
                encoder.queueInputBuffer(index, 0, len, queuedFrames * 1_000_000L / sampleRate, 0)
                queuedFrames += len / frameSize
                offset += len
                drain(false)
            }
        }
        override fun finish() {
            var polls = 0
            while (true) {
                check(++polls < 500) { "AAC encoder EOS timed out" }
                val index = encoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    encoder.queueInputBuffer(index, 0, 0, queuedFrames * 1_000_000L / sampleRate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    break
                }
                drain(false)
            }
            var attempts = 0
            while (!encoderEos && attempts++ < 400) drain(true)
            check(encoderEos) { "AAC encoder did not finish" }
        }
        private fun drain(wait: Boolean) {
            while (true) {
                val index = encoder.dequeueOutputBuffer(info, if (wait) 10_000 else 0)
                when (index) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> return
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        check(!muxStarted) { "AAC encoder changed output format twice" }
                        track = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxStarted = true
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (index >= 0) {
                        if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0) {
                            check(muxStarted) { "AAC muxer has not started" }
                            val buffer = encoder.getOutputBuffer(index) ?: error("AAC output buffer unavailable")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            muxer.writeSampleData(track, buffer, info)
                        }
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) encoderEos = true
                        encoder.releaseOutputBuffer(index, false)
                    }
                }
            }
        }
        override fun close() {
            runCatching { encoder.stop() }
            encoder.release()
            if (muxStarted) runCatching { muxer.stop() }
            muxer.release()
        }
    }


    /** Big-endian 16-bit AIFF. The existing decoder supplies little-endian 16-bit PCM. */
    private class AiffWriter(file: File, private val rate: Int, private val channels: Int) : PcmWriter {
        private val target = RandomAccessFile(file, "rw")
        private var bytes = 0L
        init {
            require(rate > 0 && channels in 1..2) { "AIFF: thông số âm thanh không hợp lệ" }
            target.setLength(0)
            target.write(ByteArray(54))
        }
        override fun write(pcm: ByteArray) {
            require(pcm.size % (channels * 2) == 0) { "AIFF: PCM frame không đầy đủ" }
            require(bytes + pcm.size < 0xFFFFFFFFL - 46L) { "AIFF vượt giới hạn 4 GB" }
            val swapped = pcm.copyOf()
            for (i in swapped.indices step 2) {
                val low = swapped[i]
                swapped[i] = swapped[i + 1]
                swapped[i + 1] = low
            }
            target.write(swapped)
            bytes += swapped.size
        }
        override fun finish() {
            val frames = bytes / (channels * 2)
            val h = ByteBuffer.allocate(54).order(ByteOrder.BIG_ENDIAN)
            h.put("FORM".toByteArray(Charsets.US_ASCII))
            h.putInt((46L + bytes).toInt())
            h.put("AIFFCOMM".toByteArray(Charsets.US_ASCII))
            h.putInt(18)
            h.putShort(channels.toShort())
            h.putInt(frames.toInt())
            h.putShort(16)
            // AIFF's 80-bit big-endian extended float representation of the sample rate.
            val log2 = 31 - Integer.numberOfLeadingZeros(rate)
            h.putShort((16383 + log2).toShort())
            h.putLong(rate.toLong() shl (63 - log2))
            h.put("SSND".toByteArray(Charsets.US_ASCII))
            h.putInt((bytes + 8L).toInt())
            h.putInt(0) // Offset
            h.putInt(0) // Block size
            target.seek(0)
            target.write(h.array())
        }
        override fun close() { target.close() }
    }

    /**
     * Streaming linear PCM resampler for the Android Opus encoder.
     * Carries the preceding sample and fractional position across decoder chunks;
     * does not reset phase for each MediaCodec output buffer.
     */
    private class Pcm16Resampler(
        private val output: PcmWriter,
        private val inputRate: Int,
        private val outputRate: Int,
        private val channels: Int
    ) : PcmWriter {
        private var totalFrames = 0L
        private var nextNumerator = 0L
        private val lastFrame = IntArray(channels)
        init { require(inputRate > 0 && outputRate > 0 && channels in 1..2) }
        override fun write(pcm: ByteArray) {
            val stride = channels * 2
            require(pcm.size % stride == 0) { "Opus: PCM frame không đầy đủ" }
            val count = pcm.size / stride
            if (count == 0) return
            val start = totalFrames
            val end = start + count
            val out = ByteArrayOutputStream((count * (outputRate.toDouble() / inputRate) * stride).toInt().coerceAtLeast(128))
            fun sample(frame: Long, channel: Int): Int {
                if (frame == start - 1) return lastFrame[channel]
                val offset = (frame - start).toInt() * stride + channel * 2
                return ((pcm[offset].toInt() and 0xFF) or (pcm[offset + 1].toInt() shl 8)).toShort().toInt()
            }
            // Keep one source frame ahead for interpolation. The last sample of a
            // chunk is saved to interpolate with the first sample of the next.
            while (nextNumerator < (end - 1) * outputRate) {
                val left = nextNumerator / outputRate
                val fraction = (nextNumerator % outputRate).toDouble() / outputRate
                for (channel in 0 until channels) {
                    val a = sample(left, channel)
                    val b = sample(left + 1, channel)
                    val value = (a + (b - a) * fraction).roundToInt().coerceIn(-32768, 32767)
                    out.write(value and 0xFF)
                    out.write((value ushr 8) and 0xFF)
                }
                nextNumerator += inputRate
            }
            for (channel in 0 until channels) lastFrame[channel] = sample(end - 1, channel)
            totalFrames = end
            if (out.size() > 0) output.write(out.toByteArray())
        }
        override fun finish() {
            if (totalFrames > 0) {
                val tail = ByteArrayOutputStream()
                while (nextNumerator < totalFrames * outputRate) {
                    for (value in lastFrame) {
                        tail.write(value and 0xFF)
                        tail.write((value ushr 8) and 0xFF)
                    }
                    nextNumerator += inputRate
                }
                if (tail.size() > 0) output.write(tail.toByteArray())
            }
            output.finish()
        }
        override fun close() { output.close() }
    }

    /** New formats supported without FFmpeg: raw ADTS AAC, native FLAC, Ogg/WebM Opus. */
    private class PlatformAudioWriter(
        file: File, private val rate: Int, private val channels: Int,
        bitrateKbps: Int, private val target: AudioTarget
    ) : PcmWriter {
        private val isOpus = target == AudioTarget.OGG || target == AudioTarget.OPUS || target == AudioTarget.WEBM
        private val isFlac = target == AudioTarget.FLAC
        private val isAac = target == AudioTarget.AAC
        private val mime = when {
            isFlac -> "audio/flac"
            isOpus -> "audio/opus"
            isAac -> MediaFormat.MIMETYPE_AUDIO_AAC
            else -> error("Codec đầu ra không hợp lệ")
        }
        private val encoder = MediaCodec.createEncoderByType(mime)
        private val muxer: MediaMuxer?
        private val stream: FileOutputStream?
        private var muxStarted = false
        private var track = -1
        private var frames = 0L
        private var ended = false
        private var wroteFlacHeader = false
        private val info = MediaCodec.BufferInfo()
        private val frequencyIndex = if (isAac) {
            listOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000,
                12000, 11025, 8000, 7350).indexOf(rate).also {
                require(it >= 0) { "AAC ADTS không hỗ trợ tần số $rate Hz" }
            }
        } else -1
        init {
            var pendingMuxer: MediaMuxer? = null
            var pendingStream: FileOutputStream? = null
            try {
                if (isOpus) {
                    require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        "Ogg/Opus cần Android 10 trở lên"
                    }
                    val kind = if (target == AudioTarget.WEBM)
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
                    else MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
                    pendingMuxer = MediaMuxer(file.absolutePath, kind)
                } else {
                    pendingStream = FileOutputStream(file)
                }
                val format = MediaFormat.createAudioFormat(mime, rate, channels).apply {
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 32 * 1024)
                    when {
                        isFlac -> setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5)
                        else -> setInteger(MediaFormat.KEY_BIT_RATE, bitrateKbps * 1000)
                    }
                    if (isAac) setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                }
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                encoder.start()
            } catch (e: Exception) {
                runCatching { encoder.stop() }
                encoder.release()
                runCatching { pendingMuxer?.release() }
                runCatching { pendingStream?.close() }
                throw e
            }
            muxer = pendingMuxer
            stream = pendingStream
        }
        override fun write(pcm: ByteArray) {
            val stride = channels * 2
            require(pcm.size % stride == 0) { "PCM không đồng bộ frame" }
            var offset = 0
            var attempts = 0
            while (offset < pcm.size) {
                var index = encoder.dequeueInputBuffer(10_000)
                if (index < 0) { drain(false); index = encoder.dequeueInputBuffer(10_000) }
                if (index < 0) {
                    check(++attempts < 500) { "Không có bộ đệm mã hóa âm thanh" }
                    continue
                }
                attempts = 0
                val buffer = encoder.getInputBuffer(index) ?: error("Thiếu bộ đệm mã hóa")
                buffer.clear()
                val size = min(pcm.size - offset, buffer.remaining()) / stride * stride
                require(size > 0) { "Bộ đệm mã hóa quá nhỏ" }
                buffer.put(pcm, offset, size)
                encoder.queueInputBuffer(index, 0, size, frames * 1_000_000L / rate, 0)
                frames += size / stride
                offset += size
                drain(false)
            }
        }
        override fun finish() {
            var polls = 0
            while (true) {
                check(++polls < 500) { "Không thể đóng luồng mã hóa" }
                val index = encoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    encoder.queueInputBuffer(index, 0, 0, frames * 1_000_000L / rate,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    break
                }
                drain(false)
            }
            var attempts = 0
            while (!ended && attempts++ < 400) drain(true)
            check(ended) { "Codec âm thanh không hoàn tất" }
            if (isFlac) check(wroteFlacHeader) { "Thiếu FLAC STREAMINFO; thiết bị không hỗ trợ xuất FLAC" }
            stream?.flush()
        }
        private fun drain(wait: Boolean) {
            while (true) {
                val index = encoder.dequeueOutputBuffer(info, if (wait) 10_000 else 0)
                when (index) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> return
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (isOpus) {
                            check(!muxStarted) { "Codec đổi format nhiều lần" }
                            track = muxer!!.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxStarted = true
                        } else if (isFlac && !wroteFlacHeader) {
                            encoder.outputFormat.getByteBuffer("csd-0")?.let { header ->
                                val data = ByteArray(header.remaining())
                                header.duplicate().get(data)
                                maybeWriteFlacHeader(data)
                            }
                        }
                    }
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> Unit
                    else -> if (index >= 0) {
                        if (info.size > 0) {
                            val buffer = encoder.getOutputBuffer(index) ?: error("Thiếu dữ liệu đầu ra")
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                if (isFlac) {
                                    val header = ByteArray(info.size)
                                    buffer.get(header)
                                    maybeWriteFlacHeader(header)
                                }
                            } else if (isOpus) {
                                check(muxStarted) { "Không tạo được container Opus" }
                                muxer!!.writeSampleData(track, buffer, info)
                            } else {
                                val data = ByteArray(info.size)
                                buffer.get(data)
                                when {
                                    isAac -> {
                                        val packetSize = data.size + 7
                                        require(packetSize < 8192) { "Khung AAC quá dài cho ADTS" }
                                        val header = byteArrayOf(
                                            0xFF.toByte(), 0xF1.toByte(),
                                            (((2 - 1) shl 6) or (frequencyIndex shl 2) or (channels shr 2)).toByte(),
                                            (((channels and 3) shl 6) or (packetSize shr 11)).toByte(),
                                            ((packetSize shr 3) and 0xFF).toByte(),
                                            (((packetSize and 7) shl 5) or 0x1F).toByte(),
                                            0xFC.toByte()
                                        )
                                        stream!!.write(header)
                                        stream!!.write(data)
                                    }
                                    isFlac -> {
                                        if (isFlacHeader(data)) {
                                            if (!wroteFlacHeader) {
                                                stream!!.write(data)
                                                wroteFlacHeader = true
                                            } // Some codecs repeat the header as a normal output buffer.
                                        } else {
                                            check(wroteFlacHeader) { "Thiếu FLAC header từ codec" }
                                            stream!!.write(data)
                                        }
                                    }
                                }
                            }
                        }
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) ended = true
                        encoder.releaseOutputBuffer(index, false)
                    }
                }
            }
        }
        private fun isFlacHeader(data: ByteArray): Boolean =
            data.size >= 4 && data[0] == 'f'.code.toByte() && data[1] == 'L'.code.toByte() &&
                data[2] == 'a'.code.toByte() && data[3] == 'C'.code.toByte()
        private fun maybeWriteFlacHeader(data: ByteArray) {
            if (!wroteFlacHeader && isFlacHeader(data)) {
                stream!!.write(data)
                wroteFlacHeader = true
            }
        }
        override fun close() {
            runCatching { encoder.stop() }
            encoder.release()
            if (muxStarted) runCatching { muxer?.stop() }
            muxer?.release()
            stream?.close()
        }
    }
}
