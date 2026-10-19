package com.salco.dragonfly.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Creates the recurring transactions that became due. Safe to run any number of times. */
@HiltWorker
class RecurringTransactionsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: RecurringTransactionsRepository,
    private val userDataRepository: UserDataRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val created = repository.generateDueTransactions(LocalDate.now())
        if (userDataRepository.userPreferences.first().recurringNotificationsEnabled) {
            RecurringNotifier(applicationContext).notify(created)
        }
        return Result.success()
    }

    companion object {
        private const val PERIODIC_WORK = "recurring-transactions-daily"
        private const val ONE_TIME_WORK = "recurring-transactions-now"

        /** Runs now (e.g. at app start) and then about once a day, even when the app is closed. */
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniqueWork(
                ONE_TIME_WORK,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<RecurringTransactionsWorker>().build(),
            )
            workManager.enqueueUniquePeriodicWork(
                PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RecurringTransactionsWorker>(1, TimeUnit.DAYS).build(),
            )
        }
    }
}
