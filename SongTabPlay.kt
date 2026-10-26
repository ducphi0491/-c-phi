@file:OptIn(
    ExperimentalUnsignedTypes::class,
    kotlin.contracts.ExperimentalContracts::class
)

package ducphi.tunnerinstrument

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.core.graphics.PathParser
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.Canvas
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
private val SUPPORTED_GP_EXTENSIONS = setOf("gp3", "gp4", "gp5", "gpx", "gp")

// Android Storage Access Framework lọc theo MIME, không lọc trực tiếp theo đuôi file.
// Danh sách này loại ảnh, video, PDF, tài liệu... khỏi màn hình chọn file.
// application/octet-stream được giữ lại vì nhiều trình quản lý file khai báo GP3/4/5/GPX/GP
// là dữ liệu nhị phân chung; sau khi chọn vẫn kiểm tra chính xác phần mở rộng ở readLocalScoreFile().
private val SUPPORTED_GP_MIME_TYPES = arrayOf(
    "application/x-guitar-pro",
    "application/x-guitar-pro3",
    "application/x-guitar-pro4",
    "application/x-guitar-pro5",
    "application/x-guitar-pro6",
    "application/x-guitar-pro7",
    "application/vnd.guitarpro",
    "application/vnd.guitar-pro",
    "application/x-gpx",
    "application/gpx+xml",
    "audio/x-guitar-pro",
    "application/octet-stream"
)
private const val LOCAL_SCORE_MAX_BYTES = 50L * 1024L * 1024L
private const val LOCAL_SCORE_URL_PREFIX = "https://tunertools.top/__local_score__/"

private const val STANDARD_SOUNDFONT_URL =
    "https://cdn.jsdelivr.net/npm/@coderline/alphatab@1.8.4/dist/soundfont/sonivox.sf2"
private const val FLUIDR3_SOUNDFONT_FILE_NAME = "FluidR3_GM.sf2"
private const val MUSESCORE_SOUNDFONT_FILE_NAME = "MuseScore_General.sf3"
private const val LOCAL_FLUIDR3_SOUNDFONT_URL =
    "https://tunertools.top/__local_soundfont__/FluidR3_GM.sf2"
private const val LOCAL_MUSESCORE_SOUNDFONT_URL =
    "https://tunertools.top/__local_soundfont__/MuseScore_General.sf3"
private const val SOUNDFONT_PREFS_NAME = "SongTabSoundFontPrefs"
private const val SOUNDFONT_PREF_MODE = "selected_mode"
private const val SOUNDFONT_PREF_SIZE = "downloaded_size"
private const val SOUNDFONT_PREF_VERSION = "downloaded_version"
private const val SOUNDFONT_PREF_PART_VERSION = "partial_version"
private const val TAB_DISPLAY_PREFS_NAME = "SongTabDisplayPrefs"
private const val TAB_DISPLAY_PREF_NOTE_NAMES = "show_note_names"

// Thoi gian dung thu chuc nang Ban nhac duoc luu rieng theo tai khoan Google.
// Tai khoan da duoc cap quyen canView/canManage tren server van duoc su dung khong gioi han.
private const val SONG_TAB_TRIAL_PREFS_NAME = "SongTabTrialPrefs"
private const val SONG_TAB_TRIAL_DAYS = 30L
private const val SONG_TAB_TRIAL_DURATION_MS =
    SONG_TAB_TRIAL_DAYS * 24L * 60L * 60L * 1000L
private const val SONG_TAB_TRIAL_START_PREFIX = "trial_start_"
private const val SONG_TAB_TRIAL_MAX_SEEN_PREFIX = "trial_max_seen_"

private data class SongTabTrialStatus(
    val email: String = "",
    val startedAt: Long = 0L,
    val expiresAt: Long = 0L,
    val effectiveNow: Long = 0L
) {
    val isStarted: Boolean get() = email.isNotBlank() && startedAt > 0L
    val isActive: Boolean get() = isStarted && effectiveNow < expiresAt
    val isExpired: Boolean get() = isStarted && !isActive
    val remainingMillis: Long get() = (expiresAt - effectiveNow).coerceAtLeast(0L)
}

private enum class SongTabGateReason {
    DEFAULT,
    TRIAL_EXPIRED
}

private fun songTabTrialUserKey(email: String): String {
    val normalized = email.trim().lowercase(Locale.ROOT)
    if (normalized.isBlank()) return ""

    val digest = MessageDigest.getInstance("SHA-256")
        .digest(normalized.toByteArray(StandardCharsets.UTF_8))
    return digest.joinToString(separator = "") { byte ->
        "%02x".format(Locale.ROOT, byte.toInt() and 0xff)
    }.take(32)
}

private fun readSongTabTrialStatus(
    context: Context,
    email: String,
    startIfMissing: Boolean
): SongTabTrialStatus {
    val normalizedEmail = email.trim().lowercase(Locale.ROOT)
    val userKey = songTabTrialUserKey(normalizedEmail)
    if (userKey.isBlank()) return SongTabTrialStatus()

    val prefs = context.getSharedPreferences(SONG_TAB_TRIAL_PREFS_NAME, Context.MODE_PRIVATE)
    val startKey = SONG_TAB_TRIAL_START_PREFIX + userKey
    val maxSeenKey = SONG_TAB_TRIAL_MAX_SEEN_PREFIX + userKey
    val deviceNow = System.currentTimeMillis().coerceAtLeast(1L)

    var startedAt = prefs.getLong(startKey, 0L)
    if (startedAt <= 0L && startIfMissing) {
        startedAt = deviceNow
        prefs.edit().putLong(startKey, startedAt).apply()
    }

    if (startedAt <= 0L) {
        return SongTabTrialStatus(email = normalizedEmail, effectiveNow = deviceNow)
    }

    // Ghi lai moc thoi gian lon nhat da thay de viec lui dong ho may khong keo dai dung thu.
    val previousMaxSeen = prefs.getLong(maxSeenKey, startedAt)
    val effectiveNow = maxOf(deviceNow, previousMaxSeen, startedAt)
    if (effectiveNow != previousMaxSeen) {
        prefs.edit().putLong(maxSeenKey, effectiveNow).apply()
    }

    val expiresAt = if (startedAt > Long.MAX_VALUE - SONG_TAB_TRIAL_DURATION_MS) {
        Long.MAX_VALUE
    } else {
        startedAt + SONG_TAB_TRIAL_DURATION_MS
    }

    return SongTabTrialStatus(
        email = normalizedEmail,
        startedAt = startedAt,
        expiresAt = expiresAt,
        effectiveNow = effectiveNow
    )
}

private fun soundFontText(
    key: String,
    currentLang: String,
    vietnameseFallback: String,
    englishFallback: String
): String {
    val translated = runCatching {
        CoreTranslator.getString(key, currentLang)
    }.getOrDefault("")

    if (
        translated.isNotBlank() &&
        translated != key &&
        !translated.contains("missing translation", ignoreCase = true)
    ) {
        return translated
    }

    val languageCode = currentLang
        .lowercase(Locale.ROOT)
        .substringBefore('-')
        .substringBefore('_')
    return if (languageCode == "vi") vietnameseFallback else englishFallback
}

private fun soundFontFormat(
    key: String,
    currentLang: String,
    vietnameseFallback: String,
    englishFallback: String,
    vararg args: Any
): String {
    val template = soundFontText(
        key = key,
        currentLang = currentLang,
        vietnameseFallback = vietnameseFallback,
        englishFallback = englishFallback
    )
    return runCatching {
        String.format(Locale.getDefault(), template, *args)
    }.getOrElse {
        val fallback = if (
            currentLang.lowercase(Locale.ROOT).substringBefore('-').substringBefore('_') == "vi"
        ) vietnameseFallback else englishFallback
        runCatching { String.format(Locale.getDefault(), fallback, *args) }
            .getOrDefault(fallback)
    }
}

private enum class TabDisplayMode(
    val jsValue: String,
    val buttonLabel: String
) {
    SCORE_TAB(jsValue = "scoretab", buttonLabel = "S+T"),
    TAB_ONLY(jsValue = "tab", buttonLabel = "TAB"),
    PARTITION_ONLY(jsValue = "score", buttonLabel = "P");

    fun menuTitle(currentLang: String): String = when (this) {
        SCORE_TAB -> soundFontText(
            "tab_display_score_tab_title",
            currentLang,
            "Khuông + TAB",
            "Notation + TAB"
        )
        TAB_ONLY -> soundFontText(
            "tab_display_tab_title",
            currentLang,
            "TAB",
            "TAB"
        )
        PARTITION_ONLY -> soundFontText(
            "tab_display_partition_title",
            currentLang,
            "Khuông nhạc",
            "Notation"
        )
    }

    fun menuDescription(currentLang: String): String = when (this) {
        SCORE_TAB -> soundFontText(
            "tab_display_score_tab_desc",
            currentLang,
            "Hiển thị khuông nhạc và TAB cùng lúc",
            "Show notation and TAB together"
        )
        TAB_ONLY -> soundFontText(
            "tab_display_tab_desc",
            currentLang,
            "Chỉ hiển thị TAB",
            "Show TAB only"
        )
        PARTITION_ONLY -> soundFontText(
            "tab_display_partition_desc",
            currentLang,
            "Chỉ hiển thị khuông nhạc",
            "Show standard notation only"
        )
    }
}

private enum class TabSoundFontMode(
    val storedValue: String,
    val jsValue: String,
    val serverProfile: String?,
    val defaultDisplayName: String,
    val localFileName: String?,
    val localUrl: String?,
    val preferenceSuffix: String
) {
    STANDARD(
        storedValue = "standard",
        jsValue = "standard",
        serverProfile = null,
        defaultDisplayName = "Sonivox",
        localFileName = null,
        localUrl = null,
        preferenceSuffix = ""
    ),
    // Giữ storedValue/jsValue cũ để người đã tải FluidR3 ở phiên bản trước không bị mất lựa chọn.
    FLUIDR3(
        storedValue = "high_quality",
        jsValue = "high_quality",
        serverProfile = "fluidr3",
        defaultDisplayName = "FluidR3 GM",
        localFileName = FLUIDR3_SOUNDFONT_FILE_NAME,
        localUrl = LOCAL_FLUIDR3_SOUNDFONT_URL,
        preferenceSuffix = ""
    ),
    MUSESCORE(
        storedValue = "musescore_sf3",
        jsValue = "musescore_sf3",
        serverProfile = "musescore",
        defaultDisplayName = "MuseScore General",
        localFileName = MUSESCORE_SOUNDFONT_FILE_NAME,
        localUrl = LOCAL_MUSESCORE_SOUNDFONT_URL,
        preferenceSuffix = "_musescore_sf3"
    );

    val isDownloadable: Boolean
        get() = this != STANDARD

    val playbackUrl: String
        get() = when (this) {
            STANDARD -> STANDARD_SOUNDFONT_URL
            FLUIDR3 -> LOCAL_FLUIDR3_SOUNDFONT_URL
            MUSESCORE -> LOCAL_MUSESCORE_SOUNDFONT_URL
        }

    fun menuTitle(currentLang: String): String = when (this) {
        STANDARD -> soundFontText(
            "tab_sound_quality_low", currentLang,
            "Chất lượng thấp", "Low quality"
        )
        MUSESCORE -> soundFontText(
            "tab_sound_quality_medium", currentLang,
            "Chất lượng trung bình", "Medium quality"
        )
        FLUIDR3 -> soundFontText(
            "tab_sound_quality_high", currentLang,
            "Chất lượng cao", "High quality"
        )
    }

    fun menuDescription(currentLang: String): String = when (this) {
        STANDARD -> soundFontText(
            "tab_sound_no_extra_storage", currentLang,
            "Không tăng dung lượng app", "Does not increase app storage"
        )
        MUSESCORE -> ""
        FLUIDR3 -> ""
    }

    val estimatedAppIncreaseText: String
        get() = when (this) {
            STANDARD -> "0 MB"
            MUSESCORE -> "38 MB"
            FLUIDR3 -> "145 MB"
        }

    companion object {
        fun fromStoredValue(value: String?): TabSoundFontMode =
            values().firstOrNull { it.storedValue == value } ?: STANDARD

        fun fromJsValue(value: String?): TabSoundFontMode =
            values().firstOrNull { it.jsValue == value } ?: STANDARD
    }
}

private fun soundFontSizePreferenceKey(mode: TabSoundFontMode): String =
    SOUNDFONT_PREF_SIZE + mode.preferenceSuffix

private fun soundFontVersionPreferenceKey(mode: TabSoundFontMode): String =
    SOUNDFONT_PREF_VERSION + mode.preferenceSuffix

private fun soundFontPartVersionPreferenceKey(mode: TabSoundFontMode): String =
    SOUNDFONT_PREF_PART_VERSION + mode.preferenceSuffix

private data class TabSoundFontInfo(
    val mode: TabSoundFontMode,
    val displayName: String,
    val fileName: String,
    val sizeBytes: Long,
    val version: String,
    val downloadUrl: String
)

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0L) return "--"
    val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
    return if (megabytes >= 1024.0) {
        String.format(Locale.getDefault(), "%.2f GB", megabytes / 1024.0)
    } else {
        String.format(Locale.getDefault(), "%.1f MB", megabytes)
    }
}

private fun resolveSongTabApiEndpoint(apiBaseUrl: String, searchPath: String): String =
    when {
        searchPath.startsWith("http", ignoreCase = true) -> searchPath
        apiBaseUrl.trimEnd('/').endsWith(".php", ignoreCase = true) ->
            apiBaseUrl.trimEnd('/')
        else -> apiBaseUrl.trimEnd('/') + "/" + searchPath.trimStart('/')
    }

private fun appendUrlParameter(url: String, parameter: String): String =
    url + if (url.contains('?')) "&$parameter" else "?$parameter"

private fun loadTabSoundFontInfo(
    apiBaseUrl: String,
    searchPath: String,
    apiToken: String?,
    mode: TabSoundFontMode,
    currentLang: String
): TabSoundFontInfo {
    require(mode.isDownloadable) { "Chế độ tiêu chuẩn không cần tải SoundFont cục bộ." }

    val endpoint = resolveSongTabApiEndpoint(apiBaseUrl, searchPath)
    val profile = mode.serverProfile
        ?: throw IllegalStateException(
            soundFontText(
                "tab_sound_info_failed", currentLang,
                "Không thể đọc thông tin SoundFont.",
                "Could not read SoundFont information."
            )
        )
    val infoUrl = appendUrlParameter(
        appendUrlParameter(endpoint, "action=soundfont_info"),
        "profile=" + URLEncoder.encode(profile, StandardCharsets.UTF_8.name())
    )
    val connection = URL(infoUrl).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "TunerInstrument-Android")
        if (!apiToken.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $apiToken")
        }

        val responseCode = connection.responseCode
        val responseText = (if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        })?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()

        if (responseCode !in 200..299) {
            val serverMessage = runCatching {
                JSONObject(responseText).optString("error")
            }.getOrNull().orEmpty()
            throw IllegalStateException(
                serverMessage.ifBlank {
                    soundFontFormat(
                        "tab_sound_server_http", currentLang,
                        "Máy chủ phản hồi HTTP %1\$d",
                        "Server returned HTTP %1\$d",
                        responseCode
                    )
                }
            )
        }

        val root = JSONObject(responseText)
        if (!root.optBoolean("success", false)) {
            throw IllegalStateException(
                root.optString("error").ifBlank {
                    soundFontText(
                        "tab_sound_info_failed", currentLang,
                        "Không thể đọc thông tin SoundFont.",
                        "Could not read SoundFont information."
                    )
                }
            )
        }

        val data = root.optJSONObject("data")
            ?: throw IllegalStateException(
                soundFontText(
                    "tab_sound_info_missing", currentLang,
                    "Máy chủ chưa trả về thông tin SoundFont.",
                    "The server did not return SoundFont information."
                )
            )
        val expectedFileName = mode.localFileName
            ?: throw IllegalStateException(
                soundFontText(
                    "tab_sound_info_failed", currentLang,
                    "Không thể đọc thông tin SoundFont.",
                    "Could not read SoundFont information."
                )
            )
        val fileName = data.optString("file_name").ifBlank { expectedFileName }
        if (!fileName.equals(expectedFileName, ignoreCase = true)) {
            throw IllegalStateException(
                soundFontFormat(
                    "tab_sound_wrong_file", currentLang,
                    "Máy chủ trả sai file SoundFont: %1\$s (mong đợi %2\$s).",
                    "The server returned the wrong SoundFont file: %1\$s (expected %2\$s).",
                    fileName,
                    expectedFileName
                )
            )
        }
        val downloadUrl = data.optString("download_url").ifBlank {
            appendUrlParameter(
                appendUrlParameter(endpoint, "action=download_soundfont"),
                "profile=" + URLEncoder.encode(profile, StandardCharsets.UTF_8.name())
            )
        }
        val sizeBytes = data.optLong("size_bytes", 0L)
        if (sizeBytes <= 0L) {
            throw IllegalStateException(
                soundFontText(
                    "tab_sound_invalid_size", currentLang,
                    "Kích thước SoundFont trên máy chủ không hợp lệ.",
                    "The server returned an invalid SoundFont size."
                )
            )
        }

        return TabSoundFontInfo(
            mode = mode,
            displayName = data.optString("display_name").ifBlank { mode.defaultDisplayName },
            fileName = fileName,
            sizeBytes = sizeBytes,
            version = data.optString("version"),
            downloadUrl = downloadUrl
        )
    } finally {
        connection.disconnect()
    }
}

private fun soundFontDirectory(context: Context): File =
    File(context.filesDir, "soundfonts")

private fun soundFontFile(context: Context, mode: TabSoundFontMode): File {
    require(mode.isDownloadable) { "Chế độ tiêu chuẩn không có file SoundFont cục bộ." }
    return File(
        soundFontDirectory(context),
        mode.localFileName ?: throw IllegalStateException("Thiếu tên file SoundFont.")
    )
}

private fun soundFontPartFile(context: Context, mode: TabSoundFontMode): File {
    require(mode.isDownloadable) { "Chế độ tiêu chuẩn không có file SoundFont tải dở." }
    val fileName = mode.localFileName
        ?: throw IllegalStateException("Thiếu tên file SoundFont.")
    return File(soundFontDirectory(context), "$fileName.part")
}

private data class LocalScorePayload(
    val requestUrl: String,
    val bytes: ByteArray
)

private data class LocalScoreSelection(
    val displayName: String,
    val extension: String,
    val bytes: ByteArray
)

private fun readLocalScoreFile(
    context: android.content.Context,
    uri: Uri
): LocalScoreSelection {
    val resolver = context.contentResolver
    var displayName: String? = null
    var declaredSize = -1L

    resolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                displayName = cursor.getString(nameIndex)
            }

            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                declaredSize = cursor.getLong(sizeIndex)
            }
        }
    }

    val resolvedName = displayName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
        ?: "ban_nhac.gp"

    val extension = resolvedName
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase(Locale.ROOT)

    if (extension !in SUPPORTED_GP_EXTENSIONS) {
        throw IllegalArgumentException(
            "File không được hỗ trợ. Hãy chọn file GP3, GP4, GP5, GPX hoặc GP."
        )
    }

    if (declaredSize > LOCAL_SCORE_MAX_BYTES) {
        throw IllegalArgumentException("File quá lớn. Kích thước tối đa là 50 MB.")
    }

    val bytes = resolver.openInputStream(uri)?.use { input ->
        ByteArrayOutputStream().use { output ->
            val buffer = ByteArray(8 * 1024)
            var totalBytes = 0L

            while (true) {
                val readCount = input.read(buffer)
                if (readCount < 0) break
                if (readCount == 0) continue

                totalBytes += readCount
                if (totalBytes > LOCAL_SCORE_MAX_BYTES) {
                    throw IllegalArgumentException("File quá lớn. Kích thước tối đa là 50 MB.")
                }
                output.write(buffer, 0, readCount)
            }

            output.toByteArray()
        }
    } ?: throw IllegalStateException("Không thể đọc file đã chọn.")

    if (bytes.isEmpty()) {
        throw IllegalArgumentException("File đã chọn không có dữ liệu.")
    }

    return LocalScoreSelection(
        displayName = resolvedName,
        extension = extension,
        bytes = bytes
    )
}

private fun buildA4PdfJobName(rawTitle: String?): String {
    val safeTitle = rawTitle
        .orEmpty()
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("\\s+"), " ")
        .trim()
        .trim('.')
        .take(80)
        .ifBlank { "Ban_nhac" }

    return "${safeTitle}_A4.pdf"
}

private enum class TuneToolsItemType {
    FILE,
    FOLDER
}

private data class TuneToolsSong(
    val id: String,
    val title: String,
    val artist: String,
    val fileUrl: String,
    val format: String,
    val itemType: TuneToolsItemType = TuneToolsItemType.FILE,
    val rhythm: String = "",
    val subfolder: String = "",
    val relativePath: String = "",
    val folderPath: String = "",
    val parentPath: String = "",
    val librarySection: String = ""
) {
    val isFolder: Boolean
        get() = itemType == TuneToolsItemType.FOLDER
}

data class AlphaTrack(
    val index: Int,
    val name: String
)

/**
 * Ba nhóm nội dung ở màn hình Bản nhạc.
 *
 * Nhạc Việt đọc trực tiếp cây thư mục /tab/Vietnam qua section=nhac_viet.
 * Quốc tế giữ nguyên luồng tìm kiếm SQLite/JSON cũ. Điệu nhạc dùng
 * section=dieu_nhac và rhythm=<tên điệu> để lấy các file GP bên trong.
 */
private enum class MusicLibrarySection {
    NHAC_VIET,
    QUOC_TE,
    DIEU_NHAC;

    fun tabTitle(currentLang: String): String = when (this) {
        NHAC_VIET -> soundFontText(
            "tab_library_vietnamese_title", currentLang,
            "Nhạc Việt", "Vietnamese"
        )
        QUOC_TE -> soundFontText(
            "tab_library_international_title", currentLang,
            "Quốc tế", "International"
        )
        DIEU_NHAC -> soundFontText(
            "tab_library_rhythms_title", currentLang,
            "Điệu nhạc", "Rhythms"
        )
    }

    fun searchHint(currentLang: String): String = when (this) {
        NHAC_VIET -> soundFontText(
            "tab_library_search_vietnamese", currentLang,
            "Tìm bài hát Việt Nam...", "Search Vietnamese songs..."
        )
        QUOC_TE -> soundFontText(
            "tab_library_search_international", currentLang,
            "Tìm bài hát quốc tế...", "Search international songs..."
        )
        DIEU_NHAC -> soundFontText(
            "tab_library_search_rhythms", currentLang,
            "Tìm Bolero, Ballad, Disco...", "Search Bolero, Ballad, Disco..."
        )
    }

    fun emptyTitle(currentLang: String): String = when (this) {
        NHAC_VIET -> soundFontText(
            "tab_library_empty_vietnamese_title", currentLang,
            "Chưa có dữ liệu Nhạc Việt", "No Vietnamese music yet"
        )
        QUOC_TE -> soundFontText(
            "tab_library_empty_international_title", currentLang,
            "Chưa có nhạc quốc tế", "No international music yet"
        )
        DIEU_NHAC -> soundFontText(
            "tab_library_empty_rhythms_title", currentLang,
            "Chưa có điệu nhạc", "No rhythms yet"
        )
    }

    fun emptyMessage(currentLang: String): String = when (this) {
        NHAC_VIET -> soundFontText(
            "tab_library_empty_vietnamese_desc", currentLang,
            "Các thư mục và file GP trong tab/Vietnam sẽ hiển thị tại đây.",
            "Folders and Guitar Pro files from tab/Vietnam will appear here."
        )
        QUOC_TE -> soundFontText(
            "tab_library_empty_international_desc", currentLang,
            "Danh sách bài hát quốc tế sẽ hiển thị tại đây.",
            "International songs will appear here."
        )
        DIEU_NHAC -> soundFontText(
            "tab_library_empty_rhythms_desc", currentLang,
            "Các điệu Bolero, Ballad, Rumba, Disco... sẽ hiển thị tại đây.",
            "Bolero, Ballad, Rumba, Disco and other rhythms will appear here."
        )
    }
}

@Composable
private fun MusicLibraryTabs(
    selectedSection: MusicLibrarySection,
    currentLang: String,
    isDark: Boolean,
    accentColor: Color,
    onSectionSelected: (MusicLibrarySection) -> Unit
) {
    val outerShape = RoundedCornerShape(13.dp)
    val itemShape = RoundedCornerShape(10.dp)
    val outerColor = if (isDark) Color(0xFF111111) else Color.White
    val outerBorder = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .height(44.dp)
            .clip(outerShape)
            .background(outerColor)
            .border(1.dp, outerBorder, outerShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicLibrarySection.values().forEach { section ->
            val selected = section == selectedSection
            val itemBackground = when {
                selected && isDark -> accentColor.copy(alpha = 0.24f)
                selected -> accentColor
                else -> Color.Transparent
            }
            val itemTextColor = when {
                selected -> Color.White
                isDark -> Color.White.copy(alpha = 0.68f)
                else -> Color(0xFF616161)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(itemShape)
                    .background(itemBackground)
                    .then(
                        if (selected && isDark) {
                            Modifier.border(
                                1.dp,
                                accentColor.copy(alpha = 0.75f),
                                itemShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSectionSelected(section) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = section.tabTitle(currentLang),
                    color = itemTextColor,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SongTabGoogleGIcon(modifier: Modifier = Modifier) {
    // Google multicolour G drawn directly from vector path data.
    // No PNG/drawable resource is required.
    val bluePath = remember {
        PathParser.createPathFromPathData(
            "M23.49 12.27c0-.79-.07-1.54-.19-2.27H12v4.51h6.47a5.55 5.55 0 0 1-2.4 3.54v2.95h3.88c2.27-2.09 3.54-5.17 3.54-8.73z"
        )!!.asComposePath()
    }
    val greenPath = remember {
        PathParser.createPathFromPathData(
            "M12 24c3.24 0 5.95-1.07 7.94-2.9l-3.88-2.95c-1.08.72-2.46 1.15-4.06 1.15-3.13 0-5.78-2.11-6.73-4.95H1.27v3.04A12 12 0 0 0 12 24z"
        )!!.asComposePath()
    }
    val yellowPath = remember {
        PathParser.createPathFromPathData(
            "M5.27 14.35A7.2 7.2 0 0 1 4.9 12c0-.82.14-1.61.37-2.35V6.61H1.27A12 12 0 0 0 0 12c0 1.93.46 3.76 1.27 5.39l4-3.04z"
        )!!.asComposePath()
    }
    val redPath = remember {
        PathParser.createPathFromPathData(
            "M12 4.7c1.77 0 3.35.61 4.6 1.8l3.44-3.44A11.54 11.54 0 0 0 12 0 12 12 0 0 0 1.27 6.61l4 3.04C6.22 6.81 8.87 4.7 12 4.7z"
        )!!.asComposePath()
    }

    Canvas(modifier = modifier) {
        val targetSize = size.minDimension
        val scaleFactor = targetSize / 24f
        val offsetX = (size.width - targetSize) / 2f
        val offsetY = (size.height - targetSize) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
        }) {
            drawPath(redPath, Color(0xFFEA4335))
            drawPath(yellowPath, Color(0xFFFBBC05))
            drawPath(greenPath, Color(0xFF34A853))
            drawPath(bluePath, Color(0xFF4285F4))
        }
    }
}

@Composable
private fun SongTabGmailBrandIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val originX = (size.width - diameter) / 2f
        val originY = (size.height - diameter) / 2f

        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8CB8F8),
                    Color(0xFF4C82D8),
                    Color(0xFF655FD3)
                ),
                start = Offset(originX, originY),
                end = Offset(originX + diameter, originY + diameter)
            ),
            topLeft = Offset(originX, originY),
            size = Size(diameter, diameter),
            cornerRadius = CornerRadius(diameter * 0.25f, diameter * 0.25f)
        )

        val tileInset = diameter * 0.13f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(originX + tileInset, originY + tileInset),
            size = Size(diameter - tileInset * 2f, diameter - tileInset * 2f),
            cornerRadius = CornerRadius(diameter * 0.18f, diameter * 0.18f)
        )

        val leftX = originX + diameter * 0.31f
        val centerX = originX + diameter * 0.50f
        val rightX = originX + diameter * 0.69f
        val topY = originY + diameter * 0.36f
        val valleyY = originY + diameter * 0.55f
        val bottomY = originY + diameter * 0.70f
        val markWidth = diameter * 0.105f

        drawLine(Color(0xFFEA4335), Offset(leftX, bottomY), Offset(leftX, topY), markWidth, StrokeCap.Round)
        drawLine(Color(0xFFEA4335), Offset(leftX, topY), Offset(centerX, valleyY), markWidth, StrokeCap.Round)

        val diagonalOrange = Offset(
            centerX + (rightX - centerX) * 0.58f,
            valleyY + (topY - valleyY) * 0.58f
        )
        drawLine(Color(0xFFFF4B3E), Offset(centerX, valleyY), diagonalOrange, markWidth, StrokeCap.Round)
        drawLine(Color(0xFFFFB300), diagonalOrange, Offset(rightX, topY), markWidth, StrokeCap.Round)

        val greenStartY = topY + (bottomY - topY) * 0.23f
        val blueStartY = topY + (bottomY - topY) * 0.60f
        drawLine(Color(0xFFFFC107), Offset(rightX, topY), Offset(rightX, greenStartY), markWidth, StrokeCap.Round)
        drawLine(Color(0xFF20B86A), Offset(rightX, greenStartY), Offset(rightX, blueStartY), markWidth, StrokeCap.Round)
        drawLine(Color(0xFF4285F4), Offset(rightX, blueStartY), Offset(rightX, bottomY), markWidth, StrokeCap.Round)
    }
}

