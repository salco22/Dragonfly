package com.salco.dragonfly.feature.transactions.recurring

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfDivider
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSegmentedControl
import com.salco.dragonfly.core.designsystem.component.DfSwitchRow
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.DatePickerField
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
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
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.ui.unitLabel
import com.salco.dragonfly.feature.transactions.R
import com.salco.dragonfly.feature.transactions.edit.TransactionEditActions
import com.salco.dragonfly.feature.transactions.edit.TransactionFormFields

@Composable
internal fun RecurringEditScreen(onBack: () -> Unit, viewModel: RecurringEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_transactions_recurring_new else R.string.feature_transactions_recurring_edit),
        onBack = onBack,
        actions = {
            if (!uiState.isNew) {
                DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_transactions_delete), { confirmDelete = true })
            }
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        if (uiState.ownAccounts.isEmpty()) {
            DfEmptyState(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(R.string.feature_transactions_no_accounts_title),
                message = stringResource(R.string.feature_transactions_no_accounts_message),
                modifier = Modifier.padding(padding),
            )
            return@DfScaffold
        }
        DfFormColumn(Modifier.padding(padding)) {
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
                    onBillChange = viewModel::onBillChange,
                    onTagInputChange = viewModel::onTagInputChange,
                    onAddTag = viewModel::addTag,
                    onRemoveTag = viewModel::removeTag,
                ),
                dateLabel = stringResource(R.string.feature_transactions_recurring_start),
            )
            DfDivider()
            DfText(stringResource(R.string.feature_transactions_recurring_repeat), style = DfTheme.typography.title)
            ScheduleFields(schedule, viewModel)
            DfButton(stringResource(R.string.feature_transactions_save), viewModel::save, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_transactions_recurring_delete_title),
            message = stringResource(R.string.feature_transactions_recurring_delete_message),
            confirmText = stringResource(R.string.feature_transactions_delete),
            dismissText = stringResource(R.string.feature_transactions_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ScheduleFields(schedule: ScheduleState, viewModel: RecurringEditViewModel) {
    val interval = schedule.interval.toIntOrNull() ?: 1
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        DfTextField(
            value = schedule.interval,
            onValueChange = viewModel::onIntervalChange,
            label = stringResource(R.string.feature_transactions_recurring_every),
            error = if (schedule.intervalError) "" else null,
            keyboard = DfKeyboard.Number,
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
    DfSegmentedControl(
        options = EndMode.entries,
        selected = schedule.end,
        optionLabel = {
            stringResource(
                when (it) {
                    EndMode.NEVER -> R.string.feature_transactions_recurring_end_never
                    EndMode.UNTIL -> R.string.feature_transactions_recurring_end_until
                    EndMode.TIMES -> R.string.feature_transactions_recurring_end_times
                },
            )
        },
        onSelect = viewModel::onEndModeChange,
    )
    when (schedule.end) {
        EndMode.NEVER -> Unit
        EndMode.UNTIL -> DatePickerField(
            label = stringResource(R.string.feature_transactions_recurring_end_date),
            date = schedule.endDate,
            onDateChange = viewModel::onEndDateChange,
        )
        EndMode.TIMES -> DfTextField(
            value = schedule.maxOccurrences,
            onValueChange = viewModel::onMaxOccurrencesChange,
            label = stringResource(R.string.feature_transactions_recurring_occurrences),
            keyboard = DfKeyboard.Number,
        )
    }
    if (schedule.endError) {
        DfText(stringResource(R.string.feature_transactions_recurring_end_error), style = DfTheme.typography.bodySmall, tone = DfTone.Negative)
    }
    if (schedule.occurrencesCreated > 0) {
        DfText(
            pluralStringResource(R.plurals.feature_transactions_recurring_created, schedule.occurrencesCreated, schedule.occurrencesCreated),
            style = DfTheme.typography.bodySmall,
            tone = DfTone.Secondary,
        )
    }
    DfSwitchRow(stringResource(R.string.feature_transactions_recurring_active), schedule.active, viewModel::onActiveChange)
}
