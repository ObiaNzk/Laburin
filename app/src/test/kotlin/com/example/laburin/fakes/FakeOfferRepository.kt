package com.example.laburin.fakes

import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeOfferRepository : OfferRepository {
    val offers = MutableStateFlow<List<Offer>>(emptyList())
    val lastSyncedAt = MutableStateFlow<Instant?>(null)
    var nextRefreshResult: RefreshResult = RefreshResult.Success
    val refreshCalls = mutableListOf<Triple<GeoLocation, Int, Category?>>()

    override fun observeInArea(area: BoundingBox, category: Category?, query: String): Flow<List<Offer>> =
        offers.map { list ->
            list.filter {
                it.location.latitude in area.minLatitude..area.maxLatitude &&
                    it.location.longitude in area.minLongitude..area.maxLongitude &&
                    (category == null || it.category == category) &&
                    it.title.contains(query, ignoreCase = true)
            }
        }

    override fun observeLastSyncedAt(): Flow<Instant?> = lastSyncedAt

    override suspend fun refresh(center: GeoLocation, radiusKm: Int, category: Category?): RefreshResult {
        refreshCalls += Triple(center, radiusKm, category)
        return nextRefreshResult
    }
}
