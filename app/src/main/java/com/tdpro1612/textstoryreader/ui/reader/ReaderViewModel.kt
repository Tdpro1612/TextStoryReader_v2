package com.tdpro1612.textstoryreader.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdpro1612.textstoryreader.database.AppDatabase
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.manager.BookManager
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.reader.ReaderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val book: BookEntity,
        val chapters: List<BookChapter>,
        val currentChapterIndex: Int,
        val currentChapterContent: String
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val bookManager = BookManager(application)
    private val bookQueries = AppDatabase.getInstance(application).bookQueries()

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentBook: BookEntity? = null
    private var chaptersList: List<BookChapter> = emptyList()
    private var currentChapterIndex: Int = 0

    fun loadBook(bookId: Int) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val book = withContext(Dispatchers.IO) {
                    bookQueries.getBookById(bookId)
                }

                if (book == null) {
                    _uiState.value = ReaderUiState.Error("Không tìm thấy thông tin sách trong dữ liệu!")
                    return@launch
                }

                currentBook = book
                currentChapterIndex = book.lastChapterIndex

                val bookUri = Uri.parse(book.filePath)

                val canOpen = withContext(Dispatchers.IO) {
                    try {
                        getApplication<Application>().contentResolver.openInputStream(bookUri)?.use { true } ?: false
                    } catch (e: Exception) {
                        false
                    }
                }

                if (!canOpen) {
                    _uiState.value = ReaderUiState.Error("Không thể mở file truyện. File có thể đã bị xóa hoặc di chuyển!")
                    return@launch
                }

                val reader = ReaderFactory.getReader(bookUri)
                chaptersList = reader.getChapterList(getApplication(), bookUri)

                if (chaptersList.isEmpty()) {
                    _uiState.value = ReaderUiState.Error("Tệp truyện rỗng hoặc không phân tích được chương nào!")
                    return@launch
                }

                if (currentChapterIndex !in chaptersList.indices) {
                    currentChapterIndex = 0
                }

                val content = reader.getChapterContent(getApplication(), bookUri, chaptersList[currentChapterIndex])

                saveProgress(
                    chapterIndex = currentChapterIndex,
                    position = book.lastPosition,
                    progress = book.readProgress
                )

                _uiState.value = ReaderUiState.Success(
                    book = book,
                    chapters = chaptersList,
                    currentChapterIndex = currentChapterIndex,
                    currentChapterContent = content
                )

            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("Lỗi khi mở truyện: ${e.localizedMessage}")
            }
        }
    }

    fun loadChapter(chapterIndex: Int) {
        val book = currentBook ?: return
        if (chapterIndex !in chaptersList.indices) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is ReaderUiState.Success) {
                    _uiState.value = currentState.copy(
                        currentChapterIndex = chapterIndex,
                        currentChapterContent = "Đang tải nội dung..."
                    )
                }

                val bookUri = Uri.parse(book.filePath)
                currentChapterIndex = chapterIndex

                // Đọc cực nhanh nhờ đã có Cache từ EpubContentReader
                val reader = ReaderFactory.getReader(bookUri)
                val content = withContext(Dispatchers.IO) {
                    reader.getChapterContent(getApplication(), bookUri, chaptersList[chapterIndex])
                }

                val progress = ((chapterIndex + 1).toFloat() / chaptersList.size.toFloat()) * 100f

                saveProgress(
                    chapterIndex = chapterIndex,
                    position = 0,
                    progress = progress
                )

                _uiState.value = ReaderUiState.Success(
                    book = book,
                    chapters = chaptersList,
                    currentChapterIndex = chapterIndex,
                    currentChapterContent = content
                )
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("Lỗi khi chuyển chương: ${e.localizedMessage}")
            }
        }
    }

    fun nextChapter() {
        if (currentChapterIndex < chaptersList.size - 1) {
            loadChapter(currentChapterIndex + 1)
        }
    }

    fun previousChapter() {
        if (currentChapterIndex > 0) {
            loadChapter(currentChapterIndex - 1)
        }
    }

    fun saveProgress(chapterIndex: Int, position: Int, progress: Float) {
        val book = currentBook ?: return
        viewModelScope.launch {
            bookManager.updateReadingProgress(
                bookId = book.id,
                chapterIndex = chapterIndex,
                position = position,
                progress = progress
            )
        }
    }
}