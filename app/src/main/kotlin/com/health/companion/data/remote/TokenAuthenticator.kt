package com.health.companion.data.remote

import com.health.companion.BuildConfig
import com.health.companion.data.remote.api.RefreshTokenRequest
import com.health.companion.data.remote.api.TokenResponse
import com.health.companion.utils.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that automatically refreshes expired JWT tokens.
 * 
 * Flow:
 * 1. Request fails with 401 Unauthorized
 * 2. Authenticator tries to refresh token using refresh_token
 * 3. If successful, retries original request with new access_token
 * 4. If refresh fails (refresh_token also expired), returns null -> user needs to re-login
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    // Use a separate OkHttpClient without authenticator to avoid infinite loops
    private val refreshClient = OkHttpClient.Builder()
        .build()
    
    @Volatile
    private var isRefreshing = false
    
    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.d("TokenAuthenticator: 401 received, attempting token refresh")
        
        // Prevent multiple simultaneous refresh attempts
        synchronized(this) {
            if (isRefreshing) {
                Timber.d("TokenAuthenticator: Already refreshing, skipping")
                return null
            }
            isRefreshing = true
        }
        
        try {
            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            
            if (refreshToken == null) {
                Timber.w("TokenAuthenticator: No refresh token available, user needs to re-login")
                runBlocking { tokenManager.clearTokens() }
                return null
            }
            
            // Call refresh endpoint
            val newTokens = refreshTokenSync(refreshToken)
            
            if (newTokens != null) {
                Timber.d("TokenAuthenticator: Token refreshed successfully")
                
                // Save new tokens
                runBlocking {
                    tokenManager.updateAccessToken(newTokens.access_token)
                    // If backend returns new refresh token, save it too
                    newTokens.refresh_token?.let { newRefresh ->
                        tokenManager.updateRefreshToken(newRefresh)
                    }
                }
                
                // Retry the original request with new token
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .addHeader("Authorization", "Bearer ${newTokens.access_token}")
                    .build()
            } else {
                Timber.w("TokenAuthenticator: Token refresh failed, user needs to re-login")
                runBlocking { tokenManager.clearTokens() }
                return null
            }
        } finally {
            synchronized(this) {
                isRefreshing = false
            }
        }
    }
    
    private fun refreshTokenSync(refreshToken: String): TokenResponse? {
        return try {
            val requestBody = json.encodeToString(
                RefreshTokenRequest.serializer(),
                RefreshTokenRequest(refresh_token = refreshToken)
            )
            
            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}/auth/refresh")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = refreshClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    json.decodeFromString<TokenResponse>(body)
                }
            } else {
                Timber.e("TokenAuthenticator: Refresh failed with code ${response.code}")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "TokenAuthenticator: Refresh request failed")
            null
        }
    }
}
