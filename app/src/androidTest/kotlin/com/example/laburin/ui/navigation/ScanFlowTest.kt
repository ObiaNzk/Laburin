package com.example.laburin.ui.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ScanFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA))
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { auth.logout() }
    }

    @Test
    fun theQrButtonOpensTheScannerAndCloseGoesBackToTheMap() {
        composeRule.waitFor("Vendedor/a")
        composeRule.onNode(hasContentDescription("Escanear QR del cartel")).performClick()

        composeRule.waitFor("Apuntá al QR del cartel")
        composeRule.onNode(hasContentDescription("Cerrar")).performClick()

        composeRule.waitFor("¿Buscás empleados? Publicá gratis")
    }
}

/** The system camera app (or any QR reader) opens the offer directly through the deep link. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OfferDeepLinkTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createEmptyComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { auth.logout() }
    }

    @Test
    fun anOfferLinkOpensThatOffersDetail() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("laburin://offer/f-4"))
            .setClass(ApplicationProvider.getApplicationContext(), MainActivity::class.java)

        ActivityScenario.launch<MainActivity>(intent).use {
            composeRule.waitUntil(15_000) {
                composeRule.onAllNodes(androidx.compose.ui.test.hasText("Vendedor/a")).fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Creá tu cuenta para ver la búsqueda y postularte").assertExists()
        }
    }
}
