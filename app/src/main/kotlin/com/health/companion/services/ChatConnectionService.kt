package com.health.companion.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.health.companion.BuildConfig
import com.health.companion.R
import com.health.companion.utils.AppLifecycleTracker
import com.health.companion.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Persistent ForegroundService that keeps a WebSocket connection alive so the
 * server can push AI-initiated messages even when the app is in the background.
 *
 * WHY A SEPARATE SERVICE (not ViewModel)?
 *  • ViewModel lives only while the Main nav graph is active.  If Android
 *    kills the Activity and recreates it, viewModelScope coroutines are cancelled.
 *  • A ForegroundService with foregroundServiceType="dataSync" has no time limit
 *    (unlike shortService which is capped at ~3 min) and is exempt from Doze.
 *  • The service owns its own OkHttp WebSocket — independent of the ViewModel.
 *
 * Lifecycle:
 *  1. ChatViewModel.init() calls start().
 *  2. Service connects WebSocket and starts a 25-second ping loop.
 *  3. Any incoming AI message while !isInForeground → shows a MessagingStyle
 *     push notification that opens the Chat screen.
 *  4. On network loss, service auto-reconnects with exponential back-off.
 *  5. ChatViewModel.onCleared() calls stop().
 */
@AndroidEntryPoint
class ChatConnectionService : Service() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var appLifecycleTracker: AppLifecycleTracker
    @Inject lateinit var reminderDeduplicator: ReminderDeduplicator

    companion object {
        private const val PRESENCE_NOTIFICATION_ID = 8001
        private const val PING_INTERVAL_MS = 20_000L   // 20 s — keeps NAT/proxy alive; server pongs within 25 s
        private const val RECONNECT_DELAY_BASE_MS = 3_000L
        private const val RECONNECT_DELAY_MAX_MS = 60_000L
        private const val WS_CLOSE_UNAUTHORIZED = 4003  // server rejects expired token — don't reconnect

        fun start(context: Context) {
            try {
                context.startForegroundService(
                    Intent(context, ChatConnectionService::class.java)
                )
                Timber.d("ChatConnectionService: start requested")
            } catch (e: Exception) {
                Timber.w(e, "Could not start ChatConnectionService")
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ChatConnectionService::class.java))
                Timber.d("ChatConnectionService: stop requested")
            } catch (e: Exception) {
                Timber.w(e, "Could not stop ChatConnectionService")
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    // stream_id → { conversation_id, accumulated text }
    private val streamBuffers = mutableMapOf<String, Pair<String, StringBuilder>>()

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    private val connectivityCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Timber.d("ChatConnectionService: network available — reconnecting")
            reconnectAttempt = 0
            scheduleReconnect(delayMs = 1_000L)
        }
        override fun onLost(network: Network) {
            Timber.d("ChatConnectionService: network lost")
            webSocket?.cancel()
            webSocket = null
        }
    }

    // ────────────────────────────────────────────────────────
    // Service lifecycle
    // ────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        showPresenceNotification()
        acquireLocks()
        registerNetworkCallback()
        connectWebSocket()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Already set up in onCreate; re-connect if somehow disconnected
        if (webSocket == null) connectWebSocket()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        pingJob?.cancel()
        reconnectJob?.cancel()
        webSocket?.close(1000, "Service destroyed")
        webSocket = null
        streamBuffers.clear()
        unregisterNetworkCallback()
        releaseLocks()
        super.onDestroy()
        Timber.d("ChatConnectionService: destroyed")
    }

    // ────────────────────────────────────────────────────────
    // Foreground notification
    // ────────────────────────────────────────────────────────

    private fun buildPresenceNotification(online: Boolean) =
        NotificationCompat.Builder(this, NotificationHelper.CHANNEL_CHAT)
            .setSmallIcon(R.drawable.ic_notification_fairy)
            .setContentTitle("FairyBerry")
            .setContentText(
                if (online) getString(R.string.connection_online)
                else getString(R.string.connection_waiting)
            )
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun showPresenceNotification() {
        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this, PRESENCE_NOTIFICATION_ID,
            buildPresenceNotification(online = false), fgType
        )
    }

    private fun updatePresenceNotification(online: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(PRESENCE_NOTIFICATION_ID, buildPresenceNotification(online))
    }

    // ────────────────────────────────────────────────────────
    // WebSocket
    // ────────────────────────────────────────────────────────

    private fun connectWebSocket() {
        val token = runBlocking { tokenManager.getAccessToken() } ?: run {
            Timber.w("ChatConnectionService: no token — cannot connect WebSocket")
            return
        }

        val wsUrl = "${BuildConfig.WS_URL}?token=$token"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d("ChatConnectionService: WebSocket connected")
                reconnectAttempt = 0
                updatePresenceNotification(online = true)
                startPingLoop()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.w(t, "ChatConnectionService: WebSocket failure (code ${response?.code})")
                pingJob?.cancel()
                this@ChatConnectionService.webSocket = null
                updatePresenceNotification(online = false)
                if (response?.code == 401 || response?.code == 403) {
                    refreshAndReconnect()
                } else {
                    scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d("ChatConnectionService: WebSocket closed ($code) $reason")
                pingJob?.cancel()
                this@ChatConnectionService.webSocket = null
                updatePresenceNotification(online = false)
                when (code) {
                    WS_CLOSE_UNAUTHORIZED -> {
                        Timber.w("ChatConnectionService: token rejected (4003) — refreshing")
                        refreshAndReconnect()
                    }
                    else -> scheduleReconnect()
                }
            }
        })
    }

    // ────────────────────────────────────────────────────────
    // Message handling + notification
    // ────────────────────────────────────────────────────────

    private fun handleIncomingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            Timber.d("ChatConnectionService: ws message type=$type")

            when (type) {
                // WS handshake confirmed — log server UTC time
                "connected" -> {
                    val serverTime = json.optJSONObject("data")?.optString("server_time")
                    Timber.d("ChatConnectionService: connected, server_time=$serverTime")
                }

                // AI-initiated chat message (legacy field — kept for compatibility)
                "message" -> {
                    val chunk = json.optString("chunk").trim()
                    if (chunk.isNotBlank() && !appLifecycleTracker.isInForeground) {
                        showChatNotification(chunk)
                    }
                }

                // New message stream starting — initialise buffer keyed by stream_id
                "stream_start" -> {
                    val data = json.optJSONObject("data") ?: return
                    val streamId = data.optString("stream_id").takeIf { it.isNotBlank() } ?: return
                    val conversationId = data.optString("conversation_id", "")
                    streamBuffers[streamId] = Pair(conversationId, StringBuilder())
                    Timber.d("ChatConnectionService: stream_start streamId=$streamId conv=$conversationId")
                }

                // Incremental text chunk — append to the right buffer
                "stream_chunk" -> {
                    val data = json.optJSONObject("data") ?: return
                    val streamId = data.optString("stream_id").takeIf { it.isNotBlank() } ?: return
                    val chunk = data.optString("chunk")
                    streamBuffers[streamId]?.second?.append(chunk)
                }

                // Stream finished — full content arrived; show notification if in background
                "stream_end" -> {
                    val data = json.optJSONObject("data") ?: return
                    val streamId = data.optString("stream_id")
                    // Prefer server-provided full_content; fall back to local buffer
                    val content = data.optString("full_content")
                        .takeIf { it.isNotBlank() }
                        ?: streamBuffers[streamId]?.second?.toString()
                        ?: ""
                    val conversationId = data.optString("conversation_id", "")
                    streamBuffers.remove(streamId)

                    Timber.d("ChatConnectionService: stream_end conv=$conversationId len=${content.length}")
                    if (content.isNotBlank() && !appLifecycleTracker.isInForeground) {
                        showChatNotification(content)
                    }
                }

                // Agent is thinking — log the stage; UI handles its own indicator via SSE/ViewModel
                "ai_thinking" -> {
                    val stage = json.optJSONObject("data")?.optString("stage") ?: "thinking"
                    Timber.d("ChatConnectionService: ai_thinking stage=$stage")
                }

                // New-style reminder push (top-level type="reminder")
                // WS only updates dedup cache; FCM is responsible for the system notification.
                "reminder" -> {
                    val reminderId = json.optString("reminder_id")
                    reminderDeduplicator.markHandled(reminderId)
                    Timber.d("ChatConnectionService: reminder WS received id=$reminderId — FCM will show notification")
                }

                // Legacy notification with reminder subtype — dedup only, FCM handles notification
                "notification" -> {
                    val data = json.optJSONObject("data") ?: return
                    val notifType = data.optString("notification_type")
                    if (notifType == "reminder") {
                        val reminderId = data.optString("reminder_id")
                        reminderDeduplicator.markHandled(reminderId)
                        Timber.d("ChatConnectionService: legacy reminder notification WS id=$reminderId — FCM will show")
                    } else if (!appLifecycleTracker.isInForeground) {
                        val msg = data.optString("message", "")
                            .ifBlank { data.optString("body", "") }
                        if (msg.isNotBlank()) showChatNotification(msg)
                    }
                }

                // Legacy reminder_push — dedup only, FCM handles notification
                "reminder_push" -> {
                    val data = json.optJSONObject("data") ?: return
                    val reminderId = data.optString("reminder_id")
                    reminderDeduplicator.markHandled(reminderId)
                    Timber.d("ChatConnectionService: reminder_push WS received id=$reminderId — FCM will show notification")
                }

                // Server heartbeat — connection is alive
                "pong" -> Timber.v("ChatConnectionService: pong received")
            }
        } catch (e: Exception) {
            Timber.w(e, "ChatConnectionService: failed to parse message: $text")
        }
    }

    /**
     * Converts legacy per-field reminder payload to the structured multi-line format
     * so it can be handled by ReminderMessageParser without a special branch.
     */
    private fun buildLegacyReminderText(data: org.json.JSONObject): String = buildString {
        val title = data.optString("title", getString(R.string.reminder_default_title))
        appendLine(title)
        val description = data.optString("description", "")
        if (description.isNotBlank()) appendLine("📝 $description")
        val category = data.optString("category", "")
        if (category.isNotBlank()) append("📁 $category")
    }.trim()

    private fun showChatNotification(content: String) {
        notificationHelper.showChatResponseNotification(
            messagePreview = content,
            conversationTitle = null
        )
        Timber.d("ChatConnectionService: notification shown for background message")
    }

    // ────────────────────────────────────────────────────────
    // Ping keepalive
    // ────────────────────────────────────────────────────────

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = serviceScope.launch {
            while (isActive) {
                delay(PING_INTERVAL_MS)
                val sent = webSocket?.send("""{"type":"ping"}""") ?: false
                Timber.v("ChatConnectionService: ping sent=$sent")
                if (!sent) break
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Token refresh before reconnect
    // ────────────────────────────────────────────────────────

    private fun refreshAndReconnect() {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Timber.w("ChatConnectionService: no refresh_token, cannot reconnect")
                return@launch
            }
            try {
                val body = kotlinx.serialization.json.Json.encodeToString(
                    com.health.companion.data.remote.api.RefreshTokenRequest.serializer(),
                    com.health.companion.data.remote.api.RefreshTokenRequest(refresh_token = refreshToken)
                )
                val request = okhttp3.Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}/auth/refresh")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val tokens = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString<com.health.companion.data.remote.api.TokenResponse>(
                            response.body?.string() ?: ""
                        )
                    tokenManager.updateAccessToken(tokens.access_token)
                    tokens.refresh_token?.let { tokenManager.updateRefreshToken(it) }
                    Timber.d("ChatConnectionService: token refreshed, reconnecting WS")
                    delay(500)
                    connectWebSocket()
                } else {
                    Timber.w("ChatConnectionService: token refresh failed ${response.code}")
                }
            } catch (e: Exception) {
                Timber.e(e, "ChatConnectionService: token refresh error")
            }
        }
    }

    // ────────────────────────────────────────────────────────
    // Reconnect with exponential back-off
    // ────────────────────────────────────────────────────────

    private fun scheduleReconnect(delayMs: Long? = null) {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            val backoff = delayMs
                ?: (RECONNECT_DELAY_BASE_MS * (1L shl reconnectAttempt.coerceAtMost(4)))
                    .coerceAtMost(RECONNECT_DELAY_MAX_MS)
            Timber.d("ChatConnectionService: reconnecting in ${backoff}ms (attempt $reconnectAttempt)")
            delay(backoff)
            reconnectAttempt++
            connectWebSocket()
        }
    }

    // ────────────────────────────────────────────────────────
    // Network callback
    // ────────────────────────────────────────────────────────

    private fun registerNetworkCallback() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val req = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(req, connectivityCallback)
        } catch (e: Exception) {
            Timber.w(e, "Could not register network callback")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(connectivityCallback)
        } catch (e: Exception) {
            Timber.w(e, "Could not unregister network callback")
        }
    }

    // ────────────────────────────────────────────────────────
    // Locks
    // ────────────────────────────────────────────────────────

    private fun acquireLocks() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AIHealthCompanion:WsConnection").also {
            it.setReferenceCounted(false)
            it.acquire()
            Timber.d("ChatConnectionService: WakeLock acquired")
        }

        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "AIHealthCompanion:WsWifi").also {
            it.setReferenceCounted(false)
            it.acquire()
            Timber.d("ChatConnectionService: WifiLock acquired")
        }
    }

    private fun releaseLocks() {
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (e: Exception) { }
        try { wifiLock?.let { if (it.isHeld) it.release() } } catch (e: Exception) { }
        wakeLock = null
        wifiLock = null
        Timber.d("ChatConnectionService: locks released")
    }
}
