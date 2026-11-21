package com.example.laburin.domain.auth

import com.example.laburin.domain.model.SignUpField
import org.junit.Assert.assertEquals
import org.junit.Test

class SignUpValidatorTest {

    private fun errors(
        firstName: String = "Ana",
        lastName: String = "Pérez",
        email: String = "ana@demo.laburin",
        password: String = "12345678",
        termsAccepted: Boolean = true,
    ) = SignUpValidator.validate(firstName, lastName, email, password, termsAccepted)

    @Test
    fun `valid data has no errors`() {
        assertEquals(emptySet<SignUpField>(), errors())
    }

    @Test
    fun `blank names are required`() {
        assertEquals(setOf(SignUpField.FIRST_NAME, SignUpField.LAST_NAME), errors(firstName = " ", lastName = ""))
    }

    @Test
    fun `email must look like an email`() {
        assertEquals(setOf(SignUpField.EMAIL), errors(email = "sin-arroba"))
    }

    @Test
    fun `the terms must be accepted to sign up`() {
        assertEquals(setOf(SignUpField.TERMS), errors(termsAccepted = false))
    }

    @Test
    fun `password needs at least 8 characters`() {
        assertEquals(setOf(SignUpField.PASSWORD), errors(password = "1234567"))
    }
}
