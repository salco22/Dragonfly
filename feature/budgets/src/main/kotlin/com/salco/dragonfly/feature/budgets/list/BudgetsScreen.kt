package com.salco.dragonfly.feature.budgets.list

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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.salco.dragonfly.core.designsystem.component.EmptyState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetsContent(
    uiState: BudgetsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBudgetClick: (Long, YearMonth) -> Unit,
    onAddBudget: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_budgets_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBudget,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_budgets_add)) },
            )
        },
    ) { padding ->
        val state = uiState as? BudgetsUiState.Success
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Filled.PieChart,
                title = stringResource(R.string.feature_budgets_empty_title),
                message = stringResource(R.string.feature_budgets_empty_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
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
                    Column(Modifier.padding(top = 8.dp)) {
                        Text(stringResource(R.string.feature_budgets_unbudgeted), style = MaterialTheme.typography.titleSmall)
                        state.unbudgeted.forEach { (currency, amount) ->
                            Text(formatMoney(amount, currency), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (state.inactive.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_budgets_inactive),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(state.inactive, key = { it.budget.id }) { spending ->
                    BudgetCard(
                        spending,
                        daysLeft = null,
                        onClick = { onBudgetClick(spending.budget.id, state.month) },
                        modifier = Modifier.alpha(0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalsCard(currency: String, totals: BudgetTotals) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TotalColumn(stringResource(R.string.feature_budgets_budgeted), formatMoney(totals.budgeted, currency))
            TotalColumn(stringResource(R.string.feature_budgets_spent), formatMoney(totals.spent, currency))
            TotalColumn(
                stringResource(R.string.feature_budgets_left),
                formatMoney(totals.left, currency),
                negative = totals.left < 0,
            )
        }
    }
}

@Composable
private fun TotalColumn(label: String, value: String, negative: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun BudgetCard(spending: BudgetSpending, daysLeft: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val currency = spending.budget.currencyCode
    OutlinedCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(spending.budget.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.feature_budgets_spent_of,
                        formatMoney(spending.spent, currency),
                        formatMoney(spending.budget.amount, currency),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BudgetProgress(spending)
            Text(
                remainingText(spending, daysLeft),
                style = MaterialTheme.typography.bodySmall,
                color = if (spending.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
