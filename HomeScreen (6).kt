package duc_phi.music

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun HomeScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    val recent = controller.recentForHome()
    val recommended = controller.recommendations()
    val driveSongs = controller.newlyAddedFromDrive()
    var showOnlineSearch by remember { mutableStateOf(false) }

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
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeHeader(
                    onSearch = { showOnlineSearch = true },
                    onSettings = onSettings
                )
            }

            item {
                HomeSearchBar(
                    initialQuery = controller.onlineSearchQuery,
                    onSearch = { query ->
                        controller.onlineSearchQuery = query
                        showOnlineSearch = true
                        if (query.isNotBlank()) controller.searchOnline(query)
                    }
                )
            }

            item {
                HeroMusicCard(
                    onOpenPersonalDrive = onOpenPersonalDrive,
                    onOpenPublicDrive = onOpenPublicDrive
                )
            }

            if (recent.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "Nghe gần đây",
                        actionText = "Xem tất cả",
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
                        title = "Đề xuất cho bạn",
                        actionText = "Thư viện",
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
                        title = "Mới thêm từ Drive",
                        actionText = "Xem tất cả",
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
                        title = "Chưa có bài hát",
                        description = "Quét nhạc trên điện thoại hoặc kết nối Google Drive để bắt đầu.",
                        buttonText = "Quét nhạc trên máy",
                        onButton = onScanLocal
                    )
                }
            }
        }
    }

    if (showOnlineSearch) {
        OnlineSearchDialog(
            controller = controller,
            onDismiss = { showOnlineSearch = false }
        )
    }
}

@Composable
private fun HomeHeader(
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.Black,
                border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.62f)),
                shadowElevation = 7.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.Black)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo MeloWare",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column {
                GradientText(
                    text = "MeloWare",
                    brush = Brush.horizontalGradient(listOf(colors.purpleBright, colors.magenta)),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Âm nhạc của bạn",
                    color = colors.textSecondary,
                    fontSize = 16.sp
                )
            }
        }
        IconButton(onClick = onSearch) {
            Icon(Icons.Rounded.Search, contentDescription = "Tìm kiếm", tint = colors.textPrimary)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Cài đặt", tint = colors.textPrimary)
        }
    }
}

