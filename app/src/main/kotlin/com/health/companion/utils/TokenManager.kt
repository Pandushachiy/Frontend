package com.health.companion.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
    }
    
    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        userId: String
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = accessToken
            preferences[REFRESH_TOKEN] = refreshToken
            preferences[USER_ID] = userId
        }
        Timber.d("Tokens saved for user: $userId")
    }
    
    suspend fun getAccessToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[ACCESS_TOKEN]
        }.first()
    }
    
    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[REFRESH_TOKEN]
        }.first()
    }
    
    suspend fun getUserId(): String? {
        return dataStore.data.map { preferences ->
            preferences[USER_ID]
        }.first()
    }
    
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(USER_ID)
        }
        Timber.d("Tokens cleared")
    }
    
    suspend fun updateAccessToken(newToken: String) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = newToken
        }
        Timber.d("Access token updated")
    }
    
    suspend fun updateRefreshToken(newToken: String) {
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
}
