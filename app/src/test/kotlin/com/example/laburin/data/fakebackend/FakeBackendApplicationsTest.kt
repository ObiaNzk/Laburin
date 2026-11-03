package com.example.laburin.data.fakebackend

import com.example.laburin.data.remote.ApiException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class FakeBackendApplicationsTest {

    private val backend = FakeBackend()

    private suspend fun candidateToken() = backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD).token

    @Test
    fun `applying to an active offer shows up in my applications as sent`() = runTest {
        val token = candidateToken()

        backend.apply(token, "a-1", "f-1")

        val mine = backend.myApplications(token)
        assertEquals(listOf("a-1"), mine.map { it.id })
        assertEquals("SENT", mine.single().status)
        assertEquals("Ayudante de taller", mine.single().offerTitle)
    }

    @Test
    fun `sending the same application twice is idempotent`() = runTest {
        val token = candidateToken()

        backend.apply(token, "a-1", "f-1")
        backend.apply(token, "a-1", "f-1")

        assertEquals(1, backend.myApplications(token).size)
    }

    @Test
    fun `a closed offer is rejected with 409`() = runTest {
        assertEquals(409, apiCode { backend.apply(candidateToken(), "a-1", "f-7") })
    }

    @Test
    fun `an unknown offer is rejected with 409 too`() = runTest {
        assertEquals(409, apiCode { backend.apply(candidateToken(), "a-1", "nope") })
    }

    @Test
    fun `an invalid token is rejected with 401`() = runTest {
        assertEquals(401, apiCode { backend.myApplications("bad-token") })
    }

    @Test
    fun `the demo candidate already has a CV`() = runTest {
        assertNotNull(backend.getCv(candidateToken()))
    }

    @Test
    fun `a new candidate has no CV until uploading one`() = runTest {
        val token = backend.signUpCandidate("Ana", "Pérez", "ana@demo.laburin", "12345678").token
        assertNull(backend.getCv(token))

        backend.uploadCv(token, "mi-cv.pdf", byteArrayOf(1, 2, 3))

        assertEquals("mi-cv.pdf", backend.getCv(token)?.fileName)
    }

    private suspend fun apiCode(block: suspend () -> Unit): Int {
        try { block() } catch (e: ApiException) { return e.code }
        fail("Expected ApiException"); error("unreachable")
    }
}
