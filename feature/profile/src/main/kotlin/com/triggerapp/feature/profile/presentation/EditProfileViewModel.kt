package com.triggerapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.domain.text.clampBio
import com.triggerapp.domain.text.clampUsername
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.UpdateBioUseCase
import com.triggerapp.domain.usecase.user.UpdateProfileFieldsUseCase
import com.triggerapp.domain.usecase.user.UpdateUsernameUseCase
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Full-screen Edit Profile [ViewModel]: loads the signed-in profile into editable fields,
 * validates DOB (DD/MM/YYYY, computes age), and persists changes through the real backend —
 * username via the server-enforced rename function, the rest via one RTDB multi-path update.
 *
 * @param observeCurrentUser Subscribes to the signed-in profile for initial field values.
 * @param updateUsername Server-validated unique-handle rename.
 * @param updateBio Persists bio.
 * @param updateProfileFields Persists displayName / gender / dob in one update.
 * @param observeNetworkOnline Connectivity guard for saves.
 * @author udit
 */
class EditProfileViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val updateUsername: UpdateUsernameUseCase,
    private val updateBio: UpdateBioUseCase,
    private val updateProfileFields: UpdateProfileFieldsUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    private var originals: Originals? = null

    init {
        load()
    }

    /**
     * Immutable snapshot of the values the fields were initialized from; used to skip no-op writes.
     * @author udit
     */
    private data class Originals(
        val displayName: String,
        val username: String,
        val bio: String,
        val gender: String,
        val dobIso: String,
    )

    /**
     * Reduces [EditProfileUiEvent] into state changes or the save sequence.
     * @author udit
     */
    fun onEvent(event: EditProfileUiEvent) {
        when (event) {
            is EditProfileUiEvent.NameChanged -> _state.update {
                it.copy(name = event.value.clampUsername())
            }
            is EditProfileUiEvent.UsernameChanged -> _state.update {
                it.copy(
                    username = event.value.filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                        .take(DisplayTextLimits.MAX_USERNAME_CHARS),
                    usernameError = null,
                )
            }
            is EditProfileUiEvent.BioChanged -> _state.update {
                it.copy(bio = event.value.clampBio())
            }
            is EditProfileUiEvent.GenderSelected -> _state.update {
                it.copy(gender = event.value, genderMenuExpanded = false)
            }
            is EditProfileUiEvent.GenderMenuToggled -> _state.update {
                it.copy(genderMenuExpanded = event.expanded)
            }
            is EditProfileUiEvent.DobChanged -> _state.update {
                val digits = event.value.filter { ch -> ch.isDigit() }.take(8)
                val display = formatDobInput(digits)
                val iso = parseDdmYyyyToIsoOrNull(display)
                it.copy(
                    dobText = display,
                    dobError = if (digits.length == 8 && iso == null) {
                        TriggerStrings.Errors.INVALID_DOB
                    } else {
                        null
                    },
                    ageText = if (iso != null) computeAgeLine(iso) else "",
                )
            }
            EditProfileUiEvent.Save -> save()
            EditProfileUiEvent.SaveErrorShown -> _state.update { it.copy(saveError = null) }
            EditProfileUiEvent.RetryLoad -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(initialLoading = true, loadError = null) }
            try {
                val user = observeCurrentUser().filterNotNull().first()
                originals = Originals(
                    displayName = user.displayName,
                    username = user.username,
                    bio = user.bio,
                    gender = user.gender,
                    dobIso = user.dob,
                )
                _state.update {
                    it.copy(
                        initialLoading = false,
                        loadError = null,
                        name = user.displayName.ifBlank { user.username },
                        username = user.username,
                        bio = user.bio,
                        gender = user.gender,
                        dobText = formatIsoToDdmYyyy(user.dob),
                        ageText = computeAgeLine(user.dob),
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        initialLoading = false,
                        loadError = e.userFacingMessage(
                            offlineFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                            genericFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                        ),
                    )
                }
            }
        }
    }

    private fun save() {
        val snapshot = _state.value
        val original = originals ?: return
        if (snapshot.saving || snapshot.dobError != null) return
        val name = snapshot.name.trim()
        val username = snapshot.username.trim()
        val bio = snapshot.bio.trim().clampBio()
        if (name.isEmpty() || username.isEmpty()) return
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(saveError = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE) }
            return
        }
        val dobIso = parseDdmYyyyToIsoOrNull(snapshot.dobText) ?: ""

        viewModelScope.launch {
            _state.update { it.copy(saving = true, saveError = null, usernameError = null) }

            // 1. Username: server-enforced rename (validates handle + re-claims unique registry key).
            if (username != original.username) {
                val renamed = updateUsername(username.clampUsername())
                if (renamed.isFailure) {
                    val e = renamed.exceptionOrNull()
                    _state.update {
                        it.copy(
                            saving = false,
                            usernameError = e?.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE,
                                genericFallback = TriggerStrings.Errors.UPDATE_FAILED,
                            ),
                        )
                    }
                    return@launch
                }
            }

            // 2. Remaining fields in one multi-path write; bio has its own endpoint.
            val fields = buildMap {
                if (name != original.displayName.ifBlank { original.username }) {
                    put(TriggerStrings.Db.CHILD_DISPLAY_NAME, name.clampUsername())
                }
                if (snapshot.gender != original.gender) {
                    put(TriggerStrings.Db.CHILD_GENDER, snapshot.gender)
                }
                if (dobIso != original.dobIso) {
                    put(TriggerStrings.Db.CHILD_DOB, dobIso)
                }
            }
            var failed: String? = null
            if (fields.isNotEmpty()) {
                updateProfileFields(fields)
                    .onFailure { e ->
                        failed = e.userFacingMessage(
                            offlineFallback = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE,
                            genericFallback = TriggerStrings.Errors.UPDATE_FAILED,
                        )
                    }
            }
            if (failed == null && bio != original.bio) {
                updateBio(bio).onFailure { e ->
                    failed = e.userFacingMessage(
                        offlineFallback = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE,
                        genericFallback = TriggerStrings.Errors.UPDATE_FAILED,
                    )
                }
            }

            if (failed != null) {
                _state.update { it.copy(saving = false, saveError = failed) }
                return@launch
            }
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        private val GENDER_OPTIONS = listOf(
            TriggerStrings.Ui.GENDER_MALE,
            TriggerStrings.Ui.GENDER_FEMALE,
            TriggerStrings.Ui.GENDER_OTHER,
            TriggerStrings.Ui.GENDER_UNDISCLOSED,
        )

        /**
         * Allowed gender values for the dropdown (exposed for the UI).
         * @author udit
         */
        val genderOptions: List<String> = GENDER_OPTIONS

        /**
         * Groups raw digits into `DD/MM/YYYY` as the user types.
         *
         * @param digits Up to 8 digits.
         * @return Progressively formatted string (e.g. `2112`, `21/12/2`).
         * @author udit
         */
        fun formatDobInput(digits: String): String {
            val d = digits.take(8)
            return when {
                d.length <= 2 -> d
                d.length <= 4 -> d.substring(0, 2) + "/" + d.substring(2)
                else -> d.substring(0, 2) + "/" + d.substring(2, 4) + "/" + d.substring(4)
            }
        }

        /**
         * Parses `DD/MM/YYYY` into an ISO `yyyy-MM-dd` string, or null when invalid/incomplete.
         *
         * @param text User-entered date text.
         * @return ISO date or null.
         * @author udit
         */
        fun parseDdmYyyyToIsoOrNull(text: String): String? {
            val parts = text.trim().split("/")
            if (parts.size != 3) return null
            val (dd, mm, yyyy) = parts.map { it.trim() }
            if (dd.length != 2 || mm.length != 2 || yyyy.length != 4) return null
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
            val date = try {
                format.parse(text.trim())
            } catch (_: ParseException) {
                return null
            } ?: return null
            // Reject future dates (typos like 2050).
            if (date.after(Calendar.getInstance().time)) return null
            return String.format(Locale.US, "%s-%s-%s", yyyy, mm, dd)
        }

        /**
         * Converts ISO `yyyy-MM-dd` back to `DD/MM/YYYY` for display; empty stays empty.
         *
         * @param iso Stored ISO date string.
         * @return Display string or "".
         * @author udit
         */
        fun formatIsoToDdmYyyy(iso: String): String {
            if (iso.length != 10) return ""
            val parts = iso.split("-")
            if (parts.size != 3) return ""
            return "${parts[2]}/${parts[1]}/${parts[0]}"
        }

        /**
         * Builds the live age line under the DOB field (e.g. `Age: 21 years`).
         *
         * @param iso ISO date string or empty.
         * @return Age line, or "" when the date is unset/invalid.
         * @author udit
         */
        fun computeAgeLine(iso: String?): String {
            if (iso.isNullOrEmpty()) return ""
            val parsedIso = if (iso.contains("/")) parseDdmYyyyToIsoOrNull(iso) else iso
            parsedIso ?: return ""
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val date = try {
                format.parse(parsedIso)
            } catch (_: ParseException) {
                return ""
            } ?: return ""
            val dob = Calendar.getInstance().apply { time = date }
            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return if (age >= 0 && age < 150) "Age: $age years" else ""
        }
    }
}
