package com.example.laburin.domain.publish

import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.fakes.REQUIRED_FIELDS
import org.junit.Assert.assertEquals
import org.junit.Test

class OfferDraftValidatorTest {

    private val valid = OfferDraft("Mozo", Category.GASTRONOMY, "Sábados", "Atención en salón")

    @Test
    fun `a complete draft is valid`() {
        assertEquals(emptySet<PublishField>(), OfferDraftValidator.validate(valid))
    }

    @Test
    fun `every field is required`() {
        assertEquals(
            REQUIRED_FIELDS,
            OfferDraftValidator.validate(OfferDraft(" ", null, "", "")),
        )
    }

    @Test
    fun `a business sign-up needs name, category, schedule, address, email and an 8 character password`() {
        assertEquals(
            BusinessSignUpField.entries.toSet(),
            BusinessSignUpValidator.validate(name = "", category = null, schedule = " ", address = "", email = "x", password = "1"),
        )
        assertEquals(
            emptySet<BusinessSignUpField>(),
            BusinessSignUpValidator.validate("Taller", Category.MECHANICS, "9 a 18", "[DIRECCION]", "taller@demo.laburin", "12345678"),
        )
    }
}
