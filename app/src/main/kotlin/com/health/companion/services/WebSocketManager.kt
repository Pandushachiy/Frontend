package com.health.companion.services

import com.health.companion.BuildConfig
import com.health.companion.utils.TokenManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// ========== WebSocket Message Types ==========

@Serializable
data class WebSocketMessage(
    val type: String = "message",
    val agent: String = "",
    val chunk: String = "",
    val confidence: Float = 1.0f,
    val timestamp: String = "",
    val data: JsonObject? = null,
    @SerialName("message_id") val messageId: String? = null
)

@Serializable
data class EmotionUpdateData(
    val valence: Float = 0f,
    val arousal: Float = 0f,
    @SerialName("primary_emotion") val primaryEmotion: String? = null,
    @SerialName("needs_support") val needsSupport: Boolean = false,
    @SerialName("mood_label") val moodLabel: String? = null
)

// ========== WebSocket Events ==========

sealed class WsEvent {
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class AiTyping(val isTyping: Boolean) : WsEvent()
    data class StreamStart(val streamId: String) : WsEvent()
    data class StreamChunk(val chunk: String, val fullContent: String, val progress: Float) : WsEvent()
    data class StreamEnd(val fullContent: String) : WsEvent()
    data class EmotionUpdate(val data: EmotionUpdateData) : WsEvent()
    data class Notification(val data: JsonObject) : WsEvent()
    data class Message(val message: WebSocketMessage) : WsEvent()
    data class Error(val message: String) : WsEvent()
}

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    
    private var webSocket: WebSocket? = null
    
    // Legacy flow for backward compatibility
    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val messages = _messages.asSharedFlow()
    
    // New events flow for streaming support
    private val _events = MutableSharedFlow<WsEvent>(replay = 0)
    val events = _events.asSharedFlow()
    
    // Connection state
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    // Current streaming content
    private val _currentStreamContent = MutableStateFlow("")
    val currentStreamContent: StateFlow<String> = _currentStreamContent.asStateFlow()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /**
     * Connect to WebSocket using JWT token (not userId!)
     * URL format: ws://HOST/api/v1/chat/ws?token=JWT_TOKEN
     */
    fun connect(userId: String): Flow<WebSocketMessage> = callbackFlow {
        // Get JWT token for WebSocket authentication
        val token = runBlocking { tokenManager.getAccessToken() }
        
        if (token == null) {
            Timber.e("Cannot connect WebSocket: no access token")
            _events.tryEmit(WsEvent.Error("No access token available"))
            close(IllegalStateException("No access token available"))
            return@callbackFlow
        }
        
        // Use token as query parameter, NOT in path!
        val wsUrl = "${BuildConfig.WS_URL}?token=$token"
        Timber.d("WebSocket connecting to: ${BuildConfig.WS_URL}?token=***")
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected for user: $userId")
                _connectionState.value = true
                _events.tryEmit(WsEvent.Connected)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text) { message ->
                    trySend(message)
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure: ${response?.message}")
                _connectionState.value = false
                _events.tryEmit(WsEvent.Error(t.message ?: "Connection failed"))
                _events.tryEmit(WsEvent.Disconnected)
                close(t)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code - $reason")
                webSocket.close(code, reason)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code - $reason")
                _connectionState.value = false
                _events.tryEmit(WsEvent.Disconnected)
                close()
            }
        }
        
        webSocket = okHttpClient.newWebSocket(request, listener)
        
        awaitClose {
            Timber.d("WebSocket flow closed")
            webSocket?.close(1000, "Client disconnect")
        }
    }
    
    /**
     * Handle incoming WebSocket message with streaming support
     */
    private fun handleMessage(text: String, legacySend: (WebSocketMessage) -> Unit) {
        try {
            val message = json.decodeFromString<WebSocketMessage>(text)
            
            when (message.type) {
                "connected" -> {
                    _events.tryEmit(WsEvent.Connected)
                }
                
                "ai_typing" -> {
                    val isTyping = message.data?.get("is_typing")?.jsonPrimitive?.boolean ?: false
                    _events.tryEmit(WsEvent.AiTyping(isTyping))
                }
                
                "stream_start" -> {
                    val streamId = message.data?.get("stream_id")?.jsonPrimitive?.contentOrNull ?: ""
                    _currentStreamContent.value = ""
                    _events.tryEmit(WsEvent.StreamStart(streamId))
                }
                
                "stream_chunk" -> {
                    message.data?.let { data ->
                        val chunk = data["chunk"]?.jsonPrimitive?.contentOrNull ?: ""
                        val fullContent = data["full_content"]?.jsonPrimitive?.contentOrNull ?: ""
                        val progress = data["progress"]?.jsonPrimitive?.floatOrNull ?: 0f
                        
                        _currentStreamContent.value = fullContent
                        _events.tryEmit(WsEvent.StreamChunk(chunk, fullContent, progress))
                    }
                }
                
                "stream_end" -> {
                    val fullContent = message.data?.get("full_content")?.jsonPrimitive?.contentOrNull 
                        ?: _currentStreamContent.value
                    _events.tryEmit(WsEvent.StreamEnd(fullContent))
                    _currentStreamContent.value = ""
                }
                
                "emotion_update" -> {
                    message.data?.let { data ->
                        try {
                            val emotionData = json.decodeFromJsonElement(EmotionUpdateData.serializer(), data)
                            _events.tryEmit(WsEvent.EmotionUpdate(emotionData))
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to parse emotion update")
                        }
                    }
                }
                
                "notification" -> {
                    message.data?.let { _events.tryEmit(WsEvent.Notification(it)) }
                }
                
                "error" -> {
                    val error = message.data?.get("error")?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                    _events.tryEmit(WsEvent.Error(error))
                }
                
                "message", "typing" -> {
                    // Legacy message handling
                    legacySend(message)
                    _messages.tryEmit(message)
                    _events.tryEmit(WsEvent.Message(message))
                }
                
                else -> {
                    // Unknown type - emit as generic message
                    legacySend(message)
                    _messages.tryEmit(message)
                    _events.tryEmit(WsEvent.Message(message))
                }
            }
            
            Timber.d("WebSocket received: ${message.type}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message: $text")
            _events.tryEmit(WsEvent.Error("Parse error: ${e.message}"))
        }
    }
    
    suspend fun send(message: String) {
        webSocket?.send(message) ?: run {
            Timber.w("WebSocket not connected, cannot send message")
        }
    }
    
    /**
     * Send chat message with streaming support
     */
    suspend fun sendChatMessage(text: String, conversationId: String? = null, stream: Boolean = true) {
        val messageJson = buildString {
            append("""{"type":"chat_message","data":{"message":""")
            append(json.encodeToString(kotlinx.serialization.serializer<String>(), text))
            if (conversationId != null) {
                append(""","conversation_id":"$conversationId"""")
            }
            append(""","stream":$stream}}""")
        }
        send(messageJson)
    }
    
    suspend fun sendTyping() {
        send("""{"type":"typing_start"}""")
    }
    
    suspend fun sendTypingStop() {
        send("""{"type":"typing_stop"}""")
    }
    
    suspend fun sendPing() {
        send("""{"type":"ping"}""")
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = false
        Timber.d("WebSocket disconnected")
    }
    
    fun isConnected(): Boolean = _connectionState.value
}
