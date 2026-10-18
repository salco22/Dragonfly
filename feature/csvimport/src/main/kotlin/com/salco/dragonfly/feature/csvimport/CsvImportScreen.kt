package com.salco.dragonfly.feature.csvimport

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfCheckboxRow
import com.salco.dragonfly.core.designsystem.component.DfDialog
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfLinearLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.ColumnRole
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.InvalidReason
import com.salco.dragonfly.core.model.SupportedDatePatterns
import com.salco.dragonfly.core.model.formatMoney

@Composable
internal fun CsvImportScreen(onBack: () -> Unit, viewModel: CsvImportViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onFileChosen(it.toString()) }
    }
    val defaultUnnamed = stringResource(R.string.feature_csvimport_unnamed_default)

    DfScaffold(title = stringResource(R.string.feature_csvimport_title), onBack = onBack) { padding ->
        DfFormColumn(Modifier.padding(padding)) {
            if (state.importing) DfLinearLoading()
            DfText(stringResource(R.string.feature_csvimport_intro), tone = DfTone.Secondary)
            if (state.accounts.isEmpty()) {
                DfText(stringResource(R.string.feature_csvimport_no_accounts), tone = DfTone.Negative)
                return@DfFormColumn
            }
            DfButton(
                stringResource(R.string.feature_csvimport_pick),
                { pickFile.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) },
                style = DfButtonStyle.Secondary,
                icon = Icons.Outlined.FileOpen,
            )
            state.fileName?.let { DfText(stringResource(R.string.feature_csvimport_file, it), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
            state.error?.let {
                DfText(
                    stringResource(if (it == ImportError.READ) R.string.feature_csvimport_error_read else R.string.feature_csvimport_error_empty),
                    tone = DfTone.Negative,
                )
            }
            if (state.roles.isEmpty()) return@DfFormColumn

            DropdownField(
                label = stringResource(R.string.feature_csvimport_account),
                options = state.accounts,
                selected = state.account,
                optionLabel = { "${it.name} · ${it.currencyCode}" },
                onSelected = { viewModel.onAccountChange(it.id) },
            )
            DfCheckboxRow(stringResource(R.string.feature_csvimport_has_header), state.hasHeader, viewModel::onHasHeaderChange)

            DfText(stringResource(R.string.feature_csvimport_columns).uppercase(), style = DfTheme.typography.overline, tone = DfTone.Tertiary)
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
                DfText(stringResource(R.string.feature_csvimport_missing_roles), tone = DfTone.Negative, style = DfTheme.typography.bodySmall)
            }
            DropdownField(
                label = stringResource(R.string.feature_csvimport_date_format),
                options = SupportedDatePatterns,
                selected = state.datePattern,
                optionLabel = { it.lowercase() },
                onSelected = viewModel::onDatePatternChange,
            )
            if (ColumnRole.COUNTERPARTY !in state.roles) {
                DfTextField(
                    value = state.unnamedCounterparty,
                    onValueChange = viewModel::onUnnamedChange,
                    label = stringResource(R.string.feature_csvimport_unnamed),
                    placeholder = defaultUnnamed,
                )
            }
            DfCheckboxRow(stringResource(R.string.feature_csvimport_skip_duplicates), state.skipDuplicates, viewModel::onSkipDuplicatesChange)

            if (state.hasRequiredRoles) Preview(state)

            DfButton(
                stringResource(R.string.feature_csvimport_import, state.toImport),
                { viewModel.import(defaultUnnamed) },
                enabled = state.toImport > 0 && !state.importing,
                fillWidth = true,
            )
        }
    }

    state.result?.let { result ->
        DfDialog(
            title = stringResource(R.string.feature_csvimport_done_title),
            onDismiss = onBack,
            confirmText = stringResource(R.string.feature_csvimport_done),
            onConfirm = onBack,
        ) {
            DfText(stringResource(R.string.feature_csvimport_done_message, result.imported, result.duplicates), tone = DfTone.Secondary)
        }
    }
}

@Composable
private fun Preview(state: CsvImportUiState) {
    val invalid = state.rows.filterIsInstance<ImportRow.Invalid>()
    val currency = state.account?.currencyCode ?: "EUR"
    DfCard(spacing = 6.dp) {
        DfText(stringResource(R.string.feature_csvimport_preview), style = DfTheme.typography.subtitle)
        DfText(
            stringResource(
                R.string.feature_csvimport_preview_summary,
                state.validCount - state.duplicateLines.size,
                state.duplicateLines.size,
                invalid.size,
            ),
            tone = DfTone.Secondary,
        )
        state.rows.filterIsInstance<ImportRow.Valid>().filter { it.line !in state.duplicateLines }.take(5).forEach { row ->
            val sign = if (row.type == com.salco.dragonfly.core.model.TransactionType.WITHDRAWAL) "−" else "+"
            DfText(
                "${row.date} · ${row.description.take(40)} · $sign${formatMoney(row.amount, currency)}",
                style = DfTheme.typography.bodySmall,
            )
        }
        invalid.take(5).forEach { row ->
            DfText(
                stringResource(R.string.feature_csvimport_invalid_row, row.line, stringResource(row.reason.labelRes())),
                style = DfTheme.typography.bodySmall,
                tone = DfTone.Negative,
            )
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
