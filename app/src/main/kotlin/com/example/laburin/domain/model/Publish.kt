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

/** The salary is typed as the user wrote it; [com.example.laburin.domain.publish.salary] reads it once validated. */
data class OfferDraft(
    val title: String,
    val category: Category?,
    val schedule: String,
    val description: String,
    val salaryMin: String = "",
    val salaryMax: String = "",
    val salaryPeriod: SalaryPeriod = SalaryPeriod.MONTH,
)

enum class PublishField { TITLE, CATEGORY, SCHEDULE, DESCRIPTION, SALARY }

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
    val salary: Salary? = null,
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
