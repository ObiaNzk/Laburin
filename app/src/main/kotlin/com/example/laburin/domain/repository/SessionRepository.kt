package com.example.laburin.domain.repository

import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionRepository {
    /** null when logged out. */
    val session: Flow<Session?>

    val isLoggedIn: Flow<Boolean> get() = session.map { it != null }
}

/** Sign-up and login need a connection (planning doc §13); the session then works offline. */
interface AuthRepository : SessionRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String): AuthResult
    suspend fun logout()
}
