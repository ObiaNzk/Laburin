package com.example.laburin.data.location

import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.LocationResult
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FallbackLocationProviderTest {

    private val here = GeoLocation(-34.6010, -58.3870)
    private val earlier = GeoLocation(-34.6100, -58.4000)
    private val source = FakeDeviceLocationSource()
    private val provider = FallbackLocationProvider(source)

    @Test
    fun `without permission it is denied and the device is not queried`() = runTest {
        source.permission = false

        assertEquals(LocationResult.PermissionDenied, provider.currentLocation())
        assertTrue(source.queries.isEmpty())
    }

    @Test
    fun `uses the current fix when the device provides one`() = runTest {
        source.current = { LocationFix(here, ageMillis = 0) }
        source.lastKnown = { LocationFix(earlier, ageMillis = 60_000) }

        assertEquals(LocationResult.Located(here), provider.currentLocation())
    }

    @Test
    fun `falls back to a recent last known fix when there is no current one`() = runTest {
        source.current = { null }
        source.lastKnown = { LocationFix(earlier, ageMillis = 5 * 60_000) }

        assertEquals(LocationResult.Located(earlier), provider.currentLocation())
    }

    @Test
    fun `falls back to the last known fix when the current request never answers`() = runTest {
        source.current = { awaitCancellation() }
        source.lastKnown = { LocationFix(earlier, ageMillis = 60_000) }

        assertEquals(LocationResult.Located(earlier), provider.currentLocation())
    }

    @Test
    fun `ignores a last known fix older than 30 minutes`() = runTest {
        source.current = { null }
        source.lastKnown = { LocationFix(earlier, ageMillis = 31 * 60_000) }

        assertEquals(LocationResult.Unavailable, provider.currentLocation())
    }

    @Test
    fun `is unavailable when there is no fix at all`() = runTest {
        source.current = { null }
        source.lastKnown = { null }

        assertEquals(LocationResult.Unavailable, provider.currentLocation())
    }

    private class FakeDeviceLocationSource : DeviceLocationSource {
        var permission = true
        var current: suspend () -> LocationFix? = { null }
        var lastKnown: suspend () -> LocationFix? = { null }
        val queries = mutableListOf<String>()

        override fun hasPermission() = permission

        override suspend fun currentFix(): LocationFix? {
            queries += "current"
            return current()
        }

        override suspend fun lastKnownFix(): LocationFix? {
            queries += "lastKnown"
            return lastKnown()
        }
    }
}
