package com.example.laburin.domain.repository

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val isLoggedIn: Flow<Boolean>
}
