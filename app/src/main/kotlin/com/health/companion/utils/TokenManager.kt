package com.health.companion.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_NAME = stringPreferencesKey("user_name")
        private val USER_EMAIL = stringPreferencesKey("user_email")
    }
    
    // In-memory cache to avoid DataStore async delays
    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var cachedRefreshToken: String? = null
    @Volatile
    private var cachedUserId: String? = null
    @Volatile
    private var isInitialized = false
    
    private val initMutex = Mutex()
    
    /**
     * Preload tokens from DataStore into memory cache.
     * Call this at app startup to ensure tokens are available immediately.
     */
    suspend fun preloadTokens() {
        initMutex.withLock {
            if (isInitialized) return
            
            try {
                val prefs = dataStore.data.first()
                cachedAccessToken = prefs[ACCESS_TOKEN]
                cachedRefreshToken = prefs[REFRESH_TOKEN]
                cachedUserId = prefs[USER_ID]
                isInitialized = true
                Timber.d("TokenManager: Tokens preloaded (hasToken: ${cachedAccessToken != null})")
            } catch (e: Exception) {
                Timber.e(e, "TokenManager: Failed to preload tokens")
            }
        }
    }

    suspend fun reloadFromStore() {
        try {
            val prefs = dataStore.data.first()
            cachedAccessToken = prefs[ACCESS_TOKEN]
            cachedRefreshToken = prefs[REFRESH_TOKEN]
            cachedUserId = prefs[USER_ID]
        } catch (e: Exception) {
            Timber.e(e, "TokenManager: Failed to reload tokens from store")
        }
    }
    
    /**
     * Get access token synchronously from cache (fast, for interceptor use)
     */
    fun getAccessTokenSync(): String? {
        return cachedAccessToken
    }
    
    /**
     * Get refresh token synchronously from cache (for TokenAuthenticator)
     */
    fun getRefreshTokenSync(): String? {
        return cachedRefreshToken
    }
    
    /**
     * Clear tokens synchronously (updates cache immediately, DataStore async)
     */
    fun clearTokensSync() {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        kotlinx.coroutines.runBlocking {
            try {
                dataStore.edit { prefs ->
                    prefs.remove(ACCESS_TOKEN)
                    prefs.remove(REFRESH_TOKEN)
                    prefs.remove(USER_ID)
                }
            } catch (e: Exception) {
                Timber.w(e, "clearTokensSync: DataStore clear failed")
            }
        }
        Timber.d("Tokens cleared from cache + DataStore (sync)")
    }
    
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userName: String? = null,
        userEmail: String? = null
    ) {
        // Update cache immediately
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        cachedUserId = userId
        isInitialized = true
        
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
            preferences[USER_ID] = userId
            userName?.let { preferences[USER_NAME] = it }
            userEmail?.let { preferences[USER_EMAIL] = it }
        }
        Timber.d("Tokens saved for user: $userId (name: $userName, email: $userEmail)")
    }
    
    suspend fun saveUserInfo(name: String, email: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_EMAIL] = email
        }
        Timber.d("User info saved: $name, $email")
    }
    
    suspend fun getUserName(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_NAME]
        }.first()
    }
    
    suspend fun getUserEmail(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_EMAIL]
        }.first()
    }
    
    suspend fun getAccessToken(): String? {
        // Return from cache if initialized, otherwise load from DataStore
        if (isInitialized) return cachedAccessToken
        preloadTokens()
        return cachedAccessToken
    }
    
    suspend fun getRefreshToken(): String? {
        // Return from cache if initialized, otherwise load from DataStore
        if (isInitialized) return cachedRefreshToken
        preloadTokens()
        return cachedRefreshToken
    }
    
    suspend fun getUserId(): String? {
        // Return from cache if initialized, otherwise load from DataStore
        if (isInitialized) return cachedUserId
        preloadTokens()
        return cachedUserId
    }
    
    suspend fun clearTokens() {
        // Clear cache immediately
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(USER_ID)
        }
        Timber.d("Tokens cleared")
    }
    
    suspend fun updateAccessToken(newToken: String) {
        // Update cache immediately
        cachedAccessToken = newToken
        
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = newToken
        }
        Timber.d("Access token updated")
    }
    
    suspend fun updateRefreshToken(newToken: String) {
        // Update cache immediately
        cachedRefreshToken = newToken
        
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN] = newToken
        }
        Timber.d("Refresh token updated")
    }
    
    /**
     * Check if user has valid tokens (for quick check without verification)
     */
    suspend fun hasTokens(): Boolean {
        return getAccessToken() != null
    }
    
    /**
     * Check if tokens are loaded (sync, for quick checks)
     */
    fun hasTokensSync(): Boolean {
        return cachedAccessToken != null
    }
}
