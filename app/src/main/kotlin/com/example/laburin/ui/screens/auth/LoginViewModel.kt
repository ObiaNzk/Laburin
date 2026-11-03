package com.example.laburin.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.AuthResult
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
class LoginViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<LoginEffect>()
    val effects: SharedFlow<LoginEffect> = _effects.asSharedFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        if (state.email.isBlank() || state.password.isEmpty()) {
            _uiState.update { it.copy(error = LoginError.MissingFields) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val error = when (val result = auth.login(state.email, state.password)) {
                AuthResult.Success -> null
                AuthResult.InvalidCredentials -> LoginError.InvalidCredentials
                AuthResult.NetworkError -> LoginError.Network
                AuthResult.EmailAlreadyRegistered, is AuthResult.ServerError -> LoginError.Server
            }
            _uiState.update { it.copy(isSubmitting = false, error = error) }
            if (error == null) _effects.emit(LoginEffect.LoggedIn)
        }
    }
}
