package com.example.laburin.ui.screens.scan

import app.cash.turbine.test
import com.example.laburin.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val vm = ScanViewModel()

    @Test
    fun `starts waiting for the camera permission`() {
        assertEquals(ScanUiState.AskingPermission, vm.uiState.value)
    }

    @Test
    fun `with permission it scans`() {
        vm.onCameraPermissionResult(granted = true)

        assertEquals(ScanUiState.Scanning, vm.uiState.value)
    }

    @Test
    fun `without permission the app stays usable through the map`() {
        vm.onCameraPermissionResult(granted = false)

        assertEquals(ScanUiState.PermissionDenied, vm.uiState.value)
    }

    @Test
    fun `an offer QR opens that offer`() = runTest {
        vm.onCameraPermissionResult(granted = true)

        vm.effects.test {
            vm.onCodeScanned("laburin://offer/f-4")
            assertEquals(ScanEffect.OpenOffer("f-4"), awaitItem())
        }
    }

    @Test
    fun `the same QR seen in several frames opens the offer only once`() = runTest {
        vm.onCameraPermissionResult(granted = true)

        vm.effects.test {
            vm.onCodeScanned("laburin://offer/f-4")
            vm.onCodeScanned("laburin://offer/f-4")
            assertEquals(ScanEffect.OpenOffer("f-4"), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `any other code is an invalid QR`() {
        vm.onCameraPermissionResult(granted = true)

        vm.onCodeScanned("https://example.com")

        assertEquals(ScanUiState.InvalidQr, vm.uiState.value)
    }

    @Test
    fun `scanning again after an invalid QR goes back to the camera`() {
        vm.onCameraPermissionResult(granted = true)
        vm.onCodeScanned("basura")

        vm.onScanAgain()

        assertEquals(ScanUiState.Scanning, vm.uiState.value)
    }

    @Test
    fun `codes are ignored while showing an invalid QR`() = runTest {
        vm.onCameraPermissionResult(granted = true)
        vm.onCodeScanned("basura")

        vm.effects.test {
            vm.onCodeScanned("laburin://offer/f-4")
            expectNoEvents()
        }
    }
}
