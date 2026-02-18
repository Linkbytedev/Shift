package com.Linkbyte.Shift.data.model

data class FriendRequest(
    val requestId: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromDisplayName: String = "",
    val fromProfileImage: String = "",
    val toUserId: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
