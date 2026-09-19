package com.triggerapp.feature.auth.presentation.otp

import com.triggerapp.domain.repository.OtpPurpose

/**
 * UI snapshot for the shared 6-digit OTP screen (signup + password reset).
 *
 * @property email Address the code was sent to.
 * @property purpose Flow the code belongs to.
 * @property code Entered digits so far (0–6).
 * @property sending True while a code email is being dispatched (initial send or resend).
 * @property verifying True while the code is being checked/used.
 * @property resendSecondsLeft Countdown until "Resend code" becomes clickable; `0` = enabled.
 * @property infoMessage Positive feedback (e.g. code sent).
 * @property errorMessage Recoverable error text.
 * @author udit
 */
data class OtpUiState(
    val email: String = "",
    val purpose: OtpPurpose = OtpPurpose.SIGN_UP,
    val code: String = "",
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val resendSecondsLeft: Int = 0,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
) {
    /**
     * True when a server round-trip is in progress (blocks inputs).
     * @author udit
     */
    val busy: Boolean get() = sending || verifying
}

/**
 * User actions on the OTP screen (MVI events).
 * @author udit
 */
sealed interface OtpUiEvent {
    /**
     * Code text changed (typing or paste); digits only, max 6.
     *
     * @property value New code text.
     * @author udit
     */
    data class CodeChanged(val value: String) : OtpUiEvent

    /**
     * User tapped "Resend code".
     * @author udit
     */
    data object Resend : OtpUiEvent

    /**
     * User tapped "Verify" (also fired automatically when the 6th digit is typed).
     * @author udit
     */
    data object Submit : OtpUiEvent
}

/**
 * One-shot navigation effects for the OTP screen (MVI effects).
 * @author udit
 */
sealed interface OtpUiEffect {
    /**
     * Signup code accepted and account created — go to the app shell.
     * @author udit
     */
    data object NavigateHome : OtpUiEffect

    /**
     * Reset code accepted — continue to the new-password screen.
     * @author udit
     */
    data object NavigateNewPassword : OtpUiEffect
}
