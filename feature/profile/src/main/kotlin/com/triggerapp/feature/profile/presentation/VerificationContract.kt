package com.triggerapp.feature.profile.presentation

import android.graphics.Bitmap
import com.triggerapp.domain.model.FaceVerification
import com.triggerapp.feature.profile.verification.LivenessPhase

/**
 * High-level stage of the verification journey (drives which UI the screen shows).
 * @author udit
 */
enum class VerificationStage {
    /** Current verification status is being read. */
    LOADING_STATUS,

    /** Intro card with status + start / re-verify action. */
    INTRO,

    /** Camera is open and the liveness challenge is running. */
    LIVE,

    /** Gender classifier is running on the captured face crop. */
    ANALYZING,

    /** Result is being written to the backend. */
    SAVING,

    /** Verification finished successfully; badge is now live. */
    SUCCESS,

    /** Verification failed with a retryable message. */
    FAILED,
}

/**
 * UI snapshot for the face-verification screen.
 *
 * @property stage Current [VerificationStage].
 * @property livenessPhase Camera challenge step shown as the instruction line.
 * @property initialLoading True until the current profile has been observed once.
 * @property loadError Load failure message for the status read.
 * @property alreadyVerified True when the user already holds a face-verified result.
 * @property existing Existing verification (shown on intro/success).
 * @property multipleFacesWarning True while more than one face is visible in frame.
 * @property capturedPreview Face crop that was classified (thumbnail on success).
 * @property result Freshly produced verification (success state).
 * @property error User-visible failure message (failed state); null otherwise.
 * @author udit
 */
data class VerificationUiState(
    val stage: VerificationStage = VerificationStage.LOADING_STATUS,
    val livenessPhase: LivenessPhase = LivenessPhase.CENTER_FACE,
    val initialLoading: Boolean = true,
    val loadError: String? = null,
    val alreadyVerified: Boolean = false,
    val existing: FaceVerification? = null,
    val multipleFacesWarning: Boolean = false,
    val capturedPreview: Bitmap? = null,
    val result: FaceVerification? = null,
    val error: String? = null,
)

/**
 * Events emitted by the verification screen.
 * @author udit
 */
sealed interface VerificationUiEvent {
    /** Load/reload the signed-in profile status. */
    data object RetryLoad : VerificationUiEvent

    /** Start the camera liveness flow from the intro. */
    data object Start : VerificationUiEvent

    /** Analyzer reported a challenge-step transition. */
    data class LivenessPhaseChanged(val phase: LivenessPhase) : VerificationUiEvent

    /** Analyzer saw more than one face in the current frame. */
    data object MultipleFaces : VerificationUiEvent

    /** Liveness passed; analyzer produced the upright face crop for classification. */
    data class FaceCaptured(val crop: Bitmap) : VerificationUiEvent

    /** User asked to retry after a failure. */
    data object Retry : VerificationUiEvent

    /** Success screen dismissed (screen navigates back). */
    data object Done : VerificationUiEvent
}
