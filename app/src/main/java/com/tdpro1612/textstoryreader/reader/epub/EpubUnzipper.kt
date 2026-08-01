package com.tdpro1612.textstoryreader.reader.epub

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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
        // Tạo tên thư mục duy nhất dựa trên Uri của sách
        val folderName = "epub_cache_" + uri.toString().hashCode()
        val targetDir = File(context.cacheDir, folderName)

        // Nếu đã tồn tại cache cũ của cuốn này -> Xóa đi để giải nén mới cho sạch
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()

        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Không thể mở InputStream từ Uri: $uri")

        ZipInputStream(inputStream.buffered()).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            val buffer = ByteArray(BUFFER_SIZE)

            while (entry != null) {
                // Lọc bỏ thư mục ẩn hoặc tệp hệ thống không cần thiết (__MACOSX, .DS_Store...)
                if (!entry.isDirectory && !entry.name.startsWith("__MACOSX")) {
                    val destFile = File(targetDir, entry.name)

                    // 🔒 KIỂM TRA BẢO MẬT ZIP SLIP (Bắt buộc cho Android 14+)
                    val canonicalDestPath = destFile.canonicalPath
                    val canonicalTargetDirPath = targetDir.canonicalPath
                    if (!canonicalDestPath.startsWith(canonicalTargetDirPath + File.separator)) {
                        throw SecurityException("Phát hiện file Zip Slip nguy hiểm: ${entry.name}")
                    }

                    // Tạo sẵn thư mục cha nếu file nằm trong subfolder (ví dụ: OEBPS/Text/chap1.html)
                    destFile.parentFile?.mkdirs()

                    // Ghi file ra ổ đĩa Cache
                    FileOutputStream(destFile).buffered().use { out ->
                        var bytesRead: Int
                        while (zip.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

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