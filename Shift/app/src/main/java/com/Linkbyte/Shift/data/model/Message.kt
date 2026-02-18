package com.Linkbyte.Shift.data.model

data class Message(
    val messageId: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val encryptedContent: String = "", // Base64 encrypted content
    val iv: String = "", // Initialization vector for decryption
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT,
    val expiresAt: Long? = null, // Timestamp when message should self-destruct
    val viewedAt: Long? = null, // When message was first viewed (for view-based destruction)
    val senderViewed: Boolean = false, // If the sender has viewed their own message
    val isExpired: Boolean = false,
    val isEdited: Boolean = false,
    val editedAt: Long? = null
)

enum class MessageType {
    TEXT,
    IMAGE
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    OPENED
}
