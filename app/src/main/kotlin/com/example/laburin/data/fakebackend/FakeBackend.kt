package com.example.laburin.data.fakebackend

import com.example.laburin.data.auth.AuthRemoteDataSource
import com.example.laburin.data.auth.AuthResponseDto
import com.example.laburin.data.auth.UserDto
import com.example.laburin.data.remote.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory stand-in for the Go API, shared by every fake remote so flows connect end to end
 * (a candidate applies, the business reviews, the candidate sees the result). State lives while the process does.
 * All accounts and data are fictional.
 */
@Singleton
class FakeBackend @Inject constructor() : AuthRemoteDataSource {

    private data class Account(val user: UserDto, val password: String)

    private val mutex = Mutex()
    private val accounts = mutableMapOf<String, Account>()

    init {
        addAccount(DEMO_CANDIDATE_ID, "CANDIDATE", "Postulante Demo", DEMO_CANDIDATE_EMAIL)
        addAccount(DEMO_BUSINESS_ID, "BUSINESS", "Comercio Demo", DEMO_BUSINESS_EMAIL)
    }

    override suspend fun login(email: String, password: String): AuthResponseDto = call {
        val account = accounts[email.lowercase()]?.takeIf { it.password == password } ?: throw ApiException(401)
        AuthResponseDto(token = newToken(), user = account.user)
    }

    override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) = call {
        if (accounts.containsKey(email.lowercase())) throw ApiException(409)
        val user = addAccount(UUID.randomUUID().toString(), "CANDIDATE", "$firstName $lastName", email, password)
        AuthResponseDto(token = newToken(), user = user)
    }

    private fun addAccount(id: String, role: String, name: String, email: String, password: String = DEMO_PASSWORD): UserDto {
        val user = UserDto(id = id, role = role, displayName = name, email = email.lowercase())
        accounts[user.email] = Account(user, password)
        return user
    }

    private fun newToken() = "fake-" + UUID.randomUUID()

    /** Simulated latency; every call is serialized like a single server would. */
    private suspend fun <T> call(block: () -> T): T {
        delay(LATENCY_MS)
        return mutex.withLock { block() }
    }

    companion object {
        const val DEMO_CANDIDATE_ID = "demo-candidate"
        const val DEMO_BUSINESS_ID = "demo-business"
        const val DEMO_CANDIDATE_EMAIL = "postulante@demo.laburin"
        const val DEMO_BUSINESS_EMAIL = "comercio@demo.laburin"
        const val DEMO_PASSWORD = "demo1234"
        private const val LATENCY_MS = 400L
    }
}
