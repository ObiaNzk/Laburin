package com.example.laburin.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.domain.model.SyncOutcome
import com.example.laburin.domain.repository.SyncScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** An expired session is not retried in a loop: the actions stay queued and login schedules a new send. */
fun SyncOutcome.toWorkResult(): ListenableWorker.Result = when (this) {
    SyncOutcome.Done, SyncOutcome.AuthRequired -> ListenableWorker.Result.success()
    SyncOutcome.Retry -> ListenableWorker.Result.retry()
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: PendingActionsSyncer,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = syncer.run().toWorkResult()
}

class WorkManagerSyncScheduler @Inject constructor(@param:ApplicationContext private val context: Context) : SyncScheduler {

    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        // A send already running may have read the queue before a new action arrived, so chain one more run.
        WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    companion object {
        const val UNIQUE_NAME = "send-pending-actions"
        private const val BACKOFF_SECONDS = 10L
    }
}
