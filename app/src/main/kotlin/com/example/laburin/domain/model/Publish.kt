package com.example.laburin.domain.model

import java.time.Instant

data class BusinessSignUp(
    val businessName: String,
    val category: Category,
    val schedule: String,
    val address: String,
    val email: String,
    val password: String,
)

data class OfferDraft(val title: String, val category: Category?, val schedule: String, val description: String)

enum class PublishField { TITLE, CATEGORY, SCHEDULE, DESCRIPTION }

enum class BusinessSignUpField { NAME, CATEGORY, SCHEDULE, ADDRESS, EMAIL, PASSWORD }

/** The server only knows ACTIVE and CLOSED; the *_PENDING_* states are local changes waiting to be sent. */
enum class PublishedOfferStatus { PENDING_PUBLISH, EDIT_PENDING_SEND, CLOSE_PENDING_SEND, ACTIVE, CLOSED }

data class PublishedOffer(
    val id: String,
    val title: String,
    val category: Category,
    val schedule: String,
    val description: String,
    val status: PublishedOfferStatus,
    val createdAt: Instant,
)

sealed interface EditResult {
    data object NeedsBusinessAccount : EditResult
    data object NotFound : EditResult
    data object Closed : EditResult
    data class Invalid(val fields: Set<PublishField>) : EditResult
    data object Queued : EditResult
}

sealed interface PublishResult {
    data object NeedsBusinessAccount : PublishResult
    data class Invalid(val fields: Set<PublishField>) : PublishResult
    data class Queued(val offer: PublishedOffer) : PublishResult
}
