package com.example.laburin.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: ProfileUiState) = composeRule.setContent {
        LaburinTheme { ProfileContent(state = state, onLogout = { clicks += "logout" }, onLogin = { clicks += "login" }) }
    }

    @Test
    fun showsNameAndEmail() {
        show(ProfileUiState.LoggedIn(Session("u-1", UserRole.CANDIDATE, "Postulante Demo", "persona@demo.laburin")))

        composeRule.onNodeWithText("Postulante Demo").assertIsDisplayed()
        composeRule.onNodeWithText("persona@demo.laburin").assertIsDisplayed()
    }

    @Test
    fun logoutEndsTheSession() {
        show(ProfileUiState.LoggedIn(Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]")))

        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()

        assertEquals(listOf("logout"), clicks)
    }

    @Test
    fun withoutSessionInvitesToLogIn() {
        show(ProfileUiState.LoggedOut)

        composeRule.onNodeWithText("Ingresar").performClick()

        assertEquals(listOf("login"), clicks)
    }
}
