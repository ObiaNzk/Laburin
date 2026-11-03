package com.example.laburin.domain.publish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferLinkTest {

    @Test
    fun `builds the link printed on the shop sign`() {
        assertEquals("laburin://offer/o-1", OfferLink.build("o-1"))
    }

    @Test
    fun `parses an offer link back to its id`() {
        assertEquals("o-1", OfferLink.parse("laburin://offer/o-1"))
    }

    @Test
    fun `tolerates surrounding whitespace and a trailing slash`() {
        assertEquals("o-1", OfferLink.parse("  laburin://offer/o-1/ "))
    }

    @Test
    fun `anything else is not an offer`() {
        listOf("https://example.com/offer/o-1", "laburin://offer/", "laburin://profile/u-1", "", "o-1")
            .forEach { assertNull(it, OfferLink.parse(it)) }
    }
}
