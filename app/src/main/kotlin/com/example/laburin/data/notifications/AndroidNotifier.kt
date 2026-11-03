package com.example.laburin.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.laburin.MainActivity
import com.example.laburin.R
import com.example.laburin.domain.model.NotificationEvent
import com.example.laburin.domain.repository.Notifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Shows local notifications for both sources: the periodic check and FCM pushes. */
class AndroidNotifier @Inject constructor(@param:ApplicationContext private val context: Context) : Notifier {

    override fun notify(event: NotificationEvent) {
        if (!canNotify()) return
        createChannel()
        val (id, title, text) = when (event) {
            is NotificationEvent.ApplicationReviewed -> Triple(
                event.applicationId.hashCode(),
                context.getString(if (event.approved) R.string.notif_approved_title else R.string.notif_rejected_title),
                context.getString(if (event.approved) R.string.notif_approved_text else R.string.notif_rejected_text, event.offerTitle),
            )
            is NotificationEvent.NewApplicants -> Triple(
                event.offerId.hashCode(),
                context.getString(R.string.notif_new_applicants_title),
                context.resources.getQuantityString(R.plurals.notif_new_applicants_text, event.count, event.count, event.offerTitle),
            )
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // Checked in canNotify().
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "application-updates"
    }
}
