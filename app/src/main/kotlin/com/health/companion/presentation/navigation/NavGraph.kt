package com.health.companion.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.companion.presentation.screens.auth.LoginScreen
import com.health.companion.presentation.screens.auth.RegisterScreen
import com.health.companion.presentation.screens.chat.ChatScreen
import com.health.companion.presentation.screens.documents.DocumentsScreen
import com.health.companion.presentation.screens.dashboard.DashboardScreen
import com.health.companion.presentation.screens.profile.ProfileScreen
import com.health.companion.presentation.screens.settings.SettingsScreen

sealed class Route(val route: String) {
    object Auth : Route("auth")
    object AuthLogin : Route("auth/login")
    object AuthRegister : Route("auth/register")

    object Main : Route("main")
    object Dashboard : Route("dashboard")
    object Chat : Route("chat")
    object Documents : Route("documents")
    object Settings : Route("settings")
    object Profile : Route("profile")
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

// Dashboard первая, Mood убран
val bottomNavItems = listOf(
    BottomNavItem(Route.Dashboard.route, Icons.Outlined.Dashboard, Icons.Default.Dashboard, "Dashboard"),
    BottomNavItem(Route.Chat.route, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat, "Чат"),
    BottomNavItem(Route.Documents.route, Icons.Outlined.Description, Icons.Default.Description, "Документы"),
    BottomNavItem(Route.Settings.route, Icons.Outlined.Settings, Icons.Default.Settings, "Settings")
)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Route.Auth.route // Require login first
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if we should show bottom navigation
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        bottomNavItems.any { it.route == destination.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 0.dp
                    ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = if (currentDestination?.hierarchy?.any {
                                            it.route == item.route
                                        } == true
                                    ) {
                                        item.selectedIcon
                                    } else {
                                        item.icon
                                    },
                                    contentDescription = item.label,
                                    modifier = Modifier
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    style = if (currentDestination?.hierarchy?.any { it.route == item.route } == true) {
                                        MaterialTheme.typography.labelMedium
                                    } else {
                                        MaterialTheme.typography.labelSmall
                                    },
                                    maxLines = 1
                                ) 
                            },
                            selected = currentDestination?.hierarchy?.any { 
                                it.route == item.route 
                            } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                        )
                    }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
        ) {
            // Auth Flow
            navigation(
                route = Route.Auth.route,
                startDestination = Route.AuthLogin.route
            ) {
                composable(Route.AuthLogin.route) {
                    LoginScreen(
                        onNavigateToRegister = {
                            navController.navigate(Route.AuthRegister.route)
                        },
                        onLoginSuccess = {
                            navController.navigate(Route.Main.route) {
                                popUpTo(Route.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Route.AuthRegister.route) {
                    RegisterScreen(
                        onNavigateToLogin = {
                            navController.popBackStack()
                        },
                        onRegisterSuccess = {
                            navController.navigate(Route.Main.route) {
                                popUpTo(Route.Auth.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            // Main Flow (with BottomNavigation)
            navigation(
                route = Route.Main.route,
                startDestination = Route.Dashboard.route // Dashboard первая
            ) {
                composable(Route.Dashboard.route) {
                    Box(modifier = Modifier.padding(paddingValues)) {
                        DashboardScreen(
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }

                composable(Route.Chat.route) { backStackEntry ->
                    val owner = remember(backStackEntry) {
                        runCatching { navController.getBackStackEntry(Route.Main.route) }
                            .getOrElse { backStackEntry }
                    }
                    ChatScreen(
                        viewModel = hiltViewModel(owner),
                        bottomBarPadding = paddingValues
                    )
                }

                composable(Route.Documents.route) {
                    Box(modifier = Modifier.padding(paddingValues)) {
                        DocumentsScreen()
                    }
                }

                composable(Route.Settings.route) {
                    Box(modifier = Modifier.padding(paddingValues)) {
                        SettingsScreen(
                            onNavigateToLogin = {
                                navController.navigate(Route.Auth.route) {
                                    popUpTo(Route.Main.route) { inclusive = true }
                                }
                            },
                            onOpenProfile = {
                                navController.navigate(Route.Profile.route)
                            }
                        )
                    }
                }

                composable(Route.Profile.route) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
