package com.Linkbyte.Shift.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.foundation.BorderStroke
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultUnlockScreen(
    onUnlock: (String) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean,
    viewModel: VaultViewModel
) {
    var pin by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Vault PIN?") },
            text = { 
                Text(
                    "This will permanently delete all photos in your vault. This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                ShiftButton(
                    text = "Reset & Delete All",
                    onClick = {
                        showResetDialog = false
                        onReset()
                    },
                    variant = ButtonVariant.Destructive
                )
            },
            dismissButton = {
                ShiftButton(
                    text = "Cancel",
                    onClick = { showResetDialog = false },
                    variant = ButtonVariant.Ghost
                )
            }
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
    val isBiometricEnabled by viewModel.isBiometricVaultEnabled.collectAsState(initial = false)
    val biometricPassword by viewModel.biometricPassword.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    // Auto-trigger biometric on start
    LaunchedEffect(isBiometricEnabled, biometricPassword) {
        if (isBiometricEnabled && biometricPassword != null && activity != null) {
            showBiometricPrompt(activity) {
                biometricPassword?.let { onUnlock(it) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Unlock Vault",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Enter your PIN to access your private photos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ShiftTextField(
                value = pin,
                onValueChange = { if (it.length <= 10) pin = it },
                label = "Enter PIN",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ShiftButton(
                text = "Unlock",
                onClick = { onUnlock(pin) },
                enabled = pin.isNotEmpty() && !isLoading,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                if (biometricPassword != null) {
                    OutlinedButton(
                        onClick = {
                            activity?.let {
                                showBiometricPrompt(it) {
                                    biometricPassword?.let { password -> onUnlock(password) }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Outlined.Fingerprint, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Biometric")
                    }
                } else {
                    Text(
                        text = "Enter PIN once to enable biometric unlock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { showResetDialog = true }) {
                Text(
                    "Forgot PIN?",
                    color = Error
                )
            }
        }
    }
    }
}


private fun showBiometricPrompt(
    activity: androidx.fragment.app.FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val biometricPrompt = androidx.biometric.BiometricPrompt(activity, executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                android.util.Log.d("VaultUnlock", "Biometric success")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                android.util.Log.e("VaultUnlock", "Biometric error: $errorCode - $errString")
                if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED && 
                    errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                     android.widget.Toast.makeText(activity, "Authentication error: $errString", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                android.util.Log.w("VaultUnlock", "Biometric failed")
                android.widget.Toast.makeText(activity, "Authentication failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        })

    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Vault")
        .setSubtitle("Use your biometric credential")
        .setNegativeButtonText("Cancel")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        android.util.Log.e("VaultUnlock", "Biometric exception", e)
        android.widget.Toast.makeText(activity, "Biometric error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
    }
}
