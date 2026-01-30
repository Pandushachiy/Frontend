package com.health.companion.presentation.screens.medical

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.MedicalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MedicalViewModel @Inject constructor(
    private val repository: MedicalRepository
) : ViewModel() {
    
    // === Loading & Error States ===
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // === Symptoms Check ===
    private val _symptomsResult = MutableStateFlow<SymptomCheckResponse?>(null)
    val symptomsResult: StateFlow<SymptomCheckResponse?> = _symptomsResult.asStateFlow()
    
    // === Drug Interactions ===
    private val _drugResult = MutableStateFlow<DrugInteractionResponse?>(null)
    val drugResult: StateFlow<DrugInteractionResponse?> = _drugResult.asStateFlow()
    
    // === Lab Results ===
    private val _labResult = MutableStateFlow<LabResultsResponse?>(null)
    val labResult: StateFlow<LabResultsResponse?> = _labResult.asStateFlow()
    
    // === Medical Search ===
    private val _searchResult = MutableStateFlow<MedicalSearchResponse?>(null)
    val searchResult: StateFlow<MedicalSearchResponse?> = _searchResult.asStateFlow()
    
    // === Recommendations ===
    private val _recommendations = MutableStateFlow<HealthRecommendationsResponse?>(null)
    val recommendations: StateFlow<HealthRecommendationsResponse?> = _recommendations.asStateFlow()
    
    // === Emergency Info ===
    private val _emergencyInfo = MutableStateFlow<EmergencyInfoResponse?>(null)
    val emergencyInfo: StateFlow<EmergencyInfoResponse?> = _emergencyInfo.asStateFlow()
    
    init {
        loadEmergencyInfo()
    }
    
    // ==================== SYMPTOMS ====================
    
    fun checkSymptoms(symptoms: String, duration: String? = null) {
        if (symptoms.isBlank()) {
            _error.value = "Опишите симптомы"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _symptomsResult.value = null
            
            repository.checkSymptoms(symptoms, duration)
                .onSuccess { _symptomsResult.value = it }
                .onFailure { 
                    _error.value = it.message ?: "Ошибка проверки симптомов"
                    Timber.e(it, "Failed to check symptoms")
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearSymptomsResult() {
        _symptomsResult.value = null
    }
    
    // ==================== DRUG INTERACTIONS ====================
    
    fun checkDrugInteractions(drugs: List<String>, includeCurrentMeds: Boolean = true) {
        if (drugs.isEmpty()) {
            _error.value = "Добавьте хотя бы одно лекарство"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _drugResult.value = null
            
            repository.checkDrugInteractions(drugs, includeCurrentMeds)
                .onSuccess { _drugResult.value = it }
                .onFailure { 
                    _error.value = it.message ?: "Ошибка проверки взаимодействий"
                    Timber.e(it, "Failed to check drug interactions")
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearDrugResult() {
        _drugResult.value = null
    }
    
    // ==================== LAB RESULTS ====================
    
    fun analyzeLabResults(results: Map<String, Float>) {
        if (results.isEmpty()) {
            _error.value = "Добавьте хотя бы один показатель"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _labResult.value = null
            
            repository.analyzeLabResults(results)
                .onSuccess { _labResult.value = it }
                .onFailure { 
                    _error.value = it.message ?: "Ошибка анализа результатов"
                    Timber.e(it, "Failed to analyze lab results")
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearLabResult() {
        _labResult.value = null
    }
    
    // ==================== MEDICAL SEARCH ====================
    
    fun searchMedical(query: String) {
        if (query.isBlank()) {
            _error.value = "Введите запрос"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _searchResult.value = null
            
            repository.searchMedical(query)
                .onSuccess { _searchResult.value = it }
                .onFailure { 
                    _error.value = it.message ?: "Ошибка поиска"
                    Timber.e(it, "Failed to search medical info")
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearSearchResult() {
        _searchResult.value = null
    }
    
    // ==================== RECOMMENDATIONS ====================
    
    fun loadRecommendations(focusArea: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getRecommendations(focusArea)
                .onSuccess { _recommendations.value = it }
                .onFailure { 
                    _error.value = it.message
                    Timber.e(it, "Failed to load recommendations")
                }
            
            _isLoading.value = false
        }
    }
    
    // ==================== EMERGENCY INFO ====================
    
    fun loadEmergencyInfo() {
        viewModelScope.launch {
            repository.getEmergencyInfo()
                .onSuccess { _emergencyInfo.value = it }
                .onFailure { Timber.e(it, "Failed to load emergency info") }
        }
    }
    
    // ==================== UTILS ====================
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearAllResults() {
        _symptomsResult.value = null
        _drugResult.value = null
        _labResult.value = null
        _searchResult.value = null
        _recommendations.value = null
    }
}

// Severity helper
enum class Severity(val color: Long, val label: String, val icon: String) {
    LOW(0xFF4CAF50, "Лёгкая", "✅"),
    MEDIUM(0xFFFF9800, "Средняя", "⚡"),
    HIGH(0xFFF44336, "Серьёзная", "⚠️"),
    URGENT(0xFFB71C1C, "СРОЧНО", "🚨");
    
    companion object {
        fun fromString(value: String): Severity = when (value.lowercase()) {
            "low" -> LOW
            "medium" -> MEDIUM
            "high" -> HIGH
            "urgent" -> URGENT
            else -> LOW
        }
    }
}

// Lab status helper
enum class LabStatus(val color: Long, val label: String) {
    NORMAL(0xFF4CAF50, "Норма"),
    LOW(0xFF2196F3, "Понижен"),
    HIGH(0xFFFF9800, "Повышен"),
    CRITICAL_LOW(0xFFF44336, "Критически низкий"),
    CRITICAL_HIGH(0xFFF44336, "Критически высокий");
    
    companion object {
        fun fromString(value: String): LabStatus = when (value.lowercase()) {
            "normal" -> NORMAL
            "low" -> LOW
            "high" -> HIGH
            "critical_low" -> CRITICAL_LOW
            "critical_high" -> CRITICAL_HIGH
            else -> NORMAL
        }
    }
}
