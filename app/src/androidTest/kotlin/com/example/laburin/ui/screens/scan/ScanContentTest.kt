package com.example.laburin.ui.screens.scan

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.laburin.ui.theme.LaburinTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    private fun show(state: ScanUiState) = composeRule.setContent {
        LaburinTheme {
            ScanContent(
                state = state,
                onClose = { clicks += "close" }, onScanAgain = { clicks += "again" }, onSearchMap = { clicks += "map" },
                camera = { Text("[CAMARA]") },
            )
        }
    }

    @Test
    fun scanningShowsTheCameraAndTheHint() {
        show(ScanUiState.Scanning)

        composeRule.onNodeWithText("[CAMARA]").assertExists()
        composeRule.onNodeWithText("Apuntá al QR del cartel").assertIsDisplayed()
    }

    @Test
    fun withoutCameraPermissionTheMapIsTheAlternative() {
        show(ScanUiState.PermissionDenied)

        composeRule.onNodeWithText("[CAMARA]").assertDoesNotExist()
        composeRule.onNodeWithText("Sin permiso de cámara").assertIsDisplayed()
        composeRule.onNodeWithText("Buscar en el mapa").performClick()

        assertEquals(listOf("map"), clicks)
    }

    @Test
    fun anInvalidQrOffersToScanAgainOrUseTheMap() {
        show(ScanUiState.InvalidQr)

        composeRule.onNodeWithText("No pudimos abrir la búsqueda").assertIsDisplayed()
        composeRule.onNodeWithText("Volver a escanear").performClick()
        composeRule.onNodeWithText("Buscar en el mapa").performClick()

        assertEquals(listOf("again", "map"), clicks)
    }

    @Test
    fun closeLeavesTheScanner() {
        show(ScanUiState.Scanning)

        composeRule.onNode(hasContentDescription("Cerrar")).performClick()

        assertEquals(listOf("close"), clicks)
    }
}
