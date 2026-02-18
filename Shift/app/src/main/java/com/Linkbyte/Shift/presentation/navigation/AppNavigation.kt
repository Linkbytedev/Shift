package com.Linkbyte.Shift.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Linkbyte.Shift.presentation.auth.LoginScreen
import com.Linkbyte.Shift.presentation.auth.SignUpScreen
import com.Linkbyte.Shift.presentation.chat.ChatListScreen
import com.Linkbyte.Shift.presentation.chat.ConversationScreen
import com.Linkbyte.Shift.presentation.profile.UserProfileScreen
import com.Linkbyte.Shift.presentation.friends.FriendsScreen
import com.Linkbyte.Shift.presentation.profile.ProfileScreen
import com.Linkbyte.Shift.presentation.settings.SettingsScreen
import com.Linkbyte.Shift.presentation.settings.PrivacySecurityScreen
import com.Linkbyte.Shift.presentation.settings.StorageManagerScreen
import com.Linkbyte.Shift.presentation.stories.StoryViewerScreen
import com.Linkbyte.Shift.presentation.call.CallScreen
import com.Linkbyte.Shift.presentation.call.CallViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.Linkbyte.Shift.webrtc.SignalingClient
import com.Linkbyte.Shift.domain.repository.AuthRepository
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    authRepository: AuthRepository,
    signalingClient: SignalingClient
) {
    // Listen for incoming calls
    // Listen for incoming calls
    androidx.compose.runtime.LaunchedEffect(Unit) {
        authRepository.getCurrentUser().collectLatest { user ->
            user?.userId?.let { userId ->
                try {
                    signalingClient.observeIncomingCalls(userId).collect { call ->
                        // Navigate to call screen if not already there
                        val route = Screen.Call.createRoute(call.callId, call.callType.name, true)
                        // Simple check to avoid double navigation if already on the call screen
                         navController.navigate(route)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToTerms = {
                    navController.navigate(Screen.TermsOfService.route)
                },
                onNavigateToPrivacy = {
                    navController.navigate(Screen.PrivacyPolicy.route)
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.ChatList.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToConversation = { conversationId ->
                    navController.navigate(Screen.Conversation.createRoute(conversationId))
                },
                onNavigateToFriends = {
                    navController.navigate(Screen.Friends.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToStoryViewer = { userId ->
                    navController.navigate(Screen.StoryViewer.createRoute(userId))
                }
            )
        }
        
        composable(
            route = Screen.Conversation.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            ConversationScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCall = { receiverId, callType, isIncoming ->
                    navController.navigate(Screen.Call.createRoute(receiverId, callType, isIncoming))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.FriendProfile.createRoute(userId, conversationId))
                }
            )
        }
        
        composable(Screen.Friends.route) {
            FriendsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConversation = { conversationId ->
                    navController.navigate(Screen.Conversation.createRoute(conversationId))
                },
                onNavigateToFriendProfile = { userId ->
                    navController.navigate(Screen.FriendProfile.createRoute(userId))
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVault = {
                    navController.navigate(Screen.Vault.route)
                },
                onNavigateToArchivedChats = {
                    navController.navigate(Screen.ArchivedChats.route)
                },
                onNavigateToPrivacySecurity = {
                    navController.navigate(Screen.PrivacySecurity.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Login.route) {
                        // Clear the backstack including the ChatList
                        popUpTo(Screen.ChatList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.PrivacySecurity.route) {
            PrivacySecurityScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStorage = {
                    navController.navigate(Screen.StorageManager.route)
                }
            )
        }

        composable(Screen.StorageManager.route) {
            StorageManagerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ArchivedChats.route) {
            com.Linkbyte.Shift.presentation.archive.ArchivedChatsScreen(
                 onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Vault.route) {
            com.Linkbyte.Shift.presentation.vault.VaultMainScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Friend Profile
        composable(
            route = Screen.FriendProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("conversationId") { 
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val conversationId = backStackEntry.arguments?.getString("conversationId")

            UserProfileScreen(
                userId = userId,
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { convId ->
                    navController.navigate(Screen.Conversation.createRoute(convId)) {
                        // Pop back to ChatList to avoid deep back stack
                        popUpTo(Screen.ChatList.route)
                    }
                }
            )
        }

        // Story Viewer
        composable(
            route = Screen.StoryViewer.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            StoryViewerScreen(
                userId = userId,
                onDismiss = { navController.popBackStack() }
            )
        }

        // Call Screen
        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("isIncoming") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val callTypeStr = backStackEntry.arguments?.getString("callType") ?: "VIDEO"
            val isIncoming = backStackEntry.arguments?.getBoolean("isIncoming") ?: false
            val callType = com.Linkbyte.Shift.data.model.CallType.valueOf(callTypeStr)
            
            val callViewModel: CallViewModel = hiltViewModel()
            
            CallScreen(
                id = id,
                callType = callType,
                isIncoming = isIncoming,
                viewModel = callViewModel,
                onCallEnded = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TermsOfService.route) {
            com.Linkbyte.Shift.presentation.common.LegalDocScreen(
                title = "Terms of Service",
                content = """
                    Terms of Service for Shift

                    1. Acceptance of Terms
                    By creating an account on Shift, you agree to these Terms.

                    2. User Conduct
                    You agree not to use Shift for any illegal or unauthorized purpose.
                    
                    3. Content
                    You are responsible for the content you share.
                    
                    4. Termination
                    We reserve the right to terminate accounts that violate these terms.
                    
                    (This is a placeholder. Please replace with actual terms.)
                """.trimIndent(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivacyPolicy.route) {
            com.Linkbyte.Shift.presentation.common.LegalDocScreen(
                title = "Privacy Policy",
                content = """
                    Privacy Policy for Shift

                    1. Data Collection
                    We collect your email, username, and profile information to provide the service.
                    
                    2. Data Usage
                    We use your data to facilitate messaging and improve the app.
                    
                    3. Data Security
                    We use encryption to protect your messages.
                    
                    (This is a placeholder. Please replace with actual policy.)
                """.trimIndent(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
