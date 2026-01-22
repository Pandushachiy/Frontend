package com.health.companion.presentation.screens.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.local.database.MoodEntryEntity
import com.health.companion.data.remote.api.MoodEntryDTO
import com.health.companion.data.repositories.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoodUiState>(MoodUiState.Idle)
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    private val _moodLevel = MutableStateFlow(3)
    val moodLevel: StateFlow<Int> = _moodLevel.asStateFlow()

    private val _stressLevel = MutableStateFlow(2)
    val stressLevel: StateFlow<Int> = _stressLevel.asStateFlow()

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText.asStateFlow()

    private val _symptoms = MutableStateFlow<Set<String>>(emptySet())
    val symptoms: StateFlow<Set<String>> = _symptoms.asStateFlow()

    private val _moodHistory = MutableStateFlow<List<MoodEntryEntity>>(emptyList())
    val moodHistory: StateFlow<List<MoodEntryEntity>> = _moodHistory.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        loadMoodHistory()
    }

    private fun loadMoodHistory() {
        viewModelScope.launch {
            try {
                healthRepository.getLocalMoodEntries()
                    .collect { history ->
                        _moodHistory.value = history
                    }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load mood history")
            }
        }
    }

    fun updateMoodLevel(level: Int) {
        _moodLevel.value = level.coerceIn(1, 5)
    }

    fun updateStressLevel(level: Int) {
        _stressLevel.value = level.coerceIn(1, 5)
    }

    fun updateJournalText(text: String) {
        _journalText.value = text
    }

    fun toggleSymptom(symptom: String) {
        val currentSymptoms = _symptoms.value.toMutableSet()
        if (currentSymptoms.contains(symptom)) {
            currentSymptoms.remove(symptom)
        } else {
            currentSymptoms.add(symptom)
        }
        _symptoms.value = currentSymptoms
    }

    fun submitMoodEntry(
        moodLevel: Int = _moodLevel.value,
        stressLevel: Int = _stressLevel.value,
        journalText: String = _journalText.value,
        symptoms: Set<String> = _symptoms.value
    ) {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                _uiState.value = MoodUiState.Saving

                val result = healthRepository.submitMoodEntry(
                    moodLevel = moodLevel,
                    stressLevel = stressLevel,
                    symptoms = symptoms.toList(),
                    journalText = journalText
                )

                result.onSuccess { savedEntry ->
                    // Reset form
                    resetForm()
                    _uiState.value = MoodUiState.Success
                    Timber.d("Mood entry saved: $savedEntry")
                }.onFailure { e ->
                    // Even on failure, data is saved locally
                    resetForm()
                    _uiState.value = MoodUiState.Success
                    Timber.w("Mood entry saved locally, sync failed: ${e.message}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to save mood entry")
                _uiState.value = MoodUiState.Error(e.message ?: "Failed to save")
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun resetForm() {
        _moodLevel.value = 3
        _stressLevel.value = 2
        _journalText.value = ""
        _symptoms.value = emptySet()
    }

    fun clearState() {
        _uiState.value = MoodUiState.Idle
    }

    fun getMoodEmoji(level: Int): String {
        return when (level) {
            1 -> "😢"
            2 -> "😔"
            3 -> "😐"
            4 -> "🙂"
            5 -> "😊"
            else -> "😐"
        }
    }

    fun getStressLabel(level: Int): String {
        return when (level) {
            1 -> "Very Low"
            2 -> "Low"
            3 -> "Moderate"
            4 -> "High"
            5 -> "Very High"
            else -> "Moderate"
        }
    }
}

sealed class MoodUiState {
    object Idle : MoodUiState()
    object Saving : MoodUiState()
    object Success : MoodUiState()
    data class Error(val message: String) : MoodUiState()
}
