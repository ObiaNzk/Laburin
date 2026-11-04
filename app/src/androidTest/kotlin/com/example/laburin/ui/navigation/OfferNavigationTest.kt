package com.example.laburin.ui.navigation

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.MainActivity
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

/** End to end with the real Hilt graph (fake remote seed data around the default center). */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfferNavigationTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject
    lateinit var auth: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { auth.logout() } // The session is persisted on the device between tests.
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
