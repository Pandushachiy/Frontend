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
import com.health.companion.services.WebSocketManager
import com.health.companion.services.WebSocketMessage
import com.health.companion.utils.TokenManager
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
     * @param images List of base64 encoded images for image-to-image editing
     */
    suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        images: List<String>? = null,
        onStatus: (String) -> Unit,
        onToken: (String) -> Unit,
        onImage: (url: String, prompt: String) -> Unit,
        onDone: (messageId: String, fullContent: String, newConversationId: String?) -> Unit,
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
    fun connectWebSocket(userId: String): Flow<WebSocketMessage>
    suspend fun disconnectWebSocket()
    suspend fun clearAllLocalData()
}

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
    private val webSocketManager: WebSocketManager,
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient // Injected client with TokenAuthenticator
) : ChatRepository {
    
    // SSE streaming client with reasonable timeouts
    private val streamClient = okHttpClient.newBuilder()
        .readTimeout(90, TimeUnit.SECONDS)  // 90 sec - enough for normal responses
        .writeTimeout(30, TimeUnit.SECONDS) 
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()
    
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
     */
    override suspend fun sendMessageStream(
        message: String,
        conversationId: String?,
        images: List<String>?,
        onStatus: (String) -> Unit,
        onToken: (String) -> Unit,
        onImage: (url: String, prompt: String) -> Unit,
        onDone: (messageId: String, fullContent: String, newConversationId: String?) -> Unit,
        onError: (String) -> Unit
    ) {
        android.util.Log.d("SSE_DEBUG", "sendMessageStream called with message: $message, images: ${images?.size ?: 0}")
        
        val token = tokenManager.getAccessToken()
        if (token == null) {
            android.util.Log.e("SSE_DEBUG", "No access token!")
            onError("Требуется авторизация")
            return
        }
        
        android.util.Log.d("SSE_DEBUG", "Token OK, preparing request...")
        
        val body = JSONObject().apply {
            put("message", message)
            conversationId?.let { put("conversation_id", it) }
            // Add images for image-to-image editing
            if (!images.isNullOrEmpty()) {
                val imagesArray = org.json.JSONArray()
                images.forEach { img ->
                    imagesArray.put(img)
                    android.util.Log.d("SSE_DEBUG", "Image base64 length: ${img.length}, prefix: ${img.take(50)}")
                }
                put("images", imagesArray)
                android.util.Log.d("SSE_DEBUG", "✅ Added ${images.size} images to request body")
            }
        }.toString()
        
        android.util.Log.d("SSE_DEBUG", "Request body length: ${body.length} chars")
        
        val url = "${BuildConfig.API_BASE_URL}/chat/send/stream"
        android.util.Log.d("SSE_DEBUG", "URL: $url")
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        
        val savedMessage = message
        val savedConversationId = conversationId
        
        android.util.Log.d("SSE_DEBUG", "Creating EventSource...")
        
        suspendCancellableCoroutine { continuation ->
            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    android.util.Log.d("SSE_DEBUG", "SSE OPENED! Response: ${response.code}")
                }
                
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    android.util.Log.d("SSE_DEBUG", "SSE EVENT: type=$type, data=$data")
                    try {
                        val json = JSONObject(data)
                        
                        when (json.optString("type")) {
                            "status" -> {
                                val status = json.optString("status")
                                Timber.d("SSE status: $status")
                                onStatus(status)
                            }
                            "token" -> {
                                val content = json.optString("content")
                                onToken(content)
                            }
                            "image" -> {
                                val url = json.optString("url")
                                val prompt = json.optString("prompt")
                                Timber.d("SSE image event received: url=$url, prompt=$prompt")
                                android.util.Log.d("SSE_IMAGE", "IMAGE EVENT: url=$url, prompt=$prompt")
                                if (url.isNotBlank()) {
                                    onImage(url, prompt)
                                } else {
                                    Timber.w("SSE image: URL is empty!")
                                }
                            }
                            "done" -> {
                                val messageId = json.optString("message_id")
                                val fullContent = json.optString("full_content")
                                val newConvId = json.optString("conversation_id").takeIf { it.isNotBlank() }
                                val imageUrl = json.optString("image_url").takeIf { it.isNotBlank() }
                                Timber.d("SSE done raw: messageId=$messageId, content=${fullContent.take(100)}, imageUrl=$imageUrl")
                                
                                // If there's an image URL in done, call onImage
                                if (imageUrl != null) {
                                    Timber.d("SSE done contains image URL: $imageUrl")
                                    onImage(imageUrl, "")
                                }
                                
                                // Save to DB in background
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val convId = newConvId ?: savedConversationId ?: UUID.randomUUID().toString()
                                        ensureConversationExists(convId, suggestTitleFromMessage(savedMessage))
                                        
                                        chatMessageDao.insert(
                                            ChatMessageEntity(
                                                id = UUID.randomUUID().toString(),
                                                conversationId = convId,
                                                content = savedMessage,
                                                role = "user"
                                            )
                                        )
                                        
                                        chatMessageDao.insert(
                                            ChatMessageEntity(
                                                id = messageId.ifEmpty { UUID.randomUUID().toString() },
                                                conversationId = convId,
                                                content = fullContent,
                                                role = "assistant"
                                            )
                                        )
                                        conversationDao.updateUpdatedAt(convId, System.currentTimeMillis())
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to save messages to DB")
                                    }
                                }
                                
                                Timber.d("SSE done: messageId=$messageId")
                                onDone(messageId, fullContent, newConvId)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                            "error" -> {
                                val errorMsg = json.optString("message", "Неизвестная ошибка")
                                Timber.e("SSE error event: $errorMsg")
                                onError(errorMsg)
                                eventSource.cancel()
                                if (continuation.isActive) continuation.resume(Unit)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse SSE data: $data")
                    }
                }
                
                override fun onClosed(eventSource: EventSource) {
                    android.util.Log.d("SSE_DEBUG", "SSE CLOSED")
                    if (continuation.isActive) continuation.resume(Unit)
                }
                
                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    android.util.Log.e("SSE_DEBUG", "SSE FAILURE! code=${response?.code}, error=${t?.message}", t)
                    
                    // Ignore socket closed after we've received data
                    if (t?.message?.contains("Socket closed") == true) return
                    
                    val errorMsg = when {
                        t is SocketTimeoutException -> {
                            if (images?.isNotEmpty() == true) {
                                "❌ Image-to-Image пока не работает. Backend не поддерживает."
                            } else {
                                "Превышено время ожидания"
                            }
                        }
                        response?.code == 401 -> "Требуется авторизация"
                        response?.code == 404 -> "Endpoint не найден"
                        response?.code == 429 -> "⏳ Слишком много запросов. Подождите минуту."
                        response?.code == 500 -> "Ошибка сервера. Попробуйте позже."
                        else -> t?.localizedMessage ?: "Ошибка подключения"
                    }
                    onError(errorMsg)
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
            
            val eventSource = EventSources.createFactory(streamClient)
                .newEventSource(request, listener)
            
            continuation.invokeOnCancellation {
                eventSource.cancel()
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
                    agent_name = entity.agentName,
                    provider = entity.provider,
                    provider_color = entity.providerColor,
                    model_used = entity.modelUsed,
                    created_at = entity.createdAt.toString()
                )
            }
        }
    }

    override fun getLocalConversationsFlow(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversationsFlow()
    }

    override suspend fun getConversations(): Result<List<ConversationDTO>> {
        return try {
            val response = chatApi.getConversations()
            val now = System.currentTimeMillis()
            val serverIds = response.items.map { it.id }.toSet()
            
            // Get current local IDs
            val localIds = conversationDao.getAllConversations().map { it.id }.toSet()
            
            // Delete local conversations that don't exist on server
            val toDelete = localIds - serverIds
            toDelete.forEach { id -> conversationDao.deleteById(id) }
            
            // Upsert server conversations
            response.items.forEach { dto ->
                conversationDao.insert(
                    ConversationEntity(
                        id = dto.id,
                        title = dto.title.ifBlank { "Новый чат" },
                        createdAt = now,
                        updatedAt = now,
                        isArchived = dto.is_archived,
                        isPinned = dto.is_pinned,
                        summary = dto.summary
                    )
                )
            }
            Timber.d("Synced ${response.items.size} conversations, removed ${toDelete.size} stale")
            Result.success(response.items)
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
            conversationDao.insert(
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
            conversationDao.insert(
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
            val entities = response.map { msg ->
                ChatMessageEntity(
                    id = msg.id,
                    conversationId = conversationId,
                    content = msg.content,
                    role = msg.role,
                    agentName = msg.agent_name,
                    provider = msg.provider,
                    providerColor = msg.provider_color,
                    modelUsed = msg.model_used,
                    createdAt = parseTimestamp(msg.created_at)
                )
            }
            chatMessageDao.insertAll(entities)
            conversationDao.updateUpdatedAt(conversationId, System.currentTimeMillis())
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
                chatApi.deleteMessage(conversationId, messageId)
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
    
    override fun connectWebSocket(userId: String): Flow<WebSocketMessage> {
        return webSocketManager.connect(userId)
    }
    
    override suspend fun disconnectWebSocket() {
        webSocketManager.disconnect()
    }
    
    override suspend fun clearAllLocalData() {
        chatMessageDao.deleteAll()
        conversationDao.deleteAll()
        Timber.d("All local chat data cleared")
    }
    
    private suspend fun ensureConversationExists(conversationId: String, title: String? = null) {
        val existing = conversationDao.getConversationById(conversationId)
        if (existing == null) {
            conversationDao.insert(
                ConversationEntity(
                    id = conversationId,
                    title = title?.ifBlank { "Новый чат" } ?: "Новый чат"
                )
            )
            Timber.d("Created local conversation: $conversationId")
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
