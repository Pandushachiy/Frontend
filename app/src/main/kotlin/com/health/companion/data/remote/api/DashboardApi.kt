package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

interface DashboardApi {
    @GET("dashboard")
    suspend fun getDashboard(): DashboardResponse
}

// ========== ROOT RESPONSE ==========

@Serializable
data class DashboardResponse(
    @SerialName("knowledge_completeness") val knowledgeCompleteness: KnowledgeCompleteness = KnowledgeCompleteness(),
    @SerialName("weekly_insight") val weeklyInsight: String = "",
    @SerialName("mood_score") val moodScore: MoodScore = MoodScore(),
    val goals: GoalsData = GoalsData(),
    @SerialName("quick_stats") val quickStats: QuickStats = QuickStats(),
    @SerialName("activity_heatmap") val activityHeatmap: ActivityHeatmap = ActivityHeatmap(),
    @SerialName("knowledge_graph") val knowledgeGraph: KnowledgeGraph = KnowledgeGraph(),
    @SerialName("today_date") val todayDate: String = "",
    @SerialName("last_updated") val lastUpdated: String = ""
)

// ========== TILE 1: Knowledge Completeness ==========

@Serializable
data class KnowledgeCompleteness(
    val percentage: Int = 0,
    @SerialName("filled_categories") val filledCategories: Int = 0,
    @SerialName("total_categories") val totalCategories: Int = 10,
    val label: String = "",
    val filled: List<String> = emptyList(),
    val missing: List<String> = emptyList()
)

// ========== TILE 3: Mood Score ==========

@Serializable
data class MoodScore(
    @SerialName("current_avg") val currentAvg: Float = 0f,
    @SerialName("trend_vs_last_week") val trendVsLastWeek: Float = 0f,
    @SerialName("trend_direction") val trendDirection: String = "stable",
    val points: List<MoodPoint> = emptyList(),
    val label: String = ""
)

@Serializable
data class MoodPoint(
    val date: String = "",
    val value: Float = 0f
)

// ========== TILE 4: Goals ==========

@Serializable
data class GoalsData(
    val items: List<GoalItem> = emptyList(),
    val total: Int = 0
)

@Serializable
data class GoalItem(
    val key: String = "",
    val value: String = "",
    val confidence: String = "inferred"
)

// ========== TILES 5-8: Quick Stats ==========

@Serializable
data class QuickStats(
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("total_messages") val totalMessages: Int = 0,
    @SerialName("mood_today") val moodToday: Float? = null,
    @SerialName("habits_completed_today") val habitsCompletedToday: Int = 0,
    @SerialName("habits_total") val habitsTotal: Int = 0
)

// ========== TILE 9: Activity Heatmap ==========

@Serializable
data class ActivityHeatmap(
    val data: List<HeatmapCell> = emptyList(),
    @SerialName("peak_weekday") val peakWeekday: Int = 0,
    @SerialName("peak_hour") val peakHour: Int = 0,
    @SerialName("peak_label") val peakLabel: String = ""
)

@Serializable
data class HeatmapCell(
    val weekday: Int = 0,
    val hour: Int = 0,
    val count: Int = 0
)

// ========== TILE 10: Knowledge Graph ==========

@Serializable
data class KnowledgeGraph(
    @SerialName("total_entities") val totalEntities: Int = 0,
    @SerialName("by_type") val byType: List<EntityType> = emptyList(),
    @SerialName("top_people") val topPeople: List<String> = emptyList()
)

@Serializable
data class EntityType(
    val type: String = "",
    val label: String = "",
    val count: Int = 0
)
