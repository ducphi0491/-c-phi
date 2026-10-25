package duc_phi.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.Calendar
import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import android.os.SystemClock
import android.util.Log
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.UnfetchedProduct
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
// region Merged source: MainActivity.kt
// Language mode is kept separate from app_language so automatic device language
// does not overwrite the user's explicit selection. Legacy app_language stays valid.
private const val MELOWARE_LANGUAGE_MODE_KEY = "app_language_mode"
private const val MELOWARE_LANGUAGE_MODE_DEVICE = "device"
private const val MELOWARE_LANGUAGE_MODE_MANUAL = "manual"

// Presets retain app_accent_color unchanged. Custom hue is a separate saved value.
private const val MELOWARE_ACCENT_HUE_KEY = "app_accent_hue"
private const val MELOWARE_CUSTOM_ACCENT_ID = "custom"

private const val MELOWARE_ADMIN_EMAIL = "ducphi0491@gmail.com"
private const val GOOGLE_DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
// Personal Drive browsing uses read-only access. Google Picker/add-file keeps drive.file
// as a separate grant so the two tokens never overwrite each other.
private const val GOOGLE_DRIVE_READONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly"
private const val GOOGLE_PICKER_FILE_IDS_PARAM = "picked_file_ids"
private const val GOOGLE_PICKER_AUDIO_MIME_TYPES =
    "application/vnd.google-apps.folder," +
            "audio/mpeg,audio/mp4,audio/x-m4a,audio/aac,audio/flac,audio/x-flac," +
            "audio/wav,audio/x-wav,audio/ogg,audio/opus,audio/amr,audio/3gpp," +
            "audio/webm,audio/x-matroska,audio/ac3,audio/eac3,audio/ac4,video/mp2t"

// The explanation is a local, one-time notice; Google alone grants OAuth access.
private enum class PersonalDriveExplanationMode { READ_ONLY, ADD_FILE }

// Keep the acknowledgement distinct for every signed-in account and scope.
// Only a short digest is used in the SharedPreferences key.
private fun personalDriveExplanationKey(mode: PersonalDriveExplanationMode, email: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(
        email.trim().lowercase(Locale.ROOT).toByteArray(StandardCharsets.UTF_8)
    )
    val digest = bytes.take(12).joinToString("") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
    return "personal_drive_notice_v1_${mode.name}_$digest"
}

private enum class PendingPersonalDriveAction {
    READ_ONLY_BROWSER,
    READ_ONLY_REFRESH,
    ADD_FILE_PICKER,
    ADD_FILE_REFRESH
}

class MainActivity : FragmentActivity() {
    private lateinit var appUpdateManager: AppUpdateManager
    private var mainUiStarted = false
    private var showSplashOnLaunch = true
    // Reapply the correct system bar appearance after app/theme changes and focus returns.
    private var systemBarsDark = true
    private var updateCheckInProgress = false
    private var updateFlowLaunching = false
    // Chi kiem tra lai khi quay ve tu Google Play neu da biet ban cap nhat la bat buoc.
    private var mandatoryUpdateRequired = false
    private var mandatoryUpdateDialog: android.app.AlertDialog? = null
    // Compose observes this when the user changes the device language.
    private var systemLanguageTag by mutableStateOf(java.util.Locale.getDefault().toLanguageTag())

    private val mandatoryUpdateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        updateFlowLaunching = false
        if (result.resultCode == Activity.RESULT_OK) {
            // Immediate Update normally restarts the app. Recheck defensively if it returns.
            checkForMandatoryUpdate()
        } else {
            showMandatoryUpdateDialog(
                message = "Bạn phải cập nhật MeloWare lên phiên bản mới nhất trước khi tiếp tục sử dụng.",
                openPlayStoreDirectly = false
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSplashOnLaunch = savedInstanceState == null
        appUpdateManager = AppUpdateManagerFactory.create(this)
        // Activate the free 30-day Premium trial on FIRST APP OPEN, not first playback.
        // The call is asynchronous; PHP decides eligibility and expiry.
        MeloWaveTrial.refresh(applicationContext)
        // Hien giao dien ngay; Play In-App Update duoc kiem tra bat dong bo.
        openMeloWareUi()
        checkForMandatoryUpdate()
    }

    override fun onResume() {
        super.onResume()
        // Refresh trial status on resume; eligible devices start automatically
        // without requiring playback, while the server prevents extending 30 days.
        MeloWaveTrial.refresh(applicationContext)
        // Refresh the language when coming back from Android Settings.
        systemLanguageTag = java.util.Locale.getDefault().toLanguageTag()
        // Khong kiem tra lap lai khi nguoi dung tro ve tu Drive, Cast, dang nhap...
        // Neu dang bat buoc cap nhat, kiem tra lai khi tro ve tu Google Play.
        if (
            !::appUpdateManager.isInitialized ||
            !mandatoryUpdateRequired ||
            updateCheckInProgress ||
            updateFlowLaunching ||
            mandatoryUpdateDialog?.isShowing == true
        ) return
        checkForMandatoryUpdate()
    }

    /**
     * Kiem tra Google Play am tham, khong chiem man hinh luc mo app.
     * Khi Google Play bao co cap nhat, van yeu cau IMMEDIATE update.
     */
    private fun checkForMandatoryUpdate() {
        if (
            updateCheckInProgress ||
            updateFlowLaunching ||
            isFinishing ||
            isDestroyed
        ) return

        updateCheckInProgress = true
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                updateCheckInProgress = false
                when {
                    info.updateAvailability() ==
                            UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        mandatoryUpdateRequired = true
                        startImmediateUpdate(info)
                    }

                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        mandatoryUpdateRequired = true
                        if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            startImmediateUpdate(info)
                        } else {
                            showMandatoryUpdateDialog(
                                message = "MeloWare đã có phiên bản mới. Thiết bị này cần mở Google Play để cập nhật trước khi tiếp tục.",
                                openPlayStoreDirectly = true
                            )
                        }
                    }

