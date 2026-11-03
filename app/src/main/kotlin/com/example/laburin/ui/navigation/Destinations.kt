package com.example.laburin.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MapDestination

/** [distanceMeters] comes from the map; entry points without a search center (e.g. QR) omit it. */
@Serializable
data class OfferDetailDestination(val offerId: String, val distanceMeters: Int? = null, val fromQr: Boolean = false) {
    companion object {
        // Must match the property names: type-safe navigation stores arguments under them.
        const val ARG_OFFER_ID = "offerId"
        const val ARG_DISTANCE_METERS = "distanceMeters"
        const val ARG_FROM_QR = "fromQr"
    }
}

@Serializable
data object SignUpDestination

@Serializable
data object LoginDestination

@Serializable
data object ProfileDestination

@Serializable
data object ApplicationsDestination

@Serializable
data object ScanDestination

/** Deep link printed in the shop sign's QR (see OfferLink). */
const val OFFER_DEEP_LINK_BASE = "laburin://offer"

@Serializable
data object PublishDestination

@Serializable
data object BusinessSignUpDestination

@Serializable
data class PublishedOfferDestination(val offerId: String) {
    companion object {
        const val ARG_OFFER_ID = "offerId"
    }
}
