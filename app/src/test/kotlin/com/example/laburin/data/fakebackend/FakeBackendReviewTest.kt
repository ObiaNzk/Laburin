package com.example.laburin.data.fakebackend

import com.example.laburin.data.remote.ApiException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class FakeBackendReviewTest {

    private val backend = FakeBackend()

    private suspend fun candidate() = backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD).token
    private suspend fun business() = backend.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD).token

    @Test
    fun `the business sees who applied, with the contact hidden`() = runTest {
        backend.apply(candidate(), "a-1", "f-4")

        val applicant = backend.applicants(business(), "f-4").single()

        assertEquals("Postulante Demo", applicant.candidateName)
        assertEquals("cv-postulante-demo.pdf", applicant.cvFileName)
        assertEquals("PENDING_REVIEW", applicant.status)
        assertNull(applicant.contactEmail)
    }

    @Test
    fun `approving reveals the contact and the candidate sees the match`() = runTest {
        val candidateToken = candidate()
        backend.apply(candidateToken, "a-1", "f-4")

        val reviewed = backend.review(business(), "a-1", approve = true)

        assertEquals("APPROVED", reviewed.status)
        assertEquals(FakeBackend.DEMO_CANDIDATE_EMAIL, reviewed.contactEmail)
        assertEquals("APPROVED", backend.myApplications(candidateToken).single().status)
    }

    @Test
    fun `rejecting keeps the contact hidden`() = runTest {
        val candidateToken = candidate()
        backend.apply(candidateToken, "a-1", "f-4")

        val reviewed = backend.review(business(), "a-1", approve = false)

        assertEquals("REJECTED", reviewed.status)
        assertNull(reviewed.contactEmail)
        assertEquals("REJECTED", backend.myApplications(candidateToken).single().status)
    }

    @Test
    fun `another business cannot see or review the applicants`() = runTest {
        backend.apply(candidate(), "a-1", "f-4")
        val other = backend.signUpBusiness(
            com.example.laburin.data.auth.BusinessSignUpDto("Otro", "STOCK", "x", "[DIRECCION]", 0.0, 0.0, "otro@demo.laburin", "12345678")
        ).token

        assertEquals(403, apiCode { backend.applicants(other, "f-4") })
        assertEquals(403, apiCode { backend.review(other, "a-1", approve = true) })
    }

    private suspend fun apiCode(block: suspend () -> Unit): Int {
        try { block() } catch (e: ApiException) { return e.code }
        fail("Expected ApiException"); error("unreachable")
    }
}
