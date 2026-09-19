package com.triggerapp.feature.auth.presentation.forgot

import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.feature.auth.presentation.AuthFlowData

/**
 * UI snapshot while the user requests a password reset via 6-digit email code (MVI state).
 *
 * @property email User-entered email address.
 * @property loading True while input validation runs.
 * @property errorMessage Validation error text, if any.
 * @author udit
 */
data class ForgotUiState(
    val email: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * User-driven events for the forgot-password screen (MVI events).
 * @author udit
 */
sealed interface ForgotUiEvent {
    /**
     * Email field text changed.
     *
     * @property value New email text from the field.
     * @author udit
     */
    data class EmailChanged(val value: String) : ForgotUiEvent

    /**
     * User tapped submit to continue to the code screen.
     * @author udit
     */
    data object Submit : ForgotUiEvent
}

/**
 * Side effects for the forgot-password screen (MVI effects).
 * @author udit
 */
sealed interface ForgotUiEffect {
    /**
     * Email accepted — continue to the 6-digit code screen (code is emailed there).
     * @author udit
     */
    data object NavigateOtp : ForgotUiEffect
}

/**
 * Bridges the forgot-password form into the shared OTP flow via [AuthFlowData].
 * @author udit
 */
internal fun startResetFlow(email: String) {
    AuthFlowData.startOtpFlow(email = email, purpose = OtpPurpose.PASSWORD_RESET)
}
