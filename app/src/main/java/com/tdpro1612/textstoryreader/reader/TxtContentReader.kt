package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class TxtContentReader : BookContentReader {

    private val chapterPattern = Pattern.compile(
        "^(Chương|Thứ|Chapter|Hồi|Quyển|Tiết|Bài|Phần)\\s+\\d+.*$|^\\d+[\\.:\\-].*$",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )

    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<BookChapter>()

        try {
            val fullText = readFullText(context, uri)
            if (fullText.isBlank()) return@withContext chapters

            val matcher = chapterPattern.matcher(fullText)
            var chapterIndex = 0
            var lastStartOffset = 0
            var lastTitle = "Phần mở đầu"

            while (matcher.find()) {
                val matchStart = matcher.start()

                if (matchStart > lastStartOffset) {
                    chapters.add(
                        BookChapter(
                            index = chapterIndex++,
                            title = lastTitle,
                            path = "",
                            path_next = "",
                            startCharOffset = lastStartOffset,
                            endCharOffset = matchStart
                        )
                    )
                }

                lastTitle = matcher.group().trim()
                lastStartOffset = matchStart
            }

            if (lastStartOffset < fullText.length) {
                chapters.add(
                    BookChapter(
                        index = chapterIndex,
                        title = lastTitle,
                        path = "",
                        path_next = "",
                        startCharOffset = lastStartOffset,
                        endCharOffset = fullText.length
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext chapters
    }

    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String = withContext(Dispatchers.IO) {
        try {
            val fullText = readFullText(context, uri)
            if (fullText.isBlank()) return@withContext "Không thể đọc dữ liệu tệp văn bản."

            val start = chapter.startCharOffset.coerceIn(0, fullText.length)
            val end = if (chapter.endCharOffset in (start + 1)..fullText.length) {
                chapter.endCharOffset
            } else {
                fullText.length
            }

            return@withContext fullText.substring(start, end).trim()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Không thể tải nội dung chương này."
        }
    }

    private fun readFullText(context: Context, uri: Uri): String {
        val charset = detectCharset(context, uri)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
        val reader = BufferedReader(InputStreamReader(inputStream, charset))
        return reader.use { it.readText() }
    }

    private fun detectCharset(context: Context, uri: Uri): Charset {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return StandardCharsets.UTF_8
            val buffer = ByteArray(4096)
            val read = inputStream.read(buffer)
            inputStream.close()

            if (read >= 2) {
                if (buffer[0] == 0xFF.toByte() && buffer[1] == 0xFE.toByte()) return Charset.forName("UTF-16LE")
                if (buffer[0] == 0xFE.toByte() && buffer[1] == 0xFF.toByte()) return Charset.forName("UTF-16BE")
            }
            StandardCharsets.UTF_8
        } catch (e: Exception) {
            StandardCharsets.UTF_8
        }
    }
}