package com.health.companion.utils

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Preloads images into Coil's disk/memory cache in background.
 *
 * Auth is handled automatically by CoilAuthInterceptor inside the ImageLoader —
 * no need to pass tokens here.
 */
object ImagePreloader {

    /**
     * Preload a list of URLs into disk + memory cache.
     * Uses full size so the disk cache entry is reusable for any display size.
     */
    suspend fun preloadImages(
        context: Context,
        urls: List<String>,
        authToken: String? = null   // kept for API compatibility, interceptor handles auth
    ) = withContext(Dispatchers.IO) {
        urls.forEach { url ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size.ORIGINAL)          // store full-res bytes in disk cache
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
                context.imageLoader.enqueue(request)
            } catch (e: Exception) {
                Timber.w(e, "Failed to preload image: $url")
            }
        }
    }

    /**
     * Preload a single full-resolution image (e.g. right after generation).
     */
    suspend fun preloadFullImage(
        context: Context,
        url: String,
        authToken: String? = null   // kept for API compatibility, interceptor handles auth
    ) = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(Size.ORIGINAL)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
            context.imageLoader.enqueue(request)
        } catch (e: Exception) {
            Timber.w(e, "Failed to preload full image: $url")
        }
    }
}
