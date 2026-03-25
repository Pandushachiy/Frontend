package com.health.companion.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.health.companion.R
import com.health.companion.data.repositories.FcmTokenRepository
import com.health.companion.utils.AppLifecycleTracker
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * FCM entry-point for push notifications delivered by Firebase when the app
 * is in the background or completely killed.
 *
 * Message types from backend data payload:
 *  "reminder" / "reminder_push" → showReminderNotification (always visible)
 *  "chat" / "message" / "stream_end" → showChatResponseNotification (only when not in foreground)
 *  anything else / absent          → showGeneralNotification
 *
 * Note: when the backend sends a message with BOTH `notification` AND `data` fields
 * and the app is killed/in background, Android shows the system notification
 * automatically and onMessageReceived is NOT called. To receive callbacks in all
 * states, the backend should send data-only messages (no `notification` field).
 * When `notification` is present and the app IS in the foreground, this method
 * IS called — we show the notification ourselves in that case.
 */
@AndroidEntryPoint
class HealthCompanionMessagingService : FirebaseMessagingService() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var fcmTokenRepository: FcmTokenRepository
    @Inject lateinit var appLifecycleTracker: AppLifecycleTracker
    @Inject lateinit var reminderDeduplicator: ReminderDeduplicator

    // ────────────────────────────────────────────────────────
    // Token lifecycle
    // ────────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM: new device token received — uploading to backend")
        fcmTokenRepository.uploadToken(token)
    }

    // ────────────────────────────────────────────────────────
    // Message dispatch
    // ────────────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val type = data["type"] ?: ""
        Timber.d("FCM: onMessageReceived type='$type' hasSysNotif=${message.notification != null}")

        when (type) {
            "reminder", "reminder_push" -> {
                val reminderId = message.data["reminder_id"] ?: ""
                if (reminderDeduplicator.isHandled(reminderId)) {
                    // WS already delivered it to the chat UI — skip duplicate notification
                    Timber.d("FCM: reminder '$reminderId' already handled via WS — skipping notification")
                } else {
                    handleReminder(message)
                }
            }
            "chat", "message", "stream_end" -> {
                if (!appLifecycleTracker.isInForeground) handleChat(message)
            }
            else -> handleGeneral(message)
        }
    }

    // ────────────────────────────────────────────────────────
    // Handlers
    // ────────────────────────────────────────────────────────

    private fun handleReminder(message: RemoteMessage) {
        val data = message.data
        val title = data["title"]
            ?: message.notification?.title
            ?: getString(R.string.reminder_default_title)

        val description = data["description"]
            ?: data["body"]
            ?: message.notification?.body
            ?: ""

        val reminderId = data["reminder_id"] ?: ""
        val priority   = data["priority"]    ?: "medium"
        val category   = data["category"]   ?: "general"

        val rawText = buildString {
            appendLine(title)
            if (description.isNotBlank()) appendLine("📝 $description")
            if (category.isNotBlank())    append("📁 $category")
        }.trim()

        val metadata = mapOf(
            "reminder_id" to reminderId,
            "priority"    to priority,
            "category"    to category,
        )

        notificationHelper.showReminderNotification(rawText = rawText, metadata = metadata)
        Timber.d("FCM: reminder notification shown (id=$reminderId priority=$priority)")
    }

    private fun handleChat(message: RemoteMessage) {
        val data = message.data
        val content = data["content"]
            ?: data["full_content"]
            ?: data["body"]
            ?: message.notification?.body
            ?: return

        if (content.isBlank()) return

        notificationHelper.showChatResponseNotification(
            messagePreview     = content,
            conversationTitle  = data["conversation_title"],
        )
        Timber.d("FCM: chat notification shown (preview: ${content.take(40)}…)")
    }

    private fun handleGeneral(message: RemoteMessage) {
        val data  = message.data
        val notif = message.notification
        val title = data["title"] ?: notif?.title ?: return
        val body  = data["body"]  ?: notif?.body  ?: return
        notificationHelper.showGeneralNotification(title, body)
        Timber.d("FCM: general notification shown ('$title')")
    }
}
