package com.salco.dragonfly.core.work

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salco.dragonfly.core.data.repository.BackupRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.data.repository.backupFileName
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Writes a backup into the folder the user chose, keeping only the most recent ones. */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val userDataRepository: UserDataRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val folderUri = userDataRepository.userPreferences.first().autoBackupFolder ?: return Result.success()
        val folder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(folderUri))
        // The folder may have been deleted or its permission revoked: nothing to retry.
        if (folder == null || !folder.canWrite()) return Result.failure()

        val name = backupFileName(LocalDate.now())
        folder.findFile(name)?.delete()
        val file = folder.createFile("application/json", name) ?: return Result.retry()
        return try {
            applicationContext.contentResolver.openOutputStream(file.uri)?.use { backupRepository.writeBackup(it) }
                ?: return Result.retry()
            pruneOldBackups(folder)
            userDataRepository.setLastBackupAt(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            file.delete()
            Result.retry()
        }
    }

    private fun pruneOldBackups(folder: DocumentFile) {
        folder.listFiles()
            .filter { it.name?.startsWith(BACKUP_PREFIX) == true && it.name?.endsWith(".json") == true }
            .sortedByDescending { it.name }
            .drop(KEPT_BACKUPS)
            .forEach { it.delete() }
    }

    companion object {
        private const val PERIODIC_WORK = "auto-backup-weekly"
        private const val ONE_TIME_WORK = "auto-backup-now"
        private const val BACKUP_PREFIX = "dragonfly-backup-"

        /** Backups kept in the folder; older ones are deleted. */
        const val KEPT_BACKUPS = 5

        /** Backs up now and then about once a week. */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<AutoBackupWorker>(7, TimeUnit.DAYS).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }
    }
}
