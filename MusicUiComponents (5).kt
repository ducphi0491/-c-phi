package duc_phi.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Immutable
internal data class MusicColors(
    val background: Color = Color(0xFF020812),
    val backgroundSoft: Color = Color(0xFF06111E),
    val card: Color = Color(0xFF091625),
    val cardRaised: Color = Color(0xFF0E2033),
    val border: Color = Color(0xFF21364D),
    val borderSoft: Color = Color(0xFF172B40),
    val purple: Color = Color(0xFF8D35FF),
    val purpleBright: Color = Color(0xFFB64DFF),
    val magenta: Color = Color(0xFFF22EDB),
    val blue: Color = Color(0xFF2F8BFF),
    val cyan: Color = Color(0xFF24C8FF),
    val green: Color = Color(0xFF21D483),
    val textPrimary: Color = Color(0xFFF7F8FC),
    val textSecondary: Color = Color(0xFFA6B0C3),
    val textMuted: Color = Color(0xFF718096),
    val red: Color = Color(0xFFFF4F70),
    val isDark: Boolean = true
)

internal val DarkMusicColors = MusicColors()

internal val LightMusicColors = MusicColors(
    background = Color(0xFFF4F7FC),
    backgroundSoft = Color(0xFFE8EEF8),
    card = Color.White,
    cardRaised = Color(0xFFF0F4FA),
    border = Color(0xFFD3DFEE),
    borderSoft = Color(0xFFE2EAF4),
    purple = Color(0xFF7929E8),
    purpleBright = Color(0xFF8D35FF),
    magenta = Color(0xFFD921C5),
    blue = Color(0xFF1E75E8),
    cyan = Color(0xFF0589C7),
    green = Color(0xFF14995D),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textMuted = Color(0xFF9CA3AF),
    red = Color(0xFFEF4444),
    isDark = false
)

internal val LocalMusicColors = compositionLocalOf { DarkMusicColors }
internal val LocalAppLanguage = compositionLocalOf { "vi" }

