package com.salco.dragonfly.feature.budgets.list

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfStat
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.budgets.BudgetProgress
import com.salco.dragonfly.feature.budgets.MonthSelector
import com.salco.dragonfly.feature.budgets.R
import com.salco.dragonfly.feature.budgets.remainingText
import java.time.YearMonth

@Composable
internal fun BudgetsScreen(
    onBudgetClick: (Long, YearMonth) -> Unit,
    onAddBudget: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsContent(uiState, viewModel::previousMonth, viewModel::nextMonth, onBudgetClick, onAddBudget)
}

@Composable
internal fun BudgetsContent(
    uiState: BudgetsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBudgetClick: (Long, YearMonth) -> Unit,
    onAddBudget: () -> Unit,
) {
    DfScaffold(
        title = stringResource(R.string.feature_budgets_title),
        floatingAction = { DfFab(stringResource(R.string.feature_budgets_add), Icons.Filled.Add, onAddBudget) },
    ) { padding ->
        val state = uiState as? BudgetsUiState.Success
        if (state == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        if (state.isEmpty) {
            DfEmptyState(
                icon = Icons.Outlined.PieChart,
                title = stringResource(R.string.feature_budgets_empty_title),
                message = stringResource(R.string.feature_budgets_empty_message),
                modifier = Modifier.padding(padding),
            )
            return@DfScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthSelector(state.month, onPreviousMonth, onNextMonth) }
            state.totals.forEach { (currency, totals) ->
                item(key = "totals_$currency") { TotalsCard(currency, totals) }
            }
            items(state.budgets, key = { it.budget.id }) { spending ->
                BudgetCard(spending, state.daysLeft, onClick = { onBudgetClick(spending.budget.id, state.month) })
            }
            if (state.unbudgeted.isNotEmpty()) {
                item {
                    DfCard {
                        DfText(stringResource(R.string.feature_budgets_unbudgeted), style = DfTheme.typography.label, tone = DfTone.Secondary)
                        state.unbudgeted.forEach { (currency, amount) -> DfAmount(formatMoney(amount, currency)) }
                    }
                }
            }
            if (state.inactive.isNotEmpty()) {
                item { DfText(stringResource(R.string.feature_budgets_inactive).uppercase(), style = DfTheme.typography.overline, tone = DfTone.Tertiary, modifier = Modifier.padding(top = 12.dp)) }
                items(state.inactive, key = { it.budget.id }) { spending ->
                    BudgetCard(spending, daysLeft = null, onClick = { onBudgetClick(spending.budget.id, state.month) }, modifier = Modifier.alpha(0.55f))
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(currency: String, totals: BudgetTotals) {
    DfCard(highlighted = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DfStat(stringResource(R.string.feature_budgets_budgeted), formatMoney(totals.budgeted, currency))
            DfStat(stringResource(R.string.feature_budgets_spent), formatMoney(totals.spent, currency))
            DfStat(
                stringResource(R.string.feature_budgets_left),
                formatMoney(totals.left, currency),
                tone = if (totals.left < 0) DfTone.Negative else DfTone.Primary,
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun BudgetCard(spending: BudgetSpending, daysLeft: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val currency = spending.budget.currencyCode
    DfCard(modifier = modifier, onClick = onClick, spacing = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DfText(spending.budget.name, style = DfTheme.typography.subtitle, modifier = Modifier.weight(1f))
            DfText(
                stringResource(R.string.feature_budgets_spent_of, formatMoney(spending.spent, currency), formatMoney(spending.budget.amount, currency)),
                style = DfTheme.typography.bodySmall,
                tone = DfTone.Secondary,
            )
        }
        BudgetProgress(spending)
        DfText(remainingText(spending, daysLeft), style = DfTheme.typography.caption, tone = if (spending.isOverspent) DfTone.Negative else DfTone.Secondary)
    }
}

@Preview
@Composable
private fun BudgetsContentPreview() {
    val groceries = BudgetSpending(Budget(1, "Spesa", 40_000, "EUR"), spent = 25_000)
    val fun_ = BudgetSpending(Budget(2, "Svago", 10_000, "EUR"), spent = 12_000)
    DragonflyTheme {
        BudgetsContent(
            uiState = BudgetsUiState.Success(
                month = YearMonth.of(2026, 10),
                budgets = listOf(groceries, fun_),
                inactive = emptyList(),
                totals = mapOf("EUR" to BudgetTotals(budgeted = 50_000, spent = 37_000)),
                unbudgeted = mapOf("EUR" to 4_250),
                daysLeft = 14,
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onBudgetClick = { _, _ -> },
            onAddBudget = {},
        )
    }
}
