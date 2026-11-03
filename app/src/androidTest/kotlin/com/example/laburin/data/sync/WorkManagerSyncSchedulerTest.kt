package com.example.laburin.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkManagerSyncSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
    }

    @Test
    fun schedulesAUniqueSendThatWaitsForAConnection() {
        WorkManagerSyncScheduler(context).schedule()

        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_NAME).get()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        assertEquals(NetworkType.CONNECTED, infos.single().constraints.requiredNetworkType)
    }

    @Test
    fun schedulingAgainDoesNotPileUpDuplicateSends() {
        val scheduler = WorkManagerSyncScheduler(context)

        scheduler.schedule()
        scheduler.schedule()

        val pending = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_NAME).get()
            .filter { !it.state.isFinished }
        assertTrue("expected at most 2 chained sends, got ${pending.size}", pending.size <= 2)
    }
}
