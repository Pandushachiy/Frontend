package com.health.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
                
                Surface(
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
