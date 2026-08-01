//package com.tdpro1612.textstoryreader.reader
//
//import android.content.Context
//import android.net.Uri
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.BufferedReader
//import java.io.InputStreamReader
//import java.nio.charset.Charset
//import java.nio.charset.StandardCharsets
//import java.util.regex.Pattern
//
//class TxtContentReader : BookContentReader {
//
//    // Regex nhận diện tên chương mở rộng (Chương 1, Chapter 1, Bài 1, Hồi 1, Quyển 1, hoặc "1.", "Chương 01:")
//    private val chapterPattern = Pattern.compile(
//        "^(Chương|Thứ|Chapter|Hồi|Quyển|Tiết|Bài|Phần)\\s+\\d+.*$|^\\d+[\\.:\\-].*$",
//        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
//    )
//
//    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
//        val chapters = mutableListOf<BookChapter>()
//
//        try {
//            val charset = detectCharset(context, uri)
//            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext chapters
//            val reader = BufferedReader(InputStreamReader(inputStream, charset))
//
//            var currentChapterTitle = "Phần mở đầu"
//            val currentContent = StringBuilder()
//            var chapterIndex = 0
//
//            reader.useLines { lines ->
//                for (line in lines) {
//                    val trimmedLine = line.trim()
//                    if (chapterPattern.matcher(trimmedLine).matches()) {
//                        if (currentContent.isNotEmpty()) {
//                            chapters.add(
//                                BookChapter(
//                                    index = chapterIndex++,
//                                    title = currentChapterTitle,
//                                    content = currentContent.toString().trim()
//                                )
//                            )
//                            currentContent.clear()
//                        }
//                        currentChapterTitle = trimmedLine
//                    } else {
//                        currentContent.append(line).append("\n")
//                    }
//                }
//            }
//
//            if (currentContent.isNotEmpty()) {
//                chapters.add(
//                    BookChapter(
//                        index = chapterIndex,
//                        title = currentChapterTitle,
//                        content = currentContent.toString().trim()
//                    )
//                )
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        return@withContext chapters
//    }
//
//    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String {
//        // Trả về nội dung chương đã phân tách
//        return if (chapter.content.isNotBlank()) {
//            chapter.content
//        } else {
//            "Không thể tải nội dung chương này."
//        }
//    }
//
//    /**
//     * Tự động phát hiện mã hóa Encoding của file TXT (Tránh lỗi font vỡ chữ Tiếng Việt)
//     */
//    private fun detectCharset(context: Context, uri: Uri): Charset {
//        return try {
//            val inputStream = context.contentResolver.openInputStream(uri) ?: return StandardCharsets.UTF_8
//            val buffer = ByteArray(4096)
//            val read = inputStream.read(buffer)
//            inputStream.close()
//
//            if (read >= 2) {
//                // Kiểm tra Byte Order Mark (BOM) của UTF-16
//                if (buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte()) return Charset.forName("UTF-16LE")
//                if (buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte()) return Charset.forName("UTF-16BE")
//            }
//            StandardCharsets.UTF_8
//        } catch (e: Exception) {
//            StandardCharsets.UTF_8
//        }
//    }
//}