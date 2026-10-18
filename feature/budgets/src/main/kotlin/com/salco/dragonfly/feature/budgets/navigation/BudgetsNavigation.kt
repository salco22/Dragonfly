package com.salco.dragonfly.feature.budgets.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.budgets.detail.BudgetDetailScreen
import com.salco.dragonfly.feature.budgets.edit.BudgetEditScreen
import com.salco.dragonfly.feature.budgets.list.BudgetsScreen
import kotlinx.serialization.Serializable
import java.time.YearMonth

@Serializable
data object BudgetsRoute

/** [month] is an ISO year-month such as "2026-10". */
@Serializable
data class BudgetDetailRoute(val budgetId: Long, val month: String)

/** [budgetId] is 0 when creating a new budget. */
@Serializable
data class BudgetEditRoute(val budgetId: Long = 0L)

fun NavController.navigateToBudgets(navOptions: NavOptions? = null) = navigate(BudgetsRoute, navOptions)

fun NavController.navigateToBudgetDetail(budgetId: Long, month: YearMonth) =
    navigate(BudgetDetailRoute(budgetId, month.toString()))

fun NavController.navigateToBudgetEdit(budgetId: Long = 0L) = navigate(BudgetEditRoute(budgetId))

fun NavGraphBuilder.budgetsScreens(
    onBack: () -> Unit,
    onBudgetClick: (Long, YearMonth) -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    composable<BudgetsRoute> { BudgetsScreen(onBudgetClick = onBudgetClick, onAddBudget = onAddBudget) }
    composable<BudgetDetailRoute> {
        BudgetDetailScreen(onBack = onBack, onEdit = onEditBudget, onTransactionClick = onTransactionClick)
    }
    composable<BudgetEditRoute> { BudgetEditScreen(onBack = onBack) }
}
