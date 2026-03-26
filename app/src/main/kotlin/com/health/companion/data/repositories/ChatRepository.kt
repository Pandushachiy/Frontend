package com.health.companion.data.repositories

import com.health.companion.BuildConfig
import com.health.companion.data.local.dao.ChatMessageDao
import com.health.companion.data.local.dao.ConversationDao
import com.health.companion.data.local.database.ChatMessageEntity
import com.health.companion.data.local.database.ConversationEntity
import com.health.companion.data.remote.api.ChatApi
import com.health.companion.data.remote.api.ChatMessageRequest
import com.health.companion.data.remote.api.ChatMessageData
import com.health.companion.data.remote.api.ChatMessageResponse
import com.health.companion.data.remote.api.ConversationDTO
import com.health.companion.data.remote.api.CreateConversationRequest
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.data.remote.api.AgentStep
import com.health.companion.data.remote.api.Citation
import com.health.companion.data.remote.api.EmotionEvent
import com.health.companion.data.remote.api.GeneratedFile
import com.health.companion.data.remote.api.ConfirmationEvent
import com.health.companion.data.remote.api.ProgressEvent
import com.health.companion.data.remote.api.ThinkingChainStep
import com.health.companion.data.remote.api.InChatReminder
import com.health.companion.data.remote.api.ReminderPriority
import com.health.companion.services.WebSocketManager
import com.health.companion.services.WebSocketMessage
import com.health.companion.services.WsEvent
import kotlinx.coroutines.flow.filterIsInstance
import com.health.companion.utils.TokenManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume

interface ChatRepository {
    suspend fun sendMessage(message: String, conversationId: String?): Result<ChatMessageResponse>
    
