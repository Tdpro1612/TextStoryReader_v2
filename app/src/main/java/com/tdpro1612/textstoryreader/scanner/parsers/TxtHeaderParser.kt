package com.tdpro1612.textstoryreader.scanner.parsers

import com.tdpro1612.textstoryreader.database.BookStatus
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

class TxtHeaderParser : BookHeaderParser {
    override val supportedExtensions: Set<String> = setOf("TXT")

    override fun parseHeader(file: File): ParsedMetadata {
        var title = file.nameWithoutExtension
        var author = "Không rõ"
        var tags = ""
        var status = BookStatus.UNKNOWN

        try {
            val buffer = ByteArray(2048)
            val bytesRead = FileInputStream(file).use { fis -> fis.read(buffer) }

            if (bytesRead > 0) {
                val headerText = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                val lines = headerText.lines().take(10)

                for (line in lines) {
                    val trimmed = line.trim()
                    val lower = trimmed.lowercase()

                    when {
                        lower.startsWith("tên truyện:") || lower.startsWith("title:") -> title = trimmed.substringAfter(":").trim()
                        lower.startsWith("tác giả:") || lower.startsWith("author:") -> author = trimmed.substringAfter(":").trim()
                        lower.startsWith("thể loại:") || lower.startsWith("tags:") -> tags = trimmed.substringAfter(":").trim()
                        lower.startsWith("trạng thái:") || lower.startsWith("status:") -> {
                            val st = trimmed.substringAfter(":").lowercase()
                            status = when {
                                st.contains("hoàn thành") || st.contains("full") -> BookStatus.COMPLETED
                                st.contains("đang ra") || st.contains("ongoing") -> BookStatus.ONGOING
                                else -> BookStatus.UNKNOWN
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedMetadata(title, author, tags, status)
    }
}