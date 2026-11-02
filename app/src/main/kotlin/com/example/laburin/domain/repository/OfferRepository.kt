package com.example.laburin.domain.repository

import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface OfferRepository {
    fun observeInArea(area: BoundingBox, category: Category?, query: String): Flow<List<Offer>>

    fun observeLastSyncedAt(): Flow<Instant?>

    /** Replaces the cached offers of that area. The server always wins. */
    suspend fun refresh(center: GeoLocation, radiusKm: Int, category: Category?): RefreshResult
}
