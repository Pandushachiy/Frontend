package com.health.companion.data.repositories

import com.health.companion.data.remote.api.CreateReminderRequest
import com.health.companion.data.remote.api.EditReminderRequest
import com.health.companion.data.remote.api.ParseReminderRequest
import com.health.companion.data.remote.api.ReminderDTO
import com.health.companion.data.remote.api.ReminderStatsResponse
import com.health.companion.data.remote.api.RemindersApi
import com.health.companion.data.remote.api.SnoozeReminderRequest
import com.health.companion.data.remote.api.UpdateReminderRequest
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface RemindersRepository {
    suspend fun getReminders(status: String = "active", category: String? = null): Result<List<ReminderDTO>>
    suspend fun createReminder(request: CreateReminderRequest): Result<ReminderDTO>
    suspend fun parseAndCreate(text: String, conversationId: String? = null): Result<ReminderDTO>
    suspend fun getUpcoming(hours: Int = 24): Result<List<ReminderDTO>>
    suspend fun getDue(): Result<List<ReminderDTO>>
    suspend fun completeReminder(id: String): Result<Boolean>
    suspend fun snoozeReminder(id: String, minutes: Int = 30): Result<Boolean>
    suspend fun deleteReminder(id: String): Result<Boolean>
    suspend fun editReminderTime(id: String, newText: String): Result<Boolean>
    suspend fun updateReminder(id: String, request: UpdateReminderRequest): Result<ReminderDTO>
    suspend fun getStats(): Result<ReminderStatsResponse>
}

@Singleton
class RemindersRepositoryImpl @Inject constructor(
    private val remindersApi: RemindersApi
) : RemindersRepository {

    override suspend fun getReminders(status: String, category: String?): Result<List<ReminderDTO>> {
        return try {
            val response = remindersApi.getReminders(status, category)
            Timber.d("Got ${response.reminders.size} reminders (status=$status)")
            Result.success(response.reminders)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting reminders: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting reminders")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting reminders")
            Result.failure(e)
        }
    }

    override suspend fun createReminder(request: CreateReminderRequest): Result<ReminderDTO> {
        return try {
            val response = remindersApi.createReminder(request)
            Timber.d("Created reminder: ${response.reminder.title}")
            Result.success(response.reminder)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error creating reminder: ${e.code()}")
            Result.failure(Exception("Ошибка создания напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error creating reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error creating reminder")
            Result.failure(e)
        }
    }

    override suspend fun parseAndCreate(text: String, conversationId: String?): Result<ReminderDTO> {
        return try {
            val response = remindersApi.parseReminder(
                ParseReminderRequest(text = text, conversationId = conversationId)
            )
            Timber.d("Parsed & created reminder: ${response.reminder.title}")
            Result.success(response.reminder)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error parsing reminder: ${e.code()}")
            Result.failure(Exception("Ошибка создания напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error parsing reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error parsing reminder")
            Result.failure(e)
        }
    }

    override suspend fun getUpcoming(hours: Int): Result<List<ReminderDTO>> {
        return try {
            val response = remindersApi.getUpcoming(hours)
            Timber.d("Got ${response.upcoming.size} upcoming reminders")
            Result.success(response.upcoming)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting upcoming: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting upcoming")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting upcoming")
            Result.failure(e)
        }
    }

    override suspend fun getDue(): Result<List<ReminderDTO>> {
        return try {
            val response = remindersApi.getDue()
            Timber.d("Got ${response.due.size} due reminders")
            Result.success(response.due)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting due: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting due")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting due")
            Result.failure(e)
        }
    }

    override suspend fun completeReminder(id: String): Result<Boolean> {
        return try {
            val response = remindersApi.completeReminder(id)
            Timber.d("Completed reminder: $id")
            Result.success(response.success)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error completing reminder: ${e.code()}")
            Result.failure(Exception("Ошибка выполнения напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error completing reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error completing reminder")
            Result.failure(e)
        }
    }

    override suspend fun snoozeReminder(id: String, minutes: Int): Result<Boolean> {
        return try {
            val response = remindersApi.snoozeReminder(id, SnoozeReminderRequest(minutes))
            Timber.d("Snoozed reminder: $id for $minutes min")
            Result.success(response.success)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error snoozing reminder: ${e.code()}")
            Result.failure(Exception("Ошибка откладывания напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error snoozing reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error snoozing reminder")
            Result.failure(e)
        }
    }

    override suspend fun deleteReminder(id: String): Result<Boolean> {
        return try {
            val response = remindersApi.deleteReminder(id)
            Timber.d("Deleted reminder: $id")
            Result.success(response.success)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error deleting reminder: ${e.code()}")
            Result.failure(Exception("Ошибка удаления напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error deleting reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error deleting reminder")
            Result.failure(e)
        }
    }

    override suspend fun updateReminder(id: String, request: UpdateReminderRequest): Result<ReminderDTO> {
        return try {
            val response = remindersApi.updateReminder(id, request)
            Timber.d("Updated reminder: $id")
            Result.success(response.reminder)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error updating reminder: ${e.code()}")
            Result.failure(Exception("Ошибка обновления напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error updating reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error updating reminder")
            Result.failure(e)
        }
    }

    override suspend fun editReminderTime(id: String, newText: String): Result<Boolean> {
        return try {
            val response = remindersApi.editReminder(id, EditReminderRequest(newText = newText))
            Timber.d("Edited reminder: $id")
            Result.success(response.success)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error editing reminder: ${e.code()}")
            Result.failure(Exception("Ошибка редактирования напоминания"))
        } catch (e: IOException) {
            Timber.e(e, "Network error editing reminder")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error editing reminder")
            Result.failure(e)
        }
    }

    override suspend fun getStats(): Result<ReminderStatsResponse> {
        return try {
            val response = remindersApi.getStats()
            Timber.d("Got reminder stats: total=${response.total}, active=${response.active}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting stats: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting stats")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting stats")
            Result.failure(e)
        }
    }
}
