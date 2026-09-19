package com.triggerapp.feature.auth.presentation

import com.triggerapp.domain.repository.OtpPurpose

/**
 * Memory-only transient state passed between the multi-step auth flows (register → OTP → home,
 * forgot → OTP → new password). Never persisted; cleared as soon as a flow completes so
 * credentials don't linger. On process death the fields reset to blank and the OTP screen
 * asks the user to start over.
 *
 * @author udit
 */
object AuthFlowData {

    /** Email currently being verified (both flows). */
    var email: String = ""

    /** Which OTP flow is active. */
    var purpose: OtpPurpose = OtpPurpose.SIGN_UP

    /** Signup-only draft credentials, used by the verification call. */
    var signupUsername: String = ""
    var signupPassword: String = ""

    /** Reset-only: code verified on the OTP screen, consumed by the new-password call. */
    var resetCode: String = ""

    /**
     * Seeds the OTP flow for [purpose].
     *
     * @param email Email that will receive the code.
     * @param purpose Signup vs password reset.
     * @author udit
     */
    fun startOtpFlow(email: String, purpose: OtpPurpose) {
        this.email = email.trim().lowercase()
        this.purpose = purpose
        resetCode = ""
    }

    /**
     * Clears signup draft data (after account creation or flow abort).
     * @author udit
     */
    fun clearSignup() {
        signupUsername = ""
        signupPassword = ""
    }

    /**
     * Clears reset draft data (after password change or flow abort).
     * @author udit
     */
    fun clearReset() {
        resetCode = ""
    }

    /**
     * True when no email is pending — e.g. after process death on the OTP screen.
     * @author udit
     */
    fun isBlank(): Boolean = email.isBlank()
}
