package com.docflowoffline

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await



// One file for display preferences, Firebase Google account and the Settings tab.
// ==================== PREFERENCES AND LANGUAGE ====================
/** Only non-sensitive UI preferences are saved on the device. */
enum class DocLanguage(val key: String) { VI("vi"), EN("en") }
enum class DocAppearance(val key: String) { SYSTEM("system"), LIGHT("light"), DARK("dark") }

internal class DocFlowPreferences(context: Context) {
    private val storage = context.applicationContext.getSharedPreferences("docflow_display", Context.MODE_PRIVATE)

    fun language(): DocLanguage = DocLanguage.entries.firstOrNull {
        it.key == storage.getString("language", "vi")
    } ?: DocLanguage.VI

    fun appearance(): DocAppearance = DocAppearance.entries.firstOrNull {
        it.key == storage.getString("appearance", "system")
    } ?: DocAppearance.SYSTEM

    fun setLanguage(language: DocLanguage) {
        storage.edit().putString("language", language.key).apply()
    }

    fun setAppearance(appearance: DocAppearance) {
        storage.edit().putString("appearance", appearance.key).apply()
    }
}

internal val LocalDocLanguage = compositionLocalOf { DocLanguage.VI }
internal val LocalDocIsDark = compositionLocalOf { false }

/** UI strings in the entry screens; document editors retain their existing source text. */
@Composable
internal fun dfText(vi: String, en: String): String =
    if (LocalDocLanguage.current == DocLanguage.EN) en else vi

// ==================== FIREBASE AUTHENTICATION ====================
/**
 * Google -> Firebase Authentication. No passwords, raw ID tokens, or login histories are persisted.
 * The admin email check is ONLY for hiding/showing UI: Firebase Rules/backend must enforce access.
 * This class deliberately does not read other users' records.
 */
internal object DocFlowAccount {
    const val ADMIN_EMAIL = "ducphi0491@gmail.com"

    fun firebaseAuthOrNull(context: Context): FirebaseAuth? {
        return runCatching {
            if (FirebaseApp.getApps(context).isEmpty() && FirebaseApp.initializeApp(context) == null) {
                null
            } else FirebaseAuth.getInstance()
        }.getOrNull()
    }

