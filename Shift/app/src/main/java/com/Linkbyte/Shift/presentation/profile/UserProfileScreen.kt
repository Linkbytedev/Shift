package com.Linkbyte.Shift.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import com.Linkbyte.Shift.presentation.chat.TimerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String,
    conversationId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val user by viewModel.user.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    var showBlockDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showRemoveFriendDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadData(userId, conversationId)
    }

    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(message = error!!)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(message = successMessage!!)
            viewModel.clearSuccess()
        }
    }

    if (showRemoveFriendDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFriendDialog = false },
            title = { Text("Remove Friend") },
            text = { Text("Are you sure you want to remove this user from your friends?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.removeFriend()
                    showRemoveFriendDialog = false
                    onNavigateBack()
                }) {
                    Text("Remove", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveFriendDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block User") },
            text = { Text("Are you sure you want to block this user? They won't be able to contact you.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.blockUser()
                    showBlockDialog = false
                    onNavigateBack() // Go back after blocking
                }) {
                    Text("Block", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearChatDialog) {
         AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat") },
            text = { Text("Are you sure you want to clear all messages in this conversation? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearChat()
                    showClearChatDialog = false
                }) {
                    Text("Clear", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTimerDialog) {
        TimerDialog(
            onDismiss = { showTimerDialog = false },
            onTimerSelected = { 
                viewModel.setTimer(it)
                showTimerDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(user?.displayName ?: "Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else if (user != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    if (user!!.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user!!.profileImageUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user!!.displayName.take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = user!!.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user!!.username}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (user!!.status.isNotEmpty()) {
                         Spacer(modifier = Modifier.height(8.dp))
                         SuggestionChip(
                            onClick = {},
                            label = { Text(user!!.status.replaceFirstChar { char: Char -> char.uppercase() }) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = when(user!!.status) {
                                    "online" -> Success.copy(alpha = 0.1f)
                                    "away" -> Warning.copy(alpha = 0.1f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                labelColor = when(user!!.status) {
                                    "online" -> Success
                                    "away" -> Warning
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = Color.Transparent
                            )
                         )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ShiftButton(
                            text = "Message",
                            onClick = { 
                                viewModel.startConversation { convId: String ->
                                    onNavigateToChat(convId)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(18.dp)) },
                            enabled = user != null
                        )
                        
                        ShiftButton(
                            text = "Call",
                            onClick = { /* TODO Calling */ },
                            modifier = Modifier.weight(1f),
                            variant = ButtonVariant.Secondary,
                            icon = { Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp)) },
                            enabled = false // Not implemented in this view
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Conversation Settings
                    if (conversation != null) {
                        SectionHeader("Conversation Settings")
                        
                        ShiftGlassCard {
                            Column {
                                val isMuted = conversation!!.mutedBy.contains(viewModel.currentUserId)
                                SettingItem(
                                    icon = if (isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
                                    title = "Mute Notifications",
                                    subtitle = if (isMuted) "Notifications are muted" else "Receive notifications",
                                    trailing = {
                                        Switch(
                                            checked = isMuted,
                                            onCheckedChange = { viewModel.toggleMute() }
                                        )
                                    }
                                )
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                
                                val timerSeconds = conversation?.messageTimer
                                val timerText = when {
                                    timerSeconds == null -> "Off"
                                    timerSeconds < 60 -> "$timerSeconds seconds"
                                    timerSeconds < 3600 -> "${timerSeconds / 60} minutes"
                                    else -> "Custom"
                                }
                                
                                SettingItem(
                                    icon = Icons.Outlined.Timer,
                                    title = "Disappearing Messages",
                                    subtitle = timerText,
                                    onClick = { showTimerDialog = true }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                                val screenshotSecurityEnabled = conversation!!.screenshotSecurityEnabled
                                val screenshotRequesterId = conversation!!.screenshotSecurityRequesterId
                                val isPending = !screenshotSecurityEnabled && screenshotRequesterId != null

                                SettingItem(
                                    icon = Icons.Filled.Lock,
                                    title = "Screenshot Security",
                                    subtitle = when {
                                        screenshotSecurityEnabled -> "Screenshots blocked"
                                        isPending -> if (screenshotRequesterId == viewModel.currentUserId) "Request sent" else "Request received"
                                        else -> "Block screenshots"
                                    },
                                    trailing = {
                                         Switch(
                                             checked = screenshotSecurityEnabled || isPending,
                                             onCheckedChange = { 
                                                 // If pending or enabled, toggling it means DISABLE/CANCEL
                                                 // If disabled, toggling it means ENABLE/REQUEST
                                                 val newValue = !(screenshotSecurityEnabled || isPending)
                                                 viewModel.toggleScreenshotSecurity(newValue)
                                             },
                                             thumbContent = if (isPending) {
                                                 { Icon(Icons.Filled.Check, null, modifier = Modifier.size(12.dp)) }
                                             } else null
                                         )
                                    }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }


    // ... existing dialogs ...

                    // Privacy & Support
                    SectionHeader("Privacy & Support")
                    ShiftGlassCard {
                        Column {
                            SettingItem(
                                icon = Icons.Outlined.Person,
                                title = "Remove Friend",
                                subtitle = "Remove from friends list",
                                onClick = { showRemoveFriendDialog = true },
                                titleColor = Error,
                                iconColor = Error
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            SettingItem(
                                icon = Icons.Default.Block,
                                title = "Block User",
                                subtitle = "Stop receiving messages",
                                onClick = { showBlockDialog = true },
                                titleColor = Error,
                                iconColor = Error
                            )
                            
                            if (conversation != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                SettingItem(
                                    icon = Icons.Default.Delete,
                                    title = "Clear Chat",
                                    subtitle = "Delete all messages",
                                    onClick = { showClearChatDialog = true },
                                    titleColor = Error,
                                    iconColor = Error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconColor)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            trailing()
        }
    }
}

