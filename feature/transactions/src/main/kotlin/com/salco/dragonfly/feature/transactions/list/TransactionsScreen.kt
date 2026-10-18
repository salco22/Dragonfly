package com.salco.dragonfly.feature.transactions.list

import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfChip
import com.salco.dragonfly.core.designsystem.component.DfChipRow
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfMenuChip
import com.salco.dragonfly.core.designsystem.component.DfPeriodSelector
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSearchField
import com.salco.dragonfly.core.designsystem.component.DfStat
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.AccountSummary
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.DayHeader
import com.salco.dragonfly.core.ui.IncomeColor
import com.salco.dragonfly.core.ui.TransactionRow
import com.salco.dragonfly.feature.transactions.R
import com.salco.dragonfly.feature.transactions.labelRes
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun TransactionsScreen(
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
    onRecurringClick: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    TransactionsContent(
        uiState = uiState,
        actions = TransactionsActions(
            onPreviousMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onTransactionClick = onTransactionClick,
            onAddTransactionClick = onAddTransactionClick,
            onRecurringClick = onRecurringClick,
            onQueryChange = viewModel::onQueryChange,
            onTypeFilter = viewModel::onTypeFilter,
            onCategoryFilter = viewModel::onCategoryFilter,
            onTagFilter = viewModel::onTagFilter,
            onBudgetFilter = viewModel::onBudgetFilter,
            onAccountFilter = viewModel::onAccountFilter,
            onClearFilters = viewModel::clearFilters,
        ),
        query = query,
    )
}

internal data class TransactionsActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onTransactionClick: (Long) -> Unit = {},
    val onAddTransactionClick: () -> Unit = {},
    val onRecurringClick: () -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onTypeFilter: (TransactionType?) -> Unit = {},
    val onCategoryFilter: (Long?) -> Unit = {},
    val onTagFilter: (Long?) -> Unit = {},
    val onBudgetFilter: (Long?) -> Unit = {},
    val onAccountFilter: (Long?) -> Unit = {},
    val onClearFilters: () -> Unit = {},
)

