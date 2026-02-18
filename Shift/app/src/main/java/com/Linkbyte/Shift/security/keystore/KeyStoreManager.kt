package com.Linkbyte.Shift.security.keystore

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages secure storage of conversation encryption keys using Android Keystore.
 * 
 * - Uses EncryptedSharedPreferences for storing conversation keys
 * - Keys are encrypted at rest using hardware-backed encryption when available
 * - Each conversation has a unique symmetric key
 */
@Singleton
class KeyStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "secure_keys"
        private const val KEY_PREFIX = "shared_conv_key_"
    }
    
    // Lazy initialization with error handling
    private val encryptedPrefs by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            // If initialization fails (e.g. corrupt keystore), delete the prefs and try again
            e.printStackTrace()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            
            // Try one more time
            try {
                createEncryptedPrefs()
            } catch (e2: Exception) {
                e2.printStackTrace()
                // If it fails again, we might be in a bad state, but at least we tried to recover.
                // In a production app, we might want to surface this error to the user or
                // fallback to non-encrypted prefs (not recommended for secrets).
                // For now, rethrow to avoid silent failures if recovery fails.
                throw RuntimeException("Failed to initialize secure storage", e2)
            }
        }
    }

    private fun createEncryptedPrefs(): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Store a conversation key securely
     */
    fun storeConversationKey(conversationId: String, key: SecretKey) {
        try {
            val keyString = Base64.encodeToString(key.encoded, Base64.NO_WRAP)
            encryptedPrefs.edit()
                .putString(KEY_PREFIX + conversationId, keyString)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Retrieve a conversation key
     */
    fun getConversationKey(conversationId: String): SecretKey? {
        try {
            val keyString = encryptedPrefs.getString(KEY_PREFIX + conversationId, null)
                ?: return null
            
            val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
            return SecretKeySpec(keyBytes, "AES")
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Delete a conversation key (e.g., when conversation is deleted)
     */
    fun deleteConversationKey(conversationId: String) {
        try {
            encryptedPrefs.edit()
                .remove(KEY_PREFIX + conversationId)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if a conversation key exists
     */
    fun hasConversationKey(conversationId: String): Boolean {
        return try {
            encryptedPrefs.contains(KEY_PREFIX + conversationId)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear all stored keys (e.g., on logout)
     */
    fun clearAllKeys() {
        try {
            encryptedPrefs.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
