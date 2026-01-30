package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

/**
 * Wellness API — Mood, Habits, Daily Digest
 */
interface WellnessApi {
    
    // ==================== MOOD ====================
    
    @POST("wellness/mood")
    suspend fun recordMood(@Body request: MoodRequest): MoodEntry
    
    @GET("wellness/mood")
    suspend fun getMoodHistory(@Query("days") days: Int = 30): List<MoodEntry>
    
    @GET("wellness/mood/today")
    suspend fun getMoodToday(): MoodTodayResponse
    
    @GET("wellness/mood/stats")
    suspend fun getMoodStats(@Query("days") days: Int = 30): MoodStats
    
    // ==================== HABITS ====================
    
    @POST("wellness/habits")
    suspend fun createHabit(@Body request: CreateHabitRequest): Habit
    
    @GET("wellness/habits")
    suspend fun getHabits(): List<Habit>
    
    @GET("wellness/habits/{id}")
    suspend fun getHabit(@Path("id") id: String): Habit
    
    @PUT("wellness/habits/{id}")
    suspend fun updateHabit(@Path("id") id: String, @Body request: UpdateHabitRequest): Habit
    
    @DELETE("wellness/habits/{id}")
    suspend fun deleteHabit(@Path("id") id: String)
    
    @POST("wellness/habits/{id}/complete")
    suspend fun completeHabit(
        @Path("id") id: String,
        @Body request: CompleteHabitRequest = CompleteHabitRequest()
    ): HabitCompletionResponse
    
    @POST("wellness/habits/{id}/uncomplete")
    suspend fun uncompleteHabit(@Path("id") id: String): HabitCompletionResponse
    
    @GET("wellness/habits/stats")
    suspend fun getHabitsStats(): HabitsStats
    
    // ==================== DIGEST ====================
    
    @GET("wellness/digest/preferences")
    suspend fun getDigestPreferences(): DigestPreferences
    
    @PUT("wellness/digest/preferences")
    suspend fun updateDigestPreferences(@Body request: DigestPreferences): DigestPreferences
    
    @GET("wellness/digest/preview")
    suspend fun getDigestPreview(): DailyDigest
}

// ==================== MOOD MODELS ====================

@Serializable
data class MoodRequest(
    @SerialName("mood_level") val moodLevel: Int,
    @SerialName("energy_level") val energyLevel: Int? = null,
    @SerialName("stress_level") val stressLevel: Int? = null,
    @SerialName("anxiety_level") val anxietyLevel: Int? = null,
    val activities: List<String> = emptyList(),
    val triggers: List<String> = emptyList(),
    @SerialName("journal_text") val journalText: String? = null
)

