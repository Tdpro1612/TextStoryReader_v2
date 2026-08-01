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
     * 1. HÀM UNZIP: Ủy quyền toàn bộ công việc xả nén cho EpubUnzipper
     */
    suspend fun unzipEpub(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
        return@withContext EpubUnzipper.unzipEpubToCache(context, uri)
    }

    /**
     * 2. HÀM GET CHAPTER LIST: Đọc Bảng Mục Lục chương từ thư mục Cache đã Unzip
     */
    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val cacheFolder = unzipEpub(context, uri)
        if (!cacheFolder.exists()) return@withContext emptyList()

        val reader = EpubFileReader(context, cacheFolder)
        return@withContext reader.getChapterList()
    }

    /**
     * 3. HÀM GET CHAPTER CONTENT: Lấy nội dung chữ của chương qua HtmlToTextParser
     */
    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String = withContext(Dispatchers.IO) {
        // Lấy lại thư mục Cache tương ứng với Uri của sách
        val folderName = "epub_cache_" + uri.toString().hashCode()
        val cacheFolder = File(context.cacheDir, folderName)

        if (!cacheFolder.exists()) {
            return@withContext "Thư mục sách không tồn tại hoặc đã bị xóa cache."
        }

        val content = HtmlToTextParser.parseChapter(chapter, cacheFolder)
        return@withContext if (content.isNotBlank()) content else "Nội dung chương rỗng."
    }
}