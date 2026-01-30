package com.health.companion.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.health.companion.BuildConfig
import com.health.companion.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        tokenManager: TokenManager
    ): ImageLoader {
        return ImageLoader.Builder(context)
            // Memory cache - 25% of app memory
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // Disk cache - 100MB для оффлайн доступа
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build()
            }
            // Агрессивное кэширование
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Crossfade для плавности
            .crossfade(150)
            // OkHttp с авторизацией для защищённых эндпоинтов
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(AuthInterceptor(tokenManager))
                    .build()
            }
            // Dispatcher для фоновой загрузки
            .dispatcher(Dispatchers.IO.limitedParallelism(4))
            // Debug logger только в debug билде
            .apply {
                if (BuildConfig.DEBUG_MODE) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

/**
 * Interceptor для добавления Authorization header к запросам изображений
 */
private class AuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        
        // Добавляем токен только для наших API эндпоинтов
        val isOurApi = request.url.host == "46.17.99.76"
        
        return if (isOurApi) {
            val token = runBlocking { tokenManager.getAccessToken() }
            val newRequest = request.newBuilder()
                .apply {
                    if (token != null) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}
