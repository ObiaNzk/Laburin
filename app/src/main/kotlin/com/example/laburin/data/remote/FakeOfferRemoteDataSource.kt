package com.example.laburin.data.remote

import com.example.laburin.domain.geo.Haversine
import com.example.laburin.domain.model.GeoLocation
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Stand-in for the Go API: same contract (docs/api/offers.md). All data is fictional. */
@Singleton
class FakeOfferRemoteDataSource @Inject constructor() : OfferRemoteDataSource {

    @Volatile
    var simulateNetworkFailure: Boolean = false

    override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?): List<OfferDto> {
        delay(SIMULATED_LATENCY_MS)
        if (simulateNetworkFailure) throw IOException("Simulated network failure")
        val center = GeoLocation(latitude, longitude)
        return SEED
            .filter { it.status == "ACTIVE" }
            .filter { category == null || it.category == category }
            .filter { Haversine.distanceMeters(center, GeoLocation(it.latitude, it.longitude)) <= radiusKm * 1_000.0 }
    }

    override suspend fun getOffer(id: String): OfferDto? {
        delay(SIMULATED_LATENCY_MS)
        if (simulateNetworkFailure) throw IOException("Simulated network failure")
        return SEED.firstOrNull { it.id == id }
    }

    private companion object {
        const val SIMULATED_LATENCY_MS = 600L
        const val UPDATED = "2026-11-01T12:00:00Z"

        fun seed(
            id: String, title: String, category: String, lat: Double, lng: Double,
            business: String, address: String, status: String = "ACTIVE",
        ) = OfferDto(
            id = id, title = title, category = category,
            description = "Buscamos una persona responsable y con ganas de aprender. No se requiere experiencia previa.",
            schedule = "Lunes a viernes, 9 a 17 h", jobType = "Jornada completa",
            businessName = business, address = address,
            latitude = lat, longitude = lng, status = status, updatedAt = UPDATED,
        )

        val SEED = listOf(
            seed("f-1", "Ayudante de taller", "MECHANICS", -34.6080, -58.3712, "Taller Ejemplo", "Av. Ejemplo 100"),
            seed("f-2", "Mozo/a fin de semana", "GASTRONOMY", -34.5997, -58.3819, "Bodegón Ejemplo", "Calle Ficticia 200"),
            seed("f-3", "Repositor/a", "STOCK", -34.6120, -58.3900, "Autoservicio Ejemplo", "Av. Ejemplo 300"),
            seed("f-4", "Vendedor/a", "CLOTHING", -34.6010, -58.3770, "Tienda Ejemplo", "Calle Ficticia 400"),
            seed("f-5", "Cocinero/a", "GASTRONOMY", -34.5850, -58.3950, "Parrilla Ejemplo", "Av. Ejemplo 500"),
            seed("f-6", "Mecánico/a", "MECHANICS", -34.6400, -58.4200, "Lubricentro Ejemplo", "Calle Ficticia 600"),
            seed("f-7", "Cajero/a", "STOCK", -34.6050, -58.3800, "Kiosco Ejemplo", "Av. Ejemplo 700", status = "CLOSED"),
        )
    }
}
