package com.example.laburin.domain.usecase

import com.example.laburin.domain.geo.Haversine
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.NearbyOffer
import com.example.laburin.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.math.roundToInt

/** SQL pre-filters by bounding box; the exact circle and the ordering are applied here. */
class ObserveNearbyOffersUseCase @Inject constructor(
    private val repository: OfferRepository,
) {
    operator fun invoke(
        center: GeoLocation,
        radiusKm: Int,
        category: Category?,
        query: String,
    ): Flow<List<NearbyOffer>> {
        val radiusMeters = radiusKm * 1_000.0
        val area = Haversine.boundingBox(center, radiusMeters)
        return repository.observeInArea(area, category, query.trim())
            .map { offers ->
                offers
                    .map { NearbyOffer(it, Haversine.distanceMeters(center, it.location).roundToInt()) }
                    .filter { it.distanceMeters <= radiusMeters }
                    .sortedBy { it.distanceMeters }
            }
            .distinctUntilChanged()
    }
}
