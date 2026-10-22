package com.dailylist.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultReminderMinutes: Int? = null,
    val defaultReminderAdvanceMinutes: Int = 0,
)

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val DEFAULT_ADVANCE_MINUTES = intPreferencesKey("default_advance_minutes")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            defaultReminderMinutes = prefs[Keys.DEFAULT_REMINDER_MINUTES],
            defaultReminderAdvanceMinutes = prefs[Keys.DEFAULT_ADVANCE_MINUTES] ?: 0,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDefaultReminder(minutes: Int?, advanceMinutes: Int) {
        context.settingsDataStore.edit {
            if (minutes == null) {
                it.remove(Keys.DEFAULT_REMINDER_MINUTES)
            } else {
                it[Keys.DEFAULT_REMINDER_MINUTES] = minutes
            }
            it[Keys.DEFAULT_ADVANCE_MINUTES] = advanceMinutes
        }
    }
}
