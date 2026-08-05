package com.tdpro1612.textstoryreader.ui.reader

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdpro1612.textstoryreader.database.AppDatabase
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.manager.BookManager
import com.tdpro1612.textstoryreader.reader.BookChapter
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

    var currentChapterIndex: Int = 0
        private set
    private var currentPosition: Int = 0

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
                // Gán đúng chương cũ và vị trí cũ từ DB
                currentChapterIndex = book.lastChapterIndex
                currentPosition = book.lastPosition

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

                // ⏱️ 1. Đo thời gian lấy Danh sách chương
                val t1 = System.currentTimeMillis()
                chaptersList = withContext(Dispatchers.IO) {
                    bookManager.getChapterList(bookUri, book.id)
                }
                Log.d("ReaderPerf", "1. Đã lấy danh sách chương (${chaptersList.size} chương): ${System.currentTimeMillis() - t1} ms")

                if (chaptersList.isEmpty()) {
                    _uiState.value = ReaderUiState.Error("Tệp truyện rỗng hoặc không phân tích được chương nào!")
                    return@launch
                }

                // Kiểm tra bound index an toàn
                if (currentChapterIndex !in chaptersList.indices) {
                    currentChapterIndex = 0
                    currentPosition = 0
                }

                // ⏱️ 2. Đo thời gian lấy Nội dung chương hiện tại
                val t2 = System.currentTimeMillis()
                val content = withContext(Dispatchers.IO) {
                    bookManager.getChapterContent(bookUri, chaptersList[currentChapterIndex])
                }
                Log.d("ReaderPerf", "2. Đã đọc xong text chương ${currentChapterIndex}: ${System.currentTimeMillis() - t2} ms")

                // ⏱️ 3. Đo thời gian Render/Đẩy sang UI State
                val t3 = System.currentTimeMillis()
                _uiState.value = ReaderUiState.Success(
                    book = book,
                    chapters = chaptersList,
                    currentChapterIndex = currentChapterIndex,
                    currentChapterContent = content
                )
                Log.d("ReaderPerf", "3. Đã đẩy dữ liệu ra UI State: ${System.currentTimeMillis() - t3} ms")

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
                currentPosition = 0 // Chuyển chương mới -> Reset vị trí cuộn về 0

                // ⏱️ Đo thời gian đọc nội dung khi chuyển chương
                val t2 = System.currentTimeMillis()
                val content = withContext(Dispatchers.IO) {
                    bookManager.getChapterContent(bookUri, chaptersList[currentChapterIndex])
                }
                Log.d("ReaderPerf", "2. [Chuyển chương $chapterIndex] Đã đọc xong text: ${System.currentTimeMillis() - t2} ms")

                val progress = ((chapterIndex + 1).toFloat() / chaptersList.size.toFloat()) * 100f

                saveProgress(
                    chapterIndex = chapterIndex,
                    position = 0,
                    progress = progress
                )

                val t3 = System.currentTimeMillis()
                _uiState.value = ReaderUiState.Success(
                    book = book,
                    chapters = chaptersList,
                    currentChapterIndex = chapterIndex,
                    currentChapterContent = content
                )
                Log.d("ReaderPerf", "3. [Chuyển chương $chapterIndex] Đã đẩy dữ liệu ra UI State: ${System.currentTimeMillis() - t3} ms")

            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("Lỗi khi chuyển chương: ${e.localizedMessage}")
            }
        }
    }

    fun forceReloadChapters() {
        val book = currentBook ?: return
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val bookUri = Uri.parse(book.filePath)

                withContext(Dispatchers.IO) {
                    bookManager.clearCache(bookUri)
                    bookQueries.deleteChaptersByBookId(book.id)
                }

                val t1 = System.currentTimeMillis()
                chaptersList = withContext(Dispatchers.IO) {
                    bookManager.getChapterList(bookUri, book.id)
                }
                Log.d("ReaderPerf", "1. [Reload] Đã lấy danh sách chương: ${System.currentTimeMillis() - t1} ms")

                if (chaptersList.isEmpty()) {
                    _uiState.value = ReaderUiState.Error("Không tìm thấy chương nào sau khi làm mới!")
                    return@launch
                }

                currentChapterIndex = 0
                currentPosition = 0

                val t2 = System.currentTimeMillis()
                val content = withContext(Dispatchers.IO) {
                    bookManager.getChapterContent(bookUri, chaptersList[currentChapterIndex])
                }
                Log.d("ReaderPerf", "2. [Reload] Đã đọc xong text chương 1: ${System.currentTimeMillis() - t2} ms")

                val t3 = System.currentTimeMillis()
                _uiState.value = ReaderUiState.Success(
                    book = book,
                    chapters = chaptersList,
                    currentChapterIndex = currentChapterIndex,
                    currentChapterContent = content
                )
                Log.d("ReaderPerf", "3. [Reload] Đã đẩy dữ liệu ra UI State: ${System.currentTimeMillis() - t3} ms")

            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error("Lỗi khi làm mới mục lục: ${e.localizedMessage}")
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
        this.currentChapterIndex = chapterIndex
        this.currentPosition = position

        viewModelScope.launch(Dispatchers.IO) {
            bookManager.updateReadingProgress(
                bookId = book.id,
                chapterIndex = chapterIndex,
                position = position,
                progress = progress
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        val book = currentBook ?: return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Lưu tiến độ đọc cuối cùng
            if (chaptersList.isNotEmpty()) {
                val progress = ((currentChapterIndex + 1).toFloat() / chaptersList.size.toFloat()) * 100f
                bookManager.updateReadingProgress(
                    bookId = book.id,
                    chapterIndex = currentChapterIndex,
                    position = currentPosition,
                    progress = progress
                )
            }

            // 2. Dọn dẹp cache của cuốn sách hiện tại thông qua BookManager
//            val bookUri = Uri.parse(book.filePath)
//            bookManager.clearCache(bookUri)
        }
    }
}