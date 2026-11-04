package com.example.laburin.data.publish

import com.example.laburin.data.remote.OfferDto
import com.example.laburin.data.remote.SalaryDto
import com.example.laburin.data.remote.apiCall
import com.example.laburin.data.remote.bearer
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import javax.inject.Inject

/** The offer's content; its id goes in the path. */
@Serializable
data class OfferContentRequest(
    val title: String,
    val category: String,
    val schedule: String,
    val description: String,
    /** No default on purpose: always sent, null meaning "A convenir". */
    val salary: SalaryDto?,
)

@Serializable
data class ReviewRequest(val approve: Boolean)

/** Contract: docs/api/business.md. */
interface BusinessApi {
    @PUT("offers/{id}")
    suspend fun putOffer(@Header("Authorization") auth: String, @Path("id") offerId: String, @Body body: OfferContentRequest)

    @POST("offers/{id}/close")
    suspend fun closeOffer(@Header("Authorization") auth: String, @Path("id") offerId: String)

    @GET("offers/mine")
    suspend fun myOffers(@Header("Authorization") auth: String): List<OfferDto>

    @GET("offers/{id}/applicants")
    suspend fun applicants(@Header("Authorization") auth: String, @Path("id") offerId: String): List<ApplicantDto>

    @PUT("applications/{id}/review")
    suspend fun review(@Header("Authorization") auth: String, @Path("id") applicationId: String, @Body body: ReviewRequest): ApplicantDto
}

class RetrofitBusinessRemoteDataSource @Inject constructor(private val api: BusinessApi) : BusinessRemoteDataSource {

    override suspend fun publishOffer(token: String, offer: PublishOfferDto) = apiCall {
        api.putOffer(bearer(token), offer.id, OfferContentRequest(offer.title, offer.category, offer.schedule, offer.description, offer.salary))
    }

    override suspend fun closeOffer(token: String, offerId: String) = apiCall { api.closeOffer(bearer(token), offerId) }

    override suspend fun myOffers(token: String) = apiCall { api.myOffers(bearer(token)) }

    override suspend fun applicants(token: String, offerId: String) = apiCall { api.applicants(bearer(token), offerId) }

    override suspend fun review(token: String, applicationId: String, approve: Boolean) =
        apiCall { api.review(bearer(token), applicationId, ReviewRequest(approve)) }
}
