package com.Linkbyte.Shift.security.vault

import com.Linkbyte.Shift.security.encryption.EncryptionService
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultEncryptionService @Inject constructor(
    private val encryptionService: EncryptionService
) {
    companion object {
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 32
    }
    
    /**
     * Generate a random salt for password derivation
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * Derive an encryption key from a password using PBKDF2
     */
    fun deriveKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        val spec: KeySpec = PBEKeySpec(
            password.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            KEY_LENGTH
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
    
    /**
     * Encrypt image data
     */
    fun encryptImage(imageData: ByteArray, key: SecretKey): EncryptedImageData {
        val result = encryptionService.encryptBytes(imageData, key)
        return EncryptedImageData(
            encryptedBytes = result.encryptedBytes,
            iv = result.iv
        )
    }
    
    /**
     * Decrypt image data
     */
    fun decryptImage(encrypted: EncryptedImageData, key: SecretKey): ByteArray {
        return encryptionService.decryptBytes(
            encryptedData = encrypted.encryptedBytes,
            iv = encrypted.iv,
            key = key
        )
    }
}

data class EncryptedImageData(
    val encryptedBytes: ByteArray,
    val iv: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedImageData

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
