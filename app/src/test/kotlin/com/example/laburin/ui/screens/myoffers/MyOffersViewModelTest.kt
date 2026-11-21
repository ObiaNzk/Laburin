package com.example.laburin.ui.screens.myoffers

import com.example.laburin.domain.model.PublishedOfferStatus
import com.example.laburin.fakes.FakeMyOffersRepository
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.MutableClock
import com.example.laburin.fakes.testPublishedOffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class MyOffersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val offers = FakeMyOffersRepository()
    private val clock = MutableClock()

    private fun TestScope.createViewModel(): MyOffersViewModel {
        val vm = MyOffersViewModel(offers, clock)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `lists the business's publications`() = runTest {
        val list = listOf(testPublishedOffer("p-1", "Mozo", PublishedOfferStatus.ACTIVE), testPublishedOffer("p-2", "Cajero/a", PublishedOfferStatus.CLOSED))
        offers.store.value = list

        assertEquals(MyOffersUiState.Content(list), createViewModel().uiState.value)
    }

    @Test
    fun `without publications it invites to publish`() = runTest {
        assertEquals(MyOffersUiState.Empty, createViewModel().uiState.value)
    }

    @Test
    fun `opening the tab brings the publications from the server`() = runTest {
        createViewModel()

        assertEquals(1, offers.refreshCalls)
    }

    @Test
    fun `coming back after a while refreshes them, right away it does not`() = runTest {
        val vm = createViewModel()
        clock.advance(Duration.ofMinutes(4))
        vm.onScreenStarted()
        assertEquals(1, offers.refreshCalls)

        clock.advance(Duration.ofMinutes(1))
        vm.onScreenStarted()

        assertEquals(2, offers.refreshCalls)
    }
}
