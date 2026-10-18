package com.salco.dragonfly.feature.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.transactions.edit.TransactionEditScreen
import com.salco.dragonfly.feature.transactions.list.TransactionsScreen
import com.salco.dragonfly.feature.transactions.recurring.RecurringEditScreen
import com.salco.dragonfly.feature.transactions.recurring.RecurringListScreen
import kotlinx.serialization.Serializable

@Serializable
data object TransactionsRoute

/**
 * [transactionId] is 0 when creating a new transaction; a new one can be prefilled with
 * [accountId], either an own account or a payee/payer.
 */
@Serializable
data class TransactionEditRoute(val transactionId: Long = 0L, val accountId: Long = 0L)

@Serializable
data object RecurringTransactionsRoute

/** [recurringId] is 0 when creating a new recurring transaction. */
@Serializable
data class RecurringEditRoute(val recurringId: Long = 0L)

fun NavController.navigateToRecurringTransactions() = navigate(RecurringTransactionsRoute)

fun NavController.navigateToRecurringEdit(recurringId: Long = 0L) = navigate(RecurringEditRoute(recurringId))

fun NavGraphBuilder.recurringTransactionsScreens(
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    composable<RecurringTransactionsRoute> { RecurringListScreen(onBack = onBack, onItemClick = onItemClick, onAdd = onAdd) }
    composable<RecurringEditRoute> { RecurringEditScreen(onBack = onBack) }
}

fun NavController.navigateToTransactions(navOptions: NavOptions? = null) =
    navigate(route = TransactionsRoute, navOptions = navOptions)

fun NavController.navigateToTransactionEdit(transactionId: Long = 0L) =
    navigate(route = TransactionEditRoute(transactionId))

fun NavController.navigateToNewTransaction(accountId: Long) =
    navigate(route = TransactionEditRoute(accountId = accountId))

fun NavGraphBuilder.transactionsScreen(
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
    onRecurringClick: () -> Unit,
) {
    composable<TransactionsRoute> {
        TransactionsScreen(
            onTransactionClick = onTransactionClick,
            onAddTransactionClick = onAddTransactionClick,
            onRecurringClick = onRecurringClick,
        )
    }
}

fun NavGraphBuilder.transactionEditScreen(onBack: () -> Unit) {
    composable<TransactionEditRoute> {
        TransactionEditScreen(onBack = onBack)
    }
}
