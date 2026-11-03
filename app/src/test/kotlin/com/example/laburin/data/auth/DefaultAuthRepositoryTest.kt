package com.example.laburin.data.auth

import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.BusinessSignUp
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class DefaultAuthRepositoryTest {

    private val remote = FakeAuthRemote()
    private val store = FakeSessionStore()
    private var scheduled = 0
    private val repository = DefaultAuthRepository(remote, store) { scheduled++ }

    private val user = UserDto(id = "u-1", role = "CANDIDATE", displayName = "[NOMBRE]", email = "[EMAIL]")

    @Test
    fun `successful login stores the session and the token`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-1", user = user) }

        assertEquals(AuthResult.Success, repository.login("[EMAIL]", "12345678"))

        assertEquals(Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]"), repository.session.first())
        assertEquals("t-1", store.saved.value?.token)
    }

    @Test
    fun `logging in resumes sending actions that were waiting for a session`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-1", user = user) }

        repository.login("[EMAIL]", "12345678")

        assertEquals(1, scheduled)
    }

    @Test
    fun `wrong credentials store nothing`() = runTest {
        remote.respond = { throw ApiException(401) }

        assertEquals(AuthResult.InvalidCredentials, repository.login("[EMAIL]", "x"))
        assertNull(repository.session.first())
    }

    @Test
    fun `login without network reports it`() = runTest {
        remote.respond = { throw IOException("offline") }

        assertEquals(AuthResult.NetworkError, repository.login("[EMAIL]", "12345678"))
    }

    @Test
    fun `sign up with a taken email reports it`() = runTest {
        remote.respond = { throw ApiException(409) }

        assertEquals(AuthResult.EmailAlreadyRegistered, repository.signUpCandidate("A", "B", "[EMAIL]", "12345678"))
    }

    @Test
    fun `successful sign up logs the user in`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-2", user = user) }

        assertEquals(AuthResult.Success, repository.signUpCandidate("A", "B", "[EMAIL]", "12345678"))
        assertEquals("u-1", repository.session.first()?.userId)
    }

    @Test
    fun `a business sign up sends its location and logs in as a business`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-b", user = user.copy(role = "BUSINESS")) }
        val signUp = BusinessSignUp("Taller", Category.MECHANICS, "9 a 18", "[DIRECCION]", "[EMAIL]", "12345678")

        assertEquals(AuthResult.Success, repository.signUpBusiness(signUp, GeoLocation(-34.6, -58.4)))

        assertEquals(UserRole.BUSINESS, repository.session.first()?.role)
        assertEquals(-34.6, remote.lastBusiness?.latitude ?: 0.0, 0.0)
        assertEquals("MECHANICS", remote.lastBusiness?.category)
    }

    @Test
    fun `a business sign up with a taken email reports it`() = runTest {
        remote.respond = { throw ApiException(409) }
        val signUp = BusinessSignUp("Taller", Category.MECHANICS, "9 a 18", "[DIRECCION]", "[EMAIL]", "12345678")

        assertEquals(AuthResult.EmailAlreadyRegistered, repository.signUpBusiness(signUp, GeoLocation(0.0, 0.0)))
    }

    @Test
    fun `other server errors are reported with their code`() = runTest {
        remote.respond = { throw ApiException(500) }

        assertEquals(AuthResult.ServerError(500), repository.login("[EMAIL]", "12345678"))
    }

    @Test
    fun `unknown roles from the server are a server error, not a session`() = runTest {
        remote.respond = { AuthResponseDto(token = "t", user = user.copy(role = "ADMIN")) }

        assertEquals(AuthResult.ServerError(200), repository.login("[EMAIL]", "12345678"))
        assertNull(repository.session.first())
    }

    @Test
    fun `an expired session logs out and is flagged until acknowledged`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-1", user = user) }
        repository.login("[EMAIL]", "12345678")

        repository.sessionExpired()

        assertNull(repository.session.first())
        assertEquals(true, repository.isSessionExpired.first())

        repository.acknowledgeSessionExpired()

        assertEquals(false, repository.isSessionExpired.first())
    }

    @Test
    fun `logging in again clears the expired flag`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-1", user = user) }
        repository.sessionExpired()

        repository.login("[EMAIL]", "12345678")

        assertEquals(false, repository.isSessionExpired.first())
    }

    @Test
    fun `logout clears the session`() = runTest {
        remote.respond = { AuthResponseDto(token = "t-1", user = user) }
        repository.login("[EMAIL]", "12345678")

        repository.logout()

        assertNull(repository.session.first())
        assertNull(store.saved.value)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is never swallowed`() = runTest {
        remote.respond = { throw CancellationException("cancelled") }
        repository.login("[EMAIL]", "12345678")
    }

    private class FakeAuthRemote : AuthRemoteDataSource {
        var respond: () -> AuthResponseDto = { error("not configured") }
        override suspend fun login(email: String, password: String) = respond()
        override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) = respond()
        var lastBusiness: BusinessSignUpDto? = null
        override suspend fun signUpBusiness(data: BusinessSignUpDto): AuthResponseDto { lastBusiness = data; return respond() }
    }

    private class FakeSessionStore : SessionStore {
        val saved = MutableStateFlow<StoredSession?>(null)
        override val stored = saved
        override suspend fun save(session: StoredSession) { saved.value = session }
        override suspend fun clear() { saved.value = null }
    }
}
