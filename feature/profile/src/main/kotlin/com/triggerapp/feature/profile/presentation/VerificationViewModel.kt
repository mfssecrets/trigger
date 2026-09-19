package com.triggerapp.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triggerapp.core.common.errors.userFacingMessage
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.model.FaceVerification
import com.triggerapp.domain.usecase.connectivity.ObserveNetworkOnlineUseCase
import com.triggerapp.domain.usecase.user.ObserveCurrentUserUseCase
import com.triggerapp.domain.usecase.user.SaveFaceVerificationUseCase
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

/**
 * Face-verification [ViewModel]: reads the stored status, drives the liveness stage
 * machine fed by [com.triggerapp.feature.profile.verification.FaceLivenessAnalyzer],
 * classifies the captured face crop on-device with [GenderClassifier], and persists the
 * structured result through [SaveFaceVerificationUseCase]. The selfie bitmap itself is
 * never uploaded — only the boolean/label/confidence payload reaches the database.
 *
 * @param observeCurrentUser Supplies the current verification status.
 * @param saveFaceVerification Writes the verification result.
 * @param observeNetworkOnline Connectivity guard for the final save.
 * @param classifier On-device gender classifier (Koin factory scope; closed with the VM).
 * @author udit
 */
class VerificationViewModel(
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val saveFaceVerification: SaveFaceVerificationUseCase,
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
            _state.update { it.copy(stage = VerificationStage.SAVING) }
            val genderLabel = if (result.isFemale) "female" else "male"
            val saved = saveFaceVerification(genderLabel, result.confidence)
            saved.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            stage = VerificationStage.SUCCESS,
                            result = FaceVerification(
                                faceVerified = true,
                                gender = genderLabel,
                                confidence = result.confidence,
                                verifiedAt = System.currentTimeMillis(),
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            stage = VerificationStage.FAILED,
                            error = e.userFacingMessage(
                                offlineFallback = TriggerStrings.Errors.VERIFICATION_SAVE_FAILED,
                                genericFallback = TriggerStrings.Errors.VERIFICATION_SAVE_FAILED,
                            ),
                        )
                    }
                },
            )
        }
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
    }
}
