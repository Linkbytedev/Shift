package com.Linkbyte.Shift.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.signIn(email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }
    
    fun signUp(email: String, password: String, username: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            // Validate inputs
            if (username.length < 3) {
                _uiState.value = AuthUiState.Error("Username must be at least 3 characters")
                return@launch
            }
            
            if (password.length < 6) {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                return@launch
            }
            
            val result = authRepository.signUp(email, password, username, displayName)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: com.Linkbyte.Shift.data.model.User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
