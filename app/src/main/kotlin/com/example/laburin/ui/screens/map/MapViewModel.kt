package com.example.laburin.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.NearbyOffer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.model.SearchFilters
import com.example.laburin.domain.repository.ConnectivityObserver
import com.example.laburin.domain.repository.FiltersRepository
import com.example.laburin.domain.repository.LocationProvider
import com.example.laburin.domain.repository.OfferRepository
import com.example.laburin.domain.usecase.ObserveNearbyOffersUseCase
import com.example.laburin.domain.usecase.RefreshNearbyOffersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val observeNearbyOffers: ObserveNearbyOffersUseCase,
    private val refreshNearbyOffers: RefreshNearbyOffersUseCase,
    offerRepository: OfferRepository,
    private val filtersRepository: FiltersRepository,
    private val locationProvider: LocationProvider,
    connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private data class LocationState(
        val center: GeoLocation = MapUiState.DEFAULT_CENTER,
        val status: LocationStatus = LocationStatus.Unknown,
    )

    private data class RefreshState(val isRefreshing: Boolean = false, val lastResult: RefreshResult? = null)

    private data class SearchParams(val location: LocationState, val filters: SearchFilters, val query: String)

    private val query = MutableStateFlow("")
    private val location = MutableStateFlow(LocationState())
    private val refreshState = MutableStateFlow(RefreshState())

    private val filters: StateFlow<SearchFilters> =
        filtersRepository.filters.stateIn(viewModelScope, SharingStarted.Eagerly, SearchFilters())

    private val isOnline: StateFlow<Boolean> =
        connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val searchParams = combine(location, filters, query, ::SearchParams)

    /** null until Room answers for the current search. */
    private val nearbyOffers: StateFlow<List<NearbyOffer>?> = searchParams
        .flatMapLatest { observeNearbyOffers(it.location.center, it.filters.radiusKm, it.filters.category, it.query) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<MapUiState> = combine(
        searchParams, nearbyOffers, isOnline, offerRepository.observeLastSyncedAt(), refreshState,
    ) { params, offers, online, lastSyncedAt, refresh ->
        MapUiState(
            query = params.query,
            radiusKm = params.filters.radiusKm,
            category = params.filters.category,
            center = params.location.center,
            locationStatus = params.location.status,
            isOnline = online,
            lastSyncedAt = lastSyncedAt,
            isRefreshing = refresh.isRefreshing,
            offers = offersState(params.location.status, offers, refresh, online),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    private val _effects = MutableSharedFlow<MapEffect>()
    val effects: SharedFlow<MapEffect> = _effects.asSharedFlow()

    init {
        // Query is filtered locally; only area or filter changes go to the network.
        viewModelScope.launch {
            combine(location.filter { it.status != LocationStatus.Unknown }.map { it.center }, filters) { c, f -> c to f }
                .distinctUntilChanged()
                .collect { (center, filters) -> refresh(center, filters) }
        }
        viewModelScope.launch {
            var wasOnline = isOnline.value
            isOnline.collect { online ->
                if (online && !wasOnline && location.value.status != LocationStatus.Unknown) refreshCurrent()
                wasOnline = online
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            val result = if (granted) locationProvider.currentLocation() else LocationResult.PermissionDenied
            location.update { current ->
                when (result) {
                    is LocationResult.Located -> LocationState(result.location, LocationStatus.Granted)
                    LocationResult.PermissionDenied -> current.copy(status = LocationStatus.Denied)
                    LocationResult.Unavailable -> current.copy(status = LocationStatus.Unavailable)
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onRadiusChange(radiusKm: Int) {
        viewModelScope.launch { filtersRepository.setRadiusKm(radiusKm) }
    }

    fun onCategoryChange(category: Category?) {
        viewModelScope.launch { filtersRepository.setCategory(category) }
    }

    fun onWidenRadius() {
        val next = SearchFilters.RADIUS_OPTIONS_KM.firstOrNull { it > filters.value.radiusKm } ?: return
        onRadiusChange(next)
    }

    fun onShowAllCategories() = onCategoryChange(null)

    fun onRetry() {
        viewModelScope.launch { refreshCurrent() }
    }

    fun onOfferClick(offerId: String) {
        val distance = nearbyOffers.value?.firstOrNull { it.offer.id == offerId }?.distanceMeters
        sendEffect(MapEffect.NavigateToOffer(offerId, distance))
    }

    fun onScanClick() = sendEffect(MapEffect.NavigateToScan)

    fun onPublishClick() = sendEffect(MapEffect.NavigateToPublish)

    private suspend fun refreshCurrent() = refresh(location.value.center, filters.value)

    private suspend fun refresh(center: GeoLocation, filters: SearchFilters) {
        if (!isOnline.value) {
            refreshState.value = RefreshState(lastResult = RefreshResult.NetworkError)
            return
        }
        refreshState.update { it.copy(isRefreshing = true) }
        val result = refreshNearbyOffers(center, filters.radiusKm, filters.category)
        refreshState.value = RefreshState(lastResult = result)
        if (result != RefreshResult.Success && !nearbyOffers.value.isNullOrEmpty()) {
            _effects.emit(MapEffect.ShowMessage(MapMessage.RefreshFailedShowingCache))
        }
    }

    private fun sendEffect(effect: MapEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun offersState(
        status: LocationStatus,
        offers: List<NearbyOffer>?,
        refresh: RefreshState,
        online: Boolean,
    ): OffersState = when {
        status == LocationStatus.Unknown || offers == null -> OffersState.Loading
        offers.isNotEmpty() -> OffersState.Content(offers)
        refresh.isRefreshing || refresh.lastResult == null -> OffersState.Loading
        refresh.lastResult == RefreshResult.Success -> OffersState.Empty
        else -> OffersState.Unavailable(isOffline = !online || refresh.lastResult == RefreshResult.NetworkError)
    }
}
