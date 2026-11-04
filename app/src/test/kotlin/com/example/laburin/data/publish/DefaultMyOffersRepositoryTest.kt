package com.example.laburin.data.publish

import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.data.auth.StoredSession
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.data.remote.OfferDto
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.FakePendingActionDao
import com.example.laburin.fakes.FakePublishedOfferDao
import com.example.laburin.fakes.FakeSessionStore
import com.example.laburin.fakes.ImmediateTransactionRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DefaultMyOffersRepositoryTest {

    private val dao = FakePublishedOfferDao()
    private val pending = FakePendingActionDao()
    private val remote = FakeBusinessRemote()
    private val session = FakeSessionStore(StoredSession("token-business", BUSINESS_SESSION))
    private var nextId = 0
    private val repository = DefaultMyOffersRepository(
        dao, pending, remote, session, ImmediateTransactionRunner,
        Clock.fixed(Instant.parse("2026-11-02T10:00:00Z"), ZoneOffset.UTC), idGenerator = { "id-${nextId++}" },
    )
    private val draft = OfferDraft("  Mozo ", Category.GASTRONOMY, "Sábados", "Atención en salón")

    @Test
    fun `publishing stores a pending offer and queues it`() = runTest {
        val offer = repository.publish(draft)

        assertEquals("id-0", offer.id)
        assertEquals("Mozo", offer.title)
        assertEquals(PublishedOfferStatus.PENDING_PUBLISH, offer.status)
        assertEquals(listOf(PendingActionsSyncer.PUBLISH to "id-0"), pending.rows.value.map { it.type to it.targetId })
        assertEquals(listOf(offer), repository.observeMine().first())
    }

    @Test
    fun `refresh brings the business's offers and statuses from the server`() = runTest {
        remote.mine = { listOf(dto("srv-1", "ACTIVE"), dto("srv-2", "CLOSED")) }

        repository.refreshMine()

        assertEquals(
            mapOf("srv-1" to PublishedOfferStatus.ACTIVE, "srv-2" to PublishedOfferStatus.CLOSED),
            repository.observeMine().first().associate { it.id to it.status },
        )
    }

    @Test
    fun `refresh never overwrites an offer still waiting to be published`() = runTest {
        val offer = repository.publish(draft)
        remote.mine = { emptyList() }

        repository.refreshMine()

        assertEquals(PublishedOfferStatus.PENDING_PUBLISH, repository.observe(offer.id).first()?.status)
    }

    // --- Edit and close (RF-03) ---

    private val changes = OfferDraft(" Mozo/a ", Category.GASTRONOMY, " Viernes y sábados ", " Atención en salón y caja ")

    private fun stored(id: String, status: PublishedOfferStatus, owner: String = BUSINESS_SESSION.userId) = PublishedOfferEntity(
        id, owner, "Mozo", "GASTRONOMY", "Sábados", "Atención en salón", status.name, Instant.parse("2026-11-01T10:00:00Z"),
    )

    @Test
    fun `editing an active offer stores the changes as pending and queues them`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.ACTIVE))

        repository.edit("p-1", changes)

        val offer = repository.observe("p-1").first()!!
        assertEquals(PublishedOfferStatus.EDIT_PENDING_SEND, offer.status)
        assertEquals(listOf("Mozo/a", "Viernes y sábados", "Atención en salón y caja"), listOf(offer.title, offer.schedule, offer.description))
        assertEquals(listOf(PendingActionsSyncer.EDIT to "p-1"), pending.rows.value.map { it.type to it.targetId })
    }

    @Test
    fun `editing an offer still waiting to be published keeps it pending publish and queues the changes`() = runTest {
        val offer = repository.publish(draft)

        repository.edit(offer.id, changes)

        assertEquals(PublishedOfferStatus.PENDING_PUBLISH, repository.observe(offer.id).first()?.status)
        assertEquals("Mozo/a", repository.observe(offer.id).first()?.title)
        assertEquals(
            listOf(PendingActionsSyncer.PUBLISH, PendingActionsSyncer.EDIT),
            pending.rows.value.filter { it.targetId == offer.id }.map { it.type },
        )
    }

    @Test
    fun `another business's offer is never edited`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.ACTIVE, owner = "otro-comercio"))

        repository.edit("p-1", changes)

        assertEquals("Mozo", dao.find("p-1")?.title)
        assertEquals(emptyList<Any>(), pending.rows.value)
    }

    @Test
    fun `closing an offer marks it as pending close and queues it`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.ACTIVE))

        repository.close("p-1")

        assertEquals(PublishedOfferStatus.CLOSE_PENDING_SEND, repository.observe("p-1").first()?.status)
        assertEquals(listOf(PendingActionsSyncer.CLOSE to "p-1"), pending.rows.value.map { it.type to it.targetId })
    }

    @Test
    fun `closing an offer that is already closed does nothing`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.CLOSED))

        repository.close("p-1")

        assertEquals(PublishedOfferStatus.CLOSED, repository.observe("p-1").first()?.status)
        assertEquals(emptyList<Any>(), pending.rows.value)
    }

    @Test
    fun `another business's offer is never closed`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.ACTIVE, owner = "otro-comercio"))

        repository.close("p-1")

        assertEquals(PublishedOfferStatus.ACTIVE.name, dao.find("p-1")?.status)
        assertEquals(emptyList<Any>(), pending.rows.value)
    }

    @Test
    fun `refresh never overwrites unsent changes or an unsent close`() = runTest {
        dao.insert(stored("p-1", PublishedOfferStatus.EDIT_PENDING_SEND))
        dao.insert(stored("p-2", PublishedOfferStatus.CLOSE_PENDING_SEND))
        remote.mine = { listOf(dto("p-1", "ACTIVE"), dto("p-2", "ACTIVE")) }

        repository.refreshMine()

        assertEquals(PublishedOfferStatus.EDIT_PENDING_SEND, repository.observe("p-1").first()?.status)
        assertEquals("Mozo", repository.observe("p-1").first()?.title)
        assertEquals(PublishedOfferStatus.CLOSE_PENDING_SEND, repository.observe("p-2").first()?.status)
    }

    private fun dto(id: String, status: String) = OfferDto(
        id = id, title = "Puesto $id", category = "STOCK", description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = 0.0, longitude = 0.0, status = status,
        updatedAt = "2026-11-01T12:00:00Z",
    )
}

