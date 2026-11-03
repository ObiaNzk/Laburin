package com.example.laburin.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.SignUpField
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun showLogin(state: LoginUiState, demoHint: String? = null) = composeRule.setContent {
        LaburinTheme {
            LoginContent(
                state = state, demoHint = demoHint,
                onEmailChange = {}, onPasswordChange = {},
                onSubmit = { clicks += "submit" }, onSignUpClick = { clicks += "signUp" }, onClose = { clicks += "close" },
            )
        }
    }

    private fun showSignUp(state: SignUpUiState) = composeRule.setContent {
        LaburinTheme {
            SignUpContent(
                state = state,
                onFirstNameChange = {}, onLastNameChange = {}, onEmailChange = {}, onPasswordChange = {},
                onSubmit = { clicks += "submit" }, onClose = { clicks += "close" },
            )
        }
    }

    // --- Login ---

    @Test
    fun loginSubmitsAndLinksToSignUp() {
        showLogin(LoginUiState())

        composeRule.onNodeWithText("Ingresar").performClick()
        composeRule.onNodeWithText("No tengo cuenta").performClick()

        assertEquals(listOf("submit", "signUp"), clicks)
    }

    @Test
    fun loginExplainsEachError() {
        val messages = mapOf(
            LoginError.MissingFields to "Completá tu email y contraseña",
            LoginError.InvalidCredentials to "Email o contraseña incorrectos",
            LoginError.Network to "Necesitás conexión para ingresar",
            LoginError.Server to "No pudimos ingresar. Probá de nuevo.",
        )
        var state by mutableStateOf(LoginUiState())
        composeRule.setContent {
            LaburinTheme { LoginContent(state, null, {}, {}, {}, {}, {}) }
        }
        messages.forEach { (error, text) ->
            state = LoginUiState(error = error)
            composeRule.onNodeWithText(text).assertIsDisplayed()
        }
    }

    @Test
    fun loginDisablesTheButtonWhileSubmitting() {
        showLogin(LoginUiState(isSubmitting = true))

        composeRule.onNodeWithText("Ingresar").assertIsNotEnabled()
    }

    @Test
    fun loginShowsTheDemoAccountHintWhenGiven() {
        showLogin(LoginUiState(), demoHint = "Cuenta demo: demo / demo")

        composeRule.onNodeWithText("Cuenta demo: demo / demo").assertIsDisplayed()
    }

    // --- Sign up ---

    @Test
    fun signUpMarksInvalidFields() {
        showSignUp(SignUpUiState(fieldErrors = setOf(SignUpField.EMAIL, SignUpField.PASSWORD)))

        composeRule.onNodeWithText("Ingresá un email válido").assertExists()
        composeRule.onNodeWithText("La contraseña necesita al menos 8 caracteres").assertExists()
        composeRule.onNodeWithText("Ingresá tu nombre").assertDoesNotExist()
    }

    @Test
    fun signUpExplainsATakenEmail() {
        showSignUp(SignUpUiState(error = SignUpError.EmailTaken))

        composeRule.onNodeWithText("Ya existe una cuenta con ese email").assertExists()
    }

    @Test
    fun signUpSubmitsAndCloses() {
        showSignUp(SignUpUiState())

        composeRule.onNodeWithText("Crear cuenta y seguir").performClick()
        composeRule.onNodeWithContentDescription("Cerrar").performClick()

        assertEquals(listOf("submit", "close"), clicks)
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescription(text: String) =
        onNode(androidx.compose.ui.test.hasContentDescription(text))
}
