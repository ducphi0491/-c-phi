package com.docflowoffline

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

/** Job is executed only after the user approves destination from Android SAF picker. */
internal data class SaveJob(
    val filename: String,
    val mime: String,
    val openAfter: Boolean = true,
    val writer: (Context, OutputStream) -> Unit
)

enum class HomeTab { HOME, CONVERT, TOOLS, ABOUT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocFlowApp(incomingUri: Uri?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var document by remember { mutableStateOf<DocumentStore.OpenFile?>(null) }
    var busy by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(HomeTab.HOME) }
    var recent by remember { mutableStateOf(DocumentStore.recent(context)) }
    var pendingJob by remember { mutableStateOf<SaveJob?>(null) }
    var showCreateMenu by remember { mutableStateOf(false) }

    fun notify(text: String) { scope.launch { snack.showSnackbar(text) } }

    fun openUri(uri: Uri) {
        scope.launch {
            busy = true
            try {
                val opened = withContext(Dispatchers.IO) { DocumentStore.open(context, uri) }
                document = opened
                DocumentStore.persistReadPermission(context, uri)
                DocumentStore.remember(context, uri, opened.name)
                recent = DocumentStore.recent(context)
            } catch (e: Exception) {
                notify("Không mở được tài liệu: ${e.message ?: e.javaClass.simpleName}")
            } finally { busy = false }
        }
    }

    LaunchedEffect(incomingUri) { if (incomingUri != null) openUri(incomingUri) }

