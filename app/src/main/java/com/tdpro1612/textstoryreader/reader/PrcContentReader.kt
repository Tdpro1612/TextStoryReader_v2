package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.regex.Pattern

class PrcContentReader : BookContentReader {

    private val chapterPattern = Pattern.compile(
        "^(Chương|Thứ|Chapter|Hồi|Quyển|Tiết|Bài|Phần)\\s+\\d+.*$|^\\d+[\\.:\\-].*$",
        Pattern.CASE_INSENSITIVE or Pattern.MULTILINE
    )

    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<BookChapter>()

        try {
            val fullText = extractPrcText(context, uri)
            if (fullText.isBlank()) return@withContext chapters

            val lines = fullText.split("\n")
            var currentChapterTitle = "Phần mở đầu"
            val currentContent = StringBuilder()
            var chapterIndex = 0

            for (line in lines) {
                val trimmedLine = line.trim()
                if (chapterPattern.matcher(trimmedLine).matches()) {
                    if (currentContent.isNotEmpty()) {
                        chapters.add(
                            BookChapter(
                                index = chapterIndex++,
                                title = currentChapterTitle,
                                content = currentContent.toString().trim()
                            )
                        )
                        currentContent.clear()
                    }
                    currentChapterTitle = trimmedLine
                } else {
                    currentContent.append(line).append("\n")
                }
            }

            if (currentContent.isNotEmpty()) {
                chapters.add(
                    BookChapter(
                        index = chapterIndex,
                        title = currentChapterTitle,
                        content = currentContent.toString().trim()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext chapters
    }

    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String {
        return if (chapter.content.isNotBlank()) {
            chapter.content
        } else {
            "Không thể tải nội dung chương này."
        }
    }

    /**
     * Bóc tách toàn bộ Text thô từ file PRC (PDB Format + PalmDOC Uncompressing)
     */
    private fun extractPrcText(context: Context, uri: Uri): String {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val bytes = inputStream.readBytes()
            if (bytes.size < 78) return ""

            val buffer = ByteBuffer.wrap(bytes)

            // Đọc số lượng record từ PalmDB Header (Offset 76, 2 bytes Big-Endian)
            val numRecords = buffer.getShort(76).toInt() and 0xFFFF
            if (numRecords <= 1) return ""

            // Lấy offset của Record 0 (chứa Metadata) và các Record dữ liệu
            val recordOffsets = IntArray(numRecords)
            for (i in 0 until numRecords) {
                recordOffsets[i] = buffer.getInt(78 + i * 8)
            }

            // Đọc Record 0 Header (MOBI Header)
            val rec0Offset = recordOffsets[0]
            val compression = buffer.getShort(rec0Offset).toInt() and 0xFFFF
            val textLength = buffer.getInt(rec0Offset + 4)
            val recordCount = buffer.getShort(rec0Offset + 8).toInt() and 0xFFFF

            val outStringBuilder = StringBuilder()

            // Duyệt qua các Record chứa text (từ Record 1 tới recordCount)
            val maxTextRecords = minOf(recordCount, numRecords - 1)
            for (i in 1..maxTextRecords) {
                val start = recordOffsets[i]
                val end = if (i < numRecords - 1) recordOffsets[i + 1] else bytes.size
                val recordData = bytes.copyOfRange(start, end)

                when (compression) {
                    1 -> { // Uncompressed (Raw Text)
                        outStringBuilder.append(String(recordData, Charset.forName("UTF-8")))
                    }
                    2 -> { // PalmDOC LZ77 Compressed
                        val decompressed = decompressPalmDoc(recordData)
                        outStringBuilder.append(String(decompressed, Charset.forName("UTF-8")))
                    }
                    else -> {
                        // Nén mã hóa DRM hoặc Huff/CDIC phức tạp hơn
                    }
                }
            }

            return outStringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        } finally {
            inputStream?.close()
        }
    }

    /**
     * Thuật toán giải nén PalmDOC LZ77
     */
    private fun decompressPalmDoc(src: ByteArray): ByteArray {
        val out = ArrayList<Byte>()
        var i = 0

        while (i < src.size) {
            val c = src[i].toInt() and 0xFF
            i++

            if (c in 1..8) {
                // Copy c bytes tiếp theo
                for (j in 0 until c) {
                    if (i < src.size) {
                        out.add(src[i])
                        i++
                    }
                }
            } else if (c <= 0x7F) {
                // Byte bình thường
                out.add(c.toByte())
            } else if (c >= 0xC0) {
                // Space + character
                out.add(' '.code.toByte())
                out.add((c xor 0x80).toByte())
            } else {
                // Sliding window copy (LZ77 back-reference)
                if (i < src.size) {
                    val c2 = src[i].toInt() and 0xFF
                    i++
                    val distance = (((c and 0x3F) shl 5) or (c2 shr 3))
                    val length = (c2 and 0x07) + 3

                    val startPos = out.size - distance
                    if (startPos >= 0) {
                        for (j in 0 until length) {
                            out.add(out[startPos + j])
                        }
                    }
                }
            }
        }

        return out.toByteArray()
    }
}