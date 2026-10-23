package com.docflowoffline

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.net.URL
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.viewinterop.AndroidView
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.*
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

    fun hasUsageConsent(uid: String): Boolean = storage.getBoolean("usage_consent_$uid", false)
    fun setUsageConsent(uid: String, allowed: Boolean) {
        storage.edit().putBoolean("usage_consent_$uid", allowed).apply()
    }
    fun lastProfileRefresh(uid: String): Long = storage.getLong("profile_refresh_$uid", 0L)
    fun setLastProfileRefresh(uid: String, time: Long) {
        storage.edit().putLong("profile_refresh_$uid", time).apply()
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
 * Google Credential Manager -> Firebase Auth -> Cloud Firestore.
 * Collects only UID, verified email, public display name and sign-in timestamps.
 * A per-user record is written only after the visitor confirms the disclosure
 * and finishes an interactive Google login. No passwords, tokens, document
 * content, IP addresses or device identifiers are captured here.
 *
 * The email test below only affects the UI. Firestore Security Rules MUST
 * authorize admin list access on the Firebase server.
 */
internal object DocFlowAccount {
    const val ADMIN_EMAIL = "ducphi0491@gmail.com"
    private const val USER_COLLECTION = "docflow_users"

    var profileSaved by mutableStateOf(false)
        private set
    var profileError by mutableStateOf<String?>(null)
        private set

    data class UserProfile(
        val uid: String,
        val email: String,
        val name: String,
        val createdAt: Timestamp?,
        val lastLoginAt: Timestamp?
    )

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

    fun hasUsageConsent(context: Context, uid: String): Boolean =
        DocFlowPreferences(context).hasUsageConsent(uid)

    fun setUsageConsent(context: Context, uid: String, allowed: Boolean) =
        DocFlowPreferences(context).setUsageConsent(uid, allowed)

    /** Refresh just the allowed profile fields, using server-side timestamps. */
    suspend fun saveOwnProfile(user: FirebaseUser) {
        val email = user.email?.takeIf { it.isNotBlank() }
            ?: error("Tài khoản Google không cung cấp địa chỉ email.")
        if (!user.isEmailVerified) error("Email Google chưa được xác minh.")

        profileSaved = false
        profileError = null
        try {
            val database = FirebaseFirestore.getInstance()
            val ref = database.collection(USER_COLLECTION).document(user.uid)
            // Transaction preserves the original createdAt for returning users.
            database.runTransaction { tx ->
                val existing = tx.get(ref)
                val record = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "email" to email,
                    "displayName" to user.displayName.orEmpty().take(120),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )
                if (!existing.exists()) {
                    record["createdAt"] = FieldValue.serverTimestamp()
                }
                tx.set(ref, record, SetOptions.merge())
                true
            }.await()
            profileSaved = true
        } catch (e: Exception) {
            profileError = e.localizedMessage ?: e.javaClass.simpleName
            throw e
        }
    }

    suspend fun signIn(activity: Activity): FirebaseUser {
        val firebase = firebaseAuthOrNull(activity)
            ?: error("Firebase chưa được cấu hình cho ứng dụng này.")
        val clientId = webClientId(activity)
            ?: error("Thiếu Web Client ID. Hãy thêm google-services.json và bật Google Sign-In trong Firebase.")

        profileSaved = false
        profileError = null
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
        val authResult = firebase.signInWithCredential(
            GoogleAuthProvider.getCredential(googleToken, null)
        ).await()
        val signedIn = authResult.user ?: error("Firebase không cung cấp phiên đăng nhập.")

        // MainActivity applies the mandatory DocFlow data policy immediately after login.
        return signedIn
    }

    // All admin queries are server-side restricted by Firestore Rules, not by this email UI check.
    data class UsageSession(
        val uid: String,
        val deviceModel: String,
        val androidVersion: String,
        val startedAt: Timestamp,
        val lastSeenAt: Timestamp?,
        val endedAt: Timestamp?,
        val durationSeconds: Long
    )

    data class AdminSnapshot(
        val users: List<UserProfile>,
        val sessions: List<UsageSession>,
        val usersTruncated: Boolean,
        val sessionsTruncated: Boolean,
        val earliestSessionDate: LocalDate,
        val sessionsError: String?
    )

    data class UserUsageDetail(
        val sessions: List<UsageSession>,
        val truncated: Boolean
    )

    private fun sessionFromDocument(doc: DocumentSnapshot): UsageSession? {
        val whenStarted = doc.getTimestamp("startedAt") ?: return null
        return UsageSession(
            uid = doc.getString("uid").orEmpty(),
            deviceModel = doc.getString("deviceModel").orEmpty(),
            androidVersion = doc.getString("androidVersion").orEmpty(),
            startedAt = whenStarted,
            lastSeenAt = doc.getTimestamp("lastSeenAt"),
            endedAt = doc.getTimestamp("endedAt"),
            durationSeconds = (doc.getLong("durationSeconds") ?: 0L).coerceAtLeast(0L)
        )
    }

    suspend fun loadAdminDashboard(user: FirebaseUser): AdminSnapshot {
        if (!isAdminUi(user)) error("Tài khoản này không có quyền quản trị.")
        val database = FirebaseFirestore.getInstance()
        val userDocs = mutableListOf<DocumentSnapshot>()
        var userCursor: DocumentSnapshot? = null
        // Capped scans avoid unexpectedly exhausting the Spark free tier.
        for (pageNumber in 0 until 10) {
            var query = database.collection("docflow_users")
                .orderBy("lastLoginAt", Query.Direction.DESCENDING)
                .limit(100)
            if (userCursor != null) query = query.startAfter(userCursor!!)
            val page = query.get(Source.SERVER).await()
            userDocs.addAll(page.documents)
            userCursor = page.documents.lastOrNull()
            if (page.size() < 100) break
        }
        val profiles = userDocs.take(1000).map { doc ->
            UserProfile(
                uid = doc.getString("uid") ?: doc.id,
                email = doc.getString("email").orEmpty(),
                name = doc.getString("displayName").orEmpty(),
                createdAt = doc.getTimestamp("createdAt"),
                lastLoginAt = doc.getTimestamp("lastLoginAt")
            )
        }

        val fromDate = LocalDate.now(DOCFLOW_TIME_ZONE)
            .withDayOfMonth(1).minusMonths(2)
        val since = Timestamp(fromDate.atStartOfDay(DOCFLOW_TIME_ZONE).toInstant().let {
            java.util.Date.from(it)
        })
        val sessionDocs = mutableListOf<DocumentSnapshot>()
        var sessionCursor: DocumentSnapshot? = null
        var sessionsError: String? = null
        try {
            for (pageNumber in 0 until 15) {
                var query = database.collection("docflow_sessions")
                    .whereGreaterThanOrEqualTo("startedAt", since)
                    .orderBy("startedAt", Query.Direction.DESCENDING)
                    .limit(100)
                if (sessionCursor != null) query = query.startAfter(sessionCursor!!)
                val page = query.get(Source.SERVER).await()
                sessionDocs.addAll(page.documents)
                sessionCursor = page.documents.lastOrNull()
                if (page.size() < 100) break
            }
        } catch (error: Exception) {
            sessionsError = error.localizedMessage ?: error.javaClass.simpleName
            Log.w("DocFlowAdmin", "Session query failed; user profiles remain available", error)
        }
        val sessions = sessionDocs.take(1500).mapNotNull(::sessionFromDocument)
        return AdminSnapshot(
            users = profiles,
            sessions = sessions,
            usersTruncated = userDocs.size >= 1000,
            sessionsTruncated = sessionDocs.size >= 1500,
            earliestSessionDate = fromDate,
            sessionsError = sessionsError
        )
    }

    /**
     * Loads the complete stored history for one UID in pages of 100 records.
     * Each page respects the Firestore rule request.query.limit <= 100.
     * A defensive 5,000-session cap prevents an accidental excessive read.
     */
    suspend fun loadUserUsageDetail(admin: FirebaseUser, targetUid: String): UserUsageDetail {
        if (!isAdminUi(admin)) error("Tài khoản này không có quyền quản trị.")
        require(targetUid.isNotBlank()) { "UID người dùng không hợp lệ." }

        val database = FirebaseFirestore.getInstance()
        val documents = mutableListOf<DocumentSnapshot>()
        var cursor: DocumentSnapshot? = null
        val pageLimit = 100L
        val maxPages = 50

        for (pageNumber in 0 until maxPages) {
            var query = database.collection("docflow_sessions")
                .whereEqualTo("uid", targetUid)
                .limit(pageLimit)
            if (cursor != null) query = query.startAfter(cursor!!)
            val page = query.get(Source.SERVER).await()
            documents.addAll(page.documents)
            cursor = page.documents.lastOrNull()
            if (page.size() < pageLimit.toInt()) break
        }

        return UserUsageDetail(
            sessions = documents.take(maxPages * pageLimit.toInt())
                .mapNotNull(::sessionFromDocument)
                .sortedByDescending { it.startedAt.seconds },
            truncated = documents.size >= maxPages * pageLimit.toInt()
        )
    }

    suspend fun signOut(activity: Activity) {
        firebaseAuthOrNull(activity)?.signOut()
        profileSaved = false
        profileError = null
        // Avoid automatically selecting the previous Google account.
        runCatching {
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        }
    }
}


