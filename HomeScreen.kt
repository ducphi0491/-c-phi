package duc_phi.music

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

@Composable
internal fun HomeScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onOpenLocalFolders: () -> Unit,
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    val recent = controller.recentForHome()
    val recommended = controller.recommendations()
    val driveSongs = controller.newlyAddedFromDrive()
    var showOnlineSearch by remember { mutableStateOf(false) }
    var showNetworkMenu by remember { mutableStateOf(false) }
    var activeAudioTool by remember { mutableStateOf<AudioToolMode?>(null) }

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
                    accountInitial = controller.personalDriveAccountEmail
                        ?.firstOrNull()?.uppercaseChar()?.toString() ?: "M",
                    onLogoFiveTap = { showOnlineSearch = true },
                    onOpenAccount = onOpenPersonalDrive,
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
                    onOpenLocalFolders = onOpenLocalFolders,
                    onOpenPersonalDrive = onOpenPersonalDrive,
                    onOpenPublicDrive = onOpenPublicDrive
                )
            }

            item {
                // One entry point for all 4 LAN / Internet connection methods.
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showNetworkMenu = true },
                    shape = RoundedCornerShape(18.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.purple.copy(alpha = 0.15f))
                                .border(
                                    BorderStroke(1.dp, colors.purple.copy(alpha = 0.36f)),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            TwoPhonesTransferIcon(
                                tint = colors.purpleBright,
                                modifier = Modifier.size(33.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                mwString("home.network_title", "Lấy nhạc qua mạng"),
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                mwString("home.network_desc", "Kết nối điện thoại và máy tính qua LAN/Wi-Fi hoặc Internet"),
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            Icons.Rounded.KeyboardArrowRight,
                            contentDescription = mwString("home.open_connections", "Mở menu kết nối"),
                            tint = colors.purpleBright
                        )
                    }
                }
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

            // Compact full-width audio tools below the recently-played section.
            // All operations remain offline and keep their existing callbacks.
            item {
                HomeAudioToolsPanel(
                    onConvert = { activeAudioTool = AudioToolMode.CONVERT },
                    onExtract = { activeAudioTool = AudioToolMode.EXTRACT }
                )
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
            HomeAudioToolRow(
                title = mwString("audio_tools.convert", "Đổi định dạng"),
                subtitle = "MP3 · M4A · WAV",
                icon = Icons.Rounded.CompareArrows,
                tint = colors.purpleBright,
                onClick = onConvert
            )
            HomeAudioToolRow(
                title = mwString("audio_tools.extract", "Tách âm thanh từ video"),
                subtitle = mwString("audio_tools.from_video", "Từ video trong máy"),
                icon = Icons.Rounded.Movie,
                tint = colors.magenta,
                onClick = onExtract
            )
        }
    }
}

@Composable
private fun HomeAudioToolRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = colors.card.copy(alpha = if (colors.isDark) 0.79f else 0.87f),
        border = BorderStroke(1.dp, colors.border.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tint.copy(alpha = if (colors.isDark) 0.28f else 0.12f))
                    .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(27.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.purpleBright,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

@Composable
private fun HomeHeader(
    accountInitial: String,
    onLogoFiveTap: () -> Unit,
    onOpenAccount: () -> Unit,
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
        IconButton(onClick = onSettings, modifier = Modifier.size(38.dp)) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = mwString("settings.title", "Cài đặt"),
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(35.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.purple, colors.magenta)))
                .clickable(onClick = onOpenAccount),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = accountInitial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
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
 * Theme-reactive hero. It deliberately uses no fixed headphone bitmap:
 * the background, glow and headphone parts are repainted from LocalMusicColors.
 */
