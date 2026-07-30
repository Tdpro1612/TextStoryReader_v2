package com.tdpro1612.textstoryreader.reader

data class BookChapter(
    val index: Int,              // Thứ tự chương (0, 1, 2...)
    val title: String,           // Tên chương (Ví dụ: "Chương 1: Mở đầu")
    val content: String = "",    // Nội dung chữ của chương
    val startCharOffset: Int = 0 // Vị trí ký tự bắt đầu (dùng cho TXT cắt nhanh)
)