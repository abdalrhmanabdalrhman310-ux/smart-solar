package com.smartsolar.app.data.remote

enum class NetworkErrorType {
    SERVER_URL_NOT_CONFIGURED,
    NO_INTERNET,
    TIMEOUT,
    SSL_OR_SECURITY_ERROR,
    UNAUTHORIZED_SESSION_EXPIRED,
    FORBIDDEN_OR_PENDING_APPROVAL,
    INVALID_SERVER_RESPONSE,
    HTTP_NOT_FOUND,
    SERVER_ERROR,
    VALIDATION_ERROR
}

sealed class NetworkResult<out T> {
    data object Loading : NetworkResult<Nothing>()
    data class Success<T>(
        val data: T,
        val message: String = ""
    ) : NetworkResult<T>()

    data class Error(
        val type: NetworkErrorType,
        val arabicMessage: String,
        val technicalDetail: String = "",
        val phpEndpoint: String = "",
        val needsPhpConfirmation: Boolean = false
    ) : NetworkResult<Nothing>()
}
