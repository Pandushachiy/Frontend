package com.health.companion.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.health.companion.presentation.theme.AppFontFamily
import com.health.companion.presentation.theme.AppThemeOption
import com.health.companion.presentation.theme.ChatBackgroundOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_CHAT_BACKGROUND = stringPreferencesKey("chat_background")
        private val KEY_TEXT_SCALE = floatPreferencesKey("text_scale")
        private val KEY_FONT_FAMILY = stringPreferencesKey("font_family")

        const val TEXT_SCALE_MIN = 0.80f
        const val TEXT_SCALE_MAX = 1.40f
        const val TEXT_SCALE_DEFAULT = 1.00f
    }

    val selectedTheme: Flow<AppThemeOption> = dataStore.data.map { prefs ->
        AppThemeOption.fromId(prefs[KEY_THEME] ?: AppThemeOption.TEAL.id)
    }

    val selectedChatBackground: Flow<ChatBackgroundOption> = dataStore.data.map { prefs ->
        ChatBackgroundOption.fromId(prefs[KEY_CHAT_BACKGROUND] ?: ChatBackgroundOption.NIGHT.id)
    }

    val textScale: Flow<Float> = dataStore.data.map { prefs ->
        prefs[KEY_TEXT_SCALE] ?: TEXT_SCALE_DEFAULT
    }

    val fontFamily: Flow<AppFontFamily> = dataStore.data.map { prefs ->
        AppFontFamily.fromId(prefs[KEY_FONT_FAMILY] ?: AppFontFamily.DEFAULT.id)
    }

    suspend fun setTheme(theme: AppThemeOption) {
        dataStore.edit { prefs -> prefs[KEY_THEME] = theme.id }
    }

    suspend fun setChatBackground(bg: ChatBackgroundOption) {
        dataStore.edit { prefs -> prefs[KEY_CHAT_BACKGROUND] = bg.id }
    }

    suspend fun setTextScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_TEXT_SCALE] = scale.coerceIn(TEXT_SCALE_MIN, TEXT_SCALE_MAX)
        }
    }

    suspend fun setFontFamily(font: AppFontFamily) {
        dataStore.edit { prefs -> prefs[KEY_FONT_FAMILY] = font.id }
    }
}
