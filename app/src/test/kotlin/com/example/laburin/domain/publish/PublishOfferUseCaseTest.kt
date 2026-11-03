package com.example.laburin.domain.publish

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishResult
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeMyOffersRepository
import com.example.laburin.fakes.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublishOfferUseCaseTest {

    private val auth = FakeAuthRepository(BUSINESS_SESSION)
    private val offers = FakeMyOffersRepository()
    private val scheduler = FakeSyncScheduler()
    private val useCase = PublishOfferUseCase(auth, offers, scheduler)
    private val draft = OfferDraft("Mozo", Category.GASTRONOMY, "Sábados", "Atención en salón")

    @Test
    fun `without a session a business account is needed`() = runTest {
        auth.session.value = null

        assertEquals(PublishResult.NeedsBusinessAccount, useCase(draft))
    }

    @Test
    fun `a candidate account cannot publish`() = runTest {
        auth.session.value = CANDIDATE_SESSION

        assertEquals(PublishResult.NeedsBusinessAccount, useCase(draft))
    }

    @Test
    fun `incomplete drafts are rejected with the missing fields`() = runTest {
        val result = useCase(draft.copy(title = "", category = null))

        assertEquals(PublishResult.Invalid(setOf(PublishField.TITLE, PublishField.CATEGORY)), result)
        assertTrue(offers.published.isEmpty())
    }

    @Test
    fun `a valid draft is queued for publishing`() = runTest {
        val result = useCase(draft)

        assertEquals(PublishResult.Queued(offers.published.single()), result)
        assertEquals(1, scheduler.scheduled)
    }
}
