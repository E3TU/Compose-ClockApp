package com.example.clockapp.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Move to data/settings if more preferences are added
enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TimeFormatMode {
    SYSTEM,
    H12,
    H24
}

data class ThemeSettings(
    val colorSource: ColorSource = ColorSource.BRAND,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val timeFormat: TimeFormatMode = TimeFormatMode.SYSTEM
)

private val Context.dataStore by preferencesDataStore(name = "theme_settings")

private val ColorSourceKey = stringPreferencesKey("color_source")
private val AppearanceModeKey = stringPreferencesKey("appearance_mode")
private val TimeFormatKey = stringPreferencesKey("time_format")

val Context.themeSettings: Flow<ThemeSettings>
    get() = dataStore.data.map { preferences ->
        ThemeSettings(
            colorSource = preferences[ColorSourceKey]
                ?.let { runCatching { ColorSource.valueOf(it) }.getOrNull() }
                ?: ColorSource.BRAND,
            appearanceMode = preferences[AppearanceModeKey]
                ?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() }
                ?: AppearanceMode.SYSTEM,
            timeFormat = preferences[TimeFormatKey]
                ?.let { runCatching { TimeFormatMode.valueOf(it) }.getOrNull() }
                ?: TimeFormatMode.SYSTEM
        )
    }

suspend fun Context.setColorSource(colorSource: ColorSource) {
    dataStore.edit { preferences ->
        preferences[ColorSourceKey] = colorSource.name
    }
}

suspend fun Context.setAppearanceMode(appearanceMode: AppearanceMode) {
    dataStore.edit { preferences ->
        preferences[AppearanceModeKey] = appearanceMode.name
    }
}

suspend fun Context.setTimeFormat(timeFormat: TimeFormatMode) {
    dataStore.edit { preferences ->
        preferences[TimeFormatKey] = timeFormat.name
    }
}
