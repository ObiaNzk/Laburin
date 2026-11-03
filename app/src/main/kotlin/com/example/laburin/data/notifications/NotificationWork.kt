package com.example.laburin.data.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.laburin.domain.repository.Notifier
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Local stand-in for push while there is no backend to send FCM messages. */
@HiltWorker
class NotificationsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checker: NotificationsChecker,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        checker.check()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "check-notifications"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationsWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

/**
 * FCM entry point, ready for the Go API (docs/api/notifications.md). Active only when the app is built with a
 * google-services.json; pushes reuse the same events and notifier as the local check.
 */
@AndroidEntryPoint
class LaburinMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notifier: Notifier

    override fun onMessageReceived(message: RemoteMessage) {
        PushPayload.parse(message.data)?.let(notifier::notify)
    }

    override fun onNewToken(token: String) {
        // To do when the API exists: send the token with POST /me/push-token (docs/api/notifications.md).
        Log.d("LaburinPush", "New FCM token received")
    }
}
