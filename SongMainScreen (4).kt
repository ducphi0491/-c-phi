package ducphi.tunnerinstrument

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import android.util.Log
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
// --- BIẾN TOÀN CỤC ĐỂ LƯU TRẠNG THÁI XUYÊN SUỐT CÁC TAB ---
var globalLastCheckTime = 0L // Lưu mốc thời gian kiểm tra cuối cùng
var globalHasNewUpdate = false

// --- TRẠNG THÁI MÁY CHỦ BẢN NHẠC / GOOGLE DRIVE DỰ PHÒNG ---
private enum class GpServerHealthState {
    CHECKING,
    PRIMARY_OK,
    BACKUP_ONLY,
    BOTH_DOWN
}

private const val GP_PRIMARY_HEALTH_URL =
    "https://tunertools.top/api_gp_search.php?action=health"
private const val GP_BACKUP_HEALTH_URL =
    "https://tuner.alwaysdata.net/api_gp_drive.php?action=health"

// Google test banner ID used only while developing this screen.
// Replace this value with the real Song Detail banner ad unit ID before release.
private const val SONG_DETAIL_BANNER_AD_UNIT_ID =
    "ca-app-pub-3940256099942544/9214589741"

private object SongDetailBannerAdsRuntime {
    @Volatile
    var initialized: Boolean = false
}

private fun probeGpServer(urlString: String, requiredTrueFlag: String?): Boolean {
    val connection = (java.net.URL(urlString).openConnection() as java.net.HttpURLConnection)
    return try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 5_000
        connection.readTimeout = 7_000
        connection.useCaches = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Cache-Control", "no-cache")
        connection.setRequestProperty("User-Agent", "TunerTools-Android-HealthCheck")

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            false
        } else {
            val responseText = connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
                .trim()

            when {
                responseText.startsWith("{") -> {
                    val root = runCatching { org.json.JSONObject(responseText) }.getOrNull()
                        ?: return false
                    requiredTrueFlag == null || root.optBoolean(requiredTrueFlag, false)
                }
                responseText.startsWith("[") -> {
                    requiredTrueFlag == null &&
                            runCatching { org.json.JSONArray(responseText) }.isSuccess
                }
                else -> false
            }
        }
    } catch (_: Exception) {
        false
    } finally {
        connection.disconnect()
    }
}

private suspend fun checkGpServerHealth(): GpServerHealthState =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (probeGpServer(GP_PRIMARY_HEALTH_URL, requiredTrueFlag = "success")) {
            GpServerHealthState.PRIMARY_OK
        } else if (probeGpServer(GP_BACKUP_HEALTH_URL, requiredTrueFlag = "ok")) {
            GpServerHealthState.BACKUP_ONLY
        } else {
            GpServerHealthState.BOTH_DOWN
        }
    }

// --- DATA CLASS CHO TÍNH NĂNG BÁO CÁO ---
data class ReportInfo(val songId: String, val title: String, val singer: String, val composer: String)

// --- HÀM ĐỔI KÝ TỰ SANG CHUẨN ÂM NHẠC ---
fun formatChordForDisplay(chord: String): String {
    return chord.replace(Regex("(?<=[A-G0-9])b"), "♭")
        .replace(Regex("(?<=[A-G0-9])#"), "♯")
}

// --- HÀM TỰ ĐỘNG GOM DÒNG HỢP ÂM BỊ RỚT ---
fun formatRawLyrics(lyrics: String): String {
    if (lyrics.isBlank()) return ""
    var formatted = lyrics

    // 1. Kéo hợp âm lên nối vào cuối dòng chữ phía trước (Nếu có 1 dấu Enter)
    formatted = formatted.replace(Regex("""(?<!\n)\n[ \t]*(\[[^\]]+\])"""), " $1")

    // 2. Kéo chữ ở dòng dưới lên nối liền vào sau hợp âm
    formatted = formatted.replace(Regex("""(\[[^\]]+\])[ \t]*\n(?!\n)"""), "$1 ")

    return formatted.trim()
}

// --- HÀM GOM DÒNG LIỀN MẠCH KHI BẬT CHẾ ĐỘ CHỈ LỜI ---
fun formatCleanLyricsOnly(lyrics: String): String {
    if (lyrics.isBlank()) return ""
    // 1. Loại bỏ toàn bộ hợp âm [Am], [C], ...
    val noChords = lyrics.replace(Regex("\\[.*?\\]"), "")

    // 2. Tách theo các đoạn lớn (ngăn cách bằng 2 dấu Enter trở lên)
    val paragraphs = noChords.split(Regex("\\n{2,}"))

    return paragraphs.joinToString("\n\n") { paragraph ->
        paragraph.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ") // Nối các mảnh chữ bị rớt dòng thành câu dài hoàn chỉnh
    }.trim()
}

// --- HÀM TỰ ĐỘNG ĐỔI ĐẠI TỪ (ANH/EM) AN TOÀN ---
fun swapPronounsSafe(lyrics: String): String {
    if (lyrics.isBlank()) return ""
    val chordMap = mutableListOf<String>()
    var tempText = lyrics

    // 1. Bảo vệ hợp âm bằng cách thay thế tạm thời thành chuỗi mã hóa
    val chordRegex = Regex("\\[(.*?)\\]")
    tempText = chordRegex.replace(tempText) { matchResult ->
        chordMap.add(matchResult.value)
        "@@C${chordMap.size - 1}@@"
    }

    // 2. Đổi đại từ Anh <-> Em (Bảo toàn chữ hoa/chữ thường)
    tempText = tempText.replace(Regex("(?<!\\p{L})(Anh|anh|Em|em)(?!\\p{L})")) { match ->
        when (match.value) {
            "Anh" -> "Em"
            "anh" -> "em"
            "Em" -> "Anh"
            "em" -> "anh"
            else -> match.value
        }
    }

    // 3. Khôi phục lại hợp âm nguyên vẹn vào đúng vị trí
    chordMap.forEachIndexed { index, chord ->
        tempText = tempText.replace("@@C$index@@", chord)
    }

    return tempText
}

class ChordVisualTransformation(private val chordColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val annotatedString = buildAnnotatedString {
            val regex = Regex("\\[.*?\\]")
            var lastIndex = 0
            regex.findAll(rawText).forEach { matchResult ->
                withStyle(SpanStyle(color = Color.Unspecified)) { append(rawText.substring(lastIndex, matchResult.range.first)) }
                withStyle(SpanStyle(color = chordColor, fontWeight = FontWeight.Bold)) {
                    val chordInside = matchResult.value.substring(1, matchResult.value.length - 1)
                    append("[${formatChordForDisplay(chordInside)}]")
                }
                lastIndex = matchResult.range.last + 1
            }
            withStyle(SpanStyle(color = Color.Unspecified)) { append(rawText.substring(lastIndex)) }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

// --- KHÓA SOL (TREBLE CLEF) CHO NÚT SHEET NHẠC ĐÃ XỬ LÝ VƯỢT KHUNG ---
@Composable
fun SheetMusicIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        // Vẽ 5 dòng kẻ (chiếm 55% chiều cao của Box để có chỗ thò lên/xuống)
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f)) {
            val w = size.width
            val h = size.height
            val spacing = h / 4
            for (i in 0..4) {
                val y = spacing * i
                drawLine(color = tint, start = Offset(0f, y), end = Offset(w, y), strokeWidth = maxOf(1f, w * 0.05f))
            }
        }
        // unbounded = true giúp chữ Khóa Sol to ra thoải mái mà không làm phình cái nút bấm
        Box(modifier = Modifier.wrapContentSize(unbounded = true)) {
            Text(
                text = "𝄞",
                color = tint,
                fontSize = 12.sp, // Đủ to để hiện rõ nét móc câu phía dưới
                fontWeight = FontWeight.Normal,
                modifier = Modifier.offset(x = 0.dp, y = (1).dp)
            )
        }
    }
}

@Composable
fun ZingMp3Icon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; if (w <= 0f || h <= 0f) return@Canvas
        val center = Offset(w / 2, h / 2); val radius = w * 0.35f
        drawRoundRect(color = Color(0xFF7B1FA2), size = Size(w, h), cornerRadius = CornerRadius(w * 0.25f))
        drawCircle(color = Color.White, radius = radius + (w * 0.04f), center = center)
        val arcSize = Size(radius * 2, radius * 2); val arcTopLeft = Offset(center.x - radius, center.y - radius)
        drawArc(color = Color(0xFF00B0FF), startAngle = 180f, sweepAngle = 90f, useCenter = true, topLeft = arcTopLeft, size = arcSize)
        drawArc(color = Color(0xFF00E676), startAngle = 270f, sweepAngle = 90f, useCenter = true, topLeft = arcTopLeft, size = arcSize)
        drawArc(color = Color(0xFFFF9100), startAngle = 0f, sweepAngle = 90f, useCenter = true, topLeft = arcTopLeft, size = arcSize)
        drawArc(color = Color(0xFFF50057), startAngle = 90f, sweepAngle = 90f, useCenter = true, topLeft = arcTopLeft, size = arcSize)
        drawCircle(color = Color.White, radius = radius * 0.35f, center = center)
        drawCircle(color = Color(0xFF7B1FA2), radius = radius * 0.15f, center = center)
    }
}
@Composable
fun AppleMusicIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Nền bo góc màu đỏ của Apple Music
        drawRoundRect(
            color = Color(0xFFFA243C),
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.25f)
        )

        val noteColor = Color.White
        // ĐÃ SỬA: Giảm bán kính nốt nhạc xuống 0.12f để tròn trịa và thanh thoát hơn
        val radius = w * 0.12f
        val stemWidth = w * 0.08f

        // Tâm 2 nốt nhạc (Tinh chỉnh lại cho cân đối với nốt nhỏ hơn)
        val leftCenter = Offset(w * 0.32f, h * 0.68f)
        val rightCenter = Offset(w * 0.68f, h * 0.58f)

        // Vẽ 2 nốt nhạc (hình tròn)
        drawCircle(color = noteColor, radius = radius, center = leftCenter)
        drawCircle(color = noteColor, radius = radius, center = rightCenter)

        // Tính toán vị trí cột dọc nằm bên PHẢI nốt nhạc
        val leftStemX = leftCenter.x + radius - stemWidth
        val rightStemX = rightCenter.x + radius - stemWidth

        // Vẽ cột trái
        drawRect(
            color = noteColor,
            topLeft = Offset(leftStemX, h * 0.35f),
            size = Size(stemWidth, leftCenter.y - h * 0.35f)
        )

        // Vẽ cột phải
        drawRect(
            color = noteColor,
            topLeft = Offset(rightStemX, h * 0.25f),
            size = Size(stemWidth, rightCenter.y - h * 0.25f)
        )

        // Vẽ thanh nối (Beam) chéo phía trên nối 2 cột
        val beamPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(leftStemX, h * 0.28f) // Góc trên trái
            lineTo(rightStemX + stemWidth, h * 0.18f) // Góc trên phải
            lineTo(rightStemX + stemWidth, h * 0.32f) // Góc dưới phải
            lineTo(leftStemX, h * 0.42f) // Góc dưới trái
            close()
        }
        drawPath(path = beamPath, color = noteColor)
    }
}

@Composable
fun SpotifyIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height; if (w <= 0f || h <= 0f) return@Canvas
        drawCircle(color = Color(0xFF1DB954), radius = w / 2, center = Offset(w / 2, h / 2))
        val arcColor = Color(0xFF191414)
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.25f, h * 0.35f); quadraticBezierTo(w * 0.5f, h * 0.2f, w * 0.75f, h * 0.35f) },
            color = arcColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.3f, h * 0.5f); quadraticBezierTo(w * 0.5f, h * 0.38f, w * 0.7f, h * 0.5f) },
            color = arcColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.35f, h * 0.65f); quadraticBezierTo(w * 0.5f, h * 0.56f, w * 0.65f, h * 0.65f) },
            color = arcColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

// ===================================================================
// HÀM PHỤ TRỢ: TẨY DẤU TIẾNG VIỆT VÀ KÝ TỰ ĐẶC BIỆT
// ===================================================================
// ===================================================================
// HÀM PHỤ TRỢ: TẨY DẤU TIẾNG VIỆT VÀ KÝ TỰ ĐẶC BIỆT
// ===================================================================
fun removeAccentsAndSpecialChars(input: String): String {
    // 1. Đổi chữ Đ/đ
    var str = input.replace("Đ", "D").replace("đ", "d")

    // 2. Tách các dấu thanh ra khỏi chữ cái
    str = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
    val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    str = pattern.matcher(str).replaceAll("")

    // 3. Xóa mọi ký tự đặc biệt, chỉ giữ lại chữ cái, số và khoảng trắng
    str = str.replace(Regex("[^a-zA-Z0-9 ]"), " ")

    return str.replace(Regex("\\s+"), " ").trim()
}


// ===================================================================
// HÀM DÙNG CHUNG ĐỂ TÌM KIẾM NHẠC TRÊN CÁC NỀN TẢNG (CÓ LOGCAT)
// ===================================================================
fun searchMusicOnApp(context: Context, appType: String, songTitle: String, singer: String, currentLang: String) {

    // NẾU LÀ APPLE HOẶC SPOTIFY -> TẨY DẤU TIẾNG VIỆT TRƯỚC KHI TÌM KIẾM
    val finalTitle = if (appType == "APPLE" || appType == "SPOTIFY") removeAccentsAndSpecialChars(songTitle) else songTitle
    val finalSinger = if (appType == "APPLE" || appType == "SPOTIFY") removeAccentsAndSpecialChars(singer) else singer

    val searchQuery = "$finalTitle $finalSinger".trim()
    Log.d("MusicSearch", "[$appType] 1. Từ khóa gốc: $searchQuery") // Ghi log 1

    var encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
    if (appType == "APPLE" || appType == "SPOTIFY") {
        encodedQuery = encodedQuery.replace("+", "%20")
    }
    Log.d("MusicSearch", "[$appType] 2. Từ khóa đã mã hóa URL: $encodedQuery") // Ghi log 2

    // Dùng mẹo tách chuỗi (+) để link thật không bị AI tự động đánh tráo
// Dùng mẹo tách chuỗi (+) để link thật không bị AI tự động đánh tráo
    val (url, packageName) = when (appType) {
        "ZING" -> Pair(
            "https://zingmp3.vn/tim-kiem/tat-ca?q=$encodedQuery",
            "com.zing.mp3"
        )
        "APPLE" -> Pair(
            "https://music.apple.com/us/search?term=$encodedQuery",
            "com.apple.android.music"
        )
        "SPOTIFY" -> {
            // Dùng URI Scheme để ra lệnh trực tiếp cho hệ điều hành mở app
            val spotUri = "spotify:search:$encodedQuery"
            val spotPkg = "com.spotify.music"
            Pair(spotUri, spotPkg)
        }
        else -> Pair("", "")
    }

    Log.d("MusicSearch", "[$appType] 3. URL Đích: $url") // Ghi log 3
    Log.d("MusicSearch", "[$appType] 4. Package Đích: $packageName") // Ghi log 4

    if (url.isBlank()) return

    try {
        Log.d("MusicSearch", "[$appType] 5. Đang thử mở bằng Ứng dụng (App)...")
        val intent = if (appType == "ZING") {
            // Sử dụng intent phát nhạc từ thanh tìm kiếm giúp ứng dụng Zing MP3 tự động chọn và phát bài hát đầu tiên
            Intent("android.media.action.MEDIA_PLAY_FROM_SEARCH").apply {
                setPackage(packageName)
                putExtra("android.intent.extra.focus", "vnd.android.cursor.item/*")
                putExtra("android.intent.extra.title", songTitle)
                putExtra("android.intent.extra.artist", singer)
                putExtra("query", searchQuery)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                if (appType != "SPOTIFY") {
                    setPackage(packageName)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        context.startActivity(intent)
        Log.d("MusicSearch", "[$appType] -> Thành công! Đã mở App.")

    } catch (e: Exception) {
        Log.e("MusicSearch", "[$appType] -> Lỗi: Không tìm thấy App. Exception: ${e.message}")

        // CƠ CHẾ DỰ PHÒNG CHUYÊN NGHIỆP
        if (appType == "SPOTIFY") {
            Log.d("MusicSearch", "[$appType] Chuyển hướng sang Google Play Store...")
            try {
                // Mở thẳng trang cài đặt Spotify trên CH Play
                val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(storeIntent)
                Toast.makeText(context, CoreTranslator.getString("spotify_install_req", currentLang), Toast.LENGTH_SHORT).show()            } catch (ex: Exception) {
                // Trường hợp máy không có CH Play (ví dụ: Huawei), đành mở bằng web
                val webUrl = "https://open.spotify.com/search/$encodedQuery"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(webIntent)
            }
        } else {
            Log.d("MusicSearch", "[$appType] 6. Chuyển sang mở bằng Trình duyệt Web...")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
        }
    }
}

// ===================================================================
// SEARCH HELPERS - tách khỏi SongTabScreen để tránh JVM MethodTooLarge
// ===================================================================
private fun openFavoriteSongsFromSearch(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    currentUserEmail: String,
    sharedPref: android.content.SharedPreferences,
    songRepository: SongRepository,
    onResetScreen: () -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onResultsChanged: (List<SearchResultItem>) -> Unit
) {
    onResetScreen()
    onLoadingChanged(true)

    coroutineScope.launch {
        try {
            val result = if (currentUserEmail.isNotBlank()) {
                // Có tài khoản: ưu tiên đồng bộ danh sách ID từ server.
                val onlineIds = SongScraper.getFavoriteIdsOnline(currentUserEmail)

                if (onlineIds != null) {
                    val editor = sharedPref.edit()
                    sharedPref.all.keys
                        .filter { it.startsWith("liked_") }
                        .forEach { editor.remove(it) }

                    onlineIds.forEach { id ->
                        editor.putBoolean("liked_$id", true)
                    }
                    editor.apply()

                    if (onlineIds.isNotEmpty()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            songRepository.getSongsByIdsLocal(onlineIds)
                        }
                    } else {
                        emptyList()
                    }
                } else {
                    // Mất mạng: dùng trạng thái yêu thích đang lưu trên thiết bị.
                    val localIds = sharedPref.all
                        .filter {
                            it.key.startsWith("liked_") &&
                                    (it.value as? Boolean) == true
                        }
                        .map { it.key.removePrefix("liked_") }

                    if (localIds.isNotEmpty()) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            songRepository.getSongsByIdsLocal(localIds)
                        }
                    } else {
                        emptyList()
                    }
                }
            } else {
                Toast.makeText(
                    context,
                    "Vui lòng đăng nhập Gmail để đồng bộ Yêu thích",
                    Toast.LENGTH_SHORT
                ).show()

                val localIds = sharedPref.all
                    .filter {
                        it.key.startsWith("liked_") &&
                                (it.value as? Boolean) == true
                    }
                    .map { it.key.removePrefix("liked_") }

                if (localIds.isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        songRepository.getSongsByIdsLocal(localIds)
                    }
                } else {
                    emptyList()
                }
            }

            onResultsChanged(result)
        } finally {
            onLoadingChanged(false)
        }
    }
}

private fun toggleSongVoiceSearch(
    currentLang: String,
    speechRecognizer: SpeechRecognizer,
    isListening: Boolean,
    speechRecognizerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    onListeningChanged: (Boolean) -> Unit,
    onInputChanged: (TextFieldValue) -> Unit
) {
    if (isListening) {
        speechRecognizer.stopListening()
        onListeningChanged(false)
        return
    }

    val speechLang = if (currentLang == "vi") "vi-VN" else currentLang

    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.startListening(intent)
        onListeningChanged(true)
        onInputChanged(TextFieldValue(""))
    } catch (_: Exception) {
        val fallbackIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLang)
        }
        speechRecognizerLauncher.launch(fallbackIntent)
    }
}

@Composable
private fun OfflineSearchDebounceEffect(
    inputText: String,
    isSearchFocused: Boolean,
    hasCurrentSong: Boolean,
    isBroadSearch: Boolean,
    offlineSearchQuery: String,
    onBroadSearchChanged: (Boolean) -> Unit,
    onOfflineQueryChanged: (String) -> Unit
) {
    val latestInputText by rememberUpdatedState(inputText)
    val latestHasCurrentSong by rememberUpdatedState(hasCurrentSong)
    val latestBroadSearch by rememberUpdatedState(isBroadSearch)
    val latestOfflineSearchQuery by rememberUpdatedState(offlineSearchQuery)

    LaunchedEffect(inputText, isSearchFocused) {
        val query = inputText.trim()

        if (query.isBlank()) {
            onBroadSearchChanged(false)
            onOfflineQueryChanged("")
            return@LaunchedEffect
        }

        // performSearch() vừa gửi đúng truy vấn này thì không chạy lại tìm nhanh.
        if (latestBroadSearch && latestOfflineSearchQuery == query) {
            return@LaunchedEffect
        }

        onBroadSearchChanged(false)

        // Không quét database với một ký tự.
        if (query.length < 2) {
            onOfflineQueryChanged("")
            return@LaunchedEffect
        }

        // Ở trang chi tiết, chỉ tìm khi người dùng thực sự focus vào input.
        if (latestHasCurrentSong && !isSearchFocused) {
            return@LaunchedEffect
        }

        delay(350)

        // Tránh ghi đè một tìm kiếm rộng vừa được gửi trong lúc debounce.
        if (latestBroadSearch && latestOfflineSearchQuery == query) {
            return@LaunchedEffect
        }
        if (latestInputText.trim() != query) {
            return@LaunchedEffect
        }
        if (latestHasCurrentSong && !isSearchFocused) {
            return@LaunchedEffect
        }

        onOfflineQueryChanged(query)
    }
}

