package com.tdpro1612.textstoryreader.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDatabaseQueries {

    // =========================================================================
    // 1. NHÓM QUÉT & THÊM SÁCH (SCAN & IMPORT)
    // =========================================================================

    @Query("SELECT * FROM books")
    suspend fun getAllBooksForScan(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity): Long

    // 🔥 MỚI: Xóa toàn bộ dữ liệu sách cũ (Dùng khi chọn quét thư mục mới)
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()


    // =========================================================================
    // 2. NHÓM THƯ VIỆN & PHÂN TRANG (LIBRARY & PAGINATION - UI 1)
    // =========================================================================

    @Query("SELECT * FROM books ORDER BY addedTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPaged(limit: Int, offset: Int): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPagedAZ(limit: Int, offset: Int): List<BookEntity>

    @Query("SELECT * FROM books ORDER BY lastReadTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPagedRecent(limit: Int, offset: Int): List<BookEntity>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getBooksByStatusPaged(status: BookStatus, limit: Int, offset: Int): List<BookEntity>

    /**
     * Đếm tổng số sách hiện có trong Thư viện (Dùng để tính tổng số trang).
     */
    @Query("SELECT COUNT(*) FROM books")
    fun getBooksCount(): Flow<Int>


    // =========================================================================
    // 3. NHÓM LỊCH SỬ ĐỌC (HISTORY - UI 7)
    // =========================================================================

    @Query("SELECT * FROM books WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<BookEntity>>

    @Query("UPDATE books SET lastReadTime = 0 WHERE id = :bookId")
    suspend fun clearHistoryForBook(bookId: Int)

    @Query("UPDATE books SET lastReadTime = 0")
    suspend fun clearAllHistory()


    // =========================================================================
    // 4. NHÓM TÌM KIẾM & LỌC NÂNG CAO (SEARCH & FILTER - UI 8)
    // =========================================================================

    @Query("""
        SELECT * FROM books 
        WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
        ORDER BY title ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchBooksPaged(query: String, limit: Int, offset: Int): List<BookEntity>

    // 🔥 MỚI: Đếm tổng số sách khớp với từ khóa tìm kiếm (Dùng tính số trang khi Search)
    @Query("""
        SELECT COUNT(*) FROM books 
        WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
    """)
    fun searchBooksCount(query: String): Flow<Int>

    @Query("""
        SELECT * FROM books 
        WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
          AND status = :status
        ORDER BY title ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchBooksWithStatusPaged(
        query: String,
        status: BookStatus,
        limit: Int,
        offset: Int
    ): List<BookEntity>


    // =========================================================================
    // 5. NHÓM CẬP NHẬT TIẾN ĐỘ & CHI TIẾT SÁCH (UPDATE & DETAILS)
    // =========================================================================

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Int): BookEntity?

    @Query("""
        UPDATE books 
        SET lastChapterIndex = :chapterIndex,
            lastPosition = :position,
            readProgress = :progress,
            lastReadTime = :readTime
        WHERE id = :bookId
    """)
    suspend fun updateReadingProgress(
        bookId: Int,
        chapterIndex: Int,
        position: Int,
        progress: Float,
        readTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Delete
    suspend fun deleteMultipleBooks(books: List<BookEntity>)


    // =========================================================================
    // 6. NHÓM QUẢN LÝ DẤU TRANG (BOOKMARKS - UI 6)
    // =========================================================================

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdTime DESC")
    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllBookmarksOfBook(bookId: Int)

    // 🔥 MỚI: Xóa toàn bộ bookmark cũ khi dọn dẹp database
    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()
}