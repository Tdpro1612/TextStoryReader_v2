package com.tdpro1612.textstoryreader.scanner

import com.tdpro1612.textstoryreader.scanner.parsers.BookHeaderParser
import com.tdpro1612.textstoryreader.scanner.parsers.EpubHeaderParser
import com.tdpro1612.textstoryreader.scanner.parsers.TxtHeaderParser
import java.io.File

object BookParserFactory {

    // Danh sách các Parser được ứng dụng hỗ trợ
    private val parsers: List<BookHeaderParser> = listOf(
        TxtHeaderParser(),
        EpubHeaderParser()
        // SAU NÀY BẠN CHỈ CẦN THÊM VÀO ĐÂY:
        // PrcHeaderParser(),
        // MobiHeaderParser()
    )

    /**
     * Trả về tập hợp tất cả đuôi file được hỗ trợ (.txt, .epub...)
     */
    fun getSupportedExtensions(): Set<String> {
        return parsers.flatMap { it.supportedExtensions }.map { it.uppercase() }.toSet()
    }

    /**
     * Tìm Parser tương ứng dựa theo đuôi file
     */
    fun getParser(file: File): BookHeaderParser? {
        val ext = file.extension.uppercase()
        return parsers.firstOrNull { it.supportedExtensions.contains(ext) }
    }
}