@Composable
private fun HeroMusicCard(
    onOpenLocalFolders: () -> Unit,
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit
) {
    val colors = LocalMusicColors.current
    val shape = RoundedCornerShape(22.dp)
    val heroColors = if (colors.isDark) {
        listOf(
            lerp(colors.background, colors.purple, 0.38f),
            lerp(colors.backgroundSoft, colors.magenta, 0.35f),
            lerp(colors.backgroundSoft, colors.purpleBright, 0.37f)
        )
    } else {
        listOf(
            lerp(colors.card, colors.purple, 0.11f),
            lerp(colors.card, colors.magenta, 0.13f),
            lerp(colors.card, colors.purpleBright, 0.18f)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(212.dp),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.purple.copy(alpha = if (colors.isDark) 0.76f else 0.36f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(heroColors))
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.magenta.copy(alpha = if (colors.isDark) 0.35f else 0.13f), Color.Transparent),
                        center = Offset(w * 0.67f, h * 0.41f),
                        radius = w * 0.47f
                    ),
                    center = Offset(w * 0.67f, h * 0.41f),
                    radius = w * 0.47f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.purpleBright.copy(alpha = if (colors.isDark) 0.24f else 0.12f), Color.Transparent),
                        center = Offset(w * 0.94f, h * 0.22f),
                        radius = w * 0.32f
                    ),
                    center = Offset(w * 0.94f, h * 0.22f),
                    radius = w * 0.32f
                )
            }

            // Draw the earphones into a vector canvas, with no theme-specific PNGs.
            HeroHeadphonesArtwork(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxWidth(0.56f)
                    .height(150.dp)
                    .offset(x = 8.dp, y = 0.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 13.dp)
            ) {
                Text(
                    text = mwString("home.unlimited", "Nhạc không\ngiới hạn"),
                    color = colors.textPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 27.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = mwString("home.hero_desc", "Nghe nhạc Offline và Google Drive\nchất lượng cao"),
                    modifier = Modifier.fillMaxWidth(0.67f),
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
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
                }
            }
        }
    }
}

