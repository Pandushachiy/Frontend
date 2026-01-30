package com.health.companion.data.repositories

import android.content.Context
import android.net.Uri
import com.health.companion.data.remote.api.AttachmentDTO
import com.health.companion.data.remote.api.AttachmentMode
import com.health.companion.data.remote.api.AttachmentsApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

interface AttachmentsRepository {
    val attachments: StateFlow<List<AttachmentDTO>>
    val isLoading: StateFlow<Boolean>
    
    suspend fun loadAttachments(conversationId: String): Result<List<AttachmentDTO>>
    suspend fun uploadAttachment(conversationId: String, uri: Uri, mode: AttachmentMode): Result<AttachmentDTO>
    suspend fun deleteAttachment(conversationId: String, attachmentId: String): Result<Unit>
    fun clearAttachments()
}

@Singleton
class AttachmentsRepositoryImpl @Inject constructor(
    private val api: AttachmentsApi,
    @ApplicationContext private val context: Context
) : AttachmentsRepository {
    
    private val _attachments = MutableStateFlow<List<AttachmentDTO>>(emptyList())
    override val attachments: StateFlow<List<AttachmentDTO>> = _attachments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    override suspend fun loadAttachments(conversationId: String): Result<List<AttachmentDTO>> {
        return try {
            _isLoading.value = true
            val result = api.getAttachments(conversationId)
            _attachments.value = result
            Timber.d("Loaded ${result.size} attachments for conversation $conversationId")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load attachments")
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    override suspend fun uploadAttachment(
        conversationId: String,
        uri: Uri,
        mode: AttachmentMode
    ): Result<AttachmentDTO> {
        return try {
            _isLoading.value = true
            
            // Get file from Uri
            val file = uriToFile(uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val modePart = mode.value.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val result = api.uploadAttachment(conversationId, filePart, modePart)
            
            // Update local list
            _attachments.value = _attachments.value + result
            
            Timber.d("Uploaded attachment: ${result.filename} (${result.id})")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to upload attachment")
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    override suspend fun deleteAttachment(conversationId: String, attachmentId: String): Result<Unit> {
        return try {
            api.deleteAttachment(conversationId, attachmentId)
            _attachments.value = _attachments.value.filter { it.id != attachmentId }
            Timber.d("Deleted attachment: $attachmentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete attachment")
            Result.failure(e)
        }
    }
    
    override fun clearAttachments() {
        _attachments.value = emptyList()
    }
    
    private fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open Uri: $uri")
        
        val fileName = getFileName(uri) ?: "attachment_${System.currentTimeMillis()}"
        val tempFile = File(context.cacheDir, fileName)
        
        FileOutputStream(tempFile).use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
    
    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else null
            } else null
        }
    }
}
