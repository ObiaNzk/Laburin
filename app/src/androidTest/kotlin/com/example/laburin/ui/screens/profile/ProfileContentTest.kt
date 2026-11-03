package com.example.laburin.ui.screens.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import java.time.Instant
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
        LaburinTheme {
            ProfileContent(
                state = state, onLogout = { clicks += "logout" }, onLogin = { clicks += "login" },
                onPickCv = { clicks += "pickCv" }, onMyOfferClick = { clicks += "offer:$it" },
            )
        }
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

    private val candidate = Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]")

    @Test
    fun candidateWithoutCvIsInvitedToUploadAPdf() {
        show(ProfileUiState.LoggedIn(candidate, cv = null))

        composeRule.onNodeWithText("Todavía no cargaste tu CV. Solo se aceptan archivos PDF.").assertIsDisplayed()
        composeRule.onNodeWithText("Cargar CV").performClick()

        assertEquals(listOf("pickCv"), clicks)
    }

    @Test
    fun candidateWithCvSeesItAndCanReplaceIt() {
        show(ProfileUiState.LoggedIn(candidate, cv = CvInfo("mi-cv.pdf")))

        composeRule.onNodeWithText("mi-cv.pdf").assertIsDisplayed()
        composeRule.onNodeWithText("Reemplazar CV").assertIsDisplayed()
    }

    @Test
    fun cvErrorsAreExplained() {
        show(ProfileUiState.LoggedIn(candidate, cv = null, cvError = CvError.NotPdf))

        composeRule.onNodeWithText("El archivo tiene que ser un PDF").assertIsDisplayed()
    }

    @Test
    fun aBusinessHasNoCvSection() {
        show(ProfileUiState.LoggedIn(Session("b-1", UserRole.BUSINESS, "[COMERCIO]", "[EMAIL]"), showsCv = false))

        composeRule.onNodeWithText("Mi CV").assertDoesNotExist()
    }

    @Test
    fun aBusinessSeesItsOffers() {
        val offer = PublishedOffer("p-1", "Mozo", Category.GASTRONOMY, "Sábados", "", PublishedOfferStatus.ACTIVE, Instant.EPOCH)
        show(ProfileUiState.LoggedIn(Session("b-1", UserRole.BUSINESS, "[COMERCIO]", "[EMAIL]"), showsCv = false, myOffers = listOf(offer)))

        composeRule.onNodeWithText("Mis publicaciones (1)").assertIsDisplayed()
        composeRule.onNodeWithText("Mozo").performClick()

        assertEquals(listOf("offer:p-1"), clicks)
    }

    @Test
    fun withoutSessionInvitesToLogIn() {
        show(ProfileUiState.LoggedOut)

        composeRule.onNodeWithText("Ingresar").performClick()

        assertEquals(listOf("login"), clicks)
    }
}
