package com.example.laburin.data.applications

import com.example.laburin.data.auth.apiErrorCode
import com.example.laburin.data.remote.ApiTestServer
import com.example.laburin.data.remote.bodyJson
import com.example.laburin.data.remote.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Contract: docs/api/applications.md. */
class RetrofitApplicationsRemoteDataSourceTest {

    @get:Rule
    val server = ApiTestServer()

    private val dataSource by lazy { RetrofitApplicationsRemoteDataSource(server.create()) }

    @Test
    fun `apply puts the application under its client generated id with the session token`() = runTest {
        server.respond(code = 201)

        dataSource.apply("t-1", "a-1", "o-1")

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/applications/a-1", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(json("""{"offerId":"o-1"}"""), request.bodyJson())
    }

    @Test
    fun `a closed offer is reported as 409`() = runTest {
        server.respond(code = 409)

        assertEquals(409, apiErrorCode { dataSource.apply("t-1", "a-1", "o-1") })
    }

    @Test
    fun `an expired session is reported as 401`() = runTest {
        server.respond(code = 401)

        assertEquals(401, apiErrorCode { dataSource.myApplications("t-1") })
    }

    @Test
    fun `my applications are read with the session token`() = runTest {
        server.respond(
            body = """[{"id":"a-1","offerId":"o-1","offerTitle":"Mozo","businessName":"[COMERCIO]","status":"SENT",
                "createdAt":"2026-11-01T12:00:00Z"}]""",
        )

        val list = dataSource.myApplications("t-1")

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/applications/mine", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(listOf(ApplicationDto("a-1", "o-1", "Mozo", "[COMERCIO]", "SENT", "2026-11-01T12:00:00Z")), list)
    }

    @Test
    fun `the CV is uploaded as a PDF file in a multipart form`() = runTest {
        server.respond()
        val pdf = "%PDF-1.4 [CONTENIDO]".toByteArray()

        dataSource.uploadCv("t-1", "cv.pdf", pdf)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/me/cv", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("""Content-Disposition: form-data; name="file"; filename="cv.pdf""""))
        assertTrue(body.contains("Content-Type: application/pdf"))
        assertArrayEquals(pdf, request.body!!.toByteArray().sliceContaining(pdf))
    }

    @Test
    fun `the uploaded CV is read with the session token`() = runTest {
        server.respond(body = """{"fileName":"cv.pdf"}""")

        val cv = dataSource.getCv("t-1")

        val request = server.takeRequest()
        assertEquals("/me/cv", request.url.encodedPath)
        assertEquals("Bearer t-1", request.headers["Authorization"])
        assertEquals(CvDto("cv.pdf"), cv)
    }

    @Test
    fun `no CV yet is null, not an error`() = runTest {
        server.respond(code = 404)

        assertNull(dataSource.getCv("t-1"))
    }

    @Test
    fun `other CV errors keep their status code`() = runTest {
        server.respond(code = 500)

        assertEquals(500, apiErrorCode { dataSource.getCv("t-1") })
    }

    private fun ByteArray.sliceContaining(part: ByteArray): ByteArray {
        val start = (0..size - part.size).first { i -> part.indices.all { this[i + it] == part[it] } }
        return copyOfRange(start, start + part.size)
    }
}
