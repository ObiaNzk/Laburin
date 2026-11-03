package com.example.laburin.ui.screens.profile

import app.cash.turbine.test
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.FakeCvRepository
import com.example.laburin.fakes.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cv = FakeCvRepository(initial = null)
    private val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46)

    private fun TestScope.createViewModel(auth: FakeAuthRepository): ProfileViewModel {
        val vm = ProfileViewModel(auth, cv)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `shows the logged in user and their CV`() = runTest {
        cv.cv.value = CvInfo("cv.pdf")
        val vm = createViewModel(FakeAuthRepository(CANDIDATE_SESSION))

        assertEquals(ProfileUiState.LoggedIn(CANDIDATE_SESSION, cv = CvInfo("cv.pdf")), vm.uiState.value)
    }

    @Test
    fun `opening the profile loads the CV from the server if needed`() = runTest {
        createViewModel(FakeAuthRepository(CANDIDATE_SESSION))

        assertEquals(1, cv.currentCalls)
    }

    @Test
    fun `without a session asks to log in`() = runTest {
        val vm = createViewModel(FakeAuthRepository(initial = null))

        assertEquals(ProfileUiState.LoggedOut, vm.uiState.value)
    }

    @Test
    fun `a business profile has no CV section`() = runTest {
        val vm = createViewModel(FakeAuthRepository(BUSINESS_SESSION))

        assertEquals(ProfileUiState.LoggedIn(BUSINESS_SESSION, cv = null, showsCv = false), vm.uiState.value)
    }

    @Test
    fun `uploading a CV shows it`() = runTest {
        val vm = createViewModel(FakeAuthRepository(CANDIDATE_SESSION))

        vm.onCvPicked("mi-cv.pdf", "application/pdf", pdf)

        assertEquals(CvInfo("mi-cv.pdf"), (vm.uiState.value as ProfileUiState.LoggedIn).cv)
        assertEquals(null, (vm.uiState.value as ProfileUiState.LoggedIn).cvError)
    }

    @Test
    fun `upload errors are explained`() = runTest {
        val vm = createViewModel(FakeAuthRepository(CANDIDATE_SESSION))
        val cases = mapOf(
            CvUploadResult.NotPdf to CvError.NotPdf,
            CvUploadResult.TooLarge to CvError.TooLarge,
            CvUploadResult.NetworkError to CvError.Network,
            CvUploadResult.ServerError(500) to CvError.Server,
        )

        cases.forEach { (result, error) ->
            cv.nextUpload = result
            vm.onCvPicked("x", "application/pdf", pdf)
            assertEquals(error, (vm.uiState.value as ProfileUiState.LoggedIn).cvError)
        }
    }

    @Test
    fun `a file that could not be read is reported`() = runTest {
        val vm = createViewModel(FakeAuthRepository(CANDIDATE_SESSION))

        vm.onCvUnreadable()

        assertEquals(CvError.Unreadable, (vm.uiState.value as ProfileUiState.LoggedIn).cvError)
    }

    @Test
    fun `logout ends the session and leaves the profile`() = runTest {
        val auth = FakeAuthRepository(CANDIDATE_SESSION)
        val vm = createViewModel(auth)

        vm.effects.test {
            vm.onLogout()
            assertEquals(ProfileEffect.LoggedOut, awaitItem())
        }
        assertEquals(listOf("logout"), auth.calls)
    }
}
