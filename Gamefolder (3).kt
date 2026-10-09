package ducphi.tunnerinstrument

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.Build
import android.os.ParcelFileDescriptor
import android.widget.VideoView
import android.content.pm.ActivityInfo
import android.media.MediaPlayer
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Semaphore
import kotlin.math.roundToInt
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class LearningMaterialType { FOLDER, IMAGE, PDF, VIDEO }
enum class LearningMaterialStorage { PUBLIC, LOCAL, MIXED }
enum class LearningMaterialAddKind { FILE, FOLDER }
enum class LearningMaterialManageScope { PUBLIC, LOCAL }

/**
 * Resolve the Activity behind a themed/context wrapper so the Documents screen
 * can safely enable or disable FLAG_SECURE while it is visible.
 */
private fun Context.findLearningMaterialsActivity(): Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        val base = current.baseContext
        if (base === current) break
        current = base
    }
    return current as? Activity
}

enum class DocumentAccessScope(val wireValue: String) {
    DOCUMENTS("documents"),
    SONG_TAB("songtab")
}

data class LearningMaterialManageRequest(
    val item: LearningMaterialUiItem,
    val scope: LearningMaterialManageScope
)

private val LocalLearningMaterialsLanguage = staticCompositionLocalOf { "en" }

private fun learningMaterialText(
    lang: String,
    key: String,
    vietnameseFallback: String,
    englishFallback: String,
    vararg formatArgs: Any
): String {
    fun usable(value: String): Boolean =
        value.isNotBlank() && value != key && !value.contains("missing translation", ignoreCase = true)

    val translated = runCatching { CoreTranslator.getString(key, lang) }.getOrDefault("")
    val english = runCatching { CoreTranslator.getString(key, "en") }.getOrDefault("")
    val template = when {
        usable(translated) -> translated
        usable(english) -> english
        lang == "vi" -> vietnameseFallback
        else -> englishFallback
    }

    if (formatArgs.isEmpty()) return template
    return runCatching {
        String.format(java.util.Locale.getDefault(), template, *formatArgs)
    }.getOrDefault(template)
}


fun documentAccessText(
    lang: String,
    key: String,
    vietnameseFallback: String,
    englishFallback: String,
    vararg formatArgs: Any
): String = learningMaterialText(
    lang,
    key,
    vietnameseFallback,
    englishFallback,
    *formatArgs
)

fun buildTunerGoogleSignInOptions(context: Context): GoogleSignInOptions {
    // Quyền tài liệu dùng chính Gmail của tài khoản Google đã đăng nhập.
    // Không yêu cầu Web Client ID / Google ID Token, vì vậy cấu hình này
    // hoạt động với luồng đăng nhập Google hiện có của ứng dụng.
    return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .build()
}

data class DocumentPermissions(
    val canView: Boolean = false,
    val canDownload: Boolean = false,
    val canUpload: Boolean = false,
    val canCreateFolder: Boolean = false,
    val canManage: Boolean = false,
    val canViewAllFolders: Boolean = true,
    val allowedFolderPaths: Set<String> = emptySet(),
    val status: String = "disabled",
    val expiresAt: String? = null
)

data class DocumentAccessInfo(
    val email: String,
    val permissions: DocumentPermissions
)

data class DocumentPermissionRecord(
    val id: Long = 0L,
    val email: String = "",
    val canView: Boolean = true,
    val canDownload: Boolean = true,
    val canUpload: Boolean = false,
    val canCreateFolder: Boolean = false,
    val canManage: Boolean = false,
    val canViewAllFolders: Boolean = true,
    val allowedFolderPaths: Set<String> = emptySet(),
    val status: String = "active",
    val expiresAt: String? = null,
    val note: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class DocumentFolderOption(
    val key: String,
    val instrument: String,
    val path: String,
    val name: String
)

private fun normalizeDocumentFolderPath(value: String): String =
    value.replace('\\', '/').split('/').filter { it.isNotBlank() }.joinToString("/")

private fun documentFolderAccessKey(instrument: String, path: String): String {
    val cleanInstrument = instrument.trim().ifBlank { "Guitar" }
    val cleanPath = normalizeDocumentFolderPath(path)
    return if (cleanPath.isBlank()) cleanInstrument else "$cleanInstrument/$cleanPath"
}

private fun DocumentPermissions.canAccessFolder(instrument: String, path: String): Boolean {
    if (canManage || canViewAllFolders) return true
    val cleanPath = normalizeDocumentFolderPath(path)
    if (cleanPath.isBlank()) return true // Luôn cho phép mở danh mục gốc đã được máy chủ lọc.

    val target = documentFolderAccessKey(instrument, cleanPath)
        .lowercase(java.util.Locale.ROOT)
    return allowedFolderPaths.any { rawAllowed ->
        val allowed = normalizeDocumentFolderPath(rawAllowed)
            .lowercase(java.util.Locale.ROOT)
        allowed.isNotBlank() && (target == allowed || target.startsWith("$allowed/"))
    }
}

sealed class DocumentAccessState {
    object Checking : DocumentAccessState()
    data class LoginRequired(
        val email: String? = null,
        val requiresReLogin: Boolean = false,
        val message: String = ""
    ) : DocumentAccessState()
    data class Denied(val email: String, val message: String) : DocumentAccessState()
    data class Granted(val info: DocumentAccessInfo) : DocumentAccessState()
    data class Error(val message: String) : DocumentAccessState()
}

class DocumentAccessException(
    val code: String,
    override val message: String,
    val statusCode: Int
) : IOException(message)

private suspend fun OkHttpClient.awaitText(request: Request): Pair<Int, String> =
    suspendCancellableCoroutine { continuation ->
        val call = newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (!continuation.isActive) return
                continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        val result = response.code to response.body?.string().orEmpty()
                        if (continuation.isActive) continuation.resume(result)
                    }
                } catch (error: Throwable) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }

private data class CachedDocumentAccessInfo(
    val info: DocumentAccessInfo,
    val savedAt: Long
)

/**
 * Keeps the most recently verified access state only inside the running app process.
 * It is used exclusively when the same valid session receives a transient HTTP 5xx/
 * connection error while the user exits and immediately reopens Documents.
 */
private object DocumentAccessRuntimeCache {
    private const val TTL_MS = 10L * 60L * 1000L
    private val entries = ConcurrentHashMap<String, CachedDocumentAccessInfo>()

    private fun key(token: String, scope: DocumentAccessScope): String =
        token + "|" + scope.wireValue

    fun get(context: Context, scope: DocumentAccessScope): DocumentAccessInfo? {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) return null
        val entry = entries[key(token, scope)] ?: return null
        if (System.currentTimeMillis() - entry.savedAt > TTL_MS) {
            entries.remove(key(token, scope))
            return null
        }
        return entry.info
    }

    fun put(context: Context, scope: DocumentAccessScope, info: DocumentAccessInfo) {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) return
        if (entries.size > 24) entries.clear()
        entries[key(token, scope)] = CachedDocumentAccessInfo(info, System.currentTimeMillis())
    }
}

object DocumentAccessStore {
    private const val PREFS = "document_access_session"
    private const val TOKEN = "session_token"
    private const val EMAIL = "email"