@Composable
private fun SongSearchInputField(
    value: TextFieldValue,
    currentLang: String,
    isFocused: Boolean,
    isListening: Boolean,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onValueChange: (TextFieldValue) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onOpenFavorites: () -> Unit,
    onToggleVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(40.dp)
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) {
                    Color.White
                } else {
                    Color.Gray.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focusState ->
                onFocusChanged(focusState.isFocused)
            }
            .padding(horizontal = 12.dp),
        textStyle = TextStyle(color = textColor, fontSize = 14.sp),
        singleLine = true,
        cursorBrush = SolidColor(tunaGreen),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = CoreTranslator.getString("search_hint", currentLang),
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }

                if (value.text.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Xóa",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    IconButton(
                        onClick = onOpenFavorites,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorites",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onToggleVoiceSearch,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Giọng nói",
                        tint = if (isListening) Color.Red else tunaGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SongDetailBottomBannerAd(
    currentLang: String,
    surfaceColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = textColor.copy(alpha = 0.10f),
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Text(
            text = if (currentLang == "vi") "Quảng cáo" else "Advertisement",
            color = textColor.copy(alpha = 0.46f),
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val adWidthDp = maxWidth.value
                .roundToInt()
                .coerceAtLeast(1)

            val adSize = remember(context, adWidthDp) {
                com.google.android.gms.ads.AdSize
                    .getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        context,
                        adWidthDp
                    )
            }

            val adHeightDp = remember(context, adSize, density) {
                with(density) {
                    adSize.getHeightInPixels(context).toDp()
                }.let { measuredHeight ->
                    if (measuredHeight.value > 0f) measuredHeight else 50.dp
                }
            }

            var adLoaded by remember(adWidthDp) { mutableStateOf(false) }
            var adLoadFailed by remember(adWidthDp) { mutableStateOf(false) }
            val loadRequested = remember(adWidthDp) {
                java.util.concurrent.atomic.AtomicBoolean(false)
            }

            val adView = remember(context, adWidthDp) {
                com.google.android.gms.ads.AdView(context).apply {
                    adUnitId = SONG_DETAIL_BANNER_AD_UNIT_ID
                    setAdSize(adSize)
                }
            }

            DisposableEffect(adView) {
                adView.adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        adLoaded = true
                        adLoadFailed = false
                    }

                    override fun onAdFailedToLoad(
                        adError: com.google.android.gms.ads.LoadAdError
                    ) {
                        adLoaded = false
                        adLoadFailed = true
                        Log.w(
                            "SongDetailBanner",
                            "Banner failed to load: ${adError.code} ${adError.message}"
                        )
                    }
                }

                onDispose {
                    adView.adListener = null
                    adView.destroy()
                }
            }

            LaunchedEffect(adView) {
                fun requestBanner() {
                    if (loadRequested.compareAndSet(false, true)) {
                        adView.loadAd(
                            com.google.android.gms.ads.AdRequest.Builder().build()
                        )
                    }
                }

                if (SongDetailBannerAdsRuntime.initialized) {
                    requestBanner()
                } else {
                    com.google.android.gms.ads.MobileAds.initialize(
                        context.applicationContext
                    ) {
                        SongDetailBannerAdsRuntime.initialized = true
                        adView.post { requestBanner() }
                    }
                }
            }

            if (!adLoadFailed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(adHeightDp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(surfaceColor.copy(alpha = 0.58f)),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { adView },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(adHeightDp)
                    )

                    if (!adLoaded) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = textColor.copy(alpha = 0.45f),
                            strokeWidth = 1.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongTabScreen(
    currentLang: String = "vi",
    bgColor: Color,
    surfaceColor: Color,
    isDark: Boolean,
    textColor: Color,
    tunaGreen: Color,
    activeTutorialStep: TutorialStep = TutorialStep.NONE, // THÊM DÒNG NÀY
    onTutorialDismiss: () -> Unit = {},
    onNavigateToChords: () -> Unit = {},
    onChordClicked: (String) -> Unit = {},
    showSupportChatIcon: Boolean = false,
    supportChatBadgeCount: Int = 0,
    onSupportChatClick: () -> Unit = {},
    initialDbInstallResult: DbBundleInstallResult? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // TỰ ĐỘNG ẨN THANH TRẠNG THÁI (PIN, SÓNG) TẠI TRANG CHỦ
    val currentView = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, currentView)
            insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

// --- BẮT ĐẦU: DATABASE OFFLINE ---
    // Bình thường MainActivity đã prepare database hoàn tất trong Splash và truyền kết quả xuống đây.
    // Vì vậy khi mở tab Bản nhạc sẽ KHÔNG xuất hiện loader thứ hai.
    // Nhánh prepare tại đây chỉ còn là dự phòng cho nơi nào gọi SongTabScreen độc lập
    // hoặc khi người dùng bấm "Thử lại" sau một lỗi cài database.
    var dbInstallRetry by remember { mutableIntStateOf(0) }
    var dbInstallResult by remember(initialDbInstallResult) {
        mutableStateOf(initialDbInstallResult)
    }

    LaunchedEffect(dbInstallRetry, initialDbInstallResult) {
        val needsPrepare = dbInstallResult == null || dbInstallRetry > 0
        if (needsPrepare) {
            dbInstallResult = SongRepository.prepareDatabases(context)
        }
    }

    val currentDbInstallResult = dbInstallResult
    if (currentDbInstallResult == null) {
        // Chỉ có thể rơi vào đây nếu SongTabScreen được gọi từ một nơi khác mà không
        // truyền kết quả startup. Luồng mở app chuẩn không còn đi qua loader này.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = tunaGreen)
        }
        return
    }

    if (!currentDbInstallResult.success) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (currentLang == "vi") {
                        "Kh\u00f4ng th\u1ec3 c\u1eadp nh\u1eadt d\u1eef li\u1ec7u"
                    } else {
                        "Cannot update song data"
                    },
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDbInstallResult.message,
                    color = textColor,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        dbInstallResult = null
                        dbInstallRetry++
                    }
                ) {
                    Text(if (currentLang == "vi") "Th\u1eed l\u1ea1i" else "Retry")
                }
            }
        }
        return
    }

    val songRepository = remember(currentDbInstallResult.songCount) {
        SongRepository(context)
    }

    // Một pipeline tìm kiếm duy nhất cho cả trang chính và trang chi tiết.
    var offlineSearchQuery by remember { mutableStateOf("") }
    var isBroadSearch by remember { mutableStateOf(false) }
    var isSearchingOffline by remember { mutableStateOf(false) }
    var isSearchTimeout by remember { mutableStateOf(false) }
    var isArtistFilter by remember { mutableStateOf(false) } // <-- THÊM CỜ NÀY

    val offlineSongs by remember(offlineSearchQuery, isBroadSearch, isArtistFilter) {
        if (offlineSearchQuery.isBlank()) {
            isSearchingOffline = false
            kotlinx.coroutines.flow.flowOf<List<Song>>(emptyList())
        } else {
            isSearchingOffline = true
            songRepository.searchOfflineSongs(
                query = offlineSearchQuery,
                limit = if (isArtistFilter) -1 else (if (isBroadSearch) 50 else 15), // Nếu là click ca sĩ -> lấy toàn bộ (-1), tìm kiếm thường -> giữ nguyên 50 / 15
                broadSearch = isBroadSearch
            )
        }
    }.collectAsState(initial = emptyList<Song>())

    // Duy trì icon loading trong 20s nếu chưa có kết quả, sau 20s mới báo không tìm thấy
    LaunchedEffect(offlineSearchQuery) {
        if (offlineSearchQuery.isNotBlank()) {
            isSearchingOffline = true
            isSearchTimeout = false
            delay(20000L)
            isSearchTimeout = true
        } else {
            isSearchingOffline = false
            isSearchTimeout = false
        }
    }

    LaunchedEffect(offlineSongs) {
        if (offlineSongs.isNotEmpty()) {
            isSearchingOffline = false
        }
    }

    var isSyncing by remember { mutableStateOf(false) }
    var syncIconColor by remember { mutableStateOf(tunaGreen) }
    var syncErrorCode by remember { mutableStateOf("") }

    // Kiểm tra riêng máy chủ Bản nhạc. Khi máy chủ chính lỗi nhưng Alwaysdata/Drive
    // vẫn hoạt động, chấm trạng thái chuyển xanh dương. Khi máy chủ chính phục hồi,
    // lần kiểm tra kế tiếp tự chuyển lại màu xanh lá.
    var gpServerHealthState by remember {
        mutableStateOf(GpServerHealthState.CHECKING)
    }

    LaunchedEffect(Unit) {
        while (true) {
            val nextState = checkGpServerHealth()
            gpServerHealthState = nextState

            delay(
                when (nextState) {
                    GpServerHealthState.PRIMARY_OK -> 30_000L
                    GpServerHealthState.BACKUP_ONLY -> 15_000L
                    GpServerHealthState.BOTH_DOWN -> 10_000L
                    GpServerHealthState.CHECKING -> 10_000L
                }
            )
        }
    }

    val gpServerIndicatorColor = when (gpServerHealthState) {
        GpServerHealthState.PRIMARY_OK -> Color(0xFF00C853)
        GpServerHealthState.BACKUP_ONLY -> Color(0xFF2196F3)
        GpServerHealthState.BOTH_DOWN -> Color(0xFFE53935)
        GpServerHealthState.CHECKING -> Color(0xFF9E9E9E)
    }

// --- BỔ SUNG CHO TÍNH NĂNG THU HÚT SỰ CHÚ Ý (KHÔNG SỐ) ---
    var hasNewUpdate by remember { mutableStateOf(globalHasNewUpdate) }

// Biến cò súng: Mỗi lần biến này thay đổi số, giao diện sẽ tự động quét lại SQLite
    var dbRefreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val currentTime = System.currentTimeMillis()
        // Chỉ quét API lại nếu đã trôi qua ít nhất 3 phút (180,000 milliseconds) kể từ lần cuối
        if (currentTime - globalLastCheckTime > 180000) {
            val updateAvailable = songRepository.checkHasUpdate()
            globalLastCheckTime = currentTime

            // Nếu có bản cập nhật, tự động đồng bộ ngay lập tức
            if (updateAvailable && !isSyncing) {
                isSyncing = true
                hasNewUpdate = false
                globalHasNewUpdate = false
                syncIconColor = tunaGreen
                syncErrorCode = ""

                val message = songRepository.syncNewSongsOnline()

                // THÊM ĐỘ TRỄ NHÂN TẠO 1.5 GIÂY ĐỂ NGƯỜI DÙNG KỊP NHÌN THẤY VÒNG XOAY ĐỒNG BỘ
                delay(1500)

                if (message.contains("Lỗi máy chủ")) {
                    syncIconColor = Color.Red
                    syncErrorCode = message.substringAfter("Lỗi máy chủ: ").trim()
                } else if (message.contains("Lỗi mạng")) {
                    syncIconColor = Color.Red
                    syncErrorCode = "ERR"
                } else {
                    syncIconColor = tunaGreen
                    syncErrorCode = ""
                    dbRefreshTrigger++ // Load lại danh sách
                }

                isSyncing = false

                if (syncIconColor == Color.Red) {
                    delay(3000)
                    syncIconColor = tunaGreen
                    syncErrorCode = ""
                }
            } else {
                hasNewUpdate = false
                globalHasNewUpdate = false
            }
        } else {
            hasNewUpdate = globalHasNewUpdate
        }
    }
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "attention")

    // 1. Hiệu ứng xoay tròn đều (360 độ) chầm chậm cho Mũi tên Reload
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f, // Đổi thành 360 độ (1 vòng trọn vẹn)
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(2500, easing = androidx.compose.animation.core.LinearEasing), // LinearEasing giúp xoay đều, 2500ms giúp xoay rất chậm
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ), label = "iconRotation"
    )

    // 2. Hiệu ứng nhịp đập mờ nhạt (Pulse) cho chấm đỏ góc phải
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "pulseAlpha"
    )
    // --- KẾT THÚC: KHAI BÁO DATABASE OFFLINE ---
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown_device"
    }
    val sharedPref = context.getSharedPreferences("LikedSongsPref", Context.MODE_PRIVATE)
    val historyPref =
        remember { context.getSharedPreferences("SearchHistory", Context.MODE_PRIVATE) }
    var searchHistory by remember {
        mutableStateOf(
            historyPref.getStringSet("history", emptySet())?.toList() ?: emptyList()
        )
    }
    var isTrendingExpanded by remember { mutableStateOf(false) }

    val addToHistory: (String) -> Unit = { newEntry ->
        if (newEntry.isNotBlank()) {
            // 1. Tách lấy tên bài hát của mục mới (bỏ qua ID nếu có)
            val newTitle = if (newEntry.contains("|")) newEntry.substringAfter("|") else newEntry

            // 2. Lọc bỏ các mục cũ có CÙNG TÊN (Không phân biệt hoa/thường, không quan tâm ID cũ là gì)
            val filteredHistory = searchHistory.filter { oldEntry ->
                val oldTitle = if (oldEntry.contains("|")) oldEntry.substringAfter("|") else oldEntry
                !oldTitle.equals(newTitle, ignoreCase = true)
            }

            // 3. Đưa mục mới lên đầu và lưu lại (giới hạn 30 mục)
            val newHistory = (listOf(newEntry) + filteredHistory).take(30)
            searchHistory = newHistory
            historyPref.edit().putStringSet("history", newHistory.toSet()).apply()
        }
    }
// --- KIỂM TRA QUYỀN ADMIN ĐỘNG TỪ DATABASE ---
    val account =
        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
    val currentUserEmail = account?.email ?: ""
    var userRole by remember { mutableStateOf("user") }
    val isStaff = userRole in listOf("super_admin", "admin", "mod")

    LaunchedEffect(currentUserEmail) {
        if (currentUserEmail.isNotEmpty()) {
            userRole = SongScraper.getUserRole(currentUserEmail)
        } else {
            userRole = "user"
        }
    }
    // ---------------------------------------------

    // 1. ĐƯA TẤT CẢ KHAI BÁO BIẾN LÊN TRƯỚC
    var urlInput by remember { mutableStateOf(TextFieldValue("")) }
    var isSearchFocused by remember { mutableStateOf(false) } // Thêm biến này để theo dõi click
    var searchResults by remember { mutableStateOf<List<SearchResultItem>?>(null) }
    var isSearchingList by remember { mutableStateOf(false) }
    var currentSongData by remember { mutableStateOf<SongDetails?>(null) }
    var isFetchingDetail by remember { mutableStateOf(false) }
    var imageCacheBuster by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRomanNumerals by remember { mutableStateOf(false) }

    var customChordColor by remember { mutableStateOf(tunaGreen) }
    var selectedDisplayMode by remember { mutableStateOf("CHORD") }
    var isFullScreen by remember { mutableStateOf(false) }
    var showChordAI by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingSongData by remember { mutableStateOf<SongDetails?>(null) }
    var customTemplateSongData by remember { mutableStateOf<SongDetails?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var reportInfo by remember { mutableStateOf<ReportInfo?>(null) }
    var showAlphaTabScreen by remember { mutableStateOf(false) }

    // BIẾN QUẢN LÝ TÍNH NĂNG ADMIN ĐĂNG THÔNG BÁO
    var showAdminActionSelector by remember { mutableStateOf(false) }
    var showPostNotificationDialog by remember { mutableStateOf(false) }

    // === ĐOẠN THÊM MỚI (BƯỚC 3.2) ===
    var showFloatingCamera by remember { mutableStateOf(false) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var showStrummingDialog by remember { mutableStateOf(false) }
    var showSongToneDialog by remember { mutableStateOf(false) }
    var tutorialVideoUrl by remember { mutableStateOf("") }
    var showPlayModeDialog by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) } // <-- BIẾN MỚI
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] == true
            val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
            if (cameraGranted && audioGranted) {
                showFloatingCamera = true
            } else {
                Toast.makeText(
                    context,
                    CoreTranslator.getString("camera_mic_permission_req", currentLang),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    // ================================

    var transposeOffset by remember { mutableStateOf(0) }
    var isAutoScrolling by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableFloatStateOf(1f) }
    var fontSizeBody by remember { mutableIntStateOf(14) }
    var popupChord by remember { mutableStateOf<String?>(null) }
    var isDoNotDisturbMode by remember { mutableStateOf(false) } // BIẾN TRẠNG THÁI KHÔNG LÀM PHIỀN

    // === THÊM BIẾN CHO TÍNH NĂNG GÕ NHỊP NÂNG CAO ===
    var isFootTapping by remember { mutableStateOf(false) }
    var metronomeTick by remember { mutableIntStateOf(-1) }
    var showBpmSettingsDialog by remember { mutableStateOf(false) }
    var isEasyChordMode by remember { mutableStateOf(false) } // BIẾN TRẠNG THÁI HỢP ÂM DỄ
    // Biến lưu tốc độ BPM có thể thay đổi được (lấy mặc định từ bài hát)
    var adjustableBpm by remember(currentSongData) {
        mutableFloatStateOf(currentSongData?.bpm?.toFloatOrNull() ?: 120f)
    }

    // THÊM MỚI: Biến lưu nhịp điệu và phách hiện tại
    var selectedTimeSignature by remember { mutableStateOf("4/4") }
    var currentBeat by remember { mutableIntStateOf(0) }

    // BỔ SUNG SOUNDPOOL CHO TIẾNG GÕ NHỊP
    val tapSoundPool = remember {
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        android.media.SoundPool.Builder().setMaxStreams(2).setAudioAttributes(audioAttributes).build()
    }
    // SỬA: Thay thế 1 âm thanh thành 2 âm thanh (Mạnh & Yếu)
    val weakTapSoundId = remember { tapSoundPool.load(context, R.raw.metronome_weak, 1) }
    val strongTapSoundId = remember { tapSoundPool.load(context, R.raw.metronome_strong, 1) }

    DisposableEffect(Unit) {
        onDispose { tapSoundPool.release() }
    }

    // Logic chạy nhịp (BPM) tự động cho Bàn chân
    LaunchedEffect(isFootTapping, adjustableBpm, selectedTimeSignature) {
        if (isFootTapping && adjustableBpm > 0) {
            val delayMillis = (60000f / adjustableBpm).toLong()
            val beatsPerMeasure = selectedTimeSignature.split("/")[0].toInt()
            currentBeat = 0 // Reset phách về 0 khi bắt đầu

            while (true) {
                metronomeTick = 1 // Kích hoạt bàn chân nảy mạnh

                // Lựa chọn âm thanh: Phách đầu tiên (0) gõ mạnh, còn lại gõ yếu
                val soundToPlay = if (currentBeat == 0 && beatsPerMeasure > 1) strongTapSoundId else weakTapSoundId
                tapSoundPool.play(soundToPlay, 1f, 1f, 1, 0, 1f)

                delay(100)
                metronomeTick = -1 // Thả bàn chân về
                val timeToSleep = delayMillis - 100
                if (timeToSleep > 0) delay(timeToSleep)

                // Tăng phách lên 1, quay về 0 nếu hết nhịp
                currentBeat = (currentBeat + 1) % beatsPerMeasure
            }
        } else {
            metronomeTick = -1
            currentBeat = 0
        }
    }
    // =====================================

    var isChordOverText by remember { mutableStateOf(false) }
    var isLyricsOnly by remember { mutableStateOf(false) }
    var activeMediaType by remember { mutableStateOf("") }
    var isPronounSwapped by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<android.webkit.WebView?>(null) }
    var customVideoView by remember { mutableStateOf<android.view.View?>(null) }
    var customVideoCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    // --- BIẾN TRẠNG THÁI CHO KARAOKE AI ---
    var isAiAnalyzing by remember { mutableStateOf(false) }
    var isFastLoadingCache by remember { mutableStateOf(false) } // Thêm cờ này
    var aiAnalysisMode by remember { mutableStateOf("") } // <-- THÊM MỚI QUẢN LÝ CHẾ ĐỘ
    var aiAnalysisStatus by remember { mutableStateOf("") }
    var aiProgress by remember { mutableFloatStateOf(0f) }
    var syncedKaraokeLines by remember { mutableStateOf<List<SyncedDbLine>?>(null) }

    // Tự động in log ra khi bất kỳ nhánh nào (Zing/Youtube/Offline) xử lý xong dữ liệu
    LaunchedEffect(syncedKaraokeLines) {
        syncedKaraokeLines?.let { lines ->
            android.util.Log.d("KaraokeLog_FullSong", "=== BẮT ĐẦU DỮ LIỆU TOÀN BÀI ===")
            lines.forEachIndexed { index, line ->
                // 1. In thời gian tổng của cả câu
                android.util.Log.d(
                    "KaraokeLog_FullSong",
                    "[Dòng ${index + 1}] ${line.originalTextWithChords} | Tổng: ${line.startTimeMs}ms -> ${line.endTimeMs}ms"
                )

                // 2. Bóc tách và in thời gian của TỪNG CHỮ để so sánh trực tiếp với JSON
                if (line.words.isNotEmpty()) {
                    val wordDetails = line.words.joinToString("  |  ") { w ->
                        "${w.word} (${w.startTimeMs} -> ${w.endTimeMs})"
                    }
                    android.util.Log.d("KaraokeLog_FullSong", "   ↳ Từng chữ: $wordDetails")
                } else {
                    android.util.Log.d("KaraokeLog_FullSong", "   ↳ Từng chữ: [Rỗng - Hệ thống nội suy tự động]")
                }
            }
            android.util.Log.d("KaraokeLog_FullSong", "=== KẾT THÚC DỮ LIỆU TOÀN BÀI ===")
        }
    }

    var currentVideoTimeMs by remember { mutableLongStateOf(0L) }
    var syncOffsetMs by remember { mutableLongStateOf(0L) } // Biến bù trừ thời gian (mili-giây)
