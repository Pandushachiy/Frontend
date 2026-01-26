package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    @GET("profile")
    suspend fun getProfile(): ProfileResponse

    @GET("profile/knowledge-graph")
    suspend fun getKnowledgeGraph(
        @Query("entity_type") entityType: String? = null,
        @Query("limit") limit: Int? = null
    ): KnowledgeGraphResponse

    @GET("profile/routing-stats")
    suspend fun getRoutingStats(): RoutingStatsResponse

    @DELETE("profile/facts/{id}")
    suspend fun deleteFact(@Path("id") id: String): DeleteResponse

    @DELETE("profile/clear-all-facts")
    suspend fun clearAllFacts(): DeleteResponse
}

// ========== PROFILE RESPONSE (NEW FORMAT) ==========

@Serializable
data class ProfileResponse(
    val user: UserInfo,
    val facts: List<FactItem> = emptyList(),
    val documents: List<DocumentItem> = emptyList(),
    val stats: Map<String, Int> = emptyMap()
)

@Serializable
data class UserInfo(
    val id: String,
    val name: String,
    val email: String,
    val avatarUrl: String? = null
)

@Serializable
data class FactItem(
    val id: String,
    val emoji: String,
    val text: String,
    val category: String,
    val canDelete: Boolean = true
)

@Serializable
data class DocumentItem(
    val id: String,
    val name: String,
    val type: String,
    val summary: String? = null,
    val entitiesCount: Int = 0,
    val uploadedAt: String
)

// ========== KNOWLEDGE GRAPH ==========

@Serializable
data class KnowledgeGraphResponse(
    val entities: List<Entity> = emptyList(),
    val relations: List<Relation> = emptyList(),
    val totalEntities: Int = 0,
    val totalRelations: Int = 0
)

@Serializable
data class Entity(
    val id: String,
    val type: String,
    val name: String,
    val description: String? = null,
    val confidence: Float = 0f
)

@Serializable
data class Relation(
    val id: String,
    val sourceName: String,
    val targetName: String,
    val type: String,
    val weight: Float = 0f
)

// ========== ROUTING STATS ==========

@Serializable
data class RoutingStatsResponse(
    val totalRequests: Int = 0,
    val semanticOnly: Int = 0,
    val aiRouting: Int = 0,
    val semanticRate: String = "0%",
    val aiRate: String = "0%"
)

// ========== DELETE RESPONSE ==========

@Serializable
data class DeleteResponse(
    val message: String,
    val success: Boolean = true
)
