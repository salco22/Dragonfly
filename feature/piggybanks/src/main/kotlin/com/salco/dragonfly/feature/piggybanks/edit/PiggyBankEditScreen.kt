package com.salco.dragonfly.feature.piggybanks.edit

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.salco.dragonfly.core.designsystem.component.DatePickerField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.piggybanks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PiggyBankEditScreen(onBack: () -> Unit, viewModel: PiggyBankEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (uiState.isNew) R.string.feature_piggybanks_new else R.string.feature_piggybanks_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_piggybanks_back)) }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_piggybanks_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        if (uiState.accounts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AccountBalance,
                title = stringResource(R.string.feature_piggybanks_error_account),
                message = stringResource(R.string.feature_piggybanks_no_accounts),
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
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.feature_piggybanks_name)) },
                isError = uiState.nameError != null,
                supportingText = when (uiState.nameError) {
                    NameError.BLANK -> { { Text(stringResource(R.string.feature_piggybanks_error_name)) } }
                    NameError.DUPLICATE -> { { Text(stringResource(R.string.feature_piggybanks_error_duplicate)) } }
                    null -> null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownField(
                label = stringResource(R.string.feature_piggybanks_account),
                options = uiState.accounts,
                selected = uiState.account,
                optionLabel = { "${it.name} · ${formatMoney(it.balance, it.currencyCode)}" },
                onSelected = { viewModel.onAccountChange(it.id) },
                isError = uiState.accountError,
            )
            OutlinedTextField(
                value = uiState.target,
                onValueChange = viewModel::onTargetChange,
                label = { Text(stringResource(R.string.feature_piggybanks_target)) },
                suffix = { Text(uiState.account?.currencyCode.orEmpty()) },
                isError = uiState.targetError,
                supportingText = if (uiState.targetError) {
                    { Text(stringResource(R.string.feature_piggybanks_error_target)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
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
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.feature_piggybanks_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_piggybanks_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_piggybanks_delete_title)) },
            text = { Text(stringResource(R.string.feature_piggybanks_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.feature_piggybanks_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_piggybanks_cancel)) } },
        )
    }
}
