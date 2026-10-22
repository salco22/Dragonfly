package com.salco.dragonfly.feature.reports

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfPeriodSelector
import com.salco.dragonfly.core.designsystem.component.DfProgressBar
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSegmentedControl
import com.salco.dragonfly.core.designsystem.component.DfTabs
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.color
import com.salco.dragonfly.core.designsystem.component.LineChart
import com.salco.dragonfly.core.designsystem.component.GroupedBarChart
import com.salco.dragonfly.core.designsystem.component.DonutChart
import com.salco.dragonfly.core.designsystem.component.DonutSlice
import com.salco.dragonfly.core.designsystem.component.ChartPalette
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.NamedTotal
import com.salco.dragonfly.core.model.PeriodLength
import com.salco.dragonfly.core.model.ReportPeriod
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.model.percentChange
import java.time.format.DateTimeFormatter

private enum class ReportTab(val labelRes: Int) {
    OVERVIEW(R.string.feature_reports_tab_overview),
    CATEGORIES(R.string.feature_reports_tab_categories),
    BUDGETS(R.string.feature_reports_tab_budgets),
    TAGS(R.string.feature_reports_tab_tags),
}

@Composable
internal fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showIncome by rememberSaveable { mutableIntStateOf(0) }

    DfScaffold(title = stringResource(R.string.feature_reports_title), onBack = onBack) { padding ->
        val state = uiState as? ReportsUiState.Success
        if (state == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            PeriodSelector(state.period, viewModel)
            DfTabs(ReportTab.entries.map { stringResource(it.labelRes) }, tab, { tab = it })
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                when (ReportTab.entries[tab]) {
                    ReportTab.OVERVIEW -> overview(state)
                    ReportTab.CATEGORIES -> categories(state, showIncome == 1) { showIncome = it }
                    ReportTab.BUDGETS -> budgets(state)
                    ReportTab.TAGS -> tags(state)
                }
                item {
                    DfText(
                        stringResource(R.string.feature_reports_currency_note, state.currencyCode),
                        style = DfTheme.typography.caption,
                        tone = DfTone.Tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(period: ReportPeriod, viewModel: ReportsViewModel) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DfSegmentedControl(
            options = PeriodLength.entries,
            selected = period.length,
            optionLabel = { length ->
                stringResource(
                    when (length) {
                        PeriodLength.MONTH -> R.string.feature_reports_month
                        PeriodLength.QUARTER -> R.string.feature_reports_quarter
                        PeriodLength.YEAR -> R.string.feature_reports_year
                    },
                )
            },
            onSelect = viewModel::setLength,
            modifier = Modifier.fillMaxWidth(),
        )
        DfPeriodSelector(
            label = period.label(),
            onPrevious = viewModel::previous,
            onNext = viewModel::next,
            previousDescription = stringResource(R.string.feature_reports_previous),
            nextDescription = stringResource(R.string.feature_reports_next),
        )
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
            Figure(stringResource(R.string.feature_reports_income), state.income, state.previousIncome, currency, DfTone.Positive, higherIsBetter = true)
            Figure(stringResource(R.string.feature_reports_expenses), state.expenses, state.previousExpenses, currency, DfTone.Negative, higherIsBetter = false)
            Figure(
                stringResource(R.string.feature_reports_net),
                state.net,
                state.previousIncome - state.previousExpenses,
                currency,
                if (state.net < 0) DfTone.Negative else DfTone.Primary,
                higherIsBetter = true,
            )
            state.savingsRate?.let { DfText(stringResource(R.string.feature_reports_savings_rate, it), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
            if (state.monthly.size > 1) {
                DfText(stringResource(R.string.feature_reports_average, formatMoney(state.averageMonthlyExpenses, currency)), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary)
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
                    firstColor = DfTone.Positive.color(),
                    secondColor = DfTone.Negative.color(),
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
        DfSegmentedControl(
            options = listOf(0, 1),
            selected = if (income) 1 else 0,
            optionLabel = { stringResource(if (it == 1) R.string.feature_reports_income else R.string.feature_reports_expenses) },
            onSelect = onToggle,
            modifier = Modifier.fillMaxWidth(),
        )
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

private fun LazyListScope.budgets(state: ReportsUiState.Success) {
    if (state.budgets.isEmpty()) {
        item { Empty(stringResource(R.string.feature_reports_no_budgets)) }
        return
    }
    items(state.budgets) { row ->
        val over = row.spent > row.budgeted
        DfCard(spacing = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DfText(row.name, modifier = Modifier.weight(1f))
                DfText(
                    stringResource(R.string.feature_reports_budget_progress, formatMoney(row.spent, state.currencyCode), formatMoney(row.budgeted, state.currencyCode)),
                    tone = if (over) DfTone.Negative else DfTone.Secondary,
                    style = DfTheme.typography.caption,
                )
            }
            DfProgressBar(row.progress, tone = if (over) DfTone.Negative else DfTone.Positive)
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
    val parts = listOfNotNull(
        pluralStringResource(R.plurals.feature_reports_transactions, total.count, total.count),
        share?.let { stringResource(R.string.feature_reports_share, Math.round(it * 100)) },
    )
    DfCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DfText(total.name ?: fallbackName, maxLines = 1)
                DfText(parts.joinToString(" · "), style = DfTheme.typography.caption, tone = DfTone.Secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                DfAmount(formatMoney(total.amount, currency), style = DfTheme.typography.body)
                change?.let {
                    // For spending, going up is bad news.
                    DfText(
                        (if (it > 0) "+" else "") + "$it%",
                        style = DfTheme.typography.caption,
                        tone = if (it > 0) DfTone.Negative else DfTone.Positive,
                    )
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: Long, previous: Long, currency: String, tone: DfTone, higherIsBetter: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        DfText(label, tone = DfTone.Secondary, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            DfAmount(formatMoney(value, currency), tone = tone)
            percentChange(value, previous)?.let { change ->
                val good = if (higherIsBetter) change >= 0 else change <= 0
                DfText(
                    stringResource(R.string.feature_reports_vs_previous, (if (change > 0) "+" else "") + "$change%"),
                    style = DfTheme.typography.caption,
                    tone = if (good) DfTone.Positive else DfTone.Negative,
                )
            }
        }
    }
}

@Composable
private fun Section(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    DfCard(spacing = 12.dp) {
        title?.let { DfText(it, style = DfTheme.typography.subtitle) }
        content()
    }
}

@Composable
private fun Empty(text: String) {
    DfText(text, tone = DfTone.Secondary, modifier = Modifier.padding(vertical = 8.dp))
}
