package ducphi.tunnerinstrument

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.SparseArray
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import okio.sink
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.sqrt

// ----------------------------------------------------------------------------
// DATA CLASS & HÀM HỖ TRỢ
// ----------------------------------------------------------------------------
data class ChordEvent(val timeMs: Long, val chord: String)
data class LyricEvent(val startTimeMs: Long, val endTimeMs: Long, val text: String)
data class GridCellData(val displayText: String, val startTimeMs: Long, val endTimeMs: Long)
data class MeasureRowData(val measureIndex: Int, val cells: List<GridCellData>)
data class ChordBlock(val chord: String, val startMs: Long, val endMs: Long)

fun frequencyToNote(frequency: Float): String {
    if (frequency < 20f || frequency > 8000f) return ""
    val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val pitchNum = Math.round(12 * (Math.log(frequency / 440.0) / Math.log(2.0)) + 69).toInt()
    if (pitchNum < 0) return ""
    val octave = (pitchNum / 12) - 1
    val noteName = notes[pitchNum % 12]
    return "$noteName$octave"
}

fun transposeChord(chord: String, offset: Int): String {
    if (offset == 0 || chord.length > 4 || chord.contains(" ")) return chord
    val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val rootEndIndex = if (chord.length > 1 && (chord[1] == '#' || chord[1] == 'b')) 2 else 1
    val rootNote = chord.substring(0, rootEndIndex)
    val suffix = chord.substring(rootEndIndex)
    val index = notes.indexOf(rootNote)
    if (index == -1) return chord
    var newIndex = (index + offset) % 12
    if (newIndex < 0) newIndex += 12
    return notes[newIndex] + suffix
}

fun generateChordSheet(chords: List<ChordEvent>, lyrics: List<LyricEvent>, bpm: Int = 0, timeSignature: String = "4/4"): String {
    val result = StringBuilder()
    var lastWrittenChord = ""
    var lastChordTime = 0L
    var lastLyricEndTime = 0L
    val beatDurationMs = if (bpm > 0) 60000L / bpm else 500L

    for (lyric in lyrics) {
        if (lyric.text.contains("[âm nhạc]", ignoreCase = true) || lyric.text.contains("[Vỗ tay]", ignoreCase = true)) continue
        val durationMs = lyric.endTimeMs - lyric.startTimeMs
        if (durationMs <= 0) continue

        val chordsInScope = chords.filter { it.timeMs > lastLyricEndTime && it.timeMs <= lyric.endTimeMs }
        val validChords = mutableListOf<ChordEvent>()
        for (chord in chordsInScope) {
            if (chord.chord != lastWrittenChord || (chord.timeMs - lastChordTime) >= 2500L) {
                if ((chord.timeMs - lastChordTime) >= 600L) {
                    validChords.add(chord)
                    lastWrittenChord = chord.chord
                    lastChordTime = chord.timeMs
                }
            }
        }

        lastLyricEndTime = lyric.endTimeMs
        if (validChords.isEmpty()) {
            result.append("${lyric.text}\n\n")
            continue
        }

        val originalWords = lyric.text.replace("\n", " ").split(" ").filter { it.isNotBlank() }
        val wordsWithChords = originalWords.toMutableList()
        var insertedCount = 0
        val introChords = validChords.filter { it.timeMs < lyric.startTimeMs }
        val inlineChords = validChords.filter { it.timeMs >= lyric.startTimeMs }

        for (chord in inlineChords) {
            val relativeChordTime = chord.timeMs - lyric.startTimeMs
            val ratio = if (relativeChordTime <= beatDurationMs) 0f
            else if (lyric.endTimeMs - chord.timeMs <= beatDurationMs * 1.5) 1f
            else {
                val quantizedTimeMs = (Math.round(relativeChordTime.toDouble() / beatDurationMs) * beatDurationMs).toLong()
                val finalTimeMs = quantizedTimeMs.coerceIn(0L, durationMs)
                finalTimeMs.toFloat() / durationMs
            }

            var baseIndex = (ratio * originalWords.size).toInt()
            if (baseIndex < 0) baseIndex = 0
            if (baseIndex >= originalWords.size && originalWords.isNotEmpty()) baseIndex = originalWords.size - 1

            val insertIndex = baseIndex + insertedCount
            wordsWithChords.add(insertIndex, "[${chord.chord}]")
            insertedCount++
        }

        if (introChords.isNotEmpty()) {
            val introString = introChords.joinToString(" ") { "[${it.chord}]" }
            result.append(introString).append("\n")
        }
        result.append(wordsWithChords.joinToString(" ")).append("\n\n")
    }

    return result.toString()
}

// ============================================================================
// 2. VIEWMODEL
// ============================================================================
class ChordViewModel : ViewModel() {
    private val _liveChord = MutableStateFlow("Đang nạp lõi...")
    val liveChord: StateFlow<String> = _liveChord

    private val _liveLyric = MutableStateFlow("")
    val liveLyric: StateFlow<String> = _liveLyric

    private val _nextLyric = MutableStateFlow("")
    val nextLyric: StateFlow<String> = _nextLyric

    private val _lyricProgress = MutableStateFlow(0f)
    val lyricProgress: StateFlow<Float> = _lyricProgress

    private val _detectedTone = MutableStateFlow("")
    val detectedTone: StateFlow<String> = _detectedTone

    private val _detectedBPM = MutableStateFlow(0)
    val detectedBPM: StateFlow<Int> = _detectedBPM

    private val _detectedTimeSignature = MutableStateFlow("")
    val detectedTimeSignature: StateFlow<String> = _detectedTimeSignature

    private val _detectedRhythm = MutableStateFlow("")
    val detectedRhythm: StateFlow<String> = _detectedRhythm

    private val _transposeOffset = MutableStateFlow(0)
    val transposeOffset: StateFlow<Int> = _transposeOffset

    private val _fullChordSheet = MutableStateFlow("")
    val fullChordSheet: StateFlow<String> = _fullChordSheet

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _isCoreReady = MutableStateFlow(false)
    val isCoreReady: StateFlow<Boolean> = _isCoreReady

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentPosMs = MutableStateFlow(0L)
    val currentPosMs: StateFlow<Long> = _currentPosMs

    private val _chordGrid = MutableStateFlow<List<MeasureRowData>>(emptyList())
    val chordGrid: StateFlow<List<MeasureRowData>> = _chordGrid

    private val _currentVideoId = MutableStateFlow("")
    val currentVideoId: StateFlow<String> = _currentVideoId

    private val _viewMode = MutableStateFlow("GRID")
    val viewMode: StateFlow<String> = _viewMode

    private val _isDownloadLyricsEnabled = MutableStateFlow(true)
    val isDownloadLyricsEnabled: StateFlow<Boolean> = _isDownloadLyricsEnabled

