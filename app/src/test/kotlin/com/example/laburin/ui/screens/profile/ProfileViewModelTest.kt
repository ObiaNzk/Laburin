package com.example.laburin.ui.screens.profile

import app.cash.turbine.test
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeAuthRepository
import com.example.laburin.fakes.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows the logged in user`() = runTest {
        val vm = ProfileViewModel(FakeAuthRepository(CANDIDATE_SESSION))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(ProfileUiState.LoggedIn(CANDIDATE_SESSION), vm.uiState.value)
    }

    @Test
    fun `without a session asks to log in`() = runTest {
        val vm = ProfileViewModel(FakeAuthRepository(initial = null))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(ProfileUiState.LoggedOut, vm.uiState.value)
    }

    @Test
    fun `logout ends the session and leaves the profile`() = runTest {
        val auth = FakeAuthRepository(CANDIDATE_SESSION)
        val vm = ProfileViewModel(auth)

        vm.effects.test {
            vm.onLogout()
            assertEquals(ProfileEffect.LoggedOut, awaitItem())
        }
        assertEquals(listOf("logout"), auth.calls)
    }
}
