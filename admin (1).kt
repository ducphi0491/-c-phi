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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
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

// =====================================================================
// admin.kt
// Module quản trị + hỗ trợ người dùng được tách khỏi MainActivity.kt.
//
// Bao gồm:
// - API hỗ trợ / Realtime inbox Admin -> User
// - Icon chat nổi và cửa sổ chat hỗ trợ
// - Cần xử lý / Lịch sử / tạo phiếu / chat trực tiếp
// - Duyệt bài
// - Thống kê, người dùng online, Admin chủ động liên hệ
// - Phân quyền Admin và quyền tài liệu
//
// Hai file cùng package ducphi.tunnerinstrument nên các hàm top-level vẫn gọi
// lẫn nhau như trước, không thay đổi PHP/MySQL/Firebase.
// =====================================================================

// =====================================================================
// ADMIN CHỦ ĐỘNG LIÊN HỆ NGƯỜI DÙNG
// =====================================================================
internal data class AdminContactRequest(
    val requestId: String,
    val ticketId: String,
    val adminName: String,
    val adminEmail: String,
    val title: String,
    val message: String,
    val status: String,
    val createdAt: Long
)

internal data class AdminContactInboxUiState(
    val requests: List<AdminContactRequest> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String = ""
)

internal data class AdminContactTarget(
    val deviceId: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String,
    val deviceName: String,
    val isOnline: Boolean
)

internal data class AdminContactSendResult(
    val success: Boolean,
    val message: String,
    val ticketId: String = "",
    val requestId: String = ""
)

internal data class UserSupportSendResult(
    val success: Boolean,
    val message: String,
    val ticketId: String = "",
    val reusedConversation: Boolean = false
)

internal data class SupportImageUploadResult(
    val imageUrl: String,
    val thumbnailUrl: String
)

private data class SupportImagePayload(
    val imageUrl: String,
    val thumbnailUrl: String,
    val caption: String
)

// Tin nhan anh tam thoi hien ngay trong chat truoc khi upload hoan tat.
// Tach khoi chatHistory de cac lan dong bo Realtime/PHP khong lam mat preview dang gui.
private data class PendingSupportImageUi(
    val localId: String,
    val imageUri: Uri,
    val caption: String,
    val senderRole: String,
    val stageText: String,
    val failed: Boolean = false,
    // Sau khi server da nhan tin nhan, van giu preview local den khi thumbnail remote
    // da nam trong Coil cache. Nho vay anh khong bi chop thanh nen mau trong luc doi model.
    val uploadedPayload: String = ""
)

private const val SUPPORT_IMAGE_PREFIX = "[[image]]"
private const val SUPPORT_THUMB_SEPARATOR = "[[thumb]]"

private fun buildSupportImagePayload(
    uploaded: SupportImageUploadResult,
    caption: String = ""
): String = buildString {
    append(SUPPORT_IMAGE_PREFIX)
    append(uploaded.imageUrl)
    if (uploaded.thumbnailUrl.isNotBlank() && uploaded.thumbnailUrl != uploaded.imageUrl) {
        append(SUPPORT_THUMB_SEPARATOR)
        append(uploaded.thumbnailUrl)
    }
    if (caption.isNotBlank()) {
        append('\n')
        append(caption.trim())
    }
}

private fun parseSupportImagePayload(text: String): SupportImagePayload? {
    if (!text.startsWith(SUPPORT_IMAGE_PREFIX)) return null

    val payload = text.removePrefix(SUPPORT_IMAGE_PREFIX)
    val firstLine = payload.substringBefore("\n").trim()
    if (firstLine.isBlank()) return null

    val imageUrl = firstLine.substringBefore(SUPPORT_THUMB_SEPARATOR).trim()
    if (!imageUrl.startsWith("http")) return null

    val thumbnailUrl = if (firstLine.contains(SUPPORT_THUMB_SEPARATOR)) {
        firstLine.substringAfter(SUPPORT_THUMB_SEPARATOR).trim().takeIf { it.startsWith("http") }
            ?: imageUrl
    } else {
        imageUrl
    }

    val caption = if (payload.contains("\n")) payload.substringAfter("\n").trim() else ""
    return SupportImagePayload(imageUrl, thumbnailUrl, caption)
}

/**
 * Các API hỗ trợ được tách sang admin.kt để MainActivity.kt gọn hơn; không cần sửa SongRepository.kt.
 * PHP vẫn là nơi kiểm tra email đăng nhập có nằm trong songs_app_admins hay không.
 */
internal object TunerSupportApi {
    private const val API_URL = "https://tunertools.top/api_songs.php"

    // SupportTicket hien tai chua co field adminRead. Luu gia tri doc tu JSON theo ticket_id
    // o day de co the bo sung read receipt ma khong phai sua SongRepository.kt.
    private val adminReadByTicket = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun adminReadForTicket(ticketId: String): Int = adminReadByTicket[ticketId] ?: 0

    private fun encodeForm(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    private suspend fun postForm(
        action: String,
        params: Map<String, String>
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val connection = (URL("$API_URL?action=${URLEncoder.encode(action, "UTF-8")}")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            useCaches = false
            connectTimeout = 15_000
            readTimeout = 45_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", "TunerApp-Android-v2.1")
        }

        try {
            val body = encodeForm(params).toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(body.size)
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            code to readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun get(
        action: String,
        params: Map<String, String>
    ): Pair<Int, String> = withContext(Dispatchers.IO) {
        val query = buildString {
            append("action=")
            append(URLEncoder.encode(action, "UTF-8"))
            params.forEach { (key, value) ->
                append('&')
                append(URLEncoder.encode(key, "UTF-8"))
                append('=')
                append(URLEncoder.encode(value, "UTF-8"))
            }
        }

        val connection = (URL("$API_URL?$query").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            useCaches = false
            connectTimeout = 15_000
            readTimeout = 45_000
            setRequestProperty("User-Agent", "TunerApp-Android-v2.1")
        }

        try {
            val code = connection.responseCode
            code to readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTicket(obj: org.json.JSONObject): SupportTicket {
        val chatHistory = mutableListOf<ChatMessage>()
        val ticketId = obj.optString("id", "")
        if (ticketId.isNotBlank()) {
            adminReadByTicket[ticketId] = obj.optInt("admin_read", 0)
        }
        val rawHistory = obj.optString("chat_history", "")

        if (rawHistory.isNotBlank() && rawHistory != "null") {
            runCatching {
                val messages = org.json.JSONArray(rawHistory)
                for (index in 0 until messages.length()) {
                    val message = messages.optJSONObject(index) ?: continue
                    chatHistory.add(
                        ChatMessage(
                            sender = message.optString("sender", "user"),
                            text = message.optString("text", ""),
                            timestamp = message.optString("timestamp", "")
                        )
                    )
                }
            }
        }

        if (chatHistory.isEmpty()) {
            val initialMessage = obj.optString("message", "")
            if (initialMessage.isNotBlank()) {
                chatHistory.add(
                    ChatMessage(
                        sender = "user",
                        text = initialMessage,
                        timestamp = obj.optString("created_at", "")
                    )
                )
            }

            val oldAdminReply = obj.optString("admin_reply", "")
            if (oldAdminReply.isNotBlank() && oldAdminReply != "null") {
                chatHistory.add(ChatMessage("admin", oldAdminReply, ""))
            }
        }

        return SupportTicket(
            id = ticketId,
            type = obj.optString("type", "support"),
            deviceId = obj.optString("device_id", ""),
            subject = obj.optString("subject", ""),
            message = obj.optString("message", ""),
            chatHistory = chatHistory,
            status = obj.optInt("status", 0),
            userRead = obj.optInt("user_read", 0),
            date = obj.optString("created_at", ""),
            userEmail = obj.optString("user_email", "")
        )
    }

    suspend fun getSupportData(
        deviceId: String,
        requesterEmail: String
    ): Pair<Int, List<SupportTicket>> {
        return try {
            val (code, body) = get(
                action = "get_support_data",
                params = mapOf(
                    "device_id" to deviceId,
                    "requester_email" to requesterEmail
                )
            )

            if (code !in 200..299 || body.isBlank()) return Pair(0, emptyList())

            val root = org.json.JSONObject(body)
            val unreadCount = root.optInt("unread_count", 0)
            val tickets = mutableListOf<SupportTicket>()
            val array = root.optJSONArray("tickets")
            if (array != null) {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    tickets.add(parseTicket(obj))
                }
            }
            Pair(unreadCount, tickets)
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể lấy dữ liệu hỗ trợ", error)
            Pair(0, emptyList())
        }
    }

    suspend fun getTicketDetail(
        ticketId: String,
        requesterEmail: String,
        deviceId: String
    ): SupportTicket? {
        if (ticketId.isBlank()) return null

        return try {
            val (code, body) = get(
                action = "get_ticket_detail",
                params = mapOf(
                    "id" to ticketId,
                    "requester_email" to requesterEmail,
                    "device_id" to deviceId
                )
            )

            if (code !in 200..299 || body.isBlank()) return null

            val root = org.json.JSONObject(body)
            if (!root.optBoolean("success", false)) return null

            val ticketObject = root.optJSONObject("ticket") ?: return null
            parseTicket(ticketObject)
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể tải chi tiết cuộc trò chuyện", error)
            null
        }
    }

    suspend fun replyTicket(
        ticketId: String,
        ticketType: String,
        requesterEmail: String,
        deviceId: String,
        replyText: String
    ): Boolean {
        if (ticketId.isBlank() || replyText.isBlank()) return false
        return try {
            val (code, body) = postForm(
                action = "reply_ticket",
                params = mapOf(
                    "id" to ticketId,
                    "type" to ticketType,
                    "requester_email" to requesterEmail,
                    "device_id" to deviceId,
                    "reply" to replyText
                )
            )
            code in 200..299 && runCatching {
                org.json.JSONObject(body).optBoolean("success", false)
            }.getOrDefault(false)
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể gửi phản hồi", error)
            false
        }
    }

    suspend fun uploadSupportImage(
        context: Context,
        ticketId: String,
        requesterEmail: String,
        deviceId: String,
        imageUri: Uri
    ): SupportImageUploadResult? = withContext(Dispatchers.IO) {
        if (ticketId.isBlank()) return@withContext null

        val resolver = context.contentResolver
        val sourceMimeType = resolver.getType(imageUri)?.lowercase() ?: "image/jpeg"
        if (!sourceMimeType.startsWith("image/")) return@withContext null

        // Anh chinh: du ro khi nguoi dung bam xem lon.
        // Thumbnail: rat nhe, chi dung trong danh sach chat de hien thi nhanh.
        val maxUploadDimension = 1600
        val uploadJpegQuality = 80
        val thumbnailMaxDimension = 480
        val thumbnailJpegQuality = 68

        var compressedFile: java.io.File? = null
        var thumbnailFile: java.io.File? = null
        var connection: HttpURLConnection? = null

        fun decodeScaledBitmap(): android.graphics.Bitmap? {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(resolver, imageUri)
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val sourceWidth = info.size.width.coerceAtLeast(1)
                    val sourceHeight = info.size.height.coerceAtLeast(1)
                    val longSide = maxOf(sourceWidth, sourceHeight)
                    if (longSide > maxUploadDimension) {
                        val scale = maxUploadDimension.toFloat() / longSide.toFloat()
                        decoder.setTargetSize(
                            (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                            (sourceHeight * scale).roundToInt().coerceAtLeast(1)
                        )
                    }
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = android.graphics.BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                resolver.openInputStream(imageUri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input, null, bounds)
                }
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

                var sampleSize = 1
                while (
                    bounds.outWidth / sampleSize > maxUploadDimension * 2 ||
                    bounds.outHeight / sampleSize > maxUploadDimension * 2
                ) {
                    sampleSize *= 2
                }

                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val decoded = resolver.openInputStream(imageUri)?.use { input ->
                    android.graphics.BitmapFactory.decodeStream(input, null, options)
                } ?: return null

                val longSide = maxOf(decoded.width, decoded.height)
                if (longSide <= maxUploadDimension) {
                    decoded
                } else {
                    val scale = maxUploadDimension.toFloat() / longSide.toFloat()
                    val scaled = android.graphics.Bitmap.createScaledBitmap(
                        decoded,
                        (decoded.width * scale).roundToInt().coerceAtLeast(1),
                        (decoded.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    )
                    if (scaled !== decoded) decoded.recycle()
                    scaled
                }
            }
        }

        try {
            val originalSize = runCatching {
                resolver.openAssetFileDescriptor(imageUri, "r")?.use { descriptor ->
                    descriptor.length.coerceAtLeast(0L)
                } ?: 0L
            }.getOrDefault(0L)

            val decodedBitmap = decodeScaledBitmap()
                ?: throw java.io.IOException("Khong giai ma duoc anh da chon")

            // JPEG khong ho tro alpha. Nen trong suot duoc dat thanh trang.
            val uploadBitmap = if (decodedBitmap.hasAlpha()) {
                val flattened = android.graphics.Bitmap.createBitmap(
                    decodedBitmap.width,
                    decodedBitmap.height,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                android.graphics.Canvas(flattened).apply {
                    drawColor(android.graphics.Color.WHITE)
                    drawBitmap(decodedBitmap, 0f, 0f, null)
                }
                if (flattened !== decodedBitmap) decodedBitmap.recycle()
                flattened
            } else {
                decodedBitmap
            }

            compressedFile = java.io.File.createTempFile(
                "tunertools_support_main_",
                ".jpg",
                context.cacheDir
            )
            java.io.FileOutputStream(compressedFile!!).buffered().use { output ->
                if (!uploadBitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG,
                        uploadJpegQuality,
                        output
                    )
                ) {
                    throw java.io.IOException("Khong nen duoc anh chinh")
                }
                output.flush()
            }

            val mainLongSide = maxOf(uploadBitmap.width, uploadBitmap.height).coerceAtLeast(1)
            val thumbnailBitmap = if (mainLongSide > thumbnailMaxDimension) {
                val scale = thumbnailMaxDimension.toFloat() / mainLongSide.toFloat()
                android.graphics.Bitmap.createScaledBitmap(
                    uploadBitmap,
                    (uploadBitmap.width * scale).roundToInt().coerceAtLeast(1),
                    (uploadBitmap.height * scale).roundToInt().coerceAtLeast(1),
                    true
                )
            } else {
                uploadBitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
            }

            thumbnailFile = java.io.File.createTempFile(
                "tunertools_support_thumb_",
                ".jpg",
                context.cacheDir
            )
            java.io.FileOutputStream(thumbnailFile!!).buffered().use { output ->
                if (!thumbnailBitmap.compress(
                        android.graphics.Bitmap.CompressFormat.JPEG,
                        thumbnailJpegQuality,
                        output
                    )
                ) {
                    throw java.io.IOException("Khong nen duoc thumbnail")
                }
                output.flush()
            }

            thumbnailBitmap.recycle()
            uploadBitmap.recycle()

            val uploadFile = compressedFile!!
            val thumbFile = thumbnailFile!!
            if (!uploadFile.isFile || uploadFile.length() <= 0L) {
                throw java.io.IOException("File anh sau khi nen khong hop le")
            }
            if (!thumbFile.isFile || thumbFile.length() <= 0L) {
                throw java.io.IOException("File thumbnail khong hop le")
            }

            android.util.Log.d(
                "SupportApi",
                "Support image optimized: original=${originalSize}B, " +
                        "main=${uploadFile.length()}B, thumb=${thumbFile.length()}B"
            )

            val boundary = "----TunerToolsChat${System.currentTimeMillis()}"
            connection = (
                    URL("$API_URL?action=upload_support_image").openConnection() as HttpURLConnection
                    ).apply {
                    requestMethod = "POST"
                    doOutput = true
                    useCaches = false
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    setChunkedStreamingMode(64 * 1024)
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    setRequestProperty("User-Agent", "TunerApp-Android-v2.1")
                }

            java.io.DataOutputStream(
                java.io.BufferedOutputStream(connection!!.outputStream)
            ).use { output ->
                fun writeUtf8(value: String) {
                    output.write(value.toByteArray(Charsets.UTF_8))
                }

                fun writeField(name: String, value: String) {
                    writeUtf8("--$boundary\r\n")
                    writeUtf8("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                    writeUtf8(value)
                    writeUtf8("\r\n")
                }

                fun writeJpegPart(name: String, file: java.io.File, fileName: String) {
                    writeUtf8("--$boundary\r\n")
                    writeUtf8(
                        "Content-Disposition: form-data; name=\"$name\"; " +
                                "filename=\"$fileName\"\r\n"
                    )
                    writeUtf8("Content-Type: image/jpeg\r\n\r\n")
                    file.inputStream().buffered(64 * 1024).use { stream ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = stream.read(buffer)
                            if (count <= 0) break
                            output.write(buffer, 0, count)
                        }
                    }
                    writeUtf8("\r\n")
                }

                writeField("id", ticketId)
                writeField("requester_email", requesterEmail)
                writeField("device_id", deviceId)

                val stamp = System.currentTimeMillis()
                writeJpegPart("image", uploadFile, "support_${stamp}.jpg")
                writeJpegPart("thumbnail", thumbFile, "support_${stamp}_thumb.jpg")
                writeUtf8("--$boundary--\r\n")
                output.flush()
            }

            val code = connection!!.responseCode
            val body = readResponse(connection!!)
            if (code !in 200..299 || body.isBlank()) return@withContext null

            val root = runCatching { org.json.JSONObject(body) }.getOrNull()
                ?: return@withContext null
            if (!root.optBoolean("success", false)) return@withContext null

            val imageUrl = root.optString("image_url", "").trim()
            if (!imageUrl.startsWith("http")) return@withContext null
            val thumbnailUrl = root.optString("thumbnail_url", "").trim()
                .takeIf { it.startsWith("http") }
                ?: imageUrl

            SupportImageUploadResult(
                imageUrl = imageUrl,
                thumbnailUrl = thumbnailUrl
            )
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Khong the toi uu/tai anh chat", error)
            null
        } finally {
            connection?.disconnect()
            runCatching { compressedFile?.delete() }
            runCatching { thumbnailFile?.delete() }
        }
    }

    suspend fun markUserRead(
        ticketId: String,
        ticketType: String,
        requesterEmail: String,
        deviceId: String
    ): Boolean {
        if (ticketId.isBlank()) return false
        return try {
            val (code, body) = postForm(
                action = "mark_user_read",
                params = mapOf(
                    "id" to ticketId,
                    "type" to ticketType,
                    "requester_email" to requesterEmail,
                    "device_id" to deviceId
                )
            )
            code in 200..299 && runCatching {
                org.json.JSONObject(body).optBoolean("success", false)
            }.getOrDefault(false)
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể đánh dấu đã đọc", error)
            false
        }
    }

    suspend fun markAdminRead(
        ticketId: String,
        ticketType: String,
        adminEmail: String,
        deviceId: String
    ): Boolean {
        if (ticketId.isBlank() || adminEmail.isBlank()) return false
        return try {
            val (code, body) = postForm(
                action = "mark_admin_read",
                params = mapOf(
                    "id" to ticketId,
                    "type" to ticketType,
                    "requester_email" to adminEmail,
                    "device_id" to deviceId
                )
            )
            val success = code in 200..299 && runCatching {
                org.json.JSONObject(body).optBoolean("success", false)
            }.getOrDefault(false)
            if (success) adminReadByTicket[ticketId] = 1
            success
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Khong the danh dau Admin da xem", error)
            false
        }
    }

    suspend fun deleteSupportTicket(
        ticketId: String,
        adminEmail: String
    ): Pair<Boolean, String> {
        if (ticketId.isBlank()) return false to "Mã cuộc trò chuyện không hợp lệ."
        if (adminEmail.isBlank()) return false to "Admin cần đăng nhập Google trước khi xóa."

        return try {
            val (code, body) = postForm(
                action = "delete_support_ticket",
                params = mapOf(
                    "id" to ticketId,
                    "admin_email" to adminEmail
                )
            )
            val json = runCatching { org.json.JSONObject(body) }.getOrNull()
            val success = code in 200..299 && json?.optBoolean("success", false) == true
            success to when {
                success -> json?.optString("message", "Đã xóa cuộc trò chuyện.").orEmpty()
                json != null -> json.optString("error", "Không thể xóa cuộc trò chuyện.")
                body.isNotBlank() -> body
                else -> "Không thể kết nối máy chủ."
            }
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể xóa cuộc trò chuyện", error)
            false to "Lỗi kết nối: ${error.message ?: "không xác định"}"
        }
    }

    suspend fun submitUserSupport(
        deviceId: String,
        requesterEmail: String,
        requesterName: String,
        subject: String,
        message: String
    ): UserSupportSendResult {
        if (deviceId.isBlank()) {
            return UserSupportSendResult(false, "Không xác định được thiết bị người dùng.")
        }
        if (message.isBlank()) {
            return UserSupportSendResult(false, "Nội dung hỗ trợ không được để trống.")
        }

        return try {
            val (code, body) = postForm(
                action = "contact_support",
                params = mapOf(
                    "device_id" to deviceId,
                    "requester_email" to requesterEmail.trim(),
                    "requester_name" to requesterName.trim(),
                    "subject" to subject.trim(),
                    "message" to message.trim()
                )
            )

            val json = runCatching { org.json.JSONObject(body) }.getOrNull()
            val success = code in 200..299 && json?.optBoolean("success", false) == true
            UserSupportSendResult(
                success = success,
                message = when {
                    success -> json?.optString("message", "Đã gửi thành công!").orEmpty()
                    json != null -> json.optString("error", "Không thể gửi nội dung hỗ trợ.")
                    body.isNotBlank() -> body
                    else -> "Không thể kết nối máy chủ."
                },
                ticketId = json?.optString("ticket_id", "").orEmpty(),
                reusedConversation = json?.optBoolean("reused_conversation", false) == true
            )
        } catch (error: Exception) {
            android.util.Log.e("SupportApi", "Không thể gửi nội dung hỗ trợ", error)
            UserSupportSendResult(false, "Lỗi kết nối: ${error.message ?: "không xác định"}")
        }
    }

    suspend fun startAdminContact(
        adminEmail: String,
        adminName: String,
        target: AdminContactTarget,
        subject: String,
        message: String
    ): AdminContactSendResult {
        if (adminEmail.isBlank()) {
            return AdminContactSendResult(false, "Admin cần đăng nhập Google trước khi liên hệ.")
        }
        if (target.deviceId.isBlank()) {
            return AdminContactSendResult(false, "Không xác định được thiết bị người dùng.")
        }
        if (message.isBlank()) {
            return AdminContactSendResult(false, "Nội dung liên hệ không được để trống.")
        }

        return try {
            val (code, body) = postForm(
                action = "admin_start_contact",
                params = mapOf(
                    "admin_email" to adminEmail,
                    "admin_name" to adminName,
                    "target_device_id" to target.deviceId,
                    "target_firebase_uid" to target.firebaseUid,
                    "target_email" to target.email,
                    "target_name" to target.displayName,
                    "target_device_name" to target.deviceName,
                    "subject" to subject,
                    "message" to message
                )
            )

            val json = runCatching { org.json.JSONObject(body) }.getOrNull()
            val success = code in 200..299 && json?.optBoolean("success", false) == true
            val responseMessage = when {
                success -> json?.optString("message", "Đã gửi lời nhắn.").orEmpty()
                json != null -> json.optString("error", "Không thể gửi lời nhắn.")
                body.isNotBlank() -> body
                else -> "Không thể kết nối máy chủ."
            }

            AdminContactSendResult(
                success = success,
                message = responseMessage,
                ticketId = json?.optString("ticket_id", "").orEmpty(),
                requestId = json?.optString("request_id", "").orEmpty()
            )
        } catch (error: Exception) {
            android.util.Log.e("AdminContact", "Không thể tạo cuộc trò chuyện", error)
            AdminContactSendResult(false, "Lỗi kết nối: ${error.message ?: "không xác định"}")
        }
    }
}

/**
 * Dem so lan dua ung dung tu background len foreground cho moi thiet bi.
 * Duoc gan tai composable inbox ma TunerScreen khoi tao cho MOI nguoi dung.
 * Khong doi logic dem thoi gian trong MainActivity va khong dem lai moi recomposition.
 * Du lieu cu (truoc ban nang cap) khong co app_open_count nen khong the suy ra.
 */
@Composable
private fun TrackAccountAppOpens(deviceId: String) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, deviceId) {
        if (deviceId.isBlank()) return@DisposableEffect onDispose { }

        val auth = FirebaseAuth.getInstance()
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        var foreground = false
        var recordedThisForeground = false
        var writeInProgress = false
        var generation = 0
        var disposed = false

        fun recordOpenIfNeeded() {
            if (disposed || !foreground || recordedThisForeground || writeInProgress) return
            val user = auth.currentUser ?: return
            val now = System.currentTimeMillis()
            val date = java.util.Date(now)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
            val month = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(date)
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
            val email = account?.email?.trim().takeUnless { it.isNullOrBlank() } ?: "guest"
            val name = account?.displayName?.takeIf { it.isNotBlank() }
                ?: if (email == "guest") "Khách" else email.substringBefore("@")
            val values = mapOf<String, Any>(
                "email" to email,
                "display_name" to name,
                "device_id" to deviceId,
                "firebase_uid" to user.uid,
                "app_open_count" to com.google.firebase.firestore.FieldValue.increment(1L),
                "last_access_time" to now,
                "device_name" to buildDeviceDisplayName(Build.MANUFACTURER, Build.BRAND, Build.MODEL),
                "device_manufacturer" to Build.MANUFACTURER,
                "device_brand" to Build.BRAND,
                "device_model" to Build.MODEL,
                "android_version" to Build.VERSION.RELEASE
            )

            writeInProgress = true
            val attemptGeneration = generation
            val batch = db.batch()
            batch.set(
                db.collection("Leaderboard_Daily").document(today).collection("Users").document(deviceId),
                values, com.google.firebase.firestore.SetOptions.merge()
            )
            batch.set(
                db.collection("Leaderboard_Monthly").document(month).collection("Users").document(deviceId),
                values, com.google.firebase.firestore.SetOptions.merge()
            )
            batch.commit()
                .addOnSuccessListener {
                    if (!disposed && generation == attemptGeneration) {
                        writeInProgress = false
                        recordedThisForeground = true
                    }
                }
                .addOnFailureListener { error ->
                    android.util.Log.e("AccountUsageStats", "Cannot record app opening", error)
                    if (!disposed && generation == attemptGeneration) writeInProgress = false
                }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    foreground = true
                    recordedThisForeground = false
                    writeInProgress = false
                    generation++
                    recordOpenIfNeeded()
                }
                Lifecycle.Event.ON_STOP -> {
                    foreground = false
                    generation++
                    recordedThisForeground = false
                    writeInProgress = false
                }
                else -> Unit
            }
        }
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) recordOpenIfNeeded()
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        auth.addAuthStateListener(authListener)
        // Compose co the duoc tao sau ON_START, van phai ghi nhan lan mo dau tien.
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !foreground) {
            foreground = true
            generation++
            recordOpenIfNeeded()
        }
        onDispose {
            disposed = true
            lifecycleOwner.lifecycle.removeObserver(observer)
            auth.removeAuthStateListener(authListener)
        }
    }
}

