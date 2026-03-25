package com.health.companion

import android.app.Application
import android.util.Log
import androidx.lifecycle.ProcessLifecycleOwner
import coil.Coil
import coil.ImageLoader
import com.health.companion.data.repositories.FcmTokenRepository
import com.health.companion.services.ChatConnectionService
import com.health.companion.services.NotificationHelper
import com.health.companion.utils.AppLifecycleTracker
import com.health.companion.utils.CrashLogger
import com.health.companion.utils.TokenManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var appLifecycleTracker: AppLifecycleTracker

    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository
    
    override fun onCreate() {
        super.onCreate()
        
        CrashLogger.install(this)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
        
        // CRITICAL: Preload auth tokens SYNCHRONOUSLY before anything else
        // This MUST complete before user can interact with the app
        // Otherwise first requests will fail with 401
        runBlocking {
            tokenManager.preloadTokens()
        }

        // If the user is already logged in, start persistent services immediately —
        // don't wait for the Chat screen to open.
        if (tokenManager.getAccessTokenSync() != null) {
            // Keep the WebSocket alive for reminder delivery on any screen
            ChatConnectionService.start(this)
            // Re-register FCM token on every app start so the backend always has
            // a fresh token (tokens rotate after ~1 month or on OS reinstall)
            fcmTokenRepository.uploadCurrentToken()
        }

        // Настраиваем Coil как глобальный ImageLoader
        Coil.setImageLoader(imageLoader)
        
        // Create notification channels (must be before any notification is shown)
        notificationHelper.createChannels()

        // Register app lifecycle tracker so we can detect foreground/background state
        appLifecycleTracker.register(ProcessLifecycleOwner.get())

        Timber.d("AI Health Companion App initialized with optimized image caching")
    }
}

/**
 * Production logging tree that only logs warnings and errors
 */
class ProductionTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            // In production, send to crash reporting service
            // FirebaseCrashlytics.getInstance().log(message)
            // t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        }
    }
}
