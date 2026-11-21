package com.example.laburin.ui.screens.myoffers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class MyOffersContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: MyOffersUiState) = composeRule.setContent {
        LaburinTheme {
            MyOffersContent(state = state, onOfferClick = { clicks += "offer:$it" }, onPublish = { clicks += "publish" })
        }
    }

    private fun offer(id: String, title: String, status: PublishedOfferStatus) =
        PublishedOffer(id, title, Category.GASTRONOMY, "Sábados", "[DESCRIPCION]", status, Instant.EPOCH)

    @Test
    fun listsThePublicationsWithTheirStatusAndOpensThem() {
        show(MyOffersUiState.Content(listOf(offer("p-1", "Mozo/a de salón", PublishedOfferStatus.ACTIVE), offer("p-2", "Cajero/a", PublishedOfferStatus.CLOSED))))

        composeRule.onNodeWithText("Publicada").assertIsDisplayed()
        composeRule.onNodeWithText("Cerrada").assertIsDisplayed()
        composeRule.onNodeWithText("Mozo/a de salón").performClick()

        assertEquals(listOf("offer:p-1"), clicks)
    }

    @Test
    fun withoutPublicationsItInvitesToPublish() {
        show(MyOffersUiState.Empty)

        composeRule.onNodeWithText("Todavía no publicaste ninguna búsqueda.").assertIsDisplayed()
        composeRule.onNodeWithText("Publicar búsqueda").performClick()

        assertEquals(listOf("publish"), clicks)
    }
}
