package com.salco.dragonfly.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.AmountText
import com.salco.dragonfly.core.designsystem.component.ChartPalette
import com.salco.dragonfly.core.designsystem.component.DonutChart
import com.salco.dragonfly.core.designsystem.component.DonutSlice
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.designsystem.component.GroupedBarChart
import com.salco.dragonfly.core.designsystem.component.LineChart
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Destinations the dashboard links to. */
internal data class HomeActions(
    val onAddAccount: () -> Unit = {},
    val onAccounts: () -> Unit = {},
    val onTransactions: () -> Unit = {},
    val onBudgets: () -> Unit = {},
    val onBills: () -> Unit = {},
    val onPayBill: (Long) -> Unit = {},
    val onPiggyBanks: () -> Unit = {},
)

@Composable
internal fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(uiState: HomeUiState, actions: HomeActions) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_home_title)) }) }) { padding ->
        val state = uiState as? HomeUiState.Success
        when {
            state == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            !state.hasAccounts -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                EmptyState(
                    icon = Icons.Filled.AccountBalance,
                    title = stringResource(R.string.feature_home_welcome_title),
                    message = stringResource(R.string.feature_home_welcome_message),
                    modifier = Modifier.weight(1f, fill = false),
                )
                Button(onClick = actions.onAddAccount) { Text(stringResource(R.string.feature_home_add_account)) }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { NetWorthCard(state, actions.onAccounts) }
                item { CashFlowCard(state, actions.onTransactions) }
                item { CategoryCard(state) }
                if (state.budgets.isNotEmpty()) item { BudgetsCard(state, actions.onBudgets) }
                item { BillsCard(state.bills, actions) }
                if (state.piggyBanks.isNotEmpty()) item { PiggyBanksCard(state, actions.onPiggyBanks) }
            }
        }
    }
}

