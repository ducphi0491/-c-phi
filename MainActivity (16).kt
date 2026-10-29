package ducphi.tunnerinstrument

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.graphics.PathParser
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.animation.core.* // Nhớ thêm import này để dùng Animation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Typography
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.nativeCanvas
import android.annotation.SuppressLint
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
class JaggedEllipseShape(
    private val numTeeth: Int = 16,
    private val toothFactor: Float = 0.2f // Tỷ lệ chiều sâu răng cưa so với kích thước
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radiusXOuter = centerX
        val radiusYOuter = centerY
        val radiusXInner = radiusXOuter - radiusXOuter * toothFactor
        val radiusYInner = radiusYOuter - radiusYOuter * toothFactor

        // Điểm gốc (inner radius)
        val x0 = centerX + radiusXInner * cos(0.0)
        val y0 = centerY + radiusYInner * sin(0.0)
        path.moveTo(x0.toFloat(), y0.toFloat())

        for (i in 0 until numTeeth) {
            val angleStart = (i * 2f * PI / numTeeth).toDouble()
            val angleTip = (angleStart + PI / numTeeth).toDouble()
            val angleEnd = ((i + 1) * 2f * PI / numTeeth).toDouble()

            // Vẽ đến tip (outer radius)
            val xTip = centerX + radiusXOuter * cos(angleTip)
            val yTip = centerY + radiusYOuter * sin(angleTip)
            path.lineTo(xTip.toFloat(), yTip.toFloat())

            // Vẽ đến gốc tiếp theo (inner radius)
            val xEnd = centerX + radiusXInner * cos(angleEnd)
            val yEnd = centerY + radiusYInner * sin(angleEnd)
            path.lineTo(xEnd.toFloat(), yEnd.toFloat())
        }
        path.close()
        return Outline.Generic(path)
    }
}
object CustomIcons {
    val Tuner: ImageVector
        get() = Builder(name = "Tuner", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 2f); lineTo(18f, 2f); quadTo(20f, 2f, 20f, 4f); lineTo(20f, 20f)
                quadTo(20f, 22f, 18f, 22f); lineTo(6f, 22f); quadTo(4f, 22f, 4f, 20f)
                lineTo(4f, 4f); quadTo(4f, 2f, 6f, 2f); close()
                moveTo(4f, 10f); lineTo(20f, 10f)
                moveTo(12f, 10f); lineTo(8f, 4f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 16f); lineTo(12.01f, 16f)
            }
        }.build()

    val Chord: ImageVector
        get() = Builder(name = "Chord", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 3f); lineTo(19f, 3f)
                moveTo(4f, 3f); lineTo(4f, 21f); moveTo(7f, 3f); lineTo(7f, 21f)
                moveTo(10f, 3f); lineTo(10f, 21f); moveTo(13f, 3f); lineTo(13f, 21f)
                moveTo(16f, 3f); lineTo(16f, 21f); moveTo(19f, 3f); lineTo(19f, 21f)
                moveTo(4f, 8f); lineTo(19f, 8f); moveTo(4f, 13f); lineTo(19f, 13f); moveTo(4f, 18f); lineTo(19f, 18f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 3.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(7f, 15.5f); lineTo(7.01f, 15.5f)
                moveTo(10f, 10.5f); lineTo(10.01f, 10.5f)
                moveTo(16f, 5.5f); lineTo(16.01f, 5.5f)
            }
        }.build()
    // [THÊM MỚI] Icon Hợp âm Ukulele (4 dây)
    val UkuleleChord: ImageVector
        get() = Builder(name = "UkuleleChord", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 3f); lineTo(18f, 3f) // Nut
                moveTo(6f, 3f); lineTo(6f, 21f); moveTo(10f, 3f); lineTo(10f, 21f)
                moveTo(14f, 3f); lineTo(14f, 21f); moveTo(18f, 3f); lineTo(18f, 21f)
                moveTo(6f, 8f); lineTo(18f, 8f); moveTo(6f, 13f); lineTo(18f, 13f); moveTo(6f, 18f); lineTo(18f, 18f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 3.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(10f, 15.5f); lineTo(10.01f, 15.5f)
                moveTo(14f, 10.5f); lineTo(14.01f, 10.5f)
                moveTo(18f, 5.5f); lineTo(18.01f, 5.5f)
            }
        }.build()

    // [THÊM MỚI] Icon Hợp âm Piano (Có 3 chấm bấm nốt)
    val PianoChord: ImageVector
        get() = Builder(name = "PianoChord", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2f, 6f); lineTo(22f, 6f); lineTo(22f, 18f); lineTo(2f, 18f); close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1f, strokeLineCap = StrokeCap.Round) {
                moveTo(6f, 6f); lineTo(6f, 18f); moveTo(10f, 6f); lineTo(10f, 18f)
                moveTo(14f, 6f); lineTo(14f, 18f); moveTo(18f, 6f); lineTo(18f, 18f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(4.5f, 6f); lineTo(7.5f, 6f); lineTo(7.5f, 12f); lineTo(4.5f, 12f); close()
                moveTo(8.5f, 6f); lineTo(11.5f, 6f); lineTo(11.5f, 12f); lineTo(8.5f, 12f); close()
                moveTo(16.5f, 6f); lineTo(19.5f, 6f); lineTo(19.5f, 12f); lineTo(16.5f, 12f); close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 3.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 15f); lineTo(4.01f, 15f)
                moveTo(12f, 15f); lineTo(12.01f, 15f)
                moveTo(20f, 15f); lineTo(20.01f, 15f)
            }
        }.build()
    val MusicScore: ImageVector
        get() = Builder(name = "MusicScore", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1f, strokeLineCap = StrokeCap.Round) {
                moveTo(2f, 6f); lineTo(22f, 6f); moveTo(2f, 9f); lineTo(22f, 9f)
                moveTo(2f, 12f); lineTo(22f, 12f); moveTo(2f, 15f); lineTo(22f, 15f); moveTo(2f, 18f); lineTo(22f, 18f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 3.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(7f, 15f); lineTo(7.01f, 15f)
                moveTo(15f, 11f); lineTo(15.01f, 11f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(8.2f, 15f); lineTo(8.2f, 6f)
                moveTo(16.2f, 11f); lineTo(16.2f, 2f)
                moveTo(8.2f, 6f); lineTo(16.2f, 2f)
            }
        }.build()

    val Metronome: ImageVector
        get() = Builder(name = "Metronome", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3f)
                lineTo(20f, 19f)
                quadTo(21f, 21f, 18f, 21f)
                lineTo(6f, 21f)
                quadTo(3f, 21f, 4f, 19f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 21f)
                lineTo(16f, 6f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 13f)
                lineTo(15f, 11f)
                lineTo(17f, 13f)
                lineTo(15f, 15f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(10.5f, 20.5f)
                lineTo(13.5f, 20.5f)
                lineTo(13.5f, 21.5f)
                lineTo(10.5f, 21.5f)
                close()
            }
        }.build()
}


// =====================================================================
// THÔNG TIN DÒNG ĐIỆN THOẠI
// =====================================================================
private data class AppDeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val deviceCode: String,
    val displayName: String,
    val androidVersion: String,
    val sdkInt: Int
)

private fun prettifyDeviceMaker(rawValue: String): String {
    val cleanValue = rawValue.trim()
    if (cleanValue.isBlank()) return ""

    return when (cleanValue.lowercase(java.util.Locale.ROOT)) {
        "samsung" -> "Samsung"
        "xiaomi" -> "Xiaomi"
        "redmi" -> "Redmi"
        "poco" -> "POCO"
        "oppo" -> "OPPO"
        "vivo" -> "vivo"
        "realme" -> "realme"
        "oneplus" -> "OnePlus"
        "huawei" -> "Huawei"
        "honor" -> "HONOR"
        "google" -> "Google"
        "motorola" -> "Motorola"
        "sony" -> "Sony"
        "asus" -> "ASUS"
        "lenovo" -> "Lenovo"
        else -> cleanValue.split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.lowercase(java.util.Locale.ROOT).replaceFirstChar { firstChar ->
                    if (firstChar.isLowerCase()) {
                        firstChar.titlecase(java.util.Locale.ROOT)
                    } else {
                        firstChar.toString()
                    }
                }
            }
    }
}

internal fun buildDeviceDisplayName(
    manufacturerRaw: String,
    brandRaw: String,
    modelRaw: String
): String {
    val manufacturer = prettifyDeviceMaker(manufacturerRaw)
    val brand = prettifyDeviceMaker(brandRaw)
    val model = modelRaw.trim()
    val maker = manufacturer.ifBlank { brand }

    return when {
        model.isBlank() && maker.isBlank() -> ""
        model.isBlank() -> maker
        maker.isBlank() -> model
        model.startsWith(maker, ignoreCase = true) -> model
        brand.isNotBlank() && model.startsWith(brand, ignoreCase = true) -> model
        else -> "$maker $model"
    }.trim()
}

private fun readCurrentDeviceInfo(): AppDeviceInfo {
    val manufacturer = prettifyDeviceMaker(Build.MANUFACTURER.orEmpty())
    val brand = prettifyDeviceMaker(Build.BRAND.orEmpty())
    val model = Build.MODEL.orEmpty().trim()
    val deviceCode = Build.DEVICE.orEmpty().trim()
    val displayName = buildDeviceDisplayName(manufacturer, brand, model)
        .ifBlank { deviceCode.ifBlank { "Android" } }

    return AppDeviceInfo(
        manufacturer = manufacturer,
        brand = brand,
        model = model,
        deviceCode = deviceCode,
        displayName = displayName,
        androidVersion = Build.VERSION.RELEASE.orEmpty().trim(),
        sdkInt = Build.VERSION.SDK_INT
    )
}


class MainActivity : ComponentActivity() {

    companion object {
        private const val ACTION_OPEN_TUNER = "ducphi.tunnerinstrument.OPEN_TUNER"
        private const val ACTION_OPEN_METRONOME = "ducphi.tunnerinstrument.OPEN_METRONOME"
        private const val ACTION_OPEN_CHORDS = "ducphi.tunnerinstrument.OPEN_CHORDS"
        private const val ACTION_OPEN_SONGS = "ducphi.tunnerinstrument.OPEN_SONGS"
    }

    private lateinit var appUpdateManager: AppUpdateManager
    private val UPDATE_REQUEST_CODE = 1001

    // Dynamic shortcut: chỉ thêm state điều hướng, không thay đổi logic chức năng cũ.
    private val shortcutTabState = mutableStateOf<String?>(null)
    private val shortcutRequestIdState = mutableLongStateOf(0L)

    // Realtime Database Presence: theo dõi thiết bị đang thực sự mở ứng dụng.
    private val presenceHandler = Handler(Looper.getMainLooper())
    private var presenceForeground = false
    private var presenceFirebaseUid: String = ""
    private var presenceRef: DatabaseReference? = null
    private var userDirectoryRef: DatabaseReference? = null
    private var presenceConnectedRef: DatabaseReference? = null
    private var presenceConnectedListener: ValueEventListener? = null

    private val presenceHeartbeat = object : Runnable {
        override fun run() {
            if (presenceForeground) {
                writePresenceOnline()
                presenceHandler.postDelayed(this, 30_000L)
            }
        }
    }

