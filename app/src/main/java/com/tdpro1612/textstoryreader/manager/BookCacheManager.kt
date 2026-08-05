package com.tdpro1612.textstoryreader.manager

import android.content.Context
import com.tdpro1612.textstoryreader.reader.epub.EpubUnzipper
import com.tdpro1612.textstoryreader.reader.txt.TxtExtractor
import java.io.File

object BookCacheManager {

    private const val MAX_CACHE_SIZE_BYTES = 1024 * 1024 * 1024L // Giới hạn 1GB

    /**
     * Kiểm tra và dọn dẹp cache theo thuật toán LRU (Cũ nhất xóa trước)
     * Đảm bảo tổng dung lượng cache sách luôn <= 1 GB.
     */
    fun ensureCacheSpace(context: Context) {
        try {
            val cacheDir = context.cacheDir ?: return

            // Lấy toàn bộ thư mục cache của các định dạng (epub_cache_, txt_cache_, pdf_cache_...)
            val bookCacheFolders = cacheDir.listFiles()?.filter { file ->
                file.isDirectory && isBookCacheFolder(file.name)
            } ?: return

            // 1. Tính tổng dung lượng tất cả thư mục cache hiện có
            var totalSize = bookCacheFolders.sumOf { calculateFolderSize(it) }

            // 2. Nếu chưa vượt trần -> Bỏ qua
            if (totalSize <= MAX_CACHE_SIZE_BYTES) return

            // 3. Sắp xếp theo thời gian truy cập gần nhất (Cũ nhất đứng đầu)
            val sortedFolders = bookCacheFolders.sortedBy { it.lastModified() }

            // 4. Xóa dần các thư mục lâu không đọc đến cho tới khi đạt mức an toàn
            for (folder in sortedFolders) {
                if (totalSize <= MAX_CACHE_SIZE_BYTES) break
                val folderSize = calculateFolderSize(folder)
                if (folder.deleteRecursively()) {
                    totalSize -= folderSize
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Nhận diện thư mục cache của các định dạng sách trong app
     */
    private fun isBookCacheFolder(folderName: String): Boolean {
        return folderName.startsWith("epub_cache_") ||
                folderName.startsWith("txt_cache_") ||
                folderName.startsWith("pdf_cache_") ||
                folderName.startsWith("mobi_cache_")
    }

    /**
     * Tính tổng dung lượng file bên trong một thư mục
     */
    private fun calculateFolderSize(folder: File): Long {
        if (!folder.exists()) return 0L
        return folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /**
     * Xóa sạch toàn bộ cache của app (Dùng khi người dùng chủ động xóa cache trong Cài đặt)
     */
    fun clearAllBookCaches(context: Context) {
        EpubUnzipper.clearAllEpubCache(context)
        TxtExtractor.clearAllTxtCache(context)
    }
}