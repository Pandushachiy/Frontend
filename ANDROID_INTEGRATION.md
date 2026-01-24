# 🤖 Android Integration Guide v3.1

> Гайд для Android-разработчиков (Kotlin + Jetpack Compose)

**Версия API:** 3.1.0  
**Base URL:** `https://api.pand-ai.com/api/v1`  
**Дата:** 22 января 2026

---

## 📋 Содержание

1. [Новые Endpoints](#-новые-endpoints)
2. [WebSocket интеграция](#-websocket-интеграция)
3. [Push Notifications (FCM)](#-push-notifications-fcm)
4. [Intelligence API](#-intelligence-api)
5. [Dashboard виджеты](#-dashboard-виджеты)
6. [Предложения по UI/UX](#-предложения-по-uiux)
7. [Архитектура](#-архитектура)

---

## 📡 Новые Endpoints

### Полный список v3.1

```kotlin
// === WebSocket ===
WS   /ws/chat?token={jwt}        // Real-time чат со streaming
GET  /ws/stats                   // Статистика соединений
GET  /ws/online                  // Online пользователи

// === Push Notifications ===
GET  /push/vapid-key             // VAPID ключ (для Web Push, не для FCM)
POST /push/subscribe             // Подписка на push
POST /push/unsubscribe           // Отписка
GET  /push/subscriptions         // Список подписок
POST /push/test                  // Тестовое уведомление
GET  /push/stats                 // Статистика

// === Dashboard Widgets ===
GET  /dashboard/mood-chart?days=7    // График настроения
GET  /dashboard/streak               // Серия активности  
GET  /dashboard/emotional-state      // Эмоциональное состояние
GET  /dashboard/memory-summary       // Сводка памяти AI

// === Intelligence API ===
POST /intelligence/emotions/analyze      // Анализ эмоций в тексте
GET  /intelligence/emotions/summary      // Эмоциональная сводка
GET  /intelligence/emotions/techniques   // Терапевтические техники

POST /intelligence/briefing              // Генерация брифинга
GET  /intelligence/notifications         // Pending уведомления
GET  /intelligence/notifications/unread-count
POST /intelligence/notifications/{id}/read
POST /intelligence/notifications/{id}/dismiss
GET  /intelligence/notifications/preferences
PATCH /intelligence/notifications/preferences

POST /intelligence/memory/search         // Поиск по памяти
POST /intelligence/memory/remember       // Запомнить факт
POST /intelligence/memory/forget         // Забыть факт
GET  /intelligence/memory/stats          // Статистика памяти

// === Security ===
GET  /security/rate-limit/stats          // Rate limit статистика
GET  /security/audit/events              // Audit лог
GET  /security/health-check              // Security health
```

---

## 🔌 WebSocket интеграция

### Зависимости

```kotlin
// build.gradle.kts (app)
dependencies {
    // OkHttp для WebSocket
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
```

### WebSocket Manager

```kotlin
// data/remote/WebSocketManager.kt
package com.pandai.data.remote

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.TimeUnit

// === Message Types ===

@Serializable
data class WsMessage(
    val type: String,
    val data: JsonObject? = null,
    val timestamp: String? = null,
    val message_id: String? = null
)

@Serializable
data class ChatMessageData(
    val message: String,
    val conversation_id: String? = null,
    val stream: Boolean = true
)

@Serializable
data class StreamChunkData(
    val stream_id: String,
    val chunk: String,
    val full_content: String,
    val progress: Float? = null
)

@Serializable
data class EmotionUpdateData(
    val valence: Float,
    val arousal: Float,
    val primary_emotion: String?,
    val needs_support: Boolean,
    val mood_label: String?
)

// === WebSocket Events ===

sealed class WsEvent {
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class AiTyping(val isTyping: Boolean) : WsEvent()
    data class StreamStart(val streamId: String) : WsEvent()
    data class StreamChunk(val chunk: String, val fullContent: String, val progress: Float) : WsEvent()
    data class StreamEnd(val fullContent: String) : WsEvent()
    data class EmotionUpdate(val data: EmotionUpdateData) : WsEvent()
    data class Notification(val data: JsonObject) : WsEvent()
    data class Error(val message: String) : WsEvent()
}

// === WebSocket Manager ===

class WebSocketManager(
    private val baseUrl: String = "wss://api.pand-ai.com/api/v1"
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _events = MutableSharedFlow<WsEvent>(replay = 0)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()
    
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    fun connect(token: String) {
        val request = Request.Builder()
            .url("$baseUrl/ws/chat?token=$token")
            .build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = true
                _events.tryEmit(WsEvent.Connected)
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                _events.tryEmit(WsEvent.Disconnected)
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                _events.tryEmit(WsEvent.Error(t.message ?: "Connection failed"))
                // Auto-reconnect after 3 seconds
                reconnect(token)
            }
        })
    }
    
    private fun handleMessage(text: String) {
        try {
            val message = json.decodeFromString<WsMessage>(text)
            
            when (message.type) {
                "connected" -> _events.tryEmit(WsEvent.Connected)
                
                "ai_typing" -> {
                    val isTyping = message.data?.get("is_typing")?.jsonPrimitive?.boolean ?: false
                    _events.tryEmit(WsEvent.AiTyping(isTyping))
                }
                
                "stream_start" -> {
                    val streamId = message.data?.get("stream_id")?.jsonPrimitive?.content ?: ""
                    _events.tryEmit(WsEvent.StreamStart(streamId))
                }
                
                "stream_chunk" -> {
                    message.data?.let { data ->
                        _events.tryEmit(WsEvent.StreamChunk(
                            chunk = data["chunk"]?.jsonPrimitive?.content ?: "",
                            fullContent = data["full_content"]?.jsonPrimitive?.content ?: "",
                            progress = data["progress"]?.jsonPrimitive?.floatOrNull ?: 0f
                        ))
                    }
                }
                
                "stream_end" -> {
                    val fullContent = message.data?.get("full_content")?.jsonPrimitive?.content ?: ""
                    _events.tryEmit(WsEvent.StreamEnd(fullContent))
                }
                
                "emotion_update" -> {
                    message.data?.let { data ->
                        val emotionData = json.decodeFromJsonElement<EmotionUpdateData>(data)
                        _events.tryEmit(WsEvent.EmotionUpdate(emotionData))
                    }
                }
                
                "notification" -> {
                    message.data?.let { _events.tryEmit(WsEvent.Notification(it)) }
                }
                
                "error" -> {
                    val error = message.data?.get("error")?.jsonPrimitive?.content ?: "Unknown error"
                    _events.tryEmit(WsEvent.Error(error))
                }
            }
        } catch (e: Exception) {
            _events.tryEmit(WsEvent.Error("Parse error: ${e.message}"))
        }
    }
    
    fun sendMessage(text: String, conversationId: String? = null) {
        val data = ChatMessageData(
            message = text,
            conversation_id = conversationId,
            stream = true
        )
        
        val message = WsMessage(
            type = "chat_message",
            data = json.encodeToJsonElement(data).jsonObject
        )
        
        webSocket?.send(json.encodeToString(message))
    }
    
    fun sendTypingStart() {
        webSocket?.send("""{"type":"typing_start"}""")
    }
    
    fun sendTypingStop() {
        webSocket?.send("""{"type":"typing_stop"}""")
    }
    
    fun sendPing() {
        webSocket?.send("""{"type":"ping"}""")
    }
    
    private fun reconnect(token: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!_connectionState.value) {
                connect(token)
            }
        }, 3000)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }
}
```

### ViewModel для чата

```kotlin
// presentation/chat/ChatViewModel.kt
package com.pandai.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pandai.data.remote.WebSocketManager
import com.pandai.data.remote.WsEvent
import com.pandai.data.remote.EmotionUpdateData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentResponse: String = "",
    val isAiTyping: Boolean = false,
    val isConnected: Boolean = false,
    val emotionalState: EmotionUpdateData? = null,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    init {
        observeWebSocket()
        connect()
    }
    
    private fun observeWebSocket() {
        viewModelScope.launch {
            webSocketManager.connectionState.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }
        
        viewModelScope.launch {
            webSocketManager.events.collect { event ->
                handleWsEvent(event)
            }
        }
    }
    
    private fun handleWsEvent(event: WsEvent) {
        when (event) {
            is WsEvent.AiTyping -> {
                _uiState.update { it.copy(isAiTyping = event.isTyping) }
            }
            
            is WsEvent.StreamStart -> {
                _uiState.update { it.copy(currentResponse = "") }
            }
            
            is WsEvent.StreamChunk -> {
                _uiState.update { it.copy(currentResponse = event.fullContent) }
            }
            
            is WsEvent.StreamEnd -> {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + ChatMessage(
                            role = "assistant",
                            content = event.fullContent
                        ),
                        currentResponse = "",
                        isAiTyping = false
                    )
                }
            }
            
            is WsEvent.EmotionUpdate -> {
                _uiState.update { it.copy(emotionalState = event.data) }
            }
            
            is WsEvent.Error -> {
                _uiState.update { it.copy(error = event.message) }
            }
            
            else -> {}
        }
    }
    
    fun connect() {
        viewModelScope.launch {
            tokenManager.getToken()?.let { token ->
                webSocketManager.connect(token)
            }
        }
    }
    
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        // Add user message
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessage(role = "user", content = text)
            )
        }
        
        // Send to server
        webSocketManager.sendMessage(text)
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    override fun onCleared() {
        webSocketManager.disconnect()
        super.onCleared()
    }
}
```

### Compose UI

```kotlin
// presentation/chat/ChatScreen.kt
package com.pandai.presentation.chat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom
    LaunchedEffect(uiState.messages.size, uiState.currentResponse) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            ChatTopBar(
                isConnected = uiState.isConnected,
                emotionalState = uiState.emotionalState
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message)
                }
                
                // Current streaming response
                if (uiState.currentResponse.isNotEmpty()) {
                    item {
                        MessageBubble(
                            ChatMessage(
                                role = "assistant",
                                content = uiState.currentResponse
                            ),
                            isStreaming = true
                        )
                    }
                }
            }
            
            // Typing indicator
            AnimatedVisibility(
                visible = uiState.isAiTyping && uiState.currentResponse.isEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                TypingIndicator()
            }
            
            // Input
            ChatInput(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                enabled = uiState.isConnected
            )
        }
    }
    
    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar
            viewModel.clearError()
        }
    }
}

@Composable
fun ChatTopBar(
    isConnected: Boolean,
    emotionalState: EmotionUpdateData?
) {
    TopAppBar(
        title = { Text("PAND-AI") },
        actions = {
            // Connection indicator
            Icon(
                imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (isConnected) Color.Green else Color.Red
            )
            
            // Mood indicator
            emotionalState?.let { emotion ->
                Spacer(Modifier.width(8.dp))
                MoodIndicator(emotion)
            }
        }
    )
}

@Composable
fun MoodIndicator(emotion: EmotionUpdateData) {
    val emoji = when {
        emotion.valence > 0.5 -> "😊"
        emotion.valence > 0 -> "🙂"
        emotion.valence > -0.3 -> "😐"
        emotion.valence > -0.6 -> "😔"
        else -> "😢"
    }
    
    val color = when {
        emotion.needs_support -> MaterialTheme.colorScheme.error
        emotion.valence > 0.3 -> Color(0xFF4CAF50)
        emotion.valence < -0.3 -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = emoji,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean = false
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isUser) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (isStreaming) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated dots
        repeat(3) { index ->
            val alpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0f at 0
                        1f at 300 + (index * 100)
                        0f at 600 + (index * 100)
                    }
                )
            )
            
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        CircleShape
                    )
            )
            if (index < 2) Spacer(Modifier.width(4.dp))
        }
        
        Spacer(Modifier.width(8.dp))
        Text(
            "AI печатает...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Написать сообщение...") },
                enabled = enabled,
                maxLines = 4
            )
            
            Spacer(Modifier.width(8.dp))
            
            IconButton(
                onClick = onSend,
                enabled = enabled && text.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Отправить")
            }
        }
    }
}
```

---

## 🔔 Push Notifications (FCM)

### Зависимости

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
}
```

### FCM Service

```kotlin
// data/remote/PandAiFcmService.kt
package com.pandai.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pandai.R
import com.pandai.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PandAiFcmService : FirebaseMessagingService() {
    
    @Inject
    lateinit var pushRepository: PushRepository
    
    override fun onNewToken(token: String) {
        // Отправить новый FCM токен на сервер
        pushRepository.registerFcmToken(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notification = message.notification
        
        // Определяем тип уведомления
        val notificationType = data["type"] ?: "default"
        
        when (notificationType) {
            "mood_check_in" -> showMoodCheckInNotification(data)
            "briefing" -> showBriefingNotification(data)
            "reminder" -> showReminderNotification(data)
            "achievement" -> showAchievementNotification(data)
            else -> showDefaultNotification(notification, data)
        }
    }
    
    private fun showMoodCheckInNotification(data: Map<String, String>) {
        showNotification(
            channelId = CHANNEL_MOOD,
            title = "💚 Как ты себя чувствуешь?",
            body = data["message"] ?: "Отметь своё настроение",
            icon = R.drawable.ic_mood,
            action = "mood_checkin"
        )
    }
    
    private fun showBriefingNotification(data: Map<String, String>) {
        showNotification(
            channelId = CHANNEL_BRIEFING,
            title = data["title"] ?: "📋 Твой брифинг готов",
            body = data["message"] ?: "Посмотри план на день",
            icon = R.drawable.ic_briefing,
            action = "briefing"
        )
    }
    
    private fun showReminderNotification(data: Map<String, String>) {
        showNotification(
            channelId = CHANNEL_REMINDER,
            title = "⏰ Напоминание",
            body = data["message"] ?: "",
            icon = R.drawable.ic_reminder,
            action = "reminder"
        )
    }
    
    private fun showAchievementNotification(data: Map<String, String>) {
        showNotification(
            channelId = CHANNEL_ACHIEVEMENT,
            title = "🏆 Достижение!",
            body = data["message"] ?: "Поздравляем!",
            icon = R.drawable.ic_achievement,
            action = "achievement"
        )
    }
    
    private fun showDefaultNotification(
        notification: RemoteMessage.Notification?,
        data: Map<String, String>
    ) {
        showNotification(
            channelId = CHANNEL_DEFAULT,
            title = notification?.title ?: data["title"] ?: "PAND-AI",
            body = notification?.body ?: data["message"] ?: "",
            icon = R.drawable.ic_notification
        )
    }
    
    private fun showNotification(
        channelId: String,
        title: String,
        body: String,
        icon: Int,
        action: String? = null
    ) {
        createNotificationChannel(channelId)
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            action?.let { putExtra("action", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getChannelName(channelId),
                NotificationManager.IMPORTANCE_HIGH
            )
            
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun getChannelName(channelId: String) = when (channelId) {
        CHANNEL_MOOD -> "Настроение"
        CHANNEL_BRIEFING -> "Брифинги"
        CHANNEL_REMINDER -> "Напоминания"
        CHANNEL_ACHIEVEMENT -> "Достижения"
        else -> "Уведомления"
    }
    
    companion object {
        const val CHANNEL_DEFAULT = "default"
        const val CHANNEL_MOOD = "mood"
        const val CHANNEL_BRIEFING = "briefing"
        const val CHANNEL_REMINDER = "reminder"
        const val CHANNEL_ACHIEVEMENT = "achievement"
    }
}
```

### Push Repository

```kotlin
// data/repository/PushRepository.kt
package com.pandai.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.pandai.data.remote.ApiService
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRepository @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun registerFcmToken(fcmToken: String? = null) {
        val token = fcmToken ?: FirebaseMessaging.getInstance().token.await()
        
        api.subscribeToPush(
            token = tokenManager.getToken() ?: return,
            request = PushSubscribeRequest(
                endpoint = "fcm://$token",
                keys = mapOf(
                    "fcm_token" to token,
                    "platform" to "android"
                ),
                deviceInfo = mapOf(
                    "os" to "Android",
                    "sdk" to Build.VERSION.SDK_INT.toString(),
                    "model" to Build.MODEL
                )
            )
        )
    }
    
    suspend fun unregister() {
        val token = FirebaseMessaging.getInstance().token.await()
        api.unsubscribeFromPush(
            token = tokenManager.getToken() ?: return,
            request = PushUnsubscribeRequest(endpoint = "fcm://$token")
        )
    }
}
```

---

## 🧠 Intelligence API

### Data Classes

```kotlin
// data/model/Intelligence.kt
package com.pandai.data.model

import kotlinx.serialization.Serializable

// === Emotions ===

@Serializable
data class EmotionAnalyzeRequest(
    val text: String
)

@Serializable
data class EmotionAnalyzeResponse(
    val valence: Float,           // -1.0 до +1.0
    val arousal: Float,           // 0.0 до 1.0
    val primary_emotion: String?, // joy, sadness, anger, fear, etc.
    val secondary_emotions: List<String>,
    val mood_label: String,       // happy, sad, anxious, calm, etc.
    val needs_support: Boolean,
    val confidence: Float,
    val detected_triggers: List<String>
)

@Serializable
data class EmotionalSummaryResponse(
    val status: String,           // ok, no_data
    val current_state: EmotionalState?,
    val session_stats: SessionStats?,
    val trend: EmotionalTrend?,
    val alerts: List<String>
)

@Serializable
data class EmotionalState(
    val valence: Float,
    val arousal: Float,
    val primary_emotion: String?
)

@Serializable
data class SessionStats(
    val messages_count: Int,
    val avg_valence: Float,
    val support_offered: Int
)

@Serializable
data class EmotionalTrend(
    val direction: String,  // improving, declining, stable
    val change: Float
)

// === Briefing ===

@Serializable
data class BriefingRequest(
    val briefing_type: String = "morning", // morning, evening, weekly
    val user_name: String? = null
)

@Serializable
data class BriefingResponse(
    val id: String,
    val briefing_type: String,
    val greeting: String,
    val sections: List<BriefingSection>,
    val total_items: Int,
    val generated_at: String
)

@Serializable
data class BriefingSection(
    val title: String,
    val icon: String,
    val items: List<String>,
    val priority: Int
)

// === Memory ===

@Serializable
data class MemorySearchRequest(
    val query: String,
    val limit: Int = 10
)

@Serializable
data class MemorySearchResponse(
    val facts: List<MemoryFact>,
    val episodes: List<MemoryEpisode>,
    val total: Int
)

@Serializable
data class MemoryFact(
    val key: String,
    val value: String,
    val category: String,
    val confidence: String
)

@Serializable
data class MemoryEpisode(
    val id: String,
    val summary: String,
    val timestamp: String
)

@Serializable
data class RememberRequest(
    val key: String,
    val value: String,
    val category: String? = null,
    val confidence: String? = null
)

@Serializable
data class MemoryStatsResponse(
    val facts_count: Int,
    val episodes_count: Int,
    val habits_count: Int,
    val total_messages: Int,
    val data_points: Int,
    val peak_hours: List<Int>,
    val frequent_topics: List<String>
)

// === Notifications ===

@Serializable
data class NotificationPreferences(
    val push_enabled: Boolean,
    val email_enabled: Boolean,
    val quiet_hours_start: Int,
    val quiet_hours_end: Int,
    val max_daily_notifications: Int,
    val digest_enabled: Boolean,
    val digest_time: Int
)
```

### API Service

```kotlin
// data/remote/IntelligenceApi.kt
package com.pandai.data.remote

import retrofit2.http.*

interface IntelligenceApi {
    
    // === Emotions ===
    
    @POST("intelligence/emotions/analyze")
    suspend fun analyzeEmotion(
        @Header("Authorization") token: String,
        @Body request: EmotionAnalyzeRequest
    ): EmotionAnalyzeResponse
    
    @GET("intelligence/emotions/summary")
    suspend fun getEmotionalSummary(
        @Header("Authorization") token: String
    ): EmotionalSummaryResponse
    
    @GET("intelligence/emotions/techniques")
    suspend fun getTherapeuticTechniques(
        @Header("Authorization") token: String,
        @Query("emotion") emotion: String? = null
    ): TherapeuticTechniquesResponse
    
    // === Briefing ===
    
    @POST("intelligence/briefing")
    suspend fun generateBriefing(
        @Header("Authorization") token: String,
        @Body request: BriefingRequest
    ): BriefingResponse
    
    // === Memory ===
    
    @POST("intelligence/memory/search")
    suspend fun searchMemory(
        @Header("Authorization") token: String,
        @Body request: MemorySearchRequest
    ): MemorySearchResponse
    
    @POST("intelligence/memory/remember")
    suspend fun rememberFact(
        @Header("Authorization") token: String,
        @Body request: RememberRequest
    ): RememberResponse
    
    @POST("intelligence/memory/forget")
    suspend fun forgetFact(
        @Header("Authorization") token: String,
        @Body request: ForgetRequest
    ): ForgetResponse
    
    @GET("intelligence/memory/stats")
    suspend fun getMemoryStats(
        @Header("Authorization") token: String
    ): MemoryStatsResponse
    
    // === Notifications ===
    
    @GET("intelligence/notifications")
    suspend fun getNotifications(
        @Header("Authorization") token: String
    ): List<NotificationItem>
    
    @GET("intelligence/notifications/unread-count")
    suspend fun getUnreadCount(
        @Header("Authorization") token: String
    ): UnreadCountResponse
    
    @GET("intelligence/notifications/preferences")
    suspend fun getNotificationPreferences(
        @Header("Authorization") token: String
    ): NotificationPreferences
    
    @PATCH("intelligence/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Header("Authorization") token: String,
        @Body preferences: NotificationPreferences
    ): NotificationPreferences
}
```

---

## 📊 Dashboard виджеты

### Data Classes

```kotlin
// data/model/Dashboard.kt
package com.pandai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MoodChartResponse(
    val type: String,
    val period_days: Int,
    val data: MoodChartData,
    val visualization: ChartVisualization
)

@Serializable
data class MoodChartData(
    val chart_data: List<MoodDataPoint>,
    val entries_count: Int,
    val average_mood: Float?,
    val best_day: MoodDataPoint?,
    val worst_day: MoodDataPoint?
)

@Serializable
data class MoodDataPoint(
    val date: String,
    val mood_level: Float?,
    val stress_level: Float?,
    val energy_level: Float?,
    val valence: Float?
)

@Serializable
data class ChartVisualization(
    val chart_type: String,
    val x_axis: String,
    val y_axes: List<String>,
    val colors: Map<String, String>
)

@Serializable
data class StreakResponse(
    val type: String,
    val data: StreakData,
    val encouragement: String
)

@Serializable
data class StreakData(
    val current_streak: Int,
    val total_days_active: Int,
    val milestones: List<Milestone>,
    val last_activity: String?
)

@Serializable
data class Milestone(
    val days: Int,
    val achieved: Boolean,
    val emoji: String? = null,
    val progress: Float? = null
)
```

### Compose Widgets

```kotlin
// presentation/dashboard/widgets/StreakWidget.kt
package com.pandai.presentation.dashboard.widgets

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandai.data.model.StreakResponse

@Composable
fun StreakWidget(
    streak: StreakResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Streak count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔥",
                    fontSize = 32.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${streak.data.current_streak}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "дней",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            // Encouragement
            Text(
                text = streak.encouragement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Milestones
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                streak.data.milestones.forEach { milestone ->
                    MilestoneChip(milestone)
                }
            }
            
            // Progress to next milestone
            streak.data.milestones.find { !it.achieved }?.let { next ->
                Spacer(Modifier.height(12.dp))
                Column {
                    Text(
                        text = "До ${next.days} дней",
                        style = MaterialTheme.typography.labelSmall
                    )
                    LinearProgressIndicator(
                        progress = (next.progress ?: 0f) / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun MilestoneChip(milestone: Milestone) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (milestone.achieved)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (milestone.achieved && milestone.emoji != null) {
                Text(milestone.emoji)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = "${milestone.days}д",
                style = MaterialTheme.typography.labelMedium,
                color = if (milestone.achieved)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### Mood Chart с MPAndroidChart

```kotlin
// presentation/dashboard/widgets/MoodChartWidget.kt
package com.pandai.presentation.dashboard.widgets

import android.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.pandai.data.model.MoodChartResponse

@Composable
fun MoodChartWidget(
    data: MoodChartResponse,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Настроение за ${data.period_days} дней",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Среднее", data.data.average_mood?.toString() ?: "—")
                StatItem("Лучший", data.data.best_day?.date?.takeLast(5) ?: "—")
                StatItem("Записей", data.data.entries_count.toString())
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Chart
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupChart(this, data)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

private fun setupChart(chart: LineChart, data: MoodChartResponse) {
    val chartData = data.data.chart_data
    val colors = data.visualization.colors
    
    // Mood line
    val moodEntries = chartData.mapIndexedNotNull { index, point ->
        point.mood_level?.let { Entry(index.toFloat(), it) }
    }
    
    val moodDataSet = LineDataSet(moodEntries, "Настроение").apply {
        color = Color.parseColor(colors["mood_level"] ?: "#4CAF50")
        setCircleColor(color)
        lineWidth = 2f
        circleRadius = 4f
        setDrawValues(false)
    }
    
    // Stress line
    val stressEntries = chartData.mapIndexedNotNull { index, point ->
        point.stress_level?.let { Entry(index.toFloat(), it) }
    }
    
    val stressDataSet = LineDataSet(stressEntries, "Стресс").apply {
        color = Color.parseColor(colors["stress_level"] ?: "#FF5722")
        setCircleColor(color)
        lineWidth = 2f
        circleRadius = 4f
        setDrawValues(false)
    }
    
    chart.apply {
        this.data = LineData(moodDataSet, stressDataSet)
        
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(
                chartData.map { it.date.takeLast(5) }
            )
            granularity = 1f
        }
        
        axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 5f
        }
        
        axisRight.isEnabled = false
        description.isEnabled = false
        legend.isEnabled = true
        
        invalidate()
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## 💡 Предложения по UI/UX

### 1. Главный экран — Morning Briefing

```
┌─────────────────────────────────────────┐
│  ☀️ С добрым утром, Виктор!             │
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ 💚 Настроение      │ 🔥 7 дней      ││
│  │ Вчера: 😊 хорошее  │ подряд!        ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ 📊 Неделя                            ││
│  │ [график настроения]                  ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ 📋 На сегодня                        ││
│  │ • Встреча в 10:00                   ││
│  │ • Выпить витамины                   ││
│  └─────────────────────────────────────┘│
│                                         │
│         [ 💬 Начать чат ]               │
│                                         │
└─────────────────────────────────────────┘
```

**API:**
```kotlin
// При открытии приложения утром
val briefing = api.generateBriefing(BriefingRequest("morning", userName))
val streak = api.getStreak()
val moodChart = api.getMoodChart(days = 7)
```

### 2. Чат с эмоциональным индикатором

```
┌─────────────────────────────────────────┐
│  ← PAND-AI                    😊 🟢     │  ← Mood indicator
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ Привет! Как дела?             [Вы] ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ Привет! Рада тебя видеть! 🤗        ││
│  │ Как прошёл день?               [AI] ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ Устал немного, много работы    [Вы] ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ ●●● AI печатает...                  ││  ← Streaming indicator
│  └─────────────────────────────────────┘│
│                                         │
├─────────────────────────────────────────┤
│  [  Написать сообщение...        ] [➤] │
└─────────────────────────────────────────┘
```

**Когда `needs_support = true`:**

```
┌─────────────────────────────────────────┐
│  ← PAND-AI                    😔 🟡     │  ← Mood changed!
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ 💙 Похоже, тебе сейчас непросто.    ││
│  │                                     ││
│  │ [ Поговорить ] [ Дыхание 🧘 ]       ││  ← Quick actions
│  └─────────────────────────────────────┘│
```

### 3. Settings — Управление памятью

```
┌─────────────────────────────────────────┐
│  ← Настройки                            │
├─────────────────────────────────────────┤
│                                         │
│  🧠 Память AI                           │
│  ─────────────────────────────────────  │
│                                         │
│  Факты обо мне: 15                      │
│  Эпизоды: 42                            │
│  Привычки: 5                            │
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ 👤 Имя: Виктор              [✏️]    ││
│  │ 🎂 Возраст: 30              [✏️]    ││
│  │ 💼 Работа: программист      [✏️]    ││
│  │ 💊 Аллергия: пенициллин     [🗑️]    ││
│  └─────────────────────────────────────┘│
│                                         │
│  [ 🔍 Поиск по памяти ]                 │
│                                         │
│  ─────────────────────────────────────  │
│                                         │
│  🔔 Уведомления                         │
│  ─────────────────────────────────────  │
│                                         │
│  Push уведомления     [═══════●]  ON    │
│  Тихие часы           22:00 - 08:00     │
│  Макс. в день         10                │
│                                         │
└─────────────────────────────────────────┘
```

### 4. Mood Check-in (Push → Activity)

```
┌─────────────────────────────────────────┐
│                                         │
│            💚 Как ты сейчас?            │
│                                         │
│     😢    😔    😐    🙂    😄          │
│     [ ]   [ ]   [ ]   [ ]   [ ]         │
│                                         │
│  ─────────────────────────────────────  │
│                                         │
│  Уровень энергии                        │
│  [●════════════════════════○]  7/10     │
│                                         │
│  Уровень стресса                        │
│  [●════════════════════════○]  3/10     │
│                                         │
│  ─────────────────────────────────────  │
│                                         │
│  Что повлияло? (опционально)            │
│  ┌─────────────────────────────────────┐│
│  │                                     ││
│  └─────────────────────────────────────┘│
│                                         │
│         [ ✓ Сохранить ]                 │
│                                         │
└─────────────────────────────────────────┘
```

---

## 🏗 Архитектура

### Рекомендуемая структура

```
app/
├── data/
│   ├── model/
│   │   ├── Chat.kt
│   │   ├── Intelligence.kt
│   │   ├── Dashboard.kt
│   │   └── Push.kt
│   ├── remote/
│   │   ├── ApiService.kt
│   │   ├── IntelligenceApi.kt
│   │   ├── WebSocketManager.kt
│   │   └── PandAiFcmService.kt
│   └── repository/
│       ├── ChatRepository.kt
│       ├── IntelligenceRepository.kt
│       ├── DashboardRepository.kt
│       └── PushRepository.kt
│
├── domain/
│   └── usecase/
│       ├── SendMessageUseCase.kt
│       ├── GetBriefingUseCase.kt
│       ├── AnalyzeEmotionUseCase.kt
│       └── ...
│
├── presentation/
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   ├── DashboardViewModel.kt
│   │   └── widgets/
│   │       ├── StreakWidget.kt
│   │       ├── MoodChartWidget.kt
│   │       ├── BriefingCard.kt
│   │       └── EmotionalStateWidget.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── MemoryManagementScreen.kt
│   │   └── NotificationSettingsScreen.kt
│   └── mood/
│       ├── MoodCheckInScreen.kt
│       └── MoodHistoryScreen.kt
│
└── di/
    ├── NetworkModule.kt
    ├── RepositoryModule.kt
    └── ViewModelModule.kt
```

### Hilt Modules

```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("X-Platform", "Android")
                    .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideIntelligenceApi(retrofit: Retrofit): IntelligenceApi {
        return retrofit.create(IntelligenceApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideWebSocketManager(): WebSocketManager {
        return WebSocketManager(BuildConfig.WS_BASE_URL)
    }
}
```

---

## ✅ Чек-лист интеграции

### Phase 1: Core (Неделя 1)

- [ ] WebSocket подключение
- [ ] Streaming ответов AI
- [ ] Typing indicator
- [ ] Reconnection logic
- [ ] Emotion update handling

### Phase 2: Intelligence (Неделя 2)

- [ ] Emotion indicator в чате
- [ ] Morning briefing на главном экране
- [ ] Streak widget
- [ ] Mood chart widget

### Phase 3: Push & Settings (Неделя 3)

- [ ] FCM интеграция
- [ ] Notification channels
- [ ] Mood check-in activity
- [ ] Memory management screen
- [ ] Notification preferences

### Phase 4: Polish (Неделя 4)

- [ ] Offline support
- [ ] Error handling
- [ ] Rate limit handling
- [ ] Analytics events
- [ ] Testing

---

## 📞 Контакты

**Backend team:** @backend_team  
**Swagger:** https://api.pand-ai.com/docs  
**Postman:** `docs/Pand-AI-Helper.postman_collection.json`

---

<div align="center">

**v3.1.0** | Android Integration Guide | Январь 2026

</div>
