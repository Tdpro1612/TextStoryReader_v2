package com.tdpro1612.textstoryreader.manager

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import com.tdpro1612.textstoryreader.database.AppDatabase
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.database.BookmarkEntity
import com.tdpro1612.textstoryreader.database.ChapterEntity
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.reader.ReaderFactory
import com.tdpro1612.textstoryreader.scanner.BookScanManager
import com.tdpro1612.textstoryreader.scanner.ScanProgressState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    /**
     * Lấy danh sách chương của sách. Ưu tiên đọc từ Cache DB (Room).
     * Nếu DB chưa có thì parse từ đĩa qua Reader và lưu vào DB.
     */
    suspend fun getChapterList(bookUri: Uri, bookId: Int): List<BookChapter> {
        Log.d("BookManager", "getChapterList -> bookUri = $bookUri, bookId = $bookId")

        // 1. Kiểm tra cache trong Room DB
        val cachedChapters = bookQueries.getChaptersByBookId(bookId)
        if (cachedChapters.isNotEmpty()) {
            Log.d("BookManager", "⚡ [CACHE HIT] Lấy ${cachedChapters.size} chương từ Database cho bookId = $bookId")
            return cachedChapters.map { entity ->
                BookChapter(
                    index = entity.chapterIndex,
                    title = entity.title,
                    path = entity.path,
                    path_next = entity.pathNext ?: "",
                    startCharOffset = entity.startCharOffset,
                    endCharOffset = entity.endCharOffset
                )
            }
        }



        // 2. Nếu DB chưa có -> Parse qua Reader
        Log.d("BookManager", "🐢 [CACHE MISS] Parse trực tiếp từ file qua Reader cho bookId = $bookId")
        val reader = ReaderFactory.getReader(bookUri)
        val parsedChapters = reader.getChapterList(context, bookUri)
        // 3. Sau khi xả nén/parse xong -> Bắn 1 request chạy ngầm dọn dẹp cache cũ nếu vượt 500MB
        // Dùng CoroutineScope(Dispatchers.IO) ở đây rất gọn và nằm đúng tầng Manager
        CoroutineScope(Dispatchers.IO).launch {
        BookCacheManager.ensureCacheSpaceAsync(context)
        }
        // 4. Lưu vào DB để cache cho các lần sau
        if (parsedChapters.isNotEmpty()) {
            val entities = parsedChapters.map { chapter ->
                ChapterEntity(
                    id = 0, // Primary Key autoGenerate
                    bookId = bookId,
                    chapterIndex = chapter.index,
                    title = chapter.title,
                    path = chapter.path,
                    pathNext = chapter.path_next,
                    startCharOffset = chapter.startCharOffset,
                    endCharOffset = chapter.endCharOffset
                )
            }
            bookQueries.deleteChaptersByBookId(bookId)
            bookQueries.insertChapters(entities)
            Log.d("BookManager", "💾 Đã lưu ${parsedChapters.size} chương vào Database cho bookId = $bookId")
        }

        return parsedChapters
    }

    suspend fun getChapterContent(bookUri: Uri, chapter: BookChapter): String {
        val reader = ReaderFactory.getReader(bookUri)
        return reader.getChapterContent(context, bookUri, chapter)
    }

    // Xóa cache riêng cho từng cuốn sách
    fun clearCache(bookUri: Uri) {
        val reader = ReaderFactory.getReader(bookUri)
        reader.clearCache(context, bookUri)
    }

    // Thêm vào trong class BookManager
    suspend fun deleteBook(book: BookEntity, deletePhysicalFile: Boolean = true) {
        // 1. Xóa FILE VẬT LÝ trên đĩa
        if (deletePhysicalFile) {
            val isDeleted = deleteFileFromStorage(context, book.filePath)
            if (isDeleted) {
                Log.d("BookManager", "🗑️ ĐÃ XÓA FILE VẬT LÝ THÀNH CÔNG: ${book.filePath}")
            } else {
                Log.e("BookManager", "❌ XÓA FILE VẬT LÝ THẤT BẠI: ${book.filePath}")
            }
        }

        // 2. Xóa dữ liệu trong Room DB dựa theo id
        bookQueries.deleteBook(book)
        bookQueries.deleteChaptersByBookId(book.id)
        bookQueries.deleteAllBookmarksOfBook(book.id)
    }

    /**
     * Hàm hỗ trợ xóa file vật lý "cân" mọi loại đường dẫn
     */
    private fun deleteFileFromStorage(context: Context, filePath: String): Boolean {
        return try {
            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)

                // Cách 1: Dùng DocumentsContract xóa trực tiếp qua SAF (Độ tin cậy cao nhất)
                val isDeleted = try {
                    DocumentsContract.deleteDocument(context.contentResolver, uri)
                } catch (e: Exception) {
                    false
                }

                if (isDeleted) return true

                // Cách 2: Fallback qua DocumentFile nếu cách 1 trượt
                val documentFile = DocumentFile.fromSingleUri(context, uri)
                return documentFile?.delete() == true

            } else {
                // Trường hợp đường dẫn File đĩa (/storage/emulated/0/...)
                val file = java.io.File(filePath)
                if (file.exists()) {
                    file.delete()
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("BookManager", "Ngoại lệ khi xóa file vật lý: ${e.message}", e)
            false
        }
    }
}