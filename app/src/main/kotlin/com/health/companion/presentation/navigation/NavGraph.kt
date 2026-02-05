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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import com.health.companion.presentation.components.GlassTheme
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
import com.health.companion.presentation.screens.profile.QuestionnaireScreen
import com.health.companion.presentation.screens.profile.ImportantDatesScreen
import com.health.companion.presentation.screens.profile.ImportantPeopleScreen
import com.health.companion.presentation.screens.settings.SettingsScreen
import com.health.companion.presentation.screens.wellness.WellnessScreen
import com.health.companion.presentation.screens.medical.MedicalAssistantScreen
import com.health.companion.presentation.screens.medical.SymptomCheckerScreen
import com.health.companion.presentation.screens.medical.DrugInteractionsScreen
import com.health.companion.presentation.screens.medical.LabResultsScreen
import com.health.companion.presentation.screens.medical.EmergencyScreen
import com.health.companion.presentation.screens.medical.RecommendationsScreen

sealed class Route(val route: String) {
    object Auth : Route("auth")
    object AuthLogin : Route("auth/login")
    object AuthRegister : Route("auth/register")

    object Main : Route("main")
    object Dashboard : Route("dashboard")
    object Chat : Route("chat")
    object Documents : Route("documents")
    object Wellness : Route("wellness")
    object Settings : Route("settings")
    object Profile : Route("profile")
    object Questionnaire : Route("profile/questionnaire")
    object ImportantDates : Route("profile/dates")
    object ImportantPeople : Route("profile/people")
    
    // Medical
    object Medical : Route("medical")
    object SymptomChecker : Route("medical/symptoms")
    object DrugInteractions : Route("medical/drugs")
    object LabResults : Route("medical/lab")
    object MedicalRecommendations : Route("medical/recommendations")
    object Emergency : Route("medical/emergency")
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

// Dashboard первая, добавлен Wellness
val bottomNavItems = listOf(
    BottomNavItem(Route.Dashboard.route, Icons.Outlined.Dashboard, Icons.Default.Dashboard, "Главная"),
    BottomNavItem(Route.Chat.route, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat, "Чат"),
    BottomNavItem(Route.Wellness.route, Icons.Default.SelfImprovement, Icons.Default.SelfImprovement, "Wellness"),
    BottomNavItem(Route.Documents.route, Icons.Outlined.Description, Icons.Default.Description, "Файлы"),
    BottomNavItem(Route.Settings.route, Icons.Outlined.Settings, Icons.Default.Settings, "Ещё")
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
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                // Glass Navigation Bar - матовое белёсое стекло
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF).copy(alpha = 0.12f),
                                    Color(0xFFE8EAF0).copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(28.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .then(
                                        if (isSelected) Modifier.background(
                                            GlassTheme.accentPrimary.copy(alpha = 0.2f)
                                        ) else Modifier
                                    )
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
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) 
                                            GlassTheme.accentPrimary 
                                        else 
                                            Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        item.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) 
                                            GlassTheme.accentPrimary 
                                        else 
                                            Color.White.copy(alpha = 0.45f),
                                        maxLines = 1,
                                        fontSize = 10.sp
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
                composable(Route.Dashboard.route) { backStackEntry ->
                    // DashboardViewModel scoped to Main graph for sharing
                    val mainEntry = remember(backStackEntry) {
                        navController.getBackStackEntry(Route.Main.route)
                    }
                        DashboardScreen(
                        viewModel = hiltViewModel(mainEntry),
                            onNavigate = { route ->
                                navController.navigate(route) {
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
                    // Get shared DashboardViewModel
                    val dashboardViewModel: com.health.companion.presentation.screens.dashboard.DashboardViewModel = hiltViewModel(mainEntry)
                    
                    ChatScreen(
                        viewModel = hiltViewModel(mainEntry),
                        bottomBarPadding = paddingValues,
                        onMessageSent = {
                            // Обновить Dashboard после отправки сообщения
                            dashboardViewModel.onMessageSent()
                        },
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

                composable(Route.Documents.route) {
                    DocumentsScreen(
                        bottomPadding = paddingValues.calculateBottomPadding()
                    )
                }
                
                composable(Route.Wellness.route) {
                    WellnessScreen()
                }

                composable(Route.Settings.route) {
                        SettingsScreen(
                            onNavigateToLogin = {
                                navController.navigate(Route.Auth.route) {
                                    popUpTo(Route.Main.route) { inclusive = true }
                                }
                            },
                            onOpenProfile = {
                                navController.navigate(Route.Profile.route)
                            },
                            onOpenMedical = {
                                navController.navigate(Route.Medical.route)
                            },
                            bottomPadding = paddingValues.calculateBottomPadding()
                        )
                }

                composable(Route.Profile.route) {
                    ProfileScreen(
                        onNavigateToQuestionnaire = {
                            navController.navigate(Route.Questionnaire.route)
                        },
                        onNavigateToDates = {
                            navController.navigate(Route.ImportantDates.route)
                        },
                        onNavigateToPeople = {
                            navController.navigate(Route.ImportantPeople.route)
                        }
                    )
                }
                
                composable(Route.Questionnaire.route) {
                    QuestionnaireScreen(
                        onComplete = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.ImportantDates.route) {
                    ImportantDatesScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.ImportantPeople.route) {
                    ImportantPeopleScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Medical screens
                composable(Route.Medical.route) {
                    MedicalAssistantScreen(
                        onNavigateToSymptoms = {
                            navController.navigate(Route.SymptomChecker.route)
                        },
                        onNavigateToDrugs = {
                            navController.navigate(Route.DrugInteractions.route)
                        },
                        onNavigateToLab = {
                            navController.navigate(Route.LabResults.route)
                        },
                        onNavigateToRecommendations = {
                            navController.navigate(Route.MedicalRecommendations.route)
                        },
                        onNavigateToEmergency = {
                            navController.navigate(Route.Emergency.route)
                        }
                    )
                }
                
                composable(Route.SymptomChecker.route) {
                    SymptomCheckerScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.DrugInteractions.route) {
                    DrugInteractionsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.LabResults.route) {
                    LabResultsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.MedicalRecommendations.route) {
                    RecommendationsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Route.Emergency.route) {
                    EmergencyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
