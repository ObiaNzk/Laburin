package com.example.laburin.data.remote

import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.domain.geo.Haversine
import com.example.laburin.domain.model.GeoLocation
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Offers endpoints of the fake API (docs/api/offers.md), backed by [FakeBackend]. All data is fictional. */
@Singleton
class FakeOfferRemoteDataSource @Inject constructor(private val backend: FakeBackend) : OfferRemoteDataSource {

    @Volatile
    var simulateNetworkFailure: Boolean = false

    override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?): List<OfferDto> {
        delay(SIMULATED_LATENCY_MS)
        if (simulateNetworkFailure) throw IOException("Simulated network failure")
        val center = GeoLocation(latitude, longitude)
        return backend.allOffers()
            .filter { it.status == FakeBackend.ACTIVE }
            .filter { category == null || it.category == category }
            .filter { Haversine.distanceMeters(center, GeoLocation(it.latitude, it.longitude)) <= radiusKm * 1_000.0 }
    }

    override suspend fun getOffer(id: String): OfferDto? {
        delay(SIMULATED_LATENCY_MS)
        if (simulateNetworkFailure) throw IOException("Simulated network failure")
        return backend.allOffers().firstOrNull { it.id == id }
    }

    private companion object {
        const val SIMULATED_LATENCY_MS = 600L
    }
}
