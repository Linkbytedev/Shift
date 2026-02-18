package com.Linkbyte.Shift.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.VaultImage
import com.Linkbyte.Shift.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val securityPreferences: com.Linkbyte.Shift.data.preferences.SecurityPreferences
) : ViewModel() {
    
    val isBiometricVaultEnabled = securityPreferences.isBiometricVaultEnabled
    
    private val _isPasswordSet = MutableStateFlow(false)
    val isPasswordSet: StateFlow<Boolean> = _isPasswordSet.asStateFlow()
    
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()
    
    private val _images = MutableStateFlow<List<VaultImage>>(emptyList())
    val images: StateFlow<List<VaultImage>> = _images.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private var currentPassword: String? = null
    
    private val _biometricPassword = MutableStateFlow<String?>(null)
    val biometricPassword: StateFlow<String?> = _biometricPassword.asStateFlow()
    
    init {
        checkPasswordStatus()
    }
    
    private fun checkPasswordStatus() {
        viewModelScope.launch {
            _isPasswordSet.value = vaultRepository.isPasswordSet()
            if (_isPasswordSet.value) {
                vaultRepository.getSavedPasswordForBiometric().onSuccess { password ->
                    _biometricPassword.value = password
                }
            }
        }
    }
    
    fun setPassword(password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _error.value = "PINs do not match"
            return
        }
        
        if (password.length !in 5..10) {
            _error.value = "PIN must be 5-10 digits"
            return
        }
        
        if (!password.all { it.isDigit() }) {
            _error.value = "PIN must contain only digits"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.setPassword(password)
                .onSuccess {
                    _isPasswordSet.value = true
                    currentPassword = password
                    _isUnlocked.value = true
                    _successMessage.value = "Vault password set successfully"
                    
                    // If biometric is enabled, save it immediately
                    viewModelScope.launch {
                        securityPreferences.isBiometricVaultEnabled.collect { enabled ->
                            if (enabled) {
                                vaultRepository.savePasswordForBiometric(password)
                            }
                        }
                    }
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }
    
    fun unlock(password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.verifyPassword(password)
                .onSuccess { isValid ->
                    if (isValid) {
                        currentPassword = password
                        _isUnlocked.value = true
                        loadImages()
                        
                        // Save for biometric if enabled
                        viewModelScope.launch {
                           if (securityPreferences.isBiometricVaultEnabled.first()) {
                               vaultRepository.savePasswordForBiometric(password)
                               _biometricPassword.value = password
                           }
                        }
                    } else {
                        _error.value = "Incorrect password"
                    }
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }
    
    fun lock() {
        currentPassword = null
        _isUnlocked.value = false
        _images.value = emptyList()
    }
    
    fun resetVault() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                vaultRepository.clearVault()
                    .onSuccess {
                        _isPasswordSet.value = false
                        _isUnlocked.value = false
                        currentPassword = null
                        _images.value = emptyList()
                        _successMessage.value = "Vault reset successfully"
                    }
                    .onFailure {
                        _error.value = "Failed to reset vault: ${it.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Failed to reset vault: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadImages() {
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.getAllImages()
                .onSuccess { _images.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
    
    fun addImage(imageData: ByteArray, fileName: String, originalUri: android.net.Uri? = null, contentResolver: android.content.ContentResolver? = null) {
        val password = currentPassword ?: run {
            _error.value = "Not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.addImage(imageData, password, fileName)
                .onSuccess {
                    _successMessage.value = "Image added to vault"
                    
                    // "Move" logic: Delete original file if URI provided
                    originalUri?.let { uri ->
                        try {
                            contentResolver?.delete(uri, null, null)
                        } catch (e: Exception) {
                            // Non-fatal, original might be read-only or permission missing
                        }
                    }
                    
                    loadImages()
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }

    fun exportImage(imageId: String) {
        val password = currentPassword ?: run {
            _error.value = "Not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.exportImage(imageId, password)
                .onSuccess {
                    _successMessage.value = "Image exported to gallery"
                    // After export, delete from vault (Move Out logic)
                    deleteImage(imageId)
                }
                .onFailure {
                    _error.value = "Export failed: ${it.message}"
                }
            _isLoading.value = false
        }
    }
    
    fun deleteImage(imageId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            vaultRepository.deleteImage(imageId)
                .onSuccess {
                    _successMessage.value = "Image deleted"
                    loadImages()
                }
                .onFailure {
                    _error.value = it.message
                }
            _isLoading.value = false
        }
    }
    
    fun getImageData(imageId: String, onSuccess: (ByteArray) -> Unit) {
        val password = currentPassword ?: run {
            _error.value = "Not authenticated"
            return
        }
        
        viewModelScope.launch {
            vaultRepository.getImageData(imageId, password)
                .onSuccess { onSuccess(it) }
                .onFailure { _error.value = it.message }
        }
    }
    
    fun getFullImageData(imageId: String, onSuccess: (ByteArray) -> Unit) {
        getImageData(imageId, onSuccess)
    }
    
    fun getThumbnailData(imageId: String, onSuccess: (ByteArray) -> Unit) {
        val password = currentPassword ?: run {
            _error.value = "Not authenticated"
            return
        }
        
        viewModelScope.launch {
            vaultRepository.getThumbnailData(imageId, password)
                .onSuccess { onSuccess(it) }
                .onFailure { _error.value = it.message }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
}
