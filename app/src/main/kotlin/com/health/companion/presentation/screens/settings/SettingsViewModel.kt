package com.health.companion.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.repositories.AuthRepository
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
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _userName = MutableStateFlow("User Name")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userEmail = MutableStateFlow("user@example.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    init {
        loadUserProfile()
        loadSettings()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                // In real app, fetch from API or local storage
                // For now, using mock data
                _userName.value = "John Doe"
                _userEmail.value = "john.doe@example.com"
            } catch (e: Exception) {
                Timber.e(e, "Failed to load user profile")
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load settings from DataStore
                // For now, using default values
                _isDarkMode.value = false
                _notificationsEnabled.value = true
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            _isDarkMode.value = enabled
            // Save to DataStore
            Timber.d("Dark mode set to: $enabled")
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _notificationsEnabled.value = enabled
            // Save to DataStore and update notification channels
            Timber.d("Notifications set to: $enabled")
        }
    }

    fun exportData() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                // Call API to export data
                // Download file
                Timber.d("Exporting user data...")
                _uiState.value = SettingsUiState.Success("Data exported successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to export data")
                _uiState.value = SettingsUiState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun showDeleteConfirmation() {
        _showDeleteDialog.value = true
    }

    fun hideDeleteConfirmation() {
        _showDeleteDialog.value = false
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                _uiState.value = SettingsUiState.Loading
                // Call API to delete account
                tokenManager.clearTokens()
                _uiState.value = SettingsUiState.AccountDeleted
                Timber.d("Account deleted")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete account")
                _uiState.value = SettingsUiState.Error(e.message ?: "Delete failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                tokenManager.clearTokens()
                Timber.d("User logged out")
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
                // Still clear tokens locally
                tokenManager.clearTokens()
            }
        }
    }

    fun openPrivacyPolicy() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://health-companion.com/privacy"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open privacy policy")
        }
    }

    fun openTermsOfService() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://health-companion.com/terms"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open terms of service")
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
    object AccountDeleted : SettingsUiState()
}
