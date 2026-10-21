package com.docflowoffline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.truncate

import kotlin.math.roundToInt
import android.graphics.Paint
import android.content.Intent
import android.content.ClipData
import android.net.Uri
import android.provider.DocumentsContract
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.PageRange
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

// ================ ExcelEditor v1.1 ================
/** DocFlow v1.1: offline spreadsheet tools built on a conservative OOXML editor. */
private val ExcelGreen = Color(0xFF167346)
private val ExcelPale = Color(0xFFE7F4ED)
// Spreadsheet overview: zoom down to 20% without changing the XLSX content.
private const val EXCEL_MIN_ZOOM = 0.20f
private const val EXCEL_MAX_ZOOM = 2.50f
private const val EXCEL_ZOOM_STEP = 0.10f
private enum class ExcelTab(val label: String) {
    HOME("Trang chủ"), INSERT("Chèn"), FORMULAS("Công thức"),
    DATA("Dữ liệu"), REVIEW("Đánh giá"), VIEW("Xem")
}
private data class ExcelUndo(
    val values: Map<String, String>,
    val styles: Map<String, OfficeOoxml.StyleChange>,
    val merges: Map<String, Boolean>
)
private enum class ExcelColorTarget { FONT, FILL }
private data class ExcelRibbonItem(val title: String, val icon: ImageVector, val id: String, val active: Boolean = false)

private fun colFromAddress(address: String): Int {
    var n = 0
    for (letter in address.takeWhile { it in 'A'..'Z' }) n = n * 26 + (letter - 'A' + 1)
    return n
}
private fun formatExcelNumber(number: Double): String {
    if (!number.isFinite()) return "#NUM!"
    return if (number == number.toLong().toDouble()) number.toLong().toString()
    else "%.8f".format(Locale.US, number).trimEnd('0').trimEnd('.')
}

/**
 * Limited offline formula preview. Unsupported syntax is kept for Excel to calculate later.
 * Sheet-qualified references are resolved through the optional workbook lookup.
 */
