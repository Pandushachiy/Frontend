package com.health.companion.data.repositories

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.health.companion.data.local.dao.DocumentDao
import com.health.companion.data.local.database.DocumentEntity
import com.health.companion.data.remote.api.DocumentApi
import com.health.companion.data.remote.api.DocumentResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface DocumentRepository {
    suspend fun uploadDocument(file: File): Result<DocumentResponse>
    suspend fun uploadDocumentFromUri(uri: Uri): Result<DocumentResponse>
    suspend fun getDocuments(): Result<List<DocumentResponse>>
    suspend fun getDocument(documentId: String): Result<DocumentResponse>
    suspend fun deleteDocument(documentId: String): Result<Unit>
    fun getLocalDocuments(): Flow<List<DocumentEntity>>
}

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentApi: DocumentApi,
    private val documentDao: DocumentDao,
    @ApplicationContext private val context: Context
) : DocumentRepository {
    
    override suspend fun uploadDocument(file: File): Result<DocumentResponse> {
        val localId = UUID.randomUUID().toString()
        
        return try {
            // Save locally first
            documentDao.insert(
                DocumentEntity(
                    id = localId,
                    filename = file.name,
                    documentType = getMimeType(file.name),
                    status = "uploading",
                    filePath = file.absolutePath
                )
            )
            
            val requestBody = file.asRequestBody(getMimeType(file.name).toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)
            
            val response = documentApi.uploadDocument(multipartBody)
            
            // Update local record
            documentDao.update(
                DocumentEntity(
                    id = response.id,
                    filename = response.filename,
                    documentType = response.mimeType ?: "unknown",
                    status = response.status ?: "uploaded",
                    filePath = file.absolutePath
                )
            )
            
            if (localId != response.id) {
                documentDao.deleteById(localId)
            }
            
            Timber.d("Document uploaded: ${response.id}")
            Result.success(response)
        } catch (e: Exception) {
            documentDao.updateStatus(localId, "error")
            Timber.e(e, "Failed to upload document")
            Result.failure(e)
        }
    }
    
    override suspend fun uploadDocumentFromUri(uri: Uri): Result<DocumentResponse> = withContext(Dispatchers.IO) {
        val localId = UUID.randomUUID().toString()
        val maxSize = 9 * 1024 * 1024 // 9 MB (backend limit is 10 MB)
        
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
            val fileName = getFileName(uri) ?: "file_${System.currentTimeMillis()}"
            
            // Save locally
            documentDao.insert(
                DocumentEntity(
                    id = localId,
                    filename = fileName,
                    documentType = mimeType,
                    status = "uploading",
                    filePath = uri.toString()
                )
            )
            
            // Read bytes
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            var bytes = inputStream.readBytes()
            inputStream.close()
            
            val originalSize = bytes.size
            Timber.d("Original file: $fileName, size: ${originalSize / 1024} KB, type: $mimeType")
            
            // Compress images if too large
            val isImage = mimeType.startsWith("image/")
            var actualMimeType = mimeType
            var actualFileName = fileName
            
            if (isImage && bytes.size > maxSize) {
                Timber.d("Image is too large (${bytes.size / 1024 / 1024} MB), compressing...")
                bytes = compressImage(bytes, maxSize)
                actualMimeType = "image/jpeg"
                actualFileName = fileName.substringBeforeLast('.') + ".jpg"
                Timber.d("Compressed to ${bytes.size / 1024} KB")
            } else if (!isImage && bytes.size > maxSize) {
                return@withContext Result.failure(Exception("Файл слишком большой (${bytes.size / 1024 / 1024} MB). Максимум: 10 MB"))
            }
            
            Timber.d("Uploading: $actualFileName, size: ${bytes.size / 1024} KB, type: $actualMimeType")
            
            // Create multipart
            val fileBody = bytes.toRequestBody(actualMimeType.toMediaType())
            val multipartBody = MultipartBody.Part.createFormData("file", actualFileName, fileBody)
            
            val response = documentApi.uploadDocument(multipartBody)
            
            // Update local
            documentDao.update(
                DocumentEntity(
                    id = response.id,
                    filename = response.filename,
                    documentType = response.mimeType ?: mimeType,
                    status = response.status ?: "uploaded",
                    filePath = uri.toString()
                )
            )
            
            if (localId != response.id) {
                documentDao.deleteById(localId)
            }
            
            Timber.d("Document uploaded from Uri: ${response.id}")
            Result.success(response)
        } catch (e: retrofit2.HttpException) {
            documentDao.updateStatus(localId, "error")
            // Извлекаем детальное сообщение из JSON ответа
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = try {
                val json = org.json.JSONObject(errorBody ?: "{}")
                json.optString("detail", "Ошибка сервера: ${e.code()}")
            } catch (ex: Exception) {
                "Ошибка сервера: ${e.code()}"
            }
            Timber.e(e, "Upload HTTP error: $errorMessage")
            Result.failure(Exception(errorMessage))
        } catch (e: Exception) {
            documentDao.updateStatus(localId, "error")
            Timber.e(e, "Failed to upload document from Uri")
            Result.failure(e)
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = it.getString(index)
            }
        }
        return name
    }
    
    private fun getMimeType(fileName: String): String = when {
        fileName.endsWith(".pdf", true) -> "application/pdf"
        fileName.endsWith(".doc", true) -> "application/msword"
        fileName.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        fileName.endsWith(".txt", true) -> "text/plain"
        fileName.endsWith(".jpg", true) || fileName.endsWith(".jpeg", true) -> "image/jpeg"
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".gif", true) -> "image/gif"
        fileName.endsWith(".webp", true) -> "image/webp"
        else -> "application/octet-stream"
    }
    
    /**
     * Compress image to fit within maxSize bytes
     * Uses progressive quality reduction and optional resizing
     * Keeps quality high enough for AI recognition (min 60% quality, min 0.5x scale)
     */
    private fun compressImage(imageBytes: ByteArray, maxSize: Int): ByteArray {
        var bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: return imageBytes
        
        // First, try just reducing quality (but not below 60% for AI recognition)
        var quality = 85
        var outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        while (outputStream.size() > maxSize && quality > 60) {
            quality -= 5
            outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
            Timber.d("Compress attempt: quality=$quality, size=${outputStream.size() / 1024} KB")
        }
        
        // If still too large, resize the image (but keep at least 50% for AI to read text/details)
        if (outputStream.size() > maxSize) {
            var scale = 0.85f
            while (outputStream.size() > maxSize && scale > 0.5f) {
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                
                outputStream = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
                Timber.d("Resize attempt: scale=$scale (${newWidth}x${newHeight}), size=${outputStream.size() / 1024} KB")
                
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
                scale -= 0.05f
            }
        }
        
        Timber.d("Final compression: quality=$quality, size=${outputStream.size() / 1024} KB")
        bitmap.recycle()
        return outputStream.toByteArray()
    }
    
    override suspend fun getDocuments(): Result<List<DocumentResponse>> {
        return try {
            Timber.d("Fetching documents from API...")
            val response = documentApi.getDocuments()
            Timber.d("API response: total=${response.total}, page=${response.page}, size=${response.size}")
            Timber.d("Fetched ${response.items.size} documents")
            response.items.forEachIndexed { index, doc ->
                Timber.d("Doc[$index]: id=${doc.id}, filename=${doc.filename}, status=${doc.status}")
            }
            Result.success(response.items)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch documents: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getDocument(documentId: String): Result<DocumentResponse> {
        return try {
            val document = documentApi.getDocument(documentId)
            Timber.d("Fetched document: $documentId")
            Result.success(document)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch document")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDocument(documentId: String): Result<Unit> {
        return try {
            documentApi.deleteDocument(documentId)
            documentDao.deleteById(documentId)
            Timber.d("Document deleted: $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete document")
            Result.failure(e)
        }
    }
    
    override fun getLocalDocuments(): Flow<List<DocumentEntity>> {
        return documentDao.getAllDocumentsFlow()
    }
}
