package com.example.laburin.model

import org.osmdroid.util.GeoPoint

data class Offer(
    val title: String,
    val category: String,
    val distanceText: String,
    val distanceMeters: Int,
    val address: String,
    val location: GeoPoint? = null
)
