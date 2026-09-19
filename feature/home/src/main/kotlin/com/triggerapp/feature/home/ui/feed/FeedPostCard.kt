package com.triggerapp.feature.home.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerPurple
import com.triggerapp.domain.model.SocialComment
import com.triggerapp.domain.model.SocialPost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CardBackground = Color(0xFF161B21)
private val CardBorderColor = Color(0xFF242E39)
private val TextMuted = Color(0xFFAFACAC)
private val TextSecondary = Color(0xFFC7CBD1)
private val LikeActiveColor = Color(0xFFFF4868)

/**
 * Live feed post card backed by a RTDB [SocialPost].
 * Features animated double-tap like, comments sheet launcher, bookmarking,
 * rich caption formatting, and an options menu whose actions hit the backend.
 *
 * @param post Live post row (author data denormalized at publish time).
 * @param isLiked Whether the current user likes this post.
 * @param isSaved Whether the current user saved this post.
 * @param timeAgo Pre-formatted relative timestamp label.
 * @param latestComment Most recent comment (preview row), or null.
 * @author triggerapp
 */
@Composable
fun FeedPostCard(
    post: SocialPost,
    isLiked: Boolean,
    isSaved: Boolean,
    timeAgo: String,
    latestComment: SocialComment?,
    onLikeToggle: (postId: String) -> Unit,
    onBookmarkToggle: (postId: String) -> Unit,
    onOpenComments: (postId: String) -> Unit,
    onSharePost: (postId: String) -> Unit,
    onQuickAddComment: (postId: String, text: String) -> Unit,
    onOptionSelected: (action: String, postId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var isExpandedCaption by remember { mutableStateOf(false) }
    var showHeartPopup by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    fun triggerDoubleTapLike() {
        if (!isLiked) {
            onLikeToggle(post.id)
        }
        coroutineScope.launch {
            showHeartPopup = true
            heartScale.snapTo(0.2f)
            heartScale.animateTo(
                targetValue = 1.3f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            )
            delay(400)
            heartScale.animateTo(0.9f, tween(150))
            showHeartPopup = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    // Avatar with Trigger gradient ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .border(
                                width = 1.8.dp,
                                brush = Brush.linearGradient(
                                    listOf(TriggerAccent, TriggerPurple, Color(0xFF63FFB0)),
                                ),
                                shape = CircleShape,
                            )
                            .padding(2.5.dp),
                    ) {
                        TriggerProfileAvatar(
                            imageUrl = post.authorAvatar,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (post.authorVerified) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Verified,
                                    contentDescription = "Verified",
                                    tint = TriggerAccent,
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "@${post.authorUsername}",
                                color = TriggerAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = " • $timeAgo",
                                color = TextMuted,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                // 3-dot Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Post options",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF1E2630)),
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isSaved) "Remove from Saved" else "Save Post", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onBookmarkToggle(post.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Share Post", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onSharePost(post.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Not Interested", color = Color(0xFFFFCC80)) },
                                onClick = {
                                    showMenu = false
                                    onOptionSelected("not_interested", post.id)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Report Post", color = Color(0xFFFF5252)) },
                                onClick = {
                                    showMenu = false
                                    onOptionSelected("report", post.id)
                                },
                            )
                        }
                    }
                }
            }

            // Post Media (with Double-Tap to Like Support)
            if (!post.imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .background(Color(0xFF0F1318))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { triggerDoubleTapLike() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    AsyncImage(
                        model = post.imageUri,
                        contentDescription = "Post image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    // Animated Heart Burst Overlay
                    if (showHeartPopup) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(heartScale.value)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = LikeActiveColor,
                                modifier = Modifier.size(56.dp),
                            )
                        }
                    }
                }
            }

            // Action Row: Like, Comment, Share, Bookmark
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onLikeToggle(post.id) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) LikeActiveColor else Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${post.likesCount}",
                            color = if (isLiked) LikeActiveColor else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Comment button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onOpenComments(post.id) }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${post.commentsCount}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Share button
                    IconButton(
                        onClick = { onSharePost(post.id) },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }

                // Bookmark / Save button
                IconButton(
                    onClick = { onBookmarkToggle(post.id) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Save post",
                        tint = if (isSaved) TriggerAccent else Color.White,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            // Real like counter line (live transaction-maintained count)
            if (post.likesCount > 0) {
                Text(
                    text = "Liked by ${post.likesCount} " + if (post.likesCount == 1L) "person" else "people",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
                )
            }

            // Caption Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                val captionText = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)) {
                        append("@${post.authorUsername}")
                    }
                    append("  ")
                    withStyle(SpanStyle(color = Color(0xFFE3E7ED), fontSize = 13.sp)) {
                        append(post.content)
                    }
                }

                Text(
                    text = captionText,
                    maxLines = if (isExpandedCaption) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp,
                    modifier = Modifier.clickable { isExpandedCaption = !isExpandedCaption },
                )

                if (post.content.length > 80 && !isExpandedCaption) {
                    Text(
                        text = "more",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { isExpandedCaption = true }
                            .padding(top = 2.dp),
                    )
                }

                // Distinctive hashtags in Trigger Mint Accent
                if (post.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        post.tags.forEach { tag ->
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                color = TriggerAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Latest comment preview & tap to open comments
            if (latestComment != null && post.commentsCount > 0) {
                Text(
                    text = "View all ${post.commentsCount} comments",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onOpenComments(post.id) }
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "@${latestComment.authorUsername}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = latestComment.text,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Quick Inline Emoji Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Add comment…",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenComments(post.id) },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("❤️", "🔥", "🚀").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clickable { onQuickAddComment(post.id, emoji) }
                                .padding(2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
