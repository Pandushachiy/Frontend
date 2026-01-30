package com.health.companion.presentation.screens.wellness

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.WellnessRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WellnessViewModel @Inject constructor(
    private val wellnessRepository: WellnessRepository
) : ViewModel() {
    
    // ==================== STATE ====================
    
    private val _uiState = MutableStateFlow<WellnessUiState>(WellnessUiState.Loading)
    val uiState: StateFlow<WellnessUiState> = _uiState.asStateFlow()
    
    // Mood
    private val _moodToday = MutableStateFlow<MoodEntry?>(null)
    val moodToday: StateFlow<MoodEntry?> = _moodToday.asStateFlow()
    
    private val _moodHistory = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moodHistory: StateFlow<List<MoodEntry>> = _moodHistory.asStateFlow()
    
    private val _moodStats = MutableStateFlow<MoodStats?>(null)
    val moodStats: StateFlow<MoodStats?> = _moodStats.asStateFlow()
    
    private val _isMoodRecordedToday = MutableStateFlow(false)
    val isMoodRecordedToday: StateFlow<Boolean> = _isMoodRecordedToday.asStateFlow()
    
    // Habits
    private val _habits = MutableStateFlow<List<Habit>>(emptyList())
    val habits: StateFlow<List<Habit>> = _habits.asStateFlow()
    
    private val _habitsStats = MutableStateFlow<HabitsStats?>(null)
    val habitsStats: StateFlow<HabitsStats?> = _habitsStats.asStateFlow()
    
    // Digest
    private val _digestPreferences = MutableStateFlow<DigestPreferences?>(null)
    val digestPreferences: StateFlow<DigestPreferences?> = _digestPreferences.asStateFlow()
    
    private val _dailyDigest = MutableStateFlow<DailyDigest?>(null)
    val dailyDigest: StateFlow<DailyDigest?> = _dailyDigest.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // ==================== INIT ====================
    
    init {
        loadAllData()
    }
    
    fun loadAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Load in parallel
            launch { loadMoodToday() }
            launch { loadMoodHistory() }
            launch { loadMoodStats() }
            launch { loadHabits() }
            launch { loadHabitsStats() }
            launch { loadDigestPreview() }
            
            _isLoading.value = false
            _uiState.value = WellnessUiState.Success
        }
    }
    
    // ==================== MOOD ====================
    
    private suspend fun loadMoodToday() {
        wellnessRepository.getMoodToday().onSuccess { response ->
            _isMoodRecordedToday.value = response.recorded
            _moodToday.value = response.entry
        }.onFailure { e ->
            Timber.e(e, "Failed to load mood today")
        }
    }
    
    private suspend fun loadMoodHistory(days: Int = 30) {
        wellnessRepository.getMoodHistory(days).onSuccess { entries ->
            _moodHistory.value = entries
        }.onFailure { e ->
            Timber.e(e, "Failed to load mood history")
        }
    }
    
    private suspend fun loadMoodStats(days: Int = 30) {
        wellnessRepository.getMoodStats(days).onSuccess { stats ->
            _moodStats.value = stats
        }.onFailure { e ->
            Timber.e(e, "Failed to load mood stats")
        }
    }
    
    fun recordMood(
        moodLevel: Int,
        energyLevel: Int? = null,
        stressLevel: Int? = null,
        anxietyLevel: Int? = null,
        activities: List<String> = emptyList(),
        triggers: List<String> = emptyList(),
        journalText: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val request = MoodRequest(
                moodLevel = moodLevel,
                energyLevel = energyLevel,
                stressLevel = stressLevel,
                anxietyLevel = anxietyLevel,
                activities = activities,
                triggers = triggers,
                journalText = journalText
            )
            
            wellnessRepository.recordMood(request).onSuccess { entry ->
                _moodToday.value = entry
                _isMoodRecordedToday.value = true
                // Reload stats
                loadMoodHistory()
                loadMoodStats()
                onSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to record mood")
                onError(e.message ?: "Ошибка записи настроения")
            }
            
            _isLoading.value = false
        }
    }
    
    // ==================== HABITS ====================
    
    private suspend fun loadHabits() {
        wellnessRepository.getHabits().onSuccess { habitsList ->
            _habits.value = habitsList.sortedBy { !it.completedToday }
        }.onFailure { e ->
            Timber.e(e, "Failed to load habits")
        }
    }
    
    private suspend fun loadHabitsStats() {
        wellnessRepository.getHabitsStats().onSuccess { stats ->
            _habitsStats.value = stats
        }.onFailure { e ->
            Timber.e(e, "Failed to load habits stats")
        }
    }
    
    fun createHabit(
        name: String,
        description: String? = null,
        emoji: String = "✅",
        frequency: String = "daily",
        frequencyTimes: Int = 1,
        reminderEnabled: Boolean = false,
        reminderTime: String? = null,
        reminderDays: List<Int> = emptyList(),
        color: String = "#6366F1",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val request = CreateHabitRequest(
                name = name,
                description = description,
                emoji = emoji,
                frequency = frequency,
                frequencyTimes = frequencyTimes,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                reminderDays = reminderDays,
                color = color
            )
            
            wellnessRepository.createHabit(request).onSuccess { habit ->
                _habits.value = _habits.value + habit
                loadHabitsStats()
                onSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to create habit")
                onError(e.message ?: "Ошибка создания привычки")
            }
            
            _isLoading.value = false
        }
    }
    
    fun completeHabit(habitId: String, note: String? = null) {
        viewModelScope.launch {
            wellnessRepository.completeHabit(habitId, note).onSuccess { response ->
                // Update local state
                _habits.value = _habits.value.map { habit ->
                    if (habit.id == habitId) {
                        habit.copy(
                            completedToday = true,
                            currentStreak = response.currentStreak,
                            bestStreak = response.bestStreak,
                            totalCompletions = response.totalCompletions
                        )
                    } else habit
                }
                loadHabitsStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to complete habit")
                _error.value = e.message
            }
        }
    }
    
    fun uncompleteHabit(habitId: String) {
        viewModelScope.launch {
            wellnessRepository.uncompleteHabit(habitId).onSuccess { response ->
                // Update local state
                _habits.value = _habits.value.map { habit ->
                    if (habit.id == habitId) {
                        habit.copy(
                            completedToday = false,
                            currentStreak = response.currentStreak,
                            bestStreak = response.bestStreak,
                            totalCompletions = response.totalCompletions
                        )
                    } else habit
                }
                loadHabitsStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to uncomplete habit")
                _error.value = e.message
            }
        }
    }
    
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            wellnessRepository.deleteHabit(habitId).onSuccess {
                _habits.value = _habits.value.filter { it.id != habitId }
                loadHabitsStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to delete habit")
                _error.value = e.message
            }
        }
    }
    
    fun updateHabit(
        habitId: String,
        name: String? = null,
        description: String? = null,
        emoji: String? = null,
        color: String? = null,
        isActive: Boolean? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val request = UpdateHabitRequest(
                name = name,
                description = description,
                emoji = emoji,
                color = color,
                isActive = isActive
            )
            
            wellnessRepository.updateHabit(habitId, request).onSuccess { habit ->
                _habits.value = _habits.value.map { 
                    if (it.id == habitId) habit else it 
                }
                onSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to update habit")
                onError(e.message ?: "Ошибка обновления")
            }
        }
    }
    
    // ==================== DIGEST ====================
    
    private suspend fun loadDigestPreview() {
        wellnessRepository.getDigestPreview().onSuccess { digest ->
            _dailyDigest.value = digest
        }.onFailure { e ->
            Timber.e(e, "Failed to load digest preview")
        }
    }
    
    fun loadDigestPreferences() {
        viewModelScope.launch {
            wellnessRepository.getDigestPreferences().onSuccess { prefs ->
                _digestPreferences.value = prefs
            }.onFailure { e ->
                Timber.e(e, "Failed to load digest preferences")
                _error.value = e.message
            }
        }
    }
    
    fun updateDigestPreferences(
        preferences: DigestPreferences,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            wellnessRepository.updateDigestPreferences(preferences).onSuccess { prefs ->
                _digestPreferences.value = prefs
                onSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to update digest preferences")
                onError(e.message ?: "Ошибка сохранения настроек")
            }
            
            _isLoading.value = false
        }
    }
    
    fun refreshDigest() {
        viewModelScope.launch {
            loadDigestPreview()
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}

sealed class WellnessUiState {
    object Loading : WellnessUiState()
    object Success : WellnessUiState()
    data class Error(val message: String) : WellnessUiState()
}
