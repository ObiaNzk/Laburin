package com.example.laburin.domain.model

enum class UserRole { CANDIDATE, BUSINESS }

data class Session(val userId: String, val role: UserRole, val displayName: String, val email: String)

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidCredentials : AuthResult
    data object EmailAlreadyRegistered : AuthResult
    data object NetworkError : AuthResult
    data class ServerError(val code: Int) : AuthResult
}

enum class SignUpField { FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, TERMS }

/**
 * Terms and conditions a candidate accepts when signing up. Provisional: they let the businesses the candidate applies
 * to see their CV, contact details included, until the CV can be redacted. Same version as the server's.
 */
object Terms {
    const val CURRENT_VERSION = "2026-11-cv-v1"
}
