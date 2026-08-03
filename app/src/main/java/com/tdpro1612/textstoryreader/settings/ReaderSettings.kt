package com.tdpro1612.textstoryreader.settings

enum class ReaderThemePreset(
    val displayName: String,
    val backgroundColorHex: Long,
    val textColorHex: Long
) {
    LIGHT("Sáng", 0xFFFFFFFF, 0xFF111111),
    DARK("Tối", 0xFF121212, 0xFFE0E0E0),
    OLED_BLACK("OLED", 0xFF000000, 0xFFCCCCCC),
    SEPIA("Vàng kem", 0xFFF4ECD8, 0xFF5F4B32),
    MINT("Xanh dịu", 0xFFE8F5E9, 0xFF1B5E20)
}

enum class FontFamilyOption(val displayName: String) {
    DEFAULT("Mặc định"),
    SERIF("Có chân (Serif)"),
    SAN_SERIF("Không chân (Sans-Serif)"),
    MONOSPACE("Máy đánh chữ (Monospace)")
}

data class ReaderSettings(
    val themePreset: ReaderThemePreset = ReaderThemePreset.LIGHT,
    val fontSizeSp: Int = 18,
    val lineHeightMultiplier: Float = 1.4f,
    val fontFamily: FontFamilyOption = FontFamilyOption.DEFAULT,
    val keepScreenOn: Boolean = true
)