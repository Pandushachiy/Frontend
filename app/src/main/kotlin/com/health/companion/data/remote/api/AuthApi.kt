package com.health.companion.data.remote.api

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): TokenResponse
    
    @GET("auth/me")
    suspend fun getMe(): MeResponse
    
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): ChangePasswordResponse
    
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
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer"
)

@Serializable
data class MeResponse(
    val id: String,
    val email: String,
    val name: String? = null,
    val is_active: Boolean = true,
    val created_at: String? = null
)

@Serializable
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String
)

@Serializable
data class ChangePasswordResponse(
    val message: String
)

@Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer",
    val expires_in: Int? = null
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val refresh_token: String? = null,
    val token_type: String = "bearer",
    val expires_in: Int? = null
)

@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class LogoutResponse(
    val message: String
)
