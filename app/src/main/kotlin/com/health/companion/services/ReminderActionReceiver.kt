package com.health.companion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.health.companion.data.repositories.RemindersRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles notification action buttons (Complete / Snooze)
 * directly from the system notification tray.
 */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COMPLETE = "com.health.companion.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.health.companion.REMINDER_SNOOZE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_NOTIF_ID = "notif_id"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }

    @Inject
    lateinit var remindersRepository: RemindersRepository

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminder_id") ?: return

        // Dismiss the notification
        val notificationId = 5000 + (reminderId.hashCode() and 0xFFF)
        NotificationManagerCompat.from(context).cancel(notificationId)

        when (intent.action) {
            ACTION_COMPLETE -> {
                Timber.d("✅ Notification action: COMPLETE reminder $reminderId")
                CoroutineScope(Dispatchers.IO).launch {
                    remindersRepository.completeReminder(reminderId)
                        .onSuccess { Timber.d("Reminder $reminderId completed via notification") }
                        .onFailure { Timber.e(it, "Failed to complete reminder from notification") }
                }
            }
            ACTION_SNOOZE -> {
                Timber.d("⏰ Notification action: SNOOZE reminder $reminderId")
                CoroutineScope(Dispatchers.IO).launch {
                    remindersRepository.snoozeReminder(reminderId, 30)
                        .onSuccess { Timber.d("Reminder $reminderId snoozed via notification") }
                        .onFailure { Timber.e(it, "Failed to snooze reminder from notification") }
                }
            }
        }
    }
}
