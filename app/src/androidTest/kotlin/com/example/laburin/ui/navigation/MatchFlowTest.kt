package com.example.laburin.ui.navigation

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.laburin.MainActivity
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.data.local.LaburinDatabase
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

/** RF-04 / RNF-05 end to end: both sides of a match on one device, with the contact revealed only after confirmation. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MatchFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(GrantPermissionRule.grant(Manifest.permission.ACCESS_COARSE_LOCATION))
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var db: LaburinDatabase
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context, Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(workerFactory).build(),
        )
        db.openHelper.writableDatabase.apply {
            listOf("applications", "pending_actions", "published_offers", "applicants").forEach { execSQL("DELETE FROM $it") }
        }
        runBlocking { auth.logout() }
    }

    @Test
    fun theBusinessApprovesAnApplicantAndOnlyThenSeesTheContact() {
        // Candidate applies.
        runBlocking { auth.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD) }
        composeRule.waitFor("Vendedor/a")
        composeRule.onNodeWithText("Vendedor/a").performClick()
        composeRule.tap("Postularme")
        composeRule.waitFor("Pendiente de envío")
        connectionIsBack(context)
        composeRule.waitFor("Postulación enviada")
        composeRule.tap("Volver")

        // Business reviews.
        runBlocking {
            auth.logout()
            auth.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD)
        }
        composeRule.waitFor("Perfil")
        composeRule.tap("Perfil")
        composeRule.waitFor("Vendedor/a")
        composeRule.tap("Vendedor/a")
        composeRule.waitFor("Postulante Demo")
        composeRule.onNodeWithText("Contacto oculto hasta aprobar").assertExists()

        composeRule.tap("Aprobar")
        composeRule.waitFor("Aprobación pendiente de envío")
        composeRule.onNodeWithText("Contacto: ${FakeBackend.DEMO_CANDIDATE_EMAIL}").assertDoesNotExist()

        connectionIsBack(context)
        composeRule.waitFor("Contacto: ${FakeBackend.DEMO_CANDIDATE_EMAIL}")

        // Candidate sees the match.
        runBlocking {
            auth.logout()
            auth.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD)
        }
        composeRule.tap("Volver")
        composeRule.waitFor("Postulaciones")
        composeRule.tap("Postulaciones")
        composeRule.waitFor("Postulación aprobada")
    }
}
