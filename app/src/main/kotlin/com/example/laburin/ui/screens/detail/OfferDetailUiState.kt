package com.example.laburin.ui.screens.detail

import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer

sealed interface OfferDetailUiState {
    data object Loading : OfferDetailUiState

    /** Only what a guest may see; the rest of the offer never reaches the UI without a session. */
    data class Guest(val title: String, val category: Category, val distanceMeters: Int?) : OfferDetailUiState

    /** [application] is the user's application to this offer, if they already applied. */
    data class Full(val offer: Offer, val distanceMeters: Int?, val application: JobApplication? = null) : OfferDetailUiState

    /** The server closed or removed the offer. */
    data object NoLongerAvailable : OfferDetailUiState

    /** Not cached and it couldn't be fetched. */
    data class Unavailable(val isOffline: Boolean) : OfferDetailUiState
}

sealed interface OfferDetailEffect {
    data object NavigateBack : OfferDetailEffect
    data object NavigateToSignUp : OfferDetailEffect
    data object NavigateToLogin : OfferDetailEffect
    data object NavigateToCvUpload : OfferDetailEffect
    data class ShowMessage(val message: OfferDetailMessage) : OfferDetailEffect
}

enum class OfferDetailMessage { BusinessCannotApply }