/** Normalized 200x160 illustration. Its colors come exclusively from the active theme. */
@Composable
private fun HeroHeadphonesArtwork(modifier: Modifier = Modifier) {
    val colors = LocalMusicColors.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas
        val sx = w / 200f
        val sy = h / 160f
        val unit = minOf(sx, sy)
        fun x(v: Float) = v * sx
        fun y(v: Float) = v * sy
        fun at(a: Float, b: Float) = Offset(x(a), y(b))
        val deep = lerp(colors.background, colors.purple, 0.20f)
        val shadow = if (colors.isDark) colors.background else lerp(colors.purple, Color.Black, 0.58f)
        val rim = colors.purpleBright
        val highlight = lerp(colors.purpleBright, Color.White, if (colors.isDark) 0.40f else 0.30f)
        val light = lerp(colors.magenta, Color.White, 0.18f)

        // Inner halo, plus faint reflected circles around the headset.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colors.purpleBright.copy(alpha = 0.28f), Color.Transparent),
                center = at(110f, 85f), radius = x(92f)
            ),
            radius = x(92f), center = at(110f, 85f)
        )
        drawCircle(
            color = colors.magenta.copy(alpha = 0.26f), radius = 2.5f * unit,
            center = at(29f, 36f)
        )
        drawCircle(
            color = highlight.copy(alpha = 0.50f), radius = 1.5f * unit,
            center = at(187f, 29f)
        )

        val band = Path().apply {
            moveTo(x(32f), y(119f))
            cubicTo(x(12f), y(48f), x(48f), y(10f), x(101f), y(12f))
            cubicTo(x(154f), y(9f), x(187f), y(51f), x(171f), y(122f))
        }
        // 3D headband: deep silhouette, accent midlayer, illuminated rim.
        drawPath(band, color = shadow.copy(alpha = 0.64f), style = Stroke(width = 32f * unit, cap = StrokeCap.Round))
        drawPath(
            band,
            brush = Brush.linearGradient(
                listOf(colors.purple, colors.magenta, colors.purpleBright, colors.purple),
                start = at(18f, 33f), end = at(189f, 134f)
            ),
            style = Stroke(width = 23f * unit, cap = StrokeCap.Round)
        )
        drawPath(band, color = deep.copy(alpha = 0.83f), style = Stroke(width = 13f * unit, cap = StrokeCap.Round))
        drawPath(
            band,
            brush = Brush.linearGradient(
                listOf(highlight.copy(alpha = 0.92f), colors.magenta.copy(alpha = 0.88f), rim.copy(alpha = 0.65f)),
                start = at(26f, 10f), end = at(176f, 119f)
            ),
            style = Stroke(width = 3.6f * unit, cap = StrokeCap.Round)
        )

        // Metallic hinges underneath the padded ear cups.
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(deep, rim, shadow)),
            topLeft = at(19f, 93f), size = Size(x(26f), y(35f)),
            cornerRadius = CornerRadius(8f * unit)
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(shadow, rim, deep)),
            topLeft = at(157f, 93f), size = Size(x(26f), y(35f)),
            cornerRadius = CornerRadius(8f * unit)
        )

        // Two softly glowing earcups, with darker inner cushions for depth.
        drawOval(
            brush = Brush.linearGradient(listOf(colors.magenta, rim, colors.purple, deep)),
            topLeft = at(8f, 101f), size = Size(x(56f), y(57f))
        )
        drawOval(
            color = shadow.copy(alpha = 0.78f),
            topLeft = at(19f, 111f), size = Size(x(32f), y(41f))
        )
        drawOval(
            brush = Brush.linearGradient(listOf(rim.copy(alpha = 0.85f), deep, shadow)),
            topLeft = at(23f, 115f), size = Size(x(22f), y(31f))
        )
        drawOval(
            brush = Brush.linearGradient(listOf(highlight, colors.purpleBright, colors.magenta, deep)),
            topLeft = at(139f, 98f), size = Size(x(58f), y(61f))
        )
        drawOval(
            color = shadow.copy(alpha = 0.82f),
            topLeft = at(152f, 108f), size = Size(x(32f), y(43f))
        )
        drawOval(
            brush = Brush.linearGradient(listOf(colors.purpleBright, deep, shadow)),
            topLeft = at(156f, 112f), size = Size(x(23f), y(34f))
        )
        drawOval(
            color = highlight.copy(alpha = 0.76f),
            topLeft = at(139f, 98f), size = Size(x(58f), y(61f)),
            style = Stroke(width = 2.1f * unit)
        )
        drawOval(
            color = light.copy(alpha = 0.66f),
            topLeft = at(8f, 101f), size = Size(x(56f), y(57f)),
            style = Stroke(width = 1.8f * unit)
        )

        // Luminous musical note inside the arch.
        val noteTone = lerp(colors.magenta, Color.White, if (colors.isDark) 0.42f else 0.18f)
        drawLine(noteTone, at(105f, 62f), at(105f, 101f), strokeWidth = 5.0f * unit, cap = StrokeCap.Round)
        drawLine(noteTone, at(105f, 62f), at(136f, 54f), strokeWidth = 5.4f * unit, cap = StrokeCap.Round)
        drawLine(noteTone, at(136f, 54f), at(136f, 92f), strokeWidth = 5.0f * unit, cap = StrokeCap.Round)
        drawOval(
            brush = Brush.linearGradient(listOf(highlight, colors.magenta)),
            topLeft = at(88f, 94f), size = Size(x(20f), y(13f))
        )
        drawOval(
            brush = Brush.linearGradient(listOf(highlight, colors.magenta)),
            topLeft = at(119f, 84f), size = Size(x(20f), y(13f))
        )
    }
}

private enum class HeroSourceType {
    LOCAL_FOLDER,
    PERSONAL_DRIVE,
    PUBLIC_LINK
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
        modifier = modifier.height(66.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (colors.isDark) colors.background.copy(alpha = 0.85f)
            else Color.White.copy(alpha = 0.91f),
        border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else colors.border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(30.dp), contentAlignment = Alignment.Center) {
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
                    HeroSourceType.PERSONAL_DRIVE -> GoogleDriveLogo(Modifier.size(29.dp))
                    HeroSourceType.PUBLIC_LINK -> {
                        Box(
                            modifier = Modifier.size(29.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.purple.copy(alpha = if (colors.isDark) 0.43f else 0.13f))
                                .border(1.dp, colors.purpleBright.copy(alpha = 0.55f), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Link,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
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