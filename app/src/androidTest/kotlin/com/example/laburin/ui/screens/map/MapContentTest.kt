package com.example.laburin.ui.screens.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.NearbyOffer
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MapContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: MapUiState, now: Instant = Instant.EPOCH) = composeRule.setContent {
        LaburinTheme {
            MapContent(
                state = state, now = now,
                onQueryChange = {}, onClearQuery = { clicks += "clearQuery" }, onRadiusChange = {},
                onCategoryChange = {}, onWidenRadius = { clicks += "widen" }, onShowAllCategories = {},
                onRetry = { clicks += "retry" }, onOfferClick = { clicks += "open:$it" }, onScanClick = {}, onPublishClick = {},
                onChooseZone = { clicks += "chooseZone" }, onDismissZone = { clicks += "dismissZone" },
                onZoneSearch = { clicks += "zone:$it" },
            )
        }
    }

    private val granted = MapUiState(locationStatus = LocationStatus.Granted)

    // --- Empty ---

    @Test
    fun emptyStateWithoutCategoryDoesNotMentionACategory() {
        show(granted.copy(radiusKm = 2, category = null, offers = OffersState.Empty))

        composeRule.onNodeWithText("No hay búsquedas a menos de 2 km").assertIsDisplayed()
    }

    @Test
    fun emptyStateWithCategoryNamesIt() {
        show(granted.copy(radiusKm = 5, category = Category.MECHANICS, offers = OffersState.Empty))

        composeRule.onNodeWithText("No hay búsquedas de mecánica a menos de 5 km").assertIsDisplayed()
    }

    @Test
    fun emptyStateWithoutSearchOffersToWidenTheRadius() {
        show(granted.copy(radiusKm = 2, offers = OffersState.Empty))

        composeRule.onNodeWithText("Ampliar a 5 km").performClick()

        assertEquals(listOf("widen"), clicks)
    }

    @Test
    fun emptyBecauseOfTheSearchNamesTheTextAndOffersToClearIt() {
        show(granted.copy(query = "zzz", offers = OffersState.Empty))

        composeRule.onNodeWithText("No hay búsquedas que coincidan con “zzz”").assertIsDisplayed()
        composeRule.onNodeWithText("Ampliar a 5 km").assertDoesNotExist()
        composeRule.onNodeWithText("Borrar búsqueda").performClick()

        assertEquals(listOf("clearQuery"), clicks)
    }

    // --- Offline / unavailable ---

    @Test
    fun offlineWithoutCacheUsesTheOfflineLayoutInsteadOfAnEmptyMap() {
        show(granted.copy(isOnline = false, offers = OffersState.Unavailable(isOffline = true)))

        composeRule.onNodeWithText("Sin conexión").assertIsDisplayed()
        composeRule.onNodeWithText("El mapa no está disponible sin conexión").assertIsDisplayed()
        composeRule.onNodeWithText("Necesitás conexión para cargar las búsquedas").assertIsDisplayed()
        composeRule.onNodeWithText("Buscar puesto").assertDoesNotExist()
    }

    @Test
    fun offlineWithoutCacheCanRetry() {
        show(granted.copy(isOnline = false, offers = OffersState.Unavailable(isOffline = true)))

        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(listOf("retry"), clicks)
    }

    @Test
    fun serverErrorWithoutCacheKeepsTheMapLayout() {
        show(granted.copy(offers = OffersState.Unavailable(isOffline = false)))

        composeRule.onNodeWithText("No pudimos cargar las búsquedas").assertIsDisplayed()
        composeRule.onNodeWithText("Buscar puesto").assertExists()
    }

    @Test
    fun offlineWithCacheShowsSavedOffersAndTheirAge() {
        val offer = Offer(
            id = "o-1", title = "Ayudante de taller", category = Category.MECHANICS, description = "", schedule = "",
            jobType = "", businessName = "[COMERCIO]", address = "[DIRECCION]",
            location = MapUiState.DEFAULT_CENTER, updatedAt = Instant.EPOCH,
        )
        show(
            granted.copy(
                isOnline = false, lastSyncedAt = Instant.EPOCH,
                offers = OffersState.Content(listOf(NearbyOffer(offer, 400))),
            ),
            now = Instant.EPOCH.plusSeconds(15 * 60),
        )

        composeRule.onNodeWithText("Sin conexión · datos de hace 15 min").assertIsDisplayed()
        composeRule.onNodeWithText("Ayudante de taller").assertIsDisplayed()
    }

    // --- Manual zone (RF-01) ---

    @Test
    fun withoutLocationTheUserCanChooseAZone() {
        show(MapUiState(locationStatus = LocationStatus.Denied, offers = OffersState.Empty))

        composeRule.onNodeWithText("Elegir zona").performClick()

        assertEquals(listOf("chooseZone"), clicks)
    }

    @Test
    fun aManualZoneShowsItsNameAndCanBeChanged() {
        show(MapUiState(locationStatus = LocationStatus.ManualZone, zoneName = "Palermo", offers = OffersState.Empty))

        composeRule.onNodeWithText("Mostrando: Palermo").assertIsDisplayed()
        composeRule.onNodeWithText("Cambiar zona").performClick()

        assertEquals(listOf("chooseZone"), clicks)
    }

    @Test
    fun theZoneDialogSearchesWhatTheUserTyped() {
        show(MapUiState(locationStatus = LocationStatus.Denied, offers = OffersState.Empty, zoneSearch = ZoneSearchState(isOpen = true)))

        composeRule.onNode(hasSetTextAction() and hasText("Barrio o dirección")).performTextInput("palermo")
        composeRule.onNodeWithText("Buscar").performClick()

        assertEquals(listOf("zone:palermo"), clicks)
    }

    @Test
    fun theZoneDialogExplainsAnUnknownZone() {
        show(
            MapUiState(
                locationStatus = LocationStatus.Denied, offers = OffersState.Empty,
                zoneSearch = ZoneSearchState(isOpen = true, error = ZoneError.NotFound),
            )
        )

        composeRule.onNodeWithText("No encontramos esa zona").assertIsDisplayed()
    }

    // --- Recently viewed (RF-02) ---

    private val viewed = NearbyOffer(
        Offer(
            id = "f-5", title = "Cocinero/a", category = Category.GASTRONOMY, description = "", schedule = "", jobType = "",
            businessName = "[COMERCIO]", address = "[DIRECCION]", location = MapUiState.DEFAULT_CENTER, updatedAt = Instant.EPOCH,
        ),
        distanceMeters = 2_700,
    )

    @Test
    fun eachOfferShowsItsSalaryOrThatItIsToBeAgreed() {
        val paid = viewed.copy(offer = viewed.offer.copy(id = "f-1", title = "Mozo/a", salary = Salary(600_000, 750_000, SalaryPeriod.MONTH)))
        show(granted.copy(offers = OffersState.Empty, recentlyViewed = listOf(paid, viewed)))

        composeRule.onNodeWithText("$ 600.000 – $ 750.000 por mes").assertExists()
        composeRule.onNodeWithText("A convenir").assertExists()
    }

    @Test
    fun recentlyViewedOffersAreListedAndOpenTheirDetail() {
        show(granted.copy(offers = OffersState.Empty, recentlyViewed = listOf(viewed)))

        composeRule.onNodeWithText("Vistas recientemente").assertExists()
        composeRule.onNodeWithText("Cocinero/a").performClick()

        assertEquals(listOf("open:f-5"), clicks)
    }

    @Test
    fun recentlyViewedOffersAreAvailableOfflineWithoutCache() {
        show(granted.copy(isOnline = false, offers = OffersState.Unavailable(isOffline = true), recentlyViewed = listOf(viewed)))

        composeRule.onNodeWithText("Vistas recientemente").assertExists()
        composeRule.onNodeWithText("Cocinero/a").assertExists()
    }

    @Test
    fun noRecentlyViewedNoSection() {
        show(granted.copy(offers = OffersState.Empty))

        composeRule.onNodeWithText("Vistas recientemente").assertDoesNotExist()
    }
}