    fun sessionToken(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(TOKEN, "")
            .orEmpty()

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(EMAIL, "")
            .orEmpty()

    fun save(context: Context, token: String, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN, token)
            .putString(EMAIL, email)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

object DocumentAccessApi {
    private const val ENDPOINT = "https://tunertools.top/api_documents.php"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun isGranted(info: DocumentAccessInfo, expectedEmail: String): Boolean =
        info.email.trim().lowercase() == expectedEmail &&
                (info.permissions.canView || info.permissions.canManage)

    private fun cachedGrantedState(
        context: Context,
        scope: DocumentAccessScope,
        expectedEmail: String
    ): DocumentAccessState.Granted? {
        val cached = DocumentAccessRuntimeCache.get(context, scope) ?: return null
        return cached.takeIf { isGranted(it, expectedEmail) }
            ?.let { DocumentAccessState.Granted(it) }
    }

    private fun isDeniedCode(code: String): Boolean = code in setOf(
        "songtab_trial_scope_only",
        "documents_not_granted",
        "document_access_required",
        "view_not_granted",
        "folder_access_denied",
        "permission_denied"
    )

    suspend fun ensureSession(
        context: Context,
        scope: DocumentAccessScope = DocumentAccessScope.DOCUMENTS
    ): DocumentAccessState = withContext(Dispatchers.IO) {
        val signInClient = GoogleSignIn.getClient(context, buildTunerGoogleSignInOptions(context))
        val silentAccount = try {
            signInClient.silentSignIn().await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val account = silentAccount
            ?: GoogleSignIn.getLastSignedInAccount(context)
            ?: run {
                DocumentAccessStore.clear(context)
                return@withContext DocumentAccessState.LoginRequired()
            }

        val currentEmail = account.email.orEmpty().trim().lowercase()
        if (currentEmail.isBlank()) {
            DocumentAccessStore.clear(context)
            return@withContext DocumentAccessState.LoginRequired(
                email = account.email,
                message = "Không đọc được Gmail của tài khoản Google. Hãy đăng nhập lại."
            )
        }

        val existingToken = DocumentAccessStore.sessionToken(context)
        val storedEmail = DocumentAccessStore.email(context).trim().lowercase()

        // Never reuse a session that belongs to another Google account.
        if (existingToken.isNotBlank() && storedEmail == currentEmail) {
            try {
                val info = me(context, scope)
                if (isGranted(info, currentEmail)) {
                    return@withContext DocumentAccessState.Granted(info)
                }
                return@withContext DocumentAccessState.Denied(
                    email = currentEmail,
                    message = "Gmail này chưa được cấp quyền sử dụng Tài liệu."
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: DocumentAccessException) {
                when {
                    error.statusCode == 401 || error.code == "session_expired" -> {
                        DocumentAccessStore.clear(context)
                        return@withContext login(context, currentEmail, scope)
                    }
                    scope == DocumentAccessScope.DOCUMENTS && isDeniedCode(error.code) -> {
                        return@withContext DocumentAccessState.Denied(
                            email = currentEmail,
                            message = error.message
                        )
                    }
                    else -> {
                        // A temporary HTTP 5xx/invalid body must not erase a valid token
                        // and must not immediately create a second auth_login request.
                        cachedGrantedState(context, scope, currentEmail)?.let {
                            return@withContext it
                        }
                        return@withContext DocumentAccessState.Error(error.message)
                    }
                }
            } catch (error: Exception) {
                cachedGrantedState(context, scope, currentEmail)?.let {
                    return@withContext it
                }
                return@withContext DocumentAccessState.Error(
                    error.message ?: "Không thể kiểm tra quyền tài liệu."
                )
            }
        }

        if (existingToken.isNotBlank() || storedEmail.isNotBlank()) {
            DocumentAccessStore.clear(context)
        }
        login(context, currentEmail, scope)
    }

    suspend fun loginWithAccount(
        context: Context,
        account: GoogleSignInAccount?,
        scope: DocumentAccessScope = DocumentAccessScope.DOCUMENTS
    ): DocumentAccessState = withContext(Dispatchers.IO) {
        if (account == null) return@withContext DocumentAccessState.LoginRequired()
        val email = account.email.orEmpty().trim().lowercase()
        if (email.isBlank()) {
            return@withContext DocumentAccessState.LoginRequired(
                email = account.email,
                message = "Không đọc được Gmail của tài khoản Google. Hãy đăng nhập lại."
            )
        }
        if (DocumentAccessStore.email(context).trim().lowercase() != email) {
            DocumentAccessStore.clear(context)
        }
        login(context, email, scope)
    }

    suspend fun login(
        context: Context,
        email: String,
        scope: DocumentAccessScope = DocumentAccessScope.DOCUMENTS
    ): DocumentAccessState = withContext(Dispatchers.IO) {
        try {
            val normalizedEmail = email.trim().lowercase()
            if (normalizedEmail.isBlank()) {
                return@withContext DocumentAccessState.LoginRequired(
                    message = "Không đọc được Gmail của tài khoản Google."
                )
            }
            val root = requestForm(
                context = null,
                action = "auth_login",
                fields = mapOf(
                    "email" to normalizedEmail,
                    "scope" to scope.wireValue
                ),
                authorized = false
            )
            if (!root.optBoolean("authorized", false)) {
                // A Documents denial must not erase an active SongTab trial session.
                if (scope == DocumentAccessScope.SONG_TAB) {
                    DocumentAccessStore.clear(context)
                }
                return@withContext DocumentAccessState.Denied(
                    email = root.optString("email", "").ifBlank { normalizedEmail },
                    message = root.optString("message", "Tài khoản chưa được cấp quyền tài liệu.")
                )
            }
            val sessionToken = root.optString("session_token", "")
            val info = parseAccessInfo(root)
            if (sessionToken.isBlank()) {
                throw DocumentAccessException(
                    code = "missing_session_token",
                    message = "Máy chủ không trả về session token.",
                    statusCode = 500
                )
            }
            DocumentAccessStore.save(context, sessionToken, info.email)
            DocumentAccessRuntimeCache.put(context, scope, info)
            DocumentAccessState.Granted(info)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: DocumentAccessException) {
            DocumentAccessState.Error(error.message)
        } catch (error: Exception) {
            DocumentAccessState.Error(error.message ?: "Không thể xác thực quyền tài liệu.")
        }
    }

    suspend fun me(
        context: Context,
        scope: DocumentAccessScope = DocumentAccessScope.DOCUMENTS
    ): DocumentAccessInfo = withContext(Dispatchers.IO) {
        val root = requestGet(
            context = context,
            action = "permission_me",
            fields = mapOf("scope" to scope.wireValue)
        )
        parseAccessInfo(root).also {
            DocumentAccessRuntimeCache.put(context, scope, it)
        }
    }

    /**
     * Check another access scope using the current session without clearing it.
     * A temporary server error falls back to the last verified state for this
     * exact session instead of logging the user out while navigating folders.
     */
    suspend fun checkCurrentScope(
        context: Context,
        scope: DocumentAccessScope
    ): DocumentAccessState = withContext(Dispatchers.IO) {
        val currentEmail = DocumentAccessStore.email(context).trim().lowercase()
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank() || currentEmail.isBlank()) {
            return@withContext DocumentAccessState.LoginRequired(
                email = currentEmail.ifBlank { null },
                message = "Vui lòng đăng nhập Google."
            )
        }

        try {
            val info = me(context, scope)
            if (isGranted(info, currentEmail)) {
                DocumentAccessState.Granted(info)
            } else {
                DocumentAccessState.Denied(
                    email = currentEmail,
                    message = "Gmail này chưa được quản trị viên cấp quyền sử dụng Tài liệu. Vui lòng liên hệ Gmail hoặc Zalo để đăng ký."
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: DocumentAccessException) {
            when {
                error.statusCode == 401 || error.code == "session_expired" -> {
                    DocumentAccessStore.clear(context)
                    DocumentAccessState.LoginRequired(
                        email = currentEmail,
                        message = error.message
                    )
                }
                scope == DocumentAccessScope.DOCUMENTS && isDeniedCode(error.code) ->
                    DocumentAccessState.Denied(
                        email = currentEmail,
                        message = error.message
                    )
                else -> cachedGrantedState(context, scope, currentEmail)
                    ?: DocumentAccessState.Error(error.message)
            }
        } catch (error: Exception) {
            cachedGrantedState(context, scope, currentEmail)
                ?: DocumentAccessState.Error(
                    error.message ?: "Không thể kiểm tra quyền Tài liệu."
                )
        }
    }

    suspend fun listPermissions(context: Context): List<DocumentPermissionRecord> = withContext(Dispatchers.IO) {
        val root = requestGet(context, "permission_list")
        buildList {
            val items = root.optJSONArray("items")
            if (items != null) {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    add(parsePermissionRecord(item))
                }
            }
        }
    }

    suspend fun savePermission(
        context: Context,
        record: DocumentPermissionRecord
    ): DocumentPermissionRecord = withContext(Dispatchers.IO) {
        val root = requestForm(
            context = context,
            action = "permission_save",
            fields = mapOf(
                "id" to record.id.toString(),
                "email" to record.email.trim().lowercase(),
                "can_view" to if (record.canView) "1" else "0",
                "can_download" to if (record.canDownload) "1" else "0",
                "can_upload" to if (record.canUpload) "1" else "0",
                "can_create_folder" to if (record.canCreateFolder) "1" else "0",
                "can_manage" to if (record.canManage) "1" else "0",
                "can_view_all_folders" to if (record.canViewAllFolders) "1" else "0",
                "allowed_folders" to org.json.JSONArray(record.allowedFolderPaths.sorted()).toString(),
                "status" to record.status,
                "expires_at" to record.expiresAt.orEmpty(),
                "note" to record.note
            ),
            authorized = true
        )
        parsePermissionRecord(root.optJSONObject("item") ?: JSONObject())
    }

    suspend fun listFolderOptions(
        context: Context,
        instrument: String = "Guitar"
    ): List<DocumentFolderOption> = withContext(Dispatchers.IO) {
        val root = requestGet(
            context = context,
            action = "permission_folder_options",
            fields = mapOf("instrument" to instrument)
        )
        buildList {
            val items = root.optJSONArray("items")
            if (items != null) {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val key = item.optString("key", "").trim()
                    val path = item.optString("path", "").trim()
                    val name = item.optString("name", path.substringAfterLast('/')).trim()
                    if (key.isNotBlank() && path.isNotBlank()) {
                        add(
                            DocumentFolderOption(
                                key = key,
                                instrument = item.optString("instrument", instrument),
                                path = path,
                                name = name.ifBlank { path.substringAfterLast('/') }
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun deletePermission(context: Context, id: Long): String = withContext(Dispatchers.IO) {
        val root = requestForm(
            context = context,
            action = "permission_delete",
            fields = mapOf("id" to id.toString()),
            authorized = true
        )
        root.optString("message", "Đã xóa quyền tài liệu.")
    }

    private fun jsonStringSet(array: org.json.JSONArray?): Set<String> = buildSet {
        if (array == null) return@buildSet
        for (index in 0 until array.length()) {
            val value = array.optString(index, "").replace('\\', '/').trim().trim('/')
            if (value.isNotBlank()) add(value)
        }
    }

    private fun parseAccessInfo(root: JSONObject): DocumentAccessInfo {
        val user = root.optJSONObject("user")
        val permissions = root.optJSONObject("permissions") ?: JSONObject()
        val email = user?.optString("email", "")
            .orEmpty()
            .ifBlank { permissions.optString("email", "") }
        return DocumentAccessInfo(
            email = email,
            permissions = DocumentPermissions(
                canView = permissions.optBoolean("can_view", false),
                canDownload = permissions.optBoolean("can_download", false),
                canUpload = permissions.optBoolean("can_upload", false),
                canCreateFolder = permissions.optBoolean("can_create_folder", false),
                canManage = permissions.optBoolean("can_manage", false),
                canViewAllFolders = permissions.optBoolean("can_view_all_folders", true),
                allowedFolderPaths = jsonStringSet(permissions.optJSONArray("allowed_folders")),
                status = permissions.optString("status", "disabled"),
                expiresAt = permissions.optString("expires_at", "").ifBlank { null }
            )
        )
    }

    private fun parsePermissionRecord(item: JSONObject): DocumentPermissionRecord =
        DocumentPermissionRecord(
            id = item.optLong("id", 0L),
            email = item.optString("email", ""),
            canView = item.optBoolean("can_view", false),
            canDownload = item.optBoolean("can_download", false),
            canUpload = item.optBoolean("can_upload", false),
            canCreateFolder = item.optBoolean("can_create_folder", false),
            canManage = item.optBoolean("can_manage", false),
            canViewAllFolders = item.optBoolean("can_view_all_folders", true),
            allowedFolderPaths = jsonStringSet(item.optJSONArray("allowed_folders")),
            status = item.optString("status", "disabled"),
            expiresAt = item.optString("expires_at", "").ifBlank { null },
            note = item.optString("note", ""),
            createdAt = item.optString("created_at", ""),
            updatedAt = item.optString("updated_at", "")
        )

    private suspend fun requestGet(
        context: Context,
        action: String,
        fields: Map<String, String> = emptyMap()
    ): JSONObject {
        val url = buildString {
            append(ENDPOINT)
            append("?action=")
            append(encode(action))
            fields.forEach { (key, value) ->
                append('&')
                append(encode(key))
                append('=')
                append(encode(value))
            }
            append("&_ts=")
            append(System.currentTimeMillis())
        }
        val requestBuilder = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "TunerTools-Android")
        applyAuthorization(context, requestBuilder)
        val (statusCode, responseText) = httpClient.awaitText(requestBuilder.build())
        return readJsonResponse(statusCode, responseText)
    }

    private suspend fun requestForm(
        context: Context?,
        action: String,
        fields: Map<String, String>,
        authorized: Boolean
    ): JSONObject {
        val form = FormBody.Builder().apply {
            add("action", action)
            fields.forEach { (key, value) -> add(key, value) }
        }.build()
        val requestBuilder = Request.Builder()
            .url("$ENDPOINT?action=${encode(action)}")
            .post(form)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "TunerTools-Android")
        if (authorized) {
            applyAuthorization(requireNotNull(context), requestBuilder)
        }
        val (statusCode, responseText) = httpClient.awaitText(requestBuilder.build())
        return readJsonResponse(statusCode, responseText)
    }

    private fun applyAuthorization(context: Context, requestBuilder: Request.Builder) {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) {
            throw DocumentAccessException("login_required", "Vui lòng đăng nhập Google.", 401)
        }
        requestBuilder
            .header("Authorization", "Bearer $token")
            .header("X-Document-Token", token)
    }

    private fun readJsonResponse(statusCode: Int, responseText: String): JSONObject {
        if (responseText.isBlank()) {
            throw DocumentAccessException("empty_response", "Máy chủ không trả về dữ liệu.", statusCode)
        }
        val cleanText = responseText.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        val root = runCatching { JSONObject(cleanText) }.getOrElse {
            throw DocumentAccessException(
                "invalid_json",
                "Máy chủ không trả về JSON hợp lệ (HTTP $statusCode).",
                statusCode
            )
        }
        if (statusCode !in 200..299 || !root.optBoolean("success", false)) {
            throw DocumentAccessException(
                code = root.optString("code", "server_error"),
                message = root.optString("message", "Không thể xử lý quyền tài liệu."),
                statusCode = statusCode
            )
        }
        return root
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}

data class LearningMaterialUiItem(
    val id: String,
    val parentId: String,
    val type: LearningMaterialType,
    val titleVi: String,
    val titleEn: String,
    val descriptionVi: String = "",
    val descriptionEn: String = "",
    val metaVi: String = "",
    val metaEn: String = "",
    val previewUrl: String = "",
    val downloadUrl: String = "",
    val path: String = "",
    val childrenCount: Int = 0,
    val storage: LearningMaterialStorage = LearningMaterialStorage.PUBLIC,
    val localFilePath: String = "",
    val containedTypes: Set<LearningMaterialType> = emptySet(),
    val containedTypesKnown: Boolean = false,
    val containedCounts: Map<LearningMaterialType, Int> = emptyMap(),
    val containedCountsKnown: Boolean = false,
    val isProtectedSystemFolder: Boolean = false,
    val ownerEmail: String = "",
    val isOwner: Boolean = false,
    val canManageItem: Boolean = false,
    val isCommunityContent: Boolean = false
)

/*
 * API tai lieu duoc dat chung trong Gamefolder.kt sau khi tach khoi GameChordScreen.kt.
 * Khong can giu file LearningMaterialsApi.kt rieng trong project.
 */
data class LearningMaterialBreadcrumb(
    val path: String,
    val name: String
)

data class LearningMaterialPage(
    val instrument: String,
    val available: Boolean,
    val currentPath: String,
    val parentPath: String?,
    val breadcrumbs: List<LearningMaterialBreadcrumb>,
    val items: List<LearningMaterialUiItem>,
    val message: String,
    val contributionFolder: String = "Cộng đồng",
    val canPublicUploadHere: Boolean = false,
    val canPublicCreateFolderHere: Boolean = false,
    val currentFolderOwnerEmail: String = "",
    val currentFolderIsOwner: Boolean = false
)

private data class LearningPickedFileInfo(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)

private fun queryLearningPickedFileInfo(context: Context, uri: Uri): LearningPickedFileInfo {
    var displayName = "tai_lieu"
    var sizeBytes = -1L

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                displayName = cursor.getString(nameIndex)?.trim().orEmpty().ifBlank { displayName }
            }
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }

    var mimeType = context.contentResolver.getType(uri).orEmpty()
    if (mimeType.isBlank()) {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    if (displayName.substringAfterLast('.', "").isBlank()) {
        val inferredExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
        if (inferredExtension.isNotBlank()) {
            displayName = "$displayName.$inferredExtension"
        }
    }

    return LearningPickedFileInfo(
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes
    )
}

private fun buildLearningBreadcrumbs(
    instrument: String,
    relativePath: String
): List<LearningMaterialBreadcrumb> = buildList {
    add(LearningMaterialBreadcrumb(path = "", name = instrument))
    if (relativePath.isNotBlank()) {
        val built = mutableListOf<String>()
        relativePath.replace('\\', '/').split('/').filter { it.isNotBlank() }.forEach { part ->
            built += part
            add(LearningMaterialBreadcrumb(path = built.joinToString("/"), name = part))
        }
    }
}

object LearningMaterialsApi {
    // PHP file placed at C:\\inetpub\\wwwroot\\api_documents.php
    private const val ENDPOINT = "https://tunertools.top/api_documents.php"
    private const val PAGE_CACHE_TTL_MS = 5L * 60L * 1000L

    private data class PageCacheEntry(
        val page: LearningMaterialPage,
        val savedAt: Long
    )

    private val pageCache = ConcurrentHashMap<String, PageCacheEntry>()

    private val listHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val healthHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(7, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun normalizePagePath(path: String): String =
        path.replace('\\', '/').split('/').filter { it.isNotBlank() }.joinToString("/")

    private fun pageCacheKey(
        context: Context,
        instrument: String,
        path: String
    ): String {
        val token = DocumentAccessStore.sessionToken(context)
        return token + "|" + instrument.trim().lowercase(java.util.Locale.ROOT) +
                "|" + normalizePagePath(path).lowercase(java.util.Locale.ROOT)
    }

    fun peekCachedPage(
        context: Context,
        instrument: String,
        path: String,
        allowExpired: Boolean = false
    ): LearningMaterialPage? {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) return null
        val key = pageCacheKey(context, instrument, path)
        val entry = pageCache[key] ?: return null
        if (!allowExpired && System.currentTimeMillis() - entry.savedAt > PAGE_CACHE_TTL_MS) {
            pageCache.remove(key)
            return null
        }
        return entry.page
    }

    fun invalidateInstrumentCache(context: Context, instrument: String) {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) return
        val prefix = token + "|" + instrument.trim().lowercase(java.util.Locale.ROOT) + "|"
        pageCache.keys.filter { it.startsWith(prefix) }.forEach { pageCache.remove(it) }
    }

    private fun cachePage(
        context: Context,
        instrument: String,
        requestedPath: String,
        page: LearningMaterialPage
    ) {
        if (pageCache.size > 80) pageCache.clear()
        pageCache[pageCacheKey(context, instrument, requestedPath)] =
            PageCacheEntry(page, System.currentTimeMillis())
    }

    suspend fun loadPage(
        context: Context,
        instrument: String,
        path: String = "",
        forceRefresh: Boolean = false
    ): LearningMaterialPage = withContext(Dispatchers.IO) {
        val cleanPath = normalizePagePath(path)
        if (!forceRefresh) {
            peekCachedPage(context, instrument, cleanPath)?.let {
                return@withContext it
            }
        }

        val requestUrl = buildString {
            append(ENDPOINT)
            append("?action=list")
            append("&instrument=")
            append(encode(instrument))
            append("&path=")
            append(encode(cleanPath))
            append("&_ts=")
            append(System.currentTimeMillis())
        }

        val requestBuilder = Request.Builder()
            .url(requestUrl)
            .get()
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .header("User-Agent", "TunerTools-Android")
        applyAuthorization(context, requestBuilder)

        val (statusCode, responseText) = listHttpClient.awaitText(requestBuilder.build())
        val root = readJsonResponse(statusCode, responseText)
        parsePage(root, instrument, cleanPath).also { page ->
            cachePage(context, instrument, cleanPath, page)
        }
    }

    private fun parsePage(
        root: JSONObject,
        instrument: String,
        requestedPath: String
    ): LearningMaterialPage {
        val currentPath = root.optString("current_path", requestedPath)
        val parentPath = if (root.isNull("parent_path")) {
            null
        } else {
            root.optString("parent_path", "")
        }

        val breadcrumbs = buildList {
            val array = root.optJSONArray("breadcrumbs")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        LearningMaterialBreadcrumb(
                            path = item.optString("path", ""),
                            name = item.optString("name", instrument)
                        )
                    )
                }
            }
            if (isEmpty()) addAll(buildLearningBreadcrumbs(instrument, currentPath))
        }

        val materials = buildList {
            val array = root.optJSONArray("items")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val type = when (item.optString("type", "").lowercase()) {
                        "folder" -> LearningMaterialType.FOLDER
                        "image" -> LearningMaterialType.IMAGE
                        "pdf" -> LearningMaterialType.PDF
                        "video" -> LearningMaterialType.VIDEO
                        else -> continue
                    }

                    val title = item.optString("name", item.optString("file_name", "Tài liệu"))
                    val description = item.optString("description", "")
                    val meta = item.optString("meta", "")
                    val containedTypesArray = item.optJSONArray("contained_types")
                    val containedTypes = mutableSetOf<LearningMaterialType>()
                    if (containedTypesArray != null) {
                        for (typeIndex in 0 until containedTypesArray.length()) {
                            when (containedTypesArray.optString(typeIndex, "").lowercase()) {
                                "image" -> containedTypes += LearningMaterialType.IMAGE
                                "pdf" -> containedTypes += LearningMaterialType.PDF
                                "video" -> containedTypes += LearningMaterialType.VIDEO
                            }
                        }
                    }

                    val containedCountsObject = item.optJSONObject("contained_counts")
                    val containedCounts = mutableMapOf<LearningMaterialType, Int>()
                    if (containedCountsObject != null) {
                        val imageCount = containedCountsObject.optInt("image", 0).coerceAtLeast(0)
                        val pdfCount = containedCountsObject.optInt("pdf", 0).coerceAtLeast(0)
                        val videoCount = containedCountsObject.optInt("video", 0).coerceAtLeast(0)
                        if (imageCount > 0) containedCounts[LearningMaterialType.IMAGE] = imageCount
                        if (pdfCount > 0) containedCounts[LearningMaterialType.PDF] = pdfCount
                        if (videoCount > 0) containedCounts[LearningMaterialType.VIDEO] = videoCount
                    }

                    if (type != LearningMaterialType.FOLDER) {
                        containedTypes += type
                        containedCounts[type] = maxOf(containedCounts[type] ?: 0, 1)
                    }
                    val containedTypesKnown =
                        type != LearningMaterialType.FOLDER || item.has("contained_types")
                    val containedCountsKnown =
                        type != LearningMaterialType.FOLDER || item.has("contained_counts")

                    add(
                        LearningMaterialUiItem(
                            id = "public:" + item.optString("id", item.optString("path", title)),
                            parentId = currentPath,
                            type = type,
                            titleVi = title,
                            titleEn = title,
                            descriptionVi = description,
                            descriptionEn = description,
                            metaVi = meta,
                            metaEn = meta,
                            previewUrl = item.optString("preview_url", ""),
                            downloadUrl = item.optString("download_url", ""),
                            path = item.optString("path", ""),
                            childrenCount = item.optInt("children_count", 0),
                            storage = LearningMaterialStorage.PUBLIC,
                            containedTypes = containedTypes,
                            containedTypesKnown = containedTypesKnown,
                            containedCounts = containedCounts,
                            containedCountsKnown = containedCountsKnown,
                            isProtectedSystemFolder = item.optBoolean("is_protected_system_folder", false),
                            ownerEmail = item.optString("owner_email", ""),
                            isOwner = item.optBoolean("is_owner", false),
                            canManageItem = item.optBoolean("can_manage_item", false),
                            isCommunityContent = item.optBoolean("is_community_content", false)
                        )
                    )
                }
            }
        }

        return LearningMaterialPage(
            instrument = root.optString("instrument", instrument),
            available = root.optBoolean("available", true),
            currentPath = currentPath,
            parentPath = parentPath,
            breadcrumbs = breadcrumbs,
            items = materials,
            message = root.optString("message", ""),
            contributionFolder = root.optString("contribution_folder", "Cộng đồng"),
            canPublicUploadHere = root.optBoolean("can_public_upload_here", false),
            canPublicCreateFolderHere = root.optBoolean("can_public_create_folder_here", false),
            currentFolderOwnerEmail = root.optString("current_folder_owner_email", ""),
            currentFolderIsOwner = root.optBoolean("current_folder_is_owner", false)
        )
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun checkConnection(context: Context, instrument: String): Boolean =
        withContext(Dispatchers.IO) {
            val requestUrl = "$ENDPOINT?action=health&_ts=${System.currentTimeMillis()}"
            val request = Request.Builder()
                .url(requestUrl)
                .get()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "TunerTools-Android-Health")
                .build()
            try {
                val (statusCode, responseText) = healthHttpClient.awaitText(request)
                if (statusCode !in 200..299) return@withContext false
                val cleanText = responseText.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
                cleanText.startsWith("{") &&
                        runCatching { JSONObject(cleanText).optBoolean("success", false) }
                            .getOrDefault(false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }

    private fun applyAuthorization(context: Context, requestBuilder: Request.Builder) {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) {
            throw IOException("Vui lòng đăng nhập Google để truy cập tài liệu.")
        }
        requestBuilder
            .header("Authorization", "Bearer $token")
            .header("X-Document-Token", token)
    }

    private fun readJsonResponse(statusCode: Int, responseText: String): JSONObject {
        if (responseText.isBlank()) {
            throw IOException("Máy chủ không trả về dữ liệu (HTTP $statusCode).")
        }
        val cleanText = responseText.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        val root = runCatching { JSONObject(cleanText) }.getOrElse {
            throw IOException("Máy chủ không trả về JSON hợp lệ (HTTP $statusCode).")
        }
        if (statusCode !in 200..299 || !root.optBoolean("success", false)) {
            throw IOException(root.optString("message", "Không thể xử lý tài liệu."))
        }
        return root
    }

    suspend fun createFolder(
        context: Context,
        instrument: String,
        path: String,
        folderName: String
    ): String = withContext(Dispatchers.IO) {
        val cleanParentPath = path.replace('\\', '/').trim('/')
        val cleanFolderName = folderName.trim()
        // Put the operation in both the query string and the POST body.
        // This avoids IIS/FastCGI configurations that occasionally do not populate
        // PHP's $_POST for an application/x-www-form-urlencoded Android request.
        val requestUrl = buildString {
            append(ENDPOINT)
            append("?action=create_folder")
            append("&instrument=").append(encode(instrument))
            append("&path=").append(encode(cleanParentPath))
            append("&name=").append(encode(cleanFolderName))
            append("&_ts=").append(System.currentTimeMillis())
        }
        val body = formBody(
            "action" to "create_folder",
            "instrument" to instrument,
            "path" to cleanParentPath,
            "name" to cleanFolderName
        )

        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("User-Agent", "TunerTools-Android")
            applyAuthorization(context, this)
            setFixedLengthStreamingMode(body.size)
        }

        try {
            connection.outputStream.use { it.write(body) }
            val root = readJsonResponse(connection)
            val item = root.optJSONObject("item")
            val createdPath = item?.optString("path", "")
                ?.replace('\\', '/')
                ?.trim('/')
                .orEmpty()

            val confirmed = root.optString("action", "") == "create_folder" &&
                    root.optBoolean("created", false) &&
                    item?.optString("type", "") == "folder" &&
                    createdPath.isNotBlank()

            if (!confirmed) {
                throw IOException(
                    "Máy chủ chưa xác nhận việc tạo thư mục. Hãy chép đè api_documents.php mới lên IIS."
                )
            }
            invalidateInstrumentCache(context, instrument)
            root.optString("message", "Đã tạo thư mục công khai.")
                .ifBlank { "Đã tạo thư mục công khai." }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun uploadFile(
        context: Context,
        instrument: String,
        path: String,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        val info = queryLearningPickedFileInfo(context, uri)
        val extension = info.displayName.substringAfterLast('.', "").lowercase()
        val uploadFileName = if (extension.isBlank()) "upload.bin" else "upload.$extension"
        val boundary = "----TunerTools${System.currentTimeMillis()}"

        fun fieldPart(name: String, value: String): String = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n")
            append(value.replace("\r", " ").replace("\n", " "))
            append("\r\n")
        }

        val prefix = buildString {
            append(fieldPart("action", "upload"))
            append(fieldPart("instrument", instrument))
            append(fieldPart("path", path))
            append(fieldPart("display_name", info.displayName))
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(uploadFileName.replace("\"", "_"))
                .append("\"\r\n")
            append("Content-Type: ").append(info.mimeType).append("\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        val suffix = "\r\n--$boundary--\r\n".toByteArray(StandardCharsets.UTF_8)

        val requestUrl = buildString {
            append(ENDPOINT)
            append("?action=upload")
            append("&instrument=").append(encode(instrument))
            append("&path=").append(encode(path.replace('\\', '/').trim('/')))
            append("&_ts=").append(System.currentTimeMillis())
        }

        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("User-Agent", "TunerTools-Android")
            applyAuthorization(context, this)

            if (info.sizeBytes >= 0L) {
                setFixedLengthStreamingMode(prefix.size.toLong() + info.sizeBytes + suffix.size.toLong())
            } else {
                setChunkedStreamingMode(64 * 1024)
            }
        }

        try {
            BufferedOutputStream(connection.outputStream, 64 * 1024).use { output ->
                output.write(prefix)
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Không thể đọc tệp đã chọn.")
                input.use { it.copyTo(output, bufferSize = 64 * 1024) }
                output.write(suffix)
                output.flush()
            }

            val root = readJsonResponse(connection)
            val item = root.optJSONObject("item")
            val confirmed = root.optString("action", "") == "upload" &&
                    root.optBoolean("uploaded", false) &&
                    item != null &&
                    item.optString("path", "").isNotBlank()
            if (!confirmed) {
                throw IOException(
                    "Máy chủ chưa xác nhận việc tải tệp. Hãy chép đè api_documents.php mới lên IIS."
                )
            }
            invalidateInstrumentCache(context, instrument)
            root.optString("message", "Đã tải tài liệu lên cộng đồng.")
                .ifBlank { "Đã tải tài liệu lên cộng đồng." }
        } finally {
            connection.disconnect()
        }
    }


    suspend fun renameItem(
        context: Context,
        instrument: String,
        path: String,
        newName: String
    ): String = withContext(Dispatchers.IO) {
        val cleanPath = path.replace('\\', '/').trim('/')
        val body = formBody(
            "action" to "rename",
            "instrument" to instrument,
            "path" to cleanPath,
            "new_name" to newName.trim()
        )
        val requestUrl = buildString {
            append(ENDPOINT)
            append("?action=rename")
            append("&instrument=").append(encode(instrument))
            append("&path=").append(encode(cleanPath))
            append("&_ts=").append(System.currentTimeMillis())
        }
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 45_000
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", "TunerTools-Android")
            applyAuthorization(context, this)
            setFixedLengthStreamingMode(body.size)
        }
        try {
            connection.outputStream.use { it.write(body) }
            val root = readJsonResponse(connection)
            if (!root.optBoolean("renamed", false)) {
                throw IOException("Máy chủ chưa xác nhận việc đổi tên.")
            }
            invalidateInstrumentCache(context, instrument)
            root.optString("message", "Đã đổi tên thành công.")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun deleteItem(
        context: Context,
        instrument: String,
        path: String
    ): String = withContext(Dispatchers.IO) {
        val cleanPath = path.replace('\\', '/').trim('/')
        val body = formBody(
            "action" to "delete_item",
            "instrument" to instrument,
            "path" to cleanPath
        )
        val requestUrl = buildString {
            append(ENDPOINT)
            append("?action=delete_item")
            append("&instrument=").append(encode(instrument))
            append("&path=").append(encode(cleanPath))
            append("&_ts=").append(System.currentTimeMillis())
        }
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            useCaches = false
            doInput = true
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", "TunerTools-Android")
            applyAuthorization(context, this)
            setFixedLengthStreamingMode(body.size)
        }
        try {
            connection.outputStream.use { it.write(body) }
            val root = readJsonResponse(connection)
            if (!root.optBoolean("deleted", false)) {
                throw IOException("Máy chủ chưa xác nhận việc xóa.")
            }
            invalidateInstrumentCache(context, instrument)
            root.optString("message", "Đã xóa thành công.")
        } finally {
            connection.disconnect()
        }
    }

    private fun applyAuthorization(context: Context, connection: HttpURLConnection) {
        val token = DocumentAccessStore.sessionToken(context)
        if (token.isBlank()) {
            throw IOException("Vui lòng đăng nhập Google để truy cập tài liệu.")
        }
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("X-Document-Token", token)
    }

    private fun readJsonResponse(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseText.isBlank()) {
            throw IOException("Máy chủ không trả về dữ liệu.")
        }

        val cleanText = responseText.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        if (!cleanText.startsWith("{")) {
            throw IOException("Máy chủ không trả về JSON hợp lệ (HTTP $statusCode).")
        }

        val root = try {
            JSONObject(cleanText)
        } catch (_: Exception) {
            throw IOException("Không thể đọc phản hồi JSON từ máy chủ.")
        }

        if (statusCode !in 200..299 || !root.optBoolean("success", false)) {
            throw IOException(root.optString("message", "Không thể xử lý tài liệu."))
        }
        return root
    }

    private fun formBody(vararg pairs: Pair<String, String>): ByteArray = pairs.joinToString("&") {
        "${encode(it.first)}=${encode(it.second)}"
    }.toByteArray(StandardCharsets.UTF_8)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
}

object LearningLocalMaterials {
    private const val ROOT_FOLDER = "TunerTools/Materials"
    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")
    private val pdfExtensions = setOf("pdf")
    private val videoExtensions = setOf("mp4", "webm", "mov", "m4v", "mkv", "avi")

    suspend fun loadPage(
        context: Context,
        instrument: String,
        path: String
    ): LearningMaterialPage = withContext(Dispatchers.IO) {
        val root = instrumentRoot(context, instrument)
        val folder = resolveFolder(root, path, create = false)
        val items = if (folder == null || !folder.isDirectory) {
            emptyList()
        } else {
            folder.listFiles()
                ?.filterNot { it.name.startsWith('.') }
                ?.mapNotNull { file -> localItem(root, path, file) }
                ?.sortedWith(learningMaterialDisplayComparator(isVietnamese = true))
                .orEmpty()
        }

        LearningMaterialPage(
            instrument = instrument,
            available = items.isNotEmpty() || folder?.exists() == true,
            currentPath = path,
            parentPath = parentPath(path),
            breadcrumbs = buildLearningBreadcrumbs(instrument, path),
            items = items,
            message = ""
        )
    }

    suspend fun createFolder(
        context: Context,
        instrument: String,
        path: String,
        folderName: String
    ): String = withContext(Dispatchers.IO) {
        val safeName = sanitizeEntryName(folderName, isFile = false)
        val parent = resolveFolder(instrumentRoot(context, instrument), path, create = true)
            ?: throw IOException("Không thể tạo thư mục lưu trên máy.")
        val target = File(parent, safeName)
        if (target.exists()) throw IOException("Tên thư mục đã tồn tại trên máy.")
        if (!target.mkdir()) throw IOException("Không thể tạo thư mục trên máy.")
        "Đã tạo thư mục trên máy."
    }

    suspend fun importFile(
        context: Context,
        instrument: String,
        path: String,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        val info = queryLearningPickedFileInfo(context, uri)
        val safeName = sanitizeEntryName(info.displayName, isFile = true)
        if (classifyFile(safeName) == null) {
            throw IOException("Chỉ hỗ trợ hình ảnh, PDF và video.")
        }

        val parent = resolveFolder(instrumentRoot(context, instrument), path, create = true)
            ?: throw IOException("Không thể truy cập thư mục lưu trên máy.")
        val target = uniqueFile(parent, safeName)
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Không thể đọc tệp đã chọn.")

        input.use { source ->
            FileOutputStream(target).use { output ->
                source.copyTo(output, bufferSize = 64 * 1024)
            }
        }

        if (target.length() <= 0L) {
            target.delete()
            throw IOException("Tệp đã chọn không có dữ liệu.")
        }
        "Đã lưu tệp trên máy."
    }

    suspend fun renameItem(
        context: Context,
        instrument: String,
        localFilePath: String,
        newName: String
    ): String = withContext(Dispatchers.IO) {
        val root = instrumentRoot(context, instrument).canonicalFile
        val source = resolveManagedLocalItem(root, localFilePath)
        if (!source.exists()) throw IOException("Tệp hoặc thư mục trên máy không còn tồn tại.")

        val requestedName = newName.trim()
        if (requestedName.isBlank()) throw IOException("Tên mới không được để trống.")

        val finalName = if (source.isDirectory) {
            sanitizeEntryName(requestedName, isFile = false)
        } else {
            val oldExtension = source.extension
            val requestedExtension = requestedName.substringAfterLast('.', "")
            val requestedBase = if (
                oldExtension.isNotBlank() && requestedExtension.equals(oldExtension, ignoreCase = true)
            ) {
                requestedName.dropLast(oldExtension.length + 1)
            } else {
                requestedName
            }
            val safeBase = sanitizeEntryName(requestedBase, isFile = false)
            if (oldExtension.isBlank()) safeBase else "$safeBase.$oldExtension"
        }

        val parent = source.parentFile ?: throw IOException("Không thể xác định thư mục cha trên máy.")
        val target = File(parent, finalName).canonicalFile
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        if (!target.path.startsWith(rootPrefix)) {
            throw IOException("Đường dẫn đổi tên trên máy không hợp lệ.")
        }
        if (target.exists()) throw IOException("Tên mới đã tồn tại trên máy.")
        if (!source.renameTo(target)) throw IOException("Không thể đổi tên nội dung trên máy.")
        "Đã đổi tên nội dung trên máy."
    }

    suspend fun deleteItem(
        context: Context,
        instrument: String,
        localFilePath: String
    ): String = withContext(Dispatchers.IO) {
        val root = instrumentRoot(context, instrument).canonicalFile
        val target = resolveManagedLocalItem(root, localFilePath)
        if (!target.exists()) return@withContext "Nội dung trên máy đã được xóa."

        val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
        if (!deleted || target.exists()) throw IOException("Không thể xóa nội dung trên máy.")
        "Đã xóa nội dung khỏi máy."
    }

    private fun resolveManagedLocalItem(root: File, localFilePath: String): File {
        if (localFilePath.isBlank()) throw IOException("Thiếu đường dẫn nội dung trên máy.")
        val target = File(localFilePath).canonicalFile
        val rootPrefix = root.path.trimEnd(File.separatorChar) + File.separator
        if (target == root || !target.path.startsWith(rootPrefix)) {
            throw IOException("Không được phép thay đổi đường dẫn này trên máy.")
        }
        return target
    }

    private fun instrumentRoot(context: Context, instrument: String): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "documents")
        val safeInstrument = sanitizeEntryName(instrument, isFile = false)
        return File(base, "$ROOT_FOLDER/$safeInstrument").apply { mkdirs() }
    }

