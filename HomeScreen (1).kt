package duc_phi.music

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.os.SystemClock
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import java.text.DateFormat
import java.util.Date
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun HomeScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onOpenLocalFolders: () -> Unit,
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit,
    onSettings: () -> Unit,
    googleAccountEmail: String?,
    googleAccountName: String?,
    googleProfilePhotoUrl: String?,
    onGoogleLogin: () -> Unit,
    onGoogleLogout: () -> Unit
) {
    val colors = LocalMusicColors.current
    val recent = controller.recentForHome()
    val recommended = controller.recommendations()
    val driveSongs = controller.newlyAddedFromDrive()
    var showOnlineSearch by remember { mutableStateOf(false) }
    var showNetworkMenu by remember { mutableStateOf(false) }
    var activeAudioTool by remember { mutableStateOf<AudioToolMode?>(null) }
    var showGoogleAccount by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var unreadNotificationCount by remember { mutableIntStateOf(HomeNotificationStore.unreadCount(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Refresh the unread badge when returning from another app or a notification.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                unreadNotificationCount = HomeNotificationStore.unreadCount(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            PlayerBottomArea(
                controller = controller,
                selectedScreen = MusicScreen.HOME
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(colors.background, colors.backgroundSoft, colors.background)
                    )
                )
                .padding(padding)
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item {
                HomeHeader(
                    googleAccountEmail = googleAccountEmail,
                    googleAccountName = googleAccountName,
                    googleProfilePhotoUrl = googleProfilePhotoUrl,
                    unreadNotifications = unreadNotificationCount,
                    onLogoFiveTap = { showOnlineSearch = true },
                    onOpenAccount = { showGoogleAccount = true },
                    onOpenNotifications = {
                        HomeNotificationStore.markAllRead(context)
                        unreadNotificationCount = 0
                        showNotifications = true
                    },
                    onSettings = onSettings
                )
            }

            item {
                HomeSearchBar(
                    initialQuery = controller.libraryQuery,
                    onSearch = { query ->
                        val cleanQuery = query.trim()
                        if (cleanQuery.isNotBlank()) {
                            controller.libraryQuery = cleanQuery
                            controller.libraryTab = LibraryTab.SONGS
                            controller.openLibrary(LibrarySource.ALL)
                        }
                    }
                )
            }

            item {
                HeroMusicCard(
                    onOpenNetworkTransfer = { showNetworkMenu = true },
                    onOpenLocalFolders = onOpenLocalFolders,
                    onOpenPersonalDrive = onOpenPersonalDrive,
                    onOpenPublicDrive = onOpenPublicDrive
                )
            }

            // Keep the audio tools immediately after the source shortcuts,
            // before the scrolling rows of recently played songs.
            item {
                HomeAudioToolsPanel(
                    onConvert = { activeAudioTool = AudioToolMode.CONVERT },
                    onExtract = { activeAudioTool = AudioToolMode.EXTRACT }
                )
            }

            if (recent.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = mwString("home.recent", "Nghe gần đây"),
                        actionText = mwString("home.view_all", "Xem tất cả"),
                        onAction = { controller.openLibrary(LibrarySource.ALL) }
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 10.dp)
                    ) {
                        items(recent, key = { it.id }) { song ->
                            LaunchedEffect(song.id) { controller.ensureArtwork(song) }
                            RecentSongCard(
                                song = song,
                                artworkUrl = controller.artworkFor(song),
                                onClick = { controller.playSong(song, recent) }
                            )
                        }
                    }
                }
            }

            if (recommended.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = mwString("home.recommended", "Đề xuất cho bạn"),
                        actionText = mwString("nav.library", "Thư viện"),
                        onAction = { controller.openLibrary() }
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 10.dp)
                    ) {
                        items(recommended, key = { it.id }) { song ->
                            LaunchedEffect(song.id) { controller.ensureArtwork(song) }
                            RecommendationCard(
                                song = song,
                                artworkUrl = controller.artworkFor(song),
                                onClick = { controller.playSong(song, recommended) }
                            )
                        }
                    }
                }
            }

            if (driveSongs.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = mwString("home.new_drive", "Mới thêm từ Drive"),
                        actionText = mwString("home.view_all", "Xem tất cả"),
                        onAction = { controller.openLibrary(LibrarySource.PERSONAL_DRIVE) }
                    )
                }
                items(driveSongs.take(5), key = { it.id }) { song ->
                    LaunchedEffect(song.id) { controller.ensureArtwork(song) }
                    SongRow(
                        song = song,
                        artworkUrl = controller.artworkFor(song),
                        isCurrent = controller.currentSong?.id == song.id,
                        isFavorite = controller.isFavorite(song),
                        onClick = { controller.playSong(song, driveSongs) },
                        onFavorite = { controller.toggleFavorite(song) }
                    )
                }
            }

            if (recent.isEmpty() && recommended.isEmpty() && driveSongs.isEmpty()) {
                item {
                    EmptyLibraryState(
                        title = mwString("home.empty_title", "Chưa có bài hát"),
                        description = mwString("home.empty_desc", "Quét nhạc trên điện thoại hoặc kết nối Google Drive để bắt đầu."),
                        buttonText = mwString("home.scan_local", "Quét nhạc trên máy"),
                        onButton = onScanLocal
                    )
                }
            }
        }
    }

    if (showNetworkMenu) {
        MeloWareNetworkMenuDialog(
            controller = controller,
            onDismiss = { showNetworkMenu = false }
        )
    }

    activeAudioTool?.let { mode ->
        MeloWaveAudioToolDialog(
            mode = mode,
            onDismiss = { activeAudioTool = null }
        )
    }

    if (showOnlineSearch) {
        OnlineSearchDialog(
            controller = controller,
            onDismiss = { showOnlineSearch = false }
        )
    }

    if (showGoogleAccount) {
        HomeGoogleAccountDialog(
            email = googleAccountEmail,
            name = googleAccountName,
            photoUrl = googleProfilePhotoUrl,
            onLogin = { showGoogleAccount = false; onGoogleLogin() },
            onLogout = { showGoogleAccount = false; onGoogleLogout() },
            onOpenDrive = { showGoogleAccount = false; onOpenPersonalDrive() },
            onSettings = { showGoogleAccount = false; onSettings() },
            onDismiss = { showGoogleAccount = false }
        )
    }

    if (showNotifications) {
        HomeNotificationsDialog(onDismiss = { showNotifications = false })
    }
}

