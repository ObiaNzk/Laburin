package com.example.laburin.data.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.laburin.domain.model.NotificationEvent
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.notifications.NotificationRules
import com.example.laburin.domain.repository.ApplicantsRepository
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.domain.repository.Notifier
import com.example.laburin.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Data payload of an FCM message, as the Go API will send it (docs/api/notifications.md). */
object PushPayload {
    fun parse(data: Map<String, String>): NotificationEvent? = when (data["type"]) {
        "APPLICATION_REVIEWED" -> {
            val id = data["applicationId"]
            val title = data["offerTitle"]
            val approved = data["approved"]?.toBooleanStrictOrNull()
            if (id != null && title != null && approved != null) NotificationEvent.ApplicationReviewed(id, title, approved) else null
        }
        "NEW_APPLICANTS" -> {
            val id = data["offerId"]
            val title = data["offerTitle"]
            val count = data["count"]?.toIntOrNull()
            if (id != null && title != null && count != null) NotificationEvent.NewApplicants(id, title, count) else null
        }
        else -> null
    }
}

/** Applicants already notified to the business, per offer. */
interface KnownApplicantsStore {
    suspend fun known(offerId: String): Set<String>
    suspend fun remember(offerId: String, applicationIds: Set<String>)
}

class DataStoreKnownApplicants @Inject constructor(private val dataStore: DataStore<Preferences>) : KnownApplicantsStore {
    override suspend fun known(offerId: String): Set<String> = dataStore.data.first()[key(offerId)].orEmpty()
    override suspend fun remember(offerId: String, applicationIds: Set<String>) {
        dataStore.edit { it[key(offerId)] = applicationIds }
    }
    private fun key(offerId: String) = stringSetPreferencesKey("notified_applicants_$offerId")
}

/** Local stand-in for push: compares what the server says now with what the app already knew. */
class NotificationsChecker @Inject constructor(
    private val session: SessionRepository,
    private val applications: ApplicationRepository,
    private val myOffers: MyOffersRepository,
    private val applicants: ApplicantsRepository,
    private val known: KnownApplicantsStore,
    private val notifier: Notifier,
) {
    suspend fun check() {
        when (session.session.first()?.role) {
            UserRole.CANDIDATE -> checkApplications()
            UserRole.BUSINESS -> checkApplicants()
            null -> Unit
        }
    }

    private suspend fun checkApplications() {
        val before = applications.observeMine().first()
        applications.refreshMine()
        NotificationRules.applicationChanges(before, applications.observeMine().first()).forEach(notifier::notify)
    }

    private suspend fun checkApplicants() {
        myOffers.refreshMine()
        for (offer in myOffers.observeMine().first().filter { it.status == PublishedOfferStatus.ACTIVE }) {
            applicants.refresh(offer.id)
            val current = applicants.observeForOffer(offer.id).first().map { it.applicationId }.toSet()
            NotificationRules.newApplicants(offer.id, offer.title, known.known(offer.id), current)?.let(notifier::notify)
            known.remember(offer.id, current)
        }
    }
}