    private fun resolveFolder(root: File, relativePath: String, create: Boolean): File? {
        var current = root.canonicalFile
        val rootPath = current.path.trimEnd(File.separatorChar) + File.separator
        relativePath.replace('\\', '/').split('/').filter { it.isNotBlank() }.forEach { rawPart ->
            val part = sanitizeEntryName(rawPart, isFile = false)
            val next = File(current, part)
            if (create && !next.exists() && !next.mkdir()) return null
            current = next.canonicalFile
            if (current.path != root.canonicalPath && !current.path.startsWith(rootPath)) {
                throw IOException("Đường dẫn lưu trên máy không hợp lệ.")
            }
        }
        return current
    }

    private fun collectContainedCounts(folder: File): Map<LearningMaterialType, Int> {
        val result = mutableMapOf<LearningMaterialType, Int>()
        val children = folder.listFiles().orEmpty()
        for (child in children) {
            if (child.name.startsWith('.')) continue
            if (child.isDirectory) {
                collectContainedCounts(child).forEach { (type, count) ->
                    result[type] = (result[type] ?: 0) + count
                }
            } else if (child.isFile) {
                classifyFile(child.name)?.let { type ->
                    result[type] = (result[type] ?: 0) + 1
                }
            }
        }
        return result.filterValues { it > 0 }
    }

    private fun localItem(root: File, parentPath: String, file: File): LearningMaterialUiItem? {
        val itemPath = if (parentPath.isBlank()) file.name else "$parentPath/${file.name}"
        if (file.isDirectory) {
            val count = file.listFiles()?.count { child ->
                child.isDirectory || (child.isFile && classifyFile(child.name) != null)
            } ?: 0
            val containedCounts = collectContainedCounts(file)
            return LearningMaterialUiItem(
                id = "local-folder:${file.absolutePath}",
                parentId = parentPath,
                type = LearningMaterialType.FOLDER,
                titleVi = file.name,
                titleEn = file.name,
                path = itemPath,
                childrenCount = count,
                storage = LearningMaterialStorage.LOCAL,
                localFilePath = file.absolutePath,
                containedTypes = containedCounts.keys,
                containedTypesKnown = true,
                containedCounts = containedCounts,
                containedCountsKnown = true
            )
        }

        if (!file.isFile) return null
        val type = classifyFile(file.name) ?: return null
        val extension = file.extension.uppercase()
        val title = file.nameWithoutExtension.ifBlank { file.name }
        val uri = Uri.fromFile(file).toString()
        val meta = "$extension • ${humanFileSize(file.length())}"
        return LearningMaterialUiItem(
            id = "local-file:${file.absolutePath}",
            parentId = parentPath,
            type = type,
            titleVi = title,
            titleEn = title,
            metaVi = meta,
            metaEn = meta,
            previewUrl = uri,
            path = itemPath,
            childrenCount = 0,
            storage = LearningMaterialStorage.LOCAL,
            localFilePath = file.absolutePath,
            containedTypes = setOf(type),
            containedTypesKnown = true,
            containedCounts = mapOf(type to 1),
            containedCountsKnown = true
        )
    }

    private fun classifyFile(name: String): LearningMaterialType? {
        val extension = name.substringAfterLast('.', "").lowercase()
        return when {
            extension in imageExtensions -> LearningMaterialType.IMAGE
            extension in pdfExtensions -> LearningMaterialType.PDF
            extension in videoExtensions -> LearningMaterialType.VIDEO
            else -> null
        }
    }

    private fun sanitizeEntryName(value: String, isFile: Boolean): String {
        var name = value.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim(' ', '.')
        if (name.isBlank() || name == "." || name == "..") {
            throw IOException(if (isFile) "Tên tệp không hợp lệ." else "Tên thư mục không hợp lệ.")
        }
        if (name.length > 160) name = name.take(160).trimEnd(' ', '.')
        return name
    }

    private fun uniqueFile(parent: File, requestedName: String): File {
        var target = File(parent, requestedName)
        if (!target.exists()) return target
        val extension = requestedName.substringAfterLast('.', "")
        val base = if (extension.isBlank()) requestedName else requestedName.dropLast(extension.length + 1)
        var index = 1
        while (target.exists()) {
            val candidate = if (extension.isBlank()) "$base ($index)" else "$base ($index).$extension"
            target = File(parent, candidate)
            index++
        }
        return target
    }

    private fun humanFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        for (unit in units) {
            if (value < 1024.0 || unit == "TB") {
                val decimals = if (value >= 100) 0 else if (value >= 10) 1 else 2
                return String.format(java.util.Locale.US, "%.${decimals}f %s", value, unit)
            }
            value /= 1024.0
        }
        return "$bytes B"
    }

    private fun parentPath(path: String): String? {
        if (path.isBlank()) return null
        val index = path.lastIndexOf('/')
        return if (index < 0) "" else path.substring(0, index)
    }
}

private fun mergeContainedCounts(
    first: Map<LearningMaterialType, Int>,
    second: Map<LearningMaterialType, Int>
): Map<LearningMaterialType, Int> {
    val result = mutableMapOf<LearningMaterialType, Int>()
    (first.keys + second.keys).forEach { type ->
        result[type] = (first[type] ?: 0) + (second[type] ?: 0)
    }
    return result.filterValues { it > 0 }
}

private fun mergeLearningMaterialItems(
    publicItems: List<LearningMaterialUiItem>,
    localItems: List<LearningMaterialUiItem>
): List<LearningMaterialUiItem> {
    val folders = linkedMapOf<String, LearningMaterialUiItem>()
    val files = mutableListOf<LearningMaterialUiItem>()

    fun addItem(item: LearningMaterialUiItem) {
        if (item.type != LearningMaterialType.FOLDER) {
            files += item
            return
        }

        val key = item.path.replace('\\', '/').lowercase()
        val previous = folders[key]
        folders[key] = if (previous == null) {
            item
        } else {
            previous.copy(
                id = "mixed-folder:${item.path}",
                childrenCount = previous.childrenCount + item.childrenCount,
                storage = LearningMaterialStorage.MIXED,
                localFilePath = previous.localFilePath.ifBlank { item.localFilePath },
                containedTypes = previous.containedTypes + item.containedTypes,
                containedTypesKnown = previous.containedTypesKnown && item.containedTypesKnown,
                containedCounts = mergeContainedCounts(previous.containedCounts, item.containedCounts),
                containedCountsKnown = previous.containedCountsKnown && item.containedCountsKnown,
                isProtectedSystemFolder = previous.isProtectedSystemFolder || item.isProtectedSystemFolder
            )
        }
    }

    publicItems.forEach(::addItem)
    localItems.forEach(::addItem)
    return folders.values + files
}

private fun learningMaterialTitle(item: LearningMaterialUiItem, isVietnamese: Boolean): String =
    if (isVietnamese) item.titleVi else item.titleEn

// Natural numeric ordering keeps lesson titles in human order:
// Bai 1, Bai 2, ... Bai 10, Bai 10.1, Bai 11.
// Vietnamese accents are normalized so both "Bai" and "Bài" are recognized.
private val learningLessonNumberRegex =
    Regex("""(?:^|[^a-z0-9])bai\s*(?:so\s*)?[-_.:]?\s*(\d+)""")
private val learningNaturalSortTokenRegex = Regex("""\d+|\D+""")
private val learningCombiningMarkRegex = Regex("""\p{M}+""")

