package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri

interface BookContentReader {
    /**
     * Lấy danh sách các chương trong file sách
     */
    suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter>

    /**
     * Lấy nội dung văn bản của một chương cụ thể
     */
    suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String
}