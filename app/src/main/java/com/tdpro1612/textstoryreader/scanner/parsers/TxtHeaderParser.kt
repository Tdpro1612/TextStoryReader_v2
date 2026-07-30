package com.tdpro1612.textstoryreader.scanner.parsers

import android.content.Context
import android.net.Uri
import com.tdpro1612.textstoryreader.database.BookStatus
import java.io.BufferedReader
import java.io.InputStreamReader

class TxtHeaderParser : BookHeaderParser {
    override val supportedExtensions: Set<String> = setOf("TXT")

    override suspend fun parseHeader(context: Context, uri: Uri): ParsedMetadata {
        var title = ""
        var author = "Không rõ"
        var tags = ""
        var status = BookStatus.UNKNOWN

        try {
            // 🔥 Dùng BufferedReader đọc từng dòng, đủ 10 dòng là tự ngắt ngay lập tức
            context.contentResolver.openInputStream(uri)?.use { rawStream ->
                BufferedReader(InputStreamReader(rawStream, Charsets.UTF_8)).use { reader ->
                    var lineCount = 0
                    var line = reader.readLine()

                    while (line != null && lineCount < 10) {
                        lineCount++
                        val trimmed = line.trim()
                        val lower = trimmed.lowercase()

                        when {
                            lower.startsWith("tên truyện:") || lower.startsWith("title:") -> {
                                title = trimmed.substringAfter(":").trim()
                            }
                            lower.startsWith("tác giả:") || lower.startsWith("author:") -> {
                                author = trimmed.substringAfter(":").trim()
                            }
                            lower.startsWith("thể loại:") || lower.startsWith("tags:") -> {
                                tags = trimmed.substringAfter(":").trim()
                            }
                            lower.startsWith("trạng thái:") || lower.startsWith("status:") -> {
                                val st = trimmed.substringAfter(":").lowercase()
                                status = when {
                                    st.contains("hoàn thành") || st.contains("full") -> BookStatus.COMPLETED
                                    st.contains("đang ra") || st.contains("ongoing") -> BookStatus.ONGOING
                                    else -> BookStatus.UNKNOWN
                                }
                            }
                        }

                        line = reader.readLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedMetadata(
            title = title,
            author = author,
            tags = tags,
            status = status
        )
    }
}