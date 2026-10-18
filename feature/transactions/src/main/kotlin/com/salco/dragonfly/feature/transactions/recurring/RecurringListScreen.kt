package com.salco.dragonfly.feature.transactions.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.PauseCircle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.RecurringTransaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import com.salco.dragonfly.core.ui.everyLabel
import com.salco.dragonfly.feature.transactions.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun RecurringListScreen(
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RecurringListContent(uiState, onBack, onItemClick, onAdd)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecurringListContent(
    uiState: RecurringListUiState,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAdd: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_transactions_recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_transactions_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_transactions_add)) },
            )
        },
    ) { padding ->
        val items = (uiState as? RecurringListUiState.Success)?.items
        when {
            items == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            items.isEmpty() -> EmptyState(
                icon = Icons.Filled.EventRepeat,
                title = stringResource(R.string.feature_transactions_recurring_empty_title),
                message = stringResource(R.string.feature_transactions_recurring_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(items, key = { it.id }) { item -> RecurringRow(item, onClick = { onItemClick(item.id) }) }
            }
        }
    }
}

@Composable
private fun RecurringRow(item: RecurringTransaction, onClick: () -> Unit) {
    val template = item.template
    val (sign, color) = when (template.type) {
        TransactionType.WITHDRAWAL -> "−" to MaterialTheme.colorScheme.error
        TransactionType.DEPOSIT -> "+" to IncomeColor
        TransactionType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurface
    }
    val next = item.nextDate
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Icon(
                if (item.active && next != null) Icons.Filled.EventRepeat else Icons.Filled.PauseCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(template.description) },
        supportingContent = {
            Column {
                Text("${item.sourceName} → ${item.destinationName}")
                Text(
                    text = everyLabel(item.rule.frequency, item.rule.interval) + " · " + when {
                        next == null -> stringResource(R.string.feature_transactions_recurring_ended)
                        !item.active -> stringResource(R.string.feature_transactions_recurring_paused)
                        else -> stringResource(
                            R.string.feature_transactions_recurring_next,
                            next.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        trailingContent = {
            Text(sign + formatMoney(template.amount, template.currencyCode), color = color, fontWeight = FontWeight.SemiBold)
        },
    )
}

@Preview
@Composable
private fun RecurringListContentPreview() {
    val start = LocalDate.of(2026, 10, 27)
    DragonflyTheme {
        RecurringListContent(
            uiState = RecurringListUiState.Success(
                listOf(
                    RecurringTransaction(
                        id = 1,
                        template = TransactionDraft(
                            type = TransactionType.DEPOSIT, description = "Stipendio", date = start, amount = 200_000,
                            currencyCode = "EUR", source = AccountRef.Existing(2), destination = AccountRef.Existing(1),
                        ),
                        rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = start),
                        sourceName = "Azienda",
                        destinationName = "Conto corrente",
                    ),
                ),
            ),
            onBack = {},
            onItemClick = {},
            onAdd = {},
        )
    }
}
