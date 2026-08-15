package com.tdpro1612.textstoryreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.settings.ReadMode
import com.tdpro1612.textstoryreader.settings.ReaderSettings
import com.tdpro1612.textstoryreader.settings.toComposeFontFamily
import kotlin.math.roundToInt

@Composable
fun ReaderContentView(
    content: String,
    currentChapter: BookChapter,
    totalChapters: Int,
    currentChapterIndex: Int,
    readProgress: Float, // Tiến độ % toàn bộ cuốn sách (0.0f -> 100.0f)
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    settings: ReaderSettings,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onProgressChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(settings.themePreset.backgroundColorHex)
    val textColor = Color(settings.themePreset.textColorHex)
    val textStyle = TextStyle(
        fontSize = settings.fontSizeSp.sp,
        fontFamily = settings.fontFamily.toComposeFontFamily(),
        lineHeight = (settings.fontSizeSp * settings.lineHeightMultiplier).sp,
        color = textColor
    )

    // 🔥 Tự động quy đổi readProgress tổng (0..100%) về % nội bộ chương (0.0f -> 1.0f)
    val intraProgress = remember(readProgress, currentChapterIndex, totalChapters) {
        if (totalChapters > 0) {
            ((readProgress / 100f * totalChapters) - currentChapterIndex).coerceIn(0f, 1f)
        } else 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when (settings.readMode) {
            ReadMode.SCROLL -> {
                ScrollReaderView(
                    content = content,
                    chapterTitle = currentChapter.title,
                    intraProgress = intraProgress,
                    textStyle = textStyle,
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    onNextChapter = onNextChapter,
                    onPreviousChapter = onPreviousChapter,
                    onProgressChanged = onProgressChanged
                )
            }
            ReadMode.PAGE_FLIP -> {
                PageFlipReaderView(
                    chapterTitle = currentChapter.title,
                    content = content,
                    intraProgress = intraProgress,
                    textStyle = textStyle,
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    onNextChapter = onNextChapter,
                    onPreviousChapter = onPreviousChapter,
                    onProgressChanged = onProgressChanged
                )
            }
        }
    }
}

