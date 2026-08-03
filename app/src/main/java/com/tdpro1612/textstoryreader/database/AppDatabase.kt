package com.tdpro1612.textstoryreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        ChapterEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Khai báo lấy ra giao diện Query
    abstract fun bookQueries(): BookDatabaseQueries

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "text_story_reader.db" // Tên file Database lưu dưới thẻ nhớ
                )
                    // .fallbackToDestructiveMigration() // Dùng khi dev để tự reset DB khi đổi phiên bản
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}