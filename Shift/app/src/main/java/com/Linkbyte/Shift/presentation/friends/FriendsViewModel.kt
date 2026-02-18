package com.Linkbyte.Shift.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.FriendRequest
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.FriendRepository
import com.Linkbyte.Shift.domain.repository.MessageRepository
import com.Linkbyte.Shift.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val pendingRequests: StateFlow<List<FriendRequest>> = _pendingRequests.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<User>>(emptyList())
    val blockedUsers: StateFlow<List<User>> = _blockedUsers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadFriends()
        loadPendingRequests()
        loadBlockedUsers()
    }

    fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendRepository.getFriends().collect { friends ->
                    _friends.value = friends
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    private fun loadPendingRequests() {
        viewModelScope.launch {
            try {
                friendRepository.getPendingRequests().collect { requests ->
                    _pendingRequests.value = requests
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            userRepository.getBlockedUsers()
                .onSuccess { _blockedUsers.value = it }
                .onFailure { /* Silent fail */ }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.searchUsers(query)
                .onSuccess { users ->
                    // Filter out current user and existing friends
                    val currentFriendIds = _friends.value.map { it.userId }
                    _searchResults.value = users.filter { user ->
                        !currentFriendIds.contains(user.userId)
                    }
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            friendRepository.sendFriendRequest(userId)
                .onSuccess {
                    _successMessage.value = "Friend request sent!"
                    _searchResults.value = _searchResults.value.filter { it.userId != userId }
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun acceptRequest(requestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            friendRepository.acceptFriendRequest(requestId)
                .onSuccess {
                    _successMessage.value = "Friend request accepted!"
                    loadFriends()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun declineRequest(requestId: String) {
        viewModelScope.launch {
            friendRepository.declineFriendRequest(requestId)
                .onSuccess { _successMessage.value = "Friend request declined" }
                .onFailure { _error.value = it.message }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // First delete the conversation with this user
            messageRepository.deleteConversationWithUser(friendId)
            
            friendRepository.removeFriend(friendId)
                .onSuccess {
                    _successMessage.value = "Friend removed"
                    loadFriends()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // First delete the conversation with this user
            messageRepository.deleteConversationWithUser(userId)
            // Then block the user
            userRepository.blockUser(userId)
                .onSuccess {
                    _successMessage.value = "User blocked and messages deleted"
                    loadFriends()
                    loadBlockedUsers()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.unblockUser(userId)
                .onSuccess {
                    _successMessage.value = "User unblocked"
                    loadBlockedUsers()
                }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun startConversation(friendId: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            messageRepository.createConversation(friendId)
                .onSuccess { conversationId -> onSuccess(conversationId) }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }
}