internal fun previewFormula(
    expression: String,
    lookup: (String) -> String,
    crossSheet: ((String, String) -> String?)? = null
): String? = AccountingFormulas.preview(expression, lookup, crossSheet)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExcelEditor(
    document: DocumentStore.OpenFile.Excel,
    save: Save,
    notify: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val formulaFocusRequester = remember { FocusRequester() }
    var activeUri by remember(document.uri) { mutableStateOf(document.uri) }
    var displayName by remember(document.uri) { mutableStateOf(document.name) }
    var sheetIndex by remember(document.uri) { mutableIntStateOf(0) }
    val sheet = document.data.sheets.getOrNull(sheetIndex) ?: return
    var activeTab by remember(document.uri) { mutableStateOf(ExcelTab.HOME) }
    var ribbonVisible by remember(document.uri) { mutableStateOf(true) }
    var zoom by remember(document.uri) { mutableFloatStateOf(1f) }
    var previousZoom by remember(document.uri) { mutableFloatStateOf(1f) }
    var zoomPickerVisible by remember(document.uri) { mutableStateOf(false) }
    var selected by remember(document.uri, sheet.partPath) { mutableStateOf("A1") }
    var addressInput by remember(document.uri, sheet.partPath) { mutableStateOf("A1") }
    val values = remember(document.uri) { mutableStateMapOf<String, String>() }
    val styleChanges = remember(document.uri) { mutableStateMapOf<String, OfficeOoxml.StyleChange>() }
    val mergeChanges = remember(document.uri) { mutableStateMapOf<String, Boolean>() }
    val undo = remember(document.uri) { mutableStateListOf<ExcelUndo>() }
    val redo = remember(document.uri) { mutableStateListOf<ExcelUndo>() }
    var clipboard by remember(document.uri) { mutableStateOf<String?>(null) }
    var searchVisible by remember(document.uri) { mutableStateOf(false) }
    var functionPickerVisible by remember(document.uri) { mutableStateOf(false) }
    var query by remember(document.uri) { mutableStateOf("") }
    var results by remember(document.uri, sheet.partPath) { mutableStateOf(emptyList<String>()) }
    var resultIndex by remember(document.uri, sheet.partPath) { mutableIntStateOf(0) }
    var filterVisible by remember(document.uri) { mutableStateOf(false) }
    var filterText by remember(document.uri, sheet.partPath) { mutableStateOf("") }
    var filterColumn by remember(document.uri, sheet.partPath) { mutableIntStateOf(1) }
    var sortOrder by remember(document.uri, sheet.partPath) { mutableIntStateOf(0) }
    var showGridLines by remember(document.uri) { mutableStateOf(true) }
    var showFormulaBar by remember(document.uri) { mutableStateOf(true) }
    var infoVisible by remember(document.uri) { mutableStateOf(false) }
    var fileMenuVisible by remember(document.uri) { mutableStateOf(false) }
    var fileInfoVisible by remember(document.uri) { mutableStateOf(false) }
    var findReplaceVisible by remember(document.uri) { mutableStateOf(false) }
    var findNeedle by remember(document.uri) { mutableStateOf("") }
    var replacement by remember(document.uri) { mutableStateOf("") }
    var colorTarget by remember(document.uri) { mutableStateOf<ExcelColorTarget?>(null) }
    var mergeDialogVisible by remember(document.uri) { mutableStateOf(false) }
    var mergeStart by remember(document.uri) { mutableStateOf("A1") }
    var mergeEnd by remember(document.uri) { mutableStateOf("B1") }
    var renameVisible by remember(document.uri) { mutableStateOf(false) }
    var renameInput by remember(document.uri) { mutableStateOf(document.name) }
    var conversionVisible by remember(document.uri) { mutableStateOf(false) }
    var backupVisible by remember(document.uri) { mutableStateOf(false) }
    var securityVisible by remember(document.uri) { mutableStateOf(false) }
    var encryptPasswordVisible by remember(document.uri) { mutableStateOf(false) }
    var encryptPassword by remember(document.uri) { mutableStateOf("") }
    var encryptConfirm by remember(document.uri) { mutableStateOf("") }
    var pendingEncryptedUri by remember(document.uri) { mutableStateOf<Uri?>(null) }
    var decryptPasswordVisible by remember(document.uri) { mutableStateOf(false) }
    var decryptPassword by remember(document.uri) { mutableStateOf("") }
    var servicesVisible by remember(document.uri) { mutableStateOf(false) }
    val xScroll = rememberScrollState()
    val yScroll = rememberLazyListState()
    val maxRows = maxOf(100, sheet.maxRow + 25).coerceAtMost(100_000)
    val maxCols = maxOf(12, sheet.maxCol + 5).coerceAtMost(256)
    val selectedKey = "${sheet.partPath}::$selected"
    val decryptPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            pendingEncryptedUri = uri
            decryptPassword = ""
            decryptPasswordVisible = true
        }
    }

    fun rawValueFor(target: OfficeOoxml.Sheet, ref: String): String =
        values["${target.partPath}::$ref"] ?: target.cells[ref].orEmpty()
    fun styleFor(target: OfficeOoxml.Sheet, ref: String): OfficeOoxml.CellAppearance {
        val base = target.appearances[ref] ?: OfficeOoxml.CellAppearance()
        val change = styleChanges["${target.partPath}::$ref"] ?: return base
        return base.copy(
            bold = change.bold ?: base.bold,
            italic = change.italic ?: base.italic,
            underline = change.underline ?: base.underline,
            alignment = change.alignment ?: base.alignment,
            fillRgb = when (change.fillRgb) {
                null -> base.fillRgb
                "" -> null
                else -> change.fillRgb
            },
            fontRgb = when (change.fontRgb) {
                null -> base.fontRgb
                "" -> null
                else -> change.fontRgb
            },
            percent = change.numFmtId?.let { it == 9 || it == 10 } ?: base.percent
        )
    }
    fun displayValueFor(target: OfficeOoxml.Sheet, ref: String): String {
        val raw = rawValueFor(target, ref)
        val shown = if (raw.startsWith("=")) {
            previewFormula(raw, { candidate -> rawValueFor(target, candidate) },
                { name, candidate -> document.data.sheets.firstOrNull { it.name.equals(name, true) }
                    ?.let { rawValueFor(it, candidate) } })
                ?: if (values.containsKey("${target.partPath}::$ref")) raw else target.displayCells[ref] ?: raw
        } else raw
        if (styleFor(target, ref).percent) {
            return shown.toDoubleOrNull()?.let { formatExcelNumber(it * 100.0) + "%" } ?: shown
        }
        return shown
    }
    fun rawValue(ref: String): String = rawValueFor(sheet, ref)
    fun activeStyle(ref: String): OfficeOoxml.CellAppearance = styleFor(sheet, ref)
    fun cellDisplay(ref: String): String = displayValueFor(sheet, ref)
    var formula by remember(document.uri, selectedKey) { mutableStateOf(rawValue(selected)) }
    LaunchedEffect(selectedKey, values[selectedKey]) {
        formula = rawValue(selected)
        addressInput = selected
    }

    fun checkpoint() {
        undo.add(ExcelUndo(values.toMap(), styleChanges.toMap(), mergeChanges.toMap()))
        if (undo.size > 30) undo.removeAt(0)
        redo.clear()
    }
    fun setValue(text: String) {
        if (rawValue(selected) == text) return
        checkpoint()
        if (text == sheet.cells[selected].orEmpty()) values.remove(selectedKey) else values[selectedKey] = text
        formula = text
    }
    fun setAppearance(change: (OfficeOoxml.StyleChange, OfficeOoxml.CellAppearance) -> OfficeOoxml.StyleChange) {
        val original = styleChanges[selectedKey] ?: OfficeOoxml.StyleChange(sheet.partPath, selected)
        val next = change(original, activeStyle(selected))
        if (next == original) return
        checkpoint()
        styleChanges[selectedKey] = next
    }
    fun effectiveMerges(target: OfficeOoxml.Sheet = sheet): List<OfficeOoxml.MergeRange> {
        val result = LinkedHashMap<String, OfficeOoxml.MergeRange>()
        target.merges.forEach { result[it.ref] = it }
        val prefix = "${target.partPath}::"
        mergeChanges.forEach { (key, merged) ->
            if (key.startsWith(prefix)) {
                OfficeOoxml.parseMergeRange(key.removePrefix(prefix))?.let { range ->
                    if (merged) result[range.ref] = range else result.remove(range.ref)
                }
            }
        }
        return result.values.sortedWith(compareBy<OfficeOoxml.MergeRange> { it.startRow }.thenBy { it.startCol })
    }
    fun mergeChangeList(snapshot: Map<String, Boolean>): List<OfficeOoxml.MergeChange> =
        snapshot.mapNotNull { (key, merged) ->
            val split = key.lastIndexOf("::")
            if (split <= 0) null else OfficeOoxml.parseMergeRange(key.substring(split + 2))?.let {
                OfficeOoxml.MergeChange(key.substring(0, split), it, merged)
            }
        }
    fun workbookBytes(
        valueSnapshot: Map<String, String> = values.toMap(),
        styleSnapshot: Map<String, OfficeOoxml.StyleChange> = styleChanges.toMap(),
        mergeSnapshot: Map<String, Boolean> = mergeChanges.toMap()
    ): ByteArray {
        val changes = valueSnapshot.map { (key, value) ->
            val separator = key.lastIndexOf("::")
            val path = key.substring(0, separator)
            val cell = key.substring(separator + 2)
            val source = document.data.sheets.firstOrNull { it.partPath == path }
            val preview = if (value.startsWith("=")) previewFormula(value,
                { ref -> valueSnapshot["$path::$ref"] ?: source?.cells?.get(ref).orEmpty() },
                { name, ref -> document.data.sheets.firstOrNull { it.name.equals(name, true) }?.let { target ->
                    valueSnapshot["${target.partPath}::$ref"] ?: target.cells[ref].orEmpty()
                } })?.toDoubleOrNull()?.toString() else null
            OfficeOoxml.CellChange(path, cell, value, preview)
        }
        return OfficeOoxml.writeWorkbook(
            document.bytes, changes, styleSnapshot.values.toList(), mergeChangeList(mergeSnapshot)
        )
    }
    fun sheetSnapshot(target: OfficeOoxml.Sheet = sheet): SpreadsheetExporter.SheetSnapshot {
        val prefix = "${target.partPath}::"
        val editedRefs = values.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
        val styledRefs = styleChanges.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
        val refs = (target.cells.keys + editedRefs + styledRefs).distinct()
        val usedRow = refs.maxOfOrNull { OfficeOoxml.addressParts(it)?.first ?: 1 } ?: 1
        val usedCol = refs.maxOfOrNull { OfficeOoxml.addressParts(it)?.second ?: 1 } ?: 1
        val rows = maxOf(1, target.maxRow, usedRow).coerceAtMost(2000)
        val columns = maxOf(1, target.maxCol, usedCol).coerceAtMost(100)
        val text = LinkedHashMap<String, String>()
        refs.forEach { ref ->
            val parts = OfficeOoxml.addressParts(ref)
            if (parts != null && parts.first <= rows && parts.second <= columns) {
                val value = displayValueFor(target, ref)
                if (value.isNotEmpty()) text[ref] = value
            }
        }
        val appearanceRefs = (target.appearances.keys + styledRefs).distinct()
        val appearances = LinkedHashMap<String, OfficeOoxml.CellAppearance>()
        appearanceRefs.forEach { ref ->
            val parts = OfficeOoxml.addressParts(ref)
            if (parts != null && parts.first <= rows && parts.second <= columns) {
                val style = styleFor(target, ref)
                if (style != OfficeOoxml.CellAppearance()) appearances[ref] = style
            }
        }
        return SpreadsheetExporter.SheetSnapshot(
            documentName = displayName, sheetName = target.name, rows = rows, columns = columns,
            values = text, appearances = appearances, merges = effectiveMerges(target)
        )
    }
    fun applyMerge(first: String, last: String) {
        val range = OfficeOoxml.normalizedMergeRange(first, last)
        if (range == null) { notify("Vùng gộp không hợp lệ. Ví dụ A1:C1."); return }
        if (range.startRow != range.endRow) {
            notify("Bản hiện tại hỗ trợ gộp ngang trong cùng một hàng để hiển thị chính xác trên điện thoại.")
            return
        }
        if (range.startCol == range.endCol) { notify("Hãy chọn ít nhất hai ô để gộp."); return }
        if (range.endCol - range.startCol + 1 > 24) { notify("Mỗi lần chỉ gộp tối đa 24 ô ngang."); return }
        val overlap = effectiveMerges().firstOrNull { it.ref != range.ref && it.overlaps(range) }
        if (overlap != null) { notify("Vùng ${range.ref} đang chồng lên ô gộp ${overlap.ref}."); return }
        val occupied = (range.startCol + 1..range.endCol).map { OfficeOoxml.cellAddress(range.startRow, it) }
            .firstOrNull { rawValue(it).isNotBlank() }
        if (occupied != null) {
            notify("Ô $occupied đang có dữ liệu. Hãy xóa nội dung các ô phụ trước khi gộp để tránh mất dữ liệu.")
            return
        }
        checkpoint()
        mergeChanges["${sheet.partPath}::${range.ref}"] = true
        selected = OfficeOoxml.cellAddress(range.startRow, range.startCol)
        notify("Đã gộp ${range.ref}. Chọn Lưu dưới dạng để ghi vào XLSX.")
    }
    fun unmergeSelected() {
        val range = effectiveMerges().firstOrNull { it.contains(selected) }
        if (range == null) { notify("Ô $selected không nằm trong vùng gộp."); return }
        checkpoint()
        mergeChanges["${sheet.partPath}::${range.ref}"] = false
        notify("Đã bỏ gộp ${range.ref}.")
    }
    fun exportSheetPdf() {
        val snapshot = sheetSnapshot()
        save("${displayName.substringBeforeLast('.')}_${sheet.name.take(24)}.pdf", DocumentStore.PDF, true) { _, output ->
            SpreadsheetExporter.writePdf(snapshot, output)
        }
    }
    fun exportSheetPng() {
        val snapshot = sheetSnapshot()
        if (snapshot.rows > 190 || snapshot.columns > 58) {
            notify("Ảnh PNG sẽ giới hạn khoảng 190 hàng và 58 cột đầu để tránh vượt bộ nhớ.")
        }
        save("${displayName.substringBeforeLast('.')}_${sheet.name.take(24)}.png", "image/png", false) { _, output ->
            SpreadsheetExporter.writePng(snapshot, output)
        }
    }
    fun exportSheetDelimited(separator: Char, extension: String, mime: String) {
        val snapshot = sheetSnapshot()
        save("${displayName.substringBeforeLast('.')}_${sheet.name.take(24)}.$extension", mime, false) { _, output ->
            output.write(SpreadsheetExporter.csv(snapshot, separator).toByteArray(Charsets.UTF_8))
        }
    }
    fun mergeWorksheets() {
        val snapshots = document.data.sheets.map { sheetSnapshot(it) }
        save("${displayName.substringBeforeLast('.')}_hop_nhat.xlsx", DocumentStore.XLSX, true) { _, output ->
            SpreadsheetExporter.writeCombinedWorkbook(snapshots, output)
        }
    }
    fun shareCurrentWorkbook(openOnly: Boolean = false) {
        val valueSnapshot = values.toMap(); val styleSnapshot = styleChanges.toMap(); val mergeSnapshot = mergeChanges.toMap()
        scope.launch {
            try {
                val shareUri = withContext(Dispatchers.IO) {
                    val bytes = workbookBytes(valueSnapshot, styleSnapshot, mergeSnapshot)
                    val folder = File(context.cacheDir, "shared").apply { mkdirs() }
                    val safe = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(90).ifBlank { "DocFlow.xlsx" }
                    val file = File(folder, if (safe.endsWith(".xlsx", true)) safe else "$safe.xlsx")
                    file.writeBytes(bytes)
                    FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                }
                val intent = Intent(if (openOnly) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply {
                    type = DocumentStore.XLSX
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    clipData = ClipData.newUri(context.contentResolver, displayName, shareUri)
                    if (openOnly) setDataAndType(shareUri, DocumentStore.XLSX)
                    else putExtra(Intent.EXTRA_STREAM, shareUri)
                }
                context.startActivity(Intent.createChooser(intent, if (openOnly) "Mở bằng ứng dụng khác" else "Chia sẻ bảng tính"))
            } catch (e: Exception) {
                notify("Không thể ${if (openOnly) "mở" else "chia sẻ"}: ${e.message ?: "lỗi hệ thống"}")
            }
        }
    }
    fun printCurrentSheet() {
        val snapshot = sheetSnapshot()
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ByteArrayOutputStream().also { SpreadsheetExporter.writePdf(snapshot, it) }.toByteArray()
                }
                printPdfBytes(context, "${displayName.substringBeforeLast('.')}_${sheet.name}.pdf", bytes)
            } catch (e: Exception) {
                notify("Không thể tạo bản in: ${e.message ?: "lỗi hệ thống"}")
            }
        }
    }
    fun renameCurrentDocument() {
        val base = renameInput.trim().replace(Regex("[\\/:*?\"<>|]"), "_")
        if (base.isBlank()) { notify("Tên tệp không được để trống."); return }
        val finalName = if (base.endsWith(".xlsx", true)) base else "$base.xlsx"
        scope.launch {
            try {
                val renamed = withContext(Dispatchers.IO) {
                    DocumentsContract.renameDocument(context.contentResolver, activeUri, finalName)
                }
                if (renamed != null) activeUri = renamed
                displayName = finalName
                renameVisible = false
                DocumentStore.remember(context, activeUri, displayName)
                notify("Đã đổi tên thành $displayName")
            } catch (e: Exception) {
                notify("Không đổi tên được. Nhà cung cấp tệp có thể không cho phép: ${e.message ?: "lỗi hệ thống"}")
            }
        }
    }
    fun addToRecent() {
        DocumentStore.remember(context, activeUri, displayName)
        notify("Đã đưa tài liệu lên đầu danh sách gần đây.")
    }
    fun createLocalBackup() {
        val valueSnapshot = values.toMap(); val styleSnapshot = styleChanges.toMap(); val mergeSnapshot = mergeChanges.toMap()
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val folder = File(context.filesDir, "docflow_backups").apply { mkdirs() }
                    val stamp = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                    val safe = displayName.substringBeforeLast('.').replace(Regex("[^A-Za-z0-9._-]"), "_").take(60)
                    File(folder, "${safe}_${stamp}.xlsx").also {
                        it.writeBytes(workbookBytes(valueSnapshot, styleSnapshot, mergeSnapshot))
                    }
                }
                notify("Đã sao lưu cục bộ: ${file.name}")
                backupVisible = false
            } catch (e: Exception) { notify("Sao lưu thất bại: ${e.message ?: "lỗi hệ thống"}") }
        }
    }
    fun restoreLatestBackup() {
        val file = File(context.filesDir, "docflow_backups").listFiles()
            ?.filter { it.extension.equals("xlsx", true) }?.maxByOrNull { it.lastModified() }
        if (file == null) { notify("Chưa có bản sao cục bộ nào."); return }
        val bytesResult = runCatching { file.readBytes() }
        if (bytesResult.isFailure) {
            notify("Không đọc được bản sao: ${bytesResult.exceptionOrNull()?.message ?: "lỗi hệ thống"}")
            return
        }
        val bytes = bytesResult.getOrThrow()
        save("${displayName.substringBeforeLast('.')}_khoi_phuc.xlsx", DocumentStore.XLSX, true) { _, output -> output.write(bytes) }
        backupVisible = false
    }

    fun undoAction() {
        if (undo.isEmpty()) return
        redo.add(ExcelUndo(values.toMap(), styleChanges.toMap(), mergeChanges.toMap()))
        val state = undo.removeAt(undo.lastIndex)
        values.clear(); values.putAll(state.values)
        styleChanges.clear(); styleChanges.putAll(state.styles)
        mergeChanges.clear(); mergeChanges.putAll(state.merges)
        formula = rawValue(selected)
    }
    fun redoAction() {
        if (redo.isEmpty()) return
        undo.add(ExcelUndo(values.toMap(), styleChanges.toMap(), mergeChanges.toMap()))
        val state = redo.removeAt(redo.lastIndex)
        values.clear(); values.putAll(state.values)
        styleChanges.clear(); styleChanges.putAll(state.styles)
        mergeChanges.clear(); mergeChanges.putAll(state.merges)
        formula = rawValue(selected)
    }
    fun navigate(input: String) {
        val normalized = input.trim().uppercase(Locale.ROOT).replace("$", "")
        if (!normalized.matches(Regex("[A-Z]{1,3}[1-9][0-9]{0,5}"))) {
            notify("Địa chỉ ô không hợp lệ. Ví dụ: A12 hoặc AZ500.")
            return
        }
        val row = normalized.dropWhile(Char::isLetter).toIntOrNull() ?: return
        val col = colFromAddress(normalized)
        if (row !in 1..maxRows || col !in 1..maxCols) {
            notify("Ô nằm ngoài vùng xem A1:${OfficeOoxml.cellAddress(maxRows,maxCols)}")
            return
        }
        if (filterText.isNotBlank() || sortOrder != 0) { filterText = ""; sortOrder = 0 }
        selected = normalized
        scope.launch {
            yScroll.scrollToItem(row - 1)
            val cellPixels = with(density) { (GRID_COLUMN_DP.dp * zoom).roundToPx() }
            xScroll.scrollTo(((col - 1) * cellPixels).coerceAtMost(xScroll.maxValue))
        }
    }
    fun applyRibbonAction(id: String) {
        when (id) {
            "undo" -> undoAction()
            "redo" -> redoAction()
            "copy" -> { clipboard = rawValue(selected); notify("Đã sao chép nội dung $selected") }
            "cut" -> { clipboard = rawValue(selected); setValue("") }
            "paste" -> { val clip = clipboard; if (clip != null) setValue(clip) else notify("Chưa có nội dung sao chép") }
            "clear" -> setValue("")
            "bold" -> setAppearance { old, appearance -> old.copy(bold = !appearance.bold) }
            "italic" -> setAppearance { old, appearance -> old.copy(italic = !appearance.italic) }
            "underline" -> setAppearance { old, appearance -> old.copy(underline = !appearance.underline) }
            "left", "center", "right" -> setAppearance { old, _ -> old.copy(alignment = id) }
            "highlight" -> setAppearance { old, appearance -> old.copy(fillRgb = if (appearance.fillRgb == "FFF2CC") "" else "FFF2CC") }
            "percent" -> setAppearance { old, appearance -> old.copy(numFmtId = if (appearance.percent) 0 else 10) }
            "insertDate" -> setValue(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            "insertTime" -> setValue(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
            "insertCheck" -> setValue("✓")
            "numberList" -> {
                val row = selected.dropWhile(Char::isLetter).toIntOrNull() ?: 1
                setValue(row.toString())
            }
            "functionCatalog" -> { activeTab = ExcelTab.FORMULAS; functionPickerVisible = true }
            "quickSUMIF", "quickSUMIFS", "quickIF", "quickROUND", "quickVLOOKUP" -> {
                accountingFunctionCatalog.firstOrNull { it.name == id.removePrefix("quick") }?.let {
                    showFormulaBar = true
                    formula = it.example
                    notify("Đã đưa ${it.name} vào thanh fx. Chỉnh phạm vi rồi nhấn ✓ để lưu vào ô $selected.")
                }
            }
            "sum", "average", "count", "min", "max" -> {
                val row = selected.dropWhile(Char::isLetter).toIntOrNull() ?: 1
                if (row < 2) notify("Hãy chọn ô ở hàng 2 trở xuống để tạo công thức từ các hàng phía trên.")
                else {
                    val col = selected.takeWhile(Char::isLetter)
                    setValue("=${id.uppercase(Locale.ROOT)}(${col}1:${col}${row - 1})")
                }
            }
            "find" -> { searchVisible = !searchVisible; if (!searchVisible) results = emptyList() }
            "replace" -> { findReplaceVisible = true }
            "filter" -> { filterVisible = !filterVisible; if (!filterVisible) filterText = ""; filterColumn = colFromAddress(selected) }
            "sortAsc" -> { sortOrder = if (sortOrder == 1) 0 else 1; filterColumn = colFromAddress(selected) }
            "sortDesc" -> { sortOrder = if (sortOrder == -1) 0 else -1; filterColumn = colFromAddress(selected) }
            "resetDataView" -> { sortOrder = 0; filterText = ""; filterVisible = false }
            "formulaBar" -> showFormulaBar = !showFormulaBar
            "gridlines" -> showGridLines = !showGridLines
            "ribbon" -> ribbonVisible = !ribbonVisible
            "zoomIn" -> zoom = (zoom + EXCEL_ZOOM_STEP).coerceAtMost(EXCEL_MAX_ZOOM)
            "zoomOut" -> zoom = (zoom - EXCEL_ZOOM_STEP).coerceAtLeast(EXCEL_MIN_ZOOM)
            "zoom25" -> zoom = 0.25f
            "zoom50" -> zoom = 0.50f
            "zoom75" -> zoom = 0.75f
            "zoom100" -> zoom = 1f
            "zoomPicker" -> zoomPickerVisible = true
            "info" -> infoVisible = !infoVisible
        }
    }
    /** One undo checkpoint for multi-cell replace; formulas are excluded to protect references. */
    fun findTextCells(): List<String> {
        if (findNeedle.isBlank()) return emptyList()
        val all = (sheet.cells.keys + values.keys.filter { it.startsWith(sheet.partPath + "::") }
            .map { it.substringAfter("::") }).distinct()
        return all.filter { ref ->
            val body = rawValue(ref)
            !body.startsWith("=") && body.contains(findNeedle, ignoreCase = true)
        }.sortedWith(compareBy<String> { it.dropWhile(Char::isLetter).toIntOrNull() ?: Int.MAX_VALUE }
            .thenBy { colFromAddress(it) })
    }
    fun replaceAllTextCells() {
        val refs = findTextCells()
        if (refs.isEmpty()) { notify("Không tìm thấy ô văn bản phù hợp."); return }
        checkpoint()
        refs.forEach { ref ->
            val changed = rawValue(ref).replace(findNeedle, replacement, ignoreCase = true)
            val key = "${sheet.partPath}::$ref"
            if (changed == sheet.cells[ref].orEmpty()) values.remove(key)
            else values[key] = changed
        }
        formula = rawValue(selected)
        notify("Đã thay nội dung trong ${refs.size} ô. Chọn Lưu bản sao để xuất XLSX.")
    }
    fun saveWorkbookCopy() {
        val valueSnapshot = values.toMap()
        val styleSnapshot = styleChanges.toMap()
        val mergeSnapshot = mergeChanges.toMap()
        save(DocumentStore.suggestedCopyName(displayName, "xlsx"), DocumentStore.XLSX, true) { _, output ->
            output.write(workbookBytes(valueSnapshot, styleSnapshot, mergeSnapshot))
        }
    }
    LaunchedEffect(sheet.partPath) { xScroll.scrollTo(0); yScroll.scrollToItem(0) }
    LaunchedEffect(zoom) {
        if (previousZoom != zoom && previousZoom > 0) {
            xScroll.scrollTo((xScroll.value * zoom / previousZoom).roundToInt().coerceAtLeast(0).coerceAtMost(xScroll.maxValue))
            previousZoom = zoom
        }
    }
    val rowMap: List<Int>? = remember(sheet.partPath, filterText, filterColumn, sortOrder, values.toMap()) {
        if (filterText.isBlank() && sortOrder == 0) null
        else {
            // Data operations are deliberately view-only, preserving formulas/styles in the actual workbook.
            val matches = (2..maxOf(2, sheet.maxRow)).filter { row ->
                filterText.isBlank() || rawValue(OfficeOoxml.cellAddress(row, filterColumn))
                    .contains(filterText.trim(), ignoreCase = true)
            }
            val sorted = if (sortOrder == 0) matches else matches.sortedWith(Comparator<Int> { a, b ->
                val av = rawValue(OfficeOoxml.cellAddress(a, filterColumn))
                val bv = rawValue(OfficeOoxml.cellAddress(b, filterColumn))
                val comparison = when {
                    av.toDoubleOrNull() != null && bv.toDoubleOrNull() != null ->
                        av.toDouble().compareTo(bv.toDouble())
                    else -> av.compareTo(bv, ignoreCase = true)
                }
                comparison * sortOrder
            })
            listOf(1) + sorted
        }
    }
    LaunchedEffect(rowMap) { if (rowMap != null) yScroll.scrollToItem(0) }
    val currentMerges = remember(sheet.partPath, mergeChanges.toMap()) { effectiveMerges(sheet) }
    val selectedMerge = currentMerges.firstOrNull { it.contains(selected) }
    val hasChanges = values.isNotEmpty() || styleChanges.isNotEmpty() || mergeChanges.isNotEmpty()
    val selectedAppearance = activeStyle(selected)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).imePadding()) {
        Row(Modifier.fillMaxWidth().height(51.dp).background(ExcelGreen).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, "Quay lại", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(displayName, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${document.data.sheets.size} trang tính · Offline", fontSize = 10.sp,
                    color = Color.White.copy(alpha = .85f))
            }
            IconButton(onClick = { applyRibbonAction("undo") }, enabled = undo.isNotEmpty(), modifier = Modifier.size(37.dp)) {
                Icon(Icons.Default.Undo, "Hoàn tác", tint = Color.White.copy(alpha = if (undo.isNotEmpty()) 1f else .4f))
            }
            IconButton(onClick = { applyRibbonAction("redo") }, enabled = redo.isNotEmpty(), modifier = Modifier.size(37.dp)) {
                Icon(Icons.Default.Redo, "Làm lại", tint = Color.White.copy(alpha = if (redo.isNotEmpty()) 1f else .4f))
            }
            IconButton(onClick = { findReplaceVisible = true }, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Default.Search, "Tìm và thay thế", tint = Color.White)
            }
            IconButton(onClick = { saveWorkbookCopy() }, modifier = Modifier.size(39.dp)) {
                Icon(Icons.Default.Save, "Lưu dưới dạng XLSX", tint = Color.White)
            }
            IconButton(onClick = { fileMenuVisible = true }, modifier = Modifier.size(35.dp)) {
                Icon(Icons.Default.MoreVert, "Thêm tùy chọn tài liệu", tint = Color.White)
            }
        }
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            edgePadding = 4.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = ExcelGreen,
            divider = { HorizontalDivider(thickness = .5.dp) }
        ) {
            ExcelTab.entries.forEach { tab ->
                Tab(selected = activeTab == tab, onClick = { activeTab = tab; ribbonVisible = true },
                    modifier = Modifier.height(42.dp),
                    text = { Text(tab.label, fontSize = 12.sp, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal) })
            }
        }
        if (ribbonVisible) {
            val actions = remember(activeTab, selectedAppearance, showGridLines, showFormulaBar,
                searchVisible, filterVisible, sortOrder) {
                when (activeTab) {
                    ExcelTab.HOME -> listOf(
                        ExcelRibbonItem("Dán", Icons.Default.ContentPaste, "paste"),
                        ExcelRibbonItem("Sao chép", Icons.Default.ContentCopy, "copy"),
                        ExcelRibbonItem("Cắt", Icons.Default.ContentCut, "cut"),
                        ExcelRibbonItem("Xóa", Icons.Default.Backspace, "clear"),
                        ExcelRibbonItem("Đậm", Icons.Default.FormatBold, "bold", selectedAppearance.bold),
                        ExcelRibbonItem("Nghiêng", Icons.Default.FormatItalic, "italic", selectedAppearance.italic),
                        ExcelRibbonItem("Gạch chân", Icons.Default.FormatUnderlined, "underline", selectedAppearance.underline),
                        ExcelRibbonItem("Trái", Icons.Default.FormatAlignLeft, "left", selectedAppearance.alignment == "left"),
                        ExcelRibbonItem("Giữa", Icons.Default.FormatAlignCenter, "center", selectedAppearance.alignment == "center"),
                        ExcelRibbonItem("Phải", Icons.Default.FormatAlignRight, "right", selectedAppearance.alignment == "right"),
                        ExcelRibbonItem("Tô vàng", Icons.Default.FormatColorFill, "highlight", selectedAppearance.fillRgb == "FFF2CC"),
                        ExcelRibbonItem("Phần trăm", Icons.Default.Percent, "percent", selectedAppearance.percent)
                    )
                    ExcelTab.INSERT -> listOf(
                        ExcelRibbonItem("Ngày", Icons.Default.Event, "insertDate"),
                        ExcelRibbonItem("Giờ", Icons.Default.Schedule, "insertTime"),
                        ExcelRibbonItem("Dấu tích", Icons.Default.CheckCircle, "insertCheck"),
                        ExcelRibbonItem("Số hàng", Icons.Default.Tag, "numberList"),
                        ExcelRibbonItem("Dán", Icons.Default.ContentPaste, "paste")
                    )
                    ExcelTab.FORMULAS -> listOf(
                        ExcelRibbonItem("Các hàm", Icons.Default.MenuBook, "functionCatalog"),
                        ExcelRibbonItem("AutoSum", Icons.Default.Functions, "sum"),
                        ExcelRibbonItem("AVERAGE", Icons.Default.Calculate, "average"),
                        ExcelRibbonItem("COUNT", Icons.Default.Numbers, "count"),
                        ExcelRibbonItem("MIN", Icons.Default.ArrowDownward, "min"),
                        ExcelRibbonItem("MAX", Icons.Default.ArrowUpward, "max"),
                        ExcelRibbonItem("SUMIF", Icons.Default.Calculate, "quickSUMIF"),
                        ExcelRibbonItem("SUMIFS", Icons.Default.Calculate, "quickSUMIFS"),
                        ExcelRibbonItem("IF", Icons.Default.AccountTree, "quickIF"),
                        ExcelRibbonItem("ROUND", Icons.Default.Numbers, "quickROUND"),
                        ExcelRibbonItem("VLOOKUP", Icons.Default.Search, "quickVLOOKUP")
                    )
                    ExcelTab.DATA -> listOf(
                        ExcelRibbonItem("Lọc", Icons.Default.FilterAlt, "filter", filterVisible),
                        ExcelRibbonItem("A→Z", Icons.Default.SortByAlpha, "sortAsc", sortOrder == 1),
                        ExcelRibbonItem("Z→A", Icons.Default.SwapVert, "sortDesc", sortOrder == -1),
                        ExcelRibbonItem("Bỏ lọc", Icons.Default.FilterAltOff, "resetDataView")
                    )
                    ExcelTab.REVIEW -> listOf(
                        ExcelRibbonItem("Tìm kiếm", Icons.Default.Search, "find", searchVisible),
                        ExcelRibbonItem("Thay thế", Icons.Default.FindReplace, "replace"),
                        ExcelRibbonItem("Hoàn tác", Icons.Default.Undo, "undo"),
                        ExcelRibbonItem("Làm lại", Icons.Default.Redo, "redo"),
                        ExcelRibbonItem("Chi tiết", Icons.Default.Info, "info")
                    )
                    ExcelTab.VIEW -> listOf(
                        ExcelRibbonItem("Thu nhỏ", Icons.Default.ZoomOut, "zoomOut"),
                        ExcelRibbonItem("Phóng to", Icons.Default.ZoomIn, "zoomIn"),
                        ExcelRibbonItem("25%", Icons.Default.ZoomOut, "zoom25"),
                        ExcelRibbonItem("50%", Icons.Default.ZoomOut, "zoom50"),
                        ExcelRibbonItem("75%", Icons.Default.CenterFocusStrong, "zoom75"),
                        ExcelRibbonItem("100%", Icons.Default.CenterFocusStrong, "zoom100"),
                        ExcelRibbonItem("Tỷ lệ khác", Icons.Default.Tune, "zoomPicker"),
                        ExcelRibbonItem("Đường lưới", Icons.Default.GridOn, "gridlines", showGridLines),
                        ExcelRibbonItem("Thanh fx", Icons.Default.Functions, "formulaBar", showFormulaBar),
                        ExcelRibbonItem("Ẩn dải", Icons.Default.ExpandLess, "ribbon")
                    )
                }
            }
            Row(Modifier.fillMaxWidth().height(77.dp).horizontalScroll(rememberScrollState())
                .padding(horizontal = 5.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                actions.forEach { action ->
                    RibbonTool(action, onClick = { applyRibbonAction(action.id) })
                }
            }
        }
        if (searchVisible) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f)
                    .height(50.dp), singleLine = true, placeholder = { Text("Tìm trong trang tính") },
                    textStyle = TextStyle(fontSize = 13.sp))
                TextButton(onClick = {
                    results = (sheet.cells.keys + values.keys.filter { it.startsWith(sheet.partPath + "::") }
                        .map { it.substringAfterLast("::") }).distinct().filter { ref ->
                        rawValue(ref).contains(query, ignoreCase = true) ||
                            cellDisplay(ref).contains(query, ignoreCase = true)
                    }.take(200)
                    resultIndex = 0
                    if (results.isNotEmpty()) navigate(results[0]) else notify("Không tìm thấy dữ liệu phù hợp")
                }, enabled = query.isNotBlank()) { Text("Tìm") }
                TextButton(onClick = {
                    if (results.isNotEmpty()) {
                        resultIndex = (resultIndex + 1) % results.size
                        navigate(results[resultIndex])
                    }
                }, enabled = results.isNotEmpty()) { Text("${if (results.isEmpty()) 0 else resultIndex + 1}/${results.size} ▶") }
            }
        }
        if (filterVisible) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cột ${OfficeOoxml.cellAddress(1, filterColumn).dropLast(1)}", fontSize = 11.sp, color = ExcelGreen)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = filterText, onValueChange = { filterText = it },
                    modifier = Modifier.weight(1f).height(49.dp), singleLine = true,
                    placeholder = { Text("Lọc chứa nội dung...") }, textStyle = TextStyle(fontSize = 13.sp))
                IconButton(onClick = { filterText = "" }) { Icon(Icons.Default.Clear, "Xóa bộ lọc") }
            }
        }
        if (showFormulaBar) {
            Row(Modifier.fillMaxWidth().height(51.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f))
                .padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = addressInput, onValueChange = { addressInput = it },
                    singleLine = true, textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.width(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp))
                        .padding(7.dp))
                IconButton(onClick = { navigate(addressInput) }, modifier = Modifier.size(29.dp)) {
                    Icon(Icons.Default.ArrowForward, "Đến ô", Modifier.size(17.dp))
                }
                Text("fx", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ExcelGreen)
                Spacer(Modifier.width(6.dp))
                BasicTextField(value = formula, onValueChange = { formula = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        setValue(formula)
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.weight(1f).focusRequester(formulaFocusRequester)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp))
                        .padding(horizontal = 7.dp, vertical = 8.dp),
                    singleLine = true, textStyle = TextStyle(fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { field ->
                        Box {
                            if (formula.isEmpty()) Text("Nhập giá trị hoặc công thức (=SUM...)",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            field()
                        }
                    })
                IconButton(onClick = { setValue(formula) }, modifier = Modifier.size(35.dp)) {
                    Icon(Icons.Default.Check, "Nhập vào ô", tint = ExcelGreen)
                }
            }
        }
        if (sheet.truncated) {
            Text("Tệp rất lớn: chỉ nạp tối đa 120.000 ô / 100.000 hàng / 256 cột. Tệp gốc được giữ nguyên.",
                modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF4D6)).padding(5.dp),
                color = Color(0xFF734F0B), fontSize = 10.sp, maxLines = 2)
        }
        ExcelGrid(
            rows = rowMap?.size ?: maxRows, columns = maxCols, selected = selected,
            cellValue = ::cellDisplay, cellAppearance = ::activeStyle, merges = currentMerges,
            rowNumbers = rowMap, showGridLines = showGridLines,
            onCellClick = { selected = it }, onCellDoubleClick = {
                selected = it
                if (!showFormulaBar) showFormulaBar = true
                formulaFocusRequester.requestFocus(); keyboardController?.show()
            }, horizontal = xScroll, vertical = yScroll,
            zoom = zoom, onPinchZoom = { zoom = (zoom * it).coerceIn(EXCEL_MIN_ZOOM, EXCEL_MAX_ZOOM) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 3.dp
        ) {
            Row(
                Modifier.fillMaxWidth().height(67.dp).horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExcelEditTool("In đậm", Icons.Default.FormatBold, selectedAppearance.bold) {
                    applyRibbonAction("bold")
                }
                ExcelEditTool(
                    "Màu chữ", Icons.Default.FormatColorText, selectedAppearance.fontRgb != null,
                    accent = selectedAppearance.fontRgb?.let(::excelComposeColor) ?: ExcelGreen
                ) { colorTarget = ExcelColorTarget.FONT }
                ExcelEditTool(
                    "Màu nền", Icons.Default.FormatColorFill, selectedAppearance.fillRgb != null,
                    accent = selectedAppearance.fillRgb?.let(::excelComposeColor) ?: ExcelGreen
                ) { colorTarget = ExcelColorTarget.FILL }
                ExcelEditTool(
                    if (selectedMerge != null) "Bỏ gộp" else "Gộp ô",
                    Icons.Default.MergeType, selectedMerge != null
                ) {
                    if (selectedMerge != null) unmergeSelected() else {
                        val parts = OfficeOoxml.addressParts(selected)
                        mergeStart = selected
                        mergeEnd = parts?.let { OfficeOoxml.cellAddress(it.first, (it.second + 1).coerceAtMost(maxCols)) } ?: selected
                        mergeDialogVisible = true
                    }
                }
                ExcelEditTool("Công cụ", Icons.Default.Apps, ribbonVisible) { ribbonVisible = !ribbonVisible }
                ExcelEditTool("Bàn phím", Icons.Default.Keyboard, false) {
                    if (!showFormulaBar) showFormulaBar = true
                    formulaFocusRequester.requestFocus(); keyboardController?.show()
                }
            }
        }
        Row(Modifier.fillMaxWidth().height(44.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f))
            .horizontalScroll(rememberScrollState()).padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            document.data.sheets.forEachIndexed { index, data ->
                Surface(onClick = { sheetIndex = index },
                    color = if (index == sheetIndex) MaterialTheme.colorScheme.surface else Color.Transparent,
                    shape = RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                        Text(data.name, maxLines = 1, fontSize = 12.sp,
                            fontWeight = if (index == sheetIndex) FontWeight.Bold else FontWeight.Normal,
                            color = if (index == sheetIndex) ExcelGreen else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().height(34.dp).padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (hasChanges) "${values.size + styleChanges.size + mergeChanges.size} thay đổi chưa lưu" else "Sẵn sàng",
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (hasChanges) ExcelGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { applyRibbonAction("zoomOut") },
                enabled = zoom > EXCEL_MIN_ZOOM + .001f,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Remove, "Thu nhỏ trang tính", Modifier.size(17.dp), tint = ExcelGreen)
            }
            Text(
                "${(zoom * 100).roundToInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ExcelGreen,
                modifier = Modifier.clickable { zoomPickerVisible = true }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            )
            IconButton(
                onClick = { applyRibbonAction("zoomIn") },
                enabled = zoom < EXCEL_MAX_ZOOM - .001f,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Default.Add, "Phóng to trang tính", Modifier.size(17.dp), tint = ExcelGreen)
            }
        }
    }
    if (zoomPickerVisible) {
        AlertDialog(
            onDismissRequest = { zoomPickerVisible = false },
            title = { Text("Tỷ lệ hiển thị Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Thu nhỏ để xem tổng thể nhiều hàng, nhiều cột. Chỉ thay đổi cách xem, không sửa file XLSX.",
                        fontSize = 13.sp
                    )
                    Text("${(zoom * 100).roundToInt()}%", color = ExcelGreen,
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = zoom.coerceIn(EXCEL_MIN_ZOOM, EXCEL_MAX_ZOOM),
                        onValueChange = { zoom = ((it * 20).roundToInt() / 20f).coerceIn(EXCEL_MIN_ZOOM, EXCEL_MAX_ZOOM) },
                        valueRange = EXCEL_MIN_ZOOM..EXCEL_MAX_ZOOM
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.25f, 0.50f, 0.75f, 1.00f).forEach { preset ->
                            OutlinedButton(
                                onClick = { zoom = preset },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                Text("${(preset * 100).roundToInt()}%", fontSize = 12.sp)
                            }
                        }
                    }
                    Text("Khoảng cho phép: 20% – 250%", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { zoomPickerVisible = false }) { Text("Xong") }
            }
        )
    }
    if (functionPickerVisible) {
        AccountingFunctionPicker(onDismiss = { functionPickerVisible = false }, onSelect = { info ->
            functionPickerVisible = false
            showFormulaBar = true
            formula = info.example
            // Do not overwrite a cell until the user confirms the formula.
        })
    }
    if (fileMenuVisible) {
        ModalBottomSheet(onDismissRequest = { fileMenuVisible = false }) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 14.dp, end = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(12.dp), color = ExcelGreen) {
                        Text("S", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(displayName, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${"%.1f".format(document.bytes.size / 1024.0)} KB · ${document.data.sheets.size} worksheet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { fileMenuVisible = false }) { Icon(Icons.Default.Close, "Đóng") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileQuickAction("Lưu dưới\ndạng", Icons.Default.SaveAs, Modifier.weight(1f)) {
                        fileMenuVisible = false; saveWorkbookCopy()
                    }
                    FileQuickAction("Tìm &\nthay thế", Icons.Default.FindReplace, Modifier.weight(1f)) {
                        fileMenuVisible = false; findReplaceVisible = true
                    }
                    FileQuickAction("Chia sẻ", Icons.Default.Share, Modifier.weight(1f)) {
                        fileMenuVisible = false; shareCurrentWorkbook()
                    }
                    FileQuickAction("In", Icons.Default.Print, Modifier.weight(1f)) {
                        fileMenuVisible = false; printCurrentSheet()
                    }
                }
                ExcelMenuCard {
                    FileActionRow("Đổi tên", "Đổi tên tệp tại vị trí hiện tại.", Icons.Default.Edit) {
                        renameInput = displayName; fileMenuVisible = false; renameVisible = true
                    }
                    FileActionRow("Thêm vào", "Đưa tài liệu lên đầu danh sách Tệp gần đây.", Icons.Default.Add) {
                        fileMenuVisible = false; addToRecent()
                    }
                }
                ExcelMenuCard {
                    FileActionRow("Xuất dưới dạng PDF", "Tạo PDF có bảng, màu nền và chữ cơ bản.", Icons.Default.PictureAsPdf) {
                        fileMenuVisible = false; exportSheetPdf()
                    }
                    FileActionRow("Xuất dưới dạng hình ảnh", "Xuất worksheet hiện tại thành ảnh PNG.", Icons.Default.Image) {
                        fileMenuVisible = false; exportSheetPng()
                    }
                    FileActionRow("Tùy chọn chuyển đổi khác", "CSV, TSV và các định dạng dữ liệu cơ bản.", Icons.Default.SwapHoriz) {
                        fileMenuVisible = false; conversionVisible = true
                    }
                }
                ExcelMenuCard {
                    FileActionRow("Hợp nhất worksheet", "Ghép dữ liệu các sheet thành một worksheet mới.", Icons.Default.TableRows) {
                        fileMenuVisible = false; mergeWorksheets()
                    }
                    FileActionRow("Tất cả dịch vụ", "Xem nhanh mọi thao tác offline đang hỗ trợ.", Icons.Default.Apps) {
                        fileMenuVisible = false; servicesVisible = true
                    }
                }
                ExcelMenuCard {
                    FileActionRow("Sao lưu và khôi phục", "Sao lưu cục bộ hoặc xuất bản khôi phục gần nhất.", Icons.Default.History) {
                        fileMenuVisible = false; backupVisible = true
                    }
                    FileActionRow("Mã hóa tài liệu", "Tạo hoặc giải mã bản sao DocFlow .dfx bằng mật khẩu.", Icons.Default.Lock) {
                        fileMenuVisible = false; securityVisible = true
                    }
                    FileActionRow("Danh mục hàm kế toán", "SUMIFS, IF, VLOOKUP, ROUND, ngày tháng và hơn 30 hàm offline.", Icons.Default.Functions) {
                        fileMenuVisible = false; functionPickerVisible = true
                    }
                    FileActionRow("Thông tin tài liệu", "Tên, dung lượng, worksheet và trạng thái chỉnh sửa.", Icons.Default.Info) {
                        fileMenuVisible = false; fileInfoVisible = true
                    }
                    FileActionRow("Mở bằng ứng dụng khác", "Mở bản có cả các chỉnh sửa hiện tại.", Icons.Default.OpenInNew) {
                        fileMenuVisible = false; shareCurrentWorkbook(openOnly = true)
                    }
                }
            }
        }
    }
    if (findReplaceVisible) {
        AlertDialog(
            onDismissRequest = { findReplaceVisible = false },
            title = { Text("Tìm kiếm & thay thế") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Trong sheet: ${sheet.name}", color = ExcelGreen,
                        style = MaterialTheme.typography.labelMedium)
                    OutlinedTextField(
                        value = findNeedle, onValueChange = { findNeedle = it },
                        label = { Text("Nội dung cần tìm") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = replacement, onValueChange = { replacement = it },
                        label = { Text("Thay bằng") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Chỉ thay chữ và số trong các ô đã nạp; bỏ qua ô công thức để tránh hỏng tham chiếu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = {
                            val matches = findTextCells()
                            if (matches.isEmpty()) notify("Không tìm thấy: $findNeedle")
                            else {
                                val index = matches.indexOf(selected)
                                navigate(matches[(index + 1) % matches.size])
                            }
                        }, enabled = findNeedle.isNotBlank()) { Text("Tìm tiếp") }
                        TextButton(onClick = {
                            val body = rawValue(selected)
                            if (findNeedle.isBlank() || body.startsWith("=") ||
                                !body.contains(findNeedle, ignoreCase = true)) {
                                notify("Ô đang chọn không chứa nội dung cần tìm.")
                            } else {
                                setValue(body.replace(findNeedle, replacement, ignoreCase = true))
                                notify("Đã thay nội dung ô $selected")
                            }
                        }, enabled = findNeedle.isNotBlank()) { Text("Thay ô") }
                    }
                    Button(
                        onClick = { replaceAllTextCells(); findReplaceVisible = false },
                        enabled = findNeedle.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Thay tất cả trong sheet") }
                }
            },
            confirmButton = { TextButton(onClick = { findReplaceVisible = false }) { Text("Đóng") } }
        )
    }
    colorTarget?.let { target ->
        val colors = listOf(
            "000000", "FFFFFF", "C00000", "FF0000", "ED7D31", "FFC000",
            "70AD47", "00B050", "00B0F0", "0070C0", "4472C4", "7030A0",
            "F4CCCC", "FCE5CD", "FFF2CC", "D9EAD3", "D0E0E3", "CFE2F3",
            "D9D2E9", "EAD1DC"
        )
        AlertDialog(
            onDismissRequest = { colorTarget = null },
            title = { Text(if (target == ExcelColorTarget.FONT) "Màu phông chữ" else "Màu nền ô") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    colors.chunked(5).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { rgb ->
                                val color = excelComposeColor(rgb)
                                Surface(
                                    onClick = {
                                        if (target == ExcelColorTarget.FONT) {
                                            setAppearance { old, _ -> old.copy(fontRgb = rgb) }
                                        } else {
                                            setAppearance { old, _ -> old.copy(fillRgb = rgb) }
                                        }
                                        colorTarget = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = color,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.size(44.dp)
                                ) {}
                            }
                            repeat(5 - row.size) { Spacer(Modifier.size(44.dp)) }
                        }
                    }
                    TextButton(onClick = {
                        if (target == ExcelColorTarget.FONT) setAppearance { old, _ -> old.copy(fontRgb = "") }
                        else setAppearance { old, _ -> old.copy(fillRgb = "") }
                        colorTarget = null
                    }) { Text("Bỏ màu tùy chỉnh") }
                }
            },
            confirmButton = { TextButton(onClick = { colorTarget = null }) { Text("Đóng") } }
        )
    }
    if (mergeDialogVisible) {
        AlertDialog(
            onDismissRequest = { mergeDialogVisible = false },
            title = { Text("Gộp ô ngang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Nhập ô đầu và ô cuối trong cùng một hàng. Dữ liệu chỉ được giữ ở ô đầu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(mergeStart, { mergeStart = it.uppercase(Locale.ROOT) },
                            label = { Text("Ô đầu") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(mergeEnd, { mergeEnd = it.uppercase(Locale.ROOT) },
                            label = { Text("Ô cuối") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = { Button(onClick = {
                mergeDialogVisible = false; applyMerge(mergeStart, mergeEnd)
            }) { Text("Gộp") } },
            dismissButton = { TextButton(onClick = { mergeDialogVisible = false }) { Text("Hủy") } }
        )
    }
    if (renameVisible) {
        AlertDialog(
            onDismissRequest = { renameVisible = false },
            title = { Text("Đổi tên tài liệu") },
            text = { OutlinedTextField(renameInput, { renameInput = it }, label = { Text("Tên tệp") },
                singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = { renameCurrentDocument() }) { Text("Đổi tên") } },
            dismissButton = { TextButton(onClick = { renameVisible = false }) { Text("Hủy") } }
        )
    }
    if (conversionVisible) {
        AlertDialog(
            onDismissRequest = { conversionVisible = false },
            title = { Text("Chuyển đổi worksheet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Chọn định dạng xuất cho worksheet ${sheet.name}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { conversionVisible = false; exportSheetDelimited(',', "csv", DocumentStore.CSV) },
                        modifier = Modifier.fillMaxWidth()) { Text("Xuất CSV") }
                    OutlinedButton(onClick = { conversionVisible = false; exportSheetDelimited('\t', "tsv", "text/tab-separated-values") },
                        modifier = Modifier.fillMaxWidth()) { Text("Xuất TSV / văn bản tab") }
                    OutlinedButton(onClick = { conversionVisible = false; exportSheetPdf() },
                        modifier = Modifier.fillMaxWidth()) { Text("Xuất PDF") }
                    OutlinedButton(onClick = { conversionVisible = false; exportSheetPng() },
                        modifier = Modifier.fillMaxWidth()) { Text("Xuất PNG") }
                }
            },
            confirmButton = { TextButton(onClick = { conversionVisible = false }) { Text("Đóng") } }
        )
    }
    if (backupVisible) {
        AlertDialog(
            onDismissRequest = { backupVisible = false },
            title = { Text("Sao lưu và khôi phục") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bản sao cục bộ nằm trong vùng riêng của ứng dụng và không tải lên mạng.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { createLocalBackup() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Tạo bản sao cục bộ")
                    }
                    OutlinedButton(onClick = { restoreLatestBackup() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.History, null); Spacer(Modifier.width(8.dp)); Text("Khôi phục bản gần nhất")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { backupVisible = false }) { Text("Đóng") } }
        )
    }
    if (securityVisible) {
        AlertDialog(
            onDismissRequest = { securityVisible = false },
            title = { Text("Bảo vệ tài liệu") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("DocFlow dùng AES-GCM để tạo bản sao .dfx. Đây không phải mật khẩu XLSX chuẩn của Microsoft Excel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        securityVisible = false; encryptPassword = ""; encryptConfirm = ""; encryptPasswordVisible = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Mã hóa tài liệu hiện tại") }
                    OutlinedButton(onClick = {
                        securityVisible = false; decryptPicker.launch(arrayOf("application/octet-stream", "*/*"))
                    }, modifier = Modifier.fillMaxWidth()) { Text("Giải mã bản sao .dfx") }
                }
            },
            confirmButton = { TextButton(onClick = { securityVisible = false }) { Text("Đóng") } }
        )
    }
    if (encryptPasswordVisible) {
        AlertDialog(
            onDismissRequest = { encryptPasswordVisible = false },
            title = { Text("Tạo bản sao mã hóa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(encryptPassword, { encryptPassword = it }, label = { Text("Mật khẩu") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(encryptConfirm, { encryptConfirm = it }, label = { Text("Nhập lại mật khẩu") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Hãy lưu mật khẩu ở nơi an toàn. DocFlow không thể khôi phục mật khẩu đã quên.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { Button(onClick = {
                when {
                    encryptPassword.length < 6 -> notify("Mật khẩu cần ít nhất 6 ký tự.")
                    encryptPassword != encryptConfirm -> notify("Hai mật khẩu không trùng nhau.")
                    else -> {
                        val valueSnapshot = values.toMap(); val styleSnapshot = styleChanges.toMap();
                        val mergeSnapshot = mergeChanges.toMap(); val password = encryptPassword
                        encryptPasswordVisible = false
                        save("${displayName.substringBeforeLast('.')}.dfx", "application/octet-stream", false) { _, output ->
                            output.write(DocFlowCrypto.encrypt(
                                workbookBytes(valueSnapshot, styleSnapshot, mergeSnapshot), password, displayName
                            ))
                        }
                    }
                }
            }) { Text("Mã hóa") } },
            dismissButton = { TextButton(onClick = { encryptPasswordVisible = false }) { Text("Hủy") } }
        )
    }
    if (decryptPasswordVisible) {
        AlertDialog(
            onDismissRequest = { decryptPasswordVisible = false; pendingEncryptedUri = null },
            title = { Text("Giải mã bản sao DocFlow") },
            text = { OutlinedTextField(decryptPassword, { decryptPassword = it }, label = { Text("Mật khẩu") },
                singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { Button(onClick = {
                val uri = pendingEncryptedUri ?: return@Button
                val password = decryptPassword
                scope.launch {
                    try {
                        val decoded = withContext(Dispatchers.IO) {
                            DocFlowCrypto.decrypt(DocumentStore.readUpTo(context, uri, 100 * 1024 * 1024), password)
                        }
                        decryptPasswordVisible = false; pendingEncryptedUri = null
                        val name = decoded.filename.substringBeforeLast('.', decoded.filename) + "_giai_ma.xlsx"
                        save(name, DocumentStore.XLSX, true) { _, output -> output.write(decoded.bytes) }
                    } catch (e: Exception) {
                        notify("Giải mã thất bại: ${e.message ?: "sai mật khẩu"}")
                    }
                }
            }) { Text("Giải mã") } },
            dismissButton = { TextButton(onClick = { decryptPasswordVisible = false; pendingEncryptedUri = null }) { Text("Hủy") } }
        )
    }
    if (servicesVisible) {
        AlertDialog(
            onDismissRequest = { servicesVisible = false },
            title = { Text("Dịch vụ Excel offline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("• Chỉnh sửa ô, hơn 40 hàm tính thử offline, màu chữ/nền và gộp ô ngang")
                    Text("• Tìm & thay thế, lọc/sắp xếp hiển thị, zoom")
                    Text("• Lưu XLSX, chia sẻ, in, đổi tên và mở bằng ứng dụng khác")
                    Text("• Xuất PDF, PNG, CSV, TSV và hợp nhất worksheet")
                    Text("• Sao lưu cục bộ và mã hóa bản sao .dfx")
                }
            },
            confirmButton = { TextButton(onClick = { servicesVisible = false }) { Text("Đóng") } }
        )
    }
    if (fileInfoVisible) {
        AlertDialog(
            onDismissRequest = { fileInfoVisible = false },
            title = { Text("Thông tin Excel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tên: $displayName")
                    Text("Dung lượng gốc: ${"%.1f".format(document.bytes.size / 1024.0)} KB")
                    Text("Số worksheet: ${document.data.sheets.size}")
                    Text("Worksheet hiện tại: ${sheet.name}")
                    Text("Ô sửa nội dung: ${values.size}")
                    Text("Ô đổi định dạng: ${styleChanges.size}")
                    Text("Thao tác gộp/bỏ gộp: ${mergeChanges.size}")
                    Text("Tệp gốc vẫn được giữ nguyên cho đến khi bạn chọn Lưu dưới dạng.")
                }
            },
            confirmButton = { TextButton(onClick = { fileInfoVisible = false }) { Text("Đóng") } }
        )
    }
    if (infoVisible) AlertDialog(onDismissRequest = { infoVisible = false },
        title = { Text("Giới hạn trình Excel offline") },
        text = { Text("Lưu thành bản sao. DocFlow tính thử nhiều hàm kế toán offline (SUMIFS, COUNTIFS, IF, ROUND, VLOOKUP tìm chính xác, TEXT, DATE...). Mỗi vùng tính tối đa 20.000 ô; công thức chưa hỗ trợ vẫn được giữ nguyên và Excel/LibreOffice sẽ tính lại khi mở tệp. Lọc/sắp xếp chỉ thay đổi cách xem. PivotTable, macro và công thức đặc biệt cần bộ máy Office đầy đủ.") },
        confirmButton = { TextButton(onClick = { infoVisible = false }) { Text("Đóng") } })
}

@Composable
private fun RibbonTool(item: ExcelRibbonItem, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(7.dp),
        color = if (item.active) ExcelPale else Color.Transparent,
        modifier = Modifier.widthIn(min = 56.dp).fillMaxHeight()) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(item.icon, contentDescription = item.title, tint = ExcelGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(5.dp))
            Text(item.title, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}


@Composable
private fun ExcelEditTool(
    label: String,
    icon: ImageVector,
    active: Boolean,
    accent: Color = ExcelGreen,
    action: () -> Unit
) {
    Surface(
        onClick = action,
        shape = RoundedCornerShape(10.dp),
        color = if (active) ExcelPale else Color.Transparent,
        modifier = Modifier.widthIn(min = 68.dp).fillMaxHeight()
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, label, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun FileQuickAction(label: String, icon: ImageVector, modifier: Modifier, action: () -> Unit) {
    Surface(
        onClick = action,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
    ) {
        Column(
            Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, label, tint = ExcelGreen, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
        }
    }
}

@Composable
private fun ExcelMenuCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .38f),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.fillMaxWidth(), content = content) }
}

