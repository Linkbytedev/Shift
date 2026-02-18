package com.Linkbyte.Shift.presentation.vault

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.data.model.VaultImage
import com.Linkbyte.Shift.ui.components.*
import com.Linkbyte.Shift.ui.theme.*
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    onBack: () -> Unit,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val images by viewModel.images.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    var selectedImageId by remember { mutableStateOf<String?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                viewModel.addImage(
                    imageData = bytes,
                    fileName = "image_${System.currentTimeMillis()}.jpg",
                    originalUri = it,
                    contentResolver = context.contentResolver
                )
            }
        }
    }
    
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
    
    // Full-screen image viewer
    selectedImageId?.let { imageId ->
        val selectedImage = images.find { it.id == imageId }
        if (selectedImage != null) {
            FullScreenImageViewer(
                image = selectedImage,
                viewModel = viewModel,
                onDismiss = { selectedImageId = null }
            )
        }
    }
    
    when {
        !isPasswordSet -> {
            VaultSetupScreen(
                onPasswordSet = { password, confirm ->
                    viewModel.setPassword(password, confirm)
                },
                isLoading = isLoading
            )
        }
        !isUnlocked -> {
            VaultUnlockScreen(
                onUnlock = { password ->
                    viewModel.unlock(password)
                },
                onReset = { viewModel.resetVault() },
                onBack = onBack,
                isLoading = isLoading,
                viewModel = viewModel
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = { Text("Vault", fontWeight = FontWeight.SemiBold) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    viewModel.lock()
                                    onBack()
                                }) {
                                    Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.lock() }) {
                                    Icon(Icons.Outlined.Lock, "Lock Vault", tint = MaterialTheme.colorScheme.onBackground)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            containerColor = Color.Transparent, // Transparent to show gradient
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(BrandGradientStart, BrandGradientEnd)
                                    ),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(Icons.Default.Add, "Add Image")
                        }
                    },
                    containerColor = Color.Transparent
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when {
                            isLoading && images.isEmpty() -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = NeonCyan
                                )
                            }
                            images.isEmpty() -> {
                                VaultEmptyState()
                            }
                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(images, key = { it.id }) { image ->
                                        VaultImageItem(
                                            image = image,
                                            onImageClick = { selectedImageId = image.id },
                                            onDeleteClick = { viewModel.deleteImage(image.id) },
                                            viewModel = viewModel
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
}

@Composable
private fun VaultEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your vault is empty",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add photos to secure them with encryption",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VaultImageItem(
    image: VaultImage,
    onImageClick: () -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: VaultViewModel
) {
    var thumbnailData by remember { mutableStateOf<ByteArray?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(image.id) {
        viewModel.getThumbnailData(image.id) { data ->
            thumbnailData = data
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Image?") },
            text = { Text("This image will be permanently deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(onClick = onImageClick)
    ) {
        thumbnailData?.let { data ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = NeonCyan,
                strokeWidth = 2.dp
            )
        }
        
        // Export/Move Out button
        IconButton(
            onClick = { viewModel.exportImage(image.id) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SaveAlt,
                    contentDescription = "Export",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Delete button
        IconButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
