package com.example.laburin.ui.screens.cv

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.data.fakebackend.FakeBackend
import com.example.laburin.ui.theme.LaburinTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.content.Context

@RunWith(AndroidJUnit4::class)
class CvViewerContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun show(state: CvViewerUiState) = composeRule.setContent {
        LaburinTheme {
            CvViewerContent(
                state = state, onBack = { calls += "back" }, onRetry = { calls += "retry" },
                onRenderFailed = { calls += "renderFailed" },
            )
        }
    }

    private fun fileWith(bytes: ByteArray) = File(context.cacheDir, "cv-test.pdf").apply { writeBytes(bytes) }

    @Test
    fun showsEveryPageOfTheCv() {
        val pdf = runBlocking {
            val backend = FakeBackend()
            backend.downloadCv(backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD).token)!!
        }

        show(CvViewerUiState.Ready(fileWith(pdf).absolutePath))

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription("Página 1 de 1"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Página 1 de 1").assertIsDisplayed()
    }

    @Test
    fun aFileThePhoneCannotRenderIsReported() {
        show(CvViewerUiState.Ready(fileWith("no es un pdf".toByteArray()).absolutePath))

        composeRule.waitUntil(5_000) { calls.contains("renderFailed") }
    }

    @Test
    fun withoutACvItSaysSo() {
        show(CvViewerUiState.NotUploaded)

        composeRule.onNodeWithText("Todavía no cargaste tu CV.").assertIsDisplayed()
    }

    @Test
    fun withoutNetworkItOffersToRetry() {
        show(CvViewerUiState.Error(CvViewerError.Network))

        composeRule.onNodeWithText("Necesitás conexión para ver tu CV").assertIsDisplayed()
        composeRule.onNodeWithText("Reintentar").performClick()

        assertEquals(listOf("retry"), calls)
    }

    @Test
    fun goingBackLeavesTheViewer() {
        show(CvViewerUiState.Loading)

        composeRule.onNodeWithText("Volver").performClick()

        assertEquals(listOf("back"), calls)
    }
}
