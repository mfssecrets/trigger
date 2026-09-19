package com.triggerapp.feature.home.ui.feed

/**
 * Data model for a comment on a feed post.
 * @author triggerapp
 */
data class FeedComment(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String = "",
    val text: String,
    val timestamp: String,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
)

/**
 * Production-ready data model representing a Trigger social feed post.
 * @author triggerapp
 */
data class FeedPost(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String = "",
    val isVerified: Boolean = false,
    val location: String? = null,
    val timeAgo: String,
    val content: String,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val initialLikes: Int,
    val likedBySummary: String = "",
    val likedByAvatars: List<String> = emptyList(),
    val comments: List<FeedComment> = emptyList(),
    val pulseScore: String = "98%",
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val likesCount: Int = initialLikes,
)

/**
 * Data model for story bubbles at the top of the feed.
 * @author triggerapp
 */
data class FeedStory(
    val id: String,
    val username: String,
    val avatarUrl: String = "",
    val isOwnStory: Boolean = false,
    val hasUnseenStory: Boolean = true,
    val storyImageUrl: String = "",
    val storyCaption: String = "",
)