                    else -> {
                        mandatoryUpdateRequired = false
                        mandatoryUpdateDialog?.dismiss()
                        mandatoryUpdateDialog = null
                    }
                }
            }
            .addOnFailureListener {
                updateCheckInProgress = false
                // Loi mang/Google Play: khong che man hinh, van nghe nhac offline.
                // Neu Play truoc do da xac nhan phai cap nhat, giu yeu cau cap nhat.
                if (mandatoryUpdateRequired && mandatoryUpdateDialog?.isShowing != true) {
                    showMandatoryUpdateDialog(
                        message = "Không thể kiểm tra lại bản cập nhật. Vui lòng cập nhật MeloWare trên Google Play.",
                        openPlayStoreDirectly = true
                    )
                }
            }
    }

    private fun startImmediateUpdate(info: AppUpdateInfo) {
        if (updateFlowLaunching || isFinishing || isDestroyed) return
        updateFlowLaunching = true

        val started = runCatching {
            appUpdateManager.startUpdateFlowForResult(
                info,
                mandatoryUpdateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
            )
        }.getOrDefault(false)

        if (!started) {
            updateFlowLaunching = false
            showMandatoryUpdateDialog(
                message = "Không thể mở màn hình cập nhật tự động. Vui lòng cập nhật MeloWare trực tiếp trên Google Play.",
                openPlayStoreDirectly = true
            )
        }
    }

    private fun showMandatoryUpdateDialog(
        message: String,
        openPlayStoreDirectly: Boolean
    ) {
        if (isFinishing || isDestroyed) return

        mandatoryUpdateDialog?.dismiss()
        mandatoryUpdateDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Cập nhật MeloWare bắt buộc")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Cập nhật ngay") { _, _ ->
                if (openPlayStoreDirectly) {
                    openMeloWareOnPlayStore()
                } else {
                    checkForMandatoryUpdate()
                }
            }
            .setNegativeButton("Thoát ứng dụng") { _, _ ->
                finishAffinity()
            }
            .create()
            .also { dialog ->
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
            }
    }

    private fun openMeloWareOnPlayStore() {
        val marketIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$packageName")
        ).apply {
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching { startActivity(marketIntent) }
            .onFailure { runCatching { startActivity(webIntent) } }
    }

    private fun openMeloWareUi() {
        if (mainUiStarted || isFinishing || isDestroyed) return
        mainUiStarted = true
        mandatoryUpdateDialog?.dismiss()
        mandatoryUpdateDialog = null
        enableEdgeToEdge()
        // Apply the saved theme immediately, avoiding a dark navigation-bar flash
        // before Compose draws the first light frame.
        val savedThemeMode = getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
            .getInt("app_theme_mode", 2)
        systemBarsDark = if (showSplashOnLaunch) true else when (savedThemeMode) {
            1 -> false
            2 -> true
            else -> (resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        configureSystemBars()
        com.google.android.gms.cast.framework.CastContext.getSharedInstance(
            applicationContext,
            ContextCompat.getMainExecutor(this)
        )

        // Khoi tao Google Mobile Ads mot lan khi ung dung mo.
        Thread {
            com.google.android.gms.ads.MobileAds.initialize(this@MainActivity) { }
        }.start()

        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
            }
            val controller = remember {
                // Service and UI share the SAME controller/player. Do not create an
                // Activity-owned ExoPlayer that disappears on screen lock/rotation.
                MeloWavePlaybackRuntime.obtain(context.applicationContext)
            }

            val translationCatalog = remember(context.applicationContext) {
                MeloWareTranslationCatalog.load(context.applicationContext)
            }
            val languageOptions = translationCatalog.languages
            val savedLanguage = remember(prefs) {
                translationCatalog.resolveLanguage(prefs.getString("app_language", null))
            }
            // Older versions stored both an auto-detected and a manually selected code
            // under app_language. Preserve that value as manual until the user opts
            // into device-language mode; do not silently override existing choices.
            var languageMode by remember(prefs) {
                mutableStateOf(
                    when (prefs.getString(MELOWARE_LANGUAGE_MODE_KEY, null)) {
                        MELOWARE_LANGUAGE_MODE_DEVICE -> MELOWARE_LANGUAGE_MODE_DEVICE
                        MELOWARE_LANGUAGE_MODE_MANUAL -> {
                            if (savedLanguage != null) MELOWARE_LANGUAGE_MODE_MANUAL
                            else MELOWARE_LANGUAGE_MODE_DEVICE
                        }
                        else -> {
                            if (savedLanguage != null) MELOWARE_LANGUAGE_MODE_MANUAL
                            else MELOWARE_LANGUAGE_MODE_DEVICE
                        }
                    }
                )
            }
            var manualLanguage by remember(prefs) {
                mutableStateOf(savedLanguage ?: translationCatalog.defaultLanguage)
            }
            // Update automatically after a system language change if Auto is selected.
            val deviceLanguage = translationCatalog.resolveLanguage(systemLanguageTag)
            val followDeviceLanguage = languageMode == MELOWARE_LANGUAGE_MODE_DEVICE
            val language = if (followDeviceLanguage) {
                deviceLanguage ?: translationCatalog.defaultLanguage
            } else {
                manualLanguage
            }
            val requireInitialLanguageChoice = followDeviceLanguage && deviceLanguage == null
            // 0: theo hệ thống, 1: sáng, 2: tối
            var themeMode by remember {
                mutableIntStateOf(prefs.getInt("app_theme_mode", 2))
            }
            // Accent is stored locally. Existing installs start with the original purple.
            var accentId by remember {
                mutableStateOf(
                    prefs.getString("app_accent_color", "purple")
                        ?.takeIf { saved ->
                            saved == MELOWARE_CUSTOM_ACCENT_ID || MusicAccentOptions.any { it.id == saved }
                        }
                        ?: "purple"
                )
            }
            // Only used when the user selects the rainbow hue slider.
            var customAccentHue by remember(prefs) {
                mutableStateOf(
                    prefs.getFloat(MELOWARE_ACCENT_HUE_KEY, 275f)
                        .takeIf { it.isFinite() }?.coerceIn(0f, 360f) ?: 275f
                )
            }
            // Show branding once on a fresh Activity launch, not when returning from
            // Google Drive, Play Billing, or when the device recreates this Activity.
            var showLaunchSplash by remember { mutableStateOf(showSplashOnLaunch) }
            var showSettings by remember { mutableStateOf(false) }
            var showPremiumDialog by remember { mutableStateOf(false) }
            var showLanguageDialog by remember { mutableStateOf(false) }
            var showThemeDialog by remember { mutableStateOf(false) }
            var showAccentDialog by remember { mutableStateOf(false) }
            var showAdminDashboard by remember { mutableStateOf(false) }
            var pendingPersonalDriveAction by remember {
                mutableStateOf<PendingPersonalDriveAction?>(null)
            }
            // Display only before the first successful grant for each Google account.
            var personalDriveExplanation by remember {
                mutableStateOf<PersonalDriveExplanationMode?>(null)
            }
            // OAuth for uploading from online links is independent from both personal Drive modes.
            var pendingOpenLinkDriveUpload by remember { mutableStateOf(false) }
            var googleSignInSuccessSerial by remember { mutableIntStateOf(0) }
            var awaitingPersonalDrivePickerResult by remember { mutableStateOf(false) }
            // Explicit read-only entry opens the in-app folder browser after authorization.
            // Silent token refresh must never reopen that browser.
            var openPersonalDriveBrowserAfterReadOnlyConsent by remember { mutableStateOf(false) }
            // Chi hien nut Admin khi tai khoan Google dang nhap trung khop,
            // khong dua vao email ghi trong SharedPreferences.
            var verifiedAdmin by remember {
                mutableStateOf(
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getLastSignedInAccount(context)?.email
                        .equals(MELOWARE_ADMIN_EMAIL, ignoreCase = true)
                )
            }

            val googleSignInOptions = remember {
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                    com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                )
                    .requestIdToken("886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com")
                    .requestEmail()
                    .requestProfile()
                    .build()
            }
            val googleSignInClient = remember {
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, googleSignInOptions)
            }
            var currentUserEmail by remember {
                mutableStateOf(
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getLastSignedInAccount(context)?.email
                        ?: prefs.getString("logged_in_email", null)
                )
            }

            // Keep BillingClient alive for the Activity (not just the Premium dialog).
            // Otherwise purchases can complete after the dialog closes without verification.
            val premiumBilling = remember(this@MainActivity) {
                MeloWarePremiumBillingManager(this@MainActivity)
            }
            DisposableEffect(premiumBilling) {
                premiumBilling.start()
                onDispose { premiumBilling.close() }
            }
            val premiumLifecycle = LocalLifecycleOwner.current
            DisposableEffect(premiumLifecycle, premiumBilling) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) premiumBilling.onResume()
                }
                premiumLifecycle.lifecycle.addObserver(observer)
                onDispose { premiumLifecycle.lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(currentUserEmail) {
                // Re-check PAID account on sign-in/out, but keep the guest trial.
                premiumBilling.onAccountChanged()
            }
            LaunchedEffect(Unit) {
                // Update entitlement near its expiry even when the user stays in the app.
                while (true) {
                    kotlinx.coroutines.delay(30_000L)
                    MeloWaveTrial.tick()
                    // Retry first-open activation after a temporary network failure,
                    // even if the user never leaves this screen or plays music.
                    if (MeloWaveTrial.visibleState == "unknown" ||
                        MeloWaveTrial.visibleState == "eligible") {
                        MeloWaveTrial.refresh(context.applicationContext)
                    }
                }
            }
            val personalDriveReadOnlyScope = remember {
                com.google.android.gms.common.api.Scope(GOOGLE_DRIVE_READONLY_SCOPE)
            }
            val personalDriveAddFileScope = remember {
                com.google.android.gms.common.api.Scope(GOOGLE_DRIVE_FILE_SCOPE)
            }
            val linkUploadScope = remember {
                com.google.android.gms.common.api.Scope(GOOGLE_DRIVE_FILE_SCOPE)
            }
            val authorizationClient = remember {
                com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(context)
            }

            val googleAuthLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val email = account?.email
                    if (!email.isNullOrBlank()) {
                        currentUserEmail = email
                        verifiedAdmin = email.equals(MELOWARE_ADMIN_EMAIL, ignoreCase = true)
                        prefs.edit().putString("logged_in_email", email).apply()
                        googleSignInSuccessSerial++
                        Toast.makeText(context, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                    }
                } catch (error: com.google.android.gms.common.api.ApiException) {
                    pendingPersonalDriveAction = null
                    pendingOpenLinkDriveUpload = false
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.consumePersonalDriveAuthorizationRequest()
                    controller.cancelPendingPublicDriveBackupAuthorization()
                    controller.cancelPendingMediaFireDriveBackupAuthorization()
                    Toast.makeText(
                        context,
                        "Lỗi Google Sign-In (${error.statusCode}): ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (error: Exception) {
                    pendingPersonalDriveAction = null
                    pendingOpenLinkDriveUpload = false
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.consumePersonalDriveAuthorizationRequest()
                    controller.cancelPendingPublicDriveBackupAuthorization()
                    controller.cancelPendingMediaFireDriveBackupAuthorization()
                    Toast.makeText(
                        context,
                        "Không thể đăng nhập: ${error.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            fun pickerFileIds(
                result: com.google.android.gms.auth.api.identity.AuthorizationResult
            ): List<String> {
                val raw = result.tokenResponseParams
                    ?.get(GOOGLE_PICKER_FILE_IDS_PARAM)
                    ?.toString()
                    .orEmpty()
                return raw.split(',')
                    .map { it.trim() }
                    .filter { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
                    .distinct()
            }

            fun acceptPersonalDriveReadOnlyAuthorization(
                result: com.google.android.gms.auth.api.identity.AuthorizationResult
            ) {
                val token = result.accessToken
                val granted = result.grantedScopes.orEmpty()
                if (GOOGLE_DRIVE_READONLY_SCOPE !in granted || token.isNullOrBlank()) {
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        "Bạn chưa cấp quyền drive.readonly cho MeloWave.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val authorizedEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                authorizedEmail?.let { email ->
                    prefs.edit().putBoolean(
                        personalDriveExplanationKey(PersonalDriveExplanationMode.READ_ONLY, email),
                        true
                    ).apply()
                }
                controller.setPersonalDriveReadOnlyAuthorization(token, authorizedEmail)
                if (openPersonalDriveBrowserAfterReadOnlyConsent) {
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.libraryTab = LibraryTab.FOLDERS
                    controller.openLibrary(LibrarySource.PERSONAL_DRIVE)
                    controller.showPersonalDriveDialog = true
                    controller.openPersonalDriveRoot()
                }
            }

            val personalDriveReadOnlyAuthorizationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { activityResult ->
                if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        "Bạn đã hủy cấp quyền chỉ đọc Google Drive.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@rememberLauncherForActivityResult
                }
                try {
                    val result = authorizationClient.getAuthorizationResultFromIntent(
                        activityResult.data!!
                    )
                    acceptPersonalDriveReadOnlyAuthorization(result)
                } catch (error: com.google.android.gms.common.api.ApiException) {
                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        "Không nhận được quyền drive.readonly (${error.statusCode}).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            fun acceptPersonalDriveAddFileAuthorization(
                result: com.google.android.gms.auth.api.identity.AuthorizationResult,
                fromPicker: Boolean
            ) {
                val token = result.accessToken
                val granted = result.grantedScopes.orEmpty()
                if (GOOGLE_DRIVE_FILE_SCOPE !in granted || token.isNullOrBlank()) {
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        "Bạn chưa cấp quyền drive.file để thêm file bằng Google Picker.",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val authorizedEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                authorizedEmail?.let { email ->
                    prefs.edit().putBoolean(
                        personalDriveExplanationKey(PersonalDriveExplanationMode.ADD_FILE, email),
                        true
                    ).apply()
                }
                controller.setPersonalDriveAddFileAuthorization(token, authorizedEmail)
                if (!fromPicker) return

                val pickedIds = pickerFileIds(result)
                if (pickedIds.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Bạn chưa chọn file hoặc thư mục Google Drive nào.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                controller.libraryTab = LibraryTab.SONGS
                controller.openLibrary(LibrarySource.PERSONAL_DRIVE)
                controller.importPersonalDrivePickerSelection(
                    accessToken = token,
                    accountEmail = currentUserEmail,
                    pickedIds = pickedIds
                )
            }

            val personalDriveAddFileAuthorizationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { activityResult ->
                val fromPicker = awaitingPersonalDrivePickerResult
                awaitingPersonalDrivePickerResult = false
                if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        if (fromPicker) {
                            "Bạn đã đóng Google Picker."
                        } else {
                            "Bạn đã hủy cấp quyền drive.file."
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    return@rememberLauncherForActivityResult
                }
                try {
                    val result = authorizationClient.getAuthorizationResultFromIntent(
                        activityResult.data!!
                    )
                    acceptPersonalDriveAddFileAuthorization(result, fromPicker)
                } catch (error: com.google.android.gms.common.api.ApiException) {
                    controller.consumePersonalDriveAuthorizationRequest()
                    Toast.makeText(
                        context,
                        "Không nhận được quyền drive.file (${error.statusCode}).",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Link mode uploads request drive.file only when the user presses Save.
            // Its token remains independent from both personal Drive access modes.
            fun cancelLinkDriveAuthorization(message: String) {
                pendingOpenLinkDriveUpload = false
                controller.consumeLinkDriveUploadAuthorizationRequest()
                controller.cancelPendingPublicDriveBackupAuthorization()
                controller.cancelPendingMediaFireDriveBackupAuthorization()
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }

            fun acceptLinkDriveAuthorization(
                result: com.google.android.gms.auth.api.identity.AuthorizationResult
            ) {
                val token = result.accessToken
                if (token.isNullOrBlank() ||
                    GOOGLE_DRIVE_FILE_SCOPE !in result.grantedScopes.orEmpty()
                ) {
                    cancelLinkDriveAuthorization(
                        "Chưa cấp quyền drive.file nên MeloWave không thể tải file lên Drive."
                    )
                    return
                }
                val email = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                controller.setLinkDriveUploadAuthorization(token, email)
            }

            val linkDriveAuthorizationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartIntentSenderForResult()
            ) { activityResult ->
                if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
                    cancelLinkDriveAuthorization("Bạn đã hủy cấp quyền drive.file để lưu file từ liên kết.")
                } else {
                    try {
                        val result = authorizationClient.getAuthorizationResultFromIntent(
                            activityResult.data!!
                        )
                        acceptLinkDriveAuthorization(result)
                    } catch (error: Exception) {
                        cancelLinkDriveAuthorization(
                            "Không thể nhận quyền drive.file: ${error.localizedMessage ?: "Lỗi Google"}"
                        )
                    }
                }
            }

            fun requestLinkDriveAuthorization() {
                val signedIn = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)
                val account = signedIn?.account
                if (account == null) {
                    pendingOpenLinkDriveUpload = true
                    googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    return
                }
                controller.syncLinkDriveUploadAccount(signedIn.email)
                val request = com.google.android.gms.auth.api.identity.AuthorizationRequest
                    .builder()
                    .setAccount(account)
                    .setRequestedScopes(listOf(linkUploadScope))
                    .setOptOutIncludingGrantedScopes(true)
                    .setPrompt(
                        com.google.android.gms.auth.api.identity.AuthorizationRequest.Prompt.NOT_SET
                    )
                    .build()

                fun authorizeNow() {
                    authorizationClient.authorize(request)
                        .addOnSuccessListener { result ->
                            if (result.hasResolution()) {
                                val pendingIntent = result.pendingIntent
                                if (pendingIntent != null) {
                                    linkDriveAuthorizationLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                    )
                                } else {
                                    cancelLinkDriveAuthorization(
                                        "Google không tạo được cửa sổ cấp quyền drive.file."
                                    )
                                }
                            } else {
                                acceptLinkDriveAuthorization(result)
                            }
                        }
                        .addOnFailureListener { error ->
                            cancelLinkDriveAuthorization(
                                "Không thể xin quyền drive.file: ${error.localizedMessage ?: "Lỗi Google"}"
                            )
                        }
                }

                val invalidToken = controller.consumeLinkDriveUploadInvalidToken()
                if (invalidToken.isNullOrBlank()) {
                    authorizeNow()
                } else {
                    authorizationClient.clearToken(
                        com.google.android.gms.auth.api.identity.ClearTokenRequest
                            .builder().setToken(invalidToken).build()
                    ).addOnCompleteListener { authorizeNow() }
                }
            }

            fun requestPersonalDriveReadOnlyAuthorization(openBrowser: Boolean) {
                openPersonalDriveBrowserAfterReadOnlyConsent = openBrowser
                val signedIn = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)
                val account = signedIn?.account
                if (account == null) {
                    pendingPersonalDriveAction = if (openBrowser) {
                        PendingPersonalDriveAction.READ_ONLY_BROWSER
                    } else {
                        PendingPersonalDriveAction.READ_ONLY_REFRESH
                    }
                    googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    return
                }

                val request = com.google.android.gms.auth.api.identity.AuthorizationRequest
                    .builder()
                    .setAccount(account)
                    .setRequestedScopes(listOf(personalDriveReadOnlyScope))
                    .setOptOutIncludingGrantedScopes(true)
                    .setPrompt(
                        com.google.android.gms.auth.api.identity.AuthorizationRequest.Prompt.NOT_SET
                    )
                    .build()

                fun authorizeNow() {
                    authorizationClient.authorize(request)
                        .addOnSuccessListener { result ->
                            if (result.hasResolution()) {
                                val pendingIntent = result.pendingIntent
                                if (pendingIntent == null) {
                                    openPersonalDriveBrowserAfterReadOnlyConsent = false
                                    controller.consumePersonalDriveAuthorizationRequest()
                                    Toast.makeText(
                                        context,
                                        "Google không tạo được cửa sổ cấp quyền drive.readonly.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    personalDriveReadOnlyAuthorizationLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                    )
                                }
                            } else {
                                acceptPersonalDriveReadOnlyAuthorization(result)
                            }
                        }
                        .addOnFailureListener { error ->
                            openPersonalDriveBrowserAfterReadOnlyConsent = false
                            controller.consumePersonalDriveAuthorizationRequest()
                            Toast.makeText(
                                context,
                                "Không thể xác thực drive.readonly: ${error.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                val rejectedToken = controller.consumePersonalDriveInvalidToken(
                    PersonalDriveAccessMode.READ_ONLY
                )
                if (rejectedToken.isNullOrBlank()) {
                    authorizeNow()
                } else {
                    authorizationClient.clearToken(
                        com.google.android.gms.auth.api.identity.ClearTokenRequest
                            .builder()
                            .setToken(rejectedToken)
                            .build()
                    ).addOnCompleteListener { authorizeNow() }
                }
            }

            fun requestPersonalDriveAddFileAuthorization(openPicker: Boolean) {
                val signedIn = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)
                val account = signedIn?.account
                if (account == null) {
                    pendingPersonalDriveAction = if (openPicker) {
                        PendingPersonalDriveAction.ADD_FILE_PICKER
                    } else {
                        PendingPersonalDriveAction.ADD_FILE_REFRESH
                    }
                    googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    return
                }

                val requestBuilder = com.google.android.gms.auth.api.identity.AuthorizationRequest
                    .builder()
                    .setAccount(account)
                    .setRequestedScopes(listOf(personalDriveAddFileScope))
                    .setOptOutIncludingGrantedScopes(true)
                    .setPrompt(
                        if (openPicker) {
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.Prompt.CONSENT
                        } else {
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.Prompt.NOT_SET
                        }
                    )

                if (openPicker) {
                    requestBuilder
                        .addResourceParameter(
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER,
                            "true"
                        )
                        .addResourceParameter(
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.ResourceParameter.PICKER_ALLOW_MULTIPLE,
                            "true"
                        )
                        .addResourceParameter(
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.ResourceParameter.PICKER_ALLOW_FOLDER_SELECTION,
                            "true"
                        )
                        .addResourceParameter(
                            com.google.android.gms.auth.api.identity.AuthorizationRequest.ResourceParameter.PICKER_MIMETYPES,
                            GOOGLE_PICKER_AUDIO_MIME_TYPES
                        )
                }
                val request = requestBuilder.build()

                fun authorizeNow() {
                    authorizationClient.authorize(request)
                        .addOnSuccessListener { result ->
                            if (result.hasResolution()) {
                                val pendingIntent = result.pendingIntent
                                if (pendingIntent == null) {
                                    controller.consumePersonalDriveAuthorizationRequest()
                                    Toast.makeText(
                                        context,
                                        "Google không tạo được cửa sổ Google Picker/drive.file.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    awaitingPersonalDrivePickerResult = openPicker
                                    personalDriveAddFileAuthorizationLauncher.launch(
                                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                    )
                                }
                            } else {
                                acceptPersonalDriveAddFileAuthorization(result, openPicker)
                            }
                        }
                        .addOnFailureListener { error ->
                            controller.consumePersonalDriveAuthorizationRequest()
                            Toast.makeText(
                                context,
                                "Không thể xác thực drive.file: ${error.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                val rejectedToken = controller.consumePersonalDriveInvalidToken(
                    PersonalDriveAccessMode.ADD_FILE
                )
                if (rejectedToken.isNullOrBlank()) {
                    authorizeNow()
                } else {
                    authorizationClient.clearToken(
                        com.google.android.gms.auth.api.identity.ClearTokenRequest
                            .builder()
                            .setToken(rejectedToken)
                            .build()
                    ).addOnCompleteListener { authorizeNow() }
                }
            }

            val audioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    controller.scanLocalMusic()
                } else {
                    Toast.makeText(
                        context,
                        "Chưa cấp quyền đọc nhạc. Bạn vẫn có thể dùng Google Drive.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) scheduleDailyReminder(context)
            }

            val requestLocalScan: () -> Unit = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                ) {
                    controller.scanLocalMusic()
                } else {
                    audioPermissionLauncher.launch(permission)
                }
            }

            // Both Home and Library use the same first-time explanation and consent flow.
            // Subsequent uses rely on Google's persisted grant: a missing/expired RAM token
            // is obtained silently when possible (NOT_SET), never stored in preferences.
            fun openAuthorizedPersonalDriveBrowser() {
                val signedInEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                val sameAccount = !signedInEmail.isNullOrBlank() &&
                    controller.personalDriveAccountEmail.equals(signedInEmail, ignoreCase = true)
                if (sameAccount && controller.hasPersonalDriveReadOnlyAuthorization) {
                    controller.libraryTab = LibraryTab.FOLDERS
                    controller.openLibrary(LibrarySource.PERSONAL_DRIVE)
                    controller.showPersonalDriveDialog = true
                    if (controller.personalDrivePath.isEmpty()) controller.openPersonalDriveRoot()
                } else {
                    // After a process restart the access token is RAM-only, but Google can
                    // return a fresh token without a new consent screen if still authorized.
                    requestPersonalDriveReadOnlyAuthorization(openBrowser = true)
                }
            }

            fun openPersonalDriveBrowserFromUser() {
                if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                    showPremiumDialog = true
                    return
                }
                val signedInEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                val acknowledged = !signedInEmail.isNullOrBlank() && prefs.getBoolean(
                    personalDriveExplanationKey(PersonalDriveExplanationMode.READ_ONLY, signedInEmail),
                    false
                )
                if (!acknowledged) {
                    personalDriveExplanation = PersonalDriveExplanationMode.READ_ONLY
                    return
                }
                openAuthorizedPersonalDriveBrowser()
            }

            fun openPersonalDrivePickerFromUser() {
                if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                    showPremiumDialog = true
                    return
                }
                val signedInEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.email
                val acknowledged = !signedInEmail.isNullOrBlank() && prefs.getBoolean(
                    personalDriveExplanationKey(PersonalDriveExplanationMode.ADD_FILE, signedInEmail),
                    false
                )
                if (!acknowledged) {
                    personalDriveExplanation = PersonalDriveExplanationMode.ADD_FILE
                    return
                }
                // Google Picker is intentionally shown for each new file selection.
                requestPersonalDriveAddFileAuthorization(openPicker = true)
            }

            // Sau khi Google Sign-In hoàn tất, tiếp tục đúng chế độ Drive người dùng đã chọn.
            LaunchedEffect(currentUserEmail, pendingPersonalDriveAction, googleSignInSuccessSerial) {
                val action = pendingPersonalDriveAction ?: return@LaunchedEffect
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getLastSignedInAccount(context)?.account
                    ?: return@LaunchedEffect
                pendingPersonalDriveAction = null
                if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                    showPremiumDialog = true
                } else {
                    when (action) {
                        PendingPersonalDriveAction.READ_ONLY_BROWSER ->
                            requestPersonalDriveReadOnlyAuthorization(openBrowser = true)
                        PendingPersonalDriveAction.READ_ONLY_REFRESH ->
                            requestPersonalDriveReadOnlyAuthorization(openBrowser = false)
                        PendingPersonalDriveAction.ADD_FILE_PICKER ->
                            requestPersonalDriveAddFileAuthorization(openPicker = true)
                        PendingPersonalDriveAction.ADD_FILE_REFRESH ->
                            requestPersonalDriveAddFileAuthorization(openPicker = false)
                    }
                }
            }

            // Access tokens chỉ giữ trong RAM. Hết token nào thì chỉ xin lại đúng scope đó.
            LaunchedEffect(controller.personalDriveAuthorizationRequest) {
                val mode = controller.personalDriveAuthorizationRequest
                    ?: return@LaunchedEffect
                controller.consumePersonalDriveAuthorizationRequest()
                if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                    showPremiumDialog = true
                } else {
                    when (mode) {
                        PersonalDriveAccessMode.READ_ONLY ->
                            requestPersonalDriveReadOnlyAuthorization(openBrowser = false)
                        PersonalDriveAccessMode.ADD_FILE ->
                            requestPersonalDriveAddFileAuthorization(openPicker = false)
                    }
                }
            }

            LaunchedEffect(currentUserEmail, pendingOpenLinkDriveUpload, googleSignInSuccessSerial) {
                if (pendingOpenLinkDriveUpload &&
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getLastSignedInAccount(context)?.account != null
                ) {
                    pendingOpenLinkDriveUpload = false
                    if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                        controller.cancelPendingPublicDriveBackupAuthorization()
                        controller.cancelPendingMediaFireDriveBackupAuthorization()
                        showPremiumDialog = true
                    } else {
                        requestLinkDriveAuthorization()
                    }
                }
            }

            LaunchedEffect(controller.linkDriveNeedsAuthorization) {
                if (controller.linkDriveNeedsAuthorization) {
                    controller.consumeLinkDriveUploadAuthorizationRequest()
                    if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                        controller.cancelPendingPublicDriveBackupAuthorization()
                        controller.cancelPendingMediaFireDriveBackupAuthorization()
                        showPremiumDialog = true
                    } else {
                        requestLinkDriveAuthorization()
                    }
                }
            }

            LaunchedEffect(Unit) {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                ) {
                    controller.scanLocalMusic()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        scheduleDailyReminder(context)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    scheduleDailyReminder(context)
                }
            }

            LaunchedEffect(controller.errorMessage) {
                controller.errorMessage?.let { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    controller.dismissError()
                }
            }

            LaunchedEffect(controller.statusMessage) {
                controller.statusMessage?.let { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    controller.dismissStatus()
                }
            }

            DisposableEffect(controller) {
                MeloWavePlaybackRuntime.attachActivity()
                onDispose { MeloWavePlaybackRuntime.detachActivity() }
            }

            UserUsageTracker(currentUserEmail = currentUserEmail)

            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            LaunchedEffect(isDark, showLaunchSplash) {
                // Variant 1 uses a dark launch background even if the saved theme
                // is light; restore the saved theme's system bars afterwards.
                systemBarsDark = showLaunchSplash || isDark
                configureSystemBars()
            }

            TunertoolsTheme(isDark = isDark, language = language, accentId = accentId, accentHue = customAccentHue) {
                val signedOutNotice = mwString("settings.signed_out", "Đã đăng xuất tài khoản")
                val onGoogleAccountAction: () -> Unit = {
                    showSettings = false
                    if (currentUserEmail != null ||
                        com.google.android.gms.auth.api.signin.GoogleSignIn
                            .getLastSignedInAccount(context) != null
                    ) {
                        controller.clearPersonalDriveSession(
                            removeSongs = true,
                            forgetAccount = true
                        )
                        googleSignInClient.signOut().addOnCompleteListener { result ->
                            if (result.isSuccessful) {
                                currentUserEmail = null
                                verifiedAdmin = false
                                showAdminDashboard = false
                                prefs.edit().remove("logged_in_email").apply()
                                MeloWarePremiumAccess.clearAccount()
                                Toast.makeText(context, signedOutNotice, Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Không thể đăng xuất Google. Vui lòng thử lại.",
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        googleAuthLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    LocalMusicColors.current.background,
                                    LocalMusicColors.current.backgroundSoft,
                                    LocalMusicColors.current.background
                                )
                            )
                        )
                ) {
                    when (controller.currentScreen) {
                        MusicScreen.HOME -> HomeScreen(
                            controller = controller,
                            // Only display a genuine signed-in Google profile (not cached email).
                            googleAccountEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)?.email,
                            googleAccountName = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)?.displayName,
                            googleProfilePhotoUrl = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)?.photoUrl?.toString(),
                            onGoogleLogin = { googleAuthLauncher.launch(googleSignInClient.signInIntent) },
                            onGoogleLogout = onGoogleAccountAction,
                            onScanLocal = requestLocalScan,
                            onOpenLocalFolders = {
                                controller.libraryTab = LibraryTab.FOLDERS
                                controller.openLibrary(LibrarySource.LOCAL)
                                requestLocalScan()
                            },
                            onBrowsePersonalDriveReadOnly = { openPersonalDriveBrowserFromUser() },
                            onAddPersonalDriveFiles = { openPersonalDrivePickerFromUser() },
                            onOpenPublicDrive = {
                                if (MeloWarePremiumAccess.googleDriveRequiresPremium) {
                                    showPremiumDialog = true
                                } else {
                                    controller.libraryTab = LibraryTab.FOLDERS
                                    controller.openLibrary(LibrarySource.PUBLIC_DRIVE)
                                    controller.showPublicDriveDialog = true
                                }
                            },
                            onOpenPremium = { showPremiumDialog = true },
                            onSettings = { showSettings = true }
                        )

                        MusicScreen.LIBRARY -> LibraryScreen(
                            controller = controller,
                            onScanLocal = requestLocalScan,
                            onBrowsePersonalDriveReadOnly = { openPersonalDriveBrowserFromUser() },
                            onAddPersonalDriveFiles = { openPersonalDrivePickerFromUser() },
                            onSettings = { showSettings = true },
                            onOpenPremium = { showPremiumDialog = true }
                        )

                        MusicScreen.NOW_PLAYING -> Column(modifier = Modifier.fillMaxSize()) {
                            // Player takes the available height; its own layout has no banner.
                            Box(modifier = Modifier.weight(1f)) {
                                NowPlayingScreen(controller, onOpenPremium = { showPremiumDialog = true })
                            }
                            // Exactly one banner, exclusively in the full player screen.
                            if (controller.currentSong != null && MeloWarePremiumAccess.shouldShowAds) {
                                MeloWareNowPlayingBanner()
                            }
                        }
                    }
                    // Overlay the existing player: no re-creation or second startup.
                    if (showLaunchSplash) {
                        MeloWaveLaunchSplash(onFinished = { showLaunchSplash = false })
                    }
                }

                if (showSettings) {
                    QuickSettingsDialog(
                        controller = controller,
                        language = language,
                        languageOptions = languageOptions,
                        followDeviceLanguage = followDeviceLanguage,
                        themeMode = themeMode,
                        accentId = accentId,
                        onOpenLanguage = {
                            showSettings = false
                            showLanguageDialog = true
                        },
                        onOpenPremium = {
                            showSettings = false
                            showPremiumDialog = true
                        },
                        onOpenTheme = {
                            showSettings = false
                            showThemeDialog = true
                        },
                        onOpenAccent = {
                            showSettings = false
                            showAccentDialog = true
                        },
                        currentUserEmail = currentUserEmail,
                        isAdmin = verifiedAdmin,
                        onOpenAdmin = {
                            showSettings = false
                            showAdminDashboard = true
                        },
                        onDisconnectDrive = {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)?.account
                            val emailToForget = account?.name ?: controller.personalDriveAccountEmail
                            fun forgetLocalExplanation() {
                                if (!emailToForget.isNullOrBlank()) {
                                    prefs.edit()
                                        .remove(personalDriveExplanationKey(PersonalDriveExplanationMode.READ_ONLY, emailToForget))
                                        .remove(personalDriveExplanationKey(PersonalDriveExplanationMode.ADD_FILE, emailToForget))
                                        .apply()
                                }
                            }
                            if (account == null) {
                                forgetLocalExplanation()
                                controller.disconnectPersonalDrive()
                                showSettings = false
                            } else {
                                val revokeRequest = com.google.android.gms.auth.api.identity.RevokeAccessRequest
                                    .builder()
                                    .setAccount(account)
                                    .setScopes(
                                        listOf(
                                            personalDriveReadOnlyScope,
                                            personalDriveAddFileScope
                                        )
                                    )
                                    .build()
                                authorizationClient.revokeAccess(revokeRequest)
                                    .addOnCompleteListener {
                                        forgetLocalExplanation()
                                        controller.disconnectPersonalDrive()
                                        showSettings = false
                                    }
                            }
                        },
                        onLoginLogout = onGoogleAccountAction,
                        onDismiss = { showSettings = false }
                    )
                }

                personalDriveExplanation?.let { mode ->
                    PersonalDriveAccessExplanationDialog(
                        mode = mode,
                        accountEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
                            .getLastSignedInAccount(context)?.email,
                        onDismiss = { personalDriveExplanation = null },
                        onConfirm = {
                            personalDriveExplanation = null
                            when (mode) {
                                PersonalDriveExplanationMode.READ_ONLY ->
                                    openAuthorizedPersonalDriveBrowser()
                                PersonalDriveExplanationMode.ADD_FILE ->
                                    requestPersonalDriveAddFileAuthorization(openPicker = true)
                            }
                        }
                    )
                }

                if (showPremiumDialog) {
                    MeloWarePremiumDialog(
                        activity = this@MainActivity,
                        billing = premiumBilling,
                        onRequestGoogleLogin = { googleAuthLauncher.launch(googleSignInClient.signInIntent) },
                        onDismiss = { showPremiumDialog = false }
                    )
                }

                if (showAdminDashboard && verifiedAdmin) {
                    MeloWareAdminUsageDialog(onDismiss = { showAdminDashboard = false })
                }

                if ((showLanguageDialog || requireInitialLanguageChoice) && !showLaunchSplash) {
                    LanguageDialog(
                        selectedCode = language,
                        options = languageOptions,
                        requiredSelection = requireInitialLanguageChoice,
                        autoSelected = followDeviceLanguage,
                        deviceLanguageCode = deviceLanguage,
                        onSelectAuto = {
                            // Re-enable following the device; do not pin a language code.
                            if (deviceLanguage != null) {
                                languageMode = MELOWARE_LANGUAGE_MODE_DEVICE
                                prefs.edit()
                                    .putString(MELOWARE_LANGUAGE_MODE_KEY, MELOWARE_LANGUAGE_MODE_DEVICE)
                                    .remove("app_language")
                                    .apply()
                                showLanguageDialog = false
                            }
                        },
                        onSelect = { code ->
                            // A user's manual choice always takes precedence on restart.
                            manualLanguage = code
                            languageMode = MELOWARE_LANGUAGE_MODE_MANUAL
                            prefs.edit()
                                .putString(MELOWARE_LANGUAGE_MODE_KEY, MELOWARE_LANGUAGE_MODE_MANUAL)
                                .putString("app_language", code)
                                .apply()
                            showLanguageDialog = false
                        },
                        onDismiss = {
                            if (!requireInitialLanguageChoice) showLanguageDialog = false
                        }
                    )
                }

                if (showThemeDialog) {
                    ThemeModeDialog(
                        selectedMode = themeMode,
                        onSelect = { mode ->
                            themeMode = mode
                            prefs.edit().putInt("app_theme_mode", mode).apply()
                            showThemeDialog = false
                        },
                        onDismiss = { showThemeDialog = false }
                    )
                }
                if (showAccentDialog) {
                    AccentColorDialog(
                        selectedId = accentId,
                        selectedHue = customAccentHue,
                        onSelect = { id ->
                            // Show the new preset immediately. The dialog stays open
                            // so the user can inspect the preview before closing it.
                            accentId = id
                            prefs.edit().putString("app_accent_color", id).apply()
                        },
                        onCustomHueChange = { hue ->
                            // Compose updates UI in real time; avoid a disk write on
                            // every gesture frame. Save once when sliding finishes.
                            customAccentHue = hue.coerceIn(0f, 360f)
                            accentId = MELOWARE_CUSTOM_ACCENT_ID
                        },
                        onCustomHueChangeFinished = {
                            if (accentId == MELOWARE_CUSTOM_ACCENT_ID) {
                                prefs.edit()
                                    .putString("app_accent_color", MELOWARE_CUSTOM_ACCENT_ID)
                                    .putFloat(MELOWARE_ACCENT_HUE_KEY, customAccentHue)
                                    .apply()
                            }
                        },
                        onDismiss = {
                            if (accentId == MELOWARE_CUSTOM_ACCENT_ID) {
                                prefs.edit()
                                    .putString("app_accent_color", MELOWARE_CUSTOM_ACCENT_ID)
                                    .putFloat(MELOWARE_ACCENT_HUE_KEY, customAccentHue)
                                    .apply()
                            }
                            showAccentDialog = false
                        }
                    )
                }

            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) configureSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun configureSystemBars() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = if (systemBarsDark) {
            android.graphics.Color.rgb(2, 8, 18)
        } else {
            android.graphics.Color.rgb(244, 247, 252)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !systemBarsDark
            isAppearanceLightNavigationBars = !systemBarsDark
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}

