package com.triggerapp.feature.home.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fullscreen Instagram-style story viewer matching the UI design model with:
 * - Multi-segment story progress indicators
 * - Top header with avatar, author name, time, options and close
 * - Full-bleed media canvas with left/right tap navigation
 * - Bottom interaction bar: pill input for "Send message...", like heart button, send button
 *
 * @author triggerapp
 */
@Composable
fun StoryViewerDialog(
    story: FeedStory,
    onDismiss: () -> Unit,
    onSendMessage: (text: String) -> Unit = {},
) {
    val totalSegments = 3
    var activeSegmentIndex by remember { mutableIntStateOf(0) }
    val progressAnimatable = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    var isLiked by remember { mutableStateOf(false) }
    var showFloatingHeart by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // Segment timer progression
    LaunchedEffect(story.id, activeSegmentIndex) {
        progressAnimatable.snapTo(0f)
        progressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4500, easing = LinearEasing),
        )
        if (activeSegmentIndex < totalSegments - 1) {
            activeSegmentIndex += 1
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            // Full-bleed Story Media
            if (story.storyImageUrl.isNotBlank()) {
                AsyncImage(
                    model = story.storyImageUrl,
                    contentDescription = "Story media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF141920), TriggerPurple.copy(alpha = 0.7f), Color.Black),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = story.storyCaption.ifBlank { "Trigger Story ✨" },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }

            // Left / Right tap zones for navigation between story segments
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (activeSegmentIndex > 0) {
                                        activeSegmentIndex -= 1
                                    }
                                },
                            )
                        },
                )
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (activeSegmentIndex < totalSegments - 1) {
                                        activeSegmentIndex += 1
                                    } else {
                                        onDismiss()
                                    }
                                },
                                onDoubleTap = {
                                    isLiked = true
                                    showFloatingHeart = true
                                    coroutineScope.launch {
                                        delay(800)
                                        showFloatingHeart = false
                                    }
                                },
                            )
                        },
                )
            }

            // Dark Top & Bottom Gradients for readable overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        ),
                    ),
            )

            // TOP SECTION: Segmented Progress Bar + Author Details + Close Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
            ) {
                // Multi-Segment Story Progress Bars (Instagram Model)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (i in 0 until totalSegments) {
                        val segmentProgress = when {
                            i < activeSegmentIndex -> 1f
                            i == activeSegmentIndex -> progressAnimatable.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Author Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TriggerProfileAvatar(
                        imageUrl = story.avatarUrl,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = story.username.lowercase(),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "2h",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.weight(1f))

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF222831)),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Story", color = Color(0xFFFF5252)) },
                                onClick = {
                                    showMenu = false
                                    onDismiss()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Mute Stories", color = Color.White) },
                                onClick = {
                                    showMenu = false
                                    onDismiss()
                                },
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Big Double-Tap Floating Heart Animation
            AnimatedVisibility(
                visible = showFloatingHeart,
                enter = scaleIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) + fadeIn(),
                exit = scaleOut(animationSpec = tween(300)) + fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF3040),
                    modifier = Modifier.size(90.dp),
                )
            }

            // Optional Story Caption overlay above interaction bar
            if (story.storyCaption.isNotBlank() && story.storyImageUrl.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 76.dp)
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = story.storyCaption,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            // BOTTOM INTERACTION BAR (Instagram UI Model: pill input, like heart, send button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Rounded pill input field with border: "Send message..." + "..."
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (messageText.isEmpty()) {
                                Text(
                                    text = "Send message...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                )
                            }
                            BasicTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // Subtle three dots button inside pill
                        Text(
                            text = "•••",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Heart like button with tactile toggle & pop
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        if (isLiked) {
                            showFloatingHeart = true
                            coroutineScope.launch {
                                delay(700)
                                showFloatingHeart = false
                            }
                        }
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like story",
                        tint = if (isLiked) Color(0xFFFF3040) else Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }

                // Send paper plane button
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (messageText.isNotBlank()) TriggerAccent else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