    private fun webClientId(context: Context): String? {
        val resource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resource == 0) return null
        return context.getString(resource).takeIf { it.isNotBlank() }
    }

    fun isConfigured(context: Context): Boolean =
        firebaseAuthOrNull(context) != null && webClientId(context) != null

    fun isAdminUi(user: FirebaseUser?): Boolean =
        user?.isEmailVerified == true && user.email?.equals(ADMIN_EMAIL, ignoreCase = true) == true

    suspend fun signIn(activity: Activity): FirebaseUser {
        val firebase = firebaseAuthOrNull(activity)
            ?: error("Firebase chưa được cấu hình cho ứng dụng này.")
        val clientId = webClientId(activity)
            ?: error("Thiếu Web Client ID. Hãy thêm google-services.json và bật Google Sign-In trong Firebase.")

        val option = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(
            context = activity,
            request = request
        )
        val googleToken = (result.credential as? CustomCredential)
            ?.takeIf { it.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL }
            ?.let { GoogleIdTokenCredential.createFrom(it.data).idToken }
            ?: error("Google không trả về thông tin đăng nhập hợp lệ.")
        // Firebase verifies the Google identity before accepting the session.
        val authResult = firebase.signInWithCredential(
            GoogleAuthProvider.getCredential(googleToken, null)
        ).await()
        return authResult.user ?: error("Firebase không cung cấp phiên đăng nhập.")
    }

    suspend fun signOut(activity: Activity) {
        firebaseAuthOrNull(activity)?.signOut()
        // Prevent a previously selected account from being silently reused.
        runCatching {
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}

// ==================== INFORMATION AND SETTINGS PAGE ====================
/** Information tab; no billing and no cross-user Firebase database reads in this phase. */
@Composable
internal fun DocFlowSettingsScreen(
    language: DocLanguage,
    changeLanguage: (DocLanguage) -> Unit,
    appearance: DocAppearance,
    changeAppearance: (DocAppearance) -> Unit,
    firebaseUser: FirebaseUser?,
    firebaseConfigured: Boolean,
    signInBusy: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    var languageDialog by remember { mutableStateOf(false) }
    var appearanceDialog by remember { mutableStateOf(false) }
    var premiumDialog by remember { mutableStateOf(false) }
    var adminDialog by remember { mutableStateOf(false) }

    val english = language == DocLanguage.EN
    fun t(vi: String, en: String): String = if (english) en else vi

    if (languageDialog) {
        SettingsSelectionDialog(
            title = t("Chọn ngôn ngữ", "Choose language"),
            choices = listOf("Tiếng Việt", "English"),
            selected = if (language == DocLanguage.VI) 0 else 1,
            dismiss = { languageDialog = false },
            choose = {
                changeLanguage(if (it == 0) DocLanguage.VI else DocLanguage.EN)
                languageDialog = false
            }
        )
    }
    if (appearanceDialog) {
        SettingsSelectionDialog(
            title = t("Chọn giao diện", "Appearance"),
            choices = listOf(
                t("Theo hệ thống", "Use device setting"),
                t("Chế độ sáng", "Light"),
                t("Chế độ tối", "Dark")
            ),
            selected = when (appearance) {
                DocAppearance.SYSTEM -> 0
                DocAppearance.LIGHT -> 1
                DocAppearance.DARK -> 2
            },
            dismiss = { appearanceDialog = false },
            choose = {
                changeAppearance(DocAppearance.entries[it])
                appearanceDialog = false
            }
        )
    }
    if (premiumDialog) {
        AlertDialog(
            onDismissRequest = { premiumDialog = false },
            icon = { Icon(Icons.Default.WorkspacePremium, null) },
            title = { Text(t("DocFlow Premium", "DocFlow Premium")) },
            text = {
                Text(t(
                    "Giao diện Premium đã được chuẩn bị. Chưa tích hợp Google Play Billing, chưa có gói bán và chưa phát sinh thanh toán.",
                    "The Premium page is ready. Google Play Billing and paid plans are not connected yet; no payment will occur."
                ))
            },
            confirmButton = {
                TextButton(onClick = { premiumDialog = false }) { Text(t("Đóng", "Close")) }
            }
        )
    }
    if (adminDialog) {
        AlertDialog(
            onDismissRequest = { adminDialog = false },
            icon = { Icon(Icons.Default.AdminPanelSettings, null) },
            title = { Text(t("Quản lý tài khoản Firebase", "Firebase account management")) },
            text = {
                Text(t(
                    "Đã chuẩn bị khu vực dành cho tài khoản quản trị. Bước tiếp theo sẽ kết nối dữ liệu hồ sơ/nhật ký đăng nhập được phép xem. Hiện chưa đọc hoặc lưu dữ liệu của người dùng khác. Quyền đọc phải được bảo vệ bằng Firebase Security Rules và custom claim admin trên máy chủ.",
                    "The admin area is ready. The next step will connect approved user profiles/sign-in records. No other users' data is read or saved yet. Access must be enforced by Firebase Security Rules and a server-issued admin custom claim."
                ))
            },
            confirmButton = {
                TextButton(onClick = { adminDialog = false }) { Text(t("Đã hiểu", "Got it")) }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Box(
                    Modifier.size(56.dp).background(
                        MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(18.dp)
                    ), contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(29.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(t("Thông tin & cài đặt", "Information & settings"),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text(t("Tùy chỉnh trải nghiệm DocFlow", "Make DocFlow yours"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { SettingsHeading(t("Tài khoản", "Account")) }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(46.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(if (firebaseUser == null) Icons.Default.PersonOutline else Icons.Default.Person,
                                null, modifier = Modifier.size(25.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                                ?: t("Chưa đăng nhập", "Not signed in"),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(firebaseUser?.email ?: t("Đăng nhập bằng tài khoản Google", "Sign in with Google"),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (DocFlowAccount.isAdminUi(firebaseUser)) {
                            Icon(Icons.Default.VerifiedUser, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (firebaseUser == null) {
                        Button(onClick = onSignIn, enabled = !signInBusy, modifier = Modifier.fillMaxWidth()) {
                            if (signInBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Login, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(t("Đăng nhập bằng Google", "Continue with Google"))
                        }
                        if (!firebaseConfigured) {
                            Text(t(
                                "Cần bổ sung cấu hình Firebase để kích hoạt đăng nhập.",
                                "Firebase configuration is required to enable sign-in."
                            ), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        OutlinedButton(onClick = onSignOut, enabled = !signInBusy,
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(t("Đăng xuất", "Sign out"))
                        }
                    }
                }
            }
        }
        if (DocFlowAccount.isAdminUi(firebaseUser)) {
            item {
                SettingsEntry(
                    title = t("Quản trị Firebase", "Firebase admin"),
                    detail = t("Quản lý dữ liệu đăng nhập (bước kế tiếp)", "Sign-in records (next phase)"),
                    icon = Icons.Default.AdminPanelSettings,
                    onClick = { adminDialog = true },
                    accent = MaterialTheme.colorScheme.primary
                )
            }
        }
        item { SettingsHeading(t("Cài đặt", "Preferences")) }
        item {
            SettingsEntry(
                title = t("Ngôn ngữ", "Language"),
                detail = if (language == DocLanguage.VI) "Tiếng Việt" else "English",
                icon = Icons.Default.Language,
                onClick = { languageDialog = true }
            )
        }
        item {
            val mode = when (appearance) {
                DocAppearance.SYSTEM -> t("Theo hệ thống", "Follow system")
                DocAppearance.LIGHT -> t("Sáng", "Light")
                DocAppearance.DARK -> t("Tối", "Dark")
            }
            SettingsEntry(
                title = t("Giao diện", "Appearance"),
                detail = mode,
                icon = if (appearance == DocAppearance.DARK) Icons.Default.DarkMode else Icons.Default.Palette,
                onClick = { appearanceDialog = true }
            )
        }
        item {
            SettingsEntry(
                title = "Premium",
                detail = t("Khám phá tính năng · chưa mở thanh toán", "Explore features · billing not yet enabled"),
                icon = Icons.Default.WorkspacePremium,
                onClick = { premiumDialog = true }
            )
        }
        item { SettingsHeading(t("Về ứng dụng", "About the app")) }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("DocFlow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(t("Đọc, chỉnh sửa và chuyển đổi tài liệu trên thiết bị.",
                        "View, edit and convert documents on your device."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("DOCX · XLSX · PPTX · PDF · TXT · CSV",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(t("Xử lý tài liệu vẫn hoạt động offline; chỉ đăng nhập mới cần Internet.",
                        "Document editing stays offline; only sign-in needs Internet."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp))
}

@Composable
private fun SettingsEntry(
    title: String,
    detail: String,
    icon: ImageVector,
    onClick: () -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(43.dp).background(
                accent.copy(alpha = .12f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(23.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsSelectionDialog(
    title: String,
    choices: List<String>,
    selected: Int,
    dismiss: () -> Unit,
    choose: (Int) -> Unit
) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(title) },
        text = {
            Column {
                choices.forEachIndexed { index, text ->
                    Surface(onClick = { choose(index) }, color = Color.Transparent) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                            RadioButton(selected = index == selected, onClick = null)
                            Text(text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) { Text(dfText("Đóng", "Close")) }
        }
    )
}
