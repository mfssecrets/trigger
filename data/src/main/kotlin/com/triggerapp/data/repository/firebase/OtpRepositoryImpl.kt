package com.triggerapp.data.repository.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import com.triggerapp.core.common.errors.UiSafeMessageException
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.repository.OtpPurpose
import com.triggerapp.domain.repository.OtpRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * [OtpRepository] backed by HTTPS-callable Cloud Functions (`functions/index.js`) plus a direct
 * read-only lookup on the server-owned `Usernames` registry for availability checks.
 * @author udit
 */
class OtpRepositoryImpl(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
    database: FirebaseDatabase,
) : OtpRepository {

    private val usernamesRef =
        database.reference.child(TriggerStrings.Db.NODE_USERNAMES)

    /**
     * Calls a Cloud Function and returns its payload map, mapping known server error tokens to
     * user-facing messages via [findFunctionsTokenMessage].
     *
     * @param name Callable name.
     * @param data Payload sent to the function.
     * @param fallback User-facing message when the error is not a known token.
     * @return Response payload as a map.
     * @author udit
     */
    private suspend fun callFunction(
        name: String,
        data: Map<String, Any?>,
        fallback: String = TriggerStrings.Errors.OTP_SEND_FAILED,
    ): Map<*, *> {
        val result = try {
            functions.getHttpsCallable(name).call(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw UiSafeMessageException(e.findFunctionsTokenMessage() ?: fallback).apply { initCause(e) }
        }
        @Suppress("UNCHECKED_CAST")
        return (result.data as? Map<*, *>).orEmpty()
    }

    /**
     * Signs the device in with a custom token returned by a verification function.
     *
     * @param token Server-minted custom token.
     * @author udit
     */
    private suspend fun signInWithCustomTokenOrThrow(token: String) {
        try {
            auth.signInWithCustomToken(token).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw UiSafeMessageException(mapFirebaseAuthThrowable(e)).apply { initCause(e) }
        }
    }

    /**
     * Emails a 6-digit code for [purpose]; the server enforces the resend cooldown.
     * @author udit
     */
    override suspend fun sendOtp(email: String, purpose: OtpPurpose): Result<Unit> = runCatching {
        callFunction(
            TriggerStrings.Functions.SEND_OTP,
            mapOf("email" to email, "purpose" to purpose.wire),
            fallback = TriggerStrings.Errors.OTP_SEND_FAILED,
        )
        Unit
    }

    /**
     * Checks a code without consuming it (wrong codes still count toward the attempt cap).
     * @author udit
     */
    override suspend fun verifyOtp(email: String, purpose: OtpPurpose, code: String): Result<Unit> =
        runCatching {
            callFunction(
                TriggerStrings.Functions.VERIFY_OTP,
                mapOf("email" to email, "purpose" to purpose.wire, "code" to code),
                fallback = TriggerStrings.Errors.OTP_INVALID,
            )
            Unit
        }

    /**
     * Verifies the signup code, creates account + profile + username claim server-side, then
     * signs the device in with the returned custom token.
     * @author udit
     */
    override suspend fun signUpWithOtp(
        email: String,
        username: String,
        password: String,
        code: String,
    ): Result<Unit> = runCatching {
        val payload = callFunction(
            TriggerStrings.Functions.SIGN_UP_WITH_OTP,
            mapOf(
                "email" to email,
                "username" to username,
                "password" to password,
                "code" to code,
            ),
            fallback = TriggerStrings.Errors.REGISTRATION_FAILED,
        )
        val token = payload["customToken"] as? String
            ?: error(TriggerStrings.Errors.NO_UID_AFTER_SIGNUP)
        signInWithCustomTokenOrThrow(token)
    }

    /**
     * Verifies the reset code, changes the password server-side, then signs the device in.
     * @author udit
     */
    override suspend fun resetPasswordWithOtp(
        email: String,
        code: String,
        newPassword: String,
    ): Result<Unit> = runCatching {
        val payload = callFunction(
            TriggerStrings.Functions.RESET_PASSWORD_WITH_OTP,
            mapOf("email" to email, "code" to code, "newPassword" to newPassword),
            fallback = TriggerStrings.Errors.OTP_INVALID,
        )
        val token = payload["customToken"] as? String
            ?: error(TriggerStrings.Errors.NOT_SIGNED_IN)
        signInWithCustomTokenOrThrow(token)
    }

    /**
     * Reads `Usernames/{lower}` — present means taken. Public-read rules make this work
     * pre-auth (e.g. on the registration screen).
     * @author udit
     */
    override suspend fun isUsernameAvailable(username: String): Boolean {
        val snapshot = try {
            usernamesRef.child(username.lowercase()).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw UiSafeMessageException(TriggerStrings.Errors.GENERIC).apply { initCause(e) }
        }
        return !snapshot.exists()
    }

    /**
     * Renames the signed-in user's handle through the server (registry claim + profile update).
     * @author udit
     */
    override suspend fun changeUsername(username: String): Result<Unit> = runCatching {
        callFunction(
            TriggerStrings.Functions.CHANGE_USERNAME,
            mapOf("username" to username),
            fallback = TriggerStrings.Errors.UPDATE_FAILED,
        )
        Unit
    }
}
