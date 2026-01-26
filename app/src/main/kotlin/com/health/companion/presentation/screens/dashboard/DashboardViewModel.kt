package com.health.companion.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.EmotionalStateResponse
import com.health.companion.data.remote.api.FactAboutMe
import com.health.companion.data.remote.api.MemorySummaryResponse
import com.health.companion.data.remote.api.QuickAction
import com.health.companion.data.remote.api.StreakInfo
import com.health.companion.data.repositories.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationEvent = _navigationEvent.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        // Сначала покажем кэш мгновенно (без isLoading)
        dashboardRepository.getCachedDashboard()?.let { cached ->
            _state.update {
                it.copy(
                    isLoading = false,
                    greeting = cached.greeting,
                    insight = cached.insight,
                    messagesThisWeek = cached.messagesThisWeek,
                    streak = cached.streak,
                    factAboutMe = cached.factAboutMe,
                    quickActions = cached.quickActions,
                    lastUpdated = cached.lastUpdated
                )
            }
            Timber.d("Dashboard init: showing cached data")
        }
        // Потом загрузим свежее в фоне
        loadDashboard()
    }

    fun refresh() {
        loadDashboard(force = true)
    }

    fun loadDashboard(force: Boolean = false) {
        viewModelScope.launch {
            // Показываем loading только если нет данных
            if (_state.value.greeting.isEmpty()) {
                _state.update { it.copy(isLoading = true, error = null) }
            }

            val dashboardDeferred = async { dashboardRepository.getDashboard() }
            val emotionalStateDeferred = async { dashboardRepository.getEmotionalState() }
            val memorySummaryDeferred = async { dashboardRepository.getMemorySummary() }

            val dashboardResult = dashboardDeferred.await()
            val emotionalState = emotionalStateDeferred.await().getOrNull()
            val memorySummary = memorySummaryDeferred.await().getOrNull()

            dashboardResult
                .onSuccess { dashboard ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            greeting = dashboard.greeting,
                            insight = dashboard.insight,
                            messagesThisWeek = dashboard.messagesThisWeek,
                            streak = dashboard.streak,
                            factAboutMe = dashboard.factAboutMe,
                            quickActions = dashboard.quickActions,
                            lastUpdated = dashboard.lastUpdated,
                            emotionalState = emotionalState,
                            memorySummary = memorySummary,
                            error = null
                        )
                    }
                    Timber.d("Dashboard loaded: streak=${dashboard.streak.days}")
                }
                .onFailure { error ->
                    // Не показываем ошибку если есть данные
                    if (_state.value.greeting.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Не удалось загрузить"
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) }
                    }
                    Timber.e(error, "Dashboard load failed")
                }
        }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                loadDashboard()
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    fun onMessageSent() {
        viewModelScope.launch {
            delay(2000)
            loadDashboard(force = true)
        }
    }

    fun navigate(route: String) {
        _navigationEvent.tryEmit(route)
    }

    fun onQuickAction(action: QuickAction) {
        when (action.action) {
            "continue_chat", "new_chat" -> navigate("chat")
            "view_docs" -> navigate("documents")
            else -> navigate(action.action)
        }
    }
}

data class DashboardState(
    val isLoading: Boolean = true,
    val greeting: String = "",
    val insight: String = "",
    val messagesThisWeek: Int = 0,
    val streak: StreakInfo = StreakInfo(),
    val factAboutMe: FactAboutMe? = null,
    val quickActions: List<QuickAction> = emptyList(),
    val lastUpdated: String = "",
    val emotionalState: EmotionalStateResponse? = null,
    val memorySummary: MemorySummaryResponse? = null,
    val error: String? = null
)
