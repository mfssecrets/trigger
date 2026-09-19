package com.triggerapp.feature.auth.presentation.forgot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.auth.SendPasswordResetUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [ViewModel] for password reset email flow.
 * Depends on domain use cases only (Clean Architecture).
 *
 * @param sendPasswordResetUseCase Triggers reset email through the domain layer.
 * @param observeNetworkOnline Connectivity [kotlinx.coroutines.flow.StateFlow]; blocks request when offline.
 * @author udit
 */
class ForgotViewModel(
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotUiState())
    val state: StateFlow<ForgotUiState> = _state.asStateFlow()

    /**
     * Reduces [ForgotUiEvent] into [ForgotUiState] or starts the reset request.
     *
     * @param event Incoming UI intent.
     * @author udit
     */
    fun onEvent(event: ForgotUiEvent) {
        when (event) {
            is ForgotUiEvent.EmailChanged ->
                _state.update { it.copy(email = event.value, errorMessage = null, message = null) }
            ForgotUiEvent.Submit -> submit()
        }
    }

    /**
     * Validates email and connectivity, then invokes [sendPasswordResetUseCase].
     * @author udit
     */
    private fun submit() {
        val email = _state.value.email.trim()
        if (email.isEmpty()) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.ENTER_EMAIL) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OFFLINE_AUTH_RESET) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorMessage = null, message = null) }
            sendPasswordResetUseCase(email)
                .onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            message = TriggerStrings.Messages.RESET_EMAIL_SENT,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.OFFLINE_AUTH_RESET,
                                genericFallback = TriggerStrings.Errors.REQUEST_FAILED,
                            ),
                        )
                    }
                }
        }
    }
}
