package com.salco.dragonfly.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.salco.dragonfly.R
import com.salco.dragonfly.feature.accounts.navigation.AccountsRoute
import com.salco.dragonfly.feature.bills.navigation.BillsRoute
import com.salco.dragonfly.feature.budgets.navigation.BudgetsRoute
import com.salco.dragonfly.feature.settings.navigation.SettingsRoute
import com.salco.dragonfly.feature.transactions.navigation.TransactionsRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
    val route: KClass<*>,
) {
    ACCOUNTS(
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        labelRes = R.string.nav_accounts,
        route = AccountsRoute::class,
    ),
    TRANSACTIONS(
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        labelRes = R.string.nav_transactions,
        route = TransactionsRoute::class,
    ),
    BUDGETS(
        selectedIcon = Icons.Filled.PieChart,
        unselectedIcon = Icons.Outlined.PieChart,
        labelRes = R.string.nav_budgets,
        route = BudgetsRoute::class,
    ),
    BILLS(
        selectedIcon = Icons.AutoMirrored.Filled.EventNote,
        unselectedIcon = Icons.AutoMirrored.Outlined.EventNote,
        labelRes = R.string.nav_bills,
        route = BillsRoute::class,
    ),
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.nav_settings,
        route = SettingsRoute::class,
    ),
}
