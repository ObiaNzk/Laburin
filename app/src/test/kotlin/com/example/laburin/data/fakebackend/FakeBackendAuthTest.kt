package com.example.laburin.data.fakebackend

import com.example.laburin.data.remote.ApiException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.fail
import org.junit.Test

class FakeBackendAuthTest {

    private val backend = FakeBackend()

    @Test
    fun `a candidate who signs up can log in`() = runTest {
        backend.signUpCandidate("Ana", "Pérez", "ana@demo.laburin", "12345678")

        val response = backend.login("ana@demo.laburin", "12345678")

        assertEquals("CANDIDATE", response.user.role)
        assertEquals("Ana Pérez", response.user.displayName)
    }

    @Test
    fun `each login returns a token`() = runTest {
        val response = backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, FakeBackend.DEMO_PASSWORD)

        assertNotEquals("", response.token)
    }

    @Test
    fun `demo business account exists`() = runTest {
        assertEquals("BUSINESS", backend.login(FakeBackend.DEMO_BUSINESS_EMAIL, FakeBackend.DEMO_PASSWORD).user.role)
    }

    @Test
    fun `wrong password is rejected with 401`() = runTest {
        assertEquals(401, apiCode { backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL, "incorrecta") })
    }

    @Test
    fun `registering an existing email is rejected with 409`() = runTest {
        assertEquals(409, apiCode { backend.signUpCandidate("Otra", "Persona", FakeBackend.DEMO_CANDIDATE_EMAIL, "12345678") })
    }

    @Test
    fun `emails are case insensitive`() = runTest {
        assertEquals("CANDIDATE", backend.login(FakeBackend.DEMO_CANDIDATE_EMAIL.uppercase(), FakeBackend.DEMO_PASSWORD).user.role)
    }

    private suspend fun apiCode(block: suspend () -> Unit): Int {
        try { block() } catch (e: ApiException) { return e.code }
        fail("Expected ApiException"); error("unreachable")
    }
}
