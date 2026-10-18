package com.salco.dragonfly.feature.reports

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.ChartPalette
import com.salco.dragonfly.core.designsystem.component.DonutChart
import com.salco.dragonfly.core.designsystem.component.DonutSlice
import com.salco.dragonfly.core.designsystem.component.GroupedBarChart
import com.salco.dragonfly.core.designsystem.component.LineChart
import com.salco.dragonfly.core.model.NamedTotal
import com.salco.dragonfly.core.model.PeriodLength
import com.salco.dragonfly.core.model.ReportPeriod
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.model.percentChange
import com.salco.dragonfly.core.ui.IncomeColor
import java.time.format.DateTimeFormatter

private enum class ReportTab(val labelRes: Int) {
    OVERVIEW(R.string.feature_reports_tab_overview),
    CATEGORIES(R.string.feature_reports_tab_categories),
    COUNTERPARTIES(R.string.feature_reports_tab_counterparties),
    BUDGETS(R.string.feature_reports_tab_budgets),
    TAGS(R.string.feature_reports_tab_tags),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showIncome by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_reports_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_reports_back)) }
                },
            )
        },
    ) { padding ->
        val state = uiState as? ReportsUiState.Success
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            PeriodSelector(state.period, viewModel)
            PrimaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 8.dp) {
                ReportTab.entries.forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(t.labelRes)) })
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when (ReportTab.entries[tab]) {
                    ReportTab.OVERVIEW -> overview(state)
                    ReportTab.CATEGORIES -> categories(state, showIncome == 1) { showIncome = it }
                    ReportTab.COUNTERPARTIES -> counterparties(state)
                    ReportTab.BUDGETS -> budgets(state)
                    ReportTab.TAGS -> tags(state)
                }
                item {
                    Text(
                        stringResource(R.string.feature_reports_currency_note, state.currencyCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(period: ReportPeriod, viewModel: ReportsViewModel) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val lengths = PeriodLength.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            lengths.forEachIndexed { i, length ->
                SegmentedButton(
                    selected = period.length == length,
                    onClick = { viewModel.setLength(length) },
                    shape = SegmentedButtonDefaults.itemShape(i, lengths.size),
                ) {
                    Text(
                        stringResource(
                            when (length) {
                                PeriodLength.MONTH -> R.string.feature_reports_month
                                PeriodLength.QUARTER -> R.string.feature_reports_quarter
                                PeriodLength.YEAR -> R.string.feature_reports_year
                            },
                        ),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::previous) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_reports_previous))
            }
            Text(period.label(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = viewModel::next) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.feature_reports_next))
            }
        }
    }
}

@Composable
private fun ReportPeriod.label(): String = when (length) {
    PeriodLength.MONTH -> start.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.uppercase() }
    PeriodLength.QUARTER -> stringResource(R.string.feature_reports_quarter_label, (start.monthValue - 1) / 3 + 1, start.year)
    PeriodLength.YEAR -> start.year.toString()
}

