package com.example.laburin.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.SignUpField
import com.example.laburin.domain.model.Terms
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.afterProcessDeath
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = SignUpViewModel(SavedStateHandle(), auth)

    private fun fillValid() {
        vm.onFirstNameChange("Ana")
        vm.onLastNameChange("Pérez")
        vm.onEmailChange("ana@demo.laburin")
        vm.onPasswordChange("12345678")
        vm.onTermsAcceptedChange(true)
    }

    @Test
    fun `the sign-up form survives Android killing the process but the password does not`() = runTest {
        val state = SavedStateHandle()
        SignUpViewModel(state, auth).apply {
            onFirstNameChange("[NOMBRE]")
            onLastNameChange("[APELLIDO]")
            onEmailChange("postulante@demo.laburin")
            onPasswordChange("12345678")
        }

        val restored = SignUpViewModel(state.afterProcessDeath(), auth).uiState.value

        assertEquals(
            listOf("[NOMBRE]", "[APELLIDO]", "postulante@demo.laburin", ""),
            listOf(restored.firstName, restored.lastName, restored.email, restored.password),
        )
        assertTrue(state.keys().none { state.get<String>(it) == "12345678" })
    }

    @Test
    fun `invalid fields are marked and nothing is sent`() = runTest {
        vm.onEmailChange("sin-arroba")

        vm.onSubmit()

        assertEquals(
            setOf(SignUpField.FIRST_NAME, SignUpField.LAST_NAME, SignUpField.EMAIL, SignUpField.PASSWORD, SignUpField.TERMS),
            vm.uiState.value.fieldErrors,
        )
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `without accepting the terms nothing is sent`() = runTest {
        fillValid()
        vm.onTermsAcceptedChange(false)

        vm.onSubmit()

        assertEquals(setOf(SignUpField.TERMS), vm.uiState.value.fieldErrors)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `valid data creates the account and continues`() = runTest {
        fillValid()

        vm.effects.test {
            vm.onSubmit()
            assertEquals(SignUpEffect.SignedUp, awaitItem())
        }
        assertEquals(listOf("signUp:Ana Pérez:ana@demo.laburin:${Terms.CURRENT_VERSION}"), auth.calls)
    }

    @Test
    fun `a taken email is reported`() = runTest {
        auth.nextResult = AuthResult.EmailAlreadyRegistered
        fillValid()

        vm.onSubmit()

        assertEquals(SignUpError.EmailTaken, vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.isSubmitting)
    }

    @Test
    fun `no network says a connection is needed`() = runTest {
        auth.nextResult = AuthResult.NetworkError
        fillValid()

        vm.onSubmit()

        assertEquals(SignUpError.Network, vm.uiState.value.error)
    }

    @Test
    fun `editing a field clears only its error`() = runTest {
        vm.onSubmit()

        vm.onFirstNameChange("Ana")

        assertEquals(setOf(SignUpField.LAST_NAME, SignUpField.EMAIL, SignUpField.PASSWORD, SignUpField.TERMS), vm.uiState.value.fieldErrors)
    }
}
