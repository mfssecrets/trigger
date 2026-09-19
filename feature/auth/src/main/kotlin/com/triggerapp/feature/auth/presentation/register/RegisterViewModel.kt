package com.triggerapp.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.domain.usecase.auth.CheckUsernameAvailabilityUseCase
import com.triggerapp.domain.usecase.auth.SignUpUseCase
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.feature.auth.presentation.AuthFlowData
import com.triggerapp.domain.repository.OtpPurpose
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
 * MVI [ViewModel] for registration: [state], [onEvent], and [effects].
 * Validates the username handle (6–25 chars, letters/digits/underscore) with a debounced
 * backend availability check, validates the email shape, and forwards the flow to the
 * 6-digit email verification screen.
 *
 * @param checkUsernameAvailability Live registry lookup.
 * @param signUpUseCase Legacy direct signup (kept for fallback callers).
 * @param observeNetworkOnline Connectivity [kotlinx.coroutines.flow.StateFlow].
 * @author udit
 */
class RegisterViewModel(
    private val checkUsernameAvailability: CheckUsernameAvailabilityUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private val _effects = Channel<RegisterUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var usernameCheckJob: Job? = null

    /**
     * Reduces [RegisterUiEvent] into [RegisterUiState] or triggers submission.
     *
     * @param event User intent from the UI.
     * @author udit
     */
    fun onEvent(event: RegisterUiEvent) {
        when (event) {
            is RegisterUiEvent.UsernameChanged -> onUsernameChanged(event.value)
            is RegisterUiEvent.EmailChanged ->
                _state.update { it.copy(email = event.value, errorMessage = null) }
            is RegisterUiEvent.PasswordChanged ->
                _state.update { it.copy(password = event.value, errorMessage = null) }
            RegisterUiEvent.Submit -> submit()
            RegisterUiEvent.ClearForm -> {
                usernameCheckJob?.cancel()
                _state.update { RegisterUiState() }
            }
        }
    }

    /**
     * Filters the username to allowed characters, then debounces a backend availability check.
     *
     * @param raw Raw typed text.
     * @author udit
     */
    private fun onUsernameChanged(raw: String) {
        val filtered = raw.filter { it in 'a'..'z' || it in 'A'..'Z' || it.isDigit() || it == '_' }
            .take(DisplayTextLimits.MAX_USERNAME_CHARS)
        _state.update { it.copy(username = filtered, errorMessage = null) }

        usernameCheckJob?.cancel()
        when {
            filtered.isEmpty() ->
                _state.update { it.copy(usernameStatus = UsernameStatus.IDLE) }
            filtered.length < DisplayTextLimits.MIN_USERNAME_CHARS || !DisplayTextLimits.USERNAME_ALLOWED.matches(filtered) ->
                _state.update { it.copy(usernameStatus = UsernameStatus.INVALID) }
            else -> {
                _state.update { it.copy(usernameStatus = UsernameStatus.CHECKING) }
                usernameCheckJob = viewModelScope.launch {
                    delay(AVAILABILITY_DEBOUNCE_MS)
                    val result = runCatching { checkUsernameAvailability(filtered) }
                    // Only apply if the field hasn't changed while we were checking.
                    if (_state.value.username == filtered) {
                        _state.update {
                            it.copy(
                                usernameStatus = if (result.getOrDefault(false)) {
                                    UsernameStatus.AVAILABLE
                                } else {
                                    UsernameStatus.TAKEN
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates all fields, seeds the OTP flow data, and continues to the verification screen.
     * @author udit
     */
    private fun submit() {
        val s = _state.value
        val username = s.username
        val email = s.email.trim()
        val password = s.password
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.ALL_FIELDS_REQUIRED) }
            return
        }
        if (username.length < DisplayTextLimits.MIN_USERNAME_CHARS) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.USERNAME_TOO_SHORT) }
            return
        }
        if (!DisplayTextLimits.USERNAME_ALLOWED.matches(username)) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.USERNAME_INVALID_RULES) }
            return
        }
        if (!DisplayTextLimits.EMAIL_SHAPE.matches(email)) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.EMAIL_INVALID_FORMAT) }
            return
        }
        if (password.length < DisplayTextLimits.MIN_PASSWORD_CHARS) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.PASSWORD_MIN) }
            return
        }
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.OFFLINE_AUTH_SIGN_UP) }
            return
        }
        if (s.usernameStatus == UsernameStatus.TAKEN) {
            _state.update { it.copy(errorMessage = TriggerStrings.Errors.USERNAME_TAKEN) }
            return
        }
        if (s.usernameStatus != UsernameStatus.AVAILABLE) {
            // No fresh verdict yet — force an immediate check before moving on.
            _state.update { it.copy(loading = true, errorMessage = null) }
            viewModelScope.launch {
                val available = runCatching { checkUsernameAvailability(username) }
                    .getOrDefault(false)
                if (!_state.value.username.equals(username, ignoreCase = false)) {
                    _state.update { it.copy(loading = false) }
                    return@launch
                }
                if (!available) {
                    _state.update {
                        it.copy(
                            loading = false,
                            usernameStatus = UsernameStatus.TAKEN,
                            errorMessage = TriggerStrings.Errors.USERNAME_TAKEN,
                        )
                    }
                    return@launch
                }
                _state.update { it.copy(loading = false, usernameStatus = UsernameStatus.AVAILABLE) }
                continueToOtp(username, email, password)
            }
            return
        }
        continueToOtp(username, email, password)
    }

    /**
     * Seeds [AuthFlowData] and emits the navigate effect.
     *
     * @param username Validated handle.
     * @param email Validated email.
     * @param password Draft password.
     * @author udit
     */
    private fun continueToOtp(username: String, email: String, password: String) {
        AuthFlowData.startOtpFlow(email = email, purpose = OtpPurpose.SIGN_UP)
        AuthFlowData.signupUsername = username
        AuthFlowData.signupPassword = password
        viewModelScope.launch { _effects.send(RegisterUiEffect.NavigateOtp) }
    }
}

private const val AVAILABILITY_DEBOUNCE_MS = 350L
