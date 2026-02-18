package com.Linkbyte.Shift.domain.repository

import com.Linkbyte.Shift.data.model.Conversation
import com.Linkbyte.Shift.data.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun sendTextMessage(conversationId: String, content: String, expiresInSeconds: Int?): Result<Message>
    suspend fun sendImageMessage(conversationId: String, imageData: ByteArray, expiresInSeconds: Int?): Result<Message>
    fun getMessages(conversationId: String): Flow<List<Message>>
    suspend fun markAsDelivered(messageId: String, conversationId: String)
    suspend fun markAsOpened(messageId: String, conversationId: String)
    fun getConversations(): Flow<List<Conversation>>
    suspend fun createConversation(otherUserId: String): Result<String>
    suspend fun deleteExpiredMessages()
    suspend fun deleteConversationWithUser(otherUserId: String): Result<Unit>
    suspend fun getEncryptionKey(conversationId: String): javax.crypto.SecretKey
    suspend fun editMessage(message: Message, newContent: String): Result<Unit>
    suspend fun deleteMessage(messageId: String): Result<Unit>
    suspend fun deleteAllUserMessages(userId: String)
    suspend fun deleteAllUserConversations(userId: String)
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun archiveConversation(conversationId: String): Result<Unit>
    suspend fun unarchiveConversation(conversationId: String): Result<Unit>
    suspend fun renameConversation(conversationId: String, newName: String): Result<Unit>
    fun getArchivedConversations(): Flow<List<Conversation>>
    suspend fun markConversationAsRead(conversationId: String)
    suspend fun setConversationMuted(conversationId: String, isMuted: Boolean): Result<Unit>
    suspend fun setConversationTimer(conversationId: String, expiresInSeconds: Int?): Result<Unit>
    suspend fun clearConversationMessages(conversationId: String): Result<Unit>
    suspend fun getConversation(conversationId: String): Result<Conversation>
    suspend fun requestScreenshotSecurity(conversationId: String, requesterId: String): Result<Unit>
    suspend fun approveScreenshotSecurity(conversationId: String): Result<Unit>
    suspend fun declineScreenshotSecurity(conversationId: String): Result<Unit>
    suspend fun disableScreenshotSecurity(conversationId: String): Result<Unit>
    suspend fun deleteConversationPermanently(conversationId: String): Result<Unit>
}
