package com.salco.dragonfly.feature.bills.list

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
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
import com.salco.dragonfly.feature.bills.tone
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

@Composable
internal fun BillsContent(
    uiState: BillsUiState,
    onBillClick: (Long) -> Unit,
    onAddBill: () -> Unit,
    onPayBill: (Long) -> Unit,
    onBack: () -> Unit = {},
) {
    DfScaffold(
        title = stringResource(R.string.feature_bills_title),
        onBack = onBack,
        floatingAction = { DfFab(stringResource(R.string.feature_bills_add), Icons.Filled.Add, onAddBill) },
    ) { padding ->
        val state = uiState as? BillsUiState.Success
        when {
            state == null -> DfLoading(Modifier.padding(padding))
            state.items.isEmpty() -> DfEmptyState(
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = stringResource(R.string.feature_bills_empty_title),
                message = stringResource(R.string.feature_bills_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
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
    DfCard(highlighted = true) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            SummaryColumn(stringResource(R.string.feature_bills_expected_month), expected, Modifier.weight(1f))
            SummaryColumn(stringResource(R.string.feature_bills_paid_month), paid, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryColumn(label: String, amounts: Map<String, Long>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DfText(label, style = DfTheme.typography.caption, tone = DfTone.Secondary)
        if (amounts.isEmpty()) DfText("—", style = DfTheme.typography.title)
        amounts.forEach { (currency, amount) -> DfText(formatMoney(amount, currency), style = DfTheme.typography.title) }
    }
}

@Composable
private fun BillCard(item: BillItem, onClick: () -> Unit, onPay: () -> Unit) {
    val bill = item.bill
    DfCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DfText(bill.name, style = DfTheme.typography.subtitle)
                DfText(
                    "${bill.amountLabel()} · ${everyLabel(bill.rule.frequency, bill.rule.interval)}",
                    style = DfTheme.typography.bodySmall,
                    tone = DfTone.Secondary,
                )
                DfText(item.status.label(), style = DfTheme.typography.caption, tone = item.status.tone())
            }
            if (item.status.canPay) {
                DfButton(stringResource(R.string.feature_bills_pay), onPay, style = DfButtonStyle.Secondary)
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
