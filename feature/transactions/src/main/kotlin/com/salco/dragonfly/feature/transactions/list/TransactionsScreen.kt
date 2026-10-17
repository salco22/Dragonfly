package com.salco.dragonfly.feature.transactions.list

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.salco.dragonfly.feature.transactions.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TransactionsScreen(
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsContent(
        uiState = uiState,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onTransactionClick = onTransactionClick,
        onAddTransactionClick = onAddTransactionClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionsContent(
    uiState: TransactionsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransactionClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_transactions_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_transactions_add)) },
            )
        },
    ) { padding ->
        when (uiState) {
            TransactionsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            is TransactionsUiState.Success -> Column(Modifier.fillMaxSize().padding(padding)) {
                MonthSelector(uiState.month, onPreviousMonth, onNextMonth)
                if (uiState.days.isEmpty()) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        title = stringResource(R.string.feature_transactions_empty_title),
                        message = stringResource(R.string.feature_transactions_empty_message),
                    )
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                        if (uiState.totals.isNotEmpty()) {
                            item { TotalsCard(uiState.totals) }
                        }
                        uiState.days.forEach { day ->
                            item(key = "day_${day.date}") { DayHeader(day.date) }
                            items(day.transactions, key = { it.id }) { transaction ->
                                TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
                            }
                        }
                    }
                }
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

@Composable
private fun DayHeader(date: LocalDate) {
    Text(
        text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val (icon, sign, color) = when (transaction.type) {
        TransactionType.WITHDRAWAL -> Triple(Icons.Filled.ArrowUpward, "−", MaterialTheme.colorScheme.error)
        TransactionType.DEPOSIT -> Triple(Icons.Filled.ArrowDownward, "+", IncomeColor)
        TransactionType.TRANSFER -> Triple(Icons.Filled.SwapHoriz, "", MaterialTheme.colorScheme.onSurface)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { TypeIcon(icon, color) },
        headlineContent = { Text(transaction.description) },
        supportingContent = {
            Column {
                Text("${transaction.source.name} → ${transaction.destination.name}")
                val labels = listOfNotNull(transaction.category?.name) + transaction.tags.map { "#${it.name}" }
                if (labels.isNotEmpty()) {
                    Text(
                        text = labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        trailingContent = {
            Text(
                text = sign + formatMoney(transaction.amount, transaction.currencyCode),
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        },
    )
}

@Composable
private fun TypeIcon(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint)
}

// Matches the primary green of the Dragonfly palette.
private val IncomeColor = Color(0xFF2E9B48)

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
                        listOf(
                            Transaction(1, TransactionType.WITHDRAWAL, "Spesa", today, 4_250, "EUR", source = checking, destination = shop),
                        ),
                    ),
                ),
                totals = mapOf("EUR" to MonthTotals(income = 0, expenses = 4_250)),
            ),
            onPreviousMonth = {},
            onNextMonth = {},
            onTransactionClick = {},
            onAddTransactionClick = {},
        )
    }
}
