package com.example.laburin.ui.screens.publish

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PublishContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    // --- Publish form ---

    private fun showForm(state: PublishUiState) = composeRule.setContent {
        LaburinTheme {
            PublishContent(
                state = state, onTitleChange = {}, onCategoryChange = { clicks += "category:$it" }, onScheduleChange = {},
                onDescriptionChange = {}, onPublish = { clicks += "publish" }, onBack = { clicks += "back" },
            )
        }
    }

    @Test
    fun formPublishes() {
        showForm(PublishUiState())

        composeRule.onNodeWithText("Nueva búsqueda").assertIsDisplayed()
        composeRule.onNodeWithText("Publicar búsqueda").performScrollTo().performClick()

        assertEquals(listOf("publish"), clicks)
    }

    @Test
    fun categoryIsChosenFromTheList() {
        showForm(PublishUiState())

        composeRule.onNodeWithText("Elegí un rubro").performClick()
        composeRule.onNodeWithText("Gastronomía").performClick()

        assertEquals(listOf("category:GASTRONOMY"), clicks)
    }

    @Test
    fun missingFieldsAreExplained() {
        showForm(PublishUiState(fieldErrors = setOf(PublishField.TITLE, PublishField.CATEGORY)))

        composeRule.onNodeWithText("Indicá el puesto").assertExists()
        composeRule.onNodeWithText("Elegí el rubro").assertExists()
        composeRule.onNodeWithText("Indicá el horario").assertDoesNotExist()
    }

    // --- Business sign-up ---

    @Test
    fun businessSignUpExplainsErrorsAndSubmits() {
        composeRule.setContent {
            LaburinTheme {
                BusinessSignUpContent(
                    state = BusinessSignUpUiState(fieldErrors = setOf(BusinessSignUpField.ADDRESS), error = BusinessSignUpError.EmailTaken),
                    onNameChange = {}, onCategoryChange = {}, onScheduleChange = {}, onAddressChange = {}, onEmailChange = {},
                    onPasswordChange = {}, onSubmit = { clicks += "submit" }, onLogin = { clicks += "login" }, onClose = {},
                )
            }
        }

        composeRule.onNodeWithText("Registrá tu comercio").assertIsDisplayed()
        composeRule.onNodeWithText("Indicá la dirección del comercio").assertExists()
        composeRule.onNodeWithText("Ya existe una cuenta con ese email").assertExists()
        composeRule.onNodeWithText("Crear cuenta de comercio").performScrollTo().performClick()
        composeRule.onNodeWithText("Ya tengo cuenta").performScrollTo().performClick()

        assertEquals(listOf("submit", "login"), clicks)
    }

    // --- Published offer ---

    private fun showPublished(status: PublishedOfferStatus) = composeRule.setContent {
        LaburinTheme {
            PublishedOfferContent(
                state = PublishedOfferUiState.Content(
                    PublishedOffer("p-1", "Mozo", Category.GASTRONOMY, "Sábados", "[DESCRIPCION]", status, Instant.EPOCH),
                    qrLink = "laburin://offer/p-1",
                ),
                onShare = { clicks += "share" }, onBack = {},
            )
        }
    }

    @Test
    fun publishedOfferShowsItsQrAndCanBeShared() {
        showPublished(PublishedOfferStatus.ACTIVE)

        composeRule.onNodeWithText("Mozo").assertIsDisplayed()
        composeRule.onNodeWithText("Publicada").assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Código QR de la búsqueda")).assertIsDisplayed()
        composeRule.onNodeWithText("Compartir").performScrollTo().performClick()

        assertEquals(listOf("share"), clicks)
    }

    @Test
    fun pendingPublicationIsExplained() {
        showPublished(PublishedOfferStatus.PENDING_PUBLISH)

        composeRule.onNodeWithText("Pendiente de publicación").assertIsDisplayed()
        composeRule.onNodeWithText("Todavía no se ve en el mapa: se publica cuando vuelva la conexión.").assertExists()
    }
}
