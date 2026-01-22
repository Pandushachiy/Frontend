package com.health.companion.data.repositories

import com.health.companion.data.remote.api.DashboardApi
import com.health.companion.data.remote.api.DashboardResponse
import timber.log.Timber
import javax.inject.Inject

interface DashboardRepository {
    suspend fun getDashboard(): Result<DashboardResponse>
}

class DashboardRepositoryImpl @Inject constructor(
    private val dashboardApi: DashboardApi
) : DashboardRepository {
    override suspend fun getDashboard(): Result<DashboardResponse> {
        return try {
            Result.success(dashboardApi.getDashboard())
        } catch (e: Exception) {
            Timber.e(e, "Failed to load dashboard")
            Result.failure(e)
        }
    }
}
