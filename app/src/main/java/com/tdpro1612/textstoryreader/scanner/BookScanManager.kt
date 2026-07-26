package com.tdpro1612.textstoryreader.scanner

import android.content.Context
import com.tdpro1612.textstoryreader.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Singleton quản lý luồng quét sách ngầm (Background Scan Thread).
 * Nơi tiếp nhận yêu cầu Quét/Refresh từ UI và đảm bảo chỉ có DUY NHẤT 1 luồng chạy ngầm.
 */
object BookScanManager {

    // Luồng quản lý Coroutine riêng cho việc Scan (Chạy trên Dispatchers.IO)
    private val scanScope = CoroutineScope(Dispatchers.IO)

    // Lưu trữ Job đang chạy ngầm
    private var scanJob: Job? = null

    // Quản lý trạng thái Quét gửi ra ngoài cho UI (StateFlow)
    private val _scanState = MutableStateFlow<ScanProgressState>(ScanProgressState.Idle)
    val scanState: StateFlow<ScanProgressState> = _scanState.asStateFlow()

    // Cấu hình chống Spam Refresh
    private var lastScanTime = 0L
    private const val REFRESH_COOLDOWN_MS = 3000L // Khoảng cách tối thiểu giữa 2 lần Refresh (3 giây)

    /**
     * Kích hoạt tiến trình Quét/Refresh thư viện.
     *
     * @param context Context ứng dụng
     * @param targetFolderPath Đường dẫn thư mục cần quét (Ví dụ: Thẻ nhớ hoặc Folder truyện)
     * @param isUserInitiated True nếu người dùng bấm nút Refresh/Vuốt màn hình (để check cooldown)
     */
    fun startScan(
        context: Context,
        targetFolderPath: String,
        isUserInitiated: Boolean = false
    ) {
        val currentTime = System.currentTimeMillis()

        // 🔒 LỚP PHÒNG THỦ 1: Kiểm tra xem Job trước có đang chạy ngầm không
        if (scanJob?.isActive == true) {
            return // Bỏ qua nếu lượt quét trước chưa xong
        }

        // 🔒 LỚP PHÒNG THỦ 2: Kiểm tra Cooldown nếu do người dùng chủ động bấm Refresh
        if (isUserInitiated && (currentTime - lastScanTime < REFRESH_COOLDOWN_MS)) {
            // Mới quét xong chưa tới 3s, trả trạng thái Idle để ẩn loading trên UI ngay
            _scanState.value = ScanProgressState.Idle
            return
        }

        // Kích hoạt luồng chạy ngầm mới
        scanJob = scanScope.launch {
            lastScanTime = System.currentTimeMillis()
            _scanState.value = ScanProgressState.Scanning(scannedCount = 0, totalFiles = 0)

            try {
                // 1. Lấy instance Query từ Room Database
                val dbQueries = AppDatabase.getInstance(context).bookQueries()

                // 2. Chuyển đường dẫn String sang đối tượng File
                val targetFolder = File(targetFolderPath)

                // 3. Gọi hàm quét thực tế từ BookScanner
                BookScanner.scanDirectory(
                    targetFolder = targetFolder,
                    dbQueries = dbQueries,
                    progressFlow = _scanState
                )

            } catch (e: Exception) {
                _scanState.value = ScanProgressState.Error(e.message ?: "Lỗi không xác định khi quét file")
            }
        }
    }

    /**
     * Hủy tiến trình Quét nếu cần thiết (ví dụ: khi app bị đóng hẳn)
     */
    fun cancelScan() {
        if (scanJob?.isActive == true) {
            scanJob?.cancel()
            _scanState.value = ScanProgressState.Idle
        }
    }
}