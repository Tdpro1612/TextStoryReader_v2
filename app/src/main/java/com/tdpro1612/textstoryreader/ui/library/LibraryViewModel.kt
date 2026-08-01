package com.tdpro1612.textstoryreader.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.manager.BookManager
import com.tdpro1612.textstoryreader.scanner.ScanProgressState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val bookManager = BookManager(application)

    val scanState: StateFlow<ScanProgressState> = bookManager.scanState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 📄 Quản lý Phân trang dạng bấm nút (Page Index)
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val pageSize = 20 // Số sách trên mỗi trang

    private val _bookList = MutableStateFlow<List<BookEntity>>(emptyList())
    val bookList: StateFlow<List<BookEntity>> = _bookList.asStateFlow()

    private val _totalBooksCount = MutableStateFlow(0)
    val totalBooksCount: StateFlow<Int> = _totalBooksCount.asStateFlow()

    // 🔥 MỚI: Flow cung cấp danh sách 20 cuốn lịch sử đọc gần nhất
    val recentHistoryBooks: StateFlow<List<BookEntity>> = bookManager.getRecentHistory(20)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Tính tổng số trang dựa trên tổng số sách
    val totalPages: StateFlow<Int> = _totalBooksCount.map { count ->
        if (count == 0) 1 else kotlin.math.ceil(count.toDouble() / pageSize).toInt()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    init {
        // Tự động load lại trang khi thay đổi từ khóa tìm kiếm hoặc số trang
        viewModelScope.launch {
            combine(_searchQuery, _currentPage) { query, page ->
                Pair(query, page)
            }.collect { (query, page) ->
                loadBooks(query, page)
            }
        }

        // Theo dõi biến động tổng số lượng sách trong DB (Cập nhật realtime khi quét file)
        @OptIn(ExperimentalCoroutinesApi::class)
        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) bookManager.getBooksCount() else bookManager.searchBooksCount(query)
            }.collect { count ->
                _totalBooksCount.value = count
                loadBooks(_searchQuery.value, _currentPage.value)
            }
        }
    }

    private suspend fun loadBooks(query: String, page: Int) {
        val offset = (page - 1) * pageSize
        val list = if (query.isBlank()) {
            bookManager.getBooksPaged(pageSize, offset)
        } else {
            bookManager.searchBooksPaged(query, pageSize, offset)
        }
        _bookList.value = list
    }

    fun searchBooks(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1 // Reset về trang 1 khi gõ từ khóa mới
    }

    fun nextPage() {
        if (_currentPage.value < totalPages.value) {
            _currentPage.value += 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 1) {
            _currentPage.value -= 1
        }
    }

    /**
     * @param clearOldData Mặc định là false để cho phép quét tiếp/quét bù.
     *                     Truyền true nếu muốn dọn sạch DB để làm mới hoàn toàn.
     */
    fun scanFolder(folderPath: String, clearOldData: Boolean = false) {
        viewModelScope.launch {
            if (clearOldData) {
                _currentPage.value = 1 // Quay về trang 1 nếu dọn dẹp mới
            }
            bookManager.startScanFolder(
                folderPath = folderPath,
                isUserInitiated = true,
                clearOldData = clearOldData
            )
        }
    }
}