package com.example.laburin.model

enum class ApplicationStatus {
    PENDING,
    SENT,
    APPROVED,
    CLOSED
}

data class JobApplication (
    val offer: Offer,
    val status: ApplicationStatus
)