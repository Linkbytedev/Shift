package com.Linkbyte.Shift.service

import android.app.NotificationChannel
import android.util.Log
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.Linkbyte.Shift.MainActivity
import com.Linkbyte.Shift.R
import com.Linkbyte.Shift.domain.repository.FriendRepository
import com.Linkbyte.Shift.domain.repository.MessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : Service() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var friendRepository: FriendRepository
    
    @Inject
    lateinit var authRepository: com.Linkbyte.Shift.domain.repository.AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isFirstMessageLoad = true
    private var isFirstRequestLoad = true
    private val unreadCounts = mutableMapOf<String, Int>() // conversationId -> unreadCount
    private var pendingRequestCount = 0

    companion object {
        const val CHANNEL_ID_FOREGROUND = "shift_service_channel"
        const val CHANNEL_ID_MESSAGES = "shift_messages_channel"
        const val CHANNEL_ID_REQUESTS = "shift_requests_channel"
        const val NOTIFICATION_ID_FOREGROUND = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID_FOREGROUND, createForegroundNotification())
        
        serviceScope.launch {
            authRepository.getCurrentUser().collectLatest { user ->
                if (user != null) {
                    observeMessages(user.userId)
                    observeFriendRequests()
                } else {
                    stopSelf()
                }
            }
        }

        return START_STICKY
    }

    private fun observeMessages(userId: String) {
        serviceScope.launch {
            try {
                messageRepository.getConversations().collectLatest { conversations ->
                    if (isFirstMessageLoad) {
                        // Initialize cache without notifying
                        conversations.forEach { conv ->
                            unreadCounts[conv.conversationId] = conv.unreadCount[userId] ?: 0
                        }
                        isFirstMessageLoad = false
                        return@collectLatest
                    }

                    conversations.forEach { conv ->
                        val currentUnread = conv.unreadCount[userId] ?: 0
                        val previousUnread = unreadCounts[conv.conversationId] ?: 0

                        if (currentUnread > previousUnread) {
                            // New message!
                            val senderName = conv.participantNames.entries.firstOrNull { it.key != userId }?.value ?: "Someone"
                            sendNotification(
                                CHANNEL_ID_MESSAGES,
                                "New Message",
                                "$senderName sent you a message",
                                conv.conversationId.hashCode() // Unique ID per conversation
                            )
                        }
                        unreadCounts[conv.conversationId] = currentUnread
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationService", "Error observing messages", e)
                // If it's a permission denied/auth error, we might want to stop the service
                if (e.message?.contains("permission-denied", ignoreCase = true) == true) {
                    stopSelf()
                }
            }
        }
    }

    private fun observeFriendRequests() {
        serviceScope.launch {
            try {
                friendRepository.getPendingRequests().collectLatest { requests ->
                     if (isFirstRequestLoad) {
                        pendingRequestCount = requests.size
                        isFirstRequestLoad = false
                        return@collectLatest
                    }

                    if (requests.size > pendingRequestCount) {
                        // New friend request!
                        val newRequest = requests.maxByOrNull { it.createdAt }
                        val senderName = newRequest?.fromUsername ?: "Someone"
                        sendNotification(
                            CHANNEL_ID_REQUESTS,
                            "New Friend Request",
                            "$senderName wants to be friends",
                            (newRequest?.requestId.hashCode())
                        )
                    }
                    pendingRequestCount = requests.size
                }
            } catch (e: Exception) {
                Log.e("NotificationService", "Error observing friend requests", e)
            }
        }
    }

    private fun createForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
        .setContentTitle("Shift is running")
        .setContentText("Listening for new messages...")
        .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun sendNotification(channelId: String, title: String, content: String, notificationId: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            // Foreground Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            // Messages Channel
            val messageChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                description = "Notifications for new messages"
            }
            manager.createNotificationChannel(messageChannel)

             // Requests Channel
            val requestChannel = NotificationChannel(
                CHANNEL_ID_REQUESTS,
                "Friend Requests",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                enableVibration(true)
                description = "Notifications for friend requests"
            }
            manager.createNotificationChannel(requestChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
