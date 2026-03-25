package com.health.companion.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.RpModel
import com.health.companion.data.remote.api.RpSessionCard
import com.health.companion.data.repositories.RpRepository
import com.health.companion.data.repositories.VoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class RpPhase { GALLERY, SETUP, CHAT }

data class RpChatMessage(
    val id: String,
    val role: String,
    val text: String
)

@HiltViewModel
class RpViewModel @Inject constructor(
    private val rpRepository: RpRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {

    private val _rpPhase = MutableStateFlow(RpPhase.GALLERY)
    val rpPhase: StateFlow<RpPhase> = _rpPhase.asStateFlow()

    private val _sessions = MutableStateFlow<List<RpSessionCard>>(emptyList())
    val sessions: StateFlow<List<RpSessionCard>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loaderText = MutableStateFlow("")
    val loaderText: StateFlow<String> = _loaderText.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _charName = MutableStateFlow("")
    val charName: StateFlow<String> = _charName.asStateFlow()

    private val _charAppearance = MutableStateFlow<String?>(null)
    val charAppearance: StateFlow<String?> = _charAppearance.asStateFlow()

    private val _tone = MutableStateFlow<String?>(null)
    val tone: StateFlow<String?> = _tone.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<RpChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<RpChatMessage>> = _chatMessages.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    private val _isUpdatingRoles = MutableStateFlow(false)
    val isUpdatingRoles: StateFlow<Boolean> = _isUpdatingRoles.asStateFlow()

    // Model selection
    private val _models = MutableStateFlow<List<RpModel>>(emptyList())
    val models: StateFlow<List<RpModel>> = _models.asStateFlow()

    private val _selectedModelKey = MutableStateFlow<String?>(null)
    val selectedModelKey: StateFlow<String?> = _selectedModelKey.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    // Current session model name (for badge in chat)
    private val _currentModelName = MutableStateFlow<String?>(null)
    val currentModelName: StateFlow<String?> = _currentModelName.asStateFlow()

    // Voice recording state from VoiceRepository (same as main chat)
    val isRecording: StateFlow<Boolean> = voiceRepository.isRecording

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var streamJob: Job? = null
    private val streamBuffer = StringBuilder()
    private var recordingStartJob: Job? = null

    init {
        loadSessions()
    }

    // ─── Voice input (exact same pattern as ChatViewModel) ───────────────────

    fun startVoiceInput() {
        recordingStartJob = viewModelScope.launch {
            if (voiceRepository.isRecording.value) return@launch
            voiceRepository.startRecording().onFailure { e ->
                Timber.e(e, "RP: Failed to start recording")
                _errorMessage.value = "Не удалось начать запись"
            }
        }
    }

    fun stopVoiceInput() {
        viewModelScope.launch {
            recordingStartJob?.join()
            recordingStartJob = null
            if (!voiceRepository.isRecording.value) return@launch
            voiceRepository.stopRecording().onSuccess { file ->
                voiceRepository.transcribe(file).onSuccess { response ->
                    if (response.text.isNotBlank() && _rpPhase.value == RpPhase.CHAT) {
                        sendMessage(response.text)
                    }
                }.onFailure { e ->
                    Timber.e(e, "RP: Transcription failed")
                    _errorMessage.value = "Не удалось распознать речь"
                }
                file.delete()
            }.onFailure { e ->
                Timber.e(e, "RP: Failed to stop recording")
                _errorMessage.value = "Ошибка записи"
            }
        }
    }

    fun cancelVoiceInput() {
        viewModelScope.launch {
            recordingStartJob?.join()
            recordingStartJob = null
            if (voiceRepository.isRecording.value) {
                voiceRepository.cancelRecording()
            }
        }
    }

    // ─── Session management ──────────────────────────────────────────────────

    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            rpRepository.getSessions().onSuccess { list ->
                _sessions.value = list
            }.onFailure {
                Timber.w(it, "Failed to load RP sessions")
            }
            _isLoading.value = false
        }
    }

    fun openSetup() {
        _rpPhase.value = RpPhase.SETUP
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            _isLoadingModels.value = true
            rpRepository.getModels().onSuccess { list ->
                _models.value = list
                // Выбираем default, если ещё не выбрано
                if (_selectedModelKey.value == null) {
                    _selectedModelKey.value = list.firstOrNull { it.isDefault }?.modelKey
                        ?: list.firstOrNull()?.modelKey
                }
            }.onFailure {
                Timber.w(it, "Failed to load RP models")
            }
            _isLoadingModels.value = false
        }
    }

    fun selectModel(modelKey: String) {
        _selectedModelKey.value = modelKey
    }

    fun openSession(card: RpSessionCard) {
        viewModelScope.launch {
            _isLoading.value = true
            _loaderText.value = "Загружаем сессию..."
            rpRepository.getState(card.sessionId).onSuccess { state ->
                _currentSessionId.value = state.sessionId
                _charName.value = state.charName
                _charAppearance.value = state.charAppearance
                _tone.value = state.tone
                _currentModelName.value = state.modelName ?: card.modelName
                _chatMessages.value = state.history.mapIndexed { i, h ->
                    RpChatMessage(id = "hist_$i", role = h.role, text = h.content)
                }
                _rpPhase.value = RpPhase.CHAT
            }.onFailure {
                _errorMessage.value = "Не удалось загрузить сессию"
            }
            _isLoading.value = false
            _loaderText.value = ""
        }
    }

    fun createSession(
        theme: String,
        charName: String,
        charDescription: String,
        userName: String,
        userDescription: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loaderText.value = "Создаём персонажа..."
            rpRepository.newSession(
                theme, charName, charDescription, userName, userDescription,
                modelKey = _selectedModelKey.value
            ).onSuccess { resp ->
                _currentSessionId.value = resp.sessionId
                _charName.value = resp.charName
                _charAppearance.value = resp.charAppearance
                _tone.value = resp.tone
                _currentModelName.value = resp.modelName
                _chatMessages.value = if (!resp.firstMessage.isNullOrBlank()) {
                    listOf(RpChatMessage(id = "first", role = "assistant", text = resp.firstMessage))
                } else emptyList()
                _rpPhase.value = RpPhase.CHAT
                loadSessions()
            }.onFailure {
                _errorMessage.value = "Не удалось создать персонажа: ${it.localizedMessage}"
            }
            _isLoading.value = false
            _loaderText.value = ""
        }
    }

    fun sendMessage(text: String) {
        val sessionId = _currentSessionId.value ?: return
        if (_isStreaming.value || text.isBlank()) return
        addMessage("user", text)
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _isStreaming.value = true
            _streamingText.value = ""
            streamBuffer.clear()
            rpRepository.sendMessage(
                sessionId = sessionId,
                message = text,
                onToken = { token ->
                    streamBuffer.append(token)
                    _streamingText.value = streamBuffer.toString()
                },
                onDone = { _ ->
                    val full = streamBuffer.toString().trim()
                    if (full.isNotBlank()) {
                        viewModelScope.launch(Dispatchers.Main) { addMessage("assistant", full) }
                    }
                    streamBuffer.clear()
                    viewModelScope.launch(Dispatchers.Main) {
                        _streamingText.value = ""
                        _isStreaming.value = false
                    }
                },
                onError = { err ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _streamingText.value = ""
                        addMessage("system", "Ошибка: $err")
                        _isStreaming.value = false
                    }
                }
            )
        }
    }

    fun updateRoles(
        charName: String? = null,
        charDescription: String? = null,
        userName: String? = null,
        userDescription: String? = null
    ) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            _isUpdatingRoles.value = true
            rpRepository.updateRoles(sessionId, charName, charDescription, userName, userDescription)
                .onSuccess { state ->
                    _charName.value = state.charName
                }.onFailure {
                    _errorMessage.value = "Не удалось обновить роли"
                }
            _isUpdatingRoles.value = false
        }
    }

    fun deleteCurrentSession() {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            rpRepository.deleteSession(sessionId).onSuccess {
                _currentSessionId.value = null
                _chatMessages.value = emptyList()
                _rpPhase.value = RpPhase.GALLERY
                loadSessions()
            }.onFailure {
                _errorMessage.value = "Не удалось удалить сессию"
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            rpRepository.deleteSession(sessionId).onSuccess {
                loadSessions()
            }
        }
    }

    fun navigateBack() {
        when (_rpPhase.value) {
            RpPhase.CHAT -> {
                if (voiceRepository.isRecording.value) voiceRepository.cancelRecording()
                _rpPhase.value = RpPhase.GALLERY
                loadSessions()
            }
            RpPhase.SETUP -> _rpPhase.value = RpPhase.GALLERY
            RpPhase.GALLERY -> {}
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun addMessage(role: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        _chatMessages.value = _chatMessages.value + RpChatMessage(
            id = "msg_${System.currentTimeMillis()}_${(0..999).random()}",
            role = role,
            text = clean
        )
    }
}
