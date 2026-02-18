package com.Linkbyte.Shift.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class ThemePreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val themeKey = intPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val mode = preferences[themeKey] ?: ThemeMode.SYSTEM.ordinal
            ThemeMode.values()[mode]
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = mode.ordinal
        }
    }
}
