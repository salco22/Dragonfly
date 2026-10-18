package com.salco.dragonfly.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.salco.dragonfly.R
import com.salco.dragonfly.feature.accounts.navigation.AccountsRoute
import com.salco.dragonfly.feature.budgets.navigation.BudgetsRoute
import com.salco.dragonfly.feature.home.navigation.HomeRoute
import com.salco.dragonfly.feature.settings.navigation.SettingsRoute
import com.salco.dragonfly.feature.transactions.navigation.TransactionsRoute
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val labelRes: Int,
    val route: KClass<*>,
) {
    HOME(Icons.Filled.Home, Icons.Outlined.Home, R.string.nav_home, HomeRoute::class),
    ACCOUNTS(Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, R.string.nav_accounts, AccountsRoute::class),
    TRANSACTIONS(Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, R.string.nav_transactions, TransactionsRoute::class),
    BUDGETS(Icons.Filled.PieChart, Icons.Outlined.PieChart, R.string.nav_budgets, BudgetsRoute::class),

    /** Settings plus the less frequent sections: bills, piggy banks, recurring, categories, tags. */
    MORE(Icons.Filled.Menu, Icons.Outlined.Menu, R.string.nav_more, SettingsRoute::class),
}
