package com.Linkbyte.Shift.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: com.Linkbyte.Shift.domain.repository.UserRepository,
    private val themePreferences: com.Linkbyte.Shift.data.preferences.ThemePreferences,
    private val securityPreferences: com.Linkbyte.Shift.data.preferences.SecurityPreferences,
    private val updateRepository: com.Linkbyte.Shift.domain.repository.UpdateRepository
) : ViewModel() {

    var userEmail by mutableStateOf<String?>("user@example.com")

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                userEmail = user?.email
            }
        }
    }
        
    private val _deleteAccountState = mutableStateOf<Result<Unit>?>(null)
    val deleteAccountState: State<Result<Unit>?> = _deleteAccountState
    
    private val _isDeleting = mutableStateOf(false)
    val isDeleting: State<Boolean> = _isDeleting

    val themeMode = themePreferences.themeMode

    // Security Settings
    // Security Settings
    val isScreenSecurityEnabled = securityPreferences.isScreenSecurityEnabled
    val isBiometricVaultEnabled = securityPreferences.isBiometricVaultEnabled

    private val _reinstallState = mutableStateOf<Result<com.Linkbyte.Shift.data.model.AppUpdateInfo>?>(null)
    val reinstallState: State<Result<com.Linkbyte.Shift.data.model.AppUpdateInfo>?> = _reinstallState

    private val _isFetchingReinstall = mutableStateOf(false)
    val isFetchingReinstall: State<Boolean> = _isFetchingReinstall

    fun setScreenSecurityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setScreenSecurityEnabled(enabled)
        }
    }

    fun setBiometricVaultEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setBiometricVaultEnabled(enabled)
        }
    }

    fun getReinstallInfo() {
        viewModelScope.launch {
            _isFetchingReinstall.value = true
            val result = updateRepository.getLatestVersionInfo()
            _reinstallState.value = result
            _isFetchingReinstall.value = false
        }
    }

    fun resetReinstallState() {
        _reinstallState.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            _isDeleting.value = true
            val result = userRepository.deleteAccount()
            _deleteAccountState.value = result
            _isDeleting.value = false
            
            if (result.isSuccess) {
                signOut()
            }
        }
    }
    
    fun resetDeleteState() {
        _deleteAccountState.value = null
    }

    fun setThemeMode(mode: com.Linkbyte.Shift.data.preferences.ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToVault: () -> Unit,
    onNavigateToArchivedChats: () -> Unit,
    onNavigateToPrivacySecurity: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showReinstallDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                ShiftButton(
                    text = "Sign Out",
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                        onSignOut()
                    },
                    variant = ButtonVariant.Destructive
                )
            },
            dismissButton = {
                ShiftButton(
                    text = "Cancel",
                    onClick = { showSignOutDialog = false },
                    variant = ButtonVariant.Ghost
                )
            }
        )
    }

    var showAppearanceDialog by remember { mutableStateOf(false) }

    if (showAppearanceDialog) {
        val currentTheme = viewModel.themeMode.collectAsState(initial = com.Linkbyte.Shift.data.preferences.ThemeMode.SYSTEM).value
        AlertDialog(
            onDismissRequest = { showAppearanceDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Appearance") },
            text = {
                Column {
                    com.Linkbyte.Shift.data.preferences.ThemeMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setThemeMode(mode) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (mode == currentTheme),
                                onClick = { viewModel.setThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentBlue,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = when (mode) {
                                    com.Linkbyte.Shift.data.preferences.ThemeMode.SYSTEM -> "System Default"
                                    com.Linkbyte.Shift.data.preferences.ThemeMode.LIGHT -> "Light"
                                    com.Linkbyte.Shift.data.preferences.ThemeMode.DARK -> "Dark"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppearanceDialog = false }) {
                    Text("Close", color = AccentBlue)
                }
            }
        )
    }
    
    if (showReinstallDialog) {
        AlertDialog(
            onDismissRequest = { showReinstallDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Reinstall App") },
            text = { Text("This will fetch the latest version from the server and open the download page. Continue?") },
            confirmButton = {
                ShiftButton(
                    text = "Reinstall",
                    onClick = {
                        showReinstallDialog = false
                        viewModel.getReinstallInfo()
                    },
                    variant = ButtonVariant.Primary
                )
            },
            dismissButton = {
                ShiftButton(
                    text = "Cancel",
                    onClick = { showReinstallDialog = false },
                    variant = ButtonVariant.Ghost
                )
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {

                // Account Section
                SettingsSectionHeader("Account")

                SettingsItem(
                    icon = Icons.Outlined.Email,
                    title = "Email",
                    subtitle = viewModel.userEmail ?: "Not available",
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App Section
                SettingsSectionHeader("App")

                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notifications",
                    subtitle = "Manage notification preferences",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Outlined.Lock,
                    title = "Privacy & Security",
                    subtitle = "Encryption and security settings",
                    onClick = onNavigateToPrivacySecurity
                )

                SettingsItem(
                    icon = Icons.Outlined.Archive,
                    title = "Archived Chats",
                    subtitle = "View and manage archived conversations",
                    onClick = onNavigateToArchivedChats
                ) 
                
                SettingsItem(
                    icon = Icons.Outlined.PhotoLibrary,
                    title = "Secure Vault",
                    subtitle = "Password-protected encrypted photos",
                    onClick = onNavigateToVault
                )

                SettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance",
                    subtitle = "Theme and display settings",
                    onClick = { showAppearanceDialog = true }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // About Section
                SettingsSectionHeader("About")

                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "About Shift",
                    subtitle = "Version ${com.Linkbyte.Shift.BuildConfig.VERSION_NAME}",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Outlined.Description,
                    title = "Terms of Service",
                    subtitle = "Read our terms",
                    onClick = { }
                )

                SettingsItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Read our privacy policy",
                    onClick = { }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Reinstall Section
                SettingsSectionHeader("Support")
                
                val reinstallState by viewModel.reinstallState
                val isFetchingReinstall by viewModel.isFetchingReinstall
                val context = androidx.compose.ui.platform.LocalContext.current

                LaunchedEffect(reinstallState) {
                    reinstallState?.onSuccess { info ->
                        com.Linkbyte.Shift.presentation.common.UpdateDownloader.downloadApk(context, info.downloadUrl)
                        viewModel.resetReinstallState()
                    }?.onFailure { e ->
                        android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        viewModel.resetReinstallState()
                    }
                }

                SettingsItem(
                    icon = Icons.Outlined.Download,
                    title = "Reinstall App",
                    subtitle = if (isFetchingReinstall) "Fetching link..." else "Download latest version from server",
                    onClick = { 
                        if (!isFetchingReinstall) {
                            showReinstallDialog = true
                        }
                    }
                )

                SettingsItem(
                    icon = Icons.Outlined.HelpOutline,
                    title = "Contact Support",
                    subtitle = "Get help at linkbytedevelopmentsolutions@gmail.com",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:linkbytedevelopmentsolutions@gmail.com")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "Shift App Support Request")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No email app found", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Danger Zone
                SettingsSectionHeader("Danger Zone")
                
                var showDeleteDialog by remember { mutableStateOf(false) }
                val isDeleting by viewModel.isDeleting
                val deleteState by viewModel.deleteAccountState
                
                LaunchedEffect(deleteState) {
                    if (deleteState?.isSuccess == true) {
                        onSignOut()
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = ErrorRed,
                        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        title = { Text("Delete Account?") },
                        text = { 
                            Column {
                                Text("This action is permanent and cannot be undone.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("All your data (messages, friends, profile) will be permanently deleted.")
                                
                                if (deleteState?.isFailure == true) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Error: ${deleteState?.exceptionOrNull()?.message}",
                                        color = ErrorRed,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            ShiftButton(
                                text = if (isDeleting) "Deleting..." else "Delete Forever",
                                onClick = {
                                    viewModel.deleteAccount()
                                },
                                variant = ButtonVariant.Destructive
                            )
                        },
                        dismissButton = {
                            ShiftButton(
                                text = "Cancel",
                                onClick = { showDeleteDialog = false },
                                variant = ButtonVariant.Ghost
                            )
                        }
                    )
                }

                SettingsItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete Account",
                    subtitle = "Permanently remove your account and data",
                    onClick = { showDeleteDialog = true }
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                // Sign Out Button
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ShiftButton(
                        text = "Sign Out",
                        onClick = { showSignOutDialog = true },
                        variant = ButtonVariant.Ghost,
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // App branding
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Shift",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary
                    )
                    Text(
                        text = "End-to-End Encrypted Messaging",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version ${com.Linkbyte.Shift.BuildConfig.VERSION_NAME} (${com.Linkbyte.Shift.BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = AccentBlue,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AccentBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(start = 72.dp))
}
