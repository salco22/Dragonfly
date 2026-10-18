package com.salco.dragonfly.feature.transactions.list

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
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
import com.salco.dragonfly.core.designsystem.component.EmptyState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionsContent(uiState: TransactionsUiState, actions: TransactionsActions, query: String = "") {
    val success = uiState as? TransactionsUiState.Success
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchOpen) {
                        SearchField(query, actions.onQueryChange)
                    } else {
                        Text(stringResource(R.string.feature_transactions_title))
                    }
                },
                actions = {
                    if (searchOpen) {
                        IconButton(onClick = {
                            searchOpen = false
                            actions.onQueryChange("")
                        }) { Icon(Icons.Filled.Close, stringResource(R.string.feature_transactions_close_search)) }
                    } else {
                        IconButton(onClick = actions.onRecurringClick) {
                            Icon(Icons.Filled.EventRepeat, stringResource(R.string.feature_transactions_recurring))
                        }
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.feature_transactions_search))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = actions.onAddTransactionClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_transactions_add)) },
            )
        },
    ) { padding ->
        if (success == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterRow(success, actions)
            if (success.isSearching) {
                Text(
                    text = pluralStringResource(R.plurals.feature_transactions_results, success.resultCount, success.resultCount),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } else {
                MonthSelector(success.month, actions.onPreviousMonth, actions.onNextMonth)
            }
            when {
                success.days.isEmpty() && success.isSearching -> EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = stringResource(R.string.feature_transactions_no_results_title),
                    message = stringResource(R.string.feature_transactions_no_results_message),
                )
                success.days.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
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
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.feature_transactions_search_hint)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

@Composable
private fun FilterRow(state: TransactionsUiState.Success, actions: TransactionsActions) {
    val filter = state.filter
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Shown first so it stays reachable when the chips overflow the screen.
        if (state.isSearching) {
            FilterChip(
                selected = false,
                onClick = actions.onClearFilters,
                label = { Text(stringResource(R.string.feature_transactions_clear_filters)) },
                leadingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(FilterChipDefaults.IconSize)) },
            )
        }
        MenuFilterChip(
            label = filter.type?.let { stringResource(it.labelRes()) } ?: stringResource(R.string.feature_transactions_filter_type),
            selected = filter.type != null,
            options = TransactionType.entries,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = actions.onTypeFilter,
        )
        MenuFilterChip(
            label = state.options.categories.firstOrNull { it.id == filter.categoryId }?.name
                ?: stringResource(R.string.feature_transactions_filter_category),
            selected = filter.categoryId != null,
            options = state.options.categories,
            optionLabel = { it.name },
            onSelected = { actions.onCategoryFilter(it?.id) },
        )
        MenuFilterChip(
            label = state.options.tags.firstOrNull { it.id == filter.tagId }?.let { "#${it.name}" }
                ?: stringResource(R.string.feature_transactions_filter_tag),
            selected = filter.tagId != null,
            options = state.options.tags,
            optionLabel = { "#${it.name}" },
            onSelected = { actions.onTagFilter(it?.id) },
        )
        if (state.options.budgets.isNotEmpty()) {
            MenuFilterChip(
                label = state.options.budgets.firstOrNull { it.id == filter.budgetId }?.name
                    ?: stringResource(R.string.feature_transactions_filter_budget),
                selected = filter.budgetId != null,
                options = state.options.budgets,
                optionLabel = { it.name },
                onSelected = { actions.onBudgetFilter(it?.id) },
            )
        }
        MenuFilterChip(
            label = state.options.accounts.firstOrNull { it.id == filter.accountId }?.name
                ?: stringResource(R.string.feature_transactions_filter_account),
            selected = filter.accountId != null,
            options = state.options.accounts,
            optionLabel = { it.name },
            onSelected = { actions.onAccountFilter(it?.id) },
        )
    }
}

/** A chip that opens a menu of [options]; picking "All" (null) removes the filter. */
@Composable
private fun <T> MenuFilterChip(
    label: String,
    selected: Boolean,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(FilterChipDefaults.IconSize)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.feature_transactions_filter_all)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_transactions_previous_month))
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.feature_transactions_next_month))
        }
    }
}

@Composable
private fun TotalsCard(totals: Map<String, MonthTotals>) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TotalColumn(
                label = stringResource(R.string.feature_transactions_income),
                values = totals.map { (currency, t) -> formatMoney(t.income, currency) },
                color = IncomeColor,
                modifier = Modifier.weight(1f),
            )
            TotalColumn(
                label = stringResource(R.string.feature_transactions_expenses),
                values = totals.map { (currency, t) -> formatMoney(t.expenses, currency) },
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TotalColumn(label: String, values: List<String>, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        values.forEach {
            Text(it, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.SemiBold)
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
