package com.triggerapp.feature.auth.presentation.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.feature.auth.presentation.AuthFlowData
import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.domain.usecase.auth.ResetPasswordWithOtpUseCase
import com.triggerapp.domain.usecase.auth.SendOtpUseCase
import com.triggerapp.domain.usecase.auth.SignUpWithOtpUseCase
import com.triggerapp.domain.usecase.auth.VerifyOtpUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI [ViewModel] for the shared 6-digit email OTP screen.
 *
 * On entry it automatically emails a code (unless the flow state was lost, e.g. after process
 * death), runs the 60s resend countdown, verifies codes, and for signup creates the account and
 * signs the device in; for reset it hands the verified code to the new-password screen.
 *
 * @author udit
 */
class OtpViewModel(
    private val sendOtp: SendOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase,
    private val signUpWithOtp: SignUpWithOtpUseCase,
    private val resetPasswordWithOtp: ResetPasswordWithOtpUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        OtpUiState(
            email = AuthFlowData.email,
            purpose = AuthFlowData.purpose,
        ),
    )
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private val _effects = Channel<OtpUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var countdownJob: Job? = null
    private var lastSubmittedCode: String? = null

    init {
        if (AuthFlowData.isBlank()) {
            _state.update {
                it.copy(errorMessage = TriggerStrings.Errors.GENERIC)
            }
        } else {
            sendCode(isInitial = true)
        }
    }

    /**
     * Reduces [OtpUiEvent] into [OtpUiState] or triggers verify/resend.
     *
     * @param event User intent from the UI.
     * @author udit
     */
    fun onEvent(event: OtpUiEvent) {
        when (event) {
            is OtpUiEvent.CodeChanged -> {
                val digits = event.value.filter { it.isDigit() }.take(CODE_LENGTH)
                _state.update { it.copy(code = digits, errorMessage = null, infoMessage = null) }
                // Auto-verify as soon as the 6th digit lands (also covers paste).
                if (digits.length == CODE_LENGTH && digits != lastSubmittedCode) {
                    submit()
                }
            }
            OtpUiEvent.Resend -> {
                val s = _state.value
                if (!s.busy && s.resendSecondsLeft <= 0) sendCode(isInitial = false)
            }
            OtpUiEvent.Submit -> submit()
        }
    }

    /**
     * Emails the code; the server enforces the 60s cooldown between sends.
     *
     * @param isInitial True for the automatic send on screen entry.
     * @author udit
     */
    private fun sendCode(isInitial: Boolean) {
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(sending = false, errorMessage = offlineMessage()) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sending = true, errorMessage = null, infoMessage = null) }
            sendOtp(_state.value.email, _state.value.purpose)
                .onSuccess {
                    lastSubmittedCode = null
                    _state.update { it.copy(sending = false, infoMessage = TriggerStrings.Messages.OTP_SENT) }
                    startResendCountdown()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            sending = false,
                            errorMessage = e.userFacingMessage(
                                offlineFallback = offlineMessage(),
                                genericFallback = TriggerStrings.Errors.OTP_SEND_FAILED,
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Verifies the entered code. Signup: creates the account + signs in. Reset: continues to the
     * new-password screen with the verified code.
     * @author udit
     */
    private fun submit() {
        val s = _state.value
        if (s.busy || AuthFlowData.isBlank()) return
        if (s.code.length < CODE_LENGTH) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OTP_INVALID) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = offlineMessage()) }
            return
        }
        lastSubmittedCode = s.code
        viewModelScope.launch {
            _state.update { it.copy(verifying = true, errorMessage = null, infoMessage = null) }
            when (_state.value.purpose) {
                OtpPurpose.SIGN_UP -> signUpWithOtp(
                    email = _state.value.email,
                    username = AuthFlowData.signupUsername,
                    password = AuthFlowData.signupPassword,
                    code = _state.value.code,
                )
                OtpPurpose.PASSWORD_RESET -> verifyOtp(
                    email = _state.value.email,
                    purpose = OtpPurpose.PASSWORD_RESET,
                    code = _state.value.code,
                )
            }
                .onSuccess {
                    _state.update { it.copy(verifying = false) }
                    when (_state.value.purpose) {
                        OtpPurpose.SIGN_UP -> {
                            AuthFlowData.clearSignup()
                            AuthFlowData.email = ""
                            _effects.send(OtpUiEffect.NavigateHome)
                        }
                        OtpPurpose.PASSWORD_RESET -> {
                            AuthFlowData.resetCode = _state.value.code
                            _effects.send(OtpUiEffect.NavigateNewPassword)
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            verifying = false,
                            code = "",
                            errorMessage = e.userFacingMessage(
                                offlineFallback = offlineMessage(),
                                genericFallback = TriggerStrings.Errors.OTP_INVALID,
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Runs the 60-second resend countdown.
     * @author udit
     */
    private fun startResendCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (left in RESEND_SECONDS downTo 1) {
                _state.update { it.copy(resendSecondsLeft = left) }
                delay(1_000)
            }
            _state.update { it.copy(resendSecondsLeft = 0) }
        }
    }

    private fun offlineMessage(): String =
        if (_state.value.purpose == OtpPurpose.SIGN_UP) {
            TriggerStrings.Errors.OFFLINE_AUTH_SIGN_UP
        } else {
            TriggerStrings.Errors.OFFLINE_AUTH_RESET
        }

    private companion object {
        const val CODE_LENGTH = 6
        const val RESEND_SECONDS = 60
    }
}
