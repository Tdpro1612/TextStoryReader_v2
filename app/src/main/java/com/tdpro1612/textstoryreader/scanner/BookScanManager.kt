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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus

/**
 * Singleton quản lý luồng quét sách ngầm (Background Scan Thread).
 * Nơi tiếp nhận yêu cầu Quét/Refresh từ UI và đảm bảo chỉ có DUY NHẤT 1 luồng chạy ngầm.
 */
object BookScanManager {

    private val scanScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private val _scanState = MutableStateFlow<ScanProgressState>(ScanProgressState.Idle)
    val scanState: StateFlow<ScanProgressState> = _scanState.asStateFlow()

    private var lastScanTime = 0L
    private const val REFRESH_COOLDOWN_MS = 3000L

    fun startScan(
        context: Context,
        targetFolderPath: String,
        isUserInitiated: Boolean = false
    ) {
        val currentTime = System.currentTimeMillis()

        if (scanJob?.isActive == true) {
            return
        }

        if (isUserInitiated && (currentTime - lastScanTime < REFRESH_COOLDOWN_MS)) {
            _scanState.value = ScanProgressState.Idle
            return
        }

        scanJob = scanScope.launch {
            lastScanTime = System.currentTimeMillis()
            _scanState.value = ScanProgressState.Scanning(scannedCount = 0, totalFiles = 0)

            try {
                // 1. Lấy Queries từ AppDatabase
                val dbQueries = AppDatabase.getInstance(context).bookQueries()

                // 2. Gọi BookScanner quét (truyền context vào để xử lý SAF Uri)
                BookScanner.scanDirectory(
                    context = context,
                    targetFolderPath = targetFolderPath,
                    dbQueries = dbQueries,
                    progressFlow = _scanState
                )

            } catch (e: Exception) {
                _scanState.value = ScanProgressState.Error(e.message ?: "Lỗi không xác định khi quét file")
            }
        }
    }

    fun cancelScan() {
        if (scanJob?.isActive == true) {
            scanJob?.cancel()
            _scanState.value = ScanProgressState.Idle
        }
    }
}