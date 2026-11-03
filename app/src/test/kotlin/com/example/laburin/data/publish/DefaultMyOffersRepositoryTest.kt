package com.example.laburin.data.publish

import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.data.auth.StoredSession
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

    private fun dto(id: String, status: String) = OfferDto(
        id = id, title = "Puesto $id", category = "STOCK", description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = 0.0, longitude = 0.0, status = status,
        updatedAt = "2026-11-01T12:00:00Z",
    )
}

class FakeBusinessRemote : BusinessRemoteDataSource {
    var publishResponse: () -> Unit = {}
    val publishCalls = mutableListOf<String>()
    var mine: () -> List<OfferDto> = { emptyList() }
    override suspend fun publishOffer(token: String, offer: PublishOfferDto) {
        publishCalls += "$token:${offer.id}"
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
