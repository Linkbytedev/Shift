package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.VaultImage

interface VaultRepository {
    /**
     * Check if vault password is set
     */
    suspend fun isPasswordSet(): Boolean
    
    /**
     * Set vault password (first time setup)
     */
    suspend fun setPassword(password: String): Result<Unit>
    
    /**
     * Verify password
     */
    suspend fun verifyPassword(password: String): Result<Boolean>
    
    /**
     * Change vault password
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>
    
    /**
     * Add image to vault
     */
    suspend fun addImage(imageData: ByteArray, password: String, fileName: String): Result<VaultImage>
    
    /**
     * Get all vault images metadata
     */
    suspend fun getAllImages(): Result<List<VaultImage>>
    
    /**
     * Get decrypted image data
     */
    suspend fun getImageData(imageId: String, password: String): Result<ByteArray>
    
    /**
     * Get decrypted thumbnail data
     */
    suspend fun getThumbnailData(imageId: String, password: String): Result<ByteArray>
    
    /**
     * Delete image from vault
     */
    suspend fun deleteImage(imageId: String): Result<Unit>
    
    /**
     * Delete all vault data
     */
    suspend fun clearVault(): Result<Unit>
    /**
     * Store password securely for biometric auto-unlock
     */
    suspend fun savePasswordForBiometric(password: String): Result<Unit>
    
    /**
     * Retrieve securely stored password
     */
    suspend fun getSavedPasswordForBiometric(): Result<String?>
    
    /**
     * Clear securely stored password
     */
    suspend fun clearSavedPassword(): Result<Unit>
    
    /**
     * Export image back to gallery
     */
    suspend fun exportImage(imageId: String, password: String): Result<Unit>
}