// This is the only AdMob view in MeloWare. Never create it in Home or Library.
private const val MELOWARE_BANNER_AD_UNIT_ID =
    "ca-app-pub-3940256099942544/6300978111"

@Composable
private fun MeloWareNowPlayingBanner() {
    val context = LocalContext.current
    val adView = remember(context) {
        com.google.android.gms.ads.AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = MELOWARE_BANNER_AD_UNIT_ID
            loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
        }
    }

    // Disposes the banner as soon as the user returns to Home or Library.
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF020812))
            .navigationBarsPadding()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .width(320.dp)
                .height(50.dp)
        )
    }
}

@Composable
private fun UserUsageTracker(currentUserEmail: String?) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var appInForeground by remember { mutableStateOf(true) }
    var sessionCounted by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            appInForeground = when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START,
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> true
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> false
                else -> appInForeground
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentUserEmail) {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "Unknown"
        val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE}"
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val dateText = dateFormatter.format(java.util.Date())
        val accessTime = timeFormatter.format(java.util.Date())
        val userNodeKey = androidId.replace(Regex("[^a-zA-Z0-9_]"), "_")

        try {
            val reference = com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).getReference("usage_stats").child(dateText).child(userNodeKey)

            var initialDurationMs = 0L
            var openCount = 1L
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                reference.get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            initialDurationMs = (snapshot.child("durationMs").value as? Number)?.toLong() ?: 0L
                            val oldCount = (snapshot.child("openCount").value as? Number)?.toLong() ?: 0L
                            openCount = if (sessionCounted) oldCount else oldCount + 1L
                        } else {
                            openCount = 1L
                        }
                        sessionCounted = true
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
            }

            var currentSessionMs = 0L
            var lastTick = System.currentTimeMillis()
            while (true) {
                if (appInForeground) {
                    val now = System.currentTimeMillis()
                    currentSessionMs += now - lastTick
                    lastTick = now
                    val totalDurationMs = initialDurationMs + currentSessionMs
                    val totalSeconds = totalDurationMs / 1000L
                    val durationText = if (totalSeconds >= 60L) {
                        "${totalSeconds / 60L}m ${totalSeconds % 60L}s"
                    } else {
                        "${totalSeconds}s"
                    }
                    val activeUser = currentUserEmail ?: "Khách (${androidId.take(6).uppercase()})"
                    reference.setValue(
                        mapOf(
                            "userId" to activeUser,
                            "deviceModel" to deviceModel,
                            "osVersion" to osVersion,
                            "deviceId" to androidId,
                            "accessTime" to accessTime,
                            "openCount" to openCount,
                            "durationString" to durationText,
                            "durationMs" to totalDurationMs
                        )
                    )
                } else {
                    lastTick = System.currentTimeMillis()
                }
                kotlinx.coroutines.delay(5_000L)
            }
        } catch (error: Exception) {
            android.util.Log.w("MeloWareUsage", "Không thể cập nhật usage_stats", error)
        }
    }
}

