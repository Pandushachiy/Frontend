package com.health.companion.data.repositories

import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject

class SkillsRepository @Inject constructor(
    private val api: SkillsApi
) {
    // ─── User Skills ───
    
    suspend fun getSkills(): Result<SkillsResponse> = try {
        Result.success(api.getSkills())
    } catch (e: Exception) {
        Timber.e(e, "Failed to get skills")
        Result.failure(e)
    }

    suspend fun getSkillDetails(name: String): Result<SkillDetailsResponse> = try {
        Result.success(api.getSkillDetails(name))
    } catch (e: Exception) {
        Timber.e(e, "Failed to get skill details: $name")
        Result.failure(e)
    }

    suspend fun toggle(name: String, enabled: Boolean): Result<Boolean> = try {
        val resp = api.toggle(name, SkillToggleRequest(enabled))
        Result.success(resp.enabled)
    } catch (e: Exception) {
        Timber.e(e, "Failed to toggle skill $name")
        Result.failure(e)
    }

    suspend fun delete(name: String): Result<Unit> = try {
        api.delete(name)
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete skill $name")
        Result.failure(e)
    }

    suspend fun setConfigKey(name: String, key: String, value: String): Result<SkillConfigUpdateResponse> = try {
        val resp = api.setConfigKey(name, key, SkillConfigValueRequest(value))
        Result.success(resp)
    } catch (e: Exception) {
        Timber.e(e, "Failed to set config key $key for $name")
        Result.failure(e)
    }

    suspend fun revealConfigKey(name: String, key: String): Result<String> = try {
        val resp = api.revealConfigKey(name, key)
        Result.success(resp.value)
    } catch (e: Exception) {
        Timber.e(e, "Failed to reveal config key $key for $name")
        Result.failure(e)
    }

    // ─── Catalog ───

    suspend fun getCatalog(): Result<SkillsCatalogResponse> = try {
        Result.success(api.getCatalog())
    } catch (e: Exception) {
        Timber.e(e, "Failed to get catalog")
        Result.failure(e)
    }

    suspend fun getCategories(): Result<List<CategoryDTO>> = try {
        Result.success(api.getCategories().categories)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get categories")
        Result.failure(e)
    }

    suspend fun getCatalogSkillDetails(skillId: String): Result<CatalogSkillDetailsResponse> = try {
        Result.success(api.getCatalogSkillDetails(skillId))
    } catch (e: Exception) {
        Timber.e(e, "Failed to get catalog skill details: $skillId")
        Result.failure(e)
    }

    suspend fun installFromCatalog(skillId: String): Result<SkillInstallResponse> = try {
        Result.success(api.installFromCatalog(skillId))
    } catch (e: Exception) {
        Timber.e(e, "Failed to install skill $skillId")
        Result.failure(e)
    }

    // ─── OAuth ───

    suspend fun getOAuthProviders(): Result<List<OAuthProviderDTO>> = try {
        val resp = api.getOAuthProviders()
        Timber.d("OAuth providers: ${resp.providers.size} items")
        Result.success(resp.providers)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get OAuth providers")
        Result.failure(e)
    }

    suspend fun getOAuthConnections(): Result<List<OAuthConnectionDTO>> = try {
        val resp = api.getOAuthConnections()
        Timber.d("OAuth connections: ${resp.connections.size} items")
        Result.success(resp.connections)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get OAuth connections")
        Result.failure(e)
    }

    suspend fun getOAuthConnectUrl(provider: String): Result<OAuthConnectResponse> = try {
        Result.success(api.getOAuthConnectUrl(provider))
    } catch (e: Exception) {
        Timber.e(e, "Failed to get OAuth connect URL for $provider")
        Result.failure(e)
    }

    suspend fun disconnectOAuth(provider: String): Result<Unit> = try {
        api.disconnectOAuth(provider)
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to disconnect OAuth: $provider")
        Result.failure(e)
    }

    // ─── Webhooks ───

    suspend fun getWebhooks(): Result<List<WebhookDTO>> = try {
        Result.success(api.getWebhooks().webhooks)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get webhooks")
        Result.failure(e)
    }

    suspend fun createWebhook(name: String, skillName: String?): Result<CreateWebhookResponse> = try {
        Result.success(api.createWebhook(CreateWebhookRequest(name, skillName)))
    } catch (e: Exception) {
        Timber.e(e, "Failed to create webhook")
        Result.failure(e)
    }

    suspend fun deleteWebhook(id: String): Result<Unit> = try {
        api.deleteWebhook(id)
        Result.success(Unit)
    } catch (e: Exception) {
        Timber.e(e, "Failed to delete webhook $id")
        Result.failure(e)
    }

    suspend fun getWebhookHistory(): Result<List<WebhookEventDTO>> = try {
        Result.success(api.getWebhookHistory().events)
    } catch (e: Exception) {
        Timber.e(e, "Failed to get webhook history")
        Result.failure(e)
    }
}
