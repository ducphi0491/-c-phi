package duc_phi.music

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

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
    var isActive by mutableStateOf(false)
        private set
    var accessKind by mutableStateOf("none")
        private set
    var expiresAtMillis by mutableStateOf(0L)
        private set
    var enforceFeatures by mutableStateOf(false)
        private set
    var checkoutEnabled by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    val canUsePaidFeatures: Boolean get() = isActive && expiresAtMillis > System.currentTimeMillis()
    val equalizerRequiresPremium: Boolean get() = enforceFeatures && !canUsePaidFeatures

    fun applyServerStatus(data: JSONObject) {
        // The backend must validate Google ID token before returning this response.
        isActive = data.optBoolean("access", false)
        accessKind = data.optString("accessKind", "none")
        expiresAtMillis = data.optLong("expiresAtMillis", 0L)
        checkoutEnabled = data.optBoolean("checkoutEnabled", false)
        enforceFeatures = data.optBoolean("enforceFeatures", false)
        lastError = null
    }

    fun reportError(message: String) {
        lastError = message
        checkoutEnabled = false // FAIL CLOSED for charging if the verification server is unavailable.
        // Do not erase a previously validated access state during a transient outage.
    }

    fun clearAccount() {
        isActive = false
        accessKind = "none"
        expiresAtMillis = 0L
        checkoutEnabled = false
        lastError = null
    }
}

/** API requests run off the UI thread. Google ID tokens are never placed in URLs or logs. */
private object PremiumBackend {
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
            val request = JSONObject().put("action", action).put("idToken", idToken)
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
                connection.outputStream.use { it.write(request.toString().toByteArray(StandardCharsets.UTF_8)) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val content = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val response = runCatching { JSONObject(content) }.getOrElse {
                    throw IllegalStateException("Máy chủ Premium chưa được cấu hình (${connection.responseCode}).")
                }
                if (connection.responseCode !in 200..299 || !response.optBoolean("ok", false)) {
                    throw IllegalStateException(response.optString("message", "Không thể xác minh Premium (${connection.responseCode})."))
                }
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
    LaunchedEffect(billing) { billing.refreshPrices() }
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
                            type = PremiumBenefitType.EQUALIZER,
                            title = mwString("premium.benefit_equalizer", "Equalizer nâng cao"),
                            detail = mwString("premium.benefit_equalizer_detail", "Tùy chỉnh âm thanh theo sở thích của bạn.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.CLOUD,
                            title = mwString("premium.benefit_drive", "Tiện ích Google Drive & chia sẻ nâng cao"),
                            detail = mwString("premium.cloud_coming", "Tính năng nâng cao đang phát triển; chia sẻ hiện tại vẫn miễn phí.")
                        )
                        PremiumBenefitCard(
                            type = PremiumBenefitType.AD_FREE,
                            title = mwString("premium.benefit_ads", "Nghe nhạc không quảng cáo"),
                            detail = mwString("premium.benefit_ads_detail", "Thưởng thức âm nhạc không bị quảng cáo làm gián đoạn.")
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
                            MeloWarePremiumAccess.enforceFeatures ->
                                mwString("premium.trial_ended", "Dùng thử đã hết hạn")
                            else -> mwString("premium.trial_pending", "Dùng thử 30 ngày sẽ mở khi máy chủ sẵn sàng")
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
                        Text(mwString("premium.sign_in", "Đăng nhập Google để kích hoạt Premium"),
                            color = colors.purpleBright)
                    }
                }
                Button(onClick = { billing.buy(selectedPlan) }, enabled = canPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)) {
                    Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (canPurchase) mwString("premium.buy_now", "Thanh toán qua Google Play")
                    else mwString("premium.activate_wait", "Kích hoạt Premium — sắp ra mắt"))
                }
                TextButton(onClick = { billing.restorePurchases(); billing.refreshEntitlement() },
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
                MeloWarePremiumAccess.lastError?.let {
                    Text(mwString("premium.server_unavailable", "Máy chủ xác minh chưa sẵn sàng. Không thu tiền."),
                        color = colors.textSecondary, fontSize = 11.sp)
                    TextButton(onClick = { billing.refreshEntitlement(); billing.refreshPrices() }) {
                        Text(mwString("premium.retry_short", "Thử lại"), color = colors.purpleBright)
                    }
                }
                Text(mwString("premium.verify_note", "Thanh toán chỉ mở khi máy chủ xác minh Google Play hoạt động. Dùng thử được tính theo tài khoản Google."),
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
private enum class PremiumBenefitType { EQUALIZER, CLOUD, AD_FREE }

@Composable
private fun PremiumBenefitCard(type: PremiumBenefitType, title: String, detail: String) {
    val colors = LocalMusicColors.current
    val accent = when (type) {
        PremiumBenefitType.EQUALIZER -> colors.cyan
        PremiumBenefitType.CLOUD -> colors.blue
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
    // Each card animates independently, so prices, Billing queries and the dialog
    // outside this small icon are not recomposed on every animation frame.
    val transition = rememberInfiniteTransition(label = "premium_icon_${type.name}")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (type) {
                    PremiumBenefitType.EQUALIZER -> 1550
                    PremiumBenefitType.CLOUD -> 2300
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
            PremiumBenefitType.EQUALIZER -> {
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
            PremiumBenefitType.CLOUD -> {
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
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(16.dp)
                        .graphicsLayer { translationX = drift * 2f }
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