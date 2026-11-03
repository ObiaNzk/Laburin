package com.example.laburin.data.location

import android.content.Context
import android.location.Address
import android.os.Build
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.ZoneResult
import com.example.laburin.domain.repository.Geocoder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

/** Adapter over the platform Geocoder (needs a connection). Results are limited to Argentina. */
class AndroidGeocoder @Inject constructor(@param:ApplicationContext private val context: Context) : Geocoder {

    override suspend fun search(query: String): ZoneResult {
        if (!android.location.Geocoder.isPresent()) return ZoneResult.NetworkError
        val platform = android.location.Geocoder(context)
        val name = "$query, Argentina"
        val address = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    platform.getFromLocationName(name, 1, object : android.location.Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                        override fun onError(errorMessage: String?) = cont.resume(null)
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) { platform.getFromLocationName(name, 1)?.firstOrNull() }
            }
        } catch (e: IOException) {
            return ZoneResult.NetworkError
        } ?: return ZoneResult.NotFound
        val label = address.subLocality ?: address.locality ?: query.trim()
        return ZoneResult.Found(label, GeoLocation(address.latitude, address.longitude))
    }
}
