package com.example.laburin.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MapCenterOffsetTest {

    @Test
    fun `moves the center up by half of what the bottom panel covers`() {
        assertEquals(-210, mapCenterOffsetY(mapHeightPx = 1_000, bottomObstructionFraction = 0.42f))
    }

    @Test
    fun `keeps the center when nothing covers the map`() {
        assertEquals(0, mapCenterOffsetY(mapHeightPx = 1_000, bottomObstructionFraction = 0f))
    }

    @Test
    fun `keeps the center before the map has been measured`() {
        assertEquals(0, mapCenterOffsetY(mapHeightPx = 0, bottomObstructionFraction = 0.42f))
    }
}
