package com.example.laburin.ui.navigation

import android.content.Context
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.MainActivity
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/** The candidate sees the CV they uploaded, as businesses will, and no copy stays on the phone afterwards. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CvViewerFlowTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(e2ePermissions())
        .around(composeRule)

    @Inject lateinit var auth: AuthRepository

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking { auth.logout() }
    }

    @Test
    fun theCandidateSeesTheirUploadedCv() {
        runBlocking { auth.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD) }
        composeRule.waitFor("Perfil")
        composeRule.tap("Perfil")
        composeRule.waitFor("Ver mi CV")
        composeRule.tap("Ver mi CV")

        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("Página 1 de 1")).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.tap("Volver")
        composeRule.waitFor("Ver mi CV")
        composeRule.waitUntil(5_000) { File(context.cacheDir, "cv").listFiles().isNullOrEmpty() }
        assertFalse(File(context.cacheDir, "cv/mi-cv.pdf").exists())
    }
}
