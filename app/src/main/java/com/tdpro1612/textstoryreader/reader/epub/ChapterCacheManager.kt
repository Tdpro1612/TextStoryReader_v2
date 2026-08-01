package com.tdpro1612.textstoryreader.reader.cache

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tdpro1612.textstoryreader.reader.BookChapter
import java.io.File

class ChapterCacheManager(private val cacheFolder: File) {

    private val gson = Gson()

    /**
     * Lấy đường dẫn file Cache JSON dựa trên tên thư mục sách
     */
    private fun getCacheFile(bookId: String): File {
        val safeFileName = bookId.replace(Regex("""[^a-zA-Z0-9_-]"""), "_")
        return File(cacheFolder, "toc_cache_$safeFileName.json")
    }

    /**
     * Đọc danh sách Chương từ Cache (Nếu có)
     */
    fun getCachedChapters(bookId: String): List<BookChapter>? {
        val cacheFile = getCacheFile(bookId)
        if (!cacheFile.exists() || cacheFile.length() == 0L) return null

        return try {
            val json = cacheFile.readText(Charsets.UTF_8)
            val type = object : TypeToken<List<BookChapter>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lưu danh sách Chương vào Cache JSON
     */
    fun saveChaptersToCache(bookId: String, chapters: List<BookChapter>) {
        if (chapters.isEmpty()) return
        try {
            val cacheFile = getCacheFile(bookId)
            val json = gson.toJson(chapters)
            cacheFile.writeText(json, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Xóa cache của 1 cuốn sách (khi người dùng nhấn Reload/Refresh mục lục)
     */
    fun clearCache(bookId: String) {
        val cacheFile = getCacheFile(bookId)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }
}