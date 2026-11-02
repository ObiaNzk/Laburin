package com.example.laburin.ui.screens.map

import app.cash.turbine.test
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.usecase.ObserveNearbyOffersUseCase
import com.example.laburin.domain.usecase.RefreshNearbyOffersUseCase
import com.example.laburin.fakes.FakeConnectivityObserver
import com.example.laburin.fakes.FakeFiltersRepository
import com.example.laburin.fakes.FakeLocationProvider
import com.example.laburin.fakes.FakeOfferRepository
import com.example.laburin.fakes.MainDispatcherRule
import com.example.laburin.fakes.OBELISCO
import com.example.laburin.fakes.testOffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeOfferRepository()
    private val filters = FakeFiltersRepository()
    private val location = FakeLocationProvider()
    private val connectivity = FakeConnectivityObserver(online = true)

    private val palermo = GeoLocation(-34.5880, -58.4300)

    private fun TestScope.createViewModel(): MapViewModel {
        val vm = MapViewModel(
            observeNearbyOffers = ObserveNearbyOffersUseCase(repository),
            refreshNearbyOffers = RefreshNearbyOffersUseCase(repository),
            offerRepository = repository,
            filtersRepository = filters,
            locationProvider = location,
            connectivityObserver = connectivity,
        )
        // uiState is WhileSubscribed: keep a subscriber like the UI would.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    @Test
    fun `before the location is resolved the screen is loading and nothing is fetched`() = runTest {
        val vm = createViewModel()

        assertEquals(OffersState.Loading, vm.uiState.value.offers)
        assertEquals(LocationStatus.Unknown, vm.uiState.value.locationStatus)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `granted permission centers on the user and refreshes that area`() = runTest {
        location.result = LocationResult.Located(palermo)
        repository.offers.value = listOf(testOffer(id = "p", location = palermo))
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = true)

        val state = vm.uiState.value
        assertEquals(palermo, state.center)
        assertEquals(LocationStatus.Granted, state.locationStatus)
        assertEquals(listOf(Triple(palermo, 2, null)), repository.refreshCalls)
        assertEquals(listOf("p"), (state.offers as OffersState.Content).offers.map { it.offer.id })
    }

    @Test
    fun `denied permission falls back to the default center`() = runTest {
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = false)

        assertEquals(LocationStatus.Denied, vm.uiState.value.locationStatus)
        assertEquals(MapUiState.DEFAULT_CENTER, vm.uiState.value.center)
        assertEquals(MapUiState.DEFAULT_CENTER, repository.refreshCalls.single().first)
    }

    @Test
    fun `successful refresh without results is the empty state`() = runTest {
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = true)

        assertEquals(OffersState.Empty, vm.uiState.value.offers)
    }

    @Test
    fun `offline without cache shows unavailable and does not hit the network`() = runTest {
        connectivity.isOnline.value = false
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = true)

        assertEquals(OffersState.Unavailable(isOffline = true), vm.uiState.value.offers)
        assertTrue(repository.refreshCalls.isEmpty())
    }

    @Test
    fun `offline with cache shows cached offers and their age`() = runTest {
        connectivity.isOnline.value = false
        val synced = Instant.parse("2026-11-02T09:45:00Z")
        repository.lastSyncedAt.value = synced
        repository.offers.value = listOf(testOffer())
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = true)

        val state = vm.uiState.value
        assertEquals(false, state.isOnline)
        assertEquals(synced, state.lastSyncedAt)
        assertTrue(state.offers is OffersState.Content)
    }

    @Test
    fun `failed refresh keeps cached offers and tells the user`() = runTest {
        repository.offers.value = listOf(testOffer())
        repository.nextRefreshResult = RefreshResult.ServerError(500)
        val vm = createViewModel()

        vm.effects.test {
            vm.onLocationPermissionResult(granted = true)
            assertEquals(MapEffect.ShowMessage(MapMessage.RefreshFailedShowingCache), awaitItem())
        }
        assertTrue(vm.uiState.value.offers is OffersState.Content)
    }

    @Test
    fun `failed refresh without cache is unavailable, not an empty list`() = runTest {
        repository.nextRefreshResult = RefreshResult.ServerError(500)
        val vm = createViewModel()

        vm.onLocationPermissionResult(granted = true)

        assertEquals(OffersState.Unavailable(isOffline = false), vm.uiState.value.offers)
    }

    @Test
    fun `coming back online refreshes`() = runTest {
        connectivity.isOnline.value = false
        val vm = createViewModel()
        vm.onLocationPermissionResult(granted = true)

        connectivity.isOnline.value = true

        assertEquals(1, repository.refreshCalls.size)
        assertEquals(true, vm.uiState.value.isOnline)
    }

    @Test
    fun `changing radius and category persists them and refreshes`() = runTest {
        val vm = createViewModel()
        vm.onLocationPermissionResult(granted = true)

        vm.onRadiusChange(5)
        vm.onCategoryChange(Category.GASTRONOMY)

        assertEquals(5, vm.uiState.value.radiusKm)
        assertEquals(Category.GASTRONOMY, vm.uiState.value.category)
        assertEquals(Triple(OBELISCO, 5, Category.GASTRONOMY), repository.refreshCalls.last())
    }

    @Test
    fun `widen radius moves to the next option`() = runTest {
        val vm = createViewModel()

        vm.onWidenRadius()

        assertEquals(5, filters.filters.value.radiusKm)
    }

    @Test
    fun `typing filters locally without refetching`() = runTest {
        repository.offers.value = listOf(testOffer(id = "a", title = "Mozo"), testOffer(id = "b", title = "Cajero"))
        val vm = createViewModel()
        vm.onLocationPermissionResult(granted = true)

        vm.onQueryChange("moz")

        assertEquals(listOf("a"), (vm.uiState.value.offers as OffersState.Content).offers.map { it.offer.id })
        assertEquals(1, repository.refreshCalls.size)
    }

    @Test
    fun `retry refreshes again`() = runTest {
        val vm = createViewModel()
        vm.onLocationPermissionResult(granted = true)

        vm.onRetry()

        assertEquals(2, repository.refreshCalls.size)
    }

    @Test
    fun `tapping an offer navigates to it`() = runTest {
        val vm = createViewModel()

        vm.effects.test {
            vm.onOfferClick("offer-9")
            assertEquals(MapEffect.NavigateToOffer("offer-9"), awaitItem())
        }
    }
}
