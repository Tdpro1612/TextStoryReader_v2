package com.tdpro1612.textstoryreader.manager

import android.content.Context
import com.tdpro1612.textstoryreader.reader.epub.EpubUnzipper
import com.tdpro1612.textstoryreader.reader.txt.TxtExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BookCacheManager {

    private const val MAX_CACHE_SIZE_BYTES = 500 * 1024 * 1024L // 500 MB

    /**
     * Chạy dọn dẹp LRU bất đồng bộ trên Dispatchers.IO để không làm chặn UI / luồng đọc sách.
     */
    suspend fun ensureCacheSpaceAsync(context: Context) = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir ?: return@withContext

            val bookCacheFolders = cacheDir.listFiles()?.filter { file ->
                file.isDirectory && isBookCacheFolder(file.name)
            } ?: return@withContext

            // 1. Tính tổng dung lượng (Dùng fast-walk)
            var totalSize = bookCacheFolders.sumOf { calculateFolderSizeFast(it) }

            // 2. Nếu chưa vượt trần -> Thoát ngay
            if (totalSize <= MAX_CACHE_SIZE_BYTES) return@withContext

            // 3. Xóa các thư mục cũ nhất theo LRU
            val sortedFolders = bookCacheFolders.sortedBy { it.lastModified() }
            for (folder in sortedFolders) {
                if (totalSize <= MAX_CACHE_SIZE_BYTES) break
                val folderSize = calculateFolderSizeFast(folder)
                if (folder.deleteRecursively()) {
                    totalSize -= folderSize
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isBookCacheFolder(folderName: String): Boolean {
        return folderName.startsWith("epub_cache_") ||
                folderName.startsWith("txt_cache_")
    }

    /**
     * Tính dung lượng thư mục nhanh bằng cách duyệt file trực tiếp
     */
    private fun calculateFolderSizeFast(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateFolderSizeFast(file) else file.length()
        }
        return size
    }

    fun clearAllBookCaches(context: Context) {
        EpubUnzipper.clearAllEpubCache(context)
        TxtExtractor.clearAllTxtCache(context)
    }
}