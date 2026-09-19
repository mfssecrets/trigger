package com.triggerapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.UploadProfileImageUseCase
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
 * Handles the profile stream and photo uploads via domain use cases (MVI + Clean Architecture).
 * Field editing (name, username, bio, gender, DOB) lives in [EditProfileViewModel].
 *
 * @param observeCurrentUser Subscribes to the signed-in profile.
 * @param uploadProfileImage Persists a new profile photo.
 * @param observeNetworkOnline Connectivity [kotlinx.coroutines.flow.StateFlow]; guards uploads when offline.
 * @author udit
 */
class ProfileViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
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
     * Reduces [ProfileUiEvent] into state changes or uploads.
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
            ProfileUiEvent.OpenPhotoViewer -> _state.update { it.copy(photoViewerVisible = true) }
            ProfileUiEvent.ClosePhotoViewer -> _state.update { it.copy(photoViewerVisible = false) }
            ProfileUiEvent.EditPhotoFromViewer -> _state.update {
                it.copy(photoViewerVisible = false, photoSourceSheetVisible = true)
            }
            ProfileUiEvent.OpenPhotoSourceSheet -> _state.update { it.copy(photoSourceSheetVisible = true) }
            ProfileUiEvent.ClosePhotoSourceSheet -> _state.update { it.copy(photoSourceSheetVisible = false) }
            is ProfileUiEvent.PhotoPicked -> uploadPhoto(event.jpegBytes)
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
