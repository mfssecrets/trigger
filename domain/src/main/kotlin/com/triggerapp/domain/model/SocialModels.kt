package com.triggerapp.domain.model

import com.triggerapp.core.strings.TriggerStrings

/**
 * A feed post stored under `Posts/{postId}`.
 *
 * Author fields are denormalized at creation time so the feed renders without extra reads;
 * `isVerified` mirrors the author's face-verification badge at publish time.
 *
 * @property id Post key.
 * @property authorId Author uid.
 * @property authorName Author display name at publish time.
 * @property authorUsername Author handle at publish time.
 * @property authorAvatar Author avatar (HTTPS / data URI / `default`).
 * @property authorVerified Whether the author was face-verified when the post was created.
 * @property content Post text.
 * @property tags Hashtags without the leading `#` (stored as a comma-joined string).
 * @property imageUri Optional attached image (data URI) or null.
 * @property createdAt Epoch millis.
 * @property likesCount Denormalized like counter maintained by clients.
 * @property commentsCount Denormalized comment counter maintained by clients.
 * @author udit
 */
data class SocialPost(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String = "",
    val authorVerified: Boolean = false,
    val content: String,
    val tags: List<String> = emptyList(),
    val imageUri: String? = null,
    val createdAt: Long = 0L,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
)

/**
 * A comment under `PostComments/{postId}/{commentId}`.
 *
 * @property id Comment key.
 * @property authorId Commenter uid.
 * @property authorName Display name at comment time.
 * @property authorUsername Handle at comment time.
 * @property authorAvatar Avatar reference.
 * @property text Comment text.
 * @property createdAt Epoch millis.
 * @author udit
 */
data class SocialComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatar: String = "",
    val text: String,
    val createdAt: Long = 0L,
)

/**
 * An in-app notification stored under `Notifications/{recipientId}/{notificationId}`.
 *
 * @property id Notification key.
 * @property type One of [TriggerStrings.NotificationTypes] (follow / like / comment / system).
 * @property actorId Acting user uid (empty for system events).
 * @property actorName Acting user display name.
 * @property actorAvatar Acting user avatar reference.
 * @property postId Related post id, when the event is about a post.
 * @property text Human-readable one-line body.
 * @property createdAt Epoch millis.
 * @property read Whether the recipient has seen the row.
 * @author udit
 */
data class SocialNotification(
    val id: String,
    val type: String,
    val actorId: String = "",
    val actorName: String = "",
    val actorAvatar: String = "",
    val postId: String = "",
    val text: String,
    val createdAt: Long = 0L,
    val read: Boolean = false,
) {
    /** True when [type] is [TriggerStrings.NotificationTypes.FOLLOW]. */
    val isFollow: Boolean get() = type == TriggerStrings.NotificationTypes.FOLLOW

    /** True when [type] is [TriggerStrings.NotificationTypes.LIKE]. */
    val isLike: Boolean get() = type == TriggerStrings.NotificationTypes.LIKE

    /** True when [type] is [TriggerStrings.NotificationTypes.COMMENT]. */
    val isComment: Boolean get() = type == TriggerStrings.NotificationTypes.COMMENT

    /** True when [type] is [TriggerStrings.NotificationTypes.SYSTEM]. */
    val isSystem: Boolean get() = type == TriggerStrings.NotificationTypes.SYSTEM
}

/**
 * Real follow relationship snapshot for one peer as seen by the current user.
 *
 * @property isFollowing Whether the current user follows the peer.
 * @property peerFollowsMe Whether the peer follows the current user (drives the Follow Back label).
 * @property followersCount Live follower count of the peer.
 * @property followingCount Live following count of the peer.
 * @author udit
 */
data class FollowState(
    val isFollowing: Boolean = false,
    val peerFollowsMe: Boolean = false,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
) {
    /**
     * Resolved button label without the legacy fake `requested` state.
     * @author udit
     */
    val label: String
        get() = when {
            isFollowing -> "Following"
            peerFollowsMe -> "Follow Back"
            else -> "Follow"
        }
}