@Composable
private fun SongTabZaloBrandIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val originX = (size.width - diameter) / 2f
        val originY = (size.height - diameter) / 2f

        drawRoundRect(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2497FF),
                    Color(0xFF1469EE),
                    Color(0xFF6753D8)
                ),
                start = Offset(originX, originY),
                end = Offset(originX + diameter, originY + diameter)
            ),
            topLeft = Offset(originX, originY),
            size = Size(diameter, diameter),
            cornerRadius = CornerRadius(diameter * 0.25f, diameter * 0.25f)
        )

        val bubbleLeft = originX + diameter * 0.16f
        val bubbleTop = originY + diameter * 0.17f
        val bubbleWidth = diameter * 0.68f
        val bubbleHeight = diameter * 0.60f
        val bubbleCorner = CornerRadius(diameter * 0.18f, diameter * 0.18f)

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(bubbleLeft, bubbleTop),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = bubbleCorner
        )
        val bubbleTail = androidx.compose.ui.graphics.Path().apply {
            moveTo(bubbleLeft + diameter * 0.17f, bubbleTop + bubbleHeight - diameter * 0.02f)
            lineTo(bubbleLeft + diameter * 0.11f, bubbleTop + bubbleHeight + diameter * 0.13f)
            lineTo(bubbleLeft + diameter * 0.31f, bubbleTop + bubbleHeight - diameter * 0.015f)
            close()
        }
        drawPath(path = bubbleTail, color = Color.White)

        val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color(0xFF0868F2).toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = diameter * 0.245f
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD
            )
        }
        val metrics = textPaint.fontMetrics
        val textCenterY = bubbleTop + bubbleHeight * 0.50f
        val baseline = textCenterY - (metrics.ascent + metrics.descent) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            "Zalo",
            originX + diameter * 0.50f,
            baseline,
            textPaint
        )
    }
}

