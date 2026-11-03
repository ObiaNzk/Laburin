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

enum class SignUpField { FIRST_NAME, LAST_NAME, EMAIL, PASSWORD }
