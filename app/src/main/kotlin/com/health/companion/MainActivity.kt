package com.health.companion

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as ComposeSurface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.health.companion.presentation.navigation.NavGraph
import com.health.companion.presentation.navigation.Route
import com.health.companion.presentation.screens.splash.SplashOverlay
import com.health.companion.presentation.theme.AppFontFamily
import com.health.companion.presentation.theme.AppThemeOption
import com.health.companion.presentation.theme.ChatBackgroundOption
import com.health.companion.presentation.theme.HealthCompanionTheme
import com.health.companion.presentation.theme.LocalAppFontFamily
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import com.health.companion.services.ChatConnectionService
import com.health.companion.services.NotificationHelper
import com.health.companion.utils.BatteryOptimizationHelper
import com.health.companion.utils.ThemeManager
import com.health.companion.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenManager: TokenManager
    @Inject lateinit var themeManager: ThemeManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private var navController: NavHostController? = null

    /**
     * Launched in onStart() every time the Activity becomes visible.
     * On Android 13+ the system dialog appears on the first call when the
     * permission hasn't been granted yet; subsequent calls are no-ops once
     * the user has made a choice.
     */
    private val notificationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Timber.d("POST_NOTIFICATIONS ${if (granted) "✅ granted" else "❌ denied"}")
        }

    // ────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────

    private var keepSystemSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSystemSplash }
        super.onCreate(savedInstanceState)

        // Edge-to-edge must be called first so the system applies transparent bars
        // before we apply our window attributes — avoids OEM-specific flash on startup.
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set nav bar to black immediately so it matches the dark splash background.
        // NavGraph's SideEffect overrides this once the main UI is loaded.
        window.navigationBarColor = android.graphics.Color.BLACK
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false

        // ── GPU acceleration ───────────────────────────────────
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        // ── High refresh rate (120Hz) ──────────────────────────
        // Only switch to a higher-refresh mode that has the SAME physical resolution
        // as the current mode.  On many OEM devices (Realme/ColorOS, Xiaomi/MIUI)
        // switching to a different resolution mode during onCreate causes a visible
        // display-mode-change flash.  Wrapped in try-catch because some OEM ROMs
        // throw on windowManager / display API calls.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display
                else @Suppress("DEPRECATION") windowManager.defaultDisplay
                display?.let { d ->
                    val currentMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) d.mode else null
                    val bestMode = d.supportedModes
                        ?.filter { mode ->
                            currentMode == null ||
                                (mode.physicalWidth == currentMode.physicalWidth &&
                                    mode.physicalHeight == currentMode.physicalHeight)
                        }
                        ?.maxByOrNull { it.refreshRate }
                    bestMode?.let { mode ->
                        window.attributes = window.attributes.apply {
                            preferredDisplayModeId = mode.modeId
                        }
                        Timber.d("Display mode: ${mode.physicalWidth}x${mode.physicalHeight} @ ${mode.refreshRate}Hz")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not set preferred display mode")
        }

        // ── Display cutout ─────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        // NOTE: window.setWindowAnimations(0) has been intentionally removed.
        // Setting it to 0 disables the system's window fade used to smooth the
        // first-frame render, which causes a bright "camera-flash" on OEM devices
        // (Realme/ColorOS, Xiaomi/MIUI).  Navigation transitions are now handled
        // entirely by Compose's NavHost enterTransition/exitTransition.
        // ──────────────────────────────────────────────────────

        // Handle notification deep link (app opened by tapping a notification)
        handleIncomingIntent(intent)

        setContent {
            val appTheme by themeManager.selectedTheme.collectAsStateWithLifecycle(
                initialValue = AppThemeOption.TEAL
            )
            val chatBackground by themeManager.selectedChatBackground.collectAsStateWithLifecycle(
                initialValue = ChatBackgroundOption.NIGHT
            )
            val textScale by themeManager.textScale.collectAsStateWithLifecycle(
                initialValue = com.health.companion.utils.ThemeManager.TEXT_SCALE_DEFAULT
            )
            val selectedFont by themeManager.fontFamily.collectAsStateWithLifecycle(
                initialValue = AppFontFamily.DEFAULT
            )

            // Масштаб шрифта — через LocalDensity.fontScale (все sp умножаются автоматически).
            // Шрифт — через LocalTextStyle: все Text() наследуют fontFamily если явно не задан.
            val baseDensity = androidx.compose.ui.platform.LocalDensity.current
            val scaledDensity = androidx.compose.ui.unit.Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * textScale
            )

            HealthCompanionTheme {
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides scaledDensity,
                    LocalAppFontFamily provides selectedFont,
                    LocalAppTheme provides appTheme,
                    LocalChatBackground provides chatBackground
                ) {
                    // Применяем шрифт через LocalTextStyle внутри MaterialTheme
                    // — все Text() с явными стилями без fontFamily унаследуют выбранный шрифт
                    androidx.compose.material3.ProvideTextStyle(
                        value = androidx.compose.ui.text.TextStyle(fontFamily = selectedFont.fontFamily)
                    ) {
                    val controller = rememberNavController()
                    navController = controller

                    val initialToken = remember { tokenManager.getAccessTokenSync() }
                    var isLoading by remember { mutableStateOf(initialToken == null) }
                    var startDestination by remember {
                        mutableStateOf(
                            if (initialToken != null) Route.Main.route else Route.Auth.route
                        )
                    }
                    val scope = rememberCoroutineScope()

                    var showSplash by remember { mutableStateOf(true) }
                    var splashFadingOut by remember { mutableStateOf(false) }
                    val splashProgress = remember { Animatable(0f) }

                    val splashAlpha by animateFloatAsState(
                        targetValue = if (splashFadingOut) 0f else 1f,
                        animationSpec = tween(durationMillis = 700),
                        finishedListener = { if (splashFadingOut) showSplash = false },
                        label = "splashAlpha"
                    )

                    LaunchedEffect(Unit) {
                        keepSystemSplash = false

                        if (isLoading) {
                            scope.launch {
                                val token = tokenManager.getAccessToken()
                                startDestination = if (token != null) Route.Main.route else Route.Auth.route
                                isLoading = false
                            }
                        }

                        splashProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 3800, easing = LinearEasing)
                        )
                        splashFadingOut = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        ComposeSurface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (isLoading && !showSplash) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (!isLoading) {
                                NavGraph(navController = controller, startDestination = startDestination, showSplash = showSplash)
                            }
                        }

                        if (showSplash) {
                            SplashOverlay(
                                progress = splashProgress.value,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(splashAlpha)
                            )
                        }
                    }
                }
            }
        }
        }
    }

    /**
     * onResume() ensures the window is fully interactive before we ask.
     * We check areNotificationsEnabled() — this works on ALL Android versions
     * including OPPO/Realme/OnePlus devices (ColorOS) where the standard
     * POST_NOTIFICATIONS dialog is sometimes suppressed.
     *
     * We also re-start the persistent WebSocket service here so it comes back
     * alive after the user logs in (token wasn't available in App.onCreate yet)
     * and after the app returns from background.  ChatConnectionService.start()
     * is idempotent — if the service is already running onStartCommand simply
     * checks the connection and returns START_STICKY without re-connecting.
     */
    override fun onResume() {
        super.onResume()
        requestNotificationPermissionIfNeeded()
        if (tokenManager.getAccessTokenSync() != null) {
            ChatConnectionService.start(this)
            // Ask user to exempt app from battery optimization.
            // On ColorOS (OPPO/Realme) also prompts to enable Autostart — without
            // this the OS kills the WebSocket service when the screen turns off.
            BatteryOptimizationHelper.requestIfNeeded(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    // ────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────

    /**
     * Robust notification permission request that works on all OEM ROMs.
     *
     * Strategy:
     *  1. Check NotificationManagerCompat.areNotificationsEnabled() — the ONLY
     *     reliable check across all Android versions and OEM ROMs (OPPO/ColorOS,
     *     Xiaomi/MIUI, Samsung/OneUI, etc.).
     *  2. On Android 13+ (TIRAMISU): try the standard runtime permission dialog.
     *  3. Fallback (or if already denied permanently): open the exact Settings page
     *     for this app's notification settings — the user can enable them there.
     *     This works on every device including heavily customised OEM ROMs.
     */
    private fun requestNotificationPermissionIfNeeded() {
        // Primary check: are notifications actually enabled for this app?
        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        if (notificationsEnabled) return  // already working, nothing to do

        Timber.d("Notifications are DISABLED — requesting permission")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                // Try standard dialog — on stock Android this shows a system dialog.
                // On some OEM ROMs it silently does nothing, so we also open Settings
                // below as a guaranteed fallback.
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // Notifications are disabled but no runtime permission to request
        // (Android 12-, or permission permanently denied on Android 13+).
        // Open the per-app notification settings — works on ALL devices and ROMs.
        openNotificationSettings()
    }

    /**
     * Opens the system settings page for this app's notifications.
     * The user can tap the toggle to enable them.
     * Works on all Android versions and OEM ROMs (OPPO/ColorOS, MIUI, etc.)
     */
    fun openNotificationSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Could not open notification settings")
        }
    }

    /**
     * When the app is opened from a notification, cancel stale notifications and
     * navigate to the appropriate screen:
     *  - open_reminder_id   → new-style reminder deep link (navigate to Chat)
     *  - open_screen="chat" → Chat screen (chat-response notifications)
     *  - open_screen="reminders" (legacy) → Chat screen
     *  - healthcompanion:// deep link → Chat screen
     *
     * The reminder ID is stored in [pendingReminderDeepLink] so the ChatViewModel
     * can retrieve it via [consumePendingReminderDeepLink] once the screen is ready.
     */
    private fun handleIncomingIntent(intent: Intent?) {
        // New-style reminder deep link sent from NotificationHelper.showReminderNotification()
        val openReminderId = intent?.getStringExtra("open_reminder_id")
        if (!openReminderId.isNullOrBlank()) {
            pendingReminderDeepLink = openReminderId
            Timber.d("Deep link from reminder notification: open_reminder_id=$openReminderId")
            navController?.navigate(Route.Chat.route) {
                launchSingleTop = true
                restoreState = true
            }
            return
        }

        val openScreen    = intent?.getStringExtra("open_screen")
        val isDeepLink    = intent?.data?.scheme == "healthcompanion"
        val reminderId    = intent?.getStringExtra("reminder_id")
        val conversationId = intent?.getStringExtra("conversation_id")

        val shouldOpenChat = openScreen == "chat"
            || openScreen == "reminders"
            || isDeepLink

        if (shouldOpenChat) {
            notificationHelper.cancelChatResponseNotification()
            navController?.navigate(Route.Chat.route) {
                launchSingleTop = true
                restoreState = true
            }
            if (!reminderId.isNullOrBlank() || !conversationId.isNullOrBlank()) {
                Timber.d("Opened from notification: reminder_id=$reminderId conversation_id=$conversationId")
            }
        }
    }

    // ── Reminder deep link handoff ──────────────────────────────────────────────
    // The ChatScreen/ViewModel calls consumePendingReminderDeepLink() once it is
    // ready, retrieves the reminder ID, and scrolls to or highlights the context.

    private var pendingReminderDeepLink: String? = null

    /**
     * Returns the pending reminder ID from a notification tap (if any) and clears it.
     * Call from ChatScreen or ChatViewModel after composition is stable.
     */
    fun consumePendingReminderDeepLink(): String? {
        val id = pendingReminderDeepLink
        pendingReminderDeepLink = null
        return id
    }
}
