package com.health.companion.data.repositories

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import timber.log.Timber
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject

interface ChatRepository {
    suspend fun sendMessage(message: String, conversationId: String?): Result<ChatMessageResponse>
    fun getConversationMessages(conversationId: String): Flow<List<MessageDTO>>
    fun getLocalConversationsFlow(): Flow<List<ConversationEntity>>
    suspend fun getConversations(): Result<List<ConversationDTO>>
    suspend fun createConversation(title: String? = null): Result<ConversationDTO>
    suspend fun createLocalConversation(title: String? = null): Result<String>
    suspend fun deleteLocalConversation(conversationId: String): Result<Unit>
    suspend fun syncConversationMessages(conversationId: String): Result<List<MessageDTO>>
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    fun connectWebSocket(userId: String): Flow<WebSocketMessage>
    suspend fun disconnectWebSocket()
}

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatMessageDao: ChatMessageDao,
    private val conversationDao: ConversationDao,
    private val webSocketManager: WebSocketManager
) : ChatRepository {
    
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
            response.items.forEach { dto ->
                conversationDao.insert(
                    ConversationEntity(
                        id = dto.id,
                        title = dto.title.ifBlank { "Новый чат" },
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
            Result.success(response.items)
        } catch (e: Exception) {
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
    
    override fun connectWebSocket(userId: String): Flow<WebSocketMessage> {
        return webSocketManager.connect(userId)
    }
    
    override suspend fun disconnectWebSocket() {
        webSocketManager.disconnect()
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
