package com.health.companion.di

import com.health.companion.BuildConfig
import com.health.companion.data.remote.TokenAuthenticator
import com.health.companion.data.remote.api.AuthApi
import com.health.companion.data.remote.api.ChatApi
import com.health.companion.data.remote.api.DashboardApi
import com.health.companion.data.remote.api.DocumentApi
import com.health.companion.data.remote.api.HealthApi
import com.health.companion.data.remote.api.ProfileApi
import com.health.companion.data.remote.api.VoiceApi
import com.health.companion.utils.TokenManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Singleton
    @Provides
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        isLenient = true
    }
    
    @Singleton
    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor { message ->
        Timber.tag("OkHttp").d(message)
    }.apply {
        level = if (BuildConfig.DEBUG_MODE) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }
    
    @Singleton
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager): Interceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Skip auth for login/register endpoints
        val path = originalRequest.url.encodedPath
        if (path.contains("auth/login") || path.contains("auth/register")) {
            return@Interceptor chain.proceed(originalRequest)
        }
        
        val token = runBlocking { tokenManager.getAccessToken() }
        
        val authenticatedRequest = if (token != null) {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        chain.proceed(authenticatedRequest)
    }
    
    @Singleton
    @Provides
    fun provideTokenAuthenticator(tokenManager: TokenManager): TokenAuthenticator {
        return TokenAuthenticator(tokenManager)
    }
    
    @Singleton
    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .addHeader("User-Agent", "HealthCompanion/1.0.0 Android")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(newRequest)
        }
        // Add authenticator for automatic token refresh on 401
        .authenticator(tokenAuthenticator)
        .build()
    
    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL + "/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    @Singleton
    @Provides
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    
    @Singleton
    @Provides
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
    
    @Singleton
    @Provides
    fun provideDocumentApi(retrofit: Retrofit): DocumentApi = retrofit.create(DocumentApi::class.java)

    @Singleton
    @Provides
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)
    
    @Singleton
    @Provides
    fun provideHealthApi(retrofit: Retrofit): HealthApi = retrofit.create(HealthApi::class.java)

    @Singleton
    @Provides
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)
    
    @Singleton
    @Provides
    fun provideVoiceApi(retrofit: Retrofit): VoiceApi = retrofit.create(VoiceApi::class.java)
}
