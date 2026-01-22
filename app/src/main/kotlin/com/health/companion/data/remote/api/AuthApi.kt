package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApi {
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    /**
     * Login - JSON with email/password
     */
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenResponse
    
    @POST("auth/logout")
    suspend fun logout(): LogoutResponse
}

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class RegisterResponse(
    val id: String? = null,
    val email: String? = null,
    val name: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer",
    val expires_in: Int = 1800
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val token_type: String = "bearer",
    val expires_in: Int = 1800
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class LogoutResponse(
    val status: String
)
