package com.tdpro1612.textstoryreader.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 1. Thông tin cơ bản
    val title: String,                  // Tên sách
    val author: String = "Không rõ",    // Tác giả
    val filePath: String,               // Đường dẫn file (.txt / .epub)
    val fileType: String,               // "TXT" hoặc "EPUB"
    val fileSize: Long = 0,             // Dung lượng file (Bytes)
    val lastModified: Long,             // Kiểm tra sách được tạo lúc nào
    val coverPath: String? = null,      // Đường dẫn ảnh bìa (nếu có)

    // 2. Phân loại & Trạng thái
    val tags: String = "",              // "Tiên Hiệp, Huyền Huyễn"
    val status: BookStatus = BookStatus.UNKNOWN, // UNKNOWN, COMPLETED, ONGOING, PAUSED

    // 3. Tiến độ đọc
    val lastChapterIndex: Int = 0,      // Index chương đang đọc dở (0, 1, 2...)
    val lastPosition: Int = 0,          // Vị trí/Con trỏ dòng trong chương
    val readProgress: Float = 0f,       // % Tiến độ đọc

    // 4. Lịch sử & Yêu thích (Phục vụ UI 1, UI 6, UI 7)
    val addedTime: Long = System.currentTimeMillis(),
    val lastReadTime: Long = 0,         // Phục vụ UI 7 (Lịch sử 10 truyện gần nhất)
    val isFavorite: Boolean = false
)