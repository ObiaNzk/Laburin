package com.example.laburin.data.auth

import com.example.laburin.data.remote.ApiException
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthRepository @Inject constructor(
    private val remote: AuthRemoteDataSource,
    private val store: SessionStore,
) : AuthRepository {

    override val session: Flow<Session?> = store.stored.map { it?.session }

    override suspend fun login(email: String, password: String): AuthResult =
        authenticate { remote.login(email.trim(), password) }

    override suspend fun signUpCandidate(firstName: String, lastName: String, email: String, password: String) =
        authenticate { remote.signUpCandidate(firstName.trim(), lastName.trim(), email.trim(), password) }

    override suspend fun logout() = store.clear()

    private suspend fun authenticate(call: suspend () -> AuthResponseDto): AuthResult {
        val response = try {
            call()
        } catch (e: IOException) {
            return AuthResult.NetworkError
        } catch (e: ApiException) {
            return when (e.code) {
                HTTP_UNAUTHORIZED -> AuthResult.InvalidCredentials
                HTTP_CONFLICT -> AuthResult.EmailAlreadyRegistered
                else -> AuthResult.ServerError(e.code)
            }
        }
        val session = response.user.toSession() ?: return AuthResult.ServerError(HTTP_OK)
        store.save(StoredSession(response.token, session))
        return AuthResult.Success
    }

    private fun UserDto.toSession(): Session? {
        val role = UserRole.entries.firstOrNull { it.name == role } ?: return null
        return Session(userId = id, role = role, displayName = displayName, email = email)
    }

    private companion object {
        const val HTTP_OK = 200
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_CONFLICT = 409
    }
}
