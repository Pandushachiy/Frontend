package com.health.companion.services

import com.health.companion.BuildConfig
import com.health.companion.utils.TokenManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WebSocketMessage(
    val agent: String = "",
    val chunk: String = "",
    val confidence: Float = 1.0f,
    val timestamp: String = "",
    val type: String = "message" // "message", "typing", "error"
)

@Singleton
class WebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    
    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<WebSocketMessage>(replay = 0)
    val messages = _messages.asSharedFlow()
    
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
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = json.decodeFromString<WebSocketMessage>(text)
                    trySend(message)
                    Timber.d("WebSocket received: ${message.type} from ${message.agent}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse WebSocket message: $text")
                }
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(t, "WebSocket failure: ${response?.message}")
                close(t)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closing: $code - $reason")
                webSocket.close(code, reason)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("WebSocket closed: $code - $reason")
                close()
            }
        }
        
        webSocket = okHttpClient.newWebSocket(request, listener)
        
        awaitClose {
            Timber.d("WebSocket flow closed")
            webSocket?.close(1000, "Client disconnect")
        }
    }
    
    suspend fun send(message: String) {
        webSocket?.send(message) ?: run {
            Timber.w("WebSocket not connected, cannot send message")
        }
    }
    
    suspend fun sendTyping() {
        val typingMessage = """{"type": "typing"}"""
        send(typingMessage)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        Timber.d("WebSocket disconnected")
    }
    
    fun isConnected(): Boolean = webSocket != null
}
