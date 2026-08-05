package com.tdpro1612.textstoryreader.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tdpro1612.textstoryreader.manager.SettingsManager
import com.tdpro1612.textstoryreader.settings.FontFamilyOption
import com.tdpro1612.textstoryreader.settings.ReadMode
import com.tdpro1612.textstoryreader.settings.ReaderSettings
import com.tdpro1612.textstoryreader.settings.ReaderThemePreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val readerSettings: StateFlow<ReaderSettings> = settingsManager.readerSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderSettings()
        )

    fun getThemeColors(preset: ReaderThemePreset): Pair<Color, Color> {
        return Pair(
            Color(preset.backgroundColorHex),
            Color(preset.textColorHex)
        )
    }

    fun updateThemePreset(preset: ReaderThemePreset) {
        viewModelScope.launch {
            settingsManager.updateThemePreset(preset)
        }
    }

    fun updateFontSize(sizeSp: Int) {
        viewModelScope.launch {
            settingsManager.updateFontSize(sizeSp)
        }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch {
            settingsManager.updateLineHeight(multiplier)
        }
    }

    fun updateFontFamily(fontFamily: FontFamilyOption) {
        viewModelScope.launch {
            settingsManager.updateFontFamily(fontFamily)
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.updateKeepScreenOn(enabled)
        }
    }

    fun updateReadMode(readMode: ReadMode) {
        viewModelScope.launch {
            settingsManager.updateReadMode(readMode)
        }
    }

    // 🔥 Hàm khôi phục toàn bộ cài đặt về mặc định
    fun resetToDefault() {
        viewModelScope.launch {
            val defaultSettings = ReaderSettings()
            settingsManager.updateThemePreset(defaultSettings.themePreset)
            settingsManager.updateFontSize(defaultSettings.fontSizeSp)
            settingsManager.updateLineHeight(defaultSettings.lineHeightMultiplier)
            settingsManager.updateFontFamily(defaultSettings.fontFamily)
            settingsManager.updateKeepScreenOn(defaultSettings.keepScreenOn)
            settingsManager.updateReadMode(defaultSettings.readMode)
        }
    }
}