package com.salco.dragonfly.feature.bills.detail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.salco.dragonfly.feature.bills.color
import com.salco.dragonfly.feature.bills.label
import com.salco.dragonfly.feature.bills.medium

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.bill?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_bills_back)) }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.billId) }) { Icon(Icons.Filled.Edit, stringResource(R.string.feature_bills_edit)) }
                },
            )
        },
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val bill = state.bill
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(bill.amountLabel(), style = MaterialTheme.typography.headlineSmall)
                        Text(everyLabel(bill.rule.frequency, bill.rule.interval), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(state.status.label(), style = MaterialTheme.typography.titleSmall, color = state.status.color())
                        bill.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = { onPay(bill.id) }, modifier = Modifier.padding(top = 8.dp)) {
                            Text(stringResource(R.string.feature_bills_register_payment))
                        }
                    }
                }
            }
            if (state.upcoming.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_bills_upcoming),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                items(state.upcoming) { date ->
                    Text(date.medium(), modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
                }
            }
            item {
                Text(
                    stringResource(R.string.feature_bills_payments),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
            if (state.payments.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_bills_no_payments),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.payments, key = { it.id }) { transaction ->
                TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
            }
        }
    }
}
