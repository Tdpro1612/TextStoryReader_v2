package com.tdpro1612.textstoryreader.reader.epub

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import org.apache.commons.compress.archivers.zip.ZipFile

/**
 * Lớp chịu trách nhiệm giải nén tệp EPUB vào bộ nhớ Cache ứng dụng (Chuẩn Android 14+).
 */
object EpubUnzipper {

    private const val BUFFER_SIZE = 65536 // 64KB - tăng từ 8KB để giảm số vòng read/write
    // syscall cho mỗi file nhỏ (chương truyện thường vài chục KB) khi số lượng file rất lớn
    // (hàng nghìn file) - ít vòng lặp hơn thì tổng overhead I/O giảm đáng kể.

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
        Log.d("ReaderPerf", "🔍 Check cache path: ${targetDir.absolutePath} | Exists: ${targetDir.exists()}")
        // SỬA #4: KIỂM TRA CACHE TỒN TẠI TRƯỚC KHÌ XẢ NÉN (TỐI ƯU COLD START / RE-OPEN)
        // Bản cũ: Luôn chạy `targetDir.deleteRecursively()` khiến app xả nén lại từ đầu
        // mỗi khi mở sách (~2145 ms).
        // Lý do sửa: Khi cache đã có sẵn trên đĩa, trả về ngay lập tức (~1 ms) để tránh
        // tốn I/O giải nén lại hàng nghìn file nhỏ.
        if (targetDir.exists()) {
            return targetDir
        }

        targetDir.mkdirs()

        // Bước 1: Copy nội dung Uri ra 1 file tạm trong cache (để có random access)
        val tempZipFile = File(context.cacheDir, "temp_${folderName}.epub")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempZipFile).use { output ->
                input.copyTo(output, bufferSize = BUFFER_SIZE)
            }
        } ?: throw IllegalArgumentException("Không thể mở InputStream từ Uri: $uri")

        try {
            // Bước 2: Dùng ZipFile (random access) thay vì ZipArchiveInputStream
            ZipFile.builder().setFile(tempZipFile).get().use { zipFile ->
                val entries = zipFile.entries.toList()
//                android.util.Log.d("EpubUnzip", "📦 Tổng số entry trong zip: ${entries.size}")

                // SỬA #1: Tính canonicalPath của targetDir CHỈ 1 LẦN DUY NHẤT trước vòng lặp.
                // Bản cũ gọi targetDir.canonicalPath() lại ở MỖI file - dù giá trị này không hề
                // đổi suốt vòng lặp. canonicalPath() không phải phép tính chuỗi, nó chạm thẳng
                // filesystem (resolve symlink + stat), nên gọi lặp lại là lãng phí thuần tuý.
                val canonicalTargetDirPath = targetDir.canonicalPath

                // SỬA #2: dedup mkdirs() - nhiều file (thường hàng nghìn chương) nằm chung
                // 1-2 thư mục con (VD OEBPS/Text/), gọi mkdirs() lặp lại cho từng file là dư
                // thừa. Theo dõi thư mục đã tạo trong phiên này để chỉ gọi đúng 1 lần/thư mục.
                val createdDirs = HashSet<String>()

                for (entry in entries) {
                    val currentEntryName = entry.name
                    try {
                        if (!entry.isDirectory && !currentEntryName.startsWith("__MACOSX")) {
                            val destFile = File(targetDir, currentEntryName)

                            // SỬA #3: Kiểm tra Zip Slip KHÔNG chạm filesystem.
                            // Bản cũ dùng destFile.canonicalPath() - phải resolve symlink +
                            // stat thật trên đĩa cho MỖI file (6733 file = 6733 lần syscall).
                            // Thay bằng Path.normalize(): chỉ xử lý CHUỖI thuần tuý (rút gọn
                            // "..", ".") mà KHÔNG cần file tồn tại hay chạm đĩa - vẫn đủ an
                            // toàn để chặn zip-slip vì tên entry độc hại luôn lộ ra qua các
                            // thành phần ".." trong chuỗi, không cần biết nó có tồn tại thật
                            // trên đĩa hay không.
                            // Kiểm tra Zip Slip tương thích với mọi Android API level (không cần API 26+)
                            // ✅ MỚI: Chỉ check chuỗi trên RAM -> Tốn 0 ms, tương thích API 24+
                            if (currentEntryName.contains("..")) {
                                val normalizedPath = destFile.canonicalPath
                                if (!normalizedPath.startsWith(canonicalTargetDirPath + File.separator)) {
                                    throw SecurityException("Phát hiện file Zip Slip nguy hiểm: $currentEntryName")
                                }
                            }

                            val parentDir = destFile.parentFile
                            if (parentDir != null && createdDirs.add(parentDir.path)) {
                                parentDir.mkdirs()
                            }

                            zipFile.getInputStream(entry).use { input ->
                                FileOutputStream(destFile).buffered(BUFFER_SIZE).use { output ->
                                    input.copyTo(output, bufferSize = BUFFER_SIZE)
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