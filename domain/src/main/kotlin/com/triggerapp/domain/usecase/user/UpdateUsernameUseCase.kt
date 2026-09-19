package com.triggerapp.domain.usecase.user

import com.triggerapp.domain.repository.UserRepository
import com.triggerapp.domain.text.clampUsername

/**
 * Application use case: update the current user’s display name.
 *
 * @author udit
 */
class UpdateUsernameUseCase(
    private val userRepository: UserRepository,
) {
    /**
     * @param username New display name.
     * @author udit
     */
    suspend operator fun invoke(username: String): Result<Unit> =
        userRepository.updateUsername(username.clampUsername())
}
