package com.docflowoffline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
import kotlin.math.roundToInt
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

// ================ ExcelEditor v0.5 ================
/** DocFlow v0.5: a compact mobile spreadsheet ribbon, not the proprietary Excel engine. */
private val ExcelGreen = Color(0xFF167346)
private val ExcelPale = Color(0xFFE7F4ED)
private enum class ExcelTab(val label: String) {
    HOME("Trang chủ"), INSERT("Chèn"), FORMULAS("Công thức"),
    DATA("Dữ liệu"), REVIEW("Đánh giá"), VIEW("Xem")
}
private data class ExcelUndo(
    val values: Map<String, String>, val styles: Map<String, OfficeOoxml.StyleChange>
)
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
 * Basic local preview for SUM/AVERAGE/COUNT/MIN/MAX over a single rectangular range.
 * Unsupported formulas are kept unchanged and delegated to Excel/LibreOffice on the next open.
 */
internal fun previewFormula(expression: String, lookup: (String) -> String): String? {
    val match = Regex("^=(SUM|AVERAGE|COUNT|MIN|MAX)\\(([A-Z]{1,3}[1-9]\\d{0,5}):([A-Z]{1,3}[1-9]\\d{0,5})\\)$", RegexOption.IGNORE_CASE)
        .matchEntire(expression.trim()) ?: return null
    val fn = match.groupValues[1].uppercase(Locale.ROOT)
    val start = match.groupValues[2].uppercase(Locale.ROOT)
    val end = match.groupValues[3].uppercase(Locale.ROOT)
    val r1 = start.dropWhile(Char::isLetter).toIntOrNull() ?: return null
    val r2 = end.dropWhile(Char::isLetter).toIntOrNull() ?: return null
    val c1 = colFromAddress(start)
    val c2 = colFromAddress(end)
    if (r1 > r2 || c1 > c2 || r2.toLong().minus(r1).plus(1) * (c2 - c1 + 1) > 30000) return null
    val numbers = mutableListOf<Double>()
    for (r in r1..r2) for (c in c1..c2) {
        val ref = OfficeOoxml.cellAddress(r, c)
        val value = lookup(ref).trim().toDoubleOrNull()
        if (value != null) numbers.add(value)
    }
    val result = when (fn) {
        "SUM" -> numbers.sum()
        "COUNT" -> numbers.size.toDouble()
        "AVERAGE" -> if (numbers.isEmpty()) return "#DIV/0!" else numbers.average()
        "MIN" -> numbers.minOrNull() ?: 0.0
        "MAX" -> numbers.maxOrNull() ?: 0.0
        else -> return null
    }
    return formatExcelNumber(result)
}

