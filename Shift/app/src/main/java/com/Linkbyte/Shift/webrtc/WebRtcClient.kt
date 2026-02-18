package com.Linkbyte.Shift.webrtc

import android.content.Context
import android.util.Log
import com.Linkbyte.Shift.data.model.IceCandidateModel
import com.Linkbyte.Shift.data.model.SdpModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRtcClient @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootEglBase: EglBase = EglBase.create()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    
    private val iceServers = listOf(
        // Google STUN (Primary & Reliable)
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
        
        // Custom ExpressTurn STUN
        PeerConnection.IceServer.builder("stun:free.expressturn.com:3478").createIceServer(),
        
        // Custom ExpressTurn TURN (Authenticated)
        PeerConnection.IceServer.builder("turn:free.expressturn.com:3478")
            .setUsername("000000002086365785")
            .setPassword("wXoAJwgCl7Su3c3pm/3PRjqlXBI=")
            .createIceServer()
    )

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val factoryOptions = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(factoryOptions)
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            
            // Allow TCP candidates as fallback
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        }
        return peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
    }

    fun createDataChannel(peerConnection: PeerConnection, label: String, observer: DataChannel.Observer): DataChannel? {
        val init = DataChannel.Init()
        init.ordered = true
        val dc = peerConnection.createDataChannel(label, init)
        dc?.registerObserver(observer)
        return dc
    }

    fun createAudioTrack(peerConnection: PeerConnection): AudioTrack? {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        
        val audioSource = peerConnectionFactory?.createAudioSource(constraints)
        val localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection.addTrack(localAudioTrack)
        return localAudioTrack
    }

    fun createVideoTrack(peerConnection: PeerConnection, videoCapturer: VideoCapturer): VideoTrack? {
        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.eglBaseContext)
        val videoSource = peerConnectionFactory?.createVideoSource(videoCapturer.isScreencast)
        videoCapturer.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
        
        val localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", videoSource)
        peerConnection.addTrack(localVideoTrack)
        return localVideoTrack
    }

    fun createOffer(peerConnection: PeerConnection, observer: SdpObserver) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    val newSdp = setStartBitrate(it.description, 32)
                    val newDesc = SessionDescription(it.type, newSdp)
                    observer.onCreateSuccess(newDesc)
                } ?: observer.onCreateSuccess(null)
            }
            override fun onSetSuccess() { observer.onSetSuccess() }
            override fun onCreateFailure(p0: String?) { observer.onCreateFailure(p0) }
            override fun onSetFailure(p0: String?) { observer.onSetFailure(p0) }
        }, constraints)
    }

    fun createAnswer(peerConnection: PeerConnection, observer: SdpObserver) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        peerConnection.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let {
                    val newSdp = setStartBitrate(it.description, 32)
                    val newDesc = SessionDescription(it.type, newSdp)
                    observer.onCreateSuccess(newDesc)
                } ?: observer.onCreateSuccess(null)
            }
            override fun onSetSuccess() { observer.onSetSuccess() }
            override fun onCreateFailure(p0: String?) { observer.onCreateFailure(p0) }
            override fun onSetFailure(p0: String?) { observer.onSetFailure(p0) }
        }, constraints)
    }

    // Helper for bandwidth limiting
    private fun setStartBitrate(sdp: String, bitrateKbps: Int): String {
        val lineSeparator = "\r\n"
        var newSdp = sdp
        if (newSdp.contains("a=mid:audio")) {
            newSdp = newSdp.replace("a=mid:audio", "a=mid:audio${lineSeparator}b=AS:$bitrateKbps")
        } else if (newSdp.contains("a=mid:0")) {
            newSdp = newSdp.replace("a=mid:0", "a=mid:0${lineSeparator}b=AS:$bitrateKbps")
        }
        return newSdp
    }

    fun setRemoteDescription(peerConnection: PeerConnection, sdp: SdpModel, observer: SdpObserver) {
        val type = when (sdp.type.lowercase()) {
            "offer" -> SessionDescription.Type.OFFER
            "answer" -> SessionDescription.Type.ANSWER
            else -> throw IllegalArgumentException("Invalid SDP type: ${sdp.type}")
        }
        val sessionDescription = SessionDescription(type, sdp.description)
        peerConnection.setRemoteDescription(observer, sessionDescription)
    }

    fun setLocalDescription(peerConnection: PeerConnection, sdp: SessionDescription, observer: SdpObserver) {
        peerConnection.setLocalDescription(observer, sdp)
    }

    fun addIceCandidate(peerConnection: PeerConnection, candidate: IceCandidateModel) {
        val iceCandidate = IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)
        peerConnection.addIceCandidate(iceCandidate)
    }
    
    fun getEglBaseContext(): EglBase.Context = rootEglBase.eglBaseContext
    
    fun getContext(): Context = context
}
