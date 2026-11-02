package com.example.laburin.ui.screens.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Category
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MapContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: MapUiState) = composeRule.setContent {
        LaburinTheme {
            MapContent(
                state = state, now = Instant.EPOCH,
                onQueryChange = {}, onRadiusChange = {}, onCategoryChange = {}, onWidenRadius = {},
                onShowAllCategories = {}, onRetry = {}, onOfferClick = {}, onScanClick = {}, onPublishClick = {},
            )
        }
    }

    @Test
    fun emptyStateWithoutCategoryDoesNotMentionACategory() {
        show(MapUiState(radiusKm = 2, category = null, locationStatus = LocationStatus.Granted, offers = OffersState.Empty))

        composeRule.onNodeWithText("No hay búsquedas a menos de 2 km").assertIsDisplayed()
    }

    @Test
    fun emptyStateWithCategoryNamesIt() {
        show(
            MapUiState(
                radiusKm = 5, category = Category.MECHANICS,
                locationStatus = LocationStatus.Granted, offers = OffersState.Empty,
            )
        )

        composeRule.onNodeWithText("No hay búsquedas de mecánica a menos de 5 km").assertIsDisplayed()
    }
}
