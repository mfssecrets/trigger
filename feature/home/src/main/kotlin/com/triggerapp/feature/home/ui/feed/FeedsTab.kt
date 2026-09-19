package com.triggerapp.feature.home.ui.feed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerPurple
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.domain.model.User
import kotlinx.coroutines.launch

private val HeaderBlack = Color(0xFF16191C)
private val StoryRingGradient = Brush.linearGradient(
    listOf(TriggerAccent, TriggerPurple, Color(0xFF63FFB0)),
)

/**
 * Production-ready Feeds screen for the Trigger application.
 * Contains:
 * - Interactive stories bar with custom glowing story rings and full-screen viewer
 * - "What's triggering your mind?" post creation card
 * - Production-ready social post cards with like, comment bottom sheet, bookmark, and share
 *
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsTab(
    currentUser: User?,
    onOpenChat: (peerId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val currentUserName = currentUser?.username ?: "Trigger User"
    val currentUserHandle = currentUser?.emailId?.substringBefore('@')?.ifBlank {
        currentUserName.lowercase().replace(" ", "_")
    } ?: "trigger_dev"
    val currentUserAvatar = currentUser?.imageUrl.orEmpty()

    // Active dialog states
    var selectedCommentPostId by remember { mutableStateOf<String?>(null) }
    var viewingStory by remember { mutableStateOf<FeedStory?>(null) }
    var showCreatePostDialog by remember { mutableStateOf(false) }

    // Initial Stories
    val stories = remember {
        mutableStateListOf(
            FeedStory(
                id = "story_me",
                username = "Your Story",
                avatarUrl = currentUserAvatar,
                isOwnStory = true,
                hasUnseenStory = false,
                storyCaption = "Share an update today!",
            ),
            FeedStory(
                id = "story_1",
                username = "Jordan",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                hasUnseenStory = true,
                storyImageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800",
                storyCaption = "Midnight coding session for the next Trigger release 🚀",
            ),
            FeedStory(
                id = "story_2",
                username = "Alex River",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                hasUnseenStory = true,
                storyImageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800",
                storyCaption = "Cyberpunk workspace vibes ✨ Loving our new M3 color palette!",
            ),
            FeedStory(
                id = "story_3",
                username = "Cleopatra",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
                hasUnseenStory = true,
                storyImageUrl = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800",
                storyCaption = "UI architecture refactor: clean, snappy, modular.",
            ),
            FeedStory(
                id = "story_4",
                username = "Sam",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300",
                hasUnseenStory = true,
                storyImageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800",
                storyCaption = "Encrypted direct messaging is officially live in Trigger! 🔒",
            ),
        )
    }

    // Story picker for adding user's own story
    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val index = stories.indexOfFirst { it.isOwnStory }
            if (index != -1) {
                stories[index] = stories[index].copy(
                    storyImageUrl = uri.toString(),
                    storyCaption = "Added to your story ✨",
                    hasUnseenStory = true,
                )
            }
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Added to your story!")
            }
        }
    }

    // Production-ready Initial Feed Posts
    val posts = remember {
        mutableStateListOf(
            FeedPost(
                id = "post_1",
                authorName = "Jordan Hayes",
                authorHandle = "@jordan_dev",
                authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                isVerified = true,
                location = "Trigger HQ • San Francisco",
                timeAgo = "18m ago",
                content = "Trigger 2.0 architecture update: We just shipped zero-latency conversation streams, full Kotlin coroutine pipelines, and high-contrast M3 theme tuning! The responsive feel on Android is on another level.",
                tags = listOf("#triggerapp", "#androiddev", "#kotlin", "#jetpackcompose"),
                imageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=900",
                initialLikes = 148,
                pulseScore = "99%",
                likedBySummary = "Liked by alex_river, cleopatra and 146 others",
                likedByAvatars = listOf(
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                ),
                comments = listOf(
                    FeedComment(
                        id = "c1",
                        authorName = "Alex River",
                        authorHandle = "@alex_river",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                        text = "The dark theme contrast with the mint accent (#63FFA3) looks unbelievable! 🔥",
                        timestamp = "12m ago",
                        likesCount = 14,
                        isLiked = true,
                    ),
                    FeedComment(
                        id = "c2",
                        authorName = "Cleopatra",
                        authorHandle = "@cleopatra",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                        text = "Can confirm message delivery latency is under 15ms. In love with this release!",
                        timestamp = "5m ago",
                        likesCount = 8,
                    ),
                ),
            ),
            FeedPost(
                id = "post_2",
                authorName = "Alex River",
                authorHandle = "@alex_river",
                authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300",
                isVerified = false,
                location = "Berlin Tech Hub",
                timeAgo = "1h ago",
                content = "Crafting digital experiences with strict attention to edge-to-edge layouts and zero keyboard collision. Android 15 ready! Check out this setup screenshot from today's hackathon session.",
                tags = listOf("#mobile", "#designsystems", "#cyberpunk"),
                imageUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=900",
                initialLikes = 89,
                pulseScore = "95%",
                likedBySummary = "Liked by jordan_dev and 88 others",
                likedByAvatars = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                ),
                comments = listOf(
                    FeedComment(
                        id = "c3",
                        authorName = "Sam",
                        authorHandle = "@sam_coder",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                        text = "That monitor lighting setup is pristine! What keyboard is that?",
                        timestamp = "40m ago",
                        likesCount = 3,
                    ),
                ),
            ),
            FeedPost(
                id = "post_3",
                authorName = "Cleopatra Vance",
                authorHandle = "@cleopatra",
                authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300",
                isVerified = true,
                location = "Kyoto, Japan",
                timeAgo = "3h ago",
                content = "Design is not just what it looks like and feels like. Design is how it works. Trigger’s unified design language gives every screen a focused, high-contrast personality without visual clutter.",
                tags = listOf("#minimalism", "#uxdesign", "#trigger"),
                imageUrl = "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=900",
                initialLikes = 234,
                pulseScore = "98%",
                likedBySummary = "Liked by jordan_dev, alex_river and 232 others",
                likedByAvatars = listOf(
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                ),
                comments = listOf(
                    FeedComment(
                        id = "c4",
                        authorName = "Jordan Hayes",
                        authorHandle = "@jordan_dev",
                        authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                        text = "100% agreed. Purposeful spacing and typography beat arbitrary features every single time.",
                        timestamp = "2h ago",
                        likesCount = 18,
                    ),
                ),
            ),
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TriggerScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Top Stories Row
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeaderBlack)
                        .padding(vertical = 12.dp),
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(stories, key = { it.id }) { story ->
                            StoryItemCircle(
                                story = story,
                                onOpenStory = {
                                    if (story.isOwnStory) {
                                        storyPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    } else {
                                        viewingStory = story
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // "Create Post / What's on your mind" Composer Card
            item {
                Surface(
                    color = Color(0xFF161B21),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF242E39)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clickable { showCreatePostDialog = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TriggerProfileAvatar(
                            imageUrl = currentUserAvatar,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "What's triggering your mind?",
                            color = Color(0xFFAFACAC),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { showCreatePostDialog = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Attach media",
                                tint = TriggerAccent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Feed Posts
            items(posts, key = { it.id }) { post ->
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    FeedPostCard(
                        post = post,
                        currentUserId = currentUser?.id.orEmpty(),
                        onLikeToggle = { postId ->
                            val index = posts.indexOfFirst { it.id == postId }
                            if (index != -1) {
                                val item = posts[index]
                                val newLiked = !item.isLiked
                                val newCount = if (newLiked) item.likesCount + 1 else (item.likesCount - 1).coerceAtLeast(0)
                                posts[index] = item.copy(isLiked = newLiked, likesCount = newCount)
                            }
                        },
                        onBookmarkToggle = { postId ->
                            val index = posts.indexOfFirst { it.id == postId }
                            if (index != -1) {
                                val item = posts[index]
                                val newSaved = !item.isSaved
                                posts[index] = item.copy(isSaved = newSaved)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (newSaved) "Post saved to your collection" else "Post removed from saved",
                                    )
                                }
                            }
                        },
                        onOpenComments = { postId ->
                            selectedCommentPostId = postId
                        },
                        onSharePost = { postId ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Trigger Post", "https://triggerapp.com/post/$postId")
                            clipboard.setPrimaryClip(clip)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Post link copied to clipboard!")
                            }
                        },
                        onQuickAddComment = { postId, emoji ->
                            val index = posts.indexOfFirst { it.id == postId }
                            if (index != -1) {
                                val item = posts[index]
                                val newComment = FeedComment(
                                    id = "c_${System.currentTimeMillis()}",
                                    authorName = currentUserName,
                                    authorHandle = "@$currentUserHandle",
                                    authorAvatarUrl = currentUserAvatar,
                                    text = emoji,
                                    timestamp = "Just now",
                                )
                                val updatedComments = item.comments + newComment
                                posts[index] = item.copy(comments = updatedComments)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Reaction posted!")
                                }
                            }
                        },
                        onOptionSelected = { action, postId ->
                            when (action) {
                                "copy_link" -> {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Trigger Post", "https://triggerapp.com/post/$postId")
                                    clipboard.setPrimaryClip(clip)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Link copied to clipboard!")
                                    }
                                }
                                "not_interested" -> {
                                    posts.removeAll { it.id == postId }
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Post hidden from your feed")
                                    }
                                }
                                "report" -> {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Report submitted. Thank you for your feedback.")
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    // Interactive Comments Bottom Sheet
    selectedCommentPostId?.let { postId ->
        val post = posts.firstOrNull { it.id == postId }
        if (post != null) {
            FeedCommentsSheet(
                post = post,
                currentUserName = currentUserName,
                currentUserHandle = currentUserHandle,
                currentUserAvatarUrl = currentUserAvatar,
                onDismiss = { selectedCommentPostId = null },
                onAddComment = { commentText ->
                    val index = posts.indexOfFirst { it.id == postId }
                    if (index != -1) {
                        val item = posts[index]
                        val newComment = FeedComment(
                            id = "c_${System.currentTimeMillis()}",
                            authorName = currentUserName,
                            authorHandle = "@$currentUserHandle",
                            authorAvatarUrl = currentUserAvatar,
                            text = commentText,
                            timestamp = "Just now",
                        )
                        posts[index] = item.copy(comments = item.comments + newComment)
                    }
                },
                onLikeComment = { commentId ->
                    val postIndex = posts.indexOfFirst { it.id == postId }
                    if (postIndex != -1) {
                        val item = posts[postIndex]
                        val updatedComments = item.comments.map { c ->
                            if (c.id == commentId) {
                                val liked = !c.isLiked
                                val count = if (liked) c.likesCount + 1 else (c.likesCount - 1).coerceAtLeast(0)
                                c.copy(isLiked = liked, likesCount = count)
                            } else c
                        }
                        posts[postIndex] = item.copy(comments = updatedComments)
                    }
                },
            )
        }
    }

    // Story Viewer Dialog
    viewingStory?.let { story ->
        StoryViewerDialog(
            story = story,
            onDismiss = { viewingStory = null },
        )
    }

    // Create Post Dialog
    if (showCreatePostDialog) {
        CreatePostDialog(
            currentUserAvatarUrl = currentUserAvatar,
            currentUserName = currentUserName,
            currentUserHandle = currentUserHandle,
            onDismiss = { showCreatePostDialog = false },
            onPublishPost = { content, imageUri, tags ->
                val newPost = FeedPost(
                    id = "post_${System.currentTimeMillis()}",
                    authorName = currentUserName,
                    authorHandle = "@$currentUserHandle",
                    authorAvatarUrl = currentUserAvatar,
                    isVerified = true,
                    location = "Trigger App",
                    timeAgo = "Just now",
                    content = content,
                    tags = tags,
                    imageUrl = imageUri,
                    initialLikes = 0,
                    pulseScore = "100%",
                    likedBySummary = "",
                    likedByAvatars = emptyList(),
                    comments = emptyList(),
                )
                posts.add(0, newPost)
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                    snackbarHostState.showSnackbar("Post published to Feeds!")
                }
            },
        )
    }
}

/**
 * Story circle item with Trigger gradient glow ring or own story "+" indicator.
 */
@Composable
private fun StoryItemCircle(
    story: FeedStory,
    onOpenStory: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onOpenStory() }
            .width(66.dp),
    ) {
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (story.isOwnStory) {
                // User's own story circle with "+" badge
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .border(1.5.dp, Color(0xFF384350), CircleShape)
                        .padding(3.dp),
                ) {
                    TriggerProfileAvatar(
                        imageUrl = story.avatarUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(TriggerAccent)
                        .border(2.dp, HeaderBlack, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add story",
                        tint = Color.Black,
                        modifier = Modifier.size(13.dp),
                    )
                }
            } else {
                // Community story with glowing gradient ring
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .border(
                            width = if (story.hasUnseenStory) 2.dp else 1.dp,
                            brush = if (story.hasUnseenStory) StoryRingGradient else Brush.linearGradient(listOf(Color(0xFF384350), Color(0xFF384350))),
                            shape = CircleShape,
                        )
                        .padding(3.5.dp),
                ) {
                    TriggerProfileAvatar(
                        imageUrl = story.avatarUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                    )
                }
            }
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = story.username,
            color = if (story.isOwnStory) Color.White else Color(0xFFC7CBD1),
            fontSize = 11.sp,
            fontWeight = if (story.isOwnStory) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
