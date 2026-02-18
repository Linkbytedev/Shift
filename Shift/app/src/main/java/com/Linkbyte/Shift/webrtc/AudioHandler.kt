package com.Linkbyte.Shift.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var previousMicrophoneMute = false
    private var previousSpeakerphoneOn = false

    private var audioFocusRequest: android.media.AudioFocusRequest? = null

    private val _isSpeakerphoneOn = MutableStateFlow(false)
    val isSpeakerphoneOn: StateFlow<Boolean> = _isSpeakerphoneOn.asStateFlow()

    fun startAudio() {
        Log.d("AudioHandler", "Starting audio handling")
        previousAudioMode = audioManager.mode
        previousMicrophoneMute = audioManager.isMicrophoneMute
        previousSpeakerphoneOn = audioManager.isSpeakerphoneOn

        // Request Audio Focus
        requestAudioFocus()

        // Set audio mode to communication for VoIP
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        
        // Ensure mic is not muted initially
        audioManager.isMicrophoneMute = false
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            audioFocusRequest = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d("AudioHandler", "Audio focus changed: $focusChange")
                }
                .build()
            
            audioFocusRequest?.let {
                audioManager.requestAudioFocus(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }
    }

    fun setSpeakerphone(on: Boolean) {
        Log.d("AudioHandler", "Setting speakerphone: $on")
        // audioManager.mode MUST be set to MODE_IN_COMMUNICATION for setSpeakerphoneOn to work on most devices
        if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        }
        audioManager.isSpeakerphoneOn = on
        _isSpeakerphoneOn.value = on
    }

    fun toggleSpeakerphone() {
        val newState = !audioManager.isSpeakerphoneOn
        setSpeakerphone(newState)
    }

    fun stopAudio() {
        Log.d("AudioHandler", "Stopping audio handling")
        // Restore previous settings
        audioManager.mode = previousAudioMode
        audioManager.isMicrophoneMute = previousMicrophoneMute
        audioManager.isSpeakerphoneOn = previousSpeakerphoneOn
        _isSpeakerphoneOn.value = previousSpeakerphoneOn
        
        // Abandon Audio Focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
