package com.Linkbyte.Shift.presentation.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.Linkbyte.Shift.ui.theme.DarkBg
import com.Linkbyte.Shift.ui.theme.ErrorRed
import com.Linkbyte.Shift.ui.theme.White

@Composable
fun CallScreen(
    id: String,
    callType: com.Linkbyte.Shift.data.model.CallType,
    isIncoming: Boolean,
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val context = LocalContext.current
    val callState by viewModel.callState.collectAsState()
    val status = callState?.status ?: com.Linkbyte.Shift.data.model.CallStatus.IDLE

    // Only Audio Permission needed
    val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    var hasPermissions by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            hasPermissions = true
        } else {
            viewModel.endCall("Permission Denied")
            onCallEnded()
        }
    }

    LaunchedEffect(hasPermissions) {
        if (!hasPermissions) {
            launcher.launch(permissions)
        } else {
            if (isIncoming) {
                viewModel.joinCall(id)
            } else {
                viewModel.startCall(id, callType)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Audio Call UI (Avatar/Generic)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(160.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Voice Call",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${status.name}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Call Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .navigationBarsPadding()
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isMuted by remember { mutableStateOf(false) }
            val isSpeakerphoneOn by viewModel.isSpeakerphoneOn.collectAsState(initial = true) // Default true

            IconButton(
                onClick = { viewModel.toggleSpeakerphone() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isSpeakerphoneOn) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(
                    imageVector = if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle Speaker",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mute",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            FloatingActionButton(
                onClick = { 
                    viewModel.endCall()
                    onCallEnded() 
                },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        


        // Answer Button (For incoming calls in INITIATED/RINGING status)
        if (isIncoming && (status == com.Linkbyte.Shift.data.model.CallStatus.INITIATED || status == com.Linkbyte.Shift.data.model.CallStatus.RINGING)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Incoming Voice Call",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.acceptCall() },
                            containerColor = Color(0xFF4CAF50), // Standard Green
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(Icons.Default.Call, "Answer", modifier = Modifier.size(40.dp))
                        }
                        Spacer(modifier = Modifier.width(48.dp))
                        FloatingActionButton(
                            onClick = { 
                                viewModel.endCall("Rejected")
                                onCallEnded()
                            },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            shape = CircleShape,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Icon(Icons.Default.CallEnd, "Reject", modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        }
    }
}

