package com.salco.dragonfly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.salco.dragonfly.feature.accounts.navigation.AccountsRoute
import com.salco.dragonfly.feature.accounts.navigation.accountDetailScreen
import com.salco.dragonfly.feature.accounts.navigation.accountEditScreen
import com.salco.dragonfly.feature.accounts.navigation.accountsScreen
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccountDetail
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccountEdit
import com.salco.dragonfly.feature.classification.navigation.classificationScreens
import com.salco.dragonfly.feature.classification.navigation.navigateToCategories
import com.salco.dragonfly.feature.classification.navigation.navigateToTags
import com.salco.dragonfly.feature.settings.navigation.settingsScreen
import com.salco.dragonfly.feature.transactions.navigation.navigateToNewTransaction
import com.salco.dragonfly.feature.transactions.navigation.navigateToTransactionEdit
import com.salco.dragonfly.feature.transactions.navigation.transactionEditScreen
import com.salco.dragonfly.feature.transactions.navigation.transactionsScreen

@Composable
fun DragonflyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AccountsRoute,
        modifier = modifier,
    ) {
        accountsScreen(
            onAccountClick = navController::navigateToAccountDetail,
            onAddAccountClick = { navController.navigateToAccountEdit() },
        )
        accountDetailScreen(
            onBack = navController::popBackStack,
            onEdit = navController::navigateToAccountEdit,
            onTransactionClick = navController::navigateToTransactionEdit,
            onAddTransaction = navController::navigateToNewTransaction,
        )
        accountEditScreen(onBack = navController::popBackStack)
        transactionsScreen(
            onTransactionClick = navController::navigateToTransactionEdit,
            onAddTransactionClick = { navController.navigateToTransactionEdit() },
        )
        transactionEditScreen(onBack = navController::popBackStack)
        settingsScreen(
            onCategoriesClick = navController::navigateToCategories,
            onTagsClick = navController::navigateToTags,
        )
        classificationScreens(onBack = navController::popBackStack)
    }
}
