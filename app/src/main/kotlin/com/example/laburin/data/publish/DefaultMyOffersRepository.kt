package com.example.laburin.data.publish

import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.PublishedOfferDao
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.data.local.TransactionRunner
import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.OfferDto
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.MyOffersRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMyOffersRepository(
    private val dao: PublishedOfferDao,
    private val pending: PendingActionDao,
    private val remote: BusinessRemoteDataSource,
    private val session: SessionStore,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val idGenerator: () -> String,
) : MyOffersRepository {

    @Inject
    constructor(
        dao: PublishedOfferDao,
        pending: PendingActionDao,
        remote: BusinessRemoteDataSource,
        session: SessionStore,
        transaction: TransactionRunner,
        clock: Clock,
    ) : this(dao, pending, remote, session, transaction, clock, { UUID.randomUUID().toString() })

    private val businessId: Flow<String?> = session.stored.map { it?.session?.userId }

    override fun observeMine(): Flow<List<PublishedOffer>> = businessId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else dao.observeMine(id).map { rows -> rows.mapNotNull { it.toDomain() } }
    }

    override fun observe(id: String): Flow<PublishedOffer?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun publish(draft: OfferDraft): PublishedOffer {
        val owner = checkNotNull(businessId.first()) { "publish() requires a business session" }
        val entity = PublishedOfferEntity(
            id = idGenerator(), businessId = owner, title = draft.title.trim(),
            category = checkNotNull(draft.category).name, schedule = draft.schedule.trim(),
            description = draft.description.trim(), status = PublishedOfferStatus.PENDING_PUBLISH.name,
            createdAt = clock.instant(),
        )
        transaction.run {
            dao.insert(entity)
            pending.insert(PendingActionEntity(idGenerator(), PendingActionsSyncer.PUBLISH, entity.id, entity.createdAt))
        }
        return checkNotNull(entity.toDomain())
    }

    override suspend fun refreshMine(): RefreshResult {
        val stored = session.stored.first() ?: return RefreshResult.Success
        val fromServer = try {
            remote.myOffers(stored.token)
        } catch (e: IOException) {
            return RefreshResult.NetworkError
        } catch (e: ApiException) {
            return RefreshResult.ServerError(e.code)
        }
        for (dto in fromServer) {
            val local = dao.find(dto.id)
            // Still waiting to be published: the local pending state stands until the send is answered.
            if (local?.status == PublishedOfferStatus.PENDING_PUBLISH.name) continue
            dto.toEntity(stored.session.userId)?.let { dao.upsert(it) }
        }
        return RefreshResult.Success
    }

    private fun PublishedOfferEntity.toDomain(): PublishedOffer? {
        val category = Category.entries.firstOrNull { it.name == category } ?: return null
        val status = PublishedOfferStatus.entries.firstOrNull { it.name == status } ?: return null
        return PublishedOffer(id, title, category, schedule, description, status, createdAt)
    }

    private fun OfferDto.toEntity(businessId: String): PublishedOfferEntity? {
        if (Category.entries.none { it.name == category } || PublishedOfferStatus.entries.none { it.name == status }) return null
        val created = try { Instant.parse(updatedAt) } catch (_: DateTimeParseException) { return null }
        return PublishedOfferEntity(id, businessId, title, category, schedule, description, status, created)
    }
}
