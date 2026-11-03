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

enum class SyncOutcome { Done, Retry, AuthRequired }
