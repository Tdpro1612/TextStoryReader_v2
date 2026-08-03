package com.tdpro1612.textstoryreader.scanner

object BookParserFactory {

    // Tập hợp các đuôi file được ứng dụng hỗ trợ
    private val SUPPORTED_EXTENSIONS = setOf("EPUB", "TXT", "PRC")

    /**
     * Trả về tập hợp tất cả đuôi file được hỗ trợ (.EPUB, .TXT, .PRC)
     */
    fun getSupportedExtensions(): Set<String> {
        return SUPPORTED_EXTENSIONS
    }
}