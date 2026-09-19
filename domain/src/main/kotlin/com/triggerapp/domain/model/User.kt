package com.triggerapp.domain.model

import com.triggerapp.core.strings.TriggerStrings

/**
 * Domain representation of a chat user profile stored under the Realtime Database `Users` node.
 *
 * @property id Firebase Auth UID / user key.
 * @property username Display name shown in the app.
 * @property emailId Account email (maybe empty in stored data).
 * @property timestamp Registration or last-profile-update marker from the backend schema.
 * @property imageUrl HTTPS URL, `data:image/...;base64,...` (RTDB avatar), or `"default"`.
 * @property bio Short user bio.
 * @property displayName Optional display name separate from the unique handle; blank falls back to [username].
 * @property gender Optional self-reported gender label; empty when unset.
 * @property dob Optional date of birth in ISO `yyyy-MM-dd`; empty when unset.
 * @property status Presence string: treated as online only when it equals [TriggerStrings.Defaults.PRESENCE_ONLINE] (case-insensitive) after trim.
 * @property searchKey Lowercase key used for prefix search in the database.
 * @property lastSeen Epoch millis of the last offline transition; `0` when never seen offline.
 * @author udit
 */
data class User(
    val id: String,
    val username: String,
    val emailId: String,
    val timestamp: String,
    val imageUrl: String,
    val bio: String,
    val status: String,
    val searchKey: String,
    val lastSeen: Long = 0L,
    val displayName: String = "",
    val gender: String = "",
    val dob: String = "",
) {
    /**
     * Name shown on profile surfaces: [displayName] when set, else the unique handle.
     * @author udit
     */
    val effectiveDisplayName: String
        get() = displayName.ifBlank { username }
    /**
     * Whether the user should be shown as online in the UI.
     * True only when [status] exactly matches the presence token (after trim), not substring match
     * (e.g. `"not online"` must not count as online).
     * @author udit
     */
    val isOnline: Boolean
        get() = status.trim().equals(TriggerStrings.Defaults.PRESENCE_ONLINE, ignoreCase = true)
}
