package com.Linkbyte.Shift.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.Linkbyte.Shift.data.model.VaultImage
import com.Linkbyte.Shift.domain.repository.VaultRepository
import com.Linkbyte.Shift.security.vault.VaultEncryptionService
import com.Linkbyte.Shift.security.vault.VaultPasswordManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.ContentValues
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val passwordManager: VaultPasswordManager,
    private val encryptionService: VaultEncryptionService
) : VaultRepository {
    
    private val vaultDir = File(context.filesDir, "vault")
    private val imagesDir = File(vaultDir, "images")
    private val thumbnailsDir = File(vaultDir, "thumbnails")
    private val metadataFile = File(vaultDir, "metadata.json.enc")
    
    init {
        // Create vault directories if they don't exist
        imagesDir.mkdirs()
        thumbnailsDir.mkdirs()
    }
    
    override suspend fun isPasswordSet(): Boolean = withContext(Dispatchers.IO) {
        passwordManager.isPasswordSet()
    }
    
    override suspend fun setPassword(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            passwordManager.setPassword(password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPassword(password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Result.success(passwordManager.verifyPassword(password))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!passwordManager.changePassword(oldPassword, newPassword)) {
                throw Exception("Invalid old password")
            }
            
            // Re-encrypt all images with new password-derived key
            val images = getAllImages().getOrThrow()
            val oldKey = encryptionService.deriveKeyFromPassword(oldPassword, passwordManager.getSalt())
            val newKey = encryptionService.deriveKeyFromPassword(newPassword, passwordManager.getSalt())
            
            images.forEach { image ->
                // Re-encrypt image
                val imageFile = File(image.encryptedPath)
                if (imageFile.exists()) {
                    val encryptedData = readEncryptedFile(imageFile)
                    val decrypted = encryptionService.decryptImage(encryptedData, oldKey)
                    val reencrypted = encryptionService.encryptImage(decrypted, newKey)
                    writeEncryptedFile(imageFile, reencrypted)
                }
                
                // Re-encrypt thumbnail
                val thumbFile = File(image.thumbnailPath)
                if (thumbFile.exists()) {
                    val encryptedData = readEncryptedFile(thumbFile)
                    val decrypted = encryptionService.decryptImage(encryptedData, oldKey)
                    val reencrypted = encryptionService.encryptImage(decrypted, newKey)
                    writeEncryptedFile(thumbFile, reencrypted)
                }
            }
            
            // Re-encrypt metadata
            saveMetadata(images, newPassword)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addImage(imageData: ByteArray, password: String, fileName: String): Result<VaultImage> = withContext(Dispatchers.IO) {
        try {
            if (!passwordManager.verifyPassword(password)) {
                throw Exception("Invalid password")
            }
            
            val imageId = UUID.randomUUID().toString()
            val key = encryptionService.deriveKeyFromPassword(password, passwordManager.getSalt())
            
            // Encrypt and save full image
            val encryptedImage = encryptionService.encryptImage(imageData, key)
            val imagePath = File(imagesDir, "$imageId.enc")
            writeEncryptedFile(imagePath, encryptedImage)
            
            // Create and encrypt thumbnail
            val thumbnail = createThumbnail(imageData)
            val encryptedThumb = encryptionService.encryptImage(thumbnail, key)
            val thumbPath = File(thumbnailsDir, "${imageId}_thumb.enc")
            writeEncryptedFile(thumbPath, encryptedThumb)
            
            // Create vault image metadata
            val vaultImage = VaultImage(
                id = imageId,
                fileName = fileName,
                addedAt = System.currentTimeMillis(),
                encryptedPath = imagePath.absolutePath,
                thumbnailPath = thumbPath.absolutePath
            )
            
            // Update metadata
            val images = getAllImages().getOrElse { emptyList() }.toMutableList()
            images.add(vaultImage)
            saveMetadata(images, password)
            
            Result.success(vaultImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAllImages(): Result<List<VaultImage>> = withContext(Dispatchers.IO) {
        try {
            if (!metadataFile.exists()) {
                return@withContext Result.success(emptyList())
            }
            
            val images = mutableListOf<VaultImage>()
            
            // For now, return metadata without decrypting (we'll decrypt when needed)
            // We'll just scan the images directory
            val imageFiles = imagesDir.listFiles()?.filter { it.extension == "enc" } ?: emptyList()
            
            imageFiles.forEach { file ->
                val imageId = file.nameWithoutExtension
                val thumbPath = File(thumbnailsDir, "${imageId}_thumb.enc")
                
                if (thumbPath.exists()) {
                    images.add(
                        VaultImage(
                            id = imageId,
                            fileName = "Image",
                            addedAt = file.lastModified(),
                            encryptedPath = file.absolutePath,
                            thumbnailPath = thumbPath.absolutePath
                        )
                    )
                }
            }
            
            Result.success(images.sortedByDescending { it.addedAt })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getImageData(imageId: String, password: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!passwordManager.verifyPassword(password)) {
                throw Exception("Invalid password")
            }
            
            val imageFile = File(imagesDir, "$imageId.enc")
            if (!imageFile.exists()) {
                throw Exception("Image not found")
            }
            
            val key = encryptionService.deriveKeyFromPassword(password, passwordManager.getSalt())
            val encryptedData = readEncryptedFile(imageFile)
            val decrypted = encryptionService.decryptImage(encryptedData, key)
            
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getThumbnailData(imageId: String, password: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            if (!passwordManager.verifyPassword(password)) {
                throw Exception("Invalid password")
            }
            
            val thumbFile = File(thumbnailsDir, "${imageId}_thumb.enc")
            if (!thumbFile.exists()) {
                throw Exception("Thumbnail not found")
            }
            
            val key = encryptionService.deriveKeyFromPassword(password, passwordManager.getSalt())
            val encryptedData = readEncryptedFile(thumbFile)
            val decrypted = encryptionService.decryptImage(encryptedData, key)
            
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteImage(imageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageFile = File(imagesDir, "$imageId.enc")
            val thumbFile = File(thumbnailsDir, "${imageId}_thumb.enc")
            
            imageFile.delete()
            thumbFile.delete()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearVault(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Delete all images and thumbnails
            imagesDir.deleteRecursively()
            thumbnailsDir.deleteRecursively()
            metadataFile.delete()
            
            // Recreate directories
            imagesDir.mkdirs()
            thumbnailsDir.mkdirs()
            
            // Clear password
            passwordManager.clearVaultPassword()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_vault_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun savePasswordForBiometric(password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().putString("vault_pin", password).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSavedPasswordForBiometric(): Result<String?> = withContext(Dispatchers.IO) {
        try {
            Result.success(sharedPreferences.getString("vault_pin", null))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearSavedPassword(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            sharedPreferences.edit().remove("vault_pin").apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportImage(imageId: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageData = getImageData(imageId, password).getOrThrow()
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Shift_Vault_$imageId.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/ShiftVault")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")
                
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(imageData)
            } ?: throw Exception("Failed to open output stream")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper functions
    
    private fun createThumbnail(imageData: ByteArray, maxSize: Int = 200): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
        
        val ratio = maxSize.toFloat() / maxOf(bitmap.width, bitmap.height)
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        
        val thumbnail = Bitmap.createScaledBitmap(bitmap, width, height, true)
        
        val outputStream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        
        return outputStream.toByteArray()
    }
    
    private fun writeEncryptedFile(file: File, data: com.Linkbyte.Shift.security.vault.EncryptedImageData) {
        // Write IV length (4 bytes) + IV + encrypted data
        file.outputStream().use { output ->
            val ivBytes = data.iv.toByteArray()
            output.write(ivBytes.size.toByteArray())
            output.write(ivBytes)
            output.write(data.encryptedBytes)
        }
    }
    
    private fun readEncryptedFile(file: File): com.Linkbyte.Shift.security.vault.EncryptedImageData {
        file.inputStream().use { input ->
            // Read IV length
            val ivLengthBytes = ByteArray(4)
            input.read(ivLengthBytes)
            val ivLength = ivLengthBytes.toInt()
            
            // Read IV
            val ivBytes = ByteArray(ivLength)
            input.read(ivBytes)
            val iv = String(ivBytes)
            
            // Read encrypted data
            val encryptedBytes = input.readBytes()
            
            return com.Linkbyte.Shift.security.vault.EncryptedImageData(encryptedBytes, iv)
        }
    }
    
    private fun saveMetadata(images: List<VaultImage>, password: String) {
        val key = encryptionService.deriveKeyFromPassword(password, passwordManager.getSalt())
        
        val jsonArray = JSONArray()
        images.forEach { image ->
            jsonArray.put(JSONObject().apply {
                put("id", image.id)
                put("fileName", image.fileName)
                put("addedAt", image.addedAt)
                put("encryptedPath", image.encryptedPath)
                put("thumbnailPath", image.thumbnailPath)
            })
        }
        
        val metadataBytes = jsonArray.toString().toByteArray()
        val encrypted = encryptionService.encryptImage(metadataBytes, key)
        writeEncryptedFile(metadataFile, encrypted)
    }
    
    private fun Int.toByteArray(): ByteArray {
        return byteArrayOf(
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
        )
    }
    
    private fun ByteArray.toInt(): Int {
        return  (this[0].toInt() and 0xFF shl 24) or
                (this[1].toInt() and 0xFF shl 16) or
                (this[2].toInt() and 0xFF shl 8) or
                (this[3].toInt() and 0xFF)
    }
}