// Local guidance only. This never replaces Google's OAuth consent screen.
@Composable
private fun PersonalDriveAccessExplanationDialog(
    mode: PersonalDriveExplanationMode,
    accountEmail: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalMusicColors.current
    val readOnly = mode == PersonalDriveExplanationMode.READ_ONLY
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = colors.purpleBright,
                        modifier = Modifier.size(27.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (readOnly) "Duyệt Google Drive chỉ đọc" else "Thêm nhạc bằng Google Picker",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                if (!accountEmail.isNullOrBlank()) {
                    Text("Tài khoản: $accountEmail", color = colors.textSecondary, fontSize = 12.sp)
                }
                Text(
                    text = if (readOnly) {
                        "1. Nhấn OK để mở bước cấp quyền của Google.\n" +
                        "2. Cho phép MeloWave xem tệp trong Google Drive.\n" +
                        "3. MeloWave mở thư mục ngay; lần sau vào thẳng khi quyền còn hiệu lực."
                    } else {
                        "1. Nhấn OK để mở Google Picker.\n" +
                        "2. Chọn những tệp hoặc thư mục nhạc bạn muốn thêm.\n" +
                        "3. MeloWave đưa các bài đã chọn vào thư viện."
                    },
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    lineHeight = 21.sp
                )
                Text(
                    text = if (readOnly) {
                        "Quyền chỉ đọc không cho phép MeloWave sửa, xóa hay tải file lên Drive. " +
                        "Tuy nhiên, ứng dụng có thể xem và tải các tệp cá nhân trong Drive; " +
                        "không yêu cầu quyền đọc Gmail hoặc danh bạ."
                    } else {
                        "Bạn chủ động chọn nội dung được chia sẻ với MeloWave. " +
                        "Quyền drive.file cho phép truy cập và quản lý tệp được chọn; " +
                        "bước này chỉ thêm nhạc vào thư viện, không tự động xóa hay sửa tệp Drive."
                    },
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = "Google có thể hiển thị cảnh báo ứng dụng chưa xác minh cho đến khi quá trình kiểm duyệt hoàn tất. " +
                        "Bạn có thể từ chối hoặc thu hồi quyền trong Tài khoản Google.",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Hủy", color = colors.textSecondary)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1.4f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Text("OK · Tiếp tục", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsDialog(
    controller: MusicController,
    language: String,
    languageOptions: List<MeloWareLanguageOption>,
    followDeviceLanguage: Boolean,
    themeMode: Int,
    accentId: String,
    onOpenLanguage: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAccent: () -> Unit,
    currentUserEmail: String?,
    isAdmin: Boolean,
    onOpenAdmin: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onLoginLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    // Android generates these installed-package values from versionName and
    // versionCode in app/build.gradle.kts at build time; no hard-coded version.
    val installedVersion = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            packageInfo.versionName.orEmpty().ifBlank { "?" } to code
        }.getOrNull()
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mwString("settings.title", "Cài đặt"),
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }

                SettingRow(
                    icon = Icons.Rounded.WorkspacePremium,
                    title = mwString("premium.menu.title", "Kích hoạt MeloWare Premium"),
                    subtitle = mwString("premium.menu.subtitle", "Gói 6 tháng / 12 tháng · Dùng thử 30 ngày"),
                    onClick = onOpenPremium
                )

                SettingRow(
                    icon = Icons.Rounded.Language,
                    title = mwString("settings.language", "Ngôn ngữ"),
                    subtitle = if (followDeviceLanguage) {
                        mwString("language.follow_device", "Theo ngôn ngữ điện thoại") + " · " +
                                (languageOptions.firstOrNull { it.code == language }?.nativeName ?: language)
                    } else {
                        languageOptions.firstOrNull { it.code == language }?.nativeName ?: language
                    },
                    onClick = onOpenLanguage
                )

                SettingRow(
                    icon = Icons.Rounded.SettingsBrightness,
                    title = mwString("settings.theme", "Chế độ giao diện"),
                    subtitle = when (themeMode) {
                        1 -> mwString("theme.light", "Sáng")
                        2 -> mwString("theme.dark", "Tối")
                        else -> mwString("theme.system", "Theo hệ thống")
                    },
                    onClick = onOpenTheme
                )

                SettingRow(
                    icon = Icons.Rounded.Palette,
                    title = mwString("settings.accent", "Màu giao diện"),
                    subtitle = mwString(
                        "accent.$accentId",
                        if (accentId == MELOWARE_CUSTOM_ACCENT_ID) mwString("accent.custom", "Tùy chỉnh")
                        else MusicAccentOptions.firstOrNull { it.id == accentId }?.title ?: "Tím"
                    ),
                    onClick = onOpenAccent
                )

                if (
                    controller.personalDriveAccountEmail != null ||
                    controller.hasPersonalDriveAuthorization ||
                    controller.personalDriveSongs.isNotEmpty()
                ) {
                    SettingRow(
                        icon = Icons.Rounded.CloudOff,
                        title = mwString("settings.disconnect_drive", "Ngắt quyền Google Drive"),
                        subtitle =
                            "Ngắt drive.readonly và drive.file, không xóa file thật trên Drive",
                        onClick = onDisconnectDrive
                    )
                }

                SettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = mwString("settings.clear_drive_cache", "Xóa cache Google Drive"),
                    subtitle = mwString("settings.clear_drive_cache_desc", "Giải phóng file tạm, không xóa nhạc gốc"),
                    onClick = {
                        controller.clearDriveCache()
                        onDismiss()
                    }
                )

                // Display the gold Premium crown only for an entitlement validated by the server.
                // Trial users are explicitly labeled to avoid presenting a trial as a purchase.
                val accountPremiumBadge = when {
                    !MeloWarePremiumAccess.canUsePaidFeatures -> null
                    MeloWarePremiumAccess.accessKind == "paid" -> "PREMIUM"
                    MeloWarePremiumAccess.accessKind == "trial" -> "PREMIUM TRIAL"
                    else -> null
                }
                SettingRow(
                    icon = Icons.Rounded.AccountCircle,
                    premiumBadge = accountPremiumBadge,
                    title = if (currentUserEmail.isNullOrBlank()) {
                        mwString("settings.google_login", "Đăng nhập Google")
                    } else {
                        mwString("settings.google_logout", "Đăng xuất")
                    },
                    subtitle = when {
                        isAdmin -> "${currentUserEmail.orEmpty()} · " + mwString(
                            "settings.admin_hint", "Chạm biểu tượng để xem thống kê"
                        )
                        currentUserEmail.isNullOrBlank() && MeloWarePremiumAccess.accessKind == "trial" ->
                            mwString("settings.guest_trial_desc", "Đang dùng thử Premium · Không cần đăng nhập Google")
                        currentUserEmail.isNullOrBlank() -> mwString(
                            "settings.google_identity_desc", "Định danh tài khoản trong thống kê sử dụng"
                        )
                        else -> currentUserEmail.orEmpty()
                    },
                    onClick = onLoginLogout,
                    onIconClick = if (isAdmin) onOpenAdmin else null
                )

                // Minimal footer: versionName is read from the installed APK
                // (generated from app/build.gradle.kts), never hard-coded in the UI.
                Text(
                    text = "version:${installedVersion?.first ?: "?"}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp),
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// RESTORED V11.2: Admin usage statistics from the original MeloWare project.
// Firebase database schema stays unchanged: usage_stats/yyyy-MM-dd/deviceKey.
private data class MeloWareUsageEntry(
    val key: String,
    val userId: String,
    val deviceId: String,
    val deviceModel: String,
    val osVersion: String,
    val accessTime: String,
    val openCount: Long,
    val durationMs: Long
) {
    val stableDeviceKey: String get() = deviceId.ifBlank { key }
}

private data class MeloWareUsageSummary(
    val devices: Int,
    val opens: Long,
    val durationMs: Long
)

private fun usageSummary(records: List<MeloWareUsageEntry>): MeloWareUsageSummary =
    MeloWareUsageSummary(
        devices = records.map { it.stableDeviceKey }.distinct().size,
        opens = records.sumOf { it.openCount },
        durationMs = records.sumOf { it.durationMs }
    )

private fun usageDuration(ms: Long): String {
    val total = (ms.coerceAtLeast(0L) / 1000L)
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else if (minutes > 0L) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun usageShiftDate(date: String, dayOffset: Int): String {
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply { isLenient = false }
    val base = runCatching { format.parse(date) }.getOrNull() ?: java.util.Date()
    val calendar = java.util.Calendar.getInstance().apply {
        time = base
        add(java.util.Calendar.DAY_OF_MONTH, dayOffset)
    }
    return format.format(calendar.time)
}

/** Server-verified trial statistics. The client never trusts a local admin boolean for data access. */
private data class MeloWaveTrialAdminDevice(
    val shortKey: String,
    val lastSeenAt: String,
    val trialExpiresAt: String,
    val state: String,
    val activeDays: Int
)

private data class MeloWaveTrialAdminReport(
    val devices: Int,
    val eligible: Int,
    val active: Int,
    val expired: Int,
    val today: Int,
    val newToday: Int,
    val activityDays: Int,
    val d1: String,
    val d7: String,
    val d30: String,
    val d1Cohort: Int,
    val d7Cohort: Int,
    val d30Cohort: Int,
    val recent: List<MeloWaveTrialAdminDevice>
)

/**
 * Requires the SAME Google Web ID token as paid Premium, and verifies admin again on PHP.
 * Never embeds the website administrator password or the HMAC secret in Android.
 */
private suspend fun loadMeloWaveTrialAdminReport(context: Context): MeloWaveTrialAdminReport =
    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val opts = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        ).requestIdToken("886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
        val googleClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, opts)
        val signedIn = runCatching {
            com.google.android.gms.tasks.Tasks.await(
                googleClient.silentSignIn(), 12, java.util.concurrent.TimeUnit.SECONDS
            )
        }.getOrNull()
        val idToken = (signedIn?.idToken
            ?: com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(context)?.idToken).orEmpty()
        if (idToken.isBlank()) throw IllegalStateException("Bạn cần đăng nhập lại Google bằng tài khoản Admin.")

        val request = org.json.JSONObject()
            .put("action", "admin_stats")
            .put("idToken", idToken)
        val conn = java.net.URL(
            "https://tunertools.top/melowave/melowave_trial.php"
        ).openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 18_000
            conn.doOutput = true
            conn.useCaches = false
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-store")
            conn.outputStream.use { out -> out.write(request.toString().toByteArray(Charsets.UTF_8)) }
            val http = conn.responseCode
            val stream = if (http in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val result = runCatching { org.json.JSONObject(body) }.getOrNull()
                ?: throw IllegalStateException("Máy chủ trả dữ liệu không hợp lệ (HTTP $http).")
            if (http !in 200..299 || !result.optBoolean("ok", false)) {
                throw IllegalStateException(
                    result.optString("message", "Không thể xem thống kê (HTTP $http).")
                )
            }
            val summary = result.optJSONObject("summary")
                ?: throw IllegalStateException("Thiếu dữ liệu thống kê từ máy chủ.")
            val recent = mutableListOf<MeloWaveTrialAdminDevice>()
            val array = result.optJSONArray("recentDevices")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val node = array.optJSONObject(i) ?: continue
                    recent.add(
                        MeloWaveTrialAdminDevice(
                            shortKey = node.optString("short_key", ""),
                            lastSeenAt = node.optString("last_seen_at", ""),
                            trialExpiresAt = if (node.isNull("trial_expires_at")) ""
                            else node.optString("trial_expires_at", ""),
                            state = when (node.optString("state", "")) {
                                "Chua dung thu" -> "Chưa dùng thử"
                                "Dang dung thu" -> "Đang dùng thử"
                                "Het han" -> "Hết hạn"
                                else -> node.optString("state", "")
                            },
                            activeDays = node.optInt("active_days", 0)
                        )
                    )
                }
            }
            MeloWaveTrialAdminReport(
                devices = summary.optInt("devices", 0),
                eligible = summary.optInt("eligible", 0),
                active = summary.optInt("active", 0),
                expired = summary.optInt("expired", 0),
                today = summary.optInt("today", 0),
                newToday = summary.optInt("newToday", 0),
                activityDays = summary.optInt("activityDays", 0),
                d1 = summary.optString("d1", "—"),
                d7 = summary.optString("d7", "—"),
                d30 = summary.optString("d30", "—"),
                d1Cohort = summary.optInt("d1cohort", 0),
                d7Cohort = summary.optInt("d7cohort", 0),
                d30Cohort = summary.optInt("d30cohort", 0),
                recent = recent
            )
        } finally {
            conn.disconnect()
        }
    }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun MeloWareAdminUsageDialog(onDismiss: () -> Unit) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    var trialReport by remember { mutableStateOf<MeloWaveTrialAdminReport?>(null) }
    var trialLoading by remember { mutableStateOf(false) }
    var trialError by remember { mutableStateOf<String?>(null) }
    var trialRefresh by remember { mutableIntStateOf(0) }
    val today = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(today) }
    var selectedMonth by remember { mutableStateOf(today.take(7)) }
    var showDayPicker by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf(true) }
    var firebaseError by remember { mutableStateOf<String?>(null) }
    var usageByDay by remember {
        mutableStateOf<Map<String, List<MeloWareUsageEntry>>>(emptyMap())
    }

    // Only request MySQL statistics while this admin tab is displayed.
    // PHP independently checks the Google ID token and the server-side allowlist.
    LaunchedEffect(selectedTab, trialRefresh) {
        if (selectedTab != 3) return@LaunchedEffect
        trialLoading = true
        trialError = null
        try {
            trialReport = loadMeloWaveTrialAdminReport(context.applicationContext)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            trialError = e.message ?: "Không thể kết nối máy chủ thống kê Premium."
        } finally {
            trialLoading = false
        }
    }

    // Subscription exists only while the admin dashboard is open.
    DisposableEffect(Unit) {
        val connectionResult = runCatching {
            com.google.firebase.database.FirebaseDatabase.getInstance(
                "https://melowave-f8056-default-rtdb.asia-southeast1.firebasedatabase.app"
            ).getReference("usage_stats")
        }
        val ref = connectionResult.getOrNull()
        if (ref == null) {
            firebaseError = "Không thể kết nối Firebase: ${connectionResult.exceptionOrNull()?.localizedMessage}"
            reading = false
        }
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val result = linkedMapOf<String, List<MeloWareUsageEntry>>()
                for (dateNode in snapshot.children) {
                    val dateKey = dateNode.key ?: continue
                    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(dateKey)) continue
                    val dayUsers = mutableListOf<MeloWareUsageEntry>()
                    for (node in dateNode.children) {
                        val userKey = node.key ?: continue
                        val row = MeloWareUsageEntry(
                            key = userKey,
                            userId = node.child("userId").value?.toString().orEmpty(),
                            deviceId = node.child("deviceId").value?.toString().orEmpty(),
                            deviceModel = node.child("deviceModel").value?.toString().orEmpty(),
                            osVersion = node.child("osVersion").value?.toString().orEmpty(),
                            accessTime = node.child("accessTime").value?.toString().orEmpty(),
                            openCount = ((node.child("openCount").value as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
                            durationMs = ((node.child("durationMs").value as? Number)?.toLong() ?: 0L).coerceAtLeast(0L)
                        )
                        dayUsers.add(row)
                    }
                    result[dateKey] = dayUsers
                }
                usageByDay = result
                reading = false
                firebaseError = null
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                firebaseError = "Firebase từ chối đọc usage_stats: ${error.message}"
                reading = false
            }
        }
        if (ref != null) ref.addValueEventListener(listener)
        onDispose { if (ref != null) ref.removeEventListener(listener) }
    }

    val availableMonths = remember(usageByDay) {
        usageByDay.keys.map { it.take(7) }.distinct().sortedDescending()
    }
    // Keep a selected month even when live data changes or when the current month is empty.
    val actualMonth = selectedMonth.takeIf { it in availableMonths }
        ?: availableMonths.firstOrNull()
        ?: today.take(7)
    val dayRows = usageByDay[selectedDay].orEmpty()
        .sortedByDescending { it.durationMs }
    val monthDays = usageByDay.keys.filter { it.startsWith(actualMonth) }.sorted()
    val monthRows = monthDays.flatMap { usageByDay[it].orEmpty() }
    val monthSummary = usageSummary(monthRows)
    val allMonths = availableMonths.take(12).sorted()
    val tabs = listOf("Theo Ngày", "Theo Tháng", "So Sánh Tháng")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng thống kê", tint = colors.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Thống kê MeloWave", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Firebase + MySQL · Admin", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    if (reading && selectedTab != 3) CircularProgressIndicator(modifier = Modifier.size(23.dp), strokeWidth = 2.dp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Thống kê sử dụng", "Premium 30 ngày").forEachIndexed { index, label ->
                        val active = if (index == 0) selectedTab != 3 else selectedTab == 3
                        Surface(
                            modifier = Modifier.weight(1f).clickable {
                                selectedTab = if (index == 0) 0 else 3
                            },
                            shape = RoundedCornerShape(13.dp),
                            color = if (active) colors.purple.copy(alpha = 0.22f) else colors.card,
                            border = BorderStroke(1.dp, if (active) colors.purpleBright else colors.border)
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 5.dp),
                                textAlign = TextAlign.Center,
                                color = if (active) colors.purpleBright else colors.textSecondary,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                if (selectedTab != 3) Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Surface(
                            modifier = Modifier.weight(1f).clickable { selectedTab = index },
                            shape = RoundedCornerShape(11.dp),
                            color = if (selectedTab == index) colors.purple.copy(alpha = 0.32f) else colors.card,
                            border = BorderStroke(
                                1.dp,
                                if (selectedTab == index) colors.purpleBright else colors.border
                            )
                        ) {
                            Box(Modifier.padding(horizontal = 3.dp, vertical = 11.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    tab,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    color = if (selectedTab == index) colors.purpleBright else colors.textSecondary,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                if (firebaseError != null && selectedTab != 3) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4B2431)
                    ) {
                        Text(firebaseError.orEmpty(), modifier = Modifier.padding(12.dp), color = Color(0xFFFFCCD4), fontSize = 12.sp)
                    }
                }

                when (selectedTab) {
                    0 -> {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ngày: $selectedDay", color = colors.purpleBright, fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                TextButton(onClick = { selectedDay = usageShiftDate(selectedDay, -1) }) {
                                    Text("‹", fontSize = 23.sp, color = colors.textPrimary)
                                }
                                TextButton(onClick = { showDayPicker = true }) {
                                    Text("Chọn", fontSize = 12.sp, color = colors.purpleBright)
                                }
                                TextButton(onClick = { selectedDay = usageShiftDate(selectedDay, 1) }) {
                                    Text("›", fontSize = 23.sp, color = colors.textPrimary)
                                }
                            }
                            val summary = usageSummary(dayRows)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MeloWareUsageCard("Thiết bị", summary.devices.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Lượt mở", summary.opens.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Thời gian", usageDuration(summary.durationMs), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Danh sách người dùng · ${dayRows.size}", color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(7.dp))
                            if (dayRows.isEmpty()) {
                                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    Text(if (reading) "Đang tải..." else "Chưa có dữ liệu ngày này", color = colors.textSecondary)
                                }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    items(dayRows.size) { index ->
                                        MeloWareUsageUserRow(index + 1, dayRows[index])
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tháng ${actualMonth.substringAfter('-')}/${actualMonth.substringBefore('-')}",
                                    color = colors.purpleBright, fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    val i = availableMonths.indexOf(actualMonth)
                                    if (i < availableMonths.lastIndex) selectedMonth = availableMonths[i + 1]
                                }, enabled = availableMonths.indexOf(actualMonth) < availableMonths.lastIndex) {
                                    Text("‹", fontSize = 24.sp)
                                }
                                TextButton(onClick = {
                                    val i = availableMonths.indexOf(actualMonth)
                                    if (i > 0) selectedMonth = availableMonths[i - 1]
                                }, enabled = availableMonths.indexOf(actualMonth) > 0) {
                                    Text("›", fontSize = 24.sp)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MeloWareUsageCard("Thiết bị", monthSummary.devices.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Lượt mở", monthSummary.opens.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Thời gian", usageDuration(monthSummary.durationMs), Modifier.weight(1f))
                            }
                            Text("Biểu đồ theo ngày", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            MeloWareUsageChart(monthDays.map { day ->
                                val item = usageSummary(usageByDay[day].orEmpty())
                                Triple(day.takeLast(2), item.opens, item.durationMs)
                            })
                            Text("Chi tiết từng ngày", color = colors.purpleBright, fontWeight = FontWeight.SemiBold)
                            monthDays.sortedDescending().forEach { day ->
                                val item = usageSummary(usageByDay[day].orEmpty())
                                MeloWareUsageDetailRow(day, item)
                            }
                            if (monthDays.isEmpty()) Text("Chưa có dữ liệu tháng này", color = colors.textSecondary)
                        }
                    }

                    2 -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("So sánh tối đa 12 tháng gần nhất", color = colors.purpleBright,
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            val monthComparisons = allMonths.map { month ->
                                val rows = usageByDay.filterKeys { it.startsWith(month) }.values.flatten()
                                month to usageSummary(rows)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MeloWareUsageCard("Số tháng", monthComparisons.size.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Tổng lượt", monthComparisons.sumOf { it.second.opens }.toString(), Modifier.weight(1f))
                                MeloWareUsageCard("Tổng giờ", String.format(java.util.Locale.US, "%.1f", monthComparisons.sumOf { it.second.durationMs } / 3600000.0), Modifier.weight(1f))
                            }
                            MeloWareUsageChart(monthComparisons.map { (month, item) ->
                                Triple(month.substringAfter('-') + "/" + month.substringBefore('-').takeLast(2), item.opens, item.durationMs)
                            })
                            monthComparisons.asReversed().forEach { (month, item) ->
                                MeloWareUsageDetailRow("Tháng $month", item)
                            }
                            if (monthComparisons.isEmpty()) Text("Chưa có dữ liệu so sánh", color = colors.textSecondary)
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Premium dùng thử 30 ngày",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { trialRefresh++ }, enabled = !trialLoading) {
                                    Text("Làm mới", color = colors.purpleBright)
                                }
                            }
                            Text(
                                "Số liệu PHP / MySQL · múi giờ UTC · yêu cầu Google Admin",
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                            if (trialLoading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Text("Đang tải thống kê từ máy chủ...", color = colors.textSecondary)
                                }
                            }
                            trialError?.let { error ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text(error, modifier = Modifier.padding(12.dp), color = colors.textPrimary)
                                }
                            }
                            trialReport?.let { report ->
                                Text("Tổng quan thiết bị", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MeloWareUsageCard("Thiết bị ghi nhận", report.devices.toString(), Modifier.weight(1f))
                                    MeloWareUsageCard("Mới hôm nay", report.newToday.toString(), Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MeloWareUsageCard("Đang dùng thử", report.active.toString(), Modifier.weight(1f))
                                    MeloWareUsageCard("Đã hết hạn", report.expired.toString(), Modifier.weight(1f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MeloWareUsageCard("Chưa kích hoạt", report.eligible.toString(), Modifier.weight(1f))
                                    MeloWareUsageCard("Hoạt động hôm nay", report.today.toString(), Modifier.weight(1f))
                                }
                                Text("Giữ chân người dùng (Retention)", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MeloWareUsageCard("D1 · ${report.d1Cohort} thiết bị", report.d1, Modifier.weight(1f))
                                    MeloWareUsageCard("D7 · ${report.d7Cohort} thiết bị", report.d7, Modifier.weight(1f))
                                    MeloWareUsageCard("D30 · ${report.d30Cohort} thiết bị", report.d30, Modifier.weight(1f))
                                }
                                Text(
                                    "Tổng ngày hoạt động: ${report.activityDays}. D1/D7/D30: tỷ lệ thiết bị quay lại đúng ngày từ lúc ghi nhận lần đầu.",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    "Thiết bị hoạt động gần nhất (${report.recent.size})",
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (report.recent.isEmpty()) {
                                    Text("Chưa có thiết bị nào được ghi nhận.", color = colors.textSecondary)
                                }
                                report.recent.forEach { device ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = colors.card,
                                        border = BorderStroke(1.dp, colors.border)
                                    ) {
                                        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "Thiết bị ${device.shortKey}…",
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(device.state, color = colors.purpleBright, fontSize = 12.sp)
                                            }
                                            Text("Lần gần nhất: ${device.lastSeenAt} UTC", color = colors.textSecondary, fontSize = 11.sp)
                                            Text(
                                                "Đã hoạt động: ${device.activeDays} ngày · Hết hạn: ${device.trialExpiresAt.ifBlank { "Chưa bắt đầu" }}",
                                                color = colors.textSecondary,
                                                fontSize = 11.sp
                                            )
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

    if (showDayPicker) {
        val picker = androidx.compose.material3.rememberDatePickerState()
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDayPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { timestamp ->
                        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        selectedDay = format.format(java.util.Date(timestamp))
                    }
                    showDayPicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDayPicker = false }) { Text("Hủy") }
            }
        ) { androidx.compose.material3.DatePicker(state = picker) }
    }
}

