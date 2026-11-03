package com.example.laburin.ui.screens.auth

import app.cash.turbine.test
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = LoginViewModel(auth)

    private fun fill() {
        vm.onEmailChange("persona@demo.laburin")
        vm.onPasswordChange("12345678")
    }

    @Test
    fun `empty fields are not sent`() = runTest {
        vm.onSubmit()

        assertEquals(LoginError.MissingFields, vm.uiState.value.error)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun `valid credentials log in and close the screen`() = runTest {
        fill()

        vm.effects.test {
            vm.onSubmit()
            assertEquals(LoginEffect.LoggedIn, awaitItem())
        }
        assertEquals(listOf("login:persona@demo.laburin"), auth.calls)
    }

    @Test
    fun `shows progress while logging in and ignores a second tap`() = runTest {
        val gate = CompletableDeferred<Unit>()
        auth.gate = { gate.await() }
        fill()

        vm.onSubmit()
        vm.onSubmit()

        assertTrue(vm.uiState.value.isSubmitting)
        assertEquals(1, auth.calls.size)
        gate.complete(Unit)
    }

    @Test
    fun `wrong credentials show an error and stop the progress`() = runTest {
        auth.nextResult = AuthResult.InvalidCredentials
        fill()

        vm.onSubmit()

        assertEquals(LoginError.InvalidCredentials, vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.isSubmitting)
    }

    @Test
    fun `no network says a connection is needed`() = runTest {
        auth.nextResult = AuthResult.NetworkError
        fill()

        vm.onSubmit()

        assertEquals(LoginError.Network, vm.uiState.value.error)
    }

    @Test
    fun `server errors are shown as a generic failure`() = runTest {
        auth.nextResult = AuthResult.ServerError(500)
        fill()

        vm.onSubmit()

        assertEquals(LoginError.Server, vm.uiState.value.error)
    }

    @Test
    fun `editing a field clears the error`() = runTest {
        vm.onSubmit()

        vm.onEmailChange("p")

        assertEquals(null, vm.uiState.value.error)
    }
}