/**
 * Usage analytics for authenticated DocFlow accounts. The mandatory login gate
 * discloses the stored fields before sign-in. No document names or contents,
 * location, IP, Advertising ID, Android ID, or device serial are collected.
 * Online means a heartbeat within 150 seconds, NOT a guaranteed live connection.
 */
private val DOCFLOW_TIME_ZONE: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

internal object DocFlowUsageTracker {
    private const val TAG = "DocFlowUsage"
    private const val PING_INTERVAL_MS = 75_000L
    private val tasks = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class ForegroundSession(
        val uid: String,
        val ref: com.google.firebase.firestore.DocumentReference,
        val openedAtElapsed: Long,
        val created: Deferred<Boolean>,
        var heartbeat: Job? = null
    )

    @Volatile private var active: ForegroundSession? = null

    fun onForeground(context: Context, user: FirebaseUser) {
        if (!DocFlowAccount.hasUsageConsent(context, user.uid)) return
        if (active?.uid == user.uid) return
        onBackground()
        val reference = FirebaseFirestore.getInstance()
            .collection("docflow_sessions").document()
        val deviceModel = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }.joinToString(" ").take(100)
        val androidVersion = Build.VERSION.RELEASE.orEmpty().take(24)
        val created = tasks.async {
            runCatching {
                reference.set(mapOf(
                    "uid" to user.uid,
                    "deviceModel" to deviceModel,
                    "androidVersion" to androidVersion,
                    "startedAt" to FieldValue.serverTimestamp(),
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "endedAt" to null,
                    "durationSeconds" to 0L
                )).await()
            }.onFailure { Log.w(TAG, "Cannot create authenticated session", it) }.isSuccess
        }
        val session = ForegroundSession(
            uid = user.uid, ref = reference,
            openedAtElapsed = SystemClock.elapsedRealtime(), created = created
        )
        active = session
        session.heartbeat = tasks.launch {
            if (!created.await()) return@launch
            while (isActive) {
                delay(PING_INTERVAL_MS)
                if (active !== session) break
                val elapsedSec = durationSeconds(session)
                runCatching {
                    session.ref.update(mapOf(
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "durationSeconds" to elapsedSec
                    )).await()
                }.onFailure { Log.w(TAG, "Session heartbeat rejected", it) }
            }
        }
        // At most one profile refresh per 12 hours on this phone and this UID.
        val settings = DocFlowPreferences(context)
        val now = System.currentTimeMillis()
        if (now - settings.lastProfileRefresh(user.uid) >= 12L * 60 * 60 * 1000) {
            tasks.launch {
                runCatching { DocFlowAccount.saveOwnProfile(user) }
                    .onSuccess { settings.setLastProfileRefresh(user.uid, System.currentTimeMillis()) }
                    .onFailure { Log.w(TAG, "Cannot refresh authenticated account profile", it) }
            }
        }
    }

    private fun durationSeconds(session: ForegroundSession): Long =
        ((SystemClock.elapsedRealtime() - session.openedAtElapsed) / 1000L)
            .coerceIn(0, 86_400)

    private fun finishSession(): Job? {
        val session = active ?: return null
        active = null
        session.heartbeat?.cancel()
        val elapsedSec = durationSeconds(session)
        return tasks.launch {
            if (!session.created.await()) return@launch
            runCatching {
                session.ref.update(mapOf(
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "endedAt" to FieldValue.serverTimestamp(),
                    "durationSeconds" to elapsedSec
                )).await()
            }.onFailure { Log.w(TAG, "Cannot close opt-in session", it) }
        }
    }

    fun onBackground() { finishSession() }

    suspend fun stopAndFlush() {
        val pending = finishSession() ?: return
        // Don't block sign-out indefinitely when the phone has no network.
        withTimeoutOrNull(2500L) { pending.join() }
    }
}

