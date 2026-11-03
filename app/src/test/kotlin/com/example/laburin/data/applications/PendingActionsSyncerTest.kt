package com.example.laburin.data.applications

import com.example.laburin.data.local.ApplicationEntity
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.data.publish.FakeBusinessRemote
import com.example.laburin.fakes.FakePublishedOfferDao
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.SyncOutcome
import com.example.laburin.fakes.CANDIDATE_STORED
import com.example.laburin.fakes.FakeApplicationDao
import com.example.laburin.fakes.FakePendingActionDao
import com.example.laburin.fakes.FakeSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.Instant

class PendingActionsSyncerTest {

    private val applications = FakeApplicationDao()
    private val pending = FakePendingActionDao()
    private val remote = FakeApplicationsRemote()
    private val session = FakeSessionStore(CANDIDATE_STORED)
    private val published = FakePublishedOfferDao()
    private val business = FakeBusinessRemote()
    private val applicants = com.example.laburin.data.publish.FakeApplicantDao()
    private val syncer = PendingActionsSyncer(pending, applications, remote, session, published, business, applicants)

    @Before
    fun setUp() = runTest {
        queueApply("a-1", "o-1", at = 1)
    }

    @Test
    fun `nothing queued is done`() = runTest {
        pending.rows.value = emptyList()

        assertEquals(SyncOutcome.Done, syncer.run())
    }

    @Test
    fun `a sent application is confirmed and leaves the queue`() = runTest {
        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("SENT", applications.statusOf("a-1"))
        assertTrue(pending.rows.value.isEmpty())
        assertEquals(listOf("token-candidate:a-1:o-1"), remote.applyCalls)
    }

