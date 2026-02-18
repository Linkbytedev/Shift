package com.Linkbyte.Shift.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.Conversation
import com.Linkbyte.Shift.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {
    
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                messageRepository.getConversations().collect { convos ->
                    _conversations.value = convos
                    _isLoading.value = false
                    _error.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load chats: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun createConversation(otherUserId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val result = messageRepository.createConversation(otherUserId)
            result.onSuccess { conversationId ->
                onSuccess(conversationId)
            }
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            messageRepository.archiveConversation(conversationId)
                .onFailure { e ->
                    _error.value = "Failed to archive chat: ${e.message}"
                }
        }
    }
    
    fun renameConversation(conversationId: String, newName: String) {
        viewModelScope.launch {
            messageRepository.renameConversation(conversationId, newName)
                .onFailure { e ->
                    // _error.value = "Failed to rename chat: ${e.message}"
                    // Optional: show error toast or snackbar
                }
        }
    }
}
