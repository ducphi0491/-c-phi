package duc_phi.music

import android.content.Context
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
// region Merged source: LibraryScreen.kt
@Composable
internal fun LibraryScreen(
    controller: MusicController,
    onScanLocal: () -> Unit,
    onAuthorizePersonalDrive: () -> Unit,
    onSettings: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val colors = LocalMusicColors.current
    val songs = controller.filteredLibrarySongs()
    var actionSong by remember { mutableStateOf<SongItem?>(null) }
    var showInternetDialog by remember { mutableStateOf(false) }
    var selectedSmartTarget by remember { mutableStateOf<SmartPlaylistTarget?>(null) }
    // A folder tap opens a browsable track list instead of immediately starting playback.
    var selectedFolder by remember { mutableStateOf<Pair<String, List<SongItem>>?>(null) }
    var editingSmartPlaylist by remember { mutableStateOf<SmartPlaylistDraft?>(null) }
    var deletingSmartPlaylist by remember { mutableStateOf<SmartPlaylistDefinition?>(null) }
    var showDuplicateManager by remember { mutableStateOf(false) }
    var duplicateScanResult by remember { mutableStateOf<List<SmartDuplicateGroup>>(emptyList()) }
    var duplicateScanDone by remember { mutableStateOf(false) }
    var duplicateScanBusy by remember { mutableStateOf(false) }
    val smartScope = rememberCoroutineScope()
    val smartPremiumLocked = MeloWarePremiumAccess.smartPlaylistRequiresPremium
    val drivePremiumLocked = MeloWarePremiumAccess.googleDriveRequiresPremium

    LaunchedEffect(drivePremiumLocked, controller.librarySource) {
        if (drivePremiumLocked && controller.librarySource in setOf(
                LibrarySource.PERSONAL_DRIVE,
                LibrarySource.PUBLIC_DRIVE
            )
        ) {
            controller.librarySource = LibrarySource.ALL
            controller.showPublicDriveDialog = false
        }
    }

    LaunchedEffect(controller.localSongs.size) {
        duplicateScanResult = emptyList()
        duplicateScanDone = false
        duplicateScanBusy = false
        showDuplicateManager = false
    }

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
                // Safe area for screen cutouts, punch-hole cameras and visible system bars.
                // Use safeDrawing once (instead of stacking statusBarsPadding) to avoid double top inset.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LibraryHeader(onSettings)
            }

            // The search bar stays immediately under the Library heading, before the
            // Songs / Albums / Artists tabs and before the source cards.
            item {
                LibrarySearch(
                    value = controller.libraryQuery,
                    onValueChange = { controller.libraryQuery = it }
                )
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
                    drivePremiumLocked = drivePremiumLocked,
                    onScanLocal = onScanLocal,
                    onAuthorizePersonalDrive = onAuthorizePersonalDrive,
                    onOpenPremium = onOpenPremium
                )
            }

            item {
                SourceFilterChips(
                    controller = controller,
                    drivePremiumLocked = drivePremiumLocked,
                    onOpenPremium = onOpenPremium
                )
            }

            // This is the only Picker entry point when Drive already has saved songs.
            // Tapping the Google Drive source card above never asks for OAuth again.
            if (controller.librarySource == LibrarySource.PERSONAL_DRIVE &&
                controller.personalDriveSongs.isNotEmpty() &&
                controller.libraryTab == LibraryTab.SONGS
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onAuthorizePersonalDrive,
                            enabled = !controller.personalDriveBusy
                        ) {
                            Icon(
                                Icons.Rounded.PlaylistAdd,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(mwString("library.add_from_drive", "Thêm nhạc từ Drive"), color = colors.purpleBright)
                        }
                    }
                }
            }

            if (controller.librarySource == LibrarySource.PERSONAL_DRIVE &&
                (controller.personalDriveBusy || controller.personalDriveError != null)
            ) {
                item {
                    PersonalDrivePickerStatus(
                        busy = controller.personalDriveBusy,
                        error = controller.personalDriveError,
                        onOpenPicker = onAuthorizePersonalDrive
                    )
                }
            }

            if (controller.librarySource == LibrarySource.INTERNET) {
                item {
                    Surface(color = colors.backgroundSoft, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wifi, contentDescription = null,
                                tint = if (controller.internetSession.dataChannelOpen) colors.green else colors.textMuted)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(mwString("library.internet_folder", "Thư mục MeloWare Internet"), color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(if (controller.internetSession.dataChannelOpen)
                                    mwString("library.internet_connected", "Đã kết nối · nghe trực tiếp từ máy A · không cần tải về")
                                else mwString("library.internet_disconnected", "Chưa kết nối với máy A"),
                                    color = colors.textSecondary, fontSize = 11.sp)
                            }
                            TextButton(onClick = { showInternetDialog = true }) {
                                Text(mwString("library.manage", "Quản lý"), color = colors.purpleBright, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            when (controller.libraryTab) {
                LibraryTab.SONGS -> {
                    if (songs.isEmpty()) {
                        item {
                            val isDrive = controller.librarySource == LibrarySource.PERSONAL_DRIVE
                            EmptyLibraryState(
                                title = if (isDrive) mwString("library.no_drive_music", "Chưa có nhạc từ Google Drive") else mwString("library.no_matching_songs", "Không có bài hát phù hợp"),
                                description = if (isDrive) {
                                    if (BuildConfig.DRIVE_READONLY_REVIEW) {
                                        "Cấp quyền đọc Drive một lần, duyệt cây thư mục và chọn nhạc ngay trong MeloWave."
                                    } else {
                                        mwString("library.picker_instruction", "Mở Google Picker và chọn các file hoặc thư mục âm thanh bạn muốn chia sẻ với MeloWare.")
                                    }
                                } else {
                                    mwString("library.scan_instruction", "Hãy quét nhạc trên máy hoặc thêm một nguồn Google Drive.")
                                },
                                buttonText = if (isDrive) {
                                    if (BuildConfig.DRIVE_READONLY_REVIEW) "Duyệt Google Drive" else mwString("library.open_picker", "Mở Google Picker")
                                } else mwString("home.scan_local", "Quét nhạc trên máy"),
                                onButton = if (isDrive) onAuthorizePersonalDrive else onScanLocal
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
                    if (groups.isEmpty()) item { EmptyGroupState(mwString("library.no_albums", "Chưa có album")) }
                    else items(groups, key = { it.first }) { (name, albumSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Album,
                            title = name,
                            subtitle = mwString("library.count_songs", "{count} bài hát", "count" to albumSongs.size),
                            artworkSong = albumSongs.firstOrNull(),
                            artworkUrl = albumSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { controller.playCollection(albumSongs, 0) }
                        )
                    }
                }

                LibraryTab.ARTISTS -> {
                    val groups = controller.groupedArtists().toList()
                    if (groups.isEmpty()) item { EmptyGroupState(mwString("library.no_artists", "Chưa có nghệ sĩ")) }
                    else items(groups, key = { it.first }) { (name, artistSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Person,
                            title = mwArtistLabel(name),
                            subtitle = mwString("library.count_songs", "{count} bài hát", "count" to artistSongs.size),
                            artworkSong = artistSongs.firstOrNull(),
                            artworkUrl = artistSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { controller.playCollection(artistSongs, 0) }
                        )
                    }
                }

                LibraryTab.FOLDERS -> {
                    val groups = controller.groupedFolders().toList()
                    if (groups.isEmpty()) item { EmptyGroupState(mwString("library.no_folders", "Chưa có thư mục")) }
                    else items(groups, key = { it.first }) { (name, folderSongs) ->
                        GroupCard(
                            icon = Icons.Rounded.Folder,
                            title = name,
                            subtitle = mwString("library.count_media", "{count} tệp phương tiện", "count" to folderSongs.size),
                            artworkSong = folderSongs.firstOrNull(),
                            artworkUrl = folderSongs.firstOrNull()?.let(controller::artworkFor),
                            onClick = { selectedFolder = name to folderSongs }
                        )
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    item {
                        SmartPlaylistProSection(
                            controller = controller,
                            premiumLocked = smartPremiumLocked,
                            onOpenPremium = onOpenPremium,
                            onOpenBuiltIn = { type -> selectedSmartTarget = SmartPlaylistTarget.BuiltIn(type) },
                            onOpenCustom = { definition -> selectedSmartTarget = SmartPlaylistTarget.Custom(definition) },
                            onCreateCustom = { editingSmartPlaylist = SmartPlaylistDraft() },
                            onEditCustom = { definition -> editingSmartPlaylist = definition.toDraft() },
                            onDeleteCustom = { definition -> deletingSmartPlaylist = definition },
                            duplicateGroups = duplicateScanResult,
                            duplicateScanDone = duplicateScanDone,
                            duplicateScanBusy = duplicateScanBusy,
                            onOpenDuplicates = {
                                if (duplicateScanDone) {
                                    showDuplicateManager = true
                                } else if (!duplicateScanBusy) {
                                    duplicateScanBusy = true
                                    val expectedSongCount = controller.localSongs.size
                                    smartScope.launch {
                                        val scanResult = runCatching {
                                            withContext(Dispatchers.Default) {
                                                controller.smartDuplicateGroups()
                                            }
                                        }.getOrDefault(emptyList())
                                        duplicateScanBusy = false
                                        if (controller.localSongs.size == expectedSongCount) {
                                            duplicateScanResult = scanResult
                                            duplicateScanDone = true
                                            showDuplicateManager = true
                                        }
                                    }
                                }
                            }
                        )
                    }
                    item {
                        PlaylistSummaryCard(
                            title = mwString("library.queue", "Danh sách đang phát"),
                            subtitle = mwString("library.count_songs", "{count} bài hát", "count" to controller.queue.size),
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
                            title = mwString("library.favorite_songs", "Bài hát yêu thích"),
                            subtitle = mwString("library.count_songs", "{count} bài hát", "count" to controller.sourceCount(LibrarySource.FAVORITES)),
                            icon = Icons.Rounded.Favorite,
                            onClick = {
                                controller.librarySource = LibrarySource.FAVORITES
                                controller.libraryTab = LibraryTab.SONGS
                            }
                        )
                    }
                    item {
                        PlaylistSummaryCard(
                            title = mwString("library.downloaded_offline", "Đã tải để nghe Offline"),
                            subtitle = mwString("library.count_songs", "{count} bài hát", "count" to controller.sourceCount(LibrarySource.DOWNLOADED)),
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

    // Reviewer can browse root/subfolders and read/play music without Google Picker.
    // This UI is kept separate from the existing production drive.file flow.
    if (BuildConfig.DRIVE_READONLY_REVIEW && controller.showPersonalDriveDialog) {
        PersonalDriveDialog(
            controller = controller,
            onAuthorize = onAuthorizePersonalDrive,
            onDismiss = { controller.showPersonalDriveDialog = false }
        )
    }

    if (showInternetDialog) {
        MeloWareInternetDialog(controller = controller, onDismiss = { showInternetDialog = false })
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

    selectedFolder?.let { (folderName, folderSongs) ->
        SmartPlaylistSongsDialog(
            controller = controller,
            title = folderName,
            songs = folderSongs,
            onDismiss = { selectedFolder = null },
            onMore = { song ->
                selectedFolder = null
                actionSong = song
            },
            folderMode = true
        )
    }

    selectedSmartTarget?.let { target ->
        val title = when (target) {
            is SmartPlaylistTarget.BuiltIn -> smartBuiltInTitle(target.type)
            is SmartPlaylistTarget.Custom -> target.definition.playlist.name
        }
        val smartSongs = when (target) {
            is SmartPlaylistTarget.BuiltIn -> controller.builtInSmartPlaylistSongs(target.type)
            is SmartPlaylistTarget.Custom -> controller.customSmartPlaylistSongs(target.definition)
        }
        SmartPlaylistSongsDialog(
            controller = controller,
            title = title,
            songs = smartSongs,
            onDismiss = { selectedSmartTarget = null },
            onMore = { song ->
                selectedSmartTarget = null
                actionSong = song
            }
        )
    }

    editingSmartPlaylist?.let { draft ->
        SmartPlaylistEditorDialog(
            initial = draft,
            onDismiss = { editingSmartPlaylist = null },
            onSave = { savedDraft ->
                controller.saveSmartPlaylist(savedDraft)
                editingSmartPlaylist = null
            }
        )
    }

    deletingSmartPlaylist?.let { definition ->
        SmartPlaylistDeleteDialog(
            name = definition.playlist.name,
            onDismiss = { deletingSmartPlaylist = null },
            onConfirm = {
                controller.deleteSmartPlaylist(definition.playlist.id)
                deletingSmartPlaylist = null
            }
        )
    }

    if (showDuplicateManager) {
        SmartDuplicateManagerDialog(
            controller = controller,
            groups = duplicateScanResult,
            onDismiss = { showDuplicateManager = false },
            onMore = { song ->
                showDuplicateManager = false
                actionSong = song
            }
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
                text = mwString("nav.library", "Thư viện"),
                color = colors.textPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = mwString("library.subtitle", "Tất cả nhạc của bạn, một nơi"),
                color = colors.textSecondary,
                fontSize = 15.sp
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = mwString("settings.title", "Cài đặt"), tint = colors.textPrimary)
        }
    }
}

@Composable
private fun LibraryTabs(
    selected: LibraryTab,
    onSelected: (LibraryTab) -> Unit
) {
    val tabs = listOf(
        LibraryTab.SONGS to (mwString("library.tabs.songs", "Bài hát") to Icons.Rounded.MusicNote),
        LibraryTab.ALBUMS to (mwString("library.tabs.albums", "Album") to Icons.Rounded.Album),
        LibraryTab.ARTISTS to (mwString("library.tabs.artists", "Nghệ sĩ") to Icons.Rounded.Person),
        LibraryTab.FOLDERS to (mwString("library.tabs.folders", "Thư mục") to Icons.Rounded.Folder),
        LibraryTab.PLAYLISTS to (mwString("library.tabs.playlists", "Playlist") to Icons.Rounded.PlaylistPlay)
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
    drivePremiumLocked: Boolean,
    onScanLocal: () -> Unit,
    onAuthorizePersonalDrive: () -> Unit,
    onOpenPremium: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        item {
            LibrarySourceCard(
                title = mwString("library.local_music", "Nhạc trên máy"),
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
                locked = drivePremiumLocked,
                onClick = {
                    if (drivePremiumLocked) {
                        onOpenPremium()
                    } else {
                        controller.librarySource = LibrarySource.PERSONAL_DRIVE
                        controller.libraryTab = LibraryTab.SONGS
                        controller.libraryQuery = "" // Clear stale search so saved songs are visible.
                        if (BuildConfig.DRIVE_READONLY_REVIEW) {
                            // Explicit user action: access the full folder tree.
                            // OAuth is silent when the grant was already approved.
                            if (controller.hasPersonalDriveAuthorization) {
                                controller.showPersonalDriveDialog = true
                                controller.openPersonalDriveRoot()
                            } else {
                                onAuthorizePersonalDrive()
                            }
                        }
                    }
                }
            )
        }
        item {
            LibrarySourceCard(
                title = mwString("home.drive_link", "Link Drive"),
                count = controller.sourceCount(LibrarySource.PUBLIC_DRIVE),
                icon = Icons.Rounded.Link,
                gradient = listOf(Color(0xFF72183E), Color(0xFF361127)),
                locked = drivePremiumLocked,
                onClick = {
                    if (drivePremiumLocked) {
                        onOpenPremium()
                    } else {
                        controller.showPublicDriveDialog = true
                        controller.librarySource = LibrarySource.PUBLIC_DRIVE
                    }
                }
            )
        }
        item {
            LibrarySourceCard(
                title = "MeloWare Internet",
                count = controller.sourceCount(LibrarySource.INTERNET),
                icon = Icons.Rounded.Wifi,
                gradient = listOf(Color(0xFF0C6774), Color(0xFF102A48)),
                onClick = {
                    controller.librarySource = LibrarySource.INTERNET
                    controller.libraryTab = LibraryTab.FOLDERS
                    controller.libraryQuery = ""
                }
            )
        }
        item {
            LibrarySourceCard(
                title = mwString("library.downloaded_source", "Đã tải Offline"),
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
    locked: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    val tileGradient = if (colors.isDark) gradient else listOf(
        androidx.compose.ui.graphics.lerp(Color.White, gradient.first(), 0.12f),
        androidx.compose.ui.graphics.lerp(Color.White, gradient.last(), 0.05f)
    )
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
                .background(Brush.linearGradient(tileGradient))
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (colors.isDark) Color.White else gradient.first(),
                    modifier = Modifier.size(32.dp)
                )
                if (locked) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                        shape = CircleShape,
                        color = Color(0xFF25152F),
                        border = BorderStroke(1.dp, Color(0xFFFFD54F))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = mwString("premium.required", "Premium"),
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            Column {
                Text(title,
                    color = if (colors.isDark) Color.White else colors.textPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(mwString("library.count_songs", "{count} bài hát", "count" to count),
                    color = if (colors.isDark) Color.White.copy(alpha = 0.72f) else colors.textSecondary,
                    fontSize = 11.sp)
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
                                mwString("home.search_hint", "Tìm bài hát, ca sĩ, album, thể loại..."),
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
private fun PersonalDrivePickerStatus(
    busy: Boolean,
    error: String?,
    onOpenPicker: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (error == null) colors.cardRaised else colors.red.copy(alpha = 0.10f),
        border = BorderStroke(
            1.dp,
            if (error == null) colors.borderSoft else colors.red.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(21.dp),
                    strokeWidth = 2.dp,
                    color = colors.purpleBright
                )
            } else {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = colors.red)
            }
            Text(
                text = if (busy) mwString("library.importing_drive", "Đang nhập nhạc đã chọn từ Google Drive...") else error.orEmpty(),
                color = if (error == null) colors.textSecondary else colors.red,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            if (!busy && error != null) {
                TextButton(onClick = onOpenPicker) {
                    Text(mwString("library.select_again", "Chọn lại"), color = colors.purpleBright)
                }
            }
        }
    }
}

@Composable
private fun SourceFilterChips(
    controller: MusicController,
    drivePremiumLocked: Boolean,
    onOpenPremium: () -> Unit
) {
    val sources = listOf(
        LibrarySource.ALL to mwString("library.all_count", "Tất cả ({count})", "count" to controller.sourceCount(LibrarySource.ALL)),
        LibrarySource.LOCAL to mwString("library.local_music", "Nhạc trên máy"),
        LibrarySource.PERSONAL_DRIVE to mwString("home.my_drive", "Drive của tôi"),
        LibrarySource.PUBLIC_DRIVE to mwString("library.shared_drive", "Drive chia sẻ"),
        LibrarySource.INTERNET to "Internet P2P",
        LibrarySource.FAVORITES to mwString("library.favorites", "Yêu thích"),
        LibrarySource.DOWNLOADED to mwString("library.downloaded_short", "Đã tải")
    )
    HorizontalChips(
        items = sources.map { (source, label) ->
            val lockedLabel = if (
                drivePremiumLocked && source in setOf(
                    LibrarySource.PERSONAL_DRIVE,
                    LibrarySource.PUBLIC_DRIVE
                )
            ) "$label  👑" else label
            lockedLabel to (controller.librarySource == source)
        },
        onClick = { index ->
            val source = sources[index].first
            if (drivePremiumLocked && source in setOf(
                    LibrarySource.PERSONAL_DRIVE,
                    LibrarySource.PUBLIC_DRIVE
                )
            ) {
                onOpenPremium()
            } else {
                controller.librarySource = source
            }
        }
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

private sealed interface SmartPlaylistTarget {
    data class BuiltIn(val type: BuiltInSmartPlaylist) : SmartPlaylistTarget
    data class Custom(val definition: SmartPlaylistDefinition) : SmartPlaylistTarget
}

@Composable
private fun SmartPlaylistProSection(
    controller: MusicController,
    premiumLocked: Boolean,
    onOpenPremium: () -> Unit,
    onOpenBuiltIn: (BuiltInSmartPlaylist) -> Unit,
    onOpenCustom: (SmartPlaylistDefinition) -> Unit,
    onCreateCustom: () -> Unit,
    onEditCustom: (SmartPlaylistDefinition) -> Unit,
    onDeleteCustom: (SmartPlaylistDefinition) -> Unit,
    duplicateGroups: List<SmartDuplicateGroup>,
    duplicateScanDone: Boolean,
    duplicateScanBusy: Boolean,
    onOpenDuplicates: () -> Unit
) {
    val colors = LocalMusicColors.current
    val builtIns = listOf(
        Triple(BuiltInSmartPlaylist.MOST_PLAYED, Icons.Rounded.Headphones, true),
        Triple(BuiltInSmartPlaylist.RECENTLY_PLAYED, Icons.Rounded.History, false),
        Triple(BuiltInSmartPlaylist.NEWLY_ADDED, Icons.Rounded.NewReleases, false),
        Triple(BuiltInSmartPlaylist.NEVER_PLAYED, Icons.Rounded.MusicNote, true),
        Triple(BuiltInSmartPlaylist.LONG_NOT_HEARD, Icons.Rounded.RestartAlt, true)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.purple.copy(alpha = if (colors.isDark) 0.42f else 0.18f),
                            colors.blue.copy(alpha = if (colors.isDark) 0.26f else 0.12f),
                            colors.card
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        color = colors.purple.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.40f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                mwString("smart.title", "Smart Playlist Pro"),
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("PRO", color = colors.purpleBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            mwString(
                                "smart.subtitle",
                                "Playlist tự cập nhật theo thói quen nghe và quy tắc của bạn"
                            ),
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                    if (premiumLocked) {
                        IconButton(onClick = onOpenPremium) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.purpleBright)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        mwString("smart.local_db", "Hoạt động trên thiết bị · Không cần máy chủ"),
                        color = colors.textMuted,
                        fontSize = 10.5.sp
                    )
                    Text(
                        if (controller.smartPlaylists.ready) mwString("smart.ready", "Đã sẵn sàng")
                        else mwString("smart.loading", "Đang khởi tạo"),
                        color = if (controller.smartPlaylists.ready) colors.green else colors.textMuted,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(builtIns, key = { it.first.name }) { (type, icon, _) ->
                val locked = premiumLocked
                val count = controller.builtInSmartPlaylistCount(type)
                SmartBuiltInPlaylistCard(
                    type = type,
                    icon = icon,
                    count = count,
                    locked = locked,
                    onClick = {
                        if (locked) onOpenPremium() else onOpenBuiltIn(type)
                    }
                )
            }
        }

        OutlinedButton(
            onClick = {
                if (premiumLocked) onOpenPremium() else onCreateCustom()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.75f))
        ) {
            Icon(
                if (premiumLocked) Icons.Rounded.Lock else Icons.Rounded.Add,
                contentDescription = null,
                tint = colors.purpleBright,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                mwString("smart.create", "Tạo playlist thông minh"),
                color = colors.purpleBright,
                fontWeight = FontWeight.Bold
            )
        }

        if (controller.smartPlaylists.customPlaylists.isNotEmpty()) {
            Text(
                mwString("smart.your_playlists", "Playlist thông minh của bạn"),
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            controller.smartPlaylists.customPlaylists.forEach { definition ->
                SmartCustomPlaylistRow(
                    definition = definition,
                    count = controller.customSmartPlaylistCount(definition),
                    locked = premiumLocked,
                    onOpen = {
                        if (premiumLocked) onOpenPremium() else onOpenCustom(definition)
                    },
                    onEdit = {
                        if (premiumLocked) onOpenPremium() else onEditCustom(definition)
                    },
                    onDelete = { onDeleteCustom(definition) }
                )
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (premiumLocked) onOpenPremium() else onOpenDuplicates()
                },
            shape = RoundedCornerShape(17.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = colors.blue.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.blue)
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        mwString("smart.duplicates", "Bài hát có thể bị trùng"),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            duplicateScanBusy -> mwString("smart.duplicate_scanning", "Đang quét thư viện…")
                            !duplicateScanDone -> mwString("smart.duplicate_scan_hint", "Nhấn để kiểm tra kho nhạc")
                            else -> mwString(
                                "smart.duplicate_count",
                                "{groups} nhóm · {files} file",
                                "groups" to duplicateGroups.size,
                                "files" to duplicateGroups.sumOf { it.songs.size }
                            )
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    if (premiumLocked) Icons.Rounded.Lock else Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = if (premiumLocked) colors.purpleBright else colors.textMuted
                )
            }
        }

        controller.smartPlaylists.lastError?.let { error ->
            Surface(
                color = colors.red.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.red.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(error, color = colors.red, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = controller.smartPlaylists::clearError, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartBuiltInPlaylistCard(
    type: BuiltInSmartPlaylist,
    icon: ImageVector,
    count: Int,
    locked: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier
            .width(154.dp)
            .height(126.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(
            1.dp,
            if (locked) colors.border else colors.purpleBright.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = RoundedCornerShape(11.dp),
                    color = colors.purple.copy(alpha = 0.14f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(21.dp))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (locked) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(17.dp))
                }
            }
            Column {
                Text(
                    smartBuiltInTitle(type),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    mwString("library.count_songs", "{count} bài hát", "count" to count),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SmartCustomPlaylistRow(
    definition: SmartPlaylistDefinition,
    count: Int,
    locked: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 12.dp, top = 9.dp, bottom = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.purple.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.purpleBright)
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    definition.playlist.name,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    mwString(
                        "smart.custom_meta",
                        "{count} bài · {rules} điều kiện",
                        "count" to count,
                        "rules" to definition.rules.size
                    ),
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            if (locked) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.textMuted, modifier = Modifier.padding(8.dp))
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = mwString("common.edit", "Sửa"), tint = colors.textSecondary)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = mwString("common.delete", "Xóa"), tint = colors.red)
            }
        }
    }
}

@Composable
private fun SmartPlaylistSongsDialog(
    controller: MusicController,
    title: String,
    songs: List<SongItem>,
    onDismiss: () -> Unit,
    onMore: (SongItem) -> Unit,
    folderMode: Boolean = false
) {
    val colors = LocalMusicColors.current
    // Multi-selection is scoped to this folder/playlist. A new folder starts clean.
    val selectedIds = remember(title, songs.map { it.id }) { mutableStateListOf<String>() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = colors.purple.copy(alpha = 0.16f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (folderMode) Icons.Rounded.Folder else Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = colors.purpleBright
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (folderMode) {
                                mwString("library.count_songs", "{count} bài hát", "count" to songs.size)
                            } else {
                                mwString("smart.auto_updated", "{count} bài · Cập nhật tự động", "count" to songs.size)
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                controller.playCollection(songs, 0)
                                if (folderMode) onDismiss()
                            }
                        },
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(mwString("smart.play_all", "Phát tất cả"))
                    }
                    OutlinedButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                controller.playCollection(songs.shuffled(), 0)
                                if (folderMode) onDismiss()
                            }
                        },
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(mwString("smart.shuffle", "Ngẫu nhiên"), color = colors.purpleBright)
                    }
                }

                if (songs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedIds.size}/${songs.size} bài đã chọn",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (selectedIds.size == songs.size) {
                                selectedIds.clear()
                            } else {
                                selectedIds.clear()
                                selectedIds.addAll(songs.map { it.id })
                            }
                        }) {
                            Text(
                                if (selectedIds.size == songs.size)
                                    mwString("library.unselect_all", "Bỏ chọn")
                                else mwString("library.select_all", "Chọn tất cả"),
                                color = colors.purpleBright
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val chosen = songs.filter { it.id in selectedIds }
                            if (chosen.isNotEmpty()) {
                                controller.playCollection(chosen, 0)
                                onDismiss()
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Phát đã chọn (${selectedIds.size})")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (songs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (folderMode) "Thư mục này chưa có bài hát" else
                                    mwString("smart.no_matching", "Chưa có bài hát phù hợp với điều kiện"),
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            LaunchedEffect(song.id) { controller.ensureArtwork(song) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = song.id in selectedIds,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (song.id !in selectedIds) selectedIds.add(song.id)
                                        } else selectedIds.remove(song.id)
                                    },
                                    modifier = Modifier.size(40.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                )
                                SongRow(
                                    song = song,
                                    artworkUrl = controller.artworkFor(song),
                                    isCurrent = controller.currentSong?.id == song.id,
                                    isFavorite = controller.isFavorite(song),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (folderMode) {
                                            // Tapping the track (not its checkbox) plays only that song.
                                            controller.playCollection(listOf(song), 0)
                                            onDismiss()
                                        } else {
                                            controller.playSong(song, songs)
                                            onDismiss()
                                        }
                                    },
                                    onFavorite = { controller.toggleFavorite(song) },
                                    onMore = if (controller.canUseLocalAudioTools(song)) {
                                        { onMore(song) }
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
private fun SmartPlaylistEditorDialog(
    initial: SmartPlaylistDraft,
    onDismiss: () -> Unit,
    onSave: (SmartPlaylistDraft) -> Unit
) {
    val colors = LocalMusicColors.current
    var name by remember(initial.id, initial.name) { mutableStateOf(initial.name) }
    var matchMode by remember(initial.id, initial.matchMode) { mutableStateOf(initial.matchMode) }
    var sortType by remember(initial.id, initial.sortType) { mutableStateOf(initial.sortType) }
    var maxItems by remember(initial.id, initial.maxItems) { mutableStateOf(initial.maxItems) }
    val rules = remember(initial.id, initial.rules.hashCode()) {
        mutableStateListOf<SmartPlaylistRuleDraft>().apply {
            addAll(initial.rules.ifEmpty { listOf(SmartPlaylistRuleDraft()) })
        }
    }
    val canSave = name.trim().isNotEmpty() && rules.isNotEmpty() && rules.all(::smartRuleIsValid)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.93f),
            shape = RoundedCornerShape(22.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (initial.id > 0L) mwString("smart.edit_title", "Sửa Smart Playlist")
                            else mwString("smart.create_title", "Tạo Smart Playlist"),
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            mwString("smart.editor_desc", "Danh sách sẽ tự cập nhật khi thư viện hoặc thói quen nghe thay đổi"),
                            color = colors.textSecondary,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(mwString("smart.name", "Tên playlist")) },
                        placeholder = { Text(mwString("smart.name_example", "Ví dụ: Ballad yêu thích")) }
                    )

                    Text(
                        mwString("smart.match", "Khớp điều kiện"),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmartChoicePill(
                            text = mwString("smart.match_all", "Tất cả (AND)"),
                            selected = matchMode == SmartMatchMode.ALL,
                            modifier = Modifier.weight(1f),
                            onClick = { matchMode = SmartMatchMode.ALL }
                        )
                        SmartChoicePill(
                            text = mwString("smart.match_any", "Ít nhất một (OR)"),
                            selected = matchMode == SmartMatchMode.ANY,
                            modifier = Modifier.weight(1f),
                            onClick = { matchMode = SmartMatchMode.ANY }
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            mwString("smart.conditions", "Điều kiện"),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${rules.size}/12",
                            color = colors.textMuted,
                            fontSize = 11.sp
                        )
                    }

                    rules.forEachIndexed { index, rule ->
                        SmartRuleEditorCard(
                            index = index,
                            rule = rule,
                            canDelete = rules.size > 1,
                            onChange = { updated -> rules[index] = updated },
                            onDelete = { if (rules.size > 1) rules.removeAt(index) }
                        )
                    }

                    if (rules.size < 12) {
                        TextButton(
                            onClick = { rules.add(SmartPlaylistRuleDraft()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.purpleBright)
                            Spacer(Modifier.width(6.dp))
                            Text(mwString("smart.add_condition", "Thêm điều kiện"), color = colors.purpleBright)
                        }
                    }

                    Text(
                        mwString("smart.sort", "Sắp xếp"),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    SmartSortPicker(selected = sortType, onSelected = { sortType = it })

                    Text(
                        mwString("smart.limit", "Giới hạn số bài"),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(20, 50, 100, 0)) { value ->
                            SmartChoicePill(
                                text = if (value == 0) mwString("smart.all", "Tất cả") else value.toString(),
                                selected = maxItems == value,
                                onClick = { maxItems = value }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(mwString("common.cancel", "Hủy"), color = colors.textSecondary)
                    }
                    Button(
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name.trim(),
                                    matchMode = matchMode,
                                    sortType = sortType,
                                    maxItems = maxItems,
                                    rules = rules.toList()
                                )
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(mwString("common.save", "Lưu"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartRuleEditorCard(
    index: Int,
    rule: SmartPlaylistRuleDraft,
    canDelete: Boolean,
    onChange: (SmartPlaylistRuleDraft) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.borderSoft)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mwString("smart.condition_number", "Điều kiện {number}", "number" to (index + 1)),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.red, modifier = Modifier.size(17.dp))
                    }
                }
            }

            SmartFieldPicker(
                selected = rule.field,
                onSelected = { field ->
                    onChange(
                        rule.copy(
                            field = field,
                            operator = smartRuleDefaultOperator(field),
                            value = if (field == SmartRuleField.FAVORITE) "true" else "",
                            secondValue = ""
                        )
                    )
                }
            )
            SmartOperatorPicker(
                field = rule.field,
                selected = rule.operator,
                onSelected = { operator -> onChange(rule.copy(operator = operator)) }
            )

            when (rule.field) {
                SmartRuleField.FAVORITE -> {
                    Text(
                        if (rule.operator == SmartRuleOperator.IS_FALSE)
                            mwString("smart.not_favorite", "Chỉ lấy bài chưa yêu thích")
                        else mwString("smart.is_favorite", "Chỉ lấy bài đã yêu thích"),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }

                SmartRuleField.SOURCE -> {
                    SmartSourcePicker(
                        selectedName = rule.value,
                        onSelected = { source -> onChange(rule.copy(value = source.name)) }
                    )
                }

                else -> {
                    val numeric = rule.field in setOf(
                        SmartRuleField.DURATION_MINUTES,
                        SmartRuleField.PLAY_COUNT,
                        SmartRuleField.DATE_ADDED_DAYS,
                        SmartRuleField.LAST_PLAYED_DAYS
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rule.value,
                            onValueChange = { onChange(rule.copy(value = it.take(200))) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text(smartRuleValueLabel(rule.field, first = true)) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                                imeAction = ImeAction.Next
                            )
                        )
                        if (rule.operator == SmartRuleOperator.BETWEEN) {
                            OutlinedTextField(
                                value = rule.secondValue,
                                onValueChange = { onChange(rule.copy(secondValue = it.take(200))) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text(smartRuleValueLabel(rule.field, first = false)) },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartFieldPicker(
    selected: SmartRuleField,
    onSelected: (SmartRuleField) -> Unit
) {
    val options = SmartRuleField.values().map { it to smartFieldLabel(it) }
    SmartTextDropdown(
        selectedLabel = smartFieldLabel(selected),
        options = options,
        onSelected = onSelected
    )
}

@Composable
private fun SmartOperatorPicker(
    field: SmartRuleField,
    selected: SmartRuleOperator,
    onSelected: (SmartRuleOperator) -> Unit
) {
    val available = smartRuleOperators(field)
    val safeSelected = selected.takeIf { it in available } ?: available.first()
    val options = available.map { it to smartOperatorLabel(it) }
    SmartTextDropdown(
        selectedLabel = smartOperatorLabel(safeSelected),
        options = options,
        onSelected = onSelected
    )
}

@Composable
private fun SmartSortPicker(
    selected: SmartSortType,
    onSelected: (SmartSortType) -> Unit
) {
    val options = SmartSortType.values().map { it to smartSortLabel(it) }
    SmartTextDropdown(
        selectedLabel = smartSortLabel(selected),
        options = options,
        onSelected = onSelected
    )
}

@Composable
private fun SmartSourcePicker(
    selectedName: String,
    onSelected: (SongSource) -> Unit
) {
    val selected = SongSource.values().firstOrNull { it.name == selectedName }
    val options = SongSource.values().map { it to smartSourceLabel(it) }
    SmartTextDropdown(
        selectedLabel = selected?.let { smartSourceLabel(it) }
            ?: mwString("smart.choose_source", "Chọn nguồn nhạc"),
        options = options,
        onSelected = onSelected
    )
}

@Composable
private fun <T> SmartTextDropdown(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelected: (T) -> Unit
) {
    val colors = LocalMusicColors.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedLabel,
                    color = colors.textPrimary,
                    fontSize = 12.5.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("▾", color = colors.purpleBright, fontSize = 16.sp)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 210.dp, max = 330.dp)
                .background(colors.card)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = colors.textPrimary, fontSize = 12.sp) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun SmartChoicePill(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.purple.copy(alpha = 0.16f) else colors.card,
        border = BorderStroke(1.dp, if (selected) colors.purpleBright else colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text,
                color = if (selected) colors.purpleBright else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SmartPlaylistDeleteDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    mwString("smart.delete_title", "Xóa Smart Playlist?"),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
                Text(
                    mwString(
                        "smart.delete_desc",
                        "Chỉ xóa quy tắc playlist “{name}”. Các file nhạc không bị xóa.",
                        "name" to name
                    ),
                    color = colors.textSecondary,
                    fontSize = 12.5.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(mwString("common.cancel", "Hủy"), color = colors.textSecondary)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(mwString("common.delete", "Xóa"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartDuplicateManagerDialog(
    controller: MusicController,
    groups: List<SmartDuplicateGroup>,
    onDismiss: () -> Unit,
    onMore: (SongItem) -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(22.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = colors.blue.copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.blue)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            mwString("smart.duplicates", "Bài hát có thể bị trùng"),
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            mwString(
                                "smart.duplicate_count",
                                "{groups} nhóm · {files} file",
                                "groups" to groups.size,
                                "files" to groups.sumOf { it.songs.size }
                            ),
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    mwString(
                        "smart.duplicate_note",
                        "MeloWave chỉ gợi ý theo tên, nghệ sĩ và thời lượng. Hãy nghe thử trước khi xóa."
                    ),
                    color = colors.textMuted,
                    fontSize = 10.5.sp
                )
                Spacer(Modifier.height(8.dp))

                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.green, modifier = Modifier.size(46.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                mwString("smart.no_duplicates", "Chưa phát hiện bài hát trùng"),
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(groups, key = { it.key }) { group ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.card,
                                border = BorderStroke(1.dp, colors.borderSoft)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        group.title,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${mwArtistLabel(group.artist)} · ${group.songs.size} file",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(Modifier.height(5.dp))
                                    group.songs.forEachIndexed { index, song ->
                                        DuplicateSongCompactRow(
                                            controller = controller,
                                            song = song,
                                            onPlay = { controller.playCollection(group.songs, index) },
                                            onMore = { onMore(song) }
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

@Composable
private fun DuplicateSongCompactRow(
    controller: MusicController,
    song: SongItem,
    onPlay: () -> Unit,
    onMore: () -> Unit
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onPlay)
            .padding(vertical = 7.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            song = song,
            artworkUrl = controller.artworkFor(song),
            modifier = Modifier.size(43.dp),
            cornerRadius = 10
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(song.folderName.ifBlank { controller.sourceLabel(song.source) })
                    append(" · ")
                    append(controller.formatBadge(song))
                    append(" · ")
                    append(song.durationLabel)
                },
                color = colors.textSecondary,
                fontSize = 10.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onMore, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

private fun smartRuleIsValid(rule: SmartPlaylistRuleDraft): Boolean {
    if (rule.field == SmartRuleField.FAVORITE) return true
    if (rule.value.isBlank()) return false

    val numericField = rule.field in setOf(
        SmartRuleField.DURATION_MINUTES,
        SmartRuleField.PLAY_COUNT,
        SmartRuleField.DATE_ADDED_DAYS,
        SmartRuleField.LAST_PLAYED_DAYS
    )
    if (numericField) {
        val first = rule.value.replace(',', '.').toDoubleOrNull() ?: return false
        if (first < 0.0) return false
        if (rule.operator == SmartRuleOperator.BETWEEN) {
            val second = rule.secondValue.replace(',', '.').toDoubleOrNull() ?: return false
            if (second < 0.0) return false
        }
    } else if (rule.operator == SmartRuleOperator.BETWEEN && rule.secondValue.isBlank()) {
        return false
    }
    return true
}

@Composable
private fun smartBuiltInTitle(type: BuiltInSmartPlaylist): String = when (type) {
    BuiltInSmartPlaylist.MOST_PLAYED -> mwString("smart.most_played", "Nghe nhiều nhất")
    BuiltInSmartPlaylist.RECENTLY_PLAYED -> mwString("smart.recently_played", "Nghe gần đây")
    BuiltInSmartPlaylist.NEWLY_ADDED -> mwString("smart.newly_added", "Mới thêm")
    BuiltInSmartPlaylist.NEVER_PLAYED -> mwString("smart.never_played", "Chưa từng nghe")
    BuiltInSmartPlaylist.LONG_NOT_HEARD -> mwString("smart.long_not_heard", "Lâu chưa nghe")
}

@Composable
private fun smartFieldLabel(field: SmartRuleField): String = when (field) {
    SmartRuleField.ARTIST -> mwString("smart.field.artist", "Nghệ sĩ")
    SmartRuleField.ALBUM -> mwString("smart.field.album", "Album")
    SmartRuleField.GENRE -> mwString("smart.field.genre", "Thể loại")
    SmartRuleField.FOLDER -> mwString("smart.field.folder", "Thư mục")
    SmartRuleField.SOURCE -> mwString("smart.field.source", "Nguồn nhạc")
    SmartRuleField.DURATION_MINUTES -> mwString("smart.field.duration", "Thời lượng (phút)")
    SmartRuleField.PLAY_COUNT -> mwString("smart.field.play_count", "Số lượt nghe")
    SmartRuleField.DATE_ADDED_DAYS -> mwString("smart.field.date_added", "Ngày thêm")
    SmartRuleField.LAST_PLAYED_DAYS -> mwString("smart.field.last_played", "Lần nghe gần nhất")
    SmartRuleField.FAVORITE -> mwString("smart.field.favorite", "Yêu thích")
}

@Composable
private fun smartOperatorLabel(operator: SmartRuleOperator): String = when (operator) {
    SmartRuleOperator.EQUALS -> mwString("smart.op.equals", "Bằng")
    SmartRuleOperator.NOT_EQUALS -> mwString("smart.op.not_equals", "Không bằng")
    SmartRuleOperator.CONTAINS -> mwString("smart.op.contains", "Có chứa")
    SmartRuleOperator.NOT_CONTAINS -> mwString("smart.op.not_contains", "Không chứa")
    SmartRuleOperator.GREATER_THAN -> mwString("smart.op.greater", "Lớn hơn")
    SmartRuleOperator.LESS_THAN -> mwString("smart.op.less", "Nhỏ hơn")
    SmartRuleOperator.BETWEEN -> mwString("smart.op.between", "Trong khoảng")
    SmartRuleOperator.WITHIN_DAYS -> mwString("smart.op.within_days", "Trong số ngày gần đây")
    SmartRuleOperator.OLDER_THAN_DAYS -> mwString("smart.op.older_days", "Cũ hơn số ngày")
    SmartRuleOperator.IS_TRUE -> mwString("smart.op.yes", "Có")
    SmartRuleOperator.IS_FALSE -> mwString("smart.op.no", "Không")
}

@Composable
private fun smartSortLabel(sort: SmartSortType): String = when (sort) {
    SmartSortType.MOST_PLAYED -> mwString("smart.sort.most_played", "Nghe nhiều nhất")
    SmartSortType.RECENTLY_PLAYED -> mwString("smart.sort.recent", "Nghe gần đây")
    SmartSortType.NEWEST_ADDED -> mwString("smart.sort.newest", "Mới thêm trước")
    SmartSortType.TITLE -> mwString("smart.sort.title", "Tên bài A–Z")
    SmartSortType.ARTIST -> mwString("smart.sort.artist", "Nghệ sĩ A–Z")
    SmartSortType.DURATION_ASC -> mwString("smart.sort.duration_asc", "Thời lượng ngắn trước")
    SmartSortType.DURATION_DESC -> mwString("smart.sort.duration_desc", "Thời lượng dài trước")
    SmartSortType.DAILY_RANDOM -> mwString("smart.sort.random", "Ngẫu nhiên theo ngày")
}

@Composable
private fun smartSourceLabel(source: SongSource): String = when (source) {
    SongSource.LOCAL_MEDIASTORE -> mwString("smart.source.local", "Nhạc trên máy")
    SongSource.PERSONAL_DRIVE -> mwString("smart.source.my_drive", "Google Drive của tôi")
    SongSource.PUBLIC_DRIVE -> mwString("smart.source.public_drive", "Drive chia sẻ")
    SongSource.DOWNLOADED -> mwString("smart.source.downloaded", "Đã tải Offline")
    SongSource.ONLINE_HTTP -> mwString("smart.source.online", "Nhạc trực tuyến")
    SongSource.INTERNET_P2P -> mwString("smart.source.p2p", "MeloWave Internet")
    SongSource.MOVIE -> mwString("smart.source.movie", "Movie")
    SongSource.APP_RESOURCE -> mwString("smart.source.app", "Âm thanh ứng dụng")
}

@Composable
private fun smartRuleValueLabel(field: SmartRuleField, first: Boolean): String = when (field) {
    SmartRuleField.DURATION_MINUTES -> if (first) mwString("smart.value.min", "Từ / giá trị")
    else mwString("smart.value.max", "Đến")
    SmartRuleField.PLAY_COUNT -> if (first) mwString("smart.value.count", "Số lượt")
    else mwString("smart.value.count_to", "Đến lượt")
    SmartRuleField.DATE_ADDED_DAYS,
    SmartRuleField.LAST_PLAYED_DAYS -> mwString("smart.value.days", "Số ngày")
    else -> if (first) mwString("smart.value.text", "Giá trị")
    else mwString("smart.value.second", "Giá trị 2")
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
                            text = mwString("library.personal_drive_title", "Google Drive của tôi"),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = controller.personalDriveAccountEmail
                                ?: mwString("library.picker_file", "Chọn file bằng Google Picker"),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !controller.personalDriveBusy) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = mwString("common.close", "Đóng"),
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
                                text = if (BuildConfig.DRIVE_READONLY_REVIEW) {
                                    "MeloWave yêu cầu quyền đọc để duyệt thư mục và phát các file âm thanh trong Google Drive. Ứng dụng không sửa hoặc xóa file Drive."
                                } else {
                                    mwString("library.picker_privacy", "MeloWare chỉ truy cập các file hoặc thư mục bạn chủ động chọn bằng Google Picker.")
                                },
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Button(
                                onClick = onAuthorize,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                            ) {
                                Text(if (BuildConfig.DRIVE_READONLY_REVIEW) "Cấp quyền đọc Drive" else mwString("library.open_picker", "Mở Google Picker"))
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
                        Text(mwString("library.reading_drive", "Đang đọc Google Drive..."), color = colors.textSecondary)
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
                                    Text(mwString("library.reauthorize", "Cấp lại quyền"), color = colors.purpleBright)
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
                                contentDescription = mwString("library.back", "Trở lại"),
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
                            mwString("library.folder_song_count", "{folders} thư mục · {songs} bài", "folders" to controller.personalDriveEntries.count { it.isFolder }, "songs" to playable.size),
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        TextButton(
                            onClick = controller::toggleAllPersonalDrivePlayable,
                            enabled = !controller.personalDriveBusy && playable.isNotEmpty()
                        ) {
                            val allSelected = playable.isNotEmpty() &&
                                    playable.all { controller.personalDriveSelectedIds.containsKey(it.id) }
                            Text(if (allSelected) mwString("library.unselect_all", "Bỏ chọn") else mwString("library.select_all", "Chọn tất cả"))
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
                                            mwString("library.file_not_downloadable", "Không hỗ trợ hoặc file không cho phép tải"),
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
                            Text(mwString("library.add_count", "Thêm {count}", "count" to controller.personalDriveSelectedIds.size))
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
                            Text(mwString("library.play", "Phát"))
                        }
                    }

                    Button(
                        onClick = { controller.addCurrentPersonalDriveFolder(playNow = true) },
                        enabled = !controller.personalDriveBusy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                    ) {
                        Text(mwString("library.play_folder_recursive", "Phát toàn bộ thư mục này (gồm thư mục con)"))
                    }
                } else if (!controller.personalDriveBusy) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mwString("library.folder_load_failed", "Chưa tải được danh sách thư mục"), color = colors.textSecondary)
                            TextButton(onClick = controller::openPersonalDriveRoot) {
                                Text(mwString("library.reload", "Tải lại"), color = colors.purpleBright)
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class OnlineLinkProvider {
    GOOGLE_DRIVE,
    MEDIAFIRE
}

@Composable
private fun PublicDriveDialog(
    controller: MusicController,
    onDismiss: () -> Unit
) {
    var onlineProvider by remember { mutableStateOf(OnlineLinkProvider.GOOGLE_DRIVE) }
    if (onlineProvider == OnlineLinkProvider.MEDIAFIRE) {
        MediaFireLinkDialog(
            controller = controller,
            onDismiss = onDismiss,
            onShowGoogleDrive = { onlineProvider = OnlineLinkProvider.GOOGLE_DRIVE }
        )
        return
    }

    val colors = LocalMusicColors.current
    val context = LocalContext.current
    var showLinkHistory by remember { mutableStateOf(false) }
    var showArtistView by remember { mutableStateOf(true) }
    var expandedArtistFolderKey by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var archivePassword by remember { mutableStateOf("") }
    var driveCopyDestination by remember { mutableStateOf("Nhạc đã lưu") }
    var pendingRemoveLinkSlot by remember { mutableStateOf<Int?>(null) }
    val linkSlotListState = rememberLazyListState()

    val activeSlot = controller.publicActiveSlot
    val openedArchive = controller.publicArchive
    val slotCount = controller.publicDriveSlotCount
    var previousSlotCount by remember { mutableStateOf(slotCount) }
    val slotSelectedCounts = List(slotCount) { slot ->
        controller.publicDriveSlotSelectedCount(slot)
    }
    val representedSelectedSourceCount = slotSelectedCounts.count { it > 0 }
    val selectedSourceCount = controller.publicPendingSourceCount
    val hiddenSelectedSourceCount = (selectedSourceCount - representedSelectedSourceCount).coerceAtLeast(0)
    val loadedLinkCount = (0 until slotCount).count { slot ->
        controller.publicDriveSlotLoaded(slot)
    }
    val slotSelectionSummary = buildList {
        slotSelectedCounts.forEachIndexed { index, count ->
            if (count > 0) add("Link ${index + 1}: $count")
        }
        if (hiddenSelectedSourceCount > 0) add("Nguồn khác: $hiddenSelectedSourceCount")
    }.joinToString(" · ")
    val normalizedQuery = searchQuery.trim().lowercase(Locale.ROOT)
    val visibleArtistFolders = if (normalizedQuery.isBlank()) {
        controller.publicArtistFolders
    } else {
        controller.publicArtistFolders.filter { folder ->
            folder.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    folder.linkName.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
    }
    val visibleEntries = if (normalizedQuery.isBlank()) {
        controller.publicEntries
    } else {
        controller.publicEntries.filter { it.name.lowercase(Locale.ROOT).contains(normalizedQuery) }
    }
    val visibleArchiveEntries = if (normalizedQuery.isBlank()) {
        openedArchive?.audioEntries.orEmpty()
    } else {
        openedArchive?.audioEntries.orEmpty().filter { entry ->
            entry.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    entry.folderPath.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
    }

    LaunchedEffect(controller.publicArchivePasswordName) {
        archivePassword = ""
    }

    LaunchedEffect(slotCount) {
        if (slotCount > previousSlotCount && slotCount > 0) {
            linkSlotListState.animateScrollToItem((slotCount - 1).coerceAtLeast(0))
        }
        previousSlotCount = slotCount
    }

    LaunchedEffect(activeSlot, controller.publicRoot?.folderId) {
        showArtistView = true
        expandedArtistFolderKey = null
        searchQuery = ""
    }

    LaunchedEffect(
        activeSlot,
        controller.publicPath.lastOrNull()?.id,
        controller.publicEntries.size
    ) {
        if (controller.publicEntries.any(controller::isPublicEntryArchive)) {
            // Archives are browsed from the real Drive tree, not the virtual artist groups.
            showArtistView = false
        }
    }

    Dialog(
        onDismissRequest = { if (!controller.publicDriveBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.background, colors.backgroundSoft, colors.background)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Full-screen header.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        enabled = !controller.publicDriveBusy
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textPrimary
                        )
                    }
                    // Cùng logo Drive + huy hiệu link như shortcut "Link Drive" trên Home.
                    GoogleDrivePublicLinkIcon(
                        modifier = Modifier.size(34.dp),
                        logoSize = 31.dp,
                        badgeSize = 15.dp
                    )
                    Spacer(Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Liên kết trực tuyến",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Google Drive · $loadedLinkCount/$slotCount link · ${controller.publicPendingCount} bài chọn",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = controller::addPublicDriveSlot,
                        enabled = controller.canAddPublicDriveSlot
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = if (controller.canAddPublicDriveSlot) {
                                "Thêm link Google Drive"
                            } else {
                                "Đã đạt tối đa ${controller.publicDriveSlotLimit} link"
                            },
                            tint = if (controller.canAddPublicDriveSlot) {
                                colors.purpleBright
                            } else {
                                colors.textMuted
                            }
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showLinkHistory = true },
                            enabled = !controller.publicDriveBusy && !controller.mediaFireBusy
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = "Lịch sử liên kết",
                                tint = colors.purpleBright
                            )
                        }
                        OnlineLinkHistoryMenu(
                            controller = controller,
                            expanded = showLinkHistory,
                            onDismiss = { showLinkHistory = false },
                            onSelectDrive = { saved -> controller.selectSavedPublicDriveLink(saved) },
                            onSelectMediaFire = { saved ->
                                controller.selectSavedMediaFireLink(saved)
                                onlineProvider = OnlineLinkProvider.MEDIAFIRE
                            }
                        )
                    }
                }

                // Danh sách workspace cuộn ngang: mặc định 2 link, có thể thêm Link 3...10.
                LazyRow(
                    state = linkSlotListState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(count = slotCount, key = { slot -> slot }) { slot ->
                        val selected = slot == activeSlot
                        val loaded = controller.publicDriveSlotLoaded(slot)
                        val hasLink = controller.publicDriveSlotUrl(slot).isNotBlank()
                        val slotSelectedCount = controller.publicDriveSlotSelectedCount(slot)
                        val slotFolderCount = controller.publicDriveSlotSelectedFolderCount(slot)
                        val slotBusy = controller.publicDriveSlotBusy(slot)
                        Surface(
                            modifier = Modifier
                                .width(165.dp)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .clickable(enabled = !controller.publicDriveBusy) {
                                    controller.selectPublicDriveSlot(slot)
                                },
                            shape = RoundedCornerShape(13.dp),
                            color = if (selected) {
                                colors.purple.copy(alpha = 0.22f)
                            } else {
                                colors.cardRaised.copy(alpha = 0.72f)
                            },
                            border = BorderStroke(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) colors.purpleBright else colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 46.dp)
                                    .padding(start = 7.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) colors.purple.copy(alpha = 0.32f)
                                            else colors.backgroundSoft
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        slotBusy -> CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            strokeWidth = 2.dp,
                                            color = colors.purpleBright
                                        )
                                        loaded -> Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = colors.purpleBright,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        else -> Icon(
                                            Icons.Rounded.Link,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "LINK ${slot + 1}",
                                            color = if (selected) colors.purpleBright else colors.textSecondary,
                                            fontSize = 10.sp,
                                            lineHeight = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        if (slotSelectedCount > 0 || slotFolderCount > 0) {
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                "$slotSelectedCount bài · $slotFolderCount mục",
                                                color = colors.purpleBright,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        if (loaded || hasLink) controller.publicDriveSlotDisplayName(slot)
                                        else "Chưa mở link",
                                        color = when {
                                            loaded -> colors.textPrimary
                                            hasLink -> colors.textSecondary
                                            else -> colors.textMuted
                                        },
                                        fontSize = 11.sp,
                                        lineHeight = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (slotCount > 2) {
                                    IconButton(
                                        onClick = { pendingRemoveLinkSlot = slot },
                                        enabled = !controller.publicDriveBusy &&
                                            controller.publicFolderSelectionBusyCount == 0,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Xóa Link ${slot + 1}",
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }

                // A single, auto-detecting URL field replaces two provider tabs and three buttons.
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    OnlineLinkInputRow(
                        value = controller.publicLinkText,
                        placeholder = "Dán link Drive, MediaFire, Fshare",
                        enabled = !controller.publicDriveBusy,
                        busy = controller.publicDriveBusy,
                        onValueChange = { value ->
                            if (controller.isMediaFireLink(value)) {
                                controller.setMediaFireLink(value)
                                onlineProvider = OnlineLinkProvider.MEDIAFIRE
                            } else {
                                controller.setPublicLink(value)
                            }
                        },
                        onPaste = {
                            val text = onlineLinkClipboard(context)
                            if (text.isNotBlank()) {
                                if (controller.isMediaFireLink(text)) {
                                    controller.setMediaFireLink(text)
                                    onlineProvider = OnlineLinkProvider.MEDIAFIRE
                                } else {
                                    controller.setPublicLink(text)
                                }
                            }
                        },
                        onClear = controller::clearPublicLink,
                        onOpen = controller::openPublicDriveLink
                    )

                    controller.publicDriveError?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.red.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.red.copy(alpha = 0.35f))
                        ) {
                            Text(
                                error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = colors.red,
                                fontSize = 11.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    controller.publicArchiveStatus?.let { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp,
                                color = colors.purpleBright
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                status,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (controller.publicPendingCount > 0 || controller.publicMixedFolderCount > 0 ||
                        controller.publicFolderSelectionBusyCount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (controller.publicFolderSelectionBusyCount > 0) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.purpleBright
                                )
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                (slotSelectionSummary.ifBlank { "Chưa có bài được chọn" }) +
                                        if (controller.publicMixedFolderCount > 0) {
                                            " · Đã lưu: ${controller.publicMixedFolderCount}"
                                        } else {
                                            ""
                                        },
                                color = colors.purpleBright,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (openedArchive != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            color = colors.purple.copy(alpha = 0.13f),
                            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = controller::closePublicArchive,
                                    enabled = !controller.publicDriveBusy,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.ArrowBack,
                                        contentDescription = "Quay lại thư mục Drive",
                                        tint = colors.textPrimary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.purple.copy(alpha = 0.24f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = colors.purpleBright,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        openedArchive.format.displayName,
                                        color = Color.White,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .background(colors.purple, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        openedArchive.source.name,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${openedArchive.audioEntries.size} bài · ${publicArchiveSizeLabel(openedArchive.source.size)}" +
                                                if (openedArchive.passwordProtected) " · Có mật khẩu" else "",
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val allArchiveSelected = openedArchive.audioEntries.isNotEmpty() &&
                                        openedArchive.audioEntries.all(controller::isPublicArchiveEntrySelected)
                                TextButton(
                                    onClick = controller::toggleAllPublicArchiveEntries,
                                    enabled = !controller.publicDriveBusy
                                ) {
                                    Text(
                                        if (allArchiveSelected) "Bỏ chọn" else "Chọn tất cả",
                                        color = colors.purpleBright,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardRaised.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp),
                                cursorBrush = SolidColor(colors.purpleBright),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                decorationBox = { inner ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (searchQuery.isBlank()) {
                                                Text(
                                                    "Tìm bài hát trong file nén",
                                                    color = colors.textMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            inner()
                                        }
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "Xóa tìm kiếm",
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    } else if (controller.publicRoot != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                true to mwString("library.drive_artist_view", "Thư mục ca sĩ"),
                                false to mwString("library.drive_folder_view", "Thư mục Drive")
                            ).forEach { (artists, label) ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { showArtistView = artists },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (showArtistView == artists) {
                                        colors.purple.copy(alpha = 0.23f)
                                    } else {
                                        colors.cardRaised
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (showArtistView == artists) colors.purpleBright else colors.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (artists) Icons.Rounded.Person else Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = if (showArtistView == artists) {
                                                colors.purpleBright
                                            } else {
                                                colors.textSecondary
                                            },
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            label,
                                            color = if (showArtistView == artists) {
                                                colors.purpleBright
                                            } else {
                                                colors.textSecondary
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardRaised.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp),
                                cursorBrush = SolidColor(colors.purpleBright),
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                decorationBox = { inner ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            tint = colors.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(7.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (searchQuery.isBlank()) {
                                                Text(
                                                    if (showArtistView) "Tìm thư mục ca sĩ" else "Tìm bài hát hoặc thư mục",
                                                    color = colors.textMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            inner()
                                        }
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = null,
                                                    tint = colors.textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (openedArchive == null && controller.publicPath.isNotEmpty() && !showArtistView) {
                        val playableEntries = controller.publicEntries.filter(controller::isPublicEntryPlayable)
                        val archiveEntries = controller.publicEntries.count(controller::isPublicEntryArchive)
                        val currentFolderSelected = controller.isCurrentPublicDriveFolderSelected()
                        val currentFolderBusy = controller.isCurrentPublicDriveFolderSelectionBusy()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = controller::goBackPublicFolder,
                                enabled = controller.publicPath.size > 1 && !controller.publicDriveBusy
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    contentDescription = mwString("library.back", "Quay lại"),
                                    tint = colors.textPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    controller.publicPath.joinToString(" / ") { it.name },
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${controller.publicEntries.count { it.isFolder }} thư mục · ${playableEntries.size} bài" +
                                            if (archiveEntries > 0) " · $archiveEntries file nén" else "",
                                    color = colors.textMuted,
                                    fontSize = 10.sp
                                )
                            }
                            TextButton(
                                onClick = controller::toggleCurrentPublicDriveFolder,
                                enabled = !controller.publicDriveBusy && !currentFolderBusy
                            ) {
                                if (currentFolderBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.purpleBright
                                    )
                                } else {
                                    Icon(
                                        if (currentFolderSelected) Icons.Rounded.Check else Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = colors.purpleBright,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (currentFolderSelected) "Bỏ chọn thư mục" else "Chọn thư mục này",
                                        color = colors.purpleBright,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (playableEntries.isNotEmpty()) {
                                TextButton(
                                    onClick = controller::toggleAllPublicEntries,
                                    enabled = !controller.publicDriveBusy
                                ) {
                                    val allSelected = playableEntries.all {
                                        controller.publicSelectedIds[it.id] == true
                                    }
                                    Text(
                                        if (allSelected) {
                                            mwString("library.unselect_all", "Bỏ chọn")
                                        } else {
                                            mwString("library.select_all", "Chọn bài")
                                        },
                                        color = colors.purpleBright,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.55f))
                )

                // Main selection list.
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (openedArchive != null) {
                        if (visibleArchiveEntries.isEmpty()) {
                            item(key = "archive_entries_empty") {
                                Text(
                                    if (searchQuery.isBlank()) {
                                        "File nén không có bài hát được hỗ trợ"
                                    } else {
                                        "Không tìm thấy bài hát phù hợp trong file nén"
                                    },
                                    modifier = Modifier.padding(18.dp),
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        items(visibleArchiveEntries, key = { "archive_${it.key}" }) { entry ->
                            val selected = controller.isPublicArchiveEntrySelected(entry)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(enabled = !controller.publicDriveBusy) {
                                        controller.togglePublicArchiveEntry(entry)
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    colors.purple.copy(alpha = 0.16f)
                                } else {
                                    colors.cardRaised.copy(alpha = 0.62f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.purpleBright.copy(alpha = 0.72f) else colors.border
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { controller.togglePublicArchiveEntry(entry) },
                                        enabled = !controller.publicDriveBusy,
                                        colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                    )
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = if (selected) colors.purpleBright else colors.textSecondary,
                                        modifier = Modifier.size(23.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            entry.name,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            buildString {
                                                if (entry.folderPath.isNotBlank()) {
                                                    append(entry.folderPath)
                                                    append(" · ")
                                                }
                                                append(publicArchiveSizeLabel(entry.size))
                                            },
                                            color = colors.textSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        openedArchive.format.displayName,
                                        color = colors.purpleBright,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(
                                                colors.purple.copy(alpha = 0.18f),
                                                RoundedCornerShape(7.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    } else if (controller.publicRoot == null && !controller.publicDriveBusy) {
                        item(key = "public_drive_empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp)
                                        .clip(CircleShape)
                                        .background(colors.purple.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Link, contentDescription = null,
                                        tint = colors.purpleBright, modifier = Modifier.size(29.dp))
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Dán liên kết để bắt đầu",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Tự nhận diện Google Drive, MediaFire và Fshare. " +
                                        "Nhấn + để thêm nguồn Drive khác.",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 5.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else if (showArtistView) {
                        if (controller.publicArtistLoading) {
                            item(key = "artist_loading") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 18.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.purpleBright
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        mwString(
                                            "library.drive_group_loading",
                                            "Đang phân nhóm thư mục ca sĩ..."
                                        ),
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        controller.publicArtistError?.let { error ->
                            item(key = "artist_error") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.red.copy(alpha = 0.1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(error, color = colors.red, fontSize = 12.sp)
                                        Text(
                                            mwString(
                                                "library.drive_group_fallback",
                                                "Bạn vẫn có thể chọn từng thư mục ở tab Thư mục Drive."
                                            ),
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (controller.publicRoot != null && !controller.publicArtistLoading &&
                            visibleArtistFolders.isEmpty() && controller.publicArtistError == null) {
                            item(key = "artist_empty") {
                                Text(
                                    if (searchQuery.isBlank()) {
                                        mwString(
                                            "library.drive_group_empty",
                                            "Không có bài hát để phân theo ca sĩ trong link này."
                                        )
                                    } else {
                                        "Không tìm thấy thư mục phù hợp"
                                    },
                                    modifier = Modifier.padding(18.dp),
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        items(visibleArtistFolders, key = { "artist_${it.key}" }) { folder ->
                            val selected = controller.isPublicArtistFolderSelected(folder)
                            val expanded = expandedArtistFolderKey == folder.key
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    colors.purple.copy(alpha = 0.16f)
                                } else {
                                    colors.cardRaised.copy(alpha = 0.62f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.purpleBright.copy(alpha = 0.72f) else colors.border
                                )
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedArtistFolderKey = if (expanded) null else folder.key
                                            }
                                            .padding(horizontal = 7.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selected,
                                            onCheckedChange = { controller.togglePublicArtistFolder(folder) },
                                            colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                        )
                                        Icon(
                                            Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = colors.purpleBright,
                                            modifier = Modifier.size(23.dp)
                                        )
                                        Spacer(Modifier.width(9.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                folder.name,
                                                color = colors.textPrimary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${folder.songs.size} bài · ${folder.linkName}",
                                                color = colors.textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            if (expanded) {
                                                Icons.Rounded.ArrowDropDown
                                            } else {
                                                Icons.Rounded.ChevronRight
                                            },
                                            contentDescription = null,
                                            tint = colors.textSecondary
                                        )
                                    }
                                    if (expanded) {
                                        folder.songs.take(40).forEach { song ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 58.dp, end = 12.dp, bottom = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    song.title,
                                                    color = colors.textSecondary,
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (folder.songs.size > 40) {
                                            Text(
                                                "+${folder.songs.size - 40} bài",
                                                color = colors.purpleBright,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(start = 59.dp, bottom = 9.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (controller.publicDriveBusy && visibleEntries.isEmpty()) {
                            item(key = "drive_loading") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.purpleBright
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        mwString("library.reading_drive", "Đang đọc Google Drive..."),
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        if (!controller.publicDriveBusy && visibleEntries.isEmpty()) {
                            item(key = "drive_entries_empty") {
                                Text(
                                    if (searchQuery.isBlank()) {
                                        "Thư mục này chưa có bài hát hoặc thư mục con"
                                    } else {
                                        "Không tìm thấy bài hát hoặc thư mục phù hợp"
                                    },
                                    modifier = Modifier.padding(18.dp),
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        items(visibleEntries, key = { "drive_${it.id}" }) { entry ->
                            val playable = controller.isPublicEntryPlayable(entry)
                            val archiveFile = controller.isPublicEntryArchive(entry)
                            val songSelected = controller.publicSelectedIds[entry.id] == true
                            val folderSelected = entry.isFolder &&
                                    controller.isPublicDriveFolderSelected(entry)
                            val folderBusy = entry.isFolder &&
                                    controller.isPublicDriveFolderSelectionBusy(entry)
                            val selected = songSelected || folderSelected

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) {
                                    colors.purple.copy(alpha = 0.16f)
                                } else {
                                    colors.cardRaised.copy(alpha = 0.62f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.purpleBright.copy(alpha = 0.72f) else colors.border
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (entry.isFolder) {
                                        if (folderBusy) {
                                            Box(
                                                modifier = Modifier.size(48.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(21.dp),
                                                    strokeWidth = 2.dp,
                                                    color = colors.purpleBright
                                                )
                                            }
                                        } else {
                                            Checkbox(
                                                checked = folderSelected,
                                                onCheckedChange = { controller.togglePublicDriveFolder(entry) },
                                                enabled = !controller.publicDriveBusy,
                                                colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                            )
                                        }
                                    } else if (archiveFile) {
                                        Box(
                                            modifier = Modifier.size(48.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                extensionOfPublicArchive(entry.name),
                                                color = colors.purpleBright,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(
                                                        colors.purple.copy(alpha = 0.18f),
                                                        RoundedCornerShape(7.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    } else {
                                        Checkbox(
                                            checked = songSelected,
                                            onCheckedChange = { controller.togglePublicEntry(entry) },
                                            enabled = playable && !controller.publicDriveBusy,
                                            colors = CheckboxDefaults.colors(checkedColor = colors.purple)
                                        )
                                    }

                                    Icon(
                                        when {
                                            entry.isFolder -> Icons.Rounded.Folder
                                            archiveFile -> Icons.Rounded.Folder
                                            else -> Icons.Rounded.MusicNote
                                        },
                                        contentDescription = null,
                                        tint = if (entry.isFolder || archiveFile) colors.purpleBright else colors.textSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                enabled = !controller.publicDriveBusy &&
                                                        (entry.isFolder || playable || archiveFile)
                                            ) {
                                                when {
                                                    entry.isFolder -> controller.openPublicFolder(entry)
                                                    archiveFile -> controller.openPublicArchive(entry)
                                                    else -> controller.togglePublicEntry(entry)
                                                }
                                            }
                                    ) {
                                        Text(
                                            entry.name,
                                            color = colors.textPrimary,
                                            fontWeight = if (entry.isFolder) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            when {
                                                folderBusy -> "Đang quét toàn bộ bài hát trong thư mục..."
                                                folderSelected -> "Đã chọn toàn bộ thư mục"
                                                entry.isFolder -> "Chạm tên hoặc mũi tên để mở"
                                                archiveFile -> "File nén ${extensionOfPublicArchive(entry.name)} · Chạm để xem nhạc bên trong"
                                                playable -> if (entry.mimeType.isBlank()) {
                                                    mwString("library.audio_file", "Tệp âm thanh")
                                                } else {
                                                    entry.mimeType
                                                }
                                                else -> mwString(
                                                    "library.owner_blocked",
                                                    "Không hỗ trợ hoặc chủ sở hữu đã chặn tải"
                                                )
                                            },
                                            color = if (!entry.isFolder && !playable && !archiveFile) {
                                                colors.textMuted
                                            } else if (folderSelected || folderBusy) {
                                                colors.purpleBright
                                            } else {
                                                colors.textSecondary
                                            },
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (entry.isFolder || archiveFile) {
                                        IconButton(
                                            onClick = {
                                                if (entry.isFolder) controller.openPublicFolder(entry)
                                                else controller.openPublicArchive(entry)
                                            },
                                            enabled = !controller.publicDriveBusy
                                        ) {
                                            Icon(
                                                Icons.Rounded.ChevronRight,
                                                contentDescription = if (entry.isFolder) "Mở thư mục" else "Mở file nén",
                                                tint = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // One-row sticky actions; selection status is already visible above.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.backgroundSoft,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        val canUseSelection = controller.publicPendingCount > 0 &&
                            !controller.publicDriveBusy &&
                            controller.publicFolderSelectionBusyCount == 0
                        Button(
                            onClick = { controller.addSelectedPublicSongs(playImmediately = false) },
                            enabled = canUseSelection,
                            modifier = Modifier.weight(1.3f).height(43.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "Thêm (${controller.publicPendingCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = { controller.addSelectedPublicSongs(playImmediately = true) },
                            enabled = canUseSelection,
                            modifier = Modifier.weight(0.84f).height(43.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null,
                                modifier = Modifier.size(18.dp), tint = colors.purpleBright)
                            Text("Phát", color = colors.purpleBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = controller::openPublicDriveBackup,
                            enabled = canUseSelection && !controller.publicDriveBackupBusy,
                            modifier = Modifier.weight(0.95f).height(43.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null,
                                modifier = Modifier.size(17.dp), tint = colors.purpleBright)
                            Spacer(Modifier.width(3.dp))
                            Text("Drive", color = colors.purpleBright, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
    pendingRemoveLinkSlot?.let { slot ->
        val selectedInSlot = controller.publicDriveSlotSelectedCount(slot)
        AlertDialog(
            onDismissRequest = { pendingRemoveLinkSlot = null },
            title = {
                Text(
                    "Xóa Link ${slot + 1}?",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (selectedInSlot > 0) {
                        "Link này đang có $selectedInSlot bài được chọn. Xóa link sẽ đóng workspace và bỏ các lựa chọn thuộc link này. Lịch sử link vẫn được giữ."
                    } else {
                        "Workspace Link ${slot + 1} sẽ bị đóng. Link đã mở vẫn còn trong Lịch sử để bạn sử dụng lại."
                    },
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        controller.removePublicDriveSlot(slot)
                        pendingRemoveLinkSlot = null
                    }
                ) {
                    Text("Xóa link", color = colors.red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveLinkSlot = null }) {
                    Text("Hủy", color = colors.textSecondary)
                }
            },
            containerColor = colors.backgroundSoft
        )
    }

    if (controller.showPublicDriveBackupDialog) {
        Dialog(
            onDismissRequest = {
                if (!controller.publicDriveBackupBusy) {
                    controller.cancelPendingPublicDriveBackupAuthorization()
                    controller.showPublicDriveBackupDialog = false
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = colors.backgroundSoft,
                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = colors.purpleBright,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Lưu vào Google Drive cá nhân",
                            modifier = Modifier.weight(1f),
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                controller.cancelPendingPublicDriveBackupAuthorization()
                                controller.showPublicDriveBackupDialog = false
                            },
                            enabled = !controller.publicDriveBackupBusy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Đóng", tint = colors.textSecondary)
                        }
                    }
                    Text(
                        "Sao chép ${controller.publicPendingCount} bài đã chọn từ các link đang sử dụng, " +
                            "giữ nguyên thư mục con. File 7Z/RAR được lưu nguyên bản.",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        "Tài khoản đích: ${controller.linkDriveUploadAccountEmail ?: "Sẽ dùng tài khoản Google đang đăng nhập"}",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text("Drive của tôi / MeloWave /", color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = driveCopyDestination,
                        onValueChange = { driveCopyDestination = it.take(90) },
                        enabled = !controller.publicDriveBackupBusy,
                        label = { Text("Tên thư mục lưu nhạc") },
                        placeholder = { Text("Nhạc đã lưu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Lưu từ liên kết sử dụng quyền drive.file, xin riêng khi nhấn Lưu. " +
                            "Chỉ sao chép file được chủ sở hữu cho phép tải/copy.",
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                    if (controller.publicDriveBackupBusy) {
                        val total = controller.publicDriveBackupTotal
                        val done = controller.publicDriveBackupDone
                        if (total > 0) {
                            Text(
                                "$done/$total file · lỗi ${controller.publicDriveBackupFailed}",
                                color = colors.purpleBright,
                                fontSize = 12.sp
                            )
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = (done.toFloat() / total).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.purpleBright
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.dp,
                                color = colors.purpleBright
                            )
                        }
                    }
                    if (controller.publicDriveBackupStage.isNotBlank()) {
                        Text(
                            controller.publicDriveBackupStage,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    controller.publicDriveBackupResult?.let { result ->
                        Text(result, color = colors.textPrimary, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (controller.publicDriveBackupBusy) controller.cancelPublicDriveBackup()
                                else {
                                    controller.cancelPendingPublicDriveBackupAuthorization()
                                    controller.showPublicDriveBackupDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (controller.publicDriveBackupBusy) "Dừng" else "Đóng")
                        }
                        Button(
                            onClick = { controller.startPublicDriveBackup(driveCopyDestination) },
                            enabled = !controller.publicDriveBackupBusy &&
                                driveCopyDestination.trim().isNotEmpty() &&
                                controller.publicPendingCount > 0,
                            modifier = Modifier.weight(1.35f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Lưu vào Drive", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    controller.publicArchivePasswordName?.let { archiveName ->
        Dialog(
            onDismissRequest = {
                if (!controller.publicDriveBusy) controller.cancelPublicArchivePassword()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.backgroundSoft,
                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.purple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.purpleBright
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "File nén được bảo vệ",
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                archiveName,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        "Nhập mật khẩu để xem và phát các bài hát bên trong. Mật khẩu chỉ được giữ trong phiên sử dụng hiện tại.",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = archivePassword,
                        onValueChange = { archivePassword = it },
                        enabled = !controller.publicDriveBusy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (archivePassword.isNotBlank()) {
                                    controller.submitPublicArchivePassword(archivePassword)
                                }
                            }
                        )
                    )
                    controller.publicArchivePasswordError?.let { error ->
                        Text(
                            error,
                            color = colors.red,
                            fontSize = 11.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = controller::cancelPublicArchivePassword,
                            enabled = !controller.publicDriveBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text("Hủy")
                        }
                        Button(
                            onClick = { controller.submitPublicArchivePassword(archivePassword) },
                            enabled = archivePassword.isNotBlank() && !controller.publicDriveBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            if (controller.publicDriveBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Mở", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun onlineLinkLooksLikeGoogleDrive(value: String): Boolean {
    val input = value.trim()
    if (input.isBlank()) return false
    // Public Drive also accepts a folder ID without a URL.
    if (Regex("[A-Za-z0-9_-]{12,}").matches(input)) return true
    val normalized = if (input.contains("://")) input else "https://$input"
    val host = runCatching {
        android.net.Uri.parse(normalized).host?.lowercase(Locale.ROOT)
    }.getOrNull()
    return host == "drive.google.com" || host == "www.drive.google.com"
}

private fun onlineLinkClipboard(context: Context): String {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
        as? android.content.ClipboardManager
    val clip = clipboard?.primaryClip
    return if (clip != null && clip.itemCount > 0) {
        clip.getItemAt(0).coerceToText(context).toString().trim()
    } else ""
}

@Composable
private fun OnlineLinkInputRow(
    value: String,
    placeholder: String,
    enabled: Boolean,
    busy: Boolean,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onOpen: () -> Unit
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(46.dp),
            shape = RoundedCornerShape(13.dp),
            color = colors.cardRaised,
            border = BorderStroke(1.dp, colors.border)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(colors.purpleBright),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { if (enabled) onOpen() }),
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPaste,
                            enabled = enabled,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Dán link từ bộ nhớ",
                                modifier = Modifier.size(17.dp),
                                tint = colors.purpleBright
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isBlank()) {
                                Text(
                                    placeholder,
                                    color = colors.textMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                        if (value.isNotBlank()) {
                            IconButton(
                                onClick = onClear,
                                enabled = enabled,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Xóa link",
                                    modifier = Modifier.size(17.dp),
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            )
        }
        Button(
            onClick = onOpen,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.width(77.dp).height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    "Mở",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun OnlineLinkHistoryMenu(
    controller: MusicController,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectDrive: (SavedPublicDriveLink) -> Unit,
    onSelectMediaFire: (SavedMediaFireLink) -> Unit
) {
    val colors = LocalMusicColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 275.dp, max = 380.dp).heightIn(max = 420.dp)
    ) {
        if (controller.savedPublicDriveLinks.isEmpty() && controller.savedMediaFireLinks.isEmpty()) {
            DropdownMenuItem(
                text = { Text("Chưa có liên kết đã lưu") },
                enabled = false,
                onClick = {}
            )
        }
        if (controller.savedPublicDriveLinks.isNotEmpty()) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Google Drive (${controller.savedPublicDriveLinks.size})",
                        color = colors.purpleBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                enabled = false,
                onClick = {}
            )
            controller.savedPublicDriveLinks.forEach { saved ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(saved.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                saved.url,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textSecondary
                            )
                        }
                    },
                    onClick = { onDismiss(); onSelectDrive(saved) },
                    trailingIcon = {
                        IconButton(
                            onClick = { controller.forgetSavedPublicDriveLink(saved) },
                            modifier = Modifier.size(32.dp),
                            enabled = !controller.publicDriveBusy
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Xóa link Drive khỏi lịch sử",
                                modifier = Modifier.size(18.dp),
                                tint = colors.textMuted
                            )
                        }
                    }
                )
            }
        }
        if (controller.savedMediaFireLinks.isNotEmpty()) {
            DropdownMenuItem(
                text = {
                    Text(
                        "MediaFire / Fshare (${controller.savedMediaFireLinks.size})",
                        color = colors.purpleBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                },
                enabled = false,
                onClick = {}
            )
            controller.savedMediaFireLinks.forEach { saved ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(saved.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                saved.url,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colors.textSecondary
                            )
                        }
                    },
                    onClick = { onDismiss(); onSelectMediaFire(saved) },
                    trailingIcon = {
                        IconButton(
                            onClick = { controller.forgetSavedMediaFireLink(saved) },
                            modifier = Modifier.size(32.dp),
                            enabled = !controller.mediaFireBusy
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Xóa link MediaFire khỏi lịch sử",
                                modifier = Modifier.size(18.dp),
                                tint = colors.textMuted
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MediaFireLinkDialog(
    controller: MusicController,
    onDismiss: () -> Unit,
    onShowGoogleDrive: () -> Unit
) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    if (controller.mediaFireWebViewVisible) {
        MediaFireWebBrowserDialog(controller)
        return
    }
    var showHistory by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var archivePassword by remember { mutableStateOf("") }
    var mediaFireDriveDestination by remember { mutableStateOf("MediaFire") }
    var mediaFireDriveSaveMode by remember { mutableStateOf(MediaFireDriveSaveMode.ORIGINAL_ARCHIVE) }
    val archive = controller.mediaFireArchive
    val original = controller.hostedOriginalFile
    val saveOriginalLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { target ->
        if (target != null) controller.saveHostedOriginalToUri(target)
    }
    val normalizedQuery = searchQuery.trim().lowercase(Locale.ROOT)
    val visibleEntries = if (normalizedQuery.isBlank()) {
        archive?.audioEntries.orEmpty()
    } else {
        archive?.audioEntries.orEmpty().filter { entry ->
            entry.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                entry.folderPath.lowercase(Locale.ROOT).contains(normalizedQuery)
        }
    }
    val allSelected = archive?.audioEntries?.let { entries ->
        entries.isNotEmpty() && entries.all(controller::isMediaFireArchiveEntrySelected)
    } ?: false

    LaunchedEffect(controller.mediaFireArchivePasswordName) {
        archivePassword = ""
    }
    // Raw files cannot be extracted; do not reuse a prior 'extracted audio' choice.
    LaunchedEffect(original?.source?.id) {
        if (original != null) mediaFireDriveSaveMode = MediaFireDriveSaveMode.ORIGINAL_ARCHIVE
    }
    LaunchedEffect(controller.hostedSourceName) {
        mediaFireDriveDestination = controller.hostedSourceName
    }

    Dialog(
        onDismissRequest = { if (!controller.mediaFireBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.background, colors.backgroundSoft, colors.background)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !controller.mediaFireBusy) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = mwString("common.close", "Đóng"),
                            tint = colors.textPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.purple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Download,
                            contentDescription = null,
                            tint = colors.purpleBright,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Liên kết trực tuyến",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (archive == null) {
                                "${controller.hostedSourceName} · Dán link tải file"
                            } else {
                                "${controller.hostedSourceName} · ${archive.audioEntries.size} bài · ${controller.mediaFireSelectedCount} chọn"
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { showHistory = true },
                            enabled = !controller.publicDriveBusy && !controller.mediaFireBusy
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = "Lịch sử liên kết",
                                tint = colors.purpleBright
                            )
                        }
                        OnlineLinkHistoryMenu(
                            controller = controller,
                            expanded = showHistory,
                            onDismiss = { showHistory = false },
                            onSelectDrive = { saved ->
                                controller.selectSavedPublicDriveLink(saved)
                                onShowGoogleDrive()
                            },
                            onSelectMediaFire = { saved -> controller.selectSavedMediaFireLink(saved) }
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    OnlineLinkInputRow(
                        value = controller.mediaFireLinkText,
                        placeholder = "Dán link Drive, MediaFire, Fshare",
                        enabled = !controller.mediaFireBusy,
                        busy = controller.mediaFireBusy,
                        onValueChange = { value ->
                            if (onlineLinkLooksLikeGoogleDrive(value)) {
                                controller.setPublicLink(value)
                                onShowGoogleDrive()
                            } else {
                                controller.setMediaFireLink(value)
                            }
                        },
                        onPaste = {
                            val text = onlineLinkClipboard(context)
                            if (text.isNotBlank()) {
                                if (onlineLinkLooksLikeGoogleDrive(text)) {
                                    controller.setPublicLink(text)
                                    onShowGoogleDrive()
                                } else {
                                    controller.setMediaFireLink(text)
                                }
                            }
                        },
                        onClear = controller::clearMediaFireLink,
                        onOpen = controller::openMediaFireLink
                    )

                    controller.mediaFireError?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.red.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.red.copy(alpha = 0.35f))
                        ) {
                            Text(
                                error,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = colors.red,
                                fontSize = 11.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // The official MediaFire website opens automatically if direct downloading
                    // is unavailable; do not ask the user to press an intermediate button.

                    controller.mediaFireStatus?.let { status ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardRaised.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.purpleBright
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        status,
                                        modifier = Modifier.weight(1f),
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (controller.mediaFireBusy) {
                                        TextButton(onClick = controller::cancelMediaFireDownload) {
                                            Text("Dừng", color = colors.red, fontSize = 11.sp)
                                        }
                                    }
                                }
                                if (controller.mediaFireProgressPercent > 0) {
                                    androidx.compose.material3.LinearProgressIndicator(
                                        progress = controller.mediaFireProgressPercent / 100f,
                                        modifier = Modifier.fillMaxWidth(),
                                        color = colors.purpleBright
                                    )
                                    Text(
                                        if (controller.mediaFireTotalBytes > 0L &&
                                            controller.mediaFireTotalBytes > controller.mediaFireDownloadedBytes) {
                                            "${publicArchiveSizeLabel(controller.mediaFireDownloadedBytes)} / " +
                                                publicArchiveSizeLabel(controller.mediaFireTotalBytes)
                                        } else {
                                            "${controller.mediaFireProgressPercent}%"
                                        },
                                        color = colors.textMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (archive == null && original == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = null,
                                tint = colors.purpleBright,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Tải file từ ${controller.hostedSourceName}",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                "RAR/7Z: đọc bài hát, phát và giải nén. " +
                                    "Fshare hỗ trợ lưu file gốc như ISO/ZIP vào máy hoặc Drive; " +
                                    "có thể cần xác nhận tải trong trang chính thức.",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            // Website fallback is automatic when the source requires it.
                        }
                    }
                } else if (original != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardRaised,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = colors.purpleBright,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        original.source.name,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "File gốc · ${publicArchiveSizeLabel(original.localFile.length())}",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Text(
                            "File đã tải vào cache ứng dụng. Định dạng này không phải " +
                                "RAR/7Z nên MeloWave không giải nén hoặc phát như một album. " +
                                "Bạn có thể lưu file gốc về máy hoặc lên Google Drive.",
                            color = colors.textSecondary,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            OutlinedButton(
                                onClick = { saveOriginalLauncher.launch(original.source.name) },
                                enabled = !controller.mediaFireBusy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Về máy", maxLines = 1)
                            }
                            Button(
                                onClick = controller::openMediaFireDriveBackup,
                                enabled = !controller.mediaFireBusy && !controller.mediaFireDriveBackupBusy,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                            ) {
                                Icon(Icons.Rounded.Cloud, contentDescription = null,
                                    modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Lên Drive", maxLines = 1)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                } else if (archive != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardRaised.copy(alpha = 0.82f),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.purple.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = colors.purpleBright
                                    )
                                }
                                Spacer(Modifier.width(9.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        archive.source.name,
                                        color = colors.textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${archive.format.displayName} · ${archive.audioEntries.size} bài · " +
                                            publicArchiveSizeLabel(archive.source.size) +
                                            if (archive.passwordProtected) " · Có mật khẩu" else "",
                                        color = colors.textSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                TextButton(
                                    onClick = controller::toggleAllMediaFireArchiveEntries,
                                    enabled = !controller.mediaFireBusy
                                ) {
                                    Text(
                                        if (allSelected) "Bỏ chọn" else "Chọn tất cả",
                                        color = colors.purpleBright,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = controller::closeMediaFireArchive,
                                    enabled = !controller.mediaFireBusy
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Đóng file nén",
                                        tint = colors.textSecondary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().height(43.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardRaised,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                enabled = !controller.mediaFireBusy,
                                singleLine = true,
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 12.sp),
                                cursorBrush = SolidColor(colors.purpleBright),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                                decorationBox = { inner ->
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Search, contentDescription = null,
                                            tint = colors.textSecondary, modifier = Modifier.size(19.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            if (searchQuery.isBlank()) {
                                                Text("Tìm bài hát trong file nén",
                                                    color = colors.textMuted, fontSize = 12.sp)
                                            }
                                            inner()
                                        }
                                        if (searchQuery.isNotBlank()) {
                                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                                                Icon(Icons.Rounded.Close, contentDescription = "Xóa tìm kiếm",
                                                    tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        if (visibleEntries.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (searchQuery.isBlank()) "File nén không có bài hát được hỗ trợ"
                                    else "Không tìm thấy bài hát phù hợp",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 4.dp)
                            ) {
                                items(visibleEntries, key = { entry -> "mediafire_${entry.key}" }) { entry ->
                                    val selected = controller.isMediaFireArchiveEntrySelected(entry)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !controller.mediaFireBusy) {
                                                controller.toggleMediaFireArchiveEntry(entry)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) {
                                            colors.purple.copy(alpha = 0.16f)
                                        } else {
                                            colors.cardRaised.copy(alpha = 0.7f)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) colors.purpleBright.copy(alpha = 0.65f)
                                            else colors.border
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selected,
                                                onCheckedChange = {
                                                    controller.toggleMediaFireArchiveEntry(entry)
                                                },
                                                enabled = !controller.mediaFireBusy,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = colors.purple,
                                                    uncheckedColor = colors.textMuted
                                                )
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    entry.name.substringBeforeLast('.', entry.name),
                                                    color = colors.textPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    buildString {
                                                        if (entry.folderPath.isNotBlank()) {
                                                            append(entry.folderPath)
                                                            append(" · ")
                                                        }
                                                        append(publicArchiveSizeLabel(entry.size))
                                                    },
                                                    color = colors.textSecondary,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                entry.name.substringAfterLast('.', "AUDIO").uppercase(Locale.ROOT),
                                                color = colors.purpleBright,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val canAct = controller.mediaFireSelectedCount > 0 && !controller.mediaFireBusy
                            OutlinedButton(
                                onClick = controller::openMediaFireDriveBackup,
                                enabled = !controller.mediaFireBusy && !controller.mediaFireDriveBackupBusy,
                                modifier = Modifier.weight(0.85f).height(43.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 3.dp)
                            ) {
                                Icon(Icons.Rounded.Cloud, contentDescription = null,
                                    tint = colors.purpleBright, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Drive", color = colors.purpleBright, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = controller::extractSelectedMediaFireSongs,
                                enabled = canAct,
                                modifier = Modifier.weight(0.85f).height(43.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 3.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null,
                                    tint = colors.purpleBright, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Offline", color = colors.purpleBright, fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Button(
                                onClick = { controller.addSelectedMediaFireSongs(playImmediately = false) },
                                enabled = canAct,
                                modifier = Modifier.weight(0.95f).height(43.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                                contentPadding = PaddingValues(horizontal = 3.dp)
                            ) {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Thêm (${controller.mediaFireSelectedCount})", color = Color.White,
                                    fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { controller.addSelectedMediaFireSongs(playImmediately = true) },
                                enabled = canAct,
                                modifier = Modifier.weight(0.75f).height(43.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f)),
                                contentPadding = PaddingValues(horizontal = 1.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Phát các bài đang chọn",
                                    tint = colors.purpleBright,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (controller.showMediaFireDriveBackupDialog) {
        val driveArchive = controller.mediaFireArchive
        val rawOriginal = controller.hostedOriginalFile
        Dialog(
            onDismissRequest = {
                if (!controller.mediaFireDriveBackupBusy) {
                    controller.cancelPendingMediaFireDriveBackupAuthorization()
                    controller.showMediaFireDriveBackupDialog = false
                }
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.backgroundSoft,
                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.heightIn(max = 660.dp)
                        .verticalScroll(rememberScrollState()).padding(17.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = colors.purpleBright,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Lưu ${controller.hostedSourceName} vào Drive cá nhân",
                            modifier = Modifier.weight(1f),
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                controller.cancelPendingMediaFireDriveBackupAuthorization()
                                controller.showMediaFireDriveBackupDialog = false
                            },
                            enabled = !controller.mediaFireDriveBackupBusy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Đóng",
                                tint = colors.textSecondary
                            )
                        }
                    }

                    driveArchive?.let { opened ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardRaised.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Column(Modifier.padding(11.dp)) {
                                Text(
                                    opened.source.name,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${opened.format.displayName} · ${publicArchiveSizeLabel(opened.source.size)} · " +
                                        "${opened.audioEntries.size} bài",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    rawOriginal?.let { raw ->
                        Text(
                            "${raw.source.name} · ${publicArchiveSizeLabel(raw.localFile.length())}",
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        "Chọn cách lưu lên Google Drive:",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    val selectedTrackCount = controller.mediaFireSelectedCount
                    val uploadTrackCount = if (selectedTrackCount > 0) {
                        selectedTrackCount
                    } else {
                        driveArchive?.audioEntries?.size ?: 0
                    }
                    val backupChoices = listOf(
                        Triple(
                            MediaFireDriveSaveMode.ORIGINAL_ARCHIVE,
                            if (rawOriginal == null) "Lưu file gốc RAR/7Z" else "Lưu file gốc",
                            "Giữ nguyên file đã tải về, không giải nén."
                        ),
                        Triple(
                            MediaFireDriveSaveMode.EXTRACTED_AUDIO,
                            "Lưu bài hát đã giải nén",
                            if (selectedTrackCount > 0) {
                                "Tải $uploadTrackCount bài đã chọn, giữ định dạng âm thanh gốc."
                            } else {
                                "Chưa chọn bài: sẽ tải tất cả $uploadTrackCount bài nhạc."
                            }
                        )
                    )
                    backupChoices.filter { (mode, _, _) ->
                        rawOriginal == null || mode == MediaFireDriveSaveMode.ORIGINAL_ARCHIVE
                    }.forEach { (mode, title, subtitle) ->
                        val chosen = mediaFireDriveSaveMode == mode
                        Surface(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !controller.mediaFireDriveBackupBusy) {
                                    mediaFireDriveSaveMode = mode
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (chosen) colors.cardRaised else colors.backgroundSoft,
                            border = BorderStroke(
                                if (chosen) 1.5.dp else 1.dp,
                                if (chosen) colors.purpleBright else colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (chosen) Icons.Rounded.Check else Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = if (chosen) colors.purpleBright else colors.textSecondary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        title,
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        subtitle,
                                        color = colors.textSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        if (mediaFireDriveSaveMode == MediaFireDriveSaveMode.ORIGINAL_ARCHIVE) {
                            "MeloWave lưu nguyên file từ bộ nhớ tạm."
                        } else {
                            "MeloWave chỉ giải nén và lưu các bài nhạc được hỗ trợ, " +
                                "từng file một; không tải file RAR/7Z lên Drive."
                        } + " Chức năng này dùng quyền drive.file riêng; " +
                            "Drive cá nhân vẫn sử dụng drive.readonly.",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    val uploadAccountEmail = controller.linkDriveUploadAccountEmail
                        ?: com.google.android.gms.auth.api.signin.GoogleSignIn
                            .getLastSignedInAccount(context)?.email
                        ?: "Đăng nhập Google khi cấp quyền"
                    Text(
                        "Tài khoản đích: $uploadAccountEmail",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        if (mediaFireDriveSaveMode == MediaFireDriveSaveMode.EXTRACTED_AUDIO) {
                            "Drive của tôi / MeloWave / [Thư mục] / [Tên album] /"
                        } else {
                            "Drive của tôi / MeloWave / [Thư mục] /"
                        },
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = mediaFireDriveDestination,
                        onValueChange = { mediaFireDriveDestination = it.take(90) },
                        enabled = !controller.mediaFireDriveBackupBusy,
                        label = { Text("Tên thư mục lưu") },
                        placeholder = { Text(controller.hostedSourceName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (controller.mediaFireDriveBackupBusy) {
                        val uploaded = controller.mediaFireDriveBackupUploadedBytes
                        val total = controller.mediaFireDriveBackupTotalBytes
                        if (total > 0L) {
                            val progress = (uploaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.purpleBright
                            )
                            Text(
                                "${publicArchiveSizeLabel(uploaded)} / ${publicArchiveSizeLabel(total)}",
                                color = colors.textMuted,
                                fontSize = 10.sp
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(25.dp),
                                strokeWidth = 2.dp,
                                color = colors.purpleBright
                            )
                        }
                    }

                    if (controller.mediaFireDriveBackupStage.isNotBlank()) {
                        Text(
                            controller.mediaFireDriveBackupStage,
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    controller.mediaFireDriveBackupResult?.let { result ->
                        Text(
                            result,
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (controller.mediaFireDriveBackupBusy) {
                                    controller.cancelMediaFireDriveBackup()
                                } else {
                                    controller.cancelPendingMediaFireDriveBackupAuthorization()
                                    controller.showMediaFireDriveBackupDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (controller.mediaFireDriveBackupBusy) "Dừng" else "Đóng")
                        }
                        Button(
                            onClick = {
                                controller.startMediaFireDriveBackup(
                                    mediaFireDriveDestination,
                                    mediaFireDriveSaveMode
                                )
                            },
                            enabled = !controller.mediaFireDriveBackupBusy &&
                                (driveArchive != null || rawOriginal != null) &&
                                mediaFireDriveDestination.trim().isNotEmpty() &&
                                (mediaFireDriveSaveMode != MediaFireDriveSaveMode.EXTRACTED_AUDIO ||
                                    uploadTrackCount > 0),
                            modifier = Modifier.weight(1.45f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            Icon(
                                Icons.Rounded.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Lưu vào Drive", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    controller.mediaFireArchivePasswordName?.let { archiveName ->
        Dialog(
            onDismissRequest = {
                if (!controller.mediaFireBusy) controller.cancelMediaFireArchivePassword()
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = colors.backgroundSoft,
                border = BorderStroke(1.dp, colors.purpleBright.copy(alpha = 0.65f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(colors.purple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.purpleBright
                            )
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "File ${controller.hostedSourceName} được bảo vệ",
                                color = colors.textPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                archiveName,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        "Mật khẩu chỉ được giữ trong phiên sử dụng hiện tại và không được ghi vào lịch sử link.",
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = archivePassword,
                        onValueChange = { archivePassword = it },
                        enabled = !controller.mediaFireBusy,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (archivePassword.isNotBlank()) {
                                    controller.submitMediaFireArchivePassword(archivePassword)
                                }
                            }
                        )
                    )
                    controller.mediaFireArchivePasswordError?.let { error ->
                        Text(error, color = colors.red, fontSize = 11.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        OutlinedButton(
                            onClick = controller::cancelMediaFireArchivePassword,
                            enabled = !controller.mediaFireBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text("Hủy")
                        }
                        Button(
                            onClick = { controller.submitMediaFireArchivePassword(archivePassword) },
                            enabled = archivePassword.isNotBlank() && !controller.mediaFireBusy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
                        ) {
                            if (controller.mediaFireBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(17.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Mở", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("SetJavaScriptEnabled")
@Composable
private fun MediaFireWebBrowserDialog(controller: MusicController) {
    val colors = LocalMusicColors.current
    val context = LocalContext.current
    val initialUrl = controller.mediaFireWebViewUrl.ifBlank { controller.mediaFireLinkText }
    var pageTitle by remember(initialUrl) {
        mutableStateOf(controller.hostedSourceName)
    }
    var browserError by remember(initialUrl) { mutableStateOf<String?>(null) }
    var loading by remember(initialUrl) { mutableStateOf(true) }
    var progress by remember(initialUrl) { mutableStateOf(0) }
    var currentUrl by remember(initialUrl) { mutableStateOf(initialUrl) }

    fun mergedCookies(downloadUrl: String, pageUrl: String): String? {
        val manager = CookieManager.getInstance()
        val values = buildList {
            manager.getCookie(pageUrl)?.takeIf { it.isNotBlank() }?.let(::add)
            manager.getCookie(downloadUrl)?.takeIf { it.isNotBlank() }?.let(::add)
        }
        return values
            .flatMap { header -> header.split(';') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("; ")
            .takeIf { it.isNotBlank() }
    }

    fun captureDownload(
        view: WebView,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        val referer = view.url?.takeIf { it.isNotBlank() } ?: currentUrl.ifBlank { initialUrl }
        CookieManager.getInstance().flush()
        browserError = null
        controller.acceptMediaFireWebDownload(
            downloadUrl = url,
            userAgent = userAgent?.takeIf { it.isNotBlank() } ?: view.settings.userAgentString,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            contentLength = contentLength,
            cookies = mergedCookies(url, referer),
            referer = referer,
            pageTitle = pageTitle
        )
    }

    val browser = remember(initialUrl) {
        WebView(context).apply mediaFireWebView@ {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.loadsImagesAutomatically = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAcceptThirdPartyCookies(this@mediaFireWebView, true)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress.coerceIn(0, 100)
                    loading = newProgress < 100
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    pageTitle = title?.trim()?.takeIf { it.isNotBlank() }
                        ?: controller.hostedSourceName
                }
            }

            webViewClient = object : WebViewClient() {
                private fun handleNavigation(view: WebView?, target: String?): Boolean {
                    val activeView = view ?: return true
                    val url = target?.trim().orEmpty()
                    if (url.isBlank()) return true
                    if (controller.isLikelyMediaFireDownloadUrl(url)) {
                        captureDownload(
                            view = activeView,
                            url = url,
                            userAgent = activeView.settings.userAgentString,
                            contentDisposition = null,
                            mimeType = null,
                            contentLength = -1L
                        )
                        return true
                    }
                    if (controller.isAllowedHostedWebAddress(url)) return false
                    browserError = "Đã chặn liên kết không thuộc máy chủ tải tin cậy."
                    return true
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean = handleNavigation(view, request?.url?.toString())

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
                    handleNavigation(view, url)

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    loading = true
                    currentUrl = url?.takeIf { it.isNotBlank() } ?: currentUrl
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    loading = false
                    progress = 100
                    currentUrl = url?.takeIf { it.isNotBlank() } ?: currentUrl
                    // The actual MediaFire Download button is shown in the browser below.
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) {
                        browserError = error?.description?.toString()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Không mở được trang ${controller.hostedSourceName}"
                    }
                }
            }

            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                if (!url.isNullOrBlank()) {
                    captureDownload(
                        view = this,
                        url = url,
                        userAgent = userAgent,
                        contentDisposition = contentDisposition,
                        mimeType = mimeType,
                        contentLength = contentLength
                    )
                }
            }

            loadUrl(
                initialUrl,
                mapOf("Accept-Language" to "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
            )
        }
    }

    DisposableEffect(browser) {
        onDispose {
            runCatching { browser.stopLoading() }
            runCatching { browser.setDownloadListener(null) }
            runCatching { browser.webChromeClient = null }
            runCatching { browser.webViewClient = WebViewClient() }
            runCatching { browser.loadUrl("about:blank") }
            runCatching { (browser.parent as? ViewGroup)?.removeView(browser) }
            runCatching { browser.destroy() }
        }
    }

    BackHandler {
        // The Android back button returns to MeloWave; it never leaves a hidden browser behind.
        controller.closeMediaFireWebView()
    }

    Dialog(
        onDismissRequest = controller::closeMediaFireWebView,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = controller::closeMediaFireWebView) {
                        Icon(
                            Icons.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = colors.textPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${controller.hostedSourceName} - Tải file",
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            pageTitle,
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { browser.reload() }) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = "Tải lại trang",
                            tint = colors.purpleBright
                        )
                    }
                }

                if (loading) {
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = progress.coerceIn(0, 100) / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.purpleBright
                    )
                }

                if (controller.mediaFireBusy) {
                    androidx.compose.material3.LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.purpleBright
                    )
                }

                (controller.mediaFireWebViewHint ?: browserError)?.let { message ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = colors.red.copy(alpha = 0.11f)
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                AndroidView(
                    factory = {
                        (browser.parent as? ViewGroup)?.removeView(browser)
                        browser
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // No duplicate "Tải file" button: user taps MediaFire's own Download button.
                // DownloadListener hands the URL, cookie and user-agent to MusicController;
                // the controller closes this dialog when a valid RAR/7Z download is captured.
            }
        }
    }
}

private fun extensionOfPublicArchive(name: String): String =
    name.substringAfterLast('.', "ARCHIVE").uppercase(Locale.ROOT)

private fun publicArchiveSizeLabel(bytes: Long): String {
    if (bytes <= 0L) return "Chưa rõ dung lượng"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit++
    }
    return if (unit == 0) {
        "${value.toLong()} ${units[unit]}"
    } else {
        String.format(Locale.getDefault(), "%.1f %s", value, units[unit])
    }
}

private enum class SongToolPage { MENU, EDIT, DELETE, CUT, MERGE, RINGTONE }
private enum class SongConsentAction { NONE, EDIT, DELETE }

/** Used in both Library and NowPlaying so the same actions are restored everywhere. */
@Composable
internal fun SongToolsHost(
    controller: MusicController,
    song: SongItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val jobScope = rememberCoroutineScope()
    var page by remember(song.id) { mutableStateOf(SongToolPage.MENU) }
    var busy by remember(song.id) { mutableStateOf(false) }
    var actionError by remember(song.id) { mutableStateOf<String?>(null) }
    var pendingConsent by remember(song.id) { mutableStateOf(SongConsentAction.NONE) }
    var editTitle by remember(song.id) { mutableStateOf(song.title) }
    var editArtist by remember(song.id) { mutableStateOf(song.artist) }

    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingConsent
        pendingConsent = SongConsentAction.NONE
        if (result.resultCode != Activity.RESULT_OK) {
            actionError = "Bạn đã hủy cấp quyền trên Android; tệp nhạc chưa bị thay đổi."
        } else {
            busy = true
            jobScope.launch {
                val outcome = when (action) {
                    SongConsentAction.EDIT -> controller.updateLocalSongInfo(song, editTitle, editArtist, consentGranted = true)
                    SongConsentAction.DELETE -> controller.deleteLocalSongFromDevice(song, consentGranted = true)
                    SongConsentAction.NONE -> LocalSongMutationResult.Failed("Không có yêu cầu nào đang chờ xác nhận.")
                }
                busy = false
                when (outcome) {
                    LocalSongMutationResult.Completed,
                    LocalSongMutationResult.LibraryOnly -> onDismiss()
                    is LocalSongMutationResult.Failed -> actionError = outcome.message
                    is LocalSongMutationResult.NeedsConsent -> actionError = "Android vẫn chưa cấp quyền. Hãy thử lại thao tác."
                }
            }
        }
    }

    fun runAction(action: SongConsentAction) {
        if (busy) return
        actionError = null
        busy = true
        jobScope.launch {
            val outcome = when (action) {
                SongConsentAction.EDIT -> controller.updateLocalSongInfo(song, editTitle, editArtist)
                SongConsentAction.DELETE -> controller.deleteLocalSongFromDevice(song)
                SongConsentAction.NONE -> LocalSongMutationResult.Failed("Yêu cầu không hợp lệ")
            }
            busy = false
            when (outcome) {
                LocalSongMutationResult.Completed,
                LocalSongMutationResult.LibraryOnly -> onDismiss()
                is LocalSongMutationResult.Failed -> actionError = outcome.message
                is LocalSongMutationResult.NeedsConsent -> {
                    pendingConsent = action
                    runCatching {
                        consentLauncher.launch(IntentSenderRequest.Builder(outcome.intentSender).build())
                    }.onFailure {
                        pendingConsent = SongConsentAction.NONE
                        actionError = "Không thể mở xác nhận Android: ${it.localizedMessage ?: "Lỗi không xác định"}"
                    }
                }
            }
        }
    }

    val legacyWritePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { approved ->
        if (approved) runAction(pendingConsent)
        else actionError = "Cần cấp quyền bộ nhớ để sửa hoặc xóa tệp nhạc trên Android cũ."
        pendingConsent = SongConsentAction.NONE
    }

    fun doAction(action: SongConsentAction) {
        // Android 9 and earlier require WRITE_EXTERNAL_STORAGE for external audio files.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingConsent = action
            legacyWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else runAction(action)
    }

    when (page) {
        SongToolPage.MENU -> SongToolsMenuDialog(
            song = song,
            busy = controller.audioToolBusy || busy,
            onEdit = { page = SongToolPage.EDIT; actionError = null },
            onDelete = { page = SongToolPage.DELETE; actionError = null },
            onCut = { page = SongToolPage.CUT },
            onMerge = { page = SongToolPage.MERGE },
            onRingtone = { page = SongToolPage.RINGTONE },
            onDismiss = onDismiss
        )
        SongToolPage.EDIT -> EditSongInfoDialog(
            song = song,
            title = editTitle,
            artist = editArtist,
            onTitleChange = { editTitle = it },
            onArtistChange = { editArtist = it },
            busy = busy,
            error = actionError,
            onSave = { doAction(SongConsentAction.EDIT) },
            onBack = { if (!busy) page = SongToolPage.MENU },
            onDismiss = onDismiss
        )
        SongToolPage.DELETE -> DeleteSongConfirmDialog(
            song = song,
            busy = busy,
            error = actionError,
            onConfirm = { doAction(SongConsentAction.DELETE) },
            onBack = { if (!busy) page = SongToolPage.MENU },
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
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            Column(modifier = Modifier.heightIn(max = 630.dp).verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mwString("library.audio_tools", "Công cụ âm thanh"), color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(song.title, color = colors.purpleBright, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onDismiss, enabled = !busy) {
                        Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
                    }
                }
                AudioToolRow(Icons.Rounded.Edit, mwString("library.edit_song_info", "Sửa thông tin bài hát"),
                    mwString("library.edit_song_info_desc", "Đổi tên bài hát và ca sĩ / nghệ sĩ"), onEdit)
                AudioToolRow(Icons.Rounded.ContentCut, mwString("library.cut", "Cắt nhạc"), mwString("library.cut_desc", "Chọn đoạn và lưu thành tệp M4A mới"), onCut)
                AudioToolRow(Icons.Rounded.PlaylistAdd, mwString("library.merge", "Ghép nhạc"), mwString("library.merge_desc", "Nối bài này với một bài hát khác"), onMerge)
                AudioToolRow(Icons.Rounded.NotificationsActive, mwString("library.ringtone", "Đặt làm nhạc chuông"), mwString("library.ringtone_desc", "Cuộc gọi hoặc âm báo thông báo"), onRingtone)
                AudioToolRow(Icons.Rounded.DeleteForever, mwString("library.delete_song", "Xóa bài hát khỏi máy"),
                    mwString("library.delete_song_desc", "Xóa file nhạc trên điện thoại (cần xác nhận)"), onDelete)
                if (busy) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.purpleBright)
                        Text(mwString("library.processing_audio", "Đang xử lý tệp âm thanh..."), color = colors.textSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditSongInfoDialog(
    song: SongItem,
    title: String,
    artist: String,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    busy: Boolean,
    error: String?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = colors.card, border = BorderStroke(1.dp, colors.border)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogToolHeader(mwString("library.edit_song_info", "Sửa thông tin bài hát"), song.title, onBack,
                    { if (!busy) onDismiss() })
                OutlinedTextField(
                    value = title, onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(mwString("library.track_title", "Tên bài hát"), color = colors.textSecondary) },
                    textStyle = TextStyle(color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = artist, onValueChange = onArtistChange,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(mwString("library.track_artist", "Ca sĩ / nghệ sĩ"), color = colors.textSecondary) },
                    textStyle = TextStyle(color = colors.textPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                Text(mwString("library.edit_song_note", "Tên hiển thị được lưu trong MeloWave và cập nhật MediaStore khi Android cho phép. Tag âm thanh bên trong file không bị ghi đè."),
                    color = colors.textSecondary, fontSize = 11.sp)
                if (!error.isNullOrBlank()) {
                    Text(error, color = Color(0xFFEF5350), fontSize = 12.sp)
                }
                Button(onClick = onSave,
                    enabled = !busy && title.isNotBlank() && title.length <= 200 && artist.length <= 200,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
                    shape = RoundedCornerShape(14.dp)) {
                    Text(if (busy) mwString("library.saving_song", "Đang lưu...") else mwString("common.save", "Lưu thay đổi"))
                }
            }
        }
    }
}

@Composable
private fun DeleteSongConfirmDialog(
    song: SongItem,
    busy: Boolean,
    error: String?,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
            color = colors.card, border = BorderStroke(1.dp, colors.border)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogToolHeader(mwString("library.delete_song", "Xóa bài hát khỏi máy"), song.title, onBack,
                    { if (!busy) onDismiss() })
                Text(mwString("library.delete_song_warning", "Bạn có chắc muốn xóa vĩnh viễn tệp nhạc này khỏi điện thoại? Không thể hoàn tác."),
                    color = colors.textPrimary, fontSize = 14.sp)
                Text("${song.title} • ${mwArtistLabel(song.artist)}", color = colors.textSecondary, maxLines = 2)
                if (!error.isNullOrBlank()) {
                    Text(error, color = Color(0xFFEF5350), fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onBack, modifier = Modifier.weight(1f), enabled = !busy) {
                        Text(mwString("common.cancel", "Hủy"), color = colors.textSecondary)
                    }
                    Button(onClick = onConfirm, enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
                        shape = RoundedCornerShape(12.dp)) {
                        Text(if (busy) mwString("library.deleting_song", "Đang xóa...") else mwString("library.confirm_delete", "Xóa khỏi máy"), color = Color.White)
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
                DialogToolHeader(mwString("library.cut", "Cắt nhạc"), song.title, onBack, onDismiss)
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
                        text = mwString("library.cut_length", "Độ dài đoạn cắt: {time}", "time" to formatDuration((selectedRange.endInclusive - selectedRange.start).toLong())),
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
                    Text(mwString("library.cut_save", "Cắt và lưu"))
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
                    title = if (step == 0) mwString("library.merge", "Ghép nhạc") else mwString("library.trim_then_merge", "Cắt đoạn rồi ghép"),
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
                    Text(mwString("library.first_song", "Bài thứ nhất"), color = colors.textSecondary, fontSize = 12.sp)
                    MergeSelectedSongCard(
                        index = 1,
                        song = baseSong,
                        controller = controller,
                        selected = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(mwString("library.choose_second_song", "Chọn bài hát thứ hai"), color = colors.textSecondary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))

                    if (candidates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(mwString("library.no_other_songs_to_merge", "Không có bài hát cục bộ khác để ghép"), color = colors.textSecondary)
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
                        Text(mwString("library.continue_trim", "Tiếp tục cắt hai bài"))
                    }
                } else {
                    val secondSong = selected
                    if (secondSong == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(mwString("library.no_second_song", "Chưa chọn bài hát thứ hai"), color = colors.textSecondary)
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
                                            mwString("library.preview_merged", "Nghe thử kết quả sau khi ghép"),
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
                                                if (previewPlaying) mwString("library.pause_preview", "Tạm dừng nghe thử") else mwString("library.preview_two", "Nghe thử hai đoạn"),
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
                            Text(mwString("library.trim_merge_save", "Cắt hai đoạn, ghép và lưu"))
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
                "${mwArtistLabel(song.artist)} · ${song.durationLabel}",
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
                        mwString("library.drag_trim_handles", "Kéo hai đầu để chọn đoạn cần ghép"),
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
                    mwString("library.start_at", "Bắt đầu {time}", "time" to formatDuration(range.start.toLong())),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    mwString("library.end_at", "Kết thúc {time}", "time" to formatDuration(range.endInclusive.toLong())),
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            Text(
                mwString("library.selected_clip", "Đoạn được chọn: {time}", "time" to formatDuration((range.endInclusive - range.start).toLong())),
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
                DialogToolHeader(mwString("library.ringtone", "Đặt làm nhạc chuông"), song.title, onBack, onDismiss)
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
                    Text(mwString("library.call_ringtone", "Nhạc chuông cuộc gọi"))
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
                    Text(mwString("library.notification_sound", "Âm báo thông báo"))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { controller.restoreDefaultRingtone(RingtoneManager.TYPE_RINGTONE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(mwString("library.restore_ringtone", "Khôi phục cuộc gọi"), fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { controller.restoreDefaultRingtone(RingtoneManager.TYPE_NOTIFICATION) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(mwString("library.restore_notification", "Khôi phục thông báo"), fontSize = 11.sp)
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
            Icon(Icons.Rounded.ArrowBack, contentDescription = mwString("library.back", "Quay lại"), tint = colors.textSecondary)
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 5.dp)) {
            Text(title, color = colors.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(songTitle, color = colors.purpleBright, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = mwString("common.close", "Đóng"), tint = colors.textSecondary)
        }
    }
}
// endregion Merged source: LibraryScreen.kt

// region Merged source: SmartPlaylistData.kt
internal enum class BuiltInSmartPlaylist {
    MOST_PLAYED,
    RECENTLY_PLAYED,
    NEWLY_ADDED,
    NEVER_PLAYED,
    LONG_NOT_HEARD
}

internal enum class SmartMatchMode { ALL, ANY }

internal enum class SmartSortType {
    MOST_PLAYED,
    RECENTLY_PLAYED,
    NEWEST_ADDED,
    TITLE,
    ARTIST,
    DURATION_ASC,
    DURATION_DESC,
    DAILY_RANDOM
}

internal enum class SmartRuleField {
    ARTIST,
    ALBUM,
    GENRE,
    FOLDER,
    SOURCE,
    DURATION_MINUTES,
    PLAY_COUNT,
    DATE_ADDED_DAYS,
    LAST_PLAYED_DAYS,
    FAVORITE
}

internal enum class SmartRuleOperator {
    EQUALS,
    NOT_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    GREATER_THAN,
    LESS_THAN,
    BETWEEN,
    WITHIN_DAYS,
    OLDER_THAN_DAYS,
    IS_TRUE,
    IS_FALSE
}

internal data class SongPlaybackStatsEntity(
    val songId: String,
    val playCount: Int = 0,
    val totalListenMs: Long = 0L,
    val firstPlayedAtMs: Long = 0L,
    val lastPlayedAtMs: Long = 0L,
    val skipCount: Int = 0
)

internal data class SmartPlaylistEntity(
    val id: Long = 0L,
    val name: String,
    val matchMode: String = SmartMatchMode.ALL.name,
    val sortType: String = SmartSortType.MOST_PLAYED.name,
    val maxItems: Int = 50,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

internal data class SmartPlaylistRuleEntity(
    val id: Long = 0L,
    val playlistId: Long,
    val field: String,
    val operator: String,
    val value: String = "",
    val secondValue: String = ""
)

internal data class SmartPlaylistRuleDraft(
    val field: SmartRuleField = SmartRuleField.ARTIST,
    val operator: SmartRuleOperator = SmartRuleOperator.CONTAINS,
    val value: String = "",
    val secondValue: String = ""
)

internal data class SmartPlaylistDraft(
    val id: Long = 0L,
    val name: String = "",
    val matchMode: SmartMatchMode = SmartMatchMode.ALL,
    val sortType: SmartSortType = SmartSortType.MOST_PLAYED,
    val maxItems: Int = 50,
    val rules: List<SmartPlaylistRuleDraft> = listOf(SmartPlaylistRuleDraft())
)

internal data class SmartPlaylistDefinition(
    val playlist: SmartPlaylistEntity,
    val rules: List<SmartPlaylistRuleEntity>
) {
    fun toDraft(): SmartPlaylistDraft = SmartPlaylistDraft(
        id = playlist.id,
        name = playlist.name,
        matchMode = runCatching { SmartMatchMode.valueOf(playlist.matchMode) }
            .getOrDefault(SmartMatchMode.ALL),
        sortType = runCatching { SmartSortType.valueOf(playlist.sortType) }
            .getOrDefault(SmartSortType.MOST_PLAYED),
        maxItems = playlist.maxItems,
        rules = rules.map { rule ->
            SmartPlaylistRuleDraft(
                field = runCatching { SmartRuleField.valueOf(rule.field) }
                    .getOrDefault(SmartRuleField.ARTIST),
                operator = runCatching { SmartRuleOperator.valueOf(rule.operator) }
                    .getOrDefault(SmartRuleOperator.CONTAINS),
                value = rule.value,
                secondValue = rule.secondValue
            )
        }.ifEmpty { listOf(SmartPlaylistRuleDraft()) }
    )
}

internal data class SmartDuplicateGroup(
    val key: String,
    val title: String,
    val artist: String,
    val songs: List<SongItem>
)

/**
 * Local SQLite storage for Smart Playlist Pro.
 *
 * This deliberately uses Android's built-in SQLiteOpenHelper instead of Room/KAPT.
 * It keeps the same database name and table schema used by the first Room build,
 * so an already-created database remains readable while no Gradle annotation
 * processor plugin is required.
 */
internal class MeloWaveSmartPlaylistDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the initial Smart Playlist schema. Future migrations belong here.
        createTables(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Also repairs a database left half-created after an interrupted first launch.
        createTables(db)
    }

    fun readAllStats(): List<SongPlaybackStatsEntity> {
        val result = ArrayList<SongPlaybackStatsEntity>()
        readableDatabase.query(
            TABLE_STATS,
            STATS_COLUMNS,
            null,
            null,
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toStatsEntity()
        }
        return result
    }

    fun getStats(songId: String): SongPlaybackStatsEntity? {
        readableDatabase.query(
            TABLE_STATS,
            STATS_COLUMNS,
            "songId = ?",
            arrayOf(songId),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toStatsEntity() else null
        }
    }

    fun upsertStats(stats: SongPlaybackStatsEntity) {
        val values = ContentValues().apply {
            put("songId", stats.songId)
            put("playCount", stats.playCount)
            put("totalListenMs", stats.totalListenMs)
            put("firstPlayedAtMs", stats.firstPlayedAtMs)
            put("lastPlayedAtMs", stats.lastPlayedAtMs)
            put("skipCount", stats.skipCount)
        }
        val rowId = writableDatabase.insertWithOnConflict(
            TABLE_STATS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        check(rowId != -1L) { "Không thể lưu thống kê nghe nhạc" }
    }

    fun readDefinitions(): List<SmartPlaylistDefinition> {
        val playlists = ArrayList<SmartPlaylistEntity>()
        val rules = ArrayList<SmartPlaylistRuleEntity>()

        readableDatabase.query(
            TABLE_PLAYLISTS,
            PLAYLIST_COLUMNS,
            null,
            null,
            null,
            null,
            "updatedAtMs DESC, id DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) playlists += cursor.toPlaylistEntity()
        }

        readableDatabase.query(
            TABLE_RULES,
            RULE_COLUMNS,
            null,
            null,
            null,
            null,
            "id ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) rules += cursor.toRuleEntity()
        }

        val rulesByPlaylist = rules.groupBy(SmartPlaylistRuleEntity::playlistId)
        return playlists.map { playlist ->
            SmartPlaylistDefinition(
                playlist = playlist,
                rules = rulesByPlaylist[playlist.id].orEmpty()
            )
        }
    }

    fun replacePlaylist(draft: SmartPlaylistDraft): Long {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val safeName = draft.name.trim().take(80).ifBlank { "Smart Playlist" }
        var playlistId = draft.id

        db.beginTransaction()
        try {
            val createdAt = if (draft.id > 0L) {
                findPlaylistCreatedAt(db, draft.id) ?: now
            } else {
                now
            }

            val playlistValues = ContentValues().apply {
                put("name", safeName)
                put("matchMode", draft.matchMode.name)
                put("sortType", draft.sortType.name)
                put("maxItems", draft.maxItems.coerceIn(0, 500))
                put("createdAtMs", createdAt)
                put("updatedAtMs", now)
            }

            if (draft.id <= 0L) {
                playlistId = db.insertOrThrow(TABLE_PLAYLISTS, null, playlistValues)
            } else {
                val updated = db.update(
                    TABLE_PLAYLISTS,
                    playlistValues,
                    "id = ?",
                    arrayOf(draft.id.toString())
                )
                if (updated == 0) {
                    playlistValues.put("id", draft.id)
                    db.insertOrThrow(TABLE_PLAYLISTS, null, playlistValues)
                }
                playlistId = draft.id
            }

            db.delete(TABLE_RULES, "playlistId = ?", arrayOf(playlistId.toString()))
            draft.rules.take(12).forEach { rule ->
                db.insertOrThrow(
                    TABLE_RULES,
                    null,
                    ContentValues().apply {
                        put("playlistId", playlistId)
                        put("field", rule.field.name)
                        put("operator", rule.operator.name)
                        put("value", rule.value.trim().take(200))
                        put("secondValue", rule.secondValue.trim().take(200))
                    }
                )
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return playlistId
    }

    fun deletePlaylist(playlistId: Long) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Explicit rule deletion also works if foreign-key enforcement is disabled by an OEM.
            db.delete(TABLE_RULES, "playlistId = ?", arrayOf(playlistId.toString()))
            db.delete(TABLE_PLAYLISTS, "id = ?", arrayOf(playlistId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun findPlaylistCreatedAt(db: SQLiteDatabase, playlistId: Long): Long? {
        db.query(
            TABLE_PLAYLISTS,
            arrayOf("createdAtMs"),
            "id = ?",
            arrayOf(playlistId.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_STATS (
                songId TEXT NOT NULL PRIMARY KEY,
                playCount INTEGER NOT NULL DEFAULT 0,
                totalListenMs INTEGER NOT NULL DEFAULT 0,
                firstPlayedAtMs INTEGER NOT NULL DEFAULT 0,
                lastPlayedAtMs INTEGER NOT NULL DEFAULT 0,
                skipCount INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PLAYLISTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                matchMode TEXT NOT NULL,
                sortType TEXT NOT NULL,
                maxItems INTEGER NOT NULL,
                createdAtMs INTEGER NOT NULL,
                updatedAtMs INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_RULES (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                playlistId INTEGER NOT NULL,
                field TEXT NOT NULL,
                operator TEXT NOT NULL,
                value TEXT NOT NULL,
                secondValue TEXT NOT NULL,
                FOREIGN KEY(playlistId) REFERENCES $TABLE_PLAYLISTS(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_smart_playlist_rules_playlistId " +
                    "ON $TABLE_RULES(playlistId)"
        )
    }

    private fun Cursor.toStatsEntity(): SongPlaybackStatsEntity = SongPlaybackStatsEntity(
        songId = getString(getColumnIndexOrThrow("songId")),
        playCount = getInt(getColumnIndexOrThrow("playCount")),
        totalListenMs = getLong(getColumnIndexOrThrow("totalListenMs")),
        firstPlayedAtMs = getLong(getColumnIndexOrThrow("firstPlayedAtMs")),
        lastPlayedAtMs = getLong(getColumnIndexOrThrow("lastPlayedAtMs")),
        skipCount = getInt(getColumnIndexOrThrow("skipCount"))
    )

    private fun Cursor.toPlaylistEntity(): SmartPlaylistEntity = SmartPlaylistEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        matchMode = getString(getColumnIndexOrThrow("matchMode")),
        sortType = getString(getColumnIndexOrThrow("sortType")),
        maxItems = getInt(getColumnIndexOrThrow("maxItems")),
        createdAtMs = getLong(getColumnIndexOrThrow("createdAtMs")),
        updatedAtMs = getLong(getColumnIndexOrThrow("updatedAtMs"))
    )

    private fun Cursor.toRuleEntity(): SmartPlaylistRuleEntity = SmartPlaylistRuleEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        playlistId = getLong(getColumnIndexOrThrow("playlistId")),
        field = getString(getColumnIndexOrThrow("field")),
        operator = getString(getColumnIndexOrThrow("operator")),
        value = getString(getColumnIndexOrThrow("value")),
        secondValue = getString(getColumnIndexOrThrow("secondValue"))
    )

    companion object {
        private const val DATABASE_NAME = "melowave_smart_playlist.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_STATS = "song_playback_stats"
        private const val TABLE_PLAYLISTS = "smart_playlists"
        private const val TABLE_RULES = "smart_playlist_rules"

        private val STATS_COLUMNS = arrayOf(
            "songId",
            "playCount",
            "totalListenMs",
            "firstPlayedAtMs",
            "lastPlayedAtMs",
            "skipCount"
        )
        private val PLAYLIST_COLUMNS = arrayOf(
            "id",
            "name",
            "matchMode",
            "sortType",
            "maxItems",
            "createdAtMs",
            "updatedAtMs"
        )
        private val RULE_COLUMNS = arrayOf(
            "id",
            "playlistId",
            "field",
            "operator",
            "value",
            "secondValue"
        )

        @Volatile
        private var instance: MeloWaveSmartPlaylistDatabase? = null

        fun get(context: Context): MeloWaveSmartPlaylistDatabase =
            instance ?: synchronized(this) {
                instance ?: MeloWaveSmartPlaylistDatabase(context).also { instance = it }
            }
    }
}

@Stable
internal class SmartPlaylistRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val database = MeloWaveSmartPlaylistDatabase.get(context)
    private val writeMutex = Mutex()

    var statsBySongId by mutableStateOf<Map<String, SongPlaybackStatsEntity>>(emptyMap())
        private set
    var customPlaylists by mutableStateOf<List<SmartPlaylistDefinition>>(emptyList())
        private set
    var ready by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    init {
        reloadAll()
    }

    private fun reloadAll() {
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                database.readAllStats() to database.readDefinitions()
            }
            withContext(Dispatchers.Main.immediate) {
                result.fold(
                    onSuccess = { (stats, definitions) ->
                        statsBySongId = stats.associateBy(SongPlaybackStatsEntity::songId)
                        customPlaylists = definitions
                        ready = true
                        lastError = null
                    },
                    onFailure = { error ->
                        ready = true
                        lastError = error.localizedMessage
                            ?: "Không thể đọc dữ liệu Smart Playlist"
                    }
                )
            }
        }
    }

    fun recordListening(
        songId: String,
        listenDeltaMs: Long,
        incrementPlayCount: Boolean,
        incrementSkipCount: Boolean,
        playedAtMs: Long = System.currentTimeMillis()
    ) {
        if (songId.isBlank()) return
        val safeDelta = listenDeltaMs.coerceAtLeast(0L)
        if (safeDelta <= 0L && !incrementPlayCount && !incrementSkipCount) return

        scope.launch(Dispatchers.IO) {
            val result = writeMutex.withLock {
                runCatching {
                    val previous = database.getStats(songId)
                        ?: SongPlaybackStatsEntity(songId = songId)
                    val updated = previous.copy(
                        playCount = previous.playCount + if (incrementPlayCount) 1 else 0,
                        totalListenMs = previous.totalListenMs + safeDelta,
                        firstPlayedAtMs = when {
                            previous.firstPlayedAtMs > 0L -> previous.firstPlayedAtMs
                            incrementPlayCount -> playedAtMs
                            else -> 0L
                        },
                        lastPlayedAtMs = if (incrementPlayCount) {
                            playedAtMs
                        } else {
                            previous.lastPlayedAtMs
                        },
                        skipCount = previous.skipCount + if (incrementSkipCount) 1 else 0
                    )
                    database.upsertStats(updated)
                    updated
                }
            }

            withContext(Dispatchers.Main.immediate) {
                result.fold(
                    onSuccess = { updated ->
                        statsBySongId = statsBySongId.toMutableMap().apply {
                            put(updated.songId, updated)
                        }
                        lastError = null
                    },
                    onFailure = { error ->
                        lastError = error.localizedMessage
                            ?: "Không thể lưu thống kê nghe nhạc"
                    }
                )
            }
        }
    }

    fun savePlaylist(draft: SmartPlaylistDraft, onSaved: ((Long) -> Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            val result = writeMutex.withLock {
                runCatching {
                    val id = database.replacePlaylist(draft)
                    id to database.readDefinitions()
                }
            }
            withContext(Dispatchers.Main.immediate) {
                result.fold(
                    onSuccess = { (id, definitions) ->
                        customPlaylists = definitions
                        lastError = null
                        onSaved?.invoke(id)
                    },
                    onFailure = { error ->
                        lastError = error.localizedMessage ?: "Không thể lưu Smart Playlist"
                    }
                )
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        if (playlistId <= 0L) return
        scope.launch(Dispatchers.IO) {
            val result = writeMutex.withLock {
                runCatching {
                    database.deletePlaylist(playlistId)
                    database.readDefinitions()
                }
            }
            withContext(Dispatchers.Main.immediate) {
                result.fold(
                    onSuccess = { definitions ->
                        customPlaylists = definitions
                        lastError = null
                    },
                    onFailure = { error ->
                        lastError = error.localizedMessage ?: "Không thể xóa Smart Playlist"
                    }
                )
            }
        }
    }

    fun clearError() {
        lastError = null
    }
}
internal fun smartPlaylistCount(
    type: BuiltInSmartPlaylist,
    songs: List<SongItem>,
    stats: Map<String, SongPlaybackStatsEntity>
): Int {
    val now = System.currentTimeMillis()
    return songs.count { song ->
        when (type) {
            BuiltInSmartPlaylist.MOST_PLAYED -> (stats[song.id]?.playCount ?: 0) > 0
            BuiltInSmartPlaylist.RECENTLY_PLAYED -> (stats[song.id]?.lastPlayedAtMs ?: 0L) > 0L
            BuiltInSmartPlaylist.NEWLY_ADDED -> song.dateAddedMs > 0L
            BuiltInSmartPlaylist.NEVER_PLAYED -> (stats[song.id]?.playCount ?: 0) == 0
            BuiltInSmartPlaylist.LONG_NOT_HEARD -> {
                val last = stats[song.id]?.lastPlayedAtMs ?: 0L
                last > 0L && now - last >= 30L * DAY_MS
            }
        }
    }
}

internal fun smartPlaylistSongs(
    type: BuiltInSmartPlaylist,
    songs: List<SongItem>,
    stats: Map<String, SongPlaybackStatsEntity>,
    limit: Int = 0
): List<SongItem> {
    val now = System.currentTimeMillis()
    val result = when (type) {
        BuiltInSmartPlaylist.MOST_PLAYED -> songs
            .filter { (stats[it.id]?.playCount ?: 0) > 0 }
            .sortedWith(
                compareByDescending<SongItem> { stats[it.id]?.playCount ?: 0 }
                    .thenByDescending { stats[it.id]?.totalListenMs ?: 0L }
                    .thenBy { smartNormalize(it.title) }
            )

        BuiltInSmartPlaylist.RECENTLY_PLAYED -> songs
            .filter { (stats[it.id]?.lastPlayedAtMs ?: 0L) > 0L }
            .sortedByDescending { stats[it.id]?.lastPlayedAtMs ?: 0L }

        BuiltInSmartPlaylist.NEWLY_ADDED -> songs
            .filter { it.dateAddedMs > 0L }
            .sortedByDescending(SongItem::dateAddedMs)

        BuiltInSmartPlaylist.NEVER_PLAYED -> songs
            .filter { (stats[it.id]?.playCount ?: 0) == 0 }
            .sortedWith(
                compareByDescending<SongItem> { it.dateAddedMs }
                    .thenBy { smartNormalize(it.title) }
            )

        BuiltInSmartPlaylist.LONG_NOT_HEARD -> songs
            .filter {
                val last = stats[it.id]?.lastPlayedAtMs ?: 0L
                last > 0L && now - last >= 30L * DAY_MS
            }
            .sortedBy { stats[it.id]?.lastPlayedAtMs ?: Long.MAX_VALUE }
    }
    return if (limit <= 0) result else result.take(limit.coerceAtMost(5_000))
}

internal fun countCustomSmartPlaylistSongs(
    definition: SmartPlaylistDefinition,
    songs: List<SongItem>,
    stats: Map<String, SongPlaybackStatsEntity>,
    isFavorite: (SongItem) -> Boolean
): Int {
    val matchMode = runCatching { SmartMatchMode.valueOf(definition.playlist.matchMode) }
        .getOrDefault(SmartMatchMode.ALL)
    val rules = definition.rules
    if (rules.isEmpty()) return songs.size
    val now = System.currentTimeMillis()
    return songs.count { song ->
        val results = rules.map { rule ->
            matchesSmartRule(song, stats[song.id], isFavorite(song), rule, now)
        }
        if (matchMode == SmartMatchMode.ALL) results.all { it } else results.any { it }
    }
}

internal fun evaluateCustomSmartPlaylistSongs(
    definition: SmartPlaylistDefinition,
    songs: List<SongItem>,
    stats: Map<String, SongPlaybackStatsEntity>,
    isFavorite: (SongItem) -> Boolean
): List<SongItem> {
    val matchMode = runCatching { SmartMatchMode.valueOf(definition.playlist.matchMode) }
        .getOrDefault(SmartMatchMode.ALL)
    val sortType = runCatching { SmartSortType.valueOf(definition.playlist.sortType) }
        .getOrDefault(SmartSortType.MOST_PLAYED)
    val rules = definition.rules
    val now = System.currentTimeMillis()

    val matched = songs.filter { song ->
        if (rules.isEmpty()) true
        else {
            val results = rules.map { rule ->
                matchesSmartRule(song, stats[song.id], isFavorite(song), rule, now)
            }
            if (matchMode == SmartMatchMode.ALL) results.all { it } else results.any { it }
        }
    }

    val sorted = when (sortType) {
        SmartSortType.MOST_PLAYED -> matched.sortedWith(
            compareByDescending<SongItem> { stats[it.id]?.playCount ?: 0 }
                .thenByDescending { stats[it.id]?.lastPlayedAtMs ?: 0L }
        )
        SmartSortType.RECENTLY_PLAYED -> matched.sortedByDescending {
            stats[it.id]?.lastPlayedAtMs ?: 0L
        }
        SmartSortType.NEWEST_ADDED -> matched.sortedByDescending(SongItem::dateAddedMs)
        SmartSortType.TITLE -> matched.sortedBy { smartNormalize(it.title) }
        SmartSortType.ARTIST -> matched.sortedWith(
            compareBy<SongItem> { smartNormalize(it.artist) }.thenBy { smartNormalize(it.title) }
        )
        SmartSortType.DURATION_ASC -> matched.sortedBy { it.durationMs }
        SmartSortType.DURATION_DESC -> matched.sortedByDescending { it.durationMs }
        SmartSortType.DAILY_RANDOM -> {
            val dailySeed = System.currentTimeMillis() / DAY_MS
            matched.sortedBy { (it.id.hashCode().toLong() xor dailySeed) }
        }
    }

    val maxItems = definition.playlist.maxItems
    return if (maxItems <= 0) sorted else sorted.take(maxItems.coerceAtMost(500))
}

private fun matchesSmartRule(
    song: SongItem,
    stats: SongPlaybackStatsEntity?,
    favorite: Boolean,
    rule: SmartPlaylistRuleEntity,
    now: Long
): Boolean {
    val field = runCatching { SmartRuleField.valueOf(rule.field) }.getOrNull() ?: return true
    val operator = runCatching { SmartRuleOperator.valueOf(rule.operator) }.getOrNull() ?: return true
    val first = rule.value.trim()
    val second = rule.secondValue.trim()

    fun stringMatch(actual: String): Boolean {
        val normalizedActual = smartNormalize(actual)
        val normalizedExpected = smartNormalize(first)
        if (normalizedExpected.isBlank()) return false
        return when (operator) {
            SmartRuleOperator.EQUALS -> normalizedActual == normalizedExpected
            SmartRuleOperator.NOT_EQUALS -> normalizedActual != normalizedExpected
            SmartRuleOperator.CONTAINS -> normalizedActual.contains(normalizedExpected)
            SmartRuleOperator.NOT_CONTAINS -> !normalizedActual.contains(normalizedExpected)
            else -> true
        }
    }

    fun numberMatch(actual: Double): Boolean {
        val expected = first.replace(',', '.').toDoubleOrNull() ?: return false
        val expected2 = if (operator == SmartRuleOperator.BETWEEN) {
            second.replace(',', '.').toDoubleOrNull() ?: return false
        } else expected
        return when (operator) {
            SmartRuleOperator.EQUALS -> abs(actual - expected) < 0.0001
            SmartRuleOperator.NOT_EQUALS -> abs(actual - expected) >= 0.0001
            SmartRuleOperator.GREATER_THAN -> actual > expected
            SmartRuleOperator.LESS_THAN -> actual < expected
            SmartRuleOperator.BETWEEN -> actual in minOf(expected, expected2)..maxOf(expected, expected2)
            else -> true
        }
    }

    fun dateMatch(timestamp: Long): Boolean {
        if (timestamp <= 0L) return false
        val days = ((now - timestamp).coerceAtLeast(0L) / DAY_MS).toDouble()
        val expected = first.replace(',', '.').toDoubleOrNull() ?: return false
        return when (operator) {
            SmartRuleOperator.WITHIN_DAYS -> days <= expected
            SmartRuleOperator.OLDER_THAN_DAYS -> days > expected
            SmartRuleOperator.LESS_THAN -> days < expected
            SmartRuleOperator.GREATER_THAN -> days > expected
            else -> true
        }
    }

    return when (field) {
        SmartRuleField.ARTIST -> stringMatch(song.artist)
        SmartRuleField.ALBUM -> stringMatch(song.album)
        SmartRuleField.GENRE -> stringMatch(song.genre)
        SmartRuleField.FOLDER -> stringMatch(song.folderName)
        SmartRuleField.SOURCE -> stringMatch(song.source.name)
        SmartRuleField.DURATION_MINUTES -> numberMatch(song.durationMs / 60_000.0)
        SmartRuleField.PLAY_COUNT -> numberMatch((stats?.playCount ?: 0).toDouble())
        SmartRuleField.DATE_ADDED_DAYS -> dateMatch(song.dateAddedMs)
        SmartRuleField.LAST_PLAYED_DAYS -> dateMatch(stats?.lastPlayedAtMs ?: 0L)
        SmartRuleField.FAVORITE -> when (operator) {
            SmartRuleOperator.IS_TRUE -> favorite
            SmartRuleOperator.IS_FALSE -> !favorite
            SmartRuleOperator.EQUALS -> favorite == first.equals("true", ignoreCase = true)
            SmartRuleOperator.NOT_EQUALS -> favorite != first.equals("true", ignoreCase = true)
            else -> true
        }
    }
}

internal fun findSmartDuplicateGroups(songs: List<SongItem>): List<SmartDuplicateGroup> {
    val local = songs.filter {
        it.source == SongSource.LOCAL_MEDIASTORE || it.source == SongSource.DOWNLOADED
    }
    val baseGroups = local.groupBy { song ->
        val cleanTitle = smartNormalize(song.title)
            .replace(Regex("\\b(official|audio|video|lyrics|lyric|320kbps|128kbps|remaster(?:ed)?)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        val cleanArtist = smartNormalize(song.artist)
            .replace(Regex("\\b(official|topic)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        "$cleanTitle|$cleanArtist"
    }.filterKeys { it.substringBefore('|').length >= 2 }

    val output = mutableListOf<SmartDuplicateGroup>()
    baseGroups.forEach { (baseKey, candidates) ->
        if (candidates.size < 2) return@forEach
        val remaining = candidates.sortedBy { it.durationMs }.toMutableList()
        var clusterIndex = 0
        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val cluster = mutableListOf(seed)
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val closeDuration = seed.durationMs <= 0L || candidate.durationMs <= 0L ||
                        abs(seed.durationMs - candidate.durationMs) <= 3_000L
                if (closeDuration) {
                    cluster += candidate
                    iterator.remove()
                }
            }
            if (cluster.size > 1) {
                output += SmartDuplicateGroup(
                    key = "$baseKey|${clusterIndex++}",
                    title = cluster.first().title,
                    artist = cluster.first().artist,
                    songs = cluster.sortedWith(
                        compareByDescending<SongItem> { it.mimeType?.contains("flac", true) == true }
                            .thenByDescending { it.durationMs }
                    )
                )
            }
        }
    }
    return output.sortedByDescending { it.songs.size }
}

internal fun smartRuleDefaultOperator(field: SmartRuleField): SmartRuleOperator = when (field) {
    SmartRuleField.ARTIST,
    SmartRuleField.ALBUM,
    SmartRuleField.GENRE,
    SmartRuleField.FOLDER -> SmartRuleOperator.CONTAINS

    SmartRuleField.SOURCE -> SmartRuleOperator.EQUALS

    SmartRuleField.DURATION_MINUTES,
    SmartRuleField.PLAY_COUNT -> SmartRuleOperator.GREATER_THAN

    SmartRuleField.DATE_ADDED_DAYS,
    SmartRuleField.LAST_PLAYED_DAYS -> SmartRuleOperator.WITHIN_DAYS

    SmartRuleField.FAVORITE -> SmartRuleOperator.IS_TRUE
}

internal fun smartRuleOperators(field: SmartRuleField): List<SmartRuleOperator> = when (field) {
    SmartRuleField.ARTIST,
    SmartRuleField.ALBUM,
    SmartRuleField.GENRE,
    SmartRuleField.FOLDER -> listOf(
        SmartRuleOperator.CONTAINS,
        SmartRuleOperator.NOT_CONTAINS,
        SmartRuleOperator.EQUALS,
        SmartRuleOperator.NOT_EQUALS
    )

    SmartRuleField.SOURCE -> listOf(
        SmartRuleOperator.EQUALS,
        SmartRuleOperator.NOT_EQUALS
    )

    SmartRuleField.DURATION_MINUTES,
    SmartRuleField.PLAY_COUNT -> listOf(
        SmartRuleOperator.GREATER_THAN,
        SmartRuleOperator.LESS_THAN,
        SmartRuleOperator.EQUALS,
        SmartRuleOperator.BETWEEN
    )

    SmartRuleField.DATE_ADDED_DAYS,
    SmartRuleField.LAST_PLAYED_DAYS -> listOf(
        SmartRuleOperator.WITHIN_DAYS,
        SmartRuleOperator.OLDER_THAN_DAYS
    )

    SmartRuleField.FAVORITE -> listOf(
        SmartRuleOperator.IS_TRUE,
        SmartRuleOperator.IS_FALSE
    )
}

private fun smartNormalize(value: String): String = Normalizer.normalize(
    value.lowercase(Locale.ROOT),
    Normalizer.Form.NFD
).replace(Regex("\\p{Mn}+"), "")
    .replace('đ', 'd')
    .replace(Regex("[^a-z0-9]+"), " ")
    .trim()

private const val DAY_MS = 86_400_000L
// endregion Merged source: SmartPlaylistData.kt
