package com.example.laburin.ui.navigation

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
class AppShellViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `follows the session`() = runTest {
        val auth = FakeAuthRepository(initial = null)
        val vm = AppShellViewModel(auth)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.isLoggedIn.collect {} }
        assertEquals(false, vm.isLoggedIn.value)

        auth.session.value = CANDIDATE_SESSION

        assertEquals(true, vm.isLoggedIn.value)
    }
}
