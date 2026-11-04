package com.example.laburin.data.auth

import com.example.laburin.data.remote.apiCall
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class CandidateSignUpRequest(val firstName: String, val lastName: String, val email: String, val password: String)

/** Contract: docs/api/auth.md. */
interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @POST("auth/signup/candidate")
    suspend fun signUpCandidate(@Body body: CandidateSignUpRequest): AuthResponseDto

    @POST("auth/signup/business")
    suspend fun signUpBusiness(@Body body: BusinessSignUpDto): AuthResponseDto
}

class RetrofitAuthRemoteDataSource @Inject constructor(private val api: AuthApi) : AuthRemoteDataSource {
    override suspend fun login(email: String, password: String) = apiCall { api.login(LoginRequest(email, password)) }

    override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) =
        apiCall { api.signUpCandidate(CandidateSignUpRequest(firstName, lastName, email, password)) }

    override suspend fun signUpBusiness(data: BusinessSignUpDto) = apiCall { api.signUpBusiness(data) }
}
