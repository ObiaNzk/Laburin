package com.example.laburin.domain.repository

import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.RefreshResult
import kotlinx.coroutines.flow.Flow

/** The logged-in business's own offers. */
interface MyOffersRepository {
    fun observeMine(): Flow<List<PublishedOffer>>
    fun observe(id: String): Flow<PublishedOffer?>

    /** Stored as "pending publish" and queued; visible on the map only once the server accepts it. */
    suspend fun publish(draft: OfferDraft): PublishedOffer

    suspend fun refreshMine(): RefreshResult

    /** Stores the changes as pending and queues them; the server decides whether the offer can still change. */
    suspend fun edit(id: String, draft: OfferDraft)

    /** Marks the offer as pending close and queues it. */
    suspend fun close(id: String)
}
