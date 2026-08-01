package com.tdpro1612.textstoryreader.reader

data class BookChapter(
    val index: Int,
    val title: String,
    val path: String = "",
    val path_next: String = "",
    val startCharOffset: Int = 0,
    val endCharOffset: Int = -1
)