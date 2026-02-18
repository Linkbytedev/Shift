package com.Linkbyte.Shift.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getCurrentUser()
                .onSuccess { _user.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateDisplayName(newName: String) {
        val currentUser = _user.value ?: return
        if (newName.isBlank()) {
            _error.value = "Display name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val updatedUser = currentUser.copy(displayName = newName)
            userRepository.updateUser(updatedUser)
                .onSuccess {
                    _user.value = updatedUser
                    _successMessage.value = "Profile updated!"
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateStatus(newStatus: String) {
        val currentUser = _user.value ?: return

        viewModelScope.launch {
            val updatedUser = currentUser.copy(status = newStatus)
            userRepository.updateUser(updatedUser)
                .onSuccess { _user.value = updatedUser }
                .onFailure { _error.value = it.message }
        }
    }

    fun uploadProfilePicture(imageData: ByteArray) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            userRepository.uploadProfilePicture(imageData)
                .onSuccess { imageUrl ->
                    val currentUser = _user.value
                    if (currentUser != null) {
                        _user.value = currentUser.copy(profileImageUrl = imageUrl)
                    }
                    _successMessage.value = "Profile picture updated!"
                }
                .onFailure { _error.value = it.message }
            _isUploadingImage.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}
