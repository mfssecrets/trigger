package com.triggerapp.data.mapper

import com.triggerapp.core.strings.TriggerStrings
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
    )
}
