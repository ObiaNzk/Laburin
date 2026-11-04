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
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.publish.salary
import com.example.laburin.data.mapper.salaryFromColumns
import com.example.laburin.data.mapper.toDomainOrNull
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
        ).withSalary(draft.salary())
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
            // Local changes not sent yet: they stand until the server answers them.
            if (local?.status in LOCAL_PENDING) continue
            dto.toEntity(stored.session.userId)?.let { dao.upsert(it) }
        }
        return RefreshResult.Success
    }

    override suspend fun edit(id: String, draft: OfferDraft) {
        val offer = ownOffer(id) ?: return
        if (offer.status in CLOSING) return
        // Not sent yet: it stays "pending publish". The edit is queued anyway, so changes made while the publish is
        // on its way are never lost (the server stores the latest content each time).
        val status = if (offer.status == PublishedOfferStatus.PENDING_PUBLISH.name) offer.status else PublishedOfferStatus.EDIT_PENDING_SEND.name
        val changed = offer.copy(
            title = draft.title.trim(), category = checkNotNull(draft.category).name, schedule = draft.schedule.trim(),
            description = draft.description.trim(), status = status,
        ).withSalary(draft.salary())
        transaction.run {
            dao.upsert(changed)
            pending.insert(PendingActionEntity(idGenerator(), PendingActionsSyncer.EDIT, id, clock.instant()))
        }
    }

    override suspend fun close(id: String) {
        val offer = ownOffer(id) ?: return
        if (offer.status in CLOSING) return
        transaction.run {
            dao.updateStatus(id, PublishedOfferStatus.CLOSE_PENDING_SEND.name)
            pending.insert(PendingActionEntity(idGenerator(), PendingActionsSyncer.CLOSE, id, clock.instant()))
        }
    }

    private suspend fun ownOffer(id: String): PublishedOfferEntity? {
        val owner = businessId.first() ?: return null
        return dao.find(id)?.takeIf { it.businessId == owner }
    }

    private fun PublishedOfferEntity.toDomain(): PublishedOffer? {
        val category = Category.entries.firstOrNull { it.name == category } ?: return null
        val status = PublishedOfferStatus.entries.firstOrNull { it.name == status } ?: return null
        return PublishedOffer(id, title, category, schedule, description, status, createdAt, salaryFromColumns(salaryMin, salaryMax, salaryPeriod))
    }

    private fun OfferDto.toEntity(businessId: String): PublishedOfferEntity? {
        if (Category.entries.none { it.name == category } || PublishedOfferStatus.entries.none { it.name == status }) return null
        val created = try { Instant.parse(updatedAt) } catch (_: DateTimeParseException) { return null }
        return PublishedOfferEntity(id, businessId, title, category, schedule, description, status, created)
            .withSalary(salary?.toDomainOrNull())
    }

    private fun PublishedOfferEntity.withSalary(salary: Salary?) =
        copy(salaryMin = salary?.min, salaryMax = salary?.max, salaryPeriod = salary?.period?.name)

    private companion object {
        val CLOSING = setOf(PublishedOfferStatus.CLOSED.name, PublishedOfferStatus.CLOSE_PENDING_SEND.name)
        val LOCAL_PENDING = setOf(
            PublishedOfferStatus.PENDING_PUBLISH.name, PublishedOfferStatus.EDIT_PENDING_SEND.name,
            PublishedOfferStatus.CLOSE_PENDING_SEND.name,
        )
    }
}
