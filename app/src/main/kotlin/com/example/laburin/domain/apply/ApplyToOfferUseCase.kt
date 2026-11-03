package com.example.laburin.domain.apply

import com.example.laburin.domain.model.ApplyResult
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.domain.repository.SessionRepository
import com.example.laburin.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * RF-02 business rules: an account and an uploaded CV are required; applying twice never duplicates.
 * Distance is not checked on purpose: an offer seen nearby can be applied to from anywhere.
 */
class ApplyToOfferUseCase @Inject constructor(
    private val session: SessionRepository,
    private val cv: CvRepository,
    private val applications: ApplicationRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(offer: Offer): ApplyResult {
        val current = session.session.first() ?: return ApplyResult.NeedsAccount
        if (current.role != UserRole.CANDIDATE) return ApplyResult.NotACandidate
        applications.observeForOffer(offer.id).first()?.let { return ApplyResult.AlreadyApplied(it) }
        if (cv.current() == null) return ApplyResult.NeedsCv
        val application = applications.apply(offer)
        scheduler.schedule()
        return ApplyResult.Queued(application)
    }
}
