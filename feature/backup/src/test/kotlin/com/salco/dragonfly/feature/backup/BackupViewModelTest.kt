package com.salco.dragonfly.feature.backup

import com.salco.dragonfly.core.data.repository.BackupRepository
import com.salco.dragonfly.core.data.repository.InvalidBackupException
import com.salco.dragonfly.core.data.repository.ParsedBackup
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BackupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeDocuments : DocumentAccess {
        val written = mutableMapOf<String, ByteArrayOutputStream>()
        val files = mutableMapOf<String, String>()
        val persisted = mutableSetOf<String>()
        var scheduled = 0
        var cancelled = 0

        override fun openOutput(uri: String): OutputStream = ByteArrayOutputStream().also { written[uri] = it }
        override fun openInput(uri: String): InputStream? = files[uri]?.let { ByteArrayInputStream(it.toByteArray()) }
        override fun persistFolderAccess(uri: String) { persisted += uri }
        override fun releaseFolderAccess(uri: String) { persisted -= uri }
        override fun folderName(uri: String) = "Drive/Backup"
        override fun scheduleAutoBackup() { scheduled++ }
        override fun cancelAutoBackup() { cancelled++ }
    }

    /** Writes "backup" or "csv"; reads only files whose content is "valid". */
    private class FakeBackupRepository : BackupRepository {
        var restored = false
        override suspend fun writeBackup(output: OutputStream) = output.write("backup".toByteArray())
        override suspend fun writeTransactionsCsv(output: OutputStream) = output.write("csv".toByteArray())
        override suspend fun readBackup(input: InputStream): ParsedBackup = throw InvalidBackupException("Not a Dragonfly backup")
        override suspend fun restoreBackup(backup: ParsedBackup) { restored = true }
    }

    private val documents = FakeDocuments()
    private val repository = FakeBackupRepository()
    private val prefs = FakeUserDataRepository()
    private val clock = Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC)
    // Created lazily so viewModelScope starts after the test dispatcher is installed.
    private val viewModel by lazy { BackupViewModel(repository, prefs, documents, clock) }

    @Test
    fun exports_writeTheChosenFilesAndRememberTheBackupTime() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals("dragonfly-backup-2026-10-18.json", viewModel.backupName())
        viewModel.exportBackup("content://backup")
        viewModel.exportCsv("content://csv")
        assertEquals("backup", documents.written.getValue("content://backup").toString())
        assertEquals("csv", documents.written.getValue("content://csv").toString())
        assertEquals(clock.millis(), prefs.userPreferences.first().lastBackupAt)
        assertEquals(BackupMessage.CsvExported, viewModel.uiState.value.message)
    }

    @Test
    fun invalidRestore_showsAnErrorAndRestoresNothing() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        documents.files["content://bad"] = "{}"
        viewModel.chooseRestore("content://bad")
        assertEquals(BackupMessage.InvalidFile, viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.pendingRestore)
        assertFalse(repository.restored)

        viewModel.messageShown()
        viewModel.chooseRestore("content://missing")
        assertEquals(BackupMessage.IoError, viewModel.uiState.value.message)
    }

    @Test
    fun autoBackup_isEnabledAndDisabled() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.enableAutoBackup("content://tree")
        assertTrue("content://tree" in documents.persisted)
        assertEquals(1, documents.scheduled)
        assertEquals("content://tree", prefs.userPreferences.first().autoBackupFolder)

        viewModel.disableAutoBackup()
        assertEquals(1, documents.cancelled)
        assertTrue(documents.persisted.isEmpty())
        assertNull(prefs.userPreferences.first().autoBackupFolder)
    }
}