@Composable
private fun FileActionRow(title: String, subtitle: String, icon: ImageVector, action: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = action).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, title, tint = ExcelGreen, modifier = Modifier.size(26.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun excelComposeColor(rgb: String): Color = runCatching {
    Color(android.graphics.Color.parseColor("#${rgb.takeLast(6)}"))
}.getOrDefault(ExcelGreen)

private fun printPdfBytes(context: android.content.Context, name: String, bytes: ByteArray) {
    val manager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
    manager.print(name, object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?, callback: PrintDocumentAdapter.LayoutResultCallback, extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) callback.onLayoutCancelled()
            else callback.onLayoutFinished(
                PrintDocumentInfo.Builder(name).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build(), true
            )
        }

        override fun onWrite(
            pages: Array<out PageRange>, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal, callback: PrintDocumentAdapter.WriteResultCallback
        ) {
            Thread {
                try {
                    if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return@Thread }
                    FileOutputStream(destination.fileDescriptor).use { it.write(bytes); it.flush() }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message ?: "Không thể ghi bản in")
                }
            }.start()
        }
    }, null)
}


// ================ ExcelGrid.kt ================
// One canvas per visible row. No 100,000 x 256 Compose TextFields or cell Views.
internal const val GRID_COLUMN_DP = 124
private const val GRID_ROW_DP = 43
private const val GRID_INDEX_DP = 50

