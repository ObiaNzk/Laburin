package com.example.laburin.ui.screens.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.publish.OfferLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ScanUiState {
    data object AskingPermission : ScanUiState
    data object Scanning : ScanUiState
    data object PermissionDenied : ScanUiState
    data object InvalidQr : ScanUiState
}

sealed interface ScanEffect {
    data class OpenOffer(val offerId: String) : ScanEffect
}

@HiltViewModel
class ScanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.AskingPermission)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ScanEffect>()
    val effects: SharedFlow<ScanEffect> = _effects.asSharedFlow()

    /** The camera keeps reporting the same code every frame; only the first one counts. */
    private var handled = false

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.value = if (granted) ScanUiState.Scanning else ScanUiState.PermissionDenied
    }

    fun onCodeScanned(text: String) {
        if (handled || _uiState.value != ScanUiState.Scanning) return
        val offerId = OfferLink.parse(text)
        if (offerId == null) {
            _uiState.value = ScanUiState.InvalidQr
            return
        }
        handled = true
        viewModelScope.launch { _effects.emit(ScanEffect.OpenOffer(offerId)) }
    }

    fun onScanAgain() {
        _uiState.value = ScanUiState.Scanning
    }
}
