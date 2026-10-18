package com.salco.dragonfly.feature.budgets.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSwitchRow
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DropdownField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.feature.budgets.R

@Composable
internal fun BudgetEditScreen(onBack: () -> Unit, viewModel: BudgetEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }
    BudgetEditContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onAmountChange = viewModel::onAmountChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        onActiveChange = viewModel::onActiveChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
internal fun BudgetEditContent(
    uiState: BudgetEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_budgets_new else R.string.feature_budgets_edit),
        onBack = onBack,
        actions = {
            if (!uiState.isNew) DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_budgets_delete), { confirmDelete = true })
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        DfFormColumn(Modifier.padding(padding)) {
            DfTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = stringResource(R.string.feature_budgets_name),
                error = when (uiState.nameError) {
                    NameError.BLANK -> stringResource(R.string.feature_budgets_error_name)
                    NameError.DUPLICATE -> stringResource(R.string.feature_budgets_error_duplicate)
                    null -> null
                },
            )
            DfTextField(
                value = uiState.amount,
                onValueChange = onAmountChange,
                label = stringResource(R.string.feature_budgets_amount),
                suffix = uiState.currencyCode,
                keyboard = DfKeyboard.Decimal,
                large = true,
                error = stringResource(R.string.feature_budgets_error_amount).takeIf { uiState.amountError },
                helper = stringResource(R.string.feature_budgets_amount_hint),
            )
            DropdownField(
                label = stringResource(R.string.feature_budgets_currency),
                options = SupportedCurrencyCodes,
                selected = uiState.currencyCode,
                optionLabel = { it },
                onSelected = onCurrencyChange,
            )
            DfSwitchRow(stringResource(R.string.feature_budgets_active), uiState.active, onActiveChange)
            DfButton(stringResource(R.string.feature_budgets_save), onSave, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_budgets_delete_title),
            message = stringResource(R.string.feature_budgets_delete_message),
            confirmText = stringResource(R.string.feature_budgets_delete),
            dismissText = stringResource(R.string.feature_budgets_cancel),
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Preview
@Composable
private fun BudgetEditContentPreview() {
    DragonflyTheme {
        BudgetEditContent(
            uiState = BudgetEditUiState(isNew = true, name = "Spesa", amount = "400"),
            onBack = {}, onNameChange = {}, onAmountChange = {}, onCurrencyChange = {}, onActiveChange = {},
            onSave = {}, onDelete = {},
        )
    }
}
