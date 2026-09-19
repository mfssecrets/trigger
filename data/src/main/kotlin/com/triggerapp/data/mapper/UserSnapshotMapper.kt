package com.triggerapp.data.mapper

import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.domain.model.FaceVerification
import com.triggerapp.domain.model.User
import com.google.firebase.database.DataSnapshot

/**
 * Builds a [User] when `id` is present; fills missing optional strings with defaults.
 *
 * @receiver User node under `Users`.
 * @return [User] or `null` if `id` is missing.
 * @author udit
 */
internal fun DataSnapshot.toUserOrNull(): User? {
    val id = child(TriggerStrings.Db.CHILD_ID).getValue(String::class.java) ?: return null
    return User(
        id = id,
        username = child(TriggerStrings.Db.CHILD_USERNAME).getValue(String::class.java).orEmpty(),
        emailId = child(TriggerStrings.Db.CHILD_EMAIL_ID).getValue(String::class.java).orEmpty(),
        timestamp = child(TriggerStrings.Db.CHILD_TIMESTAMP).getValue(String::class.java).orEmpty(),
        imageUrl = child(TriggerStrings.Db.CHILD_IMAGE_URL).getValue(String::class.java)
            .takeUnless { it.isNullOrBlank() }
            ?: TriggerStrings.Defaults.PROFILE_IMAGE,
        bio = child(TriggerStrings.Db.CHILD_BIO).getValue(String::class.java).orEmpty(),
        status = child(TriggerStrings.Db.CHILD_STATUS).getValue(String::class.java).orEmpty(),
        searchKey = child(TriggerStrings.Db.CHILD_SEARCH).getValue(String::class.java).orEmpty(),
        lastSeen = child(TriggerStrings.Db.CHILD_LAST_SEEN).getValue(Long::class.java) ?: 0L,
        displayName = child(TriggerStrings.Db.CHILD_DISPLAY_NAME).getValue(String::class.java).orEmpty(),
        gender = child(TriggerStrings.Db.CHILD_GENDER).getValue(String::class.java).orEmpty(),
        dob = child(TriggerStrings.Db.CHILD_DOB).getValue(String::class.java).orEmpty(),
        verification = child(TriggerStrings.Db.NODE_VERIFICATION).toFaceVerificationOrNull(),
    )
}

/**
 * Maps the `verification` child node to a [FaceVerification], or null when absent/invalid.
 *
 * @receiver `Users/{uid}/verification` snapshot.
 * @author udit
 */
internal fun DataSnapshot.toFaceVerificationOrNull(): FaceVerification? {
    if (!exists()) return null
    val verified = child(TriggerStrings.Db.CHILD_FACE_VERIFIED).getValue(Boolean::class.java) ?: return null
    return FaceVerification(
        faceVerified = verified,
        gender = child(TriggerStrings.Db.CHILD_DETECTED_GENDER).getValue(String::class.java).orEmpty(),
        confidence = child(TriggerStrings.Db.CHILD_CONFIDENCE).getValue(Double::class.java) ?: 0.0,
        verifiedAt = child(TriggerStrings.Db.CHILD_VERIFIED_AT).getValue(Long::class.java) ?: 0L,
    )
}
