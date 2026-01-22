package com.health.companion.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.Widget
import com.health.companion.data.repositories.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard()
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            dashboardRepository.getDashboard()
                .onSuccess { dashboard ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            greeting = dashboard.greeting,
                            moodEmoji = dashboard.moodEmoji,
                            overallStatus = dashboard.overallStatus,
                            widgets = dashboard.widgets,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось загрузить дашборд"
                        )
                    }
                }
        }
    }

    fun navigate(route: String) {
        _navigationEvent.tryEmit(route)
    }
}

data class DashboardState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val moodEmoji: String = "😊",
    val overallStatus: String = "neutral",
    val widgets: List<Widget> = emptyList(),
    val error: String? = null
)
