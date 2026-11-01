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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
private const val TAB_APPEARANCE_PREF_THEME_MODE = "appearance_theme_mode"
private const val TAB_APPEARANCE_PREF_CURSOR_COLOR = "appearance_cursor_color"
private const val TAB_APPEARANCE_PREF_ACTIVE_REGION_COLOR = "appearance_active_region_color"
private const val TAB_APPEARANCE_PREF_TAB_COLOR = "appearance_tab_color"
private const val TAB_APPEARANCE_PREF_NOTE_COLOR = "appearance_note_color"
private const val TAB_APPEARANCE_PREF_FRETBOARD_COLOR = "appearance_fretboard_color"
private const val TAB_METRONOME_PREFS_NAME = "SongTabMetronomePrefs"
private const val TAB_METRONOME_PREF_ENABLED = "metronome_enabled"

private enum class TabAppearanceThemeMode(val storedValue: String) {
    FOLLOW_APP("follow_app"),
    LIGHT("light"),
    DARK("dark");

    fun resolve(appIsDark: Boolean): Boolean = when (this) {
        FOLLOW_APP -> appIsDark
        LIGHT -> false
        DARK -> true
    }

    fun title(currentLang: String): String {
        val isVietnamese = currentLang
            .lowercase(Locale.ROOT)
            .substringBefore('-')
            .substringBefore('_') == "vi"
        return when (this) {
            FOLLOW_APP -> if (isVietnamese) "Theo ứng dụng" else "Follow app"
            LIGHT -> if (isVietnamese) "Sáng" else "Light"
            DARK -> if (isVietnamese) "Tối" else "Dark"
        }
    }

    companion object {
        fun fromStoredValue(value: String?): TabAppearanceThemeMode =
            values().firstOrNull { it.storedValue == value } ?: FOLLOW_APP
    }
}

private data class TabAppearanceColorOption(
    val id: String,
    val vietnameseName: String,
    val englishName: String,
    val lightHex: String,
    val darkHex: String
) {
    fun resolve(isDark: Boolean): String = if (isDark) darkHex else lightHex

    fun title(currentLang: String): String {
        val isVietnamese = currentLang
            .lowercase(Locale.ROOT)
            .substringBefore('-')
            .substringBefore('_') == "vi"
        return if (isVietnamese) vietnameseName else englishName
    }
}

private val TAB_CURSOR_COLOR_OPTIONS = listOf(
    TabAppearanceColorOption("purple", "Tím", "Purple", "#7C3AED", "#C084FC"),
    TabAppearanceColorOption("blue", "Xanh dương", "Blue", "#2563EB", "#60A5FA"),
    TabAppearanceColorOption("cyan", "Xanh ngọc", "Cyan", "#0891B2", "#22D3EE"),
    TabAppearanceColorOption("green", "Xanh lá", "Green", "#059669", "#34D399"),
    TabAppearanceColorOption("orange", "Cam", "Orange", "#EA580C", "#FB923C"),
    TabAppearanceColorOption("red", "Đỏ", "Red", "#DC2626", "#F87171")
)

private val TAB_ACTIVE_REGION_COLOR_OPTIONS = listOf(
    TabAppearanceColorOption("lavender", "Tím nhạt", "Lavender", "#8B5CF6", "#7C3AED"),
    TabAppearanceColorOption("sky", "Xanh trời", "Sky", "#0EA5E9", "#0284C7"),
    TabAppearanceColorOption("mint", "Bạc hà", "Mint", "#10B981", "#059669"),
    TabAppearanceColorOption("amber", "Hổ phách", "Amber", "#F59E0B", "#D97706"),
    TabAppearanceColorOption("rose", "Hồng", "Rose", "#F43F5E", "#E11D48"),
    TabAppearanceColorOption("slate", "Xám xanh", "Slate", "#64748B", "#475569")
)

private val TAB_GLYPH_COLOR_OPTIONS = listOf(
    TabAppearanceColorOption("auto", "Tự động", "Automatic", "#111827", "#F8FAFC"),
    TabAppearanceColorOption("purple", "Tím", "Purple", "#5B21B6", "#E9D5FF"),
    TabAppearanceColorOption("blue", "Xanh dương", "Blue", "#1D4ED8", "#BFDBFE"),
    TabAppearanceColorOption("green", "Xanh lá", "Green", "#047857", "#A7F3D0"),
    TabAppearanceColorOption("amber", "Hổ phách", "Amber", "#92400E", "#FDE68A"),
    TabAppearanceColorOption("rose", "Hồng", "Rose", "#9F1239", "#FECDD3")
)

private val TAB_NOTE_COLOR_OPTIONS = listOf(
    TabAppearanceColorOption("yellow", "Vàng", "Yellow", "#FBBF24", "#FFD43B"),
    TabAppearanceColorOption("orange", "Cam", "Orange", "#F97316", "#FB923C"),
    TabAppearanceColorOption("cyan", "Xanh ngọc", "Cyan", "#06B6D4", "#22D3EE"),
    TabAppearanceColorOption("green", "Xanh lá", "Green", "#10B981", "#34D399"),
    TabAppearanceColorOption("pink", "Hồng", "Pink", "#EC4899", "#F472B6"),
    TabAppearanceColorOption("purple", "Tím", "Purple", "#8B5CF6", "#C084FC")
)

private val TAB_FRETBOARD_COLOR_OPTIONS = listOf(
    TabAppearanceColorOption("auto", "Tự động", "Automatic", "#F8FAFC", "#171B24"),
    TabAppearanceColorOption("slate", "Xám đá", "Slate", "#E2E8F0", "#0F172A"),
    TabAppearanceColorOption("navy", "Xanh navy", "Navy", "#DBEAFE", "#0B1F3A"),
    TabAppearanceColorOption("forest", "Xanh rừng", "Forest", "#DCFCE7", "#10251B"),
    TabAppearanceColorOption("wood", "Màu gỗ", "Wood", "#F3E2C7", "#3A2418"),
    TabAppearanceColorOption("graphite", "Than chì", "Graphite", "#E5E7EB", "#111827")
)

private fun tabAppearanceOption(
    options: List<TabAppearanceColorOption>,
    id: String?,
    fallbackId: String
): TabAppearanceColorOption = options.firstOrNull { it.id == id }
    ?: options.first { it.id == fallbackId }

private fun tabAppearanceComposeColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color(0xFF7C3AED))

private fun tabAppearanceUsesDarkText(hex: String): Boolean {
    val color = android.graphics.Color.parseColor(hex)
    val red = android.graphics.Color.red(color) / 255.0
    val green = android.graphics.Color.green(color) / 255.0
    val blue = android.graphics.Color.blue(color) / 255.0
    fun channel(value: Double): Double =
        if (value <= 0.03928) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    val luminance = 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    return luminance > 0.48
}

