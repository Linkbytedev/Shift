package com.Linkbyte.Shift.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "security_settings")

@Singleton
class SecurityPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    private val screenSecurityKey = booleanPreferencesKey("screen_security_enabled")
    private val biometricVaultKey = booleanPreferencesKey("biometric_vault_enabled")

    // Default to true for maximum privacy, but user can now toggle it
    val isScreenSecurityEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[screenSecurityKey] ?: true
        }

    val isBiometricVaultEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[biometricVaultKey] ?: false
        }

    suspend fun setScreenSecurityEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[screenSecurityKey] = enabled
        }
    }

    suspend fun setBiometricVaultEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricVaultKey] = enabled
        }
    }
}
