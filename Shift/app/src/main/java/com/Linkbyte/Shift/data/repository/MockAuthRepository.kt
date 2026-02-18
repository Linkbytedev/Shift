package com.Linkbyte.Shift.data.repository

import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAuthRepository @Inject constructor() : AuthRepository {
    private val currentUserFlow = MutableStateFlow<User?>(null)
    private val users = mutableMapOf<String, User>()
    private val usernames = mutableSetOf<String>()

    override suspend fun signIn(email: String, password: String): Result<User> {
        val user = users.values.find { it.email == email }
        return if (user != null) {
            currentUserFlow.value = user
            Result.success(user)
        } else {
            Result.failure(Exception("User not found"))
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<User> {
        if (usernames.contains(username.lowercase())) {
            return Result.failure(Exception("Username already taken"))
        }

        val userId = "mock_user_${System.currentTimeMillis()}"
        val user = User(
            userId = userId,
            username = username,
            usernameLowercase = username.lowercase(),
            email = email,
            displayName = displayName,
            publicKey = "mock_public_key",
            status = "online"
        )
        
        users[userId] = user
        usernames.add(username.lowercase())
        currentUserFlow.value = user
        return Result.success(user)
    }

    override suspend fun signOut() {
        currentUserFlow.value = null
    }

    override fun getCurrentUser(): Flow<User?> = currentUserFlow

    override suspend fun isUsernameAvailable(username: String): Boolean {
        return !usernames.contains(username.lowercase())
    }
}
