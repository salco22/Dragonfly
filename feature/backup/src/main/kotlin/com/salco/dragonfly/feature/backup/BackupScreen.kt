package com.salco.dragonfly.feature.backup

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(context.getString(message.textRes()))
        viewModel.messageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_backup_back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                stringResource(R.string.feature_backup_intro),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )

            SectionTitle(stringResource(R.string.feature_backup_section_backup))
            ActionItem(
                icon = Icons.Filled.Backup,
                title = stringResource(R.string.feature_backup_export),
                description = stringResource(R.string.feature_backup_export_description),
                enabled = !state.busy,
                onClick = { exportBackup.launch(viewModel.backupName()) },
            )
            ActionItem(
                icon = Icons.Filled.Restore,
                title = stringResource(R.string.feature_backup_restore),
                description = stringResource(R.string.feature_backup_restore_description),
                enabled = !state.busy,
                onClick = { chooseRestore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
            )

            SectionTitle(stringResource(R.string.feature_backup_section_auto))
            ListItem(
                headlineContent = { Text(stringResource(R.string.feature_backup_auto)) },
                supportingContent = {
                    Column {
                        Text(
                            state.autoBackupFolder?.let { stringResource(R.string.feature_backup_auto_folder, it) }
                                ?: stringResource(R.string.feature_backup_auto_off),
                        )
                        Text(
                            state.lastBackupAt?.let {
                                stringResource(
                                    R.string.feature_backup_last,
                                    it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                                )
                            } ?: stringResource(R.string.feature_backup_never),
                        )
                    }
                },
                trailingContent = {
                    Switch(
                        checked = state.autoBackupFolder != null,
                        onCheckedChange = { on -> if (on) chooseFolder.launch(null) else viewModel.disableAutoBackup() },
                    )
                },
            )
            if (state.autoBackupFolder != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::runAutoBackupNow) { Text(stringResource(R.string.feature_backup_run_now)) }
                }
            }

            SectionTitle(stringResource(R.string.feature_backup_section_export))
            ActionItem(
                icon = Icons.Filled.TableChart,
                title = stringResource(R.string.feature_backup_csv),
                description = stringResource(R.string.feature_backup_csv_description),
                enabled = !state.busy,
                onClick = { exportCsv.launch(viewModel.csvName()) },
            )

            Text(
                stringResource(R.string.feature_backup_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    state.pendingRestore?.let { summary ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.feature_backup_restore_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.feature_backup_restore_message,
                        summary.exportedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        summary.accounts,
                        summary.transactions,
                        summary.budgets,
                        summary.bills,
                        summary.piggyBanks,
                        summary.rules,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmRestore) {
                    Text(stringResource(R.string.feature_backup_restore_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.feature_backup_cancel)) } },
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
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
