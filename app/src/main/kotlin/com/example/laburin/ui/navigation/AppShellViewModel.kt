package com.example.laburin.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppShellViewModel @Inject constructor(sessionRepository: SessionRepository) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> =
        sessionRepository.isLoggedIn.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
