package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.AuthRepository

/**
 * Application use case: request a password-reset email.
 *
 * @author udit
 */
class SendPasswordResetUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * @param email Address to send the reset link to.
     * @author udit
     */
    suspend operator fun invoke(email: String): Result<Unit> =
        authRepository.sendPasswordReset(email)
}