@Composable
private fun HomeAudioToolsPanel(
    onConvert: () -> Unit,
    onExtract: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.purple.copy(alpha = if (colors.isDark) 0.40f else 0.24f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            lerp(colors.card, colors.purple, if (colors.isDark) 0.22f else 0.06f),
                            lerp(colors.card, colors.backgroundSoft, 0.55f),
                            lerp(colors.card, colors.purpleBright, if (colors.isDark) 0.12f else 0.05f)
                        )
                    )
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.purpleBright,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    mwString("audio_tools.section", "Công cụ âm thanh"),
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = colors.green.copy(alpha = if (colors.isDark) 0.13f else 0.08f),
                    border = BorderStroke(1.dp, colors.green.copy(alpha = 0.36f))
                ) {
                    Text(
                        text = "Offline",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        color = colors.green,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                }
            }
            // Hai công cụ nằm CÙNG MỘT HÀNG: tối ưu chiều cao trang chủ.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeAudioToolCompactCard(
                    title = mwString("audio_tools.convert", "Chuyển đổi nhạc"),
                    subtitle = "MP3 · M4A · WAV",
                    iconKind = HomeFeatureIconKind.CONVERT,
                    onClick = onConvert,
                    modifier = Modifier.weight(1f)
                )
                HomeAudioToolCompactCard(
                    title = mwString("audio_tools.extract", "Tách âm thanh"),
                    subtitle = mwString("audio_tools.from_video", "Từ video trong máy"),
                    iconKind = HomeFeatureIconKind.VIDEO,
                    onClick = onExtract,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * These three badges intentionally share one icon style. All gradient stops come
 * from LocalMusicColors, so every accent preset, custom hue, and dark/light mode
 * is handled automatically without adding any drawable resources.
 */
private enum class HomeFeatureIconKind { NETWORK, CONVERT, VIDEO }

@Composable
private fun HomeFeatureIconBadge(
    kind: HomeFeatureIconKind,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val shape = RoundedCornerShape(11.dp)

    // Harmonized gradients: network -> primary/bright, conversion -> primary/
    // subtle cyan, video -> highlight/primary. Black is mixed in for contrast
    // so the white glyph stays visible even with amber and light themes.
    val (rawStart, rawEnd) = when (kind) {
        HomeFeatureIconKind.NETWORK -> Pair(colors.purpleBright, colors.purple)
        HomeFeatureIconKind.CONVERT -> Pair(
            lerp(colors.purple, colors.cyan, 0.13f), colors.purpleBright
        )
        HomeFeatureIconKind.VIDEO -> Pair(colors.magenta, colors.purple)
    }
    val darken = if (colors.isDark) 0.12f else 0.21f
    // Keep white foreground contrast on light accents (lime / amber / cyan),
    // not just on the default purple palette.
    fun badgeColor(raw: Color, extraShade: Float): Color {
        var candidate = lerp(raw, Color.Black, extraShade)
        repeat(8) {
            if (candidate.luminance() > 0.13f) {
                candidate = lerp(candidate, Color.Black, 0.18f)
            }
        }
        return candidate
    }
    val startColor = badgeColor(rawStart, darken)
    val endColor = badgeColor(rawEnd, darken + 0.09f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = shape,
                ambientColor = colors.purple.copy(alpha = 0.18f),
                spotColor = colors.purple.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(startColor, endColor)))
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = if (colors.isDark) 0.27f else 0.36f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.10f)
                    )
                )
            )
        )
        when (kind) {
            HomeFeatureIconKind.NETWORK -> TwoPhonesTransferIcon(
                tint = Color.White,
                modifier = Modifier.size(29.dp)
            )
            HomeFeatureIconKind.CONVERT -> Icon(
                imageVector = Icons.Rounded.CompareArrows,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = Color.White
            )
            HomeFeatureIconKind.VIDEO -> Icon(
                imageVector = Icons.Rounded.Movie,
                contentDescription = null,
                modifier = Modifier.size(25.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HomeAudioToolCompactCard(
    title: String,
    subtitle: String,
    iconKind: HomeFeatureIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val accent = if (iconKind == HomeFeatureIconKind.CONVERT) colors.purpleBright else colors.magenta
    Surface(
        modifier = modifier.height(76.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            lerp(colors.border, accent, if (colors.isDark) 0.23f else 0.14f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            colors.card.copy(alpha = if (colors.isDark) 0.93f else 0.97f),
                            lerp(colors.cardRaised, accent, if (colors.isDark) 0.10f else 0.055f)
                        )
                    )
                )
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeFeatureIconBadge(
                kind = iconKind,
                modifier = Modifier.size(38.dp)
            )
            Spacer(Modifier.width(7.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 9.4.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    googleAccountEmail: String?,
    googleAccountName: String?,
    googleProfilePhotoUrl: String?,
    unreadNotifications: Int,
    onLogoFiveTap: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapAt by remember { mutableLongStateOf(0L) }

    fun registerLogoTap() {
        val now = SystemClock.elapsedRealtime()
        logoTapCount = if (now - lastLogoTapAt <= 1_800L) logoTapCount + 1 else 1
        lastLogoTapAt = now
        if (logoTapCount >= 5) {
            logoTapCount = 0
            lastLogoTapAt = 0L
            onLogoFiveTap()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = ::registerLogoTap),
            verticalArrangement = Arrangement.Center
        ) {
            GradientText(
                text = "MeloWave",
                brush = Brush.horizontalGradient(listOf(colors.textPrimary, colors.purpleBright)),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = mwString("home.tagline", "Âm nhạc của bạn"),
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                maxLines = 1
            )
        }
        Box(modifier = Modifier.size(37.dp), contentAlignment = Alignment.Center) {
            IconButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.Rounded.NotificationsNone,
                    contentDescription = "Thông báo",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(23.dp)
                )
            }
            if (unreadNotifications > 0) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 2.dp, top = 2.dp)
                        .size(8.dp).clip(CircleShape).background(Color(0xFFFF4557))
                )
            }
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(37.dp)) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = mwString("settings.title", "Cài đặt"),
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(4.dp))
        HomeGoogleAvatar(
            email = googleAccountEmail,
            name = googleAccountName,
            photoUrl = googleProfilePhotoUrl,
            onClick = onOpenAccount,
            modifier = Modifier.size(35.dp)
        )
    }
}

