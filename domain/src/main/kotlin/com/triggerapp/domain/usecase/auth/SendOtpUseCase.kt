package com.triggerapp.domain.usecase.auth

import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.domain.repository.OtpRepository

/**
 * Emails a 6-digit verification code for the given flow (signup or password reset).
 * @author udit
 */
class SendOtpUseCase(private val otpRepository: OtpRepository) {
    /**
     * @param email Target inbox.
     * @param purpose Signup vs password reset.
     * @author udit
     */
    suspend operator fun invoke(email: String, purpose: OtpPurpose): Result<Unit> =
        otpRepository.sendOtp(email.trim().lowercase(), purpose)
}
