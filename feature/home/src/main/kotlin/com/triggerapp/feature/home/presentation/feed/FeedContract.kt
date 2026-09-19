package com.triggerapp.feature.home.presentation.feed

import com.triggerapp.domain.model.SocialComment
import com.triggerapp.domain.model.SocialPost

/**
 * UI state for the RTDB-backed feed tab (MVI).
 *
 * @property posts Newest-first feed rows from `Posts`.
 * @property likedPostIds Post ids liked by the current user.
 * @property savedPostIds Post ids saved by the current user.
 * @property hiddenPostIds Session-local hidden post ids ("Not interested" action).
 * @property isLoading True until the first feed snapshot arrives.
 * @property isPublishing True while a new post is being written to RTDB.
 * @property loadError Recoverable feed stream error.
 * @property actionError One-shot action failure message (like / comment / publish).
 * @property selectedCommentPostId Post whose comments sheet is open.
 * @property comments Live comments of [selectedCommentPostId].
 * @author udit
 */
data class FeedUiState(
    val posts: List<SocialPost> = emptyList(),
    val likedPostIds: Set<String> = emptySet(),
    val savedPostIds: Set<String> = emptySet(),
    val hiddenPostIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val isPublishing: Boolean = false,
    val loadError: String? = null,
    val actionError: String? = null,
    val selectedCommentPostId: String? = null,
    val comments: List<SocialComment> = emptyList(),
) {
    /**
     * Posts with session-hidden rows removed (keeps "Not interested" honest and local).
     * @author udit
     */
    val visiblePosts: List<SocialPost>
        get() = posts.filter { it.id !in hiddenPostIds }
}

/**
 * User intents for the feed tab.
 * @author udit
 */
sealed interface FeedUiEvent {
    /** Reload the feed after a stream error. */
    data object Retry : FeedUiEvent

    /** Like / unlike a post. */
    data class ToggleLike(val postId: String) : FeedUiEvent

    /** Save / unsave a post. */
    data class ToggleSave(val postId: String) : FeedUiEvent

    /** Hide a post for this session ("Not interested"). */
    data class HidePost(val postId: String) : FeedUiEvent

    /** File a moderation report for a post. */
    data class ReportPost(val postId: String, val reason: String) : FeedUiEvent

    /** Publish a new post (image Uri string, resolved + compressed by the ViewModel). */
    data class PublishPost(val content: String, val tags: List<String>, val imageUri: String?) : FeedUiEvent

    /** Open the comments sheet for a post. */
    data class OpenComments(val postId: String) : FeedUiEvent

    /** Close the comments sheet. */
    data object CloseComments : FeedUiEvent

    /** Add a comment to the open post. */
    data class AddComment(val postId: String, val text: String) : FeedUiEvent

    /** Clear the one-shot action error. */
    data object ConsumeActionError : FeedUiEvent
}

/**
 * One-shot side effects for the feed tab.
 * @author udit
 */
sealed interface FeedUiEffect {
    /** Post published successfully. */
    data object PostPublished : FeedUiEffect

    /** Report submitted. */
    data object ReportSubmitted : FeedUiEffect

    /** Reaction/comment posted. */
    data object CommentAdded : FeedUiEffect
}
