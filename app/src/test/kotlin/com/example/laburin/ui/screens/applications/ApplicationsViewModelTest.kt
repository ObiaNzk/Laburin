package com.example.laburin.ui.screens.applications

import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeApplicationRepository
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.testApplication
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val applications = FakeApplicationRepository()
    private val auth = FakeAuthRepository(CANDIDATE_SESSION)

    private fun TestScope.createViewModel(): ApplicationsViewModel {
        val vm = ApplicationsViewModel(applications, auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `lists the users applications with the pending count`() = runTest {
        val list = listOf(
            testApplication("a-1", status = ApplicationStatus.PENDING_SEND),
            testApplication("a-2", offerId = "o-2", status = ApplicationStatus.APPROVED),
        )
        applications.store.value = list
        applications.pendingCount.value = 1

        val vm = createViewModel()

        assertEquals(ApplicationsUiState.Content(list, pendingToSend = 1), vm.uiState.value)
    }

    @Test
    fun `no applications yet is the empty state`() = runTest {
        assertEquals(ApplicationsUiState.Empty, createViewModel().uiState.value)
    }

    @Test
    fun `logged out asks to log in`() = runTest {
        auth.session.value = null

        assertEquals(ApplicationsUiState.LoggedOut, createViewModel().uiState.value)
    }

    @Test
    fun `opening the screen refreshes statuses from the server`() = runTest {
        createViewModel()

        assertEquals(1, applications.refreshCalls)
    }

    @Test
    fun `a failed refresh keeps the list and does not break the screen`() = runTest {
        applications.nextRefresh = RefreshResult.NetworkError
        applications.store.value = listOf(testApplication())

        val vm = createViewModel()

        assertEquals(ApplicationsUiState.Content(listOf(testApplication()), pendingToSend = 0), vm.uiState.value)
    }

    @Test
    fun `pull to refresh asks the server again`() = runTest {
        val vm = createViewModel()

        vm.onRefresh()

        assertEquals(2, applications.refreshCalls)
    }
}
