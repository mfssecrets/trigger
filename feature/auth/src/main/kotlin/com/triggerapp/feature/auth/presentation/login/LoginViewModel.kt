package com.triggerapp.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.auth.SignInUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [ViewModel] for login: [state], [onEvent], and [effects] for navigation.
 * Depends on domain use cases only (Clean Architecture).
 *
 * @param signInUseCase Persists sign-in through the domain layer.
 * @param observeNetworkOnline Hot [kotlinx.coroutines.flow.StateFlow] of connectivity; guards submit when offline.
 * @author udit
 */
class LoginViewModel(
    private val signInUseCase: SignInUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Reduces [LoginUiEvent] into [LoginUiState] or starts async sign-in.
     *
     * @param event User or lifecycle intent from the UI.
     * @author udit
     */
    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged ->
                _state.update { it.copy(email = event.value, errorMessage = null) }
            is LoginUiEvent.PasswordChanged ->
                _state.update { it.copy(password = event.value, errorMessage = null) }
            LoginUiEvent.Submit -> submit()
            LoginUiEvent.ClearForm -> _state.update { LoginUiState() }
        }
    }

    /**
     * Validates input and online state, then invokes [signInUseCase].
     * @author udit
     */
    private fun submit() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        if (email.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.FILL_EMAIL_PASSWORD) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OFFLINE_AUTH_SIGN_IN) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            signInUseCase(email, password)
                .onSuccess {
                    _state.update { it.copy(loading = false) }
                    _effects.send(LoginUiEffect.NavigateHome)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.OFFLINE_AUTH_SIGN_IN,
                                genericFallback = TriggerStrings.Errors.SIGN_IN_FAILED,
                            ),
                        )
                    }
                }
        }
    }
}
