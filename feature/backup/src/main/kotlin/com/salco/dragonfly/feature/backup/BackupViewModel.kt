package com.salco.dragonfly.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BackupRepository
import com.salco.dragonfly.core.data.repository.BackupSummary
import com.salco.dragonfly.core.data.repository.InvalidBackupException
import com.salco.dragonfly.core.data.repository.ParsedBackup
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.data.repository.backupFileName
import com.salco.dragonfly.core.data.repository.csvFileName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BackupViewModel internal constructor(
    private val backupRepository: BackupRepository,
    private val userDataRepository: UserDataRepository,
    private val documents: DocumentAccess,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        backupRepository: BackupRepository,
        userDataRepository: UserDataRepository,
        documents: DocumentAccess,
    ) : this(backupRepository, userDataRepository, documents, Clock.systemDefaultZone())

    private val _work = MutableStateFlow(WorkState())

    val uiState: StateFlow<BackupUiState> = combine(userDataRepository.userPreferences, _work) { prefs, work ->
        BackupUiState(
            autoBackupFolder = prefs.autoBackupFolder?.let { documents.folderName(it) ?: it },
            lastBackupAt = prefs.lastBackupAt?.let(Instant::ofEpochMilli),
            busy = work.busy,
            message = work.message,
            pendingRestore = work.pendingRestore?.summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    /** Suggested names for the files the user is asked to create. */
    fun backupName(): String = backupFileName(LocalDate.now(clock))
    fun csvName(): String = csvFileName(LocalDate.now(clock))

    fun exportBackup(uri: String) = run(BackupMessage.Exported) {
        write(uri) { backupRepository.writeBackup(it) }
        userDataRepository.setLastBackupAt(clock.millis())
    }

    fun exportCsv(uri: String) = run(BackupMessage.CsvExported) {
        write(uri) { backupRepository.writeTransactionsCsv(it) }
    }

    /** Reads the chosen file and asks for confirmation before replacing anything. */
    fun chooseRestore(uri: String) = run(message = null) {
        val parsed = documents.openInput(uri)?.use { backupRepository.readBackup(it) } ?: throw IOException("Cannot open $uri")
        _work.update { it.copy(pendingRestore = parsed) }
    }

    fun confirmRestore() {
        val backup = _work.value.pendingRestore ?: return
        _work.update { it.copy(pendingRestore = null) }
        run(BackupMessage.Restored) { backupRepository.restoreBackup(backup) }
    }

    fun cancelRestore() = _work.update { it.copy(pendingRestore = null) }

    fun enableAutoBackup(folderUri: String) = run(BackupMessage.AutoBackupScheduled) {
        documents.persistFolderAccess(folderUri)
        userDataRepository.setAutoBackupFolder(folderUri)
        documents.scheduleAutoBackup()
    }

    fun disableAutoBackup() = run(message = null) {
        val folder = userDataRepository.userPreferences.first().autoBackupFolder
        documents.cancelAutoBackup()
        userDataRepository.setAutoBackupFolder(null)
        folder?.let(documents::releaseFolderAccess)
    }

    fun runAutoBackupNow() {
        documents.scheduleAutoBackup()
        _work.update { it.copy(message = BackupMessage.AutoBackupScheduled) }
    }

    fun messageShown() = _work.update { it.copy(message = null) }

    private suspend fun write(uri: String, block: suspend (java.io.OutputStream) -> Unit) {
        documents.openOutput(uri)?.use { block(it) } ?: throw IOException("Cannot open $uri")
    }

    private fun run(message: BackupMessage?, block: suspend () -> Unit) {
        viewModelScope.launch {
            _work.update { it.copy(busy = true) }
            val result = try {
                block()
                message
            } catch (e: InvalidBackupException) {
                if (e.message?.contains("newer") == true) BackupMessage.NewerVersion else BackupMessage.InvalidFile
            } catch (e: Exception) {
                BackupMessage.IoError
            }
            _work.update { it.copy(busy = false, message = result ?: it.message) }
        }
    }

    private data class WorkState(
        val busy: Boolean = false,
        val message: BackupMessage? = null,
        val pendingRestore: ParsedBackup? = null,
    )
}

enum class BackupMessage { Exported, CsvExported, Restored, AutoBackupScheduled, InvalidFile, NewerVersion, IoError }

data class BackupUiState(
    /** Display name of the automatic backup folder, or null when automatic backups are off. */
    val autoBackupFolder: String? = null,
    val lastBackupAt: Instant? = null,
    val busy: Boolean = false,
    /** Outcome of the last operation, shown once. */
    val message: BackupMessage? = null,
    /** Backup waiting for the user's confirmation before replacing the data. */
    val pendingRestore: BackupSummary? = null,
)
