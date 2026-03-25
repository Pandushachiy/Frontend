package com.health.companion.services

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deduplicates reminder notifications across WS and FCM channels.
 *
 * Problem: a reminder arrives via BOTH the WebSocket (while app is alive)
 * AND Firebase FCM. Without deduplication the user sees 2 system notifications.
 *
 * Rule:
 *  - WS handler marks the reminder_id as "handled" (WS delivered it to the UI).
 *  - FCM handler checks before posting: if already handled → skip.
 *  - Entries expire after TTL_MS so future identical reminder_ids are not blocked.
 */
@Singleton
class ReminderDeduplicator @Inject constructor() {

    private data class Entry(val timestampMs: Long)

    private val handled = mutableMapOf<String, Entry>()

    companion object {
        private const val TTL_MS = 60_000L  // 60 seconds window
    }

    /**
     * Mark a reminder_id as already delivered via WebSocket.
     * Subsequent FCM notification for same id within TTL will be suppressed.
     */
    fun markHandled(reminderId: String) {
        if (reminderId.isBlank()) return
        evictExpired()
        handled[reminderId] = Entry(System.currentTimeMillis())
        Timber.d("ReminderDedup: marked '$reminderId' as WS-handled")
    }

    /**
     * Returns true if this reminder_id was recently delivered via WebSocket
     * and the FCM notification should be suppressed.
     */
    fun isHandled(reminderId: String): Boolean {
        if (reminderId.isBlank()) return false
        evictExpired()
        return handled.containsKey(reminderId).also { hit ->
            if (hit) Timber.d("ReminderDedup: '$reminderId' already handled — suppressing FCM notification")
        }
    }

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        handled.entries.removeAll { (_, entry) -> now - entry.timestampMs > TTL_MS }
    }
}
