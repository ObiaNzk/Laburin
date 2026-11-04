package com.example.laburin.data.remote

import retrofit2.HttpException

object ApiConfig {
    /** Without an API address (`laburin.apiBaseUrl`) the app runs against the in-memory FakeBackend. */
    fun usesFakeBackend(baseUrl: String): Boolean = baseUrl.isBlank()
}

/** Value for the `Authorization` header of the private endpoints. */
fun bearer(token: String) = "Bearer $token"

/** Maps a non-2xx response to [ApiException]; network failures stay [java.io.IOException]. */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (e: HttpException) {
        throw ApiException(e.code(), e.message())
    }
