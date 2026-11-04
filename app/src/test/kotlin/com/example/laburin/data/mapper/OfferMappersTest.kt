package com.example.laburin.data.mapper

import com.example.laburin.data.remote.OfferDto
import com.example.laburin.data.remote.SalaryDto
import com.example.laburin.domain.model.Salary
import com.example.laburin.domain.model.SalaryPeriod
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

    // --- Salary (optional) ---

    @Test
    fun `a salary range from the server reaches the domain`() {
        val offer = dto.copy(salary = SalaryDto(5_000, 7_000, "HOUR")).toEntityOrNull()!!.toDomain()

        assertEquals(Salary(5_000, 7_000, SalaryPeriod.HOUR), offer.salary)
    }

    @Test
    fun `a single amount has no maximum`() {
        val offer = dto.copy(salary = SalaryDto(600_000, null, "MONTH")).toEntityOrNull()!!.toDomain()

        assertEquals(Salary(600_000, null, SalaryPeriod.MONTH), offer.salary)
    }

    @Test
    fun `an offer without salary is shown as to be agreed`() {
        assertNull(dto.toEntityOrNull()!!.toDomain().salary)
    }

    @Test
    fun `a salary the app cannot read is dropped but the offer is kept`() {
        val offer = dto.copy(salary = SalaryDto(5_000, null, "QUINCENA")).toEntityOrNull()

        assertEquals("o-1", offer?.id)
        assertNull(offer!!.toDomain().salary)
    }
}
