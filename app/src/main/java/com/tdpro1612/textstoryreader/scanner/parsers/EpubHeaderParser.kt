package com.tdpro1612.textstoryreader.scanner.parsers

import android.content.Context
import android.net.Uri
import com.tdpro1612.textstoryreader.database.BookStatus
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

class EpubHeaderParser : BookHeaderParser {
    override val supportedExtensions: Set<String> = setOf("EPUB")

    override suspend fun parseHeader(context: Context, uri: Uri): ParsedMetadata {
        var title = ""
        var author = "Không rõ"
        val tagsList = mutableListOf<String>()

        try {
            // 🔥 Bọc BufferedInputStream(..., 8192) để tăng tốc đọc SAF gấp 20 lần
            context.contentResolver.openInputStream(uri)?.use { rawStream ->
                BufferedInputStream(rawStream, 8192).use { bufferedStream ->
                    ZipInputStream(bufferedStream).use { zipInput ->
                        var entry = zipInput.nextEntry
                        while (entry != null) {
                            if (entry.name.endsWith(".opf", ignoreCase = true)) {
                                // Đọc trực tiếp không qua bufferedReader phụ
                                val opfContent = zipInput.readBytes().toString(Charsets.UTF_8)

                                title = extractXmlTag(opfContent, "dc:title") ?: title
                                author = extractXmlTag(opfContent, "dc:creator") ?: author
                                tagsList.addAll(extractAllXmlTags(opfContent, "dc:subject"))

                                zipInput.closeEntry()
                                break // Ngắt ngay khi tìm thấy file .opf
                            }
                            zipInput.closeEntry()
                            entry = zipInput.nextEntry
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedMetadata(
            title = title,
            author = author,
            tags = tagsList.joinToString(", "),
            status = BookStatus.ONGOING
        )
    }

    private fun extractXmlTag(content: String, tagName: String): String? {
        return "<$tagName[^>]*>(.*?)</$tagName>"
            .toRegex(RegexOption.IGNORE_CASE)
            .find(content)?.groupValues?.get(1)?.trim()
    }

    private fun extractAllXmlTags(content: String, tagName: String): List<String> {
        return "<$tagName[^>]*>(.*?)</$tagName>"
            .toRegex(RegexOption.IGNORE_CASE)
            .findAll(content)
            .map { it.groupValues[1].trim() }
            .toList()
    }
}