@Composable
private fun ScrollReaderView(
    content: String,
    chapterTitle: String,
    intraProgress: Float,
    textStyle: TextStyle,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onProgressChanged: (Float) -> Unit
) {
    val scrollState = rememberScrollState()
    var isRestored by remember(chapterTitle) { mutableStateOf(false) }

    // 🔥 Khôi phục vị trí cuộn từ intraProgress
    LaunchedEffect(chapterTitle, scrollState.maxValue) {
        if (scrollState.maxValue > 0 && !isRestored) {
            val targetPx = (scrollState.maxValue * intraProgress).toInt()
            scrollState.scrollTo(targetPx)
            isRestored = true
        }
    }

    // Lắng nghe hành vi cuộn để tính % tiến độ mới
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }
            .collect { currentPx ->
                if (scrollState.maxValue > 0 && isRestored) {
                    val progress = currentPx.toFloat() / scrollState.maxValue.toFloat()
                    onProgressChanged(progress.coerceIn(0f, 1f))
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (hasPreviousChapter) {
            OutlinedButton(
                onClick = onPreviousChapter,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text("◄ Chương trước")
            }
        }

        Text(
            text = chapterTitle,
            style = textStyle.copy(fontWeight = FontWeight.Bold, fontSize = (textStyle.fontSize.value + 4).sp),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = content,
            style = textStyle,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasNextChapter) {
                Button(
                    onClick = onNextChapter,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chương tiếp theo ►")
                }
            } else {
                Text(
                    text = "Bạn đã đọc hết chương cuối cùng",
                    style = textStyle.copy(fontSize = 14.sp)
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PageFlipReaderView(
    chapterTitle: String,
    content: String,
    intraProgress: Float,
    textStyle: TextStyle,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onProgressChanged: (Float) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val horizontalPaddingPx = with(density) { 32.dp.toPx() }
        val verticalPaddingPx = with(density) { 30.dp.toPx() }

        val maxAvailableWidth = (maxWidth.value * density.density - horizontalPaddingPx).coerceAtLeast(100f)
        val maxAvailableHeight = (maxHeight.value * density.density - verticalPaddingPx).coerceAtLeast(100f)

        val pages = remember(content, chapterTitle, textStyle, maxAvailableWidth, maxAvailableHeight) {
            paginateText(
                chapterTitle = chapterTitle,
                content = content,
                textStyle = textStyle,
                widthPx = maxAvailableWidth.toInt(),
                heightPx = maxAvailableHeight.toInt(),
                textMeasurer = textMeasurer
            )
        }

        val totalContentPages = pages.size
        val offset = if (hasPreviousChapter) 1 else 0
        val totalPages = totalContentPages + offset + (if (hasNextChapter) 1 else 0)

        // 🔥 Khôi phục trang ban đầu từ intraProgress
        val initialContentPage = if (totalContentPages > 1) {
            (intraProgress * (totalContentPages - 1)).roundToInt()
        } else 0

        val pagerState = rememberPagerState(
            initialPage = (initialContentPage + offset).coerceIn(0, totalPages - 1),
            pageCount = { totalPages }
        )

        // Lắng nghe chuyển trang để tính % tiến độ mới
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    val effectivePage = page - offset
                    if (effectivePage in pages.indices) {
                        val progress = if (totalContentPages > 1) {
                            effectivePage.toFloat() / (totalContentPages - 1).toFloat()
                        } else 0f
                        onProgressChanged(progress.coerceIn(0f, 1f))
                    }
                }
        }

        LaunchedEffect(pagerState.currentPage) {
            if (hasPreviousChapter && pagerState.currentPage == 0) {
                onPreviousChapter()
            } else if (hasNextChapter && pagerState.currentPage == totalPages - 1) {
                onNextChapter()
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val effectiveContentIndex = pageIndex - offset

            when {
                hasPreviousChapter && pageIndex == 0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OutlinedButton(onClick = onPreviousChapter) {
                            Text("◄ Vuốt hoặc bấm để về Chương trước")
                        }
                    }
                }
                hasNextChapter && pageIndex == totalPages - 1 -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(onClick = onNextChapter) {
                            Text("Vuốt hoặc bấm để sang Chương tiếp ►")
                        }
                    }
                }
                else -> {
                    val pageText = pages.getOrNull(effectiveContentIndex) ?: buildAnnotatedString { }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 2.dp)
                    ) {
                        Text(
                            text = pageText,
                            style = textStyle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopStart)
                        )

                        Text(
                            text = "${effectiveContentIndex + 1} / ${pages.size}",
                            style = textStyle.copy(
                                fontSize = 10.sp,
                                color = textStyle.color.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 0.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun paginateText(
    chapterTitle: String,
    content: String,
    textStyle: TextStyle,
    widthPx: Int,
    heightPx: Int,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
): List<AnnotatedString> {
    if (content.isBlank() && chapterTitle.isBlank()) return listOf(buildAnnotatedString { })

    val pages = mutableListOf<AnnotatedString>()

    val titleStyle = textStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = (textStyle.fontSize.value + 3).sp
    )

    var isFirstPage = true
    var remainingText = content

    while (remainingText.isNotEmpty() || isFirstPage) {
        val currentAnnotatedText = buildAnnotatedString {
            if (isFirstPage && chapterTitle.isNotBlank()) {
                pushStyle(titleStyle.toSpanStyle())
                append(chapterTitle)
                pop()
                append("\n\n")
            }
            pushStyle(textStyle.toSpanStyle())
            append(remainingText)
            pop()
        }

        val result = textMeasurer.measure(
            text = currentAnnotatedText,
            style = textStyle,
            constraints = Constraints(maxWidth = widthPx)
        )

        if (result.size.height <= heightPx) {
            pages.add(currentAnnotatedText)
            break
        }

        var lineIndex = result.lineCount - 1
        while (lineIndex > 0 && result.getLineBottom(lineIndex) > heightPx) {
            lineIndex--
        }

        if (lineIndex <= 0) {
            val cutIndex = (remainingText.length / 2).coerceAtLeast(1)
            pages.add(buildAnnotatedString { append(remainingText.substring(0, cutIndex)) })
            remainingText = remainingText.substring(cutIndex)
            isFirstPage = false
            continue
        }

        val visibleEndOffset = result.getLineEnd(lineIndex)
        val titleLengthOffset = if (isFirstPage && chapterTitle.isNotBlank()) chapterTitle.length + 2 else 0
        val actualEndOffset = (visibleEndOffset - titleLengthOffset).coerceIn(0, remainingText.length)

        var breakOffset = actualEndOffset

        for (i in actualEndOffset downTo (actualEndOffset - 50).coerceAtLeast(0)) {
            if (i < remainingText.length && (remainingText[i] == '\n' || remainingText[i] == ' ')) {
                breakOffset = i + 1
                break
            }
        }

        if (breakOffset <= 0) breakOffset = actualEndOffset.coerceAtLeast(1)

        val pageContentStr = remainingText.substring(0, breakOffset).trimEnd()

        pages.add(buildAnnotatedString {
            if (isFirstPage && chapterTitle.isNotBlank()) {
                pushStyle(titleStyle.toSpanStyle())
                append(chapterTitle)
                pop()
                append("\n\n")
            }
            pushStyle(textStyle.toSpanStyle())
            append(pageContentStr)
            pop()
        })

        remainingText = remainingText.substring(breakOffset).trimStart()
        isFirstPage = false
    }

    return if (pages.isEmpty()) listOf(buildAnnotatedString { append(chapterTitle) }) else pages
}