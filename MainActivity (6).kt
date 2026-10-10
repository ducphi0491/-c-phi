package duc_phi.music

import android.Manifest
import android.app.Activity
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val controller = remember {
                MusicController(context.applicationContext)
            }
            var showSettings by remember { mutableStateOf(false) }

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

            val drivePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val uri = data?.data
                    if (uri != null) {
                        controller.attachPersonalDrive(uri, data.flags)
                    }
                }
            }

            val requestLocalScan: () -> Unit = {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                ) {
                    controller.scanLocalMusic()
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

            TunertoolsTheme {
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
                            onPickPersonalDrive = pickPersonalDrive,
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
                        onScanLocal = requestLocalScan,
                        onPickDrive = pickPersonalDrive,
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickSettingsDialog(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onPickDrive: () -> Unit,
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
                        "Cài đặt nhanh",
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
                    subtitle = controller.personalDriveName.ifBlank { "Chưa kết nối thư mục Google Drive" },
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
                    title = "Tunertools",
                    subtitle = "Nghe nhạc Offline và Google Drive",
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

/**
 * Giữ cùng tên lớp với dự án cũ để AndroidManifest hiện tại không bị thiếu receiver
 * sau khi gom code về đúng 6 file Kotlin.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tunertools_music_reminder"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Nhắc nghe nhạc",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        builder
            .setSmallIcon(context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_media_play)
            .setContentTitle("Tunertools")
            .setContentText("Mở nhạc Offline hoặc Google Drive và thư giãn một chút nhé.")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        runCatching { manager.notify(2407, builder.build()) }
    }
}
