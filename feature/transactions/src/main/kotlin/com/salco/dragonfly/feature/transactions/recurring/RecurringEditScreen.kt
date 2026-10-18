package com.salco.dragonfly.feature.transactions.recurring

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.feature.transactions.R
import com.salco.dragonfly.feature.transactions.edit.DatePickerField
import com.salco.dragonfly.feature.transactions.edit.TransactionEditActions
import com.salco.dragonfly.feature.transactions.edit.TransactionFormFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecurringEditScreen(onBack: () -> Unit, viewModel: RecurringEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isNew) R.string.feature_transactions_recurring_new else R.string.feature_transactions_recurring_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_transactions_back))
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_transactions_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        if (uiState.ownAccounts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AccountBalance,
                title = stringResource(R.string.feature_transactions_no_accounts_title),
                message = stringResource(R.string.feature_transactions_no_accounts_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TransactionFormFields(
                uiState = uiState,
                actions = TransactionEditActions(
                    onTypeChange = viewModel::onTypeChange,
                    onAmountChange = viewModel::onAmountChange,
                    onForeignAmountChange = viewModel::onForeignAmountChange,
                    onDescriptionChange = viewModel::onDescriptionChange,
                    onDateChange = viewModel::onDateChange,
                    onAccountChange = viewModel::onAccountChange,
                    onToAccountChange = viewModel::onToAccountChange,
                    onCounterpartyChange = viewModel::onCounterpartyChange,
                    onNotesChange = viewModel::onNotesChange,
                    onCategoryChange = viewModel::onCategoryChange,
                    onBudgetChange = viewModel::onBudgetChange,
                    onTagInputChange = viewModel::onTagInputChange,
                    onAddTag = viewModel::addTag,
                    onRemoveTag = viewModel::removeTag,
                ),
                dateLabel = stringResource(R.string.feature_transactions_recurring_start),
            )

            HorizontalDivider()
            Text(stringResource(R.string.feature_transactions_recurring_repeat), style = MaterialTheme.typography.titleSmall)
            ScheduleFields(schedule, viewModel)

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_transactions_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_transactions_recurring_delete_title)) },
            text = { Text(stringResource(R.string.feature_transactions_recurring_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.feature_transactions_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_transactions_cancel)) }
            },
        )
    }
}

@Composable
private fun ScheduleFields(schedule: ScheduleState, viewModel: RecurringEditViewModel) {
    val interval = schedule.interval.toIntOrNull() ?: 1
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = schedule.interval,
            onValueChange = viewModel::onIntervalChange,
            label = { Text(stringResource(R.string.feature_transactions_recurring_every)) },
            isError = schedule.intervalError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        DropdownField(
            label = stringResource(R.string.feature_transactions_recurring_unit),
            options = RecurrenceFrequency.entries,
            selected = schedule.frequency,
            optionLabel = { unitLabel(it, interval) },
            onSelected = viewModel::onFrequencyChange,
            modifier = Modifier.weight(2f),
        )
    }

    val modes = EndMode.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = schedule.end == mode,
                onClick = { viewModel.onEndModeChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, modes.size),
            ) {
                Text(
                    stringResource(
                        when (mode) {
                            EndMode.NEVER -> R.string.feature_transactions_recurring_end_never
                            EndMode.UNTIL -> R.string.feature_transactions_recurring_end_until
                            EndMode.TIMES -> R.string.feature_transactions_recurring_end_times
                        },
                    ),
                )
            }
        }
    }
    when (schedule.end) {
        EndMode.NEVER -> Unit
        EndMode.UNTIL -> DatePickerField(
            label = stringResource(R.string.feature_transactions_recurring_end_date),
            date = schedule.endDate,
            onDateChange = viewModel::onEndDateChange,
        )
        EndMode.TIMES -> OutlinedTextField(
            value = schedule.maxOccurrences,
            onValueChange = viewModel::onMaxOccurrencesChange,
            label = { Text(stringResource(R.string.feature_transactions_recurring_occurrences)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    if (schedule.endError) {
        Text(stringResource(R.string.feature_transactions_recurring_end_error), color = MaterialTheme.colorScheme.error)
    }
    if (schedule.occurrencesCreated > 0) {
        Text(
            pluralStringResource(R.plurals.feature_transactions_recurring_created, schedule.occurrencesCreated, schedule.occurrencesCreated),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.feature_transactions_recurring_active), modifier = Modifier.weight(1f))
        Switch(checked = schedule.active, onCheckedChange = viewModel::onActiveChange)
    }
}

@Composable
private fun unitLabel(frequency: RecurrenceFrequency, interval: Int): String = pluralStringResource(
    when (frequency) {
        RecurrenceFrequency.DAILY -> R.plurals.feature_transactions_unit_day
        RecurrenceFrequency.WEEKLY -> R.plurals.feature_transactions_unit_week
        RecurrenceFrequency.MONTHLY -> R.plurals.feature_transactions_unit_month
        RecurrenceFrequency.YEARLY -> R.plurals.feature_transactions_unit_year
    },
    interval,
)
