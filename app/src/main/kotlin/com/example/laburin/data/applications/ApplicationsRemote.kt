package com.example.laburin.data.applications

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationDto(
    val id: String,
    val offerId: String,
    val offerTitle: String,
    val businessName: String,
    val status: String,
    /** ISO-8601 UTC. */
    val createdAt: String,
)

@Serializable
data class CvDto(val fileName: String)

/**
 * All calls take the session token (sent as `Authorization: Bearer`).
 * @throws java.io.IOException on network failure
 * @throws com.example.laburin.data.remote.ApiException 401 session expired, 409 offer closed or missing
 */
interface ApplicationsRemoteDataSource {
    /** Idempotent by [applicationId]: resending the same application is a no-op. */
    suspend fun apply(token: String, applicationId: String, offerId: String)
    suspend fun myApplications(token: String): List<ApplicationDto>
    suspend fun uploadCv(token: String, fileName: String, bytes: ByteArray)
    suspend fun getCv(token: String): CvDto?
}