/**
 * Lắng nghe inbox chủ động của Admin theo Android device ID.
 * Request không hết hạn: nếu app đang tắt/offline, request vẫn nằm trong RTDB và
 * sẽ được nhận ngay khi thiết bị mở app, đăng nhập Firebase và có mạng trở lại.
 */
@Composable
internal fun rememberAdminContactInbox(deviceId: String): AdminContactInboxUiState {
    // Ham nay duoc goi o TunerScreen cho moi user, khong phai chi trong UI Admin.
    TrackAccountAppOpens(deviceId)
    var requests by remember(deviceId) { mutableStateOf<List<AdminContactRequest>>(emptyList()) }
    var isLoading by remember(deviceId) { mutableStateOf(true) }
    var errorMessage by remember(deviceId) { mutableStateOf("") }

    DisposableEffect(deviceId) {
        if (deviceId.isBlank()) {
            isLoading = false
            errorMessage = "Không xác định được thiết bị."
            return@DisposableEffect onDispose { }
        }

        val auth = FirebaseAuth.getInstance()
        val requestRoot = try {
            FirebaseDatabase.getInstance().getReference("contact_requests").child(deviceId)
        } catch (error: Exception) {
            isLoading = false
            errorMessage = "Chưa cấu hình Realtime Database."
            null
        }

        var requestListener: ValueEventListener? = null
        val retryHandler = Handler(Looper.getMainLooper())

        fun stopListening() {
            retryHandler.removeCallbacksAndMessages(null)
            requestListener?.let { listener -> requestRoot?.removeEventListener(listener) }
            requestListener = null
        }

        fun startListening() {
            if (requestRoot == null || requestListener != null) return

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requests = snapshot.children.mapNotNull { child ->
                        val requestId = child.child("request_id").getValue(String::class.java)
                            ?: child.key.orEmpty()
                        val ticketId = child.child("ticket_id").getValue(String::class.java).orEmpty()
                        if (requestId.isBlank() || ticketId.isBlank()) return@mapNotNull null

                        AdminContactRequest(
                            requestId = requestId,
                            ticketId = ticketId,
                            adminName = child.child("admin_name").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() }
                                ?: "Quản trị viên",
                            adminEmail = child.child("admin_email").getValue(String::class.java).orEmpty(),
                            title = child.child("title").getValue(String::class.java)
                                ?.takeIf { it.isNotBlank() }
                                ?: "Admin muốn liên hệ",
                            message = child.child("message").getValue(String::class.java).orEmpty(),
                            status = child.child("status").getValue(String::class.java)
                                ?.lowercase(java.util.Locale.ROOT)
                                ?: "pending",
                            createdAt = child.child("created_at").getValue(Long::class.java) ?: 0L
                        )
                    }
                        .filter { it.status in setOf("pending", "shown", "later", "accepted") }
                        .sortedWith(compareBy<AdminContactRequest> { it.createdAt }.thenBy { it.requestId })

                    errorMessage = ""
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Đang chờ đồng bộ hộp thư Admin..."
                    isLoading = false
                    requestListener = null
                    // Lần đầu mở app, user_directory có thể đang được ghi song song.
                    // Thử lại sau vài giây thay vì để listener bị hủy vĩnh viễn.
                    retryHandler.postDelayed({
                        if (auth.currentUser != null) startListening()
                    }, 3_000L)
                }
            }

            requestListener = listener
            requestRoot.addValueEventListener(listener)
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                startListening()
            } else {
                stopListening()
                requests = emptyList()
                isLoading = true
            }
        }

        auth.addAuthStateListener(authListener)
        if (auth.currentUser != null) startListening()

        onDispose {
            auth.removeAuthStateListener(authListener)
            stopListening()
        }
    }

    return AdminContactInboxUiState(
        requests = requests,
        isLoading = isLoading,
        errorMessage = errorMessage
    )
}

internal fun updateAdminContactRequestStatus(
    deviceId: String,
    requestId: String,
    newStatus: String,
    onComplete: ((Boolean) -> Unit)? = null
) {
    if (deviceId.isBlank() || requestId.isBlank()) {
        onComplete?.invoke(false)
        return
    }

    val updates = mapOf<String, Any>(
        "status" to newStatus,
        "updated_at" to ServerValue.TIMESTAMP
    )

    runCatching {
        FirebaseDatabase.getInstance()
            .getReference("contact_requests")
            .child(deviceId)
            .child(requestId)
            .updateChildren(updates)
            .addOnSuccessListener { onComplete?.invoke(true) }
            .addOnFailureListener { error ->
                android.util.Log.e("AdminContact", "Không thể cập nhật trạng thái request", error)
                onComplete?.invoke(false)
            }
    }.onFailure {
        android.util.Log.e("AdminContact", "Realtime Database chưa sẵn sàng", it)
        onComplete?.invoke(false)
    }
}


