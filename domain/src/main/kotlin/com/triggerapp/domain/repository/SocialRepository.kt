package com.triggerapp.domain.repository

import com.triggerapp.domain.model.SocialComment
import com.triggerapp.domain.model.SocialNotification
import com.triggerapp.domain.model.SocialPost
import kotlinx.coroutines.flow.Flow

/**
 * Port for the prototype social graph: posts, likes, comments, saves, reports,
 * follows, and in-app notifications — all backed by Realtime Database nodes.
 *
 * Nodes:
 * - `Posts/{postId}`
 * - `PostLikes/{postId}/{uid}` + mirror `UserLikes/{uid}/{postId}`
 * - `PostComments/{postId}/{commentId}`
 * - `Followers/{userId}/{followerId}` / `Following/{userId}/{targetId}`
 * - `Notifications/{recipientId}/{notificationId}`
 * - `SavedPosts/{uid}/{postId}` / `Reports/{reportId}`
 *
 * @author udit
 */
interface SocialRepository {

    // ---- Feed ----

    /**
     * Observes the newest feed posts (client-side newest-first order).
     *
     * @param limit Max rows pulled from `Posts` (ordered by `createdAt`).
     * @return Hot flow of posts.
     * @author udit
     */
    fun observeFeed(limit: Long): Flow<List<SocialPost>>

    /**
     * Publishes a post as the signed-in user. When [jpegBytes] is non-null the image is
     * compressed into a data URI (same strategy as avatars; RTDB-only storage).
     *
     * @param content Post text.
     * @param tags Hashtag list (without `#`).
     * @param jpegBytes Optional compressed image bytes to attach.
     * @return [Result] with the new post id.
     * @author udit
     */
    suspend fun createPost(content: String, tags: List<String>, jpegBytes: ByteArray?): Result<String>

    /**
     * Likes / unlikes a post: flips `PostLikes/{postId}/{uid}` + `UserLikes/{uid}/{postId}`
     * and applies a delta transaction to `Posts/{postId}/likesCount`.
     *
     * @param postId Target post.
     * @param like True to like, false to unlike.
     * @return [Result] success when written.
     * @author udit
     */
    suspend fun setPostLiked(postId: String, like: Boolean): Result<Unit>

    /**
     * Observes the ids of all posts the signed-in user has liked.
     * @author udit
     */
    fun observeLikedPostIds(): Flow<Set<String>>

    /**
     * Observes the ids of all posts the signed-in user has saved.
     * @author udit
     */
    fun observeSavedPostIds(): Flow<Set<String>>

    /**
     * Saves / unsaves a post under `SavedPosts/{uid}/{postId}`.
     * @author udit
     */
    suspend fun setPostSaved(postId: String, saved: Boolean): Result<Unit>

    /**
     * Files a report row under `Reports/{reportId}` (comment moderation queue).
     * @author udit
     */
    suspend fun reportPost(postId: String, reason: String): Result<Unit>

    // ---- Comments ----

    /**
     * Observes comments of one post ordered oldest → newest.
     * @author udit
     */
    fun observeComments(postId: String): Flow<List<SocialComment>>

    /**
     * Adds a comment as the signed-in user, bumps the post comment counter,
     * and (when someone else authored the post) pushes a notification to the author.
     *
     * @return [Result] success when written.
     * @author udit
     */
    suspend fun addComment(postId: String, text: String): Result<Unit>

    // ---- Follows ----

    /**
     * Observes whether the signed-in user follows [peerId].
     * @author udit
     */
    fun observeIsFollowing(peerId: String): Flow<Boolean>

    /**
     * Observes whether [peerId] follows the signed-in user (Follow Back state).
     * @author udit
     */
    fun observePeerFollowsMe(peerId: String): Flow<Boolean>

    /**
     * Observes the live follower-ids list of [userId].
     * @author udit
     */
    fun observeFollowerIds(userId: String): Flow<List<String>>

    /**
     * Observes the live following-ids list of [userId].
     * @author udit
     */
    fun observeFollowingIds(userId: String): Flow<List<String>>

    /**
     * Follows / unfollows [peerId] as the signed-in user in one atomic multi-path update
     * (`Followers` + `Following`), and pushes a notification on follow.
     *
     * @return [Result] success when written.
     * @author udit
     */
    suspend fun setFollowing(peerId: String, follow: Boolean): Result<Unit>

    // ---- Notifications ----

    /**
     * Observes the signed-in user's notifications (newest first, capped).
     * @author udit
     */
    fun observeNotifications(): Flow<List<SocialNotification>>

    /**
     * Marks every notification of the signed-in user as read in one multi-path update.
     * @author udit
     */
    suspend fun markAllNotificationsRead(): Result<Unit>

    /**
     * Writes a notification row to [recipientId] (used internally by like/comment/follow;
     * exposed for the verification Cloud Function result path).
     * @author udit
     */
    suspend fun pushNotification(
        recipientId: String,
        type: String,
        text: String,
        actorId: String = "",
        actorName: String = "",
        actorAvatar: String = "",
        postId: String = "",
    ): Result<Unit>
}
