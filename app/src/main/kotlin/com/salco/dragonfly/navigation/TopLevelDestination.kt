package com.salco.dragonfly.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.salco.dragonfly.R
import com.salco.dragonfly.feature.accounts.navigation.AccountsRoute
import com.salco.dragonfly.feature.settings.navigation.SettingsRoute
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
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelRes = R.string.nav_settings,
        route = SettingsRoute::class,
    ),
}
