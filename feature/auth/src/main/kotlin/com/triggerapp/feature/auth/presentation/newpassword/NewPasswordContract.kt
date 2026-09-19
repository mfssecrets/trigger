package com.triggerapp.feature.auth.presentation.newpassword

/**
 * UI snapshot for the new-password screen (end of the OTP reset flow).
 *
 * @property password New password field.
 * @property confirm Confirmation field.
 * @property fieldErrors Per-field inline validation messages keyed by field id ("password"/"confirm").
 * @property loading True while the password change round-trip is in flight.
 * @property errorMessage Recoverable server error text (inline field errors empty).
 * @author udit
 */
data class NewPasswordUiState(
    val password: String = "",
    val confirm: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Submit allowed only when both fields have content and no round-trip is running.
     * @author udit
     */
    val canSubmit: Boolean get() = password.isNotBlank() && confirm.isNotBlank() && !loading
}

/**
 * User actions on the new-password screen (MVI events).
 * @author udit
 */
sealed interface NewPasswordUiEvent {
    /**
     * New password field changed.
     *
     * @property value New text.
     * @author udit
     */
    data class PasswordChanged(val value: String) : NewPasswordUiEvent

    /**
     * Confirm field changed.
     *
     * @property value New text.
     * @author udit
     */
    data class ConfirmChanged(val value: String) : NewPasswordUiEvent

    /**
     * User tapped "Update password".
     * @author udit
     */
    data object Submit : NewPasswordUiEvent
}

/**
 * One-shot navigation effects for the new-password screen (MVI effects).
 * @author udit
 */
sealed interface NewPasswordUiEffect {
    /**
     * Password changed and device signed in — go to the app shell.
     * @author udit
     */
    data object NavigateHome : NewPasswordUiEffect
}
