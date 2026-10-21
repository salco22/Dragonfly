package com.salco.dragonfly.feature.budgets.list

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfBudgetCard
import com.salco.dragonfly.core.designsystem.component.DfBudgetStatus
import com.salco.dragonfly.core.designsystem.component.DfHeroCard
import com.salco.dragonfly.core.designsystem.component.DfHeroNote
import com.salco.dragonfly.core.designsystem.component.DfHeroNotes
import com.salco.dragonfly.core.designsystem.component.DfHeroProgressBar
import com.salco.dragonfly.core.designsystem.component.DfInfoBanner
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
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
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
import com.salco.dragonfly.feature.budgets.MonthSelector
import com.salco.dragonfly.feature.budgets.R
import com.salco.dragonfly.feature.budgets.remainingText
import java.time.YearMonth
import kotlin.math.roundToInt

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
        subtitle = stringResource(R.string.feature_budgets_subtitle),
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
                item(key = "totals_$currency") { TotalsCard(currency, totals, state.daysLeft) }
            }
            if (state.budgets.isNotEmpty()) {
                item { DfText(stringResource(R.string.feature_budgets_limits), style = DfTheme.typography.title, modifier = Modifier.padding(top = 8.dp)) }
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
            state.totals.entries.singleOrNull()?.takeIf { it.value.left > 0 }?.let { (currency, totals) ->
                item {
                    DfInfoBanner(Icons.Outlined.Spa, stringResource(R.string.feature_budgets_margin, formatMoney(totals.left, currency)))
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

/** What is left of all budgets in [currency] this month, on the dark hero card. */
@Composable
private fun TotalsCard(currency: String, totals: BudgetTotals, daysLeft: Int?) {
    val over = totals.left < 0
    DfHeroCard(
        title = stringResource(if (over) R.string.feature_budgets_hero_title_over else R.string.feature_budgets_hero_title),
        value = formatMoney(if (over) -totals.left else totals.left, currency),
    ) {
        val progress = if (totals.budgeted > 0) totals.spent.toFloat() / totals.budgeted else 0f
        DfHeroProgressBar(progress, Modifier.padding(top = 4.dp))
        DfHeroNotes(
            stringResource(R.string.feature_budgets_hero_spent, formatMoney(totals.spent, currency)),
            stringResource(R.string.feature_budgets_hero_of, formatMoney(totals.budgeted, currency)),
        )
        val percent = (progress * 100).roundToInt()
        DfHeroNote(
            if (daysLeft != null) {
                pluralStringResource(R.plurals.feature_budgets_hero_used_days, daysLeft, percent, daysLeft)
            } else {
                stringResource(R.string.feature_budgets_hero_used, percent)
            },
            highlighted = true,
        )
    }
}

/** A budget close to its limit (85% or more) is shown as a warning before it is overspent. */
private const val NEAR_LIMIT = 0.85f

@Composable
private fun BudgetCard(spending: BudgetSpending, daysLeft: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val currency = spending.budget.currencyCode
    val status = when {
        spending.isOverspent -> DfBudgetStatus.Over
        spending.progress >= NEAR_LIMIT -> DfBudgetStatus.NearLimit
        else -> DfBudgetStatus.OnTrack
    }
    DfBudgetCard(
        icon = Icons.Outlined.PieChart,
        name = spending.budget.name,
        amounts = stringResource(R.string.feature_budgets_spent_of, formatMoney(spending.spent, currency), formatMoney(spending.budget.amount, currency)),
        progress = spending.progress,
        status = status,
        statusText = stringResource(
            when (status) {
                DfBudgetStatus.OnTrack -> R.string.feature_budgets_status_on_track
                DfBudgetStatus.NearLimit -> R.string.feature_budgets_status_near_limit
                DfBudgetStatus.Over -> R.string.feature_budgets_status_over
            },
        ),
        remainingText = if (spending.isOverspent) {
            remainingText(spending, daysLeft)
        } else {
            stringResource(R.string.feature_budgets_left_amount, formatMoney(spending.remaining, currency))
        },
        modifier = modifier,
        onClick = onClick,
    )
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
