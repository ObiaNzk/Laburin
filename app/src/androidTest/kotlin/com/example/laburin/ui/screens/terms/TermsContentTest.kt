package com.example.laburin.ui.screens.terms

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun explainsThatBusinessesCanSeeTheCvAndGoesBack() {
        var back = 0
        composeRule.setContent { LaburinTheme { TermsContent(onBack = { back++ }) } }

        composeRule.onNodeWithText("Términos y condiciones").assertIsDisplayed()
        composeRule.onNodeWithText("van a poder ver tu CV", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(1, back)
    }
}
