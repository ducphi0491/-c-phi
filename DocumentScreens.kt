package com.docflowoffline

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.text.Editable
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// ================ DocumentScreens.kt (Word/PPT/PDF/Text) ================
internal typealias Save = (String, String, Boolean, (Context, OutputStream) -> Unit) -> Unit

@Composable
internal fun DocumentScreen(
    document: DocumentStore.OpenFile,
    requestSave: Save,
    notify: (String) -> Unit,
    onBack: () -> Unit
) {
    when (document) {
        is DocumentStore.OpenFile.Word -> WordEditor(document, requestSave, notify)
        is DocumentStore.OpenFile.Excel -> ExcelEditor(document, requestSave, notify, onBack)
        is DocumentStore.OpenFile.Slides -> PowerPointEditor(document, requestSave, notify)
        is DocumentStore.OpenFile.Pdf -> PdfScreen(document, requestSave, notify)
        is DocumentStore.OpenFile.Text -> TextEditor(document, requestSave, notify)
    }
}

@Composable
private fun EditorInfo(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DocumentZoomBar(
    zoom: Float,
    minimum: Float,
    maximum: Float,
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Zoom",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalIconButton(
            onClick = { onZoom((zoom - .25f).coerceAtLeast(minimum)) },
            enabled = zoom > minimum + .01f,
            modifier = Modifier.size(35.dp)
        ) { Icon(Icons.Default.Remove, "Thu nhỏ", Modifier.size(18.dp)) }
        Text(
            "${(zoom * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(min = 47.dp)
        )
        FilledTonalIconButton(
            onClick = { onZoom((zoom + .25f).coerceAtMost(maximum)) },
            enabled = zoom < maximum - .01f,
            modifier = Modifier.size(35.dp)
        ) { Icon(Icons.Default.Add, "Phóng to", Modifier.size(18.dp)) }
        TextButton(onClick = { onZoom(1f) }, enabled = zoom != 1f) { Text("100%") }
    }
}

// ---------------- WORD ----------------

private class WordHistory {
    private val undo = ArrayDeque<SpannableStringBuilder>()
    private val redo = ArrayDeque<SpannableStringBuilder>()
    var restoring: Boolean = false

    fun capture(value: CharSequence, force: Boolean = false) {
        if (restoring) return
        val snapshot = SpannableStringBuilder(value)
        if (!force && undo.peekLast()?.toString() == snapshot.toString()) return
        undo.addLast(snapshot)
        while (undo.size > 60) undo.removeFirst()
        redo.clear()
    }

    fun undo(editor: EditText): Boolean {
        val value = undo.pollLast() ?: return false
        redo.addLast(SpannableStringBuilder(editor.text))
        restore(editor, value)
        return true
    }

    fun redo(editor: EditText): Boolean {
        val value = redo.pollLast() ?: return false
        undo.addLast(SpannableStringBuilder(editor.text))
        restore(editor, value)
        return true
    }

    private fun restore(editor: EditText, value: SpannableStringBuilder) {
        restoring = true
        editor.setText(value, TextView.BufferType.SPANNABLE)
        editor.setSelection(editor.text.length)
        restoring = false
    }
}

