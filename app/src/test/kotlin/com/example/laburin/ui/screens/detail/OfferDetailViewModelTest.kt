package com.example.laburin.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.model.ViewSource
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.SessionRepository
import com.example.laburin.domain.usecase.ObserveOfferUseCase
import com.example.laburin.domain.usecase.RefreshOfferUseCase
import com.example.laburin.domain.apply.ApplyToOfferUseCase
import com.example.laburin.domain.model.ApplicationStatus
import com.example.laburin.fakes.BUSINESS_SESSION
import com.example.laburin.fakes.CANDIDATE_SESSION
import com.example.laburin.fakes.FakeApplicationRepository
import com.example.laburin.fakes.FakeConnectivityObserver
import com.example.laburin.fakes.FakeCvRepository
import com.example.laburin.fakes.FakeSyncScheduler
import com.example.laburin.fakes.testApplication
import com.example.laburin.fakes.FakeOfferRepository
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.testOffer
import com.example.laburin.ui.navigation.OfferDetailDestination
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfferDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOfferRepository()
    private val session = FakeSessionRepository()
    private val connectivity = FakeConnectivityObserver(online = true)
    private val applications = FakeApplicationRepository()
    private val cv = FakeCvRepository()
    private val scheduler = FakeSyncScheduler()
    private val offer = testOffer(id = "offer-1")

    private fun TestScope.createViewModel(
        offerId: String = "offer-1",
        distanceMeters: Int? = 400,
        fromQr: Boolean = false,
    ): OfferDetailViewModel {
        val vm = OfferDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    OfferDetailDestination.ARG_OFFER_ID to offerId,
                    OfferDetailDestination.ARG_DISTANCE_METERS to distanceMeters,
                    OfferDetailDestination.ARG_FROM_QR to fromQr,
                )
            ),
            observeOffer = ObserveOfferUseCase(repository),
            offerRepository = repository,
            refreshOffer = RefreshOfferUseCase(repository),
            sessionRepository = session,
            connectivityObserver = connectivity,
            applications = applications,
            applyToOffer = ApplyToOfferUseCase(session, cv, applications, scheduler),
        )
        // uiState is WhileSubscribed: keep a subscriber like the UI would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    // --- Content and privacy (RF-02, RNF-05) ---

    @Test
    fun `guest sees only title, category and distance`() = runTest {
        repository.offers.value = listOf(offer)

        val vm = createViewModel()

        assertEquals(OfferDetailUiState.Guest(offer.title, offer.category, 400), vm.uiState.value)
    }

    @Test
    fun `logged in user sees the full offer`() = runTest {
        repository.offers.value = listOf(offer)
        session.session.value = Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]")

        val vm = createViewModel()

        assertEquals(OfferDetailUiState.Full(offer, 400, application = null), vm.uiState.value)
    }

    @Test
    fun `logging in while on the screen reveals the full offer`() = runTest {
        repository.offers.value = listOf(offer)
        val vm = createViewModel()

        session.session.value = Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]")

        assertEquals(OfferDetailUiState.Full(offer, 400, application = null), vm.uiState.value)
    }

    @Test
    fun `distance is optional`() = runTest {
        repository.offers.value = listOf(offer)

        val vm = createViewModel(distanceMeters = null)

        assertEquals(OfferDetailUiState.Guest(offer.title, offer.category, null), vm.uiState.value)
    }

    // --- Recently viewed (RF-02: a viewed or scanned offer stays available outside the radius) ---

    @Test
    fun `opening an offer from the map records it as viewed`() = runTest {
        repository.offers.value = listOf(offer)

        createViewModel()

        assertEquals(listOf("offer-1" to ViewSource.MAP), repository.viewed.value)
    }

    @Test
    fun `a scanned offer is recorded as viewed from a QR`() = runTest {
        repository.offers.value = listOf(offer)

        createViewModel(fromQr = true)

        assertEquals(listOf("offer-1" to ViewSource.QR), repository.viewed.value)
    }

    @Test
    fun `an offer that is not available is not recorded until it loads`() = runTest {
        connectivity.isOnline.value = false
        createViewModel(offerId = "f-4")
        assertEquals(emptyList<Any>(), repository.viewed.value)

        repository.onRefreshOffer = { repository.offers.value = listOf(testOffer(id = "f-4")) }
        connectivity.isOnline.value = true

        assertEquals(listOf("f-4" to ViewSource.MAP), repository.viewed.value)
    }

    // --- Refresh and server-wins ---

    @Test
    fun `opening the detail refreshes that offer once`() = runTest {
        repository.offers.value = listOf(offer)

        createViewModel()

        assertEquals(listOf("offer-1"), repository.refreshOfferCalls)
    }

    @Test
    fun `not cached is loading while the refresh is in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.onRefreshOffer = { gate.await() }

        val vm = createViewModel(offerId = "unknown")

        assertEquals(OfferDetailUiState.Loading, vm.uiState.value)
        gate.complete(Unit)
    }

    @Test
    fun `not cached after a successful refresh means it is no longer available`() = runTest {
        val vm = createViewModel(offerId = "closed")

        assertEquals(OfferDetailUiState.NoLongerAvailable, vm.uiState.value)
    }

    @Test
    fun `a cached offer closed by the server becomes no longer available`() = runTest {
        repository.offers.value = listOf(offer)
        repository.onRefreshOffer = { repository.offers.value = emptyList() }

        val vm = createViewModel()

        assertEquals(OfferDetailUiState.NoLongerAvailable, vm.uiState.value)
    }

    @Test
    fun `cached offer stays visible when the refresh fails`() = runTest {
        repository.offers.value = listOf(offer)
        repository.nextRefreshOfferResult = RefreshResult.NetworkError

        val vm = createViewModel()

        assertEquals(OfferDetailUiState.Guest(offer.title, offer.category, 400), vm.uiState.value)
    }

    // --- Unavailable (offline / server error) ---

    @Test
    fun `not cached and offline is unavailable without hitting the network`() = runTest {
        connectivity.isOnline.value = false

        val vm = createViewModel(offerId = "unknown")

        assertEquals(OfferDetailUiState.Unavailable(isOffline = true), vm.uiState.value)
        assertTrue(repository.refreshOfferCalls.isEmpty())
    }

    @Test
    fun `an offer opened offline (e g a scanned QR) loads by itself when the connection is back`() = runTest {
        connectivity.isOnline.value = false
        val vm = createViewModel(offerId = "f-4")
        assertEquals(OfferDetailUiState.Unavailable(isOffline = true), vm.uiState.value)

        repository.onRefreshOffer = { repository.offers.value = listOf(testOffer(id = "f-4")) }
        connectivity.isOnline.value = true

        assertEquals(listOf("f-4"), repository.refreshOfferCalls)
        assertTrue(vm.uiState.value is OfferDetailUiState.Guest)
    }

    @Test
    fun `not cached and a server error is unavailable but not offline`() = runTest {
        repository.nextRefreshOfferResult = RefreshResult.ServerError(500)

        val vm = createViewModel(offerId = "unknown")

        assertEquals(OfferDetailUiState.Unavailable(isOffline = false), vm.uiState.value)
    }

    @Test
    fun `retry fetches again and shows the offer`() = runTest {
        repository.nextRefreshOfferResult = RefreshResult.ServerError(500)
        val vm = createViewModel(offerId = "unknown")

        repository.nextRefreshOfferResult = RefreshResult.Success
        repository.onRefreshOffer = { repository.offers.value = listOf(testOffer(id = "unknown")) }
        vm.onRetry()

        assertEquals(2, repository.refreshOfferCalls.size)
        assertTrue(vm.uiState.value is OfferDetailUiState.Guest)
    }

    // --- Navigation effects ---

    @Test
    fun `actions emit navigation effects`() = runTest {
        repository.offers.value = listOf(offer)
        val vm = createViewModel()

        vm.effects.test {
            vm.onBack()
            assertEquals(OfferDetailEffect.NavigateBack, awaitItem())
            vm.onCreateAccount()
            assertEquals(OfferDetailEffect.NavigateToSignUp, awaitItem())
            vm.onLogin()
            assertEquals(OfferDetailEffect.NavigateToLogin, awaitItem())
        }
    }

    // --- Applying (RF-02) ---

    @Test
    fun `the full view shows the users application to that offer`() = runTest {
        repository.offers.value = listOf(offer)
        session.session.value = CANDIDATE_SESSION
        val application = testApplication(offerId = "offer-1", status = ApplicationStatus.SENT)
        applications.store.value = listOf(application)

        val vm = createViewModel()

        assertEquals(OfferDetailUiState.Full(offer, 400, application), vm.uiState.value)
    }

    @Test
    fun `applying queues the application and the detail shows it as pending`() = runTest {
        repository.offers.value = listOf(offer)
        session.session.value = CANDIDATE_SESSION
        val vm = createViewModel()

        vm.onApply()

        val state = vm.uiState.value as OfferDetailUiState.Full
        assertEquals(ApplicationStatus.PENDING_SEND, state.application?.status)
        assertEquals(1, scheduler.scheduled)
    }

    @Test
    fun `applying without a CV sends the user to upload it`() = runTest {
        repository.offers.value = listOf(offer)
        session.session.value = CANDIDATE_SESSION
        cv.cv.value = null
        val vm = createViewModel()

        vm.effects.test {
            vm.onApply()
            assertEquals(OfferDetailEffect.NavigateToCvUpload, awaitItem())
        }
        assertEquals(emptyList<Any>(), applications.applied)
    }

    @Test
    fun `a business account is told it cannot apply`() = runTest {
        repository.offers.value = listOf(offer)
        session.session.value = BUSINESS_SESSION
        val vm = createViewModel()

        vm.effects.test {
            vm.onApply()
            assertEquals(OfferDetailEffect.ShowMessage(OfferDetailMessage.BusinessCannotApply), awaitItem())
        }
    }

    private class FakeSessionRepository : SessionRepository {
        override val session = MutableStateFlow<Session?>(null)
    }
}
