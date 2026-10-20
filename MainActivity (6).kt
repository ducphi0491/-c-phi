package com.docflowoffline

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import android.content.Context
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

// ================ MainActivity.kt ================
// Nếu Theme.kt nằm trong package ui.theme thì bỏ dấu // ở dòng dưới:
// import com.docflowoffline.ui.theme.DocFlowTheme

class MainActivity : ComponentActivity() {
    private var incomingUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUri = if (intent?.action == Intent.ACTION_VIEW) intent?.data else null
        setContent {
            DocFlowTheme {
                DocFlowApp(incomingUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = if (intent.action == Intent.ACTION_VIEW) intent.data else null
    }
}


// ================ Theme.kt ================
private val LightScheme = lightColorScheme(
    primary = Color(0xFF245FC7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE9FF),
    onPrimaryContainer = Color(0xFF15345E),
    secondary = Color(0xFF52668A),
    onSecondary = Color.White,
    background = Color(0xFFF9FAFF),
    onBackground = Color(0xFF1C2940),
    surface = Color.White,
    onSurface = Color(0xFF1C2940),
    onSurfaceVariant = Color(0xFF64738A),
    surfaceVariant = Color(0xFFEDF2FA),
    outlineVariant = Color(0xFFDCE4EF)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFA3C5FF),
    onPrimary = Color(0xFF002A60),
    primaryContainer = Color(0xFF294777),
    onPrimaryContainer = Color(0xFFE0EBFF),
    secondary = Color(0xFFB3C7EC),
    background = Color(0xFF10151E),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF191F2B),
    onSurface = Color(0xFFE8ECF4),
    onSurfaceVariant = Color(0xFFB5C1D1),
    surfaceVariant = Color(0xFF2A3444),
    outlineVariant = Color(0xFF414E62)
)

@Composable
fun DocFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}


