package com.example.laburin.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class OfferDto(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val schedule: String,
    val jobType: String,
    val businessName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    /** ISO-8601 UTC, e.g. "2026-11-01T12:00:00Z". */
    val updatedAt: String,
)

/** Server answered with a non-2xx status. Network failures are reported as [java.io.IOException]. */
class ApiException(val code: Int, message: String? = null) : Exception(message ?: "HTTP $code")

interface OfferRemoteDataSource {
    /**
     * @throws java.io.IOException on network failure
     * @throws ApiException on a non-2xx response
     */
    suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?): List<OfferDto>

    /**
     * Any status: the caller decides what a closed offer means. Null when the server doesn't know the id (404).
     * @throws java.io.IOException on network failure
     * @throws ApiException on any other non-2xx response
     */
    suspend fun getOffer(id: String): OfferDto?
}
