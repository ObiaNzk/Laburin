package com.example.laburin.data.notifications

import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.NotificationEvent
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.repository.Notifier
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeApplicantsRepository
import com.example.laburin.fakes.FakeApplicationRepository
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeMyOffersRepository
import com.example.laburin.fakes.testApplication
import com.example.laburin.fakes.testPublishedOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsCheckerTest {

    private val auth = FakeAuthRepository(CANDIDATE_SESSION)
    private val applications = FakeApplicationRepository()
    private val myOffers = FakeMyOffersRepository()
    private val applicants = FakeApplicantsRepository()
    private val notifier = RecordingNotifier()
    private val known = InMemoryKnownApplicants()
    private val checker = NotificationsChecker(auth, applications, myOffers, applicants, known, notifier)

    @Test
    fun `a candidate is notified when the server reviewed an application`() = runTest {
        applications.store.value = listOf(testApplication("a-1", title = "Vendedor/a", status = ApplicationStatus.SENT))
        applications.onRefresh = { applications.store.value = listOf(testApplication("a-1", title = "Vendedor/a", status = ApplicationStatus.APPROVED)) }

        checker.check()

        assertEquals(listOf(NotificationEvent.ApplicationReviewed("a-1", "Vendedor/a", approved = true)), notifier.events)
    }

    @Test
    fun `a business is notified about new applicants once`() = runTest {
        auth.session.value = BUSINESS_SESSION
        myOffers.store.value = listOf(testPublishedOffer("o-1", title = "Vendedor/a", status = PublishedOfferStatus.ACTIVE))
        applicants.store.value = listOf(applicant("a-1"))

        checker.check()
        checker.check()

        assertEquals(listOf(NotificationEvent.NewApplicants("o-1", "Vendedor/a", 1)), notifier.events)
    }

    @Test
    fun `logged out nothing is checked`() = runTest {
        auth.session.value = null

        checker.check()

        assertEquals(0, applications.refreshCalls)
        assertEquals(emptyList<NotificationEvent>(), notifier.events)
    }

    private fun applicant(id: String) = Applicant(id, "[NOMBRE]", "cv.pdf", ApplicantStatus.PENDING_REVIEW, null)

    private class RecordingNotifier : Notifier {
        val events = mutableListOf<NotificationEvent>()
        override fun notify(event: NotificationEvent) { events += event }
    }

    private class InMemoryKnownApplicants : KnownApplicantsStore {
        private val known = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
        override suspend fun known(offerId: String) = known.first()[offerId].orEmpty()
        override suspend fun remember(offerId: String, applicationIds: Set<String>) {
            known.value = known.value + (offerId to applicationIds)
        }
    }
}
