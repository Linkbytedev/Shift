package com.Linkbyte.Shift.presentation.friends

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.Linkbyte.Shift.data.model.FriendRequest
import com.Linkbyte.Shift.data.model.User
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConversation: (String) -> Unit,
    onNavigateToFriendProfile: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Friends", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    edgePadding = 16.dp,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline) },
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            Box(
                                Modifier
                                    .tabIndicatorOffset(tabPositions[selectedTab])
                                    .height(2.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(AccentBlue, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("My Friends", color = if (selectedTab == 0) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Requests", color = if (selectedTab == 1) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (pendingRequests.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = Error) {
                                        Text(
                                            text = pendingRequests.size.toString(),
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Search", color = if (selectedTab == 2) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Blocked", color = if (selectedTab == 3) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> FriendsListTab(
                            friends = friends,
                            isLoading = isLoading,
                            onStartChat = { friend ->
                                viewModel.startConversation(friend.userId) { conversationId ->
                                    onNavigateToConversation(conversationId)
                                }
                            },
                            onRemoveFriend = { viewModel.removeFriend(it.userId) },
                            onBlockUser = { viewModel.blockUser(it.userId) },
                            onFriendClick = { friend -> onNavigateToFriendProfile(friend.userId) }
                        )
                        1 -> RequestsTab(
                            requests = pendingRequests,
                            isLoading = isLoading,
                            onAccept = { viewModel.acceptRequest(it.requestId) },
                            onDecline = { viewModel.declineRequest(it.requestId) }
                        )
                        2 -> SearchTab(
                            searchQuery = searchQuery,
                            onSearchQueryChange = {
                                searchQuery = it
                                viewModel.searchUsers(it)
                            },
                            searchResults = searchResults,
                            isLoading = isLoading,
                            onSendRequest = { viewModel.sendFriendRequest(it.userId) },
                            onBlockUser = { viewModel.blockUser(it.userId) }
                        )
                        3 -> BlockedUsersTab(
                            blockedUsers = blockedUsers,
                            isLoading = isLoading,
                            onUnblock = { viewModel.unblockUser(it.userId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendsListTab(
    friends: List<User>,
    isLoading: Boolean,
    onStartChat: (User) -> Unit,
    onRemoveFriend: (User) -> Unit,
    onBlockUser: (User) -> Unit,
    onFriendClick: (User) -> Unit
) {
    if (isLoading && friends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    if (friends.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Group,
            title = "No friends yet",
            subtitle = "Search for users to add them as friends."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(friends, key = { it.userId }) { friend ->
            FriendItem(
                user = friend,
                onClick = { onFriendClick(friend) },
                onChatClick = { onStartChat(friend) },
                onRemove = { onRemoveFriend(friend) },
                onBlock = { onBlockUser(friend) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(start = 64.dp))
        }
    }
}

@Composable
private fun FriendItem(
    user: User,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        ConfirmDialog(
            title = "Remove Friend",
            message = "Are you sure you want to remove ${user.displayName}?",
            confirmText = "Remove",
            onConfirm = { onRemove(); showRemoveDialog = false },
            onDismiss = { showRemoveDialog = false },
            isDestructive = true
        )
    }

    if (showBlockDialog) {
        ConfirmDialog(
            title = "Block User",
            message = "Block ${user.displayName}? They won't be able to contact you.",
            confirmText = "Block",
            onConfirm = { onBlock(); showBlockDialog = false },
            onDismiss = { showBlockDialog = false },
            isDestructive = true
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user = user, size = 48)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onChatClick) {
            Icon(Icons.AutoMirrored.Outlined.Chat, "Chat", tint = AccentBlue)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Remove Friend") },
                    onClick = { showMenu = false; showRemoveDialog = true },
                    leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) }
                )
                DropdownMenuItem(
                    text = { Text("Block", color = Error) },
                    onClick = { showMenu = false; showBlockDialog = true },
                    leadingIcon = { Icon(Icons.Outlined.Block, null, tint = Error) }
                )
            }
        }
    }
}

@Composable
private fun RequestsTab(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAccept: (FriendRequest) -> Unit,
    onDecline: (FriendRequest) -> Unit
) {
    if (isLoading && requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    if (requests.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.PersonAdd,
            title = "No pending requests",
            subtitle = "Friend requests will appear here."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests, key = { it.requestId }) { request ->
            ShiftGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = request.fromDisplayName.take(1).uppercase(),
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.fromDisplayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "@${request.fromUsername}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShiftButton(
                        text = "Accept",
                        onClick = { onAccept(request) },
                        modifier = Modifier.weight(1f)
                    )
                    ShiftButton(
                        text = "Decline",
                        onClick = { onDecline(request) },
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTab(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<User>,
    isLoading: Boolean,
    onSendRequest: (User) -> Unit,
    onBlockUser: (User) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ShiftTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = "Search username...",
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentBlue)
            }
        } else if (searchQuery.isNotEmpty() && searchResults.isEmpty()) {
            EmptyStateContent(
                icon = Icons.Outlined.Search,
                title = "No users found",
                subtitle = "Try searching for a different username."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(searchResults, key = { it.userId }) { user ->
                    SearchResultItem(
                        user = user,
                        onAddFriend = { onSendRequest(user) },
                        onBlock = { onBlockUser(user) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    user: User,
    onAddFriend: () -> Unit,
    onBlock: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user = user, size = 42)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ShiftButton(
            text = "Add",
            onClick = onAddFriend,
            modifier = Modifier.height(36.dp)
        )

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Block", color = Error) },
                    onClick = { showMenu = false; onBlock() },
                    leadingIcon = { Icon(Icons.Outlined.Block, null, tint = Error) }
                )
            }
        }
    }
}

@Composable
private fun BlockedUsersTab(
    blockedUsers: List<User>,
    isLoading: Boolean,
    onUnblock: (User) -> Unit
) {
    if (isLoading && blockedUsers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    if (blockedUsers.isEmpty()) {
        EmptyStateContent(
            icon = Icons.Outlined.Block,
            title = "No blocked users",
            subtitle = "Users you block will appear here."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(blockedUsers, key = { it.userId }) { user ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(user = user, size = 48)

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ShiftButton(
                    text = "Unblock",
                    onClick = { onUnblock(user) },
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.height(36.dp)
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(start = 64.dp))
        }
    }
}

@Composable
private fun UserAvatar(user: User, size: Int) {
    if (user.profileImageUrl.isNotEmpty()) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.displayName.take(1).uppercase(),
                style = if (size > 40) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = AccentBlue
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) Error else AccentBlue
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
