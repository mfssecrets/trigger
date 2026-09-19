package com.triggerapp.feature.profile.presentation

import com.triggerapp.domain.model.User

/**
 * UI snapshot for viewing the current profile and driving the photo flows.
 *
 *
 * @property user Loaded profile, or null while loading or on hard failure.
 * @property initialProfilePending True until the first value is received from the profile stream.
 * @property profileLoadError User-visible error when the profile stream fails (for example offline).
 * @property error Error shown on the profile screen (photo upload failures); cleared on next action.
 * @property photoUploading True while a new avatar upload is in progress.
 * @property photoViewerVisible Fullscreen avatar preview overlay.
 * @property photoSourceSheetVisible Bottom sheet for gallery vs camera.
 * @author udit
 */
data class ProfileUiState(
    val user: User? = null,
    val initialProfilePending: Boolean = true,
    val profileLoadError: String? = null,
    val error: String? = null,
    val photoUploading: Boolean = false,
    val photoViewerVisible: Boolean = false,
    val photoSourceSheetVisible: Boolean = false,
)

/**
 * User and system events for the profile screen.
 * @author udit
 */
sealed interface ProfileUiEvent {
    data object RetryLoadProfile : ProfileUiEvent
    data object OpenPhotoViewer : ProfileUiEvent
    data object ClosePhotoViewer : ProfileUiEvent
    data object EditPhotoFromViewer : ProfileUiEvent
    data object OpenPhotoSourceSheet : ProfileUiEvent
    data object ClosePhotoSourceSheet : ProfileUiEvent

    /**
     * JPEG bytes chosen after crop, ready for upload.
     *
     *
     * @property jpegBytes Compressed image payload.
     * @author udit
     */
    data class PhotoPicked(val jpegBytes: ByteArray) : ProfileUiEvent {
        /**
         * Compares [jpegBytes] by content for data class correctness.
         *
         *
         * @param other Another instance to compare.
         * @return True when byte arrays are equal.
         * @author udit
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PhotoPicked

            return jpegBytes.contentEquals(other.jpegBytes)
        }

        /**
         * Hash code derived from [jpegBytes] contents.
         *
         *
         * @return Content-based hash code.
         * @author udit
         */
        override fun hashCode(): Int {
            return jpegBytes.contentHashCode()
        }
    }
}
