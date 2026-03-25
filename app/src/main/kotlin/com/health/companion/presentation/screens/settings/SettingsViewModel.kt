package com.health.companion.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.local.ProfileCache
import com.health.companion.data.remote.api.ProfileApi
import com.health.companion.data.remote.api.ProfileStatsResponse
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.presentation.theme.AppFontFamily
import com.health.companion.presentation.theme.AppThemeOption
import com.health.companion.presentation.theme.ChatBackgroundOption
import com.health.companion.services.ChatConnectionService
import com.health.companion.utils.ThemeManager
import com.health.companion.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager,
    private val themeManager: ThemeManager,
    private val profileApi: ProfileApi,
    private val profileCache: ProfileCache,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val selectedTheme: StateFlow<AppThemeOption> = themeManager.selectedTheme
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeOption.TEAL)

    val selectedBackground: StateFlow<ChatBackgroundOption> = themeManager.selectedChatBackground
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChatBackgroundOption.NIGHT)

    val textScale: StateFlow<Float> = themeManager.textScale
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.health.companion.utils.ThemeManager.TEXT_SCALE_DEFAULT)

    val fontFamily: StateFlow<AppFontFamily> = themeManager.fontFamily
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppFontFamily.DEFAULT)

    fun setTheme(theme: AppThemeOption) {
        viewModelScope.launch { themeManager.setTheme(theme) }
    }

    fun setBackground(bg: ChatBackgroundOption) {
        viewModelScope.launch { themeManager.setChatBackground(bg) }
    }

    fun setTextScale(scale: Float) {
        viewModelScope.launch { themeManager.setTextScale(scale) }
    }

    fun setFontFamily(font: AppFontFamily) {
        viewModelScope.launch { themeManager.setFontFamily(font) }
    }

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Инициализируем из кэша мгновенно — UI не мигает пустым состоянием
    private val _userName = MutableStateFlow(
        profileCache.getDisplayName().ifEmpty { "Пользователь" }
    )
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow(
        profileCache.getEmail().ifEmpty { "—" }
    )
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _avatarEmoji = MutableStateFlow(profileCache.getAvatarEmoji())
    val avatarEmoji: StateFlow<String?> = _avatarEmoji.asStateFlow()

    init {
        refreshProfileFromNetwork()
        refreshProfileStats()
    }

    private fun refreshProfileFromNetwork() {
        viewModelScope.launch {
            try {
                val profile = runCatching { profileApi.getProfileMe() }.getOrNull() ?: return@launch

                val displayName = when {
                    !profile.nickname.isNullOrBlank() -> profile.nickname!!
                    profile.name.isNotBlank() -> profile.name
                    else -> tokenManager.getUserName() ?: return@launch
                }
                val email = profile.email ?: tokenManager.getUserEmail()

                // Обновляем кэш и StateFlow
                profileCache.update(
                    name = profile.name.takeIf { it.isNotBlank() },
                    nickname = profile.nickname,
                    email = email,
                    avatarEmoji = profile.avatarEmoji
                )

                _userName.value = displayName
                _userEmail.value = email ?: tokenManager.getUserId() ?: "—"
                if (profile.avatarEmoji != null) _avatarEmoji.value = profile.avatarEmoji

                Timber.d("Profile refreshed: $displayName, $email, avatar=${profile.avatarEmoji}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh user profile")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Clear local chat data first
                chatRepository.clearAllLocalData()
                chatRepository.disconnectWebSocket()

                // Stop the persistent WebSocket service — user is logging out
                ChatConnectionService.stop(context)

                // Then logout from server
                authRepository.logout()
                tokenManager.clearTokens()
                profileCache.clear()
                Timber.d("User logged out, local data cleared")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
                // Still clear tokens and local data on error
                try { chatRepository.clearAllLocalData() } catch (_: Exception) {}
                ChatConnectionService.stop(context)
                tokenManager.clearTokens()
                profileCache.clear()
            }
        }
    }

    fun clearState() {
        _uiState.value = SettingsUiState.Idle
    }

    private val _profileStats = MutableStateFlow<ProfileStatsResponse?>(null)
    val profileStats: StateFlow<ProfileStatsResponse?> = _profileStats.asStateFlow()

    fun refreshProfileStats() {
        viewModelScope.launch {
            runCatching { profileApi.getProfileStats() }
                .onSuccess { _profileStats.value = it }
                .onFailure { Timber.e(it, "Failed to load profile stats") }
        }
    }

    private val _isDeletingAllData = MutableStateFlow(false)
    val isDeletingAllData: StateFlow<Boolean> = _isDeletingAllData.asStateFlow()

    fun deleteAllData(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isDeletingAllData.value = true
            Timber.d("deleteAllData: started")
            try {
                Timber.d("deleteAllData: calling DELETE /api/v1/profile/data")
                profileApi.deleteAllData()
                Timber.d("deleteAllData: API success, clearing local cache")
                chatRepository.clearAllLocalData()
                Timber.d("deleteAllData: done, invoking onDone callback")
                onDone()
            } catch (e: kotlinx.coroutines.CancellationException) {
                Timber.w("deleteAllData: coroutine cancelled")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "deleteAllData FAILED [${e.javaClass.simpleName}]: ${e.message}")
                _uiState.value = SettingsUiState.Error("Не удалось удалить данные: ${e.message}")
            } finally {
                _isDeletingAllData.value = false
            }
        }
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