@Composable
private fun NetWorthCard(state: HomeUiState.Success, onClick: () -> Unit) {
    val currency = state.currencyCode
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_home_net_worth), style = MaterialTheme.typography.labelLarge)
            val main = state.netWorth[currency] ?: 0
            AmountText(formatMoney(main, currency), negative = main < 0, style = MaterialTheme.typography.headlineMedium)
            val others = state.netWorth.filterKeys { it != currency }
            if (others.isNotEmpty()) {
                Text(
                    stringResource(
                        R.string.feature_home_other_currencies,
                        currency,
                        others.entries.joinToString { (c, v) -> formatMoney(v, c) },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val history = state.netWorthHistory
            if (history.size > 1) {
                LineChart(
                    values = history.map { it.amount.toFloat() },
                    labels = history.mapIndexed { i, p -> if (i % 3 == 2 || i == history.lastIndex) p.month.shortLabel() else "" },
                    description = stringResource(
                        R.string.feature_home_net_worth_chart,
                        formatMoney(history.first().amount, currency),
                        formatMoney(history.last().amount, currency),
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CashFlowCard(state: HomeUiState.Success, onClick: () -> Unit) {
    val currency = state.currencyCode
    val month = state.thisMonth
    SectionCard(stringResource(R.string.feature_home_this_month), onSeeAll = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Figure(stringResource(R.string.feature_home_income), formatMoney(month.income, currency), IncomeColor)
            Figure(stringResource(R.string.feature_home_expenses), formatMoney(month.expenses, currency), MaterialTheme.colorScheme.error)
            Figure(
                stringResource(R.string.feature_home_balance),
                formatMoney(month.net, currency),
                if (month.net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        GroupedBarChart(
            first = state.cashFlow.map { it.income.toFloat() },
            second = state.cashFlow.map { it.expenses.toFloat() },
            labels = state.cashFlow.map { it.month.shortLabel() },
            firstColor = IncomeColor,
            secondColor = MaterialTheme.colorScheme.error,
            description = stringResource(R.string.feature_home_cash_flow_chart) + ": " +
                state.cashFlow.joinToString { "${it.month.shortLabel()} +${formatMoney(it.income, currency)} −${formatMoney(it.expenses, currency)}" },
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun CategoryCard(state: HomeUiState.Success) {
    val currency = state.currencyCode
    SectionCard(stringResource(R.string.feature_home_spending_by_category)) {
        if (state.categorySpending.isEmpty()) {
            Text(stringResource(R.string.feature_home_no_spending), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@SectionCard
        }
        val uncategorized = stringResource(R.string.feature_home_uncategorized)
        val other = stringResource(R.string.feature_home_other_categories)
        // Five largest categories, the rest grouped together.
        val top = state.categorySpending.take(5)
        val rest = state.categorySpending.drop(5).sumOf { it.amount }
        val slices = top.mapIndexed { i, c ->
            DonutSlice(c.categoryName ?: uncategorized, c.amount.toFloat(), ChartPalette[i], formatMoney(c.amount, currency))
        } + listOfNotNull(rest.takeIf { it > 0 }?.let { DonutSlice(other, it.toFloat(), ChartPalette.last(), formatMoney(it, currency)) })
        DonutChart(
            slices = slices,
            centerText = formatMoney(state.categorySpending.sumOf { it.amount }, currency),
            description = stringResource(
                R.string.feature_home_spending_chart,
                slices.joinToString { "${it.label} ${it.valueLabel}" },
            ),
        )
    }
}

@Composable
private fun BudgetsCard(state: HomeUiState.Success, onSeeAll: () -> Unit) {
    SectionCard(stringResource(R.string.feature_home_budgets), onSeeAll = onSeeAll) {
        state.budgets.forEach { spending ->
            val currency = spending.budget.currencyCode
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(spending.budget.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.feature_home_spent_of, formatMoney(spending.spent, currency), formatMoney(spending.budget.amount, currency)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spending.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { spending.progress.coerceIn(0f, 1f) },
                    color = if (spending.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun BillsCard(bills: List<Pair<Bill, BillStatus>>, actions: HomeActions) {
    SectionCard(stringResource(R.string.feature_home_bills), onSeeAll = actions.onBills) {
        if (bills.isEmpty()) {
            Text(stringResource(R.string.feature_home_no_bills), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        bills.forEach { (bill, status) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(bill.name, style = MaterialTheme.typography.bodyMedium)
                    val (text, color) = when (status) {
                        is BillStatus.Overdue -> stringResource(R.string.feature_home_overdue, status.dueDate.medium()) to MaterialTheme.colorScheme.error
                        is BillStatus.Due -> stringResource(R.string.feature_home_due, status.dueDate.medium()) to MaterialTheme.colorScheme.tertiary
                        else -> "" to MaterialTheme.colorScheme.onSurface
                    }
                    Text("${formatMoney(bill.amountMax, bill.currencyCode)} · $text", style = MaterialTheme.typography.bodySmall, color = color)
                }
                FilledTonalButton(onClick = { actions.onPayBill(bill.id) }) { Text(stringResource(R.string.feature_home_pay)) }
            }
        }
    }
}

@Composable
private fun PiggyBanksCard(state: HomeUiState.Success, onSeeAll: () -> Unit) {
    SectionCard(stringResource(R.string.feature_home_piggy_banks), onSeeAll = onSeeAll) {
        state.piggyBanks.sortedByDescending { it.progress ?: 0f }.take(3).forEach { piggy ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(piggy.piggyBank.name, style = MaterialTheme.typography.bodyMedium)
                    val target = piggy.piggyBank.targetAmount
                    Text(
                        if (target != null) {
                            stringResource(R.string.feature_home_saved_of, formatMoney(piggy.saved, piggy.currencyCode), formatMoney(target, piggy.currencyCode))
                        } else {
                            stringResource(R.string.feature_home_saved, formatMoney(piggy.saved, piggy.currencyCode))
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                piggy.progress?.let { progress ->
                    LinearProgressIndicator(progress = { progress }, color = IncomeColor, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, onSeeAll: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (onSeeAll != null) {
                    TextButton(onClick = onSeeAll) { Text(stringResource(R.string.feature_home_see_all)) }
                }
            }
            content()
        }
    }
}

@Composable
private fun Figure(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun YearMonth.shortLabel(): String = format(DateTimeFormatter.ofPattern("LLL")).replaceFirstChar { it.uppercase() }

private fun java.time.LocalDate.medium(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