private fun normalizeLearningSortText(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(learningCombiningMarkRegex, "")
        .lowercase(java.util.Locale.ROOT)
        .replace('đ', 'd')
        .trim()

private fun learningMaterialLessonNumber(title: String): Long? {
    val normalizedTitle = normalizeLearningSortText(title)
    return learningLessonNumberRegex
        .find(normalizedTitle)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
}

private fun compareLearningNaturalText(firstValue: String, secondValue: String): Int {
    val first = normalizeLearningSortText(firstValue)
    val second = normalizeLearningSortText(secondValue)
    val firstTokens = learningNaturalSortTokenRegex.findAll(first).map { it.value }.toList()
    val secondTokens = learningNaturalSortTokenRegex.findAll(second).map { it.value }.toList()
    val commonSize = minOf(firstTokens.size, secondTokens.size)

    for (index in 0 until commonSize) {
        val firstToken = firstTokens[index]
        val secondToken = secondTokens[index]
        val firstIsNumber = firstToken.all { it.isDigit() }
        val secondIsNumber = secondToken.all { it.isDigit() }

        val tokenComparison = when {
            firstIsNumber && secondIsNumber -> {
                val firstNumber = firstToken.trimStart('0').ifBlank { "0" }
                val secondNumber = secondToken.trimStart('0').ifBlank { "0" }
                when {
                    firstNumber.length != secondNumber.length ->
                        firstNumber.length.compareTo(secondNumber.length)
                    firstNumber != secondNumber -> firstNumber.compareTo(secondNumber)
                    else -> firstToken.length.compareTo(secondToken.length)
                }
            }
            firstIsNumber != secondIsNumber -> if (firstIsNumber) -1 else 1
            else -> firstToken.compareTo(secondToken)
        }

        if (tokenComparison != 0) return tokenComparison
    }

    return when {
        firstTokens.size != secondTokens.size -> firstTokens.size.compareTo(secondTokens.size)
        first != second -> first.compareTo(second)
        else -> firstValue.compareTo(secondValue, ignoreCase = true)
    }
}

private fun learningMaterialDisplayComparator(
    isVietnamese: Boolean
): Comparator<LearningMaterialUiItem> = Comparator { firstItem, secondItem ->
    // Folders remain before files, matching the existing layout.
    val firstTypeRank = if (firstItem.type == LearningMaterialType.FOLDER) 0 else 1
    val secondTypeRank = if (secondItem.type == LearningMaterialType.FOLDER) 0 else 1
    if (firstTypeRank != secondTypeRank) {
        return@Comparator firstTypeRank.compareTo(secondTypeRank)
    }

    val firstTitle = learningMaterialTitle(firstItem, isVietnamese)
    val secondTitle = learningMaterialTitle(secondItem, isVietnamese)
    val firstLessonNumber = learningMaterialLessonNumber(firstTitle)
    val secondLessonNumber = learningMaterialLessonNumber(secondTitle)

    // General documents without a lesson number stay before numbered lessons.
    when {
        firstLessonNumber == null && secondLessonNumber != null -> return@Comparator -1
        firstLessonNumber != null && secondLessonNumber == null -> return@Comparator 1
        firstLessonNumber != null && secondLessonNumber != null -> {
            val lessonComparison = firstLessonNumber.compareTo(secondLessonNumber)
            if (lessonComparison != 0) return@Comparator lessonComparison
        }
    }

    compareLearningNaturalText(firstTitle, secondTitle)
}

private fun learningMaterialDescription(item: LearningMaterialUiItem, isVietnamese: Boolean): String =
    if (isVietnamese) item.descriptionVi else item.descriptionEn

private fun learningMaterialMeta(item: LearningMaterialUiItem, isVietnamese: Boolean): String =
    if (isVietnamese) item.metaVi else item.metaEn

private fun learningMaterialStorageLabel(
    storage: LearningMaterialStorage,
    lang: String
): String = when (storage) {
    LearningMaterialStorage.PUBLIC -> learningMaterialText(lang, "lm_public", "Công khai", "Public")
    LearningMaterialStorage.LOCAL -> learningMaterialText(lang, "lm_on_device", "Trên máy", "On device")
    LearningMaterialStorage.MIXED -> learningMaterialText(lang, "lm_mixed", "Công khai + máy", "Public + device")
}

@Suppress("UNUSED_PARAMETER")
private fun learningMaterialAccent(type: LearningMaterialType): Color =
    Color(0xFF8A919B) // restrained monochrome accent

private fun learningMaterialTypeLabel(type: LearningMaterialType, lang: String): String = when (type) {
    LearningMaterialType.FOLDER -> learningMaterialText(lang, "lm_folder", "THƯ MỤC", "FOLDER")
    LearningMaterialType.IMAGE -> learningMaterialText(lang, "lm_images", "HÌNH ẢNH", "IMAGES")
    LearningMaterialType.PDF -> "PDF"
    LearningMaterialType.VIDEO -> learningMaterialText(lang, "lm_videos", "VIDEO", "VIDEOS")
}


private object LearningVideoThumbnailLoader {
    private const val MAX_FRAME_SIDE_PX = 360
    private const val MEMORY_CACHE_KB = 16 * 1024
    private const val DISK_CACHE_FOLDER = "learning_video_thumbnails"

    // Chi cho phep toi da hai video doc frame cung luc de tranh tao qua nhieu
    // ket noi mang khi danh sach co nhieu video dang hien tren man hinh.
    private val extractionSlots = Semaphore(2, true)

    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun sourceKey(item: LearningMaterialUiItem): String {
        val localPath = item.localFilePath.trim()
        if (localPath.isNotBlank()) {
            val file = File(localPath)
            return buildString {
                append("local:")
                append(file.absolutePath)
                append('|')
                append(file.length())
                append('|')
                append(file.lastModified())
            }
        }

        val remoteUrl = learningMaterialViewUrl(item).trim()
        return if (remoteUrl.isNotBlank()) {
            "remote:$remoteUrl"
        } else {
            "item:${item.id}:${item.path}"
        }
    }

    fun peek(key: String): Bitmap? = synchronized(memoryCache) {
        memoryCache.get(key)
    }

    suspend fun load(
        context: Context,
        item: LearningMaterialUiItem
    ): ImageBitmap? = withContext(Dispatchers.IO) {
        val key = sourceKey(item)

        synchronized(memoryCache) {
            memoryCache.get(key)
        }?.let { return@withContext it.asImageBitmap() }

        val cacheFile = thumbnailCacheFile(context, key)
        if (cacheFile.isFile && cacheFile.length() > 0L) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { bitmap ->
                synchronized(memoryCache) {
                    memoryCache.put(key, bitmap)
                }
                return@withContext bitmap.asImageBitmap()
            }
        }

        extractionSlots.acquire()
        try {
            // Kiem tra lai sau khi cho slot; mot coroutine khac co the da tao xong anh.
            synchronized(memoryCache) {
                memoryCache.get(key)
            }?.let { return@withContext it.asImageBitmap() }

            if (cacheFile.isFile && cacheFile.length() > 0L) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)?.let { bitmap ->
                    synchronized(memoryCache) {
                        memoryCache.put(key, bitmap)
                    }
                    return@withContext bitmap.asImageBitmap()
                }
            }

            val source = item.localFilePath.trim().ifBlank {
                learningMaterialViewUrl(item).trim()
            }
            if (source.isBlank()) return@withContext null

            val frame = extractFrame(context, source) ?: return@withContext null
            val scaledFrame = scaleFrame(frame)

            synchronized(memoryCache) {
                memoryCache.put(key, scaledFrame)
            }
            saveToDiskCache(cacheFile, scaledFrame)

            scaledFrame.asImageBitmap()
        } catch (_: Exception) {
            null
        } finally {
            extractionSlots.release()
        }
    }

    private fun extractFrame(context: Context, source: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            when {
                source.startsWith("content://", ignoreCase = true) ||
                        source.startsWith("file://", ignoreCase = true) -> {
                    retriever.setDataSource(context, Uri.parse(source))
                }

                source.startsWith("http://", ignoreCase = true) ||
                        source.startsWith("https://", ignoreCase = true) -> {
                    val headers = linkedMapOf(
                        "User-Agent" to "TunerTools-Android",
                        "Accept" to "video/*,*/*"
                    )
                    val sessionToken = DocumentAccessStore.sessionToken(context)
                    if (sessionToken.isNotBlank()) {
                        headers["Authorization"] = "Bearer $sessionToken"
                        headers["X-Document-Token"] = sessionToken
                    }
                    retriever.setDataSource(source, headers)
                }

                else -> retriever.setDataSource(source)
            }

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L

            // Lay frame vao khoang 10% video (toi da 5 giay) de tranh man hinh den
            // thuong xuat hien o frame dau tien. Video ngan se lay o khoang giua.
            val targetMs = when {
                durationMs <= 0L -> 1_000L
                durationMs < 2_000L -> (durationMs / 2L).coerceAtLeast(0L)
                else -> (durationMs / 10L).coerceIn(1_000L, 5_000L)
            }

            val targetUs = targetMs * 1_000L
            return retriever.getFrameAtTime(
                targetUs,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: retriever.getFrameAtTime(
                0L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleFrame(source: Bitmap): Bitmap {
        val maxSide = maxOf(source.width, source.height).coerceAtLeast(1)
        if (maxSide <= MAX_FRAME_SIDE_PX) return source

        val scale = MAX_FRAME_SIDE_PX.toFloat() / maxSide.toFloat()
        val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
        val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)

        if (scaled !== source) {
            runCatching { source.recycle() }
        }
        return scaled
    }

    private fun thumbnailCacheFile(context: Context, key: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { byte ->
                "%02x".format(java.util.Locale.ROOT, byte.toInt() and 0xff)
            }

        val directory = File(context.cacheDir, DISK_CACHE_FOLDER).apply { mkdirs() }
        return File(directory, "$digest.jpg")
    }

    private fun saveToDiskCache(target: File, bitmap: Bitmap) {
        runCatching {
            target.parentFile?.mkdirs()
            val temporary = File(target.absolutePath + ".tmp")
            if (temporary.exists()) temporary.delete()

            FileOutputStream(temporary).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, output)
                output.flush()
            }

            if (temporary.length() > 0L) {
                if (target.exists()) target.delete()
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            } else {
                temporary.delete()
            }
        }
    }
}