class FakeBusinessRemote : BusinessRemoteDataSource {
    var publishResponse: () -> Unit = {}
    val publishCalls = mutableListOf<String>()
    val sentOffers = mutableListOf<PublishOfferDto>()
    var closeResponse: () -> Unit = {}
    val closeCalls = mutableListOf<String>()
    override suspend fun closeOffer(token: String, offerId: String) {
        closeCalls += "$token:$offerId"
        closeResponse()
    }
    var mine: () -> List<OfferDto> = { emptyList() }
    override suspend fun publishOffer(token: String, offer: PublishOfferDto) {
        publishCalls += "$token:${offer.id}"
        sentOffers += offer
        publishResponse()
    }
    override suspend fun myOffers(token: String) = mine()

    var applicantsResponse: () -> List<ApplicantDto> = { emptyList() }
    var reviewResponse: (String, Boolean) -> ApplicantDto = { id, approve ->
        ApplicantDto(id, "o-1", "[NOMBRE]", "cv.pdf", if (approve) "APPROVED" else "REJECTED", if (approve) "[EMAIL]" else null)
    }
    val reviewCalls = mutableListOf<String>()
    override suspend fun applicants(token: String, offerId: String) = applicantsResponse()
    override suspend fun review(token: String, applicationId: String, approve: Boolean): ApplicantDto {
        reviewCalls += "$applicationId:$approve"
        return reviewResponse(applicationId, approve)
    }
}