// 👇 BỔ SUNG 2 BIẾN NÀY ĐỂ ĐIỀU KHIỂN AUDIO
    var isAudioPlaying by remember { mutableStateOf(false) }
    var totalAudioDurationMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(activeMediaType, webViewRef, mediaPlayer, isAudioPlaying) {
        var lastTickTime = System.currentTimeMillis()
        while (true) {
            val now = System.currentTimeMillis()
            val delta = now - lastTickTime
            lastTickTime = now

            if (activeMediaType == "YOUTUBE" && webViewRef != null) {
                webViewRef?.evaluateJavascript("(function() { var v = document.getElementsByTagName('video')[0]; return v ? v.currentTime : -1; })();") { result ->
                    val timeSec = result?.replace("\"", "")?.toFloatOrNull()
                    if (timeSec != null && timeSec >= 0f) { currentVideoTimeMs = (timeSec * 1000).toLong() }
                }
            } else if ((activeMediaType.endsWith("_AUDIO") || activeMediaType == "ZING_KARAOKE") && mediaPlayer != null) {
                try {
                    // Liên tục cập nhật thời gian từ MediaPlayer thật của hệ thống
                    currentVideoTimeMs = mediaPlayer!!.currentPosition.toLong()
                    isAudioPlaying = mediaPlayer!!.isPlaying
                    if (totalAudioDurationMs == 0L) {
                        totalAudioDurationMs = mediaPlayer!!.duration.toLong()
                    }
                } catch (e: Exception) {}
            } else if (activeMediaType == "ZING_KARAOKE" && mediaPlayer == null && isAudioPlaying) {
                // Tự động nhích bộ đếm nếu chạy mô phỏng không có MediaPlayer
                currentVideoTimeMs += delta
            }
            delay(16) // Quét làm mượt màn hình ở mức 60fps
        }
    }




    var guitarChordsList by remember { mutableStateOf<List<ChordModel>>(emptyList()) }
    LaunchedEffect(Unit) {
        // Đẩy việc đọc và Parse file JSON nặng xuống luồng nền (IO) để không làm giật UI
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val chords = loadChordsFromAssets(context, "guitar.json")
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                guitarChordsList = chords
            }
        }
    }
    // 2. SAU ĐÓ MỚI GỌI LAUNCHED EFFECT SỬ DỤNG CÁC BIẾN ĐÓ
    // --- LẮNG NGHE YÊU CẦU XEM TRƯỚC TỪ TRANG DUYỆT BÀI ---
    LaunchedEffect(Unit) {
        val previewId = historyPref.getString("preview_song_id", "")
        if (!previewId.isNullOrEmpty()) {
            historyPref.edit().remove("preview_song_id").apply()

            keyboardController?.hide()
            focusManager.clearFocus()

            // Lúc này code đã hiểu các biến này là gì
            currentSongData = null
            isAutoScrolling = false
            activeMediaType = ""
            webViewRef = null
            isSearchingList = false
            searchResults = null
            isFullScreen = false

            // Bắt đầu gọi API lấy chi tiết bài hát
            isFetchingDetail = true
            transposeOffset = 0

            coroutineScope.launch {
                val detail = SongScraper.fetchSongDetails(previewId, currentUserEmail)
                if (detail.error == null) {
                    currentSongData = detail
                    selectedDisplayMode = if (detail.imageCount > 0) "SHEET" else "CHORD"
                } else {
                    Toast.makeText(context, CoreTranslator.getString("preview_load_error", currentLang), Toast.LENGTH_SHORT)
                        .show()
                }
                isFetchingDetail = false
            }
        }
    }

    var selectedLangCode by remember { mutableStateOf("") }
    var translatedLyrics by remember { mutableStateOf("") }
    var translatedTitle by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }

    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(0) }

    val supportedLanguages = mapOf(
        CoreTranslator.getString("original", currentLang) to "",
        "Tiếng Việt" to "vi",
        "English" to "en",
        "Français" to "fr",
        "Español" to "es",
        "Deutsch" to "de",
        "Italiano" to "it",
        "Português" to "pt",
        "Русский" to "ru",
        "日本語" to "ja",
        "한국어" to "ko",
        "中文" to "zh-CN",
        "ภาษาไทย" to "th",
        "Bahasa Indonesia" to "id",
        "العربية" to "ar",
        "हिन्दी (Hindi)" to "hi"
    )

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                webViewRef?.onPause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                webViewRef?.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentSongData) {
        showSongToneDialog = false
        selectedLangCode = ""; translatedLyrics = ""; translatedTitle = ""
        imageCacheBuster = System.currentTimeMillis()

        // Tự động tắt thông báo reload màu vàng khi có bài hát cụ thể được mở
        if (currentSongData != null) {
            hasNewUpdate = false
            globalHasNewUpdate = false // Lưu vào biến toàn cục để không bị bật lại khi chuyển Tab

            // --- BẮT ĐẦU THÊM MỚI: BÁO CÁO LƯỢT XEM KHI MỞ BÀI ---
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val sId = currentSongData!!.songId
                if (sId.isNotBlank()) {
                    // 1. Tăng view nội bộ SQLite để icon mắt nhảy số ngay lập tức
                    songRepository.incrementViewCountLocal(sId)

                    // 2. Phóng request ngầm gọi API để Server PHP tăng view trên MySQL
                    SongScraper.fetchSongDetails(sId, currentUserEmail)

                    // 3. Giật cò súng ép giao diện (Trang chủ) Load lại để cập nhật số
                    dbRefreshTrigger++
                }
            }
            // --- KẾT THÚC THÊM MỚI ---
        }

        // --- [BẢN VÁ] RESET TOÀN BỘ DỮ LIỆU KARAOKE KHI ĐỔI BÀI HÁT ---
        syncedKaraokeLines = null
        activeMediaType = ""
        currentVideoTimeMs = 0L
        syncOffsetMs = 0L
        totalAudioDurationMs = 0L
        webViewRef = null
        isAiAnalyzing = false
        aiAnalysisMode = "" // <-- BỔ SUNG RESET MODE
        tutorialVideoUrl = ""
        isAudioPlaying = false

        // Tắt nhạc cũ an toàn chống văng App
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaPlayer = null
        // --------------------------------------------------------------

        currentSongData?.let {
            likeCount = it.likes; isLiked = it.isLikedByDevice
            sharedPref.edit().putBoolean("liked_${it.songId}", it.isLikedByDevice).apply()
        }
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    urlInput =
                        TextFieldValue(text = matches[0], selection = TextRange(matches[0].length))
                }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches =
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    urlInput =
                        TextFieldValue(text = matches[0], selection = TextRange(matches[0].length))
                }
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        onDispose { speechRecognizer.destroy() }
    }

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    urlInput =
                        TextFieldValue(text = matches[0], selection = TextRange(matches[0].length))
                }
            }
        }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            toggleSongVoiceSearch(
                currentLang = currentLang,
                speechRecognizer = speechRecognizer,
                isListening = isListening,
                speechRecognizerLauncher = speechRecognizerLauncher,
                onListeningChanged = { isListening = it },
                onInputChanged = {
                    urlInput = it
                    isBroadSearch = false
                    offlineSearchQuery = ""
                }
            )
        } else {
            Toast.makeText(
                context,
                if (currentLang == "vi") "Vui lòng cấp quyền Micro để tìm kiếm bằng giọng nói"
                else "Please grant Microphone permission for voice search",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val performSearch: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()

        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }

        val query = urlInput.text.trim()

        if (query.isBlank()) {
            offlineSearchQuery = ""
            isBroadSearch = false
            isArtistFilter = false
        } else {
            addToHistory(query)

            currentSongData = null
            isAutoScrolling = false
            activeMediaType = ""
            webViewRef = null
            isFullScreen = false
            searchResults = null

            // Chỉ khi bấm Search mới tìm chứa từ khóa và lấy tối đa 50 kết quả.
            isBroadSearch = true
            isArtistFilter = false // Tìm kiếm thông thường từ bàn phím
            offlineSearchQuery = query
        }
    }

    // Tách effect ra hàm riêng để SongTabScreen không vượt giới hạn 64 KB/method của JVM.
    OfflineSearchDebounceEffect(
        inputText = urlInput.text,
        isSearchFocused = isSearchFocused,
        hasCurrentSong = currentSongData != null,
        isBroadSearch = isBroadSearch,
        offlineSearchQuery = offlineSearchQuery,
        onBroadSearchChanged = { isBroadSearch = it },
        onOfflineQueryChanged = { offlineSearchQuery = it }
    )

    // --- BIẾN CHO TÍNH NĂNG THÔNG BÁO (CHUÔNG) ---
    var latestNotificationId by remember { mutableIntStateOf(-1) }
    var notificationMessage by remember { mutableStateOf("") }
    var hasUnreadNotification by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // Hiệu ứng lắc qua lắc lại cho chiếc chuông
    val infiniteTransitionBell = androidx.compose.animation.core.rememberInfiniteTransition(label = "bell_shake")
    val bellRotation by infiniteTransitionBell.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(150, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "bellRotation"
    )

    // Tự động kiểm tra thông báo mới từ Server
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val lastReadNotifId = sharedPref.getInt("last_read_notification_id", -1)
                val url = java.net.URL("https://tunertools.top/api_songs.php?action=check_notifications")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = org.json.JSONObject(response)
                    if (json.optBoolean("has_notification", false)) {
                        val data = json.getJSONObject("data")
                        val notifId = data.optInt("id", -1)
                        val message = data.optString("message", "")

                        if (notifId > lastReadNotifId) {
                            val savedTimeKey = "notif_time_$notifId"
                            var receiveTime = sharedPref.getLong(savedTimeKey, -1L)

                            // Nếu là lần đầu tiên thiết bị thấy thông báo này, lưu giờ hiện tại lại
                            if (receiveTime == -1L) {
                                receiveTime = System.currentTimeMillis()
                                sharedPref.edit().putLong(savedTimeKey, receiveTime).apply()
                            }

                            // Kiểm tra xem đã qua 48 tiếng chưa (48h * 60p * 60s * 1000ms)
                            val expireTimeMs = 48L * 60L * 60L * 1000L
                            val timePassed = System.currentTimeMillis() - receiveTime

                            if (timePassed < expireTimeMs) {
                                // Vẫn trong hạn 48h -> Hiện chuông
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    latestNotificationId = notifId
                                    notificationMessage = message
                                    hasUnreadNotification = true
                                }
                            } else {
                                // Quá 48h -> Tự động đánh dấu đã đọc ngầm, không thèm hiện chuông nữa
                                sharedPref.edit().putInt("last_read_notification_id", notifId).apply()
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- BỘ ĐẾM THỜI GIAN THỰC TỰ ĐỘNG GỠ CHUÔNG SAU 48H ---
    LaunchedEffect(hasUnreadNotification, latestNotificationId) {
        if (hasUnreadNotification && latestNotificationId != -1) {
            val receiveTime = sharedPref.getLong("notif_time_$latestNotificationId", System.currentTimeMillis())
            val expireTimeMs = 48L * 60L * 60L * 1000L
            val timeRemaining = (receiveTime + expireTimeMs) - System.currentTimeMillis()

            if (timeRemaining > 0) {
                // Chờ ngầm cho đến khi hết hạn
                delay(timeRemaining)
            }

            // Khi hết hạn (hoặc nếu đã lố thời gian) -> Tự động gỡ chuông và đóng bảng
            hasUnreadNotification = false
            showNotificationDialog = false
            sharedPref.edit().putInt("last_read_notification_id", latestNotificationId).apply()
        }
    }

    var hasAcceptedPolicyThisSession by rememberSaveable { mutableStateOf(false) }
    var showAddPolicyDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = if (isFullScreen || currentSongData != null) 0.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Xử lý giấu thanh tìm kiếm khi Toàn màn hình
            if (!isFullScreen) {
                // Chừa đúng vùng an toàn của camera khoét lỗ/status bar, cộng thêm 8dp cho thoáng
                Spacer(
                    modifier = Modifier
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        )
                        .height(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (currentSongData != null) 12.dp else 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentSongData != null || isFetchingDetail) {
                        IconButton(onClick = {
                            currentSongData = null; isFetchingDetail = false; transposeOffset =
                            0; isAutoScrolling = false; activeMediaType = ""; webViewRef = null
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    SongSearchInputField(
                        value = urlInput,
                        currentLang = currentLang,
                        isFocused = isSearchFocused,
                        isListening = isListening,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        tunaGreen = tunaGreen,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            urlInput = it
                            isBroadSearch = false
                            isArtistFilter = false // Reset khi người dùng gõ tay
                            if (it.text.trim().length >= 2) {
                                isSearchingOffline = true
                            } else {
                                isSearchingOffline = false
                                offlineSearchQuery = ""
                            }
                        },
                        onFocusChanged = {
                            isSearchFocused = it
                        },
                        onSearch = performSearch,
                        onClear = {
                            urlInput = TextFieldValue("")
                            offlineSearchQuery = ""
                            isBroadSearch = false
                            isArtistFilter = false
                            searchResults = null
                        },
                        onOpenFavorites = {
                            openFavoriteSongsFromSearch(
                                context = context,
                                coroutineScope = coroutineScope,
                                currentUserEmail = currentUserEmail,
                                sharedPref = sharedPref,
                                songRepository = songRepository,
                                onResetScreen = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    currentSongData = null
                                    isAutoScrolling = false
                                    activeMediaType = ""
                                    webViewRef = null
                                    urlInput = TextFieldValue("")
                                    searchResults = null
                                    isFullScreen = false
                                },
                                onLoadingChanged = {
                                    isSearchingList = it
                                },
                                onResultsChanged = {
                                    searchResults = it
                                }
                            )
                        },
                        onToggleVoiceSearch = {
                            val hasMicPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasMicPermission) {
                                toggleSongVoiceSearch(
                                    currentLang = currentLang,
                                    speechRecognizer = speechRecognizer,
                                    isListening = isListening,
                                    speechRecognizerLauncher = speechRecognizerLauncher,
                                    onListeningChanged = {
                                        isListening = it
                                    },
                                    onInputChanged = {
                                        urlInput = it
                                        isBroadSearch = false
                                        offlineSearchQuery = ""
                                    }
                                )
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                    // CỤM NÚT CÔNG CỤ SẼ TỰ ĐỘNG ẨN ĐI KHI CLICK VÀO Ô TÌM KIẾM
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isSearchFocused,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            // --- [BẮT ĐẦU] NÚT MỞ BẢNG NHẠC GUITAR PRO TAB ---
                            Spacer(modifier = Modifier.width(6.dp))

                            // Tạo hiệu ứng vòng lặp cho màu sắc RGB và độ chớp (alpha)
                            val tabInfiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "tab_blink")
                            val tabHue by tabInfiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(2000, easing = androidx.compose.animation.core.LinearEasing),
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                                ),
                                label = "tabHue"
                            )
                            val tabAlphaBlink by tabInfiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 0.2f,
                                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                    animation = androidx.compose.animation.core.tween(500), // Tốc độ chớp nháy
                                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                ),
                                label = "tabAlphaBlink"
                            )
                            // Đổi dải màu Float sang Color của Jetpack Compose
                            val tabBlinkColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(tabHue, 1f, 1f)))

                            Button(
                                onClick = {
                                    showAlphaTabScreen = true
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = CustomIcons.MusicScore,
                                        contentDescription = "Guitar Pro Tab",
                                        tint = tunaGreen,
                                        modifier = Modifier.size(20.dp) // Thu nhỏ icon một chút để nhường chỗ cho chữ
                                    )
                                    Text(
                                        text = "Tab",
                                        color = tabBlinkColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 2.dp, top = 2.dp)
                                            .alpha(tabAlphaBlink)
                                    )
                                }
                            }
                            // --- [KẾT THÚC] NÚT TAB ---

// 1. NÚT CHORD AI (Chỉ hiển thị khi đang xem 1 bài hát cụ thể)
                            if (currentSongData != null) {
                                Spacer(modifier = Modifier.width(6.dp))

// Tạo hiệu ứng nhấp nháy (alpha từ 1f -> 0.2f)
                                val infiniteTransition =
                                    androidx.compose.animation.core.rememberInfiniteTransition(label = "blink")
                                val alphaBlink by infiniteTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0.2f,
                                    animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                        animation = androidx.compose.animation.core.tween(500), // Tốc độ nhấp nháy (500ms)
                                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                                    ),
                                    label = "alphaBlink"
                                )

                                Button(
                                    onClick = {
                                        showPlayModeDialog = true
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChordAIIcon(modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Ai",
                                            color = Color.Red,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(end = 4.dp, top = 2.dp)
                                                .alpha(alphaBlink)
                                        )
                                    }
                                }
                            }

// 2. NÚT CAMERA (Chỉ hiển thị khi đang xem 1 bài hát cụ thể)
                            if (currentSongData != null && !isFullScreen) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        val hasCamera = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.CAMERA
                                        ) == PackageManager.PERMISSION_GRANTED
                                        val hasAudio = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (hasCamera && hasAudio) {
                                            showFloatingCamera = !showFloatingCamera
                                        } else {
                                            cameraPermissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.CAMERA,
                                                    Manifest.permission.RECORD_AUDIO
                                                )
                                            )
                                        }
                                    },
                                    // ĐÃ SỬA: Màu nền xám đen giống hệt các nút khác
                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                    // ĐÃ SỬA: Viền xám mờ (nếu đang tắt) hoặc Đỏ mờ (nếu đang bật)
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (showFloatingCamera) Color.Red.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "Camera",
                                        // ĐÃ SỬA: Icon xanh lá đồng bộ, chuyển Đỏ khi đang bật camera
                                        tint = if (showFloatingCamera) Color.Red else tunaGreen
                                    )
                                }
                            }
                        }
                    }

                    if (currentSongData == null) {
                        // --- NÚT ĐỒNG BỘ HOẶC TÍNH NĂNG AI ---
                        Spacer(modifier = Modifier.width(6.dp))

                        // Chuẩn bị hiệu ứng nhấp nháy cho text AI
                        val blinkTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "blink_ai_main")
                        val alphaBlink by blinkTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.2f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(500),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            ),
                            label = "alphaBlink"
                        )

                        Box(contentAlignment = Alignment.TopEnd) {
                            Button(
                                onClick = {
                                    // NẾU CÓ CẬP NHẬT HOẶC ĐANG BÁO LỖI -> CHẠY ĐỒNG BỘ RELOAD
                                    if (hasNewUpdate || isSyncing || syncErrorCode.isNotEmpty()) {
                                        if (!isSyncing) {
                                            isSyncing = true
                                            hasNewUpdate = false // Tắt hiệu ứng chấm đỏ
                                            globalHasNewUpdate = false // Cập nhật biến toàn cục
                                            globalLastCheckTime = System.currentTimeMillis() // Reset lại đồng hồ đếm 3 phút
                                            syncIconColor = tunaGreen
                                            syncErrorCode = ""

                                            coroutineScope.launch {
                                                val message = songRepository.syncNewSongsOnline()

                                                if (message.contains("Lỗi máy chủ")) {
                                                    syncIconColor = Color.Red
                                                    syncErrorCode = message.substringAfter("Lỗi máy chủ: ").trim()
                                                } else if (message.contains("Lỗi mạng")) {
                                                    syncIconColor = Color.Red
                                                    syncErrorCode = "ERR"
                                                } else {
                                                    syncIconColor = tunaGreen
                                                    syncErrorCode = ""
                                                    dbRefreshTrigger++ // Load lại danh sách
                                                }

                                                isSyncing = false

                                                if (syncIconColor == Color.Red) {
                                                    delay(3000)
                                                    syncIconColor = tunaGreen
                                                    syncErrorCode = ""
                                                }
                                            }
                                        }
                                    } else {
                                        // NẾU KHÔNG CÓ CẬP NHẬT -> CHẠY TÍNH NĂNG AI
                                        showPlayModeDialog = true
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(40.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = tunaGreen,
                                        strokeWidth = 2.dp
                                    )
                                } else if (hasNewUpdate || syncErrorCode.isNotEmpty()) {
                                    Box(contentAlignment = Alignment.Center) {
                                        // HIỂN THỊ MŨI TÊN RELOAD
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sync",
                                            tint = if (hasNewUpdate) Color(0xFFFFC107) else syncIconColor,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .graphicsLayer {
                                                    if (hasNewUpdate && !isSyncing) {
                                                        rotationZ = iconRotation
                                                    }
                                                }
                                        )

                                        if (syncErrorCode.isNotEmpty()) {
                                            Text(
                                                text = syncErrorCode,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier
                                                    .background(Color.Red, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // HIỂN THỊ ICON TÍNH NĂNG AI KHI KHÔNG CÓ UPDATE
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ChordAIIcon(modifier = Modifier.size(20.dp))
                                        Text(
                                            text = "Ai",
                                            color = Color.Red,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(end = 4.dp, top = 2.dp)
                                                .alpha(alphaBlink)
                                        )
                                    }
                                }
                            }

                            // Chấm trạng thái máy chủ nằm đè trên border góc phải nút AI:
                            // xanh lá = máy chủ chính OK; xanh dương = đang dùng dự phòng;
                            // đỏ = cả máy chủ chính và dự phòng đều lỗi; xám = đang kiểm tra.
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .size(10.dp)
                                    .background(gpServerIndicatorColor, CircleShape)
                                    .border(1.5.dp, bgColor, CircleShape)
                            )

                            // Giữ chấm báo cập nhật cũ nhưng chuyển sang góc trái để không
                            // chồng lên chấm trạng thái máy chủ.
                            if (hasNewUpdate && !isSyncing) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(x = (-2).dp, y = (-2).dp)
                                        .size(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .scale(pulseScale)
                                            .alpha(pulseAlpha)
                                            .background(Color.Red, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Red, CircleShape)
                                            .border(1.5.dp, bgColor, CircleShape)
                                    )
                                }
                            }
                        }

