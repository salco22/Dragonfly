package com.salco.dragonfly.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salco.dragonfly.core.data.repository.SyncRepository
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.model.SyncException
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Syncs with the server when signed in; does nothing otherwise. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SyncRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        repository.sync()
        Result.success()
    } catch (e: SyncException) {
        // Only a connection problem is worth retrying; the rest needs the user.
        if (e.error == SyncError.Offline || e.error == SyncError.Server) Result.retry() else Result.failure()
    }

    companion object {
        private const val PERIODIC_WORK = "sync-hourly"
        private const val ONE_TIME_WORK = "sync-soon"

        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Syncs about every hour while online, even when the app is closed. */
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
                    .setConstraints(online)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                    .build(),
            )
        }

        /** Syncs as soon as there is a connection, e.g. when the app opens or goes to the background. */
        fun syncSoon(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(online)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build(),
            )
        }
    }
}