@Composable
internal fun ExcelGrid(
    rows: Int,
    columns: Int,
    selected: String,
    cellValue: (String) -> String,
    cellAppearance: (String) -> OfficeOoxml.CellAppearance,
    merges: List<OfficeOoxml.MergeRange> = emptyList(),
    rowNumbers: List<Int>? = null,
    showGridLines: Boolean = true,
    onCellClick: (String) -> Unit,
    onCellDoubleClick: (String) -> Unit = onCellClick,
    horizontal: ScrollState,
    vertical: LazyListState,
    zoom: Float,
    onPinchZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val bg = scheme.surface
    val header = scheme.surfaceVariant
    val foreground = scheme.onSurface
    val selectedBg = androidx.compose.ui.graphics.Color(0xFFDAF0E4)
    val selectedFg = androidx.compose.ui.graphics.Color(0xFF155B38)
    val outline = scheme.outlineVariant
    val density = LocalDensity.current
    val scale = zoom.coerceIn(EXCEL_MIN_ZOOM, EXCEL_MAX_ZOOM)
    val columnWidth = GRID_COLUMN_DP.dp * scale
    val rowHeight = GRID_ROW_DP.dp * scale
    val indexWidth = GRID_INDEX_DP.dp * scale
    val columnWidthPx = with(density) { columnWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val contentWidth = (columns * GRID_COLUMN_DP).dp * scale
    val currentClick = rememberUpdatedState(onCellClick)
    val currentDoubleClick = rememberUpdatedState(onCellDoubleClick)
    val currentPinch = rememberUpdatedState(onPinchZoom)
    val mergeList = remember(merges) { merges.sortedWith(compareBy<OfficeOoxml.MergeRange> { it.startRow }.thenBy { it.startCol }) }
    val labelSize = with(density) { (13f * scale).sp.toPx() }
    val regularPaint = remember(foreground, labelSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = foreground.toArgb(); textSize = labelSize }
    }
    val highlightedPaint = remember(selectedFg, labelSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = selectedFg.toArgb(); textSize = labelSize; isFakeBoldText = true }
    }

    Column(
        modifier.background(bg, RoundedCornerShape(10.dp)).clipToBounds()
            .pointerInput(Unit) {
                // React only when at least two fingers are touching. Single-finger scrolling
                // remains owned by the horizontal ScrollState / vertical LazyColumn.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val oldDistance = (pressed[0].previousPosition - pressed[1].previousPosition).getDistance()
                            val newDistance = (pressed[0].position - pressed[1].position).getDistance()
                            if (oldDistance > 2f && newDistance > 2f) {
                                val factor = (newDistance / oldDistance).coerceIn(0.8f, 1.25f)
                                if (kotlin.math.abs(factor - 1f) > 0.005f) currentPinch.value(factor)
                            }
                            pressed.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
        Row(Modifier.fillMaxWidth().height(38.dp * scale)) {
            GridIndex("", true, Modifier.width(indexWidth), scale)
            BoxWithConstraints(Modifier.weight(1f).clipToBounds()) {
                val viewportWidthPx = constraints.maxWidth.toFloat()
                Row(Modifier.horizontalScroll(horizontal)) {
                    Canvas(Modifier.width(contentWidth).fillMaxHeight()) {
                        val start = (horizontal.value / columnWidthPx).toInt().coerceIn(0, columns - 1)
                        val end = min(columns - 1, start + ceil(viewportWidthPx / columnWidthPx).toInt() + 2)
                        val canvas = drawContext.canvas.nativeCanvas
                        for (c in start..end) {
                            val x = c * columnWidthPx
                            drawRect(header, topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(columnWidthPx, size.height))
                            drawLine(outline, Offset(x + columnWidthPx, 0f), Offset(x + columnWidthPx, size.height), 0.7.dp.toPx())
                            val label = OfficeOoxml.cellAddress(1, c + 1).dropLast(1)
                            val paint = regularPaint
                            paint.color = foreground.toArgb(); paint.isFakeBoldText = false
                            paint.isUnderlineText = false; paint.textSkewX = 0f
                            val textWidth = paint.measureText(label)
                            canvas.drawText(label, x + (columnWidthPx - textWidth) / 2, size.height / 2 + labelSize / 3, paint)
                        }
                        drawLine(outline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 0.7.dp.toPx())
                    }
                }
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), state = vertical) {
            items(count = rows, key = { it }) { zeroBased ->
                val rowNumber = rowNumbers?.getOrNull(zeroBased) ?: (zeroBased + 1)
                Row(Modifier.fillMaxWidth().height(rowHeight), verticalAlignment = Alignment.CenterVertically) {
                    val selectedRow = selected.dropWhile(Char::isLetter).toIntOrNull() == rowNumber
                    GridIndex(rowNumber.toString(), selectedRow, Modifier.width(indexWidth), scale)
                    BoxWithConstraints(Modifier.weight(1f).fillMaxHeight().clipToBounds()) {
                        val viewportWidthPx = constraints.maxWidth.toFloat()
                        Row(Modifier.horizontalScroll(horizontal)) {
                            Canvas(
                                Modifier.width(contentWidth).fillMaxHeight()
                                    .pointerInput(rowNumber, columns, columnWidthPx, mergeList) {
                                        fun addressAt(point: Offset): String {
                                            val c = (floor(point.x / columnWidthPx).toInt() + 1).coerceIn(1, columns)
                                            val merge = mergeList.firstOrNull { it.contains(rowNumber, c) }
                                            return if (merge != null) OfficeOoxml.cellAddress(merge.startRow, merge.startCol)
                                            else OfficeOoxml.cellAddress(rowNumber, c)
                                        }
                                        detectTapGestures(
                                            onTap = { currentClick.value(addressAt(it)) },
                                            onDoubleTap = { currentDoubleClick.value(addressAt(it)) }
                                        )
                                    }
                            ) {
                                val start = (horizontal.value / columnWidthPx).toInt().coerceIn(0, columns - 1)
                                // draw only visible columns even though the canvas spans all columns
                                val end = min(columns - 1, start + ceil(viewportWidthPx / columnWidthPx).toInt() + 2)
                                val canvas = drawContext.canvas.nativeCanvas
                                var column = start
                                while (column <= end) {
                                    val colOne = column + 1
                                    val merge = mergeList.firstOrNull { it.contains(rowNumber, colOne) }
                                    val visibleStartOne = if (merge != null) maxOf(merge.startCol, start + 1) else colOne
                                    if (merge != null && colOne != visibleStartOne) { column++; continue }
                                    val spanEndOne = if (merge != null) minOf(merge.endCol, end + 1, columns) else colOne
                                    val span = spanEndOne - visibleStartOne + 1
                                    val x = (visibleStartOne - 1) * columnWidthPx
                                    val width = span * columnWidthPx
                                    val address = if (merge != null) OfficeOoxml.cellAddress(merge.startRow, merge.startCol)
                                        else OfficeOoxml.cellAddress(rowNumber, colOne)
                                    val isSelected = if (merge != null) merge.contains(selected) else address == selected
                                    val style = cellAppearance(address)
                                    if (!style.fillRgb.isNullOrBlank() && style.fillRgb.matches(Regex("[0-9A-Fa-f]{6}"))) {
                                        drawRect(androidx.compose.ui.graphics.Color(
                                            android.graphics.Color.parseColor("#${style.fillRgb}")),
                                            topLeft = Offset(x, 0f),
                                            size = androidx.compose.ui.geometry.Size(width, size.height))
                                    }
                                    if (isSelected) {
                                        drawRect(selectedBg.copy(alpha = 0.55f), topLeft = Offset(x, 0f),
                                            size = androidx.compose.ui.geometry.Size(width, size.height))
                                    }
                                    val value = if (merge == null || rowNumber == merge.startRow) cellValue(address) else ""
                                    if (value.isNotEmpty()) {
                                        val paint = if (isSelected) highlightedPaint else regularPaint
                                        paint.color = style.fontRgb?.takeIf { it.matches(Regex("[0-9A-Fa-f]{6}")) }
                                            ?.let { android.graphics.Color.parseColor("#$it") }
                                            ?: if (isSelected) selectedFg.toArgb() else foreground.toArgb()
                                        paint.isFakeBoldText = isSelected || style.bold
                                        paint.isUnderlineText = style.underline
                                        paint.textSkewX = if (style.italic) -0.18f else 0f
                                        val display = value.replace('\n', ' ')
                                        val textWidth = paint.measureText(display)
                                        val margin = 8.dp.toPx()
                                        val textX = when (style.alignment) {
                                            "right" -> x + width - textWidth - margin
                                            "center" -> x + (width - textWidth) / 2f
                                            else -> x + margin
                                        }
                                        canvas.save()
                                        canvas.clipRect(x + margin / 2, 0f, x + width - margin / 2, size.height)
                                        canvas.drawText(display, textX, rowHeightPx / 2 + labelSize / 3, paint)
                                        canvas.restore()
                                    }
                                    if (showGridLines) {
                                        drawLine(outline, Offset(x, 0f), Offset(x, size.height), 0.65.dp.toPx())
                                        drawLine(outline, Offset(x + width, 0f), Offset(x + width, size.height), 0.65.dp.toPx())
                                    }
                                    if (isSelected) {
                                        val green = androidx.compose.ui.graphics.Color(0xFF167346)
                                        drawLine(green, Offset(x, 1f), Offset(x + width, 1f), 2.dp.toPx())
                                        drawLine(green, Offset(x, size.height - 1f), Offset(x + width, size.height - 1f), 2.dp.toPx())
                                        drawLine(green, Offset(x + 1f, 0f), Offset(x + 1f, size.height), 2.dp.toPx())
                                        drawLine(green, Offset(x + width - 1f, 0f), Offset(x + width - 1f, size.height), 2.dp.toPx())
                                    }
                                    column = spanEndOne
                                }
                                drawLine(outline, Offset(0f, size.height - 0.5f),
                                    Offset(size.width, size.height - 0.5f), 0.65.dp.toPx())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridIndex(label: String, active: Boolean, modifier: Modifier, scale: Float) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.fillMaxHeight()
            .background(if (active) androidx.compose.ui.graphics.Color(0xFFDAF0E4) else scheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = (12f * scale).sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) androidx.compose.ui.graphics.Color(0xFF155B38) else scheme.onSurfaceVariant)
    }
}


private data class AccountingFunctionInfo(
    val name: String,
    val category: String,
    val meaning: String,
    val syntax: String,
    val example: String
)

/** Genuine on-device formula picker: selecting a function only edits the fx field. */
private val accountingFunctionCatalog = listOf(
    AccountingFunctionInfo("SUM", "Tổng hợp", "Cộng số trong vùng", "SUM(vùng)", "=SUM(B2:B100)"),
    AccountingFunctionInfo("SUMIF", "Tổng hợp", "Cộng theo một điều kiện", "SUMIF(vùng_đk;đk;vùng_cộng)", "=SUMIF(A2:A100;\"Đã thu\";B2:B100)"),
    AccountingFunctionInfo("SUMIFS", "Tổng hợp", "Cộng theo nhiều điều kiện", "SUMIFS(vùng_cộng;vùng_đk1;đk1;...)", "=SUMIFS(C2:C100;A2:A100;\"Đã thu\";B2:B100;\">=100\")"),
    AccountingFunctionInfo("COUNT", "Tổng hợp", "Đếm ô chứa số", "COUNT(vùng)", "=COUNT(B2:B100)"),
    AccountingFunctionInfo("COUNTA", "Tổng hợp", "Đếm ô có dữ liệu", "COUNTA(vùng)", "=COUNTA(A2:A100)"),
    AccountingFunctionInfo("COUNTBLANK", "Tổng hợp", "Đếm ô trống", "COUNTBLANK(vùng)", "=COUNTBLANK(A2:A100)"),
    AccountingFunctionInfo("COUNTIF", "Tổng hợp", "Đếm theo một điều kiện", "COUNTIF(vùng;đk)", "=COUNTIF(B2:B100;\">=100\")"),
    AccountingFunctionInfo("COUNTIFS", "Tổng hợp", "Đếm theo nhiều điều kiện", "COUNTIFS(vùng1;đk1;vùng2;đk2)", "=COUNTIFS(A2:A100;\"Đã thu\";B2:B100;\">0\")"),
    AccountingFunctionInfo("AVERAGE", "Tổng hợp", "Tính trung bình", "AVERAGE(vùng)", "=AVERAGE(B2:B100)"),
    AccountingFunctionInfo("AVERAGEIF", "Tổng hợp", "Trung bình theo điều kiện", "AVERAGEIF(vùng;đk;vùng_tb)", "=AVERAGEIF(A2:A100;\"Đạt\";B2:B100)"),
    AccountingFunctionInfo("AVERAGEIFS", "Tổng hợp", "Trung bình nhiều điều kiện", "AVERAGEIFS(vùng_tb;vùng_đk;đk)", "=AVERAGEIFS(C2:C100;A2:A100;\"Đạt\";B2:B100;\">0\")"),
    AccountingFunctionInfo("MIN", "Tổng hợp", "Giá trị nhỏ nhất", "MIN(vùng)", "=MIN(B2:B100)"),
    AccountingFunctionInfo("MAX", "Tổng hợp", "Giá trị lớn nhất", "MAX(vùng)", "=MAX(B2:B100)"),
    AccountingFunctionInfo("SUBTOTAL", "Tổng hợp", "Tổng hợp dữ liệu; mã 9 = SUM, 1 = AVERAGE", "SUBTOTAL(9;vùng)", "=SUBTOTAL(9;B2:B100)"),
    AccountingFunctionInfo("SUMPRODUCT", "Tổng hợp", "Tổng tích: số lượng × đơn giá", "SUMPRODUCT(vùng_số_lượng;vùng_đơn_giá)", "=SUMPRODUCT(B2:B100;C2:C100)"),
    AccountingFunctionInfo("IF", "Điều kiện", "Phân loại Đạt / Không đạt", "IF(điều_kiện;đúng;sai)", "=IF(B2>=100;\"Đạt\";\"Không đạt\")"),
    AccountingFunctionInfo("IFERROR", "Điều kiện", "Trả giá trị thay thế nếu lỗi", "IFERROR(công_thức;giá_trị_khi_lỗi)", "=IFERROR(A2/B2;0)"),
    AccountingFunctionInfo("IFNA", "Điều kiện", "Thay thế lỗi không tìm thấy", "IFNA(công_thức;khi_không_tìm_thấy)", "=IFNA(VLOOKUP(A2;D2:E100;2;FALSE);0)"),
    AccountingFunctionInfo("AND", "Điều kiện", "Tất cả điều kiện đúng", "AND(đk1;đk2)", "=AND(B2>0;C2>0)"),
    AccountingFunctionInfo("OR", "Điều kiện", "Ít nhất một điều kiện đúng", "OR(đk1;đk2)", "=OR(B2>0;C2>0)"),
    AccountingFunctionInfo("NOT", "Điều kiện", "Đảo giá trị đúng/sai", "NOT(điều_kiện)", "=NOT(B2=0)"),
    AccountingFunctionInfo("ROUND", "Làm tròn", "Làm tròn số tiền", "ROUND(số;số_chữ_số)", "=ROUND(B2;0)"),
    AccountingFunctionInfo("ROUNDUP", "Làm tròn", "Làm tròn xa 0", "ROUNDUP(số;số_chữ_số)", "=ROUNDUP(B2;0)"),
    AccountingFunctionInfo("ROUNDDOWN", "Làm tròn", "Làm tròn về 0", "ROUNDDOWN(số;số_chữ_số)", "=ROUNDDOWN(B2;0)"),
    AccountingFunctionInfo("TRUNC", "Làm tròn", "Cắt phần thập phân", "TRUNC(số;số_chữ_số)", "=TRUNC(B2;2)"),
    AccountingFunctionInfo("INT", "Làm tròn", "Làm tròn xuống số nguyên", "INT(số)", "=INT(B2)"),
    AccountingFunctionInfo("ABS", "Làm tròn", "Giá trị tuyệt đối", "ABS(số)", "=ABS(B2)"),
    AccountingFunctionInfo("MOD", "Làm tròn", "Lấy số dư phép chia", "MOD(số;số_chia)", "=MOD(B2;12)"),
    AccountingFunctionInfo("VLOOKUP", "Tra cứu", "Tra mã hàng, đơn giá chính xác", "VLOOKUP(mã;bảng;cột;FALSE)", "=VLOOKUP(A2;D2:E100;2;FALSE)"),
    AccountingFunctionInfo("HLOOKUP", "Tra cứu", "Tra cứu theo hàng (chính xác)", "HLOOKUP(mã;bảng;hàng;FALSE)", "=HLOOKUP(A2;D1:L4;2;FALSE)"),
    AccountingFunctionInfo("XLOOKUP", "Tra cứu", "Tra cứu mã linh hoạt", "XLOOKUP(mã;vùng_tìm;vùng_trả;giá_trị_thiếu)", "=XLOOKUP(A2;D2:D100;E2:E100;\"Không thấy\")"),
    AccountingFunctionInfo("INDEX", "Tra cứu", "Lấy giá trị theo hàng/cột", "INDEX(vùng;hàng;cột)", "=INDEX(D2:E100;2;1)"),
    AccountingFunctionInfo("MATCH", "Tra cứu", "Tìm vị trí mã", "MATCH(mã;vùng;0)", "=MATCH(A2;D2:D100;0)"),
    AccountingFunctionInfo("LEFT", "Văn bản", "Lấy ký tự từ trái", "LEFT(chuỗi;số_ký_tự)", "=LEFT(A2;3)"),
    AccountingFunctionInfo("RIGHT", "Văn bản", "Lấy ký tự từ phải", "RIGHT(chuỗi;số_ký_tự)", "=RIGHT(A2;4)"),
    AccountingFunctionInfo("MID", "Văn bản", "Trích chuỗi ở giữa", "MID(chuỗi;vị_trí;số_ký_tự)", "=MID(A2;2;4)"),
    AccountingFunctionInfo("LEN", "Văn bản", "Độ dài mã", "LEN(chuỗi)", "=LEN(A2)"),
    AccountingFunctionInfo("TRIM", "Văn bản", "Xóa khoảng trắng dư", "TRIM(chuỗi)", "=TRIM(A2)"),
    AccountingFunctionInfo("UPPER", "Văn bản", "Viết hoa", "UPPER(chuỗi)", "=UPPER(A2)"),
    AccountingFunctionInfo("LOWER", "Văn bản", "Viết thường", "LOWER(chuỗi)", "=LOWER(A2)"),
    AccountingFunctionInfo("CONCAT", "Văn bản", "Nối các chuỗi", "CONCAT(giá_trị1;giá_trị2)", "=CONCAT(A2;\"-\";B2)"),
    AccountingFunctionInfo("TEXT", "Văn bản", "Định dạng số thành chữ", "TEXT(số;định_dạng)", "=TEXT(B2;\"#,##0\")"),
    AccountingFunctionInfo("VALUE", "Văn bản", "Chuyển số dạng chữ sang số", "VALUE(chuỗi)", "=VALUE(A2)"),
    AccountingFunctionInfo("TODAY", "Ngày tháng", "Ngày hiện tại trên điện thoại", "TODAY()", "=TODAY()"),
    AccountingFunctionInfo("NOW", "Ngày tháng", "Thời gian hiện tại trên điện thoại", "NOW()", "=NOW()"),
    AccountingFunctionInfo("DATE", "Ngày tháng", "Tạo ngày từ năm/tháng/ngày", "DATE(năm;tháng;ngày)", "=DATE(2026;10;21)"),
    AccountingFunctionInfo("YEAR", "Ngày tháng", "Lấy năm", "YEAR(ngày)", "=YEAR(A2)"),
    AccountingFunctionInfo("MONTH", "Ngày tháng", "Lấy tháng", "MONTH(ngày)", "=MONTH(A2)"),
    AccountingFunctionInfo("DAY", "Ngày tháng", "Lấy ngày", "DAY(ngày)", "=DAY(A2)"),
    AccountingFunctionInfo("EOMONTH", "Ngày tháng", "Ngày cuối tháng", "EOMONTH(ngày;số_tháng)", "=EOMONTH(A2;0)"),
    AccountingFunctionInfo("DAYS", "Ngày tháng", "Số ngày giữa hai mốc", "DAYS(ngày_cuối;ngày_đầu)", "=DAYS(B2;A2)"),
    AccountingFunctionInfo("NETWORKDAYS", "Ngày tháng", "Đếm ngày làm việc (thứ 2–6)", "NETWORKDAYS(đầu;cuối;ngày_nghỉ)", "=NETWORKDAYS(A2;B2)"),
)

@Composable
private fun AccountingFunctionPicker(
    onDismiss: () -> Unit,
    onSelect: (AccountingFunctionInfo) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    val categories = listOf("Tất cả", "Tổng hợp", "Điều kiện", "Làm tròn", "Tra cứu", "Văn bản", "Ngày tháng")
    val results = remember(search, category) {
        accountingFunctionCatalog.filter { info ->
            (category == "Tất cả" || info.category == category) &&
                (search.isBlank() || listOf(info.name, info.meaning, info.syntax)
                    .any { it.contains(search.trim(), ignoreCase = true) })
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Hàm Excel dành cho kế toán", fontWeight = FontWeight.Bold)
                Text("${accountingFunctionCatalog.size} hàm · tính thử offline",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Tìm hàm: SUMIF, làm tròn, tra mã…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    categories.forEach { name ->
                        FilterChip(selected = category == name, onClick = { category = name },
                            label = { Text(name, fontSize = 11.sp) })
                    }
                }
                if (results.isEmpty()) {
                    Text("Chưa tìm thấy hàm phù hợp", modifier = Modifier.padding(12.dp))
                } else {
                    LazyColumn(Modifier.heightIn(max = 350.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(results.size) { index ->
                            val info = results[index]
                            Surface(onClick = { onSelect(info) },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .4f),
                                shape = RoundedCornerShape(9.dp)) {
                                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(info.name, color = ExcelGreen, fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Add, "Chọn hàm", Modifier.size(18.dp), tint = ExcelGreen)
                                    }
                                    Text(info.meaning, fontSize = 12.sp)
                                    Text(info.syntax, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(info.example, fontSize = 11.sp, color = ExcelGreen, maxLines = 2)
                                }
                            }
                        }
                    }
                }
                Text("Chọn hàm để đưa ví dụ vào thanh fx, sửa phạm vi rồi nhấn ✓. Dùng dấu ; hoặc , ngăn tham số. Một số công thức Excel nâng cao chưa được tính thử.",
                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}
/**
 * DocFlow spreadsheet offline preview. Bounded formula subset, not a full Excel calculation engine.
 * Unsupported formulas return null: preserve the original formula and let Excel recalculate on open.
 * Arguments accept both ',' and ';'. Numeric decimals in expressions use '.'.
 */
internal object AccountingFormulas {
    private const val MAX_RANGE_CELLS = 20000
    private const val MAX_STEPS = 65000
    private const val MAX_NESTING = 24
    private val excelEpoch = LocalDate.of(1899, 12, 30)
    private class Unsupported : RuntimeException()
    private sealed class Value {
        data class Number(val v: Double, val date: Boolean = false, val dateTime: Boolean = false) : Value()
        data class Text(val v: String) : Value()
        data class BooleanValue(val v: Boolean) : Value()
        data class Matrix(val rows: Int, val cols: Int, val list: List<Value>) : Value()
        data class Error(val code: String) : Value()
        object Empty : Value()
    }
    private sealed class Node {
        data class Constant(val v: Value) : Node()
        data class Reference(val sheet: String?, val address: String) : Node()
        data class Range(val sheet: String?, val first: String, val last: String) : Node()
        data class Unary(val op: String, val node: Node) : Node()
        data class Binary(val op: String, val a: Node, val b: Node) : Node()
        data class Call(val name: String, val args: List<Node>) : Node()
    }
    private data class Token(val kind: String, val text: String)

    private fun fmt(number: Double): String {
        if (!number.isFinite()) return "#NUM!"
        if (number == 0.0) return "0"
        if (abs(number) < 9.0e15 && number == number.toLong().toDouble()) return number.toLong().toString()
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString()
    }
    private fun render(value: Value): String = when (value) {
        is Value.Number -> when {
            value.dateTime -> excelEpoch.plusDays(value.v.toLong()).toString() + " " +
                java.time.LocalTime.ofSecondOfDay(((value.v - floor(value.v)) * 86400).toLong().coerceIn(0, 86399)).toString()
            value.date -> excelEpoch.plusDays(value.v.toLong()).toString()
            else -> fmt(value.v)
        }
        is Value.Text -> value.v
        is Value.BooleanValue -> if (value.v) "TRUE" else "FALSE"
        is Value.Error -> value.code
        is Value.Matrix -> value.list.firstOrNull()?.let(::render).orEmpty()
        Value.Empty -> ""
    }
    private fun number(v: Value): Double? = when (v) {
        is Value.Number -> v.v
        is Value.BooleanValue -> if (v.v) 1.0 else 0.0
        is Value.Text -> v.v.trim().toDoubleOrNull()
        Value.Empty -> 0.0
        else -> null
    }
    private fun literalNumber(v: Value): Double? = when (v) {
        is Value.Number -> v.v
        else -> null
    }
    private fun truth(v: Value): Boolean = when (v) {
        is Value.BooleanValue -> v.v
        is Value.Number -> v.v != 0.0
        is Value.Text -> v.v.isNotBlank() && !v.v.equals("FALSE", true)
        is Value.Error -> false
        else -> false
    }
    private fun flat(v: Value): List<Value> = if (v is Value.Matrix) v.list else listOf(v)
    private fun typedCell(raw: String): Value = when {
        raw.isBlank() -> Value.Empty
        raw.equals("TRUE", true) -> Value.BooleanValue(true)
        raw.equals("FALSE", true) -> Value.BooleanValue(false)
        raw.trim().matches(Regex("0[0-9]+")) && raw.trim().length > 1 -> Value.Text(raw)
        raw.trim().toDoubleOrNull() != null -> Value.Number(raw.trim().toDouble())
        raw.startsWith("#") && raw in listOf("#REF!", "#DIV/0!", "#VALUE!", "#N/A", "#NUM!", "#NAME?", "#CIRC!") -> Value.Error(raw)
        else -> Value.Text(raw)
    }
    private fun cellPosition(input: String): Pair<Int, Int>? {
        val s = input.uppercase(Locale.ROOT).replace("$", "")
        if (!s.matches(Regex("[A-Z]{1,3}[1-9][0-9]{0,6}"))) return null
        var col = 0
        s.takeWhile { it in 'A'..'Z' }.forEach { col = col * 26 + (it - 'A' + 1) }
        val row = s.dropWhile { it in 'A'..'Z' }.toIntOrNull() ?: return null
        if (row !in 1..100_000 || col !in 1..256) return null
        return row to col
    }
    private fun address(row: Int, col: Int): String {
        var c = col
        val letters = StringBuilder()
        while (c > 0) { c--; letters.append(('A'.code + c % 26).toChar()); c /= 26 }
        return letters.reverse().toString() + row
    }

    private class Parser(source: String) {
        private val tokens = ArrayList<Token>()
        private var index = 0
        init {
            var i = 0
            while (i < source.length) {
                val ch = source[i]
                when {
                    ch.isWhitespace() -> i++
                    ch == '"' -> {
                        i++
                        val b = StringBuilder()
                        var closed = false
                        while (i < source.length) {
                            if (source[i] == '"') {
                                i++
                                if (i < source.length && source[i] == '"') { b.append('"'); i++ }
                                else { closed = true; break }
                            } else b.append(source[i++])
                        }
                        if (!closed) throw Unsupported()
                        tokens += Token("TEXT", b.toString())
                    }
                    ch == '\'' -> {
                        i++
                        val b = StringBuilder()
                        var closed = false
                        while (i < source.length) {
                            if (source[i] == '\'') {
                                i++
                                if (i < source.length && source[i] == '\'') { b.append('\''); i++ }
                                else { closed = true; break }
                            } else b.append(source[i++])
                        }
                        if (!closed) throw Unsupported()
                        tokens += Token("SHEET", b.toString())
                    }
                    ch.isDigit() || ch == '.' && i + 1 < source.length && source[i + 1].isDigit() -> {
                        val start = i
                        while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                        if (i < source.length && (source[i] == 'e' || source[i] == 'E')) {
                            val old = i++
                            if (i < source.length && (source[i] == '+' || source[i] == '-')) i++
                            val digits = i
                            while (i < source.length && source[i].isDigit()) i++
                            if (digits == i) i = old
                        }
                        tokens += Token("NUM", source.substring(start, i))
                    }
                    ch.isLetter() || ch == '_' || ch == '$' -> {
                        val start = i
                        while (i < source.length && (source[i].isLetterOrDigit() || source[i] in "_.$")) i++
                        tokens += Token("WORD", source.substring(start, i))
                    }
                    else -> {
                        val pair = if (i + 1 < source.length) source.substring(i, i + 2) else ""
                        if (pair in listOf("<=", ">=", "<>")) { tokens += Token("SYMBOL", pair); i += 2 }
                        else { if (ch !in "()+-*/^&%=<>!:,;") throw Unsupported(); tokens += Token("SYMBOL", ch.toString()); i++ }
                    }
                }
                if (tokens.size > 2500) throw Unsupported()
            }
            tokens += Token("END", "")
        }
        private fun cur() = tokens[index]
        private fun accept(s: String): Boolean {
            if (cur().kind != "SYMBOL" || cur().text != s) return false
            index++
            return true
        }
        private fun require(s: String) { if (!accept(s)) throw Unsupported() }
        private fun precedence(s: String): Int = when (s) {
            "=", "<>", ">", "<", "<=", ">=" -> 1
            "&" -> 2
            "+", "-" -> 3
            "*", "/" -> 4
            "^" -> 6
            else -> -1
        }
        fun parse(): Node {
            val result = expression(0)
            if (cur().kind != "END") throw Unsupported()
            return result
        }
        private fun expression(minimum: Int): Node {
            var left = atom()
            while (true) {
                if (accept("%")) { left = Node.Unary("%", left); continue }
                val operator = cur().text
                val priority = precedence(operator)
                if (priority < minimum) break
                index++
                val right = expression(priority + if (operator == "^") 0 else 1)
                left = Node.Binary(operator, left, right)
            }
            return left
        }
        private fun atom(): Node {
            if (accept("+")) return Node.Unary("+", expression(5))
            if (accept("-")) return Node.Unary("-", expression(5))
            if (accept("(")) { val node = expression(0); require(")"); return node }
            val t = cur()
            if (t.kind == "NUM") { index++; return Node.Constant(Value.Number(t.text.toDoubleOrNull() ?: throw Unsupported())) }
            if (t.kind == "TEXT") { index++; return Node.Constant(Value.Text(t.text)) }
            if (t.kind == "WORD" || t.kind == "SHEET") {
                index++
                if (accept("!")) {
                    val address = cur()
                    if (address.kind != "WORD") throw Unsupported()
                    index++
                    return reference(t.text, address.text)
                }
                if (t.kind == "SHEET") throw Unsupported()
                if (accept("(")) {
                    val args = ArrayList<Node>()
                    if (!accept(")")) {
                        do { args += expression(0) } while (accept(",") || accept(";"))
                        require(")")
                    }
                    return Node.Call(t.text.uppercase(Locale.ROOT), args)
                }
                if (t.text.equals("TRUE", true)) return Node.Constant(Value.BooleanValue(true))
                if (t.text.equals("FALSE", true)) return Node.Constant(Value.BooleanValue(false))
                return reference(null, t.text)
            }
            throw Unsupported()
        }
        private fun reference(sheet: String?, first: String): Node {
            if (cellPosition(first) == null) throw Unsupported()
            if (!accept(":")) return Node.Reference(sheet, first)
            var otherSheet: String? = null
            var next = cur()
            if (next.kind == "SHEET" || next.kind == "WORD" && cellPosition(next.text) == null) {
                otherSheet = next.text
                index++
                require("!")
                next = cur()
            }
            if (next.kind != "WORD" || cellPosition(next.text) == null) throw Unsupported()
            index++
            if (otherSheet != null && !otherSheet.equals(sheet, ignoreCase = true)) throw Unsupported()
            return Node.Range(sheet, first, next.text)
        }
    }

    private class Evaluator(
        private val localLookup: (String) -> String,
        private val externalLookup: ((String, String) -> String?)?
    ) {
        var steps = 0
        private val visiting = HashSet<String>()
        private fun step(count: Int = 1) { steps += count; if (steps > MAX_STEPS) throw Unsupported() }
        fun eval(node: Node, sheet: String? = null, depth: Int = 0): Value {
            step()
            if (depth > MAX_NESTING) return Value.Error("#CIRC!")
            return when (node) {
                is Node.Constant -> node.v
                is Node.Reference -> readReference(node.sheet ?: sheet, node.address, depth + 1)
                is Node.Range -> readRange(node.sheet ?: sheet, node.first, node.last, depth + 1)
                is Node.Unary -> {
                    val v = eval(node.node, sheet, depth + 1)
                    if (v is Value.Error) v else {
                        val n = number(v) ?: return Value.Error("#VALUE!")
                        Value.Number(when (node.op) { "-" -> -n; "%" -> n / 100; else -> n })
                    }
                }
                is Node.Binary -> binary(node.op, eval(node.a, sheet, depth + 1), eval(node.b, sheet, depth + 1))
                is Node.Call -> call(node, sheet, depth + 1)
            }
        }
        private fun readReference(sheet: String?, address: String, depth: Int): Value {
            val position = cellPosition(address) ?: return Value.Error("#REF!")
            val ref = address(position.first, position.second)
            val key = "${sheet?.uppercase(Locale.ROOT) ?: "@CURRENT"}!$ref"
            if (!visiting.add(key)) return Value.Error("#CIRC!")
            try {
                val raw = if (sheet == null) localLookup(ref) else externalLookup?.invoke(sheet, ref)
                    ?: return Value.Error("#REF!")
                if (raw.startsWith("=")) return eval(Parser(raw.substring(1)).parse(), sheet, depth + 1)
                return typedCell(raw)
            } finally { visiting.remove(key) }
        }
        private fun readRange(sheet: String?, first: String, last: String, depth: Int): Value {
            val a = cellPosition(first) ?: return Value.Error("#REF!")
            val b = cellPosition(last) ?: return Value.Error("#REF!")
            val rows = abs(a.first - b.first) + 1
            val cols = abs(a.second - b.second) + 1
            if (rows.toLong() * cols > MAX_RANGE_CELLS) throw Unsupported()
            val result = ArrayList<Value>(rows * cols)
            for (row in minOf(a.first, b.first)..maxOf(a.first, b.first)) {
                for (col in minOf(a.second, b.second)..maxOf(a.second, b.second)) {
                    step()
                    result += readReference(sheet, address(row, col), depth + 1)
                }
            }
            return Value.Matrix(rows, cols, result)
        }
        private fun binary(operator: String, a: Value, b: Value): Value {
            if (a is Value.Error) return a
            if (b is Value.Error) return b
            if (operator == "&") return Value.Text(render(a) + render(b))
            if (operator in listOf("=", "<>", "<", ">", "<=", ">=")) {
                val cmp = compare(a, b)
                return Value.BooleanValue(when (operator) {
                    "=" -> cmp == 0; "<>" -> cmp != 0; "<" -> cmp < 0
                    ">" -> cmp > 0; "<=" -> cmp <= 0; else -> cmp >= 0
                })
            }
            val na = number(a) ?: return Value.Error("#VALUE!")
            val nb = number(b) ?: return Value.Error("#VALUE!")
            val v = when (operator) {
                "+" -> na + nb; "-" -> na - nb; "*" -> na * nb
                "/" -> if (nb == 0.0) return Value.Error("#DIV/0!") else na / nb
                "^" -> na.pow(nb)
                else -> throw Unsupported()
            }
            return if (v.isFinite()) Value.Number(v) else Value.Error("#NUM!")
        }
        private fun compare(a: Value, b: Value): Int {
            val na = number(a)
            val nb = number(b)
            if (na != null && nb != null && !(a is Value.Text && b is Value.Text && a.v.isNotBlank() && b.v.isNotBlank() && (a.v.toDoubleOrNull() == null || b.v.toDoubleOrNull() == null))) {
                return na.compareTo(nb)
            }
            return render(a).compareTo(render(b), ignoreCase = true)
        }
        private fun value(args: List<Node>, idx: Int, sheet: String?, depth: Int, default: Value = Value.Empty): Value =
            if (idx < args.size) eval(args[idx], sheet, depth) else default
        private fun list(args: List<Node>, sheet: String?, depth: Int) = args.map { eval(it, sheet, depth) }
        private fun err(values: List<Value>): Value.Error? = values.asSequence().flatMap { flat(it).asSequence() }
            .filterIsInstance<Value.Error>().firstOrNull()
        private fun numeric(values: List<Value>): List<Double> = values.flatMap(::flat).mapNotNull(::literalNumber)
        private fun numArg(args: List<Node>, i: Int, sheet: String?, depth: Int, default: Double? = null): Double? =
            if (i >= args.size) default else number(eval(args[i], sheet, depth))
        private fun txtArg(args: List<Node>, i: Int, sheet: String?, depth: Int, default: String = ""): String =
            if (i >= args.size) default else render(eval(args[i], sheet, depth))
        private fun validateArgs(args: List<Node>, min: Int, max: Int = min) { if (args.size !in min..max) throw Unsupported() }
        private fun matchCriteria(cell: Value, criterion: Value): Boolean {
            if (cell is Value.Error) return false
            val text = render(criterion)
            val op = listOf(">=", "<=", "<>", "=", ">", "<").firstOrNull { text.startsWith(it) } ?: "="
            val wanted = if (text.startsWith(op)) text.substring(op.length) else text
            val textOfCell = render(cell)
            if (op in listOf("=", "<>") && (wanted.contains('*') || wanted.contains('?'))) {
                val pattern = StringBuilder("^")
                var escaped = false
                wanted.forEach { c ->
                    when {
                        escaped -> { pattern.append(Regex.escape(c.toString())); escaped = false }
                        c == '~' -> escaped = true
                        c == '*' -> pattern.append(".*")
                        c == '?' -> pattern.append(".")
                        else -> pattern.append(Regex.escape(c.toString()))
                    }
                }
                pattern.append('$')
                val matched = Regex(pattern.toString(), RegexOption.IGNORE_CASE).matches(textOfCell)
                return if (op == "<>") !matched else matched
            }
            val candidate: Value = when {
                wanted.isBlank() -> Value.Empty
                wanted.toDoubleOrNull() != null -> Value.Number(wanted.toDouble())
                wanted.equals("TRUE", true) -> Value.BooleanValue(true)
                wanted.equals("FALSE", true) -> Value.BooleanValue(false)
                else -> Value.Text(wanted)
            }
            val c = compare(cell, candidate)
            return when (op) {
                "=" -> c == 0; "<>" -> c != 0; ">=" -> c >= 0; "<=" -> c <= 0
                ">" -> c > 0; "<" -> c < 0; else -> false
            }
        }
        private fun criterionRows(args: List<Node>, criteriaStart: Int, sheet: String?, depth: Int): List<Pair<Value.Matrix, Value>> {
            val criteria = ArrayList<Pair<Value.Matrix, Value>>()
            var i = criteriaStart
            while (i < args.size) {
                val range = eval(args[i], sheet, depth)
                if (range !is Value.Matrix) throw Unsupported()
                criteria += range to eval(args[i + 1], sheet, depth)
                i += 2
            }
            if (criteria.any { it.first.rows != criteria[0].first.rows || it.first.cols != criteria[0].first.cols }) throw Unsupported()
            return criteria
        }
        private fun serial(v: Value): Long? = when (v) {
            is Value.Number -> v.v.toLong()
            is Value.Text -> {
                runCatching { ChronoUnit.DAYS.between(excelEpoch, LocalDate.parse(v.v.trim())) }.getOrNull()
                    ?: runCatching { ChronoUnit.DAYS.between(excelEpoch,
                        LocalDate.parse(v.v.trim(), java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))) }.getOrNull()
                    ?: v.v.trim().toDoubleOrNull()?.toLong()
            }
            else -> number(v)?.toLong()
        }
        private fun localDate(v: Value): LocalDate? = serial(v)?.takeIf { it in -100000..3000000 }?.let { excelEpoch.plusDays(it) }
    private fun parseValueNumber(text: String): Double? {
        val src = text.trim().replace(" ", "")
        if (src.isEmpty()) return null
        src.toDoubleOrNull()?.let { return it }
        val comma = src.lastIndexOf(',')
        val dot = src.lastIndexOf('.')
        if (comma >= 0 && dot >= 0) {
            val decimal = if (comma > dot) ',' else '.'
            return src.replace(if (decimal == ',') "." else ",", "")
                .replace(decimal, '.').toDoubleOrNull()
        }
        val sep = if (comma >= 0) ',' else '.'
        val segments = src.removePrefix("-").removePrefix("+").split(sep)
        if (segments.size > 1 && segments.drop(1).all { it.length == 3 && it.all(Char::isDigit) }
            && segments[0].length in 1..3) return src.replace(sep.toString(), "").toDoubleOrNull()
        if (sep == ',') return src.replace(',', '.').toDoubleOrNull()
        return null
    }
        private fun sumValues(args: List<Node>, sheet: String?, depth: Int): List<Value> = list(args, sheet, depth)

        private fun call(n: Node.Call, sheet: String?, depth: Int): Value {
            val a = n.args
            val fn = n.name
            if (fn == "IF") {
                validateArgs(a, 2, 3)
                val condition = eval(a[0], sheet, depth)
                if (condition is Value.Error) return condition
                return if (truth(condition)) eval(a[1], sheet, depth) else value(a, 2, sheet, depth, Value.BooleanValue(false))
            }
            if (fn == "IFERROR") {
                validateArgs(a, 2)
                val first = eval(a[0], sheet, depth)
                return if (first is Value.Error) eval(a[1], sheet, depth) else first
            }
            if (fn == "IFNA") {
                validateArgs(a, 2)
                val first = eval(a[0], sheet, depth)
                return if (first is Value.Error && first.code == "#N/A") eval(a[1], sheet, depth) else first
            }
            if (fn == "AND" || fn == "OR") {
                if (a.isEmpty()) throw Unsupported()
                val vals = list(a, sheet, depth)
                err(vals)?.let { return it }
                return Value.BooleanValue(if (fn == "AND") vals.all(::truth) else vals.any(::truth))
            }
            if (fn == "NOT") { validateArgs(a,1); return Value.BooleanValue(!truth(eval(a[0],sheet,depth))) }
            when (fn) {
                "SUBTOTAL" -> {
                    if (a.size < 2) throw Unsupported()
                    val mode = numArg(a, 0, sheet, depth)?.toInt() ?: return Value.Error("#VALUE!")
                    if (mode !in listOf(1, 2, 3, 4, 5, 9)) throw Unsupported() // cannot detect hidden rows offline
                    val vals = list(a.drop(1), sheet, depth)
                    err(vals)?.let { return it }
                    val items = vals.flatMap(::flat)
                    val nums = numeric(vals)
                    return when (mode) {
                        1 -> if (nums.isEmpty()) Value.Error("#DIV/0!") else Value.Number(nums.average())
                        2 -> Value.Number(nums.size.toDouble())
                        3 -> Value.Number(items.count { it !is Value.Empty && (it !is Value.Text || it.v.isNotEmpty()) }.toDouble())
                        4 -> Value.Number(nums.maxOrNull() ?: 0.0)
                        5 -> Value.Number(nums.minOrNull() ?: 0.0)
                        else -> Value.Number(nums.sum())
                    }
                }
                "SUM", "AVERAGE", "COUNT", "COUNTA", "COUNTBLANK", "MIN", "MAX", "PRODUCT", "SUMPRODUCT" -> {
                    if (a.isEmpty()) throw Unsupported()
                    val all = sumValues(a, sheet, depth)
                    err(all)?.let { return it }
                    if (fn == "SUMPRODUCT") {
                        val arrays = all.map { flat(it) }
                        val nItems = arrays[0].size
                        if (arrays.any { it.size != nItems }) return Value.Error("#VALUE!")
                        return Value.Number((0 until nItems).sumOf { i -> arrays.fold(1.0) { acc, arr -> acc * (literalNumber(arr[i]) ?: 0.0) } })
                    }
                    val allFlat = all.flatMap(::flat)
                    val numbers = numeric(all)
                    return when (fn) {
                        "SUM" -> Value.Number(numbers.sum())
                        "AVERAGE" -> if (numbers.isEmpty()) Value.Error("#DIV/0!") else Value.Number(numbers.average())
                        "COUNT" -> Value.Number(numbers.size.toDouble())
                        "COUNTA" -> Value.Number(allFlat.count { it !is Value.Empty && (it !is Value.Text || it.v.isNotEmpty()) }.toDouble())
                        "COUNTBLANK" -> Value.Number(allFlat.count { it is Value.Empty || it is Value.Text && it.v.isEmpty() }.toDouble())
                        "MIN" -> Value.Number(numbers.minOrNull() ?: 0.0)
                        "MAX" -> Value.Number(numbers.maxOrNull() ?: 0.0)
                        else -> Value.Number(numbers.fold(1.0, Double::times))
                    }
                }
                "SUMIF", "COUNTIF", "AVERAGEIF", "SUMIFS", "COUNTIFS", "AVERAGEIFS" -> {
                    when (fn) {
                        "SUMIF", "AVERAGEIF" -> validateArgs(a, 2, 3)
                        "COUNTIF" -> validateArgs(a,2)
                        "SUMIFS", "AVERAGEIFS" -> if (a.size < 3 || a.size % 2 != 1) throw Unsupported()
                        "COUNTIFS" -> if (a.size < 2 || a.size % 2 != 0) throw Unsupported()
                    }
                    val output: Value.Matrix? = when (fn) {
                        "SUMIFS", "AVERAGEIFS" -> eval(a[0], sheet, depth) as? Value.Matrix ?: throw Unsupported()
                        "SUMIF", "AVERAGEIF" -> if (a.size == 3) eval(a[2], sheet, depth) as? Value.Matrix else null
                        else -> null
                    }
                    val conditions = when (fn) {
                        "SUMIF", "COUNTIF", "AVERAGEIF" -> {
                            val input = eval(a[0],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                            listOf(input to eval(a[1],sheet,depth))
                        }
                        "COUNTIFS" -> criterionRows(a,0,sheet,depth)
                        else -> criterionRows(a,1,sheet,depth)
                    }
                    val first = conditions[0].first
                    if (output != null && output.list.size != first.list.size) return Value.Error("#VALUE!")
                    val selected = first.list.indices.filter { i -> conditions.all { (range, criteria) -> matchCriteria(range.list[i],criteria) } }
                    val numbers = selected.mapNotNull { i -> literalNumber(output?.list?.get(i) ?: first.list[i]) }
                    return when (fn) {
                        "SUMIF", "SUMIFS" -> Value.Number(numbers.sum())
                        "AVERAGEIF", "AVERAGEIFS" -> if (numbers.isEmpty()) Value.Error("#DIV/0!") else Value.Number(numbers.average())
                        else -> Value.Number(selected.size.toDouble())
                    }
                }
                "ROUND", "ROUNDUP", "ROUNDDOWN", "TRUNC", "INT", "ABS", "MOD", "POWER", "SQRT" -> {
                    when (fn) {
                        "INT", "ABS", "SQRT" -> validateArgs(a,1)
                        "MOD", "POWER" -> validateArgs(a,2)
                        else -> validateArgs(a,1,2)
                    }
                    val v = numArg(a,0,sheet,depth) ?: return Value.Error("#VALUE!")
                    val digits = numArg(a,1,sheet,depth,0.0)?.toInt() ?: 0
                    val result = when (fn) {
                        "INT" -> floor(v)
                        "ABS" -> abs(v)
                        "SQRT" -> kotlin.math.sqrt(v)
                        "MOD" -> {
                            val mod = numArg(a,1,sheet,depth) ?: return Value.Error("#VALUE!")
                            if (mod == 0.0) return Value.Error("#DIV/0!")
                            v - mod * floor(v/mod)
                        }
                        "POWER" -> v.pow(numArg(a,1,sheet,depth) ?: return Value.Error("#VALUE!"))
                        "TRUNC" -> {
                            val scale = 10.0.pow(digits.toDouble())
                            truncate(v * scale) / scale
                        }
                        else -> {
                            if (digits !in -15..15) return Value.Error("#NUM!")
                            val mode = when (fn) { "ROUNDUP" -> RoundingMode.UP; "ROUNDDOWN" -> RoundingMode.DOWN; else -> RoundingMode.HALF_UP }
                            BigDecimal.valueOf(v).setScale(digits, mode).toDouble()
                        }
                    }
                    return if (result.isFinite()) Value.Number(result) else Value.Error("#NUM!")
                }
                "VLOOKUP", "HLOOKUP" -> {
                    validateArgs(a,3,4)
                    val wanted = value(a,0,sheet,depth)
                    val table = eval(a[1],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                    val position = numArg(a,2,sheet,depth)?.toInt() ?: return Value.Error("#VALUE!")
                    val exact = if (a.size == 4) !truth(eval(a[3],sheet,depth)) else false
                    if (!exact) throw Unsupported() // Approximate matching deliberately delegated to Excel.
                    if (fn == "VLOOKUP") {
                        if (position !in 1..table.cols) return Value.Error("#REF!")
                        for (r in 0 until table.rows) if (compare(table.list[r*table.cols], wanted) == 0)
                            return table.list[r*table.cols + position - 1]
                    } else {
                        if (position !in 1..table.rows) return Value.Error("#REF!")
                        for (col in 0 until table.cols) if (compare(table.list[col], wanted) == 0)
                            return table.list[(position - 1)*table.cols + col]
                    }
                    return Value.Error("#N/A")
                }
                "XLOOKUP" -> {
                    validateArgs(a,3,4)
                    val wanted = value(a,0,sheet,depth)
                    val from = eval(a[1],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                    val to = eval(a[2],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                    if (from.list.size != to.list.size) return Value.Error("#VALUE!")
                    for (i in from.list.indices) if (compare(from.list[i],wanted) == 0) return to.list[i]
                    return if (a.size == 4) eval(a[3],sheet,depth) else Value.Error("#N/A")
                }
                "MATCH" -> {
                    validateArgs(a,2,3)
                    val wanted = value(a,0,sheet,depth)
                    val range = eval(a[1],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                    val mode = numArg(a,2,sheet,depth,1.0) ?: 1.0
                    if (mode != 0.0) throw Unsupported()
                    val i = range.list.indexOfFirst { compare(it,wanted) == 0 }
                    return if (i == -1) Value.Error("#N/A") else Value.Number((i + 1).toDouble())
                }
                "INDEX" -> {
                    validateArgs(a,2,3)
                    val range = eval(a[0],sheet,depth) as? Value.Matrix ?: throw Unsupported()
                    val row = numArg(a,1,sheet,depth)?.toInt() ?: return Value.Error("#VALUE!")
                    val col = numArg(a,2,sheet,depth,1.0)?.toInt() ?: return Value.Error("#VALUE!")
                    return if (row !in 1..range.rows || col !in 1..range.cols) Value.Error("#REF!")
                    else range.list[(row - 1)*range.cols+col-1]
                }
                "LEFT", "RIGHT", "MID", "LEN", "TRIM", "UPPER", "LOWER", "CONCAT", "CONCATENATE", "VALUE", "TEXT" -> {
                    when (fn) {
                        "MID" -> validateArgs(a,3)
                        "LEFT", "RIGHT" -> validateArgs(a,1,2)
                        "TEXT" -> validateArgs(a,2)
                        "LEN", "TRIM", "UPPER", "LOWER", "VALUE" -> validateArgs(a,1)
                        else -> if (a.isEmpty()) throw Unsupported()
                    }
                    val str = txtArg(a,0,sheet,depth)
                    return when (fn) {
                        "LEFT", "RIGHT" -> {
                            val nChars = numArg(a,1,sheet,depth,1.0)?.toInt() ?: 1
                            if (nChars < 0) Value.Error("#VALUE!") else Value.Text(if (fn == "LEFT") str.take(nChars) else str.takeLast(nChars))
                        }
                        "MID" -> {
                            val start = numArg(a,1,sheet,depth)?.toInt() ?: 0
                            val count = numArg(a,2,sheet,depth)?.toInt() ?: 0
                            if (start < 1 || count < 0) Value.Error("#VALUE!") else Value.Text(str.drop(start-1).take(count))
                        }
                        "LEN" -> Value.Number(str.length.toDouble())
                        "TRIM" -> Value.Text(str.trim().replace(Regex("\\s+"), " "))
                        "UPPER" -> Value.Text(str.uppercase(Locale.ROOT))
                        "LOWER" -> Value.Text(str.lowercase(Locale.ROOT))
                        "CONCAT", "CONCATENATE" -> Value.Text(list(a,sheet,depth).flatMap(::flat).joinToString("") { render(it) })
                        "VALUE" -> parseValueNumber(str)?.let { Value.Number(it) } ?: Value.Error("#VALUE!")
                        else -> {
                            val pattern = txtArg(a,1,sheet,depth)
                            val n = number(eval(a[0],sheet,depth)) ?: return Value.Error("#VALUE!")
                            when (pattern) {
                                "0%" -> Value.Text(fmt(n*100)+"%")
                                "0.00%" -> Value.Text("%.2f%%".format(Locale.US,n*100))
                                "#,##0", "#,##0.00", "0.00", "0" -> {
                                    val symbols = DecimalFormatSymbols(Locale.US)
                                    Value.Text(DecimalFormat(pattern,symbols).format(n))
                                }
                                else -> throw Unsupported()
                            }
                        }
                    }
                }
                "TODAY", "NOW", "DATE", "YEAR", "MONTH", "DAY", "EOMONTH", "DAYS", "NETWORKDAYS" -> {
                    when (fn) {
                        "TODAY", "NOW" -> validateArgs(a,0)
                        "DATE" -> validateArgs(a,3)
                        "YEAR", "MONTH", "DAY" -> validateArgs(a,1)
                        "EOMONTH", "DAYS" -> validateArgs(a,2)
                        else -> validateArgs(a,2,3)
                    }
                    return when (fn) {
                        "TODAY" -> Value.Number(ChronoUnit.DAYS.between(excelEpoch, LocalDate.now()).toDouble(), true)
                        "NOW" -> {
                            val now = LocalDateTime.now()
                            Value.Number(ChronoUnit.DAYS.between(excelEpoch, now.toLocalDate()) +
                                now.toLocalTime().toSecondOfDay() / 86400.0, dateTime = true)
                        }
                        "DATE" -> {
                            val y = numArg(a,0,sheet,depth)?.toInt() ?: return Value.Error("#VALUE!")
                            val m = numArg(a,1,sheet,depth)?.toLong() ?: return Value.Error("#VALUE!")
                            val d = numArg(a,2,sheet,depth)?.toLong() ?: return Value.Error("#VALUE!")
                            val date = runCatching { LocalDate.of(if (y in 0..1899) y + 1900 else y,1,1).plusMonths(m-1).plusDays(d-1) }.getOrNull()
                                ?: return Value.Error("#NUM!")
                            Value.Number(ChronoUnit.DAYS.between(excelEpoch,date).toDouble(), true)
                        }
                        "YEAR", "MONTH", "DAY" -> {
                            val dt = localDate(eval(a[0],sheet,depth)) ?: return Value.Error("#VALUE!")
                            Value.Number(when (fn) { "YEAR" -> dt.year.toDouble(); "MONTH" -> dt.monthValue.toDouble(); else -> dt.dayOfMonth.toDouble() })
                        }
                        "EOMONTH" -> {
                            val dt = localDate(eval(a[0],sheet,depth)) ?: return Value.Error("#VALUE!")
                            val months = numArg(a,1,sheet,depth)?.toLong() ?: return Value.Error("#VALUE!")
                            val next = dt.plusMonths(months).withDayOfMonth(1).plusMonths(1).minusDays(1)
                            Value.Number(ChronoUnit.DAYS.between(excelEpoch,next).toDouble(), true)
                        }
                        "DAYS" -> {
                            val end = serial(eval(a[0],sheet,depth)) ?: return Value.Error("#VALUE!")
                            val start = serial(eval(a[1],sheet,depth)) ?: return Value.Error("#VALUE!")
                            Value.Number((end-start).toDouble())
                        }
                        else -> {
                            val start = localDate(eval(a[0],sheet,depth)) ?: return Value.Error("#VALUE!")
                            val end = localDate(eval(a[1],sheet,depth)) ?: return Value.Error("#VALUE!")
                            if (abs(ChronoUnit.DAYS.between(start,end)) > 36600) throw Unsupported()
                            val holidays = if (a.size == 3) flat(eval(a[2],sheet,depth)).mapNotNull(::localDate).toHashSet() else emptySet()
                            val sign = if (start.isAfter(end)) -1 else 1
                            var dt = if (sign > 0) start else end
                            val finish = if (sign > 0) end else start
                            var count = 0
                            while (!dt.isAfter(finish)) {
                                if (dt.dayOfWeek.value <= 5 && dt !in holidays) count++
                                dt = dt.plusDays(1)
                            }
                            Value.Number((count*sign).toDouble())
                        }
                    }
                }
                else -> throw Unsupported()
            }
        }
    }

    /** Returns a formatted local preview or null if expression is unsupported / too costly. */
    fun preview(expression: String, lookup: (String) -> String,
                crossSheet: ((String, String) -> String?)? = null): String? {
        val normalized = expression.trim()
        if (!normalized.startsWith("=") || normalized.length > 2500) return null
        return try {
            val ast = Parser(normalized.substring(1)).parse()
            render(Evaluator(lookup, crossSheet).eval(ast))
        } catch (_: Unsupported) { null }
        catch (_: IllegalArgumentException) { null }
        catch (_: ArithmeticException) { "#NUM!" }
        catch (_: java.time.DateTimeException) { "#NUM!" }
        catch (_: StackOverflowError) { null }
    }
}