@Composable
private fun MeloWareUsageCard(title: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 11.sp, color = colors.textSecondary)
            Spacer(Modifier.height(5.dp))
            Text(value, fontSize = 16.sp, maxLines = 1, color = colors.purpleBright, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MeloWareUsageUserRow(index: Int, row: MeloWareUsageEntry) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$index. ${row.userId.ifBlank { "Khách" }}", color = colors.textPrimary,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                Text(usageDuration(row.durationMs), color = colors.purpleBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(listOf(row.deviceModel, row.osVersion).filter { it.isNotBlank() }.joinToString(" · "),
                color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
            Text("Vào lúc ${row.accessTime.ifBlank { "--:--:--" }}  ·  Mở ${row.openCount} lần",
                color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MeloWareUsageDetailRow(label: String, item: MeloWareUsageSummary) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text("${item.devices} thiết bị  ·  ${item.opens} lượt mở  ·  ${usageDuration(item.durationMs)}",
                color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MeloWareUsageChart(points: List<Triple<String, Long, Long>>) {
    val colors = LocalMusicColors.current
    val maxOpens = (points.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L).toFloat()
    val maxDuration = (points.maxOfOrNull { it.third } ?: 0L).coerceAtLeast(1L).toFloat()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("Vàng: lượt mở    Tím: thời gian dùng", color = colors.textSecondary, fontSize = 11.sp)
            if (points.isEmpty()) {
                Text("Chưa có dữ liệu", color = colors.textSecondary, modifier = Modifier.padding(16.dp))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    points.forEach { (label, opens, duration) ->
                        Column(Modifier.width(38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier.height(136.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(
                                    Modifier.width(12.dp)
                                        .height((5f + 125f * opens / maxOpens).dp)
                                        .background(Color(0xFFFFB74D), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                                Box(
                                    Modifier.width(12.dp)
                                        .height((5f + 125f * duration / maxDuration).dp)
                                        .background(colors.purpleBright, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 9.sp, maxLines = 1, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

/** Simple vector crown so this visual doesn't depend on emoji fonts or external assets. */
@Composable
internal fun MeloWarePremiumCrown(modifier: Modifier = Modifier.size(27.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.10f, h * 0.30f)
            lineTo(w * 0.30f, h * 0.49f)
            lineTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.70f, h * 0.49f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.79f, h * 0.75f)
            lineTo(w * 0.21f, h * 0.75f)
            close()
            moveTo(w * 0.21f, h * 0.81f)
            lineTo(w * 0.79f, h * 0.81f)
            lineTo(w * 0.79f, h * 0.91f)
            lineTo(w * 0.21f, h * 0.91f)
            close()
        }
        drawPath(crown, color = Color(0xFFFFD54F))
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    onIconClick: (() -> Unit)? = null,
    premiumBadge: String? = null
) {
    val colors = LocalMusicColors.current
    val premiumGold = Color(0xFFFFD54F)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (onIconClick != null) {
                        Modifier.clickable(onClick = onIconClick)
                    } else {
                        Modifier
                    }
                ),
            shape = CircleShape,
            color = if (premiumBadge != null) premiumGold.copy(alpha = 0.15f)
            else colors.purple.copy(alpha = if (onIconClick != null) 0.22f else 0.13f),
            border = when {
                premiumBadge != null -> BorderStroke(1.dp, premiumGold.copy(alpha = 0.80f))
                onIconClick != null -> BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.75f))
                else -> null
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (premiumBadge != null) {
                    MeloWarePremiumCrown()
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = if (onIconClick != null) "Mở thống kê Firebase" else null,
                        tint = colors.purpleBright
                    )
                }
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                if (premiumBadge != null) {
                    Text(
                        premiumBadge,
                        color = premiumGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .background(premiumGold.copy(alpha = 0.13f), RoundedCornerShape(7.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedCode: String,
    options: List<MeloWareLanguageOption>,
    requiredSelection: Boolean,
    autoSelected: Boolean,
    deviceLanguageCode: String?,
    onSelectAuto: () -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(
        onDismissRequest = { if (!requiredSelection) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !requiredSelection,
            dismissOnClickOutside = !requiredSelection
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 650.dp),
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(vertical = 14.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (requiredSelection) {
                                mwString("language.first.title", "Chọn ngôn ngữ")
                            } else {
                                mwString("language.dialog.title", "Ngôn ngữ")
                            },
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (requiredSelection) {
                                mwString(
                                    "language.first.subtitle",
                                    "Ngôn ngữ của thiết bị chưa được hỗ trợ. Hãy chọn ngôn ngữ để tiếp tục."
                                )
                            } else {
                                mwString("language.dialog.subtitle", "Chọn ngôn ngữ hiển thị cho MeloWare")
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    if (!requiredSelection) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = mwString("common.close", "Đóng"),
                                tint = colors.textSecondary
                            )
                        }
                    }
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    // Auto is offered only when Android's current language is supported.
                    if (!requiredSelection && deviceLanguageCode != null) {
                        item {
                            val systemLanguageName = options.firstOrNull {
                                it.code == deviceLanguageCode
                            }?.nativeName ?: deviceLanguageCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onSelectAuto)
                                    .padding(horizontal = 20.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Language,
                                    contentDescription = null,
                                    tint = colors.purpleBright
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        mwString("language.follow_device", "Theo ngôn ngữ điện thoại"),
                                        color = if (autoSelected) colors.purpleBright else colors.textPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        systemLanguageName + " · " + mwString(
                                            "language.follow_device_subtitle",
                                            "Tự đổi khi bạn thay ngôn ngữ điện thoại"
                                        ),
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (autoSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null,
                                        tint = colors.purpleBright)
                                }
                            }
                        }
                    }
                    items(options.size) { index ->
                        val item = options[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.code) }
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.nativeName,
                                color = if (!autoSelected && item.code == selectedCode) {
                                    colors.purpleBright
                                } else {
                                    colors.textPrimary
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (!autoSelected && item.code == selectedCode) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = colors.purpleBright
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Hue slider and the eight existing presets share the same accent theme state. */
@Composable
private fun AccentColorDialog(
    selectedId: String,
    selectedHue: Float,
    onSelect: (String) -> Unit,
    onCustomHueChange: (Float) -> Unit,
    onCustomHueChangeFinished: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val previewColor = colors.purple
    // Presets display their original hue in the slider until the user drags it.
    val displayHue = when (selectedId) {
        "purple" -> 275f
        "blue" -> 221f
        "teal" -> 175f
        "green" -> 145f
        "orange" -> 25f
        "pink" -> 332f
        "red" -> 0f
        "amber" -> 33f
        else -> selectedHue
    }
    val rainbow = remember {
        Brush.horizontalGradient(
            listOf(
                Color.hsv(0f, 1f, 1f), Color.hsv(60f, 1f, 1f),
                Color.hsv(120f, 1f, 1f), Color.hsv(180f, 1f, 1f),
                Color.hsv(240f, 1f, 1f), Color.hsv(300f, 1f, 1f),
                Color.hsv(0f, 1f, 1f)
            )
        )
    }
    val dialogMaxHeight = (LocalConfiguration.current.screenHeightDp - 72)
        .coerceIn(300, 610).dp
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = dialogMaxHeight),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mwString("settings.accent", "Màu giao diện"),
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                Text(
                    mwString("accent.prompt", "Chọn màu chủ đạo cho MeloWare"),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                MusicAccentOptions.chunked(4).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = option.id == selectedId
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelect(option.id) }
                                    .padding(vertical = 6.dp, horizontal = 1.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(43.dp)
                                        .background(option.preview, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) colors.textPrimary else colors.border,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Đang chọn",
                                            tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                                Text(
                                    mwString("accent.${option.id}", option.title),
                                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        .height(1.dp).background(colors.borderSoft)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mwString("accent.custom_section", "Tùy chỉnh màu sắc"),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedId == MELOWARE_CUSTOM_ACCENT_ID) {
                        Text("${displayHue.toInt()}°", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(12.dp)
                            .background(rainbow, RoundedCornerShape(50))
                    )
                    Slider(
                        value = displayHue,
                        onValueChange = onCustomHueChange,
                        onValueChangeFinished = onCustomHueChangeFinished,
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = previewColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(colors.cardRaised, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(38.dp)
                            .background(previewColor, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Palette, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mwString("accent.preview", "Xem trước màu đã chọn"), color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(mwString("accent.instant_save", "Áp dụng ngay · Tự động lưu"),
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier.size(24.dp)
                            .background(previewColor, CircleShape)
                            .border(1.dp, colors.border, CircleShape)
                    )
                }
                Text(
                    mwString("accent.slider_note", "Kéo thanh màu hoặc chọn nhanh một màu phía trên."),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun ThemeModeDialog(
    selectedMode: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val options = listOf(
        Triple(2, Icons.Rounded.DarkMode, mwString("theme.mode_dark", "Chế độ tối")),
        Triple(1, Icons.Rounded.LightMode, mwString("theme.mode_light", "Chế độ sáng")),
        Triple(0, Icons.Rounded.SettingsBrightness, mwString("theme.mode_system", "Theo hệ thống"))
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(vertical = 14.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        mwString("settings.theme", "Chế độ giao diện"),
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                options.forEach { (mode, icon, title) ->
                    SettingRow(
                        icon = icon,
                        title = title,
                        subtitle = if (mode == selectedMode) mwString("theme.active", "Đang sử dụng") else "",
                        onClick = { onSelect(mode) }
                    )
                }
            }
        }
    }
}

/**
 * Gộp Receiver vào MainActivity.kt để dự án chỉ còn đúng 6 file Kotlin.
 * Nội dung và lịch 21:30 được giữ theo ReminderReceiver gốc.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val channelId = "daily_music_reminder_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở nghe nhạc",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefs = context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("app_language", "vi") ?: "vi"

        val title = when (lang) {
            "en" -> "Time to relax 🌙"
            "zh" -> "放松时间 🌙"
            "ja" -> "リラックスタイム 🌙"
            "ko" -> "휴식 시간 🌙"
            "es" -> "Hora de relajarse 🌙"
            "fr" -> "Moment de détente 🌙"
            "de" -> "Zeit zum Entspannen 🌙"
            "ru" -> "Время расслабиться 🌙"
            "pt" -> "Hora de relaxar 🌙"
            "id" -> "Waktunya bersantai 🌙"
            "hi" -> "आराम का समय 🌙"
            "ar" -> "وقت الاسترخاء 🌙"
            "th" -> "เวลาพักผ่อน 🌙"
            "it" -> "Tempo di relax 🌙"
            else -> "Thư giãn cuối ngày 🌙"
        }

        val message = when (lang) {
            "en" -> "Listening to a song will help you sleep better 🎶"
            "zh" -> "听一首音乐会让您睡得更好 🎶"
            "ja" -> "音楽を聴くと、よりよく眠れるようになります 🎶"
            "ko" -> "음악을 들으면 더 쉽게 잠들 수 있습니다 🎶"
            "es" -> "Escuchar música te ayudará a dormir mejor 🎶"
            "fr" -> "Écouter de la musique vous aidera à mieux dormir 🎶"
            "de" -> "Musik zu hören hilft Ihnen, besser zu schlafen 🎶"
            "ru" -> "Прослушивание музыки поможет вам лучше уснуть 🎶"
            "pt" -> "Ouvir uma música vai te ajudar a dormir melhor 🎶"
            "id" -> "Mendengarkan musik akan membuat Anda lebih mudah tidur 🎶"
            "hi" -> "संगीत सुनने से आपको बेहतर नींद आने में मदद मिलेगी 🎶"
            "ar" -> "الاستماع إلى الموسيقى سيساعدك على النوم بشكل أفضل 🎶"
            "th" -> "การฟังเพลงจะช่วยให้คุณนอนหลับได้ง่ายขึ้น 🎶"
            "it" -> "Ascoltare musica ti aiuterà a dormire meglio 🎶"
            else -> "Nghe 1 bản nhạc sẽ làm bạn dễ ngủ hơn 🎶"
        }

        val logoBitmap = runCatching {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        }.getOrNull()

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setColor(android.graphics.Color.parseColor("#8D35FF"))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (logoBitmap != null) notificationBuilder.setLargeIcon(logoBitmap)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        // Store only notifications actually posted by MeloWave. The home bell can
        // show this local history without a new server, OAuth scope or library.
        HomeNotificationStore.record(context, title, message)
        scheduleDailyReminder(context)
    }
}

@SuppressLint("MissingPermission")
fun scheduleDailyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1220,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    } catch (error: SecurityException) {
        error.printStackTrace()
    }
}
// endregion Merged source: MainActivity.kt

// region Merged source: MeloWaveSplashScreen.kt
/**
 * MeloWave launch animation (variant 1): dark neon, real app icon and animated
 * sound bars. Runs only on a cold Activity launch; no video/GIF/network request.
 * The regular music screen initializes behind it and is revealed without
 * recreating MusicController or interrupting audio.
 */
@Composable
fun MeloWaveLaunchSplash(onFinished: () -> Unit) {
    val latestOnFinished by rememberUpdatedState(onFinished)
    val opacity = remember { Animatable(1f) }
    val iconScale = remember { Animatable(0.88f) }
    val pulse by rememberInfiniteTransition(label = "MeloWave opening sound")
        .animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Animated wave phase"
        )

    LaunchedEffect(Unit) {
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }
        // Allow users to see the animated logo and sound waves before entering the app.
        delay(2_600L)
        opacity.animateTo(targetValue = 0f, animationSpec = tween(260))
        latestOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity.value }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050916), Color(0xFF0B0920), Color(0xFF040C1C))
                )
            )
            // Block touches from going through to the player while the splash is visible.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val glowRadius = size.width * 0.74f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x663C1B9D), Color(0x253A1F84), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.41f),
                    radius = glowRadius
                ),
                center = Offset(size.width / 2f, size.height * 0.41f),
                radius = glowRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2B005CC4), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.82f),
                    radius = size.width * 0.84f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.82f),
                radius = size.width * 0.84f
            )
        }

        // Soft flowing waves at the bottom, rather than moving images or video.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .align(Alignment.BottomCenter)
        ) {
            val width = size.width
            val height = size.height
            repeat(3) { layer ->
                val baseline = height * (0.32f + layer * 0.21f)
                val amplitude = height * (0.075f + layer * 0.018f)
                val path = Path().apply {
                    moveTo(0f, baseline)
                    for (step in 1..80) {
                        val x = width * step / 80f
                        val angle = (x / width * (2.2f + layer * 0.27f) * PI.toFloat() +
                                pulse * (0.42f - layer * 0.11f)).toDouble()
                        lineTo(x, baseline + sin(angle).toFloat() * amplitude)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                val tint = when (layer) {
                    0 -> Color(0xFF7A1BF2)
                    1 -> Color(0xFF177BFF)
                    else -> Color(0xFFDB2AC9)
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.02f))
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Keep the original 220dp layout slot so the splash's text and spectrum
            // stay in their existing positions. The old circular spinner is gone.
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                val logoShape = RoundedCornerShape(34.dp)
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .shadow(18.dp, logoShape, clip = false)
                        .clip(logoShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF200B48), Color(0xFF090D2B), Color(0xFF07182D))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    // Use the real launcher icon: no replacement PNG and no new assets.
                    val appIcon = remember(context) {
                        runCatching {
                            context.packageManager.getApplicationIcon(context.packageName)
                        }.getOrNull()
                    }
                    if (appIcon != null) {
                        AndroidView(
                            factory = { viewContext ->
                                ImageView(viewContext).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageDrawable(appIcon)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // The seven-colour loading trail moves *on the actual rounded
                    // square border of the logo*, not on a separate circular ring.
                    // Compose PathMeasure follows every straight edge and corner.
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val lineWidth = 3.2.dp.toPx()
                        val inset = lineWidth / 2f + 1.1.dp.toPx()
                        val radius = (34.dp.toPx() - inset).coerceAtLeast(0f)
                        val roundBorder = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    left = inset,
                                    top = inset,
                                    right = size.width - inset,
                                    bottom = size.height - inset,
                                    topLeftCornerRadius = CornerRadius(radius),
                                    topRightCornerRadius = CornerRadius(radius),
                                    bottomRightCornerRadius = CornerRadius(radius),
                                    bottomLeftCornerRadius = CornerRadius(radius)
                                )
                            )
                        }
                        drawPath(
                            path = roundBorder,
                            color = Color(0x885A5CB9),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        val measure = PathMeasure().apply { setPath(roundBorder, true) }
                        val perimeter = measure.length
                        if (perimeter > 0f) {
                            // Seven visibly different rainbow colours, interpolated
                            // across the travelling highlight for a smooth animation.
                            val rainbow = listOf(
                                Color(0xFFFF4268), // Red
                                Color(0xFFFF9D36), // Orange
                                Color(0xFFFFE64D), // Yellow
                                Color(0xFF47E879), // Green
                                Color(0xFF24DBE9), // Cyan
                                Color(0xFF4383FF), // Blue
                                Color(0xFFBB5DFF)  // Violet
                            )
                            val position = (pulse / (2f * PI.toFloat())) * perimeter
                            val trailLength = perimeter * 0.34f
                            val pieces = 42
                            val brightStyle = Stroke(width = lineWidth, cap = StrokeCap.Round)
                            val glowStyle = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)

                            for (index in 0 until pieces) {
                                val progress = (index + 0.5f) / pieces
                                val huePoint = progress * (rainbow.size - 1)
                                val first = huePoint.toInt().coerceIn(0, rainbow.size - 2)
                                val hue = lerp(rainbow[first], rainbow[first + 1], huePoint - first)
                                val tailFade = 0.24f + progress * 0.76f
                                val from = position - trailLength + index * trailLength / pieces
                                val to = position - trailLength + (index + 1) * trailLength / pieces
                                val wrappedFrom = ((from % perimeter) + perimeter) % perimeter
                                val wrappedTo = ((to % perimeter) + perimeter) % perimeter
                                val segment = Path()
                                if (wrappedFrom < wrappedTo) {
                                    measure.getSegment(wrappedFrom, wrappedTo, segment, true)
                                } else {
                                    measure.getSegment(wrappedFrom, perimeter, segment, true)
                                    if (wrappedTo > 0f) {
                                        measure.getSegment(0f, wrappedTo, segment, true)
                                    }
                                }
                                drawPath(
                                    path = segment,
                                    color = hue.copy(alpha = tailFade * 0.30f),
                                    style = glowStyle
                                )
                                drawPath(
                                    path = segment,
                                    color = hue.copy(alpha = tailFade),
                                    style = brightStyle
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MeloWave",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFF7ADFB), Color(0xFFC16AFE), Color(0xFF4BD6F9))
                    ),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Âm nhạc của bạn",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCDD1E9),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Audio spectrum. Each bar varies with the same animation phase but a
            // different offset, producing continuous, non-repeating-looking motion.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.53f)
                    .height(49.dp)
            ) {
                val bars = 29
                val spacing = size.width / (bars + 1)
                val centerY = size.height / 2f
                val maxAmplitude = size.height * 0.84f
                val strokePx = 3.3.dp.toPx()
                val glow = Brush.verticalGradient(
                    listOf(Color(0xFFDE43F1), Color(0xFF8660FA), Color(0xFF24C7FC))
                )
                repeat(bars) { bar ->
                    val side = abs(bar - (bars - 1) / 2f) / ((bars - 1) / 2f)
                    val envelope = 1f - 0.78f * side
                    val motion = abs(
                        sin((pulse * 1.55f + bar * 0.57f).toDouble()) *
                                sin((pulse * 0.64f - bar * 0.19f).toDouble())
                    ).toFloat()
                    val heightPx = 5.dp.toPx() + maxAmplitude * envelope * (0.19f + 0.79f * motion)
                    val x = spacing * (bar + 1)
                    drawLine(
                        brush = glow,
                        start = Offset(x, centerY - heightPx / 2f),
                        end = Offset(x, centerY + heightPx / 2f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
// endregion Merged source: MeloWaveSplashScreen.kt

// region Merged source: MeloWarePremium.kt
/** Product and base-plan IDs are EXACTLY the IDs configured in Google Play Console. */
private const val PREMIUM_PRODUCT_ID = "meloware_premium"
private const val PREMIUM_6M_BASE_PLAN = "premium-6m"
private const val PREMIUM_12M_BASE_PLAN = "premium-12m"
/** HTTPS only. Publish melowave/meloware_premium.php at this path before enabling paid checkout. */
private const val PREMIUM_BACKEND_URL = "https://tunertools.top/melowave/meloware_premium.php"
private const val BILLING_LOG_TAG = "MeloWareBilling"
private const val BILLING_REQUEST_TIMEOUT_MS = 20_000L

internal data class PremiumDisplayOffer(
    val formattedPrice: String,
    val billingPeriod: String,
    val offerToken: String
)

/** Only server-validated entitlement can unlock paid features; never trust local purchase flags. */
internal object MeloWarePremiumAccess {
    // Google-verified paid entitlement is kept separate from anonymous device trial.
    // Paid always wins when both are active; signing out never discards device trial.
    private var paidIsActive by mutableStateOf(false)
    private var paidAccessKind by mutableStateOf("none")
    private var paidExpiresAtMillis by mutableStateOf(0L)
    private var paidEnforceFeatures by mutableStateOf(false)
    val isActive: Boolean get() = canUsePaidFeatures
    val accessKind: String get() = when {
        paidIsActive && paidExpiresAtMillis > System.currentTimeMillis() -> paidAccessKind
        MeloWaveTrial.isActive -> "trial"
        else -> "none"
    }
    val expiresAtMillis: Long get() = when {
        paidIsActive && paidExpiresAtMillis > System.currentTimeMillis() -> paidExpiresAtMillis
        MeloWaveTrial.isActive -> MeloWaveTrial.expiresAtMillis
        else -> 0L
    }
    val enforceFeatures: Boolean get() = paidEnforceFeatures || MeloWaveTrial.enforceFeatures
    var checkoutEnabled by mutableStateOf(false)
        private set
    var checkoutReason by mutableStateOf("")
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    val canUsePaidFeatures: Boolean get() =
        (paidIsActive && paidExpiresAtMillis > System.currentTimeMillis()) || MeloWaveTrial.isActive

    // The Premium plan is authoritative for the features below. Both a verified
    // paid entitlement and an active device Trial unlock the exact same bundle.
    // Keep these checks centralized so individual screens cannot drift apart.
    private val namedPremiumFeatureLocked: Boolean get() = !canUsePaidFeatures
    val audioConverterRequiresPremium: Boolean get() = namedPremiumFeatureLocked
    val videoExtractorRequiresPremium: Boolean get() = namedPremiumFeatureLocked
    // Review-only: let Google's verifier exercise both Drive entry paths
    // without needing a subscription. Normal production billing stays unchanged.
    val googleDriveRequiresPremium: Boolean get() =
        namedPremiumFeatureLocked && !BuildConfig.DRIVE_READONLY_REVIEW
    val equalizerRequiresPremium: Boolean get() = namedPremiumFeatureLocked
    val albumStudioRequiresPremium: Boolean get() = namedPremiumFeatureLocked
    val smartPlaylistRequiresPremium: Boolean get() = namedPremiumFeatureLocked
    val shouldShowAds: Boolean get() = !canUsePaidFeatures

    fun applyServerStatus(data: JSONObject) {
        // The backend must validate Google ID token before returning this response.
        paidIsActive = data.optBoolean("access", false)
        paidAccessKind = data.optString("accessKind", "none")
        paidExpiresAtMillis = data.optLong("expiresAtMillis", 0L)
        checkoutEnabled = data.optBoolean("checkoutEnabled", false)
        checkoutReason = data.optString("checkoutReason", "")
        paidEnforceFeatures = data.optBoolean("enforceFeatures", false)
        lastError = null
    }

    fun reportError(message: String) {
        lastError = message
        checkoutEnabled = false // FAIL CLOSED for charging if the verification server is unavailable.
        checkoutReason = ""
        // Do not erase a previously validated access state during a transient outage.
    }

    fun clearAccount() {
        paidIsActive = false
        paidAccessKind = "none"
        paidExpiresAtMillis = 0L
        paidEnforceFeatures = false
        checkoutEnabled = false
        checkoutReason = ""
        lastError = null
    }
}

/** API requests run off the UI thread. Google ID tokens are never placed in URLs or logs. */
private object PremiumBackend {
    /**
     * Temporary diagnostic acknowledgement. It contains no Google ID token or purchase token.
     * The PHP monitor uses this to prove Android received and parsed the PHP response.
     */
    private fun sendDebugAck(
        correlationId: String,
        receivedHttp: Int,
        androidParsedJson: Boolean,
        receivedOk: Boolean,
        receivedMessage: String
    ) {
        if (correlationId.isBlank()) return
        runCatching {
            val ack = JSONObject()
                .put("action", "debug_ack")
                .put("clientRequestId", UUID.randomUUID().toString())
                .put("ackRequestId", correlationId)
                .put("receivedHttp", receivedHttp)
                .put("androidParsedJson", androidParsedJson)
                .put("receivedOk", receivedOk)
                .put("receivedMessage", receivedMessage.take(240))
                .put("clientTimeMillis", System.currentTimeMillis())
            val connection = URL(PREMIUM_BACKEND_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-store")
                connection.outputStream.use {
                    it.write(ack.toString().toByteArray(StandardCharsets.UTF_8))
                }
                // Trigger the request; the acknowledgement result never changes Premium state.
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        }.onFailure {
            Log.w(BILLING_LOG_TAG, "Premium debug ACK failed", it)
        }
    }

    suspend fun request(activity: FragmentActivity, action: String, purchaseToken: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            // Refresh the short-lived Google ID token silently when possible.
            // The same Web OAuth client is used by MainActivity's Google sign-in.
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("886650248212-iuvj3vueej3u8eu6jb0mpsbifmmq7dsk.apps.googleusercontent.com")
                .requestEmail().requestProfile().build()
            val latest = runCatching {
                Tasks.await(GoogleSignIn.getClient(activity, options).silentSignIn(), 12, TimeUnit.SECONDS)
            }.getOrNull()
            val idToken = (latest?.idToken ?: GoogleSignIn.getLastSignedInAccount(activity)?.idToken)
                ?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Vui lòng đăng nhập Google trong Cài đặt.")

            val clientRequestId = UUID.randomUUID().toString()
            val appVersionName = runCatching {
                activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty()
            }.getOrDefault("")
            val request = JSONObject()
                .put("action", action)
                .put("idToken", idToken)
                .put("clientRequestId", clientRequestId)
                .put("clientTimeMillis", System.currentTimeMillis())
                .put("appVersionName", appVersionName)
            if (purchaseToken != null) request.put("purchaseToken", purchaseToken)

            val connection = URL(PREMIUM_BACKEND_URL).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 18000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-store")
                connection.outputStream.use {
                    it.write(request.toString().toByteArray(StandardCharsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val response = runCatching { JSONObject(content) }.getOrNull()
                val traceId = response?.optString("requestId")
                    ?.takeIf { it.isNotBlank() } ?: clientRequestId
                val parsed = response != null
                val responseOk = response?.optBoolean("ok", false) ?: false
                val responseMessage = response?.optString("message", "")
                    ?.takeIf { it.isNotBlank() }
                    ?: if (parsed) "" else "Android không đọc được JSON phản hồi"

                sendDebugAck(
                    correlationId = traceId,
                    receivedHttp = responseCode,
                    androidParsedJson = parsed,
                    receivedOk = responseOk,
                    receivedMessage = responseMessage
                )

                if (response == null) {
                    throw IllegalStateException(
                        "Máy chủ Premium trả dữ liệu không hợp lệ ($responseCode) [ID: ${traceId.take(8)}]."
                    )
                }
                if (responseCode !in 200..299 || !responseOk) {
                    val message = response.optString(
                        "message",
                        "Không thể xác minh Premium ($responseCode)."
                    )
                    throw IllegalStateException("$message [ID: ${traceId.take(8)}]")
                }
                Log.d(BILLING_LOG_TAG, "Premium backend $action OK, trace=${traceId.take(8)}")
                response
            } finally {
                connection.disconnect()
            }
        }
}

/** Kept for the entire MainActivity lifetime so a purchase is processed even if the dialog closes. */
internal class MeloWarePremiumBillingManager(private val activity: FragmentActivity) {
    var loading by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set
    var sixMonth by mutableStateOf<PremiumDisplayOffer?>(null)
        private set
    var twelveMonth by mutableStateOf<PremiumDisplayOffer?>(null)
        private set
    /** Query failures are separate from backend or purchase-verification messages. */
    var priceError by mutableStateOf<String?>(null)
        private set

    private var productDetails: ProductDetails? = null
    private var started = false
    private var closed = false
    private var connecting = false
    private var queryInProgress = false
    private var connectionGeneration = 0
    private var priceGeneration = 0
    private var connectionTimeoutJob: Job? = null
    private var priceTimeoutJob: Job? = null
    private val pendingVerifications = hashSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val billingClient = BillingClient.newBuilder(activity.applicationContext)
        .setListener { result, purchases ->
            activity.runOnUiThread {
                if (!closed) handlePurchaseUpdate(result, purchases.orEmpty())
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (closed || started) return
        started = true
        connect()
        refreshEntitlement()
    }

    fun onResume() {
        if (closed || !started) return
        refreshEntitlement()
        // An existing Billing connection may predate License Testing / Play Store changes.
        // Re-query prices as well as purchases when the user comes back to MeloWare.
        if (billingClient.isReady) {
            refreshPrices()
            restorePurchases()
        } else {
            connect()
        }
    }

    fun onAccountChanged() {
        MeloWarePremiumAccess.clearAccount()
        refreshEntitlement()
        if (billingClient.isReady) {
            refreshPrices()
            restorePurchases()
        }
    }

    fun refreshEntitlement() {
        if (closed) return
        if (GoogleSignIn.getLastSignedInAccount(activity)?.idToken.isNullOrBlank()) {
            MeloWarePremiumAccess.clearAccount()
            return
        }
        scope.launch {
            try {
                val status = PremiumBackend.request(activity, "status")
                if (!closed) MeloWarePremiumAccess.applyServerStatus(status)
            } catch (error: Exception) {
                if (!closed) MeloWarePremiumAccess.reportError(error.message ?: "Không kết nối được máy chủ Premium.")
            }
        }
    }

    private fun describeBillingFailure(result: BillingResult): String =
        "Google Play Billing (mã ${result.responseCode}): " +
                result.debugMessage.ifBlank { "không cung cấp mô tả lỗi" }

    private fun connect() {
        if (closed || connecting || billingClient.isReady) return
        connecting = true
        loading = true
        priceError = null
        val attempt = ++connectionGeneration
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = scope.launch {
            delay(BILLING_REQUEST_TIMEOUT_MS)
            if (!closed && connecting && attempt == connectionGeneration) {
                connecting = false
                loading = false
                priceError = "Google Play Billing không phản hồi kết nối sau 20 giây. " +
                        "Hãy kiểm tra Play Store, mạng và thử lại."
                Log.w(BILLING_LOG_TAG, "Billing service connection timed out")
            }
        }
        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    activity.runOnUiThread {
                        if (closed || attempt != connectionGeneration) return@runOnUiThread
                        connectionTimeoutJob?.cancel()
                        connecting = false
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.d(BILLING_LOG_TAG, "Billing connected; query subscriptions")
                            refreshPrices()
                            restorePurchases()
                        } else {
                            loading = false
                            priceError = describeBillingFailure(result)
                            Log.w(BILLING_LOG_TAG, priceError.orEmpty())
                        }
                    }
                }
                override fun onBillingServiceDisconnected() {
                    activity.runOnUiThread {
                        if (closed || attempt != connectionGeneration) return@runOnUiThread
                        connectionTimeoutJob?.cancel()
                        connecting = false
                        loading = false
                        priceError = "Mất kết nối dịch vụ Google Play Billing. Nhấn Thử lại."
                        Log.w(BILLING_LOG_TAG, "Billing service disconnected")
                        // Auto-service-reconnection is enabled; reconnect on next query.
                    }
                }
            })
        } catch (error: Exception) {
            connectionTimeoutJob?.cancel()
            connecting = false
            loading = false
            priceError = "Không thể bắt đầu Google Play Billing: ${error.message ?: error.javaClass.simpleName}"
            Log.e(BILLING_LOG_TAG, "Billing connection failed", error)
        }
    }

    fun refreshPrices() {
        if (closed) return
        if (!billingClient.isReady) {
            connect()
            return
        }
        if (queryInProgress) return
        queryInProgress = true
        loading = true
        priceError = null
        val attempt = ++priceGeneration
        priceTimeoutJob?.cancel()
        priceTimeoutJob = scope.launch {
            delay(BILLING_REQUEST_TIMEOUT_MS)
            if (!closed && queryInProgress && attempt == priceGeneration) {
                queryInProgress = false
                loading = false
                priceError = "Google Play không trả về giá Premium sau 20 giây. " +
                        "Kiểm tra tài khoản License Tester hoặc nhấn Thử lại."
                Log.w(BILLING_LOG_TAG, "Subscription price query timed out")
            }
        }
        val query = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS).build())
        ).build()
        try {
            billingClient.queryProductDetailsAsync(query) { result, response ->
                activity.runOnUiThread {
                    if (closed || attempt != priceGeneration) return@runOnUiThread
                    priceTimeoutJob?.cancel()
                    queryInProgress = false
                    loading = false
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        priceError = describeBillingFailure(result)
                        sixMonth = null
                        twelveMonth = null
                        productDetails = null
                        Log.w(BILLING_LOG_TAG, "Price query failed: $priceError")
                        return@runOnUiThread
                    }
                    productDetails = response.productDetailsList.firstOrNull {
                        it.productId == PREMIUM_PRODUCT_ID
                    }
                    sixMonth = offerFor(PREMIUM_6M_BASE_PLAN)
                    twelveMonth = offerFor(PREMIUM_12M_BASE_PLAN)

                    val unfetched = response.unfetchedProductList
                    val plans = productDetails?.subscriptionOfferDetails.orEmpty()
                        .map { it.basePlanId }.distinct()
                    priceError = when {
                        productDetails == null && unfetched.isNotEmpty() -> {
                            val reasons = unfetched.joinToString("; ") { item ->
                                val reason = when (item.statusCode) {
                                    UnfetchedProduct.StatusCode.INVALID_PRODUCT_ID_FORMAT ->
                                        "ID sản phẩm không hợp lệ"
                                    UnfetchedProduct.StatusCode.PRODUCT_NOT_FOUND ->
                                        "Không tìm thấy sản phẩm trên Google Play"
                                    UnfetchedProduct.StatusCode.NO_ELIGIBLE_OFFER ->
                                        "Không có gói phù hợp cho tài khoản hoặc quốc gia này"
                                    else -> "Lỗi không xác định"
                                }
                                "${item.productId}: $reason (mã ${item.statusCode})"
                            }
                            "Google Play chưa cung cấp giá: $reasons"
                        }
                        productDetails == null ->
                            "Google Play trả danh sách rỗng cho $PREMIUM_PRODUCT_ID. " +
                                    "Kiểm tra Gmail License Tester, ứng dụng trên Play và vùng phân phối."
                        sixMonth == null || twelveMonth == null -> {
                            val missing = buildList {
                                if (sixMonth == null) add(PREMIUM_6M_BASE_PLAN)
                                if (twelveMonth == null) add(PREMIUM_12M_BASE_PLAN)
                            }
                            "Đã tìm thấy sản phẩm, nhưng thiếu gói ${missing.joinToString()}. " +
                                    "Base plan Google Play trả về: ${plans.joinToString().ifBlank { "không có" }}."
                        }
                        else -> null
                    }
                    if (priceError == null) {
                        Log.d(BILLING_LOG_TAG, "Received prices for both Premium base plans")
                    } else {
                        Log.w(BILLING_LOG_TAG, priceError.orEmpty())
                    }
                }
            }
        } catch (error: Exception) {
            priceTimeoutJob?.cancel()
            queryInProgress = false
            loading = false
            priceError = "Không gửi được yêu cầu lấy giá: ${error.message ?: error.javaClass.simpleName}"
            Log.e(BILLING_LOG_TAG, "Product details request failed", error)
        }
    }

    private fun offerFor(basePlan: String): PremiumDisplayOffer? = productDetails
        ?.subscriptionOfferDetails
        ?.firstOrNull { it.basePlanId == basePlan && it.offerId == null }
        ?.let { offer ->
            PremiumDisplayOffer(
                formattedPrice = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice.orEmpty(),
                billingPeriod = offer.pricingPhases.pricingPhaseList.lastOrNull()?.billingPeriod.orEmpty(),
                offerToken = offer.offerToken
            )
        }

    fun buy(basePlanId: String) {
        if (closed || busy) return
        val offer = when (basePlanId) {
            PREMIUM_6M_BASE_PLAN -> sixMonth
            PREMIUM_12M_BASE_PLAN -> twelveMonth
            else -> null
        }
        val detail = productDetails
        if (!MeloWarePremiumAccess.checkoutEnabled || offer == null || detail == null || !billingClient.isReady) {
            statusMessage = "Chưa thể mua: Google Play hoặc máy chủ xác minh chưa sẵn sàng."
            return
        }
        if (MeloWarePremiumAccess.accessKind == "paid" && MeloWarePremiumAccess.canUsePaidFeatures) {
            statusMessage = "Premium vẫn còn hạn. Bạn có thể mua thêm khi gói hiện tại kết thúc."
            return
        }
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account?.idToken.isNullOrBlank() || account?.id.isNullOrBlank()) {
            statusMessage = "Vui lòng đăng nhập Google trong Cài đặt trước khi mua Premium."
            return
        }
        // Obfuscated ID prevents sending email or other PII to Google Play.
        val obfuscatedId = MessageDigest.getInstance("SHA-256")
            .digest("meloware:${account!!.id}".toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(detail)
                    .setOfferToken(offer.offerToken)
                    .build())
            )
            .setObfuscatedAccountId(obfuscatedId)
            .build()
        val result = billingClient.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            statusMessage = result.debugMessage.ifBlank { "Không mở được thanh toán Google Play." }
        }
    }

    private fun handlePurchaseUpdate(result: BillingResult, purchases: List<Purchase>) {
        if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            statusMessage = "Đã hủy thanh toán."
            return
        }
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            statusMessage = result.debugMessage.ifBlank { "Google Play không hoàn tất thanh toán." }
            return
        }
        processPurchases(purchases)
    }

    fun restorePurchases() {
        if (closed || !billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS).build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            activity.runOnUiThread {
                if (closed) return@runOnUiThread
                if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases)
                else statusMessage = "Không thể khôi phục giao dịch: ${result.debugMessage}"
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (PREMIUM_PRODUCT_ID !in purchase.products) continue
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> verifyPurchase(purchase.purchaseToken)
                Purchase.PurchaseState.PENDING -> statusMessage = "Giao dịch đang chờ thanh toán. Premium chưa được cấp quyền."
            }
        }
    }

    private fun verifyPurchase(token: String) {
        if (closed || token.isBlank() || !pendingVerifications.add(token)) return
        busy = true
        statusMessage = "Đang xác minh giao dịch với Google Play..."
        scope.launch {
            try {
                val response = PremiumBackend.request(activity, "verify", token)
                if (!closed) {
                    MeloWarePremiumAccess.applyServerStatus(response)
                    statusMessage = "Đã xác minh giao dịch. Quyền Premium được cập nhật."
                }
            } catch (error: Exception) {
                if (!closed) statusMessage = error.message ?: "Xác minh giao dịch thất bại. Hãy thử khôi phục."
                // Do not unlock locally. Re-query Play the next time app resumes.
            } finally {
                pendingVerifications.remove(token)
                busy = pendingVerifications.isNotEmpty()
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        connectionTimeoutJob?.cancel()
        priceTimeoutJob?.cancel()
        scope.cancel()
        billingClient.endConnection()
    }
}

@Composable
internal fun MeloWarePremiumDialog(
    activity: FragmentActivity,
    billing: MeloWarePremiumBillingManager,
    onRequestGoogleLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    // Query again whenever the Premium screen is reopened, not only at app startup.
    LaunchedEffect(billing) {
        billing.refreshPrices()
        MeloWaveTrial.refresh(activity, force = true)
    }
    var selectedPlan by androidx.compose.runtime.remember { mutableStateOf(PREMIUM_6M_BASE_PLAN) }
    val loggedIn = !GoogleSignIn.getLastSignedInAccount(activity)?.idToken.isNullOrBlank()
    val selectedOffer = if (selectedPlan == PREMIUM_6M_BASE_PLAN) billing.sixMonth else billing.twelveMonth
    val canPurchase = loggedIn && MeloWarePremiumAccess.checkoutEnabled && !billing.loading && !billing.busy &&
            selectedOffer != null && !(MeloWarePremiumAccess.accessKind == "paid" && MeloWarePremiumAccess.canUsePaidFeatures)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).heightIn(max = 660.dp),
            color = colors.card, shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PremiumHeroBadge()
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mwString("premium.title", "MeloWare Premium"),
                            color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(mwString("premium.subtitle", "Mở khóa các tính năng nâng cao"),
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }

                Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(15.dp),
                    border = BorderStroke(1.dp, colors.border)) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(mwString("premium.benefits_title", "Quyền lợi Premium"),
                            color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        PremiumBenefitCard(
                            type = PremiumBenefitType.AUDIO_CONVERTER,
                            title = mwString("premium.benefit_converter", "Chuyển đổi định dạng Pro"),
                            detail = mwString("premium.benefit_converter_detail", "Chuyển đổi một hoặc nhiều file với đầy đủ định dạng âm thanh.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.VIDEO_EXTRACTOR,
                            title = mwString("premium.benefit_extractor", "Tách âm thanh từ video"),
                            detail = mwString("premium.benefit_extractor_detail", "Chọn một hoặc nhiều video, tách và nghe thử từng file kết quả.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.GOOGLE_DRIVE,
                            title = mwString("premium.benefit_drive", "Google Drive Music"),
                            detail = mwString("premium.benefit_drive_detail", "Kết nối Drive, duyệt thư mục và phát thư viện nhạc đám mây.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.SOUND_STUDIO,
                            title = mwString("premium.benefit_equalizer", "Sound Studio Pro"),
                            detail = mwString("premium.benefit_equalizer_detail", "EQ nâng cao, preset theo thiết bị, Bass Boost, Loudness và Crossfade.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.ALBUM_STUDIO,
                            title = mwString("premium.benefit_album_studio", "Album Studio Pro"),
                            detail = mwString("premium.benefit_album_studio_detail", "Tải, thay ảnh bìa trực tuyến và mở khóa hiệu ứng album cao cấp.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.SMART_PLAYLIST,
                            title = mwString("premium.benefit_smart_playlist", "Smart Playlist Pro"),
                            detail = mwString("premium.benefit_smart_playlist_detail", "Playlist tự cập nhật, thống kê lượt nghe và phát hiện bài trùng.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.AD_FREE,
                            title = mwString("premium.benefit_ads", "Không quảng cáo"),
                            detail = mwString("premium.benefit_ads_detail", "Ẩn banner trong toàn bộ thời gian Premium hoặc Premium Trial còn hiệu lực.")
                        )
                    }
                }

                Surface(color = colors.purple.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(17.dp))
                        val currentText = when {
                            MeloWarePremiumAccess.canUsePaidFeatures && MeloWarePremiumAccess.accessKind == "paid" ->
                                mwString("premium.active_until", "Premium có hiệu lực đến") + " " + premiumDate(MeloWarePremiumAccess.expiresAtMillis)
                            MeloWarePremiumAccess.canUsePaidFeatures ->
                                mwString("premium.trial_until", "Dùng thử miễn phí đến") + " " + premiumDate(MeloWarePremiumAccess.expiresAtMillis)
                            MeloWaveTrial.isChecking ->
                                mwString("premium.trial_pending", "Đang kiểm tra và kích hoạt Premium dùng thử 30 ngày...")
                            MeloWaveTrial.visibleState == "eligible" ->
                                mwString("premium.guest_eligible", "Premium dùng thử 30 ngày tự kích hoạt khi mở ứng dụng có Internet. Không cần phát nhạc hoặc đăng nhập.")
                            MeloWaveTrial.visibleState == "expired" ->
                                mwString("premium.guest_expired", "Dùng thử Premium 30 ngày đã hết hạn")
                            MeloWaveTrial.visibleState == "disabled" ->
                                mwString("premium.guest_disabled", "Dùng thử đang tạm dừng trên máy chủ.")
                            else -> mwString("premium.guest_unknown", "Đang chờ xác thực Premium dùng thử. Kết nối Internet để ứng dụng tự kích hoạt.")
                        }
                        Text(currentText, color = colors.textPrimary, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }

                Text(mwString("premium.choose_plan_short", "Chọn gói Premium"),
                    color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                // A compact diagnostic immediately above the plans remains visible on short phones.
                if (!billing.loading && billing.priceError != null) {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.border)) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(
                                mwString("premium.price_load_error", "Chưa tải được giá Google Play") +
                                        ": " + billing.priceError.orEmpty(),
                                color = colors.textSecondary, fontSize = 11.sp
                            )
                            TextButton(onClick = { billing.refreshPrices() }) {
                                Text(mwString("premium.retry_short", "Thử lại"),
                                    color = colors.purpleBright, fontSize = 12.sp)
                            }
                        }
                    }
                }
                PremiumPlanCard(
                    title = mwString("premium.plan6_short", "6 tháng"),
                    expectedPrice = mwString("premium.expected1", "Dự kiến 1 USD"),
                    displayOffer = billing.sixMonth,
                    loading = billing.loading,
                    selected = selectedPlan == PREMIUM_6M_BASE_PLAN,
                    onSelect = { selectedPlan = PREMIUM_6M_BASE_PLAN }
                )
                PremiumPlanCard(
                    title = mwString("premium.plan12_short", "12 tháng"),
                    expectedPrice = mwString("premium.expected2", "Dự kiến 2 USD"),
                    displayOffer = billing.twelveMonth,
                    loading = billing.loading,
                    selected = selectedPlan == PREMIUM_12M_BASE_PLAN,
                    onSelect = { selectedPlan = PREMIUM_12M_BASE_PLAN }
                )
                Text(mwString("premium.prepaid", "Trả trước một lần, không tự gia hạn"),
                    color = colors.textSecondary, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth())

                if (!loggedIn) {
                    TextButton(onClick = onRequestGoogleLogin, modifier = Modifier.fillMaxWidth()) {
                        Text(mwString("premium.guest_signin", "Đăng nhập Google để mua gói 6/12 tháng"),
                            color = colors.purpleBright)
                    }
                }
                Button(onClick = { billing.buy(selectedPlan) }, enabled = canPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)) {
                    Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (canPurchase) mwString("premium.buy_now", "Thanh toán qua Google Play")
                    else if (!loggedIn) mwString("premium.guest_login_buy", "Đăng nhập để thanh toán")
                    else mwString("premium.activate_wait", "Chưa thể thanh toán"))
                }
                if (loggedIn && !MeloWarePremiumAccess.checkoutEnabled &&
                    MeloWarePremiumAccess.lastError == null && MeloWarePremiumAccess.checkoutReason.isNotBlank()) {
                    Text(MeloWarePremiumAccess.checkoutReason,
                        color = colors.textSecondary, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = {
                    billing.restorePurchases()
                    billing.refreshEntitlement()
                    MeloWaveTrial.refresh(activity, force = true)
                },
                    enabled = !billing.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(mwString("premium.restore", "Khôi phục giao dịch / Kiểm tra quyền sử dụng"),
                        color = colors.purpleBright)
                }
                if (billing.loading) {
                    Text(mwString("premium.checking", "Đang kiểm tra gói Google Play..."),
                        color = colors.textSecondary, fontSize = 11.sp)
                }
                billing.statusMessage?.let { message ->
                    Text(message, color = colors.textSecondary, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth())
                }
                MeloWaveTrial.lastError?.let { guestError ->
                    Text(guestError, color = colors.textSecondary, fontSize = 11.sp)
                }
                MeloWarePremiumAccess.lastError?.let { error ->
                    Text(mwString("premium.server_unavailable", "Máy chủ xác minh chưa sẵn sàng. Không thu tiền.") +
                            " ($error)",
                        color = colors.textSecondary, fontSize = 11.sp)
                    TextButton(onClick = { billing.refreshEntitlement(); billing.refreshPrices() }) {
                        Text(mwString("premium.retry_short", "Thử lại"), color = colors.purpleBright)
                    }
                }
                Text(mwString("premium.guest_note", "Dùng thử miễn phí tính theo thiết bị. Mua gói trả phí vẫn cần đăng nhập Google để xác minh."),
                    color = colors.textSecondary, fontSize = 11.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun premiumDate(value: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(value))

/** Animated Premium crown and sparkle; isolated from the rest of the dialog. */
@Composable
private fun PremiumHeroBadge() {
    val colors = LocalMusicColors.current
    val transition = rememberInfiniteTransition(label = "premium_hero")
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_pulse"
    )
    Box(
        modifier = Modifier.size(46.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        colors.purple.copy(alpha = 0.26f),
                        colors.blue.copy(alpha = 0.21f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.WorkspacePremium,
            contentDescription = null,
            tint = colors.purpleBright,
            modifier = Modifier.size(28.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = null,
            tint = colors.cyan,
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(3.dp)
                .size(11.dp)
                .graphicsLayer { alpha = 0.55f + (scale - 0.96f) * 3.5f }
        )
    }
}

/** Small Compose-only animations: no external assets, threads, timers or libraries. */
private enum class PremiumBenefitType {
    AUDIO_CONVERTER,
    VIDEO_EXTRACTOR,
    GOOGLE_DRIVE,
    SOUND_STUDIO,
    ALBUM_STUDIO,
    SMART_PLAYLIST,
    AD_FREE
}

@Composable
private fun PremiumBenefitCard(type: PremiumBenefitType, title: String, detail: String) {
    val colors = LocalMusicColors.current
    val accent = when (type) {
        PremiumBenefitType.AUDIO_CONVERTER -> colors.cyan
        PremiumBenefitType.VIDEO_EXTRACTOR -> colors.magenta
        PremiumBenefitType.GOOGLE_DRIVE -> colors.blue
        PremiumBenefitType.SOUND_STUDIO -> colors.purpleBright
        PremiumBenefitType.ALBUM_STUDIO -> colors.magenta
        PremiumBenefitType.SMART_PLAYLIST -> colors.purpleBright
        PremiumBenefitType.AD_FREE -> colors.green
    }
    Surface(
        color = colors.cardRaised.copy(alpha = if (colors.isDark) 0.68f else 0.85f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.7.dp, accent.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumBenefitAnimatedIcon(type = type, accent = accent)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detail,
                    color = colors.textSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PremiumBenefitAnimatedIcon(type: PremiumBenefitType, accent: Color) {
    // Only the compact icon animates; billing and the rest of the dialog remain stable.
    val transition = rememberInfiniteTransition(label = "premium_icon_${type.name}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (type) {
                    PremiumBenefitType.AUDIO_CONVERTER -> 1750
                    PremiumBenefitType.VIDEO_EXTRACTOR -> 1950
                    PremiumBenefitType.GOOGLE_DRIVE -> 2250
                    PremiumBenefitType.SOUND_STUDIO -> 1550
                    PremiumBenefitType.ALBUM_STUDIO -> 1850
                    PremiumBenefitType.SMART_PLAYLIST -> 2050
                    PremiumBenefitType.AD_FREE -> 2800
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "premium_icon_progress"
    )
    val drift = sin(phase * 2f * PI.toFloat())
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                brush = Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.08f))
                ),
                shape = RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            PremiumBenefitType.AUDIO_CONVERTER -> {
                Icon(
                    Icons.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(31.dp).graphicsLayer {
                        translationX = drift * 2.5f
                    }
                )
            }
            PremiumBenefitType.VIDEO_EXTRACTOR -> {
                Icon(
                    Icons.Rounded.VideoFile,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp).graphicsLayer {
                        scaleX = 1f + drift * 0.035f
                        scaleY = 1f + drift * 0.035f
                    }
                )
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp).size(16.dp)
                )
            }
            PremiumBenefitType.GOOGLE_DRIVE -> {
                Icon(
                    Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp).graphicsLayer {
                        translationY = drift * 3.5f
                        scaleX = 1f + drift * 0.035f
                        scaleY = 1f + drift * 0.035f
                    }
                )
            }
            PremiumBenefitType.SOUND_STUDIO -> {
                Canvas(modifier = Modifier.size(29.dp)) {
                    val count = 5
                    val barWidth = size.width / 8.2f
                    val spacing = (size.width - count * barWidth) / (count - 1)
                    for (index in 0 until count) {
                        val wave = (sin((phase + index * 0.18f) * 2f * PI.toFloat()) + 1f) / 2f
                        val barHeight = size.height * (0.27f + wave * 0.65f)
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(index * (barWidth + spacing), (size.height - barHeight) / 2f),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f)
                        )
                    }
                }
            }
            PremiumBenefitType.ALBUM_STUDIO -> {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(29.dp).graphicsLayer {
                        scaleX = 1f + drift * 0.04f
                        scaleY = 1f + drift * 0.04f
                    }
                )
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(15.dp)
                        .graphicsLayer { rotationZ = phase * 45f }
                )
            }
            PremiumBenefitType.SMART_PLAYLIST -> {
                Icon(
                    Icons.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(30.dp).graphicsLayer {
                        scaleX = 1f + drift * 0.045f
                        scaleY = 1f + drift * 0.045f
                    }
                )
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(15.dp)
                        .graphicsLayer { rotationZ = phase * 30f }
                )
            }
            PremiumBenefitType.AD_FREE -> {
                Icon(
                    Icons.Rounded.Block,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(29.dp).graphicsLayer {
                        rotationZ = drift * 6f
                        scaleX = 1f + drift * 0.04f
                        scaleY = 1f + drift * 0.04f
                    }
                )
            }
        }
    }
}

