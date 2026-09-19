package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.OtpRepository

/**
 * Changes the account password after the reset code is verified, then signs the device in.
 * @author udit
 */
class ResetPasswordWithOtpUseCase(private val otpRepository: OtpRepository) {
    /**
     * @param email Account email.
     * @param code 6-digit email code.
     * @param newPassword Replacement password.
     * @author udit
     */
    suspend operator fun invoke(email: String, code: String, newPassword: String): Result<Unit> =
        otpRepository.resetPasswordWithOtp(email.trim().lowercase(), code, newPassword)
}
