package com.tdpro1612.textstoryreader.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDatabaseQueries {

    // =========================================================================
    // 1. NHÓM QUÉT & THÊM SÁCH (SCAN & IMPORT)
    // =========================================================================

    // Lấy toàn bộ danh sách BookEntity (hoặc chỉ lấy id, path, size, lastModified)
    // để Map sẵn trên RAM phục vụ việc so sánh 18k file
    @Query("SELECT * FROM books")
    suspend fun getAllBooksForScan(): List<BookEntity>

    /**
     * Thêm hàng loạt sách mới vào Database (Bulk/Batch Insert).
     * OnConflictStrategy.IGNORE: Nếu file đã tồn tại thì bỏ qua, không đè lên.
     * Lưu 500 - 1000 cuốn một lượt chỉ mất vài miligiây.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBooks(books: List<BookEntity>)

    /**
     * Thêm 1 cuốn sách duy nhất (Dùng cho trường hợp người dùng thêm file lẻ thủ công).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity): Long


    // =========================================================================
    // 2. NHÓM THƯ VIỆN & PHÂN TRANG (LIBRARY & PAGINATION - UI 1)
    // =========================================================================

    /**
     * Lấy danh sách sách phân trang (Mặc định: Mới thêm lên đầu).
     * @param limit Số lượng sách lấy trong 1 trang (Ví dụ: 50 cuốn)
     * @param offset Số lượng sách bỏ qua (Trang 1 -> offset 0, Trang 2 -> offset 50...)
     */
    @Query("SELECT * FROM books ORDER BY addedTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPaged(limit: Int, offset: Int): List<BookEntity>

    /**
     * Lấy danh sách sách phân trang + SẮP XẾP THEO TÊN (A - Z).
     */
    @Query("SELECT * FROM books ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPagedAZ(limit: Int, offset: Int): List<BookEntity>

    /**
     * Lấy danh sách sách phân trang + SẮP XẾP THEO MỚI ĐỌC GẦN NHẤT.
     */
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getBooksPagedRecent(limit: Int, offset: Int): List<BookEntity>

    /**
     * Lấy danh sách sách phân trang + LỌC THEO TRẠNG THÁI (Đang ra, Full, Pause...).
     */
    @Query("SELECT * FROM books WHERE status = :status ORDER BY title ASC LIMIT :limit OFFSET :offset")
    suspend fun getBooksByStatusPaged(status: BookStatus, limit: Int, offset: Int): List<BookEntity>

    /**
     * Đếm tổng số sách hiện có trong Thư viện (Dùng để tính tổng số trang).
     */
    @Query("SELECT COUNT(*) FROM books")
    fun getTotalBookCount(): Flow<Int>


    // =========================================================================
    // 3. NHÓM LỊCH SỬ ĐỌC (HISTORY - UI 7)
    // =========================================================================

    /**
     * Lấy danh sách sách đọc gần đây nhất (Top 10 / Top 20) phục vụ UI 7 Lịch sử.
     * Flow giúp UI tự động cập nhật ngay lập tức khi người dùng vừa đọc xong 1 chương.
     */
    @Query("SELECT * FROM books WHERE lastReadTime > 0 ORDER BY lastReadTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 20): Flow<List<BookEntity>>

    /**
     * Xóa 1 cuốn sách cụ thể khỏi danh sách Lịch sử đọc (đưa lastReadTime về 0).
     * Cuốn sách vẫn được giữ lại trong Thư viện chính.
     */
    @Query("UPDATE books SET lastReadTime = 0 WHERE id = :bookId")
    suspend fun clearHistoryForBook(bookId: Int)

    /**
     * Reset toàn bộ Lịch sử đọc (Xóa lịch sử nhưng giữ lại sách trong Thư viện).
     */
    @Query("UPDATE books SET lastReadTime = 0")
    suspend fun clearAllHistory()


    // =========================================================================
    // 4. NHÓM TÌM KIẾM & LỌC NÂNG CAO (SEARCH & FILTER - UI 8)
    // =========================================================================

    /**
     * Tìm kiếm sách theo từ khóa (Tìm theo Tên sách HOẶC Tác giả), có Phân trang.
     * @param query Từ khóa người dùng nhập (Cần truyền dạng "%từ_khóa%")
     */
    @Query("""
        SELECT * FROM books 
        WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
        ORDER BY title ASC 
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchBooksPaged(query: String, limit: Int, offset: Int): List<BookEntity>

    /**
     * Tìm kiếm nâng cao: Từ khóa + Lọc theo Trạng thái + Phân trang.
     */
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

    /**
     * Lấy chi tiết 1 cuốn sách theo ID (Để mở vào màn hình Đọc sách).
     */
    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getBookById(bookId: Int): BookEntity?

    /**
     * Cập nhật tiến độ đọc khi người dùng chuyển chương hoặc đóng app.
     */
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

    /**
     * Bật / Tắt Yêu thích (Favorite).
     */
    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavoriteStatus(bookId: Int, isFavorite: Boolean)

    /**
     * Cập nhật toàn bộ thông tin sách (Dùng khi người dùng sửa Tên, Tác giả, Tag thủ công).
     */
    @Update
    suspend fun updateBook(book: BookEntity)

    /**
     * Xóa 1 cuốn sách khỏi Thư viện.
     */
    @Delete
    suspend fun deleteBook(book: BookEntity)

    /**
     * Xóa hàng loạt sách được chọn (Dùng cho tính năng Chế độ chọn nhiều để xóa).
     */
    @Delete
    suspend fun deleteMultipleBooks(books: List<BookEntity>)


    // =========================================================================
    // 6. NHÓM QUẢN LÝ DẤU TRANG (BOOKMARKS - UI 6)
    // =========================================================================

    /**
     * Lấy tất cả Dấu trang của 1 cuốn sách cụ thể (Sắp xếp mới nhất lên đầu).
     * Dùng Flow để Màn hình Bookmark tự cập nhật实时 khi bấm xóa/thêm.
     */
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdTime DESC")
    fun getBookmarksForBook(bookId: Int): Flow<List<BookmarkEntity>>

    /**
     * Thêm 1 Dấu trang mới (Bookmark).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    /**
     * Xóa 1 Dấu trang cụ thể.
     */
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    /**
     * Xóa tất cả Dấu trang của 1 cuốn sách.
     */
    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllBookmarksOfBook(bookId: Int)
}