@Composable
private fun PremiumPlanCard(
    title: String,
    expectedPrice: String,
    displayOffer: PremiumDisplayOffer?,
    loading: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        color = if (selected) colors.purple.copy(alpha = 0.10f) else colors.backgroundSoft,
        border = BorderStroke(1.dp, if (selected) colors.purpleBright else colors.border),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Icon(
                if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) colors.purpleBright else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = colors.textPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    if (loading) mwString("premium.checking", "Đang kiểm tra gói")
                    else if (displayOffer == null) mwString("premium.price_load_error", "Chưa tải được giá Google Play")
                    else if (MeloWarePremiumAccess.checkoutEnabled)
                        mwString("premium.ready_checkout", "Sẵn sàng thanh toán qua Google Play")
                    else mwString("premium.ready_but_locked", "Đã tìm thấy gói · chờ hoàn thiện thanh toán"),
                    color = colors.textSecondary, fontSize = 10.sp
                )
            }
            Text(
                displayOffer?.formattedPrice?.takeIf { it.isNotBlank() } ?: expectedPrice,
                color = colors.purpleBright, fontWeight = FontWeight.Bold,
                fontSize = 13.sp, textAlign = TextAlign.End
            )
        }
    }
}

// Guest trial is kept in this existing Kotlin source file. No separate MeloWaveTrial.kt.
/**
 * Anonymous Premium trial (30 days/device).
 * PHP/MySQL decides start/expiry; Android NEVER extends or calculates a new start date.
 * Does not touch paid Google Play receipts or the existing Google-sign-in flow.
 * An unmodified Android client cannot prove that a physical device is unique;
 * ANDROID_ID is a best-effort anti-repeat key, not Play Integrity attestation.
 */
