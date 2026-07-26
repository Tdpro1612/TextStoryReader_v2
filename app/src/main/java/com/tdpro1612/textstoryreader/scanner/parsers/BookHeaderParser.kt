package com.tdpro1612.textstoryreader.scanner.parsers

import com.tdpro1612.textstoryreader.database.BookStatus
import java.io.File

data class ParsedMetadata(
    val title: String,
    val author: String = "Không rõ",
    val tags: String = "",
    val status: BookStatus = BookStatus.UNKNOWN
)

interface BookHeaderParser {
    /**
     * Trả về danh sách định dạng file mà Parser này hỗ trợ (Ví dụ: setOf("TXT"))
     */
    val supportedExtensions: Set<String>

    /**
     * Hàm đọc Header trích xuất metadata
     */
    fun parseHeader(file: File): ParsedMetadata
}