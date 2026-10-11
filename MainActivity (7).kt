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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

private data class AppLanguageOption(
    val code: String,
    val nativeName: String
)

private val AppLanguageOptions = listOf(
    AppLanguageOption("vi", "Tiếng Việt"),
    AppLanguageOption("en", "English"),
    AppLanguageOption("zh", "简体中文"),
    AppLanguageOption("ja", "日本語"),
    AppLanguageOption("ko", "한국어"),
    AppLanguageOption("es", "Español"),
    AppLanguageOption("fr", "Français"),
    AppLanguageOption("de", "Deutsch"),
    AppLanguageOption("ru", "Русский"),
    AppLanguageOption("pt", "Português"),
    AppLanguageOption("id", "Bahasa Indonesia"),
    AppLanguageOption("hi", "हिन्दी"),
    AppLanguageOption("ar", "العربية"),
    AppLanguageOption("th", "ไทย"),
    AppLanguageOption("it", "Italiano")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val prefs = remember {
                context.getSharedPreferences("music_app_prefs", Context.MODE_PRIVATE)
            }
            val controller = remember {
                MusicController(context.applicationContext)
            }

            var language by remember {
                mutableStateOf(
                    prefs.getString("app_language", null)
                        ?: java.util.Locale.getDefault().language.takeIf { code ->
                            AppLanguageOptions.any { it.code == code }
                        }
                        ?: "vi"
                )
            }
            // 0: theo hệ thống, 1: sáng, 2: tối
            var themeMode by remember {
                mutableIntStateOf(prefs.getInt("app_theme_mode", 2))
            }
            var showSettings by remember { mutableStateOf(false) }
            var showLanguageDialog by remember { mutableStateOf(false) }
            var showThemeDialog by remember { mutableStateOf(false) }

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

            val drivePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val resultIntent = result.data
                    val treeUri = resultIntent?.data
                    if (treeUri != null) {
                        controller.attachPersonalDrive(treeUri, resultIntent.flags)
                    }
                }
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

            val pickPersonalDrive: () -> Unit = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                        controller.personalDriveUriText.isNotBlank()
                    ) {
                        runCatching {
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                Uri.parse(controller.personalDriveUriText)
                            )
                        }
                    }
                }
                drivePickerLauncher.launch(intent)
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
                onDispose { controller.release() }
            }

            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }

            TunertoolsTheme(isDark = isDark, language = language) {
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
                            onScanLocal = requestLocalScan,
                            onSettings = { showSettings = true }
                        )

                        MusicScreen.LIBRARY -> LibraryScreen(
                            controller = controller,
                            onScanLocal = requestLocalScan,
                            onPickPersonalDrive = pickPersonalDrive,
                            onSettings = { showSettings = true }
                        )

                        MusicScreen.NOW_PLAYING -> NowPlayingScreen(controller)
                    }
                }

                if (showSettings) {
                    QuickSettingsDialog(
                        controller = controller,
                        language = language,
                        themeMode = themeMode,
                        onScanLocal = requestLocalScan,
                        onPickDrive = pickPersonalDrive,
                        onOpenLanguage = {
                            showSettings = false
                            showLanguageDialog = true
                        },
                        onOpenTheme = {
                            showSettings = false
                            showThemeDialog = true
                        },
                        onDismiss = { showSettings = false }
                    )
                }

                if (showLanguageDialog) {
                    LanguageDialog(
                        selectedCode = language,
                        onSelect = { code ->
                            language = code
                            prefs.edit().putString("app_language", code).apply()
                            showLanguageDialog = false
                        },
                        onDismiss = { showLanguageDialog = false }
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
            }
        }
    }
}

@Composable
private fun QuickSettingsDialog(
    controller: MusicController,
    language: String,
    themeMode: Int,
    onScanLocal: () -> Unit,
    onPickDrive: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTheme: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cài đặt",
                        color = colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }

                SettingRow(
                    icon = Icons.Rounded.Language,
                    title = "Ngôn ngữ",
                    subtitle = AppLanguageOptions.firstOrNull { it.code == language }?.nativeName
                        ?: language,
                    onClick = onOpenLanguage
                )

                SettingRow(
                    icon = Icons.Rounded.SettingsBrightness,
                    title = "Chế độ giao diện",
                    subtitle = when (themeMode) {
                        1 -> "Sáng"
                        2 -> "Tối"
                        else -> "Theo hệ thống"
                    },
                    onClick = onOpenTheme
                )

                SettingRow(
                    icon = Icons.Rounded.Cached,
                    title = "Quét lại nhạc trên máy",
                    subtitle = "Cập nhật các bài vừa thêm hoặc vừa xóa",
                    onClick = {
                        onScanLocal()
                        onDismiss()
                    }
                )

                SettingRow(
                    icon = Icons.Rounded.MusicNote,
                    title = "Chọn lại Drive của tôi",
                    subtitle = controller.personalDriveName.ifBlank {
                        "Chưa kết nối thư mục Google Drive"
                    },
                    onClick = {
                        onPickDrive()
                        onDismiss()
                    }
                )

                if (controller.personalDriveUriText.isNotBlank()) {
                    SettingRow(
                        icon = Icons.Rounded.CloudOff,
                        title = "Ngắt Drive cá nhân",
                        subtitle = "Không xóa file thật trên Google Drive",
                        onClick = {
                            controller.disconnectPersonalDrive()
                            onDismiss()
                        }
                    )
                }

                SettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "Xóa cache Google Drive",
                    subtitle = "Giải phóng file tạm, không xóa nhạc gốc",
                    onClick = {
                        controller.clearDriveCache()
                        onDismiss()
                    }
                )

                SettingRow(
                    icon = Icons.Rounded.Info,
                    title = "MeloWare",
                    subtitle = "Nghe nhạc Offline và Google Drive",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = colors.purple.copy(alpha = 0.13f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = colors.purpleBright)
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LanguageDialog(
    selectedCode: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
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
                        "Ngôn ngữ",
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    items(AppLanguageOptions.size) { index ->
                        val item = AppLanguageOptions[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(item.code) }
                                .padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                item.nativeName,
                                color = if (item.code == selectedCode) {
                                    colors.purpleBright
                                } else {
                                    colors.textPrimary
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (item.code == selectedCode) {
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

@Composable
private fun ThemeModeDialog(
    selectedMode: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val options = listOf(
        Triple(2, Icons.Rounded.DarkMode, "Chế độ tối"),
        Triple(1, Icons.Rounded.LightMode, "Chế độ sáng"),
        Triple(0, Icons.Rounded.SettingsBrightness, "Theo hệ thống")
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
                        "Chế độ giao diện",
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
                        subtitle = if (mode == selectedMode) "Đang sử dụng" else "",
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
