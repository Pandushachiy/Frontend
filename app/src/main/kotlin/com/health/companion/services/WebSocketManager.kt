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
import org.json.JSONObject as OrgJsonObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
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
    data class ReminderPush(val data: JsonObject) : WsEvent()
    /** New-style reminder push from backend: update chat UI, FCM handles system notification. */
    data class ReminderEvent(
        val reminderId: String,
        val messageId: String,
        val conversationId: String,
        val title: String,
        val message: String,
        val priority: String,
    ) : WsEvent()
    data class Message(val message: WebSocketMessage) : WsEvent()
    data class Error(val message: String) : WsEvent()
    data class BackgroundTaskResult(
        val taskId: String,
        val taskName: String,
        val result: String
    ) : WsEvent()
}

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val notificationHelper: NotificationHelper,
    private val reminderDeduplicator: ReminderDeduplicator,
) {
    
    private var webSocket: WebSocket? = null
    private var currentUserId: String? = null
    private val reconnectAttempt = AtomicInteger(0)
    private var reconnectEnabled = true
    
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
        currentUserId = userId
        reconnectAttempt.set(0)
        val token = tokenManager.getAccessTokenSync()
        
        if (token == null) {
            Timber.e("Cannot connect WebSocket: no access token")
            _events.tryEmit(WsEvent.Error("No access token available"))
            close(IllegalStateException("No access token available"))
            return@callbackFlow
        }
        
        val wsUrl = "${BuildConfig.WS_URL}?token=$token"
        Timber.d("WebSocket connecting to: ${BuildConfig.WS_URL}")
        
        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .build()
        
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("WebSocket connected for user: $userId")
                reconnectAttempt.set(0)
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
                scheduleReconnect()
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code - $reason")
                webSocket.close(code, reason)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code - $reason")
                _connectionState.value = false
                _events.tryEmit(WsEvent.Disconnected)
                if (code != 1000) scheduleReconnect()
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
                
                "reminder" -> {
                    // New-style reminder push. FCM handles the system notification.
                    // We only update the chat UI and mark the id for deduplication.
                    val reminderId      = message.data?.get("reminder_id")?.jsonPrimitive?.contentOrNull ?: ""
                    val messageId       = message.data?.get("message_id")?.jsonPrimitive?.contentOrNull ?: ""
                    val conversationId  = message.data?.get("conversation_id")?.jsonPrimitive?.contentOrNull ?: ""
                    val title           = message.data?.get("title")?.jsonPrimitive?.contentOrNull ?: ""
                    val msg             = message.data?.get("message")?.jsonPrimitive?.contentOrNull ?: ""
                    val priority        = message.data?.get("priority")?.jsonPrimitive?.contentOrNull ?: "medium"

                    reminderDeduplicator.markHandled(reminderId)
                    _events.tryEmit(WsEvent.ReminderEvent(
                        reminderId     = reminderId,
                        messageId      = messageId,
                        conversationId = conversationId,
                        title          = title,
                        message        = msg,
                        priority       = priority,
                    ))
                    Timber.d("WS reminder received: '$title' (id=$reminderId conv=$conversationId)")
                }

                "notification" -> {
                    message.data?.let { data ->
                        _events.tryEmit(WsEvent.Notification(data))
                        // Legacy reminder subtype — mark for dedup but do NOT show notification
                        // (FCM will deliver the system notification)
                        val notificationType = data["notification_type"]?.jsonPrimitive?.contentOrNull
                        if (notificationType == "reminder") {
                            val reminderId = data["reminder_id"]?.jsonPrimitive?.contentOrNull ?: ""
                            reminderDeduplicator.markHandled(reminderId)
                            _events.tryEmit(WsEvent.ReminderPush(data))
                        }
                    }
                }
                
                "background_task_result" -> {
                    val taskId   = message.data?.get("task_id")?.jsonPrimitive?.contentOrNull ?: ""
                    val taskName = message.data?.get("task_name")?.jsonPrimitive?.contentOrNull ?: ""
                    val result   = message.data?.get("result")?.jsonPrimitive?.contentOrNull ?: ""
                    _events.tryEmit(WsEvent.BackgroundTaskResult(taskId, taskName, result))
                    Timber.d("WS background_task_result: '$taskName' id=$taskId")
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
    
    private fun scheduleReconnect() {
        if (!reconnectEnabled) return
        val uid = currentUserId ?: return
        val attempt = reconnectAttempt.getAndIncrement()
        val delay = (3000L * (1L shl attempt.coerceAtMost(4))).coerceAtMost(60_000L)
        Timber.d("WebSocket: scheduling reconnect in ${delay}ms (attempt $attempt)")
        Thread {
            try {
                Thread.sleep(delay)
                if (!_connectionState.value && reconnectEnabled) {
                    val token = tokenManager.getAccessTokenSync() ?: return@Thread
                    val wsUrl = "${BuildConfig.WS_URL}?token=$token"
                    val request = Request.Builder()
                        .url(wsUrl)
                        .header("Authorization", "Bearer $token")
                        .build()
                    webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                        override fun onOpen(ws: WebSocket, response: Response) {
                            reconnectAttempt.set(0)
                            _connectionState.value = true
                            _events.tryEmit(WsEvent.Connected)
                            Timber.d("WebSocket reconnected for user: $uid")
                        }
                        override fun onMessage(ws: WebSocket, text: String) {
                            handleMessage(text) { msg -> _messages.tryEmit(msg) }
                        }
                        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                            _connectionState.value = false
                            _events.tryEmit(WsEvent.Disconnected)
                            scheduleReconnect()
                        }
                        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                            _connectionState.value = false
                            _events.tryEmit(WsEvent.Disconnected)
                            if (code != 1000) scheduleReconnect()
                        }
                    })
                }
            } catch (e: Exception) {
                Timber.w(e, "WebSocket reconnect failed")
            }
        }.start()
    }

    suspend fun send(message: String) {
        webSocket?.send(message) ?: run {
            Timber.w("WebSocket not connected, cannot send message")
        }
    }
    
    suspend fun sendChatMessage(text: String, conversationId: String? = null, stream: Boolean = true) {
        val data = OrgJsonObject().apply {
            put("message", text)
            if (conversationId != null) put("conversation_id", conversationId)
            put("stream", stream)
        }
        val messageJson = OrgJsonObject().apply {
            put("type", "chat_message")
            put("data", data)
        }
        send(messageJson.toString())
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
        reconnectEnabled = false
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        _connectionState.value = false
        Timber.d("WebSocket disconnected")
    }
    
    fun isConnected(): Boolean = _connectionState.value
}
