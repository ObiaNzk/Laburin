package com.example.laburin.ui.screens.publish

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.BusinessSignUp
import com.example.laburin.domain.model.BusinessSignUpField
import com.example.laburin.domain.model.Category
import com.example.laburin.domain.model.LocationResult
import com.example.laburin.domain.model.OfferDraft
import com.example.laburin.domain.model.PublishField
import com.example.laburin.domain.model.PublishResult
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.publish.BusinessSignUpValidator
import com.example.laburin.domain.publish.OfferLink
import com.example.laburin.domain.publish.PublishOfferUseCase
import com.example.laburin.domain.publish.ReviewApplicantUseCase
import com.example.laburin.domain.repository.ApplicantsRepository
import com.example.laburin.domain.model.Applicant
import com.example.laburin.domain.repository.AuthRepository
import com.example.laburin.domain.repository.LocationProvider
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.ui.navigation.PublishedOfferDestination
import com.example.laburin.ui.screens.map.MapUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublishUiState(
    val title: String = "",
    val category: Category? = null,
    val schedule: String = "",
    val description: String = "",
    val fieldErrors: Set<PublishField> = emptySet(),
    val isPublishing: Boolean = false,
)

sealed interface PublishEffect {
    data object NeedsBusinessAccount : PublishEffect
    data class Published(val offerId: String) : PublishEffect
}

/** The form survives the detour to register the business: this ViewModel stays on the back stack. */
@HiltViewModel
class PublishViewModel @Inject constructor(private val publishOffer: PublishOfferUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(PublishUiState())
    val uiState: StateFlow<PublishUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PublishEffect>()
    val effects: SharedFlow<PublishEffect> = _effects.asSharedFlow()

    fun onTitleChange(value: String) = edit(PublishField.TITLE) { copy(title = value) }
    fun onCategoryChange(value: Category) = edit(PublishField.CATEGORY) { copy(category = value) }
    fun onScheduleChange(value: String) = edit(PublishField.SCHEDULE) { copy(schedule = value) }
    fun onDescriptionChange(value: String) = edit(PublishField.DESCRIPTION) { copy(description = value) }

    fun onPublish() {
        val state = _uiState.value
        if (state.isPublishing) return
        _uiState.update { it.copy(isPublishing = true) }
        viewModelScope.launch {
            val result = publishOffer(OfferDraft(state.title, state.category, state.schedule, state.description))
            _uiState.update { it.copy(isPublishing = false) }
            when (result) {
                PublishResult.NeedsBusinessAccount -> _effects.emit(PublishEffect.NeedsBusinessAccount)
                is PublishResult.Invalid -> _uiState.update { it.copy(fieldErrors = result.fields) }
                is PublishResult.Queued -> _effects.emit(PublishEffect.Published(result.offer.id))
            }
        }
    }

    private fun edit(field: PublishField, change: PublishUiState.() -> PublishUiState) =
        _uiState.update { it.change().copy(fieldErrors = it.fieldErrors - field) }
}

data class BusinessSignUpUiState(
    val name: String = "",
    val category: Category? = null,
    val schedule: String = "",
    val address: String = "",
    val email: String = "",
    val password: String = "",
    val fieldErrors: Set<BusinessSignUpField> = emptySet(),
    val isSubmitting: Boolean = false,
    val error: BusinessSignUpError? = null,
)

enum class BusinessSignUpError { EmailTaken, Network, Server }

sealed interface BusinessSignUpEffect {
    data object SignedUp : BusinessSignUpEffect
}

@HiltViewModel
class BusinessSignUpViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessSignUpUiState())
    val uiState: StateFlow<BusinessSignUpUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BusinessSignUpEffect>()
    val effects: SharedFlow<BusinessSignUpEffect> = _effects.asSharedFlow()

    fun onNameChange(value: String) = edit(BusinessSignUpField.NAME) { copy(name = value) }
    fun onCategoryChange(value: Category) = edit(BusinessSignUpField.CATEGORY) { copy(category = value) }
    fun onScheduleChange(value: String) = edit(BusinessSignUpField.SCHEDULE) { copy(schedule = value) }
    fun onAddressChange(value: String) = edit(BusinessSignUpField.ADDRESS) { copy(address = value) }
    fun onEmailChange(value: String) = edit(BusinessSignUpField.EMAIL) { copy(email = value) }
    fun onPasswordChange(value: String) = edit(BusinessSignUpField.PASSWORD) { copy(password = value) }

    fun onSubmit() {
        val s = _uiState.value
        if (s.isSubmitting) return
        val errors = BusinessSignUpValidator.validate(s.name, s.category, s.schedule, s.address, s.email, s.password)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            // The business usually registers from the shop; without a fix it starts at the default center.
            val place = (location.currentLocation() as? LocationResult.Located)?.location ?: MapUiState.DEFAULT_CENTER
            val data = BusinessSignUp(s.name, checkNotNull(s.category), s.schedule, s.address, s.email, s.password)
            val error = when (auth.signUpBusiness(data, place)) {
                AuthResult.Success -> null
                AuthResult.EmailAlreadyRegistered -> BusinessSignUpError.EmailTaken
                AuthResult.NetworkError -> BusinessSignUpError.Network
                AuthResult.InvalidCredentials, is AuthResult.ServerError -> BusinessSignUpError.Server
            }
            _uiState.update { it.copy(isSubmitting = false, error = error) }
            if (error == null) _effects.emit(BusinessSignUpEffect.SignedUp)
        }
    }

    private fun edit(field: BusinessSignUpField, change: BusinessSignUpUiState.() -> BusinessSignUpUiState) =
        _uiState.update { it.change().copy(fieldErrors = it.fieldErrors - field, error = null) }
}

sealed interface PublishedOfferUiState {
    data object Loading : PublishedOfferUiState
    data object NotFound : PublishedOfferUiState
    data class Content(
        val offer: PublishedOffer,
        val qrLink: String,
        val applicants: List<Applicant> = emptyList(),
    ) : PublishedOfferUiState
}

sealed interface PublishedOfferEffect {
    data class Share(val title: String, val link: String) : PublishedOfferEffect
}

@HiltViewModel
class PublishedOfferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    offers: MyOffersRepository,
    private val applicants: ApplicantsRepository,
    private val reviewApplicant: ReviewApplicantUseCase,
) : ViewModel() {

    private val offerId: String = checkNotNull(savedStateHandle[PublishedOfferDestination.ARG_OFFER_ID])

    val uiState: StateFlow<PublishedOfferUiState> = combine(offers.observe(offerId), applicants.observeForOffer(offerId)) { offer, list ->
        offer?.let { PublishedOfferUiState.Content(it, OfferLink.build(it.id), list) } ?: PublishedOfferUiState.NotFound
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PublishedOfferUiState.Loading)

    init {
        viewModelScope.launch { applicants.refresh(offerId) }
    }

    private val _effects = MutableSharedFlow<PublishedOfferEffect>()
    val effects: SharedFlow<PublishedOfferEffect> = _effects.asSharedFlow()

    fun onApprove(applicationId: String) {
        viewModelScope.launch { reviewApplicant(applicationId, approve = true) }
    }

    fun onReject(applicationId: String) {
        viewModelScope.launch { reviewApplicant(applicationId, approve = false) }
    }

    fun onShare() {
        val content = uiState.value as? PublishedOfferUiState.Content ?: return
        viewModelScope.launch { _effects.emit(PublishedOfferEffect.Share(content.offer.title, content.qrLink)) }
    }
}
