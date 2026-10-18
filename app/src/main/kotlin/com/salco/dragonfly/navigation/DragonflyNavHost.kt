package com.salco.dragonfly.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.salco.dragonfly.feature.accounts.navigation.accountDetailScreen
import com.salco.dragonfly.feature.accounts.navigation.accountEditScreen
import com.salco.dragonfly.feature.accounts.navigation.accountsScreen
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccountDetail
import com.salco.dragonfly.feature.accounts.navigation.navigateToAccountEdit
import com.salco.dragonfly.feature.backup.navigation.backupScreen
import com.salco.dragonfly.feature.backup.navigation.navigateToBackup
import com.salco.dragonfly.feature.bills.navigation.billsScreens
import com.salco.dragonfly.feature.bills.navigation.navigateToBills
import com.salco.dragonfly.feature.bills.navigation.navigateToBillDetail
import com.salco.dragonfly.feature.bills.navigation.navigateToBillEdit
import com.salco.dragonfly.feature.budgets.navigation.budgetsScreens
import com.salco.dragonfly.feature.budgets.navigation.navigateToBudgetDetail
import com.salco.dragonfly.feature.budgets.navigation.navigateToBudgetEdit
import com.salco.dragonfly.feature.home.navigation.HomeRoute
import com.salco.dragonfly.feature.home.navigation.homeScreen
import com.salco.dragonfly.ui.navigateToTopLevel
import com.salco.dragonfly.feature.csvimport.navigation.csvImportScreen
import com.salco.dragonfly.feature.csvimport.navigation.navigateToCsvImport
import com.salco.dragonfly.feature.classification.navigation.classificationScreens
import com.salco.dragonfly.feature.classification.navigation.navigateToCategories
import com.salco.dragonfly.feature.classification.navigation.navigateToTags
import com.salco.dragonfly.feature.piggybanks.navigation.navigateToPiggyBankDetail
import com.salco.dragonfly.feature.piggybanks.navigation.navigateToPiggyBankEdit
import com.salco.dragonfly.feature.piggybanks.navigation.navigateToPiggyBanks
import com.salco.dragonfly.feature.piggybanks.navigation.piggyBanksScreens
import com.salco.dragonfly.feature.rules.navigation.navigateToRuleEdit
import com.salco.dragonfly.feature.rules.navigation.navigateToRules
import com.salco.dragonfly.feature.rules.navigation.rulesScreens
import com.salco.dragonfly.feature.settings.navigation.settingsScreen
import com.salco.dragonfly.feature.transactions.navigation.navigateToBillPayment
import com.salco.dragonfly.feature.transactions.navigation.navigateToNewTransaction
import com.salco.dragonfly.feature.transactions.navigation.navigateToRecurringEdit
import com.salco.dragonfly.feature.transactions.navigation.navigateToRecurringTransactions
import com.salco.dragonfly.feature.transactions.navigation.recurringTransactionsScreens
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
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        homeScreen(
            onAddAccount = { navController.navigateToAccountEdit() },
            onAccounts = { navController.navigateToTopLevel(TopLevelDestination.ACCOUNTS) },
            onTransactions = { navController.navigateToTopLevel(TopLevelDestination.TRANSACTIONS) },
            onBudgets = { navController.navigateToTopLevel(TopLevelDestination.BUDGETS) },
            onBills = navController::navigateToBills,
            onPayBill = navController::navigateToBillPayment,
            onPiggyBanks = navController::navigateToPiggyBanks,
        )
        accountsScreen(
            onAccountClick = navController::navigateToAccountDetail,
            onAddAccountClick = { navController.navigateToAccountEdit() },
            onPiggyBanksClick = navController::navigateToPiggyBanks,
        )
        piggyBanksScreens(
            onBack = navController::popBackStack,
            onPiggyBankClick = navController::navigateToPiggyBankDetail,
            onAdd = { navController.navigateToPiggyBankEdit() },
            onEdit = navController::navigateToPiggyBankEdit,
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
            onRecurringClick = navController::navigateToRecurringTransactions,
        )
        recurringTransactionsScreens(
            onBack = navController::popBackStack,
            onItemClick = navController::navigateToRecurringEdit,
            onAdd = { navController.navigateToRecurringEdit() },
        )
        transactionEditScreen(onBack = navController::popBackStack)
        budgetsScreens(
            onBack = navController::popBackStack,
            onBudgetClick = navController::navigateToBudgetDetail,
            onAddBudget = { navController.navigateToBudgetEdit() },
            onEditBudget = navController::navigateToBudgetEdit,
            onTransactionClick = navController::navigateToTransactionEdit,
        )
        billsScreens(
            onBack = navController::popBackStack,
            onBillClick = navController::navigateToBillDetail,
            onAddBill = { navController.navigateToBillEdit() },
            onEditBill = navController::navigateToBillEdit,
            onPayBill = navController::navigateToBillPayment,
            onTransactionClick = navController::navigateToTransactionEdit,
        )
        settingsScreen(
            onCategoriesClick = navController::navigateToCategories,
            onTagsClick = navController::navigateToTags,
            onBillsClick = navController::navigateToBills,
            onPiggyBanksClick = navController::navigateToPiggyBanks,
            onRecurringClick = navController::navigateToRecurringTransactions,
            onRulesClick = navController::navigateToRules,
            onBackupClick = navController::navigateToBackup,
        )
        backupScreen(onBack = navController::popBackStack, onImportCsv = navController::navigateToCsvImport)
        csvImportScreen(onBack = navController::popBackStack)
        rulesScreens(
            onBack = navController::popBackStack,
            onRuleClick = navController::navigateToRuleEdit,
            onAdd = { navController.navigateToRuleEdit() },
        )
        classificationScreens(onBack = navController::popBackStack)
    }
}
