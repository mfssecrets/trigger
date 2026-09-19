package com.triggerapp.domain.usecase.social

import com.triggerapp.domain.model.FollowState
import com.triggerapp.domain.model.SocialNotification
import com.triggerapp.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes whether the current user follows a peer (Follow / Following button state).
 * @author udit
 */
class ObserveIsFollowingUseCase(private val repo: SocialRepository) {
    /**
     * @param peerId Peer uid.
     * @return Hot flow of the follow flag.
     * @author udit
     */
    operator fun invoke(peerId: String): Flow<Boolean> = repo.observeIsFollowing(peerId)
}

/**
 * Observes whether a peer follows the current user (drives the Follow Back label).
 * @author udit
 */
class ObservePeerFollowsMeUseCase(private val repo: SocialRepository) {
    /**
     * @param peerId Peer uid.
     * @return Hot flow of the reverse-follow flag.
     * @author udit
     */
    operator fun invoke(peerId: String): Flow<Boolean> = repo.observePeerFollowsMe(peerId)
}

/**
 * Observes the live follower-ids list of a user.
 * @author udit
 */
class ObserveFollowerIdsUseCase(private val repo: SocialRepository) {
    /**
     * @param userId Owner of the follower list.
     * @return Hot flow of follower uids.
     * @author udit
     */
    operator fun invoke(userId: String): Flow<List<String>> = repo.observeFollowerIds(userId)
}

/**
 * Observes the live following-ids list of a user.
 * @author udit
 */
class ObserveFollowingIdsUseCase(private val repo: SocialRepository) {
    /**
     * @param userId Owner of the following list.
     * @return Hot flow of followed uids.
     * @author udit
     */
    operator fun invoke(userId: String): Flow<List<String>> = repo.observeFollowingIds(userId)
}

/**
 * Follows / unfollows a peer.
 * @author udit
 */
class SetFollowingUseCase(private val repo: SocialRepository) {
    /**
     * @param peerId Peer uid.
     * @param follow True to follow, false to unfollow.
     * @author udit
     */
    suspend operator fun invoke(peerId: String, follow: Boolean): Result<Unit> =
        repo.setFollowing(peerId, follow)
}

/**
 * Observes the signed-in user's in-app notifications (newest first).
 * @author udit
 */
class ObserveNotificationsUseCase(private val repo: SocialRepository) {
    /**
     * @return Hot flow of notifications.
     * @author udit
     */
    operator fun invoke(): Flow<List<SocialNotification>> = repo.observeNotifications()
}

/**
 * Marks all notifications of the signed-in user as read.
 * @author udit
 */
class MarkAllNotificationsReadUseCase(private val repo: SocialRepository) {
    /**
     * @return Result success when written.
     * @author udit
     */
    suspend operator fun invoke(): Result<Unit> = repo.markAllNotificationsRead()
}

/**
 * Aggregates the live follow snapshot for one peer into [FollowState].
 *
 * Combines `isFollowing`, `peerFollowsMe`, and live follower/following counts —
 * replacing the old hash-based fake counters.
 *
 * @author udit
 */
class ObserveFollowStateUseCase(
    private val observeIsFollowing: ObserveIsFollowingUseCase,
    private val observePeerFollowsMe: ObservePeerFollowsMeUseCase,
    private val observeFollowerIds: ObserveFollowerIdsUseCase,
    private val observeFollowingIds: ObserveFollowingIdsUseCase,
) {
    /**
     * @param peerId Peer uid.
     * @return Hot flow of the combined follow state.
     * @author udit
     */
    operator fun invoke(peerId: String): Flow<FollowState> =
        kotlinx.coroutines.flow.combine(
            observeIsFollowing(peerId),
            observePeerFollowsMe(peerId),
            observeFollowerIds(peerId),
            observeFollowingIds(peerId),
        ) { following, peerFollowsMe, followers, followingIds ->
            FollowState(
                isFollowing = following,
                peerFollowsMe = peerFollowsMe,
                followersCount = followers.size.toLong(),
                followingCount = followingIds.size.toLong(),
            )
        }
}
