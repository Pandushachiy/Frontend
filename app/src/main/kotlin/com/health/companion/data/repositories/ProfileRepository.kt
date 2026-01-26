package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    fun getCachedProfile(): ProfileResponse?
    suspend fun getProfile(): Result<ProfileResponse>
    suspend fun getKnowledgeGraph(entityType: String? = null, limit: Int? = null): Result<KnowledgeGraphResponse>
    suspend fun deleteFact(id: String): Result<DeleteResponse>
    suspend fun clearAllFacts(): Result<DeleteResponse>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi
) : ProfileRepository {

    // Кэш на уровне Singleton
    @Volatile
    private var cachedProfile: ProfileResponse? = null
    
    @Volatile
    private var lastFetchTime: Long = 0L
    
    private val CACHE_TTL = 30_000L // 30 секунд
    
    override fun getCachedProfile(): ProfileResponse? = cachedProfile

    override suspend fun getProfile(): Result<ProfileResponse> {
        val now = System.currentTimeMillis()
        cachedProfile?.let { cached ->
            if (now - lastFetchTime < CACHE_TTL) {
                Timber.d("Profile from cache")
                return Result.success(cached)
            }
        }
        
        return try {
            val response = profileApi.getProfile()
            cachedProfile = response
            lastFetchTime = now
            Timber.d("Profile fetched: ${response.facts.size} facts")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile")
            cachedProfile?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    override suspend fun getKnowledgeGraph(entityType: String?, limit: Int?): Result<KnowledgeGraphResponse> {
        return try {
            Result.success(profileApi.getKnowledgeGraph(entityType, limit))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load knowledge graph")
            Result.failure(e)
        }
    }

    override suspend fun deleteFact(id: String): Result<DeleteResponse> {
        return try {
            val result = profileApi.deleteFact(id)
            // Обновим кэш
            cachedProfile = cachedProfile?.copy(
                facts = cachedProfile?.facts?.filterNot { it.id == id } ?: emptyList()
            )
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete fact: $id")
            Result.failure(e)
        }
    }

    override suspend fun clearAllFacts(): Result<DeleteResponse> {
        return try {
            val result = profileApi.clearAllFacts()
            // Обновим кэш
            cachedProfile = cachedProfile?.copy(facts = emptyList())
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear all facts")
            Result.failure(e)
        }
    }
}
