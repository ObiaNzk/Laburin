package com.example.laburin.data.session

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemorySessionRepositoryTest {

    @Test
    fun `starts logged out until sign-up and login exist`() = runTest {
        assertEquals(false, InMemorySessionRepository().isLoggedIn.first())
    }
}
