package com.Linkbyte.Shift.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.domain.repository.FriendRepository
import com.Linkbyte.Shift.domain.repository.MessageRepository
import com.Linkbyte.Shift.domain.repository.UserRepository
import com.Linkbyte.Shift.data.model.Conversation
import kotlinx.coroutines.flow.collectLatest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // ... existing code ...


    // ... existing code ...

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                currentUserId = user?.userId ?: ""
            }
        }
    }

    fun loadData(userId: String, conversationId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Load User
            userRepository.getUser(userId)
                .onSuccess { _user.value = it }
                .onFailure { _error.value = "Failed to load user: ${it.message}" }

            // Load Conversation if provided
            if (conversationId != null) {
                messageRepository.getConversation(conversationId)
                    .onSuccess { _conversation.value = it }
                    .onFailure { 
                        // It's okay if we can't load conversation details, maybe it was deleted
                         it.printStackTrace()
                    }
            } else {
                // Try to find existing conversation? (Optional)
            }
            _isLoading.value = false
        }
    }

    fun toggleMute() {
        val currentConv = _conversation.value ?: return
        val isMuted = currentConv.mutedBy.contains(currentUserId)
        
        viewModelScope.launch {
            messageRepository.setConversationMuted(currentConv.conversationId, !isMuted)
                .onSuccess {
                    // Update local state
                    val newMutedBy = if (isMuted) {
                        currentConv.mutedBy - currentUserId
                    } else {
                        currentConv.mutedBy + currentUserId
                    }
                    _conversation.value = currentConv.copy(mutedBy = newMutedBy)
                    _successMessage.value = if (isMuted) "Unmuted" else "Muted"
                }
                .onFailure { _error.value = "Failed to update settings" }
        }
    }

    fun setTimer(seconds: Int?) {
        val currentConv = _conversation.value ?: return
        viewModelScope.launch {
            messageRepository.setConversationTimer(currentConv.conversationId, seconds)
                .onSuccess {
                    _conversation.value = currentConv.copy(messageTimer = seconds)
                    _successMessage.value = "Timer updated"
                }
                .onFailure { _error.value = "Failed to update timer" }
        }
    }

    fun clearChat() {
        val currentConv = _conversation.value ?: return
        viewModelScope.launch {
            messageRepository.clearConversationMessages(currentConv.conversationId)
                .onSuccess {
                    _successMessage.value = "Chat cleared"
                }
                .onFailure { _error.value = "Failed to clear chat" }
        }
    }

    fun blockUser() {
        val currentUser = _user.value ?: return
        val currentConv = _conversation.value
        
        viewModelScope.launch {
            // 1. Delete conversation permanently (for both users)
            if (currentConv != null) {
                try {
                    messageRepository.deleteConversationPermanently(currentConv.conversationId)
                    android.util.Log.d("UserProfileViewModel", "Conversation permanently deleted")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Remove friend (Explicitly)
            try {
                friendRepository.removeFriend(currentUser.userId)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Block the user
            userRepository.blockUser(currentUser.userId)
                .onSuccess {
                    _successMessage.value = "User blocked"
                }
                .onFailure { _error.value = "Failed to block user" }
        }
    }

    fun removeFriend() {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            // First delete conversation
            try {
                messageRepository.deleteConversationWithUser(currentUser.userId)
            } catch (e: Exception) {
                // Ignore if fails, proceed to remove friend
            }

            friendRepository.removeFriend(currentUser.userId)
                .onSuccess {
                    _successMessage.value = "Friend removed"
                }
                .onFailure { _error.value = "Failed to remove friend" }
        }
    }

    fun toggleScreenshotSecurity(enable: Boolean) {
        val currentConv = _conversation.value ?: return
        val currentUserId = auth.currentUser?.uid ?: return
        
        android.util.Log.d("UserProfileViewModel", "Toggling SS: enable=$enable, convId=${currentConv.conversationId}, userId=$currentUserId")

        viewModelScope.launch {
            if (enable) {
                // Request enabling
                messageRepository.requestScreenshotSecurity(currentConv.conversationId, currentUserId)
                    .onSuccess {
                        _conversation.value = currentConv.copy(
                            screenshotSecurityRequesterId = currentUserId,
                            screenshotSecurityEnabled = false
                        )
                        _successMessage.value = "Request sent to ${_user.value?.displayName}"
                        android.util.Log.d("UserProfileViewModel", "Request sent success")
                    }
                    .onFailure { 
                        _error.value = "Failed to send request"
                        it.printStackTrace() 
                    }
            } else {
                if (currentConv.screenshotSecurityEnabled) {
                     messageRepository.disableScreenshotSecurity(currentConv.conversationId)
                        .onSuccess {
                            _conversation.value = currentConv.copy(
                                screenshotSecurityEnabled = false,
                                screenshotSecurityRequesterId = null
                            )
                            _successMessage.value = "Screenshot security disabled"
                            android.util.Log.d("UserProfileViewModel", "Disabled success")
                        }
                        .onFailure { e ->
                            _error.value = "Failed to disable security"
                            e.printStackTrace() 
                        }
                } else {
                     messageRepository.declineScreenshotSecurity(currentConv.conversationId)
                        .onSuccess {
                            _conversation.value = currentConv.copy(
                                screenshotSecurityRequesterId = null
                            )
                            android.util.Log.d("UserProfileViewModel", "Declined/Cancelled success")
                        }
                        .onFailure { e ->
                            _error.value = "Failed to cancel request"
                            e.printStackTrace() 
                        }
                }
            }
        }
    }
    
    fun startConversation(onSuccess: (String) -> Unit) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
             // If we already have conversation loaded, use it
             if (_conversation.value != null) {
                 onSuccess(_conversation.value!!.conversationId)
                 return@launch
             }
             
             messageRepository.createConversation(currentUser.userId)
                .onSuccess { conversationId -> onSuccess(conversationId) }
                .onFailure { _error.value = "Failed to start conversation: ${it.message}" }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}