@Composable
private fun HomeGoogleAvatar(
    email: String?,
    name: String?,
    photoUrl: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val initial = (name?.trim()?.firstOrNull() ?: email?.firstOrNull())?.uppercaseChar()?.toString()
    // Only a server-verified PAID account gets the crown. Device trial is not a purchase.
    val isPaidPremium = !email.isNullOrBlank() &&
        MeloWarePremiumAccess.accessKind == "paid" &&
        MeloWarePremiumAccess.canUsePaidFeatures
    Box(
        modifier = modifier.then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .padding(top = if (isPaidPremium) 3.dp else 0.dp,
                         end = if (isPaidPremium) 3.dp else 0.dp)
                .clip(CircleShape)
                .background(
                    if (!email.isNullOrBlank()) Brush.linearGradient(listOf(colors.purple, colors.magenta))
                    else Brush.linearGradient(listOf(colors.cardRaised, colors.card))
                )
                .border(1.dp, colors.border.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (email.isNullOrBlank()) {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = "Đăng nhập Google",
                    tint = colors.textSecondary,
                    modifier = Modifier.fillMaxSize().padding(3.dp)
                )
            } else {
                Text(
                    text = initial ?: "G",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Ảnh đại diện tài khoản Google",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        if (isPaidPremium) {
            // Tiny gold crown at the avatar's upper-right corner; does not clip with the photo.
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).size(19.dp),
                shape = CircleShape,
                color = Color(0xFF23162E),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    MeloWarePremiumCrown(modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun HomeGoogleAccountDialog(
    email: String?,
    name: String?,
    photoUrl: String?,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenDrive: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardRaised,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(Modifier.fillMaxWidth().padding(19.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeGoogleAvatar(email, name, photoUrl, onClick = null, modifier = Modifier.size(49.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tài khoản Google", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            if (email == null) "Chưa đăng nhập" else (name?.takeIf { it.isNotBlank() } ?: email),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (email != null && !name.isNullOrBlank()) {
                            Text(email, color = colors.textSecondary, fontSize = 11.sp, maxLines = 1)
                        }
                        if (email != null && MeloWarePremiumAccess.accessKind == "paid" &&
                            MeloWarePremiumAccess.canUsePaidFeatures
                        ) {
                            Text("MeloWave Premium", color = Color(0xFFFFD54F),
                                fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }
                }
                if (email == null) {
                    HomeDialogAction(mwString("settings.google_login", "Đăng nhập Google"), onLogin)
                } else {
                    HomeDialogAction("Mở Google Drive", onOpenDrive)
                    HomeDialogAction(mwString("settings.google_logout", "Đăng xuất"), onLogout)
                }
                HomeDialogAction(mwString("settings.title", "Cài đặt"), onSettings)
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Đóng", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun HomeDialogAction(title: String, onClick: () -> Unit) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Text(
            title,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            color = colors.textPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

// Notification history only includes reminders actually posted by ReminderReceiver.
internal data class HomeNotice(val createdAt: Long, val title: String, val message: String)

internal object HomeNotificationStore {
    private const val PREFS = "melowave_in_app_notifications"
    private const val ENTRIES = "recent_entries"
    private const val LAST_READ = "last_read_time"

    fun record(context: Context, title: String, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldEntries = runCatching { JSONArray(prefs.getString(ENTRIES, "[]")) }.getOrDefault(JSONArray())
        val next = JSONArray().put(
            JSONObject().put("time", System.currentTimeMillis()).put("title", title).put("message", message)
        )
        for (i in 0 until minOf(oldEntries.length(), 19)) {
            oldEntries.optJSONObject(i)?.let { next.put(it) }
        }
        prefs.edit().putString(ENTRIES, next.toString()).apply()
    }

    fun getAll(context: Context): List<HomeNotice> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(ENTRIES, "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                add(HomeNotice(obj.optLong("time"), obj.optString("title"), obj.optString("message")))
            }
        }
    }

    fun unreadCount(context: Context): Int {
        val seen = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_READ, 0L)
        return getAll(context).count { it.createdAt > seen }
    }

    fun markAllRead(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAST_READ, System.currentTimeMillis()).apply()
    }
}

@Composable
private fun HomeNotificationsDialog(onDismiss: () -> Unit) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val notices = remember { HomeNotificationStore.getAll(context) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.cardRaised,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(8.dp))
                    Text("Thông báo", modifier = Modifier.weight(1f), color = colors.textPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                if (notices.isEmpty()) {
                    Text(
                        "Chưa có thông báo nào. Các lời nhắc nghe nhạc được gửi từ ứng dụng sẽ xuất hiện tại đây.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 335.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        notices.forEach { notice ->
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = colors.card,
                                border = BorderStroke(1.dp, colors.border.copy(alpha = 0.65f))
                            ) {
                                Column(Modifier.fillMaxWidth().padding(11.dp)) {
                                    Text(notice.title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(notice.message, color = colors.textSecondary, fontSize = 12.sp, lineHeight = 17.sp)
                                    Text(
                                        remember(notice.createdAt) {
                                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                                                .format(Date(notice.createdAt))
                                        },
                                        color = colors.textSecondary.copy(alpha = 0.73f), fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}"))
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cài đặt thông báo", color = colors.purpleBright)
                }
            }
        }
    }
}

@Composable
private fun HomeSearchBar(
    initialQuery: String,
    onSearch: (String) -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val currentOnSearch by rememberUpdatedState(onSearch)
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim().orEmpty()
            if (spokenText.isNotBlank()) {
                query = spokenText
                currentOnSearch(spokenText)
            }
        }
    }

    val voicePrompt = mwString("home.voice_prompt", "Nói tên bài hát, ca sĩ hoặc album")
    val voiceUnavailable = mwString("home.voice_unavailable", "Thiết bị chưa có dịch vụ nhận dạng giọng nói.")

    fun startVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, voiceUnavailable, Toast.LENGTH_LONG).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(51.dp),
        shape = RoundedCornerShape(23.dp),
        color = colors.cardRaised.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (query.isNotBlank()) currentOnSearch(query) },
                modifier = Modifier.size(37.dp)
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = mwString("home.search", "Tìm"),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(21.dp)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                singleLine = true,
                cursorBrush = SolidColor(colors.purpleBright),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (query.isNotBlank()) currentOnSearch(query)
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                mwString("home.search_hint", "Tìm bài hát, ca sĩ, album, thể loại..."),
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        inner()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.purple.copy(alpha = if (colors.isDark) 0.26f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = ::startVoiceSearch, modifier = Modifier.size(39.dp)) {
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
}

/**
 * Banner: artwork WebP keeps its details; Kotlin draws all titles and action buttons.
 * All four shortcuts share ONE row: device / Google Drive / Drive link / network transfer.
 */
@Composable
private fun HeroMusicCard(
    onOpenNetworkTransfer: () -> Unit,
    onOpenLocalFolders: () -> Unit,
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit
) {
    val colors = LocalMusicColors.current
    val shape = RoundedCornerShape(22.dp)
    val artwork = heroBannerArtwork(colors)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.purple.copy(alpha = if (colors.isDark) 0.76f else 0.36f))
    ) {
        Column(
            modifier = Modifier.background(
                Brush.verticalGradient(
                    listOf(colors.cardRaised, colors.card, colors.backgroundSoft)
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(155.dp)) {
                Image(
                    painter = painterResource(id = artwork.first),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    // 1536:640 banner preserves the top of the headphone arch on regular phones.
                    // Prefer TOP and RIGHT when additional cropping is needed.
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopEnd,
                    colorFilter = artwork.second
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            listOf(
                                (if (colors.isDark) Color(0xFF120527) else Color.White).copy(alpha = 0.27f),
                                Color.Transparent
                            )
                        )
                    )
                )
                Column(modifier = Modifier.padding(start = 13.dp, top = 13.dp)) {
                    Text(
                        text = mwString("home.unlimited", "Nhạc không\ngiới hạn"),
                        color = if (colors.isDark) Color.White else colors.textPrimary,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 27.sp,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = mwString("home.hero_desc", "Nghe nhạc Offline và Google Drive\nchất lượng cao"),
                        modifier = Modifier.fillMaxWidth(0.63f),
                        color = if (colors.isDark) Color.White.copy(alpha = 0.89f) else colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Four equal-width shortcuts INSIDE the banner: no separate network card.
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 9.dp, end = 9.dp, top = 8.dp, bottom = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                HeroSourceAction(
                    title = mwString("home.local_music", "Nhạc máy"),
                    sourceType = HeroSourceType.LOCAL_FOLDER,
                    onClick = onOpenLocalFolders,
                    modifier = Modifier.weight(1f)
                )
                HeroSourceAction(
                    title = "Google Drive",
                    sourceType = HeroSourceType.PERSONAL_DRIVE,
                    onClick = onOpenPersonalDrive,
                    modifier = Modifier.weight(1f)
                )
                HeroSourceAction(
                    title = mwString("home.drive_link", "Link Drive"),
                    sourceType = HeroSourceType.PUBLIC_LINK,
                    onClick = onOpenPublicDrive,
                    modifier = Modifier.weight(1f)
                )
                HeroSourceAction(
                    title = mwString("home.network_title", "Lấy nhạc qua mạng"),
                    sourceType = HeroSourceType.NETWORK_TRANSFER,
                    onClick = onOpenNetworkTransfer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Tám màu có sẵn: 8 ảnh × dark/light.
 * Màu tuỳ chỉnh: tái sử dụng ảnh gốc và xoay hue bằng ColorMatrix,
 * cho phép tai nghe/đường sóng đổi ngay cả khi kéo thanh chọn màu.
 */
private fun heroBannerArtwork(colors: MusicColors): Pair<Int, ColorFilter?> {
    val option = MusicAccentOptions.firstOrNull { entry ->
        colors.purple == (if (colors.isDark) entry.darkPrimary else entry.lightPrimary)
    }
    if (option == null) {
        val original = if (colors.isDark) DarkMusicColors.purpleBright else LightMusicColors.purpleBright
        val angle = heroHue(colors.purpleBright) - heroHue(original)
        val drawable = if (colors.isDark) R.drawable.hero_headphones_purple_dark
                       else R.drawable.hero_headphones_purple_light
        return drawable to heroHueFilter(angle)
    }
    val imageRes = if (colors.isDark) {
        when (option.id) {
            "blue" -> R.drawable.hero_headphones_blue_dark
            "teal" -> R.drawable.hero_headphones_teal_dark
            "green" -> R.drawable.hero_headphones_green_dark
            "orange" -> R.drawable.hero_headphones_orange_dark
            "pink" -> R.drawable.hero_headphones_pink_dark
            "red" -> R.drawable.hero_headphones_red_dark
            "amber" -> R.drawable.hero_headphones_amber_dark
            else -> R.drawable.hero_headphones_purple_dark
        }
    } else {
        when (option.id) {
            "blue" -> R.drawable.hero_headphones_blue_light
            "teal" -> R.drawable.hero_headphones_teal_light
            "green" -> R.drawable.hero_headphones_green_light
            "orange" -> R.drawable.hero_headphones_orange_light
            "pink" -> R.drawable.hero_headphones_pink_light
            "red" -> R.drawable.hero_headphones_red_light
            "amber" -> R.drawable.hero_headphones_amber_light
            else -> R.drawable.hero_headphones_purple_light
        }
    }
    return imageRes to null
}

private fun heroHue(color: Color): Float {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

/** Xoay sắc độ trong RGB, giữ độ sáng/chi tiết của ảnh WebP cho màu tùy chỉnh. */
private fun heroHueFilter(degrees: Float): ColorFilter {
    val radians = Math.toRadians(degrees.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    return ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0.213f + 0.787f*c - 0.213f*s, 0.715f - 0.715f*c - 0.715f*s, 0.072f - 0.072f*c + 0.928f*s, 0f, 0f,
        0.213f - 0.213f*c + 0.143f*s, 0.715f + 0.285f*c + 0.140f*s, 0.072f - 0.072f*c - 0.283f*s, 0f, 0f,
        0.213f - 0.213f*c - 0.787f*s, 0.715f - 0.715f*c + 0.715f*s, 0.072f + 0.928f*c + 0.072f*s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )))
}

private enum class HeroSourceType {
    LOCAL_FOLDER,
    PERSONAL_DRIVE,
    PUBLIC_LINK,
    NETWORK_TRANSFER
}

@Composable
private fun HeroSourceAction(
    title: String,
    sourceType: HeroSourceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier.height(78.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (colors.isDark) colors.background.copy(alpha = 0.85f)
            else Color.White.copy(alpha = 0.91f),
        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else colors.border)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                when (sourceType) {
                    HeroSourceType.LOCAL_FOLDER -> {
                        Surface(
                            modifier = Modifier.size(29.dp),
                            shape = RoundedCornerShape(9.dp),
                            color = if (colors.isDark) Color(0xFF123B5E) else Color(0xFFDDF2FF),
                            border = BorderStroke(1.dp, Color(0xFF39BDF8).copy(alpha = 0.55f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (colors.isDark) Color(0xFF67D8FF) else Color(0xFF0078AD),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    HeroSourceType.NETWORK_TRANSFER -> {
                        // Theme-aware gradient matches audio tool badges. Preserve
                        // the original two-phone arrows; only their presentation changes.
                        HomeFeatureIconBadge(
                            kind = HomeFeatureIconKind.NETWORK,
                            modifier = Modifier.size(33.dp)
                        )
                    }
                    HeroSourceType.PERSONAL_DRIVE -> GoogleDriveLogo(Modifier.size(29.dp))
                    HeroSourceType.PUBLIC_LINK -> {
                        // Giữ nguyên icon gốc: logo Google Drive + huy hiệu liên kết màu hồng.
                        GoogleDriveLogo(modifier = Modifier.size(29.dp))
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
                            shape = CircleShape,
                            color = colors.magenta,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(9.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 9.5.sp,
                lineHeight = 11.5.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Google Drive logo: hai cạnh ngoài xiên THẲNG liên tục qua chỗ chuyển màu.
 * Chỉ bo cong ở đỉnh trên và hai góc đáy; tam giác rỗng ở giữa hướng xuống 6 giờ.
 * Vẽ trực tiếp trong Canvas, không cần thêm drawable/image.
 */
@Composable
private fun GoogleDriveLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Ba điểm trên mỗi cạnh xiên nằm trên CÙNG MỘT ĐƯỜNG THẲNG:
        // trái: (0.29, 0.14) -> (0.12, 0.48) -> (0.04, 0.64)
        // phải: (0.71, 0.14) -> (0.88, 0.48) -> (0.96, 0.64)
        // Vì vậy hai cạnh không bị cong hay gãy ở ranh giới giữa các dải màu.
        val seamY = h * 0.48f
        val leftSeam = w * 0.12f
        val rightSeam = w * 0.88f
        val cutLeft = w * 0.36f
        val cutRight = w * 0.64f
        val cutTipY = h * 0.705f
        val bottom = h * 0.98f
        val bottomJoin = w * 0.615f

        // Xanh lá: phần vòm đỉnh và hai cạnh xiên thẳng tới ranh giới màu.
        val greenPath = Path().apply {
            moveTo(leftSeam, seamY)
            lineTo(w * 0.29f, h * 0.14f)
            cubicTo(
                w * 0.33f, h * 0.05f,
                w * 0.42f, h * 0.025f,
                w * 0.50f, h * 0.025f
            )
            cubicTo(
                w * 0.58f, h * 0.025f,
                w * 0.67f, h * 0.05f,
                w * 0.71f, h * 0.14f
            )
            lineTo(rightSeam, seamY)
            lineTo(cutRight, seamY)
            lineTo(cutLeft, seamY)
            close()
        }
        drawPath(
            path = greenPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF49C8D7),
                    Color(0xFF10BE6B),
                    Color(0xFF0FBE69)
                ),
                start = androidx.compose.ui.geometry.Offset(w * 0.12f, h * 0.48f),
                end = androidx.compose.ui.geometry.Offset(w * 0.70f, h * 0.025f)
            )
        )

        // Xanh dương: cạnh ngoài bên trái là đường THẲNG từ mối nối
        // tới (0.04w, 0.64h); chỉ dùng Bézier tại góc đáy trái.
        val bluePath = Path().apply {
            moveTo(leftSeam, seamY)
            lineTo(cutLeft, seamY)
            lineTo(w * 0.50f, cutTipY)
            lineTo(bottomJoin, bottom)
            lineTo(w * 0.18f, bottom)
            cubicTo(
                0f, bottom,
                0f, h * 0.72f,
                w * 0.04f, h * 0.64f
            )
            close()
        }
        drawPath(
            path = bluePath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4285F4),
                    Color(0xFF3B7FF4),
                    Color(0xFF8795FF)
                ),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.48f),
                end = androidx.compose.ui.geometry.Offset(w * 0.65f, h)
            )
        )

        // Vàng: đối xứng bên phải, cạnh xiên là đường THẲNG liên tục.
        val yellowPath = Path().apply {
            moveTo(cutRight, seamY)
            lineTo(rightSeam, seamY)
            lineTo(w * 0.96f, h * 0.64f)
            cubicTo(
                w, h * 0.72f,
                w, bottom,
                w * 0.82f, bottom
            )
            lineTo(bottomJoin, bottom)
            lineTo(w * 0.50f, cutTipY)
            close()
        }
        drawPath(
            path = yellowPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD21A),
                    Color(0xFFFFCC00)
                ),
                start = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.48f),
                end = androidx.compose.ui.geometry.Offset(w, bottom)
            )
        )
    }
}

