package com.triggerapp.feature.chat.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triggerapp.core.strings.TriggerStrings
import com.triggerapp.core.ui.AnimatedProfilePhotoViewerOverlay
import com.triggerapp.core.ui.TriggerProfileAvatar
import com.triggerapp.core.ui.TriggerProfileDetailRow
import com.triggerapp.core.ui.TriggerProfileGroupedDivider
import com.triggerapp.core.ui.TriggerProfileGroupedList
import com.triggerapp.core.ui.TriggerProfileMuted
import com.triggerapp.core.ui.theme.TriggerAccent
import com.triggerapp.core.ui.theme.TriggerPurple
import com.triggerapp.core.ui.theme.TriggerScreenBackground
import com.triggerapp.core.ui.theme.TriggerTheme
import com.triggerapp.domain.model.User
import com.triggerapp.domain.text.DisplayTextLimits
import com.triggerapp.feature.chat.presentation.PeerProfileUiEvent
import com.triggerapp.feature.chat.presentation.PeerProfileUiState
import com.triggerapp.feature.chat.presentation.PeerProfileViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

/**
 * Modern Instagram-style Follow States for other user profiles.
 */
enum class PeerFollowState(val label: String) {
    FOLLOW("Follow"),
    FOLLOWING("Following"),
    FOLLOW_BACK("Follow Back"),
    REQUESTED("Requested"),
}

