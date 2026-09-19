package com.triggerapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import android.util.Base64
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.model.FaceVerification
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.VerifyFaceUseCase
import com.triggerapp.feature.profile.verification.GenderClassifier
import com.triggerapp.feature.profile.verification.LivenessPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Face-verification [ViewModel]: reads the stored status, drives the liveness stage
 * machine fed by [com.triggerapp.feature.profile.verification.FaceLivenessAnalyzer], and
 * runs a two-step check:
 *
 * 1. On-device gender pre-check with [GenderClassifier] — a free fail-fast gate so bad
 *    captures never reach (or spend) server quota.
 * 2. Server verification — the face crop is submitted to the `verifyFace` Cloud Function,
 *    which runs Face++ detection and writes `Users/{uid}/verification` itself. The
 *    client can no longer write that node (RTDB rules deny it), so the badge is
 *    tamper-proof and only the trusted backend can set it.
 *
 * @param observeCurrentUser Supplies the current verification status.
 * @param verifyFace Submits the face crop to the server.
 * @param observeNetworkOnline Connectivity guard for the server round-trip.
 * @param classifier On-device gender classifier (Koin factory scope; closed with the VM).
 * @author udit
 */
class VerificationViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val verifyFace: VerifyFaceUseCase,
    private val observeNetworkOnline: ObserveNetworkOnlineUseCase,
    private val classifier: GenderClassifier,
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationUiState())
    val state: StateFlow<VerificationUiState> = _state.asStateFlow()

    private var livenessTimeoutJob: Job? = null
    private var classifierClosed = false

    init {
        viewModelScope.launch {
            try {
                val user = observeCurrentUser().filterNotNull().first()
                _state.update {
                    it.copy(
                        initialLoading = false,
                        stage = VerificationStage.INTRO,
                        alreadyVerified = user.isFaceVerified,
                        existing = user.verification,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        initialLoading = false,
                        stage = VerificationStage.INTRO,
                        alreadyVerified = false,
                        existing = null,
                        loadError = e.userFacingMessage(
                            offlineFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                            genericFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Reduces [VerificationUiEvent] into state transitions and the classify/save sequence.
     * @author udit
     */
    fun onEvent(event: VerificationUiEvent) {
        when (event) {
            VerificationUiEvent.RetryLoad -> load()
            VerificationUiEvent.Start -> startLiveness()
            is VerificationUiEvent.LivenessPhaseChanged -> _state.update {
                it.copy(livenessPhase = event.phase, multipleFacesWarning = false)
            }
            VerificationUiEvent.MultipleFaces -> _state.update {
                it.copy(multipleFacesWarning = true)
            }
            is VerificationUiEvent.FaceCaptured -> classifyAndSave(event.crop)
            VerificationUiEvent.Retry -> startLiveness()
            VerificationUiEvent.Done -> Unit // screen pops; nothing to store
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(initialLoading = true, loadError = null) }
            try {
                val user = observeCurrentUser().filterNotNull().first()
                _state.update {
                    it.copy(
                        initialLoading = false,
                        stage = VerificationStage.INTRO,
                        alreadyVerified = user.isFaceVerified,
                        existing = user.verification,
                    )
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.update {
                    it.copy(
                        initialLoading = false,
                        stage = VerificationStage.INTRO,
                        loadError = e.userFacingMessage(
                            offlineFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                            genericFallback = TriggerStrings.Errors.LOAD_PROFILE_FAILED,
                        ),
                    )
                }
            }
        }
    }

    private fun startLiveness() {
        livenessTimeoutJob?.cancel()
        _state.update {
            it.copy(
                stage = VerificationStage.LIVE,
                livenessPhase = LivenessPhase.CENTER_FACE,
                error = null,
                result = null,
                capturedPreview = null,
                multipleFacesWarning = false,
            )
        }
        livenessTimeoutJob = viewModelScope.launch {
            delay(LIVENESS_TIMEOUT_MS)
            fail(TriggerStrings.Errors.VERIFICATION_FAILED)
        }
    }

    private fun classifyAndSave(crop: android.graphics.Bitmap) {
        livenessTimeoutJob?.cancel()
        val preview = crop
        _state.update { it.copy(stage = VerificationStage.ANALYZING, capturedPreview = preview) }
        viewModelScope.launch {
            // Step 1: free on-device pre-check — bad captures never reach the server.
            val result = withContext(Dispatchers.Default) {
                classifier.classify(crop)
            }
            if (classifierClosed) return@launch
            if (result == null || result.confidence < MIN_CONFIDENCE) {
                fail(TriggerStrings.Errors.VERIFICATION_FAILED)
                return@launch
            }
            if (!observeNetworkOnline().value) {
                fail(TriggerStrings.Errors.VERIFICATION_SAVE_FAILED)
                return@launch
            }
            // Step 2: server check — Face++ runs server-side; the function writes the badge.
            _state.update { it.copy(stage = VerificationStage.SAVING) }
            val base64 = withContext(Dispatchers.IO) { crop.toJpegBase64() }
            verifyFace(base64).fold(
                onSuccess = { verification ->
                    _state.update {
                        it.copy(
                            stage = VerificationStage.SUCCESS,
                            result = verification,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            stage = VerificationStage.FAILED,
                            error = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.VERIFICATION_SERVER_UNAVAILABLE,
                                genericFallback = TriggerStrings.Errors.VERIFICATION_SERVER_UNAVAILABLE,
                            ),
                        )
                    }
                },
            )
        }
    }

    /** Encodes the face crop as a compact base64 JPEG for the server round-trip. */
    private fun android.graphics.Bitmap.toJpegBase64(): String {
        val largest = maxOf(width, height)
        val toEncode = if (largest > UPLOAD_MAX_DIMENSION) {
            val scale = UPLOAD_MAX_DIMENSION.toFloat() / largest
            Bitmap.createScaledBitmap(
                this,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            this
        }
        val output = ByteArrayOutputStream()
        toEncode.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun fail(message: String) {
        livenessTimeoutJob?.cancel()
        _state.update { it.copy(stage = VerificationStage.FAILED, error = message) }
    }

    override fun onCleared() {
        livenessTimeoutJob?.cancel()
        classifierClosed = true
        classifier.close()
        super.onCleared()
    }

    private companion object {
        /** Below this softmax confidence the label is treated as inconclusive (retry). */
        const val MIN_CONFIDENCE = 0.85

        /** Whole liveness challenge must finish within this window. */
        const val LIVENESS_TIMEOUT_MS = 45_000L

        /** Longest edge uploaded to the server (keeps the payload small + fast). */
        const val UPLOAD_MAX_DIMENSION = 640

        /** JPEG quality for the uploaded crop. */
        const val UPLOAD_JPEG_QUALITY = 88
    }
}
