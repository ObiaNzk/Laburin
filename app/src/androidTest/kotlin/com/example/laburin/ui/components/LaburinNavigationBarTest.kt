package com.example.laburin.ui.components

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaburinNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(selected: AppTab, isLoggedIn: Boolean) = composeRule.setContent {
        LaburinNavigationBar(
            selected = selected, isLoggedIn = isLoggedIn,
            onSearchClick = { clicks += "search" },
            onApplicationsClick = { clicks += "applications" },
            onAccountClick = { clicks += "account" },
        )
    }

    @Test
    fun loggedOutOffersToLogIn() {
        show(AppTab.SEARCH, isLoggedIn = false)

        composeRule.onNodeWithText("Ingresar").assertExists()
        composeRule.onNodeWithText("Perfil").assertDoesNotExist()
    }

    @Test
    fun loggedInShowsTheProfile() {
        show(AppTab.SEARCH, isLoggedIn = true)

        composeRule.onNodeWithText("Perfil").assertExists()
    }

    @Test
    fun marksTheCurrentTab() {
        show(AppTab.APPLICATIONS, isLoggedIn = true)

        composeRule.onNodeWithText("Postulaciones").assertIsSelected()
    }

    @Test
    fun eachTabNavigates() {
        show(AppTab.SEARCH, isLoggedIn = false)

        composeRule.onNodeWithText("Buscar").performClick()
        composeRule.onNodeWithText("Postulaciones").performClick()
        composeRule.onNodeWithText("Ingresar").performClick()

        assertEquals(listOf("search", "applications", "account"), clicks)
    }
}
