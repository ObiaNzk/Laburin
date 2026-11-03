package com.example.laburin.domain.repository

import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.BusinessSignUp
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionRepository {
    /** null when logged out. */
    val session: Flow<Session?>

    val isLoggedIn: Flow<Boolean> get() = session.map { it != null }
}

/** Called when the server rejects the token (401). Pending actions are kept for the next login. */
fun interface SessionExpiryNotifier {
    suspend fun sessionExpired()
}

/** Sign-up and login need a connection (planning doc §13); the session then works offline. */
interface AuthRepository : SessionRepository, SessionExpiryNotifier {
    /** True after the server rejected the session, until the user dismisses the notice or logs in again. */
    val isSessionExpired: Flow<Boolean>
    suspend fun acknowledgeSessionExpired()

    suspend fun login(email: String, password: String): AuthResult
    suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String): AuthResult

    /** [location] places the business (and its offers) on the map. */
    suspend fun signUpBusiness(data: BusinessSignUp, location: GeoLocation): AuthResult
    suspend fun logout()
}
