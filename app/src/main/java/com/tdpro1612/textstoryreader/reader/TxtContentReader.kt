package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class TxtContentReader : BookContentReader {

    // 🔥 Regex linh hoạt: Khớp cả "Chương 1", "Chương : 1", "Chương:1", "Chương - 1"
    private val chapterPatterns = listOf(
        Regex("""(?i)^\s*(Chương|Chapter|Quyển|Tập|Hồi|Bài|Phần)\s*[:\-]?\s*[0-9IVXLCDMivxlcdm]+.*"""),
        Regex("""(?i)^\s*Thứ\s+[0-9IVXLCDMivxlcdm]+\s+Chương.*""")
    )

    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<BookChapter>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var line: String?
            var currentCharOffset = 0
            var currentChapterTitle = "Mở đầu"
            var startCharOffset = 0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                val lineLengthWithNewline = currentLine.length + 1

                if (isChapterHeader(currentLine)) {
                    if (currentCharOffset > startCharOffset) {
                        chapters.add(
                            BookChapter(
                                index = chapters.size,
                                title = currentChapterTitle,
                                startCharOffset = startCharOffset,
                                endCharOffset = currentCharOffset
                            )
                        )
                    }
                    currentChapterTitle = currentLine.trim()
                    startCharOffset = currentCharOffset
                }

                currentCharOffset += lineLengthWithNewline
            }

            // Thêm chương cuối cùng
            if (currentCharOffset > startCharOffset) {
                chapters.add(
                    BookChapter(
                        index = chapters.size,
                        title = currentChapterTitle,
                        startCharOffset = startCharOffset,
                        endCharOffset = currentCharOffset
                    )
                )
            }
        }

        if (chapters.isEmpty()) {
            return@withContext fallbackChunking(context, uri)
        }

        return@withContext chapters
    }

    override suspend fun getChapterContent(
        context: Context,
        uri: Uri,
        chapter: BookChapter
    ): String = withContext(Dispatchers.IO) {
        val contentBuilder = StringBuilder()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var currentCharOffset = 0
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                val lineLengthWithNewline = currentLine.length + 1
                val lineEndOffset = currentCharOffset + lineLengthWithNewline

                if (lineEndOffset > chapter.startCharOffset && (chapter.endCharOffset == -1 || currentCharOffset < chapter.endCharOffset)) {
                    contentBuilder.append(currentLine).append("\n")
                } else if (chapter.endCharOffset != -1 && currentCharOffset >= chapter.endCharOffset) {
                    break
                }

                currentCharOffset += lineLengthWithNewline
            }
        }

        return@withContext contentBuilder.toString().trim()
    }

    override fun clearCache(context: Context, uri: Uri) {}

    private fun isChapterHeader(line: String): Boolean {
        val cleanLine = line.replace("\uFEFF", "").trim()
        if (cleanLine.isEmpty() || cleanLine.length > 100) return false
        return chapterPatterns.any { it.matches(cleanLine) }
    }

    private fun fallbackChunking(context: Context, uri: Uri): List<BookChapter> {
        val chapters = mutableListOf<BookChapter>()
        val targetCharsPerChapter = 15000

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            var line: String?
            var currentCharOffset = 0
            var chapterStartOffset = 0
            var currentChapterCharCount = 0

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: ""
                val lineLengthWithNewline = currentLine.length + 1

                currentCharOffset += lineLengthWithNewline
                currentChapterCharCount += lineLengthWithNewline

                if (currentChapterCharCount >= targetCharsPerChapter) {
                    chapters.add(
                        BookChapter(
                            index = chapters.size,
                            title = "Phần ${chapters.size + 1}",
                            startCharOffset = chapterStartOffset,
                            endCharOffset = currentCharOffset
                        )
                    )
                    chapterStartOffset = currentCharOffset
                    currentChapterCharCount = 0
                }
            }

            if (currentCharOffset > chapterStartOffset) {
                chapters.add(
                    BookChapter(
                        index = chapters.size,
                        title = "Phần ${chapters.size + 1}",
                        startCharOffset = chapterStartOffset,
                        endCharOffset = currentCharOffset
                    )
                )
            }
        }

        return chapters
    }
}