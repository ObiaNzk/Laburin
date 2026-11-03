package com.example.laburin.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RetrofitOfferRemoteDataSourceTest {

    private val api = FakeOfferApi()
    private val dataSource = RetrofitOfferRemoteDataSource(api)

    @Test
    fun `getOffer returns the offer sent by the server`() = runTest {
        api.single = { dto }

        assertEquals(dto, dataSource.getOffer("o-1"))
    }

    @Test
    fun `getOffer returns null when the server answers 404`() = runTest {
        api.single = { throw httpError(404) }

        assertNull(dataSource.getOffer("o-1"))
    }

    @Test
    fun `getOffer maps other HTTP errors to ApiException with the status code`() = runTest {
        api.single = { throw httpError(500) }

        assertEquals(500, apiErrorCode { dataSource.getOffer("o-1") })
    }

    @Test
    fun `getOffers maps HTTP errors to ApiException with the status code`() = runTest {
        api.list = { throw httpError(503) }

        assertEquals(503, apiErrorCode { dataSource.getOffers(0.0, 0.0, 2, null) })
    }

    private suspend fun apiErrorCode(block: suspend () -> Unit): Int {
        try {
            block()
        } catch (e: ApiException) {
            return e.code
        }
        fail("Expected ApiException")
        error("unreachable")
    }

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    private val dto = OfferDto(
        id = "o-1", title = "Mozo", category = "GASTRONOMY", description = "", schedule = "", jobType = "",
        businessName = "[COMERCIO]", address = "[DIRECCION]", latitude = 0.0, longitude = 0.0,
        status = "ACTIVE", updatedAt = "2026-11-01T12:00:00Z",
    )

    private class FakeOfferApi : OfferApi {
        var list: () -> List<OfferDto> = { emptyList() }
        var single: () -> OfferDto = { error("not configured") }

        override suspend fun getOffers(latitude: Double, longitude: Double, radiusKm: Int, category: String?) = list()
        override suspend fun getOffer(id: String): OfferDto = single()
    }
}
