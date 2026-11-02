package com.example.laburin.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.repository.LocationProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

class FusedLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationProvider {

    override suspend fun currentLocation(): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionDenied
        // Without a recent fix the request can stay pending indefinitely; never block the screen on it.
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestCurrentLocation() } ?: LocationResult.Unavailable
    }

    private suspend fun requestCurrentLocation(): LocationResult =
        suspendCancellableCoroutine { continuation ->
            val cancellation = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellation.cancel() }
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token)
                    .addOnSuccessListener { location ->
                        continuation.resume(
                            location?.let { LocationResult.Located(GeoLocation(it.latitude, it.longitude)) }
                                ?: LocationResult.Unavailable
                        )
                    }
                    .addOnFailureListener { continuation.resume(LocationResult.Unavailable) }
            } catch (_: SecurityException) {
                // Permission revoked between the check and the call.
                continuation.resume(LocationResult.PermissionDenied)
            }
        }

    private fun hasLocationPermission() =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 8_000L
    }
}
