package com.tdpro1612.textstoryreader.database


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE // Xóa sách thì tự xóa luôn bookmark của sách đó
        )
    ]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val bookId: Int,                // Nối với ID của cuốn sách
    val chapterIndex: Int,          // Lưu vị trí chương
    val position: Int,              // Lưu vị trí dòng/con trỏ trong chương
    val noteSnippet: String,        // Đoạn văn ngắn được lưu lại làm ghi nhớ
    val createdTime: Long = System.currentTimeMillis() // Ngày giờ tạo bookmark
)