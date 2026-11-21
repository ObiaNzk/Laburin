package com.example.laburin.ui.screens.cv

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.CvFileResult
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.ui.navigation.CvViewerDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CvViewerUiState {
    data object Loading : CvViewerUiState
    data class Ready(val path: String) : CvViewerUiState
    data object NotUploaded : CvViewerUiState
    data class Error(val error: CvViewerError) : CvViewerUiState
}

enum class CvViewerError { Network, Server, Unreadable }

@HiltViewModel
class CvViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cv: CvRepository,
) : ViewModel() {

    /** Null for the user's own CV; set when a business opens an applicant's. */
    private val applicationId: String? = savedStateHandle[CvViewerDestination.ARG_APPLICATION_ID]

    val isOwnCv: Boolean = applicationId == null

    private val _uiState = MutableStateFlow<CvViewerUiState>(CvViewerUiState.Loading)
    val uiState: StateFlow<CvViewerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

    /** The file arrived but the phone's PDF renderer could not open it (damaged or unusual PDF). */
    fun onRenderFailed() {
        _uiState.value = CvViewerUiState.Error(CvViewerError.Unreadable)
    }

    /** The screen is gone for good: the downloaded copy must not stay on the phone. */
    override fun onCleared() = cv.closeFile()

    private fun load() {
        _uiState.value = CvViewerUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = cv.openFile(applicationId)) {
                is CvFileResult.Ready -> CvViewerUiState.Ready(result.path)
                CvFileResult.NotUploaded -> CvViewerUiState.NotUploaded
                CvFileResult.NetworkError -> CvViewerUiState.Error(CvViewerError.Network)
                is CvFileResult.ServerError -> CvViewerUiState.Error(CvViewerError.Server)
            }
        }
    }
}
