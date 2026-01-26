package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface DashboardRepository {
    fun getCachedDashboard(): DashboardResponse?
    suspend fun getDashboard(): Result<DashboardResponse>
    suspend fun getEmotionalState(): Result<EmotionalStateResponse>
    suspend fun getMemorySummary(): Result<MemorySummaryResponse>
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi
) : DashboardRepository {
    
    // Кэш на уровне Singleton - сохраняется между переходами
    @Volatile
    private var cachedDashboard: DashboardResponse? = null
    
    @Volatile
    private var cachedEmotionalState: EmotionalStateResponse? = null
    
    @Volatile
    private var cachedMemorySummary: MemorySummaryResponse? = null
    
    @Volatile
    private var lastFetchTime: Long = 0L
    
    private val CACHE_TTL = 30_000L // 30 секунд TTL
    
    override fun getCachedDashboard(): DashboardResponse? = cachedDashboard
    
    override suspend fun getDashboard(): Result<DashboardResponse> {
        // Если кэш свежий - вернём его без запроса
        val now = System.currentTimeMillis()
        cachedDashboard?.let { cached ->
            if (now - lastFetchTime < CACHE_TTL) {
                Timber.d("Dashboard from cache (age: ${now - lastFetchTime}ms)")
                return Result.success(cached)
            }
        }
        
        return try {
            val response = dashboardApi.getDashboard()
            cachedDashboard = response
            lastFetchTime = now
            Timber.d("Dashboard fetched: streak=${response.streak.days}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load dashboard")
            // При ошибке вернём кэш если есть
            cachedDashboard?.let { 
                Timber.d("Returning stale cache due to error")
                return Result.success(it) 
            }
            Result.failure(e)
        }
    }

    override suspend fun getEmotionalState(): Result<EmotionalStateResponse> {
        return try {
            val response = dashboardApi.getEmotionalState()
            cachedEmotionalState = response
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load emotional state")
            cachedEmotionalState?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    override suspend fun getMemorySummary(): Result<MemorySummaryResponse> {
        return try {
            val response = dashboardApi.getMemorySummary()
            cachedMemorySummary = response
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load memory summary")
            cachedMemorySummary?.let { return Result.success(it) }
            Result.failure(e)
        }
    }
}
