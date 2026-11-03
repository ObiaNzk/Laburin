package com.example.laburin.ui.screens.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.JobApplication
import com.example.laburin.domain.repository.ApplicationRepository
import com.example.laburin.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ApplicationsUiState {
    data object Loading : ApplicationsUiState
    data object LoggedOut : ApplicationsUiState
    data object Empty : ApplicationsUiState
    data class Content(val applications: List<JobApplication>, val pendingToSend: Int) : ApplicationsUiState
}

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val applications: ApplicationRepository,
    session: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<ApplicationsUiState> = combine(
        session.isLoggedIn, applications.observeMine(), applications.observePendingCount(),
    ) { loggedIn, list, pending ->
        when {
            !loggedIn -> ApplicationsUiState.LoggedOut
            list.isEmpty() -> ApplicationsUiState.Empty
            else -> ApplicationsUiState.Content(list, pending)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ApplicationsUiState.Loading)

    init {
        onRefresh()
    }

    /** Statuses come from the server (approved, rejected, closed); a failure keeps what is cached. */
    fun onRefresh() {
        viewModelScope.launch { applications.refreshMine() }
    }
}