@Composable
private fun RecentSongCard(
    song: SongItem,
    artworkUrl: String?,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 4.dp)
    ) {
        Box {
            ArtworkImage(
                song = song,
                artworkUrl = artworkUrl,
                modifier = Modifier.size(132.dp),
                cornerRadius = 16
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(34.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.62f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = mwString("home.play", "Phát"),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            song.title,
            color = colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            color = colors.textSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecommendationCard(
    song: SongItem,
    artworkUrl: String?,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .width(235.dp)
            .height(118.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtworkImage(
                song = song,
                artworkUrl = artworkUrl,
                modifier = Modifier.size(88.dp),
                cornerRadius = 14
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    song.artist,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun OnlineSearchDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    var query by remember(controller.onlineSearchQuery) {
        mutableStateOf(controller.onlineSearchQuery)
    }

    val onlineSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (spokenText.isNotBlank()) {
                query = spokenText
                controller.onlineSearchQuery = spokenText
                controller.searchOnline(spokenText)
            }
        }
    }

    fun startOnlineVoiceSearch() {
        val sourceName = if (controller.onlineSearchMode == OnlineSearchMode.APPLE_MUSIC) {
            "Music"
        } else {
            "Movie"
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Nói tên bài hát, ca sĩ hoặc video cần tìm trên $sourceName"
            )
        }

        try {
            onlineSpeechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "Thiết bị chưa có dịch vụ nhận dạng giọng nói.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Dialog(
        onDismissRequest = { if (!controller.onlineSearchBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.91f),
            shape = RoundedCornerShape(24.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tìm kiếm Music / Movie",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Nghe Music Preview hoặc xem Movie",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !controller.onlineSearchBusy) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }

                // Hai nua Music / Movie chung mot vien thuoc, khong co khe ho.
                // Vien ngoai co be mat toi lom sau, nua dang chon noi tim sang.
                OnlineModeSelector(
                    selectedMode = controller.onlineSearchMode,
                    onSelectMusic = {
                        controller.changeOnlineSearchMode(OnlineSearchMode.APPLE_MUSIC)
                    },
                    onSelectMovie = {
                        controller.changeOnlineSearchMode(OnlineSearchMode.MOVIE)
                    }
                )

                OnlineSearchInput(
                    query = query,
                    mode = controller.onlineSearchMode,
                    busy = controller.onlineSearchBusy,
                    onQueryChange = { value ->
                        query = value
                        controller.onlineSearchQuery = value
                    },
                    onVoiceSearch = ::startOnlineVoiceSearch,
                    onClear = {
                        query = ""
                        controller.onlineSearchQuery = ""
                    },
                    onSearch = {
                        if (query.isNotBlank()) controller.searchOnline(query)
                    }
                )

                controller.onlineSearchError?.let { message ->
                    Text(
                        text = message,
                        color = colors.red,
                        fontSize = 12.sp
                    )
                }

                if (controller.onlineSearchResults.isEmpty() && !controller.onlineSearchBusy) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (controller.onlineSearchMode == OnlineSearchMode.APPLE_MUSIC) {
                                "Music sử dụng bản nghe thử từ iTunes Search."
                            } else {
                                "Kết quả Movie sẽ được phát tại màn hình Đang phát."
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(controller.onlineSearchResults, key = { it.id }) { song ->
                            SongRow(
                                song = song,
                                artworkUrl = controller.artworkFor(song),
                                isCurrent = controller.currentSong?.id == song.id,
                                isFavorite = controller.isFavorite(song),
                                onClick = {
                                    controller.playOnlineResult(song)
                                    onDismiss()
                                },
                                onFavorite = { controller.toggleFavorite(song) }
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng", color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun OnlineSearchInput(
    query: String,
    mode: OnlineSearchMode,
    busy: Boolean,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit
) {
    val colors = LocalMusicColors.current
    val isApple = mode == OnlineSearchMode.APPLE_MUSIC
    val placeholder = if (isApple) {
        "Tên bài hát, ca sĩ hoặc album..."
    } else {
        "Tên video, bài hát hoặc kênh..."
    }
    val helperText = if (isApple) {
        "Tìm Music · Nhấn micro để tìm bằng giọng nói"
    } else {
        "Tìm Movie · Nhấn micro để tìm bằng giọng nói"
    }
    val modeIcon = if (isApple) Icons.Rounded.MusicNote else Icons.Rounded.Tv
    val canSearch = query.isNotBlank() && !busy

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = colors.cardRaised,
            border = BorderStroke(
                1.dp,
                if (query.isNotBlank()) colors.purpleBright.copy(alpha = 0.72f) else colors.border
            )
        ) {
            Row(
                modifier = Modifier.padding(start = 13.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    tint = colors.purpleBright,
                    modifier = Modifier.size(21.dp)
                )

                Spacer(Modifier.width(10.dp))

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(colors.purpleBright),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { if (canSearch) onSearch() }
                    ),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isBlank()) {
                                Text(
                                    text = placeholder,
                                    color = colors.textMuted,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Khi người dùng bắt đầu gõ, micro tự ẩn để dành chỗ cho xóa/tìm.
                if (query.isBlank() && !busy) {
                    IconButton(
                        onClick = onVoiceSearch,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Tìm kiếm bằng giọng nói",
                            tint = colors.purpleBright,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                if (query.isNotBlank() && !busy) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Xóa từ khóa",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onSearch,
                    enabled = canSearch,
                    modifier = Modifier.size(38.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            strokeWidth = 2.dp,
                            color = colors.purpleBright
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = "Tìm kiếm",
                            tint = if (canSearch) colors.purpleBright else colors.textMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = helperText,
            color = colors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 5.dp)
        )
    }
}

/**
 * Music / Movie segmented selector.
 * One dark recessed outer capsule, two contiguous segments, one elevated purple active half.
 * Search mode and actual online playback callbacks are not changed here.
 */
@Composable
private fun OnlineModeSelector(
    selectedMode: OnlineSearchMode,
    onSelectMusic: () -> Unit,
    onSelectMovie: () -> Unit
) {
    val colors = LocalMusicColors.current
    val outerShape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(
                elevation = 11.dp,
                shape = outerShape,
                ambientColor = Color.Black.copy(alpha = 0.52f),
                spotColor = colors.purple.copy(alpha = 0.22f)
            )
            .clip(outerShape)
            .background(
                // Nền rãnh chìm phía ngoài hai nửa viên thuốc.
                Brush.verticalGradient(
                    if (colors.isDark) listOf(
                        Color(0xFF01040D),
                        Color(0xFF071020),
                        Color(0xFF0F2138)
                    ) else listOf(colors.cardRaised, colors.card, colors.backgroundSoft)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        colors.purpleBright.copy(alpha = 0.36f),
                        Color.White.copy(alpha = 0.09f),
                        colors.purple.copy(alpha = 0.26f)
                    )
                ),
                shape = outerShape
            )
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            OnlineModeButton(
                title = "Music",
                icon = Icons.Rounded.MusicNote,
                selected = selectedMode == OnlineSearchMode.APPLE_MUSIC,
                onClick = onSelectMusic,
                modifier = Modifier.weight(1f)
            )
            OnlineModeButton(
                title = "Movie",
                icon = Icons.Rounded.Tv,
                selected = selectedMode == OnlineSearchMode.MOVIE,
                onClick = onSelectMovie,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OnlineModeButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val segmentShape = RoundedCornerShape(25.dp)
    val colors = LocalMusicColors.current
    val originalPurple = colors.purple == (if (colors.isDark) DarkMusicColors.purple else LightMusicColors.purple)
    val activeTop by animateColorAsState(
        targetValue = if (selected) (if (originalPurple) Color(0xFFAB40FB) else colors.purpleBright) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "onlineModeTop"
    )
    val activeBottom by animateColorAsState(
        targetValue = if (selected) (if (originalPurple) Color(0xFF6223CE) else colors.purple) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "onlineModeBottom"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) Color.White else if (colors.isDark) Color(0xFFAFBCD3) else colors.textSecondary,
        animationSpec = tween(durationMillis = 220),
        label = "onlineModeText"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(
                elevation = if (selected) 7.dp else 0.dp,
                shape = segmentShape,
                ambientColor = (if (originalPurple) Color(0xFF902BF2) else colors.purple).copy(alpha = if (selected) 0.42f else 0f),
                spotColor = (if (originalPurple) Color(0xFFAF4FFF) else colors.purpleBright).copy(alpha = if (selected) 0.56f else 0f)
            )
            .clip(segmentShape)
            .background(Brush.verticalGradient(listOf(activeTop, activeBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (selected) 0.55f else 0f),
                        Color.White.copy(alpha = if (selected) 0.06f else 0f)
                    )
                ),
                shape = segmentShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 9.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = labelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GradientText(
    text: String,
    brush: Brush,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight
) {
    androidx.compose.material3.Text(
        text = text,
        style = TextStyle(
            brush = brush,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    )
}