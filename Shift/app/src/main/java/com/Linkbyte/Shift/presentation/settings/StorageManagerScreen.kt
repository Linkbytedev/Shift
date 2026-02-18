package com.Linkbyte.Shift.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var cacheSize by remember { mutableStateOf("0 B") }
    var totalSize by remember { mutableStateOf("0 B") }
    
    fun updateSizes() {
        val cache = getDirectorySize(context.cacheDir)
        val files = getDirectorySize(context.filesDir)
        cacheSize = formatSize(cache)
        totalSize = formatSize(cache + files)
    }

    LaunchedEffect(Unit) {
        updateSizes()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
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
                    .padding(16.dp)
            ) {
                // Storage Overview Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Storage, null, tint = AccentBlue, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Total Used", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(totalSize, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                SettingsSectionHeader("Cleanup")

                StorageItem(
                    title = "Cache",
                    subtitle = "Temporary files used for performance",
                    size = cacheSize,
                    onClear = {
                        context.cacheDir.deleteRecursively()
                        updateSizes()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                StorageItem(
                    title = "Secure Vault Data",
                    subtitle = "Your encrypted photos (Manage in Vault)",
                    size = "Encrypted",
                    onClear = null // Don't allow clearing here to prevent accidents
                )

                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    "Note: Clearing cache will not delete your messages or accounts. It may briefly slow down image loading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun StorageItem(
    title: String,
    subtitle: String,
    size: String,
    onClear: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(size, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (onClear != null) {
                TextButton(
                    onClick = onClear,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(16.dp), tint = ErrorRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", color = ErrorRed, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun getDirectorySize(directory: File): Long {
    var size: Long = 0
    val files = directory.listFiles()
    if (files != null) {
        for (file in files) {
            size += if (file.isDirectory) {
                getDirectorySize(file)
            } else {
                file.length()
            }
        }
    }
    return size
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
