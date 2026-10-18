package com.salco.dragonfly.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.home.HomeActions
import com.salco.dragonfly.feature.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavController.navigateToHome(navOptions: NavOptions? = null) = navigate(HomeRoute, navOptions)

fun NavGraphBuilder.homeScreen(
    onAddAccount: () -> Unit,
    onAccounts: () -> Unit,
    onTransactions: () -> Unit,
    onBudgets: () -> Unit,
    onBills: () -> Unit,
    onPayBill: (Long) -> Unit,
    onPiggyBanks: () -> Unit,
    onReports: () -> Unit,
) {
    composable<HomeRoute> {
        HomeScreen(HomeActions(onAddAccount, onAccounts, onTransactions, onBudgets, onBills, onPayBill, onPiggyBanks, onReports))
    }
}
