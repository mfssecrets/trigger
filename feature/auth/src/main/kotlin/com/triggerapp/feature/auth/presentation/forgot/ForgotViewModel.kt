package com.triggerapp.feature.auth.presentation.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [ViewModel] for the forgot-password entry form. Validates the email shape and hands the
 * flow to the shared 6-digit code screen (which emails the code and enforces cooldowns).
 *
 * @param observeNetworkOnline Connectivity [kotlinx.coroutines.flow.StateFlow]; blocks continue when offline.
 * @author udit
 */
class ForgotViewModel(
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotUiState())
    val state: StateFlow<ForgotUiState> = _state.asStateFlow()

    private val _effects = Channel<ForgotUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /**
     * Reduces [ForgotUiEvent] into [ForgotUiState] or continues the flow.
     *
     * @param event Incoming UI intent.
     * @author udit
     */
    fun onEvent(event: ForgotUiEvent) {
        when (event) {
            is ForgotUiEvent.EmailChanged ->
                _state.update { it.copy(email = event.value, errorMessage = null) }
            ForgotUiEvent.Submit -> submit()
        }
    }

    /**
     * Validates the email and navigates to the code screen.
     * @author udit
     */
    private fun submit() {
        val email = _state.value.email.trim()
        if (email.isEmpty()) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.ENTER_EMAIL) }
            return
        }
        if (!DisplayTextLimits.EMAIL_SHAPE.matches(email)) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.EMAIL_INVALID_FORMAT) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OFFLINE_AUTH_RESET) }
            return
        }
        startResetFlow(email)
        viewModelScope.launch { _effects.send(ForgotUiEffect.NavigateOtp) }
    }
}
