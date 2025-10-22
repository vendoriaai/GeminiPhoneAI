package com.gemini.phoneai.services

import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import kotlinx.coroutines.*

class GeminiInCallService : InCallService() {
    companion object {
        private const val TAG = "GeminiInCallService"
        var instance: GeminiInCallService? = null
            private set
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeCalls = mutableMapOf<Call, Call.Callback>()

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "InCallService created")
        instance = this
    }

    override fun onDestroy() {
        Log.d(TAG, "InCallService destroyed")
        instance = null
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        Log.d(TAG, "Call added: ${call.details?.handle}")
        
        val callCallback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.d(TAG, "Call state changed: ${getStateString(state)}")
                
                when (state) {
                    Call.STATE_RINGING -> {
                        Log.d(TAG, "Incoming call ringing")
                        handleIncomingCall(call)
                    }
                    Call.STATE_ACTIVE -> {
                        Log.d(TAG, "Call is now active")
                        notifyCallActive(call)
                    }
                    Call.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Call disconnected")
                        handleCallDisconnected(call)
                    }
                    Call.STATE_DIALING -> {
                        Log.d(TAG, "Call is dialing")
                    }
                    Call.STATE_CONNECTING -> {
                        Log.d(TAG, "Call is connecting")
                    }
                    Call.STATE_HOLDING -> {
                        Log.d(TAG, "Call is on hold")
                    }
                }
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                Log.d(TAG, "Call details changed")
            }
        }

        call.registerCallback(callCallback)
        activeCalls[call] = callCallback

        // Handle initial state
        when (call.state) {
            Call.STATE_RINGING -> handleIncomingCall(call)
            Call.STATE_ACTIVE -> notifyCallActive(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        Log.d(TAG, "Call removed")
        
        activeCalls[call]?.let { callback ->
            call.unregisterCallback(callback)
            activeCalls.remove(call)
        }
    }

    private fun handleIncomingCall(call: Call) {
        coroutineScope.launch {
            delay(500) // Small delay to ensure everything is ready
            
            // Auto-answer for AI processing
            Log.d(TAG, "Auto-answering incoming call for AI processing")
            call.answer(VideoProfile.STATE_AUDIO_ONLY)
        }
    }

    private fun notifyCallActive(call: Call) {
        // Notify that call is active and AI should start processing
        Log.d(TAG, "Call is active, AI processing should begin")
    }

    private fun handleCallDisconnected(call: Call) {
        // Clean up resources for this call
        Log.d(TAG, "Handling call disconnection cleanup")
        
        activeCalls[call]?.let { callback ->
            call.unregisterCallback(callback)
            activeCalls.remove(call)
        }
    }

    fun endCall(call: Call) {
        call.disconnect()
    }

    fun holdCall(call: Call) {
        call.hold()
    }

    fun unholdCall(call: Call) {
        call.unhold()
    }

    fun muteCall(mute: Boolean) {
        setMuted(mute)
    }

    fun answerCall(call: Call) {
        call.answer(VideoProfile.STATE_AUDIO_ONLY)
    }

    fun rejectCall(call: Call, rejectWithMessage: Boolean = false, message: String? = null) {
        if (rejectWithMessage && message != null) {
            call.reject(true, message)
        } else {
            call.reject(false, null)
        }
    }

    private fun getStateString(state: Int): String {
        return when (state) {
            Call.STATE_NEW -> "NEW"
            Call.STATE_RINGING -> "RINGING"
            Call.STATE_DIALING -> "DIALING"
            Call.STATE_ACTIVE -> "ACTIVE"
            Call.STATE_HOLDING -> "HOLDING"
            Call.STATE_DISCONNECTED -> "DISCONNECTED"
            Call.STATE_CONNECTING -> "CONNECTING"
            Call.STATE_DISCONNECTING -> "DISCONNECTING"
            Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
            else -> "UNKNOWN"
        }
    }
}
