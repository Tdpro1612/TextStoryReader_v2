package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import com.tdpro1612.textstoryreader.reader.epub.EpubFileReader
import com.tdpro1612.textstoryreader.reader.epub.EpubUnzipper
import com.tdpro1612.textstoryreader.reader.epub.HtmlToTextParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EpubContentReader : BookContentReader {

    /**
     * Lấy thư mục Cache của sách.
     * Nếu thư mục chưa tồn tại hoặc bị Android xóa khi tắt app -> Tự động giải nén lại.
     */
    private suspend fun ensureCacheFolder(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        val folderName = "epub_cache_" + uri.toString().hashCode()
        val cacheFolder = File(context.cacheDir, folderName)

        // Nếu thư mục tồn tại và có chứa file bên trong thì tái sử dụng
        if (cacheFolder.exists() && cacheFolder.listFiles()?.isNotEmpty() == true) {
            return@withContext cacheFolder
        }

        // Ngược lại (mới mở app / cache bị xóa) -> Tiến hành Unzip lại
        return@withContext EpubUnzipper.unzipEpubToCache(context, uri)
    }

    /**
     * 1. HÀM GET CHAPTER LIST: Đọc Bảng Mục Lục chương từ thư mục Cache đã Unzip
     */
    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val cacheFolder = ensureCacheFolder(context, uri)
        if (!cacheFolder.exists()) return@withContext emptyList()

        val reader = EpubFileReader(context, cacheFolder)
        return@withContext reader.getChapterList()
    }

    /**
     * 2. HÀM GET CHAPTER CONTENT: Lấy nội dung chữ của chương qua HtmlToTextParser
     */
    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String = withContext(Dispatchers.IO) {
        // Tự động khôi phục cache nếu bị mất khi tắt app
        val cacheFolder = ensureCacheFolder(context, uri)

        if (!cacheFolder.exists()) {
            return@withContext "Không thể giải nén hoặc đọc nội dung sách."
        }

        val content = HtmlToTextParser.parseChapter(chapter, cacheFolder)
        return@withContext if (content.isNotBlank()) content else "Nội dung chương rỗng."
    }
}