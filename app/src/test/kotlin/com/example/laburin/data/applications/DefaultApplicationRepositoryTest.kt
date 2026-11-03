package com.example.laburin.data.applications

import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.fakes.CANDIDATE_STORED
import com.example.laburin.fakes.FakeApplicationDao
import com.example.laburin.fakes.FakePendingActionDao
import com.example.laburin.fakes.FakeSessionStore
import com.example.laburin.fakes.ImmediateTransactionRunner
import com.example.laburin.fakes.testOffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DefaultApplicationRepositoryTest {

    private val now = Instant.parse("2026-11-02T10:00:00Z")
    private val applications = FakeApplicationDao()
    private val pending = FakePendingActionDao()
    private val remote = FakeApplicationsRemote()
    private val session = FakeSessionStore(CANDIDATE_STORED)
    private var nextId = 0
    private val repository = DefaultApplicationRepository(
        applications, pending, remote, session, ImmediateTransactionRunner,
        Clock.fixed(now, ZoneOffset.UTC), idGenerator = { "id-${nextId++}" },
    )

    private val offer = testOffer(id = "o-1", title = "Mozo")

    @Test
    fun `applying stores a pending application and queues its send`() = runTest {
        val application = repository.apply(offer)

        assertEquals(ApplicationStatus.PENDING_SEND, application.status)
        assertEquals("o-1", application.offerId)
        assertEquals(now, application.createdAt)
        assertEquals(listOf(application.id), pending.rows.value.map { it.targetId })
        assertEquals(listOf(application), repository.observeMine().first())
    }

    @Test
    fun `the application id is generated on the device`() = runTest {
        assertEquals("id-0", repository.apply(offer).id)
    }

    @Test
    fun `observeForOffer finds the users application`() = runTest {
        val application = repository.apply(offer)

        assertEquals(application, repository.observeForOffer("o-1").first())
        assertEquals(null, repository.observeForOffer("o-2").first())
    }

    @Test
    fun `logged out sees no applications`() = runTest {
        repository.apply(offer)
        session.saved.value = null

        assertTrue(repository.observeMine().first().isEmpty())
    }

    @Test
    fun `pending count follows the queue`() = runTest {
        repository.apply(offer)

        assertEquals(1, repository.observePendingCount().first())
    }

    @Test
    fun `refresh takes statuses from the server`() = runTest {
        val application = repository.apply(offer)
        applications.updateStatus(application.id, "SENT")
        remote.mine = { listOf(dto(application.id, "o-1", "APPROVED")) }

        assertEquals(RefreshResult.Success, repository.refreshMine())

        assertEquals(ApplicationStatus.APPROVED, repository.observeMine().first().single().status)
    }

    @Test
    fun `refresh never overwrites an application that is still waiting to be sent`() = runTest {
        val application = repository.apply(offer)
        remote.mine = { emptyList() }

        repository.refreshMine()

        assertEquals(ApplicationStatus.PENDING_SEND, repository.observeMine().first().single { it.id == application.id }.status)
    }

    @Test
    fun `refresh adds applications made from another device`() = runTest {
        remote.mine = { listOf(dto("a-remote", "o-9", "SENT")) }

        repository.refreshMine()

        assertEquals(listOf("a-remote"), repository.observeMine().first().map { it.id })
    }

    @Test
    fun `refresh without network keeps the local list`() = runTest {
        repository.apply(offer)
        remote.mine = { throw IOException("offline") }

        assertEquals(RefreshResult.NetworkError, repository.refreshMine())
        assertEquals(1, repository.observeMine().first().size)
    }

    @Test
    fun `refresh reports server errors`() = runTest {
        remote.mine = { throw ApiException(500) }

        assertEquals(RefreshResult.ServerError(500), repository.refreshMine())
    }

    private fun dto(id: String, offerId: String, status: String) = ApplicationDto(
        id = id, offerId = offerId, offerTitle = "Mozo", businessName = "[COMERCIO]", status = status,
        createdAt = "2026-11-01T12:00:00Z",
    )
}
