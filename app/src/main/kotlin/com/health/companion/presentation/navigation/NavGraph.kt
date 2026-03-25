package com.health.companion.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
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
import com.health.companion.presentation.theme.LocalAppTheme
import com.health.companion.presentation.theme.LocalChatBackground
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.health.companion.R
import com.health.companion.presentation.screens.auth.ChangePasswordScreen
import com.health.companion.presentation.screens.auth.LoginScreen
import com.health.companion.presentation.screens.auth.RegisterScreen
import com.health.companion.presentation.screens.canvas.CanvasViewModel
import com.health.companion.presentation.screens.canvas.LivingMapScreen
import com.health.companion.presentation.screens.chat.ChatScreen
import com.health.companion.presentation.screens.chat.ChatViewModel
import com.health.companion.presentation.screens.documents.DocumentsScreen
import com.health.companion.presentation.screens.profile.ProfileScreen
import com.health.companion.presentation.screens.profile.QuestionnaireScreen
import com.health.companion.presentation.screens.profile.ImportantDatesScreen
import com.health.companion.presentation.screens.profile.ImportantPeopleScreen
import com.health.companion.presentation.screens.settings.AppearanceScreen
import com.health.companion.presentation.screens.settings.SettingsDetailScreen
import com.health.companion.presentation.screens.settings.SettingsScreen
import com.health.companion.presentation.screens.skills.SkillsScreen
import com.health.companion.presentation.screens.games.GamesScreen
import com.health.companion.presentation.screens.marketplace.MarketplaceScreen

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
    object Questionnaire : Route("profile/questionnaire")
    object ImportantDates : Route("profile/dates")
    object ImportantPeople : Route("profile/people")
    
    // Skills
    object Skills : Route("skills")

    // Marketplace
    object Marketplace : Route("marketplace")

    // Games
    object Games : Route("games")

    // Settings Detail (appearance + voice)
    object SettingsDetail : Route("settings-detail")

    // Appearance
    object Appearance : Route("appearance")

    // Change Password
    object ChangePassword : Route("change-password")
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val labelRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(Route.Dashboard.route, Icons.Outlined.Dashboard, Icons.Default.Dashboard, R.string.nav_home),
    BottomNavItem(Route.Chat.route, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat, R.string.nav_chat_label),
    BottomNavItem(Route.Documents.route, Icons.Outlined.Description, Icons.Default.Description, R.string.nav_files),
    BottomNavItem(Route.Games.route, Icons.Default.SportsEsports, Icons.Default.SportsEsports, R.string.nav_games),
    BottomNavItem(Route.Settings.route, Icons.Outlined.Settings, Icons.Default.Settings, R.string.nav_more)
)

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Route.Auth.route, // Require login first
    showSplash: Boolean = false
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val appTheme = LocalAppTheme.current
    val chatBackground = LocalChatBackground.current

    // Determine if we should show bottom navigation
    val showBottomBar = currentDestination?.hierarchy?.any { destination ->
        bottomNavItems.any { it.route == destination.route }
    } == true

    // Canvas screen gets a dark cosmic tab bar that doesn't clash with the space background
    val isCanvasScreen = currentDestination?.hierarchy?.any {
        it.route == Route.Dashboard.route
    } == true

    // Sync system navigation bar color with current screen background.
    // Keep black while splash is visible so the nav bar blends with the dark animation.
    val view = LocalView.current
    if (!view.isInEditMode) {
        val navBarColor = if (showSplash || isCanvasScreen)
            android.graphics.Color.BLACK
        else
            chatBackground.bottomColor.toArgb()
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = navBarColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                // Bottom Navigation Bar — dark on canvas, themed on other screens
                val tabBg = if (isCanvasScreen)
                    Brush.verticalGradient(listOf(Color(0xFF0C1520).copy(alpha = 0.97f), Color(0xFF060D14).copy(alpha = 0.99f)))
                else
                    Brush.verticalGradient(listOf(chatBackground.bottomColor.copy(alpha = 0.97f), chatBackground.bottomColor.copy(alpha = 0.99f)))

                val tabBorder = if (isCanvasScreen)
                    Brush.horizontalGradient(listOf(Color(0xFF1E3050).copy(alpha = 0.70f), Color(0xFF0E1E30).copy(alpha = 0.50f)))
                else
                    Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f)))

                val selectedTint  = if (isCanvasScreen) Color(0xFF8BAFD4) else appTheme.primary
                val selectedBg    = if (isCanvasScreen) Color(0xFF1A3050).copy(alpha = 0.60f) else appTheme.primary.copy(alpha = 0.18f)
                val unselectedTint = if (isCanvasScreen) Color(0xFF3A4E62) else Color.White.copy(alpha = 0.40f)

                val outerBg = if (isCanvasScreen) Color(0xFF060D14) else chatBackground.bottomColor

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(outerBg)
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tabBg)
                        .border(0.5.dp, tabBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEach { item ->
                            val isSelected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .then(if (isSelected) Modifier.background(selectedBg) else Modifier)
                                    .clickable {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    val label = stringResource(item.labelRes)
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                                        contentDescription = label,
                                        tint = if (isSelected) selectedTint else unselectedTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) selectedTint else unselectedTint,
                                        maxLines = 1,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier,
            enterTransition = {
                fadeIn(tween(280, easing = EaseOutCubic)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(280, easing = EaseOutCubic))
            },
            exitTransition = {
                fadeOut(tween(200, easing = EaseInCubic)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(200, easing = EaseInCubic))
            },
            popEnterTransition = {
                fadeIn(tween(280, easing = EaseOutCubic)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(280, easing = EaseOutCubic))
            },
            popExitTransition = {
                fadeOut(tween(200, easing = EaseInCubic)) +
                scaleOut(targetScale = 0.96f, animationSpec = tween(200, easing = EaseInCubic))
            }
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
                startDestination = Route.Chat.route // Chat — основной экран
            ) {
                composable(Route.Dashboard.route) { backStackEntry ->
                    val mainEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Route.Main.route)
                    }
                    val chatViewModel: ChatViewModel = hiltViewModel(mainEntry)
                    val canvasViewModel: CanvasViewModel = hiltViewModel()
                    LivingMapScreen(
                        chatViewModel = chatViewModel,
                        canvasViewModel = canvasViewModel,
                        onNodeAskAgent = { node ->
                            navController.navigate(Route.Chat.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        bottomPadding = paddingValues.calculateBottomPadding()
                    )
                }

                composable(Route.Chat.route) { backStackEntry ->
                    val mainEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Route.Main.route)
                    }
                    ChatScreen(
                        viewModel = hiltViewModel(mainEntry),
                        bottomBarPadding = paddingValues,
                        onMessageSent = {},
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

                composable(Route.Games.route) { backStackEntry ->
                    val mainEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Route.Main.route)
                    }
                    GamesScreen(
                        viewModel = hiltViewModel(mainEntry),
                        bottomPadding = paddingValues.calculateBottomPadding(),
                        onBack = {
                            navController.navigate(Route.Chat.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }

                composable(Route.Documents.route) {
                    DocumentsScreen(
                        bottomPadding = paddingValues.calculateBottomPadding()
                    )
                }
                
                composable(Route.Settings.route) {
                    SettingsScreen(
                        onOpenProfile = {
                            navController.navigate(Route.Profile.route)
                        },
                        onOpenSkills = {
                            navController.navigate(Route.Skills.route)
                        },
                        onOpenSettingsDetail = {
                            navController.navigate(Route.SettingsDetail.route)
                        },
                        bottomPadding = paddingValues.calculateBottomPadding()
                    )
                }

                composable(
                    Route.SettingsDetail.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    SettingsDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAppearance = { navController.navigate(Route.Appearance.route) }
                    )
                }

                composable(
                    Route.Appearance.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    AppearanceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    Route.ChangePassword.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    ChangePasswordScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(
                    Route.Skills.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    SkillsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenMarketplace = { navController.navigate(Route.Marketplace.route) }
                    )
                }
                
                composable(
                    Route.Marketplace.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    MarketplaceScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    Route.Profile.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    ProfileScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            navController.navigate(Route.Auth.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onChangePassword = {
                            navController.navigate(Route.ChangePassword.route)
                        }
                    )
                }
                
                composable(
                    Route.Questionnaire.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    QuestionnaireScreen(
                        onComplete = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(
                    Route.ImportantDates.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    ImportantDatesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(
                    Route.ImportantPeople.route,
                    enterTransition = { slideInHorizontally(tween(300, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(300, easing = EaseOutCubic)) },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(250)) },
                    popExitTransition = { slideOutHorizontally(tween(280, easing = EaseInCubic)) { it / 3 } + fadeOut(tween(250)) }
                ) {
                    ImportantPeopleScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
            }
        }
    }
}
