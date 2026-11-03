package com.example.laburin.data.location

import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.repository.LocationProvider
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Prefers a fresh fix; if the device doesn't answer in time, uses the last known one while it's recent.
 * An old fix would put the user somewhere they no longer are, so it's treated as no location.
 */
class FallbackLocationProvider @Inject constructor(
    private val source: DeviceLocationSource,
) : LocationProvider {

    override suspend fun currentLocation(): LocationResult {
        if (!source.hasPermission()) return LocationResult.PermissionDenied
        val fix = withTimeoutOrNull(CURRENT_FIX_TIMEOUT_MS) { source.currentFix() }
            ?: source.lastKnownFix()?.takeIf { it.ageMillis <= MAX_LAST_KNOWN_AGE_MS }
        return fix?.let { LocationResult.Located(it.location) } ?: LocationResult.Unavailable
    }

    private companion object {
        const val CURRENT_FIX_TIMEOUT_MS = 8_000L
        const val MAX_LAST_KNOWN_AGE_MS = 30 * 60_000L
    }
}
