package com.example.laburin.data.auth

import com.example.laburin.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(val id: String, val role: String, val displayName: String, val email: String)

@Serializable
data class AuthResponseDto(val token: String, val user: UserDto)

interface AuthRemoteDataSource {
    /**
     * @throws java.io.IOException on network failure
     * @throws com.example.laburin.data.remote.ApiException 401 wrong credentials, 409 email taken
     */
    suspend fun login(email: String, password: String): AuthResponseDto
    suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String): AuthResponseDto
}

data class StoredSession(val token: String, val session: Session)

/** Persists the session; implementations must keep the token encrypted at rest (RNF-09). */
interface SessionStore {
    val stored: Flow<StoredSession?>
    suspend fun save(session: StoredSession)
    suspend fun clear()
}
