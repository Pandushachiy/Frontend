package com.health.companion.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.health.companion.R
import androidx.core.app.ServiceCompat
import timber.log.Timber

/**
 * ForegroundService that keeps the app process alive and the network connection
 * active while an SSE streaming response is in progress.
 *
 * WHY THIS IS NEEDED:
 *  • When the screen turns off, Android's Doze/App-Standby can restrict network
 *    access and suspend CPU execution, causing OkHttp SSE connections to drop.
 *  • A running ForegroundService is explicitly exempt from Doze network restrictions.
 *  • PARTIAL_WAKE_LOCK prevents CPU suspension so the SSE coroutine keeps running.
 *  • WIFI_MODE_FULL_HIGH_PERF prevents the Wi-Fi chip from going to sleep and
 *    dropping the TCP socket that carries the SSE stream.
 *
 * Lifecycle:
 *  1. ChatViewModel calls start() before sending the SSE request.
 *  2. A silent "AI is responding…" persistent notification appears (IMPORTANCE_MIN).
 *  3. When SSE finishes (or errors out), ChatViewModel calls stop().
 *  4. onDestroy() releases all locks so the device can save power again.
 */
class ChatBackgroundService : Service() {

    companion object {
        const val ACTION_START = "com.health.companion.ACTION_START_STREAMING"

        private const val STREAMING_NOTIFICATION_ID = 7001
        private const val WAKE_LOCK_TAG = "AIHealthCompanion:SseStreaming"
        private const val WIFI_LOCK_TAG = "AIHealthCompanion:SseWifi"

        fun start(context: Context) {
            val intent = Intent(context, ChatBackgroundService::class.java)
                .apply { action = ACTION_START }
            try {
                context.startForegroundService(intent)
                Timber.d("ChatBackgroundService: start requested")
            } catch (e: Exception) {
                Timber.w(e, "Could not start ChatBackgroundService")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ChatBackgroundService::class.java))
                Timber.d("ChatBackgroundService: stop requested")
            } catch (e: Exception) {
                Timber.w(e, "Could not stop ChatBackgroundService")
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                showForegroundNotification()
                acquireLocks()
            }
            else -> stopSelf()
        }
        // START_STICKY: если OEM убьёт сервис, Android попытается его перезапустить.
        // Это вместе с WakeLock/WifiLock даёт максимальную защиту от обрыва стриминга.
        return START_STICKY
    }

    private fun showForegroundNotification() {
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_notification_fairy)
            .setContentTitle("FairyBerry")
            .setContentText(getString(R.string.ai_preparing_response))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        // DATA_SYNC не имеет 3-минутного лимита (в отличие от SHORT_SERVICE на Android 14+)
        // и уже объявлен в Manifest. Необходим для долгих задач (генерация изображений).
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, STREAMING_NOTIFICATION_ID, notification, fgType)
        Timber.d("ChatBackgroundService: foreground started")
    }

    /**
     * Acquire CPU wake lock and Wi-Fi lock to prevent the SSE TCP socket from
     * dropping when the screen turns off.
     *
     *  PARTIAL_WAKE_LOCK   – keeps CPU running, allows screen to turn off
     *  WIFI_MODE_FULL_HIGH_PERF – prevents Wi-Fi chip from sleeping (high performance,
     *                              only active while the lock is held)
     */
    private fun acquireLocks() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).also {
            it.setReferenceCounted(false)
            // 5-minute safety timeout — SSE responses shouldn't take longer
            it.acquire(5 * 60 * 1000L)
            Timber.d("ChatBackgroundService: WakeLock acquired")
        }

        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, WIFI_LOCK_TAG).also {
            it.setReferenceCounted(false)
            it.acquire()
            Timber.d("ChatBackgroundService: WifiLock acquired")
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            wakeLock = null
            Timber.d("ChatBackgroundService: WakeLock released")
        } catch (e: Exception) {
            Timber.w(e, "WakeLock release failed")
        }
        try {
            wifiLock?.let { if (it.isHeld) it.release() }
            wifiLock = null
            Timber.d("ChatBackgroundService: WifiLock released")
        } catch (e: Exception) {
            Timber.w(e, "WifiLock release failed")
        }
    }

    override fun onDestroy() {
        releaseLocks()
        super.onDestroy()
        Timber.d("ChatBackgroundService: destroyed, locks released")
    }
}
