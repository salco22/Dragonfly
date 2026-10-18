package com.salco.dragonfly.feature.budgets.edit

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DropdownField
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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.feature_budgets_new else R.string.feature_budgets_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_budgets_back))
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_budgets_delete))
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
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.feature_budgets_name)) },
                isError = uiState.nameError != null,
                supportingText = when (uiState.nameError) {
                    NameError.BLANK -> { { Text(stringResource(R.string.feature_budgets_error_name)) } }
                    NameError.DUPLICATE -> { { Text(stringResource(R.string.feature_budgets_error_duplicate)) } }
                    null -> null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.feature_budgets_amount)) },
                suffix = { Text(uiState.currencyCode) },
                isError = uiState.amountError,
                supportingText = {
                    Text(
                        stringResource(
                            if (uiState.amountError) R.string.feature_budgets_error_amount else R.string.feature_budgets_amount_hint,
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownField(
                label = stringResource(R.string.feature_budgets_currency),
                options = SupportedCurrencyCodes,
                selected = uiState.currencyCode,
                optionLabel = { it },
                onSelected = onCurrencyChange,
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feature_budgets_active), modifier = Modifier.weight(1f))
                Switch(checked = uiState.active, onCheckedChange = onActiveChange)
            }
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_budgets_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_budgets_delete_title)) },
            text = { Text(stringResource(R.string.feature_budgets_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(R.string.feature_budgets_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_budgets_cancel)) }
            },
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
