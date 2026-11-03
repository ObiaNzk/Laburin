package com.example.laburin.ui.screens.map

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.NearbyOffer
import com.example.laburin.domain.model.SearchFilters
import java.time.Instant

data class MapUiState(
    val query: String = "",
    val radiusKm: Int = SearchFilters.DEFAULT_RADIUS_KM,
    val category: Category? = null,
    val center: GeoLocation = DEFAULT_CENTER,
    /** Only set with a real GPS fix; the default center is never shown as "you are here". */
    val userLocation: GeoLocation? = null,
    val locationStatus: LocationStatus = LocationStatus.Unknown,
    val isOnline: Boolean = true,
    val lastSyncedAt: Instant? = null,
    val isRefreshing: Boolean = false,
    val offers: OffersState = OffersState.Loading,
    /** Name of the zone the user typed, when the center comes from a manual zone. */
    val zoneName: String? = null,
    val zoneSearch: ZoneSearchState = ZoneSearchState(),
) {
    companion object {
        val DEFAULT_CENTER = GeoLocation(-34.6037, -58.3816)
    }
}

enum class LocationStatus { Unknown, Granted, Denied, Unavailable, ManualZone }

data class ZoneSearchState(val isOpen: Boolean = false, val isSearching: Boolean = false, val error: ZoneError? = null)

enum class ZoneError { NotFound, Network }

sealed interface OffersState {
    data object Loading : OffersState
    data class Content(val offers: List<NearbyOffer>) : OffersState
    data object Empty : OffersState

    /** Nothing cached for this search and the refresh failed. */
    data class Unavailable(val isOffline: Boolean) : OffersState
}

sealed interface MapEffect {
    data class NavigateToOffer(val offerId: String, val distanceMeters: Int?) : MapEffect
    data object NavigateToScan : MapEffect
    data object NavigateToPublish : MapEffect
    data class ShowMessage(val message: MapMessage) : MapEffect
}

enum class MapMessage { RefreshFailedShowingCache }
