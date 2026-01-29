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
    // All fields now in camelCase (backend change 28.01.2026)
    val smartTitle: String? = null,
    val thumbnailUrl: String? = null,
    val previewUrl: String? = null,
    val downloadUrl: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val documentType: String? = null,
    val status: String? = null,
    val description: String? = null,
    val summary: String? = null,
    val extractedEntities: ExtractedEntities? = null,
    val errorMessage: String? = null,
    val uploadedAt: String? = null,
    val processedAt: String? = null
)

@Serializable
data class ExtractedEntities(
    val objects: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val isDocument: Boolean? = null,
    val isPhoto: Boolean? = null,
    val mood: String? = null,
    val ocrText: String? = null,
    val kgEntities: List<String> = emptyList(),
    val kgRelations: Int? = null
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
