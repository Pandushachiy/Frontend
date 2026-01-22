package com.health.companion.presentation.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.DailySummaryDTO
import com.health.companion.data.remote.api.HealthMetricDTO
import com.health.companion.data.repositories.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    private val _metrics = MutableStateFlow<List<HealthMetricDTO>>(emptyList())
    val metrics: StateFlow<List<HealthMetricDTO>> = _metrics.asStateFlow()

    private val _insights = MutableStateFlow<DailySummaryDTO?>(null)
    val insights: StateFlow<DailySummaryDTO?> = _insights.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedMetricType = MutableStateFlow<String?>(null)
    val selectedMetricType: StateFlow<String?> = _selectedMetricType.asStateFlow()

    init {
        loadHealthData()
    }

    fun loadHealthData() {
        viewModelScope.launch {
            try {
                _uiState.value = HealthUiState.Loading

                // Load metrics
                val metricsResult = healthRepository.getMetrics(null)
                metricsResult.onSuccess { metricsList ->
                    _metrics.value = metricsList
                    _uiState.value = HealthUiState.Success
                }.onFailure { e ->
                    Timber.e(e, "Failed to load health metrics")
                    _uiState.value = HealthUiState.Error(e.message ?: "Failed to load data")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load health data")
                _uiState.value = HealthUiState.Error(e.message ?: "Failed to load data")
            }
        }

        // Load insights separately
        viewModelScope.launch {
            try {
                val insightsResult = healthRepository.getInsights()
                insightsResult.onSuccess { insightsData ->
                    _insights.value = insightsData
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load health insights")
                // Don't set error state, insights are optional
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadHealthData()
            _isRefreshing.value = false
        }
    }

    fun selectMetricType(type: String?) {
        _selectedMetricType.value = type
        viewModelScope.launch {
            val result = healthRepository.getMetrics(type)
            result.onSuccess { metricsList ->
                _metrics.value = metricsList
            }.onFailure { e ->
                Timber.e(e, "Failed to filter metrics")
            }
        }
    }

    fun addManualMetric(metricType: String, value: Float, unit: String) {
        viewModelScope.launch {
            try {
                val result = healthRepository.addManualMetric(metricType, value, unit)
                result.onSuccess { newMetric ->
                    _metrics.value = listOf(newMetric) + _metrics.value
                    Timber.d("Manual metric added: $newMetric")
                }.onFailure { e ->
                    Timber.e(e, "Failed to add manual metric")
                    _uiState.value = HealthUiState.Error(e.message ?: "Failed to add metric")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to add manual metric")
                _uiState.value = HealthUiState.Error(e.message ?: "Failed to add metric")
            }
        }
    }

    fun clearError() {
        _uiState.value = HealthUiState.Success
    }
}

sealed class HealthUiState {
    object Loading : HealthUiState()
    object Success : HealthUiState()
    data class Error(val message: String) : HealthUiState()
}