// ================ DocFlowApp.kt ================
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
    var selectedCategory by remember { mutableStateOf<CatalogKind?>(null) }
    var folderSourcesVisible by remember { mutableStateOf(false) }
    var linkedFolders by remember { mutableStateOf(DocumentCatalog.folders(context)) }
    var catalog by remember { mutableStateOf(CatalogScan(emptyList(), emptyList(), false)) }
    var scanningFolders by remember { mutableStateOf(false) }
    var rescanToken by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(linkedFolders, rescanToken) {
        scanningFolders = true
        try {
            catalog = withContext(Dispatchers.IO) { DocumentCatalog.scan(context, linkedFolders) }
        } catch (e: Exception) {
            notify("Không thể lập danh sách tài liệu: ${e.message ?: "lỗi đọc thư mục"}")
        } finally { scanningFolders = false }
    }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                linkedFolders = DocumentCatalog.add(context, uri)
                folderSourcesVisible = false
                rescanToken++
                notify("Đã thêm thư mục. Đang phân loại tài liệu...")
            } catch (e: Exception) {
                notify("Không cấp được quyền thư mục: ${e.message ?: "lỗi hệ thống"}")
            }
        }
    }

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

    BackHandler(enabled = document != null || selectedCategory != null || folderSourcesVisible) {
        when {
            folderSourcesVisible -> folderSourcesVisible = false
            document != null -> document = null
            else -> selectedCategory = null
        }
    }
    if (folderSourcesVisible) {
        FolderSourcesDialog(
            folders = linkedFolders,
            onDismiss = { folderSourcesVisible = false },
            onAdd = { folderSourcesVisible = false; folderPicker.launch(null) },
            onRemove = { folder ->
                linkedFolders = DocumentCatalog.remove(context, folder)
                rescanToken++
            }
        )
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            when {
                document == null && selectedCategory == null -> DocFlowHomeHeader()
                document !is DocumentStore.OpenFile.Excel -> TopAppBar(
                    title = {
                        Column {
                            Text(document?.name ?: "Thư mục ${selectedCategory?.title}",
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (document != null) "Chỉnh sửa an toàn · lưu bản sao"
                                else "Tài liệu theo định dạng · offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (document != null) document = null else selectedCategory = null
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface)
                )
                else -> Unit // Excel already has its own compact toolbar.
            }
        },
        bottomBar = {
            if (document == null && selectedCategory == null) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f),
                tonalElevation = 1.dp
            ) {
                listOf(
                    Triple(HomeTab.HOME, "Tài liệu", Icons.Default.Home),
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
            if (document == null && selectedCategory == null && activeTab == HomeTab.HOME) {
                Box {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateMenu = true },
                        containerColor = Color(0xFFDCE9FF),
                        contentColor = Color(0xFF163A70),
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Tạo mới", fontWeight = FontWeight.Bold) }
                    )
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
                    notify = ::notify,
                    onBack = { document = null }
                )
            } else if (selectedCategory != null) {
                CategoryFolderScreen(
                    kind = selectedCategory!!,
                    files = catalog.items,
                    recent = recent,
                    folders = linkedFolders,
                    scanning = scanningFolders,
                    warnings = catalog.warnings,
                    limited = catalog.limited,
                    openItem = { uri, name ->
                        if (isSupportedDocument(name)) openUri(Uri.parse(uri))
                        else notify("${name.substringAfterLast('.', "").uppercase()} chưa hỗ trợ chỉnh sửa trực tiếp; hãy chuyển sang DOCX/XLSX/PPTX.")
                    },
                    addFolder = { folderPicker.launch(null) },
                    manageFolders = { folderSourcesVisible = true },
                    refresh = { rescanToken++ }
                )
            } else when (activeTab) {
                HomeTab.HOME -> HomeContent(
                    recent = recent,
                    catalog = catalog.items,
                    linkedFolders = linkedFolders,
                    scanningFolders = scanningFolders,
                    openCategory = { selectedCategory = it },
                    addFolder = { folderPicker.launch(null) },
                    manageFolders = { folderSourcesVisible = true },
                    open = { openPicker.launch(arrayOf("*/*")) },
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

/** Prominent app header, with a locally bundled icon (no Internet request). */
@Composable
private fun DocFlowHomeHeader() {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp).padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(painterResource(R.drawable.df_app), null, Modifier.size(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("DocFlow", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Văn phòng offline", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Offline", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

/** Real asset for each format, from the DocFlow artwork. Brand-neutral app ownership. */
@Composable
internal fun DocFlowFileLogo(kind: CatalogKind, modifier: Modifier = Modifier.size(50.dp)) {
    val drawable = when (kind) {
        CatalogKind.PDF -> R.drawable.df_pdf
        CatalogKind.EXCEL -> R.drawable.df_excel
        CatalogKind.WORD -> R.drawable.df_word
        CatalogKind.SLIDES -> R.drawable.df_ppt
        CatalogKind.TEXT -> R.drawable.df_text
    }
    Image(painter = painterResource(drawable), contentDescription = kind.title, modifier = modifier)
}

@Composable
private fun HomeContent(
    recent: List<DocumentStore.Recent>,
    catalog: List<CatalogItem>,
    linkedFolders: List<CatalogFolder>,
    scanningFolders: Boolean,
    openCategory: (CatalogKind) -> Unit,
    addFolder: () -> Unit,
    manageFolders: () -> Unit,
    open: () -> Unit,
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tài liệu của bạn", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold)
                Text("Làm việc ngay trên điện thoại, không tải lên máy chủ.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                shape = RoundedCornerShape(22.dp)
            )
        }
        item {
            Surface(
                onClick = open, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(23.dp), color = Color.Transparent
            ) {
                Row(
                    Modifier.fillMaxWidth().background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFDFEAFE), Color(0xFFE8F1FF))
                        ), RoundedCornerShape(23.dp)
                    ).padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Image(painterResource(R.drawable.df_folder), null, Modifier.size(54.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mở tệp từ điện thoại", color = Color(0xFF17365E),
                            fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("PDF · DOCX · XLSX · PPTX · CSV", color = Color(0xFF3C587E),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFFF0E2F9)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Chọn tệp",
                            tint = Color(0xFF15233C),
                            modifier = Modifier.padding(9.dp).size(24.dp))
                    }
                }
            }
        }
        item {
            CategoryFolderGrid(
                indexed = catalog, recent = recent, scanning = scanningFolders,
                folders = linkedFolders, open = openCategory,
                addFolder = addFolder, manageFolders = manageFolders
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tệp đã mở gần đây", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${recent.size} tệp", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall)
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
                    shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (recent.isEmpty()) Icons.Default.FolderOpen else Icons.Default.SearchOff, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(35.dp))
                        Text(if (recent.isEmpty()) "Chưa có tài liệu gần đây" else "Không có tệp phù hợp",
                            fontWeight = FontWeight.SemiBold)
                        Text(if (recent.isEmpty()) "Nhấn ‘Mở tệp’ để chọn tài liệu trên điện thoại."
                            else "Thử từ khóa khác hoặc chọn ‘Tất cả’.",
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
private fun RecentFileRow(file: DocumentStore.Recent, onClick: () -> Unit) {
    val group = groupFor(file.name)
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
            DocFlowFileLogo(catalogKind(file.name) ?: CatalogKind.TEXT,
                modifier = Modifier.size(47.dp))
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

/** Conversion and tools screens. UI-only refactor: every callback remains unchanged. */
@Composable
private fun ConvertContent(openDocument: () -> Unit, imagesPdf: () -> Unit, openPdf: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 21.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            DocFlowPageIntro(
                title = "Chuyển đổi tài liệu",
                description = "Chọn định dạng xuất phù hợp với tài liệu của bạn.",
                icon = Icons.Default.SwapHoriz,
                accent = Color(0xFF2F69D9)
            )
        }
        item { DocFlowOfflineBanner() }
        item { DocFlowSectionHeading("Chuyển đổi phổ biến", "3 tác vụ") }
        item {
            DocFlowActionCard(
                title = "Ảnh sang PDF",
                description = "Gộp một hoặc nhiều ảnh JPG, PNG thành PDF.",
                label = "JPG · PNG  →  PDF",
                accent = Color(0xFF2768CB),
                onClick = imagesPdf
            ) {
                Box(Modifier.size(53.dp)) {
                    Icon(Icons.Default.Image, null,
                        modifier = Modifier.align(Alignment.TopStart).size(40.dp),
                        tint = Color(0xFF2768CB))
                    Image(painterResource(R.drawable.df_pdf), null,
                        modifier = Modifier.align(Alignment.BottomEnd).size(32.dp))
                }
            }
        }
        item {
            DocFlowActionCard(
                title = "Word / Excel sang PDF",
                description = "Mở tài liệu, sau đó chọn Xuất PDF dạng chữ.",
                label = "DOCX · XLSX  →  PDF",
                accent = Color(0xFF138454),
                onClick = openDocument
            ) {
                Box(Modifier.size(56.dp)) {
                    Image(painterResource(R.drawable.df_word), null,
                        modifier = Modifier.size(43.dp).align(Alignment.TopStart))
                    Image(painterResource(R.drawable.df_excel), null,
                        modifier = Modifier.size(39.dp).align(Alignment.BottomEnd))
                }
            }
        }
        item {
            DocFlowActionCard(
                title = "PDF sang ảnh PNG",
                description = "Mở PDF, chọn trang cần lưu thành hình ảnh.",
                label = "PDF  →  PNG",
                accent = Color(0xFFD25A59),
                onClick = openPdf
            ) {
                Box(Modifier.size(54.dp)) {
                    Image(painterResource(R.drawable.df_pdf), null,
                        modifier = Modifier.size(44.dp).align(Alignment.TopStart))
                    Icon(Icons.Default.Collections, null,
                        modifier = Modifier.align(Alignment.BottomEnd).size(25.dp),
                        tint = Color(0xFF2F69D9))
                }
            }
        }
        item {
            DocFlowMessageCard(
                title = "Thông tin về chất lượng chuyển đổi",
                description = "Word/Excel xuất PDF dưới dạng nội dung văn bản. Bản hiện tại chưa giữ đầy đủ bố cục Office, hình vẽ và biểu đồ."
            )
        }
    }
}

@Composable
private fun ToolsContent(merge: () -> Unit, imagesPdf: () -> Unit, openPdf: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 17.dp, end = 17.dp, top = 21.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            DocFlowPageIntro(
                title = "Công cụ tài liệu",
                description = "Các tiện ích PDF cần thiết, ngay trong một nơi.",
                icon = Icons.Default.Build,
                accent = Color(0xFF7954BE)
            )
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(painterResource(R.drawable.df_pdf), null, Modifier.size(48.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Bộ công cụ PDF", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text("Ghép, tách và xuất ảnh — không cần Internet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { DocFlowSectionHeading("Công cụ thường dùng", "4 tiện ích") }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    DocFlowToolGridCard(
                        title = "Ghép PDF",
                        description = "Nối nhiều tệp thành một.",
                        accent = Color(0xFF286BCD), onClick = merge
                    ) {
                        Icon(Icons.Default.JoinFull, null, Modifier.size(31.dp),
                            tint = Color(0xFF286BCD))
                    }
                }
                Box(Modifier.weight(1f)) {
                    DocFlowToolGridCard(
                        title = "Tách trang",
                        description = "Trích các trang cần dùng.",
                        accent = Color(0xFF8854CB), onClick = openPdf
                    ) {
                        Icon(Icons.Default.ContentCut, null, Modifier.size(31.dp),
                            tint = Color(0xFF8854CB))
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    DocFlowToolGridCard(
                        title = "Ảnh → PDF",
                        description = "Tạo PDF từ JPG, PNG.",
                        accent = Color(0xFF188A76), onClick = imagesPdf
                    ) {
                        Icon(Icons.Default.Image, null, Modifier.size(31.dp),
                            tint = Color(0xFF188A76))
                    }
                }
                Box(Modifier.weight(1f)) {
                    DocFlowToolGridCard(
                        title = "PDF → PNG",
                        description = "Lưu trang PDF thành ảnh.",
                        accent = Color(0xFFD6783D), onClick = openPdf
                    ) {
                        Icon(Icons.Default.Collections, null, Modifier.size(31.dp),
                            tint = Color(0xFFD6783D))
                    }
                }
            }
        }
        item {
            DocFlowMessageCard(
                title = "Tài liệu gốc luôn được giữ lại",
                description = "Bạn sẽ chọn tên và vị trí lưu tệp kết quả. Để xuất Word/Excel sang PDF chữ, hãy sử dụng tab Chuyển đổi."
            )
        }
    }
}

@Composable
private fun DocFlowPageIntro(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text(description, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier.size(54.dp)
                .background(accent.copy(alpha = if (dark) 0.27f else 0.11f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
private fun DocFlowOfflineBanner() {
    Box(
        modifier = Modifier.fillMaxWidth().background(
            brush = Brush.horizontalGradient(listOf(Color(0xFF204B8A), Color(0xFF336FC1))),
            shape = RoundedCornerShape(22.dp)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CloudOff, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Riêng tư & offline", color = Color.White,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("Xử lý trên điện thoại, không tải lên máy chủ.",
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DocFlowSectionHeading(title: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(trailing, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DocFlowActionCard(
    title: String,
    description: String,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(67.dp)
                    .background(accent.copy(alpha = if (dark) 0.25f else 0.10f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) { artwork() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = accent, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall)
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun DocFlowToolGridCard(
    title: String,
    description: String,
    accent: Color,
    onClick: () -> Unit,
    artwork: @Composable () -> Unit
) {
    val dark = isSystemInDarkTheme()
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(175.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(52.dp)
                        .background(accent.copy(alpha = if (dark) 0.25f else 0.11f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) { artwork() }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(21.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DocFlowMessageCard(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.67f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Info, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AboutContent() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("DocFlow Offline v0.6", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Dự án Android mã nguồn để phát triển ứng dụng Office offline, không cần đăng nhập và không có quyền INTERNET.")
        HorizontalDivider()
        Text("Đã hỗ trợ", fontWeight = FontWeight.Bold)
        Text("• DOCX: xem đoạn văn, chỉnh sửa tài liệu đơn giản, lưu bản sao.\n" +
            "• XLSX: giao diện bảng tính mobile với 6 tab cơ bản, sửa ô, định dạng đơn giản, hàm tính thử, zoom.\n" +
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
