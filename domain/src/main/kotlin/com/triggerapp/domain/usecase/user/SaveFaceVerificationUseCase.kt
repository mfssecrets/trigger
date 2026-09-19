package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.repository.UserRepository

/**
 * Application use case: persist the on-device face-verification result for the
 * signed-in user under `Users/{uid}/verification`.
 *
 * @author udit
 */
class SaveFaceVerificationUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param gender Classifier label (`"male"` / `"female"`).
     * @param confidence Classifier confidence in `[0,1]`.
     * @return [Result] success when written.
     * @author udit
     */
    suspend operator fun invoke(gender: String, confidence: Double): Result<Unit> =
        userRepository.saveFaceVerification(gender, confidence)
}
