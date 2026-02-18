package com.Linkbyte.Shift.presentation.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.data.model.Story
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.domain.repository.FriendRepository
import com.Linkbyte.Shift.domain.repository.StoryRepository
import kotlinx.coroutines.flow.collectLatest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    // Grouped by userId for the stories bar
    private val _storyUsers = MutableStateFlow<List<StoryUser>>(emptyList())
    val storyUsers: StateFlow<List<StoryUser>> = _storyUsers.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                friendRepository.getFriends().collect { friends ->
                    val friendIds = friends.map { it.userId }
                    storyRepository.getStories(friendIds).collect { stories ->
                        _stories.value = stories

                        // Group stories by user
                        authRepository.getCurrentUser().collectLatest { currentUser ->
                            val currentUserId = currentUser?.userId ?: ""
                            val grouped = stories.groupBy { it.userId }
                            val users = grouped.map { (userId, userStories) ->
                                val latestStory = userStories.maxByOrNull { it.createdAt } ?: userStories.first()
                                StoryUser(
                                    userId = userId,
                                    displayName = latestStory.displayName,
                                    username = latestStory.username,
                                    stories = userStories,
                                    isOwnStory = userId == currentUserId
                                )
                            }.sortedByDescending { it.isOwnStory } // Own story first

                            _storyUsers.value = users
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun postStory(text: String, backgroundColor: String) {
        viewModelScope.launch {
            _isPosting.value = true
            storyRepository.postStory(text, backgroundColor)
                .onFailure { _error.value = it.message }
            _isPosting.value = false
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId)
                .onFailure { _error.value = it.message }
        }
    }

    fun getStoriesForUser(userId: String): List<Story> {
        return _stories.value.filter { it.userId == userId }
    }

    fun clearError() {
        _error.value = null
    }
}

data class StoryUser(
    val userId: String,
    val displayName: String,
    val username: String,
    val stories: List<Story>,
    val isOwnStory: Boolean
)
