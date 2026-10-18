package com.salco.dragonfly.feature.bills.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DatePickerField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.core.ui.unitLabel
import com.salco.dragonfly.feature.bills.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillEditScreen(onBack: () -> Unit, viewModel: BillEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.feature_bills_new else R.string.feature_bills_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_bills_back)) }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_bills_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.feature_bills_name)) },
                isError = uiState.nameError != null,
                supportingText = when (uiState.nameError) {
                    NameError.BLANK -> { { Text(stringResource(R.string.feature_bills_error_name)) } }
                    NameError.DUPLICATE -> { { Text(stringResource(R.string.feature_bills_error_duplicate)) } }
                    null -> null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.amountMin,
                    onValueChange = viewModel::onAmountMinChange,
                    label = { Text(stringResource(R.string.feature_bills_amount_min)) },
                    suffix = { Text(uiState.currencyCode) },
                    isError = uiState.amountError == AmountError.INVALID,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = uiState.amountMax,
                    onValueChange = viewModel::onAmountMaxChange,
                    label = { Text(stringResource(R.string.feature_bills_amount_max)) },
                    suffix = { Text(uiState.currencyCode) },
                    isError = uiState.amountError == AmountError.RANGE,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                stringResource(
                    when (uiState.amountError) {
                        AmountError.INVALID -> R.string.feature_bills_error_amount
                        AmountError.RANGE -> R.string.feature_bills_error_range
                        null -> R.string.feature_bills_amount_max_hint
                    },
                ),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = if (uiState.amountError != null) {
                    androidx.compose.material3.MaterialTheme.colorScheme.error
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            DropdownField(
                label = stringResource(R.string.feature_bills_currency),
                options = SupportedCurrencyCodes,
                selected = uiState.currencyCode,
                optionLabel = { it },
                onSelected = viewModel::onCurrencyChange,
            )
            DatePickerField(
                label = stringResource(R.string.feature_bills_first_date),
                date = uiState.firstDate,
                onDateChange = viewModel::onFirstDateChange,
            )
            val interval = uiState.interval.toIntOrNull() ?: 1
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.interval,
                    onValueChange = viewModel::onIntervalChange,
                    label = { Text(stringResource(R.string.feature_bills_every)) },
                    isError = uiState.intervalError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = stringResource(R.string.feature_bills_unit),
                    options = RecurrenceFrequency.entries,
                    selected = uiState.frequency,
                    optionLabel = { unitLabel(it, interval) },
                    onSelected = viewModel::onFrequencyChange,
                    modifier = Modifier.weight(2f),
                )
            }
            DatePickerField(
                label = stringResource(R.string.feature_bills_end_date),
                date = uiState.endDate,
                onDateChange = viewModel::onEndDateChange,
                onClear = { viewModel.onEndDateChange(null) },
            )
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.feature_bills_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feature_bills_active), modifier = Modifier.weight(1f))
                Switch(checked = uiState.active, onCheckedChange = viewModel::onActiveChange)
            }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_bills_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_bills_delete_title)) },
            text = { Text(stringResource(R.string.feature_bills_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.feature_bills_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_bills_cancel)) } },
        )
    }
}
