package com.salco.dragonfly.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.salco.dragonfly.MainActivityUiState
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccounts
import com.salco.dragonfly.feature.budgets.navigation.navigateToBudgets
import com.salco.dragonfly.feature.lock.LockScreen
import com.salco.dragonfly.feature.settings.navigation.navigateToSettings
import com.salco.dragonfly.feature.transactions.navigation.navigateToTransactions
import com.salco.dragonfly.navigation.DragonflyNavHost
import com.salco.dragonfly.navigation.TopLevelDestination

@Composable
fun DragonflyAppRoot(uiState: MainActivityUiState, onUnlocked: () -> Unit) {
    // The nav controller lives above the lock so the back stack survives locking.
    val navController = rememberNavController()
    when (uiState) {
        MainActivityUiState.Loading -> Surface(Modifier.fillMaxSize()) {}
        is MainActivityUiState.Ready -> Box {
            DragonflyApp(navController)
            if (uiState.locked) LockScreen(onUnlocked = onUnlocked)
        }
    }
}

@Composable
fun DragonflyApp(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isRoute(it) }

    Scaffold(
        // Insets are left to the feature screens' own Scaffolds.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination.isRoute(destination)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTopLevel(destination) },
                            icon = {
                                Icon(
                                    if (selected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        DragonflyNavHost(
            navController = navController,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}

private fun NavDestination?.isRoute(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    val options = navOptions {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (destination) {
        TopLevelDestination.ACCOUNTS -> navigateToAccounts(options)
        TopLevelDestination.TRANSACTIONS -> navigateToTransactions(options)
        TopLevelDestination.BUDGETS -> navigateToBudgets(options)
        TopLevelDestination.SETTINGS -> navigateToSettings(options)
    }
}