private fun richWordToSpannable(model: OfficeOoxml.RichWord): SpannableStringBuilder {
    val result = SpannableStringBuilder(model.text)
    model.spans.forEach { span ->
        val start = span.start.coerceIn(0, result.length)
        val end = span.end.coerceIn(start, result.length)
        if (end <= start) return@forEach
        if (span.bold) result.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (span.italic) result.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (span.underline) result.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.fontRgb?.let { runCatching { AndroidColor.parseColor("#$it") }.getOrNull() }
            ?.let { result.setSpan(ForegroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        span.highlightRgb?.let { runCatching { AndroidColor.parseColor("#$it") }.getOrNull() }
            ?.let { result.setSpan(BackgroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
        span.fontSizeSp?.let {
            result.setSpan(AbsoluteSizeSpan(it.coerceIn(6, 96), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
    model.paragraphs.forEach { paragraph ->
        val start = paragraph.start.coerceIn(0, result.length)
        var end = paragraph.end.coerceIn(start, result.length)
        if (end < result.length && result[end] == '\n') end++
        if (end <= start) return@forEach
        val alignment = when (paragraph.alignment) {
            "center" -> Layout.Alignment.ALIGN_CENTER
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            "justify" -> Layout.Alignment.ALIGN_NORMAL
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        if (paragraph.alignment != "left") {
            result.setSpan(AlignmentSpan.Standard(alignment), start, end, Spanned.SPAN_PARAGRAPH)
        }
    }
    return result
}

private fun EditText.toRichWordModel(): OfficeOoxml.RichWord {
    val value = text
    val length = value.length
    val boundaries = sortedSetOf(0, length)
    fun collectRanges(spans: Array<out Any>) {
        spans.forEach {
            boundaries += value.getSpanStart(it).coerceIn(0, length)
            boundaries += value.getSpanEnd(it).coerceIn(0, length)
        }
    }
    collectRanges(value.getSpans(0, length, StyleSpan::class.java))
    collectRanges(value.getSpans(0, length, UnderlineSpan::class.java))
    collectRanges(value.getSpans(0, length, ForegroundColorSpan::class.java))
    collectRanges(value.getSpans(0, length, BackgroundColorSpan::class.java))
    collectRanges(value.getSpans(0, length, AbsoluteSizeSpan::class.java))

    val points = boundaries.toList()
    val spans = mutableListOf<OfficeOoxml.WordSpan>()
    for (i in 0 until points.lastIndex) {
        val start = points[i]
        val end = points[i + 1]
        if (end <= start) continue
        val styleSpans = value.getSpans(start, end, StyleSpan::class.java)
        val bold = styleSpans.any { it.style == Typeface.BOLD || it.style == Typeface.BOLD_ITALIC }
        val italic = styleSpans.any { it.style == Typeface.ITALIC || it.style == Typeface.BOLD_ITALIC }
        val underline = value.getSpans(start, end, UnderlineSpan::class.java).isNotEmpty()
        val fontColor = value.getSpans(start, end, ForegroundColorSpan::class.java).lastOrNull()?.foregroundColor
        val background = value.getSpans(start, end, BackgroundColorSpan::class.java).lastOrNull()?.backgroundColor
        val size = value.getSpans(start, end, AbsoluteSizeSpan::class.java).lastOrNull()?.size
        if (bold || italic || underline || fontColor != null || background != null || size != null) {
            spans += OfficeOoxml.WordSpan(
                start,
                end,
                bold,
                italic,
                underline,
                fontColor?.let { String.format(Locale.ROOT, "%06X", it and 0xFFFFFF) },
                background?.let { String.format(Locale.ROOT, "%06X", it and 0xFFFFFF) },
                size
            )
        }
    }

    val paragraphs = mutableListOf<OfficeOoxml.WordParagraphFormat>()
    var start = 0
    for (index in 0..length) {
        if (index == length || value[index] == '\n') {
            val queryEnd = when {
                index > start -> index
                index < length -> index + 1
                start > 0 -> start
                else -> 0
            }
            val alignmentSpan = if (queryEnd > start) {
                value.getSpans(start, queryEnd, AlignmentSpan::class.java).lastOrNull()
            } else null
            val alignment = when (alignmentSpan?.alignment) {
                Layout.Alignment.ALIGN_CENTER -> "center"
                Layout.Alignment.ALIGN_OPPOSITE -> "right"
                else -> "left"
            }
            paragraphs += OfficeOoxml.WordParagraphFormat(start, index, alignment)
            start = index + 1
        }
    }
    return OfficeOoxml.RichWord(value.toString(), spans, paragraphs, true, null)
}

private fun selectedTextRange(editor: EditText, notify: (String) -> Unit): Pair<Int, Int>? {
    val start = minOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
    val end = maxOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
    if (end <= start) {
        notify("Hãy nhấn giữ và kéo hai tay nắm để bôi chọn nội dung trước.")
        return null
    }
    return start to end
}

private fun cloneSpan(span: Any): Any? = when (span) {
    is StyleSpan -> StyleSpan(span.style)
    is UnderlineSpan -> UnderlineSpan()
    is ForegroundColorSpan -> ForegroundColorSpan(span.foregroundColor)
    is BackgroundColorSpan -> BackgroundColorSpan(span.backgroundColor)
    is AbsoluteSizeSpan -> AbsoluteSizeSpan(span.size, span.dip)
    is AlignmentSpan.Standard -> AlignmentSpan.Standard(span.alignment)
    else -> null
}

private fun removeSpanInside(value: Editable, span: Any, start: Int, end: Int) {
    val spanStart = value.getSpanStart(span)
    val spanEnd = value.getSpanEnd(span)
    val flags = value.getSpanFlags(span)
    value.removeSpan(span)
    if (spanStart < start) cloneSpan(span)?.let { value.setSpan(it, spanStart, start, flags) }
    if (spanEnd > end) cloneSpan(span)?.let { value.setSpan(it, end, spanEnd, flags) }
}

private fun toggleWordStyle(
    editor: EditText,
    history: WordHistory,
    notify: (String) -> Unit,
    style: Int
) {
    val (start, end) = selectedTextRange(editor, notify) ?: return
    history.capture(editor.text, force = true)
    val matches = editor.text.getSpans(start, end, StyleSpan::class.java)
        .filter { it.style == style || it.style == Typeface.BOLD_ITALIC }
    if (matches.isNotEmpty()) {
        matches.forEach { span ->
            val spanStart = editor.text.getSpanStart(span)
            val spanEnd = editor.text.getSpanEnd(span)
            val flags = editor.text.getSpanFlags(span)
            removeSpanInside(editor.text, span, start, end)
            if (span.style == Typeface.BOLD_ITALIC) {
                val remaining = if (style == Typeface.BOLD) Typeface.ITALIC else Typeface.BOLD
                val insideStart = maxOf(start, spanStart)
                val insideEnd = minOf(end, spanEnd)
                if (insideEnd > insideStart) editor.text.setSpan(StyleSpan(remaining), insideStart, insideEnd, flags)
            }
        }
    } else {
        editor.text.setSpan(StyleSpan(style), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    editor.invalidate()
}

private fun toggleWordUnderline(editor: EditText, history: WordHistory, notify: (String) -> Unit) {
    val (start, end) = selectedTextRange(editor, notify) ?: return
    history.capture(editor.text, force = true)
    val spans = editor.text.getSpans(start, end, UnderlineSpan::class.java)
    if (spans.isNotEmpty()) spans.forEach { removeSpanInside(editor.text, it, start, end) }
    else editor.text.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    editor.invalidate()
}

private fun applyWordColor(
    editor: EditText,
    history: WordHistory,
    notify: (String) -> Unit,
    color: Int,
    background: Boolean
) {
    val (start, end) = selectedTextRange(editor, notify) ?: return
    history.capture(editor.text, force = true)
    if (background) {
        editor.text.getSpans(start, end, BackgroundColorSpan::class.java)
            .forEach { removeSpanInside(editor.text, it, start, end) }
        editor.text.setSpan(BackgroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else {
        editor.text.getSpans(start, end, ForegroundColorSpan::class.java)
            .forEach { removeSpanInside(editor.text, it, start, end) }
        editor.text.setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    editor.invalidate()
}

private fun changeWordFontSize(
    editor: EditText,
    history: WordHistory,
    notify: (String) -> Unit,
    delta: Int
) {
    val (start, end) = selectedTextRange(editor, notify) ?: return
    history.capture(editor.text, force = true)
    val current = editor.text.getSpans(start, end, AbsoluteSizeSpan::class.java).lastOrNull()?.size ?: 16
    editor.text.getSpans(start, end, AbsoluteSizeSpan::class.java)
        .forEach { removeSpanInside(editor.text, it, start, end) }
    editor.text.setSpan(
        AbsoluteSizeSpan((current + delta).coerceIn(8, 72), true),
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    editor.invalidate()
}

private fun paragraphRange(editor: EditText): Pair<Int, Int> {
    val text = editor.text
    val selectionStart = minOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
    val selectionEnd = maxOf(editor.selectionStart, editor.selectionEnd).coerceAtLeast(0)
    var start = selectionStart.coerceAtMost(text.length)
    while (start > 0 && text[start - 1] != '\n') start--
    var end = selectionEnd.coerceAtMost(text.length)
    while (end < text.length && text[end] != '\n') end++
    if (end < text.length) end++
    return start to end
}

private fun applyWordAlignment(editor: EditText, history: WordHistory, alignment: Layout.Alignment) {
    val (start, end) = paragraphRange(editor)
    history.capture(editor.text, force = true)
    editor.text.getSpans(start, end, AlignmentSpan::class.java)
        .forEach { removeSpanInside(editor.text, it, start, end) }
    if (end > start) editor.text.setSpan(AlignmentSpan.Standard(alignment), start, end, Spanned.SPAN_PARAGRAPH)
    editor.invalidate()
}

private fun applyWordList(editor: EditText, history: WordHistory, numbered: Boolean) {
    val (start, end) = paragraphRange(editor)
    history.capture(editor.text, force = true)
    val raw = editor.text.substring(start, end).removeSuffix("\n")
    val lines = raw.split('\n')
    val prefixRegex = Regex("^\\s*(?:[•-]|\\d+[.)])\\s+")
    val converted = lines.mapIndexed { index, line ->
        val clean = line.replaceFirst(prefixRegex, "")
        if (numbered) "${index + 1}. $clean" else "• $clean"
    }.joinToString("\n") + if (end > 0 && end <= editor.text.length && editor.text[end - 1] == '\n') "\n" else ""
    editor.text.replace(start, end, converted)
    editor.setSelection(start, (start + converted.length).coerceAtMost(editor.text.length))
}

private fun clearWordFormatting(editor: EditText, history: WordHistory, notify: (String) -> Unit) {
    val (start, end) = selectedTextRange(editor, notify) ?: return
    history.capture(editor.text, force = true)
    editor.text.getSpans(start, end, StyleSpan::class.java).forEach { removeSpanInside(editor.text, it, start, end) }
    editor.text.getSpans(start, end, UnderlineSpan::class.java).forEach { removeSpanInside(editor.text, it, start, end) }
    editor.text.getSpans(start, end, ForegroundColorSpan::class.java).forEach { removeSpanInside(editor.text, it, start, end) }
    editor.text.getSpans(start, end, BackgroundColorSpan::class.java).forEach { removeSpanInside(editor.text, it, start, end) }
    editor.text.getSpans(start, end, AbsoluteSizeSpan::class.java).forEach { removeSpanInside(editor.text, it, start, end) }
    editor.invalidate()
}

private fun findNextInWord(editor: EditText, query: String, from: Int = editor.selectionEnd): Boolean {
    if (query.isBlank()) return false
    val source = editor.text.toString()
    var found = source.indexOf(query, from.coerceIn(0, source.length), ignoreCase = true)
    if (found < 0 && from > 0) found = source.indexOf(query, 0, ignoreCase = true)
    if (found < 0) return false
    editor.requestFocus()
    editor.setSelection(found, found + query.length)
    return true
}

@Composable
private fun WordColorMenu(
    label: String,
    background: Boolean,
    enabled: Boolean = true,
    onColor: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = remember {
        listOf(
            AndroidColor.BLACK,
            AndroidColor.rgb(25, 82, 180),
            AndroidColor.rgb(198, 40, 40),
            AndroidColor.rgb(46, 125, 50),
            AndroidColor.rgb(123, 31, 162),
            AndroidColor.rgb(255, 235, 59),
            AndroidColor.rgb(255, 183, 77),
            AndroidColor.rgb(178, 235, 242)
        )
    }
    Box {
        AssistChip(
            onClick = { if (enabled) expanded = true },
            enabled = enabled,
            label = { Text(label) },
            leadingIcon = {
                Icon(if (background) Icons.Default.FormatColorFill else Icons.Default.FormatColorText, null)
            }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            colors.forEach { color ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(24.dp).background(Color(color), RoundedCornerShape(6.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(String.format(Locale.ROOT, "#%06X", color and 0xFFFFFF))
                        }
                    },
                    onClick = { expanded = false; onColor(color) }
                )
            }
        }
    }
}

@Composable
private fun WordEditor(
    document: DocumentStore.OpenFile.Word,
    save: Save,
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val model = remember(document.uri) { OfficeOoxml.readWordRich(document.bytes) }
    val initialText = remember(document.uri) { richWordToSpannable(model) }
    val history = remember(document.uri) { WordHistory() }
    var editor by remember(document.uri) { mutableStateOf<EditText?>(null) }
    var zoom by remember(document.uri) { mutableFloatStateOf(1f) }
    var showFind by remember(document.uri) { mutableStateOf(false) }
    var findText by remember(document.uri) { mutableStateOf("") }
    var replaceText by remember(document.uri) { mutableStateOf("") }
    var toolsExpanded by remember(document.uri) { mutableStateOf(true) }

    val surfaceColor = MaterialTheme.colorScheme.surface.toArgb()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                if (model.editable) "Word offline · bôi chọn và định dạng" else "Word · chế độ chỉ đọc",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { history.undo(editor ?: return@IconButton) }) {
                Icon(Icons.Default.Undo, "Hoàn tác")
            }
            IconButton(onClick = { history.redo(editor ?: return@IconButton) }) {
                Icon(Icons.Default.Redo, "Làm lại")
            }
            IconButton(onClick = { showFind = true }) { Icon(Icons.Default.Search, "Tìm và thay thế") }
            IconButton(
                onClick = {
                    val current = editor ?: return@IconButton
                    val rich = current.toRichWordModel()
                    save(
                        DocumentStore.suggestedCopyName(document.name, "docx"),
                        DocumentStore.DOCX,
                        true
                    ) { _, out -> out.write(OfficeOoxml.writeWordRich(document.bytes, rich)) }
                },
                enabled = model.editable
            ) { Icon(Icons.Default.Save, "Lưu bản sao") }
        }

        if (model.reason != null) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) { EditorInfo(model.reason) }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentZoomBar(zoom, .65f, 2.25f, onZoom = { zoom = it }, modifier = Modifier.weight(1f))
            TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
                Icon(Icons.Default.Build, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Công cụ")
            }
        }

        if (toolsExpanded) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = { editor?.onTextContextMenuItem(android.R.id.copy) }, label = { Text("Sao chép") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                AssistChip(onClick = { if (model.editable) editor?.onTextContextMenuItem(android.R.id.cut) },
                    label = { Text("Cắt") }, leadingIcon = { Icon(Icons.Default.ContentCut, null) }, enabled = model.editable)
                AssistChip(onClick = { if (model.editable) editor?.onTextContextMenuItem(android.R.id.paste) },
                    label = { Text("Dán") }, leadingIcon = { Icon(Icons.Default.ContentPaste, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.selectAll() }, label = { Text("Chọn tất cả") },
                    leadingIcon = { Icon(Icons.Default.SelectAll, null) })
                AssistChip(onClick = { editor?.let { toggleWordStyle(it, history, notify, Typeface.BOLD) } },
                    label = { Text("Đậm") }, leadingIcon = { Text("B", fontWeight = FontWeight.Black) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { toggleWordStyle(it, history, notify, Typeface.ITALIC) } },
                    label = { Text("Nghiêng") }, leadingIcon = { Text("I", fontWeight = FontWeight.Bold) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { toggleWordUnderline(it, history, notify) } },
                    label = { Text("Gạch chân") }, leadingIcon = { Icon(Icons.Default.FormatUnderlined, null) }, enabled = model.editable)
                WordColorMenu("Màu chữ", false, model.editable) { color -> editor?.let { applyWordColor(it, history, notify, color, false) } }
                WordColorMenu("Tô nền", true, model.editable) { color -> editor?.let { applyWordColor(it, history, notify, color, true) } }
                AssistChip(onClick = { editor?.let { changeWordFontSize(it, history, notify, -2) } },
                    label = { Text("A−") }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { changeWordFontSize(it, history, notify, 2) } },
                    label = { Text("A+") }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { applyWordAlignment(it, history, Layout.Alignment.ALIGN_NORMAL) } },
                    label = { Text("Trái") }, leadingIcon = { Icon(Icons.Default.FormatAlignLeft, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { applyWordAlignment(it, history, Layout.Alignment.ALIGN_CENTER) } },
                    label = { Text("Giữa") }, leadingIcon = { Icon(Icons.Default.FormatAlignCenter, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { applyWordAlignment(it, history, Layout.Alignment.ALIGN_OPPOSITE) } },
                    label = { Text("Phải") }, leadingIcon = { Icon(Icons.Default.FormatAlignRight, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { applyWordList(it, history, false) } },
                    label = { Text("Dấu đầu dòng") }, leadingIcon = { Icon(Icons.Default.FormatListBulleted, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { applyWordList(it, history, true) } },
                    label = { Text("Đánh số") }, leadingIcon = { Icon(Icons.Default.FormatListNumbered, null) }, enabled = model.editable)
                AssistChip(onClick = { editor?.let { clearWordFormatting(it, history, notify) } },
                    label = { Text("Xóa định dạng") }, leadingIcon = { Icon(Icons.Default.FormatClear, null) }, enabled = model.editable)
                AssistChip(
                    onClick = {
                        val text = editor?.text?.toString().orEmpty()
                        save(document.name.substringBeforeLast('.') + ".pdf", DocumentStore.PDF, true) { _, out ->
                            TextPdfExporter.create(text, out, document.name)
                        }
                    },
                    label = { Text("Xuất PDF") },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) }
                )
            }
        }

        Box(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp)).border(1.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    EditText(ctx).apply {
                        gravity = Gravity.TOP or Gravity.START
                        setPadding(28, 24, 28, 24)
                        setBackgroundColor(surfaceColor)
                        setTextColor(textColor)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                        setLineSpacing(0f, 1.18f)
                        isVerticalScrollBarEnabled = true
                        setHorizontallyScrolling(false)
                        setTextIsSelectable(true)
                        history.restoring = true
                        setText(initialText, TextView.BufferType.SPANNABLE)
                        history.restoring = false
                        if (!model.editable) keyListener = null
                        addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                                if (model.editable && s != null) history.capture(s)
                            }
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                            override fun afterTextChanged(s: Editable?) = Unit
                        })
                        editor = this
                    }
                },
                update = { view ->
                    view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * zoom)
                    view.setBackgroundColor(surfaceColor)
                    view.setTextColor(textColor)
                }
            )
        }
        Text(
            "Nhấn giữ trên chữ để bôi chọn; kéo hai tay nắm rồi dùng thanh công cụ. Sao chép vẫn hoạt động ở chế độ chỉ đọc.",
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }

    if (showFind) {
        AlertDialog(
            onDismissRequest = { showFind = false },
            title = { Text("Tìm kiếm và thay thế") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(findText, { findText = it }, label = { Text("Nội dung cần tìm") }, singleLine = true)
                    OutlinedTextField(replaceText, { replaceText = it }, label = { Text("Thay bằng") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val found = editor?.let { findNextInWord(it, findText) } == true
                            if (!found) notify("Không tìm thấy nội dung phù hợp.")
                        }) { Text("Tìm tiếp") }
                        OutlinedButton(
                            onClick = {
                                val current = editor ?: return@OutlinedButton
                                val start = minOf(current.selectionStart, current.selectionEnd)
                                val end = maxOf(current.selectionStart, current.selectionEnd)
                                if (start >= 0 && end > start && current.text.substring(start, end).equals(findText, true)) {
                                    history.capture(current.text)
                                    current.text.replace(start, end, replaceText)
                                    current.setSelection(start + replaceText.length)
                                    findNextInWord(current, findText)
                                } else if (!findNextInWord(current, findText)) notify("Không tìm thấy nội dung phù hợp.")
                            },
                            enabled = model.editable
                        ) { Text("Thay") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = editor ?: return@Button
                        if (findText.isBlank()) return@Button
                        val source = current.text.toString()
                        val regex = Regex(Regex.escape(findText), RegexOption.IGNORE_CASE)
                        val matches = regex.findAll(source).toList()
                        if (matches.isEmpty()) notify("Không tìm thấy nội dung phù hợp.")
                        else {
                            history.capture(current.text, force = true)
                            matches.asReversed().forEach { match ->
                                current.text.replace(match.range.first, match.range.last + 1, replaceText)
                            }
                            current.setSelection(current.text.length)
                            notify("Đã thay ${matches.size} vị trí.")
                        }
                    },
                    enabled = model.editable && findText.isNotBlank()
                ) { Text("Thay tất cả") }
            },
            dismissButton = { TextButton(onClick = { showFind = false }) { Text("Đóng") } }
        )
    }
}

// ---------------- POWERPOINT / TEXT ----------------

@Composable
private fun PowerPointEditor(document: DocumentStore.OpenFile.Slides, save: Save, notify: (String) -> Unit) {
    val changes = remember(document.uri) { mutableStateMapOf<String, String>() }
    var slidesZoom by remember(document.uri) { mutableFloatStateOf(1f) }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PowerPoint — chỉnh sửa chữ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        EditorInfo("Chỉ sửa đoạn chữ có sẵn trong slide. Không xem đúng bố cục hoặc sửa hiệu ứng, hình ảnh, hình khối.")
        DocumentZoomBar(slidesZoom, .75f, 2f, onZoom = { slidesZoom = it })
        Button(onClick = {
            val edits = changes.map { (key, value) ->
                val split = key.lastIndexOf("::")
                OfficeOoxml.SlideChange(key.substring(0, split), key.substring(split + 2).toInt(), value)
            }
            save(DocumentStore.suggestedCopyName(document.name, "pptx"), DocumentStore.PPTX, true) { ctx, out ->
                val data = DocumentStore.readUpTo(ctx, document.uri)
                out.write(OfficeOoxml.writePresentation(data, edits))
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Lưu bản sao PPTX")
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(document.slides) { _, slide ->
                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Slide ${slide.number}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        slide.paragraphs.forEachIndexed { index, item ->
                            if (item.isNotBlank()) {
                                val key = "${slide.partPath}::$index"
                                OutlinedTextField(
                                    value = changes[key] ?: item,
                                    onValueChange = { changes[key] = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Văn bản ${index + 1}") },
                                    textStyle = TextStyle(fontSize = (16f * slidesZoom).sp)
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
private fun TextEditor(document: DocumentStore.OpenFile.Text, save: Save, notify: (String) -> Unit) {
    var text by remember(document.uri) { mutableStateOf(document.value) }
    var textZoom by remember(document.uri) { mutableFloatStateOf(1f) }
    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (document.csv) "Chỉnh sửa CSV/TSV" else "Chỉnh sửa văn bản", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        EditorInfo("Chỉnh sửa UTF-8. Lưu thành bản sao; với CSV, hãy giữ đúng quy tắc dấu phẩy và dấu nháy.")
        DocumentZoomBar(textZoom, .75f, 2f, onZoom = { textZoom = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val body = text
                save(
                    DocumentStore.suggestedCopyName(document.name, if (document.csv) "csv" else "txt"),
                    if (document.csv) DocumentStore.CSV else DocumentStore.TXT,
                    true
                ) { _, out -> out.write(body.toByteArray(Charsets.UTF_8)) }
            }, modifier = Modifier.weight(1f)) { Text("Lưu bản sao") }
            OutlinedButton(onClick = {
                val body = text
                save(document.name.substringBeforeLast('.') + ".pdf", DocumentStore.PDF, true) { _, out ->
                    TextPdfExporter.create(body, out, document.name)
                }
            }, modifier = Modifier.weight(1f)) { Text("Xuất PDF") }
        }
        OutlinedTextField(
            text,
            { text = it },
            modifier = Modifier.fillMaxWidth().weight(1f),
            label = { Text("Nội dung") },
            textStyle = TextStyle(fontSize = (16f * textZoom).sp)
        )
    }
}

// ---------------- PDF ----------------

@Composable
private fun PdfScreen(
    document: DocumentStore.OpenFile.Pdf,
    save: Save,
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember(document.uri) { mutableIntStateOf(0) }
    var total by remember(document.uri) { mutableIntStateOf(0) }
    var zoom by remember(document.uri, page) { mutableFloatStateOf(1f) }
    var pan by remember(document.uri, page) { mutableStateOf(Offset.Zero) }
    var rendered by remember(document.uri, page) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(document.uri) { mutableStateOf(false) }
    var error by remember(document.uri) { mutableStateOf<String?>(null) }
    var selectionPages by remember(document.uri) { mutableStateOf("1") }
    var toolsExpanded by remember(document.uri) { mutableStateOf(false) }
    var selecting by remember(document.uri, page) { mutableStateOf(false) }
    var selectedArea by remember(document.uri, page) { mutableStateOf<PdfTools.Area?>(null) }
    var selectedText by remember(document.uri, page) { mutableStateOf("") }
    var selectionLoading by remember(document.uri, page) { mutableStateOf(false) }
    var showSearch by remember(document.uri) { mutableStateOf(false) }
    var searchQuery by remember(document.uri) { mutableStateOf("") }
    var searchHits by remember(document.uri) { mutableStateOf<List<PdfTools.SearchHit>>(emptyList()) }
    var searching by remember(document.uri) { mutableStateOf(false) }
    var pageMenu by remember { mutableStateOf(false) }

    val renderWidth = when {
        zoom >= 2.3f -> 2500
        zoom >= 1.4f -> 1850
        else -> 1280
    }
    LaunchedEffect(document.uri, page, renderWidth) {
        loading = true
        error = null
        try {
            val result = withContext(Dispatchers.IO) { PdfTools.renderPage(context, document.uri, page, renderWidth) }
            rendered = result.first
            total = result.second
        } catch (e: Exception) {
            rendered = null
            error = e.message ?: "Không thể hiển thị PDF."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Trang ${page + 1} / ${total.coerceAtLeast(1)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Search, "Tìm trong PDF") }
            IconButton(onClick = {
                selecting = !selecting
                if (selecting) {
                    zoom = 1f
                    pan = Offset.Zero
                    selectedArea = null
                    selectedText = ""
                }
            }) {
                Icon(if (selecting) Icons.Default.Close else Icons.Default.SelectAll, "Bôi vùng văn bản")
            }
            Box {
                IconButton(onClick = { pageMenu = true }) { Icon(Icons.Default.MoreVert, "Thêm") }
                DropdownMenu(expanded = pageMenu, onDismissRequest = { pageMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Xuất trang hiện tại thành PNG") },
                        leadingIcon = { Icon(Icons.Default.Image, null) },
                        onClick = {
                            pageMenu = false
                            val current = page
                            save(document.name.substringBeforeLast('.') + "_page_${page + 1}.png", "image/png", false) { ctx, out ->
                                PdfTools.pagePng(ctx, document.uri, current, out)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xoay trang 90°") },
                        leadingIcon = { Icon(Icons.Default.RotateRight, null) },
                        onClick = {
                            pageMenu = false
                            val current = page
                            save(document.name.substringBeforeLast('.') + "_xoay.pdf", DocumentStore.PDF, true) { ctx, out ->
                                PdfTools.rotatePage(ctx, document.uri, current, 90, out)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa trang hiện tại") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, null) },
                        onClick = {
                            pageMenu = false
                            val current = page
                            save(document.name.substringBeforeLast('.') + "_xoa_trang.pdf", DocumentStore.PDF, true) { ctx, out ->
                                PdfTools.deletePage(ctx, document.uri, current, out)
                            }
                        }
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { page = (page - 1).coerceAtLeast(0) }, enabled = page > 0) {
                Icon(Icons.Default.ChevronLeft, "Trang trước")
            }
            DocumentZoomBar(
                zoom,
                1f,
                4f,
                onZoom = {
                    zoom = it
                    if (zoom <= 1f) pan = Offset.Zero
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { page = (page + 1).coerceAtMost(total - 1) }, enabled = page < total - 1) {
                Icon(Icons.Default.ChevronRight, "Trang sau")
            }
            TextButton(onClick = { toolsExpanded = !toolsExpanded }) {
                Icon(Icons.Default.Tune, null, Modifier.size(17.dp)); Text("Trang")
            }
        }

        if (toolsExpanded) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = selectionPages,
                    onValueChange = { selectionPages = it },
                    singleLine = true,
                    label = { Text("Trang: 1-3,5") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val pages = selectionPages
                    try {
                        if (total > 0) PdfTools.parsePageRange(pages, total)
                        save(document.name.substringBeforeLast('.') + "_tach.pdf", DocumentStore.PDF, true) { ctx, out ->
                            PdfTools.extract(ctx, document.uri, pages, out)
                        }
                    } catch (e: Exception) {
                        notify(e.message ?: "Khoảng trang không hợp lệ.")
                    }
                }, enabled = total > 0) { Text("Tách") }
            }
        }

        if (selecting) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "Kéo từ góc đầu đến góc cuối của đoạn chữ cần chọn. Khi chọn vùng, zoom và kéo trang tạm khóa.",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        BoxWithConstraints(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = rendered
            val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
            val viewportWidth = constraints.maxWidth.toFloat()
            val viewportHeight = constraints.maxHeight.toFloat()
            val paddingPx = with(LocalDensity.current) { 10.dp.toPx() }
            val innerWidth = (viewportWidth - paddingPx * 2).coerceAtLeast(1f)
            val innerHeight = (viewportHeight - paddingPx * 2).coerceAtLeast(1f)
            val fitFactor = if (bitmap == null) 1f else min(
                innerWidth / bitmap.width.toFloat(),
                innerHeight / bitmap.height.toFloat()
            )
            val fittedWidth = if (bitmap == null) innerWidth else bitmap.width * fitFactor
            val fittedHeight = if (bitmap == null) innerHeight else bitmap.height * fitFactor

            fun clampOffset(value: Offset, scale: Float): Offset {
                val x = ((fittedWidth * scale - innerWidth) / 2f).coerceAtLeast(0f)
                val y = ((fittedHeight * scale - innerHeight) / 2f).coerceAtLeast(0f)
                return Offset(value.x.coerceIn(-x, x), value.y.coerceIn(-y, y))
            }
            val visibleOffset = clampOffset(pan, zoom)
            fun toNormalized(point: Offset): Offset {
                val imageWidth = fittedWidth * zoom
                val imageHeight = fittedHeight * zoom
                val left = paddingPx + (innerWidth - imageWidth) / 2f + visibleOffset.x
                val top = paddingPx + (innerHeight - imageHeight) / 2f + visibleOffset.y
                return Offset(
                    ((point.x - left) / imageWidth).coerceIn(0f, 1f),
                    ((point.y - top) / imageHeight).coerceIn(0f, 1f)
                )
            }
            fun toScreen(area: PdfTools.Area): Pair<Offset, Size> {
                val a = area.normalized()
                val imageWidth = fittedWidth * zoom
                val imageHeight = fittedHeight * zoom
                val left = paddingPx + (innerWidth - imageWidth) / 2f + visibleOffset.x
                val top = paddingPx + (innerHeight - imageHeight) / 2f + visibleOffset.y
                return Offset(left + a.left * imageWidth, top + a.top * imageHeight) to
                    Size(a.width * imageWidth, a.height * imageHeight)
            }

            if (bitmap != null) {
                Image(
                    imageBitmap!!,
                    contentDescription = "Trang PDF ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(10.dp).graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = visibleOffset.x
                        translationY = visibleOffset.y
                    }
                )
            }

            selectedArea?.let { area ->
                val (topLeft, areaSize) = toScreen(area)
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color(0x66FFD54F),
                        topLeft = topLeft,
                        size = areaSize
                    )
                    drawRect(
                        color = Color(0xFFFFA000),
                        topLeft = topLeft,
                        size = areaSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Box(
                Modifier.fillMaxSize().pointerInput(
                    selecting,
                    document.uri,
                    page,
                    viewportWidth,
                    viewportHeight,
                    bitmap?.width,
                    bitmap?.height
                ) {
                    if (selecting) {
                        var start = Offset.Zero
                        detectDragGestures(
                            onDragStart = { point ->
                                start = toNormalized(point)
                                selectedArea = PdfTools.Area(start.x, start.y, start.x, start.y)
                                selectedText = ""
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val end = toNormalized(change.position)
                                selectedArea = PdfTools.Area(start.x, start.y, end.x, end.y).normalized()
                            },
                            onDragEnd = {
                                val area = selectedArea?.normalized()
                                selecting = false
                                if (area != null && area.width > .002f && area.height > .002f) {
                                    selectionLoading = true
                                    scope.launch {
                                        selectedText = try {
                                            withContext(Dispatchers.IO) {
                                                PdfTools.extractTextInArea(context, document.uri, page, area)
                                            }
                                        } catch (e: Exception) {
                                            notify(e.message ?: "Không trích xuất được chữ trong vùng.")
                                            ""
                                        } finally {
                                            selectionLoading = false
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        detectTransformGestures { _, drag, factor, _ ->
                            val nextZoom = (zoom * factor).coerceIn(1f, 4f)
                            zoom = nextZoom
                            pan = if (nextZoom <= 1f) Offset.Zero else clampOffset(pan + drag, nextZoom)
                        }
                    }
                }
            )

            when {
                error != null -> Text("Lỗi: $error", Modifier.padding(16.dp))
                loading && bitmap == null -> CircularProgressIndicator()
                bitmap == null -> Text("Đang mở PDF...")
            }
            if (loading && bitmap != null) {
                LinearProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
            }
        }

        val area = selectedArea
        if (area != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (selectionLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        if (selectedText.isBlank())
                            "Vùng đã chọn không có lớp chữ. Bạn vẫn có thể tô sáng; PDF scan cần OCR để sao chép chữ."
                        else selectedText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = {
                                if (selectedText.isBlank()) notify("Vùng này không có chữ để sao chép.")
                                else {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("PDF", selectedText))
                                    notify("Đã sao chép nội dung được chọn.")
                                }
                            }
                        ) { Icon(Icons.Default.ContentCopy, null); Text("Sao chép") }
                        TextButton(onClick = {
                            val currentArea = area
                            val currentPage = page
                            save(document.name.substringBeforeLast('.') + "_to_sang.pdf", DocumentStore.PDF, true) { ctx, out ->
                                PdfTools.addMarkup(ctx, document.uri, currentPage, currentArea, PdfTools.Markup.HIGHLIGHT, selectedText, out)
                            }
                        }) { Icon(Icons.Default.BorderColor, null); Text("Tô sáng") }
                        TextButton(onClick = {
                            val currentArea = area
                            val currentPage = page
                            save(document.name.substringBeforeLast('.') + "_gach_chan.pdf", DocumentStore.PDF, true) { ctx, out ->
                                PdfTools.addMarkup(ctx, document.uri, currentPage, currentArea, PdfTools.Markup.UNDERLINE, selectedText, out)
                            }
                        }) { Icon(Icons.Default.FormatUnderlined, null); Text("Gạch chân") }
                        TextButton(onClick = {
                            val currentArea = area
                            val currentPage = page
                            save(document.name.substringBeforeLast('.') + "_gach_ngang.pdf", DocumentStore.PDF, true) { ctx, out ->
                                PdfTools.addMarkup(ctx, document.uri, currentPage, currentArea, PdfTools.Markup.STRIKEOUT, selectedText, out)
                            }
                        }) { Icon(Icons.Default.StrikethroughS, null); Text("Gạch ngang") }
                        TextButton(onClick = { selectedArea = null; selectedText = "" }) {
                            Icon(Icons.Default.Close, null); Text("Bỏ chọn")
                        }
                    }
                }
            }
        }

        Text(
            "Chụm hai ngón để zoom. Nhấn biểu tượng chọn để bôi vùng, sao chép chữ hoặc tạo chú thích PDF.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showSearch) {
        AlertDialog(
            onDismissRequest = { showSearch = false },
            title = { Text("Tìm trong PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        searchQuery,
                        { searchQuery = it },
                        label = { Text("Nội dung cần tìm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!searching && searchHits.isNotEmpty()) {
                        Text("${searchHits.size} kết quả", style = MaterialTheme.typography.labelLarge)
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(searchHits) { hit ->
                                ListItem(
                                    headlineContent = { Text("Trang ${hit.pageIndex + 1}") },
                                    supportingContent = { Text(hit.excerpt, maxLines = 3, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                        page = hit.pageIndex
                                        showSearch = false
                                    }
                                )
                            }
                        }
                    } else if (!searching && searchQuery.isNotBlank()) {
                        Text("Chưa có kết quả. PDF scan cần OCR để tìm chữ.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchQuery.isBlank()) return@Button
                        searching = true
                        scope.launch {
                            searchHits = try {
                                withContext(Dispatchers.IO) { PdfTools.searchText(context, document.uri, searchQuery) }
                            } catch (e: Exception) {
                                notify(e.message ?: "Không tìm kiếm được trong PDF.")
                                emptyList()
                            } finally {
                                searching = false
                            }
                        }
                    },
                    enabled = !searching && searchQuery.isNotBlank()
                ) { Text("Tìm") }
            },
            dismissButton = { TextButton(onClick = { showSearch = false }) { Text("Đóng") } }
        )
    }
}
