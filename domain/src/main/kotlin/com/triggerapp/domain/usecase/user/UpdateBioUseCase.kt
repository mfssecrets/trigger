package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.repository.UserRepository
import com.triggerapp.domain.text.clampBio

/**
 * Application use case: update the current user’s bio.
 *
 * @author udit
 */
class UpdateBioUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param bio New bio text.
     * @author udit
     */
    suspend operator fun invoke(bio: String): Result<Unit> =
        userRepository.updateBio(bio.clampBio())
}
