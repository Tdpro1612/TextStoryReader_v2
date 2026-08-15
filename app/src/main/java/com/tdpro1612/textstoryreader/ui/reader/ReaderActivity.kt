package com.tdpro1612.textstoryreader.ui.reader

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdpro1612.textstoryreader.manager.SettingsManager
import com.tdpro1612.textstoryreader.ui.settings.SettingsScreen
import com.tdpro1612.textstoryreader.ui.settings.SettingsViewModel
import kotlinx.coroutines.FlowPreview
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

        val settingsManager = SettingsManager(applicationContext)
        val settingsViewModel = SettingsViewModel(settingsManager)

        setContent {
            MaterialTheme {
                ReaderScreen(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel,
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
    settingsViewModel: SettingsViewModel,
    bookId: Int,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val readerSettings by settingsViewModel.readerSettings.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showSettingsSheet by remember { mutableStateOf(false) }

    // 🔥 Gọi hàm xử lý màu Theme trực tiếp từ SettingsViewModel
    val (backgroundColor, textColor) = settingsViewModel.getThemeColors(readerSettings.themePreset)

    // 🔥 Xử lý Luôn bật màn hình (keepScreenOn)
    DisposableEffect(readerSettings.keepScreenOn) {
        val activity = context as? Activity
        if (readerSettings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    BackHandler(enabled = drawerState.isOpen || showSettingsSheet) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (showSettingsSheet) {
            showSettingsSheet = false
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { showSettingsSheet = false }
            )
        }
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mục Lục (${successState.chapters.size} chương)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { viewModel.forceReloadChapters() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Tải lại mục lục")
                        }
                    }
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
                                maxLines = 1,
                                color = textColor
                            )
                        } else {
                            Text("Đọc truyện", color = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = textColor)
                        }
                    },
                    actions = {
                        if (uiState is ReaderUiState.Success) {
                            IconButton(onClick = { showSettingsSheet = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = textColor)
                            }
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, contentDescription = "Mục lục", tint = textColor)
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
                        color = backgroundColor,
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
                                Icon(
                                    Icons.Default.ArrowBackIosNew,
                                    contentDescription = "Chương trước",
                                    tint = if (state.currentChapterIndex > 0) textColor else textColor.copy(alpha = 0.3f)
                                )
                            }

                            Text(
                                text = "${state.currentChapterIndex + 1} / ${state.chapters.size}",
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor
                            )

                            IconButton(
                                onClick = { viewModel.nextChapter() },
                                enabled = state.currentChapterIndex < state.chapters.size - 1
                            ) {
                                Icon(
                                    Icons.Default.ArrowForwardIos,
                                    contentDescription = "Chương sau",
                                    tint = if (state.currentChapterIndex < state.chapters.size - 1) textColor else textColor.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
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
                        // ⚡ Gọi ReaderContentView với tiến độ readProgress đồng bộ chuẩn
                        ReaderContentView(
                            content = state.currentChapterContent.ifBlank { "Đang tải nội dung..." },
                            currentChapter = state.chapters[state.currentChapterIndex],
                            readProgress = state.readProgress,
                            hasPreviousChapter = state.currentChapterIndex > 0,
                            hasNextChapter = state.currentChapterIndex < state.chapters.size - 1,
                            settings = readerSettings,
                            onNextChapter = { viewModel.nextChapter() },
                            onPreviousChapter = { viewModel.previousChapter() },
                            onProgressChanged = { newProgress ->
                                viewModel.onProgressChanged(newProgress)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}