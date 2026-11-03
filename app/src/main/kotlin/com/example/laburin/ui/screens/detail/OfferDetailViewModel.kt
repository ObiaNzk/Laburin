package com.example.laburin.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.ApplyResult
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.model.Offer
import com.example.laburin.domain.model.RefreshResult
import com.example.laburin.domain.apply.ApplyToOfferUseCase
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.ConnectivityObserver
import com.example.laburin.domain.repository.SessionRepository
import com.example.laburin.domain.usecase.ObserveOfferUseCase
import com.example.laburin.domain.usecase.RefreshOfferUseCase
import com.example.laburin.ui.navigation.OfferDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OfferDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeOffer: ObserveOfferUseCase,
    private val refreshOffer: RefreshOfferUseCase,
    sessionRepository: SessionRepository,
    connectivityObserver: ConnectivityObserver,
    applications: ApplicationRepository,
    private val applyToOffer: ApplyToOfferUseCase,
) : ViewModel() {

    private val offerId: String = checkNotNull(savedStateHandle[OfferDetailDestination.ARG_OFFER_ID])
    private val distanceMeters: Int? = savedStateHandle[OfferDetailDestination.ARG_DISTANCE_METERS]

    /** null until the first refresh attempt finishes. */
    private val lastRefresh = MutableStateFlow<RefreshResult?>(null)

    private val isOnline: StateFlow<Boolean> =
        connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val offer = observeOffer(offerId).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<OfferDetailUiState> = combine(
        offer, sessionRepository.isLoggedIn, lastRefresh, applications.observeForOffer(offerId), ::toUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OfferDetailUiState.Loading)

    private val _effects = MutableSharedFlow<OfferDetailEffect>()
    val effects: SharedFlow<OfferDetailEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch { refresh() }
        // Opened offline (e.g. a QR scanned on the street): load it as soon as the connection is back.
        viewModelScope.launch {
            var wasOnline = isOnline.value
            isOnline.collect { online ->
                if (online && !wasOnline && offer.value == null) refresh()
                wasOnline = online
            }
        }
    }

    fun onRetry() {
        viewModelScope.launch { refresh() }
    }

    fun onBack() = sendEffect(OfferDetailEffect.NavigateBack)

    fun onCreateAccount() = sendEffect(OfferDetailEffect.NavigateToSignUp)

    fun onLogin() = sendEffect(OfferDetailEffect.NavigateToLogin)

    fun onApply() {
        val current = offer.value ?: return
        viewModelScope.launch {
            when (applyToOffer(current)) {
                ApplyResult.NeedsAccount -> _effects.emit(OfferDetailEffect.NavigateToSignUp)
                ApplyResult.NeedsCv -> _effects.emit(OfferDetailEffect.NavigateToCvUpload)
                ApplyResult.NotACandidate -> _effects.emit(OfferDetailEffect.ShowMessage(OfferDetailMessage.BusinessCannotApply))
                // The new or existing application reaches the screen through the repository flow.
                is ApplyResult.Queued, is ApplyResult.AlreadyApplied -> Unit
            }
        }
    }

    private suspend fun refresh() {
        lastRefresh.value = if (isOnline.value) refreshOffer(offerId) else RefreshResult.NetworkError
    }

    private fun sendEffect(effect: OfferDetailEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun toUiState(
        offer: Offer?,
        isLoggedIn: Boolean,
        refresh: RefreshResult?,
        application: JobApplication?,
    ): OfferDetailUiState = when {
        offer != null && isLoggedIn -> OfferDetailUiState.Full(offer, distanceMeters, application)
        offer != null -> OfferDetailUiState.Guest(offer.title, offer.category, distanceMeters)
        refresh == null -> OfferDetailUiState.Loading
        refresh == RefreshResult.Success -> OfferDetailUiState.NoLongerAvailable
        else -> OfferDetailUiState.Unavailable(isOffline = refresh == RefreshResult.NetworkError || !isOnline.value)
    }
}
