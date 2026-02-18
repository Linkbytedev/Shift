package com.Linkbyte.Shift.presentation.call

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Linkbyte.Shift.domain.repository.AuthRepository
import com.Linkbyte.Shift.data.model.*
import com.Linkbyte.Shift.webrtc.AudioHandler
import com.Linkbyte.Shift.webrtc.SignalingClient
import com.Linkbyte.Shift.webrtc.WebRtcClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.webrtc.*
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val webRtcClient: WebRtcClient,
    private val signalingClient: SignalingClient,
    private val audioHandler: AudioHandler,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _callState = MutableStateFlow<CallModel?>(null)
    val callState: StateFlow<CallModel?> = _callState.asStateFlow()
    
    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()
    
    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()
    
    private var timeoutJob: Job? = null
    private var peerConnection: PeerConnection? = null
    private val processedIceCandidates = mutableSetOf<String>()
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    private var currentUserId: String = ""

    init {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                currentUserId = user?.userId ?: ""
            }
        }
    }

    fun startCall(receiverId: String, callType: CallType) {
        val callerId = currentUserId
        if (callerId.isEmpty()) return
        
        val callId = UUID.randomUUID().toString()
        val initialCall = CallModel(
            callId = callId,
            callerId = callerId,
            receiverId = receiverId,
            callType = callType,
            status = CallStatus.INITIATED
        )
        
        viewModelScope.launch {
            signalingClient.initiateCall(initialCall).onSuccess {
                _callState.value = initialCall
                setupWebRtc(isCaller = true, callType = callType)
                observeCallChanges(callId)
                startTimeoutTimer()
            }
        }
    }

    fun joinCall(callId: String) {
        viewModelScope.launch {
            signalingClient.observeCall(callId).firstOrNull()?.let { call ->
                _callState.value = call
                setupWebRtc(isCaller = false, callType = call.callType)
                observeCallChanges(callId)
                signalingClient.updateCall(callId, mapOf("status" to CallStatus.RINGING.name))
            }
        }
    }

    fun acceptCall() {
        val currentCall = _callState.value ?: return
        val pc = peerConnection ?: return
        
        viewModelScope.launch {
            signalingClient.updateCall(currentCall.callId, mapOf("status" to CallStatus.ACCEPTED.name))
            
            // Create answer
            webRtcClient.createAnswer(pc, object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let { 
                        webRtcClient.setLocalDescription(pc, it, object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                viewModelScope.launch {
                                    signalingClient.updateCall(currentCall.callId, mapOf("sdpAnswer" to SdpModel(it.type.canonicalForm(), it.description)))
                                }
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        })
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            })
        }
    }

    private fun startTimeoutTimer() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(60000) // 60 seconds timeout
            val currentState = _callState.value
            if (currentState != null && 
                (currentState.status == CallStatus.INITIATED || currentState.status == CallStatus.RINGING)) {
                appendLog("Call timed out after 60s")
                endCall("No answer")
            }
        }
    }

    fun endCall(reason: String = "") {
        timeoutJob?.cancel()
        val currentCall = _callState.value ?: return
        
        viewModelScope.launch {
            signalingClient.updateCall(currentCall.callId, mapOf("status" to CallStatus.ENDED.name))
            _callState.value = currentCall.copy(status = CallStatus.ENDED)
            
            peerConnection?.close()
            peerConnection = null
            audioHandler.stopAudio()
        }
    }

    private fun setupMedia(callType: CallType) {
        val pc = peerConnection ?: return
        
        webRtcClient.createAudioTrack(pc)
        if (callType == CallType.VIDEO) {
            val enumerator = Camera2Enumerator(webRtcClient.getContext())
            val deviceName = enumerator.deviceNames.find { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull()
            
            deviceName?.let { name ->
                val videoCapturer = enumerator.createCapturer(name, null)
                val videoTrack = webRtcClient.createVideoTrack(pc, videoCapturer)
                try {
                    videoCapturer.startCapture(1280, 720, 30)
                    _localVideoTrack.value = videoTrack
                } catch (e: Exception) {
                    appendLog("Camera Error: ${e.message}")
                }
            }
        }
    }

    private val _connectionStatus = MutableStateFlow("Connecting...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()
    
    private val _debugLog = MutableStateFlow("Initializing...")
    val debugLog: StateFlow<String> = _debugLog.asStateFlow()
    
    private fun appendLog(message: String) {
        Log.d("CallViewModel", message)
        _debugLog.value = "${_debugLog.value}\n$message"
    }
    
    private fun setupWebRtc(isCaller: Boolean, callType: CallType) {
        // Audio Setup
        audioHandler.startAudio()
        // Default to Speakerphone ON for better audibility while debugging
        audioHandler.setSpeakerphone(true)
        
        // 1. Initialize PeerConnection FIRST
        peerConnection = webRtcClient.createPeerConnection(object : PeerConnection.Observer {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                _connectionStatus.value = state?.name ?: "Unknown"
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                    endCall("Connection lost")
                }
            }
            override fun onIceConnectionReceivingChange(p1: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    // Parse candidate type from SDP string
                    val sdp = it.sdp
                    val type = when {
                        sdp.contains("typ relay") -> "RELAY (TURN)"
                        sdp.contains("typ srflx") -> "SRFLX (STUN)"
                        sdp.contains("typ host") -> "HOST (LAN)"
                        else -> "UNKNOWN"
                    }
                    appendLog("Local: $type")
                    
                    viewModelScope.launch {
                        _callState.value?.callId?.let { callId ->
                            signalingClient.addIceCandidate(callId, IceCandidateModel(
                                sdpMid = it.sdpMid,
                                sdpMLineIndex = it.sdpMLineIndex,
                                sdp = it.sdp,
                                senderId = currentUserId
                            ))
                        }
                    }
                }
            }
            // Add error logging
            override fun onIceCandidateError(event: IceCandidateErrorEvent) {
                appendLog("ICE Error: ${event.errorCode} - ${event.errorText}")
            }
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {
                // Video tracks ignored for now, handled by tracks
            }
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })
        
        val pc = peerConnection ?: return
        
        // 2. Setup Media (Create tracks and add to PeerConnection) - NOW CALLED AFTER INIT
        setupMedia(callType)

        if (isCaller) {
            webRtcClient.createOffer(pc, object : SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription?) {
                    desc?.let {
                        webRtcClient.setLocalDescription(pc, it, object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                viewModelScope.launch {
                                    _callState.value?.callId?.let { id ->
                                        signalingClient.updateCall(id, mapOf("sdpOffer" to SdpModel(it.type.canonicalForm(), it.description)))
                                    }
                                }
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        })
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            })
        }
    }

    private fun observeCallChanges(callId: String) {
        viewModelScope.launch {
            signalingClient.observeCall(callId).collect { call ->
                // P2P Data calls ignored here
                _callState.value = call
                
                if (call?.status == CallStatus.ACCEPTED) {
                    timeoutJob?.cancel()
                } else if (call?.status == CallStatus.REJECTED || call?.status == CallStatus.ENDED) {
                    timeoutJob?.cancel()
                    peerConnection?.close()
                    peerConnection = null
                    audioHandler.stopAudio()
                }

                val isCaller = call?.callerId == currentUserId
                val pc = peerConnection ?: return@collect

                // Handling SDP Offer (Receiver)
                if (!isCaller) {
                    call?.sdpOffer?.let { offer ->
                        if (pc.remoteDescription == null) {
                            webRtcClient.setRemoteDescription(pc, offer, object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    // Process pending candidates if any
                                    drainPendingIceCandidates(pc)
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            })
                        }
                    }
                }

                // Handling SDP Answer (Caller)
                if (isCaller) {
                    call?.sdpAnswer?.let { answer ->
                        if (pc.remoteDescription == null) {
                            webRtcClient.setRemoteDescription(pc, answer, object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onSetSuccess() {
                                    // Process pending candidates if any
                                    drainPendingIceCandidates(pc)
                                }
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            })
                        }
                    }
                }
                
                call?.iceCandidates?.forEach { candidate ->
                    // Only process candidates from the OTHER user
                    if (candidate.senderId != currentUserId) {
                        // Deduplicate based on unique properties
                        val candidateKey = "${candidate.sdpMid}:${candidate.sdpMLineIndex}:${candidate.sdp}"
                        if (!processedIceCandidates.contains(candidateKey)) {
                            if (pc.remoteDescription != null) {
                                webRtcClient.addIceCandidate(pc, candidate)
                            } else {
                                // Buffer until remote desc is set
                                pendingIceCandidates.add(IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp))
                            }
                            processedIceCandidates.add(candidateKey)
                            
                            val sdp = candidate.sdp
                            val type = when {
                                sdp.contains("typ relay") -> "RELAY (TURN)"
                                sdp.contains("typ srflx") -> "SRFLX (STUN)"
                                sdp.contains("typ host") -> "HOST (LAN)"
                                else -> "UNKNOWN"
                            }
                            appendLog("Remote: $type")
                        }
                    }
                }
            }
        }
    }
    
    private fun drainPendingIceCandidates(pc: PeerConnection) {
        pendingIceCandidates.forEach { pc.addIceCandidate(it) }
        pendingIceCandidates.clear()
    }
    
    val isSpeakerphoneOn = audioHandler.isSpeakerphoneOn

    fun toggleSpeakerphone() {
        audioHandler.toggleSpeakerphone()
    }
    
    fun getEglBaseContext() = webRtcClient.getEglBaseContext()

    override fun onCleared() {
        super.onCleared()
        peerConnection?.close()
        peerConnection = null
        audioHandler.stopAudio()
    }
}
