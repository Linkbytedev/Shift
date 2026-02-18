package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.Story
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    suspend fun postStory(text: String, backgroundColor: String): Result<Unit>
    fun getStories(friendIds: List<String>): Flow<List<Story>>
    suspend fun deleteStory(storyId: String): Result<Unit>
}
