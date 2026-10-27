package ducphi.tunnerinstrument

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import android.util.Log
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

// ===============================================
// UI COMPONENTS TÙY CHỈNH CHO FORM NHẬP LIỆU
// ===============================================

@Composable
fun CompactInputField(value: String, onValueChange: (String) -> Unit, placeholder: String, textColor: Color, tunaGreen: Color, modifier: Modifier = Modifier.fillMaxWidth()) {
    BasicTextField(
        value = value,
        onValueChange = { val newVal = if (it.isNotEmpty() && it.length == 1 && it[0].isLowerCase()) it.replaceFirstChar { c -> c.uppercase() } else it; onValueChange(newVal) },
        modifier = modifier.height(42.dp),
        textStyle = TextStyle(color = textColor, fontSize = 14.sp),
        singleLine = true,
        cursorBrush = SolidColor(tunaGreen),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize().border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = Color.Gray.copy(alpha = 0.7f), fontSize = 14.sp)
                    innerTextField()
                }

                // Icon xóa nhanh
                if (value.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onValueChange("") }
                    )
                }
            }
        }
    )
}

@Composable
fun MultilineInputField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, placeholder: String, textColor: Color, tunaGreen: Color, modifier: Modifier = Modifier) {
    var isOpeningBracket by remember { mutableStateOf(true) }

    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            var updatedValue = newValue
            if (newValue.text.length - value.text.length == 1) {
                val cursor = newValue.selection.start
                if (cursor == 1 && newValue.text.isNotEmpty()) {
                    val charToCap = newValue.text[0]
                    if (charToCap.isLowerCase()) { val modifiedText = charToCap.uppercaseChar() + newValue.text.substring(1); updatedValue = newValue.copy(text = modifiedText, selection = newValue.selection) }
                } else if (cursor >= 2) {
                    val prevChar = newValue.text[cursor - 2]
                    if (prevChar == '[' || prevChar == '\n') {
                        val charToCap = newValue.text[cursor - 1]
                        if (charToCap.isLowerCase()) { val modifiedText = newValue.text.substring(0, cursor - 1) + charToCap.uppercaseChar() + newValue.text.substring(cursor); updatedValue = newValue.copy(text = modifiedText, selection = newValue.selection) }
                    }
                }
            }
            onValueChange(updatedValue)
        },
        modifier = modifier,
        // ĐÃ SỬA: Chữ nhỏ lại một xíu (13.sp) để câu dài không bị rớt dòng tự do, thêm khoảng cách dòng (22.sp) cho bố cục thoáng đẹp
        textStyle = TextStyle(color = textColor, fontSize = 13.sp, lineHeight = 22.sp),
        cursorBrush = SolidColor(tunaGreen),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = ChordVisualTransformation(tunaGreen),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize().border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(14.dp)) {
                Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (value.text.isEmpty()) Text(placeholder, color = Color.Gray.copy(alpha = 0.7f), fontSize = 13.sp, lineHeight = 22.sp)

                    // ĐÃ SỬA: Thêm đệm lề phải để chữ không bị chui xuống dưới nút "X"
                    Box(modifier = Modifier.padding(end = 24.dp)) {
                        innerTextField()
                    }

                    // ĐÃ SỬA: Tăng khoảng trống dưới đáy để cuộn lên không bị nút "[" màu xanh đè mất chữ
                    Spacer(modifier = Modifier.height(70.dp))
                }

                // Icon xóa nhanh đặt ở góc trên bên phải
                if (value.text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clickable { onValueChange(TextFieldValue("")) }
                    )
                }

                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).background(tunaGreen, RoundedCornerShape(8.dp)).clickable {
                        val currentText = value.text
                        val start = value.selection.min
                        val end = value.selection.max
                        val stringToInsert = if (isOpeningBracket) {
                            if (start > 0 && currentText[start - 1] != ' ' && currentText[start - 1] != '\n') " [" else "["
                        } else "]"
                        val newText = currentText.substring(0, start) + stringToInsert + currentText.substring(end)
                        val newCursorPosition = start + stringToInsert.length
                        onValueChange(TextFieldValue(text = newText, selection = TextRange(newCursorPosition)))
                        isOpeningBracket = !isOpeningBracket
                    }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(text = if (isOpeningBracket) "[" else "]", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    )
}

// ===============================================
// CÁC HÀM HỖ TRỢ VÀ UI COMPONENTS Ở CUỐI FILE
// ===============================================

@Composable
fun WrapRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        var yPosition = 0
        var xPosition = 0
        var maxLineHeight = 0
        val positions = mutableListOf<Pair<Int, Int>>()

        placeables.forEach { placeable ->
            if (xPosition + placeable.width > constraints.maxWidth && xPosition > 0) {
                xPosition = 0
                yPosition += maxLineHeight
                maxLineHeight = 0
            }
            positions.add(Pair(xPosition, yPosition))
            xPosition += placeable.width
            maxLineHeight = kotlin.math.max(maxLineHeight, placeable.height)
        }

        layout(constraints.maxWidth, yPosition + maxLineHeight) {
            placeables.forEachIndexed { index, placeable ->
                val (x, y) = positions[index]
                placeable.placeRelative(x = x, y = y)
            }
        }
    }
}

// --- HÀM TÍNH TOÁN DỊCH GIỌNG (TRANSPOSE) ---
fun transposeSingleChord(chord: String, offset: Int): String {
    if (offset == 0 || chord.isBlank()) return chord
    val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val flats = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    val match = Regex("^([A-G][#b]?)(.*)").find(chord) ?: return chord
    val root = match.groupValues[1]
    val suffix = match.groupValues[2]

    var index = notes.indexOf(root)
    if (index == -1) index = flats.indexOf(root)
    if (index == -1) return chord

    var newIndex = (index + offset) % 12
    if (newIndex < 0) newIndex += 12

    val newRoot = if (root.contains("b")) flats[newIndex] else notes[newIndex]
    return "$newRoot$suffix"
}

// --- HÀM RÚT GỌN HỢP ÂM DỄ ---
fun simplifyChord(chord: String): String {
    if (chord.isBlank()) return chord
    val noSlash = chord.substringBefore("/")
    val match = Regex("^([A-G][#b]?)(m|dim|aug)?").find(noSlash)
    return match?.value ?: noSlash
}

