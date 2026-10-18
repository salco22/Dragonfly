package com.salco.dragonfly.feature.piggybanks.detail

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import com.salco.dragonfly.feature.piggybanks.PiggyBankProgress
import com.salco.dragonfly.feature.piggybanks.R
import com.salco.dragonfly.feature.piggybanks.medium
import com.salco.dragonfly.feature.piggybanks.planLabel
import com.salco.dragonfly.feature.piggybanks.savedLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PiggyBankDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PiggyBankDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    // Leave the screen if the piggy bank was deleted from its edit screen.
    LaunchedEffect(uiState) { if (uiState is PiggyBankDetailUiState.NotFound) onBack() }
    val state = uiState as? PiggyBankDetailUiState.Success

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.summary?.piggyBank?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_piggybanks_back)) }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.piggyBankId) }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.feature_piggybanks_edit))
                    }
                },
            )
        },
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val summary = state.summary
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(summary.savedLabel(), style = MaterialTheme.typography.headlineSmall)
                        PiggyBankProgress(summary)
                        summary.planLabel(state.today)?.let { Text(it, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                        Text(
                            stringResource(R.string.feature_piggybanks_in_account, summary.accountName) + " · " +
                                stringResource(R.string.feature_piggybanks_available, formatMoney(state.availableInAccount, summary.currencyCode)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        summary.piggyBank.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = viewModel::startAdding) { Text(stringResource(R.string.feature_piggybanks_add_money)) }
                            OutlinedButton(onClick = viewModel::startRemoving, enabled = summary.saved > 0) {
                                Text(stringResource(R.string.feature_piggybanks_remove_money))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.feature_piggybanks_history),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.events.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_piggybanks_no_history),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.events, key = { it.id }) { event ->
                ListItem(
                    headlineContent = { Text(event.date.medium()) },
                    trailingContent = {
                        Text(
                            (if (event.amount > 0) "+" else "−") + formatMoney(kotlin.math.abs(event.amount), summary.currencyCode),
                            color = if (event.amount > 0) IncomeColor else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                )
            }
        }

        dialog?.let { move ->
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = {
                    Text(
                        stringResource(
                            if (move.adding) R.string.feature_piggybanks_add_money_title else R.string.feature_piggybanks_remove_money_title,
                        ),
                    )
                },
                text = {
                    val available = if (move.adding) state.availableInAccount else summary.saved
                    OutlinedTextField(
                        value = move.amount,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text(stringResource(R.string.feature_piggybanks_amount)) },
                        suffix = { Text(summary.currencyCode) },
                        isError = move.error != null,
                        supportingText = {
                            Text(
                                when (val error = move.error) {
                                    MoveMoneyError.Invalid -> stringResource(R.string.feature_piggybanks_error_amount)
                                    is MoveMoneyError.Insufficient -> stringResource(
                                        R.string.feature_piggybanks_error_insufficient,
                                        formatMoney(error.available, summary.currencyCode),
                                    )
                                    null -> stringResource(R.string.feature_piggybanks_available, formatMoney(available, summary.currencyCode))
                                },
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                },
                confirmButton = { TextButton(onClick = viewModel::confirm) { Text(stringResource(R.string.feature_piggybanks_confirm)) } },
                dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.feature_piggybanks_cancel)) } },
            )
        }
    }
}
