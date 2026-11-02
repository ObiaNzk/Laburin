package com.example.laburin.model

import org.osmdroid.util.GeoPoint
import java.util.Locale

data class Offer(
    val title: String,
    val category: String,
    val distanceMeters: Int,
    val address: String,
    val location: GeoPoint? = null,
    val description: String = "",
    val schedule: String = "",
    val jobType: String = "",
    val businessName: String = ""
) {
    val distanceText: String
        get() = if (distanceMeters >= 1000) {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        } else {
            "$distanceMeters m"
        }
}
