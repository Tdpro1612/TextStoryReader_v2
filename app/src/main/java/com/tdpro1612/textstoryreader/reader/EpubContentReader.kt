package com.tdpro1612.textstoryreader.reader

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sf.jazzlib.ZipFile
import nl.siegmann.epublib.domain.Book
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.domain.TOCReference
import nl.siegmann.epublib.epub.EpubReader
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset

class EpubContentReader : BookContentReader {

    private var cachedBook: Book? = null
    private var cachedUri: String? = null

    private var cachedZipFile: ZipFile? = null
    private var cachedTempFile: File? = null

    private suspend fun getBook(context: Context, uri: Uri): Book? = withContext(Dispatchers.IO) {
        if (cachedBook != null && cachedUri == uri.toString()) {
            return@withContext cachedBook
        }

        releaseCurrentBook()

        try {
            val tempFile = File.createTempFile("epub_cache_", ".epub", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                tempFile.delete()
                return@withContext null
            }

            val zipFile = ZipFile(tempFile)
            val book = try {
                EpubReader().readEpubLazy(zipFile, "UTF-8")
            } catch (e: Exception) {
                e.printStackTrace()
                zipFile.close()
                tempFile.inputStream().use { EpubReader().readEpub(it) }
            }

            cachedBook = book
            cachedUri = uri.toString()
            cachedZipFile = zipFile
            cachedTempFile = tempFile
            return@withContext book
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun releaseCurrentBook() {
        try {
            cachedZipFile?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedTempFile?.delete()
        cachedZipFile = null
        cachedTempFile = null
        cachedBook = null
        cachedUri = null
    }

    suspend fun getSafeBookTitle(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val book = getBook(context, uri)
        val title = book?.title
        if (!title.isNullOrBlank()) return@withContext title.trim()

        return@withContext try {
            var name = ""
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx != -1) name = cursor.getString(idx)
            }
            if (name.isNotBlank()) name.substringBeforeLast(".") else "Truyện không tên"
        } catch (e: Exception) {
            "Truyện không tên"
        }
    }

    private fun isMainTocResource(href: String?, title: String?): Boolean {
        val h = href?.lowercase().orEmpty()
        val t = title?.lowercase().orEmpty()

        val isTocTitle = t == "mục lục" || t == "contents" || t == "table of contents" || t == "toc"
        val isTocFile = h.endsWith("toc.xhtml") || h.endsWith("toc.html") ||
                h.endsWith("nav.xhtml") || h.endsWith("nav.html")

        return isTocTitle || isTocFile
    }

    override suspend fun getChapterList(context: Context, uri: Uri): List<BookChapter> = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<BookChapter>()
        val book = getBook(context, uri) ?: return@withContext chapters

        // 1. Lấy từ TOC chuẩn
        val allTocReferences = mutableListOf<TOCReference>()
        fun collectToc(references: List<TOCReference>) {
            for (ref in references) {
                allTocReferences.add(ref)
                if (!ref.children.isNullOrEmpty()) collectToc(ref.children)
            }
        }
        collectToc(book.tableOfContents?.tocReferences ?: emptyList())

        var chapterIndex = 0
        if (allTocReferences.isNotEmpty()) {
            allTocReferences.forEach { tocRef ->
                val completeHref = tocRef.completeHref ?: tocRef.resource?.href ?: ""
                val rawTitle = tocRef.title

                if (!isMainTocResource(completeHref, rawTitle)) {
                    val cleanTitle = if (!rawTitle.isNullOrBlank()) rawTitle.trim() else "Chương ${chapterIndex + 1}"
                    chapters.add(BookChapter(index = chapterIndex++, title = cleanTitle, content = completeHref))
                }
            }
        }

        // 2. Thử lấy theo Spine
        if (chapters.isEmpty()) {
            val spineReferences = book.spine?.spineReferences ?: emptyList()
            spineReferences.forEach { spineRef ->
                val resource = spineRef.resource
                val href = resource?.href ?: ""
                val rawTitle = resource?.title

                if (href.isNotBlank() && !isMainTocResource(href, rawTitle)) {
                    val cleanTitle = if (!rawTitle.isNullOrBlank()) rawTitle.trim() else "Chương ${chapterIndex + 1}"
                    chapters.add(BookChapter(index = chapterIndex++, title = cleanTitle, content = href))
                }
            }
        }

        // 3. XỬ LÝ CHỐNG OOM: Đọc lướt từng dòng bằng BufferedReader thay vì nạp cả file 30MB vào RAM
        if (chapters.isEmpty()) {
            val htmlResources = book.resources.all.filter {
                it.mediaType?.name?.contains("html", ignoreCase = true) == true
            }

            val anchorRegex = Regex("""<a\s+[^>]*href=["']#([^"']+)["'][^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)

            for (resource in htmlResources) {
                try {
                    val encoding = resource.inputEncoding ?: "UTF-8"
                    val reader = BufferedReader(InputStreamReader(resource.inputStream, Charset.forName(encoding)))

                    var line: String? = reader.readLine()
                    while (line != null) {
                        val matches = anchorRegex.findAll(line)
                        for (match in matches) {
                            val anchorId = match.groupValues[1]
                            val rawTitle = Jsoup.parse(match.groupValues[2], "", Parser.htmlParser()).text().trim()

                            if (rawTitle.isNotBlank() && !isMainTocResource(null, rawTitle)) {
                                chapters.add(
                                    BookChapter(
                                        index = chapterIndex++,
                                        title = rawTitle,
                                        content = "${resource.href}#$anchorId"
                                    )
                                )
                            }
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                if (chapters.isNotEmpty()) break
            }
        }

        // 4. Fallback: Lấy danh sách file html riêng lẻ
        if (chapters.isEmpty()) {
            val htmlResources = book.resources.all
                .filter { it.mediaType?.name?.contains("html", ignoreCase = true) == true }
                .filter { !isMainTocResource(it.href, it.title) && !it.href.lowercase().contains("out.html") }
                .sortedBy { resource ->
                    Regex("\\d+").find(resource.href)?.value?.toIntOrNull() ?: 0
                }

            htmlResources.forEach { resource ->
                chapters.add(
                    BookChapter(
                        index = chapterIndex++,
                        title = resource.title?.ifBlank { null } ?: "Chương ${chapterIndex}",
                        content = resource.href
                    )
                )
            }
        }

        return@withContext chapters
    }

    override suspend fun getChapterContent(context: Context, uri: Uri, chapter: BookChapter): String = withContext(Dispatchers.IO) {
        val book = getBook(context, uri) ?: return@withContext "Không thể tải nội dung."

        val targetHref = chapter.content
        if (targetHref.isNotBlank()) {
            val parts = targetHref.split("#")
            val rawFilePath = parts[0]
            val anchorId = if (parts.size > 1) parts[1] else null

            var resource = book.resources.getByHref(rawFilePath)
            if (resource == null) {
                val fileNameOnly = rawFilePath.substringAfterLast("/")
                resource = book.resources.all.firstOrNull {
                    it.href.endsWith(fileNameOnly) || it.href.lowercase().endsWith(fileNameOnly.lowercase())
                }
            }

            if (resource != null) {
                return@withContext if (!anchorId.isNullOrBlank()) {
                    // Đọc nội dung thông minh chỉ theo Anchor
                    extractChapterContentStream(resource, anchorId)
                } else {
                    val htmlContent = readResourceString(resource)
                    parseHtmlToFormattedText(htmlContent)
                }
            }
        }

        return@withContext "Nội dung chương rỗng."
    }

    private fun readResourceString(resource: Resource): String {
        return try {
            val encoding = resource.inputEncoding ?: "UTF-8"
            resource.inputStream.bufferedReader(Charset.forName(encoding)).use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * TỐI ƯU BỘ NHỚ STREAMING: Chỉ đọc đúng phân đoạn chứa Anchor, ngắt Stream ngay khi đủ nội dung
     */
    private fun extractChapterContentStream(resource: Resource, anchorId: String): String {
        return try {
            val encoding = resource.inputEncoding ?: "UTF-8"
            val reader = BufferedReader(InputStreamReader(resource.inputStream, Charset.forName(encoding)))

            val anchorPattern = Regex("""(?i)(id|name|href)=["']?${Regex.escape(anchorId)}["']?""")
            var foundAnchor = false
            val sb = StringBuilder()
            var charCount = 0
            val maxChars = 35000 // Giới hạn chỉ lấy tối đa 35k ký tự cho 1 chương

            var line: String? = reader.readLine()
            while (line != null) {
                if (!foundAnchor) {
                    if (anchorPattern.containsMatchIn(line)) {
                        foundAnchor = true
                        sb.append(line).append("\n")
                        charCount += line.length
                    }
                } else {
                    sb.append(line).append("\n")
                    charCount += line.length
                    // Dừng stream ngay lập tức khi đã đọc đủ độ dài 1 chương
                    if (charCount >= maxChars) break
                }
                line = reader.readLine()
            }
            reader.close()

            if (sb.isNotEmpty()) {
                parseHtmlToFormattedText(sb.toString())
            } else {
                "Nội dung chương rỗng."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Lỗi đọc nội dung chương."
        }
    }

    private fun parseHtmlToFormattedText(html: String): String {
        if (html.isBlank()) return ""

        val normalizedHtml = html
            .replace("&nbsp;", "  ")
            .replace("&#160;", "  ")
            .replace("\t", "  ")

        val doc: Document = Jsoup.parse(normalizedHtml, "", Parser.htmlParser())

        doc.select("script, style, head, nav, footer, header").remove()
        doc.select("[epub|type=toc], [epub|type~=toc]").remove()
        doc.select("[class*=toc], [id*=toc], [class*=mucluc], [id*=mucluc]").remove()

        doc.select("ul, ol").forEach { list ->
            if (list.select("a").size >= 5) {
                list.remove()
            }
        }

        doc.select("br").forEach { it.replaceWith(TextNode("\n")) }

        val blockTags = setOf(
            "p", "div", "h1", "h2", "h3", "h4", "h5", "h6",
            "li", "tr", "blockquote", "section", "article", "ul", "ol", "table", "dt", "dd"
        )

        val sb = StringBuilder()

        fun traverse(node: Node) {
            when (node) {
                is TextNode -> {
                    val text = node.wholeText
                    if (text.isNotEmpty()) sb.append(text)
                }
                is Element -> {
                    val isBlock = node.tagName().lowercase() in blockTags
                    if (isBlock) sb.append("\n\n")
                    for (child in node.childNodes()) {
                        traverse(child)
                    }
                    if (isBlock) sb.append("\n\n")
                }
                else -> {
                    for (child in node.childNodes()) {
                        traverse(child)
                    }
                }
            }
        }

        traverse(doc.body() ?: doc)

        var textContent = sb.toString().replace("\r\n", "\n")
        textContent = textContent.replace(Regex("[ \t]{2,}"), "\n\n")

        val lines = textContent
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (lines.size >= 2) {
            val line0 = lines[0].lowercase().replace(Regex("[^a-z0-9áàảãạăắcằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ]"), "")
            val line1 = lines[1].lowercase().replace(Regex("[^a-z0-9áàảãạăắcằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵđ]"), "")
            if (line0.isNotEmpty() && line1.isNotEmpty() && (line0.contains(line1) || line1.contains(line0))) {
                lines.removeAt(1)
            }
        }

        var rawResult = lines.joinToString("\n\n")

        rawResult = rawResult.replace(Regex("([^\\n])\\s*([\"“][A-ZÀÁẢẠÃĂẮẰẲẴẶÂẤẦẨẪẬĐÈÉẺẼẸÊẾỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỐỒỔỖỘƠỚỜỞỠỢÙÚỦŨỤƯỨỪỬỮỰỲÝỶỸỴ])")) { match ->
            "${match.groupValues[1]}\n\n${match.groupValues[2]}"
        }

        return rawResult
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}