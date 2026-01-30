package com.health.companion.presentation.screens.chat

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.local.database.ConversationEntity
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.data.repositories.AttachmentsRepository
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.data.repositories.VoiceRepository
import com.health.companion.data.remote.api.AttachmentDTO
import com.health.companion.data.remote.api.AttachmentMode
import com.health.companion.ml.voice.VoiceInputManager
import com.health.companion.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

sealed interface VoiceUiEvent {
    object RecordingStarted : VoiceUiEvent
    object RecordingStopped : VoiceUiEvent
    data class Error(val message: String) : VoiceUiEvent
}

enum class MessageSendStatus {
    Sending,
    Sent,
    Failed
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository,
    private val attachmentsRepository: AttachmentsRepository,
    private val voiceInputManager: VoiceInputManager,
    private val voiceRepository: VoiceRepository,
    private val tokenManager: TokenManager,
    private val savedStateHandle: SavedStateHandle,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Success)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageDTO>>(emptyList())
    val messages: StateFlow<List<MessageDTO>> = _messages.asStateFlow()

    private val _messageSendStatus = MutableStateFlow<Map<String, MessageSendStatus>>(emptyMap())
    val messageSendStatus: StateFlow<Map<String, MessageSendStatus>> = _messageSendStatus.asStateFlow()

    private val _currentMessage = MutableStateFlow("")
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _voiceEvents = MutableSharedFlow<VoiceUiEvent>(extraBufferCapacity = 2)
    val voiceEvents = _voiceEvents.asSharedFlow()

    // Voice states (server STT)
    val isRecording: StateFlow<Boolean> = voiceRepository.isRecording
    
    private val _partialVoiceResult = MutableStateFlow("")
    val partialVoiceResult: StateFlow<String> = _partialVoiceResult.asStateFlow()
    
    // Upload state
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _uploadedFiles = MutableStateFlow<List<String>>(emptyList())
    val uploadedFiles: StateFlow<List<String>> = _uploadedFiles.asStateFlow()
    
    // Attached image for Image-to-Image
    private val _attachedImageUri = MutableStateFlow<Uri?>(null)
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUri.asStateFlow()
    
    // Session Attachments
    val sessionAttachments: StateFlow<List<AttachmentDTO>> = attachmentsRepository.attachments
    val attachmentsLoading: StateFlow<Boolean> = attachmentsRepository.isLoading
    
    // Streaming state
    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()
    
    private val _streamStatus = MutableStateFlow("")
    val streamStatus: StateFlow<String> = _streamStatus.asStateFlow()
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    // Auth token for image requests
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(savedStateHandle.get<String>("conversationId"))
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    private val forceSafeVoice = isOppoDevice()

    init {
        observeConversations()
        loadRemoteConversations()
        observeCurrentConversationMessages()
        loadAuthToken()
        setupWebSocket()
        setupVoiceInput()
    }
    
    private fun setupVoiceInput() {
        // Reserved for future on-device recognition
    }

    private fun observeCurrentConversationMessages() {
        viewModelScope.launch {
            var currentJob: kotlinx.coroutines.Job? = null
            
            _currentConversationId.collect { convId ->
                // Cancel previous collection
                currentJob?.cancel()
                
                if (convId != null) {
                    // Load session attachments for this conversation
                    attachmentsRepository.loadAttachments(convId)
                    
                    // Start new collection for this conversation
                    currentJob = viewModelScope.launch {
                    chatRepository.getConversationMessages(convId)
                .catch { e -> Timber.e(e, "Failed to load messages") }
                .collect { messagesList ->
                                // Only update if this is still the current conversation
                                // AND not during streaming
                                // AND Room has at least as many messages as memory (don't lose messages)
                                if (_currentConversationId.value == convId && !_isStreaming.value) {
                                    val currentMessages = _messages.value
                                    // Only update from Room if it has more/equal messages
                                    // This prevents overwriting in-memory messages before Room sync completes
                                    if (messagesList.size >= currentMessages.size || currentMessages.isEmpty()) {
                    _messages.value = messagesList
                }
                                }
                            }
                    }
                } else {
                    // Clear attachments when no conversation
                    attachmentsRepository.clearAttachments()
                }
                // Don't clear messages when convId is null - keep showing previous
            }
        }
    }

    private fun loadAuthToken() {
        viewModelScope.launch {
            _authToken.value = tokenManager.getAccessToken()
            Timber.d("ChatViewModel: Auth token loaded = ${_authToken.value?.take(20)}...")
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.getLocalConversationsFlow()
                .catch { e -> Timber.e(e, "Failed to observe conversations") }
                .collect { list ->
                    _conversations.value = list
                    // Don't auto-select - let user choose or start new chat
                }
        }
    }

    private fun loadRemoteConversations() {
        viewModelScope.launch {
            chatRepository.getConversations()
                .onFailure { e -> Timber.e(e, "Failed to load remote conversations") }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            chatRepository.getConversations()
                .onFailure { e -> Timber.e(e, "Failed to refresh conversations") }
        }
    }

    private fun setupWebSocket() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                if (userId != null) {
                    chatRepository.connectWebSocket(userId)
                        .catch { e -> Timber.e(e, "WebSocket error") }
                        .collect { message ->
                            val lastMessage = _messages.value.lastOrNull()
                            if (lastMessage?.role == "assistant") {
                                val updatedMessages = _messages.value.toMutableList()
                                updatedMessages[updatedMessages.lastIndex] = lastMessage.copy(
                                    content = lastMessage.content + message.chunk
                                )
                                _messages.value = updatedMessages
                            }
                        }
                }
            } catch (e: Exception) {
                Timber.e(e, "WebSocket error")
            }
        }
    }

    fun sendMessage(text: String) {
        android.util.Log.d("CHAT_DEBUG", "sendMessage called: $text")
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                android.util.Log.d("CHAT_DEBUG", "Starting stream...")
                _isLoading.value = true
                _isStreaming.value = true
                _streamingResponse.value = ""
                _streamStatus.value = "thinking"
                
                // ВАЖНО: Сохраняем URI ДО всех операций!
                val currentAttachedUri = _attachedImageUri.value
                android.util.Log.d("IMAGE_DEBUG", "========== SEND MESSAGE ==========")
                android.util.Log.d("IMAGE_DEBUG", "1. Current attached URI: $currentAttachedUri")
                
                // Get attached images as base64 for sending to backend
                val attachedImages = getAttachedImagesBase64()
                val hasAttachedImage = currentAttachedUri != null && attachedImages != null
                android.util.Log.d("IMAGE_DEBUG", "2. Has attached image: $hasAttachedImage")
                android.util.Log.d("IMAGE_DEBUG", "3. Base64 images count: ${attachedImages?.size ?: 0}")
                if (attachedImages != null) {
                    android.util.Log.d("IMAGE_DEBUG", "4. Base64 first 100 chars: ${attachedImages.firstOrNull()?.take(100)}")
                }

                // Add user message immediately with the URI for display
                val userMessageId = UUID.randomUUID().toString()
                val attachedImageUris = if (hasAttachedImage && currentAttachedUri != null) {
                    listOf(currentAttachedUri.toString()).also {
                        android.util.Log.d("IMAGE_DEBUG", "5. Saving URI string for display: ${it.first()}")
                    }
                } else null
                android.util.Log.d("IMAGE_DEBUG", "6. attachedImageUris: $attachedImageUris")
                
                val userMessage = MessageDTO(
                    id = userMessageId,
                    content = text,
                    role = "user",
                    agent_name = null,
                    provider = null,
                    provider_color = null,
                    model_used = null,
                    created_at = System.currentTimeMillis().toString(),
                    imageUrl = null, // imageUrl is for AI-generated images
                    images = attachedImageUris // Прикреплённые пользователем изображения (URI для отображения)
                )
                _messages.value = _messages.value + userMessage
                android.util.Log.d("CHAT_DEBUG", "Added user message with images: ${userMessage.images}")
                updateMessageStatus(userMessageId, MessageSendStatus.Sending)
                _currentMessage.value = ""

                // Clear attached image AFTER creating message
                _attachedImageUri.value = null

                // Streaming message state
                val streamingMessageId = UUID.randomUUID().toString()
                val contentBuilder = StringBuilder()
                var messageAdded = false
                var currentImageUrl: String? = null
                
                // SSE streaming - just accumulate text, animation is in Composable
                chatRepository.sendMessageStream(
                    message = text,
                    conversationId = _currentConversationId.value,
                    images = attachedImages,
                    onStatus = { status ->
                        android.util.Log.d("STREAM_DIAG", "📊 STATUS: '$status'")
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            _streamStatus.value = status
                        }
                    },
                    onToken = { token ->
                        val tokenTime = System.currentTimeMillis()
                        contentBuilder.append(token)
                        val currentContent = contentBuilder.toString()
                        android.util.Log.d("STREAM_DIAG", "⚡ TOKEN @$tokenTime len=${currentContent.length} chunk='${token.take(20)}'")
                        
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            if (!messageAdded) {
                                messageAdded = true
                                _isLoading.value = false
                                android.util.Log.d("STREAM_DIAG", "📦 FIRST MSG ADD @${System.currentTimeMillis()} len=${currentContent.length}")
                                // Add message with special flag for animation
                                val msg = MessageDTO(
                                    id = streamingMessageId,
                                    content = currentContent,
                                    role = "assistant",
                                    agent_name = "streaming", // Flag for animation
                                    provider = null,
                                    provider_color = null,
                                    model_used = null,
                                    created_at = System.currentTimeMillis().toString(),
                                    imageUrl = currentImageUrl
                                )
                                _messages.value = _messages.value + msg
                            } else {
                                // Update content - Composable will animate
                                _messages.value = _messages.value.map { m ->
                                    if (m.id == streamingMessageId) m.copy(content = currentContent, imageUrl = currentImageUrl) else m
                                }
                            }
                        }
                    },
                    onImage = { url, prompt ->
                        android.util.Log.d("STREAM_DIAG", "🖼️ IMAGE RECEIVED: url='$url', prompt='$prompt'")
                        Timber.d("🖼️ IMAGE EVENT: url=$url, prompt=$prompt")
                        
                        // Ensure we have a valid URL
                        val fullUrl = when {
                            url.startsWith("http") -> url
                            url.startsWith("/") -> "http://46.17.99.76:8000$url"
                            else -> "http://46.17.99.76:8000/$url"
                        }
                        android.util.Log.d("STREAM_DIAG", "🖼️ FULL URL: $fullUrl")
                        currentImageUrl = fullUrl
                        
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            if (!messageAdded) {
                                messageAdded = true
                                _isLoading.value = false
                                // Create message with image
                                val msg = MessageDTO(
                                    id = streamingMessageId,
                                    content = "",
                                    role = "assistant",
                                    agent_name = "streaming",
                                    provider = null,
                                    provider_color = null,
                                    model_used = null,
                                    created_at = System.currentTimeMillis().toString(),
                                    imageUrl = fullUrl
                                )
                                _messages.value = _messages.value + msg
                                android.util.Log.d("STREAM_DIAG", "🖼️ Created message with imageUrl=$fullUrl")
                            } else {
                                // Update with image URL
                                _messages.value = _messages.value.map { m ->
                                    if (m.id == streamingMessageId) m.copy(imageUrl = fullUrl) else m
                                }
                                android.util.Log.d("STREAM_DIAG", "🖼️ Updated message with imageUrl=$fullUrl")
                            }
                        }
                    },
                    onDone = { messageId, content, newConversationId ->
                        val finalContent = content.ifEmpty { contentBuilder.toString() }
                        
                        viewModelScope.launch(Dispatchers.Main) {
                            // Update conversation ID from server (this is the authoritative ID)
                            if (newConversationId != null && newConversationId.isNotBlank()) {
                                val oldId = _currentConversationId.value
                                if (oldId != newConversationId) {
                                    Timber.d("Server assigned conversation_id: $newConversationId (was: $oldId)")
                                    _currentConversationId.value = newConversationId
                                    savedStateHandle["conversationId"] = newConversationId
                                }
                            }
                            
                            // Update content but KEEP streaming flag for animation
                            _messages.value = _messages.value.map { m ->
                                if (m.id == streamingMessageId) m.copy(content = finalContent) else m
                            }
                            
                    updateMessageStatus(userMessageId, MessageSendStatus.Sent)
                            _streamStatus.value = ""
                            
                            // Wait for animation to complete (approx 30ms per 2 chars)
                            val animationTime = (finalContent.length / 2) * 30L + 500L
                            kotlinx.coroutines.delay(animationTime)
                            
                            // NOW remove streaming flag
                            _messages.value = _messages.value.map { m ->
                                if (m.id == streamingMessageId) m.copy(agent_name = null) else m
                            }
                            
                            _isStreaming.value = false
                            
                            // Refresh conversations list to show new one
                            refreshConversations()
                        }
                    },
                    onError = { errorMsg ->
                        // Ignore "Socket closed" after message was added
                        if (messageAdded && errorMsg.contains("Socket closed", ignoreCase = true)) {
                            return@sendMessageStream
                        }
                        
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            // Handle "Conversation not found" - retry without conversation_id
                            if (errorMsg.contains("not found", ignoreCase = true) || errorMsg.contains("404")) {
                                Timber.w("Conversation not found, clearing and retrying...")
                                // Clear stale conversation
                                _currentConversationId.value?.let { staleId ->
                                    chatRepository.deleteLocalConversation(staleId)
                                }
                                _currentConversationId.value = null
                                savedStateHandle["conversationId"] = null
                                
                                // Retry message without conversation_id
                                _messages.value = _messages.value.filterNot { it.id == streamingMessageId }
                                _streamStatus.value = ""
                                _isStreaming.value = false
                                _isLoading.value = false
                                
                                // Retry
                                sendMessage(text)
                                return@launch
                            }
                            
                            // Remove streaming message if exists
                            _messages.value = _messages.value.filterNot { it.id == streamingMessageId }
                    updateMessageStatus(userMessageId, MessageSendStatus.Failed)
                            _uiState.value = ChatUiState.Error(errorMsg)
                            _streamStatus.value = ""
                            _isStreaming.value = false
                            _isLoading.value = false
                        }
                    }
                )

            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                _messageSendStatus.value = _messageSendStatus.value.toMutableMap().apply {
                    val lastUser = _messages.value.lastOrNull { it.role == "user" }
                    if (lastUser != null) put(lastUser.id, MessageSendStatus.Failed)
                }
                _uiState.value = ChatUiState.Error(e.message ?: "Ошибка")
                _streamStatus.value = ""
                _isStreaming.value = false
                _isLoading.value = false
            }
        }
    }

    fun retrySendMessage(messageId: String, content: String) {
        // Сохраняем изображения из оригинального сообщения
        val originalMessage = _messages.value.find { it.id == messageId }
        val originalImages = originalMessage?.images
        
        _messages.value = _messages.value.filterNot { it.id == messageId }
        _messageSendStatus.value = _messageSendStatus.value.toMutableMap().apply { remove(messageId) }
        
        // Восстанавливаем прикреплённое изображение если было
        if (!originalImages.isNullOrEmpty()) {
            val imageUri = originalImages.firstOrNull()
            if (imageUri != null) {
                try {
                    _attachedImageUri.value = android.net.Uri.parse(imageUri)
                } catch (e: Exception) {
                    android.util.Log.e("RETRY", "Failed to restore attached image: $e")
                }
            }
        }
        
        sendMessage(content)
    }

    fun createNewConversation() {
        // Don't create conversation on backend yet - it will be created with first message
        // Just clear current state for new chat
            _messages.value = emptyList()
            _currentMessage.value = ""
        _currentConversationId.value = null
        savedStateHandle["conversationId"] = null
        _uiState.value = ChatUiState.Success
        Timber.d("New conversation started (will be created on first message)")
    }

    fun selectConversation(conversationId: String) {
        if (_currentConversationId.value == conversationId) return
        
        _uiState.value = ChatUiState.Success // Clear any errors
        
        // Update ID immediately - Flow will load messages smoothly
        _currentConversationId.value = conversationId
        savedStateHandle["conversationId"] = conversationId
        
        // Sync from server in background (UI already updated via Flow)
        viewModelScope.launch {
            val result = chatRepository.syncConversationMessages(conversationId)
            result.onFailure { e -> 
                Timber.e(e, "Failed to sync messages for $conversationId")
                if (e.message?.contains("404") == true || e.message?.contains("not found", ignoreCase = true) == true) {
                    chatRepository.deleteLocalConversation(conversationId)
                    _currentConversationId.value = null
                    _uiState.value = ChatUiState.Error("Сессия не найдена")
                }
            }
        }
    }
    
    /**
     * Upload file from Uri (gallery, file picker, etc.)
     */
    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uiState.value = ChatUiState.Success // Clear any errors
                
                Timber.d("Uploading file from Uri: $uri")
                
                // Check if it's an image file
                val mimeType = appContext.contentResolver.getType(uri)
                val isImage = mimeType?.startsWith("image/") == true
                
                val result = documentRepository.uploadDocumentFromUri(uri)
                
                result.onSuccess { response ->
                    Timber.d("File uploaded: ${response.filename}, isImage: $isImage")
                    _uploadedFiles.value = _uploadedFiles.value + response.filename
                    
                    // Add system message with image preview for images
                    val systemMessage = MessageDTO(
                        id = UUID.randomUUID().toString(),
                        content = if (isImage) "📷 Фото добавлено в контекст" else "📎 Файл загружен: ${response.filename}",
                        role = "assistant",
                        agent_name = "system",
                        provider = "system",
                        provider_color = null,
                        model_used = null,
                        created_at = System.currentTimeMillis().toString(),
                        // Для изображений добавляем превью
                        images = if (isImage) listOf(uri.toString()) else null
                    )
                    _messages.value = _messages.value + systemMessage
                    
                }.onFailure { e ->
                    Timber.e(e, "Failed to upload file")
                    _uiState.value = ChatUiState.Error("Ошибка загрузки: ${e.message}")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Upload failed")
                _uiState.value = ChatUiState.Error("Ошибка: ${e.message}")
            } finally {
                _isUploading.value = false
            }
        }
    }
    
    /**
     * Toggle voice recording
     */
    fun toggleVoiceInput(autoSend: Boolean) {
        viewModelScope.launch {
            if (voiceRepository.isRecording.value) {
                val stopResult = voiceRepository.stopRecording()
                stopResult.onSuccess { file ->
                    _voiceEvents.tryEmit(VoiceUiEvent.RecordingStopped)
                    val transcribeResult = voiceRepository.transcribe(file)
                    transcribeResult.onSuccess { response ->
                        if (response.text.isNotBlank()) {
                            if (autoSend) {
                                sendMessage(response.text)
                            } else {
                                _currentMessage.value = response.text
                            }
                        }
                    }.onFailure { e ->
                        Timber.e(e, "Failed to transcribe")
                        _voiceEvents.tryEmit(VoiceUiEvent.Error("Не удалось распознать речь"))
                        _uiState.value = ChatUiState.Error("Не удалось распознать речь")
                    }
                    file.delete()
                }.onFailure { e ->
                    Timber.e(e, "Failed to stop recording")
                    _voiceEvents.tryEmit(VoiceUiEvent.Error("Ошибка записи"))
                    _uiState.value = ChatUiState.Error("Ошибка записи")
                }
            } else {
                val startResult = voiceRepository.startRecording()
                startResult.onFailure { e ->
                    Timber.e(e, "Failed to start recording")
                    _voiceEvents.tryEmit(VoiceUiEvent.Error("Не удалось начать запись"))
                    _uiState.value = ChatUiState.Error("Не удалось начать запись")
                }.onSuccess {
                    _voiceEvents.tryEmit(VoiceUiEvent.RecordingStarted)
                }
            }
        }
    }

    fun isVoiceAvailable(): Boolean {
        return true
    }

    fun updateCurrentMessage(text: String) {
        _currentMessage.value = text
    }

    fun clearError() {
        _uiState.value = ChatUiState.Success
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            chatRepository.deleteConversation(conversationId)
                .onFailure { e ->
                    Timber.e(e, "Failed to delete conversation")
                    chatRepository.deleteLocalConversation(conversationId)
                        .onFailure { _uiState.value = ChatUiState.Error("Не удалось удалить чат") }
                }
            if (_currentConversationId.value == conversationId) {
                _messages.value = emptyList()
                _currentConversationId.value = null
                savedStateHandle["conversationId"] = null
            }
        }
    }
    
    /**
     * Delete a single message from chat
     * Message is removed from UI immediately with animation,
     * and marked as deleted on backend (excluded from context)
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val conversationId = _currentConversationId.value ?: return@launch
            
            // Remove from local UI state immediately
            _messages.value = _messages.value.filterNot { it.id == messageId }
            
            // Notify backend to exclude from context
            try {
                chatRepository.deleteMessage(conversationId, messageId)
                Timber.d("Message deleted: $messageId")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete message from backend")
                // UI already updated, backend will be informed later
            }
        }
    }
    
    // ==================== Image-to-Image ====================
    
    /**
     * Attach image for image-to-image editing
     */
    fun attachImage(uri: Uri) {
        android.util.Log.d("IMAGE_ATTACH", "Attaching image: $uri")
        _attachedImageUri.value = uri
    }
    
    /**
     * Remove attached image
     */
    fun removeAttachedImage() {
        android.util.Log.d("IMAGE_ATTACH", "Removing attached image")
        _attachedImageUri.value = null
    }
    
    /**
     * Convert Uri to base64 string with compression
     */
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = appContext.contentResolver.openInputStream(uri) ?: return null
            
            // Декодируем bitmap
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) {
                android.util.Log.e("IMAGE_ATTACH", "Failed to decode bitmap from URI")
                return null
            }
            
            // Сжимаем если слишком большой (макс 1024px)
            val maxSize = 1024
            val scale = minOf(
                maxSize.toFloat() / originalBitmap.width,
                maxSize.toFloat() / originalBitmap.height,
                1f // не увеличиваем
            )
            
            val scaledBitmap = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    originalBitmap,
                    (originalBitmap.width * scale).toInt(),
                    (originalBitmap.height * scale).toInt(),
                    true
                )
            } else {
                originalBitmap
            }
            
            // Конвертируем в JPEG base64
            val outputStream = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            
            // Base64 с правильным padding
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)
            // Добавляем padding вручную если нужно
            val paddedBase64 = when (base64.length % 4) {
                2 -> base64 + "=="
                3 -> base64 + "="
                else -> base64
            }
            
            android.util.Log.d("IMAGE_ATTACH", "Converted to base64: ${paddedBase64.length} chars, original size: ${originalBitmap.width}x${originalBitmap.height}, scaled: ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // Освобождаем память
            if (scaledBitmap != originalBitmap) {
                scaledBitmap.recycle()
            }
            originalBitmap.recycle()
            
            "data:image/jpeg;base64,$paddedBase64"
        } catch (e: Exception) {
            android.util.Log.e("IMAGE_ATTACH", "Failed to convert Uri to base64", e)
            null
        }
    }
    
    /**
     * Get attached images as base64 list
     */
    private fun getAttachedImagesBase64(): List<String>? {
        val uri = _attachedImageUri.value ?: return null
        val base64 = uriToBase64(uri) ?: return null
        return listOf(base64)
    }
    
    // ==================== Session Attachments ====================
    
    /**
     * Load attachments for current conversation
     */
    fun loadSessionAttachments() {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            attachmentsRepository.loadAttachments(conversationId)
        }
    }
    
    /**
     * Upload file to session context
     */
    fun uploadSessionAttachment(uri: Uri) {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            attachmentsRepository.uploadAttachment(conversationId, uri, AttachmentMode.CONTEXT)
                .onFailure { e ->
                    Timber.e(e, "Failed to upload session attachment")
                }
        }
    }
    
    /**
     * Delete session attachment
     */
    fun deleteSessionAttachment(attachmentId: String) {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            attachmentsRepository.deleteAttachment(conversationId, attachmentId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.destroy()
        voiceRepository.release()
        viewModelScope.launch {
            chatRepository.disconnectWebSocket()
        }
    }

    private fun upsertMessage(message: MessageDTO) {
        val list = _messages.value.toMutableList()
        val index = list.indexOfFirst { it.id == message.id }
        if (index >= 0) {
            list[index] = message
        } else {
            list.add(message)
        }
        _messages.value = list
    }

    private fun updateMessageStatus(messageId: String, status: MessageSendStatus) {
        _messageSendStatus.value = _messageSendStatus.value.toMutableMap().apply {
            put(messageId, status)
        }
    }

    private fun isOppoDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        return manufacturer.contains("oppo") ||
            brand.contains("oppo") ||
            brand.contains("oneplus") ||
            brand.contains("realme") ||
            model.contains("oppo") ||
            model.contains("find x7")
    }
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    object Success : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}
