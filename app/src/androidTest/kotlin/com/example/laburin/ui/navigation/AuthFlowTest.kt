package com.example.laburin.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.MainActivity
import com.example.laburin.data.fakebackend.FakeBackend
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
class AuthFlowTest {

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
    fun loggingInFromAnOfferReturnsToItsFullDetail() {
        openOffer("Vendedor/a")
        composeRule.tap("Ya tengo cuenta")
        composeRule.waitFor("Ingresá a tu cuenta")

        composeRule.typeInField(0, FakeBackend.DEMO_CANDIDATE_EMAIL)
        composeRule.typeInField(1, FakeBackend.DEMO_PASSWORD)
        composeRule.tap("Ingresar")

        composeRule.waitFor("Qué buscamos") // Only the full (logged-in) detail shows it.
        composeRule.onNodeWithText("Creá tu cuenta para ver la búsqueda y postularte").assertDoesNotExist()
    }

    @Test
    fun signingUpFromAnOfferReturnsToItsFullDetail() {
        openOffer("Vendedor/a")
        composeRule.tap("Crear cuenta")
        composeRule.waitFor("Crear cuenta y seguir")

        composeRule.typeInField(0, "Ana")
        composeRule.typeInField(1, "Pérez")
        composeRule.typeInField(2, "ana.e2e@demo.laburin")
        composeRule.typeInField(3, "12345678")
        composeRule.tap("Crear cuenta y seguir")

        composeRule.waitFor("Qué buscamos") // Only the full (logged-in) detail shows it.
    }

    @Test
    fun theBottomBarLogsInShowsTheProfileAndLogsOut() {
        composeRule.waitFor("Vendedor/a")
        composeRule.tap("Ingresar")
        composeRule.waitFor("Ingresá a tu cuenta")
        composeRule.typeInField(0, FakeBackend.DEMO_CANDIDATE_EMAIL)
        composeRule.typeInField(1, FakeBackend.DEMO_PASSWORD)
        composeRule.tap("Ingresar")

        composeRule.waitFor("Perfil")
        composeRule.tap("Perfil")
        composeRule.waitFor("Postulante Demo")
        composeRule.tap("Cerrar sesión")

        composeRule.waitFor("Ingresar")
        composeRule.onNodeWithText("Perfil").assertDoesNotExist()
    }

    private fun openOffer(title: String) {
        composeRule.waitFor(title)
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitFor("Creá tu cuenta para ver la búsqueda y postularte")
    }
}
