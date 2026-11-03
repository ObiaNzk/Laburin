package com.example.laburin.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.laburin.domain.model.CvInfo
import com.example.laburin.domain.model.CvUploadResult
import com.example.laburin.domain.model.UserRole
import com.example.laburin.domain.model.Session
import com.example.laburin.domain.repository.AuthRepository
import com.example.laburin.domain.repository.CvRepository
import com.example.laburin.domain.repository.MyOffersRepository
import com.example.laburin.domain.model.PublishedOffer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data object LoggedOut : ProfileUiState
    data class LoggedIn(
        val session: Session,
        val cv: CvInfo? = null,
        val showsCv: Boolean = true,
        val isUploadingCv: Boolean = false,
        val cvError: CvError? = null,
        val myOffers: List<PublishedOffer> = emptyList(),
    ) : ProfileUiState
}

enum class CvError { NotPdf, TooLarge, Network, Server, Unreadable }

sealed interface ProfileEffect {
    data object LoggedOut : ProfileEffect
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val cv: CvRepository,
    private val myOffers: MyOffersRepository,
) : ViewModel() {

    private data class CvUpload(val isUploading: Boolean = false, val error: CvError? = null)

    private val upload = MutableStateFlow(CvUpload())

    val uiState: StateFlow<ProfileUiState> = combine(
        auth.session, cv.observeCv(), upload, myOffers.observeMine(),
    ) { session, cvInfo, upload, offers ->
        when {
            session == null -> ProfileUiState.LoggedOut
            session.role != UserRole.CANDIDATE -> ProfileUiState.LoggedIn(session, cv = null, showsCv = false, myOffers = offers)
            else -> ProfileUiState.LoggedIn(session, cvInfo, isUploadingCv = upload.isUploading, cvError = upload.error)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState.Loading)

    init {
        // Brings the server's CV into the local cache (e.g. right after logging in on a new device).
        viewModelScope.launch {
            when (auth.session.first()?.role) {
                UserRole.CANDIDATE -> cv.current()
                UserRole.BUSINESS -> myOffers.refreshMine()
                null -> Unit
            }
        }
    }

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects: SharedFlow<ProfileEffect> = _effects.asSharedFlow()

    fun onCvPicked(fileName: String, mimeType: String?, bytes: ByteArray) {
        upload.value = CvUpload(isUploading = true)
        viewModelScope.launch {
            val error = when (cv.upload(fileName, mimeType, bytes)) {
                CvUploadResult.Success -> null
                CvUploadResult.NotPdf -> CvError.NotPdf
                CvUploadResult.TooLarge -> CvError.TooLarge
                CvUploadResult.NetworkError -> CvError.Network
                is CvUploadResult.ServerError -> CvError.Server
            }
            upload.value = CvUpload(error = error)
        }
    }

    fun onCvUnreadable() {
        upload.value = CvUpload(error = CvError.Unreadable)
    }

    fun onLogout() {
        viewModelScope.launch {
            auth.logout()
            _effects.emit(ProfileEffect.LoggedOut)
        }
    }
}
