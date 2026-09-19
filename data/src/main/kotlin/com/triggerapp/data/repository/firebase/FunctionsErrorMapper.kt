package com.triggerapp.data.repository.firebase

import com.google.firebase.functions.FirebaseFunctionsException
import com.triggerapp.core.strings.TriggerStrings

/**
 * Stable error tokens thrown by the OTP/username Cloud Functions (see functions/index.js ERR map).
 * @author udit
 */
private object FunctionErrorTokens {
    const val EMAIL_INVALID = "EMAIL_INVALID"
    const val EMAIL_IN_USE = "EMAIL_IN_USE"
    const val EMAIL_NOT_FOUND = "EMAIL_NOT_FOUND"
    const val OTP_INVALID = "OTP_INVALID"
    const val OTP_EXPIRED = "OTP_EXPIRED"
    const val OTP_TOO_MANY_ATTEMPTS = "OTP_TOO_MANY_ATTEMPTS"
    const val OTP_COOLDOWN = "OTP_COOLDOWN"
    const val USERNAME_TAKEN = "USERNAME_TAKEN"
    const val USERNAME_INVALID = "USERNAME_INVALID"
    const val WEAK_PASSWORD = "WEAK_PASSWORD"
    const val OTP_SEND_FAILED = "OTP_SEND_FAILED"
    const val SERVICE_NOT_CONFIGURED = "EMAIL_SERVICE_NOT_CONFIGURED"
    const val NOT_SIGNED_IN = "NOT_SIGNED_IN"
    const val USERNAME_UNCHANGED = "USERNAME_UNCHANGED"
}

/**
 * Walks the throwable chain looking for a [FirebaseFunctionsException] raised by the
 * OTP/username functions and maps its stable token to a user-facing message.
 *
 * @receiver Any throwable thrown from a callable round-trip.
 * @return Mapped user-facing string, or `null` when this is not a known function error.
 * @author udit
 */
fun Throwable.findFunctionsTokenMessage(): String? {
    var t: Throwable? = this
    val seen = mutableSetOf<Throwable>()
    while (t != null && t !in seen) {
        seen.add(t)
        if (t is FirebaseFunctionsException) {
            return when (t.message) {
                FunctionErrorTokens.EMAIL_INVALID -> TriggerStrings.Errors.EMAIL_INVALID_FORMAT
                FunctionErrorTokens.EMAIL_IN_USE -> TriggerStrings.Errors.OTP_EMAIL_IN_USE
                FunctionErrorTokens.EMAIL_NOT_FOUND -> TriggerStrings.Errors.OTP_EMAIL_NOT_FOUND
                FunctionErrorTokens.OTP_INVALID -> TriggerStrings.Errors.OTP_INVALID
                FunctionErrorTokens.OTP_EXPIRED -> TriggerStrings.Errors.OTP_EXPIRED
                FunctionErrorTokens.OTP_TOO_MANY_ATTEMPTS -> TriggerStrings.Errors.OTP_TOO_MANY_ATTEMPTS
                FunctionErrorTokens.OTP_COOLDOWN -> TriggerStrings.Errors.OTP_COOLDOWN
                FunctionErrorTokens.USERNAME_TAKEN -> TriggerStrings.Errors.USERNAME_TAKEN
                FunctionErrorTokens.USERNAME_INVALID -> TriggerStrings.Errors.USERNAME_INVALID_RULES
                FunctionErrorTokens.WEAK_PASSWORD -> TriggerStrings.Errors.PASSWORD_MIN
                FunctionErrorTokens.OTP_SEND_FAILED -> TriggerStrings.Errors.OTP_SEND_FAILED
                FunctionErrorTokens.SERVICE_NOT_CONFIGURED -> TriggerStrings.Errors.OTP_SERVICE_NOT_CONFIGURED
                FunctionErrorTokens.NOT_SIGNED_IN -> TriggerStrings.Errors.NOT_SIGNED_IN
                FunctionErrorTokens.USERNAME_UNCHANGED -> TriggerStrings.Errors.USERNAME_UNCHANGED
                else -> null
            }
        }
        t = t.cause
    }
    return null
}
