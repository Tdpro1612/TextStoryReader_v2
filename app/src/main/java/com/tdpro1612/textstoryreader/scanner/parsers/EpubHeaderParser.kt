package com.tdpro1612.textstoryreader.scanner.parsers

import com.tdpro1612.textstoryreader.database.BookStatus
import java.io.File
import java.util.zip.ZipFile

class EpubHeaderParser : BookHeaderParser {
    override val supportedExtensions: Set<String> = setOf("EPUB")

    override fun parseHeader(file: File): ParsedMetadata {
        var title = file.nameWithoutExtension
        var author = "Không rõ"
        val tagsList = mutableListOf<String>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries()

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.endsWith(".opf")) {
                    val opfContent = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                    title = extractXmlTag(opfContent, "dc:title") ?: title
                    author = extractXmlTag(opfContent, "dc:creator") ?: author
                    tagsList.addAll(extractAllXmlTags(opfContent, "dc:subject"))
                    break
                }
            }
            zipFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedMetadata(title, author, tagsList.joinToString(", "), BookStatus.UNKNOWN)
    }

    private fun extractXmlTag(content: String, tagName: String): String? {
        return "<$tagName[^>]*>(.*?)</$tagName>".toRegex(RegexOption.IGNORE_CASE).find(content)?.groupValues?.get(1)?.trim()
    }

    private fun extractAllXmlTags(content: String, tagName: String): List<String> {
        return "<$tagName[^>]*>(.*?)</$tagName>".toRegex(RegexOption.IGNORE_CASE).findAll(content).map { it.groupValues[1].trim() }.toList()
    }
}