    private val _waveform = MutableStateFlow<List<Pair<Long, Float>>>(emptyList())
    val waveform: StateFlow<List<Pair<Long, Float>>> = _waveform

    var chordTimeline: List<ChordEvent> = emptyList()
    var lyricTimeline: List<LyricEvent> = emptyList()
    var maxSongDuration: Long = 1L

    fun setViewMode(mode: String) {
        _viewMode.value = mode
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == "GRID") "WAVE" else "GRID"
    }

    fun setDownloadLyrics(enabled: Boolean) {
        _isDownloadLyricsEnabled.value = enabled
    }

    fun resetSession(context: Context) {
        stopAnalysis(context)
        _currentVideoId.value = ""
        _isPlaying.value = false
        _isProcessing.value = false
        _lyricProgress.value = 0f
    }

    fun initCoreSystem(context: Context) {
        _liveChord.value = "Hệ thống AI Cloud sẵn sàng."
        _isCoreReady.value = true
    }

    fun startAnalysis(context: Context, inputUrl: String, fetchLyrics: Boolean) {
        val videoId = parseVideoId(inputUrl)
        if (videoId.isEmpty()) {
            _liveChord.value = "Lỗi: Link không hợp lệ!"
            return
        }

        _currentVideoId.value = videoId
        _isProcessing.value = true
        _isPlaying.value = false
        _liveChord.value = "Đang tải dữ liệu từ YouTube..."
        _liveLyric.value = ""
        _lyricProgress.value = 0f
        _detectedTone.value = ""
        _detectedBPM.value = 0
        _detectedTimeSignature.value = ""
        _detectedRhythm.value = ""
        _transposeOffset.value = 0
        _fullChordSheet.value = ""
        _currentPosMs.value = 0L
        _chordGrid.value = emptyList()
        _waveform.value = emptyList()
        chordTimeline = emptyList()
        lyricTimeline = emptyList()
        maxSongDuration = 1L

        AudioAnalyzerService.onStatusChanged = { status -> _liveChord.value = status }

        // BẮT DỮ LIỆU JSON TỪ PYTHON TRẢ VỀ
        AudioAnalyzerService.onAnalysisFinished = { parsedGrid, flatChords, lyrics, bpm, timeSig, rhythm, durationMs, simplifiedWaveform, tone ->
            chordTimeline = flatChords
            lyricTimeline = lyrics
            _chordGrid.value = parsedGrid
            _detectedTone.value = tone
            _detectedBPM.value = bpm
            _detectedTimeSignature.value = timeSig
            _detectedRhythm.value = rhythm
            _waveform.value = simplifiedWaveform

            val generatedSheet = generateChordSheet(flatChords, lyrics, bpm, timeSig)
            _fullChordSheet.value = generatedSheet

            _isProcessing.value = false
            _liveChord.value = "Phân tích xong"

            if (!fetchLyrics) {
                _liveLyric.value = "Chế độ tải lời đang bị tắt."
            } else {
                _liveLyric.value = if (lyrics.isNotEmpty()) "Đã tải xong lời bài hát." else "Video này không có lời (Subtitles)"
            }

            val fallbackDuration = (flatChords.lastOrNull()?.timeMs ?: 0L) + 5000L
            maxSongDuration = if (durationMs > 0L) durationMs else fallbackDuration
        }

        val intent = Intent(context, AudioAnalyzerService::class.java).apply {
            action = "START_DOWNLOAD_AND_ANALYZE"
            putExtra("VIDEO_ID", videoId)
            putExtra("FETCH_LYRICS", fetchLyrics)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun setTranspose(offset: Int) {
        _transposeOffset.value = offset
    }

    fun syncStateWithWebView(timeMs: Long, isPlayingStatus: Boolean) {
        _currentPosMs.value = timeMs
        _isPlaying.value = isPlayingStatus

        val activeChordEvent = chordTimeline.lastOrNull { it.timeMs <= timeMs }
        if (activeChordEvent != null) {
            _liveChord.value = activeChordEvent.chord
        }

        val activeLyricEvent = lyricTimeline.lastOrNull { timeMs >= it.startTimeMs && timeMs <= it.endTimeMs }

        val rawText = activeLyricEvent?.text ?: ""
        val processedText = if (rawText.contains("\n")) {
            rawText.split("\n").last().trim()
        } else {
            rawText.trim()
        }
        _liveLyric.value = processedText

        var foundNextLyric = ""
        var lastEndTimeOfCurrentText = activeLyricEvent?.endTimeMs ?: timeMs

        for (event in lyricTimeline) {
            if (event.startTimeMs > timeMs) {
                val rawNextText = event.text
                val processedNextText = if (rawNextText.contains("\n")) {
                    rawNextText.split("\n").last().trim()
                } else {
                    rawNextText.trim()
                }

                if (processedNextText.isNotEmpty()) {
                    if (processedNextText != processedText) {
                        foundNextLyric = processedNextText
                        break
                    } else {
                        val timeGap = event.startTimeMs - lastEndTimeOfCurrentText
                        if (timeGap > 1500L) {
                            foundNextLyric = processedNextText
                            break
                        } else {
                            lastEndTimeOfCurrentText = maxOf(lastEndTimeOfCurrentText, event.endTimeMs)
                        }
                    }
                }
            }
        }

        _nextLyric.value = foundNextLyric

        if (activeLyricEvent != null) {
            val durationMs = activeLyricEvent.endTimeMs - activeLyricEvent.startTimeMs
            if (durationMs > 0) {
                _lyricProgress.value = ((timeMs - activeLyricEvent.startTimeMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else {
                _lyricProgress.value = 1f
            }
        } else {
            _lyricProgress.value = 0f
        }
    }

    fun stopAnalysis(context: Context) {
        _isProcessing.value = false
        _currentVideoId.value = ""
        _liveChord.value = "Hệ thống sẵn sàng."
        _liveLyric.value = ""
        _lyricProgress.value = 0f
        context.stopService(Intent(context, AudioAnalyzerService::class.java))
    }

    fun parseVideoId(input: String): String {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*".toRegex()
        return pattern.find(input)?.value ?: ""
    }
}

// ============================================================================
// 3. UI COMPOSABLES
// ============================================================================

@Composable
fun TopNavigationBar(
    tone: String,
    bpm: Int,
    timeSig: String,
    rhythm: String,
    transposeOffset: Int,
    onTransposeChange: (Int) -> Unit,
    onHomeClick: () -> Unit,
    isDarkMode: Boolean,
    currentLang: String
) {
    val bgColor = if (isDarkMode) Color(0xFF162032) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    val dividerColor = if (isDarkMode) Color.DarkGray else Color.LightGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(bgColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onHomeClick) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = textColor)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = CoreTranslator.getString("tone", currentLang), color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "♭",
                        color = Color(0xFF38BDF8),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onTransposeChange(transposeOffset - 1) }
                            .padding(horizontal = 8.dp)
                    )

                    val displayTone = if (tone.isNotEmpty()) transposeChord(tone, transposeOffset) else "--"
                    Text(
                        text = displayTone,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "♯",
                        color = Color(0xFF38BDF8),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .clickable { onTransposeChange(transposeOffset + 1) }
                            .padding(horizontal = 8.dp)
                    )
                }
            }

            Box(modifier = Modifier.height(24.dp).width(1.dp).background(dividerColor))
            InfoColumn(title = CoreTranslator.getString("bpm", currentLang), value = if (bpm > 0) bpm.toString() else "--", isDarkMode = isDarkMode)

            Box(modifier = Modifier.height(24.dp).width(1.dp).background(dividerColor))
            InfoColumn(title = CoreTranslator.getString("time_sig", currentLang), value = timeSig, isDarkMode = isDarkMode)

            Box(modifier = Modifier.height(24.dp).width(1.dp).background(dividerColor))
            val rhythmTitle = if (currentLang == "vi") "Điệu" else "Rhythm"
            InfoColumn(title = rhythmTitle, value = rhythm, isDarkMode = isDarkMode)
        }
    }
}

