package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.model.FaceVerification
import com.triggerapp.domain.repository.UserRepository

/**
 * Application use case: submit the liveness-passed face crop to the `verifyFace`
 * Cloud Function. The server runs Face++ face detection, and — only on success —
 * writes `Users/{uid}/verification` itself with admin privileges.
 *
 * Clients can no longer write the verification node (RTDB rules deny it), which
 * makes the badge tamper-proof: the only writer is the trusted backend.
 *
 * @author udit
 */
class VerifyFaceUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param imageBase64 Base64 (no wraps, no data-URI prefix) JPEG of the upright face crop.
     * @return [Result] with the server-written verification payload.
     * @author udit
     */
    suspend operator fun invoke(imageBase64: String): Result<FaceVerification> =
        userRepository.verifyFaceWithServer(imageBase64)
}
