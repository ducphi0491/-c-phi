package duc_phi.music

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.os.Build
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun NowPlayingScreen(controller: MusicController) {
    val colors = LocalMusicColors.current
    val song = controller.currentSong
    var showEqualizer by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var actionSong by remember { mutableStateOf<SongItem?>(null) }
    var karaokeMode by rememberSaveable(song?.id) { mutableStateOf(false) }
    var wifiSharePage by remember { mutableStateOf(WifiSharePage.NONE) }
    var showCastScanPanel by remember { mutableStateOf(false) }
    val screenListState = rememberLazyListState()

    BackHandler { controller.goBackFromPlayer() }

    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            EmptyLibraryState(
                title = "Chưa có bài đang phát",
                description = "Chọn một bài hát trong Trang chủ hoặc Thư viện.",
                buttonText = "Mở Thư viện",
                onButton = { controller.openLibrary() }
            )
        }
        return
    }

    // Keep one Cast route button attached to the activity; the Cast chooser is
    // opened only after the user explicitly presses Search in our dialog.
    val castContext = LocalContext.current
    val castRouteButton = remember(castContext) {
        MediaRouteButton(castContext).apply {
            CastButtonFactory.setUpMediaRouteButton(castContext, this)
        }
    }

    LaunchedEffect(Unit) { controller.initializeCast() }

    LaunchedEffect(song.id, controller.isLoading) {
        if (!controller.isLoading) controller.ensureAudioTechnicalInfo(song)
    }

    // Khi mo danh sach, doi den khung tiep theo de item duoc do kich thuoc,
    // sau do cuon mem den khu vuc danh sach phat o cuoi man hinh.
    LaunchedEffect(showQueue) {
        if (showQueue) {
            withFrameNanos { }
            screenListState.animateScrollToItem(4)
        }
    }

    androidx.compose.runtime.LaunchedEffect(song.id) {
        controller.ensureArtwork(song)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, Color(0xFF0A0B22), colors.background)
                )
            )
            .statusBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        LazyColumn(
            state = screenListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
        item {
            NowPlayingHeader(
                controller = controller,
                karaokeActive = karaokeMode,
                onBack = controller::goBackFromPlayer,
                onLyrics = { karaokeMode = !karaokeMode },
                castRouteButton = castRouteButton,
                onCastRequested = { showCastScanPanel = true },
                onWifiPlayback = { wifiSharePage = WifiSharePage.CHOOSER },
                onTools = if (controller.canUseLocalAudioTools(song)) {
                    { actionSong = song }
                } else null
            )
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (song.source == SongSource.MOVIE) {
                    MoviePlayerSurface(
                        controller = controller,
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .aspectRatio(16f / 9f)
                            .heightIn(min = 200.dp)
                    )
                } else if (karaokeMode) {
                    KaraokeBackdrop(
                        controller = controller,
                        song = song,
                        modifier = Modifier
                            .fillMaxWidth(0.94f)
                            .widthIn(max = 380.dp)
                            .aspectRatio(1f)
                    )
                } else {
                    ArtworkImage(
                        song = song,
                        artworkUrl = controller.artworkFor(song),
                        modifier = Modifier
                            .fillMaxWidth(0.90f)
                            .widthIn(max = 360.dp)
                            .aspectRatio(1f),
                        cornerRadius = 24
                    )
                }
            }
        }

        item {
            TrackTitleBlock(
                controller = controller,
                song = song
            )
        }

        item {
            // Gom tiến trình, cụm Play và các nút công cụ vào một khối để đẩy chúng lên cao hơn.
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                PlaybackProgress(
                    positionMs = controller.positionMs,
                    durationMs = controller.durationMs,
                    onSeek = controller::seekTo
                )
                MainPlaybackControls(controller)
                PlayerActions(
                    controller = controller,
                    song = song,
                    onEqualizer = { showEqualizer = true },
                    onTimer = { showTimer = true },
                    onQueue = { showQueue = !showQueue },
                    queueVisible = showQueue
                )
            }
        }

        if (showQueue) {
            item(key = "now_playing_queue") {
                QueuePanel(
                    controller = controller,
                    onClose = { showQueue = false },
                    onTools = { selected -> actionSong = selected }
                )
            }
        }
        }

    }

    if (showEqualizer) {
        EqualizerDialog(
            controller = controller,
            onDismiss = { showEqualizer = false }
        )
    }

    if (showTimer) {
        SleepTimerDialog(
            currentMinutes = controller.sleepTimerMinutesLeft,
            onSelect = { minutes ->
                controller.setSleepTimer(minutes)
                showTimer = false
            },
            onDismiss = { showTimer = false }
        )
    }

    if (showCastScanPanel) {
        CastScanPanelDialog(
            controller = controller,
            onSearchDevices = {
                // Only this explicit action opens the official Cast device picker.
                controller.initializeCast()
                val result = runCatching { castRouteButton.showDialog() }
                if (result.getOrDefault(false)) {
                    showCastScanPanel = false
                } else {
                    controller.statusMessage = result.exceptionOrNull()?.localizedMessage
                        ?.let { "Không mở được danh sách Cast: $it" }
                        ?: "Không mở được bảng chọn Cast. Hãy kiểm tra Wi-Fi và thiết bị hỗ trợ Google Cast."
                }
            },
            onDismiss = { showCastScanPanel = false }
        )
    }

    when (wifiSharePage) {
        WifiSharePage.CHOOSER -> WifiShareChoiceDialog(
            onBrowser = {
                wifiSharePage = WifiSharePage.BROWSER
                if (!controller.wifiHostActive && !controller.wifiHostBusy) {
                    controller.toggleWifiHost()
                }
            },
            onMeloWare = { wifiSharePage = WifiSharePage.APP },
            onDismiss = { wifiSharePage = WifiSharePage.NONE }
        )
        WifiSharePage.BROWSER -> WifiHostDialog(
            controller = controller,
            onDismiss = { wifiSharePage = WifiSharePage.NONE }
        )
        WifiSharePage.APP -> MeloWareWifiAppDialog(
            controller = controller,
            onDismiss = { wifiSharePage = WifiSharePage.NONE }
        )
        WifiSharePage.NONE -> Unit
    }

    actionSong?.let { selectedSong ->
        SongToolsHost(
            controller = controller,
            song = selectedSong,
            onDismiss = { actionSong = null }
        )
    }
}

