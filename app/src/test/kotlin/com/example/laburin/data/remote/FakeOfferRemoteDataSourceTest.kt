package com.example.laburin.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class FakeOfferRemoteDataSourceTest {

    private val dataSource = FakeOfferRemoteDataSource(com.example.laburin.data.fakebackend.FakeBackend())

    @Test
    fun `getOffer returns an active seed offer`() = runTest {
        assertEquals("ACTIVE", dataSource.getOffer("f-1")?.status)
    }

    @Test
    fun `getOffer also returns closed offers, like the real API`() = runTest {
        assertEquals("CLOSED", dataSource.getOffer("f-7")?.status)
    }

    @Test
    fun `getOffer returns null for an unknown id`() = runTest {
        assertNull(dataSource.getOffer("does-not-exist"))
    }

    @Test(expected = IOException::class)
    fun `getOffer fails like a network error when simulating failures`() = runTest {
        dataSource.simulateNetworkFailure = true
        dataSource.getOffer("f-1")
    }
}