    val openPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri)
    }

    val savePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val job = pendingJob
        pendingJob = null
        val outputUri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && outputUri != null && job != null) {
            scope.launch {
                busy = true
                try {
                    withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(outputUri, "wt")
                            ?: error("Không tạo được tệp đích.")
                        stream.use { job.writer(context, it) }
                    }
                    notify("Đã lưu ${job.filename}")
                    if (job.openAfter && job.mime in listOf(DocumentStore.DOCX, DocumentStore.XLSX, DocumentStore.PPTX, DocumentStore.PDF, DocumentStore.TXT, DocumentStore.CSV)) {
                        openUri(outputUri)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.IO) {
                        // Avoid leaving a zero-byte or partial output after a failed conversion.
                        runCatching { DocumentsContract.deleteDocument(context.contentResolver, outputUri) }
                    }
                    notify("Lưu thất bại: ${e.message ?: e.javaClass.simpleName}")
                } finally { busy = false }
            }
        }
    }

    fun requestSave(filename: String, mime: String, openAfter: Boolean = true, writer: (Context, OutputStream) -> Unit) {
        pendingJob = SaveJob(filename, mime, openAfter, writer)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        savePicker.launch(intent)
    }

    val mergePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.size < 2) notify("Hãy chọn ít nhất 2 tệp PDF.")
        else requestSave("PDF_da_ghep.pdf", DocumentStore.PDF) { ctx, out -> PdfTools.merge(ctx, uris, out) }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        requestSave("Hinh_anh_thanh_PDF.pdf", DocumentStore.PDF) { ctx, out -> PdfTools.imagesToPdf(ctx, uris, out) }
    }

    fun createNew(asset: String, name: String, mime: String) {
        requestSave(name, mime) { ctx, out -> ctx.assets.open(asset).use { it.copyTo(out) } }
    }

    BackHandler(enabled = document != null) { document = null }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(document?.name ?: "DocFlow", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (document == null) "Văn phòng offline" else "Chỉnh sửa an toàn · lưu bản sao",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (document != null) IconButton(onClick = { document = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại")
                    } else {
                        Icon(Icons.Default.Description, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp).size(28.dp))
                    }
                },
                actions = {
                    if (document == null) {
                        Surface(shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 12.dp)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.CloudOff, null, Modifier.size(17.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Offline", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (document == null) NavigationBar {
                listOf(
                    Triple(HomeTab.HOME, "Tài liệu", Icons.Default.Folder),
                    Triple(HomeTab.CONVERT, "Chuyển đổi", Icons.Default.SwapHoriz),
                    Triple(HomeTab.TOOLS, "Công cụ", Icons.Default.Build),
                    Triple(HomeTab.ABOUT, "Thông tin", Icons.Default.Info)
                ).forEach { (tab, name, icon) ->
                    NavigationBarItem(selected = activeTab == tab, onClick = { activeTab = tab },
                        icon = { Icon(icon, contentDescription = name) }, label = { Text(name) })
                }
            }
        },
        floatingActionButton = {
            if (document == null && activeTab == HomeTab.HOME) {
                Box {
                    ExtendedFloatingActionButton(onClick = { showCreateMenu = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Tạo mới") })
                    DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                        DropdownMenuItem(text = { Text("Tài liệu Word") },
                            leadingIcon = { Icon(Icons.Default.Description, null) }, onClick = {
                                showCreateMenu = false
                                createNew("blank.docx", "Tai_lieu_moi.docx", DocumentStore.DOCX)
                            })
                        DropdownMenuItem(text = { Text("Bảng tính Excel") },
                            leadingIcon = { Icon(Icons.Default.TableChart, null) }, onClick = {
                                showCreateMenu = false
                                createNew("blank.xlsx", "Bang_tinh_moi.xlsx", DocumentStore.XLSX)
                            })
                        DropdownMenuItem(text = { Text("Slide PowerPoint") },
                            leadingIcon = { Icon(Icons.Default.Slideshow, null) }, onClick = {
                                showCreateMenu = false
                                createNew("blank.pptx", "Trinh_chieu_moi.pptx", DocumentStore.PPTX)
                            })
                        DropdownMenuItem(text = { Text("Ghi chú TXT") },
                            leadingIcon = { Icon(Icons.Default.EditNote, null) }, onClick = {
                                showCreateMenu = false
                                requestSave("Ghi_chu_moi.txt", DocumentStore.TXT) { _, out -> out.write(ByteArray(0)) }
                            })
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val doc = document
            if (doc != null) {
                DocumentScreen(
                    document = doc,
                    requestSave = { file, mime, openAfter, write -> requestSave(file, mime, openAfter, write) },
                    notify = ::notify
                )
            } else when (activeTab) {
                HomeTab.HOME -> HomeContent(
                    recent = recent,
                    open = { openPicker.launch(arrayOf("*/*")) },
                    newWord = { createNew("blank.docx", "Tai_lieu_moi.docx", DocumentStore.DOCX) },
                    newExcel = { createNew("blank.xlsx", "Bang_tinh_moi.xlsx", DocumentStore.XLSX) },
                    newSlides = { createNew("blank.pptx", "Trinh_chieu_moi.pptx", DocumentStore.PPTX) },
                    newText = { requestSave("Ghi_chu_moi.txt", DocumentStore.TXT) { _, out -> out.write(ByteArray(0)) } },
                    openRecent = { openUri(Uri.parse(it.uri)) }
                )
                HomeTab.CONVERT -> ConvertContent(
                    openDocument = { openPicker.launch(arrayOf(DocumentStore.DOCX, DocumentStore.XLSX)) },
                    imagesPdf = { imagePicker.launch(arrayOf("image/*")) },
                    openPdf = { openPicker.launch(arrayOf(DocumentStore.PDF)) }
                )
                HomeTab.TOOLS -> ToolsContent(
                    merge = { mergePicker.launch(arrayOf(DocumentStore.PDF)) },
                    imagesPdf = { imagePicker.launch(arrayOf("image/*")) },
                    openPdf = { openPicker.launch(arrayOf(DocumentStore.PDF)) }
                )
                HomeTab.ABOUT -> AboutContent()
            }
            if (busy) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

private enum class RecentFilter(val label: String) {
    ALL("Tất cả"), PDF("PDF"), WORD("Word"), EXCEL("Excel"), SLIDES("Slide"), OTHER("Khác")
}

private fun groupFor(name: String): RecentFilter = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> RecentFilter.PDF
    "docx", "doc" -> RecentFilter.WORD
    "xlsx", "xls", "csv", "tsv" -> RecentFilter.EXCEL
    "pptx", "ppt" -> RecentFilter.SLIDES
    else -> RecentFilter.OTHER
}

@Composable
private fun HomeContent(
    recent: List<DocumentStore.Recent>,
    open: () -> Unit,
    newWord: () -> Unit,
    newExcel: () -> Unit,
    newSlides: () -> Unit,
    newText: () -> Unit,
    openRecent: (DocumentStore.Recent) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(RecentFilter.ALL) }
    val filtered = remember(query, activeFilter, recent) {
        recent.filter { file ->
            (activeFilter == RecentFilter.ALL || groupFor(file.name) == activeFilter) &&
                (query.isBlank() || file.name.contains(query.trim(), ignoreCase = true))
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 105.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Tài liệu của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Làm việc ngay trên điện thoại, không tải lên máy chủ.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Tìm trong tài liệu gần đây...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (query.isNotBlank()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Xóa tìm kiếm") } }
                } else null,
                shape = RoundedCornerShape(18.dp)
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = .13f),
                        RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(27.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mở tệp từ điện thoại", fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("PDF · DOCX · XLSX · PPTX · CSV", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    FilledTonalIconButton(onClick = open) {
                        Icon(Icons.Default.ArrowForward, "Chọn tệp")
                    }
                }
            }
        }
        item {
            Text("Tạo nhanh", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                QuickCreateCard("Word", "DOCX", Icons.Default.Description, Color(0xFF2670BF), newWord)
                QuickCreateCard("Excel", "XLSX", Icons.Default.TableChart, Color(0xFF138451), newExcel)
                QuickCreateCard("Slide", "PPTX", Icons.Default.Slideshow, Color(0xFFE16B34), newSlides)
                QuickCreateCard("Ghi chú", "TXT", Icons.Default.EditNote, Color(0xFF9A7023), newText)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tệp gần đây", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${recent.size} tệp", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RecentFilter.entries.forEach { option ->
                    FilterChip(
                        selected = activeFilter == option,
                        onClick = { activeFilter = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Surface(color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (recent.isEmpty()) Icons.Default.FolderOpen else Icons.Default.SearchOff, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(34.dp))
                        Text(if (recent.isEmpty()) "Chưa có tài liệu gần đây" else "Không có tệp phù hợp",
                            fontWeight = FontWeight.Medium)
                        Text(if (recent.isEmpty()) "Nhấn 'Mở tệp' để chọn tài liệu từ điện thoại."
                            else "Thử tìm từ khóa khác hoặc chọn 'Tất cả'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (recent.isEmpty()) TextButton(onClick = open) { Text("Chọn tài liệu") }
                    }
                }
            }
        } else {
            items(filtered, key = { it.uri }) { file ->
                RecentFileRow(file = file, onClick = { openRecent(file) })
            }
        }
    }
}

@Composable
private fun QuickCreateCard(
    title: String, ext: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color, onClick: () -> Unit
) {
    Surface(onClick = onClick, modifier = Modifier.width(108.dp).height(105.dp),
        shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, modifier = Modifier.size(27.dp), tint = tint)
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Text(ext, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RecentFileRow(file: DocumentStore.Recent, onClick: () -> Unit) {
    val group = groupFor(file.name)
    val tint = when (group) {
        RecentFilter.WORD -> Color(0xFF2670BF)
        RecentFilter.EXCEL -> Color(0xFF138451)
        RecentFilter.SLIDES -> Color(0xFFE16B34)
        RecentFilter.PDF -> Color(0xFFCF484D)
        else -> MaterialTheme.colorScheme.primary
    }
    val timeText = remember(file.openedAt) {
        if (file.openedAt > 0L) {
            java.text.SimpleDateFormat("dd/MM/yyyy · HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(file.openedAt))
        } else "Đã mở gần đây"
    }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(17.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(45.dp).background(tint.copy(alpha = .10f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) {
                Icon(iconFor(file.name), null, tint = tint, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(file.name, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${group.label} · $timeText", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun iconFor(name: String): androidx.compose.ui.graphics.vector.ImageVector = when {
    name.endsWith(".xlsx", true) || name.endsWith(".csv", true) || name.endsWith(".xls", true) -> Icons.Default.TableChart
    name.endsWith(".pdf", true) -> Icons.Default.PictureAsPdf
    name.endsWith(".pptx", true) || name.endsWith(".ppt", true) -> Icons.Default.Slideshow
    else -> Icons.Default.Description
}

@Composable
private fun ConvertContent(openDocument: () -> Unit, imagesPdf: () -> Unit, openPdf: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Chuyển đổi tài liệu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Tất cả xử lý trên điện thoại. Chuyển Word / Excel sang PDF dạng chữ không giữ bố cục gốc.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
        item { ToolAction(Icons.Default.Image, "Ảnh → PDF", "Ghép một hoặc nhiều ảnh thành PDF.", imagesPdf) }
        item { ToolAction(Icons.Default.Description, "Word / Excel → PDF chữ", "Mở DOCX/XLSX, sau đó chọn Xuất PDF.", openDocument) }
        item { ToolAction(Icons.Default.Collections, "PDF → ảnh PNG", "Mở PDF, chọn trang và xuất ảnh.", openPdf) }
    }
}

@Composable
private fun ToolsContent(merge: () -> Unit, imagesPdf: () -> Unit, openPdf: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Công cụ tài liệu", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text("Tất cả thao tác bên dưới đều thực hiện trên thiết bị, không upload file.",
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { ToolAction(Icons.Default.JoinFull, "Ghép nhiều PDF", "Chọn ít nhất hai file PDF và lưu thành một file.", merge) }
        item { ToolAction(Icons.Default.Image, "Ảnh → PDF", "Tạo PDF từ một hoặc nhiều ảnh JPG/PNG.", imagesPdf) }
        item { ToolAction(Icons.Default.ContentCut, "Tách trang PDF", "Mở PDF rồi nhập phạm vi trang cần trích xuất.", openPdf) }
        item { ToolAction(Icons.Default.Collections, "PDF → PNG", "Mở PDF rồi chọn xuất trang đang xem thành ảnh.", openPdf) }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Text("Word/Excel → PDF dạng văn bản: mở tệp, chọn ‘Xuất PDF’. Công cụ này chưa giữ nguyên bố cục Office.",
                    Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun ToolAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String,
                       subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun AboutContent() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("DocFlow Offline v0.3", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Dự án Android mã nguồn để phát triển ứng dụng Office offline, không cần đăng nhập và không có quyền INTERNET.")
        HorizontalDivider()
        Text("Đã hỗ trợ", fontWeight = FontWeight.Bold)
        Text("• DOCX: xem đoạn văn, chỉnh sửa tài liệu đơn giản, lưu bản sao.\n" +
            "• XLSX: lưới cuộn hai chiều, tìm và đến ô, sửa giá trị/công thức (chưa tính lại).\n" +
            "• PPTX: xem và thay chữ có sẵn trên slide (chưa hỗ trợ tạo thêm slide hoặc sửa bố cục).\n" +
            "• TXT/CSV: đọc và chỉnh sửa.\n" +
            "• PDF: xem trang, xuất PNG, ghép, tách trang, tạo PDF từ ảnh.\n" +
            "• Xuất nội dung văn bản sang PDF dạng đơn giản.")
        HorizontalDivider()
        Text("Chưa hỗ trợ", fontWeight = FontWeight.Bold)
        Text("Hiển thị Office chuẩn WYSIWYG, chỉnh sửa PowerPoint, PivotTable/macro, PDF chỉnh sửa chữ gốc, OCR và PDF ↔ Office giữ bố cục.")
        Text("Để đạt đầy đủ tính năng Office offline cần tự biên dịch và tích hợp Collabora/LibreOffice Native Engine; mã nguồn này chưa chứa bộ máy đó.",
            color = MaterialTheme.colorScheme.primary)
    }
}
