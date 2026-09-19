package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.OtpRepository

/**
 * Creates the account after the email code is verified and signs the device in.
 * @author udit
 */
class SignUpWithOtpUseCase(private val otpRepository: OtpRepository) {
    /**
     * @param email Verified email.
     * @param username Unique handle to claim.
     * @param password Account password.
     * @param code 6-digit email code.
     * @author udit
     */
    suspend operator fun invoke(
        email: String,
        username: String,
        password: String,
        code: String,
    ): Result<Unit> = otpRepository.signUpWithOtp(email.trim().lowercase(), username, password, code)
}
