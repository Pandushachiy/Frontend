package com.health.companion.presentation.screens.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.local.database.ConversationEntity
import com.health.companion.data.remote.api.MessageDTO
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.data.repositories.VoiceRepository
import com.health.companion.ml.voice.VoiceInputManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val voiceInputManager: VoiceInputManager,
    private val voiceRepository: VoiceRepository,
    private val savedStateHandle: SavedStateHandle
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

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(savedStateHandle.get<String>("conversationId"))
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    private val forceSafeVoice = isOppoDevice()

    init {
        observeConversations()
        loadRemoteConversations()
        observeCurrentConversationMessages()
        setupWebSocket()
        setupVoiceInput()
    }
    
    private fun setupVoiceInput() {
        // Reserved for future on-device recognition
    }

    private fun observeCurrentConversationMessages() {
        viewModelScope.launch {
            _currentConversationId
                .filterNotNull()
                .flatMapLatest { convId ->
                    chatRepository.getConversationMessages(convId)
                }
                .catch { e -> Timber.e(e, "Failed to load messages") }
                .collect { messagesList ->
                    _messages.value = messagesList
                }
        }
    }

    private fun observeConversations() {
        viewModelScope.launch {
            chatRepository.getLocalConversationsFlow()
                .catch { e -> Timber.e(e, "Failed to observe conversations") }
                .collect { list ->
                    _conversations.value = list
                    // Auto-select first conversation if none selected
                    if (_currentConversationId.value == null && list.isNotEmpty()) {
                        _currentConversationId.value = list.first().id
                    }
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
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Add user message immediately
                val userMessageId = UUID.randomUUID().toString()
                val userMessage = MessageDTO(
                    id = userMessageId,
                    content = text,
                    role = "user",
                    agent_name = null,
                    provider = null,
                    provider_color = null,
                    model_used = null,
                    created_at = System.currentTimeMillis().toString()
                )
                _messages.value = _messages.value + userMessage
                updateMessageStatus(userMessageId, MessageSendStatus.Sending)
                _currentMessage.value = ""

                val result = chatRepository.sendMessage(text, _currentConversationId.value)
                
                result.onSuccess { response ->
                    if (_currentConversationId.value == null) {
                        _currentConversationId.value = response.getConversationId()
                        savedStateHandle["conversationId"] = response.getConversationId()
                    }

                    val assistantMessage = MessageDTO(
                        id = response.getMessageId().ifEmpty { java.util.UUID.randomUUID().toString() },
                        content = response.getMessageContent(),
                        role = response.message?.role ?: "assistant",
                        agent_name = response.getAgentName(),
                        provider = response.getProviderResolved(),
                        provider_color = response.getProviderColorResolved(),
                        model_used = response.getModelUsedResolved(),
                        created_at = response.getCreatedAt()
                    )
                    upsertMessage(assistantMessage)
                    updateMessageStatus(userMessageId, MessageSendStatus.Sent)
                    
                }.onFailure { e ->
                    Timber.e(e, "Failed to send message")
                    updateMessageStatus(userMessageId, MessageSendStatus.Failed)
                    _uiState.value = ChatUiState.Error(e.message ?: "Ошибка отправки")
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                _messageSendStatus.value = _messageSendStatus.value.toMutableMap().apply {
                    val lastUser = _messages.value.lastOrNull { it.role == "user" }
                    if (lastUser != null) put(lastUser.id, MessageSendStatus.Failed)
                }
                _uiState.value = ChatUiState.Error(e.message ?: "Ошибка")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retrySendMessage(messageId: String, content: String) {
        _messages.value = _messages.value.filterNot { it.id == messageId }
        _messageSendStatus.value = _messageSendStatus.value.toMutableMap().apply { remove(messageId) }
        sendMessage(content)
    }

    fun createNewConversation() {
        viewModelScope.launch {
            _messages.value = emptyList()
            _currentMessage.value = ""
            val result = chatRepository.createConversation()
            result.onSuccess { dto ->
                _currentConversationId.value = dto.id
                savedStateHandle["conversationId"] = dto.id
            }.onFailure { e ->
                Timber.e(e, "Failed to create conversation")
                val local = chatRepository.createLocalConversation()
                local.onSuccess { id ->
                    _currentConversationId.value = id
                    savedStateHandle["conversationId"] = id
                }.onFailure {
                    _uiState.value = ChatUiState.Error("Не удалось создать чат")
                }
            }
        }
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            if (_currentConversationId.value == conversationId) return@launch
            _messages.value = emptyList()
            _currentConversationId.value = conversationId
            savedStateHandle["conversationId"] = conversationId
            _isSyncing.value = true
            chatRepository.syncConversationMessages(conversationId)
                .onFailure { e ->
                    Timber.e(e, "Failed to sync messages")
                }
                .also { _isSyncing.value = false }
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
                
                val result = documentRepository.uploadDocumentFromUri(uri)
                
                result.onSuccess { response ->
                    Timber.d("File uploaded: ${response.filename}")
                    _uploadedFiles.value = _uploadedFiles.value + response.filename
                    
                    // Add system message about uploaded file
                    val systemMessage = MessageDTO(
                        id = UUID.randomUUID().toString(),
                        content = "📎 Файл загружен: ${response.filename}\nСтатус: ${response.status ?: "обработка..."}",
                        role = "assistant",
                        agent_name = "system",
                        provider = "system",
                        provider_color = null,
                        model_used = null,
                        created_at = System.currentTimeMillis().toString()
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
