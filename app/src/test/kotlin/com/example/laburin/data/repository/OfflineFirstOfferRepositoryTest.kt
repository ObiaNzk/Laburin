package com.example.laburin.data.repository

import com.example.laburin.data.local.OfferEntity
import com.example.laburin.data.local.OfferLocalDataSource
import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.OfferDto
import com.example.laburin.data.remote.OfferRemoteDataSource
import com.example.laburin.domain.geo.Haversine
import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.fakes.OBELISCO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OfflineFirstOfferRepositoryTest {

    private val now = Instant.parse("2026-11-02T10:00:00Z")
    private val local = FakeLocalDataSource()
    private val remote = FakeRemoteDataSource()
    private val repository = OfflineFirstOfferRepository(local, remote, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `successful refresh replaces the area with server data and records sync time`() = runTest {
        remote.response = { listOf(dto("a"), dto("b")) }

        val result = repository.refresh(OBELISCO, radiusKm = 2, category = Category.MECHANICS)

        assertEquals(RefreshResult.Success, result)
        val call = local.replaceCalls.single()
        assertEquals(Haversine.boundingBox(OBELISCO, 2_000.0), call.first)
        assertEquals(Category.MECHANICS, call.second)
        assertEquals(listOf("a", "b"), call.third.map { it.id })
        assertEquals(now, local.lastSynced.value)
        assertEquals("MECHANICS", remote.lastCategory)
    }

    @Test
    fun `network failure keeps the cache untouched`() = runTest {
        remote.response = { throw IOException("offline") }

        val result = repository.refresh(OBELISCO, radiusKm = 2, category = null)

        assertEquals(RefreshResult.NetworkError, result)
        assertTrue(local.replaceCalls.isEmpty())
        assertNull(local.lastSynced.value)
    }

    @Test
    fun `server error is reported with its status code`() = runTest {
        remote.response = { throw ApiException(503) }

        assertEquals(RefreshResult.ServerError(503), repository.refresh(OBELISCO, 2, null))
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is never swallowed as an error`() = runTest {
        remote.response = { throw CancellationException("cancelled") }
        repository.refresh(OBELISCO, 2, null)
    }

    @Test
    fun `observe maps cached entities to domain`() = runTest {
        remote.response = { listOf(dto("a")) }
        repository.refresh(OBELISCO, 2, null)

        val offers = repository.observeInArea(Haversine.boundingBox(OBELISCO, 2_000.0), null, "").first()

        assertEquals(listOf("a"), offers.map { it.id })
    }

    private fun dto(id: String) = OfferDto(
        id = id, title = "Puesto $id", category = "MECHANICS", description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = OBELISCO.latitude,
        longitude = OBELISCO.longitude, status = "ACTIVE", updatedAt = "2026-11-01T12:00:00Z",
    )

    private class FakeRemoteDataSource : OfferRemoteDataSource {
        var response: () -> List<OfferDto> = { emptyList() }
        var lastCategory: String? = null
        override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?): List<OfferDto> {
            lastCategory = category
            return response()
        }
    }

    private class FakeLocalDataSource : OfferLocalDataSource {
        val stored = MutableStateFlow<List<OfferEntity>>(emptyList())
        val lastSynced = MutableStateFlow<Instant?>(null)
        val replaceCalls = mutableListOf<Triple<BoundingBox, Category?, List<OfferEntity>>>()

        override fun observeInArea(area: BoundingBox, category: Category?, query: String): Flow<List<OfferEntity>> =
            stored.map { it }

        override suspend fun replaceInArea(area: BoundingBox, category: Category?, offers: List<OfferEntity>) {
            replaceCalls += Triple(area, category, offers)
            stored.value = offers
        }

        override fun observeLastSyncedAt(): Flow<Instant?> = lastSynced
        override suspend fun setLastSyncedAt(instant: Instant) { lastSynced.value = instant }
    }
}
