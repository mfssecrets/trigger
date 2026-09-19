package com.triggerapp.feature.profile.presentation

/**
 * UI snapshot for the full-screen Edit Profile screen.
 *
 *
 * @property initialLoading True until the current user has been loaded into the fields.
 * @property loadError User-visible error when the profile failed to load.
 * @property name Display name draft.
 * @property username Unique handle draft (without `@`).
 * @property bio Bio draft.
 * @property gender Selected gender label; empty when unset.
 * @property dobText Date-of-birth draft in `DD/MM/YYYY` as typed by the user.
 * @property ageText Computed age line shown under the DOB field; empty when DOB invalid/unset.
 * @property dobError Validation error for the DOB field; null when valid or empty.
 * @property usernameError Validation/save error for the username field; null when valid.
 * @property saving True while the save sequence is running (save button shows progress).
 * @property saveError Global save error shown as a snackbar; cleared on next save/dismiss.
 * @property saved One-shot flag: true right after a fully successful save (navigate back).
 * @property genderMenuExpanded Dropdown visibility for the gender selector.
 * @author udit
 */
data class EditProfileUiState(
    val initialLoading: Boolean = true,
    val loadError: String? = null,
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val gender: String = "",
    val dobText: String = "",
    val ageText: String = "",
    val dobError: String? = null,
    val usernameError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val genderMenuExpanded: Boolean = false,
) {
    /**
     * Whether the Save action is currently allowed (all fields valid and not already saving).
     * @author udit
     */
    val canSave: Boolean
        get() = !initialLoading && !saving && name.isNotBlank() && username.isNotBlank() && dobError == null
}

/**
 * Which dialog-free edit event arrived from the Edit Profile screen.
 * @author udit
 */
sealed interface EditProfileUiEvent {
    data class NameChanged(val value: String) : EditProfileUiEvent
    data class UsernameChanged(val value: String) : EditProfileUiEvent
    data class BioChanged(val value: String) : EditProfileUiEvent
    data class GenderSelected(val value: String) : EditProfileUiEvent
    data class GenderMenuToggled(val expanded: Boolean) : EditProfileUiEvent
    data class DobChanged(val value: String) : EditProfileUiEvent
    data object Save : EditProfileUiEvent
    data object SaveErrorShown : EditProfileUiEvent
    data object RetryLoad : EditProfileUiEvent
}
