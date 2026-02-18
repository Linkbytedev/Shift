package com.Linkbyte.Shift.data.model

data class Conversation(
    val conversationId: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(), // userId to displayName
    val participantAvatars: Map<String, String> = emptyMap(), // userId to avatar URL
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Map<String, Int> = emptyMap(), // userId to unread count
    val createdAt: Long = System.currentTimeMillis(),
    val encryptionKey: String = "", // Shared AES key (base64) for message encryption
    val deletedBy: List<String> = emptyList(), // User IDs who have deleted this conversation
    val archivedBy: List<String> = emptyList(), // User IDs who have archived this conversation
    val customNames: Map<String, String> = emptyMap(), // User ID to custom conversation name
    val clearedAt: Map<String, Long> = emptyMap(), // User ID to timestamp (messages before this are hidden)
    val mutedBy: List<String> = emptyList(), // User IDs who have muted this conversation
    val messageTimer: Int? = null, // Default disappearing message timer in seconds (null = off)
    val screenshotSecurityEnabled: Boolean = false, // Request approval is required to enable
    val screenshotSecurityRequesterId: String? = null // ID of user requesting to enable security
)
