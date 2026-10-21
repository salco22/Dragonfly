package com.salco.dragonfly.feature.accounts.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.LiabilityType
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.feature.accounts.R
import com.salco.dragonfly.feature.accounts.hasBalance
import com.salco.dragonfly.feature.accounts.labelRes

@Composable
internal fun AccountEditScreen(
    onBack: () -> Unit,
    viewModel: AccountEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    AccountEditContent(
        uiState = uiState,
        actions = AccountEditActions(
            onBack = onBack,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onNameChange = viewModel::onNameChange,
            onTypeChange = viewModel::onTypeChange,
            onAssetRoleChange = viewModel::onAssetRoleChange,
            onLiabilityTypeChange = viewModel::onLiabilityTypeChange,
            onCurrencyChange = viewModel::onCurrencyChange,
            onOpeningBalanceChange = viewModel::onOpeningBalanceChange,
            onIbanChange = viewModel::onIbanChange,
            onNotesChange = viewModel::onNotesChange,
            onActiveChange = viewModel::onActiveChange,
            onIncludeInNetWorthChange = viewModel::onIncludeInNetWorthChange,
        ),
    )
}

internal data class AccountEditActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onNameChange: (String) -> Unit = {},
    val onTypeChange: (AccountType) -> Unit = {},
    val onAssetRoleChange: (AssetRole) -> Unit = {},
    val onLiabilityTypeChange: (LiabilityType) -> Unit = {},
    val onCurrencyChange: (String) -> Unit = {},
    val onOpeningBalanceChange: (String) -> Unit = {},
    val onIbanChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onActiveChange: (Boolean) -> Unit = {},
    val onIncludeInNetWorthChange: (Boolean) -> Unit = {},
)

@Composable
internal fun AccountEditContent(uiState: AccountEditUiState, actions: AccountEditActions) {
    var confirmDelete by remember { mutableStateOf(false) }

    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_accounts_new else R.string.feature_accounts_edit),
        onBack = actions.onBack,
        actions = {
            if (!uiState.isNew) {
                DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_accounts_delete), { confirmDelete = true })
            }
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        DfFormColumn(Modifier.padding(padding)) {
            DfTextField(
                value = uiState.name,
                onValueChange = actions.onNameChange,
                label = stringResource(R.string.feature_accounts_name),
                error = stringResource(R.string.feature_accounts_error_name).takeIf { uiState.nameError },
            )
            DropdownField(
                label = stringResource(R.string.feature_accounts_type),
                // Payees are not managed by the user; an old one keeps its type while edited.
                options = listOf(AccountType.ASSET, AccountType.LIABILITY).let { own -> if (uiState.type in own) own else own + uiState.type },
                selected = uiState.type,
                optionLabel = { stringResource(it.labelRes()) },
                onSelected = actions.onTypeChange,
            )
            when (uiState.type) {
                AccountType.ASSET -> DropdownField(
                    label = stringResource(R.string.feature_accounts_role),
                    options = AssetRole.entries,
                    selected = uiState.assetRole,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = actions.onAssetRoleChange,
                )
                AccountType.LIABILITY -> {
                    DfText(stringResource(R.string.feature_accounts_liability_kind), style = DfTheme.typography.label, tone = DfTone.Secondary)
                    DfSegmentedControl(
                        options = LiabilityType.entries,
                        selected = uiState.liabilityType,
                        optionLabel = { stringResource(it.labelRes()) },
                        onSelect = actions.onLiabilityTypeChange,
                    )
                }
                else -> Unit
            }
            DropdownField(
                label = stringResource(R.string.feature_accounts_currency),
                options = SupportedCurrencyCodes,
                selected = uiState.currencyCode,
                optionLabel = { it },
                onSelected = actions.onCurrencyChange,
            )
            if (uiState.type.hasBalance) {
                DfTextField(
                    value = uiState.openingBalance,
                    onValueChange = actions.onOpeningBalanceChange,
                    label = stringResource(
                        if (uiState.type == AccountType.LIABILITY) R.string.feature_accounts_debt_amount
                        else R.string.feature_accounts_opening_balance,
                    ),
                    suffix = uiState.currencyCode,
                    keyboard = DfKeyboard.Decimal,
                    error = stringResource(R.string.feature_accounts_error_amount).takeIf { uiState.amountError },
                )
            }
            if (uiState.type == AccountType.ASSET) {
                DfTextField(
                    value = uiState.iban,
                    onValueChange = actions.onIbanChange,
                    label = stringResource(R.string.feature_accounts_iban),
                    keyboard = DfKeyboard.Characters,
                )
            }
            DfTextField(
                value = uiState.notes,
                onValueChange = actions.onNotesChange,
                label = stringResource(R.string.feature_accounts_notes),
                singleLine = false,
                minLines = 2,
            )
            DfSwitchRow(stringResource(R.string.feature_accounts_active), uiState.active, actions.onActiveChange)
            if (uiState.type.hasBalance) {
                DfSwitchRow(
                    stringResource(R.string.feature_accounts_include_net_worth),
                    uiState.includeInNetWorth,
                    actions.onIncludeInNetWorthChange,
                )
            }
            DfButton(stringResource(R.string.feature_accounts_save), actions.onSave, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_accounts_delete_title),
            message = stringResource(R.string.feature_accounts_delete_message),
            confirmText = stringResource(R.string.feature_accounts_delete),
            dismissText = stringResource(R.string.feature_accounts_cancel),
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Preview
@Composable
private fun AccountEditContentPreview() {
    DragonflyTheme {
        AccountEditContent(
            uiState = AccountEditUiState(isNew = false, name = "Conto corrente", openingBalance = "1520.30"),
            actions = AccountEditActions(),
        )
    }
}
