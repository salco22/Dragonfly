package com.salco.dragonfly.feature.csvimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.model.ColumnRole
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.InvalidReason
import com.salco.dragonfly.core.model.SupportedDatePatterns
import com.salco.dragonfly.core.model.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CsvImportScreen(onBack: () -> Unit, viewModel: CsvImportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFileChosen(it.toString()) }
    }
    val defaultUnnamed = stringResource(R.string.feature_csvimport_unnamed_default)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_csvimport_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_csvimport_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(stringResource(R.string.feature_csvimport_intro), style = MaterialTheme.typography.bodyMedium)
            if (state.accounts.isEmpty()) {
                Text(stringResource(R.string.feature_csvimport_no_accounts), color = MaterialTheme.colorScheme.error)
                return@Column
            }
            OutlinedButton(onClick = { pickFile.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }) {
                Icon(Icons.Filled.FileOpen, contentDescription = null)
                Text(stringResource(R.string.feature_csvimport_pick), modifier = Modifier.padding(start = 8.dp))
            }
            state.fileName?.let { Text(stringResource(R.string.feature_csvimport_file, it), style = MaterialTheme.typography.bodySmall) }
            state.error?.let {
                Text(
                    stringResource(if (it == ImportError.READ) R.string.feature_csvimport_error_read else R.string.feature_csvimport_error_empty),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.roles.isEmpty()) return@Column

            DropdownField(
                label = stringResource(R.string.feature_csvimport_account),
                options = state.accounts,
                selected = state.account,
                optionLabel = { "${it.name} · ${it.currencyCode}" },
                onSelected = { viewModel.onAccountChange(it.id) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.hasHeader, onCheckedChange = viewModel::onHasHeaderChange)
                Text(stringResource(R.string.feature_csvimport_has_header))
            }

            Text(stringResource(R.string.feature_csvimport_columns), style = MaterialTheme.typography.titleSmall)
            val example = state.rows.firstOrNull()?.line?.let { it - 1 }
            state.roles.forEachIndexed { index, role ->
                val name = state.header.getOrNull(index).orEmpty()
                DropdownField(
                    label = stringResource(R.string.feature_csvimport_column, index + 1, name),
                    options = ColumnRole.entries,
                    selected = role,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = { viewModel.onRoleChange(index, it) },
                    supportingText = example?.let { _ -> viewModel.sample(index)?.let { stringResource(R.string.feature_csvimport_example, it) } },
                )
            }
            if (!state.hasRequiredRoles) {
                Text(stringResource(R.string.feature_csvimport_missing_roles), color = MaterialTheme.colorScheme.error)
            }
            DropdownField(
                label = stringResource(R.string.feature_csvimport_date_format),
                options = SupportedDatePatterns,
                selected = state.datePattern,
                optionLabel = { it.lowercase() },
                onSelected = viewModel::onDatePatternChange,
            )
            if (ColumnRole.COUNTERPARTY !in state.roles) {
                OutlinedTextField(
                    value = state.unnamedCounterparty,
                    onValueChange = viewModel::onUnnamedChange,
                    label = { Text(stringResource(R.string.feature_csvimport_unnamed)) },
                    placeholder = { Text(defaultUnnamed) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.skipDuplicates, onCheckedChange = viewModel::onSkipDuplicatesChange)
                Text(stringResource(R.string.feature_csvimport_skip_duplicates))
            }

            if (state.hasRequiredRoles) Preview(state)

            Button(
                onClick = { viewModel.import(defaultUnnamed) },
                enabled = state.toImport > 0 && !state.importing,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.feature_csvimport_import, state.toImport)) }
        }
    }

    state.result?.let { result ->
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text(stringResource(R.string.feature_csvimport_done_title)) },
            text = { Text(stringResource(R.string.feature_csvimport_done_message, result.imported, result.duplicates)) },
            confirmButton = { TextButton(onClick = onBack) { Text(stringResource(R.string.feature_csvimport_done)) } },
        )
    }
}

@Composable
private fun Preview(state: CsvImportUiState) {
    val invalid = state.rows.filterIsInstance<ImportRow.Invalid>()
    val currency = state.account?.currencyCode ?: "EUR"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.feature_csvimport_preview), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(
                    R.string.feature_csvimport_preview_summary,
                    state.validCount - state.duplicateLines.size,
                    state.duplicateLines.size,
                    invalid.size,
                ),
            )
            state.rows.filterIsInstance<ImportRow.Valid>().filter { it.line !in state.duplicateLines }.take(5).forEach { row ->
                val sign = if (row.type == com.salco.dragonfly.core.model.TransactionType.WITHDRAWAL) "−" else "+"
                Text(
                    "${row.date} · ${row.description.take(40)} · $sign${formatMoney(row.amount, currency)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            invalid.take(5).forEach { row ->
                Text(
                    stringResource(R.string.feature_csvimport_invalid_row, row.line, stringResource(row.reason.labelRes())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun ColumnRole.labelRes(): Int = when (this) {
    ColumnRole.IGNORE -> R.string.feature_csvimport_role_ignore
    ColumnRole.DATE -> R.string.feature_csvimport_role_date
    ColumnRole.DESCRIPTION -> R.string.feature_csvimport_role_description
    ColumnRole.AMOUNT -> R.string.feature_csvimport_role_amount
    ColumnRole.INCOME -> R.string.feature_csvimport_role_income
    ColumnRole.EXPENSE -> R.string.feature_csvimport_role_expense
    ColumnRole.TYPE -> R.string.feature_csvimport_role_type
    ColumnRole.COUNTERPARTY -> R.string.feature_csvimport_role_counterparty
    ColumnRole.CATEGORY -> R.string.feature_csvimport_role_category
    ColumnRole.TAGS -> R.string.feature_csvimport_role_tags
    ColumnRole.NOTES -> R.string.feature_csvimport_role_notes
}

private fun InvalidReason.labelRes(): Int = when (this) {
    InvalidReason.DATE -> R.string.feature_csvimport_reason_date
    InvalidReason.AMOUNT -> R.string.feature_csvimport_reason_amount
    InvalidReason.ZERO_AMOUNT -> R.string.feature_csvimport_reason_zero
    InvalidReason.TRANSFER -> R.string.feature_csvimport_reason_transfer
}
