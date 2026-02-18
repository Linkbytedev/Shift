package com.Linkbyte.Shift.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStorage: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isScreenSecurityEnabled by viewModel.isScreenSecurityEnabled.collectAsState(initial = true)
    val isBiometricVaultEnabled by viewModel.isBiometricVaultEnabled.collectAsState(initial = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Privacy & Security", fontWeight = FontWeight.Bold) },
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
                SettingsSectionHeader("Protection")

                SettingsToggleItem(
                    icon = Icons.Outlined.Screenshot,
                    title = "Screen Security",
                    subtitle = "Block screenshots and hide app content in recents",
                    checked = isScreenSecurityEnabled,
                    onCheckedChange = { viewModel.setScreenSecurityEnabled(it) }
                )

                SettingsToggleItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Biometric Vault",
                    subtitle = "Use fingerprint or face ID to unlock Secure Vault",
                    checked = isBiometricVaultEnabled,
                    onCheckedChange = { viewModel.setBiometricVaultEnabled(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                SettingsSectionHeader("Data Management")

                SettingsItem(
                    icon = Icons.Outlined.Storage,
                    title = "Storage Manager",
                    subtitle = "View and manage app storage usage",
                    onClick = onNavigateToStorage
                )

                SettingsItem(
                    icon = Icons.Outlined.History,
                    title = "Auto-Delete Messages",
                    subtitle = "Currently set per-chat via timer",
                    onClick = { /* Could add global setting here later */ }
                )

                Spacer(modifier = Modifier.height(24.dp))
                
                // Privacy Info Card
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VerifiedUser, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Your Privacy Matters", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Shift uses end-to-end encryption. Only you and your recipients can read your messages. These settings help protect your physical device privacy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
