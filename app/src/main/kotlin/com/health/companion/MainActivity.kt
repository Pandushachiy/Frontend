package com.health.companion

import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as ComposeSurface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.health.companion.presentation.navigation.NavGraph
import com.health.companion.presentation.navigation.Route
import com.health.companion.presentation.theme.HealthCompanionTheme
import com.health.companion.utils.TokenManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ═══════════════════════════════════════════════════════════
        // 🚀 120Hz + GPU ACCELERATION (MAXIMUM PERFORMANCE)
        // ═══════════════════════════════════════════════════════════
        
        // 1. Force hardware acceleration on window level
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        
        // 2. Enable hardware layers for all views (GPU rendering)
        window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        
        // 3. Request highest refresh rate (120Hz if available)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            display?.let { d ->
                val modes = d.supportedModes
                val highestMode = modes.maxByOrNull { it.refreshRate }
                highestMode?.let { mode ->
                    window.attributes = window.attributes.apply {
                        preferredDisplayModeId = mode.modeId
                    }
                    android.util.Log.d("PERF", "✅ Display: ${mode.refreshRate}Hz (mode ${mode.modeId})")
                }
            }
        }
        
        // 4. Android 11+ - cutout mode for edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            android.util.Log.d("PERF", "✅ Edge-to-edge cutout mode enabled")
        }
        
        // 5. Disable window animations for instant transitions
        window.setWindowAnimations(0)
        
        // ═══════════════════════════════════════════════════════════
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // CRITICAL: Tell system we will handle insets ourselves
        // This is required for imePadding() to work correctly!
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            HealthCompanionTheme {
                val navController = rememberNavController()
                var isLoading by remember { mutableStateOf(true) }
                var startDestination by remember { mutableStateOf(Route.Auth.route) }
                val scope = rememberCoroutineScope()
                
                // Check if user is already logged in
                LaunchedEffect(Unit) {
                    scope.launch {
                        val token = tokenManager.getAccessToken()
                        startDestination = if (token != null) {
                            Route.Main.route
                        } else {
                            Route.Auth.route
                        }
                        isLoading = false
                    }
                }
                
                ComposeSurface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}
