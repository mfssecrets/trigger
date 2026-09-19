package com.triggerapp.feature.chat.presentation

import com.triggerapp.domain.model.FollowState
import com.triggerapp.domain.model.User

/**
 * UI state for the peer profile screen (MVI).
 *
 * @property user Observed profile, or `null` while loading / missing.
 * @property loadError Recoverable stream error, or `null`.
 * @property follow Live follow relationship between the current user and [User.id]
 *   (real counts from the `Followers`/`Following` nodes).
 * @property followersUsers Resolved follower profiles for the sheet.
 * @property followingUsers Resolved following profiles for the sheet.
 * @property followBusy True while a follow/unfollow write is in flight.
 * @property followError One-shot follow action failure message.
 * @author udit
 */
data class PeerProfileUiState(
    val user: User? = null,
    val loadError: String? = null,
    val follow: FollowState = FollowState(),
    val followersUsers: List<User> = emptyList(),
    val followingUsers: List<User> = emptyList(),
    val followBusy: Boolean = false,
    val followError: String? = null,
)

/**
 * User intents for PeerProfileRoute.
 * @author udit
 */
sealed interface PeerProfileUiEvent {
    data object Retry : PeerProfileUiEvent

    /** Follow / unfollow the peer (toggle of [FollowState.isFollowing]). */
    data object ToggleFollow : PeerProfileUiEvent

    /** Clear the one-shot follow error. */
    data object ConsumeFollowError : PeerProfileUiEvent
}
