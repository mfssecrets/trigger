package com.triggerapp.domain.usecase.presence

import com.triggerapp.domain.repository.UserRepository

/**
 * Writes the current user’s presence/status string (e.g. online / offline tokens).
 * @author udit
 */
class SetUserPresenceUseCase(
    private val userRepository: UserRepository,
) {
    /**
     *
     * @param status Value stored in the user profile for presence.
     * @author udit
     */
    suspend operator fun invoke(status: String) {
        userRepository.setPresence(status)
    }
}
