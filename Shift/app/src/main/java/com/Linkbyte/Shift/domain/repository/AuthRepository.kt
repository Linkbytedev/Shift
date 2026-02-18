package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<User>
    suspend fun signOut()
    fun getCurrentUser(): Flow<User?>
    suspend fun isUsernameAvailable(username: String): Boolean
}
