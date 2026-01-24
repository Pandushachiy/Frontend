package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardRepository {
    suspend fun getDashboard(): Result<DashboardResponse>
    suspend fun getMoodChart(days: Int = 7): Result<MoodChartResponse>
    suspend fun getStreak(): Result<StreakResponse>
    suspend fun getEmotionalState(): Result<EmotionalStateResponse>
    suspend fun getMemorySummary(): Result<MemorySummaryResponse>
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi
) : DashboardRepository {
    
    override suspend fun getDashboard(): Result<DashboardResponse> {
        return try {
            val response = dashboardApi.getDashboard()
            Timber.d("Dashboard loaded: ${response.widgets.size} widgets")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load dashboard")
            Result.failure(e)
        }
    }

    override suspend fun getMoodChart(days: Int): Result<MoodChartResponse> {
        return try {
            val response = dashboardApi.getMoodChart(days)
            Timber.d("Mood chart: ${response.data.entriesCount} entries over ${response.periodDays} days")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load mood chart")
            Result.failure(e)
        }
    }

    override suspend fun getStreak(): Result<StreakResponse> {
        return try {
            val response = dashboardApi.getStreak()
            Timber.d("Streak: ${response.data.currentStreak} days")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load streak")
            Result.failure(e)
        }
    }

    override suspend fun getEmotionalState(): Result<EmotionalStateResponse> {
        return try {
            val response = dashboardApi.getEmotionalState()
            Timber.d("Emotional state: ${response.primaryEmotion}, valence=${response.valence}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load emotional state")
            Result.failure(e)
        }
    }

    override suspend fun getMemorySummary(): Result<MemorySummaryResponse> {
        return try {
            val response = dashboardApi.getMemorySummary()
            Timber.d("Memory summary: ${response.factsCount} facts, ${response.episodesCount} episodes")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load memory summary")
            Result.failure(e)
        }
    }
}
