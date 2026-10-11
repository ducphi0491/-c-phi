package duc_phi.music

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@Composable
internal fun LibraryScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onAuthorizePersonalDrive: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    val songs = controller.filteredLibrarySongs()
    var actionSong by remember { mutableStateOf<SongItem?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            PlayerBottomArea(
                controller = controller,
                selectedScreen = MusicScreen.LIBRARY
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LibraryHeader(onSettings)
            }

            item {
                LibraryTabs(
                    selected = controller.libraryTab,
                    onSelected = { controller.libraryTab = it }
                )
            }

            item {
                SourceOverview(
                    controller = controller,
                    onScanLocal = onScanLocal,
                    onAuthorizePersonalDrive = onAuthorizePersonalDrive
                )
            }

            item {
                LibrarySearch(
                    value = controller.libraryQuery,
                    onValueChange = { controller.libraryQuery = it }
                )
            }

            item {
                SourceFilterChips(controller)
            }

            when (controller.libraryTab) {
                LibraryTab.SONGS -> {
                    if (songs.isEmpty()) {
                        item {
                            EmptyLibraryState(
                                title = "Không có bài hát phù hợp",
                                description = "Hãy quét nhạc trên máy hoặc thêm một nguồn Google Drive.",
                                buttonText = "Quét nhạc trên máy",
                                onButton = onScanLocal
                            )
                        }
                    } else {
                        items(songs, key = { it.id }) { song ->
                            LaunchedEffect(song.id) { controller.ensureArtwork(song) }
                            SongRow(
                                song = song,
                                artworkUrl = controller.artworkFor(song),
                                isCurrent = controller.currentSong?.id == song.id,
                                isFavorite = controller.isFavorite(song),
                                onClick = { controller.playSong(song, songs) },
                                onFavorite = { controller.toggleFavorite(song) },
                                onMore = if (controller.canUseLocalAudioTools(song)) {
                                    { actionSong = song }
                                } else null
                            )
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    val groups = controller.groupedAlbums().toList()
                    if (groups.isEmpty()) item { EmptyGroupState("Chưa có album") }
                    else items(groups, key = { it.first }) { (name, albumSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Album,
                            title = name,
                            subtitle = "${albumSongs.size} bài hát",
                            artworkSong = albumSongs.firstOrNull(),
                            artworkUrl = albumSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { controller.playCollection(albumSongs, 0) }
                        )
                    }
                }

                LibraryTab.ARTISTS -> {
                    val groups = controller.groupedArtists().toList()
                    if (groups.isEmpty()) item { EmptyGroupState("Chưa có nghệ sĩ") }
                    else items(groups, key = { it.first }) { (name, artistSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Person,
                            title = name,
                            subtitle = "${artistSongs.size} bài hát",
                            artworkSong = artistSongs.firstOrNull(),
                            artworkUrl = artistSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { controller.playCollection(artistSongs, 0) }
                        )
                    }
                }

                LibraryTab.FOLDERS -> {
                    val groups = controller.groupedFolders().toList()
                    if (groups.isEmpty()) item { EmptyGroupState("Chưa có thư mục") }
                    else items(groups, key = { it.first }) { (name, folderSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Folder,
                            title = name,
                            subtitle = "${folderSongs.size} tệp phương tiện",
                            artworkSong = folderSongs.firstOrNull(),
                            artworkUrl = folderSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { controller.playCollection(folderSongs, 0) }
                        )
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    item {
                        PlaylistSummaryCard(
                            title = "Danh sách đang phát",
                            subtitle = "${controller.queue.size} bài hát",
                            icon = Icons.Rounded.PlaylistPlay,
                            onClick = {
                                if (controller.queue.isNotEmpty()) {
                                    controller.playQueueIndex(controller.currentIndex.coerceAtLeast(0))
                                }
                            }
                        )
                    }
                    item {
                        PlaylistSummaryCard(
                            title = "Bài hát yêu thích",
                            subtitle = "${controller.sourceCount(LibrarySource.FAVORITES)} bài hát",
                            icon = Icons.Rounded.Favorite,
                            onClick = {
                                controller.librarySource = LibrarySource.FAVORITES
                                controller.libraryTab = LibraryTab.SONGS
                            }
                        )
                    }
                    item {
                        PlaylistSummaryCard(
                            title = "Đã tải để nghe Offline",
                            subtitle = "${controller.sourceCount(LibrarySource.DOWNLOADED)} bài hát",
                            icon = Icons.Rounded.Download,
                            onClick = {
                                controller.librarySource = LibrarySource.DOWNLOADED
                                controller.libraryTab = LibraryTab.SONGS
                            }
                        )
                    }
                }
            }
        }
    }

    if (controller.showPersonalDriveDialog) {
        PersonalDriveDialog(
            controller = controller,
            onAuthorize = onAuthorizePersonalDrive,
            onDismiss = { controller.showPersonalDriveDialog = false }
        )
    }

    if (controller.showPublicDriveDialog) {
        PublicDriveDialog(
            controller = controller,
            onDismiss = { controller.showPublicDriveDialog = false }
        )
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
private fun LibraryHeader(onSettings: () -> Unit) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Thư viện",
                color = colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Tất cả nhạc của bạn, một nơi",
                color = colors.textSecondary,
                fontSize = 15.sp
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Cài đặt", tint = colors.textPrimary)
        }
    }
}

