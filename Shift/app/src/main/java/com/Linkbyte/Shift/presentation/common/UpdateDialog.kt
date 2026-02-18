package com.Linkbyte.Shift.presentation.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.Linkbyte.Shift.data.model.AppUpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Permission request launcher
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
         // Check if permission granted then proceed? 
         // For now, simpler flow: user grants -> they click update again or we just trigger download
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Update Available") },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Version ${updateInfo.versionName} is available.\n\n" +
                            (updateInfo.releaseNotes ?: "New features and performance improvements.")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                             val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                             intent.data = Uri.parse("package:${context.packageName}")
                             launcher.launch(intent)
                             return@Button
                        }
                    }
                    UpdateDownloader.downloadApk(context, updateInfo.downloadUrl)
                    onDismiss()
                }
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
