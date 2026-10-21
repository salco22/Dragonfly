package com.salco.dragonfly.feature.home

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfHeroCard
import com.salco.dragonfly.core.designsystem.component.DfHeroNote
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfProgressBar
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfStat
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.LineChart
import com.salco.dragonfly.core.designsystem.component.GroupedBarChart
import com.salco.dragonfly.core.designsystem.component.DonutChart
import com.salco.dragonfly.core.designsystem.component.DonutSlice
import com.salco.dragonfly.core.designsystem.component.ChartPalette
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.formatMoney
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
    val onReports: () -> Unit = {},
)

@Composable
internal fun HomeScreen(actions: HomeActions, viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState, actions)
}

@Composable
internal fun HomeContent(uiState: HomeUiState, actions: HomeActions) {
    DfScaffold(
        title = stringResource(R.string.feature_home_title),
        subtitle = stringResource(R.string.feature_home_subtitle),
        actions = { DfIconButton(Icons.Outlined.BarChart, stringResource(R.string.feature_home_reports), actions.onReports) },
    ) { padding ->
        val state = uiState as? HomeUiState.Success
        when {
            state == null -> DfLoading(Modifier.padding(padding))
            !state.hasAccounts -> DfEmptyState(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(R.string.feature_home_welcome_title),
                message = stringResource(R.string.feature_home_welcome_message),
                actionText = stringResource(R.string.feature_home_add_account),
                onAction = actions.onAddAccount,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
    val main = state.netWorth[currency] ?: 0
    DfHeroCard(stringResource(R.string.feature_home_net_worth), formatMoney(main, currency), onClick = onClick) {
        val others = state.netWorth.filterKeys { it != currency }
        if (others.isNotEmpty()) {
            DfHeroNote(
                stringResource(
                    R.string.feature_home_other_currencies,
                    currency,
                    others.entries.joinToString { (c, v) -> formatMoney(v, c) },
                ),
                highlighted = true,
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
                onHero = true,
            )
        }
    }
}

@Composable
private fun CashFlowCard(state: HomeUiState.Success, onClick: () -> Unit) {
    val currency = state.currencyCode
    val month = state.thisMonth
    SectionCard(stringResource(R.string.feature_home_this_month), onSeeAll = onClick) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DfStat(stringResource(R.string.feature_home_income), formatMoney(month.income, currency), tone = DfTone.Positive)
            DfStat(stringResource(R.string.feature_home_expenses), formatMoney(month.expenses, currency))
            DfStat(
                stringResource(R.string.feature_home_balance),
                formatMoney(month.net, currency),
                tone = if (month.net < 0) DfTone.Negative else DfTone.Primary,
                alignEnd = true,
            )
        }
        GroupedBarChart(
            first = state.cashFlow.map { it.income.toFloat() },
            second = state.cashFlow.map { it.expenses.toFloat() },
            labels = state.cashFlow.map { it.month.shortLabel() },
            firstColor = DfTheme.colors.chartIncome,
            secondColor = DfTheme.colors.chartExpense,
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
            DfText(stringResource(R.string.feature_home_no_spending), tone = DfTone.Secondary)
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
            ProgressLine(
                label = spending.budget.name,
                value = stringResource(R.string.feature_home_spent_of, formatMoney(spending.spent, currency), formatMoney(spending.budget.amount, currency)),
                valueTone = if (spending.isOverspent) DfTone.Negative else DfTone.Secondary,
                progress = spending.progress,
                progressTone = if (spending.isOverspent) DfTone.Negative else DfTone.Positive,
            )
        }
    }
}

@Composable
private fun BillsCard(bills: List<Pair<Bill, BillStatus>>, actions: HomeActions) {
    SectionCard(stringResource(R.string.feature_home_bills), onSeeAll = actions.onBills) {
        if (bills.isEmpty()) {
            DfText(stringResource(R.string.feature_home_no_bills), tone = DfTone.Secondary)
        }
        bills.forEach { (bill, status) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    DfText(bill.name)
                    val (text, tone) = when (status) {
                        is BillStatus.Overdue -> stringResource(R.string.feature_home_overdue, status.dueDate.medium()) to DfTone.Negative
                        is BillStatus.Due -> stringResource(R.string.feature_home_due, status.dueDate.medium()) to DfTone.Warning
                        else -> "" to DfTone.Secondary
                    }
                    DfText("${formatMoney(bill.amountMax, bill.currencyCode)} · $text", style = DfTheme.typography.caption, tone = tone)
                }
                DfButton(stringResource(R.string.feature_home_pay), { actions.onPayBill(bill.id) }, style = DfButtonStyle.Secondary)
            }
        }
    }
}

@Composable
private fun PiggyBanksCard(state: HomeUiState.Success, onSeeAll: () -> Unit) {
    SectionCard(stringResource(R.string.feature_home_piggy_banks), onSeeAll = onSeeAll) {
        state.piggyBanks.sortedByDescending { it.progress ?: 0f }.take(3).forEach { piggy ->
            val target = piggy.piggyBank.targetAmount
            ProgressLine(
                label = piggy.piggyBank.name,
                value = if (target != null) {
                    stringResource(R.string.feature_home_saved_of, formatMoney(piggy.saved, piggy.currencyCode), formatMoney(target, piggy.currencyCode))
                } else {
                    stringResource(R.string.feature_home_saved, formatMoney(piggy.saved, piggy.currencyCode))
                },
                valueTone = DfTone.Secondary,
                progress = piggy.progress,
                progressTone = DfTone.Positive,
            )
        }
    }
}

@Composable
private fun ProgressLine(label: String, value: String, valueTone: DfTone, progress: Float?, progressTone: DfTone) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DfText(label, modifier = Modifier.weight(1f), maxLines = 1)
            DfText(value, style = DfTheme.typography.caption, tone = valueTone)
        }
        progress?.let { DfProgressBar(it, tone = progressTone) }
    }
}

@Composable
private fun SectionCard(title: String, onSeeAll: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    DfCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DfText(title, style = DfTheme.typography.title, modifier = Modifier.weight(1f))
            if (onSeeAll != null) {
                DfButton(stringResource(R.string.feature_home_see_all), onSeeAll, style = DfButtonStyle.Text)
            }
        }
        content()
    }
}

private fun YearMonth.shortLabel(): String = format(DateTimeFormatter.ofPattern("LLL")).replaceFirstChar { it.uppercase() }

private fun java.time.LocalDate.medium(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
