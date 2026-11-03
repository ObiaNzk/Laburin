package com.example.laburin.domain.apply

import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.ApplyResult
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.domain.repository.SyncScheduler
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeApplicationRepository
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeCvRepository
import com.example.laburin.fakes.FakeSyncScheduler
import com.example.laburin.fakes.testOffer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ApplyToOfferUseCaseTest {

    private val offer = testOffer(id = "o-1")
    private val auth = FakeAuthRepository(CANDIDATE_SESSION)
    private val cv = FakeCvRepository(CvInfo("cv.pdf"))
    private val applications = FakeApplicationRepository()
    private val scheduler = FakeSyncScheduler()
    private val useCase = ApplyToOfferUseCase(auth, cv, applications, scheduler)

    @Test
    fun `without a session the user must create an account`() = runTest {
        auth.session.value = null

        assertEquals(ApplyResult.NeedsAccount, useCase(offer))
        assertTrue(applications.applied.isEmpty())
    }

    @Test
    fun `businesses cannot apply`() = runTest {
        auth.session.value = BUSINESS_SESSION

        assertEquals(ApplyResult.NotACandidate, useCase(offer))
    }

    @Test
    fun `the CV must be uploaded before applying`() = runTest {
        cv.cv.value = null

        assertEquals(ApplyResult.NeedsCv, useCase(offer))
        assertTrue(applications.applied.isEmpty())
    }

    @Test
    fun `applying queues the application and schedules the send`() = runTest {
        val result = useCase(offer)

        assertEquals(ApplyResult.Queued(applications.applied.single()), result)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `applying twice returns the existing application without duplicating it`() = runTest {
        val first = useCase(offer) as ApplyResult.Queued

        assertEquals(ApplyResult.AlreadyApplied(first.application), useCase(offer))
        assertEquals(1, applications.applied.size)
    }
}
