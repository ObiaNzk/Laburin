package com.example.laburin.data.applications

import com.example.laburin.data.local.ApplicationEntity
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.data.publish.FakeBusinessRemote
import com.example.laburin.fakes.FakePublishedOfferDao
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.SyncOutcome
import com.example.laburin.domain.repository.SessionExpiryNotifier
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
    private var expiredNotices = 0
    private val syncer = PendingActionsSyncer(
        pending, applications, remote, session, published, business, applicants,
        sessionExpiry = SessionExpiryNotifier { expiredNotices++ },
    )

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
    fun `an expired session is reported so the user can log in again`() = runTest {
        remote.applyResponse = { throw ApiException(401) }

        syncer.run()

        assertEquals(1, expiredNotices)
    }

    @Test
    fun `another user's queued application is kept and never sent with this token`() = runTest {
        session.saved.value = CANDIDATE_STORED.copy(session = CANDIDATE_STORED.session.copy(userId = "otra-persona"))

        assertEquals(SyncOutcome.Done, syncer.run())

        assertTrue(remote.applyCalls.isEmpty())
        assertEquals(1, pending.rows.value.size)
        assertEquals("PENDING_SEND", applications.statusOf("a-1"))
    }

    @Test
    fun `another business's queued publication is kept and never sent with this token`() = runTest {
        pending.rows.value = emptyList()
        published.insert(PublishedOfferEntity("p-1", "otro-comercio", "Mozo", "GASTRONOMY", "Sábados", "[DESCRIPCION]", "PENDING_PUBLISH", Instant.EPOCH))
        pending.insert(PendingActionEntity("q-1", PendingActionsSyncer.PUBLISH, "p-1", Instant.EPOCH))

        syncer.run()

        assertTrue(business.publishCalls.isEmpty())
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
        published.insert(PublishedOfferEntity("p-1", "u-1", "Mozo", "GASTRONOMY", "Sábados", "[DESCRIPCION]", "PENDING_PUBLISH", Instant.EPOCH))
        pending.insert(PendingActionEntity("q-1", PendingActionsSyncer.PUBLISH, "p-1", Instant.EPOCH))

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("ACTIVE", published.find("p-1")?.status)
        assertEquals(listOf("token-candidate:p-1"), business.publishCalls)
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `publishing without network stays pending`() = runTest {
        pending.rows.value = emptyList()
        published.insert(PublishedOfferEntity("p-1", "u-1", "Mozo", "GASTRONOMY", "Sábados", "[DESCRIPCION]", "PENDING_PUBLISH", Instant.EPOCH))
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

    // --- Edit and close a published offer (RF-03) ---

    private suspend fun storeOffer(status: String, owner: String = "u-1", title: String = "Mozo") =
        published.insert(PublishedOfferEntity("p-1", owner, title, "GASTRONOMY", "Sábados", "[DESCRIPCION]", status, Instant.EPOCH))

    private suspend fun queueOn(type: String, id: String, at: Long) =
        pending.insert(PendingActionEntity(id, type, "p-1", Instant.ofEpochMilli(at)))

    @Test
    fun `an edit sends the offer's current content and the offer becomes active`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("EDIT_PENDING_SEND", title = "Mozo/a")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("ACTIVE", published.find("p-1")?.status)
        assertEquals("Mozo/a", business.sentOffers.single().title)
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `an edit the server rejects because the offer is closed marks it closed`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("EDIT_PENDING_SEND")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)
        business.publishResponse = { throw ApiException(409) }

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("CLOSED", published.find("p-1")?.status)
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `an edit without network stays pending`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("EDIT_PENDING_SEND")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)
        business.publishResponse = { throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())

        assertEquals("EDIT_PENDING_SEND", published.find("p-1")?.status)
        assertEquals(listOf("q-1"), pending.rows.value.map { it.id })
    }

    @Test
    fun `changes made while an edit is being sent stay pending`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("EDIT_PENDING_SEND")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)
        business.publishResponse = {
            // The business saves new changes while the previous ones are on the way.
            kotlinx.coroutines.runBlocking { queueOn(PendingActionsSyncer.EDIT, "q-2", at = 2) }
        }

        syncer.run()

        assertEquals("EDIT_PENDING_SEND", published.find("p-1")?.status)
        assertEquals(listOf("q-2"), pending.rows.value.map { it.id })
    }

    @Test
    fun `an edit queued before a close is not sent`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("CLOSE_PENDING_SEND")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)
        queueOn(PendingActionsSyncer.CLOSE, "q-2", at = 2)

        assertEquals(SyncOutcome.Done, syncer.run())

        assertTrue(business.publishCalls.isEmpty())
        assertEquals(listOf("token-candidate:p-1"), business.closeCalls)
        assertEquals("CLOSED", published.find("p-1")?.status)
    }

    @Test
    fun `a close is sent and the offer becomes closed`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("CLOSE_PENDING_SEND")
        queueOn(PendingActionsSyncer.CLOSE, "q-1", at = 1)

        assertEquals(SyncOutcome.Done, syncer.run())

        assertEquals("CLOSED", published.find("p-1")?.status)
        assertTrue(pending.rows.value.isEmpty())
    }

    @Test
    fun `closing an offer the server already closed or no longer has counts as closed`() = runTest {
        for (code in listOf(404, 409)) {
            pending.rows.value = emptyList()
            published.rows.value = emptyList()
            storeOffer("CLOSE_PENDING_SEND")
            queueOn(PendingActionsSyncer.CLOSE, "q-1", at = 1)
            business.closeResponse = { throw ApiException(code) }

            assertEquals(SyncOutcome.Done, syncer.run())

            assertEquals("CLOSED", published.find("p-1")?.status)
            assertTrue(pending.rows.value.isEmpty())
        }
    }

    @Test
    fun `a close without network stays pending`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("CLOSE_PENDING_SEND")
        queueOn(PendingActionsSyncer.CLOSE, "q-1", at = 1)
        business.closeResponse = { throw IOException("offline") }

        assertEquals(SyncOutcome.Retry, syncer.run())

        assertEquals("CLOSE_PENDING_SEND", published.find("p-1")?.status)
        assertEquals(listOf("q-1"), pending.rows.value.map { it.id })
    }

    @Test
    fun `an expired session stops a close and keeps it queued`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("CLOSE_PENDING_SEND")
        queueOn(PendingActionsSyncer.CLOSE, "q-1", at = 1)
        business.closeResponse = { throw ApiException(401) }

        assertEquals(SyncOutcome.AuthRequired, syncer.run())

        assertEquals(1, expiredNotices)
        assertEquals(listOf("q-1"), pending.rows.value.map { it.id })
    }

    @Test
    fun `a publish answered after the business closed the offer does not reopen it`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("PENDING_PUBLISH")
        queueOn(PendingActionsSyncer.PUBLISH, "q-1", at = 1)
        business.publishResponse = { kotlinx.coroutines.runBlocking { published.updateStatus("p-1", "CLOSE_PENDING_SEND") } }

        syncer.run()

        assertEquals("CLOSE_PENDING_SEND", published.find("p-1")?.status)
    }

    @Test
    fun `another business's edit and close are kept for their owner`() = runTest {
        pending.rows.value = emptyList()
        storeOffer("CLOSE_PENDING_SEND", owner = "otro-comercio")
        queueOn(PendingActionsSyncer.EDIT, "q-1", at = 1)
        queueOn(PendingActionsSyncer.CLOSE, "q-2", at = 2)

        syncer.run()

        assertTrue(business.publishCalls.isEmpty())
        assertTrue(business.closeCalls.isEmpty())
        assertEquals(listOf("q-1", "q-2"), pending.rows.value.map { it.id })
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