internal object MeloWaveTrial {
    private const val ENDPOINT = "https://tunertools.top/melowave/melowave_trial.php"
    private const val STATUS_REFRESH_MS = 5 * 60 * 1000L
    private const val START_RETRY_MS = 30 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestMutex = Mutex()
    private val throttleLock = Any()
    private var lastStatusAttemptElapsed = -STATUS_REFRESH_MS
    private var lastStartAttemptElapsed = -START_RETRY_MS

    var state by mutableStateOf("unknown")
        private set
    var expiresAtMillis by mutableStateOf(0L)
        private set
    var startedAtMillis by mutableStateOf(0L)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set
    var isChecking by mutableStateOf(false)
        private set
    var enforceFeatures by mutableStateOf(true)
        private set

    private var validatedAtElapsed by mutableStateOf(0L)
    private var remainingAtValidationMillis by mutableStateOf(0L)
    // Advances display/feature recomposition when a trial expires while the app remains open.
    private var timeTick by mutableIntStateOf(0)

    /** Uses elapsedRealtime: changing the device wall-clock cannot extend an active session. */
    val remainingMillis: Long get() {
        @Suppress("UNUSED_VARIABLE") val dependency = timeTick
        if (state != "active" || validatedAtElapsed <= 0L) return 0L
        val elapsed = (SystemClock.elapsedRealtime() - validatedAtElapsed).coerceAtLeast(0L)
        return (remainingAtValidationMillis - elapsed).coerceAtLeast(0L)
    }
    val isActive: Boolean get() = state == "active" && remainingMillis > 0L
    val visibleState: String get() = if (state == "active" && !isActive) "expired" else state

