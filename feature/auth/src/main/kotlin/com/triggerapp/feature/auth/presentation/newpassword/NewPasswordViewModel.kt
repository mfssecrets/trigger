package com.triggerapp.feature.auth.presentation.newpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.feature.auth.presentation.AuthFlowData
import com.triggerapp.domain.usecase.auth.ResetPasswordWithOtpUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [ViewModel] for the new-password screen: validates both fields, calls the server reset
 * (which verifies the OTP code again), and signs the device in with the returned token.
 *
 * @author udit
 */
class NewPasswordViewModel(
    private val resetPasswordWithOtp: ResetPasswordWithOtpUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NewPasswordUiState())
    val state: StateFlow<NewPasswordUiState> = _state.asStateFlow()

    private val _effects = Channel<NewPasswordUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Reduces [NewPasswordUiEvent] into [NewPasswordUiState] or triggers the password change.
     *
     * @param event User intent from the UI.
     * @author udit
     */
    fun onEvent(event: NewPasswordUiEvent) {
        when (event) {
            is NewPasswordUiEvent.PasswordChanged ->
                _state.update { it.copy(password = event.value, fieldErrors = it.fieldErrors - "password", errorMessage = null) }
            is NewPasswordUiEvent.ConfirmChanged ->
                _state.update { it.copy(confirm = event.value, fieldErrors = it.fieldErrors - "confirm", errorMessage = null) }
            NewPasswordUiEvent.Submit -> submit()
        }
    }

    /**
     * Validates fields and connectivity, then invokes the server-backed reset.
     * @author udit
     */
    private fun submit() {
        val s = _state.value
        if (s.loading) return
        val errors = mutableMapOf<String, String>()
        if (s.password.length < DisplayTextLimits.MIN_PASSWORD_CHARS) {
            errors["password"] = TriggerStrings.Errors.PASSWORD_MIN
        }
        if (s.confirm != s.password || s.confirm.isEmpty()) {
            errors["confirm"] = TriggerStrings.Errors.PASSWORDS_DO_NOT_MATCH
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }
        if (AuthFlowData.isBlank() || AuthFlowData.resetCode.isBlank()) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.GENERIC) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OFFLINE_AUTH_RESET) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null) }
            resetPasswordWithOtp(
                email = AuthFlowData.email,
                code = AuthFlowData.resetCode,
                newPassword = _state.value.password,
            )
                .onSuccess {
                    AuthFlowData.clearReset()
                    AuthFlowData.email = ""
                    _state.update { it.copy(loading = false) }
                    _effects.send(NewPasswordUiEffect.NavigateHome)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.OFFLINE_AUTH_RESET,
                                genericFallback = TriggerStrings.Errors.OTP_INVALID,
                            ),
                        )
                    }
                }
        }
    }
}
