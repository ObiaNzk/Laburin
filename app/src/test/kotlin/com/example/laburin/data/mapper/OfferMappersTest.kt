package com.example.laburin.data.mapper

import com.example.laburin.data.remote.OfferDto
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class OfferMappersTest {

    private val dto = OfferDto(
        id = "o-1", title = "Mozo", category = "GASTRONOMY", description = "[DESCRIPCION]",
        schedule = "Sábados", jobType = "Part time", businessName = "[COMERCIO]", address = "[DIRECCION]",
        latitude = -34.6, longitude = -58.38, status = "ACTIVE", updatedAt = "2026-11-01T12:00:00Z",
    )

    @Test
    fun `dto maps to entity and entity maps to domain`() {
        val offer = dto.toEntityOrNull()!!.toDomain()

        assertEquals("o-1", offer.id)
        assertEquals(Category.GASTRONOMY, offer.category)
        assertEquals(GeoLocation(-34.6, -58.38), offer.location)
        assertEquals(Instant.parse("2026-11-01T12:00:00Z"), offer.updatedAt)
        assertEquals("[COMERCIO]", offer.businessName)
    }

    @Test
    fun `unknown category from the server is dropped instead of crashing`() {
        assertNull(dto.copy(category = "ASTRONAUTA").toEntityOrNull())
    }

    @Test
    fun `malformed date from the server is dropped instead of crashing`() {
        assertNull(dto.copy(updatedAt = "ayer").toEntityOrNull())
    }
}