    // 1. NHÉT KHAI BÁO FONT VÀO BÊN TRONG MAIN ACTIVITY
    val InterFontFamily = FontFamily(
        androidx.compose.ui.text.font.Font(R.font.inter_regular, FontWeight.Normal),
        androidx.compose.ui.text.font.Font(R.font.inter_medium, FontWeight.Medium),
        androidx.compose.ui.text.font.Font(R.font.inter_bold, FontWeight.Bold)
    )
    val defaultTypography = Typography()
    val InterTypography = Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = InterFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = InterFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = InterFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = InterFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = InterFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = InterFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = InterFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = InterFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = InterFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = InterFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = InterFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = InterFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = InterFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = InterFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = InterFontFamily)
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [MỚI] Ép nền Android Window thành màu đen để tránh chớp trắng khi vừa mở app
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.BLACK))

        // 1. Khởi tạo JSON `CoreTranslator`
        CoreTranslator.init(this)

        // [SHORTCUT] Tạo shortcut động trực tiếp bằng Kotlin.
        // Không cần res/xml/shortcuts.xml hay shortcut_strings.xml.
        setupDynamicShortcuts()
        handleShortcutIntent(intent)

        // [REALTIME ONLINE] Đăng nhập Firebase ẩn danh và chuẩn bị cơ chế Presence.
        // Việc này không thay đổi tài khoản Google đang đăng nhập bên trong ứng dụng.
        initializeRealtimePresence()

        // Khởi tạo appUpdateManager MỘT LẦN duy nhất và chạy kiểm tra BẮT BUỘC cập nhật
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForImmediateUpdate()

        // 2. Cấu hình hệ thống (Bắt buộc phải nằm NGOÀI setContent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // [FCM] Tự động đăng ký nhận thông báo toàn hệ thống
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    android.util.Log.e("FCM", "Đăng ký nhận thông báo thất bại")
                }
            }

        // 3. Gọi setContent MỘT LẦN DUY NHẤT để render giao diện
        setContent {
            val shortcutTab = shortcutTabState.value
            val shortcutRequestId = shortcutRequestIdState.longValue

            MaterialTheme(typography = InterTypography) {
                TunerApp(
                    shortcutTab = shortcutTab,
                    shortcutRequestId = shortcutRequestId
                )
            }
        }
    }

    /**
     * Tạo shortcut động bằng Kotlin để không phải thêm các file XML riêng.
     * Nhãn shortcut lấy theo ngôn ngữ hệ thống và dùng chính CoreTranslator của app.
     */
    private fun setupDynamicShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = getSystemService(android.content.pm.ShortcutManager::class.java)
            ?: return

        val systemLocale = java.util.Locale.getDefault()
        val systemLanguage = systemLocale.language
        val systemLanguageTag = systemLocale.toLanguageTag()
        val shortcutLang = CoreTranslator.supportedLanguages
            .firstOrNull { (code, _) ->
                code.equals(systemLanguageTag, ignoreCase = true) ||
                        code.equals(systemLanguage, ignoreCase = true)
            }
            ?.first
            ?: "en"

        fun label(key: String, fallback: String): String {
            return runCatching { CoreTranslator.getString(key, shortcutLang) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() && it != key }
                ?: fallback
        }

        fun buildShortcut(
            id: String,
            shortLabel: String,
            actionName: String,
            rank: Int,
            iconType: String
        ): android.content.pm.ShortcutInfo {
            val shortcutIntent = Intent(this, MainActivity::class.java).apply {
                action = actionName
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            return android.content.pm.ShortcutInfo.Builder(this, id)
                .setShortLabel(shortLabel)
                .setLongLabel(shortLabel)
                .setRank(rank)
                // Vẽ trực tiếp đúng biểu tượng đang dùng ở thanh menu dưới.
                // Không cần thêm drawable XML riêng cho shortcut.
                .setIcon(createBottomMenuShortcutIcon(iconType))
                .setIntent(shortcutIntent)
                .build()
        }

        val shortcuts = listOf(
            buildShortcut(
                id = "shortcut_tuner",
                shortLabel = label("tune", "Tuner"),
                actionName = ACTION_OPEN_TUNER,
                rank = 0,
                iconType = "Tune"
            ),
            buildShortcut(
                id = "shortcut_metronome",
                shortLabel = label("tempo", "Metronome"),
                actionName = ACTION_OPEN_METRONOME,
                rank = 1,
                iconType = "Metronome"
            ),
            buildShortcut(
                id = "shortcut_chords",
                shortLabel = label("chorus", "Chords"),
                actionName = ACTION_OPEN_CHORDS,
                rank = 2,
                iconType = "Chorus"
            ),
            buildShortcut(
                id = "shortcut_songs",
                shortLabel = label("tab", "Songs"),
                actionName = ACTION_OPEN_SONGS,
                rank = 3,
                iconType = "tab"
            )
        )

        val maxShortcutCount = shortcutManager.maxShortcutCountPerActivity.coerceAtLeast(1)

        runCatching {
            shortcutManager.setDynamicShortcuts(shortcuts.take(maxShortcutCount))
        }.onFailure {
            android.util.Log.e("AppShortcut", "Không thể tạo dynamic shortcuts", it)
        }
    }

    /**
     * Tạo bitmap cho Dynamic Shortcut bằng chính hình dáng của 4 icon CustomIcons
     * đang dùng ở NavigationBar phía dưới: Bản nhạc, Hợp âm, Chỉnh dây, Nhịp.
     *
     * Dynamic Shortcut của Android cần android.graphics.drawable.Icon nên không thể
     * truyền trực tiếp Compose ImageVector. Vì vậy các path 24x24 của CustomIcons
     * được vẽ lại lên bitmap trong suốt ngay tại MainActivity.
     */
    private fun createBottomMenuShortcutIcon(iconType: String): android.graphics.drawable.Icon {
        val bitmapSize = 96
        val bitmap = android.graphics.Bitmap.createBitmap(
            bitmapSize,
            bitmapSize,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        val canvas = android.graphics.Canvas(bitmap)
        val iconColor = android.graphics.Color.rgb(21, 178, 125) // #15B27D - tunaGreen

        // CustomIcons dùng viewport 24x24. Chừa 3 đơn vị padding mỗi phía.
        val scale = bitmapSize / 30f
        canvas.translate(3f * scale, 3f * scale)
        canvas.scale(scale, scale)

        val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = iconColor
            style = android.graphics.Paint.Style.FILL
        }

        when (iconType) {
            // CustomIcons.MusicScore - tab Bản nhạc
            "tab" -> {
                strokePaint.strokeWidth = 1f
                listOf(6f, 9f, 12f, 15f, 18f).forEach { y ->
                    canvas.drawLine(2f, y, 22f, y, strokePaint)
                }

                strokePaint.strokeWidth = 3.5f
                canvas.drawLine(7f, 15f, 7.01f, 15f, strokePaint)
                canvas.drawLine(15f, 11f, 15.01f, 11f, strokePaint)

                strokePaint.strokeWidth = 1.5f
                canvas.drawLine(8.2f, 15f, 8.2f, 6f, strokePaint)
                canvas.drawLine(16.2f, 11f, 16.2f, 2f, strokePaint)
                canvas.drawLine(8.2f, 6f, 16.2f, 2f, strokePaint)
            }

            // CustomIcons.Chord - tab Hợp âm
            "Chorus" -> {
                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(4f, 3f, 19f, 3f, strokePaint)
                listOf(4f, 7f, 10f, 13f, 16f, 19f).forEach { x ->
                    canvas.drawLine(x, 3f, x, 21f, strokePaint)
                }
                listOf(8f, 13f, 18f).forEach { y ->
                    canvas.drawLine(4f, y, 19f, y, strokePaint)
                }

                strokePaint.strokeWidth = 3.5f
                canvas.drawLine(7f, 15.5f, 7.01f, 15.5f, strokePaint)
                canvas.drawLine(10f, 10.5f, 10.01f, 10.5f, strokePaint)
                canvas.drawLine(16f, 5.5f, 16.01f, 5.5f, strokePaint)
            }

            // CustomIcons.Tuner - tab Chỉnh dây
            "Tune" -> {
                strokePaint.strokeWidth = 1.5f
                val body = android.graphics.Path().apply {
                    moveTo(6f, 2f)
                    lineTo(18f, 2f)
                    quadTo(20f, 2f, 20f, 4f)
                    lineTo(20f, 20f)
                    quadTo(20f, 22f, 18f, 22f)
                    lineTo(6f, 22f)
                    quadTo(4f, 22f, 4f, 20f)
                    lineTo(4f, 4f)
                    quadTo(4f, 2f, 6f, 2f)
                    close()
                }
                canvas.drawPath(body, strokePaint)
                canvas.drawLine(4f, 10f, 20f, 10f, strokePaint)
                canvas.drawLine(12f, 10f, 8f, 4f, strokePaint)

                strokePaint.strokeWidth = 3f
                canvas.drawLine(12f, 16f, 12.01f, 16f, strokePaint)
            }

            // CustomIcons.Metronome - tab Nhịp
            "Metronome" -> {
                strokePaint.strokeWidth = 1.5f
                val body = android.graphics.Path().apply {
                    moveTo(12f, 3f)
                    lineTo(20f, 19f)
                    quadTo(21f, 21f, 18f, 21f)
                    lineTo(6f, 21f)
                    quadTo(3f, 21f, 4f, 19f)
                    close()
                }
                canvas.drawPath(body, strokePaint)

                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(12f, 21f, 16f, 6f, strokePaint)

                val diamond = android.graphics.Path().apply {
                    moveTo(13f, 13f)
                    lineTo(15f, 11f)
                    lineTo(17f, 13f)
                    lineTo(15f, 15f)
                    close()
                }
                canvas.drawPath(diamond, fillPaint)
                canvas.drawRect(10.5f, 20.5f, 13.5f, 21.5f, fillPaint)
            }
        }

        return android.graphics.drawable.Icon.createWithBitmap(bitmap)
    }

    /** Chuyển action của shortcut thành mã tab đang dùng sẵn trong TunerScreen. */
    private fun resolveShortcutTab(intent: Intent?): String? {
        return when (intent?.action) {
            ACTION_OPEN_TUNER -> "Tune"
            ACTION_OPEN_METRONOME -> "Metronome"
            ACTION_OPEN_CHORDS -> "Chorus"
            ACTION_OPEN_SONGS -> "tab"
            else -> null
        }
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val targetTab = resolveShortcutTab(intent) ?: return
        shortcutTabState.value = targetTab
        // Tăng ID để cùng một shortcut vẫn mở lại được nếu người dùng bấm lần nữa.
        shortcutRequestIdState.longValue = shortcutRequestIdState.longValue + 1L
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShortcutIntent(intent)
    }

    // =====================================================================
    // FIREBASE REALTIME DATABASE PRESENCE
    // =====================================================================
    private fun initializeRealtimePresence() {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            attachRealtimePresence(currentUser.uid)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                attachRealtimePresence(uid)
            }
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "RealtimePresence",
                    "Không thể đăng nhập Firebase ẩn danh. Hãy bật Anonymous Authentication.",
                    error
                )
            }
    }

    private fun attachRealtimePresence(firebaseUid: String) {
        if (presenceRef != null) return

        val realtimeDb = try {
            FirebaseDatabase.getInstance()
        } catch (error: Exception) {
            android.util.Log.e(
                "RealtimePresence",
                "Không thể mở Realtime Database. Hãy tạo database và cập nhật google-services.json.",
                error
            )
            return
        }

        presenceFirebaseUid = firebaseUid
        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        presenceRef = realtimeDb.getReference("presence").child(firebaseUid)
        // user_directory tồn tại lâu dài, không bị xóa khi app offline.
        // PHP dùng device_id này để gửi tin nhắn cho user kể cả khi họ đang offline.
        userDirectoryRef = realtimeDb.getReference("user_directory").child(deviceId)
        presenceConnectedRef = realtimeDb.getReference(".info/connected")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                if (!connected) return

                // Đăng ký xóa bản ghi trên máy chủ trước khi ghi online để tránh trạng thái treo.
                // Nhờ vậy node /presence chỉ chứa những thiết bị đang hoạt động.
                presenceRef?.onDisconnect()?.removeValue()

                if (presenceForeground) {
                    writePresenceOnline()
                    startPresenceHeartbeat()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e(
                    "RealtimePresence",
                    "Listener .info/connected bị hủy: ${error.message}"
                )
            }
        }

        presenceConnectedListener = listener
        presenceConnectedRef?.addValueEventListener(listener)

        if (presenceForeground) {
            writePresenceOnline()
            startPresenceHeartbeat()
        }
    }

    private fun writePresenceOnline() {
        if (!presenceForeground) return
        val ref = presenceRef ?: return

        val account = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getLastSignedInAccount(this)

        val email = account?.email?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
        val displayName = account?.displayName?.trim().takeUnless { it.isNullOrBlank() }
            ?: if (email != "guest") email.substringBefore("@") else "Khách"

        val deviceId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }

        val deviceInfo = readCurrentDeviceInfo()

        val commonData = hashMapOf<String, Any>(
            "firebase_uid" to presenceFirebaseUid,
            "email" to email,
            "display_name" to displayName,
            "device_id" to deviceId,
            "app_version" to versionName,
            "platform" to "android",
            "device_manufacturer" to deviceInfo.manufacturer,
            "device_brand" to deviceInfo.brand,
            "device_model" to deviceInfo.model,
            "device_code" to deviceInfo.deviceCode,
            "device_name" to deviceInfo.displayName,
            "android_version" to deviceInfo.androidVersion,
            "android_sdk" to deviceInfo.sdkInt
        )

        val onlineData = HashMap<String, Any>(commonData).apply {
            put("online", true)
            put("last_changed", ServerValue.TIMESTAMP)
        }

        val directoryData = HashMap<String, Any>(commonData).apply {
            // Node này được giữ lại khi offline để Admin vẫn liên hệ được.
            put("last_seen", ServerValue.TIMESTAMP)
            put("updated_at", ServerValue.TIMESTAMP)
        }

        ref.updateChildren(onlineData)
            .addOnFailureListener { error ->
                android.util.Log.e(
                    "RealtimePresence",
                    "Không thể cập nhật trạng thái online",
                    error
                )
            }

        userDirectoryRef?.updateChildren(directoryData)
            ?.addOnFailureListener { error ->
                android.util.Log.e(
                    "RealtimePresence",
                    "Không thể cập nhật user_directory",
                    error
                )
            }
    }

    private fun writePresenceOffline() {
        presenceRef?.removeValue()
    }

    private fun startPresenceHeartbeat() {
        presenceHandler.removeCallbacks(presenceHeartbeat)
        if (presenceForeground) {
            presenceHandler.postDelayed(presenceHeartbeat, 30_000L)
        }
    }

    private fun stopPresenceHeartbeat() {
        presenceHandler.removeCallbacks(presenceHeartbeat)
    }

    override fun onStart() {
        super.onStart()
        presenceForeground = true
        writePresenceOnline()
        startPresenceHeartbeat()
    }

    override fun onStop() {
        presenceForeground = false
        stopPresenceHeartbeat()
        writePresenceOffline()
        super.onStop()
    }

    override fun onDestroy() {
        stopPresenceHeartbeat()
        presenceConnectedListener?.let { listener ->
            presenceConnectedRef?.removeEventListener(listener)
        }
        presenceConnectedListener = null
        presenceConnectedRef = null
        presenceRef = null
        userDirectoryRef = null
        presenceFirebaseUid = ""
        super.onDestroy()
    }

    private fun checkForImmediateUpdate() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Làm mới email/tên người dùng sau khi quay lại từ màn hình đăng nhập Google.
        writePresenceOnline()

        // Chặn người dùng lách luật bằng cách thoát ra Home rồi vào lại
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Nếu người dùng cố tình hủy ngang hoặc back lại, ép họ phải thoát ứng dụng
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Bạn phải cập nhật ứng dụng để tiếp tục sử dụng.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}

/**
 * Lay ngon ngu uu tien cua dien thoai (khong phai ngon ngu da luu trong ung dung).
 * Khong ho tro ma ngon ngu tren may: mac dinh ve English.
 */
private fun systemTunerLanguage(): String {
    val configuration = android.content.res.Resources.getSystem().configuration
    val deviceLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        configuration.locale
    }
    val deviceCode = deviceLocale.language.lowercase(java.util.Locale.ROOT)
    return CoreTranslator.supportedLanguages.firstOrNull { (code, _) -> code == deviceCode }?.first ?: "en"
}