    @Test
    fun `a closed offer marks the application closed and leaves the queue`() = runTest {
        remote.applyResponse = { throw ApiException(409) }

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("OFFER_CLOSED", applications.statusOf("a-1"))
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `no network keeps everything to retry later`() = runTest {
        remote.applyResponse = { throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())

        assertEquals("PENDING_SEND", applications.statusOf("a-1"))
        assertEquals(1, pending.rows.value.size)
    }

    @Test
    fun `server errors keep everything to retry with backoff`() = runTest {
        remote.applyResponse = { throw ApiException(503) }

        assertEquals(SyncOutcome.Retry, syncer.run())
        assertEquals(1, pending.rows.value.size)
    }

    @Test
    fun `an expired session keeps the actions until the user logs in again`() = runTest {
        remote.applyResponse = { throw ApiException(401) }

        assertEquals(SyncOutcome.AuthRequired, syncer.run())
        assertEquals(1, pending.rows.value.size)
    }

    @Test
    fun `without a session nothing is sent and nothing is lost`() = runTest {
        session.saved.value = null

        assertEquals(SyncOutcome.AuthRequired, syncer.run())
        assertTrue(remote.applyCalls.isEmpty())
        assertEquals(1, pending.rows.value.size)
    }

    @Test
    fun `actions are sent oldest first and stop at the first network failure`() = runTest {
        queueApply("a-2", "o-2", at = 2)
        queueApply("a-3", "o-3", at = 3)
        var calls = 0
        remote.applyResponse = { if (++calls == 2) throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())

        assertEquals(listOf("SENT", "PENDING_SEND", "PENDING_SEND"), listOf("a-1", "a-2", "a-3").map(applications::statusOf))
        assertEquals(listOf("p-a-2", "p-a-3"), pending.rows.value.map { it.id })
    }

    @Test
    fun `an action whose application no longer exists is dropped`() = runTest {
        applications.rows.value = emptyList()

        assertEquals(SyncOutcome.Done, syncer.run())
        assertTrue(pending.rows.value.isEmpty())
        assertTrue(remote.applyCalls.isEmpty())
    }

    @Test
    fun `a published offer becomes active once the server accepts it`() = runTest {
        pending.rows.value = emptyList()
        published.insert(PublishedOfferEntity("p-1", "b-1", "Mozo", "GASTRONOMY", "Sábados", "[DESCRIPCION]", "PENDING_PUBLISH", Instant.EPOCH))
        pending.insert(PendingActionEntity("q-1", PendingActionsSyncer.PUBLISH, "p-1", Instant.EPOCH))

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("ACTIVE", published.find("p-1")?.status)
        assertEquals(listOf("token-candidate:p-1"), business.publishCalls)
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `publishing without network stays pending`() = runTest {
        pending.rows.value = emptyList()
        published.insert(PublishedOfferEntity("p-1", "b-1", "Mozo", "GASTRONOMY", "Sábados", "[DESCRIPCION]", "PENDING_PUBLISH", Instant.EPOCH))
        pending.insert(PendingActionEntity("q-1", PendingActionsSyncer.PUBLISH, "p-1", Instant.EPOCH))
        business.publishResponse = { throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())
        assertEquals("PENDING_PUBLISH", published.find("p-1")?.status)
    }

    @Test
    fun `an approval confirmed by the server reveals the contact`() = runTest {
        queueReview("a-9", status = "APPROVAL_PENDING_SEND")

        assertEquals(SyncOutcome.Done, syncer.run())

        val applicant = applicants.find("a-9")!!
        assertEquals("APPROVED", applicant.status)
        assertEquals("[EMAIL]", applicant.contactEmail)
        assertEquals(listOf("a-9:true"), business.reviewCalls)
    }

    @Test
    fun `a rejection confirmed by the server keeps the contact hidden`() = runTest {
        queueReview("a-9", status = "REJECTION_PENDING_SEND")

        syncer.run()

        assertEquals("REJECTED", applicants.find("a-9")!!.status)
        assertEquals(null, applicants.find("a-9")!!.contactEmail)
        assertEquals(listOf("a-9:false"), business.reviewCalls)
    }

    @Test
    fun `a review without network stays pending and hidden`() = runTest {
        queueReview("a-9", status = "APPROVAL_PENDING_SEND")
        business.reviewResponse = { _, _ -> throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())
        assertEquals("APPROVAL_PENDING_SEND", applicants.find("a-9")!!.status)
        assertEquals(null, applicants.find("a-9")!!.contactEmail)
    }

    private suspend fun queueReview(applicationId: String, status: String) {
        pending.rows.value = emptyList()
        applicants.upsert(com.example.laburin.data.local.ApplicantEntity(applicationId, "o-1", "[NOMBRE]", "cv.pdf", status, null))
        pending.insert(PendingActionEntity("r-$applicationId", PendingActionsSyncer.REVIEW, applicationId, Instant.EPOCH))
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is never swallowed`() = runTest {
        remote.applyResponse = { throw CancellationException("cancelled") }
        syncer.run()
    }

    private suspend fun queueApply(appId: String, offerId: String, at: Long) {
        applications.insert(
            ApplicationEntity(appId, "u-1", offerId, "Mozo", "[COMERCIO]", "PENDING_SEND", Instant.ofEpochMilli(at))
        )
        pending.insert(PendingActionEntity("p-$appId", PendingActionsSyncer.APPLY, appId, Instant.ofEpochMilli(at)))
    }
}

class FakeApplicationsRemote : ApplicationsRemoteDataSource {
    var applyResponse: () -> Unit = {}
    val applyCalls = mutableListOf<String>()
    var mine: () -> List<ApplicationDto> = { emptyList() }
    var cv: () -> CvDto? = { null }
    var uploadResponse: () -> Unit = {}

    override suspend fun apply(token: String, applicationId: String, offerId: String) {
        applyCalls += "$token:$applicationId:$offerId"
        applyResponse()
    }

    override suspend fun myApplications(token: String) = mine()
    override suspend fun uploadCv(token: String, fileName: String, bytes: ByteArray) = uploadResponse()
    override suspend fun getCv(token: String) = cv()
}
