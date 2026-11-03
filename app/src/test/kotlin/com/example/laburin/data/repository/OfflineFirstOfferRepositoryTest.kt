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
import com.example.laburin.domain.model.ViewSource
import com.example.laburin.fakes.OBELISCO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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

    // --- Recently viewed and cache limit (RF-02, RNF-08) ---

    @Test
    fun `viewing an offer records where it came from`() = runTest {
        local.stored.value = listOf(entity("a"))

        repository.markViewed("a", ViewSource.QR)

        assertEquals(listOf("a" to "QR"), local.viewed.value)
        assertEquals(listOf("a"), repository.observeRecentlyViewed().first().map { it.id })
    }

    @Test
    fun `a successful refresh limits the cache to the area and the recently viewed offers`() = runTest {
        remote.response = { listOf(dto("a")) }

        repository.refresh(OBELISCO, radiusKm = 2, category = null)

        assertEquals(listOf(Haversine.boundingBox(OBELISCO, 2_000.0) to 30), local.prunes)
    }

    @Test
    fun `a failed refresh never prunes the cache`() = runTest {
        remote.response = { throw IOException("offline") }

        repository.refresh(OBELISCO, radiusKm = 2, category = null)

        assertTrue(local.prunes.isEmpty())
    }

    // --- Offer by id (detail screen, RF-02) ---

    @Test
    fun `observeOffer emits the cached offer as a domain model`() = runTest {
        local.stored.value = listOf(entity("a"))

        assertEquals("a", repository.observeOffer("a").first()?.id)
    }

    @Test
    fun `observeOffer emits null when the offer is not cached`() = runTest {
        assertNull(repository.observeOffer("missing").first())
    }

    @Test
    fun `refreshOffer stores the active offer returned by the server`() = runTest {
        remote.single = { dto("a").copy(title = "Actualizado") }

        val result = repository.refreshOffer("a")

        assertEquals(RefreshResult.Success, result)
        assertEquals("Actualizado", repository.observeOffer("a").first()?.title)
    }

    @Test
    fun `refreshOffer removes the cached offer when the server closed it`() = runTest {
        local.stored.value = listOf(entity("a"))
        remote.single = { dto("a").copy(status = "CLOSED") }

        val result = repository.refreshOffer("a")

        assertEquals(RefreshResult.Success, result)
        assertNull(repository.observeOffer("a").first())
    }

    @Test
    fun `refreshOffer removes the cached offer when the server does not know it`() = runTest {
        local.stored.value = listOf(entity("a"))
        remote.single = { null }

        val result = repository.refreshOffer("a")

        assertEquals(RefreshResult.Success, result)
        assertNull(repository.observeOffer("a").first())
    }

    @Test
    fun `refreshOffer removes the cached offer when the server sends one the app cannot show`() = runTest {
        local.stored.value = listOf(entity("a"))
        remote.single = { dto("a").copy(category = "ASTRONAUTA") }

        repository.refreshOffer("a")

        assertNull(repository.observeOffer("a").first())
    }

    @Test
    fun `refreshOffer keeps the cache on network failure`() = runTest {
        local.stored.value = listOf(entity("a"))
        remote.single = { throw IOException("offline") }

        val result = repository.refreshOffer("a")

        assertEquals(RefreshResult.NetworkError, result)
        assertEquals("a", repository.observeOffer("a").first()?.id)
    }

    @Test
    fun `refreshOffer keeps the cache and reports the code on server error`() = runTest {
        local.stored.value = listOf(entity("a"))
        remote.single = { throw ApiException(503) }

        val result = repository.refreshOffer("a")

        assertEquals(RefreshResult.ServerError(503), result)
        assertEquals("a", repository.observeOffer("a").first()?.id)
    }

    @Test(expected = CancellationException::class)
    fun `refreshOffer never swallows cancellation`() = runTest {
        remote.single = { throw CancellationException("cancelled") }
        repository.refreshOffer("a")
    }

    private fun entity(id: String) = OfferEntity(
        id = id, title = "Puesto $id", category = Category.MECHANICS, description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = OBELISCO.latitude,
        longitude = OBELISCO.longitude, updatedAt = Instant.parse("2026-11-01T12:00:00Z"),
    )

    private fun dto(id: String) = OfferDto(
        id = id, title = "Puesto $id", category = "MECHANICS", description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = OBELISCO.latitude,
        longitude = OBELISCO.longitude, status = "ACTIVE", updatedAt = "2026-11-01T12:00:00Z",
    )

    private class FakeRemoteDataSource : OfferRemoteDataSource {
        var response: () -> List<OfferDto> = { emptyList() }
        var single: () -> OfferDto? = { null }
        var lastCategory: String? = null
        override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?): List<OfferDto> {
            lastCategory = category
            return response()
        }

        override suspend fun getOffer(id: String): OfferDto? = single()
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

        override fun observeById(id: String): Flow<OfferEntity?> = stored.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun upsert(offer: OfferEntity) {
            stored.value = stored.value.filterNot { it.id == offer.id } + offer
        }

        override suspend fun delete(id: String) {
            stored.value = stored.value.filterNot { it.id == id }
        }

        val viewed = MutableStateFlow<List<Pair<String, String>>>(emptyList())
        val prunes = mutableListOf<Pair<BoundingBox, Int>>()
        override suspend fun markViewed(offerId: String, at: Instant, source: String) {
            viewed.value = listOf(offerId to source) + viewed.value.filterNot { it.first == offerId }
        }
        override fun observeRecentlyViewed(): Flow<List<OfferEntity>> =
            combine(viewed, stored) { v, s -> v.mapNotNull { (id, _) -> s.firstOrNull { it.id == id } } }
        override suspend fun prune(area: BoundingBox, maxViewed: Int) { prunes += area to maxViewed }

        override fun observeLastSyncedAt(): Flow<Instant?> = lastSynced
        override suspend fun setLastSyncedAt(instant: Instant) { lastSynced.value = instant }
    }
}
