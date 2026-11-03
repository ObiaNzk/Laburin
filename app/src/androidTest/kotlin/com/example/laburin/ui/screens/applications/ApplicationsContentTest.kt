package com.example.laburin.ui.screens.applications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class ApplicationsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: ApplicationsUiState) = composeRule.setContent {
        LaburinTheme {
            ApplicationsContent(state = state, onLogin = { clicks += "login" }, onApplicationClick = { clicks += "open:$it" })
        }
    }

    private fun app(id: String, title: String, status: ApplicationStatus) =
        JobApplication(id, "o-$id", title, "[COMERCIO]", status, Instant.EPOCH)

    @Test
    fun eachStatusHasAClearLabel() {
        show(
            ApplicationsUiState.Content(
                listOf(
                    app("1", "Mozo", ApplicationStatus.PENDING_SEND),
                    app("2", "Cajero", ApplicationStatus.SENT),
                    app("3", "Vendedor", ApplicationStatus.APPROVED),
                    app("4", "Repositor", ApplicationStatus.REJECTED),
                    app("5", "Cocinero", ApplicationStatus.OFFER_CLOSED),
                ),
                pendingToSend = 1,
            )
        )

        listOf("Pendiente de envío", "Postulación enviada", "Postulación aprobada", "Postulación rechazada", "Búsqueda cerrada")
            .forEach { composeRule.onNodeWithText(it).assertExists() }
    }

    @Test
    fun pendingSendsAreAnnounced() {
        show(ApplicationsUiState.Content(listOf(app("1", "Mozo", ApplicationStatus.PENDING_SEND)), pendingToSend = 1))

        composeRule.onNodeWithText("1 postulación se enviará al reconectar").assertIsDisplayed()
    }

    @Test
    fun noPendingSendsHasNoBanner() {
        show(ApplicationsUiState.Content(listOf(app("1", "Mozo", ApplicationStatus.SENT)), pendingToSend = 0))

        composeRule.onNodeWithText("se enviará al reconectar", substring = true).assertDoesNotExist()
    }

    @Test
    fun approvedExplainsWhatHappensNext() {
        show(ApplicationsUiState.Content(listOf(app("1", "Mozo", ApplicationStatus.APPROVED)), pendingToSend = 0))

        composeRule.onNodeWithText("El comercio recibió tu contacto y te va a escribir para la entrevista.").assertExists()
    }

    @Test
    fun tappingAnApplicationOpensItsOffer() {
        show(ApplicationsUiState.Content(listOf(app("1", "Mozo", ApplicationStatus.SENT)), pendingToSend = 0))

        composeRule.onNodeWithText("Mozo").performClick()

        assertEquals(listOf("open:o-1"), clicks)
    }

    @Test
    fun emptyInvitesToSearch() {
        show(ApplicationsUiState.Empty)

        composeRule.onNodeWithText("Todavía no te postulaste a ninguna búsqueda").assertIsDisplayed()
    }

    @Test
    fun loggedOutInvitesToLogIn() {
        show(ApplicationsUiState.LoggedOut)

        composeRule.onNodeWithText("Ingresar").performClick()

        assertEquals(listOf("login"), clicks)
    }
}
