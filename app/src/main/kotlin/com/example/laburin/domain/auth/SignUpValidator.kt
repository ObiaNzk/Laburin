package com.example.laburin.domain.auth

import com.example.laburin.domain.model.SignUpField

object SignUpValidator {
    const val MIN_PASSWORD_LENGTH = 8
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun validate(firstName: String, lastName: String, email: String, password: String): Set<SignUpField> = buildSet {
        if (firstName.isBlank()) add(SignUpField.FIRST_NAME)
        if (lastName.isBlank()) add(SignUpField.LAST_NAME)
        if (!EMAIL.matches(email.trim())) add(SignUpField.EMAIL)
        if (password.length < MIN_PASSWORD_LENGTH) add(SignUpField.PASSWORD)
    }
}
