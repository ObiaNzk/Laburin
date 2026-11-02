package com.example.laburin.ui.navigation

import android.Manifest
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.laburin.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/** End to end with the real Hilt graph (fake remote seed data around the default center). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfferNavigationTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
        .around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun tappingAnOfferOpensItsDetailAndSignUpThenGoesBackToTheMap() {
        waitFor("Vendedor/a")

        composeRule.onNodeWithText("Vendedor/a").performClick()
        waitFor("Creá tu cuenta para ver la búsqueda y postularte")
        composeRule.onNodeWithText("Indumentaria", substring = true).assertExists()

        composeRule.onNodeWithText("Crear cuenta").performClick()
        waitFor("Crear cuenta y seguir")

        pressBack()
        waitFor("Creá tu cuenta para ver la búsqueda y postularte")

        composeRule.onNodeWithText("Volver").performClick()
        waitFor("Vendedor/a")
    }

    private fun waitFor(text: String) =
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
}
