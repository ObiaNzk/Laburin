package com.example.laburin.domain.model

/** What the user is told about, whether it comes from the local check or from a push (FCM). */
sealed interface NotificationEvent {
    data class ApplicationReviewed(val applicationId: String, val offerTitle: String, val approved: Boolean) : NotificationEvent
    data class NewApplicants(val offerId: String, val offerTitle: String, val count: Int) : NotificationEvent
}
