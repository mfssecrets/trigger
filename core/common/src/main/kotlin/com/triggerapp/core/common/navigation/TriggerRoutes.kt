package com.triggerapp.core.common.navigation

import com.triggerapp.core.strings.TriggerStrings

/**
 * Mirrors [TriggerStrings.Nav] with helpers that build concrete deep-link style paths.
 * @author udit
 */
object TriggerRoutes {
    const val SPLASH = TriggerStrings.Nav.SPLASH
    const val LOGIN = TriggerStrings.Nav.LOGIN
    const val REGISTER = TriggerStrings.Nav.REGISTER
    const val FORGOT = TriggerStrings.Nav.FORGOT
    const val OTP = TriggerStrings.Nav.OTP
    const val NEW_PASSWORD = TriggerStrings.Nav.NEW_PASSWORD
    const val HOME = TriggerStrings.Nav.HOME
    const val EDIT_PROFILE = TriggerStrings.Nav.EDIT_PROFILE
    const val CHAT_PATTERN = TriggerStrings.Nav.CHAT_PATTERN
    const val PEER_PROFILE_PATTERN = TriggerStrings.Nav.PEER_PROFILE_PATTERN

    /**
     * Builds the `chat/{peerId}` destination string for NavHost.
     *
     * @param peerId Firebase UID of the other user (path-safe).
     * @return Route string including the chat prefix and [peerId].
     * @author udit
     */
    fun chat(peerId: String): String =
        "${TriggerStrings.Nav.CHAT_PREFIX}/$peerId"

    /**
     * Builds the peer profile route for a given Firebase UID.
     *
     * @param userId Firebase UID of the profile to show (path-safe).
     * @return Route string including the peer-profile prefix and [userId].
     * @author udit
     */
    fun peerProfile(userId: String): String =
        "${TriggerStrings.Nav.PEER_PROFILE_PREFIX}/$userId"
}
