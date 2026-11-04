package com.example.laburin.data.auth

import com.example.laburin.data.remote.ApiException
import com.example.laburin.data.remote.ApiTestServer
import com.example.laburin.data.remote.bodyJson
import com.example.laburin.data.remote.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** Contract: docs/api/auth.md. */
class RetrofitAuthRemoteDataSourceTest {

    @get:Rule
    val server = ApiTestServer()

    private val dataSource by lazy { RetrofitAuthRemoteDataSource(server.create()) }

    @Test
    fun `login posts the credentials and returns the session`() = runTest {
        server.respond(body = AUTH_RESPONSE)

        val response = dataSource.login("[EMAIL]", "[PASSWORD]")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.url.encodedPath)
        assertEquals(json("""{"email":"[EMAIL]","password":"[PASSWORD]"}"""), request.bodyJson())
        assertEquals(AuthResponseDto("t-1", UserDto("u-1", "CANDIDATE", "[NOMBRE]", "[EMAIL]")), response)
    }

    @Test
    fun `login sends no session token`() = runTest {
        server.respond(body = AUTH_RESPONSE)

        dataSource.login("[EMAIL]", "[PASSWORD]")

        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `wrong credentials are reported with their status code`() = runTest {
        server.respond(code = 401)

        assertEquals(401, apiErrorCode { dataSource.login("[EMAIL]", "[PASSWORD]") })
    }

    @Test
    fun `candidate sign up posts the form`() = runTest {
        server.respond(code = 201, body = AUTH_RESPONSE)

        dataSource.signUpCandidate("[NOMBRE]", "[APELLIDO]", "[EMAIL]", "[PASSWORD]")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/signup/candidate", request.url.encodedPath)
        assertEquals(
            json("""{"firstName":"[NOMBRE]","lastName":"[APELLIDO]","email":"[EMAIL]","password":"[PASSWORD]"}"""),
            request.bodyJson(),
        )
    }

    @Test
    fun `an email already registered is reported as 409`() = runTest {
        server.respond(code = 409)

        assertEquals(409, apiErrorCode { dataSource.signUpCandidate("[NOMBRE]", "[APELLIDO]", "[EMAIL]", "[PASSWORD]") })
    }

    @Test
    fun `business sign up posts the business and its location`() = runTest {
        server.respond(code = 201, body = AUTH_RESPONSE)
        val form = BusinessSignUpDto("[COMERCIO]", "RETAIL", "9 a 18", "[DIRECCION]", -34.6, -58.4, "[EMAIL]", "[PASSWORD]")

        dataSource.signUpBusiness(form)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/auth/signup/business", request.url.encodedPath)
        assertEquals(
            json(
                """{"businessName":"[COMERCIO]","category":"RETAIL","schedule":"9 a 18","address":"[DIRECCION]",
                "latitude":-34.6,"longitude":-58.4,"email":"[EMAIL]","password":"[PASSWORD]"}""",
            ),
            request.bodyJson(),
        )
    }

    @Test(expected = IOException::class)
    fun `a network failure surfaces as IOException`() = runTest {
        server.goOffline()

        dataSource.login("[EMAIL]", "[PASSWORD]")
    }

    private companion object {
        const val AUTH_RESPONSE =
            """{"token":"t-1","user":{"id":"u-1","role":"CANDIDATE","displayName":"[NOMBRE]","email":"[EMAIL]"}}"""
    }
}

suspend fun apiErrorCode(block: suspend () -> Unit): Int {
    try {
        block()
    } catch (e: ApiException) {
        return e.code
    }
    throw AssertionError("Expected ApiException")
}
