package com.example.laburin.data.mapper

import com.example.laburin.data.local.OfferEntity
import com.example.laburin.data.remote.OfferDto
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import java.time.Instant
import java.time.format.DateTimeParseException

/** Returns null for records the app can't represent (unknown category, bad date) so one bad row doesn't break the list. */
fun OfferDto.toEntityOrNull(): OfferEntity? {
    val category = Category.entries.firstOrNull { it.name == category } ?: return null
    val updatedAt = try {
        Instant.parse(updatedAt)
    } catch (_: DateTimeParseException) {
        return null
    }
    return OfferEntity(
        id = id,
        title = title,
        category = category,
        description = description,
        schedule = schedule,
        jobType = jobType,
        businessName = businessName,
        address = address,
        latitude = latitude,
        longitude = longitude,
        updatedAt = updatedAt,
    )
}

fun OfferEntity.toDomain() = Offer(
    id = id,
    title = title,
    category = category,
    description = description,
    schedule = schedule,
    jobType = jobType,
    businessName = businessName,
    address = address,
    location = GeoLocation(latitude, longitude),
    updatedAt = updatedAt,
)
