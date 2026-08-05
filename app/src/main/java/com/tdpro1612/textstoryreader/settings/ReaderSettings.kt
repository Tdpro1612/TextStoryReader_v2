package com.tdpro1612.textstoryreader.settings

import androidx.compose.ui.text.font.FontFamily

enum class ReadMode(val displayName: String) {
    SCROLL("Cuộn dọc (Continuous Scroll)"),
    PAGE_FLIP("Lật trang (Page Flip)")
}

enum class ReaderThemePreset(
    val displayName: String,
    val backgroundColorHex: Long,
    val textColorHex: Long
) {
    // Ban ngày & Chuẩn
    LIGHT("Sáng", 0xFFFFFFFF, 0xFF1A1A1A),
    SNOW("Tuyết", 0xFFF8F9FA, 0xFF212529),
    SEPIA("Giấy Cổ", 0xFFFBF0D9, 0xFF5F4B32),
    WARM_CREAM("Kem Ấm", 0xFFF5EFEB, 0xFF4A3E3D),

    // Tone Dịu Mắt & Thư Thái
    SAGE_GREEN("Xanh Rêu", 0xFFE8EFE9, 0xFF2D3A31),
    MINT("Bạc Hà", 0xFFE0F2F1, 0xFF004D40),
    OCEAN_PASTEL("Biển Dịu", 0xFFE3F2FD, 0xFF0D47A1),
    LAVENDER("Oải Hương", 0xFFF3E5F5, 0xFF4A148C),

    // Ban Đêm & Tối (AMOLED / Dark Mode)
    DARK("Xám Tối", 0xFF121212, 0xFFE0E0E0),
    NIGHT_BLUE("Đêm Thẫm", 0xFF0F172A, 0xFF94A3B8),
    CHARCOAL("Than Hoạt Tính", 0xFF1E1E1E, 0xFFD4D4D4),
    AMOLED_BLACK("Đen Tuyền", 0xFF000000, 0xFFCCCCCC)
}

enum class FontFamilyOption(val displayName: String) {
    DEFAULT("Mặc định hệ thống"),
    SERIF("Có chân (Serif)"),
    SANS_SERIF("Không chân (Sans-Serif)"),
    MONOSPACE("Máy đánh chữ (Monospace)")
}

// Extension chuyển đổi trực tiếp sang Compose FontFamily mà không tốn tài nguyên
fun FontFamilyOption.toComposeFontFamily(): FontFamily {
    return when (this) {
        FontFamilyOption.DEFAULT -> FontFamily.Default
        FontFamilyOption.SERIF -> FontFamily.Serif
        FontFamilyOption.SANS_SERIF -> FontFamily.SansSerif
        FontFamilyOption.MONOSPACE -> FontFamily.Monospace
    }
}

data class ReaderSettings(
    val themePreset: ReaderThemePreset = ReaderThemePreset.LIGHT,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.4f,
    val fontFamily: FontFamilyOption = FontFamilyOption.DEFAULT,
    val keepScreenOn: Boolean = true,
    val readMode: ReadMode = ReadMode.SCROLL
)