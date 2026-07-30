package com.tdpro1612.textstoryreader.scanner

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.tdpro1612.textstoryreader.database.BookDatabaseQueries
import com.tdpro1612.textstoryreader.database.BookEntity
import com.tdpro1612.textstoryreader.database.BookStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

object BookScanner {

    private const val BATCH_SIZE = 20
    private const val PARALLEL_CONCURRENCY = 8

    suspend fun scanDirectory(
        context: Context,
        targetFolderPath: String,
        dbQueries: BookDatabaseQueries,
        progressFlow: MutableStateFlow<ScanProgressState>
    ) = withContext(Dispatchers.IO) {

        val supportedExtensions = BookParserFactory.getSupportedExtensions()

        // Lấy danh sách sách hiện có trong DB (nếu có) để so sánh
        val dbBooks = dbQueries.getAllBooksForScan()
        val dbBooksMap = dbBooks.associateBy { it.filePath }

        val treeUri = Uri.parse(targetFolderPath)
        val filesToProcess = queryFilesFromTreeUri(context, treeUri, supportedExtensions)
        val totalFiles = filesToProcess.size

        if (totalFiles == 0) {
            progressFlow.value = ScanProgressState.Finished(0, 0, 0)
            return@withContext
        }

        val diskFilePathsSet = HashSet<String>(totalFiles)
        val newBooksToInsert = mutableListOf<BookEntity>()
        val updatedBooksToUpdate = mutableListOf<BookEntity>()

        val scannedCount = AtomicInteger(0)
        var totalNew = 0
        var totalUpdated = 0

        filesToProcess.chunked(PARALLEL_CONCURRENCY).forEach { chunk ->

            val parsedResults = chunk.map { fileInfo ->
                async(Dispatchers.IO) {
                    val path = fileInfo.uri.toString()
                    val existingBook = dbBooksMap[path]

                    var resultInsert: BookEntity? = null
                    var resultUpdate: BookEntity? = null

                    if (existingBook == null) {
                        // Bỏ qua parseHeader giúp tốc độ quét nhanh x5-x10 lần
                        resultInsert = BookEntity(
                            title = fileInfo.nameWithoutExtension,
                            author = "Không rõ",
                            filePath = path,
                            fileType = fileInfo.extension,
                            fileSize = fileInfo.size,
                            lastModified = fileInfo.lastModified,
                            coverPath = null,
                            tags = "",
                            status = BookStatus.ONGOING,
                            lastChapterIndex = 0,
                            lastPosition = 0,
                            readProgress = 0f,
                            addedTime = System.currentTimeMillis(),
                            lastReadTime = 0,
                            isFavorite = false
                        )

                    } else if (existingBook.fileSize != fileInfo.size || existingBook.lastModified != fileInfo.lastModified) {
                        resultUpdate = existingBook.copy(
                            fileSize = fileInfo.size,
                            lastModified = fileInfo.lastModified
                        )
                    }

                    Triple(path, resultInsert, resultUpdate)
                }
            }.awaitAll()

            for (res in parsedResults) {
                diskFilePathsSet.add(res.first)
                res.second?.let {
                    newBooksToInsert.add(it)
                    totalNew++
                }
                res.third?.let {
                    updatedBooksToUpdate.add(it)
                    totalUpdated++
                }
            }

            val currentScanned = scannedCount.addAndGet(chunk.size)

            // BATCH INSERT: Lưu theo từng đợt BATCH_SIZE
            if (newBooksToInsert.size >= BATCH_SIZE) {
                dbQueries.insertBooks(newBooksToInsert.toList())
                newBooksToInsert.clear()
            }

            if (updatedBooksToUpdate.size >= BATCH_SIZE) {
                updatedBooksToUpdate.forEach { dbQueries.updateBook(it) }
                updatedBooksToUpdate.clear()
            }

            // Báo UI tiến trình quét
            progressFlow.value = ScanProgressState.Scanning(currentScanned, totalFiles)
        }

        // Lưu toàn bộ phần dư còn lại
        if (newBooksToInsert.isNotEmpty()) {
            dbQueries.insertBooks(newBooksToInsert.toList())
            newBooksToInsert.clear()
        }

        if (updatedBooksToUpdate.isNotEmpty()) {
            updatedBooksToUpdate.forEach { dbQueries.updateBook(it) }
            updatedBooksToUpdate.clear()
        }

        // Dọn dẹp sách không còn tồn tại trên thẻ nhớ
        val deletedBooksToDelete = dbBooks.filter { !diskFilePathsSet.contains(it.filePath) }
        if (deletedBooksToDelete.isNotEmpty()) {
            dbQueries.deleteMultipleBooks(deletedBooksToDelete)
        }

        progressFlow.value = ScanProgressState.Finished(totalNew, totalUpdated, deletedBooksToDelete.size)
    }

    /**
     * Hỗ trợ duyệt file từ SAF Tree Uri (Bao gồm quét thư mục con/Subdirectories nếu có)
     */
    private fun queryFilesFromTreeUri(
        context: Context,
        treeUri: Uri,
        supportedExtensions: Set<String>
    ): List<SafFileInfo> {
        val fileList = mutableListOf<SafFileInfo>()
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        val dirStack = java.util.ArrayDeque<String>()
        dirStack.push(rootDocumentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        while (dirStack.isNotEmpty()) {
            val docId = dirStack.pop()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

            try {
                context.contentResolver.query(
                    childrenUri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val dateColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val mimeType = cursor.getString(mimeColumn)
                        val childDocId = cursor.getString(idColumn)

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            dirStack.push(childDocId)
                            continue
                        }

                        val name = cursor.getString(nameColumn) ?: continue
                        val ext = name.substringAfterLast('.', "").uppercase()

                        if (supportedExtensions.contains(ext)) {
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                            val size = cursor.getLong(sizeColumn)
                            val lastModified = cursor.getLong(dateColumn)

                            fileList.add(
                                SafFileInfo(
                                    uri = fileUri,
                                    nameWithoutExtension = name.substringBeforeLast('.'),
                                    extension = ext,
                                    size = size,
                                    lastModified = lastModified
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return fileList
    }

    private data class SafFileInfo(
        val uri: Uri,
        val nameWithoutExtension: String,
        val extension: String,
        val size: Long,
        val lastModified: Long
    )
}