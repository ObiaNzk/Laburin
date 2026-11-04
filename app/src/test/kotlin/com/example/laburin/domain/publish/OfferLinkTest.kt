package com.example.laburin.domain.publish

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferLinkTest {

    private val printed = "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg?oferta=o-1"

    @Test
    fun `builds the web link printed on the shop sign`() {
        // Spelled out on purpose: a printed QR can't be changed, so this text must not change by accident.
        assertEquals(printed, OfferLink.build("o-1"))
    }

    @Test
    fun `parses the printed link back to its id`() {
        assertEquals("o-1", OfferLink.parse(printed))
    }

    @Test
    fun `other parameters next to the offer are ignored`() {
        val shared = "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg?utm_source=whatsapp&oferta=o-1"

        assertEquals("o-1", OfferLink.parse(shared))
    }

    @Test
    fun `an id with characters that need escaping survives the round trip`() {
        assertEquals("a b&c", OfferLink.parse(OfferLink.build("a b&c")))
    }

    @Test
    fun `still parses the app links printed before the web link`() {
        assertEquals("o-1", OfferLink.parse("laburin://offer/o-1"))
        assertEquals("o-1", OfferLink.parse("  laburin://offer/o-1/ "))
    }

    @Test
    fun `anything else is not an offer`() {
        listOf(
            "https://example.com/offer/o-1",
            "https://example.com/pepefroggie.jpg?oferta=o-1",
            "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg",
            "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg?oferta=",
            "https://i.kym-cdn.com/entries/icons/original/000/017/618/pepefroggie.jpg?ofertas=o-1",
            "laburin://offer/",
            "laburin://profile/u-1",
            "",
            "o-1",
        ).forEach { assertNull(it, OfferLink.parse(it)) }
    }
}