@Composable
fun TunerApp(
    viewModel: TunerViewModel = viewModel(),
    shortcutTab: String? = null,
    shortcutRequestId: Long = 0L
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE)

    // Lan dau: tu nhan ngon ngu dien thoai neu nam trong 15 ngon ngu ho tro.
    val deviceLanguage = systemTunerLanguage()
    val primarySystemLanguage = android.content.res.Resources.getSystem().configuration.let { config ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales.get(0).language.lowercase(java.util.Locale.ROOT)
        } else {
            @Suppress("DEPRECATION")
            config.locale.language.lowercase(java.util.Locale.ROOT)
        }
    }
    val isSupported = CoreTranslator.supportedLanguages.any { it.first == primarySystemLanguage }
    if (!prefs.contains("isFirstTime") && isSupported) {
        prefs.edit().putBoolean("isFirstTime", false).apply()
    }

    // appLangMode khong co o cac ban cu: mac dinh chon "system" de sua loi
    // may English nhung app van giu tieng Viet tu lan mo truoc.
    var isSystemLanguage by remember {
        mutableStateOf(prefs.getString("appLangMode", "system") != "manual")
    }
    var isFirstTime by remember { mutableStateOf(prefs.getBoolean("isFirstTime", true)) }
    var isDarkMode by remember { mutableStateOf(prefs.getBoolean("isDarkMode", true)) }
    var currentLang by remember {
        mutableStateOf(
            if (isSystemLanguage) deviceLanguage else {
                prefs.getString("appLang", deviceLanguage)
                    ?.takeIf { stored -> CoreTranslator.supportedLanguages.any { it.first == stored } }
                    ?: deviceLanguage
            }
        )
    }

    // Dong bo ma ngon ngu cho cac man hinh/workers doc truc tiep TunerPrefs.
    LaunchedEffect(currentLang) {
        if (prefs.getString("appLang", null) != currentLang) {
            prefs.edit().putString("appLang", currentLang).apply()
        }
    }

    // Khi quay lai ung dung sau khi doi ngon ngu dien thoai, cap nhat ngay
    // neu dang o che do Tu dong. Che do thu cong se khong bi ghi de.
    val appLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(appLifecycleOwner, isSystemLanguage) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isSystemLanguage) {
                val latestSystemLanguage = systemTunerLanguage()
                if (prefs.getString("appLang", null) != latestSystemLanguage) {
                    currentLang = latestSystemLanguage
                    prefs.edit().putString("appLang", latestSystemLanguage).apply()
                }
            }
        }
        appLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { appLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Truong hop Android cap nhat Configuration ngay khi app dang hien thi.
    val systemConfiguration = LocalConfiguration.current
    LaunchedEffect(systemConfiguration, isSystemLanguage) {
        if (isSystemLanguage) {
            val latestSystemLanguage = systemTunerLanguage()
            if (currentLang != latestSystemLanguage) currentLang = latestSystemLanguage
        }
    }

    // Chỉ dùng MỘT màn hình tải khi khởi động.
    // Splash sẽ giữ nguyên cho tới khi bộ database offline đã được chuẩn bị xong.
    var isAppReady by remember { mutableStateOf(false) }
    var startupDbInstallResult by remember { mutableStateOf<DbBundleInstallResult?>(null) }

    // Khối lệnh này chạy ngầm ngay khi app mở lên.
    // Toàn bộ bước prepare database được hoàn tất TRƯỚC KHI đóng Splash,
    // nhờ vậy SongTabScreen không cần hiện thêm một loader thứ hai.
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        val dbResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val prepared = SongRepository.prepareDatabases(context)

                if (prepared.success) {
                    // Khởi tạo repository ngay trong Splash để ca_si.db (nếu chưa có)
                    // cũng được chuẩn bị trước khi giao diện Bản nhạc xuất hiện.
                    try {
                        SongRepository(context)
                        prepared
                    } catch (e: Exception) {
                        DbBundleInstallResult(
                            success = false,
                            message = e.message ?: "Cannot initialize song database",
                            songCount = prepared.songCount
                        )
                    }
                } else {
                    prepared
                }
            } catch (e: Exception) {
                DbBundleInstallResult(
                    success = false,
                    message = e.message ?: "Cannot prepare song database"
                )
            }
        }

        // Lưu kết quả để truyền thẳng xuống SongTabScreen.
        // SongTabScreen sẽ dùng lại kết quả này thay vì prepare database lần thứ hai.
        startupDbInstallResult = dbResult

        // Giữ Splash tối thiểu 1.5 giây để chuyển màn hình không bị chớp.
        val timePassed = System.currentTimeMillis() - startTime
        val minSplashTime = 1500L
        if (timePassed < minSplashTime) {
            kotlinx.coroutines.delay(minSplashTime - timePassed)
        }

        // Chỉ đóng Splash sau khi prepareDatabases đã trả về kết quả.
        isAppReady = true
    }

    val bgColor = if (isDarkMode) Color(0xFF000000) else Color(0xFFF5F5F7)
    val surfaceColor = if (isDarkMode) Color(0xFF121212) else Color.White
    val navBgColor = if (isDarkMode) Color.Black else Color(0xFFEBEBEB)
    val textColor = if (isDarkMode) Color.White else Color(0xFF1B1C20)
    val tunaGreen = Color(0xFF15B27D)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Đồng bộ màu hệ thống với giao diện ứng dụng.
            val useDarkSystemBars = isDarkMode || !isAppReady
            val navigationBarColor = android.graphics.Color.parseColor(
                if (useDarkSystemBars) "#000000" else "#EBEBEB"
            )
            window.statusBarColor = android.graphics.Color.parseColor(
                if (useDarkSystemBars) "#000000" else "#F5F5F7"
            )
            window.navigationBarColor = navigationBarColor

            // Xóa đường phân cách màu xám phía trên thanh điều hướng Android.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.navigationBarDividerColor = navigationBarColor
            }

            // Android 10+ có thể tự phủ một lớp màu tương phản lên thanh điều hướng.
            // Tắt lớp phủ này để nền tối giữ đúng màu đen tuyền #000000.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            val systemBarController = WindowCompat.getInsetsController(window, view)
            systemBarController.isAppearanceLightStatusBars = !useDarkSystemBars
            systemBarController.isAppearanceLightNavigationBars = !useDarkSystemBars
        }
    }

    // [MỚI] Điều hướng logic hiển thị: Splash Screen -> Chọn Ngôn ngữ (nếu có) -> App Chính
    if (!isAppReady) {
        SplashScreen(
            tunaGreen = tunaGreen // Không cần truyền onTimeout nữa
        )
    } else if (isFirstTime) {
        FirstTimeLanguageScreen(
            bgColor = bgColor,
            surfaceColor = surfaceColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            onLanguageSelected = { selectedLang ->
                isSystemLanguage = false // Nguoi dung da chon ngon ngu bang tay.
                currentLang = selectedLang
                prefs.edit()
                    .putString("appLangMode", "manual")
                    .putString("appLang", selectedLang)
                    .putBoolean("isFirstTime", false)
                    .apply()
                isFirstTime = false
            }
        )
    } else {
        TunerScreen(
            viewModel = viewModel,
            isDarkMode = isDarkMode,
            onDarkModeToggle = {
                isDarkMode = it
                prefs.edit().putBoolean("isDarkMode", it).apply()
            },
            currentLang = currentLang,
            isSystemLanguage = isSystemLanguage,
            onLangChange = { selectedLang ->
                isSystemLanguage = false
                currentLang = selectedLang
                prefs.edit()
                    .putString("appLangMode", "manual")
                    .putString("appLang", selectedLang)
                    .apply()
            },
            onUseSystemLanguage = {
                val phoneLang = systemTunerLanguage()
                isSystemLanguage = true
                currentLang = phoneLang
                prefs.edit()
                    .putString("appLangMode", "system")
                    .putString("appLang", phoneLang)
                    .apply()
            },
            bgColor = bgColor,
            surfaceColor = surfaceColor,
            navBgColor = navBgColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            startupDbInstallResult = startupDbInstallResult,
            shortcutTab = shortcutTab,
            shortcutRequestId = shortcutRequestId
        )
    }
}

