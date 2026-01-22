package com.health.companion.presentation.screens.documents

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.DocumentResponse
import com.health.companion.data.repositories.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Idle)
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentResponse>>(emptyList())
    val documents: StateFlow<List<DocumentResponse>> = _documents.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private var pendingPhotoUri: Uri? = null
    private var pendingPhotoFile: File? = null
    private var pollingJob: Job? = null

    private val prefs = context.getSharedPreferences("document_aliases", Context.MODE_PRIVATE)
    private val _displayNames = MutableStateFlow(loadDisplayNames())
    val displayNames: StateFlow<Map<String, String>> = _displayNames.asStateFlow()

    private val localUploadTimes = mutableMapOf<String, Long>()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            try {
                _uiState.value = DocumentsUiState.Loading
                val result = documentRepository.getDocuments()
                result.onSuccess { docs ->
                    _documents.value = docs
                    ensureUploadTimes(docs)
                    handlePolling(docs)
                    _uiState.value = DocumentsUiState.Idle
                }.onFailure { e ->
                    Timber.e(e, "Failed to load documents")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to load documents")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load documents")
                _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to load documents")
            }
        }
    }

    private fun handlePolling(docs: List<DocumentResponse>) {
        val hasProcessing = docs.any { doc ->
            val status = doc.status?.lowercase() ?: ""
            status == "processing" || status == "uploading"
        }
        if (hasProcessing) {
            startPolling()
        } else {
            stopPolling()
        }
    }

    private fun loadDisplayNames(): Map<String, String> {
        return prefs.all.mapNotNull { (key, value) ->
            val name = value as? String
            if (!name.isNullOrBlank()) key to name else null
        }.toMap()
    }

    fun setDisplayName(documentId: String, name: String) {
        if (name.isBlank()) return
        prefs.edit().putString(documentId, name.trim()).apply()
        _displayNames.value = _displayNames.value.toMutableMap().apply {
            put(documentId, name.trim())
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val result = documentRepository.getDocuments()
                result.onSuccess { docs ->
                    _documents.value = docs
                    val hasProcessing = docs.any { doc ->
                        val status = doc.status?.lowercase() ?: ""
                        status == "processing" || status == "uploading"
                    }
                    if (!hasProcessing) {
                        stopPolling()
                        return@launch
                    }
                }.onFailure { e ->
                    Timber.e(e, "Polling documents failed")
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun refreshDocuments() {
        loadDocuments()
    }

    fun prepareCamera(): Uri? {
        return try {
            val photoFile = createImageFile()
            pendingPhotoFile = photoFile
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )
            pendingPhotoUri = uri
            uri
        } catch (e: Exception) {
            Timber.e(e, "Failed to prepare camera")
            null
        }
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("DOC_${timestamp}_", ".jpg", storageDir)
    }

    fun onPhotoTaken() {
        pendingPhotoFile?.let { file ->
            uploadDocumentFromFile(file)
        }
        pendingPhotoUri = null
        pendingPhotoFile = null
    }

    /**
     * Upload document from Uri (from file picker)
     */
    fun uploadDocument(uri: Uri) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uiState.value = DocumentsUiState.Uploading
                
                Timber.d("Uploading document from Uri: $uri")
                
                val result = documentRepository.uploadDocumentFromUri(uri)

                result.onSuccess { response ->
                    Timber.d("Document uploaded successfully: ${response.id}")
                    localUploadTimes[response.id] = System.currentTimeMillis()
                    loadDocuments()
                    _uiState.value = DocumentsUiState.UploadSuccess(response.id)
                }.onFailure { e ->
                    Timber.e(e, "Failed to upload document")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Upload failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload document")
                _uiState.value = DocumentsUiState.Error(e.message ?: "Upload failed")
            } finally {
                _isUploading.value = false
            }
        }
    }

    /**
     * Upload document from File (from camera)
     */
    private fun uploadDocumentFromFile(file: File) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uiState.value = DocumentsUiState.Uploading
                
                Timber.d("Uploading document from File: ${file.absolutePath}")

                val result = documentRepository.uploadDocument(file)

                result.onSuccess { response ->
                    Timber.d("Document uploaded: ${response.id}")
                    localUploadTimes[response.id] = System.currentTimeMillis()
                    loadDocuments()
                    _uiState.value = DocumentsUiState.UploadSuccess(response.id)
                }.onFailure { e ->
                    Timber.e(e, "Failed to upload document")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Upload failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload document")
                _uiState.value = DocumentsUiState.Error(e.message ?: "Upload failed")
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun openDocument(documentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = DocumentsUiState.Loading
                val result = documentRepository.getDocument(documentId)
                result.onSuccess { detail ->
                    _uiState.value = DocumentsUiState.DocumentDetail(detail)
                }.onFailure { e ->
                    Timber.e(e, "Failed to open document")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to open document")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to open document")
                _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to open document")
            }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deleteDocument(documentId)
                result.onSuccess {
                    _documents.value = _documents.value.filter { it.id != documentId }
                    Timber.d("Document deleted: $documentId")
                }.onFailure { e ->
                    Timber.e(e, "Failed to delete document")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Delete failed")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete document")
                _uiState.value = DocumentsUiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun clearState() {
        _uiState.value = DocumentsUiState.Idle
    }

    fun shouldShowReady(document: DocumentResponse, now: Long): Boolean {
        val status = document.status?.lowercase() ?: return false
        if (status != "processed") return false
        val baseTime = localUploadTimes[document.id] ?: parseInstant(document.uploaded_at)
        if (baseTime == null) return false
        val diff = now - baseTime
        val fiveMinutes = 5 * 60 * 1000L
        val tenMinutes = 10 * 60 * 1000L
        return diff in fiveMinutes..tenMinutes
    }

    private fun ensureUploadTimes(docs: List<DocumentResponse>) {
        docs.forEach { doc ->
            if (!localUploadTimes.containsKey(doc.id)) {
                parseInstant(doc.uploaded_at)?.let { localUploadTimes[doc.id] = it }
            }
        }
    }

    private fun parseInstant(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            Instant.parse(raw).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

sealed class DocumentsUiState {
    object Idle : DocumentsUiState()
    object Loading : DocumentsUiState()
    object Uploading : DocumentsUiState()
    data class UploadSuccess(val documentId: String) : DocumentsUiState()
    data class DocumentDetail(val detail: DocumentResponse) : DocumentsUiState()
    data class Error(val message: String) : DocumentsUiState()
}
