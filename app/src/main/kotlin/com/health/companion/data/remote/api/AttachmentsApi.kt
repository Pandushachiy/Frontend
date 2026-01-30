package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * API для Session Attachments
 * Позволяет загружать файлы в контекст чата
 */
interface AttachmentsApi {
    
    /**
     * Загрузить файл в сессию
     * @param conversationId ID беседы
     * @param file Файл для загрузки
     * @param attachmentMode Режим: "context" (в контекст) или "edit" (для редактирования)
     */
    @Multipart
    @POST("attachments/{conversationId}/upload")
    suspend fun uploadAttachment(
        @Path("conversationId") conversationId: String,
        @Part file: MultipartBody.Part,
        @Part("attachment_mode") attachmentMode: RequestBody
    ): AttachmentDTO
    
    /**
     * Получить список вложений сессии
     */
    @GET("attachments/{conversationId}")
    suspend fun getAttachments(
        @Path("conversationId") conversationId: String
    ): List<AttachmentDTO>
    
    /**
     * Удалить вложение
     */
    @DELETE("attachments/{conversationId}/{attachmentId}")
    suspend fun deleteAttachment(
        @Path("conversationId") conversationId: String,
        @Path("attachmentId") attachmentId: String
    ): DeleteAttachmentResponse
}

@Serializable
data class AttachmentDTO(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String? = null,
    val type: String,  // "image" | "document" | "audio"
    val filename: String,
    @SerialName("mime_type")
    val mimeType: String? = null,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("extracted_text")
    val extractedText: String? = null,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val url: String? = null,
    val status: String? = null  // "processing" | "ready" | "error"
)

@Serializable
data class DeleteAttachmentResponse(
    val status: String
)

/**
 * Режимы вложений
 */
enum class AttachmentMode(val value: String) {
    /**
     * Файл добавляется в контекст сессии
     * AI будет учитывать его содержимое во всех сообщениях
     */
    CONTEXT("context"),
    
    /**
     * Файл для редактирования (image-to-image)
     */
    EDIT("edit")
}
