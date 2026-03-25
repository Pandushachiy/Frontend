package com.health.companion.data.repositories

import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.health.companion.BuildConfig
import com.health.companion.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends the FCM device token to the backend so the server can send push
 * notifications to this specific device via Firebase Cloud Messaging.
 *
 * Backend endpoint:
 *   POST /api/v1/notifications/fcm-token
 *   Authorization: Bearer <jwt>
 *   Body: {
 *     "token": "<fcm_token>",
 *     "platform": "android",
 *     "device_name": "Samsung Galaxy S21"   // optional
 *   }
 *   Response: { "status": "ok", "subscription_id": "uuid" }
 */
@Singleton
class FcmTokenRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Fetches the current FCM token from Firebase and uploads it to the backend.
     * Safe to call at any time — fails silently if Firebase isn't configured.
     */
    fun uploadCurrentToken() {
        scope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                Timber.d("FCM: got device token, uploading to backend")
                uploadToken(token)
            } catch (e: Exception) {
                Timber.w("FCM: token fetch failed — Firebase may not be configured: ${e.message}")
            }
        }
    }

    /**
     * Uploads a known FCM token to POST /api/v1/push/subscribe.
     * Called from [uploadCurrentToken] and from [AiHealthFirebaseMessagingService.onNewToken].
     */
    fun uploadToken(fcmToken: String) {
        scope.launch {
            try {
                val accessToken = tokenManager.getAccessToken() ?: run {
                    Timber.w("FCM: no access token — skipping token upload")
                    return@launch
                }

                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val body = JSONObject()
                    .put("token", fcmToken)
                    .put("platform", "android")
                    .put("device_name", deviceName)
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}/notifications/fcm-token")
                    .addHeader("Authorization", "Bearer $accessToken")
                    .post(body)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    Timber.d("FCM: token registered on backend ✅ ($deviceName)")
                } else {
                    val errorBody = response.body?.string()
                    Timber.w("FCM: token upload failed ${response.code}: $errorBody")
                }
                response.close()
            } catch (e: Exception) {
                Timber.e(e, "FCM: token upload error")
            }
        }
    }
}
