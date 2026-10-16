package duc_phi.music

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Base64
import android.net.Uri
import android.speech.RecognizerIntent
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.Toast
import android.webkit.WebResourceError
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Phonelink
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONTokener
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.util.Locale
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@Composable
internal fun NowPlayingScreen(controller: MusicController, onOpenPremium: () -> Unit) {
    val colors = LocalMusicColors.current
    val song = controller.currentSong
    var showEqualizer by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var actionSong by remember { mutableStateOf<SongItem?>(null) }
    var karaokeMode by rememberSaveable(song?.id) { mutableStateOf(false) }
    var wifiSharePage by remember { mutableStateOf(WifiSharePage.NONE) }
    var showCastScanPanel by remember { mutableStateOf(false) }
    var showArtworkOptions by remember { mutableStateOf(false) }
    var googleArtworkFor by remember { mutableStateOf<SongItem?>(null) }
    var artworkPickerSong by remember { mutableStateOf<SongItem?>(null) }
    // Do not save the selected image until the user has adjusted its square crop.
    var pendingArtworkCrop by remember { mutableStateOf<Pair<SongItem, Uri>?>(null) }
    val artworkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { selectedUri ->
        val target = artworkPickerSong
        artworkPickerSong = null
        if (selectedUri != null && target != null) {
            pendingArtworkCrop = target to selectedUri
        }
    }
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
                    if (colors.isDark) {
                        listOf(colors.background, Color(0xFF0A0B22), colors.background)
                    } else {
                        listOf(colors.background, colors.backgroundSoft, colors.background)
                    }
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
                    onBack = controller::goBackFromPlayer,
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
                    song = song,
                    onEditArtwork = { showArtworkOptions = true },
                    karaokeActive = karaokeMode,
                    onLyrics = { karaokeMode = !karaokeMode }
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

    if (showArtworkOptions) {
        ArtworkOptionsDialog(
            customArtworkSelected = controller.hasCustomArtwork(song),
            onChoosePhoto = {
                showArtworkOptions = false
                artworkPickerSong = song
                artworkPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSearchGoogle = {
                showArtworkOptions = false
                googleArtworkFor = song
            },
            onRestoreAutomatic = {
                controller.restoreAutomaticArtwork(song)
                showArtworkOptions = false
            },
            onDismiss = { showArtworkOptions = false }
        )
    }

    googleArtworkFor?.let { target ->
        GoogleCoverSearchDialog(
            initialQuery = listOf(target.title, target.artist)
                .filter { it.isNotBlank() && !it.equals("Unknown Artist", ignoreCase = true) }
                .joinToString(" ").trim(),
            onImageChosen = { downloadedUri ->
                googleArtworkFor = null
                pendingArtworkCrop = target to downloadedUri
            },
            onChooseFromDevice = {
                googleArtworkFor = null
                artworkPickerSong = target
                artworkPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDismiss = { googleArtworkFor = null }
        )
    }

    pendingArtworkCrop?.let { selection ->
        ArtworkCropDialog(
            controller = controller,
            selectedUri = selection.second,
            onSave = { cropBounds ->
                controller.setCustomArtwork(selection.first, selection.second, cropBounds)
                pendingArtworkCrop = null
            },
            onDismiss = {
                controller.discardSearchedArtworkTemp(selection.second)
                pendingArtworkCrop = null
            }
        )
    }

    if (showEqualizer) {
        EqualizerDialog(controller = controller,
            onOpenPremium = { showEqualizer = false; onOpenPremium() },
            onDismiss = { showEqualizer = false })
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

    WifiShareDialogs(
        controller = controller,
        page = wifiSharePage,
        onNavigate = { wifiSharePage = it },
        onDismiss = { wifiSharePage = WifiSharePage.NONE }
    )

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
    onBack: () -> Unit,
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

private val lyricWordPattern = Regex("\\S+")

/**
 * Enhanced LRC uses its own <time> markers. Ordinary LRC has line timestamps
 * only, so individual words are timed approximately between consecutive lines.
 */
private fun karaokeWordCount(
    line: LyricLine,
    positionMs: Long,
    nextLineStartMs: Long?,
    trackEndMs: Long
): Int {
    val start = line.timestampMs ?: return 0
    val wordMatches = lyricWordPattern.findAll(line.text).toList()
    if (wordMatches.isEmpty() || positionMs + 120L < start) return 0

    if (line.wordTimings.size == wordMatches.size) {
        return line.wordTimings.count { it.timestampMs <= positionMs + 120L }
            .coerceIn(0, wordMatches.size)
    }

    val next = nextLineStartMs?.takeIf { it > start }
    // If the following line is close, use its actual time. Otherwise limit
    // interpolation so a long instrumental break does not crawl across a line.
    val estimatedDurationMs = (wordMatches.size * 520L).coerceIn(2_200L, 9_000L)
    val endByLine = if (next != null && next - start <= 12_000L) next else start + estimatedDurationMs
    val endByTrack = trackEndMs.takeIf { it > start } ?: Long.MAX_VALUE
    val lineEnd = minOf(endByLine, endByTrack)
    val duration = (lineEnd - start).coerceAtLeast(500L)
    val progress = ((positionMs - start).coerceAtLeast(0L).toDouble() / duration)
        .coerceIn(0.0, 1.0)
    return (1 + (progress * wordMatches.size).toInt()).coerceIn(0, wordMatches.size)
}

private fun karaokeColoredWords(
    line: LyricLine,
    highlightedCount: Int,
    highlightColor: Color,
    remainingColor: Color
): AnnotatedString = buildAnnotatedString {
    append(line.text)
    lyricWordPattern.findAll(line.text).forEachIndexed { index, match ->
        addStyle(
            SpanStyle(
                color = if (index < highlightedCount) highlightColor else remainingColor,
                fontWeight = if (index < highlightedCount) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            start = match.range.first,
            end = match.range.last + 1
        )
    }
}

/** Android's share sheet includes Zalo and Messenger when installed and able to receive text. */
private fun shareLyricsWithAndroid(context: Context, song: SongItem, lyrics: LyricsContent) {
    val plainText = lyrics.lines.joinToString("\n") { it.text.trim() }.trim()
    if (plainText.isBlank()) return
    val shareText = buildString {
        append("Lời bài hát: ").append(song.title)
        if (song.artist.isNotBlank()) append("\nCa sĩ: ").append(song.artist)
        append("\n\n").append(plainText)
        append("\n\nNguồn lời: ").append(lyrics.sourceName)
        append(" · Chia sẻ từ MeloWare")
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Lời bài hát - ${song.title}")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    val chooser = Intent.createChooser(shareIntent, "Chia sẻ lời bài hát qua Zalo, Messenger...")
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }.onFailure {
        Toast.makeText(context, "Không mở được bảng chia sẻ lời bài hát.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun KaraokeBackdrop(
    controller: MusicController,
    song: SongItem,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)
    val lyrics = controller.lyricsFor(song)
    val loading = controller.lyricsLoading(song)
    val error = controller.lyricsError(song)
    val lyricListState = rememberLazyListState()
    // A standalone opening musical symbol is often an LRC intro marker, not a lyric.
    // Skip that one decorative row to use the limited karaoke area for actual words.
    // Original indices/timestamps remain intact for word highlighting and seeking.
    val hideIntroMarker = lyrics?.lines?.takeIf { it.size > 1 }?.firstOrNull()
        ?.text?.trim()?.let { marker ->
            marker.isNotEmpty() && marker.all { it in "♪♫♬♩" }
        } == true
    val lyricStartIndex = if (hideIntroMarker) 1 else 0
    val displayedLyrics = lyrics?.lines?.drop(lyricStartIndex).orEmpty()
    var showLyricsInfo by remember(song.id) { mutableStateOf(false) }
    // Keep the small LRC status and share/info actions out of the lyric layout
    // until the user taps the backdrop. The overlay occupies no lyric height.
    var showLyricsControls by remember(song.id) { mutableStateOf(false) }
    val lyricPrimary = if (colors.isDark) Color.White else colors.textPrimary
    val lyricSecondary = if (colors.isDark) Color.White.copy(alpha = 0.70f)
        else colors.textSecondary.copy(alpha = 0.92f)
    val lyricTertiary = if (colors.isDark) Color.White.copy(alpha = 0.55f)
        else colors.textSecondary.copy(alpha = 0.76f)
    val lyricWarning = if (colors.isDark) Color(0xFFFFC06A) else Color(0xFF9A4A00)
    val timedLineCount = lyrics?.lines?.count { it.timestampMs != null } ?: 0
    val enhancedLineCount = lyrics?.lines?.count { it.wordTimings.isNotEmpty() } ?: 0
    val trackDurationMs = when {
        controller.durationMs > 1_000L -> controller.durationMs
        song.durationMs > 1_000L -> song.durationMs
        else -> 0L
    }
    val durationMismatch = lyrics?.synced == true && trackDurationMs > 20_000L &&
            lyrics.durationMs > 20_000L &&
            kotlin.math.abs(trackDurationMs - lyrics.durationMs) > 15_000L
    val lyricsStatus = when {
        loading -> "Đang tải lời..."
        lyrics?.instrumental == true -> "Bản nhạc không lời"
        lyrics?.synced == true -> "LRC đồng bộ · $timedLineCount câu"
        lyrics != null -> "Lời thường · ${lyrics.lines.size} câu"
        error != null -> "Không lấy được lời"
        else -> "Lời bài hát"
    }

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

    LaunchedEffect(activeLineIndex, lyrics?.recordId, lyricStartIndex) {
        if (activeLineIndex >= 0 && lyrics?.synced == true) {
            lyricListState.animateScrollToItem((activeLineIndex - lyricStartIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.border.copy(alpha = if (colors.isDark) 0.25f else 0.90f), shape)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { showLyricsControls = !showLyricsControls }
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
                        if (colors.isDark) {
                            listOf(
                                Color(0x99020411),
                                Color(0xDD08051A),
                                Color(0xF6050713)
                            )
                        } else {
                            listOf(
                                Color(0xEEF4F5FE),
                                Color(0xF7F7F8FF),
                                Color(0xFFF4F7FC)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                loading -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = colors.purpleBright
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("Đang tìm lời bài hát...", color = lyricPrimary.copy(alpha = 0.86f))
                            Text(
                                "Tìm theo tên bài hát và ca sĩ",
                                color = lyricTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                error != null -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.MicExternalOn,
                                contentDescription = null,
                                tint = lyricSecondary,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                error,
                                color = lyricPrimary.copy(alpha = 0.84f),
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
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
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
                                color = lyricPrimary,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                lyrics != null && lyrics.lines.isNotEmpty() -> {
                    LazyColumn(
                        state = lyricListState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(
                            items = displayedLyrics,
                            key = { visibleIndex, line -> "${line.timestampMs ?: -1L}_${visibleIndex + lyricStartIndex}" }
                        ) { visibleIndex, line ->
                            val index = visibleIndex + lyricStartIndex
                            val active = lyrics.synced && index == activeLineIndex
                            val highlightedCount = if (active) karaokeWordCount(
                                line = line,
                                positionMs = controller.positionMs,
                                nextLineStartMs = lyrics.lines.getOrNull(index + 1)?.timestampMs,
                                trackEndMs = when {
                                    controller.durationMs > 1_000L -> controller.durationMs
                                    song.durationMs > 1_000L -> song.durationMs
                                    else -> lyrics.durationMs
                                }
                            ) else 0
                            // Keep normal taps for seeking. A long press is handled by
                            // SelectionContainer: drag selection handles and choose
                            // Android's built-in Copy action (works for plain/eLRC).
                            // Per-line containers are more reliable in LazyColumn than
                            // wrapping the whole lazily-composed list.
                            SelectionContainer {
                                Text(
                                    text = if (active) karaokeColoredWords(
                                        line,
                                        highlightedCount,
                                        colors.purpleBright,
                                        lyricPrimary.copy(alpha = if (colors.isDark) 0.72f else 0.78f)
                                    ) else AnnotatedString(line.text),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showLyricsControls = !showLyricsControls
                                            line.timestampMs?.let(controller::seekTo)
                                        }
                                        .padding(horizontal = 4.dp, vertical = if (active) 4.dp else 0.dp),
                                    color = when {
                                        active -> lyricPrimary
                                        lyrics.synced -> lyricSecondary
                                        else -> lyricPrimary.copy(alpha = 0.88f)
                                    },
                                    fontSize = if (active) 21.sp else 16.sp,
                                    lineHeight = if (active) 28.sp else 23.sp,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chưa có dữ liệu lời bài hát",
                                color = lyricPrimary.copy(alpha = 0.88f),
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

        // Hidden by default; shown only after tapping the karaoke backdrop.
        // Drawing this over the content preserves the full-height lyric viewport.
        if (showLyricsControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp, vertical = 7.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (colors.isDark) Color(0xEE0A1021) else Color(0xF8FFFFFF))
                    .border(
                        1.dp,
                        colors.border.copy(alpha = if (colors.isDark) 0.45f else 0.90f),
                        RoundedCornerShape(22.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.purpleBright.copy(alpha = if (colors.isDark) 0.16f else 0.09f))
                        .clickable { showLyricsControls = false }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.MicExternalOn,
                        contentDescription = null,
                        tint = colors.purpleBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = lyricsStatus,
                        color = if (error == null) lyricPrimary.copy(alpha = 0.90f)
                        else lyricWarning,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { showLyricsInfo = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = if (durationMismatch) "Cảnh báo thời lượng lời không khớp" else "Thông tin lời bài hát",
                        modifier = Modifier.size(19.dp),
                        tint = if (durationMismatch) lyricWarning
                        else lyricSecondary
                    )
                }
                if (lyrics != null && lyrics.lines.isNotEmpty()) {
                    IconButton(
                        onClick = { shareLyricsWithAndroid(context, song, lyrics) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Share,
                            contentDescription = "Chia sẻ lời qua Zalo, Messenger",
                            modifier = Modifier.size(19.dp),
                            tint = colors.purpleBright
                        )
                    }
                }
            }

        }

        if (showLyricsInfo) {
            Dialog(onDismissRequest = { showLyricsInfo = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (colors.isDark) Color(0xFF0C182B) else colors.card,
                    border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.38f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Thông tin lời bài hát",
                                color = lyricPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showLyricsInfo = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Đóng",
                                    tint = lyricSecondary)
                            }
                        }
                        Text("Nguồn: ${lyrics?.sourceName ?: "LRCLIB"}",
                            color = lyricSecondary, fontSize = 12.sp)
                        if (lyrics != null && lyrics.lines.isNotEmpty()) {
                            Text(
                                "Chạm nhanh vào câu có mốc thời gian để tua nhạc; " +
                                        "nhấn giữ lời bài hát để bôi đen và chọn Sao chép.",
                                color = lyricSecondary,
                                fontSize = 12.sp
                            )
                            TextButton(
                                onClick = {
                                    val allLyrics = lyrics.lines.joinToString("\n") { it.text }.trim()
                                    if (allLyrics.isNotEmpty()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                                as ClipboardManager
                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText("Lời bài hát - ${song.title}", allLyrics)
                                        )
                                        Toast.makeText(context, "Đã sao chép toàn bộ lời bài hát", Toast.LENGTH_SHORT).show()
                                        showLyricsInfo = false
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null,
                                    modifier = Modifier.size(18.dp), tint = colors.purpleBright)
                                Spacer(Modifier.width(8.dp))
                                Text("Sao chép toàn bộ lời", color = colors.purpleBright)
                            }
                        }
                        Text(
                            text = when {
                                lyrics?.instrumental == true -> "Bản nhạc không lời."
                                lyrics?.synced == true -> "LRC đồng bộ: $timedLineCount câu có mốc thời gian."
                                lyrics != null -> "Lời thường: ${lyrics.lines.size} câu, không có mốc thời gian."
                                loading -> "Đang tải lời bài hát..."
                                else -> error ?: "Chưa có dữ liệu lời."
                            },
                            color = lyricPrimary.copy(alpha = 0.90f),
                            fontSize = 13.sp
                        )
                        if (lyrics?.synced == true) {
                            Text(
                                text = when {
                                    enhancedLineCount == lyrics.lines.size ->
                                        "Tô từng từ bằng mốc thời gian eLRC (nội suy nếu thiếu mốc)."
                                    enhancedLineCount > 0 ->
                                        "Có eLRC ở $enhancedLineCount câu; các câu còn lại dùng thời gian ước lượng."
                                    else -> "Tô từng từ ước lượng giữa các mốc thời gian theo câu."
                                },
                                color = lyricTertiary,
                                fontSize = 12.sp
                            )
                        }
                        if (durationMismatch && lyrics != null) {
                            Text(
                                "Lưu ý: lời dài ${formatDuration(lyrics.durationMs)}, " +
                                        "bản nhạc đang phát ${formatDuration(trackDurationMs)}. " +
                                        "Mốc thời gian có thể không khớp.",
                                color = lyricWarning,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}


private enum class WifiSharePage { NONE, CHOOSER, BROWSER, APP, LAPTOP, INTERNET }

/** Opens exactly the same 4 connection options as the Wi-Fi button on Now Playing. */
@Composable
internal fun MeloWareNetworkMenuDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    var page by remember { mutableStateOf(WifiSharePage.CHOOSER) }
    WifiShareDialogs(
        controller = controller,
        page = page,
        onNavigate = { page = it },
        onDismiss = onDismiss
    )
}

/** Shared routing: Home and Now Playing use the existing dialogs, never a duplicate UI. */
@Composable
private fun WifiShareDialogs(
    controller: MusicController,
    page: WifiSharePage,
    onNavigate: (WifiSharePage) -> Unit,
    onDismiss: () -> Unit
) {
    when (page) {
        WifiSharePage.CHOOSER -> WifiShareChoiceDialog(
            onBrowser = {
                onNavigate(WifiSharePage.BROWSER)
                if (!controller.wifiHostActive && !controller.wifiHostBusy) {
                    controller.toggleWifiHost()
                }
            },
            onMeloWare = { onNavigate(WifiSharePage.APP) },
            onLaptop = { onNavigate(WifiSharePage.LAPTOP) },
            onInternet = { onNavigate(WifiSharePage.INTERNET) },
            onDismiss = onDismiss
        )
        WifiSharePage.BROWSER -> WifiHostDialog(controller = controller, onDismiss = onDismiss)
        WifiSharePage.APP -> MeloWareWifiAppDialog(controller = controller, onDismiss = onDismiss)
        WifiSharePage.LAPTOP -> MeloWareLaptopDialog(controller = controller, onDismiss = onDismiss)
        WifiSharePage.INTERNET -> MeloWareInternetDialog(controller = controller, onDismiss = onDismiss)
        WifiSharePage.NONE -> Unit
    }
}


/** Two phones with opposite arrows for MeloWare <-> MeloWare LAN sharing. */
@Composable
internal fun TwoPhonesTransferIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(32.dp)) {
        // Draw using a normalized 36x36 grid, so the icon remains sharp on any density.
        val unit = size.minDimension / 36f
        val originX = (size.width - 36f * unit) / 2f
        val originY = (size.height - 36f * unit) / 2f
        fun p(x: Float, y: Float) = Offset(originX + x * unit, originY + y * unit)

        // Both phones are intentionally identical in size.
        fun phone(x: Float) {
            drawRoundRect(
                color = tint,
                topLeft = p(x, 6.5f),
                size = Size(10f * unit, 23f * unit),
                cornerRadius = CornerRadius(2.5f * unit),
                style = Stroke(width = 2f * unit)
            )
            drawLine(
                color = tint,
                start = p(x + 3.3f, 10f),
                end = p(x + 6.7f, 10f),
                strokeWidth = 1.4f * unit,
                cap = StrokeCap.Round
            )
            drawCircle(color = tint, radius = 0.9f * unit, center = p(x + 5f, 26.5f))
        }
        phone(0.5f)
        phone(25.5f)

        // Upper arrow points right (phone A -> phone B).
        val arrowStroke = 2.1f * unit
        fun arrowLine(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, p(x1, y1), p(x2, y2), arrowStroke, cap = StrokeCap.Round)
        }
        arrowLine(13f, 13f, 23f, 13f)
        arrowLine(19.6f, 9.8f, 23f, 13f)
        arrowLine(19.6f, 16.2f, 23f, 13f)

        // Lower arrow points left (phone B -> phone A).
        arrowLine(23f, 23f, 13f, 23f)
        arrowLine(16.4f, 19.8f, 13f, 23f)
        arrowLine(16.4f, 26.2f, 13f, 23f)
    }
}

@Composable
private fun WifiShareChoiceDialog(
    onBrowser: () -> Unit,
    onMeloWare: () -> Unit,
    onLaptop: () -> Unit,
    onInternet: () -> Unit,
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
            Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(10.dp))
                    Text(mwString("share.menu.title", "Kết nối & chia sẻ"), modifier = Modifier.weight(1f),
                        color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }
                Text(mwString("share.menu.subtitle", "Chọn cách kết nối qua Wi-Fi nội bộ hoặc Internet."),
                    color = colors.textSecondary, fontSize = 12.sp)
                // 1. Phone <-> computer: the phone creates the link, Chrome selects a folder.
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onLaptop),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.45f))
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phonelink, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mwString("share.phone_pc.title", "1. Chia sẻ điện thoại và máy tính"), color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(mwString("share.phone_pc.desc", "Mở link trên Chrome laptop, chọn thư mục để nghe hoặc tải nhạc."),
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = colors.textSecondary)
                    }
                }
                // 2. Phone <-> phone: preserve the existing MeloWare sharing logic.
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onMeloWare),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        TwoPhonesTransferIcon(
                            tint = colors.purpleBright
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mwString("share.phone_phone.title", "2. Điện thoại và điện thoại"), color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(mwString("share.phone_phone.desc", "Kết nối hai điện thoại cài MeloWare để chia sẻ nhạc."),
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = colors.textSecondary)
                    }
                }
                // 3. Link for any device: preserve the old browser HTTP share.
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onBrowser),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Link, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mwString("share.link.title", "3. Chia sẻ link (mọi thiết bị)"), color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(mwString("share.link.desc", "Mở link chia sẻ nhạc bằng trình duyệt, VLC hoặc thiết bị khác."),
                                color = colors.textSecondary, fontSize = 11.sp)
                        }
                        Icon(Icons.Rounded.KeyboardArrowRight, null, tint = colors.textSecondary)
                    }
                }
                // 4. Internet P2P: independent from the three existing LAN sharing options.
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onInternet),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Public, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(27.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(mwString("share.internet.title", "4. Kết nối qua Internet"), color = colors.textPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(3.dp))
                            Text(mwString("share.internet.desc", "Hai người dùng MeloWare ghép nối từ hai mạng khác nhau."),
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
                    Text(mwString("phone_phone.dialog.title", "Kết nối MeloWare"), modifier = Modifier.weight(1f),
                        color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    ShareGuideButton(ShareGuideType.PHONE_PHONE)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { receiverMode = false },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!receiverMode) colors.purple else colors.backgroundSoft
                        )
                    ) { Text(mwString("phone_phone.sender", "Máy gửi"), color = if (!receiverMode) Color.White else colors.textSecondary) }
                    Button(
                        onClick = { receiverMode = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (receiverMode) colors.purple else colors.backgroundSoft
                        )
                    ) { Text(mwString("phone_phone.receiver", "Máy nhận"), color = if (receiverMode) Color.White else colors.textSecondary) }
                }
                if (!receiverMode) {
                    Text(mwString("phone_phone.sender_hint", "Máy A có thể gửi bài đang phát và chọn thêm thư mục nhạc cho máy B duyệt."),
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
                        }) { Text(mwString("phone_phone.copy_address", "Sao chép địa chỉ kết nối"), color = colors.purpleBright) }
                        Text(if (controller.wifiLanDiscovery.isAdvertising)
                            "Các máy MeloWare cùng Wi-Fi có thể tìm thấy máy này."
                        else "Đang công bố thiết bị trên mạng...",
                            color = colors.textSecondary, fontSize = 11.sp)
                        OutlinedButton(
                            onClick = controller::stopWifiHost,
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text(mwString("phone_phone.stop_share", "Dừng chia sẻ")) }
                    } else {
                        Button(
                            onClick = controller::startSharingToMeloWare,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !controller.wifiHostBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) { Icon(Icons.Rounded.Wifi, null); Spacer(Modifier.width(8.dp)); Text(mwString("phone_phone.start_share", "Bật chia sẻ qua MeloWare")) }
                    }
                } else {
                    Text(mwString("phone_phone.receiver_hint", "Hai máy cần kết nối cùng Wi-Fi. Chọn thiết bị để phát bài hát ngay trong MeloWare."),
                        color = colors.textSecondary, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Thiết bị tìm thấy: ${controller.wifiLanDiscovery.peers.size}",
                            color = colors.textPrimary, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = {
                            controller.stopSearchingMeloWare()
                            controller.startSearchingMeloWare()
                        }) { Text(mwString("phone_phone.rescan", "Quét lại"), color = colors.purpleBright) }
                    }
                    if (controller.wifiLanDiscovery.isSearching && controller.wifiLanDiscovery.peers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = colors.purpleBright)
                            Spacer(Modifier.width(8.dp))
                            Text(mwString("phone_phone.searching", "Đang tìm điện thoại MeloWare..."), color = colors.textSecondary, fontSize = 12.sp)
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
                                            Text(mwString("phone_phone.sync", "Nghe đồng bộ"), fontSize = 11.sp)
                                        }
                                        TextButton(onClick = {
                                            controller.openMeloWareLibrary(peer)
                                            showRemoteLibrary = true
                                        }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(3.dp))
                                            Text(mwString("phone_phone.browse", "Duyệt thư mục"), fontSize = 11.sp)
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
                            label = { Text(mwString("phone_phone.manual_url", "Hoặc dán URL từ máy A"), fontSize = 12.sp) },
                            singleLine = true
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { controller.receiveFromMeloWareUrl(manualWifiUrl) },
                                enabled = manualWifiUrl.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                            ) { Text(mwString("phone_phone.sync", "Nghe đồng bộ"), fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = {
                                    if (controller.openMeloWareLibraryUrl(manualWifiUrl)) showRemoteLibrary = true
                                },
                                enabled = manualWifiUrl.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text(mwString("phone_phone.browse", "Duyệt thư mục"), fontSize = 11.sp) }
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
                        ) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text(mwString("common.disconnect", "Ngắt kết nối")) }
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
                        Text(mwString("link.dialog.title", "Phát nhạc qua Wi-Fi"), color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(mwString("link.dialog.subtitle", "Mở đường dẫn trên thiết bị cùng mạng"), color = colors.textSecondary, fontSize = 11.sp)
                    }
                    ShareGuideButton(ShareGuideType.LINK_ANY_DEVICE)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }

                if (controller.wifiHostBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.purpleBright)
                        Spacer(Modifier.width(10.dp))
                        Text(mwString("link.preparing", "Đang chuẩn bị file và máy chủ nội bộ..."), color = colors.textSecondary)
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
                        mwString("link.device_note", "Thiết bị nhận phải kết nối cùng Wi-Fi với điện thoại. Có thể mở URL bằng trình duyệt, VLC hoặc thiết bị âm thanh hỗ trợ URL mạng."),
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
                            Text(mwString("common.copy", "Sao chép"))
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
                            Text(mwString("common.share", "Chia sẻ"))
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
                        Text(mwString("link.stop_server", "Dừng máy chủ Wi-Fi"))
                    }
                } else if (!controller.wifiHostBusy) {
                    Button(
                        onClick = controller::toggleWifiHost,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.Wifi, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(mwString("link.start_server", "Bắt đầu phát qua Wi-Fi"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkOptionsDialog(
    customArtworkSelected: Boolean,
    onChoosePhoto: () -> Unit,
    onSearchGoogle: () -> Unit,
    onRestoreAutomatic: () -> Unit,
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
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mwString("cover.dialog_title", "Tùy chỉnh ảnh bìa"),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(colors.backgroundSoft)
                        .clickable(onClick = onChoosePhoto)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        tint = colors.purpleBright,
                        modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mwString("cover.choose", "Chọn ảnh từ điện thoại"),
                            color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(mwString("cover.choose_desc", "Lưu riêng cho bài hát này"),
                            color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                // Search results stay inside MeloWave in a WebView; no paid image API.
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(colors.backgroundSoft)
                        .clickable(onClick = onSearchGoogle)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.purpleBright,
                        modifier = Modifier.size(28.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mwString("cover.google_option", "Tìm ảnh trực tuyến"),
                            color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(mwString("cover.google_option_desc", "Tìm và chọn ảnh ngay trong MeloWave"),
                            color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
                if (customArtworkSelected) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(colors.backgroundSoft)
                            .clickable(onClick = onRestoreAutomatic)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null,
                            tint = colors.purpleBright, modifier = Modifier.size(28.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mwString("cover.restore", "Khôi phục ảnh tự động"),
                                color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(mwString("cover.restore_desc", "Dùng lại ảnh gốc hoặc ảnh online"),
                                color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
                Text(
                    mwString("cover.note", "Ảnh tùy chỉnh chỉ lưu trong MeloWave, không sửa file nhạc gốc."),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/**
 * The existing Google Images page remains the source of results, but it is not
 * displayed. A limited set of image URLs from the rendered page is shown in a
 * native Compose grid. Google does not provide this as a supported public API:
 * changes to the page, consent screens and network restrictions can break it.
 * No extra account, PHP endpoint or paid search service is used.
 */
private fun googleCoverSearchUrl(query: String): String = Uri.Builder()
    .scheme("https")
    .authority("www.google.com")
    .appendPath("search")
    .appendQueryParameter("tbm", "isch")
    .appendQueryParameter("q", query.trim())
    .build().toString()

private data class MwCoverSearchImage(val thumbnail: String, val original: String?)

/**
 * Inspect only thumbnails already rendered in the user's WebView session.
 * Use dimensions and URL checks instead of Google-specific class selectors;
 * this is still best-effort because Google can change its DOM at any time.
 * No JavaScript-to-Android bridge is exposed to untrusted web content.
 */
private val mwCoverImageExtractionScript = """
    (function () {
        var result = [];
        var seen = {};
        var photos = document.querySelectorAll('img');
        for (var i = 0; i < photos.length && result.length < 120; i++) {
            var img = photos[i];
            if (img.closest('header, nav, footer')) continue;
            var box = img.getBoundingClientRect();
            var width = Math.max(img.naturalWidth || 0, box.width || 0);
            var height = Math.max(img.naturalHeight || 0, box.height || 0);
            if (width < 72 || height < 72) continue;
            var candidates = [img.currentSrc, img.src, img.getAttribute('data-src')];
            var thumb = '';
            for (var j = 0; j < candidates.length; j++) {
                var link = candidates[j] || '';
                if (link.indexOf('//') === 0) link = 'https:' + link;
                if (link.indexOf('https://') === 0) { thumb = link; break; }
            }
            if (!thumb || thumb.length > 3500 || seen[thumb]) continue;
            if (/googlelogo|branding|favicon|\/logos\//i.test(thumb)) continue;
            var original = '';
            var anchor = img.closest('a[href]');
            if (anchor) {
                try {
                    var u = new URL(anchor.href, location.href);
                    var found = u.searchParams.get('imgurl') || u.searchParams.get('img_url');
                    if (found && found.indexOf('https://') === 0 && found.length < 3500) {
                        original = found;
                    }
                } catch (error) {}
            }
            seen[thumb] = 1;
            result.push([thumb, original]);
        }
        return result;
    })()
""".trimIndent()

private fun mwParseCoverImageResults(response: String): List<MwCoverSearchImage> {
    return try {
        val parsed = JSONTokener(response).nextValue()
        val photos = when (parsed) {
            is JSONArray -> parsed
            is String -> JSONArray(parsed)
            else -> return emptyList()
        }
        buildList {
            for (i in 0 until min(photos.length(), 120)) {
                val pair = photos.optJSONArray(i) ?: continue
                val thumbnail = pair.optString(0).trim()
                if (!thumbnail.startsWith("https://") || thumbnail.length > 3500) continue
                val original = pair.optString(1).trim().takeIf {
                    it.startsWith("https://") && it.length < 3500 && it != thumbnail
                }
                add(MwCoverSearchImage(thumbnail, original))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun GoogleCoverSearchDialog(
    initialQuery: String,
    onImageChosen: (Uri) -> Unit,
    onChooseFromDevice: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalMusicColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var pageLoading by remember { mutableStateOf(true) }
    var pageRevision by remember { mutableIntStateOf(0) }
    var downloading by remember { mutableStateOf(false) }
    var pageError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<MwCoverSearchImage?>(null) }
    var finishedScanning by remember { mutableStateOf(false) }
    val imageResults = remember { mutableStateListOf<MwCoverSearchImage>() }
    val resultGridState = rememberLazyGridState()
    val downloadFailedMessage = mwString(
        "cover.google_download_failed", "Không tải được ảnh từ trang này."
    )

    fun captureRenderedImages(webView: WebView) {
        webView.evaluateJavascript(mwCoverImageExtractionScript) { result ->
            val known = imageResults.mapTo(HashSet<String>()) { it.thumbnail }
            for (photo in mwParseCoverImageResults(result)) {
                if (imageResults.size >= 120) break
                if (known.add(photo.thumbnail)) imageResults.add(photo)
            }
        }
    }

    val imageWebView = remember(context) {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.WHITE)
            settings.apply {
                javaScriptEnabled = true // Google Images uses JS for lazy-loaded thumbnails.
                domStorageEnabled = true
                loadsImagesAutomatically = true
                allowFileAccess = false
                allowContentAccess = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) safeBrowsingEnabled = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url ?: return true
                    // The hidden page never needs to navigate to unrelated sites.
                    val host = url.host.orEmpty().lowercase(Locale.ROOT)
                    return url.scheme != "https" ||
                        !(host == "google.com" || host.endsWith(".google.com") ||
                            host == "google.com.vn" || host.endsWith(".google.com.vn") ||
                            host == "google.co.jp" || host.endsWith(".google.co.jp"))
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    pageLoading = true
                    pageError = null
                    finishedScanning = false
                    imageResults.clear()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    pageLoading = false
                    pageRevision++
                    view?.let { captureRenderedImages(it) }
                }

                override fun onReceivedError(
                    view: WebView?, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        pageLoading = false
                        finishedScanning = true
                        pageError = error?.description?.toString()
                            ?: "Không thể mở trang tìm kiếm ảnh."
                    }
                }
            }
        }
    }

    DisposableEffect(imageWebView) {
        imageWebView.loadUrl(googleCoverSearchUrl(initialQuery))
        onDispose {
            runCatching { imageWebView.stopLoading() }
            runCatching { imageWebView.loadUrl("about:blank") }
            runCatching { imageWebView.destroy() }
        }
    }

    // Google loads results in several waves. Poll a few times, scrolling the
    // *hidden* page to reveal thumbnails while keeping Compose UI unchanged.
    LaunchedEffect(imageWebView, pageRevision) {
        if (pageRevision <= 0) return@LaunchedEffect
        resultGridState.scrollToItem(0)
        finishedScanning = false
        repeat(12) { pass ->
            captureRenderedImages(imageWebView)
            if (pass in listOf(2, 5, 8)) {
                imageWebView.evaluateJavascript(
                    "window.scrollBy(0, Math.max(window.innerHeight * 0.8, 500));", null
                )
            }
            delay(650)
        }
        finishedScanning = true
    }

    // Continue retrieving the same search results as the user scrolls the grid.
    LaunchedEffect(resultGridState.firstVisibleItemIndex, imageResults.size, pageRevision) {
        if (pageRevision > 0 && imageResults.size in 9..119 &&
            resultGridState.firstVisibleItemIndex + 15 >= imageResults.size
        ) {
            imageWebView.evaluateJavascript(
                "window.scrollBy(0, Math.max(window.innerHeight, 600));", null
            )
            delay(400)
            captureRenderedImages(imageWebView)
        }
    }

    LaunchedEffect(selectedImage) {
        val choice = selectedImage ?: return@LaunchedEffect
        downloading = true
        imageError = null
        try {
            var localImage: Uri? = null
            var lastError: Exception? = null
            for (url in listOfNotNull(choice.original, choice.thumbnail).distinct()) {
                try {
                    localImage = downloadGoogleCoverImage(context.applicationContext, url)
                    break
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    lastError = failure
                }
            }
            if (localImage == null) throw lastError ?: IOException(downloadFailedMessage)
            onImageChosen(localImage)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            imageError = "$downloadFailedMessage ${failure.localizedMessage.orEmpty()}".trim()
        } finally {
            downloading = false
            selectedImage = null
        }
    }

    // Voice results are passed directly to the search, rather than relying on
    // Compose state to update between the speech callback and WebView load.
    fun submitQuery(searchText: String) {
        val keyword = searchText.trim()
        if (keyword.isBlank() || downloading) return
        query = keyword
        imageError = null
        pageError = null
        pageLoading = true
        finishedScanning = false
        imageResults.clear()
        imageWebView.loadUrl(googleCoverSearchUrl(keyword))
        focusManager.clearFocus()
        keyboard?.hide()
    }

    // Uses Android's own voice-recognition activity, as on the MeloWave home
    // screen. No RECORD_AUDIO permission, external API, or new dependency.
    val coverVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (spokenText.isNotBlank()) submitQuery(spokenText)
        }
    }

    val voicePrompt = mwString(
        "cover.voice_prompt", "Nói tên bài hát, nghệ sĩ hoặc từ khóa để tìm ảnh bìa"
    )
    val voiceUnavailable = mwString(
        "home.voice_unavailable", "Thiết bị chưa có dịch vụ nhận dạng giọng nói."
    )
    fun startCoverVoiceSearch() {
        if (downloading) return
        focusManager.clearFocus()
        keyboard?.hide()
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        }
        try {
            coverVoiceLauncher.launch(voiceIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, voiceUnavailable, Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(context, voiceUnavailable, Toast.LENGTH_LONG).show()
        }
    }

    Dialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = !downloading, onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack,
                            contentDescription = mwString("common.back", "Quay lại"),
                            tint = colors.textPrimary)
                    }
                    Text(
                        mwString("cover.google_title", "Tìm ảnh bìa"),
                        modifier = Modifier.weight(1f),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss, enabled = !downloading) {
                        Icon(Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }
                // One native search field: search, editable keyword, clear,
                // and voice input. No Google page chrome is shown to the user.
                Surface(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardRaised,
                    border = BorderStroke(
                        1.dp,
                        colors.purple.copy(alpha = if (colors.isDark) 0.47f else 0.27f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(start = 6.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { submitQuery(query) },
                            enabled = query.isNotBlank() && !downloading,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = mwString("cover.google_search", "Tìm"),
                                tint = if (query.isNotBlank()) colors.purpleBright else colors.textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.weight(1f),
                            enabled = !downloading,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            ),
                            cursorBrush = SolidColor(colors.purpleBright),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { submitQuery(query) }
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (query.isBlank()) {
                                        Text(
                                            mwString("cover.google_hint", "Nhập từ khóa tìm ảnh..."),
                                            color = colors.textMuted,
                                            maxLines = 1,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (query.isNotBlank()) {
                            IconButton(
                                onClick = { query = "" },
                                enabled = !downloading,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = mwString("cover.clear_keyword", "Xóa từ khóa"),
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.purple.copy(alpha = if (colors.isDark) 0.23f else 0.12f)
                        ) {
                            IconButton(
                                onClick = ::startCoverVoiceSearch,
                                enabled = !downloading,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = mwString("home.voice_search", "Tìm kiếm bằng giọng nói"),
                                    tint = colors.purpleBright,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Keep the original Google search page as an invisible data
                    // source. Its logos, bars and text never appear in our UI.
                    AndroidView(
                        factory = {
                            (imageWebView.parent as? ViewGroup)?.removeView(imageWebView)
                            imageWebView
                        },
                        modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.01f }
                    )
                    Box(Modifier.fillMaxSize().background(colors.background)) {
                        if (imageResults.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                state = resultGridState,
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(imageResults, key = { it.thumbnail }) { photo ->
                                    Box(
                                        modifier = Modifier.aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.card)
                                            .clickable(enabled = !downloading) {
                                                selectedImage = photo
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = photo.thumbnail,
                                            contentDescription = mwString(
                                                "cover.change", "Chọn ảnh bìa"
                                            ),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (pageLoading || !finishedScanning) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(color = colors.purpleBright)
                                        Text(mwString("cover.search_loading", "Đang tìm hình ảnh..."),
                                            color = colors.textSecondary, fontSize = 13.sp)
                                    }
                                } else {
                                    Text(
                                        mwString("cover.search_empty",
                                            "Chưa lấy được kết quả. Hãy thử từ khóa khác hoặc chọn ảnh trong máy."),
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }
                            }
                        }
                        if (downloading) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = Color.White)
                                    Text(mwString("cover.google_downloading", "Đang tải ảnh..."),
                                        color = Color.White)
                                }
                            }
                        }
                    }
                }
                val issue = imageError ?: pageError
                if (issue != null) {
                    Text(issue, fontSize = 12.sp, color = colors.red)
                }
                // Fixed bottom action: prominent, thumb-friendly gallery access.
                Button(
                    onClick = onChooseFromDevice,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    enabled = !downloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.purple,
                        contentColor = Color.White,
                        disabledContainerColor = colors.cardRaised,
                        disabledContentColor = colors.textMuted
                    )
                ) {
                    Icon(
                        Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        mwString("cover.choose", "Chọn ảnh từ điện thoại"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Safely turn a user-selected WebView image URL into a file:// URI for the
 * existing square cropper. Limit download size and reject non-image responses.
 * This requires neither a Google Image Search API key nor a new server endpoint.
 */
private suspend fun downloadGoogleCoverImage(context: Context, source: String): Uri =
    withContext(Dispatchers.IO) {
        val maxBytes = 12 * 1024 * 1024
        val folder = File(context.cacheDir, "melowave_web_cover_search")
        if (!folder.isDirectory && !folder.mkdirs()) {
            throw IOException("Không tạo được thư mục tạm")
        }
        val file = File.createTempFile("cover_", ".img", folder)
        try {
            if (source.startsWith("data:image/", ignoreCase = true)) {
                val comma = source.indexOf(',')
                if (comma <= 0 || !source.substring(0, comma).contains(";base64", ignoreCase = true)) {
                    throw IOException("Ảnh dữ liệu không được hỗ trợ")
                }
                val encoded = source.substring(comma + 1)
                if (encoded.length > (maxBytes * 4 / 3 + 2048)) {
                    throw IOException("Ảnh quá lớn")
                }
                val decoded = Base64.decode(encoded, Base64.DEFAULT)
                if (decoded.size > maxBytes) throw IOException("Ảnh quá lớn")
                FileOutputStream(file).use { it.write(decoded) }
            } else {
                var imageUrl = URL(source)
                if (imageUrl.protocol != "https") throw IOException("Chỉ hỗ trợ ảnh HTTPS")
                var redirects = 0
                var done = false
                while (!done) {
                    val connection = imageUrl.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 12000
                    connection.readTimeout = 18000
                    connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/gif,image/*;q=0.8")
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MeloWave)")
                    connection.setRequestProperty("Referer", "https://www.google.com/")
                    try {
                        val status = connection.responseCode
                        if (status in 300..399) {
                            val location = connection.getHeaderField("Location")
                                ?: throw IOException("Liên kết ảnh không hợp lệ")
                            redirects++
                            if (redirects > 4) throw IOException("Liên kết chuyển hướng quá nhiều")
                            imageUrl = URL(imageUrl, location)
                            if (imageUrl.protocol != "https") {
                                throw IOException("Chuyển hướng ảnh không an toàn")
                            }
                        } else {
                            if (status !in 200..299) {
                                throw IOException("Máy chủ ảnh trả lỗi HTTP $status")
                            }
                            if (connection.contentLengthLong > maxBytes) {
                                throw IOException("Ảnh lớn hơn 12 MB")
                            }
                            val mediaType = connection.contentType.orEmpty()
                                .substringBefore(';').trim().lowercase(Locale.ROOT)
                            if (mediaType.isNotBlank() &&
                                !mediaType.startsWith("image/") &&
                                mediaType != "application/octet-stream") {
                                throw IOException("Địa chỉ không trả về tệp hình ảnh")
                            }
                            connection.inputStream.use { input ->
                                FileOutputStream(file).use { output ->
                                    val buffer = ByteArray(8192)
                                    var total = 0L
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        total += count
                                        if (total > maxBytes) throw IOException("Ảnh lớn hơn 12 MB")
                                        output.write(buffer, 0, count)
                                    }
                                }
                            }
                            done = true
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (file.length() == 0L || bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                max(bounds.outWidth, bounds.outHeight) > 20000) {
                throw IOException("Định dạng ảnh không được hỗ trợ")
            }
            Uri.fromFile(file)
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }

/**
 * Interactive square artwork cropper. The crop is calculated in normalized original-image
 * coordinates; MusicController applies the same rectangle to the decoded bitmap when saving.
 * The user's original photo and the song metadata are never modified.
 */
@Composable
private fun ArtworkCropDialog(
    controller: MusicController,
    selectedUri: Uri,
    onSave: (RectF) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    var preview by remember(selectedUri) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(selectedUri) { mutableStateOf<String?>(null) }
    var zoom by remember(selectedUri) { mutableFloatStateOf(1f) }
    var pan by remember(selectedUri) { mutableStateOf(Offset.Zero) }
    var frameSidePx by remember(selectedUri) { mutableFloatStateOf(0f) }

    LaunchedEffect(selectedUri) {
        try {
            preview = controller.loadArtworkForCrop(selectedUri)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            loadError = error.localizedMessage ?: "Không thể mở hình ảnh"
        }
    }

    // At zoom 1, ContentScale.Crop already fills the square. At higher zoom levels,
    // clamp panning so that no blank area is exposed beyond the square preview.
    fun clampPan(proposed: Offset, cropZoom: Float): Offset {
        val image = preview ?: return Offset.Zero
        val minEdge = min(image.width, image.height).toFloat().coerceAtLeast(1f)
        val maxX = (frameSidePx * (image.width / minEdge * cropZoom - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (frameSidePx * (image.height / minEdge * cropZoom - 1f) / 2f).coerceAtLeast(0f)
        return Offset(proposed.x.coerceIn(-maxX, maxX), proposed.y.coerceIn(-maxY, maxY))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mwString("cover.crop_title", "Cắt ảnh bìa"),
                        modifier = Modifier.weight(1f),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textSecondary)
                    }
                }

                val image = preview
                when {
                    image != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFF10162C))
                                .onSizeChanged { frameSidePx = it.width.toFloat() }
                                .pointerInput(image) {
                                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                                        val nextZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                        zoom = nextZoom
                                        pan = clampPan(pan + gesturePan, nextZoom)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = image.asImageBitmap(),
                                contentDescription = mwString("cover.crop_title", "Cắt ảnh bìa"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().graphicsLayer {
                                    scaleX = zoom
                                    scaleY = zoom
                                    translationX = pan.x
                                    translationY = pan.y
                                }
                            )
                            // Thin square frame to clarify what will be saved.
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                        RoundedCornerShape(15.dp))
                            )
                        }
                        Text(
                            mwString("cover.crop_hint", "Kéo ảnh để căn giữa, chụm 2 ngón để phóng to hoặc dùng thanh kéo."),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mwString("cover.crop_zoom", "Phóng to"),
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                            Slider(
                                value = zoom,
                                onValueChange = { newZoom ->
                                    zoom = newZoom
                                    pan = clampPan(pan, newZoom)
                                },
                                valueRange = 1f..4f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.purpleBright,
                                    activeTrackColor = colors.purpleBright
                                )
                            )
                            TextButton(onClick = {
                                zoom = 1f
                                pan = Offset.Zero
                            }) {
                                Text(mwString("cover.crop_reset", "Đặt lại"), color = colors.purpleBright)
                            }
                        }
                    }
                    loadError != null -> {
                        Text(loadError.orEmpty(), color = colors.red, fontSize = 14.sp)
                    }
                    else -> {
                        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.purpleBright)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(mwString("common.cancel", "Hủy"), color = colors.textPrimary)
                    }
                    Button(
                        enabled = preview != null && frameSidePx > 0f,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val bitmap = preview ?: return@Button
                            val minEdge = min(bitmap.width, bitmap.height).toFloat()
                            val side = (minEdge / zoom).coerceAtLeast(1f)
                            val scale = frameSidePx * zoom / minEdge
                            val left = ((bitmap.width - side) / 2f - pan.x / scale)
                                .coerceIn(0f, bitmap.width - side)
                            val top = ((bitmap.height - side) / 2f - pan.y / scale)
                                .coerceIn(0f, bitmap.height - side)
                            onSave(RectF(
                                left / bitmap.width,
                                top / bitmap.height,
                                (left + side) / bitmap.width,
                                (top + side) / bitmap.height
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Text(mwString("cover.crop_save", "Lưu ảnh bìa"))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackTitleBlock(
    controller: MusicController,
    song: SongItem,
    onEditArtwork: () -> Unit,
    karaokeActive: Boolean,
    onLyrics: () -> Unit
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
            // Các thao tác cạnh tên bài hát: thêm ảnh, lời karaoke, yêu thích.
            // Chuyển nút karaoke từ thanh trên xuống đây để giao diện phát nhạc gọn hơn.
            if (song.source != SongSource.MOVIE) {
                IconButton(onClick = onEditArtwork, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = mwString("cover.change", "Thay ảnh bìa bài hát"),
                        tint = if (controller.isFavorite(song)) colors.magenta else colors.textSecondary,
                        modifier = Modifier.size(31.dp)
                    )
                }
                // Không ẩn nút khi lời chưa tải: chạm vào vẫn tìm lời LRCLIB như trước.
                IconButton(onClick = onLyrics, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MicExternalOn,
                        contentDescription = if (karaokeActive) "Ẩn lời bài hát" else "Hiện lời bài hát karaoke",
                        tint = if (karaokeActive) colors.purpleBright else colors.textSecondary,
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
            IconButton(onClick = { controller.toggleFavorite(song) }, modifier = Modifier.size(44.dp)) {
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
                tint = if (colors.isDark) colors.purpleBright else colors.purple,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                color = if (colors.isDark) colors.purpleBright else colors.purple,
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
    onOpenPremium: () -> Unit,
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
                    onOpenPremium = onOpenPremium,
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
    onOpenPremium: () -> Unit,
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
    var showPremiumRequired by rememberSaveable { mutableStateOf(false) }

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
                        if (MeloWarePremiumAccess.equalizerRequiresPremium) {
                            showPremiumRequired = true
                        } else {
                            controller.prepareAdvancedEqualizerBands()
                            showAdvancedDialog = true
                        }
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

    if (showAdvancedDialog && !MeloWarePremiumAccess.equalizerRequiresPremium) {
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
    if (showPremiumRequired) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPremiumRequired = false },
            title = { Text(mwString("premium.equalizer_locked_title", "Equalizer nâng cao · Premium")) },
            text = { Text(mwString("premium.equalizer_locked_detail", "Dùng thử đã hết. Kích hoạt Premium để mở EQ nâng cao.")) },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumRequired = false
                    onOpenPremium()
                }) { Text(mwString("premium.view_plans", "Xem gói Premium"), color = palette.purpleBright) }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumRequired = false }) {
                    Text(mwString("common.close", "Đóng"), color = palette.textSecondary)
                }
            },
            containerColor = palette.card,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary
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