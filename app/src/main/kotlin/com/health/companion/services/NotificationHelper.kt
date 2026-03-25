package com.health.companion.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import androidx.core.content.ContextCompat
import com.health.companion.MainActivity
import com.health.companion.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized notification helper.
 *
 * Channels:
 *  - CHANNEL_CHAT              – IMPORTANCE_MIN  – silent sticky for ForegroundService
 *  - CHANNEL_CHAT_RESPONSE     – IMPORTANCE_HIGH – AI replies when app is in background
 *  - CHANNEL_REMINDERS_HIGH    – IMPORTANCE_HIGH – urgent/high priority reminders
 *  - CHANNEL_REMINDERS_MEDIUM  – IMPORTANCE_DEFAULT – medium priority reminders
 *  - CHANNEL_REMINDERS_LOW     – IMPORTANCE_LOW – silent reminders
 *  - CHANNEL_GENERAL           – IMPORTANCE_DEFAULT – misc system messages
 */
@Singleton
class NotificationHelper @Inject constructor(
    private val context: Context
) {

    companion object {
        // ── Chat channels ──
        const val CHANNEL_CHAT          = "chat_messages"
        const val CHANNEL_CHAT_RESPONSE = "chat_ai_response"

        // ── Reminder channels (split by priority so user can control each separately) ──
        const val CHANNEL_REMINDERS_HIGH   = "reminders_high"
        const val CHANNEL_REMINDERS_MEDIUM = "reminders_medium"
        const val CHANNEL_REMINDERS_LOW    = "reminders_low"

        // ── Misc ──
        const val CHANNEL_GENERAL = "general"

        // ── Notification ID ranges ──
        // Reminders: base 5000, 65536 unique slots (0xFFFF mask on UUID hashCode)
        const val REMINDER_NOTIF_BASE       = 5000
        const val REMINDER_GROUP_SUMMARY_ID = 4999
        const val SNOOZE_CONFIRM_ID         = 4998
        const val CHAT_RESPONSE_NOTIFICATION_ID = 2001
        private const val GENERAL_NOTIFICATION_ID = 1001

        // ── Group key ──
        const val GROUP_REMINDERS = "group_reminders"
    }

    // ────────────────────────────────────────────────────────
    // Channel setup
    // ────────────────────────────────────────────────────────

    /**
     * Create all notification channels — call once in Application.onCreate().
     *
     * Android caches channel importance after first creation and ignores later changes.
     * We delete & recreate the old single "reminders" channel to apply the new split.
     */
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // The notification sound URI used by all audible channels
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val soundAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // Delete previously created channels so Android picks up the new sound settings.
        // Android caches channel config on first creation and ignores code changes afterwards.
        // Channels used by an active foreground service cannot be deleted — wrap in try-catch.
        val channelsToReset = listOf(
            "reminders",             // legacy single channel
            CHANNEL_REMINDERS_HIGH,
            CHANNEL_REMINDERS_MEDIUM,
            CHANNEL_REMINDERS_LOW,
            CHANNEL_CHAT_RESPONSE,
            CHANNEL_GENERAL,
        )
        channelsToReset.forEach { id ->
            try { manager.deleteNotificationChannel(id) } catch (_: Exception) {}
        }
        // CHANNEL_CHAT is used by the foreground service — delete only if safe
        try { manager.deleteNotificationChannel(CHANNEL_CHAT) } catch (_: Exception) {}

        // ── Foreground service — completely silent, no badge ──────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CHAT, context.getString(R.string.notification_channel_background), NotificationManager.IMPORTANCE_MIN).apply {
                description = context.getString(R.string.notification_channel_background_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            },
        )

        // ── AI chat responses ─────────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CHAT_RESPONSE, context.getString(R.string.notification_channel_ai_response), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_ai_response_desc)
                setSound(defaultSound, soundAttrs)
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            },
        )

        // ── Urgent / high reminders — sound + vibration ───────────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS_HIGH, context.getString(R.string.notification_channel_urgent), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.notification_channel_urgent_desc)
                setSound(defaultSound, soundAttrs)
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                setShowBadge(true)
            },
        )

        // ── Medium reminders — sound, no vibration ────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS_MEDIUM, context.getString(R.string.notification_channel_reminders), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_reminders_desc)
                setSound(defaultSound, soundAttrs)   // explicit — IMPORTANCE_DEFAULT alone is not enough on all OEMs
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(false)
                setShowBadge(true)
            },
        )

        // ── Low / silent reminders ────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS_LOW, context.getString(R.string.notification_channel_quiet), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.notification_channel_quiet_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(true)
            },
        )

        // ── General / system ──────────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GENERAL, context.getString(R.string.notification_channel_system), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.notification_channel_system_desc)
                setSound(defaultSound, soundAttrs)
            },
        )

        Timber.d("✅ Notification channels created/reset (6 channels)")
    }

    // ────────────────────────────────────────────────────────
    // Permission
    // ────────────────────────────────────────────────────────

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // ────────────────────────────────────────────────────────
    // Reminder notifications — structured backend message
    // ────────────────────────────────────────────────────────

    /**
     * Show a reminder notification from the structured multi-line backend message.
     *
     * @param rawText  Multi-line text from backend WebSocket (content field) or FCM (body)
     * @param metadata Map with keys: "reminder_id", "priority", "category"
     */
    fun showReminderNotification(rawText: String, metadata: Map<String, String>) {
        if (!hasPermission()) {
            Timber.w("Cannot show reminder notification: POST_NOTIFICATIONS not granted")
            return
        }

        val parsed = ReminderMessageParser.parse(rawText, metadata)
        val notifId = REMINDER_NOTIF_BASE + (parsed.reminderId.hashCode() and 0xFFFF)
        val channel = channelForPriority(parsed.priority)

        val accentColor = when (parsed.priority) {
            "urgent" -> Color.parseColor("#FF3B30")
            "high"   -> Color.parseColor("#FF9500")
            "medium" -> Color.parseColor("#FFCC00")
            "low"    -> Color.parseColor("#34C759")
            else     -> Color.parseColor("#007AFF")
        }

        val notifPriority = when (parsed.priority) {
            "urgent" -> NotificationCompat.PRIORITY_MAX
            "high"   -> NotificationCompat.PRIORITY_HIGH
            "low"    -> NotificationCompat.PRIORITY_LOW
            else     -> NotificationCompat.PRIORITY_DEFAULT
        }

        // Deep link: opens Chat screen with the specific reminder context
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_reminder_id", parsed.reminderId)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification_fairy)
            .setColor(accentColor)
            .setContentTitle(parsed.title)
            .setContentText(parsed.description ?: "")
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setPriority(notifPriority)
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_REMINDERS)

        // Vibration only for high-priority reminders
        if (parsed.priority in listOf("urgent", "high")) {
            builder.setVibrate(longArrayOf(0, 300, 100, 300))
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            maybeShowGroupSummary()
            Timber.d("🔔 Reminder notification shown: '${parsed.title}' id=$notifId channel=$channel")
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException showing reminder notification")
        }
    }

    // ────────────────────────────────────────────────────────
    // Group summary (Android 7+)
    // ────────────────────────────────────────────────────────

    private fun maybeShowGroupSummary() {
        if (!hasPermission()) return
        val summary = NotificationCompat.Builder(context, CHANNEL_REMINDERS_MEDIUM)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(context.getString(R.string.reminders_group_title))
            .setGroup(GROUP_REMINDERS)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(REMINDER_GROUP_SUMMARY_ID, summary)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException showing group summary")
        }
    }

    // ────────────────────────────────────────────────────────
    // Chat response notifications
    // ────────────────────────────────────────────────────────

    /**
     * Show a MessagingStyle notification when AI responds while app is in background.
     *
     * @param messagePreview  First ~200 chars of the AI response.
     * @param conversationTitle  Optional conversation title shown as sub-text.
     */
    fun showChatResponseNotification(
        messagePreview: String,
        conversationTitle: String? = null,
    ) {
        if (!hasPermission()) {
            Timber.w("Cannot show chat notification: POST_NOTIFICATIONS not granted")
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "chat")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            CHAT_RESPONSE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val preview = messagePreview.take(200)

        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT_RESPONSE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("FairyBerry")
            .setContentText(preview)
            .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(CHAT_RESPONSE_NOTIFICATION_ID, notification)
            Timber.d("Chat response notification shown (preview: ${preview.take(40)}…)")
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException showing chat notification")
        }
    }

    /**
     * Dismiss the AI chat response notification.
     * Called when the user navigates to the Chat screen.
     */
    fun cancelChatResponseNotification() {
        NotificationManagerCompat.from(context).cancel(CHAT_RESPONSE_NOTIFICATION_ID)
    }

    // ────────────────────────────────────────────────────────
    // General notification
    // ────────────────────────────────────────────────────────

    fun showGeneralNotification(title: String, body: String) {
        if (!hasPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(GENERAL_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException showing general notification")
        }
    }

    // ────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────

    /** Select reminder channel based on priority. */
    fun channelForPriority(priority: String): String = when (priority) {
        "urgent", "high" -> CHANNEL_REMINDERS_HIGH
        "low"            -> CHANNEL_REMINDERS_LOW
        else             -> CHANNEL_REMINDERS_MEDIUM
    }

    /**
     * Build a broadcast PendingIntent for notification action buttons.
     * Uses a unique requestCode = notifId XOR action.hashCode() to prevent
     * PendingIntents with different extras from overwriting each other.
     */
    fun makeBroadcastIntent(
        action: String,
        reminderId: String,
        notifId: Int,
        snoozeMinutes: Int = 30,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_NOTIF_ID, notifId)
            putExtra(ReminderActionReceiver.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            notifId xor action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
