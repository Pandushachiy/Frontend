package com.health.companion.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.repositories.AuthRepository
import com.health.companion.data.repositories.ChatRepository
import com.health.companion.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("Пользователь")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("—")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val name = tokenManager.getUserName()
                val email = tokenManager.getUserEmail()
                
                _userName.value = name ?: "Пользователь"
                _userEmail.value = email ?: tokenManager.getUserId() ?: "—"
                
                Timber.d("Profile loaded: $name, $email")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load user profile")
                _userName.value = "Пользователь"
                _userEmail.value = "—"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                // Clear local chat data first
                chatRepository.clearAllLocalData()
                chatRepository.disconnectWebSocket()
                
                // Then logout from server
                authRepository.logout()
                tokenManager.clearTokens()
                Timber.d("User logged out, local data cleared")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
                // Still clear tokens and local data on error
                try { chatRepository.clearAllLocalData() } catch (_: Exception) {}
                tokenManager.clearTokens()
            }
        }
    }

    fun clearState() {
        _uiState.value = SettingsUiState.Idle
    }
}

sealed class SettingsUiState {
    object Idle : SettingsUiState()
    object Loading : SettingsUiState()
    data class Success(val message: String) : SettingsUiState()
    data class Error(val message: String) : SettingsUiState()
}
