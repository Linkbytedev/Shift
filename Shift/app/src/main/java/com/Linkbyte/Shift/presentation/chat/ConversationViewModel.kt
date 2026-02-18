package com.Linkbyte.Shift.presentation.chat

import com.Linkbyte.Shift.domain.repository.UserRepository
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.data.model.Conversation
// import com.google.firebase.firestore.ListenerRegistration (Removed)
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.domain.repository.MessageRepository
import com.Linkbyte.Shift.domain.repository.UserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import com.Linkbyte.Shift.security.encryption.EncryptionService
import com.Linkbyte.Shift.security.keystore.KeyStoreManager
import com.Linkbyte.Shift.data.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val encryptionService: EncryptionService,
    private val keyStoreManager: KeyStoreManager,
    private val authRepository: AuthRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val signalingClient: com.Linkbyte.Shift.webrtc.SignalingClient,
    private val webRtcClient: com.Linkbyte.Shift.webrtc.WebRtcClient
    ) : ViewModel() {
    
    // ... existing ...

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                currentUserId = user?.userId ?: ""
            }
        }
    }
    private var currentConversationId: String = ""

    private val _conversation = MutableStateFlow<Conversation?>(null)
    val conversation: StateFlow<Conversation?> = _conversation.asStateFlow()

    private val _messages = MutableStateFlow<List<DecryptedMessage>>(emptyList())
    val messages: StateFlow<List<DecryptedMessage>> = _messages.asStateFlow()

    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _editingMessage = MutableStateFlow<DecryptedMessage?>(null)
    val editingMessage: StateFlow<DecryptedMessage?> = _editingMessage.asStateFlow()

    private val _otherUserId = MutableStateFlow<String?>(null)
    val otherUserId: StateFlow<String?> = _otherUserId.asStateFlow()

    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser.asStateFlow()
    
    private val _conversationDeleted = MutableStateFlow(false)
    val conversationDeleted: StateFlow<Boolean> = _conversationDeleted.asStateFlow()
    
    val isSecurityRequestPendingForMe: StateFlow<Boolean> = _conversation.map { conv ->
        conv != null && !conv.screenshotSecurityEnabled && 
        conv.screenshotSecurityRequesterId != null && 
        conv.screenshotSecurityRequesterId != currentUserId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    
    private var messageCollectionJob: Job? = null

    fun loadMessages(conversationId: String) {
        if (currentConversationId == conversationId && messageCollectionJob?.isActive == true) return
        
        messageCollectionJob?.cancel()
        currentConversationId = conversationId
        _conversationDeleted.value = false 
        
        messageCollectionJob = viewModelScope.launch {
            // Mocking conversation load
            messageRepository.getConversation(conversationId).onSuccess { conv ->
                _conversation.value = conv
                val otherId = conv.participantIds.find { it != currentUserId }
                if (otherId != null) {
                    userRepository.getUser(otherId).onSuccess { _otherUser.value = it }
                }
            }

            // Mark conversation as read when opened
            launch {
                try {
                    messageRepository.markConversationAsRead(conversationId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            messageRepository.getMessages(conversationId).collect { encryptedMessages ->
                val decrypted = decryptMessages(conversationId, encryptedMessages)
                _messages.value = decrypted
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
    }
    
    fun approveScreenshotSecurity() {
        viewModelScope.launch {
            messageRepository.approveScreenshotSecurity(currentConversationId)
        }
    }
    
    fun declineScreenshotSecurity() {
        viewModelScope.launch {
            messageRepository.declineScreenshotSecurity(currentConversationId)
        }
    }
    
    fun updateMessageText(text: String) {
        _messageText.value = text
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        val editingMsg = _editingMessage.value
        
        if (text.isBlank() || _isSending.value) return
        
        viewModelScope.launch {
            _isSending.value = true // Start loading immediately to prevent double clicks
            
            if (editingMsg != null) {
                // Handle Edit
                editMessage(editingMsg, text)
                _editingMessage.value = null
            } else {
                // Handle New Message
                messageRepository.sendTextMessage(
                    conversationId = currentConversationId,
                    content = text,
                    expiresInSeconds = null
                )
            }
            
            _messageText.value = ""
            _isSending.value = false
        }
    }
    
    fun startEditing(message: DecryptedMessage) {
        _editingMessage.value = message
        _messageText.value = message.content
    }
    
    fun cancelEditing() {
        _editingMessage.value = null
        _messageText.value = ""
    }
    
    fun sendMessageWithTimer(expiresInSeconds: Int) {
        val text = _messageText.value.trim()
        if (text.isBlank() || _isSending.value) return
        
        viewModelScope.launch {
            _isSending.value = true
            messageRepository.sendTextMessage(
                conversationId = currentConversationId,
                content = text,
                expiresInSeconds = expiresInSeconds
            )
            _messageText.value = ""
            _isSending.value = false
        }
    }

    fun sendImageMessage(uri: android.net.Uri) {
        viewModelScope.launch {
            _isSending.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.use { it.readBytes() }
                
                if (bytes != null) {
                    messageRepository.sendImageMessage(
                        conversationId = currentConversationId,
                        imageData = bytes,
                        expiresInSeconds = null // Or default?
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isSending.value = false
        }
    }
    
    fun markMessageAsOpened(messageId: String) {
        viewModelScope.launch {
            messageRepository.markAsOpened(messageId, currentConversationId)
        }
    }

    fun markAsViewedBySender(messageId: String) {
        viewModelScope.launch {
            // Mocking Firestore update
            Log.d("ConversationViewModel", "Mock: Message $messageId marked as viewed by sender")
        }
    }

    fun editMessage(message: DecryptedMessage, newContent: String) {
        // 5-minute check
        if (System.currentTimeMillis() - message.timestamp > 5 * 60 * 1000) {
            // Ideally show an error to user
            return
        }
        
        viewModelScope.launch {
            val minimalMessage = Message(
                messageId = message.messageId,
                conversationId = currentConversationId
            )
            messageRepository.editMessage(minimalMessage, newContent)
        }
    }

    fun deleteMessage(message: DecryptedMessage) {
        // 5-minute check for "Unsend"
        if (System.currentTimeMillis() - message.timestamp > 5 * 60 * 1000) {
            return
        }
        
        viewModelScope.launch {
            messageRepository.deleteMessage(message.messageId)
        }
    }

    fun deleteViewedMessage(message: DecryptedMessage) {
        // Bypass time check for "View Once" cleanup
        viewModelScope.launch {
            messageRepository.deleteMessage(message.messageId)
        }
    }

    suspend fun getDecryptedImage(message: DecryptedMessage): ByteArray? {
        return try {
            val key = messageRepository.getEncryptionKey(currentConversationId)
            
            // Download encrypted bytes
            val encryptedBytes = withContext(Dispatchers.IO) {
                URL(message.content).readBytes()
            }
            
            // Decrypt
            encryptionService.decryptBytes(encryptedBytes, message.iv, key)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private suspend fun decryptMessages(conversationId: String, messages: List<Message>): List<DecryptedMessage> {
        val key = try {
            messageRepository.getEncryptionKey(conversationId)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
        
        return messages.mapNotNull { message ->
            try {
                val decryptedContent = if (message.type == com.Linkbyte.Shift.data.model.MessageType.IMAGE) {
                    message.encryptedContent // It's the URL
                } else {
                    encryptionService.decrypt(
                        encryptedContent = message.encryptedContent,
                        iv = message.iv,
                        key = key
                    )
                }
                
                DecryptedMessage(
                    messageId = message.messageId,
                    content = decryptedContent,
                    senderId = message.senderId,
                    timestamp = message.timestamp,
                    status = message.status,
                    expiresAt = message.expiresAt,
                    isFromCurrentUser = message.senderId == currentUserId,
                    isEdited = message.isEdited,
                    type = message.type,
                    iv = message.iv,
                    viewedAt = message.viewedAt,
                    senderViewed = message.senderViewed
                )
            } catch (e: Exception) {
                null // Skip messages that can't be decrypted
            }
        }
    }
}

data class DecryptedMessage(
    val messageId: String,
    val content: String,
    val senderId: String,
    val timestamp: Long,
    val status: com.Linkbyte.Shift.data.model.MessageStatus,
    val expiresAt: Long?,
    val isFromCurrentUser: Boolean,
    val isEdited: Boolean = false,
    val type: com.Linkbyte.Shift.data.model.MessageType,
    val iv: String,
    val viewedAt: Long? = null,
    val senderViewed: Boolean = false
)
