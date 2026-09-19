package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.OtpRepository

/**
 * Realtime username availability lookup against the server registry.
 * @author udit
 */
class CheckUsernameAvailabilityUseCase(private val otpRepository: OtpRepository) {
    /**
     * @param username Candidate handle (checked exactly as typed, lowercased server-side key).
     * @return true when free; throws when the registry can't be reached.
     * @author udit
     */
    suspend operator fun invoke(username: String): Boolean =
        otpRepository.isUsernameAvailable(username)
}
