package com.tdpro1612.textstoryreader.reader.epub

import android.content.Context
import android.util.Log
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.reader.cache.ChapterCacheManager
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import kotlin.math.abs
import kotlin.system.measureTimeMillis

class EpubFileReader(
    private val context: Context,
    private val cacheFolder: File
) {

    private val TAG = "EpubFileReaderPerformance"

    // Regex chống bắt nhầm từ ghép như "Hồi lâu sau", "Tập kích x12"...
    // Regex tối giản & siêu sạch: Chỉ tập trung vào các từ khóa chương chuẩn xác
    private val strictChapterPattern = Regex(
        """(?i)^\s*(?:Chương|Chapter|Quyển|Vol|Tiết|Ngoại truyện|Lời bạt|Mở đầu|Kết thúc)\s*(\d+|[0-9IVXLCDM]+)?\b"""
    )

    private val naturalCompareTokenRegex = Regex("""\d+|\D+""")
    private val cacheManager = ChapterCacheManager(context.cacheDir)
    private val targetTags = arrayOf("h1", "h2", "h3", "h4", "h5", "h6", "p")

    private fun fastFindTagMatches(html: String): List<Triple<String, String, Int>> {
        val results = mutableListOf<Triple<String, String, Int>>()
        val n = html.length
        var i = 0

        while (i < n) {
            if (html[i] == '<') {
                var matchedTag: String? = null

                for (tag in targetTags) {
                    val tagLen = tag.length
                    val tagStart = i + 1
                    if (tagStart + tagLen <= n &&
                        html.regionMatches(tagStart, tag, 0, tagLen, ignoreCase = true)
                    ) {
                        val afterTagIdx = tagStart + tagLen
                        if (afterTagIdx < n) {
                            val c = html[afterTagIdx]
                            if (c == '>' || c.isWhitespace() || c == '/') {
                                matchedTag = tag
                                break
                            }
                        }
                    }
                }

                if (matchedTag != null) {
                    val gtIdx = html.indexOf('>', i)
                    if (gtIdx == -1) {
                        i++
                        continue
                    }

                    val closeTag = "</$matchedTag>"
                    val closeIdx = html.indexOf(closeTag, gtIdx + 1, ignoreCase = true)

                    if (closeIdx != -1) {
                        val innerHtml = html.substring(gtIdx + 1, closeIdx)
                        results.add(Triple(matchedTag, innerHtml, i))
                        i = closeIdx + closeTag.length
                        continue
                    } else {
                        i = gtIdx + 1
                        continue
                    }
                }
            }
            i++
        }

        return results
    }

    private fun isLikelyTocLinkEntry(innerHtml: String): Boolean {
        val lower = innerHtml.lowercase()
        val aIdx = lower.indexOf("<a ")
        if (aIdx == -1) return false

        val hrefIdx = lower.indexOf("href", aIdx)
        if (hrefIdx == -1) return false

        val qStartDouble = lower.indexOf('"', hrefIdx)
        val qStartSingle = lower.indexOf('\'', hrefIdx)

        val qStart = when {
            qStartDouble != -1 && qStartSingle != -1 -> minOf(qStartDouble, qStartSingle)
            qStartDouble != -1 -> qStartDouble
            qStartSingle != -1 -> qStartSingle
            else -> -1
        }
        if (qStart == -1) return false

        val quoteChar = lower[qStart]
        val qEnd = lower.indexOf(quoteChar, qStart + 1)
        if (qEnd == -1) return false

        val hrefValue = innerHtml.substring(qStart + 1, qEnd).trim()

        if (hrefValue.isBlank()) return false
        if (!hrefValue.startsWith("#")) return true

        return hasChapterKeywordFast(innerHtml)
    }

    fun getChapterList(): List<BookChapter> {
        val totalStart = System.currentTimeMillis()
        val bookId = cacheFolder.name

        var cachedChapters: List<BookChapter>? = null
        val cacheTime = measureTimeMillis {
            try {
                cachedChapters = cacheManager.getCachedChapters(bookId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi đọc Cache: ${e.localizedMessage}")
            }
        }

        if (!cachedChapters.isNullOrEmpty()) {
            Log.d(TAG, "⚡ [CACHE HIT] Đã lấy ${cachedChapters!!.size} chương từ Cache trong ${cacheTime}ms (Book: $bookId)")
            return cachedChapters!!
        }

        var chapters: List<BookChapter> = emptyList()
        val parseTime = measureTimeMillis {
            val rawChapters = parseChapterListFromDisk()
            chapters = rawChapters
                .filter { chapter -> !isJunkChapterTitle(chapter.title) }
                .mapIndexed { index, chapter ->
                    chapter.copy(index = index)
                }
        }

        val cacheWriteTime = measureTimeMillis {
            if (chapters.isNotEmpty()) {
                try {
                    cacheManager.saveChaptersToCache(bookId, chapters)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Lỗi ghi Cache: ${e.localizedMessage}")
                }
            }
        }
        // 1. Tìm vị trí index đầu tiên thỏa mãn điều kiện bắt đầu (quét cực nhanh từ trên xuống)
        val startIndex = chapters.indexOfFirst { chapter ->
            val titleLower = chapter.title.lowercase().trim()
            titleLower.contains("chương 1") ||
                    titleLower.contains("chapter 1") ||
                    titleLower.contains("chương 01") ||
                    titleLower.contains("chương 001") ||
                    titleLower.startsWith("1") ||
                    titleLower.startsWith("0") ||
                    titleLower.contains("mở đầu") ||
                    titleLower.contains("giới thiệu") ||
                    titleLower.contains("vol 1") ||
                    titleLower.contains("quyển 1")
        }

        // 2. Nếu tìm thấy (index >= 0), cắt danh sách từ đó về sau.
        // Nếu không tìm thấy, giữ nguyên danh sách cũ.
        chapters = if (startIndex != -1) {
            chapters.subList(startIndex, chapters.size).toMutableList()
        } else {
            chapters
        }
        // 3. 🧹 KHẮC PHỤC TRÙNG LẶP: Lọc bỏ các tiêu đề trùng nhau nằm sát cạnh nhau (hiện tượng file gộp dính 2 chương 1)
        chapters = chapters.filterIndexed { index, chapter ->
            if (index == 0) true
            else {
                val prevTitle = chapters[index - 1].title.lowercase().trim()
                val currTitle = chapter.title.lowercase().trim()
                // Nếu tiêu đề hiện tại khác hoàn toàn tiêu đề trước đó thì giữ lại, giống nhau thì vứt bỏ 1 cái
                currTitle != prevTitle
            }
        }.toMutableList()

        val totalTime = System.currentTimeMillis() - totalStart
        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "📊 [PERFORMANCE SUMMARY] Sách: $bookId")
        Log.i(TAG, "   • Đọc Cache: ${cacheTime}ms")
        Log.i(TAG, "   • Parse từ Disk: ${parseTime}ms")
        Log.i(TAG, "   • Ghi Cache: ${cacheWriteTime}ms")
        Log.i(TAG, "   👉 TỔNG THỜI GIAN: ${totalTime}ms (Tổng số: ${chapters.size} chương)")
        Log.i(TAG, "--------------------------------------------------")

        return chapters
    }

    fun forceReloadChapterList(): List<BookChapter> {
        val bookId = cacheFolder.name
        cacheManager.clearCache(bookId)
        return getChapterList()
    }

    private fun parseChapterListFromDisk(): List<BookChapter> {
        val chapters = mutableListOf<BookChapter>()

        var opfTime = 0L
        var tocTime = 0L
        var htmlReadTime = 0L
        var tagMatchTime = 0L
        var chapterExtractTime = 0L

        val htmlPathsInSpine: List<String>
        opfTime = measureTimeMillis {
            htmlPathsInSpine = getOrderedHtmlPathsFromOpf()
        }

        val tocList: List<Pair<String, String>>
        tocTime = measureTimeMillis {
            tocList = parseTocList()
        }

        val htmlCount = htmlPathsInSpine.size
        val tocCount = tocList.size
        val diff = abs(htmlCount - tocCount)

        // 🛑 ĐIỀU KIỆN KÍCH HOẠT FAST-PATH AN TOÀN:
        // Chỉ bật Fast-Path khi số chương trong TOC xấp xỉ số file HTML (Mỗi file 1 chương)
        if (htmlCount > 0 && tocCount > 0 && diff <= 10) {
            Log.i(TAG, "🚀 [FAST-PATH ACTIVE] Số HTML ($htmlCount) và số TOC ($tocCount) tương đương. Lấy trực tiếp từ TOC!")

            var fastIndex = 0
            val tocMapByFile = tocList.toMap()
            for (relativePath in htmlPathsInSpine) {
                val cleanFileName = relativePath.substringAfterLast("/")
                val title = tocMapByFile[cleanFileName] ?: tocMapByFile[relativePath]

                if (!title.isNullOrBlank()) {
                    chapters.add(
                        BookChapter(
                            index = fastIndex++,
                            title = title,
                            path = relativePath,
                            path_next = "",
                            startCharOffset = 0,
                            endCharOffset = -1
                        )
                    )
                }
            }
            if (chapters.isNotEmpty()) return chapters
        } else {
            Log.d(TAG, "🐢 [DEEP-SCAN ACTIVE] Phát hiện sách gộp/lệch lớn ($htmlCount file HTML vs $tocCount title TOC). Chuyển sang Deep-Scan!")
        }

        // Tạo map file -> title để hỗ trợ fallback khi scan
        val tocFileMap = mutableMapOf<String, String>()
        for ((src, title) in tocList) {
            val fileName = src.substringBefore("#").substringAfterLast("/")
            if (!tocFileMap.containsKey(fileName)) {
                tocFileMap[fileName] = title
            }
        }

        // 🐢 DEEP-SCAN: Quét 75 file HTML để trích xuất toàn bộ 1628 chương
        var globalIndex = 0
        var pendingPath: String? = null
        var pendingHtml: String? = null
        var pendingTagMatches: List<Triple<String, String, Int>>? = null

        val loopTime = measureTimeMillis {
            for (i in htmlPathsInSpine.indices) {
                val relativePath = htmlPathsInSpine[i]

                val fullHtml: String
                val tagMatchesForCurrent: List<Triple<String, String, Int>>

                if (pendingPath == relativePath && pendingHtml != null && pendingTagMatches != null) {
                    fullHtml = pendingHtml
                    tagMatchesForCurrent = pendingTagMatches
                } else {
                    val readMs = measureTimeMillis { fullHtml = readTextFile(relativePath) }
                    htmlReadTime += readMs

                    val matchMs = measureTimeMillis { tagMatchesForCurrent = fastFindTagMatches(fullHtml) }
                    tagMatchTime += matchMs
                }

                if (fullHtml.isBlank()) {
                    pendingPath = null
                    pendingHtml = null
                    pendingTagMatches = null
                    continue
                }

                val nextRelativePath = if (i < htmlPathsInSpine.size - 1) htmlPathsInSpine[i + 1] else ""

                var nextTagMatches: List<Triple<String, String, Int>>? = null
                if (nextRelativePath.isNotBlank()) {
                    var nextHtml = ""
                    val readMs = measureTimeMillis { nextHtml = readTextFile(nextRelativePath) }
                    htmlReadTime += readMs

                    val matchMs = measureTimeMillis { nextTagMatches = fastFindTagMatches(nextHtml) }
                    tagMatchTime += matchMs

                    pendingPath = nextRelativePath
                    pendingHtml = nextHtml
                    pendingTagMatches = nextTagMatches
                } else {
                    pendingPath = null
                    pendingHtml = null
                    pendingTagMatches = null
                }

                val extractMs = measureTimeMillis {
                    val extractedChapters = extractChaptersFromRawHtml(
                        relativePath = relativePath,
                        nextRelativePath = nextRelativePath,
                        fullHtml = fullHtml,
                        tagMatches = tagMatchesForCurrent,
                        nextFileTagMatches = nextTagMatches,
                        tocMap = tocFileMap
                    )

                    for (ch in extractedChapters) {
                        chapters.add(ch.copy(index = globalIndex++))
                    }
                }
                chapterExtractTime += extractMs
            }
        }

        Log.d(TAG, "  🔍 [DETAIL PARSE BREAKDOWN]")
        Log.d(TAG, "     1. Read OPF Structure: ${opfTime}ms (${htmlPathsInSpine.size} files HTML)")
        Log.d(TAG, "     2. Read TOC NCX: ${tocTime}ms (${tocList.size} titles in TOC)")
        Log.d(TAG, "     3. Total File I/O (Read Disk): ${htmlReadTime}ms")
        Log.d(TAG, "     4. Fast Tag Matching: ${tagMatchTime}ms")
        Log.d(TAG, "     5. Chapter Extractor Logic: ${chapterExtractTime}ms")
        Log.d(TAG, "     ⏱️ Total Loop Time: ${loopTime}ms")

        return chapters
    }

    private fun extractChaptersFromRawHtml(
        relativePath: String,
        nextRelativePath: String,
        fullHtml: String,
        tagMatches: List<Triple<String, String, Int>>,
        nextFileTagMatches: List<Triple<String, String, Int>>?,
        tocMap: Map<String, String>
    ): List<BookChapter> {
        val result = mutableListOf<BookChapter>()
        val rawNodes = mutableListOf<Pair<String, Int>>()

        var tocLinkCount = 0

        for ((_, innerHtml, startIdx) in tagMatches) {
            val innerLen = innerHtml.length

            if (innerLen in 3..200) {
                if (isLikelyTocLinkEntry(innerHtml)) {
                    tocLinkCount++
                    continue
                }

                if (hasChapterKeywordFast(innerHtml)) {
                    val cleanText = fastStripHtmlTags(innerHtml)
                    if (cleanText.length in 3..100 && isStrictChapterTitle(cleanText)) {
                        rawNodes.add(Pair(cleanText, startIdx))
                    }
                }
            }
        }

        if (tocLinkCount > 5 && rawNodes.size < 2) {
            return emptyList()
        }

        val filteredNodes = mutableListOf<Pair<String, Int>>()
        val rawSize = rawNodes.size
        for (i in 0 until rawSize) {
            val currentNode = rawNodes[i]
            if (i < rawSize - 1) {
                val nextNode = rawNodes[i + 1]
                if (nextNode.second - currentNode.second < 80) {
                    continue
                }
            }
            filteredNodes.add(currentNode)
        }

        val uniqueNodes = mutableListOf<Pair<String, Int>>()
        val seenKeys = HashSet<String>()

        for (node in filteredNodes) {
            val uniqueKey = "${node.first.lowercase().trim()}_${node.second}"
            if (seenKeys.add(uniqueKey)) {
                uniqueNodes.add(node)
            }
        }

        if (uniqueNodes.size >= 2) {
            for (i in uniqueNodes.indices) {
                val (title, startPos) = uniqueNodes[i]

                if (i < uniqueNodes.size - 1) {
                    val endPos = uniqueNodes[i + 1].second
                    result.add(
                        BookChapter(
                            index = 0,
                            title = title,
                            path = relativePath,
                            path_next = "",
                            startCharOffset = startPos,
                            endCharOffset = endPos
                        )
                    )
                } else {
                    val (nextPath, endPos) = findChapterEndFromMatches(nextRelativePath, nextFileTagMatches)
                    result.add(
                        BookChapter(
                            index = 0,
                            title = title,
                            path = relativePath,
                            path_next = nextPath,
                            startCharOffset = startPos,
                            endCharOffset = endPos
                        )
                    )
                }
            }
            return result
        }

        val cleanFileName = relativePath.substringAfterLast("/")
        val titleFromToc = tocMap[cleanFileName] ?: tocMap[relativePath]

        val singleTitle = titleFromToc
            ?: uniqueNodes.firstOrNull()?.first
            ?: extractFirstLineTitle(fullHtml, cleanFileName)

        result.add(
            BookChapter(
                index = 0,
                title = singleTitle,
                path = relativePath,
                path_next = "",
                startCharOffset = 0,
                endCharOffset = -1
            )
        )

        return result
    }

    private fun hasChapterKeywordFast(text: String): Boolean {
        return text.contains("Chương", ignoreCase = true) ||
                text.contains("Chapter", ignoreCase = true) ||
                text.contains("Quyển", ignoreCase = true) ||
                text.contains("Tập", ignoreCase = true) ||
                text.contains("Vol", ignoreCase = true) ||
                text.contains("Hồi", ignoreCase = true) ||
                text.contains("Tiết", ignoreCase = true)
    }

    private fun fastStripHtmlTags(input: String): String {
        val charArray = CharArray(input.length)
        var head = 0
        var inTag = false

        for (i in 0 until input.length) {
            val c = input[i]
            if (c == '<') {
                inTag = true
            } else if (c == '>') {
                inTag = false
            } else if (!inTag) {
                charArray[head++] = c
            }
        }

        return String(charArray, 0, head)
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .trim()
    }

    private fun isStrictChapterTitle(text: String): Boolean {
        return strictChapterPattern.containsMatchIn(text)
    }

    private fun findChapterEndFromMatches(
        nextRelativePath: String,
        nextTagMatches: List<Triple<String, String, Int>>?
    ): Pair<String, Int> {
        if (nextRelativePath.isBlank() || nextTagMatches == null) return Pair(nextRelativePath, -1)

        for ((_, innerHtml, startIdx) in nextTagMatches) {
            if (innerHtml.length in 3..200 &&
                !isLikelyTocLinkEntry(innerHtml) &&
                hasChapterKeywordFast(innerHtml)
            ) {
                val cleanText = fastStripHtmlTags(innerHtml)
                if (cleanText.length in 3..100 && isStrictChapterTitle(cleanText)) {
                    return Pair(nextRelativePath, startIdx)
                }
            }
        }

        return Pair(nextRelativePath, -1)
    }

    private fun readTextFile(relativePath: String): String {
        val file = File(cacheFolder, relativePath)
        return if (file.exists() && file.isFile) file.readText(Charsets.UTF_8) else ""
    }

    private fun getOrderedHtmlPathsFromOpf(): List<String> {
        var paths = listOf<String>()
        try {
            val opfFile: File? = cacheFolder.walk().firstOrNull { it.extension.lowercase() == "opf" }

            if (opfFile == null) {
                return getAllValidHtmlFilesOnDisk().map { it.relativeTo(cacheFolder).path.replace("\\", "/") }
            }

            val opfContent = opfFile.readText(Charsets.UTF_8)
            val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

            val opfFolder = if (opfFile.parentFile != cacheFolder) {
                opfFile.parentFile.relativeTo(cacheFolder).path.replace("\\", "/") + "/"
            } else ""

            val manifestMap = mutableMapOf<String, String>()
            doc.select("manifest > item").forEach { item ->
                val id = item.attr("id")
                val href = item.attr("href")
                if (isHtmlFile(href)) {
                    manifestMap[id] = opfFolder + href
                }
            }

            val resultList = mutableListOf<String>()
            doc.select("spine > itemref").forEach { itemref ->
                val idref = itemref.attr("idref")
                val fullPath = manifestMap[idref]
                if (!fullPath.isNullOrBlank() && !isJunkHtmlFile(fullPath)) {
                    resultList.add(fullPath)
                }
            }
            paths = resultList
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return if (paths.isNotEmpty()) paths else getAllValidHtmlFilesOnDisk().map { it.relativeTo(cacheFolder).path.replace("\\", "/") }
    }

    // Lấy danh sách đầy đủ các cặp (src, title) từ TOC không bị đè trùng
    private fun parseTocList(): List<Pair<String, String>> {
        val titleList = mutableListOf<Pair<String, String>>()
        try {
            val ncxFile = cacheFolder.walk().firstOrNull { it.name.lowercase().endsWith("toc.ncx") }
            if (ncxFile != null) {
                val doc = Jsoup.parse(ncxFile.readText(Charsets.UTF_8), "", Parser.xmlParser())
                doc.select("navPoint").forEach { navPoint ->
                    val title = navPoint.select("> navLabel > text").text().trim()
                    val src = navPoint.select("> content").attr("src").trim()
                    if (src.isNotBlank() && title.isNotBlank()) {
                        titleList.add(Pair(src, title))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return titleList
    }

    private fun extractFirstLineTitle(rawHtml: String, fileName: String): String {
        val cleanText = fastStripHtmlTags(rawHtml)
        val firstLine = cleanText.lines().firstOrNull { it.isNotBlank() }
        return if (!firstLine.isNullOrBlank() && firstLine.length <= 60) {
            firstLine
        } else {
            val num = Regex("""\d+""").find(fileName)?.value?.toIntOrNull()
            if (num != null) "Chương $num" else fileName
        }
    }

    private fun getAllValidHtmlFilesOnDisk(): List<File> {
        return cacheFolder.walk()
            .filter { it.isFile && isHtmlFile(it.name) && !isJunkHtmlFile(it.name) }
            .toList()
            .sortedWith(Comparator { f1, f2 -> naturalCompare(f1.name, f2.name) })
    }

    private fun isHtmlFile(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".html") || lower.endsWith(".xhtml") || lower.endsWith(".htm")
    }

    private fun isJunkHtmlFile(pathOrFileName: String): Boolean {
        val fileName = pathOrFileName.substringAfterLast("/").lowercase()
        return fileName == "out.html" || fileName == "toc.html" || fileName == "toc.xhtml"
                || fileName == "nav.html" || fileName == "nav.xhtml" || fileName.contains("cover")
                || fileName.contains("titlepage")
    }

    private fun isJunkChapterTitle(title: String): Boolean {
        val clean = title.trim().lowercase()

        val junkTitles = listOf(
            "document outline",
            "outline",
            "cover",
            "bìa",
            "trang bìa",
            "mục lục",
            "table of contents",
            "contents",
            "toc",
            "index"
        )

        return junkTitles.contains(clean) || clean.startsWith("mục lục")
    }

    private fun naturalCompare(s1: String, s2: String): Int {
        val name1 = s1.substringAfterLast("/")
        val name2 = s2.substringAfterLast("/")
        val p1 = naturalCompareTokenRegex.findAll(name1).map { it.value }.toList()
        val p2 = naturalCompareTokenRegex.findAll(name2).map { it.value }.toList()
        val size = minOf(p1.size, p2.size)
        for (i in 0 until size) {
            val a = p1[i]
            val b = p2[i]
            if (a != b) {
                val numA = a.toLongOrNull()
                val numB = numA?.let { b.toLongOrNull() }
                if (numA != null && numB != null) return numA.compareTo(numB)
                return a.compareTo(b)
            }
        }
        return p1.size.compareTo(p2.size)
    }
}