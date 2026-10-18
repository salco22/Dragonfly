package com.salco.dragonfly.feature.piggybanks.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.DatePickerField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.piggybanks.R

@Composable
internal fun PiggyBankEditScreen(onBack: () -> Unit, viewModel: PiggyBankEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_piggybanks_new else R.string.feature_piggybanks_edit),
        onBack = onBack,
        actions = {
            if (!uiState.isNew) DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_piggybanks_delete), { confirmDelete = true })
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        if (uiState.accounts.isEmpty()) {
            DfEmptyState(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(R.string.feature_piggybanks_error_account),
                message = stringResource(R.string.feature_piggybanks_no_accounts),
                modifier = Modifier.padding(padding),
            )
            return@DfScaffold
        }
        DfFormColumn(Modifier.padding(padding)) {
            DfTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.feature_piggybanks_name),
                keyboard = DfKeyboard.Sentences,
                error = when (uiState.nameError) {
                    NameError.BLANK -> stringResource(R.string.feature_piggybanks_error_name)
                    NameError.DUPLICATE -> stringResource(R.string.feature_piggybanks_error_duplicate)
                    null -> null
                },
            )
            DropdownField(
                label = stringResource(R.string.feature_piggybanks_account),
                options = uiState.accounts,
                selected = uiState.account,
                optionLabel = { "${it.name} · ${formatMoney(it.balance, it.currencyCode)}" },
                onSelected = { viewModel.onAccountChange(it.id) },
                isError = uiState.accountError,
            )
            DfTextField(
                value = uiState.target,
                onValueChange = viewModel::onTargetChange,
                label = stringResource(R.string.feature_piggybanks_target),
                suffix = uiState.account?.currencyCode.orEmpty(),
                keyboard = DfKeyboard.Decimal,
                large = true,
                error = stringResource(R.string.feature_piggybanks_error_target).takeIf { uiState.targetError },
            )
            DatePickerField(
                label = stringResource(R.string.feature_piggybanks_start_date),
                date = uiState.startDate,
                onDateChange = viewModel::onStartDateChange,
            )
            DatePickerField(
                label = stringResource(R.string.feature_piggybanks_target_date),
                date = uiState.targetDate,
                onDateChange = viewModel::onTargetDateChange,
                onClear = { viewModel.onTargetDateChange(null) },
            )
            DfTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.feature_piggybanks_notes),
                singleLine = false,
                minLines = 2,
            )
            DfButton(stringResource(R.string.feature_piggybanks_save), viewModel::save, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_piggybanks_delete_title),
            message = stringResource(R.string.feature_piggybanks_delete_message),
            confirmText = stringResource(R.string.feature_piggybanks_delete),
            dismissText = stringResource(R.string.feature_piggybanks_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