@Serializable
data class MoodEntry(
    val id: String,
    @SerialName("mood_level") val moodLevel: Int,
    @SerialName("energy_level") val energyLevel: Int? = null,
    @SerialName("stress_level") val stressLevel: Int? = null,
    @SerialName("anxiety_level") val anxietyLevel: Int? = null,
    val activities: List<String> = emptyList(),
    val triggers: List<String> = emptyList(),
    @SerialName("journal_text") val journalText: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class MoodTodayResponse(
    val recorded: Boolean,
    val entry: MoodEntry? = null
)

@Serializable
data class MoodStats(
    @SerialName("average_mood") val averageMood: Float,
    @SerialName("average_energy") val averageEnergy: Float? = null,
    @SerialName("average_stress") val averageStress: Float? = null,
    @SerialName("total_entries") val totalEntries: Int,
    @SerialName("mood_by_day") val moodByDay: Map<String, Float> = emptyMap(),
    @SerialName("common_activities") val commonActivities: List<ActivityCount> = emptyList(),
    @SerialName("common_triggers") val commonTriggers: List<TriggerCount> = emptyList(),
    val trend: String = "stable" // improving, stable, declining
)

@Serializable
data class ActivityCount(
    val activity: String,
    val count: Int
)

@Serializable
data class TriggerCount(
    val trigger: String,
    val count: Int
)

// ==================== HABITS MODELS ====================

@Serializable
data class CreateHabitRequest(
    val name: String,
    val description: String? = null,
    val emoji: String = "✅",
    val frequency: String = "daily", // daily, weekly, custom
    @SerialName("frequency_times") val frequencyTimes: Int = 1,
    @SerialName("reminder_enabled") val reminderEnabled: Boolean = false,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("reminder_days") val reminderDays: List<Int> = emptyList(),
    val color: String = "#6366F1"
)

@Serializable
data class UpdateHabitRequest(
    val name: String? = null,
    val description: String? = null,
    val emoji: String? = null,
    val frequency: String? = null,
    @SerialName("frequency_times") val frequencyTimes: Int? = null,
    @SerialName("reminder_enabled") val reminderEnabled: Boolean? = null,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("reminder_days") val reminderDays: List<Int>? = null,
    val color: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class Habit(
    val id: String,
    val name: String,
    val description: String? = null,
    val emoji: String = "✅",
    val frequency: String = "daily",
    @SerialName("frequency_times") val frequencyTimes: Int = 1,
    @SerialName("reminder_enabled") val reminderEnabled: Boolean = false,
    @SerialName("reminder_time") val reminderTime: String? = null,
    @SerialName("reminder_days") val reminderDays: List<Int> = emptyList(),
    val color: String = "#6366F1",
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("total_completions") val totalCompletions: Int = 0,
    @SerialName("completed_today") val completedToday: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CompleteHabitRequest(
    val note: String? = null,
    val count: Int = 1
)

@Serializable
data class HabitCompletionResponse(
    val status: String, // completed, uncompleted
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("best_streak") val bestStreak: Int = 0,
    @SerialName("total_completions") val totalCompletions: Int = 0
)

@Serializable
data class HabitsStats(
    @SerialName("total_habits") val totalHabits: Int = 0,
    @SerialName("active_habits") val activeHabits: Int = 0,
    @SerialName("completed_today") val completedToday: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("total_completions_week") val totalCompletionsWeek: Int = 0,
    @SerialName("completion_rate_week") val completionRateWeek: Float = 0f
)

// ==================== DIGEST MODELS ====================

@Serializable
data class DigestPreferences(
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("send_time") val sendTime: String = "08:00",
    val timezone: String = "Europe/Moscow",
    @SerialName("include_weather") val includeWeather: Boolean = true,
    @SerialName("include_quote") val includeQuote: Boolean = true,
    @SerialName("include_mood_prompt") val includeMoodPrompt: Boolean = true,
    @SerialName("include_habits_summary") val includeHabitsSummary: Boolean = true,
    val tone: String = "friendly" // friendly, professional, motivational
)

@Serializable
data class DailyDigest(
    val greeting: String,
    val weather: WeatherInfo? = null,
    val quote: QuoteInfo? = null,
    @SerialName("mood_prompt") val moodPrompt: String? = null,
    @SerialName("habits_summary") val habitsSummary: HabitsSummary? = null,
    val insights: List<String> = emptyList(),
    @SerialName("generated_at") val generatedAt: String? = null
)

@Serializable
data class WeatherInfo(
    val city: String,
    val temp: Int,
    val condition: String,
    val emoji: String
)

@Serializable
data class QuoteInfo(
    val text: String,
    val author: String = ""
)

@Serializable
data class HabitsSummary(
    val total: Int,
    @SerialName("completed_yesterday") val completedYesterday: Int,
    @SerialName("completion_rate") val completionRate: Int,
    val habits: List<HabitSummaryItem> = emptyList(),
    val message: String = ""
)

@Serializable
data class HabitSummaryItem(
    val name: String,
    val emoji: String,
    @SerialName("completed_yesterday") val completedYesterday: Boolean,
    @SerialName("current_streak") val currentStreak: Int
)
