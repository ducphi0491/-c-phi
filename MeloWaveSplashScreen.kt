package duc_phi.music

import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * MeloWave launch animation (variant 1): dark neon, real app icon and animated
 * sound bars. Runs only on a cold Activity launch; no video/GIF/network request.
 * The regular music screen initializes behind it and is revealed without
 * recreating MusicController or interrupting audio.
 */
@Composable
fun MeloWaveLaunchSplash(onFinished: () -> Unit) {
    val latestOnFinished by rememberUpdatedState(onFinished)
    val opacity = remember { Animatable(1f) }
    val iconScale = remember { Animatable(0.88f) }
    val pulse by rememberInfiniteTransition(label = "MeloWave opening sound")
        .animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1700, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Animated wave phase"
        )

    LaunchedEffect(Unit) {
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }
        // Allow users to see the animated logo and sound waves before entering the app.
        delay(2_600L)
        opacity.animateTo(targetValue = 0f, animationSpec = tween(260))
        latestOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = opacity.value }
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050916), Color(0xFF0B0920), Color(0xFF040C1C))
                )
            )
            // Block touches from going through to the player while the splash is visible.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val glowRadius = size.width * 0.74f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x663C1B9D), Color(0x253A1F84), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.41f),
                    radius = glowRadius
                ),
                center = Offset(size.width / 2f, size.height * 0.41f),
                radius = glowRadius
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2B005CC4), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.82f),
                    radius = size.width * 0.84f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.82f),
                radius = size.width * 0.84f
            )
        }

        // Soft flowing waves at the bottom, rather than moving images or video.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .align(Alignment.BottomCenter)
        ) {
            val width = size.width
            val height = size.height
            repeat(3) { layer ->
                val baseline = height * (0.32f + layer * 0.21f)
                val amplitude = height * (0.075f + layer * 0.018f)
                val path = Path().apply {
                    moveTo(0f, baseline)
                    for (step in 1..80) {
                        val x = width * step / 80f
                        val angle = (x / width * (2.2f + layer * 0.27f) * PI.toFloat() +
                            pulse * (0.42f - layer * 0.11f)).toDouble()
                        lineTo(x, baseline + sin(angle).toFloat() * amplitude)
                    }
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                val tint = when (layer) {
                    0 -> Color(0xFF7A1BF2)
                    1 -> Color(0xFF177BFF)
                    else -> Color(0xFFDB2AC9)
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(tint.copy(alpha = 0.25f), tint.copy(alpha = 0.02f))
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = iconScale.value
                    scaleY = iconScale.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 2.5.dp.toPx()
                    val ringInset = 10.dp.toPx()
                    drawCircle(
                        color = Color(0x503C56EE),
                        radius = min(size.width, size.height) / 2f - ringInset,
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFFDF40EA),
                                Color(0xFF9351FA),
                                Color(0xFF06BCF4),
                                Color(0x00A855F7)
                            )
                        ),
                        startAngle = pulse * 180f / PI.toFloat(),
                        sweepAngle = 103f,
                        useCenter = false,
                        topLeft = Offset(ringInset, ringInset),
                        size = Size(size.width - ringInset * 2f, size.height - ringInset * 2f),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .shadow(18.dp, RoundedCornerShape(34.dp), clip = false)
                        .clip(RoundedCornerShape(34.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF200B48), Color(0xFF090D2B), Color(0xFF07182D))
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFFA44AFF), Color(0xFF297FE9))
                            ),
                            shape = RoundedCornerShape(34.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val context = LocalContext.current
                    // ImageView handles adaptive and bitmap launcher icons alike.
                    val appIcon = remember(context) {
                        runCatching {
                            context.packageManager.getApplicationIcon(context.packageName)
                        }.getOrNull()
                    }
                    if (appIcon != null) {
                        AndroidView(
                            factory = { viewContext ->
                                ImageView(viewContext).apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageDrawable(appIcon)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "MeloWave",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFF7ADFB), Color(0xFFC16AFE), Color(0xFF4BD6F9))
                    ),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp
                )
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Âm nhạc của bạn",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCDD1E9),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Audio spectrum. Each bar varies with the same animation phase but a
            // different offset, producing continuous, non-repeating-looking motion.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.53f)
                    .height(49.dp)
            ) {
                val bars = 29
                val spacing = size.width / (bars + 1)
                val centerY = size.height / 2f
                val maxAmplitude = size.height * 0.84f
                val strokePx = 3.3.dp.toPx()
                val glow = Brush.verticalGradient(
                    listOf(Color(0xFFDE43F1), Color(0xFF8660FA), Color(0xFF24C7FC))
                )
                repeat(bars) { bar ->
                    val side = abs(bar - (bars - 1) / 2f) / ((bars - 1) / 2f)
                    val envelope = 1f - 0.78f * side
                    val motion = abs(
                        sin((pulse * 1.55f + bar * 0.57f).toDouble()) *
                            sin((pulse * 0.64f - bar * 0.19f).toDouble())
                    ).toFloat()
                    val heightPx = 5.dp.toPx() + maxAmplitude * envelope * (0.19f + 0.79f * motion)
                    val x = spacing * (bar + 1)
                    drawLine(
                        brush = glow,
                        start = Offset(x, centerY - heightPx / 2f),
                        end = Offset(x, centerY + heightPx / 2f),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
