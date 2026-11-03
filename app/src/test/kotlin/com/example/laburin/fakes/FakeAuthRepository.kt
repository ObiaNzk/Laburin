package com.example.laburin.fakes

import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow

val CANDIDATE_SESSION = Session("u-1", UserRole.CANDIDATE, "[NOMBRE]", "[EMAIL]")
val BUSINESS_SESSION = Session("b-1", UserRole.BUSINESS, "[COMERCIO]", "[EMAIL]")

class FakeAuthRepository(initial: Session? = null) : AuthRepository {
    override val session = MutableStateFlow(initial)
    var nextResult: AuthResult = AuthResult.Success
    var sessionOnSuccess: Session = CANDIDATE_SESSION
    /** Suspends inside the call so a test can observe the in-flight state. */
    var gate: suspend () -> Unit = {}
    val calls = mutableListOf<String>()

    override suspend fun login(email: String, password: String) = respond("login:$email")

    override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) =
        respond("signUp:$firstName $lastName:$email")

    override suspend fun logout() {
        calls += "logout"
        session.value = null
    }

    private suspend fun respond(call: String): AuthResult {
        calls += call
        gate()
        if (nextResult == AuthResult.Success) session.value = sessionOnSuccess
        return nextResult
    }
}
