package com.gemini.phoneai.services

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log
import com.gemini.phoneai.audio.CallAudioProcessor
import com.gemini.phoneai.gemini.GeminiLiveClient
import kotlinx.coroutines.*

class GeminiConnection : Connection() {
    companion object {
        private const val TAG = "GeminiConnection"
    }

    private var geminiClient: GeminiLiveClient? = null
    private var audioProcessor: CallAudioProcessor? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var phoneNumber: String? = null

    init {
        Log.d(TAG, "GeminiConnection created")
    }

    override fun onAnswer() {
        Log.d(TAG, "Call answered")
        setActive()
        startGeminiIntegration()
    }

    override fun onAnswer(videoState: Int) {
        Log.d(TAG, "Call answered with video state: $videoState")
        onAnswer()
    }

    override fun onDisconnect() {
        Log.d(TAG, "Call disconnected")
        stopGeminiIntegration()
        setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        destroy()
    }

    override fun onHold() {
        Log.d(TAG, "Call held")
        setOnHold()
        audioProcessor?.pauseAudio()
    }

    override fun onUnhold() {
        Log.d(TAG, "Call unheld")
        setActive()
        audioProcessor?.resumeAudio()
    }

    override fun onPlayDtmfTone(c: Char) {
        Log.d(TAG, "DTMF tone: $c")
    }

    override fun onAbort() {
        Log.d(TAG, "Call aborted")
        stopGeminiIntegration()
        setDisconnected(DisconnectCause(DisconnectCause.CANCELED))
        destroy()
    }

    override fun onReject() {
        Log.d(TAG, "Call rejected")
        stopGeminiIntegration()
        setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
        destroy()
    }

    fun startOutgoingCall(phoneNumber: String) {
        Log.d(TAG, "Starting outgoing call to: $phoneNumber")
        this.phoneNumber = phoneNumber
        
        coroutineScope.launch {
            // Simulate connection process
            delay(1000)
            
            withContext(Dispatchers.Main) {
                // After connection established
                setActive()
                startGeminiIntegration()
            }
        }
    }

    fun startIncomingCallHandling() {
        Log.d(TAG, "Handling incoming call with AI")
        startGeminiIntegration()
    }

    private fun startGeminiIntegration() {
        Log.d(TAG, "Starting Gemini integration")
        
        coroutineScope.launch {
            try {
                // Initialize Gemini client
                if (geminiClient == null) {
                    geminiClient = GeminiLiveClient()
                }
                
                // Initialize audio processor
                if (audioProcessor == null) {
                    audioProcessor = CallAudioProcessor(this@GeminiConnection)
                }
                
                // Connect to Gemini
                geminiClient?.connect()
                
                // Start audio processing
                audioProcessor?.startProcessing()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Gemini integration", e)
                withContext(Dispatchers.Main) {
                    setDisconnected(DisconnectCause(DisconnectCause.ERROR))
                    destroy()
                }
            }
        }
    }

    private fun stopGeminiIntegration() {
        Log.d(TAG, "Stopping Gemini integration")
        
        coroutineScope.launch {
            try {
                audioProcessor?.stopProcessing()
                geminiClient?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Gemini integration", e)
            }
        }
        
        // Cancel all coroutines
        coroutineScope.cancel()
    }
}