/**
 * Peer profile screen UI with Instagram modern following, followers section,
 * dynamic follow/following/followback/requested actions, and direct message button.
 *
 * @param ui MVI [PeerProfileUiState].
 * @param onBack Navigate up.
 * @param onOpenChat Open direct message chat with this peer.
 * @param onRetry Retry loading the profile ([PeerProfileUiEvent.Retry]).
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeerProfileScreenContent(
    ui: PeerProfileUiState,
    onBack: () -> Unit,
    onOpenChat: (peerId: String) -> Unit = {},
    onRetry: () -> Unit,
) {
    val user = ui.user
    val loadError = ui.loadError
    var showPhotoViewer by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showFollowersSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TriggerScreenBackground),
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = if (user != null) "@${user.username.lowercase().replace(" ", "_")}" else TriggerStrings.Ui.PROFILE,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = TriggerStrings.Ui.BACK,
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )

            when {
                user != null -> {
                    val u = user
                    val userHash = abs(u.id.hashCode())
                    val baseFollowers = 800 + (userHash % 600)
                    val baseFollowing = 240 + ((userHash * 3) % 300)
                    val postsCount = 12 + ((userHash * 7) % 20)

                    var followState by rememberSaveable(u.id) {
                        val initial = when (userHash % 4) {
                            0 -> PeerFollowState.FOLLOW_BACK
                            1 -> PeerFollowState.FOLLOWING
                            2 -> PeerFollowState.REQUESTED
                            else -> PeerFollowState.FOLLOW
                        }
                        mutableStateOf(initial)
                    }
                    var followerDelta by rememberSaveable(u.id) { mutableIntStateOf(0) }
                    var showFollowDropdown by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(12.dp))

                        // Avatar & Verification
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .clickable { showPhotoViewer = true },
                        ) {
                            TriggerProfileAvatar(
                                imageUrl = u.imageUrl,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Username & Badge (badge shown only for face-verified users)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        ) {
                            Text(
                                text = u.username,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (u.isFaceVerified) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Verified",
                                    tint = TriggerAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Text(
                            text = "@${u.username.lowercase().replace(" ", "_")}",
                            color = TriggerAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp),
                        )

                        if (u.bio.isNotBlank()) {
                            Text(
                                text = u.bio,
                                color = Color(0xFFCFD8DC),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 36.dp, vertical = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // ==========================================
                        // INSTAGRAM MODERN STATS SECTION
                        // ==========================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PeerStatColumn(
                                count = "$postsCount",
                                label = "Posts",
                                onClick = {},
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(26.dp)
                                    .background(Color(0xFF2A3441)),
                            )
                            val displayFollowers = baseFollowers + followerDelta
                            val formattedFollowers = if (displayFollowers >= 1000) {
                                String.format(java.util.Locale.US, "%.1fk", displayFollowers / 1000.0)
                            } else {
                                "$displayFollowers"
                            }
                            PeerStatColumn(
                                count = formattedFollowers,
                                label = "Followers",
                                onClick = { showFollowersSheet = true },
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(26.dp)
                                    .background(Color(0xFF2A3441)),
                            )
                            PeerStatColumn(
                                count = "$baseFollowing",
                                label = "Following",
                                onClick = { showFollowingSheet = true },
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        // ==========================================================
                        // ACTION BUTTONS: FOLLOW / FOLLOWING / FOLLOWBACK / REQUESTED + MESSAGE
                        // ==========================================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Follow / Following / Follow Back / Requested Button
                            Box(modifier = Modifier.weight(1f)) {
                                val isPrimary = followState == PeerFollowState.FOLLOW || followState == PeerFollowState.FOLLOW_BACK
                                val buttonBg by animateColorAsState(
                                    targetValue = if (isPrimary) TriggerAccent else Color(0xFF242C35),
                                    animationSpec = tween(250),
                                    label = "buttonBg",
                                )
                                val textColor = if (isPrimary) Color.Black else Color.White

                                Surface(
                                    onClick = {
                                        when (followState) {
                                            PeerFollowState.FOLLOW, PeerFollowState.FOLLOW_BACK -> {
                                                followState = PeerFollowState.FOLLOWING
                                                followerDelta = 1
                                            }
                                            PeerFollowState.FOLLOWING -> {
                                                showFollowDropdown = true
                                            }
                                            PeerFollowState.REQUESTED -> {
                                                followState = PeerFollowState.FOLLOW
                                                followerDelta = 0
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = buttonBg,
                                    border = if (isPrimary) null else BorderStroke(1.dp, Color(0xFF384351)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(38.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        when (followState) {
                                            PeerFollowState.FOLLOW -> {
                                                Text(
                                                    text = "Follow",
                                                    color = textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            PeerFollowState.FOLLOW_BACK -> {
                                                Text(
                                                    text = "Follow Back",
                                                    color = textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                            PeerFollowState.FOLLOWING -> {
                                                Text(
                                                    text = "Following",
                                                    color = textColor,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(15.dp),
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = textColor,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            PeerFollowState.REQUESTED -> {
                                                Text(
                                                    text = "Requested",
                                                    color = Color(0xFFA0AEC0),
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = Color(0xFFA0AEC0),
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                // Dropdown to manually switch to any of the 4 requested states for testing
                                DropdownMenu(
                                    expanded = showFollowDropdown,
                                    onDismissRequest = { showFollowDropdown = false },
                                    modifier = Modifier.background(Color(0xFF222831)),
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Follow", color = Color.White) },
                                        onClick = {
                                            followState = PeerFollowState.FOLLOW
                                            followerDelta = 0
                                            showFollowDropdown = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Following", color = TriggerAccent) },
                                        onClick = {
                                            followState = PeerFollowState.FOLLOWING
                                            followerDelta = 1
                                            showFollowDropdown = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Follow Back", color = Color.White) },
                                        onClick = {
                                            followState = PeerFollowState.FOLLOW_BACK
                                            followerDelta = 0
                                            showFollowDropdown = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Requested", color = Color(0xFFA0AEC0)) },
                                        onClick = {
                                            followState = PeerFollowState.REQUESTED
                                            followerDelta = 0
                                            showFollowDropdown = false
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Unfollow", color = Color(0xFFFF5252)) },
                                        onClick = {
                                            followState = PeerFollowState.FOLLOW
                                            followerDelta = 0
                                            showFollowDropdown = false
                                        },
                                    )
                                }
                            }

                            // Message Button (Direct Chat Action)
                            Surface(
                                onClick = { onOpenChat(u.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF242C35),
                                border = BorderStroke(1.dp, Color(0xFF384351)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Message",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Grouped Info List
                        TriggerProfileGroupedList(Modifier.padding(horizontal = 16.dp)) {
                            TriggerProfileDetailRow(
                                label = TriggerStrings.Ui.NAME,
                                value = u.username,
                                valueMaxLines = 2,
                            )
                            TriggerProfileGroupedDivider()
                            TriggerProfileDetailRow(
                                label = TriggerStrings.Ui.ABOUT,
                                value = u.bio.ifBlank { TriggerStrings.Defaults.NEW_USER_BIO },
                                valueMaxLines = DisplayTextLimits.MAX_BIO_LINES,
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // Social Mutual Connections Note
                        Surface(
                            color = Color(0xFF1B222A),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "👥 Followed by jordan_dev, alex_river and 8 others you know",
                                    color = Color(0xFF90A4AE),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }
                loadError != null -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = loadError,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(onClick = onRetry) {
                            Text(TriggerStrings.Ui.TRY_AGAIN)
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }

        // Fullscreen Profile Photo Overlay
        AnimatedProfilePhotoViewerOverlay(
            visible = showPhotoViewer && user != null,
            imageUrl = user?.imageUrl.orEmpty(),
            title = user?.username.orEmpty(),
            onDismiss = { showPhotoViewer = false },
        )

        // Options bottom sheet
        if (showOptionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showOptionsSheet = false },
                containerColor = Color(0xFF1E242B),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Profile Options", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOptionsSheet = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("Share Profile Link", color = Color.White, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOptionsSheet = false }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🚫 Block User", color = Color(0xFFFF5252), fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // Followers Bottom Sheet
        if (showFollowersSheet) {
            PeerConnectionsSheet(
                title = "Followers",
                onDismiss = { showFollowersSheet = false },
            )
        }

        // Following Bottom Sheet
        if (showFollowingSheet) {
            PeerConnectionsSheet(
                title = "Following",
                onDismiss = { showFollowingSheet = false },
            )
        }
    }
}

@Composable
private fun PeerStatColumn(
    count: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = count,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = TriggerProfileMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeerConnectionsSheet(
    title: String,
    onDismiss: () -> Unit,
) {
    val sampleUsers = remember {
        listOf(
            Triple("Jordan Hayes", "@jordan_dev", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"),
            Triple("Alex River", "@alex_river", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"),
            Triple("Cleopatra Vance", "@cleopatra", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
            Triple("Sam Coder", "@sam_coder", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C2229),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            sampleUsers.forEach { (name, handle, avatar) ->
                var isFollowed by remember { mutableStateOf(true) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TriggerProfileAvatar(
                        imageUrl = avatar,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(handle, color = Color(0xFF90A4AE), fontSize = 12.sp)
                    }
                    Surface(
                        onClick = { isFollowed = !isFollowed },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFollowed) Color(0xFF26303B) else TriggerAccent,
                        modifier = Modifier.height(32.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        ) {
                            Text(
                                text = if (isFollowed) "Following" else "Follow",
                                color = if (isFollowed) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Peer profile route: [PeerProfileViewModel] and [PeerProfileScreenContent].
 *
 * @param onBack Navigate up.
 * @param onOpenChat Navigate to direct chat with peer.
 * @param viewModel MVI [PeerProfileViewModel].
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerProfileRoute(
    onBack: () -> Unit,
    onOpenChat: (peerId: String) -> Unit = {},
    viewModel: PeerProfileViewModel = koinViewModel(),
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    PeerProfileScreenContent(
        ui = ui,
        onBack = onBack,
        onOpenChat = onOpenChat,
        onRetry = { viewModel.onEvent(PeerProfileUiEvent.Retry) },
    )
}

/**
 * Compose preview for [PeerProfileScreenContent] with a loaded sample [User].
 * @author triggerapp
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = false)
@Composable
private fun PeerProfileScreenPreview() {
    val sample = User(
        id = "1",
        username = "Alex River",
        emailId = "",
        timestamp = "0",
        imageUrl = "",
        bio = "Building Trigger · coffee · photos 🚀",
        status = "",
        searchKey = "alex",
    )
    TriggerTheme(darkTheme = true, dynamicColor = false, useBrandDarkColors = true) {
        PeerProfileScreenContent(
            ui = PeerProfileUiState(user = sample),
            onBack = {},
            onOpenChat = {},
            onRetry = {},
        )
    }
}
