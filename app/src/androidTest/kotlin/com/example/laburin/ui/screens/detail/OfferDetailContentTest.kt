package com.example.laburin.ui.screens.detail

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class OfferDetailContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val offer = Offer(
        id = "o-1", title = "Ayudante de taller", category = Category.MECHANICS,
        description = "Buscamos alguien con ganas de aprender", schedule = "Lunes a viernes, 8 a 17 h",
        jobType = "Jornada completa", businessName = "[COMERCIO]", address = "[DIRECCION]",
        location = GeoLocation(-34.6037, -58.3816), updatedAt = Instant.EPOCH,
    )

    private val clicks = mutableListOf<String>()

    private fun show(state: OfferDetailUiState) = composeRule.setContent {
        LaburinTheme {
            OfferDetailContent(
                state = state,
                onBack = { clicks += "back" },
                onCreateAccount = { clicks += "createAccount" },
                onLogin = { clicks += "login" },
                onApply = { clicks += "apply" },
                onRetry = { clicks += "retry" },
            )
        }
    }

    // --- Guest (RF-02: details need an account) ---

    @Test
    fun guestSeesTitleCategoryAndDistance() {
        show(OfferDetailUiState.Guest(offer.title, offer.category, distanceMeters = 400))

        composeRule.onNodeWithText("Ayudante de taller").assertIsDisplayed()
        composeRule.onNodeWithText("Mecánica · a 400 m").assertIsDisplayed()
    }

    @Test
    fun guestIsInvitedToCreateAnAccountAndCannotApply() {
        show(OfferDetailUiState.Guest(offer.title, offer.category, distanceMeters = 400))

        composeRule.onNodeWithText("Creá tu cuenta para ver la búsqueda y postularte").assertIsDisplayed()
        composeRule.onNodeWithText("Postularme").assertDoesNotExist()
    }

    @Test
    fun guestButtonsGoToSignUpAndLogin() {
        show(OfferDetailUiState.Guest(offer.title, offer.category, distanceMeters = 400))

        composeRule.onNodeWithText("Crear cuenta").performClick()
        composeRule.onNodeWithText("Ya tengo cuenta").performClick()

        assertEquals(listOf("createAccount", "login"), clicks)
    }

    @Test
    fun subtitleOmitsDistanceWhenUnknown() {
        show(OfferDetailUiState.Guest(offer.title, offer.category, distanceMeters = null))

        composeRule.onNodeWithText("Mecánica").assertIsDisplayed()
    }

    // --- Full (logged in) ---

    @Test
    fun fullShowsBusinessScheduleAndDescription() {
        show(OfferDetailUiState.Full(offer, distanceMeters = 1_200))

        composeRule.onNodeWithText("[COMERCIO] · a 1.2 km").assertIsDisplayed()
        composeRule.onNodeWithText("Lunes a viernes, 8 a 17 h").assertExists()
        composeRule.onNodeWithText("Buscamos alguien con ganas de aprender").assertExists()
        composeRule.onNodeWithText("Creá tu cuenta para ver la búsqueda y postularte").assertDoesNotExist()
    }

    @Test
    fun fullApplyButtonIsVisibleAndApplies() {
        show(OfferDetailUiState.Full(offer, distanceMeters = 1_200))

        composeRule.onNodeWithText("Postularme").assertIsDisplayed().performClick()

        assertEquals(listOf("apply"), clicks)
    }

    // --- Other states ---

    @Test
    fun loadingShowsAProgressIndicator() {
        show(OfferDetailUiState.Loading)

        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun noLongerAvailableExplainsIt() {
        show(OfferDetailUiState.NoLongerAvailable)

        composeRule.onNodeWithText("Esta búsqueda ya no está disponible").assertIsDisplayed()
    }

    @Test
    fun unavailableOfflineAsksForConnectionAndRetries() {
        show(OfferDetailUiState.Unavailable(isOffline = true))

        composeRule.onNodeWithText("Necesitás conexión para ver esta búsqueda").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(listOf("retry"), clicks)
    }

    @Test
    fun unavailableServerErrorSaysItCouldNotLoad() {
        show(OfferDetailUiState.Unavailable(isOffline = false))

        composeRule.onNodeWithText("No pudimos cargar esta búsqueda").assertIsDisplayed()
    }

    @Test
    fun backButtonGoesBack() {
        show(OfferDetailUiState.NoLongerAvailable)

        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(listOf("back"), clicks)
    }
}
