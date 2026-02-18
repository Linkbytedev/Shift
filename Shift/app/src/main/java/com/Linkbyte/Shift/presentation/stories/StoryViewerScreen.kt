package com.Linkbyte.Shift.presentation.stories

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.data.model.Story
import com.Linkbyte.Shift.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@Composable
fun StoryViewerScreen(
    userId: String,
    onDismiss: () -> Unit,
    viewModel: StoryViewModel = hiltViewModel()
) {
    val allStories by viewModel.stories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val stories = remember(allStories) { allStories.filter { it.userId == userId } }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    var isDismissing by remember { mutableStateOf(false) }

    // Handle empty state (after deletion)
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) {
            if (!isDismissing) {
               onDismiss()
            }
        }
        // Return clear box to avoid white flash
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
        return
    }

    // Ensure currentIndex is valid
    if (currentIndex >= stories.size) {
        currentIndex = stories.size - 1
    }
    
    val story = stories[currentIndex]

    val bgBrush = getStoryGradient(story.backgroundColor)

    // Time remaining
    val timeRemaining = remember(story) {
        val remaining = story.expiresAt - System.currentTimeMillis()
        val hours = remaining / 3_600_000
        val minutes = (remaining % 3_600_000) / 60_000
        if (hours > 0) "${hours}h ${minutes}m left" else "${minutes}m left"
    }

    // Time ago
    val timeAgo = remember(story) {
        val elapsed = System.currentTimeMillis() - story.createdAt
        val hours = elapsed / 3_600_000
        val minutes = (elapsed % 3_600_000) / 60_000
        when {
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = bgBrush)
            .clickable {
                if (!isDismissing) {
                    if (currentIndex < stories.size - 1) {
                        currentIndex++
                    } else {
                        isDismissing = true
                        onDismiss()
                    }
                }
            }
    ) {
        // Progress bars at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            stories.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= currentIndex) Color.White
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Header - author info and close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 20.dp, start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Author avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = story.displayName.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = story.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = timeAgo,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            // Delete button (own stories only)
            if (story.userId == currentUserId) {
                IconButton(onClick = {
                    val isLast = stories.size <= 1
                    viewModel.deleteStory(story.storyId)
                    if (isLast) {
                        isDismissing = true
                        onDismiss() 
                    } else if (currentIndex > 0) {
                         currentIndex--
                    }
                }) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White)
                }
            }

            IconButton(onClick = { 
                if (!isDismissing) {
                    isDismissing = true
                    onDismiss() 
                }
            }) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        // Story text content - centered
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = story.text,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
        }

        // Bottom - time remaining
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = timeRemaining,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

fun getStoryGradient(name: String): Brush {
    return when (name) {
        "gradient_cyan" -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF00BCD4))
        )
        "gradient_purple" -> Brush.linearGradient(
            colors = listOf(Color(0xFF7B1FA2), Color(0xFFE040FB))
        )
        "gradient_sunset" -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF6F00), Color(0xFFFF1744))
        )
        "gradient_forest" -> Brush.linearGradient(
            colors = listOf(Color(0xFF1B5E20), Color(0xFF00E676))
        )
        "gradient_midnight" -> Brush.linearGradient(
            colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
        )
        "gradient_pink" -> Brush.linearGradient(
            colors = listOf(Color(0xFFAD1457), Color(0xFFF48FB1))
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D47A1), Color(0xFF00BCD4))
        )
    }
}
