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
import com.health.companion.data.remote.api.AgentStep
import com.health.companion.data.remote.api.Citation
import com.health.companion.data.remote.api.EmotionEvent
import com.health.companion.data.remote.api.ConfirmationEvent
import com.health.companion.data.remote.api.GeneratedFile
import com.health.companion.data.remote.api.ProgressEvent
import com.health.companion.data.remote.api.ThinkingChainStep
import com.health.companion.services.WsEvent
import com.health.companion.data.repositories.AttachmentsRepository
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.data.repositories.DocumentRepository
import com.health.companion.data.repositories.VoiceRepository
import com.health.companion.data.remote.api.AttachmentDTO
import com.health.companion.data.remote.api.AttachmentMode
import com.health.companion.ml.voice.VoiceInputManager
import com.health.companion.services.ChatBackgroundService
import com.health.companion.services.ChatConnectionService
import com.health.companion.services.NotificationHelper
import com.health.companion.utils.AppLifecycleTracker
import com.health.companion.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.health.companion.BuildConfig
import timber.log.Timber
import java.io.File
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
    private val notificationHelper: NotificationHelper,
    private val appLifecycleTracker: AppLifecycleTracker,
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
    
    // Attached images for current message (one-time use) — поддержка нескольких фото
    private val _attachedImageUris = MutableStateFlow<List<Uri>>(emptyList())
    val attachedImageUris: StateFlow<List<Uri>> = _attachedImageUris.asStateFlow()
    
    // Для совместимости со старым кодом
    val attachedImageUri: StateFlow<Uri?> = _attachedImageUris.map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    // Session Attachments
    val sessionAttachments: StateFlow<List<AttachmentDTO>> = attachmentsRepository.attachments
    val attachmentsLoading: StateFlow<Boolean> = attachmentsRepository.isLoading
    
    // Streaming state
    private val _streamStatus = MutableStateFlow("")
    val streamStatus: StateFlow<String> = _streamStatus.asStateFlow()

    // Локализованный лейбл статуса от бэка (из status_label поля)
    private val _streamStatusLabel = MutableStateFlow<String?>(null)
    val streamStatusLabel: StateFlow<String?> = _streamStatusLabel.asStateFlow()
    
    // Контент начал стримиться (токены пошли) — НЕ перезатирает streamStatus!
    private val _contentStarted = MutableStateFlow(false)
    val contentStarted: StateFlow<Boolean> = _contentStarted.asStateFlow()
    
    // Web search обнаружен — держим минимум 2.5сек для видимости
    private val _webSearchActive = MutableStateFlow(false)
    val webSearchActive: StateFlow<Boolean> = _webSearchActive.asStateFlow()
    private var webSearchHideJob: kotlinx.coroutines.Job? = null
    
    // Image is expected in the current stream — set once generating_image status arrives,
    // stays true until stream ends so text is hidden until image loads.
    private val _imageExpectedInStream = MutableStateFlow(false)
    val imageExpectedInStream: StateFlow<Boolean> = _imageExpectedInStream.asStateFlow()
    
    
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()
    
    // Стримящийся текст — отдельный поток чтобы НЕ пересоздавать _messages каждые 20мс
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId.asStateFlow()

    // Token display — instant start, smooth large chunks
    private var tokenDispatcherJob: kotlinx.coroutines.Job? = null
    @Volatile private var sseStreamDone = false
    @Volatile private var lastStreamEndMs = 0L
    // true когда пользователь явно нажал "Новый чат" — блокирует авто-выбор последнего диалога
    @Volatile private var isNewChatMode = false
    
    // Agent thinking steps (Cursor AI style)
    private val _thinkingSteps = MutableStateFlow<List<AgentStep>>(emptyList())
    val thinkingSteps: StateFlow<List<AgentStep>> = _thinkingSteps.asStateFlow()
    
    // Generated files from AI
    private val _generatedFiles = MutableStateFlow<List<GeneratedFile>>(emptyList())
    val generatedFiles: StateFlow<List<GeneratedFile>> = _generatedFiles.asStateFlow()
    
    // Current emotion detected by AI
    private val _currentEmotion = MutableStateFlow<EmotionEvent?>(null)
    val currentEmotion: StateFlow<EmotionEvent?> = _currentEmotion.asStateFlow()
    
    // Progress from SSE stream (percent 0-100)
    private val _streamProgress = MutableStateFlow<ProgressEvent?>(null)
    val streamProgress: StateFlow<ProgressEvent?> = _streamProgress.asStateFlow()

    // Thinking chain steps (reasoning visualization)
    private val _thinkingChain = MutableStateFlow<List<ThinkingChainStep>>(emptyList())
    val thinkingChain: StateFlow<List<ThinkingChainStep>> = _thinkingChain.asStateFlow()

    // Background task result from WebSocket
    private val _backgroundTaskResult = MutableSharedFlow<WsEvent.BackgroundTaskResult>(extraBufferCapacity = 4)
    val backgroundTaskResult = _backgroundTaskResult.asSharedFlow()

    // Pending confirmation from AI (destructive action preview)
    private val _pendingConfirmation = MutableStateFlow<ConfirmationEvent?>(null)
    val pendingConfirmation: StateFlow<ConfirmationEvent?> = _pendingConfirmation.asStateFlow()
    
    private val _confirmationProcessing = MutableStateFlow(false)
    val confirmationProcessing: StateFlow<Boolean> = _confirmationProcessing.asStateFlow()
    
    // Original user message text stored for re-send after confirmation
    private var pendingConfirmationUserMessage: String? = null

    // Canvas events from SSE — (action, payloadJsonString)
    private val _canvasEvents = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8)
    val canvasEvents = _canvasEvents.asSharedFlow()

    // Emitted once when AI response is fully done (for canvas graph refresh)
    private val _messageCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val messageCompleted = _messageCompleted.asSharedFlow()

    // Auth token for image requests
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _conversations = MutableStateFlow<List<ConversationEntity>>(emptyList())
    val conversations: StateFlow<List<ConversationEntity>> = _conversations.asStateFlow()

    private val _currentConversationId = MutableStateFlow<String?>(savedStateHandle.get<String>("conversationId"))
    val currentConversationId: StateFlow<String?> = _currentConversationId.asStateFlow()
    private val forceSafeVoice = isOppoDevice()


    init {
        // Быстрая предзагрузка из Room — чат появляется мгновенно без мигания пустым экраном
        viewModelScope.launch {
            try {
                val conversations = chatRepository.getLocalConversationsFlow().first()
                val lastConv = conversations.maxByOrNull { it.updatedAt }
                if (lastConv != null && _currentConversationId.value == null) {
                    val cached = chatRepository.getConversationMessages(lastConv.id).first()
                    if (cached.isNotEmpty()) {
                        _messages.value = cached
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Preload messages from cache failed")
            }
        }
        observeConversations()
        loadRemoteConversations()
        observeCurrentConversationMessages()
        recoverStreamingDraftIfNeeded()
        loadAuthToken()
        setupWebSocket()
        observeReminderEvents()
        observeBackgroundTaskResults()
        setupVoiceInput()
        // Start persistent WebSocket service so the server can push AI-initiated
        // messages even when the user is not actively looking at the app.
        ChatConnectionService.start(appContext)
    }

    /**
     * При старте ViewModel проверяем: был ли прерван стриминг (OEM убил процесс)?
     * Если в Room есть сообщение с isStreamingDraft=true — показываем его как
     * частичный ответ и сбрасываем флаг. Полный ответ придёт при следующей синхронизации.
     */
    /**
     * При старте ViewModel (в т.ч. после гибели процесса) проверяем:
     * 1. Есть ли черновик стриминга в БД → снимаем флаг (данные уже отображаются через Room-флоу)
     * 2. Есть ли текущий разговор → синхронизируем с сервером, чтобы забрать
     *    ответ, который сервер завершил пока мы были заморожены/убиты.
     */
    private fun recoverStreamingDraftIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val convId = _currentConversationId.value ?: return@launch
                // 1. Чистим черновик если есть
                val draft = chatRepository.getStreamingDraft(convId)
                if (draft != null) {
                    Timber.w("recoverStreamingDraft: found interrupted draft id=${draft.id} len=${draft.content.length}")
                    chatRepository.clearStreamingDrafts(convId)
                }
                // 2. Синхронизируем с сервером — забираем готовый ответ (изображение, текст и т.д.)
                Timber.d("recoverStreamingDraft: syncing $convId from server")
                chatRepository.syncConversationMessages(convId)
            } catch (e: Exception) {
                Timber.w(e, "recoverStreamingDraft failed")
            }
        }
    }
    
    /**
     * Вызывается из ChatScreen при Lifecycle.ON_RESUME (возврат из фона).
     * Если стриминг НЕ активен — синхронизируем текущий разговор с сервером,
     * чтобы подобрать ответ, который сервер завершил пока мы были заморожены.
     */
    fun syncOnResume() {
        val convId = _currentConversationId.value ?: return
        // Если идёт активный стриминг — Room Flow всё равно заблокирован,
        // данные придут через polling loop в onError. Пропускаем.
        // Но если _isStreaming завис (процесс был заморожен Realme) —
        // то не блокируем, т.к. onError вызовет polling отдельно.
        if (_isStreaming.value) {
            android.util.Log.w("SSE_PERSIST", "syncOnResume SKIP (streaming active — onError will poll)")
            return
        }
        val sinceLast = System.currentTimeMillis() - lastStreamEndMs
        if (sinceLast < 30_000) {
            android.util.Log.w("SSE_PERSIST", "syncOnResume SKIP (cooldown ${sinceLast}ms)")
            return
        }
        android.util.Log.w("SSE_PERSIST", "syncOnResume for $convId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                chatRepository.syncConversationMessages(convId)
                // Принудительно обновляем UI если данные изменились
                val fresh = chatRepository.getConversationMessages(convId).first()
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (fresh.isNotEmpty() && fresh.size != _messages.value.size) {
                        android.util.Log.w("SSE_PERSIST", "syncOnResume: refreshed ${fresh.size} msgs")
                        _messages.value = fresh
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SSE_PERSIST", "syncOnResume FAILED: ${e.message}")
            }
        }
    }

    private fun observeBackgroundTaskResults() {
        viewModelScope.launch {
            chatRepository.backgroundTaskEvents.collect { event ->
                _backgroundTaskResult.emit(event)
            }
        }
    }

    private fun setupVoiceInput() {
        // Reserved for future on-device recognition
    }

    private val imageUrlRegex = Regex("""!\[.*?]\((.*?)\)""")
    private val apiHost = BuildConfig.API_BASE_URL.substringBefore("/api/")

    private fun observeCurrentConversationMessages() {
        viewModelScope.launch {
            var currentJob: kotlinx.coroutines.Job? = null
            
            _currentConversationId.collect { convId ->
                currentJob?.cancel()
                if (convId != null) {
                    attachmentsRepository.loadAttachments(convId)
                    
                    currentJob = viewModelScope.launch {
                        chatRepository.getConversationMessages(convId)
                            .catch { e -> Timber.e(e, "Failed to load messages") }
                            .collect { messagesList ->
                                if (_currentConversationId.value == convId && !_isStreaming.value) {
                                    val currentMsgs = _messages.value
                                    if (messagesList.isEmpty() && currentMsgs.isNotEmpty()) return@collect
                                    android.util.Log.w("SSE_PERSIST", "Room→UI: ${messagesList.size} msgs for $convId (was ${currentMsgs.size})")
                                    _messages.value = messagesList
                                    preloadMessageImages(messagesList)
                                }
                            }
                    }
                } else {
                    if (!_isStreaming.value) {
                        _messages.value = emptyList()
                    }
                    attachmentsRepository.clearAttachments()
                }
            }
        }
    }

    private fun preloadMessageImages(messages: List<MessageDTO>) {
        viewModelScope.launch(Dispatchers.IO) {
            val urls = mutableListOf<String>()
            for (msg in messages) {
                msg.imageUrl?.let { url ->
                    urls.add(resolveImageUrl(url))
                }
                msg.content?.let { content ->
                    imageUrlRegex.findAll(content).forEach { match ->
                        urls.add(resolveImageUrl(match.groupValues[1]))
                    }
                }
            }
            if (urls.isNotEmpty()) {
                val token = _authToken.value
                com.health.companion.utils.ImagePreloader.preloadImages(appContext, urls.distinct(), token)
            }
        }
    }

    private fun resolveImageUrl(url: String): String = when {
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "$apiHost$url"
        else -> "$apiHost/$url"
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
                    // Авто-выбор последней сессии только при первом запуске приложения,
                    // НЕ после того как пользователь явно нажал «Новый чат»
                    if (_currentConversationId.value == null && list.isNotEmpty() && !isNewChatMode) {
                        val lastConv = list.maxByOrNull { it.updatedAt }
                        lastConv?.let { selectConversation(it.id) }
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

    /**
     * When a WS reminder event arrives while the app is in the foreground,
     * sync the relevant conversation so the reminder message appears in the chat.
     * No system notification is shown here — FCM handles that.
     */
    private fun observeReminderEvents() {
        viewModelScope.launch {
            chatRepository.reminderEvents.collect { event ->
                Timber.d("ChatViewModel: reminder event received id=${event.reminderId} conv=${event.conversationId}")
                val targetConvId = event.conversationId.takeIf { it.isNotBlank() }
                    ?: _currentConversationId.value
                if (targetConvId != null) {
                    chatRepository.syncConversationMessages(targetConvId)
                }
            }
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

    private fun debugLog(msg: String) {
        Timber.d("PHOTO_DEBUG $msg")
        try {
            val f = java.io.File(appContext.filesDir, "photo_debug.log")
            f.appendText("${System.currentTimeMillis()} $msg\n")
        } catch (_: Exception) {}
    }

    private var lastSendTimeMs = 0L
    // Throttle для периодического сохранения черновика в Room (каждые 2 сек)
    @Volatile private var lastDraftSaveMs = 0L

    fun sendMessage(text: String) {
        debugLog("sendMessage ENTER text='${text.take(30)}' attachedUris=${_attachedImageUris.value.size}")
        if (text.isBlank() && _attachedImageUris.value.isEmpty()) return

        // Debounce: ignore rapid double-taps (< 600ms)
        val now = System.currentTimeMillis()
        if (now - lastSendTimeMs < 600) {
            debugLog("sendMessage DEBOUNCE skip (${now - lastSendTimeMs}ms)")
            return
        }
        lastSendTimeMs = now

        if (_isStreaming.value || _isLoading.value) {
            debugLog("sendMessage SKIP — already streaming/loading")
            return
        }
        
        val messageText = if (text.isBlank() && _attachedImageUris.value.isNotEmpty()) {
            "📷"
        } else {
            text
        }

        val isSilentUpload = text.isBlank() && _attachedImageUris.value.isNotEmpty()

        viewModelScope.launch {
            try {
                _isLoading.value = !isSilentUpload
                _isStreaming.value = true
                if (!isSilentUpload) _streamStatus.value = "thinking"
                _contentStarted.value = false
                _webSearchActive.value = false
                _imageExpectedInStream.value = false
                webSearchHideJob?.cancel()
                _thinkingSteps.value = emptyList()
                _thinkingChain.value = emptyList()
                _generatedFiles.value = emptyList()
                _currentEmotion.value = null
                _pendingConfirmation.value = null
                _confirmationProcessing.value = false
                _streamProgress.value = null
                _streamStatusLabel.value = null
                tokenDispatcherJob?.cancel()
                sseStreamDone = false
                _streamingContent.value = ""
                _streamingMessageId.value = null

                if (!isSilentUpload) ChatBackgroundService.start(appContext)
                
                // ВАЖНО: Сохраняем значения ДО любых операций!
                val currentAttachedUris = _attachedImageUris.value
                val convId = _currentConversationId.value
                
                // Конвертируем прикреплённые фото в base64 (одноразовое использование)
                val imagesToSend = if (currentAttachedUris.isNotEmpty()) {
                    currentAttachedUris.mapNotNull { uri ->
                        getAttachedImagesBase64(uri)?.firstOrNull()
                    }.takeIf { it.isNotEmpty() }
                } else null
                
                // Копируем фото во внутреннее хранилище для персистентности (content:// URIs истекают после перезапуска)
                val persistedImagePaths = if (currentAttachedUris.isNotEmpty()) {
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        currentAttachedUris.mapNotNull { uri -> copyPhotoToInternalStorage(uri) }
                    }.takeIf { it.isNotEmpty() }
                } else null
                
                // URIs для отображения — предпочитаем persistent paths, fallback на оригинальные
                val attachedImageUrisForDisplay = (persistedImagePaths ?: currentAttachedUris.map { it.toString() }).takeIf { it.isNotEmpty() }
                
                val hasAttachedImage = currentAttachedUris.isNotEmpty()

                // Add user message immediately with the URI for display
                val userMessageId = UUID.randomUUID().toString()
                
                val userMessage = MessageDTO(
                    id = userMessageId,
                    content = messageText,
                    role = "user",
                    agentName = null,
                    provider = null,
                    providerColor = null,
                    modelUsed = null,
                    createdAt = System.currentTimeMillis().toString(),
                    imageUrl = null,
                    images = attachedImageUrisForDisplay
                )
                _messages.value = _messages.value + userMessage
                
                // Clear old Failed statuses when sending new message
                _messageSendStatus.value = _messageSendStatus.value.filterValues { it != MessageSendStatus.Failed }
                updateMessageStatus(userMessageId, MessageSendStatus.Sending)
                _currentMessage.value = ""

                // Clear attached images after sending (one-time use)
                _attachedImageUris.value = emptyList()

                // Для СУЩЕСТВУЮЩИХ чатов (convId != null) — сохраняем user msg
                // в Room сразу. Для НОВЫХ чатов — сохраним в onConversation, когда
                // сервер пришлёт свой conversation ID.
                if (convId != null) {
                    android.util.Log.w("SSE_PERSIST", "Saving user msg $userMessageId to conv $convId")
                    kotlinx.coroutines.withContext(Dispatchers.IO) {
                        chatRepository.saveUserMessageNow(
                            conversationId = convId,
                            messageId = userMessageId,
                            content = messageText,
                            images = attachedImageUrisForDisplay
                        )
                    }
                    android.util.Log.w("SSE_PERSIST", "User msg saved OK")
                } else {
                    android.util.Log.w("SSE_PERSIST", "New chat — user msg will be saved in onConversation")
                }

                // Streaming message state
                val streamingMessageId = UUID.randomUUID().toString()
                val contentBuilder = StringBuilder()
                var messageAdded = false
                var currentImageUrl: String? = null
                
                // SSE streaming - just accumulate text, animation is in Composable
                chatRepository.sendMessageStream(
                    message = messageText,
                    conversationId = convId,
                    images = imagesToSend,
                    attachmentOnly = isSilentUpload,
                    onStatus = { statusText ->
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            if (statusText.isBlank() || statusText == "done") return@launch
                            // Детектируем генерацию изображений по ключевым словам
                            val lower = statusText.lowercase()
                            if (lower.contains("изображен") || lower.contains("image") ||
                                lower.contains("рисую") || lower.contains("генерир") ||
                                lower.contains("редактир")) {
                                _imageExpectedInStream.value = true
                            }
                            _streamStatus.value = statusText
                        }
                    },
                    onToken = { token ->
                        contentBuilder.append(token)

                        // ── Периодически сохраняем черновик в Room DB (каждые 2 сек) ──
                        // Если OEM убьёт процесс во время стриминга, частичный ответ
                        // сохранится и будет показан при следующем запуске приложения.
                        val nowMs = System.currentTimeMillis()
                        val draftConvId = _currentConversationId.value
                        if (nowMs - lastDraftSaveMs > 2000 && draftConvId != null) {
                            lastDraftSaveMs = nowMs
                            val draftContent = contentBuilder.toString()
                            viewModelScope.launch(Dispatchers.IO) {
                                chatRepository.upsertStreamingDraft(
                                    conversationId = draftConvId,
                                    messageId = streamingMessageId,
                                    content = draftContent,
                                    imageUrl = currentImageUrl,
                                    filesJson = null
                                )
                            }
                        }

                        // ═══ ЕДИНЫЙ DISPLAY LOOP ═══
                        // OkHttp вызывает onEvent пачкой на background-потоке.
                        // Если запускать корутину на каждый токен — contentBuilder
                        // уже полный к моменту выполнения на Main → всё появляется разом.
                        // 
                        // Решение: ОДИН цикл на Main, который каждые ~30мс
                        // синхронизирует UI с тем что накопилось в contentBuilder.
                        // Реальный стриминг (токены раз в 100мс): UI обновляется мгновенно.
                        // Пачка (все токены разом): разбивается на ~30мс кадры.
                        
                        if (tokenDispatcherJob?.isActive != true) {
                            // Добавляем placeholder в _messages ОДИН РАЗ — дальше контент идёт
                            // через _streamingContent чтобы не пересоздавать весь список каждые 20мс
                            if (!messageAdded) {
                                messageAdded = true
                                _isLoading.value = false
                                _streamingMessageId.value = streamingMessageId
                                _streamingContent.value = ""
                                _messages.value = _messages.value + MessageDTO(
                                    id = streamingMessageId,
                                    content = "",
                                    role = "assistant",
                                    agentName = "streaming",
                                    provider = null, providerColor = null, modelUsed = null,
                                    createdAt = System.currentTimeMillis().toString(),
                                    imageUrl = currentImageUrl
                                )
                            }
                            tokenDispatcherJob = viewModelScope.launch(Dispatchers.Default) {
                                _contentStarted.value = true
                                var displayedLength = 0

                                while (true) {
                                    val totalLength = contentBuilder.length

                                    if (displayedLength < totalLength) {
                                        val newChars = totalLength - displayedLength
                                        val showUpTo = if (newChars <= 6) totalLength
                                                       else minOf(displayedLength + 20, totalLength)

                                        val displayed = contentBuilder.substring(0, showUpTo)
                                        displayedLength = showUpTo

                                        // Image URL из markdown
                                        if (currentImageUrl == null && _imageExpectedInStream.value) {
                                            val mdMatch = Regex("""!\[.*?]\((https?://[^)]+)\)""").find(contentBuilder)
                                            if (mdMatch != null) currentImageUrl = mdMatch.groupValues[1]
                                        }

                                        // Обновляем ТОЛЬКО текст стрима — _messages не трогаем
                                        _streamingContent.value = displayed

                                        kotlinx.coroutines.delay(20L)
                                    } else if (sseStreamDone) {
                                        break
                                    } else {
                                        kotlinx.coroutines.delay(16L)
                                    }
                                }
                            }
                        }
                    },
                    onImage = { url, prompt ->
                        Timber.d("Image received: url=$url")
                        val apiHost = BuildConfig.API_BASE_URL.substringBefore("/api/")
                        val fullUrl = when {
                            url.startsWith("http") -> url
                            url.startsWith("/") -> "$apiHost$url"
                            else -> "$apiHost/$url"
                        }
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
                                    agentName = "streaming",
                                    provider = null,
                                    providerColor = null,
                                    modelUsed = null,
                                    createdAt = System.currentTimeMillis().toString(),
                                    imageUrl = fullUrl
                                )
                                _messages.value = _messages.value + msg
                            } else {
                                // Update with image URL
                                _messages.value = _messages.value.map { m ->
                                    if (m.id == streamingMessageId) m.copy(imageUrl = fullUrl) else m
                                }
                            }
                        }
                    },
                    onCitations = { citations ->
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            // Update streaming message with citations
                            _messages.value = _messages.value.map { m ->
                                if (m.id == streamingMessageId) m.copy(citations = citations) else m
                            }
                        }
                    },
                    onAgentStep = { step ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _thinkingSteps.value = _thinkingSteps.value + step
                            
                            // Определяем веб-поиск по содержимому agent_step
                            val detail = step.detail
                            val isWebSearch = detail.contains("🌐") || 
                                detail.contains("ищу в интернете", true) ||
                                detail.contains("ищу актуальную", true) ||
                                detail.contains("web_search", true) ||
                                detail.contains("searching", true) ||
                                detail.contains("нашла информацию в интернете", true) ||
                                detail.contains("нашёл информацию в интернете", true) ||
                                detail.contains("информацию в интернете", true)
                            
                            if (isWebSearch) {
                                _webSearchActive.value = true
                                // Отменяем предыдущий таймер если есть
                                webSearchHideJob?.cancel()
                                // Минимум 2.5 секунды показа
                                webSearchHideJob = viewModelScope.launch {
                                    kotlinx.coroutines.delay(2500L)
                                    _webSearchActive.value = false
                                }
                            }
                            
                            kotlinx.coroutines.yield()
                        }
                    },
                    onFile = { file ->
                        viewModelScope.launch(Dispatchers.Main) {
                            // Deduplicate by URL (file_ready + file_url both call this)
                            if (_generatedFiles.value.none { it.url == file.url }) {
                                _generatedFiles.value = _generatedFiles.value + file
                                // Immediately attach to the streaming bubble — don't wait for done
                                _messages.value = _messages.value.map { m ->
                                    if (m.id == streamingMessageId) {
                                        val existing = m.files.orEmpty()
                                        if (existing.none { it.url == file.url }) {
                                            m.copy(files = existing + file)
                                        } else m
                                    } else m
                                }
                            }
                        }
                    },
                    onEmotion = { emotionEvent ->
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            _currentEmotion.value = emotionEvent
                        }
                    },
                    onCanvasUpdate = { action, payloadJson ->
                        viewModelScope.launch {
                            _canvasEvents.emit(Pair(action, payloadJson))
                        }
                    },
                    onConfirmation = { event ->
                        pendingConfirmationUserMessage = messageText
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            _pendingConfirmation.value = event
                        }
                    },
                    onProgress = { progress ->
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            _streamProgress.value = progress
                        }
                    },
                    onThinkingChain = { step ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _thinkingChain.value = _thinkingChain.value + step
                        }
                    },
                    onConversation = { serverConvId, aiTitle ->
                        debugLog("onConversation: serverConvId=$serverConvId, originalConvId=$convId, hasImages=${attachedImageUrisForDisplay != null}")
                        isNewChatMode = false  // чат начался, снимаем блокировку
                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            if (_currentConversationId.value != serverConvId) {
                                _currentConversationId.value = serverConvId
                                savedStateHandle["conversationId"] = serverConvId
                            }
                        }
                        viewModelScope.launch(Dispatchers.IO) {
                            val effectiveTitle = aiTitle.ifBlank {
                                if (hasAttachedImage) "Фото" else "Новый чат"
                            }
                            chatRepository.upsertConversationWithTitle(serverConvId, effectiveTitle)
                            // Новый разговор: convId был null, early save пропущен — сохраняем сейчас
                            if (convId == null) {
                                android.util.Log.w("SSE_PERSIST", "onConversation: saving user msg for NEW conv $serverConvId")
                                chatRepository.saveUserMessageNow(
                                    conversationId = serverConvId,
                                    messageId = userMessageId,
                                    content = messageText,
                                    images = attachedImageUrisForDisplay
                                )
                                android.util.Log.w("SSE_PERSIST", "onConversation: user msg saved OK")
                            }
                        }
                    },
                    onDone = { messageId, content, newConversationId, serverUserMessageId ->
                        // Убираем голые URL файлов из текста — они уже показаны как кнопка скачивания
                        val fileUrls = _generatedFiles.value.map { it.url }.toSet()
                        val rawContent = content.ifEmpty { contentBuilder.toString() }
                        val finalContent = if (fileUrls.isNotEmpty()) {
                            var cleaned = rawContent
                            fileUrls.forEach { url ->
                                cleaned = cleaned.replace(url, "").trimEnd()
                            }
                            // Убираем оставшиеся одиночные bare-URL строки вида http://...
                            cleaned = cleaned.lines().filter { line ->
                                val trimmed = line.trim()
                                !(trimmed.startsWith("http://") || trimmed.startsWith("https://")) ||
                                    trimmed.contains(" ")  // оставляем если не одиночная ссылка
                            }.joinToString("\n").trimEnd()
                            cleaned
                        } else rawContent
                        sseStreamDone = true
                        
                        viewModelScope.launch(Dispatchers.Main) {
                            tokenDispatcherJob?.join()
                            val isNewConversation = newConversationId != null && 
                                newConversationId.isNotBlank() && 
                                _currentConversationId.value != newConversationId
                                
                            val convId = if (newConversationId != null && newConversationId.isNotBlank()) {
                                val oldId = _currentConversationId.value
                                if (oldId != newConversationId) {
                                    Timber.d("Server assigned conversation_id: $newConversationId (was: $oldId)")
                                    _currentConversationId.value = newConversationId
                                    savedStateHandle["conversationId"] = newConversationId
                                }
                                newConversationId
                            } else {
                                val localId = _currentConversationId.value ?: UUID.randomUUID().toString()
                                if (_currentConversationId.value == null) {
                                    _currentConversationId.value = localId
                                    savedStateHandle["conversationId"] = localId
                                }
                                localId
                            }
                            
                            val canonicalUserMsgId = serverUserMessageId?.takeIf { it.isNotBlank() } ?: userMessageId

                            if (isSilentUpload) {
                                // Silent upload: no assistant message, just update user msg ID and clean up
                                _messages.value = _messages.value
                                    .filter { it.id != streamingMessageId }
                                    .map { m -> if (m.id == userMessageId) m.copy(id = canonicalUserMsgId) else m }
                                updateMessageStatus(canonicalUserMsgId, MessageSendStatus.Sent)
                                _streamStatus.value = ""
                                viewModelScope.launch(Dispatchers.IO) {
                                    chatRepository.saveUserMessageNow(
                                        conversationId = convId,
                                        messageId = canonicalUserMsgId,
                                        content = messageText,
                                        images = attachedImageUrisForDisplay,
                                        oldMessageId = if (canonicalUserMsgId != userMessageId) userMessageId else null
                                    )
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        _isStreaming.value = false
                                        _isLoading.value = false
                                    }
                                }
                                refreshConversations()
                                return@launch
                            }

                            // Extract image URL from markdown content if onImage never fired
                            if (currentImageUrl == null) {
                                val mdImg = Regex("""!\[.*?]\((https?://[^)]+)\)""").find(finalContent)
                                if (mdImg != null) {
                                    currentImageUrl = mdImg.groupValues[1]
                                    android.util.Log.d("SSE_DBG", "Extracted imageUrl from markdown: ${currentImageUrl?.takeLast(40)}")
                                }
                            }

                            // Сбрасываем streaming-поток — финальный контент идёт в _messages
                            _streamingMessageId.value = null
                            _streamingContent.value = ""

                            // Normal message: finalize streaming assistant message
                            _messages.value = _messages.value.map { m ->
                                when (m.id) {
                                    userMessageId -> m.copy(id = canonicalUserMsgId)
                                    streamingMessageId -> m.copy(
                                        content = finalContent,
                                        agentName = null,
                                        imageUrl = currentImageUrl
                                    )
                                    else -> m
                                }
                            }
                            
                            updateMessageStatus(canonicalUserMsgId, MessageSendStatus.Sent)
                            
                            _streamStatus.value = ""
                            _streamProgress.value = null
                            _streamStatusLabel.value = null

                            viewModelScope.launch(Dispatchers.IO) {
                                debugLog("onDone: convId=$convId, canonicalUserMsgId=$canonicalUserMsgId, origId=$userMessageId, hasImages=${attachedImageUrisForDisplay != null}")
                                // Если сервер присвоил новый ID пользовательскому сообщению —
                                // удаляем старую запись и вставляем с каноническим ID.
                                // Без этого в Room остаются ОБА ID → дублирование в UI.
                                if (canonicalUserMsgId != userMessageId) {
                                    android.util.Log.w("SSE_PERSIST", "onDone: user msg ID changed $userMessageId→$canonicalUserMsgId, re-saving")
                                    chatRepository.saveUserMessageNow(
                                        conversationId = convId,
                                        messageId = canonicalUserMsgId,
                                        content = text,
                                        images = attachedImageUrisForDisplay,
                                        oldMessageId = userMessageId
                                    )
                                }
                                debugLog("onDone: calling saveStreamedMessages conv=$convId userMsgId=$canonicalUserMsgId images=${attachedImageUrisForDisplay?.size}")
                                // Снимаем флаг черновика ПЕРЕД финальным сохранением,
                                // чтобы Room-флоу не показал черновик после insertAll
                                chatRepository.clearStreamingDrafts(convId)
                                chatRepository.saveStreamedMessages(
                                    conversationId = convId,
                                    userMessage = text,
                                    userMessageId = canonicalUserMsgId,
                                    assistantMessageId = messageId.ifEmpty { streamingMessageId },
                                    assistantContent = finalContent,
                                    imageUrl = currentImageUrl,
                                    userImages = attachedImageUrisForDisplay,
                                    generatedFiles = _generatedFiles.value.takeIf { it.isNotEmpty() },
                                    oldUserMessageId = if (canonicalUserMsgId != userMessageId) userMessageId else null
                                )
                                debugLog("onDone: saveStreamedMessages DONE")
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    lastStreamEndMs = System.currentTimeMillis()
                                    _isStreaming.value = false
                                    _messageCompleted.tryEmit(Unit)

                                    if (!appLifecycleTracker.isInForeground) {
                                        val wasImageGeneration = _imageExpectedInStream.value
                                        val notificationText = when {
                                            wasImageGeneration && currentImageUrl != null -> "🎨 Изображение сгенерировано"
                                            wasImageGeneration -> null
                                            finalContent.isNotBlank() -> finalContent.trim()
                                            else -> null
                                        }
                                        if (notificationText != null) {
                                            val conversationTitle = _conversations.value
                                                .find { it.id == convId }?.title
                                            notificationHelper.showChatResponseNotification(
                                                messagePreview = notificationText,
                                                conversationTitle = conversationTitle
                                            )
                                        }
                                    }
                                }

                                ChatBackgroundService.stop(appContext)

                                // Title is already set via the "conversation" SSE event (AI-generated).
                                // regenerateTitle is no longer needed for new conversations.
                            }
                            
                            // Refresh conversations list to show new one
                            refreshConversations()
                        }
                    },
                    onError = { errorMsg ->
                        // onDone вызывает eventSource.cancel() → OkHttp вызывает
                        // onFailure("Canceled") → этот onError. Если onDone уже
                        // обработал результат — игнорируем.
                        if (sseStreamDone) {
                            android.util.Log.w("SSE_PERSIST", "onError IGNORED (sseStreamDone=true): '$errorMsg'")
                            return@sendMessageStream
                        }
                        android.util.Log.w("SSE_PERSIST", "onError: '$errorMsg' hasContent=$messageAdded contentLen=${contentBuilder.length} convId=${_currentConversationId.value}")
                        val hasContent = messageAdded || contentBuilder.isNotEmpty()

                        // Classify as a network/background interruption rather than a real error.
                        // This covers OkHttp socket closes, Doze-mode interruptions, and server
                        // keep-alive disconnects that are indistinguishable from real failures.
                        val isBackgroundError = errorMsg.contains("Socket", true) ||
                            errorMsg.contains("cancel", true) ||
                            errorMsg.contains("reset", true) ||
                            errorMsg.contains("closed", true) ||
                            errorMsg.contains("stream", true) ||
                            errorMsg.contains("EOF", true) ||
                            errorMsg.contains("broken pipe", true) ||
                            errorMsg.contains("connection abort", true) ||
                            errorMsg.contains("timeout", true) ||
                            errorMsg.contains("Превышено", true) ||
                            errorMsg.contains("interrupt", true)

                        if (hasContent && isBackgroundError) {
                            // Partial content received — save what we have, no visible error
                            Timber.w("Background interrupt with partial content — saving gracefully")
                            val savedContent = contentBuilder.toString()
                            // Persist partial content to DB immediately so it survives process death
                            val errorConvId = _currentConversationId.value
                            if (savedContent.isNotBlank() && errorConvId != null) {
                                viewModelScope.launch(Dispatchers.IO) {
                                    chatRepository.upsertStreamingDraft(
                                        conversationId = errorConvId,
                                        messageId = streamingMessageId,
                                        content = savedContent,
                                        imageUrl = currentImageUrl,
                                        filesJson = null
                                    )
                                    chatRepository.clearStreamingDrafts(errorConvId)
                                    chatRepository.saveStreamedMessages(
                                        conversationId = errorConvId,
                                        userMessage = text,
                                        userMessageId = userMessageId,
                                        assistantMessageId = streamingMessageId,
                                        assistantContent = savedContent,
                                        imageUrl = currentImageUrl,
                                        userImages = attachedImageUrisForDisplay,
                                        generatedFiles = _generatedFiles.value.takeIf { it.isNotEmpty() }
                                    )
                                }
                            }
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                if (savedContent.isNotBlank()) {
                                    _messages.value = _messages.value.map { msg ->
                                        if (msg.id == streamingMessageId) msg.copy(
                                            content = savedContent,
                                            agentName = null,
                                            imageUrl = currentImageUrl
                                        ) else msg
                                    }
                                }
                                _streamStatus.value = ""
                                _isStreaming.value = false
                                _isLoading.value = false
                                ChatBackgroundService.stop(appContext)
                            }
                            return@sendMessageStream
                        }

                        viewModelScope.launch(Dispatchers.Main.immediate) {
                            // Stale conversation → clear ID, remove user msg, retry cleanly
                            val hadConvId = _currentConversationId.value != null
                            if (hadConvId && (errorMsg.contains("not found", true) || errorMsg.contains("404"))) {
                                Timber.w("Conversation not found, clean retry...")
                                _currentConversationId.value = null
                                savedStateHandle["conversationId"] = null
                                _messages.value = _messages.value.filterNot {
                                    it.id == streamingMessageId || it.id == userMessageId
                                }
                                _streamStatus.value = ""
                                _isStreaming.value = false
                                _isLoading.value = false
                                _thinkingSteps.value = emptyList()
                                tokenDispatcherJob?.cancel()
                                sseStreamDone = false
                                ChatBackgroundService.stop(appContext)
                                sendMessage(text)
                                return@launch
                            }

                            // Connection dropped during thinking/generation phase (no tokens yet).
                            // Это происходит когда OEM (Realme/ColorOS OplusHansManager) замораживает
                            // процесс — TCP-сокет рвётся, но сервер продолжает генерацию.
                            if (!hasContent && isBackgroundError) {
                                android.util.Log.w("SSE_PERSIST", "BG interrupt: no content, scheduling sync for conv=${_currentConversationId.value}")
                                _messages.value = _messages.value.filterNot { it.id == streamingMessageId }
                                _messageSendStatus.value = _messageSendStatus.value
                                    .toMutableMap().apply { remove(userMessageId) }
                                _streamStatus.value = ""
                                _isLoading.value = false
                                _thinkingSteps.value = emptyList()
                                ChatBackgroundService.stop(appContext)

                                // Сервер мог завершить генерацию пока мы были заморожены.
                                // Синхронизируем ПЕРЕД тем как отпустить isStreaming, чтобы
                                // Room-Flow не затёр _messages пустым списком.
                                val syncConvId = _currentConversationId.value
                                if (syncConvId != null) {
                                    viewModelScope.launch(Dispatchers.IO) {
                                        // Опрашиваем сервер в цикле, пока не появится
                                        // ответ ассистента на наше сообщение.
                                        // Нужно для длинных генераций (изображения, >60с)
                                        // когда OEM (Realme) рвёт сокет.
                                        // Считаем от СЕРВЕРА — не от Room, чтобы orphan deletion
                                        // не сбивала счётчик. -1 означает ошибку запроса.
                                        val msgCountBefore = chatRepository.fetchServerMessageCount(syncConvId)
                                        var delayMs = 3_000L
                                        val maxTotalMs = 10 * 60 * 1000L // 10 минут для долгих задач
                                        val startMs = System.currentTimeMillis()
                                        var attempt = 0
                                        var foundNewMessage = false

                                        android.util.Log.w("SSE_PERSIST", "Poll start: server has $msgCountBefore msgs for $syncConvId")

                                        while (System.currentTimeMillis() - startMs < maxTotalMs) {
                                            attempt++
                                            kotlinx.coroutines.delay(delayMs)
                                            delayMs = (delayMs * 1.5).toLong().coerceAtMost(30_000L)
                                            try {
                                                val serverCount = chatRepository.fetchServerMessageCount(syncConvId)
                                                android.util.Log.w("SSE_PERSIST", "Poll #$attempt: server=$serverCount before=$msgCountBefore")
                                                if (serverCount > msgCountBefore) {
                                                    android.util.Log.w("SSE_PERSIST", "New message on server! Syncing to Room.")
                                                    foundNewMessage = true
                                                    break
                                                }
                                                // server вернул -1 (ошибка) — продолжаем
                                            } catch (e: Exception) {
                                                android.util.Log.w("SSE_PERSIST", "Poll #$attempt FAILED: ${e.message}")
                                            }
                                        }

                                        // Теперь делаем полный sync (с записью в Room)
                                        if (foundNewMessage) {
                                            try {
                                                chatRepository.syncConversationMessages(syncConvId)
                                                android.util.Log.w("SSE_PERSIST", "Full sync after poll OK")
                                            } catch (e: Exception) {
                                                android.util.Log.w("SSE_PERSIST", "Full sync after poll FAILED: ${e.message}")
                                            }
                                        }

                                        // КРИТИЧНО: Room Flow заблокирован пока _isStreaming=true.
                                        // Принудительно загружаем данные через .first() и пишем в _messages.
                                        val freshMessages = try {
                                            chatRepository.getConversationMessages(syncConvId).first()
                                        } catch (e: Exception) {
                                            android.util.Log.w("SSE_PERSIST", "Force-load FAILED: ${e.message}")
                                            emptyList()
                                        }
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            _isStreaming.value = false
                                            if (freshMessages.isNotEmpty()) {
                                                android.util.Log.w("SSE_PERSIST", "Force-loaded ${freshMessages.size} msgs into UI (found=$foundNewMessage)")
                                                _messages.value = freshMessages
                                            }
                                            if (foundNewMessage) {
                                                val lastAssistant = freshMessages.lastOrNull { it.role == "assistant" }
                                                if (lastAssistant != null) {
                                                    val preview = lastAssistant.imageUrl?.let { "🎨 Изображение готово" }
                                                        ?: lastAssistant.content.trim().take(120).ifBlank { null }
                                                    if (preview != null) {
                                                        notificationHelper.showChatResponseNotification(
                                                            messagePreview = preview,
                                                            conversationTitle = _conversations.value.find { it.id == syncConvId }?.title
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    _isStreaming.value = false
                                }
                                return@launch
                            }

                            // Genuine server/auth error — show to user
                            _messages.value = _messages.value.filterNot { it.id == streamingMessageId }
                            updateMessageStatus(userMessageId, MessageSendStatus.Failed)
                            _uiState.value = ChatUiState.Error(errorMsg)
                            _streamStatus.value = ""
                            _isStreaming.value = false
                            _isLoading.value = false
                            ChatBackgroundService.stop(appContext)
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
                ChatBackgroundService.stop(appContext)
            }
        }
    }

    fun approveConfirmation() {
        val event = _pendingConfirmation.value ?: return
        _confirmationProcessing.value = true
        viewModelScope.launch {
            chatRepository.confirmAction(event.confirmationId, approved = true)
            _pendingConfirmation.value = null
            _confirmationProcessing.value = false
            val originalMsg = pendingConfirmationUserMessage
            pendingConfirmationUserMessage = null
            if (!originalMsg.isNullOrBlank()) {
                sendMessage(originalMsg)
            }
        }
    }

    fun rejectConfirmation() {
        val event = _pendingConfirmation.value ?: return
        _confirmationProcessing.value = true
        viewModelScope.launch {
            chatRepository.confirmAction(event.confirmationId, approved = false)
            _pendingConfirmation.value = null
            _confirmationProcessing.value = false
            pendingConfirmationUserMessage = null
            val cancelMsg = MessageDTO(
                id = UUID.randomUUID().toString(),
                content = "🚫 Действие отменено",
                role = "assistant",
                agentName = "system",
                createdAt = System.currentTimeMillis().toString()
            )
            _messages.value = _messages.value + cancelMsg
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
                    _attachedImageUris.value = listOf(android.net.Uri.parse(imageUri))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to restore attached image")
                }
            }
        }
        
        sendMessage(content)
    }

    fun createNewConversation() {
        // Не создаём новый диалог пока идёт стриминг в текущем!
        if (_isStreaming.value) {
            Timber.w("Cannot create new conversation while streaming")
            return
        }
        
        // Don't create conversation on backend yet - it will be created with first message
        // Just clear current state for new chat
        isNewChatMode = true  // блокируем авто-выбор предыдущего диалога
        _messages.value = emptyList()
        _currentMessage.value = ""
        _currentConversationId.value = null
        savedStateHandle["conversationId"] = null
        _uiState.value = ChatUiState.Success
        _streamStatus.value = ""
        _isLoading.value = false
        _thinkingSteps.value = emptyList()
        _attachedImageUris.value = emptyList()

        Timber.d("New conversation started (will be created on first message)")
    }

    fun selectConversation(conversationId: String) {
        if (_currentConversationId.value == conversationId) return
        debugLog("selectConversation: $conversationId (was ${_currentConversationId.value})")
        
        _isStreaming.value = false
        _streamStatus.value = ""
        _isLoading.value = false
        _uiState.value = ChatUiState.Success
        _thinkingSteps.value = emptyList()
        _attachedImageUris.value = emptyList()
        _messages.value = emptyList()
        
        _currentConversationId.value = conversationId
        savedStateHandle["conversationId"] = conversationId
        
        viewModelScope.launch(Dispatchers.IO) {
            // Сначала подгружаем из локальной БД мгновенно
            val cached = chatRepository.getConversationMessages(conversationId).first()
            if (cached.isNotEmpty() && _currentConversationId.value == conversationId) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _messages.value = cached
                }
            }
            // Потом синхронизируем с сервером
            val result = chatRepository.syncConversationMessages(conversationId)
            result.onFailure { e -> 
                Timber.e(e, "Sync failed for $conversationId")
                if (e.message?.contains("404") == true || e.message?.contains("not found", ignoreCase = true) == true) {
                    if (_messages.value.isEmpty()) {
                        chatRepository.deleteLocalConversation(conversationId)
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            _currentConversationId.value = null
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Upload file from Uri (gallery, file picker, etc.)
     * Active chat (convId != null): any file type → /attachments/{convId}/upload (session context).
     * No active conversation: → /documents/upload/ (storage / RAG).
     */
    fun uploadFile(uri: Uri) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uiState.value = ChatUiState.Success

                val mimeType = appContext.contentResolver.getType(uri)
                val isImage = mimeType?.startsWith("image/") == true
                val convId = _currentConversationId.value

                if (convId != null) {
                    // Any file in an active chat → session context attachment
                    val persistentPath = if (isImage) {
                        kotlinx.coroutines.withContext(Dispatchers.IO) { copyPhotoToInternalStorage(uri) }
                    } else null

                    val result = attachmentsRepository.uploadAttachment(convId, uri, AttachmentMode.CONTEXT)
                    result.onSuccess { attachment ->
                        startAttachmentPolling(convId, attachment)
                        val imageList = if (isImage && persistentPath != null) listOf(persistentPath) else null
                        val messageId = UUID.randomUUID().toString()
                        val message = MessageDTO(
                            id = messageId,
                            content = if (isImage) "" else "📎 Документ добавлен в контекст: ${attachment.filename}",
                            role = "user",
                            agentName = null,
                            provider = null,
                            providerColor = null,
                            modelUsed = null,
                            createdAt = System.currentTimeMillis().toString(),
                            images = imageList
                        )
                        if (imageList != null) {
                            kotlinx.coroutines.withContext(Dispatchers.IO) {
                                chatRepository.saveUserMessageNow(
                                    conversationId = convId,
                                    messageId = messageId,
                                    content = message.content,
                                    images = imageList
                                )
                            }
                        }
                        _messages.value = _messages.value + message
                    }.onFailure { e ->
                        Timber.e(e, "Failed to upload attachment")
                        _uiState.value = ChatUiState.Error("Ошибка загрузки: ${e.message}")
                    }
                } else {
                    // No active conversation → storage / RAG
                    val persistentPath = if (isImage) {
                        kotlinx.coroutines.withContext(Dispatchers.IO) { copyPhotoToInternalStorage(uri) }
                    } else null

                    val result = documentRepository.uploadDocumentFromUri(uri)
                    result.onSuccess { response ->
                        _uploadedFiles.value = _uploadedFiles.value + response.filename

                        val imageList = if (isImage && persistentPath != null) listOf(persistentPath) else null
                        val messageId = UUID.randomUUID().toString()
                        val message = MessageDTO(
                            id = messageId,
                            content = if (isImage) "" else "📎 Файл загружен: ${response.filename}",
                            role = if (isImage) "user" else "assistant",
                            agentName = if (isImage) null else "system",
                            provider = if (isImage) null else "system",
                            providerColor = null,
                            modelUsed = null,
                            createdAt = System.currentTimeMillis().toString(),
                            images = imageList
                        )
                        _messages.value = _messages.value + message
                    }.onFailure { e ->
                        Timber.e(e, "Failed to upload file")
                        _uiState.value = ChatUiState.Error("Ошибка загрузки: ${e.message}")
                    }
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
     * Upload multiple files/photos to session context.
     * Active chat (convId != null): any file type → /attachments/{convId}/upload (session context).
     * No active conversation: → /documents/upload/ (storage / RAG).
     */
    fun uploadMultipleFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            try {
                _isUploading.value = true
                _uiState.value = ChatUiState.Success

                val convId = _currentConversationId.value
                val persistentPaths = mutableListOf<String>()
                val uploadedNames = mutableListOf<String>()

                for (uri in uris) {
                    val mimeType = appContext.contentResolver.getType(uri)
                    val isImage = mimeType?.startsWith("image/") == true

                    if (convId != null) {
                        // Active chat → session context attachment (images + documents)
                        if (isImage) {
                            val path = kotlinx.coroutines.withContext(Dispatchers.IO) { copyPhotoToInternalStorage(uri) }
                            if (path != null) persistentPaths.add(path)
                        }
                        val result = attachmentsRepository.uploadAttachment(convId, uri, AttachmentMode.CONTEXT)
                        result.onSuccess { attachment ->
                            startAttachmentPolling(convId, attachment)
                            if (!isImage) uploadedNames.add(attachment.filename)
                        }.onFailure { e ->
                            Timber.e(e, "Failed to upload attachment: $uri")
                        }
                    } else {
                        // No active conversation → storage / RAG
                        if (isImage) {
                            val path = kotlinx.coroutines.withContext(Dispatchers.IO) { copyPhotoToInternalStorage(uri) }
                            if (path != null) persistentPaths.add(path)
                        }
                        val result = documentRepository.uploadDocumentFromUri(uri)
                        result.onSuccess { response ->
                            _uploadedFiles.value = _uploadedFiles.value + response.filename
                        }.onFailure { e ->
                            Timber.e(e, "Failed to upload file: $uri")
                        }
                    }
                }

                // Show grouped image preview message
                if (persistentPaths.isNotEmpty()) {
                    val countText = if (persistentPaths.size == 1) "Фото" else "${persistentPaths.size} фото"
                    val messageId = UUID.randomUUID().toString()
                    val message = MessageDTO(
                        id = messageId,
                        content = "📷 $countText добавлено в контекст",
                        role = "user",
                        agentName = null,
                        provider = null,
                        providerColor = null,
                        modelUsed = null,
                        createdAt = System.currentTimeMillis().toString(),
                        images = persistentPaths
                    )
                    if (convId != null) {
                        kotlinx.coroutines.withContext(Dispatchers.IO) {
                            chatRepository.saveUserMessageNow(
                                conversationId = convId,
                                messageId = messageId,
                                content = message.content,
                                images = persistentPaths
                            )
                        }
                    }
                    _messages.value = _messages.value + message
                }

                // Show document confirmation message
                if (uploadedNames.isNotEmpty()) {
                    val label = if (uploadedNames.size == 1) uploadedNames.first() else "${uploadedNames.size} документа(ов)"
                    val messageId = UUID.randomUUID().toString()
                    val message = MessageDTO(
                        id = messageId,
                        content = "📎 Документ(ы) добавлены в контекст: $label",
                        role = "user",
                        agentName = null,
                        provider = null,
                        providerColor = null,
                        modelUsed = null,
                        createdAt = System.currentTimeMillis().toString(),
                        images = null
                    )
                    _messages.value = _messages.value + message
                }

            } catch (e: Exception) {
                Timber.e(e, "Upload multiple files failed")
                _uiState.value = ChatUiState.Error("Ошибка: ${e.message}")
            } finally {
                _isUploading.value = false
            }
        }
    }
    
    // Track the recording start job so stopVoiceInput can wait for it
    private var recordingStartJob: Job? = null

    /**
     * Start voice recording (press-and-hold)
     */
    fun startVoiceInput() {
        recordingStartJob = viewModelScope.launch {
            if (voiceRepository.isRecording.value) return@launch
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

    /**
     * Stop voice recording and transcribe (on release)
     * Waits for startVoiceInput to finish before stopping.
     */
    fun stopVoiceInput(autoSend: Boolean) {
        viewModelScope.launch {
            // Wait for recording to actually start before trying to stop
            recordingStartJob?.join()
            recordingStartJob = null

            if (!voiceRepository.isRecording.value) return@launch
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
        }
    }

    /**
     * Cancel voice recording (e.g. too short hold)
     */
    fun cancelVoiceInput() {
        viewModelScope.launch {
            recordingStartJob?.join()
            recordingStartJob = null
            if (voiceRepository.isRecording.value) {
                voiceRepository.cancelRecording()
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

    fun deleteAllConversations() {
        viewModelScope.launch {
            val ids = _conversations.value.map { it.id }
            ids.forEach { id ->
                chatRepository.deleteConversation(id)
                    .onFailure { chatRepository.deleteLocalConversation(id) }
            }
            _messages.value = emptyList()
            _currentConversationId.value = null
            savedStateHandle["conversationId"] = null
        }
    }
    
    /**
     * Delete a single message from chat
     * Message is removed from UI immediately with animation,
     * and marked as deleted on backend (excluded from context)
     */
    fun deleteMessage(messageId: String) {
        deleteMessages(setOf(messageId))
    }

    /**
     * Delete multiple messages at once (batch).
     * All messages are removed from UI in a single atomic update,
     * then each is deleted on the backend sequentially.
     */
    fun deleteMessages(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        viewModelScope.launch {
            val conversationId = _currentConversationId.value ?: return@launch

            _messages.value = _messages.value.filterNot { it.id in messageIds }

            for (id in messageIds) {
                try {
                    chatRepository.deleteMessage(conversationId, id)
                    Timber.d("Message deleted: $id")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete message from backend: $id")
                }
            }
        }
    }
    
    // ==================== Image Attachment ====================
    
    /**
     * Attach image to message (backend decides: vision / edit / ignore)
     */
    fun attachImage(uri: Uri) {
        _attachedImageUris.value = _attachedImageUris.value + uri
    }
    
    /**
     * Attach multiple images at once
     */
    fun attachImages(uris: List<Uri>) {
        _attachedImageUris.value = _attachedImageUris.value + uris
    }
    
    /**
     * Remove attached (pending) image by index
     */
    fun removeAttachedImage(index: Int = 0) {
        val current = _attachedImageUris.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _attachedImageUris.value = current
        }
        // no-op if empty
    }
    
    /**
     * Clear all attached images
     */
    fun clearAttachedImages() {
        _attachedImageUris.value = emptyList()
    }
    
    /**
     * Upload attached images to session context (WITHOUT AI response)
     * Uses /attachments/{conversation_id}/upload endpoint (NOT /documents/upload)
     * Shows photos IMMEDIATELY, uploads in background
     */
    fun uploadAttachedImages() {
        val uris = _attachedImageUris.value
        if (uris.isEmpty()) return
        
        val conversationId = _currentConversationId.value
        
        // Clear preview immediately
        _attachedImageUris.value = emptyList()

        viewModelScope.launch {
            // Copy to internal storage BEFORE showing preview — content:// URIs expire on restart
            val persistedUris = kotlinx.coroutines.withContext(Dispatchers.IO) {
                uris.mapNotNull { uri -> copyPhotoToInternalStorage(uri) }
            }.takeIf { it.isNotEmpty() } ?: uris.map { it.toString() }

            // Show photos as user message
            val userMessage = MessageDTO(
                id = UUID.randomUUID().toString(),
                content = "",
                role = "user",
                agentName = null,
                provider = null,
                providerColor = null,
                modelUsed = null,
                createdAt = System.currentTimeMillis().toString(),
                images = persistedUris
            )
            if (conversationId == null) {
                // No conversation yet — send as a regular message so backend creates the conversation
                _messages.value = _messages.value + userMessage
                val imagesToSend = uris.mapNotNull { uri -> getAttachedImagesBase64(uri)?.firstOrNull() }
                if (imagesToSend.isNotEmpty()) {
                    chatRepository.sendMessageStream(
                        message = "📷",
                        conversationId = null,
                        images = imagesToSend,
                        onStatus = {},
                        onToken = {},
                        onImage = { _, _ -> },
                        onCitations = {},
                        onAgentStep = {},
                        onFile = {},
                        onConversation = { serverConvId, aiTitle ->
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _currentConversationId.value = serverConvId
                                savedStateHandle["conversationId"] = serverConvId
                            }
                            viewModelScope.launch(Dispatchers.IO) {
                                val effectiveTitle = aiTitle.ifBlank { "Фото" }
                                chatRepository.upsertConversationWithTitle(serverConvId, effectiveTitle)
                                chatRepository.saveUserMessageNow(
                                    conversationId = serverConvId,
                                    messageId = userMessage.id,
                                    content = userMessage.content,
                                    images = persistedUris.takeIf { it.isNotEmpty() }
                                )
                            }
                        },
                        onDone = { _, _, newConvId, _ ->
                            newConvId?.let {
                                if (_currentConversationId.value != it) {
                                    _currentConversationId.value = it
                                    savedStateHandle["conversationId"] = it
                                }
                            }
                        },
                        onError = { e -> Timber.e("Upload-as-message error: $e") }
                    )
                }
                return@launch
            }

            // Показываем сообщение в UI СНАЧАЛА, потом сохраняем в Room.
            // Важен порядок: если сначала сохранить в Room, его observer обновит _messages
            // и затем + userMessage создаст дубль → краш LazyColumn (duplicate key).
            _messages.value = _messages.value + userMessage
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                chatRepository.saveUserMessageNow(
                    conversationId = conversationId,
                    messageId = userMessage.id,
                    content = userMessage.content,
                    images = persistedUris.takeIf { it.isNotEmpty() }
                )
            }

            for (uri in uris) {
                try {
                    attachmentsRepository.uploadAttachment(
                        conversationId = conversationId,
                        uri = uri,
                        mode = AttachmentMode.CONTEXT
                    ).onSuccess { attachment ->
                        startAttachmentPolling(conversationId, attachment)
                    }.onFailure { e ->
                        Timber.e(e, "Failed to upload attachment")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Upload error")
                }
            }
        }
    }
    
    /**
     * Copy photo from content:// URI to app's internal storage.
     * content:// URIs expire after app restart — internal storage paths persist.
     * Returns the file:// path string, or falls back to the original URI string on failure.
     */
    private fun copyPhotoToInternalStorage(uri: Uri): String? {
        return try {
            val dir = File(appContext.filesDir, "chat_images").also { it.mkdirs() }
            val destFile = File(dir, "photo_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: run {
                    Timber.w("copyPhotoToInternalStorage: openInputStream returned null for $uri, falling back to original URI")
                    return uri.toString()
                }
            inputStream.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (destFile.length() == 0L) {
                destFile.delete()
                Timber.w("copyPhotoToInternalStorage: copied empty file for $uri, falling back to original URI")
                return uri.toString()
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy photo to internal storage for $uri, falling back to original URI")
            uri.toString()
        }
    }

    /**
     * Convert Uri to base64 string with compression
     */
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val maxSize = 1024

            // 1. Считаем размеры без декодирования пикселей (избегаем OOM)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
            if (opts.outWidth <= 0 || opts.outHeight <= 0) return null

            // 2. Вычисляем inSampleSize чтобы декодировать сразу в нужный размер
            val sampleSize = run {
                var s = 1
                while (opts.outWidth / s > maxSize || opts.outHeight / s > maxSize) s *= 2
                s
            }

            // 3. Декодируем сразу уменьшенный bitmap — в памяти только 1-2МБ вместо 40МБ
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // 16-bit: вдвое меньше памяти
            }
            val bitmap = appContext.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val outputStream = java.io.ByteArrayOutputStream()
            if (!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)) {
                bitmap.recycle()
                return null
            }
            bitmap.recycle()

            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (e: Throwable) {
            // Throwable вместо Exception — ловит OutOfMemoryError тоже
            Timber.e(e, "uriToBase64 failed for $uri")
            null
        }
    }
    
    private fun getAttachedImagesBase64(uri: Uri?): List<String>? {
        uri ?: return null
        return uriToBase64(uri)?.let { listOf(it) }
    }
    
    // ==================== Session Attachments ====================
    
    /**
     * Load attachments for current conversation
     */
    fun loadSessionAttachments() {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            attachmentsRepository.loadAttachments(conversationId)
            
            // После загрузки — добавляем image attachments как сообщения в историю
            delay(500) // Даём время на загрузку
            addAttachmentsToMessages()
        }
    }
    
    /**
     * Convert session image attachments to messages and add to chat history
     * This ensures photos are visible after app restart
     */
    private fun addAttachmentsToMessages() {
        val attachments = attachmentsRepository.attachments.value
        if (attachments.isEmpty()) return
        
        // Filter only image attachments
        val imageAttachments = attachments.filter { it.type == "image" && it.url != null }
        if (imageAttachments.isEmpty()) return
        
        // Group by creation time (within 5 seconds = same batch)
        val grouped = imageAttachments.groupBy { 
            it.createdAt?.take(16) ?: it.id // Group by minute or use ID
        }
        
        val currentMessages = _messages.value.toMutableList()
        
        for ((_, group) in grouped) {
            // Create unique message ID from attachment IDs
            val messageId = "attachments_${group.map { it.id }.sorted().joinToString("_").hashCode()}"
            
            // Skip if already exists
            if (currentMessages.any { it.id == messageId }) continue
            
            // Build image URLs
            val imageUrls = group.mapNotNull { attachment ->
                attachment.url?.let { url ->
                    if (url.startsWith("http")) url
                    else "${BuildConfig.API_BASE_URL.removeSuffix("/api/v1")}$url"
                }
            }
            
            if (imageUrls.isNotEmpty()) {
                val attachmentMessage = MessageDTO(
                    id = messageId,
                    content = "",
                    role = "user",
                    agentName = null,
                    provider = null,
                    providerColor = null,
                    modelUsed = null,
                    createdAt = group.firstOrNull()?.createdAt ?: System.currentTimeMillis().toString(),
                    images = imageUrls
                )
                currentMessages.add(attachmentMessage)
            }
        }
        
        // Sort by createdAt and update
        _messages.value = currentMessages.sortedBy { it.createdAt }
    }
    
    /**
     * Запускает фоновый поллинг статуса вложения.
     * Вызывается сразу после успешного upload, если сервер ответил "processing".
     */
    private fun startAttachmentPolling(conversationId: String, attachment: com.health.companion.data.remote.api.AttachmentDTO) {
        if (attachment.status?.lowercase() == "processing" || attachment.status == null) {
            viewModelScope.launch {
                Timber.d("Start polling attachment ${attachment.id} in conv $conversationId")
                attachmentsRepository.pollUntilReady(conversationId, attachment.id)
            }
        }
    }

    /**
     * Upload file to session context
     */
    fun uploadSessionAttachment(uri: Uri) {
        val conversationId = _currentConversationId.value ?: return
        viewModelScope.launch {
            attachmentsRepository.uploadAttachment(conversationId, uri, AttachmentMode.CONTEXT)
                .onSuccess { attachment -> startAttachmentPolling(conversationId, attachment) }
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
        // ChatConnectionService intentionally NOT stopped here — it must stay alive
        // on all screens to deliver reminders and AI-initiated messages in background.
        // Service is stopped only on explicit logout (see SettingsViewModel.logout).
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
