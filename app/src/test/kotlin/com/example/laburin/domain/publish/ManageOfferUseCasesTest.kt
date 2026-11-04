package com.example.laburin.domain.publish

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.EditResult
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeMyOffersRepository
import com.example.laburin.fakes.FakeSyncScheduler
import com.example.laburin.fakes.testPublishedOffer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditOfferUseCaseTest {

    private val auth = FakeAuthRepository(BUSINESS_SESSION)
    private val offers = FakeMyOffersRepository()
    private val scheduler = FakeSyncScheduler()
    private val useCase = EditOfferUseCase(auth, offers, scheduler)
    private val draft = OfferDraft("Mozo/a", Category.GASTRONOMY, "Viernes y sábados", "Atención en salón")

    @Test
    fun `only a business account can edit`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))
        auth.session.value = CANDIDATE_SESSION

        assertEquals(EditResult.NeedsBusinessAccount, useCase("p-1", draft))
        assertTrue(offers.edits.isEmpty())
    }

    @Test
    fun `an unknown offer cannot be edited`() = runTest {
        assertEquals(EditResult.NotFound, useCase("missing", draft))
    }

    @Test
    fun `a closed offer cannot be edited`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.CLOSED))

        assertEquals(EditResult.Closed, useCase("p-1", draft))
        assertTrue(offers.edits.isEmpty())
    }

    @Test
    fun `an offer being closed cannot be edited`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.CLOSE_PENDING_SEND))

        assertEquals(EditResult.Closed, useCase("p-1", draft))
    }

    @Test
    fun `incomplete changes are rejected with the missing fields`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))

        val result = useCase("p-1", draft.copy(schedule = " ", description = ""))

        assertEquals(EditResult.Invalid(setOf(PublishField.SCHEDULE, PublishField.DESCRIPTION)), result)
        assertTrue(offers.edits.isEmpty())
    }

    @Test
    fun `valid changes are queued to be sent`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))

        assertEquals(EditResult.Queued, useCase("p-1", draft))
        assertEquals(listOf("p-1" to draft), offers.edits)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `an offer still waiting to be published can be edited`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.PENDING_PUBLISH))

        assertEquals(EditResult.Queued, useCase("p-1", draft))
    }
}

class CloseOfferUseCaseTest {

    private val offers = FakeMyOffersRepository()
    private val scheduler = FakeSyncScheduler()
    private val useCase = CloseOfferUseCase(offers, scheduler)

    @Test
    fun `closing an active offer queues the close`() = runTest {
        offers.store.value = listOf(testPublishedOffer("p-1", status = PublishedOfferStatus.ACTIVE))

        useCase("p-1")

        assertEquals(listOf("p-1"), offers.closes)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `closing an offer that is already closed or being closed does nothing`() = runTest {
        offers.store.value = listOf(
            testPublishedOffer("p-1", status = PublishedOfferStatus.CLOSED),
            testPublishedOffer("p-2", status = PublishedOfferStatus.CLOSE_PENDING_SEND),
        )

        useCase("p-1")
        useCase("p-2")

        assertTrue(offers.closes.isEmpty())
        assertEquals(0, scheduler.scheduled)
    }

    @Test
    fun `closing an unknown offer does nothing`() = runTest {
        useCase("missing")

        assertTrue(offers.closes.isEmpty())
    }
}
