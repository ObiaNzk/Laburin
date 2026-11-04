package com.example.laburin.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiConfigTest {

    @Test
    fun `without an API address the app uses the in-memory fake backend`() {
        assertTrue(ApiConfig.usesFakeBackend(""))
        assertTrue(ApiConfig.usesFakeBackend("  "))
    }

    @Test
    fun `with an API address the app talks to the real backend`() {
        assertFalse(ApiConfig.usesFakeBackend("https://[DOMINIO]/api/"))
    }
}
