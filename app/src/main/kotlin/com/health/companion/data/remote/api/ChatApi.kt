package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApi {
    
    @POST("chat/send")
    suspend fun sendMessage(@Body request: ChatMessageRequest): ChatMessageResponse
    
    @POST("chat/conversations")
    suspend fun createConversation(@Body request: CreateConversationRequest = CreateConversationRequest()): ConversationDTO

    @GET("chat/conversations")
    suspend fun getConversations(): ConversationsResponse
    
    @GET("chat/conversations/{conversationId}/messages")
    suspend fun getMessages(@Path("conversationId") conversationId: String): List<MessageDTO>
    
    @DELETE("chat/conversations/{conversationId}")
    suspend fun deleteConversation(@Path("conversationId") conversationId: String): DeleteResponse
}

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
    val title: String,
    val created_at: String,
    val updated_at: String? = null,
    val is_archived: Boolean = false,
    val is_pinned: Boolean = false,
    val summary: String? = null
)

@Serializable
data class MessageDTO(
    val id: String,
    val conversation_id: String? = null,
    val content: String,
    val role: String,
    val agent_name: String? = null,
    val provider: String? = null,
    val provider_color: String? = null,
    val model_used: String? = null,
    val tokens_used: Int? = null,
    val processing_time: Int? = null,
    val created_at: String
)

@Serializable
data class DeleteResponse(
    val status: String
)
