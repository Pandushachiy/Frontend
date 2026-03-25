package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

interface MarketplaceApi {

    @GET("skills/marketplace/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): MarketplaceResponse

    @GET("skills/marketplace")
    suspend fun getAll(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null
    ): MarketplaceResponse

    @GET("skills/marketplace/categories")
    suspend fun getCategories(): MarketplaceCategoriesResponse

    @GET("skills/marketplace/{slug}")
    suspend fun getSkillDetails(@Path("slug") slug: String): MarketplaceSkillDetailsResponse

    @POST("skills/marketplace/{slug}/install")
    suspend fun installSkill(@Path("slug") slug: String): MarketplaceInstallResponse
}

// ─── DTOs ───

@Serializable
data class MarketplaceSkill(
    val id: String = "",
    val slug: String = "",
    val name: String,
    val description: String = "",
    val author: String = "",
    val stars: Int = 0,
    val url: String? = null,
    val repo: String? = null,
    val icon: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_installed")
    val isInstalled: Boolean = false,
    val downloads: Int = 0
)

@Serializable
data class MarketplaceCategory(
    val id: String,
    val name: String,
    val icon: String? = null,
    val count: Int = 0
)

@Serializable
data class MarketplaceResponse(
    val skills: List<MarketplaceSkill> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
    @SerialName("has_next")
    val hasNext: Boolean = false,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
    val source: String = "clawhub"
)

@Serializable
data class MarketplaceCategoriesResponse(
    val categories: List<MarketplaceCategory> = emptyList()
)

@Serializable
data class MarketplaceSkillDetailsResponse(
    val skill: MarketplaceSkill
)

@Serializable
data class MarketplaceInstallResponse(
    val status: String = "",
    @SerialName("skill_name")
    val skillName: String = "",
    val message: String? = null,
    @SerialName("required_config")
    val requiredConfig: List<String> = emptyList()
)
