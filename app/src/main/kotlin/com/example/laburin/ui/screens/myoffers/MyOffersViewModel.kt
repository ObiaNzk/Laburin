package com.example.laburin.ui.screens.myoffers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.PublishedOffer
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.ui.screens.common.RefreshThrottle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

sealed interface MyOffersUiState {
    data object Loading : MyOffersUiState
    data object Empty : MyOffersUiState
    data class Content(val offers: List<PublishedOffer>) : MyOffersUiState
}

/** The business's middle tab: its publications, read from Room and refreshed from the server. */
@HiltViewModel
class MyOffersViewModel @Inject constructor(
    private val offers: MyOffersRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<MyOffersUiState> = offers.observeMine()
        .map { list -> if (list.isEmpty()) MyOffersUiState.Empty else MyOffersUiState.Content(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyOffersUiState.Loading)

    private val refreshOnReturn = RefreshThrottle(clock)

    init {
        refreshOnReturn.ifStale(::refresh)
    }

    /** The tab became visible again (onStart): statuses may have changed on the server meanwhile. */
    fun onScreenStarted() = refreshOnReturn.ifStale(::refresh)

    private fun refresh() {
        viewModelScope.launch { offers.refreshMine() }
    }
}