    fun tick() { timeTick++ }

    private fun query(context: Context, action: String): JSONObject {
        val appContext = context.applicationContext
        val id = Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
            ?.lowercase(Locale.ROOT)?.trim().orEmpty()
        require(Regex("^[0-9a-f]{16,32}$").matches(id)) {
            "Thiết bị không cung cấp mã Android hợp lệ."
        }
        val payload = JSONObject()
            .put("action", action)
            .put("androidId", id)
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 7_000
            connection.readTimeout = 12_000
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { stream ->
                stream.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val statusCode = connection.responseCode
            val input = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val responseText = input?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(responseText) }.getOrNull()
                ?: throw IllegalStateException("Máy chủ dùng thử trả dữ liệu không hợp lệ (HTTP $statusCode)")
            if (statusCode !in 200..299 || !response.optBoolean("ok", false)) {
                throw IllegalStateException(
                    response.optString("message", "Không thể kiểm tra dùng thử (HTTP $statusCode)")
                )
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun updateFromServer(result: JSONObject) {
        val newState = result.optString("trialState", "unknown")
        if (newState !in setOf("eligible", "active", "expired", "disabled")) {
            throw IllegalStateException("Máy chủ dùng thử trả trạng thái không hợp lệ")
        }
        val expiry = result.optLong("expiresAtMillis", 0L).coerceAtLeast(0L)
        val serverNow = result.optLong("serverTimeMillis", 0L)
        if (serverNow <= 0L) throw IllegalStateException("Thiếu thời gian máy chủ")
        startedAtMillis = result.optLong("startedAtMillis", 0L).coerceAtLeast(0L)
        expiresAtMillis = expiry
        remainingAtValidationMillis = (expiry - serverNow).coerceAtLeast(0L)
        validatedAtElapsed = SystemClock.elapsedRealtime()
        enforceFeatures = result.optBoolean("enforceFeatures", true)
        state = newState
        lastError = null
    }

    /**
     * Called as soon as MeloWave opens, WITHOUT waiting for a song to play.
     * The server remains authoritative for trial eligibility, start date and expiry.
     * First check status so an active/expired/disabled trial is never restarted.
     * A server-eligible trial is then activated automatically during this request.
     */
    fun refresh(context: Context, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        // On a first launch/network failure, retry relatively quickly; a validated
        // active or expired state only needs the usual five-minute status refresh.
        val interval = if (state == "unknown" || state == "eligible") START_RETRY_MS else STATUS_REFRESH_MS
        synchronized(throttleLock) {
            if (!force && now - lastStatusAttemptElapsed < interval) return
            lastStatusAttemptElapsed = now
        }
        val appContext = context.applicationContext
        scope.launch {
            requestMutex.withLock {
                withContext(Dispatchers.Main.immediate) { isChecking = true }
                try {
                    val status = runCatching { query(appContext, "status") }
                    val eligibleForStart = withContext(Dispatchers.Main.immediate) {
                        status.mapCatching { updateFromServer(it) }.fold(
                            onSuccess = {
                                state == "eligible" &&
                                    !(MeloWarePremiumAccess.accessKind == "paid" &&
                                        MeloWarePremiumAccess.canUsePaidFeatures)
                            },
                            onFailure = { error ->
                                lastError = "Chưa thể xác thực gói dùng thử. Hãy kết nối Internet; MeloWave sẽ tự thử lại."
                                Log.w("MeloWaveTrial", "Trial status request failed (${error.javaClass.simpleName})")
                                false
                            }
                        )
                    }
                    if (!eligibleForStart) return@withLock
                    // Prevent multiple activation requests if the user quickly resumes
                    // the Activity or repeatedly presses the status refresh button.
                    val shouldStart = synchronized(throttleLock) {
                        val time = SystemClock.elapsedRealtime()
                        if (time - lastStartAttemptElapsed < START_RETRY_MS) false
                        else {
                            lastStartAttemptElapsed = time
                            true
                        }
                    }
                    if (!shouldStart ||
                        (MeloWarePremiumAccess.accessKind == "paid" &&
                            MeloWarePremiumAccess.canUsePaidFeatures)) return@withLock

                    // The PHP endpoint already supports 'start' with androidId only;
                    // audio playback information is not needed for activation.
                    val activation = runCatching { query(appContext, "start") }
                    withContext(Dispatchers.Main.immediate) {
                        activation.mapCatching { updateFromServer(it) }.fold(
                            onSuccess = {
                                if (state == "eligible") {
                                    lastError = "Máy chủ chưa kích hoạt được Premium dùng thử. MeloWave sẽ tự thử lại."
                                }
                            },
                            onFailure = { error ->
                                lastError = "Chưa thể kích hoạt Premium dùng thử. Hãy kết nối Internet; MeloWave sẽ tự thử lại."
                                Log.w("MeloWaveTrial", "Trial start request failed (${error.javaClass.simpleName})")
                            }
                        )
                    }
                } finally {
                    withContext(Dispatchers.Main.immediate) { isChecking = false }
                }
            }
        }
    }
}
// endregion Merged source: MeloWarePremium.kt
