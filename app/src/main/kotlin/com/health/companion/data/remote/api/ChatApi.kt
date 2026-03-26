package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {
    
    @POST("chat/v3/send/stream")
    suspend fun sendMessage(@Body request: ChatMessageRequest): ChatMessageResponse
    
    @POST("chat/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest = CreateConversationRequest()): ConversationDTO

    @GET("chat/conversations")
    suspend fun getConversations(
        @Query("size") size: Int = 100,  // Загружаем больше за раз
        @Query("page") page: Int = 1
    ): ConversationsResponse
    
    @GET("chat/conversations/{conversationId}/messages")
    suspend fun getMessages(@Path("conversationId") conversationId: String): List<MessageDTO>
    
    @DELETE("chat/conversations/{conversationId}")
    suspend fun deleteConversation(@Path("conversationId") conversationId: String): ConversationDeleteResponse
    
    @DELETE("chat/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): MessageDeleteResponse
    
    /**
     * Регенерация названия сессии через LLM
     * Использует анализ сообщений диалога для автоматического именования
     */
    @POST("chat/conversations/{conversationId}/regenerate-title")
    suspend fun regenerateTitle(
        @Path("conversationId") conversationId: String
    ): RegenerateTitleResponse
    
    @POST("chat/v3/confirm/{confirmationId}")
    suspend fun confirmAction(
        @Path("confirmationId") confirmationId: String,
        @Body request: ConfirmActionRequest
    ): ConfirmActionResponse
}

@Serializable
data class ConfirmActionRequest(
    val approved: Boolean
)

@Serializable
data class ConfirmActionResponse(
    @SerialName("confirmation_id") val confirmationId: String,
    val approved: Boolean,
    val message: String? = null
)

@Serializable
data class RegenerateTitleResponse(
    val title: String,
    val id: String? = null
)

@Serializable
data class MessageDeleteResponse(
    val status: String = "ok"
)

@Serializable
data class ConversationsResponse(
    val items: List<ConversationDTO> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
    val pages: Int = 0
)

@Serializable
data class ChatMessageRequest(
    val message: String,
    val conversation_id: String? = null,
    val timezone: String = java.util.TimeZone.getDefault().id,
    val images: List<String>? = null,
    val document_ids: List<String>? = null,
    val force_agent: String? = null,
    val stream: Boolean = false
)

@Serializable
data class CreateConversationRequest(
    val title: String? = null
)

@Serializable
data class ChatMessageResponse(
    val message: ChatMessageData? = null,
    val conversation_id: String? = null,
    val agent_used: String? = null,
    val provider: String? = null,
    val provider_color: String? = null,
    val model_used: String? = null,
    val confidence: Double? = null,
    val processing_time_ms: Long? = null,
    val tokens_used: Int? = null,
    val sources: List<String>? = null,
    val citations: List<String>? = null,
    val disclaimers: List<String>? = null,
    val warnings: List<String>? = null,
    val suggested_followup: String? = null
) {
    fun getMessageContent(): String = message?.content ?: ""
    fun getMessageId(): String = message?.id ?: ""
    fun getConversationId(): String = message?.conversation_id ?: conversation_id ?: ""
    fun getAgentName(): String = message?.agent_name ?: agent_used ?: ""
    fun getCreatedAt(): String = message?.created_at ?: ""
    fun getProviderResolved(): String? = message?.provider ?: provider
    fun getProviderColorResolved(): String? = message?.provider_color ?: provider_color
    fun getModelUsedResolved(): String? = message?.model_used ?: model_used
}

@Serializable
data class ChatMessageData(
    val id: String? = null,
    val conversation_id: String? = null,
    val role: String? = null,
    val content: String? = null,
    val agent_name: String? = null,
    val provider: String? = null,
    val provider_color: String? = null,
    val model_used: String? = null,
    val confidence: Double? = null,
    val sources: List<String>? = null,
    val citations: List<String>? = null,
    val tokens_used: Int? = null,
    val processing_time: Long? = null,
    val disclaimers: List<String>? = null,
    val warnings: List<String>? = null,
    val suggested_actions: List<String>? = null,
    val created_at: String? = null
)

@Serializable
data class MessageMetadata(
    val agent: String? = null,
    val model: String? = null,
    val tokens_used: Int? = null
)


@Serializable
data class ConversationDTO(
    val id: String,
    val title: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val lastMessageAt: String? = null,  // Время последнего сообщения (camelCase от бэка)
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val summary: String? = null,
    val messageCount: Int? = null
)

@Serializable
data class MessageDTO(
    val id: String,
    val conversationId: String? = null,
    val content: String,
    val role: String,
    val agentName: String? = null,
    val provider: String? = null,
    val providerColor: String? = null,
    val modelUsed: String? = null,
    val tokensUsed: Int? = null,
    val processingTime: Int? = null,
    val createdAt: String? = null,  // camelCase от бэка
    val imageUrl: String? = null,
    val images: List<String>? = null,
    val citations: List<Citation>? = null,
    val files: List<GeneratedFile>? = null
)

@Serializable
data class Citation(
    val index: Int,
    val url: String,
    val domain: String,
    val title: String
)

@Serializable
data class ConversationDeleteResponse(
    val message: String
)

/**
 * Agent thinking step (Cursor AI style)
 */
data class AgentStep(
    val step: String,
    val detail: String
)

/**
 * File generated by AI (Excel, PDF, etc.)
 */
@Serializable
data class GeneratedFile(
    val url: String,
    val name: String,
    val icon: String? = null
)

/**
 * Emotion detected by AI from user's message.
 * SSE: {"type": "emotion", "emotion": "curious", "confidence": 0.7, "tone": "playful"}
 * Emotions: happy, sad, anxious, frustrated, neutral, excited, stressed, curious
 */
data class EmotionEvent(
    val emotion: String,
    val confidence: Float = 0.5f,
    val tone: String = ""
)

/**
 * Confirmation request from AI before destructive action.
 * SSE: {"type": "confirmation_pending", "tool": "memory_forget", "preview": "...", "confirmation_id": "..."}
 */
data class ConfirmationEvent(
    val confirmationId: String,
    val tool: String,
    val preview: String
)

/**
 * Thinking chain step from SSE stream.
 * SSE: {"type":"thinking_chain","phase":"analyze"|"execute"|"observe","step":1,"detail":"...","plan":"...","tool":"...","elapsed_ms":42}
 */
data class ThinkingChainStep(
    val phase: String,        // "analyze" | "execute" | "observe"
    val step: Int,
    val detail: String,
    val plan: String? = null, // "web_search → create_spreadsheet"
    val tool: String? = null,
    val elapsedMs: Long = 0
)

/**
 * Progress event from SSE stream.
 * SSE: {"type": "progress", "percent": 33, "step": 1, "total_steps": 2, "step_label": "..."}
 * step/totalSteps are null when backend sends percent-only mode (no plan).
 */
data class ProgressEvent(
    val percent: Int,
    val step: Int? = null,
    val totalSteps: Int? = null,
    val stepLabel: String? = null
)

/**
 * Workspace file generated during agent run.
 * Part of agent_done result.workspace_files.
 */
@Serializable
data class WorkspaceFile(
    val name: String,
    val path: String,
    @SerialName("size_bytes") val sizeBytes: Long = 0,
    @SerialName("size_display") val sizeDisplay: String = "",
    val records: Int? = null,
    val type: String = ""
)
