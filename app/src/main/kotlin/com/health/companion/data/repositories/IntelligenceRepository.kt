package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface IntelligenceRepository {
    // Emotions
    suspend fun analyzeEmotions(text: String, context: String? = null): Result<EmotionAnalyzeResponse>
    suspend fun getEmotionsSummary(days: Int = 7): Result<EmotionsSummaryResponse>
    suspend fun getTherapeuticTechniques(): Result<TherapeuticTechniquesResponse>
    
    // Briefing
    suspend fun generateBriefing(): Result<BriefingResponse>
    
    // Notifications
    suspend fun getNotifications(skip: Int = 0, limit: Int = 20): Result<NotificationsResponse>
    suspend fun getUnreadCount(): Result<UnreadCountResponse>
    suspend fun getNotificationPreferences(): Result<NotificationPreferencesResponse>
    suspend fun updateNotificationPreferences(preferences: NotificationPreferencesRequest): Result<NotificationPreferencesResponse>
    suspend fun markNotificationRead(notificationId: String): Result<NotificationItem>
    suspend fun dismissNotification(notificationId: String): Result<DismissResponse>
    
    // Memory
    suspend fun searchMemory(query: String, limit: Int = 10): Result<MemorySearchResponse>
    suspend fun rememberFact(key: String, value: String, category: String? = null): Result<RememberFactResponse>
    suspend fun forgetFact(key: String): Result<ForgetFactResponse>
    suspend fun getMemoryStats(): Result<MemoryStatsResponse>
}

@Singleton
class IntelligenceRepositoryImpl @Inject constructor(
    private val intelligenceApi: IntelligenceApi
) : IntelligenceRepository {

    // ========== EMOTIONS ==========

    override suspend fun analyzeEmotions(text: String, context: String?): Result<EmotionAnalyzeResponse> {
        return try {
            val request = EmotionAnalyzeRequest(text = text, context = context)
            val response = intelligenceApi.analyzeEmotions(request)
            Timber.d("Emotions analyzed: ${response.primary_emotion}, valence=${response.valence}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze emotions")
            Result.failure(e)
        }
    }

    override suspend fun getEmotionsSummary(days: Int): Result<EmotionsSummaryResponse> {
        return try {
            val response = intelligenceApi.getEmotionsSummary(days)
            Timber.d("Emotions summary: status=${response.status}, dominant=${response.dominant_emotion}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get emotions summary")
            Result.failure(e)
        }
    }

    override suspend fun getTherapeuticTechniques(): Result<TherapeuticTechniquesResponse> {
        return try {
            val response = intelligenceApi.getTherapeuticTechniques()
            Timber.d("Got ${response.techniques.size} therapeutic techniques")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get therapeutic techniques")
            Result.failure(e)
        }
    }

    // ========== BRIEFING ==========

    override suspend fun generateBriefing(): Result<BriefingResponse> {
        return try {
            val response = intelligenceApi.generateBriefing()
            Timber.d("Briefing generated: ${response.greeting?.take(50)}...")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate briefing")
            Result.failure(e)
        }
    }

    // ========== NOTIFICATIONS ==========

    override suspend fun getNotifications(skip: Int, limit: Int): Result<NotificationsResponse> {
        return try {
            val response = intelligenceApi.getNotifications(skip, limit)
            Timber.d("Got ${response.items.size} notifications, unread=${response.unread}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notifications")
            Result.failure(e)
        }
    }

    override suspend fun getUnreadCount(): Result<UnreadCountResponse> {
        return try {
            val response = intelligenceApi.getUnreadCount()
            Timber.d("Unread notifications: ${response.count}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get unread count")
            Result.failure(e)
        }
    }

    override suspend fun getNotificationPreferences(): Result<NotificationPreferencesResponse> {
        return try {
            val response = intelligenceApi.getNotificationPreferences()
            Timber.d("Got notification preferences")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get notification preferences")
            Result.failure(e)
        }
    }

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferencesRequest
    ): Result<NotificationPreferencesResponse> {
        return try {
            val response = intelligenceApi.updateNotificationPreferences(preferences)
            Timber.d("Updated notification preferences")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update notification preferences")
            Result.failure(e)
        }
    }

    override suspend fun markNotificationRead(notificationId: String): Result<NotificationItem> {
        return try {
            val response = intelligenceApi.markNotificationRead(notificationId)
            Timber.d("Marked notification $notificationId as read")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
            Result.failure(e)
        }
    }

    override suspend fun dismissNotification(notificationId: String): Result<DismissResponse> {
        return try {
            val response = intelligenceApi.dismissNotification(notificationId)
            Timber.d("Dismissed notification $notificationId")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to dismiss notification")
            Result.failure(e)
        }
    }

    // ========== MEMORY ==========

    override suspend fun searchMemory(query: String, limit: Int): Result<MemorySearchResponse> {
        return try {
            val request = MemorySearchRequest(query = query, limit = limit)
            val response = intelligenceApi.searchMemory(request)
            Timber.d("Memory search: found ${response.facts.size} facts, ${response.episodes.size} episodes")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to search memory")
            Result.failure(e)
        }
    }

    override suspend fun rememberFact(key: String, value: String, category: String?): Result<RememberFactResponse> {
        return try {
            val request = RememberFactRequest(key = key, value = value, category = category)
            val response = intelligenceApi.rememberFact(request)
            Timber.d("Remembered fact: $key = $value")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remember fact")
            Result.failure(e)
        }
    }

    override suspend fun forgetFact(key: String): Result<ForgetFactResponse> {
        return try {
            val request = ForgetFactRequest(key = key)
            val response = intelligenceApi.forgetFact(request)
            Timber.d("Forgot fact: $key")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to forget fact")
            Result.failure(e)
        }
    }

    override suspend fun getMemoryStats(): Result<MemoryStatsResponse> {
        return try {
            val response = intelligenceApi.getMemoryStats()
            Timber.d("Memory stats: ${response.factsCount} facts, ${response.episodesCount} episodes")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get memory stats")
            Result.failure(e)
        }
    }
}
