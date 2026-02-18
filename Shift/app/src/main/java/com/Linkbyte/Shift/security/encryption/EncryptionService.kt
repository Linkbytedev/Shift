package com.Linkbyte.Shift.security.encryption

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles AES-256-GCM encryption and decryption for messages and images.
 * 
 * This is the core security component ensuring end-to-end encryption.
 * - Uses AES-256 in GCM mode for authenticated encryption
 * - Generates unique IV for each message
 * - Returns base64-encoded ciphertext for storage
 */
@Singleton
class EncryptionService @Inject constructor() {
    
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 12 // 96 bits recommended for GCM
        private const val TAG_SIZE = 128 // 128 bits authentication tag
    }
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generate a new AES-256 key for a conversation
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE, secureRandom)
        return keyGenerator.generateKey()
    }
    
    /**
     * Convert key to base64 string for storage
     */
    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }
    
    /**
     * Convert base64 string back to SecretKey
     */
    fun stringToKey(keyString: String): SecretKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    /**
     * Encrypt plaintext and return encrypted data with IV prepended
     * 
     * @return Pair of (encryptedContent, iv) both as base64 strings
     */
    fun encrypt(plaintext: String, key: SecretKey): EncryptionResult {
        val iv = ByteArray(IV_SIZE)
        secureRandom.nextBytes(iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        return EncryptionResult(
            encryptedContent = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
    
    /**
     * Decrypt ciphertext using provided key and IV
     */
    fun decrypt(encryptedContent: String, iv: String, key: SecretKey): String {
        val ciphertext = Base64.decode(encryptedContent, Base64.NO_WRAP)
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
    
    /**
     * Encrypt binary data (for images)
     * 
     * @return Pair of (encryptedBytes, iv)
     */
    fun encryptBytes(data: ByteArray, key: SecretKey): EncryptionBytesResult {
        val iv = ByteArray(IV_SIZE)
        secureRandom.nextBytes(iv)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        
        val ciphertext = cipher.doFinal(data)
        
        return EncryptionBytesResult(
            encryptedBytes = ciphertext,
            iv = Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
    
    /**
     * Decrypt binary data (for images)
     */
    fun decryptBytes(encryptedData: ByteArray, iv: String, key: SecretKey): ByteArray {
        val ivBytes = Base64.decode(iv, Base64.NO_WRAP)
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_SIZE, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        
        return cipher.doFinal(encryptedData)
    }
}

data class EncryptionResult(
    val encryptedContent: String,
    val iv: String
)

data class EncryptionBytesResult(
    val encryptedBytes: ByteArray,
    val iv: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptionBytesResult

        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (iv != other.iv) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + iv.hashCode()
        return result
    }
}
