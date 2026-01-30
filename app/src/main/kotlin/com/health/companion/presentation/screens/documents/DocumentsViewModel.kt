package com.health.companion.presentation.screens.documents

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.DocumentResponse
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.utils.TokenManager
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
import com.health.companion.utils.ImagePreloader
import com.health.companion.BuildConfig
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
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentsUiState>(DocumentsUiState.Idle)
    val uiState: StateFlow<DocumentsUiState> = _uiState.asStateFlow()

    private val _documents = MutableStateFlow<List<DocumentResponse>>(emptyList())
    val documents: StateFlow<List<DocumentResponse>> = _documents.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    // Pending uploads - показываем сразу в списке с анимацией
    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUpload>> = _pendingUploads.asStateFlow()

    private var pendingPhotoUri: Uri? = null
    private var pendingPhotoFile: File? = null
    private var pollingJob: Job? = null

    private val prefs = context.getSharedPreferences("document_aliases", Context.MODE_PRIVATE)
    private val _displayNames = MutableStateFlow(loadDisplayNames())
    val displayNames: StateFlow<Map<String, String>> = _displayNames.asStateFlow()

    private val localUploadTimes = mutableMapOf<String, Long>()
    
    private var uploadIdCounter = 0

    init {
        loadDocuments()
        loadAuthToken()
    }

    private fun loadAuthToken() {
        viewModelScope.launch {
            _authToken.value = tokenManager.getAccessToken()
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            try {
                Timber.d("DocumentsViewModel: Starting to load documents...")
                _uiState.value = DocumentsUiState.Loading
                val result = documentRepository.getDocuments()
                result.onSuccess { docs ->
                    Timber.d("DocumentsViewModel: Successfully loaded ${docs.size} documents")
                    _documents.value = docs
                    ensureUploadTimes(docs)
                    handlePolling(docs)
                    _uiState.value = DocumentsUiState.Idle
                    
                    // Предзагружаем thumbnails в кэш для мгновенного отображения
                    preloadThumbnails(docs)
                }.onFailure { e ->
                    Timber.e(e, "DocumentsViewModel: Failed to load documents - ${e.message}")
                    _uiState.value = DocumentsUiState.Error(e.message ?: "Failed to load documents")
                }
            } catch (e: Exception) {
                Timber.e(e, "DocumentsViewModel: Exception loading documents - ${e.message}")
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
     * Upload multiple documents at once (from gallery multi-select)
     * Показывает их сразу в списке с анимацией загрузки
     */
    fun uploadDocuments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        
        Timber.d("Starting upload of ${uris.size} documents")
        
        // Создаём pending uploads сразу - они появятся в списке
        val newPending = uris.map { uri ->
            val id = "pending_${uploadIdCounter++}"
            val filename = getFilenameFromUri(uri)
            PendingUpload(
                id = id,
                uri = uri,
                filename = filename,
                status = UploadStatus.UPLOADING,
                progress = 0f
            )
        }
        
        _pendingUploads.value = _pendingUploads.value + newPending
        _isUploading.value = true
        
        // Загружаем параллельно (до 3 одновременно)
        newPending.forEach { pending ->
            viewModelScope.launch {
                try {
                    Timber.d("Uploading: ${pending.filename}")
                    
                    val result = documentRepository.uploadDocumentFromUri(pending.uri)
                    
                    result.onSuccess { response ->
                        Timber.d("Upload success: ${response.id}")
                        localUploadTimes[response.id] = System.currentTimeMillis()
                        
                        // Убираем из pending
                        _pendingUploads.value = _pendingUploads.value.filter { it.id != pending.id }
                        
                        // Добавляем в основной список
                        _documents.value = listOf(response) + _documents.value
                        
                    }.onFailure { e ->
                        Timber.e(e, "Upload failed: ${pending.filename}")
                        // Помечаем как ошибку
                        _pendingUploads.value = _pendingUploads.value.map {
                            if (it.id == pending.id) it.copy(status = UploadStatus.ERROR, error = e.message)
                            else it
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Upload exception: ${pending.filename}")
                    _pendingUploads.value = _pendingUploads.value.map {
                        if (it.id == pending.id) it.copy(status = UploadStatus.ERROR, error = e.message)
                        else it
                    }
                }
            }
        }
        
        // Следим за завершением всех загрузок
        viewModelScope.launch {
            while (_pendingUploads.value.any { it.status == UploadStatus.UPLOADING }) {
                delay(500)
            }
            _isUploading.value = false
            
            // Убираем успешные через 2 секунды
            delay(2000)
            _pendingUploads.value = _pendingUploads.value.filter { it.status == UploadStatus.ERROR }
        }
    }
    
    private fun getFilenameFromUri(uri: Uri): String {
        return try {
            // Пробуем получить DISPLAY_NAME через ContentResolver
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) return name
                    }
                }
                null
            }?.let { return it }
            
            // Fallback: используем lastPathSegment с расширением
            val segment = uri.lastPathSegment ?: "file"
            
            // Если это просто ID (числа), добавляем расширение на основе MIME
            if (segment.all { it.isDigit() }) {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when {
                    mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
                    mimeType.contains("png") -> "png"
                    mimeType.contains("webp") -> "webp"
                    mimeType.contains("gif") -> "gif"
                    mimeType.contains("pdf") -> "pdf"
                    else -> "file"
                }
                "IMG_$segment.$ext"
            } else {
                segment
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting filename from URI")
            "file_${System.currentTimeMillis()}.jpg"
        }
    }
    
    fun retryUpload(pendingId: String) {
        val pending = _pendingUploads.value.find { it.id == pendingId } ?: return
        
        // Сбрасываем статус
        _pendingUploads.value = _pendingUploads.value.map {
            if (it.id == pendingId) it.copy(status = UploadStatus.UPLOADING, error = null)
            else it
        }
        
        viewModelScope.launch {
            try {
                val result = documentRepository.uploadDocumentFromUri(pending.uri)
                result.onSuccess { response ->
                    localUploadTimes[response.id] = System.currentTimeMillis()
                    _pendingUploads.value = _pendingUploads.value.filter { it.id != pendingId }
                    _documents.value = listOf(response) + _documents.value
                }.onFailure { e ->
                    _pendingUploads.value = _pendingUploads.value.map {
                        if (it.id == pendingId) it.copy(status = UploadStatus.ERROR, error = e.message)
                        else it
                    }
                }
            } catch (e: Exception) {
                _pendingUploads.value = _pendingUploads.value.map {
                    if (it.id == pendingId) it.copy(status = UploadStatus.ERROR, error = e.message)
                    else it
                }
            }
        }
    }
    
    fun cancelPendingUpload(pendingId: String) {
        _pendingUploads.value = _pendingUploads.value.filter { it.id != pendingId }
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
        val baseTime = localUploadTimes[document.id] ?: parseInstant(document.uploadedAt)
        if (baseTime == null) return false
        val diff = now - baseTime
        val fiveMinutes = 5 * 60 * 1000L
        val tenMinutes = 10 * 60 * 1000L
        return diff in fiveMinutes..tenMinutes
    }
    
    /**
     * Проверяет, загружен ли документ недавно (в последние 2 минуты)
     * и ещё ожидает обработки ИИ (smartTitle == null)
     */
    fun isAwaitingAiProcessing(document: DocumentResponse, now: Long): Boolean {
        // Если уже есть smartTitle - обработка завершена
        if (document.smartTitle != null) return false
        
        // Если status = processing - точно в обработке
        if (document.status?.lowercase() == "processing") return true
        
        // Если недавно загружен (в течение 2 минут) и нет smartTitle - показываем обработку
        val uploadTime = localUploadTimes[document.id] ?: parseInstant(document.uploadedAt) ?: return false
        val diff = now - uploadTime
        val twoMinutes = 2 * 60 * 1000L
        return diff < twoMinutes
    }
    
    /**
     * Предзагружает thumbnails документов в кэш для мгновенного отображения
     */
    private fun preloadThumbnails(docs: List<DocumentResponse>) {
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()
            val apiHost = BuildConfig.API_BASE_URL.substringBefore("/api/")
            
            val thumbnailUrls = docs
                .filter { it.mimeType?.startsWith("image/") == true }
                .take(20) // Первые 20 изображений
                .mapNotNull { doc ->
                    doc.thumbnailUrl?.let { "$apiHost$it" }
                        ?: "${BuildConfig.API_BASE_URL}/documents/${doc.id}/thumbnail"
                }
            
            if (thumbnailUrls.isNotEmpty()) {
                ImagePreloader.preloadImages(context, thumbnailUrls, token)
            }
        }
    }

    private fun ensureUploadTimes(docs: List<DocumentResponse>) {
        docs.forEach { doc ->
            if (!localUploadTimes.containsKey(doc.id)) {
                parseInstant(doc.uploadedAt)?.let { localUploadTimes[doc.id] = it }
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

enum class UploadStatus {
    UPLOADING, SUCCESS, ERROR
}

data class PendingUpload(
    val id: String,
    val uri: Uri,
    val filename: String,
    val status: UploadStatus,
    val progress: Float = 0f,
    val error: String? = null
)
