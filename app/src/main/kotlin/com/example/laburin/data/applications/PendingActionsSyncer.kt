package com.example.laburin.data.applications

import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.local.ApplicantDao
import com.example.laburin.data.local.ApplicationDao
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.local.PublishedOfferDao
import com.example.laburin.data.local.PublishedOfferEntity
import com.example.laburin.data.publish.BusinessRemoteDataSource
import com.example.laburin.data.publish.PublishOfferDto
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.ApplicantStatus
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.SyncOutcome
import com.example.laburin.domain.repository.SessionExpiryNotifier
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

/**
 * Sends the outbox in order. Nothing is removed until the server answers for it, so app restarts, lost network
 * or an expired session never lose an action (RNF-03). The server's answer always wins.
 */
class PendingActionsSyncer @Inject constructor(
    private val pending: PendingActionDao,
    private val applications: ApplicationDao,
    private val remote: ApplicationsRemoteDataSource,
    private val session: SessionStore,
    private val publishedOffers: PublishedOfferDao,
    private val business: BusinessRemoteDataSource,
    private val applicants: ApplicantDao,
    private val sessionExpiry: SessionExpiryNotifier,
) {
    suspend fun run(): SyncOutcome {
        val actions = pending.all()
        if (actions.isEmpty()) return SyncOutcome.Done
        val stored = session.stored.first() ?: return SyncOutcome.AuthRequired
        val token = stored.token
        val userId = stored.session.userId
        for (action in actions) {
            val outcome = when (action.type) {
                APPLY -> sendApplication(token, userId, action.targetId)
                PUBLISH -> sendPublication(token, userId, action.targetId)
                REVIEW -> sendReview(token, action.targetId)
                EDIT -> sendEdit(token, userId, action.targetId)
                CLOSE -> sendClose(token, userId, action.targetId)
                else -> Sent // Unknown types come from a newer app version that was downgraded: drop them.
            }
            when (outcome) {
                Sent -> pending.delete(action.id)
                Skip -> Unit
                is Stop -> {
                    if (outcome.result == SyncOutcome.AuthRequired) sessionExpiry.sessionExpired()
                    return outcome.result
                }
            }
        }
        return SyncOutcome.Done
    }

    private suspend fun sendApplication(token: String, userId: String, applicationId: String): Step {
        val application = applications.find(applicationId) ?: return Sent
        // Someone else's action (they logged out and another person logged in): keep it for its owner.
        if (application.userId != userId) return Skip
        return try {
            remote.apply(token, application.id, application.offerId)
            applications.updateStatus(application.id, ApplicationStatus.SENT.name)
            Sent
        } catch (e: IOException) {
            Stop(SyncOutcome.Retry)
        } catch (e: ApiException) {
            when (e.code) {
                HTTP_UNAUTHORIZED -> Stop(SyncOutcome.AuthRequired)
                HTTP_CONFLICT -> {
                    applications.updateStatus(application.id, ApplicationStatus.OFFER_CLOSED.name)
                    Sent
                }
                else -> Stop(SyncOutcome.Retry)
            }
        }
    }

    private suspend fun sendPublication(token: String, userId: String, offerId: String): Step {
        val offer = publishedOffers.find(offerId) ?: return Sent
        if (offer.businessId != userId) return Skip
        return try {
            business.publishOffer(token, offer.toDto())
            // The business may have closed it while it was on its way: only a publish still pending becomes active.
            if (publishedOffers.find(offer.id)?.status == PublishedOfferStatus.PENDING_PUBLISH.name) {
                publishedOffers.updateStatus(offer.id, PublishedOfferStatus.ACTIVE.name)
            }
            Sent
        } catch (e: IOException) {
            Stop(SyncOutcome.Retry)
        } catch (e: ApiException) {
            if (e.code == HTTP_UNAUTHORIZED) Stop(SyncOutcome.AuthRequired) else Stop(SyncOutcome.Retry)
        }
    }

    /** The same PUT as publishing: the server replaces the content, or answers 409 if the offer is closed. */
    private suspend fun sendEdit(token: String, userId: String, offerId: String): Step {
        val offer = publishedOffers.find(offerId) ?: return Sent
        if (offer.businessId != userId) return Skip
        // Closing wins over changes saved before it: they are not sent.
        if (offer.status in CLOSING) return Sent
        return try {
            business.publishOffer(token, offer.toDto())
            // Changes saved while this send was on its way keep the offer pending until they are sent too.
            val current = publishedOffers.find(offerId)
            if (current?.status == PublishedOfferStatus.EDIT_PENDING_SEND.name && pending.countFor(EDIT, offerId) == 1) {
                publishedOffers.updateStatus(offerId, PublishedOfferStatus.ACTIVE.name)
            }
            Sent
        } catch (e: IOException) {
            Stop(SyncOutcome.Retry)
        } catch (e: ApiException) {
            when (e.code) {
                HTTP_UNAUTHORIZED -> Stop(SyncOutcome.AuthRequired)
                HTTP_CONFLICT -> {
                    publishedOffers.updateStatus(offerId, PublishedOfferStatus.CLOSED.name)
                    Sent
                }
                else -> Stop(SyncOutcome.Retry)
            }
        }
    }

    private suspend fun sendClose(token: String, userId: String, offerId: String): Step {
        val offer = publishedOffers.find(offerId) ?: return Sent
        if (offer.businessId != userId) return Skip
        if (offer.status != PublishedOfferStatus.CLOSE_PENDING_SEND.name) return Sent
        return try {
            business.closeOffer(token, offerId)
            publishedOffers.updateStatus(offerId, PublishedOfferStatus.CLOSED.name)
            Sent
        } catch (e: IOException) {
            Stop(SyncOutcome.Retry)
        } catch (e: ApiException) {
            when (e.code) {
                HTTP_UNAUTHORIZED -> Stop(SyncOutcome.AuthRequired)
                // Already closed, or the server no longer has it: either way it is closed.
                HTTP_NOT_FOUND, HTTP_CONFLICT -> {
                    publishedOffers.updateStatus(offerId, PublishedOfferStatus.CLOSED.name)
                    Sent
                }
                else -> Stop(SyncOutcome.Retry)
            }
        }
    }

    private fun PublishedOfferEntity.toDto() = PublishOfferDto(id, title, category, schedule, description)

    private suspend fun sendReview(token: String, applicationId: String): Step {
        val applicant = applicants.find(applicationId) ?: return Sent
        val approve = when (applicant.status) {
            ApplicantStatus.APPROVAL_PENDING_SEND.name -> true
            ApplicantStatus.REJECTION_PENDING_SEND.name -> false
            else -> return Sent // Already answered by the server.
        }
        return try {
            val confirmed = business.review(token, applicationId, approve)
            // The contact is stored only from the server's confirmation, never assumed locally (RNF-05).
            applicants.upsert(applicant.copy(status = confirmed.status, contactEmail = confirmed.contactEmail))
            Sent
        } catch (e: IOException) {
            Stop(SyncOutcome.Retry)
        } catch (e: ApiException) {
            if (e.code == HTTP_UNAUTHORIZED) Stop(SyncOutcome.AuthRequired) else Stop(SyncOutcome.Retry)
        }
    }

    private sealed interface Step
    private data object Sent : Step
    private data object Skip : Step
    private data class Stop(val result: SyncOutcome) : Step

    companion object {
        const val APPLY = "APPLY"
        const val PUBLISH = "PUBLISH"
        const val REVIEW = "REVIEW"
        const val EDIT = "EDIT"
        const val CLOSE = "CLOSE"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_CONFLICT = 409
        private const val HTTP_NOT_FOUND = 404
        private val CLOSING = setOf(PublishedOfferStatus.CLOSED.name, PublishedOfferStatus.CLOSE_PENDING_SEND.name)
    }
}
