package com.example.laburin.ui.screens.cv

import androidx.lifecycle.ViewModelStore
import com.example.laburin.domain.model.CvFileResult
import com.example.laburin.fakes.FakeCvRepository
import com.example.laburin.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CvViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cv = FakeCvRepository()

    @Test
    fun `opening the viewer shows the downloaded CV`() = runTest {
        cv.nextOpen = CvFileResult.Ready("/cache/cv/mi-cv.pdf")

        val vm = CvViewerViewModel(cv)

        assertEquals(CvViewerUiState.Ready("/cache/cv/mi-cv.pdf"), vm.uiState.value)
    }

    @Test
    fun `without an uploaded CV it says so`() = runTest {
        cv.nextOpen = CvFileResult.NotUploaded

        assertEquals(CvViewerUiState.NotUploaded, CvViewerViewModel(cv).uiState.value)
    }

    @Test
    fun `without network it offers to retry, and retrying downloads again`() = runTest {
        cv.nextOpen = CvFileResult.NetworkError
        val vm = CvViewerViewModel(cv)
        assertEquals(CvViewerUiState.Error(CvViewerError.Network), vm.uiState.value)

        cv.nextOpen = CvFileResult.Ready("/cache/cv/mi-cv.pdf")
        vm.onRetry()

        assertEquals(CvViewerUiState.Ready("/cache/cv/mi-cv.pdf"), vm.uiState.value)
        assertEquals(2, cv.openCalls)
    }

    @Test
    fun `server errors are shown as such`() = runTest {
        cv.nextOpen = CvFileResult.ServerError(500)

        assertEquals(CvViewerUiState.Error(CvViewerError.Server), CvViewerViewModel(cv).uiState.value)
    }

    @Test
    fun `a PDF the phone cannot render is reported`() = runTest {
        val vm = CvViewerViewModel(cv)

        vm.onRenderFailed()

        assertEquals(CvViewerUiState.Error(CvViewerError.Unreadable), vm.uiState.value)
    }

    @Test
    fun `leaving the viewer deletes the downloaded copy`() = runTest {
        val store = ViewModelStore().apply { put("cv", CvViewerViewModel(cv)) }

        store.clear() // what the back stack does when the user leaves the screen

        assertTrue(cv.closed)
    }
}
