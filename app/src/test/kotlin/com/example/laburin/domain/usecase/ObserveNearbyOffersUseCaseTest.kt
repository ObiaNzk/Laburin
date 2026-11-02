package com.example.laburin.domain.usecase

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.fakes.FakeOfferRepository
import com.example.laburin.fakes.OBELISCO
import com.example.laburin.fakes.testOffer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveNearbyOffersUseCaseTest {

    private val repository = FakeOfferRepository()
    private val useCase = ObserveNearbyOffersUseCase(repository)

    // ~1.08 km and ~3.5 km from the Obelisco.
    private val near = testOffer(id = "near", location = GeoLocation(-34.6083, -58.3712))
    private val far = testOffer(id = "far", location = GeoLocation(-34.5720, -58.3816))
    private val here = testOffer(id = "here", title = "Mozo", category = Category.GASTRONOMY, location = OBELISCO)

    @Test
    fun `keeps only offers inside the exact radius, sorted by distance`() = runTest {
        repository.offers.value = listOf(far, near, here)

        val result = useCase(OBELISCO, radiusKm = 2, category = null, query = "").first()

        assertEquals(listOf("here", "near"), result.map { it.offer.id })
        assertEquals(0, result.first().distanceMeters)
        assertEquals(1080.0, result[1].distanceMeters.toDouble(), 30.0)
    }

    @Test
    fun `wider radius includes farther offers`() = runTest {
        repository.offers.value = listOf(far, near)

        val result = useCase(OBELISCO, radiusKm = 5, category = null, query = "").first()

        assertEquals(listOf("near", "far"), result.map { it.offer.id })
    }

    @Test
    fun `passes category and query down to the repository`() = runTest {
        repository.offers.value = listOf(near, here)

        val result = useCase(OBELISCO, radiusKm = 2, category = Category.GASTRONOMY, query = "moz").first()

        assertEquals(listOf("here"), result.map { it.offer.id })
    }
}