@Composable
private fun InfoColumn(title: String, value: String, isDarkMode: Boolean) {
    val textColor = if (isDarkMode) Color.White else Color.Black

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            text = if (value.isNotEmpty()) value else "--",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChordGridDynamicColumns(chordGrid: List<MeasureRowData>, currentPosMs: Long, transposeOffset: Int, timeSignature: String, isDarkMode: Boolean) {
    if (chordGrid.isEmpty()) return

    val numCols = when {
        timeSignature.startsWith("3") -> 3
        timeSignature.startsWith("6") -> 6
        else -> 4
    }

    val allCells = remember(chordGrid) { chordGrid.flatMap { it.cells } }
    val gridState = rememberLazyGridState()
    var previousActiveIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(currentPosMs) {
        val activeIndex = allCells.indexOfFirst { currentPosMs >= it.startTimeMs && currentPosMs < it.endTimeMs }
        if (activeIndex != -1 && activeIndex != previousActiveIndex) {
            previousActiveIndex = activeIndex
            val targetIndex = (activeIndex - numCols).coerceAtLeast(0)
            gridState.animateScrollToItem(targetIndex)
        }
    }

    val activeColor = if (isDarkMode) Color(0xFF4B5563) else Color(0xFFFFEBEE)
    val inactiveColor = if (isDarkMode) Color(0xFF2C2D31) else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val progressColor = if (isDarkMode) Color(0xFF38BDF8) else Color.Red

    LazyVerticalGrid(
        columns = GridCells.Fixed(numCols),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(allCells.size) { index ->
            val cell = allCells[index]
            val isActive = currentPosMs >= cell.startTimeMs && currentPosMs < cell.endTimeMs
            val displayChord = if (cell.displayText.isNotEmpty()) transposeChord(cell.displayText, transposeOffset) else ""

            Box(
                modifier = Modifier
                    .height(52.dp)
                    .background(if (isActive) activeColor else inactiveColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isActive) {
                    Box(modifier = Modifier.fillMaxHeight().width(4.dp).background(progressColor).align(Alignment.CenterStart))
                }
                Text(
                    text = displayChord,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
fun WaveformView(
    waveform: List<Pair<Long, Float>>,
    chordTimeline: List<ChordEvent>,
    currentPosMs: Long,
    transposeOffset: Int,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean
) {
    val textMeasurer = rememberTextMeasurer()
    val bgColor = if (isDarkMode) Color.Black else Color(0xFFF0F2F5)
    val lineColor = if (isDarkMode) Color(0xFFD1D5DB) else Color(0xFF4B5563)
    val textColor = if (isDarkMode) Color.Black else Color.Black
    val labelBg = if (isDarkMode) Color(0xFFFFF8E7) else Color(0xFFFFFFFF)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(bgColor)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val centerHeight = canvasHeight * 0.65f

        val windowMs = 8000L
        val playheadX = canvasWidth / 2f

        if (waveform.isNotEmpty()) {
            val sortedEnergies = waveform.map { it.second }.sorted()
            val p95Energy = if (sortedEnergies.isNotEmpty()) sortedEnergies[(sortedEnergies.size * 0.95).toInt()].coerceAtLeast(0.1f) else 1f

            for (point in waveform) {
                val time = point.first
                val energy = point.second

                val timeDiff = time - currentPosMs
                val x = playheadX + (timeDiff.toFloat() / windowMs.toFloat()) * canvasWidth

                if (x in 0f..canvasWidth) {
                    val ratio = (energy / p95Energy * 1.6).coerceIn(0.0, 1.2)
                    val normalizedEnergy = sqrt(ratio).toFloat()

                    val maxBarHeight = canvasHeight * 0.6f
                    val barHeight = (normalizedEnergy * maxBarHeight).coerceAtMost(maxBarHeight)

                    val yStart = centerHeight - (barHeight / 2)
                    val yEnd = centerHeight + (barHeight / 2)

                    drawLine(
                        color = lineColor,
                        start = Offset(x, yStart),
                        end = Offset(x, yEnd),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        for (chordEvent in chordTimeline) {
            val timeDiff = chordEvent.timeMs - currentPosMs
            val x = playheadX + (timeDiff.toFloat() / windowMs.toFloat()) * canvasWidth

            if (x in -50f..(canvasWidth + 50f)) {
                val displayChord = transposeChord(chordEvent.chord, transposeOffset)
                if (displayChord.isNotEmpty()) {
                    val textLayoutResult = textMeasurer.measure(
                        text = displayChord,
                        style = TextStyle(color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    val padding = 16f
                    val rectWidth = textLayoutResult.size.width + padding * 2
                    val rectHeight = textLayoutResult.size.height + padding

                    val rectTop = canvasHeight * 0.05f
                    val rectLeft = x

                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(x, rectTop + rectHeight),
                        end = Offset(x, centerHeight),
                        strokeWidth = 2.dp.toPx()
                    )

                    drawRoundRect(
                        color = labelBg,
                        topLeft = Offset(rectLeft, rectTop),
                        size = Size(rectWidth, rectHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        color = textColor,
                        topLeft = Offset(rectLeft + padding, rectTop + padding / 2)
                    )
                }
            }
        }

        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, canvasHeight),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun YoutubeLogoIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Red, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MiniWaveformIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.12f
        val space = w * 0.22f
        val startX = (w - (space * 4)) / 2f
        val heights = listOf(h * 0.4f, h * 0.8f, h * 0.5f, h * 1f, h * 0.3f)

        for (i in 0..4) {
            val x = startX + i * space
            val barH = heights[i]
            drawLine(
                color = color,
                start = Offset(x, (h - barH) / 2f),
                end = Offset(x, (h + barH) / 2f),
                strokeWidth = barW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun MiddleControlBar(
    isLyricsEnabled: Boolean,
    onLyricsToggle: (Boolean) -> Unit,
    onYoutubeIconClick: () -> Unit,
    onGridViewToggle: () -> Unit,
    viewMode: String,
    isDarkMode: Boolean,
    currentLang: String
) {
    val bgColor = if (isDarkMode) Color(0xFF162032) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF1B1C20)
    val dividerColor = if (isDarkMode) Color.DarkGray else Color.LightGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(bgColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable { onYoutubeIconClick() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(CoreTranslator.getString("youtube", currentLang), color = textColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            YoutubeLogoIcon(modifier = Modifier.size(width = 30.dp, height = 18.dp))
        }

        Divider(color = dividerColor, modifier = Modifier.fillMaxHeight(0.6f).width(1.dp))

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(CoreTranslator.getString("lyrics", currentLang), color = textColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Switch(
                checked = isLyricsEnabled,
                onCheckedChange = onLyricsToggle,
                modifier = Modifier.scale(0.6f).height(24.dp)
            )
        }

        Divider(color = dividerColor, modifier = Modifier.fillMaxHeight(0.6f).width(1.dp))

        Column(modifier = Modifier.weight(1f).clickable { onGridViewToggle() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(if (viewMode == "GRID") CoreTranslator.getString("wave_view", currentLang) else CoreTranslator.getString("grid_view", currentLang), color = textColor, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (viewMode == "GRID") {
                MiniWaveformIcon(modifier = Modifier.size(20.dp), color = textColor)
            } else {
                Icon(
                    Icons.Default.GridView,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AI_FretboardDiagram(
    chordName: String,
    frets: List<Int>,
    fingers: List<Int> = emptyList(),
    baseFret: Int,
    numStrings: Int,
    textColor: Color,
    dotColor: Color
) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = chordName, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(20.dp))

        Box(contentAlignment = Alignment.TopStart) {
            Canvas(modifier = Modifier.width(100.dp).height(120.dp)) {
                val w = size.width
                val h = size.height
                val numFrets = 4
                val stringSpacing = w / (numStrings - 1)
                val fretSpacing = h / numFrets
                val dotRadius = stringSpacing * 0.45f

                for (i in 0..numFrets) {
                    val y = i * fretSpacing
                    val isNut = (i == 0 && baseFret == 1)
                    drawLine(
                        color = if (isNut) textColor else textColor.copy(alpha = 0.5f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (isNut) 12f else 3f
                    )
                }

                for (i in 0 until numStrings) {
                    val x = i * stringSpacing
                    drawLine(
                        color = textColor.copy(alpha = 0.8f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 3f
                    )
                }

                val pressedFingers = fingers.filterIndexed { index, finger -> finger == 1 && frets.getOrNull(index) ?: 0 > 0 }
                if (pressedFingers.size > 1) {
                    val barreFretIndex = frets.indices.firstOrNull { fingers.getOrNull(it) == 1 && frets[it] > 0 } ?: -1
                    if (barreFretIndex != -1) {
                        val barreFret = frets[barreFretIndex]

                        val minString = frets.indices.firstOrNull { frets[it] == barreFret && fingers.getOrNull(it) == 1 } ?: -1
                        val maxString = frets.indices.lastOrNull { frets[it] == barreFret && fingers.getOrNull(it) == 1 } ?: -1

                        if (minString != -1 && maxString > minString) {
                            val startX = minString * stringSpacing
                            val endX = maxString * stringSpacing
                            val y = (barreFret - 0.5f) * fretSpacing

                            drawLine(
                                color = dotColor,
                                start = Offset(startX, y),
                                end = Offset(endX, y),
                                strokeWidth = dotRadius * 1.8f,
                                cap = StrokeCap.Round
                            )

                            drawContext.canvas.nativeCanvas.drawText(
                                "1",
                                startX,
                                y - ((textPaint.descent() + textPaint.ascent()) / 2),
                                textPaint
                            )
                        }
                    }
                }

                frets.forEachIndexed { stringIndex, fretPosition ->
                    val x = stringIndex * stringSpacing

                    when {
                        fretPosition == -1 -> {
                            val xSize = dotRadius * 0.6f
                            val topY = -fretSpacing * 0.5f
                            drawLine(textColor, Offset(x - xSize, topY - xSize), Offset(x + xSize, topY + xSize), 4f, StrokeCap.Round)
                            drawLine(textColor, Offset(x + xSize, topY - xSize), Offset(x - xSize, topY + xSize), 4f, StrokeCap.Round)
                        }
                        fretPosition == 0 -> {
                            drawCircle(
                                color = textColor,
                                radius = dotRadius * 0.6f,
                                center = Offset(x, -fretSpacing * 0.5f),
                                style = Stroke(width = 4f)
                            )
                        }
                        fretPosition > 0 -> {
                            val fingerNum = fingers.getOrNull(stringIndex) ?: 0
                            val y = (fretPosition - 0.5f) * fretSpacing

                            val isPartOfBarre = fingerNum == 1 && frets.indices.count { frets[it] == fretPosition && fingers.getOrNull(it) == 1 } > 1

                            if (!isPartOfBarre) {
                                drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))

                                if (fingerNum > 0) {
                                    drawContext.canvas.nativeCanvas.drawText(
                                        fingerNum.toString(),
                                        x,
                                        y - ((textPaint.descent() + textPaint.ascent()) / 2),
                                        textPaint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (baseFret > 1) {
                Text(
                    text = "${baseFret}fr",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(x = 112.dp, y = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ChordFretboardCard(
    chordStr: String,
    dictionary: List<ChordModel>,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF1A365D),
    isDarkMode: Boolean = true
) {
    val diagramTextColor = if (isDarkMode) Color.White else Color.Black
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedContent(
            targetState = chordStr,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(300))
                )
            },
            label = "chord_slide_animation"
        ) { targetChord ->
            if (targetChord.isNotEmpty() && !targetChord.contains("Lỗi") && !targetChord.contains("Đang")) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    val match = Regex("^([A-G][#b]?)(.*)").find(targetChord)
                    if (match != null) {
                        val rawKey = match.groupValues[1]
                        val rawSuf = match.groupValues[2]
                        val searchSuffix = when (rawSuf) { "" -> "major"; "m" -> "minor"; else -> rawSuf }

                        val currentDictionaryChord = dictionary.find { it.key == rawKey && it.suffix == searchSuffix }
                        if (currentDictionaryChord != null && currentDictionaryChord.positions.isNotEmpty()) {
                            val position = currentDictionaryChord.positions.first()
                            AI_FretboardDiagram(
                                chordName = targetChord,
                                frets = position.frets,
                                fingers = position.fingers,
                                baseFret = position.baseFret,
                                numStrings = 6,
                                textColor = diagramTextColor,
                                dotColor = Color(0xFF15B27D)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomPlayerSection(
    webViewRef: WebView,
    isPlaying: Boolean,
    currentPosMs: Long,
    maxDurationMs: Long,
    transposeOffset: Int,
    onTransposeChange: (Int) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeek: (Float) -> Unit,
    isDarkMode: Boolean,
    currentLang: String
) {
    val context = LocalContext.current
    val bgColor = if (isDarkMode) Color(0xFF1A2235) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    var localProgress by remember(currentPosMs, maxDurationMs) {
        mutableStateOf(if (maxDurationMs > 0) (currentPosMs.toFloat() / maxDurationMs.toFloat()).coerceIn(0f, 1f) else 0f)
    }

    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var loopState by remember { mutableStateOf(0) }
    var loopStartMs by remember { mutableStateOf(0L) }
    var loopEndMs by remember { mutableStateOf(0L) }
    var isPitchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosMs) {
        if (loopState == 3 && currentPosMs >= loopEndMs) {
            val seekTimeSec = loopStartMs / 1000f
            webViewRef.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) v.currentTime = $seekTimeSec;", null)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(bgColor)
    ) {
        Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webViewRef }
            )
        }

        Column(
            modifier = Modifier.weight(0.55f).fillMaxHeight().padding(4.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPitchExpanded) {
                        Text(
                            text = "♭",
                            color = Color(0xFF38BDF8),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onTransposeChange(transposeOffset - 1) }
                                .padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }

                    Text(
                        text = if (transposeOffset == 0) CoreTranslator.getString("pitch", currentLang) else "${CoreTranslator.getString("pitch", currentLang)} ${if (transposeOffset > 0) "+$transposeOffset" else transposeOffset}",
                        color = if (transposeOffset == 0) Color.Gray else Color(0xFFFDE047),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { isPitchExpanded = !isPitchExpanded }
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                    )

                    if (isPitchExpanded) {
                        Text(
                            text = "♯",
                            color = Color(0xFF38BDF8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onTransposeChange(transposeOffset + 1) }
                                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
                        )
                    }
                }

                Text(
                    text = if (playbackSpeed == 1.0f) CoreTranslator.getString("speed", currentLang) else "${CoreTranslator.getString("speed", currentLang)}: ${playbackSpeed}x",
                    color = if (playbackSpeed == 1.0f) Color.Gray else Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        val speeds = listOf(1.0f, 1.25f, 1.5f, 0.5f, 0.75f)
                        playbackSpeed = speeds[(speeds.indexOf(playbackSpeed) + 1) % speeds.size]
                        val script = "var v = document.getElementsByTagName('video')[0]; if(v) { v.playbackRate = $playbackSpeed; }"
                        webViewRef.evaluateJavascript(script, null)
                    }
                )

                val loopText = when (loopState) {
                    1 -> CoreTranslator.getString("loop_all", currentLang)
                    2 -> CoreTranslator.getString("loop_a", currentLang)
                    3 -> CoreTranslator.getString("loop_a_b", currentLang)
                    else -> CoreTranslator.getString("loop", currentLang)
                }
                val loopColor = when (loopState) {
                    1, 3 -> Color(0xFF38BDF8)
                    2 -> Color(0xFFFDE047)
                    else -> Color.Gray
                }

                Text(
                    text = loopText,
                    color = loopColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable {
                            when (loopState) {
                                0 -> {
                                    loopState = 1
                                    webViewRef.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) { v.loop = true; }", null)
                                    android.widget.Toast.makeText(context, CoreTranslator.getString("toast_loop_all", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                }
                                1 -> {
                                    loopStartMs = currentPosMs
                                    loopState = 2
                                    webViewRef.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) { v.loop = false; }", null)
                                    android.widget.Toast.makeText(context, CoreTranslator.getString("toast_loop_a", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                }
                                2 -> {
                                    if (currentPosMs > loopStartMs + 1000L) {
                                        loopEndMs = currentPosMs
                                        loopState = 3
                                        android.widget.Toast.makeText(context, CoreTranslator.getString("toast_loop_ab", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        loopState = 0
                                        android.widget.Toast.makeText(context, CoreTranslator.getString("toast_loop_short", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                                3 -> {
                                    loopState = 0
                                    android.widget.Toast.makeText(context, CoreTranslator.getString("toast_loop_cancel", currentLang), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            Slider(
                value = localProgress,
                onValueChange = { newProgress -> localProgress = newProgress },
                onValueChangeFinished = { onSeek(localProgress) },
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFDE047), activeTrackColor = Color(0xFFFDE047), inactiveTrackColor = Color.DarkGray),
                modifier = Modifier.height(20.dp).padding(horizontal = 8.dp)
            )

            val curSec = (localProgress * maxDurationMs).toLong() / 1000
            val maxSec = maxDurationMs / 1000
            Text(
                text = String.format("%d:%02d / %d:%02d", curSec / 60, curSec % 60, maxSec / 60, maxSec % 60),
                color = textColor, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.FastRewind, tint = textColor, contentDescription = "") }
                IconButton(onClick = onPlayPauseClick, modifier = Modifier.size(40.dp)) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, tint = textColor, contentDescription = "")
                }
                IconButton(onClick = {}, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.FastForward, tint = textColor, contentDescription = "") }
            }
        }
    }
}

// ============================================================================
// 4. MÀN HÌNH CHÍNH (CHORD SCREEN)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChordScreen(initialUrl: String = "", passedDarkMode: Boolean = true, passedLang: String = "en", viewModel: ChordViewModel = viewModel()) {
    val context = LocalContext.current

    val prefs = context.getSharedPreferences("TunerPrefs", Context.MODE_PRIVATE)
    val currentLang = prefs.getString("appLang", "en") ?: "en"
    val isDarkMode = prefs.getBoolean("isDarkMode", true)

    val liveChord by viewModel.liveChord.collectAsState()
    val liveLyric by viewModel.liveLyric.collectAsState()
    val nextLyric by viewModel.nextLyric.collectAsState()
    val lyricProgress by viewModel.lyricProgress.collectAsState()
    val transposeOffset by viewModel.transposeOffset.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isCoreReady by viewModel.isCoreReady.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val chordGrid by viewModel.chordGrid.collectAsState()
    val currentPosMs by viewModel.currentPosMs.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val isDownloadLyricsEnabled by viewModel.isDownloadLyricsEnabled.collectAsState()

    val detectedTone by viewModel.detectedTone.collectAsState()
    val detectedBPM by viewModel.detectedBPM.collectAsState()
    val detectedTimeSignature by viewModel.detectedTimeSignature.collectAsState()
    val detectedRhythm by viewModel.detectedRhythm.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val waveform by viewModel.waveform.collectAsState()

    var inputUrlOrId by remember { mutableStateOf(initialUrl) }
    var allChordsDictionary by remember { mutableStateOf<List<ChordModel>>(emptyList()) }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val webViewRef = remember {
        WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0"
            }
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean = true
            }
            webViewClient = object : WebViewClient() {
                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    if (url != null && url.contains("watch?v=")) inputUrlOrId = url
                }
            }
        }
    }

    val handleYoutubeClick = {
        viewModel.resetSession(context)
        inputUrlOrId = ""
        webViewRef.loadUrl("https://m.youtube.com")
    }

    val handleHomeClick: () -> Unit = {
        backDispatcher?.onBackPressed()
    }

    LaunchedEffect(currentVideoId) {
        if (currentVideoId.isNotEmpty()) {
            val embedUrl = "https://www.youtube.com/embed/$currentVideoId?autoplay=0&playsinline=1&fs=1&rel=0&showinfo=0&enablejsapi=1"
            val headers = mutableMapOf<String, String>()
            headers["Referer"] = "http://ducphi.atwebpages.com/"
            webViewRef.loadUrl(embedUrl, headers)
        } else {
            if (inputUrlOrId.isEmpty()) webViewRef.loadUrl("https://m.youtube.com")
            else if (!inputUrlOrId.contains("http")) webViewRef.loadUrl("https://m.youtube.com/results?search_query=${android.net.Uri.encode(inputUrlOrId)}")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initCoreSystem(context.applicationContext)
        try { allChordsDictionary = loadChordsFromAssets(context, "guitar.json") } catch (e: Exception) {}
    }

    LaunchedEffect(isCoreReady) {
        while (true) {
            if (currentVideoId.isEmpty() && webViewRef.url != null) {
                val currentWebUrl = webViewRef.url!!
                if (currentWebUrl.contains("watch?v=") && inputUrlOrId != currentWebUrl) inputUrlOrId = currentWebUrl
            }

            if (isCoreReady && currentVideoId.isNotEmpty()) {
                val script = """
                    (function() {
                        try {
                            var v = document.getElementsByTagName('video')[0];
                            if (!v) return 'null';
                            var adShowing = document.querySelector('.ad-showing') || document.querySelector('.ytp-ad-player-overlay');
                            if (adShowing) {
                                var skipBtn = document.querySelector('.ytp-ad-skip-button') || document.querySelector('.ytm-autonav-promo-close-button');
                                if (skipBtn) skipBtn.click();
                                else { v.playbackRate = 16.0; v.muted = true; }
                                return '0|true';
                            } else {
                                if (v.playbackRate === 16.0) { v.playbackRate = 1.0; v.muted = false; }
                            }
                            return v.currentTime + '|' + v.paused;
                        } catch(e) { return 'null'; }
                    })();
                """.trimIndent()

                webViewRef.evaluateJavascript(script) { result ->
                    if (result != null && result != "\"null\"" && result != "null") {
                        try {
                            val parts = result.replace("\"", "").split("|")
                            if (parts.size == 2) {
                                val timeMs = (parts[0].toFloat() * 1000).toLong()
                                val isPaused = parts[1].toBooleanStrictOrNull() ?: true
                                viewModel.syncStateWithWebView(timeMs, !isPaused)
                            }
                        } catch (e: Exception) {}
                    }
                }
            }
            delay(50)
        }
    }

    LaunchedEffect(inputUrlOrId, isCoreReady) {
        if (isCoreReady && inputUrlOrId.isNotEmpty()) {
            val extractedId = viewModel.parseVideoId(inputUrlOrId)
            if (extractedId.isNotEmpty() && extractedId != currentVideoId && !isProcessing) {
                webViewRef.evaluateJavascript("var v = document.getElementsByTagName('video')[0]; if(v) v.pause();", null)
                viewModel.startAnalysis(context, inputUrlOrId, isDownloadLyricsEnabled)
            }
        }
    }

    val isMiniMode = currentVideoId.isNotEmpty()

    val appBgColor = if (isDarkMode) Color(0xFF0D1117) else Color(0xFFF0F2F5)
    val appSurfaceColor = if (isDarkMode) Color(0xFF162032) else Color.White
    val appTextColor = if (isDarkMode) Color.White else Color.Black

    Box(
        modifier = Modifier.fillMaxSize().background(appBgColor)
    ) {
        if (isMiniMode) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopNavigationBar(
                    tone = detectedTone,
                    bpm = detectedBPM,
                    timeSig = detectedTimeSignature,
                    rhythm = detectedRhythm,
                    transposeOffset = transposeOffset,
                    onTransposeChange = { viewModel.setTranspose(it) },
                    onHomeClick = handleHomeClick,
                    isDarkMode = isDarkMode,
                    currentLang = currentLang
                )

                Box(modifier = Modifier.weight(1f)) {
                    if (isProcessing) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.Red,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            val statusText = when {
                                liveChord.contains("Đang") -> liveChord
                                liveChord.contains("Phân tích xong") -> CoreTranslator.getString("analysis_complete", currentLang)
                                liveChord.contains("Lỗi") -> liveChord
                                else -> CoreTranslator.getString("processing", currentLang)
                            }

                            Text(
                                text = statusText,
                                color = Color.Red,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }else {
                        if (viewMode == "GRID") {
                            ChordGridDynamicColumns(
                                chordGrid = chordGrid,
                                currentPosMs = currentPosMs,
                                transposeOffset = transposeOffset,
                                timeSignature = detectedTimeSignature,
                                isDarkMode = isDarkMode
                            )
                        } else {
                            WaveformView(
                                waveform = waveform,
                                chordTimeline = viewModel.chordTimeline,
                                currentPosMs = currentPosMs,
                                transposeOffset = transposeOffset,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }

                MiddleControlBar(
                    isLyricsEnabled = isDownloadLyricsEnabled,
                    onLyricsToggle = { viewModel.setDownloadLyrics(it) },
                    onYoutubeIconClick = handleYoutubeClick,
                    onGridViewToggle = { viewModel.toggleViewMode() },
                    viewMode = viewMode,
                    isDarkMode = isDarkMode,
                    currentLang = currentLang
                )

                if (isDownloadLyricsEnabled && (liveLyric.isNotEmpty() || nextLyric.isNotEmpty())) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(appBgColor)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (liveLyric.isNotEmpty()) {
                            val p = lyricProgress.coerceIn(0f, 1f)
                            val highlightColor = if (isDarkMode) Color.Cyan else Color.Red
                            val karaokeBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                0f to highlightColor,
                                p to highlightColor,
                                (p + 0.001f).coerceAtMost(1f) to appTextColor.copy(alpha = 0.5f),
                                1f to appTextColor.copy(alpha = 0.5f)
                            )

                            Text(
                                text = liveLyric,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(
                                    brush = karaokeBrush,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }

                        if (nextLyric.isNotEmpty()) {
                            Text(
                                text = nextLyric,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDarkMode) Color.LightGray.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                val activeIndex = viewModel.chordTimeline.indexOfLast { it.timeMs <= currentPosMs }
                val currentChordStr = if (activeIndex >= 0) transposeChord(viewModel.chordTimeline[activeIndex].chord, transposeOffset) else ""
                val nextChordStr = if (activeIndex >= 0 && activeIndex + 1 < viewModel.chordTimeline.size) transposeChord(viewModel.chordTimeline[activeIndex + 1].chord, transposeOffset) else ""

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(appSurfaceColor)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val leftBg = if (isDarkMode) Color(0xFF1A365D) else Color(0xFFE3F2FD)
                    val rightBg = if (isDarkMode) Color(0xFF2D3748) else Color(0xFFF5F5F5)

                    ChordFretboardCard(
                        chordStr = currentChordStr,
                        dictionary = allChordsDictionary,
                        modifier = Modifier.weight(1f),
                        bgColor = leftBg,
                        isDarkMode = isDarkMode
                    )
                    ChordFretboardCard(
                        chordStr = nextChordStr,
                        dictionary = allChordsDictionary,
                        modifier = Modifier.weight(1f),
                        bgColor = rightBg,
                        isDarkMode = isDarkMode
                    )
                }

                BottomPlayerSection(
                    webViewRef = webViewRef,
                    isPlaying = isPlaying,
                    currentPosMs = currentPosMs,
                    maxDurationMs = viewModel.maxSongDuration,
                    transposeOffset = transposeOffset,
                    onTransposeChange = { viewModel.setTranspose(it) },
                    onPlayPauseClick = {
                        val playPauseScript = """
                            (function() {
                                try {
                                    var action = ${if (isPlaying) "'pauseVideo'" else "'playVideo'"};
                                    window.postMessage(JSON.stringify({"event":"command","func":action,"args":""}), '*');
                                    var v = document.getElementsByTagName('video')[0];
                                    if (v) {
                                        if (v.ended) { v.currentTime = 0; v.play(); }
                                        else if (v.paused && !${isPlaying}) { v.play(); }
                                        else if (!v.paused && ${isPlaying}) { v.pause(); }
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()
                        webViewRef.evaluateJavascript(playPauseScript, null)
                    },
                    onSeek = { progress ->
                        val seekTimeSec = (progress * viewModel.maxSongDuration) / 1000f
                        val seekScript = """
                            (function() {
                                var v = document.getElementsByTagName('video')[0];
                                if (v) { v.currentTime = $seekTimeSec; }
                            })();
                        """.trimIndent()
                        webViewRef.evaluateJavascript(seekScript, null)
                    },
                    isDarkMode = isDarkMode,
                    currentLang = currentLang
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { webViewRef })
            }
        }
    }
}

// ============================================================================
// 5. BACKGROUND SERVICE & HTTP UPLOAD
// ============================================================================
class AudioAnalyzerService : Service() {
    private var isRunning = false
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    companion object {
        var onStatusChanged: ((String) -> Unit)? = null
        var onAnalysisFinished: ((List<MeasureRowData>, List<ChordEvent>, List<LyricEvent>, Int, String, String, Long, List<Pair<Long, Float>>, String) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        val channelId = "chord_download_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Xử lý âm thanh AI", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Chord App Core")
            .setContentText("Đang kết nối Máy chủ AI...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoId = intent?.getStringExtra("VIDEO_ID")
        val fetchLyrics = intent?.getBooleanExtra("FETCH_LYRICS", false) ?: false

        if (intent?.action == "START_DOWNLOAD_AND_ANALYZE" && videoId != null && !isRunning) {
            isRunning = true
            executeCloudWorkflow(videoId, fetchLyrics)
        }
        return START_NOT_STICKY
    }

    // ============================================================================
// HÀM 1: QUY TRÌNH CHÍNH (TẢI FILE & GỬI LÊN MÁY CHỦ PYTHON)
// ============================================================================
    private fun executeCloudWorkflow(videoId: String, fetchLyrics: Boolean) {
        val youtubeLink = "https://www.youtube.com/watch?v=$videoId"

        serviceScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // 1. BÓC TÁCH LINK BẰNG API PYTHON
                updateStatus("Đang nhờ máy chủ bóc tách luồng âm thanh...")
                val directAudioUrl = extractAudioUrl(youtubeLink, client)

                // 2. TẢI FILE VỀ CACHE ĐIỆN THOẠI
                updateStatus("Đang tải nhạc về thiết bị...")
                val audioFile = downloadAudioToCache(applicationContext, directAudioUrl, client)

                if (!audioFile.exists() || audioFile.length() == 0L) {
                    throw Exception("Lỗi tải nhạc: File tải về rỗng hoặc không tồn tại!")
                }

                // 3. UPLOAD FILE LÊN TÊN MIỀN CHÍNH THỨC CỦA ANH
                val fileSizeInMB = String.format("%.2f", audioFile.length() / (1024.0 * 1024.0))
                updateStatus("Đang gửi file ($fileSizeInMB MB) lên Máy chủ AI...")

                val fileBody = okhttp3.RequestBody.create("audio/mp4".toMediaTypeOrNull(), audioFile)
                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.name, fileBody)
                    .build()

                // DÙNG TÊN MIỀN VÀ BẮT BUỘC THÊM USER-AGENT ĐỂ KHÔNG BỊ LỖI 502
                val apiRequest = Request.Builder()
                    .url("https://api.tunertools.top/api/analyze_file")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .post(multipartBody)
                    .build()

                updateStatus("Máy chủ đang chạy Librosa tính toán ma trận...")
                val apiResponse = client.newCall(apiRequest).execute()

                if (apiResponse.isSuccessful && apiResponse.body != null) {
                    updateStatus("Phân tích xong! Đang xử lý dữ liệu màn hình...")
                    val jsonStr = apiResponse.body!!.string()
                    val jsonObject = org.json.JSONObject(jsonStr)

                    val metaObj = jsonObject.getJSONObject("meta")
                    val tone = metaObj.optString("tone", "")
                    val bpm = metaObj.optInt("bpm", 120)
                    val rhythm = metaObj.optString("dieu", "Tự do")
                    val timeSig = metaObj.optString("nhip", "4/4")

                    val durationSec = jsonObject.optDouble("duration", 0.0)
                    val durationMs = (durationSec * 1000).toLong()

                    val chordsArray = jsonObject.optJSONArray("chords")
                    val flatChords = mutableListOf<ChordEvent>()

                    // --- KHỐI TẠO LƯỚI ---
// --- KHỐI TẠO LƯỚI CHUẨN THEO NHỊP (BPM) ---
                    val parsedGrid = mutableListOf<MeasureRowData>()
                    if (chordsArray != null) {
                        // 1. Đọc dữ liệu hợp âm rút gọn từ Python
                        for (i in 0 until chordsArray.length()) {
                            val cObj = chordsArray.getJSONObject(i)
                            val startMs = (cObj.getDouble("time") * 1000).toLong()
                            val chordName = cObj.getString("chord")
                            flatChords.add(ChordEvent(startMs, chordName))
                        }

                        // 2. Tính toán thông số Toán học của Lưới
                        val beatsPerMeasure = when { timeSig.startsWith("3") -> 3; timeSig.startsWith("6") -> 6; else -> 4 }
                        val beatDurationMs = if (bpm > 0) 60000L / bpm else 500L

                        // Tính tổng số phách (beats) và ô nhịp (measures) dựa trên thời lượng bài hát
                        val totalBeats = (durationMs / beatDurationMs).toInt() + 1
                        val totalMeasures = (totalBeats / beatsPerMeasure) + 1

                        var lastChord = ""

                        // 3. Vẽ lưới đều đặn theo từng Phách thời gian
                        for (m in 0 until totalMeasures) {
                            val cells = mutableListOf<GridCellData>()
                            for (b in 0 until beatsPerMeasure) {
                                val start = (m * beatsPerMeasure + b) * beatDurationMs
                                val end = start + beatDurationMs

                                // Dò xem tại thời điểm (start + 1/4 nhịp) đang là hợp âm gì
                                val checkTime = start + (beatDurationMs / 4)
                                val activeChordEvent = flatChords.lastOrNull { it.timeMs <= checkTime }
                                val activeChord = activeChordEvent?.chord ?: ""

                                // Tối ưu UI: Chỉ hiện chữ ở đầu ô nhịp hoặc khi chuyển hợp âm mới
                                val textToShow = if (b == 0) {
                                    activeChord
                                } else if (activeChord != lastChord) {
                                    activeChord
                                } else {
                                    ""
                                }

                                if (activeChord.isNotEmpty()) {
                                    lastChord = activeChord
                                }

                                cells.add(GridCellData(textToShow, start, end))
                            }
                            parsedGrid.add(MeasureRowData(m, cells))
                        }
                    }

                    // --- [BỔ SUNG MỚI] BÓC TÁCH DỮ LIỆU SÓNG ÂM TỪ PYTHON ---
                    val waveformArray = jsonObject.optJSONArray("waveform")
                    val parsedWaveform = mutableListOf<Pair<Long, Float>>()
                    if (waveformArray != null) {
                        for (i in 0 until waveformArray.length()) {
                            val wObj = waveformArray.getJSONObject(i)
                            val timeMs = wObj.getLong("time")
                            val energy = wObj.getDouble("energy").toFloat()
                            parsedWaveform.add(Pair(timeMs, energy))
                        }
                    }

                    if (audioFile.exists()) audioFile.delete()

                    stopSelfWorkflow(
                        grid = parsedGrid,
                        chords = flatChords,
                        lyrics = emptyList(),
                        bpm = bpm,
                        timeSignature = timeSig,
                        rhythm = rhythm,
                        durationMs = durationMs,
                        simplifiedWaveform = parsedWaveform, // Đã truyền dữ liệu sóng âm thật vào đây!
                        tone = tone
                    )
                } else {
                    updateStatus("Lỗi phân tích máy chủ: Mã ${apiResponse.code}")
                    stopSelfWorkflow(emptyList(), emptyList(), emptyList(), 0, "", "", 0L, emptyList(), "")
                }

            } catch (e: Exception) {
                Log.e("ChordLog", "Lỗi: ${e.message}")
                updateStatus("Lỗi hệ thống: ${e.localizedMessage}")
                stopSelfWorkflow(emptyList(), emptyList(), emptyList(), 0, "", "", 0L, emptyList(), "")
            }
        }
    }
    private fun updateStatus(status: String) {
        serviceScope.launch(Dispatchers.Main) { onStatusChanged?.invoke(status) }
    }

    private fun stopSelfWorkflow(
        grid: List<MeasureRowData>,
        chords: List<ChordEvent>,
        lyrics: List<LyricEvent>,
        bpm: Int,
        timeSignature: String,
        rhythm: String,
        durationMs: Long,
        simplifiedWaveform: List<Pair<Long, Float>>,
        tone: String
    ) {
        isRunning = false
        CoroutineScope(Dispatchers.Main).launch {
            if (grid.isNotEmpty() || chords.isNotEmpty()) {
                onAnalysisFinished?.invoke(grid, chords, lyrics, bpm, timeSignature, rhythm, durationMs, simplifiedWaveform, tone)
            }
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        isRunning = false
        serviceJob.cancel()
        super.onDestroy()
    }
}

// ============================================================================
// HÀM 2: GỌI TÊN MIỀN ĐỂ LẤY DIRECT LINK
// ============================================================================
suspend fun extractAudioUrl(youtubeLink: String, client: OkHttpClient): String = suspendCancellableCoroutine { continuation ->

    // DÙNG TÊN MIỀN CỦA ANH
    val pythonApiUrl = "https://api.tunertools.top/api/get_direct_link"

    val jsonBody = """
        {
            "url": "$youtubeLink"
        }
    """.trimIndent()

    val requestBody = okhttp3.RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody)

    val request = Request.Builder()
        .url(pythonApiUrl)
        .addHeader("Accept", "application/json")
        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36") // GIẤY THÔNG HÀNH CHỐNG CHẶN BOT
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            continuation.resumeWithException(Exception("Lỗi kết nối Máy chủ Python: ${e.message}"))
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            if (response.isSuccessful && response.body != null) {
                val responseData = response.body!!.string()
                try {
                    val jsonObject = org.json.JSONObject(responseData)
                    val directUrl = jsonObject.optString("url", "")

                    if (directUrl.isNotEmpty()) {
                        continuation.resume(directUrl)
                    } else {
                        continuation.resumeWithException(Exception(jsonObject.optString("error", "Máy chủ Python không tìm thấy link.")))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(Exception("Lỗi đọc dữ liệu từ máy chủ Python."))
                }
            } else {
                continuation.resumeWithException(Exception("Máy chủ Python từ chối kết nối: Mã ${response.code}"))
            }
        }
    })
}
// Hàm 2: Tải Direct Link về lưu vào Cache của điện thoại
suspend fun downloadAudioToCache(context: Context, audioUrl: String, client: OkHttpClient): File {
    val request = Request.Builder().url(audioUrl).build()
    val response = client.newCall(request).execute()

    if (!response.isSuccessful || response.body == null) {
        throw Exception("Lỗi khi tải file nhạc: ${response.code}")
    }

    // Tạo file tạm trong thư mục Cache của App
    val tempFile = File(context.cacheDir, "temp_youtube_audio.m4a")
    if (tempFile.exists()) tempFile.delete()

    // Dùng Okio (đi kèm OkHttp) để ghi luồng stream xuống ổ cứng mượt mà
    val sink = tempFile.sink().buffer()
    sink.writeAll(response.body!!.source())
    sink.close()

    return tempFile
}