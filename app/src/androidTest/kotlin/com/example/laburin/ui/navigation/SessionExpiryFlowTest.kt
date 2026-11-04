package com.example.laburin.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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

/** Planning doc §13: if the session expired, ask to log in again without losing pending actions. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SessionExpiryFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            auth.acknowledgeSessionExpired()
            auth.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD)
        }
    }

    @Test
    fun anExpiredSessionAsksToLogInAgainAndLoggingInRestoresIt() {
        composeRule.waitFor("Perfil")

        runBlocking { auth.sessionExpired() }

        composeRule.waitFor("Tu sesión venció")
        composeRule.tap("Ingresar de nuevo")
        composeRule.waitFor("Ingresá a tu cuenta")
        composeRule.typeInField(0, FakeBackend.DEMO_CANDIDATE_EMAIL)
        composeRule.typeInField(1, FakeBackend.DEMO_PASSWORD)
        composeRule.tap("Ingresar")

        composeRule.waitFor("Perfil")
        composeRule.onNodeWithText("Tu sesión venció").assertDoesNotExist()
    }

    @Test
    fun theNoticeCanBePostponed() {
        composeRule.waitFor("Perfil")

        runBlocking { auth.sessionExpired() }
        composeRule.waitFor("Tu sesión venció")
        composeRule.tap("Más tarde")

        composeRule.waitUntil(5_000) { composeRule.onAllNodes(androidx.compose.ui.test.hasText("Tu sesión venció")).fetchSemanticsNodes().isEmpty() }
        composeRule.onNodeWithText("Ingresar").assertExists()
    }
}
