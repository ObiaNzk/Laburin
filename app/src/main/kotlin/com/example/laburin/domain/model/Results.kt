package com.example.laburin.domain.model

sealed interface RefreshResult {
    data object Success : RefreshResult
    data object NetworkError : RefreshResult
    data class ServerError(val code: Int) : RefreshResult
}

sealed interface LocationResult {
    data class Located(val location: GeoLocation) : LocationResult
    data object PermissionDenied : LocationResult
    data object Unavailable : LocationResult
}
