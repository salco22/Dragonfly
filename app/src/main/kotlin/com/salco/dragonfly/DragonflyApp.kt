package com.salco.dragonfly

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.salco.dragonfly.core.work.RecurringTransactionsWorker
import com.salco.dragonfly.core.work.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DragonflyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        RecurringTransactionsWorker.schedule(this)
        SyncWorker.schedulePeriodic(this)
        // Fetch other devices' changes on opening, send ours when leaving.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = SyncWorker.syncSoon(this@DragonflyApp)
                override fun onStop(owner: LifecycleOwner) = SyncWorker.syncSoon(this@DragonflyApp)
            },
        )
    }
}
