package com.example.laburin.data.fakebackend

import com.example.laburin.data.auth.BusinessSignUpDto
import com.example.laburin.data.publish.PublishOfferDto
import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.FakeOfferRemoteDataSource
import com.example.laburin.data.remote.SalaryDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FakeBackendBusinessTest {

    private val backend = FakeBackend()
    private val draft = PublishOfferDto("p-1", "Mozo", "GASTRONOMY", "Sábados", "Atención en salón")

    private suspend fun businessToken() = backend.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD).token

    @Test
    fun `a business that signs up can log in as a business`() = runTest {
        backend.signUpBusiness(
            BusinessSignUpDto("Taller Nuevo", "MECHANICS", "9 a 18", "[DIRECCION]", -34.6, -58.4, "taller@demo.laburin", "12345678")
        )

        assertEquals("BUSINESS", backend.login("taller@demo.laburin", "12345678").user.role)
    }

    @Test
    fun `a published offer is active on the map at the business location`() = runTest {
        backend.publishOffer(businessToken(), draft)

        val offer = backend.allOffers().single { it.id == "p-1" }
        assertEquals("ACTIVE", offer.status)
        assertEquals("Comercio Demo", offer.businessName)
        assertEquals(FakeBackend.DEMO_BUSINESS_LOCATION.first, offer.latitude, 0.0)
    }

    @Test
    fun `publishing the same offer twice is idempotent`() = runTest {
        val token = businessToken()

        backend.publishOffer(token, draft)
        backend.publishOffer(token, draft)

        assertEquals(1, backend.allOffers().count { it.id == "p-1" })
    }

    @Test
    fun `my offers lists only the business's own offers`() = runTest {
        val token = businessToken()
        backend.publishOffer(token, draft)

        val mine = backend.myOffers(token).map { it.id }

        assertEquals(true, "p-1" in mine)
        assertEquals(true, mine.all { it == "p-1" || it.startsWith("f-") })
    }

    @Test
    fun `a candidate cannot publish`() = runTest {
        val candidate = backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD).token

        try {
            backend.publishOffer(candidate, draft)
            fail("Expected 403")
        } catch (e: ApiException) {
            assertEquals(403, e.code)
        }
    }

    // --- Edit and close (RF-03) ---

    @Test
    fun `putting an existing offer again replaces its content`() = runTest {
        val token = businessToken()
        backend.publishOffer(token, draft)

        backend.publishOffer(token, draft.copy(title = "Mozo/a", schedule = "Viernes y sábados"))

        val offer = backend.allOffers().single { it.id == "p-1" }
        assertEquals("Mozo/a" to "Viernes y sábados", offer.title to offer.schedule)
    }

    @Test
    fun `a closed offer leaves the map and is listed as closed for its business`() = runTest {
        val token = businessToken()
        backend.publishOffer(token, draft)

        backend.closeOffer(token, "p-1")

        assertEquals("CLOSED", backend.myOffers(token).single { it.id == "p-1" }.status)
        assertEquals(false, FakeOfferRemoteDataSource(backend).getOffers(
            FakeBackend.DEMO_BUSINESS_LOCATION.first, FakeBackend.DEMO_BUSINESS_LOCATION.second, 2, null,
        ).any { it.id == "p-1" })
    }

    @Test
    fun `the salary is published and can be changed or removed`() = runTest {
        val token = businessToken()

        backend.publishOffer(token, draft.copy(salary = SalaryDto(600_000, null, "MONTH")))
        assertEquals(SalaryDto(600_000, null, "MONTH"), backend.allOffers().single { it.id == "p-1" }.salary)

        backend.publishOffer(token, draft.copy(salary = null))
        assertEquals(null, backend.allOffers().single { it.id == "p-1" }.salary)
    }

    @Test
    fun `closing twice is idempotent`() = runTest {
        val token = businessToken()
        backend.publishOffer(token, draft)

        backend.closeOffer(token, "p-1")
        backend.closeOffer(token, "p-1")

        assertEquals("CLOSED", backend.myOffers(token).single { it.id == "p-1" }.status)
    }

    @Test
    fun `a closed offer cannot be edited`() = runTest {
        val token = businessToken()
        backend.publishOffer(token, draft)
        backend.closeOffer(token, "p-1")

        assertEquals(409, codeOf { backend.publishOffer(token, draft.copy(title = "Mozo/a")) })
    }

    @Test
    fun `another business cannot edit or close the offer`() = runTest {
        backend.publishOffer(businessToken(), draft)
        backend.signUpBusiness(
            BusinessSignUpDto("Otro", "MECHANICS", "9 a 18", "[DIRECCION]", -34.6, -58.4, "otro@demo.laburin", "12345678")
        )
        val other = backend.login("otro@demo.laburin", "12345678").token

        assertEquals(403, codeOf { backend.publishOffer(other, draft.copy(title = "Otro")) })
        assertEquals(403, codeOf { backend.closeOffer(other, "p-1") })
    }

    @Test
    fun `closing an unknown offer is reported as 404`() = runTest {
        assertEquals(404, codeOf { backend.closeOffer(businessToken(), "missing") })
    }

    private suspend fun codeOf(block: suspend () -> Unit): Int {
        try {
            block()
        } catch (e: ApiException) {
            return e.code
        }
        fail("Expected ApiException")
        error("unreachable")
    }
}
