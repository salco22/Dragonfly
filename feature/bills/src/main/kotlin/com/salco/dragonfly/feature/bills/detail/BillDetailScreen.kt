package com.salco.dragonfly.feature.bills.detail

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSectionHeader
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.ui.TransactionRow
import com.salco.dragonfly.core.ui.everyLabel
import com.salco.dragonfly.feature.bills.R
import com.salco.dragonfly.feature.bills.amountLabel
import com.salco.dragonfly.feature.bills.tone
import com.salco.dragonfly.feature.bills.label
import com.salco.dragonfly.feature.bills.medium

@Composable
internal fun BillDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onPay: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: BillDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Leave the screen if the bill was deleted from its edit screen.
    LaunchedEffect(uiState) { if (uiState is BillDetailUiState.NotFound) onBack() }
    val state = uiState as? BillDetailUiState.Success

    DfScaffold(
        title = state?.bill?.name.orEmpty(),
        onBack = onBack,
        actions = { DfIconButton(Icons.Outlined.Edit, stringResource(R.string.feature_bills_edit), { onEdit(viewModel.billId) }) },
    ) { padding ->
        if (state == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        val bill = state.bill
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                DfCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), highlighted = true, padding = 20.dp, spacing = 6.dp) {
                    DfText(bill.amountLabel(), style = DfTheme.typography.headline)
                    DfText(everyLabel(bill.rule.frequency, bill.rule.interval), tone = DfTone.Secondary)
                    DfText(state.status.label(), style = DfTheme.typography.label, tone = state.status.tone())
                    bill.notes?.let { DfText(it, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
                    DfButton(
                        stringResource(R.string.feature_bills_register_payment),
                        { onPay(bill.id) },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (state.upcoming.isNotEmpty()) {
                item { DfSectionHeader(stringResource(R.string.feature_bills_upcoming)) }
                items(state.upcoming) { date ->
                    DfText(date.medium(), modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }
            }
            item { DfSectionHeader(stringResource(R.string.feature_bills_payments)) }
            if (state.payments.isEmpty()) {
                item {
                    DfText(
                        stringResource(R.string.feature_bills_no_payments),
                        tone = DfTone.Secondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.payments, key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
            }
        }
    }
}