@Composable
private fun LibraryTabs(
    selected: LibraryTab,
    onSelected: (LibraryTab) -> Unit
) {
    val tabs = listOf(
        LibraryTab.SONGS to ("Bài hát" to Icons.Rounded.MusicNote),
        LibraryTab.ALBUMS to ("Album" to Icons.Rounded.Album),
        LibraryTab.ARTISTS to ("Nghệ sĩ" to Icons.Rounded.Person),
        LibraryTab.FOLDERS to ("Thư mục" to Icons.Rounded.Folder),
        LibraryTab.PLAYLISTS to ("Playlist" to Icons.Rounded.PlaylistPlay)
    )
    val colors = LocalMusicColors.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(tabs, key = { it.first.name }) { (tab, data) ->
            val isSelected = selected == tab
            Surface(
                modifier = Modifier.clickable { onSelected(tab) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) colors.purple else colors.cardRaised,
                border = BorderStroke(1.dp, if (isSelected) colors.purpleBright else colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        data.second,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        data.first,
                        color = if (isSelected) Color.White else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceOverview(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onAuthorizePersonalDrive: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        item {
            LibrarySourceCard(
                title = "Nhạc trên máy",
                count = controller.sourceCount(LibrarySource.LOCAL),
                icon = Icons.Rounded.Smartphone,
                gradient = listOf(Color(0xFF0A4E7A), Color(0xFF052B4A)),
                onClick = {
                    onScanLocal()
                    controller.librarySource = LibrarySource.LOCAL
                    controller.libraryTab = LibraryTab.SONGS
                }
            )
        }
        item {
            LibrarySourceCard(
                title = "Google Drive",
                count = controller.sourceCount(LibrarySource.PERSONAL_DRIVE),
                icon = Icons.Rounded.Cloud,
                gradient = listOf(Color(0xFF4B176E), Color(0xFF28113F)),
                onClick = {
                    controller.librarySource = LibrarySource.PERSONAL_DRIVE
                    controller.libraryTab = LibraryTab.FOLDERS
                    if (controller.hasPersonalDriveAuthorization) {
                        controller.showPersonalDriveDialog = true
                        if (controller.personalDrivePath.isEmpty() && !controller.personalDriveBusy) {
                            controller.openPersonalDriveRoot()
                        }
                    } else {
                        onAuthorizePersonalDrive()
                    }
                }
            )
        }
        item {
            LibrarySourceCard(
                title = "Link Drive",
                count = controller.sourceCount(LibrarySource.PUBLIC_DRIVE),
                icon = Icons.Rounded.Link,
                gradient = listOf(Color(0xFF72183E), Color(0xFF361127)),
                onClick = {
                    controller.showPublicDriveDialog = true
                    controller.librarySource = LibrarySource.PUBLIC_DRIVE
                }
            )
        }
        item {
            LibrarySourceCard(
                title = "Đã tải Offline",
                count = controller.sourceCount(LibrarySource.DOWNLOADED),
                icon = Icons.Rounded.Download,
                gradient = listOf(Color(0xFF087A57), Color(0xFF06402F)),
                onClick = {
                    controller.librarySource = LibrarySource.DOWNLOADED
                    controller.libraryTab = LibraryTab.SONGS
                }
            )
        }
    }
}

@Composable
private fun LibrarySourceCard(
    title: String,
    count: Int,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .width(155.dp)
            .height(116.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("$count bài hát", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LibrarySearch(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(19.dp),
        color = colors.cardRaised,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                singleLine = true,
                cursorBrush = SolidColor(colors.purpleBright),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
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
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun SourceFilterChips(controller: MusicController) {
    val sources = listOf(
        LibrarySource.ALL to "Tất cả (${controller.sourceCount(LibrarySource.ALL)})",
        LibrarySource.LOCAL to "Nhạc trên máy",
        LibrarySource.PERSONAL_DRIVE to "Drive của tôi",
        LibrarySource.PUBLIC_DRIVE to "Drive chia sẻ",
        LibrarySource.FAVORITES to "Yêu thích",
        LibrarySource.DOWNLOADED to "Đã tải"
    )
    HorizontalChips(
        items = sources.map { (source, label) ->
            label to (controller.librarySource == source)
        },
        onClick = { index -> controller.librarySource = sources[index].first }
    )
}

@Composable
private fun GroupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    artworkSong: SongItem?,
    artworkUrl: String?,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (artworkSong != null) {
                ArtworkImage(
                    song = artworkSong,
                    artworkUrl = artworkUrl,
                    modifier = Modifier.size(62.dp),
                    cornerRadius = 13
                )
            } else {
                Surface(
                    modifier = Modifier.size(62.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = colors.purple.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = colors.purpleBright)
                    }
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textMuted)
        }
    }
}

@Composable
private fun PlaylistSummaryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(15.dp),
                color = colors.purple.copy(alpha = 0.17f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold)
                Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textMuted)
        }
    }
}

