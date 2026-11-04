package com.example.laburin.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.MainActivity
import com.example.laburin.domain.repository.AuthRepository
import com.example.laburin.domain.repository.FiltersRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

/** RF-02: an offer seen before stays reachable after leaving its radius. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecentlyViewedFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var filters: FiltersRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            auth.logout()
            filters.setRadiusKm(5)
        }
    }

    @After
    fun tearDown() = runBlocking { filters.setRadiusKm(2) } // Filters persist on the device.

    @Test
    fun anOfferSeenFartherAwayStaysListedAfterNarrowingTheRadius() {
        composeRule.waitFor("5 búsquedas a menos de 5 km")
        // The results list is lazy: bring the farther offer on screen first.
        composeRule.onAllNodes(hasScrollToNodeAction())[0].performScrollToNode(hasText("Cocinero/a"))
        composeRule.onNodeWithText("Cocinero/a").performClick()
        composeRule.waitFor("Creá tu cuenta para ver la búsqueda y postularte")
        composeRule.tap("Volver")

        composeRule.waitFor("Radio: 5 km")
        composeRule.tap("Radio: 5 km")
        composeRule.tap("2 km")

        composeRule.waitFor("Vistas recientemente")
        composeRule.waitFor("Cocinero/a")
    }
}
