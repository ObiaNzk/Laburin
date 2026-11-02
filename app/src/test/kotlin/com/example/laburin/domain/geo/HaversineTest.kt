package com.example.laburin.domain.geo

import com.example.laburin.domain.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineTest {

    private val obelisco = GeoLocation(-34.6037, -58.3816)

    @Test
    fun `distance to itself is zero`() {
        assertEquals(0.0, Haversine.distanceMeters(obelisco, obelisco), 0.001)
    }

    @Test
    fun `distance between Obelisco and Plaza de Mayo is about 1 km`() {
        val plazaDeMayo = GeoLocation(-34.6083, -58.3712)
        assertEquals(1060.0, Haversine.distanceMeters(obelisco, plazaDeMayo), 30.0)
    }

    @Test
    fun `bounding box contains every point within the radius`() {
        val box = Haversine.boundingBox(obelisco, radiusMeters = 2_000.0)
        val north = GeoLocation(obelisco.latitude + 0.0179, obelisco.longitude)
        val east = GeoLocation(obelisco.latitude, obelisco.longitude + 0.0218)
        listOf(north, east).forEach {
            assertTrue(Haversine.distanceMeters(obelisco, it) <= 2_000.0)
            assertTrue(it.latitude in box.minLatitude..box.maxLatitude)
            assertTrue(it.longitude in box.minLongitude..box.maxLongitude)
        }
    }

    @Test
    fun `bounding box is centered on the origin`() {
        val box = Haversine.boundingBox(obelisco, radiusMeters = 5_000.0)
        assertEquals(obelisco.latitude, (box.minLatitude + box.maxLatitude) / 2, 1e-9)
        assertEquals(obelisco.longitude, (box.minLongitude + box.maxLongitude) / 2, 1e-9)
    }
}
