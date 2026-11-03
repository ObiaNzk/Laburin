package com.example.laburin.data.applications

import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.local.ApplicationDao
import com.example.laburin.data.local.ApplicationEntity
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.TransactionRunner
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.ApplicationRepository
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
class DefaultApplicationRepository(
    private val applications: ApplicationDao,
    private val pending: PendingActionDao,
    private val remote: ApplicationsRemoteDataSource,
    private val session: SessionStore,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val idGenerator: () -> String,
) : ApplicationRepository {

    @Inject
    constructor(
        applications: ApplicationDao,
        pending: PendingActionDao,
        remote: ApplicationsRemoteDataSource,
        session: SessionStore,
        transaction: TransactionRunner,
        clock: Clock,
    ) : this(applications, pending, remote, session, transaction, clock, { UUID.randomUUID().toString() })

    private val userId: Flow<String?> = session.stored.map { it?.session?.userId }

    override fun observeMine(): Flow<List<JobApplication>> = userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else applications.observeMine(id).map { rows -> rows.mapNotNull { it.toDomain() } }
    }

    override fun observeForOffer(offerId: String): Flow<JobApplication?> = userId.flatMapLatest { id ->
        if (id == null) flowOf(null) else applications.observeForOffer(id, offerId).map { it?.toDomain() }
    }

    override fun observePendingCount(): Flow<Int> = pending.observeCount()

    override suspend fun apply(offer: Offer): JobApplication {
        val id = checkNotNull(userId.first()) { "apply() requires a session" }
        val entity = ApplicationEntity(
            id = idGenerator(), userId = id, offerId = offer.id, offerTitle = offer.title,
            businessName = offer.businessName, status = ApplicationStatus.PENDING_SEND.name, createdAt = clock.instant(),
        )
        transaction.run {
            applications.insert(entity)
            pending.insert(PendingActionEntity(idGenerator(), PendingActionsSyncer.APPLY, entity.id, entity.createdAt))
        }
        return checkNotNull(entity.toDomain())
    }

    override suspend fun refreshMine(): RefreshResult {
        val stored = session.stored.first() ?: return RefreshResult.Success
        val remoteList = try {
            remote.myApplications(stored.token)
        } catch (e: IOException) {
            return RefreshResult.NetworkError
        } catch (e: ApiException) {
            return RefreshResult.ServerError(e.code)
        }
        for (dto in remoteList) {
            val local = applications.find(dto.id)
            when {
                local == null -> dto.toEntity(stored.session.userId)?.let { applications.insert(it) }
                // Not yet confirmed by the server: the local pending state is the truth until the send is answered.
                local.status == ApplicationStatus.PENDING_SEND.name -> Unit
                else -> applications.updateStatus(dto.id, dto.status)
            }
        }
        return RefreshResult.Success
    }

    private fun ApplicationEntity.toDomain(): JobApplication? {
        val status = ApplicationStatus.entries.firstOrNull { it.name == status } ?: return null
        return JobApplication(id, offerId, offerTitle, businessName, status, createdAt)
    }

    private fun ApplicationDto.toEntity(userId: String): ApplicationEntity? {
        if (ApplicationStatus.entries.none { it.name == status }) return null
        val created = try { Instant.parse(createdAt) } catch (_: DateTimeParseException) { return null }
        return ApplicationEntity(id, userId, offerId, offerTitle, businessName, status, created)
    }
}
