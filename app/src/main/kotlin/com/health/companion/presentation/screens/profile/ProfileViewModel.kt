package com.health.companion.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.KnowledgeGraphResponse
import com.health.companion.data.remote.api.ProfileResponse
import com.health.companion.data.remote.api.RoutingStatsResponse
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
    val knowledgeGraph: KnowledgeGraphResponse? = null,
    val routingStats: RoutingStatsResponse? = null,
    val deletingKey: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll(entityType: String? = null, limit: Int? = 50) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val profileResult = profileRepository.getProfile()
            profileResult.onSuccess { profile ->
                _uiState.update { it.copy(profile = profile) }
            }.onFailure { e ->
                Timber.e(e, "Profile load failed")
                _uiState.update { it.copy(error = e.message ?: "Ошибка загрузки профиля") }
            }

            val graphResult = profileRepository.getKnowledgeGraph(entityType, limit)
            graphResult.onSuccess { graph ->
                _uiState.update { it.copy(knowledgeGraph = graph) }
            }.onFailure { e ->
                Timber.e(e, "Graph load failed")
                _uiState.update { it.copy(error = e.message ?: "Ошибка загрузки графа знаний") }
            }

            val statsResult = profileRepository.getRoutingStats()
            statsResult.onSuccess { stats ->
                _uiState.update { it.copy(routingStats = stats) }
            }.onFailure { e ->
                Timber.e(e, "Routing stats load failed")
                _uiState.update { it.copy(error = e.message ?: "Ошибка загрузки статистики") }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun deleteMemory(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deletingKey = key, error = null) }
            val result = profileRepository.deleteMemory(key)
            result.onSuccess {
                _uiState.update { state ->
                    val updated = state.profile?.copy(
                        memories = state.profile.memories.filterNot { it.key == key }
                    )
                    state.copy(profile = updated, deletingKey = null)
                }
            }.onFailure { e ->
                Timber.e(e, "Delete memory failed")
                _uiState.update { it.copy(deletingKey = null, error = e.message ?: "Ошибка удаления памяти") }
            }
        }
    }
}