@Composable
private fun HomeSearchBar(
    initialQuery: String,
    onSearch: (String) -> Unit
) {
    val colors = LocalMusicColors.current
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = colors.cardRaised,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(colors.purpleBright),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isBlank()) {
                            Text(
                                "Tìm bài hát, ca sĩ, album, thể loại...",
                                color = colors.textMuted,
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                }
            )
            IconButton(onClick = { onSearch(query) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Search, contentDescription = "Tìm", tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun HeroMusicCard(
    onOpenPersonalDrive: () -> Unit,
    onOpenPublicDrive: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(224.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF15115B),
                            Color(0xFF3A145D),
                            Color(0xFF0C4B70)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 17.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.magenta.copy(alpha = 0.42f), Color.Transparent)
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Nhạc không\ngiới hạn",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 30.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "Nghe nhạc Offline và Google Drive\nchất lượng cao",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DriveHeroAction(
                        title = "Thư mục Drive",
                        subtitle = "Drive của tôi",
                        showLinkBadge = false,
                        onClick = onOpenPersonalDrive,
                        modifier = Modifier.weight(1f)
                    )
                    DriveHeroAction(
                        title = "Link Drive",
                        subtitle = "Thư mục chia sẻ",
                        showLinkBadge = true,
                        onClick = onOpenPublicDrive,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DriveHeroAction(
    title: String,
    subtitle: String,
    showLinkBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF050B18).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(39.dp),
                contentAlignment = Alignment.Center
            ) {
                GoogleDriveLogo(modifier = Modifier.size(35.dp))
                if (showLinkBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(17.dp),
                        shape = CircleShape,
                        color = colors.magenta,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Link,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.64f),
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Google Drive logo được vẽ bằng ba dải màu, giữ đúng tỷ lệ tam giác
 * và khoảng rỗng ở giữa như biểu tượng Drive hiện hành.
 */
@Composable
private fun GoogleDriveLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val green = Path().apply {
            moveTo(w * 0.36f, h * 0.05f)
            cubicTo(w * 0.32f, h * 0.05f, w * 0.29f, h * 0.08f, w * 0.27f, h * 0.12f)
            lineTo(w * 0.08f, h * 0.48f)
            lineTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.27f)
            lineTo(w * 0.62f, h * 0.48f)
            lineTo(w * 0.92f, h * 0.48f)
            lineTo(w * 0.73f, h * 0.12f)
            cubicTo(w * 0.71f, h * 0.08f, w * 0.68f, h * 0.05f, w * 0.64f, h * 0.05f)
            close()
        }
        drawPath(
            path = green,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF15C46B), Color(0xFF0DBB63)),
                start = androidx.compose.ui.geometry.Offset(w * 0.25f, 0f),
                end = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.48f)
            )
        )

        val blue = Path().apply {
            moveTo(w * 0.08f, h * 0.48f)
            lineTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.69f)
            lineTo(w * 0.37f, h * 0.93f)
            cubicTo(w * 0.35f, h * 0.97f, w * 0.31f, h * 0.98f, w * 0.26f, h * 0.98f)
            lineTo(w * 0.15f, h * 0.98f)
            cubicTo(w * 0.07f, h * 0.98f, w * 0.02f, h * 0.91f, w * 0.02f, h * 0.82f)
            cubicTo(w * 0.02f, h * 0.77f, w * 0.03f, h * 0.72f, w * 0.06f, h * 0.67f)
            close()
        }
        drawPath(
            path = blue,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4285F4), Color(0xFF2F74F3), Color(0xFF7D8CFF)),
                start = androidx.compose.ui.geometry.Offset(0f, h * 0.50f),
                end = androidx.compose.ui.geometry.Offset(w * 0.50f, h)
            )
        )

        val yellow = Path().apply {
            moveTo(w * 0.62f, h * 0.48f)
            lineTo(w * 0.92f, h * 0.48f)
            lineTo(w * 0.94f, h * 0.67f)
            cubicTo(w * 0.97f, h * 0.72f, w * 0.98f, h * 0.77f, w * 0.98f, h * 0.82f)
            cubicTo(w * 0.98f, h * 0.91f, w * 0.93f, h * 0.98f, w * 0.85f, h * 0.98f)
            lineTo(w * 0.74f, h * 0.98f)
            cubicTo(w * 0.69f, h * 0.98f, w * 0.65f, h * 0.97f, w * 0.63f, h * 0.93f)
            lineTo(w * 0.50f, h * 0.69f)
            close()
        }
        drawPath(
            path = yellow,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFD21E), Color(0xFFFFC400)),
                start = androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.48f),
                end = androidx.compose.ui.geometry.Offset(w, h)
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
                        contentDescription = "Phát",
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
    var query by remember(controller.onlineSearchQuery) {
        mutableStateOf(controller.onlineSearchQuery)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tìm nhạc trực tuyến",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Apple Music Preview hoặc YouTube",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !controller.onlineSearchBusy) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OnlineModeButton(
                        title = "Apple Music",
                        subtitle = "Nghe bản Preview",
                        icon = Icons.Rounded.MusicNote,
                        selected = controller.onlineSearchMode == OnlineSearchMode.APPLE_MUSIC,
                        onClick = { controller.changeOnlineSearchMode(OnlineSearchMode.APPLE_MUSIC) },
                        modifier = Modifier.weight(1f)
                    )
                    OnlineModeButton(
                        title = "YouTube",
                        subtitle = "Xem video trong app",
                        icon = Icons.Rounded.Tv,
                        selected = controller.onlineSearchMode == OnlineSearchMode.YOUTUBE,
                        onClick = { controller.changeOnlineSearchMode(OnlineSearchMode.YOUTUBE) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        controller.onlineSearchQuery = it
                    },
                    singleLine = true,
                    label = {
                        Text(
                            if (controller.onlineSearchMode == OnlineSearchMode.APPLE_MUSIC) {
                                "Tên bài hát hoặc ca sĩ trên Apple Music"
                            } else {
                                "Tên video hoặc bài hát trên YouTube"
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = {
                                query = ""
                                controller.onlineSearchQuery = ""
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Xóa từ khóa")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { controller.searchOnline(query) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { controller.searchOnline(query) },
                    enabled = query.isNotBlank() && !controller.onlineSearchBusy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.purple,
                        contentColor = Color.White
                    )
                ) {
                    if (controller.onlineSearchBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (controller.onlineSearchBusy) "Đang tìm..." else "Tìm kiếm",
                        fontWeight = FontWeight.Bold
                    )
                }

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
                                "Apple Music sử dụng preview hợp lệ từ iTunes Search."
                            } else {
                                "Kết quả YouTube sẽ được phát trong màn hình Đang phát."
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
private fun OnlineModeButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier
            .height(78.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) colors.purple.copy(alpha = 0.24f) else colors.card,
        border = BorderStroke(
            1.dp,
            if (selected) colors.purpleBright else colors.border
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(13.dp),
                color = if (selected) colors.purple else colors.cardRaised
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    subtitle,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 2
                )
            }
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
