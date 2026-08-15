package com.tdpro1612.textstoryreader.ui.library

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.scanner.ScanProgressState
import com.tdpro1612.textstoryreader.ui.reader.ReaderActivity

class LibraryActivity : ComponentActivity() {

    private val viewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                LibraryScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        val intent = Intent(this, ReaderActivity::class.java).apply {
                            putExtra("EXTRA_BOOK_ID", book.id)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (BookEntity) -> Unit = {}
) {
    val context = LocalContext.current

    val bookList by viewModel.bookList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val totalBooksCount by viewModel.totalBooksCount.collectAsState()

    // Lịch sử đọc từ ViewModel
    val recentHistoryBooks by viewModel.recentHistoryBooks.collectAsState()

    // Trạng thái đóng/mở BottomSheet Lịch sử
    var showHistorySheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var lastSelectedUri by remember { mutableStateOf<String?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Sách được chọn để xóa
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(currentPage) {
        if (bookList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                lastSelectedUri = it.toString()
                viewModel.scanFolder(it.toString(), clearOldData = false)
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể cấp quyền đọc/ghi thư mục!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(scanState) {
        when (val state = scanState) {
            is ScanProgressState.Error -> {
                Toast.makeText(context, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
            }
            is ScanProgressState.Finished -> {
                Toast.makeText(
                    context,
                    "Quét xong! Thêm mới: ${state.newBooksCount}, Cập nhật: ${state.updatedBooksCount}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }
    }

    // 🔥 AlertDialog Xác nhận Xóa sách
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Xóa truyện") },
            text = { Text("Bạn muốn xóa \"${book.title}\" như thế nào?") },
            confirmButton = {
                // Nút 1: Xóa sạch cả file gốc trong máy
                TextButton(
                    onClick = {
                        viewModel.deleteBook(book, deletePhysicalFile = true)
                        bookToDelete = null
                        Toast.makeText(context, "Đã gửi lệnh xóa file gốc", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa hẳn file gốc")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text("Hủy")
                    }
                    // Nút 2: Chỉ xóa khỏi App (giữ file gốc)
                    TextButton(
                        onClick = {
                            viewModel.deleteBook(book, deletePhysicalFile = false)
                            bookToDelete = null
                            Toast.makeText(context, "Đã xóa khỏi ứng dụng", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Chỉ xóa khỏi ứng dụng")
                    }
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Làm mới thư viện") },
            text = { Text("Bạn có chắc chắn muốn xóa toàn bộ danh sách cũ và quét lại từ đầu không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
                        lastSelectedUri?.let { uri ->
                            viewModel.scanFolder(uri, clearOldData = true)
                        }
                    }
                ) {
                    Text("Xác nhận quét lại")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // 🔥 Modal BottomSheet hiển thị Lịch sử đọc
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Lịch sử đọc gần đây",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (recentHistoryBooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có lịch sử đọc nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(
                            items = recentHistoryBooks,
                            key = { book -> book.id }
                        ) { book ->
                            // Xóa trong Lịch sử -> gọi removeFromHistory
                            BookItem(
                                book = book,
                                onClick = {
                                    showHistorySheet = false
                                    onBookClick(book)
                                },
                                onDeleteClick = {
                                    viewModel.removeFromHistory(book)
                                    Toast.makeText(context, "Đã xóa khỏi lịch sử đọc", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thư Viện ($totalBooksCount truyện)") },
                actions = {
                    IconButton(onClick = { showHistorySheet = true }) {
                        Icon(Icons.Default.History, contentDescription = "Lịch sử đọc")
                    }
                    IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Chọn thư mục quét")
                    }
                    if (lastSelectedUri != null) {
                        IconButton(onClick = { showClearConfirmDialog = true }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Xóa & Quét lại")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (totalBooksCount > 0) {
                Surface(
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousPage() },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trang trước")
                        }

                        Text(
                            text = "Trang $currentPage / $totalPages",
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { viewModel.nextPage() },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Trang sau")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchBooks(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm kiếm truyện hoặc tác giả...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (scanState is ScanProgressState.Scanning) {
                val state = scanState as ScanProgressState.Scanning
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Đang quét: ${state.scannedCount} / ${state.totalFiles} file...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (bookList.isEmpty() && scanState !is ScanProgressState.Scanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có truyện nào trong thư mục này.\nBấm vào biểu tượng Thư mục ở góc trên để chọn thư mục quét!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = bookList,
                        key = { book -> book.id }
                    ) { book ->
                        // 🔥 Đã nối chính xác onDeleteClick -> gán bookToDelete để mở AlertDialog
                        BookItem(
                            book = book,
                            onClick = { onBookClick(book) },
                            onDeleteClick = { bookToDelete = book }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookItem(
    book: BookEntity,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${book.fileType.uppercase()} • ${android.text.format.Formatter.formatFileSize(LocalContext.current, book.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${book.readProgress.toInt()}%",
                style = MaterialTheme.typography.labelMedium
            )
            // Menu 3 chấm
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Tùy chọn"
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Xóa khỏi thư viện", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClick() // Gọi callback bắn ra ngoài
                        }
                    )
                }
            }
        }
    }
}