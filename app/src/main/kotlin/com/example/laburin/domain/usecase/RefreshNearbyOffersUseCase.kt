package com.example.laburin.domain.usecase

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.OfferRepository
import javax.inject.Inject

class RefreshNearbyOffersUseCase @Inject constructor(
    private val repository: OfferRepository,
) {
    suspend operator fun invoke(center: GeoLocation, radiusKm: Int, category: Category?): RefreshResult =
        repository.refresh(center, radiusKm, category)
}
