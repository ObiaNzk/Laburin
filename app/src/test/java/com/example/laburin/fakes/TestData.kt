package com.example.laburin.fakes

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.GeoLocation
import com.example.laburin.domain.model.Offer
import java.time.Instant

val OBELISCO = GeoLocation(-34.6037, -58.3816)

fun testOffer(
    id: String = "offer-1",
    title: String = "Ayudante de taller",
    category: Category = Category.MECHANICS,
    location: GeoLocation = OBELISCO,
) = Offer(
    id = id,
    title = title,
    category = category,
    description = "[DESCRIPCION]",
    schedule = "Lunes a viernes, 8 a 17 h",
    jobType = "Jornada completa",
    businessName = "[COMERCIO]",
    address = "[DIRECCION]",
    location = location,
    updatedAt = Instant.parse("2026-11-01T12:00:00Z"),
)
