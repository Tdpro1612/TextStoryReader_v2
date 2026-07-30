package com.tdpro1612.textstoryreader.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tdpro1612.textstoryreader.database.AppDatabase
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.database.BookmarkEntity
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.reader.ReaderFactory
import com.tdpro1612.textstoryreader.scanner.BookScanManager
import com.tdpro1612.textstoryreader.scanner.ScanProgressState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class BookManager(private val context: Context) {

    private val bookQueries = AppDatabase.getInstance(context).bookQueries()
    private val scanManager = BookScanManager

    // --- 0. Quản lý Quét file (Scanner Integration) ---
    val scanState: StateFlow<ScanProgressState> = scanManager.scanState

    /**
     * @param clearOldData Nếu = true sẽ xóa sạch DB trước khi quét.
     *                     Nếu = false (Mặc định) sẽ giữ lại DB cũ để quét tiếp/cập nhật file mới.
     */
    suspend fun startScanFolder(
        folderPath: String,
        isUserInitiated: Boolean = true,
        clearOldData: Boolean = false
    ) {
        if (clearOldData) {
            bookQueries.deleteAllBookmarks() // Clear bookmark cũ
            bookQueries.deleteAllBooks()     // Clear danh sách sách cũ
        }
        scanManager.startScan(context, folderPath, isUserInitiated)
    }

    // --- 1. Quản lý Thư viện Phân trang theo Page Index (Limit / Offset) ---
    suspend fun getBooksPaged(limit: Int, offset: Int): List<BookEntity> {
        return bookQueries.getBooksPaged(limit, offset)
    }

    suspend fun searchBooksPaged(query: String, limit: Int, offset: Int): List<BookEntity> {
        return bookQueries.searchBooksPaged(query, limit, offset)
    }

    fun getBooksCount(): Flow<Int> = bookQueries.getBooksCount()

    fun searchBooksCount(query: String): Flow<Int> = bookQueries.searchBooksCount(query)

    // --- 2. Lịch sử đọc (UI 7) ---
    fun getRecentHistory(limit: Int = 20): Flow<List<BookEntity>> {
        return bookQueries.getRecentHistory(limit)
    }

    suspend fun clearHistoryForBook(bookId: Int) {
        bookQueries.clearHistoryForBook(bookId)
    }

    suspend fun clearAllHistory() {
        bookQueries.clearAllHistory()
    }

    // --- 3. Cập nhật tiến độ đọc (UI 3) ---
    suspend fun updateReadingProgress(
        bookId: Int,
        chapterIndex: Int,
        position: Int,
        progress: Float
    ) {
        bookQueries.updateReadingProgress(bookId, chapterIndex, position, progress)
    }

    // --- 4. Quản lý Dấu trang (UI 6) ---
    fun getBookmarks(bookId: Int): Flow<List<BookmarkEntity>> {
        return bookQueries.getBookmarksForBook(bookId)
    }

    suspend fun addBookmark(bookmark: BookmarkEntity) {
        bookQueries.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookQueries.deleteBookmark(bookmark)
    }

    // --- 5. Quản lý Đọc nội dung chương (Reader Integration) ---
    suspend fun getChapterList(bookUri: Uri): List<BookChapter> {
        Log.d("EPUB", "bookUri = $bookUri")
        val reader = ReaderFactory.getReader(bookUri)
        return reader.getChapterList(context, bookUri)
    }

    suspend fun getChapterContent(bookUri: Uri, chapter: BookChapter): String {
        val reader = ReaderFactory.getReader(bookUri)
        return reader.getChapterContent(context, bookUri, chapter)
    }
}