// ==================== MANDATORY GOOGLE LOGIN ====================
/**
 * First-run gate. No application content is composed until Firebase has an
 * authenticated Google account. Firebase Auth persists the account, so the gate
 * is skipped on later launches while that session remains valid.
 */
@Composable
internal fun DocFlowMandatoryLoginScreen(
    firebaseConfigured: Boolean,
    signInBusy: Boolean,
    authError: String?,
    onSignIn: () -> Unit
) {
    val english = LocalDocLanguage.current == DocLanguage.EN
    fun t(vi: String, en: String): String = if (english) en else vi

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Load the real Android launcher icon defined by AndroidManifest.xml
            // (@mipmap/ic_launcher), not the document graphic used in other pages.
            DocFlowLauncherAppIcon(modifier = Modifier.size(88.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                "DocFlow",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                t("Văn phòng tài liệu ngoại tuyến", "Offline document workspace"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(34.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Text(
                        t("Đăng nhập để tiếp tục", "Sign in to continue"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        t(
                            "Bạn chỉ cần đăng nhập lần đầu. Các lần mở sau, DocFlow tự sử dụng phiên Firebase còn hiệu lực.",
                            "You only need to sign in the first time. Later launches reuse the active Firebase session."
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DocFlowSignInButton(
                        text = t("Đăng nhập", "Sign in"),
                        busy = signInBusy,
                        enabled = firebaseConfigured && !signInBusy,
                        onClick = onSignIn
                    )
                    Text(
                        t(
                            "Khi đăng nhập, DocFlow ghi UID, email, tên, thời điểm và thời lượng sử dụng, mẫu máy và phiên bản Android để quản trị thống kê. Ứng dụng không thu thập nội dung tài liệu, vị trí, mật khẩu hoặc mã định danh thiết bị.",
                            "By signing in, DocFlow records UID, email, name, usage times and duration, device model and Android version for administration statistics. It does not collect document contents, location, passwords or device identifiers."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!firebaseConfigured) {
                        Text(
                            t(
                                "Firebase chưa được cấu hình đầy đủ cho đăng nhập Google.",
                                "Firebase is not fully configured for Google sign-in."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (authError != null) {
                        Text(
                            t("Không thể đăng nhập: ", "Sign-in failed: ") + authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


/**
 * Uses the real launcher image defined in AndroidManifest.xml (mipmap/ic_launcher).
 * ImageView supports adaptive launcher icons; Compose painterResource may not.
 * Nothing is hardcoded about the application's brand artwork.
 */
@Composable
private fun DocFlowLauncherAppIcon(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            android.widget.ImageView(context).apply {
                setImageDrawable(context.packageManager.getApplicationIcon(context.applicationInfo))
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                contentDescription = "DocFlow"
            }
        }
    )
}

/**
 * The complete four-color Google 'G' vector, traced from the standard Google
 * Sign-In G artwork (48x48 viewBox); not a character glyph or approximated G.
 * The existing button provides its contrasting white background and outline.
 */
private val docFlowGoogleGIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Google G",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 48f,
        viewportHeight = 48f
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(
                "M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
            ).toNodes(),
            fill = SolidColor(Color(0xFFEA4335))
        )
        addPath(
            pathData = PathParser().parsePathString(
                "M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.25 5.48-4.75 7.18l7.73 6C44.43 38.03 46.98 31.85 46.98 24.55z"
            ).toNodes(),
            fill = SolidColor(Color(0xFF4285F4))
        )
        addPath(
            pathData = PathParser().parsePathString(
                "M10.53 28.59C10.07 27.21 9.8 25.67 9.8 24s.27-3.21.73-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24s.92 7.54 2.56 10.78l7.97-6.19z"
            ).toNodes(),
            fill = SolidColor(Color(0xFFFBBC05))
        )
        addPath(
            pathData = PathParser().parsePathString(
                "M24 48c6.48 0 11.93-2.13 15.93-5.8l-7.73-6c-2.15 1.45-4.92 2.3-8.2 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
            ).toNodes(),
            fill = SolidColor(Color(0xFF34A853))
        )
    }.build()
}

/** Google-branded sign-in: the icon must not be replaced by the app artwork. */

@Composable
private fun DocFlowSignInButton(
    text: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFDADCE0)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF202124),
            disabledContainerColor = Color(0xFFF1F3F4),
            disabledContentColor = Color(0xFF7A7D81)
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(21.dp),
                strokeWidth = 2.2.dp,
                color = Color(0xFF245FC7)
            )
        } else {
            Icon(
                imageVector = docFlowGoogleGIcon,
                contentDescription = "Google",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// ==================== INFORMATION AND SETTINGS PAGE ====================
@Composable
internal fun DocFlowSettingsScreen(
    language: DocLanguage,
    changeLanguage: (DocLanguage) -> Unit,
    appearance: DocAppearance,
    changeAppearance: (DocAppearance) -> Unit,
    firebaseUser: FirebaseUser?,
    signInBusy: Boolean,
    authError: String?,
    onSignOut: () -> Unit
) {
    var languageDialog by remember { mutableStateOf(false) }
    var appearanceDialog by remember { mutableStateOf(false) }
    var premiumDialog by remember { mutableStateOf(false) }
    var adminDashboard by remember { mutableStateOf(false) }
    val english = language == DocLanguage.EN
    fun t(vi: String, en: String): String = if (english) en else vi
    val isAdmin = DocFlowAccount.isAdminUi(firebaseUser)

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
            title = { Text("DocFlow Premium") },
            text = { Text(t(
                "Mục Premium đã được chuẩn bị. Google Play Billing chưa được tích hợp, ứng dụng chưa thu phí.",
                "Premium is a placeholder. Google Play Billing is not integrated; there is no payment."
            )) },
            confirmButton = {
                TextButton(onClick = { premiumDialog = false }) { Text(t("Đóng", "Close")) }
            }
        )
    }
    if (adminDashboard && isAdmin && firebaseUser != null) {
        DocFlowAdminDashboard(firebaseUser, english, onDismiss = { adminDashboard = false })
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(29.dp))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GoogleAccountAvatar(
                            user = firebaseUser,
                            admin = isAdmin,
                            onAdminClick = { adminDashboard = true }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                firebaseUser?.displayName?.takeIf { it.isNotBlank() }
                                    ?: t("Đang kiểm tra tài khoản", "Checking account"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                firebaseUser?.email.orEmpty(),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isAdmin) {
                                Text(
                                    t("Chạm ảnh đại diện để xem thống kê", "Tap your avatar for statistics"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (isAdmin) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        t(
                            "Đã đăng nhập bằng Google. DocFlow tự ghi nhận dữ liệu thống kê theo chính sách hiển thị ở màn hình đăng nhập.",
                            "Signed in with Google. DocFlow records statistics according to the policy shown on the login screen."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onSignOut,
                        enabled = !signInBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(t("Đăng xuất", "Sign out"))
                    }
                    if (authError != null) {
                        Text(
                            t("Không thể xác thực: ", "Authentication error: ") + authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
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
                title = t("Giao diện", "Appearance"), detail = mode,
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
                    Text(t("Các công cụ tài liệu vẫn hoạt động ngoại tuyến sau khi tài khoản đã đăng nhập.",
                        "Document tools continue to work offline after the account has signed in."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Google profile photo from FirebaseUser, with a local icon fallback when offline. */
@Composable
private fun GoogleAccountAvatar(user: FirebaseUser?, admin: Boolean, onAdminClick: () -> Unit) {
    val url = user?.photoUrl?.toString()?.takeIf { it.startsWith("https://") }
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = url) {
        value = null
        if (url != null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(url).openConnection().apply {
                        connectTimeout = 3000
                        readTimeout = 3000
                    }
                    connection.getInputStream().use { BitmapFactory.decodeStream(it) }
                }.getOrNull()
            }
        }
    }
    Box(
        modifier = Modifier.size(49.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(enabled = admin, onClick = onAdminClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = if (admin) "Mở thống kê quản trị" else "Ảnh đại diện Google",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        } else {
            Icon(if (user == null) Icons.Default.PersonOutline else Icons.Default.Person,
                contentDescription = if (admin) "Mở thống kê quản trị" else "Tài khoản Google",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(Modifier.size(43.dp).background(accent.copy(alpha = .12f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) {
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

// ==================== FULL SCREEN ADMIN DASHBOARD ====================
private val ADMIN_BG = Color(0xFF080B0A)
private val ADMIN_PANEL = Color(0xFF171B19)
private val ADMIN_PANEL_ALT = Color(0xFF202724)
private val ADMIN_GREEN = Color(0xFF16BB8B)
private val ADMIN_TEXT = Color(0xFFF2F7F5)
private val ADMIN_MUTED = Color(0xFFADB7B3)

private enum class DashboardSection(val vi: String, val en: String) {
    DAY("Theo ngày", "By day"),
    MONTH("Theo tháng", "By month"),
    COMPARE("So sánh tháng", "Compare months"),
    NEW("Người mới", "New users"),
    DEVICES("Thiết bị", "Devices"),
    INACTIVE("Không hoạt động", "Inactive")
}

private data class UsageTotals(
    val launches: Int = 0,
    val uniqueUsers: Int = 0,
    val durationSeconds: Long = 0,
    val onlineUsers: Int = 0
)

private data class UserDetailTarget(
    val profile: DocFlowAccount.UserProfile,
    val selectedDevice: String? = null
)

private fun summarizeSessions(sessions: List<DocFlowAccount.UsageSession>): UsageTotals {
    val recentLimit = System.currentTimeMillis() - 150_000L
    return UsageTotals(
        launches = sessions.size,
        uniqueUsers = sessions.map { it.uid }.distinct().size,
        durationSeconds = sessions.sumOf { it.durationSeconds },
        onlineUsers = sessions.filter {
            it.endedAt == null && (it.lastSeenAt?.toDate()?.time ?: 0L) >= recentLimit
        }.map { it.uid }.distinct().size
    )
}

private fun localDay(timestamp: Timestamp?): LocalDate? =
    timestamp?.toDate()?.toInstant()?.atZone(DOCFLOW_TIME_ZONE)?.toLocalDate()

private fun localMonth(timestamp: Timestamp?): YearMonth? =
    localDay(timestamp)?.let { YearMonth.from(it) }

private fun prettyDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    return when {
        s >= 3600L -> "%dh %02dm %02ds".format(Locale.US, s / 3600, (s % 3600) / 60, s % 60)
        s >= 60L -> "%dm %02ds".format(Locale.US, s / 60, s % 60)
        else -> "${s}s"
    }
}

private fun percentChange(now: Long, previous: Long): String =
    if (previous <= 0L) "—"
    else "%+.1f%%".format(Locale.US, (now - previous).toDouble() * 100.0 / previous)

private fun uidDisplay(uid: String, users: Map<String, DocFlowAccount.UserProfile>): String =
    users[uid]?.let { it.name.ifBlank { it.email } }?.takeIf { it.isNotBlank() }
        ?: "Khách (${uid.take(6)})"

private fun profileFor(
    uid: String,
    users: Map<String, DocFlowAccount.UserProfile>
): DocFlowAccount.UserProfile = users[uid] ?: DocFlowAccount.UserProfile(
    uid = uid,
    email = "",
    name = "Khách (${uid.take(6)})",
    createdAt = null,
    lastLoginAt = null
)

@Composable
private fun DocFlowAdminDashboard(
    firebaseUser: FirebaseUser,
    english: Boolean,
    onDismiss: () -> Unit
) {
    fun t(vi: String, en: String): String = if (english) en else vi
    var section by remember { mutableStateOf(DashboardSection.DAY) }
    var selectedDay by remember { mutableStateOf(LocalDate.now(DOCFLOW_TIME_ZONE)) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now(DOCFLOW_TIME_ZONE)) }
    var inactiveDays by remember { mutableIntStateOf(30) }
    var reloadCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf<DocFlowAccount.AdminSnapshot?>(null) }
    var userDetailTarget by remember { mutableStateOf<UserDetailTarget?>(null) }

    LaunchedEffect(firebaseUser.uid, reloadCount) {
        loading = true
        failure = null
        try {
            snapshot = DocFlowAccount.loadAdminDashboard(firebaseUser)
        } catch (e: Exception) {
            failure = e.localizedMessage ?: e.javaClass.simpleName
        } finally {
            loading = false
        }
    }

    Dialog(onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = ADMIN_BG) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = t("Trở về", "Back"), tint = ADMIN_TEXT)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t("Quản lý DocFlow (Admin)", "DocFlow Admin Dashboard"),
                            color = ADMIN_GREEN, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge)
                        Text(t("Thống kê người dùng đã đăng nhập", "Authenticated user analytics"),
                            color = ADMIN_MUTED, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { reloadCount++ }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = t("Tải lại", "Refresh"),
                            tint = ADMIN_GREEN)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DashboardSection.entries.forEach { candidate ->
                        DashboardChip(
                            text = if (english) candidate.en else candidate.vi,
                            selected = section == candidate,
                            onClick = { section = candidate }
                        )
                    }
                }
                HorizontalDivider(color = ADMIN_GREEN.copy(alpha = .65f))

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ADMIN_GREEN)
                    }
                    failure != null -> Column(
                        Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(t("Không tải được dữ liệu thống kê từ Firestore:",
                            "Cannot load admin analytics from Firestore:"), color = ADMIN_TEXT)
                        Text(failure.orEmpty(), color = Color(0xFFFFA7A7))
                        Text(t("Kiểm tra Rules cho collection docflow_sessions và docflow_users.",
                            "Check the Firestore Rules for both collections."), color = ADMIN_MUTED)
                    }
                    snapshot != null -> {
                        val report = snapshot!!
                        val users = report.users.associateBy { it.uid }
                        val today = LocalDate.now(DOCFLOW_TIME_ZONE)
                        val currentMonth = YearMonth.now(DOCFLOW_TIME_ZONE)
                        val daySessions = report.sessions.filter { localDay(it.startedAt) == selectedDay }
                        val monthSessions = report.sessions.filter { localMonth(it.startedAt) == selectedMonth }
                        val thisMonthSessions = report.sessions.filter { localMonth(it.startedAt) == currentMonth }
                        // Compare equal elapsed calendar days (e.g. Oct 1–23 vs Sep 1–23),
                        // rather than comparing the current partial month with a full month.
                        val previousMonth = currentMonth.minusMonths(1)
                        val comparableDay = minOf(today.dayOfMonth, previousMonth.lengthOfMonth())
                        val lastMonthSessions = report.sessions.filter {
                            localMonth(it.startedAt) == previousMonth &&
                                (localDay(it.startedAt)?.dayOfMonth ?: 0) <= comparableDay
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (report.usersTruncated || report.sessionsTruncated) {
                                item {
                                    Text(t(
                                        "Dữ liệu vượt giới hạn tải (1.000 hồ sơ / 1.500 phiên). Các số liệu dưới đây chỉ là một phần.",
                                        "The download cap (1,000 profiles / 1,500 sessions) was reached. Results are partial."
                                    ), color = Color(0xFFFFD085), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (report.sessionsError != null) {
                                item {
                                    Text(t(
                                        "Chưa đọc được lịch sử sử dụng từ docflow_sessions. Hãy cập nhật Firestore Rules cho collection này. Lỗi: ",
                                        "Cannot read usage sessions. Update Firestore Rules for docflow_sessions. Error: "
                                    ) + report.sessionsError.orEmpty(),
                                        color = Color(0xFFFFA7A7),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (report.sessionsError != null && section != DashboardSection.NEW) {
                                item { DashboardEmpty(t(
                                    "Chưa thể tính thống kê phiên khi Firestore từ chối truy vấn. Mục Người mới vẫn có thể xem bằng dữ liệu hồ sơ.",
                                    "Session metrics are unavailable until Firestore allows queries. New users can still be viewed from saved profiles."
                                )) }
                            } else when (section) {
                                DashboardSection.DAY -> {
                                    item {
                                        DashboardDateNav(
                                            label = t("Dữ liệu ngày: ", "Date: ") +
                                                selectedDay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                            canPrev = selectedDay > report.earliestSessionDate,
                                            canNext = selectedDay < today,
                                            prev = { selectedDay = selectedDay.minusDays(1) },
                                            next = { selectedDay = selectedDay.plusDays(1) }
                                        )
                                    }
                                    item { DashboardMetricGrid(summarizeSessions(daySessions), english) }
                                    item {
                                        Text(t("Người dùng theo thời lượng", "Users by time spent"),
                                            color = ADMIN_GREEN, fontWeight = FontWeight.Bold)
                                    }
                                    val ranked = daySessions.groupBy { it.uid }
                                        .map { (uid, entries) -> Triple(uid, entries.sumOf { it.durationSeconds }, entries) }
                                        .sortedByDescending { it.second }
                                    if (ranked.isEmpty()) {
                                        item { DashboardEmpty(t("Ngày này chưa có phiên sử dụng được ghi nhận.",
                                            "No tracked usage sessions on this day.")) }
                                    } else {
                                        items(ranked.take(100), key = { it.first }) { entry ->
                                            val latest = entry.third.maxByOrNull { it.startedAt.seconds }
                                            val profile = profileFor(entry.first, users)
                                            DashboardUserRow(
                                                title = uidDisplay(entry.first, users),
                                                subtitle = profile.email.ifBlank { "UID: ${entry.first.take(12)}" },
                                                detail = "${entry.third.size} ${t("phiên", "sessions")} · " +
                                                    (latest?.deviceModel ?: ""),
                                                value = prettyDuration(entry.second),
                                                onClick = { userDetailTarget = UserDetailTarget(profile) }
                                            )
                                        }
                                    }
                                }
                                DashboardSection.MONTH -> {
                                    item {
                                        DashboardDateNav(
                                            label = t("Tháng ", "Month ") +
                                                selectedMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                                            canPrev = selectedMonth > YearMonth.from(report.earliestSessionDate),
                                            canNext = selectedMonth < currentMonth,
                                            prev = { selectedMonth = selectedMonth.minusMonths(1) },
                                            next = { selectedMonth = selectedMonth.plusMonths(1) }
                                        )
                                    }
                                    item { DashboardMetricGrid(summarizeSessions(monthSessions), english) }
                                    item {
                                        Text(t("Hoạt động theo ngày", "Daily activity"),
                                            color = ADMIN_GREEN, fontWeight = FontWeight.Bold)
                                    }
                                    val grouped = monthSessions.groupBy { localDay(it.startedAt) }
                                        .toList().sortedByDescending { it.first }
                                    if (grouped.isEmpty()) {
                                        item { DashboardEmpty(t("Chưa có dữ liệu tháng này.", "No data for this month.")) }
                                    } else {
                                        items(grouped, key = { it.first?.toString() ?: "unknown" }) { (date, entries) ->
                                            val total = summarizeSessions(entries)
                                            DashboardUserRow(
                                                title = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "—",
                                                subtitle = "${total.uniqueUsers} ${t("người dùng", "users")} · ${total.launches} ${t("lượt", "sessions")}",
                                                detail = t("Thời gian ghi nhận", "Recorded duration"),
                                                value = prettyDuration(total.durationSeconds)
                                            )
                                        }
                                    }
                                }
                                DashboardSection.COMPARE -> {
                                    item {
                                        Text(t("So sánh tháng", "Monthly comparison"),
                                            color = ADMIN_GREEN, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    item {
                                        DashboardComparison(thisMonthSessions, lastMonthSessions,
                                            currentMonth, english)
                                    }
                                }
                                DashboardSection.NEW -> {
                                    item {
                                        DashboardDateNav(
                                            label = t("Người mới tháng ", "New users in ") +
                                                selectedMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
                                            canPrev = true, canNext = selectedMonth < currentMonth,
                                            prev = { selectedMonth = selectedMonth.minusMonths(1) },
                                            next = { selectedMonth = selectedMonth.plusMonths(1) }
                                        )
                                    }
                                    val newUsers = report.users.filter { localMonth(it.createdAt) == selectedMonth }
                                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                                    item {
                                        DashboardSummaryLine(
                                            t("Tài khoản tạo mới", "New profiles"), newUsers.size.toString())
                                    }
                                    if (newUsers.isEmpty()) {
                                        item { DashboardEmpty(t("Tháng này chưa ghi nhận tài khoản mới.",
                                            "No new profiles recorded for this month.")) }
                                    } else {
                                        items(newUsers.take(100), key = { it.uid }) { user ->
                                            DashboardUserRow(
                                                title = user.name.ifBlank { user.email }, subtitle = user.email,
                                                detail = t("Ngày tạo: ", "Created: ") + formatDocFlowTime(user.createdAt),
                                                value = t("Mới", "New"),
                                                onClick = { userDetailTarget = UserDetailTarget(user) }
                                            )
                                        }
                                    }
                                }
                                DashboardSection.DEVICES -> {
                                    item {
                                        Text(t("Thiết bị được ghi nhận", "Devices recorded"),
                                            color = ADMIN_GREEN, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    item {
                                        Text(t("Không thu thập mã máy, IMEI hay vị trí. Một tài khoản có thể sử dụng nhiều thiết bị.",
                                            "No device IDs, IMEI or location are collected. One account may use multiple devices."),
                                            color = ADMIN_MUTED, style = MaterialTheme.typography.bodySmall)
                                    }
                                    val accountDeviceGroups = report.sessions.groupBy { session ->
                                        val device = "${session.deviceModel.ifBlank { "Unknown" }} · Android ${session.androidVersion}"
                                        session.uid to device
                                    }.toList().sortedByDescending { it.second.size }
                                    if (accountDeviceGroups.isEmpty()) {
                                        item { DashboardEmpty(t("Chưa có dữ liệu thiết bị được ghi nhận.",
                                            "No recorded device data yet.")) }
                                    } else {
                                        items(
                                            accountDeviceGroups.take(200),
                                            key = { "${it.first.first}|${it.first.second}" }
                                        ) { (accountDevice, entries) ->
                                            val uid = accountDevice.first
                                            val device = accountDevice.second
                                            val profile = profileFor(uid, users)
                                            val totalSeconds = entries.sumOf { it.durationSeconds }
                                            val averageSeconds = if (entries.isEmpty()) 0L else totalSeconds / entries.size
                                            DashboardUserRow(
                                                title = profile.name.ifBlank { profile.email.ifBlank { "Khách (${uid.take(6)})" } },
                                                subtitle = profile.email.ifBlank { "UID: ${uid.take(12)}" },
                                                detail = device + " · ${entries.size} ${t("lượt", "uses")} · " +
                                                    t("TB ", "Avg ") + prettyDuration(averageSeconds),
                                                value = prettyDuration(totalSeconds),
                                                onClick = {
                                                    userDetailTarget = UserDetailTarget(profile, device)
                                                }
                                            )
                                        }
                                    }
                                }
                                DashboardSection.INACTIVE -> {
                                    item {
                                        Text(t("Tài khoản chưa ghi nhận hoạt động", "Accounts with no recent recorded activity"),
                                            color = ADMIN_GREEN, style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(7, 30, 90).forEach { days ->
                                                DashboardChip("$days ${t("ngày", "days")}",
                                                    selected = inactiveDays == days,
                                                    onClick = { inactiveDays = days })
                                            }
                                        }
                                    }
                                    val lastSeenPerUid = report.sessions.groupBy { it.uid }
                                        .mapValues { (_, sessions) ->
                                            sessions.maxOfOrNull {
                                                maxOf(it.lastSeenAt?.toDate()?.time ?: 0L,
                                                    it.endedAt?.toDate()?.time ?: 0L)
                                            } ?: 0L
                                        }
                                    val threshold = System.currentTimeMillis() - inactiveDays * 24L * 60 * 60 * 1000
                                    val inactive = report.users.mapNotNull { user ->
                                        val last = maxOf(user.lastLoginAt?.toDate()?.time ?: 0L,
                                            lastSeenPerUid[user.uid] ?: 0L)
                                        if (last in 1 until threshold) user to last else null
                                    }.sortedBy { it.second }
                                    item { DashboardSummaryLine(t("Tài khoản ít hoạt động", "Inactive profiles"),
                                        inactive.size.toString()) }
                                    if (inactive.isEmpty()) {
                                        item { DashboardEmpty(t("Chưa có tài khoản nào quá hạn ghi nhận.",
                                            "No profiles exceed the inactivity threshold.")) }
                                    } else {
                                        items(inactive.take(100), key = { it.first.uid }) { (user, lastSeen) ->
                                            DashboardUserRow(
                                                title = user.name.ifBlank { user.email },
                                                subtitle = user.email,
                                                detail = t("Ghi nhận lần cuối: ", "Last recorded: ") +
                                                    formatDashboardInstant(lastSeen),
                                                value = "${(System.currentTimeMillis() - lastSeen) / 86_400_000L}d",
                                                onClick = { userDetailTarget = UserDetailTarget(user) }
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Text(t(
                                    "Số liệu gồm hồ sơ và phiên được ghi nhận từ khi người dùng đăng nhập DocFlow. Phiên bắt đầu trước 00:00 tính vào ngày bắt đầu; thời gian online ước tính theo heartbeat 75 giây (cửa sổ 150 giây). Bảng tổng quan giới hạn ba tháng gần nhất; trang chi tiết người dùng đọc toàn bộ lịch sử đã lưu.",
                                    "Statistics include profiles and sessions recorded after users sign in to DocFlow. Sessions are attributed to their starting day. Online is estimated from 75-second heartbeats (150-second window). The overview covers the last three months; user details load the full stored history."
                                ), color = ADMIN_MUTED, style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    userDetailTarget?.let { target ->
        AdminUserUsageDetailDialog(
            adminUser = firebaseUser,
            profile = target.profile,
            selectedDevice = target.selectedDevice,
            english = english,
            onDismiss = { userDetailTarget = null }
        )
    }
}

@Composable
private fun AdminUserUsageDetailDialog(
    adminUser: FirebaseUser,
    profile: DocFlowAccount.UserProfile,
    selectedDevice: String?,
    english: Boolean,
    onDismiss: () -> Unit
) {
    fun t(vi: String, en: String): String = if (english) en else vi
    var reloadCount by remember(profile.uid) { mutableIntStateOf(0) }
    var loading by remember(profile.uid) { mutableStateOf(true) }
    var failure by remember(profile.uid) { mutableStateOf<String?>(null) }
    var detail by remember(profile.uid) { mutableStateOf<DocFlowAccount.UserUsageDetail?>(null) }

    LaunchedEffect(adminUser.uid, profile.uid, reloadCount) {
        loading = true
        failure = null
        detail = null
        try {
            detail = DocFlowAccount.loadUserUsageDetail(adminUser, profile.uid)
        } catch (error: Exception) {
            failure = error.localizedMessage ?: error.javaClass.simpleName
        } finally {
            loading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = ADMIN_BG) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = t("Trở về", "Back"), tint = ADMIN_TEXT)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            t("Chi tiết người dùng", "User details"),
                            color = ADMIN_GREEN,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            profile.name.ifBlank { profile.email.ifBlank { "UID: ${profile.uid.take(12)}" } },
                            color = ADMIN_MUTED,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { reloadCount++ }, enabled = !loading) {
                        Icon(Icons.Default.Refresh, contentDescription = t("Tải lại", "Refresh"), tint = ADMIN_GREEN)
                    }
                }
                HorizontalDivider(color = ADMIN_GREEN.copy(alpha = .65f))

                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ADMIN_GREEN)
                    }
                    failure != null -> Column(
                        Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(t("Không đọc được dữ liệu người dùng:", "Cannot load user data:"), color = ADMIN_TEXT)
                        Text(failure.orEmpty(), color = Color(0xFFFFA7A7))
                    }
                    detail != null -> {
                        val sessions = detail!!.sessions
                        val totalSeconds = sessions.sumOf { it.durationSeconds }
                        val averageSeconds = if (sessions.isEmpty()) 0L else totalSeconds / sessions.size
                        val firstSession = sessions.minByOrNull { it.startedAt.seconds }
                        val latestActivity = sessions.maxOfOrNull { session ->
                            maxOf(
                                session.startedAt.toDate().time,
                                session.lastSeenAt?.toDate()?.time ?: 0L,
                                session.endedAt?.toDate()?.time ?: 0L
                            )
                        }
                        val deviceGroups = sessions.groupBy {
                            "${it.deviceModel.ifBlank { "Unknown" }} · Android ${it.androidVersion}"
                        }.toList().sortedWith(
                            compareByDescending<Pair<String, List<DocFlowAccount.UsageSession>>> {
                                it.first == selectedDevice
                            }.thenByDescending { it.second.size }
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Surface(color = ADMIN_PANEL_ALT, shape = RoundedCornerShape(16.dp)) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            profile.name.ifBlank { profile.email.ifBlank { t("Không có tên", "No name") } },
                                            color = ADMIN_TEXT,
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        if (profile.email.isNotBlank()) {
                                            Text(profile.email, color = ADMIN_MUTED)
                                        }
                                        Text("UID: ${profile.uid}", color = ADMIN_MUTED, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            t("Tạo hồ sơ: ", "Profile created: ") + formatDocFlowTime(profile.createdAt),
                                            color = ADMIN_MUTED,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            t("Đăng nhập gần nhất: ", "Last sign-in: ") + formatDocFlowTime(profile.lastLoginAt),
                                            color = ADMIN_MUTED,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.weight(1f)) {
                                        DashboardMetric(t("Tổng lượt dùng", "Total uses"), sessions.size.toString())
                                    }
                                    Box(Modifier.weight(1f)) {
                                        DashboardMetric(t("Tổng thời gian", "Total time"), prettyDuration(totalSeconds))
                                    }
                                }
                            }
                            item {
                                DashboardMetric(
                                    t("Thời gian trung bình mỗi lần", "Average time per use"),
                                    prettyDuration(averageSeconds)
                                )
                            }
                            item {
                                Surface(color = ADMIN_PANEL, shape = RoundedCornerShape(15.dp)) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(t("Tổng quan hoạt động", "Activity overview"), color = ADMIN_GREEN, fontWeight = FontWeight.Bold)
                                        Text(
                                            t("Lần đầu ghi nhận: ", "First recorded use: ") +
                                                (firstSession?.startedAt?.toDate()?.time?.let(::formatDashboardInstant) ?: "—"),
                                            color = ADMIN_MUTED
                                        )
                                        Text(
                                            t("Hoạt động gần nhất: ", "Latest activity: ") +
                                                (latestActivity?.let(::formatDashboardInstant) ?: "—"),
                                            color = ADMIN_MUTED
                                        )
                                        Text(
                                            t("Số thiết bị/mẫu máy: ", "Device models: ") + deviceGroups.size,
                                            color = ADMIN_MUTED
                                        )
                                    }
                                }
                            }
                            if (detail!!.truncated) {
                                item {
                                    Text(
                                        t(
                                            "Tài khoản có trên 5.000 phiên. Các tổng số đang hiển thị được tính trên 5.000 phiên đã tải được.",
                                            "This account has more than 5,000 sessions. Totals are based on the 5,000 loaded sessions."
                                        ),
                                        color = Color(0xFFFFD085),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            item {
                                Text(t("Thiết bị đã sử dụng", "Devices used"), color = ADMIN_GREEN,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            if (deviceGroups.isEmpty()) {
                                item { DashboardEmpty(t("Chưa có phiên sử dụng.", "No usage sessions yet.")) }
                            } else {
                                items(deviceGroups, key = { it.first }) { (device, entries) ->
                                    val deviceSeconds = entries.sumOf { it.durationSeconds }
                                    val deviceAverage = if (entries.isEmpty()) 0L else deviceSeconds / entries.size
                                    DashboardUserRow(
                                        title = device,
                                        subtitle = "${entries.size} ${t("lượt dùng", "uses")}",
                                        detail = t("Tổng ", "Total ") + prettyDuration(deviceSeconds),
                                        value = t("TB ", "Avg ") + prettyDuration(deviceAverage),
                                        highlighted = device == selectedDevice
                                    )
                                }
                            }
                            item {
                                Text(t("10 lần sử dụng gần nhất", "10 most recent uses"), color = ADMIN_GREEN,
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(sessions.take(10)) { session ->
                                DashboardUserRow(
                                    title = formatDashboardInstant(session.startedAt.toDate().time),
                                    subtitle = "${session.deviceModel.ifBlank { "Unknown" }} · Android ${session.androidVersion}",
                                    detail = if (session.endedAt == null) t("Phiên đang hoạt động", "Active session")
                                        else t("Phiên đã kết thúc", "Completed session"),
                                    value = prettyDuration(session.durationSeconds)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDashboardInstant(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(DOCFLOW_TIME_ZONE)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))

private fun formatDocFlowTime(value: Timestamp?): String =
    if (value == null) "—" else formatDashboardInstant(value.toDate().time)

@Composable
private fun DashboardChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (selected) ADMIN_GREEN else ADMIN_PANEL,
        shape = RoundedCornerShape(11.dp)) {
        Text(text, color = if (selected) ADMIN_BG else ADMIN_MUTED,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp))
    }
}

@Composable
private fun DashboardDateNav(
    label: String, canPrev: Boolean, canNext: Boolean,
    prev: () -> Unit, next: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = prev, enabled = canPrev) {
            Icon(Icons.Default.ChevronLeft, null,
                tint = if (canPrev) ADMIN_GREEN else ADMIN_MUTED)
        }
        Text(label, color = ADMIN_GREEN,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f))
        IconButton(onClick = next, enabled = canNext) {
            Icon(Icons.Default.ChevronRight, null,
                tint = if (canNext) ADMIN_GREEN else ADMIN_MUTED)
        }
    }
}

@Composable
private fun DashboardMetricGrid(totals: UsageTotals, english: Boolean) {
    fun t(vi: String, en: String) = if (english) en else vi
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                DashboardMetric(t("Lượt sử dụng", "Sessions"), totals.launches.toString())
            }
            Box(Modifier.weight(1f)) {
                DashboardMetric(t("Tổng thời gian", "Time spent"), prettyDuration(totals.durationSeconds))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                DashboardMetric(t("Người dùng", "Users"), totals.uniqueUsers.toString())
            }
            Box(Modifier.weight(1f)) {
                DashboardMetric(t("Đang online*", "Online*"), totals.onlineUsers.toString())
            }
        }
    }
}

@Composable
private fun DashboardMetric(title: String, value: String) {
    Surface(color = ADMIN_PANEL, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = ADMIN_MUTED, style = MaterialTheme.typography.bodySmall)
            Text(value, color = ADMIN_TEXT, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun DashboardComparison(
    thisMonthSessions: List<DocFlowAccount.UsageSession>,
    lastMonthSessions: List<DocFlowAccount.UsageSession>,
    currentMonth: YearMonth,
    english: Boolean
) {
    fun t(vi: String, en: String) = if (english) en else vi
    val now = summarizeSessions(thisMonthSessions)
    val prev = summarizeSessions(lastMonthSessions)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${currentMonth.minusMonths(1).format(DateTimeFormatter.ofPattern("MM/yyyy"))}  →  " +
            currentMonth.format(DateTimeFormatter.ofPattern("MM/yyyy")),
            color = ADMIN_TEXT, fontWeight = FontWeight.SemiBold)
        val metrics = listOf(
            Triple(t("Lượt sử dụng", "Sessions"), now.launches.toLong(), prev.launches.toLong()),
            Triple(t("Người dùng", "Users"), now.uniqueUsers.toLong(), prev.uniqueUsers.toLong()),
            Triple(t("Thời gian (giây)", "Time (seconds)"), now.durationSeconds, prev.durationSeconds)
        )
        metrics.forEach { (title, value, previous) ->
            Surface(color = ADMIN_PANEL, shape = RoundedCornerShape(15.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, color = ADMIN_MUTED)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(previous.toString(), color = ADMIN_TEXT)
                        Icon(Icons.Default.ArrowForward, null, tint = ADMIN_MUTED)
                        Text(value.toString(), color = ADMIN_GREEN, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(percentChange(value, previous),
                            color = if (value >= previous) ADMIN_GREEN else Color(0xFFFF7777),
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (lastMonthSessions.isEmpty()) {
            Text(t("Tháng trước chưa có dữ liệu theo dõi; không thể tính tỷ lệ tăng/giảm đáng tin cậy.",
                "No tracked sessions for the previous month; percentage changes are unavailable."),
                color = ADMIN_MUTED, style = MaterialTheme.typography.bodySmall)
        }
        Text(t("So sánh cùng số ngày đã qua của hai tháng (tháng hiện tại là số lũy kế).",
            "Compares the same elapsed calendar days in both months (month-to-date)."),
            color = ADMIN_MUTED, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DashboardUserRow(
    title: String,
    subtitle: String,
    detail: String,
    value: String,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Surface(
        modifier = rowModifier,
        color = if (highlighted) ADMIN_PANEL_ALT else ADMIN_PANEL,
        shape = RoundedCornerShape(14.dp),
        border = if (highlighted) BorderStroke(1.dp, ADMIN_GREEN) else null
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = ADMIN_TEXT, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = ADMIN_MUTED, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(detail, color = ADMIN_MUTED, style = MaterialTheme.typography.labelSmall,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(value, color = ADMIN_GREEN, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge)
            if (onClick != null) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ADMIN_GREEN)
            }
        }
    }
}

@Composable
private fun DashboardSummaryLine(label: String, value: String) {
    Surface(color = ADMIN_PANEL_ALT, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = ADMIN_MUTED, modifier = Modifier.weight(1f))
            Text(value, color = ADMIN_GREEN, fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun DashboardEmpty(message: String) {
    Text(message, color = ADMIN_MUTED,
        modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
        style = MaterialTheme.typography.bodyMedium)
}
