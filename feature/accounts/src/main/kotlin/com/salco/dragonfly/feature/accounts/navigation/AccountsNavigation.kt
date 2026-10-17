package com.salco.dragonfly.feature.accounts.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.accounts.edit.AccountEditScreen
import com.salco.dragonfly.feature.accounts.list.AccountsScreen
import kotlinx.serialization.Serializable

@Serializable
data object AccountsRoute

/** [accountId] is 0 when creating a new account. */
@Serializable
data class AccountEditRoute(val accountId: Long = 0L)

fun NavController.navigateToAccounts(navOptions: NavOptions? = null) =
    navigate(route = AccountsRoute, navOptions = navOptions)

fun NavController.navigateToAccountEdit(accountId: Long = 0L) =
    navigate(route = AccountEditRoute(accountId))

fun NavGraphBuilder.accountsScreen(
    onAccountClick: (Long) -> Unit,
    onAddAccountClick: () -> Unit,
) {
    composable<AccountsRoute> {
        AccountsScreen(onAccountClick = onAccountClick, onAddAccountClick = onAddAccountClick)
    }
}

fun NavGraphBuilder.accountEditScreen(onBack: () -> Unit) {
    composable<AccountEditRoute> {
        AccountEditScreen(onBack = onBack)
    }
}
