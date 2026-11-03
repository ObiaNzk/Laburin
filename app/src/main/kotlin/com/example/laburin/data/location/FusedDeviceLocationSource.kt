package com.example.laburin.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.laburin.domain.model.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/** Adapter over Google Play Services. Fallback rules live in [FallbackLocationProvider]. */
class FusedDeviceLocationSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: FusedLocationProviderClient,
) : DeviceLocationSource {

    override fun hasPermission() =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission") // Checked by the caller through hasPermission().
    override suspend fun currentFix(): LocationFix? {
        val cancellation = CancellationTokenSource()
        return awaitLocation(
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellation.token),
            onCancel = cancellation::cancel,
        )
    }

    @SuppressLint("MissingPermission") // Checked by the caller through hasPermission().
    override suspend fun lastKnownFix(): LocationFix? = awaitLocation(client.lastLocation)

    private suspend fun awaitLocation(task: Task<Location?>, onCancel: () -> Unit = {}): LocationFix? =
        try {
            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation { onCancel() }
                task.addOnSuccessListener { continuation.resume(it?.toFix()) }
                    .addOnFailureListener { continuation.resume(null) }
            }
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
            null
        }

    private fun Location.toFix() = LocationFix(
        location = GeoLocation(latitude, longitude),
        ageMillis = (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000,
    )
}
