package com.smartsolar.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartsolar.app.AppContainer
import com.smartsolar.app.data.local.SessionPreferencesManager
import com.smartsolar.app.data.remote.NetworkClientFactory
import com.smartsolar.app.data.remote.NetworkResult
import com.smartsolar.app.data.remote.PlatformSettingsDto
import com.smartsolar.app.data.repository.AuthRepository
import com.smartsolar.app.data.repository.ServicesRepository
import com.smartsolar.app.domain.model.ServerConfig
import com.smartsolar.app.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppRootState(
    val isInitialized: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val session: UserSession = UserSession(),
    val serverConfig: ServerConfig = ServerConfig(),
    val cartCount: Int = 0
)

sealed class ConnectionTestState {
    data object Idle : ConnectionTestState()
    data object Testing : ConnectionTestState()
    data class Success(val message: String, val settings: PlatformSettingsDto? = null) : ConnectionTestState()
    data class Failure(val message: String, val detail: String = "") : ConnectionTestState()
}

class AppSessionViewModel(
    private val sessionPreferences: SessionPreferencesManager,
    private val networkFactory: NetworkClientFactory,
    private val authRepository: AuthRepository,
    private val servicesRepository: ServicesRepository
) : ViewModel() {

    val rootState: StateFlow<AppRootState> = combine(
        sessionPreferences.onboardingCompletedFlow,
        sessionPreferences.persistedSessionFlow,
        sessionPreferences.ephemeralSession,
        sessionPreferences.serverConfigFlow,
        sessionPreferences.cartItemsFlow
    ) { onboardingDone, persisted, ephemeral, serverConfig, cart ->
        val activeSession = when {
            ephemeral != null && (ephemeral.isAuthenticated || !ephemeral.isApprovedForDashboard) -> ephemeral
            else -> persisted
        }
        AppRootState(
            isInitialized = true,
            onboardingCompleted = onboardingDone,
            session = activeSession,
            serverConfig = serverConfig,
            cartCount = cart.sumOf { it.quantity }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppRootState()
    )

    private val _sessionExpiredMessage = MutableStateFlow<String?>(null)
    val sessionExpiredMessage: StateFlow<String?> = _sessionExpiredMessage.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _profileActionState = MutableStateFlow<NetworkResult<UserSession>?>(null)
    val profileActionState: StateFlow<NetworkResult<UserSession>?> = _profileActionState.asStateFlow()

    init {
        viewModelScope.launch {
            networkFactory.sessionExpiredEvents.collect {
                sessionPreferences.clearSession()
                _sessionExpiredMessage.value = "انتهت صلاحية الجلسة، يرجى تسجيل الدخول مجدداً."
            }
        }
    }

    fun consumeSessionExpiredMessage() {
        _sessionExpiredMessage.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            sessionPreferences.setOnboardingCompleted(true)
        }
    }

    fun saveServerSettings(baseUrl: String, timeoutSeconds: Int) {
        viewModelScope.launch {
            sessionPreferences.saveServerConfig(baseUrl, timeoutSeconds)
            _connectionTestState.value = ConnectionTestState.Idle
        }
    }

    fun testServerConnection(baseUrl: String, timeoutSeconds: Int) {
        viewModelScope.launch {
            val trimmed = baseUrl.trim()
            if (trimmed.isBlank() || (!trimmed.startsWith("https://") && !trimmed.startsWith("http://"))) {
                _connectionTestState.value = ConnectionTestState.Failure(
                    "يرجى إدخال رابط خادم صحيح يبدأ بـ https:// أو http://"
                )
                return@launch
            }
            sessionPreferences.saveServerConfig(trimmed, timeoutSeconds)
            _connectionTestState.value = ConnectionTestState.Testing
            when (val res = servicesRepository.testServerConnection()) {
                is NetworkResult.Success -> {
                    _connectionTestState.value = ConnectionTestState.Success(
                        message = res.message.ifBlank { "تم الاتصال بالخادم بنجاح" },
                        settings = res.data
                    )
                }
                is NetworkResult.Error -> {
                    _connectionTestState.value = ConnectionTestState.Failure(
                        message = res.arabicMessage,
                        detail = res.technicalDetail
                    )
                }
                NetworkResult.Loading -> Unit
            }
        }
    }

    fun resetConnectionTestState() {
        _connectionTestState.value = ConnectionTestState.Idle
    }

    fun updateProfile(fullName: String, phone: String, address: String) {
        viewModelScope.launch {
            if (fullName.isBlank() || phone.isBlank()) {
                _profileActionState.value = NetworkResult.Error(
                    type = com.smartsolar.app.data.remote.NetworkErrorType.VALIDATION_ERROR,
                    arabicMessage = "يرجى إدخال الاسم الكامل ورقم الهاتف."
                )
                return@launch
            }
            _profileActionState.value = NetworkResult.Loading
            _profileActionState.value = authRepository.updateProfile(fullName, phone, address)
        }
    }

    fun clearProfileActionState() {
        _profileActionState.value = null
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppSessionViewModel(
                        container.sessionPreferences,
                        container.networkFactory,
                        container.authRepository,
                        container.servicesRepository
                    ) as T
                }
            }
    }
}
