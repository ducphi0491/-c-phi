package duc_phi.music

import android.media.audiofx.Equalizer as AudioEqualizer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C

@Composable
internal fun NowPlayingScreen(controller: MusicController) {
    val colors = LocalMusicColors.current
    val song = controller.currentSong
    var showEqualizer by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }

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

    androidx.compose.runtime.LaunchedEffect(song.id) {
        controller.ensureArtwork(song)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, Color(0xFF0A0B22), colors.background)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            NowPlayingHeader(
                song = song,
                source = controller.sourceLabel(song.source),
                onBack = controller::goBackFromPlayer,
                onFavorite = { controller.toggleFavorite(song) },
                isFavorite = controller.isFavorite(song)
            )
        }

        item {
            ArtworkImage(
                song = song,
                artworkUrl = controller.artworkFor(song),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .height(390.dp),
                cornerRadius = 26
            )
        }

        item {
            TrackTitleBlock(
                controller = controller,
                song = song
            )
        }

        item {
            PlaybackProgress(
                positionMs = controller.positionMs,
                durationMs = controller.durationMs,
                onSeek = controller::seekTo
            )
        }

        item {
            MainPlaybackControls(controller)
        }

        item {
            PlayerActions(
                controller = controller,
                song = song,
                onEqualizer = { showEqualizer = true },
                onTimer = { showTimer = true }
            )
        }

        item {
            QueuePanel(controller)
        }
    }

    if (showEqualizer) {
        EqualizerDialog(
            audioSessionId = controller.audioSessionId,
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
}

@Composable
private fun NowPlayingHeader(
    song: SongItem,
    source: String,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    isFavorite: Boolean
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = "Quay lại", tint = colors.textPrimary)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ĐANG PHÁT",
                color = colors.purpleBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.source == SongSource.PERSONAL_DRIVE || song.source == SongSource.PUBLIC_DRIVE) {
                    Icon(
                        Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(source, color = colors.textSecondary, fontSize = 13.sp)
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Yêu thích",
                tint = if (isFavorite) colors.magenta else colors.textSecondary
            )
        }
    }
}

@Composable
private fun TrackTitleBlock(
    controller: MusicController,
    song: SongItem
) {
    val colors = LocalMusicColors.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = colors.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = song.artist,
                    color = colors.textSecondary,
                    fontSize = 19.sp,
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
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetadataChip(controller.sourceLabel(song.source), Icons.Rounded.Cloud)
            MetadataChip(controller.formatBadge(song), Icons.Rounded.MusicNote)
            if (song.mimeType?.contains("flac", ignoreCase = true) == true) {
                MetadataChip("Lossless", Icons.Rounded.Equalizer)
            }
        }
    }
}

@Composable
private fun MetadataChip(text: String, icon: ImageVector) {
    val colors = LocalMusicColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.purple.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = colors.purpleBright, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(text, color = colors.purpleBright, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            )
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
        IconButton(onClick = controller::toggleShuffle, modifier = Modifier.size(46.dp)) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Ngẫu nhiên",
                tint = if (controller.shuffleEnabled) colors.purpleBright else colors.textSecondary
            )
        }
        IconButton(onClick = controller::previous, modifier = Modifier.size(54.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Bài trước",
                tint = colors.textPrimary,
                modifier = Modifier.size(38.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(86.dp)
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
        IconButton(onClick = controller::next, modifier = Modifier.size(54.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Bài tiếp theo",
                tint = colors.textPrimary,
                modifier = Modifier.size(38.dp)
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
    onTimer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerAction(Icons.Rounded.Equalizer, "Equalizer", onEqualizer)
        PlayerAction(Icons.Rounded.PlaylistPlay, "Danh sách") { controller.openLibrary() }
        PlayerAction(Icons.Rounded.Timer, "Hẹn giờ", onTimer)
        PlayerAction(Icons.Rounded.Download, "Tải về") { controller.downloadSongForOffline(song) }
        PlayerAction(Icons.Rounded.Share, "Chia sẻ", controller::shareCurrentSong)
    }
}

@Composable
private fun PlayerAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalMusicColors.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.cardRaised,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = colors.textSecondary, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = colors.textPrimary, fontSize = 11.sp)
    }
}

@Composable
private fun QueuePanel(controller: MusicController) {
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
                        onFavorite = { controller.toggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerDialog(
    audioSessionId: Int,
    onDismiss: () -> Unit
) {
    val colors = LocalMusicColors.current
    var equalizer by remember(audioSessionId) { mutableStateOf<AudioEqualizer?>(null) }
    val levels = remember(audioSessionId) { mutableStateListOf<Short>() }
    var range by remember(audioSessionId) { mutableStateOf(shortArrayOf(-1500, 1500)) }

    DisposableEffect(audioSessionId) {
        val effect = if (audioSessionId > 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            runCatching {
                AudioEqualizer(0, audioSessionId).apply { enabled = true }
            }.getOrNull()
        } else null
        equalizer = effect
        levels.clear()
        if (effect != null) {
            range = effect.bandLevelRange
            repeat(effect.numberOfBands.toInt()) { index ->
                levels.add(effect.getBandLevel(index.toShort()))
            }
        }
        onDispose {
            runCatching { effect?.release() }
            equalizer = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = colors.backgroundSoft,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = colors.purpleBright)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Equalizer",
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Đóng", tint = colors.textSecondary)
                    }
                }
                if (equalizer == null || levels.isEmpty()) {
                    Text(
                        "Equalizer chưa sẵn sàng. Hãy phát bài hát trước rồi mở lại.",
                        color = colors.textSecondary
                    )
                } else {
                    levels.forEachIndexed { index, level ->
                        val frequency = runCatching {
                            equalizer?.getCenterFreq(index.toShort())?.div(1000)
                        }.getOrNull() ?: 0
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${frequency}Hz", color = colors.textPrimary, fontSize = 12.sp)
                                Text("${level / 100f}dB", color = colors.purpleBright, fontSize = 12.sp)
                            }
                            Slider(
                                value = level.toFloat(),
                                onValueChange = { newValue ->
                                    val newLevel = newValue.toInt().toShort()
                                    levels[index] = newLevel
                                    runCatching { equalizer?.setBandLevel(index.toShort(), newLevel) }
                                },
                                valueRange = range[0].toFloat()..range[1].toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.purpleBright,
                                    activeTrackColor = colors.purple,
                                    inactiveTrackColor = colors.border
                                )
                            )
                        }
                    }
                    TextButton(
                        onClick = {
                            levels.indices.forEach { index ->
                                levels[index] = 0
                                runCatching { equalizer?.setBandLevel(index.toShort(), 0) }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Đặt lại", color = colors.purpleBright)
                    }
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
