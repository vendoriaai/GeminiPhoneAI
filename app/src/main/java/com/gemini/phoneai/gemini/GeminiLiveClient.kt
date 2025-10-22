package com.gemini.phoneai.gemini

import android.util.Base64
import android.util.Log
import com.gemini.phoneai.BuildConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class GeminiLiveClient {
    companion object {
        private const val TAG = "GeminiLiveClient"
        private const val GEMINI_WS_URL = "wss://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:bidiGenerateContent"
        private const val AUDIO_SAMPLE_RATE = 16000
    }

    interface Callback {
        fun onAudioResponse(audioData: ByteArray)
        fun onTextResponse(text: String)
        fun onError(error: String)
        fun onConnected()
        fun onDisconnected()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private var callback: Callback? = null
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)
    private val textQueue = Channel<String>(Channel.UNLIMITED)
    
    private var isConnected = false
    private var apiKey: String? = null

    init {
        // Get API key from BuildConfig or secure storage
        apiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
        
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "API key not configured. Please set GEMINI_API_KEY in gradle.properties")
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setApiKey(key: String) {
        apiKey = key
    }

    fun connect() {
        if (apiKey.isNullOrBlank()) {
            callback?.onError("API key not configured")
            return
        }

        if (isConnected) {
            Log.w(TAG, "Already connected")
            return
        }

        val url = "$GEMINI_WS_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected = true
                sendInitialConfiguration()
                callback?.onConnected()
                startMessageProcessors()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received text message: ${text.take(200)}")
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received binary message: ${bytes.size} bytes")
                handleBinaryMessage(bytes.toByteArray())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $reason")
                isConnected = false
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                isConnected = false
                callback?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}", t)
                isConnected = false
                callback?.onError(t.message ?: "Unknown error")
            }
        })
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting WebSocket")
        isConnected = false
        webSocket?.close(1000, "Disconnecting")
        webSocket = null
        coroutineScope.cancel()
    }

    fun sendAudioData(audioData: ByteArray) {
        if (!isConnected) {
            Log.w(TAG, "Cannot send audio - not connected")
            return
        }

        coroutineScope.launch {
            audioQueue.send(audioData)
        }
    }

    fun sendTextMessage(text: String) {
        if (!isConnected) {
            Log.w(TAG, "Cannot send text - not connected")
            return
        }

        coroutineScope.launch {
            textQueue.send(text)
        }
    }

    private fun sendInitialConfiguration() {
        val config = JsonObject().apply {
            add("setup", JsonObject().apply {
                add("model", JsonObject().apply {
                    addProperty("model", "models/gemini-2.0-flash-exp")
                })
                
                add("generationConfig", JsonObject().apply {
                    add("responseModalities", com.google.gson.JsonArray().apply {
                        add("AUDIO")
                        add("TEXT")
                    })
                    add("speechConfig", JsonObject().apply {
                        add("voiceConfig", JsonObject().apply {
                            add("prebuiltVoiceConfig", JsonObject().apply {
                                addProperty("voiceName", "Aoede")  // Natural voice
                            })
                        })
                    })
                })
                
                add("systemInstruction", JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", """
                                You are a helpful AI phone assistant. You are handling a phone call.
                                Be natural, conversational, and helpful. Keep responses concise and clear.
                                Listen carefully to what the caller says and respond appropriately.
                                If you don't understand something, politely ask for clarification.
                            """.trimIndent())
                        })
                    })
                })
            })
        }

        val message = gson.toJson(config)
        Log.d(TAG, "Sending initial configuration")
        webSocket?.send(message)
    }

    private fun startMessageProcessors() {
        // Process outgoing audio
        coroutineScope.launch {
            audioQueue.receiveAsFlow()
                .collect { audioData ->
                    sendAudioChunk(audioData)
                }
        }

        // Process outgoing text
        coroutineScope.launch {
            textQueue.receiveAsFlow()
                .collect { text ->
                    sendTextChunk(text)
                }
        }
    }

    private fun sendAudioChunk(audioData: ByteArray) {
        val message = JsonObject().apply {
            add("realtimeInput", JsonObject().apply {
                add("mediaChunks", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("mimeType", "audio/pcm;rate=$AUDIO_SAMPLE_RATE")
                        addProperty("data", Base64.encodeToString(audioData, Base64.NO_WRAP))
                    })
                })
            })
        }

        val jsonMessage = gson.toJson(message)
        webSocket?.send(jsonMessage)
    }

    private fun sendTextChunk(text: String) {
        val message = JsonObject().apply {
            add("clientContent", JsonObject().apply {
                add("turns", com.google.gson.JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("role", "user")
                        add("parts", com.google.gson.JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("text", text)
                            })
                        })
                    })
                })
                addProperty("turnComplete", true)
            })
        }

        val jsonMessage = gson.toJson(message)
        Log.d(TAG, "Sending text message: $text")
        webSocket?.send(jsonMessage)
    }

    private fun handleTextMessage(text: String) {
        try {
            val response = gson.fromJson(text, JsonObject::class.java)
            
            // Check for server content (audio/text responses)
            response.getAsJsonObject("serverContent")?.let { serverContent ->
                serverContent.getAsJsonObject("modelTurn")?.let { modelTurn ->
                    modelTurn.getAsJsonArray("parts")?.forEach { part ->
                        val partObj = part.asJsonObject
                        
                        // Handle text response
                        partObj.getAsJsonPrimitive("text")?.let { textPrimitive ->
                            val responseText = textPrimitive.asString
                            Log.d(TAG, "Text response: $responseText")
                            callback?.onTextResponse(responseText)
                        }
                        
                        // Handle inline audio data
                        partObj.getAsJsonObject("inlineData")?.let { inlineData ->
                            inlineData.getAsJsonPrimitive("mimeType")?.let { mimeType ->
                                if (mimeType.asString.startsWith("audio/")) {
                                    inlineData.getAsJsonPrimitive("data")?.let { data ->
                                        val audioBytes = Base64.decode(data.asString, Base64.NO_WRAP)
                                        callback?.onAudioResponse(audioBytes)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Check for setup complete
            response.getAsJsonObject("setupComplete")?.let {
                Log.d(TAG, "Setup complete")
            }
            
            // Check for errors
            response.getAsJsonObject("error")?.let { error ->
                val errorMessage = error.getAsJsonPrimitive("message")?.asString ?: "Unknown error"
                Log.e(TAG, "Server error: $errorMessage")
                callback?.onError(errorMessage)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            callback?.onError("Parse error: ${e.message}")
        }
    }

    private fun handleBinaryMessage(data: ByteArray) {
        // Binary messages are typically audio responses
        callback?.onAudioResponse(data)
    }

    fun isConnected(): Boolean = isConnected
}
