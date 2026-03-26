package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface ProfileApi {

    // ── Profile ────────────────────────────────────────────

    @GET("profile/me")
    suspend fun getProfileMe(): ProfileMeResponse

    @PATCH("profile/me")
    suspend fun updateProfileMe(@Body request: UpdateProfileRequest): GenericStatusResponse

    @PATCH("profile/med-card")
    suspend fun updateMedCard(@Body request: UpdateMedCardRequest): GenericStatusResponse

    // ── Legacy (used by canvas / settings / other features) ──

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

    @DELETE("profile/data")
    suspend fun deleteAllData(): DeleteAllDataResponse

    @GET("profile/stats")
    suspend fun getProfileStats(): ProfileStatsResponse

    // ── NSFW mode ───────────────────────────────────────────
    @GET("profile/nsfw-mode")
    suspend fun getNsfwMode(): NsfwModeResponse

    @PATCH("profile/nsfw-mode")
    suspend fun setNsfwMode(@Body request: NsfwToggleRequest): NsfwToggleResponse
}

// ═════════════════════════════════════════════════════════════
// New profile models
// ═════════════════════════════════════════════════════════════

@Serializable
data class ProfileMeResponse(
    val id: String? = null,
    val email: String? = null,
    val name: String = "",
    val nickname: String? = null,
    val age: Int? = null,
    val language: String? = null,
    val avatarEmoji: String? = null,
    val medCard: MedCard? = null
)

@Serializable
data class MedCard(
    val height: Int? = null,
    val weight: Int? = null,
    val bloodType: String? = null,
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList(),
    val medications: List<String> = emptyList()
)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val nickname: String? = null,
    val age: Int? = null,
    val language: String? = null,
    val avatarEmoji: String? = null
)

@Serializable
data class UpdateMedCardRequest(
    val height: Int? = null,
    val weight: Int? = null,
    val bloodType: String? = null,
    val allergies: List<String>? = null,
    val chronicConditions: List<String>? = null,
    val medications: List<String>? = null
)

@Serializable
data class GenericStatusResponse(
    val status: String = "ok",
    val message: String = ""
)

// ═════════════════════════════════════════════════════════════
// Legacy models (used by other features)
// ═════════════════════════════════════════════════════════════

@Serializable
data class DeleteAllDataResponse(
    val status: String = "ok",
    val message: String = "",
)

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
    val totalRequests: Int = 0,
    val semanticOnly: Int = 0,
    val aiRouting: Int = 0,
    val semanticRate: String = "0%",
    val aiRate: String = "0%"
)

@Serializable
data class DeleteResponse(
    val message: String = "",
    val id: String = ""
)

// ═════════════════════════════════════════════════════════════
// Profile stats
// ═════════════════════════════════════════════════════════════

@Serializable
data class ProfileStatsResponse(
    val conversations: ConversationStats? = null,
    val messages: MessageStats? = null,
    val costs: CostStats? = null,
    val skills: SkillStats? = null,
    @SerialName("mcp_servers") val mcpServers: McpStats? = null,
    @SerialName("generated_files") val generatedFiles: GeneratedFilesStats? = null
)

@Serializable
data class ConversationStats(val total: Int = 0)

@Serializable
data class MessageStats(
    val total: Int = 0,
    val user: Int = 0,
    val assistant: Int = 0
)

@Serializable
data class CostStats(
    @SerialName("all_time") val allTime: CostPeriod? = null,
    @SerialName("last_24h") val last24h: CostPeriod? = null,
    @SerialName("last_7d") val last7d: CostPeriod? = null,
    val note: String? = null
)

@Serializable
data class CostPeriod(
    val usd: Double = 0.0,
    val tokens: Long = 0,
    val requests: Int = 0
)

@Serializable
data class SkillStats(
    val installed: Int = 0,
    val names: List<String> = emptyList()
)

@Serializable
data class McpStats(
    val installed: Int = 0,
    val enabled: Int = 0,
    val names: List<String> = emptyList()
)

@Serializable
data class GeneratedFilesStats(
    @SerialName("total_files") val totalFiles: Int = 0,
    @SerialName("total_size_mb") val totalSizeMb: Double = 0.0
)

// ═════════════════════════════════════════════════════════════
// NSFW mode
// ═════════════════════════════════════════════════════════════

@Serializable
data class NsfwModeResponse(
    @SerialName("nsfw_mode")
    val nsfwMode: Boolean = false
)

@Serializable
data class NsfwToggleRequest(
    val enabled: Boolean
)

@Serializable
data class NsfwToggleResponse(
    val status: String = "ok",
    @SerialName("nsfw_mode")
    val nsfwMode: Boolean = false
)