@Composable
internal fun DraggableSupportChatBubble(
    visible: Boolean,
    badgeCount: Int,
    tunaGreen: Color,
    surfaceColor: Color,
    onClick: () -> Unit
) {
    if (!visible) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val dragScope = rememberCoroutineScope()

    // Nút chat nhỏ gọn, dùng cùng phong cách khung bo góc như các nút phía trên.
    val popupSize = 32.dp
    val bubbleSize = 28.dp
    val edgeMargin = 3.dp
    val bottomReserved = 76.dp

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val popupSizePx = with(density) { popupSize.roundToPx() }
    val edgeMarginPx = with(density) { edgeMargin.roundToPx() }
    val bottomReservedPx = with(density) { bottomReserved.roundToPx() }

    val maxX = (screenWidthPx - popupSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
    val maxY = (screenHeightPx - popupSizePx - bottomReservedPx).coerceAtLeast(edgeMarginPx)

    // Mặc định đặt ngay BÊN DƯỚI và CAN GIỮA với nút AI.
    // Bản trước nhìn lệch sang phải vài dp do popup/badge; dịch thêm 4dp sang trái để tâm logo
    // trùng trực quan với tâm icon AI trên thanh công cụ.
    val defaultBubbleY = with(density) { 48.dp.roundToPx() }.coerceIn(edgeMarginPx, maxY)
    val defaultBubbleX = (maxX - with(density) { 60.dp.roundToPx() }).coerceAtLeast(edgeMarginPx)
    var bubbleX by rememberSaveable(defaultBubbleX, defaultBubbleY) { mutableIntStateOf(defaultBubbleX) }
    var bubbleY by rememberSaveable(defaultBubbleX, defaultBubbleY) { mutableIntStateOf(defaultBubbleY) }
    var suppressClick by remember { mutableStateOf(false) }

    LaunchedEffect(maxX, maxY) {
        bubbleX = bubbleX.coerceIn(edgeMarginPx, maxX)
        bubbleY = bubbleY.coerceIn(edgeMarginPx, maxY)
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(bubbleX, bubbleY),
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = true
        )
    ) {
        Box(
            modifier = Modifier
                .size(popupSize)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragStart = { suppressClick = true },
                        onDragEnd = {
                            dragScope.launch {
                                kotlinx.coroutines.delay(120L)
                                suppressClick = false
                            }
                        },
                        onDragCancel = {
                            dragScope.launch {
                                kotlinx.coroutines.delay(120L)
                                suppressClick = false
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        bubbleX = (bubbleX + dragAmount.x.roundToInt())
                            .coerceIn(edgeMarginPx, maxX)
                        bubbleY = (bubbleY + dragAmount.y.roundToInt())
                            .coerceIn(edgeMarginPx, maxY)
                    }
                }
                .clickable {
                    if (!suppressClick) onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            // Không dùng khung tròn nữa. Khung 8dp + viền xám 1dp đồng bộ với nút AI/chuông phía trên.
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .shadow(3.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(surfaceColor)
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Mở trò chuyện hỗ trợ",
                    modifier = Modifier
                        .size(21.dp)
                        .clip(RoundedCornerShape(5.dp))
                )
            }

            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .height(10.dp)
                        .sizeIn(minWidth = 10.dp)
                        .background(Color.Red, RoundedCornerShape(5.dp))
                        .border(0.5.dp, Color.White, RoundedCornerShape(5.dp))
                        .padding(horizontal = 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 5.5.sp,
                        lineHeight = 6.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun DirectSupportChatOverlay(
    ticketId: String,
    deviceId: String,
    tunaGreen: Color,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    isDarkMode: Boolean,
    currentLang: String,
    onMinimize: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val requesterEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
        .getLastSignedInAccount(context)?.email.orEmpty()

    // Bang mau rieng cho cua so chat: bo xanh la, dung tong mau don sac.
    // Tin nhan cua chinh nguoi gui se dung xanh duong nhe trong TicketChatView.
    val chatNeutralAccent = if (isDarkMode) Color(0xFFD9DDE2) else Color(0xFF4A5158)
    val chatNeutralBorder = if (isDarkMode) Color(0xFF4A5057) else Color(0xFFC7CDD3)
    val chatNeutralSurface = if (isDarkMode) Color(0xFF24282D) else Color(0xFFF0F2F4)
    val chatActionBackground = if (isDarkMode) Color(0xFF343A40) else Color(0xFFE3E7EB)
    val chatActionForeground = if (isDarkMode) Color(0xFFF4F6F8) else Color(0xFF2E343A)

    var ticket by remember(ticketId) { mutableStateOf<SupportTicket?>(null) }
    var isLoading by remember(ticketId) { mutableStateOf(true) }
    var loadError by remember(ticketId) { mutableStateOf("") }
    var reloadKey by remember(ticketId) { mutableIntStateOf(0) }

    LaunchedEffect(ticketId, reloadKey) {
        isLoading = true
        loadError = ""
        ticket = TunerSupportApi.getTicketDetail(
            ticketId = ticketId,
            requesterEmail = requesterEmail,
            deviceId = deviceId
        )
        if (ticket == null) {
            loadError = "Không thể tải cuộc trò chuyện. Vui lòng kiểm tra kết nối và thử lại."
        }
        isLoading = false
    }

    // Cửa sổ chat nhỏ hơn, nổi trên tab Bản nhạc và kéo được bằng thanh tiêu đề.
    val panelWidthDp = (configuration.screenWidthDp - 42).coerceIn(275, 312).dp
    val panelHeightDp = (configuration.screenHeightDp - 260).coerceIn(330, 430).dp
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    val panelWidthPx = with(density) { panelWidthDp.roundToPx() }
    val panelHeightPx = with(density) { panelHeightDp.roundToPx() }
    val edgePx = with(density) { 8.dp.roundToPx() }
    val bottomReservedPx = with(density) { 76.dp.roundToPx() }

    val maxX = (screenWidthPx - panelWidthPx - edgePx).coerceAtLeast(edgePx)
    val maxY = (screenHeightPx - panelHeightPx - bottomReservedPx).coerceAtLeast(edgePx)

    // Mỗi lần mở chat, đặt cửa sổ đúng giữa vùng hiển thị.
    // Sau đó người dùng vẫn có thể kéo cửa sổ đến vị trí khác trong phiên chat hiện tại.
    val centerX = ((screenWidthPx - panelWidthPx) / 2).coerceIn(edgePx, maxX)
    val availableHeightPx = (screenHeightPx - bottomReservedPx).coerceAtLeast(panelHeightPx)
    val centerY = ((availableHeightPx - panelHeightPx) / 2).coerceIn(edgePx, maxY)

    var panelX by remember(ticketId, screenWidthPx, panelWidthPx) { mutableIntStateOf(centerX) }
    var panelY by remember(ticketId, screenHeightPx, panelHeightPx) { mutableIntStateOf(centerY) }

    LaunchedEffect(maxX, maxY) {
        panelX = panelX.coerceIn(edgePx, maxX)
        panelY = panelY.coerceIn(edgePx, maxY)
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(panelX, panelY),
        onDismissRequest = onMinimize,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = true
        )
    ) {
        Surface(
            modifier = Modifier
                .width(panelWidthDp)
                .height(panelHeightDp)
                .imePadding(),
            color = bgColor,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 22.dp,
            border = BorderStroke(1.dp, chatNeutralBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(surfaceColor)
                        .pointerInput(maxX, maxY) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                panelX = (panelX + dragAmount.x.roundToInt())
                                    .coerceIn(edgePx, maxX)
                                panelY = (panelY + dragAmount.y.roundToInt())
                                    .coerceIn(edgePx, maxY)
                            }
                        }
                        .padding(start = 10.dp, end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(31.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(chatNeutralSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ticket?.subject?.ifBlank { "Trò chuyện hỗ trợ" } ?: "Trò chuyện hỗ trợ",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isLoading) "Đang kết nối..." else "Trực tiếp với Admin • kéo để di chuyển",
                            color = if (isLoading) Color.Gray else chatNeutralAccent,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }

                    // Thu nhỏ: ẩn cửa sổ chat nhưng GIỮ icon nổi để mở lại ngay.
                    IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Thu nhỏ",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.16f))

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = chatNeutralAccent, modifier = Modifier.size(26.dp))
                                    Spacer(Modifier.height(9.dp))
                                    Text("Đang tải cuộc trò chuyện...", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }

                        ticket != null -> {
                            TicketChatView(
                                ticket = ticket!!,
                                isAdmin = false,
                                onBack = onMinimize,
                                tunaGreen = tunaGreen,
                                bgColor = bgColor,
                                surfaceColor = surfaceColor,
                                textColor = textColor,
                                isDarkMode = isDarkMode,
                                currentLang = currentLang,
                                showHeader = false
                            )
                        }

                        else -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color.Red,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = loadError,
                                        color = textColor,
                                        fontSize = 11.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Button(
                                        onClick = { reloadKey++ },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = chatActionBackground,
                                            contentColor = chatActionForeground
                                        )
                                    ) {
                                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("Thử lại", color = chatActionForeground, fontSize = 11.sp)
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
fun SupportCenterDialog(
    userRole: String, // Thay isAdmin thành userRole
    deviceId: String,
    onDismiss: () -> Unit,
    tunaGreen: Color, surfaceColor: Color, bgColor: Color, textColor: Color, currentLang: String, isDarkMode: Boolean,
    onNavigateToTab: (String) -> Unit,
    initialTicketId: String? = null
) {
    val coroutineScope = rememberCoroutineScope()
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }
    var showCreateNew by remember { mutableStateOf(false) }
    var initialTicketHandled by remember(initialTicketId) { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    var ticketPendingDelete by remember { mutableStateOf<SupportTicket?>(null) }
    var isDeletingTicket by remember { mutableStateOf(false) }

    // Tên/Gmail được lấy từ user_directory (Realtime Database) để các phiếu cũ
    // cũng hiển thị được tên người dùng nếu dữ liệu này còn tồn tại.
    val supportDirectoryIdentity = remember { mutableStateMapOf<String, Pair<String, String>>() }

    val isStaff = userRole in listOf("super_admin", "admin", "mod")
    val context = LocalContext.current
    val requesterEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
        .getLastSignedInAccount(context)?.email.orEmpty()

    // Realtime presence dùng chung cho Cần xử lý / Lịch sử để Admin thấy ngay
    // người dùng hiện Online hay Offline.
    val supportPresence = rememberOnlinePresenceUiState()
    val supportOnlineEmails = remember(supportPresence.users) {
        supportPresence.users
            .mapNotNull { user ->
                user.email.trim().lowercase().takeIf { it.isNotBlank() && it != "guest" }
            }
            .toSet()
    }
    val supportOnlineDevices = remember(supportPresence.users) {
        supportPresence.users
            .mapNotNull { user -> user.deviceId.trim().takeIf { it.isNotBlank() } }
            .toSet()
    }

    // Khởi tạo 2 tab mặc định cho tất cả mọi người
    val tabs = mutableListOf(CoreTranslator.getString("pending", currentLang), CoreTranslator.getString("history", currentLang))

    // Nếu là BQT (Staff) thì thêm tab Duyệt bài và Thống kê
    if (isStaff) {
        tabs.add("Duyệt bài")
        tabs.add("Thống kê")
    }

    // Nếu là Boss (Super Admin) thì thêm tab quản trị hệ thống và quyền tài liệu.
    val adminRoleTabTitle = "Phân quyền"
    val documentPermissionTabTitle = documentAccessText(
        currentLang,
        "doc_admin_tab",
        "Quyền tài liệu",
        "Document access"
    )
    if (userRole == "super_admin") {
        tabs.add(adminRoleTabTitle)
        tabs.add(documentPermissionTabTitle)
    }
    fun refreshData() {
        isLoading = true
        coroutineScope.launch {
            val (_, list) = TunerSupportApi.getSupportData(deviceId, requesterEmail)
            tickets = list

            if (!initialTicketHandled && !initialTicketId.isNullOrBlank()) {
                list.firstOrNull { it.id == initialTicketId }?.let { ticket ->
                    selectedTicket = ticket
                    initialTicketHandled = true
                    if (!isStaff && ticket.status == 1 && ticket.userRead == 0) {
                        TunerSupportApi.markUserRead(
                            ticketId = ticket.id,
                            ticketType = ticket.type,
                            requesterEmail = requesterEmail,
                            deviceId = deviceId
                        )
                    }
                }
            }

            isLoading = false
        }
    }

    LaunchedEffect(initialTicketId) { refreshData() }

    LaunchedEffect(tickets, isStaff) {
        if (!isStaff || tickets.isEmpty()) return@LaunchedEffect
        val realtimeDb = runCatching { FirebaseDatabase.getInstance() }.getOrNull()
            ?: return@LaunchedEffect
        val directoryRoot = realtimeDb.getReference("user_directory")
        tickets.map { it.deviceId.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { ticketDeviceId ->
                val snapshot = runCatching { directoryRoot.child(ticketDeviceId).get().await() }.getOrNull()
                    ?: return@forEach
                val directoryEmail = snapshot.child("email").getValue(String::class.java).orEmpty().trim()
                val directoryName = snapshot.child("display_name").getValue(String::class.java).orEmpty().trim()
                if (directoryEmail.isNotBlank() || directoryName.isNotBlank()) {
                    supportDirectoryIdentity[ticketDeviceId] = directoryEmail to directoryName
                }
            }
    }

    val displayTickets = remember(tickets, selectedTabIndex, isStaff) {
        tickets.filter { ticket ->
            if (selectedTabIndex == 0) {
                if (isStaff) ticket.status == 0 else (ticket.status == 0 || (ticket.status == 1 && ticket.userRead == 0))
            } else {
                if (isStaff) ticket.status == 1 else (ticket.status == 1 && ticket.userRead == 1)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        SideEffect {
            val dialogWindowProvider = view.parent as? androidx.compose.ui.window.DialogWindowProvider
            dialogWindowProvider?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    hide(WindowInsetsCompat.Type.statusBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                // Giữ toàn bộ tiêu đề của Quản lý hỗ trợ / Lịch sử / Chat nằm dưới tai thỏ,
                // camera đục lỗ và vùng hệ thống trên các dòng máy khác nhau.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = 6.dp)
                .navigationBarsPadding()
                .imePadding(),
            color = bgColor
        ) {
            if (selectedTicket != null) {
                TicketChatView(
                    ticket = selectedTicket!!,
                    isAdmin = isStaff, // Truyền isStaff xuống chat view
                    onBack = { selectedTicket = null; refreshData() },
                    tunaGreen = tunaGreen, bgColor = bgColor, surfaceColor = surfaceColor, textColor = textColor, isDarkMode = isDarkMode, currentLang = currentLang
                )
            } else if (showCreateNew) {
                CreateTicketView(
                    deviceId = deviceId,
                    requesterEmail = requesterEmail,
                    onBack = { showCreateNew = false; refreshData() },
                    onCreated = { ticketId ->
                        showCreateNew = false
                        coroutineScope.launch {
                            val mergedTicket = TunerSupportApi.getTicketDetail(
                                ticketId = ticketId,
                                requesterEmail = requesterEmail,
                                deviceId = deviceId
                            )
                            if (mergedTicket != null) {
                                selectedTicket = mergedTicket
                            } else {
                                refreshData()
                            }
                        }
                    },
                    tunaGreen = tunaGreen,
                    surfaceColor = surfaceColor,
                    bgColor = bgColor,
                    textColor = textColor,
                    currentLang = currentLang
                )
            } else {
                Scaffold(
                    containerColor = bgColor,
                    floatingActionButton = {
                        if (!isStaff) {
                            FloatingActionButton(
                                onClick = { showCreateNew = true },
                                containerColor = tunaGreen,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Tạo mới")
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = textColor,
                                modifier = Modifier.clickable { onDismiss() }.padding(end = 16.dp)
                            )
                            Text(
                                text = CoreTranslator.getString(if (isStaff) "support_management" else "support_center", currentLang),
                                color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold
                            )
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = bgColor,
                            contentColor = tunaGreen,
                            edgePadding = 8.dp,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = tunaGreen
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = title,
                                            color = if (selectedTabIndex == index) tunaGreen else Color.Gray,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        if (selectedTabIndex == tabs.indexOf(documentPermissionTabTitle)) {
                            DocumentPermissionManagementView(
                                currentLang = currentLang,
                                tunaGreen = tunaGreen,
                                surfaceColor = surfaceColor,
                                bgColor = bgColor,
                                textColor = textColor
                            )
                        } else if (selectedTabIndex == tabs.indexOf(adminRoleTabTitle)) {
                            AdminRoleManagementView(tunaGreen = tunaGreen, surfaceColor = surfaceColor, bgColor = bgColor, textColor = textColor)
                        } else if (selectedTabIndex == tabs.indexOf("Duyệt bài")) {
                            PendingSongsView(
                                tunaGreen = tunaGreen,
                                surfaceColor = surfaceColor,
                                bgColor = bgColor,
                                textColor = textColor,
                                currentLang = currentLang, // Truyền thêm biến ngôn ngữ
                                deviceId = deviceId        // Truyền thêm ID thiết bị để lấy API
                            )
                        } else if (selectedTabIndex == tabs.indexOf("Thống kê")) {
                            DailyStatisticsView(
                                tunaGreen = tunaGreen,
                                surfaceColor = surfaceColor,
                                bgColor = bgColor,
                                textColor = textColor
                            )
                        } else {
                            if (isLoading) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = tunaGreen)
                                }
                            } else if (displayTickets.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (selectedTabIndex == 0) CoreTranslator.getString("no_pending_messages", currentLang) else CoreTranslator.getString("empty_history", currentLang),
                                        color = Color.Gray
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                                ) {
                                    items(displayTickets, key = { it.id }) { ticket ->
                                        val isUnread = if (isStaff) ticket.status == 0 else (ticket.status == 1 && ticket.userRead == 0)
                                        val directoryIdentity = supportDirectoryIdentity[ticket.deviceId.trim()]
                                        val directoryEmail = directoryIdentity?.first.orEmpty()
                                        val directoryName = directoryIdentity?.second.orEmpty()
                                        val ticketEmail = ticket.userEmail.trim().takeIf {
                                            it.isNotBlank() && !it.equals("guest", ignoreCase = true)
                                        } ?: directoryEmail.takeIf {
                                            it.isNotBlank() && !it.equals("guest", ignoreCase = true)
                                        }.orEmpty()
                                        val identityBase = if (ticketEmail.isNotBlank()) {
                                            "Gmail: $ticketEmail"
                                        } else {
                                            "Thiết bị: ${ticket.deviceId.ifBlank { "--" }}"
                                        }
                                        val identityLine = if (directoryName.isNotBlank() &&
                                            !directoryName.equals(ticketEmail, ignoreCase = true)
                                        ) {
                                            "$identityBase • $directoryName"
                                        } else identityBase
                                        val isTicketOnline =
                                            ticket.deviceId.trim() in supportOnlineDevices ||
                                                    (ticketEmail.isNotBlank() &&
                                                            ticketEmail.lowercase() in supportOnlineEmails)
                                        val rawLastMessage = ticket.chatHistory.lastOrNull()?.text ?: ticket.message
                                        val lastMessage = when {
                                            rawLastMessage.startsWith("[[image]]") -> "📷 Hình ảnh"
                                            rawLastMessage.isBlank() -> "--"
                                            else -> rawLastMessage.replace("\n", " ").trim()
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedTicket = ticket
                                                    if (!isStaff && ticket.status == 1 && ticket.userRead == 0) {
                                                        coroutineScope.launch {
                                                            TunerSupportApi.markUserRead(
                                                                ticketId = ticket.id,
                                                                ticketType = ticket.type,
                                                                requesterEmail = requesterEmail,
                                                                deviceId = deviceId
                                                            )
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (ticket.type == "report") CoreTranslator.getString("song_error_report", currentLang) else CoreTranslator.getString("support_tag", currentLang),
                                                        color = if (ticket.type == "report") Color.Red else tunaGreen,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    // Không hiển thị chấm đỏ chưa đọc tại [Hỗ trợ].
                                                    // Chỉ hiện chấm xanh nhỏ khi người dùng đang online.
                                                    if (isStaff && isTicketOnline) {
                                                        Spacer(modifier = Modifier.width(5.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .size(7.dp)
                                                                .background(tunaGreen, CircleShape)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(ticket.date.substringBefore(" "), color = Color.Gray, fontSize = 9.sp)
                                                }

                                                if (isStaff) {
                                                    Spacer(modifier = Modifier.height(5.dp))
                                                    Text(
                                                        text = identityLine,
                                                        color = tunaGreen.copy(alpha = 0.88f),
                                                        fontSize = when {
                                                            identityLine.length > 60 -> 8.5.sp
                                                            identityLine.length > 46 -> 9.sp
                                                            identityLine.length > 34 -> 10.sp
                                                            else -> 11.sp
                                                        },
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = ticket.subject.ifBlank { "Không có chủ đề" },
                                                    color = textColor,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = lastMessage,
                                                    color = Color.Gray,
                                                    fontSize = 12.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = if (ticket.status == 1) CoreTranslator.getString("replied", currentLang) else CoreTranslator.getString("pending_status", currentLang),
                                                    color = if (ticket.status == 1) tunaGreen else Color(0xFFFFA500),
                                                    fontSize = 10.sp,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }

                                            if (isStaff) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { ticketPendingDelete = ticket },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Xóa cuộc trò chuyện",
                                                        tint = Color.Red.copy(alpha = 0.9f),
                                                        modifier = Modifier.size(19.dp)
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
        }
    }

    ticketPendingDelete?.let { ticket ->
        AlertDialog(
            onDismissRequest = { if (!isDeletingTicket) ticketPendingDelete = null },
            containerColor = surfaceColor,
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.Red
                )
            },
            title = { Text("Xóa cuộc trò chuyện?", color = textColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Toàn bộ lịch sử của cuộc trò chuyện này sẽ bị xóa khỏi hệ thống.",
                    color = textColor
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { ticketPendingDelete = null },
                    enabled = !isDeletingTicket
                ) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDeletingTicket) return@Button
                        isDeletingTicket = true
                        coroutineScope.launch {
                            val (ok, message) = TunerSupportApi.deleteSupportTicket(
                                ticketId = ticket.id,
                                adminEmail = requesterEmail
                            )
                            isDeletingTicket = false
                            if (ok) {
                                if (selectedTicket?.id == ticket.id) selectedTicket = null
                                ticketPendingDelete = null
                                refreshData()
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isDeletingTicket,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (isDeletingTicket) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Xóa", color = Color.White)
                    }
                }
            }
        )
    }
}

@Composable
fun DocumentPermissionManagementView(
    currentLang: String,
    tunaGreen: Color,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleSignInClient = remember(context) {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
            context,
            buildTunerGoogleSignInOptions(context)
        )
    }

    var accessState by remember { mutableStateOf<DocumentAccessState>(DocumentAccessState.Checking) }
    var permissionList by remember { mutableStateOf<List<DocumentPermissionRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var editingRecord by remember { mutableStateOf<DocumentPermissionRecord?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    suspend fun loadPermissions(forceAuthentication: Boolean = false) {
        isLoading = true
        errorMessage = null

        val state = if (forceAuthentication) {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getLastSignedInAccount(context)
            DocumentAccessApi.loginWithAccount(context, account)
        } else {
            DocumentAccessApi.ensureSession(context)
        }
        accessState = state

        if (state is DocumentAccessState.Granted) {
            if (!state.info.permissions.canManage) {
                errorMessage = documentAccessText(
                    currentLang,
                    "doc_admin_no_manage_permission",
                    "Tài khoản này không có quyền quản lý tài liệu.",
                    "This account cannot manage document access."
                )
                permissionList = emptyList()
            } else {
                try {
                    permissionList = DocumentAccessApi.listPermissions(context)
                } catch (error: Exception) {
                    errorMessage = error.message ?: documentAccessText(
                        currentLang,
                        "server_error",
                        "Lỗi kết nối máy chủ!",
                        "Server connection error!"
                    )
                }
            }
        }
        isLoading = false
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            coroutineScope.launch {
                isLoading = true
                val account = try {
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getSignedInAccountFromIntent(result.data)
                        .getResult(com.google.android.gms.common.api.ApiException::class.java)
                } catch (_: Exception) {
                    null
                }
                accessState = DocumentAccessApi.loginWithAccount(context, account)
                if (accessState is DocumentAccessState.Granted) {
                    loadPermissions()
                } else {
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) { loadPermissions() }

    val filteredList = remember(permissionList, searchText) {
        val query = searchText.trim().lowercase()
        if (query.isBlank()) permissionList
        else permissionList.filter {
            it.email.lowercase().contains(query) || it.note.lowercase().contains(query)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotBlank()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            documentAccessText(
                                currentLang,
                                "doc_admin_search_email",
                                "Tìm Gmail...",
                                "Search Gmail..."
                            )
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = surfaceColor,
                        unfocusedContainerColor = surfaceColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedIndicatorColor = tunaGreen,
                        cursorColor = tunaGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = { coroutineScope.launch { loadPermissions() } },
                    modifier = Modifier
                        .size(44.dp)
                        .background(surfaceColor, RoundedCornerShape(12.dp))
                        .border(1.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = tunaGreen)
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = tunaGreen)
                }

                accessState is DocumentAccessState.LoginRequired -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Login, null, tint = textColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            documentAccessText(
                                currentLang,
                                "doc_access_login_required",
                                "Cần đăng nhập Google",
                                "Google login required"
                            ),
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val loginRequired = accessState as? DocumentAccessState.LoginRequired
                                if (loginRequired?.requiresReLogin == true) {
                                    DocumentAccessStore.clear(context)
                                    googleSignInClient.signOut().addOnCompleteListener {
                                        signInLauncher.launch(googleSignInClient.signInIntent)
                                    }
                                } else {
                                    signInLauncher.launch(googleSignInClient.signInIntent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                        ) {
                            Text(
                                documentAccessText(
                                    currentLang,
                                    "doc_access_login",
                                    "Đăng nhập bằng Google",
                                    "Sign in with Google"
                                )
                            )
                        }
                    }
                }

                accessState is DocumentAccessState.Denied -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, null, tint = textColor, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            (accessState as DocumentAccessState.Denied).message,
                            color = textColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            DocumentAccessStore.clear(context)
                            googleSignInClient.signOut().addOnCompleteListener {
                                signInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        }) {
                            Text(
                                documentAccessText(
                                    currentLang,
                                    "doc_access_switch_account",
                                    "Đổi tài khoản",
                                    "Switch account"
                                )
                            )
                        }
                    }
                }

                errorMessage != null || accessState is DocumentAccessState.Error -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            errorMessage ?: (accessState as? DocumentAccessState.Error)?.message.orEmpty(),
                            color = textColor,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { coroutineScope.launch { loadPermissions() } },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                        ) {
                            Text(documentAccessText(currentLang, "lm_retry", "Thử lại", "Retry"))
                        }
                    }
                }

                filteredList.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        documentAccessText(
                            currentLang,
                            "doc_admin_empty",
                            "Chưa có Gmail được cấp quyền.",
                            "No Gmail account has been granted access."
                        ),
                        color = textColor.copy(alpha = 0.55f)
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { record ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingRecord = record
                                    showEditor = true
                                },
                            colors = CardDefaults.cardColors(containerColor = surfaceColor),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (record.status == "active") Icons.Default.CheckCircle else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (record.status == "active") tunaGreen else Color.Gray,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = record.email,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val rights = buildList {
                                        if (record.canView) add(documentAccessText(currentLang, "doc_perm_view", "Xem", "View"))
                                        if (record.canDownload) add(documentAccessText(currentLang, "doc_perm_download", "Tải", "Download"))
                                        if (record.canUpload) add(documentAccessText(currentLang, "doc_perm_upload", "Thêm file", "Upload"))
                                        if (record.canCreateFolder) add(documentAccessText(currentLang, "doc_perm_folder", "Tạo folder", "Create folder"))
                                        if (record.canManage) add(documentAccessText(currentLang, "doc_perm_manage", "Quản lý", "Manage"))
                                    }
                                    Text(
                                        text = rights.joinToString(" • ").ifBlank { "-" },
                                        color = textColor.copy(alpha = 0.58f),
                                        fontSize = 11.sp,
                                        maxLines = 2
                                    )
                                    record.expiresAt?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = documentAccessText(
                                                currentLang,
                                                "doc_perm_expires_value",
                                                vietnameseFallback = "Hết hạn: %1\$s",
                                                englishFallback = "Expires: %1\$s",
                                                it
                                            ),
                                            color = textColor.copy(alpha = 0.45f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Icon(Icons.Default.Edit, null, tint = textColor.copy(alpha = 0.55f))
                            }
                        }
                    }
                }
            }
        }

        val granted = accessState as? DocumentAccessState.Granted
        if (granted?.info?.permissions?.canManage == true && !isLoading) {
            FloatingActionButton(
                onClick = {
                    editingRecord = DocumentPermissionRecord()
                    showEditor = true
                },
                containerColor = tunaGreen,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }

    if (showEditor) {
        DocumentPermissionEditorDialog(
            currentLang = currentLang,
            record = editingRecord ?: DocumentPermissionRecord(),
            surfaceColor = surfaceColor,
            bgColor = bgColor,
            textColor = textColor,
            tunaGreen = tunaGreen,
            onDismiss = {
                showEditor = false
                editingRecord = null
            },
            onSave = { record ->
                coroutineScope.launch {
                    try {
                        DocumentAccessApi.savePermission(context, record)
                        showEditor = false
                        editingRecord = null
                        loadPermissions()
                    } catch (error: Exception) {
                        errorMessage = error.message
                    }
                }
            },
            onDelete = { record ->
                coroutineScope.launch {
                    try {
                        if (record.id > 0) DocumentAccessApi.deletePermission(context, record.id)
                        showEditor = false
                        editingRecord = null
                        loadPermissions()
                    } catch (error: Exception) {
                        errorMessage = error.message
                    }
                }
            }
        )
    }
}

@Composable
private fun DocumentPermissionEditorDialog(
    currentLang: String,
    record: DocumentPermissionRecord,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    tunaGreen: Color,
    onDismiss: () -> Unit,
    onSave: (DocumentPermissionRecord) -> Unit,
    onDelete: (DocumentPermissionRecord) -> Unit
) {
    var email by remember(record.id, record.email) { mutableStateOf(record.email) }
    var canView by remember(record.id) { mutableStateOf(record.canView) }
    var canDownload by remember(record.id) { mutableStateOf(record.canDownload) }
    var canUpload by remember(record.id) { mutableStateOf(record.canUpload) }
    var canCreateFolder by remember(record.id) { mutableStateOf(record.canCreateFolder) }
    var canManage by remember(record.id) { mutableStateOf(record.canManage) }
    var canViewAllFolders by remember(record.id) { mutableStateOf(record.canViewAllFolders) }
    var selectedFolderPaths by remember(record.id, record.allowedFolderPaths) {
        mutableStateOf(record.allowedFolderPaths)
    }
    var isActive by remember(record.id) { mutableStateOf(record.status != "disabled") }
    var expiresAt by remember(record.id, record.expiresAt) { mutableStateOf(record.expiresAt.orEmpty()) }
    var note by remember(record.id, record.note) { mutableStateOf(record.note) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fallbackFolderOptions = remember {
        listOf(
            "1. Cơ bản",
            "2. Hướng dẫn giáo trình Carulli",
            "3. Hướng dẫn Đệm hát",
            "4. fingerstyle",
            "5. Bí kíp Guitar",
            "6. Luyện ngón",
            "Cộng đồng"
        ).map { name ->
            DocumentFolderOption(
                key = "Guitar/$name",
                instrument = "Guitar",
                path = name,
                name = name
            )
        }
    }
    var folderOptions by remember { mutableStateOf(fallbackFolderOptions) }
    var folderOptionsLoading by remember { mutableStateOf(true) }
    var folderOptionsError by remember { mutableStateOf("") }

    LaunchedEffect(record.id) {
        folderOptionsLoading = true
        folderOptionsError = ""
        try {
            val serverOptions = DocumentAccessApi.listFolderOptions(context, "Guitar")
            if (serverOptions.isNotEmpty()) folderOptions = serverOptions
        } catch (error: Exception) {
            folderOptionsError = error.message.orEmpty()
            // Giữ danh sách dự phòng để Admin vẫn thiết lập được đúng 7 thư mục hiện tại.
        } finally {
            folderOptionsLoading = false
        }
    }

    val normalizedEmail = email.trim().lowercase(java.util.Locale.ROOT)
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()
    val configuration = LocalConfiguration.current
    val maxDialogHeight = (configuration.screenHeightDp - 28).coerceAtLeast(360).dp

    @Composable
    fun permissionRow(
        title: String,
        icon: ImageVector,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor.copy(alpha = 0.72f), modifier = Modifier.size(21.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = textColor, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = tunaGreen)
            )
        }
    }

    fun saveRecord() {
        if (!isEmailValid) return
        onSave(
            record.copy(
                email = normalizedEmail,
                canView = canView,
                canDownload = canDownload,
                canUpload = canUpload,
                canCreateFolder = canCreateFolder,
                canManage = canManage,
                canViewAllFolders = canManage || canViewAllFolders,
                allowedFolderPaths = if (canManage || canViewAllFolders) {
                    emptySet()
                } else {
                    selectedFolderPaths
                },
                status = if (isActive) "active" else "disabled",
                expiresAt = expiresAt.trim().ifBlank { null },
                note = note.trim()
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight),
                color = surfaceColor,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (record.id > 0) {
                                documentAccessText(
                                    currentLang,
                                    "doc_admin_edit",
                                    "Cập nhật quyền tài liệu",
                                    "Update document access"
                                )
                            } else {
                                documentAccessText(
                                    currentLang,
                                    "doc_admin_add",
                                    "Thêm Gmail được cấp quyền",
                                    "Grant Gmail access"
                                )
                            },
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = documentAccessText(
                                    currentLang,
                                    "cancel",
                                    "Đóng",
                                    "Close"
                                ),
                                tint = textColor.copy(alpha = 0.68f)
                            )
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.10f))

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = record.id == 0L,
                            isError = email.isNotBlank() && !isEmailValid,
                            label = { Text("Gmail") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            supportingText = {
                                if (email.isNotBlank() && !isEmailValid) {
                                    Text(
                                        text = documentAccessText(
                                            currentLang,
                                            "invalid_email",
                                            "Địa chỉ Gmail chưa đúng định dạng",
                                            "Invalid Gmail address"
                                        ),
                                        color = Color(0xFFEF5350)
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = bgColor,
                                unfocusedContainerColor = bgColor,
                                disabledContainerColor = bgColor,
                                errorContainerColor = bgColor,
                                focusedIndicatorColor = tunaGreen,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                disabledTextColor = textColor.copy(alpha = 0.75f)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_view", "Cho phép xem", "Allow viewing"),
                            Icons.Default.Description,
                            canView
                        ) { canView = it }

                        if (canView) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = bgColor,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = tunaGreen,
                                            modifier = Modifier.size(19.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Phạm vi thư mục được xem",
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (folderOptionsLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = tunaGreen,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                canViewAllFolders = true
                                                selectedFolderPaths = emptySet()
                                            }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = canViewAllFolders,
                                            onClick = {
                                                canViewAllFolders = true
                                                selectedFolderPaths = emptySet()
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = tunaGreen)
                                        )
                                        Text("Tất cả thư mục", color = textColor, fontSize = 13.sp)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { canViewAllFolders = false }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = !canViewAllFolders,
                                            onClick = { canViewAllFolders = false },
                                            colors = RadioButtonDefaults.colors(selectedColor = tunaGreen)
                                        )
                                        Text("Chỉ những thư mục được chọn", color = textColor, fontSize = 13.sp)
                                    }

                                    if (!canViewAllFolders) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        folderOptions.forEach { option ->
                                            val checked = option.key in selectedFolderPaths
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedFolderPaths = if (checked) {
                                                            selectedFolderPaths - option.key
                                                        } else {
                                                            selectedFolderPaths + option.key
                                                        }
                                                    }
                                                    .padding(start = 16.dp, top = 2.dp, bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { shouldCheck ->
                                                        selectedFolderPaths = if (shouldCheck) {
                                                            selectedFolderPaths + option.key
                                                        } else {
                                                            selectedFolderPaths - option.key
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(checkedColor = tunaGreen)
                                                )
                                                Text(
                                                    option.name,
                                                    color = textColor,
                                                    fontSize = 12.5.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                        if (selectedFolderPaths.isEmpty()) {
                                            Text(
                                                "Chưa chọn thư mục: tài khoản sẽ không mở được thư mục tài liệu nào.",
                                                color = Color(0xFFFFA726),
                                                fontSize = 10.5.sp,
                                                modifier = Modifier.padding(start = 18.dp, top = 4.dp)
                                            )
                                        }
                                    }

                                    if (folderOptionsError.isNotBlank()) {
                                        Text(
                                            "Đang dùng danh sách thư mục dự phòng do chưa tải được danh sách từ máy chủ.",
                                            color = Color.Gray,
                                            fontSize = 9.5.sp,
                                            modifier = Modifier.padding(top = 5.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_download", "Cho phép tải", "Allow download"),
                            Icons.Default.Download,
                            canDownload
                        ) { canDownload = it }
                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_upload", "Cho phép thêm file", "Allow upload"),
                            Icons.Default.Description,
                            canUpload
                        ) { canUpload = it }
                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_folder", "Cho phép tạo folder", "Allow folder creation"),
                            Icons.Default.Folder,
                            canCreateFolder
                        ) { canCreateFolder = it }
                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_manage", "Cho phép quản lý quyền", "Allow permission management"),
                            Icons.Default.AdminPanelSettings,
                            canManage
                        ) {
                            canManage = it
                            if (it) {
                                canView = true
                                canViewAllFolders = true
                                selectedFolderPaths = emptySet()
                            }
                        }
                        permissionRow(
                            documentAccessText(currentLang, "doc_perm_active", "Đang hoạt động", "Active"),
                            Icons.Default.CheckCircle,
                            isActive
                        ) { isActive = it }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = expiresAt,
                            onValueChange = { expiresAt = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text(
                                    documentAccessText(
                                        currentLang,
                                        "doc_perm_expires",
                                        "Ngày hết hạn (tùy chọn)",
                                        "Expiry date (optional)"
                                    )
                                )
                            },
                            placeholder = { Text("YYYY-MM-DD HH:mm:ss") },
                            leadingIcon = { Icon(Icons.Default.DateRange, null) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = bgColor,
                                unfocusedContainerColor = bgColor,
                                focusedIndicatorColor = tunaGreen,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(255) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            label = {
                                Text(
                                    documentAccessText(
                                        currentLang,
                                        "additional_notes",
                                        "Ghi chú",
                                        "Notes"
                                    )
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = bgColor,
                                unfocusedContainerColor = bgColor,
                                focusedIndicatorColor = tunaGreen,
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor
                            )
                        )

                        if (record.id > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { confirmDelete = true },
                                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF5350))
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    documentAccessText(
                                        currentLang,
                                        "doc_admin_delete",
                                        "Xóa quyền Gmail này",
                                        "Delete this Gmail permission"
                                    ),
                                    color = Color(0xFFEF5350)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.10f))

                    // Hàng thao tác luôn cố định ở đáy, không bị nội dung hoặc bàn phím che khuất.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                documentAccessText(currentLang, "cancel", "Hủy", "Cancel"),
                                color = textColor.copy(alpha = 0.68f)
                            )
                        }

                        Button(
                            onClick = { saveRecord() },
                            enabled = isEmailValid,
                            modifier = Modifier.weight(1.35f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = tunaGreen,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.24f),
                                disabledContentColor = textColor.copy(alpha = 0.42f)
                            )
                        ) {
                            Icon(
                                imageVector = if (record.id > 0) Icons.Default.Save else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (record.id > 0) {
                                    documentAccessText(
                                        currentLang,
                                        "save_changes",
                                        "Lưu thay đổi",
                                        "Save changes"
                                    )
                                } else {
                                    documentAccessText(
                                        currentLang,
                                        "add_permission",
                                        "Thêm quyền",
                                        "Grant access"
                                    )
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = surfaceColor,
            title = { Text(documentAccessText(currentLang, "delete", "Xóa", "Delete"), color = textColor) },
            text = {
                Text(
                    documentAccessText(
                        currentLang,
                        "doc_admin_delete_confirm",
                        vietnameseFallback = "Hết hạn: %1\$s",
                        englishFallback = "Expires: %1\$s",
                        record.email
                    ),
                    color = textColor
                )
            },
            confirmButton = {
                Button(
                    onClick = { confirmDelete = false; onDelete(record) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                ) {
                    Text(documentAccessText(currentLang, "delete", "Xóa", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(documentAccessText(currentLang, "cancel", "Hủy", "Cancel"), color = textColor)
                }
            }
        )
    }
}

@Composable
fun AdminRoleManagementView(tunaGreen: Color, surfaceColor: Color, bgColor: Color, textColor: Color) {
    var adminList by remember { mutableStateOf<List<AdminAccount>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editAdmin by remember { mutableStateOf<AdminAccount?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadData() {
        coroutineScope.launch { adminList = SongScraper.getAdmins() }
    }

    LaunchedEffect(Unit) { loadData() }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
            items(adminList) { admin ->
                val iconColor = when (admin.role) {
                    "super_admin" -> Color.Red
                    "admin" -> Color(0xFFE6A23C)
                    "mod" -> tunaGreen
                    else -> Color.Gray
                }

                Row(
                    modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).clickable { editAdmin = admin }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = iconColor, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(admin.email, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Quyền: ${admin.role.uppercase()} • ${admin.createdAt.substringBefore(" ")}", color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.Edit, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = tunaGreen,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, "Thêm")
        }
    }

    // DIALOG THÊM MỚI TÀI KHOẢN
    if (showAddDialog) {
        var newEmail by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("mod") }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Thêm tài khoản quản trị", color = tunaGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newEmail, onValueChange = { newEmail = it },
                        label = { Text("Địa chỉ Email (Gmail)", color = Color.Gray) },
                        textStyle = TextStyle(color = textColor),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedContainerColor = bgColor, unfocusedContainerColor = bgColor, focusedIndicatorColor = tunaGreen)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Phân quyền:", color = textColor, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newRole == "super_admin", onClick = { newRole = "super_admin" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Red))
                        Text("Super Admin (Tối cao)", color = textColor, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newRole == "admin", onClick = { newRole = "admin" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE6A23C)))
                        Text("Admin (Quản trị viên)", color = textColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newRole == "mod", onClick = { newRole = "mod" }, colors = RadioButtonDefaults.colors(selectedColor = tunaGreen))
                        Text("Moderator (Kiểm duyệt)", color = textColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = newRole == "user", onClick = { newRole = "user" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Gray))
                        Text("User (Khóa quyền)", color = textColor)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddDialog = false }) { Text("Hủy", color = Color.Gray) }
                        Button(
                            onClick = {
                                if (newEmail.isNotBlank()) {
                                    isSubmitting = true
                                    coroutineScope.launch {
                                        if (SongScraper.addAdmin(newEmail, newRole)) {
                                            showAddDialog = false; loadData()
                                        } else Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show()
                                        isSubmitting = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                        ) {
                            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Thêm", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // DIALOG SỬA / XÓA QUYỀN
    if (editAdmin != null) {
        var selectedRole by remember { mutableStateOf(editAdmin!!.role) }
        var isSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { editAdmin = null }) {
            Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Cập nhật quyền", color = tunaGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(editAdmin!!.email, color = textColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "super_admin", onClick = { selectedRole = "super_admin" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Red))
                        Text("Super Admin (Tối cao)", color = textColor, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "admin", onClick = { selectedRole = "admin" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE6A23C)))
                        Text("Admin (Quản trị viên)", color = textColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "mod", onClick = { selectedRole = "mod" }, colors = RadioButtonDefaults.colors(selectedColor = tunaGreen))
                        Text("Moderator (Kiểm duyệt)", color = textColor)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRole == "user", onClick = { selectedRole = "user" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Gray))
                        Text("User (Khóa quyền)", color = textColor)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        RadioButton(selected = selectedRole == "delete", onClick = { selectedRole = "delete" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Red))
                        Text("Xóa tài khoản này", color = Color.Red, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editAdmin = null }) { Text("Hủy", color = Color.Gray) }
                        Button(
                            onClick = {
                                isSubmitting = true
                                coroutineScope.launch {
                                    if (SongScraper.updateAdminRole(editAdmin!!.id, selectedRole)) {
                                        editAdmin = null; loadData()
                                    } else Toast.makeText(context, "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                                    isSubmitting = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                        ) {
                            if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp)) else Text("Lưu", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun TicketChatView(
    ticket: SupportTicket,
    isAdmin: Boolean,
    onBack: () -> Unit,
    tunaGreen: Color,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    isDarkMode: Boolean,
    currentLang: String,
    showHeader: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val requesterEmail = com.google.android.gms.auth.api.signin.GoogleSignIn
        .getLastSignedInAccount(context)?.email.orEmpty()
    val localDeviceId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    ) ?: "unknown_device"

    // Chat dung tong mau don sac de tach khoi mau xanh chu dao cua app.
    // Bong tin nhan cua chinh nguoi gui dung xanh duong nhe, tu dieu chinh sang/toi.
    val chatNeutralAccent = if (isDarkMode) Color(0xFFD9DDE2) else Color(0xFF4A5158)
    val chatNeutralBorder = if (isDarkMode) Color(0xFF4A5057) else Color(0xFFC7CDD3)
    val chatActionBackground = if (isDarkMode) Color(0xFF343A40) else Color(0xFFE3E7EB)
    val chatActionForeground = if (isDarkMode) Color(0xFFF4F6F8) else Color(0xFF2E343A)
    val myMessageBackground = if (isDarkMode) Color(0xFF294D65) else Color(0xFFD8ECFF)
    val myMessageText = if (isDarkMode) Color(0xFFF4FAFF) else Color(0xFF17384E)
    val otherMessageBackground = if (isDarkMode) Color(0xFF262A2D) else Color(0xFFF0F2F5)

    var replyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var pendingImageUi by remember(ticket.id) { mutableStateOf<PendingSupportImageUi?>(null) }
    var adminReadState by remember(ticket.id) {
        mutableIntStateOf(TunerSupportApi.adminReadForTicket(ticket.id))
    }
    var isReplying by remember { mutableStateOf(false) }
    var isRefreshingTicket by remember(ticket.id) { mutableStateOf(false) }
    var refreshRequestedAgain by remember(ticket.id) { mutableStateOf(false) }

    var currentTicket by remember(ticket.id) { mutableStateOf(ticket) }
    LaunchedEffect(ticket) { currentTicket = ticket }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                replyText = if (replyText.isBlank()) spoken else "$replyText $spoken"
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói nội dung tin nhắn")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Thiết bị chưa có dịch vụ nhập bằng giọng nói.", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun refreshTicketFromServer(markRead: Boolean = true) {
        if (currentTicket.id.isBlank()) return
        if (isRefreshingTicket) {
            refreshRequestedAgain = true
            return
        }

        do {
            refreshRequestedAgain = false
            isRefreshingTicket = true
            try {
                val latest = TunerSupportApi.getTicketDetail(
                    ticketId = currentTicket.id,
                    requesterEmail = requesterEmail,
                    deviceId = localDeviceId
                )

                if (latest != null) {
                    var normalizedTicket = latest
                    adminReadState = TunerSupportApi.adminReadForTicket(latest.id)
                    val lastSender = latest.chatHistory.lastOrNull()?.sender.orEmpty()

                    // User mo tin nhan cuoi cua Admin -> Admin se thay "Da xem".
                    if (!isAdmin && markRead && lastSender == "admin" && latest.userRead == 0) {
                        val marked = TunerSupportApi.markUserRead(
                            ticketId = latest.id,
                            ticketType = latest.type,
                            requesterEmail = requesterEmail,
                            deviceId = localDeviceId
                        )
                        if (marked) normalizedTicket = latest.copy(userRead = 1)
                    }

                    // Admin mo tin nhan cuoi cua User -> User se thay "Da xem".
                    if (isAdmin && markRead && lastSender != "admin" && adminReadState == 0) {
                        val marked = TunerSupportApi.markAdminRead(
                            ticketId = latest.id,
                            ticketType = latest.type,
                            adminEmail = requesterEmail,
                            deviceId = latest.deviceId
                        )
                        if (marked) {
                            adminReadState = 1
                        } else {
                            android.util.Log.w(
                                "SupportReadReceipt",
                                "Admin read fallback failed for ticket=${latest.id}, email=$requesterEmail"
                            )
                        }
                    }

                    if (normalizedTicket != currentTicket) currentTicket = normalizedTicket
                }
            } finally {
                isRefreshingTicket = false
            }
        } while (refreshRequestedAgain)
    }

    LaunchedEffect(ticket.id) {
        refreshTicketFromServer(markRead = true)
    }

    DisposableEffect(ticket.id, ticket.deviceId, localDeviceId, isAdmin) {
        if (ticket.id.isBlank()) {
            return@DisposableEffect onDispose { }
        }

        // Thread co the duoc gop theo Gmail va device_id canonical co the la may cu.
        // User lang nghe ca device cua ticket va device hien tai de receipt "Da xem"
        // ve ngay lap tuc trong moi truong hop. Admin chi can lang nghe device cua user.
        val eventDeviceIds = if (isAdmin) {
            listOf(ticket.deviceId)
        } else {
            listOf(ticket.deviceId, localDeviceId)
        }.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        val registrations = eventDeviceIds.mapNotNull { eventDeviceId ->
            val eventRef = runCatching {
                FirebaseDatabase.getInstance()
                    .getReference("support_ticket_events")
                    .child(eventDeviceId)
                    .child(ticket.id)
            }.getOrNull() ?: return@mapNotNull null

            val eventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        coroutineScope.launch { refreshTicketFromServer(markRead = true) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e(
                        "SupportRealtime",
                        "Không thể lắng nghe chat ($eventDeviceId): ${error.message}"
                    )
                }
            }
            eventRef.addValueEventListener(eventListener)
            eventRef to eventListener
        }

        onDispose {
            registrations.forEach { (ref, listener) ->
                ref.removeEventListener(listener)
            }
        }
    }

    // Dự phòng nếu Firebase event bị chậm hoặc service account tạm lỗi.
    // 2 giây đủ nhanh để receipt gần như realtime nhưng vẫn nhẹ cho server.
    LaunchedEffect(ticket.id, isAdmin) {
        while (true) {
            kotlinx.coroutines.delay(2_000L)
            refreshTicketFromServer(markRead = true)
        }
    }

    val pendingRemotePayload = pendingImageUi?.uploadedPayload?.takeIf { it.isNotBlank() }
    val visibleChatHistory = remember(currentTicket.chatHistory, pendingRemotePayload) {
        if (pendingRemotePayload.isNullOrBlank()) {
            currentTicket.chatHistory
        } else {
            // Server/Realtime co the tra message anh ve truoc khi Coil prefetch xong.
            // An ban remote trung lap va tiep tuc hien anh local de UI khong chop/nhay nen.
            currentTicket.chatHistory.filterNot { it.text == pendingRemotePayload }
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(visibleChatHistory.size, pendingImageUi?.localId, pendingImageUi?.stageText) {
        val totalItems = visibleChatHistory.size + if (pendingImageUi != null) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceColor)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTicket.subject,
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isRefreshingTicket) "Đang đồng bộ..." else if (isAdmin) "ID: ${currentTicket.deviceId.take(8)}..." else "Trò chuyện trực tiếp với Admin",
                        color = if (isRefreshingTicket) Color.Gray else chatNeutralAccent,
                        fontSize = 10.sp
                    )
                }
            }
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.14f))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                count = visibleChatHistory.size,
                key = { index ->
                    val msg = visibleChatHistory[index]
                    "chat_${index}_${msg.timestamp}_${msg.text.hashCode()}"
                }
            ) { index ->
                val chatMsg = visibleChatHistory[index]
                val isFromAdmin = chatMsg.sender == "admin"
                val isMyMessage = if (isAdmin) isFromAdmin else !isFromAdmin
                val imagePayload = parseSupportImagePayload(chatMsg.text)
                val isImageMessage = imagePayload != null
                val imageUrl = imagePayload?.imageUrl.orEmpty()
                val thumbnailUrl = imagePayload?.thumbnailUrl.orEmpty()
                val imageCaption = imagePayload?.caption.orEmpty()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = if (isFromAdmin) "Admin" else if (isAdmin) CoreTranslator.getString("user", currentLang) else CoreTranslator.getString("you", currentLang),
                        color = if (isFromAdmin) chatNeutralAccent else Color.Gray,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 3.dp)
                    )

                    val msgBgColor = if (isMyMessage) myMessageBackground else otherMessageBackground
                    val msgTextColor = if (isMyMessage) myMessageText else textColor

                    if (isImageMessage && imageUrl.startsWith("http")) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(msgBgColor)
                                // Viền màu quanh ảnh chỉ còn 1dp để ảnh gọn hơn.
                                .padding(1.dp)
                        ) {
                            val thumbnailRequest = remember(thumbnailUrl) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(thumbnailUrl)
                                    .size(720, 720)
                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .crossfade(false)
                                    .build()
                            }
                            coil.compose.AsyncImage(
                                model = thumbnailRequest,
                                contentDescription = "Ảnh trong cuộc trò chuyện",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 110.dp, max = 170.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable { previewImageUrl = imageUrl },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            if (imageCaption.isNotBlank()) {
                                Text(
                                    text = imageCaption,
                                    color = msgTextColor,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 230.dp)
                                .background(msgBgColor, RoundedCornerShape(17.dp))
                                .padding(horizontal = 12.dp, vertical = 9.dp)
                        ) {
                            Text(chatMsg.text, color = msgTextColor, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }

                    if (chatMsg.timestamp.isNotEmpty()) {
                        Text(
                            chatMsg.timestamp,
                            color = Color.Gray.copy(alpha = 0.78f),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(top = 3.dp, start = 5.dp, end = 5.dp)
                        )
                    }

                    // Chi tin nhan DA GUI sau cung cua chinh minh moi hien "Da xem",
                    // giong Messenger/Zalo. Tin dang upload khong hien read receipt.
                    val isLastMessage = index == visibleChatHistory.lastIndex && pendingImageUi == null
                    val seenByOtherSide = if (isFromAdmin) {
                        currentTicket.userRead == 1
                    } else {
                        adminReadState == 1
                    }
                    if (isLastMessage && isMyMessage && seenByOtherSide) {
                        Text(
                            text = if (currentLang.lowercase().startsWith("vi")) "Đã xem" else "Seen",
                            color = Color.Gray.copy(alpha = 0.88f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp, start = 5.dp, end = 5.dp)
                        )
                    }
                }
            }

            pendingImageUi?.let { pending ->
                item(key = "pending_image_${pending.localId}") {
                    val isPendingFromAdmin = pending.senderRole == "admin"
                    val isMyPendingMessage = if (isAdmin) isPendingFromAdmin else !isPendingFromAdmin
                    val pendingBg = if (isMyPendingMessage) myMessageBackground else otherMessageBackground
                    val pendingTextColor = if (isMyPendingMessage) myMessageText else textColor

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMyPendingMessage) Alignment.End else Alignment.Start
                    ) {
                        Text(
                            text = if (isPendingFromAdmin) {
                                "Admin"
                            } else if (isAdmin) {
                                CoreTranslator.getString("user", currentLang)
                            } else {
                                CoreTranslator.getString("you", currentLang)
                            },
                            color = if (isPendingFromAdmin) chatNeutralAccent else Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 3.dp)
                        )

                        Column(
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(pendingBg)
                                .padding(1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 110.dp, max = 170.dp)
                                    .clip(RoundedCornerShape(11.dp))
                            ) {
                                coil.compose.AsyncImage(
                                    model = pending.imageUri,
                                    contentDescription = "Ảnh đang gửi",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )

                                if (pending.failed || pending.stageText.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = if (pending.failed) 0.42f else 0.28f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (pending.failed) {
                                                Icon(
                                                    imageVector = Icons.Default.ErrorOutline,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.2.dp
                                                )
                                            }
                                            Spacer(Modifier.height(7.dp))
                                            Text(
                                                text = pending.stageText,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 2,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (pending.caption.isNotBlank()) {
                                Text(
                                    text = pending.caption,
                                    color = pendingTextColor,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (pending.stageText.isNotBlank()) {
                            Text(
                                text = pending.stageText,
                                color = if (pending.failed) Color(0xFFE57373) else Color.Gray.copy(alpha = 0.85f),
                                fontSize = 8.sp,
                                modifier = Modifier.padding(top = 3.dp, start = 5.dp, end = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Ảnh vừa chọn, chưa gửi.
        selectedImageUri?.let { uri ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 4.dp),
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, chatNeutralBorder.copy(alpha = 0.80f))
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil.compose.AsyncImage(
                        model = uri,
                        contentDescription = "Ảnh đã chọn",
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ảnh đính kèm", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Có thể nhập thêm chú thích rồi gửi", color = Color.Gray, fontSize = 9.sp)
                    }
                    IconButton(onClick = { selectedImageUri = null }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Bỏ ảnh", tint = Color.Gray, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isReplying,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Gửi hình ảnh",
                        tint = chatNeutralAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Nhập tin nhắn...", color = Color.Gray, fontSize = 12.sp) },
                    textStyle = TextStyle(color = textColor, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(22.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = bgColor,
                        unfocusedContainerColor = bgColor,
                        focusedIndicatorColor = chatNeutralAccent.copy(alpha = 0.72f),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.35f),
                        cursorColor = chatNeutralAccent
                    )
                )

                IconButton(
                    onClick = { startVoiceInput() },
                    enabled = !isReplying,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Nhập bằng giọng nói",
                        tint = chatNeutralAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(3.dp))
                val canSend = replyText.isNotBlank() || selectedImageUri != null
                IconButton(
                    onClick = {
                        if (!canSend || isReplying) return@IconButton
                        val captionToSend = replyText.trim()
                        val imageToSend = selectedImageUri
                        val senderRole = if (isAdmin) "admin" else "user"
                        val pendingId = "${System.currentTimeMillis()}_${System.nanoTime()}"

                        // Optimistic UI: anh xuat hien ngay trong khung chat sau khi bam Gui.
                        // Trong luc nen/upload, preview local duoc phu loader thay vi de nguoi dung cho man hinh trong.
                        if (imageToSend != null) {
                            pendingImageUi = PendingSupportImageUi(
                                localId = pendingId,
                                imageUri = imageToSend,
                                caption = captionToSend,
                                senderRole = senderRole,
                                stageText = "Đang nén và tải ảnh..."
                            )
                            replyText = ""
                            selectedImageUri = null
                        }

                        isReplying = true

                        coroutineScope.launch {
                            var payloadText = captionToSend

                            if (imageToSend != null) {
                                val uploadedImage = TunerSupportApi.uploadSupportImage(
                                    context = context,
                                    ticketId = currentTicket.id,
                                    requesterEmail = requesterEmail,
                                    deviceId = localDeviceId,
                                    imageUri = imageToSend
                                )
                                if (uploadedImage == null) {
                                    pendingImageUi = pendingImageUi
                                        ?.takeIf { it.localId == pendingId }
                                        ?.copy(
                                            stageText = "Gửi ảnh thất bại",
                                            failed = true
                                        )
                                    isReplying = false
                                    return@launch
                                }

                                payloadText = buildSupportImagePayload(
                                    uploaded = uploadedImage,
                                    caption = captionToSend
                                )

                                pendingImageUi = pendingImageUi
                                    ?.takeIf { it.localId == pendingId }
                                    ?.copy(
                                        stageText = "Đang hoàn tất...",
                                        failed = false,
                                        uploadedPayload = payloadText
                                    )
                            }

                            val success = TunerSupportApi.replyTicket(
                                ticketId = currentTicket.id,
                                ticketType = currentTicket.type,
                                requesterEmail = requesterEmail,
                                deviceId = localDeviceId,
                                replyText = payloadText
                            )

                            if (success) {
                                val alreadySynced = currentTicket.chatHistory.lastOrNull()?.text == payloadText
                                if (!alreadySynced) {
                                    val newMsg = ChatMessage(
                                        senderRole,
                                        payloadText,
                                        CoreTranslator.getString("just_now", currentLang)
                                    )
                                    currentTicket = currentTicket.copy(
                                        chatHistory = currentTicket.chatHistory + newMsg,
                                        status = if (isAdmin) 1 else 0,
                                        userRead = if (isAdmin) 0 else 1
                                    )
                                    adminReadState = if (isAdmin) 1 else 0
                                }

                                if (imageToSend != null) {
                                    // Server da ghi nhan xong: bo lop loading NHUNG van giu anh local sach.
                                    // Chi doi sang URL server sau khi thumbnail remote da vao memory/disk cache.
                                    pendingImageUi = pendingImageUi
                                        ?.takeIf { it.localId == pendingId }
                                        ?.copy(stageText = "", failed = false, uploadedPayload = payloadText)

                                    val remote = parseSupportImagePayload(payloadText)
                                    if (remote != null) {
                                        val prefetchRequest = coil.request.ImageRequest.Builder(context)
                                            .data(remote.thumbnailUrl)
                                            .size(720, 720)
                                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                                            .crossfade(false)
                                            .build()

                                        var cached = false
                                        repeat(3) { attempt ->
                                            val result = runCatching {
                                                coil.Coil.imageLoader(context).execute(prefetchRequest)
                                            }.getOrNull()
                                            if (result is coil.request.SuccessResult) {
                                                cached = true
                                                return@repeat
                                            }
                                            if (attempt < 2) kotlinx.coroutines.delay(350L)
                                        }

                                        // Neu cache thanh cong, viec swap local -> remote gan nhu khong the nhin thay.
                                        // Neu mang yeu, giu local them mot chut de AsyncImage co thoi gian nap nen.
                                        if (!cached) kotlinx.coroutines.delay(1200L)
                                    }

                                    kotlinx.coroutines.delay(80L)
                                    if (pendingImageUi?.localId == pendingId) {
                                        pendingImageUi = null
                                    }
                                } else {
                                    replyText = ""
                                }
                            } else {
                                if (imageToSend != null) {
                                    pendingImageUi = pendingImageUi
                                        ?.takeIf { it.localId == pendingId }
                                        ?.copy(
                                            stageText = "Gửi ảnh thất bại",
                                            failed = true
                                        )
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Không gửi được tin nhắn. Vui lòng thử lại.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            isReplying = false
                        }
                    },
                    enabled = canSend && !isReplying,
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (canSend) chatActionBackground else Color.Gray.copy(alpha = 0.18f), CircleShape)
                ) {
                    if (isReplying) {
                        CircularProgressIndicator(color = chatActionForeground, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Gửi", tint = chatActionForeground, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }

    previewImageUrl?.let { imageUrl ->
        Dialog(
            onDismissRequest = { previewImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                val fullImageRequest = remember(imageUrl) {
                    coil.request.ImageRequest.Builder(context)
                        .data(imageUrl)
                        .size(1600, 1600)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .crossfade(false)
                        .build()
                }
                coil.compose.AsyncImage(
                    model = fullImageRequest,
                    contentDescription = "Xem ảnh",
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                IconButton(
                    onClick = { previewImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng ảnh", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun CreateTicketView(
    deviceId: String,
    requesterEmail: String,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    tunaGreen: Color,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    currentLang: String
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spoken.isNotBlank()) {
                message = if (message.isBlank()) spoken else "$message $spoken"
            }
        }
    }

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toLanguageTag())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (currentLang == "vi") "Nói nội dung cần hỗ trợ" else "Speak your support message"
            )
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                if (currentLang == "vi") "Thiết bị chưa có dịch vụ nhập bằng giọng nói." else "Voice input is unavailable on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val canSend = subject.isNotBlank() && (message.isNotBlank() || selectedImageUri != null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Header đồng bộ với giao diện chat, nằm an toàn dưới camera/tai thỏ nhờ parent SupportCenterDialog.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = !isSubmitting,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }

                Spacer(Modifier.width(5.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        CoreTranslator.getString("send_new_support", currentLang),
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentLang == "vi") {
                            "Nội dung sẽ được nối vào lịch sử trò chuyện hiện tại"
                        } else {
                            "New messages are appended to your existing conversation"
                        },
                        color = tunaGreen,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text(CoreTranslator.getString("support_subject", currentLang), color = Color.Gray) },
                placeholder = {
                    Text(
                        if (currentLang == "vi") "Ví dụ: Không mở được bản nhạc" else "Example: Cannot open a song",
                        color = Color.Gray.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting,
                textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = surfaceColor,
                    unfocusedContainerColor = surfaceColor,
                    disabledContainerColor = surfaceColor,
                    focusedIndicatorColor = tunaGreen,
                    unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.30f),
                    cursorColor = tunaGreen
                )
            )

            Spacer(Modifier.height(12.dp))

            // Vùng giữa giống phần nội dung của chat thay vì một TextField lớn chiếm toàn màn hình.
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = surfaceColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = tunaGreen.copy(alpha = 0.90f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = if (currentLang == "vi") "Gửi tin nhắn cho Admin" else "Message Admin support",
                            color = textColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (currentLang == "vi") {
                                "Bạn có thể nhập nội dung, nói bằng giọng nói hoặc đính kèm hình ảnh ở thanh bên dưới."
                            } else {
                                "Type a message, use voice input, or attach an image below."
                            },
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Ảnh vừa chọn hiển thị giống preview trong cửa sổ chat.
        selectedImageUri?.let { uri ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = surfaceColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, tunaGreen.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    coil.compose.AsyncImage(
                        model = uri,
                        contentDescription = "Ảnh đính kèm",
                        modifier = Modifier
                            .size(58.dp)
                            .clip(RoundedCornerShape(9.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (currentLang == "vi") "Ảnh đính kèm" else "Attached image",
                            color = textColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (currentLang == "vi") "Ảnh sẽ được gửi vào cùng cuộc trò chuyện" else "The image will be sent to the same conversation",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                    }
                    IconButton(
                        onClick = { selectedImageUri = null },
                        enabled = !isSubmitting,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Bỏ ảnh", tint = Color.Gray, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }

        // Thanh nhập tin nhắn theo cùng bố cục với chat đang dùng.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isSubmitting,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.AddPhotoAlternate,
                        contentDescription = "Chọn hình ảnh",
                        tint = tunaGreen,
                        modifier = Modifier.size(21.dp)
                    )
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = {
                        Text(
                            if (currentLang == "vi") "Nhập nội dung cần hỗ trợ..." else "Type your support message...",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    textStyle = TextStyle(color = textColor, fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 4,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(22.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = bgColor,
                        unfocusedContainerColor = bgColor,
                        disabledContainerColor = bgColor,
                        focusedIndicatorColor = tunaGreen.copy(alpha = 0.75f),
                        unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.35f),
                        cursorColor = tunaGreen
                    )
                )

                IconButton(
                    onClick = { startVoiceInput() },
                    enabled = !isSubmitting,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Nhập bằng giọng nói",
                        tint = tunaGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(2.dp))

                IconButton(
                    onClick = {
                        if (!canSend || isSubmitting) return@IconButton

                        val subjectToSend = subject.trim()
                        val messageToSend = message.trim().ifBlank {
                            if (currentLang == "vi") "Đã gửi một hình ảnh." else "Sent an image."
                        }
                        val imageToSend = selectedImageUri
                        isSubmitting = true

                        coroutineScope.launch {
                            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                                .getLastSignedInAccount(context)
                            val effectiveEmail = requesterEmail.ifBlank { account?.email.orEmpty() }

                            val result = TunerSupportApi.submitUserSupport(
                                deviceId = deviceId,
                                requesterEmail = effectiveEmail,
                                requesterName = account?.displayName.orEmpty(),
                                subject = subjectToSend,
                                message = messageToSend
                            )

                            if (!result.success) {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                isSubmitting = false
                                return@launch
                            }

                            var imageSent = true
                            if (imageToSend != null && result.ticketId.isNotBlank()) {
                                val uploadedImage = TunerSupportApi.uploadSupportImage(
                                    context = context,
                                    ticketId = result.ticketId,
                                    requesterEmail = effectiveEmail,
                                    deviceId = deviceId,
                                    imageUri = imageToSend
                                )

                                imageSent = if (uploadedImage != null) {
                                    TunerSupportApi.replyTicket(
                                        ticketId = result.ticketId,
                                        ticketType = "support",
                                        requesterEmail = effectiveEmail,
                                        deviceId = deviceId,
                                        replyText = buildSupportImagePayload(uploadedImage)
                                    )
                                } else {
                                    false
                                }
                            }

                            val toastText = when {
                                !imageSent -> if (currentLang == "vi") {
                                    "Nội dung đã gửi nhưng hình ảnh chưa gửi được."
                                } else {
                                    "Message sent, but the image could not be uploaded."
                                }
                                result.reusedConversation -> if (currentLang == "vi") {
                                    "Đã gửi vào cuộc trò chuyện hiện tại."
                                } else {
                                    "Sent to the existing conversation."
                                }
                                else -> CoreTranslator.getString("send_success", currentLang)
                            }
                            Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()

                            subject = ""
                            message = ""
                            selectedImageUri = null
                            isSubmitting = false

                            if (result.ticketId.isNotBlank()) {
                                onCreated(result.ticketId)
                            } else {
                                onBack()
                            }
                        }
                    },
                    enabled = canSend && !isSubmitting,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (canSend) tunaGreen else Color.Gray.copy(alpha = 0.25f),
                            CircleShape
                        )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = CoreTranslator.getString("send", currentLang),
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun PendingSongsView(
    tunaGreen: Color, surfaceColor: Color, bgColor: Color, textColor: Color,
    currentLang: String, deviceId: String
) {
    var pendingList by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- STATE CHO CHỨC NĂNG XEM TRƯỚC ---
    var previewSongData by remember { mutableStateOf<SongDetails?>(null) }
    var isFetchingPreview by remember { mutableStateOf(false) }
    var previewUrl by remember { mutableStateOf("") } // Lưu URL của bài hát đang xem

    fun loadData() {
        isLoading = true
        coroutineScope.launch {
            pendingList = SongScraper.getPendingSongs()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // XỬ LÝ GIAO DIỆN HIỂN THỊ
    if (isFetchingPreview) {
        // Đang tải nội dung chi tiết
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = tunaGreen)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Đang tải bản xem trước...", color = Color.Gray)
            }
        }
    } else if (previewSongData != null) {
        // HIỂN THỊ MÀN HÌNH CHI TIẾT BÀI HÁT (XEM TRƯỚC)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header Quay lại
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                IconButton(onClick = { previewSongData = null; previewUrl = "" }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chi tiết bài hát", color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Nội dung bài hát có thể cuộn
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).background(surfaceColor, RoundedCornerShape(8.dp)).padding(16.dp)) {
                Text(previewSongData!!.title, color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Ca sĩ: ${previewSongData!!.singer} • Sáng tác: ${previewSongData!!.composer}", color = Color.Gray, fontSize = 14.sp)
                Text("Tone: ${previewSongData!!.baseTone} • Điệu: ${if (previewSongData!!.rhythm.isNotBlank()) previewSongData!!.rhythm else "Chưa có"}", color = tunaGreen, fontSize = 14.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.3f))

                if (previewSongData!!.imageCount > 0) {
                    // Hiển thị dạng Sheet ảnh
                    val baseUrl = "https://raw.githubusercontent.com/ducphi0491/sheet_images/main/"
                    for (i in 1..previewSongData!!.imageCount) {
                        val imageUrl = "$baseUrl${previewSongData!!.slug}-$i.jpg"
                        coil.compose.AsyncImage(
                            model = imageUrl,
                            contentDescription = "Trang $i",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
                        )
                    }
                } else {
                    // Hiển thị dạng Text Hợp âm
                    ChordTextRenderer(
                        lyrics = previewSongData!!.lyrics,
                        chordColor = tunaGreen,
                        textColor = textColor,
                        offset = 0,
                        fontSize = 14,
                        isChordOverText = false,
                        showRomanNumerals = false,
                        baseTone = previewSongData!!.baseTone,
                        onChordClick = {} // Không cần bấm hợp âm trong chế độ xem trước
                    )
                }
            }

            // Nút thao tác dưới cùng (Xóa / Duyệt)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                var showDeleteConfirm by remember { mutableStateOf(false) }

                // Nút Xóa
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.weight(1f).padding(end = 8.dp).height(48.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Từ chối")
                    Spacer(Modifier.width(8.dp))
                    Text("Từ chối", fontWeight = FontWeight.Bold)
                }

                // Nút Duyệt
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (SongScraper.approveSong(previewUrl)) {
                                Toast.makeText(context, "Đã duyệt thành công!", Toast.LENGTH_SHORT).show()
                                previewSongData = null
                                loadData()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = tunaGreen),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).height(48.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Duyệt")
                    Spacer(Modifier.width(8.dp))
                    Text("Duyệt bài", fontWeight = FontWeight.Bold)
                }

                // Hộp thoại xác nhận xóa
                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Từ chối bài hát", color = tunaGreen, fontWeight = FontWeight.Bold) },
                        text = { Text("Bạn có chắc muốn xóa bỏ bài hát này không?", color = textColor) },
                        containerColor = surfaceColor,
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteConfirm = false
                                coroutineScope.launch {
                                    if (SongScraper.deleteSong(previewUrl)) {
                                        Toast.makeText(context, "Đã xóa bài hát", Toast.LENGTH_SHORT).show()
                                        previewSongData = null
                                        loadData()
                                    }
                                }
                            }) { Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy", color = Color.Gray) } }
                    )
                }
            }
        }
    } else if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
    } else if (pendingList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không có bài hát nào cần duyệt", color = Color.Gray) }
    } else {
        // DANH SÁCH BÀI HÁT CHỜ DUYỆT (Mặc định)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
            items(pendingList) { song ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).clickable {
                        // KHI BẤM VÀO SẼ GỌI API LẤY CHI TIẾT
                        previewUrl = song.url
                        isFetchingPreview = true
                        coroutineScope.launch {
                            previewSongData = SongScraper.fetchSongDetails(song.url, deviceId)
                            isFetchingPreview = false
                        }
                    }) {
                        Text(song.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${song.singer} • ${song.composer}", color = Color.Gray, fontSize = 12.sp)
                        Text(if (song.imageCount > 0) "[Sheet Nhạc]" else "[Hợp âm]", color = tunaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = {
                        coroutineScope.launch {
                            if (SongScraper.approveSong(song.url)) {
                                Toast.makeText(context, "Đã duyệt thành công!", Toast.LENGTH_SHORT).show()
                                loadData()
                            }
                        }
                    }) { Icon(Icons.Default.CheckCircle, "Duyệt", tint = tunaGreen) }

                    var showDeleteConfirm by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, "Từ chối", tint = Color.Red) }
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Từ chối bài hát", color = tunaGreen, fontWeight = FontWeight.Bold) },
                            text = { Text("Bạn có chắc muốn xóa bỏ bài '${song.title}' này không?", color = textColor) },
                            containerColor = surfaceColor,
                            confirmButton = {
                                TextButton(onClick = {
                                    showDeleteConfirm = false
                                    coroutineScope.launch {
                                        if (SongScraper.deleteSong(song.url)) {
                                            Toast.makeText(context, "Đã xóa bài hát", Toast.LENGTH_SHORT).show()
                                            loadData()
                                        }
                                    }
                                }) { Text("Xóa", color = Color.Red, fontWeight = FontWeight.Bold) }
                            },
                            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy", color = Color.Gray) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}


@Composable
fun DailyStatisticsView(tunaGreen: Color, surfaceColor: Color, bgColor: Color, textColor: Color) {
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }

    var selectedTabIndex by remember { mutableStateOf(0) }
    // Gộp số người đang online trực tiếp vào tab Theo Ngày để quản trị viên
    // vừa xem thống kê sử dụng, vừa thấy trạng thái online hiện tại trên cùng màn hình.
    val tabs = listOf("Theo Ngày", "Theo Tháng", "So Sánh Tháng", "Người Mới", "Không hoạt động")

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Thu gon khoang trong phia tren va hai ben cua nhom tab thong ke.
            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 4.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.height(40.dp),
            containerColor = Color.Transparent,
            contentColor = tunaGreen,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = tunaGreen
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.height(40.dp),
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTabIndex == index) tunaGreen else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        when (selectedTabIndex) {
            0 -> DailyStatsContent(db, tunaGreen, surfaceColor, bgColor, textColor)
            1 -> MonthlyStatsContent(db, tunaGreen, surfaceColor, textColor, bgColor)
            2 -> MonthlyComparisonStatsContent(db, tunaGreen, surfaceColor, textColor)
            3 -> NewUsersStatsContent(db, tunaGreen, surfaceColor, textColor)
            4 -> InactiveDevicesStatsContent(tunaGreen, surfaceColor, bgColor, textColor)
        }
    }
}

private data class RealtimePresenceEntry(
    val firebaseUid: String,
    val deviceId: String,
    val email: String,
    val displayName: String,
    val appVersion: String,
    val deviceName: String,
    val androidVersion: String,
    val lastChanged: Long,
    val online: Boolean
)

private data class OnlineUserDisplay(
    val stableKey: String,
    val firebaseUid: String,
    val deviceId: String,
    val email: String,
    val displayName: String,
    val appVersion: String,
    val deviceName: String,
    val androidVersion: String,
    val lastChanged: Long,
    val deviceCount: Int
)

private data class PresenceDeviceDisplay(
    val stableKey: String,
    val firebaseUid: String,
    val deviceId: String,
    val email: String,
    val displayName: String,
    val appVersion: String,
    val deviceName: String,
    val androidVersion: String,
    val lastChanged: Long,
    val isOnline: Boolean
)

private data class OnlinePresenceUiState(
    val users: List<OnlineUserDisplay> = emptyList(),
    val allDevices: List<PresenceDeviceDisplay> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String = ""
)

/**
 * Theo dõi danh sách người dùng online hiện tại từ Firebase Realtime Database.
 * Một email đăng nhập trên nhiều thiết bị vẫn chỉ được tính là một người dùng.
 * Thiết bị khách được nhận diện bằng Android ID đã ghi trong trường device_id.
 */
@Composable
private fun rememberOnlinePresenceUiState(): OnlinePresenceUiState {
    var rawPresence by remember { mutableStateOf<List<RealtimePresenceEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Làm mới đồng hồ định kỳ để tự loại node đã quá hạn heartbeat.
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(15_000L)
        }
    }

    // Nếu Firebase Anonymous Auth chưa bật, tránh để UI loading vô hạn.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(8_000L)
        if (isLoading && FirebaseAuth.getInstance().currentUser == null) {
            errorMessage = "Chưa đăng nhập được Firebase. Hãy bật Anonymous Authentication."
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val presenceRoot = try {
            FirebaseDatabase.getInstance().getReference("presence")
        } catch (_: Exception) {
            errorMessage = "Chưa cấu hình Realtime Database."
            isLoading = false
            null
        }

        var databaseListener: ValueEventListener? = null

        fun stopDatabaseListener() {
            databaseListener?.let { listener ->
                presenceRoot?.removeEventListener(listener)
            }
            databaseListener = null
        }

        fun startDatabaseListener() {
            if (presenceRoot == null || databaseListener != null) return

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    rawPresence = snapshot.children.mapNotNull { child ->
                        val online = child.child("online").getValue(Boolean::class.java) ?: false
                        val email = child.child("email").getValue(String::class.java) ?: "guest"
                        val displayName = child.child("display_name").getValue(String::class.java) ?: ""
                        val deviceId = child.child("device_id").getValue(String::class.java)
                            ?: child.key.orEmpty()
                        val appVersion = child.child("app_version").getValue(String::class.java) ?: ""
                        val manufacturer = child.child("device_manufacturer")
                            .getValue(String::class.java).orEmpty()
                        val brand = child.child("device_brand")
                            .getValue(String::class.java).orEmpty()
                        val model = child.child("device_model")
                            .getValue(String::class.java).orEmpty()
                        val deviceName = child.child("device_name")
                            .getValue(String::class.java)
                            .orEmpty()
                            .ifBlank { buildDeviceDisplayName(manufacturer, brand, model) }
                        val androidVersion = child.child("android_version")
                            .getValue(String::class.java).orEmpty()
                        val lastChanged = child.child("last_changed").getValue(Long::class.java) ?: 0L

                        RealtimePresenceEntry(
                            firebaseUid = child.key.orEmpty(),
                            deviceId = deviceId,
                            email = email,
                            displayName = displayName,
                            appVersion = appVersion,
                            deviceName = deviceName,
                            androidVersion = androidVersion,
                            lastChanged = lastChanged,
                            online = online
                        )
                    }
                    errorMessage = ""
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    errorMessage = "Không thể đọc trạng thái online: ${error.message}"
                    isLoading = false
                }
            }

            databaseListener = listener
            presenceRoot.addValueEventListener(listener)
        }

        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                startDatabaseListener()
            } else {
                stopDatabaseListener()
                rawPresence = emptyList()
                isLoading = true
            }
        }

        auth.addAuthStateListener(authListener)
        if (auth.currentUser != null) startDatabaseListener()

        onDispose {
            auth.removeAuthStateListener(authListener)
            stopDatabaseListener()
        }
    }

    val onlineUsers = remember(rawPresence, currentTime) {
        rawPresence
            .filter { entry ->
                entry.online &&
                        entry.lastChanged > 0L &&
                        currentTime - entry.lastChanged <= 90_000L
            }
            .groupBy { entry ->
                if (entry.email.isNotBlank() && entry.email != "guest") {
                    entry.email.trim().lowercase()
                } else {
                    entry.deviceId.ifBlank { entry.firebaseUid }
                }
            }
            .map { (stableKey, records) ->
                val latest = records.maxByOrNull { it.lastChanged } ?: records.first()
                val fallbackGuest = latest.deviceId
                    .ifBlank { latest.firebaseUid }
                    .take(6)
                    .uppercase()
                val displayName = latest.displayName.ifBlank {
                    if (latest.email != "guest") latest.email.substringBefore("@")
                    else "Khách ($fallbackGuest)"
                }

                val latestDeviceRecord = records
                    .filter { it.deviceName.isNotBlank() }
                    .maxByOrNull { it.lastChanged }
                    ?: latest

                OnlineUserDisplay(
                    stableKey = stableKey,
                    firebaseUid = latest.firebaseUid,
                    deviceId = latest.deviceId,
                    email = latest.email,
                    displayName = displayName,
                    appVersion = latest.appVersion,
                    deviceName = latestDeviceRecord.deviceName,
                    androidVersion = latestDeviceRecord.androidVersion,
                    lastChanged = latest.lastChanged,
                    deviceCount = records.size
                )
            }
            .sortedByDescending { it.lastChanged }
    }

    // Giữ lại cả thiết bị online lẫn offline để thống kê số ngày không sử dụng.
    // Gộp theo device_id nhằm tránh một thiết bị xuất hiện nhiều lần khi Firebase UID thay đổi.
    val allDevices = remember(rawPresence, currentTime) {
        rawPresence
            .filter { it.deviceId.isNotBlank() || it.firebaseUid.isNotBlank() }
            .groupBy { entry ->
                entry.deviceId.trim().ifBlank { entry.firebaseUid.trim() }
            }
            .map { (stableKey, records) ->
                val latest = records.maxByOrNull { it.lastChanged } ?: records.first()
                val latestIdentity = records
                    .filter { it.email.isNotBlank() && !it.email.equals("guest", ignoreCase = true) }
                    .maxByOrNull { it.lastChanged }
                    ?: latest
                val latestDisplayName = records
                    .filter { it.displayName.isNotBlank() }
                    .maxByOrNull { it.lastChanged }
                    ?.displayName
                    .orEmpty()
                val latestDevice = records
                    .filter { it.deviceName.isNotBlank() }
                    .maxByOrNull { it.lastChanged }
                    ?: latest
                val latestVersion = records
                    .filter { it.appVersion.isNotBlank() }
                    .maxByOrNull { it.lastChanged }
                    ?.appVersion
                    .orEmpty()
                val currentlyOnline = records.any { entry ->
                    entry.online &&
                            entry.lastChanged > 0L &&
                            currentTime - entry.lastChanged <= 90_000L
                }

                PresenceDeviceDisplay(
                    stableKey = stableKey,
                    firebaseUid = latest.firebaseUid,
                    deviceId = latest.deviceId.ifBlank { stableKey },
                    email = latestIdentity.email,
                    displayName = latestDisplayName,
                    appVersion = latestVersion,
                    deviceName = latestDevice.deviceName,
                    androidVersion = latestDevice.androidVersion,
                    lastChanged = latest.lastChanged,
                    isOnline = currentlyOnline
                )
            }
            .sortedByDescending { it.lastChanged }
    }

    return OnlinePresenceUiState(
        users = onlineUsers,
        allDevices = allDevices,
        isLoading = isLoading,
        errorMessage = errorMessage
    )
}

// =========================================================================
// TAB: THIẾT BỊ / GMAIL KHÔNG SỬ DỤNG ỨNG DỤNG TRONG NHIỀU NGÀY
// Lịch sử lấy từ Firestore Users/Leaderboard_Monthly; Realtime presence bổ sung trạng thái online.
// =========================================================================
@Composable
private fun InactiveDevicesStatsContent(
    tunaGreen: Color,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color
) {
    val presenceState = rememberOnlinePresenceUiState()
    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    var firestoreDevices by remember { mutableStateOf<List<PresenceDeviceDisplay>>(emptyList()) }
    var isHistoryLoading by remember { mutableStateOf(true) }
    var historyError by remember { mutableStateOf("") }
    var primaryUsersLoaded by remember { mutableStateOf(false) }
    var monthlyFallbackAttempted by remember { mutableStateOf(false) }

    fun parseHistoryDocument(
        doc: com.google.firebase.firestore.DocumentSnapshot
    ): PresenceDeviceDisplay? {
        val deviceId = doc.getString("device_id").orEmpty().ifBlank { doc.id }
        val firebaseUid = doc.getString("firebase_uid").orEmpty()
        val email = doc.getString("email").orEmpty().ifBlank { "guest" }
        val displayName = doc.getString("display_name").orEmpty()
        val manufacturer = doc.getString("device_manufacturer").orEmpty()
        val brand = doc.getString("device_brand").orEmpty()
        val model = doc.getString("device_model").orEmpty()
        val deviceName = doc.getString("device_name").orEmpty()
            .ifBlank { buildDeviceDisplayName(manufacturer, brand, model) }
        val lastAccess = doc.getLong("last_access_time")
            ?: doc.getTimestamp("last_access_time")?.toDate()?.time
            ?: 0L
        if (deviceId.isBlank() || lastAccess <= 0L) return null

        return PresenceDeviceDisplay(
            stableKey = deviceId,
            firebaseUid = firebaseUid,
            deviceId = deviceId,
            email = email,
            displayName = displayName,
            appVersion = doc.getString("app_version").orEmpty(),
            deviceName = deviceName,
            androidVersion = doc.getString("android_version").orEmpty(),
            lastChanged = lastAccess,
            isOnline = false
        )
    }

    // Nguồn chính: collection Users lưu một hồ sơ bền vững cho mỗi device_id.
    // Node presence chỉ phục vụ online/heartbeat và có thể không còn các thiết bị cũ.
    DisposableEffect(firestore) {
        val registration = firestore.collection("Users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    historyError = "Không thể đọc collection Users: ${error.message}"
                    primaryUsersLoaded = true
                    return@addSnapshotListener
                }

                firestoreDevices = snapshot?.documents.orEmpty()
                    .mapNotNull(::parseHistoryDocument)
                primaryUsersLoaded = true
                if (firestoreDevices.isNotEmpty()) {
                    historyError = ""
                    isHistoryLoading = false
                }
            }

        onDispose { registration.remove() }
    }

    // Tương thích dữ liệu cũ: nếu collection Users chưa có bản ghi (APK cũ),
    // đọc 24 tháng Leaderboard_Monthly và lấy last_access_time mới nhất của từng thiết bị.
    // Vì đây chỉ là fallback nên không phát sinh 24 truy vấn khi Users đã có dữ liệu.
    LaunchedEffect(primaryUsersLoaded, firestoreDevices.isEmpty()) {
        if (!primaryUsersLoaded || firestoreDevices.isNotEmpty() || monthlyFallbackAttempted) {
            return@LaunchedEffect
        }

        monthlyFallbackAttempted = true
        isHistoryLoading = true
        try {
            val monthFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.ROOT)
            val monthKeys = buildList {
                val calendar = java.util.Calendar.getInstance()
                repeat(24) {
                    add(monthFormat.format(calendar.time))
                    calendar.add(java.util.Calendar.MONTH, -1)
                }
            }

            val monthlyDocuments = kotlinx.coroutines.coroutineScope {
                monthKeys.map { monthKey ->
                    async(Dispatchers.IO) {
                        runCatching {
                            firestore.collection("Leaderboard_Monthly")
                                .document(monthKey)
                                .collection("Users")
                                .get()
                                .await()
                                .documents
                        }.getOrDefault(emptyList())
                    }
                }.awaitAll().flatten()
            }

            firestoreDevices = monthlyDocuments
                .mapNotNull(::parseHistoryDocument)
                .groupBy { it.deviceId.ifBlank { it.stableKey } }
                .mapNotNull { (_, records) -> records.maxByOrNull { it.lastChanged } }

            if (firestoreDevices.isNotEmpty()) {
                historyError = ""
            }
        } catch (error: Exception) {
            if (historyError.isBlank()) {
                historyError = "Không thể đọc lịch sử thiết bị: ${error.message.orEmpty()}"
            }
        } finally {
            isHistoryLoading = false
        }
    }

    // Hợp nhất Firestore Users (lịch sử lâu dài) với Realtime presence
    // (trạng thái online và heartbeat mới nhất). Thiết bị chỉ tồn tại ở một nguồn
    // vẫn được giữ lại để Admin không bị mất dữ liệu.
    val allKnownDevices = remember(firestoreDevices, presenceState.allDevices) {
        val merged = linkedMapOf<String, PresenceDeviceDisplay>()
        firestoreDevices.forEach { device -> merged[device.deviceId.ifBlank { device.stableKey }] = device }
        presenceState.allDevices.forEach { realtime ->
            val key = realtime.deviceId.ifBlank { realtime.stableKey }
            val old = merged[key]
            merged[key] = if (old == null) {
                realtime
            } else {
                val newestIsRealtime = realtime.lastChanged >= old.lastChanged
                old.copy(
                    firebaseUid = if (realtime.firebaseUid.isNotBlank()) realtime.firebaseUid else old.firebaseUid,
                    email = if (newestIsRealtime && realtime.email.isNotBlank()) realtime.email else old.email,
                    displayName = if (newestIsRealtime && realtime.displayName.isNotBlank()) realtime.displayName else old.displayName,
                    appVersion = realtime.appVersion.ifBlank { old.appVersion },
                    deviceName = realtime.deviceName.ifBlank { old.deviceName },
                    androidVersion = realtime.androidVersion.ifBlank { old.androidVersion },
                    lastChanged = maxOf(old.lastChanged, realtime.lastChanged),
                    isOnline = realtime.isOnline
                )
            }
        }
        merged.values.toList()
    }

    var inactiveFromDays by rememberSaveable { mutableIntStateOf(7) }
    var searchText by rememberSaveable { mutableStateOf("") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val dayMillis = 86_400_000L

    // Tự cập nhật số ngày ngay cả khi Firebase chưa phát sinh bản ghi mới.
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }

    fun inactiveDays(lastChanged: Long): Long {
        if (lastChanged <= 0L) return 0L
        return ((nowMillis - lastChanged).coerceAtLeast(0L) / dayMillis)
    }

    val inactiveDevices = remember(
        allKnownDevices,
        inactiveFromDays,
        searchText,
        nowMillis
    ) {
        val query = searchText.trim().lowercase(java.util.Locale.ROOT)
        allKnownDevices
            .asSequence()
            .filter { device ->
                device.lastChanged > 0L &&
                        !device.isOnline &&
                        inactiveDays(device.lastChanged) >= inactiveFromDays.toLong()
            }
            .filter { device ->
                query.isBlank() ||
                        device.email.lowercase(java.util.Locale.ROOT).contains(query) ||
                        device.displayName.lowercase(java.util.Locale.ROOT).contains(query) ||
                        device.deviceName.lowercase(java.util.Locale.ROOT).contains(query) ||
                        device.deviceId.lowercase(java.util.Locale.ROOT).contains(query)
            }
            .sortedWith(
                compareBy<PresenceDeviceDisplay> { it.lastChanged }
                    .thenBy { it.email.lowercase(java.util.Locale.ROOT) }
            )
            .toList()
    }

    val uniqueEmailCount = remember(inactiveDevices) {
        inactiveDevices
            .map { it.email.trim().lowercase(java.util.Locale.ROOT) }
            .filter { it.isNotBlank() && it != "guest" }
            .distinct()
            .size
    }
    val longestInactiveDays = remember(inactiveDevices, nowMillis) {
        inactiveDevices.maxOfOrNull { inactiveDays(it.lastChanged) } ?: 0L
    }
    val dateTimeFormatter = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    }

    @Composable
    fun RowScope.summaryCard(title: String, value: String, icon: ImageVector) {
        Column(
            modifier = Modifier
                .weight(1f)
                .height(76.dp)
                .background(surfaceColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = tunaGreen, modifier = Modifier.size(17.dp))
            Text(value, color = textColor, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
            Text(
                title,
                color = Color.Gray,
                fontSize = 9.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            summaryCard("Thiết bị", inactiveDevices.size.toString(), Icons.Default.Devices)
            summaryCard("Gmail", uniqueEmailCount.toString(), Icons.Default.Email)
            summaryCard("Lâu nhất", "${longestInactiveDays} ngày", Icons.Default.AccessTime)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = surfaceColor,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.10f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Không sử dụng từ",
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { inactiveFromDays = (inactiveFromDays - 1).coerceAtLeast(1) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(bgColor, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Remove, null, tint = textColor, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = inactiveFromDays.toString(),
                        color = tunaGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.width(42.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(
                        onClick = { inactiveFromDays = (inactiveFromDays + 1).coerceAtMost(3650) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(bgColor, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, null, tint = textColor, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("ngày", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(7, 14, 30, 60, 90).forEach { days ->
                        val selected = inactiveFromDays == days
                        OutlinedButton(
                            onClick = { inactiveFromDays = days },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) tunaGreen else textColor.copy(alpha = 0.16f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) tunaGreen.copy(alpha = 0.13f) else Color.Transparent,
                                contentColor = if (selected) tunaGreen else Color.Gray
                            )
                        ) {
                            Text("$days ngày", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            placeholder = { Text("Tìm Gmail, thiết bị hoặc mã thiết bị...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedIndicatorColor = tunaGreen,
                cursorColor = tunaGreen
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Số ngày được tính từ lần hoạt động cuối cùng của thiết bị.",
            color = Color.Gray,
            fontSize = 10.5.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))

        when {
            isHistoryLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = tunaGreen)
                }
            }

            historyError.isNotBlank() && allKnownDevices.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, null, tint = Color.Red, modifier = Modifier.size(34.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            historyError,
                            color = textColor,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            inactiveDevices.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = tunaGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (allKnownDevices.isEmpty()) {
                                "Chưa có dữ liệu lịch sử thiết bị."
                            } else {
                                "Không có thiết bị nào không hoạt động từ $inactiveFromDays ngày."
                            },
                            color = Color.Gray,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(inactiveDevices, key = { it.stableKey }) { device ->
                        val days = inactiveDays(device.lastChanged)
                        val normalizedEmail = device.email.trim()
                        val isGuest = normalizedEmail.isBlank() || normalizedEmail.equals("guest", ignoreCase = true)
                        val guestCode = device.deviceId.ifBlank { device.firebaseUid }
                            .take(8)
                            .uppercase(java.util.Locale.ROOT)
                        val identityText = if (isGuest) "Khách ($guestCode)" else normalizedEmail
                        val lastUseText = dateTimeFormatter.format(java.util.Date(device.lastChanged))
                        val deviceLine = device.deviceName.ifBlank { "Không rõ thiết bị" }
                        val systemParts = listOfNotNull(
                            device.appVersion.takeIf { it.isNotBlank() }?.let { "App $it" },
                            device.androidVersion.takeIf { it.isNotBlank() }?.let { "Android $it" }
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = surfaceColor,
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.09f))
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(tunaGreen.copy(alpha = 0.12f), RoundedCornerShape(9.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Devices,
                                            contentDescription = null,
                                            tint = tunaGreen,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(9.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            identityText,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (device.displayName.isNotBlank() &&
                                            !device.displayName.equals(identityText, ignoreCase = true)
                                        ) {
                                            Text(
                                                device.displayName,
                                                color = Color.Gray,
                                                fontSize = 10.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Surface(
                                        color = Color(0xFFFFA726).copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "$days ngày",
                                            color = Color(0xFFFFA726),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(7.dp))
                                Text(
                                    "Thiết bị: $deviceLine",
                                    color = textColor.copy(alpha = 0.86f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (systemParts.isNotEmpty()) {
                                    Text(
                                        systemParts.joinToString(" • "),
                                        color = Color.Gray,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    "Lần cuối sử dụng: $lastUseText",
                                    color = tunaGreen.copy(alpha = 0.88f),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Mã thiết bị: ${device.deviceId.ifBlank { device.firebaseUid }}",
                                    color = Color.Gray.copy(alpha = 0.82f),
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Danh sách online riêng đã được gộp vào DailyStatsContent.
// =========================================================================
// TAB 3: SO SÁNH CÁC CHỈ SỐ GIỮA CÁC THÁNG (TRUY VẤN TRỰC TIẾP TỪNG THÁNG)
// =========================================================================
data class MonthSummary(
    val monthStr: String,
    val totalUniqueUsers: Int,
    val totalSessions: Int,
    val totalTimeSeconds: Long
)

@Composable
fun MonthlyComparisonStatsContent(db: com.google.firebase.firestore.FirebaseFirestore, tunaGreen: Color, surfaceColor: Color, textColor: Color) {
    var isLoading by remember { mutableStateOf(true) }
    var monthSummaries by remember { mutableStateOf<List<MonthSummary>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        kotlinx.coroutines.coroutineScope {
            try {
                val dbFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                val displayFormat = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault())
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.MONTH, -5)

                val monthsList = (0..5).map {
                    val dbStr = dbFormat.format(cal.time)
                    val dispStr = displayFormat.format(cal.time)
                    cal.add(java.util.Calendar.MONTH, 1)
                    dbStr to dispStr
                }

                val resultList = monthsList.map { (dbStr, dispStr) ->
                    async(Dispatchers.IO) {
                        try {
                            val sysSnap = db.collection("System_Stats").document(dbStr).get().await()
                            val usersSnap = db.collection("Leaderboard_Monthly").document(dbStr).collection("Users").get().await()

                            val sessions = sysSnap.getLong("total_sessions")?.toInt() ?: 0
                            val time = sysSnap.getLong("total_time_seconds") ?: 0L
                            val uniqueUsers = usersSnap.documents.mapNotNull { doc ->
                                val rawEmail = doc.getString("email") ?: "guest"
                                if (rawEmail == "guest") doc.id else rawEmail
                            }.distinct().size

                            if (sessions > 0 || uniqueUsers > 0) {
                                MonthSummary(dispStr, uniqueUsers, sessions, time)
                            } else null
                        } catch (e: Exception) { null }
                    }
                }.awaitAll().filterNotNull()

                withContext(Dispatchers.Main) { monthSummaries = resultList; isLoading = false }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false } }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
    } else if (monthSummaries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không có dữ liệu so sánh", color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text("📊 Biểu đồ so sánh chỉ số qua các tháng", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF42A5F5))); Text(" Người dùng", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFA726))); Text(" Lượt dùng", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                    Box(modifier = Modifier.size(10.dp).background(tunaGreen, CircleShape)); Text(" Thời gian (giờ)", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    val chartWidth = (80.dp * monthSummaries.size).coerceAtLeast(300.dp)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.width(chartWidth).height(200.dp)) {
                            val maxUsers = monthSummaries.maxOfOrNull { it.totalUniqueUsers }?.coerceAtLeast(1) ?: 1
                            val maxSessions = monthSummaries.maxOfOrNull { it.totalSessions }?.coerceAtLeast(1) ?: 1
                            val maxTimeHours = monthSummaries.maxOfOrNull { it.totalTimeSeconds / 3600f }?.coerceAtLeast(1f) ?: 1f
                            val maxMetric = maxOf(maxUsers.toFloat(), maxSessions.toFloat(), maxTimeHours).coerceAtLeast(1f)

                            val canvasH = size.height - 30.dp.toPx()
                            val groupW = size.width / monthSummaries.size
                            val barW = (groupW * 0.25f).coerceAtMost(16.dp.toPx())
                            val labelPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }

                            for (i in monthSummaries.indices) {
                                val item = monthSummaries[i]
                                val centerX = i * groupW + groupW / 2f
                                val uH = (item.totalUniqueUsers / maxMetric) * (canvasH * 0.8f)
                                val sH = (item.totalSessions / maxMetric) * (canvasH * 0.8f)
                                val tH = ((item.totalTimeSeconds / 3600f) / maxMetric) * (canvasH * 0.8f)

                                drawRect(color = Color(0xFF42A5F5), topLeft = Offset(centerX - barW * 1.5f, canvasH - uH), size = Size(barW, uH))
                                drawRect(color = Color(0xFFFFA726), topLeft = Offset(centerX - barW * 0.5f, canvasH - sH), size = Size(barW, sH))
                                drawRect(color = tunaGreen, topLeft = Offset(centerX + barW * 0.5f, canvasH - tH), size = Size(barW, tH))
                                drawContext.canvas.nativeCanvas.drawText(item.monthStr, centerX, canvasH + 20.dp.toPx(), labelPaint)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("📋 Chi tiết từng tháng", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(monthSummaries.size) { index ->
                val item = monthSummaries[index]
                val h = item.totalTimeSeconds / 3600; val m = (item.totalTimeSeconds % 3600) / 60
                val timeStr = "${h}h ${m}m"

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(surfaceColor, RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.monthStr, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(75.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("👤 ${item.totalUniqueUsers} người dùng", color = Color(0xFF42A5F5), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("🔄 ${item.totalSessions} lượt sử dụng", color = Color(0xFFFFA726), fontSize = 13.sp)
                    }
                    Text("⏱ $timeStr", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

// =========================================================================
// TAB 4: THỐNG KÊ SỐ NGƯỜI DÙNG MỚI THEO THÁNG
// =========================================================================
data class NewUserMonthStat(
    val monthStr: String,
    val newUsersCount: Int,
    val totalUsersInMonth: Int,
    val newUserList: List<String>
)

@Composable
fun NewUsersStatsContent(db: com.google.firebase.firestore.FirebaseFirestore, tunaGreen: Color, surfaceColor: Color, textColor: Color) {
    var isLoading by remember { mutableStateOf(true) }
    var newUsersStats by remember { mutableStateOf<List<NewUserMonthStat>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        kotlinx.coroutines.coroutineScope {
            try {
                val dbFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                val displayFormat = java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault())

                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.MONTH, -5)
                val monthsList = (0..5).map {
                    val dbStr = dbFormat.format(cal.time)
                    val dispStr = displayFormat.format(cal.time)
                    cal.add(java.util.Calendar.MONTH, 1)
                    dbStr to dispStr
                }

                val results = monthsList.map { (dbStr, dispStr) ->
                    async(Dispatchers.IO) {
                        try {
                            val snap = db.collection("Leaderboard_Monthly").document(dbStr).collection("Users").get().await()
                            val identifiers = snap.documents.mapNotNull { doc ->
                                val rawEmail = doc.getString("email") ?: "guest"
                                if (rawEmail == "guest") doc.id else rawEmail
                            }.distinct()
                            dispStr to identifiers
                        } catch (e: Exception) { dispStr to emptyList<String>() }
                    }
                }.awaitAll()

                val userFirstSeenMap = mutableMapOf<String, String>()
                val statList = mutableListOf<NewUserMonthStat>()

                for ((monthStr, userIds) in results) {
                    val newUsersInMonth = mutableListOf<String>()
                    for (uid in userIds) {
                        if (!userFirstSeenMap.containsKey(uid)) {
                            userFirstSeenMap[uid] = monthStr
                            newUsersInMonth.add(uid)
                        }
                    }
                    if (userIds.isNotEmpty()) {
                        statList.add(NewUserMonthStat(monthStr, newUsersInMonth.size, userIds.size, newUsersInMonth))
                    }
                }

                withContext(Dispatchers.Main) { newUsersStats = statList; isLoading = false }
            } catch (e: Exception) { withContext(Dispatchers.Main) { isLoading = false } }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
    } else if (newUsersStats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Không có dữ liệu người dùng mới", color = Color.Gray) }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text("📈 Biểu đồ tăng trưởng người dùng mới", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    val chartWidth = (70.dp * newUsersStats.size).coerceAtLeast(300.dp)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.width(chartWidth).height(180.dp)) {
                            val maxNew = newUsersStats.maxOfOrNull { it.newUsersCount }?.coerceAtLeast(1) ?: 1
                            val canvasH = size.height - 30.dp.toPx()
                            val spaceX = size.width / newUsersStats.size
                            val barW = (spaceX * 0.45f).coerceAtMost(24.dp.toPx())

                            val labelPaint = android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
                            val axisPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }

                            for (i in newUsersStats.indices) {
                                val item = newUsersStats[i]
                                val barH = (item.newUsersCount.toFloat() / maxNew) * (canvasH * 0.8f)
                                val x = i * spaceX + (spaceX - barW) / 2f
                                val y = canvasH - barH

                                drawRect(color = tunaGreen, topLeft = Offset(x, y), size = Size(barW, barH))
                                if (item.newUsersCount > 0) drawContext.canvas.nativeCanvas.drawText("+${item.newUsersCount}", x + barW / 2f, y - 6.dp.toPx(), labelPaint)
                                drawContext.canvas.nativeCanvas.drawText(item.monthStr, x + barW / 2f, canvasH + 20.dp.toPx(), axisPaint)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("👥 Tỷ lệ người mới theo tháng", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(newUsersStats.size) { index ->
                val item = newUsersStats[index]
                val percentNew = if (item.totalUsersInMonth > 0) (item.newUsersCount.toFloat() / item.totalUsersInMonth) * 100 else 0f

                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(surfaceColor, RoundedCornerShape(12.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.width(85.dp)) {
                        Text(item.monthStr, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${item.totalUsersInMonth} tổng user", color = Color.Gray, fontSize = 11.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("+${item.newUsersCount} người mới", color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LinearProgressIndicator(progress = { (percentNew / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = tunaGreen, trackColor = Color.Gray.copy(alpha = 0.2f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(String.format("%.1f%%", percentNew), color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

// --- THÀNH PHẦN: THỐNG KÊ THEO NGÀY ---
private data class DailyUsageRawRecord(
    val stableKey: String,
    val deviceId: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String,
    val totalTimeSeconds: Long,
    val lastAccess: Long,
    val deviceName: String,
    val androidVersion: String
)

private data class DailyUsageDisplayRecord(
    val stableKey: String,
    val deviceId: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String,
    val totalTimeSeconds: Long,
    val lastAccess: Long,
    val deviceName: String,
    val androidVersion: String
)

// Chi tiet thong ke cua mot tai khoan khi Admin cham vao dong nguoi dung.
// Du lieu thoi gian duoc ghi theo ngay/thang trong Firestore. So lan vao app chi
// duoc hien thi neu document cua tai khoan DA co truong dem rieng; khong suy doan
// tu so ngay hoat dong hay bo dem total_sessions cua ca he thong.
private data class AccountUsageTarget(
    val accountKey: String,
    val email: String,
    val displayName: String,
    val deviceId: String,
    val deviceName: String,
    val androidVersion: String,
    val isOnline: Boolean? // null neu nguon thong ke khong co du lieu presence
)

private data class AccountUsagePeriod(
    val totalSeconds: Long = 0L,
    val appOpens: Long? = null,
    val recordedDevices: Int = 0
)

private data class AccountUsageByDate(
    val date: String,
    val usage: AccountUsagePeriod
)

private fun accountAppOpenCount(doc: com.google.firebase.firestore.DocumentSnapshot): Long? {
    // app_open_count duoc ghi khi app vao foreground boi TrackAccountAppOpens.
    // Cac ten con lai chi de tuong thich voi du lieu neu da duoc ghi boi ban khac.
    for (name in listOf("app_open_count", "session_count", "sessions_count", "total_sessions")) {
        val count = doc.getLong(name)
        if (count != null) return count.coerceAtLeast(0L)
    }
    return null
}

private fun aggregateAccountUsage(
    documents: List<com.google.firebase.firestore.DocumentSnapshot>,
    accountKey: String
): AccountUsagePeriod {
    val normalizedKey = accountKey.trim().lowercase(java.util.Locale.ROOT)
    val signedInAccount = normalizedKey.contains("@")
    val matched = documents.filter { doc ->
        if (signedInAccount) {
            doc.getString("email").orEmpty().trim().lowercase(java.util.Locale.ROOT) == normalizedKey
        } else {
            // Khach duoc nhan dien boi device_id, KHONG gop tat ca email guest.
            val docDeviceId = doc.getString("device_id").orEmpty().ifBlank { doc.id }
            docDeviceId.lowercase(java.util.Locale.ROOT) == normalizedKey
        }
    }
    val counts = matched.mapNotNull(::accountAppOpenCount)
    return AccountUsagePeriod(
        totalSeconds = matched.sumOf { (it.getLong("total_time_seconds") ?: 0L).coerceAtLeast(0L) },
        // Khong tra 0 neu co document nhung chua co thong tin so lan mo app.
        appOpens = if (matched.isEmpty()) 0L else if (counts.size == matched.size) counts.sum() else null,
        recordedDevices = matched.size
    )
}

private fun formatAccountUsageTime(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val secs = safe % 60L
    return if (hours > 0L) "${hours}h ${minutes}m ${secs}s"
        else if (minutes > 0L) "${minutes}m ${secs}s"
        else "${secs}s"
}

@Composable
private fun AccountUsageDetailDialog(
    db: com.google.firebase.firestore.FirebaseFirestore,
    target: AccountUsageTarget,
    selectedDate: String,
    tunaGreen: Color,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    onDismiss: () -> Unit
) {
    var dailyUsage by remember(target.accountKey, selectedDate) { mutableStateOf(AccountUsagePeriod()) }
    var weeklyUsage by remember(target.accountKey, selectedDate) { mutableStateOf(AccountUsagePeriod()) }
    var monthlyUsage by remember(target.accountKey, selectedDate) { mutableStateOf(AccountUsagePeriod()) }
    var dailyHistory by remember(target.accountKey, selectedDate) {
        mutableStateOf<List<AccountUsageByDate>>(emptyList())
    }
    var isLoading by remember(target.accountKey, selectedDate) { mutableStateOf(true) }
    var loadError by remember(target.accountKey, selectedDate) { mutableStateOf("") }

    LaunchedEffect(target.accountKey, selectedDate, db) {
        isLoading = true
        loadError = ""
        try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply {
                isLenient = false
            }
            val referenceDate = parser.parse(selectedDate)
                ?: throw IllegalArgumentException("Ngay thong ke khong hop le")
            val calendar = java.util.Calendar.getInstance().apply { time = referenceDate }
            val dateKeys = (0 until 7).map {
                val dateKey = parser.format(calendar.time)
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
                dateKey
            }

            // Chi tai khi Admin mo chi tiet: 7 ngay va 1 thang, khong truy van
            // lich su cho toan bo danh sach hang tram nguoi dung.
            val history = kotlinx.coroutines.coroutineScope {
                dateKeys.map { dateKey ->
                    async {
                        val snapshot = db.collection("Leaderboard_Daily")
                            .document(dateKey).collection("Users").get().await()
                        AccountUsageByDate(dateKey, aggregateAccountUsage(snapshot.documents, target.accountKey))
                    }
                }.awaitAll()
            }
            val monthlySnapshot = db.collection("Leaderboard_Monthly")
                .document(selectedDate.take(7)).collection("Users").get().await()

            dailyHistory = history
            dailyUsage = history.firstOrNull()?.usage ?: AccountUsagePeriod()
            monthlyUsage = aggregateAccountUsage(monthlySnapshot.documents, target.accountKey)
            weeklyUsage = AccountUsagePeriod(
                totalSeconds = history.sumOf { it.usage.totalSeconds },
                appOpens = if (history.any { it.usage.appOpens == null }) null
                    else history.sumOf { it.usage.appOpens ?: 0L },
                recordedDevices = history.sumOf { it.usage.recordedDevices }
            )
        } catch (error: Exception) {
            loadError = "Không tải được thống kê tài khoản: ${error.localizedMessage ?: "lỗi kết nối Firebase"}"
            android.util.Log.e("AccountUsageStats", "Failed to load account detail", error)
        } finally {
            isLoading = false
        }
    }

    @Composable
    fun PeriodSummary(title: String, usage: AccountUsagePeriod) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            border = BorderStroke(1.dp, tunaGreen.copy(alpha = 0.13f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Text(title, color = tunaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thời gian sử dụng", color = Color.Gray, fontSize = 10.5.sp)
                        Text(
                            formatAccountUsageTime(usage.totalSeconds),
                            color = textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Số lần vào app", color = Color.Gray, fontSize = 10.5.sp)
                        Text(
                            usage.appOpens?.toString() ?: "Chưa ghi nhận",
                            color = if (usage.appOpens == null) Color.Gray else textColor,
                            fontSize = if (usage.appOpens == null) 12.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
                val opens = usage.appOpens
                if (opens != null && opens > 0L) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Trung bình mỗi lần: ${formatAccountUsageTime(usage.totalSeconds / opens)}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding(),
            color = bgColor
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 19.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, "Quay lại", tint = textColor)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thống kê tài khoản", color = tunaGreen, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Ngày tham chiếu: $selectedDate",
                            color = Color.Gray,
                            fontSize = 10.5.sp
                        )
                    }
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = tunaGreen)
                }
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.20f))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(color = surfaceColor, shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(13.dp)) {
                                Text(
                                    target.displayName.ifBlank { target.email.ifBlank { "Khách" } },
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (target.email.isNotBlank() && target.email != "guest") {
                                    Text(target.email, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                                } else {
                                    Text("Khách (${target.deviceId.take(8).uppercase()})", color = Color.Gray, fontSize = 11.sp)
                                }
                                if (target.deviceName.isNotBlank()) {
                                    Text(
                                        target.deviceName + if (target.androidVersion.isNotBlank()) " • Android ${target.androidVersion}" else "",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (target.isOnline != null) {
                                    Text(
                                        if (target.isOnline == true) "● Đang online" else "○ Không online",
                                        color = if (target.isOnline == true) tunaGreen else Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = tunaGreen)
                            }
                        }
                    } else if (loadError.isNotBlank()) {
                        item {
                            Text(loadError, color = Color(0xFFE57373), fontSize = 12.sp)
                        }
                    } else {
                        item { PeriodSummary("Trong ngày $selectedDate", dailyUsage) }
                        item { PeriodSummary("7 ngày tính đến $selectedDate", weeklyUsage) }
                        item { PeriodSummary("Trong tháng ${selectedDate.take(7)}", monthlyUsage) }

                        if (listOf(dailyUsage, weeklyUsage, monthlyUsage).any {
                                it.recordedDevices > 0 && it.appOpens == null
                            }) {
                            item {
                                Text(
                                    "Một số ngày trước đây có thời gian sử dụng nhưng chưa có bộ đếm " +
                                        "lượt mở app. Các lượt cũ không thể suy ra chính xác từ thời gian.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        item {
                            Text(
                                "Số lần vào app được ghi từ phiên bản có tính năng này, không bao gồm " +
                                    "lượt mở trước khi cập nhật.",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                        item {
                            Spacer(Modifier.height(3.dp))
                            Text("Lịch sử 7 ngày", color = tunaGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        items(dailyHistory, key = { it.date }) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .background(surfaceColor, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    item.date.substring(8, 10) + "/" + item.date.substring(5, 7),
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(60.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(formatAccountUsageTime(item.usage.totalSeconds), color = tunaGreen, fontSize = 12.sp)
                                    Text(
                                        "Vào app: ${item.usage.appOpens?.toString() ?: "Chưa ghi nhận"}",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                if (item.usage.recordedDevices > 0) {
                                    Icon(Icons.Default.CheckCircle, null, tint = tunaGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        item {
                            Text(
                                "Thời gian chỉ gồm dữ liệu đã đồng bộ khi phiên sử dụng kết thúc. " +
                                    "Đăng nhập cùng Gmail trên nhiều máy được cộng thời gian theo tài khoản.",
                                color = Color.Gray,
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyStatsContent(db: com.google.firebase.firestore.FirebaseFirestore, tunaGreen: Color, surfaceColor: Color, bgColor: Color, textColor: Color) {
    // Realtime Database Presence được hiển thị trực tiếp trong tab Theo Ngày.
    val onlinePresence = rememberOnlinePresenceUiState()
    val onlineUsersByKey = remember(onlinePresence.users) {
        onlinePresence.users.associateBy { it.stableKey }
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val adminAccount = com.google.android.gms.auth.api.signin.GoogleSignIn
        .getLastSignedInAccount(context)
    val adminEmail = adminAccount?.email.orEmpty()
    val adminName = adminAccount?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: adminEmail.substringBefore("@").ifBlank { "Quản trị viên" }

    var contactTarget by remember { mutableStateOf<AdminContactTarget?>(null) }
    var contactSubject by remember { mutableStateOf("Admin chủ động liên hệ") }
    var contactMessage by remember { mutableStateOf("") }
    var isSendingContact by remember { mutableStateOf(false) }
    var contactError by remember { mutableStateOf("") }
    var showOnlineUsersOnly by remember { mutableStateOf(false) }
    var selectedUsageAccount by remember { mutableStateOf<AccountUsageTarget?>(null) }

    var usageList by remember {
        mutableStateOf<List<DailyUsageDisplayRecord>>(emptyList())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    var selectedDateString by remember { mutableStateOf(dateFormatter.format(java.util.Date())) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    var yesterdayTotalUsers by remember { mutableStateOf(0) }
    var yesterdayTotalTime by remember { mutableStateOf(0L) }

    LaunchedEffect(selectedDateString) {
        db.collection("Leaderboard_Daily").document(selectedDateString).collection("Users")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val rawList = snapshot.documents.mapNotNull { doc ->
                        val rawEmail = doc.getString("email") ?: "guest"
                        val deviceId = doc.getString("device_id").orEmpty().ifBlank { doc.id }
                        // Nếu là khách, dùng ID thiết bị làm định danh.
                        val identifier = if (rawEmail == "guest") {
                            deviceId
                        } else {
                            rawEmail.trim().lowercase(java.util.Locale.ROOT)
                        }

                        val manufacturer = doc.getString("device_manufacturer").orEmpty()
                        val brand = doc.getString("device_brand").orEmpty()
                        val model = doc.getString("device_model").orEmpty()
                        val deviceName = doc.getString("device_name")
                            .orEmpty()
                            .ifBlank { buildDeviceDisplayName(manufacturer, brand, model) }

                        DailyUsageRawRecord(
                            stableKey = identifier,
                            deviceId = deviceId,
                            firebaseUid = doc.getString("firebase_uid").orEmpty(),
                            email = rawEmail,
                            displayName = doc.getString("display_name").orEmpty(),
                            totalTimeSeconds = doc.getLong("total_time_seconds") ?: 0L,
                            lastAccess = doc.getLong("last_access_time") ?: 0L,
                            deviceName = deviceName,
                            androidVersion = doc.getString("android_version").orEmpty()
                        )
                    }

                    usageList = rawList.groupBy { it.stableKey }
                        .map { (stableKey, records) ->
                            val latestRecord = records.maxByOrNull { it.lastAccess }
                                ?: records.first()
                            val latestDeviceRecord = records
                                .filter { it.deviceName.isNotBlank() }
                                .maxByOrNull { it.lastAccess }
                                ?: latestRecord

                            DailyUsageDisplayRecord(
                                stableKey = stableKey,
                                deviceId = latestRecord.deviceId,
                                firebaseUid = records
                                    .filter { it.firebaseUid.isNotBlank() }
                                    .maxByOrNull { it.lastAccess }
                                    ?.firebaseUid
                                    .orEmpty(),
                                email = latestRecord.email,
                                displayName = records
                                    .filter { it.displayName.isNotBlank() }
                                    .maxByOrNull { it.lastAccess }
                                    ?.displayName
                                    .orEmpty(),
                                totalTimeSeconds = records.sumOf { it.totalTimeSeconds },
                                lastAccess = latestRecord.lastAccess,
                                deviceName = latestDeviceRecord.deviceName,
                                androidVersion = latestDeviceRecord.androidVersion
                            )
                        }
                        .sortedByDescending { it.totalTimeSeconds }
                }
            }

        val date = dateFormatter.parse(selectedDateString)
        if (date != null) {
            val calendar = java.util.Calendar.getInstance().apply {
                time = date; add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            db.collection("Leaderboard_Daily").document(dateFormatter.format(calendar.time)).collection("Users").get()
                .addOnSuccessListener { snapshot ->
                    yesterdayTotalUsers = snapshot.documents.mapNotNull { doc ->
                        val rawEmail = doc.getString("email") ?: "guest"
                        if (rawEmail == "guest") doc.id else rawEmail
                    }.distinct().size
                    yesterdayTotalTime = snapshot.documents.sumOf { it.getLong("total_time_seconds") ?: 0L }
                }
        }
    }

    val currentTotalUsers = usageList.size
    val currentTotalTime = usageList.sumOf { it.totalTimeSeconds }

    fun formatTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return buildString { if (h > 0) append("${h}h "); if (m > 0 || h > 0) append("${m}m "); append("${s}s") }.trim()
    }

    fun getTrend(current: Float, previous: Float): Pair<String, Color> {
        if (previous == 0f) return Pair(if (current > 0) "▲ +100%" else "-", Color.Gray)
        val percent = ((current - previous) / previous) * 100
        val prefix = if (percent > 0) "▲ +" else if (percent < 0) "▼ " else ""
        val color = if (percent > 0) tunaGreen else if (percent < 0) Color.Red else Color.Gray
        return Pair(String.format("%s%.1f%%", prefix, percent), color)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Dữ liệu ngày: $selectedDateString",
                color = tunaGreen,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    "Chọn ngày",
                    tint = tunaGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Ba chi so nam tren cung mot hang, chia deu chieu rong va khong cuon ngang.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val userTrend = getTrend(currentTotalUsers.toFloat(), yesterdayTotalUsers.toFloat())
            val timeTrend = getTrend(currentTotalTime.toFloat(), yesterdayTotalTime.toFloat())
            val totalTimeText = formatTime(currentTotalTime)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(surfaceColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Người dùng", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1)
                Text(
                    "$currentTotalUsers",
                    color = textColor,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    userTrend.first,
                    color = userTrend.second,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(surfaceColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 3.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tổng thời gian", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1)
                Text(
                    totalTimeText,
                    color = textColor,
                    fontSize = when {
                        totalTimeText.length >= 10 -> 11.5.sp
                        totalTimeText.length >= 8 -> 12.5.sp
                        else -> 14.sp
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    timeTrend.first,
                    color = timeTrend.second,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .background(surfaceColor, RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = if (onlinePresence.errorMessage.isBlank()) {
                            tunaGreen.copy(alpha = 0.42f)
                        } else {
                            Color.Red.copy(alpha = 0.42f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        enabled = !onlinePresence.isLoading && onlinePresence.errorMessage.isBlank()
                    ) {
                        showOnlineUsersOnly = true
                    }
                    .padding(horizontal = 3.dp, vertical = 7.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Đang online", color = Color.Gray, fontSize = 10.5.sp, maxLines = 1)
                    Text(
                        text = when {
                            onlinePresence.isLoading -> "..."
                            onlinePresence.errorMessage.isNotBlank() -> "!"
                            else -> onlinePresence.users.size.toString()
                        },
                        color = if (onlinePresence.errorMessage.isBlank()) textColor else Color.Red,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            onlinePresence.isLoading -> "đang cập nhật"
                            onlinePresence.errorMessage.isNotBlank() -> "lỗi dữ liệu"
                            else -> "người dùng"
                        },
                        color = if (onlinePresence.errorMessage.isBlank()) tunaGreen else Color.Red,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (onlinePresence.errorMessage.isBlank()) tunaGreen else Color.Red,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        if (usageList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Chưa có dữ liệu.", color = Color.Gray) }
        } else {
            val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(usageList.size) { index ->
                    val item = usageList[index]
                    val timeSeconds = item.totalTimeSeconds
                    val lastAccess = item.lastAccess

                    val onlineUser = onlineUsersByKey[item.stableKey]
                    val isOnlineNow = onlineUser != null
                    val effectiveDeviceId = onlineUser?.deviceId
                        ?.takeIf { it.isNotBlank() }
                        ?: item.deviceId
                    val effectiveFirebaseUid = onlineUser?.firebaseUid
                        ?.takeIf { it.isNotBlank() }
                        ?: item.firebaseUid
                    val effectiveEmail = onlineUser?.email
                        ?.takeIf { it.isNotBlank() }
                        ?: item.email
                    val effectiveDisplayName = onlineUser?.displayName
                        ?.takeIf { it.isNotBlank() }
                        ?: item.displayName.ifBlank {
                            if (effectiveEmail.isNotBlank() && effectiveEmail != "guest") {
                                effectiveEmail.substringBefore("@")
                            } else {
                                "Khách (${effectiveDeviceId.take(6).uppercase()})"
                            }
                        }
                    val titleText = if (effectiveEmail.isNotBlank() && effectiveEmail != "guest") {
                        effectiveEmail
                    } else {
                        "Khách (${effectiveDeviceId.take(6).uppercase()})"
                    }
                    val phoneName = onlineUser?.deviceName
                        ?.takeIf { it.isNotBlank() }
                        ?: item.deviceName
                    val androidVersion = onlineUser?.androidVersion
                        ?.takeIf { it.isNotBlank() }
                        ?: item.androidVersion

                    val target = AdminContactTarget(
                        deviceId = effectiveDeviceId,
                        firebaseUid = effectiveFirebaseUid,
                        email = effectiveEmail.ifBlank { "guest" },
                        displayName = effectiveDisplayName,
                        deviceName = phoneName,
                        isOnline = isOnlineNow
                    )

                    val primaryIdentity = titleText
                    val secondaryDisplayName = effectiveDisplayName.takeIf {
                        it.isNotBlank() && !it.equals(titleText, ignoreCase = true)
                    }.orEmpty()
                    val compactDeviceInfo = buildString {
                        if (phoneName.isNotBlank()) append(phoneName)
                        if (androidVersion.isNotBlank()) {
                            if (isNotEmpty()) append(" • ")
                            append("Android ")
                            append(androidVersion)
                        }
                        if (lastAccess > 0L) {
                            if (isNotEmpty()) append(" • ")
                            append(timeFormatter.format(java.util.Date(lastAccess)))
                        }
                        val onlineDeviceCount = onlineUser?.deviceCount ?: 1
                        if (onlineDeviceCount > 1) {
                            if (isNotEmpty()) append(" • ")
                            append(onlineDeviceCount)
                            append(" máy")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Thu gọn tối đa khoảng trống trên/dưới giữa các thẻ thống kê.
                            .padding(vertical = 1.dp)
                            .background(surfaceColor, RoundedCornerShape(12.dp))
                            .clickable {
                                selectedUsageAccount = AccountUsageTarget(
                                    accountKey = item.stableKey,
                                    email = effectiveEmail,
                                    displayName = effectiveDisplayName,
                                    deviceId = effectiveDeviceId,
                                    deviceName = phoneName,
                                    androidVersion = androidVersion,
                                    isOnline = isOnlineNow
                                )
                            }
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val rankColor = when (index) {
                            0 -> Color(0xFFFFD700)
                            1 -> Color(0xFFC0C0C0)
                            2 -> Color(0xFFCD7F32)
                            else -> Color.Gray
                        }

                        // Chấm xanh Online nằm hẳn PHÍA TRÊN số thứ tự để không chiếm
                        // chiều ngang của Gmail / tên người dùng.
                        Column(
                            modifier = Modifier
                                .width(28.dp)
                                .padding(end = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.height(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isOnlineNow) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(tunaGreen, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = "${index + 1}",
                                color = rankColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                lineHeight = 15.2.sp,
                                maxLines = 1
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            // Dòng 1: Gmail / mã thiết bị ở bên trái, tổng thời gian ở bên phải.
                            // Chỉ giảm cỡ chữ khi chuỗi thực sự dài so với vùng còn lại.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Không dùng BoxWithConstraints ở đây để tương thích ổn định
                                // với phiên bản Compose hiện tại của dự án. Cỡ chữ được điều
                                // chỉnh theo độ dài chuỗi và vẫn giới hạn để không quá nhỏ.
                                val fittedFontSp = when {
                                    primaryIdentity.length <= 24 -> 13.4f
                                    primaryIdentity.length <= 32 -> 12.6f
                                    primaryIdentity.length <= 40 -> 11.8f
                                    else -> 10.8f
                                }
                                Text(
                                    text = primaryIdentity,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fittedFontSp.sp,
                                    lineHeight = (fittedFontSp + 0.6f).sp,
                                    letterSpacing = (-0.05).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = formatTime(timeSeconds),
                                    color = tunaGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    lineHeight = 10.9.sp,
                                    maxLines = 1
                                )
                            }

                            // Dòng 2: tên người dùng tách riêng.
                            if (secondaryDisplayName.isNotBlank()) {
                                Text(
                                    text = secondaryDisplayName,
                                    color = textColor.copy(alpha = 0.82f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.8.sp,
                                    lineHeight = 11.2.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Dòng 3: tên thiết bị + Android + thời gian truy cập; icon chat trong suốt.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = compactDeviceInfo.ifBlank { "Chưa có thông tin thiết bị" },
                                    color = Color.Gray,
                                    fontSize = 9.9.sp,
                                    lineHeight = 10.2.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(3.dp))

                                // Không dùng IconButton vì Material3 tự ép vùng chạm tối thiểu 48dp,
                                // làm icon tràn lên dòng trên. Box clickable giữ đúng kích thước nhỏ.
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = effectiveDeviceId.isNotBlank()) {
                                            contactTarget = target
                                            contactSubject = "Admin chủ động liên hệ"
                                            contactMessage = ""
                                            contactError = ""
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Liên hệ người dùng",
                                        tint = if (effectiveDeviceId.isNotBlank()) tunaGreen else Color.Gray,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedUsageAccount?.let { account ->
        AccountUsageDetailDialog(
            db = db,
            target = account,
            selectedDate = selectedDateString,
            tunaGreen = tunaGreen,
            surfaceColor = surfaceColor,
            bgColor = bgColor,
            textColor = textColor,
            onDismiss = { selectedUsageAccount = null }
        )
    }

    if (showOnlineUsersOnly) {
        Dialog(
            onDismissRequest = { showOnlineUsersOnly = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                // Dùng màu nền do MainActivity truyền xuống để đồng bộ đúng chế độ Sáng/Tối
                // của ứng dụng, không dùng MaterialTheme mặc định của Dialog.
                color = bgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        // safeDrawing tránh cạnh màn hình/camera khi hệ thống cung cấp inset.
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        // SupportCenter đang ẩn status bar trên một số máy nên inset có thể bằng 0.
                        // Chừa thêm khoảng tối thiểu để tiêu đề luôn nằm dưới tai thỏ/camera đục lỗ.
                        .padding(top = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showOnlineUsersOnly = false },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Quay lại trang Admin",
                                tint = textColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Người dùng đang online",
                                color = tunaGreen,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${onlinePresence.users.size} người dùng đang hoạt động",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(tunaGreen.copy(alpha = 0.14f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = tunaGreen,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.18f))

                    when {
                        onlinePresence.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = tunaGreen)
                            }
                        }

                        onlinePresence.errorMessage.isNotBlank() -> {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = onlinePresence.errorMessage,
                                    color = Color.Red,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        onlinePresence.users.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Hiện không có người dùng online.", color = Color.Gray)
                            }
                        }

                        else -> {
                            val onlineTimeFormatter = remember {
                                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 10.dp,
                                    bottom = 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = onlinePresence.users,
                                    key = { it.stableKey }
                                ) { user ->
                                    val guestId = user.deviceId
                                        .ifBlank { user.firebaseUid }
                                        .take(6)
                                        .uppercase()
                                    val titleText = if (user.email.isNotBlank() && user.email != "guest") {
                                        user.email
                                    } else {
                                        "Khách ($guestId)"
                                    }
                                    val target = AdminContactTarget(
                                        deviceId = user.deviceId,
                                        firebaseUid = user.firebaseUid,
                                        email = user.email.ifBlank { "guest" },
                                        displayName = user.displayName,
                                        deviceName = user.deviceName,
                                        isOnline = true
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(surfaceColor, RoundedCornerShape(12.dp))
                                            .border(
                                                1.dp,
                                                tunaGreen.copy(alpha = 0.22f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                selectedUsageAccount = AccountUsageTarget(
                                                    accountKey = user.stableKey,
                                                    email = user.email,
                                                    displayName = user.displayName,
                                                    deviceId = user.deviceId,
                                                    deviceName = user.deviceName,
                                                    androidVersion = user.androidVersion,
                                                    isOnline = true
                                                )
                                                showOnlineUsersOnly = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(tunaGreen.copy(alpha = 0.14f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = tunaGreen,
                                                modifier = Modifier.size(21.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(10.dp)
                                                    .background(surfaceColor, CircleShape)
                                                    .padding(2.dp)
                                                    .background(tunaGreen, CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(11.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = titleText,
                                                color = textColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (user.displayName.isNotBlank() &&
                                                !user.displayName.equals(titleText, ignoreCase = true)
                                            ) {
                                                Text(
                                                    text = user.displayName,
                                                    color = Color.Gray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (user.deviceName.isNotBlank()) {
                                                Text(
                                                    text = buildString {
                                                        append(user.deviceName)
                                                        if (user.androidVersion.isNotBlank()) {
                                                            append(" • Android ")
                                                            append(user.androidVersion)
                                                        }
                                                        if (user.deviceCount > 1) {
                                                            append(" • ")
                                                            append(user.deviceCount)
                                                            append(" máy")
                                                        }
                                                    },
                                                    color = Color.Gray,
                                                    fontSize = 9.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "Hoạt động: ${onlineTimeFormatter.format(java.util.Date(user.lastChanged))}",
                                                color = tunaGreen.copy(alpha = 0.86f),
                                                fontSize = 9.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "ONLINE",
                                                color = tunaGreen,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            IconButton(
                                                onClick = {
                                                    contactTarget = target
                                                    contactSubject = "Admin chủ động liên hệ"
                                                    contactMessage = ""
                                                    contactError = ""
                                                },
                                                enabled = user.deviceId.isNotBlank(),
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .background(
                                                        tunaGreen.copy(alpha = 0.14f),
                                                        CircleShape
                                                    )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Chat,
                                                    contentDescription = "Liên hệ người dùng",
                                                    tint = tunaGreen,
                                                    modifier = Modifier.size(17.dp)
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
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDateString = dateFormatter.format(java.util.Date(it)) }; showDatePicker = false }) { Text("OK", color = tunaGreen) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Hủy", color = Color.Gray) } }
        ) { DatePicker(state = datePickerState) }
    }

    contactTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                if (!isSendingContact) {
                    contactTarget = null
                    contactError = ""
                }
            },
            containerColor = surfaceColor,
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(tunaGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = tunaGreen,
                        modifier = Modifier.size(25.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Liên hệ người dùng",
                    color = tunaGreen,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = target.displayName.ifBlank {
                            if (target.email != "guest") target.email else "Khách"
                        },
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (target.email.isNotBlank() && target.email != "guest") {
                        Text(target.email, color = Color.Gray, fontSize = 11.sp)
                    }
                    Text(
                        text = buildString {
                            append(if (target.isOnline) "● Đang online" else "○ Đang offline")
                            if (target.deviceName.isNotBlank()) {
                                append(" • ")
                                append(target.deviceName)
                            }
                        },
                        color = if (target.isOnline) tunaGreen else Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = contactSubject,
                        onValueChange = {
                            contactSubject = it
                            contactError = ""
                        },
                        label = { Text("Tiêu đề") },
                        singleLine = true,
                        readOnly = isSendingContact,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            disabledContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            disabledTextColor = textColor.copy(alpha = 0.75f),
                            focusedBorderColor = tunaGreen,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.45f),
                            disabledBorderColor = Color.Gray.copy(alpha = 0.28f),
                            focusedLabelColor = tunaGreen,
                            unfocusedLabelColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            cursorColor = tunaGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contactMessage,
                        onValueChange = {
                            contactMessage = it
                            contactError = ""
                        },
                        label = { Text("Nội dung liên hệ") },
                        minLines = 4,
                        maxLines = 7,
                        readOnly = isSendingContact,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surfaceColor,
                            unfocusedContainerColor = surfaceColor,
                            disabledContainerColor = surfaceColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            disabledTextColor = textColor.copy(alpha = 0.75f),
                            focusedBorderColor = tunaGreen,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.45f),
                            disabledBorderColor = Color.Gray.copy(alpha = 0.28f),
                            focusedLabelColor = tunaGreen,
                            unfocusedLabelColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            cursorColor = tunaGreen
                        )
                    )

                    if (contactError.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = contactError,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nếu người dùng đang offline, tin nhắn vẫn được lưu và popup sẽ xuất hiện khi họ mở ứng dụng trở lại.",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        contactTarget = null
                        contactError = ""
                    },
                    enabled = !isSendingContact
                ) {
                    Text("Hủy", color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactMessage.isBlank()) {
                            contactError = "Vui lòng nhập nội dung liên hệ."
                            return@Button
                        }

                        isSendingContact = true
                        contactError = ""
                        coroutineScope.launch {
                            val result = TunerSupportApi.startAdminContact(
                                adminEmail = adminEmail,
                                adminName = adminName,
                                target = target,
                                subject = contactSubject.ifBlank { "Admin chủ động liên hệ" },
                                message = contactMessage.trim()
                            )

                            isSendingContact = false
                            if (result.success) {
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                contactTarget = null
                                contactMessage = ""
                                contactError = ""
                            } else {
                                contactError = result.message
                            }
                        }
                    },
                    enabled = !isSendingContact && contactMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                ) {
                    if (isSendingContact) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

// Model lưu dữ liệu gom nhóm của user
data class UserAggregatedData(val email: String, val sessions: Int, val totalTime: Long)

@Composable
fun MonthlyStatsContent(
    db: com.google.firebase.firestore.FirebaseFirestore,
    tunaGreen: Color,
    surfaceColor: Color,
    textColor: Color,
    bgColor: Color = Color.Transparent // Gia tri mac dinh giu tuong thich caller cu.
) {
    var isLoading by remember { mutableStateOf(true) }
    var chartData by remember { mutableStateOf<List<Pair<String, Pair<Int, Long>>>>(emptyList()) }
    var leaderboard by remember { mutableStateOf<List<UserAggregatedData>>(emptyList()) }
    var selectedUsageAccount by remember { mutableStateOf<AccountUsageTarget?>(null) }

    val calendar = remember { java.util.Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(calendar.clone() as java.util.Calendar) }
    val monthFormat = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()) }
    val displayFormat = remember { java.text.SimpleDateFormat("MM/yyyy", java.util.Locale.getDefault()) }

    LaunchedEffect(selectedMonth) {
        isLoading = true
        val monthStr = monthFormat.format(selectedMonth.time)
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

        val workCal = selectedMonth.clone() as java.util.Calendar
        workCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val maxDays = workCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val datesList = (0 until maxDays).map {
            val d = dateFormat.format(workCal.time)
            workCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
            d
        }

        kotlinx.coroutines.coroutineScope {
            val leaderboardJob = async(Dispatchers.IO) {
                try {
                    val snap = db.collection("Leaderboard_Monthly").document(monthStr).collection("Users").get().await()

                    val rawUsers = snap.documents.map { doc ->
                        val rawEmail = doc.getString("email") ?: "guest"
                        val identifier = if (rawEmail == "guest") doc.id else rawEmail
                        Pair(identifier, doc.getLong("total_time_seconds") ?: 0L)
                    }

                    rawUsers.groupBy { it.first }
                        .map { (email, records) ->
                            val totalTime = records.sumOf { it.second }
                            UserAggregatedData(email, records.size, totalTime)
                        }
                        .sortedByDescending { it.totalTime }
                } catch (e: Exception) { emptyList() }
            }

            val chartJob = async(Dispatchers.IO) {
                datesList.mapIndexed { index, dateStr ->
                    async {
                        try {
                            val snap = db.collection("Leaderboard_Daily").document(dateStr).collection("Users").get().await()

                            // Lọc trùng lặp email để lấy số lượng người dùng thực tế giống Tab Ngày
                            val usersCount = snap.documents.mapNotNull { doc ->
                                val rawEmail = doc.getString("email") ?: "guest"
                                if (rawEmail == "guest") doc.id else rawEmail
                            }.distinct().size

                            val totalTimeDay = snap.documents.sumOf { it.getLong("total_time_seconds") ?: 0L }
                            val shortDate = dateStr.substring(8, 10) + "/" + dateStr.substring(5, 7)
                            Pair(index, shortDate to Pair(usersCount, totalTimeDay))
                        } catch (e: Exception) {
                            val shortDate = dateStr.substring(8, 10) + "/" + dateStr.substring(5, 7)
                            Pair(index, shortDate to Pair(0, 0L))
                        }
                    }
                }.awaitAll().sortedBy { it.first }.map { it.second }
            }

            leaderboard = leaderboardJob.await()
            chartData = chartJob.await()
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { val newCal = selectedMonth.clone() as java.util.Calendar; newCal.add(java.util.Calendar.MONTH, -1); selectedMonth = newCal }) { Icon(Icons.Default.ChevronLeft, "Tháng trước", tint = tunaGreen) }
            Text("Tháng ${displayFormat.format(selectedMonth.time)}", color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { val newCal = selectedMonth.clone() as java.util.Calendar; newCal.add(java.util.Calendar.MONTH, 1); selectedMonth = newCal }) { Icon(Icons.Default.ChevronRight, "Tháng sau", tint = tunaGreen) }
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF888888))); Text(" Số người dùng", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 16.dp))
                Box(modifier = Modifier.size(12.dp).background(tunaGreen, CircleShape)); Text(" Thời gian sử dụng", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }

            @SuppressLint("UnusedBoxWithConstraintsScope")
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
                val minCanvasWidth = 45.dp * chartData.size
                val canvasWidth = if (minCanvasWidth > maxWidth) minCanvasWidth else maxWidth
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.width(canvasWidth).height(230.dp)) {
                        val actualCanvasWidth = size.width
                        val actualCanvasHeight = size.height - 30.dp.toPx()
                        if (chartData.isNotEmpty()) {
                            val maxUsers = chartData.maxOfOrNull { it.second.first }?.coerceAtLeast(1) ?: 1
                            val maxTime = chartData.maxOfOrNull { it.second.second }?.coerceAtLeast(1L) ?: 1L
                            val barWidth = (actualCanvasWidth / chartData.size) * 0.6f
                            val spaceX = actualCanvasWidth / chartData.size

                            val barLabelPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
                            val timeLabelPaint = android.graphics.Paint().apply { color = tunaGreen.toArgb(); textSize = 11.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true }
                            val barYPositions = FloatArray(chartData.size)

                            for (i in chartData.indices) {
                                val users = chartData[i].second.first
                                val barHeight = (users.toFloat() / maxUsers) * (actualCanvasHeight * 0.8f)
                                val x = i * spaceX + (spaceX - barWidth) / 2f
                                val y = actualCanvasHeight - barHeight
                                barYPositions[i] = y
                                drawRect(color = Color(0xFF888888).copy(alpha = 0.5f), topLeft = Offset(x, y), size = Size(barWidth, barHeight))
                                if (users > 0) drawContext.canvas.nativeCanvas.drawText(users.toString(), x + barWidth / 2f, y - 6.dp.toPx(), barLabelPaint)
                            }

                            val path = androidx.compose.ui.graphics.Path()
                            for (i in chartData.indices) {
                                val time = chartData[i].second.second
                                val x = i * spaceX + spaceX / 2f
                                val y = actualCanvasHeight - (time.toFloat() / maxTime) * (actualCanvasHeight * 0.8f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                drawCircle(color = tunaGreen, radius = 12f, center = Offset(x, y))

                                if (time > 0) {
                                    val h = time / 3600; val m = (time % 3600) / 60; val s = time % 60
                                    val timeStr = if (h > 0) "${h}h${m}" else if (m > 0) "${m}m" else "${s}s"
                                    var timeTextY = y - 12.dp.toPx()
                                    val barTextY = barYPositions[i] - 6.dp.toPx()
                                    val safeDistance = 16.dp.toPx()
                                    if (kotlin.math.abs(timeTextY - barTextY) < safeDistance) timeTextY = minOf(timeTextY, barTextY) - safeDistance
                                    drawContext.canvas.nativeCanvas.drawText(timeStr, x, timeTextY, timeLabelPaint)
                                }
                            }
                            drawPath(path = path, color = tunaGreen, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))

                            val textPaint = android.graphics.Paint().apply { color = android.graphics.Color.GRAY; textSize = 10.sp.toPx(); textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true }
                            for (i in chartData.indices) {
                                drawContext.canvas.nativeCanvas.drawText(chartData[i].first, i * spaceX + spaceX / 2f, actualCanvasHeight + 20.dp.toPx(), textPaint)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("🏆 Bảng xếp hạng (Tháng ${displayFormat.format(selectedMonth.time)})", color = tunaGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (leaderboard.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Không có dữ liệu trong tháng này.", color = Color.Gray) }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(leaderboard.size) { index ->
                        val user = leaderboard[index]
                        val displayName = if (!user.email.contains("@")) "Khách (${user.email.take(6).uppercase()})" else user.email
                        val h = user.totalTime / 3600; val m = (user.totalTime % 3600) / 60; val s = user.totalTime % 60
                        val timeStr = buildString { if (h > 0) append("${h}h "); if (m > 0 || h > 0) append("${m}m "); append("${s}s") }.trim()

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedUsageAccount = AccountUsageTarget(
                                        accountKey = user.email.trim().lowercase(java.util.Locale.ROOT),
                                        email = if (user.email.contains("@")) user.email else "guest",
                                        displayName = displayName,
                                        deviceId = if (user.email.contains("@")) "" else user.email,
                                        deviceName = "",
                                        androidVersion = "",
                                        isOnline = null
                                    )
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rankColor = when (index) { 0 -> Color(0xFFFFD700); 1 -> Color(0xFFC0C0C0); 2 -> Color(0xFFCD7F32); else -> Color.Gray }
                            Text(text = "${index + 1}", color = rankColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1, modifier = Modifier.widthIn(min = 45.dp).padding(end = 8.dp))
                            Column(modifier = Modifier.weight(1f)) { Text(displayName, color = textColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            Text(timeStr, color = tunaGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    selectedUsageAccount?.let { account ->
        AccountUsageDetailDialog(
            db = db,
            target = account,
            selectedDate = java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.getDefault()
            ).format(selectedMonth.time),
            tunaGreen = tunaGreen,
            surfaceColor = surfaceColor,
            bgColor = if (bgColor == Color.Transparent) MaterialTheme.colorScheme.background else bgColor,
            textColor = textColor,
            onDismiss = { selectedUsageAccount = null }
        )
    }

}