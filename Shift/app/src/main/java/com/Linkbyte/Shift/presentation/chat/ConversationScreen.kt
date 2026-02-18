package com.Linkbyte.Shift.presentation.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.data.model.MessageStatus
import com.Linkbyte.Shift.ui.theme.*
import com.Linkbyte.Shift.presentation.chat.DecryptedMessage
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.Linkbyte.Shift.presentation.components.FullScreenImageViewer
import com.Linkbyte.Shift.data.model.MessageType
import androidx.compose.material.icons.outlined.Image
import kotlin.math.max
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType

private fun getRemainingTime(expiresAt: Long?): String? {
    if (expiresAt == null) return null
    val remainingMillis = expiresAt - System.currentTimeMillis()
    if (remainingMillis <= 0) return null

    val seconds = remainingMillis / 1000
    return if (seconds < 60) {
        "${seconds}s"
    } else {
        "${seconds / 60}m"
    }
}


// Custom modifier to workaround ACTION_HOVER_EXIT crash
// See: https://issuetracker.google.com/issues/330543666

fun Modifier.preventHoverExit(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            // Consuming the exit event prevents the crash in some Compose versions
            if (event.type == PointerEventType.Exit) {
                event.changes.forEach { it.consume() }
            }
        }
    }
}

private val messageTimeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatMessageTime(timestamp: Long): String {
    return messageTimeFormatter.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCall: (String, String, Boolean) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val otherUserId by viewModel.otherUserId.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showTimerMenu by remember { mutableStateOf(false) }
    var viewingImage by remember { mutableStateOf<DecryptedMessage?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.sendImageMessage(uri)
        }
    }

    LaunchedEffect(conversationId) {
        viewModel.loadMessages(conversationId)
    }

    // Handle Conversation Deletion
    val conversationDeleted by viewModel.conversationDeleted.collectAsState()
    LaunchedEffect(conversationDeleted) {
        if (conversationDeleted) {
            onNavigateBack()
        }
    }

    // Security Logic
    val conversation by viewModel.conversation.collectAsState()
    val isSecurityRequestPendingForMe by viewModel.isSecurityRequestPendingForMe.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    DisposableEffect(conversation?.screenshotSecurityEnabled) {
        val window = (context as? android.app.Activity)?.window
        if (conversation?.screenshotSecurityEnabled == true) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(enabled = otherUser != null) {
                                    otherUser?.userId?.let { onNavigateToProfile(it) }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            if (otherUser != null) {
                                if (otherUser!!.profileImageUrl.isNotEmpty()) {
                                    coil.compose.AsyncImage(
                                        model = otherUser!!.profileImageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = otherUser!!.displayName.take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        otherUser!!.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    if (otherUser!!.status == "online") {
                                        Text(
                                            "Online",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Chat",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                otherUserId?.let { id ->
                                    onNavigateToCall(id, "AUDIO", false)
                                }
                            },
                            enabled = otherUserId != null
                        ) {
                            Icon(Icons.Default.Call, "Voice Call", tint = if (otherUserId != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }

                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                MessageInputBar(
                    messageText = messageText,
                    onMessageTextChange = { viewModel.updateMessageText(it) },
                    onSendClick = { viewModel.sendMessage() },
                    onTimerClick = { showTimerMenu = true },
                    onImageClick = { imagePickerLauncher.launch("image/*") },
                    isSending = isSending,
                    editingMessage = editingMessage,
                    onCancelEdit = { viewModel.cancelEditing() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                 Column(modifier = Modifier.fillMaxSize()) {
                     if (isSecurityRequestPendingForMe) {
                         SecurityRequestBanner(
                             onApprove = { viewModel.approveScreenshotSecurity() },
                             onDecline = { viewModel.declineScreenshotSecurity() }
                         )
                     }

                     if (messages.isEmpty()) {
                        EmptyMessagesState()
                     } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(messages, key = { it.messageId }) { message ->
                                // Mark as read when displayed
                                LaunchedEffect(message.messageId) {
                                    if (!message.isFromCurrentUser && 
                                        message.status != MessageStatus.OPENED &&
                                        message.type == MessageType.TEXT) {
                                        viewModel.markMessageAsOpened(message.messageId)
                                    }
                                }
                                
                                MessageBubble(
                                    message = message,
                                    onEdit = { viewModel.startEditing(it) },
                                    onDelete = { viewModel.deleteMessage(it) },
                                    onViewImage = { viewingImage = it }
                                )
                            }
                        }
                     }
                 }
            }

            if (showTimerMenu) {
                TimerDialog(
                    onDismiss = { showTimerMenu = false },
                    onTimerSelected = { seconds ->
                        viewModel.sendMessageWithTimer(seconds)
                        showTimerMenu = false
                    }
                )
            }
            
            if (viewingImage != null) {
                FullScreenImageViewer(
                    message = viewingImage!!,
                    viewModel = viewModel,  
                    modifier = Modifier.preventHoverExit(), // Apply workaround
                    onDismiss = {
                        // Delete on close (View Once)
                        viewingImage?.let { image ->
                            if (image.isFromCurrentUser) {
                                // Sender viewed it -> Mark as viewed for sender only (so it stays for recipient)
                                viewModel.markAsViewedBySender(image.messageId)
                            } else {
                                // Recipient viewed it -> Delete for everyone (standard View Once)
                                viewModel.deleteViewedMessage(image)
                            }
                        }
                        viewingImage = null
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: DecryptedMessage,
    onEdit: (DecryptedMessage) -> Unit,
    onDelete: (DecryptedMessage) -> Unit,
    onViewImage: (DecryptedMessage) -> Unit
) {
    val isFromMe = message.isFromCurrentUser
    var showMenu by remember { mutableStateOf(false) }
    val timeRemaining by produceState(initialValue = getRemainingTime(message.expiresAt), key1 = message.expiresAt) {
        if (message.expiresAt != null) {
            while (true) {
                val remaining = getRemainingTime(message.expiresAt)
                if (value != remaining) {
                    value = remaining
                }
                delay(1000)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .preventHoverExit(), // Apply workaround here too
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isFromMe) 18.dp else 4.dp,
                            bottomEnd = if (isFromMe) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (isFromMe && System.currentTimeMillis() - message.timestamp < 5 * 60 * 1000) {
                                showMenu = true
                            }
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (message.type == MessageType.IMAGE) {
                    val isOpened = if (isFromMe) message.senderViewed else message.viewedAt != null
                    
                    if (isOpened) {
                        // Already viewed (should be deleted for recipient, or hidden for sender)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Blip Opened", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        // "View Photo" Button
                         Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onViewImage(message) }
                        ) {
                            Icon(Icons.Outlined.Image, null, tint = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tap to view Blip",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Dropdown Menu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onEdit(message); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) }
                )
                DropdownMenuItem(
                    text = { Text("Unsend", color = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(message); showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.expiresAt != null && timeRemaining != null) {
                    Icon(
                        Icons.Outlined.Timer,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = TextTertiary
                    )
                    Text(
                        text = timeRemaining!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )

                if (isFromMe) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Text(
                        text = when (message.status) {
                            MessageStatus.SENT -> "Sent"
                            MessageStatus.DELIVERED -> "Delivered"
                            MessageStatus.OPENED -> "Read"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
                
                if (message.isEdited) {
                    Text(
                        text = " (Edited)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onTimerClick: () -> Unit,
    onImageClick: () -> Unit,
    isSending: Boolean,
    editingMessage: DecryptedMessage? = null,
    onCancelEdit: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (editingMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, "Editing", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Editing message",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Close,
                        "Cancel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clickable(onClick = onCancelEdit)
                            .size(20.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTimerClick) {
                    Icon(
                        Icons.Outlined.Timer,
                        "Set timer",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            
            IconButton(onClick = onImageClick) {
                Icon(
                    Icons.Outlined.Image,
                    "Send Blip",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Message...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = messageText.isNotBlank() && !isSending,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
        }
    }
}



@Composable
fun EmptyMessagesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Send the first message",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary
            )
        }
    }
}

@Composable
fun TimerDialog(
    onDismiss: () -> Unit,
    onTimerSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text("Disappearing Messages") },
        text = {
            Column {
                val options = listOf(
                    "5 seconds" to 5,
                    "10 seconds" to 10,
                    "30 seconds" to 30,
                    "1 minute" to 60,
                    "5 minutes" to 300
                )

                options.forEach { (label, seconds) ->
                    TextButton(
                        onClick = { onTimerSelected(seconds) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
