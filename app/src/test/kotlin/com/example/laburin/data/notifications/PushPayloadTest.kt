package com.example.laburin.data.notifications

import com.example.laburin.domain.model.NotificationEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushPayloadTest {

    @Test
    fun `an application review push becomes the same event as the local check`() {
        val data = mapOf("type" to "APPLICATION_REVIEWED", "applicationId" to "a-1", "offerTitle" to "Vendedor/a", "approved" to "true")

        assertEquals(NotificationEvent.ApplicationReviewed("a-1", "Vendedor/a", approved = true), PushPayload.parse(data))
    }

    @Test
    fun `a new applicants push is parsed`() {
        val data = mapOf("type" to "NEW_APPLICANTS", "offerId" to "o-1", "offerTitle" to "Mozo", "count" to "3")

        assertEquals(NotificationEvent.NewApplicants("o-1", "Mozo", 3), PushPayload.parse(data))
    }

    @Test
    fun `unknown or incomplete pushes are ignored`() {
        assertNull(PushPayload.parse(mapOf("type" to "PROMO")))
        assertNull(PushPayload.parse(mapOf("type" to "NEW_APPLICANTS", "offerId" to "o-1")))
        assertNull(PushPayload.parse(mapOf("type" to "NEW_APPLICANTS", "offerId" to "o-1", "offerTitle" to "x", "count" to "muchos")))
    }
}
