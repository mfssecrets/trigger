package com.triggerapp.feature.home.ui.feed

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.domain.model.User
import com.triggerapp.feature.home.presentation.feed.FeedUiEffect
import com.triggerapp.feature.home.presentation.feed.FeedUiEvent
import com.triggerapp.feature.home.presentation.feed.FeedViewModel
import org.koin.androidx.compose.koinViewModel

private val HeaderBlack = Color(0xFF16191C)

/**
 * Live feeds screen for the Trigger application, backed entirely by Realtime Database:
 * - "What's triggering your mind?" composer that publishes real `Posts`
 * - Real post cards (like / comment / save / report / share all write to RTDB)
 * - Realtime comments sheet
 *
 * There are no seeded posts, no demo stories, and no local-only interactions —
 * an empty list simply means no posts exist yet.
 *
 * @param currentUser Signed-in profile (composer identity).
 * @param onOpenChat Navigates to a chat thread (reserved for "share via chat").
 * @param viewModel RTDB-backed feed ViewModel (Koin).
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedsTab(
    currentUser: User?,
    onOpenChat: (peerId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentUserName = currentUser?.effectiveDisplayName ?: "Trigger User"
    val currentUserHandle = currentUser?.username ?: "trigger_user"
    val currentUserAvatar = currentUser?.imageUrl.orEmpty()

    var showCreatePostDialog by remember { mutableStateOf(false) }

    // One-shot effects → snackbars
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                FeedUiEffect.PostPublished -> {
                    listState.animateScrollToItem(0)
                    snackbarHostState.showSnackbar("Post published to Feeds!")
                }
                FeedUiEffect.ReportSubmitted -> snackbarHostState.showSnackbar("Report submitted. Thank you for your feedback.")
                FeedUiEffect.CommentAdded -> snackbarHostState.showSnackbar("Comment posted!")
            }
        }
    }

    // Action failures → snackbar, then consume
    state.actionError?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(FeedUiEvent.ConsumeActionError)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TriggerScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.loadError != null -> FeedErrorState(
                message = state.loadError.orEmpty(),
                onRetry = { viewModel.onEvent(FeedUiEvent.Retry) },
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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

                if (state.isLoading && state.visiblePosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = TriggerAccent)
                        }
                    }
                } else if (state.visiblePosts.isEmpty()) {
                    item {
                        FeedEmptyState(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp))
                    }
                } else {
                    items(state.visiblePosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            FeedPostCard(
                                post = post,
                                isLiked = post.id in state.likedPostIds,
                                isSaved = post.id in state.savedPostIds,
                                timeAgo = timeAgoLabel(post.createdAt),
                                latestComment = null,
                                onLikeToggle = { postId ->
                                    viewModel.onEvent(FeedUiEvent.ToggleLike(postId))
                                },
                                onBookmarkToggle = { postId ->
                                    viewModel.onEvent(FeedUiEvent.ToggleSave(postId))
                                },
                                onOpenComments = { postId ->
                                    viewModel.onEvent(FeedUiEvent.OpenComments(postId))
                                },
                                onSharePost = { postId ->
                                    val target = state.posts.firstOrNull { it.id == postId }
                                    val shareText = buildString {
                                        append(target?.authorName?.plus(" posted on Trigger") ?: "Trigger post")
                                        append("\n\n")
                                        append(target?.content.orEmpty())
                                    }
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Share post"))
                                },
                                onQuickAddComment = { postId, emoji ->
                                    viewModel.onEvent(FeedUiEvent.AddComment(postId, emoji))
                                },
                                onOptionSelected = { action, postId ->
                                    when (action) {
                                        "not_interested" -> viewModel.onEvent(FeedUiEvent.HidePost(postId))
                                        "report" -> viewModel.onEvent(FeedUiEvent.ReportPost(postId, "Inappropriate content"))
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Comments Bottom Sheet (live comments from PostComments/{postId})
    state.selectedCommentPostId?.let { postId ->
        val post = state.posts.firstOrNull { it.id == postId }
        if (post != null) {
            FeedCommentsSheet(
                postId = postId,
                commentsCount = post.commentsCount,
                comments = state.comments,
                currentUserHandle = currentUserHandle,
                currentUserAvatarUrl = currentUserAvatar,
                onDismiss = { viewModel.onEvent(FeedUiEvent.CloseComments) },
                onAddComment = { text ->
                    viewModel.onEvent(FeedUiEvent.AddComment(postId, text))
                },
            )
        }
    }

    // Create Post Dialog → real RTDB write
    if (showCreatePostDialog) {
        CreatePostDialog(
            currentUserAvatarUrl = currentUserAvatar,
            currentUserName = currentUserName,
            currentUserHandle = currentUserHandle,
            onDismiss = { showCreatePostDialog = false },
            onPublishPost = { content, imageUri, tags ->
                showCreatePostDialog = false
                viewModel.onEvent(FeedUiEvent.PublishPost(content, tags, imageUri))
            },
        )
    }
}

@Composable
private fun FeedEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "No posts yet",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Be the first to trigger the conversation — publish a post above.",
            color = Color(0xFFAFACAC),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FeedErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = Color(0xFFAFACAC),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) {
            Text("Try again", color = TriggerAccent, fontWeight = FontWeight.SemiBold)
        }
    }
}
