package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.domain.repository.OtpRepository

/**
 * Checks a 6-digit code without consuming it (attempt limits still apply).
 * @author udit
 */
class VerifyOtpUseCase(private val otpRepository: OtpRepository) {
    /**
     * @param email Email the code was sent to.
     * @param purpose Flow the code belongs to.
     * @param code User-entered 6-digit code.
     * @author udit
     */
    suspend operator fun invoke(email: String, purpose: OtpPurpose, code: String): Result<Unit> =
        otpRepository.verifyOtp(email.trim().lowercase(), purpose, code)
}
