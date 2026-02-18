package com.Linkbyte.Shift.data.model

data class CallModel(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val callType: CallType = CallType.VIDEO,
    val status: CallStatus = CallStatus.IDLE,
    val sdpOffer: SdpModel? = null,
    val sdpAnswer: SdpModel? = null,
    val iceCandidates: List<IceCandidateModel> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class SdpModel(
    val type: String = "",
    val description: String = ""
)

data class IceCandidateModel(
    val sdpMid: String? = null,
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val senderId: String = ""
)

enum class CallType {
    AUDIO,
    VIDEO,
    P2P_DATA
}

enum class CallStatus {
    IDLE,
    INITIATED,
    RINGING,
    ACCEPTED,
    REJECTED,
    ENDED,
    ERROR
}
