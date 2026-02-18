package com.Linkbyte.Shift.data.model

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val text: String = "",
    val backgroundColor: String = "gradient_cyan", // gradient preset name
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86_400_000 // 24 hours
)
