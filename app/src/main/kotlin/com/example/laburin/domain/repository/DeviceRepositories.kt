package com.example.laburin.domain.repository

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow

/** One-shot read: no background tracking (RNF-07). */
interface LocationProvider {
    suspend fun currentLocation(): LocationResult
}

interface ConnectivityObserver {
    val isOnline: Flow<Boolean>
}

interface FiltersRepository {
    val filters: Flow<SearchFilters>
    suspend fun setRadiusKm(radiusKm: Int)
    suspend fun setCategory(category: Category?)
}
