package com.salco.dragonfly.feature.bills.list

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.everyLabel
import com.salco.dragonfly.feature.bills.R
import com.salco.dragonfly.feature.bills.amountLabel
import com.salco.dragonfly.feature.bills.canPay
import com.salco.dragonfly.feature.bills.color
import com.salco.dragonfly.feature.bills.label
import java.time.LocalDate

@Composable
internal fun BillsScreen(
    onBack: () -> Unit,
    onBillClick: (Long) -> Unit,
    onAddBill: () -> Unit,
    onPayBill: (Long) -> Unit,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BillsContent(uiState, onBillClick, onAddBill, onPayBill, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillsContent(
    uiState: BillsUiState,
    onBillClick: (Long) -> Unit,
    onAddBill: () -> Unit,
    onPayBill: (Long) -> Unit,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_bills_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_bills_back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBill,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_bills_add)) },
            )
        },
    ) { padding ->
        val state = uiState as? BillsUiState.Success
        when {
            state == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.items.isEmpty() -> EmptyState(
                icon = Icons.Filled.ReceiptLong,
                title = stringResource(R.string.feature_bills_empty_title),
                message = stringResource(R.string.feature_bills_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.expectedThisMonth.isNotEmpty() || state.paidThisMonth.isNotEmpty()) {
                    item { SummaryCard(state.expectedThisMonth, state.paidThisMonth) }
                }
                items(state.items, key = { it.bill.id }) { item ->
                    BillCard(item, onClick = { onBillClick(item.bill.id) }, onPay = { onPayBill(item.bill.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(expected: Map<String, Long>, paid: Map<String, Long>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SummaryColumn(stringResource(R.string.feature_bills_expected_month), expected, Modifier.weight(1f))
            SummaryColumn(stringResource(R.string.feature_bills_paid_month), paid, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryColumn(label: String, amounts: Map<String, Long>, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (amounts.isEmpty()) {
            Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        amounts.forEach { (currency, amount) ->
            Text(
                formatMoney(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun BillCard(item: BillItem, onClick: () -> Unit, onPay: () -> Unit) {
    val bill = item.bill
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(bill.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${bill.amountLabel()} · ${everyLabel(bill.rule.frequency, bill.rule.interval)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(item.status.label(), style = MaterialTheme.typography.labelLarge, color = item.status.color())
            }
            if (item.status.canPay) {
                FilledTonalButton(onClick = onPay) { Text(stringResource(R.string.feature_bills_pay)) }
            }
        }
    }
}

@Preview
@Composable
private fun BillsContentPreview() {
    val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, 5))
    DragonflyTheme {
        BillsContent(
            uiState = BillsUiState.Success(
                items = listOf(
                    BillItem(Bill(1, "Affitto", 80_000, 80_000, "EUR", rule), BillStatus.Overdue(LocalDate.of(2026, 10, 5))),
                    BillItem(Bill(2, "Luce", 4_000, 7_000, "EUR", rule), BillStatus.Due(LocalDate.of(2026, 10, 25))),
                    BillItem(Bill(3, "Netflix", 1_399, 1_399, "EUR", rule), BillStatus.Paid(LocalDate.of(2026, 10, 2), 1_399, null)),
                ),
                expectedThisMonth = mapOf("EUR" to 88_399),
                paidThisMonth = mapOf("EUR" to 1_399),
            ),
            onBillClick = {},
            onAddBill = {},
            onPayBill = {},
        )
    }
}
