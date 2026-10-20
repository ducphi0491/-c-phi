package duc_phi.music

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.util.Base64
import android.net.Uri
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.SystemClock
import java.nio.ByteOrder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.security.MessageDigest
import android.provider.OpenableColumns
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
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
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.ln
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
    val context = LocalContext.current
    val visualStore = remember(context.applicationContext) {
        VisualExperienceStore(context.applicationContext)
    }
    val visualPermissionDeniedMessage = mwString(
        "visual.permission_denied",
        "Audio permission was denied. Other visual effects still work normally."
    )
    var hasVisualizerPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val visualPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasVisualizerPermission = granted
        if (!granted) controller.statusMessage = visualPermissionDeniedMessage
    }
    val visualPremiumLocked = MeloWarePremiumAccess.albumStudioRequiresPremium
    val albumArtworkPremiumLocked = MeloWarePremiumAccess.albumStudioRequiresPremium
    val configuredVisualSettings = visualStore.settings
    val effectiveVisualSettings = if (visualPremiumLocked) {
        VisualExperienceSettings.DEFAULT
    } else {
        configuredVisualSettings
    }

    val song = controller.currentSong
    var showEqualizer by remember { mutableStateOf(false) }
    var showMasterVolume by remember { mutableStateOf(false) }
    var showVisualExperience by remember { mutableStateOf(false) }
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
        if (selectedUri != null && target != null && !albumArtworkPremiumLocked) {
            pendingArtworkCrop = target to selectedUri
        }
    }
    LaunchedEffect(albumArtworkPremiumLocked) {
        if (albumArtworkPremiumLocked) {
            showArtworkOptions = false
            googleArtworkFor = null
            artworkPickerSong = null
            pendingArtworkCrop?.let { controller.discardSearchedArtworkTemp(it.second) }
            pendingArtworkCrop = null
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
    val castContext = context
    val castRouteButton = remember(castContext) {
        MediaRouteButton(castContext).apply {
            CastButtonFactory.setUpMediaRouteButton(castContext, this)
        }
    }

    LaunchedEffect(Unit) { controller.initializeCast() }

    LaunchedEffect(song.id, controller.isLoading) {
        if (!controller.isLoading) controller.ensureAudioTechnicalInfo(song)
    }

    androidx.compose.runtime.LaunchedEffect(song.id) {
        controller.ensureArtwork(song)
    }

    val visualArtworkUrl = controller.artworkFor(song)
    val albumVisualPalette = rememberVisualAlbumPalette(
        artworkUrl = visualArtworkUrl,
        colors = colors,
        enabled = effectiveVisualSettings.colorMode == VisualColorMode.ALBUM || showVisualExperience
    )
    val themeVisualPalette = remember(colors) { visualThemePalette(colors) }
    val visualPalette = if (effectiveVisualSettings.colorMode == VisualColorMode.ALBUM) {
        albumVisualPalette
    } else {
        themeVisualPalette
    }
    val visualizerSupported = song.source != SongSource.MOVIE &&
            !controller.castConnected &&
            (effectiveVisualSettings.spectrumStyle != VisualSpectrumStyle.OFF ||
                    effectiveVisualSettings.albumEffect == VisualAlbumEffect.RAINBOW_EDGE ||
                    showVisualExperience) &&
            hasVisualizerPermission &&
            controller.isPlaying
    val visualSpectrumFrame = rememberVisualSpectrum(
        audioSessionId = controller.audioSessionId,
        enabled = visualizerSupported,
        performanceMode = effectiveVisualSettings.performanceMode
    )
    val visualizerUnavailableMessage = mwString(
        "visual.unavailable",
        "Real-time spectrum is unavailable on this device"
    )
    LaunchedEffect(visualSpectrumFrame.error) {
        visualSpectrumFrame.error?.takeIf { it.isNotBlank() }?.let { detail ->
            controller.statusMessage = "$visualizerUnavailableMessage: ${detail.take(140)}"
        }
    }
    val karaokeVisible = karaokeMode ||
            effectiveVisualSettings.playerLayout == VisualPlayerLayout.KARAOKE

    Box(modifier = Modifier.fillMaxSize()) {
        VisualPlayerBackground(
            song = song,
            artworkUrl = visualArtworkUrl,
            settings = effectiveVisualSettings,
            palette = visualPalette,
            colors = colors,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        karaokeActive = karaokeVisible,
                        visualActive = effectiveVisualSettings != VisualExperienceSettings.DEFAULT,
                        onBack = controller::goBackFromPlayer,
                        onLyrics = { karaokeMode = !karaokeMode },
                        onVisualExperience = { showVisualExperience = true },
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
                        } else if (karaokeVisible) {
                            KaraokeBackdrop(
                                controller = controller,
                                song = song,
                                modifier = Modifier
                                    .fillMaxWidth(
                                        if (effectiveVisualSettings.playerLayout == VisualPlayerLayout.KARAOKE) 0.98f else 0.94f
                                    )
                                    .widthIn(
                                        max = if (effectiveVisualSettings.playerLayout == VisualPlayerLayout.KARAOKE) 430.dp else 380.dp
                                    )
                                    .aspectRatio(1f)
                            )
                        } else {
                            val defaultArtwork = effectiveVisualSettings == VisualExperienceSettings.DEFAULT
                            val artworkModifier = when {
                                effectiveVisualSettings.playerLayout == VisualPlayerLayout.IMMERSIVE -> Modifier
                                    .fillMaxWidth(0.98f)
                                    .widthIn(max = 430.dp)
                                    .aspectRatio(1f)
                                defaultArtwork || effectiveVisualSettings.albumEffect == VisualAlbumEffect.RAINBOW_EDGE -> Modifier
                                    // Keep the original square album size for Rainbow Edge.
                                    .fillMaxWidth(0.90f)
                                    .widthIn(max = 360.dp)
                                    .aspectRatio(1f)
                                else -> Modifier
                                    .fillMaxWidth(0.92f)
                                    .widthIn(max = 380.dp)
                                    .aspectRatio(1f)
                            }
                            VisualArtworkStage(
                                song = song,
                                artworkUrl = visualArtworkUrl,
                                settings = effectiveVisualSettings,
                                palette = visualPalette,
                                spectrum = visualSpectrumFrame.values,
                                isPlaying = controller.isPlaying,
                                modifier = artworkModifier
                            )
                        }
                    }
                }

                item {
                    TrackTitleBlock(
                        controller = controller,
                        song = song,
                        artworkPremiumLocked = albumArtworkPremiumLocked,
                        onEditArtwork = {
                            if (albumArtworkPremiumLocked) onOpenPremium()
                            else showArtworkOptions = true
                        },
                        karaokeActive = karaokeVisible,
                        immersive = effectiveVisualSettings.playerLayout == VisualPlayerLayout.IMMERSIVE
                    )
                }

                item {
                    // Gom tiến trình, cụm Play và các nút công cụ vào một khối để đẩy chúng lên cao hơn.
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        PlaybackProgress(
                            controller = controller,
                            song = song,
                            positionMs = controller.positionMs,
                            durationMs = if (controller.durationMs > 0L) controller.durationMs else song.durationMs,
                            isPlaying = controller.isPlaying,
                            onSeek = controller::seekTo,
                            onOpenMasterVolume = { showMasterVolume = true },
                            visualSettings = effectiveVisualSettings,
                            visualPalette = visualPalette
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

            }

        }
    }

    // Show the playlist above the *entire* Now Playing page. Opening it does not
    // scroll the player or change playback; only explicit actions do so.
    if (showQueue) {
        QueuePanel(
            controller = controller,
            onClose = { showQueue = false },
            onTools = { selected ->
                showQueue = false
                actionSong = selected
            }
        )
    }

    if (showArtworkOptions && !albumArtworkPremiumLocked) {
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

    googleArtworkFor?.takeIf { !albumArtworkPremiumLocked }?.let { target ->
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

    pendingArtworkCrop?.takeIf { !albumArtworkPremiumLocked }?.let { selection ->
        ArtworkCropDialog(
            controller = controller,
            selectedUri = selection.second,
            onSave = { cropBounds ->
                if (albumArtworkPremiumLocked) {
                    controller.discardSearchedArtworkTemp(selection.second)
                    onOpenPremium()
                } else {
                    controller.setCustomArtwork(selection.first, selection.second, cropBounds)
                }
                pendingArtworkCrop = null
            },
            onDismiss = {
                controller.discardSearchedArtworkTemp(selection.second)
                pendingArtworkCrop = null
            }
        )
    }

    if (showVisualExperience) {
        VisualExperienceDialog(
            song = song,
            artworkUrl = visualArtworkUrl,
            currentSettings = configuredVisualSettings,
            albumPalette = albumVisualPalette,
            themePalette = themeVisualPalette,
            spectrum = visualSpectrumFrame.values,
            isPlaying = controller.isPlaying,
            premiumLocked = visualPremiumLocked,
            hasVisualizerPermission = hasVisualizerPermission,
            onApply = { newSettings, requestPermission ->
                visualStore.update(newSettings)
                showVisualExperience = false
                if (requestPermission) {
                    visualPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onOpenPremium = {
                showVisualExperience = false
                onOpenPremium()
            },
            onDismiss = { showVisualExperience = false }
        )
    }

    if (showMasterVolume && song.source != SongSource.MOVIE && !controller.castConnected) {
        MasterVolumeDialog(controller = controller, onDismiss = { showMasterVolume = false })
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
    karaokeActive: Boolean,
    visualActive: Boolean,
    onBack: () -> Unit,
    onLyrics: () -> Unit,
    onVisualExperience: () -> Unit,
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

        IconButton(onClick = onVisualExperience) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = mwString("visual.open", "Open Visual Experience"),
                tint = if (visualActive) colors.purpleBright else colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Giữ nguyên nút karaoke lớn trên thanh trên cùng: nút này chỉ bật/tắt khung lời.
        IconButton(onClick = onLyrics) {
            Icon(
                imageVector = Icons.Rounded.MicExternalOn,
                contentDescription = if (karaokeActive) "Ẩn lời bài hát" else "Mở lời bài hát karaoke",
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
    val shape = RoundedCornerShape(26.dp)
    val lyrics = controller.lyricsFor(song)
    val loading = controller.lyricsLoading(song)
    val error = controller.lyricsError(song)
    val lyricScrollState = rememberScrollState()
    val lineOffsets = remember(song.id, lyrics?.recordId) {
        androidx.compose.runtime.mutableStateMapOf<Int, Int>()
    }

    // Hide the decorative standalone music note without touching lyric timestamps.
    val hideIntroMarker = lyrics?.lines?.takeIf { it.size > 1 }?.firstOrNull()
        ?.text?.trim()?.let { marker ->
            marker.isNotEmpty() && marker.all { it in "♪♫♬♩" }
        } == true
    val lyricStartIndex = if (hideIntroMarker) 1 else 0
    val displayedLyrics = lyrics?.lines?.drop(lyricStartIndex).orEmpty()

    val lyricPrimary = if (colors.isDark) Color.White else colors.textPrimary
    val lyricSecondary = if (colors.isDark) Color.White.copy(alpha = 0.70f)
    else colors.textSecondary.copy(alpha = 0.92f)
    val lyricTertiary = if (colors.isDark) Color.White.copy(alpha = 0.55f)
    else colors.textSecondary.copy(alpha = 0.76f)

    LaunchedEffect(song.id) { controller.loadLyrics(song) }

    val activeLineIndex = if (lyrics?.synced == true && lyrics.lines.isNotEmpty()) {
        lyrics.lines.indexOfLast { line ->
            val timestamp = line.timestampMs ?: Long.MAX_VALUE
            timestamp <= controller.positionMs + 120L
        }
    } else {
        -1
    }

    // Scroll to a few lines above the active line. Use a fully composed Column
    // so a text selection can span ANY adjacent lyric lines (LazyColumn only
    // composes visible lines and previously prevented cross-line selection).
    val targetLine = (activeLineIndex - lyricStartIndex - 2).coerceAtLeast(0)
    val targetTop = lineOffsets[targetLine]
    LaunchedEffect(song.id, lyrics?.recordId) { lyricScrollState.scrollTo(0) }
    LaunchedEffect(activeLineIndex, lyrics?.recordId, targetTop) {
        if (lyrics?.synced == true && activeLineIndex >= 0 && targetTop != null) {
            lyricScrollState.animateScrollTo(targetTop.coerceIn(0, lyricScrollState.maxValue))
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.card)
            .border(
                1.dp,
                colors.border.copy(alpha = if (colors.isDark) 0.25f else 0.90f),
                shape
            )
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
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    if (colors.isDark) {
                        listOf(Color(0x99020411), Color(0xDD08051A), Color(0xF6050713))
                    } else {
                        listOf(Color(0xEEF4F5FE), Color(0xF7F7F8FF), Color(0xFFF4F7FC))
                    }
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
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
                lyrics != null && displayedLyrics.isNotEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(lyricScrollState)
                            .padding(top = 4.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Exactly ONE selection scope for the entire song:
                        // long-press one line, drag handles over several lines, Copy.
                        SelectionContainer {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                displayedLyrics.forEachIndexed { visibleIndex, line ->
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
                                    Text(
                                        text = if (active) karaokeColoredWords(
                                            line,
                                            highlightedCount,
                                            colors.purpleBright,
                                            lyricPrimary.copy(alpha = if (colors.isDark) 0.72f else 0.78f)
                                        ) else AnnotatedString(line.text),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { coordinates ->
                                                val top = coordinates.positionInParent().y.roundToInt()
                                                if (lineOffsets[visibleIndex] != top) {
                                                    lineOffsets[visibleIndex] = top
                                                }
                                            }
                                            .clickable { line.timestampMs?.let(controller::seekTo) }
                                            .padding(
                                                horizontal = 4.dp,
                                                vertical = if (active) 4.dp else 0.dp
                                            ),
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
private fun LyricsInformationInlineCard(
    song: SongItem,
    controller: MusicController,
    lyrics: LyricsContent,
    onClose: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val timedCount = lyrics.lines.count { it.timestampMs != null }
    val enhancedCount = lyrics.lines.count { it.wordTimings.isNotEmpty() }
    val actualDuration = if (controller.durationMs > 1_000L) controller.durationMs else song.durationMs
    val mismatched = lyrics.synced && actualDuration > 20_000L && lyrics.durationMs > 20_000L &&
            kotlin.math.abs(actualDuration - lyrics.durationMs) > 15_000L
    val detailsText = if (lyrics.synced) {
        "LRC đồng bộ · $timedCount câu · eLRC $enhancedCount câu"
    } else {
        "Lời thường · ${lyrics.lines.size} câu"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (colors.isDark) colors.card else colors.backgroundSoft,
        border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null,
                    tint = colors.purpleBright, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Thông tin lời bài hát",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Đóng thông tin",
                        tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                }
            }
            Text(detailsText, fontSize = 12.sp, color = colors.textPrimary)
            Text(
                "Nguồn: ${lyrics.sourceName}",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
            if (mismatched) {
                Text(
                    "Lưu ý: thời lượng lời có thể không khớp bản nhạc.",
                    fontSize = 11.sp,
                    color = if (colors.isDark) Color(0xFFFFC06A) else Color(0xFF9A4A00)
                )
            }
            Text(
                "Nhấn giữ lời bài hát, kéo qua nhiều câu để chọn và sao chép.",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        val fullText = lyrics.lines.joinToString("\n") { it.text.trim() }.trim()
                        if (fullText.isNotEmpty()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("Lời bài hát - ${song.title}", fullText)
                            )
                            Toast.makeText(context,
                                "Đã sao chép toàn bộ lời bài hát", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null,
                        modifier = Modifier.size(17.dp), tint = colors.purpleBright)
                    Spacer(Modifier.width(5.dp))
                    Text("Sao chép tất cả", color = colors.purpleBright, fontSize = 11.sp)
                }
                TextButton(
                    onClick = { shareLyricsWithAndroid(context, song, lyrics) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null,
                        modifier = Modifier.size(17.dp), tint = colors.purpleBright)
                    Spacer(Modifier.width(5.dp))
                    Text("Chia sẻ lời", color = colors.purpleBright, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TrackTitleBlock(
    controller: MusicController,
    song: SongItem,
    artworkPremiumLocked: Boolean,
    onEditArtwork: () -> Unit,
    karaokeActive: Boolean,
    immersive: Boolean = false
) {
    val colors = LocalMusicColors.current
    val technicalInfo = controller.audioTechnicalInfoFor(song)
    val lyrics = controller.lyricsFor(song)
    val hasLyrics = lyrics?.lines?.isNotEmpty() == true
    var showLyricsInformation by rememberSaveable(song.id) { mutableStateOf(false) }
    LaunchedEffect(song.id, controller.isLoading) {
        if (!controller.isLoading) controller.ensureAudioTechnicalInfo(song)
    }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (immersive) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = song.title,
                    color = colors.textPrimary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (immersive) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = song.artist,
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (immersive) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Các thao tác cạnh tên bài hát: thông tin lời, thêm ảnh và yêu thích.
            // Nút karaoke lớn trên thanh trên cùng vẫn là nút bật/tắt lời bài hát.
            if (song.source != SongSource.MOVIE) {
                // Chỉ hiện nút nhỏ này khi bài hát đã có dữ liệu lời.
                // Đặt bên trái nút thêm ảnh đúng theo bố cục yêu cầu.
                if (hasLyrics) {
                    IconButton(
                        onClick = { showLyricsInformation = !showLyricsInformation },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Mở thông tin và chia sẻ lời bài hát",
                            tint = if (showLyricsInformation || karaokeActive) colors.purpleBright else colors.textSecondary,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onEditArtwork, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Outlined.AddPhotoAlternate,
                            contentDescription = mwString("cover.change", "Thay ảnh bìa bài hát"),
                            tint = if (artworkPremiumLocked) colors.textMuted
                            else if (controller.isFavorite(song)) colors.magenta
                            else colors.textSecondary,
                            modifier = Modifier.size(31.dp)
                        )
                    }
                    if (artworkPremiumLocked) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).size(17.dp),
                            shape = CircleShape,
                            color = Color(0xFF25152F),
                            border = BorderStroke(1.dp, Color(0xFFFFD54F))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.WorkspacePremium,
                                    contentDescription = mwString("premium.required", "Premium"),
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
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
        if (showLyricsInformation && lyrics != null && hasLyrics) {
            Spacer(Modifier.height(8.dp))
            LyricsInformationInlineCard(
                song = song,
                controller = controller,
                lyrics = lyrics,
                onClose = { showLyricsInformation = false }
            )
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

/**
 * Progressive waveform for local, Google Drive cache, HTTP/LAN and verified P2P.
 * Never blocks playback: pending frames are neutral ticks until actual PCM is decoded.
 * Final frames are cached on disk so reopening the same song is instantaneous.
 */
@Composable
private fun PlaybackProgress(
    controller: MusicController,
    song: SongItem,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onOpenMasterVolume: () -> Unit,
    visualSettings: VisualExperienceSettings = VisualExperienceSettings.DEFAULT,
    visualPalette: VisualAlbumPalette? = null
) {
    val colors = LocalMusicColors.current
    val progressPalette = visualPalette ?: visualThemePalette(colors)
    val context = LocalContext.current.applicationContext
    val safeDuration = max(1L, durationMs)
    val uriString = song.uri.toString()
    val supportedWaveformSource = song.source != SongSource.MOVIE && (
            song.source == SongSource.PERSONAL_DRIVE ||
                    song.source == SongSource.PUBLIC_DRIVE ||
                    song.source == SongSource.INTERNET_P2P ||
                    song.uri.scheme?.lowercase(Locale.ROOT) in listOf("content", "file", "android.resource", "http", "https")
            )
    // Duration is often discovered after an online player starts buffering.
    // Re-run the effect as soon as a valid duration becomes available.
    val analysisDuration = if (song.durationMs > 1000L) song.durationMs else safeDuration
    var waveform by remember(song.id, uriString) { mutableStateOf<MeloWaveAudioWaveform?>(null) }
    var analyzing by remember(song.id, uriString) { mutableStateOf(supportedWaveformSource) }
    var dragging by remember(song.id) { mutableStateOf(false) }
    var localValue by remember(song.id) { mutableFloatStateOf(0f) }
    val effectivePosition = if (dragging) localValue else positionMs.toFloat().coerceIn(0f, safeDuration.toFloat())
    val fraction = (effectivePosition / safeDuration.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(song.id, uriString, analysisDuration) {
        waveform = null
        analyzing = supportedWaveformSource && analysisDuration > 1000L
        if (!analyzing) return@LaunchedEffect

        // Google Drive playback already saves an authorized, fully downloaded
        // copy in DriveAudioCache. Reuse it; NEVER request the Drive file again.
        // WebRTC P2P exposes its temporary file only after SHA-256 verification.
        // Neither availability map is Compose state, so poll on the background
        // timeline and stop as soon as the song changes or the screen closes.
        val waitForCache = song.source == SongSource.PERSONAL_DRIVE ||
                song.source == SongSource.PUBLIC_DRIVE ||
                song.source == SongSource.INTERNET_P2P
        var playableUri: Uri? = controller.waveformSourceFor(song)
        if (waitForCache) {
            var attempts = 0
            while (playableUri == null && attempts < 360 &&
                currentCoroutineContext().isActive && controller.currentSong?.id == song.id) {
                delay(400L)
                playableUri = controller.waveformSourceFor(song)
                attempts++
            }
        }
        val readyUri = playableUri
        if (readyUri == null || controller.currentSong?.id != song.id) {
            analyzing = false
            return@LaunchedEffect
        }

        // The player remains independent of this decoding work. Cached
        // waveforms appear immediately; new waveforms publish ~140 ms batches.
        val result = meloWaveformForPlayableSong(
            context = context,
            song = song,
            playableUri = readyUri,
            durationMs = analysisDuration,
            isActive = { currentCoroutineContext().isActive },
            onPartial = { partial -> waveform = partial }
        )
        waveform = result
        analyzing = false
    }

    val transition = rememberInfiniteTransition(label = "melowave_waveform_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(580), repeatMode = RepeatMode.Reverse),
        label = "melowave_waveform_glow"
    )

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .semantics {
                    contentDescription = "Thanh tiến trình dạng sóng, chạm hoặc kéo để tua"
                    progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                    setProgress { newFraction ->
                        onSeek((newFraction.coerceIn(0f, 1f) * safeDuration).toLong())
                        true
                    }
                }
                .pointerInput(safeDuration) {
                    detectTapGestures { tap ->
                        onSeek(((tap.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * safeDuration).toLong())
                    }
                }
                .pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { start ->
                            dragging = true
                            localValue = (start.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * safeDuration
                        },
                        onDragEnd = {
                            if (dragging) onSeek(localValue.toLong())
                            dragging = false
                        },
                        onDragCancel = { dragging = false },
                        onDrag = { change, _ ->
                            localValue = (change.position.x / size.width.coerceAtLeast(1))
                                .coerceIn(0f, 1f) * safeDuration
                            change.consume()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height / 2f
                val progressX = fraction * size.width
                val data = waveform
                // Streaming sources show neutral placeholder ticks while decoding
                // (NOT made-up amplitude peaks). Unsupported audio keeps a flat bar.
                if (data == null && !analyzing) {
                    drawLine(
                        colors.border.copy(alpha = 0.78f),
                        start = Offset(0f, centerY), end = Offset(size.width, centerY),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                    )
                    if (progressX > 0f) drawLine(
                        progressPalette.primary,
                        start = Offset(0f, centerY), end = Offset(progressX, centerY),
                        strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round
                    )
                } else {
                    val gap = when (visualSettings.waveformStyle) {
                        VisualWaveformStyle.CLASSIC -> 4.5.dp.toPx()
                        VisualWaveformStyle.BARS -> 4.9.dp.toPx()
                        VisualWaveformStyle.MIRROR -> 5.0.dp.toPx()
                        VisualWaveformStyle.NEON -> 4.8.dp.toPx()
                    }
                    val count = (size.width / gap).toInt().coerceAtLeast(2)
                    val analyzed = data?.analyzedBuckets ?: 0
                    val available = data?.levels?.size ?: MELO_WAVE_BUCKETS
                    for (i in 0 until count) {
                        val x = (i + 0.5f) * size.width / count
                        val idx = (i.toFloat() / (count - 1) * (available - 1))
                            .toInt().coerceIn(0, available - 1)
                        val isReady = data != null && idx < analyzed
                        val sample = if (isReady) data!!.levels[idx] else 0f
                        val nearness = (1f - abs(x - progressX) / (gap * 5f)).coerceIn(0f, 1f)
                        val motion = if (
                            isReady && isPlaying && !dragging &&
                            visualSettings.performanceMode != VisualPerformanceMode.BATTERY
                        ) {
                            val basePulse = if (visualSettings.waveformStyle == VisualWaveformStyle.CLASSIC) 0.07f else 0.06f
                            val animatedPulse = if (visualSettings.waveformStyle == VisualWaveformStyle.CLASSIC) 0.14f else 0.12f
                            1f + nearness * (basePulse + pulse * animatedPulse) * (0.4f + sample * 0.6f)
                        } else 1f
                        val baseHeight = if (isReady) (3.dp.toPx() + sample * 27.dp.toPx()) * motion
                        else 3.dp.toPx()
                        val played = x <= progressX
                        val tone = if (isReady) data!!.frequencyBands[idx] else 0.5f
                        val playingColor = lerp(
                            progressPalette.primary,
                            progressPalette.secondary,
                            tone * (if (visualSettings.waveformStyle == VisualWaveformStyle.CLASSIC) 0.65f else 0.72f)
                        )
                        val barColor = when {
                            !isReady -> colors.textSecondary.copy(alpha = if (colors.isDark) 0.23f else 0.28f)
                            played -> playingColor
                            else -> colors.textSecondary.copy(alpha = if (colors.isDark) 0.52f else 0.43f)
                        }

                        when (visualSettings.waveformStyle) {
                            VisualWaveformStyle.CLASSIC -> {
                                // Preserve MeloWave's existing waveform as the free/default style.
                                drawLine(
                                    color = barColor,
                                    start = Offset(x, centerY - baseHeight / 2f),
                                    end = Offset(x, centerY + baseHeight / 2f),
                                    strokeWidth = 2.6.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            VisualWaveformStyle.BARS -> {
                                val baseline = size.height - 3.dp.toPx()
                                val height = baseHeight.coerceAtMost(size.height - 5.dp.toPx())
                                drawLine(
                                    color = barColor,
                                    start = Offset(x, baseline),
                                    end = Offset(x, baseline - height),
                                    strokeWidth = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            VisualWaveformStyle.MIRROR -> {
                                val halfHeight = baseHeight / 2f
                                val centerGap = 1.7.dp.toPx()
                                drawLine(
                                    color = barColor,
                                    start = Offset(x, centerY - centerGap),
                                    end = Offset(x, centerY - halfHeight),
                                    strokeWidth = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = barColor,
                                    start = Offset(x, centerY + centerGap),
                                    end = Offset(x, centerY + halfHeight),
                                    strokeWidth = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            VisualWaveformStyle.NEON -> {
                                drawLine(
                                    color = barColor.copy(alpha = 0.20f),
                                    start = Offset(x, centerY - baseHeight * 0.68f),
                                    end = Offset(x, centerY + baseHeight * 0.68f),
                                    strokeWidth = 7.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = barColor,
                                    start = Offset(x, centerY - baseHeight / 2f),
                                    end = Offset(x, centerY + baseHeight / 2f),
                                    strokeWidth = 2.3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                val thumbX = progressX.coerceIn(0f, size.width.coerceAtLeast(0f))
                drawCircle(progressPalette.primary.copy(alpha = 0.20f), radius = 8.5.dp.toPx(), center = Offset(thumbX, centerY))
                drawCircle(progressPalette.primary, radius = 5.7.dp.toPx(), center = Offset(thumbX, centerY))
            }
        }
        // Persistent, in-app Master Volume lives under the waveform. It does not
        // consume another slot in the five-button toolbar on small devices.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(formatDuration(effectivePosition.toLong()), color = colors.textSecondary, fontSize = 12.sp)
            if (song.source != SongSource.MOVIE && !controller.castConnected) {
                Surface(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .clickable(onClick = onOpenMasterVolume),
                    shape = RoundedCornerShape(11.dp),
                    color = colors.purple.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.38f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (controller.soundMasterVolumePercent == 0)
                                Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            contentDescription = mwString("soundstudio.master_volume", "Âm lượng MeloWave"),
                            tint = colors.purpleBright,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "${controller.soundMasterVolumePercent}%",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Text(formatDuration(safeDuration), color = colors.textSecondary, fontSize = 12.sp)
        }
    }
}

private const val MELO_WAVE_BUCKETS = 256
private const val MELO_WAVE_MAGIC = 0x4D573256 // "MW2V"; cache v2 has RMS+peak contrast and 256 buckets.
private const val MELO_WAVE_DISK_LIMIT = 220

/** analyzedBuckets marks the actual decoded portion (no fabricated peaks). */
private data class MeloWaveAudioWaveform(
    val levels: FloatArray,
    val frequencyBands: FloatArray,
    val analyzedBuckets: Int = MELO_WAVE_BUCKETS,
    val complete: Boolean = true
)

private object MeloWaveWaveformCache {
    private const val MAX_CACHED = 24
    private val entries = object : LinkedHashMap<String, MeloWaveAudioWaveform>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, MeloWaveAudioWaveform>?): Boolean =
            size > MAX_CACHED
    }
    fun get(key: String): MeloWaveAudioWaveform? = synchronized(entries) { entries[key] }
    fun put(key: String, value: MeloWaveAudioWaveform) = synchronized(entries) { entries[key] = value; Unit }
}

/** Keep a tiny versioned cache on-device. Android may clear cacheDir when storage is low. */
private fun meloWaveDiskFile(context: Context, key: String): File {
    val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray(Charsets.UTF_8))
    val hex = "0123456789abcdef"
    val name = buildString(32) {
        for (i in 0 until 16) {
            val byte = digest[i].toInt() and 255
            append(hex[byte shr 4])
            append(hex[byte and 15])
        }
    }
    return File(File(context.cacheDir, "melowave_waveforms_v2"), "$name.mwf")
}

private fun meloReadDiskWaveform(context: Context, key: String): MeloWaveAudioWaveform? {
    val file = meloWaveDiskFile(context, key)
    if (!file.isFile || file.length() > 16_384L) return null
    return try {
        DataInputStream(BufferedInputStream(FileInputStream(file))).use { input ->
            if (input.readInt() != MELO_WAVE_MAGIC || input.readInt() != MELO_WAVE_BUCKETS) return null
            val levels = FloatArray(MELO_WAVE_BUCKETS) { input.readFloat().coerceIn(0f, 1f) }
            val bands = FloatArray(MELO_WAVE_BUCKETS) { input.readFloat().coerceIn(0f, 1f) }
            if (levels.any { !it.isFinite() } || bands.any { !it.isFinite() }) return null
            file.setLastModified(System.currentTimeMillis()) // LRU eviction
            MeloWaveAudioWaveform(levels, bands)
        }
    } catch (_: Exception) {
        runCatching { file.delete() }
        null
    }
}

private fun meloWriteDiskWaveform(context: Context, key: String, wave: MeloWaveAudioWaveform) {
    if (!wave.complete || wave.analyzedBuckets != MELO_WAVE_BUCKETS) return
    val file = meloWaveDiskFile(context, key)
    val dir = file.parentFile ?: return
    if (!dir.isDirectory && !dir.mkdirs()) return
    val temp = File(dir, "${file.name}.${SystemClock.uptimeMillis()}.tmp")
    try {
        DataOutputStream(BufferedOutputStream(FileOutputStream(temp))).use { output ->
            output.writeInt(MELO_WAVE_MAGIC)
            output.writeInt(MELO_WAVE_BUCKETS)
            wave.levels.forEach(output::writeFloat)
            wave.frequencyBands.forEach(output::writeFloat)
        }
        if (!temp.renameTo(file)) {
            runCatching { file.delete() }
            if (!temp.renameTo(file)) return
        }
        dir.listFiles { candidate -> candidate.extension == "mwf" }
            ?.sortedBy { it.lastModified() }
            ?.let { ordered ->
                ordered.take((ordered.size - MELO_WAVE_DISK_LIMIT).coerceAtLeast(0))
                    .forEach { runCatching { it.delete() } }
            }
    } catch (_: Exception) {
        // A cache write error must never interrupt playback.
    } finally {
        runCatching { temp.delete() }
    }
}

/** Invalidate on local size/mtime changes; no whole-file hashing or extra permissions. */
private fun meloLocalVersion(context: Context, uri: Uri): String = when (uri.scheme?.lowercase(Locale.ROOT)) {
    "file" -> runCatching {
        val file = File(uri.path ?: "")
        "${file.length()}:${file.lastModified()}"
    }.getOrDefault("")
    "content" -> runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) cursor.getLong(sizeColumn).toString() else ""
            } else ""
        }.orEmpty()
    }.getOrDefault("")
    else -> ""
}

private suspend fun meloWaveformForPlayableSong(
    context: Context,
    song: SongItem,
    playableUri: Uri,
    durationMs: Long,
    isActive: suspend () -> Boolean,
    onPartial: suspend (MeloWaveAudioWaveform) -> Unit
): MeloWaveAudioWaveform? {
    val scheme = playableUri.scheme?.lowercase(Locale.ROOT).orEmpty()
    if (scheme !in listOf("content", "file", "android.resource", "http", "https")) return null
    val key = withContext(Dispatchers.IO) {
        // The WebRTC file name changes on every transfer. Use the stable remote
        // song ID and VERIFIED byte length to reuse its waveform across replays.
        // Other sources keep the V2 key to reuse previously analyzed local tracks.
        if (song.source == SongSource.INTERNET_P2P) {
            val size = File(playableUri.path.orEmpty()).length()
            "${song.id}|${song.uri}|$durationMs|p2p:$size|v2"
        } else {
            "${song.id}|$playableUri|$durationMs|${meloLocalVersion(context, playableUri)}|v2"
        }
    }
    MeloWaveWaveformCache.get(key)?.let { return it }
    withContext(Dispatchers.IO) { meloReadDiskWaveform(context, key) }?.let { cached ->
        MeloWaveWaveformCache.put(key, cached)
        return cached
    }
    val decoded = withContext(Dispatchers.IO) {
        meloDecodeWaveform(context, playableUri, durationMs, isActive) { partial ->
            withContext(Dispatchers.Main.immediate) { onPartial(partial) }
        }
    } ?: return null
    if (decoded.complete) {
        MeloWaveWaveformCache.put(key, decoded)
        withContext(Dispatchers.IO) { meloWriteDiskWaveform(context, key, decoded) }
    }
    return decoded
}

/**
 * Compressing each bucket to plain RMS used to flatten dynamics. Instead,
 * blend actual RMS/Peak and scale around robust 10th/90th percentiles, with
 * a minimum 18% contrast span to avoid amplifying nearly identical samples.
 * No random data, artificial wave peaks or broad neighbor smoothing.
 */
private fun meloBuildWaveform(
    sums: DoubleArray,
    peaks: FloatArray,
    counts: IntArray,
    frequency: FloatArray,
    analyzedBuckets: Int,
    complete: Boolean
): MeloWaveAudioWaveform {
    val end = analyzedBuckets.coerceIn(0, MELO_WAVE_BUCKETS)
    val energy = FloatArray(MELO_WAVE_BUCKETS) { i ->
        if (i >= end || counts[i] <= 0) 0f
        else {
            val rms = sqrt(sums[i] / counts[i]).toFloat()
            rms * 0.74f + peaks[i] * 0.26f
        }
    }
    val nonZero = energy.take(end).filter { it > 0.0001f }.sorted()
    val levels = FloatArray(MELO_WAVE_BUCKETS)
    if (nonZero.isNotEmpty()) {
        fun percentile(p: Float): Float = nonZero[((nonZero.size - 1) * p).toInt().coerceIn(0, nonZero.lastIndex)]
        val low = percentile(0.10f)
        val high = percentile(0.90f).coerceAtLeast(0.0001f)
        val span = max(high - low, high * 0.18f)
        for (i in 0 until end) {
            if (counts[i] <= 0) continue
            val relative = (energy[i] / high).coerceIn(0f, 1.3f)
            val contrast = ((energy[i] - low) / span).coerceIn(0f, 1f)
            // 60% real peak-relative level, 40% controlled local dynamic range.
            levels[i] = (0.60f * (relative / 1.3f) + 0.40f * contrast)
                .coerceIn(0f, 1f).pow(1.25f)
        }
    }
    return MeloWaveAudioWaveform(levels, frequency.copyOf(), end, complete)
}

/** Decode PCM on IO; every completed region is published progressively. */
private suspend fun meloDecodeWaveform(
    context: Context,
    uri: Uri,
    durationMs: Long,
    isActive: suspend () -> Boolean,
    onPartial: suspend (MeloWaveAudioWaveform) -> Unit
): MeloWaveAudioWaveform? {
    val buckets = MELO_WAVE_BUCKETS
    val sums = DoubleArray(buckets)
    val peaks = FloatArray(buckets)
    val counts = IntArray(buckets)
    val snippetLength = 1024
    val windows = Array(buckets) { FloatArray(snippetLength) }
    val windowSizes = IntArray(buckets)
    val frequency = FloatArray(buckets) { 0.5f }
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var decoderStarted = false
    return try {
        // Android's MediaExtractor accepts a remote URL through the String+headers
        // overload, but not reliably through the ContentResolver Uri overload.
        // This secondary, background HTTP read NEVER holds up Media3 playback.
        // Google Drive uses an authorized local cache file, not HTTP re-download.
        if (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
            extractor.setDataSource(uri.toString(), emptyMap<String, String>())
        } else {
            extractor.setDataSource(context, uri, null)
        }
        val track = (0 until extractor.trackCount).firstOrNull {
            extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val sourceFormat = extractor.getTrackFormat(track)
        val mime = sourceFormat.getString(MediaFormat.KEY_MIME) ?: return null
        val lengthUs = durationMs.coerceAtLeast(1) * 1000L
        decoder = MediaCodec.createDecoderByType(mime)
        val codec = decoder!!
        codec.configure(sourceFormat, null, null, 0)
        codec.start()
        decoderStarted = true
        var outputFormat = sourceFormat
        var sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
        var channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
        var pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
            outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
        var inputEnded = false
        var outputEnded = false
        var hasSamples = false
        val info = MediaCodec.BufferInfo()
        val started = SystemClock.elapsedRealtime()
        var lastPublishedMs = 0L
        var lastPublishedBucket = 0
        var lastReady = 0
        var frequenciesDone = 0
        var iterations = 0
        var reachedDeadline = false

        suspend fun publish(finishedBuckets: Int, isComplete: Boolean) {
            val until = finishedBuckets.coerceIn(0, buckets)
            if (until <= 0) return
            for (i in frequenciesDone until until) {
                if (windowSizes[i] >= 256) {
                    frequency[i] = meloWaveDominantBand(windows[i], windowSizes[i], sampleRate / 8.0)
                }
            }
            frequenciesDone = max(frequenciesDone, until)
            onPartial(meloBuildWaveform(sums, peaks, counts, frequency, until, isComplete))
        }

        while (!outputEnded) {
            iterations++
            if (iterations % 32 == 0) {
                if (!isActive()) return null
                if (SystemClock.elapsedRealtime() - started > 12_000L) {
                    reachedDeadline = true
                    break
                }
            }
            if (!inputEnded) {
                val index = codec.dequeueInputBuffer(5_000L)
                if (index >= 0) {
                    val buffer = codec.getInputBuffer(index) ?: return null
                    buffer.clear()
                    val length = extractor.readSampleData(buffer, 0)
                    if (length < 0) {
                        codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputEnded = true
                    } else {
                        codec.queueInputBuffer(index, 0, length, extractor.sampleTime.coerceAtLeast(0L), 0)
                        extractor.advance()
                    }
                }
            }
            when (val index = codec.dequeueOutputBuffer(info, 5_000L)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    outputFormat = codec.outputFormat
                    sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE).coerceAtLeast(1)
                    channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    pcmEncoding = if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING))
                        outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                else -> if (index >= 0) {
                    try {
                        val output = codec.getOutputBuffer(index)
                        if (output != null && info.size > 0) {
                            val sampleBytes = when (pcmEncoding) {
                                AudioFormat.ENCODING_PCM_16BIT -> 2
                                AudioFormat.ENCODING_PCM_FLOAT -> 4
                                else -> 0
                            }
                            if (sampleBytes == 0) return null
                            val frames = info.size / (channels * sampleBytes)
                            val samples = output.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                            val base = info.offset
                            for (frame in 0 until frames step 8) {
                                val bucket = (((info.presentationTimeUs + frame * 1_000_000L / sampleRate) * buckets) / lengthUs)
                                    .toInt().coerceIn(0, buckets - 1)
                                var channelEnergy = 0.0
                                var channelPeak = 0f
                                var mono = 0.0
                                val usedChannels = channels.coerceAtMost(2)
                                for (channel in 0 until usedChannels) {
                                    val offset = base + (frame * channels + channel) * sampleBytes
                                    val value = if (sampleBytes == 2) samples.getShort(offset).toDouble() / 32768.0
                                    else samples.getFloat(offset).toDouble().coerceIn(-1.0, 1.0)
                                    channelEnergy += value * value
                                    channelPeak = max(channelPeak, abs(value).toFloat())
                                    mono += value
                                }
                                mono /= usedChannels
                                // Energy of both channels (not averaged signed PCM), so
                                // opposite-phase stereo does not falsely look like silence.
                                sums[bucket] += channelEnergy / usedChannels
                                peaks[bucket] = max(peaks[bucket], channelPeak)
                                counts[bucket]++
                                if (windowSizes[bucket] < snippetLength) {
                                    windows[bucket][windowSizes[bucket]++] = mono.toFloat()
                                }
                                lastReady = max(lastReady, bucket)
                                hasSamples = true
                            }
                        }
                    } finally {
                        codec.releaseOutputBuffer(index, false)
                    }
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputEnded = true
                }
            }
            // The bucket currently playing into is not finished. Publish ONLY
            // earlier time buckets, never fill unprocessed audio with made-up peaks.
            val now = SystemClock.elapsedRealtime()
            val finished = lastReady.coerceIn(0, buckets - 1)
            if (hasSamples && finished >= 5 &&
                (lastPublishedBucket == 0 || finished - lastPublishedBucket >= 10) &&
                (lastPublishedBucket == 0 || now - lastPublishedMs >= 140L)) {
                publish(finished, false)
                lastPublishedBucket = finished
                lastPublishedMs = now
            }
        }
        if (!hasSamples) return null
        val complete = outputEnded && !reachedDeadline
        val computedBuckets = if (complete) buckets else (lastReady + 1).coerceIn(0, buckets)
        if (computedBuckets < 4) return null
        for (i in frequenciesDone until computedBuckets) {
            if (windowSizes[i] >= 256) frequency[i] = meloWaveDominantBand(windows[i], windowSizes[i], sampleRate / 8.0)
        }
        meloBuildWaveform(sums, peaks, counts, frequency, computedBuckets, complete)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    } finally {
        if (decoderStarted) runCatching { decoder?.stop() }
        runCatching { decoder?.release() }
        runCatching { extractor.release() }
    }
}

/** Approximate dominant *frequency band* of a short PCM snippet, not note transcription. */
private fun meloWaveDominantBand(samples: FloatArray, length: Int, sampleRateHz: Double): Float {
    val n = 1024
    val re = DoubleArray(n) { idx ->
        if (idx < length) samples[idx].toDouble() * (0.5 - 0.5 * cos(2.0 * PI * idx / (n - 1))) else 0.0
    }
    val im = DoubleArray(n)
    var j = 0
    for (i in 1 until n) {
        var bit = n shr 1
        while ((j and bit) != 0) { j = j xor bit; bit = bit shr 1 }
        j = j xor bit
        if (i < j) {
            val temp = re[i]; re[i] = re[j]; re[j] = temp
        }
    }
    var span = 2
    while (span <= n) {
        val angle = -2.0 * PI / span
        val wr = cos(angle)
        val wi = sin(angle)
        for (base in 0 until n step span) {
            var real = 1.0
            var imaginary = 0.0
            for (k in 0 until span / 2) {
                val p = base + k
                val q = p + span / 2
                val tr = real * re[q] - imaginary * im[q]
                val ti = real * im[q] + imaginary * re[q]
                re[q] = re[p] - tr; im[q] = im[p] - ti
                re[p] += tr; im[p] += ti
                val nextReal = real * wr - imaginary * wi
                imaginary = real * wi + imaginary * wr
                real = nextReal
            }
        }
        span = span shl 1
    }
    val lowBin = ((75.0 * n / sampleRateHz).toInt()).coerceIn(1, n / 2 - 1)
    val highBin = ((1400.0 * n / sampleRateHz).toInt()).coerceIn(lowBin, n / 2 - 1)
    var strongest = lowBin
    var maxEnergy = 0.0
    for (bin in lowBin..highBin) {
        val energy = re[bin] * re[bin] + im[bin] * im[bin]
        if (energy > maxEnergy) { maxEnergy = energy; strongest = bin }
    }
    val hz = (strongest * sampleRateHz / n).coerceIn(75.0, 1400.0)
    return (ln(hz / 75.0) / ln(1400.0 / 75.0)).toFloat().coerceIn(0f, 1f)
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
                    label = "Equalizer",
                    onClick = onEqualizer,
                    modifier = Modifier.weight(1f),
                    iconContent = { tint ->
                        EqualizerMixerGlyph(
                            tint = tint,
                            modifier = Modifier.size(23.dp)
                        )
                    }
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
    icon: ImageVector? = null,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconContent: (@Composable (Color) -> Unit)? = null
) {
    val colors = LocalMusicColors.current
    val iconTint = if (active) colors.purpleBright else colors.textSecondary
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
                if (iconContent != null) {
                    iconContent(iconTint)
                } else if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
private fun EqualizerMixerGlyph(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.semantics { contentDescription = "Equalizer" }) {
        val stroke = 2.3.dp.toPx()
        val knobWidth = 7.dp.toPx()
        val knobHeight = 4.dp.toPx()
        val xs = floatArrayOf(size.width * 0.22f, size.width * 0.50f, size.width * 0.78f)
        val knobYs = floatArrayOf(size.height * 0.66f, size.height * 0.30f, size.height * 0.52f)

        xs.forEachIndexed { index, x ->
            drawLine(
                color = tint.copy(alpha = 0.72f),
                start = Offset(x, size.height * 0.12f),
                end = Offset(x, size.height * 0.88f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(x - knobWidth / 2f, knobYs[index] - knobHeight / 2f),
                size = Size(knobWidth, knobHeight),
                cornerRadius = CornerRadius(knobHeight / 2f, knobHeight / 2f)
            )
        }
    }
}
@Composable
private fun QueuePanel(
    controller: MusicController,
    onClose: () -> Unit,
    onTools: (SongItem) -> Unit
) {
    val colors = LocalMusicColors.current
    val queue = controller.queue.toList()
    val selectedIds = remember { mutableStateListOf<String>() }
    val queueListState = rememberLazyListState()
    // A new playback queue invalidates any previous checkbox selections.
    LaunchedEffect(queue.map { it.id }) {
        selectedIds.clear()
        if (queue.isNotEmpty()) {
            queueListState.scrollToItem(controller.currentIndex.coerceIn(0, queue.lastIndex))
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.52f))
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
                    IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng danh sách phát", tint = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (queue.isNotEmpty()) {
                                controller.playCollection(queue, 0)
                                onClose()
                            }
                        },
                        enabled = queue.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(mwString("smart.play_all", "Phát tất cả"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            if (queue.isNotEmpty()) {
                                controller.playCollection(queue.shuffled(), 0)
                                onClose()
                            }
                        },
                        enabled = queue.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(mwString("smart.shuffle", "Ngẫu nhiên"), color = colors.purpleBright, maxLines = 1)
                    }
                }

                if (queue.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedIds.size}/${queue.size} bài đã chọn",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (selectedIds.size == queue.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(queue.map { it.id })
                            }
                        }) {
                            Text(
                                if (selectedIds.size == queue.size)
                                    mwString("library.unselect_all", "Bỏ chọn")
                                else mwString("library.select_all", "Chọn tất cả"),
                                color = colors.purpleBright
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val chosen = queue.filter { it.id in selectedIds }
                            if (chosen.isNotEmpty()) {
                                controller.playCollection(chosen, 0)
                                onClose()
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Phát đã chọn (${selectedIds.size})")
                    }
                }

                Spacer(Modifier.height(8.dp))
                if (queue.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Danh sách đang trống", color = colors.textSecondary)
                    }
                } else {
                    // Dialog owns its list viewport. All tracks are scrollable without
                    // moving the Now Playing page underneath the overlay.
                    LazyColumn(
                        state = queueListState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(queue, key = { index, item -> "$index:${item.id}" }) { absoluteIndex, item ->
                            LaunchedEffect(item.id) { controller.ensureArtwork(item) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (item.id !in selectedIds) selectedIds.add(item.id)
                                        } else selectedIds.remove(item.id)
                                    },
                                    modifier = Modifier.size(40.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                )
                                SongRow(
                                    song = item,
                                    artworkUrl = controller.artworkFor(item),
                                    isCurrent = absoluteIndex == controller.currentIndex,
                                    isFavorite = controller.isFavorite(item),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        controller.playQueueIndex(absoluteIndex)
                                        onClose()
                                    },
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
    fun advanced() = when(lang) { "en" -> "Advanced (10 points)"; "zh" -> "高级 (10点)"; "ja" -> "高度 (10ポイント)"; "ko" -> "고급 (10포인트)"; "es" -> "Avanzado (10 puntos)"; "fr" -> "Avancé (10 points)"; "ru" -> "Расширенный (10 точек)"; else -> "Nâng cao (10 điểm)" }
    fun reset() = when(lang) { "en" -> "Reset"; "zh" -> "重置"; "ja" -> "リセット"; "ko" -> "초기화"; "es" -> "Restablecer"; "fr" -> "Réinitialiser"; else -> "Đặt lại" }
    fun advancedEq() = when(lang) { "en" -> "Advanced EQ"; "zh" -> "高级均衡器"; "ja" -> "高度なEQ"; "ko" -> "고급 EQ"; "es" -> "Ecualizador avanzado"; else -> "EQ nâng cao" }
    fun bandDesc() = when(lang) { "en" -> "10 control points · -6 dB to +6 dB"; "zh" -> "10个控制点 · -6 dB 至 +6 dB"; "ja" -> "10コントロールポイント · -6 dB 〜 +6 dB"; "ko" -> "10개 조절점 · -6 dB ~ +6 dB"; "es" -> "10 puntos de control · -6 dB a +6 dB"; "fr" -> "10 points de contrôle · -6 dB à +6 dB"; "ru" -> "10 точек управления · от -6 до +6 дБ"; else -> "10 điểm điều khiển · -6 dB đến +6 dB" }
    fun flat() = when(lang) { "en" -> "Flat"; "zh" -> "平坦"; "ja" -> "フラット"; "ko" -> "평면"; "es" -> "Plano"; else -> "Đặt phẳng" }
    fun done() = when(lang) { "en" -> "Done"; "zh" -> "完成"; "ja" -> "完了"; "ko" -> "완료"; "es" -> "Hecho"; "fr" -> "Terminé"; else -> "Xong" }
    fun openFile() = when(lang) { "en" -> "Open file"; "zh" -> "打开文件"; "ja" -> "ファイルを開く"; "ko" -> "파일 열기"; else -> "Mở tệp" }
    fun close() = when(lang) { "en" -> "Close"; "zh" -> "关闭"; "ja" -> "閉じる"; "ko" -> "닫기"; else -> "Đóng" }
    fun errStartPlay() = when(lang) { "en" -> "Start playing music to enable EQ."; "zh" -> "开始播放音乐以启用EQ。"; else -> "Bắt đầu phát nhạc để kích hoạt EQ trên phiên âm thanh." }
    fun errNoFx() = when(lang) { "en" -> "Device does not provide AudioEffect."; "zh" -> "设备不提供AudioEffect。"; else -> "Thiết bị này chưa cung cấp AudioEffect cho phiên phát hiện tại." }
}

/** Separate quick Master Volume control on the Now Playing screen.
 * The value is saved and applied by MusicController; it never changes Android system volume.
 */
@Composable
private fun MasterVolumeDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    val palette = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 390.dp),
            shape = RoundedCornerShape(20.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (controller.soundMasterVolumePercent == 0)
                            Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = palette.purpleBright,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        mwString("soundstudio.master_volume", "Âm lượng MeloWave"),
                        modifier = Modifier.weight(1f),
                        color = palette.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = palette.textSecondary)
                    }
                }
                Text(
                    mwString(
                        "soundstudio.master_volume_hint",
                        "Chỉ điều chỉnh nhạc trong ứng dụng, không đổi âm lượng hệ thống"
                    ),
                    color = palette.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "${controller.soundMasterVolumePercent}%",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = palette.purpleBright,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
                Slider(
                    value = controller.soundMasterVolumePercent.toFloat(),
                    onValueChange = { controller.setSoundMasterVolume(it.roundToInt()) },
                    valueRange = 0f..100f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0%", color = palette.textMuted, fontSize = 11.sp)
                    TextButton(onClick = { controller.setSoundMasterVolume(100) }) {
                        Text("100%", color = palette.purpleBright, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Sound Studio settings deliberately open in their own compact dialog.
 * Neither the EQ sliders nor the underlying NowPlaying layout need extra scrolling.
 */
@Composable
private fun SoundStudioProDialog(
    controller: MusicController,
    onDismiss: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val palette = LocalMusicColors.current
    val locked = MeloWarePremiumAccess.equalizerRequiresPremium
    var customPresetName by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val deviceScroll = rememberScrollState()
    val fxScroll = rememberScrollState()
    val dspScroll = rememberScrollState()

    LaunchedEffect(Unit) { controller.refreshSoundOutput() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
                .widthIn(max = 620.dp),
            shape = RoundedCornerShape(26.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.purple.copy(alpha = 0.48f)),
            shadowElevation = 22.dp,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = palette.purple.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, palette.purpleBright.copy(alpha = 0.38f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EqualizerMixerGlyph(
                                tint = palette.purpleBright,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            mwString("soundstudio.title", "Sound Studio Pro"),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        )
                        Text(
                            mwString("soundstudio.subtitle", "Âm lượng · EQ 10 điểm · R128 · Limiter"),
                            color = palette.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = palette.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    SoundStudioTabButton(
                        label = mwString("soundstudio.device", "Thiết bị"),
                        icon = Icons.Rounded.Speaker,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    SoundStudioTabButton(
                        label = "FX",
                        icon = Icons.Rounded.Tune,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    SoundStudioTabButton(
                        label = "DSP",
                        icon = Icons.Rounded.GraphicEq,
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    when (selectedTab) {
                        0 -> SoundStudioDevicePage(
                            controller = controller,
                            locked = locked,
                            scrollState = deviceScroll,
                            customPresetName = customPresetName,
                            onPresetNameChange = { customPresetName = it.take(40) },
                            onPresetSaved = { customPresetName = "" }
                        )
                        1 -> SoundStudioFxPage(
                            controller = controller,
                            locked = locked,
                            scrollState = fxScroll
                        )
                        else -> SoundStudioDspPage(
                            controller = controller,
                            locked = locked,
                            scrollState = dspScroll
                        )
                    }
                }

                if (locked) {
                    Button(
                        onClick = onOpenPremium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.purpleBright),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text(mwString("soundstudio.unlock", "Mở khóa với Premium"))
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun SoundStudioTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.purple.copy(alpha = 0.22f) else palette.backgroundSoft,
        border = BorderStroke(
            1.dp,
            if (selected) palette.purpleBright else palette.border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) palette.purpleBright else palette.textSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = if (selected) palette.purpleBright else palette.textSecondary,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SoundStudioDevicePage(
    controller: MusicController,
    locked: Boolean,
    scrollState: androidx.compose.foundation.ScrollState,
    customPresetName: String,
    onPresetNameChange: (String) -> Unit,
    onPresetSaved: () -> Unit
) {
    val palette = LocalMusicColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SoundStudioSectionCard(
            icon = Icons.Rounded.Speaker,
            title = mwString("soundstudio.device", "Thiết bị âm thanh"),
            subtitle = controller.soundOutputName
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (controller.soundPresetSaved)
                        mwString("soundstudio.saved", "Đã có cấu hình riêng cho thiết bị này")
                    else mwString("soundstudio.not_saved", "Chưa lưu cấu hình riêng cho thiết bị này"),
                    modifier = Modifier.weight(1f),
                    color = if (controller.soundPresetSaved) palette.purpleBright else palette.textMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = controller::refreshSoundOutput) {
                    Text(
                        mwString("soundstudio.refresh", "Làm mới"),
                        color = palette.purpleBright,
                        fontSize = 10.sp
                    )
                }
            }

            SoundStudioToggleRow(
                title = mwString("soundstudio.auto_eq", "Tự áp dụng cấu hình khi kết nối"),
                subtitle = mwString("soundstudio.auto_eq_hint", "Mỗi tai nghe hoặc loa dùng cấu hình âm thanh đã lưu"),
                checked = controller.soundAutoApplyDeviceEQ,
                enabled = !locked,
                onCheckedChange = controller::setSoundAutoApplyDeviceEQ
            )

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(
                    onClick = controller::saveSoundProfileForCurrentOutput,
                    enabled = !locked,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.purpleBright),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        mwString("soundstudio.save_eq", "Lưu cấu hình thiết bị"),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
                if (controller.soundPresetSaved) {
                    OutlinedButton(
                        onClick = controller::removeSoundProfileForCurrentOutput,
                        enabled = !locked,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp)
                    ) {
                        Text(
                            mwString("soundstudio.delete_eq", "Bỏ cấu hình"),
                            fontSize = 10.sp,
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }

        SoundStudioSectionCard(
            icon = Icons.Rounded.AutoAwesome,
            title = mwString("soundstudio.auto_eq_mode", "Auto EQ theo thể loại"),
            subtitle = mwString("soundstudio.auto_eq_mode_hint", "Tắt, chỉ gợi ý hoặc tự áp dụng preset khi chuyển bài")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    AutoEqMode.OFF to mwString("soundstudio.auto_eq_off", "Tắt"),
                    AutoEqMode.SUGGEST to mwString("soundstudio.auto_eq_suggest", "Gợi ý"),
                    AutoEqMode.AUTO to mwString("soundstudio.auto_eq_auto", "Tự động")
                ).forEach { (mode, label) ->
                    SoundStudioChoiceButton(
                        label = label,
                        selected = controller.autoEqMode == mode,
                        enabled = !locked,
                        onClick = { controller.setAutoEqMode(mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            controller.autoEqSuggestionPreset?.let { suggested ->
                Spacer(Modifier.height(7.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.purpleBright.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, palette.purpleBright.copy(alpha = 0.32f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            mwString("soundstudio.auto_eq_suggestion", "Gợi ý") +
                                    ": $suggested" +
                                    controller.autoEqSuggestionGenre?.let { " · $it" }.orEmpty(),
                            modifier = Modifier.weight(1f),
                            color = palette.textPrimary,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = controller::applyAutoEqSuggestion,
                            enabled = !locked
                        ) {
                            Text(
                                mwString("soundstudio.apply_suggestion", "Áp dụng"),
                                color = palette.purpleBright,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        SoundStudioSectionCard(
            icon = Icons.Rounded.Tune,
            title = mwString("soundstudio.custom_presets", "Preset của tôi"),
            subtitle = mwString("soundstudio.custom_presets_hint", "Lưu đường cong EQ, Bass, Spatial và Loudness hiện tại")
        ) {
            if (controller.namedEqualizerPresets.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    itemsIndexed(
                        controller.namedEqualizerPresets,
                        key = { _, preset -> preset.id }
                    ) { _, preset ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.cardRaised,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    preset.name,
                                    modifier = Modifier
                                        .clickable { controller.applyNamedEqualizerPreset(preset.id) }
                                        .padding(start = 10.dp, top = 7.dp, bottom = 7.dp),
                                    color = palette.textPrimary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                IconButton(
                                    onClick = { controller.deleteNamedEqualizerPreset(preset.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
            } else {
                Text(
                    mwString("soundstudio.no_custom_presets", "Chưa có preset tùy chỉnh"),
                    color = palette.textMuted,
                    fontSize = 10.sp
                )
                Spacer(Modifier.height(7.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customPresetName,
                    onValueChange = onPresetNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = {
                        Text(
                            mwString("soundstudio.preset_name", "Tên preset"),
                            fontSize = 10.sp
                        )
                    }
                )
                Spacer(Modifier.width(7.dp))
                Button(
                    onClick = {
                        if (controller.saveNamedEqualizerPreset(customPresetName)) {
                            onPresetSaved()
                        }
                    },
                    enabled = !locked && customPresetName.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.purpleBright)
                ) {
                    Text(mwString("soundstudio.save_preset", "Lưu"), fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun SoundStudioFxPage(
    controller: MusicController,
    locked: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SoundStudioSliderCard(
            icon = Icons.Rounded.VolumeUp,
            title = mwString("soundstudio.master_volume", "Âm lượng MeloWave"),
            subtitle = mwString("soundstudio.master_volume_hint", "Chỉ điều chỉnh nhạc trong ứng dụng, không đổi âm lượng hệ thống"),
            valueText = "${controller.soundMasterVolumePercent}%",
            value = controller.soundMasterVolumePercent.toFloat(),
            valueRange = 0f..100f,
            steps = 19,
            onValueChange = { controller.setSoundMasterVolume(it.roundToInt()) },
            startLabel = "0%",
            endLabel = "100%"
        )

        SoundStudioSliderCard(
            icon = Icons.Rounded.Speaker,
            title = mwString("soundstudio.bass_boost", "Bass Boost riêng"),
            subtitle = when {
                controller.audioSessionId <= 0 -> mwString("soundstudio.play_to_detect", "Phát nhạc để kiểm tra khả năng thiết bị")
                !controller.equalizerBassBoostAvailable -> mwString("soundstudio.bass_unavailable", "Thiết bị không hỗ trợ Bass Boost phần cứng")
                controller.equalizerBassBoostStrengthSupported -> mwString("soundstudio.bass_variable", "Thiết bị hỗ trợ điều chỉnh nhiều mức")
                else -> mwString("soundstudio.bass_on_off", "Thiết bị chỉ hỗ trợ bật hoặc tắt")
            },
            valueText = "${controller.equalizerBassBoostStrength}%",
            value = controller.equalizerBassBoostStrength.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            enabled = !locked,
            onValueChange = { controller.updateEqualizerBassBoostStrength(it.roundToInt()) },
            startLabel = "0%",
            endLabel = "100%"
        )

        SoundStudioSliderCard(
            icon = Icons.Rounded.AutoAwesome,
            title = mwString("soundstudio.spatial", "Không gian âm thanh"),
            subtitle = when {
                controller.audioSessionId <= 0 -> mwString("soundstudio.play_to_detect", "Phát nhạc để kiểm tra khả năng thiết bị")
                !controller.equalizerVirtualizerAvailable -> mwString("soundstudio.spatial_unavailable", "Thiết bị không hỗ trợ Virtualizer/Spatial Audio")
                controller.equalizerVirtualizerStrengthSupported -> mwString("soundstudio.spatial_variable", "Mở rộng âm trường stereo theo nhiều mức")
                else -> mwString("soundstudio.spatial_on_off", "Thiết bị chỉ hỗ trợ bật hoặc tắt")
            },
            valueText = "${controller.equalizerVirtualizerStrength}%",
            value = controller.equalizerVirtualizerStrength.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            enabled = !locked && controller.equalizerVirtualizerAvailable,
            onValueChange = { controller.updateEqualizerVirtualizerStrength(it.roundToInt()) },
            startLabel = "0%",
            endLabel = "100%"
        )

        SoundStudioSliderCard(
            icon = Icons.Rounded.ElectricBolt,
            title = mwString("soundstudio.loudness", "Tăng âm"),
            subtitle = mwString("soundstudio.loudness_hint", "Tăng âm lượng cảm nhận; tự nén đỉnh để hạn chế méo tiếng"),
            valueText = "+${controller.equalizerLoudnessGainDb} dB",
            value = controller.equalizerLoudnessGainDb.toFloat(),
            valueRange = 1f..6f,
            steps = 4,
            enabled = !locked && controller.equalizerLoudnessEnabled,
            onValueChange = { controller.updateEqualizerLoudnessGainDb(it.roundToInt()) },
            startLabel = "+1",
            endLabel = "+6",
            switchChecked = controller.equalizerLoudnessEnabled,
            onSwitchChange = controller::setEqualizerLoudness
        )

        SoundStudioSectionCard(
            icon = Icons.Rounded.Info,
            title = mwString("soundstudio.auto_headroom", "Tự bảo vệ EQ/Bass/Spatial"),
            subtitle = when {
                !controller.equalizerAutoHeadroomEnabled ->
                    mwString("soundstudio.headroom_off", "Đã tắt; EQ, Bass Boost hoặc Spatial cao có thể gây vỡ tiếng")
                controller.equalizerHeadroomDb <= 0.05f ->
                    mwString("soundstudio.headroom_safe", "EQ, Bass Boost và Spatial đang ở mức an toàn")
                else -> mwString("soundstudio.headroom_active", "Đang bảo vệ EQ/Bass/Spatial") +
                        " -${String.format(java.util.Locale.US, "%.1f", controller.equalizerHeadroomDb)} dB"
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (controller.equalizerAutoHeadroomEnabled)
                        String.format(java.util.Locale.US, "Headroom %.1f dB", controller.equalizerHeadroomDb)
                    else mwString("soundstudio.off", "Tắt"),
                    modifier = Modifier.weight(1f),
                    color = if (controller.equalizerAutoHeadroomEnabled) palette.purpleBright else palette.textMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = controller.equalizerAutoHeadroomEnabled,
                    enabled = !locked,
                    onCheckedChange = controller::setEqualizerAutoHeadroom
                )
            }
        }

        SoundStudioSliderCard(
            icon = Icons.Rounded.GraphicEq,
            title = mwString("soundstudio.crossfade", "Chuyển bài Crossfade"),
            subtitle = mwString("soundstudio.crossfade_hint", "Hai bài chồng âm khi chuyển tiếp. Không áp dụng cho Cast, Movie và P2P."),
            valueText = if (controller.soundCrossfadeSeconds == 0)
                mwString("soundstudio.off", "Tắt") else "${controller.soundCrossfadeSeconds}s",
            value = controller.soundCrossfadeSeconds.toFloat(),
            valueRange = 0f..10f,
            steps = 9,
            enabled = !locked,
            onValueChange = { controller.setSoundCrossfadeSeconds(it.roundToInt()) },
            startLabel = "0s",
            endLabel = "10s"
        )
    }
}

@Composable
private fun SoundStudioDspPage(
    controller: MusicController,
    locked: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val palette = LocalMusicColors.current
    val integratedText = if (controller.measuredIntegratedLufs <= -69f) "--" else
        String.format(java.util.Locale.US, "%.1f", controller.measuredIntegratedLufs)
    val momentaryText = if (controller.measuredMomentaryLufs <= -69f) "--" else
        String.format(java.util.Locale.US, "%.1f", controller.measuredMomentaryLufs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SoundStudioSliderCard(
            icon = Icons.Rounded.GraphicEq,
            title = mwString("soundstudio.normalization", "Cân bằng âm lượng R128"),
            subtitle = mwString("soundstudio.normalization_hint", "Tự bù bài nhỏ và giảm bài quá lớn theo độ lớn cảm nhận"),
            valueText = String.format(java.util.Locale.US, "%.0f LUFS", controller.loudnessTargetLufs),
            value = controller.loudnessTargetLufs,
            valueRange = -23f..-9f,
            steps = 13,
            enabled = !locked && controller.loudnessNormalizationEnabled,
            onValueChange = controller::setLoudnessTargetLufs,
            startLabel = "-23",
            endLabel = "-9",
            switchChecked = controller.loudnessNormalizationEnabled,
            onSwitchChange = controller::setLoudnessNormalization
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SoundStudioMetricPill("M", "$momentaryText LUFS", Modifier.weight(1f))
            SoundStudioMetricPill("I", "$integratedText LUFS", Modifier.weight(1f))
            SoundStudioMetricPill(
                "Gain",
                String.format(java.util.Locale.US, "%+.1f dB", controller.measuredNormalizationGainDb),
                Modifier.weight(1f)
            )
        }

        SoundStudioSliderCard(
            icon = Icons.Rounded.ElectricBolt,
            title = mwString("soundstudio.dynamic_limiter", "Dynamic Peak Limiter"),
            subtitle = mwString("soundstudio.limiter_hint", "Look-ahead 8 ms, attack 2 ms, release 80 ms"),
            valueText = String.format(java.util.Locale.US, "%.1f dBFS", controller.dynamicLimiterCeilingDb),
            value = controller.dynamicLimiterCeilingDb,
            valueRange = -6f..-0.1f,
            steps = 10,
            enabled = !locked && controller.dynamicLimiterEnabled,
            onValueChange = controller::setDynamicLimiterCeilingDb,
            startLabel = "-6",
            endLabel = "-0.1",
            switchChecked = controller.dynamicLimiterEnabled,
            onSwitchChange = controller::setDynamicLimiter
        )
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SoundStudioMetricPill(
                "Peak",
                String.format(java.util.Locale.US, "%.1f dBFS", controller.measuredInputPeakDbfs),
                Modifier.weight(1f)
            )
            SoundStudioMetricPill(
                "GR",
                String.format(java.util.Locale.US, "%.1f dB", controller.measuredLimiterReductionDb),
                Modifier.weight(1f)
            )
        }

        Surface(
            shape = RoundedCornerShape(13.dp),
            color = palette.purple.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, palette.purpleBright.copy(alpha = 0.30f))
        ) {
            Row(
                modifier = Modifier.padding(11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = palette.purpleBright,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    mwString("soundstudio.dsp_hint", "R128/LUFS cân bằng âm lượng và Limiter chặn đỉnh PCM"),
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun SoundStudioSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    val palette = LocalMusicColors.current
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = palette.backgroundSoft,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(30.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = palette.purple.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = palette.purpleBright,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        color = palette.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    subtitle?.let {
                        Text(
                            it,
                            color = palette.textSecondary,
                            fontSize = 9.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun SoundStudioSliderCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    startLabel: String,
    endLabel: String,
    enabled: Boolean = true,
    switchChecked: Boolean? = null,
    onSwitchChange: ((Boolean) -> Unit)? = null
) {
    val palette = LocalMusicColors.current
    SoundStudioSectionCard(icon = icon, title = title, subtitle = subtitle) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                startLabel,
                color = palette.textMuted,
                fontSize = 9.sp,
                modifier = Modifier.width(34.dp)
            )
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            Text(
                endLabel,
                color = palette.textMuted,
                fontSize = 9.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(34.dp)
            )
            if (switchChecked != null && onSwitchChange != null) {
                Spacer(Modifier.width(5.dp))
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onSwitchChange,
                    enabled = !MeloWarePremiumAccess.equalizerRequiresPremium
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                valueText,
                color = if (enabled || switchChecked == true) palette.purpleBright else palette.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SoundStudioToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalMusicColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = palette.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitle,
                color = palette.textSecondary,
                fontSize = 9.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SoundStudioChoiceButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) palette.purpleBright else palette.border),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) palette.purpleBright else palette.textSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun SoundStudioMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = palette.backgroundSoft,
        border = BorderStroke(1.dp, palette.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = palette.textMuted, fontSize = 8.sp)
            Text(
                value,
                color = palette.purpleBright,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
private data class EqPreset(
    val name: String,
    val curveDb: List<Int>?,
    val bassBoostStrength: Int = 0,
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
            EqPreset(eqStrings.custom(), null, 0, Icons.Rounded.Tune),
            EqPreset("Normal", List(10) { 0 }, 0, Icons.Rounded.Remove),
            EqPreset("Bass Boost", listOf(6, 6, 5, 3, 1, 0, 0, 0, 0, 0), 35, Icons.Rounded.Speaker),
            EqPreset("Pop", listOf(-1, 1, 3, 4, 4, 3, 1, 0, 1, 2), 0, Icons.Rounded.Stars),
            EqPreset("Rock", listOf(5, 3, 1, 0, -1, 1, 3, 4, 5, 5), 0, Icons.Rounded.ElectricBolt),
            EqPreset("Jazz", listOf(4, 3, 1, 2, -1, -1, 0, 1, 3, 4), 0, Icons.Rounded.Piano),
            EqPreset("Classic", listOf(4, 3, 2, 2, -1, -1, 0, 2, 3, 4), 0, Icons.Rounded.Album),
            EqPreset("Vocal", listOf(-2, -1, 1, 3, 5, 4, 3, 1, 0, -1), 0, Icons.Rounded.Mic),
            EqPreset("Electronic", listOf(5, 4, 1, 0, -2, 2, 1, 4, 5, 4), 0, Icons.Rounded.ElectricBolt),
        )
    }

    var showAdvancedDialog by rememberSaveable { mutableStateOf(false) }
    var showPremiumRequired by rememberSaveable { mutableStateOf(false) }
    var showSoundStudio by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (controller.equalizerEnabled) controller.refreshEqualizer()
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = if (controller.equalizerEnabled && controller.equalizerReady)
                            palette.purpleBright else palette.textSecondary,
                        modifier = Modifier.size(26.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Equalizer",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = when {
                                !controller.equalizerEnabled -> mwString("soundstudio.eq_bypass", "Đang tắt · Bypass âm thanh gốc")
                                controller.equalizerReady -> "Đang áp dụng cho phiên âm thanh ${controller.audioSessionId}"
                                controller.audioSessionId <= 0 -> eqStrings.errStartPlay()
                                else -> controller.equalizerError ?: eqStrings.errNoFx()
                            },
                            color = if (controller.equalizerEnabled && controller.equalizerReady)
                                palette.purpleBright else palette.textMuted,
                            fontSize = 10.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Switch(
                    checked = controller.equalizerEnabled,
                    onCheckedChange = controller::setEqualizerActive
                )
            }

            Spacer(modifier = Modifier.height(9.dp))
            OutlinedButton(
                onClick = { showSoundStudio = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                border = BorderStroke(1.dp, palette.purpleBright.copy(alpha = 0.52f)),
                shape = RoundedCornerShape(11.dp),
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null,
                    tint = palette.purpleBright, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(mwString("soundstudio.title", "Sound Studio Pro"),
                    color = palette.textPrimary, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(mwString("soundstudio.subtitle", "Âm lượng · EQ 10 điểm · Không gian · Crossfade"),
                    color = palette.textSecondary, fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                            val curve = preset.curveDb
                            if (curve == null) {
                                if (MeloWarePremiumAccess.equalizerRequiresPremium) {
                                    showPremiumRequired = true
                                } else {
                                    controller.prepareAdvancedEqualizerBands()
                                    showAdvancedDialog = true
                                }
                            } else {
                                controller.selectEqualizerCurvePreset(
                                    index = index,
                                    curveDb = curve,
                                    bassBoostStrength = preset.bassBoostStrength
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            EqResponseChart(
                values = controller.equalizerResponseCurveDb,
                bypassed = controller.equalizerCompareOriginal,
                modifier = Modifier.fillMaxWidth().height(100.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        controller.setEqualizerCompareOriginal(!controller.equalizerCompareOriginal)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (controller.equalizerCompareOriginal) palette.purpleBright else palette.border
                    ),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (controller.equalizerCompareOriginal)
                            mwString("soundstudio.compare_original_active", "Đang nghe bản gốc")
                        else mwString("soundstudio.level_matched_ab", "A/B cân bằng mức"),
                        color = if (controller.equalizerCompareOriginal)
                            palette.purpleBright else palette.textPrimary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = { controller.applyEqualizerForCurrentGenre() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = palette.purpleBright,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        mwString("soundstudio.genre_eq", "Theo thể loại"),
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = palette.purpleBright,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(eqStrings.advanced(), color = palette.textPrimary, fontSize = 11.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = controller::resetEqualizer,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(eqStrings.reset(), color = palette.textPrimary, fontSize = 11.sp)
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
            controller.equalizerWarning?.let { warning ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null,
                        tint = palette.textMuted, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(warning, color = palette.textMuted, fontSize = 10.sp,
                        modifier = Modifier.weight(1f), maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showSoundStudio) {
        SoundStudioProDialog(
            controller = controller,
            onDismiss = { showSoundStudio = false },
            onOpenPremium = { showSoundStudio = false; onOpenPremium() }
        )
    }
    if (showAdvancedDialog && !MeloWarePremiumAccess.equalizerRequiresPremium) {
        AdvancedEqDialog(
            values = controller.equalizerReferenceBandValuesDb,
            labels = controller.equalizerReferenceBandFrequenciesHz.map { frequency ->
                when {
                    frequency >= 1_000 && frequency % 1_000 == 0 -> "${frequency / 1_000} kHz"
                    frequency >= 1_000 -> String.format(java.util.Locale.US, "%.1f kHz", frequency / 1_000f)
                    else -> "$frequency Hz"
                }
            },
            eqStrings = eqStrings,
            onValueChange = controller::updateEqualizerReferenceBand,
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
private fun EqResponseChart(
    values: List<Int>,
    bypassed: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    val safe = remember(values, bypassed) {
        val source = if (bypassed) List(10) { 0 } else values
        List(10) { index -> source.getOrElse(index) { 0 }.coerceIn(-6, 6) }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = palette.backgroundSoft,
        border = BorderStroke(
            1.dp,
            if (bypassed) palette.purpleBright.copy(alpha = 0.65f) else palette.border
        )
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mwString("soundstudio.eq_curve", "Đường cong EQ 10 điểm"),
                    color = palette.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (bypassed) mwString("soundstudio.original_signal", "Tín hiệu gốc")
                    else mwString("soundstudio.smooth_180", "Chuyển mượt 180 ms"),
                    color = if (bypassed) palette.purpleBright else palette.textMuted,
                    fontSize = 9.sp
                )
            }
            Spacer(Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val left = 4.dp.toPx()
                val right = size.width - 4.dp.toPx()
                val top = 3.dp.toPx()
                val bottom = size.height - 3.dp.toPx()
                val center = (top + bottom) / 2f
                fun yFor(db: Int): Float = center - (db.coerceIn(-6, 6) / 6f) * ((bottom - top) / 2f)

                listOf(-6, 0, 6).forEach { db ->
                    val y = yFor(db)
                    drawLine(
                        color = if (db == 0) palette.border else palette.borderSoft,
                        start = Offset(left, y),
                        end = Offset(right, y),
                        strokeWidth = if (db == 0) 1.2.dp.toPx() else 0.7.dp.toPx()
                    )
                }
                val points = safe.mapIndexed { index, db ->
                    val x = left + (right - left) * index / (safe.lastIndex.coerceAtLeast(1).toFloat())
                    Offset(x, yFor(db))
                }
                points.zipWithNext().forEach { (start, end) ->
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(palette.purpleBright, palette.blue),
                            startX = left,
                            endX = right
                        ),
                        start = start,
                        end = end,
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                points.forEach { point ->
                    drawCircle(
                        color = if (bypassed) palette.textMuted else palette.purpleBright,
                        radius = 2.7.dp.toPx(),
                        center = point
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("31 Hz", color = palette.textMuted, fontSize = 8.sp)
                Text("1 kHz", color = palette.textMuted, fontSize = 8.sp)
                Text("16 kHz", color = palette.textMuted, fontSize = 8.sp)
            }
        }
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
    labels: List<String>,
    eqStrings: EqLocalStrings,
    onValueChange: (index: Int, value: Int) -> Unit,
    onFlat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalMusicColors.current
    val safeValues = List(10) { index -> values.getOrElse(index) { 0 }.coerceIn(-6, 6) }
    val safeLabels = if (labels.size >= 10) labels.take(10) else listOf(
        "31 Hz", "62 Hz", "125 Hz", "250 Hz", "500 Hz",
        "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .widthIn(max = 650.dp)
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(26.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.purple.copy(alpha = 0.48f)),
            shadowElevation = 22.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = palette.purple.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, palette.purpleBright.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            EqualizerMixerGlyph(
                                tint = palette.purpleBright,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            eqStrings.advancedEq(),
                            color = palette.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            eqStrings.bandDesc(),
                            color = palette.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = eqStrings.close(),
                            tint = palette.textSecondary
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))
                CompactEqCurvePreview(
                    values = safeValues,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )
                Spacer(Modifier.height(8.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = palette.backgroundSoft,
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight()
                                .padding(top = 22.dp, bottom = 25.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("+6", color = palette.textMuted, fontSize = 8.sp)
                            Text("0", color = palette.textSecondary, fontSize = 8.sp)
                            Text("-6", color = palette.textMuted, fontSize = 8.sp)
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            safeValues.forEachIndexed { index, value ->
                                CompactEqFader(
                                    label = safeLabels[index],
                                    value = value,
                                    onValueChange = { onValueChange(index, it) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(9.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onFlat,
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, palette.border),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(eqStrings.flat(), color = palette.textSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.purple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(eqStrings.done(), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactEqCurvePreview(
    values: List<Int>,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    Canvas(modifier = modifier) {
        val left = 5.dp.toPx()
        val right = size.width - 5.dp.toPx()
        val top = 5.dp.toPx()
        val bottom = size.height - 5.dp.toPx()
        val centerY = (top + bottom) / 2f
        val amplitude = (bottom - top) / 2f

        drawRoundRect(
            color = palette.borderSoft.copy(alpha = 0.45f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        drawLine(
            color = palette.border,
            start = Offset(left, centerY),
            end = Offset(right, centerY),
            strokeWidth = 1.dp.toPx()
        )
        val safe = List(10) { index -> values.getOrElse(index) { 0 }.coerceIn(-6, 6) }
        val points = safe.mapIndexed { index, db ->
            val x = left + (right - left) * index / 9f
            val y = centerY - (db / 6f) * amplitude * 0.82f
            Offset(x, y)
        }
        points.zipWithNext().forEach { (start, end) ->
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(palette.purpleBright, palette.blue),
                    startX = left,
                    endX = right
                ),
                start = start,
                end = end,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        points.forEach { point ->
            drawCircle(
                color = palette.purpleBright,
                radius = 2.4.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun CompactEqFader(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMusicColors.current
    val density = LocalDensity.current
    var heightPx by remember { mutableFloatStateOf(1f) }
    val trackPaddingPx = with(density) { 8.dp.toPx() }

    fun valueForY(y: Float): Int {
        val top = trackPaddingPx
        val bottom = (heightPx - trackPaddingPx).coerceAtLeast(top + 1f)
        val normalized = ((y - top) / (bottom - top)).coerceIn(0f, 1f)
        return (6f - normalized * 12f).roundToInt().coerceIn(-6, 6)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDb(value),
            color = if (value == 0) palette.textSecondary else palette.purpleBright,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { heightPx = it.height.toFloat() }
                .semantics {
                    contentDescription = "$label ${formatDb(value)} dB"
                    progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), -6f..6f, 11)
                    setProgress { target ->
                        onValueChange(target.roundToInt().coerceIn(-6, 6))
                        true
                    }
                }
                .pointerInput(heightPx, onValueChange) {
                    detectTapGestures { offset -> onValueChange(valueForY(offset.y)) }
                }
                .pointerInput(heightPx, onValueChange, value) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> onValueChange(valueForY(offset.y)) },
                        onVerticalDrag = { change, _ ->
                            onValueChange(valueForY(change.position.y))
                            change.consume()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val top = 8.dp.toPx()
                val bottom = size.height - 8.dp.toPx()
                val zeroY = (top + bottom) / 2f
                val normalized = (value + 6f) / 12f
                val thumbY = bottom - normalized * (bottom - top)

                for (tick in 0..12) {
                    val y = top + (bottom - top) * (tick / 12f)
                    drawCircle(
                        color = if (tick == 6) palette.textSecondary else palette.border,
                        radius = if (tick == 6) 1.5.dp.toPx() else 1.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
                drawLine(
                    color = palette.border,
                    start = Offset(x, top),
                    end = Offset(x, bottom),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                if (abs(thumbY - zeroY) > 0.5f) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.purpleBright, palette.blue),
                            startY = min(zeroY, thumbY),
                            endY = max(zeroY, thumbY)
                        ),
                        start = Offset(x, zeroY),
                        end = Offset(x, thumbY),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = palette.textMuted.copy(alpha = 0.65f),
                    start = Offset(x - 5.dp.toPx(), zeroY),
                    end = Offset(x + 5.dp.toPx(), zeroY),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawRoundRect(
                    color = palette.purple.copy(alpha = 0.20f),
                    topLeft = Offset(x - 10.dp.toPx(), thumbY - 7.dp.toPx()),
                    size = Size(20.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = if (palette.isDark) Color.White else palette.purpleBright,
                    topLeft = Offset(x - 7.dp.toPx(), thumbY - 4.5.dp.toPx()),
                    size = Size(14.dp.toPx(), 9.dp.toPx()),
                    cornerRadius = CornerRadius(4.5.dp.toPx(), 4.5.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label
                .replace(" Hz", "")
                .replace(" kHz", "k"),
            color = palette.textPrimary,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
private fun formatDb(value: Int): String = if (value > 0) "+$value" else value.toString()