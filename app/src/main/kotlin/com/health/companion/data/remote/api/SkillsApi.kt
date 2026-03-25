package com.health.companion.data.remote.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import retrofit2.http.*

interface SkillsApi {

    // ─── User Skills ───
    
    @GET("skills")
    suspend fun getSkills(): SkillsResponse

    @GET("skills/{name}")
    suspend fun getSkillDetails(@Path("name") name: String): SkillDetailsResponse

    @PUT("skills/{name}/toggle")
    suspend fun toggle(
        @Path("name") name: String,
        @Body request: SkillToggleRequest
    ): SkillToggleResponse

    @DELETE("skills/{name}")
    suspend fun delete(@Path("name") name: String): SkillDeleteResponse

    @POST("skills/{name}/config/{key}")
    suspend fun setConfigKey(
        @Path("name") name: String,
        @Path("key") key: String,
        @Body request: SkillConfigValueRequest
    ): SkillConfigUpdateResponse

    @GET("skills/{name}/config/{key}/reveal")
    suspend fun revealConfigKey(
        @Path("name") name: String,
        @Path("key") key: String
    ): ConfigRevealResponse

    // ─── Catalog ───

    @GET("skills/catalog")
    suspend fun getCatalog(): SkillsCatalogResponse

    @GET("skills/catalog/categories")
    suspend fun getCategories(): CategoriesResponse

    @GET("skills/catalog/{skill_id}")
    suspend fun getCatalogSkillDetails(@Path("skill_id") skillId: String): CatalogSkillDetailsResponse

    @POST("skills/catalog/{skill_id}/install")
    suspend fun installFromCatalog(@Path("skill_id") skillId: String): SkillInstallResponse

    // ─── OAuth ───

    @GET("skills/oauth/providers")
    suspend fun getOAuthProviders(): OAuthProvidersResponse

    @GET("skills/oauth/connections")
    suspend fun getOAuthConnections(): OAuthConnectionsResponse

    @GET("skills/oauth/connect/{provider}")
    suspend fun getOAuthConnectUrl(@Path("provider") provider: String): OAuthConnectResponse

    @DELETE("skills/oauth/{provider}")
    suspend fun disconnectOAuth(@Path("provider") provider: String): OAuthDisconnectResponse

    // ─── Webhooks ───

    @GET("skills/webhooks")
    suspend fun getWebhooks(): WebhooksResponse

    @POST("skills/webhooks")
    suspend fun createWebhook(@Body request: CreateWebhookRequest): CreateWebhookResponse

    @DELETE("skills/webhooks/{id}")
    suspend fun deleteWebhook(@Path("id") id: String): WebhookDeleteResponse

    @GET("skills/webhooks/history")
    suspend fun getWebhookHistory(): WebhookHistoryResponse
}

// ─── User Skills DTOs ───

@Serializable
data class SkillDTO(
    val name: String,
    @SerialName("display_name")
    val displayName: String? = null,
    val description: String = "",
    val enabled: Boolean = true,
    @SerialName("trigger_words")
    val triggerWords: List<String> = emptyList(),
    @SerialName("requires_config")
    val requiresConfig: List<String> = emptyList(),
    val     config: Map<String, String> = emptyMap(),
    @SerialName("config_set")
    @Serializable(with = ConfigSetSerializer::class)
    val configSet: Map<String, ConfigKeyInfoDTO> = emptyMap(),
    @SerialName("is_configured")
    val isConfigured: Boolean = true,
    val category: String? = null,
    val icon: String? = null
) {
    fun resolvedDisplayName(): String {
        if (!displayName.isNullOrBlank()) return displayName
        if (description.isNotBlank()) {
            return description.split(" ").take(5).joinToString(" ")
        }
        return name
            .removePrefix("test_")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }
}

@Serializable
data class ConfigKeyInfoDTO(
    val filled: Boolean = false,
    @SerialName("masked_value")
    val maskedValue: String? = null,
    val required: Boolean = false
)

/**
 * Handles both old format (boolean) and new format (object) for config_set:
 * Old: {"KEY": true}
 * New: {"KEY": {"filled": true, "masked_value": "fc-45...95", "required": true}}
 */
object ConfigSetSerializer : JsonTransformingSerializer<Map<String, ConfigKeyInfoDTO>>(
    MapSerializer(String.serializer(), ConfigKeyInfoDTO.serializer())
) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonObject) return element
        return buildJsonObject {
            element.forEach { (key, value) ->
                when (value) {
                    is JsonObject -> put(key, value)
                    is JsonPrimitive -> putJsonObject(key) {
                        put("filled", value.booleanOrNull ?: false)
                        put("required", true)
                    }
                    else -> putJsonObject(key) {
                        put("filled", false)
                        put("required", true)
                    }
                }
            }
        }
    }
}

