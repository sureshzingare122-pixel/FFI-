package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.PracticeArenaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TrainerHubScreen
import com.example.ui.theme.FFITrainerProTheme
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonOrangeDim
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.AimReactionViewModel
import com.example.ui.viewmodel.FFIViewModelFactory
import com.example.ui.viewmodel.MovementTrainerViewModel
import com.example.ui.viewmodel.PracticeArenaViewModel
import com.example.ui.viewmodel.SensitivityCalibratorViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.StatisticsViewModel
import com.example.util.FeedbackEngine
import kotlinx.serialization.Serializable

@Serializable
object PracticeRoute

@Serializable
object TrainerRoute

@Serializable
object StatisticsRoute

@Serializable
object SettingsRoute

data class BottomTabItem<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FFITrainerApplication
        setContent {
            FFITrainerProTheme {
                FFITrainerRoot(
                    app = app,
                    showSplashInitially = true
                )
            }
        }
    }
}

@Composable
fun FFITrainerRoot(
    app: FFITrainerApplication,
    showSplashInitially: Boolean = true
) {
    var showSplash by rememberSaveable { mutableStateOf(showSplashInitially) }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "splash_transition"
    ) { splashVisible ->
        if (splashVisible) {
            SplashScreen(onFinished = { showSplash = false })
        } else {
            FFITrainerMainShell(
                factory = remember(app) { FFIViewModelFactory(app) },
                feedbackEngine = app.feedbackEngine
            )
        }
    }
}

@Composable
fun FFITrainerMainShell(
    factory: FFIViewModelFactory,
    feedbackEngine: FeedbackEngine
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val practiceViewModel: PracticeArenaViewModel = viewModel(factory = factory)
    val movementViewModel: MovementTrainerViewModel = viewModel(factory = factory)
    val aimReactionViewModel: AimReactionViewModel = viewModel(factory = factory)
    val calibratorViewModel: SensitivityCalibratorViewModel = viewModel(factory = factory)
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)

    val tabs = remember {
        listOf(
            BottomTabItem(
                route = PracticeRoute,
                label = "PRACTICE",
                selectedIcon = Icons.Filled.MyLocation,
                unselectedIcon = Icons.Outlined.MyLocation,
                testTag = "nav_tab_practice"
            ),
            BottomTabItem(
                route = TrainerRoute,
                label = "TRAINER",
                selectedIcon = Icons.Filled.Gamepad,
                unselectedIcon = Icons.Outlined.Gamepad,
                testTag = "nav_tab_trainer"
            ),
            BottomTabItem(
                route = StatisticsRoute,
                label = "STATISTICS",
                selectedIcon = Icons.Filled.BarChart,
                unselectedIcon = Icons.Outlined.BarChart,
                testTag = "nav_tab_statistics"
            ),
            BottomTabItem(
                route = SettingsRoute,
                label = "SETTINGS",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_tab_settings"
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MatteBlack,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = ObsidianPanel,
                tonalElevation = 4.dp
            ) {
                tabs.forEach { tab ->
                    val selected = currentDestination?.hasRoute(tab.route::class) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonOrange,
                            selectedTextColor = NeonOrange,
                            indicatorColor = NeonOrangeDim,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PracticeRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable<PracticeRoute> {
                PracticeArenaScreen(
                    viewModel = practiceViewModel,
                    feedbackEngine = feedbackEngine
                )
            }
            composable<TrainerRoute> {
                TrainerHubScreen(
                    movementViewModel = movementViewModel,
                    aimReactionViewModel = aimReactionViewModel,
                    calibratorViewModel = calibratorViewModel,
                    settingsViewModel = settingsViewModel,
                    feedbackEngine = feedbackEngine
                )
            }
            composable<StatisticsRoute> {
                StatisticsScreen(viewModel = statisticsViewModel)
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    feedbackEngine = feedbackEngine
                )
            }
        }
    }
}
