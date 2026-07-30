package com.tdpro1612.textstoryreader.scanner

/**
 * Đai diện cho trạng thái của tiến trình Quét (Scan) file sách.
 */
sealed class ScanProgressState {

    /**
     * Trạng thái rảnh rỗi (Chưa hoặc không thực hiện quét).
     */
    data object Idle : ScanProgressState()

    /**
     * Trạng thái đang tiến hành quét file.
     * @param scannedCount Số file đã xử lý/đối chiếu xong.
     * @param totalFiles Tổng số file hỗ trợ tìm thấy trên ổ cứng.
     */
    data class Scanning(
        val scannedCount: Int = 0,
        val totalFiles: Int = 0
    ) : ScanProgressState()

    /**
     * Trạng thái đã hoàn thành quét lượt này.
     * @param newBooksCount Số lượng sách mới tinh vừa được thêm vào DB.
     * @param updatedBooksCount Số lượng sách cũ vừa được cập nhật nội dung/dung lượng.
     * @param deletedBooksCount Số lượng sách đã bị xóa khỏi ổ cứng nên bị xóa khỏi DB.
     */
    data class Finished(
        val newBooksCount: Int = 0,
        val updatedBooksCount: Int = 0,
        val deletedBooksCount: Int = 0
    ) : ScanProgressState()

    /**
     * Trạng thái gặp lỗi (Ví dụ: Không có quyền truy cập bộ nhớ, thư mục không tồn tại...).
     * @param message Thông báo lỗi chi tiết.
     */
    data class Error(val message: String) : ScanProgressState()
}