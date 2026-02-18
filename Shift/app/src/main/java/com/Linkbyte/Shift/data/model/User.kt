package com.Linkbyte.Shift.data.model

data class User(
    val userId: String = "",
    val username: String = "",
    val usernameLowercase: String = "",
    val email: String = "",
    val displayName: String = "",
    val profileImageUrl: String = "",
    val publicKey: String = "", // For key exchange
    val status: String = "offline", // online, offline, away
    val createdAt: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList()
)
