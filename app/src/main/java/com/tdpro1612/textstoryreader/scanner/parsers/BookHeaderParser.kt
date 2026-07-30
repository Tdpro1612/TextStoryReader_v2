package com.tdpro1612.textstoryreader.scanner.parsers

import com.tdpro1612.textstoryreader.database.BookStatus
import android.content.Context
import android.net.Uri


data class ParsedMetadata(
    val title: String,
    val author: String = "Không rõ",
    val tags: String = "",
    val status: BookStatus = BookStatus.UNKNOWN
)

interface BookHeaderParser {
    val supportedExtensions: Set<String>

    // Đọc header bằng Context và Uri (Chuẩn SAF Android 14)
    suspend fun parseHeader(context: Context, uri: Uri): ParsedMetadata
}