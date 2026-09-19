package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.AuthRepository

/**
 * Clears the local auth session.
 * @author udit
 */
class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    /**
     * Delegates to [AuthRepository.signOut].
     *
     * @author udit
     */
    operator fun invoke() {
        authRepository.signOut()
    }
}
