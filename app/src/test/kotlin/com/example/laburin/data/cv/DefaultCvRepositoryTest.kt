package com.example.laburin.data.cv

import com.example.laburin.data.applications.CvDto
import com.example.laburin.data.applications.FakeApplicationsRemote
import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.fakes.CANDIDATE_STORED
import com.example.laburin.fakes.FakeSessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class DefaultCvRepositoryTest {

    private val remote = FakeApplicationsRemote()
    private val session = FakeSessionStore(CANDIDATE_STORED)
    private val cache = FakeCvCache()
    private val repository = DefaultCvRepository(remote, session, cache)
    private val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46) // "%PDF"

    @Test
    fun `logged out has no CV`() = runTest {
        cache.names.value = mapOf("u-1" to "cv.pdf")
        session.saved.value = null

        assertNull(repository.observeCv().first())
        assertNull(repository.current())
    }

    @Test
    fun `current uses the cached CV without calling the server`() = runTest {
        cache.names.value = mapOf("u-1" to "cv.pdf")
        remote.cv = { error("should not be called") }

        assertEquals(CvInfo("cv.pdf"), repository.current())
    }

    @Test
    fun `current asks the server when nothing is cached and remembers it`() = runTest {
        remote.cv = { CvDto("del-servidor.pdf") }

        assertEquals(CvInfo("del-servidor.pdf"), repository.current())
        assertEquals(CvInfo("del-servidor.pdf"), repository.observeCv().first())
    }

    @Test
    fun `current without cache and without network is no CV`() = runTest {
        remote.cv = { throw IOException("offline") }

        assertNull(repository.current())
    }

    @Test
    fun `each user only sees their own CV`() = runTest {
        cache.names.value = mapOf("otra-persona" to "ajeno.pdf")

        assertNull(repository.observeCv().first())
    }

    @Test
    fun `only PDF files are accepted`() = runTest {
        assertEquals(CvUploadResult.NotPdf, repository.upload("cv.docx", "application/msword", byteArrayOf(1)))
        assertNull(repository.observeCv().first())
    }

    @Test
    fun `a renamed file that is not really a PDF is rejected`() = runTest {
        assertEquals(CvUploadResult.NotPdf, repository.upload("foto.pdf", "application/pdf", byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2)))
    }

    @Test
    fun `files over 5 MB are rejected`() = runTest {
        val big = ByteArray(5 * 1024 * 1024 + 1)

        assertEquals(CvUploadResult.TooLarge, repository.upload("cv.pdf", "application/pdf", big))
    }

    @Test
    fun `a successful upload becomes the user's CV`() = runTest {
        assertEquals(CvUploadResult.Success, repository.upload("mi-cv.pdf", "application/pdf", pdf))

        assertEquals(CvInfo("mi-cv.pdf"), repository.observeCv().first())
    }

    @Test
    fun `upload without network reports it and keeps the previous CV`() = runTest {
        cache.names.value = mapOf("u-1" to "anterior.pdf")
        remote.uploadResponse = { throw IOException("offline") }

        assertEquals(CvUploadResult.NetworkError, repository.upload("nuevo.pdf", "application/pdf", pdf))
        assertEquals(CvInfo("anterior.pdf"), repository.observeCv().first())
    }

    @Test
    fun `upload server errors are reported`() = runTest {
        remote.uploadResponse = { throw ApiException(500) }

        assertEquals(CvUploadResult.ServerError(500), repository.upload("cv.pdf", "application/pdf", pdf))
    }

    private class FakeCvCache : CvCache {
        val names = MutableStateFlow<Map<String, String>>(emptyMap())
        override fun observe(userId: String): Flow<String?> = names.map { it[userId] }
        override suspend fun save(userId: String, fileName: String) { names.value = names.value + (userId to fileName) }
    }
}
