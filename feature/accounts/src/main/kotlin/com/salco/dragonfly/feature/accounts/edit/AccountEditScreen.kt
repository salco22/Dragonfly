package com.salco.dragonfly.feature.accounts.edit

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DropdownField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountEditContent(uiState: AccountEditUiState, actions: AccountEditActions) {
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.feature_accounts_new else R.string.feature_accounts_edit))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_accounts_back))
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_accounts_delete))
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
                onValueChange = actions.onNameChange,
                label = { Text(stringResource(R.string.feature_accounts_name)) },
                isError = uiState.nameError,
                supportingText = if (uiState.nameError) {
                    { Text(stringResource(R.string.feature_accounts_error_name)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            DropdownField(
                label = stringResource(R.string.feature_accounts_type),
                options = AccountType.entries,
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
                    val kinds = LiabilityType.entries
                    Text(stringResource(R.string.feature_accounts_liability_kind))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        kinds.forEachIndexed { index, kind ->
                            SegmentedButton(
                                selected = uiState.liabilityType == kind,
                                onClick = { actions.onLiabilityTypeChange(kind) },
                                shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                            ) { Text(stringResource(kind.labelRes())) }
                        }
                    }
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
                OutlinedTextField(
                    value = uiState.openingBalance,
                    onValueChange = actions.onOpeningBalanceChange,
                    label = {
                        Text(
                            stringResource(
                                if (uiState.type == AccountType.LIABILITY) R.string.feature_accounts_debt_amount
                                else R.string.feature_accounts_opening_balance,
                            ),
                        )
                    },
                    suffix = { Text(uiState.currencyCode) },
                    isError = uiState.amountError,
                    supportingText = if (uiState.amountError) {
                        { Text(stringResource(R.string.feature_accounts_error_amount)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.type == AccountType.ASSET) {
                OutlinedTextField(
                    value = uiState.iban,
                    onValueChange = actions.onIbanChange,
                    label = { Text(stringResource(R.string.feature_accounts_iban)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = actions.onNotesChange,
                label = { Text(stringResource(R.string.feature_accounts_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            SwitchRow(stringResource(R.string.feature_accounts_active), uiState.active, actions.onActiveChange)
            if (uiState.type.hasBalance) {
                SwitchRow(
                    stringResource(R.string.feature_accounts_include_net_worth),
                    uiState.includeInNetWorth,
                    actions.onIncludeInNetWorthChange,
                )
            }

            Button(onClick = actions.onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_accounts_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_accounts_delete_title)) },
            text = { Text(stringResource(R.string.feature_accounts_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete()
                }) { Text(stringResource(R.string.feature_accounts_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.feature_accounts_cancel))
                }
            },
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
