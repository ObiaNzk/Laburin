package com.example.laburin.domain.repository

import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

/** Candidates who applied to the logged-in business's offers (RF-04). */
interface ApplicantsRepository {
    fun observeForOffer(offerId: String): Flow<List<Applicant>>
    suspend fun refresh(offerId: String): RefreshResult

    /** Stored as pending and queued; the contact appears only when the server confirms an approval. */
    suspend fun review(applicationId: String, approve: Boolean)
}
