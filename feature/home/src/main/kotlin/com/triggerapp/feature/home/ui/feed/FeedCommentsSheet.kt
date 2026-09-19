package com.triggerapp.feature.home.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.domain.model.SocialComment
import kotlinx.coroutines.launch

private val SheetBackground = Color(0xFF14191F)
private val CommentSurface = Color(0xFF1B222B)
private val TextMuted = Color(0xFFAFACAC)

/**
 * Bottom sheet for viewing and adding comments to a live feed post.
 * Comments stream in realtime from `PostComments/{postId}`; every submission is
 * written to the backend and bumps the post's comment counter.
 *
 * @param postId Post whose comments are shown (for keys).
 * @param commentsCount Live denormalized comment counter from the post row.
 * @param comments Live comment rows (oldest first).
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCommentsSheet(
    postId: String,
    commentsCount: Long,
    comments: List<SocialComment>,
    currentUserHandle: String,
    currentUserAvatarUrl: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var newCommentText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun submitComment() {
        val text = newCommentText.trim()
        if (text.isNotEmpty()) {
            onAddComment(text)
            newCommentText = ""
            scope.launch {
                if (comments.isNotEmpty()) {
                    listState.animateScrollToItem(comments.size - 1)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF384350)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Comments",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = TriggerAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "$commentsCount",
                            color = TriggerAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF232D38), thickness = 0.8.dp)

            // Comments List
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No comments yet",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Be the first to start the conversation!",
                            color = TextMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(comments, key = { it.id }) { comment ->
                        CommentItemRow(comment = comment)
                    }
                }
            }

            // Quick Reaction Emojis (append to the input, submitted explicitly)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf("❤️", "🔥", "⚡", "👏", "🚀", "✨", "💯").forEach { emoji ->
                    Surface(
                        color = Color(0xFF1E2630),
                        shape = CircleShape,
                        modifier = Modifier.clickable {
                            newCommentText += emoji
                        },
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            // Comment Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101419))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriggerProfileAvatar(
                    imageUrl = currentUserAvatarUrl,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                )
                Spacer(Modifier.width(10.dp))
                OutlinedTextField(
                    value = newCommentText,
                    onValueChange = { newCommentText = it },
                    placeholder = {
                        Text(
                            text = "Add a comment as @$currentUserHandle...",
                            color = TextMuted,
                            fontSize = 14.sp,
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = CommentSurface,
                        unfocusedContainerColor = CommentSurface,
                        focusedBorderColor = TriggerAccent.copy(alpha = 0.6f),
                        unfocusedBorderColor = Color(0xFF2A3644),
                        cursorColor = TriggerAccent,
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitComment() }),
                )
                Spacer(Modifier.width(8.dp))
                val canSend = newCommentText.trim().isNotEmpty()
                IconButton(
                    onClick = { submitComment() },
                    enabled = canSend,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canSend) TriggerAccent else Color(0xFF26323E)),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (canSend) Color.Black else TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: SocialComment,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TriggerProfileAvatar(
            imageUrl = comment.authorAvatar,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@${comment.authorUsername}",
                    color = TriggerAccent,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "• ${timeAgoLabel(comment.createdAt)}",
                    color = TextMuted,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = comment.text,
                color = Color(0xFFE0E3E8),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

/**
 * Formats an epoch-millis timestamp as a compact relative label ("Just now", "18m ago",
 * "3h ago", "5d ago", or a DD/MM date for older rows). Shared by feed surfaces.
 *
 * @param createdAt Epoch millis of the row.
 * @param now Reference clock (defaults to the device time; injectable for tests).
 * @return Human-readable relative time label.
 * @author udit
 */
fun timeAgoLabel(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    if (createdAt <= 0L) return "Just now"
    val diffSeconds = (now - createdAt) / 1000
    return when {
        diffSeconds < 60 -> "Just now"
        diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
        diffSeconds < 86_400 -> "${diffSeconds / 3600}h ago"
        diffSeconds < 7 * 86_400 -> "${diffSeconds / 86_400}d ago"
        else -> {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = createdAt }
            val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val m = cal.get(java.util.Calendar.MONTH) + 1
            String.format(java.util.Locale.US, "%02d/%02d", d, m)
        }
    }
}
