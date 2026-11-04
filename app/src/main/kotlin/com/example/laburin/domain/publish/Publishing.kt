package com.example.laburin.domain.publish

import com.example.laburin.domain.auth.SignUpValidator
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishResult
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.domain.model.EditResult
import com.example.laburin.domain.model.SignUpField
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.ApplicantsRepository
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.domain.repository.SessionRepository
import com.example.laburin.domain.repository.SyncScheduler
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject

/**
 * The text encoded in the QR printed on the shop sign: a web link, so any phone camera can open it.
 *
 * There is no LaburIN domain yet, so [WEB_BASE] is a placeholder image. Printed QRs can't change: when the domain
 * exists, change [WEB_BASE] and move the old value to [PREVIOUS_WEB_BASES] so the app keeps reading signs already
 * printed. Then add the domain's App Link to the manifest so the camera opens the app directly.
 */
object OfferLink {
    private const val WEB_BASE = "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg"
    private val PREVIOUS_WEB_BASES = emptyList<String>()
    private const val PARAM = "oferta"

    /** Links printed before the web link; also the app's deep link. */
    private const val APP_PREFIX = "laburin://offer/"

    fun build(offerId: String): String = "$WEB_BASE?$PARAM=${URLEncoder.encode(offerId, Charsets.UTF_8.name())}"

    fun parse(text: String): String? {
        val link = text.trim()
        return if (link.startsWith(APP_PREFIX)) parseAppLink(link) else parseWebLink(link)
    }

    private fun parseAppLink(link: String) = link.removePrefix(APP_PREFIX)
        .trimEnd('/')
        .takeIf { it.isNotEmpty() && '/' !in it }

    private fun parseWebLink(link: String): String? {
        val base = link.substringBefore('?')
        if (base != WEB_BASE && base !in PREVIOUS_WEB_BASES) return null
        return link.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .firstNotNullOfOrNull { pair ->
                pair.split('=', limit = 2).takeIf { it.size == 2 && it[0] == PARAM }?.get(1)
            }
            ?.let { URLDecoder.decode(it, Charsets.UTF_8.name()) }
            ?.takeIf { it.isNotBlank() }
    }
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

class EditOfferUseCase @Inject constructor(
    private val session: SessionRepository,
    private val offers: MyOffersRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(offerId: String, draft: OfferDraft): EditResult {
        if (session.session.first()?.role != UserRole.BUSINESS) return EditResult.NeedsBusinessAccount
        val offer = offers.observe(offerId).first() ?: return EditResult.NotFound
        if (offer.status.isClosing()) return EditResult.Closed
        val invalid = OfferDraftValidator.validate(draft)
        if (invalid.isNotEmpty()) return EditResult.Invalid(invalid)
        offers.edit(offerId, draft)
        scheduler.schedule()
        return EditResult.Queued
    }
}

class CloseOfferUseCase @Inject constructor(
    private val offers: MyOffersRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(offerId: String) {
        val offer = offers.observe(offerId).first() ?: return
        if (offer.status.isClosing()) return
        offers.close(offerId)
        scheduler.schedule()
    }
}

/** Closed, or closed locally and waiting to be sent: either way it can't change anymore. */
fun PublishedOfferStatus.isClosing() = this == PublishedOfferStatus.CLOSED || this == PublishedOfferStatus.CLOSE_PENDING_SEND

class ReviewApplicantUseCase @Inject constructor(
    private val applicants: ApplicantsRepository,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(applicationId: String, approve: Boolean) {
        applicants.review(applicationId, approve)
        scheduler.schedule()
    }
}
