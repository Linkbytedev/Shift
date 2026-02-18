package com.Linkbyte.Shift.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object ChatList : Screen("chat_list")
    object Conversation : Screen("conversation/{conversationId}") {
        fun createRoute(conversationId: String) = "conversation/$conversationId"
    }
    // Temporarily disabled for troubleshooting
    object Friends : Screen("friends")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object PrivacySecurity : Screen("privacy_security")
    object StorageManager : Screen("storage_manager")
    object ArchivedChats : Screen("archived_chats")
    object Vault : Screen("vault")
    object FriendProfile : Screen("friend_profile/{userId}?conversationId={conversationId}") {
        fun createRoute(userId: String, conversationId: String? = null) = 
            if (conversationId != null) "friend_profile/$userId?conversationId=$conversationId" else "friend_profile/$userId"
    }
    object StoryViewer : Screen("story_viewer/{userId}") {
        fun createRoute(userId: String) = "story_viewer/$userId"
    }
    object Call : Screen("call/{id}/{callType}/{isIncoming}") {
        fun createRoute(id: String, callType: String, isIncoming: Boolean) = "call/$id/$callType/$isIncoming"
    }
    object TermsOfService : Screen("terms_of_service")
    object PrivacyPolicy : Screen("privacy_policy")
}
