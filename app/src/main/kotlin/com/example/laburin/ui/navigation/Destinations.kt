package com.example.laburin.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MapDestination

/** [distanceMeters] comes from the map; entry points without a search center (e.g. QR) omit it. */
@Serializable
data class OfferDetailDestination(val offerId: String, val distanceMeters: Int? = null) {
    companion object {
        // Must match the property names: type-safe navigation stores arguments under them.
        const val ARG_OFFER_ID = "offerId"
        const val ARG_DISTANCE_METERS = "distanceMeters"
    }
}

@Serializable
data object SignUpDestination
