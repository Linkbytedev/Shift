package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.FriendRequest
import com.Linkbyte.Shift.data.model.User
import kotlinx.coroutines.flow.Flow

interface FriendRepository {
    suspend fun sendFriendRequest(toUserId: String): Result<Unit>
    suspend fun acceptFriendRequest(requestId: String): Result<Unit>
    suspend fun declineFriendRequest(requestId: String): Result<Unit>
    fun getPendingRequests(): Flow<List<FriendRequest>>
    fun getSentRequests(): Flow<List<FriendRequest>>
    fun getFriends(): Flow<List<User>>
    suspend fun removeFriend(friendId: String): Result<Unit>
}
