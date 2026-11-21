package com.example.laburin.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

/** A business uses the middle tab for its publications; a candidate, for their applications. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MyOffersFlowTest {

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
        runBlocking { auth.logout() }
    }

    @Test
    fun theBusinessOpensAPublicationFromThePublicationsTab() {
        runBlocking { auth.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD) }
        composeRule.waitFor("Publicaciones")
        composeRule.tap("Publicaciones")
        composeRule.waitFor("Vendedor/a")
        composeRule.tap("Vendedor/a")

        composeRule.waitFor("Cerrar búsqueda") // the publication screen, whatever applicants earlier tests left
    }
}
