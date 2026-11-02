package com.example.laburin.domain.usecase

import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.OfferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** No radius check on purpose: an offer seen or scanned nearby can be opened from anywhere (RF-02). */
class ObserveOfferUseCase @Inject constructor(private val repository: OfferRepository) {
    operator fun invoke(offerId: String): Flow<Offer?> = repository.observeOffer(offerId)
}

class RefreshOfferUseCase @Inject constructor(private val repository: OfferRepository) {
    suspend operator fun invoke(offerId: String): RefreshResult = repository.refreshOffer(offerId)
}