// Chuc nang Ban nhac duoc su dung mien phi khong gioi han thoi gian.
// Van giu buoc dang nhap Google de su dung tai khoan hien co.

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
    SCORE_TAB_FRETBOARD(jsValue = "scoretab_fretboard", buttonLabel = "S+T+F"),
    TAB_ONLY(jsValue = "tab", buttonLabel = "TAB"),
    PARTITION_ONLY(jsValue = "score", buttonLabel = "P");

    fun menuTitle(currentLang: String): String = when (this) {
        SCORE_TAB -> soundFontText(
            "tab_display_score_tab_title",
            currentLang,
            "Khuông + TAB",
            "Notation + TAB"
        )
        SCORE_TAB_FRETBOARD -> soundFontText(
            "tab_display_fretboard_title",
            currentLang,
            "Khuông + TAB + Cần đàn",
            "Notation + TAB + Fretboard"
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
        SCORE_TAB_FRETBOARD -> soundFontText(
            "tab_display_fretboard_desc",
            currentLang,
            "Thu nhỏ khuông + TAB; hiển thị vị trí nốt trên cần đàn khi Play",
            "Compact notation + TAB with live fretboard notes"
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
private fun SongTabMetronomeIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val lineWidth = size.minDimension * 0.085f
        val frame = Path().apply {
            moveTo(w * 0.28f, h * 0.80f)
            lineTo(w * 0.44f, h * 0.25f)
            lineTo(w * 0.56f, h * 0.25f)
            lineTo(w * 0.72f, h * 0.80f)
            close()
        }
        drawPath(frame, color, style = Stroke(width = lineWidth))
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.66f),
            end = Offset(w * 0.76f, h * 0.13f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
        drawCircle(color, radius = lineWidth * 1.05f, center = Offset(w * 0.76f, h * 0.13f))
        drawLine(
            color = color,
            start = Offset(w * 0.21f, h * 0.86f),
            end = Offset(w * 0.79f, h * 0.86f),
            strokeWidth = lineWidth,
            cap = StrokeCap.Round
        )
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
        val subject = accessText(
            "tab_access_contact_subject",
            "Liên hệ hỗ trợ chức năng Bản nhạc TunerTools",
            "Contact TunerTools Sheet Music support"
        )
        val body = buildString {
            append(
                accessText(
                    "tab_access_contact_body",
                    "Xin chào TunerTools, vui lòng hỗ trợ tôi sử dụng chức năng Bản nhạc miễn phí",
                    "Hello TunerTools, please help me use the free Sheet Music feature"
                )
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

    val title = when (state) {
        DocumentAccessState.Checking -> accessText(
            "tab_access_checking_title", "Đang kiểm tra tài khoản...", "Checking account..."
        )
        is DocumentAccessState.LoginRequired -> accessText(
            "tab_access_login_title", "Cần đăng nhập Google", "Google sign-in required"
        )
        is DocumentAccessState.Denied -> accessText(
            "tab_access_error_title", "Không thể truy cập", "Unable to access"
        )
        is DocumentAccessState.Error -> accessText(
            "tab_access_error_title", "Không thể tải dữ liệu", "Unable to load data"
        )
        is DocumentAccessState.Granted -> ""
    }

    val message = when (state) {
        DocumentAccessState.Checking -> accessText(
            "tab_access_checking_message", "Vui lòng chờ trong giây lát.", "Please wait a moment."
        )
        is DocumentAccessState.LoginRequired -> state.message.ifBlank {
            accessText(
                "tab_free_login_message",
                "Đăng nhập Google để sử dụng chức năng Bản nhạc miễn phí, không giới hạn thời gian.",
                "Sign in with Google to use Sheet Music for free, with no time limit."
            )
        }
        is DocumentAccessState.Denied -> state.message.ifBlank {
            accessText(
                "tab_access_error_message",
                "Không thể tải dữ liệu Bản nhạc. Hãy thử lại.",
                "Unable to load Sheet Music. Please retry."
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
                        state == DocumentAccessState.Checking -> Icons.Default.Refresh
                        state is DocumentAccessState.LoginRequired -> Icons.Default.Login
                        state is DocumentAccessState.Denied -> Icons.Default.Lock
                        state is DocumentAccessState.Error -> Icons.Default.Warning
                        else -> Icons.Default.Lock
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (state is DocumentAccessState.Error) {
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

                    if (state is DocumentAccessState.Denied) {
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
                    signedInEmail = ""
                    accessState = DocumentAccessState.LoginRequired(
                        message = soundFontText(
                            "tab_free_login_message",
                            currentLang,
                            "Đăng nhập Google để sử dụng chức năng Bản nhạc miễn phí, không giới hạn thời gian.",
                            "Sign in with Google to use Sheet Music for free, with no time limit."
                        )
                    )
                } else {
                    // Mọi tài khoản Google đăng nhập thành công đều được sử dụng miễn phí lâu dài.
                    signedInEmail = email.trim().lowercase(Locale.ROOT)
                    accessState = try {
                        DocumentAccessApi.loginWithAccount(context, account, DocumentAccessScope.SONG_TAB)
                    } catch (error: Exception) {
                        // Lỗi kiểm tra phiên máy chủ không làm mất quyền dùng miễn phí.
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
            signedInEmail = ""
            accessState = DocumentAccessState.LoginRequired(
                message = soundFontText(
                    "tab_free_login_message",
                    currentLang,
                    "Đăng nhập Google để sử dụng chức năng Bản nhạc miễn phí, không giới hạn thời gian.",
                    "Sign in with Google to use Sheet Music for free, with no time limit."
                )
            )
        }
    }

    fun switchGoogleAccount() {
        resetInitialLoad()
        accessState = DocumentAccessState.Checking
        DocumentAccessStore.clear(context)
        // Đăng xuất trước để người dùng chủ động chọn tài khoản Google khác.
        googleSignInClient.signOut().addOnCompleteListener {
            signedInEmail = ""
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    LaunchedEffect(accessRefresh) {
        resetInitialLoad()
        accessState = DocumentAccessState.Checking

        val account = GoogleSignIn.getLastSignedInAccount(context)
        signedInEmail = account?.email.orEmpty().trim().lowercase(Locale.ROOT)

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

    val grantedState = accessState as? DocumentAccessState.Granted
    val effectiveEmail = grantedState?.info?.email
        ?.trim()
        ?.lowercase(Locale.ROOT)
        ?.takeIf { it.isNotBlank() }
        ?: signedInEmail
    val isSignedIn = effectiveEmail.isNotBlank()
    // Khong ap dung gioi han 30 ngay hoac quyen canView/canManage de mo Ban nhac.
    // Dang nhap Google la dieu kien duy nhat o phia giao dien; loi phien server
    // khong duoc phep khoa tinh nang mien phi.
    val canUseSheetMusic = isSignedIn

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
        (isSignedIn && accessState is DocumentAccessState.Checking) ||
                (canUseSheetMusic && initialSongs == null && initialLoadError == null)

    when {
        // Cùng một nhánh Composable được giữ trong cả hai giai đoạn nên sóng âm
        // không đổi vị trí, không chớp tiêu đề và không xuất hiện loader thứ hai.
        showUnifiedLoading -> SongTabAccessLoadingScreen(isDark = isDark)

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
            state = DocumentAccessState.LoginRequired(
                message = soundFontText(
                    "tab_free_login_message",
                    currentLang,
                    "Đăng nhập Google để sử dụng chức năng Bản nhạc miễn phí, không giới hạn thời gian.",
                    "Sign in with Google to use Sheet Music for free, with no time limit."
                )
            ),
            currentLang = currentLang,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = { accessRefresh++ }
        )

        else -> SongTabAccessGateScreen(
            isDark = isDark,
            state = DocumentAccessState.Error(
                soundFontText(
                    "tab_access_error_message",
                    currentLang,
                    "Không thể tải Bản nhạc. Hãy thử lại.",
                    "Could not load Sheet Music. Please retry."
                )
            ),
            currentLang = currentLang,
            onClose = onClose,
            onLogin = { switchGoogleAccount() },
            onRetry = { accessRefresh++ }
        )
    }
}

@Composable
private fun TabAppearanceColorPicker(
    title: String,
    currentLang: String,
    options: List<TabAppearanceColorOption>,
    selectedId: String,
    isDarkPreview: Boolean,
    accentColor: Color,
    textColor: Color,
    secondaryTextColor: Color,
    onSelected: (String) -> Unit
) {
    val selectedOption = options.firstOrNull { it.id == selectedId } ?: options.first()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedOption.title(currentLang),
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val selected = option.id == selectedId
                val resolvedHex = option.resolve(isDarkPreview)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onSelected(option.id) }
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) accentColor else secondaryTextColor.copy(alpha = 0.32f),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(tabAppearanceComposeColor(resolvedHex)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (tabAppearanceUsesDarkText(resolvedHex)) {
                                        Color(0xFF111827)
                                    } else {
                                        Color.White
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabAppearancePreview(
    isDarkPreview: Boolean,
    cursorHex: String,
    activeRegionHex: String,
    tabHex: String,
    noteHex: String,
    fretboardHex: String
) {
    val previewSurface = if (isDarkPreview) Color(0xFF0B0F1A) else Color.White
    val previewBorder = if (isDarkPreview) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f)
    val cursorColor = tabAppearanceComposeColor(cursorHex)
    val regionColor = tabAppearanceComposeColor(activeRegionHex)
    val tabColor = tabAppearanceComposeColor(tabHex)
    val noteColor = tabAppearanceComposeColor(noteHex)
    val boardColor = tabAppearanceComposeColor(fretboardHex)
    val boardTextColor = if (tabAppearanceUsesDarkText(fretboardHex)) Color(0xFF1F2937) else Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        shape = RoundedCornerShape(16.dp),
        color = previewSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, previewBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDarkPreview) Color(0xFF111827) else Color(0xFFF8FAFC))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(0.48f)
                        .fillMaxHeight()
                        .background(regionColor.copy(alpha = if (isDarkPreview) 0.28f else 0.18f))
                )
                repeat(5) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (15 + index * 9).dp)
                            .background(tabColor.copy(alpha = if (index == 2) 0.9f else 0.48f))
                    )
                }
                listOf(0.20f, 0.36f, 0.58f, 0.78f).forEachIndexed { index, fraction ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (fraction * 290).dp, y = (25 + (index % 2) * 12).dp)
                            .size(9.dp)
                            .background(tabColor, CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(cursorColor)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 2.dp)
                        .size(9.dp)
                        .background(cursorColor, CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(boardColor)
                    .border(1.dp, boardTextColor.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (12 + index * 12).dp)
                            .background(boardTextColor.copy(alpha = 0.58f))
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 30.dp, y = (-6).dp)
                        .size(24.dp)
                        .background(noteColor, CircleShape)
                        .border(2.dp, boardTextColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        color = if (tabAppearanceUsesDarkText(noteHex)) Color(0xFF111827) else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun TabAppearanceSettingsDialog(
    currentLang: String,
    appIsDark: Boolean,
    themeMode: TabAppearanceThemeMode,
    cursorColorId: String,
    activeRegionColorId: String,
    tabColorId: String,
    noteColorId: String,
    fretboardColorId: String,
    onThemeModeChange: (TabAppearanceThemeMode) -> Unit,
    onCursorColorChange: (String) -> Unit,
    onActiveRegionColorChange: (String) -> Unit,
    onTabColorChange: (String) -> Unit,
    onNoteColorChange: (String) -> Unit,
    onFretboardColorChange: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val isVietnamese = currentLang
        .lowercase(Locale.ROOT)
        .substringBefore('-')
        .substringBefore('_') == "vi"
    val previewIsDark = themeMode.resolve(appIsDark)
    val cursorOption = tabAppearanceOption(TAB_CURSOR_COLOR_OPTIONS, cursorColorId, "purple")
    val regionOption = tabAppearanceOption(TAB_ACTIVE_REGION_COLOR_OPTIONS, activeRegionColorId, "lavender")
    val tabOption = tabAppearanceOption(TAB_GLYPH_COLOR_OPTIONS, tabColorId, "auto")
    val noteOption = tabAppearanceOption(TAB_NOTE_COLOR_OPTIONS, noteColorId, "yellow")
    val boardOption = tabAppearanceOption(TAB_FRETBOARD_COLOR_OPTIONS, fretboardColorId, "auto")
    val dialogBackground = if (previewIsDark) Color(0xFF11151F) else Color(0xFFF8FAFC)
    val cardBackground = if (previewIsDark) Color(0xFF1A202C) else Color.White
    val textColor = if (previewIsDark) Color.White else Color(0xFF101828)
    val secondaryTextColor = if (previewIsDark) Color(0xFFB8C0CC) else Color(0xFF667085)
    val dividerColor = if (previewIsDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val accentColor = tabAppearanceComposeColor(cursorOption.resolve(previewIsDark))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = dialogBackground,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(25.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVietnamese) "Tùy chỉnh hiển thị" else "Display customization",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isVietnamese) {
                                "Thay đổi được áp dụng ngay và tự động lưu"
                            } else {
                                "Changes apply instantly and are saved automatically"
                            },
                            color = secondaryTextColor,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isVietnamese) "Đóng" else "Close",
                            tint = textColor
                        )
                    }
                }

                HorizontalDivider(color = dividerColor)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TabAppearancePreview(
                        isDarkPreview = previewIsDark,
                        cursorHex = cursorOption.resolve(previewIsDark),
                        activeRegionHex = regionOption.resolve(previewIsDark),
                        tabHex = tabOption.resolve(previewIsDark),
                        noteHex = noteOption.resolve(previewIsDark),
                        fretboardHex = boardOption.resolve(previewIsDark)
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, dividerColor)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isVietnamese) "Chế độ hiển thị" else "Display mode",
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TabAppearanceThemeMode.values().forEach { mode ->
                                    val selected = mode == themeMode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                if (selected) accentColor else dialogBackground
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) accentColor else dividerColor,
                                                RoundedCornerShape(11.dp)
                                            )
                                            .clickable { onThemeModeChange(mode) }
                                            .padding(horizontal = 4.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.title(currentLang),
                                            color = if (selected) {
                                                if (tabAppearanceUsesDarkText(cursorOption.resolve(previewIsDark))) {
                                                    Color(0xFF111827)
                                                } else {
                                                    Color.White
                                                }
                                            } else {
                                                textColor
                                            },
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = cardBackground,
                        border = androidx.compose.foundation.BorderStroke(1.dp, dividerColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TabAppearanceColorPicker(
                                title = if (isVietnamese) "Màu con trỏ" else "Playhead color",
                                currentLang = currentLang,
                                options = TAB_CURSOR_COLOR_OPTIONS,
                                selectedId = cursorColorId,
                                isDarkPreview = previewIsDark,
                                accentColor = accentColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onSelected = onCursorColorChange
                            )
                            HorizontalDivider(color = dividerColor)
                            TabAppearanceColorPicker(
                                title = if (isVietnamese) "Nền vùng đang chạy" else "Active region background",
                                currentLang = currentLang,
                                options = TAB_ACTIVE_REGION_COLOR_OPTIONS,
                                selectedId = activeRegionColorId,
                                isDarkPreview = previewIsDark,
                                accentColor = accentColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onSelected = onActiveRegionColorChange
                            )
                            HorizontalDivider(color = dividerColor)
                            TabAppearanceColorPicker(
                                title = if (isVietnamese) "Khuông nhạc và TAB" else "Notation and TAB",
                                currentLang = currentLang,
                                options = TAB_GLYPH_COLOR_OPTIONS,
                                selectedId = tabColorId,
                                isDarkPreview = previewIsDark,
                                accentColor = accentColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onSelected = onTabColorChange
                            )
                            HorizontalDivider(color = dividerColor)
                            TabAppearanceColorPicker(
                                title = if (isVietnamese) "Nốt trên cần đàn" else "Fretboard notes",
                                currentLang = currentLang,
                                options = TAB_NOTE_COLOR_OPTIONS,
                                selectedId = noteColorId,
                                isDarkPreview = previewIsDark,
                                accentColor = accentColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onSelected = onNoteColorChange
                            )
                            HorizontalDivider(color = dividerColor)
                            TabAppearanceColorPicker(
                                title = if (isVietnamese) "Màu mặt cần đàn" else "Fretboard surface",
                                currentLang = currentLang,
                                options = TAB_FRETBOARD_COLOR_OPTIONS,
                                selectedId = fretboardColorId,
                                isDarkPreview = previewIsDark,
                                accentColor = accentColor,
                                textColor = textColor,
                                secondaryTextColor = secondaryTextColor,
                                onSelected = onFretboardColorChange
                            )
                        }
                    }
                }

                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text(
                            text = if (isVietnamese) "Mặc định" else "Reset",
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text(
                            text = if (isVietnamese) "Hoàn tất" else "Done",
                            color = if (tabAppearanceUsesDarkText(cursorOption.resolve(previewIsDark))) {
                                Color(0xFF111827)
                            } else {
                                Color.White
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
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
    val metronomePrefs = remember(context) {
        context.getSharedPreferences(TAB_METRONOME_PREFS_NAME, Context.MODE_PRIVATE)
    }
    var metronomeEnabled by remember(context) {
        mutableStateOf(metronomePrefs.getBoolean(TAB_METRONOME_PREF_ENABLED, false))
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

    // Tùy chỉnh giao diện TAB được lưu độc lập với cài đặt chung của ứng dụng.
    // Chế độ "Theo ứng dụng" vẫn tự đồng bộ khi người dùng đổi theme ở trang Cài đặt.
    val initialAppearanceThemeMode = remember(tabDisplayPrefs) {
        TabAppearanceThemeMode.fromStoredValue(
            tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_THEME_MODE, null)
        )
    }
    var appearanceThemeMode by remember { mutableStateOf(initialAppearanceThemeMode) }
    var appearanceCursorColorId by remember(tabDisplayPrefs) {
        mutableStateOf(
            tabAppearanceOption(
                TAB_CURSOR_COLOR_OPTIONS,
                tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_CURSOR_COLOR, null),
                "purple"
            ).id
        )
    }
    var appearanceActiveRegionColorId by remember(tabDisplayPrefs) {
        mutableStateOf(
            tabAppearanceOption(
                TAB_ACTIVE_REGION_COLOR_OPTIONS,
                tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_ACTIVE_REGION_COLOR, null),
                "lavender"
            ).id
        )
    }
    var appearanceTabColorId by remember(tabDisplayPrefs) {
        mutableStateOf(
            tabAppearanceOption(
                TAB_GLYPH_COLOR_OPTIONS,
                tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_TAB_COLOR, null),
                "auto"
            ).id
        )
    }
    var appearanceNoteColorId by remember(tabDisplayPrefs) {
        mutableStateOf(
            tabAppearanceOption(
                TAB_NOTE_COLOR_OPTIONS,
                tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_NOTE_COLOR, null),
                "yellow"
            ).id
        )
    }
    var appearanceFretboardColorId by remember(tabDisplayPrefs) {
        mutableStateOf(
            tabAppearanceOption(
                TAB_FRETBOARD_COLOR_OPTIONS,
                tabDisplayPrefs.getString(TAB_APPEARANCE_PREF_FRETBOARD_COLOR, null),
                "auto"
            ).id
        )
    }
    var showAppearanceSettings by remember { mutableStateOf(false) }
    var localIsDark by remember {
        mutableStateOf(initialAppearanceThemeMode.resolve(isDark))
    }

    val appearanceCursorHex = tabAppearanceOption(
        TAB_CURSOR_COLOR_OPTIONS, appearanceCursorColorId, "purple"
    ).resolve(localIsDark)
    val appearanceActiveRegionHex = tabAppearanceOption(
        TAB_ACTIVE_REGION_COLOR_OPTIONS, appearanceActiveRegionColorId, "lavender"
    ).resolve(localIsDark)
    val appearanceTabHex = tabAppearanceOption(
        TAB_GLYPH_COLOR_OPTIONS, appearanceTabColorId, "auto"
    ).resolve(localIsDark)
    val appearanceNoteHex = tabAppearanceOption(
        TAB_NOTE_COLOR_OPTIONS, appearanceNoteColorId, "yellow"
    ).resolve(localIsDark)
    val appearanceFretboardHex = tabAppearanceOption(
        TAB_FRETBOARD_COLOR_OPTIONS, appearanceFretboardColorId, "auto"
    ).resolve(localIsDark)

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

    LaunchedEffect(isDark, appearanceThemeMode) {
        val resolvedDark = appearanceThemeMode.resolve(isDark)
        if (localIsDark != resolvedDark) localIsDark = resolvedDark
    }

    fun setAppearanceThemeMode(mode: TabAppearanceThemeMode) {
        appearanceThemeMode = mode
        localIsDark = mode.resolve(isDark)
        tabDisplayPrefs.edit()
            .putString(TAB_APPEARANCE_PREF_THEME_MODE, mode.storedValue)
            .apply()
    }

    fun setAppearanceColor(
        preferenceKey: String,
        value: String,
        onStateChanged: (String) -> Unit
    ) {
        onStateChanged(value)
        tabDisplayPrefs.edit().putString(preferenceKey, value).apply()
    }

    fun resetAppearanceSettings() {
        appearanceThemeMode = TabAppearanceThemeMode.FOLLOW_APP
        appearanceCursorColorId = "purple"
        appearanceActiveRegionColorId = "lavender"
        appearanceTabColorId = "auto"
        appearanceNoteColorId = "yellow"
        appearanceFretboardColorId = "auto"
        localIsDark = isDark
        tabDisplayPrefs.edit()
            .putString(
                TAB_APPEARANCE_PREF_THEME_MODE,
                TabAppearanceThemeMode.FOLLOW_APP.storedValue
            )
            .putString(TAB_APPEARANCE_PREF_CURSOR_COLOR, "purple")
            .putString(TAB_APPEARANCE_PREF_ACTIVE_REGION_COLOR, "lavender")
            .putString(TAB_APPEARANCE_PREF_TAB_COLOR, "auto")
            .putString(TAB_APPEARANCE_PREF_NOTE_COLOR, "yellow")
            .putString(TAB_APPEARANCE_PREF_FRETBOARD_COLOR, "auto")
            .apply()
    }

    // Gõ nhịp dùng chính bộ phát alphaTab: không dùng Timer/MediaPlayer riêng
    // để nhịp không lệch khi Pause/Play hoặc thay BPM.
    fun setMetronomeEnabled(enabled: Boolean) {
        metronomeEnabled = enabled
        metronomePrefs.edit().putBoolean(TAB_METRONOME_PREF_ENABLED, enabled).apply()
        webViewInstance?.evaluateJavascript(
            "javascript:if (typeof setMetronomeEnabled === 'function') setMetronomeEnabled($enabled);",
            null
        )
    }

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
                fun onFretboardClosed() {
                    mainHandler.post {
                        selectedDisplayMode = TabDisplayMode.SCORE_TAB
                        displayModeMenuExpanded = false
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
                initialShowNoteNames = showNoteNames,
                initialMetronomeEnabled = metronomeEnabled,
                initialCursorColor = appearanceCursorHex,
                initialActiveRegionColor = appearanceActiveRegionHex,
                initialTabColor = appearanceTabHex,
                initialNoteColor = appearanceNoteHex,
                initialFretboardColor = appearanceFretboardHex
            ), "text/html", "UTF-8", null)
        }
    }

    LaunchedEffect(webView) {
        webViewInstance = webView
        syncTabViewport(webView)
        if (isTabJsReady.get()) dispatchPendingTabSong(webView)
    }

    // Đồng bộ toàn bộ bảng màu ngay khi người dùng đổi lựa chọn, không tải lại bài.
    LaunchedEffect(
        webViewInstance,
        localIsDark,
        appearanceCursorColorId,
        appearanceActiveRegionColorId,
        appearanceTabColorId,
        appearanceNoteColorId,
        appearanceFretboardColorId
    ) {
        val script = "javascript:if(typeof applyVisualPreferences === 'function'){" +
                "applyVisualPreferences(" +
                localIsDark + "," +
                JSONObject.quote(appearanceCursorHex) + "," +
                JSONObject.quote(appearanceActiveRegionHex) + "," +
                JSONObject.quote(appearanceTabHex) + "," +
                JSONObject.quote(appearanceNoteHex) + "," +
                JSONObject.quote(appearanceFretboardHex) +
                ");}"
        webViewInstance?.evaluateJavascript(script, null)
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
        val previousOffsetX = offsetX
        val previousOffsetY = offsetY
        val previousBannerVisibility = showTabBottomBanner

        selectedDisplayMode = mode
        if (mode == TabDisplayMode.SCORE_TAB_FRETBOARD) {
            // Bố cục S+T+F phải cố định để mắt luôn thấy TAB ngay trên cần đàn.
            // Không giữ vị trí thanh điều khiển đã bị kéo ở chế độ thông thường.
            offsetX = 0f
            offsetY = 0f
            showTabBottomBanner = false
        }
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
                    offsetX = previousOffsetX
                    offsetY = previousOffsetY
                    showTabBottomBanner = previousBannerVisibility
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

    // Một số file GP3/GP4/GP5 cũ chứa tên tiếng Việt ở bảng mã ANSI, khiến
    // API hoặc alphaTab giải mã thành "V?ng Tr?ng Kh�c". Không dùng tên đã lỗi
    // làm fallback cho khuông nhạc; ưu tiên tên sạch lấy từ danh sách/đường dẫn.
    fun isUsableSongTitle(raw: String): Boolean {
        val title = raw.replace(Regex("\\s+"), " ").trim()
        if (title.isBlank() || title.contains('\uFFFD')) return false
        if (title.all { it == '?' || it.isWhitespace() }) return false
        if (Regex("""[\p{L}\p{N}]\?[\p{L}\p{N}]""").containsMatchIn(title)) return false
        if (Regex("""(?:Ã[\u0080-\u00BF]|Â[\u0080-\u00BF]|á[º»])""").containsMatchIn(title)) return false
        if (title.lowercase(Locale.ROOT) in setOf("không tên", "bài nhạc", "bản nhạc", "untitled", "n/a")) return false
        return true
    }

    fun resolveScoreFallbackTitle(song: TuneToolsSong): String {
        val providedTitle = song.title.replace(Regex("\\s+"), " ").trim()
        if (isUsableSongTitle(providedTitle)) return providedTitle

        // Tên API bị lỗi thì thử relativePath trước; nếu thiếu, lấy tên file
        // từ URL (hỗ trợ cả tên có dấu đã percent-encode, không suy đoán dấu).
        fun titleFromPath(rawPath: String): String? {
            if (rawPath.isBlank()) return null
            val path = Uri.decode(rawPath.substringBefore('?').substringBefore('#'))
            val fileName = path.trim('/').substringAfterLast('/').trim()
            val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            if (extension !in SUPPORTED_GP_EXTENSIONS) return null

            val stem = fileName.substringBeforeLast('.')
                .replace('_', ' ')
                .replace(Regex("\\s+"), " ")
                .trim()
            val title = stem.substringAfter(" - ", stem).trim()
            return title.takeIf { isUsableSongTitle(it) && it.any(Char::isLetter) }
        }

        return titleFromPath(song.relativePath)
            ?: titleFromPath(song.fileUrl)
            ?: "Bản nhạc"
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
                            // Mở bảng chọn theme và màu hiển thị chi tiết.
                            IconButton(
                                onClick = { showAppearanceSettings = true },
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
                                    imageVector = if (localIsDark) {
                                        Icons.Default.LightMode
                                    } else {
                                        Icons.Default.DarkMode
                                    },
                                    contentDescription = soundFontText(
                                        "tab_appearance_settings",
                                        currentLang,
                                        "Tùy chỉnh hiển thị",
                                        "Display customization"
                                    ),
                                    tint = tabAppearanceComposeColor(appearanceCursorHex),
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
                                // Chế độ cần đàn chừa vùng dưới cùng cho bộ điều khiển,
                                // không để thanh Play che các vị trí nốt trên dây.
                                .padding(
                                    bottom = if (selectedDisplayMode == TabDisplayMode.SCORE_TAB_FRETBOARD) {
                                        8.dp
                                    } else {
                                        40.dp
                                    }
                                )
                                .zIndex(10f)
                                // Giữ thanh điều khiển đủ rõ để bật/tắt metronome lúc Play.
                                .alpha(
                                    if (
                                        isPlaying &&
                                        selectedDisplayMode != TabDisplayMode.SCORE_TAB_FRETBOARD
                                    ) 0.75f else 1f
                                )
                                .pointerInput(selectedDisplayMode) {
                                    // Ở S+T+F, khóa thanh điều khiển tại đáy để bố cục
                                    // TAB → cần đàn → Play luôn ổn định, không bị kéo lệch.
                                    if (selectedDisplayMode != TabDisplayMode.SCORE_TAB_FRETBOARD) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                        }
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

                                // Metronome bật/tắt trong lúc phát; nhịp gõ lấy từ MIDI của
                                // alphaTab nên tự khớp với tempo và số chỉ nhịp của bản GP.
                                val metronomeAction = soundFontText(
                                    if (metronomeEnabled) "tab_metronome_turn_off" else "tab_metronome_turn_on",
                                    currentLang,
                                    if (metronomeEnabled) "Tắt máy đếm nhịp" else "Bật máy đếm nhịp",
                                    if (metronomeEnabled) "Turn off metronome" else "Turn on metronome"
                                )
                                IconButton(
                                    onClick = { setMetronomeEnabled(!metronomeEnabled) },
                                    enabled = hasScore && !isExportingPdf,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (metronomeEnabled) accentColor.copy(alpha = 0.22f)
                                            else if (localIsDark) Color.Black else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            1.dp,
                                            if (metronomeEnabled) accentColor
                                            else Color.Gray.copy(alpha = 0.3f),
                                            CircleShape
                                        )
                                        .semantics { contentDescription = metronomeAction }
                                ) {
                                    SongTabMetronomeIcon(
                                        modifier = Modifier.size(21.dp),
                                        color = if (metronomeEnabled) accentColor
                                        else if (localIsDark) Color.White else textColor
                                    )
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

    if (showAppearanceSettings) {
        TabAppearanceSettingsDialog(
            currentLang = currentLang,
            appIsDark = isDark,
            themeMode = appearanceThemeMode,
            cursorColorId = appearanceCursorColorId,
            activeRegionColorId = appearanceActiveRegionColorId,
            tabColorId = appearanceTabColorId,
            noteColorId = appearanceNoteColorId,
            fretboardColorId = appearanceFretboardColorId,
            onThemeModeChange = ::setAppearanceThemeMode,
            onCursorColorChange = { value ->
                setAppearanceColor(TAB_APPEARANCE_PREF_CURSOR_COLOR, value) {
                    appearanceCursorColorId = it
                }
            },
            onActiveRegionColorChange = { value ->
                setAppearanceColor(TAB_APPEARANCE_PREF_ACTIVE_REGION_COLOR, value) {
                    appearanceActiveRegionColorId = it
                }
            },
            onTabColorChange = { value ->
                setAppearanceColor(TAB_APPEARANCE_PREF_TAB_COLOR, value) {
                    appearanceTabColorId = it
                }
            },
            onNoteColorChange = { value ->
                setAppearanceColor(TAB_APPEARANCE_PREF_NOTE_COLOR, value) {
                    appearanceNoteColorId = it
                }
            },
            onFretboardColorChange = { value ->
                setAppearanceColor(TAB_APPEARANCE_PREF_FRETBOARD_COLOR, value) {
                    appearanceFretboardColorId = it
                }
            },
            onReset = ::resetAppearanceSettings,
            onDismiss = { showAppearanceSettings = false }
        )
    }
}

// Hàm cung cấp HTML nhúng JS alphaTab
private fun getAlphaTabHtml(
    isDarkInitial: Boolean,
    initialSoundFontMode: String,
    initialSoundFontUrl: String,
    currentLang: String,
    initialShowNoteNames: Boolean,
    initialMetronomeEnabled: Boolean,
    initialCursorColor: String,
    initialActiveRegionColor: String,
    initialTabColor: String,
    initialNoteColor: String,
    initialFretboardColor: String
): String {
    val quotedInitialSoundFontMode = JSONObject.quote(initialSoundFontMode)
    val quotedInitialSoundFontUrl = JSONObject.quote(initialSoundFontUrl)
    val quotedStandardSoundFontUrl = JSONObject.quote(STANDARD_SOUNDFONT_URL)
    val quotedInitialCursorColor = JSONObject.quote(initialCursorColor)
    val quotedInitialActiveRegionColor = JSONObject.quote(initialActiveRegionColor)
    val quotedInitialTabColor = JSONObject.quote(initialTabColor)
    val quotedInitialNoteColor = JSONObject.quote(initialNoteColor)
    val quotedInitialFretboardColor = JSONObject.quote(initialFretboardColor)
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
    val fretboardTitle = JSONObject.quote(soundFontText(
        "tab_fretboard_title", currentLang, "Cần đàn", "Fretboard"
    ))
    val fretboardIdle = JSONObject.quote(soundFontText(
        "tab_fretboard_idle", currentLang,
        "Nhấn Play để nốt chạy theo TAB", "Press Play to follow the notes"
    ))
    val fretboardActive = JSONObject.quote(soundFontText(
        "tab_fretboard_active", currentLang, "Nốt đang phát", "Playing notes"
    ))
    val fretboardPreview = JSONObject.quote(soundFontText(
        "tab_fretboard_preview", currentLang, "Xem trước nốt đầu", "First-note preview"
    ))
    val fretboardUnsupported = JSONObject.quote(soundFontText(
        "tab_fretboard_unsupported", currentLang,
        "Track này không có vị trí TAB trên dây đàn.",
        "This track does not contain fretted-string TAB notes."
    ))
    val fretboardRest = JSONObject.quote(soundFontText(
        "tab_fretboard_rest", currentLang, "Nốt nghỉ", "Rest"
    ))
    val fretboardClose = JSONObject.quote(soundFontText(
        "tab_fretboard_close", currentLang, "Đóng cần đàn", "Close fretboard"
    ))
    val fretboardString = JSONObject.quote(soundFontText(
        "tab_fretboard_string", currentLang, "Dây", "String"
    ))
    val fretboardFret = JSONObject.quote(soundFontText(
        "tab_fretboard_fret", currentLang, "Ngăn", "Fret"
    ))
    val fretboardChord = JSONObject.quote(soundFontText(
        "tab_fretboard_chord", currentLang, "Hợp âm", "Chord"
    ))
    val fretboardNote = JSONObject.quote(soundFontText(
        "tab_fretboard_note", currentLang, "Nốt", "Note"
    ))
    val fretboardNotes = JSONObject.quote(soundFontText(
        "tab_fretboard_notes", currentLang, "nốt", "notes"
    ))

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

                /*
                 * Chế độ S+T+F dùng bố cục chia đôi ổn định:
                 *   khuông/TAB ở trên -> cần đàn ngay bên dưới -> nút Play ở đáy.
                 * Bảng cần đàn không đè lên bản nhạc và không thay đổi vị trí theo nốt.
                 */
                #tt-fretboard-panel {
                    display: none;
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: auto;
                    bottom: auto;
                    z-index: 2147483000;
                    width: 100%;
                    height: var(--tt-fretboard-height, 260px);
                    box-sizing: border-box;
                    flex-direction: column;
                    padding: 0 8px;
                    overflow: hidden;
                    background: var(--tt-panel-bg, #f4f6fb);
                    color: var(--tt-panel-text, #101828);
                    border-top: 2px solid var(--tt-cursor-color, #8b5cf6);
                    box-shadow: 0 -3px 16px rgba(16, 24, 40, .18);
                    font-family: Arial, sans-serif;
                    opacity: 0;
                    visibility: hidden;
                    pointer-events: none;
                    transform: translateZ(0);
                    -webkit-transform: translateZ(0);
                    isolation: isolate;
                }
                body.tt-fretboard-open #tt-fretboard-panel {
                    display: flex !important;
                    opacity: 1 !important;
                    visibility: visible !important;
                    pointer-events: auto;
                }
                body.tt-fretboard-open #scroll-wrapper {
                    border-bottom: 1px solid rgba(139, 92, 246, .34);
                }
                body.tt-fretboard-open #alphatab-container {
                    min-height: 100% !important;
                    /* Đủ khoảng trống để hệ khuông cuối vẫn cuộn lên trọn vẹn,
                       không bị mép trên của cần đàn che mất. */
                    padding-bottom: var(--tt-score-bottom-reserve, 300px) !important;
                }
                .tt-fretboard-header {
                    height: 34px;
                    min-height: 34px;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 6px;
                    padding: 2px 1px 0;
                }
                .tt-fretboard-header-main {
                    min-width: 0;
                    flex: 1;
                    display: flex;
                    align-items: center;
                    overflow: hidden;
                }
                .tt-fretboard-header-actions {
                    flex-shrink: 0;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }
                #tt-fretboard-title {
                    flex-shrink: 0;
                    font-size: 13px;
                    line-height: 16px;
                    font-weight: 800;
                    color: #101828;
                }
                #tt-fretboard-track {
                    margin-left: 6px;
                    font-size: 9.5px;
                    line-height: 13px;
                    font-weight: 600;
                    color: #667085;
                    max-width: 45vw;
                    display: inline-block;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    vertical-align: middle;
                }
                /* Tên hợp âm được vẽ trực tiếp trong SVG, ngay phía trên
                   cụm vị trí bấm. Badge ở header chỉ được giữ lại để tương thích DOM. */
                #tt-fretboard-chord,
                #tt-fretboard-chord.is-visible {
                    display: none !important;
                }
                #tt-fretboard-close {
                    width: 27px;
                    min-width: 27px;
                    height: 27px;
                    min-height: 27px;
                    padding: 0;
                    border: 1px solid #cbd5e1;
                    background: #ffffff;
                    color: #344054;
                    border-radius: 50%;
                    font-size: 15px;
                    line-height: 25px;
                    font-weight: 700;
                }
                #tt-fretboard-stage {
                    flex: 1;
                    min-height: 112px;
                    width: 100%;
                    border-radius: 11px;
                    overflow: hidden;
                    background: var(--tt-fretboard-color, #f8fafc);
                    box-shadow: inset 0 0 0 1px var(--tt-board-border, #cbd5e1), 0 2px 10px rgba(16,24,40,.12);
                }
                #tt-fretboard-svg {
                    display: block;
                    width: 100%;
                    height: 100%;
                }
                #tt-fretboard-status {
                    flex-shrink: 0;
                    height: 23px;
                    line-height: 23px;
                    text-align: center;
                    font-size: 10.5px;
                    font-weight: 800;
                    color: #344054;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                /* Vùng này dành đúng chỗ cho thanh Play/Pause Compose cố định ở đáy. */
                #tt-fretboard-controls-space {
                    flex-shrink: 0;
                    height: var(--tt-fretboard-controls-height, 66px);
                }
                body.tt-tab-dark #tt-fretboard-panel {
                    background: var(--tt-panel-bg, #0f131c);
                    color: var(--tt-panel-text, #f9fafb);
                    border-color: var(--tt-cursor-color, #8b5cf6);
                    box-shadow: 0 -3px 16px rgba(0, 0, 0, .58);
                }
                body.tt-tab-dark #tt-fretboard-stage {
                    background: var(--tt-fretboard-color, #171b24);
                    box-shadow: inset 0 0 0 1px var(--tt-board-border, #344054), 0 2px 10px rgba(0,0,0,.38);
                }
                body.tt-tab-dark #tt-fretboard-title {
                    color: #ffffff;
                }
                body.tt-tab-dark #tt-fretboard-track {
                    color: #98a2b3;
                }
                body.tt-tab-dark #tt-fretboard-status {
                    color: #f2f4f7;
                }
                body.tt-tab-dark #tt-fretboard-close {
                    background: #252b37;
                    color: #f9fafb;
                    border-color: #475467;
                }
                body.pdf-export-mode #tt-fretboard-panel { display: none !important; }
                body.tt-fretboard-side #tt-fretboard-panel {
                    border-top: none;
                    border-left: 2px solid #8b5cf6;
                    border-radius: 14px 0 0 14px;
                }
                body.tt-fretboard-side #tt-fretboard-track { max-width: 12vw; }
                body.tt-fretboard-side #tt-fretboard-stage { min-height: 160px; }

                /* Ban TAB ngan van cho phep cuon xuong sau bai de xem banner.
                   Khong ap dung khi xuat PDF de tranh trang giay trang. */
                body:not(.pdf-export-mode) #alphatab-container {
                    min-height: calc(100vh + 150px);
                }

                /* Không tạo một GPU layer khổng lồ cho toàn bộ bài ở chế độ tối. */
                body.tt-tab-dark:not(.pdf-export-mode) #alphatab-container svg:not(.tunertools-note-name-overlay) {
                    /* Màu khuông/TAB được cấu hình bằng RenderingResources của alphaTab. */
                    filter: none !important;
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
            <section id="tt-fretboard-panel" aria-live="off">
                <div class="tt-fretboard-header">
                    <div class="tt-fretboard-header-main">
                        <span id="tt-fretboard-title"></span><span id="tt-fretboard-track"></span>
                    </div>
                    <div class="tt-fretboard-header-actions">
                        <span id="tt-fretboard-chord" aria-live="polite"></span>
                        <button type="button" id="tt-fretboard-close" onclick="closeFretboardPanel()"></button>
                    </div>
                </div>
                <div id="tt-fretboard-stage">
                    <svg id="tt-fretboard-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 390 158" role="img"></svg>
                </div>
                <div id="tt-fretboard-status"></div>
                <div id="tt-fretboard-controls-space" aria-hidden="true"></div>
            </section>
            <script>
                var currentThemeIsDark = $isDarkInitial;
                var currentVisualPreferences = {
                    cursorColor: $quotedInitialCursorColor,
                    activeRegionColor: $quotedInitialActiveRegionColor,
                    tabColor: $quotedInitialTabColor,
                    noteColor: $quotedInitialNoteColor,
                    fretboardColor: $quotedInitialFretboardColor
                };
                var visualResourceRenderTimer = null;
                var isPdfExportMode = false;
                var pdfExportWaitingForRender = false;
                var pdfExportOriginalState = null;
                var pdfExportReadyTimer = null;
                var pdfExportFallbackTimer = null;
                var alphaTabInitRetryTimer = null;
                var lastAndroidViewportWidth = 0;
                var lastAndroidViewportHeight = 0;
                var lastSyncedCssViewportWidth = 0;
                var fretboardEnabled = false;
                var fretboardLatestBeat = null;
                // Giữ cửa sổ ngăn đàn ổn định. Chỉ chuyển theo nhóm 4 ngăn khi
                // bài thực sự đi ra ngoài phạm vi đang hiển thị.
                var fretboardWindowStart = 1;
                var fretboardLabels = {
                    title: $fretboardTitle,
                    idle: $fretboardIdle,
                    active: $fretboardActive,
                    preview: $fretboardPreview,
                    unsupported: $fretboardUnsupported,
                    rest: $fretboardRest,
                    close: $fretboardClose,
                    string: $fretboardString,
                    fret: $fretboardFret,
                    chord: $fretboardChord,
                    note: $fretboardNote,
                    notes: $fretboardNotes
                };

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

                    var sideBySide = fretboardEnabled && cssWidth >= cssHeight * 1.28;
                    var boardWidth = sideBySide
                        ? Math.min(Math.round(cssWidth * .47), Math.max(230, cssWidth - 300))
                        : cssWidth;
                    var boardHeight = 0;
                    var controlsHeight = cssHeight < 560 ? 60 : 66;

                    if (fretboardEnabled && !sideBySide) {
                        // Portrait: gần một nửa chiều cao cho cần đàn. Vùng score vẫn đủ
                        // một hệ khuông/TAB, nhưng con trỏ nằm sát khu vực cần đàn.
                        var minimumScoreHeight = Math.min(
                            285,
                            Math.max(195, Math.round(cssHeight * .47))
                        );
                        var maximumBoardHeight = Math.max(0, cssHeight - minimumScoreHeight);
                        var desiredBoardHeight = Math.min(
                            330,
                            Math.max(248, Math.round(cssHeight * .47))
                        );
                        boardHeight = Math.max(0, Math.min(desiredBoardHeight, maximumBoardHeight));
                    }

                    var panel = document.getElementById('tt-fretboard-panel');
                    body.style.setProperty('--tt-fretboard-height', boardHeight + 'px');
                    body.style.setProperty('--tt-fretboard-width', boardWidth + 'px');
                    body.style.setProperty(
                        '--tt-fretboard-controls-height',
                        controlsHeight + 'px'
                    );
                    body.classList.toggle('tt-fretboard-open', !!fretboardEnabled);
                    body.classList.toggle('tt-fretboard-side', !!sideBySide);

                    var scoreWidth = sideBySide ? cssWidth - boardWidth : cssWidth;
                    var scoreHeight = sideBySide ? cssHeight : cssHeight - boardHeight;
                    var scoreBottomReserve = fretboardEnabled
                        ? Math.min(440, Math.max(230, Math.round(scoreHeight * .52)))
                        : 0;
                    body.style.setProperty(
                        '--tt-score-bottom-reserve',
                        scoreBottomReserve + 'px'
                    );
                    wrapper.style.width = Math.max(1, scoreWidth) + 'px';
                    wrapper.style.height = Math.max(1, scoreHeight) + 'px';
                    wrapper.style.minHeight = wrapper.style.height;

                    // Không dùng position:fixed cho panel vì một số Android WebView
                    // đặt sai tọa độ sau khi Compose đổi chiều cao. Luôn gắn panel vào
                    // đúng cạnh của viewport thật.
                    if (panel) {
                        if (!fretboardEnabled) {
                            panel.style.display = 'none';
                            panel.style.visibility = 'hidden';
                            panel.style.opacity = '0';
                            panel.style.pointerEvents = 'none';
                        } else if (sideBySide) {
                            panel.style.display = 'flex';
                            panel.style.visibility = 'visible';
                            panel.style.opacity = '1';
                            panel.style.pointerEvents = 'auto';
                            panel.style.left = (cssWidth - boardWidth) + 'px';
                            panel.style.top = '0px';
                            panel.style.width = boardWidth + 'px';
                            panel.style.height = cssHeight + 'px';
                        } else {
                            panel.style.display = 'flex';
                            panel.style.visibility = 'visible';
                            panel.style.opacity = '1';
                            panel.style.pointerEvents = 'auto';
                            panel.style.left = '0px';
                            panel.style.top = Math.max(0, scoreHeight) + 'px';
                            panel.style.width = cssWidth + 'px';
                            panel.style.height = boardHeight + 'px';
                        }
                    }

                    return {
                        width: wrapper.clientWidth || scoreWidth,
                        height: wrapper.clientHeight || scoreHeight
                    };
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
                    if (isApiReady) {
                        requestAnimationFrame(updateTabBannerAtScroll);
                        if (fretboardEnabled) requestAnimationFrame(scheduleFretboardRefresh);
                    }
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
                        isScoreLoading || isPdfExportMode || fretboardEnabled) {
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

                function normalizeVisualHex(value, fallback) {
                    var text = String(value || '').trim();
                    if (/^#[0-9a-f]{6}$/i.test(text)) return text.toUpperCase();
                    if (/^#[0-9a-f]{3}$/i.test(text)) {
                        return ('#' + text.charAt(1) + text.charAt(1) +
                            text.charAt(2) + text.charAt(2) +
                            text.charAt(3) + text.charAt(3)).toUpperCase();
                    }
                    return fallback;
                }

                function visualRgb(hex) {
                    var normalized = normalizeVisualHex(hex, '#000000');
                    return {
                        r: parseInt(normalized.substring(1, 3), 16),
                        g: parseInt(normalized.substring(3, 5), 16),
                        b: parseInt(normalized.substring(5, 7), 16)
                    };
                }

                function visualRgba(hex, alpha) {
                    var rgb = visualRgb(hex);
                    return 'rgba(' + rgb.r + ',' + rgb.g + ',' + rgb.b + ',' +
                        Math.max(0, Math.min(1, Number(alpha))) + ')';
                }

                function visualLuminance(hex) {
                    var rgb = visualRgb(hex);
                    function channel(value) {
                        value = value / 255;
                        return value <= 0.03928
                            ? value / 12.92
                            : Math.pow((value + 0.055) / 1.055, 2.4);
                    }
                    return 0.2126 * channel(rgb.r) +
                        0.7152 * channel(rgb.g) +
                        0.0722 * channel(rgb.b);
                }

                function visualContrast(firstHex, secondHex) {
                    var first = visualLuminance(firstHex);
                    var second = visualLuminance(secondHex);
                    var lighter = Math.max(first, second);
                    var darker = Math.min(first, second);
                    return (lighter + 0.05) / (darker + 0.05);
                }

                function visualMix(firstHex, secondHex, ratio) {
                    var first = visualRgb(firstHex);
                    var second = visualRgb(secondHex);
                    var amount = Math.max(0, Math.min(1, Number(ratio)));
                    function toHex(value) {
                        var text = Math.round(value).toString(16).toUpperCase();
                        return text.length < 2 ? '0' + text : text;
                    }
                    return '#' +
                        toHex(first.r + (second.r - first.r) * amount) +
                        toHex(first.g + (second.g - first.g) * amount) +
                        toHex(first.b + (second.b - first.b) * amount);
                }

                function visualTextColor(backgroundHex) {
                    return visualLuminance(backgroundHex) > 0.48 ? '#111827' : '#FFFFFF';
                }

                function scheduleVisualResourceRender() {
                    clearTimeout(visualResourceRenderTimer);
                    visualResourceRenderTimer = setTimeout(function() {
                        if (!api || !api.score) return;
                        var wrapper = document.getElementById('scroll-wrapper');
                        var oldScrollTop = wrapper ? wrapper.scrollTop : 0;
                        var restoreManualPosition = lastKnownPlayerState !== 1;
                        try {
                            api.render();
                            if (restoreManualPosition && wrapper) {
                                setTimeout(function() {
                                    wrapper.scrollTop = oldScrollTop;
                                }, 90);
                            }
                        } catch (_) {}
                    }, 45);
                }

                function applyAlphaTabVisualResources(shouldRender) {
                    if (!api || !api.settings || !api.settings.display) return false;

                    var glyphColor = isPdfExportMode
                        ? '#111111'
                        : normalizeVisualHex(currentVisualPreferences.tabColor, '#111827');
                    var secondaryGlyph = visualRgba(
                        glyphColor,
                        isPdfExportMode ? 0.58 : (currentThemeIsDark ? 0.62 : 0.56)
                    );
                    var staffLine = visualRgba(
                        glyphColor,
                        isPdfExportMode ? 0.48 : (currentThemeIsDark ? 0.46 : 0.40)
                    );

                    try {
                        if (typeof api.settings.fillFromJson === 'function') {
                            api.settings.fillFromJson({
                                display: {
                                    resources: {
                                        mainGlyphColor: glyphColor,
                                        secondaryGlyphColor: secondaryGlyph,
                                        staffLineColor: staffLine,
                                        barSeparatorColor: glyphColor,
                                        barNumberColor: glyphColor,
                                        scoreInfoColor: glyphColor
                                    }
                                }
                            });
                        } else {
                            return false;
                        }
                        api.updateSettings();
                        if (shouldRender) scheduleVisualResourceRender();
                        return true;
                    } catch (_) {
                        return false;
                    }
                }

                function applyVisualPreferences(
                    isDarkTheme,
                    cursorColor,
                    activeRegionColor,
                    tabColor,
                    noteColor,
                    fretboardColor
                ) {
                    currentThemeIsDark = !!isDarkTheme;
                    currentVisualPreferences.cursorColor = normalizeVisualHex(
                        cursorColor,
                        currentThemeIsDark ? '#C084FC' : '#7C3AED'
                    );
                    currentVisualPreferences.activeRegionColor = normalizeVisualHex(
                        activeRegionColor,
                        currentThemeIsDark ? '#7C3AED' : '#8B5CF6'
                    );
                    currentVisualPreferences.tabColor = normalizeVisualHex(
                        tabColor,
                        currentThemeIsDark ? '#F8FAFC' : '#111827'
                    );
                    currentVisualPreferences.noteColor = normalizeVisualHex(
                        noteColor,
                        currentThemeIsDark ? '#FFD43B' : '#FBBF24'
                    );
                    currentVisualPreferences.fretboardColor = normalizeVisualHex(
                        fretboardColor,
                        currentThemeIsDark ? '#171B24' : '#F8FAFC'
                    );

                    var container = document.getElementById('alphatab-container');
                    if (container) container.style.filter = 'none';
                    document.body.classList.toggle(
                        'tt-tab-dark',
                        currentThemeIsDark && !isPdfExportMode
                    );

                    var rootStyle = document.documentElement.style;
                    var panelBackground = currentThemeIsDark ? '#0F131C' : '#F4F6FB';
                    var panelText = currentThemeIsDark ? '#F9FAFB' : '#101828';
                    var boardColor = currentVisualPreferences.fretboardColor;
                    var boardIsLight = visualLuminance(boardColor) > 0.48;
                    var boardBorder = visualMix(
                        boardColor,
                        boardIsLight ? '#475569' : '#CBD5E1',
                        boardIsLight ? 0.50 : 0.42
                    );
                    rootStyle.setProperty('--tt-panel-bg', panelBackground);
                    rootStyle.setProperty('--tt-panel-text', panelText);
                    rootStyle.setProperty('--tt-cursor-color', currentVisualPreferences.cursorColor);
                    rootStyle.setProperty('--tt-fretboard-color', boardColor);
                    rootStyle.setProperty('--tt-board-border', boardBorder);

                    var effectiveTabColor = isPdfExportMode
                        ? '#111111'
                        : currentVisualPreferences.tabColor;
                    var fallbackTitleElement = document.getElementById(
                        'tunertools-score-title-fallback'
                    );
                    if (fallbackTitleElement) {
                        fallbackTitleElement.style.color = effectiveTabColor;
                    }

                    var cursorAlpha = currentThemeIsDark ? 0.20 : 0.13;
                    var regionAlpha = currentThemeIsDark ? 0.25 : 0.16;
                    var style = document.getElementById('dynamic-theme-style');
                    if (style) {
                        style.innerHTML =
                            '.at-cursor-bar {' +
                                'background:' + visualRgba(
                                    currentVisualPreferences.activeRegionColor,
                                    regionAlpha
                                ) + ' !important;' +
                                'border-radius:8px !important;' +
                                'box-shadow:inset 0 0 0 1px ' + visualRgba(
                                    currentVisualPreferences.activeRegionColor,
                                    currentThemeIsDark ? 0.20 : 0.12
                                ) + ' !important;' +
                            '}' +
                            '.at-cursor-beat {' +
                                'background:' + visualRgba(
                                    currentVisualPreferences.cursorColor,
                                    cursorAlpha
                                ) + ' !important;' +
                                'border-left:4px solid ' +
                                    currentVisualPreferences.cursorColor + ' !important;' +
                                'box-shadow:-2px 0 12px ' + visualRgba(
                                    currentVisualPreferences.cursorColor,
                                    currentThemeIsDark ? 0.58 : 0.32
                                ) + ' !important;' +
                            '}' +
                            'body:not(.pdf-export-mode) .tunertools-note-name {' +
                                'fill:' + currentVisualPreferences.tabColor + ' !important;' +
                                'stroke:' + (currentThemeIsDark ? '#0B0F1A' : '#FFFFFF') +
                                    ' !important;' +
                            '}' +
                            'body:not(.pdf-export-mode) .tunertools-note-name.is-active {' +
                                'fill:' + currentVisualPreferences.cursorColor + ' !important;' +
                            '}';
                    }

                    if (fretboardEnabled) scheduleFretboardRefresh();
                    applyAlphaTabVisualResources(true);
                    return true;
                }

                // Giữ API cũ để các luồng xuất PDF/khôi phục giao diện tiếp tục hoạt động.
                function toggleTheme(isDarkTheme) {
                    return applyVisualPreferences(
                        isDarkTheme,
                        currentVisualPreferences.cursorColor,
                        currentVisualPreferences.activeRegionColor,
                        currentVisualPreferences.tabColor,
                        currentVisualPreferences.noteColor,
                        currentVisualPreferences.fretboardColor
                    );
                }

                // Áp dụng đúng lựa chọn đã lưu ngay từ frame đầu tiên của WebView.
                applyVisualPreferences(
                    $isDarkInitial,
                    $quotedInitialCursorColor,
                    $quotedInitialActiveRegionColor,
                    $quotedInitialTabColor,
                    $quotedInitialNoteColor,
                    $quotedInitialFretboardColor
                );
                syncWebViewViewportBox(0, 0);

                var api = null;
                // Âm click được tạo bởi alphaTab, dùng clock của chính bài đang phát.
                // 0: tắt, 0.75: bật. Không bật count-in trước bài.
                var metronomeEnabled = $initialMetronomeEnabled;
                function setMetronomeEnabled(enabled) {
                    metronomeEnabled = !!enabled;
                    if (api) {
                        api.metronomeVolume = metronomeEnabled ? 0.75 : 0;
                        api.countInVolume = 0;
                    }
                    return true;
                }
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
                // Tên lấy từ danh sách Android được ưu tiên nếu metadata Guitar Pro
                // trống, bị lỗi bảng mã hoặc bị mất dấu so với tên sạch trong danh sách.
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

                // Tránh hiển thị lỗi giải mã ký tự như "V?ng Tr?ng Kh�c".
                // Dấu ? NẰM GIỮA chữ và ký tự thay thế U+FFFD là dấu hiệu hỏng dữ liệu,
                // khác với dấu hỏi thông thường ở cuối một câu/tên bài hát.
                function isUsableScoreTitle(raw) {
                    var title = String(raw || '').replace(/\s+/g, ' ').trim();
                    if (!title || title.indexOf('\uFFFD') >= 0) return false;
                    if (/[\u0000-\u0008\u000B\u000C\u000E-\u001F]/.test(title)) return false;
                    if (/[^\s?]\?[^\s?]/.test(title)) return false;
                    if (/(?:Ã[\u0080-\u00BF]|Â[\u0080-\u00BF]|á[º»])/.test(title)) return false;
                    if (/^\?+$/.test(title) || /^(?:không tên|bài nhạc|bản nhạc|untitled|n\/a)$/i.test(title)) return false;
                    return true;
                }

                function unaccentedTitleKey(raw) {
                    var title = String(raw || '');
                    if (typeof title.normalize === 'function') {
                        title = title.normalize('NFD').replace(/[\u0300-\u036f]/g, '');
                    }
                    return title.replace(/đ/g, 'd').replace(/Đ/g, 'D')
                        .toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
                }

                function chooseReadableScoreTitle(titleFromFile, fallbackTitle) {
                    var fileIsClean = isUsableScoreTitle(titleFromFile);
                    var fallbackIsClean = isUsableScoreTitle(fallbackTitle);
                    if (!fileIsClean) return fallbackIsClean ? fallbackTitle : 'Bản nhạc';
                    if (!fallbackIsClean) return titleFromFile;

                    // Ví dụ file chứa "Bien Nho" nhưng tên API là "Biển Nhớ":
                    // hai tên tương đương, giữ bản có dấu thay vì bản ASCII.
                    if (/^[\x00-\x7F]*$/.test(titleFromFile) &&
                        /[^\x00-\x7F]/.test(fallbackTitle) &&
                        unaccentedTitleKey(titleFromFile) === unaccentedTitleKey(fallbackTitle)) {
                        return fallbackTitle;
                    }
                    return titleFromFile;
                }

                function applyResponsiveSongHeader(score, renderAfterUpdate, forcedFontSize) {
                    if (!api || !api.settings || !score) return false;

                    var titleFromFile = String(score.title || '').replace(/\s+/g, ' ').trim();
                    var fallbackTitle = String(pendingScoreTitle || '').replace(/\s+/g, ' ').trim();
                    currentScoreTitle = chooseReadableScoreTitle(titleFromFile, fallbackTitle);

                    var scoreTitleChanged = false;

                    // Thay cả metadata hỏng (không chỉ trường hợp trống) trước khi
                    // alphaTab render để tiêu đề hiện đúng trong khuông và file PDF.
                    if (titleFromFile !== currentScoreTitle) {
                        try {
                            score.title = currentScoreTitle;
                            scoreTitleChanged = true;
                        } catch (_) {
                            // Nếu model không cho sửa, DOM fallback sẽ dùng currentScoreTitle.
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
                    element.style.color = isPdfExportMode
                        ? '#202124'
                        : normalizeVisualHex(
                            currentVisualPreferences.tabColor,
                            currentThemeIsDark ? '#F8FAFC' : '#111827'
                        );
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

                // Bản đồ cần đàn đọc số dây/ngăn và cao độ từ chính alphaTab Beat/Note.
                // Note.string = 1 là dây dưới cùng (E trầm của guitar 6 dây),
                // trong khi Staff.tuning[0] là dây trên cùng của TAB.
                function fretboardStaffForTrack() {
                    var track = selectedTrackForNoteNames();
                    if (!track || !track.staves) return null;
                    for (var i = 0; i < track.staves.length; i++) {
                        var staff = track.staves[i];
                        if (staff && staff.isStringed !== false && staff.tuning &&
                            staff.tuning.length >= 4 && staff.tuning.length <= 12) {
                            return staff;
                        }
                    }
                    return null;
                }

                function firstFrettedBeatForPreview() {
                    var staff = fretboardStaffForTrack();
                    if (!staff || !staff.bars) return null;
                    for (var b = 0; b < staff.bars.length; b++) {
                        var bar = staff.bars[b];
                        if (!bar || !bar.voices) continue;
                        for (var v = 0; v < bar.voices.length; v++) {
                            var voice = bar.voices[v];
                            if (!voice || !voice.beats) continue;
                            for (var k = 0; k < voice.beats.length; k++) {
                                var beat = voice.beats[k];
                                if (!beat || !beat.notes) continue;
                                for (var n = 0; n < beat.notes.length; n++) {
                                    if (Number(beat.notes[n].string) >= 1 &&
                                        Number(beat.notes[n].fret) >= 0) return beat;
                                }
                            }
                        }
                    }
                    return null;
                }

                function fretboardNoteLabel(pitch, usesFlats) {
                    var sharp = ['C', 'C♯', 'D', 'D♯', 'E', 'F', 'F♯', 'G', 'G♯', 'A', 'A♯', 'B'];
                    var flat = ['C', 'D♭', 'D', 'E♭', 'E', 'F', 'G♭', 'G', 'A♭', 'A', 'B♭', 'B'];
                    if (!Number.isFinite(pitch)) return '•';
                    return (usesFlats ? flat : sharp)[((Math.round(pitch) % 12) + 12) % 12];
                }

                function fretboardPitchClass(pitch) {
                    if (!Number.isFinite(pitch)) return null;
                    return ((Math.round(pitch) % 12) + 12) % 12;
                }

                function fretboardIsArpeggioBeat(beat) {
                    if (!beat) return false;
                    var raw = beat.brushType;
                    var numeric = Number(raw);
                    if (numeric === 3 || numeric === 4) return true;
                    var label = String(raw == null ? '' : raw).toLowerCase();
                    return label.indexOf('arpeggio') >= 0;
                }

                function fretboardExplicitChordName(beat) {
                    if (!beat) return '';
                    try {
                        if (beat.chord && beat.chord.name) {
                            return String(beat.chord.name).trim();
                        }
                    } catch (ignored) {}
                    var chordId = beat.chordId == null ? '' : String(beat.chordId).trim();
                    // chordId đôi khi là ID nội bộ. Chỉ dùng khi trông giống tên hợp âm.
                    return /^[A-Ga-g][#b♯♭]?/.test(chordId) ? chordId : '';
                }

                function fretboardChordRootPitchClass(chordName) {
                    var match = /^\s*([A-Ga-g])([#b♯♭]?)/.exec(String(chordName || ''));
                    if (!match) return null;
                    var natural = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
                    var pitchClass = natural[match[1].toUpperCase()];
                    if (match[2] === '#' || match[2] === '♯') pitchClass += 1;
                    if (match[2] === 'b' || match[2] === '♭') pitchClass -= 1;
                    return (pitchClass + 12) % 12;
                }

                function fretboardSamePitchClassSet(actual, expected) {
                    if (actual.length !== expected.length) return false;
                    for (var i = 0; i < actual.length; i++) {
                        if (actual[i] !== expected[i]) return false;
                    }
                    return true;
                }

                function fretboardInferChord(highlighted, usesFlats) {
                    if (!highlighted || highlighted.length < 3) return null;

                    var pitchClassMap = {};
                    var pitchClasses = [];
                    var bass = null;
                    for (var i = 0; i < highlighted.length; i++) {
                        var point = highlighted[i];
                        var pitchClass = point.pitchClass;
                        if (pitchClass == null) continue;
                        if (!Object.prototype.hasOwnProperty.call(pitchClassMap, pitchClass)) {
                            pitchClassMap[pitchClass] = true;
                            pitchClasses.push(pitchClass);
                        }
                        if (!bass || point.pitch < bass.pitch) bass = point;
                    }
                    pitchClasses.sort(function(a, b) { return a - b; });
                    if (pitchClasses.length < 3) return null;

                    // Mẫu chính xác để tránh đoán hợp âm từ hai nốt hoặc cụm nốt mơ hồ.
                    var patterns = [
                        { intervals: [0, 4, 7, 11], suffix: 'maj7' },
                        { intervals: [0, 4, 7, 10], suffix: '7' },
                        { intervals: [0, 3, 7, 10], suffix: 'm7' },
                        { intervals: [0, 3, 7, 11], suffix: 'mMaj7' },
                        { intervals: [0, 3, 6, 10], suffix: 'm7♭5' },
                        { intervals: [0, 3, 6, 9], suffix: 'dim7' },
                        { intervals: [0, 4, 7, 9], suffix: '6' },
                        { intervals: [0, 3, 7, 9], suffix: 'm6' },
                        { intervals: [0, 2, 4, 7], suffix: 'add9' },
                        { intervals: [0, 4, 7], suffix: '' },
                        { intervals: [0, 3, 7], suffix: 'm' },
                        { intervals: [0, 3, 6], suffix: 'dim' },
                        { intervals: [0, 4, 8], suffix: 'aug' },
                        { intervals: [0, 2, 7], suffix: 'sus2' },
                        { intervals: [0, 5, 7], suffix: 'sus4' }
                    ];

                    var bassPitchClass = bass ? bass.pitchClass : null;
                    var roots = [];
                    if (bassPitchClass != null) roots.push(bassPitchClass);
                    for (var rootCandidate = 0; rootCandidate < 12; rootCandidate++) {
                        if (rootCandidate !== bassPitchClass) roots.push(rootCandidate);
                    }

                    for (var r = 0; r < roots.length; r++) {
                        var root = roots[r];
                        var intervals = pitchClasses.map(function(value) {
                            return (value - root + 12) % 12;
                        }).sort(function(a, b) { return a - b; });

                        for (var p = 0; p < patterns.length; p++) {
                            if (!fretboardSamePitchClassSet(intervals, patterns[p].intervals)) continue;
                            var rootName = fretboardNoteLabel(root, usesFlats);
                            var chordName = rootName + patterns[p].suffix;
                            if (bassPitchClass != null && bassPitchClass !== root) {
                                chordName += '/' + fretboardNoteLabel(bassPitchClass, usesFlats);
                            }
                            return {
                                name: chordName,
                                rootPitchClass: root,
                                inferred: true
                            };
                        }
                    }
                    return null;
                }

                function fretboardChordForBeat(beat, highlighted, usesFlats, isPreview) {
                    // Nốt tỉa/sequential chỉ có một nốt tại beat hiện tại: không hiện hợp âm.
                    // Arpeggio được loại trừ ngay cả khi file lưu nhiều nốt trong cùng beat.
                    if (isPreview || !beat || !highlighted || highlighted.length < 2 ||
                        fretboardIsArpeggioBeat(beat)) return null;

                    var explicitName = fretboardExplicitChordName(beat);
                    if (explicitName) {
                        return {
                            name: explicitName,
                            rootPitchClass: fretboardChordRootPitchClass(explicitName),
                            inferred: false
                        };
                    }
                    return fretboardInferChord(highlighted, usesFlats);
                }

                function fretboardSvgElement(parent, tag, attrs, label) {
                    var element = document.createElementNS('http://www.w3.org/2000/svg', tag);
                    for (var key in attrs) {
                        if (Object.prototype.hasOwnProperty.call(attrs, key)) {
                            element.setAttribute(key, String(attrs[key]));
                        }
                    }
                    if (label !== undefined) element.textContent = String(label);
                    parent.appendChild(element);
                    return element;
                }


                function fretboardCollectionItems(collection) {
                    if (!collection) return [];
                    if (Array.isArray(collection)) return collection;
                    var result = [];
                    var length = Number(collection.length);
                    if (Number.isFinite(length)) {
                        for (var i = 0; i < length; i++) result.push(collection[i]);
                        return result;
                    }
                    var count = Number(collection.count);
                    if (Number.isFinite(count)) {
                        for (var j = 0; j < count; j++) {
                            result.push(typeof collection.get === 'function'
                                ? collection.get(j)
                                : collection[j]);
                        }
                    }
                    return result;
                }

                function fretboardEnumNumber(value) {
                    var numeric = Number(value);
                    return Number.isFinite(numeric) ? Math.round(numeric) : 0;
                }

                function fretboardEnumText(value) {
                    return String(value == null ? '' : value).toLowerCase();
                }

                function fretboardTechniqueNoteRef(note) {
                    if (!note) return null;
                    var fret = Math.round(Number(note.fret));
                    var stringNumber = Math.round(Number(note.string));
                    if (!Number.isFinite(fret) || !Number.isFinite(stringNumber) ||
                        fret < 0 || stringNumber < 1) return null;
                    return { fret: fret, stringNumber: stringNumber };
                }

                function fretboardBendAmountLabel(points) {
                    var list = fretboardCollectionItems(points);
                    var maximum = 0;
                    for (var i = 0; i < list.length; i++) {
                        var value = Math.abs(Number(list[i] && list[i].value));
                        if (Number.isFinite(value)) maximum = Math.max(maximum, value);
                    }
                    var quarterSteps = Math.round(maximum);
                    if (quarterSteps <= 0) return '';
                    if (quarterSteps === 1) return '1/4';
                    if (quarterSteps === 2) return '1/2';
                    if (quarterSteps === 3) return '3/4';
                    if (quarterSteps === 4) return 'Full';
                    if (quarterSteps === 5) return '1 1/4';
                    if (quarterSteps === 6) return '1 1/2';
                    if (quarterSteps === 7) return '1 3/4';
                    if (quarterSteps % 4 === 0) return String(quarterSteps / 4) + ' tones';
                    return String(quarterSteps) + '/4';
                }

                function fretboardBendTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.bendType);
                    var rawText = fretboardEnumText(note.bendType);
                    var points = fretboardCollectionItems(note.bendPoints);
                    if (type === 0 && points.length > 0) type = 1;
                    if (type === 0 && rawText.indexOf('bend') < 0 &&
                        rawText.indexOf('release') < 0 && rawText.indexOf('hold') < 0) {
                        return null;
                    }

                    if (type === 0) {
                        if (rawText.indexOf('prebendbend') >= 0) type = 7;
                        else if (rawText.indexOf('prebendrelease') >= 0) type = 8;
                        else if (rawText.indexOf('prebend') >= 0) type = 6;
                        else if (rawText.indexOf('bendrelease') >= 0) type = 4;
                        else if (rawText.indexOf('release') >= 0) type = 3;
                        else if (rawText.indexOf('hold') >= 0) type = 5;
                        else type = 2;
                    }

                    var label = 'Bend';
                    var shape = 'up';
                    if (type === 3) {
                        label = 'Release';
                        shape = 'down';
                    } else if (type === 4) {
                        label = 'Bend/Release';
                        shape = 'updown';
                    } else if (type === 5) {
                        label = 'Hold';
                        shape = 'hold';
                    } else if (type === 6) {
                        label = 'Pre-bend';
                        shape = 'preup';
                    } else if (type === 7) {
                        label = 'Pre-bend +';
                        shape = 'preup';
                    } else if (type === 8) {
                        label = 'Pre-bend/Release';
                        shape = 'updown';
                    }

                    var amount = fretboardBendAmountLabel(points);
                    if (amount && type !== 3 && type !== 5) label += ' ' + amount;
                    var bendStyle = fretboardEnumNumber(note.bendStyle);
                    var bendStyleText = fretboardEnumText(note.bendStyle);
                    if (bendStyle === 1 || bendStyleText.indexOf('gradual') >= 0) {
                        label += ' gradual';
                    } else if (bendStyle === 2 || bendStyleText.indexOf('fast') >= 0) {
                        label += ' fast';
                    }
                    return { label: label, shape: shape, type: type, style: bendStyle };
                }

                function fretboardSlideTechniques(note) {
                    if (!note) return [];
                    var result = [];
                    var inType = fretboardEnumNumber(note.slideInType);
                    var outType = fretboardEnumNumber(note.slideOutType);
                    var inText = fretboardEnumText(note.slideInType);
                    var outText = fretboardEnumText(note.slideOutType);
                    var origin = fretboardTechniqueNoteRef(note.slideOrigin);
                    var target = fretboardTechniqueNoteRef(note.slideTarget);

                    if (inType === 0) {
                        if (inText.indexOf('frombelow') >= 0) inType = 1;
                        else if (inText.indexOf('fromabove') >= 0) inType = 2;
                    }
                    if (outType === 0) {
                        if (outText.indexOf('pickslidedown') >= 0) outType = 5;
                        else if (outText.indexOf('pickslideup') >= 0) outType = 6;
                        else if (outText.indexOf('outup') >= 0) outType = 3;
                        else if (outText.indexOf('outdown') >= 0) outType = 4;
                        else if (outText.indexOf('shift') >= 0) outType = 1;
                        else if (outText.indexOf('legato') >= 0) outType = 2;
                    }

                    if (origin) {
                        result.push({ phase: 'in-target', label: 'Slide', origin: origin });
                    } else if (inType === 1 || inType === 2) {
                        result.push({
                            phase: 'in-free',
                            label: 'Slide',
                            direction: inType === 1 ? 'below' : 'above'
                        });
                    }

                    if (outType === 1 || outType === 2) {
                        result.push({ phase: 'out-target', label: 'Slide', target: target });
                    } else if (outType === 3 || outType === 4) {
                        result.push({
                            phase: 'out-free',
                            label: 'Slide',
                            direction: outType === 3 ? 'up' : 'down'
                        });
                    } else if (outType === 5 || outType === 6) {
                        result.push({
                            phase: 'out-free',
                            label: 'Pick slide',
                            direction: outType === 6 ? 'up' : 'down',
                            pick: true
                        });
                    }
                    return result;
                }


                function fretboardTechniqueRefFromValue(value) {
                    return fretboardTechniqueNoteRef(value);
                }

                function fretboardTechniqueHasValue(value) {
                    var numeric = Number(value);
                    if (Number.isFinite(numeric)) return Math.round(numeric) !== 0;
                    var text = fretboardEnumText(value);
                    return text !== '' && text !== 'none' && text !== '0' && text !== 'false';
                }

                function fretboardHarmonicTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.harmonicType);
                    var text = fretboardEnumText(note.harmonicType);
                    if (!note.isHarmonic && type === 0 && text.indexOf('harmonic') < 0) return null;
                    var labels = {
                        1: 'N.H.',
                        2: 'A.H.',
                        3: 'P.H.',
                        4: 'T.H.',
                        5: 'S.H.',
                        6: 'F.B.'
                    };
                    var label = labels[type] || 'Harm.';
                    if (text.indexOf('natural') >= 0) label = 'N.H.';
                    else if (text.indexOf('artificial') >= 0) label = 'A.H.';
                    else if (text.indexOf('pinch') >= 0) label = 'P.H.';
                    else if (text.indexOf('tap') >= 0) label = 'T.H.';
                    else if (text.indexOf('semi') >= 0) label = 'S.H.';
                    else if (text.indexOf('feedback') >= 0) label = 'F.B.';
                    return { label: label, type: type };
                }

                function fretboardVibratoTechnique(value) {
                    var type = fretboardEnumNumber(value);
                    var text = fretboardEnumText(value);
                    if (type === 0 && text.indexOf('vibrato') < 0 &&
                        text.indexOf('slight') < 0 && text.indexOf('wide') < 0) return null;
                    var wide = type === 2 || text.indexOf('wide') >= 0;
                    return { label: wide ? 'Wide vib.' : 'Vibrato', wide: wide };
                }

                function fretboardAccentTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.accentuated);
                    var text = fretboardEnumText(note.accentuated);
                    if (type === 0 && text.indexOf('normal') < 0 &&
                        text.indexOf('heavy') < 0 && text.indexOf('tenuto') < 0) return null;
                    if (type === 2 || text.indexOf('heavy') >= 0) {
                        return { label: 'Heavy accent', symbol: '>>' };
                    }
                    if (type === 3 || text.indexOf('tenuto') >= 0) {
                        return { label: 'Tenuto', symbol: '—' };
                    }
                    return { label: 'Accent', symbol: '>' };
                }

                function fretboardOrnamentTechnique(note) {
                    if (!note) return null;
                    var type = fretboardEnumNumber(note.ornament);
                    var text = fretboardEnumText(note.ornament);
                    if (type === 0 && text === '') return null;
                    if (type === 1 || text.indexOf('invertedturn') >= 0) return { label: 'Inv. turn', symbol: '↶' };
                    if (type === 2 || text === 'turn' || text.indexOf('turn') >= 0) return { label: 'Turn', symbol: '↷' };
                    if (type === 3 || text.indexOf('uppermordent') >= 0) return { label: 'Mordent +', symbol: 'M+' };
                    if (type === 4 || text.indexOf('lowermordent') >= 0) return { label: 'Mordent -', symbol: 'M-' };
                    return null;
                }

                function fretboardFingerLabel(value, rightHand) {
                    var number = Number(value);
                    if (!Number.isFinite(number) || number < 0 || number > 4) return '';
                    number = Math.round(number);
                    if (rightHand) {
                        return ['Rp', 'Ri', 'Rm', 'Ra', 'Rc'][number] || '';
                    }
                    return number === 0 ? 'LT' : 'L' + number;
                }

                function fretboardHammerPullTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(note.hammerPullOrigin);
                    var target = fretboardTechniqueRefFromValue(note.hammerPullDestination);
                    var current = fretboardTechniqueNoteRef(note);
                    if (!origin && !target && note.isHammerPullOrigin !== true) return null;
                    var destination = target || current;
                    var source = origin || current;
                    var label = 'H/P';
                    if (source && destination && Number.isFinite(source.fret) && Number.isFinite(destination.fret)) {
                        label = destination.fret >= source.fret ? 'H' : 'P';
                    }
                    return {
                        label: label,
                        origin: origin,
                        target: target,
                        phase: target || note.isHammerPullOrigin === true ? 'out' : 'in'
                    };
                }

                function fretboardSlurTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(
                        note.slurOrigin || note.effectSlurOrigin
                    );
                    var target = fretboardTechniqueRefFromValue(
                        note.slurDestination || note.effectSlurDestination
                    );
                    var starts = note.isSlurOrigin === true || note.isEffectSlurOrigin === true;
                    var ends = note.isSlurDestination === true ||
                        note.isEffectSlurDestination === true || note.hasEffectSlur === true;
                    if (!origin && !target && !starts && !ends) return null;
                    return {
                        label: 'Legato',
                        origin: origin,
                        target: target,
                        phase: target || starts ? 'out' : 'in'
                    };
                }

                function fretboardTieTechnique(note) {
                    if (!note) return null;
                    var origin = fretboardTechniqueRefFromValue(note.tieOrigin);
                    var target = fretboardTechniqueRefFromValue(note.tieDestination);
                    if (!origin && !target && note.isTieOrigin !== true && note.isTieDestination !== true) return null;
                    return {
                        label: 'Tie',
                        origin: origin,
                        target: target,
                        phase: target || note.isTieOrigin === true ? 'out' : 'in'
                    };
                }

                function fretboardTrillTechnique(note) {
                    if (!note || note.isTrill !== true) return null;
                    var targetFret = Math.round(Number(note.trillFret));
                    if (!Number.isFinite(targetFret) || targetFret < 0) {
                        var trillValue = Number(note.trillValue);
                        var currentValue = Number(note.realValue);
                        if (Number.isFinite(trillValue) && Number.isFinite(currentValue)) {
                            targetFret = Math.max(0, Math.round(Number(note.fret) + trillValue - currentValue));
                        }
                    }
                    return {
                        label: Number.isFinite(targetFret) ? 'tr ' + targetFret : 'tr',
                        targetFret: Number.isFinite(targetFret) ? targetFret : null
                    };
                }

                function fretboardThemeInk(darkInk, lightInk) {
                    return currentThemeIsDark ? darkInk : lightInk;
                }

                function fretboardNoteEffects(note) {
                    note = note || {};
                    var harmonic = fretboardHarmonicTechnique(note);
                    var vibrato = fretboardVibratoTechnique(note.vibrato);
                    var hammerPull = fretboardHammerPullTechnique(note);
                    var slur = hammerPull ? null : fretboardSlurTechnique(note);
                    var tie = fretboardTieTechnique(note);
                    var trill = fretboardTrillTechnique(note);
                    var accent = fretboardAccentTechnique(note);
                    var ornament = fretboardOrnamentTechnique(note);
                    var leftFinger = fretboardFingerLabel(note.leftHandFinger, false);
                    var rightFinger = fretboardFingerLabel(note.rightHandFinger, true);
                    var tags = [];
                    if (harmonic) tags.push({ label: harmonic.label, ink: fretboardThemeInk('#67e8f9', '#0e7490') });
                    if (note.isLeftHandTapped === true) tags.push({ label: 'T', ink: fretboardThemeInk('#f472b6', '#be185d') });
                    if (ornament) tags.push({ label: ornament.label, ink: fretboardThemeInk('#c084fc', '#7e22ce') });
                    if (leftFinger) tags.push({ label: leftFinger, ink: fretboardThemeInk('#a5b4fc', '#4338ca') });
                    if (rightFinger) tags.push({ label: rightFinger, ink: fretboardThemeInk('#a5b4fc', '#4338ca') });
                    if (note.isContinuedBend === true) tags.push({ label: 'Bend cont.', ink: fretboardThemeInk('#fb923c', '#d9480f') });
                    return {
                        dead: note.isDead === true,
                        ghost: note.isGhost === true,
                        staccato: note.isStaccato === true,
                        palmMute: note.isPalmMute === true,
                        letRing: note.isLetRing === true,
                        tapped: note.isLeftHandTapped === true,
                        harmonic: harmonic,
                        vibrato: vibrato,
                        hammerPull: hammerPull,
                        slur: slur,
                        tie: tie,
                        trill: trill,
                        accent: accent,
                        ornament: ornament,
                        tags: tags
                    };
                }

                function fretboardWhammyTechnique(beat) {
                    if (!beat) return null;
                    var type = fretboardEnumNumber(beat.whammyBarType);
                    var text = fretboardEnumText(beat.whammyBarType);
                    var points = fretboardCollectionItems(beat.whammyBarPoints);
                    var has = beat.hasWhammyBar === true || beat.isContinuedWhammy === true ||
                        type !== 0 || points.length > 0 || text.indexOf('whammy') >= 0 ||
                        text.indexOf('dive') >= 0 || text.indexOf('dip') >= 0;
                    if (!has) return null;
                    var labels = {
                        1: 'Whammy',
                        2: 'Dive',
                        3: 'Dip',
                        4: 'Whammy hold',
                        5: 'Pre-dive',
                        6: 'Pre-dive/Dive'
                    };
                    var label = labels[type] || 'Whammy';
                    if (beat.isContinuedWhammy === true && type === 0) label = 'Whammy cont.';
                    if (text.indexOf('predivedive') >= 0) label = 'Pre-dive/Dive';
                    else if (text.indexOf('predive') >= 0) label = 'Pre-dive';
                    else if (text.indexOf('dive') >= 0) label = 'Dive';
                    else if (text.indexOf('dip') >= 0) label = 'Dip';
                    else if (text.indexOf('hold') >= 0) label = 'Whammy hold';
                    var amount = fretboardBendAmountLabel(points);
                    if (amount && label.indexOf('hold') < 0) label += ' ' + amount;
                    return { label: label, type: type, points: points };
                }

                function fretboardTremoloTechnique(beat) {
                    if (!beat) return null;
                    var effect = beat.tremoloPicking || null;
                    var rawMarks = Number(effect && effect.marks);
                    var marks = Math.round(rawMarks);
                    var style = fretboardEnumNumber(effect && effect.style);
                    var legacy = fretboardEnumNumber(beat.tremoloSpeed);
                    var has = beat.isTremolo === true || effect != null || legacy !== 0;
                    if (!has) return null;
                    if (!Number.isFinite(rawMarks) || marks < 0) {
                        marks = legacy > 0 ? Math.min(5, legacy) : 3;
                    }
                    marks = Math.max(0, Math.min(5, marks));
                    var buzz = style === 1 || fretboardEnumText(effect && effect.style).indexOf('buzz') >= 0;
                    return {
                        label: buzz ? 'Buzz roll' : (marks > 0 ? 'TR x' + marks : 'TR'),
                        marks: marks,
                        buzz: buzz
                    };
                }

                function fretboardBrushTechnique(beat) {
                    if (!beat) return null;
                    var type = fretboardEnumNumber(beat.brushType);
                    var text = fretboardEnumText(beat.brushType);
                    if (type === 0 && text === '') return null;
                    if (type === 1 || text.indexOf('brushup') >= 0) return { label: 'Strum ↑', direction: 'up', arpeggio: false };
                    if (type === 2 || text.indexOf('brushdown') >= 0) return { label: 'Strum ↓', direction: 'down', arpeggio: false };
                    if (type === 3 || text.indexOf('arpeggioup') >= 0) return { label: 'Arp. ↑', direction: 'up', arpeggio: true };
                    if (type === 4 || text.indexOf('arpeggiodown') >= 0) return { label: 'Arp. ↓', direction: 'down', arpeggio: true };
                    return null;
                }

                function fretboardRasgueadoLabel(value) {
                    var type = fretboardEnumNumber(value);
                    var labels = [
                        '', 'Rasg. ii', 'Rasg. mi', 'Rasg. mii 3', 'Rasg. mii A',
                        'Rasg. pmp 3', 'Rasg. pmp A', 'Rasg. pei 3', 'Rasg. pei A',
                        'Rasg. pai 3', 'Rasg. pai A', 'Rasg. ami 3', 'Rasg. ami A',
                        'Rasg. ppp', 'Rasg. amii', 'Rasg. amip', 'Rasg. eami',
                        'Rasg. eamii', 'Rasg. peami'
                    ];
                    if (type > 0 && type < labels.length) return labels[type];
                    var text = fretboardEnumText(value);
                    return text && text !== 'none' && text !== '0' ? 'Rasgueado' : '';
                }

                function fretboardBeatTechniques(beat) {
                    var result = {
                        labels: [],
                        whammy: null,
                        tremolo: null,
                        brush: null,
                        barre: null
                    };
                    if (!beat) return result;
                    var seen = {};
                    function add(label, ink) {
                        if (!label || seen[label]) return;
                        seen[label] = true;
                        result.labels.push({ label: label, ink: ink || fretboardThemeInk('#cbd5e1', '#475569') });
                    }

                    result.whammy = fretboardWhammyTechnique(beat);
                    result.tremolo = fretboardTremoloTechnique(beat);
                    result.brush = fretboardBrushTechnique(beat);
                    if (result.whammy) add(result.whammy.label, fretboardThemeInk('#2dd4bf', '#0f766e'));
                    if (result.tremolo) add(result.tremolo.label, fretboardThemeInk('#f472b6', '#be185d'));
                    if (result.brush) add(result.brush.label, fretboardThemeInk('#a78bfa', '#6d28d9'));

                    var pickType = fretboardEnumNumber(beat.pickStroke);
                    var pickText = fretboardEnumText(beat.pickStroke);
                    if (pickType === 1 || pickText.indexOf('up') >= 0) add('Pick ↑', fretboardThemeInk('#60a5fa', '#1d4ed8'));
                    else if (pickType === 2 || pickText.indexOf('down') >= 0) add('Pick ↓', fretboardThemeInk('#60a5fa', '#1d4ed8'));

                    var beatVibrato = fretboardVibratoTechnique(beat.vibrato);
                    if (beatVibrato) add(beatVibrato.label, fretboardThemeInk('#22d3ee', '#0e7490'));
                    if (beat.isPalmMute === true) add('P.M.', fretboardThemeInk('#fbbf24', '#b45309'));
                    if (beat.isLetRing === true) add('Let ring', fretboardThemeInk('#86efac', '#047857'));

                    var graceType = fretboardEnumNumber(beat.graceType);
                    var graceText = fretboardEnumText(beat.graceType);
                    if (graceType === 1 || graceText.indexOf('onbeat') >= 0) add('Grace on beat', fretboardThemeInk('#c4b5fd', '#6d28d9'));
                    else if (graceType === 2 || graceText.indexOf('before') >= 0) add('Grace', fretboardThemeInk('#c4b5fd', '#6d28d9'));
                    else if (graceType === 3 || graceText.indexOf('bend') >= 0) add('Bend grace', fretboardThemeInk('#fb923c', '#d9480f'));

                    if (beat.slap === true) add('Slap', fretboardThemeInk('#38bdf8', '#0277b5'));
                    if (beat.pop === true) add('Pop', fretboardThemeInk('#38bdf8', '#0277b5'));
                    if (beat.tap === true) add('Tap', fretboardThemeInk('#f472b6', '#be185d'));
                    if (beat.deadSlapped === true) add('Dead slap', fretboardThemeInk('#f87171', '#b91c1c'));

                    var golpe = fretboardEnumNumber(beat.golpe);
                    var golpeText = fretboardEnumText(beat.golpe);
                    if (golpe === 1 || golpeText.indexOf('thumb') >= 0) add('Golpe T', fretboardThemeInk('#fb7185', '#be123c'));
                    else if (golpe === 2 || golpeText.indexOf('finger') >= 0) add('Golpe F', fretboardThemeInk('#fb7185', '#be123c'));

                    var wah = fretboardEnumNumber(beat.wahPedal);
                    var wahText = fretboardEnumText(beat.wahPedal);
                    if (wah === 1 || wahText.indexOf('open') >= 0) add('Wah open', fretboardThemeInk('#4ade80', '#15803d'));
                    else if (wah === 2 || wahText.indexOf('closed') >= 0) add('Wah closed', fretboardThemeInk('#4ade80', '#15803d'));

                    var fade = fretboardEnumNumber(beat.fade);
                    var fadeText = fretboardEnumText(beat.fade);
                    if (fade === 1 || fadeText.indexOf('fadein') >= 0) add('Fade in', fretboardThemeInk('#93c5fd', '#1d4ed8'));
                    else if (fade === 2 || fadeText.indexOf('fadeout') >= 0) add('Fade out', fretboardThemeInk('#93c5fd', '#1d4ed8'));
                    else if (fade === 3 || fadeText.indexOf('swell') >= 0) add('Swell', fretboardThemeInk('#93c5fd', '#1d4ed8'));

                    var crescendo = fretboardEnumNumber(beat.crescendo);
                    var crescendoText = fretboardEnumText(beat.crescendo);
                    if (crescendo === 1 || crescendoText.indexOf('crescendo') >= 0) add('Cresc.', fretboardThemeInk('#f0abfc', '#a21caf'));
                    else if (crescendo === 2 || crescendoText.indexOf('decrescendo') >= 0) add('Dim.', fretboardThemeInk('#f0abfc', '#a21caf'));

                    var rasgueado = fretboardRasgueadoLabel(beat.rasgueado);
                    if (rasgueado || beat.hasRasgueado === true) add(rasgueado || 'Rasgueado', fretboardThemeInk('#f9a8d4', '#be185d'));
                    if (beat.isEffectSlurOrigin === true || beat.isEffectSlurDestination === true ||
                        beat.isLegatoOrigin === true || beat.isLegatoDestination === true) {
                        add('Legato', fretboardThemeInk('#34d399', '#047857'));
                    }
                    if (beat.fermata) add('Fermata', fretboardThemeInk('#fde68a', '#b45309'));

                    if (beat.isBarre === true || Number(beat.barreFret) > 0 ||
                        fretboardTechniqueHasValue(beat.barreShape)) {
                        var barreFret = Math.max(0, Math.round(Number(beat.barreFret) || 0));
                        var barreShape = fretboardEnumNumber(beat.barreShape);
                        var half = barreShape === 2 || fretboardEnumText(beat.barreShape).indexOf('half') >= 0;
                        result.barre = { fret: barreFret, half: half };
                        add((half ? 'Half barre ' : 'Barre ') + barreFret, fretboardThemeInk('#c084fc', '#7e22ce'));
                    }
                    return result;
                }

                var fretboardRefreshTimer = null;

                function scheduleFretboardRefresh() {
                    if (!fretboardEnabled || isPdfExportMode) return;
                    try { refreshFretboard(); } catch (error) {
                        console.warn('Immediate fretboard render failed', error);
                    }
                    requestAnimationFrame(function() {
                        try { refreshFretboard(); } catch (error) {
                            console.warn('Animation-frame fretboard render failed', error);
                        }
                    });
                    clearTimeout(fretboardRefreshTimer);
                    fretboardRefreshTimer = setTimeout(function() {
                        try { refreshFretboard(); } catch (error) {
                            console.warn('Delayed fretboard render failed', error);
                        }
                    }, 120);
                }

                function fretboardWindowForHighlights(highlighted) {
                    var frets = highlighted.filter(function(note) {
                        return note.fret > 0;
                    }).map(function(note) {
                        return note.fret;
                    });

                    if (frets.length === 0) return fretboardWindowStart;

                    var minimumFret = Math.min.apply(null, frets);
                    var maximumFret = Math.max.apply(null, frets);
                    var currentLastFret = fretboardWindowStart + 11;

                    // The current played note always has priority. A remote slide target
                    // is clamped to the board edge instead of moving the current note away.
                    if (maximumFret > currentLastFret) {
                        fretboardWindowStart = 1 +
                            Math.ceil((maximumFret - 12) / 4) * 4;
                    } else if (minimumFret < fretboardWindowStart) {
                        fretboardWindowStart = minimumFret <= 4
                            ? 1
                            : 1 + Math.floor((minimumFret - 1) / 4) * 4;
                    }

                    fretboardWindowStart = Math.max(1, Math.min(25, fretboardWindowStart));
                    return fretboardWindowStart;
                }

                function keepScoreCursorNearFretboard(force) {
                    if (!fretboardEnabled || isPdfExportMode || isUserScrolling) return;
                    var wrapper = document.getElementById('scroll-wrapper');
                    if (!wrapper || wrapper.clientHeight <= 1) return;

                    // at-cursor-bar thường bao trọn cả khuông + TAB của ô nhịp;
                    // at-cursor-beat là vạch nốt hiện tại. Dùng hợp của cả hai để
                    // đảm bảo phần dưới khuông không nằm sau mép cần đàn.
                    var cursors = [
                        document.querySelector('.at-cursor-bar'),
                        document.querySelector('.at-cursor-beat')
                    ];
                    var activeTop = Infinity;
                    var activeBottom = -Infinity;
                    var hasCursor = false;
                    for (var i = 0; i < cursors.length; i++) {
                        var cursor = cursors[i];
                        if (!cursor) continue;
                        var rect = cursor.getBoundingClientRect();
                        if (!Number.isFinite(rect.top) || !Number.isFinite(rect.bottom) ||
                            rect.width <= 0 || rect.height <= 0) continue;
                        activeTop = Math.min(activeTop, rect.top);
                        activeBottom = Math.max(activeBottom, rect.bottom);
                        hasCursor = true;
                    }
                    if (!hasCursor) return;

                    var wrapperRect = wrapper.getBoundingClientRect();
                    var viewportHeight = wrapper.clientHeight;
                    var relativeTop = activeTop - wrapperRect.top;
                    var relativeBottom = activeBottom - wrapperRect.top;
                    var activeHeight = Math.max(1, relativeBottom - relativeTop);
                    var topMargin = Math.max(12, viewportHeight * .045);
                    var bottomMargin = Math.max(28, viewportHeight * .085);
                    var safeBottom = viewportHeight - bottomMargin;
                    var safeHeight = Math.max(1, safeBottom - topMargin);
                    var activeCenter = (relativeTop + relativeBottom) * .5;

                    // Đặt toàn bộ hệ khuông ở nửa trên của vùng score để mắt chỉ cần
                    // di chuyển một quãng ngắn xuống cần đàn, nhưng vẫn thấy trọn TAB.
                    var targetRelativeTop = topMargin;
                    if (activeHeight < safeHeight) {
                        targetRelativeTop += Math.min(
                            (safeHeight - activeHeight) * .22,
                            viewportHeight * .16
                        );
                    }

                    var shouldMove = force ||
                        relativeTop < topMargin ||
                        relativeBottom > safeBottom ||
                        activeCenter > viewportHeight * .58;
                    if (!shouldMove) return;

                    var absoluteTop = wrapper.scrollTop + relativeTop;
                    var maximumScrollTop = Math.max(0, wrapper.scrollHeight - wrapper.clientHeight);
                    var targetTop = Math.max(
                        0,
                        Math.min(maximumScrollTop, absoluteTop - targetRelativeTop)
                    );
                    if (Math.abs(wrapper.scrollTop - targetTop) > 1) {
                        // Không dùng smooth ở chế độ cần đàn: tại BPM cao, nhiều lần
                        // animation chồng nhau làm TAB trôi sau nốt đang phát.
                        wrapper.scrollTo({ top: targetTop, behavior: 'auto' });
                    }
                }

                function refreshFretboard() {
                    if (!fretboardEnabled || isPdfExportMode) return;
                    var panel = document.getElementById('tt-fretboard-panel');
                    var svg = document.getElementById('tt-fretboard-svg');
                    var status = document.getElementById('tt-fretboard-status');
                    var header = document.getElementById('tt-fretboard-title');
                    var trackName = document.getElementById('tt-fretboard-track');
                    var chordBadge = document.getElementById('tt-fretboard-chord');
                    var close = document.getElementById('tt-fretboard-close');
                    if (!panel || !svg || !status || !header || !trackName ||
                        !chordBadge || !close) return;

                    panel.style.display = 'flex';
                    panel.style.visibility = 'visible';
                    panel.style.opacity = '1';
                    panel.style.pointerEvents = 'auto';
                    header.textContent = fretboardLabels.title;
                    close.textContent = '×';
                    close.setAttribute('aria-label', fretboardLabels.close);
                    chordBadge.textContent = '';
                    chordBadge.classList.remove('is-visible');

                    while (svg.firstChild) svg.removeChild(svg.firstChild);
                    var staff = fretboardStaffForTrack();
                    var count = staff && staff.tuning ? staff.tuning.length : 0;
                    var track = selectedTrackForNoteNames();
                    if (!staff || count < 4) {
                        trackName.textContent = track && track.name ? '• ' + track.name : '';
                        status.textContent = track ? fretboardLabels.unsupported : fretboardLabels.idle;
                        return;
                    }

                    var currentBeat = fretboardLatestBeat;
                    if (currentBeat && !beatBelongsToSelectedTrack(currentBeat, track)) {
                        currentBeat = null;
                    }
                    var isPreview = !currentBeat;
                    if (!currentBeat) currentBeat = firstFrettedBeatForPreview();
                    var usesFlats = currentBeat ? beatUsesFlatNoteNames(currentBeat) : false;
                    var highlighted = [];

                    if (currentBeat && currentBeat.notes) {
                        for (var i = 0; i < currentBeat.notes.length; i++) {
                            var note = currentBeat.notes[i];
                            var number = Math.round(Number(note.string));
                            var fret = Math.round(Number(note.fret));
                            var effects = fretboardNoteEffects(note);
                            if (effects.dead && (!Number.isFinite(fret) || fret < 0)) fret = 0;
                            if (note.isVisible === false ||
                                !Number.isFinite(number) || !Number.isFinite(fret) ||
                                number < 1 || number > count || fret < 0 || fret > 36) continue;
                            var row = count - number;
                            var pitch = effects.dead ? NaN : notePitchForName(note);
                            if (!effects.dead && !Number.isFinite(pitch)) {
                                pitch = Number(staff.tuning[row]) + fret + Number(staff.capo || 0);
                            }
                            highlighted.push({
                                row: row,
                                stringNumber: number,
                                fret: fret,
                                pitch: pitch,
                                pitchClass: effects.dead ? null : fretboardPitchClass(pitch),
                                name: effects.dead ? '×' : fretboardNoteLabel(pitch, usesFlats),
                                note: note,
                                effects: effects,
                                bend: fretboardBendTechnique(note),
                                slides: fretboardSlideTechniques(note)
                            });
                        }
                    }

                    var chordCandidates = highlighted.filter(function(point) {
                        return !(point.effects && point.effects.dead);
                    });
                    var chordInfo = fretboardChordForBeat(
                        currentBeat, chordCandidates, usesFlats, isPreview
                    );
                    var beatTechniques = fretboardBeatTechniques(currentBeat);
                    function ensureBeatTechnique(label, ink) {
                        if (!label) return;
                        for (var ebt = 0; ebt < beatTechniques.labels.length; ebt++) {
                            if (beatTechniques.labels[ebt].label === label) return;
                        }
                        beatTechniques.labels.push({ label: label, ink: ink });
                    }
                    for (var hfx = 0; hfx < highlighted.length; hfx++) {
                        var noteEffects = highlighted[hfx].effects || {};
                        if (noteEffects.palmMute) ensureBeatTechnique('P.M.', fretboardThemeInk('#fbbf24', '#b45309'));
                        if (noteEffects.letRing) ensureBeatTechnique('Let ring', fretboardThemeInk('#86efac', '#047857'));
                    }
                    // Không hiển thị badge "Hợp âm: ..." ở header. Tên hợp âm
                    // sẽ nằm ngay phía trên cụm nốt trong cần đàn.

                    var firstFret = fretboardWindowForHighlights(highlighted);
                    var visibleFrets = 12;
                    var lastFret = firstFret + visibleFrets - 1;
                    var trackParts = [];
                    if (track && track.name) trackParts.push(track.name);
                    trackParts.push(fretboardLabels.fret + ' ' + firstFret + '–' + lastFret);
                    trackName.textContent = '• ' + trackParts.join(' • ');

                    var viewW = 410;
                    // More vertical room prevents adjacent string labels and note circles
                    // from touching. The open-string column is also separated from labels.
                    var viewH = Math.max(220, 27 * count + 58);
                    var labelCenterX = 17;
                    var openStringX = 50;
                    var boardStartX = 64;
                    var boardEndX = 401;
                    var boardTop = 34;
                    var boardBottom = viewH - 27;
                    var topY = 55;
                    var bottomY = viewH - 43;
                    var fretWidth = (boardEndX - boardStartX) / visibleFrets;
                    var centerY = (topY + bottomY) * .5;

                    // Màu cần đàn và nốt được lấy trực tiếp từ bảng tùy chỉnh.
                    // Độ sáng của chính mặt cần đàn quyết định màu phím, dây và chữ,
                    // nên người dùng vẫn có thể dùng cần đàn tối trong theme sáng hoặc ngược lại.
                    var boardInk = normalizeVisualHex(
                        currentVisualPreferences.fretboardColor,
                        currentThemeIsDark ? '#171B24' : '#F8FAFC'
                    );
                    var boardIsLight = visualLuminance(boardInk) > 0.48;
                    var isDarkBoardTheme = !boardIsLight;
                    var boardContrastInk = boardIsLight ? '#1F2937' : '#F8FAFC';
                    var boardBorderInk = visualMix(
                        boardInk,
                        boardIsLight ? '#475569' : '#CBD5E1',
                        boardIsLight ? .52 : .44
                    );
                    var labelInk = boardContrastInk;
                    var secondaryInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .72 : .68
                    );
                    var fretInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .44 : .40
                    );
                    var stringInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .62 : .58
                    );
                    var markerInk = visualMix(
                        boardInk,
                        boardContrastInk,
                        boardIsLight ? .50 : .46
                    );
                    var activeNoteInk = normalizeVisualHex(
                        currentVisualPreferences.noteColor,
                        currentThemeIsDark ? '#FFD43B' : '#FBBF24'
                    );
                    // Dây đang đánh dùng cùng họ màu với nốt nhưng được tăng tương phản
                    // khi người dùng chọn nền cần đàn sáng gần với màu nốt.
                    var activeInk = activeNoteInk;
                    if (visualContrast(activeInk, boardInk) < 2.35) {
                        activeInk = visualMix(
                            activeInk,
                            boardIsLight ? '#3F2A00' : '#FFFFFF',
                            boardIsLight ? .48 : .30
                        );
                    }
                    var activeTextInk = visualTextColor(activeNoteInk);
                    var previewInk = visualMix(boardInk, boardContrastInk, .22);
                    var noteInk = isPreview ? previewInk : activeNoteInk;
                    var slideInk = isDarkBoardTheme ? '#38BDF8' : '#0277B5';
                    var bendInk = isDarkBoardTheme ? '#FB923C' : '#D9480F';
                    var legatoInk = isDarkBoardTheme ? '#34D399' : '#047857';
                    var trillInk = isDarkBoardTheme ? '#F472B6' : '#BE185D';
                    var vibratoInk = isDarkBoardTheme ? '#22D3EE' : '#0E7490';
                    var whammyInk = isDarkBoardTheme ? '#2DD4BF' : '#0F766E';
                    var rhythmInk = isDarkBoardTheme ? '#A78BFA' : '#6D28D9';
                    var labelBubbleInk = visualMix(
                        boardInk,
                        boardIsLight ? '#FFFFFF' : '#000000',
                        boardIsLight ? .56 : .30
                    );
                    var labelBubbleBorderInk = boardBorderInk;
                    var stringSheenInk = boardContrastInk;
                    var stringSheenOpacity = boardIsLight ? .14 : .30;
                    var nutInk = visualMix(boardInk, boardContrastInk, .78);
                    var techniquePillInk = boardIsLight ? '#FFFFFF' : '#0B1220';
                    var techniquePillOpacity = .95;
                    var noteOutlineInk = visualTextColor(activeNoteInk);
                    var ghostInk = isDarkBoardTheme ? '#FDE68A' : '#B45309';
                    var deadNoteFillInk = boardIsLight ? '#FFFFFF' : '#111827';
                    var accentInk = boardContrastInk;
                    var slurInk = isDarkBoardTheme ? '#A5B4FC' : '#4F46E5';
                    var tieInk = visualMix(boardInk, boardContrastInk, .62);
                    var cellShadeInk = boardContrastInk;
                    var cellShadeOpacity = boardIsLight ? .026 : .018;
                    var activeRowMap = {};
                    for (var a = 0; a < highlighted.length; a++) {
                        activeRowMap[highlighted[a].row] = true;
                    }

                    svg.setAttribute('viewBox', '0 0 ' + viewW + ' ' + viewH);
                    svg.setAttribute(
                        'aria-label',
                        fretboardLabels.title + ', ' + count + ' strings, ' +
                        fretboardLabels.fret + ' ' + firstFret + ' to ' + lastFret
                    );

                    var defs = fretboardSvgElement(svg, 'defs', {});
                    var glowFilter = fretboardSvgElement(defs, 'filter', {
                        id: 'tt-note-glow', x: '-80%', y: '-80%', width: '260%', height: '260%'
                    });
                    fretboardSvgElement(glowFilter, 'feGaussianBlur', {
                        stdDeviation: 3.6, result: 'blur'
                    });
                    var glowMerge = fretboardSvgElement(glowFilter, 'feMerge', {});
                    fretboardSvgElement(glowMerge, 'feMergeNode', { 'in': 'blur' });
                    fretboardSvgElement(glowMerge, 'feMergeNode', { 'in': 'SourceGraphic' });


                    var slideArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-slide-arrow', markerWidth: 8, markerHeight: 8,
                        refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(slideArrow, 'path', {
                        d: 'M 0 0 L 8 4 L 0 8 z', fill: slideInk
                    });
                    var bendArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-bend-arrow', markerWidth: 8, markerHeight: 8,
                        refX: 7, refY: 4, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(bendArrow, 'path', {
                        d: 'M 0 0 L 8 4 L 0 8 z', fill: bendInk
                    });
                    var legatoArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-legato-arrow', markerWidth: 7, markerHeight: 7,
                        refX: 6, refY: 3.5, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(legatoArrow, 'path', {
                        d: 'M 0 0 L 7 3.5 L 0 7 z', fill: legatoInk
                    });
                    var whammyArrow = fretboardSvgElement(defs, 'marker', {
                        id: 'tt-whammy-arrow', markerWidth: 7, markerHeight: 7,
                        refX: 6, refY: 3.5, orient: 'auto', markerUnits: 'userSpaceOnUse'
                    });
                    fretboardSvgElement(whammyArrow, 'path', {
                        d: 'M 0 0 L 7 3.5 L 0 7 z', fill: whammyInk
                    });

                    fretboardSvgElement(svg, 'rect', {
                        x: 39, y: boardTop, width: boardEndX - 39,
                        height: boardBottom - boardTop, rx: 8,
                        fill: boardInk,
                        stroke: boardBorderInk,
                        'stroke-width': 1.35
                    });

                    // Phân khoang rất nhẹ; nền không còn vân gỗ cạnh tranh với nốt.
                    for (var cell = 0; cell < visibleFrets; cell++) {
                        if (cell % 2 === 1) {
                            fretboardSvgElement(svg, 'rect', {
                                x: boardStartX + cell * fretWidth,
                                y: boardTop + 1,
                                width: fretWidth,
                                height: boardBottom - boardTop - 2,
                                fill: cellShadeInk,
                                opacity: cellShadeOpacity
                            });
                        }
                    }

                    // Không tô cả khoang ngăn. Chỉ dây thực sự phát sáng để
                    // người học nhận biết nhanh mà không bị quá nhiều màu cạnh tranh.

                    // Dấu mốc 3, 5, 7, 9, 12... chỉ hỗ trợ định vị.
                    var standardMarkers = [3, 5, 7, 9, 12, 15, 17, 19, 21, 24];
                    for (var m = 0; m < standardMarkers.length; m++) {
                        var markerFret = standardMarkers[m];
                        if (markerFret < firstFret || markerFret > lastFret) continue;
                        var markerX = boardStartX +
                            (markerFret - firstFret + .5) * fretWidth;
                        if (markerFret % 12 === 0) {
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: topY + (bottomY - topY) * .34,
                                r: 3.2, fill: markerInk, opacity: .55
                            });
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: topY + (bottomY - topY) * .66,
                                r: 3.2, fill: markerInk, opacity: .55
                            });
                        } else {
                            fretboardSvgElement(svg, 'circle', {
                                cx: markerX, cy: centerY,
                                r: 3.3, fill: markerInk, opacity: .55
                            });
                        }
                    }

                    var stringStartX = boardStartX - 25;
                    for (var j = 0; j < count; j++) {
                        var y = topY + (bottomY - topY) *
                            (count === 1 ? 0 : j / (count - 1));
                        var tuningPitch = Number(staff.tuning[j]);
                        var rowIsActive = !isPreview && activeRowMap[j] === true;

                        // Tuning labels are fixed reference labels. They never light up,
                        // so an open-string note cannot be mistaken for the label itself.
                        fretboardSvgElement(svg, 'circle', {
                            cx: labelCenterX, cy: y, r: 8.8,
                            fill: labelBubbleInk, stroke: labelBubbleBorderInk, 'stroke-width': 1.2
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: labelCenterX, y: y + 3.3, 'font-size': 9.2,
                            'font-weight': 900, 'text-anchor': 'middle', fill: labelInk
                        }, fretboardNoteLabel(tuningPitch, false));

                        if (rowIsActive) {
                            fretboardSvgElement(svg, 'line', {
                                x1: stringStartX, y1: y,
                                x2: boardEndX, y2: y,
                                stroke: activeInk,
                                'stroke-width': Math.min(6.0, 4.0 + j * .22),
                                opacity: .28,
                                filter: 'url(#tt-note-glow)'
                            });
                        }
                        fretboardSvgElement(svg, 'line', {
                            x1: stringStartX, y1: y,
                            x2: boardEndX, y2: y,
                            stroke: rowIsActive ? activeInk : stringInk,
                            'stroke-width': rowIsActive
                                ? Math.min(3.2, 2.25 + j * .16)
                                : Math.min(2.35, 1.0 + j * .24),
                            opacity: rowIsActive ? 1 : .72
                        });
                        if (!rowIsActive) {
                            fretboardSvgElement(svg, 'line', {
                                x1: stringStartX, y1: y - .42,
                                x2: boardEndX, y2: y - .42,
                                stroke: stringSheenInk, 'stroke-width': .42,
                                opacity: stringSheenOpacity
                            });
                        }
                    }

                    for (var f = 0; f <= visibleFrets; f++) {
                        var fx = boardStartX + f * fretWidth;
                        fretboardSvgElement(svg, 'line', {
                            x1: fx, y1: boardTop + 1,
                            x2: fx, y2: boardBottom - 1,
                            stroke: f === 0 && firstFret === 1 ? nutInk : fretInk,
                            'stroke-width': f === 0 && firstFret === 1 ? 4.2 : 1.35,
                            opacity: f === 0 && firstFret === 1 ? 1 : .9
                        });

                        if (f < visibleFrets) {
                            var fretNumber = firstFret + f;
                            var numberX = fx + fretWidth * .5;
                            fretboardSvgElement(svg, 'text', {
                                x: numberX, y: viewH - 10,
                                'font-size': visibleFrets > 12 ? 8.5 : 10,
                                'text-anchor': 'middle', 'font-weight': 700,
                                fill: secondaryInk
                            }, fretNumber);
                        }
                    }

                    function fretboardXForFret(fret) {
                        if (fret === 0) return openStringX;
                        var rawX = boardStartX + (fret - firstFret + .5) * fretWidth;
                        return Math.max(boardStartX + 2, Math.min(boardEndX - 2, rawX));
                    }

                    function fretboardYForString(stringNumber) {
                        var row = count - Number(stringNumber);
                        if (!Number.isFinite(row) || row < 0 || row >= count) return null;
                        return topY + (bottomY - topY) *
                            (count === 1 ? 0 : row / (count - 1));
                    }

                    var techniqueLabelBoxes = [];
                    function drawTechniquePill(label, centerX, desiredY, ink) {
                        if (!label) return;
                        var width = Math.min(96, Math.max(30, 12 + label.length * 5.2));
                        var height = 15;
                        var x = Math.max(39, Math.min(viewW - width - 4, centerX - width * .5));
                        var minimumY = boardTop + 2;
                        var baseY = Math.max(minimumY, Math.min(boardBottom - height - 2, desiredY));
                        var y = baseY;
                        var offsets = [0, -16, 16, -32, 32, -48, 48, -64, 64];
                        for (var attempt = 0; attempt < offsets.length; attempt++) {
                            y = Math.max(
                                minimumY,
                                Math.min(boardBottom - height - 2, baseY + offsets[attempt])
                            );
                            var overlaps = false;
                            for (var b = 0; b < techniqueLabelBoxes.length; b++) {
                                var box = techniqueLabelBoxes[b];
                                if (!(x + width + 2 < box.x || x > box.x + box.w + 2 ||
                                    y + height + 2 < box.y || y > box.y + box.h + 2)) {
                                    overlaps = true;
                                    break;
                                }
                            }
                            if (!overlaps) break;
                        }
                        techniqueLabelBoxes.push({ x: x, y: y, w: width, h: height });
                        fretboardSvgElement(svg, 'rect', {
                            x: x, y: y, width: width, height: height, rx: 6.5,
                            fill: techniquePillInk, stroke: ink, 'stroke-width': 1.1,
                            opacity: techniquePillOpacity
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: x + width * .5, y: y + 10.6,
                            'text-anchor': 'middle', 'font-size': 8.2,
                            'font-weight': 900, fill: ink
                        }, label);
                    }

                    function drawHorizontalWave(x1, x2, y, amplitude, cycles, ink, width) {
                        var startX = Math.min(x1, x2);
                        var endX = Math.max(x1, x2);
                        if (endX - startX < 8) endX = startX + 8;
                        var segments = Math.max(4, Math.round(cycles * 2));
                        var segmentWidth = (endX - startX) / segments;
                        var path = 'M ' + startX + ' ' + y;
                        for (var waveIndex = 0; waveIndex < segments; waveIndex++) {
                            var controlX = startX + segmentWidth * (waveIndex + .5);
                            var endSegmentX = startX + segmentWidth * (waveIndex + 1);
                            var controlY = y + (waveIndex % 2 === 0 ? -amplitude : amplitude);
                            path += ' Q ' + controlX + ' ' + controlY + ' ' + endSegmentX + ' ' + y;
                        }
                        fretboardSvgElement(svg, 'path', {
                            d: path, fill: 'none', stroke: ink,
                            'stroke-width': width || 1.8,
                            'stroke-linecap': 'round', 'stroke-linejoin': 'round'
                        });
                    }

                    function drawConnectionTechnique(effect, pointX, pointY, pointRadius, ink) {
                        if (!effect) return;
                        var source = effect.phase === 'in' ? effect.origin : null;
                        var target = effect.phase === 'out' ? effect.target : null;
                        var sx = source ? fretboardXForFret(source.fret) : pointX;
                        var sy = source ? fretboardYForString(source.stringNumber) : pointY;
                        var ex = target ? fretboardXForFret(target.fret) : pointX;
                        var ey = target ? fretboardYForString(target.stringNumber) : pointY;
                        if (sy == null) sy = pointY;
                        if (ey == null) ey = pointY;
                        if (!source && effect.phase === 'in') sx = pointX - fretWidth * 1.15;
                        if (!target && effect.phase === 'out') ex = pointX + fretWidth * 1.15;
                        if (Math.abs(ex - sx) < 5 && Math.abs(ey - sy) < 5) ex = sx + fretWidth * 1.15;
                        var direction = ex >= sx ? 1 : -1;
                        if (!source) sx += direction * pointRadius * .68;
                        if (!target) ex -= direction * 3;
                        var controlX = (sx + ex) * .5;
                        var controlY = Math.min(sy, ey) - 16;
                        fretboardSvgElement(svg, 'path', {
                            d: 'M ' + sx + ' ' + sy + ' Q ' + controlX + ' ' + controlY + ' ' + ex + ' ' + ey,
                            fill: 'none', stroke: ink, 'stroke-width': 2,
                            'stroke-linecap': 'round', 'stroke-linejoin': 'round',
                            'marker-end': effect.label === 'Tie' ? '' : 'url(#tt-legato-arrow)'
                        });
                        drawTechniquePill(effect.label, controlX, controlY - 12, ink);
                    }

                    var topBadgeBoxes = [];
                    function reserveTopBadgeBox(box) {
                        if (box) topBadgeBoxes.push(box);
                    }
                    function drawTopTechniqueBadge(item) {
                        if (!item || !item.label) return;
                        var label = String(item.label);
                        var ink = item.ink || secondaryInk;
                        var width = Math.min(78, Math.max(26, 10 + label.length * 4.7));
                        var height = 13;
                        var rows = [2, 17, boardTop + 3, boardTop + 18];
                        var chosen = null;
                        for (var rowIndex = 0; rowIndex < rows.length && !chosen; rowIndex++) {
                            for (var scanX = 3; scanX <= viewW - width - 3; scanX += 3) {
                                var candidate = { x: scanX, y: rows[rowIndex], w: width, h: height };
                                var blocked = false;
                                for (var tb = 0; tb < topBadgeBoxes.length; tb++) {
                                    var occupied = topBadgeBoxes[tb];
                                    if (!(candidate.x + candidate.w + 2 < occupied.x ||
                                        candidate.x > occupied.x + occupied.w + 2 ||
                                        candidate.y + candidate.h + 1 < occupied.y ||
                                        candidate.y > occupied.y + occupied.h + 1)) {
                                        blocked = true;
                                        break;
                                    }
                                }
                                if (!blocked) {
                                    chosen = candidate;
                                    break;
                                }
                            }
                        }
                        if (!chosen) chosen = { x: 3, y: boardTop + 3, w: width, h: height };
                        reserveTopBadgeBox(chosen);
                        fretboardSvgElement(svg, 'rect', {
                            x: chosen.x, y: chosen.y, width: chosen.w, height: chosen.h,
                            rx: 6, fill: techniquePillInk, stroke: ink,
                            'stroke-width': .95, opacity: techniquePillOpacity
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: chosen.x + chosen.w * .5,
                            y: chosen.y + 9.2,
                            'text-anchor': 'middle', 'font-size': 7.2,
                            'font-weight': 900, fill: ink
                        }, label);
                    }

                    if (beatTechniques.barre && beatTechniques.barre.fret >= firstFret &&
                        beatTechniques.barre.fret <= lastFret) {
                        var barreX = fretboardXForFret(beatTechniques.barre.fret);
                        var barreY1 = topY - 8;
                        var barreY2 = beatTechniques.barre.half
                            ? topY + (bottomY - topY) * .52
                            : bottomY + 8;
                        fretboardSvgElement(svg, 'rect', {
                            x: barreX - 5.5, y: barreY1,
                            width: 11, height: Math.max(12, barreY2 - barreY1), rx: 5.5,
                            fill: fretboardThemeInk('#c084fc', '#7e22ce'), opacity: .20,
                            stroke: fretboardThemeInk('#c084fc', '#7e22ce'), 'stroke-width': 1.5
                        });
                    }

                    if (beatTechniques.brush && highlighted.length > 1) {
                        var brushRows = highlighted.map(function(point) { return point.row; });
                        var brushMinRow = Math.min.apply(null, brushRows);
                        var brushMaxRow = Math.max.apply(null, brushRows);
                        var brushTopY = topY + (bottomY - topY) *
                            (count === 1 ? 0 : brushMinRow / (count - 1));
                        var brushBottomY = topY + (bottomY - topY) *
                            (count === 1 ? 0 : brushMaxRow / (count - 1));
                        var brushX = boardEndX - 7;
                        var brushSymbol = beatTechniques.brush.direction === 'up' ? '↑' : '↓';
                        fretboardSvgElement(svg, 'line', {
                            x1: brushX, y1: brushTopY, x2: brushX, y2: brushBottomY,
                            stroke: rhythmInk, 'stroke-width': 2.2,
                            'stroke-dasharray': beatTechniques.brush.arpeggio ? '3 2' : '',
                            opacity: .95
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: brushX - 1,
                            y: beatTechniques.brush.direction === 'up' ? brushTopY - 4 : brushBottomY + 10,
                            'text-anchor': 'middle', 'font-size': 15,
                            'font-weight': 900, fill: rhythmInk
                        }, brushSymbol);
                    }

                    var chordMinX = Infinity;
                    var chordMaxX = -Infinity;
                    var renderedPoints = [];
                    for (var k = 0; k < highlighted.length; k++) {
                        var point = highlighted[k];
                        if (point.fret > 0 &&
                            (point.fret < firstFret || point.fret > lastFret)) continue;

                        var px = fretboardXForFret(point.fret);
                        var py = topY + (bottomY - topY) *
                            (count === 1 ? 0 : point.row / (count - 1));
                        var radius = Math.min(12.4, Math.max(9.4, fretWidth * .38));
                        chordMinX = Math.min(chordMinX, px);
                        chordMaxX = Math.max(chordMaxX, px);
                        renderedPoints.push({
                            point: point, px: px, py: py, radius: radius
                        });
                    }

                    if (beatTechniques.whammy && renderedPoints.length > 0) {
                        var whammyStartX = Math.max(boardStartX + 4, chordMinX - 8);
                        var whammyEndX = Math.min(boardEndX - 4, Math.max(chordMaxX + 28, whammyStartX + 48));
                        var whammyBaseY = boardTop + 10;
                        var whammyPath = '';
                        var whammyType = beatTechniques.whammy.type;
                        if (whammyType === 3) {
                            whammyPath = 'M ' + whammyStartX + ' ' + whammyBaseY +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + (whammyBaseY + 15) +
                                ' ' + whammyEndX + ' ' + whammyBaseY;
                        } else if (whammyType === 4) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 7) +
                                ' L ' + whammyEndX + ' ' + (whammyBaseY + 7);
                        } else if (whammyType === 5) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 13) +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + whammyBaseY +
                                ' ' + whammyEndX + ' ' + whammyBaseY;
                        } else if (whammyType === 6) {
                            whammyPath = 'M ' + whammyStartX + ' ' + (whammyBaseY + 13) +
                                ' Q ' + ((whammyStartX + whammyEndX) * .45) + ' ' + (whammyBaseY + 2) +
                                ' ' + whammyEndX + ' ' + (whammyBaseY + 16);
                        } else {
                            whammyPath = 'M ' + whammyStartX + ' ' + whammyBaseY +
                                ' Q ' + ((whammyStartX + whammyEndX) * .5) + ' ' + (whammyBaseY + 2) +
                                ' ' + whammyEndX + ' ' + (whammyBaseY + 15);
                        }
                        fretboardSvgElement(svg, 'path', {
                            d: whammyPath, fill: 'none', stroke: whammyInk,
                            'stroke-width': 2.2, 'stroke-linecap': 'round',
                            'stroke-linejoin': 'round',
                            'marker-end': whammyType === 4 ? '' : 'url(#tt-whammy-arrow)'
                        });
                    }

                    // Technique paths are drawn first. Note heads are drawn afterward,
                    // so every line appears to start behind the fingertip marker.
                    for (var t = 0; t < renderedPoints.length; t++) {
                        var rendered = renderedPoints[t];
                        var techniquePoint = rendered.point;
                        var tx0 = rendered.px;
                        var ty0 = rendered.py;
                        var tr = rendered.radius;
                        var slides = techniquePoint.slides || [];

                        for (var sl = 0; sl < slides.length; sl++) {
                            var slide = slides[sl];
                            var sx = tx0;
                            var sy = ty0;
                            var ex = tx0;
                            var ey = ty0;
                            var dash = slide.pick ? '4 3' : '';

                            if (slide.phase === 'out-target' && slide.target) {
                                ex = fretboardXForFret(slide.target.fret);
                                var targetY = fretboardYForString(slide.target.stringNumber);
                                ey = targetY == null ? ty0 : targetY;
                                var outDirection = ex >= tx0 ? 1 : -1;
                                sx = tx0 + outDirection * tr * .72;
                                ex -= outDirection * 5;
                            } else if (slide.phase === 'in-target' && slide.origin) {
                                sx = fretboardXForFret(slide.origin.fret);
                                var originY = fretboardYForString(slide.origin.stringNumber);
                                sy = originY == null ? ty0 : originY;
                                var inDirection = tx0 >= sx ? 1 : -1;
                                sx += inDirection * 5;
                                ex = tx0 - inDirection * tr * .72;
                                ey = ty0;
                            } else if (slide.phase === 'in-free') {
                                sx = tx0 - 29;
                                sy = ty0 + (slide.direction === 'below' ? 15 : -15);
                                ex = tx0 - tr * .72;
                                ey = ty0;
                            } else {
                                sx = tx0 + tr * .72;
                                sy = ty0;
                                ex = tx0 + 31;
                                ey = ty0 + (slide.direction === 'down' ? 15 : -15);
                            }

                            var midX = (sx + ex) * .5;
                            var midY = (sy + ey) * .5;
                            fretboardSvgElement(svg, 'path', {
                                d: 'M ' + sx + ' ' + sy +
                                    ' Q ' + midX + ' ' + (midY - 4) + ' ' + ex + ' ' + ey,
                                fill: 'none', stroke: slideInk, 'stroke-width': 2.25,
                                'stroke-linecap': 'round', 'stroke-linejoin': 'round',
                                'stroke-dasharray': dash, 'marker-end': 'url(#tt-slide-arrow)'
                            });
                            drawTechniquePill(
                                slide.label,
                                midX,
                                Math.min(sy, ey) - 20,
                                slideInk
                            );
                        }

                        var bend = techniquePoint.bend;
                        if (bend) {
                            var bendPath = '';
                            var bendLabelX = tx0 + 21;
                            var bendLabelY = ty0 - 36;
                            if (bend.shape === 'down') {
                                bendPath = 'M ' + (tx0 + 4) + ' ' + (ty0 - 31) +
                                    ' C ' + (tx0 + 16) + ' ' + (ty0 - 29) + ', ' +
                                    (tx0 + 22) + ' ' + (ty0 - 16) + ', ' +
                                    (tx0 + 23) + ' ' + (ty0 - tr * .72);
                            } else if (bend.shape === 'updown') {
                                bendPath = 'M ' + (tx0 + 3) + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 10) + ' ' + (ty0 - 33) + ', ' +
                                    (tx0 + 20) + ' ' + (ty0 - 34) + ', ' +
                                    (tx0 + 25) + ' ' + (ty0 - 23) +
                                    ' C ' + (tx0 + 29) + ' ' + (ty0 - 14) + ', ' +
                                    (tx0 + 31) + ' ' + (ty0 - 11) + ', ' +
                                    (tx0 + 34) + ' ' + (ty0 - tr * .5);
                                bendLabelX = tx0 + 18;
                                bendLabelY = ty0 - 50;
                            } else if (bend.shape === 'hold') {
                                bendPath = 'M ' + (tx0 + tr * .72) + ' ' + (ty0 - 12) +
                                    ' L ' + (tx0 + 34) + ' ' + (ty0 - 12);
                                bendLabelY = ty0 - 34;
                            } else if (bend.shape === 'preup') {
                                bendPath = 'M ' + tx0 + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 1) + ' ' + (ty0 - 19) + ', ' +
                                    (tx0 + 7) + ' ' + (ty0 - 26) + ', ' +
                                    (tx0 + 13) + ' ' + (ty0 - 33);
                                bendLabelX = tx0 + 8;
                                bendLabelY = ty0 - 50;
                            } else {
                                bendPath = 'M ' + (tx0 + 3) + ' ' + (ty0 - tr * .72) +
                                    ' C ' + (tx0 + 8) + ' ' + (ty0 - 20) + ', ' +
                                    (tx0 + 16) + ' ' + (ty0 - 27) + ', ' +
                                    (tx0 + 24) + ' ' + (ty0 - 33);
                            }
                            fretboardSvgElement(svg, 'path', {
                                d: bendPath, fill: 'none', stroke: bendInk,
                                'stroke-width': 2.35, 'stroke-linecap': 'round',
                                'stroke-linejoin': 'round',
                                'marker-end': bend.shape === 'hold' ? '' : 'url(#tt-bend-arrow)'
                            });
                            drawTechniquePill(bend.label, bendLabelX, bendLabelY, bendInk);
                        }

                        var pointEffects = techniquePoint.effects || {};
                        drawConnectionTechnique(pointEffects.hammerPull, tx0, ty0, tr, legatoInk);
                        drawConnectionTechnique(pointEffects.slur, tx0, ty0, tr, slurInk);
                        drawConnectionTechnique(pointEffects.tie, tx0, ty0, tr, tieInk);

                        if (pointEffects.trill) {
                            var trillTargetX = pointEffects.trill.targetFret == null
                                ? tx0 + fretWidth * 1.15
                                : fretboardXForFret(pointEffects.trill.targetFret);
                            if (Math.abs(trillTargetX - tx0) < 8) trillTargetX = tx0 + fretWidth;
                            var trillY = ty0 - tr - 8;
                            drawHorizontalWave(
                                tx0 + tr * .55,
                                trillTargetX,
                                trillY,
                                3.2,
                                4,
                                trillInk,
                                1.9
                            );
                            drawTechniquePill(
                                pointEffects.trill.label,
                                (tx0 + trillTargetX) * .5,
                                trillY - 19,
                                trillInk
                            );
                        }

                        if (pointEffects.vibrato) {
                            var vibratoY = ty0 - tr - 5;
                            drawHorizontalWave(
                                tx0 - tr * .75,
                                tx0 + tr * .9,
                                vibratoY,
                                pointEffects.vibrato.wide ? 4 : 2.5,
                                pointEffects.vibrato.wide ? 3 : 4,
                                vibratoInk,
                                1.8
                            );
                            drawTechniquePill(
                                pointEffects.vibrato.label,
                                tx0,
                                vibratoY - 19,
                                vibratoInk
                            );
                        }
                    }

                    for (var n = 0; n < renderedPoints.length; n++) {
                        var renderedNote = renderedPoints[n];
                        var notePoint = renderedNote.point;
                        var noteX = renderedNote.px;
                        var noteY = renderedNote.py;
                        var noteRadius = renderedNote.radius;

                        var effects = notePoint.effects || {};
                        var noteFill = effects.dead ? deadNoteFillInk : noteInk;
                        var noteStroke = effects.ghost ? ghostInk : noteOutlineInk;
                        var noteOpacity = effects.ghost ? .68 : 1;

                        fretboardSvgElement(svg, 'circle', {
                            cx: noteX, cy: noteY, r: noteRadius + 6.2,
                            fill: noteFill,
                            opacity: isPreview ? .14 : (effects.ghost ? .16 : .28),
                            filter: 'url(#tt-note-glow)'
                        });

                        if (effects.harmonic) {
                            var diamond = [
                                noteX + ',' + (noteY - noteRadius),
                                (noteX + noteRadius) + ',' + noteY,
                                noteX + ',' + (noteY + noteRadius),
                                (noteX - noteRadius) + ',' + noteY
                            ].join(' ');
                            fretboardSvgElement(svg, 'polygon', {
                                points: diamond, fill: noteFill,
                                stroke: noteStroke, 'stroke-width': 2.15,
                                opacity: noteOpacity
                            });
                        } else {
                            fretboardSvgElement(svg, 'circle', {
                                cx: noteX, cy: noteY, r: noteRadius,
                                fill: noteFill,
                                stroke: effects.dead ? activeInk : noteStroke,
                                'stroke-width': 2.15,
                                'stroke-dasharray': effects.ghost ? '3 2' : '',
                                opacity: noteOpacity
                            });
                        }

                        fretboardSvgElement(svg, 'text', {
                            x: noteX, y: noteY + 3.4, 'text-anchor': 'middle',
                            'font-size': effects.dead ? 13 : (noteRadius < 11 ? 8.2 : 9.3),
                            'font-weight': 900,
                            fill: effects.dead ? activeInk : activeTextInk
                        }, effects.dead ? '×' : notePoint.name);

                        if (effects.ghost) {
                            fretboardSvgElement(svg, 'text', {
                                x: noteX - noteRadius - 3, y: noteY + 3.2,
                                'text-anchor': 'middle', 'font-size': 12,
                                'font-weight': 800, fill: ghostInk
                            }, '(');
                            fretboardSvgElement(svg, 'text', {
                                x: noteX + noteRadius + 3, y: noteY + 3.2,
                                'text-anchor': 'middle', 'font-size': 12,
                                'font-weight': 800, fill: ghostInk
                            }, ')');
                        }

                        if (effects.staccato) {
                            fretboardSvgElement(svg, 'circle', {
                                cx: noteX, cy: noteY + noteRadius + 5,
                                r: 1.8, fill: accentInk
                            });
                        }
                        if (effects.accent) {
                            fretboardSvgElement(svg, 'text', {
                                x: noteX, y: noteY - noteRadius - 5,
                                'text-anchor': 'middle', 'font-size': 10,
                                'font-weight': 900, fill: accentInk
                            }, effects.accent.symbol);
                        }

                        if (beatTechniques.tremolo) {
                            var tremolo = beatTechniques.tremolo;
                            if (tremolo.buzz) {
                                fretboardSvgElement(svg, 'text', {
                                    x: noteX + noteRadius + 5,
                                    y: noteY - noteRadius + 2,
                                    'font-size': 10, 'font-weight': 900,
                                    fill: trillInk
                                }, 'Z');
                            } else if (tremolo.marks > 0) {
                                var tremoloBaseX = noteX - Math.min(8, tremolo.marks * 2.2);
                                var tremoloY = noteY - noteRadius - 4;
                                for (var tm = 0; tm < tremolo.marks; tm++) {
                                    var markX = tremoloBaseX + tm * 4;
                                    fretboardSvgElement(svg, 'line', {
                                        x1: markX, y1: tremoloY + 3,
                                        x2: markX + 5, y2: tremoloY - 3,
                                        stroke: trillInk, 'stroke-width': 1.8,
                                        'stroke-linecap': 'round'
                                    });
                                }
                            } else {
                                fretboardSvgElement(svg, 'text', {
                                    x: noteX + noteRadius + 4,
                                    y: noteY - noteRadius + 1,
                                    'font-size': 8.5, 'font-weight': 900,
                                    fill: trillInk
                                }, 'TR');
                            }
                        }

                        var effectTags = effects.tags || [];
                        for (var tagIndex = 0; tagIndex < effectTags.length; tagIndex++) {
                            var tag = effectTags[tagIndex];
                            var tagY = tagIndex % 2 === 0
                                ? noteY + noteRadius + 8 + Math.floor(tagIndex / 2) * 17
                                : noteY - noteRadius - 25 - Math.floor(tagIndex / 2) * 17;
                            drawTechniquePill(tag.label, noteX, tagY, tag.ink);
                        }
                    }

                    // Chỉ hiển thị tên (ví dụ: Am), đặt ngay phía trên cụm vị trí bấm.
                    // Không thêm tiền tố "Hợp âm" và không lặp tên ở dòng trạng thái.
                    if (chordInfo && chordInfo.name &&
                        Number.isFinite(chordMinX) && Number.isFinite(chordMaxX)) {
                        var chordText = String(chordInfo.name);
                        var chordCenterX = (chordMinX + chordMaxX) * .5;
                        var chordLabelWidth = Math.min(
                            102,
                            Math.max(36, 19 + chordText.length * 8.4)
                        );
                        var chordLabelX = Math.max(
                            4,
                            Math.min(viewW - chordLabelWidth - 5,
                                chordCenterX - chordLabelWidth * .5)
                        );
                        var chordPointerX = Math.max(
                            chordLabelX + 9,
                            Math.min(chordLabelX + chordLabelWidth - 9, chordCenterX)
                        );
                        var chordBox = {
                            x: chordLabelX, y: 3,
                            w: chordLabelWidth, h: 25
                        };
                        reserveTopBadgeBox(chordBox);
                        var chordBadgeInk = normalizeVisualHex(
                            currentVisualPreferences.cursorColor,
                            currentThemeIsDark ? '#C084FC' : '#7C3AED'
                        );
                        var chordBadgeBorder = visualMix(
                            chordBadgeInk,
                            visualTextColor(chordBadgeInk),
                            .42
                        );
                        fretboardSvgElement(svg, 'line', {
                            x1: chordPointerX, y1: 28,
                            x2: Math.max(39, Math.min(boardEndX, chordCenterX)), y2: boardTop - 2,
                            stroke: chordBadgeBorder, 'stroke-width': 1.4, opacity: .9
                        });
                        fretboardSvgElement(svg, 'rect', {
                            x: chordLabelX, y: 3,
                            width: chordLabelWidth, height: 25, rx: 12.5,
                            fill: chordBadgeInk, stroke: chordBadgeBorder,
                            'stroke-width': 1.2
                        });
                        fretboardSvgElement(svg, 'text', {
                            x: chordLabelX + chordLabelWidth * .5,
                            y: 20, 'text-anchor': 'middle',
                            'font-size': 13, 'font-weight': 900,
                            fill: visualTextColor(chordBadgeInk)
                        }, chordText);
                    }

                    for (var badgeIndex = 0; badgeIndex < beatTechniques.labels.length; badgeIndex++) {
                        drawTopTechniqueBadge(beatTechniques.labels[badgeIndex]);
                    }

                    if (!currentBeat) {
                        status.textContent = fretboardLabels.idle;
                    } else if (highlighted.length === 0) {
                        status.textContent = fretboardLabels.rest;
                    } else if (isPreview) {
                        status.textContent = fretboardLabels.preview;
                    } else if (chordInfo && chordInfo.name) {
                        // Tên hợp âm đã hiển thị trong SVG phía trên thế bấm.
                        status.textContent = '';
                    } else if (highlighted.length === 1) {
                        var single = highlighted[0];
                        var singleEffects = single.effects || {};
                        var singleLabel = singleEffects.dead
                            ? 'Dead note'
                            : (singleEffects.ghost ? 'Ghost ' + single.name : fretboardLabels.note + ' ' + single.name);
                        status.textContent = singleLabel + ' • ' +
                            fretboardLabels.string + ' ' + single.stringNumber + ' • ' +
                            fretboardLabels.fret + ' ' + single.fret;
                    } else {
                        // Nhiều nốt nhưng đang arpeggio hoặc không đủ chắc chắn để đặt tên hợp âm.
                        status.textContent = fretboardLabels.active + ' • ' +
                            highlighted.length + ' ' + fretboardLabels.notes;
                    }
                }

                function updateFretboardForBeat(beat) {
                    // playedBeatChanged emits Beat directly, but accept a wrapped event as
                    // a defensive fallback for older/custom alphaTab builds.
                    if (beat && !beat.notes && beat.beat) beat = beat.beat;
                    if (beat && !beatBelongsToSelectedTrack(beat, selectedTrackForNoteNames())) return;
                    fretboardLatestBeat = beat || null;
                    if (fretboardEnabled) {
                        scheduleFretboardRefresh();
                        requestAnimationFrame(function() {
                            keepScoreCursorNearFretboard(false);
                        });
                    }
                }

                function closeFretboardPanel() {
                    if (setStaveDisplayMode('scoretab') && window.AndroidInterface &&
                        typeof window.AndroidInterface.onFretboardClosed === 'function') {
                        window.AndroidInterface.onFretboardClosed();
                    }
                }

                function setStaveDisplayMode(mode) {
                    if (!api || !api.score || isPdfExportMode) return false;

                    var normalizedMode = String(mode || 'scoretab').toLowerCase();
                    if (
                        normalizedMode !== 'scoretab' &&
                        normalizedMode !== 'scoretab_fretboard' &&
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
                    var nextFretboardEnabled = normalizedMode === 'scoretab_fretboard';
                    var staveMode = nextFretboardEnabled ? 'scoretab' : normalizedMode;
                    var previousFretboardEnabled = fretboardEnabled;

                    try {
                        var wrapper = document.getElementById('scroll-wrapper');
                        var previousScrollTop = wrapper ? wrapper.scrollTop : 0;

                        if (
                            api.settings &&
                            typeof api.settings.fillFromJson === 'function'
                        ) {
                            api.settings.fillFromJson({
                                display: {
                                    staveProfile: staveMode,
                                    scale: nextFretboardEnabled ? 0.82 : 1.0,
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
                                staveMode === 'tab'
                                    ? alphaTab.StaveProfile.Tab
                                    : staveMode === 'score'
                                        ? alphaTab.StaveProfile.Score
                                        : alphaTab.StaveProfile.ScoreTab;
                            api.settings.display.scale = nextFretboardEnabled ? 0.82 : 1.0;
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

                        if (nextFretboardEnabled && !previousFretboardEnabled) {
                            fretboardWindowStart = 1;
                        }
                        fretboardEnabled = nextFretboardEnabled;
                        syncWebViewViewportBox(
                            lastAndroidViewportWidth,
                            lastAndroidViewportHeight
                        );
                        api.updateSettings();
                        applySelectedTrackSolo(true);
                        if (fretboardEnabled) {
                            notifyTabBannerVisibility(false);
                            scheduleFretboardRefresh();
                            setTimeout(function() {
                                keepScoreCursorNearFretboard(true);
                            }, 360);
                        } else {
                            notifyTabBannerVisibility(false);
                        }

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
                        fretboardEnabled = previousFretboardEnabled;
                        syncWebViewViewportBox(
                            lastAndroidViewportWidth,
                            lastAndroidViewportHeight
                        );
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
                    if (fretboardEnabled) scheduleFretboardRefresh();
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
                    // Đồng bộ màu RenderingResources trước khi score đầu tiên được render.
                    applyAlphaTabVisualResources(false);
                    setMetronomeEnabled(metronomeEnabled);

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
                            updateFretboardForBeat(null);
                        }
                        AndroidInterface.onPlayerStateChanged(lastKnownPlayerState);
                    });
                    if (api.playedBeatChanged && typeof api.playedBeatChanged.on === 'function') {
                        api.playedBeatChanged.on(function(beat) {
                            updateActiveNoteNameBeat(beat);
                            updateFretboardForBeat(beat);
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

                        if (fretboardEnabled) {
                            keepScoreCursorNearFretboard(false);
                            return;
                        }

                        var cursor = document.querySelector('.at-cursor-beat');
                        if (cursor) {
                            var rect = cursor.getBoundingClientRect();
                            var wrapperRect = wrapper.getBoundingClientRect();
                            var relativeTop = rect.top - wrapperRect.top;
                            var wrapperHeight = wrapper.clientHeight;

                            if (relativeTop > wrapperHeight * .60 ||
                                relativeTop < wrapperHeight * .10) {
                                var absoluteTop = wrapper.scrollTop + relativeTop;
                                wrapper.scrollTo({
                                    top: Math.max(0, absoluteTop - wrapperHeight * .30),
                                    behavior: 'smooth'
                                });
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
                            if (fretboardEnabled) {
                                scheduleFretboardRefresh();
                                setTimeout(function() {
                                    keepScoreCursorNearFretboard(false);
                                }, 70);
                            }

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
                        fretboardLatestBeat = null;
                        fretboardWindowStart = 1;
                        lastPlayedNoteNameBeatId = '';
                        clearNoteNameOverlay();

                        // Sửa metadata trống/hỏng bảng mã ngay trước lần render đầu.
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
                        if (fretboardEnabled) scheduleFretboardRefresh();
                        
                        var title = String(currentScoreTitle || score.title || 'Bản nhạc').trim();
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
                        fretboardLatestBeat = null;
                        fretboardWindowStart = 1;
                        currentTracks = [];
                        if (fretboardEnabled) scheduleFretboardRefresh();
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
                    fretboardLatestBeat = null;
                    fretboardWindowStart = 1;
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
                    updateFretboardForBeat(null);
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