@Composable
private fun EmptyGroupState(text: String) {
    Text(
        text,
        color = LocalMusicColors.current.textSecondary,
        modifier = Modifier.padding(28.dp)
    )
}

@Composable
private fun PersonalDriveDialog(
    controller: MusicController,
    onAuthorize: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current

    LaunchedEffect(controller.hasPersonalDriveAuthorization) {
        if (
            controller.hasPersonalDriveAuthorization &&
            controller.personalDrivePath.isEmpty() &&
            !controller.personalDriveBusy
        ) {
            controller.openPersonalDriveRoot()
        }
    }

    Dialog(
        onDismissRequest = { if (!controller.personalDriveBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.91f),
            shape = RoundedCornerShape(20.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = colors.purpleBright
                    )
                    Spacer(Modifier.size(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Drive của tôi",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = controller.personalDriveAccountEmail
                                ?: "Cần đăng nhập và cấp quyền OAuth",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !controller.personalDriveBusy) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Đóng",
                            tint = colors.textSecondary
                        )
                    }
                }

                if (!controller.hasPersonalDriveAuthorization) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Cloud,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "MeloWare cần quyền chỉ đọc để hiển thị thư mục và phát file âm thanh trong Google Drive của tài khoản bạn.",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = onAuthorize,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                            ) {
                                Text("Đăng nhập và cấp quyền Google Drive")
                            }
                        }
                    }
                    controller.personalDriveError?.let { error ->
                        Text(error, color = colors.red, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    return@Column
                }

                if (controller.personalDriveBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text("Đang đọc Google Drive...", color = colors.textSecondary)
                    }
                }

                controller.personalDriveError?.let { error ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.red.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.red.copy(alpha = 0.35f))
                    ) {
                        Column(Modifier.padding(10.dp)) {
                            Text(error, color = colors.red, fontSize = 12.sp)
                            if (controller.personalDriveNeedsAuthorization) {
                                TextButton(onClick = onAuthorize) {
                                    Text("Cấp lại quyền", color = colors.purpleBright)
                                }
                            }
                        }
                    }
                }

                val currentFolder = controller.personalDrivePath.lastOrNull()
                if (currentFolder != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = controller::goBackPersonalDrive,
                            enabled = controller.personalDrivePath.size > 1 && !controller.personalDriveBusy
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Trở lại",
                                tint = colors.textPrimary
                            )
                        }
                        Text(
                            text = controller.personalDrivePath.joinToString(" / ") { it.name },
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val playable = controller.personalDriveEntries.filter(controller::isPersonalDrivePlayable)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${controller.personalDriveEntries.count { it.isFolder }} thư mục · ${playable.size} bài",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = controller::toggleAllPersonalDrivePlayable,
                            enabled = !controller.personalDriveBusy && playable.isNotEmpty()
                        ) {
                            val allSelected = playable.isNotEmpty() &&
                                    playable.all { controller.personalDriveSelectedIds.containsKey(it.id) }
                            Text(if (allSelected) "Bỏ chọn" else "Chọn tất cả")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(controller.personalDriveEntries, key = { it.id }) { entry ->
                            val isPlayable = controller.isPersonalDrivePlayable(entry)
                            val isSelected = controller.personalDriveSelectedIds.containsKey(entry.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        enabled = !controller.personalDriveBusy &&
                                                (entry.isFolder || isPlayable)
                                    ) {
                                        if (entry.isFolder) {
                                            controller.openPersonalDriveFolder(entry)
                                        } else {
                                            controller.togglePersonalDriveSelection(entry)
                                        }
                                    }
                                    .padding(vertical = 9.dp, horizontal = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (entry.isFolder) {
                                        Icons.Rounded.Folder
                                    } else {
                                        Icons.Rounded.MusicNote
                                    },
                                    contentDescription = null,
                                    tint = if (entry.isFolder) {
                                        colors.purpleBright
                                    } else {
                                        colors.textSecondary
                                    }
                                )
                                Spacer(Modifier.size(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        entry.name,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colors.textPrimary,
                                        fontSize = 13.sp
                                    )
                                    if (!entry.isFolder && !isPlayable) {
                                        Text(
                                            "Không hỗ trợ hoặc file không cho phép tải",
                                            color = colors.textMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (entry.isFolder) {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                } else if (isPlayable) {
                                    Text(
                                        if (isSelected) "☑" else "□",
                                        color = colors.purpleBright,
                                        fontSize = 22.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { controller.addSelectedPersonalDrive(playNow = false) },
                            enabled = controller.personalDriveSelectedIds.isNotEmpty() &&
                                    !controller.personalDriveBusy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.cardRaised)
                        ) {
                            Text("Thêm ${controller.personalDriveSelectedIds.size}")
                        }
                        Button(
                            onClick = { controller.addSelectedPersonalDrive(playNow = true) },
                            enabled = controller.personalDriveSelectedIds.isNotEmpty() &&
                                    !controller.personalDriveBusy,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.blue)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Phát")
                        }
                    }

                    Button(
                        onClick = { controller.addCurrentPersonalDriveFolder(playNow = true) },
                        enabled = !controller.personalDriveBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Text("Phát toàn bộ thư mục này (gồm thư mục con)")
                    }
                } else if (!controller.personalDriveBusy) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Chưa tải được danh sách thư mục", color = colors.textSecondary)
                            TextButton(onClick = controller::openPersonalDriveRoot) {
                                Text("Tải lại", color = colors.purpleBright)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicDriveDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    Dialog(
        onDismissRequest = { if (!controller.publicDriveBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Google Drive - Link công khai",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Dán link thư mục được chia sẻ Anyone with the link",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !controller.publicDriveBusy) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardRaised,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BasicTextField(
                        value = controller.publicLinkText,
                        onValueChange = controller::setPublicLink,
                        enabled = !controller.publicDriveBusy,
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(colors.purpleBright),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { controller.openPublicDriveLink() }
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        decorationBox = { inner ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, contentDescription = null, tint = colors.textMuted)
                                Spacer(Modifier.width(9.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (controller.publicLinkText.isBlank()) {
                                        Text(
                                            "https://drive.google.com/drive/folders/...",
                                            color = colors.textMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    inner()
                                }
                                if (controller.publicLinkText.isNotBlank()) {
                                    IconButton(onClick = controller::clearPublicLink, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Xóa link", tint = colors.textSecondary)
                                    }
                                }
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = clipboard?.primaryClip
                            val text = if (clip != null && clip.itemCount > 0) {
                                clip.getItemAt(0).coerceToText(context).toString()
                            } else ""
                            if (text.isNotBlank()) controller.setPublicLink(text)
                        },
                        enabled = !controller.publicDriveBusy
                    ) {
                        Text("Dán từ bộ nhớ tạm", color = colors.purpleBright)
                    }
                    Button(
                        onClick = controller::openPublicDriveLink,
                        enabled = !controller.publicDriveBusy && controller.publicLinkText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Mở link", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (controller.publicDriveBusy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = colors.purpleBright
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Đang đọc Google Drive...", color = colors.textSecondary)
                    }
                }

                controller.publicDriveError?.let { error ->
                    Text(error, color = colors.red, fontSize = 12.sp)
                }

                if (controller.publicPath.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = controller::goBackPublicFolder,
                            enabled = controller.publicPath.size > 1 && !controller.publicDriveBusy
                        ) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = colors.textPrimary)
                        }
                        Text(
                            controller.publicPath.joinToString(" / ") { it.name },
                            color = colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val playableEntries = controller.publicEntries.filter(controller::isPublicEntryPlayable)
                if (playableEntries.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${controller.publicEntries.count { it.isFolder }} thư mục · ${playableEntries.size} bài",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = controller::toggleAllPublicEntries,
                            enabled = !controller.publicDriveBusy
                        ) {
                            val allSelected = playableEntries.all {
                                controller.publicSelectedIds[it.id] == true
                            }
                            Text(
                                if (allSelected) "Bỏ chọn" else "Chọn tất cả",
                                color = colors.purpleBright
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(controller.publicEntries, key = { it.id }) { entry ->
                        val playable = controller.isPublicEntryPlayable(entry)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .clickable(enabled = entry.isFolder || playable) {
                                    if (entry.isFolder) controller.openPublicFolder(entry)
                                    else controller.togglePublicEntry(entry)
                                }
                                .background(
                                    if (controller.publicSelectedIds[entry.id] == true) {
                                        colors.purple.copy(alpha = 0.15f)
                                    } else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (entry.isFolder) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.purpleBright)
                            } else {
                                Checkbox(
                                    checked = controller.publicSelectedIds[entry.id] == true,
                                    onCheckedChange = { controller.togglePublicEntry(entry) },
                                    enabled = playable,
                                    colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                )
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.name,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    when {
                                        entry.isFolder -> "Thư mục"
                                        playable -> entry.mimeType.ifBlank { "Tệp âm thanh" }
                                        else -> "Không hỗ trợ hoặc chủ sở hữu đã chặn tải"
                                    },
                                    color = if (!entry.isFolder && !playable) colors.textMuted else colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            if (entry.isFolder) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textMuted)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { controller.addSelectedPublicSongs(playImmediately = false) },
                        enabled = controller.publicSelectedIds.isNotEmpty() && !controller.publicDriveBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cardRaised)
                    ) {
                        Text("Thêm đã chọn", color = colors.textPrimary)
                    }
                    Button(
                        onClick = { controller.addSelectedPublicSongs(playImmediately = true) },
                        enabled = controller.publicSelectedIds.isNotEmpty() && !controller.publicDriveBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(5.dp))
                        Text("Phát", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = { controller.addCurrentPublicFolder(playImmediately = false) },
                    enabled = controller.publicPath.isNotEmpty() && !controller.publicDriveBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Thêm toàn bộ thư mục hiện tại", color = colors.purpleBright)
                }
            }
        }
    }
}

private enum class SongToolPage { MENU, CUT, MERGE, RINGTONE }

@Composable
internal fun SongToolsHost(
    controller: MusicController,
    song: SongItem,
    onDismiss: () -> Unit
) {
    var page by remember(song.id) { mutableStateOf(SongToolPage.MENU) }
    when (page) {
        SongToolPage.MENU -> SongToolsMenuDialog(
            song = song,
            busy = controller.audioToolBusy,
            onCut = { page = SongToolPage.CUT },
            onMerge = { page = SongToolPage.MERGE },
            onRingtone = { page = SongToolPage.RINGTONE },
            onDismiss = onDismiss
        )
        SongToolPage.CUT -> CutSongDialog(
            controller = controller,
            song = song,
            onBack = { page = SongToolPage.MENU },
            onDismiss = onDismiss
        )
        SongToolPage.MERGE -> MergeSongDialog(
            controller = controller,
            baseSong = song,
            onBack = { page = SongToolPage.MENU },
            onDismiss = onDismiss
        )
        SongToolPage.RINGTONE -> RingtoneSongDialog(
            controller = controller,
            song = song,
            onBack = { page = SongToolPage.MENU },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun SongToolsMenuDialog(
    song: SongItem,
    busy: Boolean,
    onCut: () -> Unit,
    onMerge: () -> Unit,
    onRingtone: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.42f))
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Công cụ âm thanh", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(song.title, color = colors.purpleBright, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                AudioToolRow(Icons.Rounded.ContentCut, "Cắt nhạc", "Chọn đoạn và lưu thành tệp M4A mới", onCut)
                AudioToolRow(Icons.Rounded.PlaylistAdd, "Ghép nhạc", "Nối bài này với một bài hát khác", onMerge)
                AudioToolRow(Icons.Rounded.NotificationsActive, "Đặt làm nhạc chuông", "Cuộc gọi hoặc âm báo thông báo", onRingtone)
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.purpleBright)
                        Text("Đang xử lý tệp âm thanh...", color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = colors.cardRaised,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(42.dp), shape = CircleShape, color = colors.purple.copy(alpha = 0.16f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = colors.textSecondary, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textMuted)
        }
    }
}