@Serializable
data class SkillsResponse(
    val skills: List<SkillDTO> = emptyList(),
    @SerialName("enabled_count")
    val enabledCount: Int = 0,
    val total: Int = 0
)

@Serializable
data class SkillDetailsResponse(
    val skill: SkillDTO,
    @SerialName("config_status")
    val configStatus: Map<String, Boolean> = emptyMap(),
    @SerialName("config_set")
    @Serializable(with = ConfigSetSerializer::class)
    val configSet: Map<String, ConfigKeyInfoDTO> = emptyMap(),
    @SerialName("is_ready")
    val isReady: Boolean = true
)

@Serializable
data class SkillToggleRequest(
    val enabled: Boolean
)

@Serializable
data class SkillToggleResponse(
    val name: String = "",
    val enabled: Boolean = false
)

@Serializable
data class SkillDeleteResponse(
    val status: String = "ok"
)

@Serializable
data class SkillConfigValueRequest(
    val value: String
)

@Serializable
data class SkillConfigUpdateResponse(
    val status: String = "ok",
    @SerialName("masked_value")
    val maskedValue: String? = null
)

@Serializable
data class ConfigRevealResponse(
    val key: String = "",
    val value: String = ""
)

// ─── Catalog DTOs ───

@Serializable
data class CategoryDTO(
    val id: String,
    val name: String,
    val icon: String? = null
)

@Serializable
data class CatalogSkillDTO(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    val icon: String? = null,
    @SerialName("requires_config")
    val requiresConfig: List<String> = emptyList(),
    @SerialName("requires_oauth")
    val requiresOAuth: String? = null,
    @SerialName("is_installed")
    val isInstalled: Boolean = false
)

@Serializable
data class SkillsCatalogResponse(
    val skills: List<CatalogSkillDTO> = emptyList(),
    val categories: List<CategoryDTO> = emptyList(),
    @SerialName("by_category")
    val byCategory: Map<String, List<CatalogSkillDTO>> = emptyMap()
)

@Serializable
data class CategoriesResponse(
    val categories: List<CategoryDTO> = emptyList()
)

@Serializable
data class ConfigHelpDTO(
    val key: String,
    val label: String,
    val description: String = "",
    val placeholder: String = "",
    @SerialName("is_secret")
    val isSecret: Boolean = false
)

@Serializable
data class CatalogSkillDetailsResponse(
    val skill: CatalogSkillDTO,
    @SerialName("config_help")
    val configHelp: List<ConfigHelpDTO> = emptyList()
)

@Serializable
data class SkillInstallResponse(
    val status: String = "ok",
    val skill: SkillDTO? = null,
    @SerialName("requires_config")
    val requiresConfig: List<String> = emptyList()
)

// ─── OAuth DTOs ───

@Serializable
data class OAuthProviderDTO(
    val name: String,
    @SerialName("display_name")
    val displayName: String = "",
    val icon: String? = null,
    val configured: Boolean = false
)

@Serializable
data class OAuthProvidersResponse(
    val providers: List<OAuthProviderDTO> = emptyList()
)

@Serializable
data class OAuthConnectionDTO(
    val provider: String,
    @SerialName("display_name")
    val displayName: String = "",
    @SerialName("user_info")
    val userInfo: OAuthUserInfo? = null,
    @SerialName("connected_at")
    val connectedAt: String? = null
)

@Serializable
data class OAuthUserInfo(
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null
)

@Serializable
data class OAuthConnectionsResponse(
    val connections: List<OAuthConnectionDTO> = emptyList()
)

@Serializable
data class OAuthConnectResponse(
    @SerialName("auth_url")
    val authUrl: String,
    val provider: String
)

@Serializable
data class OAuthDisconnectResponse(
    val status: String = "ok"
)

// ─── Webhook DTOs ───

@Serializable
data class WebhookDTO(
    val id: String,
    val name: String,
    val url: String,
    @SerialName("skill_name")
    val skillName: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("last_triggered")
    val lastTriggered: String? = null,
    @SerialName("trigger_count")
    val triggerCount: Int = 0
)

@Serializable
data class WebhooksResponse(
    val webhooks: List<WebhookDTO> = emptyList()
)

@Serializable
data class CreateWebhookRequest(
    val name: String,
    @SerialName("skill_name")
    val skillName: String? = null
)

@Serializable
data class CreateWebhookResponse(
    val webhook: WebhookDTO? = null,
    val url: String = "",
    val secret: String? = null
)

@Serializable
data class WebhookDeleteResponse(
    val status: String = "ok"
)

@Serializable
data class WebhookEventDTO(
    val id: String,
    @SerialName("event_type")
    val eventType: String,
    @SerialName("webhook_id")
    val webhookId: String,
    val timestamp: String,
    val payload: String? = null,
    val status: String = "processed"
)

@Serializable
data class WebhookHistoryResponse(
    val events: List<WebhookEventDTO> = emptyList()
)
