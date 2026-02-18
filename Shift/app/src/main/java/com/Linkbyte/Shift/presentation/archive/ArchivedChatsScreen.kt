package com.Linkbyte.Shift.presentation.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
// import com.Linkbyte.Shift.presentation.chat.components.ChatListItem
import com.Linkbyte.Shift.ui.theme.GlassPanel
import com.Linkbyte.Shift.ui.theme.PremiumGradient
// import com.Linkbyte.Shift.ui.components.* // If needed later
import com.Linkbyte.Shift.data.model.Conversation
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedChatsScreen(
    viewModel: ArchiveViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archived Chats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Fallback
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (conversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No archived chats",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.conversationId }) { conversation ->
                        ArchivedChatItem(
                            conversation = conversation,
                            currentUserId = currentUserId,
                            onUnarchive = { viewModel.unarchiveConversation(conversation.conversationId) },
                            onDelete = { viewModel.deleteConversation(conversation.conversationId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivedChatItem(
    conversation: Conversation,
    currentUserId: String,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit
) {
    // Simplified item for archive with actions
    // Reusing ChatListItem? No, different actions.
    // Let's build a custom one or wrap logic.
    
    val otherUserId = conversation.participantIds.firstOrNull { it != currentUserId } ?: return
    val displayName = conversation.participantNames[otherUserId] ?: "Unknown"
    val avatarUrl = conversation.participantAvatars[otherUserId]
    val lastMessage = conversation.lastMessage

    SwipeToReveal(
        onUnarchive = onUnarchive,
        onDelete = onDelete
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth()
            // backgroundColor and borderColor handled by new default
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Avatar (Simple placeholder or load logic if implemented elsewhere)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToReveal(
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onUnarchive()
                   true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50) // Green for unarchive
                SwipeToDismissBoxValue.EndToStart -> Color(0xFFE53935) // Red for delete
                else -> Color.Transparent
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Unarchive",
                        tint = Color.White // Keep white on colored surface
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White // Keep white on colored surface
                    )
                }
            }
        },
        content = { content() }
    )
}