@Composable
private fun NowPlayingHeader(
    controller: MusicController,
    karaokeActive: Boolean,
    onBack: () -> Unit,
    onLyrics: () -> Unit,
    castRouteButton: MediaRouteButton,
    onCastRequested: () -> Unit,
    onWifiPlayback: () -> Unit,
    onTools: (() -> Unit)?
) {
    val colors = LocalMusicColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = "Quay lại",
                tint = colors.textPrimary
            )
        }

        Spacer(Modifier.weight(1f))

        IconButton(onClick = onLyrics) {
            Icon(
                imageVector = Icons.Rounded.MicExternalOn,
                contentDescription = "Micro karaoke - Mở lời bài hát",
                tint = if (karaokeActive) colors.purpleBright else colors.textSecondary,
                modifier = Modifier.size(25.dp)
            )
        }

        Box(contentAlignment = Alignment.Center) {
            // Tapping Cast only opens MeloWare's own panel, without
            // immediately opening the system Cast route discovery dialog.
            IconButton(onClick = onCastRequested) {
                Icon(
                    Icons.Rounded.Cast,
                    contentDescription = "Cast lên TV hoặc thiết bị âm thanh",
                    tint = when {
                        controller.castConnected -> colors.purpleBright
                        controller.castConnecting -> colors.blue
                        else -> colors.textSecondary
                    }
                )
            }
            AndroidView(
                factory = { castRouteButton },
                modifier = Modifier
                    .size(1.dp)
                    .graphicsLayer(alpha = 0f)
            )
        }

        IconButton(onClick = onWifiPlayback) {
            Icon(
                Icons.Rounded.Wifi,
                contentDescription = "Phát nhạc qua Wi-Fi nội bộ",
                tint = if (controller.wifiHostActive) colors.purpleBright else colors.textSecondary
            )
        }

        if (onTools != null) {
            IconButton(onClick = onTools) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Cắt, ghép và nhạc chuông",
                    tint = colors.textSecondary
                )
            }
        }
    }
}

/**
 * First step of Cast: show a MeloWare panel. Actual discovery and route
 * selection remain in Google's supported MediaRouteButton picker and happen
 * only when the user presses the search button below.
 */
