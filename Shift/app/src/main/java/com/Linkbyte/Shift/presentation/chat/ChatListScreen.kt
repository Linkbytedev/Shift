package com.Linkbyte.Shift.presentation.chat

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
// import com.google.firebase.auth.FirebaseAuth (Removed)
import com.Linkbyte.Shift.data.model.Conversation
import com.Linkbyte.Shift.presentation.stories.StoryUser
import com.Linkbyte.Shift.presentation.stories.StoryViewModel
import com.Linkbyte.Shift.presentation.stories.getStoryGradient
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onNavigateToConversation: (String) -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStoryViewer: (String) -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    // val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentUserId = "mock_user" // Stub for UI visualization

    val storyUsers by storyViewModel.storyUsers.collectAsState()
    val isPosting by storyViewModel.isPosting.collectAsState()

    var showComposeStory by remember { mutableStateOf(false) }

    if (showComposeStory) {
        ComposeStoryDialog(
            isPosting = isPosting,
            onDismiss = { showComposeStory = false },
            onPost = { text, bgColor ->
                storyViewModel.postStory(text, bgColor)
                showComposeStory = false
            }
        )
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
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Messages",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToFriends) {
                            Icon(Icons.Outlined.PersonAdd, "Friends", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Outlined.AccountCircle, "Profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToFriends() },
                    containerColor = AccentBlue,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, "New Chat")
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    error != null -> {
                        ErrorState(
                            message = error ?: "Unknown error",
                            onRetry = { }
                        )
                    }
                    isLoading && conversations.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = AccentBlue)
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            // Stories Bar
                            item {
                                StoriesBar(
                                    storyUsers = storyUsers,
                                    onAddStory = { showComposeStory = true },
                                    onStoryClick = { userId -> onNavigateToStoryViewer(userId) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            }

                            if (conversations.isEmpty()) {
                                item { EmptyConversationsState() }
                            } else {
                                items(conversations, key = { it.conversationId }) { conversation ->
                                    ConversationItem(
                                        conversation = conversation,
                                        currentUserId = currentUserId,
                                        onClick = { onNavigateToConversation(conversation.conversationId) },
                                        onDelete = { viewModel.deleteConversation(conversation.conversationId) },
                                        onRename = { newName -> viewModel.renameConversation(conversation.conversationId, newName) }
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(start = 72.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesBar(
    storyUsers: List<StoryUser>,
    onAddStory: () -> Unit,
    onStoryClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Add Story button
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onAddStory() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Story",
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "My Story",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Friend stories
        items(storyUsers, key = { it.userId }) { storyUser ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onStoryClick(storyUser.userId) }
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .border(2.dp, AccentBlue, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = storyUser.displayName.take(1).uppercase(),
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (storyUser.isOwnStory) "You" else storyUser.displayName.split(" ").first(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ComposeStoryDialog(
    isPosting: Boolean,
    onDismiss: () -> Unit,
    onPost: (text: String, bgColor: String) -> Unit
) {
    var storyText by remember { mutableStateOf("") }
    var selectedGradient by remember { mutableStateOf("gradient_cyan") }

    val gradientOptions = listOf(
        "gradient_cyan" to "Ocean",
        "gradient_purple" to "Purple",
        "gradient_sunset" to "Sunset",
        "gradient_forest" to "Forest",
        "gradient_midnight" to "Night",
        "gradient_pink" to "Pink"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = getStoryGradient(selectedGradient))
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(
                    value = storyText,
                    onValueChange = { if (it.length <= 280) storyText = it },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    ),
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${storyText.length}/280",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(gradientOptions) { (gradientName, _) ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(brush = getStoryGradient(gradientName))
                                .then(
                                    if (selectedGradient == gradientName)
                                        Modifier.border(2.5.dp, Color.White, CircleShape)
                                    else
                                        Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                .clickable { selectedGradient = gradientName }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onPost(storyText.trim(), selectedGradient) },
                    enabled = storyText.isNotBlank() && !isPosting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        disabledContainerColor = Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Share Story",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRename: (String) -> Unit
) {
    val otherUserId = conversation.participantIds.firstOrNull { it != currentUserId } ?: ""
    val defaultName = conversation.participantNames[otherUserId] ?: "Unknown"
    val displayName = conversation.customNames[currentUserId] ?: defaultName
    val unreadCount = conversation.unreadCount[currentUserId] ?: 0

    var showOptionsDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(displayName) }

    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Chat Options", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("What would you like to do with this conversation?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                // Rename Button
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        newName = displayName
                        showRenameDialog = true
                    }
                ) { Text("Rename", color = AccentBlue) }
            },
            dismissButton = {
                // Archive Button
                TextButton(
                    onClick = {
                        showOptionsDialog = false
                        onDelete() // This calls archive
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text("Archive") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Chat", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter a new name for this chat:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShiftTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = "Name",
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        onRename(newName)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AccentBlue)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showOptionsDialog = true }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (conversation.lastMessageTime > 0) {
                        Text(
                            text = formatTime(conversation.lastMessageTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessage.ifEmpty { "No messages yet" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (unreadCount > 0) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AccentBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyConversationsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.Chat,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap + to start a new chat",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        ShiftButton(
            text = "Retry",
            onClick = onRetry,
            variant = ButtonVariant.Secondary,
            modifier = Modifier.widthIn(min = 120.dp)
        )
    }
}

private val hourMinuteFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dayFormatter = SimpleDateFormat("EEE", Locale.getDefault())
private val monthDayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> hourMinuteFormatter.format(Date(timestamp))
        diff < 604800_000 -> dayFormatter.format(Date(timestamp))
        else -> monthDayFormatter.format(Date(timestamp))
    }
}
