package com.health.companion.data.repositories

import com.health.companion.data.local.ProfileCache
import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    suspend fun getProfileMe(): Result<ProfileMeResponse>
    suspend fun updateProfileMe(request: UpdateProfileRequest): Result<GenericStatusResponse>
    suspend fun updateMedCard(request: UpdateMedCardRequest): Result<GenericStatusResponse>

    // Legacy
    fun getCachedProfile(): ProfileResponse?
    suspend fun getProfile(): Result<ProfileResponse>
    suspend fun getKnowledgeGraph(entityType: String? = null, limit: Int? = null): Result<KnowledgeGraphResponse>
    suspend fun deleteFact(id: String): Result<DeleteResponse>
    suspend fun clearAllFacts(): Result<DeleteResponse>
}

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val profileCache: ProfileCache
) : ProfileRepository {

    // ── New profile endpoints ────────────────────────────────

    override suspend fun getProfileMe(): Result<ProfileMeResponse> = runCatching {
        profileApi.getProfileMe()
    }.onSuccess { p ->
        profileCache.update(
            name = p.name.takeIf { it.isNotBlank() },
            nickname = p.nickname,
            email = p.email,
            avatarEmoji = p.avatarEmoji
        )
    }.onFailure { Timber.e(it, "Failed to load profile/me") }

    override suspend fun updateProfileMe(request: UpdateProfileRequest): Result<GenericStatusResponse> = runCatching {
        profileApi.updateProfileMe(request)
    }.onFailure { Timber.e(it, "Failed to update profile/me") }

    override suspend fun updateMedCard(request: UpdateMedCardRequest): Result<GenericStatusResponse> = runCatching {
        profileApi.updateMedCard(request)
    }.onFailure { Timber.e(it, "Failed to update med-card") }

    // ── Legacy ───────────────────────────────────────────────

    @Volatile
    private var cachedProfile: ProfileResponse? = null

    @Volatile
    private var lastFetchTime: Long = 0L

    private val CACHE_TTL = 30_000L

    override fun getCachedProfile(): ProfileResponse? = cachedProfile

    override suspend fun getProfile(): Result<ProfileResponse> {
        val now = System.currentTimeMillis()
        cachedProfile?.let { cached ->
            if (now - lastFetchTime < CACHE_TTL) return Result.success(cached)
        }
        return try {
            val response = profileApi.getProfile()
            cachedProfile = response
            lastFetchTime = now
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to load profile")
            cachedProfile?.let { return Result.success(it) }
            Result.failure(e)
        }
    }

    override suspend fun getKnowledgeGraph(entityType: String?, limit: Int?): Result<KnowledgeGraphResponse> = runCatching {
        profileApi.getKnowledgeGraph(entityType, limit)
    }.onFailure { Timber.e(it, "Failed to load knowledge graph") }

    override suspend fun deleteFact(id: String): Result<DeleteResponse> = runCatching {
        val result = profileApi.deleteFact(id)
        cachedProfile = cachedProfile?.copy(facts = cachedProfile?.facts?.filterNot { it.id == id } ?: emptyList())
        result
    }.onFailure { Timber.e(it, "Failed to delete fact: $id") }

    override suspend fun clearAllFacts(): Result<DeleteResponse> = runCatching {
        val result = profileApi.clearAllFacts()
        cachedProfile = cachedProfile?.copy(facts = emptyList())
        result
    }.onFailure { Timber.e(it, "Failed to clear all facts") }
}
