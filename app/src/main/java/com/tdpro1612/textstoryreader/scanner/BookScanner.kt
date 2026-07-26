package com.tdpro1612.textstoryreader.scanner

import com.tdpro1612.textstoryreader.database.BookDatabaseQueries
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.database.BookStatus
import com.tdpro1612.textstoryreader.scanner.parsers.ParsedMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

object BookScanner {

    private const val BATCH_SIZE = 500

    suspend fun scanDirectory(
        targetFolder: File,
        dbQueries: BookDatabaseQueries,
        progressFlow: MutableStateFlow<ScanProgressState>
    ) {
        if (!targetFolder.exists() || !targetFolder.isDirectory) {
            progressFlow.value = ScanProgressState.Error("Thư mục không tồn tại")
            return
        }

        val dbBooks = dbQueries.getAllBooksForScan()
        val dbBooksMap = dbBooks.associateBy { it.filePath }

        // 👉 Lấy danh sách đuôi file được hỗ trợ TỰ ĐỘNG từ Factory
        val supportedExtensions = BookParserFactory.getSupportedExtensions()

        val diskFiles = targetFolder.walkTopDown()
            .filter { it.isFile && supportedExtensions.contains(it.extension.uppercase()) }
            .toList()

        val totalFiles = diskFiles.size
        val diskFilePathsSet = diskFiles.map { it.absolutePath }.toHashSet()

        val newBooksToInsert = mutableListOf<BookEntity>()
        val updatedBooksToUpdate = mutableListOf<BookEntity>()
        var scannedCount = 0
        var totalNew = 0
        var totalUpdated = 0

        for (file in diskFiles) {
            scannedCount++
            val path = file.absolutePath
            val currentSize = file.length()
            val currentLastModified = file.lastModified()

            val existingBook = dbBooksMap[path]

            if (existingBook == null) {
                val newBook = parseBookFromFile(file)
                newBooksToInsert.add(newBook)
                totalNew++
            } else if (existingBook.fileSize != currentSize || existingBook.lastModified != currentLastModified) {
                val updatedBook = existingBook.copy(
                    fileSize = currentSize,
                    lastModified = currentLastModified
                )
                updatedBooksToUpdate.add(updatedBook)
                totalUpdated++
            }

            if (newBooksToInsert.size >= BATCH_SIZE) {
                dbQueries.insertBooks(newBooksToInsert)
                newBooksToInsert.clear()
            }

            progressFlow.value = ScanProgressState.Scanning(scannedCount, totalFiles)
        }

        if (newBooksToInsert.isNotEmpty()) {
            dbQueries.insertBooks(newBooksToInsert)
        }

        if (updatedBooksToUpdate.isNotEmpty()) {
            updatedBooksToUpdate.forEach { dbQueries.updateBook(it) }
        }

        val deletedBooksToDelete = dbBooks.filter { !diskFilePathsSet.contains(it.filePath) }
        if (deletedBooksToDelete.isNotEmpty()) {
            dbQueries.deleteMultipleBooks(deletedBooksToDelete)
        }

        progressFlow.value = ScanProgressState.Finished(totalNew, totalUpdated, deletedBooksToDelete.size)
    }

    private fun parseBookFromFile(file: File): BookEntity {
        val extension = file.extension.uppercase()

        // 👉 Nhờ Factory lấy Parser phù hợp
        val parser = BookParserFactory.getParser(file)
        val metadata = parser?.parseHeader(file) ?: ParsedMetadata(title = file.nameWithoutExtension)

        return BookEntity(
            title = metadata.title,
            author = metadata.author,
            filePath = file.absolutePath,
            fileType = extension,
            fileSize = file.length(),
            lastModified = file.lastModified(),
            coverPath = null,
            tags = metadata.tags,
            status = metadata.status,
            lastChapterIndex = 0,
            lastPosition = 0,
            readProgress = 0f,
            addedTime = System.currentTimeMillis(),
            lastReadTime = 0,
            isFavorite = false
        )
    }
}