    /**
     * SSE Streaming - посылает сообщение и получает ответ потоком
     * @param images List of base64 encoded images (backend decides what to do: vision/edit/generate)
     */
    suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        images: List<String>? = null,
        attachmentOnly: Boolean = false,
        onStatus: (String) -> Unit,
        onToken: (String) -> Unit,
        onImage: (url: String, prompt: String) -> Unit,
        onCitations: (List<Citation>) -> Unit,
        onAgentStep: (AgentStep) -> Unit,
        onFile: (GeneratedFile) -> Unit,
        onEmotion: (EmotionEvent) -> Unit = {},
        onReminder: (InChatReminder) -> Unit = {},
        onConversation: (conversationId: String, title: String) -> Unit = { _, _ -> },
        onCanvasUpdate: (action: String, payloadJson: String) -> Unit = { _, _ -> },
        onConfirmation: (ConfirmationEvent) -> Unit = {},
        onProgress: (ProgressEvent) -> Unit = {},
        onThinkingChain: (ThinkingChainStep) -> Unit = {},
        onDone: (messageId: String, fullContent: String, newConversationId: String?, userMessageId: String?, imageUrl: String?) -> Unit,
        onError: (String) -> Unit
    )
    
    fun getConversationMessages(conversationId: String): Flow<List<MessageDTO>>
    fun getLocalConversationsFlow(): Flow<List<ConversationEntity>>
    suspend fun getConversations(): Result<List<ConversationDTO>>
    suspend fun createConversation(title: String? = null): Result<ConversationDTO>
    suspend fun createLocalConversation(title: String? = null): Result<String>
    suspend fun deleteLocalConversation(conversationId: String): Result<Unit>
    suspend fun syncConversationMessages(conversationId: String): Result<List<MessageDTO>>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit>
    suspend fun regenerateTitle(conversationId: String): Result<String>
    suspend fun upsertConversationWithTitle(conversationId: String, title: String)
    fun connectWebSocket(userId: String): Flow<WebSocketMessage>
    /** Emits each time a WS reminder event arrives (app in foreground). Chat UI should sync. */
    val reminderEvents: Flow<WsEvent.ReminderEvent>
    /** Emits when a background task finishes and sends result via WebSocket. */
    val backgroundTaskEvents: Flow<WsEvent.BackgroundTaskResult>
    suspend fun disconnectWebSocket()
    fun disconnectWebSocketSync()
    suspend fun confirmAction(confirmationId: String, approved: Boolean): Result<Unit>
    suspend fun clearAllLocalData()
    
    // ─── Streaming draft ───────────────────────────────────────────────────
    /**
     * Insert or update a streaming draft message in Room during active SSE stream.
     * Called every ~2 seconds while tokens arrive. Survives process death so the
     * partial response is visible to the user when the app restarts.
     */
    suspend fun upsertStreamingDraft(
        conversationId: String,
        messageId: String,
        content: String,
        imageUrl: String? = null,
        filesJson: String? = null
    )

    /** Clear the isStreamingDraft flag (called on onDone / onError finalization). */
    suspend fun clearStreamingDrafts(conversationId: String)

    /** Returns an interrupted streaming draft for the given conversation, if any. */
    suspend fun getStreamingDraft(conversationId: String): com.health.companion.data.local.database.ChatMessageEntity?

    /** Count messages in Room for a conversation (used by background polling). */
    suspend fun getMessageCount(conversationId: String): Int
    /** Fetches message count directly from server — no Room operations, safe for polling. */
    suspend fun fetchServerMessageCount(conversationId: String): Int

    /**
     * Save messages to local DB (called from ViewModel after SSE completes)
     */
    suspend fun saveStreamedMessages(
        conversationId: String,
        userMessage: String,
        userMessageId: String,
        assistantMessageId: String,
        assistantContent: String,
        imageUrl: String? = null,
        userImages: List<String>? = null,
        generatedFiles: List<com.health.companion.data.remote.api.GeneratedFile>? = null,
        oldUserMessageId: String? = null
    )

    /**
     * Save the user message to Room immediately (before AI response is ready).
     * Ensures photos persist even if the app is killed before streaming completes.
     * If [oldMessageId] is provided and differs from [messageId], the old record is deleted first.
     */
    suspend fun saveUserMessageNow(
        conversationId: String,
        messageId: String,
        content: String,
        images: List<String>?,
        oldMessageId: String? = null
    )
}

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient // Injected client with TokenAuthenticator
) : ChatRepository {
    
    // SSE streaming client — CLEAN: no interceptors, HTTP/1.1 only
    // Auth token is added manually in the request header.
    // Base client interceptors cause problems:
    //   - HttpLoggingInterceptor.BODY buffers entire response
    //   - Anonymous interceptor adds "Accept: application/json" which breaks SSE
    // HTTP/2 multiplexes streams and CAN BUFFER SSE events!
    // Force HTTP/1.1 for real-time SSE delivery.
    private val streamClient = OkHttpClient.Builder()
        .protocols(listOf(okhttp3.Protocol.HTTP_1_1)) // ← CRITICAL: HTTP/2 buffers SSE!
        .readTimeout(0, TimeUnit.MILLISECONDS)   // SSE = infinite stream
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        // Zero interceptors! Auth via manual header in request
        .build()
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    /**
     * Try to refresh access token using refresh token.
     * Returns new access token or null if refresh failed.
     */
    private suspend fun tryRefreshToken(): String? {
        var refreshToken = tokenManager.getRefreshToken()
        if (refreshToken == null) {
            tokenManager.reloadFromStore()
            refreshToken = tokenManager.getRefreshToken()
        }
        if (refreshToken == null) return null
        
        return try {
            val requestBody = Json.encodeToString(
                com.health.companion.data.remote.api.RefreshTokenRequest.serializer(),
                com.health.companion.data.remote.api.RefreshTokenRequest(refresh_token = refreshToken)
            )
            
            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/auth/refresh")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = withContext(Dispatchers.IO) {
                streamClient.newCall(request).execute()
            }
            
            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    val tokens = Json.decodeFromString<com.health.companion.data.remote.api.TokenResponse>(body)
                    tokenManager.updateAccessToken(tokens.access_token)
                    tokens.refresh_token?.let { tokenManager.updateRefreshToken(it) }
                    tokens.access_token
                }
            } else {
                Timber.w("Token refresh failed: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Token refresh error")
            null
        }
    }
    
    override suspend fun sendMessage(
        message: String,
        conversationId: String?
    ): Result<ChatMessageResponse> {
        return try {
            val response = chatApi.sendMessage(
                ChatMessageRequest(
                    message = message,
                    conversation_id = conversationId
                )
            )
            
            // Ensure conversation exists in local DB
            val convId = response.getConversationId().ifEmpty { conversationId ?: UUID.randomUUID().toString() }
            ensureConversationExists(convId, suggestTitleFromMessage(message))
            
            // Save user message to local database
            chatMessageDao.insert(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = convId,
                    content = message,
                    role = "user"
                )
            )
            
            // Save assistant response to local database
            chatMessageDao.insert(
                ChatMessageEntity(
                    id = response.getMessageId().ifEmpty { UUID.randomUUID().toString() },
                    conversationId = convId,
                    content = response.getMessageContent(),
                    role = response.message?.role ?: "assistant",
                    agentName = response.getAgentName(),
                    confidence = response.confidence?.toFloat(),
                    provider = response.getProviderResolved(),
                    providerColor = response.getProviderColorResolved(),
                    modelUsed = response.getModelUsedResolved()
                )
            )
            conversationDao.updateUpdatedAt(convId, System.currentTimeMillis())
            
            Timber.d("Message sent successfully, conversation: ${response.conversation_id}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error: ${e.code()}")
            if (e.code() == 404) {
                return generateOfflineResponse(message, conversationId, "Backend не найден (404). Убедитесь что сервер запущен.")
            }
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: ConnectException) {
            Timber.e(e, "Connection error")
            generateOfflineResponse(message, conversationId, "Не удалось подключиться к серверу.")
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Timeout error")
            generateOfflineResponse(message, conversationId, "Превышено время ожидания ответа.")
        } catch (e: UnknownHostException) {
            Timber.e(e, "Unknown host")
            generateOfflineResponse(message, conversationId, "Сервер недоступен.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            generateOfflineResponse(message, conversationId, "Ошибка: ${e.localizedMessage}")
        }
    }
    
    private suspend fun generateOfflineResponse(
        message: String,
        conversationId: String?,
        errorMessage: String
    ): Result<ChatMessageResponse> {
        val newConversationId = conversationId ?: UUID.randomUUID().toString()
        
        ensureConversationExists(newConversationId, suggestTitleFromMessage(message))
        
        // Save user message locally
        chatMessageDao.insert(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = newConversationId,
                content = message,
                role = "user"
            )
        )
        
        val offlineContent = """
            ⚠️ **Режим офлайн**
            
            $errorMessage
            
            Ваше сообщение сохранено локально.
        """.trimIndent()
        
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis().toString()
        
        chatMessageDao.insert(
            ChatMessageEntity(
                id = messageId,
                conversationId = newConversationId,
                content = offlineContent,
                role = "assistant",
                agentName = "offline",
                confidence = null,
                provider = "offline"
            )
        )
        conversationDao.updateUpdatedAt(newConversationId, System.currentTimeMillis())
        
        return Result.success(
            ChatMessageResponse(
                message = ChatMessageData(
                    id = messageId,
                    content = offlineContent,
                    role = "assistant",
                    conversation_id = newConversationId,
                    created_at = timestamp,
                    agent_name = "offline"
                ),
                conversation_id = newConversationId
            )
        )
    }
    
    /**
     * SSE Streaming implementation using OkHttp EventSource
     * Automatically retries once with refreshed token on 401
     */
    override suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        images: List<String>?,
        attachmentOnly: Boolean,
        onStatus: (String) -> Unit,
        onToken: (String) -> Unit,
        onImage: (url: String, prompt: String) -> Unit,
        onCitations: (List<Citation>) -> Unit,
        onAgentStep: (AgentStep) -> Unit,
        onFile: (GeneratedFile) -> Unit,
        onEmotion: (EmotionEvent) -> Unit,
        onReminder: (InChatReminder) -> Unit,
        onConversation: (conversationId: String, title: String) -> Unit,
        onCanvasUpdate: (action: String, payloadJson: String) -> Unit,
        onConfirmation: (ConfirmationEvent) -> Unit,
        onProgress: (ProgressEvent) -> Unit,
        onThinkingChain: (ThinkingChainStep) -> Unit,
        onDone: (messageId: String, fullContent: String, newConversationId: String?, userMessageId: String?, imageUrl: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        sendMessageStreamInternal(
            message = message,
            conversationId = conversationId,
            images = images,
            attachmentOnly = attachmentOnly,
            onStatus = onStatus,
            onToken = onToken,
            onImage = onImage,
            onCitations = onCitations,
            onAgentStep = onAgentStep,
            onFile = onFile,
            onEmotion = onEmotion,
            onReminder = onReminder,
            onConversation = onConversation,
            onCanvasUpdate = onCanvasUpdate,
            onConfirmation = onConfirmation,
            onProgress = onProgress,
            onThinkingChain = onThinkingChain,
            onDone = onDone,
            onError = onError,
            isRetry = false
        )
    }
    
    /**
     * Internal SSE implementation with retry flag
     */
    private suspend fun sendMessageStreamInternal(
        message: String,
        conversationId: String?,
        images: List<String>?,
        attachmentOnly: Boolean = false,
        onStatus: (String) -> Unit,
        onToken: (String) -> Unit,
        onImage: (url: String, prompt: String) -> Unit,
        onCitations: (List<Citation>) -> Unit,
        onAgentStep: (AgentStep) -> Unit,
        onFile: (GeneratedFile) -> Unit,
        onEmotion: (EmotionEvent) -> Unit,
        onReminder: (InChatReminder) -> Unit,
        onConversation: (conversationId: String, title: String) -> Unit,
        onCanvasUpdate: (action: String, payloadJson: String) -> Unit,
        onConfirmation: (ConfirmationEvent) -> Unit,
        onProgress: (ProgressEvent) -> Unit = {},
        onThinkingChain: (ThinkingChainStep) -> Unit = {},
        onDone: (messageId: String, fullContent: String, newConversationId: String?, userMessageId: String?, imageUrl: String?) -> Unit,
        onError: (String) -> Unit,
        isRetry: Boolean
    ) {
        var token = tokenManager.getAccessToken()
        if (token == null) {
            tokenManager.reloadFromStore()
            token = tokenManager.getAccessToken()
        }
        if (token == null) {
            token = tryRefreshToken()
            if (token == null) {
                onError("Требуется авторизация")
                return
            }
        }
        
        val body = JSONObject().apply {
            put("message", message)
            put("timezone", java.util.TimeZone.getDefault().id)
            conversationId?.let { put("conversation_id", it) }
            if (attachmentOnly) put("attachment_only", true)
            if (!images.isNullOrEmpty()) {
                val imagesArray = org.json.JSONArray()
                images.forEach { img -> imagesArray.put(img) }
                put("images", imagesArray)
            }
        }.toString()
        
        val url = "${BuildConfig.API_BASE_URL}/chat/v3/send/stream"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Accept-Encoding", "identity")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        
        // Track if we got 401 and need retry
        var got401 = false
        
        suspendCancellableCoroutine { continuation ->
            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {}
                
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    try {
                        val json = JSONObject(data)
                        val eventType = json.optString("type")
                        
                        // RAW log — before any UI logic — to verify what backend actually sends
                        android.util.Log.d("SSE_RAW", "← type='$eventType' data='${data.take(120)}'")
                        
                        when (eventType) {
                            "status" -> {
                                val message = json.optString("message").takeIf { it.isNotBlank() }
                                val status = json.optString("status").takeIf { it.isNotBlank() }
                                val display = message ?: status ?: ""
                                android.util.Log.w("SSE_PERSIST", "STATUS msg='$message' status='$status' → '$display'")
                                onStatus(display)
                            }
                            "token", "content" -> {
                                val content = json.optString("content")
                                if (content.contains("{\"error\"")) {
                                    onError("❌ Ошибка генерации")
                                    eventSource.cancel()
                                    if (continuation.isActive) continuation.resume(Unit)
                                } else {
                                    onToken(content)
                                }
                            }
                            "agent_step" -> {
                                val step = json.optString("step", "")
                                val detail = json.optString("detail", "")
                                if (detail.isNotBlank()) onAgentStep(AgentStep(step, detail))
                            }
                            "emotion" -> {
                                val emotionName = json.optString("emotion", "neutral")
                                val confidence = json.optDouble("confidence", 0.5).toFloat()
                                val tone = json.optString("tone", "")
                                onEmotion(EmotionEvent(emotionName, confidence, tone))
                            }
                            "reminder" -> {
                                val reminderId = json.optString("reminder_id", "")
                                val title = json.optString("title", "")
                                val priority = json.optString("priority", "medium")
                                val isRecurring = json.optBoolean("is_recurring", false)
                                if (title.isNotBlank()) {
                                    onReminder(InChatReminder(
                                        id = reminderId,
                                        title = title,
                                        priority = ReminderPriority.from(priority),
                                        isRecurring = isRecurring
                                    ))
                                }
                            }
                            "image", "image_url" -> {
                                val url = json.optString("url")
                                if (url.isNotBlank()) onImage(url, json.optString("prompt"))
                            }
                            "file_ready" -> {
                                val fileUrl = json.optString("url", "")
                                val fileName = json.optString("name", "file")
                                val fileIcon = json.optString("icon", "").takeIf { it.isNotBlank() }
                                android.util.Log.d("SSE_DBG", "FILE_READY url='$fileUrl' name='$fileName'")
                                if (fileUrl.isNotBlank()) onFile(GeneratedFile(fileUrl, fileName, fileIcon))
                            }
                            "file_url" -> {
                                val fileUrl = json.optString("url", "")
                                val fileName = json.optString("name", "file")
                                val fileIcon = json.optString("icon", "").takeIf { it.isNotBlank() }
                                android.util.Log.d("SSE_DBG", "FILE_URL url='$fileUrl' name='$fileName'")
                                if (fileUrl.isNotBlank()) onFile(GeneratedFile(fileUrl, fileName, fileIcon))
                            }
                            "citations" -> {
                                val arr = json.optJSONArray("sources") ?: return
                                val list = mutableListOf<Citation>()
                                for (i in 0 until arr.length()) {
                                    val s = arr.getJSONObject(i)
                                    list.add(Citation(s.optInt("index", i+1), s.optString("url",""), s.optString("domain",""), s.optString("title","")))
                                }
                                onCitations(list)
                            }
                            "done" -> {
                                val messageId = json.optString("messageId").takeIf { it.isNotBlank() }
                                    ?: json.optString("message_id")
                                val fullContent = json.optString("full_content")
                                val newConvId = (json.optString("conversationId").takeIf { it.isNotBlank() }
                                    ?: json.optString("conversation_id")).takeIf { it.isNotBlank() }
                                val doneImageUrl = (json.optString("imageUrl").takeIf { it.isNotBlank() }
                                    ?: json.optString("image_url")).takeIf { it.isNotBlank() }
                                val serverUserMsgId = (json.optString("userMessageId").takeIf { it.isNotBlank() }
                                    ?: json.optString("user_message_id")).takeIf { it.isNotBlank() }
                                onDone(messageId, fullContent, newConvId, serverUserMsgId, doneImageUrl)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            "error" -> {
                                val msg = json.optJSONObject("error")?.optString("message")
                                    ?: json.optString("message") ?: "Ошибка"
                                onError(msg)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            "conversation" -> {
                                val convId = (json.optString("conversationId").takeIf { it.isNotBlank() }
                                    ?: json.optString("conversation_id")).takeIf { it.isNotBlank() }
                                val title = json.optString("title", "")
                                if (convId != null) onConversation(convId, title)
                            }
                            "canvas_update" -> {
                                val action = json.optString("action", "")
                                val payloadStr = json.optJSONObject("payload")?.toString() ?: "{}"
                                if (action.isNotBlank()) onCanvasUpdate(action, payloadStr)
                            }
                            "confirmation_pending" -> {
                                val confirmId = json.optString("confirmation_id", "")
                                val tool = json.optString("tool", "")
                                val preview = json.optString("preview", "")
                                if (confirmId.isNotBlank()) {
                                    onConfirmation(ConfirmationEvent(confirmId, tool, preview))
                                }
                            }
                            "image_generated" -> {
                                val url = json.optString("url")
                                if (url.isNotBlank()) onImage(url, json.optString("name", ""))
                            }
                            "thinking_chain" -> {
                                val phase = json.optString("phase", "analyze")
                                val step = json.optInt("step", 0)
                                val detail = json.optString("detail", "")
                                val plan = json.optString("plan").takeIf { it.isNotBlank() }
                                val tool = json.optString("tool").takeIf { it.isNotBlank() }
                                val elapsedMs = json.optLong("elapsed_ms", 0)
                                if (detail.isNotBlank()) {
                                    onThinkingChain(ThinkingChainStep(phase, step, detail, plan, tool, elapsedMs))
                                }
                            }
                            "progress" -> {
                                val percent = json.optInt("percent", 0)
                                val step = if (json.has("step")) json.optInt("step") else null
                                val totalSteps = if (json.has("total_steps")) json.optInt("total_steps") else null
                                val stepLabel = json.optString("step_label").takeIf { it.isNotBlank() }
                                onProgress(ProgressEvent(percent, step, totalSteps, stepLabel))
                            }
                            "agent_done" -> {
                                val result = json.optJSONObject("result")
                                val messageId = result?.optString("message_id") ?: ""
                                val fullContent = result?.optString("final_response") ?: ""
                                val newConvId = result?.optString("conversation_id")?.takeIf { it.isNotBlank() }
                                val serverUserMsgId = result?.optString("user_message_id")?.takeIf { it.isNotBlank() }
                                val agentImageUrl = (result?.optString("image_url") ?: "").takeIf { it.isNotBlank() }
                                val wsFiles = result?.optJSONArray("workspace_files")
                                if (wsFiles != null) {
                                    for (i in 0 until wsFiles.length()) {
                                        val wf = wsFiles.getJSONObject(i)
                                        val fileUrl = wf.optString("path", "")
                                        val fileName = wf.optString("name", "file")
                                        val fileIcon = when (wf.optString("type", "")) {
                                            "excel" -> "📊"
                                            "word" -> "📝"
                                            "pdf" -> "📄"
                                            "image" -> "🖼️"
                                            else -> "📁"
                                        }
                                        if (fileUrl.isNotBlank()) onFile(GeneratedFile(fileUrl, fileName, fileIcon))
                                    }
                                }
                                onProgress(ProgressEvent(100, 0, 0))
                                onDone(messageId, fullContent, newConvId, serverUserMsgId, agentImageUrl)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            "summary_updated" -> {
                                val convId = (json.optString("conversationId").takeIf { it.isNotBlank() }
                                    ?: json.optString("conversation_id")).takeIf { it.isNotBlank() }
                                val summaryText = json.optString("summary").takeIf { it.isNotBlank() }
                                if (convId != null && summaryText != null) {
                                    try {
                                        conversationDao.updateSummary(convId, summaryText)
                                    } catch (e: Exception) {
                                        Timber.w(e, "Failed to update summary")
                                    }
                                }
                            }
                            "formatted" -> { /* ignore */ }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "SSE parse error: $data")
                    }
                }
                
                override fun onClosed(eventSource: EventSource) {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                
                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    if (t?.message?.contains("Socket closed") == true) return
                    // eventSource.cancel() after onDone triggers onFailure("Canceled")
                    if (t is java.io.IOException && t.message?.contains("cancel", true) == true) return
                    
                    // Check for 401 - set flag for retry after continuation resumes
                    if (response?.code == 401 && !isRetry) {
                        got401 = true
                        if (continuation.isActive) continuation.resume(Unit)
                        return
                    }
                    
                    val errorMsg = when {
                        t is SocketTimeoutException -> "Превышено время ожидания"
                        response?.code == 401 -> "Требуется авторизация"
                        response?.code == 404 -> "Endpoint не найден"
                        response?.code == 429 -> "⏳ Слишком много запросов"
                        response?.code == 500 -> "Ошибка сервера"
                        else -> t?.localizedMessage ?: "Ошибка подключения"
                    }
                    onError(errorMsg)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            
            val eventSource = EventSources.createFactory(streamClient)
                .newEventSource(request, listener)
            
            continuation.invokeOnCancellation { eventSource.cancel() }
        }
        
        // After SSE completes, check if we need to retry with refreshed token
        if (got401 && !isRetry) {
            val newToken = tryRefreshToken()
            if (newToken != null) {
                    sendMessageStreamInternal(
                    message = message,
                    conversationId = conversationId,
                    images = images,
                    attachmentOnly = attachmentOnly,
                    onStatus = onStatus,
                    onToken = onToken,
                    onImage = onImage,
                    onCitations = onCitations,
                    onAgentStep = onAgentStep,
                    onFile = onFile,
                    onEmotion = onEmotion,
                    onReminder = onReminder,
                    onConversation = onConversation,
                    onCanvasUpdate = onCanvasUpdate,
                    onConfirmation = onConfirmation,
                    onProgress = onProgress,
                    onThinkingChain = onThinkingChain,
                    onDone = onDone,
                    onError = onError,
                    isRetry = true
                )
            } else {
                onError("Требуется авторизация")
            }
        }
    }
    
    override fun getConversationMessages(conversationId: String): Flow<List<MessageDTO>> {
        return chatMessageDao.getMessagesFlow(conversationId).map { entities ->
            entities.map { entity ->
                MessageDTO(
                    id = entity.id,
                    content = entity.content,
                    role = entity.role,
                    agentName = entity.agentName,
                    provider = entity.provider,
                    providerColor = entity.providerColor,
                    modelUsed = entity.modelUsed,
                    createdAt = entity.createdAt.toString(),
                    imageUrl = entity.imageUrl,
                    images = entity.images?.let {
                        try { Json.decodeFromString<List<String>>(it) } catch (e: Exception) { null }
                    },
                    files = entity.filesJson?.let {
                        try { Json.decodeFromString<List<com.health.companion.data.remote.api.GeneratedFile>>(it) } catch (e: Exception) { null }
                    }
                )
            }
        }
    }

    override fun getLocalConversationsFlow(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversationsFlow()
    }

    override suspend fun getConversations(): Result<List<ConversationDTO>> {
        return try {
            val allItems = mutableListOf<ConversationDTO>()
            var currentPage = 1
            var totalPages = 1
            
            do {
                val response = chatApi.getConversations(size = 50, page = currentPage)
                allItems.addAll(response.items)
                totalPages = response.pages
                currentPage++
            } while (currentPage <= totalPages && currentPage <= 10)
            
            val now = System.currentTimeMillis()
            val serverIds = allItems.map { it.id }.toSet()
            
            // Get current local IDs
            val localIds = conversationDao.getAllConversations().map { it.id }.toSet()
            
            // Delete local conversations that don't exist on server
            val toDelete = localIds - serverIds
            if (toDelete.isNotEmpty()) {
                Timber.w("PHOTO_DEBUG getConversations: DELETING ${toDelete.size} local conversations NOT on server: $toDelete (CASCADE will delete their messages!)")
            }
            toDelete.forEach { id -> conversationDao.deleteById(id) }
            
            // Upsert server conversations
            allItems.forEach { dto ->
                // Parse ISO datetime from backend (camelCase)
                val createdMs = dto.createdAt?.let { parseIsoDateTime(it) } ?: now
                val updatedMs = dto.updatedAt?.let { parseIsoDateTime(it) } ?: createdMs
                val lastMsgMs = dto.lastMessageAt?.let { parseIsoDateTime(it) }
                
                conversationDao.upsert(
                    ConversationEntity(
                        id = dto.id,
                        title = dto.title.ifBlank { "Новый чат" },
                        createdAt = createdMs,
                        updatedAt = updatedMs,
                        lastMessageAt = lastMsgMs ?: updatedMs,  // fallback к updatedAt
                        isArchived = dto.isArchived,
                        isPinned = dto.isPinned,
                        summary = dto.summary
                    )
                )
            }
            Timber.d("Synced ${allItems.size} conversations, removed ${toDelete.size} stale")
            Result.success(allItems)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get conversations from server")
            Result.failure(e)
        }
    }

    override suspend fun createConversation(title: String?): Result<ConversationDTO> {
        return try {
            val response = chatApi.createConversation(
                CreateConversationRequest(title = title)
            )
            val now = System.currentTimeMillis()
            conversationDao.upsert(
                ConversationEntity(
                    id = response.id,
                    title = response.title.ifBlank { title ?: "Новый чат" },
                    createdAt = now,
                    updatedAt = now
                )
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createLocalConversation(title: String?): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            conversationDao.upsert(
                ConversationEntity(
                    id = id,
                    title = title?.ifBlank { "Новый чат" } ?: "Новый чат",
                    createdAt = now,
                    updatedAt = now
                )
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncConversationMessages(conversationId: String): Result<List<MessageDTO>> {
        return try {
            val response = chatApi.getMessages(conversationId)
            
            // Load existing local messages to preserve imageUrl/images
            // that the backend may not return for older messages
            val localMessages = chatMessageDao.getMessages(conversationId)
            val localMap = localMessages.associateBy { it.id }
            val serverIdSet = response.map { it.id }.toHashSet()

            val localWithImages = localMessages.filter { !it.images.isNullOrEmpty() }
            Timber.d("sync($conversationId): ${localMessages.size} local msgs (${localWithImages.size} with images), ${response.size} server msgs")
            localWithImages.forEach { m ->
                Timber.d("sync: local img msg=${m.id}, role=${m.role}, images=${m.images?.take(100)}, inServer=${m.id in serverIdSet}")
            }

            // Orphans = local messages whose IDs are not in the server response.
            // Some of these are "early-saved" user messages with client UUIDs that were
            // saved before the server assigned a canonical ID (user exited mid-stream).
            // We try to match each orphan-with-images to a server message by content + timestamp.
            val orphansWithImages = localMessages.filter { it.id !in serverIdSet && !it.images.isNullOrEmpty() }

            // Build a map: serverMsgId → orphan local record that likely corresponds to it
            val orphanMatchMap = mutableMapOf<String, ChatMessageEntity>()
            val matchedOrphanIds = mutableSetOf<String>()
            for (msg in response) {
                if (orphansWithImages.isEmpty()) break
                val createdMs = msg.createdAt?.let { it.toLongOrNull() ?: parseIsoDateTime(it) }
                    ?: System.currentTimeMillis()
                // Match: same role + same content + timestamps within 5 minutes
                val match = orphansWithImages.firstOrNull { orphan ->
                    orphan.id !in matchedOrphanIds &&
                    orphan.role == msg.role &&
                    orphan.content == msg.content &&
                    (createdMs == 0L || kotlin.math.abs(orphan.createdAt - createdMs) < 300_000L)
                }
                if (match != null) {
                    orphanMatchMap[msg.id] = match
                    matchedOrphanIds.add(match.id)
                }
            }

            val entities = response.map { msg ->
                val createdMs = msg.createdAt?.let { 
                    it.toLongOrNull() ?: parseIsoDateTime(it) 
                } ?: System.currentTimeMillis()
                
                val local = localMap[msg.id]
                // Also check if an orphan was matched to this server message (ID mismatch case)
                val orphanMatch = orphanMatchMap[msg.id]

                // Resolve images: prefer permanent local file:// paths (never expire).
                // 1. Local record with same ID has file:// → use it
                // 2. Orphan matched by content has file:// → use it (client UUID migrated to server UUID)
                // 3. Server returned HTTP URLs → use them (may require auth, may expire)
                // 4. Fallback to whatever local has
                val resolvedImages = when {
                    !local?.images.isNullOrEmpty() && local!!.images!!.contains("file://") -> local.images
                    !orphanMatch?.images.isNullOrEmpty() && orphanMatch!!.images!!.contains("file://") -> orphanMatch.images
                    msg.images?.isNotEmpty() == true -> Json.encodeToString(msg.images)
                    else -> local?.images
                }

                val resolvedFilesJson = when {
                    local?.filesJson != null -> local.filesJson
                    msg.files?.isNotEmpty() == true -> try { Json.encodeToString(msg.files) } catch (e: Exception) { null }
                    else -> null
                }

                val resolvedImageUrl = msg.imageUrl ?: local?.imageUrl
                val cleanContent = if (resolvedImageUrl != null && msg.role == "assistant") {
                    msg.content.replace(Regex("""!\[([^\]]*)\]\(([^)]+)\)"""), "").trim()
                } else {
                    msg.content
                }

                ChatMessageEntity(
                    id = msg.id,
                    conversationId = conversationId,
                    content = cleanContent,
                    role = msg.role,
                    agentName = msg.agentName,
                    provider = msg.provider,
                    providerColor = msg.providerColor,
                    modelUsed = msg.modelUsed,
                    createdAt = createdMs,
                    imageUrl = resolvedImageUrl,
                    images = resolvedImages,
                    filesJson = resolvedFilesJson
                )
            }
            
            // Delete orphans, BUT:
            // - Matched orphans are now superseded by the server record → delete them (avoid duplicates)
            // - Unmatched orphans with images are kept (cannot determine their server equivalent yet)
            val unmatchedProtectedIds = orphansWithImages
                .filter { it.id !in matchedOrphanIds }
                .map { it.id }

            entities.filter { !it.images.isNullOrEmpty() }.forEach { e ->
                Timber.d("sync: resolved msg=${e.id}, role=${e.role}, images=${e.images?.take(100)}")
            }
            Timber.d("sync: orphans=${orphansWithImages.size}, matched=${matchedOrphanIds.size}, protected=${unmatchedProtectedIds.size}")

            // Не удаляем свежие сообщения (< 2 мин) — сервер мог не проиндексировать
            val recentProtectedIds = localMessages
                .filter { it.id !in serverIdSet && System.currentTimeMillis() - it.createdAt < 120_000 }
                .map { it.id }
            if (serverIdSet.isNotEmpty()) {
                chatMessageDao.deleteOrphans(conversationId, serverIdSet.toList() + unmatchedProtectedIds + recentProtectedIds)
            }
            // Remove matched orphans (their data was merged into the server record)
            matchedOrphanIds.forEach { orphanId -> chatMessageDao.deleteById(orphanId) }
            
            chatMessageDao.insertAll(entities)
            val lastMessageTime = entities.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis()
            conversationDao.updateUpdatedAt(conversationId, lastMessageTime)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteLocalConversation(conversationId: String): Result<Unit> {
        return try {
            chatMessageDao.deleteByConversation(conversationId)
            conversationDao.deleteById(conversationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            chatApi.deleteConversation(conversationId)
            chatMessageDao.deleteByConversation(conversationId)
            conversationDao.deleteById(conversationId)
            Timber.d("Conversation deleted: $conversationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete conversation")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            // Delete locally
            chatMessageDao.deleteById(messageId)
            // Try to delete on backend (mark as excluded from context)
            try {
                chatApi.deleteMessage(messageId)
            } catch (e: Exception) {
                // Backend might not support this yet - that's ok
                Timber.w(e, "Backend delete message failed, local delete succeeded")
            }
            Timber.d("Message deleted: $messageId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete message")
            Result.failure(e)
        }
    }
    
    /**
     * Регенерация названия сессии через LLM на бэкенде
     * Автоматически анализирует сообщения и создаёт осмысленное название
     */
    override suspend fun regenerateTitle(conversationId: String): Result<String> {
        return try {
            val response = chatApi.regenerateTitle(conversationId)
            val newTitle = response.title
            
            // Update local DB
            conversationDao.updateTitle(conversationId, newTitle)
            
            Timber.d("Title regenerated for $conversationId: $newTitle")
            Result.success(newTitle)
        } catch (e: Exception) {
            Timber.e(e, "Failed to regenerate title")
            Result.failure(e)
        }
    }
    
    override fun connectWebSocket(userId: String): Flow<WebSocketMessage> {
        return webSocketManager.connect(userId)
    }

    override val reminderEvents: Flow<WsEvent.ReminderEvent> =
        webSocketManager.events.filterIsInstance()

    override val backgroundTaskEvents: Flow<WsEvent.BackgroundTaskResult> =
        webSocketManager.events.filterIsInstance()
    
    override suspend fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }

    override fun disconnectWebSocketSync() {
        webSocketManager.disconnect()
    }
    
    override suspend fun confirmAction(confirmationId: String, approved: Boolean): Result<Unit> {
        return try {
            chatApi.confirmAction(
                confirmationId,
                com.health.companion.data.remote.api.ConfirmActionRequest(approved)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to confirm action $confirmationId")
            Result.failure(e)
        }
    }
    
    override suspend fun clearAllLocalData() {
        chatMessageDao.deleteAll()
        conversationDao.deleteAll()
        Timber.d("All local chat data cleared")
    }
    
    /**
     * Save streamed messages to local DB (called from ViewModel after SSE completes)
     */
    override suspend fun saveStreamedMessages(
        conversationId: String,
        userMessage: String,
        userMessageId: String,
        assistantMessageId: String,
        assistantContent: String,
        imageUrl: String?,
        userImages: List<String>?,
        generatedFiles: List<com.health.companion.data.remote.api.GeneratedFile>?,
        oldUserMessageId: String?
    ) {
        try {
            ensureConversationExists(conversationId, suggestTitleFromMessage(userMessage))

            // Пользовательское сообщение уже сохранено через saveUserMessageNow().
            // Если сервер присвоил новый ID — удаляем старую запись, REPLACE вставит новую.
            if (oldUserMessageId != null && oldUserMessageId != userMessageId) {
                chatMessageDao.deleteById(oldUserMessageId)
            }
            // Upsert: если запись с userMessageId уже есть (REPLACE) — обновит; нет — вставит.
            chatMessageDao.insert(
                ChatMessageEntity(
                    id = userMessageId,
                    conversationId = conversationId,
                    content = userMessage,
                    role = "user",
                    images = userImages?.let { Json.encodeToString(it) }
                )
            )
            
            // Save assistant message with accumulated content, image URL, and generated files
            val msgTimestamp = System.currentTimeMillis()
            val filesJsonStr = generatedFiles?.takeIf { it.isNotEmpty() }?.let {
                try { Json.encodeToString(it) } catch (e: Exception) { null }
            }
            chatMessageDao.insert(
                ChatMessageEntity(
                    id = assistantMessageId.ifEmpty { UUID.randomUUID().toString() },
                    conversationId = conversationId,
                    content = assistantContent,
                    role = "assistant",
                    imageUrl = imageUrl,
                    createdAt = msgTimestamp,
                    filesJson = filesJsonStr
                )
            )
            
            // Обновляем время диалога временем последнего сообщения
            conversationDao.updateUpdatedAt(conversationId, msgTimestamp)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save streamed messages")
        }
    }
    
    override suspend fun saveUserMessageNow(
        conversationId: String,
        messageId: String,
        content: String,
        images: List<String>?,
        oldMessageId: String?
    ) {
        try {
            ensureConversationExists(conversationId)
            if (oldMessageId != null && oldMessageId != messageId) {
                Timber.d("PHOTO_DEBUG saveUserMessageNow: replacing old=$oldMessageId with new=$messageId")
                chatMessageDao.deleteById(oldMessageId)
            }
            val imagesJson = images?.let { Json.encodeToString(it) }
            Timber.d("PHOTO_DEBUG saveUserMessageNow: conv=$conversationId, msg=$messageId, imagesJson=${imagesJson?.take(120)}")
            chatMessageDao.insert(
                ChatMessageEntity(
                    id = messageId,
                    conversationId = conversationId,
                    content = content,
                    role = "user",
                    images = imagesJson
                )
            )
            // Verify it was actually saved
            val saved = chatMessageDao.getMessages(conversationId).find { it.id == messageId }
            Timber.d("PHOTO_DEBUG saveUserMessageNow: VERIFY saved=${saved != null}, savedImages=${saved?.images?.take(80)}")
        } catch (e: Exception) {
            Timber.e(e, "PHOTO_DEBUG saveUserMessageNow: FAILED for $messageId")
        }
    }

    override suspend fun upsertStreamingDraft(
        conversationId: String,
        messageId: String,
        content: String,
        imageUrl: String?,
        filesJson: String?
    ) {
        try {
            ensureConversationExists(conversationId)
            val existing = chatMessageDao.getMessages(conversationId).find { it.id == messageId }
            if (existing == null) {
                chatMessageDao.insert(
                    ChatMessageEntity(
                        id = messageId,
                        conversationId = conversationId,
                        content = content,
                        role = "assistant",
                        imageUrl = imageUrl,
                        filesJson = filesJson,
                        isStreamingDraft = true
                    )
                )
            } else {
                chatMessageDao.updateStreamingContent(messageId, content, imageUrl, filesJson)
            }
        } catch (e: Exception) {
            Timber.w(e, "upsertStreamingDraft failed for $messageId")
        }
    }

    override suspend fun clearStreamingDrafts(conversationId: String) {
        try {
            chatMessageDao.clearStreamingDrafts(conversationId)
        } catch (e: Exception) {
            Timber.w(e, "clearStreamingDrafts failed for $conversationId")
        }
    }

    override suspend fun getStreamingDraft(conversationId: String): com.health.companion.data.local.database.ChatMessageEntity? {
        return try {
            chatMessageDao.getStreamingDraft(conversationId)
        } catch (e: Exception) {
            Timber.w(e, "getStreamingDraft failed for $conversationId")
            null
        }
    }

    override suspend fun getMessageCount(conversationId: String): Int {
        return try {
            chatMessageDao.getMessages(conversationId).size
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun fetchServerMessageCount(conversationId: String): Int {
        return try {
            chatApi.getMessages(conversationId).size
        } catch (e: Exception) {
            android.util.Log.w("SSE_PERSIST", "fetchServerMessageCount FAILED: ${e.message}")
            -1
        }
    }

    override suspend fun upsertConversationWithTitle(conversationId: String, title: String) {
        val existing = conversationDao.getConversationById(conversationId)
        if (existing == null) {
            conversationDao.upsert(
                ConversationEntity(
                    id = conversationId,
                    title = title.ifBlank { "Новый чат" }
                )
            )
        } else if (title.isNotBlank()) {
            conversationDao.updateTitle(conversationId, title)
        }
    }

    private suspend fun ensureConversationExists(conversationId: String, title: String? = null) {
        val existing = conversationDao.getConversationById(conversationId)
        if (existing == null) {
            conversationDao.upsert(
                ConversationEntity(
                    id = conversationId,
                    title = title?.ifBlank { "Новый чат" } ?: "Новый чат"
                )
            )
            Timber.d("Created local conversation: $conversationId")
        }
    }

    private fun parseIsoDateTime(isoString: String): Long {
        return try {
            val isUtc = isoString.endsWith("Z")
            val cleaned = isoString
                .replace("Z", "")
                .replace(Regex("\\.\\d+"), "")
                .substringBefore("+")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val localDateTime = java.time.LocalDateTime.parse(cleaned)
                val zoneId = if (isUtc) java.time.ZoneId.of("UTC") else java.time.ZoneId.systemDefault()
                localDateTime.atZone(zoneId).toInstant().toEpochMilli()
            } else {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                if (isUtc) sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                sdf.parse(cleaned)?.time ?: System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse ISO datetime: $isoString")
            System.currentTimeMillis()
        }
    }
    
    private fun parseTimestamp(createdAt: String?): Long {
        return createdAt?.toLongOrNull() ?: System.currentTimeMillis()
    }

    private fun suggestTitleFromMessage(message: String): String {
        val trimmed = message.trim().replace("\n", " ")
        return if (trimmed.length <= 40) trimmed else trimmed.take(40) + "..."
    }
}
