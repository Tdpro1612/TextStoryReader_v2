package com.tdpro1612.textstoryreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tdpro1612.textstoryreader.reader.BookChapter
import com.tdpro1612.textstoryreader.settings.ReadMode
import com.tdpro1612.textstoryreader.settings.ReaderSettings
import com.tdpro1612.textstoryreader.settings.toComposeFontFamily

@Composable
fun ReaderContentView(
    content: String,
    currentChapter: BookChapter,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    settings: ReaderSettings,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
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
                    textStyle = textStyle,
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    onNextChapter = onNextChapter,
                    onPreviousChapter = onPreviousChapter
                )
            }
            ReadMode.PAGE_FLIP -> {
                PageFlipReaderView(
                    content = content,
                    chapterTitle = currentChapter.title,
                    textStyle = textStyle,
                    hasPreviousChapter = hasPreviousChapter,
                    hasNextChapter = hasNextChapter,
                    onNextChapter = onNextChapter,
                    onPreviousChapter = onPreviousChapter
                )
            }
        }
    }
}

@Composable
private fun ScrollReaderView(
    content: String,
    chapterTitle: String,
    textStyle: TextStyle,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit
) {
    val scrollState = rememberScrollState()

    // ⚡ Reset cuộn lên đầu trang mỗi khi đổi chương
    LaunchedEffect(chapterTitle) {
        scrollState.scrollTo(0)
    }

    // Bắt sự kiện cuộn xuống đáy / lên đỉnh
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (current, max) ->
                if (max > 0 && current >= max) {
                    // Cuộn tới cuối chương
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Nút chuyển chương trước ở đầu nếu có
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

        // Card chuyển chương ở cuối
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
    content: String,
    chapterTitle: String,
    textStyle: TextStyle,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit
) {
    // Tách nội dung chương thành các đoạn trang
    val pages = rememberPages(content, chapterTitle)

    // Tổng số trang = [Thẻ Trang Trước] (nếu có) + [Các trang nội dung] + [Thẻ Trang Sau] (nếu có)
    val totalPages = pages.size + (if (hasPreviousChapter) 1 else 0) + (if (hasNextChapter) 1 else 0)
    val pagerState = rememberPagerState(initialPage = if (hasPreviousChapter) 1 else 0, pageCount = { totalPages })

    // ⚡ RESET TRANG VỀ ĐẦU CHƯƠNG MỚI: Tránh văng/lặp chương do lệch chỉ số trang giữa các chương
    LaunchedEffect(chapterTitle) {
        val startPage = if (hasPreviousChapter) 1 else 0
        pagerState.scrollToPage(startPage)
    }

    // Tự động lật sang chương mới / chương cũ khi chạm trang đầu/cuối
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
        val effectiveContentIndex = pageIndex - (if (hasPreviousChapter) 1 else 0)

        when {
            // Trang đệm quay lại chương trước
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
            // Trang đệm chuyển sang chương tiếp theo
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
            // Trang nội dung
            else -> {
                val pageText = pages.getOrNull(effectiveContentIndex) ?: ""
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = pageText,
                        style = textStyle,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${effectiveContentIndex + 1} / ${pages.size}",
                        style = textStyle.copy(fontSize = 12.sp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

/**
 * Hàm phân chia text thành các đoạn nhỏ trang lật
 */
private fun rememberPages(content: String, title: String): List<String> {
    if (content.isEmpty()) return listOf(title)

    // Phân đoạn theo chuỗi dài ~800 ký tự cho mỗi trang
    val chunkSize = 800
    val chunks = content.chunked(chunkSize)
    return listOf("$title\n\n${chunks.firstOrNull() ?: ""}") + chunks.drop(1)
}