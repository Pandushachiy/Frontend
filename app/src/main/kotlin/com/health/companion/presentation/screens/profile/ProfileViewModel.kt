package com.health.companion.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.ProfileResponse
import com.health.companion.data.repositories.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: ProfileResponse? = null,
    val deletingId: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Сначала покажем кэш (если есть)
            val cached = profileRepository.getCachedProfile()
            if (cached != null && !forceRefresh) {
                _uiState.update { it.copy(profile = cached, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // Потом загрузим свежие данные
            val result = profileRepository.getProfile()
            result.onSuccess { profile ->
                _uiState.update { it.copy(profile = profile, isLoading = false, error = null) }
            }.onFailure { e ->
                // Если есть кэш - не показываем ошибку
                if (_uiState.value.profile == null) {
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
                Timber.e(e, "Profile load failed")
            }
        }
    }

    fun refresh() = loadProfile(forceRefresh = true)

    fun deleteFact(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingId = id, error = null) }
            val result = profileRepository.deleteFact(id)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.profile?.copy(
                        facts = state.profile.facts.filterNot { it.id == id }
                    )
                    state.copy(profile = updated, deletingId = null)
                }
            }.onFailure { e ->
                Timber.e(e, "Delete fact failed")
                _uiState.update { it.copy(deletingId = null, error = e.message) }
            }
        }
    }

    fun clearAllFacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = profileRepository.clearAllFacts()
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.profile?.copy(facts = emptyList())
                    state.copy(profile = updated, isLoading = false)
                }
            }.onFailure { e ->
                Timber.e(e, "Clear facts failed")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