@Composable
private fun CutSongDialog(
    controller: MusicController,
    song: SongItem,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val maxDuration = song.durationMs.coerceAtLeast(1_000L).toFloat()
    var selectedRange by remember(song.id, maxDuration) { mutableStateOf(0f..maxDuration) }
    Dialog(onDismissRequest = { if (!controller.audioToolBusy) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DialogToolHeader("Cắt nhạc", song.title, onBack, onDismiss)
                RangeSlider(
                    value = selectedRange,
                    onValueChange = { proposed ->
                        if (proposed.endInclusive - proposed.start >= 1_000f) selectedRange = proposed
                    },
                    valueRange = 0f..maxDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.purpleBright,
                        activeTrackColor = colors.purple,
                        inactiveTrackColor = colors.borderSoft
                    )
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(selectedRange.start.toLong()), color = colors.textSecondary)
                    Text(formatDuration(selectedRange.endInclusive.toLong()), color = colors.textSecondary)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = colors.backgroundSoft) {
                    Text(
                        text = "Độ dài đoạn cắt: ${formatDuration((selectedRange.endInclusive - selectedRange.start).toLong())}",
                        color = colors.textPrimary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Button(
                    onClick = {
                        controller.cutLocalSong(song, selectedRange.start.toLong(), selectedRange.endInclusive.toLong())
                        onDismiss()
                    },
                    enabled = !controller.audioToolBusy && selectedRange.endInclusive > selectedRange.start,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                ) {
                    Icon(Icons.Rounded.ContentCut, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Cắt và lưu")
                }
            }
        }
    }
}

