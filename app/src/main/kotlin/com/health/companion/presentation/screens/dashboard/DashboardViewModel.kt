package com.health.companion.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.EmotionalStateResponse
import com.health.companion.data.remote.api.MemorySummaryResponse
import com.health.companion.data.remote.api.Widget
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

    // Виджеты которые нужно скрыть
    private val hiddenWidgetTypes = setOf("recent_documents", "quick_actions")

    // Auto-refresh
    private var refreshJob: Job? = null
    private var lastRefreshTime = 0L
    private val THROTTLE_MS = 10_000L // минимум 10 сек между запросами
    private val AUTO_REFRESH_INTERVAL = 60_000L // каждые 60 сек

    init {
        loadDashboard()
    }

    fun refresh() {
        loadDashboard(force = true)
    }

    fun loadDashboard(force: Boolean = false) {
        val now = System.currentTimeMillis()
        // Throttle: минимум 10 сек между запросами (если не force)
        if (!force && now - lastRefreshTime < THROTTLE_MS) {
            Timber.d("Dashboard load throttled")
            return
        }
        lastRefreshTime = now

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Загружаем все данные параллельно
            val dashboardDeferred = async { dashboardRepository.getDashboard() }
            val emotionalStateDeferred = async { dashboardRepository.getEmotionalState() }
            val memorySummaryDeferred = async { dashboardRepository.getMemorySummary() }

            val dashboardResult = dashboardDeferred.await()
            val emotionalState = emotionalStateDeferred.await().getOrNull()
            val memorySummary = memorySummaryDeferred.await().getOrNull()

            dashboardResult
                .onSuccess { dashboard ->
                    // Фильтруем виджеты - убираем документы и быстрые действия
                    val filteredWidgets = dashboard.widgets.filter { widget ->
                        widget.type !in hiddenWidgetTypes
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            greeting = dashboard.greeting,
                            moodEmoji = dashboard.moodEmoji,
                            overallStatus = dashboard.overallStatus,
                            widgets = filteredWidgets,
                            emotionalState = emotionalState,
                            memorySummary = memorySummary,
                            error = null
                        )
                    }
                    Timber.d("Dashboard loaded: ${filteredWidgets.size} widgets, emotional: $emotionalState")
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Не удалось загрузить дашборд"
                        )
                    }
                    Timber.e(error, "Dashboard load failed")
                }
        }
    }

    /**
     * Запуск автообновления (вызывать при входе на экран)
     */
    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL)
                loadDashboard()
            }
        }
        Timber.d("Dashboard auto-refresh started")
    }

    /**
     * Остановка автообновления (вызывать при уходе с экрана)
     */
    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
        Timber.d("Dashboard auto-refresh stopped")
    }

    /**
     * После отправки сообщения в чате — обновить dashboard с задержкой
     * (чтобы бэкенд успел проанализировать эмоции)
     */
    fun onMessageSent() {
        viewModelScope.launch {
            delay(2000) // подождать анализ эмоций
            loadDashboard(force = true)
            Timber.d("Dashboard refreshed after message sent")
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
    val emotionalState: EmotionalStateResponse? = null,
    val memorySummary: MemorySummaryResponse? = null,
    val error: String? = null
)
