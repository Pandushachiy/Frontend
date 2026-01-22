package com.health.companion.data.repositories

import com.health.companion.data.remote.api.DeleteMemoryResponse
import com.health.companion.data.remote.api.KnowledgeGraphResponse
import com.health.companion.data.remote.api.ProfileApi
import com.health.companion.data.remote.api.ProfileResponse
import com.health.companion.data.remote.api.RoutingStatsResponse
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    suspend fun getProfile(): Result<ProfileResponse>
    suspend fun getKnowledgeGraph(entityType: String? = null, limit: Int? = null): Result<KnowledgeGraphResponse>
    suspend fun getRoutingStats(): Result<RoutingStatsResponse>
    suspend fun deleteMemory(key: String): Result<DeleteMemoryResponse>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi
) : ProfileRepository {

    override suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            Result.success(profileApi.getProfile())
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile")
            Result.failure(e)
        }
    }

    override suspend fun getKnowledgeGraph(
        entityType: String?,
        limit: Int?
    ): Result<KnowledgeGraphResponse> {
        return try {
            Result.success(profileApi.getKnowledgeGraph(entityType, limit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load knowledge graph")
            Result.failure(e)
        }
    }

    override suspend fun getRoutingStats(): Result<RoutingStatsResponse> {
        return try {
            Result.success(profileApi.getRoutingStats())
        } catch (e: Exception) {
            Timber.e(e, "Failed to load routing stats")
            Result.failure(e)
        }
    }

    override suspend fun deleteMemory(key: String): Result<DeleteMemoryResponse> {
        return try {
            Result.success(profileApi.deleteMemory(key))
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete memory: $key")
            Result.failure(e)
        }
    }
}
