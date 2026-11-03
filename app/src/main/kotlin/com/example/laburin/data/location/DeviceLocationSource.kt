package com.example.laburin.data.location

import com.example.laburin.domain.model.GeoLocation

data class LocationFix(val location: GeoLocation, val ageMillis: Long)

/** Thin port over the platform location API, so the fallback rules can be tested on the JVM. */
interface DeviceLocationSource {
    fun hasPermission(): Boolean

    /** Fresh fix, or null if the device can't provide one. May never answer: callers must time out. */
    suspend fun currentFix(): LocationFix?

    suspend fun lastKnownFix(): LocationFix?
}
