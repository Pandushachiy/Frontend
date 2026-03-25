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
 * When any request receives 401, this authenticator:
 * 1. Calls POST /api/v1/auth/refresh with the stored refresh_token
 * 2. Saves the new access_token + refresh_token
 * 3. Retries the original request with the new access_token
 *
 * Thread-safety: synchronized block ensures only one refresh at a time;
 * other threads reuse the freshly obtained token.
 *
 * Retry limit: gives up after [MAX_RETRIES] consecutive 401s on the same
 * call chain to prevent infinite loops.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val refreshLock = Object()

    companion object {
        private const val MAX_RETRIES = 2
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val url = response.request.url.encodedPath
        Timber.d("TokenAuthenticator: 401 on $url, attempting refresh")

        // Count how many prior 401 responses this call chain already produced
        var priorCount = 0
        var prior = response.priorResponse
        while (prior != null) {
            if (prior.code == 401) priorCount++
            prior = prior.priorResponse
        }
        if (priorCount >= MAX_RETRIES) {
            Timber.w("TokenAuthenticator: reached $MAX_RETRIES retries, giving up")
            return null
        }

        val failedRequestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(refreshLock) {
            val currentToken = tokenManager.getAccessTokenSync()

            if (currentToken != null && currentToken != failedRequestToken) {
                Timber.d("TokenAuthenticator: token already refreshed by another thread")
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshTokenSync()
            if (refreshToken == null) {
                Timber.w("TokenAuthenticator: no refresh_token, need re-login")
                return null
            }

            val newTokens = refreshTokenSync(refreshToken)

            if (newTokens != null) {
                Timber.d("TokenAuthenticator: refresh OK, retrying $url")
                runBlocking {
                    tokenManager.updateAccessToken(newTokens.access_token)
                    newTokens.refresh_token?.let { tokenManager.updateRefreshToken(it) }
                }
                return response.request.newBuilder()
                    .removeHeader("Authorization")
                    .header("Authorization", "Bearer ${newTokens.access_token}")
                    .build()
            } else {
                Timber.w("TokenAuthenticator: refresh failed")
                return null
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
                .header("Content-Type", "application/json")
                .build()

            Timber.d("TokenAuthenticator: calling ${request.url}")

            val response = refreshClient.newCall(request).execute()

            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    Timber.d("TokenAuthenticator: refresh response OK")
                    json.decodeFromString<TokenResponse>(body)
                }
            } else {
                val errorBody = response.body?.string()
                Timber.e("TokenAuthenticator: refresh failed ${response.code}: $errorBody")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "TokenAuthenticator: refresh request exception")
            null
        }
    }
}
