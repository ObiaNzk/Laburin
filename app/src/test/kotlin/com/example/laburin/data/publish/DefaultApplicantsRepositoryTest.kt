package com.example.laburin.data.publish

import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.data.auth.StoredSession
import com.example.laburin.data.local.ApplicantDao
import com.example.laburin.data.local.ApplicantEntity
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.FakePendingActionDao
import com.example.laburin.fakes.FakeSessionStore
import com.example.laburin.fakes.ImmediateTransactionRunner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DefaultApplicantsRepositoryTest {

    private val dao = FakeApplicantDao()
    private val pending = FakePendingActionDao()
    private val remote = FakeBusinessRemote()
    private val session = FakeSessionStore(StoredSession("token-business", BUSINESS_SESSION))
    private val repository = DefaultApplicantsRepository(
        dao, pending, remote, session, ImmediateTransactionRunner, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC), idGenerator = { "q-1" },
    )

    @Test
    fun `refresh stores who applied, with the contact only when the server sends it`() = runTest {
        remote.applicantsResponse = {
            listOf(
                ApplicantDto("a-1", "o-1", "[NOMBRE] 1", "cv1.pdf", "PENDING_REVIEW", contactEmail = null),
                ApplicantDto("a-2", "o-1", "[NOMBRE] 2", "cv2.pdf", "APPROVED", contactEmail = "[EMAIL]"),
            )
        }

        repository.refresh("o-1")

        assertEquals(
            listOf(
                Applicant("a-1", "[NOMBRE] 1", "cv1.pdf", ApplicantStatus.PENDING_REVIEW, contactEmail = null),
                Applicant("a-2", "[NOMBRE] 2", "cv2.pdf", ApplicantStatus.APPROVED, contactEmail = "[EMAIL]"),
            ),
            repository.observeForOffer("o-1").first(),
        )
    }

    @Test
    fun `approving is stored as pending and queued, without revealing the contact`() = runTest {
        dao.upsert(ApplicantEntity("a-1", "o-1", "[NOMBRE]", "cv.pdf", "PENDING_REVIEW", null))

        repository.review("a-1", approve = true)

        val applicant = repository.observeForOffer("o-1").first().single()
        assertEquals(ApplicantStatus.APPROVAL_PENDING_SEND, applicant.status)
        assertEquals(null, applicant.contactEmail)
        assertEquals(listOf(PendingActionsSyncer.REVIEW to "a-1"), pending.rows.value.map { it.type to it.targetId })
    }

    @Test
    fun `rejecting is stored as pending and queued`() = runTest {
        dao.upsert(ApplicantEntity("a-1", "o-1", "[NOMBRE]", "cv.pdf", "PENDING_REVIEW", null))

        repository.review("a-1", approve = false)

        assertEquals(ApplicantStatus.REJECTION_PENDING_SEND, repository.observeForOffer("o-1").first().single().status)
    }

    @Test
    fun `refresh never overwrites a decision that is still waiting to be sent`() = runTest {
        dao.upsert(ApplicantEntity("a-1", "o-1", "[NOMBRE]", "cv.pdf", "PENDING_REVIEW", null))
        repository.review("a-1", approve = true)
        remote.applicantsResponse = { listOf(ApplicantDto("a-1", "o-1", "[NOMBRE]", "cv.pdf", "PENDING_REVIEW", null)) }

        repository.refresh("o-1")

        assertEquals(ApplicantStatus.APPROVAL_PENDING_SEND, repository.observeForOffer("o-1").first().single().status)
    }
}

class FakeApplicantDao : ApplicantDao {
    val rows = MutableStateFlow<List<ApplicantEntity>>(emptyList())
    override fun observeForOffer(offerId: String) = rows.map { l -> l.filter { it.offerId == offerId }.sortedBy { it.candidateName } }
    override suspend fun find(applicationId: String) = rows.value.firstOrNull { it.applicationId == applicationId }
    override suspend fun upsert(applicant: ApplicantEntity) {
        rows.value = rows.value.filterNot { it.applicationId == applicant.applicationId } + applicant
    }
}
