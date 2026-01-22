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

    @DELETE("profile/memories/{key}")
    suspend fun deleteMemory(@Path("key") key: String): DeleteMemoryResponse
}

@Serializable
data class ProfileResponse(
    val userId: String,
    val name: String,
    val email: String,
    val memories: List<MemoryItem> = emptyList(),
    val documentsCount: Int = 0,
    val entitiesCount: Int = 0,
    val relationsCount: Int = 0
)

@Serializable
data class MemoryItem(
    val key: String,
    val value: String,
    val type: String,
    val createdAt: String
)

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

@Serializable
data class RoutingStatsResponse(
    val totalRequests: Int,
    val semanticOnly: Int,
    val aiRouting: Int,
    val semanticRate: String,
    val aiRate: String
)

@Serializable
data class DeleteMemoryResponse(
    val message: String
)