@Composable
private fun MergeSongDialog(
    controller: MusicController,
    baseSong: SongItem,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val candidates = controller.localAudioToolSongs(baseSong.id)

    var step by remember(baseSong.id) { mutableStateOf(0) }
    var selected by remember(baseSong.id) { mutableStateOf<SongItem?>(null) }

    val firstMaxDuration = baseSong.durationMs.coerceAtLeast(1_000L).toFloat()
    val secondMaxDuration = selected?.durationMs?.coerceAtLeast(1_000L)?.toFloat() ?: 1_000f
    var firstRange by remember(baseSong.id, firstMaxDuration) {
        mutableStateOf(0f..firstMaxDuration)
    }
    var secondRange by remember(selected?.id, secondMaxDuration) {
        mutableStateOf(0f..secondMaxDuration)
    }

    val previewPlayer = remember { ExoPlayer.Builder(context).build() }
    var previewPrepared by remember { mutableStateOf(false) }
    var previewPlaying by remember { mutableStateOf(false) }
    var previewPositionMs by remember { mutableStateOf(0L) }
    var previewDragging by remember { mutableStateOf(false) }
    var draggedPreviewMs by remember { mutableStateOf(0f) }

    val firstSegmentDuration = (firstRange.endInclusive - firstRange.start)
        .toLong()
        .coerceAtLeast(1L)
    val secondSegmentDuration = (secondRange.endInclusive - secondRange.start)
        .toLong()
        .coerceAtLeast(1L)
    val previewTotalMs = (firstSegmentDuration + secondSegmentDuration).coerceAtLeast(1L)
    val latestPreviewTotalMs by rememberUpdatedState(previewTotalMs)

    fun clippedMediaItem(
        song: SongItem,
        range: ClosedFloatingPointRange<Float>
    ): MediaItem = MediaItem.Builder()
        .setUri(song.uri)
        .setClippingConfiguration(
            MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(range.start.toLong())
                .setEndPositionMs(range.endInclusive.toLong())
                .build()
        )
        .build()

    fun seekPreview(targetMs: Long) {
        val clamped = targetMs.coerceIn(0L, previewTotalMs)
        if (clamped < firstSegmentDuration) {
            previewPlayer.seekTo(0, clamped)
        } else {
            previewPlayer.seekTo(1, (clamped - firstSegmentDuration).coerceAtLeast(0L))
        }
        previewPositionMs = clamped
    }

    fun preparePreview(targetMs: Long = 0L, autoPlay: Boolean = false) {
        val secondSong = selected ?: return
        previewPlayer.pause()
        previewPlayer.setMediaItems(
            listOf(
                clippedMediaItem(baseSong, firstRange),
                clippedMediaItem(secondSong, secondRange)
            )
        )
        previewPlayer.prepare()
        previewPrepared = true
        seekPreview(targetMs)
        if (autoPlay) previewPlayer.play()
    }

    DisposableEffect(previewPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                previewPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    previewPlaying = false
                    previewPositionMs = latestPreviewTotalMs
                }
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }

    LaunchedEffect(selected?.id, firstRange, secondRange) {
        previewPlayer.pause()
        previewPlayer.clearMediaItems()
        previewPrepared = false
        previewPlaying = false
        previewPositionMs = 0L
        draggedPreviewMs = 0f
    }

    LaunchedEffect(previewPrepared, previewPlayer, firstSegmentDuration, previewTotalMs) {
        while (previewPrepared) {
            if (!previewDragging && previewPlayer.mediaItemCount > 0) {
                val combinedPosition = when (previewPlayer.currentMediaItemIndex) {
                    0 -> previewPlayer.currentPosition
                    1 -> firstSegmentDuration + previewPlayer.currentPosition
                    else -> previewPositionMs
                }
                previewPositionMs = combinedPosition.coerceIn(0L, previewTotalMs)
            }
            delay(100L)
        }
    }

    Dialog(onDismissRequest = { if (!controller.audioToolBusy) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                DialogToolHeader(
                    title = if (step == 0) "Ghép nhạc" else "Cắt đoạn rồi ghép",
                    songTitle = if (step == 0) baseSong.title else "${baseSong.title} + ${selected?.title.orEmpty()}",
                    onBack = {
                        if (step == 0) {
                            onBack()
                        } else {
                            previewPlayer.pause()
                            step = 0
                        }
                    },
                    onDismiss = onDismiss
                )
                Spacer(Modifier.height(10.dp))

                if (step == 0) {
                    Text("Bài thứ nhất", color = colors.textSecondary, fontSize = 12.sp)
                    MergeSelectedSongCard(
                        index = 1,
                        song = baseSong,
                        controller = controller,
                        selected = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Chọn bài hát thứ hai", color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))

                    if (candidates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Không có bài hát cục bộ khác để ghép", color = colors.textSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(candidates, key = { it.id }) { candidate ->
                                val active = selected?.id == candidate.id
                                MergeSelectedSongCard(
                                    index = 2,
                                    song = candidate,
                                    controller = controller,
                                    selected = active,
                                    onClick = { selected = candidate }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { step = 1 },
                        enabled = selected != null && !controller.audioToolBusy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.ContentCut, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Tiếp tục cắt hai bài")
                    }
                } else {
                    val secondSong = selected
                    if (secondSong == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Chưa chọn bài hát thứ hai", color = colors.textSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                MergeTrimCard(
                                    index = 1,
                                    song = baseSong,
                                    range = firstRange,
                                    maxDuration = firstMaxDuration,
                                    onRangeChange = { firstRange = it }
                                )
                            }
                            item {
                                MergeTrimCard(
                                    index = 2,
                                    song = secondSong,
                                    range = secondRange,
                                    maxDuration = secondMaxDuration,
                                    onRangeChange = { secondRange = it }
                                )
                            }
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = colors.backgroundSoft,
                                    border = BorderStroke(1.dp, colors.borderSoft)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "Nghe thử kết quả sau khi ghép",
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Slider(
                                            value = if (previewDragging) draggedPreviewMs else previewPositionMs.toFloat(),
                                            onValueChange = { value ->
                                                previewDragging = true
                                                draggedPreviewMs = value
                                            },
                                            onValueChangeFinished = {
                                                previewDragging = false
                                                val target = draggedPreviewMs.toLong()
                                                if (!previewPrepared) preparePreview(target, autoPlay = false)
                                                else seekPreview(target)
                                            },
                                            valueRange = 0f..previewTotalMs.toFloat(),
                                            colors = SliderDefaults.colors(
                                                thumbColor = colors.purpleBright,
                                                activeTrackColor = colors.purple,
                                                inactiveTrackColor = colors.borderSoft
                                            )
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                formatDuration(
                                                    if (previewDragging) draggedPreviewMs.toLong()
                                                    else previewPositionMs
                                                ),
                                                color = colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                formatDuration(previewTotalMs),
                                                color = colors.textSecondary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (previewPlaying) {
                                                    previewPlayer.pause()
                                                } else if (!previewPrepared) {
                                                    preparePreview(previewPositionMs, autoPlay = true)
                                                } else {
                                                    if (previewPositionMs >= previewTotalMs ||
                                                        previewPlayer.playbackState == Player.STATE_ENDED
                                                    ) {
                                                        seekPreview(0L)
                                                    }
                                                    previewPlayer.play()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(13.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.cardRaised)
                                        ) {
                                            Icon(
                                                if (previewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = colors.purpleBright
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(
                                                if (previewPlaying) "Tạm dừng nghe thử" else "Nghe thử hai đoạn",
                                                color = colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                previewPlayer.pause()
                                controller.mergeLocalSongs(
                                    first = baseSong,
                                    second = secondSong,
                                    firstStartMs = firstRange.start.toLong(),
                                    firstEndMs = firstRange.endInclusive.toLong(),
                                    secondStartMs = secondRange.start.toLong(),
                                    secondEndMs = secondRange.endInclusive.toLong()
                                )
                                onDismiss()
                            },
                            enabled = !controller.audioToolBusy &&
                                    firstRange.endInclusive > firstRange.start &&
                                    secondRange.endInclusive > secondRange.start,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Cắt hai đoạn, ghép và lưu")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeSelectedSongCard(
    index: Int,
    song: SongItem,
    controller: MusicController,
    selected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) colors.purple.copy(alpha = 0.20f) else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = colors.purple.copy(alpha = 0.18f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(index.toString(), color = colors.purpleBright, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(9.dp))
        ArtworkImage(song, controller.artworkFor(song), Modifier.size(48.dp), 10)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (selected) colors.purpleBright else colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${song.artist} · ${song.durationLabel}",
                color = colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.purpleBright)
        }
    }
}

@Composable
private fun MergeTrimCard(
    index: Int,
    song: SongItem,
    range: ClosedFloatingPointRange<Float>,
    maxDuration: Float,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    val colors = LocalMusicColors.current
    val minimumSegment = minOf(1_000f, maxDuration)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = colors.backgroundSoft,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(28.dp),
                    shape = CircleShape,
                    color = colors.purple.copy(alpha = 0.18f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(index.toString(), color = colors.purpleBright, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        song.title,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Kéo hai đầu để chọn đoạn cần ghép",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            RangeSlider(
                value = range,
                onValueChange = { proposed ->
                    if (proposed.endInclusive - proposed.start >= minimumSegment) {
                        onRangeChange(proposed)
                    }
                },
                valueRange = 0f..maxDuration,
                colors = SliderDefaults.colors(
                    thumbColor = colors.purpleBright,
                    activeTrackColor = colors.purple,
                    inactiveTrackColor = colors.borderSoft
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Bắt đầu ${formatDuration(range.start.toLong())}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    "Kết thúc ${formatDuration(range.endInclusive.toLong())}",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                "Đoạn được chọn: ${formatDuration((range.endInclusive - range.start).toLong())}",
                color = colors.purpleBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RingtoneSongDialog(
    controller: MusicController,
    song: SongItem,
    onBack: () -> Unit,
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
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogToolHeader("Đặt làm nhạc chuông", song.title, onBack, onDismiss)
                Button(
                    onClick = {
                        controller.setAsRingtone(song, RingtoneManager.TYPE_RINGTONE)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                ) {
                    Icon(Icons.Rounded.PhoneInTalk, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Nhạc chuông cuộc gọi")
                }
                Button(
                    onClick = {
                        controller.setAsRingtone(song, RingtoneManager.TYPE_NOTIFICATION)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.blue)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(7.dp))
                    Text("Âm báo thông báo")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { controller.restoreDefaultRingtone(RingtoneManager.TYPE_RINGTONE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Khôi phục cuộc gọi", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { controller.restoreDefaultRingtone(RingtoneManager.TYPE_NOTIFICATION) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Khôi phục thông báo", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogToolHeader(
    title: String,
    songTitle: String,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = colors.textSecondary)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 5.dp)) {
            Text(title, color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(songTitle, color = colors.purpleBright, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
        }
    }
}
