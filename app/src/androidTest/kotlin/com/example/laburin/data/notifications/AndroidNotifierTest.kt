package com.example.laburin.data.notifications

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.laburin.domain.model.NotificationEvent
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidNotifierTest {

    @get:Rule
    val permission: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)

    @After
    fun tearDown() = manager.cancelAll()

    @Test
    fun anApprovedApplicationIsAnnounced() {
        AndroidNotifier(context).notify(NotificationEvent.ApplicationReviewed("a-1", "Vendedor/a", approved = true))

        assertTrue(texts().any { it.contains("Vendedor/a") && it.contains("aprobaron") })
    }

    @Test
    fun newApplicantsAreAnnounced() {
        AndroidNotifier(context).notify(NotificationEvent.NewApplicants("o-1", "Mozo", count = 2))

        assertTrue(texts().any { it.contains("2 postulantes nuevos") && it.contains("Mozo") })
    }

    private fun texts(): List<String> = manager.activeNotifications.map { sbn ->
        val extras = sbn.notification.extras
        "${extras.getCharSequence("android.title")} ${extras.getCharSequence("android.text")}"
    }
}
