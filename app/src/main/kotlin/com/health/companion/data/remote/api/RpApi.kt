package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.*

interface RpApi {

    @GET("games/rp/models")
    suspend fun getModels(): RpModelsResponse

    @POST("games/rp/new")
    suspend fun newSession(@Body body: RpNewSessionRequest): RpSessionResponse

    @GET("games/rp/state")
    suspend fun getState(@Query("session_id") sessionId: String): RpSessionState

    @PATCH("games/rp/roles")
    suspend fun updateRoles(
        @Query("session_id") sessionId: String,
        @Body body: RpUpdateRolesRequest
    ): RpSessionState

    @GET("games/rp/sessions")
    suspend fun getSessions(): RpSessionsListResponse

    @DELETE("games/rp/{session_id}")
    suspend fun deleteSession(@Path("session_id") sessionId: String): Map<String, String>
}

@Serializable
data class RpModel(
    @SerialName("key") val modelKey: String,           // бэкенд отдаёт "key", не "model_key"
    @SerialName("display_name") val displayName: String,
    @SerialName("description") val description: String? = null,
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("base") val base: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false
) {
    // для бейджа используем display_name, т.к. отдельного model_name нет
    val modelName: String get() = displayName
}

@Serializable
data class RpModelsResponse(
    @SerialName("models") val models: List<RpModel>,
    @SerialName("default") val default: String? = null
)

@Serializable
data class RpNewSessionRequest(
    @SerialName("theme") val theme: String,
    @SerialName("char_name") val charName: String,
    @SerialName("char_description") val charDescription: String,
    @SerialName("user_name") val userName: String,
    @SerialName("user_description") val userDescription: String,
    @SerialName("model_key") val modelKey: String? = null
)

@Serializable
data class RpSessionResponse(
    @SerialName("session_id") val sessionId: String,
    @SerialName("char_name") val charName: String,
    @SerialName("char_personality") val charPersonality: String? = null,
    @SerialName("char_appearance") val charAppearance: String? = null,
    @SerialName("scenario") val scenario: String? = null,
    @SerialName("tone") val tone: String? = null,
    @SerialName("first_message") val firstMessage: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class RpSessionState(
    @SerialName("session_id") val sessionId: String,
    @SerialName("char_name") val charName: String,
    @SerialName("char_personality") val charPersonality: String? = null,
    @SerialName("char_appearance") val charAppearance: String? = null,
    @SerialName("scenario") val scenario: String? = null,
    @SerialName("tone") val tone: String? = null,
    @SerialName("history") val history: List<RpHistoryMessage> = emptyList(),
    @SerialName("title") val title: String? = null,
    @SerialName("model_name") val modelName: String? = null
)

@Serializable
data class RpHistoryMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class RpUpdateRolesRequest(
    @SerialName("char_name") val charName: String? = null,
    @SerialName("char_description") val charDescription: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_description") val userDescription: String? = null
)

@Serializable
data class RpSessionsListResponse(
    @SerialName("sessions") val sessions: List<RpSessionCard>
)

@Serializable
data class RpSessionCard(
    @SerialName("session_id") val sessionId: String,
    @SerialName("title") val title: String? = null,
    @SerialName("char_name") val charName: String,
    @SerialName("char_appearance") val charAppearance: String? = null,
    @SerialName("tone") val tone: String? = null,
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