// --- HÀM TÍNH TOÁN BẬC LA MÃ (ROMAN NUMERALS) ---
fun chordToRoman(chord: String, baseTone: String): String {
    if (chord.isBlank() || baseTone.isBlank()) return chord

    val notesList = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val flatsList = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    // Lấy gốc của Tone bài hát
    val baseMatch = Regex("^([A-G][#b]?)").find(baseTone) ?: return chord
    val baseRoot = baseMatch.value
    var baseIdx = notesList.indexOf(baseRoot)
    if (baseIdx == -1) baseIdx = flatsList.indexOf(baseRoot)
    if (baseIdx == -1) return chord

    // BƯỚC MỚI: Xác định bài hát đang ở Tone Trưởng hay Tone Thứ
    val isBaseMinor = baseTone.contains("m") && !baseTone.contains("maj")

    // Lấy gốc và đuôi của hợp âm hiện tại
    val chordMatch = Regex("^([A-G][#b]?)(.*)").find(chord) ?: return chord
    val chordRoot = chordMatch.groupValues[1]
    val suffix = chordMatch.groupValues[2]

    var chordIdx = notesList.indexOf(chordRoot)
    if (chordIdx == -1) chordIdx = flatsList.indexOf(chordRoot)
    if (chordIdx == -1) return chord

    // Tính khoảng cách âm (interval) - từ 0 đến 11 nửa cung
    val interval = (chordIdx - baseIdx + 12) % 12

    // BƯỚC MỚI: CHỌN MẢNG LA MÃ TÙY THEO TONE TRƯỞNG HAY THỨ
    val romanBases = if (isBaseMinor) {
        // Nếu bài hát là Tone Thứ (Ví dụ Am: C là III, F là VI, G là VII)
        arrayOf("I", "♭II", "II", "III", "♯III", "IV", "♭V", "V", "VI", "♯VI", "VII", "♯VII")
    } else {
        // Nếu bài hát là Tone Trưởng (Ví dụ C: Eb là ♭III, Ab là ♭VI, Bb là ♭VII)
        arrayOf("I", "♭II", "II", "♭III", "III", "IV", "♭V", "V", "♭VI", "VI", "♭VII", "VII")
    }

    var roman = romanBases[interval]

    // Nếu hợp âm hiện tại là Thứ (m) hoặc Giảm (dim), thì viết La Mã in thường
    val isChordMinor = (suffix.startsWith("m") && !suffix.startsWith("maj")) || suffix.startsWith("dim")
    if (isChordMinor) {
        roman = roman.lowercase()
    }

    // Ghép hậu tố (bỏ chữ 'm' đầu vì đã thể hiện qua chữ thường)
    var displaySuffix = suffix
    if (isChordMinor && displaySuffix.startsWith("m")) {
        displaySuffix = displaySuffix.substring(1)
    }

    // Chuẩn hóa ký hiệu thăng/giáng trong phần đuôi (ví dụ: b9 -> ♭9, /F# -> /F♯)
    displaySuffix = displaySuffix.replace("b", "♭").replace("#", "♯")

    return roman + displaySuffix
}
// --- KHỐI RENDER CHỮ THÔNG MINH ---
@Composable
fun ChordTextRenderer(
    lyrics: String,
    chordColor: Color,
    textColor: Color,
    offset: Int,
    fontSize: Int,
    isChordOverText: Boolean,
    showRomanNumerals: Boolean,
    isEasyChordMode: Boolean = false,
    baseTone: String,
    karaokeProgress: Float = 0f,
    onChordClick: (String) -> Unit
) {
    if (!isChordOverText) {
        AnnotatedChordText(
            lyrics = lyrics,
            chordColor = chordColor,
            textColor = textColor,
            offset = offset,
            fontSize = fontSize,
            showRomanNumerals = showRomanNumerals,
            isEasyChordMode = isEasyChordMode,
            baseTone = baseTone,
            karaokeProgress = karaokeProgress,
            onChordClick = onChordClick
        )
    } else {
        val transposedBaseTone = transposeSingleChord(baseTone, offset)
        val chordFont = if (showRomanNumerals) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.Default

        // Tính toán tổng số từ để chia màu (Karaoke)
        val lines = lyrics.split("\n")
        val cleanLyrics = lyrics.replace(Regex("\\[.*?\\]"), "").replace(Regex("\\s+"), " ").trim()
        val totalWords = cleanLyrics.split(" ").size
        val targetActiveWords = (totalWords * karaokeProgress).roundToInt()
        var currentWordCount = 0

        Column(modifier = Modifier.fillMaxWidth()) {
            for (line in lines) {
                if (!line.contains("[")) {
                    // Logic chạy chữ cho dòng không chứa hợp âm
                    val wordsInLine = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                    WrapRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        for (word in wordsInLine) {
                            // Xác định màu chữ chạy: Đỏ nếu ở chế độ sáng (textColor khác Trắng), Vàng nếu ở chế độ tối
// Đổi màu Đỏ ở nền Sáng, Vàng ở nền Tối
                            val highlightColor = if (textColor != Color.White) Color.Red else Color(0xFFFFEB3B)
                            val activeColor = if (currentWordCount < targetActiveWords) highlightColor else textColor
                            Text("$word ", color = activeColor, fontSize = fontSize.sp, lineHeight = (fontSize * 1.5).sp)
                            currentWordCount++
                        }
                    }
                    continue
                }

                val regex = Regex("\\[(.*?)\\]")
                val matches = regex.findAll(line).toList()

                WrapRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    if (matches[0].range.first > 0) {
                        val leading = line.substring(0, matches[0].range.first)
                        val words = leading.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                        for (word in words) {
                            val activeColor = if (currentWordCount < targetActiveWords) Color(0xFFFFEB3B) else textColor
                            Column { Text(" ", fontSize = fontSize.sp); Text("$word ", color = activeColor, fontSize = fontSize.sp) }
                            currentWordCount++
                        }
                    }
                    for (i in matches.indices) {
                        val m = matches[i]
                        val raw = m.groupValues[1]
                        var tChord = transposeSingleChord(raw, offset)
                        if (isEasyChordMode) tChord = simplifyChord(tChord)
                        val displayStr = if (showRomanNumerals) chordToRoman(tChord, transposedBaseTone) else formatChordForDisplay(tChord)

                        val start = m.range.last + 1
                        val end = if (i + 1 < matches.size) matches[i+1].range.first else line.length
                        val segment = line.substring(start, end)
                        val words = segment.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

                        if (words.isEmpty()) {
                            Column {
                                Text(displayStr, color = chordColor, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, fontFamily = chordFont, modifier = Modifier.clickable { onChordClick(tChord) })
                                Text(" ", fontSize = fontSize.sp)
                            }
                        } else {
                            for ((idx, word) in words.withIndex()) {
                                val activeColor = if (currentWordCount < targetActiveWords) Color(0xFFFFEB3B) else textColor
                                Column {
                                    if (idx == 0) Text(displayStr, color = chordColor, fontWeight = FontWeight.Bold, fontSize = fontSize.sp, fontFamily = chordFont, modifier = Modifier.clickable { onChordClick(tChord) })
                                    else Text(" ", fontSize = fontSize.sp)
                                    Text("$word ", color = activeColor, fontSize = fontSize.sp)
                                }
                                currentWordCount++
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun AnnotatedChordText(
    lyrics: String,
    chordColor: Color,
    textColor: Color,
    offset: Int,
    fontSize: Int,
    showRomanNumerals: Boolean,
    isEasyChordMode: Boolean = false,
    baseTone: String,
    karaokeProgress: Float = 0f, // THÊM BIẾN NÀY
    onChordClick: (String) -> Unit
) {
    val transposedBaseTone = transposeSingleChord(baseTone, offset)
    val chordFont = if (showRomanNumerals) androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.Default

    // Tính toán số lượng ký tự cần được Highlight
    val cleanTextLength = lyrics.replace(Regex("\\[.*?\\]"), "").length
    val activeCharLimit = if (karaokeProgress < 0f) 0 else (cleanTextLength * karaokeProgress).toInt()
    var currentVisibleChar = 0

    val annotatedString = buildAnnotatedString {

        // Hàm phụ trợ nối chữ với màu biến thiên
        fun appendKaraokeChunk(chunk: String) {
            var splitIndex = 0
            var charsToFill = activeCharLimit - currentVisibleChar

            if (charsToFill > 0) {
                for (i in chunk.indices) {
                    if (!chunk[i].isWhitespace()) charsToFill--
                    splitIndex++
                    if (charsToFill <= 0) break
                }
                if (splitIndex > 0) {
                    val highlightColor = if (textColor != Color.White) Color.Red else Color(0xFFFFEB3B)
                    withStyle(SpanStyle(color = highlightColor)) { append(chunk.substring(0, splitIndex)) }
                    currentVisibleChar += chunk.substring(0, splitIndex).count { !it.isWhitespace() }
                }
            }

            if (splitIndex < chunk.length) {
                withStyle(SpanStyle(color = textColor)) { append(chunk.substring(splitIndex)) }
                currentVisibleChar += chunk.substring(splitIndex).count { !it.isWhitespace() }
            }
        }

        val regex = Regex("\\[(.*?)\\]")
        var lastIndex = 0
        val matches = regex.findAll(lyrics)

        for (match in matches) {
            val textBeforeChord = lyrics.substring(lastIndex, match.range.first)
            appendKaraokeChunk(textBeforeChord)

            val raw = match.groupValues[1]
            var tChord = transposeSingleChord(raw, offset)
            if (isEasyChordMode) tChord = simplifyChord(tChord)
            val displayStr = if (showRomanNumerals) chordToRoman(tChord, transposedBaseTone) else formatChordForDisplay(tChord)

            pushStringAnnotation("CHORD", tChord)
            withStyle(SpanStyle(color = chordColor, fontWeight = FontWeight.Bold, fontFamily = chordFont)) { append("[$displayStr]") }
            pop()

            lastIndex = match.range.last + 1

            if (lastIndex < lyrics.length && lyrics[lastIndex] != ' ' && lyrics[lastIndex] != '\n' && lyrics[lastIndex] != ',' && lyrics[lastIndex] != '.') {
                appendKaraokeChunk(" ")
            }
        }
        appendKaraokeChunk(lyrics.substring(lastIndex))
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(fontSize = fontSize.sp, lineHeight = (fontSize * 1.7).sp),
        onClick = { off -> annotatedString.getStringAnnotations("CHORD", off, off).firstOrNull()?.let { onChordClick(it.item) } }
    )
}
@Composable
fun ChordPopupDialog(
    chordQuery: String,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    tunaGreen: Color,
    currentLang: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // ĐÃ SỬA: Bổ sung định kiểu List<ChordModel> rõ ràng để trình biên dịch hiểu
    var allChords by remember { mutableStateOf<List<ChordModel>>(emptyList()) }

    var selectedInstrument by remember { mutableStateOf("Guitar") }
    var currentPosIndex by remember(chordQuery, selectedInstrument) { mutableIntStateOf(0) }

    LaunchedEffect(selectedInstrument) {
        val fileName = when (selectedInstrument) {
            "Guitar" -> "guitar.json"
            "Ukulele" -> "ukulele.json"
            else -> "piano.json"
        }
        allChords = loadChordsFromAssets(context, fileName)
        CoreAudioPlayer.init()
        CoreAudioPlayer.loadInstrument(context, selectedInstrument)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = surfaceColor, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val exactMatch = allChords.find { chord ->
                    val displaySuffix = when (chord.suffix) { "major" -> ""; "minor" -> "m"; else -> chord.suffix }
                    val chordFullName = "${chord.key}$displaySuffix"
                    val isDirectMatch = chordFullName.equals(chordQuery, ignoreCase = true)

                    val enharmonicMap = mapOf("C#" to "Db", "Db" to "C#", "D#" to "Eb", "Eb" to "D#", "F#" to "Gb", "Gb" to "F#", "G#" to "Ab", "Ab" to "G#", "A#" to "Bb", "Bb" to "A#")
                    var isEnharmonicMatch = false
                    val match = Regex("^([A-G][#b]?)(.*)").find(chordQuery)

                    if (match != null) {
                        val root = match.groupValues[1]
                        val suffix = match.groupValues[2]
                        val altRoot = enharmonicMap[root]
                        if (altRoot != null) {
                            val altChordName = "$altRoot$suffix"
                            isEnharmonicMatch = chordFullName.equals(altChordName, ignoreCase = true)
                        }
                    }
                    isDirectMatch || isEnharmonicMatch
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = formatChordForDisplay(chordQuery), color = tunaGreen, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(bgColor, RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(selectedInstrument, color = textColor, fontSize = 14.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = textColor, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(surfaceColor)) {
                            listOf("Guitar", "Ukulele", "Piano").forEach { inst ->
                                DropdownMenuItem(text = { Text(inst, color = textColor) }, onClick = { selectedInstrument = inst; expanded = false })
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))

                // --- THANH ĐIỀU KHIỂN THẾ TAY ---
                if (exactMatch != null && exactMatch.positions.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (currentPosIndex > 0) currentPosIndex--
                                else currentPosIndex = exactMatch.positions.size - 1
                            },
                            modifier = Modifier.background(surfaceColor, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "Prev", tint = tunaGreen)
                        }

                        // Hiển thị số thứ tự thế tay bằng định dạng an toàn
                        val handPositionText = try {
                            String.format(CoreTranslator.getString("hand_position", currentLang), currentPosIndex + 1, exactMatch.positions.size)
                        } catch (e: Exception) {
                            "${CoreTranslator.getString("hand_position_fallback", currentLang)}: ${currentPosIndex + 1} / ${exactMatch.positions.size}"
                        }

                        Text(
                            text = handPositionText,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                currentPosIndex = (currentPosIndex + 1) % exactMatch.positions.size
                            },
                            modifier = Modifier.background(surfaceColor, CircleShape).size(36.dp)
                        ) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "Next", tint = tunaGreen)
                        }
                    }
                }

                if (exactMatch != null && exactMatch.positions.isNotEmpty()) {
                    val position = exactMatch.positions[currentPosIndex.coerceIn(0, exactMatch.positions.size - 1)]
                    val displaySuffix = when (exactMatch.suffix) { "major" -> ""; "minor" -> "m"; else -> exactMatch.suffix }
                    val chordFullName = "${exactMatch.key}$displaySuffix"
                    val displayChordFullName = formatChordForDisplay(chordFullName)

                    Box(modifier = Modifier.fillMaxWidth().clickable { CoreAudioPlayer.playChord(position.midi, selectedInstrument) }, contentAlignment = Alignment.Center) {
                        if (selectedInstrument == "Piano") {
                            PianoKeyboardDiagram(displayChordFullName, position.midi, textColor, tunaGreen)
                        } else {
                            FretboardDiagram(displayChordFullName, position.frets, fingers = position.fingers, position.baseFret, numStrings = if (selectedInstrument == "Guitar") 6 else 4, textColor, tunaGreen)
                        }
                    }
                } else {
                    Text(text = CoreTranslator.getString("not_found_chord", currentLang), color = textColor, modifier = Modifier.padding(vertical = 32.dp))
                }
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = tunaGreen), modifier = Modifier.fillMaxWidth()) {
                    Text(CoreTranslator.getString("close", currentLang), color = Color.White)
                }
            }
        }
    }
}
@Composable
fun ChordAIIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Màu gradient xanh lá - xanh dương giống logo gốc
        val gradientColors = listOf(Color(0xFF00E676), Color(0xFF00B0FF))

        // 1. Vẽ viền hình pick (phím gảy đàn guitar)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.15f)
            quadraticBezierTo(w * 0.5f, -h * 0.05f, w * 0.85f, h * 0.15f) // Cạnh trên
            quadraticBezierTo(w * 1.05f, h * 0.6f, w * 0.5f, h * 0.95f)  // Cạnh phải
            quadraticBezierTo(-w * 0.05f, h * 0.6f, w * 0.15f, h * 0.15f) // Cạnh trái
            close()
        }

        drawPath(
            path = path,
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(gradientColors),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w * 0.1f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // 2. Vẽ 5 vạch sóng âm bên trong
        val barWidth = w * 0.08f
        val spacing = w * 0.14f
        val startX = w / 2 - (spacing * 2)
        val heights = listOf(h * 0.2f, h * 0.35f, h * 0.5f, h * 0.35f, h * 0.2f)

        for (i in 0..4) {
            val x = startX + i * spacing
            val barHeight = heights[i]
            drawLine(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(gradientColors),
                start = Offset(x, h / 2 - barHeight / 2),
                end = Offset(x, h / 2 + barHeight / 2),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
fun downloadSheetImage(context: Context, url: String, fileName: String, currentLang: String) {
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url)).apply {
            // Dịch tiêu đề tải xuống
            setTitle(CoreTranslator.getString("downloading_sheet", currentLang))
            setDescription(fileName)
            setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_PICTURES, "TunerApp/$fileName")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        downloadManager.enqueue(request)
        // Dịch thông báo bắt đầu tải
        Toast.makeText(context, "${CoreTranslator.getString("download_started", currentLang)}: $fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // Dịch thông báo lỗi
        Toast.makeText(context, CoreTranslator.getString("download_error", currentLang), Toast.LENGTH_SHORT).show()
    }
}
@Composable
fun FloatingCameraOverlay(currentLang: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Tọa độ để kéo thả khung camera
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // SỬA LỖI 1: Viết rõ kiểu dữ liệu trả về cho videoCapture
    val videoCapture = remember<VideoCapture<Recorder>> {
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
            .build()
        VideoCapture.withOutput(recorder)
    }

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(120.dp, 160.dp) // Kích thước khung camera thu nhỏ
            .padding(top = 16.dp, start = 16.dp) // Cách lề một chút lúc mới mở
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .pointerInput(Unit) {
                // Cho phép dùng ngón tay kéo khung camera đi chỗ khác
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        // Màn hình hiển thị Camera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)

                // SỬA LỖI 2: Đóng gói khối lệnh bằng Runnable {}
                cameraProviderFuture.addListener(Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)
                    } catch (exc: Exception) {
                        Toast.makeText(context, CoreTranslator.getString("camera_open_error", currentLang), Toast.LENGTH_SHORT).show()
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Nút Đóng Camera
        IconButton(
            onClick = {
                if (isRecording) { recording?.stop() }
                onClose()
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(alpha=0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, "Đóng", tint = Color.White, modifier = Modifier.size(16.dp))
        }

        // Nút Bấm Quay (Đỏ) / Dừng (Vuông)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
                .clickable {
                    if (isRecording) {
                        // Dừng quay
                        recording?.stop()
                        recording = null
                        isRecording = false
                    } else {
                        // Bắt đầu quay
                        val name = "Cover_${System.currentTimeMillis()}.mp4"
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        }
                        val mediaStoreOutputOptions = MediaStoreOutputOptions
                            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                            .setContentValues(contentValues)
                            .build()

                        // Yêu cầu quyền Micro khi quay
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                            // SỬA LỖI 3: Gói khối sự kiện bằng androidx.core.util.Consumer {}
                            recording = videoCapture.output
                                .prepareRecording(context, mediaStoreOutputOptions)
                                .withAudioEnabled()
                                .start(ContextCompat.getMainExecutor(context), androidx.core.util.Consumer { recordEvent ->
                                    when(recordEvent) {
                                        is VideoRecordEvent.Start -> isRecording = true
                                        is VideoRecordEvent.Finalize -> {
                                            if (!recordEvent.hasError()) {
                                                Toast.makeText(context, CoreTranslator.getString("video_saved_gallery", currentLang), Toast.LENGTH_LONG).show()
                                            }
                                            isRecording = false
                                        }
                                    }
                                })
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Nút bên trong đổi hình dạng khi đang quay
            Box(modifier = Modifier
                .size(if (isRecording) 14.dp else 24.dp)
                .background(Color.Red, if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
            )
        }
    }
}
// ==========================================
// CÁC COMPONENT FORM DIALOG ĐƯỢC TÁCH RỜI
// Nhằm giảm tải cho hàm SongTabScreen chính
// ==========================================

@Composable
fun ReportSongDialogModal(reportInfo: ReportInfo, currentLang: String, tunaGreen: Color, textColor: Color, surfaceColor: Color, deviceId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Tải các lý do báo cáo từ file ngôn ngữ
    val reason1 = CoreTranslator.getString("report_reason_1", currentLang)
    val reason2 = CoreTranslator.getString("report_reason_2", currentLang)
    val reason3 = CoreTranslator.getString("report_reason_3", currentLang)
    val reasons = listOf(reason1, reason2, reason3)

    var selectedReason by remember { mutableStateOf(reason3) }
    var reportNote by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(12.dp), color = surfaceColor, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(CoreTranslator.getString("report_song", currentLang), color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("${CoreTranslator.getString("song_label", currentLang)}: ${reportInfo.title}", color = textColor, fontWeight = FontWeight.Bold)
                Text("${CoreTranslator.getString("singer", currentLang)}: ${reportInfo.singer} | ${CoreTranslator.getString("composer", currentLang)}: ${reportInfo.composer}", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(CoreTranslator.getString("report_reason", currentLang), color = textColor, fontWeight = FontWeight.Bold)
                reasons.forEach { reason ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 4.dp)) {
                        RadioButton(selected = (selectedReason == reason), onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = tunaGreen))
                        Text(reason, color = textColor, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reportNote, onValueChange = { reportNote = it },
                    label = { Text(CoreTranslator.getString("additional_notes", currentLang), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    textStyle = TextStyle(color = textColor),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = tunaGreen)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray) }
                    Button(
                        onClick = {
                            if (isSubmittingReport) return@Button
                            isSubmittingReport = true
                            coroutineScope.launch {
                                val success = SongScraper.submitReport(reportInfo.songId, deviceId, selectedReason, reportNote)
                                if (success) {
                                    if (selectedReason == reason2) {
                                        val subject = Uri.encode("Copyright Report: ${reportInfo.title}")
                                        val body = Uri.encode("Song: ${reportInfo.title}\nComposer: ${reportInfo.composer}\nSinger: ${reportInfo.singer}\n\nReason: $selectedReason\nNote: $reportNote\n\n[ID: ${reportInfo.songId}]")
                                        try { context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:tamthanhphuc@gmail.com?subject=$subject&body=$body") }) } catch (e: Exception) { Toast.makeText(context, CoreTranslator.getString("email_app_not_found", currentLang), Toast.LENGTH_SHORT).show() }
                                    } else { Toast.makeText(context, CoreTranslator.getString("send_success", currentLang), Toast.LENGTH_SHORT).show() }
                                    onDismiss()
                                } else { Toast.makeText(context, CoreTranslator.getString("server_error", currentLang), Toast.LENGTH_SHORT).show() }
                                isSubmittingReport = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = tunaGreen)
                    ) {
                        if (isSubmittingReport) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text(CoreTranslator.getString("send", currentLang), color = Color.White)
                    }
                }
            }
        }
    }
}
@Composable
fun AddSongDialogModal(
    currentLang: String,
    tunaGreen: Color,
    textColor: Color,
    surfaceColor: Color,
    userRole: String,
    isStaff: Boolean,
    initialSongData: SongDetails? = null,
    songRepository: SongRepository? = null,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var newTitle by remember(initialSongData) { mutableStateOf(initialSongData?.title ?: "") }
    var newSinger by remember(initialSongData) { mutableStateOf(initialSongData?.singer ?: "") }
    var newComposer by remember(initialSongData) { mutableStateOf(initialSongData?.composer ?: "") }
    var newBaseTone by remember(initialSongData) { mutableStateOf(initialSongData?.baseTone ?: "") }
    var newRhythm by remember(initialSongData) { mutableStateOf(initialSongData?.rhythm ?: "") }
    var newBpm by remember(initialSongData) { mutableStateOf(initialSongData?.bpm ?: "") }
    var newVideoUrl by remember(initialSongData) { mutableStateOf(initialSongData?.videoUrl ?: "") }
    var newLyrics by remember(initialSongData) { mutableStateOf(TextFieldValue(initialSongData?.lyrics ?: "")) }
    var isAdding by remember { mutableStateOf(false) }
    var isCollapsed by remember { mutableStateOf(false) }

    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> selectedImages = uris }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = surfaceColor, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (initialSongData != null) "Tạo phiên bản hợp âm" else CoreTranslator.getString("add_title", currentLang),
                        color = Color(0xFF1976D2),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isCollapsed = !isCollapsed }.padding(8.dp)) {
                        Text(CoreTranslator.getString(if (isCollapsed) "expand" else "collapse", currentLang), color = Color(0xFF1976D2), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, null, tint = Color(0xFF1976D2))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (!isCollapsed) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CompactInputField(value = newTitle, onValueChange = { newTitle = it }, placeholder = CoreTranslator.getString("song_name_req", currentLang), textColor = textColor, tunaGreen = tunaGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = newSinger, onValueChange = { newSinger = it }, placeholder = CoreTranslator.getString("singer", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = newComposer, onValueChange = { newComposer = it }, placeholder = CoreTranslator.getString("composer", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = newBaseTone, onValueChange = { newBaseTone = it }, placeholder = CoreTranslator.getString("tone", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = newRhythm, onValueChange = { newRhythm = it }, placeholder = CoreTranslator.getString("rhythm", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = newBpm, onValueChange = { newBpm = it }, placeholder = CoreTranslator.getString("bpm_hint", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = newVideoUrl, onValueChange = { newVideoUrl = it }, placeholder = CoreTranslator.getString("youtube_hint", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        CompactInputField(value = newTitle, onValueChange = { newTitle = it }, placeholder = CoreTranslator.getString("song_name_req", currentLang), textColor = textColor, tunaGreen = tunaGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1976D2)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))) {
                            Icon(Icons.Default.Image, contentDescription = "Select Image", modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(CoreTranslator.getString("select_sheet", currentLang), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (selectedImages.isNotEmpty()) {
                            val txt = try { String.format(CoreTranslator.getString("selected_pages", currentLang), selectedImages.size) } catch(e: Exception) { "${CoreTranslator.getString("selected_pages_fallback", currentLang)}: ${selectedImages.size}" }
                            Text(txt, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    MultilineInputField(value = newLyrics, onValueChange = { newLyrics = it }, placeholder = CoreTranslator.getString("lyrics_label", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.fillMaxWidth().weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray) }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // NÚT LƯU VÀO MÁY (CỤC BỘ)
                        OutlinedButton(
                            onClick = {
                                if (newTitle.isBlank()) return@OutlinedButton
                                isAdding = true
                                coroutineScope.launch {
                                    val songDataToSave = SongDetails(
                                        songId = initialSongData?.songId ?: "",
                                        title = newTitle,
                                        singer = newSinger.ifBlank { CoreTranslator.getString("updating", currentLang) },
                                        composer = newComposer.ifBlank { CoreTranslator.getString("updating", currentLang) },
                                        baseTone = newBaseTone.ifBlank { "C" },
                                        rhythm = newRhythm,
                                        bpm = newBpm,
                                        videoUrl = newVideoUrl,
                                        lyrics = newLyrics.text
                                    )
                                    val success = songRepository?.insertCustomSongLocal(songDataToSave) ?: false
                                    if (success) {
                                        Toast.makeText(context, "Đã lưu bản hợp âm vào máy thành công!", Toast.LENGTH_SHORT).show()
                                        onSuccess(newTitle)
                                    } else {
                                        Toast.makeText(context, "Lỗi khi lưu vào thiết bị", Toast.LENGTH_SHORT).show()
                                    }
                                    isAdding = false
                                }
                            },
                            enabled = !isAdding,
                            border = androidx.compose.foundation.BorderStroke(1.dp, tunaGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = tunaGreen)
                        ) {
                            Text("Lưu vào máy", fontSize = 13.sp)
                        }

                        // NÚT CHIA SẺ CHO CỘNG ĐỒNG (GỬI LÊN SERVER)
                        Button(
                            onClick = {
                                if (newTitle.isBlank()) return@Button
                                isAdding = true
                                coroutineScope.launch {
                                    val newSong = SongDetails(
                                        title = newTitle,
                                        singer = newSinger.ifBlank { CoreTranslator.getString("updating", currentLang) },
                                        composer = newComposer.ifBlank { CoreTranslator.getString("updating", currentLang) },
                                        baseTone = newBaseTone.ifBlank { "C" },
                                        rhythm = newRhythm,
                                        bpm = newBpm,
                                        videoUrl = newVideoUrl,
                                        lyrics = newLyrics.text
                                    )
                                    // Lưu cục bộ trước
                                    songRepository?.insertCustomSongLocal(newSong)

                                    // Đẩy lên online
                                    val success = SongScraper.addSongWithImages(context, newSong, selectedImages, userRole)
                                    if (success) {
                                        if (isStaff) {
                                            Toast.makeText(context, "Đã chia sẻ lên cộng đồng thành công!", Toast.LENGTH_SHORT).show()
                                            onSuccess(newTitle)
                                        } else {
                                            Toast.makeText(context, "Đã lưu vào máy và gửi duyệt lên cộng đồng!", Toast.LENGTH_LONG).show()
                                            onSuccess(newTitle)
                                        }
                                    } else {
                                        Toast.makeText(context, "Đã lưu vào máy (Lỗi gửi lên server)", Toast.LENGTH_SHORT).show()
                                        onSuccess(newTitle)
                                    }
                                    isAdding = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                            enabled = !isAdding
                        ) {
                            if (isAdding) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            else Text("Chia sẻ", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditSongDialogModal(currentSongData: SongDetails, currentLang: String, tunaGreen: Color, textColor: Color, surfaceColor: Color, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var editTitle by remember { mutableStateOf(currentSongData.title) }
    var editSinger by remember { mutableStateOf(currentSongData.singer) }
    var editComposer by remember { mutableStateOf(currentSongData.composer) }
    var editBaseTone by remember { mutableStateOf(currentSongData.baseTone) }
    var editRhythm by remember { mutableStateOf(currentSongData.rhythm) }
    var editBpm by remember { mutableStateOf(currentSongData.bpm) }
    var editVideoUrl by remember { mutableStateOf(currentSongData.videoUrl) }

    // ĐÃ SỬA: Sử dụng formatRawLyrics để tự động sắp xếp lại bố cục giống y hệt trang chi tiết bài hát
    var editLyrics by remember { mutableStateOf(TextFieldValue(formatRawLyrics(currentSongData.lyrics))) }

    var isUpdating by remember { mutableStateOf(false) }
    var isCollapsed by remember { mutableStateOf(false) }
    var keptOldImageIndices by remember { mutableStateOf((1..currentSongData.imageCount).toList()) }

    // ĐÃ SỬA LỖI TẠI ĐÂY: Thêm kiểu rõ ràng cho <List<Uri>> và <Any?>
    var newSelectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewImage by remember { mutableStateOf<Any?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -> newSelectedImages = newSelectedImages + uris }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = surfaceColor, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(CoreTranslator.getString("edit_title", currentLang), color = tunaGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isCollapsed = !isCollapsed }.padding(8.dp)) {
                        Text(CoreTranslator.getString(if (isCollapsed) "expand" else "collapse", currentLang), color = tunaGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Icon(if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp, null, tint = tunaGreen)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (!isCollapsed) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CompactInputField(value = editTitle, onValueChange = { editTitle = it }, placeholder = CoreTranslator.getString("song_name_req", currentLang), textColor = textColor, tunaGreen = tunaGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = editSinger, onValueChange = { editSinger = it }, placeholder = CoreTranslator.getString("singer", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = editComposer, onValueChange = { editComposer = it }, placeholder = CoreTranslator.getString("composer", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = editBaseTone, onValueChange = { editBaseTone = it }, placeholder = CoreTranslator.getString("tone", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = editRhythm, onValueChange = { editRhythm = it }, placeholder = CoreTranslator.getString("rhythm", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompactInputField(value = editBpm, onValueChange = { editBpm = it }, placeholder = CoreTranslator.getString("bpm_hint", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                                CompactInputField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, placeholder = CoreTranslator.getString("youtube_hint", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        CompactInputField(value = editTitle, onValueChange = { editTitle = it }, placeholder = CoreTranslator.getString("song_name_req", currentLang), textColor = textColor, tunaGreen = tunaGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { imagePickerLauncher.launch("image/*") }, colors = ButtonDefaults.outlinedButtonColors(contentColor = tunaGreen), border = androidx.compose.foundation.BorderStroke(1.dp, tunaGreen)) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add image", modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(CoreTranslator.getString("add_sheet", currentLang), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        val totalCount = keptOldImageIndices.size + newSelectedImages.size
                        val totalPagesText = try { String.format(CoreTranslator.getString("total_pages", currentLang), totalCount) } catch (e: Exception) { "${CoreTranslator.getString("total_pages_fallback", currentLang)}: $totalCount" }
                        Text(totalPagesText, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (keptOldImageIndices.isNotEmpty() || newSelectedImages.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState())) {
                            val baseUrl = "https://raw.githubusercontent.com/ducphi0491/sheet_images/main/"
                            val cacheBuster = remember(currentSongData) { System.currentTimeMillis() }

                            keptOldImageIndices.forEach { index ->
                                val oldImageUrl = "$baseUrl${currentSongData.slug}-$index.jpg?t=$cacheBuster"
                                Box(modifier = Modifier.padding(end = 8.dp).size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) {
                                    AsyncImage(model = oldImageUrl, contentDescription = "Old Image", modifier = Modifier.fillMaxSize().clickable { previewImage = oldImageUrl }, contentScale = ContentScale.Crop)
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { keptOldImageIndices = keptOldImageIndices.filter { it != index } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                                }
                            }
                            newSelectedImages.forEach { uri ->
                                Box(modifier = Modifier.padding(end = 8.dp).size(80.dp).clip(RoundedCornerShape(8.dp)).background(Color.Gray.copy(alpha = 0.2f))) {
                                    AsyncImage(model = uri, contentDescription = "New Image", modifier = Modifier.fillMaxSize().clickable { previewImage = uri }, contentScale = ContentScale.Crop)
                                    Box(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).clickable { newSelectedImages = newSelectedImages.filter { it != uri } }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    MultilineInputField(value = editLyrics, onValueChange = { editLyrics = it }, placeholder = CoreTranslator.getString("lyrics_label", currentLang), textColor = textColor, tunaGreen = tunaGreen, modifier = Modifier.fillMaxWidth().weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray) }
                    Button(onClick = {
                        isUpdating = true;
                        coroutineScope.launch {
                            val updatedSong = currentSongData.copy(title = editTitle, singer = editSinger, composer = editComposer, baseTone = editBaseTone, rhythm = editRhythm, bpm = editBpm, videoUrl = editVideoUrl, lyrics = editLyrics.text)
                            val success = SongScraper.updateSongWithImages(context, updatedSong, keptOldImageIndices, newSelectedImages)
                            if (success) { onSuccess() } else { Toast.makeText(context, CoreTranslator.getString("update_error_try_again", currentLang), Toast.LENGTH_SHORT).show() }
                            isUpdating = false
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = tunaGreen), enabled = !isUpdating) { if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text(CoreTranslator.getString("save", currentLang), color = Color.White) }
                }
            }
        }

        if (previewImage != null) {
            Dialog(onDismissRequest = { previewImage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    AsyncImage(model = previewImage, contentDescription = "Preview", modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                    IconButton(onClick = { previewImage = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)) { Icon(Icons.Default.Close, contentDescription = CoreTranslator.getString("close", currentLang), tint = Color.White) }
                }
            }
        }
    }
}
// ==========================================
// COMPONENT TÁCH RỜI: BẢNG LỊCH SỬ TÌM KIẾM
// ==========================================
@Composable
fun SearchHistoryOverlay(
    isSearchFocused: Boolean,
    urlInputText: String,
    searchHistory: List<String>,
    suggestSongs: List<Song>, // Vẫn giữ tham số để hàm cha khỏi báo lỗi
    songRepository: SongRepository,
    currentLang: String,
    tunaGreen: Color,
    surfaceColor: Color,
    bgColor: Color,
    textColor: Color,
    onClose: () -> Unit,
    onClearAll: () -> Unit,
    onDeleteQuery: (String) -> Unit,
    onQuerySelected: (String, Song?) -> Unit
) {
    // ĐÃ SỬA: CHỈ HIỂN THỊ OVERLAY NÀY KHI ĐANG FOCUS VÀ Ô NHẬP LIỆU TRỐNG
    if (isSearchFocused && urlInputText.isEmpty()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            // 1. Lớp nền đen mờ (Click để thoát)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onClose() }
            )

            // 2. Bảng lịch sử hiển thị đè lên nền đen
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -50 }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(max = 450.dp)
                        .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CoreTranslator.getString("search_history_title", currentLang),
                                    color = tunaGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (searchHistory.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onClearAll() }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = CoreTranslator.getString("clear_all", currentLang), color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Box(
                                        modifier = Modifier.size(28.dp).background(Color.Red.copy(alpha = 0.1f), CircleShape).clickable { onClose() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Thoát", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                        }

                        if (searchHistory.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    Text(text = CoreTranslator.getString("empty_search_history", currentLang), color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(searchHistory.size) { index ->
                                val rawQuery = searchHistory[index]
                                val songId = if (rawQuery.contains("|")) rawQuery.substringBefore("|") else ""
                                val query = if (rawQuery.contains("|")) rawQuery.substringAfter("|") else rawQuery

                                // --- BẮT ĐẦU: TRUY VẤN NGƯỢC ĐỂ LẤY ẢNH VÀ TÊN CA SĨ ---
                                var thumbnailUrl by remember(rawQuery) { mutableStateOf("") }
                                var artistName by remember(rawQuery) { mutableStateOf("") } // <-- Thêm biến chứa tên ca sĩ

                                LaunchedEffect(rawQuery) {
                                    // Repository da duoc chuan bi tai SongMainScreen, khong mo them DB o day.
                                    val song = songRepository.getSongByExactIdOrTitle(
                                        songId = songId,
                                        songTitle = query,
                                        includeLyrics = false
                                    )
                                    if (song != null) {
                                        artistName = song.ca_si
                                        if (song.youtubeUrl.isNotBlank()) {
                                            var videoId = ""
                                            val rawUrl = song.youtubeUrl
                                            if (rawUrl.contains("watch?v=")) {
                                                videoId = rawUrl.substringAfter("watch?v=").substringBefore("&")
                                            } else if (rawUrl.contains("youtu.be/")) {
                                                videoId = rawUrl.substringAfter("youtu.be/").substringBefore("?")
                                            }
                                            if (videoId.isNotBlank()) {
                                                thumbnailUrl = "https://img.youtube.com/vi/${videoId.trim()}/mqdefault.jpg"
                                            }
                                        }
                                    }
                                }
                                // --- KẾT THÚC TRUY VẤN NGƯỢC ---

                                val dummySongForHistory = remember(rawQuery, artistName) {
                                    if (artistName.isNotBlank()) {
                                        Song(id = songId, title = query, originalKey = "", rhythm = "", chordList = emptyList(), lyrics = emptyList(), favoriteCount = 0, viewCount = 0, commentCount = 0, ratingStars = 0f, ratingCount = 0, genre = emptyList(), authorId = "", authorName = "", singerNames = emptyList(), youtubeUrl = "", createdAt = "", updatedAt = "", tac_gia = "", ca_si = artistName, tong = "", dieu = "", bpm = 0, the_loai = "", slug = "", image_count = 0, zingMp3Id = "")
                                    } else {
                                        null
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onQuerySelected(query, dummySongForHistory) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color.Gray,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(28.dp)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp, 42.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (thumbnailUrl.isNotBlank()) {
                                            coil.compose.AsyncImage(
                                                model = thumbnailUrl,
                                                contentDescription = "Thumbnail",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = query,
                                            color = textColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (artistName.isNotBlank()) {
                                                artistName
                                            } else if (currentLang == "vi") {
                                                "Chạm để tìm lại"
                                            } else {
                                                "Tap to search again"
                                            },
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteQuery(rawQuery) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Xóa",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (index < searchHistory.size - 1) {
                                    HorizontalDivider(
                                        color = Color.Gray.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(start = 88.dp, end = 16.dp)
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
// ==========================================
// COMPONENT TÁCH RỜI: DANH SÁCH HỢP ÂM TỔNG HỢP CUỐI BÀI HÁT
// ==========================================
@Composable
fun SongChordsSummary(
    lyrics: String,
    transposeOffset: Int,
    isEasyChordMode: Boolean = false,
    guitarChordsList: List<ChordModel>,
    tunaGreen: Color,
    surfaceColor: Color,
    currentLang: String,
    textColor: Color,
    onChordClick: (String) -> Unit
) {
    val validChordsToDraw = remember(lyrics, transposeOffset, isEasyChordMode, guitarChordsList) {
        val regex = Regex("\\[(.*?)\\]")
        val uniqueRawChords = regex.findAll(lyrics).map { it.groupValues[1] }
            .map { transposeSingleChord(it, transposeOffset) }
            .map { if (isEasyChordMode) simplifyChord(it) else it }
            .distinct().toList()
        uniqueRawChords.mapNotNull { chordQuery ->
            val exactMatch = guitarChordsList.find { chord ->
                val displaySuffix = when (chord.suffix) { "major" -> ""; "minor" -> "m"; else -> chord.suffix }
                val chordFullName = "${chord.key}$displaySuffix"
                val isDirectMatch = chordFullName.equals(chordQuery, ignoreCase = true)
                val enharmonicMap = mapOf("C#" to "Db", "Db" to "C#", "D#" to "Eb", "Eb" to "D#", "F#" to "Gb", "Gb" to "F#", "G#" to "Ab", "Ab" to "G#", "A#" to "Bb", "Bb" to "A#")
                var isEnharmonicMatch = false
                val match = Regex("^([A-G][#b]?)(.*)").find(chordQuery)
                if (match != null) {
                    val root = match.groupValues[1]; val suffix = match.groupValues[2]
                    val altRoot = enharmonicMap[root]
                    if (altRoot != null) { val altChordName = "$altRoot$suffix"; isEnharmonicMatch = chordFullName.equals(altChordName, ignoreCase = true) }
                }
                isDirectMatch || isEnharmonicMatch
            }
            if (exactMatch != null && exactMatch.positions.isNotEmpty()) Pair(chordQuery, exactMatch) else null
        }
    }

    if (validChordsToDraw.isNotEmpty()) {
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))
        Text(text = CoreTranslator.getString("chords_in_song", currentLang), color = tunaGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            validChordsToDraw.chunked(4).forEach { rowChords ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rowChords.forEach { (chordQuery, exactMatch) ->
                        val position = exactMatch.positions[0]
                        val displaySuffix = when (exactMatch.suffix) { "major" -> ""; "minor" -> "m"; else -> exactMatch.suffix }
                        val chordFullName = "${exactMatch.key}$displaySuffix"
                        val displayChordFullName = formatChordForDisplay(chordFullName)

                        Box(modifier = Modifier.weight(1f).aspectRatio(0.65f).background(surfaceColor, RoundedCornerShape(8.dp)).border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)).clickable { onChordClick(chordQuery) }.padding(top = 8.dp, bottom = 4.dp, start = 2.dp, end = 2.dp)) {
                            Box(modifier = Modifier.align(Alignment.Center).padding(start = 2.dp, end = 14.dp)) {
                                FretboardDiagram("   ", position.frets, position.fingers, position.baseFret, 6, textColor, tunaGreen)
                            }
                            Text(text = displayChordFullName, color = textColor, fontSize = if (displayChordFullName.length >= 5) 13.sp else 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.TopCenter).offset(x = (-4).dp, y = (-2).dp))
                            if (position.baseFret > 1) { Text(text = "${position.baseFret}fr", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 43.dp)) }
                        }
                    }
                    val emptySpots = 4 - rowChords.size
                    repeat(emptySpots) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ==========================================
// COMPONENT TÁCH RỜI: DIALOG ĐỒNG Ý BẢN QUYỀN
// ==========================================
@Composable
fun CopyrightPolicyDialog(
    currentLang: String,
    tunaGreen: Color,
    surfaceColor: Color,
    textColor: Color,
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    var isPolicyChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = surfaceColor,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = CoreTranslator.getString("copyright_policy", currentLang),
                    color = tunaGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                    Text(
                        text = if (currentLang == "vi") """
                            CHÍNH SÁCH NỘI DUNG & BẢN QUYỀN

                            Tuner tools là nền tảng cộng đồng hoàn toàn miễn phí, được xây dựng như một không gian mở để kết nối những người yêu âm nhạc.

                            1. Tiêu chuẩn cộng đồng
                            • Tuyệt đối không đăng tải các bài hát có ngôn từ kích động, phản cảm, vi phạm pháp luật hoặc trái với thuần phong mỹ tục.

                            2. Cam kết về Quyền tác giả
                            • Tuân thủ pháp luật: Mọi nội dung chia sẻ phải phù hợp với quy định về sở hữu trí tuệ của Việt Nam và Quốc tế.
                            • Xác nhận trước khi đăng: Bạn cần đảm bảo mình có quyền sử dụng hoặc đã được sự cho phép của tác giả trước khi đăng tải tác phẩm không do mình sáng tác.
                            • Xử lý vi phạm: Mọi tài liệu vi phạm bản quyền hoặc tiêu chuẩn cộng đồng sẽ bị gỡ bỏ ngay lập tức mà không cần thông báo trước.

                            Bằng việc bấm Tiếp tục, bạn xác nhận đã hiểu và đồng ý cùng Tuner tools xây dựng một cộng đồng âm nhạc văn minh, thượng tôn pháp luật.
                        """.trimIndent() else """
                            CONTENT & COPYRIGHT POLICY

                            Tuner tools is a free community platform built to connect music lovers.

                            1. Community Standards
                            • Do not post songs with offensive, illegal content, or content against fine customs.

                            2. Copyright Commitment
                            • Compliance: All shared content must comply with intellectual property laws.
                            • Verify Before Posting: Ensure you have the right or permission from the author before uploading third-party works.
                            • Violations: Any violating material will be removed immediately without prior notice.

                            By clicking Continue, you agree to build a civilized, law-abiding music community with Tuner tools.
                        """.trimIndent(),
                        color = textColor,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .clickable { isPolicyChecked = !isPolicyChecked }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPolicyChecked,
                        onCheckedChange = { isPolicyChecked = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = tunaGreen,
                            checkmarkColor = Color.White
                        )
                    )
                    Text(
                        text = CoreTranslator.getString("read_and_agree", currentLang),
                        color = textColor,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            CoreTranslator.getString("cancel", currentLang),
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAccept,
                        enabled = isPolicyChecked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = tunaGreen,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            CoreTranslator.getString("continue_btn", currentLang),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SongListItemCard(
    song: Song,
    indexText: String?,
    containerColor: Color,
    currentLang: String,
    surfaceColor: Color,
    tunaGreen: Color,
    bgColor: Color,
    textColor: Color,
    deviceId: String,
    sharedPref: android.content.SharedPreferences,
    userRole: String,
    songRepository: SongRepository? = null,
    refreshTrigger: Int = 0,
    onOpenDetail: (String) -> Unit,
    onOpenCustomDetail: ((SongDetails) -> Unit)? = null,
    onReport: () -> Unit,
    onChordPopup: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (SongDetails) -> Unit = {},
    onCreateCustomChord: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLikedItem by remember { mutableStateOf(sharedPref.getBoolean("liked_${song.id}", false)) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var videoId = ""
    if (song.youtubeUrl.isNotBlank()) {
        videoId = if (song.youtubeUrl.contains("watch?v=")) song.youtubeUrl.substringAfter("watch?v=").substringBefore("&")
        else song.youtubeUrl.substringAfter("youtu.be/").substringBefore("?")
    }
    val thumbnailUrl = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/mqdefault.jpg" else ""

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpenDetail("CHORD") },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                var customLocalSong by remember(song.id, song.title, song.ca_si, refreshTrigger) { mutableStateOf<Song?>(null) }
                LaunchedEffect(song.id, song.title, song.ca_si, refreshTrigger) {
                    if (song.id.startsWith("local_")) {
                        customLocalSong = song
                    } else if (songRepository != null) {
                        customLocalSong = songRepository.getCustomSongLocalByTitle(song.title, song.ca_si)
                    }
                }

                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (indexText != null) {
                        Text(text = indexText, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, lineHeight = 22.sp, modifier = Modifier.padding(end = 6.dp))
                    }
                    Text(text = song.title, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)

                    // HIỂN THỊ NGÔI SAO MÀU VÀNG NẾU CÓ BẢN TÙY CHỈNH CỦA BẠN
                    if (customLocalSong != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                val cSong = customLocalSong!!
                                val customDetail = SongDetails(
                                    title = cSong.title,
                                    singer = cSong.ca_si,
                                    composer = cSong.tac_gia,
                                    baseTone = cSong.tong,
                                    rhythm = cSong.dieu.ifBlank { cSong.rhythm },
                                    bpm = if (cSong.bpm > 0) cSong.bpm.toString() else "",
                                    lyrics = cSong.lyrics.joinToString("\n"),
                                    videoUrl = cSong.youtubeUrl,
                                    songId = cSong.id,
                                    slug = cSong.slug,
                                    imageCount = cSong.image_count,
                                    likes = cSong.favoriteCount,
                                    isLikedByDevice = sharedPref.getBoolean("liked_${cSong.id}", false),
                                    zingMp3Id = cSong.zingMp3Id
                                )
                                onOpenCustomDetail?.invoke(customDetail)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Bản hợp âm của bạn",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (deviceId.isBlank()) {
                                Toast.makeText(context, "Vui lòng đăng nhập Gmail để thêm vào Yêu thích", Toast.LENGTH_SHORT).show()
                            } else {
                                isLikedItem = !isLikedItem
                                sharedPref.edit().putBoolean("liked_${song.id}", isLikedItem).apply()
                                coroutineScope.launch { SongScraper.toggleLike(song.id, deviceId, isLikedItem) }
                            }
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = if (isLikedItem) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = if (isLikedItem) Color.Red else Color.Gray, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.viewCount.toString(),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Lượt xem",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Đã xóa menu 3 chấm trên danh sách tìm kiếm ngoài
                }
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text(CoreTranslator.getString("delete_song", currentLang), color = tunaGreen, fontWeight = FontWeight.Bold) },
                    text = { Text("${CoreTranslator.getString("delete_confirm", currentLang)}\n'${song.title}'", color = textColor) },
                    containerColor = surfaceColor,
                    confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDeleteClick() }) { Text(CoreTranslator.getString("delete", currentLang), color = Color.Red, fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray) } }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(100.dp).height(70.dp).clip(RoundedCornerShape(8.dp)).background(bgColor), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    if (thumbnailUrl.isNotBlank()) {
                        AsyncImage(model = coil.request.ImageRequest.Builder(LocalContext.current).data(thumbnailUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "${CoreTranslator.getString("composer_prefix", currentLang)} ${song.tac_gia.ifEmpty { CoreTranslator.getString("updating", currentLang) }}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${CoreTranslator.getString("singer_prefix", currentLang)} ${song.ca_si.ifEmpty { CoreTranslator.getString("updating", currentLang) }}", color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${CoreTranslator.getString("tone", currentLang)}: ", color = Color.Gray, fontSize = 13.sp)
                        Text(text = song.tong.ifEmpty { "?" }, color = tunaGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                if (song.chordList.isNotEmpty() || song.hasLyrics) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bgColor).border(1.dp, tunaGreen, RoundedCornerShape(4.dp)).clickable { onOpenDetail("CHORD") }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Icon(imageVector = CustomIcons.Chord, contentDescription = "Chord", tint = tunaGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(CoreTranslator.getString("chord_btn", currentLang), color = tunaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (song.image_count > 0) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bgColor).border(1.dp, Color(0xFFE6A23C), RoundedCornerShape(4.dp)).clickable { onOpenDetail("SHEET") }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        SheetMusicIcon(tint = Color(0xFFE6A23C), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(CoreTranslator.getString("sheet_btn", currentLang), color = Color(0xFFE6A23C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (song.chordList.isNotEmpty() && song.chordList.any { it.isNotBlank() }) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    song.chordList.filter { it.isNotBlank() }.forEach { chord ->
                        Box(Modifier.padding(end = 6.dp).background(bgColor, RoundedCornerShape(4.dp)).clickable { onChordPopup(chord) }.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(formatChordForDisplay(chord), color = tunaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SongDetailHeader(
    context: Context,
    currentSongData: SongDetails,
    currentLang: String,
    isLiked: Boolean,
    likeCount: Int,
    tunaGreen: Color,
    textColor: Color,
    activeMediaType: String = "",
    overrideVideoUrl: String = "",
    isStaff: Boolean = false,
    userRole: String = "user",
    sharedPref: android.content.SharedPreferences,
    songRepository: SongRepository? = null,
    refreshTrigger: Int = 0,
    surfaceColor: Color = Color(0xFF1E1E1E),
    onEnterVideoFullScreen: (android.view.View, android.webkit.WebChromeClient.CustomViewCallback) -> Unit = { _, _ -> },
    onExitVideoFullScreen: () -> Unit = {},
    onWebViewCreated: (android.webkit.WebView) -> Unit = {},
    onCloseVideoClick: () -> Unit = {},
    onReportClick: () -> Unit,
    onLikeClick: () -> Unit,
    onPlayVideoClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCreateCustomChord: () -> Unit = {},
    onEditCustomChord: (SongDetails) -> Unit = {},
    onDeleteCustomChord: () -> Unit = {},
    onToneDetectClick: () -> Unit = {},
    onTutorialClick: () -> Unit = {},
    onStrummingClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var customLocalSong by remember(currentSongData.title, currentSongData.singer, refreshTrigger) { mutableStateOf<Song?>(null) }
    LaunchedEffect(currentSongData.title, currentSongData.singer, refreshTrigger) {
        if (currentSongData.songId.startsWith("local_")) {
            customLocalSong = null
        } else if (songRepository != null) {
            customLocalSong = songRepository.getCustomSongLocalByTitle(currentSongData.title, currentSongData.singer)
        }
    }
    var videoId = ""
    val rawUrl = if (overrideVideoUrl.isNotBlank()) overrideVideoUrl else currentSongData.videoUrl
    if (rawUrl.isNotBlank()) {
        videoId = if (rawUrl.contains("watch?v=")) rawUrl.substringAfter("watch?v=").substringBefore("&")
        else if (rawUrl.contains("youtu.be/")) rawUrl.substringAfter("youtu.be/").substringBefore("?")
        else if (rawUrl.contains("embed/")) rawUrl.substringAfter("embed/").substringBefore("?")
        else ""
    }
    val thumbnailUrl = if (videoId.isNotBlank()) "https://img.youtube.com/vi/${videoId.trim()}/mqdefault.jpg" else ""

    val isPlaying = activeMediaType == "YOUTUBE"
    val headerHeight = if (isPlaying) 230.dp else 220.dp

    // Trạng thái cho DropdownMenu và hộp thoại xác nhận
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showInterruptDialog by remember { mutableStateOf(false) }
    var pendingInterruptAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val handleMediaIconClick = { action: () -> Unit ->
        if (activeMediaType.isNotBlank()) {
            pendingInterruptAction = action
            showInterruptDialog = true
        } else {
            action()
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(headerHeight).clip(RoundedCornerShape(12.dp)).background(surfaceColor)
    ) {
        if (isPlaying && videoId.isNotBlank()) {
            val embedUrl = "https://www.youtube.com/embed/${videoId.trim()}?autoplay=1&playsinline=1&fs=1"
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0"
                        }
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                                super.onShowCustomView(view, callback)
                                if (view != null && callback != null) { onEnterVideoFullScreen(view, callback) }
                            }
                            override fun onHideCustomView() {
                                super.onHideCustomView()
                                onExitVideoFullScreen()
                            }
                        }
                        webViewClient = android.webkit.WebViewClient()
                        onWebViewCreated(this)
                        val headers = mutableMapOf<String, String>()
                        headers["Referer"] = "http://ducphi.atwebpages.com/"
                        loadUrl(embedUrl, headers)
                    }
                },
                update = { _ -> },
                modifier = Modifier.fillMaxSize()
            )
        } else if (thumbnailUrl.isNotBlank()) {
            coil.compose.AsyncImage(model = thumbnailUrl, contentDescription = "YouTube Thumbnail", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }

        if (!isPlaying) {
            Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(
                0.0f to Color.Black.copy(alpha = 0.35f), 0.2f to Color.Transparent, 0.6f to Color.Black.copy(alpha = 0.5f), 1.0f to Color.Black.copy(alpha = 0.95f)
            )))

            Column(modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, bottom = 0.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.Start) {
                Text(text = currentSongData.title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.fillMaxWidth(), lineHeight = 28.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                val rowHeight = 22.dp
                Row(modifier = Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${CoreTranslator.getString("composer_prefix", currentLang)} ${currentSongData.composer}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
                Row(modifier = Modifier.height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("${CoreTranslator.getString("singer_prefix", currentLang)} ${currentSongData.singer}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }

                val safeRhythm = currentSongData.rhythm.replace("\n", "").replace("\r", "").trim()
                if (safeRhythm.isNotBlank() && safeRhythm != "null") {
                    Row(modifier = Modifier.height(rowHeight).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = "${CoreTranslator.getString("rhythm", currentLang)}: $safeRhythm",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    // Ba nút chức năng dùng chung một kích thước để hàng nút luôn thẳng và cân đối.
                    val headerActionWidth = 64.dp
                    val headerActionHeight = 20.dp
                    val headerActionShape = RoundedCornerShape(4.dp)
                    val headerActionBg = Color.White.copy(alpha = 0.25f)

                    // Tắt font padding mặc định của Android. Nếu để font padding,
                    // Row sẽ căn giữa "khung chữ" nhưng phần glyph nhìn thấy vẫn bị tụt xuống.
                    val headerActionTextStyle = TextStyle(
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        )
                    )

                    Row(
                        modifier = Modifier
                            .width(headerActionWidth)
                            .height(headerActionHeight)
                            .clip(headerActionShape)
                            .background(headerActionBg)
                            .clickable { handleMediaIconClick { onToneDetectClick() } },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = songToneText("button", currentLang),
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = songToneText("button", currentLang),
                            style = headerActionTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .height(headerActionHeight)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        modifier = Modifier
                            .width(headerActionWidth)
                            .height(headerActionHeight)
                            .clip(headerActionShape)
                            .background(headerActionBg)
                            .clickable { handleMediaIconClick { onTutorialClick() } },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = "Tutorial", tint = Color.White, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = CoreTranslator.getString("tutorial_btn", currentLang),
                            style = headerActionTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .height(headerActionHeight)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        modifier = Modifier
                            .width(headerActionWidth)
                            .height(headerActionHeight)
                            .clip(headerActionShape)
                            .background(headerActionBg)
                            .clickable { handleMediaIconClick { onStrummingClick() } },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = "Tiết tấu", tint = Color.White, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Tiết tấu",
                            style = headerActionTextStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .height(headerActionHeight)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }

                    // Giữ cụm dịch vụ âm nhạc ở phía phải, còn ba nút chức năng bám mép trái
                    // cùng trục với dòng thông tin điệu nhạc phía trên.
                    Spacer(modifier = Modifier.weight(1f))

                    ZingMp3Icon(modifier = Modifier.padding(start = 6.dp).size(16.dp).clickable { handleMediaIconClick { searchMusicOnApp(context, "ZING", currentSongData.title, currentSongData.singer, currentLang) } })
                    AppleMusicIcon(modifier = Modifier.padding(start = 6.dp).size(16.dp).clickable { handleMediaIconClick { searchMusicOnApp(context, "APPLE", currentSongData.title, currentSongData.singer, currentLang) } })
                    SpotifyIcon(modifier = Modifier.padding(start = 6.dp).size(16.dp).clickable { handleMediaIconClick { searchMusicOnApp(context, "SPOTIFY", currentSongData.title, currentSongData.singer, currentLang) } })

                    if (currentSongData.videoUrl.isNotBlank()) {
                        Box(modifier = Modifier.padding(start = 6.dp).size(width = 24.dp, height = 16.dp).background(Color.Red, RoundedCornerShape(4.dp)).clickable { handleMediaIconClick { onPlayVideoClick() } }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }

        // CỤM NÚT ĐIỀU KHIỂN GÓC TRÊN BÊN PHẢI (Trái tim + Icon 3 chấm)
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isPlaying) {
                IconButton(onClick = onCloseVideoClick, modifier = Modifier.size(36.dp)) {
                    Box(modifier = Modifier.size(28.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                // 1. Nút Yêu thích (Trái tim)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onLikeClick() }.padding(horizontal = 6.dp, vertical = 6.dp)) {
                    Icon(imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Like", tint = Color.Red, modifier = Modifier.size(22.dp))
                    if (likeCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = likeCount.toString(), color = Color.Red, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Nút 3 chấm (Menu More)
                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Thao tác khác",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(surfaceColor)
                    ) {
                        // 1. Cập nhật bài hát (Chỉ dành cho Admin / Staff)
                        if (isStaff) {
                            DropdownMenuItem(
                                text = { Text(CoreTranslator.getString("edit_title", currentLang), color = textColor) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = tunaGreen, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onEditClick()
                                }
                            )
                        }

                        // 2. Báo cáo bài hát
                        DropdownMenuItem(
                            text = { Text(CoreTranslator.getString("report_song", currentLang), color = textColor) },
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.Red, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showMoreMenu = false
                                onReportClick()
                            }
                        )

                        // 3. Xóa bài hát (Chỉ dành cho Admin / Staff)
                        if (isStaff) {
                            DropdownMenuItem(
                                text = { Text(CoreTranslator.getString("delete", currentLang), color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }

                        // 4. Các chức năng bản hợp âm tự tạo
                        if (currentSongData.songId.startsWith("local_")) {
                            // Đang xem trực tiếp bản tự tạo: Sửa và Hủy
                            DropdownMenuItem(
                                text = { Text("Sửa bản hợp âm của bạn", color = tunaGreen) },
                                leadingIcon = {
                                    Icon(Icons.Default.EditNote, contentDescription = "Sửa", tint = tunaGreen, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onEditCustomChord(currentSongData)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Hủy bản hợp âm của bạn", color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.StarBorder, contentDescription = "Hủy", tint = Color.Red, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onDeleteCustomChord()
                                }
                            )
                        } else if (customLocalSong != null) {
                            // Đang xem bản gốc nhưng máy đã có bản tự tạo
                            DropdownMenuItem(
                                text = { Text("Sửa bản hợp âm của bạn", color = tunaGreen) },
                                leadingIcon = {
                                    Icon(Icons.Default.EditNote, contentDescription = "Sửa", tint = tunaGreen, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    val cSong = customLocalSong!!
                                    val customDetail = SongDetails(
                                        title = cSong.title,
                                        singer = cSong.ca_si,
                                        composer = cSong.tac_gia,
                                        baseTone = cSong.tong,
                                        rhythm = cSong.dieu.ifBlank { cSong.rhythm },
                                        bpm = if (cSong.bpm > 0) cSong.bpm.toString() else "",
                                        lyrics = cSong.lyrics.joinToString("\n"),
                                        videoUrl = cSong.youtubeUrl,
                                        songId = cSong.id,
                                        slug = cSong.slug,
                                        imageCount = cSong.image_count,
                                        likes = cSong.favoriteCount,
                                        isLikedByDevice = sharedPref.getBoolean("liked_${cSong.id}", false),
                                        zingMp3Id = cSong.zingMp3Id
                                    )
                                    onEditCustomChord(customDetail)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Hủy bản hợp âm của bạn", color = Color.Red) },
                                leadingIcon = {
                                    Icon(Icons.Default.StarBorder, contentDescription = "Hủy", tint = Color.Red, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    coroutineScope.launch {
                                        val success = songRepository?.deleteCustomSongLocal(currentSongData.title, currentSongData.singer) ?: false
                                        if (success) {
                                            customLocalSong = null
                                            onDeleteCustomChord()
                                            Toast.makeText(context, "Đã hủy bản hợp âm tự lưu", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        } else {
                            // Chưa có bản tự tạo: Thêm bản hợp âm
                            DropdownMenuItem(
                                text = { Text("Thêm hợp âm phiên bản của bạn", color = Color(0xFF00B0FF)) },
                                leadingIcon = {
                                    Icon(Icons.Default.QueueMusic, contentDescription = "Thêm", tint = Color(0xFF00B0FF), modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onCreateCustomChord()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Hộp thoại xác nhận xóa bài hát
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(CoreTranslator.getString("delete_song", currentLang), color = tunaGreen, fontWeight = FontWeight.Bold) },
                text = { Text("${CoreTranslator.getString("delete_confirm", currentLang)}\n'${currentSongData.title}'", color = textColor) },
                containerColor = surfaceColor,
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    }) {
                        Text(CoreTranslator.getString("delete", currentLang), color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(CoreTranslator.getString("cancel", currentLang), color = Color.Gray)
                    }
                }
            )
        }

        // Hộp thoại cảnh báo ngắt phát nhạc
        if (showInterruptDialog) {
            AlertDialog(
                onDismissRequest = {
                    showInterruptDialog = false
                    pendingInterruptAction = null
                },
                title = {
                    Text(
                        text = if (currentLang == "vi") "Tạm dừng phát?" else "Stop playback?",
                        color = tunaGreen,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (currentLang == "vi")
                            "Bài hát đang được phát hoặc đang chạy Karaoke. Bạn có muốn dừng lại để mở tính năng này không?"
                        else
                            "Media is currently playing. Do you want to stop and open this feature?",
                        color = textColor
                    )
                },
                containerColor = surfaceColor,
                confirmButton = {
                    TextButton(onClick = {
                        showInterruptDialog = false
                        onCloseVideoClick()
                        pendingInterruptAction?.invoke()
                        pendingInterruptAction = null
                    }) {
                        Text(if (currentLang == "vi") "Đồng ý" else "Continue", color = tunaGreen, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showInterruptDialog = false
                        pendingInterruptAction = null
                    }) {
                        Text(if (currentLang == "vi") "Quay lại" else "Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}
// ==========================================
// COMPONENT TÁCH RỜI: TRÌNH XEM SHEET NHẠC (Thu gọn)
// ==========================================
@Composable
fun SheetMusicViewer(
    currentSongData: SongDetails,
    currentLang: String,
    isFullScreen: Boolean,
    onFullScreenToggle: () -> Unit,
    scrollSpeed: Float,
    onScrollSpeedChange: (Float) -> Unit,
    isAutoScrolling: Boolean,
    onAutoScrollToggle: () -> Unit,
    tunaGreen: Color,
    bgColor: Color,
    surfaceColor: Color,
    textColor: Color,
    imageCacheBuster: Long
) {
    val context = LocalContext.current
    val toolbarBgColor = if (isFullScreen) surfaceColor else bgColor
    val toolbarBtnColor = if (isFullScreen) bgColor else surfaceColor

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(
                toolbarBgColor,
                RoundedCornerShape(if (isFullScreen) 0.dp else 8.dp)
            ).padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).background(toolbarBtnColor, CircleShape).clickable { onScrollSpeedChange((scrollSpeed - 0.5f).coerceAtLeast(0.5f)) }, Alignment.Center) {
                    Icon(Icons.Default.Remove, null, tint = textColor, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${scrollSpeed}x", color = Color.Gray, fontSize = 10.sp)
                    Box(Modifier.size(28.dp).background(if (isAutoScrolling) tunaGreen else toolbarBtnColor, CircleShape).clickable { onAutoScrollToggle() }, Alignment.Center) {
                        Icon(if (isAutoScrolling) Icons.Default.Pause else Icons.Default.KeyboardDoubleArrowDown, null, tint = if (isAutoScrolling) Color.White else tunaGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(26.dp).background(toolbarBtnColor, CircleShape).clickable { onScrollSpeedChange((scrollSpeed + 0.5f).coerceAtMost(5f)) }, Alignment.Center) {
                    Icon(Icons.Default.Add, null, tint = textColor, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.width(32.dp)); Box(Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f))); Spacer(Modifier.width(32.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onFullScreenToggle() }) {
                Text(CoreTranslator.getString(if (isFullScreen) "zoom_out" else "full_screen", currentLang), color = Color.Gray, fontSize = 10.sp)
                Box(Modifier.size(28.dp).background(if (isFullScreen) tunaGreen else toolbarBtnColor, CircleShape), Alignment.Center) {
                    Icon(if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, null, tint = if (isFullScreen) Color.White else tunaGreen, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.width(16.dp)); Box(Modifier.width(1.dp).height(30.dp).background(Color.Gray.copy(alpha = 0.3f))); Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable {
                val baseUrl = "https://raw.githubusercontent.com/ducphi0491/sheet_images/main/"
                for (i in 1..currentSongData.imageCount) {
                    val dlUrl = "$baseUrl${currentSongData.slug}-$i.jpg"
                    val fName = "${currentSongData.slug}_page_$i.jpg"
                    downloadSheetImage(context, dlUrl, fName, currentLang)
                }
            }) {
                Text(CoreTranslator.getString("download", currentLang), color = Color.Gray, fontSize = 10.sp)
                Box(Modifier.size(28.dp).background(toolbarBtnColor, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Download, null, tint = tunaGreen, modifier = Modifier.size(18.dp))
                }
            }
        }

        val baseUrl = "https://raw.githubusercontent.com/ducphi0491/sheet_images/main/"
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            for (i in 1..currentSongData.imageCount) {
                var scale by remember(currentSongData.songId, i) { mutableFloatStateOf(1f) }
                var offset by remember(currentSongData.songId, i) { mutableStateOf(Offset.Zero) }
                val supportedExtensions = listOf("jpg", "jpeg", "png", "gif")
                var extIndex by remember(currentSongData.songId, i) { mutableIntStateOf(0) }

                val currentExt = supportedExtensions[extIndex]
                val imageUrl = "$baseUrl${currentSongData.slug}-$i.$currentExt?t=$imageCacheBuster"

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(androidx.compose.ui.graphics.RectangleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                do {
                                    val event = awaitPointerEvent()
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()

                                    if (event.changes.size > 1 || scale > 1f) {
                                        scale = (scale * zoom).coerceIn(1f, 4f)
                                        if (scale > 1f) {
                                            offset = Offset(offset.x + pan.x * scale, offset.y + pan.y * scale)
                                            event.changes.forEach { it.consume() }
                                        } else { offset = Offset.Zero }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    coil.compose.SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                        contentDescription = "Trang $i",
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                        contentScale = ContentScale.FillWidth,
                        loading = {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = tunaGreen)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${CoreTranslator.getString("loading_page", currentLang)} $i...", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        },
                        error = {
                            if (extIndex < supportedExtensions.size - 1) {
                                LaunchedEffect(extIndex) { extIndex++ }
                                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = tunaGreen) }
                            } else {
                                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                    Text("${CoreTranslator.getString("cannot_load_page", currentLang)} $i", color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
// ===================================================================
// BỘ CÔNG CỤ KARAOKE: TẢI PHỤ ĐỀ YOUTUBE VÀ SO KHỚP VĂN BẢN (LOCAL)
// ===================================================================
// Thêm class mới để hứng thời gian từng chữ
data class AiWordTime(val word: String, val startTimeMs: Long, val endTimeMs: Long)

// Cập nhật 2 class cũ để chứa danh sách chữ
data class YoutubeCaptionEvent(val startTimeMs: Long, val endTimeMs: Long, val text: String, val words: List<AiWordTime> = emptyList())
data class SyncedDbLine(val originalTextWithChords: String, val startTimeMs: Long, val endTimeMs: Long, val words: List<AiWordTime> = emptyList())
// 👈 THÊM THAM SỐ videoId VÀO ĐẦU HÀM
suspend fun fetchYoutubeSubtitlesLocal(audioFile: File, videoId: String, client: okhttp3.OkHttpClient): List<YoutubeCaptionEvent> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val result = mutableListOf<YoutubeCaptionEvent>()
    try {
        android.util.Log.d("KaraokeLog", "--- GỌI SERVER PYTHON LẤY PHỤ ĐỀ (UPLOAD FILE) ---")

        // 1. Đóng gói File vào MultipartBody thay vì JSON
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("video_id", videoId) // 👈 BỔ SUNG ID BÀI HÁT
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
            )
            .build()

        // 2. GỌI LÊN SERVER CỦA BẠN
        val apiRequest = okhttp3.Request.Builder()
            .url("https://api.tunertools.top/api/get_lyrics")
            .post(requestBody)
            .build()

        val response = client.newCall(apiRequest).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
            val jsonObject = org.json.JSONObject(responseBody)
            val lyricsArray = jsonObject.optJSONArray("lyrics")

            if (lyricsArray != null) {
                for (i in 0 until lyricsArray.length()) {
                    val lObj = lyricsArray.getJSONObject(i)

                    // Bóc tách mảng words từ AI
                    val wordsArray = lObj.optJSONArray("words")
                    val aiWords = mutableListOf<AiWordTime>()
                    if (wordsArray != null) {
                        for (j in 0 until wordsArray.length()) {
                            val wObj = wordsArray.getJSONObject(j)
                            aiWords.add(
                                AiWordTime(
                                    word = wObj.optString("word", ""),
                                    startTimeMs = wObj.optLong("start_time_ms", 0L),
                                    endTimeMs = wObj.optLong("end_time_ms", 0L)
                                )
                            )
                        }
                    }

                    result.add(
                        YoutubeCaptionEvent(
                            startTimeMs = lObj.optLong("start_time_ms", 0L),
                            endTimeMs = lObj.optLong("end_time_ms", 2000L),
                            text = lObj.optString("text", "").replace("\n", " ").trim(),
                            words = aiWords
                        )
                    )
                }
                android.util.Log.d("KaraokeLog", "Thành công! Lấy được ${result.size} dòng từ Server.")
                // --- THÊM LOG KIỂM CHỨNG DỮ LIỆU GỐC CỦA AI ---
                if (result.isNotEmpty()) {
                    android.util.Log.d("KaraokeLog", "▶️ [BƯỚC 1 - AI GỬI VỀ] Câu đầu tiên: '${result[0].text}'")
                    android.util.Log.d("KaraokeLog", "▶️ [BƯỚC 1 - AI GỬI VỀ] Thời gian bắt đầu: ${result[0].startTimeMs / 1000f} giây")
                }
                // ----------------------------------------------
            }
        } else {
            android.util.Log.e("KaraokeLog", "Lỗi Server: ${response.code}")
        }
    } catch (e: Exception) {
        android.util.Log.e("KaraokeLog", "Lỗi gọi API Phụ đề: ${e.message}")
    }

    return@withContext result.distinctBy { it.text }
}
fun robustMapTimingLocal(dbLyrics: String, ytCaptions: List<YoutubeCaptionEvent>): List<SyncedDbLine> {
    val result = mutableListOf<SyncedDbLine>()

    // --- BƯỚC 1: GỘP CÂU AI (Lấp khoảng trống ngang làm đẹp UI) ---
    val mergedCaptions = mutableListOf<YoutubeCaptionEvent>()
    var currentCaption: YoutubeCaptionEvent? = null

    for (caption in ytCaptions) {
        if (currentCaption == null) {
            currentCaption = caption
        } else {
            // ĐIỀU KIỆN GỘP: Nếu câu hiện tại ít hơn 9 chữ VÀ khoảng nghỉ với câu tiếp theo dưới 1.5 giây
            if (currentCaption.words.size < 9 && (caption.startTimeMs - currentCaption.endTimeMs) < 1500L) {
                val combinedWords = currentCaption.words + caption.words
                currentCaption = YoutubeCaptionEvent(
                    startTimeMs = currentCaption.startTimeMs,
                    endTimeMs = caption.endTimeMs,
                    text = currentCaption.text + " " + caption.text,
                    words = combinedWords
                )
            } else {
                mergedCaptions.add(currentCaption)
                currentCaption = caption
            }
        }
    }
    if (currentCaption != null) mergedCaptions.add(currentCaption)

    // --- BƯỚC 2: SO KHỚP THỜI GIAN VỚI CSDL ---
    val rawDbTokens = dbLyrics.split(Regex("\\s+")).filter { it.isNotBlank() }
    var dbPointer = 0

    for (caption in mergedCaptions) {
        val aiWords = caption.words
        if (aiWords.isEmpty()) continue

        val lineDbTokens = mutableListOf<String>()
        val matchedWords = mutableListOf<AiWordTime>()

        var lineStartTime = aiWords.first().startTimeMs
        var lineEndTime = aiWords.last().endTimeMs

        for ((aiIndex, aiWord) in aiWords.withIndex()) {
            val cleanAi = aiWord.word.replace(Regex("[.,?!]"), "").trim().lowercase()
            if (cleanAi.isBlank()) continue

            // Tầm nhìn quét xa 35 chữ để vượt qua nhạc dạo hoặc sai lời
            val searchLimit = minOf(dbPointer + 35, rawDbTokens.size)

            for (i in dbPointer until searchLimit) {
                val dbToken = rawDbTokens[i]
                val cleanDb = dbToken.replace(Regex("\\[.*?\\]"), "").replace(Regex("[.,?!]"), "").trim().lowercase()

                val isMatch = cleanDb.isNotBlank() && (
                        cleanDb == cleanAi ||
                                (cleanAi.length >= 3 && cleanDb.contains(cleanAi)) ||
                                (cleanDb.length >= 3 && cleanAi.contains(cleanDb))
                        )

                // --- BỘ LỌC NGỮ CẢNH: CHỐNG NHẢY CÓC SAI CHO 90,000 BÀI HÁT ---
                var isSafeMatch = isMatch
                val jumpDistance = i - dbPointer

                if (isMatch && jumpDistance > 2) {
                    var verified = false
                    val wordsToCheck = minOf(3, aiWords.size - aiIndex - 1)

                    if (wordsToCheck == 0) {
                        // Nếu là từ cuối cùng mà bắt nhảy xa thì từ chối, trừ khi từ đó rất dài và đặc trưng
                        if (cleanAi.length < 4) isSafeMatch = false
                        else verified = true
                    } else {
                        // Tìm kiếm "điểm tựa" chéo trong 3 từ tiếp theo (Fuzzy Look-ahead)
                        for (k in 1..wordsToCheck) {
                            val nextAi = aiWords[aiIndex + k].word.replace(Regex("[.,?!]"), "").trim().lowercase()
                            if (nextAi.isBlank()) continue

                            val dbSearchEnd = minOf(i + 4, rawDbTokens.size)
                            for (j in (i + 1) until dbSearchEnd) {
                                val nextDb = rawDbTokens[j].replace(Regex("\\[.*?\\]"), "").replace(Regex("[.,?!]"), "").trim().lowercase()
                                if (nextDb.isNotBlank() && (nextDb == nextAi || (nextAi.length >= 3 && nextDb.contains(nextAi)) || (nextDb.length >= 3 && nextAi.contains(nextDb)))) {
                                    verified = true
                                    break
                                }
                            }
                            if (verified) break
                        }
                        isSafeMatch = verified
                    }
                }

                if (isSafeMatch) {
                    // Cập nhật mảng DB và Con trỏ
                    for (j in dbPointer..i) {
                        lineDbTokens.add(rawDbTokens[j])
                    }
                    matchedWords.add(aiWord)
                    dbPointer = i + 1
                    break
                }
            }
        }

        if (lineDbTokens.isNotEmpty()) {
            val reconstructedLine = lineDbTokens.joinToString(" ")
            if (matchedWords.isNotEmpty()) {
                lineEndTime = matchedWords.last().endTimeMs
            }
            result.add(SyncedDbLine(reconstructedLine, lineStartTime, lineEndTime, matchedWords))
        }
    }

    // --- BƯỚC 3: VÉT ĐÁY (Thêm các đoạn Outro) ---
    if (dbPointer < rawDbTokens.size) {
        val leftoverLine = rawDbTokens.subList(dbPointer, rawDbTokens.size).joinToString(" ")
        val lastTime = result.lastOrNull()?.endTimeMs ?: 0L
        result.add(SyncedDbLine(leftoverLine, lastTime + 500L, lastTime + 3500L, emptyList()))
    }

    return result
}
@Composable
fun ArtistHorizontalRow(
    title: String,
    artists: List<SongRepository.ArtistOffline>,
    tunaGreen: Color,
    surfaceColor: Color,
    textColor: Color,
    isCircle: Boolean = false,
    currentLang: String,
    onClickArtist: (String) -> Unit
) {
    if (artists.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = title,
            color = tunaGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // --- BỔ SUNG: HIỆU ỨNG THAY ĐỔI VỊ TRÍ ---
        var currentArtists by remember { mutableStateOf(artists) }

        LaunchedEffect(artists) {
            currentArtists = artists
            while (true) {
                // Đợi một khoảng thời gian ngẫu nhiên từ 3 đến 8 giây
                delay((3000..8000).random().toLong())

                // Tráo đổi vị trí của các ca sĩ
                val shuffledList = currentArtists.toMutableList()
                shuffledList.shuffle()
                currentArtists = shuffledList
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            currentArtists.forEach { artist ->
                var artistVideoId = ""
                if (artist.youtubeUrl.isNotBlank()) {
                    artistVideoId = if (artist.youtubeUrl.contains("watch?v=")) artist.youtubeUrl.substringAfter("watch?v=").substringBefore("&") else if (artist.youtubeUrl.contains("youtu.be/")) artist.youtubeUrl.substringAfter("youtu.be/").substringBefore("?") else ""
                }
                val artistThumb = if (artist.youtubeUrl.contains("yt3.ggpht") || artist.youtubeUrl.contains("googleusercontent") || artist.youtubeUrl.contains("img.youtube.com")) { artist.youtubeUrl } else if (artistVideoId.isNotBlank()) { "https://img.youtube.com/vi/$artistVideoId/mqdefault.jpg" } else { "" }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(100.dp).clickable { onClickArtist(artist.name) }
                ) {
                    Box(
                        modifier = Modifier.size(90.dp).clip(if (isCircle) CircleShape else RoundedCornerShape(12.dp)).background(surfaceColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                        if (artistThumb.isNotBlank()) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current).data(artistThumb).crossfade(true).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "${artist.totalSongs} ${CoreTranslator.getString("songs_count", currentLang)}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
// ==========================================
// COMPONENT TÁCH RỜI: GỢI Ý BÀI HÁT (1 CỘT 3 BÀI, VUỐT NGANG)
// ==========================================
@Composable
fun SongHorizontalRow(
    title: String,
    songs: List<Song>,
    tunaGreen: Color,
    surfaceColor: Color,
    textColor: Color,
    onClickSong: (Song) -> Unit
) {
    if (songs.isEmpty()) return

    // Nhóm danh sách thành từng cụm 3 bài hát
    // Nếu danh sách không chia hết cho 3 thì nhóm cuối sẽ chứa phần lẻ
    val chunkedSongs = songs.chunked(3)

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = title,
            color = tunaGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Duyệt qua từng nhóm (mỗi cột sẽ chứa tối đa 3 bài)
            chunkedSongs.forEach { songGroup ->
                Column(
                    modifier = Modifier.width(280.dp), // Độ rộng của mỗi cột (chứa 3 bài)
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Khoảng cách giữa các bài theo chiều dọc
                ) {
                    songGroup.forEach { song ->
                        // Trích xuất ID video để làm ảnh bìa
                        var videoId = ""
                        if (song.youtubeUrl.isNotBlank()) {
                            videoId = if (song.youtubeUrl.contains("watch?v="))
                                song.youtubeUrl.substringAfter("watch?v=").substringBefore("&")
                            else if (song.youtubeUrl.contains("youtu.be/"))
                                song.youtubeUrl.substringAfter("youtu.be/").substringBefore("?")
                            else ""
                        }
                        val thumbUrl = if (videoId.isNotBlank()) "https://img.youtube.com/vi/$videoId/mqdefault.jpg" else ""

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClickSong(song) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hình ảnh thu nhỏ vuông vức của bài hát
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(surfaceColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                                    if (thumbUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(thumbUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Chi tiết Bài hát và Ca sĩ
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = textColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium, // <-- Đổi từ Bold xuống Medium
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = song.ca_si,
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// --- DATA CLASS CHỨA THÔNG TIN VIDEO HƯỚNG DẪN ---
data class TutorialVideo(
    val videoId: String,
    val title: String,
    val channel: String,
    val views: String,
    val duration: String
)

// --- HÀM TÌM KIẾM YOUTUBE TRỰC TIẾP (KHÔNG CẦN API) ---
suspend fun searchYoutubeVideos(query: String): List<TutorialVideo> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val results = mutableListOf<TutorialVideo>()
    try {
        val client = okhttp3.OkHttpClient()
        val url = "https://www.youtube.com/results?search_query=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        val response = client.newCall(request).execute()
        val html = response.body?.string() ?: return@withContext emptyList()

        // Bóc tách JSON chứa dữ liệu kết quả tìm kiếm của YouTube
        val regex = Regex("""var ytInitialData = (\{.*?\});</script>""")
        val match = regex.find(html)
        if (match != null) {
            val jsonString = match.groupValues[1]
            val jsonObject = org.json.JSONObject(jsonString)

            try {
                val contents = jsonObject.getJSONObject("contents")
                    .getJSONObject("twoColumnSearchResultsRenderer")
                    .getJSONObject("primaryContents")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents")
                    .getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents")

                for (i in 0 until contents.length()) {
                    val item = contents.getJSONObject(i)
                    if (item.has("videoRenderer")) {
                        val video = item.getJSONObject("videoRenderer")
                        val videoId = video.optString("videoId", "")
                        val title = video.getJSONObject("title").getJSONArray("runs").getJSONObject(0).optString("text", "")
                        val channel = video.getJSONObject("longBylineText").getJSONArray("runs").getJSONObject(0).optString("text", "")
                        val views = video.optJSONObject("viewCountText")?.optString("simpleText", "") ?: ""
                        val duration = video.optJSONObject("lengthText")?.optString("simpleText", "") ?: ""

                        if (videoId.isNotEmpty()) {
                            results.add(TutorialVideo(videoId, title, channel, views, duration))
                        }
                    }
                    if (results.size >= 10) break // Giới hạn lấy 10 video
                }
            } catch (e: Exception) {
                Log.e("YoutubeSearch", "Lỗi bóc tách JSON YouTube: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.e("YoutubeSearch", "Lỗi Fetch YouTube: ${e.message}")
    }
    return@withContext results
}
// Thêm component hiệu ứng sóng âm mới
@Composable
fun AudioWaveLoadingIndicator(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "wave_loading")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..4) {
            val heightMultiplier by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(
                        durationMillis = 350,
                        delayMillis = i * 100,
                        easing = androidx.compose.animation.core.FastOutLinearInEasing
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                ),
                label = "wave_bar_$i"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp * heightMultiplier)
                    .background(color, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialBottomSheet(
    songTitle: String,
    songSinger: String,
    tunaGreen: Color,
    bgColor: Color,
    surfaceColor: Color,
    currentLang: String,
    textColor: Color,
    onDismiss: () -> Unit,
    onVideoClick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // PHÂN TÁCH DANH SÁCH TAB DỰA VÀO NGUỒN GỌI
    val isRhythmTutorial = songTitle == "điệu nhạc"
    val instruments = if (isRhythmTutorial) {
        listOf("Guitar", "Piano", "Ukulele") // Trang Điệu nhạc: Chỉ có 3 tab nhạc cụ
    } else {
        listOf("Guitar", "Piano", "Ukulele", "Học hát") // Trang Đệm hát: Có thêm tab Học hát
    }

    var selectedInstrument by remember { mutableStateOf("Guitar") }
    var videos by remember { mutableStateOf<List<TutorialVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedInstrument) {
        isLoading = true
        videos = emptyList()

        val query = when (selectedInstrument) {
            "Guitar" -> "$songTitle $songSinger đệm hát guitar"
            "Piano" -> "$songTitle $songSinger đệm hát piano"
            "Ukulele" -> "$songTitle $songSinger đệm hát ukulele"
            "Học hát" -> "$songTitle $songSinger học hát"
            else -> "$songTitle $songSinger hướng dẫn $selectedInstrument"
        }

        val rawVideos = searchYoutubeVideos(query)

        videos = rawVideos.filter { video ->
            val titleLower = video.title.lowercase()
            val songTitleLower = songTitle.lowercase()

            if (isRhythmTutorial) {
                // Bộ lọc cho trang Điệu nhạc (Khắt khe, chặn chéo nhạc cụ)
                when (selectedInstrument) {
                    "Guitar" -> titleLower.contains("guitar") && !titleLower.contains("ukulele") && !titleLower.contains("uku") && !titleLower.contains("piano") && !titleLower.contains("keyboard")
                    "Piano" -> (titleLower.contains("piano") || titleLower.contains("keyboard")) && !titleLower.contains("guitar") && !titleLower.contains("ukulele") && !titleLower.contains("uku")
                    "Ukulele" -> (titleLower.contains("ukulele") || titleLower.contains("uku")) && !titleLower.contains("guitar") && !titleLower.contains("piano") && !titleLower.contains("keyboard")
                    else -> true
                }
            } else {
                // Bộ lọc cho trang Đệm hát (Cho phép từ khóa mở rộng + Chặn chéo + Tab Học hát)
                when (selectedInstrument) {
                    "Guitar" -> (titleLower.contains("guitar") || titleLower.contains("đệm") || titleLower.contains("hợp âm") || titleLower.contains("chord") || titleLower.contains("tab") || titleLower.contains("hướng dẫn")) && !titleLower.contains("ukulele") && !titleLower.contains("uku") && !titleLower.contains("piano") && !titleLower.contains("keyboard")

                    "Piano" -> (titleLower.contains("piano") || titleLower.contains("keyboard") || titleLower.contains("đệm") || titleLower.contains("hợp âm") || titleLower.contains("chord") || titleLower.contains("hướng dẫn") || titleLower.contains("sheet")) && !titleLower.contains("guitar") && !titleLower.contains("ukulele") && !titleLower.contains("uku")

                    "Ukulele" -> (titleLower.contains("ukulele") || titleLower.contains("uku") || titleLower.contains("đệm") || titleLower.contains("hợp âm") || titleLower.contains("chord") || titleLower.contains("hướng dẫn")) && !titleLower.contains("guitar") && !titleLower.contains("piano") && !titleLower.contains("keyboard")

                    "Học hát" -> titleLower.contains("học hát") && titleLower.contains(songTitleLower)

                    else -> true
                }
            }
        }

        isLoading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color(0xFF121212), dragHandle = { BottomSheetDefaults.DragHandle(color = tunaGreen) }) {
        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 700.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = CoreTranslator.getString("learn_to_play", currentLang), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)).padding(4.dp)) {
                instruments.forEach { inst ->
                    val isSelected = selectedInstrument == inst
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (isSelected) Color(0xFF2C2C2C) else Color.Transparent).clickable { selectedInstrument = inst }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text(text = inst, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }

            Text(text = CoreTranslator.getString("tut_no_video_warning", currentLang), color = Color(0xFFFFB300), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 40.dp), contentAlignment = Alignment.Center) {
                    AudioWaveLoadingIndicator(color = tunaGreen)
                }
            } else if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 40.dp), contentAlignment = Alignment.Center) {
                    Text(CoreTranslator.getString("no_results_found", currentLang), color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(videos) { video ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onVideoClick(video.videoId) }, verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.width(130.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray)) {
                                AsyncImage(model = "https://img.youtube.com/vi/${video.videoId}/mqdefault.jpg", contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(4.dp)) }
                                if (video.duration.isNotBlank()) { Text(text = video.duration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = video.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = video.channel, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
                                Text(text = video.views, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun PlayModeSelectionDialog(
    currentLang: String,
    tunaGreen: Color,
    bgColor: Color, // THÊM BIẾN NÀY VÀO KHAI BÁO
    surfaceColor: Color,
    textColor: Color,
    userRole: String = "user", // <-- THÊM QUYỀN VÀO ĐÂY ĐỂ KIỂM TRA
    onDismiss: () -> Unit,
    onSelectMode: (String, Boolean) -> Unit,
    songRepository: SongRepository? = null,
    onSongSelected: ((SongDetails) -> Unit)? = null
) {
    // === CÁC BIẾN QUẢN LÝ TAB ===
    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedSubTab by remember { mutableIntStateOf(0) }

    var offlineSongsList by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }

    // BỔ SUNG 2 BIẾN MAP NÀY VÀO RAM
    val viewCountMap = remember { mutableStateMapOf<String, Int>() }
    val thumbnailMap = remember { mutableStateMapOf<String, String>() }

    // === BIẾN QUẢN LÝ TÌM KIẾM TRONG DIALOG ===
    var dialogSearchQuery by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // THAY THẾ TOÀN BỘ LAUNCHED_EFFECT CŨ BẰNG ĐOẠN NÀY
    LaunchedEffect(Unit) {
        isLoadingList = true
        val rawList = SongScraper.getAvailableCachedSongs()

        val (sortedList, vMap, tMap) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            // KHẮC PHỤC LỖI PRIVATE: Khởi tạo trực tiếp bằng biến context
            val db = TunerDatabaseHelper(context).readableDatabase

            val localMapById = HashMap<String, Pair<Int, String>>()
            val localMapByTitle = HashMap<String, Pair<Int, String>>()

            // 1. QUÉT SQLITE ĐÚNG 1 LẦN DUY NHẤT THAY VÌ HÀNG TRĂM LẦN
            db?.rawQuery("SELECT id, title, youtubeUrl, viewCount FROM songs", null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex("id")
                val titleIdx = cursor.getColumnIndex("title")
                val ytIdx = cursor.getColumnIndex("youtubeUrl")
                val viewIdx = cursor.getColumnIndex("viewCount")

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: ""
                    val title = cursor.getString(titleIdx) ?: ""
                    val yt = cursor.getString(ytIdx) ?: ""
                    val views = cursor.getInt(viewIdx)

                    val data = Pair(views, yt)
                    if (id.isNotBlank()) localMapById[id] = data
                    if (title.isNotBlank()) localMapByTitle[title.trim().lowercase()] = data
                }
            }

            val tempVMap = HashMap<String, Int>()
            val tempTMap = HashMap<String, String>()

            // 2. SO KHỚP VÀ SẮP XẾP TRỰC TIẾP TRÊN RAM (O(1) Siêu Tốc)
            val enriched = rawList.map { item ->
                var views = 0
                var thumb = ""

                if (item.url.length == 11) {
                    thumb = "https://img.youtube.com/vi/${item.url}/mqdefault.jpg"
                    if (localMapById.containsKey(item.url)) {
                        views = localMapById[item.url]!!.first
                    } else {
                        val titleKey = item.title.trim().lowercase()
                        if (localMapByTitle.containsKey(titleKey)) views = localMapByTitle[titleKey]!!.first
                    }
                } else {
                    var foundData = localMapById[item.url]
                    if (foundData == null) {
                        val titleKey = item.title.trim().lowercase()
                        foundData = localMapByTitle[titleKey]
                    }

                    if (foundData != null) {
                        views = foundData.first
                        val ytUrl = foundData.second
                        if (ytUrl.isNotBlank()) {
                            var vId = ""
                            if (ytUrl.contains("watch?v=")) vId = ytUrl.substringAfter("watch?v=").substringBefore("&")
                            else if (ytUrl.contains("youtu.be/")) vId = ytUrl.substringAfter("youtu.be/").substringBefore("?")

                            if (vId.isNotBlank()) thumb = "https://img.youtube.com/vi/$vId/mqdefault.jpg"
                        }
                    }
                }

                tempVMap[item.url] = views
                tempTMap[item.url] = thumb

                Pair(item, views)
            }

            val sorted = enriched.sortedByDescending { it.second }
                .map { it.first }
                .distinctBy { it.title.trim().lowercase() }
            Triple(sorted, tempVMap, tempTMap)
        }

        // Đổ dữ liệu từ RAM lên UI
        viewCountMap.clear()
        viewCountMap.putAll(vMap)
        thumbnailMap.clear()
        thumbnailMap.putAll(tMap)

        offlineSongsList = sortedList
        isLoadingList = false
    }

    var isProcessing by remember { mutableStateOf(false) }

    val processSongSelection: (SearchResultItem) -> Unit = { item ->
        if (!isProcessing) {
            isProcessing = true
            onDismiss()

            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                val localSong = songRepository?.getSongByExactIdOrTitle(item.url, item.title, item.singer)
                if (localSong != null) {
                    // --- BẮT ĐẦU CẬP NHẬT: TĂNG LƯỢT XEM VÀ CẬP NHẬT TRÊN SERVER ---
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val sId = localSong.id
                        if (sId.isNotBlank()) {
                            // 1. Tăng view nội bộ SQLite
                            songRepository.incrementViewCountLocal(sId)

                            // 2. Gọi API để Server PHP tăng view trên MySQL
                            SongScraper.fetchSongDetails(sId, userRole) // Giả định userRole có thể dùng tạm thay cho email nếu không cần chính xác email
                        }
                    }
                    // --- KẾT THÚC CẬP NHẬT ---

                    // BẢN VÁ: Kéo biến isZing lên trên để kiểm tra trước
                    val isZing = item.url.length != 11

                    val detail = SongDetails(
                        title = localSong.title, singer = localSong.ca_si, composer = localSong.tac_gia,
                        baseTone = localSong.tong, rhythm = localSong.dieu.ifBlank { localSong.rhythm }, bpm = if (localSong.bpm > 0) localSong.bpm.toString() else "",
                        lyrics = localSong.lyrics.joinToString("\n"), videoUrl = localSong.youtubeUrl,
                        songId = localSong.id, slug = localSong.slug, imageCount = localSong.image_count,

                        // BẢN VÁ: ÉP CHẾT ID TỪ CACHE (item.url) NẾU LÀ NHẠC ZING, KHÔNG DÙNG ID CỦA SQLITE NỮA
                        zingMp3Id = if (isZing) item.url else localSong.zingMp3Id
                    )
                    onSongSelected?.invoke(detail)

                    kotlinx.coroutines.delay(400)
                    val actualMode = when (selectedSubTab) {
                        0 -> if (isZing) "ZING_MP3" else "LYRICS_ONLY"
                        // BẢN VÁ: Ép nhánh "Chỉ giọng ca sĩ" luôn chạy qua Phần 7 (Zing), bất kể nguồn bài hát
                        1 -> "ZING_ACAPELLA_NO_LYRICS"
                        2 -> if (isZing) "ZING_ACAPELLA_SYNC" else "ACAPELLA"
                        else -> "CHORD_AI"
                    }

                    onSelectMode(actualMode, true) // Truyền cờ true cho Kho nhạc
                } else {
                    Toast.makeText(context, "Không tìm thấy lời bài hát", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
            }
        }
    }

    // 1. CẤU HÌNH DIALOG HIỂN THỊ FULL MÀN HÌNH
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(0.dp),
            color = bgColor, // ĐÃ SỬA: Đổi nền toàn màn hình theo chế độ Sáng/Tối
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ===============================================
                // THANH MAIN TABS (TÍNH NĂNG AI | KHO NHẠC) VÀ NÚT HOME
                // ===============================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. NÚT HOME
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(46.dp)
                            .background(surfaceColor, RoundedCornerShape(12.dp)) // ĐÃ SỬA
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)) // ĐÃ SỬA
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Quay lại",
                            tint = textColor, // ĐÃ SỬA: Tự động đổi màu Trắng/Đen
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 2. THANH MAIN TABS
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(surfaceColor, RoundedCornerShape(12.dp)) // ĐÃ SỬA
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp)) // ĐÃ SỬA
                            .padding(4.dp)
                    ) {
                        val tabs = listOf("TÍNH NĂNG AI", "KHO NHẠC")
                        tabs.forEachIndexed { index, tabTitle ->
                            val isSelected = selectedMainTab == index

                            Surface(
                                modifier = Modifier.weight(1f).clickable { selectedMainTab = index },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) tunaGreen.copy(alpha = 0.15f) else Color.Transparent,
                                shadowElevation = 0.dp,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, tunaGreen.copy(alpha = 0.3f)) else null
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (index == 0) Icons.Default.AutoAwesome else Icons.Default.LibraryMusic,
                                            contentDescription = tabTitle,
                                            tint = if (isSelected) tunaGreen else Color.Gray,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(
                                            text = tabTitle,
                                            color = if (isSelected) tunaGreen else Color.Gray,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                if (selectedMainTab == 0) {
                    // ---------------------------------------------
                    // TAB 1: TÍNH NĂNG AI
                    // ---------------------------------------------
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        PlayModeOptionItem(
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Chạy chữ", tint = Color(0xFF00B0FF), modifier = Modifier.size(20.dp)) },
                            iconColor = Color(0xFF00B0FF),
                            title = "Chỉ chạy chữ", desc = "Bài hát lần đầu cần xử lý 15s-20s (Từ lần sau chỉ mất ~1 giây).",
                            textColor = textColor
                        ) { onSelectMode("ZING_MP3", false) }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        PlayModeOptionItem(
                            icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = "Giọng ca sĩ", tint = Color(0xFF9C27B0), modifier = Modifier.size(20.dp)) },
                            iconColor = Color(0xFF9C27B0),
                            title = CoreTranslator.getString("mode_vocal_only_title", currentLang),
                            desc = CoreTranslator.getString("mode_vocal_only_desc", currentLang),
                            textColor = textColor
                        ) { onSelectMode("ZING_ACAPELLA_NO_LYRICS", false) }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        PlayModeOptionItem(
                            icon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = "Zing", tint = Color(0xFFFF9100), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Singer", tint = Color(0xFFFF9100), modifier = Modifier.size(15.dp))
                                }
                            },
                            iconColor = Color(0xFFFF9100),
                            title = "Giọng hát + Chạy Chữ", desc = "Bài hát lần đầu tiên cần xử lý 80s~120s(từ lần sau chỉ mất 1s)",
                            textColor = textColor
                        ) { onSelectMode("ZING_ACAPELLA_SYNC", false) }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        PlayModeOptionItem(
                            icon = { ChordAIIcon(modifier = Modifier.size(22.dp)) },
                            iconColor = Color(0xFF00E676),
                            title = "Phân tích hợp âm", desc = "AI tự động phân tích hợp âm bài hát",
                            textColor = textColor
                        ) { onSelectMode("CHORD_AI", false) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .background(Color(0xFFFFB300).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(CoreTranslator.getString("ai_note_warning", currentLang), color = textColor.copy(alpha = 0.8f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                } else {
                    // ---------------------------------------------
                    // TAB 2: KHO NHẠC (DỮ LIỆU ĐÃ CACHE)
                    // ---------------------------------------------
                    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .background(bgColor, RoundedCornerShape(12.dp)) // ĐÃ SỬA: Khung chứa dùng bgColor (Xám nhạt ở Sáng, Đen ở Tối)
                                .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // TÍNH TOÁN SỐ LƯỢNG BÀI HÁT TỪNG TAB
                            val countChayChu = offlineSongsList.count { it.hasLyricsCache }
                            val countCaSi = offlineSongsList.count { it.hasAudioCache }
                            val countKaraoke = offlineSongsList.count { it.hasLyricsCache && it.hasAudioCache }
                            val countHopAm = offlineSongsList.count { it.chords.isNotEmpty() }

                            val subTabs = listOf(
                                "Chạy Chữ ($countChayChu)",
                                "Ca Sĩ ($countCaSi)",
                                "Karaoke ($countKaraoke)",
                                "Hợp Âm ($countHopAm)"
                            )

                            subTabs.forEachIndexed { index, title ->
                                val isSelected = selectedSubTab == index
                                val tintColor = if (isSelected) tunaGreen else Color.Gray

                                Surface(
                                    modifier = Modifier.weight(1f).clickable { selectedSubTab = index },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) textColor.copy(alpha = 0.15f) else Color.Transparent,
                                    shadowElevation = 0.dp,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.3f)) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 4.dp), // Thu hẹp padding của nút
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        val icon = when (index) {
                                            0 -> Icons.Default.Mic
                                            1 -> Icons.Default.RecordVoiceOver
                                            2 -> Icons.Default.LibraryMusic
                                            else -> Icons.Default.GridOn
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            tint = tintColor,
                                            modifier = Modifier.size(16.dp) // Icon thu nhỏ lại
                                        )

                                        // Tách dòng
                                        val parts = title.split(" (")
                                        val mainTitle = parts.getOrNull(0) ?: title
                                        val countPart = if (parts.size > 1) "(${parts[1]}" else ""

                                        Text(
                                            text = mainTitle,
                                            color = tintColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            lineHeight = 12.sp,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp) // Giảm khoảng cách giữa Icon và Text
                                        )

                                        if (countPart.isNotEmpty()) {
                                            Text(
                                                text = countPart,
                                                color = tintColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                lineHeight = 12.sp,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))

                        // =======================================================
                        // 3. Ô TÌM KIẾM TRONG DIALOG KHO NHẠC
                        // =======================================================
                        BasicTextField(
                            value = dialogSearchQuery,
                            onValueChange = { dialogSearchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(44.dp)
                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp),
                            textStyle = TextStyle(color = textColor, fontSize = 14.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(tunaGreen),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (dialogSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Tìm kiếm bài hát...",
                                                color = Color.Gray.copy(alpha = 0.9f),
                                                fontSize = 14.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                    if (dialogSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { dialogSearchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Xóa",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // =======================================================
                        // 4. BỘ LỌC DANH SÁCH THEO TAB VÀ TỪ KHÓA
                        // =======================================================
                        val filteredList = offlineSongsList.filter { song ->
                            val matchSearch = if (dialogSearchQuery.isBlank()) true else {
                                val queryClean = removeAccentsAndSpecialChars(dialogSearchQuery)
                                val titleClean = removeAccentsAndSpecialChars(song.title)
                                val singerClean = removeAccentsAndSpecialChars(song.singer)

                                titleClean.contains(queryClean, ignoreCase = true) ||
                                        singerClean.contains(queryClean, ignoreCase = true)
                            }
                            val matchTab = when (selectedSubTab) {
                                0 -> song.hasLyricsCache
                                1 -> song.hasAudioCache
                                2 -> song.hasLyricsCache && song.hasAudioCache
                                3 -> song.chords.isNotEmpty() // Tab Hợp Âm
                                else -> true
                            }
                            matchSearch && matchTab
                        }

                        // =======================================================
                        // 5. HIỂN THỊ DANH SÁCH HOẶC TRẠNG THÁI TRỐNG
                        // =======================================================
                        if (isLoadingList) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = tunaGreen)
                            }
                        } else if (filteredList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Không có bài hát nào phù hợp.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                // ĐÃ SỬA LỖI ĐỎ BẰNG CÁCH ĐỔI TÊN BIẾN THÀNH 'songItem'
                                items(filteredList) { songItem ->

                                    // Lấy dữ liệu ngay từ RAM (Map), KHÔNG DÙNG LaunchedEffect cày ổ cứng nữa
                                    val thumbnailUrl = thumbnailMap[songItem.url] ?: ""
                                    val localViewCount = viewCountMap[songItem.url] ?: 0

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 16.dp)
                                            .background(surfaceColor, RoundedCornerShape(12.dp))
                                            .clickable { processSongSelection(songItem) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray)
                                            if (thumbnailUrl.isNotBlank()) {
                                                coil.compose.AsyncImage(
                                                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                        .data(thumbnailUrl)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = songItem.title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = songItem.singer, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }

                                        // ==========================================
                                        // NÚT XÓA ĐỘC QUYỀN CHO SUPER ADMIN (CHỈ XÓA CACHE)
                                        // ==========================================
                                        if (userRole.equals("super_admin", ignoreCase = true) || userRole.equals("supper_admin", ignoreCase = true)) {
                                            var showDeleteConfirm by remember { mutableStateOf(false) }

                                            IconButton(
                                                onClick = { showDeleteConfirm = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xóa dữ liệu tải",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            if (showDeleteConfirm) {
                                                AlertDialog(
                                                    onDismissRequest = { showDeleteConfirm = false },
                                                    title = { Text(try { CoreTranslator.getString("delete_cache_title", currentLang) } catch(e:Exception){"Dọn dẹp Kho nhạc"}, color = tunaGreen, fontWeight = FontWeight.Bold) },
                                                    text = { Text("${try { CoreTranslator.getString("delete_cache_confirm", currentLang) } catch(e:Exception){"Bạn có muốn xóa dữ liệu đã tải của bài hát"}}\n'${songItem.title}'?", color = textColor) },
                                                    containerColor = surfaceColor,
                                                    confirmButton = {
                                                        TextButton(onClick = {
                                                            showDeleteConfirm = false
                                                            coroutineScope.launch {
                                                                // CHỈ XÓA FILE CACHE VẬT LÝ TRÊN MÁY CHỦ PYTHON (KHÔNG XÓA DB GỐC)
                                                                val successCache = SongScraper.deleteServerCachePython(songItem.url)

                                                                // Đánh giá thành công
                                                                if (successCache) {
                                                                    Toast.makeText(context, try { CoreTranslator.getString("delete_cache_success", currentLang) } catch(e:Exception){"Đã dọn dẹp dữ liệu bài hát trong kho"}, Toast.LENGTH_SHORT).show()
                                                                    // Làm mới lại danh sách
                                                                    isLoadingList = true
                                                                    offlineSongsList = SongScraper.getAvailableCachedSongs()
                                                                        .distinctBy { it.title.trim().lowercase() }
                                                                    isLoadingList = false
                                                                } else {
                                                                    Toast.makeText(context, try { CoreTranslator.getString("delete_error", currentLang) } catch(e:Exception){"Lỗi khi xóa dữ liệu"}, Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }) {
                                                            Text(try { CoreTranslator.getString("delete", currentLang) } catch(e:Exception){"Xóa"}, color = Color.Red, fontWeight = FontWeight.Bold)
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { showDeleteConfirm = false }) {
                                                            Text(try { CoreTranslator.getString("cancel", currentLang) } catch(e:Exception){"Hủy"}, color = Color.Gray)
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        // --- BẮT ĐẦU ĐOẠN THÊM MỚI: Lượt xem và Icon mắt ---
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "Lượt xem",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = localViewCount.toString(),
                                                color = Color.Gray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        // --- KẾT THÚC ĐOẠN THÊM MỚI ---
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
fun PlayModeOptionItem(
    icon: @Composable () -> Unit,
    iconColor: Color,
    title: String,
    desc: String,
    textColor: Color,
    hasSwitch: Boolean = false,
    switchState: Boolean = false,
    onSwitchChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.width(14.dp))

        // Cột Text
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, color = Color.Gray, fontSize = 12.sp)
        }

        if (hasSwitch) {
            Switch(
                checked = switchState,
                onCheckedChange = { onSwitchChange?.invoke(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = iconColor, checkedTrackColor = iconColor.copy(alpha = 0.5f))
            )
        }
    }
}
@Composable
fun AudioPlayerBar(
    currentMs: Long,
    totalMs: Long,
    isPlaying: Boolean,
    tunaGreen: Color,
    textColor: Color,
    surfaceColor: Color,
    vocalStartMs: Long?,
    detectedVocalStartMs: Long?,
    onVocalStartChange: (Long) -> Unit,
    onPrepareCountIn: () -> Unit,
    onCountInPlay: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    fun formatTime(ms: Long): String {
        val seconds = ms.coerceAtLeast(0L) / 1000L
        return String.format(java.util.Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L)
    }

    fun parseTime(text: String): Long? {
        val parts = text.trim().split(":")
        val seconds = when (parts.size) {
            1 -> parts[0].toLongOrNull()
            2 -> {
                val minute = parts[0].toLongOrNull() ?: return null
                val second = parts[1].toLongOrNull() ?: return null
                if (second !in 0L..59L) return null
                minute * 60L + second
            }
            else -> return null
        } ?: return null
        if (seconds < 0L || seconds > 86_400L) return null
        return seconds * 1000L
    }

    var dragPosition by remember { mutableFloatStateOf(-1f) }
    var isSeeking by remember { mutableStateOf(false) }
    var smoothedMs by remember { mutableLongStateOf(0L) }
    var showIntroSettings by remember { mutableStateOf(false) }
    var inputTime by remember { mutableStateOf(formatTime(vocalStartMs ?: detectedVocalStartMs ?: 0L)) }
    var invalidTime by remember { mutableStateOf(false) }
    var countdownNumber by remember { mutableIntStateOf(0) }
    var countJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Keep the user-dragged position stable until MediaPlayer completes seeking.
    LaunchedEffect(currentMs) {
        if (!isSeeking && dragPosition == -1f) {
            if (currentMs >= smoothedMs || (smoothedMs - currentMs) > 1000L) {
                smoothedMs = currentMs
            }
        }
    }
    val displayPosition = if (dragPosition >= 0f) dragPosition else smoothedMs.toFloat()

    Row(
        modifier = Modifier.fillMaxWidth().background(surfaceColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(if (isPlaying) Color.Red.copy(alpha = 0.15f) else tunaGreen.copy(alpha = 0.15f))
                .clickable(enabled = countdownNumber == 0) { onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Phát / tạm dừng",
                tint = if (isPlaying) Color.Red else tunaGreen,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = formatTime(displayPosition.toLong()),
            modifier = Modifier.width(42.dp),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )

        // The seekbar remains seekable but is shorter to make room for two actions.
        Slider(
            value = displayPosition.coerceIn(0f, totalMs.coerceAtLeast(1L).toFloat()),
            onValueChange = { dragPosition = it },
            onValueChangeFinished = {
                val targetMs = dragPosition.coerceAtLeast(0f).toLong()
                onSeek(targetMs)
                coroutineScope.launch {
                    isSeeking = true
                    smoothedMs = targetMs
                    delay(800)
                    dragPosition = -1f
                    isSeeking = false
                }
            },
            valueRange = 0f..totalMs.coerceAtLeast(1L).toFloat(),
            enabled = countdownNumber == 0,
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(30.dp),
            colors = SliderDefaults.colors(
                thumbColor = tunaGreen,
                activeTrackColor = tunaGreen,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )

        // Countdown: pause first, show 3-2-1, then start exactly five seconds before the vocal marker.
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape)
                .clickable(enabled = countdownNumber == 0 && totalMs > 0L) {
                    if (vocalStartMs == null) {
                        inputTime = formatTime(detectedVocalStartMs ?: 0L)
                        invalidTime = false
                        showIntroSettings = true
                    } else {
                        onPrepareCountIn()
                        countJob?.cancel()
                        val beginMs = (vocalStartMs - 5_000L).coerceAtLeast(0L)
                        countJob = coroutineScope.launch {
                            for (n in 3 downTo 1) {
                                countdownNumber = n
                                delay(1000L)
                            }
                            countdownNumber = 0
                            onCountInPlay(beginMs)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Đếm ngược 3-2-1 rồi phát trước giọng hát 5 giây",
                tint = tunaGreen, modifier = Modifier.size(21.dp)
            )
        }

        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).clickable(enabled = countdownNumber == 0) {
                inputTime = formatTime(vocalStartMs ?: detectedVocalStartMs ?: 0L)
                invalidTime = false
                showIntroSettings = true
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Cài đặt thời gian bỏ qua dạo đầu",
                tint = if (vocalStartMs != null) tunaGreen else Color.Gray,
                modifier = Modifier.size(21.dp)
            )
        }

        Text(
            text = formatTime(totalMs),
            modifier = Modifier.width(42.dp),
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            maxLines = 1,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")
        )
    }

    if (showIntroSettings) {
        AlertDialog(
            onDismissRequest = { showIntroSettings = false },
            title = { Text("Bỏ qua dạo đầu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Đặt mốc ca sĩ bắt đầu hát hoặc điểm kết thúc đoạn dạo đầu.",
                        fontSize = 13.sp)
                    OutlinedTextField(
                        value = inputTime,
                        onValueChange = {
                            if (it.length <= 8 && it.all { c -> c.isDigit() || c == ':' }) {
                                inputTime = it
                                invalidTime = false
                            }
                        },
                        singleLine = true,
                        label = { Text("Thời điểm (mm:ss)") },
                        placeholder = { Text("00:20") },
                        isError = invalidTime,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (invalidTime) {
                        Text("Nhập thời gian hợp lệ, ví dụ 00:20.", color = Color.Red, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { inputTime = formatTime(currentMs); invalidTime = false }) {
                            Text("Lấy vị trí hiện tại", fontSize = 12.sp)
                        }
                        if (detectedVocalStartMs != null) {
                            TextButton(onClick = {
                                inputTime = formatTime(detectedVocalStartMs)
                                invalidTime = false
                            }) { Text("Theo lời đồng bộ", fontSize = 12.sp) }
                        }
                    }
                    val proposedMs = parseTime(inputTime)
                    Text(
                        if (proposedMs != null) {
                            "Sau đếm ngược 3–2–1, nhạc phát từ ${formatTime((proposedMs - 5_000L).coerceAtLeast(0L))}, " +
                                "giọng hát bắt đầu tại ${formatTime(proposedMs)}."
                        } else {
                            "Khi đếm ngược kết thúc, nhạc bắt đầu trước giọng hát 5 giây."
                        },
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val settingMs = parseTime(inputTime)
                    if (settingMs == null || (totalMs > 0L && settingMs > totalMs)) {
                        invalidTime = true
                    } else {
                        onVocalStartChange(settingMs)
                        showIntroSettings = false
                    }
                }) { Text("Lưu", color = tunaGreen) }
            },
            dismissButton = { TextButton(onClick = { showIntroSettings = false }) { Text("Hủy") } }
        )
    }

    if (countdownNumber > 0) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            // Chỉ hiển thị vòng tròn trong suốt và số đếm ngược.
            Box(
                modifier = Modifier
                    .size(142.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(
                        width = 3.dp,
                        color = tunaGreen.copy(alpha = 0.9f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownNumber.toString(),
                    color = tunaGreen,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
fun AiProcessingStepper(
    currentStep: Int,
    stepProgress: Float,
    statusText: String,
    tunaGreen: Color,
    textColor: Color,
    stepTitles: List<String> = listOf("Tải bản ghi âm gốc", "AI tách giọng ca sĩ", "Đồng bộ hợp âm")
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        stepTitles.forEachIndexed { index, title ->
            val isCompleted = index < currentStep
            val isCurrent = index == currentStep

            Row(verticalAlignment = Alignment.Top) {
                // Cột chứa vòng tròn và đường thẳng
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Vòng tròn trạng thái (Tăng lên 44.dp để chứa % rộng rãi hơn)
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            // Bước đã xong: Tròn đặc Xanh lá + Dấu check
                            Box(
                                modifier = Modifier.size(44.dp).background(tunaGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        } else if (isCurrent) {
                            // Vòng xám nền
                            Box(modifier = Modifier.size(44.dp).border(3.dp, Color.Gray.copy(alpha = 0.2f), CircleShape))
                            // Vòng xanh tiến trình đè lên
                            CircularProgressIndicator(
                                progress = stepProgress,
                                color = tunaGreen,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            // % LỌT THỎM CHÍNH GIỮA VÒNG TRÒN
                            Text(
                                text = "${(stepProgress * 100).toInt()}%",
                                color = tunaGreen,
                                fontSize = 13.sp, // Chữ to rõ ràng hơn
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // Bước chưa tới: Viền xám nhẹ, nền xám siêu mờ
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.05f), CircleShape)
                            )
                        }
                    }

                    // Đường kẻ dọc nối các bước
                    if (index < stepTitles.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(38.dp) // Kéo dài đường nối cho cân đối với vòng tròn 44dp
                                .background(if (isCompleted) tunaGreen else Color.Gray.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Cột chứa Text Title
                Column(modifier = Modifier.padding(top = 11.dp)) { // Đẩy text xuống giữa tâm vòng tròn
                    Text(
                        text = title,
                        color = if (isCompleted || isCurrent) textColor else Color.Gray.copy(alpha = 0.6f),
                        fontWeight = if (isCompleted || isCurrent) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    )
                    if (isCurrent && statusText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // XÓA PHẦN TRĂM DƯ THỪA TRONG CÂU TEXT BÊN DƯỚI BẰNG REGEX
                        val cleanStatusText = statusText
                            .replace(Regex("\\(\\d+%\\)"), "") // Xóa định dạng "(60%)"
                            .replace(Regex("\\d+%"), "")       // Xóa định dạng "60%"
                            .replace("  ", " ")                // Dọn dẹp khoảng trắng kép nếu có
                            .trim()

                        Text(
                            text = cleanStatusText,
                            color = tunaGreen,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}