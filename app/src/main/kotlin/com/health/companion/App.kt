package com.health.companion

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import dagger.hilt.android.HiltAndroidApp
import com.health.companion.utils.CrashLogger
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {
    
    @Inject
    lateinit var imageLoader: ImageLoader
    
    override fun onCreate() {
        super.onCreate()
        
        CrashLogger.install(this)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
        
        // Настраиваем Coil как глобальный ImageLoader
        Coil.setImageLoader(imageLoader)
        
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
