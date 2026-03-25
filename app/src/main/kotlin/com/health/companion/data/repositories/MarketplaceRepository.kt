package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject

class MarketplaceRepository @Inject constructor(
    private val api: MarketplaceApi
) {
    suspend fun search(
        query: String,
        page: Int = 1,
        limit: Int = 20
    ): Result<MarketplaceResponse> = try {
        val resp = api.search(query, limit, page)
        Timber.d("Marketplace search '$query': ${resp.skills.size} results, total=${resp.total}")
        Result.success(resp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to search marketplace: $query")
        Result.failure(e)
    }

    suspend fun getFirstPage(): Result<MarketplaceResponse> = try {
        val resp = api.getAll(limit = 100, cursor = null)
        Timber.d("First page: ${resp.skills.size} skills, has_next=${resp.hasNext}")
        Result.success(resp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get first page")
        Result.failure(e)
    }

    suspend fun getPage(cursor: String): Result<MarketplaceResponse> = try {
        val resp = api.getAll(limit = 100, cursor = cursor)
        Result.success(resp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get page with cursor")
        Result.failure(e)
    }

    suspend fun getAllPages(): Result<List<MarketplaceSkill>> = try {
        val allSkills = mutableListOf<MarketplaceSkill>()
        var cursor: String? = null
        var page = 0
        do {
            val resp = api.getAll(limit = 100, cursor = cursor)
            allSkills.addAll(resp.skills)
            cursor = resp.nextCursor
            page++
        } while (resp.hasNext && cursor != null && page < 20)
        Result.success(allSkills)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get all pages")
        Result.failure(e)
    }

    suspend fun getCategories(): Result<List<MarketplaceCategory>> = try {
        val resp = api.getCategories()
        Timber.d("Marketplace categories: ${resp.categories.size}")
        Result.success(resp.categories)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get marketplace categories")
        Result.failure(e)
    }

    suspend fun getSkillDetails(slug: String): Result<MarketplaceSkill> = try {
        val resp = api.getSkillDetails(slug)
        Result.success(resp.skill)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get skill details: $slug")
        Result.failure(e)
    }

    suspend fun installSkill(slug: String): Result<MarketplaceInstallResponse> = try {
        val resp = api.installSkill(slug)
        Timber.d("Install skill $slug: status=${resp.status}")
        Result.success(resp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to install skill: $slug")
        Result.failure(e)
    }
}
