package com.salco.dragonfly.feature.backup

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfLinearLoading
import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSectionHeader
import com.salco.dragonfly.core.designsystem.component.DfSwitch
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun BackupScreen(onBack: () -> Unit, onImportCsv: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val exportBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportBackup(it.toString()) }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportCsv(it.toString()) }
    }
    // Some file managers label JSON as octet-stream or plain text, so accept those too.
    val chooseRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.chooseRestore(it.toString()) }
    }
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.enableAutoBackup(it.toString()) }
    }

    DfScaffold(
        title = stringResource(R.string.feature_backup_title),
        onBack = onBack,
        message = state.message?.let { stringResource(it.textRes()) },
        onMessageShown = viewModel::messageShown,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.busy) DfLinearLoading()
            DfText(
                stringResource(R.string.feature_backup_intro),
                tone = DfTone.Secondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            DfSectionHeader(stringResource(R.string.feature_backup_section_backup))
            ActionItem(
                icon = Icons.Outlined.Backup,
                title = stringResource(R.string.feature_backup_export),
                description = stringResource(R.string.feature_backup_export_description),
                enabled = !state.busy,
                onClick = { exportBackup.launch(viewModel.backupName()) },
            )
            ActionItem(
                icon = Icons.Outlined.Restore,
                title = stringResource(R.string.feature_backup_restore),
                description = stringResource(R.string.feature_backup_restore_description),
                enabled = !state.busy,
                onClick = { chooseRestore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            )

            DfSectionHeader(stringResource(R.string.feature_backup_section_auto))
            DfListItem(
                headline = stringResource(R.string.feature_backup_auto),
                icon = Icons.Outlined.Schedule,
                supportingContent = {
                    Column {
                        DfText(
                            state.autoBackupFolder?.let { stringResource(R.string.feature_backup_auto_folder, it) }
                                ?: stringResource(R.string.feature_backup_auto_off),
                            style = DfTheme.typography.bodySmall,
                            tone = DfTone.Secondary,
                        )
                        DfText(
                            state.lastBackupAt?.let {
                                stringResource(
                                    R.string.feature_backup_last,
                                    it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                                )
                            } ?: stringResource(R.string.feature_backup_never),
                            style = DfTheme.typography.bodySmall,
                            tone = DfTone.Secondary,
                        )
                    }
                },
                trailing = {
                    DfSwitch(
                        checked = state.autoBackupFolder != null,
                        onCheckedChange = { on -> if (on) chooseFolder.launch(null) else viewModel.disableAutoBackup() },
                    )
                },
            )
            if (state.autoBackupFolder != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    DfButton(stringResource(R.string.feature_backup_run_now), viewModel::runAutoBackupNow, style = DfButtonStyle.Text)
                }
            }

            DfSectionHeader(stringResource(R.string.feature_backup_section_export))
            ActionItem(
                icon = Icons.Outlined.FileUpload,
                title = stringResource(R.string.feature_backup_import_csv),
                description = stringResource(R.string.feature_backup_import_csv_description),
                enabled = !state.busy,
                onClick = onImportCsv,
            )
            ActionItem(
                icon = Icons.Outlined.TableChart,
                title = stringResource(R.string.feature_backup_csv),
                description = stringResource(R.string.feature_backup_csv_description),
                enabled = !state.busy,
                onClick = { exportCsv.launch(viewModel.csvName()) },
            )

            DfText(
                stringResource(R.string.feature_backup_privacy),
                style = DfTheme.typography.caption,
                tone = DfTone.Tertiary,
                modifier = Modifier.padding(20.dp),
            )
        }
    }

    state.pendingRestore?.let { summary ->
        DfConfirmDialog(
            title = stringResource(R.string.feature_backup_restore_title),
            message = stringResource(
                R.string.feature_backup_restore_message,
                summary.exportedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                summary.accounts,
                summary.transactions,
                summary.budgets,
                summary.bills,
                summary.piggyBanks,
                summary.rules,
            ),
            confirmText = stringResource(R.string.feature_backup_restore_confirm),
            dismissText = stringResource(R.string.feature_backup_cancel),
            onConfirm = viewModel::confirmRestore,
            onDismiss = viewModel::cancelRestore,
        )
    }
}

private fun BackupMessage.textRes(): Int = when (this) {
    BackupMessage.Exported -> R.string.feature_backup_done_export
    BackupMessage.CsvExported -> R.string.feature_backup_done_csv
    BackupMessage.Restored -> R.string.feature_backup_done_restore
    BackupMessage.AutoBackupScheduled -> R.string.feature_backup_done_scheduled
    BackupMessage.InvalidFile -> R.string.feature_backup_error_invalid
    BackupMessage.NewerVersion -> R.string.feature_backup_error_newer
    BackupMessage.IoError -> R.string.feature_backup_error_io
}

@Composable
private fun ActionItem(icon: ImageVector, title: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    DfListItem(headline = title, supporting = description, icon = icon, onClick = onClick, enabled = enabled)
}
