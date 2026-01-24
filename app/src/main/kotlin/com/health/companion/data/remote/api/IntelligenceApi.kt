package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface IntelligenceApi {

    // ========== EMOTIONS ==========

    /**
     * Analyze emotions in text
     */
    @POST("intelligence/emotions/analyze/")
    suspend fun analyzeEmotions(
        @Body request: EmotionAnalyzeRequest
    ): EmotionAnalyzeResponse

    /**
     * Get emotional summary for user
     */
    @GET("intelligence/emotions/summary/")
    suspend fun getEmotionsSummary(
        @Query("days") days: Int = 7
    ): EmotionsSummaryResponse

    /**
     * Get therapeutic techniques based on emotional state
     */
    @GET("intelligence/emotions/techniques/")
    suspend fun getTherapeuticTechniques(): TherapeuticTechniquesResponse

    // ========== BRIEFING ==========

    /**
     * Generate morning briefing
     */
    @POST("intelligence/briefing/")
    suspend fun generateBriefing(): BriefingResponse

    // ========== NOTIFICATIONS ==========

    /**
     * Get user notifications
     */
    @GET("intelligence/notifications/")
    suspend fun getNotifications(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 20
    ): NotificationsResponse

    /**
     * Get unread notifications count
     */
    @GET("intelligence/notifications/unread-count/")
    suspend fun getUnreadCount(): UnreadCountResponse

    /**
     * Get notification preferences
     */
    @GET("intelligence/notifications/preferences/")
    suspend fun getNotificationPreferences(): NotificationPreferencesResponse

    /**
     * Update notification preferences
     */
    @PATCH("intelligence/notifications/preferences/")
    suspend fun updateNotificationPreferences(
        @Body preferences: NotificationPreferencesRequest
    ): NotificationPreferencesResponse

    /**
     * Mark notification as read
     */
    @POST("intelligence/notifications/{id}/read/")
    suspend fun markNotificationRead(@Path("id") notificationId: String): NotificationItem

    /**
     * Dismiss notification
     */
    @POST("intelligence/notifications/{id}/dismiss/")
    suspend fun dismissNotification(@Path("id") notificationId: String): DismissResponse

    // ========== MEMORY ==========

    /**
     * Search user memory
     */
    @POST("intelligence/memory/search/")
    suspend fun searchMemory(@Body request: MemorySearchRequest): MemorySearchResponse

    /**
     * Remember a fact
     */
    @POST("intelligence/memory/remember/")
    suspend fun rememberFact(@Body request: RememberFactRequest): RememberFactResponse

    /**
     * Forget a fact
     */
    @POST("intelligence/memory/forget/")
    suspend fun forgetFact(@Body request: ForgetFactRequest): ForgetFactResponse

    /**
     * Get memory statistics
     */
    @GET("intelligence/memory/stats/")
    suspend fun getMemoryStats(): MemoryStatsResponse
}

// ========== REQUEST MODELS ==========

@Serializable
data class EmotionAnalyzeRequest(
    val text: String,
    val context: String? = null
)

@Serializable
data class NotificationPreferencesRequest(
    val morning_briefing: Boolean = true,
    val emotion_alerts: Boolean = true,
    val daily_summary: Boolean = true,
    val tips_enabled: Boolean = true
)

// ========== RESPONSE MODELS ==========

@Serializable
data class EmotionAnalyzeResponse(
    val primary_emotion: String? = null,
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val emotions: Map<String, Float> = emptyMap(),
    val sentiment: String? = null,
    val confidence: Float = 0f
)

@Serializable
data class EmotionsSummaryResponse(
    val status: String? = null,
    val average_valence: Float? = null,
    val average_arousal: Float? = null,
    val dominant_emotion: String? = null,
    val emotion_distribution: Map<String, Float> = emptyMap(),
    val trend: String? = null,
    val insights: List<String> = emptyList(),
    val period_days: Int = 7
)

@Serializable
data class TherapeuticTechniquesResponse(
    val techniques: List<TherapeuticTechnique> = emptyList(),
    val recommended_for: String? = null
)

@Serializable
data class TherapeuticTechnique(
    val id: String,
    val name: String,
    val description: String,
    val duration_minutes: Int? = null,
    val category: String? = null,
    val steps: List<String> = emptyList()
)

@Serializable
data class BriefingResponse(
    val greeting: String? = null,
    val mood_insight: String? = null,
    val recommendations: List<String> = emptyList(),
    val daily_tip: String? = null,
    val weather: String? = null,
    val generated_at: String? = null
)

@Serializable
data class NotificationsResponse(
    val items: List<NotificationItem> = emptyList(),
    val total: Int = 0,
    val unread: Int = 0
)

@Serializable
data class NotificationItem(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val is_read: Boolean = false,
    val created_at: String? = null,
    val action_url: String? = null
)

@Serializable
data class UnreadCountResponse(
    val count: Int = 0
)

@Serializable
data class NotificationPreferencesResponse(
    val morning_briefing: Boolean = true,
    val emotion_alerts: Boolean = true,
    val daily_summary: Boolean = true,
    val tips_enabled: Boolean = true,
    val quiet_hours_start: String? = null,
    val quiet_hours_end: String? = null,
    val push_enabled: Boolean = true,
    val email_enabled: Boolean = false,
    val max_daily_notifications: Int = 10,
    val digest_enabled: Boolean = false,
    val digest_time: Int? = null
)

@Serializable
data class DismissResponse(
    val success: Boolean = true,
    val message: String? = null
)

// ========== MEMORY MODELS ==========

@Serializable
data class MemorySearchRequest(
    val query: String,
    val limit: Int = 10
)

@Serializable
data class MemorySearchResponse(
    val facts: List<MemoryFact> = emptyList(),
    val episodes: List<MemoryEpisode> = emptyList(),
    val total: Int = 0
)

@Serializable
data class MemoryFact(
    val key: String,
    val value: String,
    val category: String? = null,
    val confidence: String? = null
)

@Serializable
data class MemoryEpisode(
    val id: String,
    val summary: String,
    val timestamp: String? = null
)

@Serializable
data class RememberFactRequest(
    val key: String,
    val value: String,
    val category: String? = null,
    val confidence: String? = null
)

@Serializable
data class RememberFactResponse(
    val success: Boolean = true,
    val key: String? = null,
    val message: String? = null
)

@Serializable
data class ForgetFactRequest(
    val key: String
)

@Serializable
data class ForgetFactResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class MemoryStatsResponse(
    @SerialName("facts_count") val factsCount: Int = 0,
    @SerialName("episodes_count") val episodesCount: Int = 0,
    @SerialName("habits_count") val habitsCount: Int = 0,
    @SerialName("total_messages") val totalMessages: Int = 0,
    @SerialName("data_points") val dataPoints: Int = 0,
    @SerialName("peak_hours") val peakHours: List<Int> = emptyList(),
    @SerialName("frequent_topics") val frequentTopics: List<String> = emptyList()
)
