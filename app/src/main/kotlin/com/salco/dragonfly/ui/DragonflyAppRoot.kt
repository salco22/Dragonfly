package com.salco.dragonfly.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
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
import com.salco.dragonfly.core.designsystem.component.DfAppFrame
import com.salco.dragonfly.core.designsystem.component.DfNavItem
import com.salco.dragonfly.core.designsystem.component.DfNavigationBar
import com.salco.dragonfly.core.designsystem.component.DfSurface
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccounts
import com.salco.dragonfly.feature.budgets.navigation.navigateToBudgets
import com.salco.dragonfly.feature.home.navigation.navigateToHome
import com.salco.dragonfly.feature.lock.LockScreen
import com.salco.dragonfly.feature.settings.navigation.navigateToSettings
import com.salco.dragonfly.feature.sync.LoginScreen
import com.salco.dragonfly.feature.transactions.navigation.navigateToTransactions
import com.salco.dragonfly.navigation.DragonflyNavHost
import com.salco.dragonfly.navigation.TopLevelDestination

@Composable
fun DragonflyAppRoot(uiState: MainActivityUiState, onUnlocked: () -> Unit, onSkipLogin: () -> Unit) {
    // The nav controller lives above the lock so the back stack survives locking.
    val navController = rememberNavController()
    when (uiState) {
        MainActivityUiState.Loading -> DfSurface {}
        is MainActivityUiState.Ready -> Box {
            if (uiState.showLogin) LoginScreen(onSkip = onSkipLogin) else DragonflyApp(navController)
            if (uiState.locked) LockScreen(onUnlocked = onUnlocked)
        }
    }
}

@Composable
fun DragonflyApp(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isRoute(it) }

    DfAppFrame(
        bottomBar = {
            if (showBottomBar) {
                DfNavigationBar(
                    TopLevelDestination.entries.map { destination ->
                        DfNavItem(
                            label = stringResource(destination.labelRes),
                            icon = destination.unselectedIcon,
                            selectedIcon = destination.selectedIcon,
                            selected = currentDestination.isRoute(destination),
                            onClick = { navController.navigateToTopLevel(destination) },
                        )
                    },
                )
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

internal fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    val options = navOptions {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    when (destination) {
        TopLevelDestination.HOME -> navigateToHome(options)
        TopLevelDestination.ACCOUNTS -> navigateToAccounts(options)
        TopLevelDestination.TRANSACTIONS -> navigateToTransactions(options)
        TopLevelDestination.BUDGETS -> navigateToBudgets(options)
        TopLevelDestination.MORE -> navigateToSettings(options)
    }
}
