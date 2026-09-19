package com.triggerapp.domain.repository

/**
 * Which server flow a 6-digit email code belongs to.
 * @author udit
 */
enum class OtpPurpose(val wire: String) {
    SIGN_UP("signup"),
    PASSWORD_RESET("reset"),
}

/**
 * Port for the server-backed email OTP flows (Cloud Functions) and the unique username registry.
 *
 * The server emails 6-digit codes, enforces expiry/attempts/cooldown, and (for signup / password
 * reset) returns a Firebase custom token that signs the device in without re-typing credentials.
 *
 * @author udit
 */
interface OtpRepository {

    /**
     * Emails a 6-digit verification code for [purpose]. Server enforces a resend cooldown.
     *
     * @param email Target inbox.
     * @param purpose Signup vs password reset.
     * @return [Result] success when the email was dispatched.
     * @author udit
     */
    suspend fun sendOtp(email: String, purpose: OtpPurpose): Result<Unit>

    /**
     * Checks a code without consuming it (wrong codes count toward the attempt limit).
     *
     * @param email Email the code was sent to.
     * @param purpose Flow the code belongs to.
     * @param code 6-digit code from the user.
     * @return [Result] success when the code is currently valid.
     * @author udit
     */
    suspend fun verifyOtp(email: String, purpose: OtpPurpose, code: String): Result<Unit>

    /**
     * Verifies the signup code, creates the Firebase Auth account and `Users/{uid}` profile,
     * claims the unique username, and signs the device in with a custom token.
     *
     * @param email Verified email.
     * @param username Unique username (handle) to claim.
     * @param password Account password.
     * @param code 6-digit email code.
     * @return [Result] success when the account exists and the device is signed in.
     * @author udit
     */
    suspend fun signUpWithOtp(
        email: String,
        username: String,
        password: String,
        code: String,
    ): Result<Unit>

    /**
     * Verifies the reset code and changes the account password, then signs the device in.
     *
     * @param email Account email.
     * @param code 6-digit email code.
     * @param newPassword Replacement password.
     * @return [Result] success when the password changed and the device is signed in.
     * @author udit
     */
    suspend fun resetPasswordWithOtp(
        email: String,
        code: String,
        newPassword: String,
    ): Result<Unit>

    /**
     * Live availability check against the `Usernames` registry (server-owned; clients read only).
     *
     * @param username Candidate handle.
     * @return true when the handle is free; throws when the registry can't be reached.
     * @author udit
     */
    suspend fun isUsernameAvailable(username: String): Boolean

    /**
     * Renames the signed-in user's unique handle via the server (admin write path).
     *
     * @param username New handle satisfying the username rules.
     * @return [Result] success when renamed and re-claimed.
     * @author udit
     */
    suspend fun changeUsername(username: String): Result<Unit>
}
