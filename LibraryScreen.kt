package duc_phi.music

import android.content.Context
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

@Composable
internal fun LibraryScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onPickPersonalDrive: () -> Unit,
    onSettings: () -> Unit
) {
    val colors = LocalMusicColors.current
    val songs = controller.filteredLibrarySongs()

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
                    onPickPersonalDrive = onPickPersonalDrive
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
                                onFavorite = { controller.toggleFavorite(song) }
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

    if (controller.showPublicDriveDialog) {
        PublicDriveDialog(
            controller = controller,
            onDismiss = { controller.showPublicDriveDialog = false }
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
    onPickPersonalDrive: () -> Unit
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
                    if (controller.personalDriveUriText.isBlank()) onPickPersonalDrive()
                    controller.librarySource = LibrarySource.PERSONAL_DRIVE
                    controller.libraryTab = LibraryTab.SONGS
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
        items = sources.map { (_, label) -> label to (controller.librarySource == it.first) },
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(controller.publicEntries, key = { it.id }) { entry ->
                        val playable = !entry.isFolder && entry.canDownload
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .clickable {
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
                                    if (entry.isFolder) "Thư mục" else entry.mimeType.ifBlank { "Tệp âm thanh" },
                                    color = colors.textSecondary,
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