@Composable
internal fun TransactionsContent(uiState: TransactionsUiState, actions: TransactionsActions, query: String = "") {
    val success = uiState as? TransactionsUiState.Success
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchOpen) { if (searchOpen) focusRequester.requestFocus() }

    DfScaffold(
        title = stringResource(R.string.feature_transactions_title),
        titleContent = if (searchOpen) {
            {
                DfSearchField(
                    value = query,
                    onValueChange = actions.onQueryChange,
                    placeholder = stringResource(R.string.feature_transactions_search_hint),
                    focusRequester = focusRequester,
                )
            }
        } else {
            null
        },
        actions = {
            if (searchOpen) {
                DfIconButton(Icons.Filled.Close, stringResource(R.string.feature_transactions_close_search), {
                    searchOpen = false
                    actions.onQueryChange("")
                })
            } else {
                DfIconButton(Icons.Outlined.EventRepeat, stringResource(R.string.feature_transactions_recurring), actions.onRecurringClick)
                DfIconButton(Icons.Outlined.Search, stringResource(R.string.feature_transactions_search), { searchOpen = true })
            }
        },
        floatingAction = { DfFab(stringResource(R.string.feature_transactions_add), Icons.Filled.Add, actions.onAddTransactionClick) },
    ) { padding ->
        if (success == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterRow(success, actions)
            if (success.isSearching) {
                DfText(
                    text = pluralStringResource(R.plurals.feature_transactions_results, success.resultCount, success.resultCount),
                    style = DfTheme.typography.label,
                    tone = DfTone.Secondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                DfPeriodSelector(
                    label = success.month.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.uppercase() },
                    onPrevious = actions.onPreviousMonth,
                    onNext = actions.onNextMonth,
                    previousDescription = stringResource(R.string.feature_transactions_previous_month),
                    nextDescription = stringResource(R.string.feature_transactions_next_month),
                )
            }
            when {
                success.days.isEmpty() && success.isSearching -> DfEmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.feature_transactions_no_results_title),
                    message = stringResource(R.string.feature_transactions_no_results_message),
                )
                success.days.isEmpty() -> DfEmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(R.string.feature_transactions_empty_title),
                    message = stringResource(R.string.feature_transactions_empty_message),
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (success.totals.isNotEmpty()) {
                        item { TotalsCard(success.totals) }
                    }
                    success.days.forEach { day ->
                        item(key = "day_${day.date}") { DayHeader(day.date) }
                        items(day.transactions, key = { it.id }) { transaction ->
                            TransactionRow(transaction, onClick = { actions.onTransactionClick(transaction.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: TransactionsUiState.Success, actions: TransactionsActions) {
    val filter = state.filter
    DfChipRow(Modifier.padding(vertical = 4.dp)) {
        // Shown first so it stays reachable when the chips overflow the screen.
        if (state.isSearching) {
            DfChip(
                stringResource(R.string.feature_transactions_clear_filters),
                selected = false,
                onClick = actions.onClearFilters,
                onRemove = actions.onClearFilters,
            )
        }
        DfMenuChip(
            label = filter.type?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.feature_transactions_filter_type),
            selected = filter.type != null,
            options = TransactionType.entries,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = actions.onTypeFilter,
        )
        DfMenuChip(
            label = state.options.categories.firstOrNull { it.id == filter.categoryId }?.name
                ?: stringResource(R.string.feature_transactions_filter_category),
            selected = filter.categoryId != null,
            options = state.options.categories,
            optionLabel = { it.name },
            onSelected = { actions.onCategoryFilter(it?.id) },
        )
        DfMenuChip(
            label = state.options.tags.firstOrNull { it.id == filter.tagId }?.let { "#${it.name}" }
                ?: stringResource(R.string.feature_transactions_filter_tag),
            selected = filter.tagId != null,
            options = state.options.tags,
            optionLabel = { "#${it.name}" },
            onSelected = { actions.onTagFilter(it?.id) },
        )
        if (state.options.budgets.isNotEmpty()) {
            DfMenuChip(
                label = state.options.budgets.firstOrNull { it.id == filter.budgetId }?.name
                    ?: stringResource(R.string.feature_transactions_filter_budget),
                selected = filter.budgetId != null,
                options = state.options.budgets,
                optionLabel = { it.name },
                onSelected = { actions.onBudgetFilter(it?.id) },
            )
        }
        DfMenuChip(
            label = state.options.accounts.firstOrNull { it.id == filter.accountId }?.name
                ?: stringResource(R.string.feature_transactions_filter_account),
            selected = filter.accountId != null,
            options = state.options.accounts,
            optionLabel = { it.name },
            onSelected = { actions.onAccountFilter(it?.id) },
        )
    }
}

@Composable
private fun TotalsCard(totals: Map<String, MonthTotals>) {
    DfCard(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f)) {
                totals.forEach { (currency, t) ->
                    DfStat(stringResource(R.string.feature_transactions_income), formatMoney(t.income, currency), tone = DfTone.Positive)
                }
            }
            Column(Modifier.weight(1f)) {
                totals.forEach { (currency, t) ->
                    DfStat(stringResource(R.string.feature_transactions_expenses), formatMoney(t.expenses, currency))
                }
            }
        }
    }
}

@Preview
@Composable
private fun TransactionsContentPreview() {
    val checking = AccountSummary(1, "Conto corrente", AccountType.ASSET, "EUR")
    val shop = AccountSummary(2, "Supermercato", AccountType.EXPENSE, "EUR")
    val today = LocalDate.of(2026, 10, 17)
    DragonflyTheme {
        TransactionsContent(
            uiState = TransactionsUiState.Success(
                month = YearMonth.of(2026, 10),
                days = listOf(
                    DaySection(
                        today,
                        listOf(Transaction(1, TransactionType.WITHDRAWAL, "Spesa", today, 4_250, "EUR", source = checking, destination = shop)),
                    ),
                ),
                totals = mapOf("EUR" to MonthTotals(income = 0, expenses = 4_250)),
            ),
            actions = TransactionsActions(),
        )
    }
}
