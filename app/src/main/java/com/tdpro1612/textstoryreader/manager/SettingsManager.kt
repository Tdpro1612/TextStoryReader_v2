package com.tdpro1612.textstoryreader.manager

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tdpro1612.textstoryreader.settings.FontFamilyOption
import com.tdpro1612.textstoryreader.settings.ReaderSettings
import com.tdpro1612.textstoryreader.settings.ReaderThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reader_settings")

class SettingsManager(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val FONT_SIZE = intPreferencesKey("font_size")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val readerSettingsFlow: Flow<ReaderSettings> = context.dataStore.data.map { pref ->
        val themeName = pref[Keys.THEME_PRESET]
        val fontName = pref[Keys.FONT_FAMILY]

        ReaderSettings(
            themePreset = themeName?.let {
                runCatching { ReaderThemePreset.valueOf(it) }.getOrNull()
            } ?: ReaderThemePreset.LIGHT,
            fontSizeSp = pref[Keys.FONT_SIZE] ?: 18,
            lineHeightMultiplier = pref[Keys.LINE_HEIGHT] ?: 1.4f,
            fontFamily = fontName?.let {
                runCatching { FontFamilyOption.valueOf(it) }.getOrNull()
            } ?: FontFamilyOption.DEFAULT,
            keepScreenOn = pref[Keys.KEEP_SCREEN_ON] ?: true
        )
    }

    suspend fun updateThemePreset(preset: ReaderThemePreset) {
        context.dataStore.edit { pref -> pref[Keys.THEME_PRESET] = preset.name }
    }

    suspend fun updateFontSize(sizeSp: Int) {
        context.dataStore.edit { pref -> pref[Keys.FONT_SIZE] = sizeSp }
    }

    suspend fun updateLineHeight(multiplier: Float) {
        context.dataStore.edit { pref -> pref[Keys.LINE_HEIGHT] = multiplier }
    }

    suspend fun updateFontFamily(fontFamily: FontFamilyOption) {
        context.dataStore.edit { pref -> pref[Keys.FONT_FAMILY] = fontFamily.name }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[Keys.KEEP_SCREEN_ON] = enabled }
    }
}