package com.health.companion.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app is currently in the foreground or background.
 * Register once in Application.onCreate() via ProcessLifecycleOwner.
 * Safe to query from any thread (@Volatile).
 */
@Singleton
class AppLifecycleTracker @Inject constructor() : DefaultLifecycleObserver {

    @Volatile
    var isInForeground: Boolean = false
        private set

    fun register(owner: LifecycleOwner) {
        owner.lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isInForeground = false
    }
}
