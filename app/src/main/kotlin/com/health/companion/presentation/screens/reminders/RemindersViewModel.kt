package com.health.companion.presentation.screens.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.companion.data.remote.api.*
import com.health.companion.data.repositories.RemindersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val remindersRepository: RemindersRepository
) : ViewModel() {

    // === State ===
    private val _reminders = MutableStateFlow<List<ReminderDTO>>(emptyList())
    val reminders: StateFlow<List<ReminderDTO>> = _reminders.asStateFlow()

    private val _stats = MutableStateFlow<ReminderStatsResponse?>(null)
    val stats: StateFlow<ReminderStatsResponse?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusFilter = MutableStateFlow("active")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableSharedFlow<String>()
    val successMessage: SharedFlow<String> = _successMessage.asSharedFlow()

    // Push notifications (from WebSocket)
    private val _pushNotification = MutableSharedFlow<ReminderNotification>()
    val pushNotification: SharedFlow<ReminderNotification> = _pushNotification.asSharedFlow()

    init {
        loadReminders()
        loadStats()
    }

    fun loadReminders(status: String = _statusFilter.value) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            remindersRepository.getReminders(status = status).onSuccess { list ->
                _reminders.value = list
            }.onFailure { e ->
                Timber.e(e, "Failed to load reminders")
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            remindersRepository.getStats().onSuccess { s ->
                _stats.value = s
            }.onFailure { e ->
                Timber.e(e, "Failed to load stats")
            }
        }
    }

    fun setFilter(status: String) {
        _statusFilter.value = status
        loadReminders(status)
    }

    fun createFromText(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            remindersRepository.parseAndCreate(text).onSuccess { reminder ->
                _successMessage.emit("Создано: ${reminder.title}")
                loadReminders()
                loadStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to create reminder from text")
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun createReminder(
        title: String,
        description: String? = null,
        triggerAt: String? = null,
        frequency: String = "once",
        recurringDays: List<Int>? = null,
        recurringTimeHour: Int = 9,
        recurringTimeMinute: Int = 0,
        priority: String = "medium",
        category: String = "general"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = CreateReminderRequest(
                title = title,
                description = description,
                triggerAt = triggerAt,
                frequency = frequency,
                recurringDays = recurringDays,
                recurringTimeHour = recurringTimeHour,
                recurringTimeMinute = recurringTimeMinute,
                priority = priority,
                category = category
            )
            remindersRepository.createReminder(request).onSuccess { reminder ->
                _successMessage.emit("Создано: ${reminder.title}")
                loadReminders()
                loadStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to create reminder")
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun completeReminder(id: String) {
        viewModelScope.launch {
            remindersRepository.completeReminder(id).onSuccess {
                loadReminders()
                loadStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to complete reminder")
                _error.value = e.message
            }
        }
    }

    fun snoozeReminder(id: String, minutes: Int = 30) {
        viewModelScope.launch {
            remindersRepository.snoozeReminder(id, minutes).onSuccess {
                _successMessage.emit("Отложено на $minutes мин")
                loadReminders()
            }.onFailure { e ->
                Timber.e(e, "Failed to snooze reminder")
                _error.value = e.message
            }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            remindersRepository.deleteReminder(id).onSuccess {
                _reminders.value = _reminders.value.filter { it.id != id }
                loadStats()
            }.onFailure { e ->
                Timber.e(e, "Failed to delete reminder")
                _error.value = e.message
            }
        }
    }

    // Called from WebSocket parser
    fun onPushNotification(notification: ReminderNotification) {
        viewModelScope.launch {
            _pushNotification.emit(notification)
            loadReminders()
        }
    }

    fun clearError() {
        _error.value = null
    }
}
