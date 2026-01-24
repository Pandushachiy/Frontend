package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface DocumentApi {
    
    /**
     * Upload document - per backend contract just sends file
     */
    @Multipart
    @POST("documents/upload/")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part
    ): DocumentResponse
    
    @GET("documents/")
    suspend fun getDocuments(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): DocumentsListResponse
    
    @GET("documents/{documentId}/")
    suspend fun getDocument(@Path("documentId") documentId: String): DocumentResponse
    
    @DELETE("documents/{documentId}/")
    suspend fun deleteDocument(@Path("documentId") documentId: String): DeleteDocumentResponse
}

@Serializable
data class DocumentResponse(
    val id: String,
    val filename: String,
    val file_size: Long? = null,
    val mime_type: String? = null,
    val document_type: String? = null,
    val status: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val extracted_entities: ExtractedEntities? = null,
    val error_message: String? = null,
    val uploaded_at: String? = null,
    val processed_at: String? = null
)

@Serializable
data class ExtractedEntities(
    val objects: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val is_document: Boolean? = null,
    val is_photo: Boolean? = null,
    val mood: String? = null,
    val ocr_text: String? = null,
    val kg_entities: List<String> = emptyList(),
    val kg_relations: Int? = null
)

@Serializable
data class DeleteDocumentResponse(
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class DocumentsListResponse(
    val items: List<DocumentResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)
