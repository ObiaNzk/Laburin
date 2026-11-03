package com.example.laburin.ui.screens.auth

import com.example.laburin.domain.model.SignUpField

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: LoginError? = null,
)

enum class LoginError { MissingFields, InvalidCredentials, Network, Server }

sealed interface LoginEffect {
    data object LoggedIn : LoginEffect
}

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val fieldErrors: Set<SignUpField> = emptySet(),
    val isSubmitting: Boolean = false,
    val error: SignUpError? = null,
)

enum class SignUpError { EmailTaken, Network, Server }

sealed interface SignUpEffect {
    data object SignedUp : SignUpEffect
}
