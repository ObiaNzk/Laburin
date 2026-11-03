package com.example.laburin.ui.screens.profile

import androidx.lifecycle.ViewModel
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object LoggedOut : ProfileUiState
    data class LoggedIn(val session: Session) : ProfileUiState
}

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = auth.session
        .map { session -> session?.let(ProfileUiState::LoggedIn) ?: ProfileUiState.LoggedOut }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    fun onLogout() {
        viewModelScope.launch {
            auth.logout()
            _effects.emit(ProfileEffect.LoggedOut)
        }
    }
}
