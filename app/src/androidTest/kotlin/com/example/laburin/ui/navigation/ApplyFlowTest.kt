package com.example.laburin.ui.navigation

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.laburin.MainActivity
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.data.local.LaburinDatabase
import com.example.laburin.data.sync.WorkManagerSyncScheduler
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

/** RF-02 end to end: apply, see it pending, the send happens when "the network is back", then see it sent. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ApplyFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var db: LaburinDatabase
    @Inject lateinit var workerFactory: HiltWorkerFactory

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).setWorkerFactory(workerFactory).build(),
        )
        // Only this flow's tables: the activity already loaded the map's offers before @Before runs.
        db.openHelper.writableDatabase.apply {
            execSQL("DELETE FROM applications")
            execSQL("DELETE FROM pending_actions")
        }
        runBlocking { auth.logout() }
    }

    @Test
    fun applyingQueuesTheApplicationAndSendsItWhenTheConnectionIsBack() {
        runBlocking { auth.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD) }
        openOffer("Vendedor/a")

        composeRule.tap("Postularme")
        composeRule.waitFor("Pendiente de envío")

        connectionIsBack()
        composeRule.waitFor("Postulación enviada")

        composeRule.onNodeWithText("Volver").performClick()
        composeRule.tap("Postulaciones")
        composeRule.waitFor("Mis postulaciones")
        composeRule.waitFor("Vendedor/a")
        composeRule.onNodeWithText("Postulación enviada").assertExists()
    }

    @Test
    fun applyingWithoutACvLeadsToTheProfileToUploadIt() {
        runBlocking { auth.signUpCandidate("Ana", "Pérez", "ana.sincv@demo.laburin", "12345678") }
        openOffer("Vendedor/a")

        composeRule.tap("Postularme")

        composeRule.waitFor("Cargar CV")
        composeRule.onNodeWithText("Todavía no cargaste tu CV. Solo se aceptan archivos PDF.").assertExists()
    }

    private fun openOffer(title: String) {
        composeRule.waitFor(title)
        composeRule.onNodeWithText(title).performClick()
        composeRule.waitFor("Postularme")
    }

    /** WorkManager waits for a connection; in tests the constraint is satisfied explicitly. */
    private fun connectionIsBack() {
        val workManager = WorkManager.getInstance(context)
        val driver = WorkManagerTestInitHelper.getTestDriver(context)!!
        workManager.getWorkInfosForUniqueWork(WorkManagerSyncScheduler.UNIQUE_NAME).get()
            .filter { !it.state.isFinished }
            .forEach { driver.setAllConstraintsMet(it.id) }
    }
}
