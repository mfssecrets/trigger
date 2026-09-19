package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.AuthRepository

/**
 * Application use case: sign in with email and password.
 *
 * @author udit
 */
class SignInUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * @param email Account email.
     * @param password User password.
     * @author udit
     */
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        authRepository.signIn(email, password)
}
