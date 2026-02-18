package com.Linkbyte.Shift.security.vault

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultPasswordManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "vault_prefs"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_SALT = "salt"
        private const val SALT_LENGTH = 32
    }
    
    // Lazy initialization with error handling
    private val sharedPreferences by lazy {
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
                throw RuntimeException("Failed to initialize vault storage", e2)
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
     * Check if a vault password has been set
     */
    fun isPasswordSet(): Boolean {
        return try {
            sharedPreferences.contains(KEY_PASSWORD_HASH)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Set the vault password (first time setup)
     */
    fun setPassword(password: String) {
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        
        sharedPreferences.edit().apply {
            putString(KEY_PASSWORD_HASH, hash)
            putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            apply()
        }
    }
    
    /**
     * Verify a password attempt
     */
    fun verifyPassword(password: String): Boolean {
        try {
            val storedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null) ?: return false
            val saltString = sharedPreferences.getString(KEY_SALT, null) ?: return false
            val salt = Base64.decode(saltString, Base64.NO_WRAP)
            
            val inputHash = hashPassword(password, salt)
            return inputHash == storedHash
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Change the vault password
     */
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(oldPassword)) {
            return false
        }
        setPassword(newPassword)
        return true
    }
    
    /**
     * Get the salt for key derivation
     */
    fun getSalt(): ByteArray {
        val saltString = sharedPreferences.getString(KEY_SALT, null)
            ?: throw IllegalStateException("Salt not found")
        return Base64.decode(saltString, Base64.NO_WRAP)
    }
    
    /**
     * Generate a random salt
     */
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * Hash password using SHA-256 with salt
     */
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest(password.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
    
    /**
     * Clear all vault data (for factory reset)
     */
    fun clearVaultPassword() {
        try {
            sharedPreferences.edit().clear().apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
