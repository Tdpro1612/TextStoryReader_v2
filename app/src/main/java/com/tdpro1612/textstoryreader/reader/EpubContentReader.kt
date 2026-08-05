package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tdpro1612.textstoryreader.reader.epub.EpubFileReader
import com.tdpro1612.textstoryreader.reader.epub.EpubUnzipper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class EpubContentReader : BookContentReader {

    /**
     * Lấy thư mục Cache của sách.
     * Nếu thư mục chưa tồn tại hoặc bị Android xóa khi tắt app -> Tự động giải nén lại.
     */
    // Cache folder ngay trên RAM để chuyển chương không cần check đĩa
    private var currentUri: Uri? = null
    private var cachedFolder: File? = null

    private suspend fun ensureCacheFolder(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        // ⚡ 1. Nếu đang đọc cùng 1 cuốn sách và RAM đã có sẵn reference -> Trả về ngay (0 ms)
        if (currentUri == uri && cachedFolder?.exists() == true) {
            return@withContext cachedFolder!!
        }

        val folderName = "epub_cache_" + uri.toString().hashCode()
        val cacheFolder = File(context.cacheDir, folderName)

        // ⚡ 2. CHỈ CHECK THƯ MỤC TỒN TẠI (KHÔNG dùng listFiles() để tránh quét 6.700+ file)
        if (cacheFolder.exists()) {
            currentUri = uri
            cachedFolder = cacheFolder
            return@withContext cacheFolder // Chạy dưới < 1 ms
        }

        // 3. Nếu chưa có (sách mới mở lần đầu) -> Tiến hành Unzip
        val unzippedFolder = EpubUnzipper.unzipEpubToCache(context, uri)
        currentUri = uri
        cachedFolder = unzippedFolder
        return@withContext unzippedFolder
    }

    /**
     * 1. HÀM GET CHAPTER LIST: Đọc Bảng Mục Lục chương từ thư mục Cache đã Unzip
     */
    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val cacheFolder: File
        val ensureCacheTime = measureTimeMillis {
            cacheFolder = ensureCacheFolder(context, uri)
        }
        Log.d("ReaderPerf", "0. [getChapterList] ensureCacheFolder: $ensureCacheTime ms")

        if (!cacheFolder.exists()) return@withContext emptyList()

        val reader = EpubFileReader(context, cacheFolder)
        return@withContext reader.getChapterList()
    }

    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String = withContext(Dispatchers.IO) {
        val cacheFolder: File
        val ensureCacheTime = measureTimeMillis {
            cacheFolder = ensureCacheFolder(context, uri)
        }
        Log.d("ReaderPerf", "0. [getChapterContent] ensureCacheFolder: $ensureCacheTime ms")

        if (!cacheFolder.exists()) {
            return@withContext "Không thể giải nén hoặc đọc nội dung sách."
        }

        val content = HtmlToTextParser.parseChapter(chapter, cacheFolder)
        return@withContext if (content.isNotBlank()) content else "Nội dung chương rỗng."
    }

    /**
     * 3. HÀM clear cache folder
     */
    override fun clearCache(context: Context, uri: Uri) {
        // Tự tính toán lại vị trí folder cache theo URI
        val folderName = "epub_cache_" + uri.toString().hashCode()
        val cacheFolder = File(context.cacheDir, folderName)

        // 🔥 Gọi xóa thư mục cache của cuốn sách này
        EpubUnzipper.clearCache(cacheFolder)
    }
}