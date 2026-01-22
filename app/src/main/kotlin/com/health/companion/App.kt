package com.health.companion

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
        
        Timber.d("AI Health Companion App initialized")
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