@Composable
internal fun TunertoolsTheme(
    isDark: Boolean = true,
    language: String = "vi",
    content: @Composable () -> Unit
) {
    val colors = if (isDark) DarkMusicColors else LightMusicColors
    val material = if (isDark) {
        darkColorScheme(
            primary = colors.purple,
            secondary = colors.blue,
            background = colors.background,
            surface = colors.card,
            onPrimary = Color.White,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = colors.purple,
            secondary = colors.blue,
            background = colors.background,
            surface = colors.card,
            onPrimary = Color.White,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }
    CompositionLocalProvider(
        LocalMusicColors provides colors,
        LocalAppLanguage provides language
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}

@Composable
internal fun ArtworkImage(
    song: SongItem,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 16
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val shape = RoundedCornerShape(cornerRadius.dp)

    // Luon co anh du phong hai not nhac: khi chua co anh, dang tai hoac link anh loi.
    // Anh that (neu co) nam phia tren va phu kin placeholder sau khi Coil tai thanh cong.
    Box(modifier = modifier.clip(shape)) {
        DoubleNoteArtworkPlaceholder(modifier = Modifier.fillMaxSize())
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Ảnh bìa ${song.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DoubleNoteArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF05091A),
                    Color(0xFF171039),
                    Color(0xFF071429)
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val unit = size.minDimension
            if (unit <= 0f) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val noteBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF46C7),
                    Color(0xFFB43EFF),
                    Color(0xFF3DAEFF)
                ),
                start = Offset(center.x - unit * 0.32f, center.y + unit * 0.24f),
                end = Offset(center.x + unit * 0.30f, center.y - unit * 0.30f)
            )

            // Glow nhe phia sau, khong lam bien dang bieu tuong.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9E3BFF).copy(alpha = 0.30f),
                        Color(0xFF246BFF).copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = unit * 0.50f
                ),
                radius = unit * 0.50f,
                center = center
            )

            // Ty le cua cap not moc kep: dau not thap, than cao va beam nghieng nhe.
            val leftHeadCenter = Offset(center.x - unit * 0.205f, center.y + unit * 0.170f)
            val rightHeadCenter = Offset(center.x + unit * 0.165f, center.y + unit * 0.075f)
            val headWidth = unit * 0.245f
            val headHeight = unit * 0.155f
            val stemWidth = (unit * 0.047f).coerceAtLeast(1.5f)

            val leftStemX = leftHeadCenter.x + headWidth * 0.37f
            val rightStemX = rightHeadCenter.x + headWidth * 0.37f
            val leftStemTop = center.y - unit * 0.245f
            val rightStemTop = center.y - unit * 0.340f

            // Than not dung thang, cham vao mep phai dau not.
            drawLine(
                brush = noteBrush,
                start = Offset(leftStemX, leftHeadCenter.y - headHeight * 0.08f),
                end = Offset(leftStemX, leftStemTop),
                strokeWidth = stemWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawLine(
                brush = noteBrush,
                start = Offset(rightStemX, rightHeadCenter.y - headHeight * 0.08f),
                end = Offset(rightStemX, rightStemTop),
                strokeWidth = stemWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            // Beam day va mem, noi hai than theo dung do nghieng cua not kep.
            val beamPath = Path().apply {
                moveTo(leftStemX - stemWidth * 0.52f, leftStemTop - unit * 0.008f)
                cubicTo(
                    leftStemX + unit * 0.105f,
                    leftStemTop - unit * 0.022f,
                    rightStemX - unit * 0.100f,
                    rightStemTop - unit * 0.012f,
                    rightStemX + stemWidth * 0.55f,
                    rightStemTop - unit * 0.004f
                )
                lineTo(rightStemX + stemWidth * 0.55f, rightStemTop + unit * 0.086f)
                cubicTo(
                    rightStemX - unit * 0.100f,
                    rightStemTop + unit * 0.076f,
                    leftStemX + unit * 0.105f,
                    leftStemTop + unit * 0.067f,
                    leftStemX - stemWidth * 0.52f,
                    leftStemTop + unit * 0.080f
                )
                close()
            }
            drawPath(beamPath, brush = noteBrush)

            // Dau not ellipse xoay nhe, khong ve thanh hinh tron.
            fun drawNoteHead(centerPoint: Offset) {
                withTransform({ rotate(-18f, pivot = centerPoint) }) {
                    drawOval(
                        brush = noteBrush,
                        topLeft = Offset(
                            centerPoint.x - headWidth / 2f,
                            centerPoint.y - headHeight / 2f
                        ),
                        size = Size(headWidth, headHeight)
                    )
                }
            }
            drawNoteHead(leftHeadCenter)
            drawNoteHead(rightHeadCenter)

            // Vet sang rat mong giup icon ro net o kich thuoc nho.
            drawLine(
                color = Color.White.copy(alpha = 0.20f),
                start = Offset(leftStemX + stemWidth * 0.12f, leftStemTop + unit * 0.010f),
                end = Offset(rightStemX + stemWidth * 0.12f, rightStemTop + unit * 0.010f),
                strokeWidth = (unit * 0.010f).coerceAtLeast(0.8f),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

@Composable
internal fun SectionTitle(
    title: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        if (!actionText.isNullOrBlank() && onAction != null) {
            Text(
                text = actionText,
                color = colors.purpleBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
internal fun SourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    colorsGradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Surface(
        modifier = modifier
            .height(112.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colorsGradient))
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.22f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun SongRow(
    song: SongItem,
    artworkUrl: String?,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) colors.purple.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtworkImage(
            song = song,
            artworkUrl = artworkUrl,
            modifier = Modifier.size(56.dp),
            cornerRadius = 12
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) colors.purpleBright else colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${song.durationLabel}",
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Yêu thích",
                tint = if (isFavorite) colors.magenta else colors.textMuted,
                modifier = Modifier.size(22.dp)
            )
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(34.dp)) {
                Text("⋮", color = colors.textSecondary, fontSize = 24.sp)
            }
        }
    }
}

private class NotchedMiniPlayerShape(
    private val cornerRadius: Dp = 24.dp,
    private val notchRadius: Dp = 46.dp,
    private val notchDepth: Dp = 24.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val corner = with(density) { cornerRadius.toPx() }
            .coerceAtMost(size.height / 2f)
            .coerceAtMost(size.width / 2f)
        val radius = with(density) { notchRadius.toPx() }
            .coerceAtMost(size.width * 0.22f)
        val depth = with(density) { notchDepth.toPx() }
            .coerceAtMost(size.height * 0.42f)
        val centerX = size.width / 2f
        val shoulder = radius * 1.16f

        val path = Path().apply {
            moveTo(corner, 0f)
            lineTo(size.width - corner, 0f)
            quadraticBezierTo(size.width, 0f, size.width, corner)
            lineTo(size.width, size.height - corner)
            quadraticBezierTo(size.width, size.height, size.width - corner, size.height)

            // Khoet lom o canh duoi de nut Play noi len va "lem" vao card.
            lineTo(centerX + shoulder, size.height)
            cubicTo(
                centerX + radius * 0.56f,
                size.height,
                centerX + radius * 0.60f,
                size.height - depth,
                centerX,
                size.height - depth
            )
            cubicTo(
                centerX - radius * 0.60f,
                size.height - depth,
                centerX - radius * 0.56f,
                size.height,
                centerX - shoulder,
                size.height
            )

            lineTo(corner, size.height)
            quadraticBezierTo(0f, size.height, 0f, size.height - corner)
            lineTo(0f, corner)
            quadraticBezierTo(0f, 0f, corner, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun MiniPlayer(
    song: SongItem,
    artworkUrl: String?,
    positionMs: Long,
    durationMs: Long,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val shape = remember { NotchedMiniPlayerShape() }
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val shownPosition = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    val glassOverlay = if (colors.isDark) {
        listOf(
            Color(0xFF050D19).copy(alpha = 0.90f),
            Color(0xFF15122A).copy(alpha = 0.70f),
            Color(0xFF071522).copy(alpha = 0.84f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.88f),
            colors.card.copy(alpha = 0.73f),
            Color.White.copy(alpha = 0.82f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(shape)
            .background(colors.card, shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        colors.purpleBright.copy(alpha = 0.88f),
                        colors.blue.copy(alpha = 0.52f),
                        colors.purple.copy(alpha = 0.78f)
                    )
                ),
                shape = shape
            )
            .clickable(onClick = onClick)
    ) {
        // Anh bia lam nen mo trong suot. Neu chua co anh, van dung placeholder hai not nhac.
        if (!artworkUrl.isNullOrBlank()) {
            val context = androidx.compose.ui.platform.LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (colors.isDark) 0.38f else 0.22f
                        scaleX = 1.10f
                        scaleY = 1.10f
                    }
            )
        } else {
            DoubleNoteArtworkPlaceholder(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.36f }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(glassOverlay))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (colors.isDark) 0.08f else 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (colors.isDark) 0.30f else 0.06f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 25.dp),
            verticalAlignment = Alignment.Top
        ) {
            ArtworkImage(
                song = song,
                artworkUrl = artworkUrl,
                modifier = Modifier.size(70.dp),
                cornerRadius = 15
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (isFavorite) colors.magenta else colors.textPrimary.copy(alpha = 0.90f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                ) {
                    val y = size.height / 2f
                    val stroke = 4.dp.toPx()
                    val thumbRadius = 5.dp.toPx()
                    val thumbX = (size.width * progress)
                        .coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
                    drawLine(
                        color = colors.borderSoft.copy(alpha = 0.95f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            listOf(colors.purple, colors.magenta, colors.blue)
                        ),
                        start = Offset(0f, y),
                        end = Offset(thumbX, y),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawCircle(
                        color = Color.White,
                        radius = thumbRadius,
                        center = Offset(thumbX, y)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(shownPosition),
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = formatDuration(durationMs.coerceAtLeast(0L)),
                        color = colors.textPrimary.copy(alpha = 0.90f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlayerBottomArea(
    controller: MusicController,
    selectedScreen: MusicScreen,
    modifier: Modifier = Modifier
) {
    val song = controller.currentSong
    val colors = LocalMusicColors.current

    // Hai tang doc lap: mini-player o phia tren va dock mau den o phia duoi.
    // Mini-player cao 112dp; dock nam sat day: voi 190dp tong chieu cao,
    // canh dock bat dau o y=120dp, cach canh mini-player 8dp.
    // Nut Play o top dock (y=90dp) nhoi vao phan khuyet duoi mini-player.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, top = 3.dp, end = 12.dp, bottom = 4.dp)
            .height(if (song != null) 190.dp else 100.dp)
    ) {
        if (song != null) {
            MiniPlayer(
                song = song,
                artworkUrl = controller.artworkFor(song),
                positionMs = controller.positionMs,
                durationMs = controller.durationMs,
                isFavorite = controller.isFavorite(song),
                onClick = controller::openNowPlaying,
                onFavorite = { controller.toggleFavorite(song) },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Draw dock last: only the single raised Play button overlaps the song card.
        BottomPlayerBar(
            selectedScreen = selectedScreen,
            isPlaying = controller.isPlaying,
            onHome = controller::openHome,
            onPrevious = controller::previous,
            onPlayPause = controller::togglePlayback,
            onNext = controller::next,
            onLibrary = { controller.openLibrary() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
internal fun BottomPlayerBar(
    selectedScreen: MusicScreen,
    isPlaying: Boolean,
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val playInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (playPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 520f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        // Dock den thuan, tach hoan toan voi the thong tin glass phia tren.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(35.dp),
            color = if (colors.isDark) Color(0xFF030710) else colors.card,
            border = BorderStroke(1.dp, colors.purple.copy(alpha = 0.64f)),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Rounded.Home,
                    label = "Trang chủ",
                    selected = selectedScreen == MusicScreen.HOME,
                    onClick = onHome,
                    edge = NavEnd.START,
                    modifier = Modifier.weight(1f)
                )
                BottomControlIcon(
                    icon = Icons.Rounded.SkipPrevious,
                    description = "Bài trước",
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.weight(1.12f))
                BottomControlIcon(
                    icon = Icons.Rounded.SkipNext,
                    description = "Bài tiếp theo",
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Rounded.QueueMusic,
                    label = "Thư viện",
                    selected = selectedScreen == MusicScreen.LIBRARY,
                    onClick = onLibrary,
                    edge = NavEnd.END,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Vong den len cao chiem phan khuyet tren mini-player, nhung KHONG
        // noi nen mini-player voi nen dock. Nut tim duoc dat thap hon vong
        // den 12dp, vua cham/lan vao phan khuyet ma khong che thanh tien trinh.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(96.dp)
                .background(
                    if (colors.isDark) Color(0xFF01040B) else colors.background,
                    CircleShape
                )
                .border(1.dp, colors.purple.copy(alpha = 0.24f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .size(80.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .shadow(if (playPressed) 10.dp else 20.dp, CircleShape)
                .background(Color(0xFF100C22), CircleShape)
                .padding(5.dp)
                .background(
                    Brush.linearGradient(
                        listOf(colors.purpleBright, colors.purple, colors.magenta)
                    ),
                    CircleShape
                )
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Tạm dừng" else "Phát",
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

private enum class NavEnd { START, END }

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    edge: NavEnd,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 540f)
    )
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else if (selected) 0.85f else 0f,
        animationSpec = tween(durationMillis = 250)
    )
    // Hai nut dau/cuoi om sat cung bo tron ngoai cua thanh menu.
    val itemShape = when (edge) {
        NavEnd.START -> RoundedCornerShape(topStart = 35.dp, bottomStart = 35.dp, topEnd = 16.dp, bottomEnd = 16.dp)
        NavEnd.END -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 35.dp, bottomEnd = 35.dp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(itemShape)
            .background(colors.purple.copy(alpha = 0.43f * glow), itemShape)
            .border(1.5.dp, colors.purpleBright.copy(alpha = 0.80f * glow), itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else colors.textSecondary,
                modifier = Modifier
                    .size(25.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) Color.White else colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .background(
                    if (selected) Color(0xFFEEBEFF) else Color.Transparent,
                    CircleShape
                )
        )
    }
}

@Composable
private fun BottomControlIcon(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 620f)
    )

    Box(
        modifier = modifier
            .height(62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (pressed) colors.purple.copy(alpha = 0.16f) else Color.Transparent,
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (pressed) colors.purpleBright else colors.textPrimary,
            modifier = Modifier.size(if (pressed) 24.dp else 26.dp)
        )
    }
}

@Composable
internal fun EmptyLibraryState(
    title: String,
    description: String,
    buttonText: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(70.dp),
            shape = CircleShape,
            color = colors.purple.copy(alpha = 0.16f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = colors.purpleBright,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(description, color = colors.textSecondary, fontSize = 13.sp)
        Button(
            onClick = onButton,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.purple),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun sourceIcon(source: LibrarySource): ImageVector = when (source) {
    LibrarySource.LOCAL -> Icons.Rounded.Smartphone
    LibrarySource.PERSONAL_DRIVE -> Icons.Rounded.Cloud
    LibrarySource.PUBLIC_DRIVE -> Icons.Rounded.Link
    LibrarySource.DOWNLOADED -> Icons.Rounded.Download
    LibrarySource.FAVORITES -> Icons.Rounded.Favorite
    LibrarySource.ALL -> Icons.Rounded.MusicNote
}

@Composable
internal fun HorizontalChips(
    items: List<Pair<String, Boolean>>,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMusicColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEachIndexed { index, (label, selected) ->
            Surface(
                modifier = Modifier.clickable { onClick(index) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) colors.purple else colors.cardRaised,
                border = BorderStroke(1.dp, if (selected) colors.purpleBright else colors.border)
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp)
                )
            }
        }
    }
}
