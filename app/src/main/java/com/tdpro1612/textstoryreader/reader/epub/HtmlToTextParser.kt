package com.tdpro1612.textstoryreader.reader.epub

import com.tdpro1612.textstoryreader.reader.BookChapter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.io.File

object HtmlToTextParser {

    private val BLOCK_TAGS = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
        "li", "blockquote", "section", "article", "dt", "dd"
    )

    /**
     * Hàm chính: Nhận BookChapter và thư mục Cache để trích xuất ra Text chuẩn nhất.
     */
    fun parseChapter(chapter: BookChapter, cacheFolder: File): String {
        val rawHtml = extractRawHtmlFromChapter(chapter, cacheFolder)
        if (rawHtml.isBlank()) return ""
        return parseHtmlToText(rawHtml)
    }

    /**
     * Trích xuất đoạn HTML chính xác dựa vào Offset và path / path_next của BookChapter
     */
    private fun extractRawHtmlFromChapter(chapter: BookChapter, cacheFolder: File): String {
        if (chapter.path.isBlank()) return ""

        val primaryFile = File(cacheFolder, chapter.path)
        if (!primaryFile.exists()) return ""

        val fullHtml = primaryFile.readText(Charsets.UTF_8)
        if (fullHtml.isBlank()) return ""

        // TRƯỜNG HỢP 1: Chương bị đứt đoạn, tràn sang file tiếp theo (path_next)
        if (chapter.path_next.isNotBlank()) {
            val nextFile = File(cacheFolder, chapter.path_next)
            val part1 = if (chapter.startCharOffset in 0..fullHtml.length) {
                fullHtml.substring(chapter.startCharOffset)
            } else fullHtml

            val part2 = if (nextFile.exists()) {
                val nextHtml = nextFile.readText(Charsets.UTF_8)
                if (chapter.endCharOffset in 0..nextHtml.length) {
                    nextHtml.substring(0, chapter.endCharOffset)
                } else nextHtml
            } else ""

            return "$part1\n$part2"
        }

        // TRƯỜNG HỢP 2: Nằm trọn trong 1 file HTML nhưng bị cắt theo Offset (File chứa nhiều chương)
        if (chapter.endCharOffset > chapter.startCharOffset && chapter.endCharOffset <= fullHtml.length) {
            return fullHtml.substring(chapter.startCharOffset, chapter.endCharOffset)
        }

        // TRƯỜNG HỢP 3: Lấy toàn bộ file HTML (endCharOffset = -1)
        if (chapter.startCharOffset > 0 && chapter.startCharOffset < fullHtml.length) {
            return fullHtml.substring(chapter.startCharOffset)
        }

        return fullHtml
    }

    /**
     * Parse HTML thành Văn bản thuần (Clean Text)
     */
    fun parseHtmlToText(html: String): String {
        if (html.isBlank()) return ""

        var cleanInput = html
        if (cleanInput.trimStart().startsWith("id=") || cleanInput.trimStart().startsWith("class=")) {
            val closeIdx = cleanInput.indexOf('>')
            if (closeIdx != -1) {
                cleanInput = cleanInput.substring(closeIdx + 1)
            }
        }

        val normalizedHtml = cleanInput
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("\t", " ")

        val doc = Jsoup.parse(normalizedHtml, "", Parser.htmlParser())

        doc.select("script, style, head, nav, footer, header, iframe").remove()
        doc.select("[epub|type*=toc], [epub|type*=landmarks], [epub|type*=page-list]").remove()

        doc.select("ul, ol").forEach { list ->
            if (list.select("a").size >= 5) {
                list.remove()
            }
        }

        doc.select("br").forEach { it.replaceWith(TextNode("\n")) }

        val sb = StringBuilder()

        fun traverse(node: Node) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) {
                        sb.append(text)
                    }
                }
                is Element -> {
                    val isBlock = node.tagName().lowercase() in BLOCK_TAGS
                    if (isBlock) {
                        sb.append("\n\n")
                    }
                    node.childNodes().forEach { traverse(it) }
                    if (isBlock) {
                        sb.append("\n\n")
                    }
                }
                else -> node.childNodes().forEach { traverse(it) }
            }
        }

        traverse(doc.body() ?: doc)

        val lines = sb.toString()
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim().replace(Regex("""[ \t]+"""), " ") }
            .filter { it.isNotBlank() }

        return lines.joinToString("\n\n")
    }
}