private fun LazyListScope.overview(state: ReportsUiState.Success) {
    val currency = state.currencyCode
    item {
        Section {
            Figure(stringResource(R.string.feature_reports_income), state.income, state.previousIncome, currency, IncomeColor, higherIsBetter = true)
            Figure(stringResource(R.string.feature_reports_expenses), state.expenses, state.previousExpenses, currency, MaterialTheme.colorScheme.error, higherIsBetter = false)
            Figure(
                stringResource(R.string.feature_reports_net),
                state.net,
                state.previousIncome - state.previousExpenses,
                currency,
                if (state.net < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                higherIsBetter = true,
            )
            state.savingsRate?.let { Text(stringResource(R.string.feature_reports_savings_rate, it)) }
            if (state.monthly.size > 1) {
                Text(stringResource(R.string.feature_reports_average, formatMoney(state.averageMonthlyExpenses, currency)))
            }
        }
    }
    if (state.monthly.size > 1) {
        item {
            Section(stringResource(R.string.feature_reports_by_month)) {
                val labels = state.monthly.map { it.month.format(DateTimeFormatter.ofPattern("LLL")).replaceFirstChar { c -> c.uppercase() } }
                GroupedBarChart(
                    first = state.monthly.map { it.income.toFloat() },
                    second = state.monthly.map { it.expenses.toFloat() },
                    labels = if (labels.size > 6) labels.mapIndexed { i, l -> if (i % 2 == 0) l else "" } else labels,
                    firstColor = IncomeColor,
                    secondColor = MaterialTheme.colorScheme.error,
                    description = stringResource(
                        R.string.feature_reports_chart_description,
                        stringResource(R.string.feature_reports_by_month),
                        state.monthly.joinToString { "${it.month} +${formatMoney(it.income, currency)} −${formatMoney(it.expenses, currency)}" },
                    ),
                )
            }
        }
        item {
            Section(stringResource(R.string.feature_reports_net_worth)) {
                LineChart(
                    values = state.netWorth.map { it.amount.toFloat() },
                    labels = state.netWorth.mapIndexed { i, p ->
                        if (state.netWorth.size <= 6 || i % 3 == 0) p.month.format(DateTimeFormatter.ofPattern("LLL")).replaceFirstChar { it.uppercase() } else ""
                    },
                    description = stringResource(
                        R.string.feature_reports_chart_description,
                        stringResource(R.string.feature_reports_net_worth),
                        state.netWorth.joinToString { "${it.month} ${formatMoney(it.amount, currency)}" },
                    ),
                )
            }
        }
    }
}

private fun LazyListScope.categories(state: ReportsUiState.Success, income: Boolean, onToggle: (Int) -> Unit) {
    item {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(R.string.feature_reports_expenses, R.string.feature_reports_income).forEachIndexed { i, label ->
                SegmentedButton(selected = (i == 1) == income, onClick = { onToggle(i) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                    Text(stringResource(label))
                }
            }
        }
    }
    val totals = if (income) state.incomeCategories else state.expenseCategories.map { it.total }
    if (totals.isEmpty()) {
        item { Empty(stringResource(R.string.feature_reports_empty)) }
        return
    }
    item {
        Section {
            val uncategorized = stringResource(R.string.feature_reports_uncategorized)
            val other = stringResource(R.string.feature_reports_other)
            val top = totals.take(6)
            val rest = totals.drop(6).sumOf { it.amount }
            val slices = top.mapIndexed { i, t ->
                DonutSlice(t.name ?: uncategorized, t.amount.toFloat(), ChartPalette[i % ChartPalette.size], formatMoney(t.amount, state.currencyCode))
            } + listOfNotNull(rest.takeIf { it > 0 }?.let { DonutSlice(other, it.toFloat(), ChartPalette.last(), formatMoney(it, state.currencyCode)) })
            DonutChart(
                slices = slices,
                centerText = formatMoney(totals.sumOf { it.amount }, state.currencyCode),
                description = stringResource(
                    R.string.feature_reports_chart_description,
                    stringResource(R.string.feature_reports_tab_categories),
                    slices.joinToString { "${it.label} ${it.valueLabel}" },
                ),
            )
        }
    }
    if (income) {
        items(state.incomeCategories) { TotalRow(it, state.currencyCode, stringResource(R.string.feature_reports_uncategorized)) }
    } else {
        items(state.expenseCategories) { row ->
            TotalRow(row.total, state.currencyCode, stringResource(R.string.feature_reports_uncategorized), share = row.share, change = row.change)
        }
    }
}

private fun LazyListScope.counterparties(state: ReportsUiState.Success) {
    item { Text(stringResource(R.string.feature_reports_payees), style = MaterialTheme.typography.titleMedium) }
    if (state.payees.isEmpty()) item { Empty(stringResource(R.string.feature_reports_empty)) }
    items(state.payees.take(20)) { TotalRow(it, state.currencyCode, "") }
    item { Text(stringResource(R.string.feature_reports_payers), style = MaterialTheme.typography.titleMedium) }
    if (state.payers.isEmpty()) item { Empty(stringResource(R.string.feature_reports_empty)) }
    items(state.payers.take(20)) { TotalRow(it, state.currencyCode, "") }
}

private fun LazyListScope.budgets(state: ReportsUiState.Success) {
    if (state.budgets.isEmpty()) {
        item { Empty(stringResource(R.string.feature_reports_no_budgets)) }
        return
    }
    items(state.budgets) { row ->
        val over = row.spent > row.budgeted
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.feature_reports_budget_progress, formatMoney(row.spent, state.currencyCode), formatMoney(row.budgeted, state.currencyCode)),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LinearProgressIndicator(
                progress = { row.progress.coerceIn(0f, 1f) },
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun LazyListScope.tags(state: ReportsUiState.Success) {
    if (state.tags.isEmpty()) {
        item { Empty(stringResource(R.string.feature_reports_no_tags)) }
        return
    }
    items(state.tags) { TotalRow(it.copy(name = "#${it.name}"), state.currencyCode, "") }
}

@Composable
private fun TotalRow(total: NamedTotal, currency: String, fallbackName: String, share: Float? = null, change: Int? = null) {
    ListItem(
        headlineContent = { Text(total.name ?: fallbackName) },
        supportingContent = {
            val parts = listOfNotNull(
                pluralStringResource(R.plurals.feature_reports_transactions, total.count, total.count),
                share?.let { stringResource(R.string.feature_reports_share, Math.round(it * 100)) },
            )
            Text(parts.joinToString(" · "))
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(total.amount, currency), fontWeight = FontWeight.SemiBold)
                change?.let {
                    // For spending, going up is bad news.
                    Text(
                        (if (it > 0) "+" else "") + "$it%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (it > 0) MaterialTheme.colorScheme.error else IncomeColor,
                    )
                }
            }
        },
    )
}

@Composable
private fun Figure(label: String, value: Long, previous: Long, currency: String, color: Color, higherIsBetter: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoney(value, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
            percentChange(value, previous)?.let { change ->
                val good = if (higherIsBetter) change >= 0 else change <= 0
                Text(
                    stringResource(R.string.feature_reports_vs_previous, (if (change > 0) "+" else "") + "$change%"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (good) IncomeColor else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            content()
        }
    }
}

@Composable
private fun Empty(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
}
