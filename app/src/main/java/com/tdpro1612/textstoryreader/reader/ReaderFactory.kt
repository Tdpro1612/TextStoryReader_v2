package com.tdpro1612.textstoryreader.reader

import android.net.Uri

object ReaderFactory {

    private val txtReader by lazy { TxtContentReader() }

    private val epubReader by lazy { EpubContentReader() }

    fun getReader(uri: Uri): BookContentReader {

        val fileName = (uri.lastPathSegment ?: uri.path ?: "")
            .substringAfterLast('/')
            .lowercase()

        return when {
            fileName.endsWith(".txt") ->
                txtReader

            fileName.endsWith(".epub") ->
                epubReader

            else ->
                throw UnsupportedOperationException(
                    "Không hỗ trợ định dạng: $fileName"
                )
        }
    }
}