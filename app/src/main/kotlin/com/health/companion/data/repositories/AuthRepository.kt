package com.health.companion.data.repositories

import com.health.companion.data.remote.api.AuthApi
import com.health.companion.data.remote.api.LoginRequest
import com.health.companion.data.remote.api.LoginResponse
import com.health.companion.data.remote.api.RegisterRequest
import com.health.companion.data.remote.api.RegisterResponse
import com.health.companion.utils.TokenManager
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<LoginResponse>
    suspend fun register(email: String, password: String, name: String): Result<RegisterResponse>
    suspend fun logout(): Result<Unit>
    suspend fun isLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?
}

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            // JSON login request
            val response = authApi.login(LoginRequest(email = email, password = password))
            
            // Save tokens
            tokenManager.saveTokens(
                accessToken = response.access_token,
                refreshToken = response.refresh_token,
                userId = email
            )
            
            Timber.d("Login successful for: $email")
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "Login HTTP error: ${e.code()}")
            val errorBody = e.response()?.errorBody()?.string()
            Timber.e("Error body: $errorBody")
            
            val errorMessage = when (e.code()) {
                400 -> parseError(errorBody) ?: "Неверный формат запроса"
                401, 403 -> parseError(errorBody) ?: "Неверный email или пароль"
                404 -> "Сервис авторизации недоступен"
                422 -> parseError(errorBody) ?: "Проверьте введённые данные"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка сети: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Login timeout")
            Result.failure(Exception("Превышено время ожидания"))
        } catch (e: UnknownHostException) {
            Timber.e(e, "Login no internet")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Login failed: ${e.message}")
            Result.failure(Exception("Ошибка входа: ${e.message}"))
        }
    }
    
    override suspend fun register(
        email: String,
        password: String,
        name: String
    ): Result<RegisterResponse> {
        return try {
            val response = authApi.register(
                RegisterRequest(email = email, password = password, name = name)
            )
            
            Timber.d("Registration successful for: $email")
            
            // Registration returns user data, need to login to get tokens
            // Or if backend returns tokens, save them here
            Result.success(response)
        } catch (e: HttpException) {
            Timber.e(e, "Registration HTTP error: ${e.code()}")
            val errorBody = e.response()?.errorBody()?.string()
            Timber.e("Error body: $errorBody")
            
            val errorMessage = when (e.code()) {
                400 -> parseError(errorBody) ?: "Проверьте введённые данные"
                409 -> "Пользователь с таким email уже существует"
                422 -> parseError(errorBody) ?: "Неверный формат данных"
                500 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка регистрации: ${e.code()}"
            }
            Result.failure(Exception(errorMessage))
        } catch (e: SocketTimeoutException) {
            Timber.e(e, "Registration timeout")
            Result.failure(Exception("Превышено время ожидания"))
        } catch (e: UnknownHostException) {
            Timber.e(e, "Registration no internet")
            Result.failure(Exception("Нет подключения к интернету"))
        } catch (e: Exception) {
            Timber.e(e, "Registration failed: ${e.message}")
            Result.failure(Exception("Ошибка регистрации: ${e.message}"))
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            authApi.logout()
            tokenManager.clearTokens()
            Timber.d("Logout successful")
            Result.success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Timber.e(e, "Logout API failed, but tokens cleared")
            Result.success(Unit)
        }
    }
    
    override suspend fun isLoggedIn(): Boolean {
        return tokenManager.getAccessToken() != null
    }
    
    override suspend fun getCurrentUserId(): String? {
        return tokenManager.getUserId()
    }
    
    private fun parseError(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val json = JSONObject(errorBody)
            json.optString("detail", null)
        } catch (e: Exception) {
            null
        }
    }
}
