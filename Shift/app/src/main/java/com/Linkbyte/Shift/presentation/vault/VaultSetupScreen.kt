package com.Linkbyte.Shift.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSetupScreen(
    onPasswordSet: (String, String) -> Unit,
    isLoading: Boolean
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    
    val pinsMatch = pin == confirmPin && pin.isNotEmpty()
    val isValidLength = pin.length in 5..10
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "Vault Setup", 
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Secure Your Photos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create a 5-10 digit PIN to encrypt your private photos. This PIN cannot be recovered if lost.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ShiftTextField(
                value = pin,
                onValueChange = { if (it.length <= 10) pin = it },
                label = "Create PIN",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                isError = pin.isNotEmpty() && !isValidLength,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            
            if (pin.isNotEmpty() && !isValidLength) {
                Text(
                    text = "PIN must be 5-10 digits",
                    color = Error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ShiftTextField(
                value = confirmPin,
                onValueChange = { if (it.length <= 10) confirmPin = it },
                label = "Confirm PIN",
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                isError = confirmPin.isNotEmpty() && !pinsMatch,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            
            if (confirmPin.isNotEmpty() && !pinsMatch) {
                Text(
                    text = "PINs do not match",
                    color = Error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ShiftButton(
                text = "Create Vault",
                onClick = { onPasswordSet(pin, confirmPin) },
                enabled = pinsMatch && isValidLength && !isLoading,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    }
}
