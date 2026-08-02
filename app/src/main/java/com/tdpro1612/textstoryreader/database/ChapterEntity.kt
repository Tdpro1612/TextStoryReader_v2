package com.tdpro1612.textstoryreader.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "book_chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bookId"])]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int, // Giả định ID của sách trong BookEntity là Int
    val chapterIndex: Int,
    val title: String,
    val path: String,
    val pathNext: String,
    val startCharOffset: Int,
    val endCharOffset: Int
)