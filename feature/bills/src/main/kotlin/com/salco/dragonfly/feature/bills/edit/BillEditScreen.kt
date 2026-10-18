package com.salco.dragonfly.feature.bills.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
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
import androidx.compose.material.icons.outlined.Delete
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
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.core.ui.unitLabel
import com.salco.dragonfly.feature.bills.R

@Composable
internal fun BillEditScreen(onBack: () -> Unit, viewModel: BillEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_bills_new else R.string.feature_bills_edit),
        onBack = onBack,
        actions = {
            if (!uiState.isNew) DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_bills_delete), { confirmDelete = true })
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        DfFormColumn(Modifier.padding(padding)) {
            DfTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.feature_bills_name),
                keyboard = DfKeyboard.Sentences,
                error = when (uiState.nameError) {
                    NameError.BLANK -> stringResource(R.string.feature_bills_error_name)
                    NameError.DUPLICATE -> stringResource(R.string.feature_bills_error_duplicate)
                    null -> null
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DfTextField(
                        value = uiState.amountMin,
                        onValueChange = viewModel::onAmountMinChange,
                        label = stringResource(R.string.feature_bills_amount_min),
                        suffix = uiState.currencyCode,
                        keyboard = DfKeyboard.Decimal,
                        error = "".takeIf { uiState.amountError == AmountError.INVALID },
                        modifier = Modifier.weight(1f),
                    )
                    DfTextField(
                        value = uiState.amountMax,
                        onValueChange = viewModel::onAmountMaxChange,
                        label = stringResource(R.string.feature_bills_amount_max),
                        suffix = uiState.currencyCode,
                        keyboard = DfKeyboard.Decimal,
                        error = "".takeIf { uiState.amountError == AmountError.RANGE },
                        modifier = Modifier.weight(1f),
                    )
                }
                DfText(
                    stringResource(
                        when (uiState.amountError) {
                            AmountError.INVALID -> R.string.feature_bills_error_amount
                            AmountError.RANGE -> R.string.feature_bills_error_range
                            null -> R.string.feature_bills_amount_max_hint
                        },
                    ),
                    style = DfTheme.typography.caption,
                    tone = if (uiState.amountError != null) DfTone.Negative else DfTone.Secondary,
                )
            }
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
                DfTextField(
                    value = uiState.interval,
                    onValueChange = viewModel::onIntervalChange,
                    label = stringResource(R.string.feature_bills_every),
                    keyboard = DfKeyboard.Number,
                    error = "".takeIf { uiState.intervalError },
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
            DfTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.feature_bills_notes),
                singleLine = false,
                minLines = 2,
            )
            DfSwitchRow(stringResource(R.string.feature_bills_active), uiState.active, viewModel::onActiveChange)
            DfButton(stringResource(R.string.feature_bills_save), viewModel::save, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_bills_delete_title),
            message = stringResource(R.string.feature_bills_delete_message),
            confirmText = stringResource(R.string.feature_bills_delete),
            dismissText = stringResource(R.string.feature_bills_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
