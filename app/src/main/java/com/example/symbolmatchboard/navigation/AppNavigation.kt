package com.example.symbolmatchboard.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.symbolmatchboard.ui.AppViewModel
import com.example.symbolmatchboard.ui.SymbolMatchViewModel
import com.example.symbolmatchboard.ui.components.AppIcons
import com.example.symbolmatchboard.ui.screens.AboutScreen
import com.example.symbolmatchboard.ui.screens.GameScreen
import com.example.symbolmatchboard.ui.screens.HomeScreen
import com.example.symbolmatchboard.ui.screens.ResultScreen
import com.example.symbolmatchboard.ui.screens.SettingsScreen
import com.example.symbolmatchboard.ui.screens.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val RESULT = "result"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val BottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Home", AppIcons.Home),
    BottomDestination(Routes.SETTINGS, "Settings", AppIcons.Settings),
    BottomDestination(Routes.ABOUT, "About", AppIcons.Info)
)

/** Navigates only if [from] is still the current destination — blocks double taps / duplicate pushes. */
private fun NavController.navigateFrom(from: String, to: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    if (currentBackStackEntry?.destination?.route == from) navigate(to, builder)
}

private fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavigation(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val gameViewModel: SymbolMatchViewModel = viewModel()
    val settings by appViewModel.settings.collectAsStateWithLifecycle()
    val statistics by appViewModel.statistics.collectAsStateWithLifecycle()
    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BottomDestinations.any { it.route == currentRoute }

    fun startGame(from: String, popFrom: Boolean) {
        if (navController.currentBackStackEntry?.destination?.route != from) return
        gameViewModel.startNewGame()
        navController.navigate(Routes.GAME) {
            launchSingleTop = true
            if (popFrom) popUpTo(from) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { if (currentRoute != destination.route) navController.switchTab(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    navController.navigateFrom(Routes.SPLASH, Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    statistics = statistics,
                    settings = settings,
                    onPlay = { startGame(Routes.HOME, popFrom = false) },
                    onDifficultyChange = { difficulty -> appViewModel.updateSettings { it.copy(difficulty = difficulty) } },
                    onOpenSettings = { navController.switchTab(Routes.SETTINGS) },
                    onOpenAbout = { navController.switchTab(Routes.ABOUT) }
                )
            }
            composable(Routes.GAME) {
                GameScreen(
                    viewModel = gameViewModel,
                    settings = settings,
                    onExit = {
                        gameViewModel.abandonGame()
                        if (navController.currentBackStackEntry?.destination?.route == Routes.GAME) {
                            navController.popBackStack()
                        }
                    },
                    onShowResult = {
                        navController.navigateFrom(Routes.GAME, Routes.RESULT) {
                            popUpTo(Routes.GAME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.RESULT) {
                // Keep the result this screen opened with, even after "Play Again" resets the game.
                val shownResult = remember { gameState.result }
                ResultScreen(
                    result = shownResult,
                    animationsEnabled = settings.animations,
                    onPlayAgain = { startGame(Routes.RESULT, popFrom = true) },
                    onHome = {
                        if (navController.currentBackStackEntry?.destination?.route == Routes.RESULT) {
                            if (!navController.popBackStack(Routes.HOME, inclusive = false)) {
                                navController.navigate(Routes.HOME) { popUpTo(0) }
                            }
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onUpdate = appViewModel::updateSettings,
                    onResetStatistics = appViewModel::resetStatistics,
                    onResetSettings = appViewModel::resetSettings
                )
            }
            composable(Routes.ABOUT) { AboutScreen() }
        }
    }
}
