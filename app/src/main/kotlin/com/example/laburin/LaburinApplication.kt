package com.example.laburin

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.laburin.data.notifications.NotificationsWorker
import com.example.laburin.domain.repository.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LaburinApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Actions confirmed in a previous run (app closed offline) are sent as soon as there is a connection.
        syncScheduler.schedule()
        NotificationsWorker.schedule(this)
    }
}
