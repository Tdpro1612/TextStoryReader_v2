package com.tdpro1612.textstoryreader.reader.epub

import android.content.Context
import android.util.Log
import com.tdpro1612.textstoryreader.reader.BookChapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private val strictChapterPattern = Regex(
        """(?i)^\s*(?:Chương|Chapter|Quyển|Vol|Tiết|Ngoại truyện|Lời bạt|Mở đầu)\s*(\d+|[0-9IVXLCDM]+)?\b|^\s*Hồi\s*(\d+|[0-9IVXLCDM]+|:|\-)\b""",
        RegexOption.IGNORE_CASE
    )

    private val naturalCompareTokenRegex = Regex("""\d+|\D+""")
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
                    val closeLen = closeTag.length
                    var closeIdx = -1

                    for (j in (gtIdx + 1)..(n - closeLen)) {
                        if (html[j] == '<' && html.regionMatches(j, closeTag, 0, closeLen, ignoreCase = true)) {
                            closeIdx = j
                            break
                        }
                    }

                    if (closeIdx != -1) {
                        val innerHtml = html.substring(gtIdx + 1, closeIdx)
                        results.add(Triple(matchedTag, innerHtml, i))
                        i = closeIdx + closeLen
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

    suspend fun getChapterList(): List<BookChapter> {
        val totalStart = System.currentTimeMillis()

        var chapters: List<BookChapter> = emptyList()
        val parseTime = measureTimeMillis {
            val rawChapters = parseChapterListFromDisk()
            Log.d(TAG, "🔢 Sau parse trước khi lọc junk: ${rawChapters.size} chương")
            chapters = rawChapters
                .filter { chapter -> !isJunkChapterTitle(chapter.title) }
                .mapIndexed { index, chapter ->
                    chapter.copy(index = index)
                }
        }
        Log.d(TAG, "🔢 Sau parse + lọc junk: ${chapters.size} chương")

        val startIndex = chapters.indexOfFirst { chapter ->
            val titleLower = chapter.title.lowercase().trim()
            titleLower.contains("chương 1:") ||
                    titleLower.contains("chương 1.") ||
                    titleLower.contains("chapter 1:") ||
                    titleLower.contains("chương 01:") ||
                    titleLower.contains("chương 001:") ||
                    titleLower == "chương 1" ||
                    titleLower == "1" ||
                    titleLower.contains("mở đầu") ||
                    titleLower.contains("giới thiệu")
        }
        Log.d(TAG, "🎯 startIndex tìm thấy = $startIndex" + if (startIndex != -1) " (title: \"${chapters[startIndex].title}\")" else "")

        chapters = if (startIndex != -1) {
            chapters.subList(startIndex, chapters.size).toMutableList()
        } else {
            chapters
        }
        Log.d(TAG, "🔢 Sau cắt startIndex: ${chapters.size} chương")

        if (chapters.size >= 2) {
            val firstTitle = chapters[0].title.lowercase().trim()
            val secondTitle = chapters[1].title.lowercase().trim()

            if (firstTitle == secondTitle) {
                chapters = chapters.drop(1).toMutableList()
            }
        }
        Log.d(TAG, "🔢 Sau lọc trùng lặp: ${chapters.size} chương")

        val totalTime = System.currentTimeMillis() - totalStart
        Log.i(TAG, "--------------------------------------------------")
        Log.i(TAG, "📊 [PERFORMANCE SUMMARY]")
        Log.i(TAG, "   • Parse từ Disk: ${parseTime}ms")
        Log.i(TAG, "   👉 TỔNG THỜI GIAN: ${totalTime}ms (Tổng số: ${chapters.size} chương)")
        Log.i(TAG, "--------------------------------------------------")

        return chapters
    }

    private suspend fun parseChapterListFromDisk(): List<BookChapter> {
        val chapters = mutableListOf<BookChapter>()

        var opfTime = 0L
        var tocTime = 0L

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

        val tocFileMap = mutableMapOf<String, String>()
        for ((src, title) in tocList) {
            val fileName = src.substringBefore("#").substringAfterLast("/")
            if (!tocFileMap.containsKey(fileName)) {
                tocFileMap[fileName] = title
            }
        }

        // 🟢 DEEP SCAN CHẠY ĐA LUỒNG TRÊN DISPATCHERS.DEFAULT
        val loopTime = measureTimeMillis {
            val isFastPathActive = htmlCount > 0 && tocCount > 0 && diff <= 10

            val rawExtractedList = coroutineScope {
                htmlPathsInSpine.mapIndexed { i, relativePath ->
                    async(Dispatchers.Default) {
                        val fullHtml = readTextFile(relativePath)
                        if (fullHtml.isBlank()) {
                            emptyList()
                        } else {
                            val nextRelativePath = if (i < htmlPathsInSpine.size - 1) htmlPathsInSpine[i + 1] else ""

                            val tagMatches = if (isFastPathActive) emptyList() else fastFindTagMatches(fullHtml)
                            val nextTagMatches = if (!isFastPathActive && nextRelativePath.isNotBlank()) {
                                val nextHtml = readTextFile(nextRelativePath)
                                if (nextHtml.isNotBlank()) fastFindTagMatches(nextHtml) else null
                            } else null

                            extractChaptersFromRawHtml(
                                relativePath = relativePath,
                                nextRelativePath = nextRelativePath,
                                fullHtml = fullHtml,
                                tagMatches = tagMatches,
                                nextFileTagMatches = nextTagMatches,
                                tocMap = tocFileMap,
                                isFastPath = isFastPathActive
                            )
                        }
                    }
                }.awaitAll()
            }

            var globalIndex = 0
            for (chapterGroup in rawExtractedList) {
                for (ch in chapterGroup) {
                    chapters.add(ch.copy(index = globalIndex++))
                }
            }
        }

        Log.d(TAG, "  🔍 [DETAIL PARSE BREAKDOWN]")
        Log.d(TAG, "     1. Read OPF Structure: ${opfTime}ms (${htmlPathsInSpine.size} files HTML)")
        Log.d(TAG, "     2. Read TOC NCX: ${tocTime}ms (${tocList.size} titles in TOC)")
        Log.d(TAG, "     ⏱️ Total Loop Time (Parallel): ${loopTime}ms")

        Log.d(TAG, "📊 [SUMMARY RAW PARSE] Tổng số chương bóc tách thô trước khi lọc/cắt = ${chapters.size}")

        return chapters
    }

    private fun extractChaptersFromRawHtml(
        relativePath: String,
        nextRelativePath: String,
        fullHtml: String,
        tagMatches: List<Triple<String, String, Int>>,
        nextFileTagMatches: List<Triple<String, String, Int>>?,
        tocMap: Map<String, String>,
        isFastPath: Boolean = false
    ): List<BookChapter> {
        val cleanFileName = relativePath.substringAfterLast("/")
        val titleFromToc = tocMap[cleanFileName] ?: tocMap[relativePath]

        // 🚀 LỐI TẮT BỎ QUA SOI THẺ HTML NẾU ĐÃ CÓ TITLE VÀ ĐANG CHẠY FAST-PATH
        if (isFastPath && titleFromToc != null) {
            return listOf(
                BookChapter(
                    index = 0,
                    title = titleFromToc,
                    path = relativePath,
                    path_next = "",
                    startCharOffset = 0,
                    endCharOffset = -1
                )
            )
        }

        val result = mutableListOf<BookChapter>()
        val rawNodes = mutableListOf<Pair<String, Int>>()

        var tocLinkCount = 0

        for ((tagName, innerHtml, startIdx) in tagMatches) {
            val innerLen = innerHtml.length

            if (innerLen in 3..400) {
                if (isLikelyTocLinkEntry(innerHtml)) {
                    tocLinkCount++
                    continue
                }

                if (hasChapterKeywordFast(innerHtml)) {
                    val rawCleanText = fastStripHtmlTags(innerHtml)
                    val cleanText = cleanChapterTitle(rawCleanText)

                    if (cleanText.length in 3..90 && isStrictChapterTitle(cleanText)) {
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
        var skipNext = false

        for (i in 0 until rawSize) {
            if (skipNext) {
                skipNext = false
                continue
            }

            val currentNode = rawNodes[i]
            if (i < rawSize - 1) {
                val nextNode = rawNodes[i + 1]
                val distance = nextNode.second - currentNode.second

                if (distance in 0..80) {
                    if (currentNode.first.length <= nextNode.first.length) {
                        filteredNodes.add(currentNode)
                        skipNext = true
                    } else {
                        filteredNodes.add(nextNode)
                        skipNext = true
                    }
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

        // Sách gộp (Chứa >= 2 chương trong 1 file HTML)
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

        // 1 chương / File
        val singleTitle = when {
            titleFromToc != null -> titleFromToc
            uniqueNodes.isNotEmpty() -> uniqueNodes.first().first
            else -> extractFirstLineTitle(fullHtml, cleanFileName)
        }

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

    private fun cleanChapterTitle(rawTitle: String): String {
        var clean = rawTitle.lines().firstOrNull { it.isNotBlank() } ?: rawTitle
        clean = clean.trim()

        if (clean.length > 90) {
            val cutIndex = clean.indexOfAny(listOf(".", " - ", ": ", "：", "\t"))
            if (cutIndex in 10..80) {
                clean = clean.substring(0, cutIndex).trim()
            } else {
                clean = clean.take(80).trim()
            }
        }

        return clean
    }

    private fun hasChapterKeywordFast(text: String): Boolean {
        return text.contains("Chương", ignoreCase = true) ||
                text.contains("Chapter", ignoreCase = true) ||
                text.contains("Tập", ignoreCase = true) ||
                text.contains("Vol", ignoreCase = true) ||
                text.contains("Hồi", ignoreCase = true)
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
            val opfStartTime = System.currentTimeMillis()
            val opfFile: File? = cacheFolder.walk().firstOrNull { it.extension.lowercase() == "opf" }

            if (opfFile == null) {
//                Log.w(TAG, "⚠️ [OPF Parser] Không tìm thấy file .opf trong cacheFolder! Chuyển sang quét tất cả file HTML trên đĩa.")
                val fallbackPaths = getAllValidHtmlFilesOnDisk().map { it.relativeTo(cacheFolder).path.replace("\\", "/") }
//                Log.d(TAG, "📁 [OPF Fallback] Đã tìm thấy ${fallbackPaths.size} file HTML trực tiếp từ đĩa.")
                return fallbackPaths
            }

//            Log.d(TAG, "📄 [OPF Parser] Tìm thấy file OPF: ${opfFile.name}")

            val opfContent = opfFile.readText(Charsets.UTF_8)
            val doc = Jsoup.parse(opfContent, "", Parser.xmlParser())

            val opfFolder = if (opfFile.parentFile != cacheFolder) {
                opfFile.parentFile.relativeTo(cacheFolder).path.replace("\\", "/") + "/"
            } else ""

//            Log.d(TAG, "📂 [OPF Parser] Relative OPF Folder path: \"$opfFolder\"")

            val manifestMap = mutableMapOf<String, String>()
            doc.select("manifest > item").forEach { item ->
                val id = item.attr("id")
                val href = item.attr("href")
                if (isHtmlFile(href)) {
                    manifestMap[id] = opfFolder + href
                }
            }
//            Log.d(TAG, "📦 [OPF Parser] Manifest đọc được ${manifestMap.size} item HTML valid.")

            val resultList = mutableListOf<String>()
            var junkCount = 0

            doc.select("spine > itemref").forEach { itemref ->
                val idref = itemref.attr("idref")
                val fullPath = manifestMap[idref]
                if (!fullPath.isNullOrBlank()) {
                    if (!isJunkHtmlFile(fullPath)) {
                        resultList.add(fullPath)
                    } else {
                        junkCount++
                    }
                } else {
//                    Log.w(TAG, "⚠️ [OPF Parser] Spine chứa idref \"$idref\" nhưng không tìm thấy trong Manifest!")
                }
            }

            paths = resultList
            val duration = System.currentTimeMillis() - opfStartTime
//            Log.d(TAG, "✅ [OPF Parser] Hoàn thành đọc Spine trong ${duration}ms: Lấy được ${paths.size} file HTML hợp lệ (Đã lọc bỏ $junkCount file rác/bìa/mục lục).")

        } catch (e: Exception) {
//            Log.e(TAG, "❌ [OPF Parser] Lỗi xảy ra khi parse file OPF: ${e.localizedMessage}", e)
        }

        return if (paths.isNotEmpty()) {
            paths
        } else {
//            Log.w(TAG, "⚠️ [OPF Parser] Danh sách Spine rỗng! Chuyển sang quét tất cả file HTML trên đĩa.")
            val fallbackPaths = getAllValidHtmlFilesOnDisk().map { it.relativeTo(cacheFolder).path.replace("\\", "/") }
//            Log.d(TAG, "📁 [OPF Fallback] Đã tìm thấy ${fallbackPaths.size} file HTML trực tiếp từ đĩa.")
            fallbackPaths
        }
    }

    private fun parseTocList(): List<Pair<String, String>> {
        val titleList = mutableListOf<Pair<String, String>>()
        try {
            val ncxFile = cacheFolder.walk().firstOrNull { it.extension.lowercase() == "ncx" }

            if (ncxFile != null && ncxFile.exists()) {
                val contentText = ncxFile.readText(Charsets.UTF_8)
                val doc = Jsoup.parse(contentText, "", Parser.xmlParser())
                val navPoints = doc.select("navPoint")

                navPoints.forEach { navPoint ->
                    val title = navPoint.select("navLabel > text").first()?.text()?.trim()
                        ?: navPoint.select("text").first()?.text()?.trim()
                        ?: ""

                    val src = navPoint.select("> content").first()?.attr("src")?.trim()
                        ?: navPoint.select("content").first()?.attr("src")?.trim()
                        ?: ""

                    if (src.isNotBlank() && title.isNotBlank()) {
                        titleList.add(Pair(src, title))
                    }
                }

//                Log.d("EpubParser", "✅ Parse TOC xong: ${titleList.size} title")
            } else {
//                Log.e("EpubParser", "❌ KHÔNG TÌM THẤY FILE .NCX TRONG CACHE FOLDER!")
            }
        } catch (e: Exception) {
//            Log.e("EpubParser", "❌ LỖI PARSE TOC NCX: ${e.message}")
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