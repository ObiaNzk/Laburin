package com.example.laburin.domain.model

import java.time.Instant

data class Offer(
    val id: String,
    val title: String,
    val category: Category,
    val description: String,
    val schedule: String,
    val jobType: String,
    val businessName: String,
    val address: String,
    val location: GeoLocation,
    val updatedAt: Instant,
)

data class NearbyOffer(val offer: Offer, val distanceMeters: Int)

/** Fixed list until the API provides categories. */
enum class Category { GASTRONOMY, MECHANICS, STOCK, CLOTHING }

data class SearchFilters(val radiusKm: Int = DEFAULT_RADIUS_KM, val category: Category? = null) {
    companion object {
        const val DEFAULT_RADIUS_KM = 2
        val RADIUS_OPTIONS_KM = listOf(2, 5, 10, 15)
    }
}