// --- ICON TRÒ CHUYỆN HỖ TRỢ: CHỈ HIỆN KHI CÓ CUỘC TRÒ CHUYỆN ĐANG HOẠT ĐỘNG ---
                        if (showSupportChatIcon) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(contentAlignment = Alignment.TopEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(surfaceColor)
                                        .border(
                                            1.dp,
                                            tunaGreen.copy(alpha = 0.55f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSupportChatClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo),
                                        contentDescription = "Mở trò chuyện hỗ trợ",
                                        modifier = Modifier
                                            .size(21.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                }

                                if (supportChatBadgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = 5.dp, y = (-5).dp)
                                            .sizeIn(minWidth = 14.dp, minHeight = 14.dp)
                                            .background(Color.Red, CircleShape)
                                            .border(1.dp, bgColor, CircleShape)
                                            .padding(horizontal = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (supportChatBadgeCount > 9) "9+" else supportChatBadgeCount.toString(),
                                            color = Color.White,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

// --- NÚT THÊM BÀI HÁT / CHUÔNG THÔNG BÁO ---
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (hasUnreadNotification) {
                                    showNotificationDialog = true
                                } else {
                                    val acc = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                                    if (acc != null) {
                                        // KIỂM TRA QUYỀN ADMIN TỐI CAO
                                        if (userRole.equals("super_admin", ignoreCase = true) || userRole.equals("supper_admin", ignoreCase = true)) {
                                            showAdminActionSelector = true
                                        } else {
                                            // NGƯỜI DÙNG BÌNH THƯỜNG -> CHẠY THẲNG VÀO FORM THÊM BÀI HÁT
                                            if (hasAcceptedPolicyThisSession) showAddDialog = true else showAddPolicyDialog = true
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            CoreTranslator.getString("login_required_add", currentLang),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            if (hasUnreadNotification) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = "Thông báo",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            rotationZ = bellRotation
                                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.2f)
                                        }
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = CoreTranslator.getString("add", currentLang),
                                    tint = tunaGreen
                                )
                            }
                        }
                    }

                }

                Spacer(modifier = Modifier.height(12.dp))
            } // Kết thúc vùng bị ẩn khi Fullscreen

            // BẢN VÁ: Truyền currentLang vào remember và vào các hàm get
            val trendingSongs by remember(dbRefreshTrigger, currentLang) { songRepository.getTrendingSongsOffline(currentLang) }.collectAsState(initial = emptyList<Song>())
            val suggestedSongs by remember(dbRefreshTrigger, currentLang) { songRepository.getSuggestedKaraokeSongs(currentLang) }.collectAsState(initial = emptyList())
            val top50Songs by remember(dbRefreshTrigger, currentLang) { songRepository.getTop50PopularSongs(currentLang) }.collectAsState(initial = emptyList())

            // --- KHAI BÁO DỮ LIỆU TỪ CA_SI.DB ---
            val favoriteArtists by remember(dbRefreshTrigger, currentLang) { songRepository.getArtistsFromCaSiDb(null, null, currentLang) }.collectAsState(initial = emptyList<SongRepository.ArtistOffline>())
            val nhacTre8x9xArtists by remember(dbRefreshTrigger, currentLang) { songRepository.getArtistsFromCaSiDb("nhac_tre", "the_he_8x_9x_doi_dau", currentLang) }.collectAsState(initial = emptyList<SongRepository.ArtistOffline>())
            val nhacTreGenZArtists by remember(dbRefreshTrigger, currentLang) { songRepository.getArtistsFromCaSiDb("nhac_tre", "the_he_2k", currentLang) }.collectAsState(initial = emptyList<SongRepository.ArtistOffline>())
            val boleroKinhDienArtists by remember(dbRefreshTrigger, currentLang) { songRepository.getArtistsFromCaSiDb("bolero", "the_he_kinh_dien_8x_tro_ve_truoc", currentLang) }.collectAsState(initial = emptyList<SongRepository.ArtistOffline>())
            val boleroTiepNoiArtists by remember(dbRefreshTrigger, currentLang) { songRepository.getArtistsFromCaSiDb("bolero", "the_he_tiep_noi_thanh_danh_9x", currentLang) }.collectAsState(initial = emptyList<SongRepository.ArtistOffline>())

            // ĐÃ SỬA: Nền đen tuyền tiết kiệm pin, bỏ bo góc để tràn viền hoàn toàn
            val mainBoxModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(bgColor)

            Box(modifier = mainBoxModifier) {
                if (isSearchingList || isFetchingDetail) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AudioWaveLoadingIndicator(color = tunaGreen, modifier = Modifier.height(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (currentLang == "vi") "Đang xử lý dữ liệu..." else "Processing data...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                } else if (currentSongData != null) {

                    val scrollState = rememberScrollState()
                    LaunchedEffect(isAutoScrolling, scrollSpeed) {
                        if (isAutoScrolling) {
                            var exactPosition = scrollState.value.toFloat()
                            while (scrollState.value < scrollState.maxValue) {
                                delay(16L); exactPosition += (0.5f * scrollSpeed); scrollState.scrollTo(
                                    exactPosition.toInt()
                                )
                            }
                            isAutoScrolling = false
                        }
                    }

                    // Xác định trạng thái có đang phát video hoặc âm thanh hay không
                    val isMediaActive = activeMediaType == "YOUTUBE" || activeMediaType.endsWith("_AUDIO") || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE" || activeMediaType == "ZING_KARAOKE"

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Sử dụng Column tổng để tách biệt phần Header cố định và phần Lời cuộn được
                        Column(modifier = Modifier.fillMaxSize()) {

                            // 1. VÙNG HEADER CỐ ĐỊNH: Khi đang phát video HOẶC Audio, ghim Header lên đỉnh
                            if (isMediaActive && !isDoNotDisturbMode) { // Ẩn Header Media khi bật "Không làm phiền"
                                SongDetailHeader(
                                    context = context,
                                    currentSongData = currentSongData!!,
                                    currentLang = currentLang,
                                    isLiked = isLiked,
                                    likeCount = likeCount,
                                    tunaGreen = tunaGreen,
                                    textColor = textColor,
                                    activeMediaType = activeMediaType,
                                    overrideVideoUrl = tutorialVideoUrl,
                                    isStaff = isStaff,
                                    userRole = userRole,
                                    sharedPref = sharedPref,
                                    songRepository = songRepository,
                                    refreshTrigger = dbRefreshTrigger,
                                    surfaceColor = surfaceColor,
                                    onCreateCustomChord = {
                                        customTemplateSongData = SongDetails(
                                            title = currentSongData!!.title,
                                            composer = currentSongData!!.composer,
                                            singer = currentSongData!!.singer,
                                            baseTone = currentSongData!!.baseTone,
                                            rhythm = currentSongData!!.rhythm,
                                            bpm = currentSongData!!.bpm,
                                            videoUrl = currentSongData!!.videoUrl,
                                            lyrics = currentSongData!!.lyrics
                                        )
                                        showAddDialog = true
                                    },
                                    onEditCustomChord = { cDetail ->
                                        customTemplateSongData = cDetail
                                        showAddDialog = true
                                    },
                                    onDeleteCustomChord = {
                                        coroutineScope.launch {
                                            songRepository.deleteCustomSongLocal(currentSongData!!.title, currentSongData!!.singer)
                                            dbRefreshTrigger++
                                            if (currentSongData!!.songId.startsWith("local_")) {
                                                val origSong = songRepository.getSongByExactIdOrTitle("", currentSongData!!.title)
                                                if (origSong != null) {
                                                    currentSongData = SongDetails(
                                                        title = origSong.title, singer = origSong.ca_si, composer = origSong.tac_gia,
                                                        baseTone = origSong.tong, rhythm = origSong.dieu.ifBlank { origSong.rhythm },
                                                        bpm = if (origSong.bpm > 0) origSong.bpm.toString() else "", lyrics = origSong.lyrics.joinToString("\n"),
                                                        videoUrl = origSong.youtubeUrl, songId = origSong.id, slug = origSong.slug,
                                                        imageCount = origSong.image_count, zingMp3Id = origSong.zingMp3Id
                                                    )
                                                } else {
                                                    currentSongData = null
                                                }
                                            }
                                        }
                                    },
                                    onEnterVideoFullScreen = { view, callback ->
                                        customVideoView = view
                                        customVideoCallback = callback
                                    },
                                    onExitVideoFullScreen = {
                                        customVideoCallback?.onCustomViewHidden()
                                        customVideoView = null
                                        customVideoCallback = null
                                    },
                                    onWebViewCreated = { webViewRef = it },
                                    onCloseVideoClick = {
                                        webViewRef?.onPause()
                                        webViewRef?.loadUrl("about:blank")
                                        // 👇 Dọn dẹp cả Audio nếu đang phát nhạc
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                        isAudioPlaying = false

                                        activeMediaType = ""
                                        webViewRef = null
                                        tutorialVideoUrl = ""
                                    },
                                    onReportClick = {
                                        reportInfo = ReportInfo(
                                            currentSongData!!.songId,
                                            currentSongData!!.title,
                                            currentSongData!!.singer,
                                            currentSongData!!.composer
                                        )
                                    },
                                    onLikeClick = {
                                        if (currentUserEmail.isBlank()) {
                                            Toast.makeText(context, "Vui lòng đăng nhập Gmail để thêm vào Yêu thích", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val newLikedState = !isLiked
                                            isLiked = newLikedState
                                            likeCount = if (newLikedState) likeCount + 1 else maxOf(0, likeCount - 1)
                                            sharedPref.edit().putBoolean("liked_${currentSongData!!.songId}", newLikedState).apply()
                                            coroutineScope.launch { SongScraper.toggleLike(currentSongData!!.songId, currentUserEmail, newLikedState) }
                                        }
                                    },
                                    onPlayVideoClick = { /* Đang phát rồi không xử lý thêm */ },
                                    onEditClick = { showEditDialog = true },
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            // 1. Phóng lệnh xóa lên Server MySQL
                                            val successOnline = SongScraper.deleteSong(currentSongData!!.songId)

                                            // 2. Dọn dẹp bản sao trong SQLite cục bộ
                                            val successOffline = songRepository.deleteSongOffline(currentSongData!!.songId)

                                            if (successOnline || successOffline) {
                                                Toast.makeText(context, CoreTranslator.getString("song_deleted_system", currentLang), Toast.LENGTH_SHORT).show()
                                                currentSongData = null
                                                dbRefreshTrigger++
                                            } else {
                                                Toast.makeText(context, CoreTranslator.getString("delete_permission_error", currentLang), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onToneDetectClick = { showSongToneDialog = true },
                                    onTutorialClick = { showTutorialSheet = true },
                                    onStrummingClick = { showStrummingDialog = true }
                                )

                            }

                            // 2. VÙNG NỘI DUNG CUỘN ĐƯỢC: Chứa lời bài hát và các công cụ
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    // Thêm padding top 12.dp (hoặc 16.dp nếu bạn muốn thoáng hơn nữa)
                                    .padding(
                                        top = if (isFullScreen) 0.dp else 16.dp,
                                        start = if (isFullScreen) 0.dp else 12.dp,
                                        end = if (isFullScreen) 0.dp else 12.dp,
                                        bottom = if (isFullScreen) 0.dp else 12.dp
                                    )
                                    .verticalScroll(scrollState)
                            ) {
                                // Nếu KHÔNG phải chế độ FullScreen và KHÔNG phát media thì mới hiện Header cuộn theo bài hát như cũ
                                if (!isFullScreen && !isMediaActive && !isDoNotDisturbMode) { // Ẩn Header cuộn khi bật "Không làm phiền"
                                    SongDetailHeader(
                                        context = context,
                                        currentSongData = currentSongData!!,
                                        currentLang = currentLang,
                                        isLiked = isLiked,
                                        likeCount = likeCount,
                                        tunaGreen = tunaGreen,
                                        textColor = textColor,
                                        activeMediaType = activeMediaType,
                                        overrideVideoUrl = tutorialVideoUrl,
                                        isStaff = isStaff,
                                        userRole = userRole,
                                        sharedPref = sharedPref,
                                        songRepository = songRepository,
                                        refreshTrigger = dbRefreshTrigger,
                                        surfaceColor = surfaceColor,
                                        onCreateCustomChord = {
                                            customTemplateSongData = SongDetails(
                                                title = currentSongData!!.title,
                                                composer = currentSongData!!.composer,
                                                singer = currentSongData!!.singer,
                                                baseTone = currentSongData!!.baseTone,
                                                rhythm = currentSongData!!.rhythm,
                                                bpm = currentSongData!!.bpm,
                                                videoUrl = currentSongData!!.videoUrl,
                                                lyrics = currentSongData!!.lyrics
                                            )
                                            showAddDialog = true
                                        },
                                        onEditCustomChord = { cDetail ->
                                            customTemplateSongData = cDetail
                                            showAddDialog = true
                                        },
                                        onDeleteCustomChord = {
                                            coroutineScope.launch {
                                                songRepository.deleteCustomSongLocal(currentSongData!!.title, currentSongData!!.singer)
                                                dbRefreshTrigger++
                                                if (currentSongData!!.songId.startsWith("local_")) {
                                                    val origSong = songRepository.getSongByExactIdOrTitle("", currentSongData!!.title)
                                                    if (origSong != null) {
                                                        currentSongData = SongDetails(
                                                            title = origSong.title, singer = origSong.ca_si, composer = origSong.tac_gia,
                                                            baseTone = origSong.tong, rhythm = origSong.dieu.ifBlank { origSong.rhythm },
                                                            bpm = if (origSong.bpm > 0) origSong.bpm.toString() else "", lyrics = origSong.lyrics.joinToString("\n"),
                                                            videoUrl = origSong.youtubeUrl, songId = origSong.id, slug = origSong.slug,
                                                            imageCount = origSong.image_count, zingMp3Id = origSong.zingMp3Id
                                                        )
                                                    } else {
                                                        currentSongData = null
                                                    }
                                                }
                                            }
                                        },
                                        onWebViewCreated = { webViewRef = it },
                                        onCloseVideoClick = {
                                            webViewRef?.onPause()
                                            webViewRef?.loadUrl("about:blank")
                                            activeMediaType = ""
                                            webViewRef = null
                                            tutorialVideoUrl = ""
                                        },
                                        onReportClick = {
                                            reportInfo = ReportInfo(
                                                currentSongData!!.songId,
                                                currentSongData!!.title,
                                                currentSongData!!.singer,
                                                currentSongData!!.composer
                                            )
                                        },
                                        onLikeClick = {
                                            if (currentUserEmail.isBlank()) {
                                                Toast.makeText(context, "Vui lòng đăng nhập Gmail để thêm vào Yêu thích", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val newLikedState = !isLiked
                                                isLiked = newLikedState
                                                likeCount = if (newLikedState) likeCount + 1 else maxOf(0, likeCount - 1)
                                                sharedPref.edit().putBoolean("liked_${currentSongData!!.songId}", newLikedState).apply()
                                                coroutineScope.launch { SongScraper.toggleLike(currentSongData!!.songId, currentUserEmail, newLikedState) }
                                            }
                                        },
                                        onPlayVideoClick = {
                                            mediaPlayer?.release()
                                            mediaPlayer = null
                                            syncedKaraokeLines = null
                                            activeMediaType = "YOUTUBE"
                                        },
                                        onEditClick = { showEditDialog = true },
                                        onDeleteClick = {
                                            coroutineScope.launch {
                                                val successOnline = SongScraper.deleteSong(currentSongData!!.songId)
                                                val successOffline = songRepository.deleteSongOffline(currentSongData!!.songId)

                                                if (successOnline || successOffline) {
                                                    Toast.makeText(context, CoreTranslator.getString("delete_success", currentLang), Toast.LENGTH_SHORT).show()
                                                    dbRefreshTrigger++
                                                } else {
                                                    Toast.makeText(context, CoreTranslator.getString("delete_permission_error", currentLang), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onToneDetectClick = { showSongToneDialog = true },
                                        onTutorialClick = { showTutorialSheet = true },
                                        onStrummingClick = { showStrummingDialog = true }
                                    )
                                    // Đã xóa Spacer(6.dp) để kéo sát ảnh bìa vào thanh công cụ
                                }
                                // --- HIỂN THỊ SHEET NHẠC (ẢNH) ---
                                if (selectedDisplayMode == "SHEET") {
                                    SheetMusicViewer(
                                        currentSongData = currentSongData!!,
                                        currentLang = currentLang,
                                        isFullScreen = isFullScreen,
                                        onFullScreenToggle = { isFullScreen = !isFullScreen },
                                        scrollSpeed = scrollSpeed,
                                        onScrollSpeedChange = { scrollSpeed = it },
                                        isAutoScrolling = isAutoScrolling,
                                        onAutoScrollToggle = { isAutoScrolling = !isAutoScrolling },
                                        tunaGreen = tunaGreen,
                                        bgColor = bgColor,
                                        surfaceColor = surfaceColor,
                                        textColor = textColor,
                                        imageCacheBuster = imageCacheBuster
                                    )
                                }
                                // --- HIỂN THỊ HỢP ÂM CHỮ ---
                                else {
// --- TOOLBAR ĐIỀU KHIỂN HỢP ÂM CẢI TIẾN ---
                                    var isPatternBarHidden by remember(currentSongData!!.songId) {
                                        mutableStateOf(sharedPref.getBoolean("hide_pattern_${currentSongData!!.songId}", false))
                                    }
                                    var selectedPatternUpdateTrigger by remember { mutableIntStateOf(0) }

                                    if (!isDoNotDisturbMode) { // Ẩn Toolbar khi bật "Không làm phiền"
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    top = 0.dp,
                                                    bottom = if (isPatternBarHidden) 4.dp else 0.dp
                                                ),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(surfaceColor, RoundedCornerShape(12.dp))
                                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            ) {
                                                // 1. THANH CÔNG CỤ (Đã bóc tách để tránh lỗi MethodTooLarge)
                                                ChordControlToolbar(
                                                    currentLang = currentLang,
                                                    currentSongData = currentSongData!!,
                                                    surfaceColor = surfaceColor,
                                                    bgColor = bgColor,
                                                    textColor = textColor,
                                                    tunaGreen = tunaGreen,
                                                    transposeOffset = transposeOffset,
                                                    onTransposeOffsetChange = { transposeOffset = it },
                                                    isFootTapping = isFootTapping,
                                                    metronomeTick = metronomeTick,
                                                    onShowBpmSettingsDialog = { showBpmSettingsDialog = true },
                                                    scrollSpeed = scrollSpeed,
                                                    onScrollSpeedChange = { scrollSpeed = it },
                                                    isAutoScrolling = isAutoScrolling,
                                                    onAutoScrollingChange = { isAutoScrolling = it },
                                                    customChordColor = customChordColor,
                                                    onCustomChordColorChange = { customChordColor = it },
                                                    isChordOverText = isChordOverText,
                                                    onChordOverTextChange = { isChordOverText = it },
                                                    isPronounSwapped = isPronounSwapped,
                                                    onPronounSwappedChange = { isPronounSwapped = it },
                                                    showRomanNumerals = showRomanNumerals,
                                                    onRomanNumeralsChange = { showRomanNumerals = it },
                                                    isEasyChordMode = isEasyChordMode,
                                                    onEasyChordModeToggle = { isEasyChordMode = it },
                                                    isLyricsOnly = isLyricsOnly,
                                                    onLyricsOnlyToggle = { isLyricsOnly = it },
                                                    isDoNotDisturbMode = isDoNotDisturbMode,
                                                    onDoNotDisturbToggle = { isDoNotDisturbMode = it },
                                                    fontSizeBody = fontSizeBody,
                                                    onFontSizeBodyChange = { fontSizeBody = it },
                                                    selectedLangCode = selectedLangCode,
                                                    supportedLanguages = supportedLanguages,
                                                    isPatternBarHidden = isPatternBarHidden,
                                                    onTranslateAction = { langCode ->
                                                        selectedLangCode = langCode
                                                        if (langCode == "") {
                                                            translatedLyrics = ""; translatedTitle = ""
                                                        } else {
                                                            isTranslating = true; translatedLyrics = ""; translatedTitle = ""
                                                            coroutineScope.launch {
                                                                val tDef = async { SongScraper.translateText(currentSongData!!.title, langCode) }
                                                                val lDef = async { SongScraper.translateText(currentSongData!!.lyrics, langCode) }
                                                                translatedTitle = tDef.await()
                                                                translatedLyrics = lDef.await()
                                                                isTranslating = false
                                                                if (translatedLyrics.isBlank() || translatedLyrics.contains("Lỗi")) {
                                                                    Toast.makeText(context, CoreTranslator.getString("trans_error", currentLang), Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    }
                                                )

                                                // HIỂN THỊ ĐẦY ĐỦ THANH TIẾT TẤU KHI CHƯA ẨN
                                                if (currentSongData!!.rhythm.isNotBlank() && !isPatternBarHidden) {
                                                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f), thickness = 1.dp)

                                                    key(selectedPatternUpdateTrigger, currentSongData!!.songId) {
                                                        val extractedChordsForBar = remember(currentSongData!!.lyrics, transposeOffset) {
                                                            Regex("\\[(.*?)\\]")
                                                                .findAll(currentSongData!!.lyrics)
                                                                .map { transposeSingleChord(it.groupValues[1], transposeOffset) }
                                                                .distinct()
                                                                .toList()
                                                        }

                                                        SingleStrummingPatternBar(
                                                            songId = currentSongData!!.songId,
                                                            rhythmName = currentSongData!!.rhythm,
                                                            bpm = currentSongData!!.bpm.replace("\n", "").replace("\r", "").trim().toFloatOrNull() ?: 74f,
                                                            songChords = extractedChordsForBar,
                                                            guitarChordsList = guitarChordsList,
                                                            surfaceColor = surfaceColor,
                                                            tunaGreen = tunaGreen,
                                                            textColor = textColor,
                                                            onHide = {
                                                                isPatternBarHidden = true
                                                                sharedPref.edit().putBoolean("hide_pattern_${currentSongData!!.songId}", true).apply()
                                                            },
                                                            onOpenFullDialog = { showStrummingDialog = true }
                                                        )
                                                    }
                                                }

                                                // 2. THANH TUA NHẠC DÍNH LIỀN BÊN DƯỚI
                                                if (activeMediaType.endsWith("_AUDIO") || activeMediaType == "ZING_KARAOKE") {
                                                    androidx.compose.material3.HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                                    AudioPlayerBar(
                                                        currentMs = currentVideoTimeMs,
                                                        totalMs = totalAudioDurationMs,
                                                        isPlaying = isAudioPlaying,
                                                        tunaGreen = tunaGreen,
                                                        textColor = textColor,
                                                        surfaceColor = Color.Transparent, // Để trong suốt nhằm mượn màu nền của thanh công cụ
                                                        onPlayPause = {
                                                            // Ra lệnh dừng/phát trực tiếp cho phần cứng âm thanh
                                                            if (isAudioPlaying) {
                                                                mediaPlayer?.pause()
                                                            } else {
                                                                mediaPlayer?.start()
                                                            }
                                                            isAudioPlaying = !isAudioPlaying
                                                        },
                                                        onSeek = { ms ->
                                                            // Ra lệnh tua trực tiếp cho phần cứng âm thanh
                                                            mediaPlayer?.seekTo(ms.toInt())
                                                            currentVideoTimeMs = ms
                                                        }
                                                    )
                                                }
                                            }

                                            // NÚT MŨI TÊN NHỎ GỌN NẰM ĐÈ TRỰC TIẾP TRÊN BORDER DƯỚI KHI ẨN
                                            if (currentSongData!!.rhythm.isNotBlank() && isPatternBarHidden) {
                                                Box(
                                                    modifier = Modifier
                                                        .offset(y = 9.dp)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(surfaceColor)
                                                        .border(1.dp, tunaGreen.copy(alpha = 0.4f), CircleShape)
                                                        .clickable {
                                                            isPatternBarHidden = false
                                                            sharedPref.edit().putBoolean("hide_pattern_${currentSongData!!.songId}", false).apply()
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Hiện tiết tấu",
                                                        tint = tunaGreen,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        } // Đóng Box bọc ngoài cùng
                                    } // Kết thúc if (!isDoNotDisturbMode)

                                    // TẠO KHOẢNG CÁCH THOÁNG TRÁNH DÍNH NÚT MŨI TÊN MỞ RỘNG TIẾT TẤU
                                    if (!isDoNotDisturbMode) {
                                        Spacer(
                                            modifier = Modifier.height(
                                                when {
                                                    // Khoảng cách rộng hơn (24.dp) khi thanh tiết tấu đang hiển thị
                                                    currentSongData!!.rhythm.isNotBlank() && !isPatternBarHidden -> 12.dp

                                                    // Khoảng cách vừa phải (14.dp) để tránh dính nút mũi tên khi thu gọn
                                                    currentSongData!!.rhythm.isNotBlank() && isPatternBarHidden -> 12.dp

                                                    // Mặc định
                                                    else -> 12.dp
                                                }
                                            )
                                        )
                                    }

                                    if (translatedLyrics.isNotBlank() && selectedLangCode != "") {
                                        val displayLyrics = if (isPronounSwapped) swapPronounsSafe(translatedLyrics) else translatedLyrics
                                        val finalLyrics = if (isLyricsOnly) formatCleanLyricsOnly(displayLyrics) else formatRawLyrics(displayLyrics)
                                        ChordTextRenderer(lyrics = finalLyrics, chordColor = customChordColor, textColor = textColor, offset = transposeOffset, fontSize = fontSizeBody, isChordOverText = if (isLyricsOnly) false else isChordOverText, showRomanNumerals = showRomanNumerals, isEasyChordMode = isEasyChordMode, baseTone = currentSongData!!.baseTone, onChordClick = { popupChord = it })
                                        Spacer(modifier = Modifier.height(20.dp))
                                    } else {
                                        // 1. Cố định State an toàn để tránh bị null khi Render
                                        val currentKaraokeLines = syncedKaraokeLines
                                        val songData = currentSongData

                                        if (isAiAnalyzing) {
                                            AiKaraokeProcessingView(
                                                isFastLoadingCache = isFastLoadingCache,
                                                aiAnalysisMode = aiAnalysisMode,
                                                aiProgress = aiProgress,
                                                aiAnalysisStatus = aiAnalysisStatus,
                                                tunaGreen = tunaGreen,
                                                textColor = textColor
                                            )
                                            // 2. Dùng biến đã cố định thay vì ép kiểu !!
                                        } else if (currentKaraokeLines != null && songData != null && (activeMediaType == "YOUTUBE" || activeMediaType == "KARAOKE_AUDIO" || activeMediaType == "SINGER_AUDIO" || activeMediaType == "ACAPELLA_AUDIO" || activeMediaType == "LYRICS_ONLY_AUDIO" || activeMediaType == "ZING_KARAOKE")) {
                                            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                            val adjustedVideoTimeMs = currentVideoTimeMs + syncOffsetMs
                                            val activeIndex = currentKaraokeLines.indexOfLast { adjustedVideoTimeMs >= it.startTimeMs }
                                            LaunchedEffect(activeIndex) { if (activeIndex >= 0) { coroutineScope.launch { listState.animateScrollToItem(maxOf(0, activeIndex - 2)) } } }
                                            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
                                                itemsIndexed(currentKaraokeLines) { index, line ->
                                                    val isActive = index == activeIndex
                                                    val isPassed = index < activeIndex
                                                    val rawProgress = when {
                                                        isActive -> {
                                                            if (currentVideoTimeMs < line.startTimeMs) -1f
                                                            else if (line.words.isEmpty()) { val dur = (line.endTimeMs - line.startTimeMs).coerceAtLeast(1L); ((currentVideoTimeMs - line.startTimeMs).toFloat() / dur).coerceIn(0f, 1f) }
                                                            else { val currentWordIndex = line.words.indexOfLast { currentVideoTimeMs >= it.startTimeMs }; if (currentWordIndex == -1) 0f else { val currentWord = line.words[currentWordIndex]; val wordDuration = (currentWord.endTimeMs - currentWord.startTimeMs).coerceAtLeast(1L); val currentWordProgress = ((currentVideoTimeMs - currentWord.startTimeMs).toFloat() / wordDuration).coerceIn(0f, 1f); (currentWordIndex.toFloat() + currentWordProgress) / line.words.size.toFloat() } }
                                                        }
                                                        isPassed -> 1f
                                                        else -> -1f
                                                    }

                                                    val smoothProgress by androidx.compose.animation.core.animateFloatAsState(targetValue = rawProgress, animationSpec = androidx.compose.animation.core.tween(durationMillis = 50, easing = androidx.compose.animation.core.LinearEasing), label = "karaoke_smooth_progress")

                                                    val currentTextColor = if (isActive || isPassed) textColor else textColor.copy(alpha = 0.4f)
                                                    val currentChordColor = when { isActive -> Color(0xFF00E676); isPassed -> customChordColor.copy(alpha = 0.4f); else -> customChordColor }
                                                    val karaokeYellowBg = Color(0xFFFFF9C4)
                                                    val currentBg = if (isActive) karaokeYellowBg.copy(alpha = 0.3f) else Color.Transparent

                                                    val displayKaraokeLine = if (isPronounSwapped) swapPronounsSafe(line.originalTextWithChords) else line.originalTextWithChords
                                                    val finalKaraokeLine = if (isLyricsOnly) formatCleanLyricsOnly(displayKaraokeLine) else formatRawLyrics(displayKaraokeLine)

                                                    Box(modifier = Modifier.fillMaxWidth().background(currentBg, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                                                        ChordTextRenderer(lyrics = finalKaraokeLine, chordColor = currentChordColor, textColor = currentTextColor, offset = transposeOffset, fontSize = if (isActive) fontSizeBody + 2 else fontSizeBody, isChordOverText = if (isLyricsOnly) false else isChordOverText, showRomanNumerals = showRomanNumerals, isEasyChordMode = isEasyChordMode, baseTone = songData.baseTone, karaokeProgress = smoothProgress, onChordClick = { popupChord = it })
                                                    }
                                                }
                                            }
                                        } else if (songData != null) {
                                            val displayNormalLine = if (isPronounSwapped) swapPronounsSafe(songData.lyrics) else songData.lyrics
                                            val finalLyrics = if (isLyricsOnly) formatCleanLyricsOnly(displayNormalLine) else formatRawLyrics(displayNormalLine)
                                            ChordTextRenderer(lyrics = finalLyrics, chordColor = customChordColor, textColor = textColor, offset = transposeOffset, fontSize = fontSizeBody, isChordOverText = if (isLyricsOnly) false else isChordOverText, showRomanNumerals = showRomanNumerals, isEasyChordMode = isEasyChordMode, baseTone = songData.baseTone, onChordClick = { popupChord = it })
                                        }
                                        Spacer(modifier = Modifier.height(20.dp))
                                    }
                                }

                                if (guitarChordsList.isNotEmpty() && !isLyricsOnly) {
                                    SongChordsSummary(lyrics = currentSongData!!.lyrics, transposeOffset = transposeOffset, isEasyChordMode = isEasyChordMode, guitarChordsList = guitarChordsList, tunaGreen = tunaGreen, surfaceColor = surfaceColor, textColor = textColor, currentLang = currentLang, onChordClick = { popupChord = it })
                                }

                                // Banner nam trong noi dung cuon va chi xuat hien sau toan bo
                                // loi bai hat + so do hop am. Khong hien khi dang full-screen,
                                // phat media hoac dung che do Khong lam phien.
                                if (!isFullScreen && !isMediaActive && !isDoNotDisturbMode) {
                                    SongDetailBottomBannerAd(
                                        currentLang = currentLang,
                                        surfaceColor = surfaceColor,
                                        textColor = textColor
                                    )
                                }

                                // Vung trong de nut cuon-len khong de len banner khi o cuoi trang.
                                Spacer(modifier = Modifier.height(92.dp))
                            }
                        } // Kết thúc Column tổng chứa (Header cố định + Nội dung cuộn)

                        val fabAlpha by animateFloatAsState(
                            targetValue = if (scrollState.value > 800) 1f else 0f,
                            label = "fab_alpha"
                        )
                        if (fabAlpha > 0f) {
                            FloatingActionButton(
                                onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } },
                                containerColor = tunaGreen,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).alpha(fabAlpha).size(48.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Top", modifier = Modifier.size(28.dp), tint = Color.White)
                            }
                        }
                        // HIỂN THỊ NÚT THOÁT NẾU ĐANG Ở CHẾ ĐỘ KHÔNG LÀM PHIỀN
                        if (isDoNotDisturbMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp)
                                    .size(44.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .clickable { isDoNotDisturbMode = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Thoát chế độ tối giản",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        if (showSongToneDialog && currentSongData != null) {
                            SongToneDialog(
                                currentLang = currentLang,
                                baseTone = currentSongData!!.baseTone,
                                currentTransposeOffset = transposeOffset,
                                tunaGreen = tunaGreen,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                onDismiss = { showSongToneDialog = false },
                                onApplyOffset = { suggestedOffset ->
                                    // transposeOffset là độ dịch tuyệt đối so với tone gốc của bài hát.
                                    // Toàn bộ hợp âm/lời/thanh tiết tấu hiện có sẽ tự cập nhật theo biến này.
                                    transposeOffset = suggestedOffset
                                    showSongToneDialog = false
                                }
                            )
                        }

                        if (showTutorialSheet && currentSongData != null) {
                            TutorialBottomSheet(
                                songTitle = currentSongData!!.title,
                                songSinger = currentSongData!!.singer,
                                tunaGreen = tunaGreen,
                                bgColor = bgColor,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                currentLang = currentLang, // <--- BỔ SUNG DÒNG NÀY VÀO ĐÂY
                                onDismiss = { showTutorialSheet = false },
                                onVideoClick = { videoId ->
                                    tutorialVideoUrl = "https://www.youtube.com/watch?v=$videoId"
                                    activeMediaType = "YOUTUBE"
                                    showTutorialSheet = false
                                }
                            )
                        }

                        if (showStrummingDialog && currentSongData != null) {
                            val safeBpm = currentSongData!!.bpm.replace("\n", "").replace("\r", "").trim().toFloatOrNull() ?: 120f

                            val extractedChords = Regex("\\[(.*?)\\]")
                                .findAll(currentSongData!!.lyrics)
                                .map { match ->
                                    transposeSingleChord(match.groupValues[1], transposeOffset)
                                }
                                .distinct()
                                .toList()

                            StrummingPatternDialog(
                                songId = currentSongData!!.songId,
                                rhythmName = currentSongData!!.rhythm,
                                bpm = safeBpm,
                                songChords = extractedChords,
                                guitarChordsList = guitarChordsList,
                                surfaceColor = surfaceColor,
                                tunaGreen = tunaGreen,
                                textColor = textColor,
                                onPatternSelected = { patternId, isArp ->
                                    sharedPref.edit().putBoolean("hide_pattern_${currentSongData!!.songId}", false).apply()
                                    dbRefreshTrigger++
                                },
                                onDismiss = { showStrummingDialog = false }
                            )
                        }

                        // --- POPUP CÀI ĐẶT TỐC ĐỘ BPM ---
                        if (showBpmSettingsDialog) {
                            BpmSettingsDialogView(
                                adjustableBpm = adjustableBpm,
                                onAdjustableBpmChange = { adjustableBpm = it },
                                selectedTimeSignature = selectedTimeSignature,
                                onSelectedTimeSignatureChange = { selectedTimeSignature = it },
                                isFootTapping = isFootTapping,
                                onFootTappingChange = { isFootTapping = it },
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                tunaGreen = tunaGreen,
                                onDismiss = { showBpmSettingsDialog = false }
                            )
                        }
                    }
                } else if (searchResults != null) {
                    // Gọi hàm Component đã được tách ra để tránh lỗi MethodTooLargeException
                    FavoriteSongsListView(
                        searchResults = searchResults!!,
                        urlInputText = urlInput.text,
                        currentLang = currentLang,
                        textColor = textColor,
                        bgColor = bgColor,
                        surfaceColor = surfaceColor,
                        tunaGreen = tunaGreen,
                        currentUserEmail = currentUserEmail,
                        userRole = userRole,
                        sharedPref = sharedPref,
                        onOpenDetail = { item, mode ->
                            keyboardController?.hide(); focusManager.clearFocus()
                            selectedDisplayMode = mode; isFetchingDetail = true; transposeOffset = 0; isFullScreen = false
                            addToHistory("${item.url}|${item.title}")
                            coroutineScope.launch {
                                val localDetail = songRepository.getSongDetailsLocal(
                                    songId = item.url,
                                    songTitle = item.title,
                                    singer = item.singer,
                                    isLikedByDevice = sharedPref.getBoolean("liked_${item.url}", false)
                                )
                                currentSongData = localDetail
                                    ?: SongScraper.fetchSongDetails(item.url, currentUserEmail)
                                isFetchingDetail = false
                            }
                        },
                        onReport = { reportInfo = it },
                        onChordPopup = { popupChord = it },
                        onDeleteClick = { url ->
                            coroutineScope.launch {
                                val successOnline = SongScraper.deleteSong(url)
                                val successOffline = songRepository.deleteSongOffline(url)
                                if (successOnline || successOffline) { Toast.makeText(context, CoreTranslator.getString("delete_success", currentLang), Toast.LENGTH_SHORT).show(); performSearch() }
                                else { Toast.makeText(context, CoreTranslator.getString("delete_error", currentLang), Toast.LENGTH_SHORT).show() }
                            }
                        }
                    )
                } else {
                    // Gọi hàm Component Trang Khám Phá đã được tách ra
                    DiscoveryScreenView(
                        currentLang = currentLang, textColor = textColor, bgColor = bgColor, surfaceColor = surfaceColor, tunaGreen = tunaGreen, trendingSongs = trendingSongs, suggestedSongs = suggestedSongs, favoriteArtists = favoriteArtists, nhacTre8x9xArtists = nhacTre8x9xArtists, nhacTreGenZArtists = nhacTreGenZArtists, boleroKinhDienArtists = boleroKinhDienArtists, boleroTiepNoiArtists = boleroTiepNoiArtists, top50Songs = top50Songs, isTrendingExpanded = isTrendingExpanded, onTrendingExpandedToggle = { isTrendingExpanded = !isTrendingExpanded },
                        onSongClick = { song, mode ->
                            addToHistory("${song.id}|${song.title}")
                            selectedDisplayMode = mode
                            isFetchingDetail = true
                            transposeOffset = 0
                            isFullScreen = false
                            coroutineScope.launch {
                                currentSongData = songRepository.getSongDetailsLocal(
                                    song = song,
                                    isLikedByDevice = sharedPref.getBoolean("liked_${song.id}", false)
                                )
                                isFetchingDetail = false
                            }
                        },
                        onArtistClick = { artistName ->
                            val cleanName = artistName.removePrefix("ca sĩ:").removePrefix("ca si:").removePrefix("Ca sĩ:").removePrefix("Ca si:").trim()
                            urlInput = TextFieldValue("Ca sĩ: $cleanName")
                            isBroadSearch = true
                            isArtistFilter = true // Bật cờ lọc chuẩn xác theo ca sĩ
                            isSearchingOffline = true
                            offlineSearchQuery = cleanName
                        },
                        onReport = { reportInfo = it }, onChordPopup = { popupChord = it },
                        onEditSong = { songDetails ->
                            if (songDetails.songId.isBlank() || songDetails.songId.startsWith("local_")) {
                                // Mở form tạo/sửa bản hợp âm cục bộ
                                customTemplateSongData = songDetails
                                showAddDialog = true
                            } else {
                                // Đây là hành động Sửa bài hát có sẵn trên Server
                                editingSongData = songDetails
                                showEditDialog = true
                            }
                        },
                        onDeleteClick = { song ->
                            coroutineScope.launch {
                                val successOnline = SongScraper.deleteSong(song.id)
                                val successOffline = songRepository.deleteSongOffline(song.id)
                                if (successOnline || successOffline) { Toast.makeText(context, CoreTranslator.getString("song_deleted_success", currentLang), Toast.LENGTH_SHORT).show(); dbRefreshTrigger++ }
                                else { Toast.makeText(context, CoreTranslator.getString("delete_error", currentLang), Toast.LENGTH_SHORT).show() }
                            }
                        },
                        currentUserEmail = currentUserEmail, userRole = userRole, sharedPref = sharedPref
                    )
                }
                // =============================================================
                // --- BẮT ĐẦU LAYER NỔI KẾT QUẢ TÌM KIẾM OFFLINE ---
                // =============================================================
                androidx.compose.animation.AnimatedVisibility(
                    // ĐÃ SỬA LỖI TẠI ĐÂY: Thêm điều kiện ẩn Layer đi khi người dùng bấm xem bài hát
                    visible = offlineSearchQuery.isNotBlank() && currentSongData == null && !isFetchingDetail,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
                        initialOffsetY = { 50 }),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { 50 }),
                    modifier = Modifier.fillMaxSize()
                        .zIndex(9f) // zIndex cao để đè lên Layout Khám phá
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(bgColor) // Nền màu tối che đi màn hình cũ
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // HEADER TÌM KIẾM
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${CoreTranslator.getString("offline_results", currentLang)} (${offlineSongs.size})",
                                    color = tunaGreen,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                // Nút Thoát (Đóng)
                                Box(
                                    modifier = Modifier.size(28.dp)
                                        .background(Color.Red.copy(alpha = 0.1f), CircleShape)
                                        .clickable {
                                            urlInput = TextFieldValue("") // Xóa chữ
                                            offlineSearchQuery = "" // Xóa truy vấn
                                            isArtistFilter = false // Reset bộ lọc ca sĩ
                                            focusManager.clearFocus() // Hủy trỏ chuột
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Thoát",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // DANH SÁCH KẾT QUẢ
                            if (offlineSongs.isEmpty() && (!isSearchTimeout || isSearchingOffline)) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // DÙNG SÓNG ÂM THAY CHO VÒNG TRÒN
                                        AudioWaveLoadingIndicator(
                                            color = tunaGreen,
                                            modifier = Modifier.height(36.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (currentLang == "vi") "Đang tìm bài hát..." else "Searching songs...",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else if (offlineSongs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        CoreTranslator.getString("not_found_any_song", currentLang),
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(offlineSongs.size) { index ->
                                        val song = offlineSongs[index]
                                        val openSongDetail: (String) -> Unit = { mode ->
                                            keyboardController?.hide(); focusManager.clearFocus()
                                            selectedDisplayMode = mode; isFetchingDetail = true; transposeOffset = 0; isFullScreen = false

                                            // ĐÃ THÊM: Lưu ID và tên bài hát vào lịch sử khi click
                                            addToHistory("${song.id}|${song.title}")

                                            coroutineScope.launch {
                                                currentSongData = songRepository.getSongDetailsLocal(
                                                    song = song,
                                                    isLikedByDevice = sharedPref.getBoolean("liked_${song.id}", false)
                                                )
                                                isFetchingDetail = false
                                            }
                                        }

                                        // GỌI COMPONENT THẺ BÀI HÁT TỪ BÊN NGOÀI VÀO
                                        SongListItemCard(
                                            song = song,
                                            indexText = null,
                                            containerColor = surfaceColor,
                                            surfaceColor = surfaceColor,
                                            currentLang = currentLang,
                                            tunaGreen = tunaGreen,
                                            bgColor = bgColor,
                                            textColor = textColor,
                                            deviceId = currentUserEmail,
                                            sharedPref = sharedPref,
                                            userRole = userRole,
                                            songRepository = songRepository,
                                            refreshTrigger = dbRefreshTrigger,
                                            onOpenDetail = openSongDetail,
                                            onOpenCustomDetail = { customDetail ->
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                selectedDisplayMode = "CHORD"
                                                isFetchingDetail = false
                                                transposeOffset = 0
                                                isFullScreen = false
                                                addToHistory(customDetail.title)
                                                currentSongData = customDetail
                                            },
                                            onReport = { reportInfo = ReportInfo(song.id, song.title, song.ca_si, song.tac_gia) },
                                            onChordPopup = { popupChord = it },
                                            onEditClick = { songDetails ->
                                                if (songDetails.songId.isBlank() || songDetails.songId.startsWith("local_")) {
                                                    customTemplateSongData = songDetails
                                                    showAddDialog = true
                                                } else {
                                                    editingSongData = songDetails
                                                    showEditDialog = true
                                                }
                                            },
                                            onDeleteClick = {
                                                coroutineScope.launch {
                                                    val successOnline = SongScraper.deleteSong(song.id)
                                                    val successOffline = songRepository.deleteSongOffline(song.id)

                                                    if (successOnline || successOffline) {
                                                        Toast.makeText(context, CoreTranslator.getString("delete_success", currentLang), Toast.LENGTH_SHORT).show()
                                                        dbRefreshTrigger++
                                                    } else {
                                                        Toast.makeText(context, CoreTranslator.getString("delete_permission_error", currentLang), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    item { Spacer(modifier = Modifier.height(80.dp)) }
                                }
                            }
                        }
                    }
                }

                // =============================================================
                // --- GỌI COMPONENT LAYER NỔI LỊCH SỬ TÌM KIẾM TỪ BÊN NGOÀI ---
                // =============================================================
                SearchHistoryOverlay(
                    isSearchFocused = isSearchFocused,
                    urlInputText = urlInput.text,
                    searchHistory = searchHistory,
                    suggestSongs = if (
                        offlineSearchQuery == urlInput.text.trim()
                    ) {
                        offlineSongs.take(15)
                    } else {
                        emptyList()
                    },
                    songRepository = songRepository,
                    currentLang = currentLang,
                    tunaGreen = tunaGreen,
                    surfaceColor = surfaceColor,
                    bgColor = bgColor,
                    textColor = textColor,
                    onClose = { focusManager.clearFocus() },
                    onClearAll = {
                        searchHistory = emptyList()
                        historyPref.edit().putStringSet("history", emptySet()).apply()
                    },
                    onDeleteQuery = { query ->
                        val newHistory = searchHistory.filter { it != query }
                        searchHistory = newHistory
                        historyPref.edit().putStringSet("history", newHistory.toSet()).apply()
                    },
                    onQuerySelected = { query, topMatch ->
                        urlInput = TextFieldValue(query, TextRange(query.length))
                        focusManager.clearFocus()

                        isBroadSearch = false
                        offlineSearchQuery = ""
                        searchResults = null
                        isFetchingDetail = true
                        transposeOffset = 0
                        isFullScreen = false

                        coroutineScope.launch {
                            val songId = topMatch?.id ?: ""
                            // Luôn truy vấn bản ghi đầy đủ từ SQLite bằng getSongByExactIdOrTitle
                            val targetSong = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                songRepository.getSongByExactIdOrTitle(songId, query)
                            }

                            if (targetSong != null) {
                                currentSongData = SongDetails(
                                    title = targetSong.title,
                                    singer = targetSong.ca_si,
                                    composer = targetSong.tac_gia,
                                    baseTone = targetSong.tong,
                                    rhythm = targetSong.dieu.ifBlank { targetSong.rhythm },
                                    bpm = if (targetSong.bpm > 0) targetSong.bpm.toString() else "",
                                    lyrics = targetSong.lyrics.joinToString("\n"),
                                    videoUrl = targetSong.youtubeUrl,
                                    songId = targetSong.id,
                                    slug = targetSong.slug,
                                    imageCount = targetSong.image_count,
                                    likes = targetSong.favoriteCount,
                                    isLikedByDevice = sharedPref.getBoolean("liked_${targetSong.id}", false),
                                    zingMp3Id = targetSong.zingMp3Id
                                )

                                selectedDisplayMode =
                                    if (targetSong.image_count > 0 && targetSong.chordList.isEmpty()) {
                                        "SHEET"
                                    } else {
                                        "CHORD"
                                    }
                            } else {
                                // Nếu không tìm thấy bài trong SQLite thì quay về tìm kiếm offline
                                currentSongData = null
                                isBroadSearch = true
                                offlineSearchQuery = query.trim()
                            }
                            isFetchingDetail = false
                        }
                    }
                )

            }
        }
        // --- OVERLAY: XỬ LÝ PHÓNG TO FULLSCREEN YOUTUBE ---
        if (customVideoView != null) {
            Dialog(
                onDismissRequest = {
                    customVideoCallback?.onCustomViewHidden()
                    customVideoView = null
                    customVideoCallback = null
                },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false // Tràn qua cả thanh trạng thái
                )
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // Chèn View video vào toàn màn hình
                    AndroidView(
                        factory = { customVideoView!! },
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = {
                            customVideoCallback?.onCustomViewHidden()
                            customVideoView = null
                            customVideoCallback = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Thoát", tint = Color.White)
                    }
                }
            }
        }
        if (isTranslating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // Chặn click xuyên qua
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = tunaGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = CoreTranslator.getString("translating", currentLang),
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        // ==========================================
        // DIALOG: YÊU CẦU ĐỒNG Ý BẢN QUYỀN (GỌI TỪ COMPONENT NGOÀI)
        // ==========================================
        if (showAddPolicyDialog) {
            CopyrightPolicyDialog(
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                surfaceColor = surfaceColor,
                textColor = textColor,
                onDismiss = { showAddPolicyDialog = false },
                onAccept = {
                    hasAcceptedPolicyThisSession = true
                    showAddPolicyDialog = false
                    showAddDialog = true
                }
            )
        }

        // --- FORM BÁO CÁO BÀI HÁT ---
        if (reportInfo != null) {
            ReportSongDialogModal(
                reportInfo = reportInfo!!,
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                textColor = textColor,
                surfaceColor = surfaceColor,
                deviceId = deviceId,
                onDismiss = { reportInfo = null }
            )
        }

        // --- POPUP HỢP ÂM ---
        if (popupChord != null) {
            ChordPopupDialog(
                chordQuery = popupChord!!.replace("[", "").replace("]", "").trim(),
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                textColor = textColor,
                tunaGreen = tunaGreen,
                currentLang = currentLang,
                onDismiss = { popupChord = null })
        }

        // --- POPUP NỘI DUNG THÔNG BÁO ---
        if (showNotificationDialog) {
            AlertDialog(
                onDismissRequest = {
                    // Người dùng bấm ra ngoài -> Đóng Popup nhưng vẫn giữ lại Chuông
                    showNotificationDialog = false
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFFC107))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Thông báo hệ thống", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        // Nút X để đóng tạm thời (Xem lại sau)
                        IconButton(
                            onClick = { showNotificationDialog = false },
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = 12.dp) // Dịch nhẹ sang phải cho cân đối
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                text = {
                    Text(
                        text = notificationMessage,
                        color = textColor,
                        lineHeight = 22.sp // Tăng khoảng cách dòng cho dễ đọc hơn
                    )
                },
                containerColor = surfaceColor,
                confirmButton = {
                    Button(
                        onClick = {
                            // Bấm "Đã hiểu" -> Xóa chuông và đánh dấu là đã đọc
                            showNotificationDialog = false
                            hasUnreadNotification = false
                            sharedPref.edit().putInt("last_read_notification_id", latestNotificationId).apply()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                    ) {
                        Text("Đã hiểu", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // --- CHỨC NĂNG DÀNH RIÊNG CHO ADMIN TỐI CAO ---
        if (showAdminActionSelector) {
            AlertDialog(
                onDismissRequest = { showAdminActionSelector = false },
                modifier = Modifier.border(1.5.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                title = { Text("Chức năng Quản Trị Viên", color = tunaGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                showAdminActionSelector = false
                                if (hasAcceptedPolicyThisSession) showAddDialog = true else showAddPolicyDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            // Dùng fillMaxWidth và Arrangement.Start để đẩy Icon sát lề trái
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color(0xFFCE93D8)) // Màu tím nhẹ
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Thêm bài hát mới", color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                showAdminActionSelector = false
                                showPostNotificationDialog = true
                            },
                            // Đồng bộ nền xám viền mờ giống nút trên để icon xanh lá nhẹ nổi bật hơn
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            // Dùng fillMaxWidth và Arrangement.Start để đẩy Icon sát lề trái
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFA5D6A7)) // Màu xanh lá nhẹ
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Đăng thông báo hệ thống", color = textColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                containerColor = bgColor,
                confirmButton = {
                    TextButton(onClick = { showAdminActionSelector = false }) {
                        Text("Đóng", color = Color.Gray)
                    }
                }
            )
        }

        if (showPostNotificationDialog) {
            var notifContent by remember { mutableStateOf(TextFieldValue("")) }
            var isPostingNotif by remember { mutableStateOf(false) }
            var sendPushNotification by remember { mutableStateOf(true) } // Cờ gửi thông báo ngầm

            AlertDialog(
                onDismissRequest = { if (!isPostingNotif) showPostNotificationDialog = false },
                title = { Text("Nội dung thông báo", color = tunaGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Viết nội dung thông báo gửi đến tất cả người dùng:", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = notifContent,
                            onValueChange = { notifContent = it },
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            textStyle = TextStyle(color = textColor),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = surfaceColor,
                                unfocusedContainerColor = surfaceColor,
                                focusedIndicatorColor = tunaGreen
                            ),
                            placeholder = { Text("Ví dụ: App vừa cập nhật tính năng...", color = Color.Gray.copy(alpha = 0.7f)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Checkbox chọn chế độ thông báo ngầm (Push Notification)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { sendPushNotification = !sendPushNotification }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sendPushNotification,
                                onCheckedChange = { sendPushNotification = it },
                                colors = CheckboxDefaults.colors(checkedColor = tunaGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đẩy thông báo nổi ngoài màn hình (Push Notification)", color = textColor, fontSize = 13.sp)
                        }
                    }
                },
                containerColor = bgColor,
                confirmButton = {
                    Button(
                        onClick = {
                            if (notifContent.text.isBlank()) return@Button
                            isPostingNotif = true
                            coroutineScope.launch {
                                // Truyền cờ sendPushNotification xuống API
                                val success = SongScraper.postSystemNotification(notifContent.text.trim(), sendPushNotification)
                                if (success) {
                                    Toast.makeText(context, "Đã gửi thông báo thành công!", Toast.LENGTH_SHORT).show()
                                    showPostNotificationDialog = false
                                    // Tự động ép app nhận thông báo vừa đăng ngay lập tức
                                    latestNotificationId += 1
                                    notificationMessage = notifContent.text.trim()
                                    hasUnreadNotification = true

                                    // Bắt đầu tính 48h cho thông báo admin vừa đăng
                                    sharedPref.edit().putLong("notif_time_$latestNotificationId", System.currentTimeMillis()).apply()
                                } else {
                                    Toast.makeText(context, "Lỗi khi gửi thông báo", Toast.LENGTH_SHORT).show()
                                }
                                isPostingNotif = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                    ) {
                        if (isPostingNotif) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Gửi đi", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (!isPostingNotif) showPostNotificationDialog = false }) {
                        Text("Hủy", color = Color.Gray)
                    }
                }
            )
        }

        // --- BẮT ĐẦU THÊM MỚI: POPUP FORM THÊM MỚI BÀI HÁT ---
        if (showAddDialog) {
            AddSongDialogModal(
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                textColor = textColor,
                surfaceColor = surfaceColor,
                userRole = userRole,
                isStaff = isStaff,
                initialSongData = customTemplateSongData,
                songRepository = songRepository,
                onDismiss = {
                    showAddDialog = false
                    customTemplateSongData = null
                },
                onSuccess = { newTitle ->
                    showAddDialog = false
                    customTemplateSongData = null
                    dbRefreshTrigger++

                    // Tự động load và hiển thị ngay nội dung bản hợp âm vừa sửa
                    coroutineScope.launch {
                        val updatedLocalSong = songRepository.getCustomSongLocalByTitle(newTitle)
                        if (updatedLocalSong != null) {
                            currentSongData = SongDetails(
                                title = updatedLocalSong.title,
                                singer = updatedLocalSong.ca_si,
                                composer = updatedLocalSong.tac_gia,
                                baseTone = updatedLocalSong.tong,
                                rhythm = updatedLocalSong.dieu.ifBlank { updatedLocalSong.rhythm },
                                bpm = if (updatedLocalSong.bpm > 0) updatedLocalSong.bpm.toString() else "",
                                lyrics = updatedLocalSong.lyrics.joinToString("\n"),
                                videoUrl = updatedLocalSong.youtubeUrl,
                                songId = updatedLocalSong.id,
                                slug = updatedLocalSong.slug,
                                imageCount = updatedLocalSong.image_count,
                                likes = updatedLocalSong.favoriteCount,
                                isLikedByDevice = sharedPref.getBoolean("liked_${updatedLocalSong.id}", false),
                                zingMp3Id = updatedLocalSong.zingMp3Id
                            )
                        }
                    }
                }
            )
        }
        // --- KẾT THÚC THÊM MỚI ---

        val targetEditSong = editingSongData ?: currentSongData
        if (showEditDialog && targetEditSong != null) {
            EditSongDialogModal(
                currentSongData = targetEditSong,
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                textColor = textColor,
                surfaceColor = surfaceColor,
                onDismiss = {
                    showEditDialog = false
                    editingSongData = null
                },
                onSuccess = {
                    showEditDialog = false
                    val savedSongId = targetEditSong.songId
                    editingSongData = null
                    isFetchingDetail = true

                    coroutineScope.launch {
                        // 1. Ép đồng bộ dữ liệu sửa về SQLite
                        songRepository.syncNewSongsOnline()

                        // 2. Làm mới giao diện
                        dbRefreshTrigger++

                        // 3. Tải lại chi tiết bài hát nếu đang ở màn hình chi tiết
                        if (currentSongData != null && currentSongData!!.songId == savedSongId) {
                            currentSongData = SongScraper.fetchSongDetails(savedSongId, currentUserEmail)
                        }
                        isFetchingDetail = false
                        Toast.makeText(context, CoreTranslator.getString("save_sync_success", currentLang), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        // --- POPUP: CHỌN CHẾ ĐỘ PHÁT NHẠC ---
        if (showPlayModeDialog) { // <-- LƯU Ý: Bỏ đi điều kiện "&& currentSongData != null"
            PlayModeSelectionDialog(
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                bgColor = bgColor,
                surfaceColor = surfaceColor,
                userRole = userRole,
                textColor = textColor,
                onDismiss = { showPlayModeDialog = false },
                songRepository = songRepository, // Truyền Repo để lấy danh sách offline
                onSongSelected = { newSong ->
                    // Đổi sang bài mới ngay lập tức khi khách chọn
                    currentSongData = newSong
                    selectedDisplayMode = "CHORD"

                    // THÊM DÒNG NÀY: Tự động tắt cảnh báo màu vàng để không làm phiền người dùng
                    hasNewUpdate = false
                },
                onSelectMode = { mode, isFromCache ->
                    showPlayModeDialog = false
                    isFastLoadingCache = isFromCache
                    aiAnalysisMode = mode // <-- LƯU LẠI CHẾ ĐỘ ĐANG CHẠY

                    // Tắt nhạc cũ an toàn trước khi chạy Mode mới
                    try {
                        if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
                        mediaPlayer?.release()
                    } catch (e: Exception) {}
                    mediaPlayer = null
                    isAudioPlaying = false

                    when (mode) {
                        "ZING_ACAPELLA_SYNC", "ZING_ACAPELLA_NO_LYRICS" -> {
                            syncedKaraokeLines = null
                            activeMediaType = ""
                            isAiAnalyzing = true
                            aiProgress = 0.1f
                            aiAnalysisStatus = "Đang tải dữ liệu..."

                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val responseObj = SongScraper.getZingAcapellaSync(
                                    currentSongData!!.zingMp3Id,
                                    currentSongData!!.title,
                                    currentSongData!!.singer,
                                    SongScraper.getLyricsSample(currentSongData!!.lyrics),
                                    mode // Truyền chế độ xuống Python để map % chuẩn xác
                                ) { progress, message ->
                                    aiProgress = progress
                                    aiAnalysisStatus = message
                                }

                                if (responseObj != null && responseObj.optBoolean("success", false)) {
                                    val filename = responseObj.optString("acapella_filename", "")

                                    // Tự động lưu Zing ID mới vào CSDL nếu trước đó chưa có
                                    val returnedZingId = responseObj.optString("id", "")
                                    if (returnedZingId.isNotBlank()) {
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            // LUÔN LUÔN ép bắn lên Server để "chữa lành" những dòng NULL trên MySQL
                                            SongScraper.updateZingIdOnline(currentSongData!!.songId, returnedZingId)

                                            // Cập nhật lại SQLite cục bộ nếu phát hiện lệch dữ liệu
                                            if (currentSongData!!.zingMp3Id != returnedZingId) {
                                                songRepository?.updateZingIdOffline(currentSongData!!.songId, returnedZingId)
                                            }
                                        }
                                        // Cập nhật lại giao diện UI
                                        if (currentSongData!!.zingMp3Id != returnedZingId) {
                                            currentSongData = currentSongData!!.copy(zingMp3Id = returnedZingId)
                                        }
                                    }

                                    // ========================================================
                                    // TAB 2: KARAOKE (CẦN TẢI AUDIO + XỬ LÝ GHÉP JSON)
                                    // ========================================================
                                    if (mode == "ZING_ACAPELLA_SYNC") {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            aiProgress = 0.8f
                                            aiAnalysisStatus = "Đang đồng bộ lời vào hợp âm..."
                                        }

                                        val zingCaptions = mutableListOf<YoutubeCaptionEvent>()
                                        try {
                                            val karaokeData = responseObj.optJSONObject("karaoke_data")
                                            val dataObj = karaokeData?.optJSONObject("data")
                                            val sentences = dataObj?.optJSONArray("sentences")

                                            if (sentences != null) {
                                                for (i in 0 until sentences.length()) {
                                                    val sentenceObj = sentences.getJSONObject(i)
                                                    val wordsArr = sentenceObj.optJSONArray("words")
                                                    val aiWords = mutableListOf<AiWordTime>()
                                                    var sentenceText = ""
                                                    var sStart = 0L
                                                    var sEnd = 0L

                                                    if (wordsArr != null) {
                                                        for (j in 0 until wordsArr.length()) {
                                                            val wObj = wordsArr.getJSONObject(j)
                                                            val wData = wObj.optString("data", "")
                                                            val wStart = wObj.optLong("startTime", 0L)
                                                            val wEnd = wObj.optLong("endTime", 0L)

                                                            sentenceText += wData
                                                            aiWords.add(AiWordTime(wData.trim(), wStart, wEnd))

                                                            if (j == 0) sStart = wStart
                                                            if (j == wordsArr.length() - 1) sEnd = wEnd
                                                        }
                                                    }
                                                    if (aiWords.isNotEmpty()) {
                                                        zingCaptions.add(YoutubeCaptionEvent(sStart, sEnd, sentenceText.trim(), aiWords))
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) { e.printStackTrace() }

                                        if (zingCaptions.isNotEmpty()) {
                                            // ĐÃ SỬA: Ép gọn lời bài hát bằng formatRawLyrics trước khi đưa vào AI phân tích
                                            val compactedLyrics = formatRawLyrics(currentSongData!!.lyrics)
                                            val mappedLines = robustMapTimingLocal(compactedLyrics, zingCaptions)
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                aiProgress = 1.0f
                                                syncedKaraokeLines = mappedLines
                                                isAiAnalyzing = false

                                                mediaPlayer?.release()
                                                mediaPlayer = android.media.MediaPlayer().apply {
                                                    setDataSource("https://api.tunertools.top/api/stream_cache/$filename")
                                                    prepare()
                                                    start()
                                                }

                                                totalAudioDurationMs = mediaPlayer!!.duration.toLong()
                                                currentVideoTimeMs = 0L
                                                syncOffsetMs = 0L
                                                isAudioPlaying = true
                                                activeMediaType = "ZING_KARAOKE"
                                            }
                                        } else {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isAiAnalyzing = false
                                                Toast.makeText(context, "Không đọc được dữ liệu Karaoke từ Zing", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    // ========================================================
                                    // TAB 1: CA SĨ (CHỈ TẢI AUDIO, ÉP BỎ QUA JSON HOÀN TOÀN)
                                    // ========================================================
                                    else {
                                        // KHẮC PHỤC LỖI: Tải audio trên IO Thread và ép phát ngay lập tức
                                        try {
                                            val targetPath = "https://api.tunertools.top/api/stream_cache/$filename"
                                            val mp = android.media.MediaPlayer().apply {
                                                setDataSource(targetPath)
                                                prepare()
                                            }
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                aiProgress = 1.0f
                                                syncedKaraokeLines = null // Ép bằng null để giao diện không tải tính năng chữ chạy
                                                isAiAnalyzing = false

                                                mediaPlayer?.release()
                                                mediaPlayer = mp

                                                // Bắt buộc gọi start() trên MediaPlayer trước khi cập nhật các biến UI
                                                mp.start()

                                                // Cập nhật ngay các biến theo dõi thời gian và trạng thái
                                                totalAudioDurationMs = mp.duration.toLong()
                                                currentVideoTimeMs = mp.currentPosition.toLong()
                                                syncOffsetMs = 0L
                                                isAudioPlaying = mp.isPlaying

                                                // Cập nhật loại Media ĐỂ GIAO DIỆN HIỂN THỊ THANH TUA
                                                activeMediaType = "ACAPELLA_AUDIO"
                                            }
                                        } catch (e: Exception) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isAiAnalyzing = false
                                                Toast.makeText(context, "Lỗi phát âm thanh: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    val errorMsg = responseObj?.optString("error") ?: "Không thể kết nối"
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isAiAnalyzing = false
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        "ZING_MP3" -> {
                            syncedKaraokeLines = null
                            activeMediaType = ""

                            isAiAnalyzing = true
                            aiProgress = 0.1f // Khởi tạo 10%
                            aiAnalysisStatus = "Đang tìm kiếm dữ liệu lời bài hát (10%)..."

                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val responseObj = SongScraper.getZingMp3Karaoke(currentSongData!!.zingMp3Id, currentSongData!!.title, currentSongData!!.singer, SongScraper.getLyricsSample(currentSongData!!.lyrics))

                                if (responseObj != null && responseObj.optBoolean("success", false)) {
                                    // Tự động lưu Zing ID mới vào CSDL nếu trước đó chưa có
                                    val returnedZingId = responseObj.optString("id", "")
                                    if (returnedZingId.isNotBlank()) {
                                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                            // LUÔN LUÔN ép bắn lên Server để "chữa lành" những dòng NULL trên MySQL
                                            SongScraper.updateZingIdOnline(currentSongData!!.songId, returnedZingId)

                                            // Cập nhật lại SQLite cục bộ nếu phát hiện lệch dữ liệu
                                            if (currentSongData!!.zingMp3Id != returnedZingId) {
                                                songRepository?.updateZingIdOffline(currentSongData!!.songId, returnedZingId)
                                            }
                                        }
                                        // Cập nhật lại giao diện UI
                                        if (currentSongData!!.zingMp3Id != returnedZingId) {
                                            currentSongData = currentSongData!!.copy(zingMp3Id = returnedZingId)
                                        }
                                    }
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        aiProgress = 0.5f // Chuyển sang bước Đồng bộ (50%)
                                        aiAnalysisStatus = "Đang đồng bộ lời vào hợp âm (50%)..."
                                    }

                                    val zingCaptions = mutableListOf<YoutubeCaptionEvent>()
                                    try {
                                        val karaokeData = responseObj.optJSONObject("karaoke_data")
                                        val dataObj = karaokeData?.optJSONObject("data")
                                        val sentences = dataObj?.optJSONArray("sentences")

                                        if (sentences != null) {
                                            for (i in 0 until sentences.length()) {
                                                val sentenceObj = sentences.getJSONObject(i)
                                                val wordsArr = sentenceObj.optJSONArray("words")
                                                val aiWords = mutableListOf<AiWordTime>()
                                                var sentenceText = ""
                                                var sStart = 0L
                                                var sEnd = 0L

                                                if (wordsArr != null) {
                                                    for (j in 0 until wordsArr.length()) {
                                                        val wObj = wordsArr.getJSONObject(j)
                                                        val wData = wObj.optString("data", "")
                                                        val wStart = wObj.optLong("startTime", 0L)
                                                        val wEnd = wObj.optLong("endTime", 0L)

                                                        sentenceText += wData
                                                        aiWords.add(AiWordTime(wData.trim(), wStart, wEnd))

                                                        if (j == 0) sStart = wStart
                                                        if (j == wordsArr.length() - 1) sEnd = wEnd
                                                    }
                                                }
                                                if (aiWords.isNotEmpty()) {
                                                    zingCaptions.add(YoutubeCaptionEvent(sStart, sEnd, sentenceText.trim(), aiWords))
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    if (zingCaptions.isNotEmpty()) {
                                        // ĐÃ SỬA: Ép gọn lời bài hát bằng formatRawLyrics trước khi đưa vào AI phân tích
                                        val compactedLyrics = formatRawLyrics(currentSongData!!.lyrics)
                                        val mappedLines = robustMapTimingLocal(compactedLyrics, zingCaptions)

                                        // --- BẮT ĐẦU: IN LOG TOÀN BỘ BÀI HÁT ---
                                        android.util.Log.d("KaraokeLog_FullSong", "=== BẮT ĐẦU DỮ LIỆU TOÀN BÀI ===")
                                        mappedLines.forEachIndexed { index, line ->
                                            android.util.Log.d(
                                                "KaraokeLog_FullSong",
                                                "[Dòng ${index + 1}] ${line.originalTextWithChords} | ${line.startTimeMs}ms -> ${line.endTimeMs}ms"
                                            )
                                        }
                                        android.util.Log.d("KaraokeLog_FullSong", "=== KẾT THÚC DỮ LIỆU TOÀN BÀI ===")
                                        // --- KẾT THÚC: IN LOG ---

                                        // --- BẮT ĐẦU: IN LOG TOÀN BỘ BÀI HÁT ---
                                        android.util.Log.d("KaraokeLog_FullSong", "=== BẮT ĐẦU DỮ LIỆU TOÀN BÀI ===")
                                        mappedLines.forEachIndexed { index, line ->
                                            android.util.Log.d(
                                                "KaraokeLog_FullSong",
                                                "[Dòng ${index + 1}] ${line.originalTextWithChords} | ${line.startTimeMs}ms -> ${line.endTimeMs}ms"
                                            )
                                        }
                                        android.util.Log.d("KaraokeLog_FullSong", "=== KẾT THÚC DỮ LIỆU TOÀN BÀI ===")
                                        // --- KẾT THÚC: IN LOG ---
                                        val lastTime = mappedLines.lastOrNull()?.endTimeMs ?: 0L

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            aiProgress = 1.0f
                                            syncedKaraokeLines = mappedLines
                                            isAiAnalyzing = false

                                            // Bơm dữ liệu vào Máy đếm ảo để chạy Karaoke
                                            totalAudioDurationMs = lastTime + 5000L // Thêm 5 giây Outro
                                            currentVideoTimeMs = 0L
                                            syncOffsetMs = 0L
                                            isAudioPlaying = true
                                            activeMediaType = "ZING_KARAOKE"
                                        }
                                    } else {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isAiAnalyzing = false
                                            Toast.makeText(context, "Bài hát này chưa thể phân tích", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isAiAnalyzing = false
                                        Toast.makeText(context, "Không lấy được dữ liệu Karaoke bài hát này", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        "CHORD_AI" -> {
                            showChordAI = true
                        }
                        // CHỈ CÒN 3 MODE: Giọng Ca sĩ + Chữ, Chỉ Giọng ca sĩ, Chỉ chạy chữ
                        "ACAPELLA", "ACAPELLA_NO_LYRICS", "LYRICS_ONLY" -> {
                            val targetMedia = when (mode) {
                                "ACAPELLA", "ACAPELLA_NO_LYRICS" -> "ACAPELLA_AUDIO"
                                "LYRICS_ONLY" -> "LYRICS_ONLY_AUDIO"
                                else -> "ACAPELLA_AUDIO"
                            }

                            if (activeMediaType != targetMedia) {
                                isAiAnalyzing = true
                                aiProgress = 0.1f // 👈 BẮT ĐẦU: 10%
                                aiAnalysisStatus = CoreTranslator.getString("ai_checking_storage", currentLang)
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val t0_StartTime = System.currentTimeMillis()
                                    android.util.Log.d("KaraokeLog", "========================================")
                                    android.util.Log.d("KaraokeLog", "🚀 BẮT ĐẦU XỬ LÝ CHẾ ĐỘ: $mode")

                                    val client = okhttp3.OkHttpClient.Builder()
                                        .connectTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                                        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                                        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
                                        .build()

                                    val rawUrl = currentSongData!!.videoUrl
                                    var videoId = rawUrl
                                    if (rawUrl.contains("watch?v=")) videoId = rawUrl.substringAfter("watch?v=").substringBefore("&")
                                    else if (rawUrl.contains("youtu.be/")) videoId = rawUrl.substringAfter("youtu.be/").substringBefore("?")
                                    videoId = videoId.trim()

                                    try {
                                        // ========================================================
                                        // BƯỚC 0: TẦNG CACHE ĐIỆN THOẠI (LOCAL CACHE)
                                        // ========================================================
                                        val targetFile = java.io.File(context.cacheDir, "track_${videoId}_${targetMedia}.wav")
                                        val audioFile = java.io.File(context.cacheDir, "temp_audio_$videoId.m4a")

                                        val hasLocalAudio = targetFile.exists() && targetFile.length() > 100 * 1024
                                        if (hasLocalAudio) {
                                            android.util.Log.d("KaraokeLog", "⚡ [0] Đã có sẵn nhạc trong ĐIỆN THOẠI! Tốc độ ánh sáng!")
                                        }

                                        // ========================================================
                                        // BƯỚC 1: LẤY CHỮ (Bỏ qua nếu chọn ACAPELLA_NO_LYRICS)
                                        // ========================================================
                                        val cacheCheckBody = MultipartBody.Builder()
                                            .setType(MultipartBody.FORM)
                                            .addFormDataPart("video_id", videoId)
                                            .addFormDataPart("title", currentSongData!!.title)
                                            .addFormDataPart("singer", currentSongData!!.singer)
                                            .build()
                                        var hasLyricsCache = mode == "ACAPELLA_NO_LYRICS"
                                        val subtitlesList = mutableListOf<YoutubeCaptionEvent>()

                                        if (!hasLyricsCache) {
                                            val req = okhttp3.Request.Builder().url("https://api.tunertools.top/api/get_lyrics").post(cacheCheckBody).build()
                                            val res = try { client.newCall(req).execute() } catch(e:Exception) { null }
                                            if (res?.isSuccessful == true) {
                                                hasLyricsCache = true
                                                val bodyStr = res.body?.string() ?: ""
                                                if (bodyStr.isNotEmpty()) {
                                                    val jsonObject = org.json.JSONObject(bodyStr)
                                                    val lyricsArray = jsonObject.optJSONArray("lyrics")
                                                    if (lyricsArray != null) {
                                                        for (i in 0 until lyricsArray.length()) {
                                                            val lObj = lyricsArray.getJSONObject(i)
                                                            val wordsArray = lObj.optJSONArray("words")
                                                            val aiWords = mutableListOf<AiWordTime>()
                                                            if (wordsArray != null) {
                                                                for (j in 0 until wordsArray.length()) {
                                                                    val wObj = wordsArray.getJSONObject(j)
                                                                    aiWords.add(AiWordTime(wObj.optString("word", ""), wObj.optLong("start_time_ms", 0L), wObj.optLong("end_time_ms", 0L)))
                                                                }
                                                            }
                                                            subtitlesList.add(YoutubeCaptionEvent(lObj.optLong("start_time_ms", 0L), lObj.optLong("end_time_ms", 2000L), lObj.optString("text", "").replace("\n", " ").trim(), aiWords))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { aiProgress = 0.3f } // 👈 KIỂM TRA CHỮ XONG: 30%
                                        val t1_CheckCache = System.currentTimeMillis()
                                        android.util.Log.d("KaraokeLog", "⏱️ [1] Thời gian lấy Chữ: ${t1_CheckCache - t0_StartTime} ms.")

                                        // ========================================================
                                        // BƯỚC 2: TỔNG HỢP NHU CẦU & TẢI YOUTUBE (NẾU CẦN)
                                        // ========================================================
                                        var splitResponse: okhttp3.Response? = null
                                        var hasAudioCache = false
                                        val targetAudioUrl = "https://api.tunertools.top/api/get_vocals_only"

                                        if (!hasLocalAudio && (mode == "ACAPELLA_NO_LYRICS" || mode == "ACAPELLA")) {
                                            val req = okhttp3.Request.Builder().url(targetAudioUrl).post(cacheCheckBody).build()
                                            splitResponse = try { client.newCall(req).execute() } catch(e:Exception) { null }
                                            hasAudioCache = splitResponse?.isSuccessful == true
                                            if (!hasAudioCache) {
                                                splitResponse?.close()
                                                splitResponse = null
                                            }
                                        }

                                        val needAudioProcessing = !hasLocalAudio && !hasAudioCache && (mode == "ACAPELLA_NO_LYRICS" || mode == "ACAPELLA")
                                        val needLyricsProcessing = !hasLyricsCache && mode != "ACAPELLA_NO_LYRICS"
                                        val needOriginalAudio = !hasLocalAudio && mode == "LYRICS_ONLY"

                                        val needYoutubeFile = needAudioProcessing || needLyricsProcessing || needOriginalAudio
                                        var isDownloadSuccess = true

                                        if (needYoutubeFile) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                aiAnalysisStatus = CoreTranslator.getString("ai_downloading_original", currentLang)
                                                aiProgress = 0.5f // 👈 BẮT ĐẦU TẢI YOUTUBE: 50%
                                            }
                                            val json = org.json.JSONObject().apply { put("url", "https://www.youtube.com/watch?v=$videoId") }
                                            val linkReqBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                                            val linkReq = okhttp3.Request.Builder().url("https://api.tunertools.top/api/get_direct_link").post(linkReqBody).build()
                                            val linkRes = client.newCall(linkReq).execute()
                                            var directUrl = ""
                                            if (linkRes.isSuccessful) {
                                                val linkBody = linkRes.body?.string()?.trim() ?: ""
                                                if (linkBody.startsWith("{")) {
                                                    try { directUrl = org.json.JSONObject(linkBody).optString("url", "") } catch (e: Exception) { }
                                                }
                                            }

                                            if (directUrl.isNotEmpty()) {
                                                val downloadReq = okhttp3.Request.Builder().url(directUrl).build()
                                                val downloadRes = client.newCall(downloadReq).execute()
                                                if (downloadRes.isSuccessful) {
                                                    downloadRes.body?.byteStream()?.use { input -> audioFile.outputStream().use { output -> input.copyTo(output) } }
                                                    if (audioFile.length() < 100 * 1024) isDownloadSuccess = false
                                                } else isDownloadSuccess = false
                                            } else isDownloadSuccess = false
                                        }

                                        if (needYoutubeFile && !isDownloadSuccess) {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isAiAnalyzing = false
                                                aiProgress = 0f
                                                Toast.makeText(context, CoreTranslator.getString("youtube_blocked_download", currentLang), Toast.LENGTH_LONG).show()
                                            }
                                            if (audioFile.exists()) audioFile.delete()
                                            return@launch
                                        }

// ========================================================
// BƯỚC 3: UPLOAD FILE CHO PYTHON XỬ LÝ PHẦN CÒN THIẾU
// ========================================================
                                        if (needYoutubeFile && isDownloadSuccess) {
                                            if (needAudioProcessing) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    aiAnalysisStatus = CoreTranslator.getString("ai_extracting_vocals", currentLang)
                                                    aiProgress = 0.75f
                                                }
                                                val splitReqBody = MultipartBody.Builder()
                                                    .setType(MultipartBody.FORM)
                                                    .addFormDataPart("video_id", videoId)
                                                    .addFormDataPart("title", currentSongData!!.title)
                                                    .addFormDataPart("singer", currentSongData!!.singer)
                                                    .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/*".toMediaTypeOrNull()))
                                                    .build()

                                                val req = okhttp3.Request.Builder().url(targetAudioUrl).post(splitReqBody).build()

                                                // 👉 SỬA Ở ĐÂY: Dùng block .use {} để ghi file và đóng kết nối NGAY LẬP TỨC
                                                client.newCall(req).execute().use { response ->
                                                    hasAudioCache = response.isSuccessful
                                                    if (hasAudioCache) {
                                                        response.body?.byteStream()?.use { input ->
                                                            targetFile.outputStream().use { output ->
                                                                input.copyTo(output)
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (mode == "LYRICS_ONLY") {
                                                hasAudioCache = true
                                            }

                                            if (needLyricsProcessing) {
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    aiAnalysisStatus = CoreTranslator.getString("ai_syncing_lyrics", currentLang)
                                                    aiProgress = 0.85f
                                                }
                                                val lyricsReqBody = MultipartBody.Builder()
                                                    .setType(MultipartBody.FORM)
                                                    .addFormDataPart("video_id", videoId)
                                                    .addFormDataPart("title", currentSongData!!.title)
                                                    .addFormDataPart("singer", currentSongData!!.singer)
                                                    .addFormDataPart("file", audioFile.name, audioFile.asRequestBody("audio/*".toMediaTypeOrNull()))
                                                    .build()
                                                val req = okhttp3.Request.Builder().url("https://api.tunertools.top/api/get_lyrics").post(lyricsReqBody).build()
                                                val res = client.newCall(req).execute()
                                                if (res.isSuccessful) {
                                                    hasLyricsCache = true
                                                    val bodyStr = res.body?.string() ?: ""
                                                    if (bodyStr.isNotEmpty()) {
                                                        val jsonObject = org.json.JSONObject(bodyStr)
                                                        val lyricsArray = jsonObject.optJSONArray("lyrics")
                                                        if (lyricsArray != null) {
                                                            for (i in 0 until lyricsArray.length()) {
                                                                val lObj = lyricsArray.getJSONObject(i)
                                                                val wordsArray = lObj.optJSONArray("words")
                                                                val aiWords = mutableListOf<AiWordTime>()
                                                                if (wordsArray != null) {
                                                                    for (j in 0 until wordsArray.length()) {
                                                                        val wObj = wordsArray.getJSONObject(j)
                                                                        aiWords.add(AiWordTime(wObj.optString("word", ""), wObj.optLong("start_time_ms", 0L), wObj.optLong("end_time_ms", 0L)))
                                                                    }
                                                                }
                                                                subtitlesList.add(YoutubeCaptionEvent(lObj.optLong("start_time_ms", 0L), lObj.optLong("end_time_ms", 2000L), lObj.optString("text", "").replace("\n", " ").trim(), aiWords))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        val t3_AudioReady = System.currentTimeMillis()
                                        android.util.Log.d("KaraokeLog", "⏱️ [2+3] Thời gian chuẩn bị Âm thanh: ${t3_AudioReady - t1_CheckCache} ms.")

                                        // ========================================================
                                        // BƯỚC 4: LƯU FILE VÀ PHÁT NHẠC
                                        // ========================================================
                                        if (hasLocalAudio || hasAudioCache) {
                                            if (!hasLocalAudio && mode == "LYRICS_ONLY") {
                                                // Nếu chỉ chạy chữ, copy trực tiếp file gốc sang file đích
                                                audioFile.copyTo(targetFile, overwrite = true)
                                            }

                                            // Xóa file gốc thừa mứa
                                            if (audioFile.exists()) audioFile.delete()

                                            val mappedLines = if (subtitlesList.isNotEmpty()) robustMapTimingLocal(currentSongData!!.lyrics, subtitlesList) else null

                                            // --- BẮT ĐẦU: IN LOG TOÀN BỘ BÀI HÁT TỪ CACHE OFFLINE ---
                                            if (mappedLines != null) {
                                                android.util.Log.d("KaraokeLog_FullSong", "=== BẮT ĐẦU DỮ LIỆU TOÀN BÀI (CACHE) ===")
                                                mappedLines.forEachIndexed { index, line ->
                                                    android.util.Log.d(
                                                        "KaraokeLog_FullSong",
                                                        "[Dòng ${index + 1}] ${line.originalTextWithChords} | ${line.startTimeMs}ms -> ${line.endTimeMs}ms"
                                                    )
                                                }
                                                android.util.Log.d("KaraokeLog_FullSong", "=== KẾT THÚC DỮ LIỆU TOÀN BÀI ===")
                                            }
                                            // --- KẾT THÚC: IN LOG ---

                                            val t4_FinalSetup = System.currentTimeMillis()
                                            android.util.Log.d("KaraokeLog", "⏱️ [4] Thời gian Ghi File + Đồng bộ Lời: ${t4_FinalSetup - t3_AudioReady} ms.")
                                            android.util.Log.d("KaraokeLog", "🎉 TỔNG THỜI GIAN ĐÁP ỨNG: ${t4_FinalSetup - t0_StartTime} ms.")
                                            android.util.Log.d("KaraokeLog", "========================================")

                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                aiProgress = 1.0f // 👈 HOÀN THÀNH: 100%
                                                syncedKaraokeLines = mappedLines
                                                isAiAnalyzing = false
                                                mediaPlayer?.release()

                                                mediaPlayer = android.media.MediaPlayer().apply {
                                                    setDataSource(targetFile.absolutePath)
                                                    prepare()
                                                    if (mode == "LYRICS_ONLY") setVolume(0f, 0f)
                                                    start()
                                                }
                                                activeMediaType = targetMedia
                                            }
                                        } else {
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                isAiAnalyzing = false
                                                aiProgress = 0f
                                                Toast.makeText(context, CoreTranslator.getString("audio_processing_error", currentLang), Toast.LENGTH_SHORT).show()
                                            }
                                            if (audioFile.exists()) audioFile.delete()
                                        }

                                    } catch (e: Exception) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isAiAnalyzing = false
                                            aiProgress = 0f
                                            Toast.makeText(context, CoreTranslator.getString("network_server_error", currentLang), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                mediaPlayer?.start()
                            }
                        }
                    }
                }
            )
        }
// --- POPUP FORM THÊM MỚI BÀI HÁT ---
        if (activeTutorialStep != TutorialStep.NONE) {
            TutorialOverlay(
                step = activeTutorialStep,
                currentLang = currentLang,
                tunaGreen = tunaGreen,
                onDismiss = onTutorialDismiss
            )
        }

        // === ĐOẠN THÊM MỚI HIỂN THỊ CAMERA THU NHỎ (BƯỚC 4.1) ===
        if (showFloatingCamera) {
            FloatingCameraOverlay(
                currentLang = currentLang,
                onClose = { showFloatingCamera = false }
            )
        }
        // =========================================================

        // --- HIỂN THỊ MÀN HÌNH CHORD AI ---
        if (showChordAI) {
            androidx.activity.compose.BackHandler {
                showChordAI = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                val targetUrl =
                    currentSongData?.videoUrl?.takeIf { it.isNotBlank() } ?: urlInput.text
                ChordScreen(initialUrl = targetUrl)

            }
        }

        // --- HIỂN THỊ MÀN HÌNH GUITAR PRO TAB (ALPHATAB) ---
        if (showAlphaTabScreen) {
            androidx.activity.compose.BackHandler {
                showAlphaTabScreen = false
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .zIndex(20f)
            ) {
                BangNhacScreen(
                    apiBaseUrl = "https://tunertools.top",
                    isDark = isDark, // <-- BỔ SUNG DÒNG NÀY ĐỂ TRUYỀN TRẠNG THÁI
                    searchPath = "api_gp_search.php",
                    modifier = Modifier.fillMaxSize(),
                    onClose = { showAlphaTabScreen = false }
                )
            }
        }
    }
}
// ==========================================
// CÁC COMPONENT GIẢM TẢI (Dán vào CUỐI CÙNG của file SongMainScreen.kt)
// ==========================================
@Composable
fun FavoriteSongsListView(
    searchResults: List<SearchResultItem>,
    urlInputText: String,
    currentLang: String,
    textColor: Color,
    bgColor: Color,
    surfaceColor: Color,
    tunaGreen: Color,
    currentUserEmail: String,
    userRole: String,
    sharedPref: android.content.SharedPreferences,
    onOpenDetail: (SearchResultItem, String) -> Unit,
    onReport: (ReportInfo) -> Unit,
    onChordPopup: (String) -> Unit,
    onDeleteClick: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(listScrollState).padding(8.dp)) {
        if (searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (urlInputText.isEmpty()) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "No Favorites", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(CoreTranslator.getString("empty_favorites", currentLang), color = tunaGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.CloudOff, contentDescription = "Not found", tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(CoreTranslator.getString("not_found_online", currentLang), color = tunaGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(CoreTranslator.getString("offline_search_hint_1", currentLang), color = Color.Gray, fontSize = 13.sp)
                        Text(CoreTranslator.getString("offline_search_hint_2", currentLang), color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }

        searchResults.forEach { item ->
            key(item.url) {
                var isLikedItem by remember { mutableStateOf(sharedPref.getBoolean("liked_${item.url}", false)) }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                        onOpenDetail(item, if (item.chords.isNotEmpty() || item.url.isNotEmpty()) "CHORD" else "SHEET")
                    },
                    colors = CardDefaults.cardColors(containerColor = bgColor)
                ) {
                    Column(modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 14.dp, end = 8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(item.title, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${CoreTranslator.getString("singer", currentLang)}: ${item.singer}", color = Color.Gray, fontSize = 13.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(28.dp).clickable { searchMusicOnApp(context, "ZING", item.title, item.singer, currentLang) }, contentAlignment = Alignment.Center) { ZingMp3Icon(modifier = Modifier.size(20.dp)) }
                                Box(modifier = Modifier.size(28.dp).clickable { searchMusicOnApp(context, "APPLE", item.title, item.singer, currentLang) }, contentAlignment = Alignment.Center) { AppleMusicIcon(modifier = Modifier.size(20.dp)) }
                                Box(modifier = Modifier.size(28.dp).clickable { searchMusicOnApp(context, "SPOTIFY", item.title, item.singer, currentLang) }, contentAlignment = Alignment.Center) { SpotifyIcon(modifier = Modifier.size(20.dp)) }

                                if (item.videoUrl.isNotBlank()) {
                                    Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                        Box(modifier = Modifier.size(width = 22.dp, height = 15.dp).background(Color.Red, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                                if (item.zingMp3Url.isNotBlank() || item.videoUrl.isNotBlank()) { Box(modifier = Modifier.padding(horizontal = 4.dp).width(1.dp).height(16.dp).background(Color.Gray.copy(alpha = 0.3f))) }
                                IconButton(onClick = { onReport(ReportInfo(item.url, item.title, item.singer, item.composer)) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        if (currentUserEmail.isBlank()) {
                                            Toast.makeText(context, "Vui lòng đăng nhập Gmail để thêm vào Yêu thích", Toast.LENGTH_SHORT).show()
                                        } else {
                                            isLikedItem = !isLikedItem
                                            sharedPref.edit().putBoolean("liked_${item.url}", isLikedItem).apply()
                                            coroutineScope.launch { SongScraper.toggleLike(item.url, currentUserEmail, isLikedItem) }
                                        }
                                    }, modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = if (isLikedItem) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (isLikedItem) Color.Red else Color.Gray, modifier = Modifier.size(18.dp))
                                }

                                if (userRole in listOf("super_admin", "admin")) {
                                    var showDeleteConfirm by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                    if (showDeleteConfirm) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteConfirm = false },
                                            title = { Text(CoreTranslator.getString("delete_song", currentLang), color = tunaGreen, fontWeight = FontWeight.Bold) },
                                            text = { Text("${CoreTranslator.getString("delete_confirm", currentLang)}\n'${item.title}'", color = textColor) },
                                            containerColor = surfaceColor,
                                            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDeleteClick(item.url) }) { Text(CoreTranslator.getString("delete", currentLang), color = Color.Red, fontWeight = FontWeight.Bold) } },
                                            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray) } }
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            if (item.chords.isNotEmpty() || item.url.isNotEmpty()) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(surfaceColor).border(1.dp, tunaGreen, RoundedCornerShape(4.dp)).clickable { onOpenDetail(item, "CHORD") }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Icon(imageVector = CustomIcons.Chord, contentDescription = CoreTranslator.getString("chord_btn", currentLang), tint = tunaGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(CoreTranslator.getString("chord_btn", currentLang), color = tunaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            if (item.imageCount > 0) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(surfaceColor).border(1.dp, Color(0xFFE6A23C), RoundedCornerShape(4.dp)).clickable { onOpenDetail(item, "SHEET") }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    SheetMusicIcon(tint = Color(0xFFE6A23C), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(CoreTranslator.getString("sheet_btn", currentLang), color = Color(0xFFE6A23C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (item.chords.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                item.chords.forEach { chord ->
                                    Box(Modifier.padding(end = 6.dp).background(surfaceColor, RoundedCornerShape(4.dp)).clickable { onChordPopup(chord) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(formatChordForDisplay(chord), color = tunaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryScreenView(
    currentLang: String,
    textColor: Color,
    bgColor: Color,
    surfaceColor: Color,
    tunaGreen: Color,
    trendingSongs: List<Song>,
    suggestedSongs: List<Song>,
    favoriteArtists: List<SongRepository.ArtistOffline>,
    nhacTre8x9xArtists: List<SongRepository.ArtistOffline>,
    nhacTreGenZArtists: List<SongRepository.ArtistOffline>,
    boleroKinhDienArtists: List<SongRepository.ArtistOffline>,
    boleroTiepNoiArtists: List<SongRepository.ArtistOffline>,
    top50Songs: List<Song>,
    isTrendingExpanded: Boolean,
    onTrendingExpandedToggle: () -> Unit,
    onSongClick: (Song, String) -> Unit,
    onArtistClick: (String) -> Unit,
    onReport: (ReportInfo) -> Unit,
    onChordPopup: (String) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onEditSong: (SongDetails) -> Unit = {},
    currentUserEmail: String,
    userRole: String,
    sharedPref: android.content.SharedPreferences
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        if (trendingSongs.isNotEmpty()) {
            val displayedTrending = if (isTrendingExpanded) trendingSongs else trendingSongs.take(5)
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(text = CoreTranslator.getString("top_trending", currentLang), fontFamily = InterFontFamily, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = if (isTrendingExpanded) CoreTranslator.getString("collapse", currentLang) else CoreTranslator.getString("view_all", currentLang), color = tunaGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onTrendingExpandedToggle() })
                }
            }
            items(displayedTrending.size) { index ->
                val song = displayedTrending[index]
                var videoId = ""
                if (song.youtubeUrl.isNotBlank()) {
                    videoId = if (song.youtubeUrl.contains("watch?v=")) song.youtubeUrl.substringAfter("watch?v=").substringBefore("&") else song.youtubeUrl.substringAfter("youtu.be/").substringBefore("?")
                }
                val thumb = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/mqdefault.jpg" else ""

                Row(modifier = Modifier.fillMaxWidth().clickable { onSongClick(song, "CHORD") }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${index + 1}", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
                    Card(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(56.dp)) {
                        Box(modifier = Modifier.fillMaxSize().background(surfaceColor), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                            if (thumb.isNotBlank()) {
                                AsyncImage(model = coil.request.ImageRequest.Builder(LocalContext.current).data(thumb).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tiêu đề đẩy dài ra bằng weight(1f), chèn thêm padding end để không chạm sát vào số lượt xem
                            Text(
                                text = song.title,
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )

                            // --- HIỂN THỊ MẮT XEM VÀ LƯỢT THẢ TIM BÊN PHẢI CÙNG ---
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Lượt xem",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = song.viewCount.toString(),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                val isLikedItem = sharedPref.getBoolean("liked_${song.id}", false)
                                Icon(
                                    imageVector = if (isLikedItem) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Yêu thích",
                                    tint = if (isLikedItem) Color.Red else Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = song.favoriteCount.toString(),
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "${CoreTranslator.getString("composer_prefix", currentLang)} ${song.tac_gia.ifBlank { CoreTranslator.getString("updating", currentLang) }}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${CoreTranslator.getString("singer_prefix", currentLang)} ${song.ca_si.ifBlank { CoreTranslator.getString("updating", currentLang) }}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item { SongHorizontalRow(title = CoreTranslator.getString("suggested_for_you", currentLang), songs = suggestedSongs, tunaGreen = textColor, surfaceColor = surfaceColor, textColor = textColor, onClickSong = { song -> onSongClick(song, "CHORD") }) }
        item { ArtistHorizontalRow(title = CoreTranslator.getString("favorite_artists", currentLang), artists = favoriteArtists, tunaGreen = textColor, surfaceColor = surfaceColor, textColor = textColor, isCircle = true, currentLang = currentLang, onClickArtist = onArtistClick) }
        item { ArtistHorizontalRow(title = CoreTranslator.getString("pop_8x_9x", currentLang), artists = nhacTre8x9xArtists, tunaGreen = Color(0xFF4FC3F7), surfaceColor = surfaceColor, textColor = textColor, currentLang = currentLang, onClickArtist = onArtistClick) }
        item { ArtistHorizontalRow(title = CoreTranslator.getString("pop_genz_2k", currentLang), artists = nhacTreGenZArtists, tunaGreen = Color(0xFFE040FB), surfaceColor = surfaceColor, textColor = textColor, currentLang = currentLang, onClickArtist = onArtistClick) }
        item { ArtistHorizontalRow(title = CoreTranslator.getString("classic_bolero", currentLang), artists = boleroKinhDienArtists, tunaGreen = Color(0xFFFFCA28), surfaceColor = surfaceColor, textColor = textColor, currentLang = currentLang, onClickArtist = onArtistClick) }
        item { ArtistHorizontalRow(title = CoreTranslator.getString("modern_bolero", currentLang), artists = boleroTiepNoiArtists, tunaGreen = Color(0xFFFF7043), surfaceColor = surfaceColor, textColor = textColor, currentLang = currentLang, onClickArtist = onArtistClick) }

        if (top50Songs.isNotEmpty()) {
            item { Text(text = CoreTranslator.getString("top_50_popular", currentLang), fontFamily = InterFontFamily, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            items(top50Songs.size) { index ->
                val song = top50Songs[index]
                SongListItemCard(
                    song = song,
                    indexText = "${index + 1}.",
                    containerColor = surfaceColor,
                    currentLang = currentLang,
                    tunaGreen = tunaGreen,
                    bgColor = bgColor,
                    textColor = textColor,
                    deviceId = currentUserEmail,
                    sharedPref = sharedPref,
                    surfaceColor = surfaceColor,
                    userRole = userRole,
                    onOpenDetail = { mode -> onSongClick(song, mode) },
                    onOpenCustomDetail = { customDetail ->
                        onSongClick(
                            Song(
                                id = customDetail.songId,
                                title = customDetail.title,
                                originalKey = customDetail.baseTone,
                                rhythm = customDetail.rhythm,
                                chordList = emptyList(),
                                lyrics = customDetail.lyrics.split("\n"),
                                favoriteCount = customDetail.likes,
                                viewCount = 0,
                                commentCount = 0,
                                ratingStars = 0f,
                                ratingCount = 0,
                                genre = emptyList(),
                                authorId = "",
                                authorName = "",
                                singerNames = emptyList(),
                                youtubeUrl = customDetail.videoUrl,
                                createdAt = "",
                                updatedAt = "",
                                tac_gia = customDetail.composer,
                                ca_si = customDetail.singer,
                                tong = customDetail.baseTone,
                                dieu = customDetail.rhythm,
                                bpm = customDetail.bpm.toIntOrNull() ?: 0,
                                the_loai = "",
                                slug = customDetail.slug,
                                image_count = customDetail.imageCount,
                                zingMp3Id = customDetail.zingMp3Id
                            ),
                            "CHORD"
                        )
                    },
                    onReport = { onReport(ReportInfo(song.id, song.title, song.ca_si, song.tac_gia)) },
                    onChordPopup = onChordPopup,
                    onEditClick = { songDetails -> onEditSong(songDetails) },
                    onDeleteClick = { onDeleteClick(song) }
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
// ==========================================
// CÁC COMPONENT GIAO DIỆN BÓC TÁCH ĐỂ GIẢM TẢI (TRÁNH LỖI METHOD TOO LARGE)
// ==========================================

@Composable
fun ChordControlToolbar(
    currentLang: String,
    currentSongData: SongDetails,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    tunaGreen: Color,
    transposeOffset: Int,
    onTransposeOffsetChange: (Int) -> Unit,
    isFootTapping: Boolean,
    metronomeTick: Int,
    onShowBpmSettingsDialog: () -> Unit,
    scrollSpeed: Float,
    onScrollSpeedChange: (Float) -> Unit,
    isAutoScrolling: Boolean,
    onAutoScrollingChange: (Boolean) -> Unit,
    customChordColor: Color,
    onCustomChordColorChange: (Color) -> Unit,
    isChordOverText: Boolean,
    onChordOverTextChange: (Boolean) -> Unit,
    isPronounSwapped: Boolean,
    onPronounSwappedChange: (Boolean) -> Unit,
    showRomanNumerals: Boolean,
    onRomanNumeralsChange: (Boolean) -> Unit,
    isEasyChordMode: Boolean,
    onEasyChordModeToggle: (Boolean) -> Unit,
    isLyricsOnly: Boolean,
    onLyricsOnlyToggle: (Boolean) -> Unit,
    isDoNotDisturbMode: Boolean = false,
    onDoNotDisturbToggle: (Boolean) -> Unit = {},
    fontSizeBody: Int,
    onFontSizeBodyChange: (Int) -> Unit,
    selectedLangCode: String,
    supportedLanguages: Map<String, String>,
    isPatternBarHidden: Boolean = false,
    onTranslateAction: (String) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showLangDropdown by remember { mutableStateOf(false) }
    val toolbarScrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(toolbarScrollState)
                .padding(
                    top = 1.dp,
                    bottom = if (isPatternBarHidden) 12.dp else 2.dp,
                    start = 12.dp,
                    end = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. DỊCH GIỌNG
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).background(surfaceColor, RoundedCornerShape(8.dp)).border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable { onTransposeOffsetChange(transposeOffset - 1) }, Alignment.Center) { Text("♭", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(CoreTranslator.getString("tone", currentLang), color = Color.Gray, fontSize = 9.sp)
                    val displayTone = formatChordForDisplay(transposeSingleChord(currentSongData.baseTone, transposeOffset))
                    Text("[$displayTone]", color = tunaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(28.dp).background(surfaceColor, RoundedCornerShape(8.dp)).border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable { onTransposeOffsetChange(transposeOffset + 1) }, Alignment.Center) { Text("♯", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
            }
            Spacer(Modifier.width(12.dp)); Box(Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(12.dp))

            // 1.5. ĐỔI TONE NAM / NỮ (Thêm mới)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    if (transposeOffset == 0) {
                        onTransposeOffsetChange(5)
                    } else {
                        onTransposeOffsetChange(0)
                    }
                }.padding(horizontal = 4.dp)
            ) {
                Text(if (transposeOffset == 5 || transposeOffset == -7) "Tone Nữ" else "Tone Nam", color = Color.Gray, fontSize = 9.5.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier.size(28.dp).background(if (transposeOffset != 0) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (transposeOffset != 0) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = "Đổi Tone",
                        tint = if (transposeOffset != 0) Color.White else tunaGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp)); Box(Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(12.dp))

            // 2. GÕ NHỊP BPM (HIỂN THỊ CHỈ SỐ TỪ DB NẾU CÓ)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onShowBpmSettingsDialog() }.padding(horizontal = 4.dp)) {
                val safeBpm = currentSongData.bpm.replace("\n", "").replace("\r", "").trim()
                val bpmTitle = if (safeBpm.isNotBlank() && safeBpm != "0" && safeBpm != "null") "BPM: $safeBpm" else "BPM"

                Text(bpmTitle, color = if (bpmTitle == "BPM") Color.Gray else tunaGreen, fontSize = 9.sp, fontWeight = if (bpmTitle == "BPM") FontWeight.Normal else FontWeight.Bold)
                Box(modifier = Modifier.size(26.dp).background(if (isFootTapping) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isFootTapping) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) {
                    AnimatedFootCanvas(activeNoteIndex = metronomeTick, isPlaying = isFootTapping, color = if (isFootTapping) Color.White else tunaGreen, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.width(14.dp)); Box(Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(14.dp))

            // 3. TỐC ĐỘ CUỘN
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(surfaceColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onScrollSpeedChange((scrollSpeed - 0.5f).coerceAtLeast(0.5f)) }, Alignment.Center) { Icon(Icons.Default.Remove, null, tint = textColor, modifier = Modifier.size(14.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${scrollSpeed}x", color = Color.Gray, fontSize = 9.5.sp)
                    Box(Modifier.size(26.dp).background(if (isAutoScrolling) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isAutoScrolling) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onAutoScrollingChange(!isAutoScrolling) }, Alignment.Center) { Icon(if (isAutoScrolling) Icons.Default.Pause else Icons.Default.KeyboardDoubleArrowDown, null, tint = if (isAutoScrolling) Color.White else tunaGreen, modifier = Modifier.size(14.dp)) }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(26.dp).background(surfaceColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onScrollSpeedChange((scrollSpeed + 0.5f).coerceAtMost(5f)) }, Alignment.Center) { Icon(Icons.Default.Add, null, tint = textColor, modifier = Modifier.size(14.dp)) }
            }
            Spacer(Modifier.width(14.dp)); Box(Modifier.width(1.dp).height(20.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(14.dp))

            // 4. MÀU HỢP ÂM
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showColorPicker = true }.padding(horizontal = 4.dp)) {
                    Text(CoreTranslator.getString("color_label", currentLang), color = Color.Gray, fontSize = 9.5.sp)
                    Box(modifier = Modifier.size(28.dp).background(customChordColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, contentDescription = "Color", tint = if (customChordColor == textColor) bgColor else Color.White, modifier = Modifier.size(15.dp)) }
                }
                DropdownMenu(expanded = showColorPicker, onDismissRequest = { showColorPicker = false }, modifier = Modifier.background(surfaceColor).padding(8.dp)) {
                    val basicColors = listOf(tunaGreen, Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFFFFB300), Color(0xFF8E24AA), Color(0xFF00ACC1), Color(0xFFF06292), textColor)
                    Column {
                        Row { for (i in 0..3) { Box(modifier = Modifier.padding(6.dp).size(28.dp).background(basicColors[i], CircleShape).border(1.dp, if (basicColors[i] == bgColor) textColor else Color.Transparent, CircleShape).clickable { onCustomChordColorChange(basicColors[i]); showColorPicker = false }) } }
                        Row { for (i in 4..7) { Box(modifier = Modifier.padding(6.dp).size(28.dp).background(basicColors[i], CircleShape).border(1.dp, if (basicColors[i] == bgColor) textColor else Color.Transparent, CircleShape).clickable { onCustomChordColorChange(basicColors[i]); showColorPicker = false }) } }
                    }
                }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 5. NÚT LỜI (ẨN / HIỆN HỢP ÂM)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onLyricsOnlyToggle(!isLyricsOnly) }.padding(horizontal = 4.dp)) {
                Text(if (isLyricsOnly) "Hiện âm" else "Chỉ lời", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier.size(32.dp).background(if (isLyricsOnly) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isLyricsOnly) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLyricsOnly) Icons.Default.TextFields else Icons.Default.Subject,
                        contentDescription = "Chỉ lời",
                        tint = if (isLyricsOnly) Color.White else tunaGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 5.5 HỢP ÂM DỄ / KHÓ
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onEasyChordModeToggle(!isEasyChordMode) }.padding(horizontal = 4.dp)) {
                Text(if (isEasyChordMode) "H.Âm Dễ" else "H.Âm Gốc", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(Modifier.size(32.dp).background(if (isEasyChordMode) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isEasyChordMode) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape), Alignment.Center) {
                    Icon(if (isEasyChordMode) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "Dễ/Khó", tint = if (isEasyChordMode) Color.White else tunaGreen, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 6. TÁCH DÒNG
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onChordOverTextChange(!isChordOverText) }.padding(horizontal = 4.dp)) {
                Text(CoreTranslator.getString("split", currentLang), color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(Modifier.size(32.dp).background(if (isChordOverText) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isChordOverText) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape), Alignment.Center) { Icon(if (isChordOverText) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardDoubleArrowDown, null, tint = if (isChordOverText) Color.White else tunaGreen, modifier = Modifier.size(16.dp)) }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 6. ĐỔI ĐẠI TỪ ANH/EM
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onPronounSwappedChange(!isPronounSwapped) }.padding(horizontal = 4.dp)) {
                Text("Anh/Em", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(Modifier.size(32.dp).background(if (isPronounSwapped) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isPronounSwapped) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape), Alignment.Center) { Icon(Icons.Default.SwapHoriz, null, tint = if (isPronounSwapped) Color.White else tunaGreen, modifier = Modifier.size(16.dp)) }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 6.5. CHẾ ĐỘ KHÔNG LÀM PHIỀN (CHỈ LỜI BÀI HÁT)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onDoNotDisturbToggle(true) }.padding(horizontal = 4.dp)) {
                Text("Tối giản", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(Modifier.size(32.dp).background(if (isDoNotDisturbMode) tunaGreen else surfaceColor, CircleShape).border(1.dp, if (isDoNotDisturbMode) Color.Transparent else Color.Gray.copy(alpha = 0.3f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.VisibilityOff, null, tint = if (isDoNotDisturbMode) Color.White else tunaGreen, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 7. SỐ LA MÃ
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onRomanNumeralsChange(!showRomanNumerals) }.padding(horizontal = 4.dp)) {
                Text(text = if (showRomanNumerals) CoreTranslator.getString("roman_lbl", currentLang) else CoreTranslator.getString("chord_lbl", currentLang), color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Box(modifier = Modifier.size(32.dp).background(color = if (showRomanNumerals) tunaGreen else surfaceColor, shape = CircleShape).border(width = 1.dp, color = if (showRomanNumerals) Color.Transparent else Color.Gray.copy(alpha = 0.3f), shape = CircleShape), contentAlignment = Alignment.Center) { Text(text = if (showRomanNumerals) "IV" else "Am", color = if (showRomanNumerals) Color.White else tunaGreen, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, fontFamily = if (showRomanNumerals) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.Default) }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 7. FONT CHỮ
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).background(surfaceColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onFontSizeBodyChange((fontSizeBody - 1).coerceAtLeast(10)) }, Alignment.Center) { Text("A", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(CoreTranslator.getString("size", currentLang), color = Color.Gray, fontSize = 10.sp)
                    Text("$fontSizeBody", color = tunaGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(28.dp).background(surfaceColor, CircleShape).border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape).clickable { onFontSizeBodyChange((fontSizeBody + 1).coerceAtMost(30)) }, Alignment.Center) { Text("A", color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(24.dp).background(Color.Gray.copy(alpha = 0.2f))); Spacer(Modifier.width(16.dp))

            // 8. DỊCH LỜI
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { showLangDropdown = true }.padding(end = 8.dp)) {
                    Text(CoreTranslator.getString("translate", currentLang), color = Color.Gray, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(supportedLanguages.entries.find { it.value == selectedLangCode }?.key ?: CoreTranslator.getString("original", currentLang), color = tunaGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = tunaGreen, modifier = Modifier.size(16.dp))
                    }
                }
                DropdownMenu(expanded = showLangDropdown, onDismissRequest = { showLangDropdown = false }, modifier = Modifier.background(bgColor).heightIn(max = 300.dp)) {
                    supportedLanguages.forEach { (langName, langCode) ->
                        DropdownMenuItem(text = { Text(langName, color = if (selectedLangCode == langCode) tunaGreen else textColor) }, onClick = {
                            onTranslateAction(langCode)
                            showLangDropdown = false
                        })
                    }
                }
            }
        }
        if (toolbarScrollState.canScrollForward) {
            Box(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.align(Alignment.CenterEnd).width(40.dp).fillMaxHeight().background(androidx.compose.ui.graphics.Brush.horizontalGradient(colors = listOf(Color.Transparent, surfaceColor))), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Cuộn thêm", tint = tunaGreen.copy(alpha = 0.8f)) } }
        }
        if (toolbarScrollState.canScrollBackward) {
            Box(modifier = Modifier.matchParentSize()) { Box(modifier = Modifier.align(Alignment.CenterStart).width(40.dp).fillMaxHeight().background(androidx.compose.ui.graphics.Brush.horizontalGradient(colors = listOf(surfaceColor, Color.Transparent))), contentAlignment = Alignment.CenterStart) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Quay lại", tint = tunaGreen.copy(alpha = 0.8f)) } }
        }
    }
}

@Composable
fun AiKaraokeProcessingView(
    isFastLoadingCache: Boolean,
    aiAnalysisMode: String,
    aiProgress: Float,
    aiAnalysisStatus: String,
    tunaGreen: Color,
    textColor: Color
) {
    if (isFastLoadingCache) {
        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = tunaGreen, modifier = Modifier.size(50.dp), strokeWidth = 4.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Đang chuẩn bị bài hát...", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    } else {
        val (stepTitles, currentStep) = when (aiAnalysisMode) {
            "ZING_MP3", "LYRICS_ONLY" -> listOf("Lấy dữ liệu", "Đồng bộ hợp âm") to if (aiProgress < 0.5f) 0 else 1
            "ZING_ACAPELLA_NO_LYRICS", "ACAPELLA", "ACAPELLA_NO_LYRICS" -> listOf("Tải bản ghi âm gốc", "AI tách giọng ca sĩ") to if (aiProgress < 0.2f) 0 else 1
            else -> listOf("Tải bản ghi âm gốc", "AI tách giọng ca sĩ", "Đồng bộ hợp âm") to when { aiProgress < 0.2f -> 0; aiProgress < 0.9f -> 1; else -> 2 }
        }
        val animatedCircle by animateFloatAsState(targetValue = aiProgress, animationSpec = androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.LinearOutSlowInEasing), label = "progress")
        Box(modifier = Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
            AiProcessingStepper(currentStep = currentStep, stepProgress = animatedCircle, statusText = aiAnalysisStatus, tunaGreen = tunaGreen, textColor = textColor, stepTitles = stepTitles)
        }
    }
}

@Composable
fun BpmSettingsDialogView(
    adjustableBpm: Float,
    onAdjustableBpmChange: (Float) -> Unit,
    selectedTimeSignature: String,
    onSelectedTimeSignatureChange: (String) -> Unit,
    isFootTapping: Boolean,
    onFootTappingChange: (Boolean) -> Unit,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tốc độ Nhịp (BPM)", color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${adjustableBpm.toInt()}", color = textColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(60.dp))
                    Slider(value = adjustableBpm, onValueChange = onAdjustableBpmChange, valueRange = 40f..250f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(thumbColor = tunaGreen, activeTrackColor = tunaGreen.copy(alpha = 0.8f), inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Nhịp điệu", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1/4", "2/4", "3/4", "4/4", "6/8").forEach { sig ->
                        val isSelected = selectedTimeSignature == sig
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) tunaGreen else surfaceColor).border(1.dp, if (isSelected) Color.Transparent else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).clickable { onSelectedTimeSignatureChange(sig) }.padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(sig, color = if (isSelected) Color.White else textColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                FloatingActionButton(onClick = { onFootTappingChange(!isFootTapping) }, containerColor = if (isFootTapping) Color.Red.copy(alpha = 0.8f) else tunaGreen, contentColor = Color.White, modifier = Modifier.size(72.dp), shape = CircleShape) {
                    Icon(imageVector = if (isFootTapping) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "Play/Pause Metronome", modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(onClick = onDismiss) { Text("Đóng", color = Color.Gray) }
            }
        }
    }
}