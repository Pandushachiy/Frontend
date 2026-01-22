package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface HealthApi {
    
    @GET("health/metrics")
    suspend fun getMetrics(
        @Query("metric_type") metricType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): PaginatedMetricsResponse
    
    @POST("health/metrics")
    suspend fun addManualMetric(@Body metric: ManualMetricRequest): HealthMetricDTO
    
    @GET("health/summary/daily")
    suspend fun getDailySummary(): DailySummaryDTO
    
    @POST("health/mood")
    suspend fun submitMoodEntry(@Body entry: MoodEntryRequest): MoodEntryDTO
    
    @GET("health/mood")
    suspend fun getMoodHistory(@Query("days") days: Int = 30): PaginatedMoodResponse
}

/**
 * Paginated response for metrics
 * Backend returns: {"items":[],"total":0}
 */
@Serializable
data class PaginatedMetricsResponse(
    val items: List<HealthMetricDTO> = emptyList(),
    val total: Int = 0
)

@Serializable
data class HealthMetricDTO(
    val id: String,
    val metric_type: String,
    val value: Float,
    val unit: String,
    val timestamp: String
)

@Serializable
data class ManualMetricRequest(
    val metric_type: String,
    val value: Float,
    val unit: String
)

/**
 * Daily summary from backend
 * Backend returns: {"date":"...","steps":null,...,"summary_text":"...","highlights":[],...}
 */
@Serializable
data class DailySummaryDTO(
    val date: String? = null,
    val steps: Int? = null,
    val heart_rate_avg: Int? = null,
    val sleep_hours: Float? = null,
    val calories: Int? = null,
    val mood_level: Int? = null,
    val stress_level: Int? = null,
    val summary_text: String? = null,
    val highlights: List<String> = emptyList(),
    val concerns: List<String> = emptyList(),
    val recommendations: List<String> = emptyList()
)

@Serializable
data class MoodEntryRequest(
    val mood_level: Int,
    val symptoms: List<String>,
    val journal_text: String,
    val stress_level: Int
)

@Serializable
data class MoodEntryDTO(
    val id: String,
    val mood_level: Int,
    val created_at: String
)

@Serializable
data class PaginatedMoodResponse(
    val items: List<MoodEntryDTO> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20
)