// [MỚI] Giao diện màn hình Splash Screen nền đen, sóng âm
@Composable
fun SplashScreen(tunaGreen: Color) {
    // Đã xóa LaunchedEffect đếm ngược ở đây

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Hiển thị Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Hiệu ứng tải dữ liệu hình sóng âm (Sound wave)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(40.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "wave")
                // Độ cao tối đa của từng cột để tạo hình vòng cung âm thanh
                val heights = listOf(0.4f, 0.7f, 1f, 0.7f, 0.4f)

                heights.forEachIndexed { index, maxMultiplier ->
                    val heightMultiplier by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = maxMultiplier,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 450,
                                delayMillis = index * 100, // Tạo độ trễ lệch nhịp lan truyền
                                easing = LinearOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "wave_height_$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .fillMaxHeight(heightMultiplier)
                            .background(tunaGreen, RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
fun TunerScreen(
    viewModel: TunerViewModel,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    currentLang: String,
    isSystemLanguage: Boolean,
    onLangChange: (String) -> Unit,
    onUseSystemLanguage: () -> Unit,
    bgColor: Color,
    surfaceColor: Color,
    navBgColor: Color,
    textColor: Color,
    tunaGreen: Color,
    startupDbInstallResult: DbBundleInstallResult? = null,
    shortcutTab: String? = null,
    shortcutRequestId: Long = 0L
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE) }
    val defaultTabs = setOf("tab", "Chorus", "Tune", "Metronome", "minigame_hub")
    var visibleTabs by remember { mutableStateOf(prefs.getStringSet("visibleTabs", defaultTabs) ?: defaultTabs) }

    var selectedTab by remember { mutableStateOf(shortcutTab ?: "tab") }

    // Khi shortcut được bấm lúc app đang chạy, chuyển thẳng đến đúng tab.
    LaunchedEffect(shortcutRequestId, shortcutTab) {
        if (!shortcutTab.isNullOrBlank()) {
            selectedTab = shortcutTab
        }
    }

    var sharedChordQuery by remember { mutableStateOf("") }
    var activeTutorialStep by remember { mutableStateOf(TutorialStep.NONE) } // Trạng thái bài hướng dẫn hiện tại

    // Badge NEW được hiển thị trên tab Hợp âm thay vì tab Trò chơi.
    var showChorusNewBadge by remember { mutableStateOf(true) }

    // Giữ nguyên thời gian hiển thị như phiên bản trước: tự ẩn sau 5 giây.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000L)
        showChorusNewBadge = false
    }

    // Hiệu ứng nhấp nhô dùng cho badge NEW trên icon Hợp âm.
    val badgeInfiniteTransition = rememberInfiniteTransition(label = "chorusBadgeBounce")
    val badgeOffsetY by badgeInfiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chorusBadgeOffsetY"
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground by remember { mutableStateOf(true) }
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Quản lý phân loại Minigame đang chạy nội bộ trong tab minigame_hub
    var activeMinigameType by remember { mutableStateOf("") }
    // Lấy dữ liệu đã lưu thay vì để null mặc định
    var globalMinigameInstrument by remember { mutableStateOf(prefs.getString("savedInstrument", null)) }
    var showInstrumentDialog by remember { mutableStateOf(false) }

    var showLoginPromptDialog by remember { mutableStateOf(false) }
    val gso = remember(context) { buildTunerGoogleSignInOptions(context) }
    val googleSignInClient = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso) }
    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // [SỬA Ở ĐÂY] Đăng nhập thành công -> Chuyển sang tab Trò chơi ngay lập tức
            selectedTab = "minigame_hub"

            // Mở hộp thoại chọn nhạc cụ (nếu người dùng chưa chọn trước đó)
            if (globalMinigameInstrument == null) {
                showInstrumentDialog = true
            }
        }
    }
    val nestedScrollConnection = remember(selectedTab) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (selectedTab == "Chorus" || selectedTab == "tab") {
                    if (available.y < -15f) isBottomBarVisible = false
                    else if (available.y > 15f) isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // --- BIẾN ĐỂ THEO DÕI THỜI GIAN ---
    var sessionStartTime by remember { mutableStateOf(0L) }
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val deviceIdStr = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device"
    val currentDeviceInfo = remember { readCurrentDeviceInfo() }
    val screenScope = rememberCoroutineScope()

    // Trạng thái hỗ trợ được quản lý ở tầng TunerScreen để icon Chat có thể nằm ngay trên tab Bản nhạc.
    var topLevelUserRole by remember { mutableStateOf<String?>(null) }
    var songTabUnreadSupportCount by remember { mutableIntStateOf(0) }
    // Chỉ giữ ticket có tin Admin CHƯA ĐỌC để icon không đứng thường trực khi user đang chờ Admin.
    var songTabUnreadSupportTicketId by remember { mutableStateOf<String?>(null) }
    var supportStateRefreshing by remember { mutableStateOf(false) }
    var supportStateRefreshAgain by remember { mutableStateOf(false) }

    fun refreshSongTabSupportState() {
        if (supportStateRefreshing) {
            // Không bỏ mất event nếu Admin nhắn đúng lúc request trước còn đang chạy.
            supportStateRefreshAgain = true
            return
        }
        val requesterEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
            .getLastSignedInAccount(context)?.email.orEmpty()

        supportStateRefreshing = true
        screenScope.launch {
            try {
                do {
                    supportStateRefreshAgain = false

                    topLevelUserRole = if (requesterEmail.isBlank()) {
                        "user"
                    } else {
                        runCatching { SongScraper.getUserRole(requesterEmail) }
                            .getOrDefault("user")
                    }

                    val (unreadCount, supportTickets) = TunerSupportApi.getSupportData(
                        deviceId = deviceIdStr,
                        requesterEmail = requesterEmail
                    )
                    songTabUnreadSupportCount = unreadCount
                    // Sau khi user đọc/gửi trả lời, icon ẩn. Khi Admin nhắn lại (status=1, user_read=0)
                    // listener/poll sẽ tìm thấy ticket này và làm icon hiện lại ngay.
                    songTabUnreadSupportTicketId = supportTickets.firstOrNull { ticket ->
                        ticket.type == "support" && ticket.status == 1 && ticket.userRead == 0
                    }?.id
                } while (supportStateRefreshAgain)
            } finally {
                supportStateRefreshing = false
            }
        }
    }

    // Inbox Admin nằm ở tầng TunerScreen nên popup có thể hiển thị trên bất kỳ tab nào.
    val adminContactInbox = rememberAdminContactInbox(deviceIdStr)
    var deferredContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showGlobalSupportCenter by remember { mutableStateOf(false) }
    var globalSupportTicketId by remember { mutableStateOf<String?>(null) }
    // Nếu user bấm thu nhỏ, giữ ticket ở đây để icon nổi luôn hiện và mở lại đúng chat.
    var minimizedSupportTicketId by remember { mutableStateOf<String?>(null) }
    // Request chưa xử lý vẫn làm hiện icon; request accepted không giữ icon thường trực.
    val pendingAdminContact = adminContactInbox.requests.firstOrNull {
        it.status in setOf("pending", "shown", "later")
    }
    // Ưu tiên tin Admin chưa đọc; nếu chưa mở lời mời ban đầu thì dùng ticket của lời mời.
    val directSupportTicketId = minimizedSupportTicketId
        ?: songTabUnreadSupportTicketId
        ?: pendingAdminContact?.ticketId
    val visibleAdminContact = adminContactInbox.requests.firstOrNull {
        it.status in setOf("pending", "shown", "later") &&
                it.requestId !in deferredContactIds
    }
    val isTopLevelStaff = topLevelUserRole in setOf("super_admin", "admin", "mod")
    val pendingContactCount = adminContactInbox.requests.count {
        it.status in setOf("pending", "shown", "later")
    }
    val songTabChatBadgeCount = maxOf(
        songTabUnreadSupportCount,
        pendingContactCount
    )

    // Lắng nghe tín hiệu tin nhắn ngay cả khi bảng chat đang đóng.
    // Khi Admin gửi phản hồi, badge trên logo ở tab Bản nhạc được cập nhật mà không cần thoát/mở lại app.
    var topLevelSupportEventRevision by remember(deviceIdStr) {
        mutableLongStateOf(0L)
    }

    DisposableEffect(deviceIdStr, isTopLevelStaff) {
        if (isTopLevelStaff || deviceIdStr.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        val auth = FirebaseAuth.getInstance()
        val eventRef = runCatching {
            FirebaseDatabase.getInstance()
                .getReference("support_ticket_events")
                .child(deviceIdStr)
        }.getOrNull()

        if (eventRef == null) {
            return@DisposableEffect onDispose { }
        }

        var eventListener: ValueEventListener? = null
        val retryHandler = Handler(Looper.getMainLooper())

        fun stopEventListener() {
            retryHandler.removeCallbacksAndMessages(null)
            eventListener?.let { eventRef.removeEventListener(it) }
            eventListener = null
        }

        fun startEventListener() {
            // Rules cần Firebase Auth. Nếu listener gắn quá sớm khi app vừa mở, nó sẽ bị Permission denied.
            if (auth.currentUser == null || eventListener != null) return

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var newestRevision = 0L
                    var totalMessageCount = 0L
                    snapshot.children.forEach { ticketSnapshot ->
                        val revision = ticketSnapshot.child("revision").getValue(Long::class.java)
                            ?: ticketSnapshot.child("updated_at").getValue(Long::class.java)
                            ?: 0L
                        val messageCount = ticketSnapshot.child("message_count")
                            .getValue(Long::class.java) ?: 0L
                        if (revision > newestRevision) newestRevision = revision
                        totalMessageCount += messageCount
                    }

                    val eventToken = newestRevision * 1_000L + (totalMessageCount % 1_000L)
                    if (eventToken > 0L) topLevelSupportEventRevision = eventToken
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e(
                        "SupportRealtime",
                        "Listener badge bị hủy: ${error.message}. Sẽ tự kết nối lại."
                    )
                    eventListener = null
                    retryHandler.postDelayed({
                        if (auth.currentUser != null) startEventListener()
                    }, 2_500L)
                }
            }

            eventListener = listener
            eventRef.addValueEventListener(listener)
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                startEventListener()
            } else {
                stopEventListener()
            }
        }

        auth.addAuthStateListener(authListener)
        if (auth.currentUser != null) startEventListener()

        onDispose {
            auth.removeAuthStateListener(authListener)
            stopEventListener()
        }
    }

    LaunchedEffect(topLevelSupportEventRevision, showGlobalSupportCenter) {
        if (topLevelSupportEventRevision > 0L && !showGlobalSupportCenter) {
            refreshSongTabSupportState()
        }
    }

    // Dự phòng cho Realtime Database: khi user đang ở tab Bản nhạc và chat đang đóng,
    // kiểm tra nhanh định kỳ để icon vẫn hiện lại kể cả listener vừa mất kết nối.
    LaunchedEffect(selectedTab, showGlobalSupportCenter, isTopLevelStaff, isAppInForeground) {
        if (isTopLevelStaff || showGlobalSupportCenter || !isAppInForeground || selectedTab != "tab") {
            return@LaunchedEffect
        }
        while (true) {
            kotlinx.coroutines.delay(4_000L)
            refreshSongTabSupportState()
        }
    }

    // Nạp quyền tài khoản và số tin chưa đọc khi mở app, khi inbox thay đổi hoặc sau khi đóng màn hình chat.
    LaunchedEffect(deviceIdStr, adminContactInbox.requests.size, showGlobalSupportCenter) {
        if (!showGlobalSupportCenter) {
            refreshSongTabSupportState()
        }
    }

    LaunchedEffect(visibleAdminContact?.requestId) {
        val request = visibleAdminContact ?: return@LaunchedEffect
        if (request.status == "pending") {
            updateAdminContactRequestStatus(
                deviceId = deviceIdStr,
                requestId = request.requestId,
                newStatus = "shown"
            )
        }
    }

    // --- QUẢN LÝ QUYỀN MICROPHONE CHỈ KHI VÀO TAB TUNE ---
    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted && selectedTab == "Tune" && isAppInForeground) {
            viewModel.startListening()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isAppInForeground = true
                    sessionStartTime = System.currentTimeMillis() // Bắt đầu tính giờ
                    refreshSongTabSupportState()
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    isAppInForeground = false
                    if (sessionStartTime != 0L) {
                        val durationMs = System.currentTimeMillis() - sessionStartTime
                        val durationSeconds = durationMs / 1000

                        // Chỉ lưu nếu dùng lớn hơn 5 giây
                        if (durationSeconds > 5) {
                            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                            val thisMonth = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
                            val activeAccount = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)
                            val userEmail = activeAccount?.email ?: "guest"
                            val userDisplayName = activeAccount?.displayName
                                ?.takeIf { it.isNotBlank() }
                                ?: if (userEmail != "guest") userEmail.substringBefore("@") else "Khách"
                            val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

                            // Mở một lô (batch) để ghi đồng loạt vào 4 nơi (đảm bảo tính toàn vẹn dữ liệu)
                            val batch = db.batch()

                            // 1. Ghi vào BXH Hàng Ngày (Leaderboard_Daily)
                            val dailyRef = db.collection("Leaderboard_Daily").document(today).collection("Users").document(deviceIdStr)
                            batch.set(dailyRef, mapOf(
                                "email" to userEmail,
                                "display_name" to userDisplayName,
                                "device_id" to deviceIdStr,
                                "firebase_uid" to firebaseUid,
                                "total_time_seconds" to com.google.firebase.firestore.FieldValue.increment(durationSeconds),
                                "last_access_time" to System.currentTimeMillis(),
                                "device_name" to currentDeviceInfo.displayName,
                                "device_manufacturer" to currentDeviceInfo.manufacturer,
                                "device_brand" to currentDeviceInfo.brand,
                                "device_model" to currentDeviceInfo.model,
                                "device_code" to currentDeviceInfo.deviceCode,
                                "android_version" to currentDeviceInfo.androidVersion,
                                "android_sdk" to currentDeviceInfo.sdkInt
                            ), com.google.firebase.firestore.SetOptions.merge())

                            // 2. Ghi vào BXH Hàng Tháng (Leaderboard_Monthly) - Tiết kiệm công cộng dồn sau này
                            val monthlyRef = db.collection("Leaderboard_Monthly").document(thisMonth).collection("Users").document(deviceIdStr)
                            batch.set(monthlyRef, mapOf(
                                "email" to userEmail,
                                "display_name" to userDisplayName,
                                "device_id" to deviceIdStr,
                                "firebase_uid" to firebaseUid,
                                "total_time_seconds" to com.google.firebase.firestore.FieldValue.increment(durationSeconds),
                                "last_access_time" to System.currentTimeMillis(),
                                "device_name" to currentDeviceInfo.displayName,
                                "device_manufacturer" to currentDeviceInfo.manufacturer,
                                "device_brand" to currentDeviceInfo.brand,
                                "device_model" to currentDeviceInfo.model,
                                "device_code" to currentDeviceInfo.deviceCode,
                                "android_version" to currentDeviceInfo.androidVersion,
                                "android_sdk" to currentDeviceInfo.sdkInt
                            ), com.google.firebase.firestore.SetOptions.merge())

                            // 3. Ghi vào Thống Kê Tổng Hệ Thống (System_Stats) - Để vẽ biểu đồ so sánh cực nhanh
                            val systemStatsRef = db.collection("System_Stats").document(thisMonth)
                            batch.set(systemStatsRef, mapOf(
                                "total_sessions" to com.google.firebase.firestore.FieldValue.increment(1),
                                "total_time_seconds" to com.google.firebase.firestore.FieldValue.increment(durationSeconds)
                            ), com.google.firebase.firestore.SetOptions.merge())

                            // 4. Đăng ký thông tin User định danh (Dùng để đếm New User)
                            val userRef = db.collection("Users").document(deviceIdStr)
                            batch.set(userRef, mapOf(
                                "email" to userEmail,
                                "display_name" to userDisplayName,
                                "device_id" to deviceIdStr,
                                "firebase_uid" to firebaseUid,
                                "last_access_time" to System.currentTimeMillis(),
                                "device_name" to currentDeviceInfo.displayName,
                                "device_manufacturer" to currentDeviceInfo.manufacturer,
                                "device_brand" to currentDeviceInfo.brand,
                                "device_model" to currentDeviceInfo.model,
                                "device_code" to currentDeviceInfo.deviceCode,
                                "android_version" to currentDeviceInfo.androidVersion,
                                "android_sdk" to currentDeviceInfo.sdkInt
                            ), com.google.firebase.firestore.SetOptions.merge())

                            // Thực thi toàn bộ lệnh ghi
                            batch.commit()
                        }
                        sessionStartTime = 0L
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedTab, isAppInForeground) {
        if (selectedTab == "Tune" && isAppInForeground) {
            // Chỉ kiểm tra và xin quyền khi người dùng chuyển sang tab Tune
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startListening()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            viewModel.stopListening()
        }
    }

    Scaffold(
        containerColor = bgColor,
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                // Nền ngoài kéo xuyên xuống vùng thanh điều hướng Android để hai phần liền màu.
                val compactNavColor = if (isDarkMode) Color.Black else navBgColor

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(compactNavColor)
                        .navigationBarsPadding()
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        containerColor = compactNavColor,
                        contentColor = Color.Gray,
                        tonalElevation = 0.dp,
                        // Insets được xử lý bởi Box bên ngoài để chiều cao menu luôn gọn và ổn định.
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    ) {
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tunaGreen,
                            selectedTextColor = tunaGreen,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = if (isDarkMode) Color.White else Color.Black,
                            unselectedTextColor = if (isDarkMode) Color.White else Color.Black
                        )

                        // Hàm tiện ích để cấu hình Text cho đồng bộ
                        @Composable
                        fun NavLabel(text: String) {
                            Text(
                                text = text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp,
                                style = TextStyle(
                                    lineHeight = 10.sp,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                // Kéo nhãn lên gần icon hơn để giảm khoảng trống giữa icon và chữ.
                                modifier = Modifier.offset(y = (-7).dp)
                            )
                        }

                        // 1. Bản nhạc
                        if (visibleTabs.contains("tab")) {
                            NavigationBarItem(
                                icon = { Icon(CustomIcons.MusicScore, "tab") },
                                label = { NavLabel(CoreTranslator.getString("tab", currentLang)) },
                                selected = selectedTab == "tab",
                                onClick = { selectedTab = "tab" },
                                colors = navColors,
                                alwaysShowLabel = false
                            )
                        }
                        // 2. Hợp âm
                        if (visibleTabs.contains("Chorus")) {
                            NavigationBarItem(
                                icon = {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(CustomIcons.Chord, contentDescription = "Chorus")

                                        // Badge NEW được gắn ở góc trên bên phải icon Hợp âm.
                                        if (showChorusNewBadge) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 10.dp, y = (badgeOffsetY - 12).dp)
                                                    .background(Color.Yellow, RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 3.dp, vertical = 1.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "NEW",
                                                    color = Color.Red,
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        lineHeight = 7.sp,
                                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                                            includeFontPadding = false
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                label = { NavLabel(CoreTranslator.getString("chorus", currentLang)) },
                                selected = selectedTab == "Chorus",
                                onClick = { selectedTab = "Chorus" },
                                colors = navColors,
                                alwaysShowLabel = false
                            )
                        }
                        // 3. Chỉnh dây
                        if (visibleTabs.contains("Tune")) {
                            NavigationBarItem(
                                icon = { Icon(CustomIcons.Tuner, "Tune") },
                                label = { NavLabel(CoreTranslator.getString("tune", currentLang)) },
                                selected = selectedTab == "Tune",
                                onClick = { selectedTab = "Tune" },
                                colors = navColors,
                                alwaysShowLabel = false
                            )
                        }
                        // 4. Nhịp
                        if (visibleTabs.contains("Metronome")) {
                            NavigationBarItem(
                                icon = { Icon(CustomIcons.Metronome, "Metronome") },
                                label = { NavLabel(CoreTranslator.getString("tempo", currentLang)) },
                                selected = selectedTab == "Metronome",
                                onClick = { selectedTab = "Metronome" },
                                colors = navColors,
                                alwaysShowLabel = false
                            )
                        }
                        // 5. Minigame
                        if (visibleTabs.contains("minigame_hub")) {
                            NavigationBarItem(
                                icon = {
                                    // Trò chơi chỉ hiển thị icon, không còn badge NEW.
                                    Icon(Icons.Default.SportsEsports, contentDescription = "games")
                                },
                                label = { NavLabel(CoreTranslator.getString("games_leaderboard_title", currentLang)) },
                                selected = selectedTab == "minigame_hub",
                                colors = navColors,
                                alwaysShowLabel = false,
                                onClick = {
                                    // Kiểm tra xem người dùng đã đăng nhập chưa
                                    val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)

                                    if (account != null) {
                                        if (selectedTab == "minigame_hub") {
                                            // TRƯỜNG HỢP 1: ĐANG Ở SẢNH GAME MÀ BẤM LẠI ICON -> LUÔN MỞ BẢNG ĐỔI NHẠC CỤ
                                            showInstrumentDialog = true
                                        } else {
                                            // TRƯỜNG HỢP 2: TỪ TAB KHÁC CHUYỂN SANG SẢNH GAME
                                            selectedTab = "minigame_hub"
                                            // Nếu chưa từng chọn nhạc cụ thì mới bắt chọn, có rồi thì vào thẳng nền sảnh
                                            if (globalMinigameInstrument == null) {
                                                showInstrumentDialog = true
                                            }
                                        }
                                    } else {
                                        showLoginPromptDialog = true // Chưa đăng nhập -> Bật cảnh báo
                                    }
                                }
                            )
                        }

                        // Riêng tab cài đặt bắt buộc phải hiển thị
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, "settings") },
                            label = { NavLabel(CoreTranslator.getString("settings_menu", currentLang)) },
                            selected = selectedTab == "settings",
                            onClick = { selectedTab = "settings" },
                            colors = navColors,
                            alwaysShowLabel = false
                        )
                    }

                    // Duong vien xam mo o mep tren giup phan biet khu vuc menu.
                    HorizontalDivider(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                        thickness = 1.dp,
                        color = if (isDarkMode) {
                            Color.White.copy(alpha = 0.16f)
                        } else {
                            Color.Black.copy(alpha = 0.12f)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(innerPadding).nestedScroll(nestedScrollConnection)) {
            when (selectedTab) {
                "Tune" -> {
                    TunerView(
                        viewModel = viewModel,
                        currentLang = currentLang,
                        isDarkMode = isDarkMode,
                        bgColor = bgColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        tunaGreen = tunaGreen
                    )
                }
                "Metronome" -> {
                    MetronomeScreen(
                        bgColor = bgColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        tunaGreen = tunaGreen,
                        currentLang = currentLang
                    )
                }
                "Chorus" -> {
                    ChordsScreen(
                        currentLang = currentLang,
                        bgColor = bgColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        tunaGreen = tunaGreen,
                        initialQuery = sharedChordQuery,
                        onFullScreenToggle = { isFullScreen ->
                            isBottomBarVisible = !isFullScreen
                        }
                    )
                }
                "settings" -> {
                    SettingsScreen(
                        tunaGreen = tunaGreen,
                        isDarkMode = isDarkMode,
                        onDarkModeToggle = onDarkModeToggle,
                        currentLang = currentLang,
                        isSystemLanguage = isSystemLanguage,
                        onLangChange = onLangChange,
                        onUseSystemLanguage = onUseSystemLanguage,
                        textColor = textColor,
                        surfaceColor = surfaceColor,
                        bgColor = bgColor,
                        onNavigateToTab = { tab ->
                            // [THAY ĐỔI Ở ĐÂY]
                            if (tab == "minigame_hub") {
                                showInstrumentDialog = true // Hiện Dialog chọn nhạc cụ, giữ nguyên màn hình hiện tại
                            } else {
                                selectedTab = tab
                            }
                        },
                        onStartTutorial = { step -> activeTutorialStep = step },
                        visibleTabs = visibleTabs,
                        onVisibleTabsChange = { newTabs ->
                            visibleTabs = newTabs
                            prefs.edit().putStringSet("visibleTabs", newTabs).apply()
                        }
                    )
                }
                "tab" -> {
                    SongTabScreen(
                        isDark = isDarkMode,
                        currentLang = currentLang,
                        bgColor = bgColor,
                        surfaceColor = surfaceColor,
                        textColor = textColor,
                        tunaGreen = tunaGreen,
                        initialDbInstallResult = startupDbInstallResult,
                        activeTutorialStep = activeTutorialStep,
                        onTutorialDismiss = {
                            activeTutorialStep = TutorialStep.NONE
                            isBottomBarVisible = true
                        },
                        onNavigateToChords = { selectedTab = "Chorus" },
                        onChordClicked = { chord ->
                            sharedChordQuery = chord.replace("[", "").replace("]", "").trim()
                        }
                    )
                }

                // PHÂN HỆ ĐIỀU HƯỚNG TRÒ CHƠI & BẢNG XẾP HẠNG (MINIGAME HUB)
// PHÂN HỆ ĐIỀU HƯỚNG TRÒ CHƠI & BẢNG XẾP HẠNG (MINIGAME HUB)
                "minigame_hub" -> {
                    // KHI ĐANG Ở TAB MINIGAME -> KIỂM TRA XEM ĐANG Ở SẢNH HAY ĐANG TRONG GAME
                    when (activeMinigameType) {
                        "CHORD", "PITCH" -> {
                            // Chỉ vào được game nếu đã có nhạc cụ
                            if (globalMinigameInstrument != null) {
                                var minigameChords by remember { mutableStateOf<List<ChordModel>>(emptyList()) }
                                var isFetching by remember { mutableStateOf(true) }
                                val context = LocalContext.current

                                LaunchedEffect(globalMinigameInstrument) {
                                    isFetching = true
                                    val fileName = when (globalMinigameInstrument) {
                                        "Ukulele" -> "ukulele.json"
                                        "Piano" -> "piano.json"
                                        else -> "guitar.json"
                                    }
                                    minigameChords = loadChordsFromAssets(context, fileName)
                                    try {
                                        CoreAudioPlayer.init()
                                        CoreAudioPlayer.loadInstrument(context, globalMinigameInstrument ?: "Guitar")
                                    } catch (e: Exception) { e.printStackTrace() }
                                    isFetching = false
                                }

                                if (isFetching || minigameChords.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
                                    SideEffect { isBottomBarVisible = false }
                                } else {
                                    ChordPracticeScreen(
                                        allChords = minigameChords,
                                        instrument = globalMinigameInstrument!!,
                                        onInstrumentChange = {},
                                        currentLang = currentLang, bgColor = bgColor, surfaceColor = surfaceColor, textColor = textColor, tunaGreen = tunaGreen,
                                        startDirectlyInQuiz = true,
                                        quizCategory = if (activeMinigameType == "CHORD") PracticeCategory.CHORDS else PracticeCategory.PITCH,
                                        onClose = { activeMinigameType = "" }
                                    )
                                    SideEffect { isBottomBarVisible = false }
                                }
                            }
                        }
                        "NOTE" -> {
                            if (globalMinigameInstrument != null) {
                                ChordPracticeScreen(
                                    allChords = emptyList(),
                                    instrument = globalMinigameInstrument!!,
                                    onInstrumentChange = {},
                                    currentLang = currentLang, bgColor = bgColor, surfaceColor = surfaceColor, textColor = textColor, tunaGreen = tunaGreen,
                                    startDirectlyInQuiz = true,
                                    quizCategory = PracticeCategory.NOTES,
                                    onClose = { activeMinigameType = "" }
                                )
                                SideEffect { isBottomBarVisible = false }
                            }
                        }
                        "RHYTHM" -> {
                            RhythmTapScreen(
                                bgColor = bgColor, surfaceColor = surfaceColor, textColor = textColor, tunaGreen = tunaGreen, currentLang = currentLang,
                                startDirectlyInGame = true,
                                onClose = { activeMinigameType = "" }
                            )
                            SideEffect { isBottomBarVisible = false }
                        }
                        else -> {
                            // TRẠNG THÁI MẶC ĐỊNH: LUÔN HIỂN THỊ SẢNH CHỜ LÀM NỀN
                            MinigameHubScreen(
                                instrument = globalMinigameInstrument, // Có thể null không sao cả
                                bgColor = bgColor,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                tunaGreen = tunaGreen,
                                currentLang = currentLang,
                                onNavigateToGame = { gameType ->
                                    // [QUAN TRỌNG] Bấm vào game ở sảnh mà chưa chọn nhạc cụ thì mới hiện Dialog
                                    if (globalMinigameInstrument == null) {
                                        showInstrumentDialog = true
                                    } else {
                                        activeMinigameType = gameType
                                    }
                                },
                                onClose = { selectedTab = "settings" }
                            )
                            SideEffect { isBottomBarVisible = true }
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(CoreTranslator.getString("dev_feature", currentLang), color = Color.Gray, fontSize = 18.sp)
                    }
                }
            }

        }
    }
    // Logo chat nổi trên tab Bản nhạc. Mặc định đè lên góc phải nút chuông và có thể kéo thả.
    DraggableSupportChatBubble(
        visible = selectedTab == "tab" &&
                !isTopLevelStaff &&
                !directSupportTicketId.isNullOrBlank() &&
                !showGlobalSupportCenter,
        badgeCount = songTabChatBadgeCount,
        tunaGreen = tunaGreen,
        surfaceColor = surfaceColor,
        onClick = {
            val minimizedId = minimizedSupportTicketId
            if (!minimizedId.isNullOrBlank()) {
                globalSupportTicketId = minimizedId
                minimizedSupportTicketId = null
                showGlobalSupportCenter = true
            } else {
                pendingAdminContact?.let { request ->
                    deferredContactIds = deferredContactIds + request.requestId
                    if (request.status != "accepted") {
                        updateAdminContactRequestStatus(
                            deviceId = deviceIdStr,
                            requestId = request.requestId,
                            newStatus = "accepted"
                        )
                    }
                }
                globalSupportTicketId = directSupportTicketId
                showGlobalSupportCenter = !directSupportTicketId.isNullOrBlank()
            }
        }
    )

    if (showInstrumentDialog) {
        InstrumentSelectionDialog(
            currentLang = currentLang,
            bgColor = bgColor,
            surfaceColor = surfaceColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            onSelect = { selectedInst ->
                globalMinigameInstrument = selectedInst
                // THÊM DÒNG NÀY: Lưu nhạc cụ xuống bộ nhớ máy
                prefs.edit().putString("savedInstrument", selectedInst).apply()
                showInstrumentDialog = false
            },
            onDismiss = {
                showInstrumentDialog = false
            }
        )
    }
    if (showLoginPromptDialog) {
        AlertDialog(
            onDismissRequest = { showLoginPromptDialog = false },
            title = {
                Text(
                    text = CoreTranslator.getString("login_prompt_title", currentLang),
                    color = tunaGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = CoreTranslator.getString("login_prompt_desc", currentLang),
                    color = textColor,
                    fontSize = 15.sp
                )
            },
            containerColor = surfaceColor,
            confirmButton = {
                Button(
                    onClick = {
                        showLoginPromptDialog = false
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                ) {
                    Text(
                        text = CoreTranslator.getString("login_prompt_confirm", currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginPromptDialog = false }) {
                    Text(
                        text = CoreTranslator.getString("login_prompt_dismiss", currentLang),
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }


    // Popup toàn cục: nằm ngoài when(selectedTab), do đó hiện trên đúng tab user đang dùng.
    if (!showGlobalSupportCenter && visibleAdminContact != null) {
        val request = visibleAdminContact

        fun deferCurrentRequest() {
            deferredContactIds = deferredContactIds + request.requestId
            updateAdminContactRequestStatus(
                deviceId = deviceIdStr,
                requestId = request.requestId,
                newStatus = "later"
            )
        }

        AlertDialog(
            onDismissRequest = { deferCurrentRequest() },
            containerColor = surfaceColor,
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(tunaGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = tunaGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = request.title.ifBlank { "Admin muốn liên hệ" },
                    color = tunaGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = request.adminName.ifBlank { "Quản trị viên" },
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (request.adminEmail.isNotBlank()) {
                        Text(
                            text = request.adminEmail,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = request.message,
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tin nhắn được lưu lại và không tự hết hạn.",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deferCurrentRequest() }) {
                    Text("Để sau", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deferredContactIds = deferredContactIds + request.requestId
                        updateAdminContactRequestStatus(
                            deviceId = deviceIdStr,
                            requestId = request.requestId,
                            newStatus = "accepted"
                        )
                        selectedTab = "tab"
                        isBottomBarVisible = true
                        minimizedSupportTicketId = null
                        globalSupportTicketId = request.ticketId
                        showGlobalSupportCenter = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trò chuyện", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showGlobalSupportCenter && !globalSupportTicketId.isNullOrBlank()) {
        DirectSupportChatOverlay(
            ticketId = globalSupportTicketId!!,
            deviceId = deviceIdStr,
            tunaGreen = tunaGreen,
            bgColor = bgColor,
            surfaceColor = surfaceColor,
            textColor = textColor,
            isDarkMode = isDarkMode,
            currentLang = currentLang,
            onMinimize = {
                minimizedSupportTicketId = globalSupportTicketId
                showGlobalSupportCenter = false
                globalSupportTicketId = null
            },
            onDismiss = {
                showGlobalSupportCenter = false
                globalSupportTicketId = null
                minimizedSupportTicketId = null
                refreshSongTabSupportState()
            }
        )
    }
}

@Composable
fun QrCodeImage(url: String) {
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val encodedUrl = URLEncoder.encode(url, "UTF-8")
                val apiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$encodedUrl"
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val decodedBitmap = BitmapFactory.decodeStream(input)
                bitmap = decodedBitmap?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Image(bitmap = bitmap!!, contentDescription = "QR Code", modifier = Modifier.size(150.dp))
    } else {
        Box(modifier = Modifier.size(150.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun GoogleGLogoCanvas(
    modifier: Modifier = Modifier
) {
    // Official Google "G" geometry split into four brand-colour paths.
    // It is drawn directly by Compose Canvas, so no PNG/SVG resource is required.
    val bluePath = remember {
        PathParser.createPathFromPathData(
            "M22.56 12.25c0-.78-.07-1.53-.20-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
        )!!.asComposePath()
    }
    val greenPath = remember {
        PathParser.createPathFromPathData(
            "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.70 23 12 23z"
        )!!.asComposePath()
    }
    val yellowPath = remember {
        PathParser.createPathFromPathData(
            "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
        )!!.asComposePath()
    }
    val redPath = remember {
        PathParser.createPathFromPathData(
            "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.70 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.60 3.30-4.53 6.16-4.53z"
        )!!.asComposePath()
    }

    Canvas(modifier = modifier) {
        val canvasSize = size.minDimension
        val scaleFactor = canvasSize / 24f
        val offsetX = (size.width - canvasSize) / 2f
        val offsetY = (size.height - canvasSize) / 2f

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(
                scaleX = scaleFactor,
                scaleY = scaleFactor,
                pivot = Offset.Zero
            )
        }) {
            drawPath(path = bluePath, color = Color(0xFF4285F4))
            drawPath(path = greenPath, color = Color(0xFF34A853))
            drawPath(path = yellowPath, color = Color(0xFFFBBC05))
            drawPath(path = redPath, color = Color(0xFFEA4335))
        }
    }
}

private fun accountAvatarInitial(
    displayName: String?,
    email: String?
): String {
    val source = displayName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: email
            ?.substringBefore('@')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        ?: "G"

    val firstCharacter = source.firstOrNull { it.isLetterOrDigit() } ?: 'G'
    return firstCharacter.toString().uppercase(java.util.Locale.getDefault())
}

@Composable
private fun GoogleAccountInitialAvatar(
    displayName: String?,
    email: String?,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val seed = email
        ?.trim()
        ?.lowercase(java.util.Locale.ROOT)
        ?.takeIf { it.isNotBlank() }
        ?: displayName.orEmpty().trim().lowercase(java.util.Locale.ROOT)

    // Google từng dùng avatar chữ cái trên nền tròn khi tài khoản không có ảnh.
    // Chọn màu ổn định theo Gmail để cùng một tài khoản luôn có cùng màu avatar.
    val avatarColor = remember(seed, isDarkMode) {
        val darkPalette = listOf(
            Color(0xFF3367D6),
            Color(0xFF0F9D58),
            Color(0xFF7B1FA2),
            Color(0xFFE37400),
            Color(0xFF00838F),
            Color(0xFFC53929)
        )
        val lightPalette = listOf(
            Color(0xFF4285F4),
            Color(0xFF34A853),
            Color(0xFF8E5BB7),
            Color(0xFFF29900),
            Color(0xFF00ACC1),
            Color(0xFFDB4437)
        )
        val palette = if (isDarkMode) darkPalette else lightPalette
        palette[(seed.hashCode() and Int.MAX_VALUE) % palette.size]
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = accountAvatarInitial(displayName, email),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun SettingsScreen(
    tunaGreen: Color,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    currentLang: String,
    isSystemLanguage: Boolean,
    onLangChange: (String) -> Unit,
    onUseSystemLanguage: () -> Unit,
    textColor: Color,
    surfaceColor: Color,
    bgColor: Color,
    onNavigateToTab: (String) -> Unit,
    onStartTutorial: (TutorialStep) -> Unit,
    visibleTabs: Set<String>,
    onVisibleTabsChange: (Set<String>) -> Unit
) {
    var isLangDialogVisible by remember { mutableStateOf(false) }
    var showMenuConfigDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) } // Thêm dòng này
    var showShareDialog by remember { mutableStateOf(false) }
    var showCoffeeDialog by remember { mutableStateOf(false) }
    // Bank tab is remembered while the settings popup is active.
    var selectedCoffeePayment by rememberSaveable { mutableStateOf("vietcombank") }
    var coffeePendingSavePayment by rememberSaveable { mutableStateOf("vietcombank") }
    var showCopyrightDialog by remember { mutableStateOf(false) }
    var showGuideMenu by remember { mutableStateOf(false) }
    var showLoginPromptDialog by remember { mutableStateOf(false) }
    // Quyền Admin/Mod được xác minh lại khi mở Settings, khi app resume và khi bấm tài khoản.
    var userRole by remember { mutableStateOf("user") }
    var verifiedRoleEmail by remember { mutableStateOf("") }
    var isCheckingAdminRole by remember { mutableStateOf(false) }

    var showSupportCenter by remember { mutableStateOf(false) }
    var unreadSupportCount by remember { mutableStateOf(0) }

    val context = LocalContext.current

    // --- KHAI BÁO STATE CHO NHẮC NHỞ ---
    val prefs = remember { context.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE) }
    var isReminderEnabled by remember { mutableStateOf(prefs.getBoolean("isReminderEnabled", false)) }

    // Thêm state lưu giờ, phút và lời nhắc
    val defaultMsg = CoreTranslator.getString("reminder_default_title", currentLang)
    var reminderHour by remember { mutableIntStateOf(prefs.getInt("reminderHour", 20)) }
    var reminderMinute by remember { mutableIntStateOf(prefs.getInt("reminderMinute", 30)) }
    var reminderMessage by remember(currentLang) {
        mutableStateOf(prefs.getString("reminderMessage", defaultMsg) ?: defaultMsg)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isReminderEnabled = true
            prefs.edit().putBoolean("isReminderEnabled", true).apply()
            scheduleReminder(context)
        } else {
            Toast.makeText(context, CoreTranslator.getString("notification_permission_req", currentLang), Toast.LENGTH_SHORT).show()
        }
    }
    // ----------------------------------------

    val coroutineScope = rememberCoroutineScope()

    // Android Storage Access Framework: user chooses where to save the freshly
    // generated card. No broad storage permission is required.
    val saveCoffeeQrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { selectedUri ->
        if (selectedUri != null) {
            val paymentMethodToSave = coffeePendingSavePayment
            coroutineScope.launch {
                val saveSucceeded = withContext(Dispatchers.IO) {
                    runCatching {
                        // Build the shareable card from the QR bitmap at save time.
                        // Only coffee_momo_qr.png and coffee_vietqr_qr.png are required.
                        val cardBitmap = createCoffeeQrSaveBitmap(context, paymentMethodToSave)
                        try {
                            val output = context.contentResolver.openOutputStream(selectedUri)
                                ?: error("Cannot open destination URI")
                            output.use { stream ->
                                check(cardBitmap.compress(
                                    android.graphics.Bitmap.CompressFormat.PNG,
                                    100,
                                    stream
                                )) { "Cannot encode the QR image" }
                            }
                        } finally {
                            cardBitmap.recycle()
                        }
                    }.isSuccess
                }
                val resultKey = if (saveSucceeded) "coffee_saved" else "coffee_save_error"
                Toast.makeText(
                    context,
                    CoreTranslator.getString(resultKey, currentLang),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val deviceId = remember { Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_device" }

    val appLink = "https://play.google.com/store/apps/details?id=${context.packageName}"

    // Luôn ưu tiên tên thật Google. Nếu Google không trả displayName thì ghép
    // givenName/familyName; chỉ khi không có tên mới fallback về phần trước @ của Gmail.
    fun googleAccountDisplayName(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?
    ): String? {
        if (account == null) return null

        account.displayName?.trim()?.takeIf { it.isNotBlank() }?.let { return it }

        val fullName = listOfNotNull(
            account.givenName?.trim()?.takeIf { it.isNotBlank() },
            account.familyName?.trim()?.takeIf { it.isNotBlank() }
        ).joinToString(" ").trim()
        if (fullName.isNotBlank()) return fullName

        return account.email
            ?.substringBefore('@')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    val initialGoogleAccount = remember(context) {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
    }
    var loggedInUserName by remember {
        mutableStateOf(googleAccountDisplayName(initialGoogleAccount))
    }
    var loggedInUserEmail by remember { mutableStateOf(initialGoogleAccount?.email) }
    var loggedInUserPhotoUrl by remember {
        mutableStateOf(initialGoogleAccount?.photoUrl?.toString())
    }
    // Không cho phép sử dụng quyền đã xác minh cho một Gmail khác.
    val isStaff = userRole in listOf("super_admin", "admin", "mod") &&
            verifiedRoleEmail == loggedInUserEmail?.trim()?.lowercase(java.util.Locale.ROOT)

    // VIP chỉ bật khi Gmail đã được Admin cấp quyền Documents thực sự
    // VÀ đồng thời có quyền sử dụng SongTab. Tài khoản chỉ có trial 30 ngày
    // sẽ không được gắn nhãn VIP.
    var isVipAccount by remember { mutableStateOf(false) }

    suspend fun refreshVipAccountStatus(email: String?) {
        val normalizedEmail = email?.trim()?.lowercase().orEmpty()
        if (normalizedEmail.isBlank()) {
            isVipAccount = false
            return
        }

        val documentsState = DocumentAccessApi.login(
            context = context,
            email = normalizedEmail,
            scope = DocumentAccessScope.DOCUMENTS
        )
        if (documentsState !is DocumentAccessState.Granted) {
            isVipAccount = false
            return
        }

        val songTabState = DocumentAccessApi.checkCurrentScope(
            context = context,
            scope = DocumentAccessScope.SONG_TAB
        )
        isVipAccount = songTabState is DocumentAccessState.Granted
    }

    val gso = remember(context) { buildTunerGoogleSignInOptions(context) }
    val googleSignInClient = remember { com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso) }

    val googleAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                loggedInUserName = googleAccountDisplayName(account)
                loggedInUserEmail = account?.email
                loggedInUserPhotoUrl = account?.photoUrl?.toString()
                Toast.makeText(context, "${CoreTranslator.getString("hello_user", currentLang)}, $loggedInUserName", Toast.LENGTH_SHORT).show()

                // loggedInUserEmail thay đổi sẽ tự kích hoạt kiểm tra quyền bên dưới.
                userRole = "user"
                verifiedRoleEmail = ""
            } catch (e: com.google.android.gms.common.api.ApiException) { Toast.makeText(context, "${CoreTranslator.getString("error_prefix", currentLang)}: ${e.statusCode}", Toast.LENGTH_SHORT).show() }
        }
    }

    // Gắn với vòng đời thực tế của app, không phụ thuộc việc đổi tab.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isSettingsResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner, deviceId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Tự làm mới khi quay lại app, dù vẫn ở tab Cài đặt.
                    val resumedAccount = com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getLastSignedInAccount(context)
                    loggedInUserName = googleAccountDisplayName(resumedAccount)
                    loggedInUserEmail = resumedAccount?.email
                    loggedInUserPhotoUrl = resumedAccount?.photoUrl?.toString()
                    isSettingsResumed = true
                    coroutineScope.launch {
                        try {
                            refreshVipAccountStatus(resumedAccount?.email)
                        } catch (cancel: kotlinx.coroutines.CancellationException) {
                            throw cancel
                        } catch (e: Exception) {
                            android.util.Log.w("SettingsAccess", "Không thể cập nhật VIP", e)
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isSettingsResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Khi thay đổi Gmail, loại bỏ ngay quyền cũ và cập nhật lại VIP.
    LaunchedEffect(loggedInUserEmail) {
        userRole = "user"
        verifiedRoleEmail = ""
        unreadSupportCount = 0
        showSupportCenter = false
        val email = loggedInUserEmail?.trim().orEmpty()
        if (email.isBlank()) {
            isVipAccount = false
        } else {
            try {
                refreshVipAccountStatus(email)
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                android.util.Log.w("SettingsAccess", "Không thể cập nhật VIP", e)
            }
        }
    }

    // Đồng bộ quyền Admin/Mod và badge hỗ trợ ngay khi vào Settings;
    // tự đọc lại sau mỗi 15 giây trong khi người dùng đang ở tab này.
    // Chuyển sang tab khác / thoát app sẽ tự dừng vòng lặp này.
    LaunchedEffect(loggedInUserEmail, isSettingsResumed, deviceId) {
        val checkedEmail = loggedInUserEmail?.trim().orEmpty()
        if (!isSettingsResumed || checkedEmail.isBlank()) return@LaunchedEffect
        while (true) {
            try {
                val latestRole = SongScraper.getUserRole(checkedEmail)
                if (loggedInUserEmail?.trim()?.equals(checkedEmail, ignoreCase = true) == true) {
                    userRole = latestRole
                    verifiedRoleEmail = checkedEmail.lowercase(java.util.Locale.ROOT)
                    if (latestRole !in listOf("super_admin", "admin", "mod")) {
                        showSupportCenter = false
                    }
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                // Không giả định role trên máy chủ khi mất mạng; click sẽ xác minh lại.
                android.util.Log.w("SettingsAccess", "Không thể cập nhật quyền Admin", e)
            }

            try {
                val (count, _) = TunerSupportApi.getSupportData(deviceId, checkedEmail)
                if (loggedInUserEmail?.trim()?.equals(checkedEmail, ignoreCase = true) == true) {
                    unreadSupportCount = count
                }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (e: Exception) {
                android.util.Log.w("SettingsAccess", "Không thể cập nhật số tin hỗ trợ", e)
            }
            kotlinx.coroutines.delay(15_000L)
        }
    }

    val settingsLanguageCode = currentLang
        .lowercase(java.util.Locale.ROOT)
        .substringBefore('-')
        .substringBefore('_')

    fun settingsUiText(vietnamese: String, english: String): String =
        if (settingsLanguageCode == "vi") vietnamese else english

    val settingsCardColor = if (isDarkMode) Color(0xFF101619) else Color.White
    val settingsBorderColor = textColor.copy(alpha = if (isDarkMode) 0.11f else 0.09f)
    val settingsIconColor = textColor.copy(alpha = 0.78f)
    val settingsSecondaryText = textColor.copy(alpha = 0.58f)
    val dangerColor = Color(0xFFFF4D57)

    // Các điều khiển lựa chọn dùng màu trung tính theo Light/Dark theme,
    // không dùng màu xanh thương hiệu để giao diện gọn và đồng nhất hơn.
    val optionSelectedColor = if (isDarkMode) {
        Color(0xFFF1F3F4)
    } else {
        Color(0xFF202124)
    }
    val optionUnselectedColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.42f)
    } else {
        Color.Black.copy(alpha = 0.38f)
    }
    val switchCheckedThumbColor = if (isDarkMode) Color(0xFF202124) else Color.White
    val switchCheckedTrackColor = optionSelectedColor
    val switchUncheckedThumbColor = if (isDarkMode) Color(0xFFB0B7BD) else Color(0xFF6F767D)
    val switchUncheckedTrackColor = if (isDarkMode) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.Black.copy(alpha = 0.14f)
    }

    val appVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = CoreTranslator.getString("settings_title", currentLang),
            color = textColor,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(7.dp))

        // Dùng một dòng cho các mục cố định để chữ chính lớn và dễ đọc.
        // Chỉ trạng thái thay đổi (ngôn ngữ, thông báo) được đặt gọn ở bên phải.

        // ================================================================
        // GIAO DIỆN & TRẢI NGHIỆM
        // ================================================================
        SettingsSectionHeader(
            icon = Icons.Default.Settings,
            title = settingsUiText("GIAO DIỆN & TRẢI NGHIỆM", "INTERFACE & EXPERIENCE"),
            textColor = settingsSecondaryText
        )
        Spacer(modifier = Modifier.height(1.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = settingsCardColor,
            border = BorderStroke(1.dp, settingsBorderColor),
            tonalElevation = 0.dp
        ) {
            Column {
                val langName = CoreTranslator.supportedLanguages
                    .find { it.first == currentLang }
                    ?.second
                    ?: "English"

                ModernSettingRow(
                    title = CoreTranslator.getString("language", currentLang),
                    trailingText = if (isSystemLanguage) {
                        settingsUiText("Tự động ($langName)", "Auto ($langName)")
                    } else langName,
                    icon = Icons.Default.Language,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = { isLangDialogVisible = true }
                )

                ModernSettingRow(
                    title = CoreTranslator.getString("menu_display_config", currentLang),
                    icon = Icons.Default.Menu,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = { showMenuConfigDialog = true }
                )

                ModernSettingRow(
                    title = CoreTranslator.getString("dark_mode", currentLang),
                    icon = if (isDarkMode) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showChevron = false,
                    showDivider = true,
                    trailingContent = {
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onDarkModeToggle(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = switchCheckedThumbColor,
                                checkedTrackColor = switchCheckedTrackColor,
                                uncheckedThumbColor = switchUncheckedThumbColor,
                                uncheckedTrackColor = switchUncheckedTrackColor,
                                checkedBorderColor = Color.Transparent,
                                uncheckedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.scale(0.72f)
                        )
                    },
                    onClick = { onDarkModeToggle(!isDarkMode) }
                )

                val timeString = String.format("%02d:%02d", reminderHour, reminderMinute)
                ModernSettingRow(
                    title = CoreTranslator.getString("notification_title", currentLang),
                    trailingText = if (isReminderEnabled) {
                        timeString
                    } else {
                        settingsUiText("Đã tắt", "Off")
                    },
                    icon = Icons.Default.Notifications,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = false,
                    onClick = { showNotificationDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // ================================================================
        // HỖ TRỢ & THÔNG TIN
        // ================================================================
        SettingsSectionHeader(
            icon = Icons.Default.Help,
            title = settingsUiText("HỖ TRỢ & THÔNG TIN", "SUPPORT & INFORMATION"),
            textColor = settingsSecondaryText
        )
        Spacer(modifier = Modifier.height(3.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = settingsCardColor,
            border = BorderStroke(1.dp, settingsBorderColor),
            tonalElevation = 0.dp
        ) {
            Column {
                ModernSettingRow(
                    title = CoreTranslator.getString("guide", currentLang),
                    icon = Icons.Default.Help,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = { showGuideMenu = true }
                )

                ModernSettingRow(
                    title = CoreTranslator.getString("copyright_policy", currentLang),
                    icon = Icons.Default.Lock,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = { showCopyrightDialog = true }
                )

                ModernSettingRow(
                    title = CoreTranslator.getString("share_app", currentLang),
                    icon = Icons.Default.Share,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = { showShareDialog = true }
                )

                ModernSettingRow(
                    title = CoreTranslator.getString("rate_app", currentLang),
                    icon = Icons.Default.ThumbUp,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = true,
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            val reviewManager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
                            val requestFlow = reviewManager.requestReviewFlow()

                            requestFlow.addOnCompleteListener { request ->
                                if (request.isSuccessful) {
                                    val reviewInfo = request.result
                                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                                    flow.addOnCompleteListener {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("market://details?id=${context.packageName}")
                                        )
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("market://details?id=${context.packageName}")
                                    )
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                                    )
                                )
                            }
                        }
                    }
                )

                // Mục mời cà phê nằm ngay dưới "Đánh giá ứng dụng".
                ModernSettingRow(
                    title = CoreTranslator.getString("coffee_menu", currentLang),
                    icon = Icons.Default.LocalCafe,
                    iconTint = settingsIconColor,
                    textColor = textColor,
                    dividerColor = settingsBorderColor,
                    showDivider = false,
                    onClick = { showCoffeeDialog = true }
                )
            }
        }

        // Quản trị hệ thống không còn chiếm một khu vực riêng.
        // Với Admin/Mod, chạm vào khối ảnh + tên + Gmail ở mục Tài khoản
        // sẽ mở thẳng SupportCenterDialog. Điều kiện isStaff vẫn giữ nguyên.

        // ================================================================
        // TÀI KHOẢN
        // ================================================================
        SettingsSectionHeader(
            icon = Icons.Default.Person,
            title = settingsUiText("TÀI KHOẢN", "ACCOUNT"),
            textColor = settingsSecondaryText
        )
        Spacer(modifier = Modifier.height(3.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            color = settingsCardColor,
            border = BorderStroke(1.dp, settingsBorderColor),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Khối nhận diện tài khoản. Chỉ Admin/Mod mới có hành vi click:
                // chạm vào ảnh/tên/Gmail sẽ mở trực tiếp Quản trị hệ thống.
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(
                            if (!loggedInUserEmail.isNullOrBlank()) {
                                Modifier.clickable(enabled = !isCheckingAdminRole) {
                                    val emailToCheck = loggedInUserEmail?.trim().orEmpty()
                                    if (emailToCheck.isNotBlank()) {
                                        coroutineScope.launch {
                                            isCheckingAdminRole = true
                                            try {
                                                // Bắt buộc xác thực trực tiếp khi nhấn, không dùng quyền cache.
                                                val latestRole = SongScraper.getUserRole(emailToCheck)
                                                if (loggedInUserEmail?.trim()
                                                        ?.equals(emailToCheck, ignoreCase = true) == true) {
                                                    userRole = latestRole
                                                    verifiedRoleEmail = emailToCheck.lowercase(java.util.Locale.ROOT)
                                                    if (latestRole in listOf("super_admin", "admin", "mod")) {
                                                        showSupportCenter = true
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            if (currentLang == "vi") "Tài khoản chưa được cấp quyền Admin/Mod."
                                                            else "This account does not have Admin/Moderator access.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (cancel: kotlinx.coroutines.CancellationException) {
                                                throw cancel
                                            } catch (e: Exception) {
                                                android.util.Log.w("SettingsAccess", "Xác minh quyền Admin thất bại", e)
                                                Toast.makeText(
                                                    context,
                                                    if (currentLang == "vi") "Không thể xác minh quyền Admin. Hãy kiểm tra kết nối mạng."
                                                    else "Could not verify Admin access. Check your connection.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                isCheckingAdminRole = false
                                            }
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        val hasSignedInAccount =
                            loggedInUserName != null || loggedInUserEmail != null

                        if (!loggedInUserPhotoUrl.isNullOrBlank()) {
                            coil.compose.SubcomposeAsyncImage(
                                model = loggedInUserPhotoUrl,
                                contentDescription = loggedInUserName ?: "Google profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, settingsBorderColor, CircleShape),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    GoogleAccountInitialAvatar(
                                        displayName = loggedInUserName,
                                        email = loggedInUserEmail,
                                        isDarkMode = isDarkMode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                },
                                error = {
                                    GoogleAccountInitialAvatar(
                                        displayName = loggedInUserName,
                                        email = loggedInUserEmail,
                                        isDarkMode = isDarkMode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
                        } else if (hasSignedInAccount) {
                            GoogleAccountInitialAvatar(
                                displayName = loggedInUserName,
                                email = loggedInUserEmail,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(settingsIconColor.copy(alpha = 0.10f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = settingsIconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Không tạo thêm hàng "Quản trị hệ thống". Nếu có tin hỗ trợ chưa đọc,
                        // hiển thị badge ngay trên ảnh đại diện để Admin vẫn nhận biết được.
                        if (isStaff && unreadSupportCount > 0) {
                            // Badge tin cần xử lý của Admin/Mod: luôn là hình tròn đỏ,
                            // số được căn chính giữa cả chiều ngang lẫn chiều dọc.
                            val adminUnreadLabel = when {
                                unreadSupportCount > 99 -> "99+"
                                else -> unreadSupportCount.toString()
                            }
                            val adminUnreadFontSize = when {
                                unreadSupportCount > 99 -> 7.5.sp
                                unreadSupportCount > 9 -> 8.5.sp
                                else -> 9.5.sp
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 5.dp, y = (-5).dp)
                                    .size(20.dp)
                                    .background(Color.Red, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = adminUnreadLabel,
                                    color = Color.White,
                                    fontSize = adminUnreadFontSize,
                                    lineHeight = adminUnreadFontSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = TextStyle(
                                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                            includeFontPadding = false
                                        )
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(7.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = loggedInUserName
                                    ?: CoreTranslator.getString("login_google", currentLang),
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (isCheckingAdminRole) {
                                Spacer(modifier = Modifier.width(6.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = tunaGreen,
                                    strokeWidth = 2.dp
                                )
                            }
                            if (isVipAccount) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFFFFC107).copy(alpha = 0.16f),
                                            RoundedCornerShape(50)
                                        )
                                        .border(
                                            1.dp,
                                            Color(0xFFFFC107).copy(alpha = 0.60f),
                                            RoundedCornerShape(50)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "VIP",
                                        color = Color(0xFFFFC107),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (!loggedInUserEmail.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = loggedInUserEmail.orEmpty(),
                                color = settingsSecondaryText,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = settingsUiText(
                                    "Đăng nhập để đồng bộ các tính năng tài khoản",
                                    "Sign in to use account-based features"
                                ),
                                color = settingsSecondaryText,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                if (loggedInUserName != null || loggedInUserEmail != null) {
                    OutlinedButton(
                        onClick = {
                            googleSignInClient.signOut().addOnCompleteListener {
                                loggedInUserName = null
                                loggedInUserEmail = null
                                loggedInUserPhotoUrl = null
                                userRole = "user"
                                verifiedRoleEmail = ""
                                showSupportCenter = false
                                unreadSupportCount = 0
                                isVipAccount = false
                                DocumentAccessStore.clear(context)
                                Toast.makeText(
                                    context,
                                    CoreTranslator.getString("logged_out", currentLang),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .height(34.dp)
                            .widthIn(min = 108.dp),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, dangerColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dangerColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CoreTranslator.getString("logout", currentLang),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                } else {
                    Button(
                        onClick = { googleAuthLauncher.launch(googleSignInClient.signInIntent) },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF3C4043)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        GoogleGLogoCanvas(
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = CoreTranslator.getString("login_google", currentLang),
                            color = Color(0xFF3C4043),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "TuneTools · ${CoreTranslator.getString(key = "version", langCode = currentLang)} $appVersionName",
            color = settingsSecondaryText,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 1.dp)
        )
    }

    if (showGuideMenu) {
        GuideMenuDialog(
            currentLang = currentLang,
            bgColor = surfaceColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            onDismiss = { showGuideMenu = false },
            onSelectTutorial = { step ->
                showGuideMenu = false
                onStartTutorial(step)
                onNavigateToTab("tab")
            }
        )
    }

    if (isLangDialogVisible) {
        Dialog(onDismissRequest = { isLangDialogVisible = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = CoreTranslator.getString("language", currentLang), color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    LazyColumn {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUseSystemLanguage(); isLangDialogVisible = false }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSystemLanguage) Icons.Default.RadioButtonChecked
                                    else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSystemLanguage) optionSelectedColor else optionUnselectedColor,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(
                                    text = settingsUiText("Tự động (theo điện thoại)", "Automatic (phone language)"),
                                    color = textColor,
                                    fontSize = 16.sp
                                )
                            }
                            HorizontalDivider(color = textColor.copy(alpha = 0.12f))
                        }
                        items(CoreTranslator.supportedLanguages) { (code, name) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onLangChange(code); isLangDialogVisible = false }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (!isSystemLanguage && code == currentLang) {
                                        Icons.Default.RadioButtonChecked
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (!isSystemLanguage && code == currentLang) {
                                        optionSelectedColor
                                    } else {
                                        optionUnselectedColor
                                    },
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                                Text(text = name, color = textColor, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
    // --- HỘP THOẠI YÊU CẦU ĐĂNG NHẬP TRƯỚC KHI CHƠI ---
    if (showLoginPromptDialog) {
        AlertDialog(
            onDismissRequest = { showLoginPromptDialog = false },
            title = {
                Text(
                    text = CoreTranslator.getString("login_prompt_title", currentLang),
                    color = tunaGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = CoreTranslator.getString("login_prompt_desc", currentLang),
                    color = textColor,
                    fontSize = 15.sp
                )
            },
            containerColor = surfaceColor,
            confirmButton = {
                Button(
                    onClick = {
                        showLoginPromptDialog = false
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                ) {
                    Text(
                        text = CoreTranslator.getString("login_prompt_confirm", currentLang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginPromptDialog = false }) {
                    Text(
                        text = CoreTranslator.getString("login_prompt_dismiss", currentLang),
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
    if (showCoffeeDialog) {
        CoffeeSupportDialog(
            currentLang = currentLang,
            isDarkMode = isDarkMode,
            tunaGreen = tunaGreen,
            selectedMethod = selectedCoffeePayment,
            onSelectMethod = { selectedCoffeePayment = it },
            onDismiss = { showCoffeeDialog = false },
            onCopyAccount = {
                val accountId = if (selectedCoffeePayment == "momo") {
                    "PSP2630215400000072"
                } else {
                    "0041000150638"
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("Payment receiver account", accountId)
                )
                Toast.makeText(
                    context,
                    CoreTranslator.getString("coffee_copied", currentLang),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onCopyMomoPhone = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("MoMo registered phone", "0971832146")
                )
                Toast.makeText(
                    context,
                    CoreTranslator.getString("coffee_phone_copied", currentLang),
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSaveQr = {
                coffeePendingSavePayment = selectedCoffeePayment
                val filename = if (selectedCoffeePayment == "momo") {
                    "TunerTools_MoMo_QR.png"
                } else {
                    "TunerTools_Vietcombank_QR.png"
                }
                saveCoffeeQrLauncher.launch(filename)
            }
        )
    }

    if (showShareDialog) {
        Dialog(onDismissRequest = { showShareDialog = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(CoreTranslator.getString("share_app", currentLang), color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))

                    QrCodeImage(url = appLink)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("App Link", appLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, CoreTranslator.getString("link_copied", currentLang), Toast.LENGTH_SHORT).show()
                        }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(appLink, color = textColor, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = tunaGreen, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(CoreTranslator.getString("or_text", currentLang), color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Tuner & Chords App")
                                putExtra(Intent.EXTRA_TEXT, "$appLink")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, CoreTranslator.getString("share_via", currentLang)))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(CoreTranslator.getString("other_share_options", currentLang), color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showShareDialog = false }) { Text(CoreTranslator.getString("close", currentLang), color = Color.Gray) }
                }
            }
        }
    }

    if (showCopyrightDialog) {
        Dialog(onDismissRequest = { showCopyrightDialog = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = surfaceColor,
                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = CoreTranslator.getString("copyright_policy", currentLang),
                        color = tunaGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val scrollState = rememberScrollState()
                    Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {

                        Text(
                            text = if (currentLang == "vi") """
                                CHÍNH SÁCH NỘI DUNG & BẢN QUYỀN – ỨNG DỤNG TUNER TOOLS

                                Tuner tools là nền tảng cộng đồng hoàn toàn miễn phí, được xây dựng như một không gian mở để kết nối những người yêu âm nhạc. Mục tiêu chính của chúng tôi là hỗ trợ việc học tập và rèn luyện nhạc cụ (Guitar, Piano, Ukulele...) thông qua hệ thống ký âm và chia sẻ kinh nghiệm.

                                1. Cam kết về Quyền tác giả
                                • Tuân thủ pháp luật: Mọi nội dung chia sẻ phải phù hợp với quy định về sở hữu trí tuệ của Việt Nam và Quốc tế.
                                • Xác nhận trước khi đăng: Thành viên cần đảm bảo mình có quyền sử dụng hoặc được sự cho phép của tác giả trước khi đăng tải tác phẩm không do mình tự sáng tác.
                                • Xử lý vi phạm: Mọi tài liệu vi phạm bản quyền sẽ bị gỡ bỏ ngay khi được phát hiện mà không cần thông báo trước.

                                2. Hướng dẫn nội dung chia sẻ
                                • Nội dung khuyến khích: Các bài hát tự sáng tác; các bản soạn hợp âm sáng tạo; phương pháp đơn giản hóa thế bấm cho người mới bắt đầu.
                                • Lưu ý quan trọng: Tuner tools là công cụ giáo dục, không phải ứng dụng nghe nhạc, xem video hay tra cứu lời bài hát. Các đoạn trích lời bài hát (nếu có) chỉ được sử dụng duy nhất làm mốc đánh dấu vị trí hợp âm để phục vụ việc học đàn.

                                3. Hỗ trợ và Phí bản quyền
                                • Dịch vụ miễn phí: Tuner tools không thu bất kỳ khoản phí nào từ người dùng.
                                • Hỗ trợ cấp phép: Đối với các tác phẩm thuộc bên thứ ba, Ban quản trị sẽ nỗ lực kết nối với chủ sở hữu để xin phép và chi trả các khoản phí bản quyền phát sinh (nếu có) để duy trì nội dung trên nền tảng. Quá trình này có thể ảnh hưởng đến thời gian duyệt bài của bạn.

                                4. Báo cáo & Phản hồi
                                Chúng tôi luôn sẵn sàng lắng nghe và bảo vệ quyền lợi chính đáng của các tác giả. Nếu phát hiện bất kỳ sai sót hoặc vi phạm nào, vui lòng gửi phản hồi về:
                            """.trimIndent() else """
                                CONTENT & COPYRIGHT POLICY – TUNER TOOLS APP

                                Tuner tools is a completely free community platform built to connect music lovers. Our main goal is to support the learning and practice of instruments (Guitar, Piano, Ukulele...) through chords and shared experiences.

                                1. Copyright Commitment
                                • Compliance: All shared content must comply with intellectual property laws.
                                • Verify Before Posting: Ensure you have the right or permission from the author before uploading third-party works.
                                • Violations: Copyrighted material without permission will be removed without notice.

                                2. Sharing Guidelines
                                • Encouraged: Original songs, creative chord arrangements, and simplified methods for beginners.
                                • Important: Tuner tools is an educational tool. Lyrics are used solely as markers for chord positions.

                                3. Support & Fees
                                • Free Service: Tuner tools does not charge users.
                                • Licensing: We will attempt to contact owners to license third-party works.

                                4. Reporting
                                For copyright issues, please contact:
                            """.trimIndent(),
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).clickable {
                                // Open Gmail compose first; fall back to any email app if Gmail is unavailable.
                                val reportTitle = CoreTranslator.getString("submit_report", currentLang)
                                val mailSubject = "TunerTools - $reportTitle"
                                val mailUri = Uri.parse(
                                    "mailto:tamthanhphuc@gmail.com?subject=${Uri.encode(mailSubject)}"
                                )
                                val gmailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                                    setPackage("com.google.android.gm")
                                }
                                try {
                                    context.startActivity(gmailIntent)
                                } catch (_: Exception) {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, mailUri))
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            context,
                                            CoreTranslator.getString("email_app_not_found", currentLang),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Email", tint = tunaGreen, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Email: tamthanhphuc@gmail.com", color = tunaGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(8.dp)).clickable {
                                showCopyrightDialog = false
                                showSupportCenter = true
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.Red, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(CoreTranslator.getString("submit_report", currentLang), color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (currentLang == "vi") """
                                • Thời gian xử lý: Tối đa 05 ngày làm việc kể từ khi nhận được thông tin.

                                Bằng việc đăng tải nội dung, bạn xác nhận đã hiểu và đồng ý cùng Tuner tools xây dựng một cộng đồng âm nhạc văn minh, thượng tôn pháp luật.
                            """.trimIndent() else """
                                • Processing time: Up to 5 working days.
                            """.trimIndent(),
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showCopyrightDialog = false }) {
                            Text(CoreTranslator.getString("close", currentLang), color = tunaGreen)
                        }
                    }
                }
            }
        }
    }
    if (showSupportCenter) {
        SupportCenterDialog(
            userRole = userRole,
            deviceId = deviceId,
            onDismiss = { showSupportCenter = false },
            tunaGreen = tunaGreen,
            surfaceColor = surfaceColor,
            bgColor = bgColor,
            textColor = textColor,
            currentLang = currentLang,
            isDarkMode = isDarkMode,
            onNavigateToTab = onNavigateToTab //  ĐÃ THÊM: Truyền hàm chuyển tab từ SettingsScreen xuống
        )
    }

// --- HỘP THOẠI CÀI ĐẶT THÔNG BÁO ---
    if (showNotificationDialog) {
        Dialog(onDismissRequest = { showNotificationDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = CoreTranslator.getString("notification_settings_title", currentLang),
                        color = tunaGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 1. Công tắc bật/tắt
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(CoreTranslator.getString("practice_reminder", currentLang), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(CoreTranslator.getString("practice_reminder_desc", currentLang), color = Color.Gray, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        isReminderEnabled = true
                                        prefs.edit().putBoolean("isReminderEnabled", true).apply()
                                        scheduleReminder(context)
                                    }
                                } else {
                                    isReminderEnabled = false
                                    prefs.edit().putBoolean("isReminderEnabled", false).apply()
                                    cancelReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = switchCheckedThumbColor,
                                checkedTrackColor = switchCheckedTrackColor,
                                uncheckedThumbColor = switchUncheckedThumbColor,
                                uncheckedTrackColor = switchUncheckedTrackColor,
                                checkedBorderColor = Color.Transparent,
                                uncheckedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }

                    // 2. Tùy chỉnh Thời gian và Lời nhắc (Chỉ hiện khi đang bật)
                    AnimatedVisibility(visible = isReminderEnabled) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            // Chọn Giờ
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            reminderHour = hourOfDay
                                            reminderMinute = minute
                                            prefs.edit().putInt("reminderHour", hourOfDay).putInt("reminderMinute", minute).apply()
                                            // Cập nhật lại lịch khi đổi giờ
                                            scheduleReminder(context)
                                        },
                                        reminderHour, reminderMinute, true
                                    ).show()
                                }.background(bgColor, RoundedCornerShape(8.dp)).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(CoreTranslator.getString("time_lbl", currentLang), color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = String.format("%02d:%02d", reminderHour, reminderMinute),
                                    color = optionSelectedColor, fontSize = 16.sp, fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Nhập lời nhắc
                            OutlinedTextField(
                                value = reminderMessage,
                                onValueChange = {
                                    reminderMessage = it
                                    prefs.edit().putString("reminderMessage", it).apply()
                                },
                                label = { Text(CoreTranslator.getString("reminder_message_label", currentLang), color = Color.Gray) },
                                textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = bgColor,
                                    unfocusedContainerColor = bgColor,
                                    focusedIndicatorColor = optionSelectedColor
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showNotificationDialog = false }) {
                            Text(CoreTranslator.getString("close", currentLang), color = tunaGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- HỘP THOẠI CẤU HÌNH MENU HIỂN THỊ ---
    if (showMenuConfigDialog) {
        var tempSelectedTabs by remember { mutableStateOf(visibleTabs) }

        val tabOptions = listOf(
            Triple("tab", CoreTranslator.getString("tab", currentLang), CustomIcons.MusicScore),
            Triple("Chorus", CoreTranslator.getString("chorus", currentLang), CustomIcons.Chord),
            Triple("Tune", CoreTranslator.getString("tune", currentLang), CustomIcons.Tuner),
            Triple("Metronome", CoreTranslator.getString("tempo", currentLang), CustomIcons.Metronome),
            Triple("minigame_hub", CoreTranslator.getString("games_leaderboard_title", currentLang), Icons.Default.SportsEsports)
        )

        Dialog(onDismissRequest = { showMenuConfigDialog = false }) {
            Surface(shape = RoundedCornerShape(12.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = CoreTranslator.getString("menu_display_config", currentLang),
                        color = tunaGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    tabOptions.forEach { (tabKey, tabName, tabIcon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val isCurrentlyChecked = tempSelectedTabs.contains(tabKey)
                                    if (isCurrentlyChecked) {
                                        // Bắt buộc giữ lại ít nhất 1 tab
                                        if (tempSelectedTabs.size > 1) {
                                            tempSelectedTabs = tempSelectedTabs - tabKey
                                        } else {
                                            Toast.makeText(context, CoreTranslator.getString("min_1_menu_required", currentLang), Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        tempSelectedTabs = tempSelectedTabs + tabKey
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = tempSelectedTabs.contains(tabKey),
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = optionSelectedColor,
                                    uncheckedColor = optionUnselectedColor,
                                    checkmarkColor = if (isDarkMode) Color.Black else Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = tabName,
                                tint = if (tempSelectedTabs.contains(tabKey)) textColor else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = tabName, color = textColor, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showMenuConfigDialog = false }) {
                            Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                onVisibleTabsChange(tempSelectedTabs)
                                showMenuConfigDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(CoreTranslator.getString("save", currentLang), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
/**
 * Create the image to export from one QR asset plus programmatically drawn text.
 * This avoids two redundant *_card.png drawables and does not require network
 * access or storage permissions (CreateDocument handles destination access).
 * QR pixels are pasted at their native resolution to keep the code sharp.
 */
private fun createCoffeeQrSaveBitmap(
    context: Context,
    paymentMethod: String
): android.graphics.Bitmap {
    val isMomo = paymentMethod == "momo"
    val qrDrawable = if (isMomo) R.drawable.coffee_momo_qr else R.drawable.coffee_vietqr_qr
    val qrBitmap = BitmapFactory.decodeResource(context.resources, qrDrawable)
        ?: error("QR bitmap is not available")

    // The developer may replace *_qr.png with the earlier, taller QR cards.
    // If so, export that complete, styled card as-is. Reframing a portrait
    // card would duplicate or overlap its title and payment details.
    // For the original square QR images, keep generating the shareable card.
    if (qrBitmap.height > qrBitmap.width * 1.10f) {
        return qrBitmap // The save launcher recycles the returned bitmap.
    }

    try {
        val width = 1100
        val height = 1530
        val card = android.graphics.Bitmap.createBitmap(
            width, height, android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(card)
        canvas.drawColor(android.graphics.Color.WHITE)

        val accent = if (isMomo) {
            android.graphics.Color.rgb(176, 28, 121)
        } else {
            android.graphics.Color.rgb(23, 121, 93)
        }
        val darkText = android.graphics.Color.rgb(32, 38, 44)
        val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 212f, headerPaint)

        fun centeredText(
            value: String,
            baseline: Float,
            fontSize: Float,
            ink: Int,
            bold: Boolean = false
        ) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = fontSize
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                )
            }
            val maxWidth = width - 120f
            val measured = paint.measureText(value)
            if (measured > maxWidth) paint.textSize *= maxWidth / measured
            canvas.drawText(value, width / 2f, baseline, paint)
        }

        centeredText(if (isMomo) "MoMo" else "Vietcombank", 103f, 67f,
            android.graphics.Color.WHITE, true)
        centeredText("TUNER TOOLS  -  THANK YOU FOR YOUR SUPPORT", 163f, 29f,
            android.graphics.Color.WHITE)

        // Both QR PNGs include their white quiet zones; never crop them.
        val qrAreaTop = 255f
        val qrAreaHeight = 950f
        val left = ((width - qrBitmap.width) / 2).toFloat()
        val top = qrAreaTop + ((qrAreaHeight.toInt() - qrBitmap.height) / 2).toFloat()
        val qrPaint = android.graphics.Paint().apply { isFilterBitmap = false }
        canvas.drawBitmap(qrBitmap, left, top, qrPaint)

        val dividerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(222, 228, 231)
            strokeWidth = 2f
        }
        canvas.drawLine(100f, 1250f, 1000f, 1250f, dividerPaint)
        centeredText("NGUYEN DUC PHI", 1324f, 47f, darkText, true)
        centeredText(
            if (isMomo) "MoMo account: PSP2630215400000072"
            else "Account: 0041000150638",
            1400f, 39f, darkText
        )
        if (isMomo) {
            centeredText("Phone: 0971832146", 1467f, 36f, darkText)
        }
        return card
    } finally {
        qrBitmap.recycle()
    }
}

/**
 * Show two QR codes rebuilt from the original Vietcombank and MoMo payment
 * payloads. Both assets are bundled locally and no payment is made in-app.
 */
@Composable
private fun CoffeeSupportDialog(
    currentLang: String,
    isDarkMode: Boolean,
    tunaGreen: Color,
    selectedMethod: String,
    onSelectMethod: (String) -> Unit,
    onDismiss: () -> Unit,
    onCopyAccount: () -> Unit,
    onCopyMomoPhone: () -> Unit,
    onSaveQr: () -> Unit
) {
    val foreground = if (isDarkMode) Color.White else Color(0xFF1B1C20)
    val fullScreenBackground = if (isDarkMode) Color.Black else Color(0xFFF5F5F7)
    val imageBackground = if (isDarkMode) Color(0xFF101619) else Color.White
    val isMomo = selectedMethod == "momo"
    val bankName = if (isMomo) "MoMo" else "Vietcombank"
    val accountId = if (isMomo) "PSP2630215400000072" else "0041000150638"
    val bankAccent = if (isMomo) Color(0xFFB01C79) else Color(0xFF17795D)
    val qrResource = if (isMomo) R.drawable.coffee_momo_qr else R.drawable.coffee_vietqr_qr

    // Read device size before the dialog so the layout does not depend on
    // BoxWithConstraints' subcomposition scope, which fails in some setups.
    val coffeeScreenConfig = LocalConfiguration.current
    val compactLayout = coffeeScreenConfig.screenHeightDp < 700
    val sidePadding = if (coffeeScreenConfig.screenWidthDp < 350) 10.dp else 16.dp

    // Full-width Dialog instead of a centered, height-limited popup.
    // It covers Settings and the bottom app menu, with Android Back to close.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = fullScreenBackground,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = sidePadding)
                        .padding(
                            top = if (compactLayout) 6.dp else 12.dp,
                            bottom = if (compactLayout) 6.dp else 12.dp
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Dedicated full-screen header: back arrow replaces popup X.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = CoreTranslator.getString("close", currentLang),
                                tint = foreground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = tunaGreen,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = CoreTranslator.getString("coffee_title", currentLang),
                            modifier = Modifier.weight(1f),
                            color = foreground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            lineHeight = 20.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // On short phones prioritize the QR and buttons over help text.
                    if (!compactLayout) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CoreTranslator.getString("coffee_message", currentLang),
                            color = foreground.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Tabs have identical heights; switching does not move the QR.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("vietcombank" to "Vietcombank", "momo" to "MoMo").forEach { (method, label) ->
                            val active = selectedMethod == method
                            Surface(
                                onClick = { onSelectMethod(method) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) tunaGreen.copy(alpha = 0.14f)
                                        else foreground.copy(alpha = 0.04f),
                                border = BorderStroke(
                                    if (active) 1.5.dp else 1.dp,
                                    if (active) tunaGreen else foreground.copy(alpha = 0.11f)
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (active) tunaGreen else foreground,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (compactLayout) 6.dp else 10.dp))

                    // Key change: fill all remaining vertical space with the QR image.
                    // The portrait cards the user chose are never cropped, stretched,
                    // or replaced. Both methods share the same weighted image area.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(15.dp))
                            .background(imageBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(qrResource),
                            contentDescription = "$bankName payment QR - $accountId",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (compactLayout) 3.dp else 6.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(if (compactLayout) 5.dp else 8.dp))
                    Text(
                        text = "$bankName  ·  NGUYEN DUC PHI",
                        color = bankAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().height(26.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = accountId,
                            color = foreground,
                            fontSize = if (isMomo) 15.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Keep this line reserved for both banks: equal fullscreen layout.
                    Box(
                        modifier = Modifier.fillMaxWidth().height(23.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMomo) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onCopyMomoPhone() }
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CoreTranslator.getString("coffee_momo_phone", currentLang) + ": 0971832146",
                                    color = foreground.copy(alpha = 0.72f),
                                    fontSize = 10.5.sp,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = CoreTranslator.getString("coffee_copy", currentLang),
                                    tint = bankAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    if (!compactLayout) {
                        Text(
                            text = CoreTranslator.getString("coffee_scan_hint", currentLang),
                            color = foreground.copy(alpha = 0.65f),
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(if (compactLayout) 5.dp else 9.dp))

                    // Action bar stays visible; it never overlays the QR.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCopyAccount,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, tunaGreen.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = tunaGreen,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CoreTranslator.getString("coffee_copy", currentLang),
                                fontSize = 11.5.sp,
                                color = tunaGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = onSaveQr,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = CoreTranslator.getString("coffee_save", currentLang),
                                fontSize = 11.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (compactLayout) 3.dp else 7.dp))
                    Text(
                        text = CoreTranslator.getString("coffee_optional", currentLang),
                        color = foreground.copy(alpha = 0.55f),
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = title,
            color = textColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.45.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ModernSettingRow(
    title: String,
    subtitle: String? = null,
    trailingText: String? = null,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color,
    dividerColor: Color,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    badgeCount: Int = 0,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(
                    horizontal = 12.dp,
                    vertical = if (subtitle.isNullOrBlank()) 9.dp else 7.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )

                if (badgeCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-7).dp)
                            .size(17.dp)
                            .background(Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        color = textColor.copy(alpha = 0.58f),
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(7.dp))
            }

            when {
                trailingContent != null -> trailingContent()
                showChevron -> Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.62f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 43.dp, end = 12.dp),
                thickness = 1.dp,
                color = dividerColor
            )
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = Color.Gray,
    textColor: Color,
    badgeCount: Int = 0,
    badgeText: String? = null, // Nhận text cho nhãn (VD: "NEW")
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(28.dp))

            // Xử lý đếm số lượng thông báo (Badge đếm số) - Vẫn giữ nguyên trên icon
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).size(18.dp).background(Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = if (badgeCount > 99) "99+" else badgeCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Đưa Tiêu đề và Nhãn "NEW" vào cùng một hàng ngang
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = textColor, fontSize = 18.sp)

                // Hiển thị nhãn chữ ở cuối câu
                if (!badgeText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp)) // Tạo khoảng cách giữa chữ và nhãn
                    Box(
                        modifier = Modifier
                            .background(Color.Yellow, RoundedCornerShape(4.dp)) // Nền vàng, bo góc nhẹ
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.Red, // Chữ đỏ
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            if (subtitle != null) { Text(subtitle, color = Color.Gray, fontSize = 14.sp) }
        }
    }
}

@Composable
fun InstrumentSelectionDialog(
    currentLang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bgColor,
            modifier = Modifier.fillMaxWidth().border(1.dp, tunaGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = CoreTranslator.getString("select_inst_title", currentLang),
                    color = tunaGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold
                )
                Text(
                    text = CoreTranslator.getString("select_inst_desc", currentLang),
                    color = textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // [GIAO DIỆN MỚI] 3 khối 3D xếp ngang với 3 tông màu riêng biệt
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cấu hình: Tên nhạc cụ, Màu Gradient trên (sáng), Màu Gradient dưới (tối)
                    val instrumentData = listOf(
                        Triple("Guitar", Color(0xFFFF7043), Color(0xFFD84315)),   // Cam đỏ
                        Triple("Ukulele", Color(0xFFFFCA28), Color(0xFFF57F17)),  // Vàng cam
                        Triple("Piano", Color(0xFF42A5F5), Color(0xFF1565C0))     // Xanh dương
                    )

                    instrumentData.forEach { (inst, colorTop, colorBottom) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f) // Ép tỷ lệ thành khối chữ nhật đứng mập mạp
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(colorTop, colorBottom)
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.6f), Color.Black.copy(alpha = 0.4f))
                                    ), // Hiệu ứng hắt sáng viền trên, đổ bóng viền dưới tạo 3D
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelect(inst) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = getInstIcon(inst)),
                                    contentDescription = inst,
                                    modifier = Modifier.size(34.dp) // Tăng size Icon lên để làm điểm nhấn
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = CoreTranslator.getString("inst_${inst.lowercase()}", currentLang), // Sẽ tự map thành inst_guitar, inst_ukulele, inst_piano
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
fun FirstTimeLanguageScreen(
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onLanguageSelected: (String) -> Unit
) {
    // --- TẠO HIỆU ỨNG CHUYỂN ĐỘNG DÙNG CHUNG CHO LOGO VÀ TEXT ---
    val infiniteTransition = rememberInfiniteTransition(label = "Animation")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -10f, // Bắt đầu ở vị trí cao hơn 10px
        targetValue = 10f,   // Di chuyển xuống thấp hơn 10px
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing), // Chu kỳ 1.5 giây
            repeatMode = RepeatMode.Reverse // Lặp lại dạng đảo ngược (lên - xuống - lên)
        ),
        label = "OffsetY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // --- 1. HIỂN THỊ ẢNH LOGO (CÓ HIỆU ỨNG CHUYỂN ĐỘNG) ---
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(100.dp)
                .offset(y = offsetY.dp) // Ảnh nhấp nhô
                .clip(RoundedCornerShape(20.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. TEXT NHIỀU MÀU (CŨNG CÓ HIỆU ỨNG CHUYỂN ĐỘNG) ---
        Text(
            text = buildAnnotatedString {
                // Chữ "Welcome to " - Màu tím nhạt
                withStyle(style = SpanStyle(color = Color(0xFFCE93D8))) {
                    append("Welcome to ")
                }
                // Chữ "Tuner " - Màu trắng
                withStyle(style = SpanStyle(color = Color.White)) {
                    append("Tuner ")
                }
                // Chữ "Tools!" - Màu xanh da trời
                withStyle(style = SpanStyle(color = Color(0xFF29B6F6))) {
                    append("Tools!")
                }
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.offset(y = offsetY.dp) // <-- Gắn thêm offset để chữ cùng chuyển động
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "To use effectively, please select your language below.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))

        // --- 3. DANH SÁCH NGÔN NGỮ ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(surfaceColor, RoundedCornerShape(16.dp))
                .padding(8.dp)
        ) {
            items(CoreTranslator.supportedLanguages) { (code, name) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLanguageSelected(code) }
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = name, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = tunaGreen)
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
class ReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val prefs = applicationContext.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE)
        val currentLang = prefs.getString("appLang", "en") ?: "en"

        val defaultTitle = CoreTranslator.getString("reminder_default_title", currentLang)
        val customMsg = prefs.getString("reminderMessage", defaultTitle) ?: defaultTitle
        val defaultBody = CoreTranslator.getString("reminder_default_body", currentLang)
        val channelName = CoreTranslator.getString("practice_reminder", currentLang)

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "practice_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(customMsg)
            .setContentText(defaultBody)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}

fun scheduleReminder(context: Context) {
    // Đọc giờ giấc tự chọn của người dùng (Mặc định 20:30)
    val prefs = context.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE)
    val hour = prefs.getInt("reminderHour", 20)
    val minute = prefs.getInt("reminderMinute", 30)

    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    if (dueDate.before(currentDate)) {
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
    }

    val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

    val dailyWorkRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
        .addTag("daily_practice_reminder")
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "daily_practice_reminder",
        ExistingPeriodicWorkPolicy.REPLACE,
        dailyWorkRequest
    )
}

fun cancelReminder(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("daily_practice_reminder")
}
// =========================================================================
// FIREBASE CLOUD MESSAGING (FCM) SERVICE - ĐẨY THÔNG BÁO TỨC THỜI
// =========================================================================
class MyFirebaseMessagingService : com.google.firebase.messaging.FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: com.google.firebase.messaging.RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Nhận dữ liệu đẩy từ PHP
        val title = remoteMessage.notification?.title ?: "Thông báo mới"
        val body = remoteMessage.notification?.body ?: ""

        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "system_push_notification_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Thông báo quan trọng",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo) // Thay logo của bạn nếu cần
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}