@Composable
private fun LearningVideoThumbnail(
    item: LearningMaterialUiItem,
    fallbackTint: Color
) {
    val context = LocalContext.current
    val sourceKey = remember(
        item.id,
        item.previewUrl,
        item.downloadUrl,
        item.localFilePath,
        item.path
    ) {
        LearningVideoThumbnailLoader.sourceKey(item)
    }

    val initialBitmap = remember(sourceKey) {
        LearningVideoThumbnailLoader.peek(sourceKey)?.asImageBitmap()
    }

    val thumbnail by produceState<ImageBitmap?>(
        initialValue = initialBitmap,
        key1 = sourceKey
    ) {
        if (value == null) {
            value = LearningVideoThumbnailLoader.load(context, item)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!,
                contentDescription = learningMaterialTitle(
                    item,
                    LocalLearningMaterialsLanguage.current == "vi"
                ),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Lop mo nhe giup nut Play van ro tren ca khung hinh sang va toi.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .size(25.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.62f))
                    .border(1.dp, Color.White.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(17.dp)
                        .offset(x = 1.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun LearningMaterialRow(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    childrenCount: Int,
    allowLocalManagement: Boolean,
    isAccessLocked: Boolean,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onRenamePublic: () -> Unit,
    onDeletePublic: () -> Unit,
    onRenameLocal: () -> Unit,
    onDeleteLocal: () -> Unit
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val isFolder = item.type == LearningMaterialType.FOLDER
    val isMainFolder = isFolder && item.parentId.isBlank()
    val mainFolderColor = Color(0xFFF2B94B)
    val monochromeIconColor = textColor.copy(alpha = 0.80f)
    val itemIconColor = if (isMainFolder) mainFolderColor else monochromeIconColor
    val itemIconBackground = if (isMainFolder) {
        mainFolderColor.copy(alpha = 0.14f)
    } else {
        textColor.copy(alpha = 0.055f)
    }
    val itemIconBorder = if (isMainFolder) {
        mainFolderColor.copy(alpha = 0.34f)
    } else {
        textColor.copy(alpha = 0.12f)
    }
    var adminMenuExpanded by remember(item.id) { mutableStateOf(false) }
    // Public actions are available to Admin or the server-confirmed owner.
    // Local actions are normally available on the device, except for the protected
    // Community root: normal users must not see or use the three-dot menu there.
    val canManagePublic = item.canManageItem && item.storage != LearningMaterialStorage.LOCAL
    val hasLocalCopy = allowLocalManagement &&
            item.localFilePath.isNotBlank() &&
            item.storage != LearningMaterialStorage.PUBLIC
    val showOptionsMenu = canManagePublic || hasLocalCopy

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isFolder) onOpen() else onPreview() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(itemIconBackground)
                    .border(1.dp, itemIconBorder, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (item.type) {
                    LearningMaterialType.FOLDER -> Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = itemIconColor,
                        modifier = Modifier.size(28.dp)
                    )

                    LearningMaterialType.IMAGE -> {
                        val extensionLabel = learningMaterialExtension(item)
                            .uppercase(java.util.Locale.ROOT)
                            .ifBlank { "IMG" }
                            .take(4)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Collections,
                                contentDescription = extensionLabel,
                                tint = itemIconColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = extensionLabel,
                                color = itemIconColor,
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    LearningMaterialType.PDF -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "PDF",
                                tint = itemIconColor,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "PDF",
                                color = itemIconColor,
                                fontSize = 8.sp,
                                lineHeight = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }

                    LearningMaterialType.VIDEO -> LearningVideoThumbnail(
                        item = item,
                        fallbackTint = itemIconColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = learningMaterialTitle(item, isVietnamese),
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = if (isMainFolder) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                val description = learningMaterialDescription(item, isVietnamese)
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = description,
                        color = textColor.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                val storageLabel = learningMaterialStorageLabel(item.storage, materialLang)
                val detailText = if (isFolder) {
                    val countText = learningMaterialText(
                        materialLang,
                        "lm_items_inside",
                        "%1\$d mục bên trong",
                        "%1\$d items inside",
                        childrenCount
                    )
                    "$countText • $storageLabel"
                } else {
                    listOf(learningMaterialMeta(item, isVietnamese), storageLabel)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                }
                Text(
                    text = detailText,
                    color = textColor.copy(alpha = 0.50f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            if (isFolder) {
                IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
                    if (isAccessLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = learningMaterialText(
                                materialLang,
                                "doc_access_denied",
                                "Thư mục cần đăng ký quyền",
                                "Folder requires access"
                            ),
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(19.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.58f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            } else if (item.storage != LearningMaterialStorage.LOCAL && item.downloadUrl.isNotBlank()) {
                IconButton(onClick = onDownload, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = learningMaterialText(materialLang, "lm_download", "Tải xuống", "Download"),
                        tint = textColor.copy(alpha = 0.60f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showOptionsMenu) {
                Box {
                    IconButton(
                        onClick = { adminMenuExpanded = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = if (isVietnamese) "Tùy chọn" else "Options",
                            tint = textColor.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = adminMenuExpanded,
                        onDismissRequest = { adminMenuExpanded = false },
                        containerColor = surfaceColor
                    ) {
                        if (canManagePublic) {
                            val publicRenameLabel = if (item.storage == LearningMaterialStorage.MIXED) {
                                if (isVietnamese) "Đổi tên trên máy chủ" else "Rename on server"
                            } else {
                                if (isVietnamese) "Đổi tên" else "Rename"
                            }
                            val publicDeleteLabel = if (item.storage == LearningMaterialStorage.MIXED) {
                                if (isVietnamese) "Xóa khỏi máy chủ" else "Delete from server"
                            } else {
                                if (isVietnamese) "Xóa" else "Delete"
                            }

                            DropdownMenuItem(
                                text = { Text(publicRenameLabel, color = textColor) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.75f)
                                    )
                                },
                                onClick = {
                                    adminMenuExpanded = false
                                    onRenamePublic()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(publicDeleteLabel, color = Color.Red) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                },
                                onClick = {
                                    adminMenuExpanded = false
                                    onDeletePublic()
                                }
                            )
                        }

                        if (canManagePublic && hasLocalCopy) {
                            HorizontalDivider(color = textColor.copy(alpha = 0.10f))
                        }

                        if (hasLocalCopy) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isVietnamese) "Đổi tên trên máy" else "Rename on device",
                                        color = textColor
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.75f)
                                    )
                                },
                                onClick = {
                                    adminMenuExpanded = false
                                    onRenameLocal()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (isVietnamese) "Xóa khỏi máy" else "Delete from device",
                                        color = Color.Red
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = Color.Red
                                    )
                                },
                                onClick = {
                                    adminMenuExpanded = false
                                    onDeleteLocal()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun learningMaterialExtension(item: LearningMaterialUiItem): String {
    val pathExtension = item.path
        .substringAfterLast('/')
        .substringAfterLast('.', "")
        .trim()
        .lowercase()

    if (pathExtension.isNotBlank()) return pathExtension

    return when (item.type) {
        LearningMaterialType.IMAGE -> "jpg"
        LearningMaterialType.PDF -> "pdf"
        LearningMaterialType.VIDEO -> "mp4"
        LearningMaterialType.FOLDER -> "bin"
    }
}

private fun learningMaterialMimeType(item: LearningMaterialUiItem): String = when (item.type) {
    LearningMaterialType.IMAGE -> "image/*"
    LearningMaterialType.PDF -> "application/pdf"
    LearningMaterialType.VIDEO -> "video/*"
    LearningMaterialType.FOLDER -> "application/octet-stream"
}

private fun learningMaterialFileName(item: LearningMaterialUiItem): String {
    val pathName = item.path.substringAfterLast('/').trim()
    val extension = learningMaterialExtension(item)
    val title = item.titleVi.ifBlank { item.titleEn }.ifBlank { "tai_lieu" }
    val candidate = if (pathName.contains('.') && pathName.substringAfterLast('.').isNotBlank()) {
        pathName
    } else {
        "$title.$extension"
    }

    return candidate
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .trim()
        .ifBlank { "tai_lieu.$extension" }
}

private fun learningMaterialViewUrl(item: LearningMaterialUiItem): String =
    item.previewUrl.ifBlank { item.downloadUrl }

private fun enqueueLearningMaterialDownload(
    context: Context,
    item: LearningMaterialUiItem,
    lang: String
) {
    val isVietnamese = lang == "vi"
    if (item.storage == LearningMaterialStorage.LOCAL) {
        android.widget.Toast.makeText(
            context,
            learningMaterialText(lang, "lm_already_local", "Tệp này đã được lưu trên máy.", "This file is already stored on the device."),
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }

    val url = item.downloadUrl.ifBlank { item.previewUrl }
    if (url.isBlank()) {
        android.widget.Toast.makeText(
            context,
            learningMaterialText(lang, "lm_no_download_url", "Không có đường dẫn tải xuống.", "No download link is available."),
            android.widget.Toast.LENGTH_SHORT
        ).show()
        return
    }

    try {
        val fileName = learningMaterialFileName(item)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle(learningMaterialTitle(item, isVietnamese))
            setDescription(learningMaterialText(lang, "lm_downloading", "Đang tải tài liệu...", "Downloading material..."))
            setMimeType(learningMaterialMimeType(item))
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            addRequestHeader("User-Agent", "TunerTools-Android")
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "TunerTools/$fileName"
            )
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        android.widget.Toast.makeText(
            context,
            learningMaterialText(lang, "lm_download_started", "Đã bắt đầu tải xuống.", "Download started."),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    } catch (_: Exception) {
        android.widget.Toast.makeText(
            context,
            learningMaterialText(lang, "lm_download_failed", "Không thể tải tài liệu này.", "Unable to download this material."),
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

private suspend fun cacheLearningMaterial(
    context: Context,
    url: String,
    item: LearningMaterialUiItem
): File = withContext(Dispatchers.IO) {
    if (item.storage == LearningMaterialStorage.LOCAL && item.localFilePath.isNotBlank()) {
        val localFile = File(item.localFilePath)
        if (!localFile.isFile || localFile.length() <= 0L) {
            throw IOException("Tệp lưu trên máy không còn tồn tại.")
        }
        return@withContext localFile
    }

    if (url.startsWith("file://", ignoreCase = true)) {
        val localPath = Uri.parse(url).path.orEmpty()
        val localFile = File(localPath)
        if (!localFile.isFile || localFile.length() <= 0L) {
            throw IOException("Tệp lưu trên máy không còn tồn tại.")
        }
        return@withContext localFile
    }

    if (url.isBlank()) throw IOException("Đường dẫn tài liệu trống.")

    val extension = learningMaterialExtension(item)
    val cacheName = "learning_${Integer.toHexString(url.hashCode())}.$extension"
    val targetFile = File(context.cacheDir, cacheName)

    if (targetFile.exists() && targetFile.length() > 0L) {
        return@withContext targetFile
    }

    val temporaryFile = File(targetFile.absolutePath + ".part")
    var connection: HttpURLConnection? = null

    try {
        targetFile.parentFile?.mkdirs()
        if (temporaryFile.exists()) temporaryFile.delete()

        connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 90_000
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("User-Agent", "TunerTools-Android")
            setRequestProperty("Accept", "*/*")
        }

        val statusCode = connection.responseCode
        if (statusCode !in 200..299) {
            throw IOException("Máy chủ trả về mã lỗi $statusCode.")
        }

        connection.inputStream.use { input ->
            FileOutputStream(temporaryFile).use { output ->
                input.copyTo(output, bufferSize = 64 * 1024)
            }
        }

        if (temporaryFile.length() <= 0L) {
            throw IOException("Tệp tải về không có dữ liệu.")
        }

        if (targetFile.exists()) targetFile.delete()
        if (!temporaryFile.renameTo(targetFile)) {
            temporaryFile.copyTo(targetFile, overwrite = true)
            temporaryFile.delete()
        }

        targetFile
    } catch (error: Exception) {
        temporaryFile.delete()
        throw error
    } finally {
        connection?.disconnect()
    }
}

private fun learningPdfPageCount(file: File): Int {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    return try {
        renderer.pageCount
    } finally {
        renderer.close()
        descriptor.close()
    }
}

private suspend fun renderLearningPdfPage(
    file: File,
    pageIndex: Int,
    targetWidthPx: Int
): ImageBitmap = withContext(Dispatchers.IO) {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)

    try {
        val page = renderer.openPage(pageIndex)
        try {
            val safeWidth = targetWidthPx.coerceAtLeast(1)
            val scale = safeWidth.toFloat() / page.width.toFloat().coerceAtLeast(1f)
            val targetHeight = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(safeWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            val matrix = Matrix().apply { setScale(scale, scale) }
            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap.asImageBitmap()
        } finally {
            page.close()
        }
    } finally {
        renderer.close()
        descriptor.close()
    }
}

@Composable
private fun LearningMaterialViewerError(
    message: String,
    isVietnamese: Boolean,
    textColor: Color,
    tunaGreen: Color
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFEF5350),
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = learningMaterialText(materialLang, "lm_display_error", "Không thể hiển thị tài liệu", "Unable to display this material"),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            color = textColor.copy(alpha = 0.58f),
            fontSize = 12.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = learningMaterialText(materialLang, "lm_display_error_hint", "Bạn vẫn có thể dùng nút tải xuống ở phía trên.", "You can still use the download button above."),
            color = tunaGreen,
            fontSize = 11.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun LearningPdfPage(
    file: File,
    pageIndex: Int,
    pageCount: Int,
    textColor: Color,
    surfaceColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    ) {
        val availableWidth = this.maxWidth
        val density = LocalDensity.current
        val targetWidthPx = with(density) { availableWidth.roundToPx() }.coerceAtLeast(1)
        var renderError by remember(file.absolutePath, pageIndex, targetWidthPx) { mutableStateOf<String?>(null) }
        val pageBitmap by produceState<ImageBitmap?>(
            initialValue = null,
            file.absolutePath,
            pageIndex,
            targetWidthPx
        ) {
            value = try {
                renderLearningPdfPage(file, pageIndex, targetWidthPx)
            } catch (error: Exception) {
                renderError = error.message ?: "Không thể hiển thị trang PDF."
                null
            }
        }

        when {
            renderError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = renderError.orEmpty(),
                        color = textColor.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }

            pageBitmap == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFEF5350), strokeWidth = 2.dp)
                }
            }

            else -> {
                val bitmap = pageBitmap!!
                Column(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "PDF ${pageIndex + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)),
                        contentScale = ContentScale.FillWidth
                    )
                    Text(
                        text = "${pageIndex + 1} / $pageCount",
                        color = textColor.copy(alpha = 0.50f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LearningPdfViewer(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val context = LocalContext.current
    val url = learningMaterialViewUrl(item)
    var pdfFile by remember(url) { mutableStateOf<File?>(null) }
    var pageCount by remember(url) { mutableIntStateOf(0) }
    var loadError by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        pdfFile = null
        pageCount = 0
        loadError = null

        try {
            val file = cacheLearningMaterial(context, url, item)
            val count = withContext(Dispatchers.IO) { learningPdfPageCount(file) }
            if (count <= 0) throw IOException("Tệp PDF không có trang nào.")
            pdfFile = file
            pageCount = count
        } catch (error: Exception) {
            loadError = error.message ?: learningMaterialText(
                materialLang,
                "lm_pdf_error",
                "Không thể tải tệp PDF.",
                "Unable to load the PDF file."
            )
        }
    }

    when {
        loadError != null -> LearningMaterialViewerError(
            message = loadError.orEmpty(),
            isVietnamese = isVietnamese,
            textColor = textColor,
            tunaGreen = tunaGreen
        )

        pdfFile == null || pageCount <= 0 -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = tunaGreen)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = learningMaterialText(materialLang, "lm_pdf_loading", "Đang tải PDF...", "Loading PDF..."),
                    color = textColor.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(count = pageCount, key = { it }) { pageIndex ->
                LearningPdfPage(
                    file = pdfFile!!,
                    pageIndex = pageIndex,
                    pageCount = pageCount,
                    textColor = textColor,
                    surfaceColor = surfaceColor
                )
            }
        }
    }
}

private fun formatLearningVideoTime(timeMs: Int): String {
    val totalSeconds = (timeMs.coerceAtLeast(0) / 1000)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

@Composable
private fun LearningVideoViewer(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    textColor: Color,
    tunaGreen: Color,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    hasPreviousVideo: Boolean,
    hasNextVideo: Boolean,
    onPreviousVideo: () -> Unit,
    onNextVideo: () -> Unit
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val context = LocalContext.current
    val url = learningMaterialViewUrl(item)
    var videoError by remember(url) { mutableStateOf<String?>(null) }
    var isPreparing by remember(url) { mutableStateOf(true) }
    var videoView by remember(url) { mutableStateOf<VideoView?>(null) }
    var mediaPlayer by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var isVideoPlaying by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableIntStateOf(0) }
    var currentPositionMs by remember(url) { mutableIntStateOf(0) }
    var isSeeking by remember(url) { mutableStateOf(false) }
    var speedMenuExpanded by remember { mutableStateOf(false) }

    val speedOptions = remember {
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)
    }

    fun speedLabel(speed: Float): String {
        return if (kotlin.math.abs(speed - speed.toInt()) < 0.001f) {
            "${speed.toInt()}.0x"
        } else {
            "${speed}x"
        }
    }

    fun applyPlaybackSpeed(speed: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            android.widget.Toast.makeText(
                context,
                learningMaterialText(
                    materialLang,
                    "lm_speed_unsupported",
                    "Thiết bị này không hỗ trợ thay đổi tốc độ phát.",
                    "This device does not support playback-speed changes."
                ),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }

        onPlaybackSpeedChange(speed)
        val wasPlaying = runCatching { videoView?.isPlaying == true }.getOrDefault(false)
        runCatching {
            mediaPlayer?.playbackParams = android.media.PlaybackParams()
                .setSpeed(speed)
                .setPitch(1f)
        }.onFailure {
            android.widget.Toast.makeText(
                context,
                learningMaterialText(materialLang, "lm_speed_error", "Không thể đổi tốc độ phát.", "Unable to change playback speed."),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        if (!wasPlaying) {
            runCatching { videoView?.pause() }
        }
    }

    DisposableEffect(url) {
        onDispose {
            runCatching { videoView?.stopPlayback() }
            mediaPlayer = null
            videoView = null
        }
    }

    LaunchedEffect(videoView, isPreparing, videoError) {
        while (isActive) {
            val view = videoView
            if (view != null && !isPreparing && videoError == null) {
                runCatching {
                    val actualDuration = view.duration.coerceAtLeast(0)
                    if (actualDuration > 0) durationMs = actualDuration
                    if (!isSeeking) currentPositionMs = view.currentPosition.coerceAtLeast(0)
                    isVideoPlaying = view.isPlaying
                }
            }
            delay(250L)
        }
    }

    if (url.isBlank()) {
        LearningMaterialViewerError(
            message = learningMaterialText(materialLang, "lm_no_video_url", "Không có đường dẫn video.", "No video link is available."),
            isVietnamese = isVietnamese,
            textColor = textColor,
            tunaGreen = tunaGreen
        )
        return
    }

    // Video and controls are laid out in separate rows. The progress bar therefore
    // consumes its own height and never covers the picture in portrait or landscape.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            key(url) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        VideoView(viewContext).also { view ->
                            videoView = view
                            view.tag = url
                            view.setOnPreparedListener { player ->
                                mediaPlayer = player
                                isPreparing = false
                                player.isLooping = false
                                durationMs = player.duration.coerceAtLeast(0)

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    runCatching {
                                        player.playbackParams = android.media.PlaybackParams()
                                            .setSpeed(playbackSpeed.coerceIn(0.5f, 3f))
                                            .setPitch(1f)
                                    }
                                }

                                view.start()
                                isVideoPlaying = true
                            }
                            view.setOnCompletionListener {
                                currentPositionMs = durationMs
                                isVideoPlaying = false
                            }
                            view.setOnErrorListener { _, _, _ ->
                                isPreparing = false
                                isVideoPlaying = false
                                videoError = learningMaterialText(
                                    materialLang,
                                    "lm_video_error",
                                    "Thiết bị không phát được định dạng video này.",
                                    "This device cannot play the video format."
                                )
                                true
                            }
                            view.setVideoURI(Uri.parse(url))
                            view.requestFocus()
                        }
                    },
                    update = { view -> videoView = view }
                )
            }

            if (isPreparing && videoError == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = tunaGreen)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = learningMaterialText(materialLang, "lm_preparing_video", "Đang chuẩn bị video...", "Preparing video..."),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            videoError?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    LearningMaterialViewerError(
                        message = message,
                        isVietnamese = isVietnamese,
                        textColor = Color.White,
                        tunaGreen = tunaGreen
                    )
                }
            }
        }

        if (!isPreparing && videoError == null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF080808),
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Slider(
                        value = currentPositionMs
                            .coerceIn(0, durationMs.coerceAtLeast(0))
                            .toFloat(),
                        onValueChange = { value ->
                            isSeeking = true
                            currentPositionMs = value.roundToInt()
                        },
                        onValueChangeFinished = {
                            runCatching { videoView?.seekTo(currentPositionMs) }
                            isSeeking = false
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = tunaGreen,
                            activeTrackColor = tunaGreen,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousVideo,
                            enabled = hasPreviousVideo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = learningMaterialText(materialLang, "lm_previous_video", "Video trước", "Previous video"),
                                tint = if (hasPreviousVideo) Color.White else Color.White.copy(alpha = 0.28f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val view = videoView ?: return@IconButton
                                if (runCatching { view.isPlaying }.getOrDefault(false)) {
                                    view.pause()
                                    isVideoPlaying = false
                                } else {
                                    view.start()
                                    isVideoPlaying = true
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isVideoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isVideoPlaying) {
                                    learningMaterialText(materialLang, "lm_pause", "Tạm dừng", "Pause")
                                } else {
                                    learningMaterialText(materialLang, "lm_play", "Phát", "Play")
                                },
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }

                        IconButton(
                            onClick = onNextVideo,
                            enabled = hasNextVideo,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = learningMaterialText(materialLang, "lm_next_video", "Video tiếp theo", "Next video"),
                                tint = if (hasNextVideo) Color.White else Color.White.copy(alpha = 0.28f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        Text(
                            text = "${formatLearningVideoTime(currentPositionMs)} / ${formatLearningVideoTime(durationMs)}",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { speedMenuExpanded = true },
                                color = tunaGreen,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, tunaGreen)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = speedLabel(playbackSpeed),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = learningMaterialText(materialLang, "lm_select_speed", "Chọn tốc độ", "Select speed"),
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false },
                                modifier = Modifier
                                    .heightIn(max = 340.dp)
                                    .background(Color(0xFF1B1B1B))
                            ) {
                                Text(
                                    text = learningMaterialText(materialLang, "lm_playback_speed", "Tốc độ phát", "Playback speed"),
                                    color = Color.White.copy(alpha = 0.62f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                speedOptions.forEach { speed ->
                                    val selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (selected) {
                                                    "✓  ${speedLabel(speed)}"
                                                } else {
                                                    "    ${speedLabel(speed)}"
                                                },
                                                color = if (selected) tunaGreen else Color.White,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            speedMenuExpanded = false
                                            applyPlaybackSpeed(speed)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isFullscreen) {
                                    learningMaterialText(materialLang, "lm_exit_fullscreen", "Thu nhỏ màn hình", "Exit full screen")
                                } else {
                                    learningMaterialText(materialLang, "lm_enter_fullscreen", "Phóng to toàn màn hình", "Enter full screen")
                                },
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningImageViewer(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    textColor: Color,
    tunaGreen: Color
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val url = learningMaterialViewUrl(item)
    if (url.isBlank()) {
        LearningMaterialViewerError(
            message = learningMaterialText(materialLang, "lm_no_image_url", "Không có đường dẫn hình ảnh.", "No image link is available."),
            isVietnamese = isVietnamese,
            textColor = textColor,
            tunaGreen = tunaGreen
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        coil.compose.AsyncImage(
            model = url,
            contentDescription = learningMaterialTitle(item, isVietnamese),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ConfigureLearningMaterialViewerWindow(isFullscreen: Boolean) {
    val view = LocalView.current

    DisposableEffect(view, isFullscreen) {
        val dialogWindow =
            (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window

        if (dialogWindow != null) {
            // Let the viewer cover the notch/camera area with a black background.
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            dialogWindow.statusBarColor = android.graphics.Color.BLACK
            dialogWindow.navigationBarColor = android.graphics.Color.BLACK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val attributes = dialogWindow.attributes
                attributes.layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                dialogWindow.attributes = attributes
            }

            WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView).apply {
                // Status information is always hidden in the in-app viewer.
                hide(WindowInsetsCompat.Type.statusBars())

                // In landscape full-screen video, also hide Android navigation controls.
                // A swipe from the edge can temporarily reveal them.
                if (isFullscreen) {
                    hide(WindowInsetsCompat.Type.navigationBars())
                } else {
                    show(WindowInsetsCompat.Type.navigationBars())
                }

                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        onDispose { }
    }
}

@Composable
private fun LearningMaterialInAppViewer(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    hasPreviousVideo: Boolean = false,
    hasNextVideo: Boolean = false,
    onPreviousVideo: () -> Unit = {},
    onNextVideo: () -> Unit = {},
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val context = LocalContext.current
    val activity = remember(context) { context.findLearningMaterialsActivity() }
    // Preserve the exact orientation requested by the host Activity. In particular,
    // do not convert UNSPECIFIED to PORTRAIT because doing so can recreate the Activity
    // every time a video dialog is closed.
    val orientationToRestore = remember(activity) {
        activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    var isVideoFullscreen by remember { mutableStateOf(false) }
    var orientationChangedByViewer by remember { mutableStateOf(false) }
    var videoPlaybackSpeed by remember { mutableFloatStateOf(1f) }

    fun restoreViewerOrientationIfNeeded() {
        if (!orientationChangedByViewer) return
        activity?.requestedOrientation = orientationToRestore
        orientationChangedByViewer = false
    }

    fun setVideoFullscreen(fullscreen: Boolean) {
        if (item.type != LearningMaterialType.VIDEO) return
        if (fullscreen) {
            orientationChangedByViewer = true
            isVideoFullscreen = true
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            isVideoFullscreen = false
            restoreViewerOrientationIfNeeded()
        }
    }

    fun dismissViewer() {
        isVideoFullscreen = false
        restoreViewerOrientationIfNeeded()
        onDismiss()
    }

    DisposableEffect(activity, orientationToRestore) {
        onDispose {
            if (orientationChangedByViewer) {
                activity?.requestedOrientation = orientationToRestore
            }
        }
    }

    fun closeOrExitFullscreen() {
        if (isVideoFullscreen) {
            setVideoFullscreen(false)
        } else {
            dismissViewer()
        }
    }

    BackHandler { closeOrExitFullscreen() }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { closeOrExitFullscreen() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        ConfigureLearningMaterialViewerWindow(
            isFullscreen = isVideoFullscreen && item.type == LearningMaterialType.VIDEO
        )

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isVideoFullscreen && item.type == LearningMaterialType.VIDEO) {
                        Modifier
                    } else {
                        Modifier.navigationBarsPadding()
                    }
                ),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isVideoFullscreen || item.type != LearningMaterialType.VIDEO) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // safeDrawing keeps the title below a notch or hole-punch camera,
                            // even though the status bar is hidden in this dialog.
                            .background(surfaceColor)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                            )
                            .heightIn(min = 58.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { dismissViewer() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = learningMaterialText(materialLang, "lm_back", "Quay lại", "Back"),
                                tint = textColor
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = learningMaterialTitle(item, isVietnamese),
                                color = textColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            val meta = learningMaterialMeta(item, isVietnamese)
                            if (meta.isNotBlank()) {
                                Text(
                                    text = meta,
                                    color = textColor.copy(alpha = 0.52f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (item.storage != LearningMaterialStorage.LOCAL && item.downloadUrl.isNotBlank()) {
                            IconButton(onClick = onDownload) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = learningMaterialText(materialLang, "download", "Tải xuống", "Download"),
                                    tint = tunaGreen
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = textColor.copy(alpha = 0.10f),
                        thickness = 1.dp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (item.type) {
                        LearningMaterialType.IMAGE -> LearningImageViewer(
                            item = item,
                            isVietnamese = isVietnamese,
                            textColor = textColor,
                            tunaGreen = tunaGreen
                        )

                        LearningMaterialType.PDF -> LearningPdfViewer(
                            item = item,
                            isVietnamese = isVietnamese,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            tunaGreen = tunaGreen
                        )

                        LearningMaterialType.VIDEO -> LearningVideoViewer(
                            item = item,
                            isVietnamese = isVietnamese,
                            textColor = textColor,
                            tunaGreen = tunaGreen,
                            playbackSpeed = videoPlaybackSpeed,
                            onPlaybackSpeedChange = { videoPlaybackSpeed = it },
                            isFullscreen = isVideoFullscreen,
                            onToggleFullscreen = {
                                setVideoFullscreen(!isVideoFullscreen)
                            },
                            hasPreviousVideo = hasPreviousVideo,
                            hasNextVideo = hasNextVideo,
                            onPreviousVideo = onPreviousVideo,
                            onNextVideo = onNextVideo
                        )

                        LearningMaterialType.FOLDER -> LearningMaterialViewerError(
                            message = learningMaterialText(materialLang, "lm_folder_error", "Đây là thư mục tài liệu.", "This item is a folder."),
                            isVietnamese = isVietnamese,
                            textColor = textColor,
                            tunaGreen = tunaGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningMaterialPreviewDialog(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDownload: () -> Unit
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    val accent = learningMaterialAccent(item.type)
    val previewText = when (item.type) {
        LearningMaterialType.IMAGE -> learningMaterialText(materialLang, "lm_image_preview", "XEM TRƯỚC HÌNH ẢNH", "IMAGE PREVIEW")
        LearningMaterialType.PDF -> learningMaterialText(materialLang, "lm_pdf_document", "TÀI LIỆU PDF", "PDF DOCUMENT")
        LearningMaterialType.VIDEO -> learningMaterialText(materialLang, "lm_video_document", "TÀI LIỆU VIDEO", "VIDEO MATERIAL")
        LearningMaterialType.FOLDER -> ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = learningMaterialTitle(item, isVietnamese),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                val meta = learningMaterialMeta(item, isVietnamese)
                if (meta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = meta,
                        color = textColor.copy(alpha = 0.48f),
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.13f))
                        .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.type == LearningMaterialType.IMAGE && item.previewUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = item.previewUrl,
                            contentDescription = learningMaterialTitle(item, isVietnamese),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            when (item.type) {
                                LearningMaterialType.IMAGE -> Icon(
                                    Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(58.dp)
                                )
                                LearningMaterialType.PDF -> {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = accent,
                                        modifier = Modifier.size(54.dp)
                                    )
                                    Text("PDF", color = accent, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                LearningMaterialType.VIDEO -> {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(accent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                                LearningMaterialType.FOLDER -> Unit
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(previewText, color = accent, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            Text(
                                text = learningMaterialText(materialLang, "lm_tap_open", "Nhấn Mở xem để hiển thị nội dung", "Tap Open to view this material"),
                                color = textColor.copy(alpha = 0.55f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                val description = learningMaterialDescription(item, isVietnamese)
                if (description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = description,
                        color = textColor.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = tunaGreen.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = learningMaterialText(
                            materialLang,
                            "lm_in_app",
                            "Nội dung được hiển thị trực tiếp trong ứng dụng.",
                            "This content is displayed directly inside the app."
                        ),
                        color = tunaGreen,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onOpen,
                colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(learningMaterialText(materialLang, "lm_open", "Mở xem", "Open"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.storage != LearningMaterialStorage.LOCAL && item.downloadUrl.isNotBlank()) {
                    TextButton(onClick = onDownload) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = tunaGreen,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(learningMaterialText(materialLang, "download", "Tải xuống", "Download"), color = tunaGreen)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(learningMaterialText(materialLang, "close", "Đóng", "Close"), color = textColor.copy(alpha = 0.65f))
                }
            }
        }
    )
}

@Composable
private fun LearningMaterialAddDialog(
    isVietnamese: Boolean,
    currentFolderPath: String,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    isBusy: Boolean,
    errorMessage: String?,
    canUploadPublic: Boolean,
    canCreatePublicFolder: Boolean,
    isCommunityArea: Boolean,
    publicRestrictionMessage: String? = null,
    onDismiss: () -> Unit,
    onChooseFile: (LearningMaterialStorage) -> Unit,
    onCreateFolder: (LearningMaterialStorage, String) -> Unit
) {
    val materialLang = LocalLearningMaterialsLanguage.current
    var addKind by remember { mutableStateOf(LearningMaterialAddKind.FILE) }
    var storage by remember { mutableStateOf(LearningMaterialStorage.PUBLIC) }
    var folderName by remember { mutableStateOf("") }

    val publicAllowedForKind = when (addKind) {
        LearningMaterialAddKind.FILE -> canUploadPublic
        LearningMaterialAddKind.FOLDER -> canCreatePublicFolder
    }
    LaunchedEffect(addKind, publicAllowedForKind) {
        if (storage == LearningMaterialStorage.PUBLIC && !publicAllowedForKind) {
            storage = LearningMaterialStorage.LOCAL
        }
    }

    // Bảng màu trung tính dùng riêng cho các nút lựa chọn trong hộp thoại.
    val selectedButtonBackground = textColor.copy(alpha = 0.12f)
    val idleButtonBackground = textColor.copy(alpha = 0.035f)
    val selectedButtonBorder = textColor.copy(alpha = 0.42f)
    val idleButtonBorder = textColor.copy(alpha = 0.15f)
    val selectedButtonContent = textColor
    val idleButtonContent = textColor.copy(alpha = 0.68f)

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column {
                Text(
                    text = learningMaterialText(materialLang, "lm_add_to_folder", "Thêm vào thư mục", "Add to folder"),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentFolderPath,
                    color = tunaGreen,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = learningMaterialText(materialLang, "lm_content_type", "Nội dung cần thêm", "Content type"),
                    color = textColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val contentChoices: List<Triple<LearningMaterialAddKind, String, ImageVector>> = listOf(
                        Triple(
                            LearningMaterialAddKind.FILE,
                            learningMaterialText(materialLang, "lm_add_file", "Thêm file", "Add file"),
                            Icons.Default.Description
                        ),
                        Triple(
                            LearningMaterialAddKind.FOLDER,
                            learningMaterialText(materialLang, "lm_create_folder", "Tạo thư mục", "Create folder"),
                            Icons.Default.Folder
                        )
                    )

                    contentChoices.forEach { (kind, label, icon) ->
                        val selected = addKind == kind
                        val contentColor = if (selected) selectedButtonContent else idleButtonContent
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isBusy) { addKind = kind },
                            color = if (selected) selectedButtonBackground else idleButtonBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) selectedButtonBorder else idleButtonBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = label,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Text(
                    text = learningMaterialText(materialLang, "lm_storage", "Nơi lưu", "Storage"),
                    color = textColor.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val storageChoices: List<Triple<LearningMaterialStorage, String, ImageVector>> = listOf(
                        Triple(
                            LearningMaterialStorage.PUBLIC,
                            if (isCommunityArea) {
                                if (isVietnamese) "Chia sẻ cộng đồng" else "Share to community"
                            } else {
                                learningMaterialText(materialLang, "lm_public", "Công khai", "Public")
                            },
                            Icons.Default.Public
                        ),
                        Triple(
                            LearningMaterialStorage.LOCAL,
                            learningMaterialText(materialLang, "lm_on_device", "Trên máy", "On device"),
                            Icons.Default.PhoneAndroid
                        )
                    )

                    storageChoices.forEach { (mode, label, icon) ->
                        val selected = storage == mode
                        val contentColor = if (selected) selectedButtonContent else idleButtonContent
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(
                                    enabled = !isBusy && (mode != LearningMaterialStorage.PUBLIC || publicAllowedForKind)
                                ) { storage = mode },
                            color = if (selected) selectedButtonBackground else idleButtonBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) selectedButtonBorder else idleButtonBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = contentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = label,
                                    color = contentColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (!publicAllowedForKind && !publicRestrictionMessage.isNullOrBlank()) {
                    Text(
                        text = publicRestrictionMessage,
                        color = Color(0xFFFFB74D),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }

                Text(
                    text = when (storage) {
                        LearningMaterialStorage.PUBLIC -> if (publicAllowedForKind) {
                            if (isCommunityArea) {
                                if (isVietnamese) {
                                    "Nội dung sẽ được đưa lên máy chủ Cộng đồng. Chỉ chính chủ và Admin được đổi tên hoặc xóa; thành viên khác chỉ xem."
                                } else {
                                    "This content will be shared to the Community server. Only its owner and Admin can rename or delete it; other members can only view it."
                                }
                            } else {
                                learningMaterialText(
                                    materialLang,
                                    "lm_public_desc",
                                    "Nội dung sẽ được tải lên máy chủ và người được cấp quyền có thể xem.",
                                    "The content is uploaded to the server for authorized users."
                                )
                            }
                        } else {
                            documentAccessText(
                                materialLang,
                                "doc_access_no_public_write",
                                "Tài khoản không có quyền thêm nội dung công khai.",
                                "This account cannot add public content."
                            )
                        }
                        else -> learningMaterialText(
                            materialLang,
                            "lm_local_desc",
                            "Nội dung chỉ được lưu riêng trong ứng dụng trên thiết bị này.",
                            "The content is stored privately inside the app on this device."
                        )
                    },
                    color = textColor.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                if (addKind == LearningMaterialAddKind.FOLDER) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it.take(120) },
                        enabled = !isBusy,
                        singleLine = true,
                        label = {
                            Text(
                                learningMaterialText(materialLang, "lm_folder_name", "Tên thư mục", "Folder name"),
                                color = textColor.copy(alpha = 0.55f)
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(color = textColor, fontSize = 13.sp),
                        shape = RoundedCornerShape(11.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = textColor.copy(alpha = 0.50f),
                            unfocusedIndicatorColor = textColor.copy(alpha = 0.18f),
                            cursorColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF5350),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                if (isBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = textColor.copy(alpha = 0.75f),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            learningMaterialText(materialLang, "processing", "Đang xử lý...", "Processing..."),
                            color = textColor.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (addKind == LearningMaterialAddKind.FILE) {
                        onChooseFile(storage)
                    } else {
                        onCreateFolder(storage, folderName.trim())
                    }
                },
                enabled = !isBusy &&
                        (addKind == LearningMaterialAddKind.FILE || folderName.isNotBlank()) &&
                        (storage != LearningMaterialStorage.PUBLIC || publicAllowedForKind),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor,
                    contentColor = surfaceColor,
                    disabledContainerColor = textColor.copy(alpha = 0.10f),
                    disabledContentColor = textColor.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = if (addKind == LearningMaterialAddKind.FILE) {
                        Icons.Default.Description
                    } else {
                        Icons.Default.Folder
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (addKind == LearningMaterialAddKind.FILE) {
                        learningMaterialText(materialLang, "lm_choose_file", "Chọn file", "Choose file")
                    } else {
                        learningMaterialText(materialLang, "lm_create_folder", "Tạo thư mục", "Create folder")
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(
                    learningMaterialText(materialLang, "close", "Đóng", "Close"),
                    color = textColor.copy(alpha = 0.62f)
                )
            }
        }
    )
}

@Composable
private fun GoogleBrandIcon(modifier: Modifier = Modifier) {
    // Google multicolour "G" drawn directly from vector path data in code.
    // No PNG / drawable resource is required.
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
private fun GmailBrandIcon(modifier: Modifier = Modifier) {
    // Drawn entirely in Compose Canvas. No PNG/vector resource is required.
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val originX = (size.width - diameter) / 2f
        val originY = (size.height - diameter) / 2f

        // Blue-violet app-tile background, similar to the visual treatment of the
        // reference icon while remaining resolution-independent.
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

        // Multi-colour Gmail-style M mark.
        val leftX = originX + diameter * 0.31f
        val centerX = originX + diameter * 0.50f
        val rightX = originX + diameter * 0.69f
        val topY = originY + diameter * 0.36f
        val valleyY = originY + diameter * 0.55f
        val bottomY = originY + diameter * 0.70f
        val markWidth = diameter * 0.105f

        drawLine(
            color = Color(0xFFEA4335),
            start = Offset(leftX, bottomY),
            end = Offset(leftX, topY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFEA4335),
            start = Offset(leftX, topY),
            end = Offset(centerX, valleyY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )

        val diagonalOrange = Offset(
            centerX + (rightX - centerX) * 0.58f,
            valleyY + (topY - valleyY) * 0.58f
        )
        drawLine(
            color = Color(0xFFFF4B3E),
            start = Offset(centerX, valleyY),
            end = diagonalOrange,
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFB300),
            start = diagonalOrange,
            end = Offset(rightX, topY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )

        val greenStartY = topY + (bottomY - topY) * 0.23f
        val blueStartY = topY + (bottomY - topY) * 0.60f
        drawLine(
            color = Color(0xFFFFC107),
            start = Offset(rightX, topY),
            end = Offset(rightX, greenStartY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF20B86A),
            start = Offset(rightX, greenStartY),
            end = Offset(rightX, blueStartY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(rightX, blueStartY),
            end = Offset(rightX, bottomY),
            strokeWidth = markWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ZaloBrandIcon(modifier: Modifier = Modifier) {
    // Drawn entirely in Compose Canvas. No PNG/vector resource is required.
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
        val shadowOffset = diameter * 0.025f

        // Subtle shadow gives the speech bubble the same lifted appearance as the
        // familiar Zalo application icon.
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.16f),
            topLeft = Offset(bubbleLeft + shadowOffset, bubbleTop + shadowOffset),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = bubbleCorner
        )
        val shadowTail = androidx.compose.ui.graphics.Path().apply {
            moveTo(bubbleLeft + diameter * 0.17f + shadowOffset, bubbleTop + bubbleHeight - diameter * 0.02f + shadowOffset)
            lineTo(bubbleLeft + diameter * 0.11f + shadowOffset, bubbleTop + bubbleHeight + diameter * 0.13f + shadowOffset)
            lineTo(bubbleLeft + diameter * 0.31f + shadowOffset, bubbleTop + bubbleHeight - diameter * 0.015f + shadowOffset)
            close()
        }
        drawPath(path = shadowTail, color = Color.Black.copy(alpha = 0.16f))

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
private fun LearningMaterialsLoadingWave(
    modifier: Modifier = Modifier,
    color: Color
) {
    val transition = rememberInfiniteTransition(label = "learning_materials_loading_wave")
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
                        delayMillis = index * 85
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "learning_materials_loading_wave_$index"
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
private fun LearningMaterialsStartupLoadingScreen(
    bgColor: Color,
    tunaGreen: Color,
    onClose: () -> Unit
) {
    // Không hiển thị tiêu đề hoặc nút Back trong lúc khởi động.
    // Hardware/system Back vẫn có thể thoát màn hình nếu người dùng muốn.
    BackHandler(onBack = onClose)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LearningMaterialsLoadingWave(color = tunaGreen)
        }
    }
}

@Composable
private fun DocumentAccessGateScreen(
    lang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    state: DocumentAccessState,
    onClose: () -> Unit,
    onLogin: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val supportEmail = "tunertools.top@gmail.com"
    val supportZaloPhone = "0971832146"

    if (state == DocumentAccessState.Checking) {
        LearningMaterialsStartupLoadingScreen(
            bgColor = bgColor,
            tunaGreen = tunaGreen,
            onClose = onClose
        )
        return
    }

    fun launchContactIntent(primary: Intent, fallback: Intent) {
        val opened = runCatching {
            context.startActivity(primary)
            true
        }.getOrDefault(false)

        if (!opened) {
            runCatching { context.startActivity(fallback) }
                .onFailure {
                    android.widget.Toast.makeText(
                        context,
                        documentAccessText(
                            lang,
                            "doc_access_contact_open_error",
                            "Không tìm thấy ứng dụng phù hợp để mở liên hệ.",
                            "No suitable app was found to open this contact."
                        ),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun openSupportEmail(requestingEmail: String) {
        val subject = "Yêu cầu cấp quyền tài liệu TunerTools"
        val body = buildString {
            append("Xin chào TunerTools, vui lòng hỗ trợ cấp quyền tài liệu")
            if (requestingEmail.isNotBlank()) {
                append(" cho tài khoản: ")
                append(requestingEmail)
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

    @Composable
    fun ContactActionRow(
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
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.13f))
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
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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

    BackHandler(onBack = onClose)
    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = textColor)
                }
                Text(
                    text = documentAccessText(lang, "doc_access_title", "Tài liệu học đàn", "Learning materials"),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 25.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val icon = when (state) {
                                DocumentAccessState.Checking -> Icons.Default.Refresh
                                is DocumentAccessState.LoginRequired -> Icons.Default.Login
                                is DocumentAccessState.Denied -> Icons.Default.Lock
                                is DocumentAccessState.Error -> Icons.Default.CloudOff
                                is DocumentAccessState.Granted -> Icons.Default.Lock
                            }
                            Icon(icon, null, tint = textColor.copy(alpha = 0.80f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(14.dp))

                            val title = when (state) {
                                DocumentAccessState.Checking -> documentAccessText(lang, "processing", "Đang kiểm tra quyền...", "Checking access...")
                                is DocumentAccessState.LoginRequired -> documentAccessText(lang, "doc_access_login_required", "Cần đăng nhập Google", "Google login required")
                                is DocumentAccessState.Denied -> documentAccessText(lang, "doc_access_denied", "Tài khoản chưa được cấp quyền", "Account is not authorized")
                                is DocumentAccessState.Error -> documentAccessText(lang, "error", "Lỗi", "Error")
                                is DocumentAccessState.Granted -> ""
                            }
                            Text(title, color = textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)

                            val message = when (state) {
                                DocumentAccessState.Checking -> ""
                                is DocumentAccessState.LoginRequired -> state.message.ifBlank {
                                    documentAccessText(lang, "doc_access_login_hint", "Đăng nhập đúng Gmail đã được quản trị viên cấp quyền.", "Sign in with the Gmail account granted by the administrator.")
                                }
                                is DocumentAccessState.Denied -> listOf(state.email, state.message)
                                    .filter { it.isNotBlank() }
                                    .joinToString("\n")
                                is DocumentAccessState.Error -> state.message
                                is DocumentAccessState.Granted -> ""
                            }
                            if (message.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    color = textColor.copy(alpha = 0.62f),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }

                            if (state is DocumentAccessState.Denied) {
                                Spacer(modifier = Modifier.height(14.dp))
                                ContactActionRow(
                                    label = "Gmail",
                                    value = supportEmail,
                                    onClick = { openSupportEmail(state.email) }
                                ) {
                                    GmailBrandIcon(modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                ContactActionRow(
                                    label = "Zalo",
                                    value = supportZaloPhone,
                                    onClick = { openSupportZalo() }
                                ) {
                                    ZaloBrandIcon(modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))
                            when (state) {
                                DocumentAccessState.Checking -> CircularProgressIndicator(color = tunaGreen, strokeWidth = 2.dp)
                                is DocumentAccessState.LoginRequired -> Button(
                                    onClick = onLogin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF202124),
                                        disabledContainerColor = Color.White.copy(alpha = 0.65f),
                                        disabledContentColor = Color(0xFF202124).copy(alpha = 0.45f)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFDADCE0)),
                                    shape = RoundedCornerShape(11.dp)
                                ) {
                                    GoogleBrandIcon(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(7.dp))
                                    Text(
                                        text = documentAccessText(
                                            lang,
                                            "doc_access_login",
                                            "Đăng nhập bằng Google",
                                            "Sign in with Google"
                                        ),
                                        color = Color(0xFF202124),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                is DocumentAccessState.Denied,
                                is DocumentAccessState.Error -> Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onLogin,
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        GoogleBrandIcon(modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = documentAccessText(lang, "doc_access_switch_account", "Đổi tài khoản", "Switch account"),
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = onRetry,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = documentAccessText(lang, "lm_retry", "Thử lại", "Retry"),
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                                is DocumentAccessState.Granted -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningMaterialRenameDialog(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    tunaGreen: Color,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember(item.id) {
        mutableStateOf(
            if (item.type == LearningMaterialType.FOLDER) {
                learningMaterialTitle(item, isVietnamese)
            } else {
                item.path.substringAfterLast('/').substringBeforeLast('.', learningMaterialTitle(item, isVietnamese))
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                if (isVietnamese) "Đổi tên" else "Rename",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    learningMaterialTitle(item, isVietnamese),
                    color = textColor.copy(alpha = 0.62f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(160) },
                    enabled = !isBusy,
                    singleLine = true,
                    label = { Text(if (isVietnamese) "Tên mới" else "New name") },
                    textStyle = TextStyle(color = textColor),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = bgColor,
                        unfocusedContainerColor = bgColor,
                        focusedIndicatorColor = tunaGreen,
                        cursorColor = tunaGreen,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (item.type != LearningMaterialType.FOLDER) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Text(
                        if (isVietnamese) "Đuôi file được giữ nguyên để tránh làm hỏng định dạng." else "The file extension is preserved to protect the format.",
                        color = textColor.copy(alpha = 0.48f),
                        fontSize = 10.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName.trim()) },
                enabled = !isBusy && newName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isVietnamese) "Lưu" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(if (isVietnamese) "Hủy" else "Cancel", color = textColor.copy(alpha = 0.62f))
            }
        }
    )
}

@Composable
private fun LearningMaterialDeleteDialog(
    item: LearningMaterialUiItem,
    isVietnamese: Boolean,
    surfaceColor: Color,
    textColor: Color,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        containerColor = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                if (isVietnamese) "Xóa tài liệu" else "Delete material",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    if (item.type == LearningMaterialType.FOLDER) {
                        if (isVietnamese) "Xóa thư mục \"${learningMaterialTitle(item, true)}\" và toàn bộ nội dung bên trong?" else "Delete folder \"${learningMaterialTitle(item, false)}\" and all content inside it?"
                    } else {
                        if (isVietnamese) "Xóa file \"${learningMaterialTitle(item, true)}\"?" else "Delete file \"${learningMaterialTitle(item, false)}\"?"
                    },
                    color = textColor,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isVietnamese) "Thao tác này không thể hoàn tác." else "This action cannot be undone.",
                    color = Color.Red.copy(alpha = 0.82f),
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isVietnamese) "Xóa" else "Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(if (isVietnamese) "Hủy" else "Cancel", color = textColor.copy(alpha = 0.62f))
            }
        }
    )
}

@Composable
fun LearningMaterialsScreen(
    instrument: String,
    lang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onClose: () -> Unit
) {
    CompositionLocalProvider(LocalLearningMaterialsLanguage provides lang) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val googleSignInClient = remember(context) {
            GoogleSignIn.getClient(context, buildTunerGoogleSignInOptions(context))
        }

        // Base session uses SONG_TAB so a normal signed-in user can receive the
        // 30-day SongTab trial and still enter the Documents root catalog.
        var accessState by remember {
            mutableStateOf<DocumentAccessState>(DocumentAccessState.Checking)
        }
        var documentsAccessState by remember {
            mutableStateOf<DocumentAccessState>(DocumentAccessState.Checking)
        }
        var accessRefresh by remember { mutableIntStateOf(0) }
        var documentsAccessRefresh by remember { mutableIntStateOf(0) }
        var initialContentReady by remember(instrument) { mutableStateOf(false) }
        var showProtectedAccessGate by remember(instrument) { mutableStateOf(false) }

        val signInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                coroutineScope.launch {
                    initialContentReady = false
                    showProtectedAccessGate = false
                    accessState = DocumentAccessState.Checking
                    documentsAccessState = DocumentAccessState.Checking
                    val account = try {
                        GoogleSignIn.getSignedInAccountFromIntent(result.data)
                            .getResult(ApiException::class.java)
                    } catch (error: Exception) {
                        null
                    }
                    accessState = DocumentAccessApi.loginWithAccount(
                        context,
                        account,
                        DocumentAccessScope.SONG_TAB
                    )
                    if (accessState is DocumentAccessState.Granted) {
                        documentsAccessState = DocumentAccessApi.checkCurrentScope(
                            context,
                            DocumentAccessScope.DOCUMENTS
                        )
                    }
                }
            } else {
                initialContentReady = false
                accessState = DocumentAccessState.LoginRequired(
                    message = documentAccessText(
                        lang,
                        "doc_access_login_hint",
                        "Đăng nhập Google để tiếp tục.",
                        "Sign in with Google to continue."
                    )
                )
            }
        }

        fun switchGoogleAccount() {
            initialContentReady = false
            showProtectedAccessGate = false
            accessState = DocumentAccessState.Checking
            documentsAccessState = DocumentAccessState.Checking
            DocumentAccessStore.clear(context)
            // Always sign out first. Otherwise Google Sign-In can silently reuse the
            // Gmail that was denied and the account picker never appears.
            googleSignInClient.signOut().addOnCompleteListener {
                signInLauncher.launch(googleSignInClient.signInIntent)
            }
        }

        LaunchedEffect(instrument, accessRefresh) {
            initialContentReady = false
            showProtectedAccessGate = false
            accessState = DocumentAccessState.Checking
            documentsAccessState = DocumentAccessState.Checking
            accessState = DocumentAccessApi.ensureSession(
                context,
                DocumentAccessScope.SONG_TAB
            )
        }

        val grantedState = accessState as? DocumentAccessState.Granted

        // Check Documents registration separately. A trial denial does not clear
        // the SongTab session and therefore does not block the root catalog.
        LaunchedEffect(
            grantedState?.info?.email,
            documentsAccessRefresh
        ) {
            if (grantedState == null) {
                documentsAccessState = DocumentAccessState.Checking
                return@LaunchedEffect
            }
            documentsAccessState = DocumentAccessApi.checkCurrentScope(
                context,
                DocumentAccessScope.DOCUMENTS
            )
        }

        val documentsGrantedState = documentsAccessState as? DocumentAccessState.Granted
        val documentsAuthorized = documentsGrantedState != null

        LaunchedEffect(documentsAuthorized) {
            if (documentsAuthorized) {
                showProtectedAccessGate = false
            }
        }

        val showUnifiedStartupLoading =
            accessState == DocumentAccessState.Checking ||
                    (grantedState != null && !initialContentReady)

        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = accessState) {
                DocumentAccessState.Checking -> Unit

                is DocumentAccessState.Granted -> LearningMaterialsScreenContent(
                    instrument = instrument,
                    lang = lang,
                    bgColor = bgColor,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    tunaGreen = tunaGreen,
                    accessInfo = documentsGrantedState?.info ?: state.info,
                    documentsAuthorized = documentsAuthorized,
                    onRequireDocumentAccess = {
                        showProtectedAccessGate = true
                    },
                    onClose = onClose,
                    onInitialLoadFinished = { initialContentReady = true }
                )

                else -> DocumentAccessGateScreen(
                    lang = lang,
                    bgColor = bgColor,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    tunaGreen = tunaGreen,
                    state = state,
                    onClose = onClose,
                    onLogin = { switchGoogleAccount() },
                    onRetry = { accessRefresh++ }
                )
            }

            // Protected folders show the existing Gmail/Zalo gate on top of the
            // catalog. Closing the gate returns to the root list instead of leaving
            // Documents completely.
            if (grantedState != null && showProtectedAccessGate && !documentsAuthorized) {
                val gateState = when (val state = documentsAccessState) {
                    DocumentAccessState.Checking -> DocumentAccessState.Checking
                    is DocumentAccessState.Denied -> state
                    is DocumentAccessState.LoginRequired -> state
                    is DocumentAccessState.Error -> state
                    is DocumentAccessState.Granted -> state
                }
                DocumentAccessGateScreen(
                    lang = lang,
                    bgColor = bgColor,
                    surfaceColor = surfaceColor,
                    textColor = textColor,
                    tunaGreen = tunaGreen,
                    state = gateState,
                    onClose = { showProtectedAccessGate = false },
                    onLogin = { switchGoogleAccount() },
                    onRetry = {
                        documentsAccessState = DocumentAccessState.Checking
                        documentsAccessRefresh++
                    }
                )
            }

            // Một overlay duy nhất được giữ nguyên cùng vị trí trong suốt hai bước
            // đăng nhập SongTab -> tải danh mục gốc Documents.
            if (showUnifiedStartupLoading) {
                LearningMaterialsStartupLoadingScreen(
                    bgColor = bgColor,
                    tunaGreen = tunaGreen,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun LearningMaterialsScreenContent(
    instrument: String,
    lang: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    accessInfo: DocumentAccessInfo,
    documentsAuthorized: Boolean,
    onRequireDocumentAccess: () -> Unit,
    onClose: () -> Unit,
    onInitialLoadFinished: () -> Unit = {}
) {
    val isVietnamese = lang == "vi"
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDocumentAdmin = accessInfo.permissions.canManage
    val hostActivity = remember(context) { context.findLearningMaterialsActivity() }

    // Block screenshots, screen recording and recent-app previews for every
    // non-admin user while the complete Documents/folder screen is visible.
    // A Documents admin (canManage = true) is explicitly allowed to capture it.
    DisposableEffect(hostActivity, isDocumentAdmin) {
        val window = hostActivity?.window
        val secureFlag = android.view.WindowManager.LayoutParams.FLAG_SECURE
        val wasSecureBeforeThisScreen = window?.let {
            (it.attributes.flags and secureFlag) != 0
        } ?: false

        if (window != null) {
            if (isDocumentAdmin) {
                window.clearFlags(secureFlag)
            } else {
                window.addFlags(secureFlag)
            }
        }

        onDispose {
            // Restore the exact flag state that existed before opening Documents.
            if (window != null) {
                if (wasSecureBeforeThisScreen) {
                    window.addFlags(secureFlag)
                } else {
                    window.clearFlags(secureFlag)
                }
            }
        }
    }

    var currentPath by remember(instrument) { mutableStateOf("") }
    var page by remember(instrument) { mutableStateOf<LearningMaterialPage?>(null) }
    var isLoading by remember(instrument) { mutableStateOf(true) }
    var initialLoadReported by remember(instrument) { mutableStateOf(false) }
    var loadError by remember(instrument) { mutableStateOf<String?>(null) }
    var serverWarning by remember(instrument) { mutableStateOf<String?>(null) }
    // Trạng thái kết nối API công khai, dùng cho chấm tín hiệu trên nút làm mới.
    var isServerOnline by remember(instrument) { mutableStateOf(false) }
    var isNetworkChecking by remember(instrument) { mutableStateOf(false) }
    var refreshVersion by remember(instrument) { mutableIntStateOf(0) }
    var forceReloadPath by remember(instrument) { mutableStateOf<String?>(null) }
    val mergedPageCache = remember(instrument) { mutableStateMapOf<String, LearningMaterialPage>() }
    var selectedType by remember(instrument) { mutableStateOf("ALL") }
    var searchText by remember(instrument) { mutableStateOf("") }
    var viewerItem by remember { mutableStateOf<LearningMaterialUiItem?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var isAddBusy by remember { mutableStateOf(false) }
    var addError by remember { mutableStateOf<String?>(null) }
    var pendingFileStorage by remember { mutableStateOf(LearningMaterialStorage.PUBLIC) }
    var pendingFilePath by remember(instrument) { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<LearningMaterialManageRequest?>(null) }
    var deleteTarget by remember { mutableStateOf<LearningMaterialManageRequest?>(null) }
    var adminActionBusy by remember { mutableStateOf(false) }

    fun parentOf(path: String): String {
        if (path.isBlank()) return ""
        val separatorIndex = path.lastIndexOf('/')
        return if (separatorIndex < 0) "" else path.substring(0, separatorIndex)
    }

    fun showMessage(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun canOpenWithoutDocumentsRegistration(item: LearningMaterialUiItem): Boolean {
        if (item.storage != LearningMaterialStorage.PUBLIC) return true
        if (!documentsAuthorized) return item.isCommunityContent
        return accessInfo.permissions.canAccessFolder(instrument, item.path)
    }

    fun requireDocumentsRegistrationFor(item: LearningMaterialUiItem): Boolean {
        if (canOpenWithoutDocumentsRegistration(item)) return false
        if (documentsAuthorized) {
            showMessage(
                if (isVietnamese) {
                    "Gmail này chưa được cấp quyền xem thư mục ${learningMaterialTitle(item, true)}."
                } else {
                    "This Gmail account is not allowed to view ${learningMaterialTitle(item, false)}."
                }
            )
        } else {
            onRequireDocumentAccess()
        }
        return true
    }

    fun normalizedPath(path: String): String =
        path.replace('\\', '/').split('/').filter { it.isNotBlank() }.joinToString("/")

    fun navigateToPath(path: String) {
        val targetPath = normalizedPath(path)
        val cachedPage = mergedPageCache[targetPath]
            ?: LearningMaterialsApi.peekCachedPage(
                context = context,
                instrument = instrument,
                path = targetPath,
                allowExpired = true
            )
        if (cachedPage != null) {
            page = cachedPage
            isLoading = false
            loadError = null
        } else {
            isLoading = true
        }
        currentPath = targetPath
        searchText = ""
    }

    fun requestContentReload() {
        val currentKey = normalizedPath(currentPath)
        val currentSnapshot = page?.takeIf { normalizedPath(it.currentPath) == currentKey }
        mergedPageCache.clear()
        if (currentSnapshot != null) mergedPageCache[currentKey] = currentSnapshot
        forceReloadPath = currentKey
        refreshVersion++
    }

    fun goBack() {
        if (currentPath.isNotBlank()) {
            navigateToPath(page?.parentPath ?: parentOf(currentPath))
        } else {
            onClose()
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            coroutineScope.launch {
                isAddBusy = true
                addError = null
                try {
                    val message = when (pendingFileStorage) {
                        LearningMaterialStorage.PUBLIC -> {
                            if (!accessInfo.permissions.canUpload && !accessInfo.permissions.canManage) {
                                throw IOException("Tài khoản không có quyền thêm file công khai.")
                            }
                            LearningMaterialsApi.uploadFile(
                                context = context,
                                instrument = instrument,
                                path = pendingFilePath,
                                uri = uri
                            )
                        }
                        else -> LearningLocalMaterials.importFile(
                            context = context,
                            instrument = instrument,
                            path = pendingFilePath,
                            uri = uri
                        )
                    }
                    showMessage(message)
                    requestContentReload()
                } catch (error: Exception) {
                    showMessage(
                        error.message ?: learningMaterialText(
                            lang,
                            "lm_add_file_error",
                            "Không thể thêm tệp.",
                            "Unable to add the file."
                        )
                    )
                } finally {
                    isAddBusy = false
                }
            }
        }
    }

    fun createFolder(storage: LearningMaterialStorage, folderName: String) {
        if (folderName.isBlank()) return
        coroutineScope.launch {
            isAddBusy = true
            addError = null
            try {
                val message = when (storage) {
                    LearningMaterialStorage.PUBLIC -> {
                        if (!accessInfo.permissions.canCreateFolder && !accessInfo.permissions.canManage) {
                            throw IOException("Tài khoản không có quyền tạo thư mục công khai.")
                        }
                        LearningMaterialsApi.createFolder(
                            context = context,
                            instrument = instrument,
                            path = currentPath,
                            folderName = folderName
                        )
                    }
                    else -> LearningLocalMaterials.createFolder(
                        context = context,
                        instrument = instrument,
                        path = currentPath,
                        folderName = folderName
                    )
                }
                showAddDialog = false
                showMessage(message)
                requestContentReload()
            } catch (error: Exception) {
                addError = error.message ?: learningMaterialText(
                    lang,
                    "lm_create_folder_error",
                    "Không thể tạo thư mục.",
                    "Unable to create the folder."
                )
            } finally {
                isAddBusy = false
            }
        }
    }

    LaunchedEffect(instrument, currentPath, refreshVersion) {
        val requestedPath = normalizedPath(currentPath)
        val forceRefresh = forceReloadPath == requestedPath
        val cachedMergedPage = mergedPageCache[requestedPath]
        val cachedPublicPage = LearningMaterialsApi.peekCachedPage(
            context = context,
            instrument = instrument,
            path = requestedPath,
            allowExpired = true
        )

        // Show the cached page immediately while a forced/manual refresh happens in
        // the background. Navigation back never blanks an already visited folder.
        when {
            cachedMergedPage != null -> {
                page = cachedMergedPage
                isLoading = false
            }
            cachedPublicPage != null -> {
                page = cachedPublicPage
                isLoading = false
            }
            else -> isLoading = true
        }
        loadError = null
        serverWarning = null

        // A short debounce lets rapid folder/back taps cancel before opening another
        // network request. OkHttp cancellation below then closes the active socket.
        delay(90L)

        var localPage: LearningMaterialPage? = null
        var localFailure: String? = null
        try {
            localPage = LearningLocalMaterials.loadPage(context, instrument, requestedPath)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            localFailure = error.message
        }

        var publicPage: LearningMaterialPage? = null
        var publicFailure: String? = null
        isNetworkChecking = true
        try {
            publicPage = LearningMaterialsApi.loadPage(
                context = context,
                instrument = instrument,
                path = requestedPath,
                forceRefresh = forceRefresh
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            publicFailure = error.message ?: learningMaterialText(
                lang,
                "lm_server_error",
                "Không thể kết nối máy chủ tài liệu.",
                "Could not connect to the material server."
            )
            // A stale page is safer than replacing the list with an empty local folder.
            publicPage = LearningMaterialsApi.peekCachedPage(
                context = context,
                instrument = instrument,
                path = requestedPath,
                allowExpired = true
            )
        } finally {
            isNetworkChecking = false
        }

        // The path may have changed while local storage was being read. Never let an
        // old response overwrite the folder that is currently visible.
        if (normalizedPath(currentPath) != requestedPath) return@LaunchedEffect

        serverWarning = publicFailure
        isServerOnline = publicFailure == null && publicPage != null

        val previousPageForPath = mergedPageCache[requestedPath]
            ?: page?.takeIf { normalizedPath(it.currentPath) == requestedPath }

        when {
            publicPage == null && localPage == null && previousPageForPath != null -> {
                page = previousPageForPath
                mergedPageCache[requestedPath] = previousPageForPath
            }

            publicPage == null && localPage == null -> {
                page = null
                loadError = localFailure ?: publicFailure ?: learningMaterialText(
                    lang,
                    "lm_load_error",
                    "Không thể tải tài liệu.",
                    "Unable to load materials."
                )
            }

            else -> {
                val mergedItems = mergeLearningMaterialItems(
                    publicItems = publicPage?.items.orEmpty(),
                    localItems = localPage?.items.orEmpty()
                )
                val breadcrumbs = publicPage?.breadcrumbs
                    ?.takeIf { it.isNotEmpty() }
                    ?: localPage?.breadcrumbs
                        ?.takeIf { it.isNotEmpty() }
                    ?: buildLearningBreadcrumbs(instrument, requestedPath)

                val mergedPage = LearningMaterialPage(
                    instrument = instrument,
                    available = mergedItems.isNotEmpty() ||
                            publicPage?.available == true ||
                            localPage?.available == true,
                    currentPath = requestedPath,
                    parentPath = publicPage?.parentPath
                        ?: localPage?.parentPath
                        ?: parentOf(requestedPath),
                    breadcrumbs = breadcrumbs,
                    items = mergedItems,
                    message = publicPage?.message.orEmpty(),
                    contributionFolder = publicPage?.contributionFolder ?: "Cộng đồng",
                    canPublicUploadHere = publicPage?.canPublicUploadHere == true,
                    canPublicCreateFolderHere = publicPage?.canPublicCreateFolderHere == true,
                    currentFolderOwnerEmail = publicPage?.currentFolderOwnerEmail.orEmpty(),
                    currentFolderIsOwner = publicPage?.currentFolderIsOwner == true
                )
                page = mergedPage
                mergedPageCache[requestedPath] = mergedPage
            }
        }

        if (forceReloadPath == requestedPath) forceReloadPath = null
        isLoading = false
        if (!initialLoadReported) {
            initialLoadReported = true
            onInitialLoadFinished()
        }
    }

    // The online dot now calls a dedicated, database-free health endpoint. It no
    // longer downloads/scans the Guitar root every 4-8 seconds.
    LaunchedEffect(instrument, initialLoadReported, viewerItem?.id) {
        if (!initialLoadReported || viewerItem != null) return@LaunchedEffect
        delay(1_000L)
        while (isActive) {
            isNetworkChecking = true
            try {
                isServerOnline = LearningMaterialsApi.checkConnection(context, instrument)
            } finally {
                isNetworkChecking = false
            }
            delay(if (isServerOnline) 30_000L else 15_000L)
        }
    }

    BackHandler { goBack() }

    val allItems = page?.items.orEmpty()
    val selectedMaterialType = remember(selectedType) {
        when (selectedType) {
            "IMAGE" -> LearningMaterialType.IMAGE
            "PDF" -> LearningMaterialType.PDF
            "VIDEO" -> LearningMaterialType.VIDEO
            else -> null
        }
    }
    val visibleItems = remember(
        allItems,
        selectedMaterialType,
        searchText,
        isVietnamese,
        documentsAuthorized,
        accessInfo.permissions,
        instrument
    ) {
        val query = searchText.trim().lowercase()

        allItems
            .filter { item ->
                item.storage != LearningMaterialStorage.PUBLIC ||
                        !documentsAuthorized ||
                        accessInfo.permissions.canAccessFolder(instrument, item.path)
            }
            .filter { item ->
                val matchesType = when {
                    selectedMaterialType == null -> true
                    item.type == LearningMaterialType.FOLDER -> {
                        when {
                            item.containedCountsKnown -> (item.containedCounts[selectedMaterialType] ?: 0) > 0
                            item.containedTypesKnown -> selectedMaterialType in item.containedTypes
                            // Keep compatibility with an older API that returns neither summary.
                            else -> true
                        }
                    }
                    else -> item.type == selectedMaterialType
                }
                val title = learningMaterialTitle(item, isVietnamese).lowercase()
                val description = learningMaterialDescription(item, isVietnamese).lowercase()
                val matchesSearch = query.isBlank() || title.contains(query) || description.contains(query)
                matchesType && matchesSearch
            }
            .sortedWith(learningMaterialDisplayComparator(isVietnamese))
    }

    val folderCount = visibleItems.count { it.type == LearningMaterialType.FOLDER }
    val fileCount = visibleItems.size - folderCount
    val breadcrumbs = page?.breadcrumbs ?: buildLearningBreadcrumbs(instrument, currentPath)
    val currentFolderPath = breadcrumbs
        .joinToString(" / ") { it.name }
        .ifBlank { instrument }
    val contributionFolderName = page?.contributionFolder ?: "Cộng đồng"
    val normalizedCurrentPath = currentPath.replace('\\', '/').trim('/')
    val normalizedContributionPath = contributionFolderName.replace('\\', '/').trim('/')
    fun isContributionRootItem(item: LearningMaterialUiItem): Boolean =
        item.type == LearningMaterialType.FOLDER &&
                item.path.replace('\\', '/').trim('/') == normalizedContributionPath

    fun canManageLocalItem(item: LearningMaterialUiItem): Boolean =
        item.localFilePath.isNotBlank() &&
                (isDocumentAdmin || !isContributionRootItem(item))

    val isCommunityArea = normalizedCurrentPath == normalizedContributionPath ||
            (normalizedContributionPath.isNotBlank() && normalizedCurrentPath.startsWith("$normalizedContributionPath/"))
    val canPublicUploadHere = page?.canPublicUploadHere == true
    val canPublicCreateFolderHere = page?.canPublicCreateFolderHere == true
    val hasContributorWritePermission = accessInfo.permissions.canUpload || accessInfo.permissions.canCreateFolder
    val publicRestrictionMessage = if (
        !isDocumentAdmin && hasContributorWritePermission &&
        !canPublicUploadHere && !canPublicCreateFolderHere
    ) {
        if (isCommunityArea) {
            if (isVietnamese) {
                "Thư mục này thuộc thành viên khác hoặc là nội dung được bảo vệ. Bạn chỉ có quyền xem."
            } else {
                "This folder belongs to another member or is protected content. You have view-only access."
            }
        } else {
            if (isVietnamese) {
                "Muốn chia sẻ lên máy chủ, hãy vào thư mục $contributionFolderName."
            } else {
                "To share to the server, open the $contributionFolderName folder."
            }
        }
    } else null

    val networkTransition = rememberInfiniteTransition(label = "learning_network_status")
    val networkDotScale by networkTransition.animateFloat(
        initialValue = 0.76f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isServerOnline) 850 else 1250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "learning_network_dot_scale"
    )
    val networkDotAlpha by networkTransition.animateFloat(
        initialValue = 0.48f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isServerOnline) 850 else 1250),
            repeatMode = RepeatMode.Reverse
        ),
        label = "learning_network_dot_alpha"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goBack() },
                    modifier = Modifier
                        .size(40.dp)
                        .background(surfaceColor, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, learningMaterialText(lang, "lm_back", "Quay lại", "Back"), tint = textColor)
                }

                Spacer(modifier = Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = learningMaterialText(lang, "lm_title", "Tài liệu %1\$s", "%1\$s materials", instrument),
                        color = textColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = learningMaterialText(lang, "lm_subtitle", "Ảnh, PDF và video hướng dẫn", "Learning images, PDF files and videos"),
                        color = textColor.copy(alpha = 0.58f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                val headerActionShape = RoundedCornerShape(10.dp)
                val headerActionBorder = tunaGreen.copy(alpha = 0.38f)
                val networkColor = if (isServerOnline) Color(0xFF22C55E) else Color(0xFFEF5350)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Separate square button for adding files/folders.
                    IconButton(
                        onClick = {
                            addError = null
                            showAddDialog = true
                        },
                        enabled = !isAddBusy,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(headerActionShape)
                            .background(surfaceColor)
                            .border(1.dp, headerActionBorder, headerActionShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = learningMaterialText(
                                lang,
                                "lm_add_file_folder",
                                "Thêm file hoặc thư mục",
                                "Add file or folder"
                            ),
                            tint = tunaGreen,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    // Reserve a small outer frame so the animated status dot can sit
                    // precisely on the refresh button's top-right rounded corner.
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { requestContentReload() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(headerActionShape)
                                .background(surfaceColor)
                                .border(1.dp, headerActionBorder, headerActionShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (isServerOnline) {
                                    learningMaterialText(lang, "lm_refresh_online", "Làm mới - đang trực tuyến", "Refresh - online")
                                } else {
                                    learningMaterialText(lang, "lm_refresh_offline", "Làm mới - đang ngoại tuyến", "Refresh - offline")
                                },
                                tint = tunaGreen,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-1).dp, y = 1.dp)
                                .size(11.dp)
                                .graphicsLayer {
                                    scaleX = networkDotScale
                                    scaleY = networkDotScale
                                    alpha = networkDotAlpha
                                }
                                .clip(CircleShape)
                                .background(networkColor.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(networkColor)
                                    .border(1.2.dp, surfaceColor, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(11.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = surfaceColor,
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.14f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 11.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.50f),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 12.sp,
                            lineHeight = 14.sp
                        ),
                        cursorBrush = SolidColor(tunaGreen),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchText.isEmpty()) {
                                    Text(
                                        text = learningMaterialText(
                                            lang,
                                            "lm_search_hint",
                                            "Tìm tài liệu hoặc thư mục...",
                                            "Search files or folders..."
                                        ),
                                        color = textColor.copy(alpha = 0.42f),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    if (searchText.isNotEmpty()) {
                        IconButton(
                            onClick = { searchText = "" },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.52f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                breadcrumbs.forEachIndexed { index, breadcrumb ->
                    val isLast = index == breadcrumbs.lastIndex
                    Surface(
                        modifier = Modifier.clickable {
                            if (!isLast) {
                                navigateToPath(breadcrumb.path)
                            }
                        },
                        color = if (isLast) textColor.copy(alpha = 0.075f) else surfaceColor,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isLast) textColor.copy(alpha = 0.22f) else textColor.copy(alpha = 0.10f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (index == 0) {
                                Image(
                                    painter = painterResource(id = getInstIcon(instrument)),
                                    contentDescription = instrument,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = breadcrumb.name,
                                color = if (isLast) textColor else textColor.copy(alpha = 0.70f),
                                fontSize = 11.sp,
                                fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!isLast) {
                        Icon(
                            Icons.Default.ArrowForward,
                            null,
                            tint = textColor.copy(alpha = 0.30f),
                            modifier = Modifier.padding(horizontal = 3.dp).size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filterItems: List<Triple<String, String, ImageVector>> = listOf(
                Triple("ALL", learningMaterialText(lang, "lm_all", "Tất cả", "All"), Icons.Default.GridView),
                Triple("IMAGE", learningMaterialText(lang, "lm_images", "Hình ảnh", "Images"), Icons.Default.Collections),
                Triple("PDF", "PDF", Icons.Default.Description),
                Triple("VIDEO", learningMaterialText(lang, "lm_videos", "Video", "Videos"), Icons.Default.PlayArrow)
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                filterItems.forEach { (key, label, icon) ->
                    val selected = selectedType == key
                    val chipContentColor = if (selected) textColor else textColor.copy(alpha = 0.66f)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { selectedType = key },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selected) textColor.copy(alpha = 0.085f) else surfaceColor,
                        border = BorderStroke(
                            1.dp,
                            if (selected) tunaGreen.copy(alpha = 0.72f) else textColor.copy(alpha = 0.13f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = chipContentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = chipContentColor,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = learningMaterialText(lang, "lm_count", "%1\$d thư mục • %2\$d tài liệu", "%1\$d folders • %2\$d files", folderCount, fileCount),
                    color = textColor.copy(alpha = 0.58f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = learningMaterialText(lang, "lm_tap_add", "Nhấn + để thêm nội dung", "Tap + to add content"),
                    color = textColor.copy(alpha = 0.48f),
                    fontSize = 9.sp
                )
            }

            if (!serverWarning.isNullOrBlank() && page != null) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = learningMaterialText(
                        lang,
                        "lm_public_unavailable",
                        "Không kết nối được dữ liệu công khai; đang hiển thị nội dung trên máy.",
                        "Public data is unavailable; showing on-device content."
                    ),
                    color = Color(0xFFFFB74D),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Lần tải đầu tiên đã có overlay wave duy nhất ở LearningMaterialsScreen.
                    // Không tạo animation thứ hai phía sau overlay. Các lần refresh/chuyển
                    // thư mục sau khi màn hình đã hiển thị mới dùng wave tại đây.
                    if (initialLoadReported) {
                        LearningMaterialsLoadingWave(color = tunaGreen)
                    }
                }

                loadError != null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(surfaceColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFEF5350).copy(alpha = 0.30f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFEF5350), modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(learningMaterialText(lang, "lm_load_error", "Không thể tải tài liệu", "Unable to load materials"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(loadError.orEmpty(), color = textColor.copy(alpha = 0.55f), fontSize = 11.sp, lineHeight = 16.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showAddDialog = true }, border = BorderStroke(1.dp, tunaGreen)) {
                                Icon(Icons.Default.Add, null, tint = tunaGreen, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(learningMaterialText(lang, "lm_add_on_device", "Thêm trên máy", "Add on device"), color = tunaGreen)
                            }
                            Button(onClick = { requestContentReload() }, colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(learningMaterialText(lang, "lm_retry", "Thử lại", "Retry"))
                            }
                        }
                    }
                }

                allItems.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(surfaceColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .border(1.dp, textColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 26.dp, vertical = 22.dp)) {
                        Box(modifier = Modifier.size(64.dp).background(tunaGreen.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Folder, null, tint = tunaGreen, modifier = Modifier.size(35.dp))
                        }
                        Spacer(modifier = Modifier.height(13.dp))
                        Text(
                            text = learningMaterialText(lang, "lm_empty_title", "Thư mục này chưa có tài liệu", "This folder has no materials yet"),
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = learningMaterialText(
                                lang,
                                "lm_empty_desc",
                                "Bạn có thể thêm file hoặc tạo thư mục công khai hay lưu riêng trên máy.",
                                "Add a file or create a public or on-device folder."
                            ),
                            color = textColor.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                addError = null
                                showAddDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(learningMaterialText(lang, "lm_add_content", "Thêm nội dung", "Add content"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                visibleItems.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(surfaceColor.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                        .border(1.dp, textColor.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.Search, null, tint = textColor.copy(alpha = 0.28f), modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(learningMaterialText(lang, "lm_no_match", "Không tìm thấy tài liệu phù hợp", "No matching materials found"), color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(learningMaterialText(lang, "lm_no_match_hint", "Thử đổi từ khóa hoặc bộ lọc", "Try another keyword or filter"), color = textColor.copy(alpha = 0.50f), fontSize = 11.sp)
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(items = visibleItems, key = { it.id }) { item ->
                        LearningMaterialRow(
                            item = item,
                            isVietnamese = isVietnamese,
                            surfaceColor = surfaceColor,
                            textColor = textColor,
                            tunaGreen = tunaGreen,
                            childrenCount = if (
                                item.type == LearningMaterialType.FOLDER &&
                                selectedMaterialType != null &&
                                item.containedCountsKnown
                            ) {
                                item.containedCounts[selectedMaterialType] ?: 0
                            } else {
                                item.childrenCount
                            },
                            allowLocalManagement = canManageLocalItem(item),
                            isAccessLocked = item.type == LearningMaterialType.FOLDER &&
                                    !canOpenWithoutDocumentsRegistration(item),
                            onOpen = {
                                if (item.type == LearningMaterialType.FOLDER) {
                                    if (!requireDocumentsRegistrationFor(item)) {
                                        navigateToPath(item.path)
                                    }
                                }
                            },
                            onPreview = {
                                if (!requireDocumentsRegistrationFor(item)) {
                                    when (item.type) {
                                        LearningMaterialType.IMAGE,
                                        LearningMaterialType.PDF,
                                        LearningMaterialType.VIDEO -> viewerItem = item
                                        LearningMaterialType.FOLDER -> Unit
                                    }
                                }
                            },
                            onDownload = {
                                if (!requireDocumentsRegistrationFor(item)) {
                                    enqueueLearningMaterialDownload(context, item, lang)
                                }
                            },
                            onRenamePublic = {
                                renameTarget = LearningMaterialManageRequest(item, LearningMaterialManageScope.PUBLIC)
                            },
                            onDeletePublic = {
                                deleteTarget = LearningMaterialManageRequest(item, LearningMaterialManageScope.PUBLIC)
                            },
                            onRenameLocal = {
                                renameTarget = LearningMaterialManageRequest(item, LearningMaterialManageScope.LOCAL)
                            },
                            onDeleteLocal = {
                                deleteTarget = LearningMaterialManageRequest(item, LearningMaterialManageScope.LOCAL)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LearningMaterialAddDialog(
            isVietnamese = isVietnamese,
            currentFolderPath = currentFolderPath,
            surfaceColor = surfaceColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            isBusy = isAddBusy,
            errorMessage = addError,
            canUploadPublic = canPublicUploadHere,
            canCreatePublicFolder = canPublicCreateFolderHere,
            isCommunityArea = isCommunityArea,
            publicRestrictionMessage = publicRestrictionMessage,
            onDismiss = {
                if (!isAddBusy) {
                    showAddDialog = false
                    addError = null
                }
            },
            onChooseFile = { storage ->
                pendingFileStorage = storage
                pendingFilePath = currentPath
                showAddDialog = false
                addError = null
                filePicker.launch(arrayOf("image/*", "application/pdf", "video/*"))
            },
            onCreateFolder = { storage, folderName ->
                createFolder(storage, folderName)
            }
        )
    }

    renameTarget?.let { request ->
        val item = request.item
        LearningMaterialRenameDialog(
            item = item,
            isVietnamese = isVietnamese,
            surfaceColor = surfaceColor,
            bgColor = bgColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            isBusy = adminActionBusy,
            onDismiss = {
                if (!adminActionBusy) renameTarget = null
            },
            onConfirm = { newName ->
                val allowed = when (request.scope) {
                    LearningMaterialManageScope.PUBLIC -> item.canManageItem
                    LearningMaterialManageScope.LOCAL -> canManageLocalItem(item)
                }
                if (!allowed) return@LearningMaterialRenameDialog

                coroutineScope.launch {
                    adminActionBusy = true
                    try {
                        val message = when (request.scope) {
                            LearningMaterialManageScope.PUBLIC -> LearningMaterialsApi.renameItem(
                                context = context,
                                instrument = instrument,
                                path = item.path,
                                newName = newName
                            )
                            LearningMaterialManageScope.LOCAL -> LearningLocalMaterials.renameItem(
                                context = context,
                                instrument = instrument,
                                localFilePath = item.localFilePath,
                                newName = newName
                            )
                        }
                        renameTarget = null
                        if (viewerItem?.id == item.id) viewerItem = null
                        showMessage(message)
                        requestContentReload()
                    } catch (error: Exception) {
                        showMessage(error.message ?: if (isVietnamese) "Không thể đổi tên." else "Unable to rename.")
                    } finally {
                        adminActionBusy = false
                    }
                }
            }
        )
    }

    deleteTarget?.let { request ->
        val item = request.item
        LearningMaterialDeleteDialog(
            item = item,
            isVietnamese = isVietnamese,
            surfaceColor = surfaceColor,
            textColor = textColor,
            isBusy = adminActionBusy,
            onDismiss = {
                if (!adminActionBusy) deleteTarget = null
            },
            onConfirm = {
                val allowed = when (request.scope) {
                    LearningMaterialManageScope.PUBLIC -> item.canManageItem
                    LearningMaterialManageScope.LOCAL -> canManageLocalItem(item)
                }
                if (!allowed) return@LearningMaterialDeleteDialog

                coroutineScope.launch {
                    adminActionBusy = true
                    try {
                        val message = when (request.scope) {
                            LearningMaterialManageScope.PUBLIC -> LearningMaterialsApi.deleteItem(
                                context = context,
                                instrument = instrument,
                                path = item.path
                            )
                            LearningMaterialManageScope.LOCAL -> LearningLocalMaterials.deleteItem(
                                context = context,
                                instrument = instrument,
                                localFilePath = item.localFilePath
                            )
                        }
                        deleteTarget = null
                        if (viewerItem?.id == item.id) viewerItem = null
                        showMessage(message)
                        requestContentReload()
                    } catch (error: Exception) {
                        showMessage(error.message ?: if (isVietnamese) "Không thể xóa." else "Unable to delete.")
                    } finally {
                        adminActionBusy = false
                    }
                }
            }
        )
    }

    viewerItem?.let { item ->
        val videosInCurrentFolder = allItems
            .filter { it.type == LearningMaterialType.VIDEO }
            .sortedWith(learningMaterialDisplayComparator(isVietnamese))
        val currentVideoIndex = if (item.type == LearningMaterialType.VIDEO) {
            videosInCurrentFolder.indexOfFirst { it.id == item.id }
        } else {
            -1
        }

        LearningMaterialInAppViewer(
            item = item,
            isVietnamese = isVietnamese,
            bgColor = bgColor,
            surfaceColor = surfaceColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            hasPreviousVideo = currentVideoIndex > 0,
            hasNextVideo = currentVideoIndex >= 0 && currentVideoIndex < videosInCurrentFolder.lastIndex,
            onPreviousVideo = {
                if (currentVideoIndex > 0) viewerItem = videosInCurrentFolder[currentVideoIndex - 1]
            },
            onNextVideo = {
                if (currentVideoIndex >= 0 && currentVideoIndex < videosInCurrentFolder.lastIndex) {
                    viewerItem = videosInCurrentFolder[currentVideoIndex + 1]
                }
            },
            onDismiss = { viewerItem = null },
            onDownload = { enqueueLearningMaterialDownload(context, item, lang) }
        )
    }
}