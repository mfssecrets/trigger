package com.triggerapp.feature.auth.presentation.register

/**
 * Server-check state of the chosen username (handle).
 * @author udit
 */
enum class UsernameStatus {
    /** No input yet — show the format hint. */
    IDLE,

    /** Input invalid for server check (too short or bad characters). */
    INVALID,

    /** Debounced backend availability check in flight. */
    CHECKING,

    /** Backend registry says the handle is free — blue tick. */
    AVAILABLE,

    /** Backend registry says the handle is claimed. */
    TAKEN,
}

/**
 * UI snapshot during sign-up (MVI state).
 *
 * @property username Chosen username (unique handle) input.
 * @property email Account email input.
 * @property password Password input.
 * @property loading True while a validation round-trip is in progress.
 * @property errorMessage Validation or server error text, if any.
 * @property usernameStatus Live availability status for the username field.
 * @author udit
 */
data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val usernameStatus: UsernameStatus = UsernameStatus.IDLE,
)

/**
 * User actions on the registration screen (MVI events).
 * @author udit
 */
sealed interface RegisterUiEvent {
    /**
     * Username field changed.
     *
     * @property value New username text.
     * @author udit
     */
    data class UsernameChanged(val value: String) : RegisterUiEvent

    /**
     * Email field changed.
     *
     * @property value New email text.
     * @author udit
     */
    data class EmailChanged(val value: String) : RegisterUiEvent

    /**
     * Password field changed.
     *
     * @property value New password text.
     * @author udit
     */
    data class PasswordChanged(val value: String) : RegisterUiEvent

    /**
     * User submitted the registration form.
     * @author udit
     */
    data object Submit : RegisterUiEvent

    /**
     * Clears fields after the flow completes.
     * @author udit
     */
    data object ClearForm : RegisterUiEvent
}

/**
 * Side effects consumed by the UI layer (navigation, etc.).
 * @author udit
 */
sealed interface RegisterUiEffect {
    /**
     * Inputs accepted — continue to the 6-digit email verification screen.
     * @author udit
     */
    data object NavigateOtp : RegisterUiEffect
}
