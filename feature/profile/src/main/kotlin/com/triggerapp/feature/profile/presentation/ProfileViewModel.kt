package com.triggerapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.UpdateBioUseCase
import com.triggerapp.domain.usecase.user.UpdateUsernameUseCase
import com.triggerapp.domain.usecase.user.UploadProfileImageUseCase
import com.triggerapp.domain.text.clampBio
import com.triggerapp.domain.text.clampUsername
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Handles profile stream, dialog edits, and photo uploads via domain use cases (MVI + Clean Architecture).
 *
 * @param observeCurrentUser Subscribes to the signed-in profile.
 * @param updateUsername Persists display name changes.
 * @param updateBio Persists bio changes.
 * @param uploadProfileImage Persists a new profile photo.
 * @param observeNetworkOnline Connectivity [kotlinx.coroutines.flow.StateFlow]; guards saves and uploads when offline.
 * @author udit
 */
class ProfileViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val updateUsername: UpdateUsernameUseCase,
    private val updateBio: UpdateBioUseCase,
    private val uploadProfileImage: UploadProfileImageUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        startObserveUser()
    }

    /**
     * Subscribes to the current user flow and maps errors to [ProfileUiState.profileLoadError].
     * @author udit
     */
    private fun startObserveUser() {
        observeJob?.cancel()
        observeJob = observeCurrentUser()
            .onEach { user ->
                _state.update {
                    it.copy(
                        user = user,
                        profileLoadError = null,
                        initialProfilePending = false,
                    )
                }
            }
            .catch { e ->
                _state.update {
                    it.copy(
                        user = null,
                        initialProfilePending = false,
                        profileLoadError = e.userFacingMessage(
                            offlineFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                            genericFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                        ),
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Reduces [ProfileUiEvent] into state changes, saves, or uploads.
     *
     *
     * @param event Incoming UI event.
     * @author udit
     */
    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.RetryLoadProfile -> {
                _state.update {
                    it.copy(profileLoadError = null, initialProfilePending = true)
                }
                startObserveUser()
            }
            ProfileUiEvent.OpenUsernameDialog -> _state.update {
                it.copy(
                    dialog = ProfileDialog.Username,
                    dialogText = it.user?.username.orEmpty().clampUsername(),
                    error = null,
                )
            }
            ProfileUiEvent.OpenBioDialog -> _state.update {
                it.copy(
                    dialog = ProfileDialog.Bio,
                    dialogText = it.user?.bio.orEmpty().clampBio(),
                    error = null,
                )
            }
            ProfileUiEvent.OpenPhotoViewer -> _state.update { it.copy(photoViewerVisible = true) }
            ProfileUiEvent.ClosePhotoViewer -> _state.update { it.copy(photoViewerVisible = false) }
            ProfileUiEvent.EditPhotoFromViewer -> _state.update {
                it.copy(photoViewerVisible = false, photoSourceSheetVisible = true)
            }
            ProfileUiEvent.OpenPhotoSourceSheet -> _state.update { it.copy(photoSourceSheetVisible = true) }
            ProfileUiEvent.ClosePhotoSourceSheet -> _state.update { it.copy(photoSourceSheetVisible = false) }
            ProfileUiEvent.DismissDialog -> _state.update {
                it.copy(dialog = ProfileDialog.None, dialogText = "", error = null)
            }
            is ProfileUiEvent.DialogTextChanged -> _state.update {
                val next = when (it.dialog) {
                    ProfileDialog.Bio -> event.value.clampBio()
                    ProfileDialog.Username -> event.value.clampUsername()
                    else -> event.value
                }
                it.copy(dialogText = next)
            }
            ProfileUiEvent.SaveDialog -> saveDialog()
            is ProfileUiEvent.SaveProfileName -> saveUsernameDirect(event.value)
            is ProfileUiEvent.SaveProfileBio -> saveBioDirect(event.value)
            is ProfileUiEvent.PhotoPicked -> uploadPhoto(event.jpegBytes)
        }
    }

    /**
     * Persists username or bio from the open dialog when online.
     * @author udit
     */
    private fun saveDialog() {
        val dialog = _state.value.dialog
        val raw = _state.value.dialogText.trim()
        val text = when (dialog) {
            ProfileDialog.Bio -> raw.clampBio()
            ProfileDialog.Username -> raw.clampUsername()
            else -> raw
        }
        when (dialog) {
            ProfileDialog.Bio -> saveBioDirect(text)
            ProfileDialog.Username -> saveUsernameDirect(text)
            ProfileDialog.None -> Unit
        }
    }

    /**
     * Persists the username through the server-enforced rename flow, without touching dialog state.
     *
     * @param text New username (already validated server-side; clamped here).
     * @author udit
     */
    private fun saveUsernameDirect(text: String) {
        val username = text.trim().clampUsername()
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(error = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            updateUsername(username)
                .onSuccess {
                    _state.update { it.copy(loading = false, error = null, dialog = ProfileDialog.None, dialogText = "") }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE,
                                genericFallback = TriggerStrings.Errors.UPDATE_FAILED,
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Persists the bio without touching dialog state.
     *
     * @param text New bio (clamped here and server-side).
     * @author udit
     */
    private fun saveBioDirect(text: String) {
        val bio = text.trim().clampBio()
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(error = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            updateBio(bio)
                .onSuccess {
                    _state.update { it.copy(loading = false, error = null, dialog = ProfileDialog.None, dialogText = "") }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.PROFILE_SAVE_OFFLINE,
                                genericFallback = TriggerStrings.Errors.UPDATE_FAILED,
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Uploads profile image bytes when online and toggles [ProfileUiState.photoUploading].
     *
     *
     * @param bytes JPEG payload after client-side compression.
     * @author udit
     */
    private fun uploadPhoto(bytes: ByteArray) {
        if (!observeNetworkOnline().value) {
            _state.update { it.copy(error = TriggerStrings.Errors.PROFILE_PHOTO_OFFLINE) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(photoUploading = true, error = null) }
            uploadProfileImage(bytes, TriggerStrings.Media.JPEG_EXTENSION).fold(
                onSuccess = { _state.update { it.copy(photoUploading = false, error = null) } },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            photoUploading = false,
                            error = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.PROFILE_PHOTO_OFFLINE,
                                genericFallback = TriggerStrings.Errors.UPLOAD_FAILED,
                            ),
                        )
                    }
                },
            )
        }
    }
}
