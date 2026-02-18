package com.Linkbyte.Shift.presentation.friends

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.MessageRepository
import com.Linkbyte.Shift.domain.repository.UserRepository
import com.Linkbyte.Shift.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.getUser(userId)
                .onSuccess { _user.value = it }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun startConversation(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            messageRepository.createConversation(userId)
                .onSuccess { conversationId -> onSuccess(conversationId) }
                .onFailure { _error.value = it.message }
        }
    }

    fun removeFriend(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Delete conversation first
            messageRepository.deleteConversationWithUser(userId)
            
            friendRepository.removeFriend(userId)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
        }
    }

    fun blockUser(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            // Delete conversation first
            messageRepository.deleteConversationWithUser(userId)
            
            userRepository.blockUser(userId)
                .onSuccess { onSuccess() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}
