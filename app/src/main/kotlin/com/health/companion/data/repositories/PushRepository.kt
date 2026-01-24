package com.health.companion.data.repositories

import android.os.Build
import com.health.companion.data.remote.api.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface PushRepository {
    suspend fun subscribe(fcmToken: String): Result<PushSubscribeResponse>
    suspend fun unsubscribe(fcmToken: String): Result<PushUnsubscribeResponse>
    suspend fun getSubscriptions(): Result<PushSubscriptionsResponse>
    suspend fun sendTestNotification(): Result<TestNotificationResponse>
    suspend fun getStats(): Result<PushStatsResponse>
}

@Singleton
class PushRepositoryImpl @Inject constructor(
    private val pushApi: PushApi
) : PushRepository {

    override suspend fun subscribe(fcmToken: String): Result<PushSubscribeResponse> {
        return try {
            val request = PushSubscribeRequest(
                endpoint = "fcm://$fcmToken",
                keys = mapOf(
                    "fcm_token" to fcmToken,
                    "platform" to "android"
                ),
                deviceInfo = mapOf(
                    "os" to "Android",
                    "sdk" to Build.VERSION.SDK_INT.toString(),
                    "model" to Build.MODEL,
                    "manufacturer" to Build.MANUFACTURER
                )
            )
            val response = pushApi.subscribe(request)
            Timber.d("Push subscription successful: ${response.subscriptionId}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to push notifications")
            Result.failure(e)
        }
    }

    override suspend fun unsubscribe(fcmToken: String): Result<PushUnsubscribeResponse> {
        return try {
            val request = PushUnsubscribeRequest(endpoint = "fcm://$fcmToken")
            val response = pushApi.unsubscribe(request)
            Timber.d("Push unsubscription successful")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to unsubscribe from push notifications")
            Result.failure(e)
        }
    }

    override suspend fun getSubscriptions(): Result<PushSubscriptionsResponse> {
        return try {
            val response = pushApi.getSubscriptions()
            Timber.d("Got ${response.total} push subscriptions")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get push subscriptions")
            Result.failure(e)
        }
    }

    override suspend fun sendTestNotification(): Result<TestNotificationResponse> {
        return try {
            val response = pushApi.sendTestNotification()
            Timber.d("Test notification sent: ${response.success}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send test notification")
            Result.failure(e)
        }
    }

    override suspend fun getStats(): Result<PushStatsResponse> {
        return try {
            val response = pushApi.getStats()
            Timber.d("Push stats: sent=${response.totalSent}, delivered=${response.totalDelivered}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get push stats")
            Result.failure(e)
        }
    }
}
