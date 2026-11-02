package com.example.laburin.domain.model

/** Own type on purpose: the domain must not depend on a map/location library. */
data class GeoLocation(val latitude: Double, val longitude: Double)

data class BoundingBox(
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double,
)
