package com.tdpro1612.textstoryreader.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    // 1. Bổ sung indices để tăng tốc truy vấn theo bookId
    indices = [Index(value = ["bookId"])],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,                // Nối với ID của cuốn sách
    val chapterIndex: Int,          // Vị trí chương (0, 1, 2...)
    val chapterTitle: String = "",  // ✨ Thêm tên chương để hiển thị lên UI cho nhanh
    val position: Int,              // Vị trí dòng / ký tự / trượt trong chương
    val noteSnippet: String,        // Đoạn văn ngắn được trích dẫn/ghi chú
    val createdTime: Long = System.currentTimeMillis() // Thời gian tạo
)