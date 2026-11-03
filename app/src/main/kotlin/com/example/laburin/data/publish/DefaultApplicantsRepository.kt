package com.example.laburin.data.publish

import com.example.laburin.data.applications.PendingActionsSyncer
import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.local.ApplicantDao
import com.example.laburin.data.local.ApplicantEntity
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PendingActionEntity
import com.example.laburin.data.local.TransactionRunner
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.repository.ApplicantsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

class DefaultApplicantsRepository(
    private val dao: ApplicantDao,
    private val pending: PendingActionDao,
    private val remote: BusinessRemoteDataSource,
    private val session: SessionStore,
    private val transaction: TransactionRunner,
    private val clock: Clock,
    private val idGenerator: () -> String,
) : ApplicantsRepository {

    @Inject
    constructor(
        dao: ApplicantDao,
        pending: PendingActionDao,
        remote: BusinessRemoteDataSource,
        session: SessionStore,
        transaction: TransactionRunner,
        clock: Clock,
    ) : this(dao, pending, remote, session, transaction, clock, { UUID.randomUUID().toString() })

    override fun observeForOffer(offerId: String): Flow<List<Applicant>> =
        dao.observeForOffer(offerId).map { rows -> rows.mapNotNull { it.toDomain() } }

    override suspend fun refresh(offerId: String): RefreshResult {
        val token = session.stored.first()?.token ?: return RefreshResult.Success
        val fromServer = try {
            remote.applicants(token, offerId)
        } catch (e: IOException) {
            return RefreshResult.NetworkError
        } catch (e: ApiException) {
            return RefreshResult.ServerError(e.code)
        }
        for (dto in fromServer) {
            val local = dao.find(dto.applicationId)
            if (local != null && local.status in PENDING_DECISIONS) continue // Decision not answered yet: keep it.
            dao.upsert(ApplicantEntity(dto.applicationId, dto.offerId, dto.candidateName, dto.cvFileName, dto.status, dto.contactEmail))
        }
        return RefreshResult.Success
    }

    override suspend fun review(applicationId: String, approve: Boolean) {
        val applicant = dao.find(applicationId) ?: return
        val status = if (approve) ApplicantStatus.APPROVAL_PENDING_SEND else ApplicantStatus.REJECTION_PENDING_SEND
        transaction.run {
            dao.upsert(applicant.copy(status = status.name))
            pending.insert(PendingActionEntity(idGenerator(), PendingActionsSyncer.REVIEW, applicationId, clock.instant()))
        }
    }

    private fun ApplicantEntity.toDomain(): Applicant? {
        val status = ApplicantStatus.entries.firstOrNull { it.name == status } ?: return null
        return Applicant(applicationId, candidateName, cvFileName, status, contactEmail)
    }

    private companion object {
        val PENDING_DECISIONS = setOf(ApplicantStatus.APPROVAL_PENDING_SEND.name, ApplicantStatus.REJECTION_PENDING_SEND.name)
    }
}
