package com.example.laburin.domain.geo

import com.example.laburin.domain.model.BoundingBox
import com.example.laburin.domain.model.GeoLocation
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure great-circle math. SQLite has no guaranteed trig functions, so exact distance lives here. */
object Haversine {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(from: GeoLocation, to: GeoLocation): Double {
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val a = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) *
            sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    fun boundingBox(center: GeoLocation, radiusMeters: Double): BoundingBox {
        val latDelta = Math.toDegrees(radiusMeters / EARTH_RADIUS_METERS)
        val lonDelta = Math.toDegrees(
            radiusMeters / (EARTH_RADIUS_METERS * cos(Math.toRadians(center.latitude)))
        )
        return BoundingBox(
            minLatitude = center.latitude - latDelta,
            maxLatitude = center.latitude + latDelta,
            minLongitude = center.longitude - lonDelta,
            maxLongitude = center.longitude + lonDelta,
        )
    }
}
