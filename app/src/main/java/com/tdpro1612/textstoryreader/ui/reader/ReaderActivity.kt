package com.tdpro1612.textstoryreader.ui.reader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ReaderActivity : ComponentActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bookId = intent.getIntExtra("EXTRA_BOOK_ID", -1)

        if (bookId == -1) {
            Toast.makeText(this, "Không tìm thấy ID truyện!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.loadBook(bookId)

        setContent {
            MaterialTheme {
                ReaderScreen(
                    viewModel = viewModel,
                    bookId = bookId,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    bookId: Int,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                if (uiState is ReaderUiState.Success) {
                    val successState = uiState as ReaderUiState.Success
                    val drawerListState = rememberLazyListState()

                    LaunchedEffect(drawerState.isOpen) {
                        if (drawerState.isOpen && successState.currentChapterIndex >= 0) {
                            drawerListState.scrollToItem(successState.currentChapterIndex)
                        }
                    }

                    Text(
                        text = "Mục Lục (${successState.chapters.size} chương)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    HorizontalDivider()

                    LazyColumn(
                        state = drawerListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(successState.chapters) { index, chapter ->
                            val isSelected = index == successState.currentChapterIndex
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = chapter.title,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                        style = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadChapter(index)
                                        scope.launch { drawerState.close() }
                                    }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (uiState is ReaderUiState.Success) {
                            val state = uiState as ReaderUiState.Success
                            Text(
                                text = state.book.title,
                                maxLines = 1
                            )
                        } else {
                            Text("Đọc truyện")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                        }
                    },
                    actions = {
                        if (uiState is ReaderUiState.Success) {
                            IconButton(onClick = { viewModel.forceReloadChapters() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Tải lại mục lục")
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, contentDescription = "Mục lục")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (uiState is ReaderUiState.Success) {
                    val state = uiState as ReaderUiState.Success
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
                                onClick = { viewModel.previousChapter() },
                                enabled = state.currentChapterIndex > 0
                            ) {
                                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Chương trước")
                            }

                            Text(
                                text = "${state.currentChapterIndex + 1} / ${state.chapters.size}",
                                style = MaterialTheme.typography.labelLarge
                            )

                            IconButton(
                                onClick = { viewModel.nextChapter() },
                                enabled = state.currentChapterIndex < state.chapters.size - 1
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Chương sau")
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState) {
                    is ReaderUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ReaderUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadBook(bookId) }
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                    is ReaderUiState.Success -> {
                        val scrollState = rememberScrollState()

                        // Đánh dấu đã khôi phục xong vị trí ban đầu
                        var isInitialRestored by remember(state.book.id) { mutableStateOf(false) }

                        // Theo dõi chương trước đó
                        var activeChapterIndex by remember(state.book.id) { mutableIntStateOf(state.currentChapterIndex) }

                        // 1. Phục hồi vị trí cuộn ban đầu đúng chương đã đọc dở
                        LaunchedEffect(state.book.id) {
                            if (!isInitialRestored) {
                                if (state.book.lastPosition > 0) {
                                    scrollState.scrollTo(state.book.lastPosition)
                                }
                                isInitialRestored = true
                            }
                        }

                        // 2. Chỉ cuộn về 0 nếu thực sự đổi chương
                        LaunchedEffect(state.currentChapterIndex) {
                            if (isInitialRestored && state.currentChapterIndex != activeChapterIndex) {
                                scrollState.scrollTo(0)
                                activeChapterIndex = state.currentChapterIndex
                            }
                        }

                        // 3. Sử dụng debounce(300ms) để giảm tần suất ghi vào DB khi cuộn màn hình
                        LaunchedEffect(scrollState, isInitialRestored, state.currentChapterIndex) {
                            if (isInitialRestored) {
                                snapshotFlow { scrollState.value }
                                    .distinctUntilChanged()
                                    .debounce(300L) // Chờ dừng vuốt 300ms mới lưu vào DB
                                    .collect { position ->
                                        val totalChapters = state.chapters.size
                                        val progress = if (totalChapters > 0) {
                                            ((state.currentChapterIndex + 1).toFloat() / totalChapters.toFloat()) * 100f
                                        } else 0f

                                        viewModel.saveProgress(
                                            chapterIndex = state.currentChapterIndex,
                                            position = position,
                                            progress = progress
                                        )
                                    }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = state.chapters[state.currentChapterIndex].title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Text(
                                text = state.currentChapterContent.ifBlank { "Đang tải nội dung..." },
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    lineHeight = 28.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}