package com.example.laburin.ui.navigation

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import javax.inject.Inject

/** RF-03 end to end: the business edits and then closes an offer; both go through the outbox to the server. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ManageOfferFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var db: LaburinDatabase
    @Inject lateinit var backend: FakeBackend
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
    fun theBusinessEditsAndThenClosesAnOffer() {
        runBlocking { auth.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD) }
        composeRule.waitFor("Perfil")
        composeRule.tap("Perfil")
        composeRule.waitFor("Cocinero/a")
        composeRule.tap("Cocinero/a")

        // Edit: saved as pending, then sent.
        composeRule.waitFor("Editar")
        composeRule.tap("Editar")
        composeRule.waitFor("Editar búsqueda")
        composeRule.replaceInField(0, "Cocinero/a con experiencia")
        composeRule.tap("Guardar cambios")
        composeRule.waitFor("Cambios pendientes de envío")
        composeRule.onNodeWithText("Cocinero/a con experiencia").assertExists()

        connectionIsBack(context)
        composeRule.waitFor("Publicada")

        // Close: asks first, saved as pending, then sent.
        composeRule.tap("Cerrar búsqueda")
        composeRule.tap("Sí, cerrar")
        composeRule.waitFor("Cierre pendiente de envío")

        connectionIsBack(context)
        composeRule.waitFor("Cerrada")
        composeRule.onNodeWithText("Editar").assertDoesNotExist()

        val onServer = runBlocking { backend.allOffers() }.single { it.id == "f-5" }
        assertEquals("Cocinero/a con experiencia" to "CLOSED", onServer.title to onServer.status)
    }
}
