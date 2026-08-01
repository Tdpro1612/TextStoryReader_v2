package com.tdpro1612.textstoryreader.reader.epub

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * Lớp chịu trách nhiệm giải nén tệp EPUB vào bộ nhớ Cache ứng dụng (Chuẩn Android 14+).
 */
object EpubUnzipper {

    private const val BUFFER_SIZE = 8192 // Buffer 8KB tối ưu I/O

    /**
     * Giải nén toàn bộ tệp .epub vào một thư mục tạm trong cacheDir.
     *
     * @param context Context ứng dụng
     * @param uri Uri của tệp EPUB
     * @return Thư mục [File] chứa toàn bộ nội dung đã xả nén
     */
    fun unzipEpubToCache(context: Context, uri: Uri): File {
        val folderName = "epub_cache_" + uri.toString().hashCode()
        val targetDir = File(context.cacheDir, folderName)

        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        // Bước 1: Copy nội dung Uri ra 1 file tạm trong cache (để có random access)
        val tempZipFile = File(context.cacheDir, "temp_${folderName}.epub")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempZipFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Không thể mở InputStream từ Uri: $uri")

        try {
            // Bước 2: Dùng ZipFile (random access) thay vì ZipArchiveInputStream
            ZipFile.builder().setFile(tempZipFile).get().use { zipFile ->
                val entries = zipFile.entries.toList()
//                android.util.Log.d("EpubUnzip", "📦 Tổng số entry trong zip: ${entries.size}")

                for (entry in entries) {
                    val currentEntryName = entry.name
                    try {
                        if (!entry.isDirectory && !currentEntryName.startsWith("__MACOSX")) {
                            val destFile = File(targetDir, currentEntryName)
                            val canonicalDestPath = destFile.canonicalPath
                            val canonicalTargetDirPath = targetDir.canonicalPath
                            if (!canonicalDestPath.startsWith(canonicalTargetDirPath + File.separator)) {
                                throw SecurityException("Phát hiện file Zip Slip nguy hiểm: $currentEntryName")
                            }
                            destFile.parentFile?.mkdirs()

                            zipFile.getInputStream(entry).use { input ->
                                FileOutputStream(destFile).buffered().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } catch (e: Exception) {
//                        android.util.Log.e("EpubUnzip", "💥 LỖI entry '$currentEntryName': ${e.javaClass.simpleName} - ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
//            android.util.Log.e("EpubUnzip", "💥💥 LỖI KHI MỞ ZIP: ${e.javaClass.simpleName} - ${e.message}")
            e.printStackTrace()
        } finally {
            // Bước 3: Dọn file tạm
            tempZipFile.delete()
        }

//        android.util.Log.d("EpubUnzip", "✅ HOÀN TẤT giải nén, số file trong targetDir: ${targetDir.walk().count { it.isFile }}")
        return targetDir
    }

    /**
     * Dọn dẹp dĩ án, xóa thư mục Cache tạm của một cuốn sách khi đóng ứng dụng / thoát màn hình đọc.
     */
    fun clearCache(cacheFolder: File?) {
        try {
            if (cacheFolder != null && cacheFolder.exists()) {
                cacheFolder.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dọn dẹp toàn bộ tất cả thư mục cache sách cũ còn tồn đọng trong cacheDir (nếu có).
     * Có thể gọi hàm này khi mở App ở màn hình Splash.
     */
    fun clearAllEpubCache(context: Context) {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("epub_cache_")) {
                    file.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}