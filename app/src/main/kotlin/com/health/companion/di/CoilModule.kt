package com.health.companion.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.health.companion.BuildConfig
import com.health.companion.data.remote.TokenAuthenticator
import com.health.companion.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoilModule {
    
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        tokenManager: TokenManager,
        tokenAuthenticator: TokenAuthenticator
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150 * 1024 * 1024) // 150 MB
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(CoilAuthInterceptor(tokenManager))
                    // Auto-refresh expired tokens on 401 (same as main network client)
                    .authenticator(tokenAuthenticator)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()
            }
            .dispatcher(Dispatchers.IO.limitedParallelism(4))
            .apply {
                if (BuildConfig.DEBUG_MODE) {
                    logger(DebugLogger())
                }
            }
            .build()
    }
}

/**
 * Interceptor для добавления Authorization header к запросам изображений с нашего API.
 * Добавляет токен только к запросам на наш хост, не трогает внешние URLs (CDN и т.д.)
 */
private class CoilAuthInterceptor(
    private val tokenManager: TokenManager
) : Interceptor {
    private val apiHost: String by lazy {
        android.net.Uri.parse(BuildConfig.API_BASE_URL).host ?: ""
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val isOurApi = request.url.host == apiHost
        
        return if (isOurApi) {
            val token = tokenManager.getAccessTokenSync()
            val newRequest = if (token != null) {
                request.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                request
            }
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}
