package com.example.laburin.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.auth.SignUpValidator
import com.example.laburin.domain.model.AuthResult
import com.example.laburin.domain.model.SignUpField
import com.example.laburin.domain.model.Terms
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val auth: AuthRepository,
) : ViewModel() {

    // Everything but the password survives the process being killed: a secret is never written to saved state.
    private val _uiState = MutableStateFlow(
        SignUpUiState(
            firstName = savedState[FIRST_NAME] ?: "",
            lastName = savedState[LAST_NAME] ?: "",
            email = savedState[EMAIL] ?: "",
        ),
    )
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SignUpEffect>()
    val effects: SharedFlow<SignUpEffect> = _effects.asSharedFlow()

    fun onFirstNameChange(value: String) = edit(SignUpField.FIRST_NAME) { copy(firstName = value) }
    fun onLastNameChange(value: String) = edit(SignUpField.LAST_NAME) { copy(lastName = value) }
    fun onEmailChange(value: String) = edit(SignUpField.EMAIL) { copy(email = value) }
    fun onPasswordChange(value: String) = edit(SignUpField.PASSWORD) { copy(password = value) }
    fun onTermsAcceptedChange(value: Boolean) = edit(SignUpField.TERMS) { copy(termsAccepted = value) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val fieldErrors = SignUpValidator.validate(state.firstName, state.lastName, state.email, state.password, state.termsAccepted)
        if (fieldErrors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = fieldErrors) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val error = when (auth.signUpCandidate(state.firstName, state.lastName, state.email, state.password, Terms.CURRENT_VERSION)) {
                AuthResult.Success -> null
                AuthResult.EmailAlreadyRegistered -> SignUpError.EmailTaken
                AuthResult.NetworkError -> SignUpError.Network
                AuthResult.InvalidCredentials, is AuthResult.ServerError -> SignUpError.Server
            }
            _uiState.update { it.copy(isSubmitting = false, error = error) }
            if (error == null) _effects.emit(SignUpEffect.SignedUp)
        }
    }

    private fun edit(field: SignUpField, change: SignUpUiState.() -> SignUpUiState) {
        _uiState.update { it.change().copy(fieldErrors = it.fieldErrors - field, error = null) }
        val s = _uiState.value
        savedState[FIRST_NAME] = s.firstName
        savedState[LAST_NAME] = s.lastName
        savedState[EMAIL] = s.email
    }

    private companion object {
        const val FIRST_NAME = "signUp.firstName"
        const val LAST_NAME = "signUp.lastName"
        const val EMAIL = "signUp.email"
    }
}
