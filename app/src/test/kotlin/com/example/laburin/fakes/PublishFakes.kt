package com.example.laburin.fakes

import com.example.laburin.data.local.PublishedOfferDao
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.MyOffersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant

class FakeMyOffersRepository : MyOffersRepository {
    val store = MutableStateFlow<List<PublishedOffer>>(emptyList())
    val published get() = store.value
    var refreshCalls = 0
    override fun observeMine(): Flow<List<PublishedOffer>> = store
    override fun observe(id: String) = store.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun publish(draft: OfferDraft): PublishedOffer {
        val offer = testPublishedOffer("p-${store.value.size}", draft.title)
        store.value = store.value + offer
        return offer
    }
    override suspend fun refreshMine(): RefreshResult { refreshCalls++; return RefreshResult.Success }
    val edits = mutableListOf<Pair<String, OfferDraft>>()
    val closes = mutableListOf<String>()
    override suspend fun edit(id: String, draft: OfferDraft) { edits += id to draft }
    override suspend fun close(id: String) { closes += id }
}

fun testPublishedOffer(id: String = "p-1", title: String = "Mozo", status: PublishedOfferStatus = PublishedOfferStatus.PENDING_PUBLISH) =
    PublishedOffer(id, title, com.example.laburin.domain.model.Category.GASTRONOMY, "Sábados", "[DESCRIPCION]", status, Instant.EPOCH)

class FakePublishedOfferDao : PublishedOfferDao {
    val rows = MutableStateFlow<List<PublishedOfferEntity>>(emptyList())
    override fun observeMine(businessId: String) = rows.map { l -> l.filter { it.businessId == businessId }.sortedByDescending { it.createdAt } }
    override fun observe(id: String) = rows.map { l -> l.firstOrNull { it.id == id } }
    override suspend fun find(id: String) = rows.value.firstOrNull { it.id == id }
    override suspend fun insert(offer: PublishedOfferEntity) { if (rows.value.none { it.id == offer.id }) rows.value = rows.value + offer }
    override suspend fun upsert(offer: PublishedOfferEntity) { rows.value = rows.value.filterNot { it.id == offer.id } + offer }
    override suspend fun updateStatus(id: String, status: String) {
        rows.value = rows.value.map { if (it.id == id) it.copy(status = status) else it }
    }
}

class FakeApplicantsRepository : com.example.laburin.domain.repository.ApplicantsRepository {
    val store = MutableStateFlow<List<com.example.laburin.domain.model.Applicant>>(emptyList())
    val refreshCalls = mutableListOf<String>()
    val reviews = mutableListOf<String>()
    override fun observeForOffer(offerId: String): Flow<List<com.example.laburin.domain.model.Applicant>> = store
    override suspend fun refresh(offerId: String): RefreshResult { refreshCalls += offerId; return RefreshResult.Success }
    override suspend fun review(applicationId: String, approve: Boolean) { reviews += "$applicationId:$approve" }
}
