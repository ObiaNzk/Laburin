package com.example.laburin.data.remote

import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject

interface OfferApi {
    @GET("offers")
    suspend fun getOffers(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radiusKm") radiusKm: Int,
        @Query("category") category: String?,
    ): List<OfferDto>
}

class RetrofitOfferRemoteDataSource @Inject constructor(
    private val api: OfferApi,
) : OfferRemoteDataSource {

    override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?) =
        try {
            api.getOffers(latitude, longitude, radiusKm, category)
        } catch (e: HttpException) {
            throw ApiException(e.code(), e.message())
        }
}
