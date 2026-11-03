package com.example.laburin.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> =
        auth.isLoggedIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isSessionExpired: StateFlow<Boolean> =
        auth.isSessionExpired.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onExpiredNoticeDismissed() {
        viewModelScope.launch { auth.acknowledgeSessionExpired() }
    }
}