@Composable
private fun CastScanPanelDialog(
    controller: MusicController,
    onSearchDevices: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.48f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colors.purple.copy(alpha = 0.15f)
                    ) {
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Cast, contentDescription = null,
                                tint = colors.purpleBright)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Kết nối Google Cast", color = colors.textPrimary,
                            fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("TV, Chromecast và loa hỗ trợ Cast",
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng bảng Cast",
                            tint = colors.textSecondary)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Tv, contentDescription = null,
                            tint = if (controller.castConnected) colors.purpleBright
                                   else colors.textSecondary,
                            modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (controller.castConnected) "Đã kết nối thiết bị"
                                else if (controller.castConnecting) "Đang kết nối..."
                                else "Chưa kết nối thiết bị nào",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (controller.castConnected) {
                                Text(controller.castDeviceName ?: "Thiết bị Google Cast",
                                    color = colors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text(
                    "Kết nối điện thoại và TV/loa vào cùng mạng Wi-Fi. " +
                    "Nhấn Tìm thiết bị để mở danh sách Google Cast và chọn nơi phát nhạc.",
                    color = colors.textSecondary, fontSize = 12.sp
                )

                Button(
                    onClick = onSearchDevices,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                ) {
                    Icon(Icons.Rounded.Cast, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(if (controller.castConnected) "Tìm / đổi thiết bị Cast"
                         else "Bắt đầu tìm thiết bị", fontWeight = FontWeight.Bold)
                }
                Text(
                    "Bảng chọn thiết bị sẽ chỉ xuất hiện khi bạn nhấn nút trên.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun MoviePlayerSurface(
    controller: MusicController,
    song: SongItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    var fullScreenView by remember { mutableStateOf<View?>(null) }
    var fullScreenCallback by remember {
        mutableStateOf<WebChromeClient.CustomViewCallback?>(null)
    }

    val appIdentity = remember(context.packageName) {
        "https://${context.packageName.lowercase(Locale.ROOT)}"
    }
    val videoId = remember(song.id, song.uri) { controller.movieVideoId(song) }

    val webView = remember(song.id) {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(AndroidColor.BLACK)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                userAgentString = WebSettings.getDefaultUserAgent(context)
            }
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    return super.onConsoleMessage(consoleMessage)
                }

                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (view == null || callback == null) return
                    fullScreenView = view
                    fullScreenCallback = callback
                }

                override fun onHideCustomView() {
                    fullScreenCallback?.onCustomViewHidden()
                    fullScreenView = null
                    fullScreenCallback = null
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = false
            }
        }
    }

    DisposableEffect(webView) {
        controller.attachMovieWebView(webView)
        webView.onResume()
        onDispose {
            controller.detachMovieWebView(webView)
            runCatching { webView.onPause() }
            runCatching { webView.stopLoading() }
            runCatching { webView.loadUrl("about:blank") }
            runCatching { webView.destroy() }
        }
    }

    LaunchedEffect(song.id, videoId, appIdentity) {
        val resolvedVideoId = videoId
        if (resolvedVideoId == null) {
            controller.updateMoviePlaybackState(
                position = 0L,
                duration = song.durationMs,
                playerState = -1,
                ready = false,
                errorCode = 2
            )
            return@LaunchedEffect
        }
        val html = moviePlayerHtml(
            videoId = resolvedVideoId,
            origin = appIdentity
        )
        // Movie yêu cầu WebView cung cấp danh tính ứng dụng qua Referer.
        // loadDataWithBaseURL đặt base URL này làm Referer cho IFrame Player.
        webView.loadDataWithBaseURL(
            "$appIdentity/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    LaunchedEffect(webView, song.id) {
        while (true) {
            webView.evaluateJavascript(
                "window.tunertoolsState ? window.tunertoolsState() : '';"
            ) { raw ->
                val clean = raw.orEmpty()
                    .trim()
                    .removeSurrounding("\"")
                    .replace("\\\"", "\"")
                val parts = clean.split('|')
                val ready = parts.getOrNull(0) == "1"
                val position = parts.getOrNull(1)?.toDoubleOrNull()
                val duration = parts.getOrNull(2)?.toDoubleOrNull()
                val playerState = parts.getOrNull(3)?.toIntOrNull() ?: -1
                val errorCode = parts.getOrNull(4)?.toIntOrNull() ?: 0

                controller.updateMoviePlaybackState(
                    position = ((position ?: 0.0).coerceAtLeast(0.0) * 1000.0).toLong(),
                    duration = if (duration != null && duration.isFinite() && duration > 0.0) {
                        (duration * 1000.0).toLong()
                    } else {
                        song.durationMs
                    },
                    playerState = playerState,
                    ready = ready,
                    errorCode = errorCode
                )
            }
            delay(500L)
        }
    }

    BackHandler(enabled = fullScreenView != null) {
        fullScreenCallback?.onCustomViewHidden()
        fullScreenView = null
        fullScreenCallback = null
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color.Black,
        border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.38f))
    ) {
        AndroidView(
            factory = {
                FrameLayout(it).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    addView(
                        webView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    if (fullScreenView != null) {
        Dialog(
            onDismissRequest = {
                fullScreenCallback?.onCustomViewHidden()
                fullScreenView = null
                fullScreenCallback = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = {
                        FrameLayout(it).apply {
                            setBackgroundColor(AndroidColor.BLACK)
                            (fullScreenView?.parent as? ViewGroup)?.removeView(fullScreenView)
                            fullScreenView?.let { view ->
                                addView(
                                    view,
                                    FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = {
                        fullScreenCallback?.onCustomViewHidden()
                        fullScreenView = null
                        fullScreenCallback = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Thoát toàn màn hình", tint = Color.White)
                }
            }
        }
    }
}

private fun moviePlayerHtml(videoId: String, origin: String): String {
    return """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
            <meta name="referrer" content="strict-origin-when-cross-origin" />
            <style>
                html, body, #player {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    background: #000;
                }
                iframe { width: 100% !important; height: 100% !important; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var player = null;
                var playerReady = false;
                var lastError = 0;
                var autoplayBlocked = false;

                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        width: '100%',
                        height: '100%',
                        videoId: '$videoId',
                        playerVars: {
                            autoplay: 1,
                            playsinline: 1,
                            controls: 1,
                            rel: 0,
                            fs: 1,
                            origin: '$origin',
                            widget_referrer: '$origin'
                        },
                        events: {
                            onReady: function(event) {
                                playerReady = true;
                                try { event.target.playVideo(); } catch (ignore) {}
                            },
                            onError: function(event) {
                                lastError = Number(event.data) || -1;
                            },
                            onAutoplayBlocked: function() {
                                autoplayBlocked = true;
                            }
                        }
                    });
                }

                window.tunertoolsToggle = function() {
                    if (!player || !playerReady) return 'not_ready';
                    var state = player.getPlayerState();
                    if (state === YT.PlayerState.PLAYING) {
                        player.pauseVideo();
                        return 'pause';
                    }
                    player.playVideo();
                    return 'play';
                };

                window.tunertoolsSeek = function(seconds) {
                    if (!player || !playerReady) return -1;
                    player.seekTo(Math.max(0, Number(seconds) || 0), true);
                    return player.getCurrentTime();
                };

                window.tunertoolsState = function() {
                    if (!player || !playerReady) {
                        return ['0', '0', '0', '-1', String(lastError), autoplayBlocked ? '1' : '0'].join('|');
                    }
                    var current = Number(player.getCurrentTime()) || 0;
                    var duration = Number(player.getDuration()) || 0;
                    var state = Number(player.getPlayerState());
                    return ['1', String(current), String(duration), String(state), String(lastError), autoplayBlocked ? '1' : '0'].join('|');
                };
            </script>
            <script src="https://www.youtube.com/iframe_api"></script>
        </body>
        </html>
    """.trimIndent()
}

@Composable
private fun KaraokeBackdrop(
    controller: MusicController,
    song: SongItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val shape = RoundedCornerShape(26.dp)
    val lyrics = controller.lyricsFor(song)
    val loading = controller.lyricsLoading(song)
    val error = controller.lyricsError(song)
    val lyricListState = rememberLazyListState()

    LaunchedEffect(song.id) {
        controller.loadLyrics(song)
    }

    val activeLineIndex = if (lyrics?.synced == true && lyrics.lines.isNotEmpty()) {
        lyrics.lines.indexOfLast { line ->
            val timestamp = line.timestampMs ?: Long.MAX_VALUE
            timestamp <= controller.positionMs + 120L
        }
    } else {
        -1
    }

    LaunchedEffect(activeLineIndex, lyrics?.recordId) {
        if (activeLineIndex >= 0 && lyrics?.synced == true) {
            lyricListState.animateScrollToItem((activeLineIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
    ) {
        ArtworkImage(
            song = song,
            artworkUrl = controller.artworkFor(song),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = 1.12f, scaleY = 1.12f)
                .blur(22.dp),
            cornerRadius = 26
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x99020411),
                            Color(0xDD08051A),
                            Color(0xF6050713)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.purple.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f))
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MicExternalOn,
                            contentDescription = null,
                            tint = colors.purpleBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        song.artist,
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "LRCLIB",
                    color = colors.purpleBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = colors.purpleBright
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Đang tìm lời bài hát...", color = Color.White.copy(alpha = 0.86f))
                            Text(
                                "Tìm theo tên bài hát và ca sĩ",
                                color = Color.White.copy(alpha = 0.55f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MicExternalOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.62f),
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                error,
                                color = Color.White.copy(alpha = 0.84f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = { controller.loadLyrics(song, forceRefresh = true) }) {
                                Text("Tìm lại", color = colors.purpleBright)
                            }
                        }
                    }
                }

                lyrics?.instrumental == true -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Bản nhạc không lời",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                lyrics != null && lyrics.lines.isNotEmpty() -> {
                    LazyColumn(
                        state = lyricListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 58.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(
                            items = lyrics.lines,
                            key = { index, line -> "${line.timestampMs ?: -1L}_$index" }
                        ) { index, line ->
                            val active = lyrics.synced && index == activeLineIndex
                            Text(
                                text = line.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = line.timestampMs != null) {
                                        line.timestampMs?.let(controller::seekTo)
                                    }
                                    .padding(horizontal = 4.dp, vertical = if (active) 4.dp else 0.dp),
                                color = when {
                                    active -> Color.White
                                    lyrics.synced -> Color.White.copy(alpha = 0.48f)
                                    else -> Color.White.copy(alpha = 0.88f)
                                },
                                fontSize = if (active) 20.sp else 15.sp,
                                lineHeight = if (active) 27.sp else 22.sp,
                                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chưa có dữ liệu lời bài hát",
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { controller.loadLyrics(song, forceRefresh = true) }) {
                                Text("Tìm lại trên LRCLIB", color = colors.purpleBright)
                            }
                        }
                    }
                }
            }
        }
    }
}


private enum class WifiSharePage { NONE, CHOOSER, BROWSER, APP }

@Composable
private fun WifiShareChoiceDialog(
    onBrowser: () -> Unit,
    onMeloWare: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.46f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(10.dp))
                    Text("Chia sẻ nhạc qua Wi-Fi", modifier = Modifier.weight(1f),
                        color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                Text("Chọn cách kết nối với thiết bị khác cùng mạng Wi-Fi.",
                    color = colors.textSecondary, fontSize = 12.sp)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onBrowser),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Share, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("1. Chia sẻ qua trình duyệt", color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text("Tạo đường dẫn HTTP, mở bằng Chrome, VLC hoặc máy tính.",
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = colors.textSecondary)
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onMeloWare),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.45f))
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("2. Chia sẻ qua MeloWare", color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text("Hai điện thoại cài MeloWare: một máy gửi, một máy nhận.",
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeloWareWifiAppDialog(controller: MusicController, onDismiss: () -> Unit) {
    val colors = LocalMusicColors.current
    var receiverMode by rememberSaveable { mutableStateOf(false) }
    var manualWifiUrl by rememberSaveable { mutableStateOf("") }
    var showSharedFolders by remember { mutableStateOf(false) }
    var showRemoteLibrary by remember { mutableStateOf(false) }
    val context = LocalContext.current
    DisposableEffect(receiverMode) {
        if (receiverMode) controller.startSearchingMeloWare()
        onDispose { if (receiverMode) controller.stopSearchingMeloWare() }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.5f))
        ) {
            Column(
                Modifier.padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(9.dp))
                    Text("Kết nối MeloWare", modifier = Modifier.weight(1f),
                        color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { receiverMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!receiverMode) colors.purple else colors.backgroundSoft
                        )
                    ) { Text("Máy gửi", color = if (!receiverMode) Color.White else colors.textSecondary) }
                    Button(
                        onClick = { receiverMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (receiverMode) colors.purple else colors.backgroundSoft
                        )
                    ) { Text("Máy nhận", color = if (receiverMode) Color.White else colors.textSecondary) }
                }
                if (!receiverMode) {
                    Text("Máy A có thể gửi bài đang phát và chọn thêm thư mục nhạc cho máy B duyệt.",
                        color = colors.textSecondary, fontSize = 12.sp)
                    Text(controller.currentSong?.title ?: "Chưa chọn bài hát", color = colors.textPrimary,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    // Folder sharing is OFF by default. User on A explicitly selects allowed audio folders.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.purpleBright,
                            modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Thư mục chia sẻ: ${controller.wifiSharedFolderPaths.size}",
                            modifier = Modifier.weight(1f), color = colors.textPrimary, fontSize = 12.sp)
                        TextButton(onClick = { showSharedFolders = !showSharedFolders }) {
                            Text(if (showSharedFolders) "Thu gọn" else "Chọn thư mục", color = colors.purpleBright, fontSize = 12.sp)
                        }
                    }
                    if (showSharedFolders) {
                        val folders = controller.wifiShareableFolders
                        if (folders.isEmpty()) {
                            Text("Không tìm thấy thư mục nhạc đã cấp quyền đọc. Hãy cho phép MeloWare đọc nhạc trên điện thoại và quét lại thư viện.",
                                color = colors.textSecondary, fontSize = 11.sp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Chỉ chia sẻ các file âm thanh đã chọn, không chia sẻ toàn bộ bộ nhớ.",
                                    color = colors.textSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    controller.selectAllMeloWareFolders(
                                        controller.wifiSharedFolderPaths.size < folders.size
                                    )
                                }) {
                                    Text(if (controller.wifiSharedFolderPaths.size == folders.size) "Bỏ chọn" else "Chọn hết",
                                        fontSize = 11.sp, color = colors.purpleBright)
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 148.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(folders) { _, folder ->
                                    val checked = folder.path in controller.wifiSharedFolderPaths
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            controller.setMeloWareFolderShared(folder.path, !checked)
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { controller.setMeloWareFolderShared(folder.path, it) }
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(folder.path, color = colors.textPrimary,
                                                fontSize = 11.sp, maxLines = 2,
                                                overflow = TextOverflow.Ellipsis)
                                            Text("${folder.count} bài hát", color = colors.textSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    controller.wifiHostError?.let { Text(it, color = colors.red, fontSize = 12.sp) }
                    controller.wifiLanDiscovery.error?.let { Text(it, color = colors.red, fontSize = 12.sp) }
                    if (controller.wifiHostBusy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(19.dp),
                                strokeWidth = 2.dp, color = colors.purpleBright)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang chuẩn bị nguồn nhạc...", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    if (controller.wifiAppShareEnabled && controller.wifiHostActive) {
                        Text("Đang chia sẻ · ${controller.wifiHostUrl.orEmpty()}",
                            color = colors.purpleBright, fontSize = 11.sp)
                        TextButton(onClick = {
                            controller.wifiHostUrl?.let { url ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare LAN", url))
                                controller.statusMessage = "Đã sao chép địa chỉ MeloWare"
                            }
                        }) { Text("Sao chép địa chỉ kết nối", color = colors.purpleBright) }
                        Text(if (controller.wifiLanDiscovery.isAdvertising)
                            "Các máy MeloWare cùng Wi-Fi có thể tìm thấy máy này."
                            else "Đang công bố thiết bị trên mạng...",
                            color = colors.textSecondary, fontSize = 11.sp)
                        OutlinedButton(
                            onClick = controller::stopWifiHost,
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text("Dừng chia sẻ") }
                    } else {
                        Button(
                            onClick = controller::startSharingToMeloWare,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !controller.wifiHostBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) { Icon(Icons.Rounded.Wifi, null); Spacer(Modifier.width(8.dp)); Text("Bật chia sẻ qua MeloWare") }
                    }
                } else {
                    Text("Hai máy cần kết nối cùng Wi-Fi. Chọn thiết bị để phát bài hát ngay trong MeloWare.",
                        color = colors.textSecondary, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thiết bị tìm thấy: ${controller.wifiLanDiscovery.peers.size}",
                            color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = {
                            controller.stopSearchingMeloWare()
                            controller.startSearchingMeloWare()
                        }) { Text("Quét lại", color = colors.purpleBright) }
                    }
                    if (controller.wifiLanDiscovery.isSearching && controller.wifiLanDiscovery.peers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = colors.purpleBright)
                            Spacer(Modifier.width(8.dp))
                            Text("Đang tìm điện thoại MeloWare...", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                    controller.wifiLanDiscovery.error?.let { Text(it, color = colors.red, fontSize = 12.sp) }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        itemsIndexed(controller.wifiLanDiscovery.peers) { _, peer ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.backgroundSoft,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (controller.wifiReceivingPeer == peer)
                                    colors.purpleBright else colors.border)
                            ) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = colors.purpleBright,
                                            modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(peer.name, color = colors.textPrimary, fontSize = 12.sp,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(peer.address, color = colors.textSecondary, fontSize = 10.sp)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()) {
                                        TextButton(onClick = { controller.receiveFromMeloWare(peer) },
                                            modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Nghe đồng bộ", fontSize = 11.sp)
                                        }
                                        TextButton(onClick = {
                                            controller.openMeloWareLibrary(peer)
                                            showRemoteLibrary = true
                                        }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text("Duyệt thư mục", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (controller.wifiLanDiscovery.peers.isEmpty()) {
                        OutlinedTextField(
                            value = manualWifiUrl,
                            onValueChange = { manualWifiUrl = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Hoặc dán URL từ máy A", fontSize = 12.sp) },
                            singleLine = true
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { controller.receiveFromMeloWareUrl(manualWifiUrl) },
                                enabled = manualWifiUrl.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                            ) { Text("Nghe đồng bộ", fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = {
                                    if (controller.openMeloWareLibraryUrl(manualWifiUrl)) showRemoteLibrary = true
                                },
                                enabled = manualWifiUrl.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("Duyệt thư mục", fontSize = 11.sp) }
                        }
                    }
                    controller.wifiPeerMessage?.let { Text(it, color = colors.textSecondary, fontSize = 12.sp) }
                    controller.wifiReceivingPeer?.let { peer ->
                        Text("Đang nghe từ: ${peer.name}", color = colors.purpleBright, fontSize = 12.sp)
                        TextButton(onClick = {
                            controller.openMeloWareLibrary(peer)
                            showRemoteLibrary = true
                        }) {
                            Icon(Icons.Rounded.Folder, null, tint = colors.purpleBright,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duyệt thư mục máy A", color = colors.purpleBright, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = controller::stopReceivingMeloWare,
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text("Ngắt kết nối") }
                    }
                    if (controller.wifiLanDiscovery.peers.isEmpty()) {
                        Text("Nếu không thấy thiết bị: kiểm tra cùng mạng Wi-Fi, tắt AP isolation trên router và bật Máy gửi trên điện thoại A.",
                            color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
                Text("Bài đang phát và các thư mục nhạc được máy A cho phép có thể truy cập qua Wi-Fi. Không sử dụng trên Wi-Fi công cộng.",
                    color = colors.textSecondary, fontSize = 10.sp)
            }
        }
    }
    if (showRemoteLibrary) {
        MeloWareRemoteLibraryDialog(
            controller = controller,
            onDismiss = {
                showRemoteLibrary = false
                controller.closeMeloWareLibrary()
            },
            onSongSelected = {
                showRemoteLibrary = false
                controller.closeMeloWareLibrary()
                onDismiss()
            }
        )
    }
}

/** A real in-app browser: remote folders -> tracks -> ExoPlayer on phone B. */
@Composable
private fun MeloWareRemoteLibraryDialog(
    controller: MusicController,
    onDismiss: () -> Unit,
    onSongSelected: () -> Unit
) {
    val colors = LocalMusicColors.current
    val peer = controller.wifiLibraryPeer
    val folder = controller.wifiRemoteFolderPath
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.78f),
            shape = RoundedCornerShape(23.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.48f))
        ) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Thư mục nhạc được chia sẻ", color = colors.textPrimary,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(peer?.name.orEmpty(), color = colors.textSecondary,
                            fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng",
                            tint = colors.textSecondary)
                    }
                }
                if (peer != null && folder == null) {
                    TextButton(onClick = { controller.openMeloWareLibrary(peer) }) {
                        Text("Làm mới thư mục", color = colors.purpleBright, fontSize = 12.sp)
                    }
                }
                if (folder != null) {
                    TextButton(onClick = controller::backToMeloWareFolders) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tất cả thư mục", color = colors.purpleBright)
                    }
                    Text(folder, color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                controller.wifiRemoteError?.let {
                    Text(it, color = colors.red, fontSize = 12.sp)
                }
                if (controller.wifiRemoteLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp, color = colors.purpleBright)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang tải danh sách từ máy A...", color = colors.textSecondary,
                            fontSize = 12.sp)
                    }
                }
                if (!controller.wifiRemoteLoading && controller.wifiRemoteError == null) {
                    if (folder == null && controller.wifiRemoteFolders.isEmpty()) {
                        Text("Máy A chưa chọn thư mục nhạc để chia sẻ. Trên máy A, chọn Máy gửi → Chọn thư mục và đánh dấu những thư mục cho phép truy cập.",
                            color = colors.textSecondary, fontSize = 12.sp)
                    } else if (folder != null && controller.wifiRemoteSongs.isEmpty()) {
                        Text("Thư mục này không có bài nhạc để phát.",
                            color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (folder == null) {
                        itemsIndexed(controller.wifiRemoteFolders) { _, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    controller.loadMeloWareFolder(item.path)
                                },
                                color = colors.backgroundSoft,
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Folder, null, tint = colors.purpleBright,
                                        modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.path, fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary, maxLines = 2,
                                            overflow = TextOverflow.Ellipsis)
                                        Text("${item.count} bài hát", color = colors.textSecondary,
                                            fontSize = 11.sp)
                                    }
                                    Icon(Icons.Rounded.KeyboardArrowRight, null,
                                        tint = colors.textSecondary)
                                }
                            }
                        }
                    } else {
                        itemsIndexed(controller.wifiRemoteSongs) { index, item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    controller.playFromMeloWareFolder(index)
                                    onSongSelected()
                                },
                                color = colors.backgroundSoft,
                                shape = RoundedCornerShape(11.dp),
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.MusicNote, null, tint = colors.purpleBright,
                                        modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.title, color = colors.textPrimary, fontSize = 13.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.artist} · ${formatDuration(item.durationMs)}",
                                            color = colors.textSecondary, fontSize = 11.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Rounded.PlayArrow, null, tint = colors.purpleBright)
                                }
                            }
                        }
                        if (controller.wifiRemoteNextOffset >= 0) {
                            item {
                                TextButton(onClick = controller::loadMoreMeloWareFolder,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !controller.wifiRemoteLoading) {
                                    Text("Tải thêm bài hát", color = colors.purpleBright)
                                }
                            }
                        }
                    }
                }
                Text("Chỉ đọc file nhạc được máy A cho phép. Không thể xem ảnh, tài liệu hoặc sửa/xóa file của máy A.",
                    color = colors.textSecondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WifiHostDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val url = controller.wifiHostUrl
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.50f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colors.purple.copy(alpha = 0.16f)
                    ) {
                        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = colors.purpleBright)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Phát nhạc qua Wi-Fi", color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("Mở đường dẫn trên thiết bị cùng mạng", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }

                if (controller.wifiHostBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.purpleBright)
                        Spacer(Modifier.width(10.dp))
                        Text("Đang chuẩn bị file và máy chủ nội bộ...", color = colors.textSecondary)
                    }
                }

                controller.wifiHostError?.let { error ->
                    Text(error, color = colors.red, fontSize = 12.sp)
                }

                if (!url.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = colors.backgroundSoft,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            url,
                            modifier = Modifier.padding(13.dp),
                            color = colors.purpleBright,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        "Thiết bị nhận phải kết nối cùng Wi-Fi với điện thoại. Có thể mở URL bằng trình duyệt, VLC hoặc thiết bị âm thanh hỗ trợ URL mạng.",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MeloWare Wi-Fi URL", url))
                                controller.statusMessage = "Đã sao chép đường dẫn Wi-Fi"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Sao chép")
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                }
                                context.startActivity(Intent.createChooser(intent, "Chia sẻ đường dẫn MeloWare"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Chia sẻ")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            controller.stopWifiHost()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.StopCircle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Dừng máy chủ Wi-Fi")
                    }
                } else if (!controller.wifiHostBusy) {
                    Button(
                        onClick = controller::toggleWifiHost,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Bắt đầu phát qua Wi-Fi")
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackTitleBlock(
    controller: MusicController,
    song: SongItem
) {
    val colors = LocalMusicColors.current
    val technicalInfo = controller.audioTechnicalInfoFor(song)
    LaunchedEffect(song.id, controller.isLoading) {
        if (!controller.isLoading) controller.ensureAudioTechnicalInfo(song)
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = colors.textPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = song.artist,
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { controller.toggleFavorite(song) }) {
                Icon(
                    imageVector = if (controller.isFavorite(song)) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (controller.isFavorite(song)) colors.magenta else colors.textSecondary,
                    modifier = Modifier.size(31.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetadataChip(
                text = technicalInfo.qualityLabel,
                icon = Icons.Rounded.Equalizer,
                modifier = Modifier.weight(1f)
            )
            MetadataChip(
                text = technicalInfo.extensionLabel,
                icon = Icons.Rounded.MusicNote,
                modifier = Modifier.weight(1f)
            )
            MetadataChip(
                text = technicalInfo.sizeLabel,
                icon = Icons.Rounded.Download,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetadataChip(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.purple.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.purpleBright,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = colors.purpleBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaybackProgress(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit
) {
    val colors = LocalMusicColors.current
    var dragging by remember { mutableStateOf(false) }
    var localValue by remember { mutableStateOf(0f) }
    val safeDuration = max(1L, durationMs)
    val value = if (dragging) localValue else positionMs.toFloat().coerceIn(0f, safeDuration.toFloat())

    Column {
        Slider(
            value = value,
            onValueChange = {
                dragging = true
                localValue = it
            },
            onValueChangeFinished = {
                onSeek(localValue.toLong())
                dragging = false
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = colors.purpleBright,
                activeTrackColor = colors.purpleBright,
                inactiveTrackColor = colors.border
            ),
            modifier = Modifier.height(32.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(value.toLong()), color = colors.textSecondary, fontSize = 12.sp)
            Text(formatDuration(safeDuration), color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MainPlaybackControls(controller: MusicController) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = controller::toggleShuffle, modifier = Modifier.size(41.dp)) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Ngẫu nhiên",
                tint = if (controller.shuffleEnabled) colors.purpleBright else colors.textSecondary
            )
        }
        IconButton(onClick = controller::previous, modifier = Modifier.size(49.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Bài trước",
                tint = colors.textPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(22.dp, CircleShape)
                .background(
                    Brush.linearGradient(listOf(colors.purpleBright, colors.purple, colors.magenta)),
                    CircleShape
                )
                .clickable(onClick = controller::togglePlayback),
            contentAlignment = Alignment.Center
        ) {
            if (controller.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(35.dp)
                )
            } else {
                Icon(
                    imageVector = if (controller.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (controller.isPlaying) "Tạm dừng" else "Phát",
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        IconButton(onClick = controller::next, modifier = Modifier.size(49.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Bài tiếp theo",
                tint = colors.textPrimary,
                modifier = Modifier.size(34.dp)
            )
        }
        IconButton(onClick = controller::cycleRepeat, modifier = Modifier.size(46.dp)) {
            Icon(
                imageVector = if (controller.repeatSetting == RepeatSetting.ONE) {
                    Icons.Rounded.RepeatOne
                } else {
                    Icons.Rounded.Repeat
                },
                contentDescription = "Lặp lại",
                tint = if (controller.repeatSetting == RepeatSetting.OFF) colors.textSecondary else colors.purpleBright
            )
        }
    }
}

@Composable
private fun PlayerActions(
    controller: MusicController,
    song: SongItem,
    onEqualizer: () -> Unit,
    onTimer: () -> Unit,
    onQueue: () -> Unit,
    queueVisible: Boolean
) {
    val isMovie = song.source == SongSource.MOVIE

    // Khong cuon ngang: 5 nut cho nhac, 4 nut cho Movie.
    // Gioi han chieu rong de cac nut gon va sat nhau tren man hinh lon,
    // nhung van tu co theo be rong thuc te cua dien thoai nho.
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.widthIn(max = if (isMovie) 276.dp else 325.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!isMovie) {
                PlayerAction(
                    icon = Icons.Rounded.Equalizer,
                    label = "Equalizer",
                    onClick = onEqualizer,
                    modifier = Modifier.weight(1f)
                )
            }
            PlayerAction(
                icon = Icons.Rounded.PlaylistPlay,
                label = "Danh sách",
                onClick = onQueue,
                modifier = Modifier.weight(1f),
                active = queueVisible
            )
            PlayerAction(
                icon = Icons.Rounded.Timer,
                label = "Hẹn giờ",
                onClick = onTimer,
                modifier = Modifier.weight(1f)
            )
            if (!isMovie) {
                PlayerAction(
                    icon = Icons.Rounded.Download,
                    label = "Tải về",
                    onClick = { controller.downloadSongForOffline(song) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PlayerAction(
                    icon = Icons.Rounded.Tv,
                    label = "Movie",
                    onClick = controller::openCurrentMovieExternally,
                    modifier = Modifier.weight(1f)
                )
            }
            PlayerAction(
                icon = Icons.Rounded.Share,
                label = "Chia sẻ",
                onClick = controller::shareCurrentSong,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PlayerAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    val colors = LocalMusicColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 1.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(42.dp),
            shape = RoundedCornerShape(13.dp),
            color = if (active) colors.purple.copy(alpha = 0.25f) else colors.cardRaised,
            border = BorderStroke(1.dp, if (active) colors.purpleBright else colors.border)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = if (active) colors.purpleBright else colors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = colors.textPrimary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QueuePanel(
    controller: MusicController,
    onClose: () -> Unit,
    onTools: (SongItem) -> Unit
) {
    val colors = LocalMusicColors.current
    val queue = controller.queueFromCurrent()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = colors.card.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.46f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Danh sách phát",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text("${queue.size} bài", color = colors.textSecondary, fontSize = 12.sp)
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Ẩn danh sách phát", tint = colors.textSecondary)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (queue.isEmpty()) {
                Text("Danh sách đang trống", color = colors.textSecondary)
            } else {
                queue.take(6).forEachIndexed { offset, item ->
                    val absoluteIndex = controller.currentIndex + offset
                    androidx.compose.runtime.LaunchedEffect(item.id) { controller.ensureArtwork(item) }
                    SongRow(
                        song = item,
                        artworkUrl = controller.artworkFor(item),
                        isCurrent = offset == 0,
                        isFavorite = controller.isFavorite(item),
                        onClick = { controller.playQueueIndex(absoluteIndex) },
                        onFavorite = { controller.toggleFavorite(item) },
                        onMore = if (controller.canUseLocalAudioTools(item)) {
                            { onTools(item) }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.45f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                EqualizerPanel(
                    controller = controller,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 38.dp)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Đóng",
                        tint = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    currentMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Hẹn giờ tắt nhạc",
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                if (currentMinutes > 0) {
                    Text(
                        "Còn khoảng $currentMinutes phút",
                        color = colors.purpleBright,
                        fontSize = 13.sp
                    )
                }
                listOf(15, 30, 45, 60, 90).forEach { minutes ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) },
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardRaised,
                        border = BorderStroke(1.dp, colors.borderSoft)
                    ) {
                        Text(
                            "$minutes phút",
                            color = colors.textPrimary,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
                TextButton(
                    onClick = { onSelect(0) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tắt hẹn giờ", color = colors.red)
                }
            }
        }
    }
}

// ==================== EQUALIZER GỐC ĐÃ TÍCH HỢP ====================
// Lớp hỗ trợ đa ngôn ngữ cục bộ cho EqualizerPanel
private class EqLocalStrings(val lang: String) {
    fun custom() = when(lang) { "en" -> "Custom"; "zh" -> "自定义"; "ja" -> "カスタム"; "ko" -> "맞춤"; "es" -> "Personalizado"; "fr" -> "Personnalisé"; "ru" -> "Польз."; else -> "Tùy chỉnh" }
    fun advanced() = when(lang) { "en" -> "Advanced (5 bands)"; "zh" -> "高级 (5频段)"; "ja" -> "高度 (5バンド)"; "ko" -> "고급 (5밴드)"; "es" -> "Avanzado (5 bandas)"; else -> "Nâng cao (5 băng tần)" }
    fun reset() = when(lang) { "en" -> "Reset"; "zh" -> "重置"; "ja" -> "リセット"; "ko" -> "초기화"; "es" -> "Restablecer"; "fr" -> "Réinitialiser"; else -> "Đặt lại" }
    fun advancedEq() = when(lang) { "en" -> "Advanced EQ"; "zh" -> "高级均衡器"; "ja" -> "高度なEQ"; "ko" -> "고급 EQ"; "es" -> "Ecualizador avanzado"; else -> "EQ nâng cao" }
    fun bandDesc() = when(lang) { "en" -> "5 bands · -6 dB to +6 dB"; "zh" -> "5频段 · -6 dB 至 +6 dB"; "ja" -> "5バンド · -6 dB 〜 +6 dB"; else -> "5 băng tần · -6 dB đến +6 dB" }
    fun flat() = when(lang) { "en" -> "Flat"; "zh" -> "平坦"; "ja" -> "フラット"; "ko" -> "평면"; "es" -> "Plano"; else -> "Đặt phẳng" }
    fun done() = when(lang) { "en" -> "Done"; "zh" -> "完成"; "ja" -> "完了"; "ko" -> "완료"; "es" -> "Hecho"; "fr" -> "Terminé"; else -> "Xong" }
    fun openFile() = when(lang) { "en" -> "Open file"; "zh" -> "打开文件"; "ja" -> "ファイルを開く"; "ko" -> "파일 열기"; else -> "Mở tệp" }
    fun close() = when(lang) { "en" -> "Close"; "zh" -> "关闭"; "ja" -> "閉じる"; "ko" -> "닫기"; else -> "Đóng" }
    fun errStartPlay() = when(lang) { "en" -> "Start playing music to enable EQ."; "zh" -> "开始播放音乐以启用EQ。"; else -> "Bắt đầu phát nhạc để kích hoạt EQ trên phiên âm thanh." }
    fun errNoFx() = when(lang) { "en" -> "Device does not provide AudioEffect."; "zh" -> "设备不提供AudioEffect。"; else -> "Thiết bị này chưa cung cấp AudioEffect cho phiên phát hiện tại." }
}

private data class EqPreset(
    val name: String,
    val bass: Int,
    val mid: Int,
    val treble: Int,
    val icon: ImageVector
)

@Composable
internal fun EqualizerPanel(
    controller: MusicController,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMusicColors.current
    val appLanguage = LocalAppLanguage.current
    val eqStrings = remember(appLanguage) { EqLocalStrings(appLanguage) }

    val eqPresets = remember(appLanguage) {
        listOf(
            EqPreset(eqStrings.custom(), 4, 2, 3, Icons.Rounded.Tune),
            EqPreset("Normal", 0, 0, 0, Icons.Rounded.Remove),
            EqPreset("Bass Boost", 6, 1, 5, Icons.Rounded.Speaker),
            EqPreset("Pop", 2, 1, 3, Icons.Rounded.Stars),
            EqPreset("Rock", 4, 0, 3, Icons.Rounded.ElectricBolt),
            EqPreset("Jazz", 1, 3, 4, Icons.Rounded.Piano),
            EqPreset("Classic", 0, 2, 3, Icons.Rounded.Album),
        )
    }

    var showAdvancedDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        controller.refreshEqualizer()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(18.dp),
        color = palette.card.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = if (controller.equalizerReady) palette.purpleBright else palette.textSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                    Column {
                        Text(
                            text = "Equalizer",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = when {
                                controller.equalizerReady -> "Đang áp dụng cho phiên âm thanh ${controller.audioSessionId}"
                                controller.audioSessionId <= 0 -> eqStrings.errStartPlay()
                                else -> controller.equalizerError ?: eqStrings.errNoFx()
                            },
                            color = if (controller.equalizerReady) palette.purpleBright else palette.textMuted,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 2.dp),
            ) {
                itemsIndexed(eqPresets, key = { _, it -> it.name }) { index, preset ->
                    PresetChip(
                        label = preset.name,
                        icon = preset.icon,
                        selected = controller.equalizerSelectedPresetIndex == index,
                        onClick = {
                            controller.selectEqualizerPreset(
                                index = index,
                                bass = preset.bass,
                                mid = preset.mid,
                                treble = preset.treble
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                EqControlColumn(
                    label = "BASS",
                    value = controller.equalizerBass,
                    onValueChange = controller::updateEqualizerBass,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                EqControlColumn(
                    label = "MID",
                    value = controller.equalizerMid,
                    onValueChange = controller::updateEqualizerMid,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
                EqControlColumn(
                    label = "TREBLE",
                    value = controller.equalizerTreble,
                    onValueChange = controller::updateEqualizerTreble,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        controller.prepareAdvancedEqualizerBands()
                        showAdvancedDialog = true
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = palette.purpleBright,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(eqStrings.advanced(), color = palette.textPrimary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = controller::resetEqualizer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(eqStrings.reset(), color = palette.textPrimary, fontSize = 12.sp)
                }
            }

            controller.equalizerError?.takeIf { !controller.equalizerReady }?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error,
                        color = palette.textMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = controller::refreshEqualizer) {
                        Text("Thử lại", color = palette.purpleBright, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showAdvancedDialog) {
        AdvancedEqDialog(
            values = listOf(
                controller.equalizerBand60,
                controller.equalizerBand230,
                controller.equalizerBand910,
                controller.equalizerBand3600,
                controller.equalizerBand14000
            ),
            eqStrings = eqStrings,
            onValueChange = controller::updateAdvancedEqualizerBand,
            onFlat = controller::setAdvancedEqualizerFlat,
            onDismiss = { showAdvancedDialog = false },
        )
    }
}

@Composable
private fun PresetChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMusicColors.current
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) palette.purple else palette.cardRaised,
        border = BorderStroke(
            1.dp,
            if (selected) palette.purpleBright else palette.borderSoft,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else palette.textSecondary,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = if (selected) Color.White else palette.textPrimary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EqControlColumn(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMusicColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        VerticalEqFader(
            label = label,
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(60.dp)
                .weight(1f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        RotaryKnob(
            label = label,
            value = value,
            onValueChange = onValueChange,
        )
    }
}

@Composable
private fun VerticalEqFader(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMusicColors.current
    val density = LocalDensity.current
    var heightPx by remember { mutableFloatStateOf(1f) }
    val topPaddingPx = with(density) { 18.dp.toPx() }
    val bottomPaddingPx = with(density) { 18.dp.toPx() }

    fun valueForY(y: Float): Int {
        val trackHeight = max(1f, heightPx - topPaddingPx - bottomPaddingPx)
        val normalizedFromTop = ((y - topPaddingPx) / trackHeight).coerceIn(0f, 1f)
        return (6f - normalizedFromTop * 12f).roundToInt().coerceIn(-6, 6)
    }

    Box(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.toFloat() }
            .semantics {
                contentDescription = "$label ${formatDb(value)} dB"
            }
            .pointerInput(heightPx, onValueChange) {
                detectTapGestures { offset ->
                    onValueChange(valueForY(offset.y))
                }
            }
            .pointerInput(heightPx, onValueChange, value) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        onValueChange(valueForY(offset.y))
                    },
                    onVerticalDrag = { change, _ ->
                        onValueChange(valueForY(change.position.y))
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val x = size.width * 0.66f
            val top = 18.dp.toPx()
            val bottom = size.height - 18.dp.toPx()
            val centerY = (top + bottom) / 2f
            val normalized = (value + 6f) / 12f
            val thumbY = bottom - normalized * (bottom - top)

            drawLine(
                color = palette.border,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(palette.purpleBright, palette.blue),
                    startY = top,
                    endY = bottom,
                ),
                start = Offset(x, thumbY),
                end = Offset(x, bottom),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )

            for (tick in 0..12) {
                val y = top + (bottom - top) * (tick / 12f)
                val isMajor = tick == 0 || tick == 6 || tick == 12
                val tickHalfWidth = if (isMajor) 13.dp.toPx() else 6.dp.toPx()
                drawLine(
                    color = if (isMajor) palette.border else palette.borderSoft,
                    start = Offset(x - tickHalfWidth, y),
                    end = Offset(x + tickHalfWidth, y),
                    strokeWidth = if (isMajor) 1.2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            drawLine(
                color = palette.border,
                start = Offset(size.width * 0.22f, centerY),
                end = Offset(size.width * 0.96f, centerY),
                strokeWidth = 1.dp.toPx(),
            )

            drawCircle(
                color = palette.purple.copy(alpha = 0.18f),
                radius = 14.dp.toPx(),
                center = Offset(x, thumbY),
            )
            drawCircle(
                color = Color.White,
                radius = 9.dp.toPx(),
                center = Offset(x, thumbY),
            )
            drawCircle(
                color = if(palette.isDark) Color(0xFFD8DFEA) else Color.Gray,
                radius = 9.dp.toPx(),
                center = Offset(x, thumbY),
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        Text(
            text = "+6dB",
            color = palette.textSecondary,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            text = "0dB",
            color = palette.textPrimary,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        Text(
            text = "-6dB",
            color = palette.textSecondary,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun RotaryKnob(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val palette = LocalMusicColors.current
    Box(
        modifier = Modifier
            .size(68.dp)
            .semantics {
                contentDescription = "Núm $label ${formatDb(value)} dB"
            }
            .pointerInput(value, onValueChange) {
                var accumulatedDrag = 0f

                detectDragGestures { change, dragAmount ->
                    val delta = (dragAmount.x - dragAmount.y) / 12f
                    accumulatedDrag += delta

                    val intDelta = accumulatedDrag.roundToInt()
                    if (intDelta != 0) {
                        onValueChange((value + intDelta).coerceIn(-6, 6))
                        accumulatedDrag -= intDelta
                    }
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(68.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.42f
            val startAngle = 135f
            val totalSweep = 270f
            val normalized = (value + 6f) / 12f
            val activeSweep = totalSweep * normalized

            for (tick in 0..24) {
                val angle = startAngle + totalSweep * (tick / 24f)
                val radians = angle * PI.toFloat() / 180f
                val outer = radius * 1.22f
                val inner = radius * if (tick % 4 == 0) 1.06f else 1.11f
                val start = Offset(
                    x = center.x + cos(radians) * inner,
                    y = center.y + sin(radians) * inner,
                )
                val end = Offset(
                    x = center.x + cos(radians) * outer,
                    y = center.y + sin(radians) * outer,
                )
                drawLine(
                    color = if (tick / 24f <= normalized) {
                        palette.purpleBright
                    } else {
                        palette.textMuted
                    },
                    start = start,
                    end = end,
                    strokeWidth = if (tick % 4 == 0) 2.dp.toPx() else 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            drawArc(
                color = palette.border,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(palette.purple, palette.blue, palette.purple),
                    center = center,
                ),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = if(palette.isDark) listOf(Color(0xFF172437), Color(0xFF07111E)) else listOf(palette.backgroundSoft, palette.background),
                    center = center,
                    radius = radius,
                ),
                radius = radius * 0.88f,
                center = center,
            )
            drawCircle(
                color = palette.textMuted,
                radius = radius * 0.88f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            val indicatorAngle = (startAngle + activeSweep) * PI.toFloat() / 180f
            val indicatorCenter = Offset(
                x = center.x + cos(indicatorAngle) * radius * 0.64f,
                y = center.y + sin(indicatorAngle) * radius * 0.64f,
            )
            drawCircle(
                color = if(palette.isDark) Color.White else palette.purpleBright,
                radius = 2.3.dp.toPx(),
                center = indicatorCenter,
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatDb(value),
                color = palette.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AdvancedEqDialog(
    values: List<Int>,
    eqStrings: EqLocalStrings,
    onValueChange: (index: Int, value: Int) -> Unit,
    onFlat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalMusicColors.current
    val labels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
            shadowElevation = 20.dp,
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = palette.purpleBright,
                        )
                        Column {
                            Text(
                                text = eqStrings.advancedEq(),
                                color = palette.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = eqStrings.bandDesc(),
                                color = palette.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = eqStrings.close(),
                            tint = palette.textSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                labels.forEachIndexed { index, label ->
                    AdvancedBandRow(
                        label = label,
                        value = values.getOrElse(index) { 0 },
                        onValueChange = { onValueChange(index, it) },
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onFlat) {
                        Text(
                            text = eqStrings.flat(),
                            color = palette.textSecondary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.purple,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(eqStrings.done())
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvancedBandRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val palette = LocalMusicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 12.sp,
            modifier = Modifier.width(58.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(-6, 6)) },
            valueRange = -6f..6f,
            steps = 11,
            colors = SliderDefaults.colors(
                thumbColor = if(palette.isDark) Color.White else palette.purple,
                activeTrackColor = palette.purple,
                inactiveTrackColor = palette.border,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${formatDb(value)} dB",
            color = palette.purpleBright,
            fontSize = 11.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
    }
}

private fun formatDb(value: Int): String = if (value > 0) "+$value" else value.toString()
