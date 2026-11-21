package com.example.laburin.domain.model

import java.time.Instant

enum class ApplicationStatus { PENDING_SEND, SENT, APPROVED, REJECTED, OFFER_CLOSED }

data class JobApplication(
    val id: String,
    val offerId: String,
    val offerTitle: String,
    val businessName: String,
    val status: ApplicationStatus,
    val createdAt: Instant,
)

sealed interface ApplyResult {
    data object NeedsAccount : ApplyResult
    data object NotACandidate : ApplyResult
    data object NeedsCv : ApplyResult
    data class AlreadyApplied(val application: JobApplication) : ApplyResult
    data class Queued(val application: JobApplication) : ApplyResult
}

data class CvInfo(val fileName: String)

sealed interface CvUploadResult {
    data object Success : CvUploadResult
    data object NotPdf : CvUploadResult
    data object TooLarge : CvUploadResult
    data object NetworkError : CvUploadResult
    data class ServerError(val code: Int) : CvUploadResult
}

/** The user's own CV, downloaded to be shown in the app. */
sealed interface CvFileResult {
    /** [path] is a private file of the app, deleted with [com.example.laburin.domain.repository.CvRepository.closeFile]. */
    data class Ready(val path: String) : CvFileResult
    data object NotUploaded : CvFileResult
    data object NetworkError : CvFileResult
    data class ServerError(val code: Int) : CvFileResult
}

enum class SyncOutcome { Done, Retry, AuthRequired }
