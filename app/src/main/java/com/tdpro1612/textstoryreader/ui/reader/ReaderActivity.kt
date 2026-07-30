package com.tdpro1612.textstoryreader.ui.reader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    is ReaderUiState.Success -> {
                        val scrollState = rememberScrollState()

                        LaunchedEffect(state.currentChapterIndex) {
                            scrollState.scrollTo(0)
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