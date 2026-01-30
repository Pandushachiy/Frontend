package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface WellnessRepository {
    // Mood
    suspend fun recordMood(request: MoodRequest): Result<MoodEntry>
    suspend fun getMoodHistory(days: Int = 30): Result<List<MoodEntry>>
    suspend fun getMoodToday(): Result<MoodTodayResponse>
    suspend fun getMoodStats(days: Int = 30): Result<MoodStats>
    
    // Habits
    suspend fun createHabit(request: CreateHabitRequest): Result<Habit>
    suspend fun getHabits(): Result<List<Habit>>
    suspend fun getHabit(id: String): Result<Habit>
    suspend fun updateHabit(id: String, request: UpdateHabitRequest): Result<Habit>
    suspend fun deleteHabit(id: String): Result<Unit>
    suspend fun completeHabit(id: String, note: String? = null): Result<HabitCompletionResponse>
    suspend fun uncompleteHabit(id: String): Result<HabitCompletionResponse>
    suspend fun getHabitsStats(): Result<HabitsStats>
    
    // Digest
    suspend fun getDigestPreferences(): Result<DigestPreferences>
    suspend fun updateDigestPreferences(preferences: DigestPreferences): Result<DigestPreferences>
    suspend fun getDigestPreview(): Result<DailyDigest>
}

@Singleton
class WellnessRepositoryImpl @Inject constructor(
    private val wellnessApi: WellnessApi
) : WellnessRepository {
    
    // ==================== MOOD ====================
    
    override suspend fun recordMood(request: MoodRequest): Result<MoodEntry> {
        return try {
            val response = wellnessApi.recordMood(request)
            Timber.d("Mood recorded: ${response.moodLevel}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error recording mood: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error recording mood")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error recording mood")
            Result.failure(e)
        }
    }
    
    override suspend fun getMoodHistory(days: Int): Result<List<MoodEntry>> {
        return try {
            val response = wellnessApi.getMoodHistory(days)
            Timber.d("Got ${response.size} mood entries")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting mood history: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting mood history")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting mood history")
            Result.failure(e)
        }
    }
    
    override suspend fun getMoodToday(): Result<MoodTodayResponse> {
        return try {
            val response = wellnessApi.getMoodToday()
            Timber.d("Mood today: recorded=${response.recorded}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting mood today: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting mood today")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting mood today")
            Result.failure(e)
        }
    }
    
    override suspend fun getMoodStats(days: Int): Result<MoodStats> {
        return try {
            val response = wellnessApi.getMoodStats(days)
            Timber.d("Got mood stats: avg=${response.averageMood}, trend=${response.trend}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting mood stats: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting mood stats")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting mood stats")
            Result.failure(e)
        }
    }
    
    // ==================== HABITS ====================
    
    override suspend fun createHabit(request: CreateHabitRequest): Result<Habit> {
        return try {
            val response = wellnessApi.createHabit(request)
            Timber.d("Created habit: ${response.name}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error creating habit: ${e.code()}")
            Result.failure(Exception("Ошибка создания привычки"))
        } catch (e: IOException) {
            Timber.e(e, "Network error creating habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error creating habit")
            Result.failure(e)
        }
    }
    
    override suspend fun getHabits(): Result<List<Habit>> {
        return try {
            val response = wellnessApi.getHabits()
            Timber.d("Got ${response.size} habits")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting habits: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting habits")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting habits")
            Result.failure(e)
        }
    }
    
    override suspend fun getHabit(id: String): Result<Habit> {
        return try {
            val response = wellnessApi.getHabit(id)
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting habit: ${e.code()}")
            Result.failure(Exception("Привычка не найдена"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting habit")
            Result.failure(e)
        }
    }
    
    override suspend fun updateHabit(id: String, request: UpdateHabitRequest): Result<Habit> {
        return try {
            val response = wellnessApi.updateHabit(id, request)
            Timber.d("Updated habit: ${response.name}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error updating habit: ${e.code()}")
            Result.failure(Exception("Ошибка обновления привычки"))
        } catch (e: IOException) {
            Timber.e(e, "Network error updating habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error updating habit")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteHabit(id: String): Result<Unit> {
        return try {
            wellnessApi.deleteHabit(id)
            Timber.d("Deleted habit: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error deleting habit: ${e.code()}")
            Result.failure(Exception("Ошибка удаления привычки"))
        } catch (e: IOException) {
            Timber.e(e, "Network error deleting habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error deleting habit")
            Result.failure(e)
        }
    }
    
    override suspend fun completeHabit(id: String, note: String?): Result<HabitCompletionResponse> {
        return try {
            val response = wellnessApi.completeHabit(id, CompleteHabitRequest(note = note))
            Timber.d("Completed habit: $id, streak=${response.currentStreak}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error completing habit: ${e.code()}")
            Result.failure(Exception("Ошибка отметки привычки"))
        } catch (e: IOException) {
            Timber.e(e, "Network error completing habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error completing habit")
            Result.failure(e)
        }
    }
    
    override suspend fun uncompleteHabit(id: String): Result<HabitCompletionResponse> {
        return try {
            val response = wellnessApi.uncompleteHabit(id)
            Timber.d("Uncompleted habit: $id")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error uncompleting habit: ${e.code()}")
            Result.failure(Exception("Ошибка отмены привычки"))
        } catch (e: IOException) {
            Timber.e(e, "Network error uncompleting habit")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error uncompleting habit")
            Result.failure(e)
        }
    }
    
    override suspend fun getHabitsStats(): Result<HabitsStats> {
        return try {
            val response = wellnessApi.getHabitsStats()
            Timber.d("Got habits stats: total=${response.totalHabits}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting habits stats: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting habits stats")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting habits stats")
            Result.failure(e)
        }
    }
    
    // ==================== DIGEST ====================
    
    override suspend fun getDigestPreferences(): Result<DigestPreferences> {
        return try {
            val response = wellnessApi.getDigestPreferences()
            Timber.d("Got digest preferences: enabled=${response.isEnabled}")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting digest preferences: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting digest preferences")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting digest preferences")
            Result.failure(e)
        }
    }
    
    override suspend fun updateDigestPreferences(preferences: DigestPreferences): Result<DigestPreferences> {
        return try {
            val response = wellnessApi.updateDigestPreferences(preferences)
            Timber.d("Updated digest preferences")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error updating digest preferences: ${e.code()}")
            Result.failure(Exception("Ошибка сохранения настроек"))
        } catch (e: IOException) {
            Timber.e(e, "Network error updating digest preferences")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error updating digest preferences")
            Result.failure(e)
        }
    }
    
    override suspend fun getDigestPreview(): Result<DailyDigest> {
        return try {
            val response = wellnessApi.getDigestPreview()
            Timber.d("Got digest preview")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error getting digest preview: ${e.code()}")
            Result.failure(Exception("Ошибка сервера: ${e.code()}"))
        } catch (e: IOException) {
            Timber.e(e, "Network error getting digest preview")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Error getting digest preview")
            Result.failure(e)
        }
    }
}
