package com.health.companion.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.LifeContextRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: LifeContextRepository
) : ViewModel() {
    
    // === Profile State ===
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // === Questionnaire State ===
    private val _questionnaire = MutableStateFlow<QuestionnaireResponse?>(null)
    val questionnaire: StateFlow<QuestionnaireResponse?> = _questionnaire.asStateFlow()
    
    private val _currentSection = MutableStateFlow(0)
    val currentSection: StateFlow<Int> = _currentSection.asStateFlow()
    
    private val _answers = MutableStateFlow<MutableMap<String, Any>>(mutableMapOf())
    val answers: StateFlow<Map<String, Any>> = _answers.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    // === Important Dates ===
    private val _importantDates = MutableStateFlow<List<ImportantDate>>(emptyList())
    val importantDates: StateFlow<List<ImportantDate>> = _importantDates.asStateFlow()
    
    // === Important People ===
    private val _importantPeople = MutableStateFlow<List<ImportantPerson>>(emptyList())
    val importantPeople: StateFlow<List<ImportantPerson>> = _importantPeople.asStateFlow()
    
    // === Life Patterns ===
    private val _patterns = MutableStateFlow<LifePatternsResponse?>(null)
    val patterns: StateFlow<LifePatternsResponse?> = _patterns.asStateFlow()
    
    val sections = listOf("basic", "health", "lifestyle", "social", "goals", "mental")
    
    init {
        loadProfile()
        loadQuestionnaire()
        loadImportantDates()
        loadImportantPeople()
    }
    
    // ==================== PROFILE ====================
    
    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getProfile()
                .onSuccess { _profile.value = it }
                .onFailure { 
                    Timber.e(it, "Failed to load profile")
                    _error.value = it.message
                }
            
            _isLoading.value = false
        }
    }
    
    // ==================== QUESTIONNAIRE ====================
    
    fun loadQuestionnaire(section: String? = null) {
        viewModelScope.launch {
            repository.getQuestionnaire(section)
                .onSuccess { _questionnaire.value = it }
                .onFailure { Timber.e(it, "Failed to load questionnaire") }
        }
    }
    
    fun setAnswer(key: String, value: Any) {
        _answers.value = _answers.value.toMutableMap().apply {
            put(key, value)
        }
    }
    
    fun nextSection() {
        if (_currentSection.value < sections.size - 1) {
            _currentSection.value++
        }
    }
    
    fun previousSection() {
        if (_currentSection.value > 0) {
            _currentSection.value--
        }
    }
    
    fun goToSection(index: Int) {
        if (index in sections.indices) {
            _currentSection.value = index
        }
    }
    
    fun saveAnswers(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            
            repository.saveAnswers(_answers.value)
                .onSuccess { 
                    loadProfile()
                    onSuccess()
                }
                .onFailure { 
                    _error.value = it.message
                    Timber.e(it, "Failed to save answers")
                }
            
            _isSaving.value = false
        }
    }
    
    // ==================== IMPORTANT DATES ====================
    
    fun loadImportantDates() {
        viewModelScope.launch {
            repository.getImportantDates()
                .onSuccess { _importantDates.value = it }
                .onFailure { Timber.e(it, "Failed to load important dates") }
        }
    }
    
    fun addImportantDate(date: String, title: String, eventType: String = "custom", recurring: Boolean = true) {
        viewModelScope.launch {
            _isSaving.value = true
            val request = ImportantDateCreate(date, title, eventType, recurring)
            
            repository.addImportantDate(request)
                .onSuccess { loadImportantDates() }
                .onFailure { _error.value = it.message }
            
            _isSaving.value = false
        }
    }
    
    fun deleteImportantDate(id: String) {
        viewModelScope.launch {
            repository.deleteImportantDate(id)
                .onSuccess { loadImportantDates() }
                .onFailure { _error.value = it.message }
        }
    }
    
    // ==================== IMPORTANT PEOPLE ====================
    
    fun loadImportantPeople() {
        viewModelScope.launch {
            repository.getImportantPeople()
                .onSuccess { _importantPeople.value = it }
                .onFailure { Timber.e(it, "Failed to load important people") }
        }
    }
    
    fun addImportantPerson(name: String, relation: String, details: String? = null, birthday: String? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            val request = ImportantPersonCreate(name, relation, details, birthday)
            
            repository.addImportantPerson(request)
                .onSuccess { loadImportantPeople() }
                .onFailure { _error.value = it.message }
            
            _isSaving.value = false
        }
    }
    
    fun deleteImportantPerson(id: String) {
        viewModelScope.launch {
            repository.deleteImportantPerson(id)
                .onSuccess { loadImportantPeople() }
                .onFailure { _error.value = it.message }
        }
    }
    
    // ==================== PATTERNS ====================
    
    fun loadPatterns() {
        viewModelScope.launch {
            repository.getLifePatterns()
                .onSuccess { _patterns.value = it }
                .onFailure { Timber.e(it, "Failed to load patterns") }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
