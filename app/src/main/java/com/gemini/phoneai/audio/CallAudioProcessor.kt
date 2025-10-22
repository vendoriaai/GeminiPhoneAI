package com.gemini.phoneai.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.ActivityCompat
import com.gemini.phoneai.gemini.GeminiLiveClient
import com.gemini.phoneai.services.GeminiConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CallAudioProcessor(
    private val connection: GeminiConnection
) : GeminiLiveClient.Callback {
    
    companion object {
        private const val TAG = "CallAudioProcessor"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO
        private const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val AUDIO_BUFFER_MULTIPLIER = 2
    }

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var textToSpeech: TextToSpeech? = null
    private var geminiClient: GeminiLiveClient? = null
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)
    
    private val audioPlaybackQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private var bufferSize: Int = 0
    private var context: Context? = null

    init {
        initializeAudioComponents()
        initializeGeminiClient()
    }

    private fun initializeAudioComponents() {
        try {
            // Calculate buffer size
            bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size")
                bufferSize = SAMPLE_RATE * 2 // fallback
            }

            // Initialize AudioRecord for capturing call audio
            val audioSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                MediaRecorder.AudioSource.VOICE_COMMUNICATION
            } else {
                MediaRecorder.AudioSource.VOICE_CALL
            }

            audioRecord = AudioRecord(
                audioSource,
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT,
                bufferSize * AUDIO_BUFFER_MULTIPLIER
            )

            // Initialize AudioTrack for playing AI responses
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG_OUT)
                .setEncoding(AUDIO_FORMAT)
                .build()

            audioTrack = AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize * AUDIO_BUFFER_MULTIPLIER,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )

            Log.d(TAG, "Audio components initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize audio components", e)
        }
    }

    private fun initializeGeminiClient() {
        geminiClient = GeminiLiveClient().apply {
            setCallback(this@CallAudioProcessor)
        }
    }

    fun initializeTextToSpeech(context: Context) {
        this.context = context
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported")
                } else {
                    Log.d(TAG, "TTS initialized successfully")
                    setupTtsListeners()
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun setupTtsListeners() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "TTS started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "TTS completed: $utteranceId")
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "TTS error: $utteranceId")
            }
        })
    }

    fun startProcessing() {
        Log.d(TAG, "Starting audio processing")
        
        // Connect to Gemini
        geminiClient?.connect()
        
        // Start audio capture
        startAudioCapture()
        
        // Start audio playback processor
        startAudioPlayback()
    }

    fun stopProcessing() {
        Log.d(TAG, "Stopping audio processing")
        
        isRecording.set(false)
        isPlaying.set(false)
        
        coroutineScope.launch {
            try {
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null

                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null

                geminiClient?.disconnect()
                
                textToSpeech?.stop()
                textToSpeech?.shutdown()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio processing", e)
            }
        }
        
        coroutineScope.cancel()
    }

    fun pauseAudio() {
        Log.d(TAG, "Pausing audio")
        isRecording.set(false)
        audioRecord?.stop()
        audioTrack?.pause()
    }

    fun resumeAudio() {
        Log.d(TAG, "Resuming audio")
        startAudioCapture()
        audioTrack?.play()
    }

    private fun startAudioCapture() {
        if (audioRecord == null) {
            Log.e(TAG, "AudioRecord not initialized")
            return
        }

        coroutineScope.launch {
            try {
                audioRecord?.startRecording()
                isRecording.set(true)
                
                val buffer = ByteArray(bufferSize)
                
                while (isRecording.get()) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        // Send audio chunk to Gemini
                        val audioChunk = buffer.copyOf(bytesRead)
                        geminiClient?.sendAudioData(audioChunk)
                        
                        // Optional: Process audio locally (e.g., volume detection)
                        processAudioLocally(audioChunk)
                    }
                    
                    delay(10) // Small delay to prevent tight loop
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in audio capture", e)
                isRecording.set(false)
            }
        }
    }

    private fun startAudioPlayback() {
        coroutineScope.launch {
            audioTrack?.play()
            isPlaying.set(true)
            
            audioPlaybackQueue.receiveAsFlow().collect { audioData ->
                if (isPlaying.get()) {
                    playAudioData(audioData)
                }
            }
        }
    }

    private fun playAudioData(audioData: ByteArray) {
        try {
            audioTrack?.let { track ->
                val written = track.write(audioData, 0, audioData.size)
                if (written < 0) {
                    Log.e(TAG, "Error writing audio data: $written")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        }
    }

    private fun processAudioLocally(audioData: ByteArray) {
        // Calculate audio volume for visualization or silence detection
        val shorts = ShortArray(audioData.size / 2)
        ByteBuffer.wrap(audioData).asShortBuffer().get(shorts)
        
        var sum = 0.0
        for (sample in shorts) {
            sum += sample * sample
        }
        val rms = Math.sqrt(sum / shorts.size)
        val db = 20 * Math.log10(rms)
        
        // Log volume level occasionally
        if (System.currentTimeMillis() % 1000 < 50) {
            Log.v(TAG, "Audio level: ${db.toInt()} dB")
        }
    }

    // GeminiLiveClient.Callback implementations
    override fun onAudioResponse(audioData: ByteArray) {
        Log.d(TAG, "Received audio response: ${audioData.size} bytes")
        
        coroutineScope.launch {
            audioPlaybackQueue.send(audioData)
        }
    }

    override fun onTextResponse(text: String) {
        Log.d(TAG, "Received text response: $text")
        
        // Optionally use TTS as fallback if no audio response
        if (textToSpeech != null && !isPlaying.get()) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, "gemini_${System.currentTimeMillis()}")
        }
    }

    override fun onError(error: String) {
        Log.e(TAG, "Gemini error: $error")
    }

    override fun onConnected() {
        Log.d(TAG, "Connected to Gemini")
    }

    override fun onDisconnected() {
        Log.d(TAG, "Disconnected from Gemini")
    }

    fun setMuted(muted: Boolean) {
        if (muted) {
            audioRecord?.stop()
        } else {
            audioRecord?.startRecording()
        }
    }

    fun setSpeakerphone(enabled: Boolean, context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = enabled
    }
}
