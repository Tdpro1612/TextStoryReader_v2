package com.tdpro1612.textstoryreader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.tdpro1612.textstoryreader.manager.BookCacheManager
import com.tdpro1612.textstoryreader.ui.library.LibraryActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 0. Dọn dẹp sạch sẽ tất cả cache tồn đọng của app trước khi vào Thư viện
//        BookCacheManager.clearAllBookCaches(applicationContext)

        // 1. Mở LibraryActivity ngay khi app khởi chạy
        val intent = Intent(this, LibraryActivity::class.java).apply {
            // Xóa MainActivity khỏi Back Stack để khi bấm nút Back ở Thư viện sẽ thoát App luôn
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        // 2. Đóng MainActivity ngay lập tức để giải phóng RAM
        finish()
    }
}