@Composable
internal fun ExcelEditor(
    document: DocumentStore.OpenFile.Excel,
    save: Save,
    notify: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var sheetIndex by remember(document.uri) { mutableIntStateOf(0) }
    val sheet = document.data.sheets.getOrNull(sheetIndex) ?: return
    var activeTab by remember(document.uri) { mutableStateOf(ExcelTab.HOME) }
    var ribbonVisible by remember(document.uri) { mutableStateOf(true) }
    var zoom by remember(document.uri) { mutableFloatStateOf(1f) }
    var previousZoom by remember(document.uri) { mutableFloatStateOf(1f) }
    var selected by remember(document.uri, sheet.partPath) { mutableStateOf("A1") }
    var addressInput by remember(document.uri, sheet.partPath) { mutableStateOf("A1") }
    val values = remember(document.uri) { mutableStateMapOf<String, String>() }
    val styleChanges = remember(document.uri) { mutableStateMapOf<String, OfficeOoxml.StyleChange>() }
    val undo = remember(document.uri) { mutableStateListOf<ExcelUndo>() }
    val redo = remember(document.uri) { mutableStateListOf<ExcelUndo>() }
    var clipboard by remember(document.uri) { mutableStateOf<String?>(null) }
    var searchVisible by remember(document.uri) { mutableStateOf(false) }
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
    val xScroll = rememberScrollState()
    val yScroll = rememberLazyListState()
    val maxRows = maxOf(100, sheet.maxRow + 25).coerceAtMost(100_000)
    val maxCols = maxOf(12, sheet.maxCol + 5).coerceAtMost(256)
    val selectedKey = "${sheet.partPath}::$selected"

    fun rawValue(ref: String): String = values["${sheet.partPath}::$ref"] ?: sheet.cells[ref].orEmpty()
    fun activeStyle(ref: String): OfficeOoxml.CellAppearance {
        val base = sheet.appearances[ref] ?: OfficeOoxml.CellAppearance()
        val change = styleChanges["${sheet.partPath}::$ref"] ?: return base
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
            percent = change.numFmtId?.let { it == 9 || it == 10 } ?: base.percent
        )
    }
    fun cellDisplay(ref: String): String {
        val raw = rawValue(ref)
        val shown = if (raw.startsWith("=")) {
            previewFormula(raw, ::rawValue) ?: if (values.containsKey("${sheet.partPath}::$ref")) raw
                else sheet.displayCells[ref] ?: raw
        } else raw
        if (activeStyle(ref).percent) {
            return shown.toDoubleOrNull()?.let { formatExcelNumber(it * 100.0) + "%" } ?: shown
        }
        return shown
    }
    var formula by remember(document.uri, selectedKey) { mutableStateOf(rawValue(selected)) }
    LaunchedEffect(selectedKey, values[selectedKey]) {
        formula = rawValue(selected)
        addressInput = selected
    }

    fun checkpoint() {
        undo.add(ExcelUndo(values.toMap(), styleChanges.toMap()))
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
    fun undoAction() {
        if (undo.isEmpty()) return
        redo.add(ExcelUndo(values.toMap(), styleChanges.toMap()))
        val state = undo.removeAt(undo.lastIndex)
        values.clear(); values.putAll(state.values)
        styleChanges.clear(); styleChanges.putAll(state.styles)
        formula = rawValue(selected)
    }
    fun redoAction() {
        if (redo.isEmpty()) return
        undo.add(ExcelUndo(values.toMap(), styleChanges.toMap()))
        val state = redo.removeAt(redo.lastIndex)
        values.clear(); values.putAll(state.values)
        styleChanges.clear(); styleChanges.putAll(state.styles)
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
            "sum", "average", "count", "min", "max" -> {
                val row = selected.dropWhile(Char::isLetter).toIntOrNull() ?: 1
                if (row < 2) notify("Hãy chọn ô ở hàng 2 trở xuống để tạo công thức từ các hàng phía trên.")
                else {
                    val col = selected.takeWhile(Char::isLetter)
                    setValue("=${id.uppercase(Locale.ROOT)}(${col}1:${col}${row - 1})")
                }
            }
            "find" -> { searchVisible = !searchVisible; if (!searchVisible) results = emptyList() }
            "filter" -> { filterVisible = !filterVisible; if (!filterVisible) filterText = ""; filterColumn = colFromAddress(selected) }
            "sortAsc" -> { sortOrder = if (sortOrder == 1) 0 else 1; filterColumn = colFromAddress(selected) }
            "sortDesc" -> { sortOrder = if (sortOrder == -1) 0 else -1; filterColumn = colFromAddress(selected) }
            "resetDataView" -> { sortOrder = 0; filterText = ""; filterVisible = false }
            "formulaBar" -> showFormulaBar = !showFormulaBar
            "gridlines" -> showGridLines = !showGridLines
            "ribbon" -> ribbonVisible = !ribbonVisible
            "zoomIn" -> zoom = (zoom + .15f).coerceAtMost(2.5f)
            "zoomOut" -> zoom = (zoom - .15f).coerceAtLeast(.65f)
            "zoom100" -> zoom = 1f
            "info" -> infoVisible = !infoVisible
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
            val sorted = if (sortOrder == 0) matches else matches.sortedWith(Comparator { a, b ->
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
    val hasChanges = values.isNotEmpty() || styleChanges.isNotEmpty()
    val selectedAppearance = activeStyle(selected)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).imePadding()) {
        Row(Modifier.fillMaxWidth().height(51.dp).background(ExcelGreen).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Default.ArrowBack, "Quay lại", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(document.name, color = Color.White, fontWeight = FontWeight.Bold,
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
            IconButton(onClick = {
                val changes = values.map { (key, value) ->
                    val separator = key.lastIndexOf("::")
                    val path = key.substring(0, separator)
                    val cell = key.substring(separator + 2)
                    val source = document.data.sheets.firstOrNull { it.partPath == path }
                    val preview = if (value.startsWith("=")) previewFormula(value) { ref ->
                        values["$path::$ref"] ?: source?.cells?.get(ref).orEmpty()
                    }?.toDoubleOrNull()?.toString() else null
                    OfficeOoxml.CellChange(path, cell, value, preview)
                }
                val formatting = styleChanges.values.toList()
                save(DocumentStore.suggestedCopyName(document.name, "xlsx"), DocumentStore.XLSX, true) { _, output ->
                    output.write(OfficeOoxml.writeWorkbook(document.bytes, changes, formatting))
                }
            }, modifier = Modifier.size(41.dp)) {
                Icon(Icons.Default.Save, "Lưu bản sao Excel", tint = Color.White)
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
                        ExcelRibbonItem("AutoSum", Icons.Default.Functions, "sum"),
                        ExcelRibbonItem("AVERAGE", Icons.Default.Calculate, "average"),
                        ExcelRibbonItem("COUNT", Icons.Default.Numbers, "count"),
                        ExcelRibbonItem("MIN", Icons.Default.ArrowDownward, "min"),
                        ExcelRibbonItem("MAX", Icons.Default.ArrowUpward, "max")
                    )
                    ExcelTab.DATA -> listOf(
                        ExcelRibbonItem("Lọc", Icons.Default.FilterAlt, "filter", filterVisible),
                        ExcelRibbonItem("A→Z", Icons.Default.SortByAlpha, "sortAsc", sortOrder == 1),
                        ExcelRibbonItem("Z→A", Icons.Default.SwapVert, "sortDesc", sortOrder == -1),
                        ExcelRibbonItem("Bỏ lọc", Icons.Default.FilterAltOff, "resetDataView")
                    )
                    ExcelTab.REVIEW -> listOf(
                        ExcelRibbonItem("Tìm kiếm", Icons.Default.Search, "find", searchVisible),
                        ExcelRibbonItem("Hoàn tác", Icons.Default.Undo, "undo"),
                        ExcelRibbonItem("Làm lại", Icons.Default.Redo, "redo"),
                        ExcelRibbonItem("Chi tiết", Icons.Default.Info, "info")
                    )
                    ExcelTab.VIEW -> listOf(
                        ExcelRibbonItem("Thu nhỏ", Icons.Default.ZoomOut, "zoomOut"),
                        ExcelRibbonItem("Phóng to", Icons.Default.ZoomIn, "zoomIn"),
                        ExcelRibbonItem("100%", Icons.Default.CenterFocusStrong, "zoom100"),
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
                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(5.dp))
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
            cellValue = ::cellDisplay, cellAppearance = ::activeStyle,
            rowNumbers = rowMap, showGridLines = showGridLines,
            onCellClick = { selected = it }, horizontal = xScroll, vertical = yScroll,
            zoom = zoom, onPinchZoom = { zoom = (zoom * it).coerceIn(.65f, 2.5f) },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
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
        Row(Modifier.fillMaxWidth().height(26.dp).padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (hasChanges) "${values.size + styleChanges.size} thay đổi chưa lưu" else "Sẵn sàng",
                fontSize = 10.sp, color = if (hasChanges) ExcelGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f))
            if (rowMap != null) Text("Đang lọc/sắp xếp hiển thị · ", fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(zoom * 100).roundToInt()}%", fontSize = 11.sp, color = ExcelGreen,
                modifier = Modifier.clickable { activeTab = ExcelTab.VIEW; ribbonVisible = true })
        }
    }
    if (infoVisible) AlertDialog(onDismissRequest = { infoVisible = false },
        title = { Text("Giới hạn trình Excel offline") },
        text = { Text("Lưu thành bản sao. Các nút định dạng cơ bản được ghi vào XLSX; công thức SUM/AVERAGE/COUNT/MIN/MAX tính thử trên dữ liệu số. Sắp xếp và lọc chỉ thay đổi cách xem, không ghi lại thứ tự hàng. PivotTable, macro, biểu đồ và định dạng phức tạp cần bộ máy LibreOffice native.") },
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
    rowNumbers: List<Int>? = null,
    showGridLines: Boolean = true,
    onCellClick: (String) -> Unit,
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
    val scale = zoom.coerceIn(0.65f, 2.5f)
    val columnWidth = GRID_COLUMN_DP.dp * scale
    val rowHeight = GRID_ROW_DP.dp * scale
    val indexWidth = GRID_INDEX_DP.dp * scale
    val columnWidthPx = with(density) { columnWidth.toPx() }
    val rowHeightPx = with(density) { rowHeight.toPx() }
    val contentWidth = (columns * GRID_COLUMN_DP).dp * scale
    val currentClick = rememberUpdatedState(onCellClick)
    val currentPinch = rememberUpdatedState(onPinchZoom)
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
                                    .pointerInput(rowNumber, columns, columnWidthPx) {
                                        detectTapGestures { point ->
                                            val c = (floor(point.x / columnWidthPx).toInt() + 1).coerceIn(1, columns)
                                            currentClick.value(OfficeOoxml.cellAddress(rowNumber, c))
                                        }
                                    }
                            ) {
                                val start = (horizontal.value / columnWidthPx).toInt().coerceIn(0, columns - 1)
                                // draw only visible columns even though the canvas spans all columns
                                val end = min(columns - 1, start + ceil(viewportWidthPx / columnWidthPx).toInt() + 2)
                                val canvas = drawContext.canvas.nativeCanvas
                                for (column in start..end) {
                                    val x = column * columnWidthPx
                                    val address = OfficeOoxml.cellAddress(rowNumber, column + 1)
                                    val isSelected = address == selected
                                    val style = cellAppearance(address)
                                    if (!style.fillRgb.isNullOrBlank() && style.fillRgb.matches(Regex("[0-9A-Fa-f]{6}"))) {
                                        drawRect(androidx.compose.ui.graphics.Color(
                                            android.graphics.Color.parseColor("#${style.fillRgb}")),
                                            topLeft = Offset(x, 0f),
                                            size = androidx.compose.ui.geometry.Size(columnWidthPx, size.height))
                                    }
                                    if (isSelected) {
                                        drawRect(selectedBg.copy(alpha = 0.55f), topLeft = Offset(x, 0f),
                                            size = androidx.compose.ui.geometry.Size(columnWidthPx, size.height))
                                    }
                                    val value = cellValue(address)
                                    if (value.isNotEmpty()) {
                                        val paint = if (isSelected) highlightedPaint else regularPaint
                                        paint.isFakeBoldText = isSelected || style.bold
                                        paint.isUnderlineText = style.underline
                                        paint.textSkewX = if (style.italic) -0.18f else 0f
                                        val display = value.replace('\n', ' ')
                                        val textWidth = paint.measureText(display)
                                        val margin = 8.dp.toPx()
                                        val textX = when (style.alignment) {
                                            "right" -> x + columnWidthPx - textWidth - margin
                                            "center" -> x + (columnWidthPx - textWidth) / 2f
                                            else -> x + margin
                                        }
                                        canvas.save()
                                        canvas.clipRect(x + margin / 2, 0f, x + columnWidthPx - margin / 2, size.height)
                                        canvas.drawText(display, textX, rowHeightPx / 2 + labelSize / 3, paint)
                                        canvas.restore()
                                    }
                                    if (showGridLines) drawLine(outline, Offset(x + columnWidthPx, 0f),
                                        Offset(x + columnWidthPx, size.height), 0.65.dp.toPx())
                                    if (isSelected) {
                                        val blue = androidx.compose.ui.graphics.Color(0xFF167346)
                                        drawLine(blue, Offset(x, 1f), Offset(x + columnWidthPx, 1f), 2.dp.toPx())
                                        drawLine(blue, Offset(x, size.height - 1f), Offset(x + columnWidthPx, size.height - 1f), 2.dp.toPx())
                                        drawLine(blue, Offset(x + 1f, 0f), Offset(x + 1f, size.height), 2.dp.toPx())
                                        drawLine(blue, Offset(x + columnWidthPx - 1f, 0f), Offset(x + columnWidthPx - 1f, size.height), 2.dp.toPx())
                                    }
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
