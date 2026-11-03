package com.example.laburin.domain.model

enum class ApplicantStatus { PENDING_REVIEW, APPROVAL_PENDING_SEND, REJECTION_PENDING_SEND, APPROVED, REJECTED }

/** [contactEmail] is only known after the server confirms the match (RNF-05). */
data class Applicant(
    val applicationId: String,
    val candidateName: String,
    val cvFileName: String?,
    val status: ApplicantStatus,
    val contactEmail: String?,
)
