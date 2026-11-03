package com.example.laburin.domain.notifications

import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.NotificationEvent
import com.example.laburin.fakes.testApplication
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationRulesTest {

    @Test
    fun `an application that became approved notifies the candidate`() {
        val before = listOf(testApplication("a-1", title = "Vendedor/a", status = ApplicationStatus.SENT))
        val after = listOf(testApplication("a-1", title = "Vendedor/a", status = ApplicationStatus.APPROVED))

        assertEquals(
            listOf(NotificationEvent.ApplicationReviewed("a-1", "Vendedor/a", approved = true)),
            NotificationRules.applicationChanges(before, after),
        )
    }

    @Test
    fun `a rejection also notifies`() {
        val before = listOf(testApplication("a-1", status = ApplicationStatus.SENT))
        val after = listOf(testApplication("a-1", status = ApplicationStatus.REJECTED))

        assertEquals(false, (NotificationRules.applicationChanges(before, after).single() as NotificationEvent.ApplicationReviewed).approved)
    }

    @Test
    fun `nothing changed, or a status that was already known, notifies nothing`() {
        val approved = listOf(testApplication("a-1", status = ApplicationStatus.APPROVED))

        assertEquals(emptyList<NotificationEvent>(), NotificationRules.applicationChanges(approved, approved))
    }

    @Test
    fun `other status changes are not worth a notification`() {
        val before = listOf(testApplication("a-1", status = ApplicationStatus.PENDING_SEND))
        val after = listOf(testApplication("a-1", status = ApplicationStatus.SENT))

        assertEquals(emptyList<NotificationEvent>(), NotificationRules.applicationChanges(before, after))
    }

    @Test
    fun `new applicants are counted per offer`() {
        assertEquals(
            NotificationEvent.NewApplicants("o-1", "Vendedor/a", count = 2),
            NotificationRules.newApplicants("o-1", "Vendedor/a", knownIds = setOf("a-1"), currentIds = setOf("a-1", "a-2", "a-3")),
        )
    }

    @Test
    fun `no new applicants, no notification`() {
        assertEquals(null, NotificationRules.newApplicants("o-1", "Vendedor/a", setOf("a-1"), setOf("a-1")))
    }
}
