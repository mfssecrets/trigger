package com.triggerapp.data.repository.firebase

import android.util.Log
import com.triggerapp.core.strings.TriggerStrings
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

private const val AUTH_ERROR_LOG_TAG = "TriggerAuthError"

/**
 * Maps Firebase Auth failures to curated [TriggerStrings.Errors] lines only (never raw SDK text).
 *
 *
 * @param e Failure from Auth or related I/O.
 * @return String from [TriggerStrings.Errors] suitable for [com.triggerapp.core.common.errors.UiSafeMessageException].
 * @author udit
 */
internal fun mapFirebaseAuthThrowable(e: Throwable): String {
    Log.w(AUTH_ERROR_LOG_TAG, "Auth failure (mapping to catalog string for UI)", e)
    when (e) {
        is FirebaseAuthWeakPasswordException ->
            return TriggerStrings.Errors.WEAK_PASSWORD
        is FirebaseAuthUserCollisionException ->
            return TriggerStrings.Errors.EMAIL_ALREADY_IN_USE
        is FirebaseAuthInvalidUserException ->
            return TriggerStrings.Errors.INVALID_EMAIL_OR_PASSWORD
        is FirebaseAuthInvalidCredentialsException ->
            return TriggerStrings.Errors.INVALID_EMAIL_OR_PASSWORD
        is FirebaseAuthException -> {
            return when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> TriggerStrings.Errors.INVALID_EMAIL
                "ERROR_WRONG_PASSWORD",
                "ERROR_USER_NOT_FOUND",
                "ERROR_INVALID_CREDENTIAL",
                -> TriggerStrings.Errors.INVALID_EMAIL_OR_PASSWORD
                "ERROR_USER_DISABLED" -> TriggerStrings.Errors.USER_DISABLED
                "ERROR_TOO_MANY_REQUESTS" -> TriggerStrings.Errors.TOO_MANY_ATTEMPTS
                "ERROR_OPERATION_NOT_ALLOWED" -> TriggerStrings.Errors.OPERATION_NOT_ALLOWED
                "ERROR_NETWORK_REQUEST_FAILED" -> TriggerStrings.Errors.NETWORK_ERROR
                else -> TriggerStrings.Errors.GENERIC
            }
        }
        else -> {
            val blob = "${e.message.orEmpty()} ${e.cause?.message.orEmpty()}"
            return when {
                blob.contains("API key", ignoreCase = true) ||
                    blob.contains("api_key", ignoreCase = true) ->
                    TriggerStrings.Errors.FIREBASE_CONFIG_API_KEY
                blob.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                    TriggerStrings.Errors.FIREBASE_CONFIG_API_KEY
                else -> TriggerStrings.Errors.GENERIC
            }
        }
    }
}
