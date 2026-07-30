package com.tdpro1612.textstoryreader.scanner

import com.tdpro1612.textstoryreader.scanner.parsers.BookHeaderParser
import com.tdpro1612.textstoryreader.scanner.parsers.EpubHeaderParser
import com.tdpro1612.textstoryreader.scanner.parsers.TxtHeaderParser

object BookParserFactory {

    // Danh sách các Parser được ứng dụng hỗ trợ
    private val parsers: List<BookHeaderParser> = listOf(
        TxtHeaderParser(),
        EpubHeaderParser()
    )

    /**
     * Trả về tập hợp tất cả đuôi file được hỗ trợ (.TXT, .EPUB...)
     */
    fun getSupportedExtensions(): Set<String> {
        return parsers.flatMap { it.supportedExtensions }.map { it.uppercase() }.toSet()
    }

    /**
     * Tìm Parser tương ứng dựa theo đuôi file (String) thay vì Java File
     */
    fun getParser(extension: String): BookHeaderParser? {
        val ext = extension.uppercase()
        return parsers.firstOrNull { parser ->
            parser.supportedExtensions.any { it.equals(ext, ignoreCase = true) }
        }
    }
}