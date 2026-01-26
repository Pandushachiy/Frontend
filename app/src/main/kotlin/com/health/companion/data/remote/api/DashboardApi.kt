package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse

    @GET("dashboard/mood-chart")
    suspend fun getMoodChart(@Query("days") days: Int = 7): MoodChartResponse

    @GET("dashboard/streak")
    suspend fun getStreak(): StreakResponse

    @GET("dashboard/emotional-state")
    suspend fun getEmotionalState(): EmotionalStateResponse

    @GET("dashboard/memory-summary")
    suspend fun getMemorySummary(): MemorySummaryResponse
}

// ========== DASHBOARD (NEW FORMAT) ==========

@Serializable
data class DashboardResponse(
    val greeting: String = "",
    val insight: String = "",
    val messagesThisWeek: Int = 0,
    val streak: StreakInfo = StreakInfo(),
    val factAboutMe: FactAboutMe? = null,
    val quickActions: List<QuickAction> = emptyList(),
    val lastUpdated: String = ""
)

@Serializable
data class StreakInfo(
    val days: Int = 0,
    val emoji: String = "👋",
    val message: String = "Начни общение!"
)

@Serializable
data class FactAboutMe(
    val emoji: String = "💡",
    val text: String = ""
)

@Serializable
data class QuickAction(
    val id: String,
    val emoji: String,
    val title: String,
    val action: String
)

// ========== MOOD CHART ==========

@Serializable
data class MoodChartResponse(
    val type: String = "mood_chart",
    @SerialName("period_days") val periodDays: Int = 7,
    val data: MoodChartData = MoodChartData(),
    val visualization: ChartVisualization? = null
)

@Serializable
data class MoodChartData(
    @SerialName("chart_data") val chartData: List<MoodDataPoint> = emptyList(),
    @SerialName("entries_count") val entriesCount: Int = 0,
    @SerialName("average_mood") val averageMood: Float? = null,
    @SerialName("best_day") val bestDay: MoodDataPoint? = null,
    @SerialName("worst_day") val worstDay: MoodDataPoint? = null
)

@Serializable
data class MoodDataPoint(
    val date: String,
    @SerialName("mood_level") val moodLevel: Float? = null,
    @SerialName("stress_level") val stressLevel: Float? = null,
    @SerialName("energy_level") val energyLevel: Float? = null,
    val valence: Float? = null
)

@Serializable
data class ChartVisualization(
    @SerialName("chart_type") val chartType: String = "line",
    @SerialName("x_axis") val xAxis: String = "date",
    @SerialName("y_axes") val yAxes: List<String> = emptyList(),
    val colors: Map<String, String> = emptyMap()
)

// ========== STREAK ==========

@Serializable
data class StreakResponse(
    val type: String = "streak",
    val data: StreakData = StreakData(),
    val encouragement: String = ""
)

@Serializable
data class StreakData(
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("total_days_active") val totalDaysActive: Int = 0,
    val milestones: List<Milestone> = emptyList(),
    @SerialName("last_activity") val lastActivity: String? = null
)

@Serializable
data class Milestone(
    val days: Int,
    val achieved: Boolean = false,
    val emoji: String? = null,
    val progress: Float? = null
)

// ========== EMOTIONAL STATE ==========

@Serializable
data class EmotionalStateResponse(
    val type: String = "emotional_state",
    val valence: Float = 0f,
    val arousal: Float = 0f,
    @SerialName("primary_emotion") val primaryEmotion: String? = null,
    @SerialName("secondary_emotions") val secondaryEmotions: List<String> = emptyList(),
    @SerialName("mood_label") val moodLabel: String? = null,
    @SerialName("needs_support") val needsSupport: Boolean = false,
    val confidence: Float = 0f,
    @SerialName("detected_triggers") val detectedTriggers: List<String> = emptyList()
)

// ========== MEMORY SUMMARY ==========

@Serializable
data class MemorySummaryResponse(
    val type: String = "memory_summary",
    @SerialName("facts_count") val factsCount: Int = 0,
    @SerialName("episodes_count") val episodesCount: Int = 0,
    @SerialName("recent_facts") val recentFacts: List<String> = emptyList(),
    @SerialName("ai_knows_about") val aiKnowsAbout: List<String> = emptyList()
)
