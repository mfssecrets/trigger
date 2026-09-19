package com.triggerapp.domain.usecase.social

import com.triggerapp.domain.model.SocialComment
import com.triggerapp.domain.model.SocialPost
import com.triggerapp.domain.repository.SocialRepository
import kotlinx.coroutines.flow.Flow

/**
 * Observes the newest feed posts.
 * @author udit
 */
class ObserveFeedUseCase(private val repo: SocialRepository) {
    /**
     * @param limit Max posts to pull from the backend.
     * @return Hot flow of posts.
     * @author udit
     */
    operator fun invoke(limit: Long): Flow<List<SocialPost>> = repo.observeFeed(limit)
}

/**
 * Publishes a new post as the signed-in user.
 * @author udit
 */
class CreatePostUseCase(private val repo: SocialRepository) {
    /**
     * @param content Post text.
     * @param tags Hashtags without `#`.
     * @param jpegBytes Optional attached image bytes (compressed JPEG).
     * @return Result with the new post id.
     * @author udit
     */
    suspend operator fun invoke(content: String, tags: List<String>, jpegBytes: ByteArray?): Result<String> =
        repo.createPost(content, tags, jpegBytes)
}

/**
 * Likes / unlikes a post.
 * @author udit
 */
class SetPostLikedUseCase(private val repo: SocialRepository) {
    /**
     * @param postId Post key.
     * @param liked True to like.
     * @author udit
     */
    suspend operator fun invoke(postId: String, liked: Boolean): Result<Unit> =
        repo.setPostLiked(postId, liked)
}

/**
 * Observes post ids liked by the signed-in user.
 * @author udit
 */
class ObserveLikedPostIdsUseCase(private val repo: SocialRepository) {
    /**
     * @return Hot flow of liked post ids.
     * @author udit
     */
    operator fun invoke(): Flow<Set<String>> = repo.observeLikedPostIds()
}

/**
 * Observes post ids saved by the signed-in user.
 * @author udit
 */
class ObserveSavedPostIdsUseCase(private val repo: SocialRepository) {
    /**
     * @return Hot flow of saved post ids.
     * @author udit
     */
    operator fun invoke(): Flow<Set<String>> = repo.observeSavedPostIds()
}

/**
 * Saves / unsaves a post.
 * @author udit
 */
class SetPostSavedUseCase(private val repo: SocialRepository) {
    /**
     * @param postId Post key.
     * @param saved True to save.
     * @author udit
     */
    suspend operator fun invoke(postId: String, saved: Boolean): Result<Unit> =
        repo.setPostSaved(postId, saved)
}

/**
 * Files a moderation report for a post.
 * @author udit
 */
class ReportPostUseCase(private val repo: SocialRepository) {
    /**
     * @param postId Reported post.
     * @param reason Free-text reason chosen in the UI.
     * @author udit
     */
    suspend operator fun invoke(postId: String, reason: String): Result<Unit> =
        repo.reportPost(postId, reason)
}

/**
 * Observes comments of one post.
 * @author udit
 */
class ObservePostCommentsUseCase(private val repo: SocialRepository) {
    /**
     * @param postId Post key.
     * @return Hot flow of comments ordered oldest → newest.
     * @author udit
     */
    operator fun invoke(postId: String): Flow<List<SocialComment>> = repo.observeComments(postId)
}

/**
 * Adds a comment to a post.
 * @author udit
 */
class AddPostCommentUseCase(private val repo: SocialRepository) {
    /**
     * @param postId Post key.
     * @param text Comment text.
     * @author udit
     */
    suspend operator fun invoke(postId: String, text: String): Result<Unit> =
        repo.addComment(postId, text)
}
