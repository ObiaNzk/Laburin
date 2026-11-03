package com.example.laburin.domain.publish

import com.example.laburin.domain.auth.SignUpValidator
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishResult
import com.example.laburin.domain.model.SignUpField
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.ApplicantsRepository
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.domain.repository.SessionRepository
import com.example.laburin.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** The text encoded in the QR printed on the shop sign. */
object OfferLink {
    private const val PREFIX = "laburin://offer/"

    fun build(offerId: String): String = PREFIX + offerId

    fun parse(text: String): String? = text.trim()
        .takeIf { it.startsWith(PREFIX) }
        ?.removePrefix(PREFIX)
        ?.trimEnd('/')
        ?.takeIf { it.isNotEmpty() && '/' !in it }
}

object OfferDraftValidator {
    fun validate(draft: OfferDraft): Set<PublishField> = buildSet {
        if (draft.title.isBlank()) add(PublishField.TITLE)
        if (draft.category == null) add(PublishField.CATEGORY)
        if (draft.schedule.isBlank()) add(PublishField.SCHEDULE)
        if (draft.description.isBlank()) add(PublishField.DESCRIPTION)
    }
}

object BusinessSignUpValidator {
    fun validate(name: String, category: Category?, schedule: String, address: String, email: String, password: String) =
        buildSet {
            if (name.isBlank()) add(BusinessSignUpField.NAME)
            if (category == null) add(BusinessSignUpField.CATEGORY)
            if (schedule.isBlank()) add(BusinessSignUpField.SCHEDULE)
            if (address.isBlank()) add(BusinessSignUpField.ADDRESS)
            // Same email and password rules as candidates.
            val accountErrors = SignUpValidator.validate("x", "x", email, password)
            if (SignUpField.EMAIL in accountErrors) add(BusinessSignUpField.EMAIL)
            if (SignUpField.PASSWORD in accountErrors) add(BusinessSignUpField.PASSWORD)
        }
}

/** RF-03: only a business account can publish; incomplete drafts never reach the queue. */
class PublishOfferUseCase @Inject constructor(
    private val session: SessionRepository,
    private val offers: MyOffersRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(draft: OfferDraft): PublishResult {
        if (session.session.first()?.role != UserRole.BUSINESS) return PublishResult.NeedsBusinessAccount
        val invalid = OfferDraftValidator.validate(draft)
        if (invalid.isNotEmpty()) return PublishResult.Invalid(invalid)
        val offer = offers.publish(draft)
        scheduler.schedule()
        return PublishResult.Queued(offer)
    }
}

class ReviewApplicantUseCase @Inject constructor(
    private val applicants: ApplicantsRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(applicationId: String, approve: Boolean) {
        applicants.review(applicationId, approve)
        scheduler.schedule()
    }
}
