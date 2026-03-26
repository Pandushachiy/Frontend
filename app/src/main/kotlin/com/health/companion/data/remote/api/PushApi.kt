package com.health.companion.data.remote.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST

interface PushApi {

    /**
     * Get VAPID public key for Web Push (not needed for FCM, but available)
     */
    @GET("push/vapid-key")
    suspend fun getVapidKey(): VapidKeyResponse

    /**
     * Subscribe to push notifications
     */
    @POST("push/subscribe")
    suspend fun subscribe(@Body request: PushSubscribeRequest): PushSubscribeResponse

    /**
     * Unsubscribe from push notifications
     */
    @POST("push/unsubscribe")
    suspend fun unsubscribe(@Body request: PushUnsubscribeRequest): PushUnsubscribeResponse

    /**
     * Get user's push subscriptions
     */
    @GET("push/subscriptions")
    suspend fun getSubscriptions(): List<PushSubscription>

    /**
     * Send test notification
     */
    @POST("push/test")
    suspend fun sendTestNotification(): TestNotificationResponse

    /**
     * Get push statistics
     */
    @GET("push/stats")
    suspend fun getStats(): PushStatsResponse
}

// ========== REQUEST MODELS ==========

@Serializable
data class PushSubscribeRequest(
    val platform: String,
    val token: String,
    @SerialName("device_name") val deviceName: String = "",
    @SerialName("device_info") val deviceInfo: Map<String, String> = emptyMap()
)

@Serializable
data class PushUnsubscribeRequest(
    @SerialName("subscription_id") val subscriptionId: String
)

// ========== RESPONSE MODELS ==========

@Serializable
data class VapidKeyResponse(
    @SerialName("public_key") val publicKey: String
)

@Serializable
data class PushSubscribeResponse(
    val success: Boolean = true,
    val message: String? = null,
    @SerialName("subscription_id") val subscriptionId: String? = null
)

@Serializable
data class PushUnsubscribeResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class PushSubscription(
    val id: String,
    val endpoint: String,
    val platform: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_used") val lastUsed: String? = null
)

@Serializable
data class TestNotificationResponse(
    val success: Boolean = true,
    val message: String? = null
)

@Serializable
data class PushStatsResponse(
    @SerialName("total_sent") val totalSent: Int = 0,
    @SerialName("total_delivered") val totalDelivered: Int = 0,
    @SerialName("total_failed") val totalFailed: Int = 0,
    @SerialName("active_subscriptions") val activeSubscriptions: Int = 0
)
