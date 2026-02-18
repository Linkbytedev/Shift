package com.Linkbyte.Shift.presentation.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.Conversation
import com.Linkbyte.Shift.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadArchivedConversations()
    }

    private fun loadArchivedConversations() {
        viewModelScope.launch {
            repository.getArchivedConversations()
                .onStart { _isLoading.value = true }
                .catch { e -> 
                    _isLoading.value = false
                    e.printStackTrace()
                }
                .collectLatest { convs ->
                    _conversations.value = convs
                    _isLoading.value = false
                }
        }
    }

    fun unarchiveConversation(conversationId: String) {
        viewModelScope.launch {
            repository.unarchiveConversation(conversationId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
        }
    }
}
