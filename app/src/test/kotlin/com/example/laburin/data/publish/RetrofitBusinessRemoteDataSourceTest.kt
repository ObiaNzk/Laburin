package com.example.laburin.data.publish

import com.example.laburin.data.auth.apiErrorCode
import com.example.laburin.data.remote.ApiTestServer
import com.example.laburin.data.remote.bodyJson
import com.example.laburin.data.remote.json
import com.example.laburin.data.remote.SalaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Contract: docs/api/business.md. */
class RetrofitBusinessRemoteDataSourceTest {

    @get:Rule
    val server = ApiTestServer()

    private val dataSource by lazy { RetrofitBusinessRemoteDataSource(server.create()) }

    @Test
    fun `publishing puts the offer under its client generated id`() = runTest {
        server.respond(code = 201)

        dataSource.publishOffer("t-1", PublishOfferDto("o-1", "Mozo", "GASTRONOMY", "Fines de semana", "[DESCRIPCION]"))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/offers/o-1", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(
            json("""{"title":"Mozo","category":"GASTRONOMY","schedule":"Fines de semana","description":"[DESCRIPCION]","salary":null}"""),
            request.bodyJson(),
        )
    }

    @Test
    fun `the salary travels with the offer`() = runTest {
        server.respond(code = 201)

        dataSource.publishOffer(
            "t-1", PublishOfferDto("o-1", "Mozo", "GASTRONOMY", "", "", salary = SalaryDto(600_000, 750_000, "MONTH")),
        )

        assertEquals(
            json("""{"min":600000,"max":750000,"period":"MONTH"}"""),
            (server.takeRequest().bodyJson() as kotlinx.serialization.json.JsonObject)["salary"],
        )
    }

    @Test
    fun `an account that is not a business is reported as 403`() = runTest {
        server.respond(code = 403)

        assertEquals(403, apiErrorCode { dataSource.publishOffer("t-1", PublishOfferDto("o-1", "Mozo", "GASTRONOMY", "", "")) })
    }

    @Test
    fun `my offers are read with the session token`() = runTest {
        server.respond(
            body = """[{"id":"o-1","title":"Mozo","category":"GASTRONOMY","description":"","schedule":"","jobType":"",
                "businessName":"[COMERCIO]","address":"[DIRECCION]","latitude":-34.6,"longitude":-58.4,"status":"ACTIVE",
                "updatedAt":"2026-11-01T12:00:00Z"}]""",
        )

        val offers = dataSource.myOffers("t-1")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/offers/mine", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(listOf("o-1"), offers.map { it.id })
    }

    @Test
    fun `applicants are read for one offer and only approved ones carry the contact`() = runTest {
        server.respond(
            body = """[
                {"applicationId":"a-1","offerId":"o-1","candidateName":"[NOMBRE]","cvFileName":"cv.pdf","status":"PENDING_REVIEW","contactEmail":null},
                {"applicationId":"a-2","offerId":"o-1","candidateName":"[NOMBRE]","cvFileName":null,"status":"APPROVED","contactEmail":"[EMAIL]"}
            ]""",
        )

        val applicants = dataSource.applicants("t-1", "o-1")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/offers/o-1/applicants", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(
            listOf(
                ApplicantDto("a-1", "o-1", "[NOMBRE]", "cv.pdf", "PENDING_REVIEW", null),
                ApplicantDto("a-2", "o-1", "[NOMBRE]", null, "APPROVED", "[EMAIL]"),
            ),
            applicants,
        )
    }

    @Test
    fun `another business's offer is reported as 403`() = runTest {
        server.respond(code = 403)

        assertEquals(403, apiErrorCode { dataSource.applicants("t-1", "o-1") })
    }

    @Test
    fun `a review is sent for the application and returns the confirmed applicant`() = runTest {
        server.respond(
            body = """{"applicationId":"a-1","offerId":"o-1","candidateName":"[NOMBRE]","cvFileName":"cv.pdf",
                "status":"APPROVED","contactEmail":"[EMAIL]"}""",
        )

        val confirmed = dataSource.review("t-1", "a-1", approve = true)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/applications/a-1/review", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(json("""{"approve":true}"""), request.bodyJson())
        assertEquals(ApplicantDto("a-1", "o-1", "[NOMBRE]", "cv.pdf", "APPROVED", "[EMAIL]"), confirmed)
    }

    @Test
    fun `closing posts to the offer's close endpoint with the session token`() = runTest {
        server.respond()

        dataSource.closeOffer("t-1", "o-1")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/offers/o-1/close", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
    }

    @Test
    fun `closing an offer that is already closed is reported as 409`() = runTest {
        server.respond(code = 409)

        assertEquals(409, apiErrorCode { dataSource.closeOffer("t-1", "o-1") })
    }
}
