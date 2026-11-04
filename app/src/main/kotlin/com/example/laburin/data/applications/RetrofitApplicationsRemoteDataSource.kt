package com.example.laburin.data.applications

import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.apiCall
import com.example.laburin.data.remote.bearer
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import javax.inject.Inject

@Serializable
data class ApplyRequest(val offerId: String)

/** Contract: docs/api/applications.md. */
interface ApplicationsApi {
    @PUT("applications/{id}")
    suspend fun apply(@Header("Authorization") auth: String, @Path("id") applicationId: String, @Body body: ApplyRequest)

    @GET("applications/mine")
    suspend fun myApplications(@Header("Authorization") auth: String): List<ApplicationDto>

    @Multipart
    @PUT("me/cv")
    suspend fun uploadCv(@Header("Authorization") auth: String, @Part file: MultipartBody.Part)

    @GET("me/cv")
    suspend fun getCv(@Header("Authorization") auth: String): CvDto
}

class RetrofitApplicationsRemoteDataSource @Inject constructor(private val api: ApplicationsApi) : ApplicationsRemoteDataSource {

    override suspend fun apply(token: String, applicationId: String, offerId: String) =
        apiCall { api.apply(bearer(token), applicationId, ApplyRequest(offerId)) }

    override suspend fun myApplications(token: String) = apiCall { api.myApplications(bearer(token)) }

    override suspend fun uploadCv(token: String, fileName: String, bytes: ByteArray) = apiCall {
        val file = MultipartBody.Part.createFormData("file", fileName, bytes.toRequestBody(PDF))
        api.uploadCv(bearer(token), file)
    }

    override suspend fun getCv(token: String): CvDto? =
        try {
            apiCall { api.getCv(bearer(token)) }
        } catch (e: ApiException) {
            if (e.code == HTTP_NOT_FOUND) null else throw e
        }

    private companion object {
        val PDF = "application/pdf".toMediaType()
        const val HTTP_NOT_FOUND = 404
    }
}
