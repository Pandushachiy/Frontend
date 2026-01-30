package com.health.companion.utils

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Утилита для предзагрузки изображений в кэш
 * Используется для мгновенного отображения при переходе между экранами
 */
object ImagePreloader {
    
    /**
     * Предзагрузить список URL в фоне
     */
    suspend fun preloadImages(
        context: Context,
        urls: List<String>,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        urls.forEach { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .apply {
                        if (authToken != null && url.contains("46.17.99.76")) {
                            addHeader("Authorization", "Bearer $authToken")
                        }
                    }
                    // Не показываем результат, просто загружаем в кэш
                    .size(256, 256) // Thumbnail size
                    .build()
                
                context.imageLoader.enqueue(request)
            } catch (e: Exception) {
                Timber.w(e, "Failed to preload image: $url")
            }
        }
        Timber.d("Preloading ${urls.size} images...")
    }
    
    /**
     * Предзагрузить одно изображение в полном размере
     */
    suspend fun preloadFullImage(
        context: Context,
        url: String,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .apply {
                    if (authToken != null && url.contains("46.17.99.76")) {
                        addHeader("Authorization", "Bearer $authToken")
                    }
                }
                .build()
            
            context.imageLoader.enqueue(request)
            Timber.d("Preloading full image: $url")
        } catch (e: Exception) {
            Timber.w(e, "Failed to preload full image: $url")
        }
    }
}
