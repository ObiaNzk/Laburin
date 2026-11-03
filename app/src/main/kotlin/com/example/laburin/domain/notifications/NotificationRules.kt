package com.example.laburin.domain.notifications

import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.NotificationEvent

object NotificationRules {

    private val REVIEWED = setOf(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED)

    /** Only the business's decision is worth a notification; it is reported once, when it first appears. */
    fun applicationChanges(before: List<JobApplication>, after: List<JobApplication>): List<NotificationEvent> {
        val previous = before.associate { it.id to it.status }
        return after
            .filter { it.status in REVIEWED && previous[it.id] != it.status }
            .map { NotificationEvent.ApplicationReviewed(it.id, it.offerTitle, approved = it.status == ApplicationStatus.APPROVED) }
    }

    fun newApplicants(offerId: String, offerTitle: String, knownIds: Set<String>, currentIds: Set<String>): NotificationEvent? {
        val added = (currentIds - knownIds).size
        return if (added > 0) NotificationEvent.NewApplicants(offerId, offerTitle, added) else null
    }
}
