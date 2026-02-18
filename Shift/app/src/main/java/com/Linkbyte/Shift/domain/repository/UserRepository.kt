package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun getUser(userId: String): Result<User>
    suspend fun uploadProfilePicture(imageData: ByteArray): Result<String>
    suspend fun blockUser(userId: String): Result<Unit>
    suspend fun unblockUser(userId: String): Result<Unit>
    suspend fun getBlockedUsers(): Result<List<User>>
    suspend fun deleteAccount(): Result<Unit>
}