@Composable
private fun SongTabContactActionRow(
    textColor: Color,
    label: String,
    value: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        color = textColor.copy(alpha = 0.045f),
        shape = RoundedCornerShape(11.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.13f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(textColor.copy(alpha = 0.07f)),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = textColor.copy(alpha = 0.56f),
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
                Text(
                    text = value,
                    color = textColor,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.42f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun SongTabLoadingWave(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF15B27D)
) {
    val transition = rememberInfiniteTransition(label = "song_tab_loading_wave")
    val maxHeights = listOf(0.46f, 0.72f, 1.0f, 0.72f, 0.46f)

    Row(
        modifier = modifier.height(46.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        maxHeights.forEachIndexed { index, maxHeight ->
            val heightFactor by transition.animateFloat(
                initialValue = 0.18f,
                targetValue = maxHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 430,
                        delayMillis = index * 85,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "song_tab_loading_wave_$index"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height((46f * heightFactor).dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun SongTabAccessLoadingScreen(
    isDark: Boolean
) {
    val bgColor = if (isDark) Color.Black else Color(0xFFF5F6FA)
    val accentColor = Color(0xFF15B27D)

    // Một loader duy nhất dùng xuyên suốt cả giai đoạn kiểm tra quyền và tải
    // danh sách ban đầu. Không hiển thị tiêu đề hoặc nút Back khi đang tải.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SongTabLoadingWave(color = accentColor)
        }
    }
}

@Composable
private fun SongTabAccessGateScreen(
    isDark: Boolean,
    state: DocumentAccessState,
    currentLang: String,
    gateReason: SongTabGateReason = SongTabGateReason.DEFAULT,
    onClose: () -> Unit,
    onLogin: () -> Unit,
    onRetry: () -> Unit
) {
    val bgColor = if (isDark) Color.Black else Color(0xFFF5F6FA)
    val surfaceColor = if (isDark) Color(0xFF121212) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF202124)
    val accentColor = Color(0xFF15B27D)
    val context = LocalContext.current
    val supportEmail = "tunertools.top@gmail.com"
    val supportZaloPhone = "0971832146"
    val isTrialExpired = gateReason == SongTabGateReason.TRIAL_EXPIRED

    fun accessText(
        key: String,
        vietnameseFallback: String,
        englishFallback: String
    ): String = soundFontText(
        key = key,
        currentLang = currentLang,
        vietnameseFallback = vietnameseFallback,
        englishFallback = englishFallback
    )

    fun launchContactIntent(primary: Intent, fallback: Intent) {
        val opened = runCatching {
            context.startActivity(primary)
            true
        }.getOrDefault(false)

        if (!opened) {
            runCatching { context.startActivity(fallback) }
                .onFailure {
                    Toast.makeText(
                        context,
                        accessText(
                            "tab_access_contact_open_error",
                            "Không tìm thấy ứng dụng phù hợp để mở liên hệ.",
                            "No suitable app was found to open this contact."
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun requestingEmail(): String = when (state) {
        is DocumentAccessState.Denied -> state.email
        is DocumentAccessState.Granted -> state.info.email
        else -> GoogleSignIn.getLastSignedInAccount(context)?.email.orEmpty()
    }

    fun openSupportEmail() {
        val accountEmail = requestingEmail()
        val subject = if (isTrialExpired) {
            accessText(
                "tab_trial_contact_subject",
                "Yêu cầu gia hạn chức năng Bản nhạc TunerTools",
                "Request to extend TunerTools Sheet Music access"
            )
        } else {
            accessText(
                "tab_access_contact_subject",
                "Yêu cầu cấp quyền Bản nhạc TunerTools",
                "Request TunerTools Sheet Music access"
            )
        }
        val body = buildString {
            append(
                if (isTrialExpired) {
                    accessText(
                        "tab_trial_contact_body",
                        "Xin chào TunerTools, thời gian dùng thử 30 ngày của tôi đã hết. Vui lòng hỗ trợ gia hạn hoặc cấp quyền sử dụng chức năng Bản nhạc",
                        "Hello TunerTools, my 30-day trial has expired. Please help extend or grant access to the Sheet Music feature"
                    )
                } else {
                    accessText(
                        "tab_access_contact_body",
                        "Xin chào TunerTools, vui lòng hỗ trợ cấp quyền sử dụng chức năng Bản nhạc",
                        "Hello TunerTools, please help grant access to the Sheet Music feature"
                    )
                }
            )
            if (accountEmail.isNotBlank()) {
                append(
                    accessText(
                        "tab_access_for_account",
                        " cho tài khoản: ",
                        " for account: "
                    )
                )
                append(accountEmail)
            }
            append(".")
        }
        val mailUri = Uri.parse(
            "mailto:$supportEmail?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val fallback = Intent(Intent.ACTION_SENDTO, mailUri)
        val gmail = Intent(fallback).apply { setPackage("com.google.android.gm") }
        launchContactIntent(gmail, fallback)
    }

    fun openSupportZalo() {
        val zaloUri = Uri.parse("https://zalo.me/$supportZaloPhone")
        val fallback = Intent(Intent.ACTION_VIEW, zaloUri)
        val zalo = Intent(fallback).apply { setPackage("com.zing.zalo") }
        launchContactIntent(zalo, fallback)
    }

    val title = if (isTrialExpired) {
        accessText(
            "tab_trial_expired_title",
            "Thời gian dùng thử đã hết",
            "Your trial has expired"
        )
    } else {
        when (state) {
            DocumentAccessState.Checking -> accessText(
                "tab_access_checking_title",
                "Đang kiểm tra quyền...",
                "Checking access..."
            )
            is DocumentAccessState.LoginRequired -> accessText(
                "tab_access_login_title",
                "Cần đăng nhập Google",
                "Google sign-in required"
            )
            is DocumentAccessState.Denied -> accessText(
                "tab_access_denied_title",
                "Tài khoản chưa được cấp quyền",
                "Account has not been granted access"
            )
            is DocumentAccessState.Error -> accessText(
                "tab_access_error_title",
                "Không thể tải dữ liệu",
                "Unable to load data"
            )
            is DocumentAccessState.Granted -> ""
        }
    }

    val message = if (isTrialExpired) {
        buildString {
            val email = requestingEmail()
            if (email.isNotBlank()) {
                append(email)
                append("\n")
            }
            append(
                accessText(
                    "tab_trial_expired_message",
                    "Bạn đã sử dụng đủ 30 ngày miễn phí của chức năng Bản nhạc. Vui lòng liên hệ qua Gmail hoặc Zalo để được hỗ trợ gia hạn hoặc cấp quyền.",
                    "You have completed the 30-day free Sheet Music trial. Contact us by Gmail or Zalo to extend or request access."
                )
            )
        }
    } else {
        when (state) {
            DocumentAccessState.Checking -> accessText(
                "tab_access_checking_message",
                "Vui lòng chờ trong giây lát.",
                "Please wait a moment."
            )
            is DocumentAccessState.LoginRequired -> state.message.ifBlank {
                accessText(
                    "tab_trial_login_message",
                    "Đăng nhập Google để bắt đầu 30 ngày sử dụng miễn phí chức năng Bản nhạc.",
                    "Sign in with Google to start a 30-day free Sheet Music trial."
                )
            }
            is DocumentAccessState.Denied -> buildString {
                if (state.email.isNotBlank()) append(state.email)
                if (state.email.isNotBlank() && state.message.isNotBlank()) append("\n")
                append(
                    state.message.ifBlank {
                        accessText(
                            "tab_access_denied_message",
                            "Gmail này chưa được cấp quyền sử dụng chức năng Bản nhạc.",
                            "This Gmail account has not been granted access to Sheet Music."
                        )
                    }
                )
            }
            is DocumentAccessState.Error -> state.message.ifBlank {
                accessText(
                    "tab_access_error_message",
                    "Không thể kết nối máy chủ hoặc tải dữ liệu.",
                    "Unable to connect to the server or load data."
                )
            }
            is DocumentAccessState.Granted -> ""
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = accessText(
                            "tab_access_back",
                            "Quay lại",
                            "Back"
                        ),
                        tint = textColor
                    )
                }
                Text(
                    text = accessText(
                        "tab_access_screen_title",
                        "Bản nhạc",
                        "Sheet Music"
                    ),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = when {
                        isTrialExpired -> Icons.Default.Lock
                        state == DocumentAccessState.Checking -> Icons.Default.Refresh
                        state is DocumentAccessState.LoginRequired -> Icons.Default.Login
                        state is DocumentAccessState.Denied -> Icons.Default.Lock
                        state is DocumentAccessState.Error -> Icons.Default.Warning
                        else -> Icons.Default.Lock
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (state is DocumentAccessState.Error && !isTrialExpired) {
                            Color(0xFFEF5350)
                        } else {
                            textColor.copy(alpha = 0.82f)
                        },
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = title,
                        color = textColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        text = message,
                        color = Color.Gray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )

                    if (state is DocumentAccessState.Denied || isTrialExpired) {
                        Spacer(modifier = Modifier.height(14.dp))
                        SongTabContactActionRow(
                            textColor = textColor,
                            label = "Gmail",
                            value = supportEmail,
                            onClick = { openSupportEmail() }
                        ) {
                            SongTabGmailBrandIcon(modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SongTabContactActionRow(
                            textColor = textColor,
                            label = "Zalo",
                            value = supportZaloPhone,
                            onClick = { openSupportZalo() }
                        ) {
                            SongTabZaloBrandIcon(modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    when {
                        isTrialExpired -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onLogin,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF202124)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFDADCE0)
                                    )
                                ) {
                                    SongTabGoogleGIcon(modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        accessText(
                                            "tab_access_switch_account",
                                            "Đổi tài khoản",
                                            "Switch account"
                                        ),
                                        color = Color(0xFF202124),
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text(
                                        accessText(
                                            "tab_access_retry",
                                            "Thử lại",
                                            "Retry"
                                        ),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        state == DocumentAccessState.Checking -> {
                            CircularProgressIndicator(
                                color = accentColor,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        state is DocumentAccessState.LoginRequired -> {
                            Button(
                                onClick = onLogin,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF202124)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0))
                            ) {
                                SongTabGoogleGIcon(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(9.dp))
                                Text(
                                    text = accessText(
                                        "tab_access_login_button",
                                        "Đăng nhập bằng Google",
                                        "Sign in with Google"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        state is DocumentAccessState.Denied -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onLogin,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF202124)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFDADCE0)
                                    )
                                ) {
                                    SongTabGoogleGIcon(modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        accessText(
                                            "tab_access_switch_account",
                                            "Đổi tài khoản",
                                            "Switch account"
                                        ),
                                        color = Color(0xFF202124),
                                        maxLines = 1,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                ) {
                                    Text(
                                        accessText(
                                            "tab_access_retry",
                                            "Thử lại",
                                            "Retry"
                                        ),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        state is DocumentAccessState.Error -> {
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    accessText(
                                        "tab_access_retry",
                                        "Thử lại",
                                        "Retry"
                                    ),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BangNhacScreen(
    apiBaseUrl: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    searchPath: String = "api_gp_search.php",
    apiToken: String? = null,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentLang = context
        .getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE)
        .getString("appLang", "en")
        ?.takeIf { it.isNotBlank() }
        ?: "en"
    val scope = rememberCoroutineScope()
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, buildTunerGoogleSignInOptions(context))
    }

    var accessState by remember {
        mutableStateOf<DocumentAccessState>(DocumentAccessState.Checking)
    }
    var accessRefresh by remember { mutableIntStateOf(0) }

    var signedInEmail by remember {
        mutableStateOf(
            GoogleSignIn.getLastSignedInAccount(context)
                ?.email
                .orEmpty()
                .trim()
                .lowercase(Locale.ROOT)
        )
    }
    var trialStatus by remember(signedInEmail) {
        mutableStateOf(
            readSongTabTrialStatus(
                context = context,
                email = signedInEmail,
                startIfMissing = signedInEmail.isNotBlank()
            )
        )
    }

    // Danh sách Nhạc Việt ban đầu được tải tại lớp ngoài. Nhờ vậy cùng một
    // SongTabAccessLoadingScreen được giữ nguyên vị trí từ lúc kiểm tra quyền
    // cho tới khi dữ liệu đầu tiên thật sự sẵn sàng.
    var initialSongs by remember(apiBaseUrl, searchPath, apiToken) {
        mutableStateOf<List<TuneToolsSong>?>(null)
    }
    var initialLoadError by remember(apiBaseUrl, searchPath, apiToken) {
        mutableStateOf<String?>(null)
    }
    var initialSearchSource by remember(apiBaseUrl, searchPath, apiToken) {
        mutableStateOf(SheetMusicServerSource.PRIMARY)
    }
    var initialLoadRefresh by remember { mutableIntStateOf(0) }

    fun resetInitialLoad() {
        initialSongs = null
        initialLoadError = null
        initialSearchSource = SheetMusicServerSource.PRIMARY
    }

    fun refreshAccountTrial(
        email: String,
        startIfMissing: Boolean
    ) {
        val normalized = email.trim().lowercase(Locale.ROOT)
        signedInEmail = normalized
        trialStatus = readSongTabTrialStatus(
            context = context,
            email = normalized,
            startIfMissing = startIfMissing && normalized.isNotBlank()
        )
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                resetInitialLoad()
                accessState = DocumentAccessState.Checking
                val account = try {
                    GoogleSignIn.getSignedInAccountFromIntent(result.data)
                        .getResult(ApiException::class.java)
                } catch (_: Exception) {
                    null
                }

                val email = account?.email.orEmpty()
                if (account == null || email.isBlank()) {
                    refreshAccountTrial("", startIfMissing = false)
                    accessState = DocumentAccessState.LoginRequired(
                        message = soundFontText(
                            "tab_trial_login_message",
                            currentLang,
                            "Đăng nhập Google để bắt đầu 30 ngày sử dụng miễn phí chức năng Bản nhạc.",
                            "Sign in with Google to start a 30-day free Sheet Music trial."
                        )
                    )
                } else {
                    // Moc 30 ngay chi duoc tao sau khi Google tra ve dang nhap thanh cong.
                    refreshAccountTrial(email, startIfMissing = true)
                    accessState = try {
                        DocumentAccessApi.loginWithAccount(context, account, DocumentAccessScope.SONG_TAB)
                    } catch (error: Exception) {
                        // Quyen may chu co the tam thoi khong doc duoc, nhung tai khoan van
                        // duoc dung thu theo moc thoi gian da luu tren thiet bi.
                        DocumentAccessState.Error(
                            error.message ?: soundFontText(
                                "tab_access_error_message",
                                currentLang,
                                "Không thể kết nối máy chủ hoặc tải dữ liệu.",
                                "Unable to connect to the server or load data."
                            )
                        )
                    }
                }
            }
        } else {
            resetInitialLoad()
            refreshAccountTrial("", startIfMissing = false)
            accessState = DocumentAccessState.LoginRequired(
                message = soundFontText(
                    "tab_trial_login_message",
                    currentLang,
                    "Đăng nhập Google để bắt đầu 30 ngày sử dụng miễn phí chức năng Bản nhạc.",
                    "Sign in with Google to start a 30-day free Sheet Music trial."
                )
            )
        }
    }

    fun switchGoogleAccount() {
        resetInitialLoad()
        accessState = DocumentAccessState.Checking
        DocumentAccessStore.clear(context)
        // Đăng xuất trước để Google không tự động dùng lại Gmail vừa bị từ chối hoặc đã hết hạn.
        googleSignInClient.signOut().addOnCompleteListener {
            refreshAccountTrial("", startIfMissing = false)
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    LaunchedEffect(accessRefresh) {
        resetInitialLoad()
        accessState = DocumentAccessState.Checking

        val account = GoogleSignIn.getLastSignedInAccount(context)
        val email = account?.email.orEmpty()
        if (email.isNotBlank()) {
            // Tai khoan da dang nhap tu phien ban truoc se bat dau 30 ngay khi mo Ban nhac lan dau.
            refreshAccountTrial(email, startIfMissing = true)
        } else {
            refreshAccountTrial("", startIfMissing = false)
        }

        accessState = try {
            DocumentAccessApi.ensureSession(context, DocumentAccessScope.SONG_TAB)
        } catch (error: Exception) {
            DocumentAccessState.Error(
                error.message ?: soundFontText(
                    "tab_access_error_message",
                    currentLang,
                    "Không thể kết nối máy chủ hoặc tải dữ liệu.",
                    "Unable to connect to the server or load data."
                )
            )
        }
    }

    // Làm mới mốc thời gian trong lúc người dùng đang đứng tại màn hình này để
    // trạng thái hết hạn có hiệu lực mà không cần đóng/mở lại ứng dụng.
    LaunchedEffect(signedInEmail, trialStatus.startedAt) {
        while (signedInEmail.isNotBlank() && trialStatus.startedAt > 0L) {
            delay(60_000L)
            trialStatus = readSongTabTrialStatus(
                context = context,
                email = signedInEmail,
                startIfMissing = false
            )
        }
    }

    val grantedState = accessState as? DocumentAccessState.Granted
    val hasPermanentViewPermission = grantedState?.info?.permissions?.let {
        it.canView || it.canManage
    } == true

    val effectiveEmail = grantedState?.info?.email
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotBlank() }
        ?: signedInEmail
    val isSignedIn = effectiveEmail.isNotBlank()
    val trialBelongsToCurrentAccount = trialStatus.email.equals(effectiveEmail, ignoreCase = true)
    val hasActiveTrial = isSignedIn && trialBelongsToCurrentAccount && trialStatus.isActive
    val hasExpiredTrial = isSignedIn && trialBelongsToCurrentAccount && trialStatus.isExpired
    val canUseSheetMusic = hasPermanentViewPermission || hasActiveTrial
    val showTrialExpired = hasExpiredTrial && !hasPermanentViewPermission

    // Dùng thử và quyền vĩnh viễn cùng dùng chung luồng tải dữ liệu.
    LaunchedEffect(
        effectiveEmail,
        canUseSheetMusic,
        initialLoadRefresh,
        apiBaseUrl,
        searchPath,
        apiToken
    ) {
        if (!isSignedIn || !canUseSheetMusic) return@LaunchedEffect

        initialSongs = null
        initialLoadError = null
        try {
            val initialOutcome = withContext(Dispatchers.IO) {
                TuneToolsSheetMusicApi.search(
                    apiBaseUrl = apiBaseUrl,
                    searchPath = searchPath,
                    keyword = "",
                    apiToken = apiToken,
                    section = MusicLibrarySection.NHAC_VIET,
                    rhythm = null,
                    folder = null
                )
            }
            initialSongs = initialOutcome.songs
            initialSearchSource = initialOutcome.source
        } catch (error: Exception) {
            initialLoadError = "Lỗi tải dữ liệu: ${error.message ?: "Không xác định"}"
        }
    }

    val showUnifiedLoading =
        accessState is DocumentAccessState.Checking ||
                (isSignedIn && !trialStatus.isStarted && !hasPermanentViewPermission) ||
                (canUseSheetMusic && initialSongs == null && initialLoadError == null)

    when {
        // Cùng một nhánh Composable được giữ trong cả hai giai đoạn nên sóng âm
        // không đổi vị trí, không chớp tiêu đề và không xuất hiện loader thứ hai.
        showUnifiedLoading -> SongTabAccessLoadingScreen(isDark = isDark)

        showTrialExpired -> SongTabAccessGateScreen(
            isDark = isDark,
            state = DocumentAccessState.Denied(
                email = effectiveEmail,
                message = ""
            ),
            currentLang = currentLang,
            gateReason = SongTabGateReason.TRIAL_EXPIRED,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = { accessRefresh++ }
        )

        canUseSheetMusic && initialLoadError != null -> SongTabAccessGateScreen(
            isDark = isDark,
            state = DocumentAccessState.Error(initialLoadError.orEmpty()),
            currentLang = currentLang,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = {
                initialLoadError = null
                initialSongs = null
                initialLoadRefresh++
            }
        )

        canUseSheetMusic && initialSongs != null -> BangNhacScreenContent(
            apiBaseUrl = apiBaseUrl,
            isDark = isDark,
            modifier = modifier,
            searchPath = searchPath,
            apiToken = apiToken,
            currentLang = currentLang,
            initialSongs = initialSongs.orEmpty(),
            initialSearchSource = initialSearchSource,
            onClose = onClose
        )

        !isSignedIn -> SongTabAccessGateScreen(
            isDark = isDark,
            state = if (accessState is DocumentAccessState.LoginRequired) {
                accessState
            } else {
                DocumentAccessState.LoginRequired(
                    message = soundFontText(
                        "tab_trial_login_message",
                        currentLang,
                        "Đăng nhập Google để bắt đầu 30 ngày sử dụng miễn phí chức năng Bản nhạc.",
                        "Sign in with Google to start a 30-day free Sheet Music trial."
                    )
                )
            },
            currentLang = currentLang,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = { accessRefresh++ }
        )

        else -> SongTabAccessGateScreen(
            isDark = isDark,
            state = DocumentAccessState.Denied(
                email = effectiveEmail,
                message = soundFontText(
                    "tab_access_denied_message",
                    currentLang,
                    "Tài khoản này chưa có quyền xem nội dung.",
                    "This account does not have permission to view this content."
                )
            ),
            currentLang = currentLang,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = { accessRefresh++ }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BangNhacScreenContent(
    apiBaseUrl: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    searchPath: String = "api_gp_search.php",
    apiToken: String? = null,
    currentLang: String,
    initialSongs: List<TuneToolsSong>,
    initialSearchSource: SheetMusicServerSource,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope = rememberCoroutineScope()
    val localScorePayload = remember {
        AtomicReference<LocalScorePayload?>(null)
    }
    val pdfPrintStarted = remember { AtomicBoolean(false) }
    val pdfJobName = remember { AtomicReference("Ban_nhac_A4.pdf") }

    // Ba cấu hình âm thanh TAB theo thứ tự chất lượng người dùng đánh giá:
    // 1) Chất lượng thấp: Sonivox tải từ CDN, không chiếm thêm bộ nhớ lâu dài.
    // 2) Chất lượng trung bình: MuseScore_General.sf3 tải một lần và có thể xóa riêng.
    // 3) Chất lượng cao: FluidR3_GM.sf2 tải một lần và có thể xóa riêng.
    val soundFontPrefs = remember(context) {
        context.getSharedPreferences(SOUNDFONT_PREFS_NAME, Context.MODE_PRIVATE)
    }
    val tabDisplayPrefs = remember(context) {
        context.getSharedPreferences(TAB_DISPLAY_PREFS_NAME, Context.MODE_PRIVATE)
    }
    val downloadableSoundFontModes = remember {
        // Thứ tự hiển thị: SF3 trung bình trước, SF2 cao nhất sau.
        listOf(TabSoundFontMode.MUSESCORE, TabSoundFontMode.FLUIDR3)
    }
    val soundFontFiles = remember(context) {
        downloadableSoundFontModes.associateWith { soundFontFile(context, it) }
    }
    val soundFontPartFiles = remember(context) {
        downloadableSoundFontModes.associateWith { soundFontPartFile(context, it) }
    }
    val startupReadySoundFonts = remember(context) {
        downloadableSoundFontModes.filterTo(mutableSetOf()) { mode ->
            val file = soundFontFiles.getValue(mode)
            val savedSize = soundFontPrefs.getLong(soundFontSizePreferenceKey(mode), 0L)
            file.isFile &&
                    file.length() > 1024L * 1024L &&
                    (savedSize <= 0L || file.length() == savedSize)
        }
    }
    val startupSoundFontMode = remember(context) {
        val savedMode = TabSoundFontMode.fromStoredValue(
            soundFontPrefs.getString(SOUNDFONT_PREF_MODE, TabSoundFontMode.STANDARD.storedValue)
        )
        if (!savedMode.isDownloadable || savedMode in startupReadySoundFonts) {
            savedMode
        } else {
            TabSoundFontMode.STANDARD
        }
    }
    val localSoundFontFileRefs = remember {
        downloadableSoundFontModes.associateWith { mode ->
            AtomicReference<File?>(
                soundFontFiles.getValue(mode).takeIf { mode in startupReadySoundFonts }
            )
        }
    }

    val soundFontInfos = remember { mutableStateMapOf<TabSoundFontMode, TabSoundFontInfo>() }
    val soundFontInfoErrors = remember { mutableStateMapOf<TabSoundFontMode, String>() }
    val downloadedSoundFontModes = remember {
        mutableStateMapOf<TabSoundFontMode, Boolean>().apply {
            downloadableSoundFontModes.forEach { put(it, it in startupReadySoundFonts) }
        }
    }
    val localSoundFontDataModes = remember {
        mutableStateMapOf<TabSoundFontMode, Boolean>().apply {
            downloadableSoundFontModes.forEach { mode ->
                put(
                    mode,
                    soundFontFiles.getValue(mode).exists() ||
                            soundFontPartFiles.getValue(mode).exists()
                )
            }
        }
    }
    val soundFontDownloadedBytesByMode = remember {
        mutableStateMapOf<TabSoundFontMode, Long>().apply {
            downloadableSoundFontModes.forEach { mode ->
                put(mode, soundFontPartFiles.getValue(mode).takeIf { it.isFile }?.length() ?: 0L)
            }
        }
    }
    val soundFontDownloadProgressByMode = remember {
        mutableStateMapOf<TabSoundFontMode, Float>().apply {
            downloadableSoundFontModes.forEach { put(it, 0f) }
        }
    }

    var selectedSoundFontMode by remember { mutableStateOf(startupSoundFontMode) }
    var activeSoundFontMode by remember { mutableStateOf(startupSoundFontMode) }
    var soundFontMenuExpanded by remember { mutableStateOf(false) }
    var downloadingSoundFontMode by remember { mutableStateOf<TabSoundFontMode?>(null) }
    var isSwitchingSoundFont by remember { mutableStateOf(false) }
    var isCheckingSoundFontCompatibility by remember { mutableStateOf(false) }
    var soundFontDecodeProgress by remember { mutableFloatStateOf(0f) }
    var soundFontFallbackRequest by remember { mutableIntStateOf(0) }
    var soundFontRestoreRequest by remember { mutableIntStateOf(0) }
    var temporarySoundFontFallbackFrom by remember {
        mutableStateOf<TabSoundFontMode?>(null)
    }

    val startupSoundFontUrl = startupSoundFontMode.playbackUrl

    // Quản lý trạng thái Sáng/Tối cục bộ
    var localIsDark by remember(isDark) { mutableStateOf(isDark) }

    // Quản lý trạng thái Xoay màn hình
    var isLandscape by remember {
        mutableStateOf(activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    }

    val bgColor = if (localIsDark) Color.Black else Color(0xFFF5F6FA)
    val surfaceColor = if (localIsDark) Color.Black else Color.White
    val textColor = if (localIsDark) Color.White else Color(0xFF2D3436)
    val accentColor = if (localIsDark) Color(0xFF9FA8DA) else Color(0xFF5E35B1)

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val isTabJsReady = remember { AtomicBoolean(false) }
    val pendingTabSongScript = remember { AtomicReference<String?>(null) }

    fun dispatchPendingTabSong(target: WebView) {
        pendingTabSongScript.getAndSet(null)?.let { command ->
            target.evaluateJavascript(command, null)
        }
    }

    fun syncTabViewport(target: WebView) {
        target.post {
            val viewportWidth = target.width
            val viewportHeight = target.height
            if (viewportWidth > 0 && viewportHeight > 0) {
                target.evaluateJavascript(
                    "javascript:if(typeof onAndroidViewportChanged==='function'){" +
                            "onAndroidViewportChanged($viewportWidth,$viewportHeight);}",
                    null
                )
            }
        }
    }

    var selectedMusicSection by remember {
        mutableStateOf(MusicLibrarySection.NHAC_VIET)
    }
    val searchQueries = remember {
        mutableStateMapOf(
            MusicLibrarySection.NHAC_VIET to "",
            MusicLibrarySection.QUOC_TE to "",
            MusicLibrarySection.DIEU_NHAC to ""
        )
    }
    val songs = remember(initialSongs) {
        mutableStateListOf<TuneToolsSong>().also { it.addAll(initialSongs) }
    }
    var activeSearchSource by remember(initialSearchSource) {
        mutableStateOf(initialSearchSource)
    }
    var searchRequestId by remember { mutableIntStateOf(0) }

    // null: đang ở danh sách các điệu; khác null: đang xem file của một điệu.
    var selectedRhythm by remember { mutableStateOf<String?>(null) }

    // Duong dan tuong doi dang mo ben trong /tab/Vietnam. Chuoi rong la thu muc goc.
    var selectedVietnamFolder by remember { mutableStateOf("") }

    var currentSong by remember { mutableStateOf<TuneToolsSong?>(null) }
    var tracks by remember { mutableStateOf<List<AlphaTrack>>(emptyList()) }
    var selectedTrackIndex by remember { mutableIntStateOf(0) }

    var isSearching by remember { mutableStateOf(false) }
    var isLoadingSong by remember { mutableStateOf(false) }
    var isPlayerReady by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasScore by remember { mutableStateOf(false) }
    // Banner TAB chi duoc hien thi khi nguoi dung cuon qua phan cuoi ban nhac.
    var showTabBottomBanner by remember { mutableStateOf(false) }

    // BPM đang hiển thị trong ô nhập và BPM đã áp dụng vào bản nhạc.
    var bpmText by remember { mutableStateOf("") }
    var currentBpm by remember { mutableIntStateOf(0) }

    // Mặc định giữ nguyên bố cục hiện tại: khuông nhạc + TAB.
    // Người dùng có thể chuyển nhanh sang TAB hoặc Partition (chỉ khuông nhạc).
    var selectedDisplayMode by remember { mutableStateOf(TabDisplayMode.SCORE_TAB) }
    var displayModeMenuExpanded by remember { mutableStateOf(false) }
    var showNoteNames by remember(context) {
        mutableStateOf(
            tabDisplayPrefs.getBoolean(TAB_DISPLAY_PREF_NOTE_NAMES, false)
        )
    }

    var trackMenuExpanded by remember { mutableStateOf(false) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var hideExtraButtons by remember { mutableStateOf(false) }
    var isImportingLocalFile by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }

    fun finishPdfExport(message: String? = null, isError: Boolean = false) {
        webViewInstance?.evaluateJavascript(
            "javascript:if (typeof restoreAfterA4PdfExport === 'function') { restoreAfterA4PdfExport(); }",
            null
        )
        pdfPrintStarted.set(false)
        isExportingPdf = false

        if (!message.isNullOrBlank()) {
            Toast.makeText(
                context,
                message,
                if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startA4PdfPrint() {
        if (!isExportingPdf || !pdfPrintStarted.compareAndSet(false, true)) return

        val targetWebView = webViewInstance
        val printManager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (targetWebView == null || printManager == null) {
            finishPdfExport(
                message = "Thiết bị không hỗ trợ dịch vụ xuất PDF.",
                isError = true
            )
            return
        }

        try {
            val jobName = pdfJobName.get().ifBlank { "Ban_nhac_A4.pdf" }
            val printAdapter = targetWebView.createPrintDocumentAdapter(jobName)
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asPortrait())
                .setResolution(
                    PrintAttributes.Resolution(
                        "tunertools_pdf_a4",
                        "TuneTools PDF A4",
                        600,
                        600
                    )
                )
                // Đơn vị margins là 1/1000 inch; 394 xấp xỉ 10 mm.
                .setMinMargins(PrintAttributes.Margins(394, 394, 394, 394))
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()

            val printJob = printManager.print(jobName, printAdapter, printAttributes)
            Toast.makeText(
                context,
                "Chọn ‘Lưu dưới dạng PDF’ để lưu bản nhạc khổ A4.",
                Toast.LENGTH_LONG
            ).show()

            // Giữ WebView ở bố cục A4 cho tới khi hộp thoại in/lưu kết thúc.
            scope.launch {
                while (
                    !printJob.isCompleted &&
                    !printJob.isCancelled &&
                    !printJob.isFailed
                ) {
                    delay(400L)
                }

                when {
                    printJob.isFailed -> finishPdfExport(
                        message = "Xuất PDF không thành công.",
                        isError = true
                    )
                    printJob.isCompleted -> finishPdfExport(
                        message = "Đã hoàn tất xuất PDF A4."
                    )
                    else -> finishPdfExport()
                }
            }
        } catch (e: Exception) {
            finishPdfExport(
                message = "Không thể mở chức năng xuất PDF: ${e.message ?: "Lỗi không xác định"}",
                isError = true
            )
        }
    }

    // Khởi tạo WebView một lần duy nhất để không bị reload khi xoay màn hình (chuyển sang Dialog)
    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setBackgroundColor(android.graphics.Color.TRANSPARENT)

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.let(::syncTabViewport)
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url.toString()

                    // File người dùng chọn trong bộ nhớ máy được phục vụ qua một URL ảo
                    // cùng origin với WebView. Cách này đọc được cả content:// mà không
                    // yêu cầu quyền truy cập toàn bộ bộ nhớ.
                    val localPayload = localScorePayload.get()
                    if (localPayload != null && url == localPayload.requestUrl) {
                        return WebResourceResponse(
                            "application/octet-stream",
                            null,
                            200,
                            "OK",
                            mapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Cache-Control" to "no-store"
                            ),
                            ByteArrayInputStream(localPayload.bytes)
                        )
                    }

                    // Hai SoundFont tải thêm được phục vụ qua URL ảo. Dữ liệu thật nằm
                    // trong filesDir/soundfonts nên không làm tăng kích thước APK/AAB.
                    val localSoundFontMode = when (url) {
                        LOCAL_FLUIDR3_SOUNDFONT_URL -> TabSoundFontMode.FLUIDR3
                        LOCAL_MUSESCORE_SOUNDFONT_URL -> TabSoundFontMode.MUSESCORE
                        else -> null
                    }
                    if (localSoundFontMode != null) {
                        val localSoundFont = localSoundFontFileRefs[localSoundFontMode]?.get()
                        if (localSoundFont != null && localSoundFont.isFile && localSoundFont.length() > 0L) {
                            return try {
                                WebResourceResponse(
                                    "application/octet-stream",
                                    null,
                                    200,
                                    "OK",
                                    mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Cache-Control" to "no-store",
                                        "Content-Length" to localSoundFont.length().toString()
                                    ),
                                    localSoundFont.inputStream().buffered()
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }

                    if (url.endsWith(".gp3") || url.endsWith(".gp4") || url.endsWith(".gp5") || url.endsWith(".gpx") || url.endsWith(".gp")) {
                        try {
                            val connection = URL(url).openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            val inputStream = connection.inputStream
                            val responseHeaders = mutableMapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Access-Control-Allow-Methods" to "GET, OPTIONS",
                                "Access-Control-Allow-Headers" to "*"
                            )
                            return WebResourceResponse("application/octet-stream", "UTF-8", 200, "OK", responseHeaders, inputStream)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            addOnLayoutChangeListener { view, left, top, right, bottom,
                                        oldLeft, oldTop, oldRight, oldBottom ->
                val changed = right - left != oldRight - oldLeft ||
                        bottom - top != oldBottom - oldTop
                if (changed) (view as? WebView)?.let(::syncTabViewport)
            }

            addJavascriptInterface(object {
                @JavascriptInterface
                fun onTabReady() {
                    mainHandler.post {
                        isTabJsReady.set(true)
                        webViewInstance?.let(::dispatchPendingTabSong)
                    }
                }

                @JavascriptInterface
                fun onPlayerReady() {
                    mainHandler.post {
                        isPlayerReady = true
                        isSwitchingSoundFont = false
                        isCheckingSoundFontCompatibility = false
                    }
                }

                @JavascriptInterface
                fun onSoundFontLoadProgress(loaded: Double, total: Double) {
                    mainHandler.post {
                        soundFontDecodeProgress = if (total > 0.0) {
                            (loaded / total).toFloat().coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    }
                }

                @JavascriptInterface
                fun onSoundFontLoaded(mode: String, temporaryFallback: Boolean) {
                    mainHandler.post {
                        val loadedMode = TabSoundFontMode.fromJsValue(mode)
                        activeSoundFontMode = loadedMode
                        isSwitchingSoundFont = false
                        soundFontDecodeProgress = 1f

                        if (!temporaryFallback) {
                            // Lựa chọn thủ công là cấu hình người dùng muốn dùng cho các bài sau.
                            selectedSoundFontMode = loadedMode
                            temporarySoundFontFallbackFrom = null
                            soundFontPrefs.edit()
                                .putString(SOUNDFONT_PREF_MODE, loadedMode.storedValue)
                                .apply()
                        }
                        // Với fallback tạm thời, chỉ activeSoundFontMode đổi sang STANDARD.
                        // selectedSoundFontMode và SharedPreferences vẫn giữ SF2/SF3 đã chọn.
                    }
                }

                @JavascriptInterface
                fun onSoundFontCompatibilityCheckChanged(checking: Boolean) {
                    mainHandler.post {
                        // Khong ha isPlayerReady khi chi dang kiem tra am thanh. Neu bai tuong thich,
                        // nut Play phai su dung lai duoc ngay sau khi phep kiem tra ket thuc.
                        isCheckingSoundFontCompatibility = checking
                    }
                }

                @JavascriptInterface
                fun onSoundFontAutoFallback(mode: String) {
                    mainHandler.post {
                        val incompatibleMode = TabSoundFontMode.fromJsValue(mode)
                        temporarySoundFontFallbackFrom = incompatibleMode
                        isCheckingSoundFontCompatibility = false
                        isSwitchingSoundFont = true
                        isPlayerReady = false
                        soundFontDecodeProgress = 0f

                        Toast.makeText(
                            context,
                            soundFontFormat(
                                "tab_sound_auto_fallback", currentLang,
                                "Bộ âm thanh %1\$s không tạo được âm thanh cho bản nhạc này. Ứng dụng sẽ tự chuyển sang %2\$s.",
                                "The %1\$s audio pack produced no sound for this score. The app will switch to %2\$s automatically.",
                                incompatibleMode.menuTitle(currentLang),
                                TabSoundFontMode.STANDARD.menuTitle(currentLang)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                @JavascriptInterface
                fun onSoundFontLoadFailed(mode: String, message: String) {
                    mainHandler.post {
                        val failedMode = TabSoundFontMode.fromJsValue(mode)
                        isSwitchingSoundFont = false
                        isCheckingSoundFontCompatibility = false
                        isPlayerReady = false
                        soundFontDecodeProgress = 0f

                        if (failedMode.isDownloadable) {
                            selectedSoundFontMode = TabSoundFontMode.STANDARD
                            activeSoundFontMode = TabSoundFontMode.STANDARD
                            soundFontPrefs.edit()
                                .putString(
                                    SOUNDFONT_PREF_MODE,
                                    TabSoundFontMode.STANDARD.storedValue
                                )
                                .apply()
                            soundFontFallbackRequest++
                        }

                        val displayedMessage = if (failedMode.isDownloadable) {
                            soundFontFormat(
                                "tab_sound_load_fallback", currentLang,
                                "Không thể sử dụng %1\$s cho bản nhạc này. Ứng dụng sẽ chuyển sang %2\$s.",
                                "Could not use %1\$s for this score. The app will switch to %2\$s.",
                                failedMode.menuTitle(currentLang),
                                TabSoundFontMode.STANDARD.menuTitle(currentLang)
                            )
                        } else {
                            message.ifBlank {
                                soundFontText(
                                    "tab_sound_load_failed", currentLang,
                                    "Không thể nạp bộ âm thanh đã chọn.",
                                    "Could not load the selected audio pack."
                                )
                            }
                        }

                        Toast.makeText(
                            context,
                            displayedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                @JavascriptInterface
                fun onPlayerStateChanged(state: Int) { mainHandler.post { isPlaying = (state == 1) } }

                @JavascriptInterface
                fun onTempoChanged(bpm: Int) {
                    mainHandler.post {
                        if (bpm > 0) {
                            currentBpm = bpm
                            bpmText = bpm.toString()
                        }
                    }
                }

                @JavascriptInterface
                fun onTempoChangeFailed(message: String) {
                    mainHandler.post {
                        Toast.makeText(
                            context,
                            if (message.isBlank()) "Không thể thay đổi BPM." else message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                @JavascriptInterface
                fun onPdfLayoutReady() {
                    mainHandler.post {
                        startA4PdfPrint()
                    }
                }

                @JavascriptInterface
                fun onPdfExportFailed(message: String) {
                    mainHandler.post {
                        finishPdfExport(
                            message = if (message.isBlank()) {
                                "Không thể chuẩn bị bản nhạc để xuất PDF."
                            } else {
                                message
                            },
                            isError = true
                        )
                    }
                }

                @JavascriptInterface
                fun onTabBottomBannerVisibilityChanged(visible: Boolean) {
                    // JavaScriptInterface co the duoc goi tren background thread.
                    mainHandler.post {
                        val shouldShow = visible && hasScore && !isLoadingSong && currentSong != null
                        if (showTabBottomBanner != shouldShow) {
                            showTabBottomBanner = shouldShow
                        }
                    }
                }

                @JavascriptInterface
                fun onScoreLoaded(tracksJson: String, title: String) {
                    // AlphaTab đã đọc xong dữ liệu nên có thể giải phóng bản sao byte local.
                    localScorePayload.set(null)
                    mainHandler.post {
                        if (title.startsWith("Lỗi")) {
                            isLoadingSong = false
                            hasScore = false
                            showTabBottomBanner = false
                            isPlayerReady = false
                            currentBpm = 0
                            bpmText = ""
                            Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
                        } else {
                            hasScore = true
                            isLoadingSong = false
                            val parsedTracks = mutableListOf<AlphaTrack>()
                            try {
                                val arr = JSONArray(tracksJson)
                                for (i in 0 until arr.length()) {
                                    parsedTracks.add(
                                        AlphaTrack(
                                            arr.getJSONObject(i).getInt("index"),
                                            arr.getJSONObject(i).getString("name")
                                        )
                                    )
                                }
                                tracks = parsedTracks
                                selectedTrackIndex = 0

                                // Nếu bài trước phải tạm dùng Sonivox, bài mới sẽ thử lại đúng
                                // cấu hình SF2/SF3 mà người dùng đã chọn và đã tải.
                                if (
                                    temporarySoundFontFallbackFrom != null &&
                                    activeSoundFontMode != selectedSoundFontMode &&
                                    selectedSoundFontMode.isDownloadable &&
                                    downloadedSoundFontModes[selectedSoundFontMode] == true
                                ) {
                                    soundFontRestoreRequest++
                                }
                            } catch (_: Exception) {
                                tracks = emptyList()
                            }
                        }
                    }
                }
            }, "AndroidInterface")

            // Trình duyệt khởi tạo với cờ DarkTheme hiện tại
            loadDataWithBaseURL("https://tunertools.top", getAlphaTabHtml(
                isDarkInitial = localIsDark,
                initialSoundFontMode = startupSoundFontMode.jsValue,
                initialSoundFontUrl = startupSoundFontUrl,
                currentLang = currentLang,
                initialShowNoteNames = showNoteNames
            ), "text/html", "UTF-8", null)
        }
    }

    LaunchedEffect(webView) {
        webViewInstance = webView
        syncTabViewport(webView)
        if (isTabJsReady.get()) dispatchPendingTabSong(webView)
    }

    // Gọi JS thay đổi giao diện mượt mà không tải lại nhạc
    LaunchedEffect(localIsDark) {
        webViewInstance?.evaluateJavascript("javascript:if(typeof toggleTheme === 'function') toggleTheme($localIsDark);", null)
    }

    fun switchSoundFontMode(mode: TabSoundFontMode) {
        if (mode.isDownloadable && downloadedSoundFontModes[mode] != true) {
            Toast.makeText(
                context,
                soundFontFormat(
                    "tab_sound_download_first", currentLang,
                    "Hãy tải %1\$s trước.",
                    "Download %1\$s first.",
                    mode.menuTitle(currentLang)
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val targetWebView = webViewInstance
        if (targetWebView == null) {
            Toast.makeText(
                context,
                soundFontText(
                    "tab_sound_player_not_ready", currentLang,
                    "Trình phát chưa sẵn sàng.",
                    "The player is not ready."
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        soundFontMenuExpanded = false
        selectedSoundFontMode = mode
        temporarySoundFontFallbackFrom = null
        isCheckingSoundFontCompatibility = false
        soundFontPrefs.edit()
            .putString(SOUNDFONT_PREF_MODE, mode.storedValue)
            .apply()
        isSwitchingSoundFont = true
        isPlayerReady = false
        soundFontDecodeProgress = 0f

        val jsMode = JSONObject.quote(mode.jsValue)
        val jsUrl = JSONObject.quote(mode.playbackUrl)
        targetWebView.evaluateJavascript(
            "javascript:(function(){" +
                    "if(typeof switchSoundFontMode !== 'function') return false;" +
                    "return switchSoundFontMode($jsMode, $jsUrl);" +
                    "})();"
        ) { result ->
            if (result != "true") {
                mainHandler.post {
                    isSwitchingSoundFont = false
                    isPlayerReady = false
                    Toast.makeText(
                        context,
                        soundFontText(
                            "tab_sound_switch_failed", currentLang,
                            "Không thể chuyển bộ âm thanh. Hãy mở lại Bản nhạc.",
                            "Could not switch the audio pack. Reopen Sheet Music."
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(soundFontFallbackRequest, webViewInstance) {
        if (soundFontFallbackRequest > 0 && webViewInstance != null) {
            switchSoundFontMode(TabSoundFontMode.STANDARD)
        }
    }

    LaunchedEffect(soundFontRestoreRequest, webViewInstance) {
        val preferredMode = selectedSoundFontMode
        if (
            soundFontRestoreRequest > 0 &&
            webViewInstance != null &&
            preferredMode.isDownloadable &&
            downloadedSoundFontModes[preferredMode] == true &&
            activeSoundFontMode != preferredMode
        ) {
            temporarySoundFontFallbackFrom = null
            switchSoundFontMode(preferredMode)
        }
    }

    LaunchedEffect(apiBaseUrl, searchPath, apiToken) {
        downloadableSoundFontModes.forEach { mode ->
            soundFontInfoErrors.remove(mode)
            try {
                val info = withContext(Dispatchers.IO) {
                    loadTabSoundFontInfo(apiBaseUrl, searchPath, apiToken, mode, currentLang)
                }
                soundFontInfos[mode] = info

                val completeFile = soundFontFiles.getValue(mode)
                val partFile = soundFontPartFiles.getValue(mode)
                if (completeFile.isFile) {
                    val savedVersion = soundFontPrefs.getString(
                        soundFontVersionPreferenceKey(mode),
                        null
                    )
                    val sizeMatches = completeFile.length() == info.sizeBytes
                    val versionMatches = savedVersion.isNullOrBlank() ||
                            savedVersion == info.version

                    if (sizeMatches && versionMatches) {
                        downloadedSoundFontModes[mode] = true
                        localSoundFontDataModes[mode] = true
                        localSoundFontFileRefs[mode]?.set(completeFile)
                        soundFontPrefs.edit()
                            .putLong(soundFontSizePreferenceKey(mode), info.sizeBytes)
                            .putString(soundFontVersionPreferenceKey(mode), info.version)
                            .apply()
                    } else {
                        // File hoàn chỉnh sai kích thước hoặc máy chủ có phiên bản mới:
                        // giữ lại để người dùng chủ động xóa/tải lại nhưng không dùng để phát.
                        downloadedSoundFontModes[mode] = false
                        localSoundFontDataModes[mode] = true
                        localSoundFontFileRefs[mode]?.set(null)
                        if (
                            selectedSoundFontMode == mode ||
                            activeSoundFontMode == mode
                        ) {
                            selectedSoundFontMode = TabSoundFontMode.STANDARD
                            activeSoundFontMode = TabSoundFontMode.STANDARD
                            soundFontFallbackRequest++
                        }
                    }
                } else {
                    downloadedSoundFontModes[mode] = false
                    localSoundFontFileRefs[mode]?.set(null)
                    localSoundFontDataModes[mode] = partFile.exists()
                }

                if (partFile.isFile && info.sizeBytes > 0L) {
                    val downloadedBytes = partFile.length()
                    soundFontDownloadedBytesByMode[mode] = downloadedBytes
                    soundFontDownloadProgressByMode[mode] =
                        (downloadedBytes.toDouble() / info.sizeBytes.toDouble())
                            .toFloat()
                            .coerceIn(0f, 1f)
                    localSoundFontDataModes[mode] = true
                }
            } catch (error: Exception) {
                if (downloadedSoundFontModes[mode] != true) {
                    soundFontInfoErrors[mode] =
                        error.message ?: soundFontText(
                            "tab_sound_server_error", currentLang,
                            "Không thể kết nối máy chủ SoundFont.",
                            "Could not connect to the SoundFont server."
                        )
                }
            }
        }
    }

    fun startSoundFontDownload(mode: TabSoundFontMode) {
        if (!mode.isDownloadable || downloadingSoundFontMode != null) return

        downloadingSoundFontMode = mode
        soundFontInfoErrors.remove(mode)

        scope.launch {
            try {
                val info = soundFontInfos[mode] ?: withContext(Dispatchers.IO) {
                    loadTabSoundFontInfo(apiBaseUrl, searchPath, apiToken, mode, currentLang)
                }.also { soundFontInfos[mode] = it }

                val completeFile = soundFontFiles.getValue(mode)
                val partFile = soundFontPartFiles.getValue(mode)
                val downloadedFile = withContext(Dispatchers.IO) {
                    val directory = soundFontDirectory(context)
                    if (!directory.exists() && !directory.mkdirs()) {
                        throw IllegalStateException(
                            soundFontText(
                                "tab_sound_create_dir_failed", currentLang,
                                "Không thể tạo thư mục lưu SoundFont.",
                                "Could not create the SoundFont storage folder."
                            )
                        )
                    }

                    val savedCompleteVersion = soundFontPrefs.getString(
                        soundFontVersionPreferenceKey(mode),
                        null
                    )
                    if (
                        completeFile.isFile &&
                        completeFile.length() == info.sizeBytes &&
                        (savedCompleteVersion.isNullOrBlank() ||
                                savedCompleteVersion == info.version)
                    ) {
                        return@withContext completeFile
                    }

                    val savedPartVersion = soundFontPrefs.getString(
                        soundFontPartVersionPreferenceKey(mode),
                        null
                    )
                    if (partFile.exists() && savedPartVersion != info.version) {
                        partFile.delete()
                    }

                    soundFontPrefs.edit()
                        .putString(soundFontPartVersionPreferenceKey(mode), info.version)
                        .apply()

                    var existingBytes = partFile.takeIf { it.isFile }?.length() ?: 0L
                    if (existingBytes > info.sizeBytes) {
                        partFile.delete()
                        existingBytes = 0L
                    }

                    if (existingBytes == info.sizeBytes && existingBytes > 0L) {
                        if (completeFile.exists()) completeFile.delete()
                        if (!partFile.renameTo(completeFile)) {
                            partFile.copyTo(completeFile, overwrite = true)
                            partFile.delete()
                        }
                        return@withContext completeFile
                    }

                    val remainingBytes = info.sizeBytes - existingBytes
                    val safetyBytes = 20L * 1024L * 1024L
                    if (
                        directory.usableSpace > 0L &&
                        directory.usableSpace < remainingBytes + safetyBytes
                    ) {
                        throw IllegalStateException(
                            soundFontFormat(
                                "tab_sound_storage_not_enough", currentLang,
                                "Bộ nhớ trống không đủ. Cần thêm ít nhất %1\$s.",
                                "Not enough free storage. At least %1\$s more is required.",
                                formatStorageSize(remainingBytes + safetyBytes)
                            )
                        )
                    }

                    val connection = URL(info.downloadUrl).openConnection() as HttpURLConnection
                    try {
                        connection.requestMethod = "GET"
                        connection.connectTimeout = 20_000
                        connection.readTimeout = 120_000
                        connection.setRequestProperty("Accept", "application/octet-stream")
                        connection.setRequestProperty("Accept-Encoding", "identity")
                        connection.setRequestProperty("User-Agent", "TunerInstrument-Android")
                        if (!apiToken.isNullOrBlank()) {
                            connection.setRequestProperty("Authorization", "Bearer $apiToken")
                        }
                        if (existingBytes > 0L) {
                            connection.setRequestProperty("Range", "bytes=$existingBytes-")
                        }

                        val responseCode = connection.responseCode
                        var append =
                            responseCode == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L

                        if (responseCode == HttpURLConnection.HTTP_OK && existingBytes > 0L) {
                            partFile.delete()
                            existingBytes = 0L
                            append = false
                        }

                        if (
                            responseCode != HttpURLConnection.HTTP_OK &&
                            responseCode != HttpURLConnection.HTTP_PARTIAL
                        ) {
                            val serverMessage = connection.errorStream
                                ?.bufferedReader(StandardCharsets.UTF_8)
                                ?.use { it.readText() }
                                .orEmpty()
                            throw IllegalStateException(
                                runCatching {
                                    JSONObject(serverMessage).optString("error")
                                }.getOrNull().orEmpty().ifBlank {
                                    soundFontFormat(
                                        "tab_sound_http_failed", currentLang,
                                        "Tải SoundFont thất bại: HTTP %1\$d",
                                        "SoundFont download failed: HTTP %1\$d",
                                        responseCode
                                    )
                                }
                            )
                        }

                        var totalWritten = existingBytes
                        BufferedInputStream(connection.inputStream, 128 * 1024).use { input ->
                            BufferedOutputStream(
                                FileOutputStream(partFile, append),
                                128 * 1024
                            ).use { output ->
                                val buffer = ByteArray(128 * 1024)
                                var lastUiUpdate = 0L

                                while (true) {
                                    val readCount = input.read(buffer)
                                    if (readCount < 0) break
                                    if (readCount == 0) continue

                                    output.write(buffer, 0, readCount)
                                    totalWritten += readCount

                                    val now = System.currentTimeMillis()
                                    if (now - lastUiUpdate >= 120L) {
                                        lastUiUpdate = now
                                        val bytesForUi = totalWritten
                                        mainHandler.post {
                                            soundFontDownloadedBytesByMode[mode] = bytesForUi
                                            soundFontDownloadProgressByMode[mode] =
                                                (bytesForUi.toDouble() / info.sizeBytes.toDouble())
                                                    .toFloat()
                                                    .coerceIn(0f, 1f)
                                            localSoundFontDataModes[mode] = true
                                        }
                                    }
                                }
                                output.flush()
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }

                    if (partFile.length() != info.sizeBytes) {
                        throw IllegalStateException(
                            soundFontFormat(
                                "tab_sound_file_incomplete", currentLang,
                                "File tải chưa hoàn tất: %1\$s / %2\$s",
                                "Download incomplete: %1\$s / %2\$s",
                                formatStorageSize(partFile.length()),
                                formatStorageSize(info.sizeBytes)
                            )
                        )
                    }

                    if (completeFile.exists() && !completeFile.delete()) {
                        throw IllegalStateException(
                            soundFontText(
                                "tab_sound_replace_failed", currentLang,
                                "Không thể thay thế SoundFont cũ.",
                                "Could not replace the previous SoundFont."
                            )
                        )
                    }

                    if (!partFile.renameTo(completeFile)) {
                        partFile.copyTo(completeFile, overwrite = true)
                        partFile.delete()
                    }

                    completeFile
                }

                localSoundFontFileRefs[mode]?.set(downloadedFile)
                downloadedSoundFontModes[mode] = true
                localSoundFontDataModes[mode] = true
                soundFontDownloadedBytesByMode[mode] = downloadedFile.length()
                soundFontDownloadProgressByMode[mode] = 1f
                soundFontPrefs.edit()
                    .putLong(soundFontSizePreferenceKey(mode), downloadedFile.length())
                    .putString(soundFontVersionPreferenceKey(mode), info.version)
                    .remove(soundFontPartVersionPreferenceKey(mode))
                    .apply()

                Toast.makeText(
                    context,
                    soundFontFormat(
                        "tab_sound_downloaded", currentLang,
                        "Đã tải %1\$s (%2\$s).",
                        "Downloaded %1\$s (%2\$s).",
                        info.displayName,
                        formatStorageSize(downloadedFile.length())
                    ),
                    Toast.LENGTH_LONG
                ).show()

                switchSoundFontMode(mode)
            } catch (error: Exception) {
                val completeFile = soundFontFiles.getValue(mode)
                val partFile = soundFontPartFiles.getValue(mode)
                localSoundFontDataModes[mode] = completeFile.exists() || partFile.exists()
                val errorMessage = error.message ?: soundFontText(
                    "tab_sound_download_failed", currentLang,
                    "Không thể tải SoundFont.",
                    "Could not download SoundFont."
                )
                soundFontInfoErrors[mode] = errorMessage
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                downloadingSoundFontMode = null
            }
        }
    }

    fun deleteSoundFont(mode: TabSoundFontMode) {
        if (!mode.isDownloadable) return
        if (downloadingSoundFontMode == mode) {
            Toast.makeText(
                context,
                soundFontText(
                    "tab_sound_delete_wait", currentLang,
                    "SoundFont đang tải. Hãy đợi hoàn tất rồi xóa.",
                    "SoundFont is downloading. Wait until it finishes before deleting."
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val wasActive =
            selectedSoundFontMode == mode || activeSoundFontMode == mode
        if (wasActive) {
            if (webViewInstance != null) {
                switchSoundFontMode(TabSoundFontMode.STANDARD)
            } else {
                selectedSoundFontMode = TabSoundFontMode.STANDARD
                activeSoundFontMode = TabSoundFontMode.STANDARD
                soundFontPrefs.edit()
                    .putString(
                        SOUNDFONT_PREF_MODE,
                        TabSoundFontMode.STANDARD.storedValue
                    )
                    .apply()
            }
        }

        localSoundFontFileRefs[mode]?.set(null)
        scope.launch {
            if (wasActive) delay(350L)

            val completeFile = soundFontFiles.getValue(mode)
            val partFile = soundFontPartFiles.getValue(mode)
            val deleted = withContext(Dispatchers.IO) {
                var success = true
                if (completeFile.exists()) {
                    success = completeFile.delete() && success
                }
                if (partFile.exists()) {
                    success = partFile.delete() && success
                }
                success
            }

            if (deleted) {
                downloadedSoundFontModes[mode] = false
                localSoundFontDataModes[mode] = false
                soundFontDownloadedBytesByMode[mode] = 0L
                soundFontDownloadProgressByMode[mode] = 0f
                soundFontInfoErrors.remove(mode)
                soundFontPrefs.edit()
                    .remove(soundFontSizePreferenceKey(mode))
                    .remove(soundFontVersionPreferenceKey(mode))
                    .remove(soundFontPartVersionPreferenceKey(mode))
                    .apply()
                Toast.makeText(
                    context,
                    soundFontFormat(
                        "tab_sound_deleted", currentLang,
                        "Đã xóa %1\$s khỏi thiết bị.",
                        "Removed %1\$s from this device.",
                        mode.menuTitle(currentLang)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                localSoundFontDataModes[mode] = completeFile.exists() || partFile.exists()
                Toast.makeText(
                    context,
                    soundFontText(
                        "tab_sound_delete_failed", currentLang,
                        "Không thể xóa hoàn toàn file SoundFont.",
                        "Could not completely delete the SoundFont file."
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun selectTrack(index: Int) {
        if (index !in tracks.indices) return
        selectedTrackIndex = index
        isCheckingSoundFontCompatibility = false
        webViewInstance?.evaluateJavascript("javascript:selectTrackSolo($index);", null)
    }

    fun applyBpm() {
        if (!hasScore) return

        val targetBpm = bpmText.toIntOrNull()
        if (targetBpm == null) {
            Toast.makeText(context, "Vui lòng nhập BPM.", Toast.LENGTH_SHORT).show()
            return
        }
        if (targetBpm !in 20..400) {
            Toast.makeText(
                context,
                "BPM phải nằm trong khoảng 20 đến 400.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        focusManager.clearFocus()
        webViewInstance?.evaluateJavascript(
            "javascript:if (typeof setScoreBpm === 'function') { setScoreBpm($targetBpm); } else { AndroidInterface.onTempoChangeFailed('Bộ điều khiển BPM chưa sẵn sàng.'); }",
            null
        )
    }

    fun switchDisplayMode(mode: TabDisplayMode) {
        displayModeMenuExpanded = false
        if (mode == selectedDisplayMode || isExportingPdf) return

        val targetWebView = webViewInstance
        if (targetWebView == null || !hasScore) {
            Toast.makeText(
                context,
                soundFontText(
                    "tab_display_not_ready",
                    currentLang,
                    "Bản nhạc chưa sẵn sàng để đổi chế độ hiển thị.",
                    "The score is not ready for changing the display mode."
                ),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val previousMode = selectedDisplayMode
        selectedDisplayMode = mode
        val quotedMode = JSONObject.quote(mode.jsValue)
        targetWebView.evaluateJavascript(
            "javascript:(function(){" +
                    "if(typeof setStaveDisplayMode !== 'function') return false;" +
                    "return setStaveDisplayMode($quotedMode);" +
                    "})();"
        ) { result ->
            if (result != "true") {
                mainHandler.post {
                    selectedDisplayMode = previousMode
                    Toast.makeText(
                        context,
                        soundFontText(
                            "tab_display_switch_failed",
                            currentLang,
                            "Không thể đổi chế độ hiển thị.",
                            "Could not change the display mode."
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    fun setNoteNamesEnabled(enabled: Boolean) {
        if (isExportingPdf || enabled == showNoteNames) return

        val previousValue = showNoteNames
        showNoteNames = enabled
        tabDisplayPrefs.edit()
            .putBoolean(TAB_DISPLAY_PREF_NOTE_NAMES, enabled)
            .apply()

        val targetWebView = webViewInstance
        if (targetWebView == null || !hasScore) return

        targetWebView.evaluateJavascript(
            "javascript:(function(){" +
                    "if(typeof setNoteNamesVisible !== 'function') return false;" +
                    "return setNoteNamesVisible($enabled);" +
                    "})();"
        ) { result ->
            if (result != "true") {
                mainHandler.post {
                    showNoteNames = previousValue
                    tabDisplayPrefs.edit()
                        .putBoolean(TAB_DISPLAY_PREF_NOTE_NAMES, previousValue)
                        .apply()
                    Toast.makeText(
                        context,
                        soundFontText(
                            "tab_note_names_switch_failed",
                            currentLang,
                            "Không thể thay đổi hiển thị tên nốt.",
                            "Could not change note-name display."
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun requestPdfExport() {
        if (!hasScore || isExportingPdf) return

        val targetWebView = webViewInstance
        if (targetWebView == null) {
            Toast.makeText(
                context,
                "Bản nhạc chưa sẵn sàng để xuất PDF.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        focusManager.clearFocus()
        pdfPrintStarted.set(false)
        pdfJobName.set(buildA4PdfJobName(currentSong?.title))
        isExportingPdf = true

        targetWebView.evaluateJavascript(
            "javascript:if (typeof prepareA4PdfExport === 'function') { prepareA4PdfExport(); } else { AndroidInterface.onPdfExportFailed('Bộ xuất PDF chưa sẵn sàng.'); }",
            null
        )
    }

    fun searchSongs(
        section: MusicLibrarySection = selectedMusicSection,
        queryOverride: String? = null,
        rhythmOverride: String? = null,
        folderOverride: String? = null,
        showNoResultsToast: Boolean = true
    ) {
        val q = (queryOverride ?: searchQueries[section].orEmpty()).trim()
        val requestedRhythm = if (section == MusicLibrarySection.DIEU_NHAC) {
            (rhythmOverride ?: selectedRhythm)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val requestedVietnamFolder = if (section == MusicLibrarySection.NHAC_VIET) {
            (folderOverride ?: selectedVietnamFolder)
                .trim()
                .trim('/')
        } else {
            ""
        }

        focusManager.clearFocus()

        // Cho phép phát sinh request mới ngay cả khi request cũ chưa xong.
        // searchRequestId sẽ loại bỏ kết quả cũ khi nó trả về muộn.
        searchRequestId += 1
        val requestId = searchRequestId
        isSearching = true
        songs.clear()

        scope.launch {
            try {
                val outcome = withContext(Dispatchers.IO) {
                    TuneToolsSheetMusicApi.search(
                        apiBaseUrl = apiBaseUrl,
                        searchPath = searchPath,
                        keyword = q,
                        apiToken = apiToken,
                        section = section,
                        rhythm = requestedRhythm,
                        folder = requestedVietnamFolder.takeIf { it.isNotEmpty() }
                    )
                }
                val result = outcome.songs

                val isStillSameRhythm =
                    section != MusicLibrarySection.DIEU_NHAC ||
                            selectedRhythm == requestedRhythm
                val isStillSameVietnamFolder =
                    section != MusicLibrarySection.NHAC_VIET ||
                            selectedVietnamFolder == requestedVietnamFolder

                // Chỉ nhận kết quả của lần tìm mới nhất, đúng tab và đúng thư mục.
                if (
                    requestId == searchRequestId &&
                    selectedMusicSection == section &&
                    isStillSameRhythm &&
                    isStillSameVietnamFolder
                ) {
                    activeSearchSource = outcome.source
                    songs.addAll(result)
                    if (result.isEmpty() && showNoResultsToast) {
                        val message = when {
                            section == MusicLibrarySection.DIEU_NHAC &&
                                    requestedRhythm != null ->
                                "Không tìm thấy bản Tab trong điệu $requestedRhythm."

                            section == MusicLibrarySection.DIEU_NHAC ->
                                "Không tìm thấy điệu nhạc phù hợp."

                            section == MusicLibrarySection.NHAC_VIET &&
                                    requestedVietnamFolder.isNotBlank() ->
                                "Không tìm thấy bản Tab trong thư mục ${requestedVietnamFolder.substringAfterLast('/')}."

                            section == MusicLibrarySection.NHAC_VIET ->
                                "Không tìm thấy Nhạc Việt phù hợp."

                            else -> "Không tìm thấy nội dung phù hợp."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                val isStillSameRhythm =
                    section != MusicLibrarySection.DIEU_NHAC ||
                            selectedRhythm == requestedRhythm
                val isStillSameVietnamFolder =
                    section != MusicLibrarySection.NHAC_VIET ||
                            selectedVietnamFolder == requestedVietnamFolder

                if (
                    requestId == searchRequestId &&
                    selectedMusicSection == section &&
                    isStillSameRhythm &&
                    isStillSameVietnamFolder
                ) {
                    Toast.makeText(
                        context,
                        "Lỗi tải dữ liệu: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                if (requestId == searchRequestId) {
                    isSearching = false
                }
            }
        }
    }

    // Khi một request phải dùng máy chủ dự phòng, ứng dụng sẽ không "kẹt" ở đó.
    // Cứ mỗi 20 giây, máy chủ chính được kiểm tra lại bằng đúng truy vấn đang mở.
    // Chỉ cần máy chủ chính trả về JSON hợp lệ (kể cả danh sách rỗng), nguồn dữ liệu
    // được chuyển về PRIMARY và danh sách hiện tại được làm mới tự động.
    LaunchedEffect(activeSearchSource) {
        if (activeSearchSource != SheetMusicServerSource.FALLBACK) {
            return@LaunchedEffect
        }

        while (activeSearchSource == SheetMusicServerSource.FALLBACK) {
            delay(TuneToolsSheetMusicApi.PRIMARY_RECOVERY_CHECK_INTERVAL_MS)

            if (isSearching) continue

            val sectionSnapshot = selectedMusicSection
            val querySnapshot = searchQueries[sectionSnapshot].orEmpty().trim()
            val rhythmSnapshot = if (sectionSnapshot == MusicLibrarySection.DIEU_NHAC) {
                selectedRhythm
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val folderSnapshot = if (sectionSnapshot == MusicLibrarySection.NHAC_VIET) {
                selectedVietnamFolder
                    .trim()
                    .trim('/')
                    .takeIf { it.isNotEmpty() }
            } else {
                null
            }

            val primarySongs = runCatching {
                withContext(Dispatchers.IO) {
                    TuneToolsSheetMusicApi.searchPrimaryOnly(
                        apiBaseUrl = apiBaseUrl,
                        searchPath = searchPath,
                        keyword = querySnapshot,
                        apiToken = apiToken,
                        section = sectionSnapshot,
                        rhythm = rhythmSnapshot,
                        folder = folderSnapshot
                    )
                }
            }.getOrNull() ?: continue

            // Máy chủ chính đã phục hồi. Đổi trạng thái ngay để mọi request sau
            // tiếp tục ưu tiên máy chủ chính. Bản nhạc đang phát không bị gián đoạn.
            activeSearchSource = SheetMusicServerSource.PRIMARY

            val sameSection = selectedMusicSection == sectionSnapshot
            val sameQuery = searchQueries[sectionSnapshot].orEmpty().trim() == querySnapshot
            val sameRhythm = sectionSnapshot != MusicLibrarySection.DIEU_NHAC ||
                    selectedRhythm == rhythmSnapshot
            val sameFolder = sectionSnapshot != MusicLibrarySection.NHAC_VIET ||
                    selectedVietnamFolder.trim().trim('/') == folderSnapshot.orEmpty()

            if (
                currentSong == null &&
                !isLoadingSong &&
                sameSection &&
                sameQuery &&
                sameRhythm &&
                sameFolder
            ) {
                // Vô hiệu hóa request cũ nếu có rồi thay danh sách dự phòng bằng dữ liệu chính.
                searchRequestId += 1
                songs.clear()
                songs.addAll(primarySongs)
                isSearching = false
            }

            runCatching {
                android.util.Log.i(
                    "TuneToolsSheetMusicApi",
                    "Primary GP server recovered. Returned to primary source."
                )
            }
        }
    }

    fun openRhythmFolder(item: TuneToolsSong) {
        if (!item.isFolder || isLoadingSong) return

        val rhythmName = item.rhythm
            .ifBlank { item.title }
            .trim()

        if (rhythmName.isEmpty()) return

        selectedRhythm = rhythmName
        searchQueries[MusicLibrarySection.DIEU_NHAC] = ""
        hideExtraButtons = false

        searchSongs(
            section = MusicLibrarySection.DIEU_NHAC,
            queryOverride = "",
            rhythmOverride = rhythmName,
            showNoResultsToast = false
        )
    }

    fun showRhythmFolders() {
        selectedRhythm = null
        searchQueries[MusicLibrarySection.DIEU_NHAC] = ""
        hideExtraButtons = false

        searchSongs(
            section = MusicLibrarySection.DIEU_NHAC,
            queryOverride = "",
            rhythmOverride = null,
            showNoResultsToast = false
        )
    }

    fun openVietnamFolder(item: TuneToolsSong) {
        if (!item.isFolder || isLoadingSong) return

        val folderPath = item.folderPath
            .ifBlank { item.relativePath }
            .trim()
            .trim('/')

        if (folderPath.isEmpty()) return

        selectedVietnamFolder = folderPath
        searchQueries[MusicLibrarySection.NHAC_VIET] = ""
        hideExtraButtons = false

        searchSongs(
            section = MusicLibrarySection.NHAC_VIET,
            queryOverride = "",
            folderOverride = folderPath,
            showNoResultsToast = false
        )
    }

    fun showParentVietnamFolder() {
        val currentFolder = selectedVietnamFolder.trim().trim('/')
        val parentFolder = if (currentFolder.contains('/')) {
            currentFolder.substringBeforeLast('/')
        } else {
            ""
        }

        selectedVietnamFolder = parentFolder
        searchQueries[MusicLibrarySection.NHAC_VIET] = ""
        hideExtraButtons = false

        searchSongs(
            section = MusicLibrarySection.NHAC_VIET,
            queryOverride = "",
            folderOverride = parentFolder,
            showNoResultsToast = false
        )
    }

    fun selectMusicSection(section: MusicLibrarySection) {
        if (section == selectedMusicSection) return

        // Hủy hiệu lực kết quả của request cũ nếu người dùng đổi tab khi đang tải.
        searchRequestId += 1
        isSearching = false
        selectedMusicSection = section
        selectedRhythm = null
        selectedVietnamFolder = ""
        songs.clear()
        focusManager.clearFocus()
        hideExtraButtons = false

        searchSongs(
            section = section,
            rhythmOverride = null,
            showNoResultsToast = false
        )
    }

    fun resolveScoreFallbackTitle(song: TuneToolsSong): String {
        val providedTitle = song.title
            .replace(Regex("\\s+"), " ")
            .trim()

        if (
            providedTitle.isNotBlank() &&
            !providedTitle.equals("Không tên", ignoreCase = true) &&
            !providedTitle.equals("Bài nhạc", ignoreCase = true) &&
            !providedTitle.equals("Untitled", ignoreCase = true)
        ) {
            return providedTitle
        }

        // Kết quả fallback từ SQLite luôn có relative_path. Khi cột title rỗng,
        // lấy tên file làm tiêu đề để bản nhạc vẫn có tên ở phần đầu khuông.
        val fileStem = song.relativePath
            .trim()
            .trim('/')
            .substringAfterLast('/')
            .substringBeforeLast('.', missingDelimiterValue = "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        val titleFromFileName = fileStem
            .substringAfter(" - ", fileStem)
            .trim()

        return titleFromFileName
            .ifBlank { providedTitle }
            .ifBlank { "Bài nhạc" }
    }

    fun openSong(song: TuneToolsSong) {
        if (isLoadingSong) return

        // Không để kết quả tìm kiếm đang chạy quay về phủ lên bản nhạc vừa mở.
        searchRequestId += 1
        isSearching = false

        if (!song.fileUrl.startsWith(LOCAL_SCORE_URL_PREFIX)) {
            localScorePayload.set(null)
        }

        hideExtraButtons = false
        focusManager.clearFocus()

        currentSong = song
        showTabBottomBanner = false
        isLoadingSong = true
        isPlaying = false
        isPlayerReady = false
        isCheckingSoundFontCompatibility = false
        hasScore = false
        currentBpm = 0
        bpmText = ""
        tracks = emptyList()
        songs.clear()

        offsetX = 0f
        offsetY = 0f

        val quotedFileUrl = JSONObject.quote(song.fileUrl)
        val quotedSongTitle = JSONObject.quote(resolveScoreFallbackTitle(song))
        pendingTabSongScript.set(
            "javascript:if(typeof stopPlayer==='function')stopPlayer();" +
                    "loadScoreFromUrl($quotedFileUrl, $quotedSongTitle);"
        )
        if (isTabJsReady.get()) {
            webViewInstance?.let(::dispatchPendingTabSong)
        }
    }

    // Mở file GP trực tiếp từ bộ nhớ máy bằng Android Storage Access Framework.
    val localFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && !isImportingLocalFile && !isLoadingSong) {
            isImportingLocalFile = true
            focusManager.clearFocus()

            scope.launch {
                try {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    val selectedFile = withContext(Dispatchers.IO) {
                        readLocalScoreFile(context, uri)
                    }

                    val requestUrl = buildString {
                        append(LOCAL_SCORE_URL_PREFIX)
                        append(System.nanoTime())
                        append('.')
                        append(selectedFile.extension)
                    }

                    localScorePayload.set(
                        LocalScorePayload(
                            requestUrl = requestUrl,
                            bytes = selectedFile.bytes
                        )
                    )

                    val localTitle = selectedFile.displayName
                        .substringBeforeLast('.', missingDelimiterValue = selectedFile.displayName)
                        .ifBlank { "Bản nhạc trong máy" }

                    openSong(
                        TuneToolsSong(
                            id = "local:$uri",
                            title = localTitle,
                            artist = "Tệp trong máy",
                            fileUrl = requestUrl,
                            format = selectedFile.extension
                        )
                    )
                } catch (e: Exception) {
                    localScorePayload.set(null)
                    Toast.makeText(
                        context,
                        e.message ?: "Không thể mở file đã chọn.",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isImportingLocalFile = false
                }
            }
        }
    }

    // Launcher nhận nội dung tìm kiếm bằng giọng nói cho tab đang chọn.
    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )
            if (!matches.isNullOrEmpty()) {
                val section = selectedMusicSection
                val spokenText = matches[0]
                searchQueries[section] = spokenText
                searchSongs(
                    section = section,
                    queryOverride = spokenText
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localScorePayload.set(null)
            pdfPrintStarted.set(false)
            webViewInstance?.destroy()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val screenContent = @Composable {
        val isViewingSong = currentSong != null || isLoadingSong || hasScore
        val isInsideRhythmFolder =
            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                    !selectedRhythm.isNullOrBlank() &&
                    !isViewingSong
        val isInsideVietnamFolder =
            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                    selectedVietnamFolder.isNotBlank() &&
                    !isViewingSong
        val showBackButton = isViewingSong || isInsideRhythmFolder || isInsideVietnamFolder
        val currentSearchText = searchQueries[selectedMusicSection].orEmpty()
        val currentSearchHint = when {
            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                    !selectedRhythm.isNullOrBlank() ->
                soundFontFormat(
                    "tab_library_search_in_rhythm", currentLang,
                    "Tìm bản TAB trong %1\$s...", "Search TABs in %1\$s...",
                    selectedRhythm.orEmpty()
                )

            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                    selectedVietnamFolder.isNotBlank() ->
                soundFontFormat(
                    "tab_library_search_in_folder", currentLang,
                    "Tìm trong %1\$s...", "Search in %1\$s...",
                    selectedVietnamFolder.substringAfterLast('/')
                )

            else -> selectedMusicSection.searchHint(currentLang)
        }
        val currentFolderRootTitle = when {
            isInsideRhythmFolder -> MusicLibrarySection.DIEU_NHAC.tabTitle(currentLang)
            isInsideVietnamFolder -> MusicLibrarySection.NHAC_VIET.tabTitle(currentLang)
            else -> ""
        }
        val currentFolderPathTitle = when {
            isInsideRhythmFolder -> selectedRhythm.orEmpty()
            isInsideVietnamFolder -> selectedVietnamFolder
                .split('/')
                .filter { it.isNotBlank() }
                .joinToString("  ›  ")
            else -> ""
        }
        val parsedBpm = bpmText.toIntOrNull()
        val canApplyBpm = hasScore &&
                parsedBpm != null &&
                parsedBpm in 20..400 &&
                parsedBpm != currentBpm

        Surface(
            modifier = modifier.fillMaxSize(),
            color = bgColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                    )
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // --- HEADER & Ô TÌM KIẾM ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = !(isLandscape && isPlaying), // Ẩn khi đang xoay ngang VÀ nhạc đang phát
                    enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            if (isViewingSong) 6.dp else 8.dp
                        )
                    ) {
                        // NÚT HOME HOẶC BACK
                        IconButton(
                            onClick = {
                                when {
                                    isViewingSong -> {
                                        // Dọn bản nhạc rồi quay lại đúng danh sách trước đó.
                                        currentSong = null
                                        hasScore = false
                                        showTabBottomBanner = false
                                        isLoadingSong = false
                                        isPlaying = false
                                        isPlayerReady = false
                                        currentBpm = 0
                                        bpmText = ""
                                        tracks = emptyList()
                                        localScorePayload.set(null)
                                        pendingTabSongScript.set(null)
                                        webViewInstance?.evaluateJavascript("javascript:stopPlayer();", null)

                                        searchSongs(
                                            section = selectedMusicSection,
                                            rhythmOverride = selectedRhythm,
                                            folderOverride = selectedVietnamFolder,
                                            showNoResultsToast = false
                                        )
                                    }

                                    isInsideVietnamFolder -> {
                                        // Lui mot cap trong cay thu muc /tab/Vietnam.
                                        showParentVietnamFolder()
                                    }

                                    isInsideRhythmFolder -> {
                                        // Từ danh sách file của một điệu quay lại danh sách các điệu.
                                        showRhythmFolders()
                                    }

                                    else -> {
                                        // Đang ở danh sách gốc: thoát màn hình Bản nhạc.
                                        onClose()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (localIsDark) Color.Black else surfaceColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = if (showBackButton) Icons.Default.ArrowBack else Icons.Default.Home,
                                contentDescription = if (showBackButton) "Quay lại" else "Trang chủ",
                                tint = if (localIsDark) Color.White else textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (isViewingSong) {
                            // Khi đã chọn bài: ẩn ô tìm kiếm và thay bằng ô nhập BPM.
                            BasicTextField(
                                value = bpmText,
                                onValueChange = { rawValue ->
                                    bpmText = rawValue
                                        .filter { it.isDigit() }
                                        .take(3)
                                },
                                enabled = hasScore,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (hasScore) {
                                            accentColor.copy(alpha = 0.65f)
                                        } else {
                                            Color.Gray.copy(alpha = 0.3f)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    // Thu gọn phần đệm để ưu tiên không gian cho số BPM 3 chữ số.
                                    .padding(start = 4.dp, end = 2.dp),
                                textStyle = TextStyle(
                                    color = if (localIsDark) Color.White else textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(
                                    if (localIsDark) Color.White else accentColor
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { applyBpm() }
                                ),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (bpmText.isEmpty()) {
                                                Text(
                                                    text = if (isLoadingSong) "..." else "--",
                                                    color = Color.Gray.copy(alpha = 0.75f),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            innerTextField()
                                        }

                                        Text(
                                            text = "BPM",
                                            color = if (hasScore) accentColor else Color.Gray,
                                            // Cỡ chữ vừa phải: dễ đọc hơn nhưng vẫn ưu tiên chỗ cho số BPM.
                                            fontSize = 7.sp,
                                            lineHeight = 7.sp,
                                            letterSpacing = (-0.25).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )

                                        IconButton(
                                            onClick = { applyBpm() },
                                            enabled = canApplyBpm,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Áp dụng BPM",
                                                tint = if (canApplyBpm) {
                                                    accentColor
                                                } else {
                                                    Color.Gray.copy(alpha = 0.35f)
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            )

                            // Chọn cách hiển thị: khuông + TAB, TAB hoặc Partition.
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (localIsDark) Color.Black else surfaceColor
                                        )
                                        .border(
                                            1.dp,
                                            if (hasScore) {
                                                accentColor.copy(alpha = 0.55f)
                                            } else {
                                                Color.Gray.copy(alpha = 0.3f)
                                            },
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable(
                                            enabled = hasScore && !isExportingPdf
                                        ) {
                                            focusManager.clearFocus()
                                            displayModeMenuExpanded = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LibraryMusic,
                                            contentDescription = selectedDisplayMode.menuTitle(currentLang),
                                            tint = if (hasScore) {
                                                if (localIsDark) Color.White else accentColor
                                            } else {
                                                Color.Gray.copy(alpha = 0.4f)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = selectedDisplayMode.buttonLabel,
                                            color = if (hasScore) {
                                                if (localIsDark) Color.White else accentColor
                                            } else {
                                                Color.Gray.copy(alpha = 0.4f)
                                            },
                                            fontSize = 7.5.sp,
                                            lineHeight = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = displayModeMenuExpanded,
                                    onDismissRequest = { displayModeMenuExpanded = false },
                                    modifier = Modifier
                                        .widthIn(min = 230.dp, max = 280.dp)
                                        .background(surfaceColor)
                                ) {
                                    Text(
                                        text = soundFontText(
                                            "tab_display_menu_title",
                                            currentLang,
                                            "Chế độ hiển thị",
                                            "Display mode"
                                        ),
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            top = 10.dp,
                                            bottom = 6.dp
                                        )
                                    )
                                    HorizontalDivider(
                                        color = Color.Gray.copy(alpha = 0.18f),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    TabDisplayMode.values().forEach { mode ->
                                        val selected = mode == selectedDisplayMode
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = mode.menuTitle(currentLang),
                                                            color = if (selected) accentColor else textColor,
                                                            fontSize = 13.sp,
                                                            fontWeight = if (selected) {
                                                                FontWeight.Bold
                                                            } else {
                                                                FontWeight.SemiBold
                                                            }
                                                        )
                                                        Text(
                                                            text = mode.menuDescription(currentLang),
                                                            color = Color.Gray,
                                                            fontSize = 10.sp,
                                                            lineHeight = 13.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (selected) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = { switchDisplayMode(mode) }
                                        )
                                    }

                                    HorizontalDivider(
                                        color = Color.Gray.copy(alpha = 0.18f),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                enabled = hasScore && !isExportingPdf
                                            ) {
                                                setNoteNamesEnabled(!showNoteNames)
                                            }
                                            .padding(
                                                start = 16.dp,
                                                end = 12.dp,
                                                top = 9.dp,
                                                bottom = 9.dp
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = soundFontText(
                                                    "tab_note_names_title",
                                                    currentLang,
                                                    "Hiển thị tên nốt",
                                                    "Show note names"
                                                ),
                                                color = if (showNoteNames) accentColor else textColor,
                                                fontSize = 13.sp,
                                                fontWeight = if (showNoteNames) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.SemiBold
                                                }
                                            )
                                            Text(
                                                text = soundFontText(
                                                    "tab_note_names_desc",
                                                    currentLang,
                                                    "C, D, E, F, G, A, B bên dưới mỗi khuông",
                                                    "C, D, E, F, G, A, B below each staff"
                                                ),
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                lineHeight = 13.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Switch(
                                            checked = showNoteNames,
                                            onCheckedChange = null,
                                            enabled = hasScore && !isExportingPdf
                                        )
                                    }
                                }
                            }

                            // Xuất track đang hiển thị thành PDF A4.
                            IconButton(
                                onClick = { requestPdfExport() },
                                enabled = hasScore && !isExportingPdf,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (hasScore) {
                                            accentColor.copy(alpha = 0.55f)
                                        } else {
                                            Color.Gray.copy(alpha = 0.3f)
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (isExportingPdf) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(19.dp),
                                        color = if (localIsDark) Color.White else accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Xuất PDF A4",
                                        tint = if (hasScore) {
                                            if (localIsDark) Color.White else accentColor
                                        } else {
                                            Color.Gray.copy(alpha = 0.4f)
                                        },
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                        } else {
                            // Khi đang ở danh sách: hiển thị ô tìm kiếm như cũ.
                            BasicTextField(
                                value = currentSearchText,
                                onValueChange = { searchQueries[selectedMusicSection] = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            hideExtraButtons = true
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                textStyle = TextStyle(
                                    color = if (localIsDark) Color.White else textColor,
                                    fontSize = 15.sp
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(if (localIsDark) Color.White else accentColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { searchSongs(section = selectedMusicSection) }
                                ),
                                decorationBox = { innerTextField ->
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (currentSearchText.isEmpty()) {
                                                Text(
                                                    currentSearchHint,
                                                    color = Color.Gray.copy(alpha = 0.8f),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                        if (currentSearchText.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    searchQueries[selectedMusicSection] = ""
                                                    searchSongs(
                                                        section = selectedMusicSection,
                                                        queryOverride = "",
                                                        showNoResultsToast = false
                                                    )
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Xóa",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(
                                                            android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                                                        ).apply {
                                                            putExtra(
                                                                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                                            )
                                                            putExtra(
                                                                android.speech.RecognizerIntent.EXTRA_LANGUAGE,
                                                                if (selectedMusicSection == MusicLibrarySection.QUOC_TE) {
                                                                    "en-US"
                                                                } else {
                                                                    "vi-VN"
                                                                }
                                                            )
                                                            putExtra(
                                                                android.speech.RecognizerIntent.EXTRA_PROMPT,
                                                                currentSearchHint
                                                            )
                                                        }
                                                        speechLauncher.launch(intent)
                                                    } catch (_: Exception) {
                                                        Toast.makeText(
                                                            context,
                                                            "Thiết bị không hỗ trợ giọng nói",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Mic,
                                                    contentDescription = "Giọng nói",
                                                    tint = accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )

                            // Không cần nút tìm riêng vì bàn phím đã có phím Search.
                            // Vị trí này dùng để mở file GP trong thư mục của điện thoại.
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    localFileLauncher.launch(SUPPORTED_GP_MIME_TYPES)
                                },
                                enabled = !isImportingLocalFile && !isLoadingSong,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (isImportingLocalFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(19.dp),
                                        color = if (localIsDark) Color.White else accentColor,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Chọn file GP trong máy",
                                        tint = if (localIsDark) Color.White else accentColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                            }
                        }

                        // CÁC NÚT BÊN PHẢI (Ẩn/Hiện khi Focus vào ô tìm kiếm và CẦN tab nhạc đã hiển thị)
                        if (!hideExtraButtons && hasScore) {
                            // NÚT ĐỔI GIAO DIỆN SÁNG/TỐI
                            IconButton(
                                onClick = { localIsDark = !localIsDark },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = if (localIsDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Giao diện",
                                    tint = if (localIsDark) Color.White else textColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            // NÚT XOAY MÀN HÌNH
                            IconButton(
                                onClick = {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        if (localIsDark) Color.Black else surfaceColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ScreenRotation,
                                    contentDescription = "Xoay màn hình",
                                    tint = if (localIsDark) Color.White else textColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } // <--- ĐÓNG THẺ AnimatedVisibility

                // --- 3 NHÓM NỘI DUNG: NHẠC VIỆT / QUỐC TẾ / ĐIỆU NHẠC ---
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isViewingSong && !(isLandscape && isPlaying),
                    enter = androidx.compose.animation.expandVertically() +
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() +
                            androidx.compose.animation.fadeOut()
                ) {
                    MusicLibraryTabs(
                        selectedSection = selectedMusicSection,
                        currentLang = currentLang,
                        isDark = localIsDark,
                        accentColor = accentColor,
                        onSectionSelected = { selectMusicSection(it) }
                    )
                }

                // Hien thi vi tri hien tai khi dang o trong thu muc Nhac Viet hoac Dieu nhac.
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isViewingSong &&
                            (isInsideVietnamFolder || isInsideRhythmFolder),
                    enter = androidx.compose.animation.expandVertically() +
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically() +
                            androidx.compose.animation.fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.10f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentFolderRootTitle,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  ›  ",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Text(
                            text = currentFolderPathTitle,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- KHUNG HIỂN THỊ CHÍNH ---
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // 1. LỚP DƯỚI CÙNG: WEBVIEW BẢN NHẠC
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            // Tháo WebView khỏi parent cũ (nếu có) trước khi gắn vào View mới của Dialog
                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            webView
                        },
                        update = { target -> syncTabViewport(target) }
                    )

                    // LỚP TRUNG GIAN 1: TRẠNG THÁI ĐANG TẢI / CHƯA CÓ DỮ LIỆU
                    val showCurrentSongList = songs.isNotEmpty()

                    if (!hasScore && !isLoadingSong && !showCurrentSongList) {
                        val vietnamFolderName = selectedVietnamFolder
                            .substringAfterLast('/')
                            .ifBlank { "Vietnam" }
                        val emptyTitle = when {
                            isSearching ->
                                "Đang tải danh sách..."

                            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                                    selectedRhythm != null &&
                                    currentSearchText.isNotBlank() ->
                                "Không tìm thấy bản Tab"

                            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                                    selectedRhythm != null ->
                                "Chưa có bản Tab trong ${selectedRhythm.orEmpty()}"

                            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                                    selectedVietnamFolder.isNotBlank() &&
                                    currentSearchText.isNotBlank() ->
                                "Không tìm thấy bản Tab"

                            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                                    selectedVietnamFolder.isNotBlank() ->
                                "Thư mục $vietnamFolderName đang trống"

                            currentSearchText.isNotBlank() ->
                                "Không tìm thấy nội dung"

                            else -> selectedMusicSection.emptyTitle(currentLang)
                        }
                        val emptyMessage = when {
                            isSearching ->
                                "Vui lòng chờ trong giây lát."

                            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                                    selectedRhythm != null &&
                                    currentSearchText.isNotBlank() ->
                                "Hãy thử tên file khác trong điệu ${selectedRhythm.orEmpty()}."

                            selectedMusicSection == MusicLibrarySection.DIEU_NHAC &&
                                    selectedRhythm != null ->
                                "Thư mục này chưa có file GP3, GP4, GP5, GPX hoặc GP."

                            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                                    selectedVietnamFolder.isNotBlank() &&
                                    currentSearchText.isNotBlank() ->
                                "Hãy thử tên file khác trong thư mục $vietnamFolderName."

                            selectedMusicSection == MusicLibrarySection.NHAC_VIET &&
                                    selectedVietnamFolder.isNotBlank() ->
                                "Thư mục này chưa có thư mục con hoặc file GP được hỗ trợ."

                            currentSearchText.isNotBlank() ->
                                "Hãy thử từ khóa khác hoặc kiểm tra lại nội dung tìm kiếm."

                            else -> selectedMusicSection.emptyMessage(currentLang)
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isSearching) {
                                // Giữ đúng một kiểu loading từ lúc kiểm tra quyền cho tới khi
                                // danh sách đầu tiên trả về. Không hiển thị thêm vòng tròn hay
                                // dòng "Đang tải danh sách..." để tránh cảm giác chạy 2 tiến trình.
                                SongTabLoadingWave(color = Color(0xFF15B27D))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(accentColor.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (
                                            selectedMusicSection == MusicLibrarySection.DIEU_NHAC ||
                                            selectedMusicSection == MusicLibrarySection.NHAC_VIET
                                        ) {
                                            Icons.Default.FolderOpen
                                        } else {
                                            Icons.Default.LibraryMusic
                                        },
                                        contentDescription = null,
                                        tint = accentColor.copy(alpha = 0.82f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = emptyTitle,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = emptyMessage,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // LỚP TRUNG GIAN 2: THANH CÔNG CỤ KÉO THẢ
                    if (tracks.isNotEmpty() && hasScore && songs.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                                .padding(bottom = 40.dp)
                                .zIndex(10f)
                                .alpha(if (isPlaying) 0.15f else 1f)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = if (localIsDark) Color.Black else surfaceColor),
                            border = if (localIsDark) androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color.Gray.copy(alpha = 0.3f)
                            ) else null,
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box {
                                    OutlinedButton(
                                        onClick = { trackMenuExpanded = true },
                                        modifier = Modifier.height(40.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (localIsDark) Color.Black else Color.Transparent,
                                            contentColor = if (localIsDark) Color.White else textColor
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color.Gray.copy(alpha = 0.3f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LibraryMusic,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tracks.getOrNull(selectedTrackIndex)?.name
                                                ?: "N/A",
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.widthIn(max = 90.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = trackMenuExpanded,
                                        onDismissRequest = { trackMenuExpanded = false },
                                        modifier = Modifier.background(surfaceColor)
                                    ) {
                                        tracks.forEachIndexed { index, track ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = track.name,
                                                        color = textColor
                                                    )
                                                },
                                                onClick = {
                                                    trackMenuExpanded = false
                                                    selectTrack(index)
                                                }
                                            )
                                        }
                                    }
                                }

                                Box {
                                    IconButton(
                                        onClick = { soundFontMenuExpanded = true },
                                        enabled = !isPlaying,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (localIsDark) Color.Black else Color.Transparent,
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                Color.Gray.copy(alpha = 0.3f),
                                                CircleShape
                                            )
                                    ) {
                                        if (downloadingSoundFontMode != null || isSwitchingSoundFont) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = if (localIsDark) Color.White else accentColor,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = soundFontText(
                                                    "tab_sound_choose", currentLang,
                                                    "Chọn âm thanh TAB", "Choose TAB audio"
                                                ),
                                                tint = if (
                                                    activeSoundFontMode.isDownloadable
                                                ) {
                                                    accentColor
                                                } else {
                                                    if (localIsDark) Color.White else textColor
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = soundFontMenuExpanded,
                                        onDismissRequest = { soundFontMenuExpanded = false },
                                        modifier = Modifier
                                            .widthIn(min = 300.dp, max = 350.dp)
                                            .background(surfaceColor)
                                    ) {
                                        Text(
                                            text = soundFontText(
                                                "tab_sound_menu_title", currentLang,
                                                "Âm thanh TAB", "TAB audio"
                                            ),
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(
                                                start = 16.dp,
                                                end = 16.dp,
                                                top = 10.dp,
                                                bottom = 6.dp
                                            )
                                        )

                                        // Checkbox hoạt động như lựa chọn đơn: luôn chỉ có một cấu hình được chọn.
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(
                                                    enabled = !isSwitchingSoundFont &&
                                                            downloadingSoundFontMode == null
                                                ) {
                                                    switchSoundFontMode(TabSoundFontMode.STANDARD)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = activeSoundFontMode == TabSoundFontMode.STANDARD,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        switchSoundFontMode(TabSoundFontMode.STANDARD)
                                                    }
                                                },
                                                enabled = !isSwitchingSoundFont &&
                                                        downloadingSoundFontMode == null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = accentColor,
                                                    checkmarkColor = Color.White,
                                                    uncheckedColor = textColor.copy(alpha = 0.48f)
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = TabSoundFontMode.STANDARD.menuTitle(currentLang),
                                                    color = textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = TabSoundFontMode.STANDARD.menuDescription(currentLang),
                                                    color = Color.Gray,
                                                    fontSize = 11.sp,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }

                                        HorizontalDivider(
                                            color = Color.Gray.copy(alpha = 0.18f),
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )

                                        downloadableSoundFontModes.forEachIndexed { index, mode ->
                                            val info = soundFontInfos[mode]
                                            val completeFile = soundFontFiles.getValue(mode)
                                            val isDownloaded = downloadedSoundFontModes[mode] == true
                                            val hasLocalData = localSoundFontDataModes[mode] == true
                                            val isDownloading = downloadingSoundFontMode == mode
                                            val sizeBytes = when {
                                                info?.sizeBytes ?: 0L > 0L -> info?.sizeBytes ?: 0L
                                                completeFile.isFile -> completeFile.length()
                                                else -> 0L
                                            }
                                            val subtitle = when {
                                                isDownloading -> {
                                                    val progress = soundFontDownloadProgressByMode[mode] ?: 0f
                                                    val percent = (progress * 100f)
                                                        .roundToInt()
                                                        .coerceIn(0, 100)
                                                    soundFontFormat(
                                                        "tab_sound_downloading_progress", currentLang,
                                                        "Đang tải %1\$d%% · %2\$s / %3\$s",
                                                        "Downloading %1\$d%% · %2\$s / %3\$s",
                                                        percent,
                                                        formatStorageSize(
                                                            soundFontDownloadedBytesByMode[mode] ?: 0L
                                                        ),
                                                        formatStorageSize(sizeBytes)
                                                    )
                                                }
                                                isDownloaded -> soundFontFormat(
                                                    "tab_sound_downloaded_storage", currentLang,
                                                    "Đã tải · dung lượng app tăng khoảng %1\$s",
                                                    "Downloaded · app storage increased by about %1\$s",
                                                    mode.estimatedAppIncreaseText
                                                )
                                                hasLocalData -> soundFontText(
                                                    "tab_sound_incomplete_continue", currentLang,
                                                    "Tải chưa hoàn tất · nhấn để tiếp tục",
                                                    "Download incomplete · tap to continue"
                                                )
                                                else -> soundFontFormat(
                                                    "tab_sound_increase_storage", currentLang,
                                                    "Tăng dung lượng app lên khoảng %1\$s",
                                                    "Increases app storage by about %1\$s",
                                                    mode.estimatedAppIncreaseText
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 8.dp,
                                                        end = 6.dp,
                                                        top = 5.dp,
                                                        bottom = 5.dp
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable(
                                                            enabled = !isSwitchingSoundFont &&
                                                                    downloadingSoundFontMode == null
                                                        ) {
                                                            if (isDownloaded) {
                                                                switchSoundFontMode(mode)
                                                            } else {
                                                                startSoundFontDownload(mode)
                                                            }
                                                        }
                                                        .padding(vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = activeSoundFontMode == mode,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                if (isDownloaded) {
                                                                    switchSoundFontMode(mode)
                                                                } else {
                                                                    startSoundFontDownload(mode)
                                                                }
                                                            }
                                                        },
                                                        enabled = !isSwitchingSoundFont &&
                                                                downloadingSoundFontMode == null,
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = accentColor,
                                                            checkmarkColor = Color.White,
                                                            uncheckedColor = textColor.copy(alpha = 0.48f)
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = mode.menuTitle(currentLang),
                                                            color = textColor,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        Text(
                                                            text = subtitle,
                                                            color = if (isDownloaded) {
                                                                accentColor.copy(alpha = 0.86f)
                                                            } else {
                                                                Color.Gray
                                                            },
                                                            fontSize = 10.sp,
                                                            lineHeight = 13.sp
                                                        )
                                                    }
                                                }

                                                when {
                                                    isDownloading -> {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(21.dp),
                                                            color = accentColor,
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                    }
                                                    hasLocalData -> {
                                                        IconButton(
                                                            onClick = { deleteSoundFont(mode) },
                                                            enabled = downloadingSoundFontMode == null,
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = soundFontFormat(
                                                                    "tab_sound_delete", currentLang,
                                                                    "Xóa %1\$s", "Delete %1\$s",
                                                                    mode.menuTitle(currentLang)
                                                                ),
                                                                tint = Color(0xFFEF5350),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                    else -> {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier
                                                                .padding(end = 8.dp)
                                                                .size(19.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            soundFontInfoErrors[mode]
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let { errorText ->
                                                    Text(
                                                        text = errorText,
                                                        color = Color(0xFFEF5350),
                                                        fontSize = 10.sp,
                                                        lineHeight = 13.sp,
                                                        modifier = Modifier.padding(
                                                            start = 52.dp,
                                                            end = 16.dp,
                                                            bottom = 6.dp
                                                        )
                                                    )
                                                }

                                            if (index < downloadableSoundFontModes.lastIndex) {
                                                HorizontalDivider(
                                                    color = Color.Gray.copy(alpha = 0.13f),
                                                    modifier = Modifier.padding(horizontal = 12.dp)
                                                )
                                            }
                                        }

                                        if (
                                            isSwitchingSoundFont ||
                                            (soundFontDecodeProgress in 0.001f..0.999f)
                                        ) {
                                            Text(
                                                text = soundFontFormat(
                                                    "tab_sound_prepare_progress", currentLang,
                                                    "Đang chuẩn bị bộ âm thanh %1\$d%%",
                                                    "Preparing audio pack %1\$d%%",
                                                    (soundFontDecodeProgress * 100f)
                                                        .roundToInt()
                                                        .coerceIn(0, 100)
                                                ),
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 6.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        webViewInstance?.evaluateJavascript(
                                            "javascript:playPause();",
                                            null
                                        )
                                    },
                                    enabled = isPlayerReady &&
                                            !isCheckingSoundFontCompatibility &&
                                            !isSwitchingSoundFont,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isPlaying) Color(0xFFE53935) else accentColor
                                    ),
                                    modifier = Modifier.height(40.dp).widthIn(min = 100.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    if (isCheckingSoundFontCompatibility) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(17.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        when {
                                            isCheckingSoundFontCompatibility -> soundFontText(
                                                "tab_sound_checking_compatibility", currentLang,
                                                "Đang kiểm tra...", "Checking..."
                                            )
                                            isPlaying -> "Pause"
                                            else -> "Play"
                                        },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // LỚP TRÊN CÙNG: DANH SÁCH THEO TAB ĐANG CHỌN
                    if (showCurrentSongList) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            shape = RoundedCornerShape(0.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = songs,
                                    key = { "${it.itemType}|${it.id}|${it.fileUrl}|${it.rhythm}|${it.folderPath}" }
                                ) { song ->
                                    val isRhythmSection =
                                        selectedMusicSection == MusicLibrarySection.DIEU_NHAC
                                    val isVietnamSection =
                                        selectedMusicSection == MusicLibrarySection.NHAC_VIET
                                    val isFolder = song.isFolder

                                    // Chỉ tìm ảnh iTunes cho danh sách bài hát thông thường.
                                    // Thư mục/file Điệu nhạc dùng icon cục bộ để tránh gọi mạng thừa.
                                    var thumbnailUrl by remember(song.id) { mutableStateOf("") }
                                    LaunchedEffect(song.id, isRhythmSection, isFolder) {
                                        thumbnailUrl = if (!isRhythmSection && !isFolder) {
                                            getITunesThumbnail(song.title, song.artist)
                                        } else {
                                            ""
                                        }
                                    }

                                    val itemSubtitle = when {
                                        isFolder && isVietnamSection ->
                                            "Chọn để mở thư mục"

                                        isFolder ->
                                            "Chọn để xem các file Guitar Pro"

                                        isRhythmSection -> {
                                            val details = mutableListOf<String>()
                                            song.format
                                                .takeIf { it.isNotBlank() && it != "folder" }
                                                ?.uppercase(Locale.ROOT)
                                                ?.let(details::add)

                                            song.subfolder
                                                .takeIf { it.isNotBlank() }
                                                ?.let(details::add)

                                            if (details.size < 2) {
                                                song.artist
                                                    .takeIf { it.isNotBlank() }
                                                    ?.let(details::add)
                                            }

                                            details.distinct().joinToString(" • ")
                                                .ifBlank { "Bản Tab điệu ${selectedRhythm.orEmpty()}" }
                                        }

                                        isVietnamSection -> {
                                            val details = mutableListOf<String>()
                                            song.format
                                                .takeIf { it.isNotBlank() && it != "folder" }
                                                ?.uppercase(Locale.ROOT)
                                                ?.let(details::add)

                                            song.subfolder
                                                .takeIf { it.isNotBlank() }
                                                ?.split('/')
                                                ?.lastOrNull()
                                                ?.takeIf { it.isNotBlank() }
                                                ?.let(details::add)

                                            if (details.size < 2) {
                                                song.artist
                                                    .takeIf { it.isNotBlank() }
                                                    ?.let(details::add)
                                            }

                                            details.distinct().joinToString(" • ")
                                                .ifBlank { "Nhạc Việt" }
                                        }

                                        else -> song.artist
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(surfaceColor)
                                            .clickable(enabled = !isLoadingSong && !isSearching) {
                                                if (isFolder) {
                                                    when (selectedMusicSection) {
                                                        MusicLibrarySection.NHAC_VIET ->
                                                            openVietnamFolder(song)
                                                        MusicLibrarySection.DIEU_NHAC ->
                                                            openRhythmFolder(song)
                                                        MusicLibrarySection.QUOC_TE -> Unit
                                                    }
                                                } else {
                                                    openSong(song)
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(
                                                    if (isFolder) {
                                                        accentColor.copy(alpha = 0.12f)
                                                    } else {
                                                        bgColor
                                                    },
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clip(RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when {
                                                isFolder -> {
                                                    Icon(
                                                        imageVector = Icons.Default.FolderOpen,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(27.dp)
                                                    )
                                                }

                                                thumbnailUrl.isNotEmpty() -> {
                                                    coil.compose.AsyncImage(
                                                        model = thumbnailUrl,
                                                        contentDescription = null,
                                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                else -> {
                                                    Icon(
                                                        imageVector = Icons.Default.LibraryMusic,
                                                        contentDescription = null,
                                                        tint = accentColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(25.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                color = textColor,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = itemSubtitle,
                                                color = Color.Gray,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (isFolder) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "›",
                                                color = accentColor,
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Light
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Khong chiem chieu cao man hinh khi dang doc / choi TAB.
                // Chi hien banner khi JS xac nhan nguoi dung da cuon den SAU ban nhac.
                // Van giu banner ngoai WebView de khong can thiep AlphaTab va AdMob.
                if (hasScore && showTabBottomBanner) {
                    SongDetailBottomBannerAd(
                        currentLang = currentLang,
                        surfaceColor = if (localIsDark) Color.Black else surfaceColor,
                        textColor = if (localIsDark) Color.White else textColor,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
    // 1. Ẩn thanh trạng thái của Activity gốc (Áp dụng cho CẢ DỌC LẪN NGANG)
    val currentView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, currentView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        onDispose {
            // Đã tắt chức năng khôi phục thanh pin/sóng để ẩn luôn ở trang chủ
        }
    }

    // HIỂN THỊ GIAO DIỆN TÙY THEO CHẾ ĐỘ
    if (isLandscape) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {
                isLandscape = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogView = androidx.compose.ui.platform.LocalView.current

            // 2. Ẩn tiếp thanh trạng thái của chính Dialog đang nổi lên
            DisposableEffect(dialogView) {
                val dialogWindow = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
                if (dialogWindow != null) {
                    val insetsController = androidx.core.view.WindowCompat.getInsetsController(dialogWindow, dialogView)
                    insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
                onDispose {}
            }

            screenContent()
        }
    } else {
        screenContent()
    }
}

// Hàm cung cấp HTML nhúng JS alphaTab
private fun getAlphaTabHtml(
    isDarkInitial: Boolean,
    initialSoundFontMode: String,
    initialSoundFontUrl: String,
    currentLang: String,
    initialShowNoteNames: Boolean
): String {
    val quotedInitialSoundFontMode = JSONObject.quote(initialSoundFontMode)
    val quotedInitialSoundFontUrl = JSONObject.quote(initialSoundFontUrl)
    val quotedStandardSoundFontUrl = JSONObject.quote(STANDARD_SOUNDFONT_URL)
    val quotedSoundFontStartFailed = JSONObject.quote(
        soundFontText(
            "tab_sound_alpha_start_failed", currentLang,
            "AlphaTab không thể bắt đầu nạp SoundFont.",
            "AlphaTab could not start loading the SoundFont."
        )
    )
    val quotedSoundFontLoadFailedPrefix = JSONObject.quote(
        soundFontText(
            "tab_sound_load_failed_prefix", currentLang,
            "Không thể nạp SoundFont: ",
            "Could not load SoundFont: "
        )
    )

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <script src="https://cdn.jsdelivr.net/npm/@coderline/alphatab@1.8.4/dist/alphaTab.js"></script>
            <style id="dynamic-theme-style"></style>
            <style>
                html, body {
                    margin: 0; padding: 0;
                    width: 100%; height: 100%;
                    min-width: 100%; min-height: 100%;
                    background: transparent;
                    overflow: hidden;
                }

                body { position: relative; }

                .wrapper {
                    position: fixed;
                    top: 0; left: 0;
                    width: 100vw; height: 100vh;
                    min-height: 100vh;
                    box-sizing: border-box;
                    overflow-y: auto;
                    overflow-x: hidden;
                    -webkit-overflow-scrolling: touch;
                }

                #alphatab-container {
                    position: relative;
                    width: 100%;
                    box-sizing: border-box;
                    margin-top: -10px;
                    padding-bottom: 60vh;
                    filter: none !important;
                }

                /* Ban TAB ngan van cho phep cuon xuong sau bai de xem banner.
                   Khong ap dung khi xuat PDF de tranh trang giay trang. */
                body:not(.pdf-export-mode) #alphatab-container {
                    min-height: calc(100vh + 150px);
                }

                /* Không tạo một GPU layer khổng lồ cho toàn bộ bài ở chế độ tối. */
                body.tt-tab-dark:not(.pdf-export-mode) #alphatab-container svg:not(.tunertools-note-name-overlay) {
                    filter: invert(1) hue-rotate(180deg);
                }

                body.tt-tab-dark:not(.pdf-export-mode) .tunertools-note-name {
                    fill: #b8c2cd;
                    stroke: rgba(0, 0, 0, 0.92);
                }

                body.tt-tab-dark:not(.pdf-export-mode) .tunertools-note-name.is-active {
                    fill: #d0b9ff;
                }

                body.pdf-export-mode #alphatab-container svg {
                    filter: none !important;
                }

                /*
                 * Một số file tải từ kho SQLite không có metadata title bên trong GP.
                 * Khối này chỉ xuất hiện khi alphaTab không tự vẽ được tiêu đề. Nó vẫn
                 * nằm trong vùng bản nhạc, ngay trên khuông đầu tiên, không nằm ở toolbar.
                 */
                #tunertools-score-title-fallback {
                    display: none;
                    width: 100%;
                    box-sizing: border-box;
                    margin: 0;
                    padding: 4px 14px 3px;
                    overflow: hidden;
                    white-space: nowrap;
                    text-align: center;
                    text-overflow: clip;
                    font-family: Arial, sans-serif;
                    font-weight: 700;
                    line-height: 1.15;
                    color: #202124;
                    transform-origin: center center;
                }

                body.pdf-export-mode #tunertools-score-title-fallback {
                    color: #000000 !important;
                    width: 190mm !important;
                    max-width: 190mm !important;
                }

                .tunertools-note-name-overlay {
                    position: absolute;
                    left: 0;
                    top: 0;
                    overflow: visible;
                    pointer-events: none;
                    z-index: 4;
                }

                .tunertools-note-name {
                    font-family: Arial, sans-serif;
                    font-size: 8.5px;
                    font-weight: 600;
                    text-anchor: middle;
                    fill: #5f6368;
                    paint-order: stroke fill;
                    stroke: rgba(255, 255, 255, 0.90);
                    stroke-width: 1.1px;
                    stroke-linejoin: round;
                }

                .tunertools-note-name.is-active {
                    fill: #5e35b1;
                    font-weight: 700;
                }

                body.pdf-export-mode .tunertools-note-name {
                    fill: #424242;
                    stroke: #ffffff;
                }

                /* Bố cục tạm thời dùng riêng cho WebView PrintDocumentAdapter. */
                html.pdf-export-mode,
                body.pdf-export-mode {
                    width: 190mm !important;
                    min-width: 190mm !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: visible !important;
                    background: #ffffff !important;
                    color-scheme: light !important;
                }

                body.pdf-export-mode .wrapper {
                    position: static !important;
                    width: 190mm !important;
                    min-width: 190mm !important;
                    height: auto !important;
                    min-height: 0 !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    overflow: visible !important;
                    background: #ffffff !important;
                }

                body.pdf-export-mode #alphatab-container {
                    width: 190mm !important;
                    min-width: 190mm !important;
                    margin: 0 !important;
                    padding: 0 !important;
                    filter: none !important;
                    background: #ffffff !important;
                }

                body.pdf-export-mode .at-cursor-bar,
                body.pdf-export-mode .at-cursor-beat {
                    display: none !important;
                }

                @media print {
                    html, body,
                    .wrapper,
                    #alphatab-container {
                        overflow: visible !important;
                        background: #ffffff !important;
                    }

                    #alphatab-container {
                        filter: none !important;
                        padding-bottom: 0 !important;
                    }

                    #alphatab-container svg {
                        filter: none !important;
                    }

                    .at-cursor-bar,
                    .at-cursor-beat {
                        display: none !important;
                    }
                }
            </style>
        </head>
        <body>
            <div class="wrapper" id="scroll-wrapper">
                <div id="tunertools-score-title-fallback" aria-hidden="true"></div>
                <div id="alphatab-container"></div>
            </div>
            <script>
                var currentThemeIsDark = $isDarkInitial;
                var isPdfExportMode = false;
                var pdfExportWaitingForRender = false;
                var pdfExportOriginalState = null;
                var pdfExportReadyTimer = null;
                var pdfExportFallbackTimer = null;
                var alphaTabInitRetryTimer = null;
                var lastAndroidViewportWidth = 0;
                var lastAndroidViewportHeight = 0;
                var lastSyncedCssViewportWidth = 0;

                function syncWebViewViewportBox(androidWidth, androidHeight) {
                    var root = document.documentElement;
                    var body = document.body;
                    var wrapper = document.getElementById('scroll-wrapper');
                    if (!root || !body || !wrapper || isPdfExportMode) {
                        return {
                            width: wrapper ? wrapper.clientWidth : 0,
                            height: wrapper ? wrapper.clientHeight : 0
                        };
                    }

                    var dpr = Math.max(1, Number(window.devicePixelRatio) || 1);
                    var physicalWidth = Math.max(0, Number(androidWidth) || 0);
                    var physicalHeight = Math.max(0, Number(androidHeight) || 0);
                    var cssWidth = Math.round(Number(window.innerWidth) || 0);
                    var cssHeight = Math.round(Number(window.innerHeight) || 0);

                    if (cssWidth <= 1 && physicalWidth > 0) cssWidth = Math.round(physicalWidth / dpr);
                    if (cssHeight <= 1 && physicalHeight > 0) cssHeight = Math.round(physicalHeight / dpr);
                    if (cssWidth <= 1) cssWidth = Math.round(Number(root.clientWidth) || 0);
                    if (cssHeight <= 1) cssHeight = Math.round(Number(root.clientHeight) || 0);
                    if (cssWidth <= 1 || cssHeight <= 1) {
                        return { width: wrapper.clientWidth || 0, height: wrapper.clientHeight || 0 };
                    }

                    root.style.width = cssWidth + 'px';
                    root.style.height = cssHeight + 'px';
                    root.style.minHeight = cssHeight + 'px';
                    body.style.width = cssWidth + 'px';
                    body.style.height = cssHeight + 'px';
                    body.style.minHeight = cssHeight + 'px';
                    wrapper.style.position = 'fixed';
                    wrapper.style.top = '0px';
                    wrapper.style.left = '0px';
                    wrapper.style.right = 'auto';
                    wrapper.style.bottom = 'auto';
                    wrapper.style.width = cssWidth + 'px';
                    wrapper.style.height = cssHeight + 'px';
                    wrapper.style.minHeight = cssHeight + 'px';

                    return { width: wrapper.clientWidth || cssWidth, height: wrapper.clientHeight || cssHeight };
                }

                function onAndroidViewportChanged(width, height) {
                    width = Math.max(0, Math.round(Number(width) || 0));
                    height = Math.max(0, Math.round(Number(height) || 0));
                    if (width > 0) lastAndroidViewportWidth = width;
                    if (height > 0) lastAndroidViewportHeight = height;

                    var previousWidth = lastSyncedCssViewportWidth;
                    var viewport = syncWebViewViewportBox(width, height);
                    if (viewport.width <= 1 || viewport.height <= 1) return;
                    lastSyncedCssViewportWidth = viewport.width;

                    if (!isApiReady || !api) {
                        initAlphaTab(0);
                    } else if (
                        previousWidth > 0 &&
                        previousWidth !== viewport.width &&
                        api.score &&
                        !isPdfExportMode
                    ) {
                        tabBannerLastScoreSvg = null;
                        window.dispatchEvent(new Event('resize'));
                    }
                    // Re-evaluate when Compose inserts/removes the ad and changes
                    // WebView height. The two thresholds prevent visibility loops.
                    if (isApiReady) requestAnimationFrame(updateTabBannerAtScroll);
                }

                // Vi tri banner dua vao khuong nhac SVG cuoi cung, KHONG dua vao
                // padding-bottom 60vh hoac min-height cua #alphatab-container.
                // Hai khoang trong CSS do khong phai la noi dung bai hat.
                var tabBannerVisible = false;
                var tabBannerManuallyScrolled = false;
                var tabBannerPreviousScrollTop = 0;
                var tabBannerLastScoreSvg = null;

                function notifyTabBannerVisibility(visible) {
                    visible = !!visible;
                    if (visible === tabBannerVisible) return;
                    tabBannerVisible = visible;
                    if (window.AndroidInterface &&
                        typeof window.AndroidInterface.onTabBottomBannerVisibilityChanged === 'function') {
                        window.AndroidInterface.onTabBottomBannerVisibilityChanged(visible);
                    }
                }

                function resetTabBannerForScore() {
                    tabBannerManuallyScrolled = false;
                    tabBannerPreviousScrollTop = 0;
                    tabBannerLastScoreSvg = null;
                    notifyTabBannerVisibility(false);
                }

                function findLastScoreSvg(score) {
                    if (tabBannerLastScoreSvg && score.contains(tabBannerLastScoreSvg)) {
                        return tabBannerLastScoreSvg;
                    }
                    // AlphaTab ve cac he khuong thanh SVG. Loai tru overlay ten not
                    // va cac SVG dieu khien nho khong phai khuong nhac.
                    var svgList = score.querySelectorAll('svg:not(.tunertools-note-name-overlay)');
                    for (var i = svgList.length - 1; i >= 0; i--) {
                        var rect = svgList[i].getBoundingClientRect();
                        if (rect.width >= 60 && rect.height >= 20) {
                            tabBannerLastScoreSvg = svgList[i];
                            return tabBannerLastScoreSvg;
                        }
                    }
                    return null;
                }

                function updateTabBannerAtScroll() {
                    var wrapper = document.getElementById('scroll-wrapper');
                    var score = document.getElementById('alphatab-container');
                    if (!wrapper || !score || !api || !api.score ||
                        isScoreLoading || isPdfExportMode) {
                        notifyTabBannerVisibility(false);
                        return;
                    }

                    var lastSvg = findLastScoreSvg(score);
                    if (!lastSvg) {
                        notifyTabBannerVisibility(false);
                        return;
                    }

                    var viewportTop = wrapper.getBoundingClientRect().top;
                    var visibleBottom = wrapper.scrollTop + wrapper.clientHeight;
                    var songEnd = lastSvg.getBoundingClientRect().bottom -
                        viewportTop + wrapper.scrollTop;

                    // Khi phan cuoi khuong nhac da nam trong khung nhin, banner
                    // xuat hien. Khi banner lam WebView thap xuong, giu nguong
                    // an cach xa hon de tranh nhap nhay do resize.
                    var threshold = tabBannerVisible ? songEnd - 200 : songEnd - 18;
                    var maxScrollTop = Math.max(0, wrapper.scrollHeight - wrapper.clientHeight);
                    var minimumScrollTop = Math.min(56, Math.max(16, maxScrollTop - 16));
                    notifyTabBannerVisibility(
                        tabBannerManuallyScrolled &&
                        wrapper.scrollTop >= minimumScrollTop &&
                        visibleBottom >= threshold
                    );
                }

                // Hàm thay đổi giao diện động
                function toggleTheme(isDarkTheme) {
                    currentThemeIsDark = !!isDarkTheme;
                    var container = document.getElementById('alphatab-container');
                    if (container) container.style.filter = 'none';
                    document.body.classList.toggle(
                        'tt-tab-dark',
                        currentThemeIsDark && !isPdfExportMode
                    );

                    var fallbackTitleElement = document.getElementById('tunertools-score-title-fallback');
                    if (fallbackTitleElement) {
                        fallbackTitleElement.style.color = (isPdfExportMode || !currentThemeIsDark)
                            ? '#202124'
                            : '#ffffff';
                    }

                    var style = document.getElementById('dynamic-theme-style');
                    if (style) {
                        if (isDarkTheme) {
                            style.innerHTML = `
                                .at-cursor-bar { background: rgba(0, 230, 118, 0.25) !important; }
                                .at-cursor-beat { background: rgba(255, 82, 82, 0.45) !important; border-left: 4px solid rgba(255, 82, 82, 1) !important; box-shadow: -2px 0 10px rgba(255, 82, 82, 0.8) !important; }
                            `;
                        } else {
                            style.innerHTML = `
                                .at-cursor-bar { background: rgba(255, 242, 0, 0.25) !important; }
                                .at-cursor-beat { background: rgba(64, 114, 203, 0.3) !important; border-left: 4px solid rgba(64, 114, 203, 1) !important; box-shadow: none !important; }
                            `;
                        }
                    }
                }
                
                // Set màu và kích thước thật ngay khi tải lần đầu.
                toggleTheme($isDarkInitial);
                syncWebViewViewportBox(0, 0);

                var api = null;
                var currentTracks = [];
                var selectedTrackForPlayback = 0;
                var isApiReady = false;
                var pendingSoundFontMode = $quotedInitialSoundFontMode;
                var pendingSoundFontTemporaryFallback = false;
                var activeSoundFontMode = null;
                var isSoundFontLoading = true;
                var soundFontStartFailedText = $quotedSoundFontStartFailed;
                var soundFontLoadFailedPrefix = $quotedSoundFontLoadFailedPrefix;
                var standardSoundFontUrl = $quotedStandardSoundFontUrl;
                var isScoreLoading = false;
                var lastKnownPlayerState = 0;
                var autoPlayAfterSoundFontReady = false;
                var autoFallbackInProgress = false;
                var soundFontCompatibilityCheckInProgress = false;
                var soundFontCompatibilityCheckToken = 0;
                var soundFontCompatibilityScoreGeneration = 0;
                var soundFontCompatibilityCache = {};
                var noteNamesVisible = $initialShowNoteNames;
                var noteNameOverlayId = 'tunertools-note-name-overlay';
                var lastPlayedNoteNameBeatId = '';
                var defaultSystemPaddingBottom = 10;
                var defaultLastSystemPaddingBottom = 5;
                var noteNameSystemPaddingBottom = 24;
                var noteNameLastSystemPaddingBottom = 24;

                // Bản sao BPM gốc để mọi lần chỉnh đều tính từ dữ liệu ban đầu,
                // tránh sai số cộng dồn khi người dùng đổi BPM nhiều lần.
                var originalTempoEntries = [];
                var originalBaseBpm = 120;
                var currentDisplayedBpm = 120;

                // Tiêu đề bài hát được đo theo chiều rộng thật của WebView.
                // Font được ghi lại bằng Settings.fillFromJson để alphaTab chuyển chuỗi CSS
                // thành đúng kiểu Font nội bộ trước khi render lại.
                var currentScoreTitle = 'Bài nhạc';
                var scoreTitleFallbackId = 'tunertools-score-title-fallback';
                // Tên lấy từ danh sách Android chỉ được dùng khi file GP không có metadata title.
                // Tiêu đề vẫn được render bên trong đầu bản nhạc của alphaTab như trước đây.
                var pendingScoreTitle = '';
                var scrollToTopAfterNextRender = false;
                var currentTitleFontSize = 18;
                var lastResponsiveTitleFont = '';
                var titleResizeTimer = null;
                var titlePostRenderTimer = null;
                var isCorrectingRenderedTitle = false;

                function getTitleViewportMetrics() {
                    var wrapper = document.getElementById('scroll-wrapper');
                    var viewportWidth = wrapper && wrapper.clientWidth > 0
                        ? wrapper.clientWidth
                        : (window.innerWidth || document.documentElement.clientWidth || 320);

                    // Giữ một khoảng an toàn ở cả hai cạnh màn hình.
                    var horizontalSafeArea = viewportWidth <= 340 ? 24 : 32;
                    return {
                        wrapper: wrapper,
                        viewportWidth: viewportWidth,
                        maxTitleWidth: Math.max(88, viewportWidth - horizontalSafeArea)
                    };
                }

                function measureTitleWidth(title, fontSize) {
                    try {
                        var canvas = document.createElement('canvas');
                        var context = canvas.getContext('2d');
                        if (!context) return NaN;
                        context.font = 'bold ' + fontSize + 'px Arial, sans-serif';
                        return context.measureText(String(title || '')).width;
                    } catch (_) {
                        return NaN;
                    }
                }

                function calculateResponsiveTitleFont(title) {
                    var metrics = getTitleViewportMetrics();
                    var safeTitle = String(title || 'Bài nhạc').replace(/\s+/g, ' ').trim();

                    // Màn hình dọc dùng cỡ tối đa nhỏ hơn; màn hình ngang vẫn có thể hiển thị lớn.
                    var maxFontSize = metrics.viewportWidth >= 600
                        ? 26
                        : (metrics.viewportWidth >= 400 ? 22 : 20);
                    var minFontSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                    var targetWidth = metrics.maxTitleWidth * 0.94;

                    var measuredAtMax = measureTitleWidth(safeTitle, maxFontSize);
                    if (!Number.isFinite(measuredAtMax) || measuredAtMax <= 0) {
                        // Dự phòng cho WebView rất cũ không đo được canvas text.
                        var estimated = targetWidth / Math.max(1, safeTitle.length * 0.58);
                        return Math.max(
                            minFontSize,
                            Math.min(maxFontSize, Math.floor(estimated * 4) / 4)
                        );
                    }

                    if (measuredAtMax <= targetWidth) {
                        return maxFontSize;
                    }

                    // Tìm nhị phân theo bước 0.25px để tên dài co vừa chính xác hơn.
                    var low = minFontSize;
                    var high = maxFontSize;
                    var best = minFontSize;
                    for (var i = 0; i < 14; i++) {
                        var middle = (low + high) / 2;
                        var measured = measureTitleWidth(safeTitle, middle);
                        if (Number.isFinite(measured) && measured <= targetWidth) {
                            best = middle;
                            low = middle;
                        } else {
                            high = middle;
                        }
                    }

                    return Math.max(
                        minFontSize,
                        Math.min(maxFontSize, Math.floor(best * 4) / 4)
                    );
                }

                function createAlphaTabFont(cssFont) {
                    try {
                        if (
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.model &&
                            alphaTab.model.Font &&
                            typeof alphaTab.model.Font.fromJson === 'function'
                        ) {
                            return alphaTab.model.Font.fromJson(cssFont);
                        }
                    } catch (_) {
                        // Settings.fillFromJson phía dưới là nhánh chính.
                    }
                    return null;
                }

                function ensureScoreTitleSettingVisible() {
                    if (!api || !api.settings || !api.settings.notation) return false;

                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                notation: {
                                    elements: { scoreTitle: true }
                                }
                            });
                            return true;
                        }
                    } catch (_) {
                        // Tiếp tục sang nhánh tương thích bên dưới.
                    }

                    try {
                        if (
                            api.settings.notation.elements &&
                            typeof api.settings.notation.elements.set === 'function' &&
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.NotationElement
                        ) {
                            api.settings.notation.elements.set(
                                alphaTab.NotationElement.ScoreTitle,
                                true
                            );
                            return true;
                        }
                    } catch (_) {
                        // Không chặn việc render nếu bản alphaTab cũ không có Map.set.
                    }

                    return false;
                }

                function updateAlphaTabHeaderSettings(titleFont) {
                    if (!api || !api.settings || !api.settings.display) return false;

                    // Luôn bật lại ScoreTitle để việc đổi chế độ TAB/tiết tấu không làm mất
                    // tiêu đề ở phần đầu bản nhạc.
                    ensureScoreTitleSettingVisible();

                    // Đây là cách an toàn cho bản Web: chuỗi CSS được parser của alphaTab
                    // chuyển thành Font nội bộ, thay vì gán thẳng String vào thuộc tính Font.
                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    padding: [12, 10, 12, 24],
                                    resources: {
                                        titleFont: titleFont,
                                        subTitleFont: '14px Arial, sans-serif',
                                        wordsFont: '12px Arial, sans-serif',
                                        copyrightFont: 'bold 10px Arial, sans-serif'
                                    }
                                },
                                notation: {
                                    elements: { scoreTitle: true }
                                }
                            });
                            return true;
                        }
                    } catch (_) {
                        // Tiếp tục sang nhánh tương thích bên dưới.
                    }

                    var resources = api.settings.display.resources;
                    if (!resources) return false;

                    var parsedTitleFont = createAlphaTabFont(titleFont);
                    var parsedSubTitleFont = createAlphaTabFont('14px Arial, sans-serif');
                    var parsedWordsFont = createAlphaTabFont('12px Arial, sans-serif');
                    var parsedCopyrightFont = createAlphaTabFont('bold 10px Arial, sans-serif');

                    resources.titleFont = parsedTitleFont || titleFont;
                    resources.subTitleFont = parsedSubTitleFont || resources.subTitleFont;
                    resources.wordsFont = parsedWordsFont || resources.wordsFont;
                    resources.copyrightFont = parsedCopyrightFont || resources.copyrightFont;
                    api.settings.display.padding = [12, 10, 12, 24];
                    return true;
                }

                function applyResponsiveSongHeader(score, renderAfterUpdate, forcedFontSize) {
                    if (!api || !api.settings || !score) return false;

                    var titleFromFile = String(score.title || '').replace(/\s+/g, ' ').trim();
                    var fallbackTitle = String(pendingScoreTitle || '').replace(/\s+/g, ' ').trim();
                    currentScoreTitle = titleFromFile || fallbackTitle || 'Bài nhạc';

                    var scoreTitleChanged = false;

                    // Một số file GP tải từ SQLite không lưu metadata title. Ghi tên bài
                    // từ kết quả tìm kiếm vào Score trước khi alphaTab render khuông đầu.
                    if (!titleFromFile && fallbackTitle) {
                        try {
                            score.title = currentScoreTitle;
                            scoreTitleChanged = true;
                        } catch (_) {
                            // WebView cũ có thể dùng model chỉ đọc; DOM fallback phía dưới sẽ xử lý.
                        }
                    }

                    ensureScoreTitleSettingVisible();
                    var calculatedSize = Number.isFinite(Number(forcedFontSize))
                        ? Number(forcedFontSize)
                        : calculateResponsiveTitleFont(currentScoreTitle);
                    var metrics = getTitleViewportMetrics();
                    var minimumSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                    var maximumSize = metrics.viewportWidth >= 600
                        ? 26
                        : (metrics.viewportWidth >= 400 ? 22 : 20);
                    var fontSize = Math.max(
                        minimumSize,
                        Math.min(maximumSize, Math.floor(calculatedSize * 4) / 4)
                    );
                    var titleFont = 'bold ' + fontSize + 'px Arial, sans-serif';
                    var fontChanged = lastResponsiveTitleFont !== titleFont;

                    if (fontChanged) {
                        if (!updateAlphaTabHeaderSettings(titleFont)) return false;
                        currentTitleFontSize = fontSize;
                        lastResponsiveTitleFont = titleFont;
                    } else {
                        // Dù font không đổi, title vừa chèn vẫn cần buộc alphaTab nhận settings mới.
                        currentTitleFontSize = fontSize;
                    }

                    if (fontChanged || scoreTitleChanged) {
                        api.updateSettings();
                    }

                    if (renderAfterUpdate && api.score) {
                        api.render();
                    }
                    return fontChanged || scoreTitleChanged;
                }

                function normalizeRenderedText(value) {
                    return String(value || '').replace(/\s+/g, ' ').trim();
                }

                function hideFallbackScoreTitle() {
                    var element = document.getElementById(scoreTitleFallbackId);
                    if (element) {
                        element.style.display = 'none';
                        element.style.transform = '';
                        element.textContent = '';
                    }

                    var container = document.getElementById('alphatab-container');
                    if (container && !isPdfExportMode) {
                        container.style.marginTop = '-10px';
                    }
                }

                function showFallbackScoreTitle() {
                    var title = normalizeRenderedText(currentScoreTitle);
                    if (!title || title === 'Bài nhạc') {
                        hideFallbackScoreTitle();
                        return;
                    }

                    var element = document.getElementById(scoreTitleFallbackId);
                    if (!element) return;

                    element.textContent = title;
                    element.style.display = 'block';
                    element.style.fontSize = currentTitleFontSize + 'px';
                    element.style.color = (isPdfExportMode || !currentThemeIsDark)
                        ? '#202124'
                        : '#ffffff';
                    element.style.transform = '';

                    var container = document.getElementById('alphatab-container');
                    if (container) {
                        // Bỏ margin âm để khuông đầu tiên không đè lên tiêu đề dự phòng.
                        container.style.marginTop = isPdfExportMode ? '0px' : '-2px';
                    }

                    // Phòng tên quá dài trên màn hình nhỏ: co ngang nhưng không cắt chữ.
                    requestAnimationFrame(function() {
                        if (!element || element.style.display === 'none') return;
                        var availableWidth = Math.max(88, element.clientWidth - 28);
                        var contentWidth = element.scrollWidth;
                        if (contentWidth > availableWidth && contentWidth > 0) {
                            var scaleX = Math.max(0.55, Math.min(1, availableWidth / contentWidth));
                            element.style.transform = 'scaleX(' + scaleX + ')';
                        }
                    });
                }

                function findRenderedTitleElement() {
                    var expectedTitle = normalizeRenderedText(currentScoreTitle);
                    if (!expectedTitle) return null;

                    var textNodes = document.querySelectorAll('#alphatab-container svg text');
                    for (var i = 0; i < textNodes.length; i++) {
                        if (normalizeRenderedText(textNodes[i].textContent) === expectedTitle) {
                            return textNodes[i];
                        }
                    }
                    return null;
                }

                function verifyRenderedTitleFits() {
                    clearTimeout(titlePostRenderTimer);
                    titlePostRenderTimer = setTimeout(function() {
                        if (!api || !api.score || isCorrectingRenderedTitle) return;

                        var titleElement = findRenderedTitleElement();
                        if (!titleElement) {
                            showFallbackScoreTitle();
                            return;
                        }

                        hideFallbackScoreTitle();

                        var metrics = getTitleViewportMetrics();
                        var wrapper = metrics.wrapper;
                        if (!wrapper) return;

                        var titleRect = titleElement.getBoundingClientRect();
                        var wrapperRect = wrapper.getBoundingClientRect();
                        var leftLimit = wrapperRect.left + 8;
                        var rightLimit = wrapperRect.right - 8;
                        var isOverflowing =
                            titleRect.width > metrics.maxTitleWidth ||
                            titleRect.left < leftLimit ||
                            titleRect.right > rightLimit;

                        var minimumSize = metrics.viewportWidth <= 320 ? 7.5 : 8;
                        if (!isOverflowing) return;

                        // Với tên đặc biệt dài, khi đã chạm cỡ chữ tối thiểu thì nén ngang
                        // riêng dòng tiêu đề để vẫn hiển thị đủ, không cắt ký tự hay xuống dòng.
                        if (currentTitleFontSize <= minimumSize + 0.01) {
                            var finalScale = Math.min(
                                1,
                                (metrics.maxTitleWidth / Math.max(1, titleRect.width)) * 0.96
                            );
                            try {
                                var naturalSvgLength = titleElement.getComputedTextLength();
                                if (Number.isFinite(naturalSvgLength) && naturalSvgLength > 0) {
                                    titleElement.setAttribute(
                                        'textLength',
                                        String(naturalSvgLength * finalScale)
                                    );
                                    titleElement.setAttribute('lengthAdjust', 'spacingAndGlyphs');
                                } else {
                                    throw new Error('Không đọc được chiều dài SVG');
                                }
                            } catch (_) {
                                titleElement.style.transformBox = 'fill-box';
                                titleElement.style.transformOrigin = 'center center';
                                titleElement.style.transform = 'scaleX(' + finalScale + ')';
                            }
                            return;
                        }

                        // Đo trực tiếp phần SVG đã render để bù sai số font/devicePixelRatio.
                        var widthRatio = metrics.maxTitleWidth / Math.max(1, titleRect.width);
                        var nextSize = Math.floor(
                            Math.max(minimumSize, currentTitleFontSize * widthRatio * 0.92) * 4
                        ) / 4;
                        if (nextSize >= currentTitleFontSize) {
                            nextSize = Math.max(minimumSize, currentTitleFontSize - 0.5);
                        }
                        if (Math.abs(nextSize - currentTitleFontSize) < 0.24) return;

                        isCorrectingRenderedTitle = true;
                        applyResponsiveSongHeader(api.score, true, nextSize);
                        setTimeout(function() {
                            isCorrectingRenderedTitle = false;
                            verifyRenderedTitleFits();
                        }, 420);
                    }, 80);
                }

                var isUserScrolling = false;
                var scrollTimeout = null;
                
                function ensureInitialTempoAutomation(score) {
                    if (!score || !score.masterBars || score.masterBars.length === 0) return;

                    var firstMasterBar = score.masterBars[0];
                    if (!firstMasterBar.tempoAutomations) {
                        firstMasterBar.tempoAutomations = [];
                    }

                    if (firstMasterBar.tempoAutomations.length === 0) {
                        var initialValue = Number(score.tempo);
                        if (!Number.isFinite(initialValue) || initialValue <= 0) {
                            initialValue = 120;
                        }

                        var automation = null;
                        try {
                            if (
                                typeof alphaTab !== 'undefined' &&
                                alphaTab.model &&
                                alphaTab.model.Automation
                            ) {
                                automation = new alphaTab.model.Automation();
                            }
                        } catch (_) {
                            automation = null;
                        }

                        // Dạng object này vẫn chứa đầy đủ các thuộc tính mà renderer/MIDI cần.
                        if (!automation) {
                            automation = {};
                        }
                        automation.type = 0;
                        automation.isLinear = false;
                        automation.value = initialValue;
                        automation.ratioPosition = 0;
                        automation.text = '';
                        automation.isVisible = true;
                        firstMasterBar.tempoAutomations.push(automation);
                    } else {
                        // Yêu cầu của màn hình này là luôn nhìn thấy BPM đầu bài.
                        firstMasterBar.tempoAutomations[0].isVisible = true;
                    }
                }

                function captureOriginalTempos(score) {
                    originalTempoEntries = [];
                    ensureInitialTempoAutomation(score);

                    if (score && score.masterBars) {
                        for (var barIndex = 0; barIndex < score.masterBars.length; barIndex++) {
                            var masterBar = score.masterBars[barIndex];
                            var automations = masterBar.tempoAutomations || [];
                            for (var automationIndex = 0; automationIndex < automations.length; automationIndex++) {
                                var automation = automations[automationIndex];
                                var value = Number(automation.value);
                                if (Number.isFinite(value) && value > 0) {
                                    originalTempoEntries.push({
                                        automation: automation,
                                        value: value
                                    });
                                }
                            }
                        }
                    }

                    originalBaseBpm = Number(score && score.tempo);
                    if (!Number.isFinite(originalBaseBpm) || originalBaseBpm <= 0) {
                        originalBaseBpm = originalTempoEntries.length > 0
                            ? originalTempoEntries[0].value
                            : 120;
                    }

                    currentDisplayedBpm = Math.round(originalBaseBpm);
                    AndroidInterface.onTempoChanged(currentDisplayedBpm);
                }

                function setScoreBpm(targetBpm) {
                    try {
                        targetBpm = Math.round(Number(targetBpm));
                        if (!Number.isFinite(targetBpm) || targetBpm < 20 || targetBpm > 400) {
                            AndroidInterface.onTempoChangeFailed('BPM phải nằm trong khoảng 20 đến 400.');
                            return false;
                        }
                        if (!api || !api.score || originalTempoEntries.length === 0) {
                            AndroidInterface.onTempoChangeFailed('Bản nhạc chưa sẵn sàng để đổi BPM.');
                            return false;
                        }

                        var ratio = targetBpm / originalBaseBpm;
                        for (var i = 0; i < originalTempoEntries.length; i++) {
                            var entry = originalTempoEntries[i];
                            entry.automation.value = Math.max(
                                1,
                                Math.round(entry.value * ratio)
                            );
                        }

                        // Đảm bảo BPM đầu bài chính xác tuyệt đối với số người dùng nhập.
                        if (
                            api.score.masterBars &&
                            api.score.masterBars.length > 0 &&
                            api.score.masterBars[0].tempoAutomations &&
                            api.score.masterBars[0].tempoAutomations.length > 0
                        ) {
                            api.score.masterBars[0].tempoAutomations[0].value = targetBpm;
                            api.score.masterBars[0].tempoAutomations[0].isVisible = true;
                        }

                        // BPM đã được ghi thẳng vào data model nên giữ playbackSpeed = 100%.
                        // Re-render cập nhật con số tempo trên khuông nhạc;
                        // loadMidiForScore tạo lại MIDI để tốc độ nghe khớp với con số đó.
                        api.stop();
                        api.playbackSpeed = 1.0;
                        api.render();
                        if (typeof api.loadMidiForScore === 'function') {
                            api.loadMidiForScore();
                        }

                        currentDisplayedBpm = targetBpm;
                        AndroidInterface.onTempoChanged(targetBpm);
                        return true;
                    } catch (error) {
                        AndroidInterface.onTempoChangeFailed(
                            'Không thể đổi BPM: ' + (error && error.message ? error.message : error)
                        );
                        return false;
                    }
                }


                function clearNoteNameOverlay() {
                    var existing = document.getElementById(noteNameOverlayId);
                    if (existing && existing.parentNode) {
                        existing.parentNode.removeChild(existing);
                    }
                }

                function noteNameBeatId(beat) {
                    if (!beat) return '';

                    var beatId = Number(beat.id);
                    if (Number.isFinite(beatId)) {
                        return String(Math.round(beatId));
                    }

                    var displayStart = Number(beat.absoluteDisplayStart);
                    var beatIndex = Number(beat.index);
                    if (Number.isFinite(displayStart)) {
                        return 's' + String(Math.round(displayStart)) +
                            'i' + String(Number.isFinite(beatIndex) ? Math.round(beatIndex) : 0);
                    }
                    return '';
                }

                function selectedTrackForNoteNames() {
                    if (!currentTracks || currentTracks.length === 0) return null;
                    var safeIndex = Math.max(
                        0,
                        Math.min(selectedTrackForPlayback, currentTracks.length - 1)
                    );
                    return currentTracks[safeIndex] || null;
                }

                function trackOfBeatForNoteNames(beat) {
                    try {
                        return beat && beat.voice && beat.voice.bar && beat.voice.bar.staff
                            ? beat.voice.bar.staff.track
                            : null;
                    } catch (_) {
                        return null;
                    }
                }

                function beatBelongsToSelectedTrack(beat, selectedTrack) {
                    if (!beat || !selectedTrack) return true;
                    var beatTrack = trackOfBeatForNoteNames(beat);
                    if (!beatTrack) return true;
                    if (beatTrack === selectedTrack) return true;

                    var beatTrackIndex = Number(beatTrack.index);
                    var selectedTrackIndex = Number(selectedTrack.index);
                    return Number.isFinite(beatTrackIndex) &&
                        Number.isFinite(selectedTrackIndex) &&
                        beatTrackIndex === selectedTrackIndex;
                }

                function beatUsesFlatNoteNames(beat) {
                    try {
                        var bar = beat && beat.voice ? beat.voice.bar : null;
                        var masterBar = bar ? bar.masterBar : null;
                        var keySignature = Number(masterBar ? masterBar.keySignature : 0);
                        return Number.isFinite(keySignature) && keySignature < 0;
                    } catch (_) {
                        return false;
                    }
                }

                function notePitchForName(note) {
                    if (!note || note.isVisible === false || note.isDead === true || note.isPercussion === true) {
                        return null;
                    }

                    var pitch = NaN;
                    try {
                        if (typeof note.calculateRealValue === 'function') {
                            pitch = Number(note.calculateRealValue(true, true));
                        }
                    } catch (_) {
                        pitch = NaN;
                    }

                    if (!Number.isFinite(pitch)) pitch = Number(note.realValue);
                    if (!Number.isFinite(pitch)) pitch = Number(note.displayValue);
                    if (!Number.isFinite(pitch)) return null;
                    return Math.round(pitch);
                }

                function noteKeyForName(note, noteIndex, pitch) {
                    var noteId = Number(note ? note.id : NaN);
                    if (Number.isFinite(noteId)) {
                        return 'id' + String(Math.round(noteId));
                    }

                    var stringNumber = Number(note ? note.string : NaN);
                    return 's' + (Number.isFinite(stringNumber) ? String(Math.round(stringNumber)) : 'x') +
                        'p' + String(Math.round(pitch)) +
                        'i' + String(noteIndex);
                }

                function collectBeatNoteNameEntries(beat) {
                    if (!beat || beat.isRest === true || beat.isEmpty === true || !beat.notes) {
                        return [];
                    }

                    var useFlats = beatUsesFlatNoteNames(beat);
                    var sharpNames = ['C', 'C\u266f', 'D', 'D\u266f', 'E', 'F', 'F\u266f', 'G', 'G\u266f', 'A', 'A\u266f', 'B'];
                    var flatNames = ['C', 'D\u266d', 'D', 'E\u266d', 'E', 'F', 'G\u266d', 'G', 'A\u266d', 'A', 'B\u266d', 'B'];
                    var selectedNames = useFlats ? flatNames : sharpNames;
                    var entries = [];

                    // Giữ từng nốt vật lý của hợp âm, kể cả khi nhiều dây tạo ra
                    // cùng một tên nốt ở các quãng tám khác nhau. Ví dụ hợp âm 6 dây
                    // E-B-E-G#-B-E phải hiển thị đủ 6 tên, không rút gọn còn E-G#-B.
                    for (var noteIndex = 0; noteIndex < beat.notes.length; noteIndex++) {
                        var note = beat.notes[noteIndex];
                        var pitch = notePitchForName(note);
                        if (!Number.isFinite(pitch)) continue;

                        var pitchClass = ((pitch % 12) + 12) % 12;
                        var stringNumber = Number(note ? note.string : NaN);
                        entries.push({
                            pitch: pitch,
                            name: selectedNames[pitchClass],
                            stringNumber: Number.isFinite(stringNumber) ? stringNumber : NaN,
                            noteIndex: noteIndex,
                            key: noteKeyForName(note, noteIndex, pitch)
                        });
                    }

                    // Đọc từ âm thấp lên âm cao; nếu hai nốt đồng âm thì giữ thứ tự dây.
                    entries.sort(function(left, right) {
                        if (left.pitch !== right.pitch) return left.pitch - right.pitch;
                        if (
                            Number.isFinite(left.stringNumber) &&
                            Number.isFinite(right.stringNumber) &&
                            left.stringNumber !== right.stringNumber
                        ) {
                            return right.stringNumber - left.stringNumber;
                        }
                        return left.noteIndex - right.noteIndex;
                    });
                    return entries;
                }

                function updateActiveNoteNameBeat(beat) {
                    lastPlayedNoteNameBeatId = noteNameBeatId(beat);
                    var labels = document.querySelectorAll('.tunertools-note-name');
                    var searchToken = lastPlayedNoteNameBeatId
                        ? '|' + lastPlayedNoteNameBeatId + '|'
                        : '';

                    for (var labelIndex = 0; labelIndex < labels.length; labelIndex++) {
                        var label = labels[labelIndex];
                        var beatIds = label.getAttribute('data-beat-ids') || '';
                        var isActive = searchToken && beatIds.indexOf(searchToken) >= 0;
                        if (isActive) {
                            label.classList.add('is-active');
                        } else {
                            label.classList.remove('is-active');
                        }
                    }
                }

                function renderNoteNameOverlay() {
                    clearNoteNameOverlay();
                    if (!noteNamesVisible || !api || !api.score) return;

                    var lookup = api.boundsLookup ||
                        (api.renderer ? api.renderer.boundsLookup : null);
                    if (!lookup || !lookup.staffSystems || lookup.staffSystems.length === 0) return;

                    var container = document.getElementById('alphatab-container');
                    if (!container) return;

                    var selectedTrack = selectedTrackForNoteNames();
                    var svgNamespace = 'http://www.w3.org/2000/svg';
                    var overlay = document.createElementNS(svgNamespace, 'svg');
                    overlay.setAttribute('id', noteNameOverlayId);
                    overlay.setAttribute('class', 'tunertools-note-name-overlay');
                    overlay.setAttribute('aria-hidden', 'true');

                    var maximumRight = Math.max(container.clientWidth || 0, container.scrollWidth || 0);
                    var maximumBottom = Math.max(container.clientHeight || 0, container.scrollHeight || 0);
                    var labelsCreated = 0;

                    for (var systemIndex = 0; systemIndex < lookup.staffSystems.length; systemIndex++) {
                        var systemBounds = lookup.staffSystems[systemIndex];
                        if (!systemBounds) continue;

                        var visualBounds = systemBounds.visualBounds || systemBounds.realBounds;
                        var realBounds = systemBounds.realBounds || visualBounds;
                        if (!visualBounds || !realBounds) continue;

                        maximumRight = Math.max(
                            maximumRight,
                            Number(realBounds.x || 0) + Number(realBounds.w || 0)
                        );
                        maximumBottom = Math.max(
                            maximumBottom,
                            Number(realBounds.y || 0) + Number(realBounds.h || 0)
                        );

                        var visualBottom = Number(visualBounds.y || 0) + Number(visualBounds.h || 0);
                        var labelBaseline = visualBottom + 12;
                        maximumBottom = Math.max(maximumBottom, labelBaseline + 4);

                        var groupsByX = {};
                        // Cùng một beat có thể xuất hiện trong bounds của cả khuông nhạc
                        // và TAB. Chỉ lấy một lần để không nhân đôi tên nốt.
                        var processedBeatIds = {};
                        var masterBarBoundsList = systemBounds.bars || [];
                        for (var masterIndex = 0; masterIndex < masterBarBoundsList.length; masterIndex++) {
                            var masterBarBounds = masterBarBoundsList[masterIndex];
                            var barBoundsList = masterBarBounds && masterBarBounds.bars
                                ? masterBarBounds.bars
                                : [];

                            for (var barIndex = 0; barIndex < barBoundsList.length; barIndex++) {
                                var barBounds = barBoundsList[barIndex];
                                var beatBoundsList = barBounds && barBounds.beats
                                    ? barBounds.beats
                                    : [];

                                for (var beatIndex = 0; beatIndex < beatBoundsList.length; beatIndex++) {
                                    var beatBounds = beatBoundsList[beatIndex];
                                    var beat = beatBounds ? beatBounds.beat : null;
                                    if (!beatBelongsToSelectedTrack(beat, selectedTrack)) continue;

                                    var beatId = noteNameBeatId(beat);
                                    if (
                                        beatId &&
                                        Object.prototype.hasOwnProperty.call(processedBeatIds, beatId)
                                    ) {
                                        continue;
                                    }

                                    var entries = collectBeatNoteNameEntries(beat);
                                    if (entries.length === 0) continue;
                                    if (beatId) processedBeatIds[beatId] = true;

                                    var x = Number(beatBounds.onNotesX);
                                    if (!Number.isFinite(x)) {
                                        var beatVisualBounds = beatBounds.visualBounds || beatBounds.realBounds;
                                        if (!beatVisualBounds) continue;
                                        x = Number(beatVisualBounds.x || 0) +
                                            Number(beatVisualBounds.w || 0) / 2;
                                    }

                                    var roundedX = Math.round(x * 2) / 2;
                                    var groupKey = String(roundedX);
                                    var group = groupsByX[groupKey];
                                    if (!group) {
                                        group = {
                                            x: roundedX,
                                            entries: [],
                                            entryKeys: {},
                                            beatIds: {}
                                        };
                                        groupsByX[groupKey] = group;
                                    }

                                    for (var entryIndex = 0; entryIndex < entries.length; entryIndex++) {
                                        var entry = entries[entryIndex];
                                        var entryKey = (beatId || ('x' + groupKey)) + '|' + entry.key;
                                        if (Object.prototype.hasOwnProperty.call(group.entryKeys, entryKey)) {
                                            continue;
                                        }
                                        group.entryKeys[entryKey] = true;
                                        group.entries.push(entry);
                                    }

                                    if (beatId) group.beatIds[beatId] = true;
                                }
                            }
                        }

                        var groups = [];
                        for (var groupProperty in groupsByX) {
                            if (Object.prototype.hasOwnProperty.call(groupsByX, groupProperty)) {
                                groups.push(groupsByX[groupProperty]);
                            }
                        }
                        groups.sort(function(left, right) { return left.x - right.x; });

                        for (var groupIndex = 0; groupIndex < groups.length; groupIndex++) {
                            var currentGroup = groups[groupIndex];
                            var groupEntries = (currentGroup.entries || []).slice();
                            groupEntries.sort(function(left, right) {
                                if (left.pitch !== right.pitch) return left.pitch - right.pitch;
                                if (
                                    Number.isFinite(left.stringNumber) &&
                                    Number.isFinite(right.stringNumber) &&
                                    left.stringNumber !== right.stringNumber
                                ) {
                                    return right.stringNumber - left.stringNumber;
                                }
                                return left.noteIndex - right.noteIndex;
                            });

                            var nameParts = [];
                            for (var nameIndex = 0; nameIndex < groupEntries.length; nameIndex++) {
                                nameParts.push(groupEntries[nameIndex].name);
                            }
                            if (nameParts.length === 0) continue;

                            // Không loại tên trùng: mỗi dây/nốt của hợp âm được thể hiện riêng.
                            var labelText = nameParts.join('\u00b7');
                            var leftBoundary = groupIndex > 0
                                ? (groups[groupIndex - 1].x + currentGroup.x) / 2
                                : Number(realBounds.x || 0);
                            var rightBoundary = groupIndex < groups.length - 1
                                ? (currentGroup.x + groups[groupIndex + 1].x) / 2
                                : Number(realBounds.x || 0) + Number(realBounds.w || 0);
                            var availableWidth = Math.max(7, rightBoundary - leftBoundary - 2);
                            var baseFontSize = nameParts.length >= 5 ? 7.8 : 8.5;
                            var minimumFontSize = nameParts.length >= 5 ? 5.6 : 6.2;
                            var estimatedWidth = Math.max(1, labelText.length * baseFontSize * 0.57);
                            var fontSize = estimatedWidth > availableWidth
                                ? Math.max(minimumFontSize, baseFontSize * availableWidth / estimatedWidth)
                                : baseFontSize;

                            var textNode = document.createElementNS(svgNamespace, 'text');
                            textNode.setAttribute('class', 'tunertools-note-name');
                            textNode.setAttribute('x', String(currentGroup.x));
                            textNode.setAttribute('y', String(labelBaseline));
                            textNode.style.fontSize = String(fontSize) + 'px';
                            textNode.textContent = labelText;

                            var beatIdParts = [];
                            for (var beatIdKey in currentGroup.beatIds) {
                                if (Object.prototype.hasOwnProperty.call(currentGroup.beatIds, beatIdKey)) {
                                    beatIdParts.push(beatIdKey);
                                }
                            }
                            textNode.setAttribute(
                                'data-beat-ids',
                                '|' + beatIdParts.join('|') + '|'
                            );

                            var adjustedEstimatedWidth = Math.max(
                                1,
                                labelText.length * fontSize * 0.57
                            );
                            if (adjustedEstimatedWidth > availableWidth && availableWidth > 4) {
                                textNode.setAttribute('textLength', String(availableWidth));
                                textNode.setAttribute('lengthAdjust', 'spacingAndGlyphs');
                            }

                            overlay.appendChild(textNode);
                            labelsCreated++;
                        }
                    }

                    if (labelsCreated === 0) return;

                    var overlayWidth = Math.max(1, Math.ceil(maximumRight));
                    var overlayHeight = Math.max(1, Math.ceil(maximumBottom));
                    overlay.setAttribute('width', String(overlayWidth));
                    overlay.setAttribute('height', String(overlayHeight));
                    overlay.setAttribute('viewBox', '0 0 ' + overlayWidth + ' ' + overlayHeight);
                    overlay.style.width = String(overlayWidth) + 'px';
                    overlay.style.height = String(overlayHeight) + 'px';
                    container.appendChild(overlay);
                    updateActiveNoteNameBeat(
                        lastPlayedNoteNameBeatId ? { id: Number(lastPlayedNoteNameBeatId) } : null
                    );
                }

                function setNoteNamesVisible(visible) {
                    if (isPdfExportMode) return false;

                    var nextVisible = !!visible;
                    if (nextVisible === noteNamesVisible) {
                        if (nextVisible) renderNoteNameOverlay();
                        else clearNoteNameOverlay();
                        return true;
                    }

                    var previousVisible = noteNamesVisible;
                    noteNamesVisible = nextVisible;
                    if (!noteNamesVisible) {
                        clearNoteNameOverlay();
                        lastPlayedNoteNameBeatId = '';
                    }

                    if (!api || !api.settings || !api.settings.display) return true;

                    try {
                        var wrapper = document.getElementById('scroll-wrapper');
                        var previousScrollTop = wrapper ? wrapper.scrollTop : 0;
                        var targetSystemPadding = noteNamesVisible
                            ? noteNameSystemPaddingBottom
                            : defaultSystemPaddingBottom;
                        var targetLastSystemPadding = noteNamesVisible
                            ? noteNameLastSystemPaddingBottom
                            : defaultLastSystemPaddingBottom;

                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    systemPaddingBottom: targetSystemPadding,
                                    lastSystemPaddingBottom: targetLastSystemPadding
                                }
                            });
                        } else {
                            api.settings.display.systemPaddingBottom = targetSystemPadding;
                            api.settings.display.lastSystemPaddingBottom = targetLastSystemPadding;
                        }

                        api.updateSettings();
                        if (api.score) {
                            applySelectedTrackSolo(true);
                        }

                        setTimeout(function() {
                            var currentWrapper = document.getElementById('scroll-wrapper');
                            if (currentWrapper) {
                                var maxScroll = Math.max(
                                    0,
                                    currentWrapper.scrollHeight - currentWrapper.clientHeight
                                );
                                currentWrapper.scrollTop = Math.min(previousScrollTop, maxScroll);
                            }
                        }, 320);
                        return true;
                    } catch (error) {
                        noteNamesVisible = previousVisible;
                        console.warn('Could not change note-name display', error);
                        return false;
                    }
                }

                function setStaveDisplayMode(mode) {
                    if (!api || !api.score || isPdfExportMode) return false;

                    var normalizedMode = String(mode || 'scoretab').toLowerCase();
                    if (
                        normalizedMode !== 'scoretab' &&
                        normalizedMode !== 'tab' &&
                        normalizedMode !== 'score'
                    ) {
                        normalizedMode = 'scoretab';
                    }

                    // TAB-only dùng từng ký hiệu trường độ riêng ở phía dưới khuông.
                    // Phần thân nốt sẽ được rút ngắn bằng custom renderer bên dưới,
                    // nhờ đó không còn đường dọc chạy xuyên qua dây và số ngăn đàn.
                    // Ở chế độ có khuông nhạc, dùng Automatic để tránh lặp ký hiệu tiết tấu.
                    var tabRhythmMode = normalizedMode === 'tab'
                        ? 'ShowWithBars'
                        : 'Automatic';
                    var tabRhythmHeight = 25;

                    try {
                        var wrapper = document.getElementById('scroll-wrapper');
                        var previousScrollTop = wrapper ? wrapper.scrollTop : 0;

                        if (
                            api.settings &&
                            typeof api.settings.fillFromJson === 'function'
                        ) {
                            api.settings.fillFromJson({
                                display: {
                                    staveProfile: normalizedMode,
                                    systemPaddingBottom: noteNamesVisible
                                        ? noteNameSystemPaddingBottom
                                        : defaultSystemPaddingBottom,
                                    lastSystemPaddingBottom: noteNamesVisible
                                        ? noteNameLastSystemPaddingBottom
                                        : defaultLastSystemPaddingBottom
                                },
                                notation: {
                                    rhythmMode: tabRhythmMode,
                                    rhythmHeight: tabRhythmHeight,
                                    elements: { scoreTitle: true }
                                }
                            });
                        } else if (
                            api.settings &&
                            api.settings.display &&
                            typeof alphaTab !== 'undefined' &&
                            alphaTab.StaveProfile
                        ) {
                            api.settings.display.staveProfile =
                                normalizedMode === 'tab'
                                    ? alphaTab.StaveProfile.Tab
                                    : normalizedMode === 'score'
                                        ? alphaTab.StaveProfile.Score
                                        : alphaTab.StaveProfile.ScoreTab;
                            api.settings.display.systemPaddingBottom = noteNamesVisible
                                ? noteNameSystemPaddingBottom
                                : defaultSystemPaddingBottom;
                            api.settings.display.lastSystemPaddingBottom = noteNamesVisible
                                ? noteNameLastSystemPaddingBottom
                                : defaultLastSystemPaddingBottom;

                            if (api.settings.notation) {
                                ensureScoreTitleSettingVisible();
                                if (alphaTab.TabRhythmMode) {
                                    api.settings.notation.rhythmMode =
                                        normalizedMode === 'tab'
                                            ? alphaTab.TabRhythmMode.ShowWithBars
                                            : alphaTab.TabRhythmMode.Automatic;
                                } else {
                                    api.settings.notation.rhythmMode = tabRhythmMode;
                                }
                                api.settings.notation.rhythmHeight = tabRhythmHeight;
                            }
                        } else {
                            return false;
                        }

                        api.updateSettings();
                        applySelectedTrackSolo(true);

                        // Sau khi render lại, giữ vị trí cuộn gần nhất để người dùng
                        // không bị đưa đột ngột về đầu bản nhạc.
                        setTimeout(function() {
                            var currentWrapper = document.getElementById('scroll-wrapper');
                            if (currentWrapper) {
                                var maxScroll = Math.max(
                                    0,
                                    currentWrapper.scrollHeight - currentWrapper.clientHeight
                                );
                                currentWrapper.scrollTop = Math.min(previousScrollTop, maxScroll);
                            }
                        }, 300);

                        return true;
                    } catch (error) {
                        console.warn('Could not change stave display mode', error);
                        return false;
                    }
                }

                function resetSoundFontCompatibilityState() {
                    soundFontCompatibilityCheckToken++;
                    soundFontCompatibilityCache = {};
                    if (soundFontCompatibilityCheckInProgress) {
                        soundFontCompatibilityCheckInProgress = false;
                        AndroidInterface.onSoundFontCompatibilityCheckChanged(false);
                    }
                }

                function applySelectedTrackSolo(shouldRender) {
                    if (!api || !currentTracks || currentTracks.length === 0) return;

                    var safeIndex = Math.max(
                        0,
                        Math.min(selectedTrackForPlayback, currentTracks.length - 1)
                    );
                    selectedTrackForPlayback = safeIndex;

                    var selectedTrack = currentTracks[safeIndex];
                    api.changeTrackSolo(currentTracks, false);
                    if (shouldRender) {
                        api.renderTracks([selectedTrack]);
                    }
                    api.changeTrackSolo([selectedTrack], true);
                }

                function selectedTrackNoteRange() {
                    if (!currentTracks || currentTracks.length === 0) return null;
                    var track = currentTracks[selectedTrackForPlayback];
                    if (!track || !track.staves) return null;

                    // Track bi mute hoac volume bang 0 se im lang voi moi SoundFont. Khong coi
                    // day la loi tuong thich de tranh chuyen cau hinh sai nguyen nhan.
                    var playbackInfo = track.playbackInfo;
                    if (
                        playbackInfo &&
                        (playbackInfo.isMute === true || Number(playbackInfo.volume) <= 0)
                    ) {
                        return null;
                    }

                    var firstTick = Number.POSITIVE_INFINITY;
                    var lastTick = 0;
                    var firstDuration = 0;
                    var noteCount = 0;

                    for (var staffIndex = 0; staffIndex < track.staves.length; staffIndex++) {
                        var staff = track.staves[staffIndex];
                        if (!staff || !staff.bars) continue;

                        for (var barIndex = 0; barIndex < staff.bars.length; barIndex++) {
                            var bar = staff.bars[barIndex];
                            if (!bar || !bar.voices) continue;

                            for (var voiceIndex = 0; voiceIndex < bar.voices.length; voiceIndex++) {
                                var voice = bar.voices[voiceIndex];
                                if (!voice || !voice.beats) continue;

                                for (var beatIndex = 0; beatIndex < voice.beats.length; beatIndex++) {
                                    var beat = voice.beats[beatIndex];
                                    if (!beat || !beat.notes || beat.notes.length === 0) continue;

                                    var tick = Number(beat.absolutePlaybackStart);
                                    if (!Number.isFinite(tick)) continue;
                                    var duration = Number(beat.playbackDuration);
                                    if (!Number.isFinite(duration) || duration <= 0) duration = 960;

                                    if (tick < firstTick) {
                                        firstTick = tick;
                                        firstDuration = duration;
                                    }
                                    lastTick = Math.max(lastTick, tick + duration);
                                    noteCount += beat.notes.length;
                                }
                            }
                        }
                    }

                    if (!Number.isFinite(firstTick) || noteCount <= 0) return null;

                    // Bat dau ngay truoc not dau tien de khong nham doan nghi dau bai la loi.
                    var startTick = Math.max(0, firstTick - Math.max(60, firstDuration * 0.10));
                    var minimumSpan = Math.max(3840, firstDuration * 8);
                    var maximumEnd = startTick + 15360;
                    var endTick = Math.min(Math.max(lastTick + 960, startTick + minimumSpan), maximumEnd);
                    if (!Number.isFinite(endTick) || endTick <= startTick) {
                        endTick = startTick + 7680;
                    }

                    return {
                        startTick: startTick,
                        endTick: endTick,
                        noteCount: noteCount
                    };
                }

                function containsAudibleSamples(samples) {
                    if (!samples || samples.length === 0) return false;

                    var peak = 0;
                    var energy = 0;
                    var count = 0;
                    for (var i = 0; i < samples.length; i++) {
                        var value = Number(samples[i]);
                        if (!Number.isFinite(value)) continue;
                        var absolute = Math.abs(value);
                        if (absolute > peak) peak = absolute;
                        energy += value * value;
                        count++;
                    }

                    if (count === 0) return false;
                    var rms = Math.sqrt(energy / count);
                    // Preset bi thieu tra ve mau bang 0. Nguong rat thap de khong loai nham not pianissimo.
                    return peak >= 0.00001 || rms >= 0.000001;
                }

                async function verifySelectedSoundFontProducesAudio() {
                    if (!api || !api.score || activeSoundFontMode === 'standard') return true;
                    if (typeof api.exportAudio !== 'function') return true;

                    var noteRange = selectedTrackNoteRange();
                    // Track khong co not la van de noi dung/track, khong phai loi SoundFont.
                    if (!noteRange) return true;

                    var cacheKey = [
                        soundFontCompatibilityScoreGeneration,
                        selectedTrackForPlayback,
                        activeSoundFontMode
                    ].join('|');
                    if (Object.prototype.hasOwnProperty.call(soundFontCompatibilityCache, cacheKey)) {
                        return soundFontCompatibilityCache[cacheKey] === true;
                    }

                    var checkToken = ++soundFontCompatibilityCheckToken;
                    soundFontCompatibilityCheckInProgress = true;
                    AndroidInterface.onSoundFontCompatibilityCheckChanged(true);
                    var exporter = null;

                    try {
                        api.stop();
                        var options = new alphaTab.synth.AudioExportOptions();
                        options.sampleRate = 22050;
                        options.masterVolume = 1.0;
                        options.metronomeVolume = 0.0;
                        options.useSyncPoints = false;
                        options.playbackRange = {
                            startTick: noteRange.startTick,
                            endTick: noteRange.endTick
                        };

                        if (options.trackVolume && typeof options.trackVolume.set === 'function') {
                            for (var trackIndex = 0; trackIndex < currentTracks.length; trackIndex++) {
                                options.trackVolume.set(
                                    trackIndex,
                                    trackIndex === selectedTrackForPlayback ? 1.0 : 0.0
                                );
                            }
                        }

                        exporter = await api.exportAudio(options);
                        var audible = false;
                        for (var chunkIndex = 0; chunkIndex < 20; chunkIndex++) {
                            if (checkToken !== soundFontCompatibilityCheckToken) return true;
                            var chunk = await exporter.render(400);
                            if (!chunk) break;
                            if (containsAudibleSamples(chunk.samples)) {
                                audible = true;
                                break;
                            }
                            if (
                                Number.isFinite(Number(chunk.currentTick)) &&
                                Number(chunk.currentTick) >= noteRange.endTick
                            ) {
                                break;
                            }
                        }

                        if (checkToken === soundFontCompatibilityCheckToken) {
                            soundFontCompatibilityCache[cacheKey] = audible;
                        }
                        return audible;
                    } catch (error) {
                        // Neu WebView/alphaTab khong ho tro exportAudio, khong duoc fallback nham.
                        console.warn('SoundFont compatibility check skipped', error);
                        soundFontCompatibilityCache[cacheKey] = true;
                        return true;
                    } finally {
                        if (exporter && typeof exporter.destroy === 'function') {
                            try { exporter.destroy(); } catch (_) {}
                        }
                        if (checkToken === soundFontCompatibilityCheckToken) {
                            soundFontCompatibilityCheckInProgress = false;
                            AndroidInterface.onSoundFontCompatibilityCheckChanged(false);
                        }
                    }
                }

                function autoFallbackToStandard() {
                    if (
                        !api ||
                        activeSoundFontMode === 'standard' ||
                        autoFallbackInProgress
                    ) {
                        return false;
                    }

                    var incompatibleMode = activeSoundFontMode || 'standard';
                    autoFallbackInProgress = true;
                    AndroidInterface.onSoundFontAutoFallback(incompatibleMode);
                    var started = switchSoundFontMode(
                        'standard',
                        standardSoundFontUrl,
                        true,
                        true
                    );
                    if (!started) {
                        autoFallbackInProgress = false;
                    }
                    return started;
                }

                function signalA4PdfLayoutReady() {
                    if (!isPdfExportMode || !pdfExportWaitingForRender) return;

                    pdfExportWaitingForRender = false;
                    clearTimeout(pdfExportReadyTimer);
                    clearTimeout(pdfExportFallbackTimer);
                    setTimeout(function() {
                        AndroidInterface.onPdfLayoutReady();
                    }, 80);
                }

                function prepareA4PdfExport() {
                    try {
                        if (!api || !api.score) {
                            AndroidInterface.onPdfExportFailed('Bản nhạc chưa sẵn sàng để xuất PDF.');
                            return;
                        }
                        if (isPdfExportMode) return;

                        var wrapper = document.getElementById('scroll-wrapper');
                        pdfExportOriginalState = {
                            scale: Number(api.settings.display.scale),
                            stretchForce: Number(api.settings.display.stretchForce),
                            enableLazyLoading: typeof api.settings.core.enableLazyLoading === 'boolean'
                                ? api.settings.core.enableLazyLoading
                                : true,
                            scrollTop: wrapper ? wrapper.scrollTop : 0
                        };

                        api.stop();
                        notifyTabBannerVisibility(false);
                        isPdfExportMode = true;
                        pdfExportWaitingForRender = true;
                        document.documentElement.classList.add('pdf-export-mode');
                        document.body.classList.add('pdf-export-mode');
                        toggleTheme(currentThemeIsDark);

                        // Cùng các thông số alphaTab dùng cho bản in: render toàn bộ,
                        // scale 0.8, stretchForce 0.8 và chiều rộng vùng nội dung A4.
                        api.settings.core.enableLazyLoading = false;
                        api.settings.display.scale = 0.8;
                        api.settings.display.stretchForce = 0.8;

                        // Tính lại tiêu đề theo chiều rộng 190 mm rồi render track đang chọn.
                        lastResponsiveTitleFont = '';
                        applyResponsiveSongHeader(api.score, false);
                        applySelectedTrackSolo(true);

                        clearTimeout(pdfExportFallbackTimer);
                        pdfExportFallbackTimer = setTimeout(function() {
                            signalA4PdfLayoutReady();
                        }, 7000);
                    } catch (error) {
                        restoreAfterA4PdfExport();
                        AndroidInterface.onPdfExportFailed(
                            'Không thể chuẩn bị PDF: ' +
                            (error && error.message ? error.message : error)
                        );
                    }
                }

                function restoreAfterA4PdfExport() {
                    clearTimeout(pdfExportReadyTimer);
                    clearTimeout(pdfExportFallbackTimer);
                    pdfExportWaitingForRender = false;

                    if (!isPdfExportMode) return;
                    isPdfExportMode = false;
                    document.documentElement.classList.remove('pdf-export-mode');
                    document.body.classList.remove('pdf-export-mode');
                    syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );

                    try {
                        if (api && api.score && pdfExportOriginalState) {
                            api.settings.core.enableLazyLoading =
                                pdfExportOriginalState.enableLazyLoading;
                            api.settings.display.scale = Number.isFinite(pdfExportOriginalState.scale)
                                ? pdfExportOriginalState.scale
                                : 1.0;
                            api.settings.display.stretchForce =
                                Number.isFinite(pdfExportOriginalState.stretchForce)
                                    ? pdfExportOriginalState.stretchForce
                                    : 1.0;

                            lastResponsiveTitleFont = '';
                            applyResponsiveSongHeader(api.score, false);
                            applySelectedTrackSolo(true);

                            var oldScrollTop = pdfExportOriginalState.scrollTop || 0;
                            setTimeout(function() {
                                var wrapper = document.getElementById('scroll-wrapper');
                                if (wrapper) wrapper.scrollTop = oldScrollTop;
                            }, 350);
                        }
                    } catch (_) {
                        // Không chặn giao diện nếu thiết bị hủy hộp thoại in giữa chừng.
                    } finally {
                        toggleTheme(currentThemeIsDark);
                        pdfExportOriginalState = null;
                    }
                }

                function switchSoundFontMode(
                    mode,
                    soundFontUrl,
                    temporaryFallback,
                    autoPlayAfterLoad
                ) {
                    if (!api || !soundFontUrl) return false;

                    try {
                        api.stop();
                        resetSoundFontCompatibilityState();
                        pendingSoundFontMode = String(mode || 'standard');
                        pendingSoundFontTemporaryFallback = !!temporaryFallback;
                        autoPlayAfterSoundFontReady = !!autoPlayAfterLoad;
                        isSoundFontLoading = true;

                        // append=false da thay the toan bo SoundFont cu. Khong reset truoc de
                        // tranh tao khoang thoi gian player khong con preset nao.
                        var started = api.loadSoundFont(soundFontUrl, false);
                        if (!started) {
                            var failedMode = pendingSoundFontMode || 'standard';
                            pendingSoundFontMode = null;
                            pendingSoundFontTemporaryFallback = false;
                            autoPlayAfterSoundFontReady = false;
                            isSoundFontLoading = false;
                            AndroidInterface.onSoundFontLoadFailed(
                                failedMode,
                                soundFontStartFailedText
                            );
                            return false;
                        }

                        return true;
                    } catch (error) {
                        var failedMode = pendingSoundFontMode || String(mode || 'standard');
                        pendingSoundFontMode = null;
                        pendingSoundFontTemporaryFallback = false;
                        autoPlayAfterSoundFontReady = false;
                        isSoundFontLoading = false;
                        AndroidInterface.onSoundFontLoadFailed(
                            failedMode,
                            soundFontLoadFailedPrefix +
                                (error && error.message ? error.message : error)
                        );
                        return false;
                    }
                }

                // alphaTab mặc định nối thân trường độ từ số ngăn đàn xuống beam,
                // vì vậy đường dọc đi xuyên qua khuông TAB. TunerTools giữ cách tính
                // trường độ của alphaTab nhưng chỉ vẽ đoạn thân ngắn ngay phía trên beam.
                // Custom renderer chạy ở main thread nên initAlphaTab đặt useWorkers=false.
                var detachedTabRhythmFactoryPatched = false;

                function installDetachedTabRhythmPatch() {
                    if (detachedTabRhythmFactoryPatched) return true;

                    try {
                        if (
                            typeof alphaTab === 'undefined' ||
                            !alphaTab.Environment ||
                            !alphaTab.Environment.defaultRenderers
                        ) {
                            return false;
                        }

                        var factories = alphaTab.Environment.defaultRenderers;
                        var tabFactory = null;
                        for (var factoryIndex = 0; factoryIndex < factories.length; factoryIndex++) {
                            var candidate = factories[factoryIndex];
                            if (
                                candidate &&
                                candidate.staffId === 'tab' &&
                                typeof candidate.create === 'function'
                            ) {
                                tabFactory = candidate;
                                break;
                            }
                        }

                        if (!tabFactory) return false;
                        if (tabFactory.__tunerToolsDetachedRhythmPatched === true) {
                            detachedTabRhythmFactoryPatched = true;
                            return true;
                        }

                        var originalCreateTabRenderer = tabFactory.create;
                        tabFactory.create = function(renderer, bar) {
                            var tabRenderer = originalCreateTabRenderer.call(this, renderer, bar);

                            if (
                                tabRenderer &&
                                tabRenderer.__tunerToolsThinRhythmBeam !== true
                            ) {
                                try {
                                    // Beam mặc định của alphaTab khá dày khi thu nhỏ trên màn hình
                                    // điện thoại. Chỉ giảm độ dày các thanh ngang của TAB; số lượng
                                    // một, hai hoặc ba thanh vẫn giữ nguyên để đọc đúng trường độ.
                                    Object.defineProperty(tabRenderer, 'beamThickness', {
                                        configurable: true,
                                        get: function() {
                                            var sourceThickness =
                                                this.smuflMetrics &&
                                                Number.isFinite(Number(this.smuflMetrics.beamThickness))
                                                    ? Number(this.smuflMetrics.beamThickness)
                                                    : 3;

                                            return Math.max(
                                                0.85,
                                                Math.min(1.15, sourceThickness * 0.34)
                                            );
                                        }
                                    });
                                    tabRenderer.__tunerToolsThinRhythmBeam = true;
                                } catch (_) {
                                    // Nếu phiên bản alphaTab không cho ghi đè getter thì vẫn dùng
                                    // renderer mặc định, không làm gián đoạn việc tải bản nhạc.
                                }
                            }

                            if (
                                tabRenderer &&
                                tabRenderer.__tunerToolsDetachedRhythmRenderer !== true
                            ) {
                                tabRenderer.paintBeamingStem = function(
                                    beat,
                                    cy,
                                    x,
                                    topY,
                                    bottomY,
                                    canvas
                                ) {
                                    if (!canvas || typeof canvas.fillRect !== 'function') return;

                                    var stemTop = Number(topY);
                                    var stemBottom = Number(bottomY);
                                    if (!Number.isFinite(stemTop) || !Number.isFinite(stemBottom)) return;
                                    if (stemBottom < stemTop) {
                                        var swapY = stemTop;
                                        stemTop = stemBottom;
                                        stemBottom = swapY;
                                    }

                                    var availableHeight = stemBottom - stemTop;
                                    if (availableHeight <= 0) return;

                                    var configuredRhythmHeight = 25;
                                    if (
                                        this.settings &&
                                        this.settings.notation &&
                                        Number.isFinite(Number(this.settings.notation.rhythmHeight))
                                    ) {
                                        configuredRhythmHeight = Number(
                                            this.settings.notation.rhythmHeight
                                        );
                                    }

                                    // Chỉ giữ 7-12 px cuối của thân nốt. Đoạn này nằm hoàn toàn
                                    // trong vùng trường độ dưới khuông, không chạm dây hoặc số TAB.
                                    var preferredStemLength = Math.max(
                                        7,
                                        Math.min(12, configuredRhythmHeight * 0.42)
                                    );
                                    var detachedStemLength = Math.min(
                                        availableHeight,
                                        preferredStemLength
                                    );
                                    var detachedStemTop = stemBottom - detachedStemLength;
                                    var stemThickness = this.smuflMetrics &&
                                        Number.isFinite(Number(this.smuflMetrics.stemThickness))
                                        ? Number(this.smuflMetrics.stemThickness)
                                        : 1;

                                    canvas.fillRect(
                                        Number(x),
                                        detachedStemTop,
                                        stemThickness,
                                        detachedStemLength
                                    );
                                };
                                tabRenderer.__tunerToolsDetachedRhythmRenderer = true;
                            }
                            return tabRenderer;
                        };

                        tabFactory.__tunerToolsDetachedRhythmPatched = true;
                        detachedTabRhythmFactoryPatched = true;
                        return true;
                    } catch (error) {
                        console.warn('Could not install detached TAB rhythm renderer', error);
                        return false;
                    }
                }

                function initAlphaTab(attempt) {
                    attempt = Number(attempt) || 0;
                    if (isApiReady || api) return;

                    var wrapper = document.getElementById('scroll-wrapper');
                    var container = document.getElementById('alphatab-container');
                    var viewport = syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );

                    if (
                        !wrapper ||
                        !container ||
                        typeof alphaTab === 'undefined' ||
                        viewport.width <= 1 ||
                        viewport.height <= 1
                    ) {
                        clearTimeout(alphaTabInitRetryTimer);
                        alphaTabInitRetryTimer = setTimeout(function() {
                            initAlphaTab(attempt + 1);
                        }, attempt < 20 ? 100 : 250);
                        return;
                    }

                    clearTimeout(alphaTabInitRetryTimer);
                    installDetachedTabRhythmPatch();
                    
                    wrapper.addEventListener('touchstart', function() {
                        isUserScrolling = true;
                        clearTimeout(scrollTimeout);
                    }, { passive: true });

                    wrapper.addEventListener('touchmove', function() {
                        // WebView co the phat scroll sau touchmove (inertia/fling).
                        // Ghi nhan thao tac thuc, khong phu thuoc event scroll dau tien.
                        if (wrapper.scrollTop > 0) tabBannerManuallyScrolled = true;
                    }, { passive: true });

                    wrapper.addEventListener('touchend', function() {
                        scrollTimeout = setTimeout(function() { isUserScrolling = false; }, 3000);
                    }, { passive: true });
                    
                    wrapper.addEventListener('wheel', function() {
                        isUserScrolling = true;
                        clearTimeout(scrollTimeout);
                        scrollTimeout = setTimeout(function() { isUserScrolling = false; }, 3000);
                    }, { passive: true });

                    wrapper.addEventListener('scroll', function() {
                        var newScrollTop = Math.max(0, wrapper.scrollTop);
                        // AlphaTab tu cuon theo not dang phat khong duoc tinh
                        // la nguoi dung chu dong cuon den cuoi bai.
                        if (isUserScrolling &&
                            newScrollTop > tabBannerPreviousScrollTop + 0.5) {
                            tabBannerManuallyScrolled = true;
                        }
                        tabBannerPreviousScrollTop = newScrollTop;
                        updateTabBannerAtScroll();
                    }, { passive: true });

                    api = new alphaTab.AlphaTabApi(document.getElementById('alphatab-container'), {
                        // Tắt render worker để custom TabBarRenderer ở trên được áp dụng.
                        core: { engine: 'svg', useWorkers: false, enableLazyLoading: false },
                        display: {
                            layoutMode: 'page',
                            staveProfile: 'scoretab',
                            stretchForce: 1.0,
                            systemPaddingBottom: noteNamesVisible
                                ? noteNameSystemPaddingBottom
                                : defaultSystemPaddingBottom,
                            lastSystemPaddingBottom: noteNamesVisible
                                ? noteNameLastSystemPaddingBottom
                                : defaultLastSystemPaddingBottom,
                            padding: [12, 10, 12, 24],
                            resources: {
                                titleFont: 'bold 18px Arial, sans-serif',
                                subTitleFont: '14px Arial, sans-serif',
                                wordsFont: '12px Arial, sans-serif',
                                copyrightFont: 'bold 10px Arial, sans-serif'
                            }
                        },
                        // Automatic không vẽ thêm tiết tấu khi khuông nhạc đang hiển thị.
                        // Khi người dùng chọn TAB, setStaveDisplayMode() chuyển sang
                        // ShowWithBars và custom renderer chỉ giữ ký hiệu ở dưới khuông.
                        notation: {
                            rhythmMode: 'Automatic',
                            rhythmHeight: 25,
                            elements: { scoreTitle: true }
                        },
                        player: {
                            enablePlayer: true,
                            enableCursor: true,
                            soundFont: $quotedInitialSoundFontUrl,
                            scrollElement: wrapper,
                            // Không để alphaTab tự nhảy tới khuông đầu khi vừa tạo cursor.
                            // Cuộn theo khuông lúc phát được xử lý bởi playerPositionChanged.
                            scrollMode: 'Off'
                        }
                    });

                    api.soundFontLoad.on(function(e) {
                        AndroidInterface.onSoundFontLoadProgress(
                            Number(e && e.loaded ? e.loaded : 0),
                            Number(e && e.total ? e.total : 0)
                        );
                    });
                    api.soundFontLoaded.on(function() {
                        isSoundFontLoading = false;
                        activeSoundFontMode = pendingSoundFontMode ||
                            activeSoundFontMode ||
                            'standard';
                        var wasTemporaryFallback = pendingSoundFontTemporaryFallback;
                        pendingSoundFontMode = null;
                        pendingSoundFontTemporaryFallback = false;
                        AndroidInterface.onSoundFontLoaded(
                            activeSoundFontMode,
                            wasTemporaryFallback
                        );

                        // Doi SoundFont trong luc score dang mo phai tao lai MIDI truoc khi Play.
                        if (api && api.score && typeof api.loadMidiForScore === 'function') {
                            api.loadMidiForScore();
                        }
                    });
                    api.playerReady.on(function() {
                        AndroidInterface.onPlayerReady();
                        if (autoPlayAfterSoundFontReady) {
                            autoPlayAfterSoundFontReady = false;
                            autoFallbackInProgress = false;
                            setTimeout(function() {
                                if (api && api.isReadyForPlayback) {
                                    api.play();
                                }
                            }, 50);
                        }
                    });
                    api.playerStateChanged.on(function(e) {
                        lastKnownPlayerState = Number(e && e.state ? e.state : 0);
                        if (e && e.stopped === true) {
                            updateActiveNoteNameBeat(null);
                        }
                        AndroidInterface.onPlayerStateChanged(lastKnownPlayerState);
                    });
                    if (api.playedBeatChanged && typeof api.playedBeatChanged.on === 'function') {
                        api.playedBeatChanged.on(function(beat) {
                            updateActiveNoteNameBeat(beat);
                        });
                    }
                    api.midiLoaded.on(function() {
                        // loadMidiForScore() tạo lại MIDI khi đổi BPM; áp lại track đang solo.
                        applySelectedTrackSolo(false);
                    });

                    api.playerPositionChanged.on(function(e) {
                        // Sự kiện này cũng phát sinh khi MIDI/cursor vừa khởi tạo.
                        // Chỉ tự cuộn khi người dùng đã thật sự bấm Play.
                        if (isUserScrolling || lastKnownPlayerState !== 1) return;

                        var cursor = document.querySelector('.at-cursor-beat');
                        if (cursor) {
                            var rect = cursor.getBoundingClientRect();
                            var wrapperHeight = wrapper.clientHeight;
                            
                            if (rect.top > wrapperHeight * 0.6 || rect.top < wrapperHeight * 0.1) {
                                var absoluteTop = wrapper.scrollTop + rect.top;
                                wrapper.scrollTo({ top: absoluteTop - (wrapperHeight * 0.3), behavior: 'smooth' });
                            }
                        }
                    });

                    api.error.on(function(e) {
                        var message = 'Lỗi AlphaTab: ' +
                            (e && e.message ? e.message : e);

                        if (isScoreLoading) {
                            isScoreLoading = false;
                            AndroidInterface.onScoreLoaded("[]", message);
                        } else if (isSoundFontLoading || pendingSoundFontMode) {
                            var failedMode = pendingSoundFontMode ||
                                activeSoundFontMode ||
                                'standard';
                            pendingSoundFontMode = null;
                            pendingSoundFontTemporaryFallback = false;
                            autoPlayAfterSoundFontReady = false;
                            autoFallbackInProgress = false;
                            isSoundFontLoading = false;
                            AndroidInterface.onSoundFontLoadFailed(failedMode, message);
                        } else {
                            AndroidInterface.onScoreLoaded("[]", message);
                        }
                    });

                    // Kiểm tra lại đúng kích thước SVG sau khi alphaTab đã gắn kết quả
                    // vào DOM. Nếu font thực tế vẫn rộng hơn màn hình thì tự co thêm.
                    var completedRenderEvent = api.postRenderFinished || api.renderFinished;
                    if (completedRenderEvent && typeof completedRenderEvent.on === 'function') {
                        completedRenderEvent.on(function() {
                            verifyRenderedTitleFits();
                            renderNoteNameOverlay();

                            // Mỗi lần mở bài mới phải quay về đúng đầu bản nhạc để người dùng
                            // nhìn thấy tiêu đề. Không áp dụng khi chỉ đổi track hoặc đổi chế độ.
                            if (scrollToTopAfterNextRender) {
                                scrollToTopAfterNextRender = false;
                                var renderedWrapper = document.getElementById('scroll-wrapper');
                                if (renderedWrapper) renderedWrapper.scrollTop = 0;
                            }

                            // Sau khi alphaTab ve lai, SVG cuoi co the da bi thay the.
                            tabBannerLastScoreSvg = null;
                            setTimeout(updateTabBannerAtScroll, 50);

                            if (pdfExportWaitingForRender) {
                                // Đợi tiêu đề và các SVG ổn định; nếu alphaTab render lại,
                                // timer được đặt lại để PDF không bị thiếu trang hoặc cắt chữ.
                                clearTimeout(pdfExportReadyTimer);
                                pdfExportReadyTimer = setTimeout(function() {
                                    signalA4PdfLayoutReady();
                                }, 700);
                            }
                        });
                    }

                    api.scoreLoaded.on(function(score) {
                        isScoreLoading = false;
                        soundFontCompatibilityScoreGeneration++;
                        resetSoundFontCompatibilityState();
                        autoFallbackInProgress = false;
                        autoPlayAfterSoundFontReady = false;
                        currentTracks = score.tracks;
                        lastPlayedNoteNameBeatId = '';
                        clearNoteNameOverlay();

                        // Khôi phục tiêu đề ngay trong phần đầu bản nhạc. Nếu metadata của
                        // file GP trống thì dùng tên bài từ danh sách Android làm dự phòng.
                        applyResponsiveSongHeader(score, false);
                        captureOriginalTempos(score);

                        var tracksData = [];
                        for(var i=0; i<score.tracks.length; i++) {
                            tracksData.push({ index: i, name: score.tracks[i].name || ('Track ' + (i+1)) });
                        }
                        
                        if(currentTracks.length > 0) {
                            selectedTrackForPlayback = 0;
                            applySelectedTrackSolo(true);
                        }
                        
                        var title = String(score.title || currentScoreTitle || 'Bài nhạc').trim();
                        AndroidInterface.onScoreLoaded(JSON.stringify(tracksData), title);

                        // Một số bản alphaTab không tạo header nếu file gốc không có metadata.
                        // Kiểm tra lại sau render; nếu vẫn thiếu thì hiển thị title dự phòng
                        // ngay trong vùng bản nhạc, phía trên khuông đầu tiên.
                        setTimeout(function() {
                            verifyRenderedTitleFits();
                        }, 180);
                    });
                    
                    // Khi xoay màn hình hoặc WebView thay đổi chiều rộng, tính lại font
                    // để tiêu đề vẫn vừa cả ở chế độ dọc lẫn ngang.
                    window.addEventListener('resize', function() {
                        clearTimeout(titleResizeTimer);
                        titleResizeTimer = setTimeout(function() {
                            if (!api || !api.score || isPdfExportMode) return;
                            applyResponsiveSongHeader(api.score, true);
                        }, 180);
                    });

                    isApiReady = true;
                    lastSyncedCssViewportWidth = wrapper.clientWidth || lastSyncedCssViewportWidth;
                    AndroidInterface.onTabReady();
                }
                
                async function loadScoreFromUrl(fileUrl, fallbackTitle) {
                    if (!isApiReady || !api) {
                        setTimeout(function() {
                            loadScoreFromUrl(fileUrl, fallbackTitle);
                        }, 500);
                        return;
                    }

                    try {
                        pendingScoreTitle = String(fallbackTitle || '')
                            .replace(/\s+/g, ' ')
                            .trim();
                        hideFallbackScoreTitle();
                        resetTabBannerForScore();
                        scrollToTopAfterNextRender = true;

                        // Xóa vị trí cuộn của bài trước ngay khi bắt đầu tải. Sau lần render
                        // đầu tiên sẽ đặt lại thêm một lần để chắc chắn tiêu đề không bị khuất.
                        var wrapper = document.getElementById('scroll-wrapper');
                        if (wrapper) wrapper.scrollTop = 0;

                        isScoreLoading = true;
                        resetSoundFontCompatibilityState();
                        autoFallbackInProgress = false;
                        autoPlayAfterSoundFontReady = false;
                        const res = await fetch(fileUrl);
                        if (!res.ok) throw new Error("Mã phản hồi HTTP " + res.status);
                        const buffer = await res.arrayBuffer();
                        api.load(new Uint8Array(buffer));
                    } catch (err) {
                        isScoreLoading = false;
                        AndroidInterface.onScoreLoaded("[]", "Lỗi Fetch JS: " + err.message);
                    }
                }
                
                function selectTrackSolo(index) {
                    if (!currentTracks || index < 0 || index >= currentTracks.length) return;
                    selectedTrackForPlayback = index;
                    lastPlayedNoteNameBeatId = '';
                    clearNoteNameOverlay();
                    resetSoundFontCompatibilityState();
                    applySelectedTrackSolo(true);
                }

                async function playPause() {
                    if (!api) return false;

                    if (lastKnownPlayerState === 1) {
                        api.pause();
                        return true;
                    }
                    if (
                        isSoundFontLoading ||
                        soundFontCompatibilityCheckInProgress ||
                        !api.isReadyForPlayback
                    ) {
                        return false;
                    }

                    if (activeSoundFontMode && activeSoundFontMode !== 'standard') {
                        var compatible = await verifySelectedSoundFontProducesAudio();
                        if (!compatible) {
                            autoFallbackToStandard();
                            return true;
                        }
                    }

                    return api.play();
                }
                function stopPlayer() {
                    resetSoundFontCompatibilityState();
                    if (api) api.stop();
                }

                window.addEventListener('load', function() {
                    syncWebViewViewportBox(
                        lastAndroidViewportWidth,
                        lastAndroidViewportHeight
                    );
                    initAlphaTab(0);
                });
                if (document.readyState === 'complete') {
                    setTimeout(function() { initAlphaTab(0); }, 0);
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
// ----------------- API BẢN NHẠC -----------------
private enum class SheetMusicServerSource {
    PRIMARY,
    FALLBACK
}

private data class SheetMusicSearchOutcome(
    val songs: List<TuneToolsSong>,
    val source: SheetMusicServerSource
)

private object TuneToolsSheetMusicApi {
    private const val FALLBACK_API_BASE_URL = "https://tuner.alwaysdata.net"
    private const val FALLBACK_SEARCH_ENDPOINT =
        "https://tuner.alwaysdata.net/api_gp_drive.php"

    // Máy chủ chính cần chuyển sang dự phòng tương đối nhanh khi thật sự mất kết nối.
    private const val PRIMARY_CONNECT_TIMEOUT_MS = 8_000
    private const val PRIMARY_READ_TIMEOUT_MS = 20_000
    private const val FALLBACK_CONNECT_TIMEOUT_MS = 10_000
    private const val FALLBACK_READ_TIMEOUT_MS = 30_000

    // Khi đang dùng máy chủ dự phòng, kiểm tra lại máy chủ chính định kỳ.
    const val PRIMARY_RECOVERY_CHECK_INTERVAL_MS = 20_000L

    /**
     * Lỗi HTTP có mã phản hồi để phân biệt lỗi máy chủ 5xx với lỗi quyền 4xx.
     * Chỉ lỗi máy chủ/tạm thời mới được phép chuyển sang máy chủ dự phòng.
     */
    private class SearchHttpException(
        val statusCode: Int,
        message: String
    ) : IllegalStateException(message)

    /** Máy chủ trả HTTP 200 nhưng nội dung không phải JSON tìm kiếm hợp lệ. */
    private class InvalidSearchResponseException(
        message: String,
        cause: Throwable? = null
    ) : IllegalStateException(message, cause)

    fun search(
        apiBaseUrl: String,
        searchPath: String,
        keyword: String,
        apiToken: String?,
        section: MusicLibrarySection,
        rhythm: String? = null,
        folder: String? = null
    ): SheetMusicSearchOutcome {
        val primaryUrl = buildPrimarySearchUrl(
            apiBaseUrl = apiBaseUrl,
            searchPath = searchPath,
            keyword = keyword,
            section = section,
            rhythm = rhythm,
            folder = folder
        )

        try {
            val primaryJson = httpGetText(
                urlString = primaryUrl,
                apiToken = apiToken,
                connectTimeoutMs = PRIMARY_CONNECT_TIMEOUT_MS,
                readTimeoutMs = PRIMARY_READ_TIMEOUT_MS
            )

            // Kết quả rỗng hợp lệ vẫn được giữ nguyên, tuyệt đối không fallback chỉ vì không có bài.
            return SheetMusicSearchOutcome(
                songs = parseSearchResponse(primaryJson, apiBaseUrl),
                source = SheetMusicServerSource.PRIMARY
            )
        } catch (primaryError: Exception) {
            if (!shouldUseFallback(primaryError)) {
                throw primaryError
            }

            runCatching {
                android.util.Log.w(
                    "TuneToolsSheetMusicApi",
                    "Primary GP server failed. Switching to backup server.",
                    primaryError
                )
            }

            val fallbackUrl = buildFallbackSearchUrl(keyword)

            try {
                val fallbackJson = httpGetText(
                    urlString = fallbackUrl,
                    apiToken = null,
                    connectTimeoutMs = FALLBACK_CONNECT_TIMEOUT_MS,
                    readTimeoutMs = FALLBACK_READ_TIMEOUT_MS
                )

                return SheetMusicSearchOutcome(
                    songs = parseSearchResponse(
                        jsonText = fallbackJson,
                        apiBaseUrl = FALLBACK_API_BASE_URL
                    ),
                    source = SheetMusicServerSource.FALLBACK
                )
            } catch (fallbackError: Exception) {
                val primaryMessage = primaryError.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "không thể kết nối"
                val fallbackMessage = fallbackError.message
                    ?.takeIf { it.isNotBlank() }
                    ?: "không thể kết nối"

                val combinedError = IllegalStateException(
                    "Máy chủ chính lỗi ($primaryMessage) và máy chủ dự phòng cũng lỗi " +
                            "($fallbackMessage).",
                    fallbackError
                )
                combinedError.addSuppressed(primaryError)
                throw combinedError
            }
        }
    }

    /**
     * Chỉ gọi máy chủ chính, không tự chuyển sang dự phòng. Hàm này được dùng
     * để kiểm tra máy chủ chính đã phục hồi hay chưa khi ứng dụng đang ở chế độ dự phòng.
     */
    fun searchPrimaryOnly(
        apiBaseUrl: String,
        searchPath: String,
        keyword: String,
        apiToken: String?,
        section: MusicLibrarySection,
        rhythm: String? = null,
        folder: String? = null
    ): List<TuneToolsSong> {
        val primaryUrl = buildPrimarySearchUrl(
            apiBaseUrl = apiBaseUrl,
            searchPath = searchPath,
            keyword = keyword,
            section = section,
            rhythm = rhythm,
            folder = folder
        )

        val primaryJson = httpGetText(
            urlString = primaryUrl,
            apiToken = apiToken,
            connectTimeoutMs = PRIMARY_CONNECT_TIMEOUT_MS,
            readTimeoutMs = PRIMARY_READ_TIMEOUT_MS
        )

        return parseSearchResponse(primaryJson, apiBaseUrl)
    }

    private fun buildPrimarySearchUrl(
        apiBaseUrl: String,
        searchPath: String,
        keyword: String,
        section: MusicLibrarySection,
        rhythm: String?,
        folder: String?
    ): String {
        val primaryEndpoint = when {
            searchPath.startsWith("http", ignoreCase = true) -> searchPath
            apiBaseUrl.trimEnd('/').endsWith(".php", ignoreCase = true) ->
                apiBaseUrl.trimEnd('/')
            else -> joinUrl(apiBaseUrl, searchPath)
        }

        val parameters = mutableListOf(
            "q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.name())
        )

        when (section) {
            MusicLibrarySection.NHAC_VIET -> {
                parameters += "section=nhac_viet"
                folder
                    ?.trim()
                    ?.trim('/')
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        parameters += "folder=" +
                                URLEncoder.encode(it, StandardCharsets.UTF_8.name())
                    }
            }

            MusicLibrarySection.DIEU_NHAC -> {
                parameters += "section=dieu_nhac"
                rhythm
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        parameters += "rhythm=" +
                                URLEncoder.encode(it, StandardCharsets.UTF_8.name())
                    }
            }

            MusicLibrarySection.QUOC_TE -> {
                parameters += "section=quoc_te"
            }
        }

        val separator = if (primaryEndpoint.contains("?")) "&" else "?"
        return primaryEndpoint + separator + parameters.joinToString("&")
    }

    /**
     * Máy chủ dự phòng dùng DB + Google Drive và chỉ cần action=search, q, limit.
     * Không truyền token của máy chủ chính sang Alwaysdata.
     */
    private fun buildFallbackSearchUrl(keyword: String): String {
        val parameters = listOf(
            "action=search",
            "q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8.name()),
            "limit=100"
        )
        return FALLBACK_SEARCH_ENDPOINT + "?" + parameters.joinToString("&")
    }

    /**
     * Chỉ fallback khi server thật sự không dùng được:
     * - lỗi mạng/timeout/DNS/SSL;
     * - HTTP 408, 429 hoặc 5xx;
     * - HTTP 200 nhưng JSON tìm kiếm bị hỏng/không đúng cấu trúc.
     *
     * Không fallback với 401/403/404 và không fallback với danh sách rỗng hợp lệ.
     */
    private fun shouldUseFallback(error: Throwable): Boolean {
        var current: Throwable? = error

        while (current != null) {
            when (current) {
                is SearchHttpException -> {
                    return current.statusCode == 408 ||
                            current.statusCode == 429 ||
                            current.statusCode in 500..599
                }

                is InvalidSearchResponseException -> return true
                is java.io.IOException -> return true
            }

            current = current.cause
        }

        return false
    }

    private fun parseSearchResponse(
        jsonText: String,
        apiBaseUrl: String
    ): List<TuneToolsSong> {
        val trimmed = jsonText.trim()
        if (trimmed.isEmpty()) {
            throw InvalidSearchResponseException("Máy chủ trả về dữ liệu trống.")
        }

        val array = try {
            when {
                trimmed.startsWith("[") -> JSONArray(trimmed)

                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("data")
                        ?: root.optJSONArray("songs")
                        ?: root.optJSONArray("results")
                        ?: throw InvalidSearchResponseException(
                            firstNonBlank(
                                root.optString("error"),
                                root.optString("message"),
                                "Phản hồi tìm kiếm không có danh sách kết quả."
                            )
                        )
                }

                else -> throw InvalidSearchResponseException(
                    "Máy chủ trả về dữ liệu không phải JSON."
                )
            }
        } catch (error: InvalidSearchResponseException) {
            throw error
        } catch (error: Exception) {
            throw InvalidSearchResponseException(
                "Không thể đọc dữ liệu tìm kiếm từ máy chủ.",
                error
            )
        }

        val result = ArrayList<TuneToolsSong>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val rawItemType = item.optString("item_type")
                .trim()
                .lowercase(Locale.ROOT)
            val rawFormat = item.optString("format")
                .trim()
                .lowercase(Locale.ROOT)
                .removePrefix(".")
            val isFolder = rawItemType == "folder" || rawFormat == "folder"
            val itemType = if (isFolder) {
                TuneToolsItemType.FOLDER
            } else {
                TuneToolsItemType.FILE
            }

            val title = firstNonBlank(
                item.optString("title"),
                item.optString("name"),
                "Không tên"
            )
            val artist = firstNonBlank(
                item.optString("artist"),
                item.optString("singer")
            )
            val rhythm = firstNonBlank(
                item.optString("rhythm"),
                if (isFolder && item.optString("library_section") == "dieu_nhac") {
                    title
                } else {
                    ""
                }
            )
            val librarySection = item.optString("library_section").trim()
            val relativePath = firstNonBlank(
                item.optString("relative_path"),
                item.optString("file_path")
            ).trim()
            val folderPath = firstNonBlank(
                item.optString("folder_path"),
                if (isFolder) relativePath else ""
            ).trim().trim('/')
            val parentPath = item.optString("parent_path")
                .trim()
                .trim('/')

            val fileUrl = firstNonBlank(
                item.optString("file_url"),
                item.optString("download_url"),
                item.optString("url")
            )

            // Thư mục Điệu nhạc cố ý không có file_url; file thông thường thì bắt buộc có.
            if (!isFolder && fileUrl.isBlank()) continue

            val resolvedUrl = when {
                fileUrl.isBlank() -> ""
                fileUrl.startsWith("http", ignoreCase = true) -> fileUrl
                else -> joinUrl(apiBaseUrl, fileUrl)
            }
            val format = when {
                isFolder -> "folder"
                rawFormat.isNotBlank() -> rawFormat
                else -> "gp5"
            }
            val fallbackId = if (isFolder) {
                "folder:$librarySection:${folderPath.ifBlank { rhythm.ifBlank { title } }}"
            } else {
                resolvedUrl
            }

            result += TuneToolsSong(
                id = firstNonBlank(item.optString("id"), fallbackId),
                title = title,
                artist = artist,
                fileUrl = resolvedUrl,
                format = format,
                itemType = itemType,
                rhythm = rhythm,
                subfolder = item.optString("subfolder").trim(),
                relativePath = relativePath,
                folderPath = folderPath,
                parentPath = parentPath,
                librarySection = librarySection
            )
        }
        return result
    }

    private fun httpGetText(
        urlString: String,
        apiToken: String?,
        connectTimeoutMs: Int,
        readTimeoutMs: Int
    ): String {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.setRequestProperty("User-Agent", "TunerInstrument-Android")
            if (!apiToken.isNullOrBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $apiToken")
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream?.use { input ->
                ByteArrayOutputStream().use { output ->
                    input.copyTo(output)
                    output.toByteArray()
                }
            }?.toString(StandardCharsets.UTF_8).orEmpty()

            if (responseCode !in 200..299) {
                val serverMessage = runCatching {
                    val root = JSONObject(responseText)
                    firstNonBlank(
                        root.optString("error"),
                        root.optString("message")
                    )
                }.getOrNull().orEmpty()

                throw SearchHttpException(
                    statusCode = responseCode,
                    message = serverMessage.ifBlank {
                        "Máy chủ phản hồi HTTP $responseCode"
                    }
                )
            }

            return responseText
        } finally {
            connection.disconnect()
        }
    }

    private fun joinUrl(base: String, path: String): String =
        base.trimEnd('/') + "/" + path.trimStart('/')

    private fun firstNonBlank(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() } ?: ""
}

// --- HÀM LẤY ẢNH THUMBNAIL TỪ ITUNES API ---
suspend fun getITunesThumbnail(title: String, artist: String): String {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // Tẩy bớt dấu ngoặc đơn (nếu có) để tìm kiếm chính xác hơn
            val cleanTitle = title.replace(Regex("\\(.*?\\)"), "").trim()
            val query = java.net.URLEncoder.encode("$cleanTitle $artist", "UTF-8")
            val url = java.net.URL("https://itunes.apple.com/search?term=$query&entity=song&limit=1")

            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    return@withContext results.getJSONObject(0).optString("artworkUrl100", "")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ""
    }
}