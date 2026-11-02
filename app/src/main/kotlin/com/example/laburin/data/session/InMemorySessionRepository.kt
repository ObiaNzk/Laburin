package com.example.laburin.data.session

import com.example.laburin.domain.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Placeholder until sign-up/login exist. Replace with a Keystore-backed session (RNF-09). */
@Singleton
class InMemorySessionRepository @Inject constructor() : SessionRepository {
    override val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(false)
}
