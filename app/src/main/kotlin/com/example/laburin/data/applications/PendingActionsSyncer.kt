package com.example.laburin.data.applications

import com.example.laburin.data.auth.SessionStore
import com.example.laburin.data.local.ApplicationDao
import com.example.laburin.data.local.PendingActionDao
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.SyncOutcome
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
) {
    suspend fun run(): SyncOutcome {
        val actions = pending.all()
        if (actions.isEmpty()) return SyncOutcome.Done
        val token = session.stored.first()?.token ?: return SyncOutcome.AuthRequired
        for (action in actions) {
            val outcome = when (action.type) {
                APPLY -> sendApplication(token, action.targetId)
                else -> Sent // Unknown types come from a newer app version that was downgraded: drop them.
            }
            when (outcome) {
                Sent -> pending.delete(action.id)
                is Stop -> return outcome.result
            }
        }
        return SyncOutcome.Done
    }

    private suspend fun sendApplication(token: String, applicationId: String): Step {
        val application = applications.find(applicationId) ?: return Sent
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

    private sealed interface Step
    private data object Sent : Step
    private data class Stop(val result: SyncOutcome) : Step

    companion object {
        const val APPLY = "APPLY"
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_CONFLICT = 409
    }
}
